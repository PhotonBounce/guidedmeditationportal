# DOM QA Report — R49 — 2026-06-20

## main.js — Tab-title "new message" notification + Ctrl+K + textarea typing glow

### Tab-title notification when window is hidden

When a bot reply arrives while the user is on another browser tab (`document.hidden`
is true), the page title changes to `"(N) Photon Bounce"` — same pattern as email
clients and messaging apps that show an unread count in the tab.

```javascript
var _origTitle = document.title;  // captured once at widget-init time
var _tabUnread = 0;

// In addMsg(), inside the if (cls === 'bot') block:
if (document.hidden) {
  _tabUnread++;
  document.title = '(' + _tabUnread + ') Photon Bounce';
}

// visibilitychange listener (added after openBrain):
document.addEventListener('visibilitychange', function() {
  if (!document.hidden && _tabUnread > 0) { _tabUnread = 0; document.title = _origTitle; }
});

// In openBrain() (when user opens the chat widget):
if (_tabUnread > 0) { _tabUnread = 0; document.title = _origTitle; }

// In submit handler's finally block — conditional reset:
if (!document.hidden) document.title = _savedTitle;
```

**Why `_origTitle` at IIFE init time, not `_savedTitle` from the submit handler?**
`_savedTitle` is a `const` declared inside each call to the submit handler closure —
it captures `document.title` at the moment the user sends a message, which may be
`"(2) Photon Bounce"` if the user was away when the first two bot replies arrived.
Restoring `_savedTitle` in that case would restore the stale count string. `_origTitle`
is captured once at widget init before any title changes have occurred — it is always
the clean page title that should be restored.

**`visibilitychange` fires on all tab-switch and browser-minimize events** in modern
browsers. The handler resets `_tabUnread` and restores `_origTitle`. Opening the brain
widget (`openBrain()`) also resets — the user is clearly engaged with the chat.

**`if (!document.hidden) document.title = _savedTitle`**: the old unconditional reset
at the end of the fetch handler would clobber the notification title set by `addMsg`.
The conditional version: if the user is looking at the tab (not hidden) when the
response arrives, the title resets to the saved value normally. If the user is on
another tab when the response arrives, `addMsg` sets the notification title and the
`finally` block doesn't overwrite it — the notification persists until the user returns.

**Counter accumulates**: if 3 bot replies arrive while the user is away (multi-turn
follow-up from a queued message, or a bug where multiple replies fire), the counter
shows `(3) Photon Bounce` — the user sees the full backlog count, not just `(1)`.

**Scope**: `_origTitle` and `_tabUnread` are both at IIFE scope — accessible from
`addMsg()`, `openBrain()`, the `visibilitychange` handler, and the submit handler. A
`var` declared at this scope is the correct choice (not `const` inside a block which
would be inaccessible from sibling closures).

### Ctrl+K shortcut to clear chat

```javascript
// Added to the existing input.addEventListener('keydown', ...) handler:
if (e.ctrlKey && (e.key === 'k' || e.key === 'K')) {
  e.preventDefault();
  clearChat();
}
```

`clearChat` is a function DECLARATION (`function clearChat() {...}`) at line ~862 in
the IIFE. Function declarations are hoisted within their containing scope — the
`keydown` listener closure captures a reference to `clearChat` that resolves at
fire time, when `clearChat` is definitely defined.

`e.preventDefault()` prevents the browser's default Ctrl+K action (which in some
browsers focuses the address bar or the search bar). The chat input must be focused
for the shortcut to fire — this is intentional (user is in the chat context).

**Why input keydown, not document keydown**: `clearChat` is defined within the
chatbot widget's `if(brain)` setup block. The document-level keydown at line ~306
(which handles Ctrl+F search) is at a higher scope and may not have access to
`clearChat` depending on how the IIFE is structured. The `input.addEventListener`
is definitively in the same scope. Adding global keyboard shortcuts at document scope
would also risk interfering with other page interactions.

**Help panel updated**: `'<span><kbd>Ctrl</kbd>+<kbd>K</kbd> Clear chat</span>'`
added to the `_helpPanel.innerHTML` array. The panel now lists 7 shortcuts.

### Textarea typing glow

A subtle gold border and box-shadow glow appears on the textarea while the user has
typed content (class `pb-brain__input--typing`). Removed on blur.

```javascript
// Added inside existing input.addEventListener('input', ...) handler:
input.classList.toggle('pb-brain__input--typing', input.value.length > 0);

// Added inside existing input.addEventListener('blur', ...) handler:
input.classList.remove('pb-brain__input--typing');
```

**`classList.toggle(class, condition)` — the two-argument form**: sets the class if
the condition is `true`, removes it if `false`. This avoids an `if/else` and keeps
the existing event handler lean. Equivalent to:
```javascript
if (input.value.length > 0) input.classList.add('pb-brain__input--typing');
else input.classList.remove('pb-brain__input--typing');
```

**Removed on blur**: when the textarea loses focus, the glow is removed regardless
of whether content remains. This prevents the gold glow persisting while the user is
looking elsewhere on the page (e.g., reading a bot reply). The glow is a typing
indicator, not a "has content" indicator.

**`input.value.length > 0` not `input.value.trim()`**: `trim()` would hide the glow
while the user types only spaces (e.g., spacing before the first word). The glow
should appear as soon as ANY character is typed — visual feedback that the box is
active. The char-limit counter uses `trim()` because it counts meaningful characters;
the glow uses raw length because it's about activity state.

## main.css — Textarea typing glow

```css
.pb-brain__input--typing{
  border-color:rgba(255,212,0,.3) !important;
  box-shadow:0 0 0 2px rgba(255,212,0,.07);
  transition:border-color .15s,box-shadow .15s;
}
```

**`!important` on `border-color`**: the textarea's default focus style (set on
`:focus`) likely has higher specificity than `.pb-brain__input--typing` (class
alone). The `!important` ensures the gold tint overrides the focus style when the
class is active. Box-shadow doesn't need `!important` because it's additive — the
browser composites multiple box-shadows.

**`rgba(255,212,0,.3)`**: same gold hue as the progress bars, message ping, and
inline code, but at `.3` alpha (same as the response-time badge at `.35`, keeping
it quiet). The glow should say "you're actively composing" without competing with
the message content.

**`box-shadow:0 0 0 2px rgba(255,212,0,.07)`**: an outset ring 2px wide at very
low opacity (`.07`) — barely visible but adds depth. The `0 0 0 2px` pattern means
no offset (centered) and no blur (sharp ring), giving a precise outline glow rather
than a diffuse shadow.

**`transition:border-color .15s,box-shadow .15s`**: the glow fades in and out in
150ms — same as the send-hint transition and the copy-button hover. All minor UI
state changes in this widget use the same 150ms timing for consistency.

## brainstorm.php — 3 new intent handlers

### 0d-pre45-a) Estate agent / letting agent / property management website
Keywords: estate agent website, estate agency website, letting agent website,
letting agency website, property management website, property agent website,
property finder website, residential estate agent website, commercial estate agent
website, new homes estate agent website, independent estate agent website,
high street estate agent website, online estate agent website, property developer
website, buy to let website, property investment website, landlord letting website

Response: listings feed + instant valuation are the two conversion anchors
- Property search/listings: IDX/property feed from Rightmove/Zoopla back to own site;
  filters; featured properties rotator; from $400
- Instant valuation tool: ValPal / GetAgent / Hometrack embed; captures name + email +
  phone + address; highest-converting vendor lead magnet; from $250
- Book a valuation CTA: in-person or video; sold price evidence on landing page; from $200
- Area guides: one per area; schools/transport/restaurants/parks/price trend; ranks for
  "[area] estate agents"; from $150/guide
- Landlord services: full vs let-only; Consumer Rights Act 2015 fee transparency required;
  EPC/gas safety/EICR compliance; from $200
- Sold/let results: Land Registry widget; Google Reviews; average days to sell;
  average % of asking price; from $200
- Compliance: TPO/PRS membership required since Oct 2014; NAEA Propertymark logo;
  CMP scheme name required since Apr 2019; ICO GDPR; from $150
- From $700 / $1,400+

### 0d-pre45-b) Wedding photographer / portrait / event photographer / videographer
Keywords: wedding photographer website, wedding photography website, portrait
photographer website, event photographer website, commercial photographer website,
fashion photographer website, videographer website, wedding videographer website,
newborn photographer website, family photographer website, product photographer
website, headshot photographer website, boudoir photographer website, documentary
photographer website, lifestyle photographer website, photographer website

Response: emotional impact first — 8-second style decision; curated not comprehensive
- Portfolio galleries: curated (30 best images > 300 average); category tabs; lazy-loaded
- Full-shoot blog posts: one per wedding/client with consent; ranks for "[venue name]
  wedding photographer"; each post is a landing page; from $100/post
- Investment/pricing page: second-most-visited page; hiding it loses enquiries; from $250
- About page: story + style philosophy + your face (couples hire a person); from $200
- Enquiry form: date + venue + type + budget; HoneyBook / Pixieset / Studio Ninja CRM
- Venue-specific SEO pages: 10–30 venues; long-tail, high buyer intent; from $100/page
- Instagram feed embed: real-time gallery; from $150
- Client gallery portal: Pixieset or Pic-Time private delivery; from $200
- From $600 / $1,200+

### 0d-pre45-c) Music teacher / piano / guitar / music school / music tutor website
Keywords: music teacher website, piano teacher website, guitar teacher website,
violin teacher website, singing teacher website, music school website, music tutor
website, music lessons website, drum teacher website, cello teacher website, trumpet
teacher website, saxophone teacher website, music academy website, music studio
website, online music teacher website, music tutoring website

Response: tutor credibility + trial lesson booking + safeguarding are the three conversion pillars
- Tutor profile: ABRSM/Trinity/conservatoire trained; Grade 8+; DBS checked; instruments
  + genres + experience range; from $150/profile
- Lesson types/pricing: 30/45/60-min; in-person vs online; group vs 1:1; trial lesson;
  from $200
- Book a trial lesson: Calendly/Acuity; instrument + pupil age + level; from $200
- Exam prep page: ABRSM / Trinity College London / Rock School grades 1–8 + diplomas;
  from $200
- Pupil achievements: grade pass rates; merit/distinction pupils (with consent); from $150
- Video performances: short YouTube clips with parental consent (under-18s); most
  persuasive content on a music teacher site; from $150
- Safeguarding statement: DBS certificate date; safeguarding policy; GDPR notice;
  essential for under-18s; from $100
- Gift vouchers: WooCommerce; "gift a trial lesson"; from $150
- From $550 / $1,100+

## QA results (33/33 all pass)

| Check | Result |
|-------|--------|
| _origTitle captures document.title | OK |
| _tabUnread declared at 0 | OK |
| bot addMsg document.hidden block exists | OK |
| tab title set to N Photon Bounce | OK |
| visibilitychange handler registered | OK |
| visibilitychange resets _tabUnread | OK |
| visibilitychange restores _origTitle | OK |
| openBrain resets _tabUnread | OK |
| openBrain restores _origTitle | OK |
| submit handler conditional title reset | OK |
| Ctrl+K in input keydown handler | OK |
| Ctrl+K calls clearChat() | OK |
| Ctrl+K in help panel | OK |
| typing glow class toggled on input event | OK |
| typing glow removed on blur | OK |
| pb-brain__input--typing CSS block | OK |
| typing glow gold border-color | OK |
| typing glow box-shadow | OK |
| estate agent keywords | OK |
| TPO property ombudsman required | OK |
| CMP required note | OK |
| ValPal GetAgent valuation tool | OK |
| from $700 estate agent | OK |
| wedding photographer keywords | OK |
| venue SEO pages mention | OK |
| Pixieset HoneyBook Studio Ninja | OK |
| curated 30 best images note | OK |
| from $600 photographer | OK |
| music teacher keywords | OK |
| ABRSM Trinity Rock School | OK |
| DBS safeguarding under 18 | OK |
| trial lesson booking CTA | OK |
| from $550 music teacher | OK |
