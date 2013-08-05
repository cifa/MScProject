package testing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.ac.soton.combinator.core.CombinationType;
import uk.ac.soton.combinator.core.Combinator;
import uk.ac.soton.combinator.core.CombinatorOrientation;
import uk.ac.soton.combinator.core.DataFlow;
import uk.ac.soton.combinator.core.MessagePool;
import uk.ac.soton.combinator.core.Port;
import uk.ac.soton.combinator.data.BackOffTreiberStack;
import uk.ac.soton.combinator.data.EliminationExchanger2;
import uk.ac.soton.combinator.wire.AdaptorPullWire;
import uk.ac.soton.combinator.wire.ChoiceReceiveWire;
import uk.ac.soton.combinator.wire.ChoiceSendWire;
import uk.ac.soton.combinator.wire.ReverseWire;

public class EliminationStack<T> extends Combinator implements IStack<T> {
	
	public EliminationStack() {
		ChoiceSendWire<Object> sendChoice = new ChoiceSendWire<>(
				Object.class, 2, combinatorOrientation);
		ChoiceReceiveWire<Object> receiveChoice = new ChoiceReceiveWire<>(
				Object.class, 2, combinatorOrientation);
		BackOffTreiberStack<Object> stack = new BackOffTreiberStack<>(
				Object.class, combinatorOrientation);
		EliminationExchanger2<Object> eliminationExchanger = 
				new EliminationExchanger2<>(Object.class, combinatorOrientation);
		
		Combinator eliminationStack = sendChoice
				.combine(
						(stack.combine(eliminationExchanger,CombinationType.VERTICAL)),
						CombinationType.HORIZONTAL)
				.combine(receiveChoice, CombinationType.HORIZONTAL);
		
		ReverseWire reverse = new ReverseWire(
				eliminationStack.getPortDefinitionCompatibleWithRightBoundary(),
				combinatorOrientation);
		AdaptorPullWire<Object> pull = new AdaptorPullWire<>(Object.class, 1, 
				CombinatorOrientation.getOpposite(combinatorOrientation));
		
		this.combine(
				(eliminationStack.combine(pull, CombinationType.VERTICAL)
						.combine(reverse, CombinationType.HORIZONTAL)),
				CombinationType.HORIZONTAL);
	}

	@Override
	protected List<Port<?>> initLeftBoundary() {
		return Collections.emptyList();
	}

	@Override
	protected List<Port<?>> initRightBoundary() {
		List<Port<?>> ports = new ArrayList<Port<?>>();
		ports.add(Port.getActivePort(Object.class, DataFlow.OUT));
		ports.add(Port.getActivePort(Object.class, DataFlow.IN));
		return ports;
	}

	@Override
	public void push(T value) {
		getRightBoundary().send(MessagePool.createMessage(Object.class, value), 0);
	}

	@SuppressWarnings("unchecked")
	@Override
	public T pop() {
		return (T) getRightBoundary().receive(1).get();
	}
}
