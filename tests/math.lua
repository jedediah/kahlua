assert(1 < 2, "1 must be less than 2")
assert(not (2 < 1), "2 must not be less than 1")

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

assertEquals(math.pow(1.234, 10.170355), 8.48608917)

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

