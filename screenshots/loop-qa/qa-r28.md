# DOM QA Report — R28 — 2026-06-20

## main.js — Ctrl+↑ to restore last sent message

Classic edit-last pattern (familiar from CLI history and modern chat apps):

```javascript
// Ctrl+↑ on empty input — restore last sent message (edit-last pattern).
input.addEventListener('keydown', function(e) {
  if (e.ctrlKey && e.key === 'ArrowUp' && !input.value.trim()) {
    var _last = null;
    for (var _li = chatMsgs.length - 1; _li >= 0; _li--) {
      if (chatMsgs[_li].cls === 'user') { _last = chatMsgs[_li]; break; }
    }
    if (_last) {
      e.preventDefault();
      input.value = _last.text;
      input.dispatchEvent(new Event('input'));
      input.selectionStart = input.selectionEnd = input.value.length;
    }
  }
});
```

Behavior:
- Only fires when: Ctrl is held, ↑ is pressed, AND the input is currently empty
  (so it never interferes with normal text editing or standalone ↑ navigation)
- Iterates `chatMsgs` in reverse to find the most recent message where
  `cls === 'user'`; does nothing if there are no user messages yet
- `input.dispatchEvent(new Event('input'))` triggers the existing auto-resize
  listener, so the textarea grows to the right height for the restored text
- `selectionStart = selectionEnd = input.value.length` positions the cursor
  at the end of the restored text (ready to append rather than overwrite)
- `e.preventDefault()` stops the browser from scrolling the page up

Shortcuts panel updated to list the new shortcut (6th entry):
```
Ctrl + ↑    Edit last message
```
The `?` help panel now shows all 6 keyboard shortcuts.

## brainstorm.php — 3 new intent handlers

### 0d-pre24-a) Gym / fitness studio / personal trainer / yoga site
Keywords: gym website, fitness website, personal trainer, yoga studio, crossfit,
pilates site, bootcamp, personal training, fitness class, gym membership,
fitness studio, spin class, martial arts website

Response:
- Class schedule: weekly timetable, filter by instructor/type, live availability;
  MindBody or Glofox embed as $200 add-on; custom built from $350
- Online booking: class or PT session + Stripe deposit; cancel/reschedule
  self-service; Twilio/Mailchimp SMS + email reminders
- Membership tiers: monthly/annual subscriptions via WooCommerce Subscriptions
  or MemberPress; member portal login; free-trial logic; from $400
- Trainer profiles: photo, bio, specialisms, video intro, direct-book button
- VOD library: locked-behind-membership workout library; Bunny.net or Vimeo
  hosting; watch progress tracking; from $600
- Local SEO: Local Business JSON-LD, Google Business, map pack, class schema
- From $500 studio site / $1,200+ with member portal + VOD
- Closes: "group classes, PT, online coaching, or combination?"

### 0d-pre24-b) Real estate / estate agent / property listing site
Keywords: real estate website, property listing, estate agent website,
realtor website, homes for sale, property website, letting agent, idx integration,
mls integration, real estate site, property for sale, estate agency,
letting website, property search

Response:
- Property listings: custom post type; photo gallery, floor plan, map, price,
  beds/baths/sqft; staff-editable via WP admin; from $450
- MLS/IDX: IDX Broker or Showcase IDX ($60-$80/mo); live active listings auto-feed
- Advanced search: filter by location, price, beds, baths, type, new-build;
  saved search + email alert on new match; from $300
- Mortgage calculator: monthly payment, total cost, deposit breakdown; free
- Valuation/appraisal form: multi-step → CRM/email; highest-converting page
- Local area pages: per suburb/neighbourhood; schema + Maps; targets "[area] agent"
- From $600 (10-listing) / $1,500+ (IDX + saved-search alerts)
- Closes: "solo agent / team / agency? US (IDX) or UK/EU?"

### 0d-pre24-c) Wedding / event planning / venue / coordinator site
Keywords: wedding website, wedding planner, event planning website, venue website,
event coordinator, bridal website, wedding photography, wedding videographer,
wedding site, event venue, event management website, party planner,
corporate events website

Response:
- Hero + gallery: full-screen Bunny.net video hero; filterable photo gallery;
  before/after slider; from $400
- Package pages: priced tiers with inclusions checklist; FAQ accordion;
  photo testimonials; structured data for rich results
- Enquiry form: multi-step (date → type → guests → budget → contact); Gravity
  Forms with conditional logic; feeds CRM / Dubsado / HoneyBook
- Contract + deposit: HelloSign e-signature; Stripe 30% deposit; triggers
  automated welcome email sequence
- Client portal: checklist + mood board upload + timeline; WP user roles; from $400
- Real wedding showcase: blog-style; Pinterest-optimised; long-tail SEO
- From $550 (6-page planner/photographer) / $1,400+ (venue with full booking stack)
- Closes: "planner / photographer / videographer / venue? Online contracts needed?"

## QA results (23/23 all pass)
| Check | Result |
|-------|--------|
| Ctrl+ArrowUp keydown listener | OK |
| only fires when input empty | OK |
| finds last user msg | OK |
| sets input.value | OK |
| dispatches input event for resize | OK |
| cursor at end | OK |
| Ctrl+↑ in help panel | OK |
| 6 shortcuts total | OK |
| gym keywords | OK |
| MindBody or Glofox | OK |
| WooCommerce Subscriptions / MemberPress | OK |
| VOD locked behind membership | OK |
| gym from $500 | OK |
| real estate keywords | OK |
| IDX Broker | OK |
| mortgage calculator | OK |
| saved-search email alert | OK |
| from $600 agency site | OK |
| wedding keywords | OK |
| Dubsado / HoneyBook | OK |
| HelloSign + Stripe | OK |
| planning checklist + mood board | OK |
| from $550 planner site | OK |
