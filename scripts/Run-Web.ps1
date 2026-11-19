[CmdletBinding()]
param([ValidateRange(1, 65535)][int]$Port = 8080)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
$previousPort = $env:PORT
try {
    $maven = 'mvn'
    if (Test-Path -LiteralPath 'config/local-tools.json') {
        $configured = (Get-Content -Raw 'config/local-tools.json' | ConvertFrom-Json).maven
        if ($configured -and (Test-Path -LiteralPath $configured)) { $maven = $configured }
    }
    $env:PORT = "$Port"
    & $maven spring-boot:run
    exit $LASTEXITCODE
} finally {
    $env:PORT = $previousPort
    Pop-Location
}
