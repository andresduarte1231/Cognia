import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, fecha, mensajeError, moneda } from '../api.js';
import { Estado, Insignia, Paginador } from '../components/ui.jsx';

export function MisPedidos() {
  const [pagina, setPagina] = useState(0);
  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => {
    api.get('/api/pedidos', { params: { pagina, tamano: 10 } }).then((r) => setData(r.data)).catch((e) => setError(mensajeError(e)));
  }, [pagina]);
  return (
    <div className="container">
      <h1>Mis pedidos</h1>
      <Estado cargando={!data && !error} error={error} vacio={data && data.contenido.length === 0 ? 'Aún no tienes pedidos.' : null}>
        {data && (
          <>
            <div className="tabla-wrap"><table className="tabla">
              <thead><tr><th>Número</th><th>Fecha</th><th>Estado</th><th>Total</th><th /></tr></thead>
              <tbody>{data.contenido.map((p) => (
                <tr key={p.id}><td>{p.numeroPedido}</td><td>{fecha(p.creadoEn)}</td><td><Insignia estado={p.estado} /></td><td>{moneda(p.total)}</td><td><Link to={`/mis-pedidos/${p.id}`}>Ver detalle</Link></td></tr>
              ))}</tbody>
            </table></div>
            <Paginador pagina={data.pagina} totalPaginas={data.totalPaginas} onChange={setPagina} />
          </>
        )}
      </Estado>
    </div>
  );
}

export function PedidoDetalle() {
  const { id } = useParams();
  const [p, setP] = useState(null);
  const [error, setError] = useState('');
  const [accion, setAccion] = useState('');
  const [msg, setMsg] = useState('');

  const cargar = useCallback(() => api.get(`/api/pedidos/${id}`).then((r) => setP(r.data)).catch((e) => setError(e.response?.status === 404 ? 'Pedido no encontrado.' : mensajeError(e))), [id]);
  useEffect(() => { cargar(); }, [cargar]);

  async function ejecutar(nombre, fn) {
    setAccion(nombre); setMsg(''); setError('');
    try { setP((await fn()).data); } catch (e) { setError(mensajeError(e)); } finally { setAccion(''); }
  }

  const pendientePago = p && p.estado === 'PENDIENTE' && p.pago?.estado === 'PENDIENTE';
  const cancelable = p && ['PENDIENTE', 'CONFIRMADO'].includes(p.estado);

  return (
    <div className="container">
      <p><Link to="/mis-pedidos">← Mis pedidos</Link></p>
      <Estado cargando={!p && !error} error={!p ? error : ''}>
        {p && (
          <>
            <h1>Pedido {p.numeroPedido}</h1>
            <p><Insignia estado={p.estado} /> · {fecha(p.creadoEn)} · {p.metodoEntrega === 'DOMICILIO' ? 'Domicilio' : 'Recogida en tienda'}</p>
            {p.metodoEntrega === 'DOMICILIO' && <p>Dirección: {p.direccionEntrega}{p.barrio ? `, ${p.barrio}` : ''}{p.ciudad ? `, ${p.ciudad}` : ''}</p>}
            <div className="tabla-wrap"><table className="tabla">
              <thead><tr><th>Producto</th><th>Precio</th><th>Cantidad</th><th>Subtotal</th></tr></thead>
              <tbody>{p.detalles.map((d) => <tr key={d.productoId}><td>{d.nombreProducto}</td><td>{moneda(d.precioUnitario)}</td><td>{d.cantidad}</td><td>{moneda(d.subtotal)}</td></tr>)}</tbody>
            </table></div>
            <p>Subtotal: {moneda(p.subtotal)} · Entrega: {moneda(p.costoEntrega)}</p>
            <p className="total">Total: <b>{moneda(p.total)}</b></p>
            {p.pago && <p>Pago simulado: <b>{p.pago.estado}</b> (ref. {p.pago.referencia})</p>}
            {error && <p className="error" role="alert">{error}</p>}
            {msg && <p className="ok" role="status">{msg}</p>}
            {pendientePago && (
              <div className="card pad">
                <h3>Simular pago</h3>
                <p className="nota">Entorno académico: elige el resultado del pago simulado. Un pago aprobado confirma el pedido; uno rechazado lo cancela y libera las existencias.</p>
                <div className="fila">
                  <button className="btn" disabled={!!accion} onClick={() => ejecutar('ok', () => api.post(`/api/pedidos/${p.id}/pago`, { resultado: 'APROBADO' }))}>Pago aprobado</button>
                  <button className="btn sec" disabled={!!accion} onClick={() => ejecutar('no', () => api.post(`/api/pedidos/${p.id}/pago`, { resultado: 'RECHAZADO' }))}>Pago rechazado</button>
                </div>
              </div>
            )}
            {cancelable && <p><button className="btn sec" disabled={!!accion} onClick={() => window.confirm('¿Cancelar este pedido?') && ejecutar('cancel', () => api.post(`/api/pedidos/${p.id}/cancelar`))}>Cancelar pedido</button></p>}
          </>
        )}
      </Estado>
    </div>
  );
}
