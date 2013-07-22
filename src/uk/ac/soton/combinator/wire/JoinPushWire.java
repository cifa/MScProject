package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.ReentrantLock;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.CombinatorThreadPool;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;

public class JoinPushWire<T> extends Combinator {
	
	private final Class<T> dataType;
	private final int noOfJoinPorts;
	private final CyclicBarrier barrier;
	private final ReentrantLock[] locks;
	private final AtomicReferenceArray<Message<T>> joinMessages;
	private final MessageFailureException ex = new MessageFailureException("Unable to join all messages (not equal)");
	
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
		barrier = new CyclicBarrier(noOfJoinPorts, new Runnable() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				msgJoinSuccessful = true;
				Message<T>[] msgs = (Message<T>[]) new Message<?>[JoinPushWire.this.noOfJoinPorts];
				msgs[0] = joinMessages.get(0);
				
				for(int i=1; i<JoinPushWire.this.noOfJoinPorts; i++) {
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
							getRightBoundary().send(joinMsg, 0);
						}
					});
				}
			}
		});
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
					throws MessageFailureException {
				locks[portIndex].lock();
				try {
					joinMessages.set(portIndex, (Message<T>) msg);
					barrier.await();
					// join complete -> was it a success??
					if(!msgJoinSuccessful) {
						throw ex;
					}
				} catch (InterruptedException | BrokenBarrierException e) {
					// this shouldn't really happen
					new MessageFailureException("Barrier Broken");
				} finally {
					locks[portIndex].unlock();
				}
			}
		});
	}

}
