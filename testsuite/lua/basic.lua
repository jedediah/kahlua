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

	assert(not (a == b))
	assert(not rawequal(a, b))
	b = 1.0
	assert(a == b)
	assert(rawequal(a, b))
	b = 2.0
	assert(not (a == b))
	assert(not rawequal(a, b))
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



do
	local function test(a, b)
		-- test OP_LT
		if a < b then
			assert(true)
		else
			assert(false)
		end
		if not (a < b) then
			assert(false)
		else
			assert(true)
		end
		if b < a then
			assert(false)
		else
			assert(true)
		end
		if not (b < a) then
			assert(true)
		else
			assert(false)
		end

		-- test OP_LE
		if a <= b then
			assert(true)
		else
			assert(false)
		end
		if not (a <= b) then
			assert(false)
		else
			assert(true)
		end
		if b <= a then
			assert(false)
		else
			assert(true)
		end
		if not (b <= a) then
			assert(true)
		else
			assert(false)
		end
	end
	test(1, 2)
	test("1", "2")

end

do
	local ok, errmsg = pcall(function() (nil)() end)
	assert(not ok)

	local ok, errmsg = pcall(function() return (nil)() end)
	assert(not ok)
end

