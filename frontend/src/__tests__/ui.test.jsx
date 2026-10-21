import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CartProvider } from '../cart.jsx';
import { ProductoCard } from '../components/ui.jsx';
import { mensajeError, moneda } from '../api.js';

const producto = { id: 7, nombre: 'Panela', categoriaNombre: 'Despensa', precio: 3000, precioAnterior: 3500, enPromocion: true, existencias: 4, disponible: true, imagenUrl: null };
const envolver = (ui) => render(<MemoryRouter><CartProvider>{ui}</CartProvider></MemoryRouter>);

describe('ProductoCard', () => {
  beforeEach(() => localStorage.clear());

  it('muestra precio, promocion y permite agregar al carrito', async () => {
    envolver(<ProductoCard p={producto} />);
    expect(screen.getByText('Panela')).toBeInTheDocument();
    expect(screen.getByText('Promoción')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: /agregar al carrito/i }));
    expect(JSON.parse(localStorage.getItem('cognia_carrito'))[0].id).toBe(7);
  });

  it('deshabilita productos agotados', () => {
    envolver(<ProductoCard p={{ ...producto, disponible: false, existencias: 0 }} />);
    expect(screen.getByRole('button', { name: /no disponible/i })).toBeDisabled();
    expect(screen.getByText('Agotado')).toBeInTheDocument();
  });
});

describe('utilidades', () => {
  it('formatea moneda colombiana', () => {
    expect(moneda(12500)).toMatch(/12\.500/);
  });
  it('traduce errores del backend sin exponer detalles internos', () => {
    expect(mensajeError({ response: { status: 409, data: { mensaje: 'Sin existencias' } } })).toBe('Sin existencias');
    expect(mensajeError({ response: { status: 503, data: {} } })).toMatch(/no está disponible/);
    expect(mensajeError({})).toMatch(/conectar/);
    vi.restoreAllMocks();
  });
});