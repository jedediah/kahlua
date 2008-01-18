#!/bin/bash
cd tests
make
cd ..
java -cp /usr/share/java/emma.jar emmarun -r html -cp bin -sp src se.krka.kahlua.test.Test tests/bin

