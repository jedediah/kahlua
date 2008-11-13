-- test concatenation
do
	local s1, s2 = "hello", "world"
	local s = s1 .. s2
	assert(s == "helloworld")
end

do
	local s1, s2, s3, s4 = "this", "is", "a", "test"
	local s = s1 .. s2 .. s3 .. s4
	assert(s == "thisisatest")
end

do
	local meta = {__concat = function(a, b)
		if type(a) == "table" then
			a = a[1]
		end
		if type(b) == "table" then
			b = b[1]
		end
		return a .. b
	end}
	local t1 = setmetatable({"hello"}, meta)
	local t2 = setmetatable({" "}, meta)
	local t3 = setmetatable({"world"}, meta)
	local s = t1 .. t2 .. t3
	assert(s == "hello world")
end

do
	local t1 = {"hello"}
	local t2 = {" "}
	local t3 = {"world"}
	local s
	local ok, errmsg = pcall(function() s = t1 .. t2 .. t3 end)
	assert(not ok)
end

do
	assert(type(string) == "table")
	assert(type(string.sub) == "function")
	assert(type(string.byte) == "function")
	assert(type(string.len) == "function")
	assert(type(string.char) == "function")
	assert(type(string.lower) == "function")
	assert(type(string.upper) == "function")
	assert(type(string.reverse) == "function")

	-- testing handling of not enough parameters
	assert(not pcall(string.sub))
	assert(not pcall(string.byte))
	assert(not pcall(string.len))
	assert(not pcall(string.lower))
	assert(not pcall(string.upper))
	assert(not pcall(string.reverse))

end


do
	local a,b,c,d,e,f,g = string.byte("Hello world", 1, 5)
	assert(a == string.byte("H"))
	assert(b == string.byte("e"))
	assert(c == string.byte("l"))
	assert(d == string.byte("l"))
	assert(e == string.byte("o"))
	assert(f == nil)
	assert(g == nil)
end

do
	local function testReverse(s)
		assert(#s == #(s:reverse()))
		assert(s:reverse():reverse() == s)
	end 
	testReverse"hello world"
	testReverse""
	testReverse"a"
	testReverse"ÅÄÖ"
	testReverse"adolf i paris rapar sirap i floda"
end

s = "Hello world"

assert(#s == 11)
assert(s:sub(1, 5) == "Hello")
assert(s:sub(1, -1) == "Hello world")
assert(s:sub(1, -5) == "Hello w")
assert(s:sub(1, -7) == "Hello")
assert(s:sub(1, 0) == "")
assert(s:sub(1, -25) == "")

assert(s.sub == string.sub)

assert("ape" < "banana")

assert("xyz" <= "xyz")

assert(s:byte(1) == 72)

assert(string.char(65) == "A")

assert(s:lower() == "hello world")
assert(s:upper() == "HELLO WORLD")

assert(s:match("Hello") == "Hello")
assert(s:match("H.l%D%w") == "Hello")

assert(s:find("worl") == 7)
assert(not s:find("worlld"))

assert(not s:find("%w%w%w%w%w%w"))
assert(s:find("%w%w%w%w%w") == 1)
assert(s:find("%w%w%w%w%w",5) == 7)
assert(not s:find("%w%w%w%w%w",8))

do
	local s2 = "abcdabcd"
	assert(s2:find("bc") == 2)
	assert(s2:match("bc") == "bc")
	assert(s2:match("%wc") == "bc")
	assert(not s2:find("cd",10))
	assert(s2:find("cd",-4) == 7)
	assert(s2:find("cd",-8) == 3)
	
	assert(s2:find("bcd$") == 6)
	assert(not s2:find("abc$"))
	assert(s2:find("^abcdabcd$") == 1)
	assert(not s2:find("^abcd$"))
end	

