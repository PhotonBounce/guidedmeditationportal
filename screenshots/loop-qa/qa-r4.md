# DOM QA Report — R4 — 2026-06-20

## main.js — Copy reply button on bot messages

`addMsg()` modified: when `cls === 'bot'` and `navigator.clipboard` is available,
a `<button class="pb-brain__copy">` is appended inside each bot reply div.

- Click copies `plain(text)` (stripped HTML → plain text) to clipboard
- Icon flips to ✓ for 1.5 s then resets to 📋
- Hidden by default (opacity:0), revealed on parent hover or focus-visible
- Falls back gracefully when Clipboard API absent (button not injected)

## main.css — copy button + chip focus styles

- `.pb-brain__chip:focus-visible` — gold outline, keyboard-accessible
- `.pb-brain__msg--bot` — `position:relative; padding-right:28px` (space for copy btn)
- `.pb-brain__copy` — positioned absolute top-right, opacity 0 default
- `.pb-brain__msg--bot:hover .pb-brain__copy` — opacity 1 on hover
- `.pb-brain__copy:hover` — gold color
- `.pb-brain__copy:focus-visible` — visible outline for keyboard users

## brainstorm.php — 3 new intent handlers (total: 19 intent sections)

| Handler | Keywords |
|---------|---------|
| Deadline / urgency (0d-pre6) | urgent, asap, as soon as possible, need it by, rush order, can you start today |
| Already has designs (0d-pre7) | have a figma, have designs, have mockups, have wireframes, have a prototype |
| Free / low budget (0d-pre8) | for free, pro bono, no budget, low budget, can't afford, too expensive |

## QA results (15/15 pass)
| Check | Result |
|-------|--------|
| Copy btn injected on bot msg | OK |
| Copy btn clipboard.writeText | OK |
| Copy btn checkmark flash | OK |
| Copy btn class pb-brain__copy | OK |
| chip:focus-visible style | OK |
| copy btn CSS | OK |
| bot msg position:relative | OK |
| copy btn opacity:0 default | OK |
| copy btn hover opacity:1 | OK |
| Urgency handler | OK |
| Designs/mockup handler | OK |
| Low budget handler | OK |
| Tech stack handler (R3 retained) | OK |
| NDA handler (R3 retained) | OK |
| Russian Cyrillic (R2 retained) | OK |
