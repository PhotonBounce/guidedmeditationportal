# DOM QA Report — R20 — 2026-06-20

## main.js — Chat drawer minimize/collapse toggle

New `−` button added to the header (between export and mute):

```javascript
var collapseBtn = document.createElement('button');
collapseBtn.className = 'pb-brain__collapse';
collapseBtn.innerHTML = '&#8722;'; // − minus sign
var _brainCollapsed = false;
collapseBtn.addEventListener('click', function() {
  _brainCollapsed = !_brainCollapsed;
  brain.classList.toggle('is-collapsed', _brainCollapsed);
  collapseBtn.innerHTML = _brainCollapsed ? '&#9652;' : '&#8722;'; // ▲ or −
  collapseBtn.title = _brainCollapsed ? 'Expand chat' : 'Minimize chat';
  collapseBtn.setAttribute('aria-label', _brainCollapsed ? 'Expand chat' : 'Minimize chat');
});
```

Header order: `[↺ newchat] [⇓ export] [− collapse] [🔊 mute] [× close]`

When collapsed:
- CSS hides `.pb-brain__log`, `.pb-brain__form`, `.pb-brain__listening`, `.pb-brain__scroll-down`
- `brain.is-collapsed` reduces to `height:auto; min-height:0` so only the header shows
- Button icon changes to `▲` to indicate "click to expand"
- Aria-label toggles between "Minimize chat" / "Expand chat"

This lets the user keep the chat session alive and accessible while reclaiming vertical space.

## main.css — .pb-brain__collapse + .is-collapsed

```css
.pb-brain__collapse {
  background:none; border:1px solid rgba(255,255,255,.15);
  color:rgba(255,255,255,.5); width:28px; height:28px; border-radius:50%;
  font-size:16px; cursor:pointer; display:flex;
  align-items:center; justify-content:center; flex-shrink:0; transition:.15s;
}
.pb-brain__collapse:hover { border-color:rgba(255,255,255,.4); color:rgba(255,255,255,.9); }
.pb-brain.is-collapsed .pb-brain__log,
.pb-brain.is-collapsed .pb-brain__form,
.pb-brain.is-collapsed .pb-brain__listening,
.pb-brain.is-collapsed .pb-brain__scroll-down { display:none !important; }
.pb-brain.is-collapsed { min-height:0; height:auto; }
```

`!important` on the display:none rules to override any existing display values
from other rules or inline styles (e.g., the listening banner's `display:flex`).

## brainstorm.php — 3 new intent handlers

### 0d-pre16-a) Stripe / subscription billing / SaaS pricing model
Keywords: stripe integration, stripe setup, payment gateway, stripe connect,
subscription billing, recurring billing, billing portal, stripe customer portal,
usage-based pricing, freemium model, tiered pricing setup, metered billing,
stripe webhooks, stripe subscription, payment integration, accept payments

Response:
- Products & prices — monthly/annual subscriptions, one-time, metered usage; synced to WP
- Customer portal — Stripe's hosted portal; plan changes, cancellation, invoices
- Webhooks — payment.succeeded / subscription.updated / deleted → WP role/feature sync
- Coupons & trials — 7/14/30-day trials, %, fixed, referral discounts; all native Stripe
- Alternatives — PayPal, Square, LemonSqueezy, Paddle (EU VAT)
- Covered in SaaS/App $750+; closes with plan structure question

### 0d-pre16-b) UX research / usability testing / design audit
Keywords: ux research, usability testing, user testing, user research, user interviews,
usability audit, heuristic evaluation, accessibility audit, a11y audit, design audit,
ux audit, ui ux review, wcag

Response:
- Heuristic evaluation: 10 Nielsen heuristics, severity ratings; from $100
- Usability testing: task-based remote sessions, 5-user rule, report + fix list
- Accessibility audit (WCAG 2.1 AA): axe DevTools + NVDA/VoiceOver manual; from $150
- Design audit: contrast, typography, spacing, UX patterns; Figma annotations
- Prototype testing: Figma interactive → test before build
- Closes: "validate new design, audit existing, or improve conversion?"

### 0d-pre16-c) Cybersecurity / security audit / pen test / hardening
Keywords: security audit, penetration testing, pen test, pentest,
vulnerability scan, owasp, xss protection, sql injection, website security,
security review, csrf protection, security hardening, brute force protection

Response:
- OWASP Top 10 code review: XSS/SQLi/CSRF/broken auth/IDOR; severity report
- WordPress hardening: brute-force protection, XML-RPC off, hidden login, 2FA
- Dependency scan: npm audit + WP plugin vulnerability check
- Security headers: CSP, HSTS, X-Frame-Options, Permissions-Policy, Referrer-Policy
- Full pen test (OWASP ZAP + manual): from $300 standalone, $150 add-on
- PCI DSS / SOC 2 / HIPAA: certified auditor required; can refer one
- Closes: "existing site with possible breach, or new build to lock down?"

## QA results (23/23 all pass)
| Check | Result |
|-------|--------|
| collapseBtn element created | OK |
| pb-brain__collapse class | OK |
| minus char initial HTML | OK |
| _brainCollapsed declared | OK |
| classList toggle is-collapsed | OK |
| ▲ icon when collapsed | OK |
| collapseBtn in btnGroup | OK |
| pb-brain__collapse CSS | OK |
| is-collapsed hides log | OK |
| is-collapsed hides form | OK |
| is-collapsed height:auto | OK |
| Stripe intent keywords | OK |
| Stripe webhooks | OK |
| Customer portal | OK |
| LemonSqueezy alt | OK |
| UX intent keywords | OK |
| Heuristic evaluation | OK |
| WCAG accessibility | OK |
| Figma prototype | OK |
| Security intent keywords | OK |
| OWASP Top 10 | OK |
| CSP headers | OK |
| PCI DSS disclaimer | OK |
