$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot
$toolsPath=Join-Path $repo 'config/local-tools.json'
if(Test-Path -LiteralPath $toolsPath) {
    $config=Get-Content -LiteralPath $toolsPath -Raw|ConvertFrom-Json
    $mavenCommand=$config.maven
    if([string]::IsNullOrWhiteSpace($mavenCommand)){throw 'Thiếu đường dẫn maven trong config/local-tools.json'}
} else {
    $mavenTool=Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if(-not $mavenTool){$mavenTool=Get-Command mvn -ErrorAction SilentlyContinue}
    if(-not $mavenTool){throw 'Chưa tìm thấy Maven. Cài Maven vào PATH hoặc khai báo config/local-tools.json; xem docs/CAI_DAT.md.'}
    $mavenCommand=$mavenTool.Source
}
Push-Location $repo
try {
    & $mavenCommand -B -ntp compile exec:java
    if($LASTEXITCODE -ne 0){throw 'Không thể chạy ứng dụng; xem docs/CAI_DAT.md'}
} finally {Pop-Location}
