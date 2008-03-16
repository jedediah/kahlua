package se.krka.kahlua.vm;

public class LuaCallFrame {
	public LuaThread thread;
	
	public LuaCallFrame(LuaThread thread) {
		this.thread = thread;
	}
	
	public LuaClosure closure;
	public int pc;

	public int localBase;
	int returnBase;
	public int nArguments;

	boolean fromLua;
	public boolean insideCoroutine;
	
	boolean restoreTop;
	
	public void set(int index, Object o) {
		thread.objectStack[localBase + index] = o;
	}

	public Object get(int index) {
		return thread.objectStack[localBase + index];
	}

	public void push(Object x) {
		int top = getTop();
		setTop(top + 1);
		set(top, x);
	}

	public void push(Object x, Object y) {
		int top = getTop();
		setTop(top + 2);
		set(top, x);
		set(top + 1, y);
	}
	
	public final void stackCopy(int startIndex, int destIndex, int len) {
		thread.stackCopy(localBase + startIndex, localBase + destIndex, len);
	}
	
	public void stackClear(int startIndex, int endIndex) {
		for (; startIndex <= endIndex; startIndex++) {
			thread.objectStack[localBase + startIndex] = null;
		}
	}
	
	public void setTop(int index) {
		thread.setTop(localBase + index);
	}

	public void closeUpvalues(int a) {
		thread.closeUpvalues(localBase + a);
	}

	public UpValue findUpvalue(int b) {
		return thread.findUpvalue(localBase + b);
	}

	public int getTop() {
		return thread.getTop() - localBase;
	}

	public void init() {
		pc = 0;
		
		if (closure != null) {
			if (closure.prototype.isVararg) {
				localBase += nArguments;
				
				setTop(closure.prototype.maxStacksize);
				int toCopy = Math.min(nArguments, closure.prototype.numParams);
				stackCopy(-nArguments, 0, toCopy);
			} else {
				setTop(closure.prototype.maxStacksize);
			}
		}
	}

	public void pushVarargs(int index, int n) {
		int nParams = closure.prototype.numParams;
		int nVarargs = nArguments - nParams;
		if (nVarargs < 0) nVarargs = 0;
		if (n == -1) n = nVarargs;
		if (nVarargs > n) nVarargs = n;
		
		setTop(index + n);
		
		stackCopy(-nArguments + nParams, index, nVarargs);
		
		int numNils = n - nVarargs;
		if (numNils > 0) {
			stackClear(index + nVarargs, index + n - 1);
		}
	}
	
	public LuaTable getEnvironment() {
		if (closure != null) {
			return closure.env;
		}
		return thread.state.environment;
	}
}
