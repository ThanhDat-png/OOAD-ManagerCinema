$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot
$config=Get-Content -LiteralPath (Join-Path $repo 'config/local-tools.json') -Raw|ConvertFrom-Json
Push-Location $repo
try {
    & $config.maven -B -ntp compile exec:java
    if($LASTEXITCODE -ne 0){throw 'Không thể chạy ứng dụng'}
} finally {Pop-Location}
