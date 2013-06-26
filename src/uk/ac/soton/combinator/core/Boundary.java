package uk.ac.soton.combinator.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class Boundary {

	private final CopyOnWriteArrayList<Port<?>> boundaryInterface;
	private AtomicBoolean initialized;
	
	Boundary() {
		this.boundaryInterface = new CopyOnWriteArrayList<Port<?>>();
		this.initialized = new AtomicBoolean(false);
	}
	
	public void send(Message<?> msg, int portNumber) {
		boundaryInterface.get(portNumber).send(msg);
	}
	
	public Message<?> receive(int portNumber) {
		return boundaryInterface.get(portNumber).receive();
	}
	
	void setBoundaryInterface(List<Port<?>> ports) {
		if(initialized.compareAndSet(false, true)) {
			boundaryInterface.addAll(ports);
		}
	}
	
	List<Port<?>> getBoundaryInterface() {
		return new ArrayList<Port<?>>(boundaryInterface);
	}

	boolean isBoundaryInitialized() {
		return initialized.get();
	}
}
