import { useCallback, useEffect, useState } from 'react';
import { api, mensajeError, moneda } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { Campo, Estado, Paginador } from '../../components/ui.jsx';

const VACIO = { categoriaId: '', nombre: '', descripcion: '', precio: '', precioAnterior: '', existencias: 0, inventarioMinimo: 0, imagenUrl: '', estado: 'ACTIVO' };

export default function AdminProductos() {
  const { usuario } = useAuth();
  const puedeEditar = usuario.rol === 'PROPIETARIO' || usuario.rol === 'ADMINISTRADOR';
  const [q, setQ] = useState('');
  const [pagina, setPagina] = useState(0);
  const [data, setData] = useState(null);
  const [cats, setCats] = useState([]);
  const [form, setForm] = useState(null);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');

  const cargar = useCallback(() => api.get('/api/admin/productos', { params: { q: q || undefined, pagina, tamano: 10 } })
    .then((r) => setData(r.data)).catch((e) => setError(mensajeError(e))), [q, pagina]);
  useEffect(() => { cargar(); }, [cargar]);
  useEffect(() => { api.get('/api/admin/categorias').then((r) => setCats(r.data)).catch(() => {}); }, []);

  async function guardar(e) {
    e.preventDefault();
    setError(''); setMsg('');
    const body = {
      categoriaId: Number(form.categoriaId), nombre: form.nombre.trim(), descripcion: form.descripcion || null,
      precio: Number(form.precio), precioAnterior: form.precioAnterior === '' || form.precioAnterior == null ? null : Number(form.precioAnterior),
      existencias: Number(form.existencias), inventarioMinimo: Number(form.inventarioMinimo), imagenUrl: form.imagenUrl || null, estado: form.estado,
    };
    try {
      let saved = form.id ? (await api.put(`/api/admin/productos/${form.id}`, body)).data : (await api.post('/api/admin/productos', body)).data;
      if (form.archivo) {
        const fd = new FormData();
        fd.append('archivo', form.archivo);
        saved = (await api.post(`/api/admin/productos/${saved.id}/imagen`, fd)).data;
      }
      setForm(null); setMsg(`Producto «${saved.nombre}» guardado.`); cargar();
    } catch (err) { setError(mensajeError(err)); }
  }

  async function estado(p) {
    setError('');
    try { await api.patch(`/api/admin/productos/${p.id}/estado`, { estado: p.estado === 'INACTIVO' ? 'ACTIVO' : 'INACTIVO' }); cargar(); } catch (err) { setError(mensajeError(err)); }
  }

  const f = (k) => (e) => setForm({ ...form, [k]: e.target.value });

  return (
    <>
      <h1>Productos</h1>
      <div className="fila">
        <input type="search" aria-label="Buscar" placeholder="Buscar por nombre" value={q} onChange={(e) => { setQ(e.target.value); setPagina(0); }} />
        {puedeEditar && <button className="btn" onClick={() => { setForm({ ...VACIO, categoriaId: cats[0]?.id ?? '' }); setMsg(''); }}>Nuevo producto</button>}
      </div>
      {msg && <p className="ok" role="status">{msg}</p>}
      {error && <p className="error" role="alert">{error}</p>}

      {form && (
        <form className="form card pad" onSubmit={guardar}>
          <h2>{form.id ? 'Editar producto' : 'Nuevo producto'}</h2>
          <Campo etiqueta="Nombre"><input required maxLength={160} value={form.nombre} onChange={f('nombre')} /></Campo>
          <Campo etiqueta="Categoría">
            <select required value={form.categoriaId} onChange={f('categoriaId')}>
              <option value="" disabled>Selecciona…</option>
              {cats.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </select>
          </Campo>
          <Campo etiqueta="Descripción"><textarea rows={3} maxLength={2000} value={form.descripcion || ''} onChange={f('descripcion')} /></Campo>
          <div className="cols">
            <Campo etiqueta="Precio (COP)"><input type="number" required min="0" step="0.01" value={form.precio} onChange={f('precio')} /></Campo>
            <Campo etiqueta="Precio anterior (promoción)"><input type="number" min="0" step="0.01" value={form.precioAnterior ?? ''} onChange={f('precioAnterior')} /></Campo>
          </div>
          <div className="cols">
            <Campo etiqueta="Existencias"><input type="number" min="0" value={form.existencias} onChange={f('existencias')} /></Campo>
            <Campo etiqueta="Inventario mínimo"><input type="number" min="0" value={form.inventarioMinimo} onChange={f('inventarioMinimo')} /></Campo>
          </div>
          <Campo etiqueta="Estado">
            <select value={form.estado} onChange={f('estado')}><option>ACTIVO</option><option>INACTIVO</option></select>
          </Campo>
          <Campo etiqueta="Imagen (JPG, PNG o WEBP)"><input type="file" accept="image/jpeg,image/png,image/webp" onChange={(e) => setForm({ ...form, archivo: e.target.files[0] })} /></Campo>
          <div className="fila"><button className="btn">Guardar</button><button type="button" className="btn sec" onClick={() => setForm(null)}>Cancelar</button></div>
        </form>
      )}

      <Estado cargando={!data && !error} error={!data ? error : ''}>
        {data && (
          <>
            <div className="tabla-wrap"><table className="tabla">
              <thead><tr><th>Nombre</th><th>Categoría</th><th>Precio</th><th>Stock</th><th>Estado</th><th /></tr></thead>
              <tbody>{data.contenido.map((p) => (
                <tr key={p.id}>
                  <td>{p.nombre}{p.enPromocion && ' 🏷️'}</td><td>{p.categoriaNombre}</td><td>{moneda(p.precio)}</td>
                  <td className={p.existencias <= p.inventarioMinimo ? 'error' : ''}>{p.existencias}</td><td>{p.estado}</td>
                  <td>{puedeEditar && <><button className="btn sec" onClick={() => { setForm({ ...p, categoriaId: p.categoriaId, archivo: null }); setMsg(''); }}>Editar</button>{' '}
                    <button className="btn sec" onClick={() => estado(p)}>{p.estado === 'INACTIVO' ? 'Activar' : 'Desactivar'}</button></>}</td>
                </tr>))}</tbody>
            </table></div>
            <Paginador pagina={data.pagina} totalPaginas={data.totalPaginas} onChange={setPagina} />
          </>
        )}
      </Estado>
    </>
  );
}
