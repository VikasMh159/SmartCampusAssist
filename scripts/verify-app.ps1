$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
Set-Location $repoRoot

Write-Host "Running clean debug build..."
& .\gradlew.bat clean :app:assembleDebug

if ($LASTEXITCODE -ne 0) {
    throw "Debug build failed."
}

Write-Host "Running lint..."
& .\gradlew.bat :app:lintDebug

if ($LASTEXITCODE -ne 0) {
    throw "Lint failed."
}

Write-Host "Verification complete."
