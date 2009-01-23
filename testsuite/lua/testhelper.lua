local tests = {}
function runtest(name, f)
	local oldSuite = suite
	suite = suite or name
	local status, msg, stacktrace = pcall(f)
	tests[suite] = tests[suite] or {}
	
	local testcase = {name = name, status = status}
	if not status then
		testcase.error = msg
		testcase.stacktrace = stacktrace
	end
	table.insert(tests[suite], testcase)
	
	suite = oldSuite
end

local template, template_suite, template_test

function string:gsubplain(pattern, repl, n)
	return self:gsub(pattern, string.gsub(tostring(repl), "%%", "%%%%"), n)
end

function generatereport()
	local suitesTotal, suitesSuccess = 0, 0
	local testsTotal, testsSuccess = 0, 0
	local suitesOutput = ""
	
	local suiteOutput = ""
	
	for suiteName, suite in pairs(tests) do
		local suiteTotal, suiteSuccess = 0, 0
		
		local testSuccessOutput = ""
		local testFailOutput = ""
		for i, test in ipairs(suite) do
			local text = template_test:
				gsubplain("@@NAME@@", test.name or "<unnamed>"):
				gsubplain("@@ERROR@@", test.error or ""):
				gsubplain("@@STACKTRACE@@", test.stacktrace or "")

			if test.status then
				testSuccessOutput = testSuccessOutput .. text
			else
				testFailOutput = testFailOutput .. text
			end
			
			suiteTotal = suiteTotal + 1
			suiteSuccess = suiteSuccess + (test.status and 1 or 0)
		end

					
		suiteOutput = suiteOutput .. template_suite:
			gsubplain("@@NAME@@", suiteName or "<unnamed>"):
			gsubplain("@@TOTAL@@", suiteTotal):
			gsubplain("@@SUCCESS@@", suiteSuccess):
			gsubplain("@@TESTS_OK@@", testSuccessOutput):
			gsubplain("@@TESTS_FAIL@@", testFailOutput)

		if suiteSuccess == suiteTotal then
			suitesSuccess = suitesSuccess + 1
		end
		suitesTotal = suitesTotal + 1
		testsTotal = testsTotal + suiteTotal
		testsSuccess = testsSuccess + suiteSuccess
	end
	
	local report = template:
		gsubplain("@@SUITE_TOTAL@@", suitesTotal):
		gsubplain("@@SUITE_SUCCESS@@", suitesSuccess):
		gsubplain("@@SUITE_FAIL@@", suitesTotal - suitesSuccess):
		gsubplain("@@TESTS_TOTAL@@", testsTotal):
		gsubplain("@@TESTS_SUCCESS@@", testsSuccess):
		gsubplain("@@TESTS_FAIL@@", testsTotal - testsSuccess):
		gsubplain("@@SUITES@@", suiteOutput)
	return report, suitesTotal, suitesSuccess, testsTotal, testsSuccess
end

template = [[
<html>
	<body>
		<h1>Test results</h1>
		<table border="1">
			<thead>
				<tr>
					<th>&nbsp;</th><th>Number total</th><th>Number of successful</th><th>Number of failures</th>
				</tr>
			</thead>
			<tbody>
				<tr>
					<th>Suites:</th><td>@@SUITE_TOTAL@@</td><td>@@SUITE_SUCCESS@@</td><td>@@SUITE_FAIL@@</td>
				</tr>
				<tr>
					<th>Tests:</th><td>@@TESTS_TOTAL@@</td><td>@@TESTS_SUCCESS@@</td><td>@@TESTS_FAIL@@</td>
				</tr>
			</tbody>
		</table>
		<h1>Suites</h1>
		@@SUITES@@
	</body>
</html>
]]

template_suite = [[
		<h2>Details</h1>
		<p>Name: @@NAME@@</p>
		<p>Number of tests: @@TOTAL@@</p>
		<p>Number of successful tests: @@SUCCESS@@</p>

		<h4>Failed tests</h4>
		<table border="1">
			<thead>
				<tr><th>Name</th><th>Error message</th><th>Stack trace</th></tr>
			</thead>
			<tbody>
				@@TESTS_FAIL@@
			</tbody>
		</table border="1">
		
		<h4>Successful tests</h4>
		<table border="1">
			<thead>
				<tr><th>Name</th><th>&nbsp;</th><th>&nbsp;</th></tr>
			</thead>
			<tbody>
				@@TESTS_OK@@
			</tbody>
		</table>
]]

template_test = [[
	<tr><td>@@NAME@@</td><td><pre>@@ERROR@@</pre></td><td><pre>@@STACKTRACE@@</pre></td></tr>
]]

--[[
test("ok!", function() assert(true) end)
test("fail", function() assert(false, "something is wrong") end)
suite = "my suite"
test("fail", function() assert(false, "something is wrong again") end)
print(generatereport())
--]]

