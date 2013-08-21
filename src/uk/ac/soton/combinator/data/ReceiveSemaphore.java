package uk.ac.soton.combinator.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.RequestFailureException;

public class ReceiveSemaphore<T> extends AbstractSemaphore<T> {
	
	public ReceiveSemaphore(Class<T> dataType, int permits, CombinatorOrientation orientation) {
		super(dataType, new Semaphore(permits), orientation);
	}
	
	public ReceiveSemaphore(Class<T> dataType, AbstractSemaphore<T> linkedSemaphore, 
			CombinatorOrientation orientation) {
		super(dataType, linkedSemaphore.semaphore, orientation);
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveOutPort(dataType, new PassiveOutPortHandler<T>() {
			
			private final RequestFailureException ex = 
					new RequestFailureException("No Semaphore permission available");

			@SuppressWarnings("unchecked")
			@Override
			public Message<? extends T> produce() {
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
