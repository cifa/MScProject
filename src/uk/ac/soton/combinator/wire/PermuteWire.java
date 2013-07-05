package uk.ac.soton.combinator.wire;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.soton.combinator.core.BoundaryInitializationException;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.ControlType;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.PortDefinition;
import uk.ac.soton.combinator.core.RequestFailureException;

public class PermuteWire extends Combinator {
	
	private final static int LEFT = 0;
	private final static int RIGHT = 1;
	
	private final PortDefinition<?>[] portsDefinitions;
	private final int[] portPermutations;
	
	public PermuteWire(PortDefinition<?>[] portsDefinitions, 
			int[] portPermutations, CombinatorOrientation orientation) {
		super(orientation);
		if(portsDefinitions == null || portsDefinitions.length == 0) {
			throw new IllegalArgumentException("At least one port definition required"); 
		}
		if(portPermutations == null || portsDefinitions.length != portPermutations.length) {
			throw new IllegalArgumentException("The number of port permutations must correspond to the number of port types");
		}
		this.portsDefinitions = Arrays.copyOf(portsDefinitions, portsDefinitions.length);
		this.portPermutations = Arrays.copyOf(portPermutations, portPermutations.length);
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
		for(int i=0; i<portsDefinitions.length; i++) {
			PortDefinition<?> def = portsDefinitions[i];
			if(def.getPortControlType() == ControlType.ACTIVE) {
				ports.add(Port.getActivePort(def.getPortDataType(), def.getPortDataFlow()));
			} else {
				Port<?> port;
				if(def.getPortDataFlow() == DataFlow.OUT) {
					port = Port.getPassiveOutPort(Object.class, 
							new PermutePassiveOutPortHandler(portPermutations[i], RIGHT));
				} else {
					port = Port.getPassiveInPort(Object.class, 
							new PermutePassiveInPortHandler(portPermutations[i], RIGHT));
				}
				try {
					portDataType.set(port, def.getPortDataType());
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new BoundaryInitializationException("Couldn't set field 'portDataType' in class Port", e);
				}
				ports.add(port);
			}
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
		for(int i=0; i<portsDefinitions.length; i++) {
			// find the corresponding port on the left
			int portIndex = 0;
			for(; portIndex<portPermutations.length; portIndex++) {
				if(portPermutations[portIndex] == i) break;
			}
			/*
			 * We need to create a complimentary port to the corresponding
			 * one on the left boundary. That means the same data type but
			 * opposite data flow and port control type
			 */
			PortDefinition<?> def = portsDefinitions[portIndex];
			if(def.getPortControlType() == ControlType.PASSIVE) {
				// Passive complemented by Active
				ports.add(Port.getActivePort(def.getPortDataType(), 
						DataFlow.getOposite(def.getPortDataFlow())));
			} else {
				// ... and vice versa
				Port<?> port;
				if(def.getPortDataFlow() == DataFlow.IN) {
					port = Port.getPassiveOutPort(Object.class, 
							new PermutePassiveOutPortHandler(portIndex, LEFT));
				} else {
					port = Port.getPassiveInPort(Object.class, 
							new PermutePassiveInPortHandler(portIndex, LEFT));
				}
				try {
					portDataType.set(port, def.getPortDataType());
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new BoundaryInitializationException("Couldn't set field 'portDataType' in class Port", e);
				}
				ports.add(port);
			}
		}
		return ports;
	}
	
	private class PermutePassiveOutPortHandler extends PassiveOutPortHandler<Object> {
		
		private final int portIndex;
		private final int side;
		
		PermutePassiveOutPortHandler(int portIndex, int side) {
			this.portIndex = portIndex;
			this.side = side;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Message<Object> produce() throws RequestFailureException {
			if(side == LEFT) {
				return (Message<Object>) getLeftBoundary().receive(portIndex);
			} else {
				return (Message<Object>) getRightBoundary().receive(portIndex);
			}
		}	
	};
	
	private class PermutePassiveInPortHandler extends PassiveInPortHandler<Object> {
		
		private final int portIndex;
		private final int side;
		
		PermutePassiveInPortHandler(int portIndex, int side) {
			this.portIndex = portIndex;
			this.side = side;
		}
		
		@Override
		public void accept(Message<? extends Object> msg)
				throws MessageFailureException {
			if(side == LEFT) {
				getLeftBoundary().send(msg, portIndex);
			} else {
				getRightBoundary().send(msg, portIndex);
			}
		}
	};

}
