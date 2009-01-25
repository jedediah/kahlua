/*
Copyright (c) 2008 Kristofer Karlsson <kristofer.karlsson@gmail.com>

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

import java.util.Vector;

public class LuaThread {
	public LuaThread parent;
	
	public String stackTrace = "";

	public Vector liveUpvalues;

	public static final int MAX_STACK_SIZE = 1000;
	public static final int INITIAL_STACK_SIZE = 10;

	private static final int MAX_CALL_FRAME_STACK_SIZE = 100;
	private static final int INITIAL_CALL_FRAME_STACK_SIZE = 10;
	
	public Object[] objectStack;
	public int top;

	public LuaCallFrame[] callFrameStack;
	public int callFrameTop;
	
	public LuaState state;

	public int expectedResults;
	
	public LuaThread(LuaState state) {
		this.state = state;
	}

	private void init() {
		if (objectStack == null) {
			objectStack = new Object[INITIAL_STACK_SIZE];
			callFrameStack = new LuaCallFrame[INITIAL_CALL_FRAME_STACK_SIZE];
			liveUpvalues = new Vector();
		}
	}
	
	public LuaCallFrame pushNewCallFrame(LuaClosure closure, int localBase, int returnBase, int nArguments, boolean fromLua, boolean insideCoroutine) {
		setCallFrameStackTop(callFrameTop + 1);
		LuaCallFrame callFrame = currentCallFrame();
		
		callFrame.localBase = localBase;
		callFrame.returnBase = returnBase;
		callFrame.nArguments = nArguments;
		callFrame.fromLua = fromLua;
		callFrame.insideCoroutine = insideCoroutine;
		callFrame.closure = closure;
		
		return callFrame;
	}

	public void popCallFrame() {
		if (callFrameTop == 0) {
			throw new RuntimeException("Stack underflow");			
		}
		setCallFrameStackTop(callFrameTop - 1);
	}
	
	private final void ensureCallFrameStackSize(int index) {
		init();
		if (index > MAX_CALL_FRAME_STACK_SIZE) {
			throw new RuntimeException("Stack overflow");			
		}
		int oldSize = callFrameStack.length;
		int newSize = oldSize;
		while (newSize <= index) {
			newSize = 2 * newSize;
		}
		if (newSize > oldSize) {
			LuaCallFrame[] newStack = new LuaCallFrame[newSize];
			System.arraycopy(callFrameStack, 0, newStack, 0, oldSize);
			callFrameStack = newStack;
		}
	}

	public final void setCallFrameStackTop(int newTop) {
		if (newTop > callFrameTop) {
			ensureCallFrameStackSize(newTop);
		} else {
			callFrameStackClear(newTop, callFrameTop - 1);
		}
		callFrameTop = newTop;
	}
	
	private void callFrameStackClear(int startIndex, int endIndex) {
		for (; startIndex <= endIndex; startIndex++) {
			LuaCallFrame callFrame = callFrameStack[startIndex];
			if (callFrame != null) {
				callFrameStack[startIndex].closure = null;
			}
		}
	}

	private final void ensureStacksize(int index) {
		init();
		if (index > MAX_STACK_SIZE) {
			throw new RuntimeException("Stack overflow");			
		}
		int oldSize = objectStack.length;
		int newSize = oldSize;
		while (newSize <= index) {
			newSize = 2 * newSize;
		}
		if (newSize > oldSize) {
			Object[] newStack = new Object[newSize];
			System.arraycopy(objectStack, 0, newStack, 0, oldSize);
			objectStack = newStack;
		}
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
		if (len > 0 && startIndex != destIndex) {
			System.arraycopy(objectStack, startIndex, objectStack, destIndex, len);
		}
	}

	public final void stackClear(int startIndex, int endIndex) {
		for (; startIndex <= endIndex; startIndex++) {
			objectStack[startIndex] = null;
		}
	}    

	/*
	 * End of stack code
	 */

	public final void closeUpvalues(int closeIndex) {
		// close all open upvalues
		
		int loopIndex = liveUpvalues.size();
		while (--loopIndex >= 0) {
			UpValue uv = (UpValue) liveUpvalues.elementAt(loopIndex);
			if (uv.index < closeIndex) {
				return;
			}
			uv.value = objectStack[uv.index];
			uv.thread = null;
			liveUpvalues.removeElementAt(loopIndex);
		}
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
		uv.thread = this;
		uv.index = scanIndex;
		
		liveUpvalues.insertElementAt(uv, loopIndex + 1);
		return uv;				
	}

	public LuaCallFrame currentCallFrame() {
		if (callFrameTop > 0) {
			LuaCallFrame callFrame = callFrameStack[callFrameTop - 1]; 
			if (callFrame == null) {
				callFrame = new LuaCallFrame(this);
				callFrameStack[callFrameTop - 1] = callFrame;
			}
			return callFrame;
		}
		return null;
	}

	public int getTop() {
		return top;
	}

	public void cleanCallFrames(LuaCallFrame callerFrame) {
		LuaCallFrame frame;
		while ((frame = currentCallFrame()) != callerFrame) {
			closeUpvalues(frame.returnBase);
			addStackTrace(frame);				
			popCallFrame();
		}
	}

	void addStackTrace(LuaCallFrame frame) {
		if (frame.closure != null) {
			int[] lines = frame.closure.prototype.lines;
			if (lines != null) {
				frame.pc--;
				if (frame.pc < lines.length) {
					stackTrace += "at " + frame.closure.prototype + ":" + lines[frame.pc] + " (opcode: " + frame.pc + ")\n";
				}
			}
		}
	}
}
