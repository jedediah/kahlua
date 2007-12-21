
function foo()
	error("test error")
end

function bar()
	foo()
end

bar()
