# DOM QA Report — R58 — 2026-06-20

## main.js — R57 rollback + R58 Conversion CTA card

### R57 rollback (duplicate thumbs rating removed)

The R57 `pb-brain__rating` / `pb-brain__rate-btn` block was discovered to be redundant with
the pre-existing `pb-brain__fb` / `pb-brain__fb-btn` "Was this helpful?" feature that was
already part of the codebase before R55. Both showed 👍 👎 on every bot reply. The R57
block was removed from JS (and its corresponding CSS from the R57 addition to main.css)
to restore a single coherent feedback UI.

**What pb-brain__fb does (retained)**: after every bot message, two thumb buttons appear.
Clicking one permanently locks both (disabled=true), fades out the unchosen button to 20%
opacity, and fires a `window.gtag('event', 'chat_feedback', { value: 1 or 0 })` event.
The GA4 event is the primary value — it lets the site owner see in Google Analytics which
AI replies are rated helpful vs unhelpful.

### R58 CTA conversion card

When a bot reply contains pricing signals or booking signals, a small gold-bordered card
appears below the response: "Ready to get started?" with a "Book a free 15-min call ↗"
button. Clicking the button auto-submits "Book a free 15-min call" as a user message,
which the AI concierge routes to a booking confirmation flow.

```javascript
// R58: Conversion CTA card — injects a book-a-call prompt when bot reply mentions pricing or booking.
if (cls === 'bot') {
  var _lrp = plain(text).toLowerCase();
  var _hasCta = /from \$|from £|\/month|\$\d|£\d|book a|free call|get in touch|contact us|starting from|from just|our prices|pricing|get started/.test(_lrp);
  if (_hasCta) {
    var _oldCta = log.querySelector('.pb-brain__cta-card');
    if (_oldCta) _oldCta.remove();
    var _cta = document.createElement('div');
    _cta.className = 'pb-brain__cta-card';
    _cta.innerHTML = '<span class="pb-brain__cta-msg">Ready to get started?</span>'
      + '<button type="button" class="pb-brain__cta-btn">Book a free 15-min call &#8599;</button>';
    _cta.querySelector('.pb-brain__cta-btn').addEventListener('click', function() {
      _cta.remove();
      if (input && form) { input.value = 'Book a free 15-min call'; form.dispatchEvent(new Event('submit', { bubbles: true })); }
    });
    log.appendChild(_cta);
    requestAnimationFrame(function() { requestAnimationFrame(function() { _cta.classList.add('pb-brain__cta-card--vis'); }); });
  }
}
```

**Regex design**: the alternation covers the most common ways the PHP handlers signal price
or booking intent:
- `from \$` / `from £` — matches "From $600" / "From £500" (PHP handlers use "From \$nnn")
- `\/month` — matches "$99/month" in recurring service responses
- `\$\d` / `£\d` — matches any inline price mention like "$200" or "£150"
- `book a`, `free call`, `get in touch`, `contact us` — booking and contact signals
- `starting from`, `from just` — alternative price intro patterns
- `our prices`, `pricing`, `get started` — explicit pricing intent signals

**`plain(text)`**: strips all HTML from the bot reply before applying the regex. This is
important because bot responses contain HTML entities (`&bull;`, `<strong>`, `\$600`) and
the regex is matching plaintext concepts, not HTML markup.

**Single-card policy**: `var _oldCta = log.querySelector('.pb-brain__cta-card'); if (_oldCta) _oldCta.remove();` ensures only one CTA card is visible at a time. If the user gets two pricing replies in a row, the card updates in place rather than stacking.

**User-message dismissal**: the R55 user-cleanup line was extended to also remove any
existing CTA card when the user sends a message. This prevents the CTA from lingering
awkwardly while the bot is thinking:
```javascript
if (cls === 'user') {
  var _ehEl = log.querySelector('.pb-brain__empty-hints'); if (_ehEl) _ehEl.remove();
  var _ctaEl = log.querySelector('.pb-brain__cta-card'); if (_ctaEl) _ctaEl.remove();
}
```

**Double-rAF fade-in**: same pattern as empty-hints (R55) and smart chips — two animation
frames ensure the opacity:0 initial state is committed before the --vis class triggers the
CSS transition.

**Append to `log` not `div`**: the CTA card is appended to the log container, not the
message div, so it appears as a free-floating card below the message and smart chips — not
squashed inside the bot bubble.

## main.css — R57 CSS removed, R58 CTA CSS added

```css
/* R58 — conversion CTA card (book-a-call prompt on pricing replies) */
.pb-brain__cta-card{
  display:flex; align-items:center; justify-content:space-between; flex-wrap:wrap;
  gap:10px; margin:6px 12px 4px;
  padding:10px 14px;
  background:rgba(255,212,0,.07);
  border:1px solid rgba(255,212,0,.28);
  border-radius:10px;
  opacity:0; transform:translateY(5px);
  transition:opacity .3s ease, transform .3s ease;
}
.pb-brain__cta-card.pb-brain__cta-card--vis{
  opacity:1; transform:translateY(0);
}
.pb-brain__cta-msg{
  font-size:12px; color:rgba(255,255,255,.7); line-height:1.3;
}
.pb-brain__cta-btn{
  background:rgba(255,212,0,.15);
  border:1px solid rgba(255,212,0,.5);
  border-radius:8px; cursor:pointer;
  font-size:12px; font-weight:500; padding:5px 12px;
  color:rgba(255,212,0,.95);
  transition:background .15s, border-color .15s;
  white-space:nowrap;
}
.pb-brain__cta-btn:hover{
  background:rgba(255,212,0,.25);
  border-color:rgba(255,212,0,.75);
}
```

**Gold at 7% opacity background + 28% border**: the CTA card uses the same gold family
(`#ffd400`) as the rest of the widget. At 7% fill it's clearly distinct from bot bubbles
without being garish. The 28% border is perceptible at the widget's dark background while
remaining subtle.

**`flex-wrap:wrap`**: on narrow screens (mobile), the label and button wrap to two rows
with the 10px gap preserving readability.

**`transform:translateY(5px)` slide-in**: a small vertical slide combined with the opacity
transition matches the motion language of the empty-hints tiles (R55) and smart chips.

**`white-space:nowrap` on button**: "Book a free 15-min call ↗" is the canonical CTA text
and should never wrap mid-phrase. The `flex-wrap:wrap` is on the card container, so if the
label is long, the button wraps to a new row as a full-width block — not the button text
itself breaking.

## brainstorm.php — 3 new intent handlers

### 0d-pre54-a) Estate agent / lettings agent / property developer / block management
Keywords: estate agent website, letting agent website, property developer website,
property management website, property website, estate agency website, real estate website,
property listing website, house sales website, property portal website, hmo management
website, buy to let management website, property investment website, new homes developer
website, property finder website, block management website

Response: vendor / buyer / landlord / tenant — three audiences, one site
- Property listing and search: Rightmove/Zoopla feed (Reapit/Alto/Jupix); ValPal IOV
  widget; live listings; from $400
- Rightmove/Zoopla advertising pages: Featured Agent + Premier Agent status; from $100
- Instant online valuation (IOV): ValPal/Hometrack/Sprift; #1 lead-gen tool for agents;
  4x more likely to book appraisal; from $200
- Vendor guides / market reports: Land Registry price trends; local expert positioning;
  from $150
- Landlord services: Tenant Fees Act 2019 compliance; TDS/DPS/myDeposits; Client Money
  Protection (CMP) scheme membership; ARLA Propertymark; from $200
- CMP compliance (legally required since April 2019): criminal offence not to belong;
  up to $30,000 fine; mandatory display on website; from $100
- Tenant area: Goodlord/Vouch referencing; deposit protection; right to rent; from $150
- Block management page: service charges, Section 20; ground rent; from $150
- Redress scheme membership (mandatory): Property Ombudsman or Property Redress Scheme;
  must appear on site; from $80
- Area pages: hyper-local content per neighbourhood; sold prices; school catchments;
  from $150/page
- From $600 / $1,500+

### 0d-pre54-b) Hotel / boutique hotel / B&B / holiday cottage / glamping / serviced apartment
Keywords: hotel website, boutique hotel website, bed and breakfast website, bb website,
holiday cottage website, holiday let website, holiday rental website, serviced apartment
website, glamping website, guest house website, airbnb website, self catering website,
lodge website, resort website, country house hotel website, wedding venue website

Response: convert OTA lookers to direct bookers (save 15-25% commission)
- Direct booking engine: Beds24, Lodgify, Little Hotelier, ResNexus; zero commission;
  rate parity awareness; from $300
- Best rate guarantee banner: direct price matches/beats OTA; book-direct discount;
  from $100
- Rooms/accommodation pages: each room/cottage as own page; professional photography;
  occupancy; bed config; view; accessibility; from $200
- Photography + virtual tour: highest ROI investment; Matterport 360°; drone;
  +40% conversion vs phone images; from $250
- Experiences + local area guide: restaurants; attractions; events; reduces arrival
  anxiety; from $150
- Gift vouchers: GiftPro/WooCommerce; high-margin zero-inventory revenue; from $200
- Weddings and events: exclusive hire; capacity; catering; highest-value transactions
  on the site; from $200
- Spa and dining menus: seasonal PDFs; afternoon tea; private dining; from $150
- TripAdvisor + Google Reviews: schema markup; certificate of excellence widget; from $100
- Accessibility statement: Equality Act 2010; step-free; hearing loops; from $100
- From $600 / $1,500+

### 0d-pre54-c) Event photographer / wedding photographer / videographer
Keywords: photographer website, photography website, wedding photographer website, event
photographer website, videographer website, commercial photographer website, portrait
photographer website, family photographer website, newborn photographer website, product
photographer website, aerial photographer website, drone photographer website, corporate
photographer website, music photographer website, sports photographer website, fashion
photographer website

Response: portfolio load speed + "I can tell YOUR story" messaging
- Portfolio gallery (optimised): WebP/AVIF compressed; lazy loading; 15-25 curated
  hero images per genre; Squarespace/Format/Envira Gallery; from $300
- Image optimisation pipeline: WebP + responsive srcset + CDN (Cloudflare/BunnyCDN);
  PageSpeed 90+ mobile target; from $200
- Packages and pricing page: coverage hours; albums; engagement session; elopement;
  opacity on pricing loses leads to competitors; from $150
- Inquiry form + availability: HoneyBook/Dubsado/Calendly; auto-response email; 40%
  of leads lost to slow follow-up; from $200
- Real wedding blog posts (venue SEO): "Sophie & James at [Venue]"; 800+ words with
  venue, florist, dress designer; ranks for "[venue] wedding photographer"; from $150/post
- About page with story: personality + approach converts undecided leads at same price;
  from $150
- Style guide / FAQ: what to wear; rain contingency; delivery timeline and format;
  from $100
- Location pages: "[city] wedding photographer" per market; from $100/page
- Client galleries / delivery portal: Pixieset/Pic-Time/Shootproof; download + print
  ordering; embedded on own domain; from $200
- Styled shoots collaborations: Junebug/Styled Shoots UK; luxury market authority;
  from $100
- From $550 / $1,200+

## QA results (45/45 all pass)

| Check | Result |
|-------|--------|
| R57 thumbs REMOVED | OK |
| pb-brain__rating NOT in JS | OK |
| R58 comment present | OK |
| cta-card class near R58 | OK |
| _hasCta regex | OK |
| regex book a | OK |
| regex pricing | OK |
| oldCta.remove() | OK |
| cta-msg in innerHTML | OK |
| cta-btn in innerHTML | OK |
| Book a free 15-min call (w728) | OK |
| cta-card--vis (w1171) | OK |
| R55 cleanup removes cta-card | OK |
| no U+2018 | OK |
| R57 CSS REMOVED | OK |
| R58 CTA CSS comment | OK |
| .pb-brain__cta-card rule | OK |
| cta gold border | OK |
| cta opacity:0 | OK |
| cta-card--vis opacity:1 | OK |
| .pb-brain__cta-msg rule | OK |
| .pb-brain__cta-btn rule | OK |
| cta-btn gold bg | OK |
| cta-btn gold text (w728) | OK |
| estate agent + letting keywords | OK |
| Rightmove + Zoopla | OK |
| CMP criminal offence | OK |
| Property Ombudsman | OK |
| Tenant Fees Act 2019 | OK |
| ValPal IOV | OK |
| estate agent price line | OK |
| hotel + holiday cottage keywords | OK |
| OTA 15-25% commission | OK |
| Little Hotelier + Beds24 | OK |
| best rate guarantee | OK |
| GiftPro vouchers | OK |
| TripAdvisor | OK |
| hotel price line | OK |
| photographer + wedding keywords | OK |
| WebP + AVIF | OK |
| HoneyBook + Dubsado | OK |
| Pixieset gallery | OK |
| venue SEO posts | OK |
| PageSpeed target | OK |
| photographer price line | OK |
