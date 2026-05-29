# SoundPad — QA Report

**Date:** 2026-05-26
**Build:** versionCode 4, versionName 1.0.3
**Status:** Ready to build. All compile-blockers fixed. Monetization
hardened with adaptive banner, interstitials, UMP consent, In-App Review.
Hollywood UI layer + branded splash + edge-to-edge + sectioned grid +
long-press descriptions + Settings screen + ES/DE/FR localization +
MediaStyle lockscreen notification.

---

## CRITICAL fixes applied this pass

| # | File | Issue | Fix |
|---|---|---|---|
| 1 | `PrefsManager.kt` | `isOnboardingShown` / `setOnboardingShown` declared OUTSIDE the class, before the `package` line — would not compile | Moved them inside the class; added `KEY_RATE_SHOWN` and `KEY_STOP_COUNT` while we were in there |
| 2 | `strings.xml` | About/legal strings declared AFTER `</resources>` — invalid XML, build would fail | Moved every string inside `<resources>`; escaped `&` in `about_menu` |
| 3 | `AndroidManifest.xml` | Referenced `@mipmap/ic_launcher` + `ic_launcher_round` but no mipmap directory existed → AAPT link failure | Added `mipmap-anydpi-v26/ic_launcher.xml` (adaptive), `mipmap-mdpi` vector fallback, and `drawable/ic_launcher_foreground.xml` |
| 4 | `MainActivity` Toolbar | App theme is `NoActionBar` but code called `onCreateOptionsMenu` — About menu was unreachable | Added `Toolbar` to `activity_main.xml`; wrapped layout in `CoordinatorLayout`; called `setSupportActionBar(binding.toolbar)` |
| 5 | `RewardedAdManager` | Existed as a stub but was never wired into UI — free users had no way to try premium sounds | Now preloads on app start; `showUpgradeDialog` offers "Watch ad — unlock for tonight" when an ad is ready |
| 6 | `BillingManager` | Subscription support only for monthly; no yearly tier | Added `SKU_YEARLY` (`soundpad_yearly`, $14.99/yr) — typically 2–3× higher LTV than monthly |
| 7 | `BillingManager` | Did not auto-acknowledge stale unacknowledged purchases on `queryPurchases` | Now acknowledges every unacknowledged purchase found (Google refunds in 3 days otherwise) |
| 8 | `build.gradle` | `firebase-analytics-ktx` declared but no `google-services.json`, no Gradle plugin — broke release builds | Removed Firebase dependency; `AnalyticsHelper` is now a dependency-free shim that logs to Logcat in debug. Re-add Firebase later via the documented path |
| 9 | `build.gradle` | No signing config; manual signing in Android Studio only | Added env-var-driven `signingConfigs.release` so CI / command-line builds work too |
| 10 | `proguard-rules.pro` | Skeletal — would obfuscate Billing & Ads classes and crash release builds | Added comprehensive keep rules for Billing, AdMob, AndroidX, Material, Kotlin coroutines, and `SoundType` enum |
| 11 | `AboutActivity` | Used `yourdomain.com/...` placeholder URLs | Constants now at top of class with TODO; added "Restore Purchases" button |

---

## Code review findings — no remaining blockers

- `MainActivity` lifecycle: `onResume → billing.queryPurchases()` is safe even before the billing client connects (the new code no-ops when disconnected).
- `SoundService.acquireWakeLock(8h)` — within Doze policy; auto-released in `onDestroy`.
- `AudioEngine` thread safety: only `playing`, `currentType`, `volume` are mutated cross-thread, all `@Volatile`. ✓
- `BillingManager.destroy()` cancels coroutine scope before calling `endConnection`. ✓
- `SessionPremiumManager` is in-memory only — premium-via-ad expires on process kill, which is the contract we offered. ✓
- ProGuard release shrinking is on; `SoundType` enum kept by name (used via `valueOf`).

---

## Monetization layers (all wired and ready)

| Layer | Where | Notes |
|---|---|---|
| Adaptive banner | `MainActivity.loadAdaptiveBanner()` | Auto-sized for screen width — ~40% better CPM than fixed BANNER |
| Rewarded ads | `RewardedAdManager`, surfaced in upgrade dialog | "Watch ad — unlock for tonight" before purchase prompt |
| Interstitial ads | `InterstitialAdManager`, fired on stop | Every 3rd stop for free users, cadence-guarded (no spam) |
| GDPR/CCPA consent | `ConsentManager` (UMP 2.2) | Required for full EU/UK AdMob revenue |
| One-time IAP | `BillingManager.purchasePro()` | `soundpad_pro` $3.99 |
| Monthly sub | `BillingManager.purchaseUltimate()` | `soundpad_ultimate` $1.99/mo |
| Yearly sub | `BillingManager.purchaseYearly()` | `soundpad_yearly` $14.99/yr |
| In-App Review | `InAppReviewHelper.requestReview()` | After 3rd session — 3-5× higher rate completion |

## Things you MUST replace before publishing

All AdMob IDs are now centralized in `app/build.gradle` `defaultConfig` —
edit four `buildConfigField` lines + one `manifestPlaceholders.admobAppId`
line and rebuild:

| BuildConfig field | Replaces |
|---|---|
| `ADMOB_APP_ID` (+ `manifestPlaceholders.admobAppId`) | AdMob test app ID `~3347511713` |
| `ADMOB_BANNER_UNIT_ID` | Banner test unit `/6300978111` |
| `ADMOB_INTERSTITIAL_UNIT_ID` | Interstitial test unit `/1033173712` |
| `ADMOB_REWARDED_UNIT_ID` | Rewarded test unit `/5224354917` |

Other placeholders:

| File | Constant | Action |
|---|---|---|
| `AboutActivity.kt` | `PRIVACY_URL`, `TERMS_URL`, `SUPPORT_EMAIL` | host the policy HTML from `docs/` on GitHub Pages and paste URLs |

---

## Visual / motion layer (added in v1.0.2 pass)

| Component | File | Behaviour |
|---|---|---|
| Cosmic gradient background | `bg_cosmic_gradient.xml` + `themes.xml` | Vertical indigo gradient replaces flat navy |
| Animated star field | `NightSkyView.kt` | 60 stars in 3 depth layers, drift + twinkle, accelerometer parallax |
| Breathing moon | `MoonView.kt` | 4s scale pulse + counter-phase halo |
| UI sound effects | `SoundEffects.kt` | DSP-synthesized tap / chime / swoosh / reward |
| Haptic feedback | `HapticHelper.kt` | Predefined `EFFECT_TICK / CLICK / HEAVY_CLICK` |
| Timer progress ring | `CircularTimerRingView.kt` | Glowing cyan arc around the play button |
| Card animations | `SoundAdapter.kt` | Scale-on-press, active-card pulse, stagger entrance |
| Aurora premium card | `bg_premium_aurora.xml` | Gradient-backed upsell |

## Manual QA checklist (run on device after install)

- [ ] App launches without crashing
- [ ] Stars visible in background, gently drift; tilt the phone and watch them parallax-shift
- [ ] Moon at the top breathes (subtle scale pulse with a halo)
- [ ] Sound cards stagger-fade in on first load
- [ ] Tapping any card produces a tick haptic and a soft "tap" sound (if system touch sounds are on)
- [ ] Starting playback plays a "chime"; switching plays a "swoosh"
- [ ] Rewarded-ad reward plays the ascending arpeggio + heavier haptic
- [ ] Setting a timer animates the cyan ring sweeping down around the play button
- [ ] Branded splash (animated moon) shows on cold launch
- [ ] Status bar transparent — cosmic gradient extends edge-to-edge
- [ ] Sound grid shows section headers: NOISE / NATURE / MECHANICAL / SYNTHETIC
- [ ] Long-press any sound card → description popup
- [ ] Toolbar overflow → Settings → toggle haptics off → tap a card, no buzz
- [ ] Settings → toggle UI sounds off → no chime/tap audio
- [ ] Settings → Replay onboarding → restart app → onboarding shows again
- [ ] Lockscreen notification shows MediaStyle layout with Stop button
- [ ] System language ES/DE/FR → strings translate (settings menu, dialogs)
- [ ] Onboarding dialog appears on first launch only
- [ ] 3 free sounds (White / Pink / Brown) play and loop without gaps
- [ ] Tap a premium sound → dialog offers 4 paths (ad / one-time / monthly / yearly)
- [ ] "Watch ad" option appears only if a rewarded ad is loaded (gives it ~5s)
- [ ] After watching test ad, the selected premium sound plays
- [ ] Volume slider works
- [ ] Sleep timer counts down and stops audio at 0
- [ ] Lock the screen → audio continues
- [ ] Pull down notification → "Stop" button works
- [ ] Toolbar → About menu opens AboutActivity
- [ ] About → Privacy Policy / Terms / Contact buttons work
- [ ] About → Restore Purchases re-queries Google Play
- [ ] Adaptive banner shows for free users; hidden after purchase
- [ ] Stop the sound 3 times in a session → interstitial ad shows on the 3rd
- [ ] First-launch consent form appears (EU only — use VPN to test)
- [ ] After 3 sessions, native In-App Review modal pops up (not market://)
- [ ] Kill the app → audio continues (foreground service alive)
- [ ] Rotation: locked to portrait — no relaunch
