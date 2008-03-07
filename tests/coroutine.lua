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
	yield(2)
	for i = 3, 1e10, 2 do
		if isprime(i) then
			yield(i)
			break
		end
	end
end

generator = coroutine.wrap(getprimes)

for p in generator do
	print(p, "is prime!")
end

