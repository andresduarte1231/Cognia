# Detiene los servicios Spring Boot (no MySQL).
foreach($p in 4000,4001,4002,4003){ & "$PSScriptRoot\stop-service.ps1" -Port $p }
