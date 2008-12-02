function assertEqual(actual, expected, msg)
	assert(expected == actual, msg or "expected " .. tostring(expected) .. ", actual " .. tostring(actual)) 
end

-- test concatenation
do
	local s1, s2 = "hello", "world"
	local s = s1 .. s2
	assertEqual(s,"helloworld")
end

do
	local s1, s2, s3, s4 = "this", "is", "a", "test"
	local s = s1 .. s2 .. s3 .. s4
	assertEqual(s,"thisisatest")
end

do
	local meta = {__concat = function(a, b)
		if type(a) == "table" then
			a = a[1]
		end
		if type(b) == "table" then
			b = b[1]
		end
		return a .. b
	end}
	local t1 = setmetatable({"hello"}, meta)
	local t2 = setmetatable({" "}, meta)
	local t3 = setmetatable({"world"}, meta)
	local s = t1 .. t2 .. t3
	assertEqual(s,"hello world")
end

do
	local t1 = {"hello"}
	local t2 = {" "}
	local t3 = {"world"}
	local s
	local ok, errmsg = pcall(function() s = t1 .. t2 .. t3 end)
	assert(not ok)
end

do
	assertEqual(type(string),"table")
	assertEqual(type(string.sub),"function")
	assertEqual(type(string.byte),"function")
	assertEqual(type(string.len),"function")
	assertEqual(type(string.char),"function")
	assertEqual(type(string.lower),"function")
	assertEqual(type(string.upper),"function")
	assertEqual(type(string.reverse),"function")

	-- testing handling of not enough parameters
	assert(not pcall(string.sub))
	assert(not pcall(string.byte))
	assert(not pcall(string.len))
	assert(not pcall(string.lower))
	assert(not pcall(string.upper))
	assert(not pcall(string.reverse))

end


do
	local a,b,c,d,e,f,g = string.byte("Hello world", 1, 5)
	assert(a == string.byte("H"))
	assert(b == string.byte("e"))
	assert(c == string.byte("l"))
	assert(d == string.byte("l"))
	assert(e == string.byte("o"))
	assert(f == nil)
	assert(g == nil)
end

do
	local function testReverse(s)
		assertEqual(#(s:reverse()),#s)
		assertEqual(s:reverse():reverse(),s)
	end 
	testReverse"hello world"
	testReverse""
	testReverse"a"
	testReverse"ÅÄÖ"
	testReverse"adolf i paris rapar sirap i floda"
end

s = "Hello world"

assertEqual(#s,11)
assertEqual(s:sub(1, 5),"Hello")
assertEqual(s:sub(1, -1),"Hello world")
assertEqual(s:sub(1, -5),"Hello w")
assertEqual(s:sub(1, -7),"Hello")
assertEqual(s:sub(1, 0),"")
assertEqual(s:sub(1, -25),"")

assertEqual(s.sub,string.sub)

assert("ape" < "banana")

assert("xyz" <= "xyz")

assertEqual(s:byte(1),72)

assertEqual(string.char(65),"A")

assertEqual(s:lower(),"hello world")
assertEqual(s:upper(),"HELLO WORLD")

assertEqual(s:match("Hello"),"Hello")
assertEqual(s:match("H.l%D%w"),"Hello")

assertEqual(s:match("H[e][lo][lo][lo]"),"Hello")
assertEqual(s:find("[Hello][Hello][Hello]"),1)
assertEqual(s:find("[hello][hello][hello]"),2)

assertEqual(s:find("worl"),7)
assertEqual(s:find("worlld"),nil)

assertEqual(s:find("%w%w%w%w%w"),1)
assertEqual(s:find("%w%w%w%w%w",5),7)
assertEqual(s:find("%w%w%w%w%w%w"),nil)
assertEqual(s:find("%w%w%w%w%w",8),nil)

do
	local s2 = "abcdabcd"
	assertEqual(s2:find("bc"),2)
	assertEqual(s2:match("bc"),"bc")
	assertEqual(s2:match("%wc"),"bc")
	assertEqual(s2:find("cd",10),nil)
	assertEqual(s2:find("cd",-4),7)
	assertEqual(s2:find("cd",-8),3)
	
	assertEqual(s2:find("bcd$"),6)
	assertEqual(s2:find("abc$"),nil)
	assertEqual(s2:find("^abcdabcd$"),1)
	assertEqual(s2:find("^abcd$"),nil)
	
	local s3 = "123$^xy"
	assertEqual(s3:find("3$^x"),3)
end	

do
	local s = "12345abcdef"
	local si, ei, cap1 = s:find("(45ab)")
	assertEqual(si,4)
	assertEqual(ei,7)
	assertEqual(cap1,"45ab")
	assertEqual(s:match("(45ab)"),"45ab")
	
	si, ei, cap1 = s:find("cd()")
	assertEqual(si,8)
	assertEqual(ei,9)
	assertEqual(cap1,10)
	assertEqual(s:match("cd()"),10)
	
	local cap2, cap3, cap4 = nil, nil, nil
	si, ei, cap1, cap2 = s:find("(23)%d%d%a(bc)")
	assertEqual(si,2)
	assertEqual(ei,8)
	assertEqual(cap1,"23")
	assertEqual(cap2,"bc")
	cap1, cap2 = s:match("(23)%d%d%a(bc)")
	assertEqual(cap1,"23")
	assertEqual(cap2,"bc")
	
	si,ei,cap1,cap2 = s:find("%d(%d%d(%a%a)%a)%a")
	assertEqual(si,3)
	assertEqual(ei,9)
	assertEqual(cap1,"45abc")
	assertEqual(cap2,"ab")
	cap1,cap2 = s:match("%d(%d%d(%a%a)%a)%a")
	assertEqual(cap1,"45abc")
	assertEqual(cap2,"ab")
	
	si,ei,cap1,cap2,cap3,cap4 = s:find("%d(%d%a(%a)())%a(%x%a)")
	assertEqual(si,4)
	assertEqual(ei,10)
	assertEqual(cap1,"5ab")
	assertEqual(cap2,"b")
	assertEqual(cap3,8)
	assertEqual(cap4,"de")
	cap1,cap2,cap3,cap4 = s:match("%d(%d%a(%a)())%a(%x%a)")
	assertEqual(cap1,"5ab")
	assertEqual(cap2,"b")
	assertEqual(cap3,8)
	assertEqual(cap4,"de")
	
	assertEqual(s:find("%d(%u%l)"),nil)
	assertEqual(s:match("%d(%u%l)"),nil)
end

do
	local s = "wxyzabcd1111;.,"
    local si,ei = s:find("cd1*")
    assertEqual(si,7)
    assertEqual(ei,12)
    
	local si,ei = s:find("bce?d11")
	assertEqual(si,6)
	assertEqual(ei,10)
	
	si, ei = s:find("1-")
	assertEqual(si,1)
	assertEqual(ei,0)
	
    si, ei = s:find("1-1")
    assertEqual(si,9)
    assertEqual(ei,9)
    
    si, ei = s:find("1*1")
    assertEqual(si,9)
    assertEqual(ei,12)
    
    si, ei = s:find("1+1")
    assertEqual(si,9)
    assertEqual(ei,12)
end

local b = "6 - (x + (y^2 - 3z) / 7xy)"
assertEqual(b:find("%b()"),5)
assertEqual(b:find("%b)("),nil)

do
	local s2 = "hello world from Lua"
	local t2 = {}
    for w in string.gmatch(s2, "%w+") do
        --table.insert(t2,w) -- table.insert doesnt work atm
        t2[#t2+1] = w
    end
    
    assertEqual(#t2,4)
    assertEqual(t2[1],"hello")
    assertEqual(t2[2],"world")
    assertEqual(t2[3],"from")
    assertEqual(t2[4],"Lua")
	
	local t = {}
    local s = "from=world, to=Lua"
    for k, v in string.gmatch(s, "(%w+)=(%w+)") do
        t[k] = v
    end
	assertEqual(t.from, "world")
	assertEqual(t.to, "Lua")
end

function concattest(...)
	local t = {test = "world"}
	local tmp = ...
	local s = "hello" .. t.test
	assertEqual(s,"helloworld")
end
concattest()

function concattest2(...)
	local function t() return "world" end
	local tmp = ...
	local s = "hello" .. t()
	assertEqual(s,"helloworld")
end
concattest2()

function concattest3(...)
	local t = setmetatable({}, {__index = function() return "world" end})
	local tmp = ...
	local s = "hello" .. t.test
	assertEqual(s,"helloworld")
end
concattest3()

function concattest4(...)
	local t = setmetatable({}, {__index = function() return "world" end})
	local tmp = ...
	local s = tmp .. t.test
	assertEqual(s,"helloworld")
end
concattest4("hello")