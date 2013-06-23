package uk.ac.soton.combinator.core;

public class Message<T> {

	private final Class<T> messageDataType;
	private final T content;
	private volatile boolean typeVerified;

	public Message(Class<T> messageDataType, T content) {
		if(messageDataType == null) {
			throw new IllegalArgumentException("Message Data Type cannot be null");
		}
		this.messageDataType = messageDataType;
		this.content = content;
		setTypeVerified(false);
	}

	public Class<T> getMessageDataType() {
		return messageDataType;
	}

	public T getContent() {
		return content;
	}

	boolean isTypeVerified() {
		return typeVerified;
	}

	/*
	 * There is a single point of entry to the 'system', where the message
	 * type needs to be verified. Beyond that point the message type safety
	 * is ensured by the typed ports which must be correctly wired up  
	 */
	void setTypeVerified(boolean typeVerified) {
		this.typeVerified = typeVerified;
	}
}
