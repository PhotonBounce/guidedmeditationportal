# DOM QA Report — R40 — 2026-06-20

## main.js — Input message history navigation (↑/↓ terminal-style)

Replaces the previous `Ctrl+↑` single-message restore with full multi-message
history navigation. Pressing `↑` on an empty input field (or when the cursor is
not in the middle of typed content) cycles backward through all previously sent
messages. `↓` cycles forward. Pressing `↓` past the newest message exits history
mode and returns to an empty input. Standard behavior in terminal emulators,
IRC clients, and most chat apps.

### What was there before (removed)

```javascript
// Ctrl+↑ on empty input — restore last sent message (edit-last pattern).
input.addEventListener('keydown', function(e) {
  if (e.ctrlKey && e.key === 'ArrowUp' && !input.value.trim()) {
    var _last = null;
    for (var _li = chatMsgs.length - 1; _li >= 0; _li--) {
      if (chatMsgs[_li].cls === 'user') { _last = chatMsgs[_li]; break; }
    }
    if (_last) {
      e.preventDefault();
      input.value = _last.text;
      input.dispatchEvent(new Event('input'));
      input.selectionStart = input.selectionEnd = input.value.length;
    }
  }
});
```

The old handler only ever restored the single most-recent user message. It
required `Ctrl` which is not ergonomic and not discoverable.

### New implementation

```javascript
// ↑/↓ history nav — terminal-style cycling through sent messages on empty input.
// ↑ enters history mode (going oldest first); ↓ exits back toward present.
// Multiline input (Shift+Enter newlines) is left alone so caret can move between lines.
var _histIdx = -1;
input.addEventListener('keydown', function(e) {
  if (e.key === 'ArrowUp') {
    if (input.value.indexOf('\n') !== -1) return;        // multiline: let textarea handle
    if (input.value.trim() !== '' && _histIdx === -1) return; // typed content: don't clobber
    var _hm = chatMsgs.filter(function(m) { return m.cls === 'user'; });
    if (!_hm.length) return;
    e.preventDefault();
    if (_histIdx === -1) _histIdx = _hm.length;          // start past newest
    _histIdx = Math.max(0, _histIdx - 1);
    input.value = _hm[_histIdx].text;
    input.dispatchEvent(new Event('input'));
    input.selectionStart = input.selectionEnd = input.value.length;
  } else if (e.key === 'ArrowDown' && _histIdx !== -1) {
    var _hm = chatMsgs.filter(function(m) { return m.cls === 'user'; });
    e.preventDefault();
    _histIdx++;
    if (_histIdx >= _hm.length) { _histIdx = -1; input.value = ''; input.dispatchEvent(new Event('input')); }
    else { input.value = _hm[_histIdx].text; input.dispatchEvent(new Event('input')); input.selectionStart = input.selectionEnd = input.value.length; }
  }
});
```

**`var _histIdx = -1`** — function-scoped (before the listener). `-1` = not in
history mode; `0` to `msgs.length-1` = index into user-sent messages.

**Multiline guard**: `if (input.value.indexOf('\n') !== -1) return` — if the
user used Shift+Enter to write a multi-line message, `↑` should move the caret
between lines (native textarea behavior), not enter history mode. This check
skips history entirely when the textarea has any newline.

**Content guard**: `if (input.value.trim() !== '' && _histIdx === -1) return` —
if the user has typed content and is NOT already in history mode, don't clobber
it. This lets the user continue naturally typing without accidentally replacing
their draft. Once in history mode (`_histIdx !== -1`), `↑` navigates normally
(the input already contains a history entry, so overwriting it is expected).

**Entry into history**: `if (_histIdx === -1) _histIdx = _hm.length` sets the
index to one past the newest message (virtual "blank slot"), then `Math.max(0,
_histIdx - 1)` decrements — so the first `↑` goes to the newest message
(`_hm.length - 1`), second `↑` goes to the one before it, and so on. Clamped
at `0` so you can't go before the oldest message.

**Exit via `↓`**: when `_histIdx >= _hm.length`, history is over — `_histIdx`
resets to `-1` and input clears (`value = ''`). This mirrors the terminal
convention of `↓` past the newest entry restoring an empty prompt.

**Reset on send**: `_histIdx = -1` added immediately after `input.value = ''`
in the form submit handler. Sending a message always exits history mode.

**`dispatchEvent(new Event('input'))`** on every change — triggers the typing
hint CSS animation logic and the auto-resize handler without duplicating code.

**`selectionStart = selectionEnd = input.value.length`** — places the cursor
at the end of the restored message (standard behavior).

## No CSS changes in R40 — behaviour-only round.

## brainstorm.php — 3 new intent handlers

### 0d-pre36-a) Estate agent / lettings agent / property management company
Keywords: estate agent website, lettings agent website, property management website,
estate agency website, letting agent website, property agent website, property developer
website, new homes website, residential sales website, commercial property website,
property sales website, landlord website, block management website, HMO management website

Response: conversion target is vendors (sell) and landlords (let) — not just browsers
- Property search and listings: WP-Property / Easy Property Listings / custom CPT;
  search by price/beds/type/area; saved searches with email alerts; floor plan +
  virtual tour embed; Rightmove/Zoopla feed integration (CRM-dependent); from $600
- Valuation lead capture: "Get a free valuation" is the highest-value conversion;
  instant online estimate (Property Data API / Hometrack) + in-person booking
  (Calendly); Gravity Forms with postcode lookup; from $350
- Landlord and vendor pages: separate per audience; landlord = yield + compliance
  (EICR / EPC / gas safety / HMO licence) + tenant-find vs full-management tiers;
  vendor = sales process timeline + fees; from $300
- Tenant portal / repair request: maintenance ticket form + tenancy document access
  (AST / deposit certificate / inventory); Arthur or Fixflo integration; from $300
- Branch pages: one per office; Local Business schema; area guides ("best streets in
  [town]" long-form); school catchment links; commute times; local SEO
- CRM integration: Reapit / Jupix / Dezrez / Alto (Zoopla) / Property Hive (WP plugin)
- From $700 listings + valuation + branch; $1,400+ with portal + CRM feed + area guides

### 0d-pre36-b) Dance studio / dance school / ballet school / performing arts academy
Keywords: dance studio website, dance school website, ballet school website, performing
arts website, dance academy website, dance lessons website, ballet website, ballroom
dancing website, contemporary dance website, tap dance website, street dance website,
salsa dance website, dance class website, latin dance website, Irish dance website,
cheerleading website

Response:
- Class timetable: WP Amelia or The Booking Factory; filter by age/style/level/day;
  capacity per slot; add-to-Google-Calendar link; from $400
- Online enrolment + direct debit: WooCommerce Subscriptions for monthly class packages;
  GoCardless for DD (UK) or Stripe recurring; trial class booking with deposit; from $400
- Style pages: one per discipline (ballet/tap/jazz/contemporary/hip-hop/ballroom/Latin/
  Irish/acro/street); level ladder from beginner to performance; from $200/page
- Show and performance archive: annual show gallery; video reel; student testimonials
- Exam and festival results: ISTD / RAD / IDTA exam grades; festival trophy results;
  competitive credentials are a major purchase signal for competitive families
- Teacher profiles: headshots + training background + performance history + qualifications
- Uniform/merchandise shop: WooCommerce; branded kit + show costumes; from $300
- Safeguarding and DBS: enhanced DBS note; safeguarding policy PDF; GDPR media consent;
  required for any studio with under-18 students (not optional)
- From $600 timetable + enrolment + style pages; $1,100+ with show archive + shop

### 0d-pre36-c) Wedding photographer / portrait photographer / commercial photographer
Keywords: wedding photographer website, wedding photography website, event photographer
website, portrait photographer website, commercial photographer website, photographer
portfolio website, photography portfolio website, headshot photographer website, family
photographer website, newborn photographer website, boudoir photographer website,
product photographer website, food photographer website, brand photographer website

Response: portfolio-first but must convert enquiries — gallery weight vs CTA visibility
- Portfolio / gallery: Justified Image Grid or Modula; WebP + lazy load; full-screen
  lightbox; curated (30–50 images, not the full archive); load time IS conversion time;
  from $400
- Enquiry + booking flow: Gravity Forms or HoneyBook / Studio Ninja integration; wedding
  form: date + venue + guest count + how they found you; portrait: session type +
  preferred date; auto-reply with pricing guide PDF; from $300
- Real wedding / real shoot pages: one per featured commission; venue + suppliers +
  narrative + 15–20 select images; each targets "[venue name] wedding photographer"
  keyword; from $150/page
- Packages and pricing: transparent or enquiry-only; wedding: coverage hours + album +
  second shooter; portrait: duration + digital files + prints
- Testimonials: Google Reviews widget; one pull quote per gallery section; video
  testimonials where available
- Style and approach page: documentary vs posed; natural light vs studio; editing style
  (film emulation / moody / bright + airy); helps self-select clients
- Location SEO: "[city] wedding photographer" landing pages; "[venue] wedding photography"
  from real-shoot pages; from $100/page
- From $500 portfolio + enquiry + packages; $1,000+ with real-shoot pages + location SEO

## QA results (23/25 pattern + 2 manual = 25/25 correct)

| Check | Result |
|-------|--------|
| _histIdx var declared | OK |
| ArrowUp handler | OK |
| ArrowDown handler | OK |
| multiline guard indexOf newline (manual) | OK — line 385: `indexOf('\n') !== -1) return;` confirmed; QA regex false-negative (pattern `\\n` in regex vs literal `\n` in source) |
| enters history mode at msgs.length | OK |
| clamps to 0 with Math.max | OK |
| ArrowDown exits at msgs.length | OK |
| histIdx reset on submit | OK |
| userMsgs filtered from chatMsgs | OK |
| selectionEnd set to value.length | OK |
| estate agent keywords | OK |
| Rightmove Zoopla feed | OK |
| valuation lead capture | OK |
| Reapit Jupix Dezrez Alto | OK |
| from $700 estate agent | OK |
| dance studio keywords | OK |
| GoCardless direct debit | OK |
| ISTD RAD IDTA exams | OK |
| safeguarding DBS notice | OK |
| from $600 dance studio | OK |
| photographer keywords | OK |
| HoneyBook Studio Ninja | OK |
| real shoot pages (manual) | OK — PHP line confirmed; QA pattern false-negative (case-sensitive: `real wedding` vs `Real wedding`) |
| location SEO pages | OK |
| from $500 photographer | OK |
