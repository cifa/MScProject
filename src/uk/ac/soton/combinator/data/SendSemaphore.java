package uk.ac.soton.combinator.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;

public class SendSemaphore<T> extends AbstractSemaphore<T> {

	public SendSemaphore(Class<T> dataType, int permits, CombinatorOrientation orientation) {
		super(dataType, new Semaphore(permits), orientation);
	}
	
	public SendSemaphore(Class<T> dataType, AbstractSemaphore<T> linkedSemaphore, 
			CombinatorOrientation orientation) {
		super(dataType, linkedSemaphore.semaphore, orientation);
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {
			
			private final MessageFailureException ex = 
					new MessageFailureException("No Semaphore permission available");

			@Override
			public void accept(Message<? extends T> msg) {
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
