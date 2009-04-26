package se.krka.kahlua.luaj.compiler;

import java.io.InputStream;
import java.io.Reader;

import org.luaj.vm.LDouble;
import org.luaj.vm.LNumber;
import org.luaj.vm.LuaState;
import org.luaj.vm.Platform;

import se.krka.kahlua.stdlib.MathLib;

/**
 * This is a dummy implementation, and should ONLY be used to handle compilation, not any runtime jobs!
 *
 */
public class KahluaLuaJPlatform extends Platform {

	public KahluaLuaJPlatform() {
	}
	
	public Reader createReader(InputStream arg0) {
		return null;
	}

	public String getName() {
		return null;
	}

	public String getProperty(String arg0) {
		return null;
	}

	protected void installOptionalLibs(LuaState arg0) {
		
	}

	public LNumber mathPow(LNumber base, LNumber exp) {
        return LDouble.numberOf(MathLib.pow(base.toJavaDouble(),exp.toJavaDouble()));
	}

	public LNumber mathop(int arg0, LNumber arg1) {
		return null;
	}

	public LNumber mathop(int arg0, LNumber arg1, LNumber arg2) {
		return null;
	}

	public InputStream openFile(String arg0) {
		return null;
	}
}
