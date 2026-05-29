# 🌙 SoundPad — Sleep & Focus Sounds

14 scientifically-tuned noise colors and immersive nature sounds. Sleep
timer up to 8 hours. Plays in the background. Freemium with a clean
upgrade path (rewarded-ad → one-time → subscription).

| | |
|---|---|
| **Platform** | Android 5.0+ (API 21+) |
| **Stack** | Kotlin · pure-DSP audio synthesis (no audio files) · Google Play Billing 6 · AdMob 23 · UMP consent 2.2 · Play In-App Review 2 |
| **Build** | Gradle 8.4 · AGP 8.2 · Kotlin 1.9 |
| **Status** | Ready to publish — see `PUBLISH_NOW.md` |

---

## Quick start — install on your phone

Plug in your Android device with USB Debugging on, then double-click:

```
SHIP_IT.bat
```

First run installs JDK 17 + Android SDK portably into `.tools\` and pushes
the app to your phone in ~6 minutes total. No system installs.

---

## Project layout

```
app/                          Android source (Kotlin)
  src/main/java/com/soundpad/sleep/
    MainActivity.kt           UI controller + ad/billing wiring
    AudioEngine.kt            Real-time PCM synthesis (the core)
    SoundService.kt           Foreground service + wake lock + audio focus
    BillingManager.kt         Play Billing 6 (one-time + monthly + yearly)
    RewardedAdManager.kt      Watch-ad-to-unlock-for-session flow
    InterstitialAdManager.kt  Every-3rd-stop ad cadence for free users
    ConsentManager.kt         GDPR/CCPA consent via Google UMP
    InAppReviewHelper.kt      Play In-App Review API wrapper
    AboutActivity.kt          Legal / support / restore purchases
    ...
docs/                         GitHub Pages site — privacy + terms
marketing/                    SVGs + HTML + render_pngs.bat → Play Store assets
.tools/                       Portable JDK + Android SDK (created on first run)
```

---

## The ship pipeline

| Step | Script | What it does |
|---|---|---|
| 1 | `SHIP_IT.bat` | Bootstrap + build + install on attached phone |
| 2 | `marketing/render_pngs.bat` | Render icon, feature graphic, screenshots |
| 3 | `create_keystore.bat` | One-time release signing key creation |
| 4 | `build_release_aab.bat` | Build the signed AAB for Play Console |

Read `PUBLISH_NOW.md` for the full sequence from clean machine → live on
the Play Store.

---

## Monetization model

- **Free tier** — 3 sounds (White / Pink / Brown), banner + rewarded ads,
  interstitial every 3rd stop
- **One-time $3.99** (`soundpad_pro`) — unlock all 14 sounds, no ads, forever
- **Monthly $1.99** (`soundpad_ultimate`) — same benefits, 3-day free trial
- **Yearly $14.99** (`soundpad_yearly`) — same benefits, 7-day free trial,
  saves 37% vs monthly (anchor product — highest LTV)

Rewarded-ad-for-session-unlock gives free users a taste of premium sounds,
which is the single highest-converting upgrade trigger in the funnel.

---

## Replacing test ad IDs

All AdMob IDs are centralized — edit `app/build.gradle` `defaultConfig`:

```groovy
buildConfigField "String", "ADMOB_APP_ID",        "\"...\""
buildConfigField "String", "ADMOB_BANNER_UNIT_ID", "\"...\""
buildConfigField "String", "ADMOB_INTERSTITIAL_UNIT_ID", "\"...\""
buildConfigField "String", "ADMOB_REWARDED_UNIT_ID", "\"...\""
manifestPlaceholders.admobAppId = "..."
```

Rebuild. That's it.

---

## Documentation

- `PUBLISH_NOW.md` — fastest path from clean machine to live on Play
- `LAUNCH_CHECKLIST.md` — long-form launch guide with monetization tips
- `PLAY_CONSOLE_COPY.md` — every Play form field, pre-written
- `QA_REPORT.md` — what was fixed + manual on-device test checklist
- `PRIVACY_POLICY.md` / `TERMS_OF_SERVICE.md` — source policy docs
- `docs/` — hostable HTML versions of the policies (GitHub Pages ready)

---

## License

Personal use only. Source code is not licensed for redistribution.
