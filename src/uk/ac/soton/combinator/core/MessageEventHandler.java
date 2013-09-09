package uk.ac.soton.combinator.core;

/**
 * @author Ales Cirnfus
 * 
 * An implementation of this interface can be associated with a message
 * to receive a notification when the message status changes
 *
 * @param <T> type of the associated message
 */
public interface MessageEventHandler<T> {
	
	void messageInvalidated(Message<T> message, T content);
	void messageFullyAcknowledged(Message<T> message);
}
