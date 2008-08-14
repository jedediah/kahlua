#!/bin/bash
find src | grep "\.java$" | xargs javac -d bin -target 1.5 -source 1.5

