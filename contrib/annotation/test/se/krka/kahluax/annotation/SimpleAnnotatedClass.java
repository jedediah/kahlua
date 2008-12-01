package se.krka.kahluax.annotation;

@LuaClass
public class SimpleAnnotatedClass {

	@LuaMethod
	public void doStuff() {
		
	}

	@LuaMethod(alias="MWA")
	public void methodWithArgs(int foo, String bar) {
		
	}
}
