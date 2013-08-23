package uk.ac.soton.combinator.core.exception;

public class CombinatorEmptyCollectionException extends
		CombinatorTransientFailureException {

	private static final long serialVersionUID = -7216294402758153965L;

	public CombinatorEmptyCollectionException() {}
	
	public CombinatorEmptyCollectionException(String msg) {
		super(msg);
	}
}
