# DOM QA Report — R22 — 2026-06-20

## main.js — In-chat search bar (Ctrl+F)

New `_srchInput` element inserted between the header and the message log.
Triggered by `Ctrl+F` while the brain drawer is visible:

```javascript
var _srchInput = document.createElement('input');
_srchInput.type = 'search';
_srchInput.className = 'pb-brain__search';
_srchInput.setAttribute('placeholder', 'Search messages…');
_srchInput.setAttribute('aria-label', 'Search chat messages');
if (log && log.parentNode) log.parentNode.insertBefore(_srchInput, log);

function _srchFilter() {
  var q = _srchInput.value.toLowerCase().trim();
  var msgs = log ? log.querySelectorAll('.pb-brain__msg') : [];
  [].forEach.call(msgs, function(m) {
    m.style.opacity = (!q || m.textContent.toLowerCase().indexOf(q) !== -1) ? '' : '0.15';
  });
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
    _srchInput.focus(); _srchInput.select();
  }
  if (e.key === 'Escape' && document.activeElement === _srchInput) {
    _srchClose(); if (input) input.focus();
  }
});

// Auto-close search when brain hides
if (brain) new MutationObserver(function() {
  if (brain.offsetParent === null) _srchClose();
}).observe(brain, { attributes: true, attributeFilter: ['style', 'class'] });
```

Behavior:
- Hidden by default (display:none); appears above the message log when Ctrl+F is pressed
- Non-matching messages: `opacity:0.15` (dims them, keeps layout intact — no jumping)
- Matching messages: `opacity:''` (reset to default)
- Escape key: clears query, restores all messages, removes `--open` class, returns focus to input
- Native `type="search"` gives a built-in ✕ clear button in Chrome/Safari; input event
  fires on clear so the filter resets automatically
- MutationObserver watches the brain element for style/class changes; calls `_srchClose()`
  when `offsetParent === null` (brain hidden), so stale filter state is never left behind
- `brain.offsetParent !== null` guards against triggering Ctrl+F when the chat is closed

## main.css — .pb-brain__search

```css
.pb-brain__search {
  display:none; width:100%; box-sizing:border-box;
  background:rgba(255,255,255,.06); border:none;
  border-bottom:1px solid rgba(255,255,255,.12);
  color:#fff; font-size:13px; outline:none;
  padding:8px 12px; transition:background .15s,border-color .15s;
}
.pb-brain__search--open { display:block; }
.pb-brain__search::placeholder { color:rgba(255,255,255,.3); }
.pb-brain__search:focus {
  background:rgba(255,255,255,.09);
  border-bottom-color:rgba(255,212,0,.45);
}
.pb-brain__search::-webkit-search-cancel-button { cursor:pointer; }
```

No visible border on 3 sides — only a bottom separator line separating it from the log.
Matches the dark glassmorphism palette. Focus state uses the brand gold accent.

## brainstorm.php — 3 new intent handlers

### 0d-pre18-a) WordPress Multisite / multi-site network
Keywords: wordpress multisite, multisite, multi-site, multiple websites, site network,
subdomain network, subdirectory sites, wpmu, multi-tenant wordpress, manage multiple sites,
mainwp, managewp, network of sites, wp network, subsite

Response:
- Network setup: subdomain or subdirectory mode; wildcard DNS required for subdomains; from $150
- Shared codebase: one theme/plugin install; per-site Customizer overrides
- User management: super admin vs. site admin roles; single SSO login across sites
- Content sharing: shared media library, cross-site taxonomy, content mirroring
- MainWP dashboard: manage plugin/theme/core updates across 100s of sites from one screen
- Hosting: Kinsta and WP Engine support natively; standard shared hosting usually can't
- Closes: "how many sites, same or different designs, user management centralised?"

### 0d-pre18-b) Digital products / Easy Digital Downloads / downloadable files
Keywords: digital product, downloadable product, sell ebook, sell pdf, sell template,
sell digital download, sell presets, sell fonts, digital downloads, easy digital downloads,
edd, sell files, digital goods, sell software, gumroad alternative, digital store,
sell downloads, license key

Response:
- Easy Digital Downloads (EDD): per-download pricing, purchase logs, per-email download
  limits, software licence key add-on; from $350 standalone
- WooCommerce + virtual/downloadable: good when mixing physical and digital
- File security: signed S3 URL or WP nonce; files outside web root; count-limited links
- Delivery: confirmation email + customer re-download dashboard; PDF personalisation add-on
- No transaction tax: Stripe direct (no 9% Gumroad fee); Paddle for EU VAT
- Membership gating: MemberPress or Restrict Content Pro
- Closes: "files only or mix? And licence keys needed?"

### 0d-pre18-c) Print-on-demand / merchandise / Printful / Printify
Keywords: print on demand, printful, printify, pod store, custom merch, merchandise store,
sell t-shirts, sell hoodies, sell mugs, dropship merch, custom apparel, branded merchandise,
merch store, sell merch, print and ship, white label merch

Response:
- Printful or Printify + WooCommerce: syncs products/variants/pricing; no stock held
- Product range: t-shirts, hoodies, mugs, phone cases, posters, tote bags, hats
- Mockup generator: auto-produces product images; no photo shoot needed
- Custom branding: inside label, packing slip, thank-you card insert (Printful Pro)
- Order flow: customer orders → WooCommerce webhook → Printful fulfils → tracking emailed
- Margin: 30–40% typical; you set retail price, Printful deducts base cost
- From $600 for standalone store with domain, SSL, WooCommerce, Printful integration
- Closes: "what products, and add-on or standalone store?"

## QA results (28/28 all pass)
| Check | Result |
|-------|--------|
| _srchInput element created | OK |
| search type="search" | OK |
| pb-brain__search class | OK |
| inserted before log | OK |
| _srchFilter function | OK |
| opacity filter on mismatch | OK |
| _srchClose function | OK |
| Ctrl+F opens search | OK |
| Escape closes search | OK |
| pb-brain__search--open added | OK |
| MutationObserver cleanup | OK |
| pb-brain__search display:none | OK |
| --open display:block | OK |
| placeholder colour | OK |
| focus border gold | OK |
| webkit cancel cursor | OK |
| Multisite keywords | OK |
| MainWP dashboard | OK |
| Kinsta Multisite | OK |
| super admin role | OK |
| EDD keywords | OK |
| signed S3 URL | OK |
| Gumroad alternative | OK |
| Paddle EU VAT | OK |
| Printful keywords | OK |
| mockup generator | OK |
| 30-40% margin | OK |
| WooCommerce webhook | OK |
