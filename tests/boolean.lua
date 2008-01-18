local t = true
local f = false

assert(t == t)
assert(f == f)
assert(not not t == t)
assert(not not f == f)
assert(t ~= f)

assert(t and t)
assert(not(t and f))
assert(not(f and t))

assert(f or t)
assert(t or f)

local v0 = 1
local v1 = 1
local v2 = 2

assert(v0 <= v1)

assert(v1 < v2)
assert(v1 <= v2)

