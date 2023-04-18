@echo off
for /f "tokens=1" %%A in ('jps -lv ^| find "JavaBspLauncher"') do (taskkill /F /PID %%A)
for /f "tokens=1" %%A in ('jps -lv ^| find "Unknown"') do (taskkill /F /PID %%A)
for /f "tokens=1" %%A in ('jps -lv ^| find "GradleDaemon"') do (taskkill /F /PID %%A)
