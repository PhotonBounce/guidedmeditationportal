# DOM QA Report — R37 — 2026-06-20

## main.js — Search match count below the Ctrl+F bar

The existing search bar (Ctrl+F) already dims non-matching messages to 15% opacity
but gave no indication of how many results matched. R37 adds a `span.pb-brain__search-count`
immediately below the search input, showing "N match" / "N matches" / "No results"
while a query is active.

### `_srchCount` element setup

```javascript
var _srchCount = document.createElement('span');
_srchCount.className = 'pb-brain__search-count';
_srchCount.setAttribute('aria-live', 'polite');
_srchCount.setAttribute('aria-atomic', 'true');
_srchCount.style.display = 'none';
if (log && log.parentNode) log.parentNode.insertBefore(_srchCount, log);
```

DOM order between the chat header and the message log:
1. `_srchInput` (already present, inserted before log in a previous round)
2. `_srchCount` (new — inserted before log, appears immediately after srchInput)
3. `log` (the message list)

`aria-live="polite"` + `aria-atomic="true"` — screen readers announce the count
after each keystroke pause; `atomic` means the entire count is re-read as one unit
rather than announcing just the changed character.

### `_srchFilter()` updated

```javascript
function _srchFilter() {
  var q = _srchInput.value.toLowerCase().trim();
  var msgs = log ? log.querySelectorAll('.pb-brain__msg') : [];
  var _n = 0;
  [].forEach.call(msgs, function(m) {
    var _hit = !q || m.textContent.toLowerCase().indexOf(q) !== -1;
    m.style.opacity = _hit ? '' : '0.15';
    if (q && _hit) _n++;
  });
  _srchCount.textContent = q ? (_n === 0 ? 'No results' : _n + ' match' + (_n !== 1 ? 'es' : '')) : '';
  _srchCount.style.display = q ? '' : 'none';
}
```

Changes from the previous version:
- Added `var _n = 0` counter
- `_hit` variable extracted (previously the ternary was inline in `style.opacity`)
- `if (q && _hit) _n++` — only counts when `q` is non-empty AND the message matches
  (not counting non-matching dim messages; not counting all messages when no query)
- Correct pluralisation: "1 match" / "2 matches" / "No results"
- `_srchCount.style.display = q ? '' : 'none'` — hidden when no query active
  (`''` restores the block display from CSS; not `'block'` so the display value
  from the stylesheet is respected)

`_srchClose()` unchanged — its `_srchInput.value = ''; _srchFilter();` already
triggers the count to clear and `display:none` to fire through `_srchFilter()`.

## main.css — `.pb-brain__search-count`

```css
.pb-brain__search-count {
  font-size:10px; color:rgba(255,255,255,.38);
  padding:2px 12px 4px; text-align:right;
  background:rgba(0,0,0,.18);
  border-bottom:1px solid rgba(255,255,255,.07);
  user-select:none;
}
```

- `text-align:right` — count sits in the top-right; reads as a metadata annotation
  not a main content element
- `rgba(255,255,255,.38)` — subtle; not competing with message content
- `rgba(0,0,0,.18)` background — very slight dark wash to visually group it with the
  search input above (same section)
- `border-bottom` matches the search input's `border-bottom` visual rhythm
- `user-select:none` — selecting "3 matches" would be confusing; it's not content
- No transition — this is a display:none toggle, not an opacity fade

## brainstorm.php — 3 new intent handlers

### 0d-pre33-a) Art gallery / commercial gallery / museum / cultural venue
Keywords: art gallery website, commercial gallery website, museum website, gallery
website, contemporary art website, fine art gallery website, art dealer website,
art consultant website, exhibition website, cultural venue website, sculpture gallery,
print gallery website, art fair website, public gallery website, photography gallery

Response:
- Artist and collection CPTs: one per artist (bio/CV/selected shows) + one per work
  (medium/dimensions/year/edition/price on enquiry vs listed); filterable by medium,
  year, artist; from $500
- Exhibition archive: past/current/upcoming; opening date + private view time; press
  release PDF; install shots gallery; from $300
- Enquiry and purchase flow: "Enquire about this work" form per artwork; price on
  request vs listed price toggle; collector enquiry → gallery director; from $250
- Online viewing room: private OVR for art fair week or collector previews; password-
  protected with full artwork grid + zoom + enquiry; from $350
- Art fair presence: landing page per fair (Frieze/Art Basel/TEFAF/The Armory); booth
  number + participating artists + install preview; SEO around fair week
- Press and publications: reviews, catalogue PDFs, editorial coverage; institutional
  credibility signals to collectors and curators
- Newsletter / collector list: Mailchimp or FluentCRM; segmented by interest
  (photography/painting/sculpture); private view invitations; $200 set-up
- From $600 with artist and exhibition CPTs / $1,200+ with OVR + fair pages + segmentation
- Closes: "commercial gallery / public museum / artist studio site?"

### 0d-pre33-b) Brewery / craft beer / distillery / cidery / winery
Keywords: brewery website, craft brewery website, microbrewery website, craft beer
website, distillery website, craft distillery website, gin distillery website, whisky
distillery website, rum distillery website, vodka distillery website, winery website,
vineyard website, cidery website, meadery website, taproom website, tasting room website

Response:
- WooCommerce product range: ABV + volume + tasting notes + food pairing + allergen
  info; case discounts; filterable by type; from $500
- Age gate: full-screen overlay, date-of-birth entry or 18+/21+ click; session
  cookie consent; from $150
- Taproom / tours: booking with capacity + Stripe deposit; from $300
- Trade enquiry: on-trade (pubs/restaurants/bars) and off-trade (retailers/wholesalers)
  forms; min order + delivery area + price list PDF; from $200
- Story and process page: founders, equipment photography, grain-to-glass narrative;
  key differentiator vs large producers; essential for premium positioning
- Stockist locator: Google Map + searchable list; from $250
- Subscription / mixed case club: WooCommerce Subscriptions; monthly or quarterly;
  early access for members; from $350
- Compliance: Drinkaware (UK) / Drinkwise (Australia) logos; responsible drinking
  disclaimer; no under-18 targeting
- From $550 product range + age gate / $1,200+ taproom + trade + subscription club
- Closes: "primary revenue: DTC online / trade wholesale / cellar-door visitors?"

### 0d-pre33-c) Driving school / driving instructor / DVSA / intensive courses
Keywords: driving school website, driving instructor website, driver training website,
driving lessons website, driving tuition website, intensive driving course website,
pass plus website, advanced driving website, fleet driver training website, fleet
training website, automatic driving lessons website, motorway lessons website,
dvsa website, driving test website, young driver website

Response:
- Online lesson booking: lesson type (manual/automatic/intensive/Pass Plus/motorway);
  duration tiers; pick-up area by postcode; preferred instructor (multi-instructor);
  Stripe upfront or card-on-file; from $350
- Instructor profiles: photo + DVSA ADI badge number + experience + areas covered +
  availability indicator; confidence signal for anxious learners and parents
- Intensive course packages: page per package (20h/30h/40h crash course); estimated
  pass timeline + what's included + test booking guidance; Stripe single payment; $250
- Theory test prep: DVSA hazard perception links or embed; blog posts by topic
- Pass rates and reviews: first-time pass rate prominently displayed; Google Reviews
  widget; Trustpilot badge; highest-converting trust signal
- Gift vouchers: WooCommerce gift card plugin; email delivery; from $200
- Area SEO pages: one per town or postcode; Local Business schema; from $80 per page
- From $450 single instructor + booking + area pages / $900+ multi-instructor with
  intensive packages + gift vouchers + area SEO

## QA results (27/28 pattern + 1 manual = 28/28 correct)
| Check | Result |
|-------|--------|
| _srchCount element created | OK |
| pb-brain__search-count class | OK |
| aria-live polite | OK |
| aria-atomic true | OK |
| display:none initial | OK |
| inserted before log | OK |
| _n counter increments on hit | OK |
| count text pluralisation | OK |
| No results text | OK |
| display '' : 'none' (manual) | OK — empty string '' not ' '; regex used space-padded variant |
| pb-brain__search-count CSS | OK |
| font-size 10px | OK |
| text-align right | OK |
| art gallery keywords | OK |
| online viewing room OVR | OK |
| art fair Frieze / Basel / TEFAF | OK |
| collector newsletter segmented | OK |
| from $600 gallery | OK |
| brewery keywords | OK |
| age gate date-of-birth | OK |
| Drinkaware / Drinkwise | OK |
| subscription mixed case club | OK |
| from $550 brewery | OK |
| driving school keywords | OK |
| DVSA ADI badge number | OK |
| intensive course packages 20h/30h/40h | OK |
| area SEO pages per town/postcode | OK |
| from $450 single instructor | OK |
