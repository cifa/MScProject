package testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.MessageEventHandler;
import uk.ac.soton.combinator.core.Port;

public class CallbackProducer extends Combinator implements Runnable, MessageEventHandler<Integer> {
	private Random rand = new Random();
	private int noOfMsgs;
	private final CountDownLatch endGate;
	public int failures;
	public int successes;
	private Thread executor;
	private volatile boolean messageHandled;
	
	public CallbackProducer(int noOfMsgs, CountDownLatch endGate, CombinatorOrientation orientation) {
		super(orientation);
		this.noOfMsgs = noOfMsgs;
		this.endGate = endGate;
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
		executor = Thread.currentThread();
		for (int i = 0; i < (noOfMsgs + failures); i++) {
			try {
				int content = rand.nextInt(2);
				System.out.println("Sending -> " + executor.getName() + " (" + content + ")");
				getRightBoundary().send(new Message<Integer>(Integer.class, content, this), 0);
//				System.out.println("gone through -> " + executor.getName());
			} catch (MessageFailureException ex) {
//				System.out.println(ex.getMessage() + " -> " + executor.getName());
			}
//			System.out.println("parking -> " + executor.getName());
			synchronized(this) {
				while(! messageHandled) {
					try {
						wait();
					} catch (InterruptedException e) {}
				}
				messageHandled = false;
			}
//			LockSupport.park(this);			
///			System.out.println("unparked -> " + executor.getName());
		}
		endGate.countDown();
	}

	@Override
	public void messageInvalidated(Message<Integer> message, Integer content) {
		failures++;
		System.out.println("F -> " + executor.getName() + " (" + content + ")");
		synchronized(this) {
			messageHandled = true;
			notifyAll();
		}
//		LockSupport.unpark(executor);
	}

	@Override
	public void messageFullyAcknowledged(Message<Integer> message) {
		successes++;
		System.out.println("S -> " + executor.getName()  + " (" + message.get() + ")");
		synchronized(this) {
			messageHandled = true;
			notifyAll();
		}
//		LockSupport.unpark(executor);
	}
}
