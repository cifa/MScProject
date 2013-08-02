package testing;

import java.util.concurrent.CountDownLatch;

import uk.ac.soton.combinator.core.CombinationType;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.CombinatorThreadPool;
import uk.ac.soton.combinator.wire.AdaptorPushWire;
import uk.ac.soton.combinator.wire.CopyWire;
import uk.ac.soton.combinator.wire.JoinPushWire;
import uk.ac.soton.combinator.wire.PermuteWire;

public class CallbackTest {

	public static void main(String[] args) throws InterruptedException {
		CountDownLatch endGate = new CountDownLatch(3);
		
		CallbackProducer p1 = new CallbackProducer(10, endGate, CombinatorOrientation.LEFT_TO_RIGHT);
		CallbackProducer p2 = new CallbackProducer(10, endGate, CombinatorOrientation.LEFT_TO_RIGHT);
		CallbackProducer p3 = new CallbackProducer(10, endGate, CombinatorOrientation.LEFT_TO_RIGHT);
		
		Combinator producers = p1.combine(p2, CombinationType.VERTICAL)
				.combine(p3, CombinationType.VERTICAL);
		
		Combinator segment1 = new CopyWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT)
				.combine(new CopyWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT), CombinationType.VERTICAL)
				.combine(new AdaptorPushWire<>(Integer.class, 1, CombinatorOrientation.LEFT_TO_RIGHT), CombinationType.VERTICAL);
		
		Combinator segment2 = new AdaptorPushWire<>(Integer.class, 1, CombinatorOrientation.LEFT_TO_RIGHT)
				.combine(new JoinPushWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT), CombinationType.VERTICAL)
				.combine(new JoinPushWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT), CombinationType.VERTICAL);
	
		Combinator segment3 = new PermuteWire(segment2.getPortDefinitionCompatibleWithRightBoundary(), 
				new int[] {1,0,2}, CombinatorOrientation.LEFT_TO_RIGHT);
		
		Combinator segment4 = new AdaptorPushWire<>(Integer.class, 1, CombinatorOrientation.LEFT_TO_RIGHT)
				.combine(new JoinPushWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT), CombinationType.VERTICAL);
		
		Combinator segment5 = new JoinPushWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT);
		
		Combinator consumer = new SimpleConsumer(CombinatorOrientation.LEFT_TO_RIGHT);
		
		producers.combine(segment1, CombinationType.HORIZONTAL)
				.combine(segment2, CombinationType.HORIZONTAL)
				.combine(segment3, CombinationType.HORIZONTAL)
				.combine(segment4, CombinationType.HORIZONTAL)
				.combine(segment5, CombinationType.HORIZONTAL)
				.combine(consumer, CombinationType.HORIZONTAL);
		
		new Thread(p1, "producer-1").start();
		new Thread(p2, "producer-2").start();
		new Thread(p3, "producer-3").start();
		
		endGate.await();
		
		System.out.println("Producer 1: " + p1.successes + " successes and " 
					+ p1.failures+ " failures");
		System.out.println("Producer 2: " + p2.successes + " successes and " 
				+ p2.failures+ " failures");
		System.out.println("Producer 3: " + p3.successes + " successes and " 
				+ p3.failures+ " failures");
		
		CombinatorThreadPool.shutdown();
	}

}
