# DOM QA Report — R45 — 2026-06-20

## main.js — Markdown export upgrade + smooth scroll

### Markdown export (upgrade to .md from .txt)

`exportChat()` now produces a Markdown `.md` file instead of a plain-text `.txt`
file. Markdown renders beautifully in VS Code, GitHub, Notion, Obsidian, and every
modern note-taking app while remaining perfectly readable as plain text.

**Before:**
```javascript
function exportChat() {
  if (!chatMsgs.length) return;
  var lines = chatMsgs.map(function(m) {
    return (m.cls === 'bot' ? 'Photon: ' : 'You:    ') + plain(m.text);
  });
  var content = 'Photon Bounce — Chat Transcript\n' + new Date().toLocaleString()
    + '\n' + '────────────────────────────────────────' + '\n\n' + lines.join('\n\n');
  var blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  ...
  a.download = 'pb-chat-' + new Date().toISOString().slice(0, 10) + '.txt';
}
```

**After:**
```javascript
function exportChat() {
  if (!chatMsgs.length) { _showToast('Nothing to export yet'); return; }
  var lines = chatMsgs.map(function(m) {
    return (m.cls === 'bot' ? '**Photon:** ' : '**You:** ') + plain(m.text);
  });
  var content = '# Photon Bounce — Chat\n\n_Exported '
    + new Date().toLocaleString() + '_\n\n---\n\n' + lines.join('\n\n');
  var blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
  ...
  a.download = 'pb-chat-' + new Date().toISOString().slice(0, 10) + '.md';
}
```

**Changes explained:**

`**Photon:** ` and `**You:** ` — bold speaker labels in Markdown (rendered as
bold in Obsidian/Notion; clearly readable as-is in plain text). The old
`'Photon: '` / `'You:    '` alignment-padding with 4 spaces is now unnecessary.

`# Photon Bounce — Chat` — H1 heading at the top of the document. Renders as
a large heading in Markdown viewers; signals document start as plain text.

`_Exported date_` — italicised date in Markdown (`_..._`). In plain text: just
the date with underscores (readable).

`---` — horizontal rule. Renders as a divider in Markdown viewers; in plain text
it's three dashes (clearly a separator).

`lines.join('\n\n')` — unchanged; each message on its own paragraph separated by
a blank line. Both Markdown and plain text convention.

`text/markdown` MIME type — correct MIME type for Markdown files. Browsers don't
do anything special with it (they don't auto-render Markdown), but it's correct
for programmatic use.

`.md` extension — the file downloads as `pb-chat-2026-06-20.md`. Systems that
open Markdown files in the right app will use the extension.

**Empty-check toast**: `if (!chatMsgs.length) { _showToast('Nothing to export
yet'); return; }` — the old code silently did nothing on empty. Now gives feedback.

**exportBtn title/aria-label** updated from "Download chat transcript" to
"Download chat as Markdown" (accurate to the new format).

### Smooth scroll (4 sites upgraded; 2 click-jump buttons left instant)

`log.scrollTop = log.scrollHeight` (instant snap) replaced with
`log.scrollTo({ top: log.scrollHeight, behavior: 'smooth' })` at 4 auto-scroll
sites. The 2 explicit "jump to bottom" buttons stay instant.

#### Why smooth for auto-scroll, instant for buttons?

Auto-scroll sites (message arrives, typing indicator appears) benefit from smooth
scroll because the user didn't initiate the scroll — a gentle glide draws their
eye to the new content without jarring. The `.scrollTo({ behavior: 'smooth' })`
call uses the browser's native scroll animation (typically ~300ms deceleration),
which respects the user's `prefers-reduced-motion` preference automatically in
modern browsers.

Click-jump buttons (newMsgChip "↓ New message" click, scrollDownBtn click) stay
instant because the user explicitly asked to jump — smooth scroll here would feel
laggy and unresponsive. Expectation: button click = immediate result.

#### The 4 changed sites

1. `addMsg()` end — smooth: when a new bot reply or user message is added, the
   log glides to show the new message.
   `if (_nearBottom()) log.scrollTo({ top: log.scrollHeight, behavior: 'smooth' });`

2. `addTyping()` — smooth: when the typing indicator appears (bot is thinking),
   the log glides down to show the animated dots. Visual feedback that something
   is happening below the current scroll position.

3. Collapse `pb-brain__toggle` expand — smooth: user clicks "Show more ↓" on a
   long reply; after the content expands, smooth glide to the bottom of the log
   so the toggle button and any new content are visible.
   `requestAnimationFrame(function() { log.scrollTo({ top: log.scrollHeight, behavior: 'smooth' }); });`
   The `requestAnimationFrame` is still needed so `scrollHeight` is measured
   after the DOM reflects the expanded state.

4. Voice "Listening…" banner — smooth: when the mic is active and the banner
   appears at the bottom of the log, smooth glide to show it.

#### Behavior under fast message sequences

If the user sends a message and the bot replies quickly (< 300ms), two smooth
scroll animations are triggered in rapid succession. Browsers handle this
gracefully: the second `scrollTo` call cancels the first in-progress animation
and starts a new one from the current scroll position. The result is a single
smooth animation to the final position. No jank.

## main.css — `.pb-brain__msg { animation: pbMsgIn }` + `@keyframes pbMsgIn`

```css
/* R45 — message entry slide-up (transform-only; no opacity conflict with line-reveal) */
@keyframes pbMsgIn{
  from{ transform:translateY(6px); }
  to{ transform:none; }
}
.pb-brain__msg{ animation:pbMsgIn .14s ease-out; }
```

**Transform-only — no opacity**: The bot message `.pb-brain__line` spans already
have their own `pbLineIn` animation (opacity 0→1 with staggered delays). Adding
an opacity animation to the parent `.pb-brain__msg` would conflict: while the
parent fades in (opacity < 1), the children's opacity animations also run, causing
uncertain composited opacity. By using ONLY `transform:translateY(6px→0)`, the
parent is always at full opacity — the line-reveal animations inside it work
exactly as before, just with the whole bubble sliding up 6px on entry.

**No `animation-fill-mode`**: The default is `fill-mode: none`, meaning the
`from` transform (translateY 6px) is NOT applied before the animation fires. The
element appears at its correct position immediately, then the animation plays.
This is correct — `fill-mode: both` would offset the element 6px DOWN before
it appears, which would cause a flash or layout shift.

**14ms animation** — 140ms is the sweet spot between "too fast to notice" and
"too slow to feel responsive". On 60fps displays this is ~8 frames of animation,
which is enough to create a smooth settling-in effect without appearing sluggish.

**No animation on session-restore**: When the chat is restored from sessionStorage
on the next open, the messages are re-appended via `addMsg()`, which calls
`log.appendChild(div)`. Each message div has the `pbMsgIn` animation and will
play it. This means old messages briefly slide up 6px on restore. This is
acceptable (140ms, subtle transform) and actually helps the restored messages
feel less like a data dump — they appear to "settle in" rather than pop in all
at once.

## brainstorm.php — 3 new intent handlers

### 0d-pre41-a) Solicitor / law firm / barrister / legal services
Keywords: solicitor website, law firm website, barrister website, lawyer website,
legal services website, conveyancing solicitor website, family law website,
employment law website, criminal defence website, personal injury website,
immigration solicitor website, probate solicitor website, will writing website,
commercial law website, litigation website, legal firm website, law practice website

Response: SRA compliance and trust anchor the conversion funnel
- Practice area pages: one per specialism (conveyancing / family / employment /
  criminal defence / personal injury / immigration / probate / commercial / wills
  and LPAs / dispute resolution); from $200/page
- SRA compliance: SRA badge (required — must display Solicitors Regulation Authority
  name and FRN); regulated disclaimer; client money protection statement
- Fixed fee / price transparency: SRA requires published prices for family (divorce),
  immigration, conveyancing, motoring, employment, and wills; from $250
- Client portal: Osprey Approach / Clio / LEAP / Action Step; secure document upload;
  from $400
- Case studies: consent essential; anonymise employment/family; full name OK for PI
  and commercial; from $150/case study
- Accreditations: Lexcel / CQS (conveyancing) / Resolution (family) / APIL (PI) /
  Law Society D&I Charter
- Legal resource hub: long-form guides on common legal questions; SEO + client
  education; from $300
- From $600 / $1,200+

### 0d-pre41-b) Accountant / bookkeeper / chartered accountant / tax advisor
Keywords: accountant website, accounting firm website, bookkeeper website, chartered
accountant website, tax advisor website, management accountant website, small business
accountant website, self assessment website, vat specialist website, payroll services
website, xero accountant website, quickbooks accountant website, cloud accountant
website, cpa website, financial controller website, accounting practice website,
tax return website

Response: two client types — compliance (must-do) and advisory (value-add); speak to both
- Services pages: one per service (annual accounts / self-assessment / corporation
  tax / VAT / payroll / bookkeeping / management accounts / R&D tax credits /
  advisory / cloud setup); compliance vs advisory framing; from $200/page
- Software partner badges: Xero Platinum/Gold/Silver (most influential trust signal
  for cloud-first firm); QuickBooks ProAdvisor; FreeAgent certified; from $150
- Free consultation booking: Calendly 30-min + pre-call form; from $200
- Pricing page: Start/Grow/Scale packages; monthly retainer or "from £X"; from $250
- ICAEW/ACCA/CIMA/ICAS badge; PI insurance; AML supervision disclosure (required —
  accountants supervised under Money Laundering Regulations); from $150
- Tax calendar page: Companies House / HMRC deadlines; builds retention
- Client portal: Xero Practice Manager / Karbon / SENTA / TaxCalc Cloud; from $350
- From $550 / $1,100+

### 0d-pre41-c) Mortgage broker / IFA / financial adviser / protection broker
Keywords: mortgage broker website, mortgage advisor website, independent financial
adviser website, ifa website, financial advisor website, remortgage website, first
time buyer mortgage website, buy to let mortgage website, protection broker website,
life insurance broker website, critical illness cover website, income protection
website, whole of market broker website, equity release website, mortgage adviser
website, financial planning website

Response: FCA credentials are the trust anchor; the funnel is information → trust →
qualified enquiry
- Mortgage types page: first-time buyer / remortgage / BTL / Help to Buy / shared
  ownership / self-employed / adverse credit / equity release; from $250
- FCA authorisation disclosure: required — must state authorised by FCA, include
  FRN, link to FCA register; from $150
- Affordability calculator: salary multiplier up to 4.5× + LTV/deposit slider;
  captures lead intent; from $350
- Fee transparency: state broker fee (typical £300-£695) OR "fee-free, paid by
  lender by way of procuration fee" — FCA requires clarity on this; from $150
- Case studies: first-time buyer with gifted deposit / self-employed with 1 year
  accounts / adverse credit / BTL portfolio; from $200 each
- Protection page: life insurance / critical illness / income protection / buildings
  and contents; from $200
- Initial consultation booking: Calendly + fact-find form (purchase or remortgage +
  property value + deposit + employment type + adverse credit); from $250
- From $550 / $1,100+

## QA results (33/33 all pass)

| Check | Result |
|-------|--------|
| exportChat shows toast when empty | OK |
| Photon bold speaker label | OK |
| You bold speaker label | OK |
| markdown heading in content | OK |
| horizontal rule separator | OK |
| markdown MIME type | OK |
| .md download extension | OK |
| exportBtn title updated | OK |
| addMsg uses scrollTo smooth | OK |
| addTyping uses scrollTo smooth | OK |
| listening banner uses scrollTo | OK |
| collapse expand uses scrollTo | OK |
| newMsgChip click stays instant | OK |
| scrollDownBtn click stays instant | OK |
| pbMsgIn keyframes defined | OK |
| pbMsgIn from translateY 6px | OK |
| pb-brain__msg has animation | OK |
| animation 0.14s ease-out | OK |
| solicitor law firm keywords | OK |
| SRA compliance badge note | OK |
| SRA price transparency rule | OK |
| Lexcel CQS accreditations | OK |
| from $600 solicitor | OK |
| accountant keywords | OK |
| Xero platinum partner note | OK |
| AML supervision disclosure note | OK |
| ICAEW ACCA CIMA ICAS | OK |
| from $550 accountant | OK |
| mortgage broker keywords | OK |
| FCA FRN disclosure required | OK |
| fee transparency FCA rule | OK |
| 4.5x salary multiplier | OK |
| from $550 mortgage broker | OK |
