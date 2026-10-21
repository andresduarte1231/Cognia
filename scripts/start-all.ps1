# Inicia MySQL (XAMPP) si hace falta y los servicios Spring Boot. Uso: .\scripts\start-all.ps1
$root = Split-Path $PSScriptRoot -Parent
if(-not (Get-NetTCPConnection -State Listen -LocalPort 3306 -ErrorAction SilentlyContinue)){
  Start-Process 'C:\xampp\mysql\bin\mysqld.exe' -ArgumentList '--defaults-file=C:\xampp\mysql\bin\my.ini','--standalone' -WindowStyle Hidden; Start-Sleep 10 }
$svcs = [ordered]@{ 'auth-service'=4001; 'catalog-inventory-service'=4002; 'orders-sales-service'=4003; 'api-gateway'=4000 }
foreach($n in $svcs.Keys){
  if(Get-NetTCPConnection -State Listen -LocalPort $svcs[$n] -ErrorAction SilentlyContinue){ Write-Host "$n ya escucha en $($svcs[$n])"; continue }
  Start-Process powershell -WindowStyle Hidden -ArgumentList '-NoProfile','-File',"$root\scripts\run-service.ps1",'-Name',$n
  Write-Host "Iniciando $n (puerto $($svcs[$n]))"
}
foreach($p in $svcs.Values){ for($i=0;$i -lt 40;$i++){ if(Get-NetTCPConnection -State Listen -LocalPort $p -ErrorAction SilentlyContinue){break}; Start-Sleep 2 }; Write-Host "puerto $p :" ([bool](Get-NetTCPConnection -State Listen -LocalPort $p -ErrorAction SilentlyContinue)) }
