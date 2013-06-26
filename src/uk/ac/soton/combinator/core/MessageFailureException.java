package uk.ac.soton.combinator.core;

public class MessageFailureException extends RuntimeException {

	private static final long serialVersionUID = -2012050000958549367L;

	public MessageFailureException() {}
	
	public MessageFailureException(String msg) {
		super(msg);
	}
}
