@echo off
setlocal

cd .\src\

REM Compile the Java player code
javac -cp game_engine.jar --release 8 Agent.java

REM If compilation failed, stop the script
if errorlevel 1 (
    echo Compilation failed. Exiting...
    exit /b 1
)

if "%~1"=="" (
    echo Please provide a seed number as an argument.
    exit /b
)

set "seed=%~1"

REM Run the game engine with Agent
java -jar game_engine.jar 60 game.mario.MarioGame %seed% 1000 Agent

echo Seed: %seed%
