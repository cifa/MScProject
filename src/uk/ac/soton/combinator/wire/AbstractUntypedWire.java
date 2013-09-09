package uk.ac.soton.combinator.wire;

import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.ControlType;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.Message;
import uk.ac.soton.combinator.core.PassiveInPortHandler;
import uk.ac.soton.combinator.core.PassiveOutPortHandler;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.core.PortDefinition;
import uk.ac.soton.combinator.core.exception.CombinatorFailureException;

public abstract class AbstractUntypedWire extends Combinator {
	
	protected enum Side {
		LEFT, RIGHT
	}

	public AbstractUntypedWire() {
		super();
	}
	
	public AbstractUntypedWire(CombinatorOrientation orientation) {
		super(orientation);
	}
	
	protected <T> Port<T> getPort(PortDefinition<T> def, Side side, int connectedPortIndex) {
		Port<T> port;
		if(def.getPortControlType() == ControlType.ACTIVE) {
			port = Port.getActivePort(def.getPortDataType(), def.getPortDataFlow());
		} else {
			if(def.getPortDataFlow() == DataFlow.OUT) {
				port = Port.getPassiveOutPort(def.getPortDataType(), 
						new IdentityPassiveOutPortHandler<T>(connectedPortIndex, side));
			} else {
				port = Port.getPassiveInPort(def.getPortDataType(), 
						new IdentityPassiveInPortHandler<T>(connectedPortIndex, side));
			}
		}
		return port;
	}
	
	protected <T> Port<T> getComplementaryPort(PortDefinition<T> def, Side side, int connectedPortIndex) {
		Port<T> port;
		if(def.getPortControlType() == ControlType.PASSIVE) {
			// Passive complemented by Active
			port = Port.getActivePort(def.getPortDataType(), DataFlow.getOposite(def.getPortDataFlow()));
		} else {
			if(def.getPortDataFlow() == DataFlow.IN) {
				port = Port.getPassiveOutPort(def.getPortDataType(), 
						new IdentityPassiveOutPortHandler<T>(connectedPortIndex, side));
			} else {
				port = Port.getPassiveInPort(def.getPortDataType(), 
						new IdentityPassiveInPortHandler<T>(connectedPortIndex, side));
			}
		}
		return port;
	}
	
	private class IdentityPassiveOutPortHandler<T> extends PassiveOutPortHandler<T> {
		
		private final int portIndex;
		private final Side side;
		
		IdentityPassiveOutPortHandler(int portIndex, Side side) {
			this.portIndex = portIndex;
			this.side = side;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Message<? extends T> produce() throws CombinatorFailureException  {
			if(side == Side.LEFT) {
				return (Message<? extends T>) receiveLeft(portIndex);
			} else {
				return (Message<? extends T>) receiveRight(portIndex);
			}
		}	
	};
	
	private class IdentityPassiveInPortHandler<T> extends PassiveInPortHandler<T> {
		
		private final int portIndex;
		private final Side side;
		
		IdentityPassiveInPortHandler(int portIndex, Side side) {
			this.portIndex = portIndex;
			this.side = side;
		}
		
		@Override
		public void accept(Message<? extends T> msg)
				throws CombinatorFailureException {
			if(side == Side.LEFT) {
				sendLeft(msg, portIndex);
			} else {
				sendRight(msg, portIndex);
			}
		}
	};
}
