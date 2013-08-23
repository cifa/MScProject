package uk.ac.soton.combinator.core.exception;

public class BoundaryInitializationException extends RuntimeException {

	private static final long serialVersionUID = -7117382919656586167L;

	public BoundaryInitializationException() {}
	
	public BoundaryInitializationException(String msg) {
		super(msg);
	}
	
	public BoundaryInitializationException(String msg, Throwable cause) {
        super(cause);
	}
}
