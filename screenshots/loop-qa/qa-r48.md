# DOM QA Report — R48 — 2026-06-20

## main.js — Response-time badge + `_italic_` in format()

### Response-time badge on bot messages

Each bot reply produced by a live fetch (not session-restore) now shows a `⚡ Xs`
badge hidden behind the message bubble. Hovering the bot message reveals it alongside
the timestamp and word-count badges.

```javascript
var _lastSendTime = 0; // declared at IIFE top

// In form submit handler (before addMsg('user')):
_lastSendTime = Date.now();

// In addMsg(), after _tsEl creation:
if (cls === 'bot' && _lastSendTime > 0 && (Date.now() - _lastSendTime) < 30000) {
  var _rtEl = document.createElement('small');
  _rtEl.className = 'pb-brain__resp-time';
  _rtEl.textContent = '⚡ ' + ((Date.now() - _lastSendTime) / 1000).toFixed(1) + 's';
  div.appendChild(_rtEl);
  _lastSendTime = 0;
}
```

**`_lastSendTime` at IIFE scope**: must be accessible from both the form submit
handler (which sets it) and `addMsg()` (which reads it). Both are closures inside the
same IIFE, so a `var` at IIFE top is the correct scope. An alternative (passing it as
an argument) would require changing the `addMsg` signature, which would break all
existing call sites.

**30-second guard**: `(Date.now() - _lastSendTime) < 30000` prevents the badge from
appearing on session-restore calls to `addMsg`. When the chat is restored from
`sessionStorage pb_chat_v1` on page load, `_lastSendTime` is still `0` (reset at
the end of the previous session when the last response arrived, or never set if this
is the first session). The `_lastSendTime > 0` check already catches the null case,
but the 30-second window is a second guard: if somehow `_lastSendTime` were non-zero
from a stale previous state, a restore call happening > 30s after the last send
would still be filtered out.

**Reset after badge**: `_lastSendTime = 0` inside the badge block ensures only the
FIRST bot reply after a user send gets a response-time badge. If a bot response is
split into multiple `addMsg` calls (rare in this codebase — the handler returns a
single string — but defensive), only the first one would show timing.

**`<small>` element**: `<small>` is semantically correct for fine print and legal text
in HTML5. Here it renders at `font-size:9px` (smaller than the surrounding message
text), which is the correct visual role — it's supplementary metadata, not primary
content. An alternative `<span>` would also work visually but `<small>` communicates
semantic significance to assistive technology.

**`.toFixed(1)`**: one decimal place (e.g., "⚡ 1.4s"). `Date.now()` difference in
milliseconds divided by 1000 gives a fractional second. Without `.toFixed(1)` the
value would be e.g. "1.4156982..." — noisy. One decimal gives enough precision to
show "⚡ 0.3s" (fast local fallback) vs "⚡ 2.8s" (OpenAI API round trip) at a glance.
Zero decimal places would round "0.4s" to "0s" — misleading.

### `_italic_` support in `format()`

Markdown `_italic text_` now renders as `<em>italic text</em>`.

```javascript
.replace(/(?<!\w)_([^_\n]+)_(?!\w)/g, '<em>$1</em>')
```

Added after the `**bold**` replacement, before the link patterns.

**Why here in the pipeline**: at this point in `format()`, fenced code blocks and
inline code have already been extracted and replaced with `\x00CB0\x00` / `\x00IC0\x00`
placeholders. The HTML-escape step has run (so `_` in any `<pre>` content is already
in a placeholder and won't be touched). The italic replacement runs on prose text only.

**`(?<!\w)` negative lookbehind**: ensures the opening `_` is NOT preceded by a word
character. Without this, `snake_case` would match `snake` + `_case_` (if `case_` ends
before a non-word char). With the lookbehind, `y_` in `snake_case_type` is preceded
by `e` (a word char) → no match. `_italic_` at the start of a sentence is preceded by
a space or `<br>` (non-word) → matches.

**`(?!\w)` negative lookahead**: ensures the closing `_` is NOT followed by a word
character. Prevents matching `_var1_var2` as `_var1` + italic-marker-on-`var2`.

**`[^_\n]+` inner pattern**: no nested `_` or newlines in the italic span. This
prevents a long paragraph with multiple standalone underscores from merging into one
giant italic span. If a message contains `_alpha_ middle _beta_`, each pair matches
independently.

**Lookbehind availability**: `(?<!\w)` negative lookbehind is available in Chrome 62+,
Firefox 78+, Safari 16.4+, and all major browsers since ~2022. The chatbot targets
modern browsers (it uses `async/await`, `Array.from`, `sessionStorage`, `CSS custom
properties`) — all of which require the same browser tier or earlier. No polyfill needed.

**Position relative to bold**: `**bold**` is replaced before `_italic_`. This prevents
the (pathological) case of `**_both_**` being parsed as bold then failing on
`_` inside the already-replaced `<strong>` tags (the `_` would be in rendered HTML, not
source text). In practice the bot doesn't generate nested markdown; the ordering just
avoids an edge case.

## main.css — Response-time badge styles

```css
.pb-brain__resp-time{
  display:block; font-size:9px; color:rgba(255,212,0,.35);
  text-align:right; margin-top:2px; opacity:0;
  transition:opacity .15s;
}
.pb-brain__msg--bot:hover .pb-brain__resp-time{ opacity:1; }
```

**`display:block` with `text-align:right`**: the badge appears below the message
text, right-aligned. `display:block` makes it take full width of its container
(`.pb-brain__msg--bot`), so `text-align:right` pushes the text to the right edge of
the bubble. This mirrors the right-aligned timestamp (`.pb-brain__ts`) and word-count
(`.pb-brain__wc`) badges for visual consistency.

**`opacity:0` initial / revealed on hover**: same reveal pattern as `.pb-brain__ts`
and `.pb-brain__wc`. All three metadata badges appear on hover via `opacity`
transition. `opacity:0` keeps the badge in the accessibility tree (unlike `display:none`
which removes it) — screen readers can still read it.

**`.pb-brain__msg--bot:hover` selector**: scoped specifically to bot messages. User
messages (`--me`) don't have response-time badges so no rule needed for them. The
hover scoping on the parent message div means the entire bubble surface area triggers
the reveal — the user doesn't need to hover over the tiny badge itself.

**`transition:opacity .15s`**: 150ms is the same as `.pb-brain__wc`. The fade-in feels
instantaneous but avoids the harsh pop of `transition: none`.

**Gold at low opacity `rgba(255,212,0,.35)`**: lower than the word-count badge's alpha
(which is `.28` for the text, but references the same gold family). The ⚡ emoji
already has high visual weight; the surrounding text being at `.35` alpha keeps it
quiet — the badge supplements, not competes with, the message content.

## brainstorm.php — 3 new intent handlers

### 0d-pre44-a) Tattoo studio / tattoo artist / piercing studio website
Keywords: tattoo studio website, tattoo artist website, tattoo parlour website,
tattoo shop website, piercing studio website, body piercing website, tattoo removal
website, laser tattoo removal website, traditional tattoo website, japanese tattoo
website, watercolour tattoo website, blackwork tattoo website, realism tattoo website,
semi-permanent makeup tattoo website, microblading tattoo website, fine line tattoo website

Response: artist portfolio quality is the product — healed photos and style separation convert
- Artist portfolio pages: healed tattoo photos (NOT fresh — healed result is the promise);
  style categories (traditional/Japanese/blackwork/fine line/watercolour/realism/
  neo-traditional/lettering/geometric); from $250 per artist
- Consultation booking: reference images + placement + size + budget + skin tone; deposit
  at booking (£50–£100 credited against tattoo); Calendly or custom form; from $300
- Style guide pages: "What is blackwork?" / "How long does a sleeve take?" — educates
  first-timers and reduces admin emails; from $150
- Aftercare page: written and video instructions; healed-tattoo FAQ; from $150
- Compliance: Skin Piercing and Tattooing Act; local authority licence number per
  tattooist; single-use needle statement; age restriction 18+ statement (required by law)
- Flash sale / available designs: artist flash sheets bookable immediately; lower price
  point; converts walk-in traffic; WooCommerce or simple form; from $200
- Gift vouchers: WooCommerce; birthday/Christmas; "gift a consultation" option; from $150
- From $550 / $1,100+

### 0d-pre44-b) Childminder / nursery / preschool / day nursery / after-school club website
Keywords: childminder website, nursery website, day nursery website, preschool website,
pre-school website, after school club website, breakfast club website, holiday club website,
childcare website, nanny agency website, au pair agency website, creche website,
early years website, toddler group website, mother and toddler website, out of school
care website, wrap around care website

Response: Ofsted rating + environment photos are the conversion anchors
- About the setting: Ofsted number + most recent outcome (prominent); photos of rooms,
  outdoor space, meals, activities; opening hours; age range; from $250
- Online enquiry + waiting list: child name + DoB + start date + session type + funding
  eligibility; WooCommerce/Gravity Forms; deposit to secure place; from $300
- Government-funded places page: 15h universal entitlement (all 3–4yr-olds); 30h extended
  (working parents); 15h from 9 months (April 2024 expansion); Tax-Free Childcare;
  most-asked parent question; from $200
- Team profiles: key person system; Level 3 qualification + DBS-checked + paediatric
  first-aid certification date; from $150/profile
- Curriculum: EYFS framework; phonics/maths/creative play/outdoor; forest school; from $200
- Parent testimonials: Google Reviews widget; specific quotes about settling-in
- Safeguarding/policies: safeguarding policy; SENCO contact; GDPR notice; from $150
- From $600 / $1,200+

### 0d-pre44-c) Building contractor / builder / construction company website
Keywords: builder website, building contractor website, construction company website,
general contractor website, property developer website, house builder website,
extension builder website, loft conversion website, renovation contractor website,
refurbishment contractor website, groundworks contractor website, commercial
contractor website, fit-out contractor website, main contractor website, building
company website, construction firm website

Response: polished project portfolio + accreditation signals convert — the before/after IS the CV
- Project portfolio: before/after; project type + location + approximate value + client
  quote; from $300
- Services pages: extensions/loft/new builds/renovations/groundworks/commercial/
  fit-out/structural repairs; typical timeline + indicative price; from $150/page
- Planning and process page: planning permission/permitted development/building control;
  stages (feasibility→design→contract→build→snagging→sign-off); reduces pre-sales calls
- Accreditations: FMB (Federation of Master Builders); NHBC/Premier Guarantee warranty;
  CHAS/Constructionline (commercial tenders); from $150
- Free quote form: project type + location + size (m²) + planning status + budget range;
  48-hour response acknowledgement; from $200
- Testimonials/case studies: one per project type; Google Reviews; from $200/case study
- Compliance: VAT registration (B2B); public liability £5m minimum; employers' liability
  (legally required); waste carrier licence; from $150
- From $600 / $1,200+

## QA results (37/37 all pass)

| Check | Result |
|-------|--------|
| _lastSendTime declared at IIFE top | OK |
| _lastSendTime set in submit handler | OK |
| resp-time guard: cls === bot | OK |
| resp-time guard: < 30000ms | OK |
| resp-time element created as small | OK |
| resp-time class pb-brain__resp-time | OK |
| resp-time lightning emoji text | OK |
| resp-time toFixed 1 | OK |
| _lastSendTime reset to 0 after badge | OK |
| italic regex lookahead/behind added | OK |
| italic em tag in format | OK |
| pb-brain__resp-time CSS block | OK |
| resp-time font-size 9px | OK |
| resp-time gold colour | OK |
| resp-time opacity 0 initial | OK |
| resp-time hover opacity 1 | OK |
| bot hover reveals resp-time | OK |
| tattoo studio keywords | OK |
| flash sheet available designs | OK |
| aftercare page tattoo | OK |
| age restriction 18+ law | OK |
| skin piercing tattooing act | OK |
| healed tattoo photos note | OK |
| from $550 tattoo | OK |
| childminder nursery keywords | OK |
| Ofsted rating mention | OK |
| EYFS framework mention | OK |
| government funded places | OK |
| 30-hour entitlement note | OK |
| Tax-Free Childcare scheme | OK |
| from $600 childminder | OK |
| builder contractor keywords | OK |
| FMB federation of master builders | OK |
| NHBC warranty provider | OK |
| public liability 5m | OK |
| waste carrier licence builder | OK |
| from $600 builder | OK |
