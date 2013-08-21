package uk.ac.soton.combinator.core;

interface PassivePortHandler<T> {
	void acceptMsg(Message<?> msg);
	Message<? extends T> produce() throws RequestFailureException;
}
