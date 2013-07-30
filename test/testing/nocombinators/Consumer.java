package testing.nocombinators;

import java.util.concurrent.CountDownLatch;

import testing.IStack;

public class Consumer implements Runnable {

	private final IStack<Integer> stack;	
	private final int noOfMsgs;
	private final CountDownLatch startGate;
	private final CountDownLatch endGate;
	
	public Consumer(IStack<Integer> stack, int noOfMsgs, 
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
					Integer value = stack.pop();
					if(value == null) i--;
				}
			} finally {
				endGate.countDown();
			}
		} catch (InterruptedException ex) {}
	}

}
