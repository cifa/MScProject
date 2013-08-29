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
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.exception.CombinatorTransientFailureException;

/**
 * Well-performing elimination exchanger inspired by the 
 * algorithm used in java.util.concurrent.Exchanger 
 */
public class EliminationExchanger<T> extends Combinator {
	
	private final static int OFFERED = 0;
	private final static int TAKEN = 1;
	private final static int WITHDRAWN = 2;
	
	private final static int PUSH = 0;
	private final static int POP = 1;
	
	private static final int SIZE = (Runtime.getRuntime().availableProcessors() + 1);
	private static final int SPINS = (Runtime.getRuntime().availableProcessors() == 1) ? 0 : 2000;
	private final Class<T> dataType;
	private final AtomicReference<Node<Message<? extends T>>>[] slots;
	
	private final AtomicInteger max = new AtomicInteger(1);
	
	private static final CombinatorTransientFailureException TRANSIENT_EXCEPTION = 
			new CombinatorTransientFailureException("Exchange failed");
	
	@SuppressWarnings("unchecked")
	public EliminationExchanger(Class<T> dataType, CombinatorOrientation orientation) {
		super(orientation);
		this.dataType = dataType;
		slots = (AtomicReference<Node<Message<? extends T>>>[]) new AtomicReference<?>[SIZE];
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

			@Override
			public void accept(Message<? extends T> msg)
					throws CombinatorTransientFailureException {
				in.incrementAndGet();
				Node<Message<? extends T>> node = exchange(new Node<Message<? extends T>>(msg, PUSH));
				if (node.getStamp() != TAKEN) {
					throw TRANSIENT_EXCEPTION;
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
			public Message<? extends T> produce() throws CombinatorTransientFailureException {
				out.incrementAndGet();
				Node<Message<? extends T>> node = exchange(new Node<Message<? extends T>>(null, POP));
				if (node.getStamp() != TAKEN) {
					throw TRANSIENT_EXCEPTION;
				}
				outSuc.incrementAndGet();
				return (Message<? extends T>) node.getReference();
			}
		}));
		return ports;
	}
	
	private Node<Message<? extends T>> exchange(Node<Message<? extends T>> me) {
		int index = 0;
		int fails = 0;
		Node<Message<? extends T>> you;

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
				if (m > (index >>> 1) && m > 1) {		
					max.compareAndSet(m, m - 1);  
				}
				break;
			} else if (++fails > 1) {
				Thread.yield();
				int m = max.get();
				if(fails > m * 3) {
					break;
				} else if (fails > 2 && m < SIZE && max.compareAndSet(m, m + 1)) {
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
	
	private static final class Node<V> extends AtomicStampedReference<Object> {  
		
		public final V value;
		public final Thread waiter;
		public final int type;
		
		Node(V value, int type) {
			super(null, OFFERED);
			this.value = value;
			this.waiter = Thread.currentThread();
			this.type = type;
		}
    }

}
