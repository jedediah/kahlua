#!/bin/bash
cd tests
make && java -cp /usr/share/java/emma.jar emmarun -r html -cp ../bin se.krka.kahlua.test.Test bin
cd ..

