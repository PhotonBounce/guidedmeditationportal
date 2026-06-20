# DOM QA Report — R14 — 2026-06-20

## main.js — Contextual follow-up chips (smarter)

Follow-up chip injection now reads the first user message from the DOM
(`.pb-brain__msg--me`) to decide which chips to show after the 2nd bot message.

Default set: `[Book a call] [All pricing] [Portfolio]`

Swaps triggered by keyword match in first user message:
| Pattern | Chip replaced | New chip |
|---------|--------------|----------|
| pric/cost/how much/budget | All pricing | ⏱ How long? |
| portfolio/past work/examples | Portfolio | 🕶 Timeline |
| book/call/meet/schedule | Book a call | 💳 Payment |

This avoids showing the user a chip for something they already asked about,
making the suggestions contextually relevant.

Note: DOM-based approach used (not `history[]`) because `history.push()` runs
AFTER `addMsg('bot')` fires in the submit handler, so `history` is empty at
chip injection time. Reading `.pb-brain__msg--me` is reliable and fast.

## main.js — Rotating textarea placeholder

Cycles through 7 example questions every 3.5 seconds when the input is
empty and unfocused:
1. "What do you want to build?"
2. "What does a marketplace cost?"
3. "How long for an AI chatbot?"
4. "Can you build in 3D / WebGL?"
5. "What is AEO and why does it matter?"
6. "Do you handle SEO + Core Web Vitals?"
7. "How does milestone payment work?"

Guard: `if (document.activeElement !== input && !input.value)` — never
changes placeholder while the user is typing or has content in the field.
Uses `setInterval` inside the `if (input)` block (already in scope).

## inc/schema.php — Conditional Review/testimonial schema

Two Review objects emitted on the homepage (inside `is_front_page()`) when
both `pb_t1_author` + `pb_t1_body` Customizer options are set:

```php
'@type'        => 'Review',
'itemReviewed' => ['@id' => site_url + '#organization'],
'reviewRating' => ['@type' => 'Rating', 'ratingValue' => 5, 'bestRating' => 5],
'author'       => ['@type' => 'Person', 'name' => sanitize_text_field($t['author'])],
'reviewBody'   => sanitize_text_field($t['body']),
```

Not emitted if either field is empty (prevents fake/empty review schema).
Owner populates via Customizer: pb_t1_author / pb_t1_body / pb_t2_author / pb_t2_body.

## brainstorm.php — 3 new intent handlers

### 0d-pre10-a) Admin dashboard / internal tools
Keywords: admin dashboard, internal tool, back-office, admin panel, management portal,
staff portal, team dashboard, reporting dashboard, control panel, custom crm

Response: SaaS/App $750+; RBAC, data tables (filter/sort/export), Chart.js/D3 charts,
REST/GraphQL API layer, audit log; custom CRM integrations scoped separately; asks about
data model + users

### 0d-pre10-b) Tech stack / framework recommendation
Keywords: what tech stack, which framework, react vs next, should i use wordpress,
webflow vs wordpress, what do you use, tech recommendation, best stack for

Response: decision matrix — WordPress (content sites, CMS-editable), Next.js/React
(interactive apps), Headless WP + Next.js (best of both), Shopify (serious e-commerce),
WebGL/Three.js (3D/AR); asks for core use case

### 0d-pre10-c) Headless CMS / JAMstack / API-first
Keywords: headless cms, headless wordpress, api-first, decoupled frontend, jamstack,
nextjs headless, sanity.io, contentful, strapi, ssr vs ssg, edge rendering

Response: full stack — WP/Sanity/Strapi as CMS, Next.js/Astro frontend, Vercel/Netlify
hosting, REST/GraphQL + webhook revalidation; when-to-go-headless vs when-to-stay-coupled
decision guide; asks about content model + traffic

## QA results (19/19 all pass)
| Check | Result |
|-------|--------|
| Admin dashboard intent | OK |
| Role-based access in response | OK |
| Tech stack intent | OK |
| WordPress in stack response | OK |
| Headless/JAMstack intent | OK |
| Next.js + Sanity in response | OK |
| ISR/SSR mentioned | OK |
| Review @type | OK |
| Review itemReviewed | OK |
| Review conditional on theme mod | OK |
| sanitize_text_field on review | OK |
| chipDefs array | OK |
| DOM context from first user msg | OK |
| pricing swap chip | OK |
| booking swap chip | OK |
| phList rotation array | OK |
| phIdx rotation logic | OK |
| setInterval for rotation | OK |
| unfocused guard | OK |
