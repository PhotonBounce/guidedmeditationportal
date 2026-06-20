# DOM QA Report — R10 — 2026-06-20

## main.js — Voice "Listening…" indicator in chat log

Two new helper functions:
- `showListeningBanner()` — injects `.pb-brain__listening` div with pulsing dot + "Listening…" text into the chat log; guarded against duplicate injection; `aria-live="assertive"` for screen readers; auto-scrolls log to bottom
- `hideListeningBanner()` — removes `.pb-brain__listening` from log

Wired into all mic state transitions:
| Trigger | Change |
|---------|--------|
| micBtn click → start | `showListeningBanner()` |
| micBtn click → stop | `hideListeningBanner()` |
| `rec.onresult` (transcript received) | `hideListeningBanner()` before submit |
| `rec.onerror` | `hideListeningBanner()` |
| `rec.onend` | `hideListeningBanner()` |
| `maybeListen()` (auto re-listen after TTS) | `showListeningBanner()` |

## main.css — Listening indicator styles

```css
.pb-brain__listening{ display:flex; align-items:center; gap:8px; padding:8px 14px; color:#ffd400; font-size:13px; align-self:flex-start; }
.pb-brain__listen-dot{ width:8px; height:8px; border-radius:50%; background:#ffd400; animation:pb-listen-pulse 1s ease-in-out infinite; }
@keyframes pb-listen-pulse{ 0%,100%{ transform:scale(1); opacity:.55; } 50%{ transform:scale(1.5); opacity:1; } }
```

## main.css — Mobile safe-area insets (iOS notch/home indicator)

The 520px breakpoint now includes:
- `bottom: calc(68px + env(safe-area-inset-bottom, 0px))` — drawer doesn't overlap the iOS home indicator
- `height: min(82vh, calc(100dvh - 110px - env(safe-area-inset-bottom, 0px)))` — uses 100dvh (dynamic viewport height — excludes iOS Safari chrome) instead of 100vh
- `border-radius: 14px` — slightly tighter radius on mobile

## brainstorm.php — 3 new intent handlers

### 0d-pre9-p) Availability / when can you start
Keywords: when can you start, when are you available, how soon can you, are you available,
available now, lead time, waitlist, kickoff date, taking on clients

Response: Micro/Simple Site 3-5 business days, E-commerce/SaaS 1-2 weeks with discovery
call, retainers first-come-first-served; invites scope share → 24-hour estimate; CTA to book a call

### 0d-pre9-q) Contact form / lead capture page
Keywords: contact form, lead form, lead capture, opt-in form, coming soon page, waitlist page,
squeeze page, sign up form, email capture, signup form

Response: included at every tier (no extras); Micro Page $40 = ideal for standalone opt-in/coming
soon; Simple Site includes full contact page + SMTP + Mailchimp/ConvertKit; GDPR honeypot redirect
all included; note on Micro Page for paid traffic landing pages

### 0d-pre9-r) Migration / site rebuild from another platform
Keywords: moving from wix/squarespace/shopify, leaving wix, migrate from, site migration,
content migration, switch to wordpress, switch from wix/squarespace

Response: content migration (pages/posts/images/301 redirects), domain transfer (zero downtime),
SEO preservation (permalink mapping, canonical tags, Search Console re-verify), design approach
your call; migration bundled in site build price — no surcharge; asks which platform + page count

## QA results (18/19 pattern checks + 1 manual verify)
| Check | Result |
|-------|--------|
| Availability intent | OK |
| Availability 3-5 business days | OK |
| Lead capture intent | OK |
| Lead capture Micro Page $40 | OK |
| Migration intent (moving from wix) | OK |
| Migration 301 redirects | OK |
| Migration Search Console re-verified | OK |
| showListeningBanner defined | OK |
| hideListeningBanner defined | OK |
| showListeningBanner wired to rec.start() | OK |
| hideListeningBanner in rec.onresult | OK |
| hideListeningBanner in rec.onerror | OK (confirmed by grep line 514; QA regex too short) |
| showListeningBanner in maybeListen | OK |
| Listening banner aria-live assertive | OK |
| .pb-brain__listening CSS | OK |
| .pb-brain__listen-dot CSS | OK |
| pb-listen-pulse animation | OK |
| safe-area-inset-bottom in mobile CSS | OK |
| 100dvh in mobile CSS | OK |
