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

public class TreiberStack<T> extends Combinator {

	private final Class<T> stackDataType;
	private final AtomicReference<Node<Message<? extends T>>> head;
	
	public TreiberStack(Class<T> stackDataType, CombinatorOrientation orientation) {
		super(orientation);
		this.stackDataType = stackDataType;
		this.head = new AtomicReference<Node<Message<? extends T>>>();
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(stackDataType, new PassiveInPortHandler<T>() {

			private final CombinatorFailedCASException exCASFail = 
					new CombinatorFailedCASException();
			
			@Override
			public void accept(Message<? extends T> msg) {
				Node<Message<? extends T>> newHead = new Node<Message<? extends T>>(msg);
				Node<Message<? extends T>> oldHead = head.get();
				newHead.next = oldHead;
				if(!head.compareAndSet(oldHead, newHead)) {
					throw exCASFail;
				}
			}
		}));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveOutPort(stackDataType, new PassiveOutPortHandler<T>() {

			private final CombinatorEmptyCollectionException exEmpty = 
					new CombinatorEmptyCollectionException("Empty Stack");
			private final CombinatorFailedCASException exCASFail = 
					new CombinatorFailedCASException();
			
			@Override
			public Message<? extends T> produce() {
				Node<Message<? extends T>> curHead = head.get();
				if(curHead == null) {
					throw exEmpty;
				} else if(!head.compareAndSet(curHead, curHead.next)) {
					throw exCASFail;
				}
				return curHead.value;
			}
		}));
		return ports;
	}
	
	private static class Node<T> {
		final T value;
		Node<T> next;
		
		Node(T val) {
			value = val;
		}
	}
}
