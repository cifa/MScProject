package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;

import uk.ac.soton.combinator.core.Backoff;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorTransientFailureException;

public class AdaptorPullWire<T> extends Combinator {
	
	private final Class<T> adaptorDataType;
	private final int noOfPullPorts;
	private final boolean retryOnTransientFailure;
	
	public AdaptorPullWire(Class<T> adaptorDataType, int noOfPullPorts, CombinatorOrientation orientation) {
		this(adaptorDataType, noOfPullPorts, orientation, true);
	}
	
	public AdaptorPullWire(Class<T> adaptorDataType, int noOfPullPorts, 
			CombinatorOrientation orientation, boolean retryOnTransientFailure) {
		super(orientation);
		if(adaptorDataType == null) {
			throw new IllegalArgumentException("Adaptor Wire Data Type cannot be null");
		}
		if(noOfPullPorts < 1) {
			throw new IllegalArgumentException("Pull Adaptor Wire must have at least one pull port");
		}
		this.adaptorDataType = adaptorDataType;
		this.noOfPullPorts = noOfPullPorts;
		this.retryOnTransientFailure = retryOnTransientFailure;
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getActivePort(adaptorDataType, DataFlow.IN));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		PassiveOutPortHandler<T> outHandler = new PassiveOutPortHandler<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public Message<? extends T> produce() {
				Backoff backoff = null;
				while (true) {
					try {
						return (Message<? extends T>) receiveLeft(0);
					} catch (CombinatorTransientFailureException ex) {
						if(retryOnTransientFailure) {
							// back off and retry
							if(backoff == null) {
								backoff = new Backoff();
							}
							try {
								backoff.backoff();
							} catch (InterruptedException e) {
								// assoc msg must have been invalidated - clear flag
								Thread.interrupted();
							}
						} else {
							// allowed to fail transiently -> re-throw 
							throw ex;
						}
					}
				}
				
			}
		};
		List<Port<?>> ports = new ArrayList<Port<?>>();
		for(int i=0; i<noOfPullPorts; i++) {
			ports.add(Port.getPassiveOutPort(adaptorDataType, outHandler));
		}
		return ports;
	}

}
