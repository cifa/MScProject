package uk.ac.soton.combinator.core;

public interface MessageEventHandler<T> {
	
	void messageInvalidated(Message<T> message, T content);
	void messageFullyAcknowledged(Message<T> message);
}
