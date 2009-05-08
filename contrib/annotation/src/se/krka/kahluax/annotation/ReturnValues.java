package se.krka.kahluax.annotation;

import se.krka.kahlua.vm.LuaCallFrame;

public class ReturnValues {
	private LuaCallFrame callFrame;
	private int args;
	
	ReturnValues() {		
	}
	
	void reset(LuaCallFrame callFrame) {
		this.callFrame = callFrame;
		args = 0;
	}
	
	public ReturnValues push(Object o) {
		args += callFrame.push(Converter.toLuaObject(o));
		return this;
	}
	
	int getNArguments() {
		return args;
	}
}
