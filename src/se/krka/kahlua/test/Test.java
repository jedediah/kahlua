/*
Copyright (c) 2007 Kristofer Karlsson <kristofer.karlsson@gmail.com>

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
package se.krka.kahlua.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.stdlib.CoroutineLib;
import se.krka.kahlua.stdlib.MathLib;
import se.krka.kahlua.stdlib.StringLib;
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
		
		LuaClosure closure;
		try {
			closure = LuaPrototype.loadByteCode(new FileInputStream(stdlib), state.environment);
			state.call(closure, null, null, null);
			return state;		
		} catch (RuntimeException e) {
			System.out.println("Stdlib failed: " + e.getMessage());
			e.printStackTrace();
			System.out.println(state.currentThread.stackTrace);
			throw e;
		}
	}
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		File dir = new File(args[0]);

		LuaState state = getState(dir);

		File[] children; 
		if (args.length < 2) {
			children = dir.listFiles();
		} else {
			children = new File[1];
			children[0] = new File(dir, args[1]);
		}
		int successful = 0;
		int total = 0;
		for (int i = 0; i < children.length; i++) {
			File child = children[i];
			
			try {
				if (!child.getName().equals("stdlib.lbc") && child.getName().endsWith(".lbc")) {
					total++;
					LuaClosure closure = LuaPrototype.loadByteCode(new FileInputStream(child), state.environment);
					if (closure == null) {
						throw new RuntimeException("null");
					}
					state.call(closure, null, null, null);
					System.out.println("Ok:     " + child);
					successful++;
				}
			} catch (RuntimeException e) {
				System.out.println("Failed: " + child + ": " +  e.getMessage());
				e.printStackTrace();
				System.out.println(state.currentThread.stackTrace);

				// TODO: Repair cleanup code!
				state.currentThread.cleanCallFrames(null);
				state.currentThread.setTop(0);
				
			}
		}
		System.out.println(successful + " of " + total + " tests were ok!");
	}
}
