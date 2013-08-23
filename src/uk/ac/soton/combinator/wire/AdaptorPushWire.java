package uk.ac.soton.combinator.wire;

import java.util.ArrayList;
import java.util.List;

import uk.ac.soton.combinator.core.Backoff;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorTransientFailureException;

public class AdaptorPushWire<T> extends Combinator {
	
	private final Class<T> adaptorDataType;
	private final int noOfPushPorts;
	private final boolean optimisticRetry;
	
	public AdaptorPushWire(Class<T> adaptorDataType, int noOfPushPorts, CombinatorOrientation orientation) {
		this(adaptorDataType, noOfPushPorts, orientation, true);
	}
	
	public AdaptorPushWire(Class<T> adaptorDataType, int noOfPushPorts, 
			CombinatorOrientation orientation, boolean optimisticRetry) {
		super(orientation);
		if(adaptorDataType == null) {
			throw new IllegalArgumentException("Adaptor Wire Data Type cannot be null");
		}
		if(noOfPushPorts < 1) {
			throw new IllegalArgumentException("Push Adaptor Wire must have at least one push port");
		}
		this.adaptorDataType = adaptorDataType;
		this.noOfPushPorts = noOfPushPorts;
		this.optimisticRetry = optimisticRetry;
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		PassiveInPortHandler<T> inHandler = new PassiveInPortHandler<T>() {

			@Override
			public void accept(Message<? extends T> msg) {
				Backoff backoff = null;
				while(true) {
					try {
						sendRight(msg, 0);
						// success -> return
						break;
					} catch (CombinatorTransientFailureException ex) {
						if (optimisticRetry) {
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
		for(int i=0; i<noOfPushPorts; i++) {
			ports.add(Port.getPassiveInPort(adaptorDataType, inHandler));
		}
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getActivePort(adaptorDataType, DataFlow.OUT));
		return ports;
	}

}
