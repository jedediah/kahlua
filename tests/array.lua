a = array.new()
assert(a)
assert(#a == 0)

assert(getmetatable(a) == "restricted")
assert(array.push)

assert(a.push)

assert(type(a.push) == "function")
a:push(40)
a:push(50)
a:push(60)
a:push(70)

assert(#a == 4)

assert(a[2] == 60)
assert(a[3] == 70)
a[3] = 90

assert(a[3] == 90)

