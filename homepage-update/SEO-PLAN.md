# photon-bounce.com — Deep SEO + Lead-Gen Plan (get leads ASAP)

Goal: turn the homepage from a portfolio brochure into a **lead machine** that ranks for
buyer-intent searches, gets cited by AI answer engines, and converts the traffic it already
gets. Ordered so the fastest-money items come first.

---

## 0. What's already fixed in this deploy (baseline)
- Removed fake testimonials / "Receipts" / fake "Trusted by" logos / fake subscriber count
  (fake social proof *hurts* E-E-A-T and can trigger trust issues; real or nothing).
- Killed the dead portfolio links (they pointed to pages that 302 back to the homepage —
  bad internal linking). Portfolio now renders **real `pb_project` posts** with live permalinks.
- Added **meta description + canonical + robots** tags (were missing) via `inc/schema.php`.
- The theme already emits strong JSON-LD (Organization, LocalBusiness, FAQ, HowTo, Service,
  Breadcrumb, Speakable) + OG/Twitter — this deploy makes sure that module is actually **live**
  (your current production theme is an older build that wasn't emitting it).

> **Do these two things in Search Console the day you deploy** (biggest, free wins):
> 1. Verify the site in **Google Search Console** + **Bing Webmaster Tools**.
> 2. Submit the XML sitemap and **request indexing** of the homepage + top 5 service/project pages.

---

## 1. Keyword + intent map (target these, in priority order)

You sell to three buyers: **founders/startups**, **small businesses**, **research teams**.
Chase *commercial-intent* and *local* queries first — they convert; ignore vanity head terms.

| Priority | Query cluster (examples) | Intent | Landing page |
|---|---|---|---|
| P0 | "hire a developer to build a web app", "freelance saas developer", "fixed price website developer" | Buyer | Homepage / `/quote/` |
| P0 | "ai chatbot developer for small business", "custom gpt agent developer", "rag chatbot development" | Buyer | A new `/ai-agents/` service page |
| P0 | "web developer boston", "ai developer boston", "3d web developer near me" | Local buyer | Homepage + Google Business Profile |
| P1 | "three.js developer for hire", "webgl website developer", "interactive 3d website cost" | Buyer | `/3d-webgl/` service page |
| P1 | "how much does a custom web app cost", "ai chatbot price", "website cost 2026" | Research→buyer | A pricing/guide page (you already have `/#pricing`) |
| P2 | "ar app development for research", "unity ar medical visualization" | Niche buyer | `/ar-vr/` service page |
| P2 | "seo for startups", "aeo optimization service", "get cited by chatgpt" | Buyer | `/seo/` + `/aeo/` pages |

**Action:** create one dedicated, indexable **service page per cluster** (`/web-development/`,
`/ai-agents/`, `/3d-webgl/`, `/seo/`, `/aeo/`, `/branding/`). Each page: H1 = the exact query,
300–800 words, a price table, 1–2 real project links, FAQ block, and a single CTA (`/quote/` or
`/book/`). Right now everything funnels through the homepage — that caps you to ~1 ranking page.
Service pages are how a studio site gets 6–10 ranking pages instead of 1.

---

## 2. On-page SEO (per page)
- **Title tags:** unique, ≤60 chars, lead with the query + "| Photon Bounce". Homepage is good
  ("Custom Web Apps, AI & 3D/AR Studio for Founders"). Service pages need their own.
- **One H1 per page** = primary keyword. Use H2s for sub-topics (you have clean H2s already).
- **Meta descriptions:** 140–160 chars, write them as ad copy with the price hook
  ("Fixed-price web apps from $40. AI agents, 3D/WebGL, SEO. Boston studio, ships in days.").
- **Internal links:** homepage → each service page; each project → its service page; service
  pages → `/quote/`. This is your biggest untapped on-page lever now that dead links are gone.
- **Image SEO:** descriptive `alt` (the dynamic portfolio grid now uses the project title as alt),
  compress hero/video poster, lazy-load (already done), serve WebP where possible.

## 3. Technical SEO
- **Sitemap:** ensure `/wp-sitemap.xml` (WP core) or Yoast/RankMath sitemap is live and submitted.
- **Canonical/robots:** now emitted by the theme (guarded off if you run Yoast/RankMath).
- **Core Web Vitals:** the homepage is heavy (Three.js hero, matrix canvases, video). Targets:
  LCP < 2.5s, INP < 200ms, CLS < 0.1. Quick wins: `preconnect` to YouTube/fonts, defer non-critical
  JS, set explicit width/height on images, and make the autoplay intro video `preload="none"` +
  poster image. Test on PageSpeed Insights mobile (that's the score Google uses).
- **HTTPS, one host:** force `https://www.photon-bounce.com` (or non-www) and 301 the other — pick
  one and be consistent (mixed signals dilute ranking).
- **Schema validation:** after deploy, run the homepage through Google Rich Results Test + 
  schema.org validator; fix any warnings (the email fields across modules should all match — pick
  one public address, e.g. `pb@photon-bounce.com`, and use it everywhere).

## 4. AEO — get cited by ChatGPT / Perplexity / Google AI (you asked for this)
This is where studios win cheaply in 2026. AI engines cite pages that **answer a question crisply
in plain text with structure**.
- Keep the **FAQ schema** (you have it) and expand FAQ to cover: "how much does X cost", "how long
  does it take", "do you work with non-US clients", "what payment do you accept" — these are the
  questions buyers ask AI before they contact you.
- Add a short, factual **"Pricing" answer block** in plain prose near `/#pricing` ("A custom web
  app at Photon Bounce costs $40–$750 depending on scope…") — AI loves quotable, specific numbers.
- The theme already emits **Speakable** + **HowTo** schema — good for voice/AI surfacing.
- Make sure `robots.txt` does **not** block `GPTBot`, `PerplexityBot`, `ClaudeBot`, `Google-Extended`
  unless you want to opt out of AI traffic (you don't — these send referrals).

## 5. Local SEO (Boston) — fast, high-intent, low competition
- Create/claim a **Google Business Profile** ("Photon Bounce", category: Website Designer /
  Software Company, service-area business). This alone can rank you for "web developer boston" fast.
- LocalBusiness schema is already in the theme (Boston, MA, geo) — keep NAP (name/address/phone)
  **identical** across the site, GBP, and any directory.
- Get listed in 5–10 quality directories (Clutch, GoodFirms, DesignRush, LinkedIn, local chamber).

## 6. Content / authority (compounding, start now)
- Publish 1–2 posts/month targeting **bottom-funnel** questions: "What does a custom AI chatbot
  cost in 2026?", "Should a startup hire a freelancer or an agency?", "Three.js vs. video for a
  3D hero". Each post links to the matching service page + a CTA. This is how you rank for the P1
  "cost/price" cluster without paying for ads.
- Turn your real projects into proper **case studies** (problem → approach → result → tech). Real
  case studies = the social proof we just removed-the-fake-version of, and they rank + convert.
- Backlinks: list the free guides on relevant roundups; answer questions on Reddit/Indie Hackers/
  Stack Overflow with a link to a genuinely useful page (not spam).

## 7. Conversion / lead-gen (fastest revenue — do alongside SEO)
Traffic only pays if it converts. The homepage now leads with **My Apps** + clean portfolio +
a working sales chatbot (text **and** voice). Tighten the funnel:
- **One primary CTA** repeated: "Get a fixed-price quote" → `/quote/`. The sticky bar + exit modal
  already push this; make sure `/quote/` and `/book/` pages actually exist and work.
- **Lead capture** is wired (`pb/v1/lead`, the gated playbook). Make sure leads land somewhere you
  check (email/CRM) — a form that goes nowhere is lost money.
- **Chatbot = your 24/7 SDR.** With an LLM key (optional) it's smarter, but even the built-in
  responder now recommends a tier + price + booking link on every turn. Watch what people ask it
  and feed real FAQs back into the FAQ section (double win: CRO + AEO).
- **Speed to lead:** put the phone/WhatsApp (already on the page) above the fold on mobile.

## 8. Measurement (so you know what's working)
- Google Search Console (impressions, queries, CTR, position) — your #1 SEO dashboard.
- GA4 or a privacy-light analytics (Plausible) — track `/quote/` starts, form submits, chatbot
  opens, "book" clicks as **conversions**.
- Review monthly: which queries bring impressions but low CTR (→ rewrite title/meta), which pages
  get traffic but no conversions (→ fix CTA).

---

## 30-day action checklist (highest ROI first)
1. **Deploy this update** (My Apps top, no fake content, working portfolio, schema live).
2. **Search Console + Bing** verify, submit sitemap, request indexing. *(day 1)*
3. **Google Business Profile** — create + verify (Boston). *(week 1)*
4. Build **6 service pages** (web, AI agents, 3D/WebGL, SEO, AEO, brand), each with H1=query,
   price table, FAQ, 1 CTA. *(weeks 1–2)*
5. Internal-link homepage ↔ service pages ↔ projects. *(week 2)*
6. Write **3 "cost/price" blog posts** for the high-intent research cluster. *(weeks 2–4)*
7. PageSpeed pass on the homepage (defer video, preconnect, image dims). *(week 3)*
8. Turn 3 real projects into full **case studies**. *(weeks 3–4)*
9. List on **Clutch/GoodFirms/DesignRush** + 5 directories. *(week 4)*

This sequence front-loads the free, fast, high-intent wins (indexing, GBP, service pages) that
actually produce inbound leads, then layers in the compounding content/authority work.
