package uk.ac.soton.combinator.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.RequestFailureException;

public class EliminationExchanger<T> extends Combinator {
	
	private final static int OFFERED = 0;
	private final static int TAKEN = 1;
	private final static int WITHDRAWN = 2;
	
	private static final int SIZE = 256;//(Runtime.getRuntime().availableProcessors() + 1);
	private static final int SPINS = (Runtime.getRuntime().availableProcessors() == 1) ? 0 : 2000;
	private static final long BACKOFF_BASE = 128L;
	private static final Random rand = new Random();
	private final Class<T> dataType;
	private final AtomicReference<Node<T>>[] slots;
	
	private final AtomicInteger max = new AtomicInteger(1);
	
	private final MessageFailureException msgEx = new MessageFailureException();
	private final RequestFailureException reqEx = new RequestFailureException();
	
	@SuppressWarnings("unchecked")
	public EliminationExchanger(Class<T> dataType, CombinatorOrientation orientation) {
		super(orientation);
		this.dataType = dataType;
		slots = (AtomicReference<Node<T>>[]) new AtomicReference<?>[SIZE];
		for(int i=0; i<SIZE; i++) {
			slots[i] = new AtomicReference<>();
		}
	}
	
	public AtomicInteger in = new AtomicInteger();
	public AtomicInteger inSuc = new AtomicInteger();
	public AtomicInteger off = new AtomicInteger();

	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {
	
			@Override
			public void accept(Message<? extends T> msg)
					throws MessageFailureException {
				
				int index = 0;
				int fails = 0;
				Node<T> node = new Node<T>(msg.getContent());
				in.incrementAndGet();
				
				while(true) {
					if(slots[index].compareAndSet(null, node)) {
						off.incrementAndGet();
						await(node, randomDelay(fails));
						slots[index].compareAndSet(node, null);
						if(node.get() == TAKEN) {
							inSuc.incrementAndGet();
							return;
						} else {
							int m = max.get();
							if(index > 0 && m > 1) {
								max.compareAndSet(m, m - 1);
							}
							throw msgEx;
						}
					} else if (fails > SIZE) {
						throw msgEx;
					} else if(++fails > 1) {               
		                int m = max.get();
		                if (fails > 3 && m < SIZE && max.compareAndSet(m, m + 1)) {
		                	index = m-1;
		                } else {
		                	index = rand.nextInt(m);
		                }
		            }
				}
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

			@Override
			public Message<T> produce() throws RequestFailureException {
				out.incrementAndGet();
				Node<T> node;
				for(int i=0; i<max.get(); i++) {
					if((node = slots[i].get()) != null 
							&& node.compareAndSet(OFFERED, TAKEN)) {
						slots[i].compareAndSet(node, null);
						LockSupport.unpark(node.waiter);
						outSuc.incrementAndGet();
						return new Message<T>(dataType, node.value);
					}
				}
				throw reqEx;
			}
		}));
		return ports;
	}
	
	private static void await(Node<?> node, long nanos) {
		boolean waited = false;
		int spins = SPINS;
        while(true) {
        	if(node.get() == TAKEN) {
        		return;
        	} else if(spins-- > 0) {
        		// spin wait 
        	} else if(! waited) {
        		LockSupport.parkNanos(node.waiter, nanos);
        		waited = true;
        	} else {
				node.compareAndSet(OFFERED, WITHDRAWN);
        		return;
        	}
        }
	}
	
	private static long randomDelay(int fails) {
		return ((BACKOFF_BASE << fails) - 1) & rand.nextInt();
	}
	
	private static final class Node<T> extends AtomicReference<Integer> {  
		
		private static final long serialVersionUID = -5498638701290520216L;
		
		public final T value;
		public final Thread waiter;
		
		Node(T value) {
			super(OFFERED);
			this.value = value;
			this.waiter = Thread.currentThread();
		}
    }

}
