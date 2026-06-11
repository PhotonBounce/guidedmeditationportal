# SoundPad — Launch Checklist

The fastest path from "code is ready" to "earning revenue on Google Play."
Follow this in order — every step is required.

---

## Phase 0 — Build it on your machine (today, 30 minutes)

You need Android Studio (or just the command-line gradle wrapper + Android SDK).

### Option A — Android Studio (recommended for first-timers)

1. Install Android Studio: https://developer.android.com/studio
2. **File → Open** → select `D:\sleepapprepo`
3. Wait for Gradle sync (first time downloads ~500 MB)
4. Connect your Galaxy 14 via USB. Enable USB debugging:
   - Settings → About phone → tap **Build number** 7 times
   - Settings → Developer options → enable **USB debugging**
5. Allow USB debugging when prompted on the phone
6. Click ▶ Run. The app installs in ~30 seconds.

### Option B — Command line (fastest if you already have the SDK)

```bash
cd D:\sleepapprepo
.\gradlew.bat installDebug      # builds + installs to attached device
# Or for a release-signed APK:
.\gradlew.bat assembleRelease
adb install app\build\outputs\apk\release\app-release.apk
```

Don't have the gradle wrapper jar yet? Easiest: open the project in Android
Studio once — it'll create `gradle\wrapper\gradle-wrapper.jar` automatically.

### What to test on the phone

Run through the manual QA checklist at the bottom of `QA_REPORT.md`.
Anything broken → fix → rebuild → reinstall.

---

## Phase 1 — Host your privacy policy & terms (10 minutes)

Google Play **requires** a hosted URL. GitHub Pages is the cheapest path.

1. Create a free GitHub account if you don't have one
2. Create a new public repo called `soundpad-app` (or any name)
3. Add two files to the repo's root:
   - `privacy.md` — copy/paste contents of `PRIVACY_POLICY.md`
   - `terms.md`   — copy/paste contents of `TERMS_OF_SERVICE.md`
4. Repo → Settings → Pages → Source: `main` branch / root → Save
5. Wait ~60 seconds. Your URLs will be:
   - `https://<your-username>.github.io/soundpad-app/privacy`
   - `https://<your-username>.github.io/soundpad-app/terms`
6. Open `app/src/main/java/com/soundpad/sleep/AboutActivity.kt` and replace
   the `PRIVACY_URL` / `TERMS_URL` / `SUPPORT_EMAIL` constants.

---

## Phase 2 — Google Play Console (one-time, ~30 minutes)

> You said you're already authenticating — this is what to do once that's done.

1. Go to https://play.google.com/console (you've already paid the $25)
2. Click **Create app**
   - App name: **SoundPad — Sleep & Focus Noise**
   - Default language: English (US)
   - App or game: **App**
   - Free or paid: **Free**
   - Accept declarations
3. Complete the **Dashboard → setup tasks**:
   - App access → "All functionality available without restrictions"
   - Ads → **Yes, contains ads**
   - Content rating → fill questionnaire (you'll get "Everyone")
   - Target audience → 18+ (or 13+; avoid <13 to skip COPPA paperwork)
   - News app → No
   - Data safety → see Phase 4

---

## Phase 3 — AdMob (15 minutes, do this BEFORE Phase 4)

1. https://admob.google.com → sign in
2. **Apps → Add App → Android → No, app is not on a store yet** (for now)
3. App name: SoundPad. Copy your **App ID** — format `ca-app-pub-XXXXXX~XXXXXX`
4. Inside that app, **Add Ad Unit** three times:
   - Banner — name "SoundPad Banner"   → copy unit ID
   - Rewarded — name "SoundPad Unlock" → copy unit ID
   - (Optional) Interstitial later
5. In your code, replace the three test IDs:
   - `app/src/main/AndroidManifest.xml` → `APPLICATION_ID` meta-data
   - `app/src/main/res/layout/activity_main.xml` → `AdView ads:adUnitId`
   - `app/src/main/java/com/soundpad/sleep/RewardedAdManager.kt` → `UNIT_ID`

> Until your real IDs propagate (~24h after first request), expect "no fill"
> — that's normal. Test IDs always fill.

---

## Phase 4 — In-app products (10 minutes)

In Play Console → your app → **Monetize → Products**:

### One-time product
| Field | Value |
|---|---|
| Product ID | `soundpad_pro` *(must match exactly)* |
| Name | SoundPad Pro |
| Description | Unlock all 14 sounds and remove ads forever |
| Price | $3.99 |
| Status | **Active** |

### Subscription — Monthly
**Monetize → Subscriptions → Create**
| Field | Value |
|---|---|
| Product ID | `soundpad_ultimate` |
| Name | SoundPad Ultimate (Monthly) |
| Base plan ID | `monthly` |
| Billing period | 1 month |
| Price | $1.99 |
| Free trial | 3 days (huge boost to conversion) |
| Status | **Active** |

### Subscription — Yearly  (this is your highest-LTV product)
| Field | Value |
|---|---|
| Product ID | `soundpad_yearly` |
| Name | SoundPad Ultimate (Yearly) |
| Base plan ID | `yearly` |
| Billing period | 1 year |
| Price | $14.99 |
| Free trial | 7 days |
| Status | **Active** |

> **Why yearly first:** sleep apps see 60–80% of revenue from yearly subs
> once they're offered. The $14.99/yr vs $1.99×12=$23.88 framing converts.

---

## Phase 5 — Sign the release build (one time, keep keystore SAFE forever)

In Android Studio: **Build → Generate Signed Bundle / APK → Android App Bundle**.

- Create new keystore: save to e.g. `D:\soundpad-release.jks` — back it up to
  Google Drive AND a USB drive. If you lose it, you can never update the
  app on Play again.
- Key alias: `soundpad-key`
- Validity: 25 years
- Use a strong password — store it in a password manager.

Then build the release `.aab`. It lands in `app\release\app-release.aab`.

> **Optional:** to enable command-line release builds, edit
> `~/.gradle/gradle.properties` and add:
> ```
> SOUNDPAD_KEYSTORE=D:\\soundpad-release.jks
> SOUNDPAD_KEY_ALIAS=soundpad-key
> SOUNDPAD_STORE_PASSWORD=...
> SOUNDPAD_KEY_PASSWORD=...
> ```
> Then `gradlew bundleRelease` produces a signed AAB.

---

## Phase 6 — Store listing (30 minutes)

Play Console → **Grow → Store presence → Main store listing**:

### Short description (80 chars)
> White noise, brown noise, rain, ocean & sleep sounds. Fall asleep faster.

### Full description (use as-is)
```
SoundPad is your sleep and focus multi-tool. Scientifically-tuned noise
colors and immersive nature sounds mask distractions, calm racing minds,
and help you fall asleep faster — or focus deeper during work and study.

🔊 14 HIGH-QUALITY SOUNDS
• White Noise — masks all frequencies equally
• Pink Noise — warm, natural, the most popular sleep sound
• Brown Noise — deep bass rumble, loved for ADHD focus
• Blue Noise — crisp masking for tinnitus relief
• Violet Noise — ultra-bright for extreme masking
• Gentle Rain · Ocean Waves · Campfire · Forest Wind · Thunder Roll
• Box Fan — classic bedroom hum
• Spaceship · Womb Sounds · Crystal Bowls — original synthetics

⏱ SLEEP TIMER — Set it and forget it: 15 minutes to 8 hours.

🎚 VOLUME CONTROL — Fine-tune the perfect masking level.

🔋 BACKGROUND PLAYBACK — Screen off, phone locked, app closed — keeps playing.

✨ SOUNDPAD PRO
• One-time unlock: $3.99 — own it forever, remove all ads
• Monthly subscription: $1.99/month — 3-day free trial
• Yearly subscription: $14.99/year — 7-day free trial, save 37%

Made for restless sleepers, focused workers, anxious minds, and tired
parents. 🌙
```

### Required assets

| Asset | Size | Notes |
|---|---|---|
| App icon | 512×512 PNG | Make one in Canva ("moon app icon"). The in-app launcher is already set up. |
| Feature graphic | 1024×500 PNG | Required. Show the app + tagline. |
| Phone screenshots | 1080×1920+ | Minimum 2, recommended 4–8. Take live on your Galaxy 14: Power+Volume-Down. |
| Tablet screenshots | optional | Skip for v1 |

### Tag the right categories
- Category: **Health & Fitness**
- Tags: Sleep, Meditation, Relaxation, White noise, Focus
- Email: your real email (won't be public unless you toggle it)
- Website: your GitHub Pages URL or a Linktree

---

## Phase 7 — Data safety form (15 minutes)

Play Console → **App content → Data safety**. Answer:

- Does your app collect or share data? **Yes** (because of AdMob)
- Data types collected:
  - **Device or other IDs** (Advertising ID) — for ads — not shared
  - **App activity** (crash reports) — for app functionality — not shared
  - **In-app purchase status** — for app functionality — not shared
- Is data encrypted in transit? **Yes** (HTTPS by default)
- Can users request deletion? **Yes** — they can uninstall

This usually takes 1 form submission. Match the answers above to what
`PRIVACY_POLICY.md` says, otherwise Play will reject.

---

## Phase 8 — Submit & wait (5 minutes + 3–7 days review)

1. **Production → Create new release**
2. Upload `app-release.aab` from Phase 5
3. Release name: `1.0.5 — Brand Update & Bug Fixes`
4. Release notes:
   ```
   1.0.5 — Brand Update & Bug Fixes
   • Completed brand transition from SoundPad/ZenPulse to Ausis.
   • Upgraded AI assistant to AusisBot.
   • Decluttered the main dashboard by moving the app description card to the onboarding screens.
   • Replaced moon/eclipse toolbar icons with the premium new Ausis app icon.
   • Resolved layout inflation and Switch view crashes on physical devices.
   • Fixed bottom navigation menu transition issues.
   • Integrated production-ready AdMob support for release configurations.
   • Updated privacy policy and terms links to point to the new branded paths.
   ```
5. **Review release → Start rollout to Production**
6. Google reviews in 3–7 days for first submission.

---

## Phase 9 — After launch: monetization optimization

These move the needle 5–10×, in order of impact:

| Lever | Effort | Impact |
|---|---|---|
| **TikTok organic** — post "Brown noise for ADHD focus" demo videos, link to Play | 1h/day | Highest. Niche has millions of monthly searches. |
| **ASO** — get keywords into title: "SoundPad — White Noise · Brown Noise · Sleep Sounds" | 5min | High. Edit in Play Console store listing. |
| **A/B test pricing** — try $2.99 one-time vs $3.99, $0.99 monthly vs $1.99 | weekly | Medium-high. Play Console → Experiments. |
| **Yearly promo** — first month, mark yearly as "limited launch price" | 5min | Medium. Anchors LTV upward. |
| **Reply to every review** in first 90 days | 5min/day | Boosts ranking algorithm dramatically. |
| **Add interstitial on stop** (free tier only) | 1h dev | Medium. Triples ad revenue but watch retention. |
| **Add presets / mixer** (premium feature) | weekend | Medium. Higher perceived value → better conversion. |
| **Cross-promote with another free indie app** | varies | Low-medium, free traffic. |

---

## Phase 10 — Optional later: iOS build

React Native or Flutter port doubles addressable market. Sleep apps make
~2× as much per user on iOS due to higher willingness to pay. Plan for
month 3+ after you've validated on Android.

---

## What's already done in the code — quick reference

- ✅ 14 sounds with DSP synthesis (no audio files needed)
- ✅ Freemium model (3 free / 11 premium)
- ✅ One-time IAP + monthly + **yearly** subscription
- ✅ Rewarded ad flow ("watch ad → unlock for tonight")
- ✅ Banner ads for free users
- ✅ Sleep timer (15min → 8h)
- ✅ Foreground service + wake lock for background playback
- ✅ Adaptive launcher icon (works API 21–34)
- ✅ ProGuard rules for release shrinking
- ✅ Toolbar + About / Privacy / Terms / Support / Restore Purchases
- ✅ Onboarding dialog on first launch
- ✅ Rate-us prompt after 3 sessions
- ✅ Signing config (env-var driven)
- ✅ Hosted privacy policy & terms templates

---

*The shortest path to first dollar: finish Phase 0–5 today (test ads still
work for installs), submit Phase 6–8 tomorrow, wait 3–7 days, then start
posting "brown noise ADHD" TikToks the moment you go live.*
