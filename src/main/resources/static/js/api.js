// Cliente HTTP: agrega el token, convierte errores del servidor en mensajes legibles.
const Api = (() => {
  const CLAVE = 'bm_sesion';
  let alExpirar = () => {};

  function sesion() {
    try { return JSON.parse(sessionStorage.getItem(CLAVE)); } catch { return null; }
  }
  function guardar(s) {
    try { sessionStorage.setItem(CLAVE, JSON.stringify(s)); } catch { /* sin almacenamiento */ }
  }
  function limpiar() {
    try { sessionStorage.removeItem(CLAVE); } catch { /* nada */ }
  }

  async function pedir(metodo, ruta, cuerpo) {
    const s = sesion();
    const cabeceras = {};
    if (cuerpo !== undefined) cabeceras['Content-Type'] = 'application/json';
    if (s && s.token) cabeceras['Authorization'] = 'Bearer ' + s.token;

    let resp;
    try {
      resp = await fetch(ruta, {
        method: metodo,
        headers: cabeceras,
        body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
      });
    } catch {
      throw new Error('No hay conexión con el servidor');
    }

    if (resp.status === 401 && s) {
      limpiar();
      alExpirar();
      throw new Error('Tu sesión expiró, ingresa de nuevo');
    }
    if (resp.status === 204) return null;

    let datos = null;
    try { datos = await resp.json(); } catch { /* sin cuerpo */ }
    if (!resp.ok) throw new Error((datos && datos.error) || 'Error ' + resp.status);
    return datos;
  }

    // Descarga de archivos EXCEL NO BORRAR
  async function blob(ruta) {
    const s = sesion();
    let resp;
    try {
      resp = await fetch(ruta, { headers: s && s.token ? { Authorization: 'Bearer ' + s.token } : {} });
    } catch {
      throw new Error('No hay conexión con el servidor');
    }
    if (resp.status === 401) { limpiar(); alExpirar(); throw new Error('Tu sesión expiró, ingresa de nuevo'); }
    if (!resp.ok) {
      let d = null;
      try { d = await resp.json(); } catch { /* sin cuerpo */ }
      throw new Error((d && d.error) || 'Error ' + resp.status);
    }
    return resp.blob();
  }


  return {
    sesion, guardar, limpiar,blob,
    alExpirar(fn) { alExpirar = fn; },
    get: (r) => pedir('GET', r),
    post: (r, b) => pedir('POST', r, b),
    put: (r, b) => pedir('PUT', r, b),
    patch: (r, b) => pedir('PATCH', r, b),
    del: (r) => pedir('DELETE', r),
  };
})();