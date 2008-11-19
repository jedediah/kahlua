package se.krka.kahluax.annotation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;

/**
 * A tool to automatically expose java classes and
 * methods to a lua state
 * NOTE: This tool requires annotations (java 1.5 or higher) to work
 * and is therefore not supported in J2ME.
 */
public class LuaJavaClassFactory {
	private LuaState state;

	public LuaJavaClassFactory(LuaState state) {
		this.state = state;
	}

	public void exposeClass(Class clazz) {
		if (!isLuaClass(clazz)) {
			return;
		}
		if (!isExposed(clazz)) {
			Class superClazz = getSuperClass(clazz);
			exposeClass(superClazz);
			
			LuaTable metatable = new LuaTable();
			LuaTable indextable = new LuaTable();
			metatable.rawset("__index", indextable);
			state.setUserdataMetatable(clazz, metatable);

			LuaTable superMetatable = (LuaTable) state.userdataMetatables.rawget(superClazz);
			indextable.metatable = superMetatable;
			populateMethods(clazz, indextable);
		}
	}

	private void populateMethods(Class clazz, LuaTable indextable) {
		for (Method method : clazz.getMethods()) {
			if (method.isAnnotationPresent(LuaMethod.class)) {
				LuaMethod luaMethod = method.getAnnotation(LuaMethod.class);

				String methodName = method.getName();
				if (!luaMethod.alias().equals("[unassigned]")) {
					methodName = luaMethod.alias();
				}
				
				indextable.rawset(methodName, new MethodObject(method));
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

	private class MethodObject implements JavaFunction {
		private int nrOfParams;
		private Method method;

		private MethodObject(Method method) {
			this.method = method;
			nrOfParams = method.getParameterTypes().length;
		}

		private Object toDouble(Object param) {
			if (param instanceof Double) {
				return param;
			} else if (param instanceof Long) {
				return Double.valueOf(((Long) param).doubleValue());
			} else if (param instanceof Integer) {
				return Double.valueOf(((Integer) param).doubleValue());
			} else if (param instanceof Float) {
				return Double.valueOf(((Float) param).doubleValue());
			} else if (param instanceof Byte) {
				return Double.valueOf(((Byte) param).doubleValue());
			}
			return null;
		}
		
		private Object[] castParams(Object[] params, Class[] paramTypes) {
			Object[] newParams = new Object[paramTypes.length];

			for (int i = 0; i < paramTypes.length; i++) {
				Object param = params[i];
				Class paramType = paramTypes[i];
				if (paramType == Long.class || paramType == long.class) {
					param = Long.valueOf(((Double) param).longValue());
				} else if (paramType == Integer.class || paramType == int.class) {
					param = Integer.valueOf(((Double) param).intValue());
				} else if (paramType == Float.class || paramType == float.class) {
					param = Float.valueOf(((Double) param).floatValue());
				} else if (paramType == Byte.class || paramType == byte.class) {
					param = Byte.valueOf(((Double) param).byteValue());
				}
				newParams[i] = param;
			}
			return newParams;
		}

		
		public int call(LuaCallFrame luaCallFrame, int i) {
			BaseLib.luaAssert(i == nrOfParams + 1, "Wrong number of params!");
			Object owner = luaCallFrame.get(0);
			Object returnObject = null;
			try {
				if (nrOfParams != 0) {
					Object params[] = new Object[nrOfParams];
					for (int paramIndex = 0; paramIndex < nrOfParams; paramIndex++) {
						params[paramIndex] = luaCallFrame.get(paramIndex + 1);
					}
	
					Object[] newParams = castParams(params, method.getParameterTypes());

					returnObject = method.invoke(owner, newParams);
				} else {
					returnObject = method.invoke(owner);
				}
			} catch (IllegalAccessException e) {
				throw new RuntimeException("Illegal access to method, " + e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException("Illegal invocation of method, " + e);
			}
			Object d = toDouble(returnObject);
			if (d != null) {
				luaCallFrame.push(d);
			} else {
				if (returnObject instanceof Boolean) {
					luaCallFrame.push(LuaState.toBoolean(((Boolean) returnObject).booleanValue()));
				} else if (returnObject instanceof String) {
					luaCallFrame.push(((String) returnObject).intern());
				} else if (returnObject instanceof List) {
					List l = (List) returnObject;
					LuaTable t = new LuaTable();
					for (int index = 0; index < l.size(); index++) {
						t.rawset((double) index + 1, l.get(index));
					}
					luaCallFrame.push(t);
				} else {
					luaCallFrame.push(returnObject);
				}
			}
			return 1;
		}
	}
}
