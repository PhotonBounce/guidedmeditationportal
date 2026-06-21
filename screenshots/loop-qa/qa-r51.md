# DOM QA Report — R51 — 2026-06-20

## main.js — Markdown list rendering in `format()` + confetti on first user message

### Ordered and unordered list rendering in `format()`

LLM responses frequently contain markdown lists (`1. item`, `- item`). Previously
these rendered as plain text with bullet or number characters; now they produce proper
`<ol>/<ul>` elements with styled list items.

**Extraction strategy — placeholder approach:**

Lists must be extracted BEFORE the HTML-escape step, for the same reason code blocks
are extracted: if `- <em>text</em>` were HTML-escaped first, the `<` and `>` would
become `&lt;` and `&gt;`, making the content unrecognisable as HTML when restored.
And unlike code blocks, list item CONTENT should receive inline formatting (bold,
italic, del, links), so a custom helper processes each item independently.

```javascript
var _listBlocks = [];
var _fmtItem = function(s) {
  return s
    .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')
    .replace(/\*\*(.+?)\*\*/g,'<strong>$1</strong>')
    .replace(/(?<!\w)_([^_\n]+)_(?!\w)/g,'<em>$1</em>')
    .replace(/~~([^~\n]+)~~/g,'<del>$1</del>')
    .replace(/\[([^\]]+)\]\((https?:[^)]+)\)/g,'<a href="$2" target="_blank" rel="noopener">$1</a>');
};
// Ordered list: consecutive lines starting with N.
text = text.replace(/((?:^|\n)\d+\. [^\n]+)+/g, function(block) {
  var html = '<ol class="pb-brain__ol">' + block.trim().split('\n').map(function(l) {
    return '<li class="pb-brain__li">' + _fmtItem(l.replace(/^\d+\. /, '')) + '</li>';
  }).join('') + '</ol>';
  _listBlocks.push(html); return '\x00LB' + (_listBlocks.length - 1) + '\x00';
});
// Unordered list: consecutive lines starting with - or *
text = text.replace(/((?:^|\n)[*-] [^\n]+)+/g, function(block) {
  var html = '<ul class="pb-brain__ul">' + block.trim().split('\n').map(function(l) {
    return '<li class="pb-brain__li">' + _fmtItem(l.replace(/^[*-] /, '')) + '</li>';
  }).join('') + '</ul>';
  _listBlocks.push(html); return '\x00LB' + (_listBlocks.length - 1) + '\x00';
});
```

Restored at the end of `format()` after code-block and inline-code restores:
```javascript
_listBlocks.forEach(function(h, i) { text = text.replace('\x00LB' + i + '\x00', h); });
```

**`_fmtItem` helper — why not call `format()` recursively:**
Recursive `format()` would re-enter the code-block and list extraction passes on
list item content, risking infinite recursion if an item contains a code fence or
a nested list. The helper function is a safe subset: HTML-escape + inline formatting
only (bold, italic, del, links). URL auto-detection omitted — rare in concierge list
items and avoids the `(^|[\s(])` regex over individual item text.

**`((?:^|\n)\d+\. [^\n]+)+` regex analysis:**
- `(?:^|\n)` — each item starts at string start or after a newline (non-capturing)
- `\d+\.` — any digit sequence followed by a dot: `1.`, `2.`, `99.`
- ` [^\n]+` — one mandatory space then item content (no newlines allowed in a single item)
- `+` on the outer group — consecutive items collected into one block match

A blank line between list items (`\n\n`) breaks the match because `\n\n\d+` fails: the
second `\n` is not `\d`. This correctly produces two separate `<ol>` elements for two
separated numbered lists — matches reader expectation.

**Position relative to `\n→<br>`:**
The list extraction runs BEFORE `.replace(/\n/g, '<br>')`. The `\n` characters
between list items are consumed by the list-extraction regex (they're part of the
match). The placeholder `\x00LB0\x00` replaces the entire block including the
newlines. After `\n→<br>` converts remaining newlines to `<br>`, the placeholder
text (all ASCII, no `\n`) is unaffected. Restore happens after the `<br>` step —
the `<ol>` HTML is inserted into already-processed output cleanly.

**Early-exit interaction:** `format()` returns early if the text already contains
HTML tags (`<a`, `<strong`, etc.) — this is the guard for PHP-side responses that
already contain HTML. LLM responses arrive as plain markdown text and pass through
the full pipeline including list extraction.

**`\x00` null-byte prefix:** same convention as `\x00CB` (code blocks) and `\x00IC`
(inline code). The null byte is a non-printable character that won't appear in any
user input or LLM response. `\x00LB0\x00` is unique: even if a response happens to
contain the string `LB0`, it would not match `\x00LB0\x00` (null bytes required).

CSS for lists:
```css
.pb-brain__ol,.pb-brain__ul{
  margin:5px 0 5px 18px; padding:0;
  display:block;
}
.pb-brain__ol{ list-style:decimal; }
.pb-brain__ul{ list-style:disc; }
.pb-brain__li{
  display:list-item; margin:2px 0;
  color:rgba(255,255,255,.88); font-size:13px; line-height:1.55;
}
.pb-brain__ol .pb-brain__li::marker,.pb-brain__ul .pb-brain__li::marker{
  color:rgba(255,212,0,.6);
}
```

**`display:block` on the `ol`/`ul`**: the bot message uses `.pb-brain__text` which
contains `.pb-brain__line` spans. These are `display:inline` spans separated by `<br>`
elements. A list block replacement (`\x00LB0\x00`) gets wrapped in a `.pb-brain__line`
span, making the `<ol>` display as `inline` (inheriting the span). `display:block`
overrides this, giving the list its natural block layout so items stack vertically.

**`margin:5px 0 5px 18px`**: the `18px` left margin indents the list away from the
bubble edge. Without it, list bullets/numbers would appear at the same horizontal
position as prose text — the indentation is the visual cue that makes lists scannable.
`5px` top and bottom separates the list from surrounding prose.

**`list-style:decimal` / `list-style:disc`**: standard browser defaults. Reset is needed
because browser user-agent stylesheets may be overridden by the theme's CSS reset.
Explicit declaration ensures lists render correctly even in a fully reset environment.

**`::marker` pseudo-element**: gold tint `rgba(255,212,0,.6)` on bullet and number
characters. `::marker` is supported in Chrome 86+, Firefox 68+, Safari 15.4+ — the
same browser floor as the lookbehind regex in `_italic_` detection (all require ES2018+).
This colours the marker (bullet disc, decimal number) without affecting item text.

**`display:list-item` on `pb-brain__li`**: without `display:list-item`, an element
doesn't render its `::marker` and doesn't participate in the `list-style` from the
parent. The `.pb-brain__line` span inside which the `<li>` renders might inherit
`display:inline`, suppressing the list-item box. The explicit declaration ensures
correct rendering.

### Confetti burst on the first user message

The very first message a visitor types triggers a brief confetti shower (18 particles)
over the page — a micro-delight that signals "you've started a conversation" and rewards
the first interaction.

```javascript
function _confetti() {
  var cols = ['#ffd400','rgba(255,212,0,.8)','#fff','rgba(255,255,255,.7)','rgba(255,180,0,.9)'];
  for (var i = 0; i < 18; i++) {
    var p = document.createElement('div');
    p.className = 'pb-brain__confetti';
    p.style.left = (Math.random() * 100) + '%';
    p.style.animationDelay = (Math.random() * 0.55) + 's';
    p.style.background = cols[i % cols.length];
    var sz = (4 + Math.random() * 5) + 'px';
    p.style.width = sz; p.style.height = sz;
    document.body.appendChild(p);
    setTimeout(function(el) { if (el.parentNode) el.parentNode.removeChild(el); }, 2200, p);
  }
}

// In submit handler, after addMsg(text, 'user'):
if (chatMsgs.filter(function(m) { return m.cls === 'user'; }).length === 1) { _confetti(); }
```

**`chatMsgs.filter(m.cls === 'user').length === 1`**: `addMsg()` pushes to `chatMsgs`
before returning. By the time this line runs, the first user message is in the array.
The check `=== 1` fires exactly once — only on the FIRST send of the session. On
session restore, `chatMsgs` is pre-populated from `sessionStorage pb_chat_v1`, so
the array is already > 1 when the user types again → confetti skipped. This is
correct: confetti marks the first-ever interaction, not restarts.

**Why check AFTER `addMsg` not before**: checking `length === 0` before the push
would also work, but `=== 1` after is more readable ("this is the first user message").

**`document.body.appendChild(p)`**: confetti particles are appended to `<body>` with
`position:fixed`, making them overlay the entire page rather than being clipped by the
chatbot widget's overflow. Alternative: appending to the `.pb-brain` container would
clip confetti to the widget bounds and feel more contained. Body-level confetti creates
a more celebratory, whole-page moment.

**`position:fixed; top:0`**: particles start at the top of the viewport. Combined with
`left` set to a random 0–100% position, they scatter across the full viewport width
before falling. `z-index:9999` ensures they appear above all page content.

**18 particles**: enough for visual impact without performance cost. Each particle is a
single DOM element with CSS animation — no canvas, no requestAnimationFrame, no library.
18 elements × 1.7s animation = ~30 animation-frame updates total, negligible on any
modern device.

**Color palette**: gold (`#ffd400`) and white (`#fff`) with varying opacity variants.
Five colors cycle: i % 5 distributes them evenly across 18 particles. The gold and
white match the chatbot's existing color language — the confetti feels designed, not
random.

**`animationDelay: Math.random() * 0.55s`**: particles launch with up to 550ms
stagger. Without delay, all 18 would fall in sync — a wall of moving color. The
stagger creates the visual of individual particles drifting at different moments,
mimicking real confetti physics.

**`setTimeout(removeChild, 2200ms)`**: particles are cleaned up from the DOM after
2.2 seconds (the animation is 1.7s; the 500ms margin handles delayed particles).
The `if (el.parentNode)` guard prevents errors if the particle was already removed
(e.g., page navigation during animation).

CSS:
```css
@keyframes pb-confetti-fall{
  0%  { transform:translateY(-20px) rotate(0deg) scale(1); opacity:1; }
  70% { opacity:.9; }
  100%{ transform:translateY(110vh) rotate(720deg) scale(.5); opacity:0; }
}
.pb-brain__confetti{
  position:fixed; top:0; z-index:9999; pointer-events:none;
  border-radius:2px;
  animation:pb-confetti-fall 1.7s cubic-bezier(.25,.8,.25,1) both;
}
```

**`translateY(110vh)` end state**: particles fall below the viewport bottom. `110vh`
ensures even particles that start slightly high still clear the bottom before fading.
The `scale(.5)` shrinks them as they fall — smaller particles at distance mimics
physical perspective.

**`rotate(720deg)`**: two full rotations over 1.7s. Each particle starts at 0° and
ends at `0° + random-delay × angular-velocity`. The varying stagger means particles
rotate to different final angles, avoiding the lockstep appearance.

**`cubic-bezier(.25,.8,.25,1)`**: the browser's `ease-out` approximation — fast
initial velocity that decelerates. Mimics gravity (fast fall, slowing at the bottom).
The alternative `linear` would feel mechanical.

**`opacity:.9` at 70%**: particles stay nearly fully visible for most of the fall,
then fade quickly in the last 30% of the animation. This keeps them visible while
falling past visible content and fades them out before they disappear off-screen —
cleaner than a linear fade throughout.

## brainstorm.php — 3 new intent handlers

### 0d-pre47-a) Dog groomer / pet groomer / mobile dog groomer / dog salon website
Keywords: dog groomer website, dog grooming website, pet groomer website, pet grooming
website, mobile dog groomer website, mobile grooming website, dog salon website, dog
parlour website, pet salon website, puppy groomer website, cat groomer website, dog
wash website, hand strip groomer website, show dog groomer website, dog grooming van
website, dog spa website

Response: trust signals + frictionless booking — pet owners hand over something precious
- Meet the groomer: City & Guilds Level 3 / iPET Network / BII qualifications; paediatric
  first aid certification date; face photo with a dog; from $200
- Online booking: Shortcuts, Petlinx, or Calendly; breed + size + coat type + services;
  deposit to reduce no-shows; from $300
- Services with breed-size pricing: small/medium/large/giant bands; coat surcharges; from $250
- Before/after gallery: healed coat photos; breed-standard clips (Bichon, Poodle, Schnauzer,
  Cocker, Westie); primary conversion asset; from $200
- FAQ / first visit page: anxious dogs, puppy policy, matting, insurance; from $150
- Breed-specific pages: Cockapoo/Golden Retriever/Doodle grooming; from $100/page
- Mobile grooming map: service area postcodes; no trailing leads USP; from $150
- Insurance / licensing: public liability; local authority registration (Animal Welfare
  (Licensing) Act 2018); from $100
- From $400 / $800+

### 0d-pre47-b) Spa / wellness centre / massage therapist / holistic therapist / retreat
Keywords: spa website, day spa website, wellness centre website, wellness center website,
massage therapist website, massage therapy website, holistic therapist website, holistic
therapy website, beauty spa website, med spa website, medical spa website, skin clinic
website, facialist website, reflexology website, aromatherapy website, reiki website,
sound healing website, retreat website, wellbeing website, relaxation centre website,
spa salon website, wellness studio website

Response: sensory atmosphere + frictionless booking — digital experience must match
in-person promise
- Online booking: Fresha (zero commission), Phorest, Mindbody, Zenoti; deposit option;
  booking button above fold on mobile; from $350
- Treatments menu: grouped by category; duration + price + contraindications; from $250
- Gift vouchers: WooCommerce or Treatwell; seasonal; highest-ROI page for spa sites; from $200
- Meet the therapists: ITEC / VTCT / CIBTAC / CIDESCO qualifications; from $150/profile
- Memberships / packages: Stripe recurring billing; pre-paid bundles; from $250
- Medical aesthetics (if applicable): CQC registration required for injectables; GPhC-
  regulated pharmacy; prescriber details; from $200
- Wellbeing blog: informational → booking funnel; from $150/post
- GDPR consent & health forms: Jotform/Typeform e-signature; ICO + CNHC compliance; from $150
- From $550 / $1,100+

### 0d-pre47-c) Event venue / wedding venue / conference venue / function room website
Keywords: event venue website, wedding venue website, conference venue website, function
room website, party venue website, event space website, banqueting suite website,
corporate event venue website, private hire venue website, barn wedding venue website,
manor house wedding website, hotel wedding venue website, micro wedding venue website,
wedding barn website, meeting room website, events hall website, reception venue website,
event hall website

Response: emotional imagery + smooth enquiry-to-quote — buyers shortlist on images
- Virtual tour / gallery: Matterport ~£300/session; ceremony + reception + outdoor + kitchen;
  shortlist decisions made on imagery; from $300
- Capacity and layout: theatre/cabaret/boardroom/banquet/ceremony configs; floor plan PDF;
  from $200
- Packages and pricing: wedding + corporate DDR + evening-only; minimum spend; exclusivity;
  venues that hide pricing get fewer qualified enquiries; from $250
- Enquiry & availability form: CRM integration (HubSpot/Zoho/Salesforce); auto-reply with
  PDF brochure; from $300
- Preferred suppliers page: caterers, florists, photographers, bands, AV; from $150
- Real weddings blog: one post per wedding (with consent); ranks for "[venue name] wedding";
  from $100/post
- Corporate / private hire: HDMI/AV/PA/stage; corporate invoice process; from $200
- Accessibility statement: Equality Act 2010 required; step-free, hearing loop, dietary;
  from $100
- Local authority licences: registered for civil ceremonies; Temporary Events Notice limit
  (499 people); alcohol licence; from $100
- From $700 / $1,400+

## QA results (44/44 all pass)

| Check | Result |
|-------|--------|
| _listBlocks declared | OK |
| _fmtItem helper | OK |
| ol class pb-brain__ol in code | OK |
| ul class pb-brain__ul in code | OK |
| li class pb-brain__li in code | OK |
| LB placeholder in list code | OK |
| _listBlocks restore | OK |
| _fmtItem bold | OK |
| _fmtItem italic | OK |
| _fmtItem del | OK |
| _confetti declared | OK |
| confetti class name | OK |
| confetti body.appendChild | OK |
| confetti removeChild cleanup | OK |
| confetti first-msg guard | OK |
| confetti length===1 | OK |
| .pb-brain__ol,.pb-brain__ul combined rule | OK |
| ol list-style decimal | OK |
| ul list-style disc | OK |
| .pb-brain__li block | OK |
| li display list-item | OK |
| li ::marker rule | OK |
| li marker gold | OK |
| @keyframes pb-confetti-fall | OK |
| confetti rotate 720deg | OK |
| .pb-brain__confetti{ | OK |
| confetti fixed | OK |
| confetti z-9999 | OK |
| dog groomer City Guilds iPET | OK |
| Shortcuts Petlinx Calendly booking | OK |
| Cockapoo breed page | OK |
| Animal Welfare Act 2018 | OK |
| healed coat photos gallery | OK |
| dog groomer price line | OK |
| ITEC VTCT CIBTAC CIDESCO | OK |
| CQC injectables spa | OK |
| Fresha zero commission | OK |
| spa membership Stripe recurring | OK |
| spa price line | OK |
| Matterport virtual tour | OK |
| cabaret banquet layout configs | OK |
| civil ceremonies registrar | OK |
| Equality Act 2010 | OK |
| venue price line | OK |
