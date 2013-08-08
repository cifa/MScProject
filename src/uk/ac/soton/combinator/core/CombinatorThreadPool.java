package uk.ac.soton.combinator.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CombinatorThreadPool {

	private final static ExecutorService ThreadPool = Executors.newCachedThreadPool();
	
	public static void execute(Runnable command) {
		ThreadPool.execute(command);
	}
	
	public static void shutdown() {
		ThreadPool.shutdownNow();
//		ThreadPool.shutdown();
	}
}
