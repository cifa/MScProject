package uk.ac.soton.combinator.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Ales Cirnfus
 *
 * Threads are created when needed and reused if available. 
 * Used mainly combinators such as the Copy and Pull Join Wire, 
 * which execute multiple asynchronous tasks.
 */
public final class CombinatorThreadPool {

	private final static ExecutorService ThreadPool = Executors.newCachedThreadPool();
	
	public static void execute(Runnable command) {
		ThreadPool.execute(command);
	}
	
	public static void shutdown() {
		ThreadPool.shutdownNow();
	}
}
