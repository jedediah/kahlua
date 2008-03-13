all:	test

compile:	$(patsubst %.lua,bin/%.lbc,$(wildcard *.lua))

bin/%.lbc:	%.lua
	luac -o $@ $<

test:	mkdir compile

mkdir:
	mkdir -p bin

