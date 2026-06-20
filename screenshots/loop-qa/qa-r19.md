# DOM QA Report — R19 — 2026-06-20

## main.js — "Was this helpful?" thumbs feedback on bot messages

Every bot message now gets a thumbs-up / thumbs-down feedback pair injected
after the copy button and before the timestamp:

```javascript
if (cls === 'bot') {
  var _fbDiv = document.createElement('div');
  _fbDiv.className = 'pb-brain__fb';
  // upBtn (👍) + dnBtn (👎) created with aria-labels
  var _pbFbLock = function(chosen) {
    [_upBtn, _dnBtn].forEach(function(b) {
      b.disabled = true;
      b.style.opacity = b === chosen ? '1' : '0.2';
    });
    try { if (window.gtag) window.gtag('event', 'chat_feedback', { value: chosen === _upBtn ? 1 : 0, event_category: 'chatbot' }); } catch(e) {}
  };
  _upBtn.addEventListener('click', function() { _pbFbLock(_upBtn); });
  _dnBtn.addEventListener('click', function() { _pbFbLock(_dnBtn); });
  _fbDiv.appendChild(_upBtn); _fbDiv.appendChild(_dnBtn);
  div.appendChild(_fbDiv);
}
```

Behavior:
- Hidden at opacity:0 by default (matches timestamp behavior); appears on :hover
- `pointer-events:none` by default so no accidental hover-focus; restored on :hover
- On click: both buttons disabled, chosen = opacity 1, unchosen = opacity 0.2
- Fires `window.gtag('event', 'chat_feedback', {value: 1/0})` if GA4 is loaded
- Closure-scoped per message — each bot reply has independent lock state
- Uses `var _pbFbLock` (not function declaration) to avoid strict-mode issues in if-blocks

## main.css — pb-brain__fb styles

```css
.pb-brain__fb {
  display:flex; gap:4px; margin-top:6px;
  opacity:0; transition:opacity .2s; pointer-events:none;
}
.pb-brain__msg:hover .pb-brain__fb { opacity:1; pointer-events:auto; }
.pb-brain__fb-btn {
  background:none; border:1px solid rgba(255,255,255,.12);
  border-radius:4px; color:rgba(255,255,255,.35);
  cursor:pointer; font-size:11px; padding:2px 6px;
  transition:border-color .15s,color .15s,opacity .15s; line-height:1.4;
}
.pb-brain__fb-btn:hover:not(:disabled) { border-color:rgba(255,255,255,.45); color:rgba(255,255,255,.95); }
.pb-brain__fb-btn:disabled { cursor:default; }
```

Same hide-on-idle / reveal-on-hover pattern as the timestamp. `:not(:disabled)`
guard prevents hover styles on the deselected button after locking.

## brainstorm.php — 3 new intent handlers

### 0d-pre15-a) Blockchain / Web3 / smart contracts / NFTs / dApps
Keywords: blockchain, web3, smart contract, nft, nft marketplace, defi, cryptocurrency,
crypto wallet, dapp, decentralized, solidity, ethereum, polygon, token, web3 integration,
metamask, walletconnect, ipfs, solana, base chain, layer 2

Response:
- Smart contracts: Solidity (ERC-20, ERC-721, ERC-1155); Hardhat; audit strongly recommended
- dApp frontend: ethers.js/wagmi + Next.js; MetaMask + WalletConnect; ENS resolution
- NFT marketplace: custom OpenSea-style or storefront; IPFS/Pinata for metadata
- Token gating: restrict content to wallet holders; WP plugin or custom API middleware
- Crypto payments: Coinbase Commerce or direct wallet on WP/Next.js
- From $750+; closes: "What chain — Ethereum, Polygon, Solana, Base?"

### 0d-pre15-b) No-code / low-code / Webflow / Bubble / Framer
Keywords: no-code, low-code, nocode, webflow, bubble.io, framer, squarespace, wix,
no code tool, webflow vs wordpress, webflow vs custom, framer vs, vs webflow, which platform

Response: honest comparison format
- Webflow/Framer: design-heavy, limited custom logic, harder to hand off to devs
- Squarespace/Wix: fastest for simple needs; platform lock-in; limited custom logic
- WordPress: CMS-heavy sites, client-editable, huge plugin ecosystem — primary stack
- Next.js/React: apps, dashboards, user accounts, high-performance
- Custom is worth it when: custom functionality, no platform ceiling, scaling beyond templates
- Closes: "What does the site need to do that a template can't handle?"

### 0d-pre15-c) Data visualization / charts / dashboards
Keywords: data visualization, data viz, chart, graphs, dashboard analytics,
interactive chart, d3.js, chart.js, recharts, data dashboard, reporting dashboard,
analytics dashboard, charting library, visualize data, echarts, leaflet, mapbox

Response: library decision matrix
- Chart.js: fast, common types (line/bar/pie/radar), lightweight, bundled free in SaaS
- D3.js: max flexibility, force-directed, geographic; complex builds from $400
- Recharts/Nivo: React-native, declarative API for Next.js dashboards
- Leaflet.js/Mapbox GL: geographic data + choropleth; custom tiles; offline capable
- Apache ECharts: large data volumes; financial/operational dashboards
- Live data: REST API or WebSocket; real-time no reload
- Closes: "How much data, update frequency, interactive filtering needed?"

## QA results (27/27 all pass)
| Check | Result |
|-------|--------|
| _fbDiv created | OK |
| pb-brain__fb class | OK |
| _upBtn 👍 HTML | OK |
| _dnBtn 👎 HTML | OK |
| _pbFbLock function | OK |
| GA4 gtag event | OK |
| buttons disabled on lock | OK |
| fbDiv appended | OK |
| cls bot guard | OK |
| pb-brain__fb CSS | OK |
| fb opacity:0 default | OK |
| fb hover opacity:1 | OK |
| pointer-events:auto on hover | OK |
| pb-brain__fb-btn CSS | OK |
| disabled cursor:default | OK |
| Blockchain intent keywords | OK |
| Solidity in response | OK |
| wagmi + Next.js | OK |
| IPFS in NFT | OK |
| No-code intent keywords | OK |
| Webflow in response | OK |
| honest comparison framing | OK |
| Data viz intent keywords | OK |
| Chart.js | OK |
| D3.js | OK |
| Leaflet | OK |
| live data WebSocket | OK |
