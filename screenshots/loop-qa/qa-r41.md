# DOM QA Report — R41 — 2026-06-20

## main.js — Character count / limit indicator

Shows a live "N left" counter below the input field once the user has typed more
than 100 characters. The counter turns amber inside the last 100 characters (≥ 400)
and red when over the 500-character soft limit. The send button is disabled when
over limit (preventing accidental submission of very long messages).

### Implementation

```javascript
// Character count indicator — hidden below 100 chars; shows "N left" as user nears limit.
var _charLimit = 500;
var _charCount = document.createElement('span');
_charCount.className = 'pb-brain__charcount';
_charCount.setAttribute('aria-live', 'polite');
_charCount.setAttribute('aria-atomic', 'true');
_charCount.style.display = 'none';
if (form) form.appendChild(_charCount);
function _updateCharCount() {
  var n = input.value.length;
  var rem = _charLimit - n;
  if (n < 100) { _charCount.style.display = 'none'; return; }
  _charCount.style.display = '';
  _charCount.textContent = rem >= 0 ? rem + ' left' : Math.abs(rem) + ' over limit';
  _charCount.classList.toggle('pb-brain__charcount--warn', rem < 100 && rem >= 0);
  _charCount.classList.toggle('pb-brain__charcount--over', rem < 0);
  var _sb = form ? form.querySelector('[type="submit"]') : null;
  if (_sb) _sb.disabled = rem < 0;
}
input.addEventListener('input', _updateCharCount);
```

Placement: inside the `if (input)` block, appended to `form` (the same parent as
the send-hint). Runs after the send-hint setup, so `form` is already the correct
reference.

**`_charLimit = 500`** — chosen as a soft limit: most AI concierge queries are
well under 200 characters; 500 is long enough for any reasonable question while
discouraging paste-dumps of long documents.

**"hidden below 100" threshold**: the counter never appears for short messages.
Showing "498 left" when the user types "hi" is visual noise. The 100-char threshold
means normal messages (1–3 sentences) never see the counter; only longer messages
trigger it.

**`rem >= 0 ? rem + ' left' : Math.abs(rem) + ' over limit'`** — positive
remaining shows "N left"; negative remaining shows "N over limit" (e.g., "5 over
limit") rather than showing a negative number. This phrasing is clear without
being alarming — it tells users what to do (shorten the message) without a red
wall of errors.

**`--warn` class**: `rem < 100 && rem >= 0` — fires when between 400 and 500
chars. Amber color (`rgba(255,200,50,.75)`) creates a visual alert that the limit
is approaching without triggering the "danger" state.

**`--over` class**: `rem < 0` — fires when past 500 chars. Red + bold
(`rgba(255,90,90,.95)`, `font-weight:600`). The send button is also disabled via
`_sb.disabled = rem < 0` to prevent accidental over-limit submission.

**`aria-live="polite"` + `aria-atomic="true"`** — screen readers announce count
changes without interrupting ongoing speech. `polite` defers to current speech;
`atomic` reads the whole counter ("98 left") not just the changed digit.

**Send-button re-enable**: the `finally` block in the form submit handler always
re-enables the input and button, so after a send the button is correctly restored.
The counter hides automatically (input is cleared → `n === 0 < 100 → display:none`).

## main.css — `.pb-brain__charcount` / `--warn` / `--over`

```css
.pb-brain__charcount {
  display:block; font-size:9px; color:rgba(255,255,255,.28);
  text-align:right; padding:0 10px 4px;
  pointer-events:none; user-select:none; transition:color .2s;
}
.pb-brain__charcount--warn { color:rgba(255,200,50,.75); }
.pb-brain__charcount--over { color:rgba(255,90,90,.95); font-weight:600; }
```

- `font-size:9px` — same as word-count badge (R39), smallest legible size
- `text-align:right` — aligns with the word-count badge and the "right" metadata
  column at the bottom of the form
- `padding:0 10px 4px` — 10px left-right matches the input field's horizontal
  padding so text aligns visually to the send button
- `transition:color .2s` — smooth color shift from default → amber → red;
  avoids a jarring sudden change as the user types through thresholds
- `pointer-events:none; user-select:none` — counter is purely decorative metadata;
  selecting "98 left" or accidentally clicking it would confuse users

## brainstorm.php — 3 new intent handlers

### 0d-pre37-a) Music school / music lessons / instrument teacher / music tuition
Keywords: music school website, music lessons website, guitar teacher website,
piano teacher website, guitar lessons website, piano lessons website, drum lessons
website, violin lessons website, singing lessons website, voice lessons website,
music tuition website, music academy website, instrument lessons website, online
music lessons website, music teacher website, music studio website

Response:
- Instrument / lesson-type pages: one per instrument (guitar/piano/drums/violin/
  singing/bass/ukulele/saxophone/keyboard/cello); grade ladder (beginner through
  Grade 8 / diploma); in-person and online options; from $200/page
- Trial lesson booking: WP Amelia or Calendly; reduced-rate first slot (£15/30min
  is industry standard); auto-confirm with pre-lesson notes; highest lead-to-student
  converter on music sites; from $250
- Timetable and lesson booking: recurring weekly/fortnightly slots; 30/45/60-minute
  durations; teacher preference (multi-teacher); Stripe card-on-file or DD; from $400
- Teacher profiles: headshots + LRAM/LLCM/BMus/Dip ABRSM/PGCE credentials +
  performance background (orchestral/touring/recording) + DBS for under-18s
- Grade exam results: ABRSM / RSL / Trinity Distinction/Merit tally by grade; recent
  pass rate; major purchase signal for parents choosing a teacher for their child
- Online lessons page: Zoom/Teams-compatible; equipment advice; latency note (fine
  for one-to-one, not for ensemble); from $150
- From $600 instrument pages + trial booking + teacher profiles; $1,100+ with
  timetable system + online lesson page + exam results showcase

### 0d-pre37-b) Escape room / immersive experience / laser tag / axe throwing / VR
Keywords: escape room website, escape room booking website, immersive experience
website, adventure gaming website, laser tag website, axe throwing website, virtual
reality experience website, vr experience website, puzzle room website, mystery room
website, immersive gaming website, team building venue website, family entertainment
website, indoor adventure website

Response: one job — fill slots and push group bookings
- Room/experience pages: one per theme (mystery/horror/adventure/sci-fi/heist/family);
  difficulty rating; min/max players; suitability tags (corporate/birthday/date night/
  stag&hen/family); record times; from $250/room
- Live slot booking: FareHarbor / Xola / Peek Pro / Checkfront; real-time availability;
  group size selector; Stripe at booking; 24h reminder emails; from $450
- Gift vouchers: WooCommerce gift cards; email delivery; Christmas + birthday push; from $200
- Corporate and team-building page: private hire; invoice BACS payment for businesses;
  bespoke challenge design; enquiry form with date + group size + catering; from $250
- Birthday/hen/stag packages: tiered (room-only / +prosecco / +pizza+prosecco);
  promo codes; group discount (10+ = 15% off); from $200
- Record board: fastest completions per room; encourages rebooking; drives social
  sharing; cheap to build, high engagement
- Multi-location: location selector; sub-page per venue with own calendar; from $400
- From $600 rooms + booking + vouchers; $1,200+ corporate + packages + multi-location

### 0d-pre37-c) Franchise / franchisor / franchise opportunity / franchisee website
Keywords: franchise website, franchisor website, franchise opportunity website,
franchise for sale website, franchise recruitment website, franchise business website,
buy a franchise website, become a franchisee website, franchise network website,
franchisee website

Response: two distinct audiences — recruiting new franchisees vs existing franchisee local site

Franchisor (recruiting prospects):
- Franchise opportunity page: investment range (licence fee + working capital);
  territory map; earnings potential (realistic); what's included (training/support/
  territory/brand/tech stack); from $500
- Discovery day / enquiry flow: multi-step qualification form (occupation, liquid
  capital, location, motivation); discovery day booking (Calendly); pre-NDA info pack
  triggered on email; from $350
- Franchisee testimonials and case studies: real franchisee stories; revenue ranges
  if willing to share; day-in-the-life video; most persuasive asset
- Franchise awards: bfa membership (British Franchise Association); Franchise Direct
  ratings; British Franchise Awards nominations — trust signals for due diligence
- Franchisee portal: training materials, marketing assets, ops manuals, brand
  guidelines behind login; from $400
- From $800 opportunity + discovery day + testimonials; $1,500+ with territory
  map + portal + full recruitment funnel

Franchisee (individual territory site):
- Local site under brand's domain architecture (/location or subdomain); local NAP
  consistency; local testimonials and photos; service area schema; CTA routes to
  national booking or local number; from $300

## QA results (30/30 all pass)

| Check | Result |
|-------|--------|
| _charLimit 500 declared | OK |
| _charCount element created | OK |
| pb-brain__charcount class | OK |
| aria-live polite on count | OK |
| display none initial | OK |
| appended to form | OK |
| _updateCharCount function | OK |
| hides below 100 chars | OK |
| shows remaining or over | OK |
| warn class toggle | OK |
| over class toggle | OK |
| disables send when over | OK |
| input listener calls update | OK |
| charcount CSS block | OK |
| font-size 9px charcount | OK |
| warn color amber | OK |
| over color red bold | OK |
| music school keywords | OK |
| ABRSM RSL Trinity grades | OK |
| trial lesson booking | OK |
| from $600 music school | OK |
| escape room keywords | OK |
| FareHarbor Xola booking | OK |
| corporate team-building page | OK |
| from $600 escape room | OK |
| franchise keywords | OK |
| discovery day enquiry flow | OK |
| bfa membership British Franchise | OK |
| franchisee portal login | OK |
| from $800 franchisor | OK |
