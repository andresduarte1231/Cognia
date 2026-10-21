import { useCallback, useEffect, useState } from 'react';
import { api, fecha, mensajeError, moneda } from '../../api.js';
import { ESTADOS_PEDIDO, Estado, Insignia, Paginador, etiquetaEstado } from '../../components/ui.jsx';

export default function AdminPedidos() {
  const [fl, setFl] = useState({ numero: '', estado: '', desde: '', hasta: '' });
  const [pagina, setPagina] = useState(0);
  const [data, setData] = useState(null);
  const [sel, setSel] = useState(null);
  const [nuevo, setNuevo] = useState('');
  const [error, setError] = useState('');

  const cargar = useCallback(() => api.get('/api/admin/pedidos', {
    params: { numero: fl.numero || undefined, estado: fl.estado || undefined, desde: fl.desde || undefined, hasta: fl.hasta || undefined, pagina, tamano: 10 },
  }).then((r) => { setData(r.data); setError(''); }).catch((e) => setError(mensajeError(e))), [fl, pagina]);
  useEffect(() => { cargar(); }, [cargar]);

  async function ver(id) {
    setError('');
    try { const { data: p } = await api.get(`/api/admin/pedidos/${id}`); setSel(p); setNuevo(''); } catch (e) { setError(mensajeError(e)); }
  }

  async function cambiar() {
    setError('');
    try { const { data: p } = await api.patch(`/api/admin/pedidos/${sel.id}/estado`, { estado: nuevo }); setSel(p); setNuevo(''); cargar(); } catch (e) { setError(mensajeError(e)); }
  }

  const f = (k) => (e) => { setFl({ ...fl, [k]: e.target.value }); setPagina(0); };

  return (
    <>
      <h1>Pedidos</h1>
      <div className="filtros">
        <input aria-label="Número de pedido" placeholder="Número de pedido" value={fl.numero} onChange={f('numero')} />
        <select aria-label="Estado" value={fl.estado} onChange={f('estado')}><option value="">Todos los estados</option>{ESTADOS_PEDIDO.map((s) => <option key={s} value={s}>{etiquetaEstado(s)}</option>)}</select>
        <input type="date" aria-label="Desde" value={fl.desde} onChange={f('desde')} />
        <input type="date" aria-label="Hasta" value={fl.hasta} onChange={f('hasta')} />
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      <Estado cargando={!data && !error} error="" vacio={data && data.contenido.length === 0 ? 'No hay pedidos con esos filtros.' : null}>
        {data && (
          <>
            <div className="tabla-wrap"><table className="tabla"><thead><tr><th>Número</th><th>Fecha</th><th>Entrega</th><th>Estado</th><th>Total</th><th /></tr></thead>
              <tbody>{data.contenido.map((p) => <tr key={p.id}><td>{p.numeroPedido}</td><td>{fecha(p.creadoEn)}</td><td>{p.metodoEntrega}</td><td><Insignia estado={p.estado} /></td><td>{moneda(p.total)}</td><td><button className="btn sec" onClick={() => ver(p.id)}>Detalle</button></td></tr>)}</tbody></table></div>
            <Paginador pagina={data.pagina} totalPaginas={data.totalPaginas} onChange={setPagina} />
          </>
        )}
      </Estado>
      {sel && (
        <div className="card pad">
          <h2>Pedido {sel.numeroPedido} <Insignia estado={sel.estado} /></h2>
          <p>{sel.metodoEntrega === 'DOMICILIO' ? `Domicilio: ${sel.direccionEntrega || ''} ${sel.barrio || ''} ${sel.ciudad || ''}` : 'Recogida en tienda'}</p>
          {sel.observacionesEntrega && <p>Observaciones: {sel.observacionesEntrega}</p>}
          <ul>{sel.detalles.map((d) => <li key={d.productoId}>{d.cantidad} × {d.nombreProducto} — {moneda(d.subtotal)}</li>)}</ul>
          <p><b>Total: {moneda(sel.total)}</b> · Pago: {sel.pago?.estado}</p>
          <div className="fila">
            <select aria-label="Nuevo estado" value={nuevo} onChange={(e) => setNuevo(e.target.value)}>
              <option value="">Cambiar estado…</option>
              {ESTADOS_PEDIDO.filter((s) => s !== sel.estado).map((s) => <option key={s} value={s}>{etiquetaEstado(s)}</option>)}
            </select>
            <button className="btn" disabled={!nuevo} onClick={cambiar}>Actualizar</button>
            <button className="btn sec" onClick={() => setSel(null)}>Cerrar</button>
          </div>
          <small>El servidor solo permite las transiciones válidas según el método de entrega.</small>
        </div>
      )}
    </>
  );
}
