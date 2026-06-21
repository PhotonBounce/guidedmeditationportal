# DOM QA Report — R47 — 2026-06-20

## main.js — Code block rendering + live message-count badge

### Code block rendering in `format()`

`format()` now handles both fenced code blocks (` ``` `) and inline code (`` ` ``),
extracting them before HTML-escaping so their content is preserved verbatim.

#### Why placeholder extraction before HTML-escape?

The existing `format()` pipeline starts with `.replace(/&/g, '&amp;').replace(/</g,
'&lt;').replace(/>/g, '&gt;')`. If a code block containing `<div>` or `&nbsp;` were
processed through this step, the code content would be double-escaped (e.g.
`&lt;div&gt;` becomes `&amp;lt;div&amp;gt;`). The placeholder approach extracts code
regions first, assigns them safe escaped HTML, substitutes a null-safe placeholder
string, lets the rest of the pipeline run (which won't touch the placeholder — it
contains no HTML-significant characters), then restores the pre-built HTML at the end.

```javascript
var _codeBlocks = [], _inlineCodes = [];
text = text.replace(/```[\w]*\n?([\s\S]*?)```/g, function(_, code) {
  var safe = code.trim().replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  _codeBlocks.push('<pre class="pb-brain__code-block"><code>' + safe + '</code></pre>');
  return '\x00CB' + (_codeBlocks.length - 1) + '\x00';
});
text = text.replace(/`([^`\n]+)`/g, function(_, code) {
  var safe = code.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  _inlineCodes.push('<code class="pb-brain__code-inline">' + safe + '</code>');
  return '\x00IC' + (_inlineCodes.length - 1) + '\x00';
});
// ... (existing escape + markdown pipeline) ...
_codeBlocks.forEach(function(h, i) { text = text.replace('\x00CB' + i + '\x00', h); });
_inlineCodes.forEach(function(h, i) { text = text.replace('\x00IC' + i + '\x00', h); });
```

**`[\w]*` after ` ``` `**: matches optional language hint (` ```javascript `, ` ```php `)
and ignores it for rendering (no syntax highlighting at this stage, just a styled block).

**Fenced blocks use `[\s\S]*?`**: non-greedy multiline match so multiple code blocks in
one message each match the nearest closing ` ``` ` rather than spanning across all blocks.

**Inline code uses `[^`\n]+`**: excludes backticks and newlines from inline code so
multi-line ` ``` ` blocks don't get greedily matched as inline.

**`\x00` null byte as separator**: `\x00CB0\x00` etc. are placeholder strings that
contain characters no real user message would contain, so replacement is collision-safe.
The `\x00` characters survive the HTML-escape and bold/link replacements unchanged (none
of those patterns touch null bytes).

### Code block line rendering in `addMsg()`

The existing `_fmtLines.map(...).join('<br>')` approach was replaced with a `forEach`
that inspects each line:

```javascript
var _lh = '';
_fmtLines.forEach(function(ln, i) {
  var isBlk = ln.trimStart().startsWith('<pre');
  if (!isBlk && i > 0 && !_fmtLines[i-1].trimStart().startsWith('<pre')) _lh += '<br>';
  _lh += isBlk
    ? '<div class="pb-brain__line pb-brain__line--code">' + ln + '</div>'
    : '<span class="pb-brain__line" style="animation-delay:' + (i * 70) + 'ms">' + ln + '</span>';
});
_textBody.innerHTML = _lh;
```

**Why `div` not `span` for code blocks**: `<pre>` is a block-level element. Placing a
block element inside a `<span>` (inline element) is invalid HTML and produces inconsistent
rendering across browsers. A `<div.pb-brain__line--code>` is a block container that
correctly wraps the `<pre>`.

**No animation on code blocks**: the `animation-delay` is omitted on code block lines.
The staggered line-reveal animation (`pbLineIn`) works well for short prose lines (70ms
per line), but a code block is a self-contained chunk — animating it as one of many
lines at 70ms × N delay would make it appear confusingly late after long prose responses.
Code blocks appear immediately; prose lines above them still animate in.

**No `<br>` adjacent to code block `<div>`**: block elements create their own line
breaks. A `<br>` before or after a `<div>` would create an extra blank line. The
condition `if (!isBlk && i > 0 && !_fmtLines[i-1].trimStart().startsWith('<pre'))` adds
`<br>` only between two non-code lines.

### Live message-count badge

A `<span class="pb-brain__msg-count">` is inserted after the `<h3>` title in
`.pb-brain__head`. It shows `"N message(s)"` and updates on every `addMsg()` call and
on `clearChat()`.

```javascript
var _mcEl = null;  // declared at top of IIFE (before addMsg is defined)

function _updateMsgCount() {
  if (!_mcEl) return;  // no-op until brainHead setup assigns _mcEl
  var n = chatMsgs.length;
  _mcEl.textContent = n > 0 ? n + (n === 1 ? ' message' : ' messages') : '';
}
```

**Why a function declaration**: `_updateMsgCount` is called from `addMsg()` and
`clearChat()` which are defined EARLIER in the IIFE than the brainHead setup block
(where `_mcEl` is assigned). Function declarations are hoisted in JavaScript — they're
available throughout the entire function scope regardless of where they appear in the
source. The `if (!_mcEl) return` guard handles the early phase (before DOM setup) when
`_mcEl` is `null`, making calls from `restoreChat()` safely no-ops.

**`chatMsgs.length` as source of truth**: `chatMsgs` tracks all messages added via
`addMsg()` (excluding the initial HTML-template greeting, which is intentional — the
badge counts the session conversation, not the static welcome message). After session
restore, `chatMsgs` reflects all restored messages; after `clearChat()`, it's reset to
0 and the badge clears.

**Empty state**: when `n === 0`, `_mcEl.textContent = ''` — the badge is visually empty.
It becomes visible once the first real message is exchanged. This avoids a confusing
`"0 messages"` on fresh sessions.

## main.css — Message count + code block styles

```css
.pb-brain__msg-count{
  display:block; font-size:10px; color:rgba(255,255,255,.28);
  letter-spacing:.4px; margin-top:1px; min-height:13px;
  transition:opacity .3s;
}
.pb-brain__code-inline{
  font-family:ui-monospace,'Cascadia Code',Consolas,'Courier New',monospace;
  font-size:.85em; padding:1px 5px; border-radius:3px;
  background:rgba(255,212,0,.1); color:rgba(255,212,0,.92);
  border:1px solid rgba(255,212,0,.2); white-space:pre;
}
pre.pb-brain__code-block{
  font-family:ui-monospace,'Cascadia Code',Consolas,'Courier New',monospace;
  font-size:.78em; line-height:1.55; margin:6px 0 4px; padding:10px 12px;
  background:rgba(0,0,0,.35); border:1px solid rgba(255,255,255,.08);
  border-radius:6px; overflow-x:auto; white-space:pre;
  color:rgba(255,255,255,.88);
}
pre.pb-brain__code-block code{ font-size:inherit; background:none; border:none; padding:0; }
.pb-brain__line--code{ display:block; }
```

- `ui-monospace` — modern CSS system font stack starting with the OS's default monospace
  UI font (SF Mono on macOS, Cascadia Code on Windows 11). Falls back through Consolas
  (Windows), Courier New.
- Code inline: gold tint (`rgba(255,212,0,.92)`) matches the existing gold accent family
  and makes inline code visually distinct from surrounding prose without requiring a
  separate colour.
- Code block: dark glass (`rgba(0,0,0,.35)`) — darker than the message bubble backgrounds,
  creating clear visual separation. `overflow-x:auto` so wide code blocks scroll
  horizontally rather than overflowing the chat bubble.
- `.pb-brain__line--code{ display:block }` — ensures the `div` wrapper respects block
  flow even if parent CSS applies `inline` or `inline-block` to `.pb-brain__line`.

## brainstorm.php — 3 new intent handlers

### 0d-pre43-a) Vet clinic / veterinary practice / animal hospital website
Keywords: vet website, vet clinic website, veterinary practice website, veterinary clinic
website, animal hospital website, pet care website, veterinary surgery website, small
animal vet website, exotic vet website, emergency vet website, referral vet website,
equine vet website, farm vet website, cat clinic website, dog clinic website, rabbit vet website

Response: RCVS registration + OOH cover are the two conversion anchors
- Services pages: vaccinations / neutering / dental / surgery / diagnostics /
  physiotherapy / hydrotherapy / acupuncture / oncology / ophthalmology; species tabs
  where multi-species; from $200/page
- Online booking: VetDesk / RxWorks / Provet Cloud; routine vs urgent vs emergency
  triage; species selector; from $300
- RCVS compliance: RCVS-accredited practice logo (Practice Standards Scheme); named vet
  with RCVS number; OOH cover statement (RCVS Code requires 24/7 emergency access —
  must state who provides OOH cover)
- Pet health library: condition guides; seasonal reminders; organic search; from $250
- Pet health plans: monthly direct debit (Vetsure / VetEnvoy / PetsApp); vaccinations /
  flea-worm / annual check-up; highest-retention product; from $300
- Emergency page: OOH number; triage guide; pet first-aid (choking/bleeding/poisoning/RTA)
- Team profiles: RCVS number + specialism per vet; from $150
- From $600 / $1,200+

### 0d-pre43-b) Restaurant / café / pub / bar / hospitality website
Keywords: restaurant website, café website, pub website, bar website, bistro website,
brasserie website, fine dining website, pizza restaurant website, Indian restaurant website,
Chinese restaurant website, Italian restaurant website, takeaway website, food truck
website, coffee shop website, tea room website, catering company website, wedding caterer
website, hospitality website

Response: food photography + friction-free booking move the needle more than anything else
- Menu page: HTML not PDF (PDF not crawlable or accessible); allergen filters required
  under Natasha's Law; specials updated weekly; from $250
- Online reservation: ResDiary / OpenTable / SevenRooms / Tock; deposit option for large
  parties; from $300
- Food photography coordination: professional shoot 1-2 hours; hero image carousel; the
  highest-ROI investment for restaurant marketing; from $300
- Private dining and events page: capacity / room hire / catering packages; enquiry form
- Takeaway integration: direct (Square/Slerp — no commission) vs marketplace (Deliveroo/
  Uber Eats/Just Eat — 30% commission); direct ordering recommended; from $300
- Google Business integration: menu synced to profile; Reserve with Google button; from $150
- Gift vouchers: WooCommerce; Mother's Day / Christmas gifting; from $200
- From $600 / $1,200+

### 0d-pre43-c) Landscaper / garden designer / groundsworker / garden maintenance website
Keywords: landscaper website, landscaping website, garden designer website, garden design
website, groundsworker website, garden maintenance website, gardener website, lawn care
website, tree surgeon website, arborist website, hedge trimming website, patio installation
website, decking installation website, artificial grass website, turf laying website,
fencing contractor website, driveway installer website, outdoor lighting website

Response: before/after project photography + local trust signals — the portfolio IS the product
- Portfolio gallery: before/after paired images; garden type filters (formal / cottage /
  contemporary / wildlife / roof terrace / commercial); from $300
- Service pages: garden design / full build / planting scheme / lawn care / tree surgery /
  patio / decking / fencing / driveways / irrigation / lighting / maintenance; from $150/page
- Design process page: consultation → concept → planting schedule → build → aftercare;
  justifies design fees; from $200
- Consultation CTA: Calendly 30-min site survey; pre-form (garden size / budget / style /
  timeline); from $200
- Compliance: BALI (British Association of Landscape Industries) membership; LANTRA-trained
  note; public liability £1m+; waste carrier licence (Environment Agency — required for
  removing soil or plant waste)
- Local SEO pages: "[city] garden designer" / "[county] landscaper"; from $150/page
- Seasonal content: spring planting / autumn prep / winter care; email newsletter; from $150
- From $550 / $1,100+

## QA results (46/47 auto + 1 manual = 47/47 all pass)

| Check | Result |
|-------|--------|
| _mcEl null declared at top | OK |
| format _codeBlocks array declared | OK |
| format fenced blocks push pre tag | OK |
| format fenced placeholder CB return | OK |
| format inline codes push code tag | OK |
| format inline placeholder IC return | OK |
| format codeBlocks restore forEach | OK |
| format inlineCodes restore forEach | OK |
| format fenced regex in source | OK |
| isBlk detection startsWith pre | OK |
| code line uses div.pb-brain__line--code | OK |
| _updateMsgCount function declared | OK |
| _updateMsgCount guards null _mcEl | OK |
| msg count uses chatMsgs.length (manual) | OK — var n = chatMsgs.length; n + (n === 1 ? ...) pattern; QA string matched on `chatMsgs.length + (chatMsgs.length ===` but code uses temp variable n — semantically identical |
| _updateMsgCount called in addMsg | OK |
| _updateMsgCount called in clearChat | OK |
| _mcEl created as span element | OK |
| _mcEl class pb-brain__msg-count | OK |
| _mcEl inserted via insertBefore | OK |
| _updateMsgCount called after insert | OK |
| msg-count CSS block | OK |
| msg-count font-size 10px | OK |
| msg-count letter-spacing | OK |
| code-inline CSS block | OK |
| code-inline monospace font | OK |
| code-inline gold colour | OK |
| code-block CSS block | OK |
| code-block dark background | OK |
| code-block overflow-x auto | OK |
| line--code display block | OK |
| vet clinic keywords | OK |
| RCVS registration required | OK |
| OOH cover statement | OK |
| pet health plans preventive club | OK |
| VetDesk RxWorks booking | OK |
| from $600 vet | OK |
| restaurant café pub keywords | OK |
| HTML menu not PDF note | OK |
| Natasha's Law allergen note | OK |
| ResDiary OpenTable SevenRooms | OK |
| direct vs marketplace 30% commission | OK |
| from $600 restaurant | OK |
| landscaper garden designer keywords | OK |
| BALI membership note | OK |
| waste carrier licence note | OK |
| before-after portfolio emphasis | OK |
| from $550 landscaper | OK |
