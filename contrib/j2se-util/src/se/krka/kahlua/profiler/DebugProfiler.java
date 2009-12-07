package se.krka.kahlua.profiler;

import java.io.Writer;
import java.io.PrintWriter;

public class DebugProfiler implements Profiler {
	private PrintWriter output;

	public DebugProfiler(Writer output) {
		this.output = new PrintWriter(output);
	}

    public synchronized void getSample(Sample sample) {
		output.println("Sample: " + sample.getTime() + " ms");
		for (StacktraceElement stacktraceElement : sample.getList()) {
			output.println("\t" + stacktraceElement.getSource() + ":" + stacktraceElement.getLine());
		}
	}
}
