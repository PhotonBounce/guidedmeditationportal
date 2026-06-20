# DOM QA Report — R11 — 2026-06-20

## main.js — sessionStorage chat persistence

`chatMsgs` array (declared alongside `history`) tracks every message added via `addMsg()`.

### Save flow
Inside `addMsg(text, cls)`, before the final `scrollTop`:
```javascript
chatMsgs.push({ text: text, cls: cls });
saveChat();  // → sessionStorage.setItem('pb_chat_v1', JSON.stringify(chatMsgs))
```

### Restore flow
`restoreChat()` IIFE runs immediately on brain init (before SR/mic setup):
- Reads `pb_chat_v1` from sessionStorage
- If valid array, calls `addMsg(m.text, m.cls)` for each saved message
- Errors swallowed safely — no risk on private/incognito sessions

### Clear flow
`clearChat()` called by the new "↺" button and on future resets:
- Empties `chatMsgs` and `history`
- Removes `pb_chat_v1` from sessionStorage
- Trims log DOM to the first child (preserving pre-rendered welcome message)
- Shows initial quick-reply chips again

## main.js — "New conversation" button in chat header

`newChatBtn` (class `pb-brain__newchat`) injected into `.pb-brain__head`:
- Groups with mute button and close button in a `.pb-brain__head-btns` flex row
- Removes the close button from its original position and re-appends inside the group: `[↺] [🔊] [×]`
- On click: `clearChat()`, resets voice state, stops speaking, stops recognition, hides listening banner

## main.css — newchat button styles
```css
.pb-brain__newchat{ background:none; border:1px solid rgba(255,255,255,.15); color:rgba(255,255,255,.5); width:28px; height:28px; border-radius:50%; }
.pb-brain__newchat:hover{ border-color:#ffd400; color:#ffd400; }
.pb-brain__newchat:focus-visible{ outline:1px solid rgba(255,212,0,.5); }
```

## inc/schema.php — AggregateRating on Organization

`Organization` block restructured from inline `array_filter([...])` to `$org` variable.

`AggregateRating` added conditionally:
```php
$rating_val = get_theme_mod( 'pb_rating_value', '' );
$review_cnt = get_theme_mod( 'pb_review_count', '' );
if ( $rating_val && $review_cnt ) {
    $org['aggregateRating'] = [
        '@type'       => 'AggregateRating',
        'ratingValue' => (float) $rating_val,
        'reviewCount' => (int)   $review_cnt,
        'bestRating'  => 5,
        'worstRating' => 1,
    ];
}
```
Owner configures via Customizer → `pb_rating_value` + `pb_review_count`. Not emitted until both are set (prevents fake/empty schema).

## brainstorm.php — 3 new intent handlers

### 0d-pre9-s) ADA / accessibility / WCAG compliance
Keywords: ada compliant, accessible website, screen reader, wcag, 508 compliance,
aria labels, color contrast, keyboard navigation, disability access

Response: WCAG 2.1 AA included at all tiers (semantic HTML, focus indicators, ARIA,
4.5:1 contrast, keyboard nav, VoiceOver/NVDA testing); formal audit report available as standalone

### 0d-pre9-t) Payment plan / installments
Keywords: pay in installments, payment plan, split payment, pay monthly, deposit,
50% deposit, milestone payment, pay over time, partial payment, pay in stages

Response: 50/50 milestone split standard; 33/33/33 on request for $750+ builds;
Stripe (card/Apple Pay/Google Pay) + Cash App + crypto on request

### 0d-pre9-u) Native mobile app (iOS / Android)
Keywords: ios app, android app, native app, app store, google play, react native,
flutter, apk, ipa file, cross-platform app, mobile app development

Response: PWA → SaaS $750 (offline, push notifications, home-screen installable);
React Native → $1,500–$3,000 (true native iOS+Android, App Store submission included)

## QA results (24/24 all pass)
| Check | Result |
|-------|--------|
| AggregateRating @type | OK |
| ratingValue from theme mod | OK |
| reviewCount from theme mod | OK |
| $org variable restructure | OK |
| $graph[] = $org | OK |
| ADA/accessibility intent | OK |
| WCAG 2.1 AA in response | OK |
| Payment plan intent | OK |
| 50% upfront in response | OK |
| Native mobile app intent | OK |
| React Native in response | OK |
| chatMsgs array declared | OK |
| saveChat function | OK |
| clearChat function | OK |
| chatMsgs.push in addMsg | OK |
| sessionStorage setItem | OK |
| sessionStorage removeItem | OK |
| restoreChat IIFE | OK |
| restoreChat reads sessionStorage | OK |
| newChatBtn declared | OK |
| pb-brain__newchat class | OK |
| clearChat on button click | OK |
| .pb-brain__newchat CSS | OK |
| newchat hover CSS | OK |
