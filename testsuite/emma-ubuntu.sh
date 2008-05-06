#!/bin/bash
make && java -cp /usr/share/java/emma.jar emmarun -r html -cp ../bin:bin -sp ../src Test bin

