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



public interface JavaFunction {
	/**
	 * General contract
	 * 
	 *  Input:
	 *  stack[base] will be "this", and
	 *  stack[base + 1] .. stack[top - 1] will be the arguments
	 *  
	 *  Output:
	 *  stack[base] .. stack[base + nReturnValues - 1]
	 *  
	 *  If you use stack slots above top - 1, i.e. having more outputs than inputs
	 *  you will have set the top manually. 
	 *  
	 * @param state - the lua state to call on.
	 * @param base  - the base index. 
	 * @return nReturnValues - the number of return values from the function
	 */
	public abstract int call(LuaState state, int base);
}
