# DOM QA Report — R52 — 2026-06-20

## main.js — `isBlk` bugfix + markdown table rendering + Ctrl+E export shortcut

### `isBlk` fix in `addMsg()`: block elements no longer wrapped in inline `<span>`

**Bug introduced in R51:** `format()` now returns `<ol>`, `<ul>`, `<blockquote>`, and `<table>`
elements after the placeholder restore step. These are block-level HTML elements. The
`addMsg()` function splits formatted text by `<br>` and wraps each line fragment in a
`.pb-brain__line` span. Wrapping a block-level element in an inline `<span>` is invalid
HTML — the browser promotes the block element out of the span, breaking animation delays
and the line rendering.

**Fix:**
```javascript
var _trimLn = ln.trimStart();
var isBlk = /^<(pre|ol|ul|blockquote|table)[\s>]/.test(_trimLn);
var isPreBlk = _trimLn.startsWith('<pre');
if (!isBlk && i > 0 && !/^<(pre|ol|ul|blockquote|table)[\s>]/.test(_fmtLines[i-1].trimStart())) _lh += '<br>';
_lh += isPreBlk
  ? '<div class="pb-brain__line pb-brain__line--code">' + ln + '</div>'
  : isBlk ? ln
  : '<span class="pb-brain__line" style="animation-delay:' + (i * 70) + 'ms">' + ln + '</span>';
```

**Three-way ternary**: `<pre>` blocks keep their `pb-brain__line--code` div wrapper (for
syntax highlight styling and copy-button positioning). All other block elements (`<ol>`,
`<ul>`, `<blockquote>`, `<table>`) are emitted directly — they carry their own CSS class
and don't need a wrapper. Inline content gets the animated `.pb-brain__line` span.

**`/^<(pre|ol|ul|blockquote|table)[\s>]/` regex**: the `[\s>]` after the tag name
prevents false matches on hypothetical `<prelabel>` or `<older>`. After a valid HTML
block element tag name, the next character is always a space (before an attribute) or
`>` (for self-contained open tags without attributes). This makes the regex both
correct and safe against content injection.

**`<br>` insertion logic (`if (!isBlk && ...)`)**:
- Between two inline lines: `<br>` inserted to preserve visual line breaks
- Before or after a block element: NO `<br>` — the block renders as a full-width
  element and adds its own visual spacing (CSS `margin`). Inserting `<br>` before
  a `<ul>` would add unwanted blank space above the list

### Markdown table rendering in `format()`

LLM responses comparing services, pricing, or features often use pipe-delimited tables.
Previously these appeared as garbled pipe-character text. Now they render as styled
HTML tables.

```javascript
var _tblBlocks = [];
text = text.replace(/((?:(?:^|\n)\|[^\n]+)+)/g, function(block) {
  var rows = block.trim().split('\n').filter(Boolean);
  var sepIdx = -1;
  for (var _ri = 0; _ri < rows.length; _ri++) {
    if (/^\|[-:| ]+\|$/.test(rows[_ri].trim())) { sepIdx = _ri; break; }
  }
  if (sepIdx < 1 || !rows.slice(sepIdx + 1).some(function(r) { return r.indexOf('|') > -1; })) return block;
  var _tcols = function(row) { return row.trim().replace(/^\||\|$/g, '').split('|').map(function(c) { return c.trim(); }); };
  var _tHtml = '<table class="pb-brain__tbl"><thead><tr>' +
    _tcols(rows[sepIdx - 1]).map(function(c) { return '<th>' + _fmtItem(c) + '</th>'; }).join('') +
    '</tr></thead><tbody>' +
    rows.slice(sepIdx + 1).filter(function(r) { return r.trim() && r.indexOf('|') > -1; }).map(function(row) {
      return '<tr>' + _tcols(row).map(function(c) { return '<td>' + _fmtItem(c) + '</td>'; }).join('') + '</tr>';
    }).join('') + '</tbody></table>';
  _tblBlocks.push(_tHtml); return '\x00TB' + (_tblBlocks.length - 1) + '\x00';
});
```

Restored after lists and code blocks:
```javascript
_tblBlocks.forEach(function(h, i) { text = text.replace('\x00TB' + i + '\x00', h); });
```

**`/((?:(?:^|\n)\|[^\n]+)+)/g` regex analysis:**
- `(?:^|\n)` — table starts at string start or after a newline (prevents partial matches mid-line)
- `\|[^\n]+` — one pipe character followed by any non-newline content (the row content)
- `+` on the outer group — consecutive pipe-rows collected into one block

A blank line (two consecutive newlines) breaks the match, correctly separating two
adjacent tables. The match captures the FULL table block including all rows.

**Separator row detection `^\|[-:| ]+\|$`:**
The markdown table standard requires a separator row between header and data:
```
| Col 1 | Col 2 |
|-------|-------|     ← separator: only pipes, dashes, colons, spaces
| data  | data  |
```
The regex `/^\|[-:| ]+\|$/.test(row)` detects this separator. The `:` character is
for column alignment syntax (`|:---|`, `|:---:|`, `|---:|`). Alignment information
is not currently used (all cells are left-aligned), but the separator is correctly
recognised regardless of alignment markers.

**`sepIdx < 1` guard**: if the separator is the very first row (no header row before it),
the block is not a valid table. `sepIdx < 1` catches this edge case (when `sepIdx === 0`).

**`!rows.slice(sepIdx + 1).some(r => r.indexOf('|') > -1)` guard**: if there are no
data rows (the separator is the last row), skip table rendering. This prevents a table
with only a header row and separator from rendering an empty `<tbody>`.

**`_tcols()` helper**: strips leading and trailing `|`, splits on `|`, and trims
whitespace from each cell. This handles `| data |` (with spaces around pipe) and
`|data|` (without spaces) identically.

**`_fmtItem(c)` per cell**: each cell's content goes through the inline-formatting
helper (bold, italic, del, links, HTML-escape). This correctly renders `**bold**` inside
table cells, which is common in LLM-generated comparison tables.

**Placeholder `\x00TB`**: same convention as `\x00CB` (code blocks), `\x00IC` (inline
code), and `\x00LB` (lists). Table extraction runs after list extraction and before
HTML-escape, ensuring: (a) list markers inside tables don't accidentally create nested
lists, (b) the table HTML itself isn't HTML-escaped when restored.

CSS:
```css
.pb-brain__tbl{
  display:table; width:100%; border-collapse:collapse;
  margin:6px 0; font-size:12.5px; line-height:1.45;
}
.pb-brain__tbl th{
  background:rgba(255,212,0,.1); color:rgba(255,212,0,.9);
  font-weight:600; text-align:left;
  padding:5px 10px; border-bottom:1px solid rgba(255,212,0,.25);
}
.pb-brain__tbl td{
  padding:4px 10px; color:rgba(255,255,255,.82);
  border-bottom:1px solid rgba(255,255,255,.06);
}
.pb-brain__tbl tr:last-child td{ border-bottom:none; }
.pb-brain__tbl tr:hover td{ background:rgba(255,212,0,.04); }
```

**`display:table`**: the `<table>` element with `display:table` sits inside a
`.pb-brain__line` span (since block detection in `addMsg` covers `<table` now, the span
is NOT actually added — the table emits directly). The explicit `display:table` overrides
any inherited `display:inline` from ancestor elements.

**`border-collapse:collapse`**: removes the double-border between adjacent cells. Without
it, each `th` and `td` has its own border, creating visible gaps.

**Header styling**: gold background `rgba(255,212,0,.1)` with gold text `.9` and
`font-weight:600` makes the header row visually distinct from data rows. The bottom
border `rgba(255,212,0,.25)` is the visual divider between header and body.

**Row hover**: `rgba(255,212,0,.04)` — a barely perceptible gold tint that indicates
which row the cursor is on. Makes the table feel interactive without distracting from
content reading. `pointer-events:auto` is the default, so the hover works on all cells.

### Ctrl+E → export chat (+ help panel)

```javascript
// In keydown handler (alongside Ctrl+K):
if (e.ctrlKey && (e.key === 'e' || e.key === 'E')) {
  e.preventDefault();
  if (typeof exportChat === 'function') exportChat();
}
```

Added to help panel:
```javascript
'<span><kbd>Ctrl</kbd>+<kbd>E</kbd> Export chat</span>',
```

**`typeof exportChat === 'function'` guard**: `exportChat` is a function declaration
inside the `if (brain)` block. Function declarations in blocks are technically
block-scoped in strict mode. The `typeof` check is a safe defensive guard against
reference errors in edge cases (e.g., if the widget didn't initialise for some reason).

**`e.preventDefault()`**: prevents any browser default for Ctrl+E (some browsers use
Ctrl+E to focus the search bar or address bar). The chat input must be focused for this
shortcut to fire.

## brainstorm.php — 3 new intent handlers

### 0d-pre48-a) Florist / wedding florist / flower shop / event florist website
Keywords: florist website, flower shop website, florist shop website, wedding florist
website, event florist website, floral designer website, flower delivery website, local
florist website, florist near me website, bouquet website, funeral flowers website,
sympathy flowers website, corporate flowers website, flower subscription website, dried
flower website, flower studio website

Response: photography is the product; ordering and booking journeys must be frictionless
- Gallery with occasion tabs: weddings/funerals/corporate/birthday/get-well/anniversary/
  seasonal; professional photography essential; images in situ not white-background; from $250
- Online ordering: WooCommerce or Shopify; size/price variants; same-day delivery option;
  local postcode checker; from $400
- Wedding consultation page: highest-value page; bridal bouquets/table centres/ceremony
  arches/buttonholes; consultation booking form (date+venue+style+budget); from $250
- Seasonal/occasion pages: Valentine's, Mother's Day, Christmas, Sympathy; from $100/page
- Funeral flowers page: coffin sprays, wreaths, posies, hearts, letters; direct order or
  phone; sympathetic tone; generates significant revenue, under-invested by most florists
- Flower subscriptions: Stripe recurring billing; weekly/fortnightly; gift 3-months;
  from $250
- Corporate accounts: reception flowers, events, office deliveries, invoice; from $150
- Real weddings gallery/blog: venue-specific ranking; from $100/post
- From $450 / $900+

### 0d-pre48-b) Interior designer / interior decorator / home stager / design consultant
Keywords: interior designer website, interior design website, interior decorator website,
home stager website, home staging website, interior design studio website, interior design
consultant website, residential interior designer website, commercial interior designer
website, kitchen designer website, bathroom designer website, home renovation website,
soft furnishings website, colour consultant website, e-design website, virtual interior
design website

Response: sell vision before relationship; portfolio converts before the first call
- Portfolio / project pages: before/after; room type + brief/challenge/solution; RIBA/
  BIID accreditation logo; professional photography is the #1 investment; from $300
- Style quiz or discovery call: 5-question style quiz + tailored package recommendation;
  or "Book a discovery call" form (project type + budget + timeline); 40-60% conversion
  rate for well-qualified leads; from $250
- Services and process page: full design/soft furnishings/e-design/colour consultation/
  hourly advice; step-by-step process reduces cancellations; from $200
- E-design / virtual service: flat-fee remote; questionnaire + mood board + supplier list;
  national market; from $150
- Investment / pricing page: even a price range filters low-budget enquiries; second-most-
  visited page; from $200
- Supplier and trade partnerships: trade-only pricing access; BIID/NEC3 contract; from $150
- Press page: House Beautiful, Homes & Gardens, AD features dramatically increase credibility
- Interior design blog: from $150/post
- From $550 / $1,100+

### 0d-pre48-c) Architect / architectural practice / planning consultant website
Keywords: architect website, architectural practice website, architecture firm website,
architectural design website, planning consultant website, planning architect website,
residential architect website, commercial architect website, listed building architect
website, conservation architect website, extension architect website, loft conversion
architect website, new build architect website, landscape architect website, architectural
drawings website, aps architect website

Response: portfolio credibility + clearly articulated process; clients commit to a long
relationship so trust signals are everything
- Project portfolio: drone/professional photography; RIBA stage completed; planning
  reference numbers (verifiable public record); filter by type; from $400
- Services and RIBA stages 0-7: most clients don't understand what an architect does
  beyond "draws plans" — explaining the full service justifies the fee; from $250
- ARB / RIBA registration: ARB is a legal requirement to use title "architect" in UK;
  RIBA chartered member logo; verify links; from $100
- Planning approval success rate: "98% planning approval rate" is a powerful signal;
  from $150
- Free initial consultation CTA: Calendly/MS Bookings embed; project type + address +
  brief; from $200
- Permitted development / planning guide: "Do I need planning permission for my extension?"
  highest-traffic question; ranks well; from $200
- Heritage and conservation: Grade I/II*/II LBC consent; conservation area constraints;
  IHBC/SPAB; high-fee niche; from $200
- Planning reference links to local authority portal: unique credibility signal no other
  profession can replicate; from $150
- Fees and process: 8-15% of construction cost or fixed-stage fees; publishing range
  keeps the "how much does an architect cost?" traffic on-site; from $150
- From $600 / $1,400+

## QA results (34/35 auto + 1 manual = 35/35 all pass)

| Check | Result |
|-------|--------|
| isBlk regex covers ol ul blockquote table | OK |
| isPreBlk separate variable | OK |
| block elements emitted without wrapper | OK |
| _tblBlocks array declared | OK |
| table extraction replace call | OK |
| separator row detection | OK |
| table header parsed | OK |
| th cell uses _fmtItem | OK |
| tbody data rows parsed | OK |
| td cell uses _fmtItem | OK |
| _tblBlocks restore after _listBlocks | OK |
| Ctrl+E condition in keydown | OK |
| exportChat called on Ctrl+E (manual) | OK — check window 120 misses `)` at pos 121; code is `exportChat()` confirmed in file |
| Ctrl+E in help panel shortcuts | OK |
| .pb-brain__tbl block | OK |
| table display:table | OK |
| table border-collapse | OK |
| th gold tinted background | OK |
| td border-bottom | OK |
| tr:hover gold tint | OK |
| florist and wedding florist keywords | OK |
| coffin sprays wreaths funeral flowers | OK |
| flower subscription Stripe recurring | OK |
| real wedding gallery blog florist | OK |
| florist price summary line | OK |
| interior designer and home stager keywords | OK |
| RIBA and BIID credentials | OK |
| e-design virtual remote service | OK |
| discovery call 40-60% conversion | OK |
| interior design price summary | OK |
| architect and planning consultant keywords | OK |
| ARB legally required title | OK |
| RIBA stages 0-7 | OK |
| planning approval rate signal | OK |
| architect price summary | OK |
