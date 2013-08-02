package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.CombinatorThreadPool;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.RequestFailureException;

public class JoinPullWire<T> extends Combinator {
	
	private final Class<T> dataType;
	private final int noOfJoinPorts;
	private final ReentrantLock mutexOut;
	private final Message<T>[] joinMessages;
	private final int retries;
	private final Backoff pullBackoff;
	private AtomicInteger noOfMsgToFetch = new AtomicInteger();
	
	/**
	 * Creates a new join pull wire with an unlimited number of message pull retries. 
	 * This wire keeps trying to retrieve messages from the join ports till it succeeds
	 * on all of them. Note that only failed ports are retried so no messages are discarded
	 * between retries.
	 */
	public JoinPullWire(Class<T> dataType, int noOfJoinPorts, boolean fair, CombinatorOrientation orientation) {
		this(dataType, noOfJoinPorts, fair, orientation, -1);
	}
	
	/**
	 * Creates a new join pull wire with a specified number of message pull retries. 
	 * 
	 * @param noOfPullRetries	if less than 0 then the number of message pull retries is UNLIMITED
	 * 							if 0 then an attempt 
	 */
	@SuppressWarnings("unchecked")
	public JoinPullWire(Class<T> dataType, int noOfJoinPorts, boolean fair, CombinatorOrientation orientation, int noOfPullRetries) {
		super(orientation);
		if(dataType == null) {
			throw new IllegalArgumentException("Join Wire Data Type cannot be null");
		}
		if(noOfJoinPorts < 2) {
			throw new IllegalArgumentException("Join Wire must have at least two join ports");
		}
		this.dataType = dataType;
		this.noOfJoinPorts = noOfJoinPorts;
		this.retries = noOfPullRetries;
		mutexOut = new ReentrantLock(fair);
		joinMessages = (Message<T>[]) new Message<?>[noOfJoinPorts];
		pullBackoff = new Backoff();
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		// add the required number of join ports
		for(int i=0; i<noOfJoinPorts; i++) {
			ports.add(Port.getActivePort(dataType, DataFlow.IN));
		}
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveOutPort(dataType, new PassiveOutPortHandler<T>() {
			
			private final RequestFailureException ex = new RequestFailureException("Unable to join all messages (not equal)");

			@Override
			public Message<T> produce() throws RequestFailureException {
				mutexOut.lock();
				try {
					int retriesLeft = retries;
					// start fetching on all parts
					noOfMsgToFetch.set(noOfJoinPorts);
					while(retries < 0 || retriesLeft >= 0) {
						// how many runners do we need?
						CountDownLatch runnersComplete = new CountDownLatch(noOfMsgToFetch.get());
						// start join runners on ports that haven't fetched a message yet
						for(int i=0; i<noOfJoinPorts; i++) {
							if(joinMessages[i] == null) {
								CombinatorThreadPool.execute(new JoinRunner(i, runnersComplete));
							}
						}
						// wait for all runners to complete
						runnersComplete.await();
						if(noOfMsgToFetch.get() == 0) {
							// we have all messages -> are they contents the same?	
							/* TODO What if we didn't check for equality?? We would 
							 * have a join message with (potentially) unequal contents
							 * but the consumer could eventually decide if that's ok.
							 * (This wouldn't work with the current implementation of
							 * failure handling where fully acknowledged msg removes
							 * all associations with encapsulated/wrapper messages)
							 */
							for(int i=1; i<noOfJoinPorts; i++) {
								if(!joinMessages[0].contentEquals(joinMessages[i])) {
									// FAILURE - invalidate all join messages
									for(Message<T> msg : joinMessages) {
										msg.cancel(false);
									}
									throw ex;
								}
							}
							// return a new join message
							return new Message<T>(Arrays.copyOf(joinMessages, joinMessages.length));					
						} else {
							// some msgs not fetched -> let's back off and retry
							if(retries < 0 || --retriesLeft >= 0) {
								// don't backoff if there is no retry left
								pullBackoff.backoff();
							}	
						}
					}
					// can only happen with a limited number of retries (or none)
					throw new RequestFailureException("Unable to fetch messages from all join ports");
				} catch (InterruptedException e) {
					// this shouldn't really happen
					throw new RequestFailureException("Interupted when waiting for join ports to fetch messages");
				} finally {
					// reset the wire for next join operation
					for(int i=0; i<noOfJoinPorts; i++) {
						joinMessages[i] = null;
					}
					// ... and let go
					mutexOut.unlock();
				}
			}
		}));
		return ports;
	}
	
	private class JoinRunner implements Runnable {
		
		private final CountDownLatch runnersComplete;
		private final int portIndex;
		
		public JoinRunner(int portIndex, CountDownLatch complete) {
			this.portIndex = portIndex;
			this.runnersComplete = complete;
		}
		
		@Override
		public void run() {
			try {
				@SuppressWarnings("unchecked")
				Message<T> msg = (Message<T>) getLeftBoundary().receive(portIndex);
				// set msg for this port
				joinMessages[portIndex] = msg;
				// and (atomically) mark its success
				noOfMsgToFetch.decrementAndGet();
			} catch(RequestFailureException ex) {
				// no msg will trigger a retry (if applicable)
			} finally {
				runnersComplete.countDown();
			}
		}
	}
}
