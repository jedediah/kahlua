do
    local properties = {}
    setmetatable(properties, {__mode = "k"})

    function withproperties(obj)
        local old = getmetatable(obj)
        local function index(t, k)
            local p = properties[t]
            if p then
                return p[k]
            end
            if old then
                local oldindex = old.__index
                if oldindex then
                    if type(oldindex) == "function" then
                        return oldindex(t, k)
                    end
                    return oldindex[k]
                end
            end
        end
        local function newindex(t, k, v)
            local p = properties[t]
            if not p then
                p = {}
                properties[t] = p
            end
            p[k] = v
        end
        setmetatable(obj, {__index = index, __newindex = newindex})
        return obj
    end
end

local x = newobject();
local y = newobject();

testAssert(getmetatable(x) == nil)
testAssert(getmetatable(y) == nil)

-- test properties
local foo = withproperties(newobject())
testAssert(foo.foo == nil)
foo.foo = "hello"
testAssert(foo.foo == "hello")

-- test recursive properties
local bar = newobject()
setmetatable(bar, {__index = foo})
bar = withproperties(bar)
testAssert(bar.bar == nil)
testAssert(bar.foo == "hello")
bar.foo = "goodbye"
testAssert(bar.foo == "goodbye")
testAssert(foo.foo == "hello")

-- test one metatable per object
local mt = {}
local x2 = setmetatable(x, mt)
testAssert(x == x2)
testAssert(getmetatable(x) == mt)
testAssert(getmetatable(y) == nil)
