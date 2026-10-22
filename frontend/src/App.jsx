import { Link, Route, Routes } from 'react-router-dom';
import { Layout, RequiereAdmin, RequiereSesion } from './components/Layout.jsx';
import Home from './pages/Home.jsx';
import Catalogo from './pages/Catalogo.jsx';
import ProductoDetalle from './pages/ProductoDetalle.jsx';
import Carrito from './pages/Carrito.jsx';
import { Login } from './pages/Acceso.jsx';
import Checkout from './pages/Checkout.jsx';
import { MisPedidos, PedidoDetalle } from './pages/Pedidos.jsx';
import { Legal } from './pages/Legal.jsx';
import AdminLayout from './pages/admin/AdminLayout.jsx';
import Dashboard from './pages/admin/Dashboard.jsx';
import AdminProductos from './pages/admin/Productos.jsx';
import AdminCategorias from './pages/admin/Categorias.jsx';
import AdminInventario from './pages/admin/Inventario.jsx';
import AdminPedidos from './pages/admin/Pedidos.jsx';
import AdminUsuarios from './pages/admin/Usuarios.jsx';
import AdminReportes from './pages/admin/Reportes.jsx';
import AdminConfig from './pages/admin/Configuracion.jsx';

const NoEncontrado = () => (
  <div className="container estado"><h1>Página no encontrada</h1><Link to="/">Volver al inicio</Link></div>
);

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Home />} />
        <Route path="productos" element={<Catalogo />} />
        <Route path="productos/:id" element={<ProductoDetalle />} />
        <Route path="carrito" element={<Carrito />} />
        <Route path="login" element={<Login />} />
        <Route path="privacidad" element={<Legal tipo="privacidad" />} />
        <Route path="terminos" element={<Legal tipo="terminos" />} />
        <Route path="checkout" element={<Checkout />} />
        <Route path="mis-pedidos" element={<RequiereSesion><MisPedidos /></RequiereSesion>} />
        <Route path="mis-pedidos/:id" element={<RequiereSesion><PedidoDetalle /></RequiereSesion>} />
        <Route path="admin" element={<RequiereAdmin><AdminLayout /></RequiereAdmin>}>
          <Route index element={<Dashboard />} />
          <Route path="productos" element={<AdminProductos />} />
          <Route path="categorias" element={<AdminCategorias />} />
          <Route path="inventario" element={<AdminInventario />} />
          <Route path="pedidos" element={<AdminPedidos />} />
          <Route path="usuarios" element={<AdminUsuarios />} />
          <Route path="reportes" element={<AdminReportes />} />
          <Route path="configuracion" element={<AdminConfig />} />
        </Route>
        <Route path="*" element={<NoEncontrado />} />
      </Route>
    </Routes>
  );
}
