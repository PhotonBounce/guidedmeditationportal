# DOM QA Report — R18 — 2026-06-20

## main.js — Message timestamps on hover

Every chat message (user, bot, err) now gets a `<time>` element injected as its
last child, with the send time in HH:MM format and an ISO-8601 `datetime` attribute:

```javascript
var _tsEl = document.createElement('time');
_tsEl.className = 'pb-brain__ts';
var _tn = new Date();
_tsEl.textContent = _tn.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
_tsEl.setAttribute('datetime', _tn.toISOString());
div.appendChild(_tsEl);
```

- `toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })` uses the browser's
  locale for AM/PM vs 24h format — no hardcoded format string
- `datetime` attr carries the full ISO-8601 timestamp for machine-readability / accessibility
- Appended after the copy button (bot messages), or after the text (user messages)
- Invisible by default; shown on hover via CSS opacity transition

## main.css — Timestamp styles

```css
.pb-brain__ts {
  display:block; font-size:10px; color:rgba(255,255,255,.25);
  margin-top:4px; opacity:0; transition:opacity .2s;
  pointer-events:none; user-select:none;
}
.pb-brain__msg--me .pb-brain__ts { text-align:right; }
.pb-brain__msg--bot .pb-brain__ts { text-align:left; }
.pb-brain__msg:hover .pb-brain__ts { opacity:1; }
```

- `pointer-events:none` — timestamp is informational only, doesn't interfere with clicks
- `user-select:none` — hovering over the timestamp area won't start a text selection
- Alignment follows the bubble alignment: right for user, left for bot
- 200ms opacity transition — feels snappy but not instant

## brainstorm.php — 3 new intent handlers

### 0d-pre14-a) Web scraping / workflow automation / API integration
Keywords: web scraping, data scraping, scrape website, scrape data, automate workflow,
workflow automation, zapier alternative, make.com, api integration, connect apis,
data pipeline, webhook integration, third-party api, integrate crm, integrate stripe,
integrate api, n8n, automation script

Response:
- Web scraping: Python (Beautiful Soup/Playwright) or Node.js (Puppeteer); depends on
  JS rendering + anti-bot measures; from $150
- Workflow automation: Zapier, Make.com, or self-hosted n8n; trigger→action chains;
  custom webhook endpoint included
- API integration: REST/GraphQL; OAuth 2, API key, JWT; any public or private API
- Data pipeline: ETL fetch→transform→store; cron or webhook triggered; outputs to
  DB/CSV/Google Sheets/Airtable
- Common APIs bundled free: Stripe, WooCommerce webhooks, Mailchimp, SendGrid,
  Google Sheets, HubSpot

### 0d-pre14-b) Social media strategy / content calendar
Keywords: social media strategy, content calendar, social media content,
instagram strategy, tiktok strategy, linkedin content, twitter strategy,
social media marketing, content plan, posting schedule, social content plan

Response:
- Brand identity → all social profiles in one kit (headers, profile, story templates)
- Content repurposing engine → SEO articles auto-formatted for social with OG + schema
- AI content toolkit → custom Claude/GPT system prompt tuned to brand voice
- Out of scope: scheduling, community management, posting → recommend Buffer/Hootsuite
- Closes: "brand design layer, content strategy layer, or writing toolkit?"

### 0d-pre14-c) Podcast website / audio player / voice app
Keywords: podcast website, podcast player, audio player, voice app, podcast hosting,
podcast setup, audio content, audio streaming, podcast directory, spotify embed,
soundcloud embed, rss feed podcast, show notes, wavesurfer

Response:
- Podcast website: HTML5 player or Spotify/SoundCloud embed; episode archive with show
  notes + transcripts; RSS feed for Apple/Spotify directories; from $500
- WaveSurfer.js: custom waveform player for immersive audio landing pages; scrub+play
- Podcast hosting: not in-house; recommend Buzzsprout, Transistor, or Anchor (free)
- Voice UX: SpeechSynthesis + SpeechRecognition bundled in SaaS builds; Alexa/Google
  Assistant out of scope
- Closes: "podcast site, embedded player, or voice-driven interface?"

## QA results (21/21 all pass)
| Check | Result |
|-------|--------|
| _tsEl time element created | OK |
| _tsEl pb-brain__ts class | OK |
| timestamp toLocaleTimeString | OK |
| datetime ISO attribute set | OK |
| _tsEl appended to div | OK |
| pb-brain__ts display:block | OK |
| pb-brain__ts opacity:0 | OK |
| ts hover opacity:1 | OK |
| ts user right-align | OK |
| ts bot left-align | OK |
| Scraping intent keywords | OK |
| Playwright in scraping | OK |
| Puppeteer in scraping | OK |
| n8n in automation | OK |
| Stripe bundled free | OK |
| Social media intent keywords | OK |
| AI content toolkit | OK |
| Buffer/Hootsuite | OK |
| Podcast intent keywords | OK |
| WaveSurfer.js | OK |
| Buzzsprout | OK |
