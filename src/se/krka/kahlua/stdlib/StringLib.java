/*
Copyright (c) 2007-2009 Kristofer Karlsson <kristofer.karlsson@gmail.com>

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
	private static final int GSUB = 9;

	private static final int NUM_FUNCTIONS = 10;

	private static final String SPECIALS = "^$*+?.([%-";
	private static final int LUA_MAXCAPTURES = 32;
	private static final char L_ESC = '%';
	private static final int CAP_UNFINISHED = ( -1 );
	private static final int CAP_POSITION = ( -2 );

	private static final String[] names;
	private static StringLib[] functions;  

	// NOTE: String.class won't work in J2ME - so this is used as a workaround
	private static final Class STRING_CLASS = "".getClass();
	
	private static final int MAX_DOUBLE_PRECISION = 10;

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
		names[GSUB] = "gsub";

		functions = new StringLib[NUM_FUNCTIONS];
		for (int i = 0; i < NUM_FUNCTIONS; i++) {
			functions[i] = new StringLib(i);
		}
	}

	private int methodId;       
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
		case FIND: return findAux(callFrame, true);
		case MATCH: return findAux(callFrame, false);
		case GSUB: return gsub(callFrame, nArguments);
		default: return 0; // Should never happen.
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

	private String pad(String value, int width, boolean leftJustify, char pad) {
		int paddingNeeded = width - value.length();
		if (paddingNeeded <= 0) {
			return value;
		}
		// NOTE: this could be optimized similar to how integer power is implemented.
		StringBuffer sb = new StringBuffer();
		
		if (leftJustify) {
			sb.append(value);
		}
		for (int i = 0; i < paddingNeeded; i++) {
			sb.append(pad);
		}
		if (!leftJustify && value.length() > 0) {
			
			// Annoying hack to avoid printing numbers as 0000+12.34 instead of
			// the more appropiate +000012.34
			char c = value.charAt(0);
			if (pad == '0' && (c == '-' || c == '+' || c == ' ')) {
				sb.insert(0, c);
				sb.append(value.substring(1));
			} else {
				sb.append(value);
			}
		}
		return sb.toString();
	}
	
	private int format(LuaCallFrame callFrame, int nArguments) {
		String f = (String) BaseLib.getArg(callFrame, 1, BaseLib.TYPE_STRING, "format");

		int len = f.length();
		int argc = 2;
		StringBuffer result = new StringBuffer();
		for (int i = 0; i < len; i++) {
			char c = f.charAt(i);
			if (c == '%') {
				i++;
				BaseLib.luaAssert(i < len, "incomplete option to 'format'");
				c = f.charAt(i);
				if (c == '%') {
					result.append('%');
				} else {
					// Detect flags
					boolean repr = false;
					boolean zeroPadding = false;
					boolean leftJustify = false;
					boolean showPlus = false;
					boolean spaceForSign = false;
					flagLoop: while (true) {
						switch (c) {
						case '-':
							leftJustify = true;
							break;
						case '+':
							showPlus = true;
							break;
						case ' ':
							spaceForSign = true;
							break;
						case '#':
							repr = true;
							break;
						case '0':
							zeroPadding = true;
							break;
						default:
							break flagLoop;
						}
						i++;
						BaseLib.luaAssert(i < len, "incomplete option to 'format'");
						c = f.charAt(i);
					}

					// Detect width
					int width = 0;
					while (c >= '0' && c <= '9') {
						width = 10 * width + (int) (c - '0');
						i++;
						BaseLib.luaAssert(i < len, "incomplete option to 'format'");
						c = f.charAt(i);
					}
					
					// Detect precision
					int precision = 0;
					boolean hasPrecision = false;
					if (c == '.') {
						hasPrecision = true;
						i++;
						BaseLib.luaAssert(i < len, "incomplete option to 'format'");
						c = f.charAt(i);

						while (c >= '0' && c <= '9') {
							precision = 10 * precision + (int) (c - '0');
							i++;
							BaseLib.luaAssert(i < len, "incomplete option to 'format'");
							c = f.charAt(i);
						}
					}
					
					if (leftJustify) {
						zeroPadding = false;
					}
					// This will be overriden to space for the appropiate specifiers
					char padCharacter = zeroPadding ? '0' : ' ';

					// Detect specifier and compute result
					String formatResult;
					switch (c) {
					case 'c':
						formatResult = String.valueOf((char)(getDoubleArg(callFrame, argc)).intValue());
						padCharacter = ' ';
						break;
					case 'o':
						formatResult = formatNumberByBase(getDoubleArg(callFrame, argc), 8);
						if (repr && !formatResult.equals("0")) {
							formatResult = "0" + formatResult;
						}
						break;
					case 'x':
					case 'X':
						formatResult = formatNumberByBase(getDoubleArg(callFrame, argc), 16);
						if (repr && !formatResult.equals("0")) {
							formatResult = "0" + c + formatResult;
						}
						if (c == 'X') {
							formatResult = formatResult.toUpperCase();
						}
						break;
					case 'u':
						formatResult = Long.toString(unsigned(
								getDoubleArg(callFrame, argc)));
						break;
					case 'd':
					case 'i':
						Double v = getDoubleArg(callFrame, argc);
						formatResult = Long.toString(v.longValue());
						formatResult = handleSign(showPlus, spaceForSign, formatResult);
						break;
					case 'e':
					case 'E':
						if (!hasPrecision) {
							precision = 6;
						}
						v = getDoubleArg(callFrame, argc);
						if (v.isInfinite() || v.isNaN()) {
							formatResult = v.toString();
						} else {
							formatResult = getScientificFormat(v.doubleValue(), precision, repr, c, false);
						}
						formatResult = handleSign(showPlus, spaceForSign, formatResult);
						break;
					case 'f':
						if (!hasPrecision) {
							precision = 6;
						}
						v = getDoubleArg(callFrame, argc);
						if (v.isInfinite() || v.isNaN()) {
							formatResult = v.toString();
						} else {
							formatResult = toPrecision(v.doubleValue(), precision, repr);
						}
						formatResult = handleSign(showPlus, spaceForSign, formatResult);
						break;
					case 'g':
					case 'G':
					{
						// Default precision
						if (!hasPrecision) {
							precision = 6;
						}
						
						// Precision is significant digits for %g
						if (precision <= 0) {
							precision = 1;
						}
						
						// first round to correct significant digits (precision),
						// then check which formatting to be used.
						v = getDoubleArg(callFrame, argc);
						if (v.isInfinite() || v.isNaN()) {
							formatResult = v.toString();
						} else {
							double x = MathLib.roundToSignificantNumbers(v.doubleValue(), precision);

							/*
							 * Choose %f version if:
							 *     |v| >= 10^(-4)
							 * AND
							 *     |v| < 10^(precision)
							 *     
							 * otherwise, choose %e
							 */ 
							double xAbs = Math.abs(x);
							if (xAbs >= 1e-4 && xAbs < MathLib.ipow(10, precision)) {
								long longValue = (long) xAbs;
								int iPartSize;
								if (longValue == 0) {
									iPartSize = 0;
								} else {
									iPartSize = 1;
									while (longValue >= 10) {
										longValue /= 10;
										iPartSize++;
									}
								}
								// format with %f, with precision significant numbers
								formatResult = toSignificantNumbers(x, precision - iPartSize, repr);								
							} else {
								// format with %g, with precision significant numbers, i.e. precision -1 digits
								// but skip trailing zeros unless repr
								formatResult = getScientificFormat(x, precision - 1, repr, (char) (c - 2), true);
							}
						}
						formatResult = handleSign(showPlus, spaceForSign, formatResult);
						break;
					}
					case 's': {
						padCharacter = ' ';
						String s = getStringArg(callFrame, argc);
						if (hasPrecision) {
							s = s.substring(0, Math.min(precision, s.length()));
						}
						formatResult = s;
						argc++;
						break;
					}
					case 'q':
						String q = getStringArg(callFrame, argc);
						
						// Write directly to the buffer instead, since %q does not support any options
						formatResult = "";
						//padCharacter = ' '; // This one isn't needed since padding does not occur for %q
						
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
					result.append(pad(formatResult,	width, leftJustify, padCharacter));
				}
			} else {
				result.append(c);
			}
		}
		callFrame.push(result.toString());
		return 1;
	}

	private String toPrecision(double number, int precision, boolean requirePeriod) {
		number = MathLib.roundToPrecision(number, Math.min(MAX_DOUBLE_PRECISION, precision));
		
		double absNumber = Math.abs(number);
		double iPart = Math.floor(absNumber);
		double fPart = absNumber - iPart;
		for (int i = 0; i < precision; i++) {
			fPart *= 10.0;
		}
		long fPartLong = (long) Math.ceil(fPart - 0.5);

		String iPartString = Long.toString((long) iPart);
		String fPartString;
		if (fPartLong == 0) {
			fPartString = "";
		} else {
			fPartString = Long.toString(fPartLong);
		}
		fPartString = pad(fPartString, precision, false, '0');
		
		String negative = (number < 0) ? "-" : "";
		if (precision == 0) {
			return negative + iPartString + (requirePeriod ? "." : "");
		} else {
			return negative + iPartString + "." + fPartString;
		}
	}

	private String toSignificantNumbers(double number, int significantDecimals, boolean includeTrailingZeros) {
		double absNumber = Math.abs(number);
		double iPart = Math.floor(absNumber);
		String iPartString = Long.toString((long) iPart);
		
		double fPart = absNumber - iPart;
		
		boolean hasNotStarted = iPartString.equals("0") && fPart != 0;
		int zeroPaddingBefore = 0;
		for (int i = 0; i < zeroPaddingBefore + significantDecimals; i++) {
			fPart *= 10.0;
			if (hasNotStarted && Math.floor(fPart) == 0) {
				zeroPaddingBefore++;
			}
		}
		long fPartLong = (long) Math.ceil(fPart - 0.5);

		if (!includeTrailingZeros) {
			while (fPartLong > 0 && (fPartLong % 10) == 0) {
				fPartLong /= 10;
				significantDecimals--;
			}
		}
		
		String fPartString;
		if (fPartLong == 0) {
			fPartString = "";
		} else {
			fPartString = Long.toString(fPartLong);
		}
		
		if (includeTrailingZeros && fPartString.length() < significantDecimals) {
			fPartString = pad(fPartString, significantDecimals, true, '0');
		}
		fPartString = pad(fPartString, zeroPaddingBefore + fPartString.length(), false, '0');
		
		String negative = (number < 0) ? "-" : "";
		if (fPartString.equals("")) {
			return negative + iPartString + (includeTrailingZeros ? "." : "");
		} else {
			return negative + iPartString + "." + fPartString;
		}
	}

	private String getScientificFormat(double x, int precision, boolean repr, char expChar, boolean useSignificantNumbers) {
		double xAbs = Math.abs(x);
		int exponent = 0;
		char expSign;
		if (xAbs >= 1.0) {
			while (xAbs >= 10.0) {
				xAbs /= 10.0;
				exponent++;
			}
			expSign = '+';
		} else {
			while (xAbs < 1.0) {
				xAbs *= 10.0;
				exponent--;
			}
			expSign = '-';
		}
		int absExponent = Math.abs(exponent);
		return
		(x < 0 ? "-" : "") +
		(useSignificantNumbers ? toSignificantNumbers(xAbs, precision, repr) : toPrecision(xAbs, precision, repr)) +
		expChar + expSign + (absExponent < 10 ? "0" : "") + absExponent; 
	}

	private String handleSign(boolean showPlus, boolean spaceForSign, String formatResult) {
		if (formatResult.length() > 0 && formatResult.charAt(0) != '-') {
			if (showPlus) {
				return "+" + formatResult;
			}
			if (spaceForSign) {
				return " " + formatResult;
			}
		}
		return formatResult;
	}

	private String getStringArg(LuaCallFrame callFrame, int argc) {
		return (String) BaseLib.getArg(callFrame, argc, "string", "format");
	}

	private Double getDoubleArg(LuaCallFrame callFrame, int argc) {
		return (Double)BaseLib.getArg(callFrame, argc, "number", "format");
	}

	private int lower(LuaCallFrame callFrame, int nArguments) {
		BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
		String s = (String) callFrame.get(0);

		callFrame.push(s.toLowerCase());
		return 1;
	}

	private int upper(LuaCallFrame callFrame, int nArguments) {
		BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
		String s = (String) callFrame.get(0);

		callFrame.push(s.toUpperCase());
		return 1;
	}

	private int reverse(LuaCallFrame callFrame, int nArguments) {
		BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
		String s = (String) callFrame.get(0);
		s = new StringBuffer(s).reverse().toString();
		callFrame.push(s);
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
		return callFrame.push(sb.toString());
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

		return callFrame.push(res);
	}

	/* Pattern Matching
	 * Original code that this was adapted from is copyright (c) 2008 groundspeak, inc.
	 */

	public static class MatchState {

		public MatchState () {
			capture = new Capture[ LUA_MAXCAPTURES ];
			for ( int i = 0; i < LUA_MAXCAPTURES; i ++ ) {
				capture[i] = new Capture ();
			}
		}
		public StringPointer src_init;  /* init of source string */

		public int endIndex; /* end (`\0') of source string */

		public LuaCallFrame callFrame;
		public int level;  /* total number of captures (finished or unfinished) */

		public Capture[] capture;

		public static class Capture {

			public StringPointer init;
			public int len;
		}

		public Object[] getCaptures() {
			Object[] caps = new String[level];
			for (int i = 0; i < level; i++) {
				if (capture[i].len == CAP_POSITION) {
					caps[i] = new Double(src_init.length() - capture[i].init.length() + 1);
				} else {
					caps[i] = capture[i].init.getString().substring(0, capture[i].len);
				}
			}
			return caps;
		}
	}

	public static class StringPointer {

		private String string;
		private int index = 0;

		public StringPointer(String original) {
			this.string = original;
		}

		public StringPointer(String original, int index) {
			this.string = original;
			this.index = index;
		}

		public StringPointer getClone() {
			StringPointer newSP = new StringPointer( this.getOriginalString(), this.getIndex() );
			return newSP;
		}

		public int getIndex () {
			return index;
		}

		public void setIndex ( int ind ) {
			index = ind;
		}

		public String getOriginalString () {
			return string;
		}

		public void setOriginalString(String orStr) {
			string = orStr;
		}

		public String getString() {
			return getString(0);
		}

		public String getString(int i) {
			return string.substring ( index + i, string.length () );
		}

		public char getChar() {
			return getChar(0);
		}

		public char getChar(int strIndex) {
			if ( index + strIndex >= string.length () )
				return '\0';
			else
				return string.charAt ( index + strIndex );
		}

		public int length() {
			return string.length () - index;
		}

		public int postIncrStringI ( int num ) {
			int oldIndex = index;
			index += num;
			return oldIndex;
		}

		public int preIncrStringI ( int num ) {
			index += num;
			return index;
		}

		public char postIncrString ( int num ) {
			char c = getChar();
			index += num;
			return c;
		}

		public char preIncrString ( int num ) {
			index += num;
			return getChar();
		}

		public int compareTo(StringPointer cmp, int len) {
			return this.string.substring(this.index,this.index+len).compareTo(
					cmp.string.substring(cmp.index, cmp.index+len));
		}
	}

	private static Object push_onecapture ( MatchState ms, int i, StringPointer s, StringPointer e ) {
		if (i >= ms.level) {
			if ( i == 0 ) { // ms->level == 0, too
				String res = s.string.substring(s.index, e.index);
				ms.callFrame.push(res);
				return res;
			} else {
				throw new RuntimeException("invalid capture index");
			}
		} else {
			int l = ms.capture[i].len;
			if (l == CAP_UNFINISHED) {
				throw new RuntimeException("unfinished capture");
			} else if (l == CAP_POSITION) {
				Double res = new Double(ms.src_init.length() - ms.capture[i].init.length() + 1);
				ms.callFrame.push(res);
				return res;
			} else {
				int index = ms.capture[i].init.index;
				String res = ms.capture[i].init.string.substring(index, index+l);
				ms.callFrame.push(res);
				return res;
			}
		}
	}

	private static int push_captures ( MatchState ms, StringPointer s, StringPointer e ) {
		int nlevels = ( ms.level == 0 && s != null ) ? 1 : ms.level;
		BaseLib.luaAssert(nlevels <= LUA_MAXCAPTURES, "too many captures");
		for (int i = 0; i < nlevels; i++) {
			push_onecapture (ms, i, s, e);
		}
		return nlevels;  // number of strings pushed
	}

	private static boolean noSpecialChars(String pattern) {
		for (int i = 0; i < SPECIALS.length(); i++) {
			if (pattern.indexOf(SPECIALS.charAt(i)) > -1) {
				return false;
			}
		}
		return true;
	}

	private static int findAux (LuaCallFrame callFrame, boolean find ) {
		String f = find ? names[FIND] : names[MATCH];
		String source = (String) BaseLib.getArg(callFrame, 1, BaseLib.TYPE_STRING, f);
		String pattern = (String) BaseLib.getArg(callFrame, 2, BaseLib.TYPE_STRING, f);
		Double i = ((Double)(BaseLib.getOptArg(callFrame, 3, BaseLib.TYPE_NUMBER)));
		boolean plain = LuaState.boolEval(BaseLib.getOptArg(callFrame, 4, BaseLib.TYPE_BOOLEAN));
		int init = (i == null ? 0 : i.intValue() - 1);

		if ( init < 0 ) {
			// negative numbers count back from the end of the string.
			init += source.length();
			if ( init < 0 ) {
				init = 0; // if we are still negative, just start at the beginning.
			}
		} else if ( init > source.length() ) {
			init = source.length();
		}

		if ( find && ( plain || noSpecialChars(pattern) ) ) { // explicit plain request or no special characters?
			// do a plain search
			int pos = source.indexOf(pattern, init);
			if ( pos > -1 ) {
				return callFrame.push(LuaState.toDouble(pos + 1), LuaState.toDouble(pos + pattern.length()));
			}
		} else {
			StringPointer s = new StringPointer(source);
			StringPointer p = new StringPointer(pattern);

			MatchState ms = new MatchState ();
			boolean anchor = false;
			if ( p.getChar () == '^' ) {
				anchor = true;
				p.postIncrString ( 1 );
			}
			StringPointer s1 = s.getClone();
			s1.postIncrString ( init );

			ms.callFrame = callFrame;
			ms.src_init = s.getClone();
			ms.endIndex = s.getString().length();
			do {
				StringPointer res;
				ms.level = 0;
				if ( ( res = match ( ms, s1, p ) ) != null ) {
					if ( find ) {
						return callFrame.push(new Double(s.length () - s1.length () + 1), new Double(s.length () - res.length ())) +
						push_captures ( ms, null, null );
					} else {
						return push_captures ( ms, s1, res );
					}
				}

			} while ( s1.postIncrStringI ( 1 ) < ms.endIndex && !anchor );
		}
		return callFrame.pushNil();  // not found
	}

	private static StringPointer startCapture ( MatchState ms, StringPointer s, StringPointer p, int what ) {
		StringPointer res;
		int level = ms.level;
		BaseLib.luaAssert(level < LUA_MAXCAPTURES, "too many captures");

		ms.capture[level].init = s.getClone();
		ms.capture[level].init.setIndex ( s.getIndex () );
		ms.capture[level].len = what;
		ms.level = level + 1;
		if ( ( res = match ( ms, s, p ) ) == null ) /* match failed? */ {
			ms.level --;  /* undo capture */
		}
		return res;
	}

	private static int captureToClose ( MatchState ms ) {
		int level = ms.level;
		for ( level --; level >= 0; level -- ) {
			if ( ms.capture[level].len == CAP_UNFINISHED ) {
				return level;
			}
		}
		throw new RuntimeException("invalid pattern capture");
	}

	private static StringPointer endCapture ( MatchState ms, StringPointer s, StringPointer p ) {
		int l = captureToClose ( ms );
		StringPointer res;
		ms.capture[l].len = ms.capture[l].init.length () - s.length ();  /* close capture */
		if ( ( res = match ( ms, s, p ) ) == null ) /* match failed? */ {
			ms.capture[l].len = CAP_UNFINISHED;  /* undo capture */
		}
		return res;
	}

	private static int checkCapture ( MatchState ms, int l ) {
		l -= '1'; // convert chars 1-9 to actual ints 1-9
		BaseLib.luaAssert(l < 0 || l >= ms.level || ms.capture[l].len == CAP_UNFINISHED,
		"invalid capture index");
		return l;
	}

	private static StringPointer matchCapture ( MatchState ms, StringPointer s, int l ) {
		int len;
		l = checkCapture ( ms, l );
		len = ms.capture[l].len;
		if ( ( ms.endIndex - s.length () ) >= len && ms.capture[l].init.compareTo(s, len) == 0 ) {
			StringPointer sp = s.getClone();
			sp.postIncrString ( len );
			return sp;
		}
		else {
			return null;
		}
	}

	private static StringPointer matchBalance ( MatchState ms, StringPointer ss, StringPointer p ) {

		BaseLib.luaAssert(!(p.getChar () == 0 || p.getChar ( 1 ) == 0), "unbalanced pattern");

		StringPointer s = ss.getClone();
		if ( s.getChar () != p.getChar () ) {
			return null;
		} else {
			int b = p.getChar ();
			int e = p.getChar ( 1 );
			int cont = 1;

			while ( s.preIncrStringI ( 1 ) < ms.endIndex ) {
				if ( s.getChar () == e ) {
					if (  -- cont == 0 ) {
						StringPointer sp = s.getClone();
						sp.postIncrString ( 1 );
						return sp;
					}
				} else if ( s.getChar () == b ) {
					cont ++;
				}
			}
		}
		return null;  /* string ends out of balance */
	}

	private static StringPointer classEnd ( MatchState ms, StringPointer pp ) {
		StringPointer p = pp.getClone();
		switch ( p.postIncrString ( 1 ) ) {
		case L_ESC: {
			BaseLib.luaAssert(p.getChar () != '\0', "malformed pattern (ends with '%%')");
			p.postIncrString ( 1 );
			return p;
		}
		case '[': {
			if ( p.getChar () == '^' ) {
				p.postIncrString ( 1 );
			}
			do { // look for a `]' 
				BaseLib.luaAssert(p.getChar () != '\0', "malformed pattern (missing ']')");

				if ( p.postIncrString ( 1 ) == L_ESC && p.getChar () != '\0' ) {
					p.postIncrString ( 1 );  // skip escapes (e.g. `%]')
				}

			} while ( p.getChar () != ']' );

			p.postIncrString ( 1 );
			return p;
		}
		default: {
			return p;
		}
		}
	}

	private static boolean singleMatch ( char c, StringPointer p, StringPointer ep ) {
		switch ( p.getChar () ) {
		case '.':
			return true;  // matches any char
		case L_ESC:
			return matchClass ( p.getChar ( 1 ), c );
		case '[': {
			StringPointer sp = ep.getClone();
			sp.postIncrString ( -1 );
			return matchBracketClass ( c, p, sp );
		}
		default:
			return ( p.getChar () == c );
		}
	}

	private static StringPointer minExpand ( MatchState ms, StringPointer ss, StringPointer p, StringPointer ep ) {
		StringPointer sp = ep.getClone();
		StringPointer s = ss.getClone();

		sp.postIncrString ( 1 );
		while (true) {
			StringPointer res = match ( ms, s, sp );
			if ( res != null ) {
				return res;
			} else if ( s.getIndex () < ms.endIndex && singleMatch ( s.getChar (), p, ep ) ) {
				s.postIncrString ( 1 );  // try with one more repetition 
			} else {
				return null;
			}
		}
	}

	private static StringPointer maxExpand(MatchState ms, StringPointer s, StringPointer p, StringPointer ep) {
		int i = 0;  // counts maximum expand for item
		while (s.getIndex () + i < ms.endIndex && singleMatch(s.getChar(i), p, ep)) {
			i ++;
		}
		// keeps trying to match with the maximum repetitions 
		while (i >= 0) {
			StringPointer sp1 = s.getClone();
			sp1.postIncrString(i);
			StringPointer sp2 = ep.getClone();
			sp2.postIncrString(1);
			StringPointer res = match(ms, sp1, sp2);
			if (res != null) {
				return res;
			}
			i --;  // else didn't match; reduce 1 repetition to try again
		}
		return null;
	}

	private static boolean matchBracketClass(char c, StringPointer pp, StringPointer ecc) {
		StringPointer p = pp.getClone();
		StringPointer ec = ecc.getClone();
		boolean sig = true;
		if (p.getChar(1) == '^') {
			sig = false;
			p.postIncrString(1);  // skip the `^'
		}
		while (p.preIncrStringI(1) < ec.getIndex()) {
			if (p.getChar() == L_ESC) {
				p.postIncrString(1);
				if (matchClass(c, p.getChar())) {
					return sig;
				}
			} else if ((p.getChar(1) == '-') && (p.getIndex() + 2 < ec.getIndex())) {
				p.postIncrString(2);
				if (p.getChar(-2) <= c && c <= p.getChar()) {
					return sig;
				}
			} else if (p.getChar () == c) {
				return sig;
			}
		}
		return !sig;
	}

	private static StringPointer match(MatchState ms, StringPointer ss, StringPointer pp) {
		StringPointer s = ss.getClone();
		StringPointer p = pp.getClone();
		boolean isContinue = true;
		boolean isDefault = false;
		while (isContinue) {
			isContinue = false;
			isDefault = false;
			switch (p.getChar()) {
			case '(': { // start capture
				StringPointer p1 = p.getClone();
				if (p.getChar(1) == ')') { // position capture?
					p1.postIncrString(2);
					return startCapture(ms, s, p1, CAP_POSITION);
				} else {
					p1.postIncrString(1);
					return startCapture(ms, s, p1, CAP_UNFINISHED);
				}
			}
			case ')': { // end capture 
				StringPointer p1 = p.getClone();
				p1.postIncrString(1);
				return endCapture(ms, s, p1);
			}
			case L_ESC: {
				switch (p.getChar(1)) {
				case 'b': { // balanced string?
					StringPointer p1 = p.getClone();
					p1.postIncrString(2);
					s = matchBalance(ms, s, p1);
					if (s == null) {
						return null;
					}
					p.postIncrString(4);
					isContinue = true;
					continue; // else return match(ms, s, p+4);
				}
				case 'f': { // frontier?
					p.postIncrString (2);
					BaseLib.luaAssert(p.getChar() == '[' , "missing '[' after '%%f' in pattern");

					StringPointer ep = classEnd(ms, p);  // points to what is next
					char previous = (s.getIndex() == ms.src_init.getIndex()) ? '\0' : s.getChar(-1);

					StringPointer ep1 = ep.getClone();
					ep1.postIncrString(-1);
					if (matchBracketClass(previous, p, ep1) || !matchBracketClass(s.getChar(), p, ep1)) {
						return null;
					}
					p = ep;
					isContinue = true;
					continue; // else return match(ms, s, ep);
				}
				default: {
					if (Character.isDigit(p.getChar(1))) { // capture results (%0-%9)?
						s = matchCapture(ms, s, p.getChar(1));
						if (s == null) {
							return null;
						}
						p.postIncrString(2);
						isContinue = true;
						continue; // else return match(ms, s, p+2) 
					}
					isDefault = true; // case default 
				}
				}
				break;
			}
			case '\0': {  // end of pattern
				return s;  // match succeeded
			}
			case '$': {
				if (p.getChar(1) == '\0') { // is the `$' the last char in pattern?
					return (s.getIndex() == ms.endIndex) ? s : null;  // check end of string 
				}
			}
			default: { // it is a pattern item
				isDefault = true;
			}
			}

			if (isDefault) { // it is a pattern item
				isDefault = false;
				StringPointer ep = classEnd(ms, p);  // points to what is next
				boolean m = (s.getIndex () < ms.endIndex && singleMatch(s.getChar(), p, ep));
				switch (ep.getChar()) {
				case '?':  { // optional
					StringPointer res;
					StringPointer s1 = s.getClone();
					s1.postIncrString ( 1 );
					StringPointer ep1 = ep.getClone();
					ep1.postIncrString ( 1 );

					if (m && ((res = match(ms, s1, ep1)) != null)) {
						return res;
					}
					p = ep;
					p.postIncrString(1);
					isContinue = true;
					continue; // else return match(ms, s, ep+1);
				}
				case '*': { // 0 or more repetitions 
					return maxExpand(ms, s, p, ep);
				}
				case '+': { // 1 or more repetitions
					StringPointer s1 = s.getClone();
					s1.postIncrString(1);
					return (m ? maxExpand(ms, s1, p, ep) : null);
				}
				case '-': { // 0 or more repetitions (minimum) 
					return minExpand(ms, s, p, ep);
				}
				default: {
					if (!m) {
						return null;
					}
					s.postIncrString(1);

					p = ep;
					isContinue = true;
					continue; // else return match(ms, s+1, ep);
				}
				}
			}
		}
		return null;
	}

	private static boolean matchClass(char classIdentifier, char c) {
		boolean res;
		switch (Character.toLowerCase(classIdentifier)) {
		case 'a': res = Character.isLowerCase(c) || Character.isUpperCase(c); break;
		case 'c': res = isControl(c); break;
		case 'd': res = Character.isDigit(c); break;
		case 'l': res = Character.isLowerCase(c); break;
		case 'p': res = isPunct(c); break;
		case 's': res = isSpace(c); break;
		case 'u': res = Character.isUpperCase(c); break;
		case 'w': res = Character.isLowerCase(c) || Character.isUpperCase(c) || Character.isDigit(c); break;
		case 'x': res = isHex(c); break;
		case 'z': res = (c == 0); break;
		default: return (classIdentifier == c);
		}
		return Character.isLowerCase(classIdentifier) ? res : !res;
	}

	private static boolean isPunct(char c) {
		return ( c >= 0x21 && c <= 0x2F ) || 
		( c >= 0x3a && c <= 0x40 ) || 
		( c >= 0x5B && c <= 0x60 ) || 
		( c >= 0x7B && c <= 0x7E );
	}

	private static boolean isSpace(char c) {
		return ( c >= 0x09 && c <= 0x0D ) || c == 0x20 ;
	}

	private static boolean isControl(char c) {
		return ( c >= 0x00 && c <= 0x1f ) || c == 0x7f;
	}

	private static boolean isHex(char c) {
		return ( c >= '0' && c <= '9' ) || ( c >= 'a' && c <= 'f' ) || ( c >= 'A' && c <= 'F' );
	}

	private static int gsub(LuaCallFrame cf, int nargs) {
		String srcTemp = (String)BaseLib.getArg(cf, 1, BaseLib.TYPE_STRING, names[GSUB]);
		String pTemp = (String)BaseLib.getArg(cf, 2, BaseLib.TYPE_STRING, names[GSUB]);
		Object repl = BaseLib.getArg(cf, 3, null, names[GSUB]);
		Double num = (Double)BaseLib.getOptArg(cf, 4, BaseLib.TYPE_NUMBER);
		// if i isn't supplied, we want to substitute all occurrences of the pattern
		int maxSubstitutions = (num == null) ? Integer.MAX_VALUE : num.intValue(); 

		StringPointer pattern = new StringPointer (pTemp);
		StringPointer src = new StringPointer (srcTemp);

		boolean anchor = false;
		if (pattern.getChar() == '^') {
			anchor = true;
			pattern.postIncrString ( 1 );
		}

		String replType = BaseLib.type(repl);
		BaseLib.luaAssert(replType == BaseLib.TYPE_FUNCTION ||
				replType == BaseLib.TYPE_STRING || 
				replType == BaseLib.TYPE_TABLE, 
				"string/function/table expected, got "+replType);

		MatchState ms = new MatchState ();
		ms.callFrame = cf;
		ms.src_init = src.getClone();
		ms.endIndex = src.length();

		int n = 0;
		StringBuffer b = new StringBuffer();
		StringPointer e = null;
		while (n < maxSubstitutions) {
			ms.level = 0;
			e = match(ms, src, pattern);
			if (e != null) {
				n++;
				addValue(ms, repl, b, src, e);
			}

			if (e != null && e.getIndex() > src.getIndex()) { // non empty match?
				src.setIndex (e.getIndex());  // skip it 
			} else if (src.getIndex() < ms.endIndex) {
				b.append(src.postIncrString(1));
			} else {
				break;
			}

			if (anchor) {
				break;
			}
		}
		return cf.push(b.append(src.getString()).toString(), new Double(n));
	}

	private static void addValue(MatchState ms, Object repl, StringBuffer b, StringPointer src, StringPointer e) {
		String type = BaseLib.type(repl);
		if (type == BaseLib.TYPE_NUMBER || type == BaseLib.TYPE_STRING) {
			b.append(addString (ms, repl, src, e));
		} else if (type == BaseLib.TYPE_FUNCTION) {
			Object res = ms.callFrame.thread.state.call(repl,ms.getCaptures());
			b.append(res);
		} else if (type == BaseLib.TYPE_TABLE) {
			Object cap = ms.getCaptures()[0];
			b.append(((LuaTable)repl).rawget(cap));
		}
	}

	private static String addString(MatchState ms, Object repl, StringPointer s, StringPointer e) {
		String replTemp = BaseLib.tostring(repl, ms.callFrame.thread.state);
		StringPointer replStr = new StringPointer (replTemp);
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < replTemp.length(); i++) {
			if (replStr.getChar ( i ) != L_ESC) {
				buf.append(replStr.getChar(i));
			} else {
				i ++;  // skip ESC
				if (!Character.isDigit(replStr.getChar(i))) {
					buf.append(replStr.getChar(i));
				} else if (replStr.getChar(i) == '0') {
					String str = s.getString ();
					int len = s.length() - e.length();
					if (len > str.length() ) {
						len = str.length();
					}
					buf.append(str.substring(0, len));
				} else {
					Object o = ms.getCaptures()[replStr.getChar(i) - '1'];
					if(o instanceof Double) {
						Double doubleValue = ((Double)o);
						if( doubleValue.doubleValue() - doubleValue.intValue() == 0 ) {
							buf.append(String.valueOf(((Double)o).intValue())); 
						} else {
							buf.append(String.valueOf(((Double)o).doubleValue()));
						}
					} else {
						buf.append(o);
					}
				}
			}
		}
		return buf.toString();
	}
}
