# DOM QA Report — R9 — 2026-06-20

## inc/service-pages.php — Service + Offer JSON-LD (new)

`pb_aurora_service_schema()` hooked at `wp_head` priority 8 (before FAQ schema at same priority).

For every service page slug (`web-development`, `ai-agents`, etc.):
- Emits `@type: Service` with:
  - `name` from `$s['h1']`
  - `description` from `$s['intro']`
  - `url` = site root + slug + /
  - `provider: { @id: site_url + '#organization' }`
  - `areaServed: "Worldwide"`
  - `offers[]` built from `$s['prices']` array:
    - `@type: Offer`
    - `name`: price tier name
    - `description`: tier description
    - `price`: numeric only (extracted via `preg_replace('/[^0-9.]/', '', $p[1])`)
    - `priceCurrency: "USD"`
    - `availability: InStock`

Each service page now emits 2 JSON-LD blocks:
1. `Service` with Offer pricing (priority 8)
2. `FAQPage` with 5 FAQ Q&As (priority 8)

## brainstorm.php — 3 new intent handlers

### 0d-pre9-m) Marketplace / multi-vendor platform
Keywords: marketplace, multi-vendor, airbnb like, etsy like, uber for, buy and sell
platform, p2p platform, peer-to-peer, rental marketplace, classified ads

Response: two tiers — basic directory $750, marketplace with escrow/reviews/dashboards
$1,500–$3,500 — asks about specific features (escrow, reviews, split payouts)

### 0d-pre9-n) Nonprofit / charity / NGO
Keywords: non-profit, nonprofit, not for profit, ngo, charity, 501c3, fundraising site,
donation page, volunteer organization, foundation site

Response: same price menu + notes on Stripe Giving Fund fee waiver, WCAG 2.1 AA
included, recommends Simple Site $115 as practical starting point

### 0d-pre9-o) Directory / job board / listing site
Keywords: directory site, listing site, job board, business directory, review site,
property listings, real estate listing, freelancer directory

Response: SaaS/App $750 baseline + paid listings (Stripe), user accounts (+$150–200),
map view, review system as scope variables — asks for site type to give precise quote

## main.js — Character counter on textarea

`charCount` span injected after the textarea input element:
- Updates on 'input' event: `len + ' / 2000'`
- Adds `pb-brain__char-count--warn` class at ≥1800 characters
- `aria-live="polite"` + `aria-atomic="true"` for screen readers

## main.css — Character counter styles
```
.pb-brain__char-count{ display:block; text-align:right; font-size:11px; color:rgba(244,246,251,.3); }
.pb-brain__char-count--warn{ color:rgba(255,180,0,.7); }
```

## QA note
Service schema `@type: Offer` confirmed present at line 244 of service-pages.php
(verification grep matched — QA script had wrong whitespace in string comparison).

## QA results (14/14 true checks pass, 1 false-negative from spacing difference)
| Check | Result |
|-------|--------|
| Service schema function added | OK |
| Service @type Service | OK |
| Service price extraction regex | OK |
| Service schema hook registered | OK |
| Marketplace intent | OK |
| Marketplace scope $1,500–$3,500 | OK |
| Nonprofit intent | OK |
| Directory/job board intent | OK |
| Char counter element created | OK |
| Char counter updates text | OK |
| Char counter warn class | OK |
| Char counter aria-live | OK |
| Char counter CSS | OK |
| Char counter warn CSS | OK |
| @type Offer present (verified by grep) | OK |
