package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.CombinatorThreadPool;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;

public class CopyWire<T> extends Combinator {
	
	private final Class<T> dataType;
	private final int noOfCopyPorts;
	private final ReentrantLock mutexIn;
	private volatile boolean copyFailed;
	private final ConcurrentLinkedQueue<CopyRunner> runners;
	
	public CopyWire(Class<T> dataType, int noOfCopyPorts, boolean fair, CombinatorOrientation orientation) {
		super(orientation);
		if(dataType == null) {
			throw new IllegalArgumentException("Copy Wire Data Type cannot be null");
		}
		if(noOfCopyPorts < 2) {
			throw new IllegalArgumentException("Copy Wire must have at least two copy ports");
		}
		this.dataType = dataType;
		this.noOfCopyPorts = noOfCopyPorts;
		mutexIn = new ReentrantLock(fair);
		runners = new ConcurrentLinkedQueue<>();
	}
	
	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {
			
			private final MessageFailureException ex = new MessageFailureException("Copy failed");

			@SuppressWarnings("unchecked")
			@Override
			public void accept(final Message<? extends T> msg)
					throws MessageFailureException {
				
				mutexIn.lock();
				try {
					// make sure failure flag is reset
					copyFailed = false;
					final CountDownLatch copyStart = new CountDownLatch(1);
					final CountDownLatch copyComplete = new CountDownLatch(noOfCopyPorts);
					// start all copy runners (threads)
					for(int i=0; i<noOfCopyPorts; i++) {
						// create copy message by encapsulating the original one
						Message<T> copyMsg = (Message<T>) new Message<>(msg);
						CopyRunner runner = new CopyRunner(copyMsg, i, copyStart, copyComplete);
						runners.add(runner);
						CombinatorThreadPool.execute(runner);
					}
//					System.out.println(msg);
					// allow runners to execute when all wrappers around the original
					// mesage are initialised
					copyStart.countDown();
					// wait for all runners to complete
					copyComplete.await();
					// all done -> check for problems
					if(copyFailed) {
						throw ex;
					}
				} catch (InterruptedException e) {
					// this shouldn't really happen
					throw new MessageFailureException("Interupted when waiting for copy to complete");
				} finally {
					mutexIn.unlock();
				}
			}
		}));	
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		// add the required number of copy ports
		for(int i=0; i<noOfCopyPorts; i++) {
			ports.add(Port.getActivePort(dataType, DataFlow.OUT));
		}
		return ports;
	}
	
	private class CopyRunner implements Runnable {
		
		private volatile Thread executor;
		private volatile boolean cancelled;
		
		private final CountDownLatch copyStart;
		private final CountDownLatch copyComplete;
		private final int portIndex;
		private final Message<T> msg;
		
		public CopyRunner(Message<T> msg, int portIndex, 
				CountDownLatch copyStart, CountDownLatch copyComplete) {
			
			this.msg = msg;
			this.portIndex = portIndex;
			this.copyStart = copyStart;
			this.copyComplete = copyComplete;
		}
		
		@Override
		public void run() {
			executor = Thread.currentThread();
			if(! cancelled) {
				try {
					copyStart.await();
					getRightBoundary().send(msg, portIndex);
				} catch(MessageFailureException | InterruptedException ex) {
					copyFailed = true;
				} finally {
					runners.remove(this);
					if(msg.isCancelled()) {
						CopyRunner runner;
						while((runner = runners.poll()) != null) {
							runner.cancelled = true;
							if(runner.executor != null) {
								runner.executor.interrupt();
							}
						}
					}
					copyComplete.countDown();
				}
			} else {
				runners.remove(this);
				copyComplete.countDown();
			}
		}
	}

}
