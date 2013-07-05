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

public class BackOffTreiberStack<T> extends Combinator {

	private final Class<T> stackDataType;
	private final AtomicReference<Node<T>> head;
	
	public BackOffTreiberStack(Class<T> stackDataType, CombinatorOrientation orientation) {
		super(orientation);
		this.stackDataType = stackDataType;
		this.head = new AtomicReference<Node<T>>();
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(stackDataType, new PassiveInPortHandler<T>() {

			private final MessageFailureException ex = new MessageFailureException();
			
			@Override
			public void accept(Message<? extends T> msg) {
				Node<T> newHead = new Node<T>(msg.getContent());
				Node<T> oldHead = head.get();
				newHead.next = oldHead;
				if(!head.compareAndSet(oldHead, newHead)) {
					throw ex;
				}
			}
		}));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveOutPort(stackDataType, new PassiveOutPortHandler<T>() {

			private final RequestFailureException ex = new RequestFailureException();
			
			@Override
			public Message<T> produce() {
				Node<T> curHead = head.get();
				if(curHead == null || !head.compareAndSet(curHead, curHead.next)) {
					throw ex;
				}
				return new Message<T>(stackDataType, curHead.value);
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
