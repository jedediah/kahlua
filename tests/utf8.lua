utf8str = "Hellö wörld";
assert(utf8str:sub(4, 4) == "l")
assert(utf8str:sub(5, 5) == "ö")
assert(utf8str:sub(6, 6) == " ")
assert(utf8str:sub(7, 7) == "w")
assert(utf8str:sub(8, 8) == "ö")

