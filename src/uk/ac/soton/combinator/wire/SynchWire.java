package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorFailureException;
import uk.ac.soton.combinator.core.exception.CombinatorPermanentFailureException;
import uk.ac.soton.combinator.core.exception.CombinatorTransientFailureException;

public class SynchWire<T> extends Combinator {
	
	private static final CombinatorPermanentFailureException PERMANENT_EXCEPTION = 
			new CombinatorPermanentFailureException("Exchange message invalidated");
	
	private static final CombinatorTransientFailureException TRANSIENT_EXCEPTION =
			new CombinatorTransientFailureException("Exchange timeout");
	
	private final Class<T> dataType;
	private final int timeout;
	private final ReentrantLock mutexIn, mutexOut;
	private final Exchanger<Message<? extends T>> exchanger;
	
	public SynchWire(Class<T> dataType, CombinatorOrientation orientation, boolean fair) {
		this(dataType, orientation, fair, 0);
	}
	
	public SynchWire(Class<T> dataType, CombinatorOrientation orientation, boolean fair, int timeout) {
		super(orientation);
		this.dataType = dataType;
		this.timeout = timeout;
		mutexIn = new ReentrantLock(fair);
		mutexOut = new ReentrantLock(fair);
		exchanger = new Exchanger<>();
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {

			@Override
			public void accept(Message<? extends T> msg)
					throws CombinatorFailureException {
				
				mutexIn.lock();
				try {
					if(timeout > 0) {
						exchanger.exchange(msg, timeout, TimeUnit.MILLISECONDS);
					} else {
						exchanger.exchange(msg);
					}	
				} catch (InterruptedException ex) {
					// message has been invalidated -> permanent exception 
					throw PERMANENT_EXCEPTION;
				} catch (TimeoutException ex) {
					// timeout is a transient failure
					throw TRANSIENT_EXCEPTION;
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
		ports.add(Port.getPassiveOutPort(dataType, new PassiveOutPortHandler<T>() {
			
			@Override
			public Message<? extends T> produce() throws CombinatorFailureException {
				mutexOut.lock();
				try {
					Message<? extends T> msg;
					if(timeout > 0) {
						msg = exchanger.exchange(null, timeout, TimeUnit.MILLISECONDS);
					} else {
						msg = exchanger.exchange(null);
					}
					return msg;
				} catch (InterruptedException ex) {
					// this should not really happen
					throw new CombinatorPermanentFailureException();
				} catch (TimeoutException ex) {
					// timeout is a transient failure
					throw TRANSIENT_EXCEPTION;
				} finally{
					mutexOut.unlock();
				}
			}
		}));
		return ports;
	}

}
