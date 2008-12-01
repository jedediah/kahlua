package se.krka.kahluax.annotation;

import junit.framework.TestCase;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.JavaFunction;

public class AnnotationTest extends TestCase {

	public void testSimpleAnnotation() {
		LuaState state = new LuaState(System.out);
		LuaJavaClassFactory factory = new LuaJavaClassFactory(state);
		factory.exposeClass(SimpleAnnotatedClass.class);
		SimpleAnnotatedClass aClass = new SimpleAnnotatedClass();
		LuaTable metatable = (LuaTable) state.getmetatable(aClass, true);
		LuaTable indexTable = (LuaTable) metatable.rawget("__index");
		JavaFunction func = (JavaFunction) indexTable.rawget("MWA");
		assertNotNull(func);
	}

	public void testInheritedAnnotation() {
		LuaState state = new LuaState(System.out);
		LuaJavaClassFactory factory = new LuaJavaClassFactory(state);
		factory.exposeClass(InheritedAnnotationClass.class);
		InheritedAnnotationClass aClass = new InheritedAnnotationClass();
		LuaTable metatable = (LuaTable) state.getmetatable(aClass, true);
		
		LuaTable indexTable = (LuaTable) metatable.rawget("__index");
		JavaFunction func = (JavaFunction) indexTable.rawget("inheritedMethodWithArgs");
		assertNotNull(func);

		LuaTable inheritedMeta = indexTable.metatable;
		LuaTable inheritedIndexTable = (LuaTable) inheritedMeta.rawget("__index");
		func = (JavaFunction) inheritedIndexTable.rawget("baseMethodWithArgs");
		assertNotNull(func);

	}

}
