import { useState } from 'react';
import { Link } from 'react-router-dom';
import { moneda } from '../api.js';
import { useCart } from '../cart.jsx';
import { Campo } from '../components/ui.jsx';

export const WHATSAPP = '573053188171';

export function construirMensaje(items, total, f) {
  const domicilio = f.metodoEntrega === 'DOMICILIO';
  const lineas = [
    '*Nuevo pedido - Aquí es Toña*',
    '',
    `Cliente: ${f.nombre.trim()}`,
    `Teléfono: ${f.telefono.trim()}`,
    `Entrega: ${domicilio ? 'Domicilio' : 'Recogida en tienda'}`,
  ];
  if (domicilio) {
    lineas.push(`Dirección: ${f.direccion.trim()}`);
    if (f.barrio.trim()) lineas.push(`Barrio: ${f.barrio.trim()}`);
    if (f.ciudad.trim()) lineas.push(`Ciudad: ${f.ciudad.trim()}`);
  }
  lineas.push('', '*Productos:*');
  items.forEach((i) => lineas.push(`- ${i.cantidad} x ${i.nombre} (${moneda(i.precio * i.cantidad)})`));
  lineas.push('', `Subtotal productos: ${moneda(total)}`);
  if (domicilio) lineas.push('El costo del domicilio se acuerda por este chat.');
  if (f.observaciones.trim()) lineas.push('', `Observaciones: ${f.observaciones.trim()}`);
  return lineas.join('\n');
}

export default function Checkout() {
  const { items, total, vaciar } = useCart();
  const [f, setF] = useState({ nombre: '', telefono: '', metodoEntrega: 'RECOGIDA', direccion: '', barrio: '', ciudad: '', observaciones: '' });
  const [enviado, setEnviado] = useState(false);

  if (enviado) return <div className="container estado"><h1>¡Pedido enviado a WhatsApp!</h1><p>Continúa la conversación para confirmar el pedido y el domicilio.</p><Link to="/productos" className="btn">Seguir comprando</Link></div>;
  if (items.length === 0) return <div className="container estado"><h1>No hay productos en el carrito</h1><Link to="/productos" className="btn">Ver productos</Link></div>;

  const domicilio = f.metodoEntrega === 'DOMICILIO';

  function enviar(e) {
    e.preventDefault();
    const url = `https://wa.me/${WHATSAPP}?text=${encodeURIComponent(construirMensaje(items, total, f))}`;
    window.open(url, '_blank', 'noopener');
    vaciar();
    setEnviado(true);
  }

  return (
    <div className="container estrecho">
      <h1>Confirmar pedido</h1>
      <form className="form" onSubmit={enviar}>
        <Campo etiqueta="Nombre"><input required maxLength={120} value={f.nombre} onChange={(e) => setF({ ...f, nombre: e.target.value })} /></Campo>
        <Campo etiqueta="Teléfono"><input required type="tel" maxLength={20} value={f.telefono} onChange={(e) => setF({ ...f, telefono: e.target.value })} /></Campo>
        <fieldset>
          <legend>Entrega</legend>
          <label className="check"><input type="radio" name="entrega" checked={!domicilio} onChange={() => setF({ ...f, metodoEntrega: 'RECOGIDA' })} /> Recogida en tienda</label>
          <label className="check"><input type="radio" name="entrega" checked={domicilio} onChange={() => setF({ ...f, metodoEntrega: 'DOMICILIO' })} /> Domicilio (el costo se acuerda por WhatsApp)</label>
        </fieldset>
        {domicilio && (
          <>
            <Campo etiqueta="Dirección de entrega"><input required maxLength={255} value={f.direccion} onChange={(e) => setF({ ...f, direccion: e.target.value })} /></Campo>
            <Campo etiqueta="Barrio"><input maxLength={120} value={f.barrio} onChange={(e) => setF({ ...f, barrio: e.target.value })} /></Campo>
            <Campo etiqueta="Ciudad"><input maxLength={120} value={f.ciudad} onChange={(e) => setF({ ...f, ciudad: e.target.value })} /></Campo>
          </>
        )}
        <Campo etiqueta="Observaciones (opcional)"><textarea maxLength={500} rows={3} value={f.observaciones} onChange={(e) => setF({ ...f, observaciones: e.target.value })} /></Campo>
        <div className="resumen">
          <p>Productos ({items.length}): <b>{moneda(total)}</b></p>
          <small>No incluye domicilio.</small>
        </div>
        <button className="btn">Enviar pedido por WhatsApp</button>
      </form>
    </div>
  );
}