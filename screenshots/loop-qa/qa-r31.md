# DOM QA Report — R31 — 2026-06-20

## main.js — Enter to send / Shift+Enter for newline

The shortcuts panel already listed "Enter = Send" and "Shift+Enter = New line"
(added in R23), but the actual handler was missing. R31 wires it up:

```javascript
// Enter sends; Shift+Enter inserts a newline (textarea default blocked).
input.addEventListener('keydown', function(e) {
  if (e.key === 'Enter' && !e.shiftKey && !e.ctrlKey && !e.altKey && !e.metaKey) {
    e.preventDefault();
    if (input.value.trim() && form) {
      form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    }
  }
});
```

Behavior:
- `!e.shiftKey`: Shift+Enter is NOT intercepted — browser inserts a newline as expected
- `!e.ctrlKey && !e.altKey && !e.metaKey`: other modifier+Enter combos (Ctrl+Enter,
  Cmd+Enter on Mac, Alt+Enter) are also left alone — future-proof for IME composition
- Guard: `input.value.trim()` — pressing Enter on an empty textarea does nothing;
  no empty message submitted, no API call wasted
- `form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }))` —
  fires the same async form-submit handler that the Send button triggers, so all
  existing logic (typing indicator, title change, disable/re-enable, history) runs
  exactly as if the user clicked Send

QA note: the `prevents default` regex used `{0,60}` between the `'Enter'` condition
string and `e.preventDefault`, but the full condition guard (all modifier keys) spans
~75 characters + newline + indentation. Verified at lines 377-378 that the code is
correct — false-negative only.

## brainstorm.php — 3 new intent handlers

### 0d-pre27-a) Childcare / nursery / daycare / preschool / after-school
Keywords: childcare website, nursery website, daycare website, preschool website,
after-school website, childminder website, kindergarten website, primary school
website, school website, early years website, montessori website, childcare centre,
kids club website, creche website

Response:
- Ofsted / inspection rating badge: prominently displayed; current cert PDF linked;
  immediately builds trust with prospective parents
- Enrolment enquiry form: child DOB, start date, days required, dietary/medical notes;
  routed to setting manager; from $250
- Waiting list: WPForms with auto-confirm + admin dashboard; from $200
- Virtual tour: 360° gallery or video walkthrough; key for parents who can't visit
- Parent portal link: Famly, Kinderly, Tapestry, Brightwheel — auth by platform
- Safeguarding page: named DSL, policy PDF, GDPR notice; required by Ofsted/EYFS
- Term dates calendar: The Events Calendar plugin; holiday club + inset days
- From $400 solo childminder / $800+ multi-room nursery
- Closes: "capacity and Ofsted (UK) / state-licensed (US)?"

### 0d-pre27-b) Cleaning / home services / trades / maintenance company
Keywords: cleaning website, cleaning company website, home services website,
tradesman website, plumber website, electrician website, handyman website,
painter decorator website, landscaping website, gardening website,
window cleaning website, pest control website, maintenance website,
cleaning business website

Response:
- Instant quote form: property type/size/frequency/postcode → real-time estimate +
  lead email capture; highest-converting element for cleaning; from $350
- Online booking: service + frequency (one-off/weekly/fortnightly) + date/time +
  Stripe upfront or card-on-file for recurring; SMS + email reminders; from $400
- Before/after gallery: filterable by job type; WebP lazy-load; social proof
- Service area pages: one per town/postcode; "cleaning company [area]" clusters;
  Local Business JSON-LD; Google Maps
- Google Reviews widget: live pull; star badge in header; key trust signal
- Team / vetting page: DBS-checked / police-vetted badges; named operatives with photo
- Franchise / multi-location: subdomain or subfolder per location; $250/location add-on
- From $450 solo / $900+ team with booking + quote + multi-area SEO
- Closes: "domestic / commercial / trade service? Instant booking or lead-capture?"

### 0d-pre27-c) Funeral home / memorial / celebrant / bereavement services
Keywords: funeral home website, funeral director website, memorial website,
celebrant website, bereavement website, funeral services website, cremation website,
burial services website, funeral parlour website, death doula website,
obituary website, grief counselling website

Response:
- Design language: muted palette, generous white space, serif type, no aggressive
  CTAs; must feel calming and dignified; from $600
- Service pages: burial, cremation, direct cremation, celebration of life,
  repatriation; with clear pricing (required by FCA regulation 2021 in England+Wales)
- Pre-need / pre-planning: funeral plan enquiry + downloadable guide + links to
  NAFD / FPA-registered plan providers
- Online obituary / tribute: password-protected per family; photo gallery, memory
  wall comments, candle lighting; families share with friends; from $300
- Out-of-hours contact: 24/7 phone number prominently displayed; click-to-call;
  never behind a contact form alone
- Bereavement resources: Cruse, Sue Ryder; practical "what to do" checklist
- WCAG 2.1 AA: large body text (18px+), high-contrast mode, print stylesheet
- From $600 single-location / $1,200+ with tribute pages + pre-need + multi-location
- Closes: "funeral home / celebrant / memorial artist / bereavement counsellor?"

## QA results (21/22 pattern + 1 manual = 22/22 correct)
| Check | Result |
|-------|--------|
| Enter keydown listener | OK |
| e.preventDefault() (manual verify) | OK — multiline span exceeded regex {0,60} |
| guards empty input | OK |
| dispatches submit event | OK |
| metaKey guard | OK |
| Shift+Enter in shortcuts panel | OK |
| childcare keywords | OK |
| Ofsted badge | OK |
| Famly/Kinderly/Tapestry/Brightwheel | OK |
| safeguarding DSL page | OK |
| from $400 childminder | OK |
| cleaning keywords | OK |
| instant quote form | OK |
| DBS-checked vetting | OK |
| service area pages | OK |
| from $450 solo trader | OK |
| funeral keywords | OK |
| FCA regulation 2021 | OK |
| online obituary / tribute | OK |
| 24/7 out-of-hours | OK |
| WCAG 2.1 AA 18px | OK |
| from $600 funeral home | OK |
