package uk.ac.soton.combinator.core.exception;

public class CombinatorFailureException extends RuntimeException {

	private static final long serialVersionUID = -7319082058225417043L;

	public CombinatorFailureException() {}
	
	public CombinatorFailureException(String msg) {
		super(msg);
	}
}
