package se.krka.kahluax.annotation;

import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;

public class LuaCaller {

	public static Object[] pcall(LuaState state, LuaClosure closure, Object... args) {
		for (int i = args.length - 1; i >= 0; i--) {
			args[i] = Converter.toLuaObject(args[i]);
		}
		return state.pcall(closure, args);
	}
}
