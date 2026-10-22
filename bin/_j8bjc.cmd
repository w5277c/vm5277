@echo off
setlocal enabledelayedexpansion

set "LOCAL_JRE_DIR=%vm5277%\tools\jre"
set "JRE_TMP_FILE=%vm5277%\tools\jre.zip"

if exist "%LOCAL_JRE_DIR%\bin\java.exe" (
    exit /b 1
)

where java >nul 2>&1
if %errorlevel% equ 0 (
    exit /b 0
) else (
    set "OS=windows"
    
    if "%PROCESSOR_ARCHITECTURE%"=="AMD64" (
        set "ARCH=x64"
    ) else if "%PROCESSOR_ARCHITECTURE%"=="ARM64" (
        set "ARCH=aarch64"
    ) else (
        set "ARCH=x86"
    )

    set "BASE_URL=https://vm5277.ru/tools"
    set "JRE_URL=!BASE_URL!/jre11_!OS!-!ARCH!.zip"
    
    echo Downloading JRE for '!OS!-!ARCH! ...'
    
    if not exist "%LOCAL_JRE_DIR%" mkdir "%LOCAL_JRE_DIR%"

    powershell -NoProfile -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; $ProgressPreference = 'SilentlyContinue'; Invoke-WebRequest -Uri $env:JRE_URL -OutFile '%JRE_TMP_FILE%' -UserAgent 'vm5277 win script/0.1'" >nul 2>&1
    if !errorlevel! neq 0 (
        call :error_exit "Failed to download the file. Please check your internet connection and link availability."
        exit /b 2
    )
    
    for %%I in ("%JRE_TMP_FILE%") do set "FILE_SIZE=%%~zI"
    if not exist "%JRE_TMP_FILE%" (
        call :error_exit "The downloaded file is empty or missing."
        exit /b 2
    ) else if !FILE_SIZE! equ 0 (
        call :error_exit "The downloaded file is empty or missing."
        exit /b 2
    )
    
    echo Extracting JRE...
    powershell -NoProfile -Command "$shell = New-Object -ComObject Shell.Application; $zip = $shell.NameSpace('%JRE_TMP_FILE%'); $dest = $shell.NameSpace('%LOCAL_JRE_DIR%'); $dest.CopyHere($zip.Items(), 20)" >nul 2>&1
    if !errorlevel! neq 0 (
        call :error_exit "Error extracting the archive. The file might be corrupted."
        exit /b 2
    )

    set "INNER_DIR="
    for /d %%i in ("%LOCAL_JRE_DIR%\*") do (
        if exist "%%i\release" set "INNER_DIR=%%i"
    )

    if defined INNER_DIR (
        xcopy "!INNER_DIR!\" "%LOCAL_JRE_DIR%\" /E /H /Y /Q >nul 2>&1
	rmdir /s /q "!INNER_DIR!" >nul 2>&1
    ) else (
        call :error_exit "Invalid JRE archive structure: 'release' file not found."
        exit /b 3
    )

    del "%JRE_TMP_FILE%" 2>nul
    
    if exist "%LOCAL_JRE_DIR%\bin\java.exe" (
        echo Portable JRE has been successfully deployed.
        echo.
        exit /b 1
    ) else (
        call :error_exit "The archive was extracted, but the java executable was not found."
        exit /b 2
    )
)

:error_exit
echo. >&2
echo [ERROR] %~1 >&2
if exist "%JRE_TMP_FILE%" del "%JRE_TMP_FILE%" 2>nul
exit /b 2
