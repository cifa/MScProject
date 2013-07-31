package uk.ac.soton.combinator.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Exchanger;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.RequestFailureException;

@Deprecated
public class EliminationExchanger3<T> extends Combinator {
	
	private final Exchanger<T> exchanger;
	private final Class<T> dataType;
	
	private static final MessageFailureException msgEx = new MessageFailureException();
	private static final RequestFailureException reqEx = new RequestFailureException();
	
	public EliminationExchanger3(Class<T> dataType, CombinatorOrientation orientation) {
		super(orientation);
		this.dataType = dataType;
		this.exchanger = new Exchanger<>();
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {

			@Override
			public void accept(Message<? extends T> msg)
					throws MessageFailureException {
				try {
					if(exchanger.exchange(msg.get()) != null) {
						throw msgEx;
					}				
				} catch (InterruptedException e) {
					throw msgEx;
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
			public Message<T> produce() throws RequestFailureException {
				try {
					T value = exchanger.exchange(null);
					if(value != null) {
						return new Message<T>(dataType, value);
					}				
				} catch (InterruptedException e) {}
				throw reqEx;
			}
		}));
		return ports;
	}

}
