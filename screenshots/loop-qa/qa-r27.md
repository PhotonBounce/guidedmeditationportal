# DOM QA Report — R27 — 2026-06-20

## main.js — Copy confirmation toast

New `_showToast(msg)` function appends a brief floating toast inside the brain panel:

```javascript
function _showToast(msg) {
  if (!brain) return;
  var _t = document.createElement('div');
  _t.className = 'pb-brain__toast';
  _t.setAttribute('role', 'status');
  _t.setAttribute('aria-live', 'polite');
  _t.textContent = msg;
  brain.appendChild(_t);
  requestAnimationFrame(function() {
    requestAnimationFrame(function() { _t.classList.add('pb-brain__toast--vis'); });
  });
  setTimeout(function() {
    _t.classList.remove('pb-brain__toast--vis');
    setTimeout(function() { if (_t.parentNode) _t.parentNode.removeChild(_t); }, 280);
  }, 1700);
}
```

Called from the copy button success handler:
```javascript
cpBtn.addEventListener('click', function() {
  navigator.clipboard.writeText(plain(text)).then(function() {
    cpBtn.innerHTML = '&#10003;';
    _showToast('Copied to clipboard');    // ← new
    setTimeout(function() { cpBtn.innerHTML = '&#128203;'; }, 1500);
  });
});
```

Behavior:
- Double `requestAnimationFrame` ensures the element has a computed style before
  `--vis` is added, so the CSS opacity/transform transition fires correctly
- Toast stays visible for 1700ms, then fades out over 280ms
- After fade-out, the element removes itself from the DOM (no accumulation)
- `role="status"` + `aria-live="polite"` announces "Copied to clipboard" to
  screen readers without interrupting other announcements
- Copy button checkmark (✓) behaviour is unchanged

## main.css — .pb-brain__toast

```css
.pb-brain__toast {
  position:absolute; top:48px; left:50%;
  transform:translateX(-50%) translateY(-6px);
  background:rgba(22,30,52,.97); border:1px solid rgba(255,255,255,.15);
  border-radius:6px; color:rgba(255,255,255,.9);
  font-size:12px; padding:5px 14px;
  opacity:0; pointer-events:none;
  transition:opacity .18s, transform .18s;
  white-space:nowrap; z-index:11;
}
.pb-brain__toast--vis {
  opacity:1; transform:translateX(-50%) translateY(0);
}
```

Positioned at `top:48px` — just below the header (similar to the shortcuts panel
but anchored to the top rather than floating at the bottom). `z-index:11` sits
above the `.pb-brain__newmsg` chip (z-index:10).

## brainstorm.php — 3 new intent handlers

### 0d-pre23-a) Nonprofit / charity / NGO / donation site
Keywords: nonprofit, non-profit, charity website, ngo, donation, fundraising,
charity site, nonprofit site, volunteer, donation website, 501c3, foundation website,
giving campaign

Response:
- GiveWP: single + recurring donations; Stripe + PayPal; auto PDF receipt; from $350
- Campaign pages: fundraising thermometer, goal tracking, anonymised donor wall
- Volunteer management: shift signup, coordinator dashboard, email automation
- Google Ad Grants: $10,000/month free Google Ads for nonprofits — setup included
- Annual report / impact page: data viz of metrics, financial summary, board listing
- Discounts: TechSoup, Cloudflare Nonprofits, Mailchimp 15% discount
- Closes: "donations, volunteer recruitment, events, or all three?"

### 0d-pre23-b) Education / eLearning / online course / LMS
Keywords: online course, elearning, lms, learning management system, course website,
teach online, sell courses, membership learning, education platform, tutoring site,
student portal, school website, learndash, tutor lms, thinkific alternative,
online education

Response: plugin decision matrix
- LearnDash: quizzes, certs, progress, groups, SCORM 1.2; $199/yr + setup; multi-course
- Tutor LMS: free core, clean UI; first-course creators; total from $350
- BuddyBoss: LMS + community (profiles, messaging, groups, courses); from $700
- Video: Bunny.net or Vimeo — never YouTube (ads + distracting autoplay)
- Checkout: WooCommerce one-time / subscription / bundle; Stripe + PayPal
- Certificates: PDF cert on completion; custom design included
- From $450 single-course / $1,200+ full multi-course LMS
- Closes: "how many courses, one-time purchase / subscription / membership?"

### 0d-pre23-c) Healthcare / medical practice / telemedicine / therapist site
Keywords: healthcare website, medical website, doctor website, clinic website,
telemedicine, telehealth, patient portal, hipaa, medical practice, dentist website,
therapist website, mental health site, gp website, psychologist website

Response: with important disclaimers
- Appointment booking: Calendly (HIPAA plan) or SimplePractice for therapists;
  custom WP booking form from $350; no PHI stored server-side
- Telemedicine: Doxy.me embed (HIPAA-compliant video); Zoom Healthcare (requires BAA)
  — platform connection to site, not built in-house
- HIPAA: no PHI in contact form logs; SSL; no GA4 on patient pages without consent;
  technical layer handled but compliance officer must verify
- Patient portal: login link to EHR (Epic, Athenahealth, SimplePractice); auth by them
- Medical copy: I provide layout + structure; clinical team writes + approves all content
- ADA/WCAG 2.1 AA: audited + remediated as standard for healthcare
- From $600 practice site; $250 add-on for HIPAA form config audit
- Closes: "what type of practice, and booking / telemedicine / portal link needed?"

## QA results (26/26 all pass)
| Check | Result |
|-------|--------|
| _showToast defined | OK |
| toast div created | OK |
| role status aria-live | OK |
| double rAF transition | OK |
| self-removes after fade | OK |
| called on clipboard copy | OK |
| checkmark still shows | OK |
| pb-brain__toast CSS | OK |
| position:absolute | OK |
| opacity:0 default | OK |
| pointer-events:none | OK |
| --vis opacity:1 | OK |
| nonprofit keywords | OK |
| GiveWP | OK |
| Google Ad Grants $10k/mo | OK |
| volunteer management | OK |
| elearning keywords | OK |
| LearnDash | OK |
| BuddyBoss | OK |
| never YouTube | OK |
| SCORM | OK |
| healthcare keywords | OK |
| HIPAA | OK |
| Doxy.me | OK |
| PHI / Protected Health Information | OK |
| WCAG healthcare | OK |
