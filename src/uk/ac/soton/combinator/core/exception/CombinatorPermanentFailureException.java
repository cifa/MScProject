package uk.ac.soton.combinator.core.exception;

public class CombinatorPermanentFailureException extends
		CombinatorFailureException {

	private static final long serialVersionUID = 3425738370815564198L;

	public CombinatorPermanentFailureException() {}
	
	public CombinatorPermanentFailureException(String msg) {
		super(msg);
	}
}
