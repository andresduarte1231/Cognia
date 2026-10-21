# Prueba de disponibilidad y recuperacion: detiene un servicio, espera la alerta en Prometheus y lo restaura.
param([int]$Port = 4002, [string]$Job = 'catalog-inventory-service')
$root = Split-Path $PSScriptRoot -Parent
$v=@{}; Get-Content "$root\.env" | ? { $_ -match '^\s*[^#=]+=' } | % { $k,$x = $_ -split '=',2; $v[$k.Trim()]=$x.Trim() }
$h=@{Authorization='Basic '+[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("$($v.METRICS_USER):$($v.METRICS_PASSWORD)"))}
function T($m){ Write-Host ("[{0}] {1}" -f (Get-Date -Format 'HH:mm:ss'), $m) }
function Up { (Invoke-RestMethod "http://127.0.0.1:9090/api/v1/query?query=up%7Bjob%3D%22$Job%22%7D" -Headers $h).data.result[0].value[1] }
function Alertas { (Invoke-RestMethod 'http://127.0.0.1:9090/api/v1/alerts' -Headers $h).data.alerts | % { "$($_.labels.alertname)/$($_.labels.job)=$($_.state)" } }
function Gw { try { (Invoke-WebRequest "http://127.0.0.1:4000/api/productos" -UseBasicParsing -TimeoutSec 10).StatusCode } catch { $_.Exception.Response.StatusCode.value__ } }
T "Estado inicial: up=$(Up) ; GET /api/productos via gateway = $(Gw)"
T "Deteniendo $Job (puerto $Port)"
& "$root\scripts\stop-service.ps1" -Port $Port | Out-Null
$t0 = Get-Date
T "Gateway con servicio caido: GET /api/productos = $(Gw)  (esperado 502/503/504, no 200)"
T "Gateway sigue sirviendo la SPA: GET / = $((Invoke-WebRequest http://127.0.0.1:4000/ -UseBasicParsing).StatusCode)"
$firing = $false
for($i=0; $i -lt 18 -and -not $firing; $i++){
  Start-Sleep 5
  $a = @(Alertas)
  if($a -match 'ServicioNoDisponible.*firing'){ $firing = $true }
}
T "up=$(Up) ; alertas: $($a -join ', ') ; tiempo hasta firing: $([int]((Get-Date)-$t0).TotalSeconds)s ; firing=$firing"
T "Recuperando servicio"
$t1 = Get-Date
Start-Process powershell -WindowStyle Hidden -ArgumentList '-NoProfile','-File',"$root\scripts\run-service.ps1",'-Name',$Job
$ok=$false
for($i=0; $i -lt 40 -and -not $ok; $i++){ Start-Sleep 5; if((Up) -eq '1'){ $ok=$true } }
T "up=$(Up) tras $([int]((Get-Date)-$t1).TotalSeconds)s ; GET /api/productos = $(Gw)"
Start-Sleep 20
T "Alertas tras recuperacion: $((@(Alertas) -join ', '))  (vacio = resuelta)"