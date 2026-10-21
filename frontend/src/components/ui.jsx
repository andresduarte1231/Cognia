import { Link } from 'react-router-dom';
import { moneda } from '../api.js';
import { useCart } from '../cart.jsx';

export function Estado({ cargando, error, vacio, children }) {
  if (cargando) return <p className="estado" role="status">Cargando…</p>;
  if (error) return <p className="estado error" role="alert">{error}</p>;
  if (vacio) return <p className="estado">{vacio}</p>;
  return children;
}

export function Paginador({ pagina, totalPaginas, onChange }) {
  if (!totalPaginas || totalPaginas <= 1) return null;
  return (
    <nav className="paginador" aria-label="Paginación">
      <button className="btn sec" disabled={pagina <= 0} onClick={() => onChange(pagina - 1)}>Anterior</button>
      <span>Página {pagina + 1} de {totalPaginas}</span>
      <button className="btn sec" disabled={pagina + 1 >= totalPaginas} onClick={() => onChange(pagina + 1)}>Siguiente</button>
    </nav>
  );
}

export function ProductoCard({ p }) {
  const { agregar } = useCart();
  const agotado = !p.disponible;
  return (
    <article className="card producto">
      <Link to={`/productos/${p.id}`} className="img-wrap">
        <img src={p.imagenUrl || '/placeholder.svg'} alt={p.nombre} loading="lazy" onError={(e) => { e.currentTarget.src = '/placeholder.svg'; }} />
        {p.enPromocion && <span className="badge promo">Promoción</span>}
        {agotado && <span className="badge agotado">Agotado</span>}
      </Link>
      <div className="card-body">
        <small>{p.categoriaNombre}</small>
        <h3><Link to={`/productos/${p.id}`}>{p.nombre}</Link></h3>
        <p className="precio">
          {moneda(p.precio)}
          {p.enPromocion && p.precioAnterior && <s>{moneda(p.precioAnterior)}</s>}
        </p>
        <button className="btn" disabled={agotado} onClick={() => agregar(p)}>{agotado ? 'No disponible' : 'Agregar al carrito'}</button>
      </div>
    </article>
  );
}

export function Campo({ etiqueta, children }) {
  return (
    <label className="campo">
      <span>{etiqueta}</span>
      {children}
    </label>
  );
}

export const ESTADOS_PEDIDO = ['PENDIENTE', 'CONFIRMADO', 'EN_PREPARACION', 'LISTO_PARA_RECOGIDA', 'EN_CAMINO', 'ENTREGADO', 'CANCELADO'];
export const etiquetaEstado = (e) => (e || '').replaceAll('_', ' ').toLowerCase().replace(/^./, (c) => c.toUpperCase());
export const Insignia = ({ estado }) => <span className={`estado-pill e-${estado}`}>{etiquetaEstado(estado)}</span>;
