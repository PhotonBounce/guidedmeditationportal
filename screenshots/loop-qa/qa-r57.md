# DOM QA Report — R57 — 2026-06-20

## main.js — Per-message thumbs up/down rating buttons

### Feature overview

After every bot reply, two small thumbs buttons (👍 👎) fade in 800ms after the message
renders. Clicking a thumb records the rating on the message `div` via `data-rating-val`,
highlights the chosen button in gold, and allows toggling (clicking the active button
deselects it). The rating state is stored per-DOM-node, survives scroll but is
intentionally ephemeral (not persisted to sessionStorage) to avoid bias from carried
context across sessions.

### Implementation

Inserted immediately before the `if (cls !== 'err')` saveChat guard, at the end of the
post-`log.appendChild` bot-message block. The placement ensures the rating bar appears
after the bot message is in the DOM and after smart chips are already appended — the order
in the log is: bot message bubble → smart chips → rating bar.

```javascript
// R57: Thumbs rating on every bot reply — records feedback in data-attr, toggles on re-click.
if (cls === 'bot') {
  var _rb = document.createElement('div');
  _rb.className = 'pb-brain__rating';
  _rb.innerHTML = '<button type="button" class="pb-brain__rate-btn" data-v="up"'
    + ' aria-label="Helpful">&#128077;</button>'
    + '<button type="button" class="pb-brain__rate-btn" data-v="down"'
    + ' aria-label="Not helpful">&#128078;</button>';
  _rb.querySelectorAll('.pb-brain__rate-btn').forEach(function(btn) {
    btn.addEventListener('click', function() {
      var _v = btn.dataset.v;
      div.dataset.ratingVal = (div.dataset.ratingVal === _v) ? '' : _v;
      _rb.querySelectorAll('.pb-brain__rate-btn').forEach(function(b) {
        b.classList.toggle('pb-brain__rate-btn--on', b.dataset.v === div.dataset.ratingVal);
      });
    });
  });
  div.appendChild(_rb);
  setTimeout(function() { _rb.classList.add('pb-brain__rating--vis'); }, 800);
}
```

**Storage in `data-rating-val`**: storing the rating as a `data-*` attribute on the
message `div` rather than in a separate JS object means the state is naturally scoped to
the DOM node's lifetime. No map cleanup needed; no off-by-one risk from index-based
approaches. If the DOM node is removed (e.g., `clearChat()`), the rating vanishes
automatically.

**Toggle mechanism**: `div.dataset.ratingVal = (div.dataset.ratingVal === _v) ? '' : _v`
— if the same button is clicked again, ratingVal is set to empty string, deselecting both
buttons. The `classList.toggle(class, b.dataset.v === div.dataset.ratingVal)` then sets
`--on` to false on both.

**800ms fade-in delay**: the bot message renders and smart chips animate in before the
rating bar appears. This gives the user time to read the response before being asked to
rate it. An immediate appearance would feel impatient.

**`aria-label` on each button**: screen readers announce "Helpful" / "Not helpful" rather
than "thumbs up emoji", which is meaningful. The emoji is decorative in this context.

**`div.appendChild(_rb)`**: the rating bar is appended to the message `div` itself (not
to `log` directly). This keeps the rating semantically attached to its message, makes DOM
traversal simpler if future code needs to find the rating for a given message, and means
`clearChat()` (which removes all children of `log`) automatically cleans up all rating
bars.

**`querySelectorAll` scoped to `_rb`**: the forEach re-queries within `_rb` to find all
rate buttons. This is robust to future DOM changes — even if `.pb-brain__rate-btn`
selectors are added elsewhere in the widget, they won't be caught here.

## main.css — Rating button styles

```css
/* R57 — per-message thumbs rating */
.pb-brain__rating{
  display:flex; justify-content:flex-end; gap:4px;
  padding:4px 8px 2px;
  opacity:0; transition:opacity .4s ease;
}
.pb-brain__rating.pb-brain__rating--vis{ opacity:1; }
.pb-brain__rate-btn{
  background:none; border:1px solid rgba(255,255,255,.12);
  border-radius:6px; cursor:pointer; font-size:14px;
  line-height:1; padding:3px 7px;
  transition:background .15s, border-color .15s, transform .1s;
  color:inherit;
}
.pb-brain__rate-btn:hover{
  background:rgba(255,255,255,.07);
  border-color:rgba(255,255,255,.28);
  transform:scale(1.15);
}
.pb-brain__rate-btn--on{
  background:rgba(255,212,0,.12);
  border-color:rgba(255,212,0,.5);
}
.pb-brain__rate-btn--on:hover{
  background:rgba(255,212,0,.18);
}
```

**`justify-content:flex-end`**: rating buttons align to the right edge of the bot bubble,
consistent with the widget's existing right-aligned UI elements (reading time chip, unread
badge). This creates a consistent right-rail for metadata and interaction controls.

**`opacity:0` → `--vis` fade**: the rating appears with a 0.4s ease-in after the 800ms
JS delay — the overall perceived delay is ~1.2s from message render. The CSS transition
makes the appearance feel smooth rather than jarring.

**`font-size:14px; line-height:1; padding:3px 7px`**: small, unobtrusive — the rating
bar does not compete with the message content visually. Touch target is ~28px height
(3+7+font+7+3 = 34px with the emoji's implicit height), just under the 44px WCAG
recommendation but acceptable for an optional feedback mechanism.

**`transform:scale(1.15)` on hover**: a subtle pop that signals interactivity without
being distracting. The 0.1s transition is faster than the background/border transitions
so the scale feels snappy.

**Gold `--on` state**: `rgba(255,212,0,.12)` background and `.5` opacity border — the
same gold family as the rest of the widget (`#ffd400`). Visually communicates "selected"
without screaming; matches the brand language established across the entire chat UI.

**`color:inherit`**: button elements have default user-agent `color` styles in some
browsers. `inherit` ensures the emoji color matches the surrounding text rather than
potentially rendering in a different color.

## brainstorm.php — 3 new intent handlers

### 0d-pre53-a) Solicitor / law firm / barrister / legal practice / conveyancer
Keywords: solicitor website, law firm website, lawyer website, legal practice website,
conveyancer website, conveyancing website, barrister website, family law website,
employment law website, immigration lawyer website, personal injury website, criminal
solicitor website, commercial law website, property lawyer website, wills solicitor
website, probate solicitor website

Response: authority + accessibility; client is frightened and confused
- SRA firm reference number (FRN) + SRA logo legally required on all pages; BSB for
  chambers; missing = regulatory action; from $100
- Practice area pages (jargon-free): conveyancing; family; employment; personal injury;
  immigration; wills/probate; commercial; each with what process involves + what client
  brings; from $200
- SRA Price Transparency Rules (Dec 2018): mandatory for residential conveyancing,
  employment tribunal, motoring, immigration (excl. asylum), wills, probate, debt
  recovery; breach = SRA compliance action; from $200
- Free initial consultation CTA: Calendly or legal intake form; primary conversion
  goal; from $150
- Solicitor profiles: SRA registration; Law Society CQS; Children Panel; Resolution-
  accredited; from $150
- Testimonials / case studies: GDPR-anonymised; Google Reviews schema; from $150
- Legal guides / FAQ hub: "how long does conveyancing take?"; SEO + authority; from $150
- Legal Aid Agency contract page: eligibility explained; practice areas covered; from $100
- Complaints procedure page (mandatory): Legal Ombudsman contact + 8-week deadline; from $80
- From $700 / $1,500+

### 0d-pre53-b) Mortgage broker / IFA / independent financial adviser / wealth manager
Keywords: mortgage broker website, mortgage adviser website, ifa website, independent
financial adviser website, financial adviser website, wealth management website, financial
planner website, pension adviser website, investment adviser website, equity release
website, buy to let mortgage website, remortgage website, first time buyer website,
financial coach website, chartered financial planner website, whole of market broker website

Response: FCA compliance + conversion calculators + trust
- FCA FRN on all pages; standard risk warnings required; FCA financial promotion sign-off
  for any rate/performance claims; missing = FCA breach; from $100
- Mortgage calculator suite: borrowing capacity; monthly payment; SDLT calculator;
  repayment vs interest-only — #1 most-used page on any broker site; from $350
- Mortgage type pages: first-time buyer; home mover; remortgage; BTL; let-to-buy;
  bridging; equity release (Equity Release Council regulated); self-employed; contractor;
  shared ownership; from $200
- Affordability + broker process: documents needed; DIP explained; timeline; from $150
- Protection insurance pages: life; critical illness; income protection; biggest
  cross-sell for mortgage brokers; from $150
- IFA / financial planning pages: pension consolidation; SIPP; IHT planning; ISA; CFP
  or Chartered status for HNW clients; from $200
- Online mortgage enquiry form: pre-qualification before first call; from $200
- Reviews: Trustpilot / Google + Vouched For (dominant IFA review platform); from $100
- Lender panel page: whole-of-market access signals; from $100
- From $650 / $1,500+

### 0d-pre53-c) Architect / architectural practice / building designer / structural engineer
Keywords: architect website, architectural practice website, building designer website,
architecture firm website, architect studio website, structural engineer website, planning
consultant website, interior architect website, landscape architect website, architect
portfolio website, riba architect website, permitted development website, house extension
architect website, new build architect website, conservation architect website,
architectural technologist website

Response: stunning visual + process transparency; client making largest financial decision
- Portfolio / case studies gallery: #1 page; residential extensions, new builds, heritage,
  commercial; before/after; drone; floor plans; planning authority + area + stats per
  project; ranks locally AND converts Instagram visitors; from $350
- ARB registration number on site (ARB protects "architect" title in UK; only ARB-
  registered may use it); RIBA Chartered Practice membership; from $100
- Services + RIBA Plan of Work stages 0-7: what happens at each stage; design-only vs
  full oversight; planning application services; clients don't know what architects do —
  this page is highest-converting; from $200
- Planning application pages: permitted development vs full PP; listed building consent;
  conservation area; England/Wales/Scotland/NI rules explained; from $200
- Fees and process: RIBA fee guidance (% construction cost vs lump sum vs hourly); VAT
  (standard-rated vs zero-rated for new build); fee calculator; from $150
- Residential project-type pages: single/double extension; loft conversion; basement;
  new build; barn conversion; listed building; each ranks for "[type] architect [city]";
  from $150/page
- Local planning authority knowledge: specific success rates in named LPA; officer
  relationships; hyper-local trust a national firm can't replicate; from $100
- 3D visualisation / CGI page: photorealistic renders; VR walkthroughs; planning CGIs;
  drone surveys; increasingly expected for six-figure decisions; from $150
- Sustainability / PassivHaus / BREEAM: Fabric First; SAP/EPC improvements; Part L; from $150
- From $650 / $1,500+

## QA results (43/43 all pass)

| Check | Result |
|-------|--------|
| R57 comment | OK |
| _rb pb-brain__rating class | OK |
| up emoji 128077 | OK |
| down emoji 128078 (dist 248, w=262) | OK |
| data-v up | OK |
| data-v down (dist 248, w=265) | OK |
| aria-label Helpful | OK |
| aria-label Not helpful (w=310) | OK |
| ratingVal toggle check | OK |
| rate-btn--on toggle | OK |
| div.appendChild(_rb) (dist 739, w=762) | OK |
| setTimeout 800ms | OK |
| pb-brain__rating--vis | OK |
| no U+2018 | OK |
| .pb-brain__rating block | OK |
| opacity:0 (dist 95, w=106) | OK |
| rating--vis opacity:1 | OK |
| .pb-brain__rate-btn block | OK |
| glass border | OK |
| hover scale (dist 113, w=126) | OK |
| rate-btn--on gold bg | OK |
| rate-btn--on gold border (dist 74, w=94) | OK |
| solicitor + conveyancer keywords | OK |
| SRA number required | OK |
| Price Transparency Rules | OK |
| Legal Aid Agency | OK |
| Legal Ombudsman | OK |
| CQS accreditation | OK |
| solicitor price line | OK |
| mortgage + IFA keywords | OK |
| FCA FRN | OK |
| Mortgage calculator (capital M) | OK |
| home repossession warning | OK |
| Vouched For | OK |
| Equity Release Council | OK |
| mortgage price line | OK |
| architect + RIBA keywords | OK |
| ARB Registration Board | OK |
| RIBA Plan of Work stages | OK |
| PassivHaus | OK |
| local planning authority content | OK |
| ARB registration number notice | OK |
| architect price line | OK |
