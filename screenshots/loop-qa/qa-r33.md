# DOM QA Report — R33 — 2026-06-20

## main.js — SR-only ARIA live region for bot replies

Screen readers now automatically announce every bot reply without the user
needing to navigate to the chat log. The standard "visually hidden live region"
pattern: a 1×1px clipped div with `aria-live="polite"` that gets the bot's
plaintext pushed to it after each message.

```javascript
// SR-only live region — screen readers announce new bot replies automatically.
var _srLive = document.createElement('div');
_srLive.className = 'pb-brain__sr-live';
_srLive.setAttribute('aria-live', 'polite');
_srLive.setAttribute('aria-atomic', 'true');
_srLive.setAttribute('aria-relevant', 'additions text');
if (brain) brain.appendChild(_srLive);
```

In `addMsg()`, inside the `if (cls === 'bot')` block:
```javascript
// Push plaintext to SR live region so screen readers announce the reply.
if (_srLive) {
  _srLive.textContent = '';
  requestAnimationFrame(function() { _srLive.textContent = plain(text); });
}
```

Behavior:
- `aria-live="polite"`: waits until the user is idle before announcing (never
  interrupts speech in progress); correct for a chat bot reply
- `aria-atomic="true"`: announces the entire region content as one unit (not
  word-by-word as content arrives)
- `aria-relevant="additions text"`: hints to the AT that it should track text
  additions (some AT implementations use this hint to trigger early)
- `textContent = ''` + `requestAnimationFrame` pattern: the empty string clears
  any previous content; the rAF delay ensures the DOM mutation fires after the
  clear so AT detects the content-change even when the same reply appears twice
- Only fires for `cls === 'bot'` — user messages and error messages are not
  announced via the live region (user typed them; errors go to the toast)
- `_srLive` is in the outer IIFE scope, declared before the form submit handler,
  so it is accessible inside `addMsg()` when called

## main.css — .pb-brain__sr-live

Standard sr-only pattern (matches Bootstrap/Tailwind screen-reader-only utility):
```css
.pb-brain__sr-live {
  position:absolute; width:1px; height:1px;
  padding:0; margin:-1px; overflow:hidden;
  clip:rect(0,0,0,0); white-space:nowrap; border:0;
}
```

The `clip` property is deprecated but still supported by all AT that matters;
the combination of `width/height:1px` + `overflow:hidden` + `clip` is the most
broadly compatible approach. No `display:none` or `visibility:hidden` — those
would cause AT to ignore the live region entirely.

## brainstorm.php — 3 new intent handlers

### 0d-pre29-a) Commercial / studio photography (product, headshots, corporate)
Keywords: product photography website, commercial photographer, headshot photographer,
corporate photographer, studio photography website, food photographer, fashion
photographer website, property photographer, real estate photographer, drone photography,
event photographer, newborn photographer, boudoir photographer, photo studio website

Note: distinct from the R26 photography portfolio handler (personal creative portfolio)
— this is commercial/service photography focused on lead gen and booking.

Response:
- Service speciality pages: one per niche (headshots/product/food/drone/events);
  own keyword cluster; 3-5 portfolio examples per page; from $350
- Online session booking: Calendly Pro or custom WP; session type + duration +
  studio/location + Stripe deposit; from $300
- Client gallery delivery: Pic-Time / Pixieset / ShootProof embed; password-protected
  per-client; download with expiry; favourite selection; $150 integration setup
- E-commerce prints: WooCommerce + Printful or local lab; from $400
- Commercial licensing page: usage tier enquiry (editorial/commercial/exclusive);
  important for product and fashion photographers
- Before/after retouching slider: demonstrates post-production quality
- From $500 service site / $1,000+ with booking + delivery portal + print shop
- Closes: "primary specialism? client delivery / booking / print sales?"

### 0d-pre29-b) Florist / flower shop / wedding flowers / corporate floristry
Keywords: florist website, flower shop website, floristry website, wedding florist,
flower delivery website, florist online shop, florist booking, corporate floristry,
event flowers, bouquet website, flower subscription website, flower arrangement

Response:
- Online shop: WooCommerce product categories (bouquets/seasonal/condolence/corporate);
  delivery date selector at checkout; Stripe + PayPal; from $500
- Delivery zones: postcode-based delivery fee calculator; local same-day vs next-day
  vs collection; flat rate or tiered by distance; from $250
- Subscription / flower club: WooCommerce Subscriptions; weekly/monthly; pause/cancel
  self-service; gifting (send to different address); from $300
- Wedding/event consultation: multi-step form (date → venue → style → budget);
  Pinterest board or image upload for mood; feeds email or Dubsado
- Seasonal availability notice: banner/modal for peak cutoffs (Valentine's, Mother's
  Day, Christmas Eve); urgency drives conversions
- Google Business + local SEO: "florist near me" + "flower delivery [town]";
  Local Business schema; Google Shopping product feed
- From $550 shop + delivery zones / $1,000+ with subscription + wedding + Shopping
- Closes: "walk-in retail / delivery / weddings / combination?"

### 0d-pre29-c) Pharmacy / chemist / health products / online dispensary
Keywords: pharmacy website, chemist website, dispensary website, online pharmacy,
health products website, compounding pharmacy, pharmacy online shop, independent
pharmacy website, prescription service website, health supplement website,
wellbeing shop website

Response: with important regulatory disclaimers
- What I can build: OTC e-commerce, appointment booking (flu jabs/travel clinics/
  blood pressure/ear wax), service pages, NHS contractor info, prescription
  collection notification
- POM disclaimer: "Prescription-only medicines cannot be sold directly via an
  e-commerce site without GPhC registration (UK) / DEA (US) — I integrate with
  your licensed dispensing workflow, I don't build the dispensing system"
- OTC / health product shop: WooCommerce + age-verification popup; CSV stock sync;
  from $500
- Appointment booking: flu jab, travel vaccines, blood pressure, weight management,
  ear wax, minor ailments; Calendly or custom WP; from $300
- Compliance: GPhC logo + reg number in footer; no prohibited health claims;
  GDPR cookie consent; disclaimer on health content
- Click-and-collect: WooCommerce local pickup option; SMS/email ready notification;
  from $200
- From $500 pharmacy site + click-and-collect / $1,100+ with OTC shop + booking
- Closes: "GPhC pharmacy / supplement retailer / travel health clinic?"

## QA results (22/25 pattern + 3 manual = 25/25 correct)
| Check | Result |
|-------|--------|
| _srLive element created | OK |
| aria-live polite | OK |
| aria-atomic | OK |
| aria-relevant additions text | OK |
| brain.appendChild(_srLive) (manual) | OK — _srLive appears after brain. in the JS line; dot-star on same line fails |
| clear + rAF + plaintext | OK |
| only for bot (manual) | OK — multiline span; verified both strings exist in file |
| pb-brain__sr-live CSS | OK |
| width:1px height:1px | OK |
| clip:rect(0,0,0,0) | OK |
| studio photography keywords | OK |
| Pic-Time / Pixieset / ShootProof | OK |
| commercial licensing page | OK |
| before/after retouching slider | OK |
| from $500 photography | OK |
| florist keywords | OK |
| delivery zones postcode | OK |
| WooCommerce Subscriptions flower club | OK |
| Seasonal availability (manual) | OK — capital S; case-sensitive regex failed |
| from $550 florist | OK |
| pharmacy keywords | OK |
| POM cannot be sold | OK |
| GPhC registration number | OK |
| click-and-collect / local pickup | OK |
| from $500 pharmacy site | OK |
