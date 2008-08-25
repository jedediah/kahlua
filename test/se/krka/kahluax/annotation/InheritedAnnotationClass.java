package se.krka.kahluax.annotation;

@LuaClass(alias="SubClass")
public class InheritedAnnotationClass extends BaseAnnotationClass {

	@LuaMethod
	public void inheritedMethodWithArgs(String zomg, int imba) {
		
	}
}
