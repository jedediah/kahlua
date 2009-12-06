package se.krka.kahlua.profiler;

import se.krka.kahlua.vm.*;

import java.util.TimerTask;
import java.util.Timer;
import java.util.ArrayList;
import java.util.List;

public class Sampler {
	private final LuaState state;
	private final Timer timer;
	private final long period;
	private final Profiler profiler;

	public Sampler(LuaState state, long period, Profiler profiler) {
		this.state = state;
		this.period = period;
		this.profiler = profiler;
		timer = new Timer("Kahlua Sampler", true);
	}

	public void start() {
		TimerTask timerTask = new TimerTask() {
			@Override
			public void run() {
				List<StacktraceElement> list = new ArrayList<StacktraceElement>();
				appendList(list, state.currentThread);
				profiler.getSample(list, period);				
			}
		};
		timer.scheduleAtFixedRate(timerTask, 0, period);
	}

	private void appendList(List<StacktraceElement> list, LuaThread thread) {
		int top = thread.callFrameTop;
		for (int i = top - 1; i >= 0; i--) {
			LuaCallFrame frame = thread.callFrameStack[i];

			int pc = frame.pc - 1;
			LuaClosure closure = frame.closure;
			if (closure != null) {
				list.add(new StacktraceElement(pc, closure.prototype));
			}
		}
		if (thread.parent != null) {
			appendList(list, thread.parent);
		}
	}

	public void stop() {
		timer.cancel();
	}
}
