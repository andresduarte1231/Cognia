import { useCallback, useEffect, useState } from 'react';
import { api, fecha, mensajeError } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { Campo, Estado, Paginador } from '../../components/ui.jsx';

const ROLES = ['ADMINISTRADOR', 'EMPLEADO_VENTAS', 'CLIENTE'];

export default function AdminUsuarios() {
  const { usuario } = useAuth();
  const esOwner = usuario.rol === 'PROPIETARIO';
  const [fl, setFl] = useState({ q: '', rol: '', estado: '' });
  const [pagina, setPagina] = useState(0);
  const [data, setData] = useState(null);
  const [form, setForm] = useState(null);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');

  const cargar = useCallback(() => api.get('/api/admin/usuarios', { params: { q: fl.q || undefined, rol: fl.rol || undefined, estado: fl.estado || undefined, pagina, tamano: 10 } })
    .then((r) => setData(r.data)).catch((e) => setError(mensajeError(e))), [fl, pagina]);
  useEffect(() => { cargar(); }, [cargar]);

  async function guardar(e) {
    e.preventDefault();
    setError(''); setMsg('');
    try {
      if (form.id) await api.put(`/api/admin/usuarios/${form.id}`, { nombre: form.nombre.trim(), rol: form.rol });
      else await api.post('/api/admin/usuarios', { nombre: form.nombre.trim(), correo: form.correo.trim(), rol: form.rol, password: form.password || null });
      setForm(null); setMsg('Usuario guardado.'); cargar();
    } catch (err) { setError(mensajeError(err)); }
  }

  async function accion(fn, texto) {
    setError(''); setMsg('');
    try { const r = await fn(); setMsg(typeof texto === 'function' ? texto(r.data) : texto); cargar(); } catch (err) { setError(mensajeError(err)); }
  }

  return (
    <>
      <h1>Usuarios</h1>
      <div className="filtros">
        <input aria-label="Buscar" placeholder="Nombre o correo" value={fl.q} onChange={(e) => { setFl({ ...fl, q: e.target.value }); setPagina(0); }} />
        <select aria-label="Rol" value={fl.rol} onChange={(e) => { setFl({ ...fl, rol: e.target.value }); setPagina(0); }}><option value="">Todos los roles</option>{['PROPIETARIO', ...ROLES].map((r) => <option key={r}>{r}</option>)}</select>
        <select aria-label="Estado" value={fl.estado} onChange={(e) => { setFl({ ...fl, estado: e.target.value }); setPagina(0); }}><option value="">Todos los estados</option><option>ACTIVO</option><option>INACTIVO</option><option>BLOQUEADO</option></select>
        {esOwner && <button className="btn" onClick={() => { setForm({ nombre: '', correo: '', rol: 'EMPLEADO_VENTAS', password: '' }); setMsg(''); }}>Nuevo usuario</button>}
      </div>
      {!esOwner && <p className="nota">Como administrador solo puedes consultar usuarios; las modificaciones son exclusivas del propietario.</p>}
      {msg && <p className="ok" role="status">{msg}</p>}
      {error && <p className="error" role="alert">{error}</p>}

      {form && (
        <form className="form card pad" onSubmit={guardar}>
          <h2>{form.id ? 'Editar usuario' : 'Nuevo usuario'}</h2>
          <Campo etiqueta="Nombre"><input required maxLength={120} value={form.nombre} onChange={(e) => setForm({ ...form, nombre: e.target.value })} /></Campo>
          {!form.id && <Campo etiqueta="Correo"><input type="email" required value={form.correo} onChange={(e) => setForm({ ...form, correo: e.target.value })} /></Campo>}
          <Campo etiqueta="Rol"><select value={form.rol} onChange={(e) => setForm({ ...form, rol: e.target.value })}>{ROLES.map((r) => <option key={r}>{r}</option>)}</select></Campo>
          {!form.id && <Campo etiqueta="Contraseña inicial (opcional; si se omite se genera una temporal)"><input type="password" autoComplete="new-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} /></Campo>}
          <div className="fila"><button className="btn">Guardar</button><button type="button" className="btn sec" onClick={() => setForm(null)}>Cancelar</button></div>
        </form>
      )}

      <Estado cargando={!data && !error} error="">
        {data && (
          <>
            <div className="tabla-wrap"><table className="tabla"><thead><tr><th>Nombre</th><th>Correo</th><th>Rol</th><th>Estado</th><th>Creado</th><th /></tr></thead>
              <tbody>{data.contenido.map((u) => {
                const propio = u.id === usuario.id || u.rol === 'PROPIETARIO';
                return (
                  <tr key={u.id}><td>{u.nombre}</td><td>{u.correo}</td><td>{u.rol}</td><td>{u.estado}</td><td>{fecha(u.creadoEn)}</td>
                    <td>{esOwner && !propio && (
                      <>
                        <button className="btn sec" onClick={() => setForm({ ...u })}>Editar</button>{' '}
                        <button className="btn sec" onClick={() => accion(() => api.patch(`/api/admin/usuarios/${u.id}/estado`, { estado: u.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO' }), 'Estado actualizado.')}>{u.estado === 'ACTIVO' ? 'Desactivar' : 'Activar'}</button>{' '}
                        <button className="btn sec" onClick={() => window.confirm(`¿Restablecer la contraseña de ${u.correo}?`) && accion(() => api.post(`/api/admin/usuarios/${u.id}/restablecer-password`), (d) => `Contraseña temporal (se muestra una sola vez): ${d.passwordTemporal}`)}>Restablecer clave</button>
                      </>
                    )}</td></tr>
                );
              })}</tbody></table></div>
            <Paginador pagina={data.pagina} totalPaginas={data.totalPaginas} onChange={setPagina} />
          </>
        )}
      </Estado>
    </>
  );
}
