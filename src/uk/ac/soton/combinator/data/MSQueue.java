package uk.ac.soton.combinator.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.RequestFailureException;

public class MSQueue<T> extends Combinator {
	
	private final Class<T> dataType;
	private final AtomicReference<Node<Message<T>>> head;
	private final AtomicReference<Node<Message<T>>> tail;
	
	public MSQueue(Class<T> queueDataType, CombinatorOrientation orientation) {
		super(orientation);
		dataType = queueDataType;
		head = new AtomicReference<>(new Node<Message<T>>(null, null));
		tail = new AtomicReference<>(head.get());
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public void accept(Message<? extends T> msg)
					throws MessageFailureException {
				Node<Message<T>> nTail = new Node<Message<T>>((Message<T>) msg, null);
				while(true) {
					Node<Message<T>> cTail = tail.get();
					Node<Message<T>> cNext = cTail.next.get();
					if(cTail == tail.get()) {
						if(cNext == null) {
							if(cTail.next.compareAndSet(null, nTail)) {
								tail.compareAndSet(cTail, nTail);
								return;
							} 
						} else {
							tail.compareAndSet(cTail, cNext);
						}
					}
				}
			}
			
		}));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveOutPort(dataType, new PassiveOutPortHandler<T>() {
			
			private final RequestFailureException ex = new RequestFailureException("Empty queue");

			@Override
			public Message<T> produce() throws RequestFailureException {
				while(true) {
					Node<Message<T>> cHead = head.get();
					Node<Message<T>> cTail = tail.get();
					Node<Message<T>> cNext = cHead.next.get();
					if(cHead == head.get()) {
						if(cHead == cTail) {
							if(cNext == null) {
								throw ex;
							}
							tail.compareAndSet(cTail, cNext);
						} else {
							Message<T> msg = cNext.value;
							if(head.compareAndSet(cHead, cNext)) {
								return msg;
							}
						}
					}
				}
			}
		}));
		return ports;
	}
	
	private class Node<E> {
		final E value;
		final AtomicReference<Node<E>> next;
		
		Node(E value, Node<E> next) {
			this.value = value;
			this.next = new AtomicReference<>(next);
		}
		
		public String toString() {
			return "Val: " + value + next.get();
		}
	}

}
