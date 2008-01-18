assert(string)
assert(string.sub)

s = "Hello world"

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
