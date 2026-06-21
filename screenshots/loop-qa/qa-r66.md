# DOM QA Report — R66 — 2026-06-21

## main.js — Topic quick-nav bar (MutationObserver sticky panel)

After 2 or more distinct topic tags (`.pb-brain__topic-tag`) accumulate in the chat log,
a sticky bar appears at the top of the log listing each unique topic label as a clickable
chip. Clicking a chip smooth-scrolls to the first message tagged with that topic and briefly
flashes the tag gold to confirm the jump. The bar is rebuilt by a `MutationObserver` that
watches only direct children of the log (no subtree — avoids observing its own mutations).

### Implementation

```javascript
// R66: Topic quick-nav — sticky bar lists detected topic tags; click scrolls to first occurrence.
if (log && !window._pbQnInited) {
  window._pbQnInited = true;
  var _qnBar = document.createElement('div');
  _qnBar.className = 'pb-brain__quicknav';
  _qnBar.style.display = 'none';
  log.insertBefore(_qnBar, log.firstChild);

  function _qnBuild() {
    var tags = log.querySelectorAll('.pb-brain__topic-tag');
    var seen = {}, items = [];
    for (var _qi = 0; _qi < tags.length; _qi++) {
      var lbl = tags[_qi].textContent.trim();
      if (!seen[lbl]) { seen[lbl] = tags[_qi]; items.push({ lbl: lbl, el: tags[_qi] }); }
    }
    if (items.length < 2) { _qnBar.style.display = 'none'; return; }
    _qnBar.innerHTML = '<span class="pb-brain__quicknav-label">Jump to:</span>';
    items.forEach(function(it) {
      var _chip = document.createElement('button');
      _chip.type = 'button';
      _chip.className = 'pb-brain__quicknav-chip';
      _chip.textContent = it.lbl;
      (function(_it) {
        _chip.addEventListener('click', function() {
          _it.el.scrollIntoView({ behavior: 'smooth', block: 'center' });
          _it.el.classList.add('pb-brain__topic-tag--flash');
          setTimeout(function() { _it.el.classList.remove('pb-brain__topic-tag--flash'); }, 900);
        });
      })(it);
      _qnBar.appendChild(_chip);
    });
    _qnBar.style.display = 'flex';
  }

  var _qnMO = new MutationObserver(function(muts) {
    var added = false;
    muts.forEach(function(mu) {
      mu.addedNodes.forEach(function(n) { if (n !== _qnBar) added = true; });
    });
    if (added) _qnBuild();
  });
  _qnMO.observe(log, { childList: true });
}
```

**`log.insertBefore(_qnBar, log.firstChild)` before `observe()`**: the prepend happens before
observation starts, so it never triggers the observer. No risk of the bar's own insertion
causing an infinite rebuild loop.

**`childList: true` without `subtree`**: only direct child additions/removals trigger the
callback. Modifying `_qnBar.innerHTML` (a grandchild of `log`) does NOT trigger the
observer — preventing infinite rebuild loops without needing a disconnect/reconnect pattern.

**`n !== _qnBar` filter**: the observer callback checks that the added node is not the bar
itself before setting `added = true`. This is a belt-and-suspenders guard in case the
observer scope ever widens — future changes to the observe call won't cause recursion.

**IIFE `(function(_it) { ... })(it)` closure**: each button's click handler closes over
`_it` (a copy of `it` at the moment of the IIFE call), not over `it` in the outer loop.
Without this, all chips would reference the last `it` value after the loop ends.

**`seen` object deduplication**: if the user asks two questions in the same niche (two
messages with a "Legal" tag), `seen["Legal"]` captures the first `.pb-brain__topic-tag`
element found. Clicking "Legal" navigates to the earliest occurrence — chronologically
correct and avoids jumping to the most recent (which the user can already see).

**`position: sticky; top: 0` within scrolling log**: the bar is a direct child of `log`
(the scrolling container). `sticky` within a scroll container sticks to the top of the
container's viewport. As the user scrolls through a long conversation, the nav bar stays
visible at the top of the visible log area — no fixed-position hacks needed.

**2-tag threshold**: a single-topic conversation doesn't need navigation. The bar only
appears when there are 2+ distinct topic labels, ensuring it adds value rather than
cluttering a short exchange.

**`scrollIntoView({ behavior: 'smooth', block: 'center' })`**: `block: 'center'` positions
the target topic tag at the vertical center of the log viewport, not at the top. If the
topic tag is near the end of a message, centering keeps more surrounding context visible
than `block: 'start'` would.

**`pb-brain__topic-tag--flash` animation (900ms)**: after scroll completes (~300ms for
smooth scroll), the tag still visually highlights via a gold keyframe animation for 900ms,
confirming "this is the message you jumped to." The flash removes itself via `setTimeout`.

## main.css — Quick-nav bar + tag flash

```css
/* R66 — topic quick-nav bar + tag flash */
.pb-brain__quicknav{
  display:flex;
  align-items:center;
  gap:5px;
  flex-wrap:wrap;
  padding:6px 10px;
  background:rgba(255,212,0,.04);
  border-bottom:0.5px solid rgba(255,212,0,.14);
  position:sticky;
  top:0;
  z-index:5;
  backdrop-filter:blur(4px);
}
.pb-brain__quicknav-label{
  font-size:10px;
  color:rgba(255,255,255,.35);
  letter-spacing:.04em;
  white-space:nowrap;
  margin-right:2px;
}
.pb-brain__quicknav-chip{
  background:rgba(255,212,0,.07);
  border:0.5px solid rgba(255,212,0,.3);
  border-radius:10px;
  color:rgba(255,212,0,.8);
  font-size:10px;font-weight:500;
  padding:2px 8px;
  cursor:pointer;
  font-family:inherit;
  transition:background .12s,color .12s;
  white-space:nowrap;
}
.pb-brain__quicknav-chip:hover{ background:rgba(255,212,0,.18); color:rgba(255,212,0,1); }
@keyframes pb-tag-flash{ 0%,100%{ background:rgba(255,212,0,.07) } 50%{ background:rgba(255,212,0,.32) } }
.pb-brain__topic-tag--flash{ animation:pb-tag-flash .9s ease; }
```

**`backdrop-filter:blur(4px)` on nav bar**: the bar overlays content as messages scroll behind
it. The blur prevents the text behind the bar from being fully legible, maintaining the
illusion that the bar "floats" above the conversation rather than being a hard HTML element.
`rgba(255,212,0,.04)` at 4% opacity keeps the bar nearly transparent; the blur makes it opaque
enough to be readable.

**`flex-wrap:wrap`**: topic chip labels can be multiple words ("Pricing & Features", "Portfolio
& Showcase"). On a narrow widget (~320px), 3+ chips would overflow a single row without wrap.

**`@keyframes pb-tag-flash` symmetric**: the tag's gold background peaks at 50% (32% opacity)
and returns to rest at 100% (7% opacity = the tag's normal resting background). A pure
fade-in/stay animation would leave the tag permanently highlighted; the symmetric fade
confirms the destination without leaving a permanent visual artefact.

## brainstorm.php — 3 new intent handlers

### 0d-pre62-a) Painter and decorator / painting and decorating / Federation of Master Decorators
Keywords: painter and decorator website, painting and decorating website, decorator website,
painter decorator website, decorating company website, wallpaper hanger website, interior
painter website, exterior painter website, commercial painter website, painting contractor
website, decorating contractor website, painting business website, house painter website,
property painter website, decorating services website, painting services website

Response: portfolio quality signals before they pick up the phone
- Portfolio gallery: before & after; room type (bedroom; kitchen; exterior; commercial;
  feature wall; wallpaper); finish type (matt; silk; eggshell; limewash); from £200
- Service pages: interior; exterior; wallpapering; coving & cornices; spray finishing; wood
  staining; commercial contracts; new-build decoration; from £150/page
- Online quote form: room count; surface area; prep; desired finish; timeline; from £200
- Products & paints page: Farrow & Ball; Little Greene; Dulux Trade; Zinsser; brand
  association builds perceived quality; from £100
- Insurance & accreditation: Federation of Master Decorators; TrustMark; Guild of Master
  Craftsmen; from £100
- Coverage area pages: "painter decorator [town]"; from £100/page
- From £500 / £1,200+

### 0d-pre62-b) Pest control / BPCA / RSPH / HSE / HACCP / commercial vermin
Keywords: pest control website, pest exterminator website, pest controller website, vermin
control website, rodent control website, rat pest control website, mouse control website,
wasp nest website, cockroach control website, bed bug website, ant control website,
mole catcher website, pigeon control website, commercial pest control website,
fumigation website, bpca pest control website

Response: urgency + 24/7 availability closes the call; BPCA/RSPH credentials close the trust
- Emergency callout page: 24/7; same-day; call-tracking number above fold; from £200
- Pest ID pages: rats; mice; wasps; hornets; cockroaches; bed bugs; ants; pigeons; moles;
  squirrels; fleas; moths; cluster flies; "[pest] control [city]" SEO; from £150/page
- BPCA / RSPH credentials: British Pest Control Association; Royal Society for Public Health;
  HSE-compliant use; from £100
- Domestic vs commercial split: homeowners (one-off) vs commercial (contract; HACCP; BRC
  audit ready); from £150
- Proofing & prevention: rat proofing; bird netting; spike installation; fly screens;
  recurring revenue stream; from £150
- Coverage area pages: "rat control [town]"; "wasp nest removal [town]"; from £100/page
- From £500 / £1,200+

### 0d-pre62-c) Care home / domiciliary care / CQC / NHS Continuing Healthcare / dementia
Keywords: care home website, residential care website, nursing home website, domiciliary
care website, home care website, live-in care website, cqc care website, elderly care
website, dementia care website, supported living website, respite care website, care agency
website, homecare website, adult social care website, disability care website

Response: families decide emotionally; CQC compliance and empathy together close the choice
- CQC rating: Outstanding/Good; registration number; mandatory above fold; from £100
- Care type pages: residential; nursing; dementia & Alzheimer's; respite; day care; live-in;
  domiciliary; supported living; learning disabilities; from £150/page
- Virtual tour & gallery: families research remotely; room types; gardens; from £250
- Staff profiles: DBS checked; QCF/NVQ Level 2/3; Dementia Friends; from £150
- Fees & funding: NHS Continuing Healthcare; Local Authority; self-funding; Attendance
  Allowance; transparency reduces enquiry friction; from £150
- Online enquiry / show-round booking: Calendly; low-friction first step; from £200
- Testimonials from families: specific ("Mum settled within a week"); from £100
- From £700 / £1,800+

## QA results (41/41 all pass)

| Check | Result |
|-------|--------|
| R66 comment | OK |
| _pbQnInited guard | OK |
| pb-brain__quicknav class | OK |
| log.insertBefore | OK |
| pb-brain__quicknav-label (double-quote form) | OK |
| pb-brain__quicknav-chip | OK |
| querySelectorAll topic-tag | OK |
| items.length < 2 guard | OK |
| scrollIntoView smooth | OK |
| topic-tag--flash add | OK |
| topic-tag--flash remove setTimeout 900 | OK |
| MutationObserver childList | OK |
| added nodes filter (n !== _qnBar) | OK |
| R66 before form submit | OK |
| R66 CSS comment | OK |
| .pb-brain__quicknav rule | OK |
| position:sticky top:0 | OK |
| z-index:5 | OK |
| backdrop-filter blur | OK |
| .pb-brain__quicknav-label | OK |
| .pb-brain__quicknav-chip rule | OK |
| @keyframes pb-tag-flash | OK |
| .topic-tag--flash rule | OK |
| pre62-a decorator | OK |
| decorator keywords | OK |
| Farrow Ball Little Greene | OK |
| Federation Master Decorators | OK |
| decorator price 500/1200 | OK |
| pre62-b pest control | OK |
| pest control keywords | OK |
| BPCA RSPH credentials | OK |
| pest identification pages | OK |
| HACCP BRC commercial | OK |
| pest price 500/1200 | OK |
| pre62-c care home | OK |
| care home keywords | OK |
| CQC Outstanding/Good | OK |
| NHS Continuing Healthcare | OK |
| Dementia Alzheimer page | OK |
| care price 700/1800 | OK |
| pre62 before pre61 | OK |
