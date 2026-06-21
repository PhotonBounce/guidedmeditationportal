# DOM QA Report — R64 — 2026-06-21

## main.js — Price highlight (TreeWalker £/$/€ pattern)

Every bot message is post-processed by a `document.createTreeWalker` pass that finds text
nodes containing price patterns (£NNN, $NNN, €NNN, with optional comma separators, decimal
cents, and k/+ suffixes). Each match is wrapped in a `.pb-brain__price-spot` span — bold,
gold-coloured, gold underline — making quoted prices instantly scannable. Hover brightens
the background to rgba(255,212,0,.12).

### Implementation

```javascript
// R64: Price highlight — TreeWalker wraps £/$/ price mentions in bot replies with .pb-brain__price-spot.
if (cls === 'bot' && _textBody) {
  var _prRe = /([£$€][0-9][0-9,]*(?:\.[0-9]{1,2})?(?:k|\+)?)/g;
  var _walker = document.createTreeWalker(_textBody, NodeFilter.SHOW_TEXT, null, false);
  var _tnodes = [];
  var _tw;
  while ((_tw = _walker.nextNode())) _tnodes.push(_tw);
  _tnodes.forEach(function(_t) {
    if (!_prRe.test(_t.nodeValue)) { _prRe.lastIndex = 0; return; }
    _prRe.lastIndex = 0;
    var _frag = document.createDocumentFragment();
    var _last = 0, _pm;
    while ((_pm = _prRe.exec(_t.nodeValue))) {
      if (_pm.index > _last) _frag.appendChild(document.createTextNode(_t.nodeValue.slice(_last, _pm.index)));
      var _psp = document.createElement('span');
      _psp.className = 'pb-brain__price-spot';
      _psp.textContent = _pm[0];
      _frag.appendChild(_psp);
      _last = _pm.index + _pm[0].length;
    }
    _prRe.lastIndex = 0;
    if (_last < _t.nodeValue.length) _frag.appendChild(document.createTextNode(_t.nodeValue.slice(_last)));
    _t.parentNode.replaceChild(_frag, _t);
  });
}
```

**TreeWalker vs innerHTML replace**: operating on text nodes directly via TreeWalker avoids
the risk of matching price patterns inside HTML attribute values or tag names. `innerHTML`
replace runs on the serialised string including `<strong>`, `href="..."`, etc. — a `$` in
an attribute URL would be incorrectly wrapped. TreeWalker only visits `#text` nodes (type 3).

**`NodeFilter.SHOW_TEXT`**: tells the walker to yield only text nodes (type=3), skipping
element nodes, comment nodes, and processing instructions. The `null` filter argument
accepts all text nodes without further filtering.

**`_prRe.lastIndex = 0` after test()**: `RegExp.prototype.test()` advances `lastIndex`
when the `g` flag is set. After calling `_prRe.test(node.value)`, `lastIndex` points past
the match. Resetting to 0 ensures `_prRe.exec()` in the `while` loop starts from the
beginning of the string rather than mid-string.

**Two-pass pattern**: first pass collects all text nodes into `_tnodes` array. We cannot
modify nodes while walking — `replaceChild` during walking mutates the tree and causes the
walker to skip or revisit nodes. Collecting then iterating is the standard pattern.

**Fragment construction**: for each text node, a DocumentFragment is built by alternating
plain text nodes (for non-price runs) with `.pb-brain__price-spot` spans (for matches).
`replaceChild(frag, _t)` atomically swaps the original text node for the fragment — the
fragment's children are moved into the parent, preserving surrounding DOM structure.

**`[0-9]` not `\d`**: backslash in regex literals written via file concatenation can be
doubled by some tooling. `[0-9]` is equivalent to `\d` and avoids the issue entirely.

**Pattern `[£$€][0-9][0-9,]*(?:\.[0-9]{1,2})?(?:k|\+)?`**:
- `[£$€]` — currency prefix; multi-currency support for international SaaS/eCommerce niches
- `[0-9][0-9,]*` — integer part with optional thousand separators (`£1,200`)
- `(?:\.[0-9]{1,2})?` — optional decimal component (`£9.99`)
- `(?:k|\+)?` — optional `k` (thousands notation, `£10k`) or `+` (range indicator, `£600+`)

**Insertion point**: after the R59 niche tag block and before the clipboard copy button
block. At this point, `_textBody` is fully populated and already appended to `div` but the
message bubble is not yet added to the chat log. All text-node operations here are on a
subtree that's not yet live in the document — reflows are deferred until `log.appendChild`.

## main.css — Price spot styling

```css
/* R64 — price highlight spans in bot replies */
.pb-brain__price-spot{
  color:rgba(255,212,0,.95);
  font-weight:500;
  border-bottom:0.5px solid rgba(255,212,0,.45);
  cursor:default;
  transition:background .12s,color .12s;
  border-radius:2px;
  padding:0 1px;
}
.pb-brain__price-spot:hover{
  background:rgba(255,212,0,.12);
  color:rgba(255,212,0,1);
}
```

**`border-bottom` underline vs `text-decoration`**: `border-bottom: 0.5px solid` gives
sub-pixel precision on retina screens and allows colour control independent of the text
colour. `text-decoration: underline` inherits colour and cannot be coloured distinctly with
the same gold opacity family.

**`cursor:default`**: the span is not interactive (no click handler), so `pointer` would
falsely suggest clickability. `default` keeps the text cursor, maintaining scannable feel
without false affordance.

**`padding:0 1px`**: tiny horizontal breathing room for the underline to extend fractionally
beyond the currency symbol on the left and the last digit on the right.

**`border-radius:2px`**: the hover background clips to a very slight rounding. At 2px on a
~14px tall span, this is barely perceptible — just enough to smooth the `.12` gold fill
against the dark bubble background.

**`transition:background .12s,color .12s`**: faster than the CTA card (`.3s`) because the
price spot is a passive indicator, not a primary call to action. `.12s` feels snappy on hover.

## brainstorm.php — 3 new intent handlers

### 0d-pre60-a) Tattoo studio / tattoo artist / piercing / fine line / neo-traditional
Keywords: tattoo website, tattoo studio website, tattoo artist website, piercing website,
piercing studio website, fine line tattoo website, neo traditional tattoo website, blackwork
tattoo website, watercolour tattoo website, realism tattoo website, japanese tattoo website,
sleeve tattoo website, custom tattoo website, cover up tattoo website, tattoo removal
website, tattoo flash website

Response: portfolio IS the business
- Portfolio with style filters: fine line; blackwork; neo-trad; Japanese; realism; watercolour;
  geometric; cover-up; healed photo examples; video flip; from £250
- Style-specific pages: each targets "[style] tattoo [city]"; explain technique + aftercare;
  from £150/page
- Artist profiles: own gallery; specialism; booking link; from £100/artist
- Booking form: reference image upload; body placement; size estimate; budget; Calendly or
  Acuity; deposit-required flow; from £250
- Custom quote form: stops time-wasting DMs; narrows to serious clients; from £150
- Aftercare page: healed photo submission CTA for social proof; from £100
- FAQ: pain; healing; price-per-hour vs flat; touch-up policy; from £100
- Flash sale / available designs: WooCommerce; from £200
- Guest artist page: from £100
- Instagram feed: tattoo studios live on Instagram; from £100
- From £550 / £1,300+

### 0d-pre60-b) Beauty therapist / lash tech / nail tech / aesthetics clinic / Fresha/JCCP
Keywords: beauty therapist website, beauty salon website, beauty clinic website, lash
technician website, lash extension website, nail technician website, nail salon website,
aesthetics clinic website, aesthetics website, semi permanent makeup website, microblading
website, dermaplaning website, skin clinic website, facial website, brow specialist website,
waxing website, spray tan website, teeth whitening website

Response: book-online beats Instagram DM every time
- Online booking: Fresha (most popular UK beauty); Treatwell; deposits take no-shows from 30%
  to under 5%; clients book at midnight; from £300
- Treatment menu pages: lashes (classic/hybrid/volume/mega-volume); nails (gel/acrylic/BIAB/
  nail art); brows (microblading/SPMU/lamination); skin (chemical peel/microneedling/
  dermaplaning/HydraFacial/LED); aesthetics (lip filler/anti-wrinkle/PRP/profhilo);
  from £150/page
- Before & after gallery: JCCP + ASA rules on aesthetic before/afters; from £200
- Aesthetics compliance: CQC registration; registered nurse/doctor credentials; Botox = POM
  prescriber on-site since July 2022 (England); JCCP/BACN membership; from £150
- Price list: transparency reduces consultation abandonment; from £100
- Gift vouchers/loyalty: Fresha vouchers; highest AOV uplift; from £150
- Blog/aftercare: "How long do lash extensions last?"; from £100/post
- Patch test policy: lashes; tints; henna; essential for insurance; from £100
- From £550 / £1,300+

### 0d-pre60-c) Life coach / executive coach / ICF PCC/MCC / mindset / NLP
Keywords: life coach website, life coaching website, executive coach website, executive
coaching website, business coach website, business coaching website, mindset coach website,
nlp coach website, nlp practitioner website, career coach website, confidence coach website,
leadership coach website, performance coach website, transformational coach website,
wellbeing coach website, accountability coach website

Response: authority, empathy, and an undeniable first step
- Niche-specific headline: "C-suite leaders who want to [outcome]" converts 3× better than
  "I'm a life coach"; from £200
- Signature programme page: 12-week; 6-month; group vs 1-2-1; milestones; investment
  transparency for high-ticket clients; from £200
- Credentials: ICF (PCC/MCC); EMCC (EIA); ILM Level 7; ANLP NLP Master Practitioner;
  essential for corporate B2B clients; from £100
- Case studies with measurable outcomes: "Promoted to MD within 9 months"; story arc:
  situation → challenge → result; from £200
- Discovery call booking: 20-min free call; Calendly; low-friction for £2,000+ programmes;
  from £200
- Lead magnet: "The 5 Mindset Shifts" PDF; email list; ConvertKit/Mailchimp; highest ROI
  acquisition channel for coaches; from £150
- Speaking & media page: podcast appearances; keynote topics; press logos; from £150
- Retreat/group event: Eventbrite or Stripe direct; from £150
- Blog/podcast embed: thought leadership; SEO; from £100/post
- From £600 / £1,400+

## QA results (31/31 all pass)

| Check | Result |
|-------|--------|
| R64 comment | OK |
| cls===bot guard | OK |
| _prRe with backslash-dot | OK |
| _prRe with backslash-plus | OK |
| createTreeWalker | OK |
| NodeFilter.SHOW_TEXT | OK |
| _tnodes array | OK |
| pb-brain__price-spot class | OK |
| createDocumentFragment | OK |
| replaceChild frag | OK |
| R64 after R59 | OK |
| R64 before clipboard | OK |
| R64 CSS comment | OK |
| .price-spot rule | OK |
| gold .95 text | OK |
| border-bottom underline | OK |
| :hover bg gold | OK |
| pre60-a tattoo | OK |
| tattoo keywords | OK |
| Fresha/Treatwell | OK |
| tattoo price | OK |
| pre60-b beauty | OK |
| beauty keywords | OK |
| JCCP Botox July 2022 | OK |
| beauty price | OK |
| pre60-c coach | OK |
| coach keywords | OK |
| ICF EMCC credentials | OK |
| ConvertKit lead magnet | OK |
| coach price | OK |
| pre60 before pre59 | OK |
