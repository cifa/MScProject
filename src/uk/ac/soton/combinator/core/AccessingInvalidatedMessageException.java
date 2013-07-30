package uk.ac.soton.combinator.core;

public class AccessingInvalidatedMessageException extends IllegalStateException {

	private static final long serialVersionUID = -1573718001273682316L;

	public AccessingInvalidatedMessageException() {}
	
	public AccessingInvalidatedMessageException(String msg) {
		super(msg);
	}
}
