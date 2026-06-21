/* Photon-Bounce Aurora v1.8.2 — main interaction bundle */
(() => {
  'use strict';

  /* ── Site music (MP3 autoplay with interaction fallback) ── */
  (function initMusic() {
    const tracks = [
      (window.PB_AURORA?.themeUrl || '') + '/../media/sitemusic/pb1.mp3',
      (window.PB_AURORA?.themeUrl || '') + '/../media/sitemusic/pb2.mp3',
    ].filter(Boolean);
    if (!tracks.length) return;
    let current = 0;
    const audio = new Audio();
    audio.loop = false;
    audio.volume = 0.25;
    function playNext() {
      audio.src = tracks[current % tracks.length];
      audio.play().catch(() => {});
      current++;
    }
    audio.addEventListener('ended', playNext);
    playNext();
    function resumeOnInteraction() {
      if (audio.paused) { playNext(); }
      document.removeEventListener('click', resumeOnInteraction);
      document.removeEventListener('touchstart', resumeOnInteraction);
    }
    document.addEventListener('click', resumeOnInteraction);
    document.addEventListener('touchstart', resumeOnInteraction);

    // Header mute toggle
    var toggle = document.getElementById('pb-music-toggle');
    if (toggle) {
      toggle.addEventListener('click', function(e) {
        e.preventDefault();
        e.stopPropagation();
        audio.muted = !audio.muted;
        toggle.classList.toggle('is-muted', audio.muted);
        toggle.innerHTML = audio.muted ? '<span aria-hidden="true">&#128263;</span>' : '<span aria-hidden="true">&#127925;</span>';
      });
    }
  })();

  /* ── Sound effects on buttons / links ── */
  (function initSfx() {
    const ctx = typeof AudioContext !== 'undefined' ? new AudioContext() : null;
    if (!ctx) return;
    function blip(freq, duration, type) {
      freq = freq || 880; duration = duration || 0.06; type = type || 'sine';
      if (ctx.state === 'suspended') ctx.resume();
      const o = ctx.createOscillator();
      const g = ctx.createGain();
      o.type = type;
      o.frequency.setValueAtTime(freq, ctx.currentTime);
      g.gain.setValueAtTime(0.08, ctx.currentTime);
      g.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration);
      o.connect(g);
      g.connect(ctx.destination);
      o.start();
      o.stop(ctx.currentTime + duration);
    }
    document.addEventListener('click', function(e) {
      var btn = e.target.closest('button, .pb-btn, a, [data-pb-ripple], summary');
      if (!btn) return;
      if (btn.closest('.pb-brain__form, .pb-exit__form, .pb-footer__subscribe, [data-pb-subscribe-form]')) { blip(660, 0.05); return; }
      if (btn.closest('[data-pb-cat-tabs]')) { blip(1100, 0.04); return; }
      blip(880, 0.05);
    });
  })();

  /* Scroll reveal */
  const revealObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) entry.target.classList.add('is-revealed');
    });
  }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });
  document.querySelectorAll('[data-pb-reveal]').forEach((el) => revealObserver.observe(el));

  /* Pricing category tabs */
  document.querySelectorAll('[data-pb-cat-tabs]').forEach((tablist) => {
    const tabs = tablist.querySelectorAll('[data-cat]');
    const panels = tablist.parentElement.querySelectorAll('[data-cat-panel]');
    tabs.forEach((tab) => {
      tab.addEventListener('click', () => {
        const cat = tab.dataset.cat;
        tabs.forEach((t) => { t.classList.toggle('is-on', t === tab); t.setAttribute('aria-selected', t === tab ? 'true' : 'false'); });
        panels.forEach((p) => { p.classList.toggle('is-on', p.dataset.catPanel === cat); });
      });
    });
  });

  /* Pricing calculator */
  document.querySelectorAll('[data-pb-calc]').forEach((calc) => {
    const typeEl = calc.querySelector('[data-calc-type]');
    const pagesEl = calc.querySelector('[data-calc-pages]');
    const pagesOut = calc.querySelector('[data-calc-pages-out]');
    const aiSelect = calc.querySelector('[data-calc-ai-feat]');
    const seoEl = calc.querySelector('[data-calc-seo]');
    const seoOut = calc.querySelector('[data-calc-seo-out]');
    const rushEl = calc.querySelector('[data-calc-rush]');
    const priceEl = calc.querySelector('[data-calc-price]');

    const base = { micro: 40, simple: 115, full: 300, webgl: 600, saas: 750, ai: 900 };
    const pageCost = { micro: 0, simple: 15, full: 25, webgl: 35, saas: 40, ai: 45 };
    const seoCost = [0, 30, 75, 150, 250, 400];

    function recalc() {
      const type = typeEl?.value || 'simple';
      const pages = parseInt(pagesEl?.value || 5, 10);
      const seo = parseInt(seoEl?.value || 2, 10);
      const rush = rushEl?.checked ? 1.4 : 1;
      let aiSum = 0;
      if (aiSelect && aiSelect.value) {
        aiSum = parseInt(aiSelect.value.split(':')[1] || 0, 10);
      }
      if (pagesOut) pagesOut.textContent = pages;
      if (seoOut) seoOut.textContent = seo;
      const low = Math.round(((base[type] || 115) + (pages - 1) * (pageCost[type] || 15) + aiSum + (seoCost[seo] || 0)) * rush);
      const high = Math.round(low * 1.25);
      if (priceEl) priceEl.innerHTML = '$' + low.toLocaleString() + ' - $' + high.toLocaleString();
    }
    typeEl?.addEventListener('change', recalc);
    pagesEl?.addEventListener('input', recalc);
    seoEl?.addEventListener('input', recalc);
    rushEl?.addEventListener('change', recalc);
    aiSelect?.addEventListener('change', recalc);
    recalc();
  });

  /* Build form toggle */
  const buildForm = document.querySelector('[data-build-form]');
  const buildGrid = document.querySelector('.pb-build__grid');
  document.querySelectorAll('[data-build-kind]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const kind = btn.dataset.buildKind;
      const price = btn.dataset.buildPrice;
      const label = btn.dataset.buildLabel;
      const titleEl = buildForm?.querySelector('[data-build-form-title]');
      if (titleEl) titleEl.textContent = label;
      buildGrid?.classList.add('is-hidden');
      buildForm?.removeAttribute('hidden');
    });
  });
  document.querySelectorAll('[data-build-cancel]').forEach((btn) => {
    btn.addEventListener('click', () => {
      buildForm?.setAttribute('hidden', '');
      buildGrid?.classList.remove('is-hidden');
    });
  });

  /* Footer subscribe */
  document.querySelectorAll('[data-pb-subscribe-form]').forEach((form) => {
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const msg = form.querySelector('[data-pb-subscribe-msg]');
      const fd = new FormData(form);
      const body = {};
      fd.forEach((v, k) => { body[k] = v; });
      try {
        const res = await fetch(form.dataset.pbRest || (PB_AURORA?.restUrl + 'pb/v1/subscribe'), {
          method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
        });
        const data = await res.json();
        if (msg) { msg.textContent = data.ok ? 'Subscribed! Welcome aboard.' : (data.error || 'Could not subscribe.'); msg.className = 'pb-footer__subscribe-status pb-subscribe__msg ' + (data.ok ? 'is-ok' : 'is-err'); }
      } catch (err) {
        if (msg) { msg.textContent = 'Network error.'; msg.className = 'pb-footer__subscribe-status pb-subscribe__msg is-err'; }
      }
    });
  });

  /* Lead form wiring (exit modal + any [data-pb-lead-form]) */
  document.querySelectorAll('[data-pb-lead-form]').forEach((form) => {
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      const msg = form.querySelector('[data-pb-lead-msg]');
      const fd = new FormData(form);
      const data = {};
      fd.forEach(function(v, k){ data[k] = v; });
      data.ref = location.href;
      if (!data.email || !/.+@.+\..+/.test(data.email)) {
        if (msg) { msg.textContent = 'Please enter a valid email.'; msg.className = 'pb-exit__msg is-err'; }
        return;
      }
      if (msg) { msg.textContent = 'Sending…'; msg.className = 'pb-exit__msg is-pending'; }
      fetch(form.dataset.pbRest || (PB_AURORA?.restUrl + 'pb/v1/lead'), {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(data),
      }).then(function(r){ return r.json(); }).then(function(j){
        if (j && j.ok) {
          var playbook = j.playbook || '/playbook/';
          if (msg) { msg.innerHTML = 'Got it — the playbook is on its way to your inbox. Or <a href="' + playbook + '" target="_blank" rel="noopener" style="color:#ffd400;text-decoration:underline">read it right now in your browser →</a>'; msg.className = 'pb-exit__msg is-ok'; }
          form.reset();
          try { window.open(playbook, '_blank', 'noopener'); } catch (e) {}
        } else {
          if (msg) { msg.textContent = (j && j.error) ? ('Hmm: ' + j.error) : 'Something blocked the send. Try again?'; msg.className = 'pb-exit__msg is-err'; }
        }
      }).catch(function(){ if (msg) { msg.textContent = 'Network error. Try again?'; msg.className = 'pb-exit__msg is-err'; } });
    });
  });

  /* Sticky bottom bar */
  var stick = document.getElementById('pb-stickbar');
  if (stick) {
    var dismissed = false;
    try { dismissed = localStorage.getItem('pb_stick_dismiss_v1') === '1'; } catch (e) {}
    var closeBtn = stick.querySelector('[data-pb-stickbar-close]');
    if (closeBtn) closeBtn.addEventListener('click', function(){
      stick.hidden = true; dismissed = true;
      try { localStorage.setItem('pb_stick_dismiss_v1', '1'); } catch (e) {}
    });
    window.addEventListener('scroll', function() {
      if (dismissed) return;
      if ((window.scrollY || 0) > 600) { if (stick.hidden) stick.hidden = false; }
    }, { passive: true });
  }

  /* Exit modal (explicit open only) */
  var exitEl = document.getElementById('pb-exit');
  function closeExit() {
    if (!exitEl || exitEl.hidden) return;
    exitEl.hidden = true;
    document.body.classList.remove('pb-no-scroll');
    try { localStorage.setItem('pb_exit_seen_v1', '1'); } catch (e) {}
  }
  if (exitEl) {
    exitEl.querySelectorAll('[data-pb-exit-close]').forEach(function(b){ b.addEventListener('click', closeExit); });
    document.addEventListener('keydown', function(e){ if (e.key === 'Escape') closeExit(); });
    window.pbOpenPlaybook = function() { exitEl.hidden = false; document.body.classList.add('pb-no-scroll'); };
    document.querySelectorAll('[data-pb-open-exit], [data-pb-open-playbook]').forEach(function(b){
      b.addEventListener('click', function(e){ e.preventDefault(); window.pbOpenPlaybook(); });
    });
  }

  /* Tel / WhatsApp decoder */
  function decode(b64) { try { return atob(b64); } catch (e) { return ''; } }
  function bindTel(sel, attr) {
    document.querySelectorAll(sel).forEach(function(a) {
      a.addEventListener('click', function(e) {
        var url = decode(a.getAttribute(attr));
        if (!url) return;
        e.preventDefault();
        if (url.indexOf('tel:') === 0) location.href = url;
        else window.open(url, '_blank', 'noopener');
      }, { passive: false });
    });
  }
  bindTel('[data-pb-tel]', 'data-pb-tel');
  bindTel('[data-pb-wa]',  'data-pb-wa');

  /* Mobile nav toggle */
  const navToggle = document.querySelector('[data-pb-nav-toggle]');
  const nav = document.getElementById('pb-nav');
  if (navToggle && nav) {
    navToggle.addEventListener('click', () => {
      const open = nav.classList.toggle('is-open');
      navToggle.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
  }
})();


/* ================================================================
   CHATBOT / CONCIERGE FIX — open/close + form handling
   ================================================================ */
(function () {
  'use strict';
  const brain = document.getElementById('pb-brain');
  const openBtns = document.querySelectorAll('[data-pb-brainstorm-open], [data-pb-open-orb]');
  const closeBtns = document.querySelectorAll('[data-pb-brainstorm-close]');
  const form = document.querySelector('[data-pb-brain-form]');
  const input = document.querySelector('[data-pb-brain-input]');
  const log = document.querySelector('[data-pb-brain-log]');
  const micBtn = document.querySelector('[data-pb-brain-mic]');
  const orb = document.querySelector('.pb-orb');

  // In-chat search bar — Ctrl+F while brain is open
  var _srchInput = document.createElement('input');
  _srchInput.type = 'search';
  _srchInput.className = 'pb-brain__search';
  _srchInput.setAttribute('placeholder', 'Search messages…');
  _srchInput.setAttribute('aria-label', 'Search chat messages');
  _srchInput.setAttribute('autocomplete', 'off');
  if (log && log.parentNode) log.parentNode.insertBefore(_srchInput, log);
  var _srchCount = document.createElement('span');
  _srchCount.className = 'pb-brain__search-count';
  _srchCount.setAttribute('aria-live', 'polite');
  _srchCount.setAttribute('aria-atomic', 'true');
  _srchCount.style.display = 'none';
  if (log && log.parentNode) log.parentNode.insertBefore(_srchCount, log);
  function _srchFilter() {
    var q = _srchInput.value.toLowerCase().trim();
    var msgs = log ? log.querySelectorAll('.pb-brain__msg') : [];
    var _n = 0;
    [].forEach.call(msgs, function(m) {
      var _hit = !q || m.textContent.toLowerCase().indexOf(q) !== -1;
      m.style.opacity = _hit ? '' : '0.15';
      if (q && _hit) _n++;
    });
    _srchCount.textContent = q ? (_n === 0 ? 'No results' : _n + ' match' + (_n !== 1 ? 'es' : '')) : '';
    _srchCount.style.display = q ? '' : 'none';
  }
  function _srchClose() {
    _srchInput.value = ''; _srchFilter();
    _srchInput.classList.remove('pb-brain__search--open');
  }
  _srchInput.addEventListener('input', _srchFilter);
  document.addEventListener('keydown', function(e) {
    var brainVisible = brain && brain.offsetParent !== null;
    if (e.ctrlKey && (e.key === 'f' || e.key === 'F') && brainVisible) {
      e.preventDefault();
      _srchInput.classList.add('pb-brain__search--open');
      _srchInput.focus();
      _srchInput.select();
    }
    if (e.key === 'Escape' && document.activeElement === _srchInput) { _srchClose(); if (input) input.focus(); }
  });
  if (brain) new MutationObserver(function() {
    if (brain.offsetParent === null) _srchClose();
  }).observe(brain, { attributes: true, attributeFilter: ['style', 'class'] });

  // "New message" chip — appears when bot replies while user is scrolled up
  var _newMsgChip = document.createElement('button');
  _newMsgChip.type = 'button';
  _newMsgChip.className = 'pb-brain__newmsg';
  _newMsgChip.setAttribute('aria-label', 'Jump to latest message');
  _newMsgChip.innerHTML = '&#8595;&#xFE0E; New message';
  if (brain) brain.appendChild(_newMsgChip);
  _newMsgChip.addEventListener('click', function() {
    if (log) log.scrollTop = log.scrollHeight;
    _newMsgChip.classList.remove('pb-brain__newmsg--vis');
  });
  if (log) log.addEventListener('scroll', function() {
    if (_nearBottom()) _newMsgChip.classList.remove('pb-brain__newmsg--vis');
  });

  // Scroll progress strip — sticky 2px bar at top of log; gold fill shows % scrolled.
  var _scrollProg = document.createElement('div');
  _scrollProg.className = 'pb-brain__scrollprog';
  _scrollProg.setAttribute('aria-hidden', 'true');
  var _scrollProgBar = document.createElement('div');
  _scrollProgBar.className = 'pb-brain__scrollprog-bar';
  _scrollProg.appendChild(_scrollProgBar);
  if (log) log.insertBefore(_scrollProg, log.firstChild);
  function _updateScrollProg() {
    if (!log) return;
    var range = log.scrollHeight - log.clientHeight;
    if (range < 40) { _scrollProg.style.display = 'none'; return; }
    _scrollProg.style.display = '';
    _scrollProgBar.style.width = Math.min(100, Math.round(log.scrollTop / range * 100)) + '%';
  }
  if (log) log.addEventListener('scroll', _updateScrollProg, { passive: true });

  // SR-only live region — screen readers announce new bot replies automatically.
  var _srLive = document.createElement('div');
  _srLive.className = 'pb-brain__sr-live';
  _srLive.setAttribute('aria-live', 'polite');
  _srLive.setAttribute('aria-atomic', 'true');
  _srLive.setAttribute('aria-relevant', 'additions text');
  if (brain) brain.appendChild(_srLive);

  // Textarea auto-resize — grows up to 120px, resets to 1 row on submit.
  // Character counter shows "n / 2000" and turns amber near the limit.
  if (input) {
    input.style.overflowY = 'hidden';
    var charCount = document.createElement('span');
    charCount.className = 'pb-brain__char-count';
    charCount.textContent = '0 / 2000';
    charCount.setAttribute('aria-live', 'polite');
    charCount.setAttribute('aria-atomic', 'true');
    if (input.parentNode) input.parentNode.insertBefore(charCount, input.nextSibling);
    function resizeInput() {
      input.style.height = 'auto';
      input.style.height = Math.min(input.scrollHeight, 120) + 'px';
      var len = input.value.length;
      charCount.textContent = len + ' / 2000';
      charCount.classList.toggle('pb-brain__char-count--warn', len >= 1800);
    }
    input.addEventListener('input', resizeInput);

    // Rotate placeholder to inspire questions — only when input is empty and unfocused.
    var phList = [
      'What do you want to build?',
      'What does a marketplace cost?',
      'How long for an AI chatbot?',
      'Can you build in 3D / WebGL?',
      'What is AEO and why does it matter?',
      'Do you handle SEO + Core Web Vitals?',
      'How does milestone payment work?',
    ];
    var phIdx = 0;
    setInterval(function() {
      if (document.activeElement !== input && !input.value) {
        phIdx = (phIdx + 1) % phList.length;
        input.placeholder = phList[phIdx];
      }
    }, 3500);
    // ↑/↓ history nav — terminal-style cycling through sent messages on empty input.
    // ↑ enters history mode (going oldest first); ↓ exits back toward present.
    // Multiline input (Shift+Enter newlines) is left alone so caret can move between lines.
    var _histIdx = -1;
    input.addEventListener('keydown', function(e) {
      if (e.key === 'ArrowUp') {
        if (input.value.indexOf('\n') !== -1) return;        // multiline: let textarea handle
        if (input.value.trim() !== '' && _histIdx === -1) return; // typed content: don't clobber
        var _hm = chatMsgs.filter(function(m) { return m.cls === 'user'; });
        if (!_hm.length) return;
        e.preventDefault();
        if (_histIdx === -1) _histIdx = _hm.length;          // start past newest
        _histIdx = Math.max(0, _histIdx - 1);
        input.value = _hm[_histIdx].text;
        input.dispatchEvent(new Event('input'));
        input.selectionStart = input.selectionEnd = input.value.length;
      } else if (e.key === 'ArrowDown' && _histIdx !== -1) {
        var _hm = chatMsgs.filter(function(m) { return m.cls === 'user'; });
        e.preventDefault();
        _histIdx++;
        if (_histIdx >= _hm.length) { _histIdx = -1; input.value = ''; input.dispatchEvent(new Event('input')); }
        else { input.value = _hm[_histIdx].text; input.dispatchEvent(new Event('input')); input.selectionStart = input.selectionEnd = input.value.length; }
      }
    });
    // Enter sends; Shift+Enter inserts a newline (textarea default blocked).
    input.addEventListener('keydown', function(e) {
      if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.altKey && !e.metaKey) {
        e.preventDefault();
        if (input.value.trim() && form) {
          form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
        }
      }
      // Ctrl+K — clear chat (power-user shortcut; mirrored in help panel).
      if (e.ctrlKey && (e.key === 'k' || e.key === 'K')) {
        e.preventDefault();
        clearChat();
      }
      // Ctrl+E — export chat as Markdown
      if (e.ctrlKey && (e.key === 'e' || e.key === 'E')) {
        e.preventDefault();
        if (typeof exportChat === 'function') exportChat();
      }
    });
    // "Enter ↵ to send" hint — reveals after 800ms typing pause, hides on blur or submit.
    var _sendHint = document.createElement('span');
    _sendHint.className = 'pb-brain__send-hint';
    _sendHint.setAttribute('aria-hidden', 'true');
    _sendHint.innerHTML = 'Enter &#8629; to send &nbsp;&middot;&nbsp; Shift+Enter for new line';
    if (form) form.appendChild(_sendHint);
    var _hintTimer = null;
    input.addEventListener('input', function() {
      clearTimeout(_hintTimer);
      _sendHint.classList.remove('pb-brain__send-hint--vis');
      if (input.value.trim()) {
        _hintTimer = setTimeout(function() { _sendHint.classList.add('pb-brain__send-hint--vis'); }, 800);
      }
      // Typing glow: gold border tint while there is content in the textarea.
      input.classList.toggle('pb-brain__input--typing', input.value.length > 0);
    });
    input.addEventListener('blur', function() {
      clearTimeout(_hintTimer); _sendHint.classList.remove('pb-brain__send-hint--vis');
      input.classList.remove('pb-brain__input--typing');
    });
    // Character count indicator — hidden below 100 chars; shows "N left" as user nears limit.
    var _charLimit = 500;
    var _charCount = document.createElement('span');
    _charCount.className = 'pb-brain__charcount';
    _charCount.setAttribute('aria-live', 'polite');
    _charCount.setAttribute('aria-atomic', 'true');
    _charCount.style.display = 'none';
    if (form) form.appendChild(_charCount);
    function _updateCharCount() {
      var n = input.value.length;
      var rem = _charLimit - n;
      if (n < 100) { _charCount.style.display = 'none'; return; }
      _charCount.style.display = '';
      _charCount.textContent = rem >= 0 ? rem + ' left' : Math.abs(rem) + ' over limit';
      _charCount.classList.toggle('pb-brain__charcount--warn', rem < 100 && rem >= 0);
      _charCount.classList.toggle('pb-brain__charcount--over', rem < 0);
      var _sb = form ? form.querySelector('[type="submit"]') : null;
      if (_sb) _sb.disabled = rem < 0;
    }
    input.addEventListener('input', _updateCharCount);
  }

  // (Auto-open removed — the chat opens only when the visitor clicks the orb,
  //  so it never starts talking on its own on page load.)

  function _greetUser() {
    if (!log) return;
    var _hr = new Date().getHours();
    var _tod = _hr < 12 ? 'Good morning' : _hr < 17 ? 'Good afternoon' : 'Good evening';
    addMsg(_tod + "! I'm Photon — what are you building today?", 'bot');
    // R55: Empty-state suggestion tiles—shown before the user sends anything.
    var _eh = document.createElement('div');
    _eh.className = 'pb-brain__empty-hints';
    _eh.setAttribute('aria-label', 'Try asking about…');
    var _ehItems = [
      { emoji: '&#128736;', label: 'What can you build for me?' },
      { emoji: '&#128176;', label: 'What do your services cost?' },
      { emoji: '&#128247;', label: 'Show me your portfolio' },
      { emoji: '&#128269;', label: 'Tell me about SEO' },
    ];
    _ehItems.forEach(function(item) {
      var _ehBtn = document.createElement('button');
      _ehBtn.type = 'button'; _ehBtn.className = 'pb-brain__eh-tile';
      _ehBtn.innerHTML = '<span class="pb-brain__eh-icon">' + item.emoji + '</span><span class="pb-brain__eh-label">' + item.label + '</span>';
      _ehBtn.addEventListener('click', function() {
        _eh.remove();
        if (input && form) { input.value = item.label; form.dispatchEvent(new Event('submit', { bubbles: true })); }
      });
      _eh.appendChild(_ehBtn);
    });
    log.appendChild(_eh);
    requestAnimationFrame(function() { requestAnimationFrame(function() { _eh.classList.add('pb-brain__eh--vis'); }); });
  }

  function _showToast(msg) {
    if (!brain) return;
    var _t = document.createElement('div');
    _t.className = 'pb-brain__toast';
    _t.setAttribute('role', 'status');
    _t.setAttribute('aria-live', 'polite');
    _t.textContent = msg;
    brain.appendChild(_t);
    requestAnimationFrame(function() { requestAnimationFrame(function() { _t.classList.add('pb-brain__toast--vis'); }); });
    setTimeout(function() {
      _t.classList.remove('pb-brain__toast--vis');
      setTimeout(function() { if (_t.parentNode) _t.parentNode.removeChild(_t); }, 280);
    }, 1700);
  }

  var _origTitle = document.title;  // captured once at widget-init time
  var _tabUnread = 0;               // new-message tab-title counter
  var _unreadCount = 0;
  function _updateOrbBadge() {
    if (!orb) return;
    var _badge = orb.querySelector('.pb-orb__badge');
    if (!_badge) {
      _badge = document.createElement('span');
      _badge.className = 'pb-orb__badge';
      _badge.setAttribute('aria-label', '0 unread messages');
      orb.appendChild(_badge);
    }
    _badge.textContent = _unreadCount > 9 ? '9+' : (_unreadCount > 0 ? String(_unreadCount) : '');
    _badge.setAttribute('aria-label', _unreadCount + ' unread message' + (_unreadCount !== 1 ? 's' : ''));
    _badge.classList.toggle('pb-orb__badge--vis', _unreadCount > 0);
  }

  function openBrain() {
    if (!brain) return;
    brain.hidden = false;
    document.documentElement.classList.add('pb-brain-open');
    // Restore chips if the log only has the initial bot message (fresh session)
    var chips = brain.querySelector('.pb-brain__chips');
    if (chips && log && log.querySelectorAll('.pb-brain__msg').length <= 1) {
      chips.style.display = '';
    }
    if (chatMsgs.length === 0 && log && log.querySelectorAll('.pb-brain__msg').length === 0) {
      _greetUser();
    }
    _unreadCount = 0;
    _updateOrbBadge();
    if (_tabUnread > 0) { _tabUnread = 0; document.title = _origTitle; }
    if (input) input.focus();
  }

  // Restore tab title when user returns from another tab.
  document.addEventListener('visibilitychange', function() {
    if (!document.hidden && _tabUnread > 0) { _tabUnread = 0; document.title = _origTitle; }
  });

  function closeBrain() {
    if (!brain) return;
    brain.hidden = true;
    document.documentElement.classList.remove('pb-brain-open');
    _updateOrbBadge();
  }

  openBtns.forEach(btn => btn.addEventListener('click', function() { openBrain(); _updateOrbBadge(); }));
  closeBtns.forEach(btn => btn.addEventListener('click', closeBrain));

  // Ctrl+/ (or Cmd+/) toggles the chatbot
  document.addEventListener('keydown', function(e) {
    if ((e.ctrlKey || e.metaKey) && e.key === '/') {
      e.preventDefault();
      if (brain && !brain.hidden) { closeBrain(); } else { openBrain(); }
    }
  });

  // Close on backdrop click or Escape
  if (brain) {
    brain.addEventListener('click', e => {
      if (e.target === brain) closeBrain();
    });
    document.addEventListener('keydown', e => {
      if (e.key === 'Escape' && !brain.hidden) closeBrain();
    });
  }

  // Focus trap — Tab/Shift+Tab cycles within the open drawer (WCAG 2.1 SC 2.1.2).
  if (brain) {
    brain.addEventListener('keydown', function(e) {
      if (e.key !== 'Tab' || brain.hidden) return;
      var _foc = Array.prototype.slice.call(
        brain.querySelectorAll('button:not([disabled]), input:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])')
      ).filter(function(el) { return el.offsetParent !== null; });
      if (!_foc.length) return;
      var _first = _foc[0];
      var _last = _foc[_foc.length - 1];
      if (e.shiftKey) {
        if (document.activeElement === _first) { e.preventDefault(); _last.focus(); }
      } else {
        if (document.activeElement === _last) { e.preventDefault(); _first.focus(); }
      }
    });
  }

  // Orb z-index fix
  if (orb) {
    orb.style.zIndex = '99999';
    orb.style.position = 'fixed';
    orb.setAttribute('title', 'Chat with Photon · Ctrl+/');
  }

  // Form submission + two-way voice
  if (form && input && log) {
    const restUrl = window.PB_AURORA?.restUrl || '';
    // Prefer the absolute REST URL embedded on the form (like the subscribe form),
    // so the endpoint resolves even if PB_AURORA isn't localized on the live host.
    const endpoint = form.dataset.pbRest || (restUrl + 'pb/v1/brainstorm');
    const voiceEndpoint = restUrl ? (restUrl + 'pb/v1/voice') : '';
    let history = [];
    let voiceMode = false;   // becomes true once the visitor talks -> bot replies aloud
    let listening = false;
    let currentAudio = null;
    var chatMsgs = []; // persisted to sessionStorage for cross-reload chat restore
    var _mcEl = null;  // message-count badge element (set after brainHead is ready)
    var _lastSendTime = 0; // timestamp of last user send; cleared after bot responds
    // R54: Session statistics accumulators
    var _statsSessionStart = Date.now();
    var _statsRespTimes = [];
    var _statsBotWords = 0;

    // Strip HTML to plain text (for TTS + history).
    function plain(html) {
      const d = document.createElement('div');
      d.innerHTML = html;
      return (d.textContent || d.innerText || '').replace(/\s+/g, ' ').trim();
    }

    // Bot replies may be HTML (local responder) or markdown (LLM). Normalize either
    // into safe HTML so nothing renders as run-on text or literal ** / \n.
    // Code blocks and inline code are extracted before HTML-escaping so their
    // content is preserved verbatim, then restored as styled <pre>/<code> elements.
    function format(text) {
      if (/<(a|strong|br|ul|ol|li|p|em)\b/i.test(text)) return text;
      var _codeBlocks = [], _inlineCodes = [];
      text = text.replace(/```[\w]*\n?([\s\S]*?)```/g, function(_, code) {
        var safe = code.trim().replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
        _codeBlocks.push('<pre class="pb-brain__code-block"><code>' + safe + '</code></pre>');
        return '\x00CB' + (_codeBlocks.length - 1) + '\x00';
      });
      text = text.replace(/`([^`\n]+)`/g, function(_, code) {
        var safe = code.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
        _inlineCodes.push('<code class="pb-brain__code-inline">' + safe + '</code>');
        return '\x00IC' + (_inlineCodes.length - 1) + '\x00';
      });
      var _listBlocks = [];
      var _fmtItem = function(s) {
        return s
          .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
          .replace(/\*\*(.+?)\*\*/g,'<strong>$1</strong>')
          .replace(/(?<!\w)_([^_\n]+)_(?!\w)/g,'<em>$1</em>')
          .replace(/~~([^~\n]+)~~/g,'<del>$1</del>')
          .replace(/\[([^\]]+)\]\((https?:[^)]+)\)/g,'<a href="$2" target="_blank" rel="noopener">$1</a>');
      };
      text = text.replace(/((?:^|\n)\d+\. [^\n]+)+/g, function(block) {
        var html = '<ol class="pb-brain__ol">' + block.trim().split('\n').map(function(l) {
          return '<li class="pb-brain__li">' + _fmtItem(l.replace(/^\d+\. /, '')) + '</li>';
        }).join('') + '</ol>';
        _listBlocks.push(html); return '\x00LB' + (_listBlocks.length - 1) + '\x00';
      });
      text = text.replace(/((?:^|\n)[*-] [^\n]+)+/g, function(block) {
        var html = '<ul class="pb-brain__ul">' + block.trim().split('\n').map(function(l) {
          return '<li class="pb-brain__li">' + _fmtItem(l.replace(/^[*-] /, '')) + '</li>';
        }).join('') + '</ul>';
        _listBlocks.push(html); return '\x00LB' + (_listBlocks.length - 1) + '\x00';
      });
      var _tblBlocks = [];
      text = text.replace(/((?:(?:^|\n)\|[^\n]+)+)/g, function(block) {
        var rows = block.trim().split('\n').filter(Boolean);
        var sepIdx = -1;
        for (var _ri = 0; _ri < rows.length; _ri++) {
          if (/^\|[-:| ]+\|$/.test(rows[_ri].trim())) { sepIdx = _ri; break; }
        }
        if (sepIdx < 1 || !rows.slice(sepIdx + 1).some(function(r) { return r.indexOf('|') > -1; })) return block;
        var _tcols = function(row) { return row.trim().replace(/^\||\|$/g, '').split('|').map(function(c) { return c.trim(); }); };
        var _tHtml = '<table class="pb-brain__tbl"><thead><tr>' +
          _tcols(rows[sepIdx - 1]).map(function(c) { return '<th>' + _fmtItem(c) + '</th>'; }).join('') +
          '</tr></thead><tbody>' +
          rows.slice(sepIdx + 1).filter(function(r) { return r.trim() && r.indexOf('|') > -1; }).map(function(row) {
            return '<tr>' + _tcols(row).map(function(c) { return '<td>' + _fmtItem(c) + '</td>'; }).join('') + '</tr>';
          }).join('') + '</tbody></table>';
        _tblBlocks.push(_tHtml); return '\x00TB' + (_tblBlocks.length - 1) + '\x00';
      });
      text = text
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/(?<!\w)_([^_\n]+)_(?!\w)/g, '<em>$1</em>')
        .replace(/~~([^~\n]+)~~/g, '<del>$1</del>')
        .replace(/^&gt;\s*(.+)/gm, '<blockquote class="pb-brain__blockquote">$1</blockquote>')
        .replace(/\[([^\]]+)\]\((https?:[^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
        .replace(/(^|[\s(])((?:https?:\/\/|www\.)[^\s<)]+)/g, '$1<a href="$2" target="_blank" rel="noopener">$2</a>')
        .replace(/\n/g, '<br>');
      _codeBlocks.forEach(function(h, i) { text = text.replace('\x00CB' + i + '\x00', h); });
      _inlineCodes.forEach(function(h, i) { text = text.replace('\x00IC' + i + '\x00', h); });
      _listBlocks.forEach(function(h, i) { text = text.replace('\x00LB' + i + '\x00', h); });
      _tblBlocks.forEach(function(h, i) { text = text.replace('\x00TB' + i + '\x00', h); });
      return text;
    }

    // R56: Auto-link bare https?:// URLs in bot response HTML without double-linking.
    function _autoLink(html) {
      return html.replace(/(<a\b[^>]*>[\s\S]*?<\/a>|<[^>]+>)|(https?:\/\/[\w\-\.\/\?\#\=\&\%\+\:\~@,;!\*]+[\w\/\?])/gi, function(m, tag, url) {
        return tag ? tag : '<a class="pb-brain__autolink" href="' + url + '" target="_blank" rel="noopener noreferrer">' + url + '</a>';
      });
    }

    function addMsg(text, cls) {
      const div = document.createElement('div');
      div.className = 'pb-brain__msg pb-brain__msg--' + (cls === 'bot' ? 'bot' : cls === 'err' ? 'err' : 'me');
      if (cls !== 'err') div.classList.add('pb-brain__msg--new');
      if (cls === 'bot') {
        var _fmtLines = _autoLink(format(text)).split('<br>');  // R56: auto-link URLs
        var _textBody = document.createElement('div');
        _textBody.className = 'pb-brain__text';
        var _lh = '';
        _fmtLines.forEach(function(ln, i) {
          var _trimLn = ln.trimStart();
          var isBlk = /^<(pre|ol|ul|blockquote|table)[\s>]/.test(_trimLn);
          var isPreBlk = _trimLn.startsWith('<pre');
          if (!isBlk && i > 0 && !/^<(pre|ol|ul|blockquote|table)[\s>]/.test(_fmtLines[i-1].trimStart())) _lh += '<br>';
          _lh += isPreBlk
            ? '<div class="pb-brain__line pb-brain__line--code">' + ln + '</div>'
            : isBlk ? ln
            : '<span class="pb-brain__line" style="animation-delay:' + (i * 70) + 'ms">' + ln + '</span>';
        });
        _textBody.innerHTML = _lh;
        div.appendChild(_textBody);
      } else {
        div.innerHTML = text.replace(/</g, '&lt;');
      }
      if (cls === 'bot' && navigator.clipboard) {
        var cpBtn = document.createElement('button');
        cpBtn.type = 'button';
        cpBtn.className = 'pb-brain__copy';
        cpBtn.title = 'Copy reply';
        cpBtn.setAttribute('aria-label', 'Copy reply');
        cpBtn.innerHTML = '&#128203;';
        cpBtn.addEventListener('click', function() {
          navigator.clipboard.writeText(plain(text)).then(function() {
            cpBtn.innerHTML = '&#10003;';
            _showToast('Copied to clipboard');
            setTimeout(function() { cpBtn.innerHTML = '&#128203;'; }, 1500);
          });
        });
        div.appendChild(cpBtn);
      }
      // "Was this helpful?" thumbs — hidden until hover; GA4 event on selection
      if (cls === 'bot') {
        var _fbDiv = document.createElement('div');
        _fbDiv.className = 'pb-brain__fb';
        _fbDiv.setAttribute('aria-label', 'Was this helpful?');
        var _upBtn = document.createElement('button');
        _upBtn.type = 'button'; _upBtn.className = 'pb-brain__fb-btn';
        _upBtn.setAttribute('aria-label', 'Helpful'); _upBtn.innerHTML = '&#128077;';
        var _dnBtn = document.createElement('button');
        _dnBtn.type = 'button'; _dnBtn.className = 'pb-brain__fb-btn';
        _dnBtn.setAttribute('aria-label', 'Not helpful'); _dnBtn.innerHTML = '&#128078;';
        var _pbFbLock = function(chosen) {
          [_upBtn, _dnBtn].forEach(function(b) { b.disabled = true; b.style.opacity = b === chosen ? '1' : '0.2'; });
          try { if (window.gtag) window.gtag('event', 'chat_feedback', { value: chosen === _upBtn ? 1 : 0, event_category: 'chatbot' }); } catch(e) {}
        };
        _upBtn.addEventListener('click', function() { _pbFbLock(_upBtn); });
        _dnBtn.addEventListener('click', function() { _pbFbLock(_dnBtn); });
        _fbDiv.appendChild(_upBtn); _fbDiv.appendChild(_dnBtn);
        div.appendChild(_fbDiv);
        // Bookmark / star button — toggled via aria-pressed; starred state persists to sessionStorage.
        var _starBtn = document.createElement('button');
        _starBtn.type = 'button'; _starBtn.className = 'pb-brain__star';
        _starBtn.title = 'Bookmark this reply';
        _starBtn.setAttribute('aria-label', 'Bookmark reply');
        _starBtn.setAttribute('aria-pressed', 'false');
        _starBtn.innerHTML = '&#9733;';
        _starBtn.addEventListener('click', function() {
          var _on = div.classList.toggle('pb-brain__msg--starred');
          _starBtn.setAttribute('aria-pressed', String(_on));
          _starBtn.classList.toggle('pb-brain__star--on', _on);
          try {
            var _stars = JSON.parse(sessionStorage.getItem('pb_stars_v1') || '[]');
            var _key = plain(text).slice(0, 80);
            if (_on) { if (_stars.indexOf(_key) < 0) _stars.push(_key); }
            else { _stars = _stars.filter(function(s) { return s !== _key; }); }
            sessionStorage.setItem('pb_stars_v1', JSON.stringify(_stars));
          } catch(e) {}
        });
        div.appendChild(_starBtn);
      }
      // Retry button on API error messages
      if (cls === 'err') {
        var _retryBtn = document.createElement('button');
        _retryBtn.type = 'button';
        _retryBtn.className = 'pb-brain__retry';
        _retryBtn.innerHTML = '&#8635; Retry';
        _retryBtn.setAttribute('aria-label', 'Retry last message');
        _retryBtn.addEventListener('click', function() {
          var _userMsgs = log.querySelectorAll('.pb-brain__msg--me');
          var _lastUserMsg = _userMsgs[_userMsgs.length - 1];
          var _lastUserText = _lastUserMsg ? _lastUserMsg.textContent.trim() : '';
          div.remove();
          if (_lastUserMsg) _lastUserMsg.remove();
          for (var _ri = chatMsgs.length - 1; _ri >= 0; _ri--) {
            if (chatMsgs[_ri].cls === 'user') { chatMsgs.splice(_ri, 1); break; }
          }
          saveChat();
          if (_lastUserText && input && form) {
            input.value = _lastUserText;
            form.dispatchEvent(new Event('submit', { bubbles: true }));
          }
        });
        div.appendChild(_retryBtn);
      }
      // Timestamp badge — revealed on hover via CSS opacity transition
      var _tsEl = document.createElement('time');
      _tsEl.className = 'pb-brain__ts';
      var _tn = new Date();
      _tsEl.textContent = 'just now';
      _tsEl.title = _tn.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + ' · ' + _tn.toLocaleDateString();
      _tsEl.setAttribute('datetime', _tn.toISOString());
      div.appendChild(_tsEl);
      // Response-time badge on live bot replies (not session-restore; guard: < 30s elapsed).
      if (cls === 'bot' && _lastSendTime > 0 && (Date.now() - _lastSendTime) < 30000) {
        var _rtMs = Date.now() - _lastSendTime;
        _statsRespTimes.push(_rtMs);
        var _rtEl = document.createElement('small');
        _rtEl.className = 'pb-brain__resp-time';
        _rtEl.textContent = '⚡ ' + (_rtMs / 1000).toFixed(1) + 's';
        div.appendChild(_rtEl);
        _lastSendTime = 0;
      }
      // Word count badge on bot replies — gives a sense of response length at a glance.
      if (cls === 'bot') {
        var _wcWords = plain(text).trim().split(/\s+/).filter(Boolean).length;
        _statsBotWords += _wcWords;
        if (_wcWords > 0) {
          var _wcBadge = document.createElement('span');
          _wcBadge.className = 'pb-brain__wc';
          var _wcRt = _wcWords >= 200 ? ' · ~' + Math.ceil(_wcWords / 200) + ' min' : '';
          _wcBadge.textContent = _wcWords + ' word' + (_wcWords !== 1 ? 's' : '') + _wcRt;
          _wcBadge.setAttribute('aria-hidden', 'true');
          div.appendChild(_wcBadge);
        }
        // Collapse long replies (6+ line-spans) behind a "Show more ↓" toggle.
        // _textBody is the div.pb-brain__text wrapper created at addMsg() top;
        // the toggle is appended to div (outside the wrapper) so overflow:hidden never clips it.
        var _tbody = div.querySelector('.pb-brain__text');
        var _tbLines = _tbody ? _tbody.querySelectorAll('.pb-brain__line') : [];
        if (_tbody && _tbLines.length > 5) {
          _tbody.classList.add('pb-brain__text--collapsed');
          var _expandBtn = document.createElement('button');
          _expandBtn.type = 'button';
          _expandBtn.className = 'pb-brain__toggle';
          _expandBtn.textContent = 'Show more ↓';
          _expandBtn.setAttribute('aria-expanded', 'false');
          var _colOpen = false;
          _expandBtn.addEventListener('click', function() {
            _colOpen = !_colOpen;
            _tbody.classList.toggle('pb-brain__text--collapsed', !_colOpen);
            _expandBtn.textContent = _colOpen ? 'Show less ↑' : 'Show more ↓';
            _expandBtn.setAttribute('aria-expanded', String(_colOpen));
            if (_colOpen) { requestAnimationFrame(function() { log.scrollTo({ top: log.scrollHeight, behavior: 'smooth' }); }); }
          });
          div.appendChild(_expandBtn);
        }
      }
      // R55: Remove empty-state hints on first user message
      if (cls === 'user') { var _ehEl = log.querySelector('.pb-brain__empty-hints'); if (_ehEl) _ehEl.remove(); var _ctaEl = log.querySelector('.pb-brain__cta-card'); if (_ctaEl) _ctaEl.remove(); }
      log.appendChild(div);
      // After the 2nd bot message (first substantive reply), inject follow-up action chips.
      if (cls === 'bot') {
        var botMsgs = log.querySelectorAll('.pb-brain__msg--bot');
        if (botMsgs.length === 2 && !log.querySelector('.pb-brain__chips-followup')) {
          var fu = document.createElement('div');
          fu.className = 'pb-brain__chips pb-brain__chips-followup';
          fu.setAttribute('aria-label', 'Quick actions');
          // Contextual chips — swap out the chip that matches what the user already asked about.
          var h0 = (log.querySelector('.pb-brain__msg--me') || {}).textContent || '';
          h0 = h0.toLowerCase();
          var chipDefs = [
            { txt: '&#128197; Book a call',   send: 'Book a free 15-min call' },
            { txt: '&#128176; All pricing',   send: 'What is the full pricing list?' },
            { txt: '&#127912; Portfolio',     send: 'Show me your portfolio and past work' },
          ];
          if (/pric|cost|how much|budget|charg/.test(h0))             chipDefs[1] = { txt: '&#9201; How long?',      send: 'How long does a typical project take from start to launch?' };
          if (/portfolio|past work|example|case studi|sample/.test(h0)) chipDefs[2] = { txt: '&#128338; Timeline',     send: 'How long does a typical project take from start to launch?' };
          if (/book|call|meet|schedule|talk|appointment/.test(h0))    chipDefs[0] = { txt: '&#128179; Payment',      send: 'How does payment work — do you require a deposit?' };
          fu.innerHTML = chipDefs.map(function(c) {
            return '<button type="button" class="pb-brain__chip" data-chip="' + c.send.replace(/"/g,'&quot;') + '">' + c.txt + '</button>';
          }).join('');
          fu.classList.add('pb-chips-entering');
          log.appendChild(fu);
          requestAnimationFrame(function() { requestAnimationFrame(function() { fu.classList.remove('pb-chips-entering'); }); });
          fu.querySelectorAll('.pb-brain__chip').forEach(function(chip) {
            chip.addEventListener('click', function() {
              if (!input || !form) return;
              input.value = chip.dataset.chip;
              fu.style.display = 'none';
              form.dispatchEvent(new Event('submit', { bubbles: true }));
            });
          });
        }
      }
      // R53: Smart per-response suggestion chips — context-aware follow-ups after every bot reply.
      if (cls === 'bot') {
        var _oldSmart = log.querySelector('.pb-brain__chips-smart');
        if (_oldSmart) _oldSmart.remove();
        var _smartChipsData = [
          { k: ['price','cost','invest','budget','charg','fee','from $'], c: ['How long will it take?', 'Can I see examples of your work?', 'What’s included in the price?'] },
          { k: ['seo','rank','google','search engine','keyword'], c: ['How long to see SEO results?', 'Do you write the content?', 'What about Google Ads?'] },
          { k: ['ecommerce','shop','woocommerce','shopify','stripe','checkout'], c: ['Which platform do you recommend?', 'Can I manage products myself?', 'What payment methods can I accept?'] },
          { k: ['booking','appointment','calendar','schedule','reservation'], c: ['Which booking system do you use?', 'Can it send automated reminders?', 'Does it sync with Google Calendar?'] },
          { k: ['wedding','venue','ceremony','celebration'], c: ['Do you have a weddings portfolio?', 'What’s your booking lead time?', 'Do you offer day-of coordination?'] },
          { k: ['maintenance','support','update','hosting','domain'], c: ['What’s included in ongoing support?', 'Do you offer managed hosting?', 'How do I update content myself?'] },
          { k: ['mobile','responsive','phone','speed','performance','core web'], c: ['How do you test on mobile?', 'What about page speed scores?', 'Do you optimise images automatically?'] },
          { k: ['brand','logo','design','colour','font','identity'], c: ['Do you offer logo design?', 'How many design revisions are included?', 'What if I already have a logo?'] },
          { k: ['social','instagram','facebook','twitter','linkedin'], c: ['Do you manage social media too?', 'Can I embed an Instagram feed?', 'What about Open Graph previews?'] },
          { k: ['architect','planning','building','extension','loft'], c: ['What’s your planning approval rate?', 'Do you handle listed buildings?', 'What RIBA stages do you cover?'] },
          { k: ['interior','decorator','stager','staging','e-design'], c: ['Do you offer virtual e-design?', 'Can I see your portfolio?', 'What\'s the discovery call process?'] },
          { k: ['florist','flower','bouquet','arrangement'], c: ['Do you deliver same-day?', 'Can I see your floral portfolio?', 'Do you offer flower subscriptions?'] },
          { k: ['childminder','nursery','childcare','child','ofsted'], c: ['How do parents book online?', 'Can I show term-time availability?', 'How do you handle safeguarding info?'] },
          { k: ['music','teacher','tutor','lesson','instrument','singing','guitar','piano'], c: ['Can students book lessons online?', 'Do you offer trial lessons?', 'How do you show your teaching style?'] },
          { k: ['personal trainer','fitness','gym','workout','coach','nutrition','pt '], c: ['Can clients book sessions online?', 'Do you offer online coaching?', 'How do you show client results?'] },
        ];
        var _smartDefaultChips = ['How much does this cost?', 'How long does it take?', 'Can I see past work?'];
        var _lowerResp = plain(text).toLowerCase();
        var _pickedChips = _smartDefaultChips;
        for (var _scIdx = 0; _scIdx < _smartChipsData.length; _scIdx++) {
          var _kws = _smartChipsData[_scIdx].k, _hit = false;
          for (var _kIdx = 0; _kIdx < _kws.length; _kIdx++) {
            if (_lowerResp.indexOf(_kws[_kIdx]) > -1) { _hit = true; break; }
          }
          if (_hit) { _pickedChips = _smartChipsData[_scIdx].c; break; }
        }
        var _scDiv = document.createElement('div');
        _scDiv.className = 'pb-brain__chips-smart pb-brain__chips';
        _scDiv.setAttribute('aria-label', 'Suggested follow-ups');
        _pickedChips.forEach(function(label) {
          var _scBtn = document.createElement('button');
          _scBtn.type = 'button'; _scBtn.className = 'pb-brain__chip pb-brain__chip--smart';
          _scBtn.textContent = label;
          _scBtn.addEventListener('click', function() {
            _scDiv.remove();
            if (input && form) { input.value = label; form.dispatchEvent(new Event('submit', { bubbles: true })); }
          });
          _scDiv.appendChild(_scBtn);
        });
        _scDiv.classList.add('pb-chips-entering');
        log.appendChild(_scDiv);
        requestAnimationFrame(function() { requestAnimationFrame(function() { _scDiv.classList.remove('pb-chips-entering'); }); });
      }
     
      // R58: Conversion CTA card — injects a book-a-call prompt when bot reply mentions pricing or booking.
      if (cls === 'bot') {
        var _lrp = plain(text).toLowerCase();
        var _hasCta = /from \$|from £|\/month|\$\d|£\d|book a|free call|get in touch|contact us|starting from|from just|our prices|pricing|get started/.test(_lrp);
        if (_hasCta) {
          var _oldCta = log.querySelector('.pb-brain__cta-card');
          if (_oldCta) _oldCta.remove();
          var _cta = document.createElement('div');
          _cta.className = 'pb-brain__cta-card';
          _cta.innerHTML = '<span class="pb-brain__cta-msg">Ready to get started?</span><button type="button" class="pb-brain__cta-btn">Book a free 15-min call &#8599;</button>';
          _cta.querySelector('.pb-brain__cta-btn').addEventListener('click', function() {
            _cta.remove();
            if (input && form) { input.value = 'Book a free 15-min call'; form.dispatchEvent(new Event('submit', { bubbles: true })); }
          });
          log.appendChild(_cta);
          requestAnimationFrame(function() { requestAnimationFrame(function() { _cta.classList.add('pb-brain__cta-card--vis'); }); });
        }
      } if (cls !== 'err') { chatMsgs.push({ text: text, cls: cls }); saveChat(); }
      if (cls === 'bot') {
        if (brain && brain.hidden) _unreadCount++;
        _updateOrbBadge();
        // Tab-title notification: "(N) Photon Bounce" when user is on another tab.
        if (document.hidden) {
          _tabUnread++;
          document.title = '(' + _tabUnread + ') Photon Bounce';
        }
        if (!_nearBottom()) _newMsgChip.classList.add('pb-brain__newmsg--vis');
        // Push plaintext to SR live region so screen readers announce the reply.
        if (_srLive) { _srLive.textContent = ''; requestAnimationFrame(function() { _srLive.textContent = plain(text); }); }
      }
      if (_nearBottom()) log.scrollTo({ top: log.scrollHeight, behavior: 'smooth' });
      _updateMsgCount();
    }

    function saveChat() {
      try { sessionStorage.setItem('pb_chat_v1', JSON.stringify(chatMsgs)); } catch(e) {}
    }

    // Relative timestamps — rewrite pb-brain__ts textContent every 60s.
    function _relTime(iso) {
      var diff = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000));
      if (diff < 60) return 'just now';
      var m = Math.floor(diff / 60);
      if (m < 60) return m + 'm ago';
      var h = Math.floor(m / 60);
      if (h < 24) return h + 'h ago';
      return Math.floor(h / 24) + 'd ago';
    }
    function _updateTimestamps() {
      var tss = log ? log.querySelectorAll('time.pb-brain__ts') : [];
      [].forEach.call(tss, function(t) {
        var iso = t.getAttribute('datetime');
        if (iso) t.textContent = _relTime(iso);
      });
    }
    setInterval(_updateTimestamps, 60000);

    function clearChat() {
      chatMsgs = []; history = [];
      try { sessionStorage.removeItem('pb_chat_v1'); } catch(e) {}
      while (log.children.length > 1) log.removeChild(log.lastChild);
      var chips = brain.querySelector('.pb-brain__chips');
      if (chips) chips.style.display = '';
      _updateOrbBadge();
      _updateMsgCount();
      _greetUser();
    }

    // Live message-count badge in header. Hoisted as a function declaration so addMsg()
    // (defined earlier) can call it before _mcEl is assigned in the brainHead setup block.
    function _updateMsgCount() {
      if (!_mcEl) return;
      var n = chatMsgs.length;
      _mcEl.textContent = n > 0 ? n + (n === 1 ? ' message' : ' messages') : '';
    }

    function exportChat() {
      if (!chatMsgs.length) { _showToast('Nothing to export yet'); return; }
      var lines = chatMsgs.map(function(m) {
        return (m.cls === 'bot' ? '**Photon:** ' : '**You:** ') + plain(m.text);
      });
      var content = '# Photon Bounce — Chat\n\n_Exported ' + new Date().toLocaleString() + '_\n\n---\n\n' + lines.join('\n\n');
      var blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
      var url = URL.createObjectURL(blob);
      var a = document.createElement('a');
      a.href = url;
      a.download = 'pb-chat-' + new Date().toISOString().slice(0, 10) + '.md';
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
    }

    function printChat() {
      if (!chatMsgs.length) { _showToast('Nothing to print yet'); return; }
      var rows = chatMsgs.map(function(m) {
        var who = m.cls === 'bot' ? 'Photon' : 'You';
        var bg  = m.cls === 'bot' ? '#f0f4f8' : '#e8f0fe';
        return '<div style="margin:0 0 12px;padding:10px 14px;background:' + bg + ';border-radius:8px;">' +
               '<strong style="font-size:11px;text-transform:uppercase;letter-spacing:.06em;">' + who + '</strong>' +
               '<div style="margin:4px 0 0;font-size:13.5px;line-height:1.55;">' +
               m.text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') + '</div></div>';
      });
      var html = '<!DOCTYPE html><html><head><meta charset="utf-8">' +
        '<title>Photon Bounce — Chat Transcript</title>' +
        '<style>body{font-family:system-ui,sans-serif;margin:32px;color:#1a1a1a;max-width:700px}' +
        'h1{font-size:18px;margin:0 0 4px;color:#0d1b2a}' +
        'p.meta{font-size:12px;color:#666;margin:0 0 24px;border-bottom:1px solid #dde;padding-bottom:12px}' +
        '@media print{body{margin:16px}}</style></head><body>' +
        '<h1>Photon Bounce — Chat Transcript</h1>' +
        '<p class="meta">Exported ' + new Date().toLocaleString() + ' &nbsp;&middot;&nbsp; ' + chatMsgs.length + ' messages</p>' +
        rows.join('') + '</body></html>';
      var w = window.open('', '_blank', 'width=800,height=600');
      if (w) { w.document.write(html); w.document.close(); w.focus(); w.print(); }
      else { _showToast('Allow pop-ups to print'); }
    }

    function _nearBottom() { return !log || (log.scrollHeight - log.scrollTop - log.clientHeight) < 90; }

    function addTyping() {
      const div = document.createElement('div');
      div.className = 'pb-brain__msg pb-brain__msg--bot pb-brain__typing';
      div.innerHTML = '<span></span><span></span><span></span>';
      log.appendChild(div);
      if (_nearBottom()) log.scrollTo({ top: log.scrollHeight, behavior: 'smooth' });
      return div;
    }

    // --- Voice output: ElevenLabs proxy, browser TTS fallback ---
    function stopSpeaking() {
      if (currentAudio) { try { currentAudio.pause(); } catch (e) {} currentAudio = null; }
      if (window.speechSynthesis) window.speechSynthesis.cancel();
    }
    async function speak(htmlOrText) {
      if (localStorage.getItem('pb_voice_muted') === '1') return;
      const said = plain(htmlOrText);
      if (!said) return;
      stopSpeaking();
      if (voiceEndpoint) {
        try {
          const r = await fetch(voiceEndpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: said })
          });
          if (r.ok) {
            const blob = await r.blob();
            if (blob && blob.size > 0 && (blob.type || '').indexOf('audio') === 0) {
              const url = URL.createObjectURL(blob);
              currentAudio = new Audio(url);
              currentAudio.onended = () => { URL.revokeObjectURL(url); currentAudio = null; maybeListen(); };
              await currentAudio.play();
              return;
            }
          }
        } catch (e) {}
      }
      if (window.speechSynthesis) {
        const u = new SpeechSynthesisUtterance(said);
        u.rate = 1.0; u.pitch = 1.05;
        u.onend = () => maybeListen();
        window.speechSynthesis.speak(u);
      }
    }

    // Restore previous conversation from sessionStorage (survives page reload).
    (function restoreChat() {
      try {
        var saved = JSON.parse(sessionStorage.getItem('pb_chat_v1') || 'null');
        if (!Array.isArray(saved) || !saved.length) return;
        saved.forEach(function(m) { if (m.text && m.cls) addMsg(m.text, m.cls); });
      } catch(e) {}
    })();

    // --- Voice input: speech recognition ---
    // Quick-reply chips: clicking a chip fills + submits the textarea
    document.querySelectorAll('.pb-brain__chip').forEach(function(chip) {
      chip.addEventListener('click', function() {
        if (!input || !form) return;
        input.value = chip.dataset.chip || chip.textContent.replace(/^[^\w]+/, '').trim();
        form.dispatchEvent(new Event('submit', { bubbles: true }));
        // Hide chips after first use
        var chips = document.querySelector('.pb-brain__chips');
        if (chips) chips.style.display = 'none';
      });
    });

    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    let rec = null;
    // Detect Cyrillic input and switch recognition language accordingly
    function detectLang(text) {
      return /[Ѐ-ӿ]/.test(text) ? 'ru-RU' : 'en-US';
    }
    // Visual "Listening…" indicator injected into log when voice is active.
    function showListeningBanner() {
      if (!log || log.querySelector('.pb-brain__listening')) return;
      var banner = document.createElement('div');
      banner.className = 'pb-brain__listening';
      banner.setAttribute('aria-live', 'assertive');
      banner.innerHTML = '<span class="pb-brain__listen-dot"></span>Listening…';
      log.appendChild(banner);
      if (_nearBottom()) log.scrollTo({ top: log.scrollHeight, behavior: 'smooth' });
    }
    function hideListeningBanner() {
      if (!log) return;
      var banner = log.querySelector('.pb-brain__listening');
      if (banner) banner.remove();
    }
    if (SR && micBtn) {
      rec = new SR();
      rec.continuous = false;
      rec.interimResults = false;
      rec.lang = (navigator.language || '').toLowerCase().startsWith('ru') ? 'ru-RU' : 'en-US';
      rec.onresult = e => {
        const transcript = e.results[0][0].transcript;
        input.value = transcript;
        listening = false;
        micBtn.classList.remove('is-recording');
        hideListeningBanner();
        voiceMode = true;
        rec.lang = detectLang(transcript);
        form.dispatchEvent(new Event('submit', { bubbles: true }));
      };
      rec.onerror = () => { listening = false; micBtn.classList.remove('is-recording'); hideListeningBanner(); };
      rec.onend = () => { listening = false; micBtn.classList.remove('is-recording'); hideListeningBanner(); };
      micBtn.addEventListener('click', () => {
        if (listening) { try { rec.stop(); } catch (e) {} listening = false; voiceMode = false; micBtn.classList.remove('is-recording'); hideListeningBanner(); stopSpeaking(); return; }
        stopSpeaking();
        try { rec.start(); listening = true; voiceMode = true; micBtn.classList.add('is-recording'); showListeningBanner(); } catch (e) {}
      });
    } else if (micBtn) {
      micBtn.style.display = 'none';
    }
    // After the bot finishes speaking, listen again for a hands-free back-and-forth.
    function maybeListen() {
      if (!rec || !voiceMode || !brain || brain.hidden) return;
      setTimeout(() => {
        if (voiceMode && !listening && brain && !brain.hidden) {
          try { rec.start(); listening = true; micBtn.classList.add('is-recording'); showListeningBanner(); } catch (e) {}
        }
      }, 400);
    }

    // API fetch progress bar — thin gold line at panel top during waiting.
    var _progressBar = document.createElement('div');
    _progressBar.className = 'pb-brain__progress';
    _progressBar.setAttribute('aria-hidden', 'true');
    if (brain) brain.prepend(_progressBar);
    function _progressStart() {
      _progressBar.style.width = '0%';
      _progressBar.style.opacity = '1';
      _progressBar.classList.add('pb-brain__progress--run');
    }
    function _progressDone() {
      _progressBar.classList.remove('pb-brain__progress--run');
      _progressBar.style.width = '100%';
      setTimeout(function() { _progressBar.style.opacity = '0'; }, 200);
      setTimeout(function() { _progressBar.style.width = '0%'; }, 600);
    }
    function _confetti() {
      var cols = ['#ffd400','rgba(255,212,0,.8)','#fff','rgba(255,255,255,.7)','rgba(255,180,0,.9)'];
      for (var i = 0; i < 18; i++) {
        var p = document.createElement('div');
        p.className = 'pb-brain__confetti';
        p.style.left = (Math.random() * 100) + '%';
        p.style.animationDelay = (Math.random() * 0.55) + 's';
        p.style.background = cols[i % cols.length];
        var sz = (4 + Math.random() * 5) + 'px';
        p.style.width = sz; p.style.height = sz;
        document.body.appendChild(p);
        setTimeout(function(el) { if (el.parentNode) el.parentNode.removeChild(el); }, 2200, p);
      }
    }

    form.addEventListener('submit', async e => {
      e.preventDefault();
      const text = input.value.trim();
      if (!text) return;
      _lastSendTime = Date.now();
      addMsg(text, 'user');
      if (chatMsgs.filter(function(m) { return m.cls === 'user'; }).length === 1) { _confetti(); }
      input.value = '';
      _histIdx = -1;
      input.style.height = 'auto';
      clearTimeout(_hintTimer); _sendHint.classList.remove('pb-brain__send-hint--vis');
      const sendBtn = form.querySelector('[type="submit"]');
      const typing = addTyping();
      input.disabled = true;
      var _savedTitle = document.title;
      document.title = 'Photon is thinking…';
      if (sendBtn) { sendBtn.disabled = true; sendBtn.textContent = '…'; }
      _progressStart();
      try {
        const r = await fetch(endpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ message: text, history: history, path: location.pathname, title: _savedTitle })
        });
        typing.remove();
        const data = await r.json();
        if (data.ok !== false && data.reply) {
          addMsg(data.reply, 'bot');
          if (voiceMode) speak(data.reply);
          history.push({ role: 'user', content: text });
          history.push({ role: 'assistant', content: plain(data.reply) });
          if (history.length > 20) history = history.slice(-20);
          if (data.closed) voiceMode = false;
        } else {
          addMsg('Photon is offline right now. Email ' + (window.PB_AURORA?.email || 'hello@photon-bounce.com') + ' and we will pick it up.', 'err');
        }
      } catch (err) {
        typing.remove();
        addMsg('Connection hiccup — try again, or email us directly.', 'err');
      } finally {
        _progressDone();
        input.disabled = false;
        if (sendBtn) { sendBtn.disabled = false; sendBtn.textContent = 'Send'; }
        if (!document.hidden) document.title = _savedTitle;
        input.focus();
      }
    });

    // Scroll-to-bottom floating button — appears when log is scrolled up > 60px
    var scrollDownBtn = document.createElement('button');
    scrollDownBtn.type = 'button';
    scrollDownBtn.className = 'pb-brain__scroll-down';
    scrollDownBtn.setAttribute('aria-label', 'Scroll to latest message');
    scrollDownBtn.innerHTML = '&#8595;';
    brain.appendChild(scrollDownBtn);
    log.addEventListener('scroll', function() {
      var gap = log.scrollHeight - log.clientHeight - log.scrollTop;
      scrollDownBtn.classList.toggle('is-visible', gap > 60);
    });
    scrollDownBtn.addEventListener('click', function() {
      log.scrollTop = log.scrollHeight;
    });

    // Stop voice + listening when the chat closes.
    closeBtns.forEach(b => b.addEventListener('click', () => {
      voiceMode = false; listening = false; stopSpeaking();
      if (rec) { try { rec.stop(); } catch (e) {} }
    }));

    // Voice mute toggle injected into chat header
    var muteBtn = document.createElement('button');
    muteBtn.type = 'button';
    muteBtn.className = 'pb-brain__mute';
    var muted = localStorage.getItem('pb_voice_muted') === '1';
    muteBtn.innerHTML = muted ? '&#128263;' : '&#128266;';
    muteBtn.title = 'Toggle voice replies';
    muteBtn.setAttribute('aria-label', muted ? 'Voice muted' : 'Voice on');
    muteBtn.style.cssText = 'background:none;border:1px solid rgba(255,255,255,.2);color:rgba(255,255,255,.6);border-radius:50%;width:28px;height:28px;cursor:pointer;font-size:13px;flex-shrink:0;';
    muteBtn.addEventListener('click', function() {
      muted = !muted;
      localStorage.setItem('pb_voice_muted', muted ? '1' : '0');
      muteBtn.innerHTML = muted ? '&#128263;' : '&#128266;';
      muteBtn.setAttribute('aria-label', muted ? 'Voice muted' : 'Voice on');
      if (muted) stopSpeaking();
    });
    var brainHead = document.querySelector('.pb-brain__head');
    if (brainHead) {
      // Message-count badge — inserted after the h3 title; _mcEl declared at top of IIFE.
      var _h3 = brainHead.querySelector('h3');
      if (_h3) {
        _mcEl = document.createElement('span');
        _mcEl.className = 'pb-brain__msg-count';
        _h3.parentNode.insertBefore(_mcEl, _h3.nextSibling);
        _updateMsgCount();
      }
      // "New conversation" reset button — clears log + sessionStorage
      var newChatBtn = document.createElement('button');
      newChatBtn.type = 'button';
      newChatBtn.className = 'pb-brain__newchat';
      newChatBtn.title = 'Start new conversation';
      newChatBtn.setAttribute('aria-label', 'Start new conversation');
      newChatBtn.innerHTML = '&#x21BA;';
      newChatBtn.addEventListener('click', function() {
        clearChat();
        voiceMode = false; listening = false; stopSpeaking();
        if (rec) { try { rec.stop(); } catch(e) {} }
        hideListeningBanner();
      });
      // Wrap buttons in a flex row so they sit next to the close button
      var btnGroup = brainHead.querySelector('.pb-brain__head-btns');
      if (!btnGroup) {
        btnGroup = document.createElement('div');
        btnGroup.className = 'pb-brain__head-btns';
        btnGroup.style.cssText = 'display:flex;align-items:center;gap:6px;flex-shrink:0;';
        var closeBtn = brainHead.querySelector('[data-pb-brainstorm-close]');
        var exportBtn = document.createElement('button');
        exportBtn.type = 'button';
        exportBtn.className = 'pb-brain__export';
        exportBtn.title = 'Download chat as Markdown';
        exportBtn.setAttribute('aria-label', 'Download chat as Markdown');
        exportBtn.innerHTML = '&#8659;';
        exportBtn.addEventListener('click', exportChat);
        var printBtn = document.createElement('button');
        printBtn.type = 'button';
        printBtn.className = 'pb-brain__print';
        printBtn.title = 'Print or save as PDF';
        printBtn.setAttribute('aria-label', 'Print or save as PDF');
        printBtn.innerHTML = '&#128438;';
        printBtn.addEventListener('click', printChat);
        var collapseBtn = document.createElement('button');
        collapseBtn.type = 'button';
        collapseBtn.className = 'pb-brain__collapse';
        collapseBtn.title = 'Minimize chat';
        collapseBtn.setAttribute('aria-label', 'Minimize chat');
        collapseBtn.innerHTML = '&#8722;';
        var _brainCollapsed = false;
        collapseBtn.addEventListener('click', function() {
          _brainCollapsed = !_brainCollapsed;
          brain.classList.toggle('is-collapsed', _brainCollapsed);
          collapseBtn.innerHTML = _brainCollapsed ? '&#9652;' : '&#8722;';
          collapseBtn.title = _brainCollapsed ? 'Expand chat' : 'Minimize chat';
          collapseBtn.setAttribute('aria-label', _brainCollapsed ? 'Expand chat' : 'Minimize chat');
        });
        var helpBtn = document.createElement('button');
        helpBtn.type = 'button';
        helpBtn.className = 'pb-brain__help';
        helpBtn.title = 'Keyboard shortcuts';
        helpBtn.setAttribute('aria-label', 'Keyboard shortcuts');
        helpBtn.setAttribute('aria-expanded', 'false');
        helpBtn.innerHTML = '&#63;';
        var _helpOpen = false;
        var _helpPanel = document.createElement('div');
        _helpPanel.className = 'pb-brain__shortcuts';
        _helpPanel.setAttribute('role', 'tooltip');
        _helpPanel.innerHTML = [
          '<strong>Shortcuts</strong>',
          '<span><kbd>Ctrl</kbd>+<kbd>/</kbd> Open / close chat</span>',
          '<span><kbd>Ctrl</kbd>+<kbd>F</kbd> Search messages</span>',
          '<span><kbd>Shift</kbd>+<kbd>Enter</kbd> New line</span>',
          '<span><kbd>Enter</kbd> Send message</span>',
          '<span><kbd>Esc</kbd> Close / cancel search</span>',
          '<span><kbd>↑</kbd> / <kbd>↓</kbd> Browse sent messages</span>',
          '<span><kbd>Ctrl</kbd>+<kbd>K</kbd> Clear chat</span>',
          '<span><kbd>Ctrl</kbd>+<kbd>E</kbd> Export chat</span>',
        ].join('');
        brainHead.appendChild(_helpPanel);
        helpBtn.addEventListener('click', function() {
          _helpOpen = !_helpOpen;
          _helpPanel.classList.toggle('pb-brain__shortcuts--open', _helpOpen);
          helpBtn.setAttribute('aria-expanded', String(_helpOpen));
        });
        document.addEventListener('click', function(e) {
          if (_helpOpen && !_helpPanel.contains(e.target) && e.target !== helpBtn) {
            _helpOpen = false; _helpPanel.classList.remove('pb-brain__shortcuts--open');
            helpBtn.setAttribute('aria-expanded', 'false');
          }
        });
        // Starred filter — ★ button hides all non-starred bot replies until toggled off.
        var _sfBtn = document.createElement('button');
        _sfBtn.type = 'button';
        _sfBtn.className = 'pb-brain__starfilter';
        _sfBtn.title = 'Show starred replies only';
        _sfBtn.setAttribute('aria-label', 'Show starred replies only');
        _sfBtn.setAttribute('aria-pressed', 'false');
        _sfBtn.innerHTML = '&#9733;';
        var _sfOn = false;
        _sfBtn.addEventListener('click', function() {
          var _sc = log ? log.querySelectorAll('.pb-brain__msg--starred').length : 0;
          if (!_sfOn && _sc === 0) { _showToast('Star a reply first ★'); return; }
          _sfOn = !_sfOn;
          log.classList.toggle('pb-brain__log--star-only', _sfOn);
          _sfBtn.classList.toggle('pb-brain__starfilter--on', _sfOn);
          _sfBtn.setAttribute('aria-pressed', String(_sfOn));
          _showToast(_sfOn ? _sc + ' starred repl' + (_sc === 1 ? 'y' : 'ies') : 'All messages');
        });
        // R54: Session stats button + panel
        var _statsBtn = document.createElement('button');
        _statsBtn.type = 'button'; _statsBtn.className = 'pb-brain__stats-btn';
        _statsBtn.title = 'Session stats'; _statsBtn.setAttribute('aria-label', 'Session stats');
        _statsBtn.setAttribute('aria-expanded', 'false'); _statsBtn.innerHTML = '&#128202;';
        var _statsPanel = document.createElement('div');
        _statsPanel.className = 'pb-brain__stats-panel';
        _statsPanel.setAttribute('role', 'status');
        var _statsOpen = false;
        var _renderStats = function() {
          var _elapsed = Math.floor((Date.now() - _statsSessionStart) / 1000);
          var _mins = Math.floor(_elapsed / 60), _secs = _elapsed % 60;
          var _sessionTime = _mins > 0 ? _mins + 'm ' + _secs + 's' : _secs + 's';
          var _uCount = chatMsgs.filter(function(m) { return m.cls === 'user'; }).length;
          var _avgResp = _statsRespTimes.length > 0
            ? (_statsRespTimes.reduce(function(a, b) { return a + b; }, 0) / _statsRespTimes.length / 1000).toFixed(1) + 's'
            : '—';
          _statsPanel.innerHTML = [
            '<strong>&#128202; Session stats</strong>',
            '<span>Your messages <b>' + _uCount + '</b></span>',
            '<span>Bot words&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>' + _statsBotWords.toLocaleString() + '</b></span>',
            '<span>Avg response&nbsp;<b>' + _avgResp + '</b></span>',
            '<span>Session time&nbsp;<b>' + _sessionTime + '</b></span>',
          ].join('');
        };
        _statsBtn.addEventListener('click', function() {
          _statsOpen = !_statsOpen;
          if (_statsOpen) _renderStats();
          _statsPanel.classList.toggle('pb-brain__stats-panel--open', _statsOpen);
          _statsBtn.setAttribute('aria-expanded', String(_statsOpen));
        });
        document.addEventListener('click', function(e) {
          if (_statsOpen && !_statsPanel.contains(e.target) && e.target !== _statsBtn) {
            _statsOpen = false;
            _statsPanel.classList.remove('pb-brain__stats-panel--open');
            _statsBtn.setAttribute('aria-expanded', 'false');
          }
        });
        brainHead.appendChild(_statsPanel);
        if (closeBtn) { brainHead.removeChild(closeBtn); btnGroup.appendChild(newChatBtn); btnGroup.appendChild(exportBtn); btnGroup.appendChild(printBtn); btnGroup.appendChild(_sfBtn); btnGroup.appendChild(_statsBtn); btnGroup.appendChild(collapseBtn); btnGroup.appendChild(muteBtn); btnGroup.appendChild(helpBtn); btnGroup.appendChild(closeBtn); }
        else { btnGroup.appendChild(newChatBtn); btnGroup.appendChild(exportBtn); btnGroup.appendChild(printBtn); btnGroup.appendChild(_sfBtn); btnGroup.appendChild(_statsBtn); btnGroup.appendChild(collapseBtn); btnGroup.appendChild(muteBtn); btnGroup.appendChild(helpBtn); }
        brainHead.appendChild(btnGroup);
      } else {
        btnGroup.insertBefore(newChatBtn, btnGroup.firstChild);
        btnGroup.appendChild(muteBtn);
      }
    }
  }
})();

/* ================================================================
   ORB ATTENTION PULSE — extra ring after 20s if chat never opened
   ================================================================ */
(function () {
  'use strict';
  var orbEl = document.querySelector('.pb-orb');
  var brainEl = document.getElementById('pb-brain');
  if (!orbEl || !brainEl) return;
  var cancelled = false;
  document.addEventListener('click', function(e) {
    if (e.target.closest('[data-pb-brainstorm-open],[data-pb-open-orb]')) cancelled = true;
  });
  setTimeout(function() {
    if (cancelled || !brainEl.hidden) return;
    orbEl.classList.add('pb-orb--nudge');
    setTimeout(function() { orbEl.classList.remove('pb-orb--nudge'); }, 2000);
  }, 20000);
})();

/* ================================================================
   MATRIX SEPARATOR — canvas code rain with 3D rotation effect
   ================================================================ */
(function () {
  'use strict';
  const chars = 'アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲン0123456789ABCDEF<>/{}[];=+*$#@!';
  const seps = document.querySelectorAll('.pb-matrix-sep');

  seps.forEach(canvas => {
    const ctx = canvas.getContext('2d');
    let w, h, drops = [];
    const fontSize = 16;
    let columns;

    function resize() {
      const parent = canvas.parentElement;
      if (!parent) return;
      w = canvas.width = parent.offsetWidth;
      h = canvas.height = parent.offsetHeight;
      columns = Math.floor(w / fontSize);
      drops = [];
      for (let i = 0; i < columns; i++) {
        drops[i] = Math.random() * -100;
      }
    }

    let frame = 0;
    function draw() {
      ctx.fillStyle = 'rgba(5, 6, 11, 0.08)';
      ctx.fillRect(0, 0, w, h);
      ctx.font = 'bold ' + fontSize + 'px JetBrains Mono, monospace';

      for (let i = 0; i < drops.length; i++) {
        const char = chars[Math.floor(Math.random() * chars.length)];
        const x = i * fontSize;
        const y = drops[i] * fontSize;

        // 3D rotation effect via skew + scale
        ctx.save();
        ctx.translate(x + fontSize/2, y);
        const rot = Math.sin(frame * 0.02 + i * 0.1) * 0.15;
        const scale = 0.8 + Math.sin(frame * 0.03 + i * 0.2) * 0.2;
        ctx.rotate(rot);
        ctx.scale(scale, scale);

        // Glow head
        ctx.shadowColor = '#00d8ff';
        ctx.shadowBlur = 8;
        ctx.fillStyle = '#00d8ff';
        ctx.fillText(char, -fontSize/2, 0);
        ctx.shadowBlur = 0;
        ctx.restore();

        // Trail
        if (y > fontSize * 2) {
          ctx.fillStyle = 'rgba(0, 216, 255, 0.15)';
          ctx.fillText(char, x, y - fontSize);
        }

        if (y > h && Math.random() > 0.975) {
          drops[i] = 0;
        }
        drops[i]++;
      }
      frame++;
      requestAnimationFrame(draw);
    }

    resize();
    draw();
    window.addEventListener('resize', resize);
  });
})();

/* ================================================================
   PARALLAX + SCROLL REVEAL for desktop and mobile
   ================================================================ */
(function () {
  'use strict';
  const isMobile = window.matchMedia('(pointer: coarse)').matches;

  // Parallax on data-parallax elements (subtle, performant)
  const parallaxEls = document.querySelectorAll('[data-parallax]');
  if (parallaxEls.length && !isMobile) {
    let ticking = false;
    window.addEventListener('scroll', () => {
      if (!ticking) {
        requestAnimationFrame(() => {
          const y = window.scrollY;
          parallaxEls.forEach(el => {
            const speed = parseFloat(el.dataset.parallax) || 0.3;
            const rect = el.getBoundingClientRect();
            const offset = (rect.top + y) * speed;
            el.style.transform = 'translateY(' + (y * speed - offset) + 'px)';
          });
          ticking = false;
        });
        ticking = true;
      }
    }, { passive: true });
  }

  // Scroll reveal with IntersectionObserver
  const revealEls = document.querySelectorAll('[data-pb-reveal]');
  if ('IntersectionObserver' in window) {
    const io = new IntersectionObserver((entries) => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('is-revealed');
          io.unobserve(entry.target);
        }
      });
    }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });
    revealEls.forEach(el => io.observe(el));
  } else {
    revealEls.forEach(el => el.classList.add('is-revealed'));
  }
})();
