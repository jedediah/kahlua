package se.krka.kahluax.annotation;

@LuaClass
public class BaseAnnotationClass {

	public int foo;
	public String bar;

	@LuaMethod
	public void baseDoStuff() {

	}

	@LuaMethod
	public void baseMethodWithArgs(int foo, String bar) {
		this.foo = foo;
		this.bar = bar;

	}
}
