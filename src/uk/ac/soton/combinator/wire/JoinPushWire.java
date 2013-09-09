package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

import uk.ac.soton.combinator.core.Backoff;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.CombinatorThreadPool;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorFailureException;
import uk.ac.soton.combinator.core.exception.CombinatorPermanentFailureException;
import uk.ac.soton.combinator.core.exception.CombinatorTransientFailureException;

public class JoinPushWire<T> extends Combinator implements Runnable {
	
	private static final CombinatorPermanentFailureException JOIN_EXCEPTION = 
			new CombinatorPermanentFailureException("Unable to join all messages (not equal)");
	private static final CombinatorPermanentFailureException BARRIER_INTERRUPTION_EXCEPTION = 
			new CombinatorPermanentFailureException("Thread interrupted while waiting");
	private static final CombinatorTransientFailureException BARRIER_BROKEN_EXCEPTION = 
			new CombinatorTransientFailureException("Barrier broken by another waiting thread");
	
//	private static int counter = 0;
//	private final int id = ++counter;
	
	private final Class<T> dataType;
	private final int noOfJoinPorts;
	private final CyclicBarrier barrier;
	private final ReentrantLock[] locks;
	private final AtomicReferenceArray<Message<T>> joinMessages;
	
	private volatile boolean msgJoinSuccessful;
	
	public JoinPushWire(Class<T> dataType, int noOfJoinPorts, boolean fair, CombinatorOrientation orientation) {
		super(orientation);
		if(dataType == null) {
			throw new IllegalArgumentException("Join Wire Data Type cannot be null");
		}
		if(noOfJoinPorts < 2) {
			throw new IllegalArgumentException("Join Wire must have at least two join ports");
		}
		this.dataType = dataType;
		this.noOfJoinPorts = noOfJoinPorts;
		joinMessages = new AtomicReferenceArray<>(noOfJoinPorts);
		locks = new ReentrantLock[noOfJoinPorts];
		for(int i=0; i<noOfJoinPorts; i++) {
			locks[i] = new ReentrantLock(fair);
		}
		barrier = new CyclicBarrier(noOfJoinPorts, this);
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		for(int i=0; i<noOfJoinPorts; i++) {
			ports.add(createLeftJoinPort(i));
		}
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getActivePort(dataType, DataFlow.OUT));
		return ports;
	}
	
	private Port<T> createLeftJoinPort(final int index) {
		return Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {
			
			private final int portIndex = index;

			@SuppressWarnings("unchecked")
			@Override
			public void accept(Message<? extends T> msg)
					throws CombinatorFailureException{
				locks[portIndex].lock();
				try {
					joinMessages.set(portIndex, (Message<T>) msg);
					barrier.await();
					// join complete -> was it a success??
					if(!msgJoinSuccessful) {
						// FAILURE - invalidate all messages
						msg.cancel(false);
						throw JOIN_EXCEPTION;
					}
				} catch (InterruptedException | BrokenBarrierException e) {
					/* This means that one of the join messages has been invalidated
					 * and the barrier has been broken. This releases all waiting threads
					 * including those whose messages are still valid.
					 */
					if(msg.isCancelled()) {
						// invalid messages fail pernamently
						Thread.interrupted();
						throw BARRIER_INTERRUPTION_EXCEPTION;
					} else {
						// valid messages fail transiently
						throw BARRIER_BROKEN_EXCEPTION;
					}
				} finally {
					if(barrier.isBroken()) {
						barrier.reset();
					}
					locks[portIndex].unlock();
				}
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		msgJoinSuccessful = true;
		Message<T>[] msgs = (Message<T>[]) new Message<?>[noOfJoinPorts];
		msgs[0] = joinMessages.get(0);
		
		for(int i=1; i<noOfJoinPorts; i++) {
			msgs[i] = joinMessages.get(i);
			if(!msgs[0].contentEquals(msgs[i])) {
				msgJoinSuccessful = false;
				break;
			}
		}
		
		if(msgJoinSuccessful) {
			// we create a new join message from all received messages
			final Message<T> joinMsg = new Message<>(msgs);
			// ... and send it on
			CombinatorThreadPool.execute(new Runnable() {	
				@Override
				public void run() {
					Backoff backoff = null;
					while(true) {
						try {
							sendRight(joinMsg, 0);
							break;
						} catch (CombinatorTransientFailureException ex) {
							// back off and retry
							if(backoff == null) {
								backoff = new Backoff();
							}
							try {
								backoff.backoff();
							} catch (InterruptedException e) {
								// assoc msg of joinMsg invalidated -> joinMsg now invalid as well
								break;
							}
						} catch (CombinatorPermanentFailureException ex) {
							// joinMsg failed permanently -> nothing we can do now
							break;
						}
					}
				}
			});
		}
	}

}
