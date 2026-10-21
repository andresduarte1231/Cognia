# Uso: .\scripts\stop-service.ps1 -Port 4002   (detiene el proceso que escucha en ese puerto)
param([Parameter(Mandatory)][int]$Port)
Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue | % { Stop-Process -Id $_.OwningProcess -Force; Write-Host "Detenido PID $($_.OwningProcess) en puerto $Port" }
