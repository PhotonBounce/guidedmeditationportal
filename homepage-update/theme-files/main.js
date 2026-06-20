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
  }

  // (Auto-open removed — the chat opens only when the visitor clicks the orb,
  //  so it never starts talking on its own on page load.)

  function openBrain() {
    if (!brain) return;
    brain.hidden = false;
    document.documentElement.classList.add('pb-brain-open');
    // Restore chips if the log only has the initial bot message (fresh session)
    var chips = brain.querySelector('.pb-brain__chips');
    if (chips && log && log.querySelectorAll('.pb-brain__msg').length <= 1) {
      chips.style.display = '';
    }
    if (input) input.focus();
  }

  function closeBrain() {
    if (!brain) return;
    brain.hidden = true;
    document.documentElement.classList.remove('pb-brain-open');
  }

  openBtns.forEach(btn => btn.addEventListener('click', openBrain));
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

  // Orb z-index fix
  if (orb) {
    orb.style.zIndex = '99999';
    orb.style.position = 'fixed';
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

    // Strip HTML to plain text (for TTS + history).
    function plain(html) {
      const d = document.createElement('div');
      d.innerHTML = html;
      return (d.textContent || d.innerText || '').replace(/\s+/g, ' ').trim();
    }

    // Bot replies may be HTML (local responder) or markdown (LLM). Normalize either
    // into safe HTML so nothing renders as run-on text or literal ** / \n.
    function format(text) {
      if (/<(a|strong|br|ul|ol|li|p|em)\b/i.test(text)) return text;
      return text
        .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
        .replace(/\[([^\]]+)\]\((https?:[^)]+)\)/g, '<a href="$2" target="_blank" rel="noopener">$1</a>')
        .replace(/(^|[\s(])((?:https?:\/\/|www\.)[^\s<)]+)/g, '$1<a href="$2" target="_blank" rel="noopener">$2</a>')
        .replace(/\n/g, '<br>');
    }

    function addMsg(text, cls) {
      const div = document.createElement('div');
      div.className = 'pb-brain__msg pb-brain__msg--' + cls;
      div.innerHTML = cls === 'bot' ? format(text) : text.replace(/</g, '&lt;');
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
            setTimeout(function() { cpBtn.innerHTML = '&#128203;'; }, 1500);
          });
        });
        div.appendChild(cpBtn);
      }
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
          log.appendChild(fu);
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
      if (cls !== 'err') { chatMsgs.push({ text: text, cls: cls }); saveChat(); }
      log.scrollTop = log.scrollHeight;
    }

    function saveChat() {
      try { sessionStorage.setItem('pb_chat_v1', JSON.stringify(chatMsgs)); } catch(e) {}
    }

    function clearChat() {
      chatMsgs = []; history = [];
      try { sessionStorage.removeItem('pb_chat_v1'); } catch(e) {}
      while (log.children.length > 1) log.removeChild(log.lastChild);
      var chips = brain.querySelector('.pb-brain__chips');
      if (chips) chips.style.display = '';
    }

    function addTyping() {
      const div = document.createElement('div');
      div.className = 'pb-brain__msg pb-brain__msg--bot pb-brain__typing';
      div.innerHTML = '<span></span><span></span><span></span>';
      log.appendChild(div);
      log.scrollTop = log.scrollHeight;
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
      log.scrollTop = log.scrollHeight;
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

    form.addEventListener('submit', async e => {
      e.preventDefault();
      const text = input.value.trim();
      if (!text) return;
      addMsg(text, 'user');
      input.value = '';
      input.style.height = 'auto';
      const typing = addTyping();
      try {
        const r = await fetch(endpoint, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ message: text, history: history, path: location.pathname, title: document.title })
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
        if (closeBtn) { brainHead.removeChild(closeBtn); btnGroup.appendChild(newChatBtn); btnGroup.appendChild(muteBtn); btnGroup.appendChild(closeBtn); }
        else { btnGroup.appendChild(newChatBtn); btnGroup.appendChild(muteBtn); }
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
