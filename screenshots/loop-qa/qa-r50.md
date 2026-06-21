# DOM QA Report — R50 — 2026-06-20

## main.js — `~~strikethrough~~` + `> blockquote` in format()

### `~~strikethrough~~` → `<del>`

```javascript
.replace(/~~([^~\n]+)~~/g, '<del>$1</del>')
```

Added after the `_italic_` replacement, before the link patterns.

**`[^~\n]+`** — no nested `~~` or newlines in the strikethrough span. This prevents
a message with multiple independent `~~term~~` pairs from merging into one giant struck-
through span. It also prevents cross-paragraph strikethrough (common mistake in bot
responses when two separate `~~` appear in different paragraphs — each would need to be
on the same line for the match).

**Position after `_italic_`**: both italic and strikethrough use the same exclusion
strategy (no nesting, no newlines). Running them in sequence is safe — neither pattern
can accidentally consume the other's markers, since `_text_` and `~~text~~` use
completely different delimiter characters.

**`<del>` semantics**: the `<del>` element marks text that has been deleted or
superseded. In a web agency concierge context, this is useful for bot replies that
correct a previous statement ("the price is ~~$500~~ **$450**") or cross off completed
items. Screen readers typically announce `<del>` content with "deleted" or "strikethrough",
which preserves the intended meaning.

CSS:
```css
.pb-brain__msg del { text-decoration:line-through; color:rgba(255,255,255,.38); }
```

`rgba(255,255,255,.38)` — faded white (same opacity as `.pb-brain__ts` timestamp
badge) signals "reduced importance" without being invisible. The line-through is
the primary semantic indicator; the color reduction reinforces it visually.

### `> blockquote` → `<blockquote>`

```javascript
.replace(/^&gt;\s*(.+)/gm, '<blockquote class="pb-brain__blockquote">$1</blockquote>')
```

**Why match `&gt;` not `>`**: at this point in the pipeline, the HTML-escape step
(`.replace(/>/g, '&gt;')`) has already run. The `>` at the start of a line has been
converted to `&gt;`. Matching `^&gt;` is both necessary (the literal `>` is gone) and
collision-safe (no other text in the pipeline starts with `&gt;` at line start unless
it was originally a `>` in the markdown).

**`/gm` flags**: `g` for all matches in the string; `m` for multiline mode so `^`
matches the start of each logical line (not just the start of the whole string). Without
`m`, only the very first line starting with `&gt;` would match.

**Position before `\n→<br>`**: placed after all the bold/italic/link replacements but
before `.replace(/\n/g, '<br>')`. This ensures that a multi-line blockquote:
```
> Line one
> Line two
```
becomes two adjacent `<blockquote>` elements separated by a `\n` (which then becomes
`<br>`). The `<br>` between adjacent blockquotes creates a small gap, but this is
acceptable — truly contiguous blockquote blocks are rare in bot replies. An alternative
(grouping consecutive `>` lines into a single `<blockquote>`) requires a multi-pass
approach that would complicate the single-chain pipeline.

CSS:
```css
.pb-brain__blockquote{
  display:block; margin:4px 0; padding:3px 10px;
  border-left:2px solid rgba(255,212,0,.4);
  background:rgba(255,212,0,.04); border-radius:0 4px 4px 0;
  color:rgba(255,255,255,.6); font-style:italic;
}
```

- Left border: 2px gold at `.4` opacity — the classic blockquote styling language,
  adapted to the gold palette. Visually distinct from the bot message border
  (`.2` alpha) and the inline code border (`.2` alpha).
- `rgba(255,212,0,.04)` background: barely-there gold wash; less than the bot bubble's
  `.08` background so the blockquote appears nested/indented within the bubble.
- `color:rgba(255,255,255,.6)` — slightly dimmer than message text (`.88`), further
  reinforcing the "quoted/secondary" nature of the content.
- `font-style:italic` — universal blockquote convention; readable at `rgba(.6)` white.
- `border-radius:0 4px 4px 0` — sharp left edge (flush against the border) with rounded
  right; matches the `pre.pb-brain__code-block` border-radius convention.

## main.css — Speech bubble tails (`::before` triangles)

```css
.pb-brain__msg--bot:not(.is-thinking)::before{
  content:''; position:absolute; left:-6px; top:15px;
  border:6px solid transparent;
  border-right-color:rgba(255,212,0,.2); border-left:0;
  width:0; height:0; pointer-events:none;
}
.pb-brain__msg--me{ position:relative; }
.pb-brain__msg--me::before{
  content:''; position:absolute; right:-6px; top:15px;
  border:6px solid transparent;
  border-left-color:rgba(255,255,255,.12); border-right:0;
  width:0; height:0; pointer-events:none;
}
```

**CSS triangle technique**: a zero-size element (`width:0; height:0`) with a transparent
border on all four sides (`border:6px solid transparent`) and one coloured side produces
a triangle pointing AWAY from the coloured side. The coloured side is the base of the
triangle; the element appears as a triangle pointing toward the opposite side. Bot
messages use `border-right-color` (base is on the right), producing a triangle pointing
LEFT (outward from the bubble). User messages use `border-left-color`, pointing RIGHT.

**Why `border: 6px solid transparent` then override one side**: it's not possible to
set `border-right:6px solid rgba(...)` and also have the triangle shape — the adjacent
sides (top, bottom) must also have `6px` border width to produce the triangle geometry.
The override `border-left:0` removes the base entirely (a 0-width base on the opposite
side makes the triangle fully pointed rather than having a left edge).

**`position:absolute; left:-6px; top:15px`**: the tail floats 6px outside the left
edge of the bot bubble. The `top:15px` aligns the tail's midpoint with approximately
the second line of text (`padding: 10px` top + `font-size 13.5px line-height 1.55 ≈
21px per line` → 10px + ~10px = 20px center of first line; 15px feels right visually).
This is independent of message height — the tail stays at the top of the bubble,
correct for both short and long messages.

**`:not(.is-thinking)` exclusion**: the typing indicator uses `pb-brain__msg--bot`
with `is-thinking`. Adding a speech tail to the three animated dots would look
incorrect — the tail should only appear on complete message bubbles.

**`.pb-brain__msg--bot` already has `position:relative`** (set in an earlier round).
`.pb-brain__msg--me` did not — added now so `position:absolute` on `::before` is
correctly contained within the user bubble.

**`border-right-color: rgba(255,212,0,.2)`**: matches the `.pb-brain__msg--bot` border
color exactly. The tail appears to be an extension of the bubble's left border, giving
the illusion that the bubble is "pointing" at the source. Similarly for user bubbles:
`border-left-color: rgba(255,255,255,.12)` matches `.pb-brain__msg--me` border.

**`pointer-events:none`**: the triangle is decorative. Without this, the 6px×6px area
outside the bubble would be a clickable zone with undefined behavior. Setting
`pointer-events:none` ensures clicks pass through to elements behind it.

## brainstorm.php — 3 new intent handlers

### 0d-pre46-a) Hairdresser / barber / hair salon / barbershop website
Keywords: hairdresser website, hair salon website, barber website, barbershop website,
hair stylist website, hair colourist website, hair extensions website, blow dry bar
website, keratin treatment website, afro hair salon website, men's hair salon website,
ladies hairdresser website, mobile hairdresser website, wedding hair website, bridal
hair website

Response: portfolio + instant booking + Instagram are the three conversion levers
- Team + style portfolio: healed before/after; category tabs (balayage/highlights/
  men's cut/bridal/afro); from $250
- Online booking: Treatwell / Fresha (free) / Shortcuts / Phorest; service + stylist
  selector; deposit option (reduces no-shows); from $300
- Services and pricing: all services with price + duration; short/long hair distinction;
  from $200
- Colour services: balayage/highlights/ombre/toner/colour correction; consultation
  required note for correction; from $150
- Gift vouchers: WooCommerce; Christmas/Mother's Day/birthday; "gift a blow-dry"; from $150
- Bridal/special occasion: trial + day-of; bridal party packages; from $150
- Instagram feed embed: real-time portfolio; closes discovery-to-booking gap; from $150
- From $550 / $1,000+

### 0d-pre46-b) Plumber / heating engineer / boiler installation / gas engineer website
Keywords: plumber website, plumbing website, heating engineer website, boiler installation
website, gas engineer website, gas safe engineer website, boiler repair website, central
heating website, underfloor heating website, bathroom installation website, kitchen
plumbing website, emergency plumber website, drain unblocking website, bathroom fitter
website, wet room website, plumbing company website

Response: Gas Safe registration + fixed-price emergency page are the two conversion anchors
- Emergency call-out: 24/7; response time; fixed call-out charge upfront; burst pipe/
  no hot water/boiler breakdown/drain triage; most-visited page; from $200
- Services: boiler installation/service/repair; central heating; underfloor heating;
  bathroom; wet room; kitchen; drain unblocking; leak detection; from $150/page
- Gas Safe registration: legally required; Gas Safe Register number on homepage; verify-
  online link; Boiler Plus 2018 compliance note; from $100
- Boiler brand certifications: Worcester Bosch Accredited / Vaillant Advanced / Ideal
  Installer; manufacturer extended warranty (up to 12 years) only via accredited;
  critical upsell; from $150
- Finance options: 0%/low-deposit Barclays Partner Finance / Novuna; most boiler
  replacements £2,500–£4,000; finance doubles conversion on quote pages; from $200
- Free boiler quote form: property type + boiler make/age + fault + postcode; from $200
- Area pages: "[area] plumber" / "[area] boiler installation"; from $100/page
- From $600 / $1,200+

### 0d-pre46-c) Electrician / electrical contractor / NICEIC / EV charger website
Keywords: electrician website, electrical contractor website, electrical company website,
niceic electrician website, napit electrician website, domestic electrician website,
commercial electrician website, ev charger installation website, solar panel installation
website, fuse box replacement website, electrical installation website, part p electrician
website, rewiring website, smart home electrician website, emergency electrician website

Response: NICEIC/NAPIT badge + Part P compliance + EV charger page are the highest-value
conversion elements
- NICEIC / NAPIT registration badge: on homepage + every service page; click-to-verify
  link; Part P self-certification (Building Regulations) explained; from $100
- Services: fuse box/consumer unit; full rewires; EV charger; solar PV/battery;
  smart home/Hive/Nest; outdoor lighting; commercial periodic inspection; from $150/page
- EV charger page: OZEV-approved installer required for OLEV grant (currently £350 for
  homeowners); brands (Hypervolt/Ohme/Myenergi Zappi/Andersen A2); fastest-growing
  residential electrical service; from $200
- EICR page: mandatory 5-year inspection for landlords since July 2020; fixed fee
  upfront; speeds landlord decision; from $150
- Free quote form: job type + circuits + property type + postcode + urgency; from $200
- Emergency electrician: 24/7; trips/power outage/sparking/burning smell; fixed
  call-out; highest-urgency traffic; from $200
- Area pages: "[area] electrician" / "[area] EV charger installation"; from $100/page
- From $600 / $1,200+

## QA results (31/32 auto + 1 manual = 32/32 all pass)

| Check | Result |
|-------|--------|
| strikethrough tilde-tilde pattern in format | OK |
| strikethrough produces del tag | OK |
| strikethrough after italic in pipeline | OK |
| blockquote &gt; pattern in format (manual) | OK — source has `class="pb-brain__blockquote">$1</blockquote>` with double-quoted class attr; QA needle used `class=\'...\'>` (single-quote) — false negative |
| blockquote placed before nl-to-br in format | OK |
| .pb-brain__msg del CSS | OK |
| del line-through | OK |
| .pb-brain__blockquote CSS | OK |
| blockquote border-left gold | OK |
| blockquote italic | OK |
| bot::before tail | OK |
| bot tail border-right-color gold | OK |
| bot tail left:-6px | OK |
| user msg position:relative added | OK |
| user::before tail | OK |
| user tail border-left-color | OK |
| user tail right:-6px | OK |
| hairdresser keywords | OK |
| Treatwell Fresha Shortcuts booking | OK |
| Instagram embed for hair | OK |
| from $550 hairdresser | OK |
| plumber gas engineer keywords | OK |
| Gas Safe required | OK |
| Worcester Vaillant accredited installer | OK |
| finance Barclays Novuna | OK |
| from $600 plumber | OK |
| electrician NICEIC keywords | OK |
| NICEIC NAPIT badge | OK |
| OZEV EV charger approved | OK |
| EICR landlord mandatory | OK |
| Part P compliance note | OK |
| from $600 electrician | OK |
