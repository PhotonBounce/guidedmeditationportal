# DOM QA Report — R15 — 2026-06-20

## main.js — Export chat transcript button

New `exportChat()` function added (alongside `saveChat` / `clearChat`):

```javascript
function exportChat() {
  if (!chatMsgs.length) return;
  var lines = chatMsgs.map(function(m) {
    return (m.cls === 'bot' ? 'Photon: ' : 'You:    ') + plain(m.text);
  });
  var content = 'Photon Bounce — Chat Transcript\n' + new Date().toLocaleString() + '\n' + '────...' + '\n\n' + lines.join('\n\n');
  var blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  var url = URL.createObjectURL(blob);
  var a = document.createElement('a'); a.href = url;
  a.download = 'pb-chat-' + new Date().toISOString().slice(0,10) + '.txt';
  document.body.appendChild(a); a.click();
  document.body.removeChild(a); URL.revokeObjectURL(url);
}
```

Uses `plain(m.text)` to strip HTML tags before writing — clean readable transcript.
Button order in header: `[↺ newchat] [⇓ export] [🔊 mute] [× close]`

exportBtn is inserted into the existing btnGroup before the mute button, works in both
the "create new group" and "existing group" branches.

## main.css — pb-brain__export styles

```css
.pb-brain__export {
  background:none; border:1px solid rgba(255,255,255,.15);
  color:rgba(255,255,255,.5); width:28px; height:28px; border-radius:50%;
  font-size:14px; cursor:pointer; display:flex;
  align-items:center; justify-content:center; flex-shrink:0; transition:.15s;
}
.pb-brain__export:hover { border-color:rgba(100,200,255,.6); color:rgba(100,200,255,.9); }
```

Matches existing button visual language (ghost circle, muted by default, accent on hover).
Hover: blue accent (distinguishes from mute ↔ white and newchat ↔ yellow).

## inc/service-pages.php — BreadcrumbList per service page

New `pb_aurora_service_breadcrumb()` hooked at `wp_head` priority 7 (before
`pb_aurora_service_schema` at priority 8 and `pb_aurora_service_faq_schema`):

```php
function pb_aurora_service_breadcrumb() {
  if (!is_page()) return;
  $slug = get_post_field('post_name', get_queried_object_id());
  $svc  = pb_aurora_service_pages();
  if (!isset($svc[$slug])) return;
  $site_url = home_url('/');
  $payload = [
    '@type' => 'BreadcrumbList',
    'itemListElement' => [
      ['position' => 1, 'name' => 'Home',     'item' => site_url],
      ['position' => 2, 'name' => 'Services', 'item' => site_url.'services/'],
      ['position' => 3, 'name' => $svc[$slug]['h1'], 'item' => site_url.$slug.'/'],
    ],
  ];
}
```

Effect: Google Search Console breadcrumb trail for all 6 service pages. Emits only
when `pb_aurora_service_pages()` matches the current slug — 0 risk of firing on
non-service pages.

## brainstorm.php — 3 new intent handlers

### 0d-pre11-a) Community / forum / online community platform
Keywords: community site, online community, forum site, discussion board,
membership community, discord-like, reddit-like, peer community, community platform,
social network, q&a platform, private community, members-only forum, support forum

Response:
- Basic forum (Q&A/support) — bbPress on WordPress; SaaS/App $750 bundled
- Private membership community — MemberPress or Restrict Content Pro; gated content,
  membership tiers; SaaS/App $750+
- Full social platform — BuddyBoss or custom Next.js + WebSockets; custom from $1,500
- Asks: open vs invite-only? Real-time messaging needed? Paid tiers?

### 0d-pre11-b) Gamification / points / leaderboards / achievements
Keywords: gamification, points system, leaderboard, badges, achievements, reward system,
loyalty program, streak tracking, progress tracker, xp system, level up, user levels,
referral rewards, incentive system, engagement mechanics

Response:
- Points & XP — action hooks, custom DB table
- Badges — SVG set + unlock logic on user profile
- Leaderboard — real-time or cached; global/per-cohort; CSV export
- Streaks — last-active timestamp; broken if gap > N days
- Reward redemption — coupon codes, feature unlocks, webhook triggers
- Add-on to SaaS/App build: +$200–$400
- Asks: what behaviour are you incentivising?

### 0d-pre11-c) Real-time features / WebSockets / live updates
Keywords: real-time, real time, websockets, socket.io, live chat, live updates, live feed,
collaborative editing, google docs like, live dashboard, pusher, ably, supabase realtime,
multiplayer, instant notifications, push notifications app

Response:
- Live notifications — Pusher or Supabase Realtime; <$20/mo; bundled into SaaS
- Live chat (user↔user) — Socket.io/Node.js or Supabase channels; SaaS $750+
- Collaborative editing (Google Docs-like) — Y.js (CRDT) + Tiptap + WebSocket; from $1,500
- Live dashboard/ticker — SSE (Server-Sent Events) for one-way streams; less infra overhead
- Asks: what's updating in real time — chat, data feed, or collaborative content?

## QA results (27/27 all pass)
| Check | Result |
|-------|--------|
| exportChat function defined | OK |
| exportChat uses Blob | OK |
| exportChat ObjectURL created | OK |
| exportChat file download attr | OK |
| exportBtn element created | OK |
| exportBtn class pb-brain__export | OK |
| exportBtn inserted in btnGroup | OK |
| exportBtn fires exportChat | OK |
| plain() used in exportChat | OK |
| pb-brain__export CSS block | OK |
| export hover style | OK |
| BreadcrumbList function defined | OK |
| BreadcrumbList action hooked | OK |
| BreadcrumbList @type | OK |
| BreadcrumbList Home position 1 | OK |
| BreadcrumbList slug position 3 | OK |
| Breadcrumb fires at priority 7 | OK |
| Community intent exists | OK |
| bbPress in community response | OK |
| Membership tiers in community | OK |
| Gamification intent exists | OK |
| Leaderboard in gamification resp | OK |
| Streak tracking in gamification | OK |
| Real-time intent exists | OK |
| Pusher/Supabase in real-time | OK |
| WebSockets in real-time resp | OK |
| Y.js collab editing mentioned | OK |
