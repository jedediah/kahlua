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
package se.krka.kahlua.stdlib;

import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;

public final class BaseLib implements JavaFunction {

	private static final int PCALL = 0;
	private static final int PRINT = 1;
	private static final int SELECT = 2;
	private static final int TYPE = 3;
	private static final int TOSTRING = 4;
	private static final int TONUMBER = 5;
	private static final int GETMETATABLE = 6;
	private static final int SETMETATABLE = 7;
	private static final int ERROR = 8;
	private static final int UNPACK = 9;
	private static final int NEXT = 10;
	private static final int SETFENV = 11;
	private static final int GETFENV = 12;
	private static final int RAWEQUAL = 13;
	private static final int RAWSET = 14;
	private static final int RAWGET = 15;

	private static final int NUM_FUNCTIONS = 16;
	
	private static final String[] names;
	private static final Object MODE_KEY = "__mode".intern();
	
	static {
		names = new String[NUM_FUNCTIONS];
		names[PCALL] = "pcall";
		names[PRINT] = "print";
		names[SELECT] = "select";
		names[TYPE] = "type";
		names[TOSTRING] = "tostring";
		names[TONUMBER] = "tonumber";
		names[GETMETATABLE] = "getmetatable";
		names[SETMETATABLE] = "setmetatable";
		names[ERROR] = "error";
		names[UNPACK] = "unpack";
		names[NEXT] = "next";
		names[SETFENV] = "setfenv";
		names[GETFENV] = "getfenv";
		names[RAWEQUAL] = "rawequal";
		names[RAWSET] = "rawset";
		names[RAWGET] = "rawget";
	}

	private int index;
	private static BaseLib[] functions;	
	
	public BaseLib(int index) {
		this.index = index;
	}

	public static void register(LuaState state) {
		if (functions == null) {
			functions = new BaseLib[NUM_FUNCTIONS];
			for (int i = 0; i < NUM_FUNCTIONS; i++) {
				functions[i] = new BaseLib(i);
			}
		}
		
		for (int i = 0; i < NUM_FUNCTIONS; i++) {
			state.environment.rawset(names[i], functions[i]);
		}
	}

	public String toString() {
		return names[index];
	}

	public int call(LuaState state, int base)  {
		int nArguments = state.top - base - 1;
		switch (index) {
		case PCALL: return pcall(state, base, nArguments);
		case PRINT: return print(state, base, nArguments);
		case SELECT: return select(state, base, nArguments);
		case TYPE: return type(state, base, nArguments);
		case TOSTRING: return tostring(state, base, nArguments);
		case TONUMBER: return tonumber(state, base, nArguments);
		case GETMETATABLE: return getmetatable(state, base, nArguments);
		case SETMETATABLE: return setmetatable(state, base, nArguments);
		case ERROR: return error(state, base, nArguments);
		case UNPACK: return unpack(state, base, nArguments);
		case NEXT: return next(state, base, nArguments);
		case SETFENV: return setfenv(state, base, nArguments);
		case GETFENV: return getfenv(state, base, nArguments);
		case RAWEQUAL: return rawequal(state, base, nArguments);
		case RAWSET: return rawset(state, base, nArguments);
		case RAWGET: return rawget(state, base, nArguments);
		default:
			// Should never happen
			// throw new Error("Illegal function object");
			return 0;
		}
	}

	private int rawget(LuaState state, int base, int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments");
        LuaTable t = (LuaTable) state.stack[base + 1];
        Object key = state.stack[base + 2];
        
        state.stack[base] = t.rawget(key);
		return 1;
	}

	private int rawset(LuaState state, int base, int nArguments) {
        luaAssert(nArguments >= 3, "Not enough arguments");
        LuaTable t = (LuaTable) state.stack[base + 1];
        Object key = state.stack[base + 2];
        Object value = state.stack[base + 3];
        
        t.rawset(key, value);
        state.stack[base] = t;
        return 1;
	}

	private int rawequal(LuaState state, int base, int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments");
        Object o1 = state.stack[base + 1];
        Object o2 = state.stack[base + 2];
        
        state.stack[base] = toBoolean(LuaTable.luaEquals(o1, o2));
		return 1;
	}

	private static final Boolean toBoolean(boolean b) {
		if (b) return Boolean.TRUE;
		return Boolean.FALSE;
	}
	private int setfenv(LuaState state, int base, int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments");

        Object o = state.stack[base + 1];
    	luaAssert(o instanceof JavaFunction, "expected a lua function");
        
        LuaTable newEnv = (LuaTable) state.stack[base + 2];
        luaAssert(newEnv != null, "expected a table");
        
    	LuaClosure closure = (LuaClosure) o;
    	closure.env = newEnv;
        
        state.stack[base] = closure;
		return 1;
	}

	private int getfenv(LuaState state, int base, int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments");
		
        Object res = null;
        Object o = state.stack[base + 1];
        if (o instanceof LuaClosure) {
        	LuaClosure closure = (LuaClosure) o;
        	res = closure.env;
        } else {
        	luaAssert(o instanceof JavaFunction, "expected a function");
        	res = state.environment;
        }
        
        state.stack[base] = res;
		return 1;
	}

	private int next(LuaState state, int base, int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments");

        LuaTable t = (LuaTable) state.stack[base + 1];
        Object key = null;
        
        if (nArguments >= 2) {
        	key = state.stack[base + 2];
        }

        Object nextKey = t.next(key);
        if (nextKey == null) {
        	state.stack[base] = null;
        	return 1;
        }
        
        state.setTop(base + 2);            
    	        
        Object value = t.rawget(nextKey);
        
        state.stack[base] = nextKey;
        state.stack[base + 1] = value;
        return 2;
	}

	private int unpack(LuaState state, int base, int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments");

        LuaTable t = (LuaTable) state.stack[base + 1];


        Object di = null, dj = null;
        if (nArguments >= 2) {
        	di = state.stack[base + 2];
        }
        if (nArguments >= 3) {
        	dj = state.stack[base + 3];        	
        }
        
        int i, j;
        if (di != null) {
        	i = (int) LuaState.fromDouble(di);
        } else {
        	i = 1;
        }

        if (dj != null) {
        	j = (int) LuaState.fromDouble(dj);
        } else {
        	j = t.len();
        }
        
        int nReturnValues = 1 + j - i;

        if (nReturnValues <= 0) {
        	return 0;
        }

        state.setTop(base + nReturnValues);
        for (int b = 0; b < nReturnValues; b++) {
        	state.stack[base + b] = t.rawget(LuaState.toDouble((i + b)));
        }
        
        return nReturnValues;
	}

	private int error(LuaState state, int base, int nArguments) {
		if (nArguments >= 1) {
			String msg = rawTostring((String) state.stack[base + 1]);
			throw new RuntimeException(msg);
		}
		return 0;
	}

	public static int pcall(LuaState state, int base, int nArguments) {
		try {
			int nValues = state.call(base + 1);
			state.setTop(base + nValues + 1);
			state.stackCopy(base, base + 1, nValues);
			state.stack[base] = Boolean.TRUE;

			return 1 + nValues;
		} catch (RuntimeException e) {
			state.setTop(base + 3);
			state.stack[base] = Boolean.FALSE;
			String s = e.getMessage();
			if (s != null) {
			    s = s.intern();
			}

			state.stack[base + 1] = s;
			state.stack[base + 2] = state.stackTrace.intern();
			
			// Clean up
			state.cleanup(base);
			
			return 2;
		}
	}

	private static int print(LuaState state, int base, int nArguments) {
		Object toStringFun = state.tableGet(state.environment, "tostring");
		StringBuffer sb = new StringBuffer();
		for (int i = 1; i <= nArguments; i++) {
			Object res = state.call(toStringFun, state.stack[base + i], null, null);
			sb.append(res);
			if (i < nArguments) {
				sb.append("\t");
			}
		}
		state.out.println(sb.toString());
		return 0;
	}
	
	private static int select(LuaState state, int base, int nArguments) {
		luaAssert(nArguments >= 1, "Not enough arguments");
		Object arg1 = state.stack[base + 1];
		if (arg1 instanceof String) {
			luaAssert(((String) arg1).charAt(0) == '#', "Bad argument");
			state.stack[base] = LuaState.toDouble(nArguments - 1);
			return 1;
		}
		double d_index = LuaState.fromDouble(arg1);
		int index = (int) d_index;
		if (index >= 1 && index <= (nArguments - 1)) {
			int nResults = nArguments - index;
			state.stackCopy(base + index + 1, base, nResults);
			return nResults;
		}
		return 0;		
	}

	public static void luaAssert(boolean b, String msg) {
		if (!b) {
			throw new RuntimeException(msg);
		}
	}

	private static int getmetatable(LuaState state, int base, int nArguments) {
		luaAssert(nArguments >= 1, "Not enough arguments");
		Object o = state.stack[base + 1];
		
		
		Object metatable = state.getmetatable(o, false);
		state.stack[base] = metatable;
		return 1;
	}

	private static int setmetatable(LuaState state, int base, int nArguments) {
		luaAssert(nArguments >= 2, "Not enough arguments");
		
		Object o = state.stack[base + 1];

		LuaTable newMeta = (LuaTable) (state.stack[base + 2]);
		setmetatable(state, o, newMeta, false);
		state.stack[base] = o;
		return 1;
	}

	public static void setmetatable(LuaState state, Object o, LuaTable newMeta, boolean raw) {
		luaAssert(o != null, "Expected table, got nil");
		
		LuaTable oldMeta;
		
		LuaTable to = null;
		Class co = null;
		
		if (o instanceof LuaTable) {
			to = (LuaTable) o;
			oldMeta = to.metatable;
		} else {
			co = o.getClass();
			oldMeta = (LuaTable) state.userdataMetatables.rawget(co);
		}

		if (!raw && oldMeta != null && state.tableGet(oldMeta, "__metatable") != null) {
			throw new RuntimeException("Can not set metatable of protected object");
		}
		
		if (to != null) {
			to.metatable = newMeta;
			boolean weakKeys = false, weakValues = false;
			if (newMeta != null) {
				Object modeObj = newMeta.rawget(MODE_KEY);
				if (modeObj != null && modeObj instanceof String) {
					String mode = (String)modeObj;
					weakKeys = (mode.indexOf((int)'k') >= 0);
					weakValues = (mode.indexOf((int)'v') >= 0);
				}
			}
            if (weakKeys != to.weakKeys || weakValues != to.weakValues) {
            	to.updateWeakSettings(weakKeys, weakValues);
            }
		} else {			
			state.userdataMetatables.rawset(co, newMeta);
		}
	}

	private static int type(LuaState state, int base, int nArguments) {
		luaAssert(nArguments >= 1, "Not enough arguments");
		Object o = state.stack[base + 1];
		state.stack[base] = type(o);
		return 1;
	}

	public static String type(Object o) {
		if (o == null) return "nil";
		if (o instanceof String) return "string";
		if (o instanceof Double) return "number";
		if (o instanceof Boolean) return "boolean";
		if (o instanceof JavaFunction) return "function";
		if (o instanceof LuaClosure) return "function";
		if (o instanceof LuaTable) return "table";
		return "userdata"; 
	}

	private static int tostring(LuaState state, int base, int nArguments) {
		luaAssert(nArguments >= 1, "Not enough arguments");
		Object o = state.stack[base + 1];
		Object res = tostring(o, state);
		state.stack[base] = res;
		return 1;
	}

	public static String tostring(Object o, LuaState state) {
		if (o == null) return "nil";
		if (o instanceof String) return (String) o;
		if (o instanceof Double) return ((Double) o).toString().intern();
		if (o instanceof Boolean) return o == Boolean.TRUE ? "true" : "false";
		if (o instanceof JavaFunction) return "function";
		if (o instanceof LuaClosure) return "function";
		
		Object tostringFun = state.getMetaOp(o, "__tostring");
		if (tostringFun != null) {
			String res = (String) state.call(tostringFun, o, null, null);
			return res;
		}
		
		if (o instanceof LuaTable) return "table";
		throw new RuntimeException("no __tostring found on object");
	}

	private static int tonumber(LuaState state, int base, int nArguments) {
		luaAssert(nArguments >= 1, "Not enough arguments");
		Object o = state.stack[base + 1];

		if (o instanceof Double) {
			o = ((Double) o).toString();
		}
		
		String s = (String) o;

		int radix = 10;
		if (nArguments > 1) {
			double dradix = LuaState.fromDouble(state.stack[base + 2]);
			radix = (int) dradix;
			if (radix != dradix) {
				throw new RuntimeException("base is not an integer");
			}
		}
		Object res = tonumber(s, radix);
		state.stack[base] = res;
		return 1;			
	}

	public static Double tonumber(String s) {
		return tonumber(s, 10);
	}

	public static Double tonumber(String s, int radix)  {
		if (radix < 2 || radix > 36) {
			throw new RuntimeException("base out of range");
		}

		try {
			if (radix == 10) {
				return LuaState.toDouble(Double.parseDouble(s));
			} else {
				return LuaState.toDouble(Integer.parseInt(s, radix));
			}
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static String rawTostring(Object o) {
		if (o instanceof String) {
			return (String) o;
		}
		if (o instanceof Double) {
			return ((Double) o).toString();
		}
		return null;
	}

	public static Double rawTonumber(Object o) {
		if (o instanceof Double) {
			return (Double) o;
		}
		if (o instanceof String) {
			return tonumber((String) o);
		}
		return null;
	}
	
	public static boolean isFunction(Object o) {
		return o instanceof JavaFunction || o instanceof LuaClosure;
	}
}
