/* Power of Mind — microsite interactions + faithful in-browser app emulator.
   The phone demo mirrors the real Android screens 1:1 (QuizActivity →
   paywall → DashboardActivity → AffirmationPlayerActivity), same copy,
   same gold theme, with sample data. */
(function () {
  "use strict";
  const reduce = matchMedia("(prefers-reduced-motion:reduce)").matches;

  /* ---------------- Embers canvas ---------------- */
  const canvas = document.getElementById("embers");
  if (canvas && !reduce) {
    const ctx = canvas.getContext("2d");
    let W, H, embers = [];
    const spawn = (rand) => ({
      x: Math.random() * W, y: rand ? Math.random() * H : H + 10,
      r: Math.random() * 2.2 + 0.6, s: Math.random() * 0.6 + 0.2,
      drift: (Math.random() - 0.5) * 0.4, a: Math.random() * 0.5 + 0.2,
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
        ctx.fillStyle = g; ctx.beginPath(); ctx.arc(e.x, e.y, e.r * 4, 0, 7); ctx.fill();
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
  addEventListener("mousemove", (e) => { mx = e.clientX / innerWidth - 0.5; my = e.clientY / innerHeight - 0.5; if (!reduce) applyParallax(); });

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
    const el = stat.querySelector(".stat-num"); if (!el) return;
    const target = +el.dataset.count || 0, prefix = el.dataset.prefix || "";
    let cur = 0; const step = target / 60;
    const t = setInterval(() => { cur += step; if (cur >= target) { cur = target; clearInterval(t); } el.textContent = prefix + Math.floor(cur).toLocaleString(); }, 16);
  }

  /* ===================== FAITHFUL APP EMULATOR ===================== */
  const screen = document.getElementById("appScreen");
  if (!screen) return;

  const state = { habit: "vaping", habitLabel: "vaping", cost: 7, days: 7, triggers: new Set() };

  // Mirrors AffirmationContent.kt
  const AFF = {
    vaping: ["My lungs are healing with every clean breath.", "I breathe deeply, because I can.", "I am not a smoker. I am free."],
    smoking: ["Each smoke-free hour is a gift to my future self.", "I am clean, clear, and in control.", "My body thanks me for every craving I ride out."],
    alcohol: ["I wake up clear, and I am proud of that.", "I don't need a drink to feel at ease.", "Clear-headed is my natural state."],
    social_media: ["My attention belongs to me.", "I am fully present in my real life.", "The feed can wait. My life cannot."],
    doomscrolling: ["I put the phone down and pick my life up.", "My mind is calm when I stop feeding it noise.", "I decide what deserves my attention."],
    _general: ["I am stronger than this craving.", "This urge will pass — whether I feed it or not.", "Every clean hour is rebuilding me.", "I choose the person I am becoming.", "I am free, one breath at a time."],
  };
  const affLines = () => (AFF[state.habit] || []).concat(AFF._general);
  const habitWord = () => (state.habit === "other" ? "the habit" : state.habitLabel);

  // The real 5-step QuizActivity
  const QUIZ = [
    { title: "What are you breaking free from?", sub: "Pick the one that fits best — your plan is built around it.", type: "single",
      opts: [["Vaping", "vaping", 7], ["Smoking", "smoking", 9], ["Social media", "social_media", 0], ["Doomscrolling", "doomscrolling", 0], ["Alcohol", "alcohol", 12], ["Something else", "other", 0]] },
    { title: "How long have you been trying to quit?", sub: "There's no wrong answer. This just sets the right pace.", type: "single",
      opts: [["I'm just starting"], ["Less than a month"], ["1–6 months"], ["Over 6 months"]] },
    { title: "When do the urges hit hardest?", sub: "Choose all that apply — affirmations will target these moments.", type: "multi",
      opts: [["Stress"], ["Boredom"], ["Loneliness"], ["After meals"], ["Social situations"], ["Mornings"], ["Late nights"]] },
    { title: "Roughly what does the habit cost you a day?", sub: "We'll turn every clean day into money saved.", type: "cost",
      opts: [["Under $5", 3], ["$5–$10", 7], ["$10–$20", 15], ["Over $20", 25]] },
    { title: "What will freedom feel like?", sub: "One line, in your own words. We'll weave it into your affirmations.", type: "text" },
  ];
  let step = 0;

  const optRow = (label) => `<div class="opt" data-pick>${label}</div>`;

  function quizScreen() {
    const q = QUIZ[step];
    let body;
    if (q.type === "text") {
      body = `<input class="aff-input" data-text placeholder="e.g. waking up clear-headed, present with my kids…" />`;
    } else {
      body = q.opts.map((o) => optRow(o[0])).join("");
    }
    const cta = step === QUIZ.length - 1 ? "See my plan" : "Continue";
    return `
      <p class="sub" style="letter-spacing:.14em;margin-bottom:6px">STEP ${step + 1} OF ${QUIZ.length}</p>
      <h4>${q.title}</h4>
      <p class="sub">${q.sub}</p>
      <div class="opts-wrap">${body}</div>
      <button class="pill" data-cont ${q.type === "text" ? "" : "disabled style=\"opacity:.45\""}>${cta}</button>`;
  }

  const screens = {
    welcome: () => `
      <div style="margin:auto;text-align:center">
        <div class="orb-mini" aria-hidden="true"></div>
        <h4 style="text-align:center">Power of Mind</h4>
        <p class="sub" style="text-align:center">Break free from the habit holding you back. Build your plan in a minute.</p>
      </div>
      <button class="pill" data-action="startquiz">Begin</button>`,
    quiz: quizScreen,
    paywall: () => `
      <h4>Your free-from plan is ready</h4>
      <p class="sub">A daily affirmation path built around your triggers to help you stay free from ${habitWord()} — plus a one-tap panic button for the hard moments.</p>
      <div class="benefit">✓ Daily audio affirmations over calming soundscapes</div>
      <div class="benefit">✓ Track every clean day and the money you save</div>
      <div class="benefit">✓ One-tap panic button to ride out cravings</div>
      <div class="benefit">✓ Milestone celebrations that keep you going</div>
      <div class="plan-opt sel" data-plan><b>Annual — best value</b><span>$39.99/yr</span></div>
      <div class="plan-opt" data-plan>Monthly<span>$9.99/mo</span></div>
      <button class="pill" data-action="dashboard">Start my journey</button>
      <div class="link-row" data-action="dashboard">Preview the app (demo)</div>`,
    dashboard: () => `
      <div class="dash-head">
        <span class="sub" style="flex:1">Stay free today.</span>
        <span class="ic" data-action="player">↗</span>
        <span class="ic">⚙</span>
      </div>
      <div class="card-hero">
        <div class="hero-num" data-days>0</div>
        <div class="hero-cap">DAYS FREE</div>
        <div class="hero-habit">free from ${habitWord()} · best: ${state.days} days</div>
      </div>
      <div class="mini-card"><span>💰  Money saved</span><b data-money style="color:var(--gold)">$0.00</b></div>
      <div class="mini-card"><span>🛡️  Urges beaten</span><b style="color:var(--amber)">4</b></div>
      <button class="pill" data-action="player">▶   Today's affirmation</button>
      <button class="panic" data-action="player">🆘   I'm having an urge</button>
      <div class="link-row faint" data-action="milestone">I slipped — reset my counter</div>`,
    player: () => `
      <div class="player-bar"><span class="ic">🔈</span><span class="ic" data-action="dashboard">✕</span></div>
      <div class="aff-line" data-aff>${affLines()[0]}</div>
      <div class="aff-cap">♪  Breathe slowly · let each line land</div>
      <button class="pill ghost" data-action="dashboard" style="margin-top:14px">Done</button>`,
    milestone: () => `
      <div style="margin:auto;text-align:center">
        <div style="font-size:48px">🏆</div>
        <div class="hero-num">${state.days}</div>
        <div class="hero-cap">DAYS FREE</div>
        <p class="sub" style="text-align:center;margin-top:10px">One week free — momentum is yours.</p>
      </div>
      <button class="pill" data-action="dashboard">Keep going</button>`,
  };

  let cycleTimer = null;
  function show(id) {
    if (cycleTimer) { clearInterval(cycleTimer); cycleTimer = null; }
    const html = typeof screens[id] === "function" ? screens[id]() : screens[id];
    screen.innerHTML = `<div class="scr">${html}</div>`;
    const el = screen.firstElementChild;
    requestAnimationFrame(() => el.classList.add("active"));
    if (id === "dashboard") animateDash(el);
    if (id === "player") runPlayer(el);
  }

  function animateDash(el) {
    const dEl = el.querySelector("[data-days]"), mEl = el.querySelector("[data-money]");
    const money = state.days * state.cost; let d = 0, m = 0;
    const t = setInterval(() => {
      d += state.days / 28; m += money / 28;
      if (d >= state.days) { d = state.days; m = money; clearInterval(t); }
      dEl.textContent = Math.floor(d); mEl.textContent = "$" + m.toFixed(2);
    }, 28);
  }
  function runPlayer(el) {
    const lines = affLines(), target = el.querySelector("[data-aff]"); let i = 0;
    cycleTimer = setInterval(() => {
      target.style.opacity = 0;
      setTimeout(() => { i = (i + 1) % lines.length; target.textContent = lines[i]; target.style.opacity = 1; }, 500);
    }, 4000);
  }

  // advance enable/disable for the Continue button
  function refreshCont() {
    const q = QUIZ[step]; const cont = screen.querySelector("[data-cont]"); if (!cont) return;
    let ok = q.type === "text";
    if (q.type === "multi") ok = !!screen.querySelector(".opt.sel");
    else if (q.type !== "text") ok = !!screen.querySelector(".opt.sel");
    cont.disabled = !ok; cont.style.opacity = ok ? 1 : 0.45;
  }

  screen.addEventListener("click", (e) => {
    const opt = e.target.closest(".opt[data-pick]");
    if (opt) {
      const q = QUIZ[step];
      if (q.type === "multi") opt.classList.toggle("sel");
      else { screen.querySelectorAll(".opt").forEach((o) => o.classList.remove("sel")); opt.classList.add("sel"); }
      refreshCont();
      return;
    }
    const plan = e.target.closest("[data-plan]");
    if (plan) { screen.querySelectorAll(".plan-opt").forEach((p) => p.classList.remove("sel")); plan.classList.add("sel"); return; }

    const cont = e.target.closest("[data-cont]");
    if (cont) {
      const q = QUIZ[step];
      const sel = screen.querySelector(".opt.sel");
      if (step === 0 && sel) { // habit
        const label = sel.textContent.trim();
        const found = q.opts.find((o) => o[0] === label);
        if (found) { state.habit = found[1] || "other"; state.habitLabel = label.toLowerCase(); state.cost = found[2] || 7; }
      }
      if (q.type === "cost" && sel) {
        const found = q.opts.find((o) => o[0] === sel.textContent.trim());
        if (found) state.cost = found[1];
      }
      if (step < QUIZ.length - 1) { step++; show("quiz"); } else { show("paywall"); }
      return;
    }

    const act = e.target.closest("[data-action]");
    if (!act) return;
    const a = act.dataset.action;
    if (a === "startquiz") { step = 0; show("quiz"); }
    else show(a);
  });

  show("welcome");
})();
