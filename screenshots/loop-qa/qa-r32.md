# DOM QA Report — R32 — 2026-06-20

## main.js — Focus trap inside the chat drawer

When the drawer is open, Tab and Shift+Tab now cycle only among focusable
elements *inside* the drawer — keyboard and screen reader users can't accidentally
escape to the page behind. Required by WCAG 2.1 SC 2.1.2 (No Keyboard Trap)
and expected by AT users as standard modal/panel behaviour.

```javascript
// Focus trap — Tab/Shift+Tab cycles within the open drawer (WCAG 2.1 SC 2.1.2).
if (brain) {
  brain.addEventListener('keydown', function(e) {
    if (e.key !== 'Tab' || brain.hidden) return;
    var _foc = Array.prototype.slice.call(
      brain.querySelectorAll(
        'button:not([disabled]), input:not([disabled]), ' +
        'textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
      )
    ).filter(function(el) { return el.offsetParent !== null; }); // visible only
    if (!_foc.length) return;
    var _first = _foc[0];
    var _last  = _foc[_foc.length - 1];
    if (e.shiftKey) {
      if (document.activeElement === _first) { e.preventDefault(); _last.focus(); }
    } else {
      if (document.activeElement === _last) { e.preventDefault(); _first.focus(); }
    }
  });
}
```

Behavior:
- Early return if `e.key !== 'Tab'` — no overhead on every keypress
- Early return if `brain.hidden` — trap is inactive while drawer is closed
- `offsetParent !== null` filter excludes elements hidden with `display:none`,
  `visibility:hidden`, or inside a `hidden` ancestor — e.g. the search input
  before it's opened, or buttons inside collapsed sections
- Shift+Tab on first focusable → jumps to last (cycles backward)
- Tab on last focusable → jumps to first (cycles forward)
- All other Tab presses fall through to default browser behaviour (move to next
  element in the natural order within the drawer)
- Does NOT use the pattern of calling `e.preventDefault()` on every Tab — only
  intercepts the wrap-around at the boundaries, so in-drawer Tab order is natural

Notes:
- R32 also confirmed that mute state is already fully persisted to localStorage
  (`pb_voice_muted` key, read on init at muteBtn construction, written on toggle,
  checked in `speak()`) — no changes needed to mute persistence

## brainstorm.php — 3 new intent handlers

### 0d-pre28-a) Architecture / interior design / landscape design / design studio
Keywords: architecture website, architect website, interior design website,
interior designer website, landscape design website, design studio website,
architectural firm, interior architecture, space design website,
architecture portfolio, architectural visualization, interior decorator website,
design agency portfolio

Response:
- Project portfolio CPT: full-screen photos, category tags, location, year, area;
  Masonry or editorial grid, filterable; from $500
- Full-screen photography: parallax hero, WebP, blurhash lazy-load, Lightbox with
  keyboard nav, EXIF/credit line support
- 3D / CGI: Sketchfab embed (interactive 3D models), Matterport virtual tour embed
  for completed spaces, before/after slider for renovation projects
- Services page: phased breakdown (concept, DD, planning, CD, PM); fee structure
  (% of build cost / fixed fee / hourly) explained clearly
- Awards + press: RIBA, AIA, Dezeen, Architizer A+; schema markup for awards
- Enquiry form: project type, location, area, budget, timeline; from $200
- From $600 solo / $1,400+ studio with 3D + Matterport + award pages
- Closes: "solo practitioner / small studio / larger firm? Scale of projects?"

### 0d-pre28-b) Sports club / team / association / leisure centre
Keywords: sports club website, football/tennis/cricket/rugby/swimming/athletics/
cycling/golf club, sports team website, leisure centre website, sports association,
martial arts club, gym club, sports coaching website

Response:
- Membership signup + renewal: WooCommerce Subscriptions; annual/family/junior/
  concession rates; Stripe + PayPal; auto renewal reminders; from $400
- Fixtures + results: upcoming calendar, live score or post-match entry, auto
  league table, iCal export, The Events Calendar plugin; from $300
- Player / member profiles: login-gated; stats, season history, squad management;
  from $400
- Volunteer + team management: role sign-up, availability poll, WhatsApp/email
  links, team-sheet PDF download
- News / match reports: WP posts; auto-post to Facebook + Twitter via Zapier
- Sponsorship page: tiered packages (kit/pitch-side/match); logo wall; enquiry form
- From $450 small club / $1,100+ multi-team with profiles + league tables
- Closes: "sport, how many teams/age groups, membership renewal priority?"

### 0d-pre28-c) Recruitment agency / headhunter / staffing / job board
Keywords: recruitment website, recruitment agency website, staffing website,
headhunter website, job board website, jobs website, employment agency website,
talent acquisition website, executive search website, temp agency website,
IT recruitment website, HR recruitment website

Response:
- Job listings CPT: role title, location, salary, type (perm/contract/temp),
  sector, skills; filterable search; Apply button with CV upload; from $450
- CV / resume upload: speculative registration; PDF/DOCX; auto-tagged by sector;
  emails consultant; Gravity Forms + GDPR consent tick
- Candidate portal: login account; saved jobs; application status; WP user roles;
  from $350
- Employer / client page: post-a-vacancy form; sector specialisms; client logos;
  placed-candidate case studies
- Indeed / LinkedIn sync: WP Job Manager feeds Indeed; LinkedIn via Direct Jobs API
  (requires partner approval); from $250 add-on
- Sector pages: one per specialism; "[sector] recruitment [city]" keywords
- From $550 boutique agency / $1,200+ multi-sector with portal + employer area + sync
- Closes: "sectors you recruit in, and live job board or lead-capture?"

## QA results (21/21 all pass)
| Check | Result |
|-------|--------|
| focus trap keydown on brain | OK |
| queries focusable elements | OK |
| filters visible only | OK |
| Shift+Tab wraps to last | OK |
| Tab wraps to first | OK |
| WCAG 2.1 SC 2.1.2 comment | OK |
| architecture keywords | OK |
| Matterport virtual tour | OK |
| Sketchfab 3D embed | OK |
| RIBA / AIA awards | OK |
| from $600 solo architect | OK |
| sports club keywords | OK |
| WooCommerce Subscriptions | OK |
| fixtures / results / league table | OK |
| tiered sponsorship page | OK |
| from $450 small club | OK |
| recruitment keywords | OK |
| CV upload + GDPR consent | OK |
| Indeed / LinkedIn sync | OK |
| sector pages SEO | OK |
| from $550 boutique agency | OK |
