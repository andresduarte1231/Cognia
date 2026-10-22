import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api, mensajeError } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Campo } from '../components/ui.jsx';

export function Login() {
  const { login } = useAuth();
  const nav = useNavigate();
  const loc = useLocation();
  const [f, setF] = useState({ correo: '', password: '' });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function enviar(e) {
    e.preventDefault();
    setError(''); setEnviando(true);
    try {
      const u = await login(f.correo.trim(), f.password);
      nav(loc.state?.desde || (u.rol === 'CLIENTE' ? '/' : '/admin'), { replace: true });
    } catch (err) {
      setError(err.response?.status === 401 ? 'Correo o contraseña incorrectos.' : mensajeError(err));
    } finally { setEnviando(false); }
  }

  return (
    <div className="container estrecho">
      <h1>Iniciar sesión</h1>
      <form className="form" onSubmit={enviar}>
        <Campo etiqueta="Correo electrónico"><input type="email" required autoComplete="username" value={f.correo} onChange={(e) => setF({ ...f, correo: e.target.value })} /></Campo>
        <Campo etiqueta="Contraseña"><input type="password" required autoComplete="current-password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} /></Campo>
        {error && <p className="error" role="alert">{error}</p>}
        <button className="btn" disabled={enviando}>{enviando ? 'Ingresando…' : 'Ingresar'}</button>
      </form>
    </div>
  );
}

export function Registro() {
  const { login } = useAuth();
  const nav = useNavigate();
  const [f, setF] = useState({ nombre: '', correo: '', password: '', acepto: false });
  const [error, setError] = useState('');
  const [enviando, setEnviando] = useState(false);

  async function enviar(e) {
    e.preventDefault();
    setError(''); setEnviando(true);
    try {
      await api.post('/api/auth/registro', { nombre: f.nombre.trim(), correo: f.correo.trim(), password: f.password });
      await login(f.correo.trim(), f.password);
      nav('/', { replace: true });
    } catch (err) { setError(mensajeError(err)); } finally { setEnviando(false); }
  }

  return (
    <div className="container estrecho">
      <h1>Crear cuenta</h1>
      <form className="form" onSubmit={enviar}>
        <Campo etiqueta="Nombre completo"><input required maxLength={120} autoComplete="name" value={f.nombre} onChange={(e) => setF({ ...f, nombre: e.target.value })} /></Campo>
        <Campo etiqueta="Correo electrónico"><input type="email" required autoComplete="username" value={f.correo} onChange={(e) => setF({ ...f, correo: e.target.value })} /></Campo>
        <Campo etiqueta="Contraseña (mínimo 8 caracteres, con letras y números)"><input type="password" required minLength={8} maxLength={72} autoComplete="new-password" value={f.password} onChange={(e) => setF({ ...f, password: e.target.value })} /></Campo>
        <label className="check">
          <input type="checkbox" required checked={f.acepto} onChange={(e) => setF({ ...f, acepto: e.target.checked })} />
          <span>Autorizo el tratamiento de mis datos personales según la <Link to="/privacidad" target="_blank">política de privacidad</Link> (Ley 1581 de 2012).</span>
        </label>
        {error && <p className="error" role="alert">{error}</p>}
        <button className="btn" disabled={enviando}>{enviando ? 'Creando…' : 'Crear cuenta'}</button>
      </form>
      <p>¿Ya tienes cuenta? <Link to="/login">Inicia sesión</Link></p>
    </div>
  );
}
