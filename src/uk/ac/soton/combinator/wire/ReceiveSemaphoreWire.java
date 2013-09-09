package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorFailureException;
import uk.ac.soton.combinator.core.exception.CombinatorTransientFailureException;

public class ReceiveSemaphoreWire<T> extends AbstractSemaphoreWire<T> {
	
	public ReceiveSemaphoreWire(Class<T> dataType, int permits, CombinatorOrientation orientation) {
		super(dataType, new Semaphore(permits), orientation);
	}
	
	public ReceiveSemaphoreWire(Class<T> dataType, AbstractSemaphoreWire<T> linkedSemaphore, 
			CombinatorOrientation orientation) {
		super(dataType, linkedSemaphore.semaphore, orientation);
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveOutPort(dataType, new PassiveOutPortHandler<T>() {
			
			private final CombinatorTransientFailureException ex = 
					new CombinatorTransientFailureException("No Semaphore permission available");

			@SuppressWarnings("unchecked")
			@Override
			public Message<? extends T> produce() throws CombinatorFailureException {
				if(semaphore.tryAcquire()) {
					try {
						return (Message<? extends T>) receiveRight(0);
					} finally {
						semaphore.release();
					}
				} else {
					throw ex;
				}
			}
		}));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getActivePort(dataType, DataFlow.IN));
		return ports;
	}

}
