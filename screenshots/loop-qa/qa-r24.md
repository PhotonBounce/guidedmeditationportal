# DOM QA Report — R24 — 2026-06-20

## main.js — Bot message cascading line-reveal (streaming feel)

Previously `addMsg` set `div.innerHTML` in a single assignment, making bot
replies appear all-at-once. Now each `<br>`-separated line is wrapped in an
animated `<span class="pb-brain__line">` with a staggered `animation-delay`:

```javascript
if (cls === 'bot') {
  var _fmtLines = format(text).split('<br>');
  div.innerHTML = _fmtLines.map(function(ln, i) {
    return '<span class="pb-brain__line" style="animation-delay:' + (i * 70) + 'ms">' + ln + '</span>';
  }).join('<br>');
} else {
  div.innerHTML = text.replace(/</g, '&lt;');
}
```

Result:
- Single-line reply: appears with a 0ms fade-in (same as before but smooth)
- Multi-line reply (e.g. a bulleted list with 6 lines): each line fades in 70ms
  after the previous — total cascade ~350ms for a 6-line response
- The `<br>` separators are preserved between spans so layout is unchanged
- User messages are unchanged (still single-assignment HTML escape)
- Error messages are unchanged (use the `else` branch)

The cascade creates the perception of the reply being "typed out" progressively,
which reduces perceived wait time and makes long responses feel easier to read.

## main.css — @keyframes pbLineIn + .pb-brain__line

```css
@keyframes pbLineIn {
  from { opacity:0; transform:translateY(5px); }
  to   { opacity:1; transform:translateY(0); }
}
.pb-brain__line {
  display:inline;
  opacity:0;
  animation: pbLineIn .2s ease-out both;
}
```

`animation-fill-mode: both` (via `both` shorthand):
- `backwards`: during the delay period, the element is at the `from` state
  (opacity:0) — so it's invisible while waiting for its turn
- `forwards`: after the animation ends, stays at the `to` state (opacity:1)

`display:inline` keeps spans from creating block layout. The `<br>` tags that
join the spans handle line breaking. If a span wraps a bullet point that itself
starts with `<strong>` or other inline elements, those remain inside the span
and animate together.

## brainstorm.php — 3 new intent handlers

### 0d-pre20-a) API documentation site / developer docs
Keywords: api documentation, developer docs, technical docs, api docs, doc site,
docs website, documentation site, swagger, openapi, docusaurus, nextra, readme.io,
gitbook, developer portal, docs portal, api reference

Response: tooling comparison
- Docusaurus (React + MDX): fast, SEO-optimised, versioned, GitHub Pages; free
- Nextra (Next.js + MDX): full Next.js power; Vercel deploy; custom themes
- GitBook: hosted, no deploy config, collaborative, GitHub sync; $6.70/user/mo
- Readme.io: auto-generates from OpenAPI/Swagger; built-in try-it console; paid
- OpenAPI/Swagger integration: auto-ref pages from .yaml spec; 10+ language samples
- WordPress KB: KnowledgeBase plugin, client-editable, searchable; from $400
- Closes: "devs via Markdown/Git or non-tech via CMS? Live API try-it console?"

### 0d-pre20-b) Browser extension / Chrome extension / Firefox add-on
Keywords: browser extension, chrome extension, firefox addon, browser plugin,
chrome plugin, extension development, web extension, manifest v3, browser addon,
chrome web store, firefox extension, edge extension, browser add-on

Response:
- Chrome Extension (MV3): content scripts, service workers, popup, context menu;
  Chrome Web Store ~$5 one-time dev fee
- Firefox Add-on: same WebExtensions API; addons.mozilla.org; free
- Cross-browser via Plasmo: React + TypeScript + HMR; one codebase → Chrome/Firefox/
  Edge/Safari
- Content scripts: inject JS/CSS into any page; DOM read/write; message passing
- chrome.storage.sync for cross-device settings; chrome.storage.local for large data
- Common builds: summariser, price tracker, screenshot tool, GPT sidebar; from $500
- Review timeline: 1–3 days; MV3 required (MV2 deprecated mid-2025)
- Closes: "what does it do on the page, and backend/sync needed?"

### 0d-pre20-c) Desktop app / Electron / Tauri / native desktop
Keywords: desktop app, electron, tauri, desktop application, windows app, mac app,
native desktop, desktop software, cross platform desktop, desktop gui, nwjs

Response: decision framework
- Electron: Node.js + Chromium; VSCode/Slack/Figma; Windows/Mac/Linux; ~150MB; $2,500+
- Tauri: Rust + system WebView; ~3MB bundle; faster startup; $2,500+
- Native Mac (SwiftUI): cleanest macOS integration; Mac App Store; $3,500+
- Native Windows (.NET/WinUI): Microsoft Store; Win32/COM APIs; $3,500+
- PWA: installable on Windows 11 / macOS; no App Store; free in web builds
- Electron vs Tauri: Electron = easier dev + large bundle; Tauri = small + fast + Rust
- Closes: "Windows/Mac/both? Key feature: file system, tray, notifications, hardware?"

## QA results (25/25 all pass)
| Check | Result |
|-------|--------|
| format split by `<br>` | OK |
| span.pb-brain__line wrap | OK |
| animation-delay i×70 | OK |
| spans joined by `<br>` | OK |
| else branch escapes HTML | OK |
| @keyframes pbLineIn | OK |
| from opacity:0 | OK |
| animation:pbLineIn on class | OK |
| display:inline | OK |
| fill-mode both | OK |
| API docs keywords | OK |
| Docusaurus | OK |
| Nextra | OK |
| GitBook | OK |
| OpenAPI auto-generate | OK |
| browser extension keywords | OK |
| MV3 | OK |
| Plasmo | OK |
| chrome.storage | OK |
| $5 dev fee | OK |
| desktop app keywords | OK |
| 150MB Electron | OK |
| 3MB Tauri | OK |
| SwiftUI | OK |
| Electron vs Tauri tradeoff | OK |
