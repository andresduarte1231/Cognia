import { Link, NavLink, Outlet, Navigate, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth.jsx';
import { useCart } from '../cart.jsx';

export function Layout() {
  const { usuario, logout, esAdmin } = useAuth();
  const { unidades } = useCart();
  const nav = useNavigate();
  return (
    <>
      <a className="skip" href="#contenido">Saltar al contenido</a>
      <header className="header">
        <div className="container header-in">
          <Link to="/" className="logo"><img src="/mascota.png" alt="" width="52" height="52" /><span>Aquí es Toña</span></Link>
          <nav className="nav" aria-label="Principal">
            <NavLink to="/" end>Inicio</NavLink>
            <NavLink to="/productos">Productos</NavLink>
            <a href="/#nosotros">Nosotros</a>
            <a href="/#domicilios">Domicilios</a>
            <a href="/#faq">Preguntas</a>
            <a href="/#contacto">Contacto</a>
          </nav>
          <div className="acciones">
            <NavLink to="/carrito" className="btn sec">Carrito ({unidades})</NavLink>
            {usuario ? (
              <>
                {esAdmin && <NavLink to="/admin" className="btn sec">Panel</NavLink>}
                <NavLink to="/mis-pedidos" className="btn sec">Mis pedidos</NavLink>
                <button className="btn" onClick={async () => { await logout(); nav('/'); }}>Salir</button>
              </>
            ) : (
              <NavLink to="/login" className="btn">Ingresar</NavLink>
            )}
          </div>
        </div>
      </header>
      <main id="contenido"><Outlet /></main>
      <footer className="footer">
        <div className="container">
          <p><b>Aquí es Toña</b> · Proyecto académico COGNIA</p>
          <p><Link to="/privacidad">Política de privacidad</Link> · <Link to="/terminos">Términos y condiciones</Link></p>
          <small>Los pagos de esta tienda son simulados: no se solicitan datos bancarios ni de tarjetas.</small>
        </div>
      </footer>
    </>
  );
}

export function RequiereSesion({ children }) {
  const { usuario, cargando } = useAuth();
  if (cargando) return <p className="estado">Cargando…</p>;
  return usuario ? children : <Navigate to="/login" replace />;
}

export function RequiereAdmin({ children }) {
  const { usuario, cargando, esAdmin } = useAuth();
  if (cargando) return <p className="estado">Cargando…</p>;
  if (!usuario) return <Navigate to="/login" replace />;
  return esAdmin ? children : <Navigate to="/" replace />;
}
