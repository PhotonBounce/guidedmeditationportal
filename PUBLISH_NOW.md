# SoundPad — Publish Now

Everything that can be automated is automated. The only things left are the
physical-world steps no software can do for you. Here they are, in order.

---

## 1. Install on your Galaxy and smoke-test  (one click, ~6 min first run)

Plug your phone in with USB Debugging enabled, then:

```
Double-click: SHIP_IT.bat
```

That's it. On first run it downloads JDK 17 + Android SDK into `.tools\` (no
admin, nothing touches your system), builds the APK, installs it on the
phone, and launches it. Later runs reuse the cache and finish in ~30 sec.

If you'd rather run the steps separately:
- `setup_environment.bat` — one-time, installs JDK + Android SDK portably
- `build_and_install.bat` — build APK + push to phone

Then run through `QA_REPORT.md` → "Manual QA checklist" (14 items, 5 min).

If anything's broken, tell me what — I fix it and we re-run.

---

## 2. Render marketing PNGs  (30 seconds, fully automatic)

```
Double-click: marketing\render_pngs.bat
```

Uses headless Edge (shipped with Windows 10/11 — no install needed) to render
5 perfect PNGs from the SVG/HTML templates already in `marketing/`:

```
marketing/icon_512.png             ← Play Store app icon
marketing/feature_graphic.png      ← Play Store feature graphic
marketing/screenshot_1_main.png    ← Phone screenshot 1
marketing/screenshot_2_premium.png ← Phone screenshot 2
marketing/screenshot_3_timer.png   ← Phone screenshot 3
```

If you'd rather use real device screenshots from your Galaxy, those go in
the same slots. The synthetic ones are good enough for v1 launch.

---

## 3. Host the privacy policy + terms  (90 seconds)

The `docs/` folder is already a complete GitHub Pages site.

1. Create a public GitHub repo named `soundpad-sleep`
2. Push this whole `D:\sleepapprepo` to it (`.gitignore` already excludes the AAB)
3. Repo → Settings → Pages → Source: **main / /docs** → Save
4. Wait 60 seconds. URLs are live:
   - https://YOUR_USERNAME.github.io/soundpad-sleep/privacy.html
   - https://YOUR_USERNAME.github.io/soundpad-sleep/terms.html

If your username isn't `soundpad-sleep`, edit two constants in
`app/src/main/java/com/soundpad/sleep/AboutActivity.kt` to match, then rebuild.

---

## 4. Get AdMob IDs  (15 minutes)

https://admob.google.com → Add App → Android → "not on store yet" → create
four ad units (App + Banner + Interstitial + Rewarded), copy each ID.

All four are now centralized in `app/build.gradle` — find this block and
paste your real IDs in:

```groovy
buildConfigField "String", "ADMOB_APP_ID",           "\"YOUR_APP_ID\""
buildConfigField "String", "ADMOB_BANNER_UNIT_ID",   "\"YOUR_BANNER\""
buildConfigField "String", "ADMOB_INTERSTITIAL_UNIT_ID", "\"YOUR_INTERSTITIAL\""
buildConfigField "String", "ADMOB_REWARDED_UNIT_ID", "\"YOUR_REWARDED\""
manifestPlaceholders.admobAppId = "YOUR_APP_ID"
```

Rebuild after editing — `SHIP_IT.bat` again, or `gradlew assembleDebug`.

---

## 5. Create signing keystore  (one time, 60 seconds)

```
Double-click: create_keystore.bat
```

Pick a strong password. **Back up the `.jks` file to Google Drive + USB stick.**
If you lose it, Google will never let you update the app again.

Then add to `%USERPROFILE%\.gradle\gradle.properties`:
```
SOUNDPAD_KEYSTORE=C:\Users\YOU\soundpad-release.jks
SOUNDPAD_KEY_ALIAS=soundpad-key
SOUNDPAD_STORE_PASSWORD=...
SOUNDPAD_KEY_PASSWORD=...
```

---

## 6. Build the upload artifact  (60 seconds)

```
Double-click: build_release_aab.bat
```

Output: `app\build\outputs\bundle\release\app-release.aab` ← this is what
Play Console accepts.

---

## 7. Play Console: create products + upload  (20 minutes)

Open `PLAY_CONSOLE_COPY.md` in one window, Play Console in another. Every
field is pre-written — paste, don't think.

Order inside Play Console:
1. Create app  (1 min)
2. App content → Privacy policy → paste the URL  (10 sec)
3. App content → Data safety → answer per the doc  (5 min)
4. App content → all other tasks  (5 min — Google rates the questionnaire)
5. Monetize → Products → create `soundpad_pro` ($3.99 in-app)  (2 min)
6. Monetize → Subscriptions → create `soundpad_ultimate` ($1.99/mo)  (2 min)
7. Monetize → Subscriptions → create `soundpad_yearly` ($14.99/yr)  (2 min)
8. Main store listing → paste short + full description, upload assets  (5 min)
9. Production → Create new release → upload `app-release.aab` → write notes → Submit  (3 min)

---

## 8. Wait 3–7 days for Google review

Day-0 marketing playbook (do these the moment you're approved):
- TikTok: "Brown noise for ADHD focus" + 15-second app demo, link in bio
- Reddit: post to r/sleep, r/ADHD, r/insomnia with a free-app-feedback frame
- Reply to every review in week 1 — this directly boosts ranking

Numbers to track in Play Console → Statistics:
- Day-1 retention should be 35%+ for sleep apps
- Free → paid conversion should be 1.5%+ in month 1, climbing to 4–6% by month 3
- Subscription-to-yearly mix should hit 30%+ within 60 days

---

## What's in this repo

```
app/                                  Android source
docs/                                 Hostable privacy site (GitHub Pages ready)
marketing/                            SVGs + HTML templates + render_pngs.bat
build_and_install.bat                 ← debug install on attached phone
build_release_aab.bat                 ← signed AAB for Play Console
create_keystore.bat                   ← run once to create your signing key
LAUNCH_CHECKLIST.md                   long-form launch guide
PLAY_CONSOLE_COPY.md                  every form field, pre-written
PRIVACY_POLICY.md / TERMS_OF_SERVICE.md  source markdown
QA_REPORT.md                          what I fixed + on-device test list
PUBLISH_NOW.md                        ← you are here
```

The only steps I can't do for you are the physical ones: plugging in your
phone, double-clicking the build scripts on your machine, owning the Google
account that submits the app, and pressing the "Submit" button. Everything
else is wired up.
