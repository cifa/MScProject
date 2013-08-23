package uk.ac.soton.combinator.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorEmptyCollectionException;
import uk.ac.soton.combinator.core.exception.CombinatorFailedCASException;

public class MSQueue<T> extends Combinator {
	
	private final Class<T> dataType;
	private final AtomicReference<Node<Message<? extends T>>> head;
	private final AtomicReference<Node<Message<? extends T>>> tail;
	
	public MSQueue(Class<T> queueDataType, CombinatorOrientation orientation) {
		super(orientation);
		dataType = queueDataType;
		head = new AtomicReference<>(new Node<Message<? extends T>>(null, null));
		tail = new AtomicReference<>(head.get());
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {
			
			private final CombinatorFailedCASException ex = new CombinatorFailedCASException();

			@Override
			public void accept(Message<? extends T> msg) throws CombinatorFailedCASException {
				Node<Message<? extends T>> nTail = new Node<Message<? extends T>>(msg, null);
				Node<Message<? extends T>> cTail = tail.get();
				Node<Message<? extends T>> cNext = cTail.next.get();
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
				throw ex;
			}
			
		}));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveOutPort(dataType, new PassiveOutPortHandler<T>() {
			
			private final CombinatorFailedCASException casEx = new CombinatorFailedCASException();
			private final CombinatorEmptyCollectionException emptyEx = 
					new CombinatorEmptyCollectionException("Empty queue");

			@Override
			public Message<? extends T> produce() 
					throws CombinatorFailedCASException, CombinatorEmptyCollectionException {
				
				Node<Message<? extends T>> cHead = head.get();
				Node<Message<? extends T>> cTail = tail.get();
				Node<Message<? extends T>> cNext = cHead.next.get();
				if(cHead == head.get()) {
					if(cHead == cTail) {
						if(cNext == null) {
							throw emptyEx;
						}
						tail.compareAndSet(cTail, cNext);
					} else {
						Message<? extends T> msg = cNext.value;
						if(head.compareAndSet(cHead, cNext)) {
							return msg;
						}
					}
				}
				throw casEx;
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
