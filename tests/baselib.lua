local ok, ret = pcall(function() return t.a.b end)
assert(ok)

ok, ret = pcall(error)
assert(ok)

ok, ret = pcall(error"")
assert(not ok)

local ok, msg, stacktrace = pcall(function() assert(false, "errmsg") end)
assert(not ok)
assert(msg == "errmsg")
assert(type(stacktrace) == "string")

assert(select(2, 4,5,6) == 5)
assert(select("#") == 0)
assert(select("#",7,8,9,10) == 4)
assert(select("#", select(2, 4,5,6,7,8)) == 4)

local t = {10,20,30,40}
assert(select("#", unpack(t)) == 4)
assert(select("#", unpack(t, 1, #t)) == 4)
assert(select("#", unpack(t, 1, 3)) == 3)
assert(select("#", unpack(t, 1, 10)) == 10)
assert(select("#", unpack(t, -10, 10)) == 21)

