package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.RequestFailureException;

public class ChoiceReceiveWire<T> extends Combinator {

	private final Class<T> dataType;
	private final int noOfChoices;

	public ChoiceReceiveWire(Class<T> dataType, int noOfChoicePorts, CombinatorOrientation orientation) {
		super(orientation);
		if(dataType == null) {
			throw new IllegalArgumentException("Receive Choice Wire Data Type cannot be null");
		}
		if(noOfChoicePorts < 2) {
			throw new IllegalArgumentException("Receive Choice Wire must have at least two choice ports");
		}
		this.dataType = dataType;
		this.noOfChoices = noOfChoicePorts;
	}
	
	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		// add the required number of choices
		for(int i=0; i<noOfChoices; i++) {
			ports.add(Port.getActivePort(dataType, DataFlow.IN));
		}
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveOutPort(dataType, new PassiveOutPortHandler<T>() {

			@Override
			public Message<T> produce() throws RequestFailureException {
				int portIndex = 0;
				//TODO do we really wanna loop forever if all choices always fail?
				while(true) {
					try {
						// try to receive a message on the current choice port
						@SuppressWarnings("unchecked")
						Message<T> msg = (Message<T>) getLeftBoundary().receive(portIndex);
//						System.out.println("Succeeded on port " + portIndex);
						// we've got a message -> return it
						return msg;
					} catch(RequestFailureException ex) {
						// no luck on this port -> move to the next choice
						if(++portIndex == noOfChoices) {
							portIndex = 0;
//							System.out.println("All choices failed");
						}
					}
				}
			}
		}));
		return ports;
	}

}
