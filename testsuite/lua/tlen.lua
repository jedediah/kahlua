local t = {}
assert(#t == 0)
for i = 1, 100 do
	t[i] = i
	assert(#t == i)
end

