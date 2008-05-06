local meta = {}
meta.__index = _G

env1 = setmetatable({}, meta)
function f1()
	x = "f1"
	assert(getfenv() == env1)
	
	assert(getfenv(0) == _G)
	assert(getfenv(1) == env1)
	assert(getfenv(2) == env2)
	assert(getfenv(3) == env3)
	assert(getfenv(4) == env4)
	
	assert(getfenv(f1) == env1)
	assert(getfenv(f2) == env2)
	assert(getfenv(f3) == env3)
	assert(getfenv(f4) == env4)
end
setfenv(f1, env1)

env2 = setmetatable({}, meta)
function f2()
	x = "f2"
	f1()
	assert(x == "f2")
end
setfenv(f2, env2)

env3 = setmetatable({}, meta)
function f3()
	x = "f3"
	f2()
	assert(x == "f3")
end
setfenv(f3, env3)

env4 = setmetatable({}, meta)
function f4()
	x = "f4"
	f3()
	assert(x == "f4")
end
setfenv(f4, env4)

assert(x == nil)
f4()
assert(x == nil)

