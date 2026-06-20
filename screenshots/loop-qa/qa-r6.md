# DOM QA Report — R6 — 2026-06-20

## brainstorm.php — 4 new intent handlers

### 0d-pre9) Domain / hosting
Keywords: who hosts, hosting included, do you provide hosting, domain name, domain included, ssl certificate, https, server, vps, cloud hosting, where will it live

Response covers:
- Domain registration (~$15/yr via Namecheap/Google Domains, user owns it)
- Hosting: managed WP (WP Engine / SiteGround) vs VPS (DigitalOcean / Railway)
- SSL always included via Let's Encrypt
- Care Plan $50/mo as the "set and forget" option

### 0d-pre10) Third-party API / platform integrations
Keywords: stripe, paypal, payment gateway, api integration, zapier, make.com, hubspot, salesforce, mailchimp, klaviyo, twilio, sendgrid, airtable, notion api, webhook, crm integration, connect to my, integrate with, google analytics, meta pixel

Response covers 5 integration categories with named tools in each:
- Payments (Stripe, PayPal, Square)
- Automation (Zapier, Make.com, n8n)
- CRM / Email (HubSpot, Mailchimp, Klaviyo, SendGrid)
- Analytics (GA4, Meta Pixel, GTM)
- Databases / no-code (Airtable, Notion API, Supabase)

### 0d-pre11) Demo / live example request
Keywords: can I see a demo, show me a demo, live example, working example, see it live, demo site, see the chatbot, try the chatbot

Response: points to OccupantKiller (WebGL 3D), Ausis (Android AI), this site itself (WP + chatbot + orb), and /portfolio/ — with booking link for project-specific walkthrough

### 0d-pre12) Referral / warm lead
Keywords: someone recommended, was referred, my friend told, heard about you from, recommended by, your name came up

Response: warm acknowledgment + immediate pivot to "what's the project?"

## Total $has() blocks in brainstorm.php: 38 (includes nested Russian language sub-checks)

## QA results (10/10 pass)
| Check | Result |
|-------|--------|
| Domain/hosting handler | OK |
| Let's Encrypt mention | OK |
| API integrations handler | OK |
| Zapier + Make.com | OK |
| Demo request handler | OK |
| OccupantKiller link in demo | OK |
| Referral handler | OK |
| Non-tech client (R5 retained) | OK |
| Urgency handler (R4 retained) | OK |
| NDA handler (R3 retained) | OK |

## Cumulative loop progress
| Round | What was added |
|-------|----------------|
| R1 | 7 intents, chips, Ctrl+/, Russian voice detection, chip CSS |
| R2 | 5 intents, Russian language replies, voice mute button, orb nudge |
| R3 | 5 intents, 12 new service FAQs (30 total, 6 pages × 5 FAQs) |
| R4 | Copy-reply btn, chip focus-visible CSS, 3 intents (urgency, designs, budget) |
| R5 | inc/faq.php (10 home FAQs), FAQ CSS accordion, 3 intents (non-tech, post-launch, revisions) |
| R6 | 4 intents (hosting/domain, API/integrations, demo request, referral) |
