import { describe, expect, it } from 'vitest';
import { reducer } from '../cart.jsx';

const p = { id: 1, nombre: 'Arroz', precio: '4500', existencias: 5, imagenUrl: null };

describe('carrito', () => {
  it('agrega un producto y acumula cantidades', () => {
    let s = reducer([], { type: 'agregar', producto: p, cantidad: 2 });
    s = reducer(s, { type: 'agregar', producto: p, cantidad: 1 });
    expect(s).toHaveLength(1);
    expect(s[0].cantidad).toBe(3);
    expect(s[0].precio).toBe(4500);
  });

  it('no supera las existencias', () => {
    let s = reducer([], { type: 'agregar', producto: p, cantidad: 10 });
    expect(s[0].cantidad).toBe(5);
    s = reducer(s, { type: 'cantidad', id: 1, cantidad: 99 });
    expect(s[0].cantidad).toBe(5);
  });

  it('cantidad minima 1, quitar y vaciar', () => {
    let s = reducer([], { type: 'agregar', producto: p });
    s = reducer(s, { type: 'cantidad', id: 1, cantidad: 0 });
    expect(s[0].cantidad).toBe(1);
    expect(reducer(s, { type: 'quitar', id: 1 })).toEqual([]);
    expect(reducer(s, { type: 'vaciar' })).toEqual([]);
  });
});