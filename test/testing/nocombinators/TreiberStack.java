package testing.nocombinators;

import java.util.concurrent.atomic.AtomicReference;

import testing.IStack;


public class TreiberStack<T> implements IStack<T> {
	
	private final AtomicReference<Node<T>> head;
	
	public TreiberStack() {
		this.head = new AtomicReference<>();
	}
	
	public void push(T value) {
		Node<T> newHead = new Node<T>(value);
		Node<T> oldHead;
		do {
			oldHead = head.get();
			newHead.next = oldHead;
		} while(!head.compareAndSet(oldHead, newHead));
	}
	
	public T pop() {
		Node<T> curHead;
		do {
			curHead = head.get();
			if(curHead == null) {
				return null;
			}
		} while(!head.compareAndSet(curHead, curHead.next));
		return curHead.value;
	}
	
	private static class Node<T> {
		final T value;
		Node<T> next;
		
		Node(T val) {
			value = val;
		}
	}
}
