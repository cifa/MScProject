package uk.ac.soton.combinator.core;

public enum DataFlow {
	IN, OUT;
	
	public static DataFlow getOposite(DataFlow df) {
		if(df == IN) {
			return OUT;
		}
		return IN;
	}
}
