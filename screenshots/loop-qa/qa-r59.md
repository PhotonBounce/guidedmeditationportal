# DOM QA Report — R59 — 2026-06-20

## main.js — Niche topic tag in bot replies

When the bot sends a reply, the first 400 characters of its plaintext are scanned against
a 21-entry niche map. If a keyword matches, a small `pb-brain__topic-tag` pill is
prepended before the message text body — so it appears above the bot bubble content.
This makes the chatbot feel niche-aware and contextually intelligent.

### Implementation

```javascript
// R59: Niche topic tag — labels each bot reply with its detected service category.
if (cls === 'bot') {
  var _nicheMap = [
    {k:['solicitor','law firm','barrister','conveyancing','gdpr','legal practice'], label:'Legal'},
    {k:['mortgage broker','ifa ','financial adviser','equity release','pension'], label:'Finance'},
    {k:['architect','riba plan','arb-registered','planning application'], label:'Architecture'},
    {k:['dentist','dental practice','gdp ','orthodont','invisalign'], label:'Dental'},
    {k:['veterinary','vet ','animal hospital','rcvs'], label:'Veterinary'},
    {k:['physiotherapist','physio ','osteopath','chiropractor','hcpc'], label:'Health & Therapy'},
    {k:['funeral director','cremation','bereavement','nafd','saif'], label:'Funeral Services'},
    {k:['optician','optometrist','eye test','goc register'], label:'Optical'},
    {k:['nutritionist','dietitian','registered dietitian'], label:'Nutrition'},
    {k:['estate agent','letting agent','rightmove','valpal'], label:'Property'},
    {k:['hotel website','boutique hotel','holiday cottage','direct booking'], label:'Hospitality'},
    {k:['wedding photographer','event photographer','pixieset','portfolio gallery'], label:'Photography'},
    {k:['graphic design','brand agency','creative studio','freelance designer'], label:'Design'},
    {k:['car garage','mot centre','auto mechanic','bodyshop'], label:'Automotive'},
    {k:['catering company','private chef','mobile catering','food truck'], label:'Catering'},
    {k:['restaurant','cafe','pub ','bar ','hospitality website'], label:'Food & Drink'},
    {k:['ecommerce','woocommerce','shopify','online shop'], label:'eCommerce'},
    {k:['saas','software','app ','api '], label:'Software'},
    {k:['personal trainer','fitness','pilates','gym website'], label:'Fitness'},
    {k:['hairdresser','barber','hair salon'], label:'Hair & Beauty'},
    {k:['childminder','nursery','daycare'], label:'Childcare'},
  ];
  var _ltp = plain(text).toLowerCase().slice(0, 400);
  var _matchedNiche = null;
  for (var _ni = 0; _ni < _nicheMap.length && !_matchedNiche; _ni++) {
    for (var _nki = 0; _nki < _nicheMap[_ni].k.length; _nki++) {
      if (_ltp.indexOf(_nicheMap[_ni].k[_nki]) > -1) { _matchedNiche = _nicheMap[_ni]; break; }
    }
  }
  if (_matchedNiche) {
    var _ntag = document.createElement('span');
    _ntag.className = 'pb-brain__topic-tag';
    _ntag.textContent = _matchedNiche.label;
    div.insertBefore(_ntag, div.firstChild);
  }
}
```

**Why `.slice(0, 400)`**: PHP handler responses open with the service name bold in the
first sentence (e.g., "Graphic design / brand agency website — ..."). Limiting the scan
to 400 chars catches every handler opening without scanning the entire (potentially long)
GPT-4o-mini response, which would risk false positives from keywords mentioned in a
comparison context further down.

**21-entry niche map**: covers all major service categories in brainstorm.php, grouped
so that the most specific keywords appear first per entry (e.g., 'solicitor' before 'law
firm') to bias toward the cleaner label. The `!_matchedNiche` guard on the outer loop
stops scanning on first match.

**`plain(text)`**: same helper used by the CTA card (R58) — strips all HTML before
scanning. The PHP handler responses contain `<strong>`, `&bull;`, etc., which would
interfere with substring matching.

**`div.insertBefore(_ntag, div.firstChild)`**: inserts before the `_textBody` div. In
the rendered message, the tag pill appears above the bot response text, visually labelling
the category before the user starts reading.

**Insertion point**: the block is inserted immediately BEFORE `if (cls === 'bot' &&
navigator.clipboard)` (the copy-button block). This is after `div.appendChild(_textBody)`
completes, so `div.firstChild` is the text body element at insertion time.

**No CSS transition or animation**: the topic tag is immediately visible (opacity:1). An
animation would feel overly busy given that the typing indicator, smart chips (R54), and
CTA card (R58) already have motion. The tag is a small static label — instant display is
the right choice.

**Niche coverage cross-map** (21 labels):
Legal · Finance · Architecture · Dental · Veterinary · Health & Therapy ·
Funeral Services · Optical · Nutrition · Property · Hospitality · Photography ·
Design · Automotive · Catering · Food & Drink · eCommerce · Software · Fitness ·
Hair & Beauty · Childcare

## main.css — Topic tag

```css
/* R59 — niche topic tag in bot replies */
.pb-brain__topic-tag{
  display:inline-block;
  font-size:10px; font-weight:500;
  padding:2px 8px;
  border-radius:10px;
  margin:0 0 5px;
  background:rgba(255,212,0,.07);
  border:0.5px solid rgba(255,212,0,.38);
  color:rgba(255,212,0,.88);
  letter-spacing:.04em;
  vertical-align:middle;
}
```

**Same gold family as CTA card and smart chips**: rgba(255,212,0) at 7%/38%/88% background/
border/text. The pill is visually consistent with the widget's existing gold language without
being heavy. At 10px/500 weight it reads as a label, not a heading.

**`border-radius:10px`**: full pill shape at the tag's height (approx 18px), consistent
with smart chips.

**`margin:0 0 5px`**: 5px gap between the tag and the first line of the bot response text
provides breathing room without excessive vertical space.

**`0.5px solid border`**: sub-pixel border renders as a very fine line on high-DPI screens,
subtle and polished.

## brainstorm.php — 3 new intent handlers

### 0d-pre55-a) Graphic design studio / brand agency / creative agency / freelance designer
Keywords: graphic design website, graphic designer website, brand agency website, brand
design website, creative agency website, creative studio website, branding agency website,
design studio website, visual identity website, logo designer website, identity design
website, brand strategist website, packaging design website, freelance designer website,
illustration website, motion graphics website, infographic design website, print design
website, design portfolio website, art direction website

Response: portfolio IS the product; speed and storytelling
- Portfolio / case studies: grid or masonry; client + brief + challenge + outcome; discipline
  filter (branding, packaging, digital, motion); from $300
- Visual identity / branding page: logo; typeface; colour system; guidelines PDF; brand
  audit offer; highest-value service page; from $200
- Services breakdown: brand strategy; logo & identity; print; packaging; digital; motion;
  illustration; from $200
- About / studio story: founder; team; process; tools (Adobe CC, Figma, Procreate); from $150
- Process page: discovery → concept → refinement → delivery; reduces scope creep; from $150
- Client logos / testimonials: Google Reviews schema; from $100
- Pricing transparency: brand starter/full identity/day rate; from $100
- Blog / insight articles: colour psychology; rebranding triggers; from $100/post
- Inquiry / brief form: project type; budget; timeline; Notion / Trello; from $150
- From $550 / $1,200+

### 0d-pre55-b) Car garage / MOT centre / auto mechanic / vehicle repair / bodyshop
Keywords: car garage website, garage website, mot centre website, mot garage website,
auto mechanic website, vehicle repair website, bodyshop website, body shop website,
tyre fitter website, tyre shop website, auto repair website, mechanic website, car
servicing website, service centre website, oil change website, brake repair website,
exhaust repair website, transmission repair website, auto electrician website, car
diagnostic website

Response: local trust + online booking beats the fast-fit chains
- Online MOT / service booking: Garage Hive, GarageWire, Autoflow, or Calendly; customers
  book 11pm on Sunday; from $300
- MOT reminder email/SMS: DVSA API; automated renewal prompts; highest-ROI retention; $250
- Services pages: MOT (Class IV/VII); annual/interim/major service; brakes; tyres; exhausts;
  AC regas; diagnostics; EV/hybrid; each as own page for local SEO; from $200
- Tyre price checker: MyTyres / Blackcircles API; closes comparison shoppers; from $350
- DVSA MOT history check widget: reg-plate lookup; from $200
- Trust signals: Bosch Car Service; RAC/AA approved; IMI accreditation; Trading Standards; $100
- Google Reviews integration: schema; "[city] garage reviews" SEO; from $100
- Fleet / business vehicles: invoicing; account customer; from $150
- Vehicle collection & delivery: courtesy car; from $100
- Electric vehicle page: OZEV-certified; EV health check; from $150
- From $600 / $1,400+

### 0d-pre55-c) Catering company / private chef / mobile catering / food truck / events caterer
Keywords: catering website, catering company website, private chef website, mobile catering
website, food truck website, wedding caterer website, events catering website, corporate
catering website, buffet catering website, hog roast website, bbq catering website, street
food website, catering van website, outside catering website, finger food catering website,
canape catering website, school catering website, university catering website, film catering
website, festival catering website

Response: appetite appeal + availability confidence
- Gallery: high-res food photography; event type carousel; from $250
- Menu pages: canapés; bowl food; buffet; plated dinner; BBQ / hog roast; allergen matrix;
  Natasha's Law 2021 (mandatory allergen info on PPDS food); from $200
- Natasha's Law compliance: Food Information (Amendment) (England) Regulations 2021;
  mandatory allergen info display on menus and website; from $150
- Event types: weddings; corporate; product launches; birthdays; film/TV; festivals; from
  $150/page
- Enquiry and quote form: event date; guest count; venue; budget; dietary; filters leads; $200
- Availability calendar: Calendly; reduces phone tag; from $200
- Testimonials + real event blog: "[venue] caterer" SEO posts; from $150/post
- Food hygiene rating: 5-star FSA badge; from $80
- Corporate accounts: board lunches; training days; retainer; from $150
- Staff / chef profiles: City & Guilds; NVQ Level 3; WSET; from $100
- From $600 / $1,400+

## QA results (47/47 all pass)

| Check | Result |
|-------|--------|
| R59 comment present | OK |
| _nicheMap array | OK |
| Legal label | OK |
| Finance label | OK |
| Dental label | OK |
| Design label | OK |
| Automotive label | OK |
| Catering label | OK |
| plain(text) slice 400 | OK |
| _matchedNiche init null | OK |
| nested loop over nicheMap | OK |
| inner loop over keywords | OK |
| indexOf keyword match | OK |
| pb-brain__topic-tag class | OK |
| insertBefore(ntag, firstChild) | OK |
| topic-tag before copy-btn | OK |
| R59 CSS comment | OK |
| .pb-brain__topic-tag rule | OK |
| inline-block display | OK |
| font-size 10px | OK |
| border-radius 10px | OK |
| gold border (.38) | OK |
| gold color (.88) | OK |
| margin bottom 5px | OK |
| pre55-a present | OK |
| graphic design keywords | OK |
| brand agency keyword | OK |
| portfolio case studies (55a) | OK |
| visual identity page (55a) | OK |
| design price line | OK |
| pre55-b present | OK |
| car garage keyword | OK |
| mot centre keyword | OK |
| online MOT booking (55b) | OK |
| Bosch Car Service trust | OK |
| DVSA MOT history | OK |
| EV page (55b) | OK |
| garage price line | OK |
| pre55-c present | OK |
| catering website keyword | OK |
| private chef keyword | OK |
| Natasha's Law 2021 | OK |
| allergen matrix mention | OK |
| availability calendar (55c) | OK |
| testimonials real event blog | OK |
| catering price line | OK |
| pre55 before pre54 | OK |
