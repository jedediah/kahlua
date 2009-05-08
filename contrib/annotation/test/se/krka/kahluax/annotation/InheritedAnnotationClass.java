package se.krka.kahluax.annotation;

@LuaClass
public class InheritedAnnotationClass extends BaseAnnotationClass {

	public String zomg;
	public int imba;
	public String s;
	public double d;
	public boolean b;
	public int i;
	public int x;
	public int y;

	@LuaMethod
	public void inheritedMethodWithArgs(String zomg, int imba) {
		this.zomg = zomg;
		this.imba = imba;
	}
	
	@LuaMethod(global = true)
	public void myGlobalFunction(String s, double d, boolean b, int i) {
		this.s = s;
		this.d = d;
		this.b = b;
		this.i = i;
	}
	
	@LuaMethod(global = true)
	public void myGlobalFunction2(ReturnValues r, int x, int y) {
		this.x = x;
		this.y = y;
		r.push(x * y);
		r.push(x + y);
	}
	
}
