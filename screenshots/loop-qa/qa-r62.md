# DOM QA Report — R62 — 2026-06-21

## main.js — Text-selection "Ask about this" action pill

When the user selects text within the chat log (any bot or user message), a small gold
pill labelled "Ask about this →" appears near the selection. Clicking the pill clears
the selection, fills the input with the selected text, and auto-submits it as a new
message — exactly like the quote-reply pattern in modern AI applications.

### Implementation

```javascript
// R62: Text-selection action — floating pill lets user send selected chat text as a new message.
if (log && input && form) {
  var _selPill = document.createElement('button');
  _selPill.type = 'button';
  _selPill.className = 'pb-brain__selaction';
  _selPill.textContent = 'Ask about this →';
  _selPill.style.display = 'none';
  document.body.appendChild(_selPill);
  document.addEventListener('selectionchange', function() {
    var sel = window.getSelection();
    var txt = sel ? sel.toString().trim() : '';
    if (!txt || txt.length < 3 || txt.length > 220) { _selPill.style.display = 'none'; return; }
    var range = sel.rangeCount ? sel.getRangeAt(0) : null;
    if (!range) { _selPill.style.display = 'none'; return; }
    if (!log.contains(range.commonAncestorContainer)) { _selPill.style.display = 'none'; return; }
    var rect = range.getBoundingClientRect();
    _selPill.style.cssText = 'display:block;position:fixed;left:'
      + Math.min(rect.right, window.innerWidth - 170) + 'px;top:' + (rect.bottom + 8) + 'px;';
    _selPill.dataset.txt = txt;
  });
  _selPill.addEventListener('click', function() {
    var txt = _selPill.dataset.txt || '';
    _selPill.style.display = 'none';
    window.getSelection().removeAllRanges();
    if (!txt) return;
    input.value = txt;
    input.focus();
    form.dispatchEvent(new Event('submit', { bubbles: true }));
  });
  document.addEventListener('mousedown', function(e) {
    if (e.target !== _selPill) _selPill.style.display = 'none';
  });
}
```

**`selectionchange` event**: fires whenever the selection changes — on mouseup, keyboard
selection, programmatic changes. More reliable than `mouseup` alone (which misses keyboard
selections). Fires frequently but returns early in O(1) if `txt.length < 3`.

**3-character minimum**: prevents the pill appearing on accidental single-character clicks
or double-click word selection of very short words. 3 chars also filters out most
whitespace-only selections since `.trim()` is applied first.

**220-character maximum**: selections longer than 220 characters would produce an unwieldy
message. Beyond this the pill is hidden, nudging the user to be more selective.

**`log.contains(range.commonAncestorContainer)`**: ensures the pill ONLY appears when text
is selected within the chat log container, not from page text outside the widget (navigation,
headers, etc.). Without this guard, selecting the site title would trigger the pill.

**`window.innerWidth - 170` clamp**: prevents the pill from overflowing off-screen on the
right edge. 170px accounts for the pill's maximum width. The pill anchors to `rect.right`
(end of selection) so it appears near where the user finished dragging.

**`dataset.txt` storage**: the selected text is stored on the element between `selectionchange`
and `click` events. The `removeAllRanges()` call in the click handler clears the selection
BEFORE reading from dataset — if it read from `getSelection()` after clearing, it would get
empty string.

**`mousedown` dismiss on body**: any click outside the pill dismisses it (the `selectionchange`
would also clear it when the selection changes, but `mousedown` catches the case where the
user clicks elsewhere in the log without creating a new selection — e.g., a single click to
place the cursor).

**`e.target !== _selPill` guard**: prevents the dismiss from firing on the pill button itself.
Without this, clicking the pill would first fire `mousedown` (hiding it) before `click` fires
— the click handler would run but find `_selPill.style.display === 'none'` and the stored
text might be stale or absent.

**`document.body.appendChild(_selPill)`**: appended to body rather than the log container
because `position:fixed` positions relative to the viewport, not the container. Appending
to body also ensures z-index: 9999 stacks above all widget layers.

**`style.cssText` inline override**: the click handler needs to set `display:block` AND
the position in one tick. Using `cssText` in `selectionchange` is slightly riskier than
individual property sets (it wipes all inline styles) but acceptable here because the only
inline style this element ever has is `display:none` (set on creation) or the
`display:block;position:fixed;left:...;top:...` block set by the handler.

## main.css — Selection action pill

```css
/* R62 — text-selection action pill (Ask about this) */
.pb-brain__selaction{
  z-index:9999;
  background:rgba(20,20,40,.97);
  border:0.5px solid rgba(255,212,0,.55);
  border-radius:20px;
  color:rgba(255,212,0,.95);
  font-size:11px; font-weight:500; font-family:inherit;
  padding:5px 13px;
  cursor:pointer;
  white-space:nowrap;
  box-shadow:0 2px 10px rgba(0,0,0,.45);
  pointer-events:auto;
  transition:background .15s;
}
.pb-brain__selaction:hover{ background:rgba(255,212,0,.15); }
```

**`background:rgba(20,20,40,.97)`**: near-opaque very dark navy — slightly lighter than
pure black, consistent with the widget's dark background. At .97 opacity, underlying text
is completely obscured (contrast-safe).

**`border-radius:20px`**: full-pill shape. At the pill's ~28px height, 20px gives a clean
capsule. Consistent with smart chips and the CTA button.

**`box-shadow:0 2px 10px rgba(0,0,0,.45)`**: adds a small elevation shadow. The pill
appears floating above the chat log — the shadow distinguishes it from flat chip-style
elements and signals it's a transient contextual action.

**`font-family:inherit`**: button elements have a different default font in some browsers.
`inherit` ensures the pill text matches the widget's font.

**`pointer-events:auto`**: overrides any `pointer-events:none` that might be applied to
parent elements. The pill must be clickable even if it's positioned over elements that have
pointer events disabled.

## brainstorm.php — 3 new intent handlers

### 0d-pre58-a) Dog trainer / dog behaviourist / puppy training / canine training
Keywords: dog trainer website, dog training website, dog behaviourist website, puppy training
website, canine trainer website, dog obedience website, dog agility website, k9 trainer
website, gundog trainer website, dog socialisation website, reactive dog trainer website,
dog behaviour consultant website, force free dog trainer website, positive reinforcement dog
website, dog training classes website, dog walking training website

Response: expertise, method, and local trust
- Credentials: IMDT; APDT; CCAB (ASAB regulated); ABTC (only statutory regulated body);
  force-free vs balanced method statement; from $150
- Services: puppy classes; 1-2-1; group; adolescent dogs; reactive dog; recall; agility;
  gundog; trick; residential stays; from $200
- Behaviour consultation: ABTC behaviourist for aggression/anxiety/separation; vet-referral
  only for clinical cases; from $150
- Class booking: Calendly; BookWhen; ClassBento; from $250
- Video testimonials/case studies: before/after video clips; highest persuasion format; $200
- Resources/blog: "Why does my dog pull on lead?"; email opt-in; free download; from $100
- Location/area pages: "dog trainer [town]"; from $100/page
- FAQ: sessions needed; puppy age; e-collar statement; from $100
- From $550 / $1,200+

### 0d-pre58-b) Jeweller / bespoke jewellery / engagement ring designer / watch specialist
Keywords: jeweller website, jewellery website, bespoke jewellery website, engagement ring
website, wedding ring website, watch specialist website, goldsmith website, jewellery
designer website, fine jewellery website, custom jewellery website, diamond ring website,
antique jewellery website, estate jewellery website, jewellery repair website, watch repair
website, silversmith website

Response: desire, trust, and the story of a piece
- Portfolio/collections gallery: large-format styled photography; lifestyle shots; 360° views;
  from $350
- Bespoke design process: consultation → CAD renders → wax/3D print → setting → hallmarking;
  6-12 week timeline; journey narrative; from $250
- Engagement ring collection: solitaire; halo; three-stone; fancy shapes; metal choice;
  setting style; education for first-time buyers; from $300
- Diamond/gemstone education: 4Cs; lab-grown vs natural; GIA/IGI grading; Kimberley Process;
  ethical sourcing; from $200
- UK Assay Office hallmarking: London/Birmingham/Sheffield/Edinburgh; hallmark meanings; $100
- Repair/valuation: re-sizing; claw re-tipping; antique restoration; insurance valuation; $150
- Wishlist/save to gallery: customers share with partners; WooCommerce/Wishlist Plus; $200
- Secure checkout: PCI DSS; Stripe/SagePay; certificate of authenticity; from $300
- Instagram-feed integration: most jewellery discovery on Instagram; from $100
- From $700 / $1,800+

### 0d-pre58-c) Copywriter / content writer / freelance copywriter / ghostwriter / SEO copywriter
Keywords: copywriter website, copywriting website, content writer website, freelance copywriter
website, ghostwriter website, brand copywriter website, seo copywriter website, email copywriter
website, direct response copywriter website, b2b copywriter website, saas copywriter website,
healthcare copywriter website, finance copywriter website, advertising copywriter website, ux
writer website, marketing copywriter website

Response: demonstrate in the copy what you promise to deliver
- Homepage above fold: benefit headline (not "I'm a copywriter"); target audience named;
  the copy itself IS the portfolio; from $200
- Specialism/niche page: SaaS; fintech; healthcare; legal; B2B; DTC; email sequences; being
  specific doubles day rates; from $200
- Portfolio: before/after rewrites; PDF downloads; links to live work; Notion embed; from $200
- Process page: brief → research → draft → amends → sign-off; rounds included; from $150
- Services + pricing: per project vs per word vs day rate vs retainer; transparency eliminates
  timewasters; from $150
- Testimonials with metrics: "email open rate from 18% to 34%"; vague praise converts badly;
  from $100
- Blog with CTAs: each post ends with "Need help with your copy?"; from $100/post
- Email list/lead magnet: swipe file; brand voice guide; subject line formulas; ConvertKit;
  from $150
- Contact/brief form: project type; industry; deadline; budget; from $150
- About page: credibility background (journalism, marketing, sector); personality closes sale;
  from $100
- From $500 / $1,100+

## QA results (45/45 all pass)

| Check | Result |
|-------|--------|
| R62 comment | OK |
| _selPill created | OK |
| pb-brain__selaction class | OK |
| Ask about this text | OK |
| document.body.appendChild | OK |
| selectionchange listener | OK |
| min char guard 3 | OK |
| max char guard 220 | OK |
| log.contains check | OK |
| position:fixed | OK |
| window.innerWidth clamp | OK |
| click listener sends msg | OK |
| getSelection().removeAllRanges | OK |
| form.dispatchEvent submit | OK |
| mousedown dismiss | OK |
| R62 before form submit | OK |
| R62 CSS comment | OK |
| .pb-brain__selaction rule | OK |
| z-index 9999 | OK |
| border-radius 20px | OK |
| gold border .55 | OK |
| gold color .95 | OK |
| :hover rule | OK |
| pre58-a present | OK |
| dog trainer keywords | OK |
| ABTC regulated | OK |
| IMDT credentials | OK |
| reactive dog programme | OK |
| class booking | OK |
| dog trainer price line | OK |
| pre58-b present | OK |
| jeweller keywords | OK |
| bespoke design process | OK |
| 4Cs diamond education | OK |
| UK Assay Office | OK |
| Kimberley Process | OK |
| jeweller price line | OK |
| pre58-c present | OK |
| copywriter keywords | OK |
| specialism/niche page | OK |
| before/after rewrites | OK |
| email list/lead magnet | OK |
| ConvertKit | OK |
| copywriter price line | OK |
| pre58 before pre57 | OK |
