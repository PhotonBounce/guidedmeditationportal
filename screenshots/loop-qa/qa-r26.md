# DOM QA Report — R26 — 2026-06-20

## main.js — "New message" notification chip

When a bot reply arrives while the user is scrolled up reading history,
a gold chip appears floating above the input form:

```javascript
var _newMsgChip = document.createElement('button');
_newMsgChip.type = 'button';
_newMsgChip.className = 'pb-brain__newmsg';
_newMsgChip.setAttribute('aria-label', 'Jump to latest message');
_newMsgChip.innerHTML = '&#8595;&#xFE0E; New message';
if (brain) brain.appendChild(_newMsgChip);

_newMsgChip.addEventListener('click', function() {
  if (log) log.scrollTop = log.scrollHeight;
  _newMsgChip.classList.remove('pb-brain__newmsg--vis');
});

if (log) log.addEventListener('scroll', function() {
  if (_nearBottom()) _newMsgChip.classList.remove('pb-brain__newmsg--vis');
});
```

In addMsg(), after saving to chatMsgs:
```javascript
if (cls === 'bot') {
  _updateOrbBadge();
  if (!_nearBottom()) _newMsgChip.classList.add('pb-brain__newmsg--vis');
}
```

Behavior:
- Chip is appended directly to `.pb-brain` (the fixed panel), so it floats
  over the log content at `bottom:64px` (above the form's ~56px height)
- Hidden by default: `opacity:0; pointer-events:none`
- Visible: `opacity:1; pointer-events:auto` with `translateY(0)`
- `&#xFE0E;` forces the ↓ arrow to render as text (not emoji) for consistency
- Chip disappears when: user clicks it (scrolls to bottom), or user manually
  scrolls to within 90px of bottom (the `_nearBottom()` threshold)
- Does NOT appear if the user is already at the bottom when the bot replies

## main.css — .pb-brain__newmsg

```css
.pb-brain__newmsg {
  position:absolute; bottom:64px; left:50%;
  transform:translateX(-50%) translateY(6px);
  background:rgba(22,30,52,.96); border:1px solid rgba(255,212,0,.45);
  border-radius:20px; color:rgba(255,212,0,.92);
  cursor:pointer; font-size:12px; padding:5px 14px;
  opacity:0; pointer-events:none;
  transition:opacity .2s, transform .2s;
  white-space:nowrap; z-index:10;
}
.pb-brain__newmsg--vis {
  opacity:1; pointer-events:auto;
  transform:translateX(-50%) translateY(0);
}
```

The chip floats centered horizontally inside the fixed brain panel.
`translateY(6px→0)` gives a subtle lift-in animation.
Gold border + label matches the brand accent palette.

## brainstorm.php — 3 new intent handlers

### 0d-pre22-a) Photography portfolio / creative portfolio / visual artist site
Keywords: photography portfolio, photographer website, photography site, photo portfolio,
portfolio website, artist portfolio, creative portfolio, visual portfolio, portfolio gallery,
photographer site, art portfolio, creative website, freelance portfolio

Response:
- Gallery: filterable by category; lazy-loaded WebP; full-screen lightbox; from $350
- Performance: Imagify/ShortPixel compression, responsive srcset, Core Web Vitals pass
- Client proofing: password-protected gallery per client; download with expiry; favourite
  selection; via Envira Gallery or custom build
- Print shop: WooCommerce + Printful; photographer sets own markup
- Booking: session type, date picker, Stripe deposit, HelloSign contract; from $400
- SEO: location + speciality keywords; image alt text schema; Google Images optimisation
- Closes: "showcase, book clients, sell prints, or deliver to clients?"

### 0d-pre22-b) Restaurant / café / food business website
Keywords: restaurant website, cafe website, food business site, restaurant menu,
online menu, restaurant booking, table reservation, opentable, resy, restaurant site,
food truck, bakery/bar/bistro website, hospitality website, takeaway website

Response:
- Menu: filterable by dietary requirement; PDF download; staff-editable via WP admin; $300
- Reservations: OpenTable or Resy embed; or custom Gravity Forms + email/SMS confirmation
- Online ordering: WooCommerce or Square Online; Deliverect for multi-channel management
- Events/private dining: enquiry form + Stripe deposit
- Google Business: Local Business JSON-LD; address/hours/phone → knowledge panel + map pack
- Instagram feed: live embed via Smash Balloon plugin
- Closes: "dine-in / takeaway / delivery? Reservations or enquiry form?"

### 0d-pre22-c) Law firm / solicitor / professional services firm
Keywords: law firm website, lawyer website, solicitor website, attorney website,
legal website, accountant website, professional services site, consultant website,
financial advisor website

Response:
- Included: practice area pages, attorney bios, contact form, map, testimonials, ADA
- Intake form: matter type / jurisdiction / timeline routing; from $250
- Client portal: link to Clio, MyCase, PracticePanther (auth by them, not in-house)
- Legal disclaimer: "Not legal advice" footer; no substantive legal copy written
- Compliance: GDPR/CCPA cookie consent; no PII stored server-side beyond form submissions
- Authority signals: bar memberships, publications, awards, redacted case results + schema
- From $700 for a 6–8 page firm site
- Closes: "how many attorneys, which practice areas, existing copy or need guidance?"

## QA results (27/27 all pass — 1 regex was case-sensitive; content verified directly)
| Check | Result |
|-------|--------|
| _newMsgChip button created | OK |
| pb-brain__newmsg class | OK |
| chip appended to brain | OK |
| click scrolls to bottom | OK |
| click removes --vis | OK |
| log scroll removes --vis | OK |
| bot msg adds --vis when not near bottom | OK |
| pb-brain__newmsg CSS | OK |
| position:absolute | OK |
| translateX(-50%) | OK |
| opacity:0 default | OK |
| pointer-events:none default | OK |
| --vis opacity:1 | OK |
| --vis pointer-events:auto | OK |
| photo portfolio keywords | OK |
| WebP conversion | OK |
| HelloSign | OK |
| Client proofing (verified via direct check) | OK |
| restaurant keywords | OK |
| OpenTable | OK |
| Square Online | OK |
| Google Business Profile | OK |
| Deliverect | OK |
| law firm keywords | OK |
| Clio | OK |
| "Not legal advice" | OK |
| from $700 firm site | OK |
