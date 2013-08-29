package testing.Philosopher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageEventHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorPermanentFailureException;

public class Philosopher extends Combinator implements Runnable {
	
	private static final MessageEventHandler<Integer> callback = 
			new MessageEventHandler<Integer>() {

				@Override
				public void messageInvalidated(Message<Integer> message, Integer content) {
					System.out.println("Request from Philosopher " + content + " has been" +
							" cancelled -> he's gonna STARVE");	
				}

				@Override
				public void messageFullyAcknowledged(Message<Integer> message) {}
			};
	
	private PhilosopherState state = PhilosopherState.THINKING;
	private final int id;
	private Random rand = new Random();
	
	public Philosopher(int id) {
		this.id = id;
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		return Collections.emptyList();
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		// port 0 to request forks to eat
		ports.add(Port.getActivePort(Integer.class, DataFlow.OUT));
		// port 1 to release forks after eating
		ports.add(Port.getActivePort(Integer.class, DataFlow.OUT));
		return ports;
	}

	@Override
	public void run() {
		// run for 10 seconds
		long finish = System.currentTimeMillis() + 10000;
		while(System.currentTimeMillis() < finish) {
			if(state == PhilosopherState.THINKING) {
				System.out.println("Philosopher " + id + " is trying to get the forks");
				try {
					// grab the forks
					sendRight(new Message<Integer>(Integer.class, id), 0);
					state = PhilosopherState.EATING;
					System.out.println("Philosopher " + id + " has acquired both forks");
				} catch (CombinatorPermanentFailureException ex) {
					System.out.println("Philosopher " + id + " is STARVING");
				}
			} else {
				System.out.println("Philosopher " + id + " has finished EATING now");
				// return the forks
				sendRight(new Message<Integer>(Integer.class, id), 1);
				state = PhilosopherState.THINKING;
				System.out.println("Philosopher " + id + " has return the forks");
			}
			int wait = rand.nextInt(100);
			System.out.println("Philosopher " + id + " is " + state + " (for " + wait + " ms)");
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {}
		}
		// make sure you don't keep the forks when you leave
		if(state == PhilosopherState.EATING) {
			// return the forks
			sendRight(new Message<Integer>(Integer.class, id), 1);
		}
	}

}
