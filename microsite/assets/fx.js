// Power of Mind — eye-candy layer: energy field, UI sound, ripples, tilt, parallax.
(function () {
  "use strict";
  const reduce = matchMedia("(prefers-reduced-motion:reduce)").matches;

  /* ---------------- Web Audio: synthesized UI sounds ---------------- */
  let actx = null;
  let muted = localStorage.getItem("pom_muted") === "1";
  const ac = () => {
    if (!actx) { try { actx = new (window.AudioContext || window.webkitAudioContext)(); } catch (e) {} }
    if (actx && actx.state === "suspended") actx.resume();
    return actx;
  };
  function tone(freq, dur, type, gain, slideTo) {
    if (muted) return;
    const c = ac(); if (!c) return;
    const o = c.createOscillator(), g = c.createGain();
    o.type = type || "sine"; o.frequency.value = freq;
    if (slideTo) o.frequency.exponentialRampToValueAtTime(slideTo, c.currentTime + dur);
    g.gain.value = 0;
    g.gain.linearRampToValueAtTime(gain || 0.04, c.currentTime + 0.012);
    g.gain.exponentialRampToValueAtTime(0.0001, c.currentTime + dur);
    o.connect(g).connect(c.destination);
    o.start(); o.stop(c.currentTime + dur + 0.02);
  }
  const sfx = {
    hover: () => tone(900, 0.05, "sine", 0.012),
    click: () => { tone(523.25, 0.13, "triangle", 0.05, 784); tone(1046.5, 0.10, "sine", 0.02); },
  };

  /* sound toggle */
  const toggle = document.getElementById("soundToggle");
  const syncToggle = () => { if (toggle) toggle.textContent = muted ? "🔇" : "🔈"; };
  if (toggle) {
    syncToggle();
    toggle.addEventListener("click", () => {
      muted = !muted; localStorage.setItem("pom_muted", muted ? "1" : "0"); syncToggle();
      if (!muted) { ac(); sfx.click(); }
    });
  }

  /* ---------------- Button/option sound + ripple (delegated, covers demo) ---------------- */
  let lastHover = 0;
  document.addEventListener("mouseover", (e) => {
    const t = e.target.closest(".btn,.pill,.opt,.card");
    if (t && Date.now() - lastHover > 70) { lastHover = Date.now(); sfx.hover(); }
  });
  document.addEventListener("click", (e) => {
    const t = e.target.closest(".btn,.pill,.opt");
    if (!t) return;
    sfx.click();
    if (t.matches(".btn,.pill")) ripple(t, e);
  });
  function ripple(el, e) {
    const r = document.createElement("span");
    r.className = "ripple";
    const rect = el.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height);
    r.style.width = r.style.height = size + "px";
    r.style.left = (e.clientX - rect.left - size / 2) + "px";
    r.style.top = (e.clientY - rect.top - size / 2) + "px";
    el.appendChild(r);
    setTimeout(() => r.remove(), 650);
  }

  if (!reduce) {
    /* ---------------- 3D card tilt ---------------- */
    document.querySelectorAll(".card").forEach((card) => {
      card.addEventListener("mousemove", (e) => {
        const r = card.getBoundingClientRect();
        const px = (e.clientX - r.left) / r.width - 0.5;
        const py = (e.clientY - r.top) / r.height - 0.5;
        card.style.transform = `perspective(720px) rotateX(${-py * 7}deg) rotateY(${px * 9}deg) translateY(-6px)`;
      });
      card.addEventListener("mouseleave", () => { card.style.transform = ""; });
    });

    /* ---------------- Hero mouse parallax ---------------- */
    const orb = document.querySelector(".logo-orb");
    const htitle = document.querySelector(".hero-title");
    addEventListener("mousemove", (e) => {
      const x = e.clientX / innerWidth - 0.5, y = e.clientY / innerHeight - 0.5;
      if (orb) orb.style.transform = `translate(${x * 20}px, ${y * 16}px)`;
      if (htitle) htitle.style.transform = `translate(${x * 9}px, ${y * 6}px)`;
    });

    /* ---------------- Energy field: a living "mind" network ---------------- */
    const cv = document.getElementById("energy");
    if (cv) {
      const ctx = cv.getContext("2d");
      let W, H, nodes = [];
      const N = Math.min(64, Math.floor(innerWidth / 26));
      const resize = () => { W = cv.width = innerWidth; H = cv.height = innerHeight; };
      const seed = () => {
        nodes = [];
        for (let i = 0; i < N; i++) nodes.push({
          x: Math.random() * W, y: Math.random() * H,
          vx: (Math.random() - 0.5) * 0.25, vy: (Math.random() - 0.5) * 0.25,
          p: Math.random() * 6.28,
        });
      };
      const tick = (t) => {
        ctx.clearRect(0, 0, W, H);
        for (const n of nodes) {
          n.x += n.vx; n.y += n.vy;
          if (n.x < 0 || n.x > W) n.vx *= -1;
          if (n.y < 0 || n.y > H) n.vy *= -1;
        }
        for (let i = 0; i < nodes.length; i++) {
          for (let j = i + 1; j < nodes.length; j++) {
            const a = nodes[i], b = nodes[j];
            const dx = a.x - b.x, dy = a.y - b.y;
            const d = Math.hypot(dx, dy);
            if (d < 132) {
              ctx.strokeStyle = `rgba(255,178,86,${(1 - d / 132) * 0.13})`;
              ctx.lineWidth = 1;
              ctx.beginPath(); ctx.moveTo(a.x, a.y); ctx.lineTo(b.x, b.y); ctx.stroke();
            }
          }
        }
        const now = (t || 0) / 1000;
        for (const n of nodes) {
          const pulse = 1.1 + Math.sin(now * 1.4 + n.p) * 0.5;
          ctx.fillStyle = "rgba(255,201,120,0.4)";
          ctx.beginPath(); ctx.arc(n.x, n.y, pulse, 0, 7); ctx.fill();
        }
        requestAnimationFrame(tick);
      };
      addEventListener("resize", () => { resize(); seed(); }, { passive: true });
      resize(); seed(); requestAnimationFrame(tick);
    }
  }
})();
