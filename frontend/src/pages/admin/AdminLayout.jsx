import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '../../auth.jsx';

export default function AdminLayout() {
  const { usuario } = useAuth();
  const r = usuario.rol;
  const catalogo = r === 'PROPIETARIO' || r === 'ADMINISTRADOR';
  const items = [
    ['/admin', 'Resumen', true, true],
    ['/admin/pedidos', 'Pedidos', true],
    ['/admin/productos', 'Productos', true],
    ['/admin/categorias', 'Categorías', catalogo],
    ['/admin/inventario', 'Inventario', true],
    ['/admin/usuarios', 'Usuarios', catalogo],
    ['/admin/reportes', 'Reportes', true],
    ['/admin/configuracion', 'Tienda', catalogo],
  ].filter((i) => i[2]);
  return (
    <div className="container admin">
      <aside className="admin-nav" aria-label="Administración">
        <p><small>{usuario.nombre} · {r}</small></p>
        {items.map(([to, t, , end]) => <NavLink key={to} to={to} end={!!end}>{t}</NavLink>)}
      </aside>
      <section className="admin-main"><Outlet /></section>
    </div>
  );
}
