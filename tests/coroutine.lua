do
	local coro = coroutine.create(function(a,b,c)
		assert(a == 11)
		assert(b == 22)
		assert(c == 33)
		return 55, 66, 77
	end)
	assert(coroutine.status(coro) == "suspended")

	local ok, x, y, z = coroutine.resume(coro, 11, 22, 33)
	assert(ok)
	assert(x == 55)
	assert(y == 66)
	assert(z == 77)

	assert(coroutine.status(coro) == "dead")
end

do
	local coro = coroutine.create(function(a,b,c)
		assert(a == 11)
		assert(b == 22)
		assert(c == 33)
		coroutine.yield(55, 66, 77)
		return 1, 2, 3
	end)
	local ok, x, y, z = coroutine.resume(coro, 11, 22, 33)
	assert(ok)
	assert(x == 55)
	assert(y == 66)
	assert(z == 77)

	assert(coroutine.status(coro) == "suspended")

	ok, x, y, z = coroutine.resume(coro)
	assert(ok)
	assert(x == 1)
	assert(y == 2)
	assert(z == 3)

	assert(coroutine.status(coro) == "dead")
end

local sqrt = math.sqrt
local function isprime(n)
	if n % 2 == 0 then
		return false
	end
	local max = sqrt(n)
	for i = 3, max, 2 do
		if n % i == 0 then
			return false
		end
	end
	return true
end

function getprimes()
	coroutine.yield(2)
	for i = 3, 1e10, 2 do
		if isprime(i) then
			coroutine.yield(i)
		end
	end
end

generator = coroutine.wrap(getprimes)
t = {2,3,5,7,11,13,17,19,23}

for i = 1, #t do
	local p1 = t[i]
	local p2 = generator()
	print(p1, p2)
end

--[[
local i = 1
for p in generator do
	local correct = t[i]
	i = i + 1
	assert(p == correct, p .. " ~= " .. correct)
	if p > 10 then
		break
	end
end

for p in generator do
	local correct = t[i]
	i = i + 1
	assert(p == correct, p .. " ~= " .. correct)
	if p > 20 then
		break
	end
end
--]]

do

	local ok, err, stacktrace = pcall(function()
		local f = coroutine.wrap(function(...)
			assert(select("#", ...) == 3)
			error("test")
		end)
		f(11,22,33)
	end)

	assert(not ok)
	assert(err:sub(-4, -1) == "test", err)

	local coro = coroutine.create(function()
		error("test")
	end)
	assert(coroutine.status(coro) == "suspended")

	local status, err = coroutine.resume(coro)

	assert(not status)
	assert(err:sub(-4, -1) == "test")

	assert(coroutine.status(coro) == "dead")

end

