# DOM QA Report — R7 — 2026-06-20

## brainstorm.php changes

### New service intents in // 4) elseif chain (+3)

| Intent | Keywords | Tier recommended |
|--------|---------|-----------------|
| Membership / subscription / gated content | membership, subscription site, gated content, members only, paid community, paywall, recurring billing | SaaS / App $750 |
| Blog / news / editorial | blog, news site, editorial site, publication, magazine site, media site | Full Site $300 |
| Booking / appointment system | appointment, booking system, online booking, calendar booking, scheduling page, consultation booking | Simple Site $115 (Calendly embed) or Full Site $300 (custom engine) |

### Improved // 6) Thanks handler
- Word limit raised from ≤4 → ≤6 (catches "thank you so much", "that sounds great")
- Added keywords: perfect, sounds good, got it

### Improved // 7) Fallback
- NEW: when $words >= 15 (long, descriptive message that matched no handler):
  → "That sounds like something worth scoping properly..." + email brief path
  → Asks "website, app, AI tool, or brand?" to re-route
- Original short fallback retained for short unmatched messages

## main.js — textarea auto-resize

- Added `resizeInput()` function: sets height to `auto` then `Math.min(scrollHeight, 120)px`
- Wired to `input` event listener — grows in real time as user types
- Textarea height reset to `auto` after each form submit (so it snaps back to 1-row)
- `overflowY: hidden` set so no scrollbar appears while resizing

## QA results (12/12 pass)
| Check | Result |
|-------|--------|
| Membership intent | OK |
| Blog/news intent | OK |
| Booking/appointment intent | OK |
| Thanks handler <=6 words | OK |
| Thanks includes "perfect"/"sounds good" | OK |
| Fallback long-message branch (>=15) | OK |
| Long fallback asks website/app/AI/brand | OK |
| Auto-resize listener added | OK |
| Auto-resize max 120px | OK |
| Textarea height reset after submit | OK |
| R6 referral handler retained | OK |
| R5 FAQ module wired in schema | OK |
