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

public class LuaSuccess extends LuaReturn {
	public LuaSuccess(Object[] returnValues) {
		super(returnValues);
	}

	@Override
	public boolean isSuccess() {
		return true;
	}

	@Override
	Object getErrorObject() {
		throw new UnsupportedOperationException("Not valid when isSuccess is true");
	}

	@Override
	String getErrorString() {
		throw new UnsupportedOperationException("Not valid when isSuccess is true");
	}

	@Override
	String getLuaStackTrace() {
		throw new UnsupportedOperationException("Not valid when isSuccess is true");
	}

	@Override
	Exception getJavaException() {
		throw new UnsupportedOperationException("Not valid when isSuccess is true");
	}

	@Override
	Object getFirst() {
		return getReturnValue(0);
	}

	@Override
	Object getSecond() {
		return getReturnValue(1);
	}

	@Override
	Object getThird() {
		return getReturnValue(2);
	}

	@Override
	Object getReturnValue(int index) {
		int realIndex = index + 1;
		if (realIndex >= returnValues.length || realIndex < 1) {
			return null;
		}
		return returnValues[realIndex];
	}

	@Override
	Object getNumReturnValues() {
		return returnValues.length - 1;
	}
}
