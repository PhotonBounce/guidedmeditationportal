# DOM QA Report — R42 — 2026-06-20

## main.js — Collapsible long bot replies

Bot messages with 6 or more line-spans (≈ 6 bullet points or paragraphs) are
collapsed to approximately 3 visible lines with a gradient fade at the bottom
and a "Show more ↓" button below. Clicking expands to full height and changes to
"Show less ↑". A second click re-collapses.

### Structural change — `div.pb-brain__text` wrapper

The bot text is now wrapped in a dedicated `div.pb-brain__text` rather than set
directly on the message div. This is the key structural decision that makes the
collapse feature work without clipping the toggle button.

**Before (direct innerHTML on message div):**
```javascript
if (cls === 'bot') {
  var _fmtLines = format(text).split('<br>');
  div.innerHTML = _fmtLines.map(function(ln, i) {
    return '<span class="pb-brain__line" style="animation-delay:' + (i * 70) + 'ms">' + ln + '</span>';
  }).join('<br>');
}
```

**After (text inside pb-brain__text wrapper):**
```javascript
if (cls === 'bot') {
  var _fmtLines = format(text).split('<br>');
  var _textBody = document.createElement('div');
  _textBody.className = 'pb-brain__text';
  _textBody.innerHTML = _fmtLines.map(function(ln, i) {
    return '<span class="pb-brain__line" style="animation-delay:' + (i * 70) + 'ms">' + ln + '</span>';
  }).join('<br>');
  div.appendChild(_textBody);
}
```

DOM structure of a collapsed bot message:
```
div.pb-brain__msg.pb-brain__msg--bot
  div.pb-brain__text.pb-brain__text--collapsed   ← text body, max-height clipped
    span.pb-brain__line (0) ← visible
    <br>
    span.pb-brain__line (1) ← visible (line-reveal animation unchanged)
    ...
    span.pb-brain__line (4) ← last visible
    <br>
    span.pb-brain__line (5) ← clipped by max-height
    ...
  div.pb-brain__fb                                ← thumbs; outside text wrapper
  button.pb-brain__star                           ← star; outside text wrapper
  time.pb-brain__ts                               ← timestamp; outside text wrapper
  span.pb-brain__wc                               ← word count; outside text wrapper
  button.pb-brain__toggle                         ← "Show more ↓"; SIBLING to text wrapper
```

The critical design choice: `max-height + overflow:hidden` is applied to
`.pb-brain__text` ONLY. The toggle button is appended to `div` (the message
div), not to `_textBody`, so it is never inside the clipped container and is
always visible.

Why not `display:none` on individual spans? The spans are joined with `<br>`
elements. Setting `display:none` on a span doesn't hide its preceding `<br>`,
leaving orphaned line-break whitespace that creates ugly empty lines. The
`max-height` approach avoids this entirely — the wrapper clips from the bottom,
including the `<br>` elements, and the `::after` gradient hides the cut-off line.

### Collapse toggle implementation

```javascript
var _tbody = div.querySelector('.pb-brain__text');
var _tbLines = _tbody ? _tbody.querySelectorAll('.pb-brain__line') : [];
if (_tbody && _tbLines.length > 5) {
  _tbody.classList.add('pb-brain__text--collapsed');
  var _expandBtn = document.createElement('button');
  _expandBtn.type = 'button';
  _expandBtn.className = 'pb-brain__toggle';
  _expandBtn.textContent = 'Show more ↓';
  _expandBtn.setAttribute('aria-expanded', 'false');
  var _colOpen = false;
  _expandBtn.addEventListener('click', function() {
    _colOpen = !_colOpen;
    _tbody.classList.toggle('pb-brain__text--collapsed', !_colOpen);
    _expandBtn.textContent = _colOpen ? 'Show less ↑' : 'Show more ↓';
    _expandBtn.setAttribute('aria-expanded', String(_colOpen));
    if (_colOpen) { requestAnimationFrame(function() { log.scrollTop = log.scrollHeight; }); }
  });
  div.appendChild(_expandBtn);
}
```

**Threshold: `_tbLines.length > 5`** — brainstorm responses typically have
8–12 bullet points (line-spans); 5 visible lines shows the opening sentence,
blank line, and first 3–4 bullets — enough to evaluate the response without
committing to reading the whole thing.

**`_colOpen` state** — plain boolean per message (not persisted to sessionStorage;
collapsed state resets on page reload, which is appropriate — the user chose to
read it once, not always).

**`classList.toggle('pb-brain__text--collapsed', !_colOpen)`** — uses the
second argument (force flag) for reliable toggle: `!_colOpen` = `true` means
"add the class" = collapse; `!_colOpen` = `false` means "remove the class" =
expand. More reliable than checking `contains()` and toggling manually.

**`requestAnimationFrame(function() { log.scrollTop = log.scrollHeight; })`** —
after expand, wait one frame for the DOM to reflow (max-height is now removed),
then scroll to the bottom of the log. Without the rAF, `scrollHeight` might be
stale from before the class change.

**`aria-expanded`** — screen readers hear "Show more button, collapsed" when
`aria-expanded="false"` and "Show less button, expanded" when `aria-expanded="true"`.

## main.css — `.pb-brain__text` / `.pb-brain__text--collapsed` / `.pb-brain__toggle`

```css
.pb-brain__text { display:block; }
.pb-brain__text--collapsed {
  max-height:178px; overflow:hidden; position:relative;
}
.pb-brain__text--collapsed::after {
  content:''; position:absolute; bottom:0; left:0; right:0; height:64px;
  background:linear-gradient(transparent, rgba(10,20,46,.97));
  pointer-events:none;
}
.pb-brain__toggle {
  display:block; width:100%; margin-top:6px; padding:4px 0;
  font-size:11px; color:rgba(255,255,255,.42);
  background:none; border:1px solid rgba(255,255,255,.12);
  border-radius:4px; cursor:pointer; text-align:center;
  transition:.16s;
}
.pb-brain__toggle:hover { color:rgba(255,255,255,.78); border-color:rgba(255,255,255,.28); }
.pb-brain__toggle:focus-visible { outline:1px solid rgba(255,255,255,.3); border-radius:4px; }
```

- `max-height:178px` — approximately 3 full bullet points of text at the chat
  panel's font size (~15px line height × 10 lines + padding); generous enough to
  show the opening hook without truncating mid-sentence on line 1
- `position:relative` on `--collapsed` is required for `::after` to position
  correctly (absolutely inside the clipped container)
- `::after` gradient: `rgba(10,20,46,.97)` — matches the `.pb-brain` background
  color (dark navy); nearly opaque so the cut-off line is completely hidden
- `pointer-events:none` on `::after` — the gradient overlay must not intercept
  mouse events; the toggle button below it still needs to be clickable
- Toggle button: `display:block; width:100%` — full width of the message bubble;
  looks like a native "expand" control. `text-align:center` and `border:1px solid`
  make it visually clear it's interactive

## brainstorm.php — 3 new intent handlers

### 0d-pre38-a) Dog groomer / pet groomer / mobile groomer / pet salon
Keywords: dog groomer website, dog grooming website, pet groomer website,
pet grooming website, mobile dog groomer website, mobile grooming website,
pet salon website, dog salon website, cat groomer website, grooming salon website,
pet stylist website, dog wash website, grooming parlour website, animal grooming website

Response: trust and convenience are the two conversion levers
- Service menu with breed-size pricing: full groom / bath & brush / hand strip /
  scissor finish / nail trim / wash & dry; small/medium/large/giant size tiers;
  matted and double-coat surcharges; from $300
- Online booking: WP Amelia or SimplyBook.me; breed + service + coat condition;
  24h SMS reminder; automated rebooking suggestion at 6–8 weeks; from $350
- Breed speciality pages: one per breed (Doodles/Cocker Spaniels/Schnauzers/
  Bichons/Poodles); coat-specific style notes; "[breed] groomer [town]" SEO; from $150/page
- Before/after gallery: consent photos filterable by breed; most-converting content
  on grooming sites; coat transformation proof
- Mobile grooming page: service radius map; "no salon noise or strange dogs —
  less stressful for anxious pets"; premium pricing justification; from $200
- Trust: City & Guilds or iPET Network Level 3 qualification; PetPlan Business/
  Dognanny insurance; pet first aid; British Dog Groomers' Association membership
- Gift vouchers: WooCommerce gift cards; email delivery; from $150
- From $450 service menu + booking; $900+ with breed pages + gallery + mobile page

### 0d-pre38-b) Life coach / executive coach / business coach / NLP practitioner
Keywords: life coach website, life coaching website, executive coach website,
business coach website, executive coaching website, leadership coach website,
career coach website, NLP practitioner website, NLP coach website, mindset coach
website, performance coach website, confidence coach website, wellbeing coach website

Response: coaching is unregulated; credibility comes from results, not credentials
- ICF and EMCC membership: ICF (International Coaching Federation) / EMCC (European
  Mentoring & Coaching Council) are the nearest equivalent credentials in an
  unregulated industry; ICF ACC/PCC/MCC levels signal training hours — display prominently
- Core offer page: signature programme (12-week/6-month/3×1:1); target client avatar
  at specific enough detail; transformation arc; clear price or "from £X"; from $350
- Discovery call booking: Calendly; 30–60 min free; pre-call questionnaire (what are
  you working toward? what have you already tried?); auto-reply; from $250
- Case studies/testimonials: coaches can share full client names + companies with
  consent (unlike therapists); video testimonials; LinkedIn recommendation screenshots;
  specific transformation result ("promoted within 4 months" beats "helpful coach"); from $200
- Lead magnet: "5 steps to [outcome]" PDF or 5-day email challenge; Mailchimp or
  ConvertKit sequence (5–7 emails); from $300
- Group programme or cohort: WooCommerce or Kajabi; waitlist page; often higher-margin
  than 1:1; from $400
- Podcast / content hub: episode archive or YouTube embed; thought-leader positioning
- From $500 offer page + booking + lead magnet; $1,000+ with case studies + group
  programme + content hub

### 0d-pre38-c) Event planner / wedding planner / corporate events coordinator
Keywords: event planner website, wedding planner website, party planner website,
corporate events website, event management website, event coordinator website,
event organiser website, wedding coordinator website, wedding stylist website,
event styling website, luxury events website, corporate event planner website,
event production website, birthday party planner website

Response: distinct from venue (location) and photographer (images) — planner's
value is co-ordination and supplier relationships
- Service tiers page: Full planning (venue search → on-the-day) / Partial planning
  (main suppliers booked, I co-ordinate rest) / Day-of co-ordination (your plan,
  my execution) / Styling & décor only; clear scope per tier; sets expectations
  pre-enquiry; from $400
- Portfolio: event categories (weddings/corporate/birthday/charity/private dinner/
  away day); role callout per project (full/partial/day-of/styling); narrative +
  detail shots rather than a photography-style gallery; from $400
- Real event case studies: brief + challenge + planner's specific contribution +
  result; the planner's role IS the story; from $200/case study
- Enquiry form: event type + date + guest count + location + budget range + current
  stage; auto-CRM routing (Airtable or HoneyBook); from $250
- Supplier network page: preferred vendors (venues/caterers/florists/photographers/
  bands/AV/marquees); reciprocal links; signals established industry relationships
- Corporate events page: B2B tone; product launch/conference/gala/team away day;
  invoice and BACS; company name + objectives + budget form
- From $550 service tiers + portfolio + enquiry; $1,100+ with case studies + corporate
  page + supplier network

## QA results (34/34 all pass)

| Check | Result |
|-------|--------|
| _textBody div created | OK |
| pb-brain__text class on wrapper | OK |
| lines innerHTML inside textBody | OK |
| div.appendChild textBody | OK |
| _tbody query finds wrapper | OK |
| tbLines querySelectorAll line-spans | OK |
| collapses when more than 5 lines | OK |
| adds --collapsed class | OK |
| expand button created | OK |
| pb-brain__toggle class | OK |
| aria-expanded false initial | OK |
| toggle class on click | OK |
| show more text | OK |
| show less text | OK |
| aria-expanded updates | OK |
| scroll to bottom on expand | OK |
| toggle appended to div | OK |
| pb-brain__text CSS | OK |
| --collapsed max-height | OK |
| --collapsed gradient after | OK |
| pb-brain__toggle CSS | OK |
| toggle hover border | OK |
| dog groomer keywords | OK |
| iPET City Guilds qualification | OK |
| mobile grooming framing | OK |
| from $450 dog groomer | OK |
| life coach keywords | OK |
| ICF EMCC credentials | OK |
| target avatar specificity note | OK |
| from $500 life coach | OK |
| event planner keywords | OK |
| service tiers planner | OK |
| Airtable HoneyBook CRM | OK |
| from $550 event planner | OK |
