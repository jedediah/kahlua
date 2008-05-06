local t = {
"", "What is the best programming language?", "Lua", "Basic", "Lisp", "Java", "Easy", "Programming",
"", "What is the worst programming language?", "Basic", "Lua", "Lisp", "Java", "Easy", "Programming",
}

local numQuestions = math.floor(#t / 8)
local score, total = 0, 0
while true do
	local qId = math.random(1, numQuestions) - 1
	local offset = qId * 8
	local correct = t[offset + 3]
	local topic = t[offset + 8]
	local question = t[offset + 2]
	local answers = {correct, t[offset+ 4], t[offset + 5], t[offset + 6]}
	for i = 1, 10 do
		local a, b = math.random(1, 4), math.random(1, 4)
		answers[a], answers[b] = answers[b], answers[a]
	end
	local response = query(topic .. ": ", question, answers[1], answers[2], answers[3], answers[4])
	total = total + 1
	local s = "Wrong! "
	if response == tostring(correct) then
		score = score + 1
		s = "Correct! "
	end
	response = query(s, "Current score: " .. score .. "/" .. total, "Next question", "Quit")
	if response == "Quit" then
		return
	end
end

