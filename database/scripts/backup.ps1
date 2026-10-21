param([string]$Mysqldump='C:\xampp\mysql\bin\mysqldump.exe')
$root = Split-Path $PSScriptRoot -Parent | Split-Path -Parent
$f = "$root\database\backup_$(Get-Date -Format yyyyMMdd_HHmmss).sql"
& $Mysqldump -uroot --routines cognia_aqui_es_tona --result-file=$f; Write-Host "Backup: $f"
