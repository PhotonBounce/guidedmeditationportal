# DOM QA Report — R60 — 2026-06-20

## main.js — Input history (↑/↓ arrow cycling through sent messages)

Pressing ↑ in the chat input cycles backwards through the user's previously sent messages,
identical to terminal command history. Pressing ↓ moves forward. Returning to -1 (current)
restores the draft the user was typing. A small `.pb-brain__hist-indicator` label fades in
below the input when history mode is active and fades out when the user returns to the draft
or starts typing.

### Implementation

```javascript
// R60: Input history — ↑/↓ arrows cycle through previously sent messages; indicator fades in.
if (cls === 'user') {
  if (typeof window._pbHistInited === 'undefined' && input) {
    window._pbHistInited = true;
    window._pbInputHistory = [];
    window._pbHistPtr = -1;
    window._pbHistDraft = '';
    var _histInd = document.createElement('div');
    _histInd.className = 'pb-brain__hist-indicator';
    _histInd.textContent = '↑ history';
    if (input.parentNode) input.parentNode.appendChild(_histInd);
    input.addEventListener('keydown', function(e) {
      var _hist = window._pbInputHistory;
      if (!_hist || !_hist.length) return;
      if (e.key === 'ArrowUp' && !e.shiftKey) {
        if (window._pbHistPtr === -1) window._pbHistDraft = input.value;
        window._pbHistPtr = Math.min(window._pbHistPtr + 1, _hist.length - 1);
        input.value = _hist[_hist.length - 1 - window._pbHistPtr];
        _histInd.classList.add('pb-brain__hist-indicator--vis');
        e.preventDefault();
      } else if (e.key === 'ArrowDown' && !e.shiftKey && window._pbHistPtr > -1) {
        window._pbHistPtr--;
        input.value = (window._pbHistPtr === -1) ? window._pbHistDraft : _hist[_hist.length - 1 - window._pbHistPtr];
        if (window._pbHistPtr === -1) _histInd.classList.remove('pb-brain__hist-indicator--vis');
        e.preventDefault();
      } else if (e.key !== 'ArrowUp' && e.key !== 'ArrowDown') {
        window._pbHistPtr = -1;
        _histInd.classList.remove('pb-brain__hist-indicator--vis');
      }
    });
  }
  if (window._pbInputHistory) { window._pbInputHistory.push(plain(text)); window._pbHistPtr = -1; }
}
```

**Lazy init on first user message**: the keydown listener and indicator DOM element are created
once, on the first user message. This avoids needing to find a separate widget init hook and
keeps the code self-contained within addMsg. `typeof window._pbHistInited === 'undefined'`
is used rather than `!window._pbHistInited` to avoid re-init if `_pbHistInited` is falsy but
set (e.g., `false`). After init, any subsequent user message just pushes to history and resets
the pointer.

**`window._pb*` namespace**: using `window.*` rather than local `var` makes the state
persistent across chatbot widget reloads (if the container is destroyed and re-mounted),
and avoids closure issues when `addMsg` is called from different code paths. The `_pb`
prefix namespaces the globals to avoid conflicts with other scripts.

**Pointer arithmetic**: `_pbHistPtr = -1` means "at current draft, not in history".
`_pbHistPtr = 0` means "most recent message" (= `_pbInputHistory[length-1]`).
`_pbHistPtr = n` means "n+1 messages back". The formula
`_hist[_hist.length - 1 - window._pbHistPtr]` maps ptr=0 → last item, ptr=1 → second
last item, etc. This is the standard command-history pointer convention (same as bash).

**Draft save**: on the first ↑ press (`_pbHistPtr === -1`), the current input value is
saved to `_pbHistDraft`. On ↓ past the most recent entry (ptr drops to -1), the draft is
restored. This means users don't lose what they were typing by accidentally pressing ↑.

**Non-↑/↓ key resets pointer**: if the user types any non-arrow key while in history mode,
`_pbHistPtr` resets to -1 and the indicator hides. The user has "escaped" history and is
editing the restored message.

**`e.preventDefault()` on ArrowUp/Down**: prevents the cursor from jumping to start/end
of line in text inputs, and prevents the page from scrolling. Without this, ↑ in a textarea
moves the cursor to the previous line rather than cycling history.

**`!e.shiftKey` guard**: Shift+Arrow performs text selection, which should not be intercepted.
Only bare ↑/↓ trigger history cycling.

**Insertion point**: immediately after the R58 user-cleanup block (`if (_ctaEl) _ctaEl.remove();`)
and before `log.appendChild(div)`. The user-message cleanup runs first, then history is
updated, then the message is committed to the DOM.

## main.css — History indicator

```css
/* R60 — input history indicator (shows when ↑/↓ cycling sent messages) */
.pb-brain__hist-indicator{
  font-size:10px;
  color:rgba(255,212,0,.65);
  padding:2px 6px;
  opacity:0;
  transition:opacity .2s ease;
  pointer-events:none;
  letter-spacing:.04em;
  user-select:none;
}
.pb-brain__hist-indicator.pb-brain__hist-indicator--vis{ opacity:1; }
```

**`pointer-events:none` + `user-select:none`**: the indicator is purely decorative — clicking
it should not interact with anything, and dragging over it should not select text. These two
properties make it completely passive from a user interaction perspective.

**`opacity:0` → `.--vis` → `opacity:1`**: same transition pattern as topic tag (R59) and
CTA card (R58). 0.2s is faster than the CTA card (0.3s) because the indicator appears in
response to a keypress and should feel nearly instant.

**Gold at 65% opacity**: slightly muted compared to the CTA button (95%) and topic tag (88%)
to signal it's a supplementary status indicator, not a call to action. Consistent gold family.

## brainstorm.php — 3 new intent handlers

### 0d-pre56-a) Chartered surveyor / RICS surveyor / building survey / homebuyer report
Keywords: surveyor website, chartered surveyor website, building surveyor website, rics
surveyor website, homebuyer survey website, structural survey website, home survey website,
valuation surveyor website, party wall surveyor website, dilapidations surveyor website,
quantity surveyor website, rics website, surveying firm website, building consultancy website,
property surveyor website, snagging surveyor website

Response: authority, credentials, and local expertise
- RICS regulated firm badge: mandatory display; RICS Dispute Resolution Service; from $100
- Survey types: Level 1 (condition); Level 2 (homebuyer, most popular); Level 3 (full
  structural); each page explains who needs it; from $200
- Building defects library: damp; subsidence; roof; timber decay; Japanese knotweed;
  lintel failure; highest organic traffic content for surveyors; from $200
- Online quote/instruction form: postcode; property type; bedrooms; value; lender; from $250
- Party wall: Party Wall Act 1996; Section 3 notice; agreed vs appointed; high-value repeat
  solicitor referrals; from $150
- Valuation services: RICS Red Book; Help to Buy; shared ownership; probate; matrimonial; $150
- Commercial & dilapidations: terminal dilapidations; lease-end negotiation; B2B; from $150
- Team: MRICS/FRICS designations; ARB; local market knowledge; from $100
- Coverage area pages: "building surveyor [town]"; HM Land Registry data; from $100/page
- From $650 / $1,500+

### 0d-pre56-b) Social media agency / content creator / influencer marketing / TikTok agency
Keywords: social media agency website, social media manager website, social media marketing
website, content creator website, influencer marketing website, tiktok agency website,
instagram agency website, content marketing agency website, digital content website, social
media consultant website, community manager website, ugc creator website, content strategy
website, brand content website, video content agency website, social media management website

Response: results-first, not just "pretty posts"
- Case studies with metrics: reach; engagement rate; follower growth; CPE; ROAS; from $300
- Services: organic social; paid social (Meta, TikTok, LinkedIn); content production;
  influencer seeding; community management; UGC creation; social listening; from $200
- Platform specialisms: Instagram Reels; TikTok; LinkedIn B2B; Pinterest; YouTube Shorts;
  Threads; specific KPIs per platform; from $150
- Process: discovery → audit → strategy → calendar → Planable/Loomly/Later → reporting; $150
- Content portfolio / reel: video grid; before/after account audits; from $200
- Influencer roster methodology: nano/micro/macro; vetting; exclusivity; seeding vs paid; $150
- Pricing transparency: starter/growth/enterprise retainer; from $100
- Social proof strip: follower counts managed; posts created; brands; from $100
- Careers / creator submissions: attract UGC talent; from $100
- From $600 / $1,400+

### 0d-pre56-c) Translation agency / interpreter / localisation / certified translation
Keywords: translation agency website, translation company website, translation services
website, interpreter website, interpretation services website, localisation website,
localization website, transcreation website, certified translation website, sworn translation
website, legal translation website, medical translation website, technical translation website,
language services website, document translation website, subtitling website

Response: precision, certification, and turnaround speed
- Language pairs pages: English ↔ French/Spanish/Arabic etc.; ATA/ITI/CIOL credentials per
  page; from $150/page
- Specialism pages: legal (certified; sworn; court-accepted); medical; technical; financial;
  transcreation; subtitling; from $150/page
- Certified/sworn translation: UKVI/Home Office; FCDO apostille; company house; birth/
  marriage/death certificates; most searched by individuals; from $200
- Quote / file upload form: source & target language; word count; subject; deadline;
  urgency surcharge; from $250
- CAT tools & quality: SDL Trados; memoQ; translation memory; ISO 17100; from $150
- Machine translation + MTPE: DeepL / Google + human review; speed vs quality trade-off; $150
- Interpreting services: consecutive; simultaneous; OPI; VRI; BSL; court; medical; from $150
- Client portal / project tracking: file delivery; version history; invoice; from $300
- ATA / ITI / CIOL / CIoL membership badges; from $100
- From $600 / $1,400+

## QA results (45/45 all pass)

| Check | Result |
|-------|--------|
| R60 comment | OK |
| _pbHistInited | OK |
| _pbInputHistory init | OK |
| _pbHistPtr | OK |
| _pbHistDraft | OK |
| hist-indicator element | OK |
| parentNode append | OK |
| keydown listener | OK |
| ArrowUp branch | OK |
| ArrowDown branch | OK |
| hist-indicator--vis add | OK |
| hist-indicator--vis remove | OK |
| preventDefault | OK |
| _pbInputHistory.push | OK |
| before log.appendChild | OK |
| R60 CSS comment | OK |
| .hist-indicator rule | OK |
| opacity:0 | OK |
| gold .65 | OK |
| transition .2s | OK |
| user-select:none | OK |
| --vis opacity:1 | OK |
| pre56-a present | OK |
| surveyor keywords | OK |
| RICS regulated | OK |
| Level 2 homebuyer | OK |
| Party Wall Act | OK |
| MRICS/FRICS | OK |
| surveyor price | OK |
| pre56-b present | OK |
| social media agency keywords | OK |
| TikTok keyword | OK |
| metrics case studies | OK |
| Planable/Loomly/Later | OK |
| influencer roster | OK |
| social media price | OK |
| pre56-c present | OK |
| translation agency keywords | OK |
| certified translation keyword | OK |
| ISO 17100 | OK |
| UKVI / Home Office | OK |
| CIOL membership | OK |
| SDL Trados | OK |
| translation price | OK |
| pre56 before pre55 | OK |
