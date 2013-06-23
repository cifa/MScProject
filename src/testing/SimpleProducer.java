package testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.Port;

public class SimpleProducer extends Combinator implements Runnable {
	
	private Random rand = new Random();

	@Override
	protected List<Port<?>> initLeftBoundary() {
		return Collections.emptyList();
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getActivePort(Integer.class, DataFlow.OUT));
		return ports;
	}

	@Override
	public void run() {
		for (int i = 0; i < 10; i++) {
			rightBoundary.send(new Message<Integer>(Integer.class, rand.nextInt(100)), 0);
		}
	}

}
