package testing;

import uk.ac.soton.combinator.core.CombinationType;

public class Main {

	public static void main(String[] args) {
		SimpleProducer p = new SimpleProducer();
		SimpleConsumer c = new SimpleConsumer();
		p.combine(c, CombinationType.HORIZONTAL);
		new Thread(p).start();
	}

}
