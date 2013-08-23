package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.PortDefinition;

public class ReverseWire extends AbstractUntypedWire {
	
	private final PortDefinition<?>[] portsDefinitions;
	
	public ReverseWire(PortDefinition<?>[] portsDefinitions, CombinatorOrientation orientation) {
		super(orientation);
		if(portsDefinitions == null || portsDefinitions.length == 0) {
			throw new IllegalArgumentException("At least one port definition required"); 
		}
		this.portsDefinitions = Arrays.copyOf(portsDefinitions, portsDefinitions.length);
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>(portsDefinitions.length * 2);
		// add the top ports first 
		for(int i=0; i<portsDefinitions.length; i++) {
			ports.add(getPort(portsDefinitions[i], Side.LEFT, portsDefinitions.length + i));
		}
		// ... and then add the complementary ones
		for(int i=0; i<portsDefinitions.length; i++) {
			ports.add(getComplementaryPort(portsDefinitions[i], Side.LEFT, i));
		}
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		return Collections.emptyList();
	}

}
