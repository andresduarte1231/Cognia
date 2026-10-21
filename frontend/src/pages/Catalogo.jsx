import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api, mensajeError } from '../api.js';
import { Estado, Paginador, ProductoCard } from '../components/ui.jsx';

const ORDENES = [
  ['nombre|asc', 'Nombre (A-Z)'],
  ['nombre|desc', 'Nombre (Z-A)'],
  ['precio|asc', 'Precio: menor a mayor'],
  ['precio|desc', 'Precio: mayor a menor'],
];

export default function Catalogo() {
  const [sp, setSp] = useSearchParams();
  const q = sp.get('q') || '';
  const categoria = sp.get('categoria') || '';
  const orden = sp.get('orden') || 'nombre|asc';
  const pagina = Number(sp.get('pagina') || 0);
  const [texto, setTexto] = useState(q);
  const [cats, setCats] = useState([]);
  const [data, setData] = useState(null);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => { api.get('/api/categorias').then((r) => setCats(r.data)).catch(() => {}); }, []);

  useEffect(() => {
    const [o, d] = orden.split('|');
    setCargando(true);
    api.get('/api/productos', { params: { q: q || undefined, categoriaId: categoria || undefined, orden: o, direccion: d, pagina, tamano: 12 } })
      .then((r) => { setData(r.data); setError(''); })
      .catch((e) => setError(mensajeError(e)))
      .finally(() => setCargando(false));
  }, [q, categoria, orden, pagina]);

  const set = (cambios) => {
    const n = new URLSearchParams(sp);
    Object.entries({ pagina: 0, ...cambios }).forEach(([k, v]) => (v ? n.set(k, v) : n.delete(k)));
    setSp(n);
  };

  return (
    <div className="container">
      <h1>Productos</h1>
      <form className="filtros" role="search" onSubmit={(e) => { e.preventDefault(); set({ q: texto.trim() }); }}>
        <input type="search" aria-label="Buscar por nombre" placeholder="Buscar por nombre" value={texto} maxLength={100} onChange={(e) => setTexto(e.target.value)} />
        <select aria-label="Categoría" value={categoria} onChange={(e) => set({ categoria: e.target.value })}>
          <option value="">Todas las categorías</option>
          {cats.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
        </select>
        <select aria-label="Ordenar" value={orden} onChange={(e) => set({ orden: e.target.value })}>
          {ORDENES.map(([v, t]) => <option key={v} value={v}>{t}</option>)}
        </select>
        <button className="btn">Buscar</button>
      </form>
      <Estado cargando={cargando} error={error} vacio={data && data.contenido.length === 0 ? 'No se encontraron productos.' : null}>
        {data && (
          <>
            <div className="grid">{data.contenido.map((p) => <ProductoCard key={p.id} p={p} />)}</div>
            <Paginador pagina={data.pagina} totalPaginas={data.totalPaginas} onChange={(n) => set({ pagina: n || '' })} />
          </>
        )}
      </Estado>
    </div>
  );
}
