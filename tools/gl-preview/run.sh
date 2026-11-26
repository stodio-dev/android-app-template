#!/usr/bin/env bash
# Renders a level with the app's real shaders (Shaders.java) and mesh (core/Mesh.java) in headless Chromium's
# WebGL 1, which compiles GLSL ES 1.00 like the phone does. Catches shader compile/link errors, wrong winding and
# arrows pointing the wrong way without a device. The face texture is a rough stand-in for Atlas.java.
#
#   bash tools/gl-preview/run.sh <level> [skinIndex] [backgroundIndex] [blocksToRemoveFirst]
#
# Prints "OK <n> vertices" or the compiler's error; the picture is build/gl-preview/level-<level>.png.
set -euo pipefail
cd "$(dirname "$0")/../.."
level=${1:?level number}; skin=${2:-0}; bg=${3:-0}; taps=${4:-0}
out=build/gl-preview
mkdir -p "$out/classes"
javac -nowarn -encoding UTF-8 -d "$out/classes" \
  $(find app/src/main/java -path '*/core/*.java') \
  $(find app/src/main/java -name Shaders.java) tools/gl-preview/Preview.java
java -cp "$out/classes" com.stodio.arrowcubes.Preview "$level" "$skin" "$bg" "$taps" > "$out/level-$level.json"
node tools/gl-preview/shot.js "$PWD/$out/level-$level.json" "$PWD/$out/level-$level.png" || true
echo "picture: $out/level-$level.png"
