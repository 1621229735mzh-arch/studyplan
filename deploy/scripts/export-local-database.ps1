param(
    [Parameter(Mandatory = $true)][ValidatePattern('^[A-Za-z0-9_]+$')][string]$Database,
    [string]$DbUser = 'root',
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306
)
# 在本机交互式 PowerShell 中执行；密码由 mysqldump 提示输入，不写入命令或脚本。
$ErrorActionPreference = 'Stop'
$projectRoot = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$destination = Join-Path $projectRoot 'deploy/backups'
New-Item -ItemType Directory -Path $destination -Force | Out-Null
$backup = Join-Path $destination "$Database-$(Get-Date -Format 'yyyyMMdd-HHmmss').sql"
if (Test-Path $backup) { throw 'Backup already exists' }
Write-Host 'Pause editing in the LOCAL application until backup and cutover finish.'
& mysqldump --host=$DbHost --port=$Port --user=$DbUser --password --single-transaction --no-tablespaces --set-gtid-purged=OFF --routines --triggers --default-character-set=utf8mb4 --result-file=$backup $Database
if ($LASTEXITCODE -ne 0) { throw "Export failed. Do NOT import partial file: $backup" }
if ((Get-Item $backup).Length -eq 0) { throw 'Empty backup' }
Write-Host 'Export succeeded. Keep this private file separate from the program bundle.'
Get-Item $backup | Select-Object FullName, Length
Get-FileHash $backup -Algorithm SHA256
