#!/usr/bin/env bash
# Checks of the core package (plain JDK 8+, no Android). Run from the repo root: bash core-tests/run.sh
set -e
OUT=build/core-tests
rm -rf "$OUT" && mkdir -p "$OUT/classes"
javac -nowarn -Xlint:-options -source 8 -target 8 -encoding UTF-8 -d "$OUT/classes" \
  $(find app/src/main/java/com/stodio/app/core -name '*.java') \
  $(find core-tests/src -name '*.java')
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp "$OUT/classes" com.stodio.app.tests.CoreTests
