package uk.ac.soton.combinator.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public class Message<T> {

	private final Class<T> messageDataType;
	private final T content;
	private final MessageInvalidationCallback invalidationCallback;
	
	private volatile boolean typeVerified;
	private volatile Message<T>[] encapsulatedMsgs;
	private volatile Set<Message<T>> wrapperMsgs;
	private volatile boolean acknowledged = true;
	
	private AtomicBoolean valid;
	

	public Message(Class<T> messageDataType, T content) {
		this(messageDataType, content, null);
	}
	
	public Message(Class<T> messageDataType, T content, MessageInvalidationCallback invalidationCallback) {
		if(messageDataType == null) {
			throw new IllegalArgumentException("Message Data Type cannot be null");
		}
		this.messageDataType = messageDataType;
		this.content = content;
		this.invalidationCallback = invalidationCallback;
		valid = new AtomicBoolean(true);
		setTypeVerified(false);
	}
	
	/**
	 * Constructor used by copy and join wires to wrap other messages
	 * @param msgs  arbitrary number of messages to wrap (at least one) 
	 */
	//TODO this shouldn't be public - package refactoring needed
	@SafeVarargs
	public Message(Message<T>... msgs) {
		// TODO do we copy the message content or set it to null?
		this(msgs[0].messageDataType, msgs[0].content);
		encapsulatedMsgs = msgs;
		// Message wrapper is type safe
		typeVerified = true;
		// assoc the encapsulated msgs with this wrapper
		for(Message<T> msg : msgs) {
			msg.addOuterWrapperMessage(this);
		}
		// wrappers aren't acknowledged by default
		acknowledged = false;
	}

	public Class<T> getMessageDataType() {
		return messageDataType;
	}

	public T getContent() throws AccessingInvalidatedMessageException, NotYetAcknowledgedException {
		/*
		 * Make the content of messages directly available in the MessageValidator validate(...) 
		 * method provided that the MessageValidator is invoked through the static 
		 * validateMessageContent method of the Message class.
		 * TODO it might be better to grab the executing thread id and associate it with 
		 * messages being validated. These would then granted unrestricted access to the content
		 * if getContent() was invoked through that thread
		 */
		if(Thread.currentThread().getStackTrace()[3].getMethodName().equals("validateMessageContent")) {
			return content;
		}
		
		// otherwise content accessible only if msg is valid and fully acknowledged
		
		// an attempt to access the content is considered as acknowledgement that the 
		// message has been received by its consumer
		acknowledged = true;
		 
		while(! isMessageFullyAcknowledged() || ! isMessageValid()) {
			// throw exception if message invalidated
			if(! isMessageValid()) {
				throw new AccessingInvalidatedMessageException("Cannot access the " +
						"content of an invalidated message");
			}
		}
			
		return content;
	}
	
	
	public boolean isMessageValid() {
		return valid.get();
	}
	
	/* TODO we cannot stop the validator from exposing the message content(s)
	 * to the outside world once it gets its hands on it ... any solution?
	 */
	public static boolean validateMessageContent(MessageValidator validator, final Message<?>... msgs) {
		if(validator == null || msgs == null) {
			throw new IllegalArgumentException("MessageValidator and/or Messages to validate cannot be null");
		}
		boolean valid = validator.validate(msgs);
		// either acknowledge or invalidate all msgs 
		// (these are guaranteed to be top level wrappers or root messages)
		for(Message<?> msg : msgs) {
			if(valid) {
				msg.acknowledged = true;
			} else {
				msg.invalidateMessage();
			}
		}
		return valid;
	}
	
	private boolean isMessageFullyAcknowledged() {
		boolean ack = acknowledged; // must be true for simple unwrapped msgs
		if(encapsulatedMsgs != null) {
			for(Message<T> msg : encapsulatedMsgs) {
				ack = msg.isMessageFullyAcknowledged();
				// if any encaps msg not acknowledged then this one isn't either
				if(! ack) break;
			}
		} else if(wrapperMsgs != null) {
			// a bottom-of-the-hierarchy msg that has been wrapped -> are all wrappers acknowledged?
			for(Message<T> msg : wrapperMsgs) {
				ack = msg.isWrapperAck();
				// if any wrapper not acknowledged neither is this message
				if(! ack) break;
			}
		}		
		return ack;
	}
	
	private boolean isWrapperAck() {
		// top level wrapper returns its acknowledge value 
		if(wrapperMsgs == null) {
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
		return ack;
	}
	
	void invalidateMessage() {
		// we want to invalidate each message only once
		if(valid.compareAndSet(true, false)) {
			if(encapsulatedMsgs != null) {
				for(Message<T> msg : encapsulatedMsgs) {
					msg.invalidateMessage();
				}
			}
			if(invalidationCallback != null) {
				//TODO call invalidation method 
			}
			if(wrapperMsgs != null) {
				for(Message<T> msg : wrapperMsgs) {
					msg.invalidateMessage();
				}
			}
		}
	}

	boolean isTypeVerified() {
		return typeVerified;
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
	
	// NOTE probably not needed
//	boolean removeOuterWrapperMessage(Message<T> wrapperMsg) {
//		if(wrapperMsgs != null) {
//			return wrapperMsgs.remove(wrapperMsg);
//		}
//		return false;
//	}

	/*
	 * There is a single point of entry to the 'system', where the message
	 * type needs to be verified. Beyond that point the message type safety
	 * is ensured by the typed ports which must be correctly wired up  
	 */
	void setTypeVerified(boolean typeVerified) {
		this.typeVerified = typeVerified;
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
}
