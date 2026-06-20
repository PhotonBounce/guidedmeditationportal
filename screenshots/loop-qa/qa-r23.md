# DOM QA Report — R23 — 2026-06-20

## main.js — Smart auto-scroll (don't scroll if user is reading history)

New `_nearBottom()` helper function:

```javascript
function _nearBottom() {
  return !log || (log.scrollHeight - log.scrollTop - log.clientHeight) < 90;
}
```

Returns `true` if the user is within 90px of the bottom; `false` if they've
scrolled up into history. Three previously unconditional scroll calls are now
guarded with this check:

```javascript
// in addMsg():
if (_nearBottom()) log.scrollTop = log.scrollHeight;

// in addTyping():
if (_nearBottom()) log.scrollTop = log.scrollHeight;

// in showListeningBanner():
if (_nearBottom()) log.scrollTop = log.scrollHeight;
```

The scroll-down button's explicit click handler is NOT guarded — when the user
clicks "↓ scroll to bottom", it should always scroll regardless of position.

Before this change: any new bot message would yank the user back to the bottom
even if they were scrolled up reading earlier replies.
After: the user can scroll up freely; auto-scroll only resumes once they've
scrolled back to within 90px of the bottom.

## main.js — `?` keyboard shortcuts help panel

New `helpBtn` (?) added to the header button group (before the close button):

```javascript
var helpBtn = document.createElement('button');
helpBtn.type = 'button';
helpBtn.className = 'pb-brain__help';
helpBtn.title = 'Keyboard shortcuts';
helpBtn.setAttribute('aria-label', 'Keyboard shortcuts');
helpBtn.setAttribute('aria-expanded', 'false');
helpBtn.innerHTML = '&#63;'; // ?

var _helpOpen = false;
var _helpPanel = document.createElement('div');
_helpPanel.className = 'pb-brain__shortcuts';
_helpPanel.setAttribute('role', 'tooltip');
_helpPanel.innerHTML = [
  '<strong>Shortcuts</strong>',
  '<span><kbd>Ctrl</kbd>+<kbd>/</kbd> Open / close chat</span>',
  '<span><kbd>Ctrl</kbd>+<kbd>F</kbd> Search messages</span>',
  '<span><kbd>Shift</kbd>+<kbd>Enter</kbd> New line</span>',
  '<span><kbd>Enter</kbd> Send message</span>',
  '<span><kbd>Esc</kbd> Close / cancel search</span>',
].join('');
brainHead.appendChild(_helpPanel);

helpBtn.addEventListener('click', function() {
  _helpOpen = !_helpOpen;
  _helpPanel.classList.toggle('pb-brain__shortcuts--open', _helpOpen);
  helpBtn.setAttribute('aria-expanded', String(_helpOpen));
});

// Outside click dismisses the panel
document.addEventListener('click', function(e) {
  if (_helpOpen && !_helpPanel.contains(e.target) && e.target !== helpBtn) {
    _helpOpen = false;
    _helpPanel.classList.remove('pb-brain__shortcuts--open');
    helpBtn.setAttribute('aria-expanded', 'false');
  }
});
```

Header btn order: [↺ newchat] [⇓ export] [− collapse] [🔊 mute] [? help] [× close]

Accessibility: `aria-expanded` toggles on the button; `role="tooltip"` on the panel.

## main.css — .pb-brain__help + .pb-brain__shortcuts

```css
.pb-brain__help {
  background:none; border:1px solid rgba(255,255,255,.15);
  color:rgba(255,255,255,.45); width:22px; height:22px; border-radius:50%;
  font-size:11px; font-weight:700; cursor:pointer; display:flex;
  align-items:center; justify-content:center; flex-shrink:0; transition:.15s;
}
.pb-brain__help:hover { border-color:rgba(255,255,255,.4); color:rgba(255,255,255,.9); }

.pb-brain__shortcuts {
  display:none; position:absolute; bottom:calc(100% + 6px); right:12px;
  background:rgba(18,18,28,.97); border:1px solid rgba(255,255,255,.15);
  border-radius:8px; color:rgba(255,255,255,.8); font-size:12px;
  padding:10px 14px; white-space:nowrap; z-index:10; line-height:1;
  box-shadow:0 4px 18px rgba(0,0,0,.45);
}
.pb-brain__shortcuts--open { display:flex; flex-direction:column; gap:7px; }
.pb-brain__shortcuts strong { color:#fff; font-size:11px; text-transform:uppercase; letter-spacing:.06em; }
.pb-brain__shortcuts kbd {
  background:rgba(255,255,255,.1); border:1px solid rgba(255,255,255,.2);
  border-radius:3px; font-size:10px; padding:1px 4px; line-height:1.4;
}
.pb-brain__hdr { position:relative; }
```

`position:absolute; bottom:calc(100% + 6px)` positions the panel above the header.
`position:relative` on `.pb-brain__hdr` establishes the containing block.

## brainstorm.php — 3 new intent handlers

### 0d-pre19-a) Subscription box / recurring physical goods
Keywords: subscription box, subscription service, recurring order, recurring product,
monthly box, curated box, product subscription, recurring shipment, subscribe and save

Response:
- WooCommerce Subscriptions: recurring Stripe billing; free trials; pause/cancel/upgrade
  from customer account; from $350 + WCS licence
- Billing cycles: weekly/monthly/quarterly/annually; prorated changes
- Box management: per-shipment variant selection, skip-a-month, gifting, address change
- Fulfilment: ShipStation / EasyPost bulk label generation on billing date; auto-tracking
- Churn prevention: dunning emails on failed payment; automatic retry schedule
- Analytics: subscriber MRR, churn rate, LTV in WooCommerce dashboard
- Closes: "what's in the box, how often, do customers customise?"

### 0d-pre19-b) Event ticketing / registration / virtual events
Keywords: event ticketing, ticket sales, event registration, ticket website, sell tickets,
virtual event, online event, event site, webinar site, conference website, summit website,
events calendar, event page

Response:
- Ticketing: WooCommerce ticket products; QR code on confirmation email; PDF ticket
- Registration: Gravity Forms + conditional logic, time-slot selection, capacity limits
- Virtual events: Zoom/Google Meet embed; gated livestream; post-event recordings
- Multi-event calendar: The Events Calendar; filterable by date/location/type; iCal export
- Pricing: early-bird, promo codes, group discounts, deposit + balance payments
- Refund policy: Stripe rules enforced (full >30d, 50% within 7d, no-refund <7d)
- From $500 single-event / $800+ multi-event calendar with registration + ticketing
- Closes: "single or recurring calendar, in-person or virtual, free or paid?"

### 0d-pre19-c) Mobile app vs PWA vs React Native vs Flutter
Keywords: mobile app, native app, ios app, android app, react native, flutter, pwa,
progressive web app, app development, mobile app vs website, build an app, hybrid app,
cross platform app, native vs pwa, mobile development, app vs website

Response: decision framework
- PWA: runs in browser; installable from Safari/Chrome; offline + push; NO App Store;
  included in Next.js and WP builds at no extra cost
- React Native: shared iOS+Android codebase; near-native; App Store + Play Store; OTA
  updates; from $2,500+
- Flutter: beautiful custom UI; single Dart codebase for iOS/Android/web; from $2,500+
- Native Swift/Kotlin: max hardware (Bluetooth, NFC, ARKit, CarPlay); from $5,000+ per
  platform — referred to specialist partners
- Decision guide: PWA when content/dashboard/SaaS with no hardware APIs;
  React Native/Flutter when App Store listing + hardware features needed
- Closes: "App Store listing + which hardware features?"

## QA results (33/33 all pass)
| Check | Result |
|-------|--------|
| _nearBottom defined | OK |
| 90px threshold | OK |
| addMsg scroll guarded | OK |
| addTyping scroll guarded | OK |
| scroll-down btn unguarded | OK |
| helpBtn created | OK |
| pb-brain__help class | OK |
| ? innerHTML | OK |
| _helpPanel created | OK |
| pb-brain__shortcuts class | OK |
| Ctrl+/ shortcut | OK |
| Ctrl+F shortcut | OK |
| Shift+Enter shortcut | OK |
| --open toggle on click | OK |
| outside click closes | OK |
| helpBtn in btnGroup | OK |
| circle border-radius | OK |
| position:absolute panel | OK |
| --open display:flex | OK |
| kbd styling | OK |
| hdr position:relative | OK |
| subscription box keywords | OK |
| WooCommerce Subscriptions | OK |
| ShipStation | OK |
| dunning emails | OK |
| event ticketing keywords | OK |
| QR code tickets | OK |
| Events Calendar plugin | OK |
| mobile app keywords | OK |
| PWA described | OK |
| React Native | OK |
| Flutter | OK |
| Choose PWA when guide | OK |
