/*
 Copyright (c) 2009 Kristofer Karlsson <kristofer.karlsson@gmail.com>

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

package se.krka.kahlua.integration.expose;

import java.util.WeakHashMap;
import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.LuaTableImpl;

public class UserdataProperties {
	private final IndexFunction indexFunction;
	private final NewIndexFunction newIndexFunction;
	private final WeakHashMap<Object, LuaTable> properties;
	private final LuaTable fallbackIndex;

	public UserdataProperties(final LuaTable fallbackIndex) {
		properties = new WeakHashMap<Object, LuaTable>();
		this.fallbackIndex = fallbackIndex;
		indexFunction = new IndexFunction();
		newIndexFunction = new NewIndexFunction();
	}

	private LuaTable getProperties(Object self) {
		LuaTable t = properties.get(self);
		if (t == null) {
			t = new LuaTableImpl();
			properties.put(self, t);
		}
		return t;
	}

	public JavaFunction getIndexFunction() {
		return indexFunction;

	}

	public JavaFunction getNewIndexFunction() {
		return newIndexFunction;
	}

	public class IndexFunction implements JavaFunction {
		private IndexFunction() {
		}

		@Override
		public int call(LuaCallFrame callFrame, int nArguments) {
			BaseLib.luaAssert(nArguments >= 2, "not enough arguments");
			Object self = callFrame.get(0);
			Object key = callFrame.get(1);

			if (("__fallback").equals(key)) {
				return callFrame.push(fallbackIndex);
			}

			LuaTable propertiesTable = getProperties(self);
			Object value = propertiesTable.rawget(key);
			if (value == null && fallbackIndex != null) {
				value = callFrame.thread.state.tableGet(fallbackIndex, key);
			}
			return callFrame.push(value);
		}

		public LuaTable getFallBackIndex() {
			return fallbackIndex;
		}
	}

	public class NewIndexFunction implements JavaFunction {
		private NewIndexFunction() {
		}

		@Override
		public int call(LuaCallFrame callFrame, int nArguments) {
			BaseLib.luaAssert(nArguments >= 3, "not enough arguments");
			Object self = callFrame.get(0);
			Object key = callFrame.get(1);
			Object value = callFrame.get(2);

			LuaTable propertiesTable = getProperties(self);
			propertiesTable.rawset(key, value);
			return 0;
		}
	};
}
