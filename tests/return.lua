function foo()
   return 1,2,3
end

local a,b,c,d = foo()
assert(a == 1)
assert(b == 2)
assert(c == 3)
assert(d == nil)

