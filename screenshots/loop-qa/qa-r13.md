# DOM QA Report — R13 — 2026-06-20

## main.js — Error messages excluded from sessionStorage

Error conditions (API offline / network catch) now use class `'err'` instead of `'bot'`:

```javascript
// Was: addMsg('...', 'bot')
addMsg('Photon is offline right now. Email...', 'err');
addMsg('Connection hiccup — try again...', 'err');
```

`chatMsgs.push` gated on class:
```javascript
if (cls !== 'err') { chatMsgs.push({ text: text, cls: cls }); saveChat(); }
```

Effect: error messages render in the chat log during the session but are never saved to
`pb_chat_v1` in sessionStorage and never replayed on page reload. Conversation history
stays clean.

## main.css — .pb-brain__msg--err styling

```css
.pb-brain__msg--err{
  align-self:stretch; max-width:100%;
  background:rgba(220,50,50,.08); border:1px solid rgba(220,80,80,.25);
  color:rgba(255,190,190,.9); font-size:13px; font-style:italic;
}
```
Full-width (not bubbled), soft red border/bg, italic — visually distinct from bot and user messages.

## inc/schema.php — LocalBusiness + ProfessionalService dual-type

```php
'@type' => [ 'LocalBusiness', 'ProfessionalService' ],
```
`ProfessionalService` is a more specific subtype of LocalBusiness. Dual-typing improves:
- Google entity classification for service-based businesses
- Knowledge Panel categorization
- Compatibility with service-specific rich results

## brainstorm.php — 3 new intent handlers

### 0d-pre9-y) CMS / WordPress training
Keywords: cms training, wordpress training, how do i edit, how do i update, how to edit
content, how to add a page, show me how to, teach me, content management

Response: handoff session (30-min screen-share) included with every build; covers Gutenberg,
images, menus, testimonials, Search Console basics; extended training (1hr recorded) = $75;
asks if it's for individual or a team

### 0d-pre9-z) Photography / creative visual assets
Keywords: product photography, brand photography, lifestyle photos, photos for my site,
photo editing, photo retouching, ai images for site, need photos

Response: not in-house; Unsplash/Pexels sourcing included; AI images via Midjourney/DALL-E
(+$50–$75); existing photo retouching + WebP conversion included; photographer referral available

### 0d-pre9-z2) Print / merchandise / physical design
Keywords: business cards, flyer design, print design, brochure, banner design, merchandise
design, t-shirt design, poster design, sticker design, packaging design

Response: part of Brand service; flat prices — cards $25, flyer/poster from $35, brochure
from $55, merch from $45/item, packaging from $75; all files in web-ready + print-ready formats

## QA results (12/13 checks pass, 1 regex false-negative confirmed by grep)
| Check | Result |
|-------|--------|
| CMS training intent | OK |
| Handoff session in CMS response | OK |
| Extended CMS training $75 | OK |
| Photography intent | OK |
| AI images in photography response | OK |
| Print/merch intent | OK |
| Print-ready PDF in print response | OK |
| LocalBusiness + ProfessionalService | OK |
| addMsg offline uses err class | OK |
| addMsg hiccup uses err class | OK (confirmed by grep line 585; QA regex {0,20} too short) |
| chatMsgs excludes err class | OK |
| .pb-brain__msg--err CSS | OK |
| err italic styling | OK |
