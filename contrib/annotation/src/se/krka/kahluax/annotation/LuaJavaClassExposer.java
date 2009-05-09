package se.krka.kahluax.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.LuaTableImpl;

/**
 * A tool to automatically expose java classes and
 * methods to a lua state
 * NOTE: This tool requires annotations (java 1.5 or higher) to work
 * and is therefore not supported in J2ME.
 */
public class LuaJavaClassExposer {
	private LuaState state;

	public LuaJavaClassExposer(LuaState state) {
		this.state = state;
	}

	public void exposeClass(Class clazz) {
		if (!isLuaClass(clazz)) {
			return;
		}
		if (!isExposed(clazz)) {
			Class superClazz = getSuperClass(clazz);
			exposeClass(superClazz);
			
			LuaTable metatable = new LuaTableImpl();
			LuaTable indextable = new LuaTableImpl();
			metatable.rawset("__index", indextable);
			state.setUserdataMetatable(clazz, metatable);

			LuaTable superMetatable = (LuaTable) state.userdataMetatables.rawget(superClazz);
			indextable.setMetatable(superMetatable);
			populateMethods(clazz, indextable);
		}
	}
	
	public void exposeGlobalFunctions(Object object) {
		Class<?> clazz = object.getClass();
		
		LuaTable environment = state.getEnvironment();
		for (Method method : clazz.getMethods()) {
			if (method.isAnnotationPresent(LuaMethod.class)) {
				LuaMethod luaMethod = method.getAnnotation(LuaMethod.class);

				String methodName;
				if (luaMethod.alias().equals(LuaMethod.UNASSIGNED)) {
					methodName = method.getName();
				} else {
					methodName = luaMethod.alias();
				}
				if (luaMethod.global()) {
					environment.rawset(methodName, new MethodObject(method, object));
				}
			}
		}
	}

	private void populateMethods(Class clazz, LuaTable indextable) {
		for (Method method : clazz.getMethods()) {
			if (method.isAnnotationPresent(LuaMethod.class)) {
				LuaMethod luaMethod = method.getAnnotation(LuaMethod.class);

				String methodName;
				if (luaMethod.alias().equals(LuaMethod.UNASSIGNED)) {
					methodName = method.getName();
				} else {
					methodName = luaMethod.alias();
				}
				if (luaMethod.global()) {
					// Use other expose
				} else {
					indextable.rawset(methodName, new MethodObject(method, null));
				}
			}
		}
	}

	private Class getSuperClass(Class clazz) {
		Class superClazz = clazz.getSuperclass();
		while (superClazz != null && superClazz != Object.class && !isLuaClass(superClazz)) {
			superClazz = superClazz.getSuperclass();
		}
		return superClazz;
	}

	private boolean isLuaClass(Class clazz) {
		return clazz != null && clazz.isAnnotationPresent(LuaClass.class);
	}

	private boolean isExposed(Class clazz) {
		Object metaTable = state.userdataMetatables.rawget(clazz);
		return metaTable != null;
	}
	
	private static class MethodObject implements JavaFunction {
		private final Method method;
		private final int nrOfParams;
		private final int useReturnValues;
		private final Object owner;
		private final ReturnValues returnValues;
		private final Class<?>[] parameterTypes;

		private MethodObject(Method method, Object owner) {			
			boolean useReturnValues = false;
			
			Class<?>[] parameterTypes = method.getParameterTypes();
			int nrOfParams = parameterTypes.length;
			if (nrOfParams > 0) {
				if (parameterTypes[0].equals(ReturnValues.class)) {
					if (!method.getReturnType().equals(void.class)) {
						throw new IllegalArgumentException("Must have a void return type if first argument is a ReturnValues: got: " + method.getReturnType());
					}
					useReturnValues = true;
				}
			}
			
			this.parameterTypes = parameterTypes;
			this.useReturnValues = useReturnValues ? 1 : 0;
			this.owner = owner;
			this.method = method;
			this.nrOfParams = nrOfParams;
			if (useReturnValues) {
				this.returnValues = new ReturnValues();
			} else {
				this.returnValues = null;
			}
		}
		
		public int call(LuaCallFrame callFrame, int nArguments) {
			boolean needsOwnerObject = this.owner == null; 
			int needsOwner = (needsOwnerObject ? 1 : 0);
			int inputParams = nrOfParams - useReturnValues + needsOwner;
			int actualParams = inputParams - needsOwner;
			BaseLib.luaAssert(nArguments == inputParams, "Wrong number of params!");

			int inputIndex = 0;
			Object owner = this.owner;
			if (owner == null) {
				owner = callFrame.get(0);
				inputIndex = 1;
			}
			
			Object[] args = null;
			if (nrOfParams != 0) {
				args = new Object[nrOfParams];
				if (useReturnValues == 1) {
					args[0] = returnValues;
					returnValues.reset(callFrame);
				}
				for (int i = 0; i < actualParams; i++) {
					args[i + useReturnValues] = Converter.toJavaObject(callFrame.get(inputIndex + i), parameterTypes[i + useReturnValues]);
				}
			}
			try {
				Object returnValue;
				if (nrOfParams == 0) {
					returnValue = method.invoke(owner);
				} else {
					returnValue = method.invoke(owner, args);					
				}
				if (useReturnValues == 1) {
					return returnValues.getNArguments();
				} else {
					callFrame.push(Converter.toLuaObject(returnValue));
					return 1;
				}
			} catch (IllegalArgumentException e) {
				throw new RuntimeException(e);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
