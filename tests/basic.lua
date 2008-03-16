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
	function f()
		f()
	end

	local ok, errorMsg = pcall(f)
	assert(not ok)
end

