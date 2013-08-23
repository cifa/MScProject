package testing.Philosopher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorFailedCASException;
import uk.ac.soton.combinator.core.exception.CombinatorFailureException;
import uk.ac.soton.combinator.core.exception.CombinatorPermanentFailureException;

public class Fork extends Combinator {
	
	private static final Random rand = new Random();
	
	private final AtomicInteger holder;
	private final int id;
	
	public Fork(int id) {
		holder = new AtomicInteger();
		this.id = id;
	}
	
	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(Integer.class, new PassiveInPortHandler<Integer>() {
			// Philosophers can request the fork on port 0
			@Override
			public void accept(Message<? extends Integer> msg) throws CombinatorFailureException {
				if(rand.nextInt(1000) > 990) {
					System.out.println("Fork " + id + " CANCELLING message");
					msg.cancel(false);
				}
				try {
					if(!holder.compareAndSet(0, msg.get())) {
						throw new CombinatorFailedCASException("Phil " + msg.get() + " failed to " +
								"grab fork " + id);
					}
//					System.out.println("Fork " + id + " held by " + msg.get());
				} catch(CancellationException e) {
					System.out.println("Fork " + id + " detected CANCELLED message");
					throw new CombinatorPermanentFailureException();
				}
			}
		}));
		ports.add(Port.getPassiveInPort(Integer.class, new PassiveInPortHandler<Integer>() {
			// Philosophers can release the fork on port 1
			@Override
			public void accept(Message<? extends Integer> msg) {
				if(!holder.compareAndSet(msg.get(), 0)) {
					throw new CombinatorFailedCASException("Phil " + msg.get() + " failed to " +
							"return fork " + id);
				}
			}
		}));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		return Collections.emptyList();
	}

}
