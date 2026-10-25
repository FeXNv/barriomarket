'use strict';

// ---------- utilidades ----------
function el(tag, props = {}, ...hijos) {
  const e = document.createElement(tag);
  for (const [k, v] of Object.entries(props)) {
    if (k === 'class') e.className = v;
    else if (k === 'text') e.textContent = v;
    else if (k.startsWith('on')) e.addEventListener(k.slice(2), v);
    else e.setAttribute(k, v);
  }
  for (const h of hijos) if (h != null) e.append(h);   // append(texto) crea nodo de texto: seguro
  return e;
}
const $ = (id) => document.getElementById(id);
const moneda = (n) => 'S/ ' + Number(n).toFixed(2);
const cantidad = (n) => Number(n).toLocaleString('es-PE', { maximumFractionDigits: 3 });
const ETIQUETA_ESTADO = { verde: 'Disponible', amarillo: 'Poco stock', rojo: 'Agotado' };
const PASO_STOCK = { unidad: 1, kg: 0.5, litro: 0.5 };   // lo que suman/restan los botones − y +

let productos = [];
let categorias = [];
let editando = null;   // producto en edición, o null si es nuevo

// ---------- pantallas ----------
function mostrarAuth() {
  $('pantalla-app').classList.add('oculto');
  $('pantalla-auth').classList.remove('oculto');
}
function mostrarApp() {
  const s = Api.sesion();
  $('usuario-nombre').textContent = s ? s.nombre : '';
  $('pantalla-auth').classList.add('oculto');
  $('pantalla-app').classList.remove('oculto');
  cambiarVista('inventario');
}

const cargadores = {
  inventario: cargarInventario,
  caja: cargarCaja,
  alertas: cargarAlertas,
  categorias: cargarCategorias,
  reportes: cargarReportes,
};

function cambiarVista(nombre) {
  document.querySelectorAll('.nav').forEach((b) => b.classList.toggle('activa', b.dataset.vista === nombre));
  document.querySelectorAll('.vista').forEach((v) => v.classList.toggle('oculto', v.id !== 'vista-' + nombre));
  if (cargadores[nombre]) cargadores[nombre]();
  refrescarContadorAlertas();
}



// ---------- autenticación ----------
document.querySelectorAll('.tab').forEach((t) => t.addEventListener('click', () => {
  document.querySelectorAll('.tab').forEach((x) => x.classList.toggle('activa', x === t));
  $('form-login').classList.toggle('oculto', t.dataset.auth !== 'login');
  $('form-registro').classList.toggle('oculto', t.dataset.auth !== 'registro');
}));

async function enviarAuth(evento, ruta) {
  evento.preventDefault();
  const form = evento.target;
  const error = form.querySelector('.error');
  error.textContent = '';
  const datos = Object.fromEntries(new FormData(form));
  if (!datos.email) delete datos.email;
  try {
    const r = await Api.post(ruta, datos);
    Api.guardar({ token: r.token, idUsuario: r.idUsuario, nombre: r.nombre });
    form.reset();
    mostrarApp();
  } catch (e) {
    error.textContent = e.message;
  }
}
$('form-login').addEventListener('submit', (e) => enviarAuth(e, '/api/auth/login'));
$('form-registro').addEventListener('submit', (e) => enviarAuth(e, '/api/auth/register'));

$('btn-salir').addEventListener('click', () => { Api.limpiar(); mostrarAuth(); });
Api.alExpirar(mostrarAuth);

document.querySelectorAll('.nav').forEach((b) => b.addEventListener('click', () => cambiarVista(b.dataset.vista)));

// ---------- inventario ----------
async function cargarInventario() {
  $('msg-inventario').textContent = '';
  try {
    [productos, categorias] = await Promise.all([Api.get('/api/productos'), Api.get('/api/categorias')]);
    renderInventario();
  } catch (e) {
    $('msg-inventario').textContent = e.message;
  }
}

function nombreCategoria(id) {
  const c = categorias.find((x) => x.id === id);
  return c ? c.nombre : '—';
}

function renderInventario() {
  const filtro = $('buscar').value.trim().toLowerCase();
  const lista = productos.filter((p) => p.nombre.toLowerCase().includes(filtro));
  const cuerpo = $('tbody-productos');
  cuerpo.replaceChildren();

  $('vacio-inventario').classList.toggle('oculto', productos.length > 0);

  for (const p of lista) {
    const paso = PASO_STOCK[p.unidadMedida] || 1;
    cuerpo.append(el('tr', {},
      el('td', { text: p.nombre }),
      el('td', { text: nombreCategoria(p.idCategoria) }),
      el('td', { class: 'num', text: moneda(p.precio) }),
      el('td', { class: 'num', text: cantidad(p.stock) + ' ' + p.unidadMedida }),
      el('td', {}, el('span', { class: 'badge ' + p.estado, text: ETIQUETA_ESTADO[p.estado] || p.estado })),
      el('td', {}, el('div', { class: 'acciones' },
        el('button', { class: 'btn chico', type: 'button', 'aria-label': 'Restar ' + p.nombre, text: '−',
          onclick: () => ajustar(p, 'merma', paso) }),
        el('button', { class: 'btn chico', type: 'button', 'aria-label': 'Sumar ' + p.nombre, text: '+',
          onclick: () => ajustar(p, 'reposicion', paso) }),
        el('button', { class: 'btn chico', type: 'button', text: 'Editar', onclick: () => abrirFormulario(p) }),
        el('button', { class: 'btn chico peligro', type: 'button', text: 'Eliminar', onclick: () => eliminar(p) }),
      )),
    ));
  }
}
$('buscar').addEventListener('input', renderInventario);

async function ajustar(p, tipo, cant) {
  $('msg-inventario').textContent = '';
  try {
    const actualizado = await Api.patch('/api/productos/' + p.id + '/stock', { tipo, cantidad: cant });
    productos = productos.map((x) => (x.id === p.id ? actualizado : x));
    renderInventario();
  } catch (e) {
    $('msg-inventario').textContent = e.message;
  }
}

async function eliminar(p) {
  if (!confirm('¿Eliminar "' + p.nombre + '"?')) return;
  $('msg-inventario').textContent = '';
  try {
    await Api.del('/api/productos/' + p.id);
    productos = productos.filter((x) => x.id !== p.id);
    renderInventario();
  } catch (e) {
    $('msg-inventario').textContent = e.message;
  }
}

// ---------- formulario de producto ----------
const dialogo = $('dlg-producto');
const formProducto = $('form-producto');

function abrirFormulario(p) {
  editando = p || null;
  $('titulo-producto').textContent = p ? 'Editar producto' : 'Nuevo producto';
  formProducto.querySelector('.error').textContent = '';

  const select = formProducto.elements.idCategoria;
  select.replaceChildren(...categorias.map((c) => el('option', { value: String(c.id), text: c.nombre })));

  formProducto.reset();
  if (p) {
    formProducto.elements.nombre.value = p.nombre;
    select.value = String(p.idCategoria);
    formProducto.elements.precio.value = p.precio;
    formProducto.elements.unidadMedida.value = p.unidadMedida;
    formProducto.elements.stock.value = p.stock;
    formProducto.elements.stockMinimo.value = p.stockMinimo ?? '';
  }
  dialogo.showModal();
}
$('btn-nuevo').addEventListener('click', () => abrirFormulario(null));
$('btn-cancelar').addEventListener('click', () => dialogo.close());

formProducto.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const f = formProducto.elements;
  const error = formProducto.querySelector('.error');
  error.textContent = '';

  const cuerpo = {
    nombre: f.nombre.value.trim(),
    idCategoria: Number(f.idCategoria.value),
    precio: Number(f.precio.value),
    stock: Number(f.stock.value),
    unidadMedida: f.unidadMedida.value,
  };
  if (f.stockMinimo.value !== '') cuerpo.stockMinimo = Number(f.stockMinimo.value);

  if (cuerpo.unidadMedida === 'unidad' && !Number.isInteger(cuerpo.stock)) {
    error.textContent = 'Los productos por unidad solo admiten stock entero';
    return;
  }

  try {
    const guardado = editando
      ? await Api.put('/api/productos/' + editando.id, cuerpo)
      : await Api.post('/api/productos', cuerpo);
    productos = editando
      ? productos.map((x) => (x.id === guardado.id ? guardado : x))
      : [...productos, guardado].sort((a, b) => a.nombre.localeCompare(b.nombre));
    dialogo.close();
    renderInventario();
  } catch (e) {
    error.textContent = e.message;
  }
});
// ---------- Modo Caja ----------
let carrito = [];   // cada línea: { producto, cantidad, precio }
const FRACCIONES = [['1', 1], ['½', 0.5], ['¼', 0.25], ['⅛', 0.125]];
const redondear3 = (n) => Math.round(n * 1000) / 1000;
const subtotal = (l) => Math.round(l.cantidad * l.precio * 100) / 100;

function mensaje(id, texto, ok = false) {
  const e = $(id);
  e.textContent = texto;
  e.classList.toggle('ok', ok);
}

async function cargarCaja() {
  mensaje('msg-caja', '');
  try {
    productos = await Api.get('/api/productos');
    // refresca las líneas con el stock actual y descarta productos que ya no existen
    carrito = carrito
      .map((l) => ({ ...l, producto: productos.find((p) => p.id === l.producto.id) }))
      .filter((l) => l.producto);
    renderGrid();
    renderCarrito();
  } catch (e) {
    mensaje('msg-caja', e.message);
  }
}

function renderGrid() {
  const filtro = $('buscar-caja').value.trim().toLowerCase();
  const grid = $('grid-caja');
  grid.replaceChildren();

  for (const p of productos.filter((x) => x.nombre.toLowerCase().includes(filtro))) {
    const agotado = Number(p.stock) <= 0;
    const bloqueo = agotado ? { disabled: '' } : {};
    const botones = p.unidadMedida === 'unidad'
      ? [el('button', { class: 'btn chico', type: 'button', text: '+1',
          'aria-label': 'Agregar 1 ' + p.nombre, ...bloqueo, onclick: () => agregar(p, 1) })]
      : FRACCIONES.map(([txt, val]) => el('button', { class: 'btn chico', type: 'button', text: txt,
          'aria-label': 'Agregar ' + txt + ' ' + p.unidadMedida + ' de ' + p.nombre,
          ...bloqueo, onclick: () => agregar(p, val) }));

    grid.append(el('div', { class: 'tile' + (agotado ? ' agotada' : '') },
      el('strong', { text: p.nombre }),
      el('span', { class: 'sub', text: moneda(p.precio) + ' / ' + p.unidadMedida }),
      el('span', { class: 'badge ' + p.estado, text: cantidad(p.stock) + ' ' + p.unidadMedida }),
      el('div', { class: 'acciones' }, ...botones),
    ));
  }
}
$('buscar-caja').addEventListener('input', renderGrid);

function agregar(p, cant) {
  mensaje('msg-caja', '');
  const linea = carrito.find((l) => l.producto.id === p.id);
  const nueva = redondear3((linea ? linea.cantidad : 0) + cant);
  if (nueva > Number(p.stock)) {
    mensaje('msg-caja', 'No hay suficiente stock de ' + p.nombre);
    return;
  }
  if (linea) linea.cantidad = nueva;
  else carrito.push({ producto: p, cantidad: cant, precio: Number(p.precio) });
  renderCarrito();
}

function renderCarrito() {
  const cont = $('lineas-carrito');
  cont.replaceChildren();
  $('carrito-vacio').classList.toggle('oculto', carrito.length > 0);

  for (const l of carrito) {
    const esUnidad = l.producto.unidadMedida === 'unidad';
    const inCant = el('input', { type: 'number', min: '0.001', step: esUnidad ? '1' : '0.001',
      value: String(l.cantidad), 'aria-label': 'Cantidad de ' + l.producto.nombre });
    const inPrecio = el('input', { type: 'number', min: '0.01', step: '0.01',
      value: l.precio.toFixed(2), 'aria-label': 'Precio de ' + l.producto.nombre });
    const sub = el('strong', { text: moneda(subtotal(l)) });

    const refrescar = () => { sub.textContent = moneda(subtotal(l)); actualizarTotales(); };
    inCant.addEventListener('input', () => { l.cantidad = Number(inCant.value) || 0; refrescar(); });
    inPrecio.addEventListener('input', () => { l.precio = Number(inPrecio.value) || 0; refrescar(); });

    cont.append(el('div', { class: 'linea' },
      el('div', { class: 'linea-nombre' },
        el('strong', { text: l.producto.nombre }),
        el('span', { class: 'sub', text: l.producto.unidadMedida })),
      inCant, inPrecio, sub,
      el('button', { class: 'btn chico peligro', type: 'button', text: '✕',
        'aria-label': 'Quitar ' + l.producto.nombre,
        onclick: () => { carrito = carrito.filter((x) => x !== l); renderCarrito(); } }),
    ));
  }
  actualizarTotales();
}

function totalCarrito() {
  return Math.round(carrito.reduce((s, l) => s + subtotal(l), 0) * 100) / 100;
}

function actualizarTotales() {
  const total = totalCarrito();
  $('total-venta').textContent = moneda(total);
  const paga = Number($('paga-con').value);
  if (!(paga > 0)) $('vuelto').textContent = '—';
  else if (paga >= total) $('vuelto').textContent = moneda(paga - total);
  else $('vuelto').textContent = 'Falta ' + moneda(total - paga);
}
$('paga-con').addEventListener('input', actualizarTotales);

$('btn-vaciar').addEventListener('click', () => {
  carrito = [];
  $('paga-con').value = '';
  mensaje('msg-caja', '');
  renderCarrito();
});

$('btn-cobrar').addEventListener('click', async () => {
  if (carrito.length === 0) { mensaje('msg-caja', 'Agrega productos a la venta'); return; }
  if (carrito.some((l) => !(l.cantidad > 0) || !(l.precio > 0))) {
    mensaje('msg-caja', 'Revisa cantidades y precios: deben ser mayores que cero');
    return;
  }

  const items = carrito.map((l) => {
    const item = { idProducto: l.producto.id, cantidad: l.cantidad };
    // el precio solo se envía si el bodeguero lo cambió respecto al de lista
    if (Math.abs(l.precio - Number(l.producto.precio)) > 0.0001) item.precioUnitario = l.precio;
    return item;
  });

  const boton = $('btn-cobrar');
  boton.disabled = true;
  try {
    const venta = await Api.post('/api/ventas', { items });
    carrito = [];
    $('paga-con').value = '';
    productos = await Api.get('/api/productos');
    renderGrid();
    renderCarrito();
    mensaje('msg-caja', 'Venta registrada: ' + moneda(venta.total), true);
  } catch (e) {
    mensaje('msg-caja', e.message);
  } finally {
    boton.disabled = false;
  }
});


// ---------- Alertas ----------
async function refrescarContadorAlertas() {
  try {
    const lista = await Api.get('/api/recordatorios');
    $('nav-alertas').textContent = lista.length ? 'Alertas (' + lista.length + ')' : 'Alertas';
  } catch { /* si falla, se deja sin contador */ }
}

async function cargarAlertas() {
  mensaje('msg-alertas', '');
  try {
    renderAlertas(await Api.get('/api/recordatorios'));
  } catch (e) {
    mensaje('msg-alertas', e.message);
  }
}

function renderAlertas(lista) {
  const cont = $('lista-alertas');
  cont.replaceChildren();
  $('sin-alertas').classList.toggle('oculto', lista.length > 0);

  for (const a of lista) {
    const agotado = a.tipoAlerta === 'agotado';
    cont.append(el('div', { class: 'alerta ' + (agotado ? 'rojo' : 'amarillo') },
      el('div', {},
        el('strong', { text: agotado ? 'Agotado' : 'Poco stock' }),
        el('span', { text: ' · ' + a.mensaje }),
        el('div', { class: 'sub', text: new Date(a.fechaCreacion).toLocaleString('es-PE') })),
      el('button', { class: 'btn chico', type: 'button', text: 'Marcar como leído',
        onclick: async () => {
          try {
            await Api.patch('/api/recordatorios/' + a.id + '/leido');
            await cargarAlertas();
            await refrescarContadorAlertas();
          } catch (e) {
            mensaje('msg-alertas', e.message);
          }
        } }),
    ));
  }
}

// ---------- Categorías ----------
async function cargarCategorias() {
  mensaje('msg-categorias', '');
  try {
    categorias = await Api.get('/api/categorias');
    renderCategorias();
  } catch (e) {
    mensaje('msg-categorias', e.message);
  }
}

function renderCategorias() {
  const ul = $('lista-categorias');
  ul.replaceChildren();
  for (const c of categorias) {
    ul.append(el('li', { class: 'fila' },
      el('span', { text: c.nombre }),
      el('button', { class: 'btn chico peligro', type: 'button', text: 'Eliminar',
        'aria-label': 'Eliminar categoría ' + c.nombre, onclick: () => eliminarCategoria(c) }),
    ));
  }
}

async function eliminarCategoria(c) {
  if (!confirm('¿Eliminar la categoría "' + c.nombre + '"?')) return;
  mensaje('msg-categorias', '');
  try {
    await Api.del('/api/categorias/' + c.id);
    categorias = categorias.filter((x) => x.id !== c.id);
    renderCategorias();
  } catch (e) {
    mensaje('msg-categorias', e.message);
  }
}

$('form-categoria').addEventListener('submit', async (evento) => {
  evento.preventDefault();
  const form = evento.target;
  mensaje('msg-categorias', '');
  try {
    const nueva = await Api.post('/api/categorias', { nombre: form.elements.nombre.value });
    categorias = [...categorias, nueva].sort((a, b) => a.nombre.localeCompare(b.nombre));
    form.reset();
    renderCategorias();
  } catch (e) {
    mensaje('msg-categorias', e.message);
  }
});

// ---------- Reportes ----------
const hoyISO = () => new Date().toLocaleDateString('en-CA');   // AAAA-MM-DD en hora local
const rangoReporte = () =>
  '?desde=' + encodeURIComponent($('rep-desde').value) + '&hasta=' + encodeURIComponent($('rep-hasta').value);

async function cargarReportes() {
  if (!$('rep-hasta').value) {
    const hoy = new Date();
    $('rep-hasta').value = hoyISO();
    $('rep-desde').value = new Date(hoy.getFullYear(), hoy.getMonth(), 1).toLocaleDateString('en-CA');
  }
  await verReporte();
}

async function verReporte() {
  mensaje('msg-reportes', '');
  try {
    const filas = await Api.get('/api/reportes/ventas' + rangoReporte());
    const cuerpo = $('tbody-reporte');
    cuerpo.replaceChildren();
    let total = 0;
    for (const f of filas) {
      total += Number(f.subtotal);
      cuerpo.append(el('tr', {},
        el('td', { text: String(f.idVenta) }),
        el('td', { text: new Date(f.fecha).toLocaleString('es-PE') }),
        el('td', { text: f.producto }),
        el('td', { class: 'num', text: cantidad(f.cantidad) }),
        el('td', { class: 'num', text: moneda(f.precioUnitario) }),
        el('td', { class: 'num', text: moneda(f.subtotal) }),
      ));
    }
    $('resumen-reporte').textContent = filas.length ? filas.length + ' líneas de venta · Total' : 'Sin ventas en este rango';
    $('total-reporte').textContent = filas.length ? moneda(total) : '';
  } catch (e) {
    mensaje('msg-reportes', e.message);
  }
}
$('btn-ver-reporte').addEventListener('click', verReporte);

$('btn-excel').addEventListener('click', async () => {
  mensaje('msg-reportes', '');
  try {
    const archivo = await Api.blob('/api/reportes/ventas/excel' + rangoReporte());
    const url = URL.createObjectURL(archivo);
    const enlace = el('a', { href: url, download: 'ventas-' + $('rep-desde').value + '-a-' + $('rep-hasta').value + '.xlsx' });
    document.body.append(enlace);
    enlace.click();
    enlace.remove();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch (e) {
    mensaje('msg-reportes', e.message);
  }
});

// ---------- arranque ----------
if (Api.sesion()) mostrarApp(); else mostrarAuth();