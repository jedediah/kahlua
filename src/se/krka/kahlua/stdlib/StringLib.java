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
	private static final String HEXCHARS = "0123456789abcdefABCDEF";
	
	private static final int MINCAPLOC = 0;
	
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
		//case FIND: return Strlib.str_find(callFrame, nArguments);
		//case MATCH: return Strlib.str_match(callFrame, nArguments);
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
	
	// Pattern Matching
	
	private int findAux(LuaCallFrame callFrame, int nArguments, boolean find) {
		String f = find ? "find" : "match";
		String source = (String) BaseLib.getArg(callFrame, 1, BaseLib.TYPE_STRING, f);
		String pattern = (String) BaseLib.getArg(callFrame, 2, BaseLib.TYPE_STRING, f);
		Double i = (Double)(BaseLib.getOptArg(callFrame, 3, BaseLib.TYPE_NUMBER));
		boolean plain = LuaState.boolEval(BaseLib.getOptArg(callFrame, 4, BaseLib.TYPE_BOOLEAN));
		int init = 0;
		if (i != null) {
			init = i.intValue();
			if (init < 0) {
				// negative numbers count backwards from the end.
				init += source.length();
			} else if (init > 0) { 
				// lua strings reference characters starting at 1, so subtract 1 to translate to java
				init--;
			} // fall through case is if init = 0, in which case we leave it as is.
		}
		
		if (plain || noSpecialChars(pattern)) {  
			//  do a plain search
			int pos = source.indexOf(pattern, init);
			if (pos > -1) {
				if (find) { // if find is called, return the start and end pos of the pattern in the source string.
					return callFrame.push(LuaState.toDouble(pos + 1), LuaState.toDouble(pos + pattern.length()));
				} else { // if match is called, return the pattern itself since there's no special chars to parse.
					return callFrame.push(pattern);
				}
			}
		} else {
			boolean anchor = false;
			int pIndex = 0;
			int sIndex = init;
			if (pattern.charAt(0) == '^') {
				anchor = true;
				pIndex = 1;
			}
			do {
				int[][] captures = createCaptures();
				int level = match(captures, source, pattern, sIndex, pIndex);
				if (level > -1) {
					if (find) {
						int[] indices = captures[MAXCAPTURES];
						// shift base right by 2, add the 2 later
						return callFrame.push(new Double(indices[0]+1), new Double(indices[1])) + 
							pushCaptures(callFrame, source, captures, level, false);
					} else {
						boolean maincap = true;
						if (level > 1) // don't push the main match if there are captures in the pattern.
							maincap = false;
						return pushCaptures(callFrame, source, captures, level, maincap);
					}
				}
				sIndex++;
			} while (source.length() > sIndex && !anchor);
		}
		// find unsuccessful, return nil.
		return callFrame.push(null);
	}
	
	private boolean matchClass(char classIdentifier, char c) {
		boolean res;
		switch (Character.toLowerCase(classIdentifier)) {
	    case 'a': res = Character.isLowerCase(c) || Character.isUpperCase(c); break;
	    case 'c': res = (int)(c) < 32; break;
	    case 'd': res = Character.isDigit(c); break;
	    case 'l': res = Character.isLowerCase(c); break;
	    case 'p': res = c > 32 && matchClass('W',c); break;
	    //(PUNCTUATION.indexOf(c) > -1); break;
	    case 's': res = (c == 32 || (c > 8 && c < 14)); break;
	    case 'u': res = Character.isUpperCase(c); break;
	    case 'w': res = matchClass('a',c) || matchClass('d',c); break;
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
					return sig ? p.indexOf(']', index)+1 : -1;
				}
			} else if (c == pc) {
					return sig ? p.indexOf(']', index)+1 : -1;
			}
			index++;
		}
		return sig ? -1 : p.indexOf(']', index)+1;
	}

	private int singleMatch(char sc, String pattern,  int pIndex) {
		char pc = pattern.charAt(pIndex);
		switch (pc) {
			case '.': return pIndex + 1;
			case '%': 
				if (matchClass(pattern.charAt(pIndex + 1), sc)) {
					return pIndex + 2;
				} else {
					return -1;
				}
			case '[': return matchBracketClass(sc, pattern, pIndex+1);
			
			default: return (pc == sc) ? (pIndex + 1) : -1;
		}
	}
	
	private int match(int[][] captures, String source,
			String pattern, int sIndex, int pIndex) {
		int level = 0;
		int si = sIndex;
		int caplevel = 0;
		while (pIndex < pattern.length() && si <= source.length()) {
			//TODO: capture references (%0-%9), balanced matches
			switch (pattern.charAt(pIndex)) {
		    case '(':   // start a capture
				BaseLib.luaAssert(level < MAXCAPTURES, "too many captures");
		    	if (pattern.charAt(pIndex+1) == ')') {  //position capture?
		    		captures[level][0] = si;
		    		captures[level][1] = si;
		    		pIndex += 2;
		    	} else {
		    		captures[level][0] = si;
		    		caplevel++;
		    		pIndex++;
		    	}
	    		level++;
		    	continue;
		    case ')':  //end a capture
		    	if (caplevel > 0) {
		    		captures[captureToClose(captures, level)][1] = si;
		    		caplevel--;
		    		pIndex++;
		    		continue;
		    	} // don't break here since we want to check the ) normally too.
		    case '$': 
				if (pIndex == pattern.length()-1 && si == source.length()) {
					// match the end of string pattern char if we passed the end of the source.
					pIndex++;
					break;
				} // don't break here since we want to check the $ normally too.
		    default:
		    	if (si >= source.length())
		    		return -1;
		    
		    	int pi = classend(pattern,pIndex);
		    	//boolean m = si < source.length() && singleMatch(source.charAt(si), pattern, pi) > -1;
				//TODO: accumulators (*,+,-,?)
				if (pi < pattern.length()) {
					switch (pattern.charAt(pi)) {
					case '?':
					case '*':
					case '+':
					case '-':
					default:
						pIndex = singleMatch(source.charAt(si), pattern, pIndex);
						break;
					}
				} else {
					pIndex = singleMatch(source.charAt(si), pattern, pIndex);
				}
				
				if (pIndex < 0) 
					return -1;
				
				si++;
		    	break;
			}
		}
		// if the pattern isn't finished when the loop finishes, the pattern doesn't match or
		// if there are any open captures, don't match.
		if (pIndex < pattern.length() || caplevel > 0) 
			return -1; 
		// store the full match for find and if we didnt have any other captures.
		captures[MAXCAPTURES][0] = sIndex; 
		captures[MAXCAPTURES][1] = si;
		return level;
	}
	
	private int matchOptional(String source, int si, String pattern,
			int pIndex, boolean m) {
		
		return pIndex;
	}

	private int minExpand (int[][] captures, String source, int s,
            String pattern, int ep) {
		for (;;) {
		    int res = match(captures, source, pattern, s, ep+1);
		    if (res != -1)
		    	return res;
		    else if (s<source.length() && singleMatch(source.charAt(s), pattern, ep) > -1)
		    	s++;  // try with one more repetition
		    else 
		    	return -1;
		}
	}
	
	private int classend(String pattern, int pIndex) {
		switch (pattern.charAt(pIndex)) {
	    case '%': 
	    	pIndex++;
	        BaseLib.luaAssert(pIndex < pattern.length(), "malformed pattern (ends with %)");
	        return pIndex+1;
	    case '[': 
	    	pIndex++;
	    	if (pattern.charAt(pIndex) == '^') pIndex++;
	    	do {  // look for a `]' 
	    		BaseLib.luaAssert(pIndex < pattern.length(), "malformed pattern (missing ])");
	    		if (pattern.charAt(pIndex++) == '%' && pIndex != pattern.length())
	    			pIndex++;  // skip escapes (e.g. `%]') 
	    	} while (pattern.charAt(pIndex) != ']');
	    	return pIndex+1;
	    default: 
	    	return pIndex;
		}
	}
		
	private int[][] createCaptures() {
		int[][] result = new int[MAXCAPTURES + 1][2];// +1 for the initial search
		for (int i = 0; i < result.length; i++) {
			result[i][0] = 0;
			result[i][1] = -1;
		}
		return result;
	}
	
	private int captureToClose (int[][] captures, int level) {
		// if there are captures that are started but not finished, find the latest one
		int[] capture;
		for (int i = level; i >= MINCAPLOC; i--) {
			capture = captures[i];
			if (capture[0] != 0 && capture[1] == -1)
				return i;
		}
		return level;
	}
	
	private int pushCaptures(LuaCallFrame callFrame, String source, int[][] caps, 
			int level, boolean maincap) {
		int pushed = 0;
		int i = 0;
		for (int j = 0; j <= level; j++) {
			int from = caps[i][0];
			int to = caps[i++][1];
			pushed += pushCapture(callFrame, source, from, to);
		}
		if (maincap) {
			int from = caps[MAXCAPTURES][0];
			int to = caps[MAXCAPTURES][1];
						
			pushed += pushCapture(callFrame, source, from, to);
		}
		return pushed;
	}

	private int pushCapture(LuaCallFrame callFrame, String source, int from,
			int to) {
		if (from == to) {
			// location capture
			return callFrame.push(new Double(from+1));
		} else if (from < to) {
			return callFrame.push(source.substring(from, to).intern());
		}
		return 0;
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
