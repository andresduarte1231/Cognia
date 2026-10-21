import { useCallback, useEffect, useState } from 'react';
import { api, descargarCsv, fecha, mensajeError } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { Campo, Estado, Paginador } from '../../components/ui.jsx';

export default function AdminInventario() {
  const { usuario } = useAuth();
  const puedeEditar = usuario.rol === 'PROPIETARIO' || usuario.rol === 'ADMINISTRADOR';
  const [alertas, setAlertas] = useState(null);
  const [mov, setMov] = useState(null);
  const [pagina, setPagina] = useState(0);
  const [productos, setProductos] = useState([]);
  const [f, setF] = useState({ productoId: '', tipo: 'ENTRADA', cantidad: 1, motivo: '' });
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');

  const cargar = useCallback(() => {
    api.get('/api/admin/inventario/alertas').then((r) => setAlertas(r.data)).catch((e) => setError(mensajeError(e)));
    api.get('/api/admin/inventario/movimientos', { params: { pagina, tamano: 10 } }).then((r) => setMov(r.data)).catch((e) => setError(mensajeError(e)));
  }, [pagina]);
  useEffect(() => { cargar(); }, [cargar]);
  useEffect(() => { api.get('/api/admin/productos', { params: { tamano: 100 } }).then((r) => setProductos(r.data.contenido)).catch(() => {}); }, []);

  async function registrar(e) {
    e.preventDefault();
    setError(''); setMsg('');
    try {
      await api.post('/api/admin/inventario/movimientos', { productoId: Number(f.productoId), tipo: f.tipo, cantidad: Number(f.cantidad), motivo: f.motivo.trim() });
      setMsg('Movimiento registrado.'); setF({ ...f, cantidad: 1, motivo: '' }); cargar();
    } catch (err) { setError(mensajeError(err)); }
  }

  return (
    <>
      <h1>Inventario</h1>
      {error && <p className="error" role="alert">{error}</p>}
      {msg && <p className="ok" role="status">{msg}</p>}
      <h2>Alertas de existencias bajas</h2>
      <Estado cargando={!alertas && !error} error="" vacio={alertas && alertas.length === 0 ? 'No hay productos con existencias bajas.' : null}>
        {alertas && <ul>{alertas.map((p) => <li key={p.id}><b>{p.nombre}</b>: {p.existencias} (mínimo {p.inventarioMinimo})</li>)}</ul>}
      </Estado>

      {puedeEditar && (
        <form className="form card pad" onSubmit={registrar}>
          <h2>Registrar movimiento</h2>
          <Campo etiqueta="Producto">
            <select required value={f.productoId} onChange={(e) => setF({ ...f, productoId: e.target.value })}>
              <option value="" disabled>Selecciona…</option>
              {productos.map((p) => <option key={p.id} value={p.id}>{p.nombre} (stock {p.existencias})</option>)}
            </select>
          </Campo>
          <Campo etiqueta="Tipo">
            <select value={f.tipo} onChange={(e) => setF({ ...f, tipo: e.target.value })}>
              <option value="ENTRADA">Entrada (suma)</option><option value="SALIDA">Salida (resta)</option><option value="AJUSTE">Ajuste (fija el total)</option>
            </select>
          </Campo>
          <Campo etiqueta="Cantidad"><input type="number" min="0" required value={f.cantidad} onChange={(e) => setF({ ...f, cantidad: e.target.value })} /></Campo>
          <Campo etiqueta="Motivo"><input required maxLength={200} value={f.motivo} onChange={(e) => setF({ ...f, motivo: e.target.value })} /></Campo>
          <button className="btn">Registrar</button>
        </form>
      )}

      <h2>Historial de movimientos</h2>
      {mov && (
        <>
          <div className="tabla-wrap"><table className="tabla"><thead><tr><th>Fecha</th><th>Producto</th><th>Tipo</th><th>Cant.</th><th>Motivo</th></tr></thead>
            <tbody>{mov.contenido.map((m) => <tr key={m.id}><td>{fecha(m.creadoEn)}</td><td>{m.productoId}</td><td>{m.tipo}</td><td>{m.cantidad}</td><td>{m.motivo}</td></tr>)}</tbody></table></div>
          <Paginador pagina={mov.pagina} totalPaginas={mov.totalPaginas} onChange={setPagina} />
        </>
      )}
    </>
  );
}
