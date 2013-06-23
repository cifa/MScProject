package uk.ac.soton.combinator.core;

public abstract class PassiveOutPortHandler<T> implements PassivePortHandler<T> {

	@Override
	public final void acceptMsg(Message<?> msg) {
		throw new UnsupportedOperationException("Passive OUT port cannot accept messages");
	}

}
