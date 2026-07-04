@echo off
REM Compile every source file into .\out (JDK 11+)
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
del sources.txt
echo Compilation finished. Classes are in .\out
