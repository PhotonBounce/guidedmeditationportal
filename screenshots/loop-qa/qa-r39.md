# DOM QA Report — R39 — 2026-06-20

## main.js — Word count badge on bot replies

Every bot message now carries a word-count annotation that appears on hover
alongside the existing timestamp. The count is computed from the plaintext content
(HTML stripped) and displayed as "N word" or "N words" (correct English
pluralisation). Gives the user an instant sense of reply length — useful for
knowing whether to skim or read carefully.

### Implementation

```javascript
// Word count badge on bot replies — gives a sense of response length at a glance.
if (cls === 'bot') {
  var _wcWords = plain(text).trim().split(/\s+/).filter(Boolean).length;
  if (_wcWords > 0) {
    var _wcBadge = document.createElement('span');
    _wcBadge.className = 'pb-brain__wc';
    _wcBadge.textContent = _wcWords + ' word' + (_wcWords !== 1 ? 's' : '');
    _wcBadge.setAttribute('aria-hidden', 'true');
    div.appendChild(_wcBadge);
  }
}
```

Placement: immediately after `div.appendChild(_tsEl)` (the timestamp), before
`log.appendChild(div)` — so the badge is the last child of the message `div`,
below the timestamp.

**`plain(text)`** — existing helper that strips HTML tags; called by the star
button and the SR live region. This ensures `<br>` and `<strong>` tags from
`$nl()` PHP responses don't inflate the word count.

**`.trim().split(/\s+/).filter(Boolean).length`**:
- `.trim()` — strips leading/trailing whitespace (prevents empty first/last token)
- `.split(/\s+/)` — splits on any whitespace run (spaces, newlines, tabs)
- `.filter(Boolean)` — removes empty strings that `split` produces on a
  whitespace-only string (belt-and-suspenders; trim() makes this rare)
- `.length` — word count

**`if (_wcWords > 0)`** — guards against a 0-word message (edge case: a bot
reply that is somehow empty after stripping; the badge wouldn't render). An empty
badge would confuse users ("0 words?" is an error signal, not a word count).

**`aria-hidden="true"`** — the badge is purely decorative context; the SR live
region (R33) already announces the full reply text. The word count is not worth
re-announcing via screen reader — it would be noise after every bot message.

### CSS

```css
.pb-brain__wc {
  display:block; font-size:9px; color:rgba(255,255,255,.2);
  text-align:right; margin-top:2px;
  opacity:0; transition:opacity .2s;
  pointer-events:none; user-select:none;
}
.pb-brain__msg:hover .pb-brain__wc { opacity:1; }
```

- Shares the hover-reveal pattern of the timestamp (`.pb-brain__ts` also has
  `opacity:0` → `opacity:1` on `.pb-brain__msg:hover`)
- `text-align:right` — aligns with the timestamp (which is `text-align:left` for
  bot messages); placing the word count right-aligned creates a reading footer:
  time at left, count at right
- `rgba(255,255,255,.2)` — very subtle; even more receded than the timestamp
  (`.25`). The count is useful information but should never compete with content
- `9px` — 1px smaller than the timestamp (`10px`), reinforcing the hierarchy:
  timestamp is the primary metadata, word count is secondary
- `margin-top:2px` — tight spacing below the timestamp

## brainstorm.php — 3 new intent handlers

### 0d-pre35-a) Subscription box / DTC subscription brand / monthly box / mystery box
Keywords: subscription box website, subscription website, subscription box, monthly box
website, dtc subscription website, direct to consumer website, product subscription
website, mystery box website, gift box subscription website, beauty box website, snack
box website, book subscription website, hobby box website, kids subscription box, pet
subscription box website

Response:
- WooCommerce Subscriptions: monthly / quarterly / annual billing; pause and skip options
  (pause > cancel for churn reduction: subscribers who skip stay; ones who can't pause
  leave); gift subscription option; first-box discount or free trial; Stripe + PayPal;
  from $600
- Box contents page: current month's box reveal with product photography; spoiler count
  ("6–8 full-size products"); retail value callout ("£80 value for £29.99");
  past box archive as proof of quality
- Quiz / personalisation flow: 3–5 question style/preference quiz before checkout;
  feeds curation logic or WooCommerce product variations; reduces perceived risk;
  from $350
- Referral programme: ReferralHero or WooCommerce Coupons; unique subscriber link;
  reward per paid referral (free box / discount / bonus product); from $300
- Subscriber portal: WooCommerce Subscriptions self-service; skip a month, update
  address, swap plan, view past boxes; eliminates support tickets; from $300
- Waitlist and scarcity: sold-out months create FOMO; email capture; countdown timer;
  from $150
- Trust and social proof: Trustpilot widget; UGC Instagram unboxing gallery (Taggbox /
  Curator); influencer mention logos; subscriber-count milestone badge
- From $700 subscriptions + box reveal + subscriber portal; $1,300+ with quiz +
  referral programme + influencer UGC

### 0d-pre35-b) Mortgage broker / IFA / financial planner / FCA-regulated advice
Keywords: mortgage broker website, mortgage adviser website, mortgage advisor website,
ifa website, independent financial adviser website, financial planner website, financial
planning website, wealth management website, financial advice website, remortgage
website, buy to let mortgage website, first time buyer website, equity release website,
protection adviser website, pension adviser website, regulated financial advice website

Response:
- FCA compliance essentials (mandatory): FCA number + "authorised and regulated by
  the Financial Conduct Authority" in footer; risk warnings on relevant pages
  ("your home may be repossessed if you do not keep up repayments on your mortgage");
  FSCS membership logo; cookie and privacy policy
- Service pages: one per specialism (residential, remortgage, BTL, first-time buyer,
  equity release, protection, pension); FAQ schema; "book a free consultation" CTA;
  from $350/page or $700 for first four
- Mortgage calculator: repayment vs interest-only; LTV + rate inputs; indicative
  monthly payment; must carry "illustrative purposes only" disclaimer; from $350
- Free consultation booking: Calendly; phone / video / in-office; pre-qualifying
  questions (purchase / remortgage / BTL, approximate value, deposit); from $250
- Client portal / secure form: document upload (payslips, bank statements, ID);
  Gravity Forms or Formstack with GDPR consent; replaces unencrypted email; from $300
- Case studies: anonymised scenarios (adverse credit, self-employed, large loan, BTL
  portfolio); builds confidence for complex-situation clients
- From $650 compliance-compliant site + service pages + booking; $1,200+ with
  calculator + portal + case studies + full service range

### 0d-pre35-c) Therapist / counsellor / psychologist / mental health practice
Keywords: therapist website, counsellor website, counselor website, psychologist website,
psychotherapist website, mental health website, cbt website, cognitive behavioural therapy
website, anxiety therapist website, depression therapist website, trauma therapist website,
bereavement counsellor website, relationship therapist website, couples therapy website,
eating disorder therapist website, adhd therapist website, private psychiatry website,
psychiatric clinic website

Response:
- Design language: calm palette (soft neutrals, muted greens, warm blues); photographer
  of therapist in actual consulting room (not stock); no clinical imagery; no aggressive
  CTAs; generous whitespace; accessibility non-negotiable (18px+ body, high contrast,
  keyboard navigation)
- Speciality pages: one per presenting issue (anxiety, depression, trauma/PTSD, OCD,
  eating disorders, bereavement, relationship difficulties, self-esteem, addiction);
  own keyword cluster; from $300/page
- Therapist profile: BACP / UKCP / BPS accreditation; modality (CBT, EMDR,
  psychodynamic, person-centred, integrative, ACT); years + supervised hours +
  professional indemnity; membership number
- Online session booking: Calendly or Jane App; session type (video / phone /
  in-person); 50-minute or 80-minute slot; first session at lower fee; Stripe; from $250
- Therapy process page: "what happens in the first session" walkthrough; most
  under-used conversion tool in therapy sites; reduces fear of the unknown
- Fees and insurance: transparent pricing (private therapy £60–£150 per session);
  BUPA / AXA / Cigna / WPA panel if applicable; EAP work
- Crisis resources: Samaritans, Crisis Text Line, MIND — required ethically and
  practically; positioned clearly without alarming general enquirers
- From $500 profile + booking; $1,000+ with speciality pages + fee schedule +
  therapy process guide + insurance

## QA results (26/27 pattern + 1 manual = 27/27 correct)

| Check | Result |
|-------|--------|
| _wcWords split filter (manual) | OK — line 664; `var _wcWords = plain(text).trim().split(/\s+/).filter(Boolean).length;` confirmed via Grep; QA pattern false-negative on `/\s+/` regex escaping |
| pb-brain__wc class | OK |
| aria-hidden on badge | OK |
| pluralisation word/words | OK |
| only for bot cls | OK |
| badge appended to div | OK |
| badge before log.appendChild | OK |
| pb-brain__wc CSS | OK |
| font-size 9px | OK |
| opacity 0 transition | OK |
| hover shows wc | OK |
| subscription box keywords | OK |
| WooCommerce Subscriptions pause + skip | OK |
| quiz personalisation flow | OK |
| referral programme ReferralHero | OK |
| retail value callout | OK |
| from $700 subscription box | OK |
| mortgage broker keywords | OK |
| FCA footer mandatory | OK |
| FSCS membership logo | OK |
| mortgage calculator disclaimer | OK |
| from $650 mortgage | OK |
| therapist keywords | OK |
| BACP UKCP BPS accreditation | OK |
| Jane App booking | OK |
| crisis resources Samaritans | OK |
| from $500 therapist | OK |
