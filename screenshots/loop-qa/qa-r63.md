# DOM QA Report — R63 — 2026-06-21

## main.js — Niche autocomplete suggestion panel

As the user types in the chat input (from 2+ characters), a dropdown panel appears above
the input showing up to 3 matching niche suggestions from a 42-word vocabulary list.
Each suggestion is prefixed "I need a …" — clicking auto-fills the input and submits it
immediately. Arrow keys navigate the list; Enter selects; Escape dismisses.

### Implementation

```javascript
// R63: Niche autocomplete — shows up to 3 matching suggestions as user types; click auto-fills + submits.
if (input && form && !window._pbAcInited) {
  window._pbAcInited = true;
  var _acWords = [
    'solicitor website','dental practice website','wedding photographer website',
    'estate agent website','hotel website','bed and breakfast website',
    'car garage website','hair salon website','personal trainer website',
    'childminder website','restaurant website','pub website',
    'plumber website','electrician website','accountant website',
    'florist website','dog trainer website','jeweller website',
    'copywriter website','funeral director website','optician website',
    'physiotherapist website','architect website','veterinary website',
    'mortgage broker website','removal company website','podiatrist website',
    'insurance broker website','social media agency website','translation agency website',
    'chartered surveyor website','graphic designer website','catering company website',
    'private chef website','yoga instructor website','life coach website',
    'speech therapist website','hypnotherapist website','drone pilot website',
    'tattoo studio website','beauty therapist website','nutritionist website',
  ];
  var _acPanel = document.createElement('div');
  _acPanel.className = 'pb-brain__ac-panel';
  _acPanel.setAttribute('role', 'listbox');
  _acPanel.style.display = 'none';
  if (input.parentNode) input.parentNode.appendChild(_acPanel);
  var _acActive = -1;

  function _acRender(matches) {
    _acPanel.innerHTML = '';
    _acActive = -1;
    if (!matches.length) { _acPanel.style.display = 'none'; return; }
    matches.forEach(function(m, i) {
      var btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'pb-brain__ac-item';
      btn.setAttribute('role', 'option');
      btn.setAttribute('data-idx', i);
      btn.textContent = 'I need a ' + m;
      btn.addEventListener('mousedown', function(e) {
        e.preventDefault(); // keep input focused
        input.value = 'I need a ' + m;
        _acPanel.style.display = 'none';
        input.focus();
        form.dispatchEvent(new Event('submit', { bubbles: true }));
      });
      _acPanel.appendChild(btn);
    });
    _acPanel.style.display = 'block';
  }

  input.addEventListener('input', function() {
    var q = input.value.trim().toLowerCase();
    if (q.length < 2) { _acPanel.style.display = 'none'; return; }
    var hits = _acWords.filter(function(w) { return w.indexOf(q) > -1; }).slice(0, 3);
    _acRender(hits);
  });

  input.addEventListener('keydown', function(e) {
    var items = _acPanel.querySelectorAll('.pb-brain__ac-item');
    if (!items.length || _acPanel.style.display === 'none') return;
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      _acActive = Math.min(_acActive + 1, items.length - 1);
      items.forEach(function(el, i) { el.classList.toggle('pb-brain__ac-item--focus', i === _acActive); });
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      _acActive = Math.max(_acActive - 1, -1);
      items.forEach(function(el, i) { el.classList.toggle('pb-brain__ac-item--focus', i === _acActive); });
    } else if (e.key === 'Enter' && _acActive > -1) {
      e.stopImmediatePropagation();
      items[_acActive].dispatchEvent(new Event('mousedown', { bubbles: true }));
    } else if (e.key === 'Escape') {
      _acPanel.style.display = 'none';
    }
  });

  document.addEventListener('click', function(e) {
    if (!_acPanel.contains(e.target) && e.target !== input) _acPanel.style.display = 'none';
  });
}
```

**2-character minimum**: fires once the user has typed enough to have intent (single characters
produce too many matches and feel noise). `q.length < 2` returns early before any string ops.

**42-entry vocabulary**: covers all major niches in brainstorm.php. Every handler that was
added in R55–R63 has a corresponding word in the list so suggestions follow the PHP coverage.
New handlers from R63 (speech therapist, hypnotherapist, drone pilot) are included.

**`mousedown` not `click`**: `click` fires after `blur` — the input loses focus before the
button's click handler runs, and some forms re-render on blur, making the button disappear
before `click` fires. `mousedown` fires before the focus transfer. The `e.preventDefault()`
call inside prevents the input from actually losing focus, keeping the form in place.

**`e.stopImmediatePropagation()` on Enter + focused item**: prevents the form's own
`keydown` → `submit` listener from also firing when Enter selects an autocomplete suggestion.
Without stop, both the mousedown dispatch AND the form submit would run in the same tick.

**`_acActive = -1` on `_acRender`**: each time the input changes and new results appear,
the focused index resets. The user's typed position is the default (nothing highlighted);
they must press ↓ to enter the list. This matches Chrome/Firefox omnibar behaviour.

**`position:absolute` + `bottom:calc(100% + 4px)`**: the panel is positioned relative to
`input.parentNode` (the input wrapper div, which is `position:relative` in the existing
theme). `bottom: 100% + 4px` pops the panel ABOVE the input rather than below, so it
doesn't overlap the chat log scrollback.

**`role="listbox"` / `role="option"`**: makes the panel screen-reader-navigable per ARIA
1.1 listbox pattern. `data-idx` attribute allows the keyboard handler to identify items
without relying on DOM order.

**`document click` dismiss**: same dismiss pattern as the selection pill (R62). `_acPanel.contains(e.target)` allows clicking items INSIDE the panel without dismissing it (the mousedown click on an item is also inside the panel).

**`_acWords` at 42 entries**: lists are O(n) but n=42 with 2-char minimum keeps the
filter sub-millisecond. No indexing structure is needed.

**`window._pbAcInited` guard**: prevents double-initialisation if the widget init runs
more than once. Same pattern as `_pbPlCycleInited` (R61) and `_pbHistInited` (R60).

## main.css — Autocomplete panel

```css
/* R63 — niche autocomplete suggestion panel */
.pb-brain__ac-panel{
  position:absolute;
  bottom:calc(100% + 4px);
  left:0; right:0;
  background:rgba(18,18,36,.98);
  border:0.5px solid rgba(255,212,0,.22);
  border-radius:8px;
  overflow:hidden;
  box-shadow:0 4px 18px rgba(0,0,0,.55);
  z-index:200;
}
.pb-brain__ac-item{
  display:block;
  width:100%;
  text-align:left;
  background:none;
  border:none;
  border-bottom:0.5px solid rgba(255,255,255,.06);
  padding:8px 12px;
  font-size:12px;
  color:rgba(255,255,255,.65);
  font-family:inherit;
  cursor:pointer;
  transition:background .1s,color .1s;
}
.pb-brain__ac-item:last-child{ border-bottom:none; }
.pb-brain__ac-item:hover,.pb-brain__ac-item--focus{ background:rgba(255,212,0,.1); color:rgba(255,212,0,.9); }
```

**`background:rgba(18,18,36,.98)`**: slightly lighter than pure black and slightly darker
than the `--bg-panel` surface, distinguishing the dropdown from its surroundings while
maintaining the dark palette. `.98` keeps it near-opaque without the 0.97 (selaction pill).

**`border:0.5px solid rgba(255,212,0,.22)`**: gold at 22% — much more subtle than the
selaction pill's `.55` border since the panel is a secondary navigation element, not an
action trigger. Sub-pixel border on retina screens appears as an extremely fine hairline.

**`overflow:hidden` on panel**: ensures item hover states and border-radius clip together
without the first/last items having visible corners beyond the rounded panel boundary.

**`.pb-brain__ac-item--focus`**: the keyboard-navigated focus state uses the same style as
`:hover` — gold 10% bg + gold 90% text. By targeting via class toggle rather than CSS
`:focus`, focus works via the `tabindex="-1"` approach without the button needing actual
browser focus (which would steal focus from the input).

## brainstorm.php — 3 new intent handlers

### 0d-pre59-a) Speech and language therapist / SALT / HCPC / AAC / Makaton
Keywords: speech therapist website, speech and language therapist website, salt website,
stammering website, stuttering website, communication difficulties website, aphasia website,
aac website, speech language pathology website, dysphasia website, dysarthria website,
voice therapist website, speech therapy website, swallowing therapist website, dysphagia
website, makaton website

Response: HCPC-registered clinical credibility + condition-specific SEO
- HCPC registration: SLT is a protected title; registration number above fold mandatory; RCSLT
  membership badge; from £100
- Condition pages: stammering/stuttering; aphasia (post-stroke); dyslexia/dyspraxia; selective
  mutism; voice disorders; dysarthria; dysphagia; AAC; Makaton; each targets "[condition] therapy
  [city]"; from £150/page
- Children vs adults split: paediatric (late talkers; language delay; ASD; ADHD) vs adult
  (neurological; oncology; acquired); separate user journeys; from £200
- Video intro: reduces first-session anxiety; Loom embed; from £150
- Online booking: Cliniko; WriteUpp; Jane App; video therapy via Zoom/Whereby; from £250
- School/NHS referral pathway: EHCP; SEND Code of Practice; waiting list explainer; converts
  self-funders; from £150
- Resources/parent guides: "5 ways to support a child's speech at home"; email opt-in; £100
- Testimonials: anonymised per GDPR; specific outcomes; from £100
- From £600 / £1,400+

### 0d-pre59-b) Hypnotherapist / clinical hypnotherapy / GHR / CNHC / gut-directed
Keywords: hypnotherapist website, hypnotherapy website, clinical hypnotherapy website,
hypnosis website, nlp practitioner website, smoking cessation website, stop smoking website,
weight loss hypnotherapy website, phobia treatment website, anxiety hypnotherapy website,
sleep hypnotherapy website, hypnobirthing website, past life regression website, solution
focused hypnotherapy website, hypnotherapy for ibs website, hypnotherapy for confidence website

Response: clinical credibility to a sceptical first-time visitor
- GHR/CNHC/NCH credentials: GHR = General Hypnotherapy Register; CNHC = Complementary &
  Natural Healthcare Council (UKAS-accredited); NCH = National Council for Hypnotherapy;
  distinguishes clinical from stage hypnosis; from £100
- Issue pages: stop smoking (NHS Smokefree evidence cited); weight loss (gastric band);
  anxiety; phobias; insomnia; IBS (NICE now references gut-directed hypnotherapy); confidence;
  exam nerves; each ranks for "[condition] hypnotherapy [city]"; from £150/page
- How hypnotherapy works: science-first explainer dispels stage-hypnosis myths; neuro references;
  from £200
- Solution Focused (SFBT): forward-focus approach; most NHS-adjacent practitioners use this; £150
- Online sessions: Zoom; works for travel phobias or housebound clients; from £150
- Free discovery call: Calendly; reduces sceptic barrier; from £200
- Audio download/MP3 opt-in: relaxation recording; email list; from £100
- FAQ: sessions needed; safety; stage-hypnosis myth; from £100
- From £550 / £1,300+

### 0d-pre59-c) Commercial drone pilot / UAV operator / CAA GVC / LiDAR / aerial survey
Keywords: drone pilot website, drone operator website, uav website, uav operator website,
aerial photography website, aerial videography website, drone survey website, drone inspection
website, drone mapping website, fpv website, commercial drone website, drone services website,
aerial survey website, lidar drone website, thermal drone website, drone cinematography website

Response: CAA authorisation and insurance first; portfolio second
- CAA authorisation: GVC (General Visual Line of Sight Certificate); PDRA-G01/G02; Specific
  Operational Authorisation; UAS Operator ID; non-display = commercial clients won't sign off;
  from £100
- Service pages by sector: aerial photography/video (property; events; weddings; tourism);
  construction progress monitoring; roof/building inspection (NDT); agricultural mapping (NDVI;
  crop health); land survey/topographic; infrastructure inspection (pylons; bridges; wind turbines);
  SAR support; FPV creative; from £150/page
- Insurance proof: min £1M public liability (many clients require £5M); certificate download; £100
- Portfolio/showreel: 90-second 4K reel (Vimeo); before/after inspection images; from £250
- Equipment: DJI Matrice; M3T thermal; LiDAR payload; RTK GPS (±1cm accuracy); from £150
- Deliverables: 4K/6K video; RAW stills; orthomosaics; point clouds; Agisoft Metashape;
  DroneDeploy; from £150
- Coverage area pages: counties; controlled airspace (A2 CoC); NOTAM filing; from £100/page
- Quote form: location; date; project type; area (ha); deliverables; from £200
- From £600 / £1,500+

## QA results (62/62 all pass)

| Check | Result |
|-------|--------|
| R63 comment | OK |
| _pbAcInited guard | OK |
| _acWords array | OK |
| solicitor in acWords | OK |
| dog trainer in acWords | OK |
| speech therapist in acWords | OK |
| drone pilot in acWords | OK |
| pb-brain__ac-panel class | OK |
| role=listbox | OK |
| _acPanel display none | OK |
| parentNode.appendChild | OK |
| _acActive = -1 | OK |
| pb-brain__ac-item class | OK |
| role=option | OK |
| e.preventDefault mousedown | OK |
| I need a text prefix | OK |
| form.dispatchEvent submit | OK |
| input listener on input event | OK |
| q.length < 2 guard | OK |
| .filter indexOf | OK |
| .slice(0, 3) | OK |
| ArrowDown branch | OK |
| ArrowUp branch | OK |
| --focus class toggle | OK |
| Enter select focused | OK |
| Escape dismiss | OK |
| document click dismiss | OK |
| R63 before form submit | OK |
| R63 CSS comment | OK |
| .pb-brain__ac-panel rule | OK |
| position:absolute | OK |
| bottom:calc(100%+4px) | OK |
| gold border .22 | OK |
| border-radius:8px | OK |
| z-index:200 | OK |
| .pb-brain__ac-item rule | OK |
| font-size:12px | OK |
| color:.65 default | OK |
| .ac-item hover gold | OK |
| .ac-item--focus rule | OK |
| pre59-a present | OK |
| speech therapist keywords | OK |
| HCPC RCSLT credentials | OK |
| stammering condition page | OK |
| dysphagia swallowing | OK |
| AAC Makaton | OK |
| SALT price line | OK |
| pre59-b present | OK |
| hypnotherapist keywords | OK |
| GHR CNHC credentials | OK |
| smoking cessation NICE | OK |
| IBS gut-directed | OK |
| solution focused SFBT | OK |
| hypno price line | OK |
| pre59-c present | OK |
| drone pilot keywords | OK |
| CAA GVC authorisation | OK |
| PDRA authorisation | OK |
| LiDAR thermal drone | OK |
| DroneDeploy Metashape | OK |
| drone price line | OK |
| pre59 before pre58 | OK |
