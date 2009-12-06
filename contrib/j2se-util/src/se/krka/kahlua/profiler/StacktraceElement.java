package se.krka.kahlua.profiler;

import se.krka.kahlua.vm.LuaPrototype;

public class StacktraceElement {
	private final int pc;
	private final LuaPrototype prototype;

	public StacktraceElement(int pc, LuaPrototype prototype) {
		this.pc = pc;
		this.prototype = prototype;
	}

	public int getLine() {
		if (pc >= 0 && pc < prototype.lines.length) {
			return prototype.lines[pc];
		}
		return 0;
	}
	
	public String getSource() {
		return prototype.name;
	}
}
