package uk.ac.soton.combinator.core;


public abstract class PassiveInPortHandler<T> implements PassivePortHandler<T> {

	@Override
	public final Message<T> produce() {
		throw new UnsupportedOperationException("Passive IN port cannot produce messages");
	}

	@SuppressWarnings("unchecked")
	@Override
	public final void acceptMsg(Message<?> msg) {
		accept((Message<? extends T>) msg);
	}
	
	public abstract void accept(Message<? extends T> msg) throws MessageFailureException;

}
