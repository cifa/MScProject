package testing.nocombinators;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.LockSupport;

public class Eliminator<T> {
	
	private final static int OFFERED = 0;
	private final static int TAKEN = 1;
	private final static int WITHDRAWN = 2;
	
	private final static int PUSH = 0;
	private final static int POP = 1;
	
	private final static int SIZE = (Runtime.getRuntime().availableProcessors() + 1); //32;
	private final static int SPINS = (Runtime.getRuntime().availableProcessors() == 1) ? 0 : 2000;
	
	private final AtomicReference<Node<T>>[] slots;
	private final AtomicInteger max = new AtomicInteger(1);
	
	@SuppressWarnings("unchecked")
	public Eliminator() {
		slots = (AtomicReference<Node<T>>[]) new AtomicReference<?>[SIZE];
		for(int i=0; i<SIZE; i++) {
			slots[i] = new AtomicReference<>();
		}
	}
	
	public boolean offer(T value) {	
		return exchange(new Node<T>(value, PUSH)).getStamp() == TAKEN;
	}
	
	@SuppressWarnings("unchecked")
	public T get() {
		return (T) exchange(new Node<T>(null, POP)).getReference();
	}
	
	private Node<T> exchange(Node<T> me) {
		int index = 0;
		int fails = 0;
		Node<T> you;

		while (true) {
			if ((you = slots[index].get()) != null
					&& me.type != you.type
					&& you.compareAndSet(null, me.value, OFFERED, TAKEN)) {
				slots[index].compareAndSet(you, null);
				LockSupport.unpark(you.waiter);
				me.compareAndSet(null, you.value, OFFERED, TAKEN);
				break;
			} else if (you == null && slots[index].compareAndSet(null, me)) {
				await(me, index == 0);
				slots[index].compareAndSet(me, null);
				int m = max.get();
				if (m > (index >>>= 1) && m > 1) {		
					max.compareAndSet(m, m - 1);  
				}
				break;
			} else if (++fails > 1) {
				Thread.yield();
				int m = max.get();
				if(fails > m * 3) {
					break;
				} else if (fails > 3 && m < SIZE && max.compareAndSet(m, m + 1)) {
					index = m;
				} else if (--index < 0) {
					index = m - 1;
				}
			}
		}
		return me;
	}
	
	private static void await(Node<?> node, boolean timed) {
		boolean waited = false;
		int spins = SPINS;
        while(true) {
        	if(node.getStamp() == TAKEN) {
        		return;
        	} else if(spins-- > 0) {
        		// spin wait 
        	} else if(timed && ! waited) {
        		LockSupport.parkNanos(node.waiter, 3000000);
        		waited = true;
        	} else {
				node.compareAndSet(null, null, OFFERED, WITHDRAWN);
        		return;
        	}
        }
	}
	
	private static final class Node<T> extends AtomicStampedReference<Object> {  
		
		public final T value;
		public final Thread waiter;
		public final int type;
		
		Node(T value, int type) {
			super(null, OFFERED);
			this.value = value;
			this.waiter = Thread.currentThread();
			this.type = type;
		}
    }
}
