# Power of Mind — Publishing checklist

Everything needed to ship: web, APK/AAB, and Google Play. Work top to bottom.

## ⛔ 0. Fix the crash first
The app reportedly crashed on launch on the test device. **Do not publish until that's
confirmed fixed.**

The build now **reports its own crashes** — no adb needed:
1. Install the new debug APK (rebuild from **Actions → "Android Build"**).
2. If it crashes, **just reopen the app**. The first screen now shows the exact stack
   trace with **Share** / **Copy** buttons — send me that and I'll fix the precise line.
3. (Optional fallback) `crashlog.bat` or `adb logcat -d AndroidRuntime:E *:S` still works.

This build also hardens the launch path: the splash tolerates a missing icon, UI sound
effects can never crash the process, and a global handler catches anything else.
Publishing a crashing build will get the listing rejected or your account flagged.

## 1. Pre-release code changes (one-time, before the release build)
These don't affect the debug build you're testing; do them when ready to publish.

1. **Unique applicationId.** ✅ Done — set to `com.powerofmind.app` in `app/build.gradle`
   (the debug build installs as `com.powerofmind.app.debug`). The launch lines in
   `START.bat`, `start.bat`, `INSTALL_TO_PHONE.bat`, and `diagnose.bat` were updated to
   match. Note the code `namespace` deliberately stays `com.auroramind.meditation`, so the
   launch activity is still `com.auroramind.meditation.SplashActivity` — that's normal,
   applicationId (store identity) and namespace (code package) are independent.
2. **AdMob.** ✅ Done — the release build uses the Power of Mind AdMob app
   `ca-app-pub-7584543130600454~8331616858` with its banner + interstitial units.
   Ads show to free users only (banner on the dashboard, interstitial on every 2nd
   finished session); subscribers are ad-free. No rewarded unit yet — add one if wanted.
3. **Support email / website** in the privacy + terms pages are `support@photon-bounce.com`
   — change if needed.

## 2. Build the signed AAB (for Play)
1. GitHub repo → **Settings → Secrets and variables → Actions** → add 4 secrets:
   - `POM_KEYSTORE_BASE64` = contents of `powerofmind-upload.jks.b64` (sent in chat)
   - `POM_STORE_PASSWORD` = the keystore password I sent you in chat (keep it in a password manager — it is intentionally not written in this repo)
   - `POM_KEY_PASSWORD` = same value as `POM_STORE_PASSWORD`
   - `POM_KEY_ALIAS` = `powerofmind`
2. **Actions → "Release AAB (signed)" → Run workflow.**
3. Download `app-release.aab` from the run's Artifacts. This is what you upload to Play.

## 3. Debug APK (testing only — not for Play)
**Actions → "Android Build" → Run workflow** → download `app-debug.apk` from Artifacts.
Or build locally with `START.bat`.

## 4. Google Play Console
1. Create app → **Power of Mind**, Health & Fitness, Free.
2. Keep **Play App Signing ON** (default) — your `.jks` is then the *upload* key (resettable).
3. **Internal testing track** → upload `app-release.aab` → add yourself as a tester first.
4. **Main store listing** → fill from `play-store/LISTING.md`; upload `play-store/icon_512.png`,
   `play-store/feature_graphic_1024x500.png`, and `play-store/screenshots/play_1..5.png`.
5. **Privacy policy** → `http://photon-bounce.com/pom/privacy.html`
6. **Content rating** questionnaire → likely Teen (supportive references to quitting).
7. **Data safety** form → per `play-store/LISTING.md`.
8. **Monetize → Subscriptions** → create `pom_annual` at **$1.99/year** (annual only —
   monthly was dropped). The free tier is ad-supported with the "I Am Free" theme; the
   subscription removes ads and unlocks all 7 affirmation themes.
9. Promote internal → closed → production when happy.

## 5. Web (marketing site + legal)
Already live at **http://photon-bounce.com/pom/** (privacy: `/pom/privacy.html`, terms:
`/pom/terms.html`). To redeploy after changes: **Actions → "Deploy Website" → Run workflow**
(enter the FTP password as the input). Source is in `microsite/`.

## Asset inventory (in repo)
- `play-store/` — icon, feature graphic, 5 phone screenshots, listing copy
- `branding/` — source logo + generated icons + feature graphic
- `screenshots/powerofmind/` — per-page app screenshots
- `microsite/` — the live marketing site (deployed to /pom)
