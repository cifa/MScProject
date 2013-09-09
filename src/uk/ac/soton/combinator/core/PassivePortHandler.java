package uk.ac.soton.combinator.core;

import uk.ac.soton.combinator.core.exception.CombinatorFailureException;

interface PassivePortHandler<T> {
	void acceptMsg(Message<?> msg) throws CombinatorFailureException, UnsupportedOperationException;
	Message<? extends T> produce() throws CombinatorFailureException, UnsupportedOperationException;
}
