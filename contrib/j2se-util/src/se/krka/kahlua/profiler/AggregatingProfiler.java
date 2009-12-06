package se.krka.kahlua.profiler;

import java.util.List;
import java.util.Map;

public class AggregatingProfiler implements Profiler {
	private final StacktraceCounter root = new StacktraceCounter();

	public AggregatingProfiler() {
	}

	public synchronized void getSample(List<StacktraceElement> list, long time) {
		root.addTime(time);

		StacktraceCounter counter = root;
		int n = list.size() - 1;
		while (n > 0) {
			StacktraceElement childElement = list.get(n);
			StacktraceCounter childCounter = counter.getOrCreateChild(childElement);

			childCounter.addTime(time);

			counter = childCounter;
			n--;
		}
	}

	public void prettyPrint() {
		root.prettyPrint("Total", "");
	}
}
