
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

LOCK TABLES `categorias` WRITE;
/*!40000 ALTER TABLE `categorias` DISABLE KEYS */;
INSERT INTO `categorias` (`id`, `nombre`, `descripcion`, `estado`, `creado_en`) VALUES (1,'Despensa','Productos básicos de despensa','ACTIVA','2026-10-20 02:37:14'),(2,'Bebidas','Bebidas y refrescos','ACTIVA','2026-10-20 02:37:14'),(3,'Snacks','Pasabocas y productos para picar','ACTIVA','2026-10-20 02:37:14'),(4,'Lácteos y refrigerados','Productos refrigerados','ACTIVA','2026-10-20 02:37:14'),(5,'Aseo del hogar','Productos de limpieza','ACTIVA','2026-10-20 02:37:14'),(6,'Cuidado personal','Productos de cuidado personal','ACTIVA','2026-10-20 02:37:14');
/*!40000 ALTER TABLE `categorias` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `productos` WRITE;
/*!40000 ALTER TABLE `productos` DISABLE KEYS */;
INSERT INTO `productos` (`id`, `categoria_id`, `nombre`, `descripcion`, `precio`, `precio_anterior`, `existencias`, `inventario_minimo`, `imagen_url`, `estado`, `creado_en`, `actualizado_en`) VALUES (1,1,'Arroz blanco 1kg','Arroz de grano largo, ideal para el almuerzo diario.',4200.00,NULL,70,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-21 02:14:54'),(2,1,'Aceite vegetal 1L','Aceite vegetal para cocinar y freír.',12900.00,NULL,45,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-21 03:18:32'),(3,1,'Lentejas 500g','Lentejas seleccionadas, listas para cocinar.',3800.00,NULL,60,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-20 04:34:13'),(4,2,'Agua cristal 600ml','Agua natural embotellada.',1800.00,NULL,150,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-21 03:18:32'),(5,2,'Jugo de naranja 1L','Jugo de naranja sin conservantes.',6500.00,NULL,40,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-20 04:34:13'),(6,3,'Papas fritas 150g','Papas fritas con sal.',4500.00,NULL,70,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-20 04:34:13'),(7,3,'Galletas de avena','Paquete de galletas de avena y miel.',3200.00,NULL,55,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-20 04:34:13'),(8,4,'Leche entera 1L','Leche entera pasteurizada.',4600.00,NULL,90,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-20 04:34:13'),(9,4,'Queso campesino 250g','Queso fresco campesino.',8900.00,NULL,30,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-20 04:34:13'),(10,5,'Detergente líquido 1L','Detergente para ropa de uso diario.',14500.00,NULL,35,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-20 04:34:13'),(11,5,'Jabón lavaloza 500g','Jabón lavaloza en crema.',5200.00,NULL,50,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-20 04:34:13'),(12,6,'Jabón de baño x3','Paquete de tres jabones de baño.',7800.00,NULL,45,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-20 04:34:13'),(13,6,'Crema dental 90g','Crema dental con flúor.',5900.00,NULL,60,5,'/placeholder.svg','ACTIVO','2026-10-20 04:34:13','2026-10-20 04:34:13'),(14,1,'Prueba 1792470875851','tmp',1000.00,NULL,5,1,'/placeholder.svg','INACTIVO','2026-10-20 04:34:35','2026-10-20 04:34:35');
/*!40000 ALTER TABLE `productos` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `configuracion_tienda` WRITE;
/*!40000 ALTER TABLE `configuracion_tienda` DISABLE KEYS */;
INSERT INTO `configuracion_tienda` (`id`, `clave`, `valor`, `actualizado_en`) VALUES (1,'nombre_comercial','Aquí es Toña','2026-10-20 02:37:14'),(2,'proyecto','COGNIA','2026-10-20 02:37:14'),(3,'moneda','COP','2026-10-20 02:37:14'),(4,'pagos_reales_habilitados','false','2026-10-20 02:37:14'),(5,'domicilio_habilitado','true','2026-10-20 02:37:14'),(6,'recogida_habilitada','true','2026-10-20 02:37:14'),(7,'mision','Facilitar el acceso a alimentos y productos de consumo de calidad con un servicio cercano y confiable. (Editable por el propietario)','2026-10-20 04:45:17'),(8,'vision','Ser la tienda virtual de referencia de nuestra comunidad. (Editable por el propietario)','2026-10-20 04:45:17'),(9,'valores','Honestidad,Calidad,Servicio,Cercanía','2026-10-21 02:03:47'),(10,'costo_domicilio','5000','2026-10-20 04:45:17'),(11,'contacto_telefono','','2026-10-20 04:45:17'),(12,'contacto_correo','','2026-10-20 04:45:17'),(13,'contacto_direccion','','2026-10-20 04:45:17'),(14,'info_domicilios','Los domicilios se coordinan con la dirección indicada al hacer el pedido.','2026-10-21 02:03:47'),(15,'info_recogida','Puedes recoger tu pedido en la tienda cuando esté listo.','2026-10-21 02:03:47'),(16,'promociones_texto','Productos con precio rebajado se marcan automáticamente como promoción.','2026-10-21 02:03:47');
/*!40000 ALTER TABLE `configuracion_tienda` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

