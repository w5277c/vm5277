@echo off
setlocal enabledelayedexpansion

call "%vm5277%\bin\_j8bjc.cmd"
set "STATUS=!errorlevel!"

if "!STATUS!" == "0" (
    set "JAVA_EXEC=java.exe"
) else if "!STATUS!" == "1" (
    set "JAVA_EXEC=%vm5277%\tools\jre\bin\java.exe"
) else (
    echo [ERROR] Java verification script failed. >&2
    exit /b 1
)

"!JAVA_EXEC!" -cp "%vm5277%\bin\libs\*" ru.vm5277.compiler.Main %*