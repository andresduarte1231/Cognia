param([string]$Mysql='C:\xampp\mysql\bin\mysql.exe')
# Se usa "source" para que el cliente lea el archivo directamente en UTF-8 (una tuberia de PowerShell corrompe los tildes).
$root = Split-Path $PSScriptRoot -Parent
$cs = '--default-character-set=utf8mb4'
foreach($dir in 'migrations','seed'){
  Get-ChildItem "$root\$dir\*.sql" | Sort-Object Name | % {
    Write-Host "Aplicando $dir\$($_.Name)"
    $ruta = $_.FullName -replace '\\','/'
    & $Mysql -uroot $cs cognia_aqui_es_tona -e "source $ruta"
  }
}