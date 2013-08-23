package uk.ac.soton.combinator.core.exception;

public class RequestFailureException extends RuntimeException {

	private static final long serialVersionUID = -6829935899230728930L;
	
	public RequestFailureException() {}
	
	public RequestFailureException(String msg) {
		super(msg);
	}

}
