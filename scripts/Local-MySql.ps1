param([ValidateSet('Start','Stop','Status')][string]$Action='Status')
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot
$config=Get-Content -LiteralPath (Join-Path $repo 'config/local-tools.json') -Raw|ConvertFrom-Json
$admin=Join-Path $config.mysqlBin 'mysqladmin.exe'
$server=Join-Path $config.mysqlBin 'mysqld.exe'
$clientArg="--defaults-extra-file=$($config.mysqlAdminConfig)"
function Test-Ready {
    $ErrorActionPreference='Continue'
    & $admin $clientArg ping 2>$null|Out-Null
    return ($LASTEXITCODE -eq 0)
}
if($Action -eq 'Stop') {
    & $admin $clientArg shutdown
    if($LASTEXITCODE -ne 0){throw 'Không thể dừng MySQL của đồ án'}
    return
}
if(Test-Ready){Write-Output 'MySQL 127.0.0.1:3307 ready'; return}
if($Action -eq 'Status'){throw 'MySQL của đồ án chưa chạy'}
Start-Process -FilePath $server -ArgumentList @("--defaults-file=`"$($config.mysqlDefaults)`"") -WindowStyle Hidden
for($attempt=0;$attempt -lt 30;$attempt++) {
    Start-Sleep -Milliseconds 300
    if(Test-Ready){Write-Output 'MySQL 127.0.0.1:3307 ready'; return}
}
throw 'MySQL chưa sẵn sàng. Kiểm tra server.log trong thư mục runtime.'
