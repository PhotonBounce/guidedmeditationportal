# DOM QA Report — R8 — 2026-06-20

## main.js — Follow-up action chips in conversation log

After the 2nd bot message (the first substantive reply, not the welcome), a
`.pb-brain__chips-followup` div is injected directly into the log:

- "📅 Book a call" → sends "Book a free 15-min call"
- "💰 All pricing" → sends "What is the full pricing list?"
- "🎨 Portfolio" → sends "Show me your portfolio and past work"

Chips auto-hide on click (fu.style.display = 'none').
Only injected once (guarded by querySelector('.pb-brain__chips-followup')).
Chip click handler uses unicode-safe textContent strip regex.

## main.css — Follow-up chip variant

`.pb-brain__chips-followup{ border-top:none; margin-top:6px; padding-top:6px; }`

Inherits all `.pb-brain__chips` styles (display:flex, flex-wrap, gap).

## brainstorm.php — 4 new intent handlers

### 0d-pre9a) Guarantee / refund / satisfaction policy
Keywords: guarantee, money back, refund, satisfaction guarantee, what if not happy,
not satisfied, can I get a refund, risk, protected

Response: written scope first, 2 revision rounds included, partial refund on
missed-scope milestones, honest "no refund for changing your mind" policy

### 0d-pre9b) White-label / agency sub-contracting
Keywords: white label, whitelabel, white-label, agency work, sub-contractor,
work under my brand, resell your services, build for my client, agency partner

Response: explicitly OK, same fixed prices, NDA before discussion, no PB branding
unless requested

### 0d-pre9c) GDPR / data protection / privacy / security
Keywords: gdpr, data protection, privacy policy, hipaa, cookie consent, cookie
compliance, data residency, is it secure, security compliance, pii, ccpa

Response: GDPR/CCPA cookie banner + privacy stub included, no server-side data
storage by default, HTTPS/security headers, WP hardening, HIPAA stack notes

### 0d-pre9d) Email marketing as content service
Keywords: email marketing, email newsletter, email campaign, email list, drip
campaign, email sequence, welcome email, newsletter content, email copywriting

Response: content/strategy = SMM tier $75/mo; technical setup (Mailchimp/Klaviyo
config, automation flows) = bundled into site/SaaS build; pivots with clarifying Q

## inc/schema.php — Speakable selector expansion

Added to SpeakableSpecification cssSelector array:
- `.pb-section__head p` — section intro paragraphs
- `.pb-faq__q` — FAQ question text (all 10 home FAQs now speakable)
- `.pb-faq__a` — FAQ answer text
- `[data-speakable]` — opt-in attribute for any future content

## QA results (16/16 pass)
| Check | Result |
|-------|--------|
| Follow-up chips injected on 2nd bot msg | OK |
| Follow-up Book a call chip | OK |
| Follow-up All pricing chip | OK |
| Follow-up Portfolio chip | OK |
| Follow-up chips deregister after click | OK |
| Follow-up chips CSS | OK |
| Guarantee/refund intent | OK |
| White-label intent | OK |
| GDPR/privacy intent | OK |
| Email marketing intent | OK |
| HIPAA mention | OK |
| Speakable FAQ Q selector | OK |
| Speakable FAQ A selector | OK |
| Speakable [data-speakable] selector | OK |
| R7 membership intent retained | OK |
| R6 referral intent retained | OK |
