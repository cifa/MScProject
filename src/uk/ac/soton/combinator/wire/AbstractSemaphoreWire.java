package uk.ac.soton.combinator.wire;

import java.util.concurrent.Semaphore;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;

public abstract class AbstractSemaphoreWire<T> extends Combinator {
	
	protected final Class<T> dataType;
	protected final Semaphore semaphore;

	public AbstractSemaphoreWire(Class<T> dataType, Semaphore semaphore, CombinatorOrientation orientation) {
		super(orientation);
		if(dataType == null) {
			throw new IllegalArgumentException("Send Semaphore Data Type cannot be null");
		}
		this.dataType = dataType;
		this.semaphore = semaphore;
	}
	
}
