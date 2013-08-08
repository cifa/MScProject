package testing;

import java.util.concurrent.CountDownLatch;

import uk.ac.soton.combinator.core.CombinationType;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.CombinatorThreadPool;
import uk.ac.soton.combinator.core.MessageValidator;
import uk.ac.soton.combinator.wire.AdaptorPushWire;
import uk.ac.soton.combinator.wire.CopyWire;
import uk.ac.soton.combinator.wire.JoinPushWire;
import uk.ac.soton.combinator.wire.PermuteWire;
import uk.ac.soton.combinator.wire.SynchWire;

public class CallbackTest {
	
	private static MessageValidator<Integer> validator1 = new MessageValidator<Integer>() {
		
		@Override
		public boolean validate(Integer... contents) {
			return contents[0] % 2 == 0;
		}
	};
	
	private static MessageValidator<Integer> validator2 = new MessageValidator<Integer>() {
		
		@Override
		public boolean validate(Integer... contents) {
			return contents[0] % 3 == 0;
		}
	};

	public static void main(String[] args) throws InterruptedException {
		CountDownLatch endGate = new CountDownLatch(1);
		
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
		
		Combinator segment6 = new CopyWire<>(Integer.class, 2, false, CombinatorOrientation.LEFT_TO_RIGHT);
		
		CallbackActiveConsumer activeConsumer = new CallbackActiveConsumer(validator1, CombinatorOrientation.LEFT_TO_RIGHT);
		
		Combinator consumers = new SynchWire<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT, false)
				.combine(activeConsumer, CombinationType.HORIZONTAL)	
				.combine(new CallbackConsumer(validator2, CombinatorOrientation.LEFT_TO_RIGHT), CombinationType.VERTICAL);
		
		producers.combine(segment1, CombinationType.HORIZONTAL)
				.combine(segment2, CombinationType.HORIZONTAL)
				.combine(segment3, CombinationType.HORIZONTAL)
				.combine(segment4, CombinationType.HORIZONTAL)
				.combine(segment5, CombinationType.HORIZONTAL)
				.combine(segment6, CombinationType.HORIZONTAL)
				.combine(consumers, CombinationType.HORIZONTAL);
		
		Thread t1 = new Thread(p1, "producer-1");
		Thread t2 = new Thread(p2, "producer-2");
		Thread t3 = new Thread(p3, "producer-3");
		Thread t4 = new Thread(activeConsumer, "active consumer");
		t1.start();
		t2.start();
		t3.start();
		t4.start();
		
		endGate.await();
		
		activeConsumer.stop();
		
		t1.interrupt();
		t2.interrupt();
		t3.interrupt();
		
		System.out.println("Producer 1: " + p1.successes + " successes and " 
					+ p1.failures+ " failures");
		System.out.println("Producer 2: " + p2.successes + " successes and " 
				+ p2.failures+ " failures");
		System.out.println("Producer 3: " + p3.successes + " successes and " 
				+ p3.failures+ " failures");
		
		CombinatorThreadPool.shutdown();
	}

}
