-- Contenido editable de la landing (INSERT IGNORE: no pisa valores ya existentes). Sin datos de contacto inventados.
INSERT IGNORE INTO configuracion_tienda (clave, valor) VALUES
('mision','Facilitar el acceso a alimentos y productos de consumo de calidad con un servicio cercano y confiable. (Editable por el propietario)'),
('vision','Ser la tienda virtual de referencia de nuestra comunidad. (Editable por el propietario)'),
('valores','Honestidad,Calidad,Servicio,Cercanía'),
('costo_domicilio','5000'),
('contacto_telefono',''),
('contacto_correo',''),
('contacto_direccion',''),
('info_domicilios','Los domicilios se coordinan con la dirección indicada al hacer el pedido.'),
('info_recogida','Puedes recoger tu pedido en la tienda cuando esté listo.'),
('promociones_texto','Productos con precio rebajado se marcan automáticamente como promoción.');
