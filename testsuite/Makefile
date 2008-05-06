all:	test

compile:	$(patsubst lua/%.lua,bin/%.lbc,$(wildcard lua/*.lua))

bin/Test.class:	src/Test.java
	javac -cp ../bin src/Test.java -d bin

bin/%.lbc:	lua/%.lua
	luac -o $@ $<
test:	mkdir compile bin/Test.class

mkdir:
	mkdir -p bin

