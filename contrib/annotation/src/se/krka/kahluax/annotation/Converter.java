package se.krka.kahluax.annotation;

import java.util.List;

import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.LuaTableImpl;


public class Converter {

	public static Object toLuaObject(Object javaObject) {
		if (javaObject == null || javaObject instanceof String || javaObject instanceof Double) {
			return javaObject;
		}
		if (javaObject instanceof Boolean) {
			return ((Boolean) javaObject).booleanValue() ? Boolean.TRUE : Boolean.FALSE;
		}
		if (javaObject instanceof List) {
			List l = (List) javaObject;
			LuaTable t = new LuaTableImpl();
			for (int index = 0; index < l.size(); index++) {
				t.rawset((double) index + 1, l.get(index));
			}
			return t;
		}
		if (javaObject instanceof Long) {
			return Double.valueOf(((Long) javaObject).doubleValue());
		}
		if (javaObject instanceof Short) {
			return Double.valueOf(((Short) javaObject).doubleValue());
		}
		if (javaObject instanceof Byte) {
			return Double.valueOf(((Byte) javaObject).doubleValue());
		}
		if (javaObject instanceof Integer) {
			return Double.valueOf(((Integer) javaObject).doubleValue());
		}
		if (javaObject instanceof Float) {
			return Double.valueOf(((Float) javaObject).doubleValue());
		}
		return javaObject;
	}

	public static Object toJavaObject(Object object, Class<?> clazz) {
		if (object == null) {
			return null;
		}
		if (clazz.equals(Double.class)) {
			if (!(object instanceof Double)) {
				throw new RuntimeException("Expected Double but got " + object);
			}
			return object;
		}
		
		if (clazz.equals(String.class)) {
			if (!(object instanceof String)) {
				throw new RuntimeException("Expected String but got " + object);
			}
			return object;
		}
		
		if (clazz.equals(Boolean.class)) {
			if (!(object instanceof Boolean)) {
				throw new RuntimeException("Expected Boolean but got " + object);
			}
			return object;
		}

		if (clazz.equals(Integer.class) || clazz.equals(int.class)) {
			if (!(object instanceof Double)) {
				throw new RuntimeException("Expected Double but got " + object);
			}
			return Integer.valueOf(((Double) object).intValue());
		}

		if (clazz.equals(Long.class) || clazz.equals(Long.class)) {
			if (!(object instanceof Double)) {
				throw new RuntimeException("Expected Double but got " + object);
			}
			return Long.valueOf(((Double) object).longValue());
		}

		if (clazz.equals(Short.class) || clazz.equals(short.class)) {
			if (!(object instanceof Double)) {
				throw new RuntimeException("Expected Double but got " + object);
			}
			return Short.valueOf(((Double) object).shortValue());
		}
		

		if (clazz.equals(Byte.class) || clazz.equals(byte.class)) {
			if (!(object instanceof Double)) {
				throw new RuntimeException("Expected Double but got " + object);
			}
			return Byte.valueOf(((Double) object).byteValue());
		}

		if (clazz.equals(Float.class) || clazz.equals(float.class)) {
			if (!(object instanceof Double)) {
				throw new RuntimeException("Expected Double but got " + object);
			}
			return Float.valueOf(((Double) object).floatValue());
		}
		
		return object;
	}
}
