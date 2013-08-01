package uk.ac.soton.combinator.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;
import java.util.concurrent.locks.LockSupport;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.RequestFailureException;

/**
 * Well-performing elimination exchanger inspired by the 
 * algorithm used in java.util.concurrent.Exchanger 
 */
public class EliminationExchanger2<T> extends Combinator {
	
	private final static int OFFERED = 0;
	private final static int TAKEN = 1;
	private final static int WITHDRAWN = 2;
	
	private static final int SIZE = 32;//(Runtime.getRuntime().availableProcessors() + 1);
	private static final int SPINS = (Runtime.getRuntime().availableProcessors() == 1) ? 0 : 2000;
	private final Class<T> dataType;
	private final AtomicReference<Node<Message<T>>>[] slots;
	
	private final AtomicInteger max = new AtomicInteger(1);
	
	private final MessageFailureException msgEx = new MessageFailureException();
	private final RequestFailureException reqEx = new RequestFailureException();
	
	@SuppressWarnings("unchecked")
	public EliminationExchanger2(Class<T> dataType, CombinatorOrientation orientation) {
		super(orientation);
		this.dataType = dataType;
		slots = (AtomicReference<Node<Message<T>>>[]) new AtomicReference<?>[SIZE];
		for(int i=0; i<SIZE; i++) {
			slots[i] = new AtomicReference<>();
		}
	}

	public AtomicInteger in = new AtomicInteger();
	public AtomicInteger inSuc = new AtomicInteger();
	
	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public void accept(Message<? extends T> msg)
					throws MessageFailureException {
				in.incrementAndGet();
				Node<Message<T>> node = exchange(new Node<Message<T>>((Message<T>) msg));
				if (node.getStamp() != TAKEN) {
					throw msgEx;
				}
				inSuc.incrementAndGet();
			}
		}));
		return ports;
	}
	
	public AtomicInteger out = new AtomicInteger();
	public AtomicInteger outSuc = new AtomicInteger();

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveOutPort(dataType, new PassiveOutPortHandler<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public Message<T> produce() throws RequestFailureException {
				out.incrementAndGet();
				Node<Message<T>> node = exchange(new Node<Message<T>>(null));
				if (node.getStamp() != TAKEN) {
					throw reqEx;
				}
				outSuc.incrementAndGet();
				return (Message<T>) node.getReference();
			}
		}));
		return ports;
	}
	
	private Node<Message<T>> exchange(Node<Message<T>> me) {
		int index = 0;
		int fails = 0;
		Node<Message<T>> you;

		while (true) {
			if ((you = slots[index].get()) != null
					&& ((me.value == null && you.value != null) || (me.value != null && you.value == null))
					&& you.compareAndSet(null, me.value, OFFERED, TAKEN)) {
				slots[index].compareAndSet(you, null);
				LockSupport.unpark(you.waiter);
				me.compareAndSet(null, you.value, OFFERED, TAKEN);
				break;
			} else if (you == null && slots[index].compareAndSet(null, me)) {
				await(me, index == 0);
				slots[index].compareAndSet(me, null);
				if(index == 0 || me.getStamp() == TAKEN) {
					break;
				} else {
					fails++;
					me = new Node<Message<T>>(me.value);
					int m = max.get();
					if (m > (index >>>= 1) && m > 1) {		
						max.compareAndSet(m, m - 1);  
					}	                    
				}
			} else if (++fails > 1) {
				Thread.yield();
				int m = max.get();				
				if (fails > 3 && m < SIZE && max.compareAndSet(m, m + 1)) {
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
		
		Node(T value) {
			super(null, OFFERED);
			this.value = value;
			this.waiter = Thread.currentThread();
		}
    }

}
