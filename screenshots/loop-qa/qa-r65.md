# DOM QA Report — R65 — 2026-06-21

## main.js — Bot message entrance slide-in animation

Every new bot reply receives the class `pb-brain__msg--entering` immediately before
`log.appendChild(div)` commits it to the DOM. The CSS `@keyframes pb-msg-in` animates
the message from `opacity:0; translateY(8px)` to `opacity:1; translateY(0)` over 350ms
with a cubic-bezier ease-out. A `setTimeout(400ms)` removes the class after the animation
completes, leaving no permanent class residue on older messages.

### Implementation

```javascript
// R65: Bot message entrance — slides new bot replies in from below.
if (cls === 'bot') {
  div.classList.add('pb-brain__msg--entering');
  setTimeout(function() { div.classList.remove('pb-brain__msg--entering'); }, 400);
}
```

**Insertion point**: immediately before `log.appendChild(div)` — the class is on the element
when it enters the live DOM, so the browser starts the animation on the first paint. Adding
the class after `appendChild` causes a brief first-frame flash at full opacity before the
animation begins (the browser may batch the paint and then see the class).

**`setTimeout(400ms)` for cleanup**: the animation is 350ms. 400ms gives 50ms buffer before
the class is removed — the animation reaches `opacity:1; translateY(0)` and the `forwards`
fill mode keeps it there, so removing the class at 400ms is visually clean.

**Why not `animationend` event**: `animationend` listener on each new `div` adds 1 listener
per bot message (memory grows linearly with message count). `setTimeout` with a single fixed
delay achieves the same cleanup at negligible overhead and avoids the GC pressure of repeated
`addEventListener`/`removeEventListener` pairs.

**Why only `cls === 'bot'`**: user messages don't need entrance animation — they appear in
response to an explicit keyboard action and feel wrong if they animate in. Bot messages arrive
after a wait (API round-trip), so a smooth entrance makes the transition from "typing
indicator" to "message" feel polished rather than abrupt.

**`forwards` fill mode in CSS**: without `forwards`, the element returns to `opacity:0;
translateY(8px)` after the animation completes (before `setTimeout` fires). `forwards`
holds the final keyframe state. Removing the class at 400ms releases the compositor layer
rather than leaving all messages with an active animation fill.

## main.css — Entrance animation

```css
/* R65 — bot message entrance slide-in animation */
@keyframes pb-msg-in {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}
.pb-brain__msg--entering {
  animation: pb-msg-in .35s cubic-bezier(.25,.46,.45,.94) forwards;
}
```

**`translateY(8px)`**: 8px is the sweet spot for "entered from below" — perceived as a
gentle lift without being a dramatic scroll. 4px reads as a vibration; 16px reads as a
slide that's competing with the content. The chat widget is narrow and 8px maps to roughly
half the line-height of the bubble text.

**`cubic-bezier(.25,.46,.45,.94)`**: the CSS ease-out curve — fast start, slow finish.
Objects moving INTO the viewport (appearing) should decelerate as they arrive, matching
physical intuition of something landing rather than jumping. Matches the ease-out used by
iOS UIKit's default spring for list insertions.

**`opacity: 0` start**: a purely transform-based animation (starting at full opacity but
translateY(8px)) would show a flash of content in the wrong position on the first render
frame. Starting at `opacity:0` ensures the element is invisible until the browser has
composited the starting position, preventing the flash.

**No `animation-delay`**: the message appears immediately. Delay would mean the element
is invisible (at `opacity:0` via `forwards` backwards fill) for the delay period —
potentially confusing if the typing indicator disappears and nothing appears for 200ms.

## brainstorm.php — 3 new intent handlers

### 0d-pre61-a) Music teacher / guitar teacher / piano / singing / ABRSM / Rockschool
Keywords: music teacher website, guitar teacher website, piano teacher website, singing
teacher website, music school website, drum teacher website, violin teacher website,
music tutor website, music lessons website, online music lessons website, music academy
website, singing lessons website, music studio website, music education website,
rockschool teacher website, abrsm teacher website

Response: teaching style showcase + frictionless trial booking
- Online booking: Calendly; LessonSpace (built for music tuition); Acuity; trial lesson as
  first CTA; from £200
- Instrument/subject pages: guitar; piano; drums; vocals; violin; bass; ukulele; music theory;
  each targets "[instrument] lessons [city]"; from £150/page
- ABRSM & Rockschool grade prep: exam board credentials; pass rate stats; highest-converting
  trust signal for parents; from £100
- Age-group pages: children (fun/games/patience); teenagers; adults (returning or beginner);
  genre preference (classical vs contemporary vs pop vs rock); from £100/page
- Online vs in-person: separate pages; Zoom/Google Meet; hardware recommendations; from £100
- Teacher profiles: background; qualifications; DBS check; teaching style; video intro;
  from £100/teacher
- Pupil performances / recital page: social proof; builds community; from £100
- Free resource: first chord sheet; practice routine PDF; email opt-in; from £100
- FAQ: practice frequency; own instrument; starting age; from £100
- From £500 / £1,200+

### 0d-pre61-b) Interior designer / interior decorator / BIID / e-design / home stylist
Keywords: interior designer website, interior design website, interior decorator website,
interior design studio website, home stylist website, interior styling website, commercial
interior design website, residential interior design website, interior architect website,
space planner website, kitchen designer website, bathroom designer website, furniture
designer website, show home designer website, property developer interior website,
staging website

Response: portfolio IS the sales pitch; every image must earn its place
- Portfolio/case studies: before & after; room-type filter (kitchen/bathroom/living/bedroom/
  office/commercial); full-bleed photography; project story (brief → concept → delivery);
  from £300
- Service pages: full design (concept to completion); interior styling; online e-design;
  show home/property staging; commercial fit-out; from £150/page
- Design process: discovery → mood board → space planning → procurement → installation;
  reduces scope creep; from £150
- Online e-design: Zoom consultations; e-design packages; flat fee vs hourly; from £200
- BIID/RIBA membership: British Institute of Interior Design; professional indemnity;
  distinguishes qualified from decorators; from £100
- Style/aesthetic pages: Scandi; biophilic; maximalist; coastal; mid-century; from £100/page
- Press/features: Homes & Gardens; House Beautiful; Livingetc; Grand Designs; from £100
- Discovery call Calendly: free 30-min filter for high-ticket projects; from £150
- From £650 / £1,600+

### 0d-pre61-c) Landscape gardener / garden designer / BALI / APL / Landscape Institute
Keywords: landscape gardener website, landscaping website, garden designer website, landscape
design website, garden landscaping website, landscaping company website, garden maintenance
website, lawn care website, garden care website, patio design website, decking website,
artificial grass website, landscape architect website, outdoor living website, garden
renovation website, grounds maintenance website

Response: photography-first, local SEO-second
- Portfolio: before & after; garden type filter (formal/cottage/contemporary/wildlife/
  low-maintenance/courtyard); from £250
- Service pages: full garden design; maintenance contracts; lawn care; patio & decking;
  artificial grass; planting schemes; irrigation; lighting; outdoor kitchens; from £150/page
- Online quote form: garden size; work type; photos; postcode; timeline; from £200
- Maintenance contract page: fortnightly/monthly/seasonal; recurring revenue; from £150
- Design process: site survey → concept → planting → installation → aftercare; from £150
- BALI/APL/Landscape Institute: BALI = approved contractor; APL = professional landscapers;
  Landscape Institute = chartered landscape architect; from £100
- Coverage area pages: "landscape gardener [town]"; high-converting for maintenance; £100/page
- Seasonal blog: "Best plants for north-facing garden"; long-tail SEO; from £100/post
- From £600 / £1,400+

## QA results (30/30 all pass)

| Check | Result |
|-------|--------|
| R65 comment | OK |
| cls===bot guard (within 100) | OK |
| classList.add entering | OK |
| setTimeout 400 | OK |
| classList.remove entering | OK |
| R65 before log.appendChild | OK |
| R65 CSS comment | OK |
| @keyframes pb-msg-in | OK |
| from opacity:0 translateY(8px) | OK |
| to opacity:1 translateY(0) | OK |
| .pb-brain__msg--entering rule | OK |
| animation pb-msg-in .35s | OK |
| cubic-bezier easing | OK |
| forwards fill mode | OK |
| pre61-a music teacher | OK |
| music teacher keywords | OK |
| ABRSM Rockschool credentials | OK |
| LessonSpace booking | OK |
| music price 500/1200 | OK |
| pre61-b interior designer | OK |
| interior designer keywords | OK |
| BIID membership | OK |
| e-design online | OK |
| interior price 650/1600 | OK |
| pre61-c landscape gardener | OK |
| landscape keywords | OK |
| BALI APL credentials | OK |
| Landscape Institute | OK |
| landscape price 600/1400 | OK |
| pre61 before pre60 | OK |
