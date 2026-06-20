# DOM QA Report — R17 — 2026-06-20

## main.js — Follow-up chip fade-in animation

When the follow-up chips appear after the 2nd bot message, they now
slide up and fade in via CSS transition rather than appearing instantly.

JS change (before `log.appendChild(fu)`):
```javascript
fu.classList.add('pb-chips-entering');
log.appendChild(fu);
requestAnimationFrame(function() {
  requestAnimationFrame(function() {
    fu.classList.remove('pb-chips-entering');
  });
});
```

The double-rAF pattern is necessary: the first frame catches after DOM insertion,
the second ensures the browser has computed the initial styles before the class
is removed and the transition fires. Without the double-rAF, removing the class
in the same frame as insertion means the browser sees no delta and skips the transition.

## main.js — Orb keyboard shortcut tooltip

The `.pb-orb` chat bubble now has a native browser tooltip:
```javascript
orb.setAttribute('title', 'Chat with Photon · Ctrl+/');
```

Displays on hover, accessible to screen readers via `title`. Helps users
discover the `Ctrl+/` (or `Cmd+/`) toggle without reading docs.

## main.css — Chip slide-in transition + entering state

```css
.pb-brain__chips-followup {
  /* added: */ transition: opacity .35s ease, transform .35s ease;
}
.pb-chips-entering { opacity: 0; transform: translateY(10px); }
```

After the double-rAF removes `.pb-chips-entering`, the browser transitions
from `opacity:0 / translateY(10px)` to the default `opacity:1 / translateY(0)`.
Duration 350ms — slightly longer than the typing indicator pulse (300ms)
so the chips feel deliberate rather than instant.

## brainstorm.php — 3 new intent handlers

### 0d-pre13-a) White-label / reseller / agency partnership
Keywords: white label, white-label, resell, reseller, agency reselling,
rebrand for clients, white label solution, agency white label, client reselling,
under my brand, under our brand, agency partnership, dev partnership, whitelabel

Response:
- Deliverables — no Photon Bounce branding; code/repos/assets transfer cleanly
- Agency rate — volume discount for 3+ projects/year; email for rate card
- Branding — WP admin footer + emails match your agency domain
- Communication — I can face the client or stay behind the scenes — your choice
- NDA — available on request; mutual or one-way
- No licensing fees — no per-project/per-seat charge
- Closes: "How many projects/year and what stack?"

### 0d-pre13-b) CRO / conversion rate optimization
Keywords: conversion rate, cro, conversion optimization, a/b testing, ab testing,
split testing, landing page optimization, heatmap, click tracking, funnel analysis,
bounce rate, optimize conversions, improve conversions, conversion funnel, low conversion

Response:
- A/B testing — VWO/Optimizely integration OR vanilla JS split test + GA4 event tracking
- Heatmap & session recording — Microsoft Clarity (free) or Hotjar; configured + dashboard
- Funnel analysis — GA4 conversion events + funnels; Looker Studio dashboard
- Landing page variant — full new LP from $150; tested against existing
- Speed → conversions: "each 1s improvement = up to 7% conversion lift"
- Closes: "What's the current conversion rate and what page/funnel?"

### 0d-pre13-c) AR / VR / spatial computing / immersive web
Keywords: augmented reality, ar filter, virtual reality, vr experience, spatial computing,
apple vision pro, webxr, ar/vr, mixed reality, xr experience, metaverse, immersive experience,
360 video, three.js vr, aframe, model viewer, product try-on, ar try on

Response:
- Web AR (product try-on) — <model-viewer> with USDZ+GLB; no app download required;
  bundled into 3D/WebGL service
- WebXR in-browser VR — A-Frame or Three.js XR; runs in Meta Quest browser; from $750
- AR filters — Spark AR (Meta) or Effect House (TikTok); from $200/filter
- Apple Vision Pro — visionOS SwiftUI spatial apps; scoping available in 2026
- 360° video — pannellum.js or Three.js equirectangular; desktop + mobile gyro
- Closes: "Product visualization, AR filter, or full immersive experience?"

## QA results (20/20 all pass)
| Check | Result |
|-------|--------|
| pb-chips-entering class added | OK |
| rAF double-frame remove | OK |
| .pb-chips-entering opacity:0 | OK |
| .pb-chips-entering translateY | OK |
| chips-followup CSS transition | OK |
| orb title Ctrl+/ | OK |
| White-label intent keywords | OK |
| Agency rate in WL response | OK |
| NDA in WL response | OK |
| No licensing fees in WL | OK |
| CRO intent keywords | OK |
| A/B testing in CRO | OK |
| Microsoft Clarity in CRO | OK |
| GA4 funnel in CRO | OK |
| 1s improvement stat | OK |
| AR/VR intent keywords | OK |
| model-viewer in AR | OK |
| WebXR/A-Frame in AR | OK |
| Spark AR filter | OK |
| 360° video | OK |
