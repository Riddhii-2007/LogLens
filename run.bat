@echo off
echo ========================================================
echo                  LogLens Application
echo ========================================================
echo.

echo [1/3] Generating sources.txt...
dir /s /b src\*.java > sources.txt

if not exist bin (
    echo Creating bin directory...
    mkdir bin
)

echo [2/3] Compiling source code...
javac -d bin -cp "lib/sqlite-jdbc-3.36.0.3.jar" @sources.txt

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Compilation failed.
    pause
    exit /b %ERRORLEVEL%
)

echo [3/3] Launching LogLens Dashboard...
java -cp "bin;lib/sqlite-jdbc-3.36.0.3.jar" loglens.ui.AppLauncher
