package uk.ac.soton.combinator.core;

public class IncompatiblePortsException extends RuntimeException {

	private static final long serialVersionUID = -9003162101028703865L;

	public IncompatiblePortsException() {}
	
	public IncompatiblePortsException(String msg) {
		super(msg);
	}
}
