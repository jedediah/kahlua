local mt = {}
function mt.__index(t, k)
	if type(k) == "string" then
		return k .. k
	end
end

local t = setmetatable({}, mt)
for key, value in pairs{1} do
	assert(t.hello == "hellohello")
end

