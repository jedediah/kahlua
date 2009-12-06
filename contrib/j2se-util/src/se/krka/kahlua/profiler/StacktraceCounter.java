package se.krka.kahlua.profiler;

import java.util.HashMap;
import java.util.Map;

public class StacktraceCounter {
	private final Map<StacktraceElement, StacktraceCounter> children = new HashMap<StacktraceElement, StacktraceCounter>();
	private long time = 0;

	public void addTime(long time) {
		this.time += time;
	}

	public StacktraceCounter getOrCreateChild(StacktraceElement childElement) {
		StacktraceCounter stacktraceCounter = children.get(childElement);
		if (stacktraceCounter == null) {
			stacktraceCounter = new StacktraceCounter();
			children.put(childElement, stacktraceCounter);
		}
		return stacktraceCounter;
	}

	public void prettyPrint(String name, String indent) {
		System.out.println(indent + name + ": " + time);
		for (Map.Entry<StacktraceElement, StacktraceCounter> entry : children.entrySet()) {
			entry.getValue().prettyPrint(entry.getKey().toString(), indent + "  ");
		}
	}
}
