package testing;

import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Port;

public class Main {

	public static void main(String[] args) {
		Port<Integer> p1 = new Port<Integer>(Integer.class, DataFlow.OUT);
	}

}
