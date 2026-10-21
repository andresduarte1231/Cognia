import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { api, getToken, setToken, setUnauthorizedHandler } from './api.js';

const AuthCtx = createContext(null);
export const useAuth = () => useContext(AuthCtx);

export const ROLES_ADMIN = ['PROPIETARIO', 'ADMINISTRADOR', 'EMPLEADO_VENTAS'];

export function AuthProvider({ children }) {
  const [usuario, setUsuario] = useState(null);
  const [cargando, setCargando] = useState(!!getToken());

  const salir = useCallback(() => {
    setToken(null);
    setUsuario(null);
  }, []);

  useEffect(() => {
    setUnauthorizedHandler(salir);
    if (!getToken()) return;
    api.get('/api/auth/me').then((r) => setUsuario(r.data)).catch(salir).finally(() => setCargando(false));
  }, [salir]);

  const login = useCallback(async (correo, password) => {
    const { data } = await api.post('/api/auth/login', { correo, password });
    setToken(data.token);
    setUsuario(data.usuario);
    return data.usuario;
  }, []);

  const logout = useCallback(async () => {
    try {
      await api.post('/api/auth/logout');
    } catch {
      /* el cierre local siempre se realiza */
    }
    salir();
  }, [salir]);

  const value = useMemo(
    () => ({ usuario, cargando, login, logout, setUsuario, esAdmin: !!usuario && ROLES_ADMIN.includes(usuario.rol) }),
    [usuario, cargando, login, logout],
  );
  return <AuthCtx.Provider value={value}>{children}</AuthCtx.Provider>;
}
