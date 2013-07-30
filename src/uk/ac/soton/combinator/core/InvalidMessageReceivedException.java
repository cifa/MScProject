package uk.ac.soton.combinator.core;

public class InvalidMessageReceivedException extends RequestFailureException {

	private static final long serialVersionUID = -2255908908928571588L;

	public InvalidMessageReceivedException() {}
	
	public InvalidMessageReceivedException(String msg) {
		super(msg);
	}
}
