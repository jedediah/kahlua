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
   local assert = assert
   local type = type
   assert(type(a) == type(b))
   if (type(a) == "number") then
      local f = math.abs
      local v = a - b;
      assert(v)
      local v2 = math.abs(v)
      assert(v2)
      assert(v2 < 1e-6)
   else
      assert(a == b)
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

   local s = tostring(c % c)
   assert(s == "NaN", "0 % 0 was: " .. s)

end

-- test math functions
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
	end
end

assertEquals(math.pow(1.234, 10.170355), 8.48608917)


