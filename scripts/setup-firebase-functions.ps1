param(
    [string]$ProjectId = "smartcampusassist"
)

$ErrorActionPreference = "Stop"

Write-Host "Using Firebase project: $ProjectId"

Push-Location "$PSScriptRoot\..\functions"
try {
    npm install
} finally {
    Pop-Location
}

$secrets = @(
    "SMTP_HOST",
    "SMTP_PORT",
    "SMTP_USER",
    "SMTP_PASS",
    "SMTP_FROM",
    "SMTP_SECURE"
)

foreach ($secret in $secrets) {
    $value = Read-Host "Enter value for $secret"
    if ([string]::IsNullOrWhiteSpace($value)) {
        throw "Secret $secret cannot be empty."
    }

    $tempFile = [System.IO.Path]::GetTempFileName()
    try {
        Set-Content -Path $tempFile -Value $value -NoNewline
        npx firebase-tools functions:secrets:set $secret --project $ProjectId --data-file $tempFile
    } finally {
        Remove-Item $tempFile -ErrorAction SilentlyContinue
    }
}

npx firebase-tools deploy --project $ProjectId --only functions,firestore:rules
