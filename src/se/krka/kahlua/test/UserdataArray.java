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

import java.util.Vector;

import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;

public class UserdataArray implements JavaFunction {
	
	private static final int LENGTH = 0;
	private static final int INDEX = 1;
	private static final int NEWINDEX = 2;
	private static final int NEW = 3;
	private static final int PUSH = 4;
	
	private static LuaTable metatable;
	
	public static void register(LuaState state) {
		if (metatable == null) {
			metatable = new LuaTable();
			metatable.rawset("__metatable", "restricted");
			metatable.rawset("__len", new UserdataArray(LENGTH));
			metatable.rawset("__index", new UserdataArray(INDEX));
			metatable.rawset("__newindex", new UserdataArray(NEWINDEX));
			
			metatable.rawset("new", new UserdataArray(NEW));
			metatable.rawset("push", new UserdataArray(PUSH));
		}
		
		state.setUserdataMetatable(Vector.class, metatable);
		state.environment.rawset("array", metatable);
	}

	private int index;
	
	private UserdataArray(int index) {
		this.index = index;
	}
	
	public int call(LuaState state, int base) {
		int nArguments = state.top - base;
		switch (index) {
		case LENGTH: return length(state, base, nArguments);
		case INDEX: return index(state, base, nArguments);
		case NEWINDEX: return newindex(state, base, nArguments);
		case NEW: return newVector(state, base, nArguments);
		case PUSH: return push(state, base, nArguments);
		}
		return 0;
	}

	private int push(LuaState state, int base, int nArguments) {
		BaseLib.luaAssert(nArguments >= 2, "not enough parameters");		
		Vector v = (Vector) state.stack[base + 1];
		Object value = state.stack[base + 2];
		
		v.addElement(value);
		state.stack[base] = v;
		return 1;
	}

	private int newVector(LuaState state, int base, int nArguments) {
		state.stack[base] = new Vector();
		return 1;
	}

	private int newindex(LuaState state, int base, int nArguments) {
		BaseLib.luaAssert(nArguments >= 3, "not enough parameters");
		Vector v = (Vector) state.stack[base + 1];
		Object value = state.stack[base + 3];
		
		v.setElementAt(value, (int) LuaState.fromDouble(state.stack[base + 2]));
		return 0;
	}

	private int index(LuaState state, int base, int nArguments) {
		BaseLib.luaAssert(nArguments >= 2, "not enough parameters");
		Vector v = (Vector) state.stack[base + 1];
		
		Object key = state.stack[base + 2];
		Object res;
		if (key instanceof Double) {
			res = v.elementAt((int) LuaState.fromDouble(key));
		} else {
			res = metatable.rawget(key);
		}
		state.stack[base] = res;
		return 1;
	}

	private int length(LuaState state, int base, int nArguments) {
		BaseLib.luaAssert(nArguments >= 1, "not enough parameters");
		Vector v = (Vector) state.stack[base + 1];
		double size = v.size();
		state.stack[base] = LuaState.toDouble(size);
		return 1;
	}
}
