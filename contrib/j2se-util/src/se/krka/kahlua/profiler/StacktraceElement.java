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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof StacktraceElement)) return false;

		StacktraceElement that = (StacktraceElement) o;

		if (getLine() != that.getLine()) return false;
		if (!prototype.equals(that.prototype)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = getLine();
		result = 31 * result + prototype.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return getSource() + ":" + getLine();
	}
}
