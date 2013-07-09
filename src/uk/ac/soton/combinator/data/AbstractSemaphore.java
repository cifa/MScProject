package uk.ac.soton.combinator.data;

import java.util.concurrent.Semaphore;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;

public abstract class AbstractSemaphore<T> extends Combinator {
	
	protected final Class<T> dataType;
	protected final Semaphore semaphore;

	public AbstractSemaphore(Class<T> dataType, Semaphore semaphore, CombinatorOrientation orientation) {
		super(orientation);
		if(dataType == null) {
			throw new IllegalArgumentException("Send Semaphore Data Type cannot be null");
		}
		this.dataType = dataType;
		this.semaphore = semaphore;
	}
	
}
