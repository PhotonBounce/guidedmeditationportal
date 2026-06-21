# DOM QA Report — R29 — 2026-06-20

## main.js — Message bookmarking (star button)

Star (★) button appended to every bot message. Hidden by default; revealed on
message hover. Gold when starred. State persisted to `sessionStorage('pb_stars_v1')`.

```javascript
var _starBtn = document.createElement('button');
_starBtn.type = 'button'; _starBtn.className = 'pb-brain__star';
_starBtn.title = 'Bookmark this reply';
_starBtn.setAttribute('aria-label', 'Bookmark reply');
_starBtn.setAttribute('aria-pressed', 'false');
_starBtn.innerHTML = '&#9733;'; // ★

_starBtn.addEventListener('click', function() {
  var _on = div.classList.toggle('pb-brain__msg--starred');
  _starBtn.setAttribute('aria-pressed', String(_on));
  _starBtn.classList.toggle('pb-brain__star--on', _on);  // second arg = force value
  try {
    var _stars = JSON.parse(sessionStorage.getItem('pb_stars_v1') || '[]');
    var _key = plain(text).slice(0, 80);  // first 80 chars of plaintext as key
    if (_on)  { if (_stars.indexOf(_key) < 0) _stars.push(_key); }
    else      { _stars = _stars.filter(function(s) { return s !== _key; }); }
    sessionStorage.setItem('pb_stars_v1', JSON.stringify(_stars));
  } catch(e) {}
});
div.appendChild(_starBtn);
```

Behavior:
- Appended after the thumbs `_fbDiv`, before the timestamp — inside the `if (cls==='bot')` block
- `classList.toggle('pb-brain__msg--starred')` returns the new state (`true`=starred)
- `classList.toggle('pb-brain__star--on', _on)` — second arg forces state (no flicker)
- `aria-pressed` updated for screen readers on each toggle
- sessionStorage key: first 80 chars of the message's plaintext — unique enough per session
- Messages starred stay gold even after `_stars` sessionStorage write fails (try/catch)
- Does NOT restore star state on page refresh (sessionStorage used for reference only;
  restoring star visuals on load would require matching text back to DOM elements — future work)

## main.css — .pb-brain__star

```css
.pb-brain__star {
  background:none; border:none; cursor:pointer; font-size:13px;
  color:rgba(255,255,255,.22); padding:2px 4px; line-height:1;
  opacity:0; transition:opacity .15s, color .2s; vertical-align:middle;
}
.pb-brain__msg:hover .pb-brain__star { opacity:1; }
.pb-brain__star:hover                { color:rgba(255,212,0,.75); }
.pb-brain__star--on                  { color:#ffd400 !important; opacity:1 !important; }
.pb-brain__msg--starred              { border-left:2px solid rgba(255,212,0,.6); }
```

Notes:
- Star is invisible (opacity:0) until the user hovers the message — same pattern as cpBtn
- On hover pre-glow: rgba(255,212,0,.75) — same gold palette as copy button and new-msg chip
- `--on` uses `!important` to win over the hover state opacity
- `--starred` border-left uses `rgba(255,212,0,.6)` (60% opacity gold) — matches brand accent
  at reduced opacity so it reads as an accent, not an error

QA note: 2 regex patterns gave false-negatives:
- Pattern for `classList.toggle('pb-brain__star--on')` didn't account for the second
  boolean argument (`, _on`); manually verified the code at line 553 is correct.
- Pattern checked for `#ffd400` in `--starred` but the CSS uses `rgba(255,212,0,.6)`;
  manually verified that the border-left line exists at CSS line 2925, correct value.

## brainstorm.php — 3 new intent handlers

### 0d-pre25-a) Beauty salon / spa / hair / nail / aesthetics studio
Keywords: beauty salon, hair salon, nail salon, spa website, beauty website,
aesthetics website, barbershop website, salon website, lash studio, brow studio,
beauty studio, makeup artist website, tattoo studio website, massage therapist website

Response:
- Online booking: service + provider + date/time + Stripe; Fresha or Vagaro embed
  (free plan); or custom from $350; SMS + email reminders cut no-shows ~30%
- Service menu: categorised (hair/nails/skincare), duration + price; staff-editable; $300
- Team / stylist pages: photo, bio, specialities, Instagram embed, direct-book button
- Gallery: before/after slider, filterable, WebP lazy-load, Smash Balloon Instagram feed
- Gift vouchers: WooCommerce gift cards; purchasable online, redeemable at checkout; $200
- Local SEO: "[treatment] near me" + "[treatment] [town]" keywords; Reviews widget; schema
- From $450 solo / $900+ multi-stylist with booking + vouchers
- Closes: "team size and do you use Fresha or Vagaro already?"

### 0d-pre25-b) Music / band / artist / DJ / musician website
Keywords: music website, band website, musician website, dj website,
artist website music, album website, tour dates, gig listings, music streaming,
soundcloud, spotify artist, music portfolio, record label website, singer website

Response:
- Hero + embedded player: Spotify/Apple Music/SoundCloud embed + WaveSurfer.js; $350
- Tour dates: Bandsintown or Songkick widget (auto-updating) or custom CPT + Eventbrite; $200
- Discography: album artwork, track listings, streaming links (Spotify, Apple, YouTube, Deezer)
- Merch store: WooCommerce + Printful (apparel, vinyl, posters); no upfront stock; $400
- EPK (press kit): password-protected; bio, hi-res photos, logo, rider, press clippings
- Mailing list: Mailchimp/Klaviyo signup + lead-magnet (free download)
- From $500 EPK/bio site / $1,000+ full stack
- Closes: "solo / band / label? Priority: booking gigs / selling music / growing fans?"

### 0d-pre25-c) Travel / tourism / tour operator / activity booking site
Keywords: travel website, tourism website, tour operator, activity booking,
travel agency, holiday website, tour booking, travel blog, excursion website,
adventure tourism, eco tourism, safari website, boat charter, travel booking site

Response:
- Tour listings CPT: overview, itinerary accordion, included/excluded, difficulty,
  gallery, pricing tiers; staff-editable; from $450
- Online booking: date picker + group size + Stripe or PayPal; deposit or full pay; $400
- FareHarbor / Bokun / Rezdy integration: if already on OTA channels (Viator,
  GetYourGuide), embed widget — keeps availability in sync automatically
- Destination pages: per destination; local tips, map, weather, testimonials;
  "[activity] in [destination]" keyword targeting; strong SEO for tour operators
- TripAdvisor / Google Reviews live pull: trust signals essential for travel
- Travel blog / itinerary content: long-form SEO for top-of-funnel organic traffic
- Multi-currency + multilingual: WooCommerce currency switcher + WPML; $350
- From $650 (5 tours) / $1,500+ with channel-manager integration
- Closes: "number of tours and do you use FareHarbor / Bokun / Rezdy?"

## QA results (25/27 pattern-verified + 2 manually confirmed = 27/27 correct)
| Check | Result |
|-------|--------|
| star button created | OK |
| aria-pressed attribute | OK |
| ★ innerHTML | OK |
| toggles --starred class | OK |
| toggles --on class (manual verify) | OK — 2nd-arg syntax caused regex miss |
| sessionStorage pb_stars_v1 | OK |
| 80-char key | OK |
| filter on unstar | OK |
| pb-brain__star CSS | OK |
| opacity:0 default | OK |
| revealed on hover | OK |
| gold --on colour | OK |
| --starred border-left (manual verify) | OK — rgba() not #ffd400 caused regex miss |
| beauty salon keywords | OK |
| Fresha or Vagaro | OK |
| WooCommerce gift cards | OK |
| from $450 solo stylist | OK |
| music keywords | OK |
| WaveSurfer.js | OK |
| Bandsintown or Songkick | OK |
| EPK / press kit | OK |
| WooCommerce + Printful | OK |
| travel keywords | OK |
| FareHarbor / Bokun / Rezdy | OK |
| Viator / GetYourGuide | OK |
| destination pages | OK |
| from $650 travel site | OK |
