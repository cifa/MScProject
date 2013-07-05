package uk.ac.soton.combinator.core;

public class Port<T> {
	
	private final Class<T> portDataType;
	private final DataFlow portDataFlow;
	private final ControlType portControlType;
	private final PassivePortHandler<T> handler;
	private volatile Port<?> connectedTo;
	
	public static <T> Port<T> getActivePort(Class<T> portDataType, DataFlow portDataFlow) {
		return new Port<T>(portDataType, portDataFlow, ControlType.ACTIVE, null);
	}
	
	public static <T> Port<T> getPassiveInPort(Class<T> portDataType, PassiveInPortHandler<T> handler) {
		return new Port<T>(portDataType, DataFlow.IN, ControlType.PASSIVE, handler);
	}
	
	public static <T> Port<T> getPassiveOutPort(Class<T> portDataType, PassiveOutPortHandler<T> handler) {
		return new Port<T>(portDataType, DataFlow.OUT, ControlType.PASSIVE, handler);
	}
	
	private Port(Class<T> portDataType, DataFlow portDataFlow, ControlType ct, PassivePortHandler<T> h) {
		if(portDataType == null) {
			throw new IllegalArgumentException("Port Data Type cannot be null");
		}
		if(ct == ControlType.PASSIVE && h == null) {
			throw new IllegalArgumentException("Handler of a Passive Port cannot be null");
		}
		this.portDataType = portDataType;
		this.portDataFlow = portDataFlow;
		this.portControlType = ct;
		this.handler = h;
	}
	
	PassivePortHandler<T> getHandler() {
		if(portControlType == ControlType.ACTIVE) {
			throw new UnsupportedOperationException("Active port cannot provide a passive handler");
		}
		return handler;
	}
	
	void send(Message<?> msg) {
		if(portControlType == ControlType.PASSIVE) {
			throw new UnsupportedOperationException("Passive port cannot actively send a message");
		}
		if(! msg.isTypeVerified()) {
			if(msg.getMessageDataType().equals(portDataType)) {
				msg.setTypeVerified(true);
			} else {
				throw new IllegalArgumentException("Message<" + msg.getMessageDataType().getCanonicalName() 
						+ "> cannot be sent through Port<" + portDataType.getCanonicalName() +">");
			}
		}
		connectedTo.getHandler().acceptMsg(msg);
	}
	
	@SuppressWarnings("unchecked")
	Message<? extends T> receive() {
		if(portControlType == ControlType.PASSIVE) {
			throw new UnsupportedOperationException("Passive port cannot actively receive a message");
		}
		return (Message<? extends T>) connectedTo.getHandler().produce();
	}

	static void connectPorts(Port<?> p1, Port<?> p2) throws IncompatiblePortsException {	
		// must define data flow from left to right or vice versa
		if(p1.portDataFlow == p2.portDataFlow) {
			throw new IncompatiblePortsException("Connected ports cannot have the same Data Flow (" 
					+ p1.portDataFlow.toString() + ")");
		}
		// one port must be active and the other passive
		if(p1.portControlType == p2.portControlType) {
			throw new IncompatiblePortsException("Connected ports cannot have the same Control Type (" 
					+ p1.portControlType.toString() + ")");
		}
		// the IN port data type must be either the same or superclass of the OUT port type
		if((p1.portDataFlow == DataFlow.OUT && ! p2.portDataType.isAssignableFrom(p1.portDataType))
				|| (p1.portDataFlow == DataFlow.IN && ! p1.portDataType.isAssignableFrom(p2.portDataType))){
			throw new IncompatiblePortsException("Incompatible port data types");
		} 
		// make the actual connection 
		if(p1.portControlType == ControlType.ACTIVE) {
			p1.connectedTo = p2;
		} else {
			p2.connectedTo = p1;
		}
	}
	
	PortDefinition<T> getOppositePortDefinition() {
		return new PortDefinition<>(portDataType, 
				DataFlow.getOposite(portDataFlow), 
				ControlType.getOposite(portControlType));
	}
	
	@Override
	public String toString() {
		return portControlType + " " + portDataFlow + " Port<" + portDataType.getCanonicalName() + ">";
	}
}
