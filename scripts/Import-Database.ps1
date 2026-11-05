param(
    [Parameter(Mandatory=$true)][string]$MySqlExe,
    [Parameter(Mandatory=$true)][string]$ClientConfig,
    [Parameter(Mandatory=$true)][ValidatePattern('^[A-Za-z_][A-Za-z0-9_]{0,63}$')][string]$DatabaseName
)
$ErrorActionPreference='Stop'
$repo=Split-Path $PSScriptRoot
$clientPath=(Resolve-Path -LiteralPath $MySqlExe).Path
$configPath=(Resolve-Path -LiteralPath $ClientConfig).Path
if(-not (Test-Path -LiteralPath $clientPath -PathType Leaf) -or -not (Test-Path -LiteralPath $configPath -PathType Leaf)) {throw 'Thiếu mysql.exe hoặc file cấu hình quản trị'}
function Invoke-MySql([string]$Sql) {
    $start=New-Object System.Diagnostics.ProcessStartInfo
    $start.FileName=$clientPath
    $start.Arguments='"--defaults-extra-file='+$configPath+'" --default-character-set=utf8mb4 --batch --skip-column-names'
    $start.UseShellExecute=$false
    $start.CreateNoWindow=$true
    $start.RedirectStandardInput=$true
    $start.RedirectStandardOutput=$true
    $start.RedirectStandardError=$true
    $start.StandardOutputEncoding=New-Object System.Text.UTF8Encoding($false)
    $start.StandardErrorEncoding=New-Object System.Text.UTF8Encoding($false)
    $process=New-Object System.Diagnostics.Process
    $process.StartInfo=$start
    try {
        if(-not $process.Start()){throw 'Không chạy được MySQL client'}
        $outputTask=$process.StandardOutput.ReadToEndAsync()
        $errorTask=$process.StandardError.ReadToEndAsync()
        $bytes=[System.Text.Encoding]::UTF8.GetBytes($Sql+[Environment]::NewLine)
        $process.StandardInput.BaseStream.Write($bytes,0,$bytes.Length)
        $process.StandardInput.Close()
        $process.WaitForExit()
        $outputText=$outputTask.GetAwaiter().GetResult()
        $errorText=$errorTask.GetAwaiter().GetResult()
        if($process.ExitCode -ne 0){throw ('Nhập SQL thất bại: '+$errorText.Trim())}
        return $outputText.Trim()
    } finally {$process.Dispose()}
}
$scripts=@('001_tai_khoan.sql','002_demo_uc01.sql','003_giao_dich.sql','004_phim.sql','005_demo_the_loai.sql','006_lich_chieu.sql','007_demo_lich_chieu.sql','008_bao_toan_thong_tin_ghe.sql','009_demo_danh_muc_phong_ghe.sql','010_san_pham.sql')
# Resolve and read every source before creating anything. Do not depend on the calling directory.
$source=New-Object System.Text.StringBuilder
$quote=[char]96
[void]$source.AppendLine('CREATE DATABASE '+$quote+$DatabaseName+$quote+' CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;')
[void]$source.AppendLine('USE '+$quote+$DatabaseName+$quote+';')
foreach($script in $scripts) {
    $sqlPath=Join-Path (Join-Path $repo 'Database') $script
    if(-not (Test-Path -LiteralPath $sqlPath -PathType Leaf)){throw ('Thiếu SQL: '+$script)}
    [void]$source.AppendLine([System.IO.File]::ReadAllText($sqlPath,[System.Text.Encoding]::UTF8))
}
$version=Invoke-MySql 'SELECT VERSION();'
if($version -match 'MariaDB' -or $version -notmatch '^([0-9]+)\.' -or [int]$Matches[1] -lt 8){throw 'Đồ án cần MySQL 8 trở lên; chưa kiểm tra tương thích MariaDB'}
$exists=Invoke-MySql ("SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='"+$DatabaseName+"';")
if($exists -ne '0'){throw 'Database đã tồn tại. Không nhập lại hoặc ghi đè; hãy chọn tên database mới.'}
# CREATE without IF NOT EXISTS also rejects a database created concurrently after the check.
$null=Invoke-MySql $source.ToString()
Write-Output ('Đã tạo database '+$DatabaseName+' và nhập đủ SQL 001–010. Cấu hình tài khoản ứng dụng theo docs/CAI_DAT.md.')
