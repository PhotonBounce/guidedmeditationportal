# DOM QA Report — R3 — 2026-06-20

## service-pages.php FAQ enrichment (12 new FAQs)

Each of the 6 service pages went from 3 FAQs → 5 FAQs (30 total).
All are emitted as FAQ JSON-LD schema for AEO/SEO.

| Service | New FAQs |
|---------|---------|
| web-development | "Can you work with my existing site?" + "What is in the handoff package?" |
| ai-agents | "How do you train the bot on my content?" + "What language models does it use?" |
| 3d-webgl | "Can I embed 3D into my existing site?" + "What 3D file formats do you accept?" |
| seo | "Do you do link building?" + "What does the monthly SEO report include?" |
| aeo | "How do I know if AI engines are citing me?" + "Does my site need structured data already?" |
| branding | "How long does a brand project take?" + "Do I own the copyright to my logo?" |

## brainstorm.php — 5 new intent handlers

- Tech stack (what technologies, what frameworks, what do you use)
- Testimonials/reviews (references, client feedback, success stories)
- Solo/team (do you work alone, freelancer, agency or freelance)
- Platform alternatives (shopify, squarespace, webflow, wix — compare + custom pitch)
- NDA/confidentiality (non-disclosure, is my idea safe, IP agreement)

## Intent count after R1+R2+R3: 16 named intent sections

## QA results (10/10)
| Check | Result |
|-------|--------|
| Tech stack | OK |
| Testimonials | OK |
| Solo/team | OK |
| Platform alternatives | OK |
| NDA | OK |
| WordPress | OK |
| Redesign | OK |
| Timeline | OK |
| Startup | OK |
| Russian/Cyrillic | OK |

## FAQ count verification
- Total FAQ entries: 30 (6 services × 5 each) ✅
- copyright FAQ present ✅
- link building FAQ present ✅
- glTF/GLB FAQ present ✅
- monthly report FAQ present ✅
- AI crawler (ClaudeBot) FAQ present ✅
- CMS/existing site FAQ present ✅
- RAG training FAQ present ✅

## template-service.php DOM QA
- template-service.php calls pb_aurora_render_service_page() correctly
- Falls back to the_content() if function not registered (safe)
- get_header() / get_footer() called correctly
- No hardcoded URLs or unsafe output (esc_html / esc_url throughout)
