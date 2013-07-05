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
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.RequestFailureException;

public class SynchWire<T> extends Combinator {
	
	private final Class<T> dataType;
	private final int timeout;
	private final ReentrantLock mutexIn, mutexOut;
	private final Exchanger<Message<T>> exchanger;
	
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
			
			private final MessageFailureException ex = new MessageFailureException();

			@SuppressWarnings("unchecked")
			@Override
			public void accept(Message<? extends T> msg)
					throws MessageFailureException {
				
				mutexIn.lock();
				try {
					if(timeout > 0) {
						exchanger.exchange((Message<T>) msg, timeout, TimeUnit.MILLISECONDS);
					} else {
						exchanger.exchange((Message<T>) msg);
					}	
				} catch (InterruptedException | TimeoutException e) {
					// no exchange -> fail
					throw ex;
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
			
			private final RequestFailureException ex = new RequestFailureException("Bound busy");

			@Override
			public Message<T> produce() throws RequestFailureException {
				mutexOut.lock();
				try {
					Message<T> msg;
					if(timeout > 0) {
						msg = exchanger.exchange(null, timeout, TimeUnit.MILLISECONDS);
					} else {
						msg = exchanger.exchange(null);
					}
					/*
					 * TODO if the getContent() can fail then we should delay the exit
					 * of the other thread involved in the exchange till the content is
					 * acquired successfully. If not the other thread should also throw 
					 * an exception
					 */
					return new Message<T>(dataType, msg.getContent());
				} catch (InterruptedException | TimeoutException e) {
					// no exchange -> fail
					throw ex;
				} finally{
					mutexOut.unlock();
				}
			}
		}));
		return ports;
	}

}
