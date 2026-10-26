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

// Icono según el nombre del producto (o la unidad si no hay coincidencia)
const ICONOS = [
  [/arroz|fideo|tallar|pasta|harina|avena|lenteja|frijol|menestra/i, '🍚'],
  [/leche|yogur|queso|mantequilla/i, '🥛'],
  [/pan|galleta|keke|torta/i, '🍞'],
  [/huevo/i, '🥚'],
  [/pollo|carne|pescado|jamón|jamon|salchicha|atún|atun/i, '🍗'],
  [/gaseosa|agua|jugo|cerveza|bebida|refresco|inca|coca/i, '🥤'],
  [/papa|cebolla|tomate|zanahoria|verdura|limón|limon|ajo/i, '🥔'],
  [/manzana|plátano|platano|naranja|fruta|uva|palta/i, '🍎'],
  [/azúcar|azucar|sal|aceite|vinagre|salsa|mayonesa|ketchup/i, '🧂'],
  [/jab[oó]n|detergente|lej[ií]a|papel|shampoo|limpi|desinfect/i, '🧼'],
  [/chocolate|caramelo|dulce|snack|chizito|papas fritas/i, '🍫'],
  [/caf[eé]|t[eé]\b|infusi/i, '☕'],
];
const ICONO_UNIDAD = { unidad: '📦', kg: '⚖️', litro: '🧴' };
function icono(p) {
  for (const [re, i] of ICONOS) if (re.test(p.nombre)) return i;
  return ICONO_UNIDAD[p.unidadMedida] || '📦';
}

// ---------- avisos y confirmación ----------
function toast(texto, tipo = 'ok') {
  const t = el('div', { class: 'toast ' + tipo, role: 'status', text: texto });
  $('toasts').append(t);
  setTimeout(() => t.remove(), 3200);
}

const dlgConfirmar = $('dlg-confirmar');
let resolverConfirmar = null;
function confirmar(titulo, texto, botonSi = 'Eliminar', peligroso = true) {
  $('confirmar-si').className = 'btn ' + (peligroso ? 'peligro-lleno' : 'primario');
  $('confirmar-titulo').textContent = titulo;
  $('confirmar-texto').textContent = texto;
  $('confirmar-si').textContent = botonSi;
  dlgConfirmar.showModal();
  return new Promise((resolver) => { resolverConfirmar = resolver; });
}
function cerrarConfirmar(valor) {
  if (dlgConfirmar.open) dlgConfirmar.close();
  if (resolverConfirmar) { resolverConfirmar(valor); resolverConfirmar = null; }
}
$('confirmar-si').addEventListener('click', () => cerrarConfirmar(true));
$('confirmar-no').addEventListener('click', () => cerrarConfirmar(false));
dlgConfirmar.addEventListener('cancel', () => cerrarConfirmar(false));   // tecla Esc

// ---------- modo oscuro ----------
function temaActual() { return document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light'; }
function pintarBotonesTema() {
  const oscuro = temaActual() === 'dark';
  document.querySelectorAll('.btn-tema').forEach((b) => {
    b.textContent = oscuro ? '☀️' : '🌙';
    b.title = oscuro ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro';
  });
}
document.querySelectorAll('.btn-tema').forEach((b) => b.addEventListener('click', () => {
  const nuevo = temaActual() === 'dark' ? 'light' : 'dark';
  document.documentElement.setAttribute('data-theme', nuevo);
  try { localStorage.setItem('bm_tema', nuevo); } catch { /* sin almacenamiento */ }
  pintarBotonesTema();
}));
pintarBotonesTema();

// ---------- Campos numéricos: solo dígitos, un punto y límites de longitud ----------
function limpiarDecimal(valor, maxEnteros, maxDecimales) {
  const v = valor.replace(/,/g, '.').replace(/[^0-9.]/g, '');
  const i = v.indexOf('.');
  const enteros = (i === -1 ? v : v.slice(0, i)).slice(0, maxEnteros);
  if (i === -1 || maxDecimales === 0) return enteros;
  const decimales = v.slice(i + 1).replace(/\./g, '').slice(0, maxDecimales);
  return enteros + '.' + decimales;
}

// Fase de captura: se limpia antes de que los demás manejadores lean el valor.
document.addEventListener('input', (e) => {
  const t = e.target;
  if (!(t instanceof HTMLInputElement) || !t.dataset.enteros) return;
  const limpio = limpiarDecimal(t.value, Number(t.dataset.enteros), Number(t.dataset.decimales || 0));
  if (limpio !== t.value) t.value = limpio;
}, true);

let productos = [];
let categorias = [];
let editando = null;          // producto en edición, o null si es nuevo
let filtroEstado = 'todos';   // todos | verde | amarillo | rojo
let filtroCategoria = 0;      // 0 = todas

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
  cambiarVista('inicio');
}

const cargadores = {
  inicio: cargarInicio,
  inventario: cargarInventario,
  caja: cargarCaja,
  alertas: cargarAlertas,
  categorias: cargarCategorias,
  reportes: cargarReportes,
};

function cambiarVista(nombre) {
  document.querySelectorAll('.nav').forEach((b) => b.classList.toggle('activa', b.dataset.vista === nombre));
  document.querySelectorAll('.vista').forEach((v) => v.classList.toggle('oculto', v.id !== 'vista-' + nombre));
  window.scrollTo(0, 0);
  if (cargadores[nombre]) cargadores[nombre]();
  refrescarContadorAlertas();
  actualizarBarraVenta();
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

$('btn-salir').addEventListener('click', () => { Api.limpiar(); carrito = []; mostrarAuth(); });
Api.alExpirar(mostrarAuth);

document.querySelectorAll('.nav').forEach((b) => b.addEventListener('click', () => cambiarVista(b.dataset.vista)));
document.querySelectorAll('[data-ir]').forEach((b) => b.addEventListener('click', () => cambiarVista(b.dataset.ir)));

// ---------- Resumen (inicio) ----------
async function cargarInicio() {
  $('msg-inicio').textContent = '';
  const s = Api.sesion();
  const hora = new Date().getHours();
  const saludo = hora < 12 ? 'Buenos días' : hora < 19 ? 'Buenas tardes' : 'Buenas noches';
  $('saludo').textContent = saludo + (s && s.nombre ? ', ' + s.nombre : '') + '. Así va tu bodega hoy.';
  try {
    const hoy = hoyISO();
    //lista de productos, ventas de hoy y últimos movimientos recientes (8) - 
    // FUNCION DE MAPEO NO BORRAR - | CUIDADO!!!!
    const [lista, ventas, movs] = await Promise.all([
      Api.get('/api/productos'),
      Api.get('/api/reportes/ventas?desde=' + hoy + '&hasta=' + hoy),
      Api.get('/api/historial?limite=8'),
    ]);
    productos = lista;
    renderInicio(ventas);
    renderActualizaciones(movs);
  } catch (e) {
    $('msg-inicio').textContent = e.message;
  }
}





function renderInicio(ventas) {
  const cuenta = { verde: 0, amarillo: 0, rojo: 0 };
  for (const p of productos) if (cuenta[p.estado] !== undefined) cuenta[p.estado]++;
  $('st-total').textContent = String(productos.length);
  $('st-verde').textContent = String(cuenta.verde);
  $('st-amarillo').textContent = String(cuenta.amarillo);
  $('st-rojo').textContent = String(cuenta.rojo);

  const total = ventas.reduce((s, f) => s + Number(f.subtotal), 0);
  const nVentas = new Set(ventas.map((f) => f.idVenta)).size;
  $('ventas-hoy').textContent = moneda(total);
  $('ventas-hoy-det').textContent = nVentas
    ? nVentas + (nVentas === 1 ? ' venta registrada hoy' : ' ventas registradas hoy')
    : 'Aún no hay ventas hoy';

  const criticos = productos
    .filter((p) => p.estado === 'rojo' || p.estado === 'amarillo')
    .sort((a, b) => Number(a.stock) - Number(b.stock))
    .slice(0, 6);
  const ul = $('lista-por-agotar');
  ul.replaceChildren();
  $('sin-por-agotar').classList.toggle('oculto', criticos.length > 0);
  for (const p of criticos) {
    ul.append(el('li', {},
      el('span', { text: icono(p) + ' ' + p.nombre }),
      el('span', { class: 'badge ' + p.estado, text: cantidad(p.stock) + ' ' + p.unidadMedida }),
    ));
  }
}

//constantes y funciones para el historial de movimientos, no BORRAR!!! , manejo de tiempos y renderizado de movimientos recientes
// cuidao , al borrar no se mapean los datos de historial 
const ICONO_MOV = { venta: '🛒', reposicion: '📥', ajuste: '✏️', merma: '📉' };
const TEXTO_MOV = { venta: 'Venta', reposicion: 'Reposición', ajuste: 'Ajuste', merma: 'Merma' };

function hace(fechaTexto) {
  const seg = Math.max(0, Math.round((Date.now() - new Date(fechaTexto).getTime()) / 1000));
  if (seg < 60) return 'hace un momento';
  const min = Math.floor(seg / 60);
  if (min < 60) return 'hace ' + min + ' min';
  const h = Math.floor(min / 60);
  if (h < 24) return 'hace ' + h + ' h';
  const d = Math.floor(h / 24);
  return 'hace ' + d + (d === 1 ? ' día' : ' días');
}

function renderActualizaciones(lista) {
  const ul = $('lista-actualizaciones');
  ul.replaceChildren();
  $('sin-actualizaciones').classList.toggle('oculto', lista.length > 0);
  for (const m of lista) {
    ul.append(el('li', {},
      el('span', { text: (ICONO_MOV[m.tipoMovimiento] || '•') + ' ' + m.producto + ' · ' + (TEXTO_MOV[m.tipoMovimiento] || m.tipoMovimiento) }),
      el('span', { class: 'sub', text: cantidad(m.stockAnterior) + ' → ' + cantidad(m.stockNuevo) + ' · ' + hace(m.fechaHora) })));
  }
}

document.querySelectorAll('.stat').forEach((b) => b.addEventListener('click', () => {
  filtroEstado = b.dataset.filtro;
  filtroCategoria = 0;
  $('buscar').value = '';
  cambiarVista('inventario');
}));

// ---------- inventario ----------
async function cargarInventario() {
  $('msg-inventario').textContent = '';
  try {
    [productos, categorias] = await Promise.all([Api.get('/api/productos'), Api.get('/api/categorias')]);
    renderChips();
    renderInventario();
  } catch (e) {
    $('msg-inventario').textContent = e.message;
  }
}

function nombreCategoria(id) {
  const c = categorias.find((x) => x.id === id);
  return c ? c.nombre : '—';
}

function renderChips() {
  document.querySelectorAll('#chips-estado .chip').forEach((c) =>
    c.classList.toggle('activa', c.dataset.filtro === filtroEstado));
  const cont = $('chips-categoria');
  cont.replaceChildren();
  cont.classList.toggle('oculto', categorias.length === 0);
  const crear = (id, texto) => el('button', {
    type: 'button', class: 'chip' + (filtroCategoria === id ? ' activa' : ''), text: texto,
    onclick: () => { filtroCategoria = id; renderChips(); renderInventario(); },
  });
  cont.append(crear(0, 'Todas las categorías'), ...categorias.map((c) => crear(c.id, c.nombre)));
}
document.querySelectorAll('#chips-estado .chip').forEach((c) => c.addEventListener('click', () => {
  filtroEstado = c.dataset.filtro;
  renderChips();
  renderInventario();
}));

function renderInventario() {
  const filtro = $('buscar').value.trim().toLowerCase();
  const lista = productos.filter((p) =>
    p.nombre.toLowerCase().includes(filtro)
    && (filtroEstado === 'todos' || p.estado === filtroEstado)
    && (filtroCategoria === 0 || p.idCategoria === filtroCategoria));
  const grid = $('grid-inventario');
  grid.replaceChildren();

  $('vacio-inventario').classList.toggle('oculto', productos.length > 0);
  $('sin-resultados').classList.toggle('oculto', productos.length === 0 || lista.length > 0);

  for (const p of lista) {
    const paso = PASO_STOCK[p.unidadMedida] || 1;
    grid.append(el('article', { class: 'prod' },
      el('div', { class: 'prod-cab' },
        el('div', { class: 'prod-ico', 'aria-hidden': 'true', text: icono(p) }),
        el('div', { class: 'prod-datos' },
          el('strong', { text: p.nombre }),
          el('span', { text: nombreCategoria(p.idCategoria) })),
        el('button', { class: 'badge boton ' + p.estado, type: 'button',
          title: 'Toca para cambiar el estado', 'aria-label': 'Cambiar estado de ' + p.nombre + ' (ahora ' + (ETIQUETA_ESTADO[p.estado] || p.estado) + ')',
          text: (ICONO_ESTADO[p.estado] || '') + ' ' + (ETIQUETA_ESTADO[p.estado] || p.estado),
          onclick: () => ciclarEstado(p) })),
      el('div', { class: 'prod-medio' },
        el('span', { class: 'prod-precio', text: moneda(p.precio) + ' / ' + p.unidadMedida })),
      el('div', { class: 'prod-stock' },
        el('button', { class: 'btn chico', type: 'button', 'aria-label': 'Restar ' + p.nombre, text: '−',
          onclick: () => ajustar(p, 'merma', paso) }),
        el('strong', { text: cantidad(p.stock) + ' ' + p.unidadMedida }),
        el('button', { class: 'btn chico', type: 'button', 'aria-label': 'Sumar ' + p.nombre, text: '+',
          onclick: () => ajustar(p, 'reposicion', paso) })),
      el('div', { class: 'prod-acciones' },
        el('button', { class: 'btn chico', type: 'button', text: '✏️ Editar', onclick: () => abrirFormulario(p) }),
        el('button', { class: 'btn chico peligro', type: 'button', text: '🗑️ Eliminar', onclick: () => eliminar(p) })),
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
    refrescarContadorAlertas();
  } catch (e) {
    toast(e.message, 'mal');
  }
}

// ---------- Interruptor táctil de 3 estados (verde > amarillo > rojo > verde) ---------- NO BORRAR 
const ICONO_ESTADO = { verde: '🟢', amarillo: '🟡', rojo: '🔴' };

// Stock que corresponde a cada color, según la misma regla del semáforo del servidor
function stockParaEstado(p, estado) {
  const min = Number(p.stockMinimo ?? 3);
  const redondeo = (n) => (p.unidadMedida === 'unidad' ? Math.ceil(n) : n);
  if (estado === 'rojo') return 0;
  if (estado === 'amarillo') return redondeo(min);
  return redondeo(Math.max(min * 2, min + 1));
}

function siguienteEstado(p) {
  const min = Number(p.stockMinimo ?? 3);
  if (p.estado === 'verde') return min > 0 ? 'amarillo' : 'rojo';   // con mínimo 0 no existe amarillo
  if (p.estado === 'amarillo') return 'rojo';
  return 'verde';
}

async function ciclarEstado(p) {
  const destino = siguienteEstado(p);
  const nuevo = stockParaEstado(p, destino);
  const ok = await confirmar(
    'Cambiar estado a ' + ETIQUETA_ESTADO[destino].toLowerCase(),
    '"' + p.nombre + '": el stock pasará de ' + cantidad(p.stock) + ' a ' + cantidad(nuevo) + ' ' + p.unidadMedida
      + '. Se guardará como ajuste en el historial.',
    'Cambiar', false);
  if (!ok) return;
  try {
    const actualizado = await Api.patch('/api/productos/' + p.id + '/stock', { tipo: 'ajuste', cantidad: nuevo });
    productos = productos.map((x) => (x.id === p.id ? actualizado : x));
    renderInventario();
    refrescarContadorAlertas();
    toast(p.nombre + ': ' + ETIQUETA_ESTADO[actualizado.estado].toLowerCase());
  } catch (e) {
    toast(e.message, 'mal');
  }
}

async function eliminar(p) {
  if (!(await confirmar('¿Eliminar producto?', '"' + p.nombre + '" dejará de aparecer en tu inventario.'))) return;
  try {
    await Api.del('/api/productos/' + p.id);
    productos = productos.filter((x) => x.id !== p.id);
    renderInventario();
    toast('Producto eliminado');
  } catch (e) {
    toast(e.message, 'mal');
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
  const nuevaCat = f.nuevaCategoria.value.trim();

  const cuerpo = {
    nombre: f.nombre.value.trim(),
    idCategoria: Number(f.idCategoria.value),
    precio: Number(f.precio.value),
    stock: Number(f.stock.value),
    unidadMedida: f.unidadMedida.value,
  };

  if (!nuevaCat && !f.idCategoria.value) {
    error.textContent = 'Elige una categoría o crea una nueva';
    return;
  }
  if (f.stockMinimo.value !== '') cuerpo.stockMinimo = Number(f.stockMinimo.value);
  if (!(cuerpo.precio > 0)) {
    error.textContent = 'El precio debe ser mayor que cero';
    return;
  }
  if (!(cuerpo.stock >= 0)) {
    error.textContent = 'El stock no es válido';
    return;
  }
  if (cuerpo.unidadMedida === 'unidad' && !Number.isInteger(cuerpo.stock)) {
    error.textContent = 'Los productos por unidad solo admiten stock entero';
    return;
  }

  try {
    if (nuevaCat) {
      const c = await Api.post('/api/categorias', { nombre: nuevaCat });
      categorias = [...categorias, c].sort((a, b) => a.nombre.localeCompare(b.nombre));
      cuerpo.idCategoria = c.id;
    }
    const guardado = editando
      ? await Api.put('/api/productos/' + editando.id, cuerpo)
      : await Api.post('/api/productos', cuerpo);
    productos = editando
      ? productos.map((x) => (x.id === guardado.id ? guardado : x))
      : [...productos, guardado].sort((a, b) => a.nombre.localeCompare(b.nombre));
    dialogo.close();
    renderInventario();
    toast(editando ? 'Producto actualizado' : 'Producto creado');
  } catch (e) {
    error.textContent = e.message;
  }
});

// ---------- Modo Caja ----------
let carrito = [];   // cada línea: { producto, cantidad, precio }
const FRACCIONES = [['1', 1], ['½', 0.5], ['¼', 0.25], ['⅛', 0.125]];
const redondear3 = (n) => Math.round(n * 1000) / 1000;
const subtotal = (l) => Math.round(l.cantidad * l.precio * 100) / 100;

// Mensaje en línea (errores). Los mensajes de éxito salen como aviso flotante.
function mensaje(id, texto, ok = false) {
  const e = $(id);
  if (ok && texto) { e.textContent = ''; e.classList.remove('ok'); toast(texto); return; }
  e.textContent = texto;
  e.classList.toggle('ok', ok);
}

async function cargarCaja() {
  mensaje('msg-caja', '');
  try {
    productos = await Api.get('/api/productos');
    // refresca las líneas con el stock actual y descarta productos que ya no existen - FUNCION DE MAPEO NO BORRAR - | CUIDADO!!!!
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
      el('span', { class: 'tile-ico', 'aria-hidden': 'true', text: icono(p) }),
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
    toast('No hay suficiente stock de ' + p.nombre, 'mal');
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

    const inCant = el('input', { type: 'text', inputmode: 'decimal', autocomplete: 'off',
      'data-enteros': '7', 'data-decimales': esUnidad ? '0' : '3',
      value: String(l.cantidad), 'data-campo': 'cantidad', 'aria-label': 'Cantidad de ' + l.producto.nombre });
    const inPrecio = el('input', { type: 'text', inputmode: 'decimal', autocomplete: 'off',
      'data-enteros': '6', 'data-decimales': '2',
      value: l.precio.toFixed(2), 'data-campo': 'precio', 'aria-label': 'Precio de ' + l.producto.nombre });

    const sub = el('strong', { class: 'linea-sub', text: moneda(subtotal(l)) });

    const refrescar = () => { sub.textContent = moneda(subtotal(l)); actualizarTotales(); };
    inCant.addEventListener('input', () => { l.cantidad = Number(inCant.value) || 0; refrescar(); });
    inPrecio.addEventListener('input', () => { l.precio = Number(inPrecio.value) || 0; refrescar(); });

    cont.append(el('div', { class: 'linea', 'data-id': String(l.producto.id) },
      el('div', { class: 'linea-nombre' },
        el('strong', { text: icono(l.producto) + ' ' + l.producto.nombre }),
        el('button', { class: 'btn chico peligro', type: 'button', text: '✕',
          'aria-label': 'Quitar ' + l.producto.nombre,
          onclick: () => { carrito = carrito.filter((x) => x !== l); renderCarrito(); } })),
      el('label', {}, 'Cantidad (' + l.producto.unidadMedida + ')', inCant),
      el('label', {}, 'Precio (S/)', inPrecio),
      sub,
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
  actualizarBarraVenta();
}
$('paga-con').addEventListener('input', actualizarTotales);

// Barra flotante (solo se ve en tableta/teléfono) con el total de la venta en curso
function actualizarBarraVenta() {
  const enCaja = !$('vista-caja').classList.contains('oculto');
  $('barra-venta').classList.toggle('oculto', !(enCaja && carrito.length > 0));
  const n = carrito.length;
  $('bv-cant').textContent = n + (n === 1 ? ' producto' : ' productos');
  $('bv-total').textContent = moneda(totalCarrito());
}
$('barra-venta').addEventListener('click', () => $('carrito').scrollIntoView({ behavior: 'smooth', block: 'start' }));

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
    refrescarContadorAlertas();
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
    const c = $('cont-alertas');
    c.textContent = String(lista.length);
    c.classList.toggle('oculto', lista.length === 0);
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

async function accionRapida(a, cant, aviso) {
  try {
    await Api.patch('/api/productos/' + (a.idProducto ?? a.productoId) + '/stock', { tipo: 'ajuste', cantidad: cant });
    toast(aviso);
    await cargarAlertas();
    await refrescarContadorAlertas();
  } catch (e) {
    toast(e.message, 'mal');
  }
}

function renderAlertas(lista) {
  const cont = $('lista-alertas');
  cont.replaceChildren();
  $('sin-alertas').classList.toggle('oculto', lista.length > 0);

  for (const a of lista) {
    const agotado = a.tipoAlerta === 'agotado';
    const tieneProducto = (a.idProducto ?? a.productoId) != null;
    const botones = [];
    if (tieneProducto) {
      if (!agotado) {
        botones.push(el('button', { class: 'btn chico', type: 'button', text: 'Marcar agotado',
          onclick: async () => {
            if (await confirmar('Marcar como agotado', 'El stock del producto pasará a 0.', 'Marcar', false)) {
              accionRapida(a, 0, 'Producto marcado como agotado');
            }
          } }));
      }
      botones.push(el('button', { class: 'btn chico primario', type: 'button', text: 'Reponer a 5',
        onclick: () => accionRapida(a, 5, 'Stock repuesto a 5') }));
    }
    botones.push(el('button', { class: 'btn chico', type: 'button', text: 'Marcar como leído',
      onclick: async () => {
        try {
          await Api.patch('/api/recordatorios/' + a.id + '/leido');
          await cargarAlertas();
          await refrescarContadorAlertas();
        } catch (e) {
          mensaje('msg-alertas', e.message);
        }
      } }));

    cont.append(el('div', { class: 'alerta ' + (agotado ? 'rojo' : 'amarillo') },
      el('div', {},
        el('strong', { text: (agotado ? '⛔ Agotado' : '⚠️ Poco stock') }),
        el('span', { text: ' · ' + a.mensaje }),
        el('div', { class: 'sub', text: new Date(a.fechaCreacion).toLocaleString('es-PE') })),
      el('div', { class: 'alerta-acciones' }, ...botones),
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
      el('span', { text: '🏷️ ' + c.nombre }),
      el('button', { class: 'btn chico peligro', type: 'button', text: 'Eliminar',
        'aria-label': 'Eliminar categoría ' + c.nombre, onclick: () => eliminarCategoria(c) }),
    ));
  }
}

async function eliminarCategoria(c) {
  if (!(await confirmar('¿Eliminar categoría?', 'Se eliminará la categoría "' + c.nombre + '".'))) return;
  mensaje('msg-categorias', '');
  try {
    await Api.del('/api/categorias/' + c.id);
    categorias = categorias.filter((x) => x.id !== c.id);
    renderCategorias();
    toast('Categoría eliminada');
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
    toast('Categoría agregada');
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

function seleccionarDiaReporte(fecha) {
  const dia = fecha.toLocaleDateString('en-CA');
  $('rep-desde').value = dia;
  $('rep-hasta').value = dia;
  verReporte();
}
//botones de selección rápida de día para reportes - 
$('btn-rep-hoy').addEventListener('click', () => seleccionarDiaReporte(new Date()));
$('btn-rep-ayer').addEventListener('click', () => {
  const ayer = new Date();
  ayer.setDate(ayer.getDate() - 1);
  seleccionarDiaReporte(ayer);
});






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
    toast('Excel descargado');
  } catch (e) {
    mensaje('msg-reportes', e.message);
  }
});

// ---------- Mi cuenta (perfil) ----------
const dlgPerfil = $('dlg-perfil');
const formPerfil = $('form-perfil');
const formPassword = $('form-password');

function msgForm(form, texto, ok = false) {
  const e = form.querySelector('.error');
  e.textContent = texto;
  e.classList.toggle('ok', ok);
}

$('btn-perfil').addEventListener('click', async () => {
  msgForm(formPerfil, '');
  msgForm(formPassword, '');
  formPassword.reset();
  dlgPerfil.showModal();
  try {
    const p = await Api.get('/api/perfil');
    formPerfil.elements.nombre.value = p.nombre;
    formPerfil.elements.email.value = p.email ?? '';
    formPerfil.elements.telefono.value = p.telefono;
  } catch (e) {
    msgForm(formPerfil, e.message);
  }
});

formPerfil.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  msgForm(formPerfil, '');
  const f = formPerfil.elements;
  try {
    const p = await Api.put('/api/perfil', {
      nombre: f.nombre.value.trim(),
      email: f.email.value.trim() || null,
    });
    const s = Api.sesion();
    if (s) { s.nombre = p.nombre; Api.guardar(s); }
    $('usuario-nombre').textContent = p.nombre;
    msgForm(formPerfil, 'Datos guardados', true);
  } catch (e) {
    msgForm(formPerfil, e.message);
  }
});

formPassword.addEventListener('submit', async (evento) => {
  evento.preventDefault();
  msgForm(formPassword, '');
  const f = formPassword.elements;
  if (f.passwordNueva.value !== f.passwordNueva2.value) {
    msgForm(formPassword, 'Las contraseñas nuevas no coinciden');
    return;
  }
  try {
    await Api.put('/api/perfil/password', {
      passwordActual: f.passwordActual.value,
      passwordNueva: f.passwordNueva.value,
    });
    formPassword.reset();
    msgForm(formPassword, 'Contraseña actualizada', true);
  } catch (e) {
    msgForm(formPassword, e.message);
  }
});

$('btn-cerrar-perfil').addEventListener('click', () => dlgPerfil.close());

// ---------- Calculadora ----------
const dlgCalc = $('dlg-calc');
const calc = { actual: '0', anterior: null, op: null, nuevo: true };
const TECLAS = [['7', '8', '9', '÷'], ['4', '5', '6', '×'], ['1', '2', '3', '-'], ['0', '.', 'C', '+'], ['=']];
let lineaActiva = null;   // producto de la última línea del carrito que se tocó

$('lineas-carrito').addEventListener('focusin', (e) => {
  const linea = e.target.closest ? e.target.closest('.linea') : null;
  if (linea) lineaActiva = Number(linea.dataset.id);
});

function calcPintar() { $('calc-pantalla').textContent = calc.actual; }
function calcReiniciar() { calc.actual = '0'; calc.anterior = null; calc.op = null; calc.nuevo = true; calcPintar(); }

function calcOperar() {
  if (calc.op === null || calc.anterior === null) return;
  const a = calc.anterior;
  const b = Number(calc.actual);
  let r;
  if (calc.op === '+') r = a + b;
  else if (calc.op === '-') r = a - b;
  else if (calc.op === '×') r = a * b;
  else r = b === 0 ? NaN : a / b;
  calc.anterior = null;
  calc.op = null;
  calc.nuevo = true;
  calc.actual = Number.isFinite(r) ? String(Math.round(r * 1e6) / 1e6) : 'Error';
}

function calcTecla(t) {
  if (calc.actual === 'Error' && t !== 'C') calcReiniciar();
  if (t === 'C') { calcReiniciar(); return; }
  if (/^[0-9]$/.test(t)) {
    calc.actual = calc.nuevo || calc.actual === '0' ? t : (calc.actual.length < 12 ? calc.actual + t : calc.actual);
    calc.nuevo = false;
  } else if (t === '.') {
    if (calc.nuevo) { calc.actual = '0.'; calc.nuevo = false; }
    else if (!calc.actual.includes('.')) calc.actual += '.';
  } else if (t === '=') {
    calcOperar();
  } else {   // + - × ÷
    if (calc.op !== null && !calc.nuevo) calcOperar();
    if (calc.actual !== 'Error') { calc.anterior = Number(calc.actual); calc.op = t; calc.nuevo = true; }
  }
  calcPintar();
}

for (const fila of TECLAS) {
  for (const t of fila) {
    $('calc-teclas').append(el('button', { type: 'button', class: 'btn calc-tecla' + (/[0-9.]/.test(t) ? '' : ' op') + (t === '=' ? ' igual primario' : ''),
      text: t, 'aria-label': t === 'C' ? 'Borrar' : t, onclick: () => calcTecla(t) }));
  }
}

function calcUsar(campo) {
  calcOperar();
  calcPintar();
  const v = Number(calc.actual);
  if (!Number.isFinite(v) || v < 0) { toast('El resultado no sirve para este campo', 'mal'); return; }
  let destino;
  if (campo === 'paga') {
    destino = $('paga-con');
  } else {
    const id = lineaActiva != null ? lineaActiva : (carrito.length ? carrito[carrito.length - 1].producto.id : null);
    destino = id == null ? null : document.querySelector('.linea[data-id="' + id + '"] input[data-campo="' + campo + '"]');
  }
  if (!destino) { toast('Agrega un producto a la venta primero', 'mal'); return; }
  destino.value = String(v);
  destino.dispatchEvent(new Event('input', { bubbles: true }));
  toast('Resultado copiado');
  dlgCalc.close();
}
document.querySelectorAll('[data-usar]').forEach((b) => b.addEventListener('click', () => calcUsar(b.dataset.usar)));
$('btn-calc').addEventListener('click', () => { calcReiniciar(); dlgCalc.showModal(); });
$('calc-cerrar').addEventListener('click', () => dlgCalc.close());

// ---------- Conexión y PWA ----------
function pintarConexion() { $('aviso-offline').classList.toggle('oculto', navigator.onLine); }
window.addEventListener('online', pintarConexion);
window.addEventListener('offline', pintarConexion);
pintarConexion();

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => { navigator.serviceWorker.register('/sw.js').catch(() => { /* sin PWA */ }); });
}

// ---------- arranque ----------
if (Api.sesion()) mostrarApp(); else mostrarAuth();
