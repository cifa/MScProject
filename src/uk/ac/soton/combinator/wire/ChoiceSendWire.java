package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.InvalidMessageSentException;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;

public class ChoiceSendWire<T> extends Combinator {
	
	private final Class<T> dataType;
	private final int noOfChoices;

	public ChoiceSendWire(Class<T> dataType, int noOfChoicePorts, CombinatorOrientation orientation) {
		super(orientation);
		if(dataType == null) {
			throw new IllegalArgumentException("Send Choice Wire Data Type cannot be null");
		}
		if(noOfChoicePorts < 2) {
			throw new IllegalArgumentException("Send Choice Wire must have at least two choice ports");
		}
		this.dataType = dataType;
		this.noOfChoices = noOfChoicePorts;
	}
	
	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {

			@Override
			public void accept(Message<? extends T> msg)
					throws MessageFailureException {
				int portIndex = 0;
				//TODO do we really wanna loop forever if all choices always fail?
				while(true) {
					// give up on cancelled messages
					if(msg.isCancelled()) {
						throw new InvalidMessageSentException();
					}
					try {
						// try to send the message on the current choice port
						getRightBoundary().send(msg, portIndex);
						// that's gone through -> return
						return;
					} catch(MessageFailureException ex) {
						// no luck on this port -> move to the next choice
						if(++portIndex == noOfChoices) {
							portIndex = 0;
						}
					}
				}
			}
		}));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		// add the required number of choices
		for(int i=0; i<noOfChoices; i++) {
			ports.add(Port.getActivePort(dataType, DataFlow.OUT));
		}
		return ports;
	}

}
