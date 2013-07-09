package testing.nocombinators;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class EliminationStack<T> {

	private final AtomicReference<Node<T>> head;
	private final Eliminator<T> eliminator;
	
	public AtomicInteger in = new AtomicInteger();
	public AtomicInteger inSuc = new AtomicInteger();
	public AtomicInteger out = new AtomicInteger();
	public AtomicInteger outSuc = new AtomicInteger();
	
	public EliminationStack() {
		this.head = new AtomicReference<>();
		this.eliminator = new Eliminator<>();
	}
	
	public void push(T value) {
		Node<T> newHead = new Node<T>(value);
		Node<T> oldHead;
		while(true) {
			oldHead = head.get();
			newHead.next = oldHead;
			if(head.compareAndSet(oldHead, newHead)) {
				break;
			}
			in.incrementAndGet();
			if(eliminator.offer(value)){
				inSuc.incrementAndGet();
				break;
			}
		}
	}
	
	public T pop() {	
		while(true) {
			Node<T> curHead = head.get();
//			if(curHead == null) {
//				return null;
//			}
			if(curHead != null && head.compareAndSet(curHead, curHead.next)) {
				return curHead.value;
			}
			out.incrementAndGet();
			T value = eliminator.get();
			if(value != null) {
				outSuc.incrementAndGet();
				return value;
			}
		}
	}
	
	private static class Node<T> {
		final T value;
		Node<T> next;
		
		Node(T val) {
			value = val;
		}
	}
}
