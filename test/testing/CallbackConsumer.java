package testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageValidator;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;

public class CallbackConsumer extends Combinator {
	
	private final MessageValidator<Integer> validator;
	
	public CallbackConsumer(MessageValidator<Integer> validator, CombinatorOrientation orientation) {
		super(orientation);
		this.validator = validator;
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(Integer.class, new PassiveInPortHandler<Integer>() {

			@Override
			public void accept(Message<? extends Integer> msg) {
				if(Message.validateMessageContent(validator, msg)) {
					try {
						System.out.println(msg.get());
					} catch(CancellationException ex) {
//						System.out.println(ex.getMessage());
					}
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
