/*
 Copyright (c) 2009 Per Malmén <per.malmen@gmail.com>

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

package se.krka.kahlua.integration;

import java.util.Arrays;
import java.util.Iterator;

public class LuaSuccess extends LuaReturn {
	LuaSuccess(Object[] returnValues) {
		super(returnValues);
	}

	@Override
	public boolean isSuccess() {
		return true;
	}

	@Override
	public Object getErrorObject() {
		throw new UnsupportedOperationException("Not valid when isSuccess is true");
	}

	@Override
	public String getErrorString() {
		throw new UnsupportedOperationException("Not valid when isSuccess is true");
	}

	@Override
	public String getLuaStackTrace() {
		throw new UnsupportedOperationException("Not valid when isSuccess is true");
	}

	@Override
	public RuntimeException getJavaException() {
		throw new UnsupportedOperationException("Not valid when isSuccess is true");
	}

	@Override
	public Object getFirst() {
		return getReturnValue(0);
	}

	@Override
	public Object getSecond() {
		return getReturnValue(1);
	}

	@Override
	public Object getThird() {
		return getReturnValue(2);
	}

	@Override
	public Object getReturnValue(int index) {
		int realIndex = index + 1;
		if (realIndex >= returnValues.length || realIndex < 1) {
			return null;
		}
		return returnValues[realIndex];
	}

	@Override
	public int getNumReturnValues() {
		return returnValues.length - 1;
	}
	
	@Override
	public Object[] getReturnValues() {
		return Arrays.copyOfRange(returnValues, 1, returnValues.length);
	}
	
	@Override
	public Iterator<Object> iterator() {
		return new Iter();
	}
	
	private class Iter implements Iterator<Object> {
		private int pos = 1;
		
		@Override
		public boolean hasNext() {
			return pos < returnValues.length;
		}
		
		@Override
		public Object next() {
			Object obj = returnValues[pos];
			pos++;
			return obj;
		}
		
		@Override
		public void remove() {
			throw new UnsupportedOperationException("Not valid for immutable list");
		}
	}
}
