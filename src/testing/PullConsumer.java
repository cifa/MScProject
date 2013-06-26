package testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.RequestFailureException;

public class PullConsumer<T> extends Combinator implements Runnable {
	
	private final Class<T> dataType;
	
	public PullConsumer(Class<T> dataType, CombinatorOrientation orientation) {
		super(orientation);
		if(dataType == null) {
			throw new IllegalArgumentException("Data Type cannot be null");
		}
		this.dataType = dataType;
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getActivePort(dataType, DataFlow.IN));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		return Collections.emptyList();
	}

	@Override
	public void run() {
		for (int i = 0; i < 3; i++) {	
			try {
				@SuppressWarnings("unchecked")
				Message<T> msg = (Message<T>) getLeftBoundary().receive(0);
				System.out.println(msg.getContent());
			} catch (RequestFailureException ex) {
				i--;
			}
		}
	}

}
