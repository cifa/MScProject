package testing;

import java.util.concurrent.CountDownLatch;

import testing.nocombinators.Consumer;
import testing.nocombinators.Producer;
import uk.ac.soton.combinator.core.CombinationType;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.CombinatorThreadPool;
import uk.ac.soton.combinator.core.ControlType;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.PortDefinition;
import uk.ac.soton.combinator.data.TreiberStack;
import uk.ac.soton.combinator.data.EliminationExchanger;
import uk.ac.soton.combinator.data.MSQueue;
import uk.ac.soton.combinator.deprecated.EliminationArray;
import uk.ac.soton.combinator.deprecated.EliminationExchanger1;
import uk.ac.soton.combinator.deprecated.EliminationExchanger3;
import uk.ac.soton.combinator.deprecated.Mutex;
import uk.ac.soton.combinator.deprecated.TreiberStackOld;
import uk.ac.soton.combinator.wire.CopyWire;
import uk.ac.soton.combinator.wire.AdaptorPullWire;
import uk.ac.soton.combinator.wire.JoinPullWire;
import uk.ac.soton.combinator.wire.AdaptorPushWire;
import uk.ac.soton.combinator.wire.ChoiceReceiveWire;
import uk.ac.soton.combinator.wire.ChoiceSendWire;
import uk.ac.soton.combinator.wire.JoinPushWire;
import uk.ac.soton.combinator.wire.PermuteWire;
import uk.ac.soton.combinator.wire.ReceiveSemaphoreWire;
import uk.ac.soton.combinator.wire.ReverseWire;
import uk.ac.soton.combinator.wire.SendSemaphoreWire;
import uk.ac.soton.combinator.wire.SynchWire;

public class CombinatorTests {
	
	public static void main(String[] args) {
//		simpleOneToOne();
//		simpleManyToOneWithPushAdaptor();
//		simpleManyToOneWithPushAdaptor2();
		treiberStackTest(10, 1000000);
		treiberStackTest(10, 1000000);
		treiberStackTest(10, 1000000);
		treiberStackTest(10, 1000000);
		treiberStackTest(10, 1000000);
		treiberStackTest(10, 1000000);
//		System.out.println("-----------------------------------------------------------------");
//		eliminationStackWithSemaphoresAndExchangerTest(1000,10000);
//		eliminationStackWithSemaphoresAndExchangerTest(1000,10000);
//		eliminationStackWithSemaphoresAndExchangerTest(1000,10000);
//		eliminationStackWithSemaphoresAndExchangerTest(1000,10000);
//		eliminationStackWithSemaphoresAndExchangerTest(1000,10000);
//		eliminationStackWithSemaphoresAndExchangerTest(1000,10000);
		System.out.println("-----------------------------------------------------------------");
		eliminationBackOffStackTest(10, 1000000);
		eliminationBackOffStackTest(10, 1000000);
		eliminationBackOffStackTest(10, 1000000);
		eliminationBackOffStackTest(10, 1000000);
		eliminationBackOffStackTest(10, 1000000);
		eliminationBackOffStackTest(10, 1000000);
//		System.out.println("-----------------------------------------------------------------");
//		eliminationExchangerTest(1000, 10000);
//		eliminationExchangerTest(1000, 10000);
//		eliminationExchangerTest(1000, 10000);
//		eliminationExchangerTest(1000, 10000);
//		eliminationExchangerTest(1000, 10000);
//		eliminationExchangerTest(1000, 10000);
//		System.out.println("-----------------------------------------------------------------");
//		synchWireTest(100,100);
//		synchWireTestRigtToLeft(100,100);
//		copyWireTest(10, 100);
//		copyAndJoinWithTwoStacksTest(100, 1000);
//		copyAndJoinWithTwoQueuesTest(100, 1000);
//		permuteWirePortTest();
//		permuteWiresTest(10);
//		joinPushWireTest();
//		reverseWirePortTest();
//		eliminationStackComponentTest(1000, 10000);
//		eliminationStackComponentTest(1000, 10000);
//		eliminationStackComponentTest(1000, 10000);
//		eliminationStackComponentTest(1000, 10000);
//		eliminationStackComponentTest(1000, 10000);
//		eliminationStackComponentTest(1000, 10000);
		// always shut down all threads
		CombinatorThreadPool.shutdown();
	}


	private static void simpleOneToOne() {
		SimpleProducer p = new SimpleProducer(10, CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleConsumer c = new SimpleConsumer(CombinatorOrientation.LEFT_TO_RIGHT);
		p.combine(c, CombinationType.HORIZONTAL);
		new Thread(p).start();
	}

	private static void simpleManyToOneWithPushAdaptor() {
		SimpleProducer p1 = new SimpleProducer(10, CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleProducer p2 = new SimpleProducer(10, CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleProducer p3 = new SimpleProducer(10, CombinatorOrientation.LEFT_TO_RIGHT);
		AdaptorPushWire<Integer> adaptor = new AdaptorPushWire<>(Integer.class,
				3, CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleConsumer c = new SimpleConsumer(
				CombinatorOrientation.LEFT_TO_RIGHT);
		p1.combine(p2, CombinationType.VERTICAL)
				.combine(p3, CombinationType.VERTICAL)
				.combine(adaptor, CombinationType.HORIZONTAL)
				.combine(c, CombinationType.HORIZONTAL);
		new Thread(p1).start();
		new Thread(p2).start();
		new Thread(p3).start();
	}

	private static void simpleManyToOneWithPushAdaptor2() {
		SimpleProducer p1 = new SimpleProducer(10, CombinatorOrientation.RIGHT_TO_LEFT);
		SimpleProducer p2 = new SimpleProducer(10, CombinatorOrientation.RIGHT_TO_LEFT);
		SimpleProducer p3 = new SimpleProducer(10, CombinatorOrientation.RIGHT_TO_LEFT);
		AdaptorPushWire<Integer> adaptor = new AdaptorPushWire<>(Integer.class,
				3, CombinatorOrientation.RIGHT_TO_LEFT);
		SimpleConsumer c = new SimpleConsumer(
				CombinatorOrientation.RIGHT_TO_LEFT);
		Combinator producers = p1.combine(p2, CombinationType.VERTICAL)
				.combine(p3, CombinationType.VERTICAL);
		System.out.println(producers);
		System.out.println(adaptor);
		System.out.println(c);
		c.combine(adaptor, CombinationType.HORIZONTAL).combine(producers,
				CombinationType.HORIZONTAL);
		// p1.combine(c, CombinationType.HORIZONTAL);
		new Thread(p1).start();
		new Thread(p2).start();
		new Thread(p3).start();
	}

	private static void treiberStackTest(int noOfProducers, int msgsPerProducer) {
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(noOfProducers * 2);

		Combinator producers = initProducers(noOfProducers, msgsPerProducer, startGate, endGate);
		Combinator consumers = initPullConsumers(noOfProducers, msgsPerProducer, startGate, endGate);

		AdaptorPushWire<Integer> leftAdaptor = new AdaptorPushWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);
		AdaptorPullWire<Integer> rightAdaptor = new AdaptorPullWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);

		TreiberStack<Integer> stack = new TreiberStack<>(Integer.class,
				CombinatorOrientation.LEFT_TO_RIGHT);

		producers.combine(leftAdaptor, CombinationType.HORIZONTAL)
				.combine(stack, CombinationType.HORIZONTAL)
				.combine(rightAdaptor, CombinationType.HORIZONTAL)
				.combine(consumers, CombinationType.HORIZONTAL);

		// run the test
		try {
			// back off to let all other threads initialise
			Thread.sleep(100);
		} catch (InterruptedException e) {}

		long start = System.currentTimeMillis();
		startGate.countDown();
		try {
			endGate.await();
		} catch (InterruptedException ex) {
		}
		long end = System.currentTimeMillis();
		System.out.println("Treiber Stack execution time: " + (end - start)
				+ " ms");
	}
	
	private static void eliminationStackWithSemaphoresAndExchangerTest(int noOfProducers, int msgsPerProducer) {
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(noOfProducers * 2);

		Combinator producers = initProducers(noOfProducers, msgsPerProducer, startGate, endGate);
		Combinator consumers = initPullConsumers(noOfProducers, msgsPerProducer, startGate, endGate);

		AdaptorPushWire<Integer> leftAdaptor = new AdaptorPushWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);
		AdaptorPullWire<Integer> rightAdaptor = new AdaptorPullWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);

		TreiberStackOld<Integer> stack = new TreiberStackOld<>(Integer.class,
				CombinatorOrientation.LEFT_TO_RIGHT);
		SendSemaphoreWire<Integer> leftSemaphore = new SendSemaphoreWire<>(Integer.class, 
				8, CombinatorOrientation.LEFT_TO_RIGHT);
		ReceiveSemaphoreWire<Integer> rightSemaphore = new ReceiveSemaphoreWire<>(Integer.class,
				leftSemaphore, CombinatorOrientation.RIGHT_TO_LEFT);
		EliminationExchanger<Integer> eliminationExchanger = 
				new EliminationExchanger<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);

		ChoiceSendWire<Integer> sendChoice = new ChoiceSendWire<>(
				Integer.class, 2, CombinatorOrientation.LEFT_TO_RIGHT);
		ChoiceReceiveWire<Integer> receiveChoice = new ChoiceReceiveWire<>(
				Integer.class, 2, CombinatorOrientation.LEFT_TO_RIGHT);

		Combinator eliminationStack = leftSemaphore
				.combine(stack, CombinationType.HORIZONTAL)
				.combine(rightSemaphore, CombinationType.HORIZONTAL)
				.combine(eliminationExchanger, CombinationType.VERTICAL);

		producers.combine(leftAdaptor, CombinationType.HORIZONTAL)
				.combine(sendChoice, CombinationType.HORIZONTAL)
				.combine(eliminationStack, CombinationType.HORIZONTAL)
				.combine(receiveChoice, CombinationType.HORIZONTAL)
				.combine(rightAdaptor, CombinationType.HORIZONTAL)
				.combine(consumers, CombinationType.HORIZONTAL);

		// run the test
		try {
			// back off to let all other threads initialise
			Thread.sleep(100);
		} catch (InterruptedException e) {}

		long start = System.currentTimeMillis();
		startGate.countDown();
		try {
			endGate.await();
		} catch (InterruptedException ex) {
		}
		long end = System.currentTimeMillis();
		System.out.println("Elimination Stack with Semaphores and Exchanger execution time: " + (end - start)
				+ " ms");
		System.out.println("IN: " + eliminationExchanger.in.get());
//		System.out.println("IN Offered: " + eliminationExchanger.off.get());
		System.out.println("IN Success: " + eliminationExchanger.inSuc.get());
		System.out.println("OUT: " + eliminationExchanger.out.get());
		System.out.println("OUT Success: " + eliminationExchanger.outSuc.get());
	}

	private static void eliminationBackOffStackTest(int noOfProducers, int msgsPerProducer) {
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(noOfProducers * 2);

		Combinator producers = initProducers(noOfProducers, msgsPerProducer, startGate, endGate);
		Combinator consumers = initPullConsumers(noOfProducers, msgsPerProducer, startGate, endGate);

		AdaptorPushWire<Integer> leftAdaptor = new AdaptorPushWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);
		AdaptorPullWire<Integer> rightAdaptor = new AdaptorPullWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);

		TreiberStack<Integer> stack = new TreiberStack<>(
				Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		EliminationExchanger<Integer> eliminationExchanger = 
				new EliminationExchanger<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);

		ChoiceSendWire<Integer> sendChoice = new ChoiceSendWire<>(
				Integer.class, 2, CombinatorOrientation.LEFT_TO_RIGHT);
		ChoiceReceiveWire<Integer> receiveChoice = new ChoiceReceiveWire<>(
				Integer.class, 2, CombinatorOrientation.LEFT_TO_RIGHT);

		Combinator eliminationStack = stack.combine(eliminationExchanger,
				CombinationType.VERTICAL);

		producers.combine(leftAdaptor, CombinationType.HORIZONTAL)
				.combine(sendChoice, CombinationType.HORIZONTAL)
				.combine(eliminationStack, CombinationType.HORIZONTAL)
				.combine(receiveChoice, CombinationType.HORIZONTAL)
				.combine(rightAdaptor, CombinationType.HORIZONTAL)
				.combine(consumers, CombinationType.HORIZONTAL);

		// run the test
		try {
			// back off to let all other threads initialise
			Thread.sleep(500);
		} catch (InterruptedException e) {}

		long start = System.currentTimeMillis();
		startGate.countDown();
		try {
			endGate.await();
		} catch (InterruptedException ex) {
		}
		long end = System.currentTimeMillis();
		System.out
				.println("Elimination Stack execution time: "
						+ (end - start) + " ms");
		
//		System.out.println("IN: " + eliminationArray.in.get());
//		System.out.println("OUT: " + eliminationArray.out.get());
//		System.out.println("IN: " + eliminationExchanger.in.get());
//		System.out.println("IN Success: " + eliminationExchanger.inSuc.get());
//		System.out.println("OUT: " + eliminationExchanger.out.get());
//		System.out.println("OUT Success: " + eliminationExchanger.outSuc.get());
	}
	
	private static void eliminationExchangerTest(int noOfProducers, int msgsPerProducer) {
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(noOfProducers * 2);

		Combinator producers = initProducers(noOfProducers, msgsPerProducer, startGate, endGate);
		Combinator consumers = initPullConsumers(noOfProducers, msgsPerProducer, startGate, endGate);

		AdaptorPushWire<Integer> leftAdaptor = new AdaptorPushWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);
		AdaptorPullWire<Integer> rightAdaptor = new AdaptorPullWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);

		EliminationExchanger<Integer> eliminationExchanger = 
				new EliminationExchanger<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);


		producers.combine(leftAdaptor, CombinationType.HORIZONTAL)
				.combine(eliminationExchanger, CombinationType.HORIZONTAL)
				.combine(rightAdaptor, CombinationType.HORIZONTAL)
				.combine(consumers, CombinationType.HORIZONTAL);

		// run the test
		try {
			// back off to let all other threads initialise
			Thread.sleep(100);
		} catch (InterruptedException e) {}

		long start = System.currentTimeMillis();
		startGate.countDown();
		try {
			endGate.await();
		} catch (InterruptedException ex) {
		}
		long end = System.currentTimeMillis();
		System.out.println("Elimination Exchanger execution time: " + (end - start)
				+ " ms");
//		System.out.println("IN: " + eliminationExchanger.in.get());
////		System.out.println("IN Offered: " + eliminationExchanger.off.get());
//		System.out.println("IN Success: " + eliminationExchanger.inSuc.get());
//		System.out.println("OUT: " + eliminationExchanger.out.get());
//		System.out.println("OUT Success: " + eliminationExchanger.outSuc.get());
	}

	private static void synchWireTest(int noOfProducers, int msgsPerProducer) {
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(noOfProducers * 2);

		Combinator producers = null;
		Combinator consumers = null;

		for (int i = 0; i < noOfProducers; i++) {
			SimpleProducer prod = new SimpleProducer(
					msgsPerProducer, CombinatorOrientation.LEFT_TO_RIGHT);
			PullConsumer<Integer> cons = new PullConsumer<>(Integer.class,
					msgsPerProducer, CombinatorOrientation.LEFT_TO_RIGHT);
			if (i > 0) {
				producers = producers.combine(prod, CombinationType.VERTICAL);
				consumers = consumers.combine(cons, CombinationType.VERTICAL);
			} else {
				producers = prod;
				consumers = cons;
			}

			new LatchRunner(prod, startGate, endGate).start();
			new LatchRunner(cons, startGate, endGate).start();
		}

		AdaptorPushWire<Integer> leftAdaptor = new AdaptorPushWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);
		AdaptorPullWire<Integer> rightAdaptor = new AdaptorPullWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);
		SynchWire<Integer> synchWire = new SynchWire<>(Integer.class,
				CombinatorOrientation.LEFT_TO_RIGHT, false, 10);

		producers.combine(leftAdaptor, CombinationType.HORIZONTAL)
				.combine(synchWire, CombinationType.HORIZONTAL)
				.combine(rightAdaptor, CombinationType.HORIZONTAL)
				.combine(consumers, CombinationType.HORIZONTAL);

		// run the test
		try {
			// back off to let all other threads initialise
			Thread.sleep(500);
		} catch (InterruptedException e) {}

		long start = System.currentTimeMillis();
		startGate.countDown();
		try {
			endGate.await();
		} catch (InterruptedException ex) {
		}
		long end = System.currentTimeMillis();
		System.out.println("Synch Wire execution time: " + (end - start)
				+ " ms");
	}

	private static void synchWireTestRigtToLeft(int noOfProducers, int msgsPerProducer) {
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(noOfProducers * 2);

		Combinator producers = initProducers(noOfProducers, msgsPerProducer, startGate, 
				endGate, CombinatorOrientation.RIGHT_TO_LEFT);
		Combinator consumers = initPullConsumers(noOfProducers, msgsPerProducer, startGate, 
				endGate, CombinatorOrientation.RIGHT_TO_LEFT);

		AdaptorPushWire<Integer> rightAdaptor = new AdaptorPushWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.RIGHT_TO_LEFT);
		AdaptorPullWire<Integer> leftAdaptor = new AdaptorPullWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.RIGHT_TO_LEFT);
		SynchWire<Integer> synchWire = new SynchWire<>(Integer.class,
				CombinatorOrientation.RIGHT_TO_LEFT, false, 10);

		consumers.combine(leftAdaptor, CombinationType.HORIZONTAL)
				.combine(synchWire, CombinationType.HORIZONTAL)
				.combine(rightAdaptor, CombinationType.HORIZONTAL)
				.combine(producers, CombinationType.HORIZONTAL);

		// run the test
		try {
			// back off to let all other threads initialise
			Thread.sleep(100);
		} catch (InterruptedException e) {}

		long start = System.currentTimeMillis();
		startGate.countDown();
		try {
			endGate.await();
		} catch (InterruptedException ex) {
		}
		long end = System.currentTimeMillis();
		System.out.println("Synch Wire execution time: " + (end - start)
				+ " ms");
	}
	
	private static void copyWireTest(int noOfProducers, int msgsPerProducer) {
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(noOfProducers);

		Combinator producers = null;

		for (int i = 0; i < noOfProducers; i++) {
			SimpleProducer prod = new SimpleProducer(
					msgsPerProducer, CombinatorOrientation.LEFT_TO_RIGHT);
			if (i > 0) {
				producers = producers.combine(prod, CombinationType.VERTICAL);
			} else {
				producers = prod;
			}

			new LatchRunner(prod, startGate, endGate).start();
		}
		
		SimpleConsumer c1 = new SimpleConsumer(CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleConsumer c2 = new SimpleConsumer(CombinatorOrientation.LEFT_TO_RIGHT);
		Combinator consumers = c1.combine(c2, CombinationType.VERTICAL);

		AdaptorPushWire<Integer> leftAdaptor = new AdaptorPushWire<>(
				Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);
		CopyWire<Number> copyWire = new CopyWire<>(Number.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT);

		producers.combine(leftAdaptor, CombinationType.HORIZONTAL)
				.combine(copyWire, CombinationType.HORIZONTAL)
				.combine(consumers, CombinationType.HORIZONTAL);

		// run the test
		try {
			// back off to let all other threads initialise
			Thread.sleep(100);
		} catch (InterruptedException e) {}

		long start = System.currentTimeMillis();
		startGate.countDown();
		try {
			endGate.await();
		} catch (InterruptedException ex) {
		}
		long end = System.currentTimeMillis();
		System.out.println("Copy Wire execution time: " + (end - start)
				+ " ms");
	}
	
	private static void copyAndJoinWithTwoStacksTest(int noOfProducers, int msgsPerProducer) {
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(noOfProducers * 2);

		Combinator producers = initProducers(noOfProducers, msgsPerProducer, startGate, endGate);
		Combinator consumers = initPullConsumers(noOfProducers, msgsPerProducer, startGate, endGate);

		AdaptorPushWire<Integer> leftAdaptor = new AdaptorPushWire<>(Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);
		AdaptorPullWire<Integer> rightAdaptor= new AdaptorPullWire<>(Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);
		SendSemaphoreWire<Integer> sendSemaphore = new SendSemaphoreWire<>(Integer.class, 1, CombinatorOrientation.LEFT_TO_RIGHT);
		ReceiveSemaphoreWire<Integer> receiveSemaphore = new ReceiveSemaphoreWire<>(Integer.class, sendSemaphore, CombinatorOrientation.RIGHT_TO_LEFT);
		CopyWire<Integer> copyWire = new CopyWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT);
		JoinPullWire<Integer> joinWire = new JoinPullWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT, false);
		TreiberStack<Integer> s1 = new TreiberStack<Integer>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		TreiberStack<Integer> s2 = new TreiberStack<Integer>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		Combinator stacks = s1.combine(s2, CombinationType.VERTICAL);
		
		producers.combine(leftAdaptor, CombinationType.HORIZONTAL)
				.combine(sendSemaphore, CombinationType.HORIZONTAL)
				.combine(copyWire, CombinationType.HORIZONTAL)
				.combine(stacks, CombinationType.HORIZONTAL)
				.combine(joinWire, CombinationType.HORIZONTAL)
				.combine(receiveSemaphore, CombinationType.HORIZONTAL)
				.combine(rightAdaptor, CombinationType.HORIZONTAL)
				.combine(consumers, CombinationType.HORIZONTAL);

		// run the test
		try {
			// back off to let all other threads initialise
			Thread.sleep(100);
		} catch (InterruptedException e) {}

		long start = System.currentTimeMillis();
		startGate.countDown();
		try {
			endGate.await();
		} catch (InterruptedException ex) {
		}
		long end = System.currentTimeMillis();
		System.out.println("Copy and Join wires over 2 stacks execution time: " + (end - start)
				+ " ms");
	}
	
	private static void copyAndJoinWithTwoQueuesTest(int noOfProducers, int msgsPerProducer) {
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(noOfProducers * 2);

		Combinator producers = initProducers(noOfProducers, msgsPerProducer, startGate, endGate);
		Combinator consumers = initPullConsumers(noOfProducers, msgsPerProducer, startGate, endGate);

		AdaptorPushWire<Integer> leftAdaptor = new AdaptorPushWire<>(Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);
		AdaptorPullWire<Integer> rightAdaptor= new AdaptorPullWire<>(Integer.class, noOfProducers, CombinatorOrientation.LEFT_TO_RIGHT);
		CopyWire<Integer> copyWire = new CopyWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT);
		JoinPullWire<Integer> joinWire = new JoinPullWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT);
		MSQueue<Integer> q1 = new MSQueue<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		MSQueue<Integer> q2 = new MSQueue<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		Combinator queues = q1.combine(q2, CombinationType.VERTICAL);
		
		producers.combine(leftAdaptor, CombinationType.HORIZONTAL)
				.combine(copyWire, CombinationType.HORIZONTAL)
				.combine(queues, CombinationType.HORIZONTAL)
				.combine(joinWire, CombinationType.HORIZONTAL)
				.combine(rightAdaptor, CombinationType.HORIZONTAL)
				.combine(consumers, CombinationType.HORIZONTAL);

		// run the test
		try {
			// back off to let all other threads initialise
			Thread.sleep(100);
		} catch (InterruptedException e) {}

		long start = System.currentTimeMillis();
		startGate.countDown();
		try {
			endGate.await();
		} catch (InterruptedException ex) {
		}
		long end = System.currentTimeMillis();
		System.out.println("Copy and Join wires over 2 queues execution time: " + (end - start)
				+ " ms");
	}
	
	private static void permuteWirePortTest() {
		PortDefinition<?>[] defs = new PortDefinition<?>[4];
		defs[0] = new PortDefinition<>(Integer.class, DataFlow.IN, ControlType.PASSIVE);
		defs[1] = new PortDefinition<>(Number.class, DataFlow.OUT, ControlType.ACTIVE);
		defs[2] = new PortDefinition<>(Long.class, DataFlow.IN, ControlType.ACTIVE);
		defs[3] = new PortDefinition<>(Short.class, DataFlow.OUT, ControlType.PASSIVE);
		
		PermuteWire pw = new PermuteWire(defs, new int[] {1,3,0,2}, CombinatorOrientation.LEFT_TO_RIGHT);
		
		System.out.println(pw);
	}
	
	private static void reverseWirePortTest() {
		PortDefinition<?>[] defs = new PortDefinition<?>[4];
		defs[0] = new PortDefinition<>(Integer.class, DataFlow.IN, ControlType.PASSIVE);
		defs[1] = new PortDefinition<>(Number.class, DataFlow.OUT, ControlType.ACTIVE);
		defs[2] = new PortDefinition<>(Long.class, DataFlow.IN, ControlType.ACTIVE);
		defs[3] = new PortDefinition<>(Short.class, DataFlow.OUT, ControlType.PASSIVE);
		
		ReverseWire rw = new ReverseWire(defs, CombinatorOrientation.LEFT_TO_RIGHT);
		
		System.out.println(rw);
	}
	
	private static void permuteWiresTest(int msgsPerProducer) {
		final CountDownLatch startGate = new CountDownLatch(1);
		final CountDownLatch endGate = new CountDownLatch(6);

		Combinator producers = initProducers(2, msgsPerProducer, startGate, endGate);
		Combinator consumers = initPullConsumers(4, msgsPerProducer, startGate, endGate);
		
		CopyWire<Integer> cw1 = new CopyWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT);
		CopyWire<Number> cw2 = new CopyWire<>(Number.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT);
		Combinator copy = cw1.combine(cw2, CombinationType.VERTICAL);
		
//		PermutePushWire pwPush = new PermutePushWire(
//				new Class<?>[] {Integer.class, Integer.class, Number.class, Number.class},
//				new int[] {0,2,1,3}, CombinatorOrientation.LEFT_TO_RIGHT);
		
		PermuteWire pwPush = new PermuteWire(
				copy.getPortDefinitionCompatibleWithRightBoundary(), 
				new int[] {0,2,1,3}, CombinatorOrientation.LEFT_TO_RIGHT);
		
		SynchWire<Integer> sw1 = new SynchWire<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT, false);
		SynchWire<Number> sw2 = new SynchWire<>(Number.class, CombinatorOrientation.LEFT_TO_RIGHT, false);
		SynchWire<Integer> sw3 = new SynchWire<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT, false);
		SynchWire<Number> sw4 = new SynchWire<>(Number.class, CombinatorOrientation.LEFT_TO_RIGHT, false);
		Combinator synch = 
			 sw1.combine(sw2, CombinationType.VERTICAL)
				.combine(sw3, CombinationType.VERTICAL)
				.combine(sw4, CombinationType.VERTICAL);
		
		System.out.println(synch);
		
//		PermutePullWire pwPull = new PermutePullWire(
//				new Class<?>[] {Integer.class, Number.class, Integer.class, Number.class},
//				new int[] {3,1,2,0}, CombinatorOrientation.LEFT_TO_RIGHT);
		
//		PermuteWire pwPull = new PermuteWire(
//				synch.getPortDefinitionCompatibleWithRightBoundary(), 
//				new int[] {3,1,2,0}, CombinatorOrientation.LEFT_TO_RIGHT);
		
		PermuteWire pwPull = new PermuteWire(
				consumers.getPortDefinitionCompatibleWithLeftBoundary(), 
				new int[] {3,1,2,0}, CombinatorOrientation.RIGHT_TO_LEFT);
		
		System.out.println(pwPull);
		
		producers.combine(copy, CombinationType.HORIZONTAL)
				.combine(pwPush, CombinationType.HORIZONTAL)
				.combine(synch, CombinationType.HORIZONTAL)
				.combine(pwPull, CombinationType.HORIZONTAL)
				.combine(consumers, CombinationType.HORIZONTAL);
		
		// run the test
		try {
			// back off to let all other threads initialise
			Thread.sleep(100);
		} catch (InterruptedException e) {}

		long start = System.currentTimeMillis();
		startGate.countDown();
		try {
			endGate.await();
		} catch (InterruptedException ex) {}
		long end = System.currentTimeMillis();
				System.out.println("Permutation Wires execution time: " + (end - start)
						+ " ms");		
	}
	
	private static void joinPushWireTest() {
		SimpleProducer p1 = new SimpleProducer(10, CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleProducer p2 = new SimpleProducer(10, CombinatorOrientation.LEFT_TO_RIGHT);
		
		JoinPushWire<Integer> joinWire = new JoinPushWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT);
		
		SimpleConsumer c = new SimpleConsumer(CombinatorOrientation.LEFT_TO_RIGHT);
		
		p1.combine(p2, CombinationType.VERTICAL)
			.combine(joinWire, CombinationType.HORIZONTAL)
			.combine(c, CombinationType.HORIZONTAL);
		
		Thread t1 = new Thread(p1);
		Thread t2 = new Thread(p2);
		
		t1.start();
		t2.start();
		
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("P1 failures: " + p1.failures);
		System.out.println("P2 failures: " + p2.failures);
	}
	
	private static void eliminationStackComponentTest(int producers, int msgs) {
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
	}
	
	private static Combinator initProducers(int noOfProducers, int msgsPerProducer, 
			CountDownLatch startGate, CountDownLatch endGate) {
		
		return initProducers(noOfProducers, msgsPerProducer, startGate, 
				endGate, CombinatorOrientation.LEFT_TO_RIGHT);
	}
	
	private static Combinator initProducers(int noOfProducers, int msgsPerProducer, 
			CountDownLatch startGate, CountDownLatch endGate, CombinatorOrientation orientation) {
		Combinator producers = null;
		for (int i = 0; i < noOfProducers; i++) {
			SimpleProducer prod = new SimpleProducer(
					msgsPerProducer, orientation);
			if (i > 0) {
				producers = producers.combine(prod, CombinationType.VERTICAL);
			} else {
				producers = prod;
			}

			new LatchRunner(prod, startGate, endGate).start();
		}
		return producers;
	}
	
	private static Combinator initPullConsumers(int noOfProducers, int msgsPerProducer, 
			CountDownLatch startGate, CountDownLatch endGate) {
		
		return initPullConsumers(noOfProducers, msgsPerProducer, startGate, 
				endGate, CombinatorOrientation.LEFT_TO_RIGHT);
	}
	
	private static Combinator initPullConsumers(int noOfProducers, int msgsPerProducer, 
			CountDownLatch startGate, CountDownLatch endGate, CombinatorOrientation orientation) {
		Combinator consumers = null;
		for (int i = 0; i < noOfProducers; i++) {
			PullConsumer<?> cons;
			if (i<2) {
				cons = new PullConsumer<Number>(Number.class,
						msgsPerProducer, orientation);
			} else {
				cons = new PullConsumer<Integer>(Integer.class,
						msgsPerProducer, orientation);
			}
			if (i > 0) {
				consumers = consumers.combine(cons, CombinationType.VERTICAL);
			} else {
				consumers = cons;
			}

			new LatchRunner(cons, startGate, endGate).start();
		}
		return consumers;
	}

	private static class LatchRunner extends Thread {

		private final Runnable task;
		final CountDownLatch startGate;
		final CountDownLatch endGate;

		LatchRunner(Runnable task, CountDownLatch startGate,
				CountDownLatch endGate) {
			this.task = task;
			this.startGate = startGate;
			this.endGate = endGate;
		}

		@Override
		public void run() {
			try {
				startGate.await();
				try {
					task.run();
				} finally {
					endGate.countDown();
				}
			} catch (InterruptedException ex) {}
		}
	}
}
