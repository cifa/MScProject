package uk.ac.soton.combinator.deprecated;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.MessageFailureException;
import uk.ac.soton.combinator.core.exception.RequestFailureException;

/**
 * Use Send and Receive Semaphores instead
 * @author Cifa
 */
@Deprecated
public class Mutex<T> {
	
	private AtomicBoolean busy;
	private final Class<T> dataType;
	
	public final Combinator LeftBound;
	public final Combinator RightBound;
	
	public Mutex(Class<T> dataType, CombinatorOrientation orientation) {
		this.dataType = dataType;
		this.busy = new AtomicBoolean();
		if(orientation == CombinatorOrientation.LEFT_TO_RIGHT) {
			LeftBound = new InBound(orientation);
			RightBound = new OutBound(orientation);
		} else {
			LeftBound = new OutBound(orientation);
			RightBound = new InBound(orientation);
		}
	}
	
	private class InBound extends Combinator {
		
		private final MessageFailureException ex = new MessageFailureException("Bound busy");
		
		public InBound(CombinatorOrientation orientation) {
			super(orientation);
		}

		@Override
		protected List<Port<?>> initLeftBoundary() {
			List<Port<?>> ports = new ArrayList<Port<?>>();
			ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {

				@Override
				public void accept(Message<? extends T> msg) {
					if(busy.compareAndSet(false, true)) {
						// we're in and can pass the message on
						try {
							sendRight(msg, 0);
						} finally {
							// make it available again
							busy.set(false);
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
	
	private class OutBound extends Combinator {
		
		private final RequestFailureException ex = new RequestFailureException("Bound busy");

		public OutBound(CombinatorOrientation orientation) {
			super(orientation);
		}
		
		@Override
		protected List<Port<?>> initLeftBoundary() {
			List<Port<?>> ports = new ArrayList<Port<?>>();
			ports.add(Port.getActivePort(dataType, DataFlow.IN));
			return ports;
		}

		@Override
		protected List<Port<?>> initRightBoundary() {
			List<Port<?>> ports = new ArrayList<Port<?>>();
			ports.add(Port.getPassiveOutPort(dataType, new PassiveOutPortHandler<T>() {

				@SuppressWarnings("unchecked")
				@Override
				public Message<? extends T> produce() {
					if(busy.compareAndSet(false, true)) {
						// we're in and can pass the message on
						try {
							return (Message<? extends T>) receiveLeft(0);
						} finally {
							// make it available again
							busy.set(false);
						}
					} else {
						throw ex;
					}
				}
			}));
			return ports;
		}
	}
}
