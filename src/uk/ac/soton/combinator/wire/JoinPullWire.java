package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
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
	private final AtomicReferenceArray<Message<T>> joinMessages;
	private final int retries = 5;
	private final long backOffPeriod = 10;
	private AtomicInteger noOfMsgToFetch = new AtomicInteger();
	
	public JoinPullWire(Class<T> dataType, int noOfJoinPorts, boolean fair, CombinatorOrientation orientation) {
		super(orientation);
		if(dataType == null) {
			throw new IllegalArgumentException("Join Wire Data Type cannot be null");
		}
		if(noOfJoinPorts < 2) {
			throw new IllegalArgumentException("Join Wire must have at least two join ports");
		}
		this.dataType = dataType;
		this.noOfJoinPorts = noOfJoinPorts;
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
			
			private final RequestFailureException ex = new RequestFailureException("Unable to join all messages (not equal)");

			@SuppressWarnings("unchecked")
			@Override
			public Message<T> produce() throws RequestFailureException {
				mutexOut.lock();
				try {
//					Thread.sleep(100);
					int retriesLeft = retries;
					// start fetching on all parts
					noOfMsgToFetch.set(noOfJoinPorts);
					while(retries < 0 || retriesLeft >= 0) {
						// how many runners do we need?
						CountDownLatch runnersComplete = new CountDownLatch(noOfMsgToFetch.get());
						// start join runners on ports that haven't fetched a message yet
						for(int i=0; i<noOfJoinPorts; i++) {
							if(joinMessages.get(i) == null) {
								CombinatorThreadPool.execute(new JoinRunner(i, runnersComplete));
							}
						}
						// wait for all runners to complete
						runnersComplete.await();
						if(noOfMsgToFetch.get() == 0) {
							// we have all messages -> are they the same?
							Message<T>[] msgs = (Message<T>[]) new Message<?>[noOfJoinPorts];
							msgs[0] = joinMessages.get(0);
							for(int i=1; i<noOfJoinPorts; i++) {
								msgs[i] = joinMessages.get(i);
								if(!msgs[0].contentEquals(msgs[i])) {
									throw ex;
								}
							}
							// all fine -> return a join message
							return new Message<>(msgs);
						} else if(noOfMsgToFetch.get() == noOfJoinPorts) {
							// no messages AT ALL -> fail without backoff 
							throw new RequestFailureException("Unable to fetch any messages at all");
						} else {
							// some msgs not fetched -> let's back off and retry
							if(retries < 0 || --retriesLeft >= 0) {
								// don't sleep if there is no retry left
								Thread.sleep(backOffPeriod);
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
						joinMessages.set(i, null);
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
				joinMessages.set(portIndex, msg);
				// and (atomically) mark its success
				noOfMsgToFetch.decrementAndGet();
			} catch(RequestFailureException ex) {
				// no msg will trigger a retry (if applicable)
//				System.out.println(ex.getMessage());
			} finally {
				runnersComplete.countDown();
			}
		}
	}
}
