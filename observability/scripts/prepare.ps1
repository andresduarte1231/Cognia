# Genera (si no existen) los secretos y archivos de ejecucion de Prometheus y Grafana en observability\runtime.
# Nada de lo generado se versiona. Requiere python con el paquete bcrypt para el hash de Prometheus.
$root = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$rt = "$root\observability\runtime"
New-Item -ItemType Directory -Force $rt, "$rt\grafana-provisioning\datasources", "$rt\grafana-provisioning\dashboards", "$root\observability\data\prometheus", "$root\observability\data\grafana" | Out-Null
$u = New-Object Text.UTF8Encoding $false

$envp = "$root\.env"
$vars = @{}
Get-Content $envp | ? { $_ -match '^\s*[^#=]+=' } | % { $k,$v = $_ -split '=',2; $vars[$k.Trim()] = $v.Trim() }
function Nuevo-Secreto { -join ((48..57)+(65..90)+(97..122) | Get-Random -Count 24 | % { [char]$_ }) }
foreach($k in 'GRAFANA_ADMIN_PASSWORD') {
  if(-not $vars[$k]) { $vars[$k] = Nuevo-Secreto; Add-Content $envp "`n$k=$($vars[$k])" }
}
if(-not $vars['METRICS_PASSWORD']) { throw 'METRICS_PASSWORD no esta definido en .env' }

# Prometheus: autenticacion basica en su interfaz y API (mismo usuario/clave de metricas) y archivo de clave para el scrape.
[IO.File]::WriteAllText("$rt\metrics.pass", $vars['METRICS_PASSWORD'], $u)
$hash = $vars['METRICS_PASSWORD'] | python -c "import bcrypt,sys;print(bcrypt.hashpw(sys.stdin.read().strip().encode(),bcrypt.gensalt(10)).decode())"
[IO.File]::WriteAllText("$rt\prometheus-web.yml", "basic_auth_users:`n  $($vars['METRICS_USER']): '$hash'`n", $u)

# Grafana: datasource con credenciales y proveedor de paneles.
[IO.File]::WriteAllText("$rt\grafana-provisioning\datasources\prometheus.yml", @"
apiVersion: 1
datasources:
  - name: Prometheus
    uid: cognia-prometheus
    type: prometheus
    access: proxy
    url: http://127.0.0.1:9090
    isDefault: true
    basicAuth: true
    basicAuthUser: $($vars['METRICS_USER'])
    secureJsonData:
      basicAuthPassword: $($vars['METRICS_PASSWORD'])
"@, $u)
$dash = ("$root\observability\grafana\dashboards") -replace '\\','/'
[IO.File]::WriteAllText("$rt\grafana-provisioning\dashboards\cognia.yml", @"
apiVersion: 1
providers:
  - name: COGNIA
    folder: COGNIA
    type: file
    disableDeletion: false
    allowUiUpdates: false
    options:
      path: $dash
"@, $u)
Write-Host 'Archivos de observabilidad preparados en observability\runtime'