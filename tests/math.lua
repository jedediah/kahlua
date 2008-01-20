function isInf(x)
	return x + 1 == x
end

function isPosInf(x)
	return isInf(x) and x > 0
end

function isNegInf(x)
	return isInf(x) and x < 0
end

function isNaN(x)
	return not (x == x) and not isInf(x)
end
	

assert(1 < 2, "1 must be less than 2")
assert(not (2 < 1), "2 must not be less than 1")

do
	local value1 = 1.234567
	local value2 = 1.234567
	value2 = value2 * 2
	value2 = value2 / 2

	assert(value1 == value2)
	assert(rawequal(value1, value2))
	
	local t = {}
	t[value1] = 1
	t[value2] = 2
	assert(t[value1] == 2)
	assert(t[value2] == 2)

	local zero = 0
	value1 = 1 * zero
	value2 = -1 * zero
	t[value1] = 1
	t[value2] = 2
	assert(t[value1] == 2)
	assert(t[value2] == 2)
end

function assertEquals(a, b)
	local errMsg = "expected " .. tostring(a) .. " = " .. tostring(b)
   local assert = assert
   local type = type
   assert(type(a) == type(b), "not same type")
   if (type(a) == "number") then
      local f = math.abs
      local v = a - b;
      local v2 = math.abs(v)
      assert(v2 < 1e-6, errMsg)
   else
      assert(a == b, errMsg)
   end
end

assertEquals(1, 1)
assertEquals(2.123, 2.123)

assertEquals(math.cos(1), 0.540302306)
assertEquals(math.cos(0.234), 0.972746698)


assertEquals(math.sin(0.234), 0.231870355)

do
   local x = 123
   assertEquals(-x, -123)
end

assertEquals(true, not not true)
assertEquals(true, not not 1)
assertEquals(false, not 0)


do
   local a = 1
   local b = 2
   local c = 0
   assert(a / c == b / c, "+inf broken")
   assert(-a / c == -b / c, "-inf broken")

   local s = tostring(c % c):lower()
   assert(s == "nan", "0 % 0 was: " .. s)

end

do
	for i = 1, 10, 0.1 do
		assertEquals(math.sqrt(i^2), i)
	end
end

do
	for i = 1, 10, 0.1 do
		assertEquals(3^(-i), 1/(3^i))
	end
	
	assert(isNaN((-123)^1.1))

	local mult = 1	
	for i = 0, 20 do
		local x = 1.123
		assertEquals(x ^ i, mult)
		mult = mult * x;
	end
	
end

do
	assert(isPosInf(math.floor(1/0)))
	assert(isNegInf(math.floor(-1/0)))
	assert(isNaN(math.floor(0/0)))
	assert(isPosInf(math.ceil(1/0)))
	assert(isNegInf(math.ceil(-1/0)))
	assert(isNaN(math.ceil(0/0)))

	for i = -2, 2, 0.01 do
		local f = math.floor(i)
		local c = math.ceil(i)
		assert(c >= f)
		if c == f then
			assert(i == c)
			assert(i == f)
		end
	end
end

do	
	local a, b
	a, b = math.modf(1 / 0)
	assert(isPosInf(a))
	assert(b == 0)
	
	a, b = math.modf(-1 / 0)
	assert(isNegInf(a))
	assert(b == 0)

	a, b = math.modf(0 / 0)
	assert(isNaN(a))
	assert(isNaN(b))

	a, b = math.modf(-2.5)
	assert(a == -2)
	assert(b == -.5)

	for i = -2, 2, 0.01 do
		local ipart, fpart = math.modf(i)
		assert(ipart + fpart == i)
	end
end

local function assertInterval(value, low, high)
	assert(value >= low)
	assert(value <= high)
end

for i = -10, 10, 0.01 do
	assertInterval(math.cos(i), -1, 1)
	assertInterval(math.sin(i), -1, 1)
end

for i = -1, 1, 0.01 do
	assertInterval(math.acos(i), -math.pi, math.pi)
	assertInterval(math.asin(i), -math.pi, math.pi)
end
do
	local v
	v = math.acos(1.01) assert(isNaN(v), "expected NaN, got " .. v)
	v = math.acos(-1.01) assert(isNaN(v), "expected NaN, got " .. v)
	v = math.asin(1.01) assert(isNaN(v), "expected NaN, got " .. v)
	v = math.asin(-1.01) assert(isNaN(v), "expected NaN, got " .. v)
end

for i = -10, 10, 0.01 do
	local v1 = math.cos(i) ^ 2 + math.sin(i) ^ 2
	assertEquals(v1, 1)
end

for i = 0, math.pi, 0.01 do
	assertEquals(math.acos(math.cos(i)), i)
end

for i = -math.pi / 2, math.pi / 2, 0.01 do
	assertEquals(math.asin(math.sin(i)), i)
	assertEquals(math.atan(math.tan(i)), i)
end

for i = -100, 100, 0.5 do
	assertEquals(math.tan(math.atan(i)), i)
end

assert(math.atan(0) == 0)
assertEquals(math.atan(1 / 0), math.pi/2)
assertEquals(math.atan(-1 / 0), -math.pi/2)
assertEquals(math.atan2(1 / 0, 123), math.pi/2)
assertEquals(math.atan2(-1 / 0, 123), -math.pi/2)

assertEquals(math.atan2(2, 123), 0.01625872980513)
assertEquals(math.atan2(2, -123), 3.1253339237847)

assertEquals(math.atan2(2, 3), 0.58800260354757)
assertEquals(math.atan2(2, -3), 2.5535900500422)
assertEquals(math.atan2(-2, 3), -0.58800260354757)
assertEquals(math.atan2(-2, -3), -2.5535900500422)

assert(isNaN(math.sin(1 / 0)))
assert(isNaN(math.cos(1 / 0)))
assert(isNaN(math.tan(1 / 0)))

do
	local v = math.atan2(1, 0)
	for i = 0, 10 do
		assert(0 == math.atan2(0, i))
	end
	for i = 1, 20 do
		assertEquals(v, math.atan2(i, 0))
		assert(math.atan2(i, 2) == math.atan(i / 2))
	end
end

do
	assert(isNaN(math.rad(0/0)))
	assert(isNaN(math.deg(0/0)))
	assert(isPosInf(math.rad(1/0)))
	assert(isPosInf(math.deg(1/0)))
	assert(isNegInf(math.rad(-1/0)))
	assert(isNegInf(math.deg(-1/0)))
	
	assertEquals(math.pi, math.rad(180))
	assertEquals(360, math.deg(2 * math.pi))
	
	for i = 1, 100, 0.1 do
		assertEquals(i, math.rad(math.deg(i)))
		assertEquals(i, math.deg(math.rad(i)))
	end
end

do
	local a, b
	a, b = math.frexp(0/0)
	assert(isNaN(a) and b == 0)
	a, b = math.frexp(1/0)
	assert(isPosInf(a) and b == 0)
	a, b = math.frexp(-1/0)
	assert(isNegInf(a) and b == 0)

	assert(isPosInf(math.ldexp(1/0, 1)))
	assert(isNegInf(math.ldexp(-1/0, 1)))
	assert(isNaN(math.ldexp(0/0, 1)))

	for i = -10, 10, 0.1 do
		assertEquals(i, math.ldexp(math.frexp(i)))
		assertEquals(i, math.ldexp(i, 0/0))
		assertEquals(i, math.ldexp(i, 1/0))
		assertEquals(i, math.ldexp(i, -1/0))
	end
end

do
	assert(isNaN(math.fmod(0/0, 0/0)))
	assert(isNaN(math.fmod(0/0, 2)))
	assert(isNaN(math.fmod(2, 0/0)))
	assert(isNaN(math.fmod(1/0, 0/0)))
	assert(isNaN(math.fmod(1/0, 1/0)))
	assert(isNaN(math.fmod(-1/0, 0/0)))
	assert(isNaN(math.fmod(-1/0, 1/0)))
	
	for i = 1, 10 do
		assert(i == math.fmod(i, 1/0))
		assert(i == math.fmod(i, -1/0))
	end
	
	for i = 0.1, 10, 0.5 do
		assertEquals(math.fmod(i, 0.5), 0.1)
	end
	for i = -10.1, 0, 0.5 do
		assertEquals(math.fmod(i, 0.5), -0.1)
	end
	for i = 0.1, 10, 0.5 do
		assertEquals(math.fmod(i, -0.5), 0.1)
	end
	for i = -10.1, 0, 0.5 do
		assertEquals(math.fmod(i, -0.5), -0.1)
	end
end

-- test exp
do
	local e = math.exp(1)
	local x = 1
	for i = 1, 10 do
		x = x * e
		assertEquals(math.exp(i), x)
	end
end

-- test log
do
	assert(isNaN(math.log(-1)))
	assert(isNegInf(math.log(0)))
	assert(isPosInf(math.log(1/0)))
	
	local x = 1
	local e = math.exp(1)
	for i = 1, 10 do
		x = x * e
		assertEquals(math.log(x), i)
		assertEquals(math.log(1 / x), -i)
		
		assertEquals(math.log10(x), math.log(x) / math.log(10))
	end
end

assertEquals(math.pow(1.234, 10.170355), 8.48608917)


