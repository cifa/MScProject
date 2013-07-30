package uk.ac.soton.combinator.wire;

import java.util.Random;

class Backoff {
	
	private static final int NCPU = Runtime.getRuntime().availableProcessors();
	
	private long count = 0;
	private int maxBackoff = 15;
	private int minSpins = 20000;
	private Random rand;
	
	
	public Backoff() {
		rand = new Random(Thread.currentThread().getId());
	}
	
	public void backoff() throws InterruptedException {
		// no wait first time around - just yield
		if(count > 0) {
			// busy wait for multiple CPUs otherwise just sleep
			if(NCPU > 1) {
				long spins = (rand.nextInt(minSpins) + minSpins) * count * count * count;
				for( ; spins > 0; spins--) 
					;
			} else {
				Thread.sleep((rand.nextInt(2) + 1) * count);
			}
		}
		// hint that we are willing to give up the processor
		Thread.yield();
		count = Math.min(count+1, maxBackoff);
	}
	
	public void reset() {
		count = 0;
	}
}
