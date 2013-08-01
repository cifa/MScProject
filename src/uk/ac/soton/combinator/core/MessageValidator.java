package uk.ac.soton.combinator.core;

public interface MessageValidator<T> {

	@SuppressWarnings("unchecked")
	boolean validate(final T... contents);
}
