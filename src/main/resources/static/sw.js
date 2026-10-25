// Service worker de BarrioMarket: guarda la pantalla para abrirla sin conexión.
// Los datos (/api) nunca se guardan: siempre vienen del servidor.
const CACHE = 'barriomarket-v1';
const BASE = ['/', '/css/app.css', '/js/tema.js', '/js/api.js', '/js/app.js', '/manifest.webmanifest', '/icons/icon-192.png'];

self.addEventListener('install', (e) => {
  e.waitUntil(
    caches.open(CACHE)
      .then((c) => Promise.allSettled(BASE.map((u) => c.add(u))))
      .then(() => self.skipWaiting()));
});

self.addEventListener('activate', (e) => {
  e.waitUntil(
    caches.keys()
      .then((claves) => Promise.all(claves.filter((k) => k !== CACHE).map((k) => caches.delete(k))))
      .then(() => self.clients.claim()));
});

// Con internet siempre se usa la versión más nueva; sin internet, la guardada.
self.addEventListener('fetch', (e) => {
  const req = e.request;
  const url = new URL(req.url);
  if (req.method !== 'GET' || url.origin !== self.location.origin || url.pathname.startsWith('/api/')) return;

  e.respondWith(
    fetch(req)
      .then((resp) => {
        if (resp.ok) {
          const copia = resp.clone();
          caches.open(CACHE).then((c) => c.put(req, copia));
        }
        return resp;
      })
      .catch(() => caches.match(req).then((g) => g || (req.mode === 'navigate' ? caches.match('/') : undefined))));
});
