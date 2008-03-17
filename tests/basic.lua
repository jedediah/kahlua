function foo()
   assert(1.2 == 1.2)
end

function bar()
   assert(4.2 == 4.2)
end


foo()
bar()

assert("hej" == "hej")

do
	local t = {}
	t.a = true
	assert(t.a == true)
	t.a = false
	assert(t.a == false)
	t.a = nil
	assert(t.a == nil)
end


do
	local function foo(...)
		return select("#", ...)
	end
	assert(foo(1, 2, 3) == 3)
end

do
	local a, b = nil, nil
	assert(a == b)
	assert(rawequal(a, b))
	a = 1.0
	b = 1.0
	assert(rawequal(a, b))
end

do
	local x = 1.2
	local ok, errmsg = pcall(function() (1.2).x = y end)
	assert(not ok)
end

do
	function f()
		f()
	end

	local ok, errorMsg = pcall(f)
	assert(not ok)
end

do
	local ok, errmsg = pcall(function() error(nil) end)
	assert(not ok)
	assert(errmsg == nil)
end

do
	local ok, errmsg = pcall(function() error() end)
	assert(ok)
end

do
	local t = {}
	local ok, errmsg = pcall(function() error(t) end)
	assert(not ok)
	assert(errmsg == t)
end

