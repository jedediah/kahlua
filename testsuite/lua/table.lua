local t = {}

for i = 1, 100 do
   t[i] = i * 3
end

assert(next(t), "next must not be nil")

do
   local c = 0
   for k in pairs(t) do
      c = c + 1
   end
   assert(c == 100, "wrong number of elements in table")

end

for k, v in pairs(t) do
   assert(t[k] == v)
   assert(t[k] == 3 * k)
end

local a, b = -1, 0
t[a * b] = -1
t[b] = 1

assert(t[a * b] == 1)


rawset(t, "hello", "world")
assert(rawget(t, "hello") == "world")
setmetatable(t, {__index = function() return nil end, __newindex = function() end})
assert(rawget(t, "hello") == "world")
rawset(t, "hello", "WORLD")
assert(rawget(t, "hello") == "WORLD")


do
	local t = {}
	for i = 1, 6 do
		for j = 1, 2^i do
			t[i] = i^2
		end
		for k, v in next, t do
			assert(k^2 == v)
			t[k] = nil
		end
	end
end

function endswith(s1, s2)
	return s1:sub(-#s2, -1) == s2
end


local status, errmsg = pcall(function() local t = {} t[0/0] = 1 end)
assert(not status)
assert(endswith(errmsg, "table index is NaN"))

local status, errmsg = pcall(function() local t = {} t[nil] = 1 end)
assert(not status)
assert(endswith(errmsg, "table index is nil"))

local status, errmsg = pcall(function() local t = {} next(t, "bad key") end)
assert(not status)
assert(endswith(errmsg, "invalid key to 'next'"))

do
	t = {1, 2, 3, 4, 5, 6, 7}
	assert(#t == 7)
	
	t = {math.cos(1)}
	assert(#t == 1)
	
	function f() return 1 end
	t = {f()}
	assert(#t == 1)
	
	function f() return 1, 2, 3, 4, 5 end
	t = {f()}
	assert(#t == 5)

	t = {1, 2, 3, f()}
	assert(#t == 8)

	t = {f(), 1, 2, 3}
	assert(#t == 4)

	t = {f(), nil}
	assert(#t == 1)
end

do
    if tableconcat==nil then
        tableconcat = table.concat
    end
	local t = {"Hello", "World"}
	assert(tableconcat(t) == "HelloWorld")

	t = {"Hello", "World"}
	assert(tableconcat(t, " ") == "Hello World")
	
	t = {"Hello", "World"}
	assert(tableconcat(t, 1.5) == "Hello1.5World")

	t = {"a", "b", "c"}
	assert(tableconcat(t, " ") == "a b c")
	
	t = {"a", "b", "c"}
	assert(tableconcat(t, " ", 1, 3) == "a b c")
	
	t = {"a", "b", "c"}
	assert(tableconcat(t, " ", 2, 3) == "b c")
	
	t = {"a", "b", "c"}
	assert(tableconcat(t, " ", 1, 2) == "a b")
	
	t = {"a", "b", "c"}
	assert(tableconcat(t, " ", 1, 1) == "a")
	
	t = {"a", "b", "c"}
	assert(tableconcat(t, " ", 100, 99) == "")	
end

do
	local function sortAndVerify(t)
		local len = #t
		table.sort(t)
		assert(len == #t)
		if len > 1 then
			local prev = t[1]
			for i = 2, #t do
				local cur = t[i]
				assert(not (cur < prev))
				prev = cur
			end
		end
		
	end
	sortAndVerify{1000, 55, [0] = 0}
	sortAndVerify{1000, 55, 10}
	sortAndVerify{1000}
	sortAndVerify{1000, 100, 2000, 200}
	sortAndVerify{1000, 100, 2000, 200, 150}
	sortAndVerify{1, 2, 3, 4, 5, 6}
	sortAndVerify{6, 5, 4, 3, 2, 1}
end

