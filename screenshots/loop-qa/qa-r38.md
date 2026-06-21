# DOM QA Report — R38 — 2026-06-20

## main.js — API fetch progress bar (NProgress-style gold line)

A 2px gold line sweeps from left to right along the very top of the chat panel
while the API fetch is in-flight. On response (or error), the bar snaps to 100%
width and fades out. Gives the user clear visual feedback that "Photon is
thinking…" without relying solely on the typing indicator dots.

### `_progressBar` element creation

```javascript
var _progressBar = document.createElement('div');
_progressBar.className = 'pb-brain__progress';
_progressBar.setAttribute('aria-hidden', 'true');
if (brain) brain.prepend(_progressBar);
```

`brain.prepend()` places it before all other children — it's the first child of
the chat panel so it sits at the top, visually under the header border. `aria-hidden`
means screen readers never encounter this decorative element.

### `_progressStart()` and `_progressDone()` functions

```javascript
function _progressStart() {
  _progressBar.style.width = '0%';
  _progressBar.style.opacity = '1';
  _progressBar.classList.add('pb-brain__progress--run');
}
function _progressDone() {
  _progressBar.classList.remove('pb-brain__progress--run');
  _progressBar.style.width = '100%';
  setTimeout(function() { _progressBar.style.opacity = '0'; }, 200);
  setTimeout(function() { _progressBar.style.width = '0%'; }, 600);
}
```

`_progressStart()` behavior:
- Resets width to `0%` (in case a previous run ended mid-animation)
- Sets opacity to `1` (makes the bar visible)
- Adds `--run` class → triggers CSS `width: 82%` transition at `2.2s
  cubic-bezier(.1,0,.4,1)` — starts fast, decelerates; reaches ~82% but never
  completes, so the user always sees it still moving (honest: it won't lie about
  progress by completing early)

`_progressDone()` behavior (in `finally` block — fires on success AND error):
- Removes `--run` → CSS falls back to `transition:opacity .3s ease` (width now
  transitions instantly since there's no width transition without `--run`)
- Sets width to `100%` → bar jumps to full-width instantly (success signal)
- After 200ms: fades opacity to 0 (smooth exit)
- After 600ms: resets width to `0%` (cleanup for next fetch, after opacity is gone)

The 200ms / 600ms cascade ensures the bar is invisible before resetting, so
the reset jump is never visible.

### Placement in form submit handler

```javascript
if (sendBtn) { sendBtn.disabled = true; sendBtn.textContent = '…'; }
_progressStart();
try {
  const r = await fetch(endpoint, { ... });
  ...
} catch (err) {
  ...
} finally {
  _progressDone();
  input.disabled = false;
  ...
}
```

`_progressStart()` is called after disabling the send button but before `try` —
ensures the animation begins at the exact moment the fetch starts. `_progressDone()`
is in `finally` so it runs whether the fetch succeeded, failed with a caught error,
or threw an uncaught exception.

## main.css — `.pb-brain__progress` / `.pb-brain__progress--run`

```css
.pb-brain__progress {
  position:absolute; top:0; left:0; height:2px;
  background:linear-gradient(90deg, rgba(255,212,0,.95), rgba(255,212,0,.3));
  width:0%; opacity:0; pointer-events:none; z-index:1;
  transition:opacity .3s ease;
}
.pb-brain__progress--run {
  transition:width 2.2s cubic-bezier(.1,0,.4,1), opacity .25s ease;
  width:82%;
}
```

- `position:absolute; top:0; left:0` — anchors to the `.pb-brain` fixed container;
  `.pb-brain` has `overflow:hidden` so the bar is clipped to the panel's
  `border-radius:18px` corners without needing its own radius
- `z-index:1` — above the message log but below any modals or toasts within brain
- Gold gradient fades from nearly opaque left to 30% right — gives a sense of
  motion even when the width isn't changing much
- `cubic-bezier(.1,0,.4,1)` — aggressive ease-out; starts at full speed, strongly
  decelerates; mimics the feeling that the server responded quickly at first then
  is doing heavier work

## brainstorm.php — 3 new intent handlers

### 0d-pre34-a) Hotel / boutique hotel / guesthouse / B&B / self-catering
Keywords: hotel website, boutique hotel website, guesthouse website, bed and
breakfast website, b&b website, self-catering website, holiday cottage website,
holiday let website, apart-hotel website, serviced apartment website, lodge website,
inn website, hotel booking website, accommodation website, hostel website,
hotel direct booking

Response: anti-OTA framing (15–25% commission reduction as the core benefit):
- Direct booking engine: Checkfront / Beds24 / Lodgify / WP Hotel Booking; real-time
  availability; rate per room type; "best rate guaranteed" badge; from $500
- Room pages: one per room type; full-screen gallery; bed config, capacity, view,
  floor; en-suite/shared; pet-friendly + accessible flags; from $350
- Packages and offers: romantic break / family / Christmas; WooCommerce or booking
  plugin add-ons (breakfast/spa/flowers/champagne); from $300
- Local area guide: attractions/restaurants/walks/transport; strengthens "[town]
  hotel" SEO; positions property as local expert
- Events and weddings: if applicable; enquiry form with dates + guest count + catering
- Review integration: TripAdvisor / Google / Booking.com score / Trustpilot above fold
- Channel manager note: Lodgify / Cloudbeds / SiteMinder for zero double-booking across
  OTAs — I integrate with chosen platform
- From $600 booking engine + rooms / $1,300+ packages + area guide + events + channel
- Closes: "how many rooms? reduce OTA dependence?"

### 0d-pre34-b) Day spa / luxury spa / wellness retreat / thermal baths
Keywords: day spa website, luxury spa website, spa website, wellness retreat website,
spa retreat website, spa hotel website, thermal spa website, spa resort website,
wellness centre website, holistic wellness website, spa day website, spa break website,
spa treatments website, massage therapy website, hydrotherapy website,
flotation therapy website, cryotherapy website

Response:
- Treatment menu: one page per category (massages/facials/body wraps/hydrotherapy/
  thermal circuit/holistic); duration, price, contraindications, booking CTA; $400
- Online booking: treatment + therapist preference + date/time + add-ons (90min
  upgrade/aromatherapy/herbal tea on arrival); Stripe deposit; pre-treatment advice
  confirmation email; $400
- Spa day/break packages: half-day/full-day/overnight; "Build your own spa day"
  selector (treatment + lunch + access level); $350
- Gift vouchers: monetary or treatment-specific; email delivery; WooCommerce or
  gift voucher plugin; Christmas, Mother's Day, birthdays; $250
- Membership: WooCommerce Subscriptions (spa access + treatment allowance + member
  rate); cancel anytime; $350
- Photography/video: serene imagery of treatment rooms + thermal pools + relaxation
  lounges; 30-60s ambient video hero; highest-ROI spend for a spa
- Wellness blog + email lead magnet: seasonal treatments, therapist interviews;
  "seasonal wellness guide" PDF lead magnet
- From $600 treatment menu + booking / $1,200+ packages + vouchers + membership + blog
- Closes: "day spa / destination retreat / spa hotel page?"

### 0d-pre34-c) Personal trainer / fitness coach / nutritionist / online coaching
Keywords: personal trainer website, personal training website, fitness coach website,
pt website, fitness coaching website, online coaching website, strength coach website,
conditioning coach website, sports coach website, nutritionist website, nutrition
coach website, dietitian website, weight loss coach website, body transformation
website, online personal trainer website, health coach website

Response:
- Transformation gallery: before/after with consent; weight/body comp/strength
  benchmarks; client quote + timeline; highest-converting trust signal for fitness
- Coaching packages: 12-week / 6-week / monthly rolling; what's included (sessions/
  nutrition plan/WhatsApp check-ins/app access); Stripe payment; $300
- Online booking: free 20-min discovery call (Calendly); session credit system; $250
- Client app integration: Trainerize / My PT Hub / TrueCoach embed or link page; $150
- Specialist niche pages: pre/postnatal / over-50s / sport-specific / weight loss /
  powerlifting / marathon prep; own keyword cluster per page; $200/page
- Credentials: REPs (UK) or NASM/ACE (US) registration; Level 3/4 PT qualification;
  first aid; Insure4Sport; regulated nutrition disclaimer if nutritionist
- Freebie lead magnet: PDF workout/meal prep/7-day challenge; Mailchimp or
  ConvertKit email sequence on download
- From $400 PT site + packages + booking / $800+ niche pages + app + lead magnet
- Closes: "in-person / online / hybrid? specialist niche?"

## QA results (29/32 pattern + 3 manual = 32/32 correct)
| Check | Result |
|-------|--------|
| _progressBar element created | OK |
| pb-brain__progress class | OK |
| aria-hidden on bar | OK |
| brain.prepend(_progressBar) | OK |
| _progressStart width + opacity (manual) | OK — multiline; both lines confirmed present |
| adds --run class on start | OK |
| _progressDone removes --run | OK |
| _progressDone sets width 100% | OK |
| opacity 0 timeout | OK |
| reset width timeout | OK |
| _progressStart called before try (manual) | OK — multiline span; confirmed in file |
| _progressDone called in finally | OK |
| pb-brain__progress CSS | OK |
| position:absolute top:0 left:0 height:2px | OK |
| gold gradient rgba(255,212,0) | OK |
| --run width:82% | OK |
| cubic-bezier(.1,0,.4,1) | OK |
| hotel keywords | OK |
| Beds24 / Lodgify | OK |
| OTA commission 15-25% (manual) | OK — pattern order was commission→15-25%, not 15-25%→commission |
| channel manager Lodgify/Cloudbeds/SiteMinder | OK |
| from $600 hotel | OK |
| spa keywords | OK |
| Build your own spa day | OK |
| Mother's Day birthdays | OK |
| WooCommerce Subscriptions cancel anytime | OK |
| from $600 spa | OK |
| PT keywords | OK |
| Trainerize / My PT Hub / TrueCoach | OK |
| REPs / NASM / ACE | OK |
| freebie lead magnet Mailchimp/ConvertKit | OK |
| from $400 PT site | OK |
