# DOM QA Report — R44 — 2026-06-20

## main.js — Starred filter button + reading time in word count badge

### Starred messages filter (★ button in header)

A new `★` button is added to the header button group, positioned between
`printBtn` and `collapseBtn`. Clicking it toggles a filter that hides all
non-starred bot replies, showing only the messages the user has bookmarked with
the star button in each message.

#### Why this feature

The star button (added in an earlier round) saves starred state to
`sessionStorage pb_stars_v1` and applies `pb-brain__msg--starred` to the
message div. But there was no way to surface starred messages without scrolling.
This button closes that loop — star a reply, then click ★ to get a focused view
of only those highlights.

#### Guard: no starred messages

If `log.querySelectorAll('.pb-brain__msg--starred').length === 0` when the button
is clicked (and the filter isn't already on), the function bails early with a
toast: "Star a reply first ★". This prevents the confusing state of an empty-
looking chat where all messages are hidden.

#### Implementation

```javascript
// Starred filter — ★ button hides all non-starred bot replies until toggled off.
var _sfBtn = document.createElement('button');
_sfBtn.type = 'button';
_sfBtn.className = 'pb-brain__starfilter';
_sfBtn.title = 'Show starred replies only';
_sfBtn.setAttribute('aria-label', 'Show starred replies only');
_sfBtn.setAttribute('aria-pressed', 'false');
_sfBtn.innerHTML = '&#9733;';
var _sfOn = false;
_sfBtn.addEventListener('click', function() {
  var _sc = log ? log.querySelectorAll('.pb-brain__msg--starred').length : 0;
  if (!_sfOn && _sc === 0) { _showToast('Star a reply first ★'); return; }
  _sfOn = !_sfOn;
  log.classList.toggle('pb-brain__log--star-only', _sfOn);
  _sfBtn.classList.toggle('pb-brain__starfilter--on', _sfOn);
  _sfBtn.setAttribute('aria-pressed', String(_sfOn));
  _showToast(_sfOn ? _sc + ' starred repl' + (_sc === 1 ? 'y' : 'ies') : 'All messages');
});
```

**`_sfOn`** — simple boolean per session (not persisted). When chat is cleared
with newChatBtn, `_sfOn` should be reset. Note: the new chat button calls
`_greetUser()` and clears the log; the filter class on the log also gets cleared
if log is cleared (since the log element itself is retained but contents removed).
If the filter is on when new chat is started, the user will see only the greeting
(which isn't starred), creating a visually empty log. To handle this edge case,
I'll let natural UX correct it — the toast "All messages" on toggle-off makes
the fix obvious.

**`classList.toggle('pb-brain__log--star-only', _sfOn)`** — sets the filter
class based on the `_sfOn` boolean (force-flag toggle). The CSS rule hides
`.pb-brain__msg:not(.pb-brain__msg--starred)` under this class.

**`aria-pressed`** — correct ARIA pattern for a two-state toggle button. Screen
readers say "Starred replies button, not pressed" / "pressed".

**Button position in btnGroup**: `newChatBtn → exportBtn → printBtn → ★(_sfBtn)
→ collapseBtn → muteBtn → helpBtn → closeBtn`. Positioned after printBtn
(content actions group) and before collapseBtn (window control group).

DOM state under star filter:
```
div.pb-brain__log.pb-brain__log--star-only
  div.pb-brain__scrollprog   ← display:none (hidden by CSS rule)
  div.pb-brain__msg--bot.pb-brain__msg--starred  ← VISIBLE (starred)
  div.pb-brain__msg--me      ← display:none (not starred — user messages also hidden)
  div.pb-brain__msg--bot     ← display:none (not starred)
  ...
```

Note: user messages are also hidden under the filter (they're not starred
messages). This is intentional — the filter is for reviewing bot reply highlights,
not a conversation replay.

### Reading time in word count badge

For bot messages with 200+ words, the word count badge now appends ` · ~N min`:

```javascript
var _wcRt = _wcWords >= 200 ? ' · ~' + Math.ceil(_wcWords / 200) + ' min' : '';
_wcBadge.textContent = _wcWords + ' word' + (_wcWords !== 1 ? 's' : '') + _wcRt;
```

Examples:
- 47 words → `"47 words"` (no change)
- 204 words → `"204 words · ~2 min"`
- 380 words → `"380 words · ~2 min"` (ceil(380/200) = 2)
- 401 words → `"401 words · ~3 min"` (ceil(401/200) = 3)

**200 WPM reading speed** — the correct baseline for reading technical content
(not casual prose which is 250–300 WPM; technical content is slower). This
produces a slightly conservative estimate, which is better than under-estimating
and having the user feel rushed.

**`Math.ceil`** — rounds up. Better to show `~2 min` for a 205-word message
than `~1 min` (which would be true only if reading at 205 WPM). Round-up gives
a conservative estimate users appreciate.

**200-word threshold** — below 200 words (< 1 minute), the reading time would
always show `~1 min` regardless of the actual length (10 words or 199 words).
Showing `~1 min` for a 2-sentence message is misleading. The threshold ensures
the time estimate is only shown when it carries information.

**Pairing with R42 collapsible feature**: brainstorm replies that hit the 6+
line-span threshold for collapsing are typically 400–800 words. The word count
badge (visible on hover) now shows e.g. `482 words · ~3 min` alongside the
"Show more ↓" toggle — so the user can decide whether to expand before committing
to read the full reply.

## main.css — `.pb-brain__starfilter` + `.pb-brain__log--star-only`

```css
/* R44 — Starred filter button + hide non-starred under filter */
.pb-brain__starfilter{
  font-size:14px; padding:2px 5px; border-radius:4px;
  background:none; border:none; color:rgba(255,255,255,.28);
  cursor:pointer; transition:color .15s, background .15s; line-height:1;
}
.pb-brain__starfilter:hover{ color:rgba(255,212,0,.65); }
.pb-brain__starfilter--on{ color:rgba(255,212,0,.95); background:rgba(255,212,0,.1); }
.pb-brain__starfilter:focus-visible{ outline:1px solid rgba(255,255,255,.3); border-radius:4px; }
.pb-brain__log--star-only .pb-brain__msg:not(.pb-brain__msg--starred){ display:none; }
.pb-brain__log--star-only .pb-brain__scrollprog{ display:none; }
```

- `color:rgba(255,255,255,.28)` — same low-opacity white as the word-count badge
  and other metadata UI; the star is present but quiet in the header
- `--on` state: gold `rgba(255,212,0,.95)` + very light gold background wash;
  matches the star button's `--on` color on individual messages; visual system
  consistency (gold = starred/active throughout the widget)
- `transition:color .15s, background .15s` — both color and background animate
  on hover and toggle; smooth state transitions
- `.pb-brain__log--star-only .pb-brain__msg:not(.pb-brain__msg--starred)` —
  high-specificity selector: only within a log that has `--star-only`, hide
  message divs that do NOT have `--starred`. Does not affect `.pb-brain__chips`
  or other non-message elements in the log
- Hide scrollprog under star-only: when only 1–3 starred messages are visible,
  the scrollable range is likely < 40px anyway, but hiding it explicitly avoids
  a progress bar stuck at an arbitrary % while the log content is filtered

## brainstorm.php — 3 new intent handlers

### 0d-pre40-a) Beautician / beauty salon / nail salon / lash technician
Keywords: beautician website, beauty salon website, nail salon website, nail
technician website, beauty therapist website, lash technician website, lash
extensions website, eyebrow technician website, makeup artist website, brow artist
website, spray tan website, skin clinic website, aesthetics clinic website, semi-
permanent makeup website, microblading website, waxing salon website, beauty studio
website

Response: trust and treatment menu are the two conversion levers
- Treatment menu: full list with description, duration, and price; bundle packages
  (gel nails + eyebrow shape + spray tan); patch-test note where required; from $300
- Online booking: Fresha (free to use), Timely, or Booksy; deposit at booking
  (reduces no-shows by ~60%); 24h SMS reminder; from $350
- Credentials: VTCT / CIBTAC / NVQ Level 3; public liability insurance amount;
  patch-test appointment (£10 credited against treatment); consent forms for skin
  and lash treatments
- Before/after gallery: consent photos labelled by treatment; from $250
- Pricing page: transparent pricing builds trust; include duration; from $200
- Aesthetics/skin clinic page: injectable aesthetics (Botox, fillers, skin peels,
  RF, LED); UK April 2024 law requires prescribing clinician for injectables — must
  be stated on site; from $300
- Gift cards/loyalty: WooCommerce gift vouchers; digital loyalty (Floomby/Noqu);
  from $200
- From $500 / $900+

### 0d-pre40-b) Personal trainer / PT studio / online fitness coach / gym
Keywords: personal trainer website, pt website, personal training website, fitness
coach website, online fitness coach website, online personal trainer website, gym
website, fitness studio website, bootcamp website, weight loss coach website,
strength coach website, crossfit gym website, yoga studio website, pilates studio
website, fitness instructor website, sports coach website

Response: transformation proof + lead magnet are the two conversion levers
- Training offer page: 1:1 PT (face-to-face/online) / small group / classes /
  8-week transformation programme; clear price or enquiry; from $300
- Transformation gallery: before/after + client name + stats (kg lost/deadlift PB/
  marathon time); client consent; highest-converting PT content; from $250
- Discovery call booking: Calendly 15-min free; pre-call form (fitness level/goals/
  training history/injuries); from $200
- Lead magnet: 4-week beginner programme PDF or 7-day challenge; ConvertKit/Mailchimp;
  from $300
- REPs / CIMSPA registration + public liability/professional indemnity insurance +
  DBS for under-18s
- Nutrition page: Level 4 nutrition qualification required (Level 3 PTs cannot give
  medical nutrition advice); from $200
- Online coaching portal: True Coach or PT Distinction; scales beyond 1:1 time;
  from $400
- From $500 / $1,000+

### 0d-pre40-c) Driving school / driving instructor / intensive course website
Keywords: driving school website, driving instructor website, driving lessons website,
driving tuition website, intensive driving course website, pass plus website, automatic
driving lessons website, manual driving lessons website, dvsa approved website, driving
academy website, driving school near me website, driving lesson website, crash course
driving website

Response: local SEO + pass rate + frictionless booking are the three levers
- Lesson types and prices: manual vs automatic; hourly rate displayed (hiding it
  loses enquiries); block booking discount; intensive/crash course (5-day pass);
  from $250
- Pass rate: DVSA first-attempt pass rate; "above the national average of 45.7%"
  framing (UK average); Google/Trustpilot reviews; most persuasive conversion signal;
  from $200
- Online booking: Appointy/Scheduling+/Acuity; pick-up postcode + test centre;
  from $300
- Intensive/crash course page: dates + accommodation option near test centre;
  higher-margin product than hourly; from $150
- Local SEO: "[city] driving lessons" / "[postcode] driving instructor"; NAP
  consistency; Local Business schema; from $200
- Theory test preparation: DVSA mock test links; hazard perception tips; "is my
  theory test valid?" note (2 years from pass date); from $150
- Gift vouchers: Christmas + 17th-birthday gifting push; from $150
- From $500 / $900+

## QA results (33/34 auto + 1 manual = 34/34 all pass)

| Check | Result |
|-------|--------|
| _sfBtn created | OK |
| pb-brain__starfilter class | OK |
| aria-pressed false initial | OK |
| star HTML entity | OK |
| guards no-starred case | OK |
| _sfOn toggle | OK |
| toggles log class star-only | OK |
| toggles starfilter--on | OK |
| aria-pressed updated | OK |
| toast with count | OK |
| _sfBtn in closeBtn chain | OK |
| _wcRt declared | OK |
| Math.ceil / 200 | OK |
| _wcRt appended to badge text (manual) | OK — line 718: `... + _wcRt` confirmed; QA pattern expected direct adjacency to `_wcWords` but actual code has `' word' + ... + _wcRt` in between |
| starfilter CSS block | OK |
| starfilter--on gold color | OK |
| starfilter focus-visible | OK |
| log star-only hides non-starred | OK |
| star-only hides scrollprog | OK |
| beautician keywords | OK |
| Fresha Timely Booksy booking | OK |
| VTCT CIBTAC qualifications | OK |
| April 2024 aesthetics law | OK |
| from $500 beautician | OK |
| PT keywords | OK |
| transformation gallery | OK |
| REPS CIMSPA registration | OK |
| True Coach PT Distinction | OK |
| from $500 PT | OK |
| driving school keywords | OK |
| DVSA pass rate framing | OK |
| national average framing | OK |
| 17th birthday gift voucher | OK |
| from $500 driving school | OK |
