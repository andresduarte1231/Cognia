import { useEffect, useState } from 'react';
import { api, fecha, mensajeError, moneda } from '../../api.js';
import { useAuth } from '../../auth.jsx';
import { Estado, etiquetaEstado } from '../../components/ui.jsx';

export default function Dashboard() {
  const { usuario } = useAuth();
  const [d, setD] = useState(null);
  const [error, setError] = useState('');
  const puedeAuditar = usuario.rol === 'PROPIETARIO' || usuario.rol === 'ADMINISTRADOR';

  useEffect(() => {
    const pedidos = [
      api.get('/api/admin/reportes/resumen'),
      api.get('/api/admin/reportes/mas-vendidos', { params: { limite: 5 } }),
      api.get('/api/admin/inventario/resumen'),
      puedeAuditar ? api.get('/api/admin/auditoria', { params: { tamano: 8 } }) : Promise.resolve({ data: { contenido: [] } }),
    ];
    Promise.all(pedidos)
      .then(([r, mv, inv, aud]) => setD({ r: r.data, mv: mv.data, inv: inv.data, aud: aud.data.contenido }))
      .catch((e) => setError(mensajeError(e)));
  }, [puedeAuditar]);

  return (
    <>
      <h1>Resumen</h1>
      <Estado cargando={!d && !error} error={error}>
        {d && (
          <>
            <div className="kpis">
              <Kpi t="Productos activos" v={d.inv.productosActivos} />
              <Kpi t="Categorías" v={d.inv.categorias} />
              <Kpi t="Existencias bajas" v={d.inv.existenciasBajas} alerta={d.inv.existenciasBajas > 0} />
              <Kpi t="Productos agotados" v={d.inv.productosAgotados} alerta={d.inv.productosAgotados > 0} />
              <Kpi t="Pedidos pendientes" v={d.r.pedidosPorEstado.PENDIENTE ?? 0} />
              <Kpi t="En preparación" v={d.r.pedidosPorEstado.EN_PREPARACION ?? 0} />
              <Kpi t="Entregados" v={d.r.pedidosPorEstado.ENTREGADO ?? 0} />
              <Kpi t="Ventas confirmadas" v={moneda(d.r.ventasConfirmadas)} sub={`${d.r.cantidadVentas} ventas`} />
            </div>
            <h2>Productos más vendidos</h2>
            {d.mv.length === 0 ? <p className="nota">Sin ventas confirmadas todavía.</p> : (
              <div className="tabla-wrap"><table className="tabla"><thead><tr><th>Producto</th><th>Unidades</th><th>Ingresos</th></tr></thead>
                <tbody>{d.mv.map((p) => <tr key={p.productoId}><td>{p.nombre}</td><td>{p.unidades}</td><td>{moneda(p.ingresos)}</td></tr>)}</tbody></table></div>
            )}
            <h2>Pagos simulados</h2>
            <p>{Object.entries(d.r.pagosPorEstado).map(([k, v]) => `${etiquetaEstado(k)}: ${v}`).join(' · ') || 'Sin pagos'}</p>
            {puedeAuditar && (
              <>
                <h2>Actividad reciente</h2>
                <div className="tabla-wrap"><table className="tabla"><thead><tr><th>Fecha</th><th>Acción</th><th>Recurso</th><th>Resultado</th></tr></thead>
                  <tbody>{d.aud.map((a) => <tr key={a.id}><td>{fecha(a.creadoEn)}</td><td>{a.accion}</td><td>{a.recurso} {a.recursoId}</td><td>{a.resultado}</td></tr>)}</tbody></table></div>
              </>
            )}
          </>
        )}
      </Estado>
    </>
  );
}

const Kpi = ({ t, v, sub, alerta }) => (
  <div className={`kpi ${alerta ? 'alerta' : ''}`}><span>{t}</span><b>{v}</b>{sub && <small>{sub}</small>}</div>
);
