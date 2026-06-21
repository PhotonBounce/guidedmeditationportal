# DOM QA Report — R35 — 2026-06-20

## main.js — Unread message count badge on the orb

The orb now shows a red circular badge with the count of bot replies received
while the drawer was closed. The previous implementation counted all-time bot
messages and showed the badge whenever the drawer was closed, even on first load.
The new implementation tracks only messages received since the drawer was last
opened, showing 0 (hidden) when the user opens the drawer and resetting each time.

### Changes to `_updateOrbBadge()`

Added `var _unreadCount = 0;` immediately before the function (function-scoped
via `var` hoisting to the IIFE; accessible everywhere in the IIFE after this point).

Old implementation counted `chatMsgs.filter(m => m.cls === 'bot').length` — shows
the total number of bot messages ever, visible on every close even for old sessions.

New implementation:
```javascript
var _unreadCount = 0;
function _updateOrbBadge() {
  if (!orb) return;
  var _badge = orb.querySelector('.pb-orb__badge');
  if (!_badge) {
    _badge = document.createElement('span');
    _badge.className = 'pb-orb__badge';
    _badge.setAttribute('aria-label', '0 unread messages');
    orb.appendChild(_badge);
  }
  _badge.textContent = _unreadCount > 9 ? '9+' : (_unreadCount > 0 ? String(_unreadCount) : '');
  _badge.setAttribute('aria-label', _unreadCount + ' unread message' + (_unreadCount !== 1 ? 's' : ''));
  _badge.classList.toggle('pb-orb__badge--vis', _unreadCount > 0);
}
```

- `aria-label` updates on every call (correct pluralisation: "1 unread message" vs
  "2 unread messages") — screen reader users who navigate to the orb button hear the
  unread count even though the badge text is visually inside the button
- Badge is hidden (`opacity:0` via CSS) when `_unreadCount === 0` — no pill shows on
  page load or after the user opens the drawer

### Increment in `addMsg()` bot branch

```javascript
if (cls === 'bot') {
  if (brain && brain.hidden) _unreadCount++;
  _updateOrbBadge();
  ...
}
```

Only increments if the drawer is currently `hidden` — if the user is watching the
conversation, the count stays at 0 (no false badge). `_updateOrbBadge()` is then
called to update the badge text and visibility immediately.

### Reset in `openBrain()`

```javascript
_unreadCount = 0;
_updateOrbBadge();
if (input) input.focus();
```

Resets and hides the badge as soon as the drawer opens (before `input.focus()`).
`_updateOrbBadge()` is also called by the open-button click listener and by
`closeBrain()` — having it in `openBrain()` itself covers keyboard-toggle opens
and any future programmatic opens.

## main.css — `.pb-orb__badge` / `.pb-orb__badge--vis`

```css
.pb-orb__badge {
  position:absolute; top:-4px; right:-4px;
  min-width:18px; height:18px; padding:0 4px;
  background:#e53935; color:#fff;
  font-size:10px; font-weight:700; line-height:18px; text-align:center;
  border-radius:9px; white-space:nowrap;
  box-shadow:0 1px 4px rgba(0,0,0,.4);
  opacity:0; transform:scale(.6);
  transition:opacity .18s ease, transform .18s ease;
  pointer-events:none;
}
.pb-orb__badge--vis { opacity:1; transform:scale(1); }
```

Design notes:
- `position:absolute; top:-4px; right:-4px` — anchored to the top-right corner of
  the orb button (`.pb-orb` already has `position:fixed` which creates a stacking
  context; the badge needs the orb to be `position:relative` — `fixed` elements
  establish a stacking context so children position relative to the fixed ancestor)
- `#e53935` — Material Design Red 600; universally recognised as "notification badge"
- `min-width:18px` — ensures "1" doesn't make an oval; `border-radius:9px` keeps it
  circular at 18×18 and pill-shaped for "9+"
- `scale(.6)` default — the badge pops in with a scale animation, not just a fade
- `pointer-events:none` — the orb's click handler still fires

## brainstorm.php — 3 new intent handlers

### 0d-pre31-a) Tattoo studio / artist / piercing / body art
Keywords: tattoo studio website, tattoo artist website, tattoo shop website,
tattoo parlour website, tattoo booking website, tattoo portfolio website,
custom tattoo website, tattoo flash website, piercing website, body art website,
tattoo artist portfolio, ink studio website

Response:
- Artist portfolios: one page per artist; filterable by style (traditional/blackwork/
  realism/watercolour/fine-line/Japanese/neo-trad/geometric); WebP + Lightbox; from
  $300 per artist / $500 multi-artist studio
- Online booking: consultation request with style reference upload, placement, size,
  existing tattoo info, preferred artist; Stripe 10-20% deposit to hold the slot;
  auto-confirm email with aftercare PDF; from $350
- Flash sale / available designs: filterable grid of ready-to-book flash; "Claim this
  design" form; drives quick bookings in quieter periods
- Walk-in availability indicator: editable banner or WP Notification Bar; low effort
  high footfall impact
- Aftercare & FAQ page: illustrated; reduces support queries; signals professionalism
- Minimum price policy prominently stated (£80 minimum)
- Age verification: 18+ notice in footer and booking form; under-18 policy if applicable
- From $500 solo / $1,000+ multi-artist with flash shop + deposit booking
- Closes: "how many artists? online deposit booking priority?"

### 0d-pre31-b) Veterinary practice / pet clinic / animal hospital
Keywords: vet website, veterinary website, vets website, animal hospital website,
pet clinic website, veterinary practice website, veterinary surgery website,
vet clinic website, exotic vet website, veterinary specialist website,
animal clinic website, pet hospital website, vet practice website,
veterinary nurse website

Note: distinct from 0d-pre26-c (pet products/ezyVet subscription auto-ship) —
this is the clinical practice website (bookings, team, services, repeat Rx).

Response:
- Online appointment booking: appointment type (new patient/vaccination/emergency/
  dental/nurse consult); species selector (dogs/cats/rabbits/exotics); preferred vet;
  Calendly or custom WP + ezyVet/Animana integration; from $350
- Species and service pages: one per species/service type; "rabbit vet [town]" keyword
  clusters; from $400
- Team profiles: RCVS credentials + CHTs + clinical interests; builds retention
- Repeat prescription request: patient name + medication + quantity; reduces phone
  calls; from $200
- Pet health articles/blog: tick season, heat, fireworks anxiety, dental month; SEO
- Emergency OOH info: 24/7 number or named OOH provider above the fold (critical for
  new clients in distress)
- RCVS Practice Standards logo and scheme tier displayed; immediate trust signal
- From $500 small practice / $1,100+ multi-branch with booking + Rx + species SEO
- Closes: "single or multi-branch? exotics or mainly dogs and cats?"

### 0d-pre31-c) Coworking space / serviced offices / hot desking / flexible workspace
Keywords: coworking space website, coworking website, serviced office website,
hot desking website, flexible workspace website, shared office website,
coworking membership website, virtual office website, business hub website,
innovation hub website, startup hub website, managed office website,
office rental website, desk rental website

Response:
- Membership tier pages: Hot desk / Dedicated desk / Private office / Enterprise suite;
  per-day + monthly + annual pricing table; amenities checklist; from $400
- Space tour / booking: Calendly for in-person tour OR Matterport 360° virtual tour
  embed; showing up is highest-converting action; from $250
- Meeting room booking: WP Amelia or Checkfront; hourly hire for non-members; capacity
  + A/V specs; Stripe at booking; from $400
- Member portal: book desks + rooms; view invoices; community Slack link; door code
  delivery; from $600
- Community page: member spotlight grid; events calendar; member blog; drives referrals
- Virtual office packages: registered address + mail handling + call forwarding +
  meeting credits; from $200 add-on page
- Location SEO: "coworking space [city]" + "serviced office [city]"; Google Business;
  Local Business + Event schema
- From $500 brochure with tour booking / $1,200+ with member portal + room booking
- Closes: "single location or multi-site network? per-visit or monthly plans?"

## QA results (28/28 all pass)
| Check | Result |
|-------|--------|
| _unreadCount var declared | OK |
| badge uses _unreadCount not _cnt | OK |
| aria-label on badge creation | OK |
| aria-label updates dynamically | OK |
| toggle --vis uses _unreadCount | OK |
| addMsg increments when hidden | OK |
| openBrain resets _unreadCount | OK |
| pb-orb__badge CSS | OK |
| position:absolute top:-4px right:-4px | OK |
| background:#e53935 (red) | OK |
| opacity:0 transform:scale(.6) default | OK |
| --vis opacity:1 scale(1) | OK |
| tattoo keywords | OK |
| flash sale / available designs | OK |
| walk-in availability indicator | OK |
| Stripe deposit 10-20% | OK |
| from $500 solo tattoo artist | OK |
| vet keywords | OK |
| species selector (dogs, cats, rabbits) | OK |
| repeat prescription request form | OK |
| RCVS Practice Standards | OK |
| emergency OOH 24/7 above the fold | OK |
| from $500 small vet practice | OK |
| coworking keywords | OK |
| Matterport 360° virtual tour | OK |
| WP Amelia or Checkfront | OK |
| virtual office packages / registered address | OK |
| from $500 brochure with tour booking | OK |
