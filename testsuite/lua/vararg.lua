function id(...)
	return ...
end

local a, b, c, d = id(1, 2, 3)
assert(a == 1, "a == 1")
assert(b == 2, "b == 2")
assert(c == 3, "c == 3")
assert(d == nil, "d == nil")

assert(select("#", id(1,2,3)) == 3)
assert(select(2, id(1,20,3)) == 20)
assert(select("2", id(1,20,3)) == 20)

function test(a,b,c, ...)
   assert(select("#", ...) == 3, "vararg size incorrect")
end
test(1,2,3,4,5,6)


function rec2(a, ...)
   if a then
      return 1 + rec2(...)
   end
   return 0
end

function rec(a, ...)
   if a then
      return a * rec(...)
   end
   return 1
end

assert(rec(10,2,3) == 60)


function rec(a, ...)
   if a then
      return a * rec(...)
   end
   return 1
end

assert(rec(10,2,3) == 60)

function tailrec(acc, a, ...)
   if a then
      return tailrec(acc * a, ...)
   end
   return acc
end

local v = tailrec(1, 10, 2, 3)
assert(v == 60, "v == 60")


do
	local function foo(...)
		local a, b, c, d = ...
		return a, b, c, d
	end
	local a, b, c, d = foo(1,2,3,4,5,6)
	assert(a == 1 and b == 2 and c == 3 and d == 4)
	a, b, c, d = foo(1, 2)
	assert(a == 1 and b == 2 and c == nil and d == nil)
end

