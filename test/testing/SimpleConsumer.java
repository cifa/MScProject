package testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;

public class SimpleConsumer extends Combinator {
	
	public SimpleConsumer(CombinatorOrientation orientation) {
		super(orientation);
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(Number.class, new PassiveInPortHandler<Number>() {

			@Override
			public void accept(Message<? extends Number> msg) {
				System.out.println(msg.getContent());
			}
		}));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		return Collections.emptyList();
	}

}
