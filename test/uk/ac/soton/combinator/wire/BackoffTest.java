package uk.ac.soton.combinator.wire;

public class BackoffTest {

	public static void main(String[] args) throws InterruptedException {
		Backoff b = new Backoff();
		for(int i=0; i<20; i++) {
			long start = System.currentTimeMillis();
			b.backoff();
			System.out.println("Wait: " + (System.currentTimeMillis() - start));
		}
	}

}
