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
	private static final int FIND = 7;
	private static final int MATCH = 8;

	private static final int NUM_FUNCTIONS = 9;
	
	private static final int MAXCAPTURES = 32;
	private static final String SPECIALS = "^$*+?.([%-";
	private static final String PUNCTUATION = "!\"§$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
	private static final String HEXCHARS = "0123456789abcdefABCDEF";
	
	private static final String[] names;
	
	// NOTE: String.class won't work in J2ME - so this is used as a workaround
	private static final Class STRING_CLASS = "".getClass();
	
	static {
		names = new String[NUM_FUNCTIONS];
		names[SUB] = "sub";
		names[CHAR] = "char";
		names[BYTE] = "byte";
		names[LOWER] = "lower";
		names[UPPER] = "upper";
		names[REVERSE] = "reverse";
		names[FORMAT] = "format";
		names[FIND] = "find";
		names[MATCH] = "match";
	}

	private int methodId;
	private static StringLib[] functions;	
	static {
		functions = new StringLib[NUM_FUNCTIONS];
		for (int i = 0; i < NUM_FUNCTIONS; i++) {
			functions[i] = new StringLib(i);
		}
	}
	
	public StringLib(int index) {
		this.methodId = index;
	}

	public static void register(LuaState state) {
		LuaTable string = new LuaTable();
		state.environment.rawset("string", string);
		for (int i = 0; i < NUM_FUNCTIONS; i++) {
			string.rawset(names[i], functions[i]);
		}
		
		string.rawset("__index", string);
		state.setUserdataMetatable(STRING_CLASS, string);
	}

	public String toString() {
		return names[methodId];
	}

	public int call(LuaCallFrame callFrame, int nArguments)  {
		switch (methodId) {
		case SUB: return sub(callFrame, nArguments);
		case CHAR: return stringChar(callFrame, nArguments);
		case BYTE: return stringByte(callFrame, nArguments);
		case LOWER: return lower(callFrame, nArguments);
		case UPPER: return upper(callFrame, nArguments);
		case REVERSE: return reverse(callFrame, nArguments);
		case FORMAT: return format(callFrame, nArguments);
		case FIND: return findAux(callFrame, nArguments, true);
		case MATCH: return findAux(callFrame, nArguments, false);
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
	
	private String formatNumberByBase(Double num, int base) {
		long number = unsigned(num);
		return Long.toString(number, base);
	}
	
	private int format(LuaCallFrame callFrame, int nArguments) {
		//BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
		String f = (String) BaseLib.getArg(callFrame, 1, "string", "format");
		
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
					result.append((char)((Double)BaseLib.getArg(callFrame, 
						argc, "number", "format")).intValue());
					break;
				case 'o':
					result.append(formatNumberByBase(
						(Double)BaseLib.getArg(callFrame, argc, "number", "format"), 8));
					break;
				case 'x':
					result.append(formatNumberByBase(
						(Double)BaseLib.getArg(callFrame, argc, "number", "format"), 16));
					break;
				case 'X':
					result.append(formatNumberByBase(
						(Double)BaseLib.getArg(callFrame, argc, "number", "format"), 16).toUpperCase());
					break;
				case 'u':
					result.append(Long.toString(unsigned(
						(Double)BaseLib.getArg(callFrame, argc, "number", "format"))));
					break;
				case 'd':
				case 'i':
					Double v = (Double)BaseLib.getArg(callFrame, argc, "number", 
							"format");
					result.append(Long.toString(v.longValue()));
					break;
				case 'e':
				case 'E':
				case 'f':
					result.append(((Double)BaseLib.getArg(callFrame, argc, "number", 
							"format")).toString());
					break;
				case 'G':
				case 'g':
					v = (Double)BaseLib.getArg(callFrame, argc, "number", 
							"format");
					result.append(BaseLib.numberToString(v));
					break;
				case 's': {
					String s = (String) BaseLib.getArg(callFrame, argc, "string", "format");
					result.append(s);
					argc++;
					break;
				}
				case 'q':
					String q = BaseLib.rawTostring(
							BaseLib.getArg(callFrame, argc, "string", "format"));
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
		callFrame.push(result.toString().intern());
		return 1;
	}
	
	private int lower(LuaCallFrame callFrame, int nArguments) {
		BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
		String s = (String) callFrame.get(0);

		callFrame.push(s.toLowerCase().intern());
		return 1;
	}

	private int upper(LuaCallFrame callFrame, int nArguments) {
		BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
		String s = (String) callFrame.get(0);

		callFrame.push(s.toUpperCase().intern());
		return 1;
	}
	
	private int reverse(LuaCallFrame callFrame, int nArguments) {
		BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
		String s = (String) callFrame.get(0);
		s = new StringBuffer(s).reverse().toString();
		callFrame.push(s.intern());
		return 1;
	}
	
	private int stringByte(LuaCallFrame callFrame, int nArguments) {
		BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
		String s = (String) callFrame.get(0);
		
		Double di = null;
		Double dj = null;
		if (nArguments >= 2) {
			di = BaseLib.rawTonumber(callFrame.get(1));
			if (nArguments >= 3) {
				dj = BaseLib.rawTonumber(callFrame.get(2));
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

		callFrame.setTop(nReturns);
		int offset = ii - 1;
		for (int i = 0; i < nReturns; i++) {
			char c = s.charAt(offset + i);

			callFrame.set(i, new Double((double) c)); 
				
		}
		return nReturns;
	}

	private int stringChar(LuaCallFrame callFrame, int nArguments) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < nArguments; i++) {
			double d = LuaState.fromDouble(callFrame.get(i));
			int num = (int) d;
			sb.append((char) num);
		}
		callFrame.push(sb.toString().intern());
		return 1;
	}

	private int sub(LuaCallFrame callFrame, int nArguments) {
		String s = (String) callFrame.get(0);
		double start = LuaState.fromDouble(callFrame.get(1));
		double end = -1;
		if (nArguments >= 3) {
			end = LuaState.fromDouble(callFrame.get(2));
		}
		String res;
		int istart = (int) start;
		int iend = (int) end;

		if (istart < 0) {
			istart += Math.max(0, s.length() + 1);
		}
		
		if (iend < 0) {
			iend += Math.max(0, s.length() + 1);
		}
		
		if (istart > iend) {
			callFrame.push("");
			return 1;
		}
		res = s.substring(istart - 1, iend);
		res = res.intern();

		callFrame.push(res);
		return 1;
	}
	
	private boolean matchClass(char classIdentifier, char c) {
		boolean res;
		switch (Character.toLowerCase(classIdentifier)) {
	    case 'a': res = Character.isLowerCase(c) || Character.isUpperCase(c); break;
	    case 'c': res = (int)(c) < 32; break;
	    case 'd': res = Character.isDigit(c); break;
	    case 'l': res = Character.isLowerCase(c); break;
	    case 'p': res = (PUNCTUATION.indexOf(c) > -1); break;
	    case 's': res = (c == 32 || (c > 8 && c < 14)); break;
	    case 'u': res = Character.isUpperCase(c); break;
	    case 'w': res = Character.isDigit(c) || Character.isLowerCase(c) ||
				Character.isUpperCase(c); break;
	    case 'x': res = (HEXCHARS.indexOf(c) > -1); break;
	    case 'z': res = (c == 0); break;
	    default: return (classIdentifier == c);
		}
		return Character.isLowerCase(classIdentifier) ? res : !res;
	}

	private int matchBracketClass(char c, String p, int index) { 
		boolean sig = true;
		int len = p.length();
		if (p.charAt(index) == '^') { // index *after* opening bracket
			sig = false;
			index++; // skip the '^'
		}
		while (index < len) {
			char pc = p.charAt(index);
			if (pc == '%') {
				index++;
				if (index < len && matchClass(p.charAt(index), c)) {
					return sig ? p.indexOf(']', index) : -1;
				}
			} else if (pc == ']') {
				return -1;
			} else if (index + 2 < len && p.charAt(index + 1) == '-') {
				if (c >= pc && c <= p.charAt(index + 2)) {
					return sig ? p.indexOf(']', index) : -1;
				}
			} else if (c == pc) {
					return sig ? p.indexOf(']', index) : -1;
			}
			index++;
		}
		return sig ? -1 : p.indexOf(']', index);
	}

	private int singleMatch(char sc, String pattern, int pIndex) {
		char pc = pattern.charAt(pIndex);
		switch (pc) {
			case '.': return pIndex + 1;
			case '%': 
				if (matchClass(pattern.charAt(pIndex + 1), sc)) {
					return pIndex + 2;
				} else {
					return -1;
				}
			case '[': return matchBracketClass(sc, pattern, pIndex);
			default: return (pc == sc) ? (pIndex + 1) : -1;
		}
	}
	
	private int match(String source, String pattern,
			int sIndex, int pIndex, int[] captures, int level) {
		int sLen = source.length();
		int pLen = pattern.length();
		int si = sIndex;
		while (pIndex < pLen && si < sLen) {
			//TODO: captures, etc.
			pIndex = singleMatch(source.charAt(si), pattern, pIndex);
			if (pIndex < 0) return -1;
			si++;
		}
		captures[0] = sIndex + 1;
		captures[1] = si;
		return level;
	}
	
	private int findAux(LuaCallFrame callFrame, int nArguments, boolean find) {
		String f = find?"find":"match";
		String source = (String) BaseLib.getArg(callFrame, 1, BaseLib.TYPE_STRING, f);
		String pattern = (String) BaseLib.getArg(callFrame, 2, BaseLib.TYPE_STRING, f);
		Double i = (Double)(BaseLib.getOptArg(callFrame, 3, BaseLib.TYPE_NUMBER));
		boolean plain = LuaState.boolEval(BaseLib.getOptArg(callFrame, 4, BaseLib.TYPE_BOOLEAN));
		int init = 0;
		if (i != null) {
			init = i.intValue() - 1;
			if (init < 0) { 
				init = 0;
			} else if (init >= source.length()) {
				init = source.length()-1;
			}
		}
		if (plain || noSpecialChars(pattern)) {  
			//  do a plain search
			int pos = source.indexOf(pattern, init);
			if (pos > -1) {
				if (find) { // if find is called, return the start and end pos of the pattern in the source string.
					callFrame.push(LuaState.toDouble(pos + 1), LuaState.toDouble(pos + pattern.length()));
					return 2;
				} else { // if match is called, return the pattern itself since there's no special chars.
					callFrame.push(pattern);
					return 1;
				}
			} else {
				return 0; // find unsuccessful, returns nil.
			}
		} else {
			boolean anchor = false;
			int pIndex = 0;
			int sIndex = 0;
			if (pattern.charAt(0) == '^') {
				anchor = true;
				pIndex = 1;
			}
			do {
				int[] captures = createCaptures();
				int level = match(source, pattern, sIndex, pIndex, captures, 1);
				if (level > -1) {
					if (find) {
						callFrame.push(new Double(captures[0]), new Double(captures[0] + captures[1] - 1));
						// shift base right by 2, add the 2 later
						pushCaptures(callFrame, source, captures, level);
						return level + 2;
					} else {
						pushCaptures(callFrame, source, captures, level);
						return level;
					}
				}
				sIndex++;
			} while (source.length() > sIndex && !anchor);
		}
		return 0;
	}

	private int[] createCaptures() {
		int[] result = new int[MAXCAPTURES * 2];
		for (int i = 0; i < MAXCAPTURES * 2; i += 2) {
			result[i] = 0;
			result[i + 1] = -1;
		}
		return result;
	}
	
	private void pushCaptures(LuaCallFrame callFrame, String source, int[] caps, 
			int level) {
		int i = 0;
		for (int j = 0; j < level; j ++) {
			int from = caps[i++];
			int to = caps[i++];
			if (from == to) {
				// location capture
				callFrame.push(new Double(from));
			} else {
				callFrame.push(source.substring(from - 1, to).intern());
			}
		}
	}
	
	private boolean noSpecialChars(String pattern) {
		for (int i = 0; i < SPECIALS.length(); i++) {
			if (pattern.indexOf(SPECIALS.charAt(i)) > -1) {
				return false;
			}
		}
		return true;
	}
}
