import { useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { api, mensajeError, moneda } from '../api.js';
import { useCart } from '../cart.jsx';
import { Campo } from '../components/ui.jsx';

export default function Checkout() {
  const { items, total, vaciar } = useCart();
  const nav = useNavigate();
  const [cfg, setCfg] = useState({});
  const [f, setF] = useState({ metodoEntrega: 'RECOGIDA', direccionEntrega: '', barrio: '', ciudad: '', observaciones: '' });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);
  // Una clave por intento de compra: reintentos del mismo envío no duplican el pedido.
  const clave = useRef(crypto.randomUUID());

  useEffect(() => { api.get('/api/configuracion/publica').then((r) => setCfg(r.data)).catch(() => {}); }, []);

  if (items.length === 0) return <div className="container estado"><h1>No hay productos en el carrito</h1><Link to="/productos" className="btn">Ver productos</Link></div>;

  const domicilio = f.metodoEntrega === 'DOMICILIO';
  const costo = domicilio ? Number(cfg.costo_domicilio || 0) : 0;
  const domOk = cfg.domicilio_habilitado !== 'false';
  const recOk = cfg.recogida_habilitada !== 'false';

  async function confirmar(e) {
    e.preventDefault();
    setError(''); setEnviando(true);
    try {
      const body = {
        items: items.map((i) => ({ productoId: i.id, cantidad: i.cantidad })),
        metodoEntrega: f.metodoEntrega,
        direccionEntrega: domicilio ? f.direccionEntrega.trim() : null,
        barrio: domicilio ? f.barrio.trim() || null : null,
        ciudad: domicilio ? f.ciudad.trim() || null : null,
        observaciones: f.observaciones.trim() || null,
      };
      const { data } = await api.post('/api/pedidos', body, { headers: { 'Idempotency-Key': clave.current } });
      vaciar();
      nav(`/mis-pedidos/${data.id}`, { replace: true });
    } catch (err) { setError(mensajeError(err)); } finally { setEnviando(false); }
  }

  return (
    <div className="container estrecho">
      <h1>Confirmar pedido</h1>
      <form className="form" onSubmit={confirmar}>
        <fieldset>
          <legend>Entrega</legend>
          <label className="check"><input type="radio" name="entrega" disabled={!recOk} checked={!domicilio} onChange={() => setF({ ...f, metodoEntrega: 'RECOGIDA' })} /> Recogida en tienda</label>
          <label className="check"><input type="radio" name="entrega" disabled={!domOk} checked={domicilio} onChange={() => setF({ ...f, metodoEntrega: 'DOMICILIO' })} /> Domicilio ({moneda(cfg.costo_domicilio || 0)})</label>
        </fieldset>
        {domicilio && (
          <>
            <Campo etiqueta="Dirección de entrega"><input required maxLength={255} value={f.direccionEntrega} onChange={(e) => setF({ ...f, direccionEntrega: e.target.value })} /></Campo>
            <Campo etiqueta="Barrio"><input maxLength={120} value={f.barrio} onChange={(e) => setF({ ...f, barrio: e.target.value })} /></Campo>
            <Campo etiqueta="Ciudad"><input maxLength={120} value={f.ciudad} onChange={(e) => setF({ ...f, ciudad: e.target.value })} /></Campo>
          </>
        )}
        <Campo etiqueta="Observaciones (opcional)"><textarea maxLength={500} rows={3} value={f.observaciones} onChange={(e) => setF({ ...f, observaciones: e.target.value })} /></Campo>
        <div className="resumen">
          <p>Productos ({items.length}): {moneda(total)}</p>
          <p>Entrega: {moneda(costo)}</p>
          <p className="total">Total estimado: <b>{moneda(total + costo)}</b></p>
          <small>El total definitivo lo calcula el servidor con los precios vigentes.</small>
        </div>
        {error && <p className="error" role="alert">{error}</p>}
        <button className="btn" disabled={enviando}>{enviando ? 'Creando pedido…' : 'Confirmar pedido'}</button>
      </form>
      <p className="nota">Después de crear el pedido podrás simular el pago. No se solicitan datos de tarjetas.</p>
    </div>
  );
}
