/*
Copyright (c) 2007 Kristofer Karlsson <kristofer.karlsson@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package se.krka.kahlua.vm;

import java.io.PrintStream;
import java.util.Random;
import java.util.Vector;

import se.krka.kahlua.stdlib.BaseLib;
import se.krka.kahlua.stdlib.MathLib;

public final class LuaState {
	private static final int FIELDS_PER_FLUSH = 50;
	private static final int OP_MOVE = 0;
	private static final int OP_LOADK = 1;
	private static final int OP_LOADBOOL = 2;
	private static final int OP_LOADNIL = 3;
	private static final int OP_GETUPVAL = 4;
	private static final int OP_GETGLOBAL = 5;
	private static final int OP_GETTABLE = 6;
	private static final int OP_SETGLOBAL = 7;
	private static final int OP_SETUPVAL = 8;
	private static final int OP_SETTABLE = 9;
	private static final int OP_NEWTABLE = 10;
	private static final int OP_SELF = 11;
	private static final int OP_ADD = 12;
	private static final int OP_SUB = 13;
	private static final int OP_MUL = 14;
	private static final int OP_DIV = 15;
	private static final int OP_MOD = 16;
	private static final int OP_POW = 17;
	private static final int OP_UNM = 18;
	private static final int OP_NOT = 19;
	private static final int OP_LEN = 20;
	private static final int OP_CONCAT = 21;
	private static final int OP_JMP = 22;
	private static final int OP_EQ = 23;
	private static final int OP_LT = 24;
	private static final int OP_LE = 25;
	private static final int OP_TEST = 26;
	private static final int OP_TESTSET = 27;
	private static final int OP_CALL = 28;
	private static final int OP_TAILCALL = 29;
	private static final int OP_RETURN = 30;
	private static final int OP_FORLOOP = 31;
	private static final int OP_FORPREP = 32;
	private static final int OP_TFORLOOP = 33;
	private static final int OP_SETLIST = 34;
	private static final int OP_CLOSE = 35;
	private static final int OP_CLOSURE = 36;
	private static final int OP_VARARG = 37;
	
	public LuaTable environment;

	public Vector liveUpvalues;


	/*
	 * Stack operations follow
	 */
	public static final int MAX_STACK_SIZE = 1000;
	public static final int INITIAL_STACK_SIZE = 100;
	public Object[] stack;
	public int top;

	// Needed for Math lib - every state needs its own random
	public Random random = new Random();

	public LuaTable userdataMetatables;

	public String stackTrace;

	public PrintStream out;

	static final int MAX_INDEX_RECURSION = 100;

	private static final String meta_ops[];
	static {
		meta_ops = new String[38];
		meta_ops[OP_ADD] = "__add";
		meta_ops[OP_SUB] = "__sub";
		meta_ops[OP_MUL] = "__mul";
		meta_ops[OP_DIV] = "__div";
		meta_ops[OP_MOD] = "__mod";
		meta_ops[OP_POW] = "__pow";

		meta_ops[OP_EQ] = "__eq";
		meta_ops[OP_LT] = "__lt";
		meta_ops[OP_LE] = "__le";
	}

	private final void ensureStacksize(int index) {
		if (index > MAX_STACK_SIZE) {
			throw new RuntimeException("Stack overflow");			
		}
		int oldSize = stack.length;
		int newSize = Math.min(MAX_STACK_SIZE, 2 * oldSize);
		Object[] newStack = new Object[newSize];
		System.arraycopy(stack, 0, newStack, 0, oldSize);
		stack = newStack;
	}

	public final void setTop(int newTop) {
		if (top < newTop) {
			ensureStacksize(newTop);
		} else {
			stackClear(newTop, top - 1);
		}
		top = newTop;
	}

	public final void stackCopy(int startIndex, int destIndex, int len) {
		if (len > 0) {
			System.arraycopy(stack, startIndex, stack, destIndex, len);
		}
	}

	public final void stackClear(int startIndex, int endIndex) {
		for (; startIndex <= endIndex; startIndex++) {
			stack[startIndex] = null;
		}
	}    

	/*
	 * End of stack code
	 */

	public LuaState(PrintStream stream) {
		out = stream;
		setup();
		reset();
	}

	public final void reset() {
		environment = new LuaTable();
		userdataMetatables = new LuaTable();
		environment.rawset("_G", environment);
		environment.rawset("_VERSION", "Lua 5.1 for CLDC 1.1");
	}

	private final void setup() {
		stack = new Object[INITIAL_STACK_SIZE];
		liveUpvalues = new Vector();
		top = 0;
		stackTrace = "";
	}

	public final void cleanup(int top) {
		closeUpvalues(top);
		stackTrace = "";
	}
	
	/*
	public void inspectStack(int base) {
		System.out.println("-- Current Stack --");
		for (int i = 0; i < top; i++) {
			Object o = stack[i];

			if (i == base) {
				System.out.print("base: ");
			}
			System.out.println(i + ": " + BaseLib.type(o) + ": " + o);
		}
		System.out.println("-- Live upvalues --");
		for (int i = 0; i < liveUpvalues.size(); i++) {
			UpValue uv = (UpValue) liveUpvalues.elementAt(i);
			System.out.println(uv.index + ": " + uv.getValue());
		}
		System.out.println("-------------------");

	}
	 */
	
	public int call(int base) {
		Object o = stack[base];

		if (!(o instanceof LuaClosure) && !(o instanceof JavaFunction)) {
			o = prepareMetatableCall(base, o);
		}
		
		if (o instanceof JavaFunction) {
			int nReturnValues = ((JavaFunction) o).call(this, base);
			setTop(base + nReturnValues);

			return nReturnValues;
		}
		
		if (!(o instanceof LuaClosure)) {
			throw new RuntimeException("tried to call a non-function");
		}
		int nReturnValues = runLuaClosure(base);
		return nReturnValues;
	}

	private final Object prepareMetatableCall(int base, Object o) {
		Object f = getMetaOp(o, "__call");

		int nArguments = top - base - 1; 

		setTop(top + 1);
		stackCopy(base, base + 1, nArguments);
		stack[base] = f;

		o = f;
		return o;
	}

	private final int runLuaClosure(int base) {
		LuaClosure closure;
		LuaTable env;
		LuaPrototype prototype = null;
		int[] opcodes;
		Object[] constants;
		int pc = 0;
		int numParams;
		int numVarargs;

		int nArguments;

		try {
			while (true) {
				// assert stack[base] instanceof LuaClosure
				closure = (LuaClosure) stack[base];

				// Remember to let OP_RETURN and OP_TAILCALL reverse this
				base++;

				nArguments = top - base;

				env = closure.env;
				prototype = closure.prototype;
				opcodes = prototype.opcodes;
				constants = prototype.constants;
				numParams = prototype.numParams;

				numVarargs = 0;
				if (prototype.isVararg && nArguments > numParams) {
					numVarargs = nArguments - numParams;

					// Since there are vararg arguments,
					// move the base and copy params

					int newBase = base + nArguments;

					setTop(newBase + prototype.maxStacksize);

					stackCopy(base, newBase, numParams);
					base = newBase;
				}
				setTop(base + prototype.maxStacksize);

				stackClear(base + Math.min(nArguments, numParams) + 1, base + prototype.maxStacksize - 1);


				pc = 0;

				tailcall_escape_label:
					while (true) {
						int a, b, c;

						int op = opcodes[pc++];
						int opcode = op & 63;
/*
						System.out.println("\n\nStack before operation:");
						inspectStack(base);
						System.out.println("Line: " + prototype.lines[pc - 1]);
						System.out.println("opcode: " + opcode);
						System.out.println("a: " + getA8(op));
						System.out.println("b: " + getB9(op));
						System.out.println("c: " + getC9(op));
*/
						switch (opcode) {
						case OP_MOVE: {
							a = getA8(op);
							b = getB9(op);
							stack[base + a] = stack[base + b];
							break;
						}
						case OP_LOADK: {
							a = getA8(op);
							b = getBx(op);
							Object o2 = constants[b];
							stack[base + a] = o2;
							break;
						}
						case OP_LOADBOOL: {
							a = getA8(op);
							b = getB9(op);
							c = getC9(op);
							Boolean bool = b == 0 ? Boolean.FALSE : Boolean.TRUE;
							stack[base + a] = bool;
							if (c != 0) {
								pc++;
							}
							break;
						}
						case OP_LOADNIL: {
							a = getA8(op);
							b = getB9(op);
							stackClear(base + a, base + b);
							break;
						}
						case OP_GETUPVAL: {
							a = getA8(op);
							b = getB9(op);
							UpValue uv = closure.upvalues[b];
							stack[base + a] = uv.getValue();
							break;
						}
						case OP_GETGLOBAL: {
							a = getA8(op);
							b = getBx(op);
							Object res = tableGet(env, constants[b]);
							stack[base + a] = res;
							break;
						}
						case OP_GETTABLE: {
							a = getA8(op);
							b = getB9(op);
							c = getC9(op);

							Object bObj = stack[base + b];
							
							Object key = getRegisterOrConstant(constants, base, c);

							Object res = tableGet(bObj, key);
				    		stack[base + a] = res;
				    		
				    		
				    		//System.out.println("Stack after GETTABLE: " + res + " base = " + base + ", a = " + a);
				    		//inspectStack(base);
							break;
						}
						case OP_SETGLOBAL: {
							a = getA8(op);
							b = getBx(op);
							Object value = stack[base + a];
							Object key = constants[b];
							
							tableSet(env, key, value);
							
							break;
						}
						case OP_SETUPVAL: {
							a = getA8(op);
							b = getB9(op);

							UpValue uv = closure.upvalues[b];
							uv.setValue(stack[base + a]);

							break;
						}
						case OP_SETTABLE: {
							a = getA8(op);
							b = getB9(op);
							c = getC9(op);

							Object aObj = stack[base + a];
							
							Object key = getRegisterOrConstant(constants, base, b);
							Object value = getRegisterOrConstant(constants, base, c);

							tableSet(aObj, key, value);
							
							break;
						}
						case OP_NEWTABLE: {
							a = getA8(op);

							// Used to set up initial array and hash size - not implemented
							// b = getB9(op);
							// c = getC9(op);

							LuaTable t = new LuaTable();
							stack[base + a] = t;
							break;
						}
						case OP_SELF: {
							a = getA8(op);
							b = getB9(op);
							c = getC9(op);

							Object key = getRegisterOrConstant(constants, base, c);
							Object bObj = stack[base + b];

							Object fun = tableGet(bObj, key);

							stack[base + a] = fun;
							stack[base + a + 1] = bObj;
							break;
						}
						case OP_ADD:
						case OP_SUB:
						case OP_MUL:
						case OP_DIV:
						case OP_MOD:
						case OP_POW: {
							a = getA8(op);
							b = getB9(op);
							c = getC9(op);

							Object bo = getRegisterOrConstant(constants, base, b);
							Object co = getRegisterOrConstant(constants, base, c);

							Double bd = null, cd = null;
							Object res = null;
							if ((bd = BaseLib.rawTonumber(bo)) == null || (cd = BaseLib.rawTonumber(co)) == null) {
								String meta_op = meta_ops[opcode];

								Object metafun = null;
								if (bd == null) {
									metafun = getMetaOp(bo, meta_op);
								}
								if (metafun == null && cd == null) {
									metafun = getMetaOp(co, meta_op);
								}
								BaseLib.luaAssert(metafun != null, "no meta function was found for " + meta_op);
								res = call(metafun, bo, co, null);
							} else {
								res = primitiveMath(bo, co, opcode);
							}
							stack[base + a] = res;
							break;
						}
						case OP_UNM: {
							a = getA8(op);
							b = getB9(op);
							Object aObj = stack[base + b];

							Double aDouble = BaseLib.rawTonumber(aObj);
							Object res;
							if (aDouble != null) {
								res = toDouble(-fromDouble(aDouble));
							} else {
								Object metafun = getMetaOp(aObj, "__unm");
								res = call(metafun, aObj, null, null);
							}
							stack[base + a] = res;
							break;
						}
						case OP_NOT: {
							a = getA8(op);
							b = getB9(op);
							Object aObj = stack[base + b];
							stack[base + a] = toBoolean(!boolEval(aObj));
							break;					
						}
						case OP_LEN: {
							a = getA8(op);
							b = getB9(op);

							Object o = stack[base + b];
							Object res;
							if (o instanceof LuaTable) {
								LuaTable t = (LuaTable) o;
								res = toDouble(t.len());
							} else if (o instanceof String) {
								String s = (String) o;
								res = toDouble(s.length());
							} else {
								Object f = getMetaOp(o, "__len");

								res = call(f, o, null, null);
							}
							stack[base + a] = res;
							break;
						}
						case OP_CONCAT: {
							a = getA8(op);
							b = getB9(op);
							c = getC9(op);

							int first = base + b;
							int last = base + c;

							Object res = stack[last];
							last--;

							while (first <= last) {
								// Optimize for multi string concats
								{
									String resStr = BaseLib.rawTostring(res);
									if (res != null) {

										int nStrings = 0;
										int pos = last;
										while (first <= pos) {
											Object o = stack[pos];
											pos--;
											if (BaseLib.rawTostring(o) == null) {
												break;
											}
											nStrings++;
										}
										if (nStrings > 0) {
											StringBuffer concatBuffer = new StringBuffer();

											int firstString = last - nStrings + 1;
											while (firstString <= last) {
												concatBuffer.append(BaseLib.rawTostring(stack[firstString]));
												firstString++;
											}
											concatBuffer.append(resStr);

											res = concatBuffer.toString().intern();

											last = last - nStrings;
										}
									}
								}
								if (first <= last) {
									Object leftConcat = stack[last];

									Object metafun = getMetaOp(leftConcat, "__concat");
									if (metafun == null) {
										metafun = getMetaOp(res, "__concat");
									}
									res = call(metafun, leftConcat, res, null);
									last--;
								}
							}
							stack[base + a] = res;
							break;					
						}
						case OP_JMP: {
							pc += getSBx(op);
							break;
						}
						case OP_EQ:
						case OP_LT:
						case OP_LE: {
							a = getA8(op);
							b = getB9(op);
							c = getC9(op);

							Object bo = getRegisterOrConstant(constants, base, b);
							Object co = getRegisterOrConstant(constants, base, c);


							if (bo instanceof Double && co instanceof Double) {
								double bd_primitive = fromDouble(bo);
								double cd_primitive = fromDouble(co);

								if (opcode == OP_EQ) {
									if ((bd_primitive == cd_primitive) == (a == 0)) {
										pc++;
									}
								} else {
									if (opcode == OP_LT) {
										if ((bd_primitive < cd_primitive) == (a == 0)) {
											pc++;
										}
									} else { // opcode must be OP_LE
										if ((bd_primitive <= cd_primitive) == (a == 0)) {
											pc++;
										}									
									}
								}
							} else if (bo instanceof String && co instanceof String) {
								if (opcode == OP_EQ) {
									if ((bo == co) == (a == 0)) {
										pc++;
									}
								} else {
									String bs = (String) bo;
									String cs = (String) co;
									int cmp = bs.compareTo(cs);
																		
									if (opcode == OP_LT) {
										if ((cmp < 0) == (a == 0)) {
											pc++;
										}
									} else { // opcode must be OP_LE
										if ((cmp <= 0) == (a == 0)) {
											pc++;
										}									
									}
								}
							} else {
								boolean invert = false;

								String meta_op = meta_ops[opcode];

								Object metafun = null;
								if (bo == null) {
									metafun = getMetaOp(bo, meta_op);
								}
								if (metafun == null && co == null) {
									metafun = getMetaOp(co, meta_op);
								}

								/* Special case:
								 * OP_LE uses OP_LT if __le is not defined.
								 * if a <= b is then translated to not (b < a)
								 */
								if (metafun == null && opcode == OP_LE) {
									if (bo == null) {
										metafun = getMetaOp(bo, "__lt");
									}
									if (metafun == null && co == null) {
										metafun = getMetaOp(co, "__lt");
									}								
									// Swap the objects
									Object tmp = bo;
									bo = co;
									co = tmp;

									// Invert a (i.e. add the "not"
									invert = true;
								}

								boolean resBool;
								if (metafun == null && opcode == OP_EQ) {
									resBool = LuaState.luaEquals(bo, co); 
								} else {							
									Object res = call(metafun, bo, co, null);
									resBool = boolEval(res);
								}

								if (invert) {
									resBool = !resBool; 
								}
								if (resBool == (a == 0)) {
									pc++;
								}
							}
							break;
						}
						case OP_TEST: {
							a = getA8(op);
							// b = getB9(op);
							c = getC9(op);

							Object value = stack[base + a];
							if (boolEval(value) == (c == 0)) {
								pc++;
							}

							break;
						}
						case OP_TESTSET: {
							a = getA8(op);
							b = getB9(op);
							c = getC9(op);


							Object value = stack[base + b];
							if (boolEval(value) != (c == 0)) {
								stack[base +a] = value;
							} else {
								pc++;
							}

							break;
						}
						case OP_CALL: {
							a = getA8(op);
							b = getB9(op);
							c = getC9(op);
							int nArguments2 = b - 1;
							if (nArguments2 != -1) {
								setTop(base + a + nArguments2 + 1);
							}

							call(base + a);
							if (c != 0) {
								setTop(base + prototype.maxStacksize);
							}
							break;
						}
						case OP_TAILCALL: {
							closeUpvalues(base);

							a = getA8(op);
							b = getB9(op);
							int nArguments2 = b - 1;
							if (nArguments2 == -1) {
								nArguments2 = top - (base + a);
							}

							Object funcObj = stack[base + a];

							int realBase = base - 1;
							if (numVarargs > 0) {
								realBase -= nArguments;
							}

							stackCopy(base + a, realBase, nArguments2 + 1);
							base = realBase;
							setTop(realBase + nArguments2 + 1);

							if (funcObj instanceof LuaTable) {
								funcObj = prepareMetatableCall(base, funcObj);
							}

							if (funcObj instanceof LuaClosure) {
								break tailcall_escape_label;
							} else {
								return call(realBase);
							}
						}
						case OP_RETURN: {
							a = getA8(op);
							b = getB9(op);

							closeUpvalues(base);

							b--;
							if (b == -1) {
								b = top - (base + a);
							}

							int realBase = base - 1;
							if (numVarargs > 0) {
								realBase -= nArguments;
							}

							stackCopy(base + a, realBase, b + 1);
							setTop(realBase + b);

							return b;
						}
						case OP_FORPREP: {
							a = getA8(op);
							b = getSBx(op);

							double iter = fromDouble(stack[base + a]);
							double step = fromDouble(stack[base + a + 2]);
							stack[base + a] = toDouble(iter - step);
							pc += b;
							break;
						}
						case OP_FORLOOP: {
							a = getA8(op);

							double iter = fromDouble(stack[base + a]);
							double end = fromDouble(stack[base + a + 1]);
							double step = fromDouble(stack[base + a + 2]);
							iter += step;
							Double iterDouble = toDouble(iter);
							stack[base + a] = iterDouble;

							if ((step > 0) ? iter <= end : iter >= end) {
								b = getSBx(op);
								pc += b;
								stack[base + a + 3] = iterDouble;
							} else {
								setTop(base + a);
							}
							break;
						}
						case OP_TFORLOOP: {
							a = getA8(op);
							c = getC9(op);

							// Prepare for call					
							setTop(base + a + 6);
							stackCopy(base + a, base + a + 3, 3);

							call(base + a + 3);
							setTop(base + a + 3 + c);

							Object aObj3 = stack[base + a + 3];
							if (aObj3 != null) {
								stack[base + a + 2] = aObj3;
							} else {
								pc++;
							}
							break;
						}
						case OP_SETLIST: {
							a = getA8(op);
							b = getB9(op);
							c = getC9(op);

							if (c == 0) {
								c = opcodes[pc++];						
							}

							int offset = (c - 1) * FIELDS_PER_FLUSH;

							LuaTable t = (LuaTable) stack[base + a];
							for (int i = 1; i <= b; i++) {
								Object key = toDouble(offset + i);
								Object value = stack[base + a + i];
								t.rawset(key, value);
							}
							break;
						}
						case OP_CLOSE: {
							a = getA8(op);
							closeUpvalues(base + a);
							break;
						}
						case OP_CLOSURE: {
							a = getA8(op);
							b = getBx(op);
							LuaPrototype newPrototype = prototype.prototypes[b];
							LuaClosure newClosure = new LuaClosure(newPrototype, env);
							stack[base + a] = newClosure;
							int numUpvalues = newPrototype.numUpvalues;
							for (int i = 0; i < numUpvalues; i++) {
								op = opcodes[pc++];
								opcode = op & 63;
								b = getB9(op);
								switch (opcode) {
								case OP_MOVE: {
									newClosure.upvalues[i] = findUpvalue(base + b);
									break;
								}
								case OP_GETUPVAL: {
									newClosure.upvalues[i] = closure.upvalues[b];
									break;
								}
								default:
									// should never happen
								}
							}
							break;
						}
						case OP_VARARG: {
							a = getA8(op);
							b = getB9(op) - 1;
							if (b == -1) {
								b = numVarargs;
								setTop(base + a + b);
							}
							int nCopy = Math.min(numVarargs, b);
							if (nCopy > 0) {
								stackCopy(base - numVarargs, base + a, nCopy);
							}
							int firstNil = base + a + b;
							int numNils = b - nCopy;
							int afterLastNil = firstNil + numNils;

							stackClear(firstNil, afterLastNil - 1);

							break;
						}
						default: {
							// unreachable for proper bytecode
						}
						} // switch
					}
			}
		} catch (RuntimeException e) {
			int[] lines = prototype.lines;
			pc--;
			if (pc < lines.length) {
				stackTrace = stackTrace + "at " + prototype + ": " + lines[pc] + "\n";
			}
			throw e;
		}
	}

	public final Object getMetaOp(Object o, String meta_op) {
		if (o == null) {
			return null;
		}
		LuaTable meta = (LuaTable) getmetatable(o, true);
		if (meta == null) {
			return null;
		}
		return meta.rawget(meta_op);
	}

	public void setUserdataMetatable(Class type, LuaTable metatable) {
		userdataMetatables.rawset(type, metatable);
	}

	private final Object getRegisterOrConstant(Object[] constants, int base,
			int index) {
		Object o;
		int cindex = index - 256;
		if (cindex < 0) {
			o = stack[base + index];
		} else {
			o = constants[cindex];
		}
		return o;
	}

	/*
	 * private static final int getA24(int op) { return (op >>> 6); }
	 */

	private static final int getA8(int op) {
		return (op >>> 6) & 255;
	}

	private static final int getC9(int op) {
		return (op >>> 14) & 511;
	}

	private static final int getB9(int op) {
		return (op >>> 23) & 511;
	}

	private static final int getBx(int op) {
		return (op >>> 14);
	}

	private static final int getSBx(int op) {
		return (op >>> 14) - 131071;
	}

	private Double primitiveMath(Object x, Object y, int opcode)
	{
		double v1 = fromDouble(x);
		double v2 = fromDouble(y);
		double res = 0;
		switch (opcode) {
		case OP_ADD:
			res = v1 + v2;
			break;
		case OP_SUB:
			res = v1 - v2;
			break;
		case OP_MUL:
			res = v1 * v2;
			break;
		case OP_DIV:
			res = v1 / v2;
			break;
		case OP_MOD:
			if (v2 == 0) {
				res = Double.NaN;
			} else {
				int ipart = (int) (v1 / v2);
				res = v1 - ipart * v2;
			}
			break;
		case OP_POW:
			res = MathLib.pow(v1, v2);
			break;
		default:
			// this should be unreachable
		}
		return toDouble(res);
	}

	public final Object call(Object fun, Object arg1, Object arg2, Object arg3) {
		int oldTop = top;
		setTop(top + 4);
		stack[oldTop] = fun;
		stack[oldTop + 1] = arg1;
		stack[oldTop + 2] = arg2;
		stack[oldTop + 3] = arg3;
		call(oldTop);

		Object ret = stack[oldTop];

		setTop(oldTop);
		return ret;		
	}
	
	public final Object tableGet(Object table, Object key) {
		Object curObj = table;
    	for (int i = LuaState.MAX_INDEX_RECURSION; i > 0; i--) {
    		boolean isTable = curObj instanceof LuaTable; 
    		if (isTable) {
				LuaTable t = (LuaTable) curObj;
				Object res = t.rawget(key);
				if (res != null) {
					return res;
				}
    		}
    		Object metaOp = getMetaOp(curObj, "__index");
    		if (metaOp == null) {
    			if (isTable) {
    				return null;
    			}
    	   		throw new RuntimeException("attempted index of non-table");
    		}	    		
    		if (metaOp instanceof JavaFunction || metaOp instanceof LuaClosure) {
        		Object res = call(metaOp, table, key, null);
    			return res;
    		} else {
    			curObj = metaOp;
    		}
    	}
   		throw new RuntimeException("recursive metatable!");
	}
	
	public final void tableSet(Object table, Object key, Object value) {
		LuaTable.checkKey(key);

		Object curObj = table;
    	for (int i = LuaState.MAX_INDEX_RECURSION; i > 0; i--) {
    		Object metaOp;
    		if (curObj instanceof LuaTable) {
				LuaTable t = (LuaTable) curObj;

				if (t.rawget(key) != null) {
					t.rawset(key, value);
					return;
				}
				
	    		metaOp = getMetaOp(curObj, "__newindex");
	    		if (metaOp == null) {
	    			t.rawset(key, value);
	    			return;
	    		}									
    		} else {
    			metaOp = getMetaOp(curObj, "__newindex");
    			BaseLib.luaAssert(metaOp != null, "attempted index of non-table");
    		}
    		if (metaOp instanceof JavaFunction || metaOp instanceof LuaClosure) {
    			call(metaOp, table, key, value);
    			return;
    		} else {
    			curObj = metaOp;
    		}
    	}
   		throw new RuntimeException("recursive metatable!");
	}

	public final Object getmetatable(Object o, boolean raw) {
		LuaTable metatable;
		if (o instanceof LuaTable) {
			LuaTable t = (LuaTable) o;
			metatable = t.metatable;
		} else {
			metatable = (LuaTable) userdataMetatables.rawget(o.getClass());
		}
		
		if (!raw && metatable != null) {
			Object meta2 = metatable.rawget("__metatable");
			if (meta2 != null) {
				return meta2;
			}
		}
		return metatable;
	}

	public final UpValue findUpvalue(int scanIndex) {
		// TODO: use binary search instead?
		int loopIndex = liveUpvalues.size();
		while (--loopIndex >= 0) {
			UpValue uv = (UpValue) liveUpvalues.elementAt(loopIndex);
			if (uv.index == scanIndex) {
				return uv;
			}
			if (uv.index < scanIndex) {
				break;
			}
		}
		UpValue uv = new UpValue();
		uv.state = this;
		uv.index = scanIndex;
		
		liveUpvalues.insertElementAt(uv, loopIndex + 1);
		return uv;				
	}

	public final void closeUpvalues(int closeIndex) {
		// close all open upvalues
		
		int loopIndex = liveUpvalues.size();
		while (--loopIndex >= 0) {
			UpValue uv = (UpValue) liveUpvalues.elementAt(loopIndex);
			if (uv.index < closeIndex) {
				return;
			}
			uv.value = stack[uv.index];
			uv.state = null;
			liveUpvalues.removeElementAt(loopIndex);
		}
	}
	
	public static boolean luaEquals(Object a, Object b) {
		if (a == null || b == null) {
			return a == b;
		}
		if (a instanceof Double && b instanceof Double) {
			Double ad = (Double) a;
			Double bd = (Double) b;
			return ad.doubleValue() == bd.doubleValue();
		}
		return a == b;
	}

	public static double fromDouble(Object o) {
		return ((Double) o).doubleValue();
	}

	public static Double toDouble(double d) {
		return new Double(d);
	}

	public static boolean boolEval(Object o) {
		return (o != null) && (o != Boolean.FALSE);
	}

	public static Boolean toBoolean(boolean b) {
		if (b) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}
}
