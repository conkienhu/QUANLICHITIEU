#!/bin/sh
# Biên dịch và chạy: ./build.sh
set -e
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out com.expense.Main
