# Photon‑Bounce homepage update — what to upload

This bundle contains everything you need to publish the homepage changes. **Nothing
here touches the live server** — you upload it yourself through your usual workflow
(FTP / File Manager / child‑theme update). All edits are **additive**: your original
homepage content is unchanged; I only appended a "My Apps" strip and fixed the chatbot.

---

## What changed (plain English)

1. **"My Apps" strip** added to the homepage, just below your existing Portfolio
   section. It's a horizontal scroll row of 5 cards — OccupantKiller, Ausis, Guided
   Meditation Portal, GovDAO, Friendai — each linking to its microsite. Uses your
   theme's existing card styling, so it matches the rest of the page.
2. **Chatbot no longer auto‑talks.** It used to start speaking on page load *and*
   when the window opened. Both auto‑triggers are removed — voice now plays **only**
   when a visitor presses the "Play AI voice welcome" button, and the chat window
   opens **only** when someone clicks the orb.
3. **Chatbot now sells your services**, not your personal life. The welcome message
   and system prompt were rewritten to greet the visitor, ask what they're building,
   and recommend the right service + price. (Backend pricing menu + content filter
   kept intact.)
4. **Orb button restyled** — was rendering oddly because of conflicting CSS rules.
   It's now a clean pill in the bottom‑right corner.
5. **Better voice ready for your ElevenLabs key** — see step C below. Without a key
   it falls back to the browser's robotic voice (the "poor voice quality" you heard).

---

## A. Files to upload — destination map

Upload preserving these exact paths. **Back up the originals first** (copy the five
live files somewhere before overwriting).

| File in this bundle | Upload to (on the server) |
|---|---|
| `theme-files/index.php`            | `/wp-content/themes/photon-bounce-aurora/index.php` |
| `theme-files/main.js`              | `/wp-content/themes/photon-bounce-aurora/main.js` |
| `theme-files/main.css`             | `/wp-content/themes/photon-bounce-aurora/main.css` |
| `theme-files/inc/brainstorm.php`   | `/wp-content/themes/photon-bounce-aurora/inc/brainstorm.php` |
| `theme-files/inc/elevenlabs.php`   | `/wp-content/themes/photon-bounce-aurora/inc/elevenlabs.php` |

> If you run a **child theme**, these files live in the parent theme
> `photon-bounce-aurora`. Update them there (or copy into the child theme if it
> overrides them). The five files above are the only theme files that changed.

## B. Card images to upload

Create the folder `/wp-content/uploads/photon-apps/` if it doesn't exist, then upload
all five JPGs into it:

| File in this bundle | Upload to |
|---|---|
| `uploads-photon-apps/occupantkiller.jpg` | `/wp-content/uploads/photon-apps/occupantkiller.jpg` |
| `uploads-photon-apps/ausis.jpg`          | `/wp-content/uploads/photon-apps/ausis.jpg` |
| `uploads-photon-apps/guidedmed.jpg`      | `/wp-content/uploads/photon-apps/guidedmed.jpg` |
| `uploads-photon-apps/govdao.jpg`         | `/wp-content/uploads/photon-apps/govdao.jpg` |
| `uploads-photon-apps/friendai.jpg`       | `/wp-content/uploads/photon-apps/friendai.jpg` |

The card image filenames must match exactly — `index.php` references them by these
names.

## C. Turn on the good ElevenLabs voice (you do this — I never touch your key)

You already have an ElevenLabs subscription, so the integration is wired and ready;
it just needs your key. **Pick one:**

- **Easy way (WordPress Customizer):** Appearance → Customize → **Voice Welcome** →
  paste your key into **"ElevenLabs API Key"**. Optionally set a **Voice ID** (default
  is "Sarah", a free premade voice — paste any voice ID from your ElevenLabs Voice
  Library for a different one). Save/Publish.
- **Or in `wp-config.php`:** add a line near the top:
  `define( 'PB_ELEVENLABS_KEY', 'your-key-here' );`

That's the whole fix — with a key present, the welcome and replies use your premium
ElevenLabs voice (model `eleven_turbo_v2_5`) instead of the browser voice. **Do not
send me the key** — set it yourself on the server.

---

## D. After you upload — quick check

1. Load the homepage. Scroll to just below Portfolio → you should see the **"My Apps"**
   strip with 5 cards; each "Visit microsite" / "Play it" link opens the right app.
2. **No voice should play on its own.** It should stay silent until you press
   "Play AI voice welcome".
3. Click the orb (bottom‑right pill) → the chat opens, greets you, and talks about
   **services/pricing**, not personal stuff. Type a question → you get a real reply.
4. With your ElevenLabs key set, the voice should sound natural.

---

## E. Heads‑up: the microsite links

The strip links to `/occupantkiller/`, `/ausis/`, `/guidedmeditation/`, `/govdao/`
and `/friendai/`. **OccupantKiller** and **Guided Meditation** are already live.
The other three resolve only once **their own app projects** publish their microsites
to those paths (those aren't mine to deploy — the homepage just links to them). If any
of `/ausis/`, `/govdao/`, `/friendai/` isn't live yet, that card's link will 404 until
that app deploys — the card itself is fine and will start working automatically once
the microsite is up. Tell me if you'd rather I temporarily hide a card until its
microsite is ready.
