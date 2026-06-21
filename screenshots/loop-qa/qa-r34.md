# DOM QA Report — R34 — 2026-06-20

## main.js — "Enter ↵ to send" typing-pause hint

After the user types something and pauses for 800ms without sending, a subtle hint
appears below the input: **"Enter ↵ to send · Shift+Enter for new line"**. It hides
on blur, on submit, and instantly when the input is cleared. Pure discoverability UX
— reveals the keyboard shortcut added in R31 to users who might not have noticed it.

```javascript
// "Enter ↵ to send" hint — reveals after 800ms typing pause, hides on blur or submit.
var _sendHint = document.createElement('span');
_sendHint.className = 'pb-brain__send-hint';
_sendHint.setAttribute('aria-hidden', 'true');
_sendHint.innerHTML = 'Enter &#8629; to send &nbsp;&middot;&nbsp; Shift+Enter for new line';
if (form) form.appendChild(_sendHint);
var _hintTimer = null;
input.addEventListener('input', function() {
  clearTimeout(_hintTimer);
  _sendHint.classList.remove('pb-brain__send-hint--vis');
  if (input.value.trim()) {
    _hintTimer = setTimeout(function() { _sendHint.classList.add('pb-brain__send-hint--vis'); }, 800);
  }
});
input.addEventListener('blur', function() {
  clearTimeout(_hintTimer); _sendHint.classList.remove('pb-brain__send-hint--vis');
});
```

In the form submit handler, after `input.value = '';`:
```javascript
clearTimeout(_hintTimer); _sendHint.classList.remove('pb-brain__send-hint--vis');
```

Behavior:
- 800ms debounce — only appears when the user has paused (not while actively typing)
- Clears immediately on `input` event if `input.value.trim()` is empty (backspace-to-empty)
- `blur` listener: hides the hint if the user clicks outside the input without sending —
  avoids showing a stale hint when they reopen the drawer later
- Submit handler clear: covers the Enter-to-send path (Enter dispatches 'submit' directly,
  the submit handler clears the field — without this line the hint might briefly flash
  visible after submit before the next `input` event fires)
- `aria-hidden="true"`: hint is decorative/instructional only — screen reader users already
  see the shortcuts panel (added in R23); no need to announce the hint as live text
- `var _sendHint` and `var _hintTimer` are declared with `var` inside the `if (input) {}`
  block; `var` is function-scoped in non-strict JS so both variables are hoisted to the
  outer IIFE and are accessible inside the form `submit` async handler
- No timer leaks: every `input` event first calls `clearTimeout(_hintTimer)` before
  optionally setting a new one; `blur` and `submit` also always call `clearTimeout`

## main.css — .pb-brain__send-hint

```css
.pb-brain__send-hint {
  display:block; font-size:10px; line-height:1.4;
  color:rgba(255,255,255,.28); padding:2px 10px 5px;
  opacity:0; transition:opacity .22s ease;
  pointer-events:none; user-select:none;
}
.pb-brain__send-hint--vis { opacity:1; }
```

Design notes:
- `rgba(255,255,255,.28)` — very muted; appears only when needed, doesn't compete
  with the message log or input
- `opacity:0` default + `transition` — the class toggle produces a smooth 220ms fade
- `pointer-events:none; user-select:none` — hint text cannot be clicked or selected,
  so it can't intercept taps on mobile
- `display:block` — sits on its own line inside the form, below the input

## brainstorm.php — 3 new intent handlers

### 0d-pre30-a) Optician / optometrist / eyewear / ophthalmology
Keywords: optician website, optometrist website, eyewear website, glasses website,
spectacles website, contact lens website, optometry website, optician practice website,
eye test booking, ophthalmologist website, eye care website, vision centre website,
eye clinic website, optical practice website

Response:
- Eye test booking: Calendly or custom WP; test types (full exam / contact lens check
  / children's / dry eye / OCT scan); optometrist selection; pre-appointment questionnaire;
  SMS + email reminders; from $300
- Frame try-on gallery: filterable product grid (shape/colour/brand/gender/price); virtual
  try-on via Ditto or GlassesOn iframe; from $400
- Online shop: WooCommerce; lens type upsell at checkout (single vision / varifocal /
  reading / blue light); prescription PDF/JPEG upload; bundle pricing; from $600
- Prescription recall: automated email 2 years after exam; Mailchimp or FluentCRM; $250
- NHS information: sight test eligibility checker; GOS voucher info; accepted private schemes
- Trust signals: GOC registration number in footer; College of Optometrists / ABDO / FODO
- From $450 booking site / $1,100+ with shop + virtual try-on + recall emails
- Closes: "bookings / online sales / both?"

### 0d-pre30-b) Property management / letting agent / HMO management
Keywords: property management website, property manager website, hmo management website,
landlord services website, rental management website, block management website, property
management company website, residential management website, property maintenance website,
property concierge website, estate management website, facility management website

Note: distinct from 0d-pre24-b real estate/IDX Broker (sales-focused estate agents) —
this is property management/lettings management, a service-business angle.

Response:
- Landlord lead-gen pages: one per service type (HMO / AST / block / holiday let);
  percentage fee calculator; enquiry form; from $400
- Tenant portal: WP user roles; maintenance report + photo upload; rent payment history;
  key documents (tenancy agreement / EPC / gas safety); from $500
- Maintenance request form: issue category + urgency + photo; routes to contractor or
  inbox; from $300
- Compliance hub: EPC / EICR / gas safety / legionella; expiry-date reminders; HMO
  licence conditions
- CMP (Client Money Protection) badge prominently displayed; required under Tenant Fees
  Act 2019 — immediate trust signal for landlords
- Portfolio showcase: properties managed, testimonials, contractor network
- From $500 brochure + maintenance form / $1,100+ with tenant portal + compliance hub
- Closes: "AST lettings / HMOs / blocks of flats / commercial?"

### 0d-pre30-c) Language school / tutoring centre / English teaching / adult education
Keywords: language school website, english language school, english teaching website,
tutoring centre website, tutoring website, online tutoring website, language course website,
esl website, tefl website, language academy website, adult education website, language
learning website, private tutor website, exam tuition website, gcse tuition website,
a level tuition website

Response:
- Course pages: one per level/exam (GCSE English, A-Level Maths, IELTS, Cambridge First,
  Business English, Conversational Spanish); includes teacher bio, class size, timetable,
  price, outcomes; from $350
- Online booking and enrolment: course + start date + level + trial lesson; Stripe at
  booking; from $350
- Level placement test: Typeform or Gravity Forms; 10-15 questions; auto-calculates
  A1-C2; triggers email with recommended course; from $300
- Student learning portal: homework files, lesson recordings, vocabulary lists, progress
  tracker; WP user roles; from $500
- Teacher profiles: PGCE / CELTA / native/near-native; specialist subjects; availability
- Accreditation badges: British Council inspected, English UK, Ofsted, Cambridge Exam
  Centre; header or footer
- Visa information page: UKVI-recognised sponsor status, CAS number guidance; important
  for international students booking IELTS / academic English
- From $450 brochure + enrolment form / $1,000+ with portal + placement test + WPML
- Closes: "bricks-and-mortar school / 1-to-1 tutoring / online teaching?"

## QA results (27/28 pattern + 1 manual = 28/28 correct)
| Check | Result |
|-------|--------|
| _sendHint element created | OK |
| pb-brain__send-hint class | OK |
| aria-hidden on hint | OK |
| Enter ↵ innerHTML (&#8629;) | OK |
| appended to form | OK |
| _hintTimer declared | OK |
| input listener shows hint after 800ms | OK |
| blur listener hides hint (manual) | OK — multiline span; both halves confirmed present |
| submit handler clears hint | OK |
| pb-brain__send-hint CSS | OK |
| font-size:10px | OK |
| opacity:0 + transition | OK |
| --vis opacity:1 | OK |
| optician keywords | OK |
| eye test booking types (OCT scan) | OK |
| virtual try-on Ditto / GlassesOn | OK |
| GOC registration number | OK |
| from $450 optician | OK |
| property management keywords | OK |
| CMP Client Money Protection | OK |
| tenant portal (photo upload + payment history) | OK |
| Tenant Fees Act 2019 | OK |
| from $500 property mgmt | OK |
| language school keywords | OK |
| placement test A1–C2 level | OK |
| UKVI / CAS number visa page | OK |
| British Council / Ofsted badges | OK |
| from $450 language school | OK |
