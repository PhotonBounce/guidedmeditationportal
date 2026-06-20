# DOM QA Report — R2 — 2026-06-20

## Brainstorm intents added (R2)
- 0) Russian/Cyrillic detection: preg_match with Unicode range + u-flag
  - Greeting / generic Russian → reply in Russian
  - Price query in Russian → Russian price list
  - Web site query in Russian → Russian recommendation
  - AI/chatbot in Russian → Russian recommendation
- 0d) WordPress/CMS: wordpress, woocommerce, wp theme, cms
- 0e) Redesign/fix: redesign, refresh, fix my site, slow website, broken site
- 0f) Timeline: how long, turnaround, delivery time, how fast, rush
- 0g) Startup/new biz: startup, new business, just starting, my idea, bootstrap

## main.js additions (R2)
- Voice mute toggle button injected into .pb-brain__head (emoji speaker icons)
  - Reads/writes localStorage pb_voice_muted
  - Calls stopSpeaking() when muted
- Orb attention pulse: .pb-orb--nudge added after 20s if brain never opened
  - Fires once per page load; cancelled if user clicks orb/open-btn

## main.css additions (R2)
- @keyframes pb-orb-nudge: triple-ring ripple outward animation
- .pb-orb--nudge: runs pb-orb-nudge 1s ease-out 2x (two rings, then stops)

## QA results (13/13 pass)
| Check | Result |
|-------|--------|
| Russian regex u-flag | OK |
| WP intent handler | OK |
| Redesign handler | OK |
| Timeline handler | OK |
| Startup handler | OK |
| Orb nudge CSS | OK |
| Orb nudge keyframe | OK |
| Orb nudge JS | OK |
| Mute btn created | OK |
| Voice mute toggle | OK |
| Chip click handler | OK |
| Chips display none | OK |
| Ctrl+/ shortcut | OK |
