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
				"function bar(i)\n" +				// 1
						"return i * 2\n" +			// 2
						"end\n" +					// 3
						"function foo()\n" +		// 4
						"for i = 1, 1000000 do\n" +	// 5
						"bar(i)\n" +				// 6
						"bar(i)\n" +				// 7
						"bar(i)\n" +				// 8
						"bar(i)\n" +				// 9
						"bar(i)\n" +				// 10
						"bar(i)\n" +				// 11
						"bar(i)\n" +				// 12
						"bar(i)\n" +				// 13
						"end\n" +					// 14
						"end\n" +					// 15
						"foo()\n" +					// 16
						"foo()\n",					// 17
				"test.lua",
				state.getEnvironment());
		AggregatingProfiler profiler = new AggregatingProfiler();
		Sampler sampler = new Sampler(state, 1, profiler);
		sampler.start();
		state.pcall(fun);
		sampler.stop();
		profiler.prettyPrint();
	}
}
