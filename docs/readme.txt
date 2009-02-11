<wiki:toc max_depth="2" />

Kahlua is a Virtual Machine together with a standard library,
all implemented in Java. It tries to emulate Lua as much as possible,
while still reusing as much as possible from Java. Nothing has been cloned or
ported directly from Lua, instead it's done by reverse engineering the
features that exists in Lua, mostly by using the Lua reference manual and PiL
as reference.

The target platform for Kahlua is J2ME (CLDC 1.1), which is commonly
included in mobile phones.
Everything in CLDC 1.1 is also included in standard Java so this will
run in most Java environments.

Kahlua can easily be integrated with a J2ME project - 
see the example midlet in the distribution.
The Java source code just one class file and about 130 lines
of easy to read Java.

Kahlua may also be used for J2SE- or J2EE projects, but you would need to
use a stand alone lua compiler, since Kahlua lacks one.
This reduces the usefulness of Kahlua on J2SE environments.

=Goals=
The Kahlua project has the following goals (in no particular order):
  * Small class file footprint
  * Fast runtime of the most common operations
  * Same behaviour as standard Lua
  * Must be able to run on CLDC 1.1
  * Compact and non-redundant source
  * Maintain a large test base and high test coverage.

=License=
Kahlua is distributed under the MIT licence which is the same as standard Lua
which means you can
pretty much use it in any way you want.
However, I would very much appreciate bug reports, bug fixes, optimizations
or simply any good idea that might improve Kahlua.

=Building Kahlua=
Kahlua uses Apache Ant to build itself.
Simply run `ant` from the project root to compile, package and run all the test
cases.

==Testing==
Kahlua is developed by Test Driven Development and
uses its own test suite (implemented in Lua) to verify that it's
always working correctly. It currently has about 98% code coverage.

After having built Kahlua, you can find a test report inside `testreport.html`
and a coverage report inside `testsuite/coverage/coverage.html`.

=Differences compared with regular Lua=
Kahlua is not fully compatible with Lua, which is to be expected as it only
runs from Java CLDC 1.1.

Some functions in the standard library are missing and some functions do not
behave exactly the same way. This is all documented in order to help the
transition
from Lua to Kahlua.

==Basic Kahlua types==
The fundamental types in Lua of course have their corresponding type in Kahlua.
`nil`, `number`, `boolean` and `string` map directly to `null`, `Double`, `Boolean` and
`String`.
`table` and `coroutine` map to Kahlua's own `LuaTable` and `LuaThread`.
`function` maps to either the `JavaFunction` interface if it's implemented in Java
and `LuaClosure` if it's a Lua function.

This means that you can pass doubles, booleans and strings directly into Lua
without
any problems.

==Garbage collection==
Kahlua uses Javas own garbage collection with means that all of the
collectgarbage
options are not supported, as well as the `__gc` metatable mode.

==Userdata==
Kahlua does not share Luas concept of userdata.
Instead, you can simply pass any object into Kahlua.
In order to operate on them, you must set the metatable for the class,
which will then
apply to all objects of exactly that class (not subclasses).

The only way to operate on userdata is to export java functions that can
manipulate them.
(See example of this in the `UserdataArray` class.)

==Strings==
Strings in Kahlua are represented in UTF-16, just like Java strings
(this is because Kahlua strings are in fact represented as Java strings).

==Missing functions==
The following functions are not implemented in Kahlua:
  * io.`*`
  * debug.`*`
  * string.dump
  * loadstring, loadfile, load, dofile
  * gcinfo
  * xpcall
  * os.execute, os.exit, os.setlocale, os.getenv, os.remove, os.clock, os.tmpname, os.rename
  * newproxy
  * gcinfo (deprecated in lua)

==Functions with different behaviour==
These functions behave almost like in Lua:

===pcall===
`pcall` only differs in the return values on failure.
`pcall` in Lua returns false, errormessage
`pcall` in Kahlua returns:
  # `false`
  # `errormessage`
  # `stacktrace` (as a string with newlines separating each call frame)
  # the original java exception

===collectgarbage===
`collectgarbage([opt])` works like this for the following values of `opt`:
  * `null`, `"step"`, `"collect"` all run `System.gc()` in Java, which doesn't guarantee any specific behaviour in Java
  * `"count"` returns three values: `usedMemory`, `freeMemory`, `totalMemory` (all in KB)
  * all other values for `opt` result in an error.

===error===
Instead of being `error(message, [level])` as in Lua,
Kahlua has `error(message, [stacktrace])`.
`stacktrace` is appended to the total stacktrace when caught by `pcall`.

Just like in Lua, if `error` is called with zero parameters, it does nothing.

==Functions that behave the same as in Lua==
  * assert
  * getfenv, setfenv
  * getmetatable, setmetatable
  * next, ipairs, pairs
  * rawequal, rawget, rawset
  * select
  * tonumber, tostring
  * type
  * unpack
  * module
  * require
  * coroutine.`*`
  * string.`*`
  * table.`*`
  * math.`*`
  * os.date, os.difftime, os.time


==Extra functions==
===debugstacktrace===
There is already a similar function in Lua,
called `debug.traceback ([thread,] [message] [, level])`.

`debugstacktrace` in Kahlua is used like this:
`debugstacktrace(thread, level, count, halt_at)`.
  * `thread` is the thread to inspect. A value of `nil` represents the current thread.
  * `level` is an integer >= 0 representing at which stack level to start. level 0 is debugstacktrace itself, level 1 is the function that called `debugstacktrace`, and so on. The default value is 0.
  * `count` is an integer representing the upper limit of how many stackframes to return. Defaults to all stackframes
  * `halt_at` is an integer representing the stack height at which to stop giving a stacktrace. The default value is 0.


==Compat options==
Kahlua implements no compat-options, i.e. no support for `arg` as in Lua 5.0

==Lack of a compiler==
Kahlua currently does not have a compiler,
and can only load precompiled Lua bytecode.
This is something that may be added in the future.

Instead Kahlua can load precompiled lua 5.1b bytecode.
Use luac -o output.lbc input.lua to compile a lua source file to bytecode.
You can then use `LuaPrototype.loadBytecode` to get a lua closure.
See se.krka.kahlua.test.Test to see examples of usage.

==Thread safety==
Kahlua is not thread safe in any way.
Do not attempt to use a `LuaState` concurrently from different threads.
Also do not try to move a `LuaFunction` or `JavaFunction` into a different state.
Sharing of booleans, numbers, strings and tables is safe,
but you must manually ensure that no thread is writing to a `LuaTable` while
another is reading (or writing), since the data structure is not thread safe.

However, you can create as many `LuaState`s as you want to run in different
threads if they do not communicate directly. All `LuaState`s are completely
independent of one another and do not share any writeable static fields.

=Major changes since Kahlua release 2008-10-11=
  * Strings used to be interned everywhere, but this has been changed for three reasons:
    # Always interning strings is a large strain on the permgen heap space which may be limited in size. It may even be dangerous to exceed the permgen limit on J2ME devices.
    # Taking advantage of string equality as identity did not give significant enough performance increase to motivate the harder semantics.
    # It was hard to use Java API, since you had to take responsibility for interning all strings that may lead into Lua.
  
    This means that you:
    # must stop assuming that strings from Lua have been interned and can be compared for equality by using identity.
    # should stop interning strings that you send to Lua.
  
  * Environments are stored per `LuaThread`, not `LuaState`. This means that you can't access `LuaState.environment` any more - however, there is now a convenience method `LuaState.getEnvironment()` that gets the environment from the current thread.
  
=Java API=
It's quite easy to add Kahlua to your Java application.
For a quick start guide, I recommend looking at `Test.java` and the midlet demo
distributed with Kahlua. They are both quite simple classes. The midlet even
shows how to export its own function to Kahlua.

==Exporting your own functions to Lua==
Kahlua defines an interface called `JavaFunction`, which needs only one method:
{{{
int call(LuaCallFrame callFrame, int nArguments);
}}}

`callFrame` contains all the input arguments, which can be reached by using
`callFrame.get(i)`, where 0 <= `i` < `nArguments`

Note that `nArguments` is how many arguments it was called with,
not how many you expect. You should be prepared to handle both when you receive
fewer or more arguments than expected.
Also note that all numbers from Lua will be in the type Double,
so if you're expecting an int or a long, you have to do the conversion yourself.

It's safe to throw any kind of `RuntimeException` from here - Lua will catch it
and convert it to a proper Lua error.

Input is only half of the fun though, you may also want to send some output
back. The outputs are placed at the same place as the inputs, so you can
either choose to overwrite them directly, or push the return values above them.
Kahlua will take care of removing the unwanted objects after the return.

Use `callFrame.push(object)` to push as many objects that you want to return.
Then, return that same number.

You can also use `callFrame.setTop(nReturnValues)` to specify how many
return values you want, and then use `callFrame.set(index, object)` to fill up
the list of return values. As with the input, 0 <= `index` < `nReturnValues`.

In either case, ALWAYS remember to return the number of return values, which
must be an integer >= 0.
For examples on how `JavaFunctions` typically look, see any of the classes in
`se.krka.kahlua.stdlib`

==Inspecting Lua from Java==

Unlike Lua, Kahlua does not need a large set of API functions to be useful.
While Lua provides a clear distinction between Lua space and C space,
Kahlua does not do the same for Java. Lua objects are not directly available
to C code, so instead you must manipulate them by using the API helper
functions. For instance, to get a value out of a table in Lua, you must do
something like:
{{{
lua_State *L = myState;
lua_getglobal(L, "myTable");
lua_pushstring(L, "myKey");
lua_gettable(L, stack_index);
lua_gettable(L, -2);
// the value is now at stack index -1, to inspect it, use
if (lua_isboolean(L, -1)) {
   bool value = lua_toboolean(L, -1);
}
}}}
In Kahlua, you don't need such helper functions, you can use the Java objects directly:

{{{
LuaState state = myState;
LuaTable table = (LuaTable) state.getEnvironment().rawget("myTable");
Object value = table.rawget("myKey");
if (value instanceof Boolean) {
	boolean value = ((Boolean) value).booleanValue();
}
}}}
The only helper functions you really need are
`LuaState.getEnvironment()` and `LuaState.pcall()`.


