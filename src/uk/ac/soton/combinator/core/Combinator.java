package uk.ac.soton.combinator.core;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Combinator {
	
	private AtomicBoolean combinable;

	protected final Boundary leftBoundary;
	protected final Boundary rightBoundary;
	
	protected abstract List<Port<?>> initLeftBoundary();
	protected abstract List<Port<?>> initRightBoundary();
	
	public Combinator() {
		this(CombinatorOrientation.LEFT_TO_RIGHT);
	}
	
	public Combinator(CombinatorOrientation orientation) {
		if(orientation == CombinatorOrientation.LEFT_TO_RIGHT) {
			leftBoundary = new Boundary(initLeftBoundary());
			rightBoundary = new Boundary(initRightBoundary());
		} else {
			leftBoundary = new Boundary(initRightBoundary());
			rightBoundary = new Boundary(initLeftBoundary());
		}
		// every combinator can be combined with another combinator -> so it's always combinable to start with
		combinable = new AtomicBoolean(true);
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
						"vertically combined combinators. " + thisRight.size() + " port(s) connecting to "
						+ otherLeft.size() + " port(s)");
			}
			
			/*
			 * This can throw IncompatiblePortsException at any stage which means
			 * that some of the ports can be connected successfully before the whole
			 * combination fails. This can leave both combinators in an unstable state
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
			return new Combinator() {
				
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
			
			return new Combinator() {

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
	}
}
