# DOM QA Report — R54 — 2026-06-20

## main.js — Session statistics panel (📊)

### Feature overview

A 📊 button added to the chatbot header opens a compact overlay panel showing live session
statistics: user message count, total bot words received, average response time, and elapsed
session time. The panel updates every time it's opened so figures are always current.

### Tracking variables (declared at widget init alongside `_lastSendTime`)

```javascript
var _statsSessionStart = Date.now();  // when the widget initialised
var _statsRespTimes = [];             // response times in ms per bot reply
var _statsBotWords = 0;              // cumulative bot words received this session
```

**Why three separate vars rather than one object**: each is accessed from a different
location in `addMsg()` (response-time block, word-count block) and the render function.
Flat vars avoid property-access overhead and keep each increment readable in context.

### Response-time capture (in `addMsg()`, response-time badge block)

```javascript
// Before (computed Date.now() - _lastSendTime twice):
_rtEl.textContent = '⚡ ' + ((Date.now() - _lastSendTime) / 1000).toFixed(1) + 's';

// After:
var _rtMs = Date.now() - _lastSendTime;
_statsRespTimes.push(_rtMs);          // ← push BEFORE clearing _lastSendTime
var _rtEl = document.createElement('small');
_rtEl.className = 'pb-brain__resp-time';
_rtEl.textContent = '⚡ ' + (_rtMs / 1000).toFixed(1) + 's';
div.appendChild(_rtEl);
_lastSendTime = 0;
```

**Extracted `_rtMs`**: by computing the elapsed time once and storing it, the badge
text and the stats array share the same value. Without the extraction, the two
`Date.now()` calls could differ by a millisecond (e.g. 1499ms vs 1500ms) producing
a rounding discrepancy between the badge and the average.

**Push before clear**: `_statsRespTimes.push(_rtMs)` runs before `_lastSendTime = 0`.
If the order were reversed, `_rtMs` would be negative (Date.now() - 0 = current epoch).
The `< 30000` guard on the outer `if` ensures outliers (e.g. a 45s slow API response)
don't skew the average — these are shown as the special case badge but not accumulated.

### Bot word accumulator (in `addMsg()`, word-count block)

```javascript
var _wcWords = plain(text).trim().split(/\s+/).filter(Boolean).length;
_statsBotWords += _wcWords;   // ← accumulate BEFORE the > 0 badge guard
if (_wcWords > 0) { ...badge... }
```

**Accumulate unconditionally** (before the `_wcWords > 0` guard): a bot message with
0 words is an edge case that shouldn't reset or skip, but adding 0 is a no-op so the
order doesn't actually matter for correctness — it just reads more clearly before the
conditional.

### Stats button and panel (in the header section)

```javascript
var _statsBtn = document.createElement('button');
_statsBtn.type = 'button'; _statsBtn.className = 'pb-brain__stats-btn';
_statsBtn.title = 'Session stats'; _statsBtn.setAttribute('aria-label', 'Session stats');
_statsBtn.setAttribute('aria-expanded', 'false'); _statsBtn.innerHTML = '&#128202;';

var _statsPanel = document.createElement('div');
_statsPanel.className = 'pb-brain__stats-panel';
_statsPanel.setAttribute('role', 'status');

var _statsOpen = false;
var _renderStats = function() {
  var _elapsed = Math.floor((Date.now() - _statsSessionStart) / 1000);
  var _mins = Math.floor(_elapsed / 60), _secs = _elapsed % 60;
  var _sessionTime = _mins > 0 ? _mins + 'm ' + _secs + 's' : _secs + 's';
  var _uCount = chatMsgs.filter(function(m) { return m.cls === 'user'; }).length;
  var _avgResp = _statsRespTimes.length > 0
    ? (_statsRespTimes.reduce(function(a, b) { return a + b; }, 0) / _statsRespTimes.length / 1000).toFixed(1) + 's'
    : '—';
  _statsPanel.innerHTML = [
    '<strong>&#128202; Session stats</strong>',
    '<span>Your messages <b>' + _uCount + '</b></span>',
    '<span>Bot words&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<b>' + _statsBotWords.toLocaleString() + '</b></span>',
    '<span>Avg response&nbsp;<b>' + _avgResp + '</b></span>',
    '<span>Session time&nbsp;<b>' + _sessionTime + '</b></span>',
  ].join('');
};
```

**`_renderStats` called on every open**: the panel is rendered fresh each time `_statsBtn`
is clicked (not on every message), so the numbers are always up-to-date at the moment
of viewing. Rendering on every message would be wasted work — users rarely have the panel
open during a conversation.

**`_statsRespTimes.reduce(a, b) => a + b`**: sum of all response times, divided by
length, divided by 1000 = average in seconds. `toFixed(1)` gives one decimal place.
If no responses yet, shows `—` rather than `0.0s` or `NaN` which would look like a bug.

**`_statsBotWords.toLocaleString()`**: uses locale-aware number formatting so `1234`
displays as `1,234` in English locales. Cosmetic but makes large word counts readable.

**`chatMsgs.filter(m => m.cls === 'user').length`**: message count from `chatMsgs` array
rather than a counter var — this is the source of truth; using a counter would drift if
messages are deleted (e.g. via `clearChat()`).

**`'status'` role**: `_statsPanel.setAttribute('role', 'status')` announces the panel
content to screen readers when it appears, without demanding immediate focus (which
`role="alert"` would do). Appropriate for non-urgent informational updates.

**Outside-click close**:
```javascript
document.addEventListener('click', function(e) {
  if (_statsOpen && !_statsPanel.contains(e.target) && e.target !== _statsBtn) {
    _statsOpen = false;
    _statsPanel.classList.remove('pb-brain__stats-panel--open');
    _statsBtn.setAttribute('aria-expanded', 'false');
  }
});
```

**`!_statsPanel.contains(e.target) && e.target !== _statsBtn`**: the double guard is
necessary. `contains()` returns false for the button itself (since button is not inside
the panel), so without the second condition, clicking the button would both open AND
immediately close the panel.

**Button placement**: `_statsBtn` added after `_sfBtn` (star filter) and before
`collapseBtn` in both the `closeBtn` and `else` branches of the button-group
construction, keeping the visual order: ↺ new-chat → ↓ export → 🖨 print → ★ filter
→ 📊 stats → − collapse → 🔇 mute → ? help.

## main.css — Stats panel styles

```css
.pb-brain__stats-btn{
  background:none; border:none; cursor:pointer;
  padding:2px 4px; font-size:14px; line-height:1;
  color:rgba(255,255,255,.55); transition:color .15s;
}
.pb-brain__stats-btn:hover{ color:rgba(255,212,0,.85); }
.pb-brain__stats-panel{
  position:absolute; top:100%; right:8px;
  background:rgba(15,15,24,.96);
  border:1px solid rgba(255,255,255,.12);
  border-radius:10px; padding:12px 16px;
  font-size:12px; line-height:1.9;
  color:rgba(255,255,255,.75);
  min-width:190px;
  box-shadow:0 8px 24px rgba(0,0,0,.5);
  display:none; z-index:120;
}
.pb-brain__stats-panel--open{ display:block; }
.pb-brain__stats-panel strong{
  display:block; color:#ffd400; margin-bottom:6px;
  font-size:12.5px; letter-spacing:.01em;
}
.pb-brain__stats-panel span{
  display:flex; justify-content:space-between; gap:12px;
}
.pb-brain__stats-panel b{
  color:rgba(255,255,255,.9); font-weight:600;
}
```

**`position:absolute; top:100%; right:8px`**: the panel drops below the header
(which has `position:relative` or `position:sticky`) and right-aligns with the button
group. `right:8px` gives a small inset from the edge.

**`display:none` / `display:block`** toggle via class: simpler than `opacity` + pointer-
events approach because the panel doesn't need an entrance animation (it opens in-place
below the button). If an animation were added later, it could be done with
`@keyframes` on the `--open` state.

**`line-height:1.9`**: the stats rows each have a `<span>` with `display:flex` that
provides the left/right columns — but `line-height:1.9` on the parent provides the
vertical spacing between rows without needing explicit margin on each span.

**`z-index:120`**: above the scrollbar, floating chips, and most overlays in the chat
widget (which typically sit at z-index < 100), but below any modal or overlay that might
exist in the outer page.

## brainstorm.php — 3 new intent handlers

### 0d-pre50-a) Accountant / bookkeeper / chartered accountant / tax advisor
Keywords: accountant website, chartered accountant website, bookkeeper website, accounting
firm website, tax advisor website, tax accountant website, small business accountant website,
self employed accountant website, cpa website, payroll website, management accountant
website, accountancy practice website, financial accountant website, vat accountant website,
company accounts website, annual accounts website

Response: trust + clarity about who you serve; generalist messaging converts poorly
- Niche homepage messaging: "specialist accountants for landlords" > generic; from $200
- Services pages: self-assessment, limited company, VAT, payroll, bookkeeping, mgmt
  accounts, R&D credits, CIS, MTD, HMRC investigations; from $150/page
- Fixed-fee pricing page: single biggest conversion lever; tiered guide (sole trader /
  limited co / SME); from $200
- ICAEW/ACCA/CIMA registration: professional body logo; regulated vs unregulated
  distinction matters; from $100
- Making Tax Digital page: MTD is legally required for VAT; MTD for Income Tax coming;
  anxious Google searches convert to enquiries; from $150
- Onboarding process page: step-by-step; Xero/QuickBooks/Sage/FreeAgent partner logos;
  from $200
- Software partner logos: Xero Silver/Gold/Platinum; QuickBooks ProAdvisor; FreeAgent
  Partner; from $100
- Tax deadline calendar / blog: evergreen, high-traffic; funnels to contact form; from $100/post
- Testimonials with business context: from $100
- From $500 / $1,000+

### 0d-pre50-b) Solicitor / law firm / conveyancer / family lawyer
Keywords: solicitor website, law firm website, legal website, conveyancer website,
conveyancing website, family lawyer website, family law website, employment lawyer website,
immigration lawyer website, personal injury lawyer website, commercial lawyer website,
property lawyer website, will writer website, legal services website, barrister website,
criminal lawyer website

Response: authority + accessibility; clients may be frightened or in dispute
- Practice area pages: genuinely informative 1,000-word pages convert vs 100-word lists;
  family law, conveyancing, wills/probate, employment, PI, commercial, immigration,
  criminal defence; from $200/page
- SRA/Law Society registration: SRA number in footer every page; legally required;
  verification link to SRA register; from $100
- Initial consultation booking: form (matter type + brief + preferred contact);
  Calendly for lawyer own availability; from $200
- Fixed-fee transparency page: SRA mandates prices published for 6 areas (conveyancing,
  probate, employment tribunal, motoring, immigration, debt recovery); compliance +
  conversion; from $250
- Team/lawyer profiles: name, photo, year of qualification, specialisms, languages,
  memberships (Resolution, ACTAPS); clients choose lawyer as much as firm; from $150
- Testimonials and case studies: carefully worded (no outcome promises); anonymous
  case studies; from $150
- Client guides/FAQs: "How long does conveyancing take?"; "What is the divorce process?";
  ranks for high-intent searches; from $150
- Legal jargon glossary: plain English; ranks for long-tail; from $100
- Complaints procedure: legally required; professional handling increases confidence; from $50
- From $600 / $1,400+

### 0d-pre50-c) Estate agent / letting agent / property management company
Keywords: estate agent website, letting agent website, property agent website, estate
agency website, letting agency website, property management website, property company
website, real estate agent website, landlord service website, property management company
website, buy to let website, holiday let website, short term rental website, property
investment website, sales and lettings website, residential property website

Response: two audiences (buyers/tenants + vendors/landlords); most agency sites fail both
- Property search/listings: filtering by bedrooms/price/area/type; CRM feed integration
  (Rightmove/Zoopla); creates reason to return; from $400
- Valuation landing page: "What is my home worth?" most searched UK property phrase;
  instant online valuation (Land Registry API) or "Book a free valuation" form; from $250
- Landlord services page: full mgmt vs rent-only vs tenant-find; guaranteed rent; HMO;
  fee schedules; EPC/gas safe/EICR/Right to Rent compliance; from $200
- Why choose us: avg days to sell/let; % asking price achieved; Google rating; from $150
- ARLA/NAEA Propertymark + Property Ombudsman/Redress Scheme (legally required for
  letting agents in England); client money protection scheme; from $100
- Area guides: per area; local amenities, prices, transport, schools; ranks for
  "houses for sale in [area]"; from $200/area
- Staff profiles: negotiators/managers with photos; vendors instruct the person; from $150
- Google reviews feed embedded; from $150
- Market update blog: "[Area] property market report Q1 2025"; from $150/post
- From $600 / $1,400+

## QA results (39/42 auto + 3 manual = 42/42 all pass)

| Check | Result |
|-------|--------|
| _statsSessionStart declared | OK |
| _statsRespTimes array | OK |
| _statsBotWords counter | OK |
| _rtMs extracted before clear | OK |
| _statsRespTimes.push(_rtMs) | OK |
| response badge text uses _rtMs | OK |
| _statsBotWords += _wcWords | OK |
| accumulator before badge condition | OK |
| _statsBtn created | OK |
| _statsPanel created | OK |
| _renderStats function | OK |
| session time from _statsSessionStart | OK |
| avg resp uses _statsRespTimes.reduce | OK |
| uCount from chatMsgs.filter | OK |
| _statsBotWords.toLocaleString in panel (manual) | OK — at dist 770, needle 30 chars, ends at 800 = window boundary; code confirmed |
| click toggles panel class | OK |
| outside-click collapses panel (manual) | OK — 'false' at dist 217, ends at 224 > window 220; code confirmed |
| _statsPanel appended to brainHead | OK |
| _statsBtn added to closeBtn branch | OK |
| _statsBtn added to else branch | OK |
| .pb-brain__stats-btn block | OK |
| stats-btn hover gold (manual) | OK — rgba at dist 34, ends at 52 > window 50; code confirmed |
| .pb-brain__stats-panel block | OK |
| panel absolute position | OK |
| panel display:none | OK |
| .pb-brain__stats-panel--open display:block | OK |
| panel strong gold | OK |
| panel span flex row | OK |
| accountant and chartered accountant keywords | OK |
| ICAEW ACCA CIMA credentials | OK |
| MTD Making Tax Digital page | OK |
| Xero QuickBooks software logos | OK |
| accountant price line | OK |
| solicitor and law firm keywords | OK |
| SRA regulation number required | OK |
| SRA price transparency for 6 areas | OK |
| solicitor price line | OK |
| estate agent and letting agent keywords | OK |
| ARLA NAEA Propertymark logos | OK |
| Property Redress Scheme legally required | OK |
| valuation page described | OK |
| estate agent price line | OK |
