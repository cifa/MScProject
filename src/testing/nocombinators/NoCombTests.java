package testing.nocombinators;

import java.util.concurrent.CountDownLatch;

import uk.ac.soton.combinator.core.Message;

public class NoCombTests {

	public static void main(String[] args) {
		eliminationStackTest(1000, 10000);
		eliminationStackTest(1000, 10000);
		eliminationStackTest(1000, 10000);
		eliminationStackTest(1000, 10000);
		eliminationStackTest(1000, 10000);
		eliminationStackTest(1000, 10000);
		System.out.println("-----------------------------------------------------------------");
		eliminationStackMsgTest(1000, 10000);
		eliminationStackMsgTest(1000, 10000);
		eliminationStackMsgTest(1000, 10000);
		eliminationStackMsgTest(1000, 10000);
		eliminationStackMsgTest(1000, 10000);
		eliminationStackMsgTest(1000, 10000);
	}

	private static void eliminationStackTest(int producers, int msgs) {
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(producers * 2);

		EliminationStack<Integer> stack = new EliminationStack<>();

		for (int i = 0; i < producers; i++) {
			Producer p = new Producer(stack, msgs, startGate, endGate);
			Consumer c = new Consumer(stack, msgs, startGate, endGate);
			new Thread(p).start();
			new Thread(c).start();
		}

		// run the test
		try {
			// back off to let all other threads initialise
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		long start = System.currentTimeMillis();
		startGate.countDown();
		try {
			endGate.await();
		} catch (InterruptedException ex) {}
		long end = System.currentTimeMillis();
		System.out.println("Elimination Stack execution time: " + (end - start)
				+ " ms");
		System.out.println("IN: " + stack.in.get());
		System.out.println("IN Success: " + stack.inSuc.get());
		System.out.println("OUT: " + stack.out.get());
		System.out.println("OUT Success: " + stack.outSuc.get());
	}
	
	private static void eliminationStackMsgTest(int producers, int msgs) {
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(producers * 2);

		EliminationStack<Message<Integer>> stack = new EliminationStack<>();

		for (int i = 0; i < producers; i++) {
			MessageProducer p = new MessageProducer(stack, msgs, startGate, endGate);
			MessageConsumer c = new MessageConsumer(stack, msgs, startGate, endGate);
			new Thread(p).start();
			new Thread(c).start();
		}

		// run the test
		try {
			// back off to let all other threads initialise
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}

		long start = System.currentTimeMillis();
		startGate.countDown();
		try {
			endGate.await();
		} catch (InterruptedException ex) {}
		long end = System.currentTimeMillis();
		System.out.println("Elimination Stack execution time: " + (end - start)
				+ " ms");
		System.out.println("IN: " + stack.in.get());
		System.out.println("IN Success: " + stack.inSuc.get());
		System.out.println("OUT: " + stack.out.get());
		System.out.println("OUT Success: " + stack.outSuc.get());
	}
}
