t1 = {}
t2 = {}
setmetatable(t1, {__index = t2})
setmetatable(t2, {__index = function() return "the value" end})
assert(t1.key == t2.key)
assert(t1.key == "the value")
