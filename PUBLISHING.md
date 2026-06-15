# Power of Mind — Publishing checklist

Everything needed to ship: web, APK/AAB, and Google Play. Work top to bottom.

## ⛔ 0. Fix the crash first
The app currently crashes on launch on the test device. **Do not publish until that's
fixed.** Run `crashlog.bat` (or `adb logcat -d AndroidRuntime:E *:S`) right after it
crashes and send the `FATAL EXCEPTION` block. Publishing a crashing build will get the
listing rejected or your account flagged.

## 1. Pre-release code changes (one-time, before the release build)
These don't affect the debug build you're testing; do them when ready to publish.

1. **Unique applicationId.** Currently `com.auroramind.meditation` (inherited). For a new
   listing use your own, e.g. in `app/build.gradle`:
   `applicationId "com.powerofmind.app"`
   (Then update the launch line in `START.bat` to `com.powerofmind.app.debug/...SplashActivity`.)
   *Say the word and I'll do this.*
2. **AdMob.** The release build uses the old app's AdMob IDs (`app/build.gradle`, release
   block). Create a new AdMob app + ad units and swap them in, or remove ads entirely.
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
8. **Monetize → Subscriptions** → create `pom_annual` ($39.99/yr) and `pom_monthly` ($9.99/mo).
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
