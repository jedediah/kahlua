package se.krka.kahlua.profiler;

import java.util.List;

public interface Profiler {
	void getSample(List<StacktraceElement> list, long time);
}
