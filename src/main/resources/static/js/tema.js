// Aplica el tema guardado (o el del sistema) antes de pintar, para evitar el parpadeo.
(function () {
  let tema = null;
  try { tema = localStorage.getItem('bm_tema'); } catch { /* sin almacenamiento */ }
  if (tema !== 'dark' && tema !== 'light') {
    tema = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  document.documentElement.setAttribute('data-theme', tema);
})();
