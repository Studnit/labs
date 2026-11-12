(function () {
  'use strict';
  var meta = function (n) { var e = document.querySelector('meta[name="' + n + '"]'); return e ? e.content : ''; };
  var csrfHeader = meta('_csrf_header'), csrfToken = meta('_csrf');

  window.apiPost = function (url, body) {
    var h = { 'X-Requested-With': 'XMLHttpRequest', 'Accept': 'application/json' };
    if (csrfHeader) h[csrfHeader] = csrfToken;
    if (body) h['Content-Type'] = 'application/json';
    return fetch(url, { method: 'POST', headers: h, body: body ? JSON.stringify(body) : undefined, credentials: 'same-origin' })
      .then(function (r) { return r.json().then(function (j) { if (!r.ok) throw new Error(j.error || 'Request failed'); return j; }); });
  };

  function toast(title, message, link) {
    var stack = document.getElementById('toast-stack'); if (!stack || !window.bootstrap) return;
    var el = document.createElement('div'); el.className = 'toast align-items-center'; el.setAttribute('role', 'alert');
    var body = document.createElement('div'); body.className = 'toast-body';
    var strong = document.createElement('strong'); strong.textContent = title; body.appendChild(strong);
    if (message) { body.appendChild(document.createElement('br')); body.appendChild(document.createTextNode(message)); }
    if (link && link.charAt(0) === '/' && link.charAt(1) !== '/') {
      body.style.cursor = 'pointer'; body.addEventListener('click', function () { window.location.href = link; });
    }
    var wrap = document.createElement('div'); wrap.className = 'd-flex'; wrap.appendChild(body);
    var close = document.createElement('button'); close.type = 'button'; close.className = 'btn-close me-2 m-auto'; close.setAttribute('data-bs-dismiss', 'toast'); wrap.appendChild(close);
    el.appendChild(wrap); stack.appendChild(el);
    var t = new bootstrap.Toast(el, { delay: 6000 }); t.show(); el.addEventListener('hidden.bs.toast', function () { el.remove(); });
  }
  window.slToast = toast;

  // ---- WebSocket (notifications + chat) ----
  var ws, attempt = 0;
  window.rt = { send: function (o) { if (ws && ws.readyState === 1) { ws.send(JSON.stringify(o)); return true; } return false; } };
  function connect() {
    if (document.body.dataset.auth !== 'true') return;
    ws = new WebSocket((location.protocol === 'https:' ? 'wss://' : 'ws://') + location.host + '/ws');
    ws.onopen = function () { attempt = 0; };
    ws.onmessage = function (e) {
      var d; try { d = JSON.parse(e.data); } catch (x) { return; }
      if (d.type === 'notification') {
        var b = document.getElementById('notif-badge');
        if (b) { b.textContent = d.unread; b.classList.toggle('d-none', !d.unread); }
        toast(d.title, d.message, d.link);
      } else if (d.type === 'chat') {
        document.dispatchEvent(new CustomEvent('rt:chat', { detail: d.message }));
      } else if (d.type === 'error') { toast('Error', d.message); }
    };
    ws.onclose = function () { setTimeout(connect, Math.min(30000, 1000 * Math.pow(2, attempt++))); };
  }
  connect();

  // ---- confirm dialogs ----
  document.addEventListener('submit', function (e) {
    var c = e.target.getAttribute && e.target.getAttribute('data-confirm');
    if (c && !window.confirm(c)) e.preventDefault();
  });

  // ---- fill modals from the trigger button's data-* ----
  document.addEventListener('show.bs.modal', function (e) {
    var m = e.target, b = e.relatedTarget;
    if (!m.hasAttribute('data-fill')) return;
    m.querySelectorAll('[name]').forEach(function (inp) {
      var v = b && b.dataset && b.dataset[inp.name] !== undefined ? b.dataset[inp.name] : (inp.dataset.default || '');
      inp.value = v;
    });
  });

  // ---- likes ----
  document.addEventListener('click', function (e) {
    var btn = e.target.closest('[data-like]'); if (!btn) return;
    e.preventDefault();
    window.apiPost('/feed/api/' + btn.dataset.like + '/like').then(function (r) {
      btn.classList.toggle('liked', r.liked);
      btn.querySelector('i').className = r.liked ? 'bi bi-heart-fill' : 'bi bi-heart';
      btn.querySelector('span').textContent = r.likes;
    }).catch(function (err) { toast('Error', err.message); });
  });

  // ---- chat page ----
  var root = document.getElementById('chat-root');
  if (root) {
    var me = Number(root.dataset.me), room = root.dataset.room ? Number(root.dataset.room) : null;
    var list = document.getElementById('chat-messages'), form = document.getElementById('chat-form'), input = document.getElementById('chat-input');
    var add = function (m) {
      var d = document.createElement('div'); d.className = 'msg' + (m.senderId === me ? ' me' : '');
      var meta = document.createElement('div'); meta.className = 'meta'; meta.textContent = (m.senderId === me ? 'You' : m.sender) + ' · ' + m.at;
      var t = document.createElement('div'); t.textContent = m.content;
      d.appendChild(meta); d.appendChild(t); list.appendChild(d); list.scrollTop = list.scrollHeight;
    };
    if (room) {
      fetch('/chat/api/rooms/' + room + '/messages', { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
        .then(function (r) { return r.json(); }).then(function (a) { a.forEach(add); });
      document.addEventListener('rt:chat', function (e) { if (e.detail.roomId === room) add(e.detail); });
      form.addEventListener('submit', function (e) {
        e.preventDefault(); var v = input.value.trim(); if (!v) return;
        if (window.rt.send({ type: 'chat', roomId: room, content: v })) input.value = '';
        else toast('Offline', 'Reconnecting… try again in a moment.');
      });
    }
  }

  // ---- QR scanner: works in every browser (BarcodeDetector -> jsQR fallback -> photo upload -> manual code) ----
  var video = document.getElementById('scanner-video');
  if (video) {
    var status = document.getElementById('scanner-status'), frame = document.getElementById('scan-frame');
    var canvas = document.getElementById('scanner-canvas'), ctx2d = canvas.getContext('2d', { willReadFrequently: true });
    var stream = null, running = false;
    var say = function (t, cls) { status.textContent = t; status.className = 'mt-3 mb-0 ' + (cls || 'text-muted'); };

    // Accepts a full URL or just the 32-char code and sends the user to the check-in page.
    function go(raw) {
      raw = (raw || '').trim();
      var m = raw.match(/\/attendance\/scan\/([a-f0-9]{32})(?:[\/?#]|$)/i) || raw.match(/^([a-f0-9]{32})$/i);
      if (!m) return false;
      if (/^https?:/i.test(raw)) { try { if (new URL(raw).origin !== location.origin) return false; } catch (x) { return false; } }
      running = false; stopCamera();
      location.href = '/attendance/scan/' + m[1].toLowerCase();
      return true;
    }
    function stopCamera() { if (stream) { stream.getTracks().forEach(function (t) { t.stop(); }); stream = null; } }
    function decodeJsQR(img, w, h) {
      if (!window.jsQR) return null;
      var r = window.jsQR(img.data, w, h, { inversionAttempts: 'attemptBoth' });
      return r ? r.data : null;
    }

    // 1) live camera
    function startCamera() {
      var md = navigator.mediaDevices;
      if (!md || !md.getUserMedia) {
        say('Live camera is not available here (it needs HTTPS). Use "Upload a photo" or enter the code below.', 'text-warning');
        return;
      }
      md.getUserMedia({ video: { facingMode: { ideal: 'environment' } }, audio: false }).then(function (s) {
        stream = s; video.srcObject = s; video.setAttribute('playsinline', 'true');
        return video.play();
      }).then(function () {
        frame.classList.remove('idle'); running = true; say('Point the camera at the library QR code…');
        var det = null;
        if ('BarcodeDetector' in window) { try { det = new BarcodeDetector({ formats: ['qr_code'] }); } catch (x) { det = null; } }
        (function tick() {
          if (!running) return;
          if (video.readyState >= 2 && video.videoWidth) {
            if (det) {
              det.detect(video).then(function (codes) {
                for (var i = 0; i < codes.length; i++) if (go(codes[i].rawValue)) return;
                setTimeout(tick, 250);
              }).catch(function () { det = null; setTimeout(tick, 250); });
              return;
            }
            var w = Math.min(video.videoWidth, 640), h = Math.round(w * video.videoHeight / video.videoWidth);
            canvas.width = w; canvas.height = h; ctx2d.drawImage(video, 0, 0, w, h);
            var code = decodeJsQR(ctx2d.getImageData(0, 0, w, h), w, h);
            if (code && go(code)) return;
          }
          setTimeout(tick, 200);
        })();
      }).catch(function (e) {
        var denied = e && (e.name === 'NotAllowedError' || e.name === 'SecurityError');
        say(denied ? 'Camera permission was denied. Allow camera access, or use "Upload a photo" / enter the code below.'
                   : 'Could not start the camera. Use "Upload a photo" or enter the code below.', 'text-warning');
      });
    }
    var startBtn = document.getElementById('scanner-start');
    if (startBtn) startBtn.addEventListener('click', function () { stopCamera(); startCamera(); });
    startCamera();

    // 2) photo of the QR (phone camera / gallery) — decoded in the browser
    var file = document.getElementById('scanner-file');
    if (file) file.addEventListener('change', function () {
      var f = file.files && file.files[0]; if (!f) return;
      say('Reading photo…');
      var url = URL.createObjectURL(f), img = new Image();
      img.onload = function () {
        var tries = [1, 0.6, 0.35], found = null;
        for (var k = 0; k < tries.length && !found; k++) {
          var w = Math.max(200, Math.round(Math.min(img.width, 1600) * tries[k])), h = Math.round(w * img.height / img.width);
          canvas.width = w; canvas.height = h; ctx2d.drawImage(img, 0, 0, w, h);
          found = decodeJsQR(ctx2d.getImageData(0, 0, w, h), w, h);
        }
        URL.revokeObjectURL(url);
        if (!found || !go(found)) say('No valid library QR found in that photo. Try a clearer, closer photo.', 'text-danger');
      };
      img.onerror = function () { say('Could not open that image.', 'text-danger'); };
      img.src = url;
    });

    // 3) manual code / link
    var manual = document.getElementById('scanner-manual');
    if (manual) manual.addEventListener('submit', function (e) {
      e.preventDefault();
      if (!go(document.getElementById('scanner-code').value)) say('That is not a valid library code or link.', 'text-danger');
    });
    window.addEventListener('pagehide', stopCamera);
  }
})();
