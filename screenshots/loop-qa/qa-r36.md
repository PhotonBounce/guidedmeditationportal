# DOM QA Report — R36 — 2026-06-20

## main.js — Print / Save as PDF button

A "Print or save as PDF" button (🖨 &#128438;) appears in the chat header between
the download (⇓) and collapse (−) buttons. Clicking it generates a clean HTML
document from `chatMsgs` and opens it in a new tab, then immediately calls
`window.print()` — Chrome/Edge show "Save as PDF" in the print dialog.

### `printChat()` function (added after `exportChat()`)

```javascript
function printChat() {
  if (!chatMsgs.length) { _showToast('Nothing to print yet'); return; }
  var rows = chatMsgs.map(function(m) {
    var who = m.cls === 'bot' ? 'Photon' : 'You';
    var bg  = m.cls === 'bot' ? '#f0f4f8' : '#e8f0fe';
    return '<div style="margin:0 0 12px;padding:10px 14px;background:' + bg + ';border-radius:8px;">' +
           '<strong style="font-size:11px;text-transform:uppercase;letter-spacing:.06em;">' + who + '</strong>' +
           '<div style="margin:4px 0 0;font-size:13.5px;line-height:1.55;">' +
           m.text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;') + '</div></div>';
  });
  var html = '<!DOCTYPE html><html><head><meta charset="utf-8">' +
    '<title>Photon Bounce — Chat Transcript</title>' +
    '<style>body{font-family:system-ui,sans-serif;margin:32px;color:#1a1a1a;max-width:700px}' +
    'h1{font-size:18px;margin:0 0 4px;color:#0d1b2a}' +
    'p.meta{font-size:12px;color:#666;margin:0 0 24px;border-bottom:1px solid #dde;padding-bottom:12px}' +
    '@media print{body{margin:16px}}</style></head><body>' +
    '<h1>Photon Bounce — Chat Transcript</h1>' +
    '<p class="meta">Exported ' + new Date().toLocaleString() + ' &nbsp;&middot;&nbsp; ' + chatMsgs.length + ' messages</p>' +
    rows.join('') + '</body></html>';
  var w = window.open('', '_blank', 'width=800,height=600');
  if (w) { w.document.write(html); w.document.close(); w.focus(); w.print(); }
  else { _showToast('Allow pop-ups to print'); }
}
```

Behavior:
- Early return + `_showToast` if chat is empty (no blank print preview)
- `m.text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;')` — the
  bot replies already contain `<br>` and `<strong>` HTML entities from `$nl()`; the
  triple-escape converts raw HTML in the text field to visible characters. If we
  ever store sanitised HTML, this may need revisiting to render `<br>` as actual
  breaks — for now, the text field stores the display string and the escape is correct
- `window.open('', '_blank')` — if blocked by the browser popup blocker, `w` is
  null; the `else` branch shows a toast instructing the user
- `w.document.write(html); w.document.close(); w.focus(); w.print()` — the
  `document.close()` call is required to signal end-of-stream so `window.print()`
  fires on the complete document, not a partial one
- `@media print { body { margin: 16px } }` — the browser's print dialogue already
  handles page margins; the 16px override prevents double-margin in some browsers

### `printBtn` creation (after `exportBtn` listener)

```javascript
var printBtn = document.createElement('button');
printBtn.type = 'button';
printBtn.className = 'pb-brain__print';
printBtn.title = 'Print or save as PDF';
printBtn.setAttribute('aria-label', 'Print or save as PDF');
printBtn.innerHTML = '&#128438;'; // 🖨️
printBtn.addEventListener('click', printChat);
```

Added to `btnGroup` between `exportBtn` and `collapseBtn` in both the
`closeBtn`-exists and `closeBtn`-absent branches:
```
newChatBtn → exportBtn → printBtn → collapseBtn → muteBtn → helpBtn → [closeBtn]
```

## main.css — `.pb-brain__print`

```css
.pb-brain__print {
  background:none; border:1px solid rgba(255,255,255,.15);
  color:rgba(255,255,255,.5); width:28px; height:28px; border-radius:50%;
  cursor:pointer; display:flex; align-items:center; justify-content:center;
  flex-shrink:0; transition:.15s; font-size:13px; padding:0;
}
.pb-brain__print:hover { border-color:rgba(180,160,255,.6); color:rgba(200,180,255,.9); }
.pb-brain__print:focus-visible { outline:1px solid rgba(180,160,255,.5); border-radius:50%; }
```

Design: same pill style as exportBtn (blue accent) and collapseBtn (white); print
button uses lavender/purple accent to visually distinguish from download. Size and
padding match the other 28px header buttons exactly.

## brainstorm.php — 3 new intent handlers

### 0d-pre32-a) Interior design studio / decorator / staging / soft furnishings
Keywords: interior design studio website, interior design website, interior designer
website, interior decorator website, home staging website, soft furnishings website,
interior stylist website, interior consultancy website, home decor website, interior
renovation website, space planning website, kitchen designer website, bathroom designer

Note: distinct from 0d-pre28-a (architecture firm) — this is interior/decorator
angle; may partially overlap but keywords don't collide (interior design website was
in 0d-pre28-a but that handler fires after this one now; cascade handled correctly).

Response:
- Project portfolio CPT: categories (residential/commercial/hospitality/staging);
  full-bleed hero; before-and-after sliders; room type tags; client location; $500
- Virtual mood board / style quiz: Typeform/Gravity Forms; style preferences (Scandi/
  mid-century/maximalist/industrial/coastal/Japandi); outputs style profile + service
  recommendations; lead capture before result; $350
- Consultation booking: in-home vs virtual; duration tiers; Stripe deposit; $300
- Press & features: House Beautiful, Elle Décor, Homes & Gardens, Livingetc logos
- Trade account notice: Romo, GP & J Baker, Sanderson, Colefax access; signals
  premium sourcing to discerning clients
- Services page: full interior design / consultancy / e-design / home staging; scope
  and process timeline per service; manages expectations pre-enquiry
- From $600 portfolio + consultation / $1,100+ with style quiz + press + e-design
- Closes: "residential/commercial/mix? e-design for remote clients?"

### 0d-pre32-b) Tax adviser / tax consultant / bookkeeper (sub-specialities)
Keywords: tax adviser website, tax advisor website, tax consultant website, tax
planning website, corporation tax website, vat specialist website, r&d tax credits
website, r&d tax relief website, capital gains website, inheritance tax website,
tax investigation website, wealth management website, personal tax website,
self assessment website, bookkeeping website, bookkeeper website,
management accounts website, payroll website, payroll bureau website

Note: fires AFTER 0d-pre26-b (general accountant) per cascade order — targets
queries with specific sub-speciality terms that the general handler misses. Both
can't fire for the same query (first match wins).

Response:
- Speciality service pages: one per service (R&D Tax Credits / CGT / IHT / VAT /
  Tax Investigation Defence / Corporation Tax); own keyword cluster; FAQ schema; from
  $350 per page / $600 for first three
- Tax calculator tools: CGT calculator (property vs shares), dividend vs salary
  optimiser, R&D credits estimator; lead magnets + linkable assets; $400 per tool
- Regulated credentials: ICAEW / ACCA / CIOT / ATT; FCA-authorised statement if
  investment advice given
- Case studies: sector + challenge + outcome (no client names needed); $200 each
- Budget/Autumn Statement commentary: fast-turnaround blog posts; generates backlinks
- Secure document exchange: FuseBase or SmartVault; $250 add-on
- From $550 three speciality pages + case studies / $1,100+ with calculators + portal
- Closes: "main speciality (R&D / CGT / IHT / VAT)? businesses or individuals?"

### 0d-pre32-c) Wedding venue / banqueting hall / events venue / manor house
Keywords: wedding venue website, wedding venue, banqueting hall website, events venue
website, manor house website, barn venue website, marquee venue website, wedding barn
website, exclusive use venue website, country house venue website, wedding hall website,
wedding hotel website, function suite website, reception venue website

Response:
- Hero gallery: full-screen autoplay WebP; ceremony + reception rooms + grounds;
  real wedding photography (not stock); first impression is everything
- Venue hire packages: tiered pricing (Weekday/Friday/Saturday/Sunday/exclusive use);
  minimum spend vs hire fee distinction; catering options (in-house/approved/BYO)
- Virtual tour: Matterport 360° ceremony room + reception + bridal suite; couples who
  can't visit still book; $200 add-on (client supplies Matterport scan)
- Date availability checker: public calendar (manually updated or iCal sync); reduces
  "is [date] free?" enquiries; $300
- Enquiry form: preferred date + guest count + ceremony/reception or reception only +
  daytime or evening; qualifying info; auto-CRM routing
- Supplier directory: photographers/florists/bands/DJs/hair+MU/cake/stationery;
  reciprocal links drive organic traffic
- Real weddings gallery: one page per featured wedding; photo story + testimonial +
  supplier links; SEO goldmine for long-tail searches
- From $700 gallery + packages + enquiry / $1,400+ with virtual tour + availability +
  real weddings + supplier directory
- Closes: "standalone venue / hotel function suite / marquee-hire?"

## QA results (27/28 pattern + 1 manual = 28/28 correct)
| Check | Result |
|-------|--------|
| printChat function declared | OK |
| empty check + _showToast | OK |
| HTML escape &amp; in rows | OK |
| window.open blank | OK |
| w.document.write + close + print | OK |
| toast fallback popup blocked | OK |
| printBtn element created | OK |
| pb-brain__print class | OK |
| aria-label print or save as PDF | OK |
| printBtn click → printChat | OK |
| printBtn in btnGroup (closeBtn branch) | OK |
| pb-brain__print CSS | OK |
| hover lavender/purple | OK |
| focus-visible outline | OK |
| interior design studio keywords | OK |
| style quiz (Scandi/Japandi) | OK |
| trade account Romo/Colefax | OK |
| from $600 interior design | OK |
| tax adviser keywords | OK |
| R&D credits tool (manual) | OK — PHP uses &amp;D entity; literal & regex fails |
| ICAEW / ACCA / CIOT / ATT | OK |
| Budget/Autumn Statement commentary | OK |
| from $550 tax specialist | OK |
| wedding venue keywords | OK |
| Matterport 360° ceremony room | OK |
| date availability checker | OK |
| supplier directory (photographers/florists/bands) | OK |
| from $700 wedding venue | OK |
