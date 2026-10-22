$ScriptPath = $MyInvocation.MyCommand.Path
if ([string]::IsNullOrEmpty($ScriptPath)) { $ScriptPath = $MyInvocation.ScriptName }
$CurrentDir = Split-Path -Parent $ScriptPath

$VmDir      = Split-Path -Parent $CurrentDir
$VmBin      = Join-Path $VmDir "bin"

if ([Environment]::GetEnvironmentVariable('vm5277', 'User') -ne $VmDir) {
    [Environment]::SetEnvironmentVariable('vm5277', $VmDir, 'User')
    Write-Host "[OK] Environment variable vm5277 has been set." -ForegroundColor Green
}

$UserPath = [Environment]::GetEnvironmentVariable('Path', 'User')
if (($UserPath -split ';') -notcontains $VmBin) {
    $NewPath = if ([string]::IsNullOrEmpty($UserPath)) { $VmBin } else { "$UserPath;$VmBin" }
    [Environment]::SetEnvironmentVariable('Path', $NewPath, 'User')
    Write-Host "[OK] Path $VmBin has been permanently added to Path." -ForegroundColor Green
} else {
    Write-Host "[INFO] Path $VmBin is already in Path." -ForegroundColor Cyan
}
