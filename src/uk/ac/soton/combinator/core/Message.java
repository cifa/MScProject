package uk.ac.soton.combinator.core;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;


public class Message<T> {

	private final Class<T> messageDataType;
	private final T content;
	private final MessageInvalidationCallback invalidationCallback;
	
	private volatile boolean typeVerified;
	private volatile Message<T>[] encapsulatedMsgs;
	private volatile Set<Message<T>> wrapperMsgs;
	
	private AtomicBoolean valid;
	

	public Message(Class<T> messageDataType, T content) {
		this(messageDataType, content, null);
	}
	
	public Message(Class<T> messageDataType, T content, MessageInvalidationCallback invalidationCallback) {
		if(messageDataType == null) {
			throw new IllegalArgumentException("Message Data Type cannot be null");
		}
		this.messageDataType = messageDataType;
		this.content = content;
		this.invalidationCallback = invalidationCallback;
		valid = new AtomicBoolean(true);
		setTypeVerified(false);
	}
	
	/**
	 * Constructor used by copy and join wires to wrap other messages
	 * @param msgs  arbitrary number of messages to wrap (at least one) 
	 */
	//TODO this shouldn't be public - package refactoring needed
	@SafeVarargs
	public Message(Message<T>... msgs) {
		// TODO do we copy the message content or set it to null?
		this(msgs[0].messageDataType, msgs[0].content);
		encapsulatedMsgs = msgs;
		// Message wrapper is type safe
		typeVerified = true;
	}

	public Class<T> getMessageDataType() {
		return messageDataType;
	}

	public T getContent() {
		return content;
	}
	
	public void invalidateMessage() {
		// we want to invalidate each message only once
		if(valid.compareAndSet(true, false)) {
			if(encapsulatedMsgs != null) {
				for(Message<T> msg : encapsulatedMsgs) {
					msg.invalidateMessage();
				}
			}
			if(invalidationCallback != null) {
				//TODO call invalidation method 
			}
			if(wrapperMsgs != null) {
				for(Message<T> msg : wrapperMsgs) {
					msg.invalidateMessage();
				}
			}
		}
	}
	
	boolean isValidMessage() {
		return valid.get();
	}

	boolean isTypeVerified() {
		return typeVerified;
	}
	
	boolean addOuterWrapperMessage(Message<T> wrapperMsg) {
		// initialize the wrapper set only when it's really needed
		if(wrapperMsgs == null) {
			synchronized (this) {
				if(wrapperMsgs == null) {
					wrapperMsgs = new HashSet<>();
				}
			}
		}
		return wrapperMsgs.add(wrapperMsg);
	}
	
	boolean removeOuterWrapperMessage(Message<T> wrapperMsg) {
		if(wrapperMsgs != null) {
			return wrapperMsgs.remove(wrapperMsg);
		}
		return false;
	}

	/*
	 * There is a single point of entry to the 'system', where the message
	 * type needs to be verified. Beyond that point the message type safety
	 * is ensured by the typed ports which must be correctly wired up  
	 */
	void setTypeVerified(boolean typeVerified) {
		this.typeVerified = typeVerified;
	}
	
	public boolean contentEquals(Message<T> other) {
		if(other != null) {
			if(content == null) {
				return other.content == null;
			} else {
				return content.equals(other.content);
			}
		}
		return false;
	}
}
