package se.krka.kahlua.profiler;

import org.junit.Test;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.luaj.compiler.LuaCompiler;

import java.io.IOException;
import java.io.StringWriter;

public class DumpProfilerTest {
	@Test
	public void simpleTest() throws IOException {
		LuaState state = new LuaState();
		LuaClosure fun = LuaCompiler.loadstring(
				"function bar(i)\n" +
						"return i * 2\n" +
						"end\n" +
						"function foo()\n" +
						"for i = 1, 10000 do\n" +
						"bar(i)\n" +
						"end\n" +
						"end\n" +
						"foo()",
				"test.lua",
				state.getEnvironment());
		StringWriter writer = new StringWriter();
		Sampler sampler = new Sampler(state, 1, new DumpProfiler(writer));
		sampler.start();
		state.pcall(fun);
		sampler.stop();
		System.out.println(writer.getBuffer().toString());
	}
}
