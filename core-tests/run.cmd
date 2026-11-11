@echo off
rem Checks of the core package (plain JDK 8+). Run from the repo root: core-tests\run.cmd
if exist build\core-tests rmdir /s /q build\core-tests
mkdir build\core-tests\classes
dir /s /b app\src\main\java\com\stodio\app\core\*.java core-tests\src\*.java > build\core-tests\sources.txt
javac -nowarn -Xlint:-options -source 8 -target 8 -encoding UTF-8 -d build\core-tests\classes @build\core-tests\sources.txt || exit /b 1
java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -cp build\core-tests\classes com.stodio.app.tests.CoreTests
