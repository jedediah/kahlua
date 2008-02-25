
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

assert(string.match(s, "Hello") == "Hello")
assert(string.match(s, "H.l%D%w") == "Hello")

