package uk.ac.soton.combinator.deprecated;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.RequestFailureException;

public class TreiberStackOld<T> extends Combinator {
	
	private final Class<T> stackDataType;
	private final AtomicReference<Node<Message<? extends T>>> head;
	
	public TreiberStackOld(Class<T> stackDataType, CombinatorOrientation orientation) {
		super(orientation);
		this.stackDataType = stackDataType;
		this.head = new AtomicReference<Node<Message<? extends T>>>();
	}
	
	public AtomicInteger size = new AtomicInteger();

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(stackDataType, new PassiveInPortHandler<T>() {

			@Override
			public void accept(Message<? extends T> msg) {				
				Node<Message<? extends T>> newHead = new Node<Message<? extends T>>(msg);
				Node<Message<? extends T>> oldHead;
				do {
					oldHead = head.get();
					newHead.next = oldHead;
				} while(!head.compareAndSet(oldHead, newHead));
			}
		}));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveOutPort(stackDataType, new PassiveOutPortHandler<T>() {

			private final RequestFailureException ex = new RequestFailureException("Empty stack");
			
			@Override
			public Message<? extends T> produce() {
				Node<Message<? extends T>> curHead;
				do {
					curHead = head.get();
					if(curHead == null) {
						throw ex;
					}
				} while(!head.compareAndSet(curHead, curHead.next));
				return curHead.value;
			}
		}));
		return ports;
	}
	
	private static class Node<V> {
		final V value;
		Node<V> next;
		
		Node(V val) {
			value = val;
		}
	}
}
