package uk.ac.soton.combinator.core;

/**
 * @author Ales Cirnfus
 * 
 * Implementations of this interface are used in invocations of
 * {@code public static <T> boolean validateMessageContent(final MessageValidator<T> validator, final Message<? extends T>... msgs )}
 * to provide a predicate under which the messages are deemed to be valid
 * 
 * @param <T> type of the validated messages
 */
public interface MessageValidator<T> {

	@SuppressWarnings("unchecked")
	boolean validate(final T... contents);
}
