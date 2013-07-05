package uk.ac.soton.combinator.core;

public enum ControlType {
	ACTIVE, PASSIVE;
	
	public static ControlType getOposite(ControlType ct) {
		if(ct == ACTIVE) {
			return PASSIVE;
		}
		return ACTIVE;
	}
}
