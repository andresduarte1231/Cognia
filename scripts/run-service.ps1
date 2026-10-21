# Uso: .\scripts\run-service.ps1 -Name catalog-inventory-service [-Port 4002]
param([Parameter(Mandatory)][string]$Name,[int]$Port=0)
$root = Split-Path $PSScriptRoot -Parent
Get-Content "$root\.env" | ? { $_ -match '^\s*[^#=]+=' } | % { $k,$v = $_ -split '=',2; [Environment]::SetEnvironmentVariable($k.Trim(),$v.Trim(),'Process') }
if($Port){ $env:SERVER_PORT = "$Port" }
$jar = Get-ChildItem "$root\backend\$Name\target\$Name-*.jar" | ? { $_.Name -notmatch 'original' } | Select -First 1
New-Item -ItemType Directory -Force "$root\logs" | Out-Null
Set-Location "$root\backend\$Name"
& java -jar $jar.FullName *>> "$root\logs\$Name.log"
