# DOM QA Report — R16 — 2026-06-20

## main.js — Submit guard (input + button disabled during API fetch)

Prevents double-sends and gives clear visual feedback while waiting for response.

Implementation:
```javascript
const sendBtn = form.querySelector('[type="submit"]');
const typing = addTyping();
input.disabled = true;
if (sendBtn) { sendBtn.disabled = true; sendBtn.textContent = '…'; }
try {
  // ... fetch ...
} catch (err) {
  // ...
} finally {
  input.disabled = false;
  if (sendBtn) { sendBtn.disabled = false; sendBtn.textContent = 'Send'; }
  input.focus();
}
```

Key behaviors:
- `finally` block (not just success/catch) guarantees re-enable even on unhandled errors
- `input.focus()` returns keyboard focus after response — no mouse click needed to continue
- Send button text changes to `…` while waiting (shows action is in progress)
- All three typing/voice/chip paths that call submit get the guard automatically

## main.css — Disabled state for input + submit button

```css
.pb-brain__input:disabled { opacity:.45; cursor:not-allowed; resize:none; }
.pb-brain__form button[type="submit"]:disabled { opacity:.45; cursor:not-allowed; pointer-events:none; }
```

`pointer-events:none` on the button prevents the click-through race where a double-tap
on mobile could fire a second submit before `disabled` is set. `resize:none` prevents
the handle from showing during the disabled state (redundant resize, but looks cleaner).

## brainstorm.php — 3 new intent handlers

### 0d-pre12-a) SaaS user onboarding / activation flow
Keywords: onboarding flow, user onboarding, welcome flow, getting started flow,
activation flow, onboarding checklist, onboarding wizard, new user flow, product tour,
guided tour, user activation, first-time user, setup wizard

Response:
- Welcome wizard — multi-step modal; collects preferences; saves to user meta; progress bar
- Onboarding checklist — floating widget with completion % (like Intercom); tasks link to screens
- Email drip — D0 welcome, D3 feature tips, D7 spotlight; WP Cron or Zapier; MJML-templated
- Empty-state prompts — custom illustration + CTA for "add your first item" screens
- Product tour — Shepherd.js sequential tooltips; skip/resume in localStorage
- Closes with: "What does a user need to do to reach their first 'aha moment'?"

Covered in SaaS/App $750+ build — onboarding is part of the product experience, not an add-on.

### 0d-pre12-b) Maintenance / support / monthly retainer
Keywords: maintenance plan, support retainer, monthly retainer, ongoing support,
website maintenance, update plan, care plan, post-launch support, site support,
bug fixes ongoing, manage my site, keep my site updated

Response — 3 tier pricing:
- Basic $99/mo — WP core/plugin/security updates, daily backups, uptime monitoring
- Growth $199/mo — Basic + up to 4 content edits/mo, Core Web Vitals check, priority email support
- Pro $349/mo — Growth + 2 dev hours/mo (bugs/features), A/B test setup, monthly analytics report

All plans monthly, cancel any time. Closes: "What do you currently have for maintenance?"

### 0d-pre12-c) Video / animation / explainer video
Keywords: explainer video, animated video, product video, demo video, screen recording,
lottie animation, svg animation, video editing, video production, motion graphics,
product demo, walkthrough video, promo video, rive animation, hero animation

Response split into two categories:
- UI/web animations (bundled): Lottie JSON drops; Rive state-machine (heroes/loaders); CSS/GSAP scroll-triggered
- Standalone video (referred): screen recording + voiceover from $150; animated explainer
  (partner studio) $400–$800; product demo with CTA overlay from $99

Closes: "Is this for a UI animation inside the site, or a standalone video?"

## QA results (23/23 all pass)
| Check | Result |
|-------|--------|
| sendBtn querySelector | OK |
| input.disabled = true | OK |
| sendBtn.disabled = true | OK |
| sendBtn text → ellipsis | OK |
| finally block | OK |
| input.disabled = false in finally | OK |
| sendBtn re-enabled in finally | OK |
| sendBtn text → Send | OK |
| input.focus() in finally | OK |
| input:disabled opacity CSS | OK |
| button:disabled opacity CSS | OK |
| Onboarding intent keywords | OK |
| Welcome wizard in response | OK |
| Shepherd.js product tour | OK |
| Email drip in onboarding | OK |
| Maintenance keywords | OK |
| $99/mo basic | OK |
| $199/mo growth | OK |
| $349/mo pro | OK |
| Video/animation keywords | OK |
| Lottie in response | OK |
| Rive in response | OK |
| GSAP in response | OK |
