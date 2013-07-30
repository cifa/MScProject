package uk.ac.soton.combinator.core;

public class NotYetAcknowledgedException extends IllegalStateException {

	private static final long serialVersionUID = 8193039018399240084L;

	public NotYetAcknowledgedException() {}
	
	public NotYetAcknowledgedException(String msg) {
		super(msg);
	}
}
