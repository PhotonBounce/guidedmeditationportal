# DOM QA Report — R12 — 2026-06-20

## brainstorm.php — 3 new intent handlers

### 0d-pre9-v) Subscription / recurring billing (SaaS pricing model)
Keywords: subscription billing, recurring billing, saas pricing model, monthly subscription,
stripe subscriptions, metered billing, usage-based pricing, per-seat pricing, billing portal,
freemium model, free trial billing

Response: Stripe Billing (monthly/annual/per-seat/metered), free trial → paid upgrade,
webhooks for plan changes/cancellations/failed payments, Stripe customer portal for self-serve
upgrades/downgrades; → SaaS/App $750+ tier; asks about plan structure

### 0d-pre9-w) Multilingual / i18n / RTL support
Keywords: multiple languages, multilingual site, bilingual site, i18n, translate my site,
spanish/french/german/arabic version, rtl support, right to left, polylang, wpml, localization

Response: Polylang Free/WPML for 2-3 languages; CSS logical properties + RTL stylesheet for
Arabic/Hebrew/Persian; DeepL/Google Translate API for auto-translation (+$150); language switcher
styled to match design; asks language count + content source

### 0d-pre9-x) Performance / page speed / Core Web Vitals
Keywords: page speed, performance optimization, core web vitals, lighthouse score, slow website,
lcp, cls, inp, fid, page load time, gtmetrix, pagespeed insights, webpagetest

Response: LCP (WebP + preload), CLS (explicit dimensions, no layout-shifting fonts),
INP (deferred/split heavy JS), target 90+ Lighthouse homepage / 85+ service pages; existing
site audit available; asks new build vs existing site optimization

## inc/schema.php — HowToStep position fields

Added `'position' => N` (1-4) to all 4 HowToStep entries:
- Step 1: Discovery
- Step 2: Scope & Estimate
- Step 3: Build in Public
- Step 4: Launch & Iterate

`position` is a required field for Google's HowTo rich result validation.
Previously missing, causing potential eligibility warning in Rich Results Test.

## main.js — Scroll-to-bottom floating button

`scrollDownBtn` appended to `.pb-brain` aside (position:absolute inside the
fixed drawer):
- Class: `pb-brain__scroll-down`
- Aria-label: "Scroll to latest message"
- innerHTML: ↓ (↓)
- Appears (`.is-visible`) when `log.scrollHeight - log.clientHeight - log.scrollTop > 60px`
- Hides automatically when user scrolls back to bottom
- On click: `log.scrollTop = log.scrollHeight`

## main.css — Scroll-to-bottom button styles

```css
.pb-brain__scroll-down{ position:absolute; bottom:86px; right:14px; z-index:20; width:30px; height:30px; border-radius:50%; background:rgba(8,10,18,.9); border:1px solid rgba(255,212,0,.45); color:#ffd400; opacity:0; pointer-events:none; transition:opacity .18s; }
.pb-brain__scroll-down.is-visible{ opacity:1; pointer-events:all; }
.pb-brain__scroll-down:hover{ background:rgba(255,212,0,.1); }
```

## QA results (21/21 all pass)
| Check | Result |
|-------|--------|
| Subscription billing intent | OK |
| Stripe Billing in response | OK |
| Multilingual intent | OK |
| Polylang in multilingual response | OK |
| RTL support mentioned | OK |
| Performance intent | OK |
| LCP mentioned | OK |
| CLS mentioned | OK |
| INP mentioned | OK |
| HowToStep position 1 | OK |
| HowToStep position 2 | OK |
| HowToStep position 3 | OK |
| HowToStep position 4 | OK |
| scrollDownBtn declared | OK |
| pb-brain__scroll-down class | OK |
| scroll listener on log | OK |
| is-visible toggle | OK |
| scrollDownBtn click handler | OK |
| .pb-brain__scroll-down CSS | OK |
| .is-visible CSS | OK |
| scroll button hover | OK |
