package uk.ac.soton.combinator.core;

import uk.ac.soton.combinator.core.exception.CombinatorFailureException;


public abstract class PassiveInPortHandler<T> implements PassivePortHandler<T> {

	@Override
	public final Message<? extends T> produce() throws UnsupportedOperationException{
		throw new UnsupportedOperationException("Passive IN port cannot produce messages");
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void acceptMsg(Message<?> msg) throws CombinatorFailureException {
		accept((Message<? extends T>) msg);
	}
	
	public abstract void accept(Message<? extends T> msg) throws CombinatorFailureException;

}
