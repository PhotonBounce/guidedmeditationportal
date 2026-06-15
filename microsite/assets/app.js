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

  const state = { habit: "vaping", habitLabel: "vaping", cost: 7, days: 7, triggers: new Set(), premium: false, freedom: "" };

  const MILESTONES = [1, 3, 7, 14, 30, 60, 90, 180, 365];

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

  // Affirmation library — mirrors AffirmationContent.THEMES in the app
  const THEMES = [["free", "I Am Free", "🕊️", false], ["craving", "Craving Crusher", "🛡️", true], ["morning", "Morning Power", "🌅", true], ["calm", "Calm & Steady", "🌿", true], ["sleep", "Sleep & Release", "🌙", true], ["strength", "Strength & Resolve", "💪", true], ["worth", "Worthy & Whole", "✨", true]];
  const THEME_LINES = {
    free: ["I am free, one breath at a time.", "I am not my habit — I am the one who chose to stop.", "Every clean hour is rebuilding me.", "I choose the person I am becoming.", "Freedom feels better than the habit ever did."],
    craving: ["This craving will pass — whether I feed it or not.", "I can feel the urge and still not move.", "The wave rises, the wave falls. I am the shore.", "Five minutes — I only need to outlast five minutes."],
    morning: ["Today I begin clean and clear.", "I decide who I am today, and I choose free.", "My energy is mine to spend on what matters.", "I rise, and I rise free."],
    calm: ["My calm belongs to me.", "I breathe in steadiness, I breathe out the urge.", "I am grounded, I am safe, I am enough.", "Stillness is my strength."],
    sleep: ["I let go of today and rest in my progress.", "My body heals as I sleep, clean and calm.", "Tomorrow I wake up free.", "I release what I cannot control."],
    strength: ["I am stronger than this craving.", "My resolve is deeper than any urge.", "I keep my promises to myself.", "I am built for this."],
    worth: ["I am worthy of the clean life I'm building.", "I am enough, exactly as I am right now.", "I deserve to feel good without it.", "My value was never in the habit."],
  };
  let activeLines = null;

  // Bottom navigation — shown on the main app screens.
  const NAV = [["dashboard", "⌂", "Home"], ["library", "✦", "Affirm"], ["resources", "✚", "Help"], ["profile", "◉", "You"]];
  const TAB_SCREENS = ["dashboard", "library", "resources", "profile"];
  const navBar = (active) => `<div class="tabbar">${NAV.map((n) =>
    `<div class="tab${n[0] === active ? " on" : ""}" data-action="${n[0]}"><span class="tab-ic">${n[1]}</span><span class="tab-lb">${n[2]}</span></div>`).join("")}</div>`;

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
      <div class="intro">
        <div class="intro-logo"><span class="pulse-ring"></span><span class="pulse-ring r2"></span><img src="assets/logo.png" alt="" /></div>
        <h4 style="text-align:center;margin-top:18px">Power of Mind</h4>
        <p class="eyebrow-mini">BREAK FREE · ONE CLEAN DAY AT A TIME</p>
        <p class="sub" style="text-align:center">Quit the habit holding you back and rewire your mind with daily affirmations.</p>
      </div>
      <button class="pill" data-action="startquiz">Begin</button>`,
    quiz: quizScreen,
    paywall: () => `
      <h4>Your free-from plan is ready</h4>
      <p class="sub">A daily affirmation path built around your triggers to help you stay free from ${habitWord()} — plus a one-tap panic button for the hard moments.</p>
      <div class="benefit">✓ All 7 affirmation themes unlocked</div>
      <div class="benefit">✓ No ads</div>
      <div class="benefit">✓ Track every clean day and the money you save</div>
      <div class="benefit">✓ One-tap panic button + milestone celebrations</div>
      <div class="plan-opt sel" data-plan><b>Annual — unlock everything</b><span>$1.99/yr</span></div>
      <button class="pill" data-action="subscribe">Start my journey</button>
      <div class="link-row" data-action="freetier">Maybe later — continue free</div>`,
    dashboard: () => {
      const next = MILESTONES.find((m) => m > state.days) || 365;
      const frac = Math.min(state.days / next, 1);
      const filled = Math.min(state.days, 7);
      const why = (state.freedom && state.freedom.trim()) || "waking up clear-headed and present";
      const dots = Array.from({ length: 7 }, (_, i) => `<span class="dot${i < filled ? " on" : ""}"></span>`).join("");
      return `
      <div class="dash-head">
        <img class="dash-logo" src="assets/logo.png" alt="" />
        <span class="sub" style="flex:1">Stay free today.</span>
        <span class="ic" data-action="coach" title="Coach">◍</span>
        <span class="ic" data-action="resources" title="Get help">✚</span>
        <span class="ic" data-action="profile" title="You">◉</span>
      </div>
      <div class="card-hero">
        <div class="ring" style="--p:${frac}"><div class="ring-inner"><div class="hero-num" data-days>0</div><div class="hero-cap">DAYS FREE</div></div></div>
        <div class="hero-habit">free from ${habitWord()} · best: ${state.days} days</div>
        <div class="next-ms">✦ ${next - state.days} days to your ${next}-day milestone</div>
      </div>
      <div class="streak">${dots}</div>
      <div class="why-card"><span class="why-label">YOUR WHY</span><p>${why}</p></div>
      <div class="mini-card"><span><i class="chip">💰</i>Money saved</span><b data-money style="color:var(--gold)">$0.00</b></div>
      <div class="mini-card"><span><i class="chip">🛡️</i>Urges beaten</span><b style="color:var(--amber)">4</b></div>
      <button class="pill" data-action="library">▶   Today's affirmation</button>
      <button class="panic" data-action="breathe">🆘   I'm having an urge</button>
      <div class="link-row faint" data-action="milestone">I slipped — reset my counter</div>`;
    },
    library: () => `
      <div class="player-bar"><span style="flex:1;font-family:'Iowan Old Style',Georgia,serif;font-size:20px;color:var(--cream)">Affirmations</span><span class="ic" data-action="dashboard">✕</span></div>
      <p class="sub" style="margin-bottom:10px">Pick a set and let each line land over the soundscape.</p>
      ${THEMES.map((t) => { const locked = t[3] && !state.premium; return `<div class="lib-card" data-theme="${t[0]}"><span class="lib-emoji">${t[2]}</span><span class="lib-title">${t[1]}</span><span style="color:var(--gold)">${locked ? "🔒" : "▶"}</span></div>`; }).join("")}`,
    player: () => `
      <div class="player-bar"><span class="ic" data-mute>🔊</span><span class="ic" data-action="dashboard">✕</span></div>
      <div class="aff-line" data-aff>${(activeLines || affLines())[0]}</div>
      <div class="aff-cap">Breathe slowly · let each line land</div>
      <div class="sound-row">${["Embers", "Rain", "Ocean", "Forest", "Silence"].map((s, i) => `<span class="sound-chip${i === 0 ? " on" : ""}" data-sound="${s}">${s}</span>`).join("")}</div>`,
    milestone: () => `
      <div class="confetti-wrap" aria-hidden="true">${Array.from({ length: 14 }, (_, i) => `<span class="confetti c${i % 5}" style="left:${(i * 7 + 5) % 100}%;animation-delay:${(i % 7) * 0.18}s"></span>`).join("")}</div>
      <div style="margin:auto;text-align:center;position:relative;z-index:1">
        <div class="ms-badge">🏆</div>
        <div class="hero-num">${state.days}</div>
        <div class="hero-cap">DAYS FREE</div>
        <p class="sub" style="text-align:center;margin-top:10px">A week clean — and you can feel it. Momentum is yours.</p>
        <p class="ms-aff">"I am proud of how far I've already come."</p>
      </div>
      <button class="pill" data-action="share">✦  Share my win</button>
      <div class="link-row" data-action="dashboard">Keep going</div>`,
    breathe: () => `
      <div class="player-bar"><span style="flex:1"></span><span class="ic" data-action="dashboard">✕</span></div>
      <div class="breathe-wrap">
        <div class="breathe-orb"><span data-breathe>Breathe in</span></div>
        <p class="breathe-cap">Ride the wave — the urge always passes.</p>
      </div>
      <button class="pill" data-action="player">Play a craving-crusher ▸</button>`,
    share: () => `
      <div class="player-bar"><span style="flex:1;font-family:'Iowan Old Style',Georgia,serif;font-size:18px;color:var(--cream)">Share your progress</span><span class="ic" data-action="dashboard">✕</span></div>
      <div class="share-card">
        <img class="share-logo" src="assets/logo.png" alt="" />
        <div class="share-num">${state.days}</div>
        <div class="share-cap">DAYS FREE</div>
        <div class="share-line">$${(state.days * state.cost).toFixed(0)} saved · free from ${habitWord()}</div>
        <div class="share-brand">◇ Power of Mind</div>
      </div>
      <button class="pill" data-action="dashboard">Share ▸</button>`,
    calendar: () => {
      const cells = 35, filled = Math.min(state.days, cells);
      const grid = Array.from({ length: cells }, (_, i) => {
        const on = i >= cells - filled;
        const today = i === cells - 1;
        return `<span class="cal-cell${on ? " on" : ""}${today ? " today" : ""}"></span>`;
      }).join("");
      return `
      <div class="dash-head"><span style="flex:1;font-family:'Iowan Old Style',Georgia,serif;font-size:20px;color:var(--cream)">Your calendar</span><span class="ic" data-action="profile">✕</span></div>
      <p class="sub" style="margin-bottom:12px">Every gold day is a day you stayed free.</p>
      <div class="cal-head">${["S", "M", "T", "W", "T", "F", "S"].map((w) => `<span>${w}</span>`).join("")}</div>
      <div class="cal-grid">${grid}</div>
      <p class="next-ms" style="margin-top:16px">🔥 ${state.days}-day streak · keep it glowing</p>`;
    },
    insights: () => {
      const saved = Math.round(state.days * state.cost);
      const bars = Array.from({ length: 7 }, (_, i) => `<span class="bar" style="height:${(20 + (i + 1) / 7 * 75).toFixed(0)}%"></span>`).join("");
      return `
      <div class="dash-head"><span style="flex:1;font-family:'Iowan Old Style',Georgia,serif;font-size:20px;color:var(--cream)">Insights</span><span class="ic" data-action="profile">✕</span></div>
      <div class="stat-grid">
        <div class="stat-box"><b>${state.days}</b><span>days free</span></div>
        <div class="stat-box"><b>$${saved}</b><span>saved</span></div>
        <div class="stat-box"><b>4</b><span>urges beaten</span></div>
        <div class="stat-box"><b>${state.days}</b><span>best streak</span></div>
      </div>
      <p class="sub" style="margin:18px 0 8px">Money saved, building daily</p>
      <div class="bar-chart">${bars}</div>`;
    },
    coach: () => `
      <div class="dash-head"><span style="flex:1;font-family:'Iowan Old Style',Georgia,serif;font-size:20px;color:var(--cream)">Your coach</span><span class="ic" data-action="dashboard">✕</span></div>
      <div class="chat">
        <div class="msg coach">Hey — I'm glad you're here. What's coming up for you right now?</div>
        <div class="msg user">I'm getting a craving 😣</div>
        <div class="msg coach">A craving is a wave — it rises, peaks, and passes, usually within a few minutes. You don't have to fight it; you just have to outlast it. Want to ride it out together?</div>
      </div>
      <div class="chat-chips">
        <span class="chip-btn" data-action="breathe">Breathe with me</span>
        <span class="chip-btn" data-action="library">Play an affirmation</span>
      </div>
      <div class="chat-input"><span>Type a message…</span><span class="chat-send">➤</span></div>`,
    reminder: () => `
      <div class="dash-head"><span style="flex:1;font-family:'Iowan Old Style',Georgia,serif;font-size:20px;color:var(--cream)">Daily reminder</span><span class="ic" data-action="profile">✕</span></div>
      <p class="sub" style="margin-bottom:12px">A gentle nudge each day to listen and stay on track.</p>
      <div class="mini-card"><span><i class="chip">🔔</i>Daily reminder</span><span class="toggle on" data-toggle>●</span></div>
      <div class="reminder-time">8:00 <span>AM</span></div>
      <div class="time-chips">${["7:00", "8:00", "9:00", "20:00", "21:00"].map((t, i) => `<span class="time-chip${i === 1 ? " on" : ""}" data-time="${t}">${t}</span>`).join("")}</div>
      <p class="demo-note" style="margin-top:14px">Uses a local notification — no account, nothing leaves your phone.</p>`,
    resources: () => `
      <div class="dash-head"><span style="flex:1;font-family:'Iowan Old Style',Georgia,serif;font-size:20px;color:var(--cream)">Get help now</span></div>
      <p class="sub" style="margin-bottom:12px">A craving is temporary. If it's too much, reach a real person — free, confidential, any time.</p>
      ${[["Suicide & Crisis Lifeline", "988", "Call or text · 24/7"], ["SAMHSA Helpline", "1-800-662-4357", "Substance-use referrals · 24/7"], ["Quit smoking / vaping", "QUIT-NOW", "Free coaching · 1-800-784-8669"], ["Crisis Text Line", "741741", "Text HOME"], ["Emergency", "911", "Immediate danger"]].map((h) =>
        `<div class="help-card"><div class="help-tx"><b>${h[0]}</b><span>${h[2]}</span></div><span class="help-num">${h[1]} ▸</span></div>`).join("")}
      <p class="demo-note" style="margin-top:8px">Not a medical or crisis service.</p>`,
    profile: () => `
      <div class="dash-head"><span style="flex:1;font-family:'Iowan Old Style',Georgia,serif;font-size:20px;color:var(--cream)">Your plan</span></div>
      <div class="card-hero" style="padding:20px 16px">
        <div class="orb-mini" style="width:52px;height:52px;margin-bottom:8px"></div>
        <div style="color:var(--cream);font-weight:700;font-size:16px">Free from ${habitWord()}</div>
        <div class="sub" style="margin:4px 0 0">${state.premium ? "Premium · all themes unlocked" : "Free plan · ad-supported"}</div>
      </div>
      <div class="mini-card" data-action="calendar" style="cursor:pointer"><span><i class="chip">📅</i>Streak calendar</span><span style="color:var(--gold)">›</span></div>
      <div class="mini-card" data-action="insights" style="cursor:pointer"><span><i class="chip">📊</i>Insights</span><span style="color:var(--gold)">›</span></div>
      <div class="mini-card" data-action="reminder" style="cursor:pointer"><span><i class="chip">🔔</i>Daily reminder</span><span style="color:var(--gold)">›</span></div>
      <button class="pill" data-action="share">✦  Share my progress</button>
      ${state.premium ? "" : `<div class="link-row" data-action="paywall">Go Premium · $1.99/yr →</div>`}`,
  };

  let cycleTimer = null;
  function show(id) {
    if (cycleTimer) { clearInterval(cycleTimer); cycleTimer = null; }
    stopAudio();
    const hasTabs = TAB_SCREENS.includes(id);
    let html = typeof screens[id] === "function" ? screens[id]() : screens[id];
    if (hasTabs) html += navBar(id);
    screen.innerHTML = `<div class="scr${hasTabs ? " with-tabs" : ""}">${html}</div>`;
    const el = screen.firstElementChild;
    requestAnimationFrame(() => el.classList.add("active"));
    if (id === "dashboard") animateDash(el);
    if (id === "player") runPlayer(el);
    if (id === "breathe") runBreathe(el);
  }

  function animateDash(el) {
    const dEl = el.querySelector("[data-days]"), mEl = el.querySelector("[data-money]"), ring = el.querySelector(".ring");
    const money = state.days * state.cost;
    const targetP = ring ? parseFloat(ring.style.getPropertyValue("--p")) || 0 : 0;
    if (ring) ring.style.setProperty("--p", "0");
    let d = 0, m = 0, p = 0;
    const t = setInterval(() => {
      d += state.days / 28; m += money / 28; p += targetP / 28;
      if (d >= state.days) { d = state.days; m = money; p = targetP; clearInterval(t); }
      dEl.textContent = Math.floor(d); mEl.textContent = "$" + m.toFixed(2);
      if (ring) ring.style.setProperty("--p", String(Math.min(p, targetP)));
    }, 28);
  }
  // Spoken affirmations (Web Speech) + a soft ambient pad so the player actually
  // "plays" like the app: each line is read aloud as it crossfades in.
  let speakOn = true, audioCtx = null, ambient = null;
  function speak(text) {
    if (!speakOn || !("speechSynthesis" in window)) return;
    try {
      speechSynthesis.cancel();
      const u = new SpeechSynthesisUtterance(text);
      u.rate = 0.84; u.pitch = 1; u.volume = 1;
      speechSynthesis.speak(u);
    } catch (e) {}
  }
  function startAmbient() {
    if (!speakOn || ambient) return;
    try {
      audioCtx = audioCtx || new (window.AudioContext || window.webkitAudioContext)();
      const g = audioCtx.createGain(); g.gain.value = 0.05; g.connect(audioCtx.destination);
      const lp = audioCtx.createBiquadFilter(); lp.type = "lowpass"; lp.frequency.value = 520; lp.connect(g);
      const a = audioCtx.createOscillator(); a.type = "sine"; a.frequency.value = 146.83; a.connect(lp);
      const b = audioCtx.createOscillator(); b.type = "sine"; b.frequency.value = 220; b.detune.value = 3; b.connect(lp);
      a.start(); b.start(); ambient = { a, b };
    } catch (e) {}
  }
  function stopAudio() {
    try { if ("speechSynthesis" in window) speechSynthesis.cancel(); } catch (e) {}
    if (ambient) { try { ambient.a.stop(); ambient.b.stop(); } catch (e) {} ambient = null; }
  }
  function runPlayer(el) {
    const lines = activeLines || affLines(), target = el.querySelector("[data-aff]"); let i = 0;
    startAmbient(); speak(lines[0]);
    cycleTimer = setInterval(() => {
      target.style.opacity = 0;
      setTimeout(() => { i = (i + 1) % lines.length; target.textContent = lines[i]; target.style.opacity = 1; speak(lines[i]); }, 500);
    }, 5500);
  }
  function runBreathe(el) {
    const label = el.querySelector("[data-breathe]"); if (!label) return;
    const phases = ["Breathe in", "Hold", "Breathe out", "Hold"]; let i = 0;
    label.textContent = phases[0];
    cycleTimer = setInterval(() => { i = (i + 1) % phases.length; label.textContent = phases[i]; }, 4000);
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
      if (q.type === "text") {
        const inp = screen.querySelector("[data-text]");
        if (inp && inp.value.trim()) state.freedom = inp.value.trim();
      }
      if (step < QUIZ.length - 1) { step++; show("quiz"); } else { show("paywall"); }
      return;
    }

    const lib = e.target.closest("[data-theme]");
    if (lib) {
      const t = THEMES.find((x) => x[0] === lib.dataset.theme);
      if (t && t[3] && !state.premium) { show("paywall"); return; }
      activeLines = THEME_LINES[lib.dataset.theme] || null; show("player"); return;
    }

    const mute = e.target.closest("[data-mute]");
    if (mute) {
      speakOn = !speakOn; mute.textContent = speakOn ? "🔊" : "🔇";
      if (speakOn) startAmbient(); else stopAudio();
      return;
    }

    const sound = e.target.closest("[data-sound]");
    if (sound) {
      screen.querySelectorAll(".sound-chip").forEach((c) => c.classList.remove("on"));
      sound.classList.add("on");
      return;
    }

    const tchip = e.target.closest("[data-time]");
    if (tchip) {
      screen.querySelectorAll(".time-chip").forEach((c) => c.classList.remove("on"));
      tchip.classList.add("on");
      const [hh, mm] = tchip.dataset.time.split(":"); const h = +hh;
      const disp = screen.querySelector(".reminder-time");
      if (disp) disp.innerHTML = `${((h + 11) % 12) + 1}:${mm} <span>${h < 12 ? "AM" : "PM"}</span>`;
      return;
    }
    const tog = e.target.closest("[data-toggle]");
    if (tog) { tog.classList.toggle("on"); tog.textContent = tog.classList.contains("on") ? "●" : "○"; return; }

    const act = e.target.closest("[data-action]");
    if (!act) return;
    const a = act.dataset.action;
    if (a === "startquiz") { step = 0; show("quiz"); }
    else if (a === "subscribe") { state.premium = true; show("dashboard"); }
    else if (a === "freetier") { state.premium = false; show("dashboard"); }
    else { if (a === "player") activeLines = null; show(a); }
  });

  show("welcome");
})();
