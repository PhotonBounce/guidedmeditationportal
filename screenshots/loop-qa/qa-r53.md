# DOM QA Report — R53 — 2026-06-20

## main.js — Smart per-response suggestion chips

### Feature overview

After every bot response, 2–3 contextual suggestion chips appear below the message.
Chips are keyword-matched to the response content so they feel directly relevant.
Clicking a chip sends that text as the user's next message. The previous set of chips
is removed whenever a new response arrives, keeping the UI clean.

### Implementation

```javascript
// R53: Smart per-response suggestion chips — context-aware follow-ups after every bot reply.
if (cls === 'bot') {
  var _oldSmart = log.querySelector('.pb-brain__chips-smart');
  if (_oldSmart) _oldSmart.remove();
  var _smartChipsData = [
    { k: ['price','cost','invest','budget','charg','fee','from $'],
      c: ['How long will it take?', 'Can I see examples of your work?', 'What's included in the price?'] },
    // ... 14 more topic groups
  ];
  var _smartDefaultChips = ['How much does this cost?', 'How long does it take?', 'Can I see past work?'];
  var _lowerResp = plain(text).toLowerCase();
  var _pickedChips = _smartDefaultChips;
  for (var _scIdx = 0; _scIdx < _smartChipsData.length; _scIdx++) {
    var _kws = _smartChipsData[_scIdx].k, _hit = false;
    for (var _kIdx = 0; _kIdx < _kws.length; _kIdx++) {
      if (_lowerResp.indexOf(_kws[_kIdx]) > -1) { _hit = true; break; }
    }
    if (_hit) { _pickedChips = _smartChipsData[_scIdx].c; break; }
  }
  var _scDiv = document.createElement('div');
  _scDiv.className = 'pb-brain__chips-smart pb-brain__chips';
  _scDiv.setAttribute('aria-label', 'Suggested follow-ups');
  _pickedChips.forEach(function(label) {
    var _scBtn = document.createElement('button');
    _scBtn.type = 'button'; _scBtn.className = 'pb-brain__chip pb-brain__chip--smart';
    _scBtn.textContent = label;
    _scBtn.addEventListener('click', function() {
      _scDiv.remove();
      if (input && form) { input.value = label; form.dispatchEvent(new Event('submit', { bubbles: true })); }
    });
    _scDiv.appendChild(_scBtn);
  });
  _scDiv.classList.add('pb-chips-entering');
  log.appendChild(_scDiv);
  requestAnimationFrame(function() {
    requestAnimationFrame(function() { _scDiv.classList.remove('pb-chips-entering'); });
  });
}
```

### Keyword topics (15 groups)

| Topic | Sample keywords | Sample chips |
|-------|----------------|--------------|
| Price/budget | price, cost, invest, budget, charg, fee | How long will it take? / Can I see examples? |
| SEO | seo, rank, google, search engine, keyword | How long to see SEO results? / Do you write content? |
| E-commerce | ecommerce, shop, woocommerce, shopify, stripe | Which platform do you recommend? |
| Booking | booking, appointment, calendar, schedule | Which booking system? / Can it send reminders? |
| Wedding/events | wedding, venue, ceremony, celebration | Do you have a weddings portfolio? |
| Maintenance | maintenance, support, update, hosting | What's included in support? |
| Mobile/perf | mobile, responsive, phone, speed, core web | How do you test on mobile? |
| Brand/logo | brand, logo, design, colour, font | Do you offer logo design? |
| Social media | social, instagram, facebook, twitter | Do you manage social too? |
| Architect | architect, planning, building, extension | What's your planning approval rate? |
| Interior design | interior, decorator, stager, e-design | Do you offer virtual e-design? |
| Florist | florist, flower, bouquet, arrangement | Do you deliver same-day? |
| Childminder | childminder, nursery, childcare, ofsted | How do parents book online? |
| Music teacher | music, teacher, tutor, lesson, instrument | Can students book lessons online? |
| Personal trainer | personal trainer, fitness, gym, workout | Can clients book sessions online? |
| Default (no match) | — | How much does this cost? / How long does it take? |

### Design decisions

**`_oldSmart` cleanup**: before rendering new chips, any existing `.pb-brain__chips-smart`
element is removed. This means only one set of smart chips is ever visible — the most recent
one. Without this, chips would accumulate after every bot message.

**Keyword cascade (first-match wins)**: the 15 groups are tried in order. The first keyword
group with a match in the response wins. Order matters: price/budget comes first because cost
questions are the most common follow-up regardless of topic.

**`plain(text).toLowerCase()`**: keyword matching uses the plain-text version of the response
(HTML stripped) to prevent accidental CSS class name matches or HTML entity text from
triggering incorrect topic detection.

**`_smartDefaultChips`**: if no keyword group matches (e.g. the bot replied with a greeting
or a clarification), the three universal fallbacks appear — How much does this cost? / How
long does it take? / Can I see past work? These are the three most common next questions
regardless of topic.

**Relationship to existing followup chips (line 847–879)**: the existing block fires ONCE
on the 2nd bot message with fixed action chips (Book a call / All pricing / Portfolio). The
new `.pb-brain__chips-smart` block fires on EVERY bot message with contextual topic chips.
They serve different roles: the fixed chips prompt the user towards conversion actions; the
smart chips guide topic exploration. Both can coexist — the smart chips are injected after
the fixed chips block (they go in order: fixed chips at message 2, then smart chips after
every message including message 2).

**Double-rAF for slide-in**: `classList.add('pb-chips-entering')` is set synchronously
before `log.appendChild`. The element is created with `opacity:0; transform:translateY(5px)`
(from CSS). Then two nested `requestAnimationFrame` calls remove the class, letting the
CSS transition run. One rAF is not enough — the browser needs two frames to register the
initial style before transitioning away from it.

**`form.dispatchEvent(new Event('submit', { bubbles: true }))`**: sends the message by
simulating a form submit, which is the same mechanism the Enter-key handler uses. This
ensures the chat history, bot API call, and all side effects behave identically to a
manual message send. `bubbles: true` is required so the submit event propagates to the
form element from the button's position in the DOM.

### Coexistence with existing features

- **The existing chips** (`.pb-brain__chips-followup`) are not modified
- **Copy/thumbs/star/retry buttons** are appended before smart chips so the visual order
  is: message text → copy/thumbs/star → timestamp/response-time/word-count → smart chips
- **Screen reader live region** (`_srLive`) is still updated on the bot message before
  chips are rendered — chips themselves don't announce to SR
- **Scroll behaviour** is inherited from the log's existing `scrollTo` at the end of
  `addMsg()` — no additional scroll needed since chips are appended inside the log

## main.css — Smart chip styles

```css
/* R53 — smart per-response suggestion chips */
.pb-brain__chips-smart{
  display:flex; flex-wrap:wrap; gap:5px;
  padding:4px 12px 10px;
  opacity:0; transform:translateY(5px);
  transition:opacity .25s ease, transform .25s ease;
}
.pb-brain__chips-smart:not(.pb-chips-entering){
  opacity:1; transform:translateY(0);
}
.pb-brain__chip--smart{
  font-size:11px; padding:4px 11px;
  border:1px solid rgba(255,255,255,.18);
  color:rgba(255,255,255,.6);
  background:transparent; border-radius:20px;
  cursor:pointer; line-height:1.4;
  transition:border-color .15s, color .15s, background .15s;
  white-space:nowrap;
}
.pb-brain__chip--smart:hover,
.pb-brain__chip--smart:focus-visible{
  border-color:rgba(255,212,0,.55);
  color:rgba(255,212,0,.9);
  background:rgba(255,212,0,.06);
  outline:none;
}
```

**Visual hierarchy**: smart chips use white-toned borders (`.18` opacity) vs the existing
followup chips which use the gold accent. This makes smart chips feel like suggestions
(exploratory) rather than actions (conversion). On hover they warm to gold to indicate
interactivity.

**`white-space:nowrap`**: prevents chip labels from wrapping mid-word. With `flex-wrap:wrap`
on the container, whole chips wrap to a new row when they don't fit, which is better than
a chip with a line-break inside it.

**`focus-visible`**: keyboard users tabbing to chips see the same gold glow as hover,
removing the jarring `:focus` outline (suppressed with `outline:none`). `focus-visible`
only fires on keyboard navigation, not mouse clicks, so mouse users never see an
unexpected outline flash.

**Slide-in animation**: identical to `.pb-chips-entering` pattern used by the existing
followup chips (R52). Consistent micro-interaction throughout the chat.

## brainstorm.php — 3 new intent handlers

### 0d-pre49-a) Childminder / nursery / childcare / day nursery / after-school club
Keywords: childminder website, nursery website, childcare website, day nursery website,
after-school club website, childcare provider website, childminder site, pre-school website,
preschool website, creche website, childcare setting website, childminding website,
wrap around care website, out of school club website, holiday club website, daycare website

Response: two audiences (parents + Ofsted); trust signals must dominate
- Ofsted grade and registration number: prominently displayed; link to inspection report;
  Outstanding/Good grade badge converts immediately; from $100
- Availability and session booking: term-time sessions; funded 15/30 hours eligibility
  and how to apply; waitlist signup; breakfast/after-school/holiday club; from $250
- Fees and funding calculator: transparent pricing removes biggest friction point; from $200
- Safeguarding/DBS page: DBS status; safeguarding policy PDF; first aid; food hygiene;
  Paediatric First Aid; from $150
- A day in the life / daily routine: EYFS learning framework; outdoor play; mealtimes;
  sleep routines; staff:child ratios by age; from $200
- Parent testimonials and gallery: real photos (not stock); parental media consent note;
  Ofsted Parent View link; from $150
- Staff profiles: named staff + photo + qualifications (Level 3 CCLD, EYPS, QTS) +
  personal note; from $150
- Online registration/enquiry form: child DOB + required sessions + start date; from $150
- Blog / newsletter archive: monthly activity updates; from $100
- From $500 / $900+

### 0d-pre49-b) Music teacher / music school / music tutor / singing teacher
Keywords: music teacher website, music tutor website, music school website, music lessons
website, singing teacher website, guitar teacher website, piano teacher website, drum teacher
website, violin teacher website, music lesson website, music studio website, instrument
lessons website, music academy website, online music lessons website, vocal coach website,
music theory website

Response: teacher IS the product; journey from found-on-Google to booked-trial in <60 seconds
- Teacher profile: performing experience; qualifications (ABRSM/RCM/Trinity diploma; DBS;
  degree); teaching style; instruments/ages; own performing video; from $200
- Online booking: Calendly/Acuity/BookWhen; real-time availability; trial lesson separate;
  Zoom/Meet link for online; from $250
- Lesson options and fees: by instrument; by duration (20/30/45/60 min); in-person/online;
  adult vs child; ABRSM exam packages; from $150
- Student progress videos/audio samples: real pupils 6 months in; parental permission; from $150
- Exam results: ABRSM/Trinity pass rates and distinctions; "100% pass rate across 47
  candidates in 2024"; from $100
- FAQ page: what age to start; do we need a piano at home; complete beginners; from $150
- Genre/style showcase: classical vs jazz vs pop; targets "jazz piano teacher London"; from $100
- Local SEO page: per instrument per location for multi-teacher schools; from $150
- Online lessons page: national market via Zoom; from $150
- From $450 / $850+

### 0d-pre49-c) Personal trainer / PT studio / fitness instructor / online coach
Keywords: personal trainer website, pt website, fitness trainer website, personal training
website, fitness instructor website, online coach website, online coaching website, fitness
coach website, gym website, pt studio website, fitness studio website, bootcamp website,
nutrition coach website, strength coach website, crossfit website, pilates instructor website

Response: transformation proof + personality before 3-month commitment
- Before/after transformation gallery: highest-converting element; real client results with
  written permission; performance milestones; from $200
- Services and packages: 1-2-1 in-person; group PT; online coaching; nutrition; hybrid;
  4/8/12-week programs; recurring vs block; clear pricing anchor; from $200
- Online coaching page: scalable income; Trainerize/MyPTHub/TrueCoach app; weekly
  check-ins + WhatsApp; national/international; from $150
- Free consultation CTA: Calendly; 20-minute free call; highest-converting single action
  for a PT website; from $200
- Client testimonials with numbers: "lost 12kg in 16 weeks"; "bench 60→100kg"; from $150
- About/credentials: Level 2 Gym Instructor + Level 3 Personal Trainer; CIMSPA/REPs
  registered; specialist qualifications; DBS for minors; public liability; from $150
- Specialist niche page: pre/postnatal, over-50s, powerlifting, running coaching;
  niching dramatically improves ranking and enquiry quality; from $150
- Location/facilities: studio equipment; or for mobile PT: areas covered; from $100
- Blog/content marketing: informational top-of-funnel; from $150/post
- From $500 / $950+

## QA results (37/39 auto + 2 manual = 39/39 all pass)

| Check | Result |
|-------|--------|
| R53 smart chips block present | OK |
| _oldSmart chip cleanup | OK |
| _smartChipsData declared | OK |
| price/cost keywords | OK |
| seo/rank keywords | OK |
| ecommerce keywords | OK |
| music/teacher keywords | OK |
| fitness/PT keywords | OK |
| _smartDefaultChips fallback | OK |
| plain(text).toLowerCase() keyword match | OK |
| _scIdx loop iterates data (manual) | OK — dist 233, needle 30 chars, total 263 > window 250; code confirmed in file |
| pb-brain__chip--smart class | OK |
| dispatchEvent on chip click | OK |
| _scDiv.classList.add for pb-chips-entering | OK |
| _scDiv.classList.remove in double-rAF (manual) | OK — dist 154, needle 41 chars > window 150; code confirmed in file |
| .pb-brain__chips-smart block | OK |
| chips flex wrap gap | OK |
| translate-in translateY(5px) | OK |
| transition opacity ease | OK |
| :not(.pb-chips-entering) restores opacity | OK |
| .pb-brain__chip--smart block | OK |
| chip--smart border-radius 20px | OK |
| chip hover gold border | OK |
| chip hover gold text | OK |
| childminder keywords in PHP | OK |
| Ofsted grade registration | OK |
| funded 15/30 hours eligibility | OK |
| DBS safeguarding mentioned | OK |
| childminder price line | OK |
| music teacher keywords in PHP | OK |
| ABRSM Trinity qualifications | OK |
| exam pass rate statistic | OK |
| national online lessons market | OK |
| music teacher price line | OK |
| PT keywords in PHP | OK |
| before/after gallery | OK |
| CIMSPA REPs registration | OK |
| Level 3 Personal Trainer credential | OK |
| PT price line | OK |
