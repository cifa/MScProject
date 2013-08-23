package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

import uk.ac.soton.combinator.core.Backoff;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.CombinatorThreadPool;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorPermanentFailureException;
import uk.ac.soton.combinator.core.exception.CombinatorTransientFailureException;

public class JoinPullWire<T> extends Combinator {
	
	private static final CombinatorPermanentFailureException PERMANENT_EXCEPTION = 
			new CombinatorPermanentFailureException("Unable to join all messages (not equal)");

	private static final CombinatorTransientFailureException TRANSIENT_EXCEPTION = 
			new CombinatorTransientFailureException("No messages currently available");
	
	private final Class<T> dataType;
	private final int noOfJoinPorts;
	private final ReentrantLock mutexOut;
	private final AtomicReferenceArray<Message<T>> joinMessages;
	private final AtomicInteger noOfPulledMsgs;
	private final boolean optimisticRetry;
	private volatile boolean permanentFailure;
	
	public JoinPullWire(Class<T> dataType, int noOfJoinPorts, boolean fair, CombinatorOrientation orientation) {
		this(dataType, noOfJoinPorts, fair, orientation, true);
	}
	
	public JoinPullWire(Class<T> dataType, int noOfJoinPorts, boolean fair, 
			CombinatorOrientation orientation, boolean otimisticRetry) {
		
		super(orientation);
		if(dataType == null) {
			throw new IllegalArgumentException("Join Wire Data Type cannot be null");
		}
		if(noOfJoinPorts < 2) {
			throw new IllegalArgumentException("Join Wire must have at least two join ports");
		}
		this.dataType = dataType;
		this.noOfJoinPorts = noOfJoinPorts;
		this.optimisticRetry = otimisticRetry;
		noOfPulledMsgs = new AtomicInteger();
		mutexOut = new ReentrantLock(fair);
		joinMessages = new AtomicReferenceArray<>(noOfJoinPorts);
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
			
			@Override
			public Message<? extends T> produce() throws CombinatorPermanentFailureException {
				mutexOut.lock();
				try {
					// make sure the flags are reset
					permanentFailure = false;
					noOfPulledMsgs.set(0);
					// how many runners do we need?
					CountDownLatch runnersComplete = new CountDownLatch(noOfJoinPorts);
					for(int i=0; i<noOfJoinPorts; i++) {
						CombinatorThreadPool.execute(new JoinRunner(i, runnersComplete));
					}
					// wait for all runners to complete
					try {
						runnersComplete.await();
					} catch (InterruptedException e) {
						permanentFailure = true;
					}
					// all good?
					if(!permanentFailure && noOfPulledMsgs.get() == noOfJoinPorts) {
						@SuppressWarnings("unchecked")
						Message<T>[] msgs = (Message<T>[]) new Message<?>[noOfJoinPorts];
						msgs[0] = joinMessages.get(0); 
						// we have all messages -> are they contents the same?	
						/* TODO What if we didn't check for equality?? We would 
						 * have a join message with (potentially) unequal contents
						 * but the consumer could eventually decide if that's ok.
						 * (This wouldn't work with the current implementation of
						 * failure handling where fully acknowledged msg removes
						 * all associations with encapsulated/wrapper messages)
						 */
						for(int i=1; i<noOfJoinPorts; i++) {
							if(joinMessages.get(0).contentEquals(joinMessages.get(i))) {
								msgs[i] = joinMessages.get(i);
							} else {
								// FAILURE -> unequal messages
								permanentFailure = true;
								break;
							}
						}
						
						if(!permanentFailure) {
							// all messages are equal -> return join message
							return new Message<T>(msgs);
						}
					}
					
					// STILL HERE -> did we get any msgs at all?
					if(noOfPulledMsgs.get() == 0) {
						// transiently ran out of retries -> fail transiently 
						throw TRANSIENT_EXCEPTION;
					}
					
					// there must have been a permanent failure
					// invalidate any fetched msgs and clear up
					for(int i=0; i<noOfJoinPorts; i++) {
						Message<T> msg = joinMessages.getAndSet(i, null);
						if(msg != null) {
							msg.cancel(false);
						}
					}
					// throw permanent failure exception
					throw PERMANENT_EXCEPTION;
				} finally {
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
		private Backoff backoff;
		
		public JoinRunner(int portIndex, CountDownLatch complete) {
			this.portIndex = portIndex;
			this.runnersComplete = complete;
		}
		
		@Override
		public void run() {
			while(!permanentFailure) {
				try {
					@SuppressWarnings("unchecked")
					Message<T> msg = (Message<T>) receiveLeft(portIndex);
					// set msg for this port
					joinMessages.set(portIndex, msg);
					noOfPulledMsgs.incrementAndGet();
					// success -> return
					break;
				} catch (CombinatorTransientFailureException ex) {
					if(!optimisticRetry || permanentFailure) {
						break;
					}
					// back off and retry
					if(backoff == null) {
						backoff = new Backoff();
					}
					try {
						backoff.backoff();
					} catch (InterruptedException e) {
						// shouldn't happen -> just clear the interrupt flag
						Thread.interrupted();
					}
				} catch (CombinatorPermanentFailureException ex) {
					permanentFailure = true;
				}				
			}
			// runner done
			runnersComplete.countDown();
		}
	}
}
