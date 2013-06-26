package testing;

import uk.ac.soton.combinator.core.CombinationType;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.data.EliminationBound;
import uk.ac.soton.combinator.data.TreiberStack;
import uk.ac.soton.combinator.wire.PullAdaptorWire;
import uk.ac.soton.combinator.wire.PushAdaptorWire;

public class Main {

	public static void main(String[] args) {
	//	simpleOneToOne();
	//	simpleManyToOneWithPushAdaptor();
	//	simpleManyToOneWithPushAdaptor2();
	//	treiberStackWithMultipleProducersAndConsumers();
		boundedStackWithMultipleProducersAndConsumers();
	}
	
	private static void simpleOneToOne() {
		SimpleProducer p = new SimpleProducer(CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleConsumer c = new SimpleConsumer(CombinatorOrientation.LEFT_TO_RIGHT);
		p.combine(c, CombinationType.HORIZONTAL);
		new Thread(p).start();
	}
	
	private static void simpleManyToOneWithPushAdaptor() {
		SimpleProducer p1 = new SimpleProducer(CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleProducer p2 = new SimpleProducer(CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleProducer p3 = new SimpleProducer(CombinatorOrientation.LEFT_TO_RIGHT);
		PushAdaptorWire<Integer> adaptor = new PushAdaptorWire<>(Integer.class, 3, CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleConsumer c = new SimpleConsumer(CombinatorOrientation.LEFT_TO_RIGHT);
		p1.combine(p2, CombinationType.VERTICAL)
				.combine(p3, CombinationType.VERTICAL)
				.combine(adaptor, CombinationType.HORIZONTAL)
				.combine(c, CombinationType.HORIZONTAL);
//		p1.combine(c, CombinationType.HORIZONTAL);
		new Thread(p1).start();
		new Thread(p2).start();
		new Thread(p3).start();
	}
	
	private static void simpleManyToOneWithPushAdaptor2() {
		SimpleProducer p1 = new SimpleProducer(CombinatorOrientation.RIGHT_TO_LEFT);
		SimpleProducer p2 = new SimpleProducer(CombinatorOrientation.RIGHT_TO_LEFT);
		SimpleProducer p3 = new SimpleProducer(CombinatorOrientation.RIGHT_TO_LEFT);
		PushAdaptorWire<Integer> adaptor = new PushAdaptorWire<>(Integer.class, 3, CombinatorOrientation.RIGHT_TO_LEFT);
		SimpleConsumer c = new SimpleConsumer(CombinatorOrientation.RIGHT_TO_LEFT);
		Combinator producers = p1.combine(p2, CombinationType.VERTICAL).combine(p3, CombinationType.VERTICAL);
		c.combine(adaptor, CombinationType.HORIZONTAL).combine(producers, CombinationType.HORIZONTAL);
//		p1.combine(c, CombinationType.HORIZONTAL);
		new Thread(p1).start();
		new Thread(p2).start();
		new Thread(p3).start();
	}
	
	private static void treiberStackWithMultipleProducersAndConsumers() {
		SimpleProducer p1 = new SimpleProducer(CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleProducer p2 = new SimpleProducer(CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleProducer p3 = new SimpleProducer(CombinatorOrientation.LEFT_TO_RIGHT);
		
		PullConsumer<Integer> c1 = new PullConsumer<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		PullConsumer<Integer> c2 = new PullConsumer<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		PullConsumer<Integer> c3 = new PullConsumer<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		
		PushAdaptorWire<Integer> leftAdaptor = new PushAdaptorWire<>(Integer.class, 3, CombinatorOrientation.LEFT_TO_RIGHT);
		PullAdaptorWire<Integer> rightAdaptor = new PullAdaptorWire<>(Integer.class, 3, CombinatorOrientation.LEFT_TO_RIGHT);
		
		TreiberStack<Integer> stack = new TreiberStack<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		
		Combinator producers = p1.combine(p2, CombinationType.VERTICAL).combine(p3, CombinationType.VERTICAL);
		Combinator consumers = c1.combine(c2, CombinationType.VERTICAL).combine(c3, CombinationType.VERTICAL);
		
		producers.combine(leftAdaptor, CombinationType.HORIZONTAL)
				.combine(stack, CombinationType.HORIZONTAL)
				.combine(rightAdaptor, CombinationType.HORIZONTAL)
				.combine(consumers, CombinationType.HORIZONTAL);
		
		new Thread(p1).start();
		new Thread(p2).start();
		new Thread(p3).start();
		new Thread(c1).start();
		new Thread(c2).start();
		new Thread(c3).start();
	}
	
	private static void boundedStackWithMultipleProducersAndConsumers() {
		SimpleProducer p1 = new SimpleProducer(CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleProducer p2 = new SimpleProducer(CombinatorOrientation.LEFT_TO_RIGHT);
		SimpleProducer p3 = new SimpleProducer(CombinatorOrientation.LEFT_TO_RIGHT);
		
		PullConsumer<Integer> c1 = new PullConsumer<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		PullConsumer<Integer> c2 = new PullConsumer<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		PullConsumer<Integer> c3 = new PullConsumer<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		
		PushAdaptorWire<Integer> leftAdaptor = new PushAdaptorWire<>(Integer.class, 3, CombinatorOrientation.LEFT_TO_RIGHT);
		PullAdaptorWire<Integer> rightAdaptor = new PullAdaptorWire<>(Integer.class, 3, CombinatorOrientation.LEFT_TO_RIGHT);
		
		TreiberStack<Integer> stack = new TreiberStack<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		EliminationBound<Integer> bound = new EliminationBound<>(Integer.class, CombinatorOrientation.LEFT_TO_RIGHT);
		
		Combinator producers = p1.combine(p2, CombinationType.VERTICAL).combine(p3, CombinationType.VERTICAL);
		Combinator consumers = c1.combine(c2, CombinationType.VERTICAL).combine(c3, CombinationType.VERTICAL);
		Combinator boundedStack = bound.LeftBound.combine(stack, CombinationType.HORIZONTAL).combine(bound.RightBound,CombinationType.HORIZONTAL);
		
		producers.combine(leftAdaptor, CombinationType.HORIZONTAL)
				.combine(boundedStack, CombinationType.HORIZONTAL)
				.combine(rightAdaptor, CombinationType.HORIZONTAL)
				.combine(consumers, CombinationType.HORIZONTAL);
		
		new Thread(p1).start();
		new Thread(p2).start();
		new Thread(p3).start();
		new Thread(c1).start();
		new Thread(c2).start();
		new Thread(c3).start();
	}

}
