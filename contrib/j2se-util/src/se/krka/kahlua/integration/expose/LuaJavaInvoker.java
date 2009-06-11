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

package se.krka.kahlua.integration.expose;

import java.lang.reflect.InvocationTargetException;
import se.krka.kahlua.converter.LuaConversionError;
import se.krka.kahlua.converter.LuaConverterManager;
import se.krka.kahlua.integration.expose.caller.Caller;
import se.krka.kahlua.integration.processor.LuaClassDebugInformation;
import se.krka.kahlua.integration.processor.LuaMethodDebugInformation;
import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;

public class LuaJavaInvoker implements JavaFunction {
	private final LuaJavaClassExposer exposer;
	private final LuaConverterManager manager;
	private final Class<?> clazz;
	private final String name;
	private final Caller caller;

	private final ReturnValues returnValues;
	private final Class<?>[] parameterTypes;
	private final int numMethodParams;
	private final boolean needsReturnValues;
	private final int actualMethodParams;
	private final int methodParamStart;
	private final boolean hasSelf;
	private final int luaParamStart;
	private final int luaParams;


	public LuaJavaInvoker(LuaJavaClassExposer exposer, LuaConverterManager manager, Class<?> clazz, String name, Caller caller) {
		this.exposer = exposer;
		this.manager = manager;
		this.clazz = clazz;
		this.name = name;
		this.caller = caller;

		returnValues = new ReturnValues(manager);
		parameterTypes = caller.getParameterTypes();
		numMethodParams = parameterTypes.length;
		needsReturnValues = caller.needsMultipleReturnValues();		
		if (needsReturnValues) {
			actualMethodParams = numMethodParams - 1;
			methodParamStart = 1;
		} else {
			actualMethodParams = numMethodParams;
			methodParamStart = 0;
		}
		hasSelf = caller.hasSelf();
		if (hasSelf) {
			luaParams = actualMethodParams + 1;
			luaParamStart = 1;
		} else {
			luaParams = actualMethodParams;
			luaParamStart = 0;
		}
	}
	
	public int call(LuaCallFrame callFrame, int nArguments) {
		if (nArguments != luaParams) {
			String errorMessage;
			if (hasSelf && nArguments <= 0) {
				errorMessage = "Expected a method call but got a function call.";
			} else {
				int selfDecr = hasSelf ? 1 : 0;
				int expected = luaParams - selfDecr;
				int got = nArguments - selfDecr;
				errorMessage = "Expected " + expected + " arguments but got " + got + ".";
			}
			String syntax = getFunctionSyntax();
			if (syntax != null) {
				errorMessage += " Correct syntax: " + syntax;
			}
			BaseLib.fail(errorMessage);
		}
		Object self = null;
		if (hasSelf) {
			self = callFrame.get(0);
		}
		returnValues.reset(callFrame);
		Object[] params = buildParams(callFrame);
		
		try {
			caller.call(self, returnValues, params);
			return returnValues.getNArguments();
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		} catch (LuaConversionError e) {
			throw new RuntimeException(e);
		} catch (InstantiationException e) {
			throw new RuntimeException(e);
		} finally {
			returnValues.reset(null);
		}
	}
	
	private Object[] buildParams(LuaCallFrame callFrame) {
		Object[] result = new Object[numMethodParams];
		if (needsReturnValues) {
			result[0] = returnValues;
		}
		for (int i = actualMethodParams - 1; i >= 0; i--) {
			Object obj = callFrame.get(i + luaParamStart);
			int resultIndex = i + methodParamStart;
			try {
				result[resultIndex] = manager.fromLuaToJava(obj, parameterTypes[resultIndex]); 
			} catch (RuntimeException e) {
				throw newError(i, e);
			} catch (LuaConversionError e) {
				throw newError(i, e);
			}
		}
		return result;
	}

	private RuntimeException newError(int i, Exception e) {
		int argumentIndex = i + 1;
		String errorMessage = e.getMessage() + " at argument #" + argumentIndex;
		String argumentName = getParameterName(i);
		if (argumentName != null) {
			errorMessage += ", " + argumentName;
		}
		return new RuntimeException(errorMessage);
	}

	private String getFunctionSyntax() {
		LuaMethodDebugInformation methodDebug = getMethodDebugData();
		if (methodDebug != null) {
			return methodDebug.getLuaDescription();
		}
		return null;
	}


	public LuaMethodDebugInformation getMethodDebugData() {
		LuaClassDebugInformation debugInformation = exposer.getDebugdata(clazz);
		if (debugInformation == null) {
			return null;
		}
		LuaMethodDebugInformation methodDebug = debugInformation.methods.get(name);
		return methodDebug;
	}

	private String getParameterName(int i) {
		LuaMethodDebugInformation methodDebug = getMethodDebugData();
		if (methodDebug != null) {
			return methodDebug.getParameterName(i);
		}
		return null;
	}
	
}

