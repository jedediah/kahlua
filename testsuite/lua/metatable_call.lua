local mt = {}
local t = setmetatable({}, mt)

function mt.__call(caller, ...)
	assert(caller == t)
	local a, b, c, d = ...
	assert(a == 1)
	assert(b == 2)
	assert(c == 3)
	assert(d == nil)
end

t(1, 2, 3)

