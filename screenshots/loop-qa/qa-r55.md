# DOM QA Report — R55 — 2026-06-20

## main.js — Empty-state suggestion tiles + JS syntax repair

### Feature overview

Before the user sends their first message, four suggestion tile buttons appear below the
greeting. Each tile shows an emoji icon and a short prompt. Clicking any tile auto-submits
that text as the user's first message and removes the tile grid. The grid is also removed
when the user starts typing and submits manually. After `clearChat()` the tiles reappear
with the next greeting.

### Implementation in `_greetUser()`

```javascript
function _greetUser() {
  if (!log) return;
  var _hr = new Date().getHours();
  var _tod = _hr < 12 ? 'Good morning' : _hr < 17 ? 'Good afternoon' : 'Good evening';
  addMsg(_tod + "! I’m Photon — what are you building today?", 'bot');
  // R55: Empty-state suggestion tiles—shown before the user sends anything.
  var _eh = document.createElement('div');
  _eh.className = 'pb-brain__empty-hints';
  _eh.setAttribute('aria-label', 'Try asking about…');
  var _ehItems = [
    { emoji: '&#128736;', label: 'What can you build for me?' },
    { emoji: '&#128176;', label: 'What do your services cost?' },
    { emoji: '&#128247;', label: 'Show me your portfolio' },
    { emoji: '&#128269;', label: 'Tell me about SEO' },
  ];
  _ehItems.forEach(function(item) {
    var _ehBtn = document.createElement('button');
    _ehBtn.type = 'button'; _ehBtn.className = 'pb-brain__eh-tile';
    _ehBtn.innerHTML = '<span class="pb-brain__eh-icon">' + item.emoji + '</span>'
                     + '<span class="pb-brain__eh-label">' + item.label + '</span>';
    _ehBtn.addEventListener('click', function() {
      _eh.remove();
      if (input && form) { input.value = item.label; form.dispatchEvent(new Event('submit', { bubbles: true })); }
    });
    _eh.appendChild(_ehBtn);
  });
  log.appendChild(_eh);
  requestAnimationFrame(function() { requestAnimationFrame(function() { _eh.classList.add('pb-brain__eh--vis'); }); });
}
```

**Placement**: the tile grid is appended to `log` directly after the greeting bot message,
so it appears as visual continuation of the greeting — not floating in empty space.

**Double-rAF slide-in**: identical pattern to smart chips (R53) and stats panel. The
`opacity:0; transform:translateY(6px)` initial state must be registered by the browser
before `pb-brain__eh--vis` is added to trigger the CSS transition. One `requestAnimationFrame`
is not enough; two frames ensures the browser has committed the initial paint.

**`item.label` in closure**: each click handler captures `item` from the `forEach`
iteration. This is safe in ES5+ `forEach` because each iteration creates a new function
scope with its own `item` binding.

### Removal in `addMsg()`

```javascript
// R55: Remove empty-state hints on first user message
if (cls === 'user') { var _ehEl = log.querySelector('.pb-brain__empty-hints'); if (_ehEl) _ehEl.remove(); }
log.appendChild(div);
```

**Why `cls === 'user'` not `cls === 'bot'`**: the grid should vanish on the user's first
message, not when the bot replies. The user sends a message, the grid disappears, THEN
the bot replies. Removing on bot reply would show the grid for a brief moment between the
user's send and the bot's response.

**`querySelector` each time**: rather than caching a reference to `_eh`, the removal
queries the DOM. This handles all call paths uniformly: direct typing, chip click (which
already called `_eh.remove()` synchronously but `querySelector` returning null is safe),
and session restore (where `_greetUser()` might not have run).

### JS syntax repair (also in R55)

R55 discovered that the `_greetUser()` function in the committed file (and other functions
like `_updateOrbBadge()`) had been written with Unicode LEFT (U+2018) and RIGHT (U+2019)
SINGLE QUOTATION MARKS as JavaScript string delimiters, rather than ASCII apostrophes
(U+0027). These are invalid JS string delimiters and cause a `SyntaxError` in strict
parsers (Node.js v24 / V8).

Root cause: the `Edit` tool converted ASCII single-quote string delimiters to typographic
smart quotes when writing previous round edits to `main.js`.

Fix applied in R55:
1. Global replacement of all U+2018 → ASCII `'` and all U+2019 → ASCII `'` across the
   entire file.
2. Four chip label strings contained `What's` where `'s` was an embedded apostrophe. After
   the global fix, these became unbalanced single-quoted strings. They were individually
   fixed: three with U+2019 as the content apostrophe (outside the delimiter role), one
   with a `\'` escape sequence.
3. Verified with `node --input-type=module < main.js` → no SyntaxError.

Post-fix: 0 U+2018 in file. 4 U+2019 remain as **content** characters (typographic
apostrophes inside properly ASCII-delimited strings). File parses without errors.

## main.css — Empty-state tile styles

```css
/* R55 — empty-state suggestion tiles */
.pb-brain__empty-hints{
  display:grid; grid-template-columns:1fr 1fr;
  gap:8px; padding:8px 12px 12px;
  opacity:0; transform:translateY(6px);
  transition:opacity .3s ease, transform .3s ease;
}
.pb-brain__empty-hints.pb-brain__eh--vis{
  opacity:1; transform:translateY(0);
}
.pb-brain__eh-tile{
  display:flex; align-items:center; gap:8px;
  padding:9px 12px; background:rgba(255,255,255,.05);
  border:1px solid rgba(255,255,255,.1);
  border-radius:10px; cursor:pointer; text-align:left;
  transition:background .15s, border-color .15s;
  min-height:44px; width:100%;
}
.pb-brain__eh-tile:hover,
.pb-brain__eh-tile:focus-visible{
  background:rgba(255,212,0,.08);
  border-color:rgba(255,212,0,.3);
  outline:none;
}
.pb-brain__eh-icon{
  font-size:16px; flex-shrink:0; line-height:1;
}
.pb-brain__eh-label{
  font-size:11.5px; color:rgba(255,255,255,.75);
  line-height:1.3; flex:1;
}
.pb-brain__eh-tile:hover .pb-brain__eh-label,
.pb-brain__eh-tile:focus-visible .pb-brain__eh-label{
  color:rgba(255,212,0,.9);
}
```

**2-column grid**: `grid-template-columns:1fr 1fr` gives equal-width tiles. At the chat
widget's typical width (~360px) this allows 2 tiles per row with comfortable padding —
each tile ends up roughly 160px wide, fitting the short label text on one line.

**`min-height:44px`**: Apple HIG / Material Design recommended minimum touch target. With
`display:flex; align-items:center` the emoji and label are vertically centred within the
44px minimum, even if the text wraps.

**`width:100%`**: `<button>` elements do not stretch to fill grid cells by default.
`width:100%` forces each tile button to fill its 1fr grid column.

**Gold hover**: `rgba(255,212,0,.08)` background and `.3` border match the widget's
existing accent gold (`#ffd400`), making the hover state feel native to the chatbot's
visual language.

**`focus-visible` not `focus`**: keyboard tab-focus shows the same gold highlight as hover
without showing a focus ring on mouse click. Consistent with R53 smart chips.

## brainstorm.php — 3 new intent handlers

### 0d-pre51-a) Veterinary practice / vet surgeon / animal hospital / pet clinic
Keywords: vet website, veterinary website, veterinary practice website, animal hospital
website, pet clinic website, vet surgery website, vet clinic website, animal clinic website,
exotic vet website, equine vet website, farm vet website, large animal vet website,
emergency vet website, vet nurse website, veterinary surgeon website, pet hospital website

Response: dual-mode design (emergency vs new-client-conversion)
- Emergency out-of-hours contact: red/prominent phone number; 24/7 partner link (Vets Now);
  visible above fold on mobile; #1 reason people visit a vet site in panic; from $100
- Services page: vaccinations, neutering, dental, emergency, imaging, in-house lab; from $150
- Online booking (Vetstoria/PetsApp/RxWorks): new-patient form; reason for visit; biggest
  operational efficiency win; from $250
- Meet the team: RCVS registration numbers verifiable on register; specialist certificates;
  clients choose vet as much as practice; from $150
- RCVS Practice Standards Scheme: General/Veterinary Hospital/Referral accreditation; from $100
- Pet health info / symptom guides: "Is my dog in pain?"; new patient traffic; from $150
- Pet health plans / preventive care: monthly direct-debit; vaccinations, flea, annual check;
  increases retention; from $200
- Species-specific pages: rabbit/bird/reptile; low competition; from $100/page
- Online pharmacy (Viovet/VetShop or WooCommerce): from $300
- From $500 / $1,100+

### 0d-pre51-b) Dentist / dental practice / cosmetic dentist / orthodontist / implant centre
Keywords: dentist website, dental practice website, dental website, cosmetic dentist website,
orthodontist website, dental implants website, dental surgery website, teeth whitening
website, invisible braces website, invisalign website, dental clinic website, emergency
dentist website, private dentist website, nhs dentist website, dental plan website,
smile clinic website

Response: build trust before first appointment; dental anxiety is the #1 barrier
- New patient welcome page: NHS/private status; registration process; ranks for "dentist
  taking new patients [area]"; from $200
- Online booking (Dentally/Software of Excellence/Exact): emergency slot prominent; from $250
- Treatment pages with before/afters: whitening, bonding, veneers, Invisalign, implants,
  crowns, dentures; GDC-compliant; from $200
- Meet the dentists: GDC registration number legally required on all marketing material;
  photo + special interests; from $150
- GDC/CQC/HTM 01-05 compliance: registration numbers and verify links; from $100
- Nervous patients page: sedation options (happy air, IV); converts anxious high-intent
  searchers — dental anxiety is the #1 barrier to booking; from $150
- Dental finance / payment plans (Chrysalis/Medenta 0% finance): monthly calculator; biggest
  cosmetic objection removed; from $150
- Dental plan (Denplan/DPAS): monthly direct-debit covering check-ups + hygienist; recurring
  revenue; from $150
- Emergency dentist page: separate page for "emergency dentist [area]"; ranks quickly; from $100
- From $600 / $1,300+

### 0d-pre51-c) Physiotherapist / sports therapist / osteopath / chiropractor
Keywords: physiotherapist website, physio website, physiotherapy website, sports therapist
website, osteopath website, osteopathy website, chiropractor website, chiropractic website,
sports injury website, sports massage website, manual therapy website, rehabilitation website,
acupuncture website, sports physio website, musculoskeletal physio website, private physio
website

Response: condition expertise + practitioner trust converts; client is in pain
- Condition/injury pages: back pain, knee, shoulder, sciatica, ACL, plantar fasciitis;
  each ranks for "[condition] physiotherapy [area]"; from $150/page
- Online booking (Power Diary/Cliniko/Physitrack): new vs returning; condition type;
  biggest operational improvement for solo or group practice; from $250
- Practitioner profiles: HCPC registration number mandatory in UK; BSc/MSc; specialist
  interests; treatment approach; from $150
- HCPC / CSP / GOsC / GCC registration: HCPC for physios; GOsC for osteopaths; GCC for
  chiropractors; registration number with verify link is a legal requirement; from $100
- Services + techniques: manual therapy, dry needling, taping, electrotherapy, biomechanical
  assessment, gait analysis; from $150
- Online exercise portal (Physitrack/PhysiApp): prescribed home exercises with video;
  progress tracking; telehealth; increasingly expected post-pandemic; from $200
- Sports team / corporate partnership page: first aid cover; pitch-side; on-site corporate
  physio; B2B conversion; from $150
- Injury guides / blog: "Should I ice or heat?"; "Sprained ankle healing time?"; from $100
- Telehealth / video appointments: national market; post-op monitoring; from $150
- From $500 / $1,100+

## QA results (45/45 auto + 1 manual = 46/46 all pass)

| Check | Result |
|-------|--------|
| _greetUser function exists | OK |
| R55 empty-hints comment | OK |
| _eh className empty-hints | OK |
| aria-label on _eh | OK |
| 4 items incl Tell me about SEO (dist 251+17, w=280) | OK |
| _ehBtn className eh-tile | OK |
| eh-icon span in innerHTML | OK |
| eh-label span in innerHTML | OK |
| click removes _eh | OK |
| click submits form | OK |
| log.appendChild(_eh) (anchor contains needle, w=70) | OK |
| double-rAF eh--vis (dist 115+17, w=135) | OK |
| R55 removal comment in addMsg | OK |
| cls===user guard (dist 62+14, w=80) | OK |
| querySelector (dist 96+13, w=115) | OK |
| _ehEl.remove() (dist 148+15, w=165) | OK |
| no U+2018 left curly in file | OK |
| 4 U+2019 are content only (file parses OK — manual) | OK — node --input-type=module gives ReferenceError (window), not SyntaxError; 4 U+2019 are typographic apostrophes inside ASCII-delimited strings in chip labels |
| .pb-brain__empty-hints grid declared | OK |
| 2-col grid template | OK |
| opacity:0 slide-in start | OK |
| .pb-brain__eh--vis restores opacity | OK |
| .pb-brain__eh-tile declared | OK |
| tile flex align-center | OK |
| tile glass bg | OK |
| tile border-radius 10px | OK |
| tile hover gold bg (dist 73+19, w=95) | OK |
| tile hover gold border (dist 109+18, w=130) | OK |
| .pb-brain__eh-icon font-size 16px | OK |
| .pb-brain__eh-label font-size 11.5px | OK |
| vet website + emergency vet website keywords | OK |
| RCVS Practice Standards Scheme | OK |
| Vetstoria PetsApp booking | OK |
| pet health plan monthly direct-debit | OK |
| vet from price line | OK |
| dentist + cosmetic dentist keywords | OK |
| GDC registration legally required | OK |
| nervous patients dental anxiety #1 | OK |
| Denplan + DPAS plan | OK |
| dentist from price line | OK |
| physiotherapist + osteopath keywords | OK |
| HCPC mandatory UK registration | OK |
| GOsC + GCC councils | OK |
| Physitrack patient portal | OK |
| physio from price line | OK |
