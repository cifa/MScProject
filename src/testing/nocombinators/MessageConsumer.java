package testing.nocombinators;

import java.util.concurrent.CountDownLatch;

import uk.ac.soton.combinator.core.Message;

public class MessageConsumer implements Runnable {
	
	private final EliminationStack<Message<Integer>> stack;	
	private final int noOfMsgs;
	private final CountDownLatch startGate;
	private final CountDownLatch endGate;
	
	public MessageConsumer(EliminationStack<Message<Integer>> stack, int noOfMsgs, 
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
					Message<Integer> value = stack.pop();
					if(value == null) i--;
				}
			} finally {
				endGate.countDown();
			}
		} catch (InterruptedException ex) {}
	}
}
