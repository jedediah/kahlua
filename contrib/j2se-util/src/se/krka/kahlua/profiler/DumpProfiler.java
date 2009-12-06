package se.krka.kahlua.profiler;

import java.util.List;
import java.io.Writer;
import java.io.PrintWriter;

public class DumpProfiler implements Profiler {
	private PrintWriter output;

	public DumpProfiler(Writer output) {
		this.output = new PrintWriter(output);
	}

	public synchronized void getSample(List<StacktraceElement> list, long time) {
		output.println("Sample: " + time + " ms");
		for (StacktraceElement stacktraceElement : list) {
			output.println("\t" + stacktraceElement.getSource() + ":" + stacktraceElement.getLine());
		}
	}
}
