package testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageValidator;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.RequestFailureException;

public class CallbackActiveConsumer extends Combinator implements Runnable {
	
	private volatile boolean stopped;
	private final MessageValidator<Integer> validator;
	private Thread executor;
	
	public CallbackActiveConsumer(MessageValidator<Integer> validator, 
			CombinatorOrientation orientation) {
		
		super(orientation);
		this.validator = validator;
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getActivePort(Integer.class, DataFlow.IN));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		return Collections.emptyList();
	}

	@Override
	public void run() {
		executor = Thread.currentThread();
		while(! stopped) {	
			try {
				@SuppressWarnings("unchecked")
				Message<Integer> msg = (Message<Integer>) getLeftBoundary().receive(0);
				if(Message.validateMessageContent(validator, msg)) {
					System.out.println(msg.get()); 
				}
			} catch(CancellationException | RequestFailureException ex) {
//					System.out.println(ex.getMessage());
			}
			
			
		}
	}
	
	public void stop() {
		stopped = true;
		executor.interrupt();
	}

}
