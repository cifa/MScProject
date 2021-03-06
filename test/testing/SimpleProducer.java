package testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorPermanentFailureException;

public class SimpleProducer extends Combinator implements Runnable {
	
	private Random rand = new Random();
	private final int noOfMsgs;
	public int failures;
	
	public SimpleProducer(int noOfMsgs, CombinatorOrientation orientation) {
		super(orientation);
		this.noOfMsgs = noOfMsgs;
	}

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
		for (int i = 0; i < noOfMsgs; i++) {
			try {
				sendRight(new Message<Integer>(Integer.class, rand.nextInt(100)), 0);
			} catch (CombinatorPermanentFailureException ex) {
				i--;
				failures++;
				System.out.println("producer backoff");
			}
		}
	}

}
