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

package se.krka.kahlua.integration.expose.caller;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import se.krka.kahlua.integration.expose.ReturnValues;

public class MethodCaller implements Caller {

	private final Method method;
	private final Class[] parameterTypes;
	private final boolean needsReturnValues;
	private final boolean hasReturnValue;
	private final Object owner;
	private final boolean hasSelf;
	
	public MethodCaller(Method method, Object owner, boolean hasSelf) {
		this.method = method;
		this.owner = owner;
		this.hasSelf = hasSelf;
		
		hasReturnValue = !method.getReturnType().equals(Void.TYPE);
		
		parameterTypes = method.getParameterTypes();
		boolean needsReturnValues = false;
		if (parameterTypes.length > 0) {
			if (parameterTypes[0].equals(ReturnValues.class)) {
				if (hasReturnValue) {
					throw new IllegalArgumentException("Must have a void return type if first argument is a ReturnValues: got: " + method.getReturnType());
				}
				needsReturnValues = true;
			}
		}
		this.needsReturnValues = needsReturnValues;
	}
	
	@Override
	public void call(Object self, ReturnValues rv, Object[] params) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		if (!hasSelf) {
			self = owner;
		}
		if (needsReturnValues) {
			method.invoke(self, params);
		} else {
			Object ret = method.invoke(self, params);
			if (hasReturnValue) {
				rv.push(ret);
			}
		}
	}
	
	@Override
	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}

	@Override
	public boolean hasSelf() {
		return hasSelf;
	}

	@Override
	public boolean needsMultipleReturnValues() {
		return needsReturnValues;
	}
}
