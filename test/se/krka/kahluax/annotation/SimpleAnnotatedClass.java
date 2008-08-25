package se.krka.kahluax.annotation;

@LuaClass(alias="SimpleClass")
public class SimpleAnnotatedClass {

	@LuaMethod
	public void doStuff() {
		
	}

	@LuaMethod
	public void methodWithArgs(int foo, String bar) {
		
	}
}
