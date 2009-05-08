package se.krka.kahluax.annotation;

import java.io.IOException;

import junit.framework.TestCase;
import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.JavaFunction;

public class AnnotationTest extends TestCase {

	public void testSimpleAnnotation() {
		LuaState state = new LuaState(System.out);
		LuaJavaClassExposer factory = new LuaJavaClassExposer(state);
		factory.exposeClass(SimpleAnnotatedClass.class);
		SimpleAnnotatedClass aClass = new SimpleAnnotatedClass();
		LuaTable metatable = (LuaTable) state.getmetatable(aClass, true);
		LuaTable indexTable = (LuaTable) metatable.rawget("__index");
		JavaFunction func = (JavaFunction) indexTable.rawget("MWA");
		assertNotNull(func);
	}

	public void testInheritedAnnotation() throws IOException {
		LuaState state = new LuaState(System.out);
		LuaJavaClassExposer factory = new LuaJavaClassExposer(state);
		factory.exposeClass(InheritedAnnotationClass.class);
		InheritedAnnotationClass aClass = new InheritedAnnotationClass();
		LuaTable metatable = (LuaTable) state.getmetatable(aClass, true);
		
		LuaTable indexTable = (LuaTable) metatable.rawget("__index");
		JavaFunction func = (JavaFunction) indexTable.rawget("inheritedMethodWithArgs");
		assertNotNull(func);

		LuaTable inheritedMeta = indexTable.getMetatable();
		LuaTable inheritedIndexTable = (LuaTable) inheritedMeta.rawget("__index");
		func = (JavaFunction) inheritedIndexTable.rawget("baseMethodWithArgs");
		assertNotNull(func);

		{
			InheritedAnnotationClass testObject = new InheritedAnnotationClass();
			state.getEnvironment().rawset("testObject", testObject);
			String testString = "testObject:inheritedMethodWithArgs('hello', 123)";
			LuaClosure closure = LuaCompiler.loadstring(testString, "src", state.getEnvironment());
			state.call(closure, null);
			assertEquals(testObject.imba, 123);
			assertEquals(testObject.zomg, "hello");
		}
		
		{
			InheritedAnnotationClass testObject = new InheritedAnnotationClass();
			state.getEnvironment().rawset("testObject", testObject);
			String testString = "testObject:baseMethodWithArgs(112233, 'world')";
			LuaClosure closure = LuaCompiler.loadstring(testString, "src", state.getEnvironment());
			state.call(closure, null);
			assertEquals(testObject.foo, 112233);
			assertEquals(testObject.bar, "world");
		}		
	}


	public void testBadCall() throws IOException {
		LuaState state = new LuaState(System.out);
		LuaJavaClassExposer factory = new LuaJavaClassExposer(state);
		factory.exposeClass(InheritedAnnotationClass.class);

		InheritedAnnotationClass testObject = new InheritedAnnotationClass();
		state.getEnvironment().rawset("testObject", testObject);
		String testString = "testObject:inheritedMethodWithArgs('hello', 'world')";
		LuaClosure closure = LuaCompiler.loadstring(testString, "src", state.getEnvironment());
		try {
			state.call(closure, null);
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Expected Double but got world");
		}
	}
	
	public void testNotEnoughParameters() throws IOException {
		LuaState state = new LuaState(System.out);
		LuaJavaClassExposer factory = new LuaJavaClassExposer(state);
		factory.exposeClass(InheritedAnnotationClass.class);

		InheritedAnnotationClass testObject = new InheritedAnnotationClass();
		state.getEnvironment().rawset("testObject", testObject);
		String testString = "testObject:inheritedMethodWithArgs('hello')";
		LuaClosure closure = LuaCompiler.loadstring(testString, "src", state.getEnvironment());
		try {
			state.call(closure, null);
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Wrong number of params!");
		}
	}
	
	public void testGlobalFunctionWrongNumberOfParams() throws IOException {
		LuaState state = new LuaState(System.out);
		LuaJavaClassExposer factory = new LuaJavaClassExposer(state);

		InheritedAnnotationClass testObject = new InheritedAnnotationClass();
		factory.exposeGlobalFunctions(testObject);

		String testString = "myGlobalFunction('hello')";
		LuaClosure closure = LuaCompiler.loadstring(testString, "src", state.getEnvironment());
		try {
			state.call(closure, null);
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Wrong number of params!");
		}
	}
	
	public void testGlobalFunctionOk() throws IOException {
		LuaState state = new LuaState(System.out);
		LuaJavaClassExposer factory = new LuaJavaClassExposer(state);

		InheritedAnnotationClass testObject = new InheritedAnnotationClass();
		factory.exposeGlobalFunctions(testObject);

		String testString = "myGlobalFunction('hello', 1, true, 3)";
		LuaClosure closure = LuaCompiler.loadstring(testString, "src", state.getEnvironment());
		state.call(closure, null);
		assertEquals(testObject.s, "hello");
		assertEquals(testObject.d, 1.0);
		assertEquals(testObject.b, true);
		assertEquals(testObject.i, 3);
	}
	
	public void testGlobalFunctionReturnValues() throws IOException {
		LuaState state = new LuaState(System.out);
		LuaJavaClassExposer factory = new LuaJavaClassExposer(state);

		InheritedAnnotationClass testObject = new InheritedAnnotationClass();
		factory.exposeGlobalFunctions(testObject);

		String testString = "local a, b = myGlobalFunction2(5, 7); assert(a == 5*7, '1st'); assert(b == 5+7, '2nd');";
		LuaClosure closure = LuaCompiler.loadstring(testString, "src", state.getEnvironment());
		state.call(closure, null);
		assertEquals(testObject.x, 5);
		assertEquals(testObject.y, 7);
	}
	
	public void testMethodWithMultipleReturnValues() throws IOException {
		LuaState state = new LuaState(System.out);
		LuaJavaClassExposer factory = new LuaJavaClassExposer(state);

		InheritedAnnotationClass testObject = new InheritedAnnotationClass();
		factory.exposeClass(InheritedAnnotationClass.class);
		state.getEnvironment().rawset("testObject", testObject);

		String testString = "local a, b = testObject:inheritedMethodWithMultipleReturns(); assert(a == 'Hello', '1st'); assert(b == 'World', '2nd');";
		LuaClosure closure = LuaCompiler.loadstring(testString, "src", state.getEnvironment());
		state.call(closure, null);
		
		testString = "local a, b = testObject:inheritedMethodWithMultipleReturns2('prefix'); assert(a == 'prefixHello', '1st'); assert(b == 'prefixWorld', '2nd');";
		closure = LuaCompiler.loadstring(testString, "src", state.getEnvironment());
		state.call(closure, null);
		
	}
	
	
	
}
