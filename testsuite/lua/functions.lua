function a(x,y,z)
   return x, y, z
end

local x,y,z = a(5,7,9)
assert(x == 5)
assert(y == 7)
assert(z == 9)

x, y = a(6,8,10)
assert(x == 6)
assert(y == 8)
assert(z == 9)

local z2 = 0
x,y,z,z2 = a(10, 20, 30)
assert(x == 10)
assert(y == 20)
assert(z == 30)
assert(z2 == nil)
