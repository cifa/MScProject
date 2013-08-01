package uk.ac.soton.combinator.core;

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;


public class Message<T> implements Future<T> {
	
	/* Denotes the initial state of the message meaning
	 * that is has not been either fully acknowledged or 
	 * cancelled (invalidated)
	 */
	private static final int ACTIVE = 0;
	
	// Indicates that the message has been invalidated (cancelled)
	private static final int CANCELLED = 1;
	
	/* Indicates that this message and all other related messages have been
	 * acknowledged and the message content can be safely retrieved
	 */
	private static final int FULLY_ACKNOWLEDGED = 2;
	
	// hold the current state of the message (one of the 3 above)
	private final AtomicInteger messageState;
	
	private final Class<T> messageDataType;
	private final T content;
	
	/* Invoked at most once when the message is invalidated (cancelled)
	 * This callback is optional (producers don't have to provide one)
	 */
	private final MessageEventHandler<T> messageCallback;
	
	/* Messages are type verified when they are sent through a port 
	 * for the first time (type-safe combinations of ports ensure type
	 * safety from there onwards)
	 */
	private volatile boolean typeVerified;
	
	/* Indicates that this message has reached its intended recipient 
	 * that has either tried to obtain the content through one of the
	 * get() methods or validated the message content using a MessageValidator
	 * and the static validateMessageContent(...) method. However, this
	 * doesn't mean that the content can be retrieved as the message might 
	 * need to wait for other related messages to be also acknowledged
	 */
	private volatile boolean acknowledged;
	
	/* Denotes the thread that is currently executing the full acknowledgement
	 * check on this message. If this message is a (top level) wrapper message 
	 * then the isWrapperAck() method will be also invoked as part of the full
	 * acknowledgement check. In such a case it would not be beneficial to unpark
	 * other threads waiting in the get() methods as it requires an intervention 
	 * of a full acknowledgement check invoked in another (related) message to 
	 * alter the state of this message.
	 */
	private volatile Thread fullAcknowledgementRunner;
	
	// This message is wrapped around these messages
	private volatile Message<T>[] encapsulatedMsgs;
	
	// List of messages (wrappers) that are wrapped around this messages
	private volatile Set<Message<T>> wrapperMsgs;	
	
	/* Queue of threads trying to retrieve the message content through
	 * one of the get() methods when the message is not fully acknowledged
	 * yet.
	 */
	private final ConcurrentLinkedQueue<Thread> waiters; 
	
	
	//******** CONSTRUCTORS **************

	public Message(Class<T> messageDataType, T content) {
		this(messageDataType, content, null);
	}
	
	public Message(Class<T> messageDataType, T content, MessageEventHandler<T> messageCallback) {
		if(messageDataType == null) {
			throw new IllegalArgumentException("Message Data Type cannot be null");
		}
		this.messageDataType = messageDataType;
		this.content = content;
		this.messageCallback = messageCallback;
		this.messageState = new AtomicInteger(ACTIVE);
		this.waiters = new ConcurrentLinkedQueue<>();
	}
	
	/**
	 * Constructor used by copy and join wires to wrap other messages
	 * @param msgs  arbitrary number of messages to wrap (at least one) 
	 */
	//TODO maybe, this shouldn't be public?? (would require package refactoring)
	@SafeVarargs
	public Message(Message<T>... msgs) {
		// TODO copying the content of the first msg will only work for immutable objects
		this(msgs[0].messageDataType, msgs[0].content);
		encapsulatedMsgs = msgs;
		// Message wrapper is type safe
		setTypeVerified(true);
		// assoc the encapsulated msgs with this wrapper
		for(Message<T> msg : msgs) {
			msg.addOuterWrapperMessage(this);
			// encapsulated msgs must be active
			msg.messageState.set(ACTIVE);
		}
	}
	
	//********** METHODS ***************

	@Override
	public T get() throws CancellationException {
		// an attempt to access the content is considered as acknowledgement that the 
		// message has been received by its consumer
		acknowledged = true;
		// msgs must be fully acknowledged before we can return the value
		while(messageState.get() != FULLY_ACKNOWLEDGED) {
			// add current thread as a potential waiter
			waiters.add(Thread.currentThread());
			
			// throw exception if message invalidated (cancelled)
			if(isCancelled()) {
				if(! waiters.remove(Thread.currentThread())) {
					/*
					 * This means that another thread executed unparkWaiters
					 * in after the current thread was added to the waiters queue.
					 * This means that the next call to park() is guaranteed not to
					 * block - get rid of it
					 */
					LockSupport.park(this);
				}
				throw new CancellationException("Cannot access the " +
						"content of an invalidated message");
			}
			
			synchronized (this) {
				// check if fully acknowledged now 
				if(runFullAcknowledgementTest()) {
					//try to remove from waiters if fully acknowledged
					if(! waiters.remove(Thread.currentThread())) {					
						LockSupport.park(this);
					}
					// fully acknowledged -> return content
					break;
				}
			}

			// wait for changes (either invalidation or full acknowledgement)
			LockSupport.park(this);
		}
		// we are fully acknowledged and can return the value
		return content;
	}
	

	@Override
	public T get(long timeout, TimeUnit unit) throws CancellationException, TimeoutException {
		// an attempt to access the content is considered as acknowledgement that the 
		// message has been received by its consumer
		acknowledged = true;
		
		if(messageState.get() != FULLY_ACKNOWLEDGED) {
			if(isCancelled()) {
				throw new CancellationException("Cannot access the " +
						"content of an invalidated message");
			}
			synchronized (this) {
				if(runFullAcknowledgementTest()) {
					// fully acknowledged -> return content
					return content;
				}
			}
			// we need to wait
			waiters.add(Thread.currentThread());
			LockSupport.parkNanos(this, unit.toNanos(timeout));
			
			// re-check if still valid
			if(isCancelled()) {
				throw new CancellationException("Cannot access the " +
						"content of an invalidated message");
			}
			// timeout if still not acknowledged
			if(messageState.get() != FULLY_ACKNOWLEDGED) {
				throw new TimeoutException("Message not fully acknowledged yet");
			}
		}
		// fully acknowledged -> return content
		return content;
	}
	
	/**
	 * Cancels (invalidates) the message
	 * 
	 * @param mayInterruptIfRunning	 no relevance - disregarded 
	 * @return	true if the message is cancelled (invalidated), false otherwise
	 * 			(e.g. message already cancelled - invalidated)
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		// Message cannot be cancelled (invalidated) if the content
		// of the message has already been fully acknowledged.
		// Also, we want to invalidate each message only once
		boolean invalidate = messageState.compareAndSet(ACTIVE, CANCELLED);

		if(invalidate) {
			// notify related msgs using local vars
			Message<T>[] encaps = encapsulatedMsgs;
			Set<Message<T>> wrappers = wrapperMsgs;
			
			// we've reached a final immutable state -> no longer
			// dependant on any encapsulated/wrapper messages
			encapsulatedMsgs = null;
			wrapperMsgs = null;
			
			/* Unpark any threads waiting to get the content - they will
			 * throw a CancellationException as the message is now invalid (cancelled)
			 */
			unparkWaiters();
			
			// invalidate all encapsulated messages
			if(encaps != null) {
				for(Message<T> msg : encaps) {
					msg.cancel(mayInterruptIfRunning);
				}
			}
			// invoke invalidation callback on this message (if any)
			if(messageCallback != null) {
				// execute the callback as a separate task
				CombinatorThreadPool.execute(new Runnable() {
					
					@Override
					public void run() {
						messageCallback.messageInvalidated(Message.this, content);
					}
				});
				
			}
			// invalidate all wrappers
			if(wrappers != null) {
				for(Message<T> msg : wrappers) {
					msg.cancel(mayInterruptIfRunning);
				}
			}
		}
		return invalidate;
	}
	
	@Override
	public boolean isCancelled() {
		return messageState.get() == CANCELLED;
	}
	
	@Override
	public boolean isDone() {
		if(messageState.get() == ACTIVE) {
			// can it be fully acknowledged now?
			runFullAcknowledgementTest();
		}
		return messageState.get() != ACTIVE;
	}
	
	public Class<T> getMessageDataType() {
		return messageDataType;
	}
	
	public boolean contentEquals(Message<T> other) {
		if(other != null) {
			if(content == null) {
				return other.content == null;
			} else {
				return content.equals(other.content);
			}
		}
		return false;
	}
	
	
	@Override
	public String toString() {
		if(wrapperMsgs == null ) {
			return "NO WRAPPERS";
		}
		return "WRAPPER COUNT: " + wrapperMsgs.size();
	}
	
	/* TODO we cannot stop the validator from exposing the message content(s)
	 * to the outside world once it gets its hands on it ... any solution?
	 */
	@SafeVarargs
	@SuppressWarnings("unchecked")
	public static <T> boolean validateMessageContent(final MessageValidator<T> validator, 
			final Message<T>... msgs) {
		if(validator == null || msgs == null) {
			throw new IllegalArgumentException("MessageValidator and/or Messages to validate cannot be null");
		}
		// put content of all msgs into array
		T[] contents = (T[]) Array.newInstance(msgs[0].getMessageDataType(), msgs.length);
		for(int i=0; i<msgs.length; i++) {
			contents[i] = msgs[i].content;
		}
		// run validator
		boolean valid = validator.validate(contents);
		// either acknowledge or invalidate all msgs 
		// (these are guaranteed to be top level wrappers or root messages)
		for(Message<?> msg : msgs) {
			if(valid) {
				msg.acknowledged = true;
			} else {
				msg.cancel(false);
			}
		}
		return valid;
	}
	
	boolean addOuterWrapperMessage(Message<T> wrapperMsg) {
		// initialize the wrapper set only when it's really needed
		if(wrapperMsgs == null) {
			synchronized (this) {
				if(wrapperMsgs == null) {
					wrapperMsgs = new HashSet<>();
				}
			}
		}
		return wrapperMsgs.add(wrapperMsg);
	}

	/*
	 * There is a single point of entry to the 'system', where the message
	 * type needs to be verified. Beyond that point the message type safety
	 * is ensured by the typed ports which must be correctly wired up  
	 */
	void setTypeVerified(boolean typeVerified) {
		this.typeVerified = typeVerified;
	}
	
	boolean isTypeVerified() {
		return typeVerified;
	}
	
	private synchronized boolean runFullAcknowledgementTest() {
		fullAcknowledgementRunner = Thread.currentThread();
		boolean ack = isMessageFullyAcknowledged();
		fullAcknowledgementRunner = null;
		return ack;
	}
	
	private boolean isMessageFullyAcknowledged() {
		// must be true for simple unwrapped msgs
		boolean ack = true;

		if(messageState.get() == ACTIVE) {
			/* We do these checks with local pointer as the
			 * instance variables can be reset during execution 
			 * resulting in a NullPointerException
			 * (performing these checks after a reset has no side
			 * effects - they are useless - performance??)
			 */
			Message<T>[] encaps = encapsulatedMsgs;
			Set<Message<T>> wrappers = wrapperMsgs;
			if(encaps != null) {
				for(Message<T> msg : encaps) {
					ack = msg.isMessageFullyAcknowledged();
					// if any encaps msg not acknowledged then this one isn't either
					if(! ack) break;
				}
			} else if(wrappers != null) {
				// a bottom-of-the-hierarchy msg that has been wrapped -> are all wrappers acknowledged?
				for(Message<T> msg : wrappers) {
					ack = msg.isWrapperAck();
					// if any wrapper not acknowledged neither is this message
					if(! ack) break;
				}
			}
		}
		
		if(ack) {
			// try to set the state to FULLY_ACKNOWLEDGED 
			// (not guaranteed to succeed as another thread might have already done it)
			if(messageState.compareAndSet(ACTIVE, FULLY_ACKNOWLEDGED)) {
				// we've reached a final immutable state -> no longer
				// dependant on any encapsulated/wrapper messages
				encapsulatedMsgs = null;
				wrapperMsgs = null;
				
				// invoke full acknowledgement callback on this message (if any)
				if(messageCallback != null) {
					// execute the callback as a separate task
					CombinatorThreadPool.execute(new Runnable() {
						
						@Override
						public void run() {
							messageCallback.messageFullyAcknowledged(Message.this);
						}
					});	
				}
			}
		}
		return messageState.get() == FULLY_ACKNOWLEDGED;
//		fullyAcknowledged = ack  && valid.get();
//		return fullyAcknowledged;
	}
	
	private boolean isWrapperAck() {
		// top level wrapper returns its acknowledge value 
		if(wrapperMsgs == null) {
			if(! Thread.currentThread().equals(fullAcknowledgementRunner)) {
				unparkWaiters();
			}
			return acknowledged;
		}
		// otherwise we go up the hierarchy
		boolean ack = true;
		for(Message<T> msg : wrapperMsgs) {
			if(! msg.isWrapperAck()) {
				ack = false;
				break;
			}
		}
		// if all my wrappers are acknowledged so am I 
		acknowledged = ack;
		return ack;
	}
	
	private void unparkWaiters() {
		Thread t;
		while((t = waiters.poll()) != null) {
			LockSupport.unpark(t);
		}
	}
}
