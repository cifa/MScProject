package uk.ac.soton.combinator.core;

public final class PortDefinition<T> {

	private final Class<T> portDataType;
	private final DataFlow portDataFlow;
	private final ControlType portControlType;
	
	public PortDefinition(Class<T> portDataType, DataFlow portDataFlow, ControlType portControlType) {
		this.portDataType = portDataType;
		this.portDataFlow = portDataFlow;
		this.portControlType = portControlType;
	}

	public Class<T> getPortDataType() {
		return portDataType;
	}

	public DataFlow getPortDataFlow() {
		return portDataFlow;
	}

	public ControlType getPortControlType() {
		return portControlType;
	}
}
