#!/bin/bash
rm -rf kahlua.jar
cd bin
jar cf ../kahlua.jar ./se/krka/kahlua
jar cf ../kahluax-compiler.jar ./se/krka/kahluax/compiler
jar cf ../kahluax-annotation.jar ./se/krka/kahluax/annotation
cd ..

