package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorFailureException;
import uk.ac.soton.combinator.core.exception.CombinatorTransientFailureException;

public class SendSemaphoreWire<T> extends AbstractSemaphoreWire<T> {

	public SendSemaphoreWire(Class<T> dataType, int permits, CombinatorOrientation orientation) {
		super(dataType, new Semaphore(permits), orientation);
	}
	
	public SendSemaphoreWire(Class<T> dataType, AbstractSemaphoreWire<T> linkedSemaphore, 
			CombinatorOrientation orientation) {
		super(dataType, linkedSemaphore.semaphore, orientation);
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {
			
			private final CombinatorTransientFailureException ex = 
					new CombinatorTransientFailureException("No Semaphore permission available");

			@Override
			public void accept(Message<? extends T> msg) throws CombinatorFailureException {
				if(semaphore.tryAcquire()) {
					try {
						sendRight(msg, 0);
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
		ports.add(Port.getActivePort(dataType, DataFlow.OUT));
		return ports;
	}
}
