import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import se.krka.kahlua.luaj.compiler.LuaCompiler;
import se.krka.kahlua.vm.LuaClosure;
import se.krka.kahlua.vm.LuaTable;
import se.krka.kahlua.vm.LuaTableImpl;

public class LuaC {
	
	public static void main(String[] args) throws FileNotFoundException, IOException {
		if (args.length < 2) {
			System.err.println("Not enough arguments");
			System.err.println("Syntax: java LuaC <input.lua> <output.lbc>");
			System.exit(1);
		}
		
		File input = new File(args[0]);
		System.out.println("Input: " + input.getCanonicalPath());
		File output = new File(args[1]);
		System.out.println("Output: " + output.getCanonicalPath());
		
		LuaTable table = new LuaTableImpl();
		LuaClosure closure = LuaCompiler.loadis(new FileInputStream(input), input.getName(), table);
		closure.prototype.dump(new FileOutputStream(output));
	}
}
