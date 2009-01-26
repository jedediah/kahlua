local t = {}
testAssert(#t == 0)
testCall(function()
	for i = 1, 100 do
		t[i] = i
		assert(#t == i)
	end
end)

setmetatable(t, {__len = function() return 123 end})
testAssert(#t == 100)

