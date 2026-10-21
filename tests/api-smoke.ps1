# Prueba de integracion de API (flujo comercial y seguridad). Uso: .\tests\api-smoke.ps1 [-Gateway http://127.0.0.1:4000] [-Direct]
param([string]$Gateway='http://127.0.0.1:4000',[switch]$Direct,[string]$OwnerEmail='propietario@cognia.local',[string]$OwnerPass='Cognia2026Owner')
$A=$Gateway;$C=$Gateway;$O=$Gateway
if($Direct){$A='http://127.0.0.1:4001';$C='http://127.0.0.1:4002';$O='http://127.0.0.1:4003'}
$script:res=@()
function T($id,$desc,[scriptblock]$b){ try{ $r=& $b; if($r -eq $true){$script:res+=[pscustomobject]@{Id=$id;Prueba=$desc;Estado='PASA'}}else{$script:res+=[pscustomobject]@{Id=$id;Prueba=$desc;Estado="FALLA ($r)"}} }catch{ $script:res+=[pscustomobject]@{Id=$id;Prueba=$desc;Estado="FALLA ($($_.Exception.Message))"} } }
function Call($m,$u,$body,$tok,$extra=@{}){ $h=@{}+$extra; if($tok){$h.Authorization="Bearer $tok"}; $p=@{Method=$m;Uri=$u;Headers=$h;ContentType='application/json';UseBasicParsing=$true;ErrorAction='Stop'}; if($body){$p.Body=($body|ConvertTo-Json -Depth 6 -Compress)}
  try{ $r=Invoke-WebRequest @p; [pscustomobject]@{Code=[int]$r.StatusCode;Json=$(try{$r.Content|ConvertFrom-Json}catch{$null})} }catch{ $c=0;$j=$null; if($_.Exception.Response){$c=[int]$_.Exception.Response.StatusCode; try{ $sr=New-Object IO.StreamReader($_.Exception.Response.GetResponseStream()); $j=$sr.ReadToEnd()|ConvertFrom-Json }catch{} }; [pscustomobject]@{Code=$c;Json=$j} } }
$sfx=Get-Random -Maximum 99999
$own=(Call POST "$A/api/auth/login" @{correo=$OwnerEmail;password=$OwnerPass}).Json.token
$ca="cli.a.$sfx@example.com";$cb="cli.b.$sfx@example.com"
T 'T01' 'Registro cliente ignora rol enviado (siempre CLIENTE)' { $r=Call POST "$A/api/auth/registro" @{nombre='Cliente A';correo=$ca;password='Prueba12345';rol='PROPIETARIO'}; ($r.Code -eq 201 -and $r.Json.rol -eq 'CLIENTE') -or $r.Code }
$null=Call POST "$A/api/auth/registro" @{nombre='Cliente B';correo=$cb;password='Prueba12345'}
$ta=(Call POST "$A/api/auth/login" @{correo=$ca;password='Prueba12345'}).Json.token
$tb=(Call POST "$A/api/auth/login" @{correo=$cb;password='Prueba12345'}).Json.token
T 'T02' 'Login con credenciales invalidas devuelve 401' { (Call POST "$A/api/auth/login" @{correo=$ca;password='Incorrecta1'}).Code -eq 401 }
T 'T03' 'Contrasena debil rechazada (400)' { (Call POST "$A/api/auth/registro" @{nombre='X';correo="x$sfx@example.com";password='abc'}).Code -eq 400 }
T 'T04' 'Catalogo publico accesible sin token' { $r=Call GET "$C/api/productos?tamano=3"; $r.Code -eq 200 -and $r.Json.contenido.Count -gt 0 }
T 'T05' 'Categorias publicas' { (Call GET "$C/api/categorias").Json.Count -ge 1 }
T 'T06' 'Producto inactivo no visible publicamente (404)' { (Call GET "$C/api/productos/14").Code -eq 404 }
T 'T07' 'Dashboard admin sin token -> 401' { (Call GET "$C/api/admin/inventario/resumen").Code -eq 401 }
T 'T08' 'Cliente no accede al dashboard -> 403' { (Call GET "$C/api/admin/inventario/resumen" $null $ta).Code -eq 403 }
T 'T09' 'Propietario accede al dashboard' { (Call GET "$C/api/admin/inventario/resumen" $null $own).Code -eq 200 }
T 'T10' 'Cliente no accede a usuarios admin -> 403' { (Call GET "$A/api/admin/usuarios" $null $ta).Code -eq 403 }
T 'T11' 'Rutas internas no expuestas por el gateway / sin rol SERVICIO' { $c=(Call POST "$C/internal/inventario/reservas" @{pedidoId=1;items=@(@{productoId=1;cantidad=1})} $ta).Code; $c -in 403,404 }
$p1=(Call GET "$C/api/productos/1").Json; $stock0=$p1.existencias
$k1="idem-$sfx-0001"
$pedido=$null
T 'T12' 'Crear pedido (precio calculado en backend, ignora precio enviado)' { $r=Call POST "$O/api/pedidos" @{items=@(@{productoId=1;cantidad=2;precio=1});metodoEntrega='RECOGIDA'} $ta @{'Idempotency-Key'=$k1}; $script:pedido=$r.Json; $r.Code -eq 201 -and $r.Json.subtotal -eq ($p1.precio*2) -and $r.Json.estado -eq 'PENDIENTE' -and $r.Json.pago.estado -eq 'PENDIENTE' -or "code=$($r.Code)" }
T 'T13' 'Idempotencia: misma clave no duplica pedido' { $r=Call POST "$O/api/pedidos" @{items=@(@{productoId=1;cantidad=2});metodoEntrega='RECOGIDA'} $ta @{'Idempotency-Key'=$k1}; $r.Json.id -eq $script:pedido.id }
T 'T14' 'Inventario descontado por la reserva' { (Call GET "$C/api/productos/1").Json.existencias -eq ($stock0-2) }
T 'T15' 'Cliente B no puede ver pedido de A (404)' { (Call GET "$O/api/pedidos/$($script:pedido.id)" $null $tb).Code -eq 404 }
T 'T16' 'Cliente B no puede pagar pedido de A' { (Call POST "$O/api/pedidos/$($script:pedido.id)/pago" @{resultado='APROBADO'} $tb).Code -eq 404 }
T 'T17' 'Pago aprobado confirma el pedido' { $r=Call POST "$O/api/pedidos/$($script:pedido.id)/pago" @{resultado='APROBADO'} $ta; $r.Json.estado -eq 'CONFIRMADO' -and $r.Json.pago.estado -eq 'APROBADO' }
T 'T18' 'Movimiento SALIDA registrado al confirmar y sin doble descuento' { $m=(Call GET "$C/api/admin/inventario/movimientos?productoId=1&tamano=10" $null $own).Json.contenido; ($m | ? {$_.pedidoId -eq $script:pedido.id -and $_.tipo -eq 'SALIDA'}) -and (Call GET "$C/api/productos/1").Json.existencias -eq ($stock0-2) }
$k2="idem-$sfx-0002"
T 'T19' 'Pago rechazado cancela pedido y libera inventario' { $s=(Call GET "$C/api/productos/2").Json.existencias; $r=Call POST "$O/api/pedidos" @{items=@(@{productoId=2;cantidad=3});metodoEntrega='DOMICILIO';direccionEntrega='Calle 1 # 2-3';ciudad='Ciudad'} $ta @{'Idempotency-Key'=$k2}; $d=(Call GET "$C/api/productos/2").Json.existencias; $r2=Call POST "$O/api/pedidos/$($r.Json.id)/pago" @{resultado='RECHAZADO'} $ta; $e=(Call GET "$C/api/productos/2").Json.existencias; ($d -eq $s-3) -and $r2.Json.estado -eq 'CANCELADO' -and $e -eq $s -and $r.Json.costoEntrega -gt 0 }
T 'T20' 'Cantidad mayor a existencias -> 409 y sin cambios' { $s=(Call GET "$C/api/productos/3").Json.existencias; $r=Call POST "$O/api/pedidos" @{items=@(@{productoId=3;cantidad=100});metodoEntrega='RECOGIDA'} $ta @{'Idempotency-Key'="idem-$sfx-0003"}; $r.Code -eq 409 -and (Call GET "$C/api/productos/3").Json.existencias -eq $s -or "code=$($r.Code)" }
T 'T21' 'Pedido sin Idempotency-Key -> 422' { (Call POST "$O/api/pedidos" @{items=@(@{productoId=1;cantidad=1});metodoEntrega='RECOGIDA'} $ta).Code -eq 422 }
T 'T22' 'Domicilio sin direccion -> 422' { (Call POST "$O/api/pedidos" @{items=@(@{productoId=1;cantidad=1});metodoEntrega='DOMICILIO'} $ta @{'Idempotency-Key'="idem-$sfx-0004"}).Code -eq 422 }
T 'T23' 'Historial del cliente solo contiene sus pedidos' { $l=(Call GET "$O/api/pedidos" $null $ta).Json.contenido; $l.Count -ge 2 -and -not ($l | ? {$_.clienteId -ne $script:pedido.clienteId}) }
T 'T24' 'Personal actualiza estado: CONFIRMADO->EN_PREPARACION->LISTO_PARA_RECOGIDA' { $a=Call PATCH "$O/api/admin/pedidos/$($script:pedido.id)/estado" @{estado='EN_PREPARACION'} $own; $b=Call PATCH "$O/api/admin/pedidos/$($script:pedido.id)/estado" @{estado='LISTO_PARA_RECOGIDA'} $own; $a.Code -eq 200 -and $b.Json.estado -eq 'LISTO_PARA_RECOGIDA' }
T 'T25' 'Transicion invalida rechazada (409)' { (Call PATCH "$O/api/admin/pedidos/$($script:pedido.id)/estado" @{estado='EN_PREPARACION'} $own).Code -eq 409 }
T 'T26' 'Cliente no puede cambiar estado de pedido (403)' { (Call PATCH "$O/api/admin/pedidos/$($script:pedido.id)/estado" @{estado='ENTREGADO'} $ta).Code -eq 403 }
T 'T27' 'Reporte de ventas: solo pagos aprobados' { $r=(Call GET "$O/api/admin/reportes/resumen" $null $own).Json; $r.cantidadVentas -ge 1 -and $r.ventasConfirmadas -gt 0 }
T 'T28' 'Reporte CSV disponible' { (Call GET "$O/api/admin/reportes/ventas.csv" $null $own).Code -eq 200 }
T 'T29' 'Propietario crea empleado; administrador no puede crear usuarios' { $e=Call POST "$A/api/admin/usuarios" @{nombre='Empleado';correo="emp$sfx@example.com";rol='EMPLEADO_VENTAS';password='Empleado123'} $own; $adm=Call POST "$A/api/admin/usuarios" @{nombre='Admin';correo="adm$sfx@example.com";rol='ADMINISTRADOR';password='Admin12345'} $own; $ta2=(Call POST "$A/api/auth/login" @{correo="adm$sfx@example.com";password='Admin12345'}).Json.token; $x=Call POST "$A/api/admin/usuarios" @{nombre='Z';correo="z$sfx@example.com";rol='CLIENTE';password='Cliente12345'} $ta2; $e.Code -eq 201 -and $x.Code -eq 403 }
T 'T30' 'No se puede asignar rol PROPIETARIO por API' { (Call POST "$A/api/admin/usuarios" @{nombre='P';correo="p$sfx@example.com";rol='PROPIETARIO';password='Propiet12345'} $own).Code -eq 403 }
T 'T31' 'Empleado de ventas: lee pero no edita productos' { $te=(Call POST "$A/api/auth/login" @{correo="emp$sfx@example.com";password='Empleado123'}).Json.token; (Call GET "$C/api/admin/productos" $null $te).Code -eq 200 -and (Call PATCH "$C/api/admin/productos/1/estado" @{estado='ACTIVO'} $te).Code -eq 403 }
T 'T32' 'Token manipulado rechazado (401)' { (Call GET "$C/api/admin/inventario/resumen" $null ($own.Substring(0,$own.Length-4)+'AAAA')).Code -eq 401 }
T 'T33' 'Validacion: precio negativo -> 400' { (Call PATCH "$C/api/admin/productos/1/precio" @{precio=-5} $own).Code -eq 400 }
T 'T34' 'Auditoria administrativa accesible al propietario' { (Call GET "$A/api/admin/auditoria?tamano=5" $null $own).Json.contenido.Count -gt 0 }
$script:res | Format-Table -AutoSize
$f=@($script:res | ? { $_.Estado -ne 'PASA' }).Count
Write-Host ""; Write-Host "Resultado: $($script:res.Count - $f)/$($script:res.Count) pruebas pasan"
exit $f
