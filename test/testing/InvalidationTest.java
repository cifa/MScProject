package testing;

import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageValidator;

public class InvalidationTest {

	public static void main(String[] args) {
		
		Message<Integer> msg = new Message<>(Integer.class, 5);
		MessageValidator validator = new MessageValidator() {
			
			@SuppressWarnings("unchecked")
			@Override
			public boolean validate(Message<?>... msgs) {
				msgs.toString();
				return ((Message<Integer>) msgs[0]).getContent() == 6;
			}
		};
		
		boolean valid = Message.validateMessageContent(validator, msg);
		System.out.println(valid);
	}

}
