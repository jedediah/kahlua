all:	test

compile:	$(patsubst %.lua,bin/%.lbc,$(wildcard *.lua))

bin/%.lbc:	%.lua
	luac -o $@ $<

test:	compile

