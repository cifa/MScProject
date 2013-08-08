package testing.Philosopher;

import uk.ac.soton.combinator.core.CombinationType;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.CombinatorThreadPool;
import uk.ac.soton.combinator.wire.AdaptorPushWire;
import uk.ac.soton.combinator.wire.ChoiceSendWire;
import uk.ac.soton.combinator.wire.CopyWire;
import uk.ac.soton.combinator.wire.PermuteWire;

public class PhilosophersTest {

	public static void main(String[] args) throws InterruptedException {
		Philosopher phil1 = new Philosopher(1);
		Philosopher phil2 = new Philosopher(2);
		Philosopher phil3 = new Philosopher(3);
		Philosopher phil4 = new Philosopher(4);
		Philosopher phil5 = new Philosopher(5);
		
		Combinator p1 = getWiredPhilosopher(phil1);
		Combinator p2 = getWiredPhilosopher(phil2);
		Combinator p3 = getWiredPhilosopher(phil3);
		Combinator p4 = getWiredPhilosopher(phil4);
		Combinator p5 = getWiredPhilosopher(phil5);
		Combinator phils = p1.combine(p2, CombinationType.VERTICAL)
				.combine(p3, CombinationType.VERTICAL)
				.combine(p4, CombinationType.VERTICAL)
				.combine(p5, CombinationType.VERTICAL);
		Combinator f1 = getWiredFork(1);
		Combinator f2 = getWiredFork(2);
		Combinator f3 = getWiredFork(3);
		Combinator f4 = getWiredFork(4);
		Combinator f5 = getWiredFork(5);
		Combinator forks = f1.combine(f2, CombinationType.VERTICAL)
				.combine(f3, CombinationType.VERTICAL)
				.combine(f4, CombinationType.VERTICAL)
				.combine(f5, CombinationType.VERTICAL);
		
		PermuteWire philForksWiring = new PermuteWire(phils.getPortDefinitionCompatibleWithRightBoundary(),
				new int[] {1,4,3,6,5,8,7,10,9,12,11,14,13,16,15,18,17,0,19,2}, 
				CombinatorOrientation.LEFT_TO_RIGHT);
		
		phils.combine(philForksWiring, CombinationType.HORIZONTAL)
				.combine(forks, CombinationType.HORIZONTAL);
		
		new Thread(phil1, "Phil 1").start();
		new Thread(phil2, "Phil 2").start();
		new Thread(phil3, "Phil 3").start();
		new Thread(phil4, "Phil 4").start();
		new Thread(phil5, "Phil 5").start();
		
		System.out.println("Main sleeping");
		Thread.sleep(11000);
		System.out.println("Pool shutting down");
		// always shut down all threads
		CombinatorThreadPool.shutdown();
	}
	
	public static Combinator getWiredPhilosopher(Philosopher phil) {
		
		AdaptorPushWire<Integer> bottomSinglePush = new AdaptorPushWire<>(Integer.class, 1, 
				CombinatorOrientation.LEFT_TO_RIGHT);
		
		AdaptorPushWire<Integer> releaseDoublePush = new AdaptorPushWire<>(Integer.class, 2, 
				CombinatorOrientation.LEFT_TO_RIGHT); 
		
		CopyWire<Integer> releaseCopy = new CopyWire<>(Integer.class, 2, false,
				CombinatorOrientation.LEFT_TO_RIGHT);
		
		CopyWire<Integer> acquireCopy = new CopyWire<>(Integer.class, 2, false,
				CombinatorOrientation.LEFT_TO_RIGHT);
		
		ChoiceSendWire<Integer> choice = new ChoiceSendWire<>(Integer.class, 2,
				CombinatorOrientation.LEFT_TO_RIGHT);
		
		Combinator rightSide = acquireCopy.combine(
				(releaseDoublePush.combine(releaseCopy, CombinationType.HORIZONTAL)),
				CombinationType.VERTICAL);
		
		Combinator leftSide = phil.combine(
				(choice.combine(bottomSinglePush, CombinationType.VERTICAL)), 
				CombinationType.HORIZONTAL);
		
		return leftSide.combine(rightSide, CombinationType.HORIZONTAL);
	}
	
	public static Combinator getWiredFork(int id) {
		AdaptorPushWire<Integer> acquirePush = new AdaptorPushWire<>(Integer.class, 2, 
				CombinatorOrientation.LEFT_TO_RIGHT);
		AdaptorPushWire<Integer> releastPush = new AdaptorPushWire<>(Integer.class, 2, 
				CombinatorOrientation.LEFT_TO_RIGHT);
		Fork fork = new Fork(id);
		
		return acquirePush.combine(releastPush, CombinationType.VERTICAL)
				.combine(fork, CombinationType.HORIZONTAL);
	}

}
