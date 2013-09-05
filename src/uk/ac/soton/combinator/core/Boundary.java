package uk.ac.soton.combinator.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Ales Cirnfus
 *
 * This class represents one of the boundaries of a combinator
 * and provides methods to send and receive messages over specific
 * ports.
 * 
 * Note that once initialised the boundary become immutable
 */
public class Boundary {

	private final ArrayList<Port<?>> boundaryInterface;
	private AtomicBoolean initialized;
	
	Boundary() {
		this.boundaryInterface = new ArrayList<Port<?>>();
		this.initialized = new AtomicBoolean(false);
	}
	
	void send(Message<?> msg, int portNumber) {
		boundaryInterface.get(portNumber).send(msg);
	}
	
	Message<?> receive(int portNumber) {
		return boundaryInterface.get(portNumber).receive();
	}
	
	boolean setBoundaryInterface(List<Port<?>> ports) {
		if(initialized.compareAndSet(false, true)) {
			return boundaryInterface.addAll(ports);
		}
		return false;
	}
	
	List<Port<?>> getBoundaryInterface() {
		return new ArrayList<Port<?>>(boundaryInterface);
	}

	boolean isBoundaryInitialized() {
		return initialized.get();
	}
}
