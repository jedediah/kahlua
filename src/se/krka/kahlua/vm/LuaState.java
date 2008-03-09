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
	public LuaThread currentThread;

	// Needed for Math lib - every state needs its own random
	public Random random = new Random();

	public LuaTable userdataMetatables;

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
		currentThread = new LuaThread(this);
	}

	private final void setup() {
	}

	public void inspectStack(LuaCallFrame callFrame) {
		System.out.println("-- Current Stack --");
		for (int i = 0; i < callFrame.getTop(); i++) {
			Object o = callFrame.get(i);
			
			System.out.println(i + ": " + BaseLib.type(o) + ": " + o);
		}
		/*
		System.out.println("-- Live upvalues --");
		for (int i = 0; i < liveUpvalues.size(); i++) {
			UpValue uv = (UpValue) liveUpvalues.elementAt(i);
			System.out.println(uv.index + ": " + uv.getValue());
		}
		*/
		System.out.println("-------------------");

	}
	
	public int call(int nArguments) {
		int top = currentThread.getTop();
		int base = top - nArguments - 1;
		Object o = currentThread.objectStack[base];

		/*
		if (!(o instanceof LuaClosure) && !(o instanceof JavaFunction)) {
			o = prepareMetatableCall(base, o);
		}
		*/
		
		if (o == null) {
			throw new RuntimeException("tried to call nil");
		}
		
		if (o instanceof JavaFunction) {
			return callJava((JavaFunction) o, base, nArguments);
		}
		
		if (!(o instanceof LuaClosure)) {
			throw new RuntimeException("tried to call a non-function");
		}

		LuaCallFrame callFrame = currentThread.pushNewCallFrame((LuaClosure) o, base + 1, base, nArguments, false, false);
		callFrame.init();
		
		luaMainloop();

		int nReturnValues = currentThread.getTop() - base;
		
		currentThread.stackTrace = "";
		
		return nReturnValues;
	}

	private int callJava(JavaFunction f, int base, int nArguments) {
		LuaThread thread = currentThread;
		
		LuaCallFrame callFrame = thread.pushNewCallFrame(null, base + 1, base, nArguments, false, false);

		//System.out.println("Pre: " + f);
		//inspectStack(callFrame);
		
		int nReturnValues = f.call(callFrame, nArguments); 
		
		//System.out.println("Post: " + f);
		//System.out.println("return values: " + nReturnValues);
		//inspectStack(callFrame);
		
		// Clean up return values
		int top = callFrame.getTop();
		int actualReturnBase = top - nReturnValues; 

		//System.out.println("Copy " + actualReturnBase + " to " + -1 + " [" + nReturnValues);
		callFrame.stackCopy(actualReturnBase, -1, nReturnValues);
		callFrame.setTop(nReturnValues - 1);
		
		//System.out.println("Post2: " + f + ", " + actualReturnBase);
		//inspectStack(callFrame);
		
		thread.popCallFrame();
		
		return nReturnValues;
	}

	private final Object prepareMetatableCall(Object o) {
		if (o instanceof JavaFunction || o instanceof LuaClosure) {
			return o;
		}
		
		Object f = getMetaOp(o, "__call");

		return f;
	}

	public final void luaMainloop() {
		LuaCallFrame callFrame = currentThread.currentCallFrame();
		LuaClosure closure = callFrame.closure;
		LuaPrototype prototype = closure.prototype;
		int[] opcodes = prototype.opcodes;
		
		try {
			while (true) {
				int a, b, c;

				int op = opcodes[callFrame.pc++];
				int opcode = op & 63;
				
				//inspectStack(callFrame);
				//System.out.println(opcode);
				
				int returnBase = callFrame.returnBase;
				switch (opcode) {
				case OP_MOVE: {
					a = getA8(op);
					b = getB9(op);
					callFrame.set(a, callFrame.get(b));
					break;
				}
				case OP_LOADK: {
					a = getA8(op);
					b = getBx(op);
					callFrame.set(a, prototype.constants[b]);
					break;
				}
				case OP_LOADBOOL: {
					a = getA8(op);
					b = getB9(op);
					c = getC9(op);
					Boolean bool = b == 0 ? Boolean.FALSE : Boolean.TRUE;
					callFrame.set(a, bool);
					if (c != 0) {
						callFrame.pc++;
					}
					break;
				}
				case OP_LOADNIL: {
					a = getA8(op);
					b = getB9(op);
					callFrame.stackClear(a, b);
					break;
				}
				case OP_GETUPVAL: {
					a = getA8(op);
					b = getB9(op);
					UpValue uv = closure.upvalues[b];
					callFrame.set(a, uv.getValue());
					break;
				}
				case OP_GETGLOBAL: {
					a = getA8(op);
					b = getBx(op);
					Object res = tableGet(closure.env, prototype.constants[b]);
					callFrame.set(a, res);
					break;
				}
				case OP_GETTABLE: {
					a = getA8(op);
					b = getB9(op);
					c = getC9(op);

					Object bObj = callFrame.get(b);

					Object key = getRegisterOrConstant(callFrame, c);

					Object res = tableGet(bObj, key);
					callFrame.set(a, res);
					break;
				}
				case OP_SETGLOBAL: {
					a = getA8(op);
					b = getBx(op);
					Object value = callFrame.get(a);
					Object key = prototype.constants[b];

					tableSet(closure.env, key, value);

					break;
				}
				case OP_SETUPVAL: {
					a = getA8(op);
					b = getB9(op);

					UpValue uv = closure.upvalues[b];
					uv.setValue(callFrame.get(a));

					break;
				}
				case OP_SETTABLE: {
					a = getA8(op);
					b = getB9(op);
					c = getC9(op);

					Object aObj = callFrame.get(a);

					Object key = getRegisterOrConstant(callFrame, b);
					Object value = getRegisterOrConstant(callFrame, c);

					tableSet(aObj, key, value);

					break;
				}
				case OP_NEWTABLE: {
					a = getA8(op);

					// Used to set up initial array and hash size - not implemented
					// b = getB9(op);
					// c = getC9(op);

					LuaTable t = new LuaTable();
					callFrame.set(a, t);
					break;
				}
				case OP_SELF: {
					a = getA8(op);
					b = getB9(op);
					c = getC9(op);

					Object key = getRegisterOrConstant(callFrame, c);
					Object bObj = callFrame.get(b);

					Object fun = tableGet(bObj, key);

					callFrame.set(a, fun);
					callFrame.set(a + 1, bObj);
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

					Object bo = getRegisterOrConstant(callFrame, b);
					Object co = getRegisterOrConstant(callFrame, c);

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
					callFrame.set(a, res);
					break;
				}
				case OP_UNM: {
					a = getA8(op);
					b = getB9(op);
					Object aObj = callFrame.get(b);

					Double aDouble = BaseLib.rawTonumber(aObj);
					Object res;
					if (aDouble != null) {
						res = toDouble(-fromDouble(aDouble));
					} else {
						Object metafun = getMetaOp(aObj, "__unm");
						res = call(metafun, aObj, null, null);
					}
					callFrame.set(a, res);
					break;
				}
				case OP_NOT: {
					a = getA8(op);
					b = getB9(op);
					Object aObj = callFrame.get(b);
					callFrame.set(a, toBoolean(!boolEval(aObj)));
					break;					
				}
				case OP_LEN: {
					a = getA8(op);
					b = getB9(op);

					Object o = callFrame.get(b);
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
					callFrame.set(a, res);
					break;
				}
				case OP_CONCAT: {
					a = getA8(op);
					b = getB9(op);
					c = getC9(op);

					int first = b;
					int last = c;

					Object res = currentThread.objectStack[last];
					last--;

					while (first <= last) {
						// Optimize for multi string concats
						{
							String resStr = BaseLib.rawTostring(res);
							if (res != null) {

								int nStrings = 0;
								int pos = last;
								while (first <= pos) {
									Object o = callFrame.get(pos);
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
										concatBuffer.append(BaseLib.rawTostring(callFrame.get(firstString)));
										firstString++;
									}
									concatBuffer.append(resStr);

									res = concatBuffer.toString().intern();

									last = last - nStrings;
								}
							}
						}
						if (first <= last) {
							Object leftConcat = callFrame.get(last);

							Object metafun = getMetaOp(leftConcat, "__concat");
							if (metafun == null) {
								metafun = getMetaOp(res, "__concat");
							}
							
							res = call(metafun, leftConcat, res, null);
							last--;
						}
					}
					callFrame.set(a,  res);
					break;
				}
				case OP_JMP: {
					callFrame.pc += getSBx(op);
					break;
				}
				case OP_EQ:
				case OP_LT:
				case OP_LE: {
					a = getA8(op);
					b = getB9(op);
					c = getC9(op);

					Object bo = getRegisterOrConstant(callFrame, b);
					Object co = getRegisterOrConstant(callFrame, c);


					if (bo instanceof Double && co instanceof Double) {
						double bd_primitive = fromDouble(bo);
						double cd_primitive = fromDouble(co);

						if (opcode == OP_EQ) {
							if ((bd_primitive == cd_primitive) == (a == 0)) {
								callFrame.pc++;
							}
						} else {
							if (opcode == OP_LT) {
								if ((bd_primitive < cd_primitive) == (a == 0)) {
									callFrame.pc++;
								}
							} else { // opcode must be OP_LE
								if ((bd_primitive <= cd_primitive) == (a == 0)) {
									callFrame.pc++;
								}									
							}
						}
					} else if (bo instanceof String && co instanceof String) {
						if (opcode == OP_EQ) {
							if ((bo == co) == (a == 0)) {
								callFrame.pc++;
							}
						} else {
							String bs = (String) bo;
							String cs = (String) co;
							int cmp = bs.compareTo(cs);

							if (opcode == OP_LT) {
								if ((cmp < 0) == (a == 0)) {
									callFrame.pc++;
								}
							} else { // opcode must be OP_LE
								if ((cmp <= 0) == (a == 0)) {
									callFrame.pc++;
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
							callFrame.pc++;
						}
					}
					break;
				}
				case OP_TEST: {
					a = getA8(op);
					// b = getB9(op);
					c = getC9(op);

					Object value = callFrame.get(a);
					if (boolEval(value) == (c == 0)) {
						callFrame.pc++;
					}

					break;
				}
				case OP_TESTSET: {
					a = getA8(op);
					b = getB9(op);
					c = getC9(op);


					Object value = callFrame.get(b);
					if (boolEval(value) != (c == 0)) {
						callFrame.set(a, value);
					} else {
						callFrame.pc++;
					}

					break;
				}
				case OP_CALL:
				{
					a = getA8(op);
					b = getB9(op);
					c = getC9(op);
					int nArguments2 = b - 1;
					if (nArguments2 != -1) {
						callFrame.setTop(a + nArguments2 + 1);
					} else {
						nArguments2 = callFrame.getTop() - a - 1;
					}

					callFrame.restoreTop = c != 0;
					
					Object fun = prepareMetatableCall(callFrame.get(a));

					int base = callFrame.localBase;
					
					if (fun instanceof LuaClosure) {
						LuaCallFrame newCallFrame = currentThread.pushNewCallFrame((LuaClosure) fun, base + a + 1, base + a, nArguments2, true, callFrame.insideCoroutine);
						newCallFrame.init();
						
						callFrame = newCallFrame;
						closure = newCallFrame.closure;
						prototype = closure.prototype;
						opcodes = prototype.opcodes;
					} else if (fun instanceof JavaFunction) {
						int nReturnValues = callJava((JavaFunction) fun, base + a, nArguments2);
					
						// TODO: handle correct top after yield / resume
						
						// The call might have changed something...
						callFrame = currentThread.currentCallFrame();
						closure = callFrame.closure;
						prototype = closure.prototype;
						opcodes = prototype.opcodes;
						

						if (callFrame.restoreTop) {
							callFrame.setTop(prototype.maxStacksize);
						}
					} else {
						throw new RuntimeException("Tried to call a non-function: " + fun);
					}

					break;
				}
				case OP_TAILCALL: {
					int base = callFrame.localBase;
					
					currentThread.closeUpvalues(base);

					a = getA8(op);
					b = getB9(op);
					int nArguments2 = b - 1;
					if (nArguments2 == -1) {
						nArguments2 = callFrame.getTop() - a - 1;
					}

					callFrame.restoreTop = false;
					
					Object fun = prepareMetatableCall(callFrame.get(a));
					
					currentThread.stackCopy(base + a, returnBase, nArguments2 + 1);
					currentThread.setTop(returnBase + nArguments2 + 1);
					
					if (fun instanceof LuaClosure) {
						callFrame.localBase = returnBase + 1;
						callFrame.nArguments = nArguments2;
						callFrame.closure = (LuaClosure) fun; 
						callFrame.init();
						
					} else if (fun instanceof JavaFunction) {
						int nReturnValues = callJava((JavaFunction) fun, base + a, nArguments2);

						// TODO: handle yield
						currentThread.popCallFrame();
						if (callFrame.fromLua) {
							callFrame = currentThread.currentCallFrame();
							closure = callFrame.closure;
							prototype = closure.prototype;
							opcodes = prototype.opcodes;

							if (callFrame.restoreTop) {
								callFrame.setTop(prototype.maxStacksize);
							}
						} else {
							return;
						}
						
					} else {
						throw new RuntimeException("Tried to call a non-function: " + fun);
					}
					
					break;
				}
				case OP_RETURN: {
					// TODO: Set up return to recreate top according to prototype
					
					a = getA8(op);
					b = getB9(op) - 1;

					int base = callFrame.localBase;
					currentThread.closeUpvalues(base);

					if (b == -1) {
						b = callFrame.getTop() - a;
					}

					currentThread.stackCopy(callFrame.localBase + a, returnBase, b);
					currentThread.setTop(returnBase + b);

					// TODO: handle yield
					currentThread.popCallFrame();
					if (callFrame.fromLua) {
						callFrame = currentThread.currentCallFrame();
						closure = callFrame.closure;
						prototype = closure.prototype;
						opcodes = prototype.opcodes;
						

						if (callFrame.restoreTop) {
							callFrame.setTop(prototype.maxStacksize);
						}
					} else {
						return;
					}
					break;
				}
				case OP_FORPREP: {
					a = getA8(op);
					b = getSBx(op);

					double iter = fromDouble(callFrame.get(a));
					double step = fromDouble(callFrame.get(a + 2));
					callFrame.set(a, toDouble(iter - step));
					callFrame.pc += b;
					break;
				}
				case OP_FORLOOP: {
					a = getA8(op);

					double iter = fromDouble(callFrame.get(a));
					double end = fromDouble(callFrame.get(a + 1));
					double step = fromDouble(callFrame.get(a + 2));
					iter += step;
					Double iterDouble = toDouble(iter);
					callFrame.set(a,  iterDouble);

					if ((step > 0) ? iter <= end : iter >= end) {
						b = getSBx(op);
						callFrame.pc += b;
						callFrame.set(a + 3, iterDouble);
					} else {
						callFrame.setTop(a);
					}
					break;
				}
				case OP_TFORLOOP: {
					a = getA8(op);
					c = getC9(op);

					callFrame.setTop(a + 6);
					callFrame.stackCopy(a, a + 3, 3);
					call(2);
					callFrame.setTop(a + 3 + c);

					Object aObj3 = callFrame.get(a + 3);
					if (aObj3 != null) {
						callFrame.set(a + 2, aObj3);
					} else {
						callFrame.pc++;
					}
					break;
				}
				case OP_SETLIST: {
					a = getA8(op);
					b = getB9(op);
					c = getC9(op);

					if (c == 0) {
						c = opcodes[callFrame.pc++];						
					}

					int offset = (c - 1) * FIELDS_PER_FLUSH;

					LuaTable t = (LuaTable) callFrame.get(a);
					for (int i = 1; i <= b; i++) {
						Object key = toDouble(offset + i);
						Object value = callFrame.get(a + i);
						t.rawset(key, value);
					}
					break;
				}
				case OP_CLOSE: {
					a = getA8(op);
					callFrame.closeUpvalues(a);
					break;
				}
				case OP_CLOSURE: {
					a = getA8(op);
					b = getBx(op);
					LuaPrototype newPrototype = prototype.prototypes[b];
					LuaClosure newClosure = new LuaClosure(newPrototype, closure.env);
					callFrame.set(a, newClosure);
					int numUpvalues = newPrototype.numUpvalues;
					for (int i = 0; i < numUpvalues; i++) {
						op = opcodes[callFrame.pc++];
						opcode = op & 63;
						b = getB9(op);
						switch (opcode) {
						case OP_MOVE: {
							newClosure.upvalues[i] = callFrame.findUpvalue(b);
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
					
					callFrame.pushVarargs(a, b);
					break;
				}
				default: {
					// unreachable for proper bytecode
				}
				} // switch
			}
		} catch (RuntimeException e) {
			while (true) {
				callFrame = currentThread.currentCallFrame();
				if (callFrame == null || !callFrame.fromLua) {
					break;
				}
				currentThread.cleanCallFrames(callFrame);
				currentThread.addStackTrace(callFrame);
				currentThread.popCallFrame();
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

	private final Object getRegisterOrConstant(LuaCallFrame callFrame, int index) {
		Object o;
		int cindex = index - 256;
		if (cindex < 0) {
			o = callFrame.get(index);
		} else {
			o = callFrame.closure.prototype.constants[cindex];
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
			// TODO: consider using math.fmod?
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
		int oldTop = currentThread.getTop();
		currentThread.setTop(oldTop + 4);
		currentThread.objectStack[oldTop] = fun;
		currentThread.objectStack[oldTop + 1] = arg1;
		currentThread.objectStack[oldTop + 2] = arg2;
		currentThread.objectStack[oldTop + 3] = arg3;
		int nReturnValues = call(3);

		Object ret = null;
		if (nReturnValues >= 1) {
			ret = currentThread.objectStack[oldTop];
		}
		currentThread.setTop(oldTop);
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
		return b ? Boolean.TRUE : Boolean.FALSE;
	}

}
