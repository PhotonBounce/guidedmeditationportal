# DOM QA Report — R61 — 2026-06-21

## main.js — Placeholder text cycling

While the input is empty and unfocused, rotates through 10 hint questions every 4 seconds
to guide visitors who don't know what to ask. Cycling stops permanently once the user
sends their first message. Re-focuses to a clean default placeholder when the input is
focused. Restores the current hint text on blur if the value is still empty.

### Implementation

```javascript
// R61: Placeholder cycling — rotates hint questions in the input while empty and unfocused.
if (input && !window._pbPlCycleInited) {
  window._pbPlCycleInited = true;
  var _plHints = [
    'What type of website do you need?',
    'Try: "I run a dental practice..."',
    'Try: "I\'m a wedding photographer..."',
    'Try: "I have a B&B in Cornwall"',
    'Try: "I need a website for my cafe"',
    'Try: "I\'m a solicitor in Manchester"',
    'Ask about pricing, timelines, or features',
    'Try: "I\'m a personal trainer..."',
    'Try: "I run a hair salon..."',
    'What kind of business do you have?',
  ];
  var _plIdx = 0;
  var _plDefault = input.placeholder || '';
  input.placeholder = _plHints[0];
  var _plTimer = setInterval(function() {
    if (document.activeElement === input || input.value.trim()) return;
    input.classList.remove('pb-brain__input--pl');
    void input.offsetWidth;
    _plIdx = (_plIdx + 1) % _plHints.length;
    input.placeholder = _plHints[_plIdx];
    input.classList.add('pb-brain__input--pl');
  }, 4000);
  input.addEventListener('focus', function() {
    input.placeholder = _plDefault;
    input.classList.remove('pb-brain__input--pl');
  });
  input.addEventListener('blur', function() {
    if (!input.value.trim()) input.placeholder = _plHints[_plIdx];
  });
  input.addEventListener('keydown', function _plStop(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
      clearInterval(_plTimer);
      input.placeholder = _plDefault;
      input.removeEventListener('keydown', _plStop);
    }
  });
}
```

**`void input.offsetWidth` (reflow trick)**: removing the CSS class, forcing a reflow, then
re-adding the class restarts the CSS `@keyframes` animation. Without the reflow, removing and
re-adding the same class in the same frame has no effect — the browser optimises it away.
This is the standard technique for restarting a CSS animation via JS.

**`input.placeholder = _plHints[0]` on init**: replaces whatever placeholder the HTML
attribute had (`_plDefault`) immediately, so the first hint is visible from page load.

**`_plDefault` saved**: on focus, the placeholder reverts to the original HTML attribute value
(usually "" or the theme's default hint text). This avoids the cycling hints overriding the
default placeholder permanently.

**`input.value.trim()` guard in interval**: if the user has started typing but hasn't sent yet,
the interval fires but finds `value.trim()` non-empty and returns early — the placeholder
won't change while the user is composing a message. The `document.activeElement === input`
guard additionally skips the swap while the input is focused, even if it's empty.

**`clearInterval` on first Enter**: once the first message is sent, cycling is permanently
disabled. The `keydown` listener removes itself (named `_plStop`) to avoid memory leaks.
The cycling is a first-impression hook — after the conversation starts, the input should
behave like a normal chat input.

**Insertion point**: immediately before `form.addEventListener('submit', async e => {...})`
in the chatbot widget init section. The `input` and `form` variables are both in scope at
this point (defined a few lines above). The `window._pbPlCycleInited` guard prevents
double-initialisation if the function is called more than once.

**`[data-pb-brain-input]` selector**: `input` is assigned via
`document.querySelector('[data-pb-brain-input]')` in the existing widget init. The
placeholder feature operates on the same reference.

## main.css — Placeholder cycling fade-in

```css
/* R61 — placeholder text cycling (rotates hint questions while input is empty) */
@keyframes pb-pl-fade{ from{opacity:.2} to{opacity:.65} }
.pb-brain__input--pl::placeholder{ animation:pb-pl-fade .4s ease forwards; }
```

**`@keyframes pb-pl-fade`**: fades the placeholder text from near-invisible (0.2) to
standard placeholder opacity (0.65). The `from` opacity of 0.2 means each new hint text
"emerges" rather than appearing abruptly, creating a soft reveal effect.

**`.pb-brain__input--pl::placeholder`**: the animation only applies to the `::placeholder`
pseudo-element — the user's typed text is unaffected regardless of the class being present.

**`forwards` fill mode**: the placeholder stays at 0.65 after the animation ends. Without
`forwards`, it would snap back to its default opacity (usually 1.0 in most browsers).

**0.4s ease**: slightly slower than the `--vis` transitions (0.2–0.3s) to give a gentle
"materialising" feel appropriate for a cycling display rather than a response animation.

## brainstorm.php — 3 new intent handlers

### 0d-pre57-a) Removal company / house removals / man and van / storage / office removals
Keywords: removal company website, removals website, house removals website, man and van
website, moving company website, office removals website, commercial removals website,
furniture removals website, piano removals website, international removals website, overseas
removals website, european removals website, student removals website, storage company
website, self storage website, shipping container storage website

Response: instant quote + trust over price
- Instant online quote form: from/to postcode; bedrooms; access notes; move date; from $300
- Volume calculator: room-by-room inventory; eliminates underquoting; from $250
- Services: domestic; commercial; man and van; packing; storage; piano; antiques; from $200
- Storage: container; self-storage; containerised; short/long-term; business archive; from $150
- International/European: groupage vs full-load; customs; HMRC approved; RHA; BAR; from $150
- BAR accreditation: British Association of Removers; Approved Remover mark; from $100
- Google Reviews/Trustpilot: schema; from $100
- Moving guide blog: "How to pack a kitchen"; from $100/post
- Fleet/team: HGV; Luton; GPS; uniformed crews; from $100
- Insurance: goods in transit; public liability; employer's; from $100
- From $600 / $1,400+

### 0d-pre57-b) Insurance broker / protection adviser / general insurance / commercial insurance
Keywords: insurance broker website, insurance website, insurance adviser website, protection
broker website, life insurance website, home insurance website, commercial insurance website,
business insurance website, van insurance website, fleet insurance website, tradesman
insurance website, liability insurance website, professional indemnity website, employers
liability website, public liability website, income protection website

Response: trust + clarity over price-comparison jargon
- FCA authorisation: FCA reference number; firm status; must appear on all pages; from $100
- Product/cover pages: life; critical illness; income protection; key person; shareholder;
  BI; public liability; professional indemnity; employer's; from $150/page
- Business insurance hub: sub-pages per trade (builder, electrician, accountant, IT
  contractor); each ranks for "[trade] insurance UK"; from $200
- Fact find / needs analysis form: pre-qualifies before call; from $250
- Calculators: life cover; income replacement; critical illness sum assured; from $300
- Claims support page: broker advocacy; post-sale loyalty; from $100
- Blog/guides: "Do I need key person insurance?"; from $100/post
- BIBA / CII / PFS membership: trust signals; from $100
- From $600 / $1,400+

### 0d-pre57-c) Podiatrist / chiropodist / foot clinic / biomechanics / orthotics
Keywords: podiatrist website, chiropodist website, foot clinic website, podiatry clinic
website, biomechanics website, orthotics website, gait analysis website, ingrown toenail
website, diabetic foot website, verruca treatment website, corn removal website, foot health
practitioner website, nail surgery website, sports podiatrist website, podiatric surgeon
website, HCPC podiatrist website

Response: HCPC credentials + condition-specific SEO
- HCPC registration: protected title; registration number above fold; from $100
- Condition/treatment pages: ingrown toenail (nail surgery & bracing); verruca (SWIFT
  microwave; acid; needling); corn & callus; fungal nail (laser); heel pain (plantar fasciitis;
  Sever's); bunion; each ranks for "[condition] treatment [city]"; from $150/page
- Diabetic foot care: NHS commissioned; vascular assessment; NICE guidelines; from $200
- Biomechanics & gait analysis: video analysis; 3D pressure mapping; CAD/CAM orthotics;
  sports biomechanics; from $200
- Online booking: Cliniko; Jane App; WriteUpp; Acuity; from $250
- Sports podiatry: running injuries; Achilles; shin splints; taping; from $150
- Children's foot health: flat feet; in-toeing; Sever's; from $150
- NHS vs private: explains referral; direct access; converts self-funders; from $100
- College of Podiatry/FCP; Society of Chiropodists; HCPC; from $100
- From $600 / $1,400+

## QA results (38/38 all pass)

| Check | Result |
|-------|--------|
| R61 comment | OK |
| _pbPlCycleInited | OK |
| _plHints array | OK |
| dental hint | OK |
| photographer hint | OK |
| setInterval 4000 | OK |
| pb-brain__input--pl add | OK |
| focus listener | OK |
| blur listener | OK |
| Enter clears interval | OK |
| R61 before form submit | OK |
| R61 CSS comment | OK |
| @keyframes pb-pl-fade | OK |
| --pl::placeholder rule | OK |
| animation pb-pl-fade | OK |
| pre57-a present | OK |
| removal company keywords | OK |
| man and van keyword | OK |
| BAR accreditation | OK |
| RHA accreditation | OK |
| volume calculator | OK |
| removal price line | OK |
| pre57-b present | OK |
| insurance broker keywords | OK |
| FCA authorisation | OK |
| cover calculator | OK |
| BIBA membership | OK |
| fact find form | OK |
| insurance price line | OK |
| pre57-c present | OK |
| podiatrist keywords | OK |
| HCPC registration | OK |
| ingrown toenail | OK |
| diabetic foot | OK |
| gait analysis | OK |
| Cliniko booking | OK |
| podiatry price line | OK |
| pre57 before pre56 | OK |
