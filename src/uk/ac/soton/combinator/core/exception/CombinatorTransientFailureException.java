package uk.ac.soton.combinator.core.exception;

public class CombinatorTransientFailureException extends
		CombinatorFailureException {

	private static final long serialVersionUID = -1485892935564073882L;
	
	public CombinatorTransientFailureException() {}
	
	public CombinatorTransientFailureException(String msg) {
		super(msg);
	}

}
