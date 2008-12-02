local function assertEquals(a, b)
	assert(a == b, "Expected " .. a .. " == " .. b)
end

local tbl = "*t"
local str = "%c"

local christmas = {year = 2008, month = 12, day = 25,}
local newyearseve = {year = 2008, month = 12, day = 31,}
local oct25 = {year = 2008, month = 10, day = 25, hour = 2, min = 8, sec = 0, wday = 7, yday = 298,}
local nov1 = {year = 2008, month = 11, day = 1, hour = 2, min = 8, sec = 0, wday = 7, yday = 306,}

local halloween = 1225440000  -- seconds since the epoch for halloween 2008, 8am in UTC

do
	local res = os.date(tbl, halloween)
	assertEquals(res.year, 2008)
	assertEquals(res.month, 10)
	assertEquals(res.day, 31)
	assertEquals(res.hour, 8)
	assertEquals(res.min, 0)
	assertEquals(res.sec, 0)
	assertEquals(res.wday, 6)
	assertEquals(res.yday, 304)
	
	assertEquals(res.year, tonumber(os.date("%Y", halloween)))
	assertEquals(res.month, tonumber(os.date("%m", halloween)))
	assertEquals(res.day, tonumber(os.date("%d", halloween)))
	assertEquals(res.yday, tonumber(os.date("%j", halloween)))
	assertEquals(res.hour, tonumber(os.date("%H", halloween)))
	assertEquals(res.min, tonumber(os.date("%M", halloween)))
	assertEquals(res.sec, tonumber(os.date("%S", halloween)))
end

assert(os.difftime(os.time(christmas), os.time(newyearseve)) < 0,1)
assert(os.difftime(os.time(newyearseve), os.time(christmas)) > 0,2)
assertEquals(os.difftime(os.time(christmas), os.time(christmas)),0)

assertEquals(os.difftime(os.time(newyearseve), os.time(christmas)),6*24*60*60)

local nowtime = os.time()

assertEquals(os.time(os.date(tbl, nowtime)),nowtime)

do
	local res = os.date(tbl, os.time(oct25))
	assertEquals(res.year, oct25.year)
	assertEquals(res.month, oct25.month)
	assertEquals(res.day, oct25.day)
	assertEquals(res.hour, oct25.hour)
	assertEquals(res.min, oct25.min)
	assertEquals(res.sec, oct25.sec)
	assertEquals(res.wday, oct25.wday)
	assertEquals(res.yday, oct25.yday)
end

-- Locale dependant test, so comment it out
-- assertEquals(os.date(str,halloween), "Fri Oct 31 04:00:00 EDT 2008")
