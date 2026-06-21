# DOM QA Report — R25 — 2026-06-20

## main.js — Time-of-day greeting on fresh session

New `_greetUser()` helper checks the browser's local hour and injects a
contextual welcome message as the first bot message on a fresh session:

```javascript
function _greetUser() {
  if (!log) return;
  var _hr = new Date().getHours();
  var _tod = _hr < 12 ? 'Good morning' : _hr < 17 ? 'Good afternoon' : 'Good evening';
  addMsg(_tod + '! I'm Photon — what are you building today?', 'bot');
}
```

Time ranges:
- 00:00–11:59 → "Good morning!"
- 12:00–16:59 → "Good afternoon!"
- 17:00–23:59 → "Good evening!"

Called from:
1. `openBrain()` — only when `chatMsgs.length === 0 && log.querySelectorAll('.pb-brain__msg').length === 0`
   (fresh session; not shown if a session is being restored from sessionStorage)
2. `clearChat()` — called after the chat is cleared by the ↺ new-chat button,
   so the empty chat doesn't feel abandoned

The greeting is stored in `chatMsgs` and sessionStorage (like any bot message),
so if the user refreshes, the session is restored with the greeting visible.

## main.js — Orb message-count badge

`_updateOrbBadge()` maintains a red count badge on the `.pb-orb` button
showing how many bot replies are in the current session:

```javascript
function _updateOrbBadge() {
  if (!orb) return;
  var _badge = orb.querySelector('.pb-orb__badge');
  if (!_badge) {
    _badge = document.createElement('span');
    _badge.className = 'pb-orb__badge';
    orb.appendChild(_badge);
  }
  var _cnt = chatMsgs.filter(function(m) { return m.cls === 'bot'; }).length;
  _badge.textContent = _cnt > 9 ? '9+' : (_cnt > 0 ? String(_cnt) : '');
  _badge.classList.toggle('pb-orb__badge--vis',
    _cnt > 0 && (brain ? brain.hidden !== false : true));
}
```

Badge behavior:
- Shows count of bot replies; capped at "9+" for large sessions
- Only visible when the drawer is CLOSED (`brain.hidden !== false`)
- Disappears when the user opens the chat (brain.hidden = false)
- Called after each bot message (`addMsg` gated on `cls === 'bot'`)
- Called in `closeBrain()` — immediately shows the badge as drawer closes
- Called in `clearChat()` — hides the badge when chat is reset
- `openBtns` click handler also calls `_updateOrbBadge()` to hide badge on open

Badge is lazily created on first call (checks for existing element first).

## main.css — .pb-orb__badge

```css
.pb-orb__badge {
  position:absolute; top:-5px; right:-5px;
  background:#ff4c4c; color:#fff; border-radius:50%;
  font-size:9px; font-weight:700; line-height:1;
  min-width:17px; height:17px; box-sizing:border-box;
  display:flex; align-items:center; justify-content:center; padding:0 3px;
  opacity:0; transform:scale(.5); transition:opacity .18s,transform .18s;
  pointer-events:none; border:2px solid rgba(10,16,30,.9);
}
.pb-orb__badge--vis { opacity:1; transform:scale(1); }
```

The `.pb-orb` is `position:fixed`, which establishes a containing block for
absolutely positioned children — no `position:relative` needed.
The dark border (`rgba(10,16,30,.9)`) creates a "cut-out" effect separating
the badge from the orb background, making it readable on any orb color.
Scale animation (0.5→1) on `--vis` class gives a springy pop-in feel.

## brainstorm.php — 3 new intent handlers

### 0d-pre21-a) Podcast / audio production / show notes site
Keywords: podcast production, audio editing, show notes, podcast website, podcast site,
podcast player, podcast hosting, audio recording, podcast episode, podcast rss,
distribute podcast, apple podcasts, spotify podcast

Response:
- Podcast website: episode archive, WaveSurfer.js waveform player, show notes,
  transcripts, guest bios, RSS feed; from $500
- Distribution: Buzzsprout / Transistor → Apple Podcasts, Spotify, Amazon, iHeart
- Transcript integration: Whisper AI / Rev → collapsible show notes; big SEO gain
- Audio production: out of scope — freelance engineer referrals from network
- Audiogram generator for social: static waveform clip export; $200 add-on
- Closes: "podcast site, embedded player, or both?"

### 0d-pre21-b) SaaS onboarding flow / activation / product-led growth
Keywords: saas onboarding, user onboarding, onboarding flow, onboarding wizard,
onboarding checklist, activation flow, product-led growth, plg, user activation,
welcome flow, first run experience, in-app onboarding

Response:
- Welcome wizard: 2–5 step setup; skippable; progress bar; persisted; from $350
- Checklist: Intercom-style; items checked as user acts; completion → confetti; $250
- Guided tour: Shepherd.js tooltip overlay; once-per-user; skippable; from $200
- Empty-state CTAs: friendly first-action prompts instead of blank screens
- Activation metric: define + instrument "aha moment" in GA4/Mixpanel; from $150
- Email drip on sign-up: 3–5 email sequence via Mailchimp/Klaviyo/Postmark; $200
- Closes: "what's the 'aha moment' for your product?"

### 0d-pre21-c) Multilingual / i18n / translated website
Keywords: multilingual, multi-language, translate website, website translation, i18n,
internationalisation, localisation, wpml, polylang, translatepress, rtl support,
arabic/chinese website

Response:
- WPML: industry standard, per-language pages, WooCommerce multilingual; $99/yr
- Polylang: lighter; free for 2 languages; good for blogs
- TranslatePress: visual front-end editor; AI auto-translate add-on; $89/yr
- Translation workflow: DeepL API for first draft, then human review or PO file export
- RTL support: Arabic, Hebrew, Urdu, Farsi; CSS direction:rtl; RTL theme tested
- SEO: hreflang tags per language; /fr/ /de/ slug structure; Yoast SEO Premium
- Closes: "how many languages + which, and is content already translated?"

## QA results (32/32 all pass)
| Check | Result |
|-------|--------|
| _greetUser defined | OK |
| Good morning/afternoon/evening | OK |
| chatMsgs.length===0 guard | OK |
| _greetUser from openBrain | OK |
| _greetUser from clearChat | OK |
| _updateOrbBadge defined | OK |
| pb-orb__badge in JS | OK |
| bot count filter | OK |
| 9+ cap | OK |
| --vis toggled | OK |
| _updateOrbBadge after bot msg | OK |
| _updateOrbBadge in closeBrain | OK |
| hidden when brain visible | OK |
| pb-orb__badge CSS | OK |
| position:absolute | OK |
| red background | OK |
| opacity:0 default | OK |
| --vis opacity:1 | OK |
| scale transition | OK |
| podcast keywords | OK |
| WaveSurfer | OK |
| Whisper AI | OK |
| Buzzsprout | OK |
| saas onboarding keywords | OK |
| Shepherd.js | OK |
| aha moment | OK |
| email drip on sign-up | OK |
| multilingual keywords | OK |
| WPML | OK |
| TranslatePress | OK |
| hreflang | OK |
| RTL support | OK |
