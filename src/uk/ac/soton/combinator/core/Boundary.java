package uk.ac.soton.combinator.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Boundary {

	private final CopyOnWriteArrayList<Port<?>> boundaryInterface;
	
	Boundary(List<Port<?>> ports) {
		this.boundaryInterface = new CopyOnWriteArrayList<Port<?>>(ports);
	}
	
	public void send(Message<?> msg, int portNumber) {
		boundaryInterface.get(portNumber).send(msg);
	}
	
	public Message<?> receive(int portNumber) {
		return boundaryInterface.get(portNumber).receive();
	}
	
	List<Port<?>> getBoundaryInterface() {
		return new ArrayList<Port<?>>(boundaryInterface);
	}
}
