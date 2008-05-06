assert("a" ~= nil)
assert("a" ~= 1)

metaA = {}
function metaA.__eq(a,b)
	return a == b
end

metaB = {}
function metaB.__eq(a,b)
	return true
end

ta = setmetatable({}, metaA)
tb = setmetatable({}, metaB)
tc = setmetatable({}, metaB)

assert(ta ~= tb)
assert(tb == tc)

