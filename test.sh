#!/bin/bash
cd tests
make && java -cp ../bin se.krka.kahlua.test.Test bin
cd ..
