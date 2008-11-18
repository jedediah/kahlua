/*
Copyright (c) 2007-2008 Kristofer Karlsson <kristofer.karlsson@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.stdlib.CoroutineLib;
import se.krka.kahlua.stdlib.MathLib;
import se.krka.kahlua.stdlib.StringLib;
import se.krka.kahlua.stdlib.OsLib;
import se.krka.kahlua.test.UserdataArray;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaPrototype;
import se.krka.kahlua.vm.LuaState;

public class Test {
	private static LuaState getState(File dir) throws FileNotFoundException, IOException {
		File stdlib = new File(dir, "stdlib.lbc");

		LuaState state = new LuaState(System.out);
		
		BaseLib.register(state);
		MathLib.register(state);
		StringLib.register(state);
		UserdataArray.register(state);
		CoroutineLib.register(state);
		OsLib.register(state);

		LuaClosure closure = LuaPrototype.loadByteCode(new FileInputStream(stdlib), state.environment);
		Object[] results = state.pcall(closure);
		if (results[0] != Boolean.TRUE) {
			System.out.println("Stdlib failed: " + results[1]);
			((Throwable) (results[3])).printStackTrace();
			System.out.println(results[2]);
			state = null;
		}
		return state;
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		File dir = new File(args[0]);

		LuaState state = getState(dir);

		File[] children = null;
		for (int i = 1; i < args.length; i++) {
			File f = new File(dir, args[i]);
			if (f.exists() && f.isFile()) {
				if (children == null) {
					children = new File[args.length];
				}
				children[i] =  f;
			} else {
			}
		}
		if (children == null) {
			children = dir.listFiles();
		}
		
		int successful = 0;
		int total = 0;
		for (int i = 0; i < children.length; i++) {
			File child = children[i];
			if (child != null && !child.getName().equals("stdlib.lbc") && child.getName().endsWith(".lbc")) {
				total++;
				
				LuaClosure closure = LuaPrototype.loadByteCode(new FileInputStream(child), state.environment);
				Object[] results = state.pcall(closure);
				if (results[0] == Boolean.TRUE) {
					System.out.println("Ok:     " + child);
					successful++;
				} else {
					System.out.println("Failed: " + child + ": " +  results[1]);
					((Throwable) (results[3])).printStackTrace();
					System.out.println(results[2]);
				}
			}
		}
		System.out.println(successful + " of " + total + " tests were ok!");
	}
}
