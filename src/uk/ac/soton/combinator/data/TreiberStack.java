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
import uk.ac.soton.combinator.core.RequestFailureException;

public class TreiberStack<T> extends Combinator {
	
	private final Class<T> stackDataType;
	private final AtomicReference<Node<T>> head;
//	private final AtomicInteger size, total;
	
	public TreiberStack(Class<T> stackDataType, CombinatorOrientation orientation) {
		super(orientation);
		this.stackDataType = stackDataType;
		this.head = new AtomicReference<Node<T>>();
//		this.size = new AtomicInteger();
//		this.total = new AtomicInteger();
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(stackDataType, new PassiveInPortHandler<T>() {

			@Override
			public void accept(Message<? extends T> msg) {
				Node<T> newHead = new Node<T>(msg.getContent());
				Node<T> oldHead;
				do {
					oldHead = head.get();
					newHead.next = oldHead;
				} while(!head.compareAndSet(oldHead, newHead));
//				System.out.println("Stack size: " + size.incrementAndGet());
//				System.out.println("Stack total: " + total.incrementAndGet());
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
			public Message<T> produce() {
				Node<T> curHead;
				do {
					curHead = head.get();
					if(curHead == null) {
//						System.out.println("Stack size: " + size.get());
						throw ex;
					}
				} while(!head.compareAndSet(curHead, curHead.next));
//				System.out.println("Stack size: " + size.decrementAndGet());
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