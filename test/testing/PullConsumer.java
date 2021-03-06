package testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorPermanentFailureException;
import uk.ac.soton.combinator.core.exception.RequestFailureException;

public class PullConsumer<T> extends Combinator implements Runnable {
	
	private final Class<T> dataType;
	private final int noOfMsgs;
	
	public PullConsumer(Class<T> dataType, int noOfMsg, CombinatorOrientation orientation) {
		super(orientation);
		if(dataType == null) {
			throw new IllegalArgumentException("Data Type cannot be null");
		}
		this.dataType = dataType;
		this.noOfMsgs = noOfMsg;
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
		for (int i = 0; i < noOfMsgs; i++) {	
			try {
				@SuppressWarnings("unchecked")
				Message<T> msg = (Message<T>) receiveLeft(0);
//				System.out.println(msg.get());
			} catch (CombinatorPermanentFailureException ex) {
				i--;
//				System.out.println(ex.getMessage());
			}
		}
	}

}
