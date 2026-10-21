# Inicia Prometheus (127.0.0.1:9090) y Grafana (127.0.0.1:3000) sin Docker.
$root = Split-Path $PSScriptRoot -Parent
& "$root\observability\scripts\prepare.ps1"
$rt = "$root\observability\runtime"
$prom = Get-ChildItem "$root\observability\bin" -Recurse -Filter prometheus.exe | Select -First 1
$graf = Get-ChildItem "$root\observability\bin" -Recurse -Filter grafana-server.exe | Select -First 1
if(-not $prom -or -not $graf){ throw 'Faltan binarios en observability\bin (ver docs\observabilidad.md)' }
$vars=@{}; Get-Content "$root\.env" | ? { $_ -match '^\s*[^#=]+=' } | % { $k,$v = $_ -split '=',2; $vars[$k.Trim()]=$v.Trim() }

if(-not (Get-NetTCPConnection -LocalPort 9090 -State Listen -ErrorAction SilentlyContinue)){
  Start-Process $prom.FullName -WindowStyle Hidden -WorkingDirectory $prom.DirectoryName -ArgumentList @(
    "--config.file=$root\observability\prometheus\prometheus.yml",
    "--web.config.file=$rt\prometheus-web.yml",
    "--web.listen-address=127.0.0.1:9090",
    "--storage.tsdb.path=$root\observability\data\prometheus",
    "--storage.tsdb.retention.time=15d") -RedirectStandardError "$root\logs\prometheus.err.log" -RedirectStandardOutput "$root\logs\prometheus.out.log"
}
if(-not (Get-NetTCPConnection -LocalPort 3000 -State Listen -ErrorAction SilentlyContinue)){
  $env:GF_SERVER_HTTP_ADDR='127.0.0.1'; $env:GF_SERVER_HTTP_PORT='3000'
  $env:GF_SECURITY_ADMIN_USER='admin'; $env:GF_SECURITY_ADMIN_PASSWORD=$vars['GRAFANA_ADMIN_PASSWORD']
  $env:GF_USERS_ALLOW_SIGN_UP='false'; $env:GF_AUTH_ANONYMOUS_ENABLED='false'
  $env:GF_ANALYTICS_REPORTING_ENABLED='false'; $env:GF_ANALYTICS_CHECK_FOR_UPDATES='false'
  $env:GF_PATHS_DATA="$root\observability\data\grafana"; $env:GF_PATHS_LOGS="$root\logs"
  $env:GF_PATHS_PROVISIONING="$rt\grafana-provisioning"
  $env:GF_DASHBOARDS_DEFAULT_HOME_DASHBOARD_PATH="$root\observability\grafana\dashboards\cognia-01-estado-general.json"
  Start-Process $graf.FullName -WindowStyle Hidden -WorkingDirectory (Split-Path $graf.DirectoryName -Parent) -ArgumentList '--homepath', (Split-Path $graf.DirectoryName -Parent) -RedirectStandardError "$root\logs\grafana.err.log" -RedirectStandardOutput "$root\logs\grafana.out.log"
}
Start-Sleep 12
Write-Host 'Prometheus: http://127.0.0.1:9090   Grafana: http://127.0.0.1:3000 (usuario admin; clave GRAFANA_ADMIN_PASSWORD en .env)'