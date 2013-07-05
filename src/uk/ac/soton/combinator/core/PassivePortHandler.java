package uk.ac.soton.combinator.core;

interface PassivePortHandler<T> {
	void acceptMsg(Message<?> msg);
	Message<T> produce() throws RequestFailureException;
}
