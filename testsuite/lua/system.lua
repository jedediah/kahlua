collectgarbage("count")
collectgarbage("step")

assert(collectgarbage("count") > 0)
assert(not pcall(collectgarbage, "invalid string"))
assert(not pcall(collectgarbage, {}))
assert(not pcall(collectgarbage, true))
assert(not pcall(collectgarbage, false))
assert(not pcall(collectgarbage, 7))

