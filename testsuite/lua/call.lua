t1 = {}
t2 = {}
setmetatable(t1, {__call = t2})
setmetatable(t2, {__call = function() return "hello world" end})
testAssert(pcall(t1) == false)
testAssert(t2() == "hello world")

