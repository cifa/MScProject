package uk.ac.soton.combinator.core;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class MessagePool implements Runnable {

	public static AtomicInteger count = new AtomicInteger();
	private static Thread runner;
	private static ReferenceQueue<Message<?>> refQueue = new ReferenceQueue<>();
	private static ConcurrentLinkedQueue<Message<?>> msgPool = new ConcurrentLinkedQueue<>();
	private static ConcurrentLinkedQueue<Reference<Message<?>>> weakRefs = new ConcurrentLinkedQueue<>();
	
	static {
		new Thread(new MessagePool()).start();
	}
	
	public static void test(Message<?> msg) {
		weakRefs.add(new WeakReference<Message<?>>(msg, refQueue));
	}
	
	public static int poolSize() {
		return msgPool.size();
	}
	
	public static void shutdown() {
		runner.interrupt();
	}
	
	@Override
	public void run() {
		runner = Thread.currentThread();
		while(true) {
			try {
				Reference<? extends Message<?>> ref = refQueue.remove();
				
				Message<?> msg = ref.get();
//				weakRefs.remove(ref);
				if(msg == null) {
					count.incrementAndGet();
//					msgPool.offer(msg);
				}
			} catch (InterruptedException e) {
				break;
			}
		}
	}	
}