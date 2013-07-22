package uk.ac.soton.combinator.core;

public enum CombinatorOrientation {
	LEFT_TO_RIGHT, RIGHT_TO_LEFT;
	
	public static CombinatorOrientation getOpposite(CombinatorOrientation orientation) {
		if(orientation == LEFT_TO_RIGHT) {
			return RIGHT_TO_LEFT;
		}
		return LEFT_TO_RIGHT;
	}
}
