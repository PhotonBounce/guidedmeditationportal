# DOM QA Report — R30 — 2026-06-20

## main.js — Document title "Photon is thinking…" while API pending

When the user sends a message and the API fetch is in flight, `document.title`
is changed to `'Photon is thinking…'`. It is restored in the `finally` block
so it fires on both success and error paths.

```javascript
const typing = addTyping();
input.disabled = true;
var _savedTitle = document.title;           // ← save before modify
document.title = 'Photon is thinking…';    // ← shown in browser tab
if (sendBtn) { sendBtn.disabled = true; sendBtn.textContent = '…'; }
try {
  const r = await fetch(endpoint, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      message: text, history: history,
      path: location.pathname,
      title: _savedTitle   // ← send original title, not the modified one
    })
  });
  ...
} finally {
  input.disabled = false;
  if (sendBtn) { sendBtn.disabled = false; sendBtn.textContent = 'Send'; }
  document.title = _savedTitle;  // ← always restore
  input.focus();
}
```

Behavior:
- `_savedTitle = document.title` is captured BEFORE modifying it, so the fetch
  body still sends the correct page title as context to the AI (not "Photon is
  thinking…" which would be confusing as page context)
- The modified title is visible in the browser tab and in the taskbar/dock
  while the user is waiting — particularly useful if they tab away
- `finally` block guarantees restoration even on network error or JSON parse
  failure; the user never gets a stuck "thinking…" tab
- No CSS changes required — this is purely a JS behavioural enhancement

## brainstorm.php — 3 new intent handlers

### 0d-pre26-a) Car dealership / automotive / vehicle sales site
Keywords: car dealership website, automotive website, vehicle dealership,
car sales website, used car dealership, new car dealership, car lot website,
auto dealership, vehicle sales site, car showroom website, used cars website,
motorbike dealership, truck dealership

Response:
- Vehicle inventory CPT: VIN, make, model, year, mileage, price, condition,
  colour, photos; advanced filter search; staff-editable; from $500
- Finance calculator: deposit/term/APR → estimated monthly payment; links to
  finance partner enquiry form
- Trade-in enquiry: registration/VIN → mileage → condition → contact; email alert
  to sales team
- Test drive booking: specific vehicle pre-selected, date/time, Stripe £0 pre-auth
  to reduce no-shows, SMS confirmation; from $300
- Video walkarounds: per-vehicle YouTube/Vimeo embed or self-hosted; muted autoplay
- Local SEO: "[make] dealership [city]" clusters; Google Business; Vehicle schema
- OEM note: franchise dealers (Ford/VW/BMW) may need OEM-mandated platforms;
  I build for independent dealers only
- From $700 used-car lot / $1,500+ full stack
- Closes: "stock count and new / used / specialist?"

### 0d-pre26-b) Accountant / bookkeeper / tax professional / CPA
Keywords: accountant website, bookkeeper website, accountancy website,
tax professional, cpa website, chartered accountant, bookkeeping website,
tax advisor website, payroll service website, accounting firm website,
tax return website, financial accountant

Note: this handler fires BEFORE the law firm 0d-pre22-c handler (which also
catches 'accountant website'), so accountant queries now get a more specific
and tailored response.

Response:
- Service pages: tax, bookkeeping, payroll, accounts filing, VAT, management
  accounts, R&D tax credits — each page targets its own keyword cluster
- Client portal link: Xero, QuickBooks, FreeAgent, TaxDome, Iris; auth by platform
- Secure document exchange: restricted WP media library (client role) or Dropbox
  Business; encrypted upload form; from $200
- Discovery call booking: Calendly 15-min free consultation; highest-converting CTA
- GDPR/data: privacy policy, cookie consent, DPA; no financial data in WP DB
- Professional body badges: ICAEW, ACCA, CIMA, AAT (UK); AICPA, CPA (US)
- From $450 sole practitioner / $900+ firm with multiple service lines
- Closes: "team size and which accounting software do clients use?"

### 0d-pre26-c) Pet business / grooming studio / vet clinic / pet shop
Keywords: pet business website, pet grooming website, dog grooming website,
veterinary website, vet website, pet shop website, pet supplies website,
pet care website, animal shelter website, dog training website, cattery website,
kennels website, dog walker website, pet photography

Response:
- Booking: pet type + service + groomer/vet + date/time + Stripe deposit;
  SMS + email reminders; cancel/reschedule self-service; from $350
- Team + service menu: bios, certs, specialisms, direct-book; duration +
  price + breed-size surcharges
- Before/after gallery: filterable by breed; WebP lazy-loaded; SEO content
- Pet records portal (vet): links to ezyVet, VetsPetPortal, Vet24; auth by platform
- E-commerce (pet shop): WooCommerce; variable shipping; Stripe + PayPal;
  subscription auto-ship for food/treats; from $500
- Google Reviews + Local Business schema: "trust signals especially critical
  for pet care where owners are emotionally invested"
- From $400 grooming / $600 vet / $900+ pet shop
- Closes: "grooming / vet / pet shop / combination?"

## QA results (20/20 all pass)
| Check | Result |
|-------|--------|
| _savedTitle captures title | OK |
| title → "thinking" while pending | OK |
| fetch body uses _savedTitle | OK |
| finally restores title | OK |
| car dealership keywords | OK |
| VIN/make/model CPT | OK |
| finance calculator | OK |
| Vehicle schema markup | OK |
| OEM / franchise note | OK |
| from $700 car lot | OK |
| accountant keywords | OK |
| Xero/QuickBooks/FreeAgent/TaxDome | OK |
| ICAEW/ACCA/CIMA | OK |
| Calendly discovery call | OK |
| from $450 sole practitioner | OK |
| pet keywords | OK |
| ezyVet / VetsPetPortal | OK |
| WooCommerce subscription auto-ship | OK |
| from $400 grooming | OK |
| emotionally invested trust signals | OK |
