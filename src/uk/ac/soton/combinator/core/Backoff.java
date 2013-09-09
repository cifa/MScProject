package uk.ac.soton.combinator.core;

import java.util.Random;

/**
 * @author Ales Cirnfus
 * 
 * This class implements a lightweight backo scheme which, 
 * depending on the number of available processors, uses either 
 * busy-wait (multi-core) or thread suspension (single-core) to wait
 * for a random period of time. The backoff period increases 
 * exponentially every time the backoff() method is called.
 */
public class Backoff {
	
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
