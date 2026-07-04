#!/bin/bash
# Compile every source file into ./out (JDK 11+)
mkdir -p out
javac -d out $(find src -name '*.java')
echo "Compilation finished. Classes are in ./out"
