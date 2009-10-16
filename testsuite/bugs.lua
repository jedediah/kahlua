local f = function(a, b, c)
    return nil, a, b, c
end

assert(f(1, 2, 3) == nil) --ok
assert(f(1, 2, 3, 4) == nil) -- Fail: returns 4

