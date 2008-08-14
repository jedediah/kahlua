#!/bin/bash
rm -rf bin
mkdir -p bin
find src | grep "\.java$" | grep -v "kahluax" | xargs javac -d bin -target 1.4 -source 1.4
