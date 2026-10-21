import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api, mensajeError, moneda } from '../api.js';
import { Estado, ProductoCard } from '../components/ui.jsx';

const FAQ = [
  ['¿Cómo hago un pedido?', 'Agrega productos al carrito, inicia sesión, elige domicilio o recogida en tienda y confirma. El precio final lo calcula siempre el sistema.'],
  ['¿Cómo funciona el pago?', 'Los pagos de esta tienda son simulados (proyecto académico). No se piden datos de tarjetas ni bancarios.'],
  ['¿Puedo cancelar un pedido?', 'Sí, mientras el pedido esté pendiente o confirmado, desde «Mis pedidos». Las existencias reservadas se liberan.'],
  ['¿Qué pasa si un producto se agota?', 'Se marca como agotado y no puede agregarse al carrito; si se agota durante tu compra, el sistema te lo informará.'],
];

export default function Home() {
  const [cfg, setCfg] = useState({});
  const [cats, setCats] = useState([]);
  const [destacados, setDestacados] = useState([]);
  const [promos, setPromos] = useState([]);
  const [cargando, setCargando] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([
      api.get('/api/configuracion/publica'),
      api.get('/api/categorias'),
      api.get('/api/productos', { params: { tamano: 8, orden: 'nombre' } }),
      api.get('/api/productos', { params: { promocion: true, tamano: 4 } }),
    ])
      .then(([c, k, p, pr]) => {
        setCfg(c.data); setCats(k.data); setDestacados(p.data.contenido); setPromos(pr.data.contenido);
      })
      .catch((e) => setError(mensajeError(e)))
      .finally(() => setCargando(false));
  }, []);

  const valores = (cfg.valores || '').split(',').map((v) => v.trim()).filter(Boolean);
  const hayContacto = cfg.contacto_correo || cfg.contacto_telefono || cfg.contacto_direccion;

  return (
    <>
      <section className="hero">
        <div className="container hero-in">
          <div>
            <h1>{cfg.nombre_comercial || 'Aquí es Toña'}</h1>
            <p>Alimentos y productos de consumo para tu hogar, con domicilio o recogida en tienda.</p>
            <Link to="/productos" className="btn grande">Ver productos</Link>
          </div>
          <img className="hero-logo" src="/logo-completo.png" alt="Logo Aquí es Toña: abuela sonriente con una paleta" width="320" height="427" />
        </div>
      </section>

      <div className="container">
        <Estado cargando={cargando} error={error}>
          {cats.length > 0 && (
            <section aria-labelledby="h-cats">
              <h2 id="h-cats">Categorías</h2>
              <div className="chips">
                {cats.map((c) => <Link key={c.id} className="chip" to={`/productos?categoria=${c.id}`}>{c.nombre}</Link>)}
              </div>
            </section>
          )}

          {promos.length > 0 && (
            <section aria-labelledby="h-promos">
              <h2 id="h-promos">Promociones</h2>
              {cfg.promociones_texto && <p className="nota">{cfg.promociones_texto}</p>}
              <div className="grid">{promos.map((p) => <ProductoCard key={p.id} p={p} />)}</div>
            </section>
          )}

          <section aria-labelledby="h-dest">
            <h2 id="h-dest">Catálogo destacado</h2>
            {destacados.length === 0 ? <p className="estado">Aún no hay productos disponibles.</p> : <div className="grid">{destacados.map((p) => <ProductoCard key={p.id} p={p} />)}</div>}
            <p className="centro"><Link to="/productos" className="btn sec">Ver todo el catálogo</Link></p>
          </section>

          <section id="nosotros" aria-labelledby="h-nos">
            <h2 id="h-nos">Sobre nosotros</h2>
            <div className="cols">
              <div className="card pad"><h3>Misión</h3><p>{cfg.mision}</p></div>
              <div className="card pad"><h3>Visión</h3><p>{cfg.vision}</p></div>
              <div className="card pad"><h3>Valores</h3><ul>{valores.map((v) => <li key={v}>{v}</li>)}</ul></div>
            </div>
          </section>

          <section aria-labelledby="h-ben">
            <h2 id="h-ben">Beneficios</h2>
            <div className="cols">
              <div className="card pad"><h3>Precios claros</h3><p>Ves el precio y la disponibilidad real de cada producto.</p></div>
              <div className="card pad"><h3>Dos formas de recibir</h3><p>Domicilio o recogida en tienda, como prefieras.</p></div>
              <div className="card pad"><h3>Compra segura</h3><p>El sistema valida existencias y recalcula el total antes de confirmar.</p></div>
            </div>
          </section>

          <section id="domicilios" aria-labelledby="h-dom">
            <h2 id="h-dom">Domicilios y recogida en tienda</h2>
            <div className="cols">
              <div className="card pad">
                <h3>Domicilio</h3>
                <p>{cfg.info_domicilios}</p>
                {cfg.costo_domicilio && <p><b>Costo:</b> {moneda(cfg.costo_domicilio)}</p>}
              </div>
              <div className="card pad"><h3>Recogida en tienda</h3><p>{cfg.info_recogida}</p></div>
            </div>
          </section>

          <section id="faq" aria-labelledby="h-faq">
            <h2 id="h-faq">Preguntas frecuentes</h2>
            {FAQ.map(([q, a]) => (
              <details key={q} className="faq"><summary>{q}</summary><p>{a}</p></details>
            ))}
          </section>

          <section id="contacto" aria-labelledby="h-con">
            <h2 id="h-con">Contacto</h2>
            {hayContacto ? (
              <ul className="sinvineta">
                {cfg.contacto_direccion && <li>Dirección: {cfg.contacto_direccion}</li>}
                {cfg.contacto_telefono && <li>Teléfono: {cfg.contacto_telefono}</li>}
                {cfg.contacto_correo && <li>Correo: {cfg.contacto_correo}</li>}
              </ul>
            ) : (
              <p className="nota">Los datos de contacto aún no han sido configurados por el propietario.</p>
            )}
          </section>
        </Estado>
      </div>
    </>
  );
}
