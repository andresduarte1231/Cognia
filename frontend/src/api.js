import axios from 'axios';

const TOKEN_KEY = 'cognia_token';

// Mismo origen: el gateway sirve el frontend y las API. VITE_API_URL permite apuntar a otro gateway.
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '', timeout: 20000 });

// El token vive solo en sessionStorage (se borra al cerrar la pestaña).
export const getToken = () => sessionStorage.getItem(TOKEN_KEY);
export const setToken = (t) => (t ? sessionStorage.setItem(TOKEN_KEY, t) : sessionStorage.removeItem(TOKEN_KEY));

api.interceptors.request.use((cfg) => {
  const t = getToken();
  if (t) cfg.headers.Authorization = `Bearer ${t}`;
  return cfg;
});

let onUnauthorized = () => {};
export const setUnauthorizedHandler = (fn) => (onUnauthorized = fn);

api.interceptors.response.use(
  (r) => r,
  (e) => {
    if (e.response?.status === 401 && getToken() && !e.config?.url?.includes('/api/auth/login')) onUnauthorized();
    return Promise.reject(e);
  },
);

export function mensajeError(e) {
  const d = e?.response?.data;
  if (d?.detalles && Array.isArray(d.detalles) && d.detalles.length) return d.detalles.join('. ');
  if (d?.mensaje) return d.mensaje;
  if (e?.response?.status === 503) return 'El servicio no está disponible en este momento.';
  if (e?.code === 'ECONNABORTED') return 'La solicitud tardó demasiado. Intenta de nuevo.';
  if (!e?.response) return 'No fue posible conectar con el servidor.';
  return 'Ocurrió un error inesperado.';
}

export const moneda = (v) =>
  new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(Number(v || 0));

export const fecha = (iso) => (iso ? new Date(iso).toLocaleString('es-CO', { dateStyle: 'medium', timeStyle: 'short' }) : '');

export async function descargarCsv(url, nombre, params) {
  const r = await api.get(url, { params, responseType: 'blob' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(r.data);
  a.download = nombre;
  a.click();
  URL.revokeObjectURL(a.href);
}
