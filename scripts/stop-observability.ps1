foreach($p in 9090,3000){
  Get-NetTCPConnection -LocalPort $p -State Listen -ErrorAction SilentlyContinue | % { Stop-Process -Id $_.OwningProcess -Force; Write-Host "Detenido puerto $p" }
}