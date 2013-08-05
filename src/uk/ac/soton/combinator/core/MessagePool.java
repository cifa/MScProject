package uk.ac.soton.combinator.core;

import java.util.concurrent.ConcurrentLinkedQueue;

public class MessagePool {

	private static ConcurrentLinkedQueue<Message<?>> msgPool = new ConcurrentLinkedQueue<>();
	
	public static <T> Message<T> createMessage(Class<T> messageDataType, T content) {
		return createMessage(messageDataType, content, null);	
	}
	
	
	public static <T> Message<T> createMessage(Class<T> messageDataType, T content, MessageEventHandler<T> messageCallback) {
		if(messageDataType == null) {
			throw new IllegalArgumentException("Message Data Type cannot be null");
		}
		@SuppressWarnings("unchecked")
		Message<T> msg = (Message<T>) msgPool.poll();
		if(msg == null) {
			msg = new Message<>();
		} else {
			msg.messageState.set(Message.ACTIVE);
		}		
		msg.messageDataType = messageDataType;
		msg.content = content;
		msg.messageCallback = messageCallback;
		return msg;	
	}
	
	@SafeVarargs
	public static <T> Message<T> createMessage(Message<T>... msgs) {
		Message<T> msg = createMessage(msgs[0].messageDataType, msgs[0].content, null);
		msg.encapsulatedMsgs = msgs;
		// Message wrapper is type safe
		msg.setTypeVerified(true);
		// assoc the encapsulated msgs with this wrapper
		boolean valid = true;
		for(Message<T> m : msgs) {
			m.addOuterWrapperMessage(msg);
			// has any of the encapsulated msgs just been cancelled?
			valid = valid && !m.isCancelled();
			// encapsulated msgs must be active
			m.messageState.set(Message.ACTIVE);
			// only top level msgs are carried
			m.currentCarrier = null;
			
		}
		// cannot build a valid msg from invalidated ones
		if(! valid) {
			msg.cancel(false);
		}
		return msg;	
	}
	
	public static void recycle(Message<?> msg) {
		msgPool.offer(msg);
	}
	
	public static int poolSize() {
		return msgPool.size();
	}
	
}