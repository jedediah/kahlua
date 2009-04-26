package se.krka.kahlua.vm;

public interface LuaTable {
	void setMetatable(LuaTable metatable);
	LuaTable getMetatable();
	void rawset(Object key, Object value);
	Object rawget(Object key);
	Object next(Object key);
	int len();
	void updateWeakSettings(boolean weakKeys, boolean weakValues);
}
