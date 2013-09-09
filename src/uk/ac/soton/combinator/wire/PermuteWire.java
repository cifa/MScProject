package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.PortDefinition;

public class PermuteWire extends AbstractUntypedWire {

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
		
		List<Port<?>> ports = new ArrayList<Port<?>>();
		for(int i=0; i<portsDefinitions.length; i++) {
			ports.add(getPort(portsDefinitions[i], Side.RIGHT, portPermutations[i]));
		}
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		
		List<Port<?>> ports = new ArrayList<Port<?>>();
		for(int i=0; i<portsDefinitions.length; i++) {
			// find the corresponding port on the left
			int portIndex = 0;
			for(; portIndex<portPermutations.length; portIndex++) {
				if(portPermutations[portIndex] == i) break;
			}
			/*
			 * We need to create a complementary port to the corresponding
			 * one on the left boundary. That means the same data type but
			 * opposite data flow and port control type
			 */
			ports.add(getComplementaryPort(portsDefinitions[portIndex], Side.LEFT, portIndex));
		}
		return ports;
	}
}
