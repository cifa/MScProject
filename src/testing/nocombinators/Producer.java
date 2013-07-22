package testing.nocombinators;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import testing.IStack;

public class Producer implements Runnable {
	
	private final static Random rand = new Random();
	
	private final IStack<Integer> stack;	
	private final int noOfMsgs;
	private final CountDownLatch startGate;
	private final CountDownLatch endGate;
	
	public Producer(IStack<Integer> stack, int noOfMsgs, 
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
					stack.push(rand.nextInt(100));
				}
			} finally {
				endGate.countDown();
			}
		} catch (InterruptedException ex) {}
	}

}
