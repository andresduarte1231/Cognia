import { Link, useNavigate } from 'react-router-dom';
import { moneda } from '../api.js';
import { useCart } from '../cart.jsx';

export default function Carrito() {
  const { items, total, cambiar, quitar, vaciar } = useCart();
  const nav = useNavigate();
  if (items.length === 0) {
    return <div className="container estado"><h1>Tu carrito está vacío</h1><Link to="/productos" className="btn">Ver productos</Link></div>;
  }
  return (
    <div className="container">
      <h1>Carrito de compras</h1>
      <div className="tabla-wrap">
        <table className="tabla">
          <thead><tr><th>Producto</th><th>Precio</th><th>Cantidad</th><th>Subtotal</th><th /></tr></thead>
          <tbody>
            {items.map((i) => (
              <tr key={i.id}>
                <td><Link to={`/productos/${i.id}`}>{i.nombre}</Link></td>
                <td>{moneda(i.precio)}</td>
                <td><input type="number" aria-label={`Cantidad de ${i.nombre}`} min="1" max={Math.min(100, i.existencias ?? 100)} value={i.cantidad} onChange={(e) => cambiar(i.id, Number(e.target.value) || 1)} /></td>
                <td>{moneda(i.precio * i.cantidad)}</td>
                <td><button className="btn sec" onClick={() => quitar(i.id)}>Quitar</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="total">Subtotal: <b>{moneda(total)}</b></p>
      <p className="nota">El pedido se envía por WhatsApp. El costo del domicilio se acuerda por ese chat.</p>
      <div className="fila">
        <button className="btn sec" onClick={vaciar}>Vaciar carrito</button>
        <button className="btn" onClick={() => nav('/checkout')}>Continuar con el pedido</button>
      </div>
    </div>
  );
}
