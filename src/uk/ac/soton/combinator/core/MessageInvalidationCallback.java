package uk.ac.soton.combinator.core;

public interface MessageInvalidationCallback<T> {
	
	void messageInvalidated(Message<T> invalidatedMsg, T msgContent);
}
