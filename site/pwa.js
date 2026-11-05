// Registers the service worker. Loaded with defer from every generated page,
// as a separate file rather than inline: one cached request beats repeating
// the same bytes across ~2,400 pages.
(function () {
  if (!('serviceWorker' in navigator)) return;

  window.addEventListener('load', function () {
    navigator.serviceWorker.register('/sw.js').then(function () {
      return navigator.serviceWorker.ready;
    }).then(function (registration) {
      // Ask for this page to be stored. On a first visit the worker was still
      // installing when the page was fetched, so it never saw that request --
      // without this, the daf being read right now is the one that would be
      // missing when the signal drops.
      var worker = navigator.serviceWorker.controller || registration.active;
      if (worker) {
        worker.postMessage({ type: 'cache-page', url: window.location.href });
      }
    }).catch(function (err) {
      // Nothing to do for the reader -- the site works without the worker,
      // it just will not be available offline.
      if (window.console) console.warn('Service worker registration failed:', err);
    });
  });
})();
