@echo off

cd .\src\

REM Run the game engine with Agent
java -jar game_engine.jar 45 game.mario.MarioGame 1234567890 10000 game.mario.players.HumanPlayer
