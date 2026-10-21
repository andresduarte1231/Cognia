const TEXTOS = {
  privacidad: {
    titulo: 'Política de privacidad y tratamiento de datos personales',
    bloques: [
      ['Responsable', 'Aquí es Toña (proyecto académico COGNIA). Los datos de identificación y contacto del responsable deben ser completados por el propietario antes de operar comercialmente.'],
      ['Datos que recolectamos', 'Nombre, correo electrónico, contraseña (almacenada cifrada, nunca en texto plano) y, al pedir un domicilio, dirección de entrega. No se solicitan datos de tarjetas ni bancarios.'],
      ['Finalidad', 'Gestionar tu cuenta, procesar y entregar pedidos, y mantener la seguridad del servicio.'],
      ['Tus derechos (Ley 1581 de 2012)', 'Como titular puedes conocer, actualizar, rectificar y suprimir tus datos, y revocar la autorización, conforme a la Ley 1581 de 2012 y el Decreto 1377 de 2013 (compilado en el Decreto 1074 de 2015).'],
      ['Seguridad', 'Aplicamos controles técnicos razonables (cifrado de contraseñas, control de acceso por roles, registros de auditoría). Ningún sistema es infalible.'],
    ],
  },
  terminos: {
    titulo: 'Términos y condiciones',
    bloques: [
      ['Naturaleza del servicio', 'Esta tienda es un proyecto académico. Los pagos son simulados y no generan cobros reales.'],
      ['Precios y existencias', 'Los precios y la disponibilidad se validan en el servidor al confirmar cada pedido y pueden cambiar sin previo aviso.'],
      ['Pedidos', 'Un pedido pendiente que no se pague puede cancelarse automáticamente y sus existencias se liberan.'],
      ['Uso adecuado', 'Está prohibido intentar vulnerar la seguridad del sistema o acceder a cuentas ajenas.'],
    ],
  },
};

export function Legal({ tipo }) {
  const t = TEXTOS[tipo];
  return (
    <div className="container estrecho">
      <h1>{t.titulo}</h1>
      {t.bloques.map(([h, p]) => (<section key={h}><h2>{h}</h2><p>{p}</p></section>))}
    </div>
  );
}
