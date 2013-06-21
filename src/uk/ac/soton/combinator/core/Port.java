package uk.ac.soton.combinator.core;

public class Port<T> {
	
	private final Class<T> portDataType;
	private final DataFlow portDataFlow;
	private final ControlType portControlType;
	private final PassivePortHandler<T> handler;
//	private volatile Port<T>

	public Port(Class<T> portDataType, DataFlow portDataFlow) {
		// active ports don't have handlers
		this(portDataType, portDataFlow, ControlType.ACTIVE, null);
		
	}
	
	public Port(Class<T> portDataType, DataFlow portDataFlow, PassivePortHandler<T> handler) {
		this(portDataType, portDataFlow, ControlType.PASSIVE, handler);
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

	public static void connectPorts(Port<? extends Object> p1, Port<? extends Object> p2) {	
		if(p1.portDataFlow == p2.portDataFlow) {
			throw new IncompatiblePortsException("Connected ports cannot have the same Data Flow (" 
					+ p1.portDataFlow.toString() + ")");
		}
		if(p1.portControlType == p2.portControlType) {
			throw new IncompatiblePortsException("Connected ports cannot have the same Control Type (" 
					+ p1.portControlType.toString() + ")");
		}
		
		if((p1.portDataFlow == DataFlow.OUT && ! p2.portDataType.isAssignableFrom(p1.portDataType))
				|| (p1.portDataFlow == DataFlow.IN && ! p1.portDataType.isAssignableFrom(p2.portDataType))){
			throw new IncompatiblePortsException("Incompatible port data types");
		} 
		
		if(p1.portControlType == ControlType.ACTIVE) {
			
		}
	}
}
