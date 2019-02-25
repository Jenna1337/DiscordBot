@echo off
for /F "tokens=*" %%L in ('""%ProgramFiles%\AutoHotkey\AutoHotkey.exe" "MakeBlacklistProviderTable.ahk" %*"') do echo %%L
