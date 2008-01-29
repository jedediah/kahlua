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
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;

public final class StringLib implements JavaFunction {

	private static final int SUB = 0;
	private static final int CHAR = 1;
	private static final int BYTE = 2;
	private static final int LOWER = 3;
	private static final int UPPER = 4;
	private static final int REVERSE = 5;
	private static final int FORMAT = 6;

	private static final int NUM_FUNCTIONS = 7;
	
	
	private static final String[] names;
	static {
		names = new String[NUM_FUNCTIONS];
		names[SUB] = "sub";
		names[CHAR] = "char";
		names[BYTE] = "byte";
		names[LOWER] = "lower";
		names[UPPER] = "upper";
		names[REVERSE] = "reverse";
		names[FORMAT] = "format";
	}

	private int index;
	private static StringLib[] functions;	
	static {
		functions = new StringLib[NUM_FUNCTIONS];
		for (int i = 0; i < NUM_FUNCTIONS; i++) {
			functions[i] = new StringLib(i);
		}
	}
	
	public StringLib(int index) {
		this.index = index;
	}

	public static void register(LuaState state) {
		LuaTable string = new LuaTable();
		state.environment.rawset("string", string);
		for (int i = 0; i < NUM_FUNCTIONS; i++) {
			string.rawset(names[i], functions[i]);
		}
		
		string.rawset("__index", string);
		state.setUserdataMetatable(String.class, string);
	}

	public String toString() {
		return names[index];
	}

	public int call(LuaState state, int base)  {
		int nArguments = state.top - base - 1;
		switch (index) {
		case SUB: return sub(state, base, nArguments);
		case CHAR: return stringChar(state, base, nArguments);
		case BYTE: return stringByte(state, base, nArguments);
		case LOWER: return lower(state, base, nArguments);
		case UPPER: return upper(state, base, nArguments);
		case REVERSE: return reverse(state, base, nArguments);
		case FORMAT: return format(state, base, nArguments);
		default:
			// Should never happen
			// throw new Error("Illegal function object");
			return 0;
		}
	}
	
	private long unsigned(Double o) {
		if (o == null) return 0;
		long v = o.longValue();
		if (v < 0L) v += (1L << 32);
		return v;
	}
	
	private String formatNumberByBase(Double num, int base, String digits) {
		long number = unsigned(num);
		StringBuffer result = new StringBuffer();
		while (number > 0) {
			result.append(digits.charAt((int)(number % base)));
			number /= base;
		}
		return result.reverse().toString(); // intern not needed here.
	}
	
	private int format(LuaState state, int base, int arguments) {
		final String lowerHex = "0123456789abcdef";
		final String upperHex = "0123456789ABCDEF";
		//BaseLib.luaAssert(arguments >= 1, "not enough arguments");
		Object o = BaseLib.getArg(state, base, 1, "string", "format");
		if (o instanceof Double) {
			// coerce number to string
			state.stack[base + 1] = ((Double) o).toString().intern();
			return 1;
		}
		String f = (String) o;
		int len = f.length();
		int argc = 2;
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < len; i++) {
			char c = f.charAt(i);
			if (c == '%') {
				i++;
				BaseLib.luaAssert(i < len, "invalid option '%' to 'format'");
				c = f.charAt(i);
				switch (c) {
				case '%': 
					result.append('%');
					break;
				case 'c':
					result.append((char)((Double)BaseLib.getArg(state, base, 
						argc, "number", "format")).intValue());
					break;
				case 'o':
					result.append(formatNumberByBase(
						(Double)BaseLib.getArg(state, base, argc, "number", "format"), 8,
							lowerHex));
					break;
				case 'x':
					result.append(formatNumberByBase(
						(Double)BaseLib.getArg(state, base, argc, "number", "format"), 16,
							lowerHex));
					break;
				case 'X':
					result.append(formatNumberByBase(
						(Double)BaseLib.getArg(state, base, argc, "number", "format"), 16,
							upperHex));
					break;
				case 'u':
					result.append(Long.toString(unsigned(
						(Double)BaseLib.getArg(state, base, argc, "number", "format"))));
					break;
				case 'd':
				case 'i':
					Double v = (Double)BaseLib.getArg(state, base, argc, "number", 
							"format");
					result.append(Long.toString(v.longValue()));
					break;
				case 'e':
				case 'E':
				case 'f':
					result.append(((Double)BaseLib.getArg(state, base, argc, "number", 
							"format")).toString());
					break;
				case 'G':
				case 'g':
					v = (Double)BaseLib.getArg(state, base, argc, "number", 
							"format");
					result.append(BaseLib.numberToString(v));
					break;
				case 's':
					o = BaseLib.getArg(state, base, argc, "string", "format");
					result.append((String)o);
					argc++;
					break;
				case 'q':
					String q = BaseLib.rawTostring(
							BaseLib.getArg(state, base, argc, "string", "format"));
					result.append('"');
					for (int j = 0; j < q.length(); j++) {
						char d = q.charAt(j);
						switch (d) {
						case '\\': result.append("\\"); break;
						case '\n': result.append("\\\n"); break;
						case '\r': result.append("\\r"); break;
						case '"': result.append("\\\""); break;
						default: result.append(d);
						}
					}
					result.append('"');
					argc++;
					break;
				default:
					throw new RuntimeException("invalid option '%" + c + 
							"' to 'format'");
				}
			} else {
				result.append(c);
			}
		}
		state.stack[base] = result.toString().intern();
		return 1;
	}
	
	private int lower(LuaState state, int base, int arguments) {
		BaseLib.luaAssert(arguments >= 1, "not enough arguments");
		String s = (String) state.stack[base + 1];

		state.stack[base] = s.toLowerCase().intern();
		return 1;
	}

	private int upper(LuaState state, int base, int arguments) {
		BaseLib.luaAssert(arguments >= 1, "not enough arguments");
		String s = (String) state.stack[base + 1];

		state.stack[base] = s.toUpperCase().intern();
		return 1;
	}
	
	private int reverse(LuaState state, int base, int arguments) {
		BaseLib.luaAssert(arguments >= 1, "not enough arguments");
		String s = (String) state.stack[base + 1];
		s = new StringBuffer(s).reverse().toString();
		state.stack[base] = s.intern();
		return 1;
	}
	
	private int stringByte(LuaState state, int base, int arguments) {
		BaseLib.luaAssert(arguments >= 1, "not enough arguments");
		String s = (String) state.stack[base + 1];
		
		Double di = null;
		Double dj = null;
		if (arguments >= 2) {
			di = (Double) state.stack[base + 2];
			if (arguments >= 3) {
				dj = (Double) state.stack[base + 3];
			}
		}
		double di2 = 1, dj2 = 1;
		if (di != null) {
			di2 = LuaState.fromDouble(di);
		}
		if (dj != null) {
			dj2 = LuaState.fromDouble(dj);
		}
		
		int ii = (int) di2, ij = (int) dj2;
		
		int len = s.length();
		ii = Math.min(ii, len); 
		ij = Math.min(ij, len);
		int nReturns = 1 +ij - ii;

		state.setTop(base + nReturns);
		int offset = ii - 1;
		for (int i = 0; i < nReturns; i++) {
			char c = s.charAt(offset + i);
			
			state.stack[base + i] = new Double((double) c); 
				
		}
		return nReturns;
	}

	private int stringChar(LuaState state, int base, int arguments) {
		StringBuffer sb = new StringBuffer();
		for (int i = 1; i <= arguments; i++) {
			double d = LuaState.fromDouble(state.stack[base + i]);
			int num = (int) d;
			sb.append((char) num);
		}
		state.stack[base] = sb.toString().intern();
		return 1;
	}

	private int sub(LuaState state, int base, int arguments) {
		BaseLib.luaAssert(arguments >= 2, "not enough arguments");
		String s = (String) state.stack[base + 1];
		double start = LuaState.fromDouble(state.stack[base + 2]);
		double end = -1;
		if (arguments >= 3) {
			end = LuaState.fromDouble(state.stack[base + 3]);
		}
		String res;
		int istart = (int) start;
		int iend = (int) end;
		
		if (iend < 0) {
			iend += s.length() + 1;
		}
		if (iend < 0) {
			iend = 0;
		}
		
		if (istart > iend) {
			state.stack[base] = "";
			return 1;
		}
		res = s.substring(istart - 1, iend);
		res = res.intern();
		
		state.stack[base] = res;
		return 1;
	}
}
