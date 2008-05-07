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
package se.krka.kahlua.stdlib;

import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaException;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.LuaThread;

public final class BaseLib implements JavaFunction {

	private static final Runtime RUNTIME = Runtime.getRuntime();
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
	private static final int COLLECTGARBAGE = 16;

	private static final int NUM_FUNCTIONS = 17;
	
	private static final String[] names;
	private static final Object MODE_KEY = "__mode";
	private static final Object DOUBLE_ONE = new Double(1.0);
	
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
		names[COLLECTGARBAGE] = "collectgarbage";
	}

	private int index;
	public static BaseLib[] functions;	
	
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


	public int call(LuaCallFrame callFrame, int nArguments) {
		switch (index) {
		case PCALL: return pcall(callFrame, nArguments);
		case PRINT: return print(callFrame, nArguments);
		case SELECT: return select(callFrame, nArguments);
		case TYPE: return type(callFrame, nArguments);
		case TOSTRING: return tostring(callFrame, nArguments);
		case TONUMBER: return tonumber(callFrame, nArguments);
		case GETMETATABLE: return getmetatable(callFrame, nArguments);
		case SETMETATABLE: return setmetatable(callFrame, nArguments);
		case ERROR: return error(callFrame, nArguments);
		case UNPACK: return unpack(callFrame, nArguments);
		case NEXT: return next(callFrame, nArguments);
		case SETFENV: return setfenv(callFrame, nArguments);
		case GETFENV: return getfenv(callFrame, nArguments);
		case RAWEQUAL: return rawequal(callFrame, nArguments);
		case RAWSET: return rawset(callFrame, nArguments);
		case RAWGET: return rawget(callFrame, nArguments);
		case COLLECTGARBAGE: return collectgarbage(callFrame, nArguments);
		default:
			// Should never happen
			// throw new Error("Illegal function object");
			return 0;
		}
	}

	private int rawget(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments");
        LuaTable t = (LuaTable) callFrame.get(0);
        Object key = callFrame.get(1);
        
        callFrame.push(t.rawget(key));
        return 1;
	}

	private int rawset(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 3, "Not enough arguments");
        LuaTable t = (LuaTable) callFrame.get(0);
        Object key = callFrame.get(1);
        Object value = callFrame.get(2);
        
        t.rawset(key, value);
        callFrame.setTop(1);
        return 1;        
	}

	private int rawequal(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments");
        Object o1 = callFrame.get(0);
        Object o2 = callFrame.get(1);

        callFrame.push(toBoolean(LuaState.luaEquals(o1, o2)));
        return 1;
	}

	private static final Boolean toBoolean(boolean b) {
		if (b) return Boolean.TRUE;
		return Boolean.FALSE;
	}
	
	private int setfenv(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 2, "Not enough arguments");

        Object o = callFrame.get(0);
    	luaAssert(o instanceof LuaClosure, "expected a lua function");
        
        LuaTable newEnv = (LuaTable) callFrame.get(1);
        luaAssert(newEnv != null, "expected a table");
        
    	LuaClosure closure = (LuaClosure) o;
    	closure.env = newEnv;
        
    	callFrame.setTop(1);
    	return 1;
	}

	private int getfenv(LuaCallFrame callFrame, int nArguments) {
		Object o = DOUBLE_ONE;
		if (nArguments >= 1) {
	        o = callFrame.get(0);
		}
	
        Object res = null;
        if (o == null || o instanceof JavaFunction) {
        	res = callFrame.thread.state.environment;
        } else if (o instanceof LuaClosure) {
        	LuaClosure closure = (LuaClosure) o;
        	res = closure.env;
        } else {
        	Double d = rawTonumber(o);
        	luaAssert(d != null, "Expected number");
        	int level = d.intValue();
        	luaAssert(level >= 0, "level must be non-negative");
        	int callFrame2index = callFrame.thread.callFrameTop - level - 1;
        	luaAssert(callFrame2index >= 0, "invalid level");
        	LuaCallFrame callFrame2 = callFrame.thread.callFrameStack[callFrame2index];
        	res = callFrame2.getEnvironment();
        }
        callFrame.push(res);
        return 1;
	}

	private int next(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments");

        LuaTable t = (LuaTable) callFrame.get(0);
        Object key = null;
        
        if (nArguments >= 2) {
        	key = callFrame.get(1);
        }

        Object nextKey = t.next(key);
        if (nextKey == null) {
        	callFrame.setTop(1);
        	callFrame.set(0, null);
        	return 1;
        }

        Object value = t.rawget(nextKey);
        
    	callFrame.setTop(2);
    	callFrame.set(0, nextKey);
    	callFrame.set(1, value);
    	return 2;
	}

	private int unpack(LuaCallFrame callFrame, int nArguments) {
        luaAssert(nArguments >= 1, "Not enough arguments");

        LuaTable t = (LuaTable) callFrame.get(0);

        Object di = null, dj = null;
        if (nArguments >= 2) {
        	di = callFrame.get(1);
        }
        if (nArguments >= 3) {
        	dj = callFrame.get(2);        	
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
        	callFrame.setTop(0);
        	return 0;
        }

        callFrame.setTop(nReturnValues);
        for (int b = 0; b < nReturnValues; b++) {
        	callFrame.set(b, t.rawget(LuaState.toDouble((i + b))));
        }
        return nReturnValues;
	}

	private int error(LuaCallFrame callFrame, int nArguments) {
		if (nArguments >= 1) {
			throw new LuaException(callFrame.get(0));
		}
		return 0;
	}

	public static int pcall(LuaCallFrame callFrame, int nArguments) {
		return callFrame.thread.state.pcall(nArguments - 1);
	}

	private static int print(LuaCallFrame callFrame, int nArguments) {
		LuaState state = callFrame.thread.state;
		LuaTable env = state.environment;
		Object toStringFun = state.tableGet(env, "tostring");
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < nArguments; i++) {
			Object res = state.call(toStringFun, callFrame.get(i), null, null);
			
			sb.append(res);
			if (i < nArguments) {
				sb.append("\t");
			}
		}
		state.out.println(sb.toString());
		return 0;
	}
	
	private static int select(LuaCallFrame callFrame, int nArguments) {
		luaAssert(nArguments >= 1, "Not enough arguments");
		Object arg1 = callFrame.get(0);
		if (arg1 instanceof String) {
			if (((String) arg1).startsWith("#")) {
				callFrame.push(LuaState.toDouble(nArguments - 1));
				return 1;
			}
		}
		Double d_indexDouble = rawTonumber(arg1);
		double d_index = LuaState.fromDouble(d_indexDouble);
		int index = (int) d_index;
		if (index >= 1 && index <= (nArguments - 1)) {
			int nResults = nArguments - index;
			return nResults;
		}
		return 0;		
	}

	public static void luaAssert(boolean b, String msg) {
		if (!b) {
			throw new RuntimeException(msg);
		}
	}

	public static String numberToString(Double num) {
		double n = num.doubleValue();
		if (Math.floor(n) == n) {
			return String.valueOf(num.longValue());
		}
		return num.toString();
	}
	
	public static Object getArg(LuaCallFrame callFrame, int n, String type,
				String function) {
		Object o = callFrame.get(n - 1);
		if (o == null) {
			throw new RuntimeException("bad argument #" + n + "to '" + function + 
				"' (" + type + " expected, got no value)");
		}
		// type coercion
		if (type == "string") {
			String res = rawTostring(o);
			if (res != null) {
				return res;
			}
		} else if (type == "number") {
			Double d = rawTonumber(o);
			if (d != null) {
				return d;
			}
			throw new RuntimeException("bad argument #" + n + " to '" + function +
			"' (number expected, got string)");
		}
		// type checking
		String isType = type(o);
		luaAssert(type == isType,
				"bad argument #" + n + " to '" + function +"' (" + type + 
				" expected, got " + isType + ")");
		return o;

	}

	public static Object getOptArg(LuaCallFrame callFrame, int n, String type) {
		Object o = callFrame.get(n);
		if (o == null) {
			return null;
		}
		// type coercion
		if (type == "string") {
			return rawTostring(o);
		} else if (type == "number") {
			return rawTonumber(o);
		}
		// no type checking, this is optional after all
		return o;
	}

	private static int getmetatable(LuaCallFrame callFrame, int nArguments) {
		luaAssert(nArguments >= 1, "Not enough arguments");
		Object o = callFrame.get(0);
		
		Object metatable = callFrame.thread.state.getmetatable(o, false);
		callFrame.push(metatable);
		return 1;
	}

	private static int setmetatable(LuaCallFrame callFrame, int nArguments) {
		luaAssert(nArguments >= 2, "Not enough arguments");
		
		Object o = callFrame.get(0);

		LuaTable newMeta = (LuaTable) (callFrame.get(1));
		setmetatable(callFrame.thread.state, o, newMeta, false);
		
		callFrame.setTop(1);
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
           	to.updateWeakSettings(weakKeys, weakValues);
		} else {			
			state.userdataMetatables.rawset(co, newMeta);
		}
	}

	private static int type(LuaCallFrame callFrame, int nArguments) {
		luaAssert(nArguments >= 1, "Not enough arguments");
		Object o = callFrame.get(0);
		callFrame.push(type(o));
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
		if (o instanceof LuaThread) return "thread";
		return "userdata"; 
	}

	private static int tostring(LuaCallFrame callFrame, int nArguments) {
		luaAssert(nArguments >= 1, "Not enough arguments");
		Object o = callFrame.get(0);
		Object res = tostring(o, callFrame.thread.state).intern();
		callFrame.push(res);
		return 1;
	}

	public static String tostring(Object o, LuaState state) {
		if (o == null) return "nil";
		if (o instanceof String) return (String) o;
		if (o instanceof Double) return rawTostring(o);
		if (o instanceof Boolean) return o == Boolean.TRUE ? "true" : "false";
		if (o instanceof JavaFunction) return "function 0x" + System.identityHashCode(o);
		if (o instanceof LuaClosure) return "function 0x" + System.identityHashCode(o);
		
		Object tostringFun = state.getMetaOp(o, "__tostring");
		if (tostringFun != null) {
			String res = (String) state.call(tostringFun, o, null, null);
			
			return res;
		}
		
		if (o instanceof LuaTable) return "table 0x" + System.identityHashCode(o);
		throw new RuntimeException("no __tostring found on object");
	}

	private static int tonumber(LuaCallFrame callFrame, int nArguments) {
		luaAssert(nArguments >= 1, "Not enough arguments");
		Object o = callFrame.get(0);

		if (nArguments == 1) {
			callFrame.push(rawTonumber(o));
			return 1;
		}

		String s = (String) o;

		Object radixObj = callFrame.get(1);
		Double radixDouble = rawTonumber(radixObj); 
		luaAssert(radixDouble != null, "Argument 2 must be a number");
		
		double dradix = LuaState.fromDouble(radixDouble);
		int radix = (int) dradix;
		if (radix != dradix) {
			throw new RuntimeException("base is not an integer");
		}
		Object res = tonumber(s, radix);
		callFrame.push(res);
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
				return Double.valueOf(s);
			} else {
				return LuaState.toDouble(Integer.parseInt(s, radix));
			}
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static int collectgarbage(LuaCallFrame callFrame, int nArguments) {
		Object option = null;
		if (nArguments > 0) {
			option = callFrame.get(0);
		}
		
		if (option == null || option == "step" || option == "collect") {
			System.gc();
			return 0;
		}

		if (option == "count") {
			long freeMemory = RUNTIME.freeMemory();
			long totalMemory = RUNTIME.totalMemory();
			callFrame.setTop(3);
			callFrame.set(0, toKiloBytes(totalMemory - freeMemory));
			callFrame.set(1, toKiloBytes(freeMemory));
			callFrame.set(2, toKiloBytes(totalMemory));
			return 3;
		}
		throw new RuntimeException("invalid option: " + option);
	}

	private static Double toKiloBytes(long freeMemory) {
		return LuaState.toDouble((double) (freeMemory) / 1024.0);
	}
	
	public static String rawTostring(Object o) {
		if (o instanceof String) {
			return (String) o;
		}
		if (o instanceof Double) {
			return numberToString((Double) o);
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
}
