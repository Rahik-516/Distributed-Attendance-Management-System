@echo off
REM Start one faculty client. Usage: run_client.bat [host] ["Window Label"]
set HOST=%1
if "%HOST%"=="" set HOST=localhost
java -cp out client.AttendanceClient %HOST% %2
