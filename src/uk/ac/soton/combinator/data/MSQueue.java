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
	private final AtomicReference<Node<T>> head;
	private final AtomicReference<Node<T>> tail;
	
	public MSQueue(Class<T> queueDataType, CombinatorOrientation orientation) {
		super(orientation);
		dataType = queueDataType;
		head = new AtomicReference<Node<T>>(new Node<T>(null, null));
		tail = new AtomicReference<Node<T>>(head.get());
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {

			@Override
			public void accept(Message<? extends T> msg)
					throws MessageFailureException {
				Node<T> nTail = new Node<T>(msg.getContent(), null);
				while(true) {
					Node<T> cTail = tail.get();
					Node<T> cNext = cTail.next.get();
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
					Node<T> cHead = head.get();
					Node<T> cTail = tail.get();
					Node<T> cNext = cHead.next.get();
					if(cHead == head.get()) {
						if(cHead == cTail) {
							if(cNext == null) {
								throw ex;
							}
							tail.compareAndSet(cTail, cNext);
						} else {
							T value = cNext.value;
							if(head.compareAndSet(cHead, cNext)) {
								return new Message<T>(dataType, value);
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
