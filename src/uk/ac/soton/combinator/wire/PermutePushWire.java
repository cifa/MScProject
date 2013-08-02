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
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;

/**
 * Initial attempt at permute wires - not very flexible
 * Use <code>PermuteWire</code> instead
 */
@Deprecated
public class PermutePushWire extends Combinator {
	
	private final int[] portPermutations;
	private final Class<?>[] portTypes;
	
	public PermutePushWire(Class<?>[] portTypes, int[] portPermutations, CombinatorOrientation orientation) {
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
		Field portDataType = null;
		try {
			portDataType = Port.class.getDeclaredField("portDataType");
		} catch (NoSuchFieldException | SecurityException e) {
			throw new BoundaryInitializationException("Couldn't get field 'portDataType' in class Port", e);
		}
		portDataType.setAccessible(true);
		List<Port<?>> ports = new ArrayList<Port<?>>();
		for(int i=0; i<portTypes.length; i++) {
			Port<Object> port = Port.getPassiveInPort(Object.class, new LeftPortHandler(portPermutations[i]));
			try {
				portDataType.set(port, portTypes[i]);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				throw new BoundaryInitializationException("Couldn't set field 'portDataType' in class Port", e);
			}
			ports.add(port);
		}
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		for(int i=0; i<portTypes.length; i++) {
			ports.add(Port.getActivePort(portTypes[portPermutations[i]], DataFlow.OUT));
		}
		return ports;
	}
	
	private class LeftPortHandler extends PassiveInPortHandler<Object> {
		
		final int portIndex;
		
		LeftPortHandler(int portIndex) {
			this.portIndex = portIndex;
		}
		
		@Override
		public void accept(Message<? extends Object> msg)
				throws MessageFailureException {
			getRightBoundary().send(msg, portIndex);
		}
	};

}
