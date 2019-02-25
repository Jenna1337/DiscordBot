@echo off
for /F "tokens=*" %%L in ('""%ProgramFiles%\AutoHotkey\AutoHotkey.exe" "%1" %*"') do echo %%L
