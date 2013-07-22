package uk.ac.soton.combinator.core;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Combinator {
	
	private final static int LEFT = 0;
	private final static int RIGHT = 1;
	
	private AtomicBoolean combinable;
	private final Boundary leftBoundary;
	private final Boundary rightBoundary;
	
	protected final CombinatorOrientation combinatorOrientation;
	
	protected abstract List<Port<?>> initLeftBoundary();
	protected abstract List<Port<?>> initRightBoundary();
	
	public Combinator() {
		this(CombinatorOrientation.LEFT_TO_RIGHT);
	}
	
	public Combinator(CombinatorOrientation orientation) {
		leftBoundary = new Boundary();
		rightBoundary = new Boundary();
		combinatorOrientation = orientation;
		// every combinator can be combined with another combinator -> so it's always combinable to start with
		combinable = new AtomicBoolean(true);
	}
	
	private void initBoundaries() {
		if(combinatorOrientation == CombinatorOrientation.LEFT_TO_RIGHT) {
			if(! leftBoundary.isBoundaryInitialized()) {
				leftBoundary.setBoundaryInterface(initLeftBoundary());
			}
			if(! rightBoundary.isBoundaryInitialized()) {
				rightBoundary.setBoundaryInterface(initRightBoundary());
			}	
		} else {
			if(! leftBoundary.isBoundaryInitialized()) {
				leftBoundary.setBoundaryInterface(initRightBoundary());
			}
			if(! rightBoundary.isBoundaryInitialized()) {
				rightBoundary.setBoundaryInterface(initLeftBoundary());
			}
		}
	}
	
	public Combinator combine(final Combinator other, CombinationType combinationType) {
		// check if both combinators can be combined (not combined yet)
		if(combinable.compareAndSet(true, false)) {
			if(! other.combinable.compareAndSet(true, false)) {
				combinable.compareAndSet(false, true);
				throw new IllegalCombinationException("Combinator already combined");
			}
		} else {
			throw new IllegalCombinationException("Combinator already combined");
		}
		// make sure all boundaries are initialized
		initBoundaries();
		other.initBoundaries();
		
		Combinator combinationResult;
		
		// try and combine the combinators
		if(combinationType == CombinationType.HORIZONTAL) {
			// try to connect the right boundary of this combinator with the left boundary of the other
			List<Port<?>> thisRight = rightBoundary.getBoundaryInterface();
			List<Port<?>> otherLeft = other.leftBoundary.getBoundaryInterface();
			// we need the same number of ports
			if(thisRight.size() != otherLeft.size()) {
				// combination not possible -> both should stay combinable
				combinable.compareAndSet(false, true);
				other.combinable.compareAndSet(false, true);
				throw new IllegalCombinationException("Incompatible boundery sizes of " +
						"horizontally combined combinators. " + thisRight.size() + " port(s) connecting to "
						+ otherLeft.size() + " port(s)");
			}
			
			/*
			 * This can throw IncompatiblePortsException at any stage which means
			 * that some of the ports can be connected successfully before the whole
			 * combination fails. This can leave both combinators in an inconsistent state
			 * and, therefore, their 'combinable' flags are left marked as false; 
			 * effectively rendering them unusable.
			 */
			for(int i=0; i<thisRight.size(); i++) {
				Port<?> left = thisRight.get(i);
				Port<?> right = otherLeft.get(i);
				Port.connectPorts(left, right);
			}
			
			// with the inner boundaries connected we can expose the outside ones as a new combinator
			final List<Port<?>> newLeftBoundary = leftBoundary.getBoundaryInterface();
			combinationResult = new Combinator() {
				
				@Override
				protected List<Port<?>> initLeftBoundary() {
					return newLeftBoundary;
				}
				
				@Override
				protected List<Port<?>> initRightBoundary() {
					return other.rightBoundary.getBoundaryInterface();
				}
			};
		} else {
			// VERTICAL combination - merge left and right boundaries
			final List<Port<?>> newLeftBoundary = leftBoundary.getBoundaryInterface();
			newLeftBoundary.addAll(other.leftBoundary.getBoundaryInterface());
			
			final List<Port<?>> newRightBoundary = rightBoundary.getBoundaryInterface();
			newRightBoundary.addAll(other.rightBoundary.getBoundaryInterface());
			
			combinationResult = new Combinator() {

				@Override
				protected List<Port<?>> initLeftBoundary() {				
					return newLeftBoundary;
				}
				
				@Override
				protected List<Port<?>> initRightBoundary() {	
					return newRightBoundary;
				}
			};
		}
		// properly initialise the boundaries of the new combinator;
		combinationResult.initBoundaries();
		return combinationResult;
	}
	
	public PortDefinition<?>[] getPortDefinitionCompatibleWithLeftBoundary() {
		return getBoundaryDefinition(LEFT);
	}
	
	public PortDefinition<?>[] getPortDefinitionCompatibleWithRightBoundary() {
		return getBoundaryDefinition(RIGHT);
	}
	
	private PortDefinition<?>[] getBoundaryDefinition(int which) {
		// make sure boundaries are initialised
		initBoundaries();
		List<Port<?>> ports;
		if(which == LEFT) {
			 ports = leftBoundary.getBoundaryInterface();
		} else {
			ports = rightBoundary.getBoundaryInterface();
		}
		PortDefinition<?>[] defs = new PortDefinition<?>[ports.size()];
		for(int i=0; i<ports.size(); i++) {
			defs[i] = ports.get(i).getOppositePortDefinition();
		}
		return defs;
	}
	
	protected Boundary getLeftBoundary() {
		if(combinatorOrientation == CombinatorOrientation.LEFT_TO_RIGHT) {
			return leftBoundary;
		} else {
			return rightBoundary;
		}
	}
	
	protected Boundary getRightBoundary() {
		if(combinatorOrientation == CombinatorOrientation.LEFT_TO_RIGHT) {
			return rightBoundary;
		} else {
			return leftBoundary;
		}
	}
	
	@Override
	public String toString() {
		// make sure boundaries are initialised
		initBoundaries();
		String ret = "Combinator (" + combinatorOrientation + ")\n" 
				+ "Left Ports: \n";
		for(Port<?> p : leftBoundary.getBoundaryInterface()) {
			ret += p.toString() + "\n";
		}
		ret += "Right Ports: \n";
		for(Port<?> p : rightBoundary.getBoundaryInterface()) {
			ret += p.toString() + "\n";
		}
		return ret;
	}
}
