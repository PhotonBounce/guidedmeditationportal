# DOM QA Report — R5 — 2026-06-20

## inc/faq.php — New file (Home FAQ module)

Created `homepage-update/theme-files/inc/faq.php` with:

### pb_aurora_faq_items() (guarded with function_exists)
10 home-page FAQ entries consumed by schema.php → FAQPage JSON-LD:
1. How much does a website cost?
2. How does your fixed-price model work?
3. What is included in every project?
4. How many revisions do I get?
5. Can you maintain my site after launch?
6. Do you work with non-technical clients?
7. Do you work with international clients?
8. What payment methods do you accept?
9. How long does a typical project take?
10. How do I get started?

### pb_aurora_faq_render() (guarded with function_exists)
Renders a `<details>/<summary>` accordion (no JS required) with:
- Section id="faq" + aria-labelledby
- `data-pb-reveal` on each item for scroll-reveal animation
- `wp_kses_post()` for answer content safety

## inc/schema.php change
Added `require_once __DIR__ . '/faq.php';` so the FAQ module auto-loads
when schema.php is included, without touching the live functions.php.

## main.css — FAQ accordion styles
- `.pb-faq` section: 60/80px vertical padding
- `.pb-faq__item`: bottom border separator
- `.pb-faq__q`: Space Grotesk 600, flex + justify-between for chevron
- Chevron via CSS data-URI SVG, rotates 180° on [open] state
- `.pb-faq__a`: 72% opacity body text, 1.7 line-height
- Mobile: 14px font below 600px

## brainstorm.php — 3 new intent handlers (total: 22 handler sections)

| Handler | Keywords |
|---------|---------|
| Non-technical client (0d-pre6a) | not technical, not a developer, don't code, just have an idea, never built a website |
| Post-launch support (0d-pre6b) | after launch, ongoing support, maintenance plan, what happens after, something breaks |
| Revision policy (0d-pre6c) | revisions, revision rounds, how many changes, change my mind, feedback rounds |

## QA results (15/15 pass)
| Check | Result |
|-------|--------|
| faq.php function_exists guard (items) | OK |
| faq.php function_exists guard (render) | OK |
| faq.php has 10 FAQ items | OK |
| faq.php renders details/summary | OK |
| faq.php uses wp_kses_post | OK |
| schema.php requires faq.php | OK |
| CSS .pb-faq section | OK |
| CSS pb-faq__item | OK |
| CSS pb-faq__q chevron | OK |
| CSS open state rotate | OK |
| Non-technical handler | OK |
| Post-launch support handler | OK |
| Revision policy handler | OK |
| Low budget handler (R4 retained) | OK |
| Urgency handler (R4 retained) | OK |
