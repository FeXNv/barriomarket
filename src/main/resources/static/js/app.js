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

const cargadores = { inventario: cargarInventario };

function cambiarVista(nombre) {
  document.querySelectorAll('.nav').forEach((b) => b.classList.toggle('activa', b.dataset.vista === nombre));
  document.querySelectorAll('.vista').forEach((v) => v.classList.toggle('oculto', v.id !== 'vista-' + nombre));
  if (cargadores[nombre]) cargadores[nombre]();
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

// ---------- arranque ----------
if (Api.sesion()) mostrarApp(); else mostrarAuth();