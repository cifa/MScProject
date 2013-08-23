package uk.ac.soton.combinator.core.exception;

public class CombinatorFailedCASException extends
		CombinatorTransientFailureException {

	private static final long serialVersionUID = 4719345656392302748L;

	public CombinatorFailedCASException() {}
	
	public CombinatorFailedCASException(String msg) {
		super(msg);
	}
}
