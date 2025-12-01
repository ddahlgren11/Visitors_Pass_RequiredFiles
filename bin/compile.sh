#!/bin/bash
rm -rf out
mkdir -p out
find src -name "*.java" > sources.txt
javac -d out -cp lib/junit.jar:src @sources.txt
javac -d out -cp out:lib/junit.jar $(find test -name "*.java")
