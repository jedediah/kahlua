local meta = {}
function meta.__add(a, b)
	local c = {}
	for i = 1, math.max(#a, #b) do
		c[i] = (a[i] or 0) + (b[i] or 0)
	end
	return c
end

function meta.__sub(a, b)
	local c = {}
	for i = 1, math.max(#a, #b) do
		c[i] = (a[i] or 0) - (b[i] or 0)
	end
	return c
end

local a = setmetatable({}, meta)
local b = setmetatable({}, meta)
a[1] = 15
a[2] = 30
a[3] = 20

b[1] = 9
b[2] = 51

local c = a + b
assert(type(c) == "table")
assert(c[1] == 24)
assert(c[2] == 81)
assert(c[3] == 20)

local c = a - b
assert(type(c) == "table")
assert(c[1] == 6)
assert(c[2] == -21)
assert(c[3] == 20)

function endswith(s1, s2)
	return s1:sub(-#s2, -1) == s2
end

do
	local meta = {}
	meta.__index = meta
	meta.__newindex = meta

	local t = setmetatable(meta, meta)
	local ok, errmsg = pcall(function() return t.hello end)
	assert(not ok, "expected recursive metatable error")
	assert(endswith(errmsg, "loop in gettable"), "wrong error message: " .. errmsg)
	
	local ok, errmsg = pcall(function() t.hello = "world" end)
	assert(not ok, "expected recursive metatable error")
	assert(endswith(errmsg, "loop in settable"), "wrong error message: " .. errmsg)
end

do
	local t1, t2 = {}, {}
	local ok, errmsg = pcall(function() return t1 + t2 end)
	assert(not ok)
	--assert(endswith(errmsg, "no meta function was found for __add"))
	
	local ok, errmsg = pcall(function() return -t1 end)
	assert(not ok)

	local ok, errmsg = pcall(function() return t1 <= t2 end)
	assert(not ok)
	
	local ok, errmsg = pcall(function() return t1 == t2 end)
	assert(ok)
end


do
	local meta = {__lt = function(a, b) return true end}
	local t1 = setmetatable({}, meta)
	local t2 = setmetatable({}, meta)
	assert(t1 < t2)
	assert(t2 < t1)
	assert(not (t1 <= t2))
	assert(not (t2 <= t1))
end

do
	local meta1 = {__lt = function(a, b) return true end}
	local meta2 = {__lt = function(a, b) return false end}
	local t1 = setmetatable({}, meta1)
	local t2 = setmetatable({}, meta2)
	local ok, errmsg = pcall(function() assert(t1 < t2) end)
	assert(not ok)
end

