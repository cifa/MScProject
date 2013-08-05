package testing;

import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessagePool;
import uk.ac.soton.combinator.core.MessageValidator;

public class InvalidationTest {
	
	private static final ScheduledExecutorService worker = 
				  Executors.newScheduledThreadPool(50);

	public static void main(String[] args) {
		
		final Message<Integer> msg1 = MessagePool.createMessage(Integer.class, 6);
		final Message<Integer> msg2 = MessagePool.createMessage(msg1);
		final Message<Integer> msg3 = MessagePool.createMessage(msg1);
		
		for(int i=0; i<20;i++) {
			worker.execute(new Runnable() {

				@Override
				public void run() {
					try {
						System.out.println("Msg2 run: " + msg2.get());
					} catch(CancellationException ex) {
						ex.printStackTrace();
					}
				}
				
			});
		}
		
		worker.schedule(new Runnable() {

			@Override
			public void run() {
				try {
					System.out.println("Msg3: " + msg3.get());
				} catch(CancellationException ex) {
					ex.printStackTrace();
				}
			}
			
		}, 3, TimeUnit.SECONDS);
		
		
		MessageValidator<Integer> validator = new MessageValidator<Integer>() {
			
			@Override
			public boolean validate(Integer... contents) {
				return contents[0] == 6;
			}
		};
		
		boolean valid = Message.validateMessageContent(validator, msg2);
		System.out.println(valid);
		try {
			System.out.println(msg2.get(1, TimeUnit.SECONDS));
		} catch (CancellationException | TimeoutException e) {
			e.printStackTrace();
		}
		try {
			System.out.println("2nd timeout: " + msg2.get(3, TimeUnit.SECONDS));
		} catch (CancellationException | TimeoutException e) {
			e.printStackTrace();
		}
	}

}
