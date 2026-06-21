# DOM QA Report — R56 — 2026-06-20

## main.js — Auto-link bare URLs in bot responses

### Feature overview

When a bot response contains bare `https://...` URLs (common in GPT-4o-mini replies or
future handler expansions), `_autoLink()` wraps them in styled `<a>` tags before the
message is rendered. The function uses a tag-first alternation to skip over existing
`<a href="...">` elements and HTML attributes, so it never double-links or corrupts markup.

### Implementation

```javascript
// R56: Auto-link bare https?:// URLs in bot response HTML without double-linking.
function _autoLink(html) {
  return html.replace(
    /(<a\b[^>]*>[\s\S]*?<\/a>|<[^>]+>)|(https?:\/\/[\w\-\.\/\?\#\=\&\%\+\:\~@,;!\*]+[\w\/\?])/gi,
    function(m, tag, url) {
      return tag ? tag
                 : '<a class="pb-brain__autolink" href="' + url
                   + '" target="_blank" rel="noopener noreferrer">' + url + '</a>';
    }
  );
}
```

Called in `addMsg()`:
```javascript
var _fmtLines = _autoLink(format(text)).split('<br>');  // R56: auto-link URLs
```

**Tag-first alternation**: the regex alternates between two groups — `(<a\b...>...<\/a>|<[^>]+>)` matches any existing HTML tag or link (captured in group 1) and `(https?://...)` matches a bare URL (captured in group 2). When the callback receives a group-1 match (`tag`), it returns it unchanged. When it receives a group-2 match (`url`), it wraps it in `<a>`. This prevents:
- Double-linking URLs already inside `<a href="...">` from the bot
- Accidentally matching partial URLs inside HTML attribute values (e.g., `href="https://..."` — the surrounding `href="` and `"` are matched by `<[^>]+>` first)

**`[\s\S]*?` in the `<a>` group**: the `*?` lazy quantifier stops at the first `</a>`, so a long bot response with multiple links is handled correctly even if they're close together.

**URL endpoint char class**: the trailing `[\w\/\?]` ensures the URL doesn't end in a bare punctuation mark (period, comma, closing parenthesis) that often follows URLs in prose — e.g., "see https://example.com." auto-links `https://example.com` without the trailing dot.

**`rel="noopener noreferrer"`**: both attributes are included. `noopener` prevents the linked page from accessing `window.opener` (security). `noreferrer` additionally suppresses the Referer header (privacy). Both are standard for `target="_blank"` links.

**`target="_blank"`**: opens external links in a new tab. Users stay in the chat while exploring linked resources.

**Harmless for PHP handler responses**: local rule-based PHP handler responses don't include bare URLs — they're prose advice with HTML entities. `_autoLink()` finds no matches and returns the HTML unchanged (no performance overhead — one regex pass regardless).

## main.css — Auto-link styles

```css
/* R56 — auto-linked URLs in bot responses */
a.pb-brain__autolink{
  color:rgba(255,212,0,.82);
  text-decoration:underline;
  text-decoration-color:rgba(255,212,0,.28);
  text-underline-offset:2px;
  word-break:break-all;
  transition:color .15s, text-decoration-color .15s;
}
a.pb-brain__autolink:hover{
  color:#ffd400;
  text-decoration-color:rgba(255,212,0,.65);
}
a.pb-brain__autolink::after{
  content:' ↗';
  font-size:.7em;
  vertical-align:super;
  opacity:.55;
}
```

**Gold at `.82` opacity**: slightly dimmer than the full `#ffd400` to distinguish auto-links
from the bot's own gold-highlighted bold text (`<strong>`). On hover they reach full
`#ffd400` to signal interactivity.

**`text-underline-offset:2px`**: raises the underline slightly off the baseline for
readability — avoids the underline merging with descenders in the URL text.

**`text-decoration-color: .28 → .65`**: low-opacity underline at rest (doesn't compete
with text) transitioning to a more visible underline on hover. Smooth `transition` on both
color and underline opacity makes the hover feel polished.

**`word-break:break-all`**: URLs can be very long. Without this, a long URL would overflow
the chat bubble width on mobile or in the chat log's constrained width.

**`::after content: ' ↗'`**: the north-east arrow at `.55` opacity is a conventional
"external link" indicator. `font-size:.7em; vertical-align:super` sizes it as a superscript
so it doesn't affect line height. Placed via pseudo-element to keep the DOM clean — no
extra span needed in JS.

**`a.pb-brain__autolink` specificity**: using the element + class selector avoids
conflicts with generic `a` styles in the theme's main stylesheet.

## brainstorm.php — 3 new intent handlers

### 0d-pre52-a) Funeral director / funeral home / crematorium / memorial services
Keywords: funeral director website, funeral home website, funeral parlour website, funeral
service website, crematorium website, funeral plans website, funeral chapel website,
memorial website, bereavement website, funeral arranger website, funeral celebrant website,
graveside service website, funeral cost website, direct cremation website, natural burial
website, green funeral website

Response: dual-mode (emergency grief vs calm pre-planning); trust above everything
- 24/7 contact line: most critical element; prominently above fold on mobile; from $100
- What to do when someone dies: step-by-step calm guide; highest-intent page on site; from $200
- Services pages: burial, cremation, direct cremation, natural/woodland burial, humanist/
  civil/religious, repatriation; CMA price transparency legally required (England & Wales
  since Sept 2021); from $200
- CMA price transparency compliance: Competition and Markets Authority; legally required
  in England and Wales; standardised price lists online; non-compliance risks investigation;
  from $150
- Pre-paid funeral plans: FCA-regulated since 2022; Safe Hands, Golden Charter, Dignity
  Plans, Co-op Funeralcare; from $200
- Memorial and tribute pages: online obituary form; live-stream for remote family; memorial
  garden; condolence messages; charitable donations in lieu of flowers; from $250
- Meet the team: NAFD / SAIF / BIFD membership logos; local roots; trust-led; from $150
- NAFD / SAIF membership: National Association of Funeral Directors / Society of Allied
  and Independent Funeral Directors; from $100
- Bereavement support resources: Cruse Bereavement Care, WAY Widowed and Young; from $150
- Area coverage / local crematorium names: ranks for "funeral director [area]"; from $100
- From $600 / $1,400+

### 0d-pre52-b) Optician / optometrist / contact lens clinic / eyewear boutique
Keywords: optician website, optometrist website, eye test website, eyewear website,
glasses website, contact lens website, optical practice website, eye care website,
spectacles website, sunglasses website, laser eye surgery website, dry eye clinic website,
myopia management website, children optician website, optical boutique website,
independent optician website

Response: NHS convenience + fashion desire — two completely different value propositions
- Online appointment booking (Acuity, Optinet): NHS vs private; OCT; children; most
  patients decide on booking convenience; from $250
- Frames/eyewear gallery or online shop: Lindberg, Silhouette, Tom Ford; virtual try-on;
  WooCommerce for prescription glasses; from $400
- Eye tests and services pages: NHS (GOS voucher info), private, children's (free <16),
  OCT, visual fields, driving standard; from $200
- GOC registration: General Optical Council; legally required to practice; registration
  number + GOC register verify link on team page; from $100
- NHS GOS voucher information: entitlement explanation; converts NHS patients who don't
  know they're eligible; from $150
- Contact lens subscription/home delivery: Acuvue, CooperVision, Alcon; recurring revenue;
  from $300
- Specialist clinic pages: dry eye (TearLab, LipiFlow); myopia management (Orthokeratology,
  MiSight); low vision; keratoconus; very low search competition; from $150/page
- Laser eye surgery referral: pre-assessment; LASIK/LASEK/SMILE co-management; from $150
- Lens technology page: varifocal, photochromic (Transitions), blue light; converts upgrades;
  from $150
- From $600 / $1,400+

### 0d-pre52-c) Nutritionist / dietitian / health coach / weight management clinic
Keywords: nutritionist website, dietitian website, nutrition coach website, health coach
website, weight management website, wellness coach website, nutrition consultant website,
sports nutritionist website, clinical nutritionist website, registered dietitian website,
functional medicine website, gut health website, eating disorder website, diabetes dietitian
website, nutrition therapy website, wellness practitioner website

Response: must establish HCPC-protected Registered Dietitian vs unprotected nutritionist
- HCPC / UKVRN / BDA / BANT registration: HCPC registration number for RDs (protected
  title, legally required); UKVRN RNutr/ANutr for nutritionists; credentials above the
  fold; from $100
- Specialism pages: gut health (IBS, IBD, SIBO, microbiome); weight management; sports;
  eating disorders (ARFID, BED — qualified specialists only); diabetes; PCOS; pregnancy;
  paediatric; oncology; each ranks for "[condition] dietitian"; from $150/page
- Services and packages: initial consultation 60-90 min; follow-ups; 6-week/12-week
  programmes; corporate nutrition; Zoom; from $200
- Booking integration: Calendly or Practice Better (specialist nutrition software);
  intake form with current medication; from $250
- Recipe hub / free resources: IBS-friendly meal plan; 7-day gut reset; email opt-in gated;
  builds mailing list; from $200
- Evidence-based blog: high health-anxious search traffic; from $100/post
- Corporate wellness page: staff webinars; canteen consultation; executive health; from $150
- Media and press page: TV/radio/podcast; national press quotes; authority differentiator;
  from $100
- NHS / GP referral pathway: EMIS/SystmOne; Choosing Wisely; from $150
- From $550 / $1,200+

## QA results (36/36 auto = 36/36 all pass)

| Check | Result |
|-------|--------|
| R56 comment on _autoLink | OK |
| _autoLink function defined | OK |
| regex has <a tag alternation | OK |
| regex has https?:// capture | OK |
| returns tag when matched | OK |
| pb-brain__autolink class in anchor | OK |
| target=_blank on autolink | OK |
| rel=noopener on autolink | OK |
| _autoLink(format(text)) in addMsg | OK |
| R56 comment on call site | OK |
| no U+2018 in file | OK |
| a.pb-brain__autolink block | OK |
| gold color on link | OK |
| text-underline-offset (dist 127+21, w=152) | OK |
| word-break break-all (dist 156+20, w=180) | OK |
| hover selector | OK |
| hover full gold #ffd400 | OK |
| ::after arrow NE | OK |
| funeral director + direct cremation keywords | OK |
| CMA price transparency note | OK |
| CMA legally required statement | OK |
| NAFD + SAIF membership | OK |
| FCA-regulated plans 2022 | OK |
| funeral price line | OK |
| optician + optometrist keywords | OK |
| GOC register verify link | OK |
| NHS GOS voucher info | OK |
| contact lens subscription | OK |
| Orthokeratology MiSight specialist | OK |
| optician price line | OK |
| nutritionist + registered dietitian keywords | OK |
| HCPC-protected title distinction | OK |
| UKVRN + BANT credentials | OK |
| SIBO + ARFID conditions | OK |
| Practice Better booking | OK |
| nutritionist price line | OK |
