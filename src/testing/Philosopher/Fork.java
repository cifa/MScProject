package testing.Philosopher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;

public class Fork extends Combinator {
	
	private final AtomicInteger holder;
	private final MessageFailureException ex = new MessageFailureException();
	
	public Fork() {
		holder = new AtomicInteger();
	}
	
	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(Integer.class, new PassiveInPortHandler<Integer>() {
			// Philosophers can request the fork on port 0
			@Override
			public void accept(Message<? extends Integer> msg) {
				if(!holder.compareAndSet(0, msg.getContent())) {
					throw ex;
				}
			}
		}));
		ports.add(Port.getPassiveInPort(Integer.class, new PassiveInPortHandler<Integer>() {
			// Philosophers can release the fork on port 1
			@Override
			public void accept(Message<? extends Integer> msg) {
				if(!holder.compareAndSet(msg.getContent(), 0)) {
					throw ex;
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
