# DOM QA Report — R46 — 2026-06-20

## Bug fix (pre-existing): `addMsg` class name ternary restored

Line 612 was `div.className = 'pb-brain__msg pb-brain__msg--' + cls` (raw concat).
With `cls = 'user'` (the submit handler value), this produced `pb-brain__msg--user`
while the CSS only knows `pb-brain__msg--me`. The retry button's
`querySelectorAll('.pb-brain__msg--me')` would also return empty — retry would
silently fail to find the last user message.

**Fix:** restored the original ternary:
```javascript
div.className = 'pb-brain__msg pb-brain__msg--' +
  (cls === 'bot' ? 'bot' : cls === 'err' ? 'err' : 'me');
```

`'user'` (and any other non-bot, non-err cls value) now maps to `--me`. CSS,
retry button, and star filter all use `--me` and now work correctly.

## main.js — Relative timestamps + `--new` ping class

### Relative timestamps

Timestamps on every chat message now display relative time ("just now", "2m ago",
"1h ago") instead of the fixed locale clock string. They update every 60 seconds
via `setInterval`.

#### Why relative instead of absolute

Absolute times ("10:35 PM") answer a different question than what users in an
active conversation want to know: "how long ago did this happen?" A fresh reply
showing "just now" is instant reinforcement that the bot responded immediately.
A reply from "3m ago" gives the user a sense of the latency during a back-and-forth
without opening their system clock.

#### How it works

```javascript
function _relTime(iso) {
  var diff = Math.max(0, Math.round((Date.now() - new Date(iso).getTime()) / 1000));
  if (diff < 60) return 'just now';
  var m = Math.floor(diff / 60);
  if (m < 60) return m + 'm ago';
  var h = Math.floor(m / 60);
  if (h < 24) return h + 'h ago';
  return Math.floor(h / 24) + 'd ago';
}
function _updateTimestamps() {
  var tss = log ? log.querySelectorAll('time.pb-brain__ts') : [];
  [].forEach.call(tss, function(t) {
    var iso = t.getAttribute('datetime');
    if (iso) t.textContent = _relTime(iso);
  });
}
setInterval(_updateTimestamps, 60000);
```

**Source of truth**: `datetime` ISO attribute on each `<time>` element, set at
message creation time. `_relTime` reads this and computes `Date.now() - new Date(iso)`.

**Initial state**: when `addMsg` creates a message, `_tsEl.textContent = 'just now'`
immediately (changed from `toLocaleTimeString`). After 60 seconds, `_updateTimestamps`
will rewrite it to "1m ago", then "2m ago" at 2 minutes, etc. No false "just now"
for old session-restored messages — those will be corrected on the first interval fire
(within 60 seconds of page load).

**Hover title**: `_tsEl.title = "10:35 PM · 6/20/2026"` — the absolute time is still
accessible via the browser's native tooltip on hover over the timestamp badge. The
user never permanently loses the exact time, they just need to hover to see it.
Timestamps are already revealed on hover via CSS opacity transition — the title tooltip
appears on the same hover. No extra interaction cost.

**60-second interval**: chosen because "just now" → "1m ago" is the only transition
that has a 60-second window. All subsequent transitions (1m → 2m, 5m → 6m, 59m → 1h)
happen at exact minute multiples. A 60-second interval means the displayed time is
accurate to within ±60 seconds — sufficient precision for the conversational context.
A 1-second interval would be 60× as many DOM updates for no perceivable user benefit.

### `pb-brain__msg--new` ping class

Every new non-error message (bot and user) gets `div.classList.add('pb-brain__msg--new')`
on creation. The CSS uses this class to layer a gold ring ping on top of the existing
R45 slide-up animation.

```javascript
if (cls !== 'err') div.classList.add('pb-brain__msg--new');
```

The class is never removed — the animation plays once and the element returns to its
default `box-shadow: none` (animation-fill-mode defaults to none, so no fill persists).

## main.css — `@keyframes pbMsgPing` + `.pb-brain__msg--new`

```css
@keyframes pbMsgPing {
  from { box-shadow: 0 0 0 0 rgba(255,212,0,.28); }
  to   { box-shadow: 0 0 0 8px rgba(255,212,0,0); }
}
.pb-brain__msg--new { animation: pbMsgIn .14s ease-out, pbMsgPing .6s ease-out; }
```

**Box-shadow instead of background**: a radial box-shadow ring expands from the
message border outward (0px → 8px spread) while fading to fully transparent. This
appears as a gold ripple that radiates from the bubble edge. It doesn't affect the
message background, text, or any child element — purely additive visual effect.

**Why gold**: `rgba(255,212,0,.28)` starting opacity matches the R45/R44 gold design
language (scroll progress bar, starred items, fetch progress bar all use the same gold
family). The ring visually "tags" the new message with the same accent colour.

**Layering with R45 `pbMsgIn`**: `.pb-brain__msg` sets `animation: pbMsgIn .14s ease-out`
(specificity 0,1,0). `.pb-brain__msg--new` wins with specificity 0,2,0 (two classes)
and sets `animation: pbMsgIn .14s ease-out, pbMsgPing .6s ease-out` — both animations
run simultaneously. The slide-up (140ms) finishes first; the gold ring (600ms) is still
fading out while the text is settling in. No conflict — they animate different properties
(`transform` vs `box-shadow`).

**Error messages excluded**: error messages (`cls === 'err'`) don't get `--new`. They
have their own red styling and are usually short inline notices — a gold ring ping would
be visually inconsistent.

## brainstorm.php — 3 new intent handlers

### 0d-pre42-a) Dentist / dental practice / orthodontist website
Keywords: dentist website, dental practice website, dental clinic website, NHS dentist
website, private dentist website, orthodontist website, cosmetic dentist website,
dental implants website, dental hygienist website, teeth whitening website, Invisalign
website, braces website, emergency dentist website, childrens dentist website, smile
clinic website, dental surgery website

Response: GDC registration + emergency access are the two conversion anchors
- Treatment pages: check-up / hygienist / composite bonding / veneers / whitening /
  implants / Invisalign / fixed braces / extractions / root canal / emergency /
  children's dentistry; what to expect description; NHS charge band or price; $200/page
- Online booking: Dentally / Software of Excellence / Exact; NHS vs private slot types;
  new patient form; $300
- GDC and CQC compliance: GDC registration number per dentist (required by GDC Standards
  for Dental Professionals); CQC registration and inspection outcome; complaints procedure
- NHS charge bands: Band 1 £26.80 / Band 2 £73.50 / Band 3 £319.10 (England); private
  fee guide; mixed practice explanation
- Cosmetic before/after gallery: composite bonding / Invisalign / whitening; consent photos
- Emergency page: same-day slots vs 111 pathway; knocked-out tooth guide (re-implant within
  30 minutes, milk transport medium); ranks for "[area] emergency dentist"
- From $600 / $1,200+

### 0d-pre42-b) Optician / optical practice / optometrist website
Keywords: optician website, optical practice website, optometrist website, contact lens
specialist website, glasses website, prescription glasses website, sunglasses website,
eye test website, eye examination website, childrens eye test website, dry eye clinic
website, myopia management website, ophthalmologist website, vision therapy website,
spectacle website, eyewear website

Response: GOC trust signals + frame selection experience are the two conversion levers
- Eye examination page: GOC registered optometrist; £30–£55 sight test; OCT scan
  premium; diabetes/glaucoma/AMD screening; children's NHS-funded tests; $200
- Online booking: Optinet / Acuity / Phorest; test type selector; $300
- GOC compliance: GOC (General Optical Council) number per optometrist (required); CQC;
  professional indemnity; complaints link
- Frames and lenses: designer brands (Ray-Ban / Oakley / Tom Ford / Lindberg / Silhouette);
  lens types (single vision / bifocal / varifocal / photochromic); coatings; $250
- Virtual try-on: Ditto / Fittingbox embed; reduces frame-selection anxiety; $200
- Contact lens page: daily / monthly / toric / multifocal; reorder subscription; $200
- Myopia management: Ortho-K / MiSight / low-dose atropine; parent-focused content; $200
- From $550 / $1,100+

### 0d-pre42-c) Chiropractor / physiotherapist / osteopath / sports therapist website
Keywords: chiropractor website, physiotherapist website, physio website, osteopath
website, sports therapist website, massage therapist website, sports massage website,
back pain clinic website, neck pain website, sports injury website, rehabilitation
clinic website, acupuncturist website, manual therapist website, musculoskeletal
clinic website, msk clinic website, sports clinic website, chiropractic website

Response: condition pages (not treatment pages) are the highest-converting structure
- Condition pages: lower back pain / sciatica / neck pain / shoulder / knee / hip /
  sports injury / post-op rehab / headaches / pregnancy pain; patients search by
  condition not modality; $150/page
- Online booking: Cliniko / Power Diary / Jane App; new patient vs follow-up; $300
- Regulatory compliance: GCC (General Chiropractic Council) number — required by law
  (Chiropractors Act 1994); HCPC for physios (required); GOSC for osteopaths (required);
  CNHC for massage/sports therapists; display registration numbers prominently
- Practitioner profiles: headshots + registration number + specialisms + CPD interests +
  sport they treat; builds rapport; $150/profile
- Condition-matched testimonials: lower back testimonials on the lower back page etc.
- Self-help video content: rehab exercise guides on YouTube; when to seek help guide
- Insurance page: BUPA / AXA / employer healthcare; direct billing note; $150
- From $500 / $1,000+

## QA results (40/40 all pass)

| Check | Result |
|-------|--------|
| class name ternary restored | OK |
| --new class added for non-err | OK |
| _tsEl.textContent = just now | OK |
| _tsEl.title has toLocaleTimeString | OK |
| _tsEl.title has toLocaleDateString | OK |
| _relTime function defined | OK |
| _relTime returns just now < 60 | OK |
| _relTime m ago branch | OK |
| _relTime h ago branch | OK |
| _relTime d ago branch | OK |
| _updateTimestamps function defined | OK |
| querySelectorAll time.pb-brain__ts | OK |
| forEach updates textContent from iso | OK |
| setInterval 60000 for timestamps | OK |
| @keyframes pbMsgPing defined | OK |
| pbMsgPing from box-shadow 0 spread | OK |
| pbMsgPing to box-shadow 8px spread fade | OK |
| --new runs both animations | OK |
| dentist keywords present | OK |
| GDC registration required note | OK |
| CQC registration note | OK |
| NHS charge bands listed | OK |
| emergency dentist page | OK |
| knocked-out tooth guide | OK |
| Dentally Software of Excellence | OK |
| from $600 dentist | OK |
| optician keywords present | OK |
| GOC registration required note | OK |
| OCT scan premium mention | OK |
| myopia management page | OK |
| virtual try-on Ditto Fittingbox | OK |
| from $550 optician | OK |
| chiropractor physio keywords | OK |
| condition pages not treatment pages | OK |
| GCC required by law | OK |
| HCPC required by law | OK |
| GOSC osteopaths required | OK |
| Cliniko Power Diary Jane App | OK |
| BUPA AXA insurance page | OK |
| from $500 chiropractor | OK |
