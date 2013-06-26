package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;

public class PullAdaptorWire<T> extends Combinator {
	
	private final Class<T> adaptorDataType;
	private final int noOfPullPorts;
	
	public PullAdaptorWire(Class<T> adaptorDataType, int noOfPullPorts, CombinatorOrientation orientation) {
		super(orientation);
		if(adaptorDataType == null) {
			throw new IllegalArgumentException("Adaptor Wire Data Type cannot be null");
		}
		if(noOfPullPorts < 1) {
			throw new IllegalArgumentException("Pull Adaptor Wire must have at least one pull port");
		}
		this.adaptorDataType = adaptorDataType;
		this.noOfPullPorts = noOfPullPorts;
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getActivePort(adaptorDataType, DataFlow.IN));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		PassiveOutPortHandler<T> outHandler = new PassiveOutPortHandler<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public Message<T> produce() {
				return (Message<T>) getLeftBoundary().receive(0);
			}
		};
		List<Port<?>> ports = new ArrayList<Port<?>>();
		for(int i=0; i<noOfPullPorts; i++) {
			ports.add(Port.getPassiveOutPort(adaptorDataType, outHandler));
		}
		return ports;
	}

}
