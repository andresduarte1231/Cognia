import { useCallback, useEffect, useState } from 'react';
import { api, mensajeError } from '../../api.js';
import { Campo, Estado } from '../../components/ui.jsx';

export default function AdminCategorias() {
  const [lista, setLista] = useState(null);
  const [form, setForm] = useState(null);
  const [error, setError] = useState('');

  const cargar = useCallback(() => api.get('/api/admin/categorias').then((r) => setLista(r.data)).catch((e) => setError(mensajeError(e))), []);
  useEffect(() => { cargar(); }, [cargar]);

  async function guardar(e) {
    e.preventDefault();
    setError('');
    const body = { nombre: form.nombre.trim(), descripcion: form.descripcion || null, estado: form.estado };
    try {
      if (form.id) await api.put(`/api/admin/categorias/${form.id}`, body); else await api.post('/api/admin/categorias', body);
      setForm(null); cargar();
    } catch (err) { setError(mensajeError(err)); }
  }

  return (
    <>
      <h1>Categorías</h1>
      <button className="btn" onClick={() => setForm({ nombre: '', descripcion: '', estado: 'ACTIVA' })}>Nueva categoría</button>
      {error && <p className="error" role="alert">{error}</p>}
      {form && (
        <form className="form card pad" onSubmit={guardar}>
          <Campo etiqueta="Nombre"><input required maxLength={100} value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} /></Campo>
          <Campo etiqueta="Descripción"><textarea maxLength={1000} value={form.descripcion || ''} onChange={(e) => setForm({ ...form, descripcion: e.target.value })} /></Campo>
          <Campo etiqueta="Estado"><select value={form.estado} onChange={(e) => setForm({ ...form, estado: e.target.value })}><option>ACTIVA</option><option>INACTIVA</option></select></Campo>
          <div className="fila"><button className="btn">Guardar</button><button type="button" className="btn sec" onClick={() => setForm(null)}>Cancelar</button></div>
        </form>
      )}
      <Estado cargando={!lista && !error} error="">
        {lista && <div className="tabla-wrap"><table className="tabla"><thead><tr><th>Nombre</th><th>Descripción</th><th>Estado</th><th /></tr></thead>
          <tbody>{lista.map((c) => <tr key={c.id}><td>{c.nombre}</td><td>{c.descripcion}</td><td>{c.estado}</td><td><button className="btn sec" onClick={() => setForm({ ...c })}>Editar</button></td></tr>)}</tbody></table></div>}
      </Estado>
    </>
  );
}
