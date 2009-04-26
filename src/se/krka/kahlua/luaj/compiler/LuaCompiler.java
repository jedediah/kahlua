package se.krka.kahlua.luaj.compiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.luaj.compiler.LuaC;
import org.luaj.vm.LBoolean;
import org.luaj.vm.LNil;
import org.luaj.vm.LNumber;
import org.luaj.vm.LPrototype;
import org.luaj.vm.LString;
import org.luaj.vm.LValue;
import org.luaj.vm.LuaErrorException;
import org.luaj.vm.Platform;

import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.vm.JavaFunction;
import se.krka.kahlua.vm.LuaCallFrame;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaPrototype;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;

public class LuaCompiler implements JavaFunction {

	private final int index;
	
	private static final int LOADSTRING = 0;
	private static final int LOADSTREAM = 1;
	private static final String[] names = new String[] {
		"loadstring",
		"loadstream",
	};
	
	private static final LuaCompiler[] functions = new LuaCompiler[names.length];
	static {
		for (int i = 0; i < names.length; i++) {
			functions[i] = new LuaCompiler(i);
		}
		Platform.setInstance(new KahluaLuaJPlatform());
	}
	private static final LuaC compiler = new LuaC();
	

	private LuaCompiler(int index) {
		this.index = index;		
	}
	
	public static void register(LuaState state) {
		LuaTable env = state.getEnvironment();
		for (int i = 0; i < names.length; i++) {
			env.rawset(names[i], functions[i]);
		}
		LuaTable packageTable = (LuaTable) env.rawget("package");
		LuaTable loadersTable = (LuaTable) packageTable.rawget("loaders");
	}
	
	public int call(LuaCallFrame callFrame, int nArguments) {
		switch (index) {
		case LOADSTRING: return loadstring(callFrame, nArguments);
		case LOADSTREAM: return loadstream(callFrame, nArguments);
		}
		return 0;
	}
	
	private int loadstream(LuaCallFrame callFrame, int nArguments) {
		try {
			BaseLib.luaAssert(nArguments >= 2, "not enough arguments");
			InputStream is = (InputStream) callFrame.get(0);
			BaseLib.luaAssert(is != null, "No inputstream given");
			String name = (String) callFrame.get(1);
			return callFrame.push(loadis(is, name, callFrame.getEnvironment()));
		} catch (RuntimeException e) {
			return callFrame.push(null, e.getMessage());
		} catch (IOException e) {
			return callFrame.push(null, e.getMessage());
		}
	}

	private int loadstring(LuaCallFrame callFrame, int nArguments) {
		try {
			BaseLib.luaAssert(nArguments >= 1, "not enough arguments");
			String source = (String) callFrame.get(0);
			BaseLib.luaAssert(source != null, "No source given");
			String name = (String) callFrame.get(1);
			if (name == null) {
				name = "<stdin>";
			}
			return callFrame.push(loadstring(source, name, callFrame.getEnvironment()));
		} catch (RuntimeException e) {
			return callFrame.push(null, e.getMessage());
		} catch (IOException e) {
			return callFrame.push(null, e.getMessage());
		}
	}

	public static LuaClosure loadis(InputStream inputStream, String name, LuaTable environment) throws IOException {
		BaseLib.luaAssert(name != null, "no name given the compilation unit");
		try {
			LPrototype luaj_prototype = compiler.compile(inputStream.read(), inputStream, name);
			return new LuaClosure(copyPrototype(luaj_prototype), environment);		
		} catch (LuaErrorException e) {
			throw new RuntimeException(e.getMessage());
		}		
	}
	
	public static LuaClosure loadstring(String source, String name, LuaTable environment) throws IOException {
		return loadis(new ByteArrayInputStream(source.getBytes("UTF-8")), name, environment);
	}

	private static LuaPrototype copyPrototype(LPrototype luaj_prototype) {
		LuaPrototype kahlua_prototype = new LuaPrototype();
		
		kahlua_prototype.code = luaj_prototype.code;
		kahlua_prototype.constants = copyValues(luaj_prototype.k);
		kahlua_prototype.isVararg = luaj_prototype.is_vararg != 0;
		kahlua_prototype.lines = luaj_prototype.lineinfo;
		kahlua_prototype.maxStacksize = luaj_prototype.maxstacksize;
		kahlua_prototype.name = toJavaString(luaj_prototype.source);
		kahlua_prototype.numParams = luaj_prototype.numparams;
		kahlua_prototype.numUpvalues = luaj_prototype.nups;
		
		LPrototype[] prototypes = luaj_prototype.p;
		int n = prototypes.length;
		
		LuaPrototype[] luaPrototypes = new LuaPrototype[n];
		kahlua_prototype.prototypes = luaPrototypes;
		
		for (int i = 0; i < n ; i++) {
			luaPrototypes[i] = copyPrototype(prototypes[i]);
		}
		
		return kahlua_prototype;
	}

	private static Object[] copyValues(LValue[] k) {
		int n = k.length;
		Object[] returnValue = new Object[n];
		for (int i = 0; i < n; i++) {
			LValue value = k[i];
			if (value instanceof LBoolean) {
				returnValue[i] = LuaState.toBoolean(value.toJavaBoolean());
			} else if (value instanceof LNumber) {
				returnValue[i] = value.toJavaBoxedDouble();
			} else if (value instanceof LString) {
				returnValue[i] = toJavaString((LString) value);
			} else if (value instanceof LNil) {
				returnValue[i] = null;
			} else {
				throw new RuntimeException("Invalid type in prototype: " + value + ", " + value.getClass());
			}
		}
		return returnValue;
	}
	
	public static String toJavaString(LString ls) {
		char[] c=new char[ls.m_length];
		int i, j, b, n=0;
		for ( i=ls.m_offset, j=ls.m_offset+ls.m_length; i<j; ) {
			if ((b = ls.m_bytes[i++]) == 0) {
				c[n++] = (char) 0;
			} else {
				c[n++] = (char) (
						(b>=0||i>=j)?
								b:
									(b<-32||i+1>=j)? 
											(((b&0x3f) << 6) 
													| (ls.m_bytes[i++]&0x3f)):
														(((b&0xf) << 12) 
																| ((ls.m_bytes[i++]&0x3f)<<6)
																|  (ls.m_bytes[i++]&0x3f))
				);
			}
		}
		return new String(c,0,n);
	}
	
}

