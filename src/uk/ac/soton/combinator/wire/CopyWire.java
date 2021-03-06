package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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

public class CopyWire<T> extends Combinator {
	
	private static final CombinatorPermanentFailureException PERMANENT_EXCEPTION = 
			new CombinatorPermanentFailureException("Copy failed permanently");
	private static final CombinatorTransientFailureException TRANSIENT_EXCEPTION = 
			new CombinatorTransientFailureException("Copy failed transiently");
	
	private final Class<T> dataType;
	private final int noOfCopyPorts;
	private final ReentrantLock mutexIn;
	private final boolean retryOnTransientFailure;
	private volatile boolean permanentFailure, transientFailure;
	
	public CopyWire(Class<T> dataType, int noOfCopyPorts, boolean fair, CombinatorOrientation orientation) {
		this(dataType, noOfCopyPorts, fair, orientation, true);
	}
	
	public CopyWire(Class<T> dataType, int noOfCopyPorts, boolean fair, CombinatorOrientation orientation, boolean retryOnTransientFailure) {
		super(orientation);
		if(dataType == null) {
			throw new IllegalArgumentException("Copy Wire Data Type cannot be null");
		}
		if(noOfCopyPorts < 2) {
			throw new IllegalArgumentException("Copy Wire must have at least two copy ports");
		}
		this.dataType = dataType;
		this.noOfCopyPorts = noOfCopyPorts;
		this.retryOnTransientFailure = retryOnTransientFailure;
		mutexIn = new ReentrantLock(fair);
	}
	
	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public void accept(final Message<? extends T> msg)
					throws CombinatorFailureException {
				
				mutexIn.lock();
				try {
					// make sure failure flags are reset
					permanentFailure = false;
					transientFailure = false;
					final CountDownLatch copyStart = new CountDownLatch(1);
					final CountDownLatch copyComplete = new CountDownLatch(noOfCopyPorts);
					// start all copy runners (threads)
					for(int i=0; i<noOfCopyPorts; i++) {
						// create copy message by encapsulating the original one
						Message<T> copyMsg = (Message<T>) new Message<>(msg);
						CombinatorThreadPool.execute(new CopyRunner(copyMsg, i, copyStart, copyComplete));
					}
					// allow runners to execute when all wrappers around the original
					// mesage are initialised
					copyStart.countDown();
					// wait for all runners to complete
					copyComplete.await();
					// all done -> check for problems
					if(permanentFailure) {
						// permanent failure more important -> check first
						throw PERMANENT_EXCEPTION;
					}
					if(transientFailure) {
						// only possible with optimistic retry switched off
						throw TRANSIENT_EXCEPTION;
					}
				} catch (InterruptedException e) {
					// this shouldn't really happen
					msg.cancel(false);
					throw PERMANENT_EXCEPTION;
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
		
		private final CountDownLatch copyStart, copyComplete;
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
			try {
				copyStart.await();
				Backoff backoff = null;
				while(true) {
					try {
						sendRight(msg, portIndex);
						// success
						break;
					} catch(CombinatorTransientFailureException ex) {
						if(retryOnTransientFailure) {
							// back off and retry 
							if(backoff == null) {
								backoff = new Backoff();
							}
							backoff.backoff();
						} else {
							// re-throw the transient exception
							throw ex;
						}
					}
				}
			} catch (CombinatorPermanentFailureException | InterruptedException e) {
				// permanent failure
				permanentFailure = true;
			} catch (CombinatorTransientFailureException ex) {
				// only possible with optimistic retry off
				transientFailure = true;
			}
			copyComplete.countDown();
		}
	}

}
