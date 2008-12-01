local function assertEquals(a, b)
	assert(a == b, "Expected " .. a .. " == " .. b)
end

local function tablesequal(t1, t2)
    for k1,v1 in pairs(t1) do
        if v1 ~= t2[k1] then
            return false
        end
    end
    return true
end

local tbl = "*t"
local str = "%c"

local christmas = {year = 2008, month = 12, day = 25,}
local newyearseve = {year = 2008, month = 12, day = 31,}
local oct25 = {year = 2008, month = 10, day = 25, hour = 2, min = 8, sec = 0, wday = 7, yday = 298,}
local nov1 = {year = 2008, month = 11, day = 1, hour = 2, min = 8, sec = 0, wday = 7, yday = 305,}

local halloween = 1225440000  -- seconds since the epoch for halloween 2008, 8am

assert(os.difftime(os.time(christmas), os.time(newyearseve)) < 0,1)
assert(os.difftime(os.time(newyearseve), os.time(christmas)) > 0,2)
assertEquals(os.difftime(os.time(christmas), os.time(christmas)),0)

assertEquals(os.difftime(os.time(newyearseve), os.time(christmas)),6*24*60*60)

local nowtime = os.time()
local nowdate = os.date(tbl, nowtime)

print(os.difftime(os.time(nowdate),nowtime))
assertEquals(os.time(os.date(tbl, nowtime)),nowtime)
assert(tablesequal(os.date(tbl, os.time(oct25)), oct25))

-- Locale dependant test, so comment it out
-- assertEquals(os.date(str,halloween), "Fri Oct 31 04:00:00 EDT 2008")
