local x = {value = 6}
setmetatable(x, x)
x.__add = function(a, b)
    if (type(a) == 'number') then return a + b.value end
    if (type(b) == 'number') then return a.value + b end
    return a.value + b.value

end

assert(x + x == 12)
assert(5 + x == 11)
assert(x + 5 == 11)


