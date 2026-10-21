import { createContext, useContext, useEffect, useMemo, useReducer } from 'react';

const CartCtx = createContext(null);
export const useCart = () => useContext(CartCtx);

const KEY = 'cognia_carrito';
const MAX_ITEM = 100;

export function reducer(state, a) {
  switch (a.type) {
    case 'agregar': {
      const p = a.producto;
      const ex = state.find((i) => i.id === p.id);
      const tope = Math.min(MAX_ITEM, p.existencias ?? MAX_ITEM);
      if (ex) return state.map((i) => (i.id === p.id ? { ...i, existencias: p.existencias, cantidad: Math.min(i.cantidad + (a.cantidad || 1), tope) } : i));
      return [...state, { id: p.id, nombre: p.nombre, precio: Number(p.precio), imagenUrl: p.imagenUrl, existencias: p.existencias, cantidad: Math.min(a.cantidad || 1, tope) }];
    }
    case 'cantidad':
      return state.map((i) => (i.id === a.id ? { ...i, cantidad: Math.max(1, Math.min(a.cantidad, MAX_ITEM, i.existencias ?? MAX_ITEM)) } : i));
    case 'quitar':
      return state.filter((i) => i.id !== a.id);
    case 'vaciar':
      return [];
    default:
      return state;
  }
}

function inicial() {
  try {
    const v = JSON.parse(localStorage.getItem(KEY) || '[]');
    return Array.isArray(v) ? v : [];
  } catch {
    return [];
  }
}

export function CartProvider({ children }) {
  const [items, dispatch] = useReducer(reducer, undefined, inicial);
  useEffect(() => localStorage.setItem(KEY, JSON.stringify(items)), [items]);
  // El total mostrado es informativo: el backend recalcula los precios al crear el pedido.
  const value = useMemo(() => {
    const total = items.reduce((s, i) => s + i.precio * i.cantidad, 0);
    const unidades = items.reduce((s, i) => s + i.cantidad, 0);
    return {
      items, total, unidades,
      agregar: (producto, cantidad = 1) => dispatch({ type: 'agregar', producto, cantidad }),
      cambiar: (id, cantidad) => dispatch({ type: 'cantidad', id, cantidad }),
      quitar: (id) => dispatch({ type: 'quitar', id }),
      vaciar: () => dispatch({ type: 'vaciar' }),
    };
  }, [items]);
  return <CartCtx.Provider value={value}>{children}</CartCtx.Provider>;
}
