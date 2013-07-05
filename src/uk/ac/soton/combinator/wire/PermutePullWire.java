package uk.ac.soton.combinator.wire;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.soton.combinator.core.BoundaryInitializationException;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.RequestFailureException;

public class PermutePullWire extends Combinator {
	
	private final int[] portPermutations;
	private final Class<?>[] portTypes;
	
	public PermutePullWire(Class<?>[] portTypes, int[] portPermutations, CombinatorOrientation orientation) {
		super(orientation);
		if(portTypes == null || portTypes.length == 0) {
			throw new IllegalArgumentException("At least one port type required"); 
		}
		if(portPermutations == null || portTypes.length != portPermutations.length) {
			throw new IllegalArgumentException("The number of port permutations must correspond to the number of port types");
		}
		this.portTypes = Arrays.copyOf(portTypes, portTypes.length);
		this.portPermutations = portPermutations;
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		for(int i=0; i<portTypes.length; i++) {
			ports.add(Port.getActivePort(portTypes[i], DataFlow.IN));
		}
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		Field portDataType = null;
		try {
			portDataType = Port.class.getDeclaredField("portDataType");
		} catch (NoSuchFieldException | SecurityException e) {
			throw new BoundaryInitializationException("Couldn't get field 'portDataType' in class Port", e);
		}
		portDataType.setAccessible(true);
		List<Port<?>> ports = new ArrayList<Port<?>>();
		for(int i=0; i<portTypes.length; i++) {
			Port<Object> port = Port.getPassiveOutPort(Object.class, new RightPortHandler(portPermutations[i]));
			try {
				portDataType.set(port, portTypes[portPermutations[i]]);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new BoundaryInitializationException("Couldn't set field 'portDataType' in class Port", e);
			}
			ports.add(port);
		}
		return ports;
	}

	private class RightPortHandler extends PassiveOutPortHandler<Object> {
		
		final int portIndex;
		
		RightPortHandler(int portIndex) {
			this.portIndex = portIndex;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Message<Object> produce() throws RequestFailureException {
			return (Message<Object>) getLeftBoundary().receive(portIndex);
		}	
	};
}
