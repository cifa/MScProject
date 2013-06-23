package uk.ac.soton.combinator.core;

public class IllegalCombinationException extends RuntimeException {

	private static final long serialVersionUID = 1097714357309073174L;
	
	public IllegalCombinationException() {}
	
	public IllegalCombinationException(String msg) {
		super(msg);
	}

}
