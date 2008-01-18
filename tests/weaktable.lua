local function count(t)
	local n = 0
	for k in next, t do
		n = n + 1
	end
	return n
end

local t = {}

t["a"] = 1
t["b"] = 1
t["c"] = 1
assert(count(t) == 3)

setmetatable(t, {__mode = "k"})

assert(count(t) == 3)
t["d"] = 1
assert(count(t) == 4)

local t2 = {}
t[t2] = 1
assert(count(t) == 5)

t2 = nil
collectgarbage();
collectgarbage();
collectgarbage();
assert(count(t) == 4)

local t3 = {}
t[t3] = 1
assert(count(t) == 5)
collectgarbage();
collectgarbage();
collectgarbage();
assert(count(t) == 5)
setmetatable(t, {__mode = "v"})
t3 = nil
collectgarbage();
collectgarbage();
collectgarbage();
assert(count(t) == 5)
local t4 = {}
t[1] = t4
assert(count(t) == 6)
collectgarbage();
collectgarbage();
collectgarbage();
t4 = nil
collectgarbage();
collectgarbage();
collectgarbage();
assert(count(t) == 5)
