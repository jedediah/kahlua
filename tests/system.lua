assert(collectgarbage("count") > 0)
assert(select("#", collectgarbage("step")) == 1)
assert(type(collectgarbage("step")) == "boolean")
assert(not pcall(collectgarbage, "invalid string"))
assert(not pcall(collectgarbage, {}))
assert(not pcall(collectgarbage, true))
assert(not pcall(collectgarbage, false))
assert(not pcall(collectgarbage, 7))

if pcall(collectgarbage, "total") then
	assert(collectgarbage("total") > 0)
end
if pcall(collectgarbage, "free") then
	assert(collectgarbage("free") > 0)
end

