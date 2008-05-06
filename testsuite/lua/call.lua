t1 = {}
t2 = {}
setmetatable(t1, {__call = t2})
setmetatable(t2, {__call = function() return "hello world" end})
assert(pcall(t1) == false)
assert(t2() == "hello world")

