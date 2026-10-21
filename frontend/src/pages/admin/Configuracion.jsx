import { useEffect, useState } from 'react';
import { api, mensajeError } from '../../api.js';
import { Campo } from '../../components/ui.jsx';

const CAMPOS = [
  ['mision', 'Misión', true], ['vision', 'Visión', true], ['valores', 'Valores (separados por coma)'],
  ['costo_domicilio', 'Costo de domicilio (COP)'], ['info_domicilios', 'Información de domicilios', true], ['info_recogida', 'Información de recogida en tienda', true],
  ['promociones_texto', 'Texto de promociones', true], ['contacto_direccion', 'Dirección de contacto'], ['contacto_telefono', 'Teléfono de contacto'], ['contacto_correo', 'Correo de contacto'],
];

export default function AdminConfig() {
  const [v, setV] = useState(null);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');

  useEffect(() => { api.get('/api/configuracion/publica').then((r) => setV(r.data)).catch((e) => setError(mensajeError(e))); }, []);

  async function guardar(e) {
    e.preventDefault();
    setError(''); setMsg('');
    const valores = Object.fromEntries(CAMPOS.map(([k]) => [k, v[k] ?? '']));
    try { await api.put('/api/admin/configuracion', { valores }); setMsg('Configuración guardada.'); } catch (err) { setError(mensajeError(err)); }
  }

  return (
    <>
      <h1>Datos de la tienda</h1>
      <p className="nota">Estos textos aparecen en la página pública. No inventes datos de contacto: déjalos vacíos si aún no los tienes.</p>
      {v && (
        <form className="form" onSubmit={guardar}>
          {CAMPOS.map(([k, t, largo]) => (
            <Campo key={k} etiqueta={t}>
              {largo ? <textarea rows={3} maxLength={2000} value={v[k] || ''} onChange={(e) => setV({ ...v, [k]: e.target.value })} /> : <input maxLength={2000} value={v[k] || ''} onChange={(e) => setV({ ...v, [k]: e.target.value })} />}
            </Campo>
          ))}
          {error && <p className="error" role="alert">{error}</p>}
          {msg && <p className="ok" role="status">{msg}</p>}
          <button className="btn">Guardar</button>
        </form>
      )}
      {!v && error && <p className="error">{error}</p>}
    </>
  );
}
