@echo off
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0init.ps1"

for %%I in ("%~dp0..") do set "VM_DIR=%%~fI"
set "vm5277=%VM_DIR%"
set "PATH=%VM_DIR%\bin;%PATH%"

echo [OK] VM5277 Toolkit initialized successfully.