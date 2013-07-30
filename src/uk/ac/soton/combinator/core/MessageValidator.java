package uk.ac.soton.combinator.core;

public interface MessageValidator {

	boolean validate(final Message<?>... msgs);
}
