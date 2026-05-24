# SoundPad — Complete Build & Publish Guide

## What You Have
A production-ready Android app with:
- **14 sounds**: White / Pink / Brown / Blue / Violet noise + Rain / Ocean / Fire / Fan / Wind / Thunder + Spaceship / Womb / Crystal (original synthetics)
- **Freemium model**: 3 free sounds, 11 premium
- **In-app purchase** (`soundpad_pro` — $3.99 one-time)
- **Subscription** (`soundpad_ultimate` — $1.99/month)
- **AdMob ads** for free users (auto-hidden after purchase)
- **Background playback** via foreground service + wake lock
- **Sleep timer** up to 8 hours

---

## STEP 1 — Install Android Studio

1. Download from https://developer.android.com/studio
2. Install, launch, and complete the initial setup wizard (it installs the Android SDK automatically)

---

## STEP 2 — Open the Project

1. In Android Studio: **File → Open**
2. Navigate to the `SoundPad/` folder you received and click **OK**
3. When asked to trust the project, click **Trust Project**
4. Wait for Gradle sync to finish (first time downloads ~500 MB)
   - If prompted to upgrade Gradle, click **OK**
   - If sync fails, go to **File → Invalidate Caches → Invalidate and Restart**

---

## STEP 3 — Test on Your Physical Phone

### Enable Developer Mode on your phone:
1. **Settings → About Phone → Build Number** — tap 7 times
2. **Settings → Developer Options** — enable **USB Debugging**
3. Connect phone to PC via USB cable
4. Allow USB debugging when prompted on phone

### Run the app:
1. In Android Studio, select your device from the device dropdown (top toolbar)
2. Click the **▶ Run** button (green triangle) or press **Shift+F10**
3. The app installs and launches on your phone in ~30 seconds

### Test checklist:
- [ ] All 3 free sounds play and loop without gaps
- [ ] Tap a premium sound → upgrade dialog appears
- [ ] Volume slider works
- [ ] Timer counts down and stops audio
- [ ] Lock screen → audio continues playing (wake lock working)
- [ ] Pull down notification → "Stop" button works
- [ ] Kill the app → audio continues (foreground service working)

---

## STEP 4 — Set Up Google Play Console

1. Go to https://play.google.com/console
2. Pay the **one-time $25 developer registration fee**
3. Complete your developer profile
4. Click **Create app**
   - App name: **SoundPad — Sleep & Focus Noise**
   - Language: English
   - App / Game: **App**
   - Free / Paid: **Free** (freemium model)

---

## STEP 5 — Set Up In-App Products

In Google Play Console → your app → **Monetize → In-app products**:

### One-time purchase:
| Field | Value |
|---|---|
| Product ID | `soundpad_pro` |
| Name | SoundPad Pro |
| Description | Unlock all 14 sounds and remove ads forever |
| Price | $3.99 |
| Status | Active |

### Subscription:
Go to **Monetize → Subscriptions → Create subscription**:
| Field | Value |
|---|---|
| Product ID | `soundpad_ultimate` |
| Name | SoundPad Ultimate |
| Description | All Pro features + sound mixer and scheduling |
| Base plan price | $1.99 / month |
| Free trial | 3 days (recommended) |

> **Important:** Products must be **Active** in the console before billing works in your app.

---

## STEP 6 — Set Up AdMob

1. Go to https://admob.google.com → sign in with Google
2. **Apps → Add App → Android**
   - App name: **SoundPad**
   - Published: **No** (for now)
3. Copy your **AdMob App ID** (format: `ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX`)
4. In `AndroidManifest.xml`, replace the test App ID:
   ```xml
   android:value="ca-app-pub-XXXXXXXXXXXXXXXX~XXXXXXXXXX"
   ```
5. In `activity_main.xml`, replace the test banner unit ID:
   ```xml
   ads:adUnitId="ca-app-pub-XXXXXXXXXXXXXXXX/XXXXXXXXXX"
   ```

> Test IDs currently in the code are Google's official test IDs — safe for development, **must** be replaced before publishing.

---

## STEP 7 — Generate a Release APK / AAB

### Create a signing keystore (do this ONCE — keep it safe forever):
1. In Android Studio: **Build → Generate Signed Bundle / APK**
2. Choose **Android App Bundle** (required for Play Store)
3. Click **Create new...**
   - Key store path: Save to a safe folder (e.g. `~/Documents/soundpad-release.jks`)
   - Password: choose a strong password — **write it down**
   - Key alias: `soundpad-key`
   - Validity: `25` (years)
   - Fill in your name/org
4. Click **OK**, then **Next**, then choose **release** build variant
5. Click **Finish** — the `.aab` file is generated in `app/release/`

### Configure signing in build.gradle (for future automated builds):
In `app/build.gradle`, inside `android {}`:
```groovy
signingConfigs {
    release {
        storeFile file("/path/to/soundpad-release.jks")
        storePassword "your-password"
        keyAlias "soundpad-key"
        keyPassword "your-password"
    }
}
buildTypes {
    release {
        signingConfig signingConfigs.release
        // ... rest of config
    }
}
```

---

## STEP 8 — Complete Store Listing

In Google Play Console → **Grow → Store presence → Main store listing**:

### App details:
```
Short description (80 chars):
White noise, pink noise, brown noise & sleep sounds. Fall asleep faster.

Full description:
SoundPad is the ultimate sleep & focus multi-tool. Scientifically-designed
noise colors and nature sounds mask distractions, help you fall asleep
faster, and improve focus during work or study.

🔊 14 SOUNDS INCLUDED
• White Noise — masks all distractions equally
• Pink Noise — warm, balanced, the most popular sleep sound
• Brown Noise — deep bass rumble, loved for ADHD focus
• Blue Noise — crisp masking for tinnitus relief
• Violet Noise — ultra-bright for extreme masking
• Gentle Rain — soft rainfall on leaves
• Ocean Waves — rolling beach waves
• Campfire — crackling fire and warm rumble
• Forest Wind — breeze through the treetops
• Thunder Roll — distant storm with rain
• Box Fan — classic bedroom fan hum
• Spaceship — deep space ambient drone (original)
• Womb Sounds — heartbeat + whoosh for babies (original)
• Crystal Bowls — Tibetan singing bowl meditation (original)

⏱ SLEEP TIMER — Set it and forget it. Audio fades after 30 min, 1 hour,
or up to 8 hours.

🎚 VOLUME CONTROL — Fine-tune to the perfect masking level.

🔋 PLAYS IN BACKGROUND — Screen off, phone locked — SoundPad keeps playing.

✨ SOUNDPAD PRO — One-time $3.99 unlocks all sounds and removes ads forever.
```

### Screenshots (minimum 2, recommended 8):
- Take screenshots on your phone while the app is running
- Required sizes: phone screenshots (1080×1920 or similar)
- In Android Studio: press the camera icon in the emulator/device panel

### App icon:
- Size: **512×512 PNG** with no transparency
- Create one at https://www.canva.com (search "app icon", use moon theme)
- Or use Android Studio's Image Asset Studio: **File → New → Image Asset**

---

## STEP 9 — Upload & Submit

1. Google Play Console → **Release → Production → Create new release**
2. Upload your `.aab` file from `app/release/`
3. Write release notes:
   ```
   🌙 Initial release
   • White, Pink, and Brown noise FREE
   • 11 additional premium sounds
   • Sleep timer up to 8 hours
   • Plays with screen off
   ```
4. Click **Review release**, then **Start rollout to Production**
5. Google reviews the app — usually **3–7 days** for the first submission

---

## STEP 10 — After Launch Checklist

| Task | Why |
|---|---|
| Reply to every review | Boosts ranking algorithm |
| Monitor Crashlytics | Add Firebase for crash reports |
| A/B test your price | Try $2.99 vs $4.99 for Pro |
| Add iOS version (React Native) | 2× addressable market |
| Create TikTok content | "Brown noise for ADHD" — huge organic traffic |
| Target keywords in title | "sleep sounds", "white noise", "brown noise ADHD" |

---

## Monetization Projections (realistic)

| Metric | Conservative | Optimistic |
|---|---|---|
| Downloads / month | 500 | 5,000 |
| Free → Pro conversion | 3% | 8% |
| Pro purchases / month | 15 | 400 |
| Revenue / month | ~$45 | ~$1,200 |
| Ad revenue (CPM ~$1) | ~$3 | ~$30 |

**Keys to the high end**: ASO keywords, TikTok organic ("brown noise ADHD" = millions of views), and getting featured by Google Play.

---

## Troubleshooting

| Problem | Fix |
|---|---|
| Gradle sync fails | File → Invalidate Caches |
| `INSTALL_FAILED_USER_RESTRICTED` | Enable USB debugging in Developer Options |
| Billing returns `BILLING_UNAVAILABLE` | Only works on real device with Play Store |
| Audio skips after a few hours | Wake lock is set — check battery optimization settings |
| App crashes on launch | Check Logcat in Android Studio for the error |
| AdMob shows no ads | Normal for first 24h — ads fill rate builds over time |

---

## File Structure Reference

```
SoundPad/
├── app/src/main/java/com/soundpad/sleep/
│   ├── SoundType.kt        — Enum of all 14 sounds with metadata
│   ├── AudioEngine.kt      — DSP audio synthesis (the core)
│   ├── SoundService.kt     — Foreground service, audio focus, wake lock
│   ├── BillingManager.kt   — Google Play Billing 6.x wrapper
│   ├── PrefsManager.kt     — SharedPreferences helper
│   ├── SoundAdapter.kt     — RecyclerView adapter for sound grid
│   └── MainActivity.kt     — UI controller
├── app/src/main/res/
│   ├── layout/
│   │   ├── activity_main.xml      — Main screen layout
│   │   └── item_sound_card.xml   — Sound card layout
│   ├── values/
│   │   ├── colors.xml    — Dark theme palette
│   │   ├── strings.xml   — All user-facing strings
│   │   └── themes.xml    — Material dark theme
│   └── drawable/
│       └── ic_notification.xml   — Moon notification icon
└── PUBLISHING_GUIDE.md
```

---

*Good luck — the sleep sounds market is real and growing. "Brown noise ADHD" alone gets millions of searches monthly. 🌙*
