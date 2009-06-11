/*
 Copyright (c) 2009 Kristofer Karlsson <kristofer.karlsson@gmail.com>

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

package se.krka.kahlua.threading;

import java.util.concurrent.atomic.AtomicInteger;

import java.io.IOException;

import se.krka.kahlua.luaj.compiler.LuaCompiler;

import se.krka.kahlua.vm.LuaClosure;

import java.io.PrintStream;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import se.krka.kahlua.vm.LuaState;
import se.krka.kahlua.vm.LuaTable;

/**
 * 
 * A decorator for a LuaState, which makes it easier to use the same LuaState in multiple
 * threads.
 * Note that this does not mean that the you can run several functions concurrently in the LuaState,
 * it will in fact just ensure that they do NOT run at the same time, in order to avoid a broken
 * Lua state.
 *
 * You MUST lock the state before doing any operation on it, and unlock it when you're done.
 * Calling pcall will grab the lock implicitly.
 */
public class ThreadSafeLuaState extends LuaState {
	private final Lock lock = new ReentrantLock();
	
	
	public ThreadSafeLuaState(PrintStream stream) {
		super(stream, false);
		reset();
	}

	public ThreadSafeLuaState() {
		this(System.out);
	}	
		
	
	@Override
	public void lock() {
		lock.lock();
	}
	
	@Override
	public void unlock() {
		lock.unlock();
	}

	@Override
	public int call(int arguments) {
		try {
			lock();
			return super.call(arguments);
		} finally {
			unlock();
		}
	}

	@Override
	public int pcall(int arguments) {
		try {
			lock();
			return super.pcall(arguments);
		} finally {
			unlock();
		}
	}

	@Override
	public Object[] pcall(Object fun) {
		try {
			lock();
			return super.pcall(fun);
		} finally {
			unlock();
		}
	}

	@Override
	public final Object[] pcall(Object fun, Object[] args) {
		lock();
		try {
			return super.pcall(fun, args);
		} finally {
			unlock();
		}		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void setUserdataMetatable(Class type, LuaTable metatable) {
		try {
			lock();
			super.setUserdataMetatable(type, metatable);
		} finally {
			unlock();
		}
	}
	
	@Override
	public Object call(Object fun, Object arg1, Object arg2, Object arg3) {
		try {
			lock();
			return super.call(fun, arg1, arg2, arg3);
		} finally {
			unlock();
		}
	}

	@Override
	public Object call(Object fun, Object[] args) {
		try {
			lock();
			return super.call(fun, args);
		} finally {
			unlock();
		}
	}

	@Override
	public LuaTable getEnvironment() {
		try {
			lock();
			return super.getEnvironment();
		} finally {
			unlock();
		}
	}

	@Override
	public Object getMetaOp(Object o, String meta_op) {
		try {
			lock();
			return super.getMetaOp(o, meta_op);
		} finally {
			unlock();
		}
	}

	@Override
	public Object getmetatable(Object o, boolean raw) {
		try {
			lock();
			return super.getmetatable(o, raw);
		} finally {
			unlock();
		}
	}

	@Override
	public LuaClosure loadByteCodeFromResource(String name, LuaTable environment) {
		try {
			lock();
			return super.loadByteCodeFromResource(name, environment);
		} finally {
			unlock();
		}
	}

	@Override
	public Object tableGet(Object table, Object key) {
		try {
			lock();
			return super.tableGet(table, key);
		} finally {
			unlock();
		}
	}

	@Override
	public void tableSet(Object table, Object key, Object value) {
		try {
			lock();
			super.tableSet(table, key, value);
		} finally {
			unlock();
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		final ThreadSafeLuaState state = new ThreadSafeLuaState();
		//final LuaState state = new LuaState();
		
		final LuaClosure c = LuaCompiler.loadstring("x = (x or 0) + 1", "", state.getEnvironment());
		final AtomicInteger counter = new AtomicInteger(0);
		for(int i = 0; i < 100; i++) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (int i = 0; i < 100; i++) {
						try {
							Thread.sleep((long) (Math.random() * 10));
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						state.pcall(c);
					}
					counter.incrementAndGet();
				}
				
			}).start();
		}
		while (counter.get() != 100) {
			Thread.sleep(100);
		}
		final LuaClosure c2 = LuaCompiler.loadstring("print('x='..x)", "", state.getEnvironment());
		state.pcall(c2);
	}
}
