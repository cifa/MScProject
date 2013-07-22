package testing.nocombinators;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import testing.IStack;
import uk.ac.soton.combinator.core.Message;

public class MessageProducer implements Runnable  {

	private final static Random rand = new Random();
	
	private final IStack<Message<Integer>> stack;	
	private final int noOfMsgs;
	private final CountDownLatch startGate;
	private final CountDownLatch endGate;
	
	public MessageProducer(IStack<Message<Integer>> stack, int noOfMsgs, 
			CountDownLatch startGate, CountDownLatch endGate) {
		
		this.stack = stack;
		this.noOfMsgs = noOfMsgs;
		this.startGate = startGate;
		this.endGate = endGate;
	}
	@Override
	public void run() {
		try {
			startGate.await();
			try {
				for(int i=0; i<noOfMsgs; i++) {
					stack.push(new Message<Integer>(Integer.class, rand.nextInt(100)));
				}
			} finally {
				endGate.countDown();
			}
		} catch (InterruptedException ex) {}
	}

}
