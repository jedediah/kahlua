package se.krka.kahlua.profiler;

import java.util.*;

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

	public void prettyPrint(String name, String indent, long totalTime, long parentTime) {
		System.out.println(String.format("%s%s   %d ms (%.1f%% of total, %.1f%% of parent)",
				indent,
				name,
				getTime(),
				100.0 * getTime() / totalTime,
				100.0 * getTime() / parentTime));
		StacktraceElement[] sortedChildren = new StacktraceElement[children.size()];
		children.keySet().toArray(sortedChildren);
		Arrays.sort(sortedChildren, new Comparator<StacktraceElement>() {
			public int compare(StacktraceElement a, StacktraceElement b) {
				return Long.signum(children.get(b).getTime() - children.get(a).getTime());
			}
		});

		for (StacktraceElement sortedChild : sortedChildren) {
			StacktraceCounter counter = children.get(sortedChild);
			counter.prettyPrint(sortedChild.toString(), indent + "  ", totalTime, getTime());
		}
	}

	public long getTime() {
		return time;
	}
}
