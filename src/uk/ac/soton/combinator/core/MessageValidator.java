package uk.ac.soton.combinator.core;

public interface MessageValidator<T> {

	boolean validate(final T... contents);
}
