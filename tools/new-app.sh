#!/usr/bin/env bash
# Turns a fresh copy of this template into a new app. Run once, from the repo root, right after
# "Use this template" made the new repo:
#
#   bash tools/new-app.sh "שם האפליקציה" com.stodio.myapp MyApp
#
#   1. the name on the phone (Hebrew is fine)
#   2. the Android package name: lowercase letters, digits and dots, unique per app, never changed later
#   3. the APK file name in the "latest" release (Latin letters and digits, no spaces)
#
# It renames the Java package com.stodio.app to the new one (sources, tests, namespace, test runners) and sets
# app.id / app.apk in gradle.properties, app_name in strings.xml and rootProject.name. Then: commit, push, done.
set -euo pipefail
cd "$(dirname "$0")/.."

name=${1:?the app name, e.g. \"מונה צעדים\"}
pkg=${2:?the package name, e.g. com.stodio.steps}
apk=${3:?the APK file name, e.g. Steps}

[[ $pkg =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]] || { echo "bad package name: $pkg" >&2; exit 1; }
[[ $apk =~ ^[A-Za-z][A-Za-z0-9_-]*$ ]] || { echo "bad APK name: $apk" >&2; exit 1; }
old=com.stodio.app
[ -d app/src/main/java/com/stodio/app ] || { echo "already renamed (no com/stodio/app sources)" >&2; exit 1; }
[ "$pkg" != "$old" ] || { echo "pick a package name other than the template's" >&2; exit 1; }

newdir=${pkg//.//}
move() {  # $1 = source root holding com/stodio/app
  mkdir -p "$1/$newdir.tmp"
  git mv "$1/com/stodio/app"/* "$1/$newdir.tmp/" 2>/dev/null || mv "$1/com/stodio/app"/* "$1/$newdir.tmp/"
  rm -rf "$1/com/stodio/app"
  rmdir "$1/com/stodio" "$1/com" 2>/dev/null || true
  mkdir -p "$(dirname "$1/$newdir")"
  mv "$1/$newdir.tmp" "$1/$newdir"
}
move app/src/main/java
move core-tests/src

esc_old=${old//./\\.}
grep -rl "$old" app core-tests | while read -r f; do
  sed -i "s/$esc_old/$pkg/g" "$f"
done
sed -i "s#com/stodio/app#$newdir#g; s#com\\\\stodio\\\\app#${newdir//\//\\\\}#g" core-tests/run.sh core-tests/run.cmd
sed -i "s/^app\.id=.*/app.id=$pkg/; s/^app\.apk=.*/app.apk=$apk/" gradle.properties
sed -i "s/^rootProject\.name = .*/rootProject.name = \"$apk\"/" settings.gradle
python3 - "$name" <<'PY'
import sys, re, html
p = 'app/src/main/res/values/strings.xml'
s = open(p, encoding='utf-8').read()
s = re.sub(r'(<string name="app_name">).*?(</string>)', lambda m: m.group(1) + html.escape(sys.argv[1], quote=False) + m.group(2), s)
open(p, 'w', encoding='utf-8').write(s)
PY
echo "done: $name ($pkg), APK $apk.apk"
echo "now: bash core-tests/run.sh, then commit and push to main"
