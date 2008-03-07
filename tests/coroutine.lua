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

