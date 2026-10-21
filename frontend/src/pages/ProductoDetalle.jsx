import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api, mensajeError, moneda } from '../api.js';
import { useCart } from '../cart.jsx';
import { Estado } from '../components/ui.jsx';

export default function ProductoDetalle() {
  const { id } = useParams();
  const { agregar } = useCart();
  const [p, setP] = useState(null);
  const [cant, setCant] = useState(1);
  const [error, setError] = useState('');
  const [ok, setOk] = useState(false);

  useEffect(() => {
    setP(null); setError('');
    api.get(`/api/productos/${id}`).then((r) => setP(r.data)).catch((e) => setError(e.response?.status === 404 ? 'Producto no encontrado.' : mensajeError(e)));
  }, [id]);

  return (
    <div className="container">
      <p><Link to="/productos">← Volver al catálogo</Link></p>
      <Estado cargando={!p && !error} error={error}>
        {p && (
          <div className="detalle">
            <img src={p.imagenUrl || '/placeholder.svg'} alt={p.nombre} onError={(e) => { e.currentTarget.src = '/placeholder.svg'; }} />
            <div>
              <small>{p.categoriaNombre}</small>
              <h1>{p.nombre}</h1>
              <p className="precio grande">{moneda(p.precio)} {p.enPromocion && p.precioAnterior && <s>{moneda(p.precioAnterior)}</s>}</p>
              <p>{p.descripcion || 'Sin descripción.'}</p>
              <p>{p.disponible ? <b className="ok">Disponible ({p.existencias} unidades)</b> : <b className="error">Agotado</b>}</p>
              {p.disponible && (
                <div className="fila">
                  <input type="number" aria-label="Cantidad" min="1" max={Math.min(100, p.existencias)} value={cant}
                    onChange={(e) => setCant(Math.max(1, Math.min(Number(e.target.value) || 1, p.existencias, 100)))} />
                  <button className="btn" onClick={() => { agregar(p, cant); setOk(true); }}>Agregar al carrito</button>
                </div>
              )}
              {ok && <p className="ok" role="status">Agregado. <Link to="/carrito">Ir al carrito</Link></p>}
            </div>
          </div>
        )}
      </Estado>
    </div>
  );
}
