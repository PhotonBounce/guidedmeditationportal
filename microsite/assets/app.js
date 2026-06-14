/* Power of Mind — microsite interactions + in-browser app demo */
(function () {
  "use strict";
  const reduce = matchMedia("(prefers-reduced-motion:reduce)").matches;

  /* ---------------- Embers canvas ---------------- */
  const canvas = document.getElementById("embers");
  if (canvas && !reduce) {
    const ctx = canvas.getContext("2d");
    let W, H, embers = [];
    const spawn = (rand) => ({
      x: Math.random() * W,
      y: rand ? Math.random() * H : H + 10,
      r: Math.random() * 2.2 + 0.6,
      s: Math.random() * 0.6 + 0.2,
      drift: (Math.random() - 0.5) * 0.4,
      a: Math.random() * 0.5 + 0.2,
      hue: Math.random() * 30 + 25,
    });
    const resize = () => { W = canvas.width = innerWidth; H = canvas.height = innerHeight; };
    const seed = () => { embers = []; const n = Math.min(70, Math.floor(innerWidth / 22)); for (let i = 0; i < n; i++) embers.push(spawn(true)); };
    const tick = () => {
      ctx.clearRect(0, 0, W, H);
      for (const e of embers) {
        e.y -= e.s; e.x += e.drift; e.a -= 0.0009;
        if (e.y < -10 || e.a <= 0) Object.assign(e, spawn(false));
        const g = ctx.createRadialGradient(e.x, e.y, 0, e.x, e.y, e.r * 4);
        g.addColorStop(0, `hsla(${e.hue},100%,65%,${e.a})`);
        g.addColorStop(1, `hsla(${e.hue},100%,55%,0)`);
        ctx.fillStyle = g;
        ctx.beginPath(); ctx.arc(e.x, e.y, e.r * 4, 0, 7); ctx.fill();
      }
      requestAnimationFrame(tick);
    };
    addEventListener("resize", () => { resize(); seed(); }, { passive: true });
    resize(); seed(); tick();
  }

  /* ---------------- Parallax glows ---------------- */
  const glows = [...document.querySelectorAll(".glow")];
  let mx = 0, my = 0;
  const applyParallax = () => {
    const y = scrollY;
    for (const g of glows) {
      const d = parseFloat(g.dataset.depth) || 0.1;
      g.style.transform = `translate(${mx * d * 40}px, ${y * d + my * d * 40}px)`;
    }
  };
  addEventListener("scroll", applyParallax, { passive: true });
  addEventListener("mousemove", (e) => {
    mx = e.clientX / innerWidth - 0.5;
    my = e.clientY / innerHeight - 0.5;
    if (!reduce) applyParallax();
  });

  /* ---------------- Reveal + stat counters ---------------- */
  const io = new IntersectionObserver((entries) => {
    for (const en of entries) {
      if (!en.isIntersecting) continue;
      en.target.classList.add("in");
      if (en.target.classList.contains("stat")) countUp(en.target);
      io.unobserve(en.target);
    }
  }, { threshold: 0.18 });
  document.querySelectorAll(".reveal, .stat").forEach((el) => io.observe(el));

  function countUp(stat) {
    const el = stat.querySelector(".stat-num");
    if (!el) return;
    const target = +el.dataset.count || 0;
    const prefix = el.dataset.prefix || "";
    let cur = 0;
    const step = target / 60;
    const t = setInterval(() => {
      cur += step;
      if (cur >= target) { cur = target; clearInterval(t); }
      el.textContent = prefix + Math.floor(cur).toLocaleString();
    }, 16);
  }

  /* ===================== APP DEMO ===================== */
  const screen = document.getElementById("appScreen");
  if (!screen) return;

  const state = { habit: "vaping", habitLabel: "vaping", cost: 7, days: 7 };

  const AFF = {
    vaping: ["My lungs are healing with every clean breath.", "I am not a smoker. I am free.", "This craving will pass — whether I feed it or not."],
    alcohol: ["I wake up clear, and I am proud of that.", "I don't need a drink to feel at ease.", "Clear-headed is my natural state."],
    doomscrolling: ["I put the phone down and pick my life up.", "My attention belongs to me.", "I decide what deserves my attention."],
    social_media: ["My attention belongs to me.", "I am fully present in my real life.", "The feed can wait. My life cannot."],
    _general: ["I am stronger than this craving.", "Every clean hour is rebuilding me.", "I choose the person I am becoming."],
  };
  const affLines = () => (AFF[state.habit] || []).concat(AFF._general);

  const opt = (label, key, cost) =>
    `<div class="opt" data-action="pick" data-key="${key}" data-label="${label}" data-cost="${cost}">${label}</div>`;

  const screens = {
    welcome: () => `
      <div style="margin:auto;text-align:center">
        <div style="font-size:46px">🛡️</div>
        <h4 style="text-align:center">Power of Mind</h4>
        <p class="sub" style="text-align:center">Break free from the habit holding you back. Build your plan in four taps.</p>
      </div>
      <button class="pill" data-action="go" data-to="q_habit">Begin</button>`,
    q_habit: () => `
      <p class="sub" style="letter-spacing:.14em;margin-bottom:6px">STEP 1 OF 2</p>
      <h4>What are you breaking free from?</h4>
      ${opt("Vaping", "vaping", 7)}${opt("Doomscrolling", "doomscrolling", 0)}${opt("Drinking", "alcohol", 12)}${opt("Social media", "social_media", 0)}
      <button class="pill" data-cont data-action="cont-habit" disabled style="opacity:.45">Continue</button>`,
    q_trigger: () => `
      <p class="sub" style="letter-spacing:.14em;margin-bottom:6px">STEP 2 OF 2</p>
      <h4>When do urges hit hardest?</h4>
      ${opt("Stress", "stress", 0)}${opt("Boredom", "boredom", 0)}${opt("Late nights", "nights", 0)}${opt("After meals", "meals", 0)}
      <button class="pill" data-cont data-action="cont-trigger" disabled style="opacity:.45">See my plan</button>`,
    paywall: () => `
      <h4>Your plan is ready</h4>
      <p class="sub">A daily affirmation path built around your triggers, plus a panic button for the hard moments.</p>
      <div class="mini-card"><span>✓ Daily audio affirmations</span></div>
      <div class="mini-card"><span>✓ Clean-day &amp; money tracker</span></div>
      <div class="mini-card"><span>✓ One-tap panic button</span></div>
      <div class="mini-card" style="border-color:var(--gold)"><span><b>Annual — best value</b></span><b>$39.99/yr</b></div>
      <button class="pill" data-action="start">Start my journey</button>
      <button class="pill ghost" data-action="start">Preview the app (demo)</button>`,
    dashboard: () => `
      <p class="sub" style="margin-bottom:4px">Stay free today.</p>
      <div class="hero-num" data-days>0</div>
      <div class="hero-cap">DAYS FREE</div>
      <div class="hero-habit">free from ${state.habitLabel} · best: ${state.days} days</div>
      <div class="mini-card"><span>💰 Money saved</span><b data-money>$0.00</b></div>
      <div class="mini-card"><span>🛡️ Urges beaten</span><b>4</b></div>
      <button class="pill" data-action="player">▶ Today's affirmation</button>
      <button class="panic" data-action="player">🆘 I'm having an urge</button>`,
    player: () => `
      <div style="display:flex;justify-content:flex-end"><span data-close style="cursor:pointer;color:var(--muted);font-size:20px" data-action="back-dash">✕</span></div>
      <div class="aff-line" data-aff>${affLines()[0]}</div>
      <div class="aff-cap">Breathe slowly · let each line land</div>
      <button class="pill ghost" data-action="back-dash" style="margin-top:14px">Done</button>`,
  };

  let cycleTimer = null;
  function show(id) {
    if (cycleTimer) { clearInterval(cycleTimer); cycleTimer = null; }
    screen.innerHTML = `<div class="scr">${screens[id]()}</div>`;
    const el = screen.firstElementChild;
    requestAnimationFrame(() => el.classList.add("active"));
    if (id === "dashboard") animateDash(el);
    if (id === "player") runPlayer(el);
  }

  function animateDash(el) {
    const dEl = el.querySelector("[data-days]");
    const mEl = el.querySelector("[data-money]");
    const money = state.days * state.cost;
    let d = 0, m = 0;
    const t = setInterval(() => {
      d += state.days / 28; m += money / 28;
      if (d >= state.days) { d = state.days; m = money; clearInterval(t); }
      dEl.textContent = Math.floor(d);
      mEl.textContent = "$" + m.toFixed(2);
    }, 28);
  }

  function runPlayer(el) {
    const lines = affLines();
    const target = el.querySelector("[data-aff]");
    let i = 0;
    cycleTimer = setInterval(() => {
      target.style.opacity = 0;
      setTimeout(() => { i = (i + 1) % lines.length; target.textContent = lines[i]; target.style.opacity = 1; }, 500);
    }, 3500);
  }

  screen.addEventListener("click", (e) => {
    const t = e.target.closest("[data-action]");
    if (!t) return;
    const a = t.dataset.action;
    if (a === "go") return show(t.dataset.to);
    if (a === "pick") {
      screen.querySelectorAll(".opt").forEach((o) => o.classList.remove("sel"));
      t.classList.add("sel");
      screen._pick = { ...t.dataset };
      const c = screen.querySelector("[data-cont]");
      if (c) { c.disabled = false; c.style.opacity = 1; }
      return;
    }
    if (a === "cont-habit") {
      const p = screen._pick; if (!p) return;
      state.habit = p.key; state.habitLabel = p.label.toLowerCase(); state.cost = +p.cost || 0;
      return show("q_trigger");
    }
    if (a === "cont-trigger") return show("paywall");
    if (a === "start") return show("dashboard");
    if (a === "player") return show("player");
    if (a === "back-dash") return show("dashboard");
  });

  show("welcome");
})();
