# DOM QA Report — R43 — 2026-06-20

## main.js — Scroll progress strip + help panel shortcut fix

### Scroll progress strip

A 2px gold bar sits sticky at the top of the message log. As the user scrolls
through the conversation, the gold fill grows from left to right (0% = top, 100%
= fully scrolled to bottom). Hides entirely when the log has less than 40px of
scrollable range (short sessions with only 1–2 messages — no point showing a
progress bar on a micro-conversation).

#### Why `position:sticky; top:0` inside the log

The scrollprog element is inserted as the FIRST child of `.pb-brain__log`.
`position:sticky; top:0` inside an `overflow-y:auto` container sticks the
element to the top of the container's visible viewport as the user scrolls —
standard progressive enhancement. The 2px height is negligible in the flow.

Alternative approaches considered:
- `position:absolute; top:56px` on `.pb-brain` — requires hardcoding the
  header height offset (fragile if header height changes)
- `position:fixed` — would need `z-index` battle and viewport-relative coords
- CSS custom property (`--sp`) via `setProperty` — adds complexity for no gain

The sticky-inside-scrollable approach is the simplest and most robust.

#### The 40px range threshold

`log.scrollHeight - log.clientHeight < 40` means the entire conversation fits
in the viewport with less than 40px of overflow. Showing a progress bar here
would be visual noise — the user can already see everything. 40px is about 2
lines of text, a safe threshold above a "no actual scrolling needed" state.

#### Implementation

```javascript
// Scroll progress strip — sticky 2px bar at top of log; gold fill shows % scrolled.
var _scrollProg = document.createElement('div');
_scrollProg.className = 'pb-brain__scrollprog';
_scrollProg.setAttribute('aria-hidden', 'true');
var _scrollProgBar = document.createElement('div');
_scrollProgBar.className = 'pb-brain__scrollprog-bar';
_scrollProg.appendChild(_scrollProgBar);
if (log) log.insertBefore(_scrollProg, log.firstChild);
function _updateScrollProg() {
  if (!log) return;
  var range = log.scrollHeight - log.clientHeight;
  if (range < 40) { _scrollProg.style.display = 'none'; return; }
  _scrollProg.style.display = '';
  _scrollProgBar.style.width = Math.min(100, Math.round(log.scrollTop / range * 100)) + '%';
}
if (log) log.addEventListener('scroll', _updateScrollProg, { passive: true });
```

**`aria-hidden="true"`** — purely decorative. Screen readers don't need to
announce scroll percentage; the chat log is already navigable by heading/landmark.

**`{ passive: true }`** on the scroll listener — scroll listeners that never
call `preventDefault` should be declared passive. Passive listeners execute
off the main thread in browsers that support it, preventing jank.

**`Math.min(100, Math.round(...))`** — `Math.round` keeps the percentage an
integer so the bar doesn't flicker on fractional pixel changes. `Math.min(100,
...)` prevents the bar from briefly showing >100% due to rounding in browsers
with fractional scroll values.

DOM structure of the log after this change:
```
div.pb-brain__log             ← overflow-y:auto; scroll container
  div.pb-brain__scrollprog    ← position:sticky; top:0; 2px; aria-hidden=true
    div.pb-brain__scrollprog-bar   ← width:N%; gold gradient
  div.pb-brain__msg.pb-brain__msg--bot   ← first greeting
  div.pb-brain__msg.pb-brain__msg--user  ← user sends
  ...
```

### Help panel shortcut fix (R40 follow-up)

In R40, the `Ctrl+↑` single-message restore was replaced with terminal-style
`↑/↓` multi-message history navigation. The keyboard shortcuts help panel
(`helpBtn` → `_helpPanel`) still showed the old shortcut. Fixed:

**Before:**
```javascript
'<span><kbd>Ctrl</kbd>+<kbd>↑</kbd> Edit last message</span>',
```

**After:**
```javascript
'<span><kbd>↑</kbd> / <kbd>↓</kbd> Browse sent messages</span>',
```

No functional change — just corrects stale documentation visible to users who
open the `?` help panel. "Browse sent messages" is more descriptive than "Edit
last message" because the new feature cycles through ALL sent messages, not just
the most recent one.

## main.css — `.pb-brain__scrollprog` / `.pb-brain__scrollprog-bar`

```css
/* R43 — Scroll progress strip */
.pb-brain__scrollprog{
  position:sticky; top:0; z-index:3; height:2px;
  background:rgba(255,255,255,.06); pointer-events:none; display:none;
}
.pb-brain__scrollprog-bar{
  height:100%; width:0;
  background:linear-gradient(90deg,rgba(255,212,0,.6),rgba(255,180,0,.35));
  transition:width .08s linear; border-radius:0 1px 1px 0;
}
```

- `position:sticky; top:0` — sticks to top of `.pb-brain__log` scroll viewport
- `z-index:3` — above message content; below the fetch-progress bar (which also
  uses z-index) if they ever co-exist
- `height:2px` — matches the fetch progress bar (R38) thickness for visual
  consistency; thin enough to be non-intrusive
- `background:rgba(255,255,255,.06)` — barely-visible track; dark navy mix
- `pointer-events:none` — strip must never intercept clicks on the first message
- `display:none` initial state — set to `''` by JS when range ≥ 40px
- Gold gradient `rgba(255,212,0,.6) → rgba(255,180,0,.35)`: same gold as the
  fetch progress bar (R38), creating a consistent visual language for
  "progress" indicators across the chat widget
- `transition:width .08s linear` — 80ms is fast enough to feel live but slow
  enough to be smooth. Longer (200ms) would lag noticeably behind scrolling;
  shorter would look choppy.
- `border-radius:0 1px 1px 0` — right end of the filled bar has a soft
  1px radius; left end is flush with the container edge. Gives the bar a
  clean leading edge without appearing clipped.

## brainstorm.php — 3 new intent handlers

### 0d-pre39-a) Flooring installer / floor layer / carpet fitter / flooring company
Keywords: flooring installer website, flooring company website, carpet fitter website,
floor layer website, carpet fitting website, hardwood floor website, laminate flooring
website, vinyl flooring website, parquet floor website, carpet installer website, wood
floor website, flooring contractor website, floor fitting website, floor installer
website, lvt flooring website, amtico website, karndean website, engineered wood
flooring website

Response: material-first decision journey — customer picks material THEN searches for installer
- Flooring type pages: one per material (carpet/hardwood/engineered wood/LVT/
  laminate/parquet/polished concrete/Amtico/Karndean/herringbone); species/grade/
  construction notes; underfloor heating compatibility; from $150/page
- Room calculator: dimensions → m² + waste factor (10% for LVT/laminate, 15% for
  herringbone/carpet seaming); optional labour rate → rough total; from $350
- Before/after gallery: filterable by room type (bedroom/kitchen/hallway/open-plan/
  staircase/office) and material; from $250
- Showroom/sample page: Matterport 3D tour embed or sample-request form; reduces
  decision anxiety before the site visit; from $200
- Trade supply page: contractor pricing for developers/property managers; bulk order;
  sample service; B2B tone; from $200
- Aftercare and maintenance guides: per-material cleaning guides; warranty info;
  reinforces expertise
- From $500 type pages + calculator + gallery; $900+ with showroom + trade page + aftercare

### 0d-pre39-b) Cleaning company / office cleaner / domestic cleaner / end-of-tenancy
Keywords: cleaning company website, cleaning service website, office cleaning website,
commercial cleaning website, domestic cleaning website, house cleaning website, end
of tenancy cleaning website, deep cleaning website, industrial cleaning website,
contract cleaning website, office cleaner website, carpet cleaning website, window
cleaning website, oven cleaning website, home cleaning website, professional cleaning
website, cleaning business website

Response: contract frequency and trust are the two conversion levers
- Service pages: one per service (office contract / domestic regular / end-of-tenancy /
  carpet extraction / window / oven / event / post-construction); from $200/page
- Online quote form: property type + sq ft + frequency (one-off/weekly/fortnightly/
  monthly) + access (key-hold/alarm code) + add-ons; email quote within 24h; from $250
- DBS-checked staff page: enhanced DBS; uniformed and ID-badged; key-hold + alarm-code
  procedures; public liability + employer's liability amounts; most-read page for
  contract cleaning prospects
- COSHH/eco-cleaning section: COSHH-assessed products; Force of Nature/Delphis Eco
  option; biodegradable consumables; critical for food-prep/healthcare/school contracts
- End-of-tenancy guarantee: re-clean within 72 hours if deposit deducted; strongest
  differentiator in tenant cleaning; from $150 to add
- Checklist downloads: EOT checklist PDF; office handover checklist; positions company
  as expert; from $150
- Case studies: contract wins (office/sq ft/frequency/scope) + before/after carpet
  extraction photos; from $150/case study
- From $550 service pages + quote form + DBS page; $1,000+ with EOT guarantee +
  checklists + case studies

### 0d-pre39-c) Locksmith / emergency locksmith / security company / safe installer
Keywords: locksmith website, emergency locksmith website, locksmith near me website,
key cutter website, security company website, lock company website, safe installer
website, cctv installer website, access control website, security systems website,
door entry system website, master key system website, commercial locksmith website,
residential locksmith website, locksmith business website

Response: two trust barriers — emergency pricing suspicion + "is this engineer
legitimate?" — both need addressing on the homepage
- Emergency call-out page: 24/7; response time (20–30 min radius); NON-DESTRUCTIVE
  entry emphasis (85% of lock-outs can be opened without drilling — most customers
  don't know this and it's the strongest differentiator); FIXED call-out rate
  displayed (not "from £X" which triggers bait-and-switch suspicion); from $200
- Service pages: residential (lock change/upgrade/duplicate key/window lock) /
  commercial (master key system/access control/panic hardware/mortice) / auto
  (vehicle lock-out/transponder key programming) / emergency (24/7/boarding up
  after break-in); from $150/page
- Cylinder/lock standards page: BS EN 1303 / TS 007 3-star / Secured by Design;
  Ultion/Mul-T-Lock/ASSA ABLOY/Chubb recommendations; customers who research
  know these terms and this page earns the quote
- Security survey enquiry: free survey → quotation → install pipeline; CCTV and
  access control upsell path; from $200
- Trust signals: MLA (Master Locksmith Association) approved badge — fewer than
  2,000 in the UK; DBS-checked engineers; public liability insurance amount;
  no call-out fee (if applicable); genuine Google reviews embed
- CCTV/access control page: CCTV survey + Hikvision/Dahua/Avigilon systems;
  Paxton/HID access control; door entry and intercom; from $300
- Area pages: "[area] locksmith" + response time + local reviews; up to 5 radius
  towns; Local Business schema; from $100/page
- From $500 emergency page + service pages + trust signals; $1,000+ with CCTV/
  access control + cylinder standards + area pages

## QA results (36/37 auto + 1 manual = 37/37 all pass)

| Check | Result |
|-------|--------|
| scrollprog div created | OK |
| scrollprog bar child div | OK |
| scrollprog appended to bar (manual) | OK — `_scrollProg.appendChild(_scrollProgBar)` at line 341; QA regex false-negative (underscore prefix not in pattern) |
| inserted as log.firstChild | OK |
| _updateScrollProg function | OK |
| range = scrollHeight - clientHeight | OK |
| hides when range < 40 | OK |
| width Math.min 100 | OK |
| passive scroll listener | OK |
| bar width set as % | OK |
| stale Ctrl+up removed | OK |
| new up/down shortcut in help | OK |
| scrollprog sticky top 0 | OK |
| scrollprog height 2px | OK |
| scrollprog display none | OK |
| scrollprog-bar gold gradient | OK |
| scrollprog-bar transition | OK |
| scrollprog-bar border-radius | OK |
| flooring installer keywords | OK |
| LVT karndean amtico | OK |
| room calculator waste factor | OK |
| underfloor heating compat | OK |
| from $500 flooring | OK |
| cleaning company keywords | OK |
| end of tenancy cleaning kw | OK |
| DBS checked staff page | OK |
| COSHH eco cleaning | OK |
| EOT re-clean guarantee | OK |
| from $550 cleaning | OK |
| locksmith keywords | OK |
| non-destructive entry emphasis | OK |
| MLA approved badge | OK |
| BS EN 1303 cylinder standard | OK |
| TS007 3-star | OK |
| Ultion Mul-T-Lock Chubb | OK |
| CCTV Hikvision Paxton | OK |
| from $500 locksmith | OK |
