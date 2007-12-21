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
package se.krka.kahlua.vm;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import se.krka.kahlua.stdlib.BaseLib;

public final class LuaPrototype {
	public int[] opcodes;

	// Constants
	public Object[] constants;
	public LuaPrototype[] prototypes;

    public int numParams;

    public boolean isVararg;

    // debug info
    public String name;
    public int[] lines;

	public int numUpvalues;

	public int maxStacksize;
    
	public LuaPrototype(DataInputStream in, boolean littleEndian, String parentName) throws IOException {
		int tmp;
		
		int len = in.readInt();
		loadAssert(len >= 0);
		len = toInt(len, littleEndian);
		
		name = readLuaString(in, len);
		if (name == null) {
			name = parentName;
		}

		// Commented out since they are not used
		// read line defined and last line defined
		in.readInt();		
		in.readInt();
		
		numUpvalues = in.read();
		numParams = in.read();
		int isVararg = in.read();
		this.isVararg = (isVararg & 2) != 0;
		maxStacksize = in.read();

		int codeLen = toInt(in.readInt(), littleEndian);
		opcodes = new int[codeLen];
		for (int i = 0; i < codeLen; i++) {
			int op = toInt(in.readInt(), littleEndian);
			opcodes[i] = op;
		}

		int constantsLen = toInt(in.readInt(), littleEndian);
		constants = new Object[constantsLen];
		for (int i = 0; i < constantsLen; i++) {
			int type = in.read();
			switch (type) {
			case 0:
			    constants[i] = null;
			    break;
			case 1:
			    int b = in.read();
			    constants[i] = b == 0 ? Boolean.FALSE : Boolean.TRUE;
			    break;
			case 3:
				long bits = in.readLong();
				if (littleEndian) bits = rev(bits);
				constants[i] = LuaState.toDouble(Double.longBitsToDouble(bits));
				break;
			case 4:
				len = toInt(in.readInt(), littleEndian);
				constants[i] = readLuaString(in, len);
				break;
			default:
				// This should never happen
			}
		}

		int prototypesLen = toInt(in.readInt(), littleEndian);
		prototypes = new LuaPrototype[prototypesLen];
		for (int i = 0; i < prototypesLen; i++) {
			prototypes[i] = new LuaPrototype(in, littleEndian, name);
		}
		
		// DEBUGGING INFORMATION
		
		// read lines
		tmp = toInt(in.readInt(), littleEndian);

		lines = new int[tmp];
		
		for (int i = 0; i < tmp; i++) {
			int tmp2 = toInt(in.readInt(), littleEndian);
			lines[i] = tmp2;
		}

		// skip locals
		tmp = toInt(in.readInt(), littleEndian);
		for (int i = 0; i < tmp; i++) {
			len = toInt(in.readInt(), littleEndian);
			readLuaString(in, len);
			in.readInt();
			in.readInt();
		}
		
		// read upvalues
		tmp = toInt(in.readInt(), littleEndian);
		for (int i = 0; i < tmp; i++) {
			len = toInt(in.readInt(), littleEndian);
			readLuaString(in, len);
		}
	}
	
	public String toString() {
		return name;
	}
	
	private static String readLuaString(DataInputStream in, int len) throws IOException {
		if (len == 0) {
			return null;
		}
		StringBuffer sb = new StringBuffer(len);
		
		// Intentional, read only len - 1 chars - the last character is a \0
		for (int i = len - 1; i > 0; i--) {
			sb.append((char) in.read());		
		}
		
		// Read the \0
		in.read();
		
		//int tmp = in.read();
		//assert(tmp == 0);
		
		return sb.toString().intern();
	}
	
	public static int rev(int v) {
		int a, b, c, d;
		a = (v >>> 24) & 255;
		b = (v >>> 16) & 255;
		c = (v >>> 8) & 255;
		d = v & 255;
		return (d << 24) | (c << 16) | (b << 8) | a;
		
		// This works in J2SE, but not J2ME
		// return Integer.reverseBytes(v);
	}
	
	public static long rev(long v) {
		long a, b, c, d, e, f, g, h;
		a = (v >>> 56) & 255;
		b = (v >>> 48) & 255;
		c = (v >>> 40) & 255;
		d = (v >>> 32) & 255;
		e = (v >>> 24) & 255;
		f = (v >>> 16) & 255;
		g = (v >>> 8) & 255;
		h = v & 255;
		return	(h << 56) | (g << 48)
			| (f << 40) | (e << 32)
			| (d << 24) | (c << 16)
			| (b << 8) | a;

		// This works in J2SE, but not J2ME
		// return Long.reverseBytes(v);
	}

	public static int toInt(int bits, boolean littleEndian) {
		return littleEndian ? rev(bits) : bits;
	}
	

	public static LuaClosure loadByteCode(DataInputStream in, LuaTable env)
	throws IOException {
		int tmp;

//		Read lua header
		tmp = in.read();
		loadAssert(tmp == 27);

		tmp = in.read();
		loadAssert(tmp == 'L');

		tmp = in.read();
		loadAssert(tmp == 'u');

		tmp = in.read();
		loadAssert(tmp == 'a');

//		Version = 5.1
		tmp = in.read();
		loadAssert(tmp == 0x51);

//		Format
		tmp = in.read();
		loadAssert(tmp == 0);

//		Little Endian!
		boolean littleEndian = in.read() == 1;

//		Size of int
		tmp = in.read();
		loadAssert(tmp == 4);

//		Size of size_t
		tmp = in.read();
		loadAssert(tmp == 4);

//		Size of instruction
		tmp = in.read();
		loadAssert(tmp == 4);

//		Size of number
		tmp = in.read();
		loadAssert(tmp == 8);

//		Integral
		tmp = in.read();
		loadAssert(tmp == 0);

//		Done with header, start reading functions
		LuaPrototype mainPrototype = new LuaPrototype(in, littleEndian, null);
		LuaClosure closure = new LuaClosure(mainPrototype, env);
		return closure;
	}

	private static void loadAssert(boolean c) throws IOException {
	    if (!c) throw new IOException("could not load bytecode");
	}

	public static LuaClosure loadByteCode(InputStream in, LuaTable env) throws IOException {
		return loadByteCode(new DataInputStream(in), env);
	}
	

}
