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
import se.krka.kahlua.stdlib.MathLib;
import se.krka.kahlua.stdlib.StringLib;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaPrototype;
import se.krka.kahlua.vm.LuaState;

public class Test {
	public static void main(String[] args) throws FileNotFoundException, IOException {
		File f = new File(args[0]);
		System.out.println("Loading file: stdlib.lbc");
		File stdlib = new File(f, "stdlib.lbc");

		LuaState state = new LuaState(System.out);
		
		BaseLib.register(state);
		MathLib.register(state);
		StringLib.register(state);
		UserdataArray.register(state);
		
		LuaClosure closure;
		try {
			closure = LuaPrototype.loadByteCode(new FileInputStream(stdlib), state.environment);
			state.call(closure, null, null, null);
		} catch (RuntimeException e) {
			System.out.println("Stdlib failed:");

			e.printStackTrace();
			System.out.println(state.stackTrace);
			
			return;
		}
				
		File[] children = f.listFiles();
		for (int i = 0; i < children.length; i++) {
			File child = children[i];
			
			try {
				if (child != stdlib && child.getName().endsWith(".lbc")) {
					System.out.println("Testing file: " + child.getName());
					closure = LuaPrototype.loadByteCode(new FileInputStream(child), state.environment);
					state.call(closure, null, null, null);

					System.out.println(child + " Ok!");
				}
			} catch (RuntimeException e) {
				System.out.println(child + " Failed:");

				e.printStackTrace();
				System.out.println(state.stackTrace);
				
				state.cleanup(0);
				state.setTop(0);
			}
		}
	}
}
