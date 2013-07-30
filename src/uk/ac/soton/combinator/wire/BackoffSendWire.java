package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;

/**
 * Sending a message to the only port exposed by BackoffSendWire never succeeds.
 * Instead the executing thread backoffs for a random amount of time after which
 * a MessageFailureException is thrown. The backoff time grows exponentially each
 * time a specific message is pushed through the wire.
 */
public class BackoffSendWire<T> extends Combinator {

	private final Class<T> dataType;
	// make sure references to unreachable msgs are not kept
	private final WeakHashMap<Message<? extends T>, Backoff> backoffs;

	public BackoffSendWire(Class<T> dataType, CombinatorOrientation orientation) {
		super(orientation);
		this.dataType = dataType;
		backoffs = new WeakHashMap<>();
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {
			
				private final MessageFailureException ex = new MessageFailureException();

				@Override
				public void accept(Message<? extends T> msg)
						throws MessageFailureException {
					if(! backoffs.containsKey(msg)) {
						backoffs.put(msg, new Backoff());
					}
					try {
						backoffs.get(msg).backoff();
					} catch (InterruptedException e) {}
					throw ex;
				}
			}));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		return Collections.emptyList();
	}

}
