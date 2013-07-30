package uk.ac.soton.combinator.core;

public class InvalidMessageSentException extends MessageFailureException {

	private static final long serialVersionUID = -4351334910277160916L;

	public InvalidMessageSentException() {}
	
	public InvalidMessageSentException(String msg) {
		super(msg);
	}
}
