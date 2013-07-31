package uk.ac.soton.combinator.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicStampedReference;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.MessageFailureException;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.RequestFailureException;

@Deprecated
public class EliminationArray<T> extends Combinator {
	
	private final static int EMPTY = 0;
	private final static int OFFERED = 1;
	private final static int TAKEN = 2;
	
	private final Class<T> dataType;
	private final AtomicStampedReference<T>[] slots;
	private final long timeout;
	private final Random rand;
//	private final AtomicInteger throughput;
	
	private final MessageFailureException msgEx = new MessageFailureException();
	private final RequestFailureException reqEx = new RequestFailureException();

	@SuppressWarnings("unchecked")
	public EliminationArray(Class<T> dataType, CombinatorOrientation orientation, 
			int arraySize, long timeoutInMilliseconds) {
		super(orientation);
		this.dataType = dataType;
		slots = (AtomicStampedReference<T>[]) new AtomicStampedReference<?>[arraySize];
		for(int i=0; i<arraySize; i++) {
			slots[i] = new AtomicStampedReference<T>(null, EMPTY);
		}
		timeout = timeoutInMilliseconds;
		rand = new Random();
//		throughput = new AtomicInteger();
	}
	
	public AtomicInteger in = new AtomicInteger();
	public AtomicInteger out = new AtomicInteger();
	
	@Override
	protected List<Port<?>> initLeftBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveInPort(dataType, new PassiveInPortHandler<T>() {

			@Override
			public void accept(Message<? extends T> msg) throws MessageFailureException {
				in.incrementAndGet();
				T offer = msg.get();
				long time = System.currentTimeMillis() + timeout;
				boolean offered = false;
				int slotIndex = -1;
				while(System.currentTimeMillis() < time) {
					if(!offered) {
						slotIndex = rand.nextInt(slots.length);
						// try to offer the message load
						offered = slots[slotIndex].compareAndSet(null, offer, EMPTY, OFFERED);
					} else {						
						if(slots[slotIndex].getStamp() == TAKEN) {
							// message load passed over successfully -> reset and return
							slots[slotIndex].compareAndSet(offer, null, TAKEN, EMPTY);
							out.incrementAndGet();
							return;
						}
					}
					Thread.yield();
				}
				// timeout - withdraw the offer and back off
				if(offered) {
					if(slots[slotIndex].compareAndSet(offer, null, OFFERED, EMPTY)) {
						throw msgEx;
					} else {
						// hmm, somebody must have taken it after all -> reset the slot and return
						slots[slotIndex].compareAndSet(offer, null, TAKEN, EMPTY);
						out.incrementAndGet();
					}
				} else {
					// didn't even manage to offer the message
					throw msgEx;
				}
			}
		}));
		return ports;
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getPassiveOutPort(dataType, new PassiveOutPortHandler<T>() {
			
			@Override
			public Message<T> produce() {
				long time = System.currentTimeMillis() + timeout;
				while(System.currentTimeMillis() < time) {
					int slotIndex = rand.nextInt(slots.length);
					T content = slots[slotIndex].getReference();
					// try to claim an offered value by changing the stamp
					if(content != null 
							&& slots[slotIndex].compareAndSet(content, content, OFFERED, TAKEN)) {
//						System.out.println("Elimination Array throughput: " + throughput.incrementAndGet());
						// offer taken -> pass it on in a new message
						return new Message<T>(dataType, content);			
					}
				}
				// no offer taken within the time limit -> back off
				throw reqEx;	
			}
		}));
		return ports;
	}

}
