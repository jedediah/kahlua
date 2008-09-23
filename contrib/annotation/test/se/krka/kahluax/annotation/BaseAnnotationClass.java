package se.krka.kahluax.annotation;

@LuaClass(alias="BaseClass")
public class BaseAnnotationClass {

	@LuaMethod
	public void baseDoStuff() {

	}

	@LuaMethod
	public void baseMethodWithArgs(int foo, String bar) {

	}
}
