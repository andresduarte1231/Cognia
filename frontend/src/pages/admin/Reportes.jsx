import { useEffect, useState } from 'react';
import { api, descargarCsv, fecha, mensajeError, moneda } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { Estado, Paginador } from '../../components/ui.jsx';

export default function AdminReportes() {
  const { usuario } = useAuth();
  const [rango, setRango] = useState({ desde: '', hasta: '' });
  const [ventas, setVentas] = useState(null);
  const [top, setTop] = useState(null);
  const [pagos, setPagos] = useState(null);
  const [pp, setPp] = useState(0);
  const [aud, setAud] = useState(null);
  const [error, setError] = useState('');
  const params = { desde: rango.desde || undefined, hasta: rango.hasta || undefined };
  const verAud = usuario.rol === 'PROPIETARIO' || usuario.rol === 'ADMINISTRADOR';

  useEffect(() => {
    api.get('/api/admin/reportes/ventas', { params }).then((r) => setVentas(r.data)).catch((e) => setError(mensajeError(e)));
    api.get('/api/admin/reportes/mas-vendidos', { params: { ...params, limite: 10 } }).then((r) => setTop(r.data)).catch((e) => setError(mensajeError(e)));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rango]);
  useEffect(() => { api.get('/api/admin/pagos', { params: { ...params, pagina: pp, tamano: 10 } }).then((r) => setPagos(r.data)).catch((e) => setError(mensajeError(e))); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [rango, pp]);
  useEffect(() => { if (verAud) api.get('/api/admin/auditoria', { params: { tamano: 15 } }).then((r) => setAud(r.data.contenido)).catch(() => {}); }, [verAud]);

  const csv = (url, nombre) => descargarCsv(url, nombre, params).catch((e) => setError(mensajeError(e)));

  return (
    <>
      <h1>Reportes</h1>
      <div className="filtros">
        <input type="date" aria-label="Desde" value={rango.desde} onChange={(e) => setRango({ ...rango, desde: e.target.value })} />
        <input type="date" aria-label="Hasta" value={rango.hasta} onChange={(e) => setRango({ ...rango, hasta: e.target.value })} />
      </div>
      {error && <p className="error" role="alert">{error}</p>}
      <p className="nota">Las ventas solo incluyen pagos aprobados en pedidos no cancelados; los pagos rechazados no cuentan como venta.</p>

      <h2>Ventas por día <button className="btn sec" onClick={() => csv('/api/admin/reportes/ventas.csv', 'ventas.csv')}>Exportar CSV</button></h2>
      <Estado cargando={!ventas && !error} error="" vacio={ventas && ventas.length === 0 ? 'Sin ventas en el periodo.' : null}>
        {ventas && <div className="tabla-wrap"><table className="tabla"><thead><tr><th>Fecha</th><th>Pedidos</th><th>Total</th></tr></thead>
          <tbody>{ventas.map((v) => <tr key={v.fecha}><td>{v.fecha}</td><td>{v.pedidos}</td><td>{moneda(v.total)}</td></tr>)}</tbody></table></div>}
      </Estado>

      <h2>Productos más vendidos <button className="btn sec" onClick={() => csv('/api/admin/reportes/mas-vendidos.csv', 'mas-vendidos.csv')}>Exportar CSV</button></h2>
      {top && (top.length === 0 ? <p className="nota">Sin datos.</p> : <div className="tabla-wrap"><table className="tabla"><thead><tr><th>Producto</th><th>Unidades</th><th>Ingresos</th></tr></thead>
        <tbody>{top.map((p) => <tr key={p.productoId}><td>{p.nombre}</td><td>{p.unidades}</td><td>{moneda(p.ingresos)}</td></tr>)}</tbody></table></div>)}

      <h2>Pagos simulados</h2>
      {pagos && (
        <>
          <div className="tabla-wrap"><table className="tabla"><thead><tr><th>Fecha</th><th>Referencia</th><th>Estado</th><th>Importe</th></tr></thead>
            <tbody>{pagos.contenido.map((p) => <tr key={p.id}><td>{fecha(p.creadoEn)}</td><td>{p.referencia}</td><td>{p.estado}</td><td>{moneda(p.importe)}</td></tr>)}</tbody></table></div>
          <Paginador pagina={pagos.pagina} totalPaginas={pagos.totalPaginas} onChange={setPp} />
        </>
      )}

      {verAud && aud && (
        <>
          <h2>Actividad administrativa</h2>
          <div className="tabla-wrap"><table className="tabla"><thead><tr><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Recurso</th><th>Resultado</th></tr></thead>
            <tbody>{aud.map((a) => <tr key={a.id}><td>{fecha(a.creadoEn)}</td><td>{a.usuarioId ?? '-'}</td><td>{a.accion}</td><td>{a.recurso} {a.recursoId}</td><td>{a.resultado}</td></tr>)}</tbody></table></div>
        </>
      )}
    </>
  );
}
