# Play Console — Copy-Paste Reference

Every field Google will ask you to fill, pre-written. Don't think, paste.

---

## Create app

| Field | Value |
|---|---|
| App name | SoundPad — Sleep & Focus Noise |
| Default language | English – en-US |
| App or game | App |
| Free or paid | Free |
| Declarations | ✓ Developer Program Policies · ✓ US export laws |

---

## App content → tasks

### Privacy policy
```
https://soundpad-sleep.github.io/privacy.html
```

### App access
> All functionality is available without any restrictions.

### Ads
> Yes, my app contains ads.

### Content rating
Run the questionnaire — pick these:
- Violence: None
- Sexual content: None
- Profanity: None
- Controlled substances: None
- Gambling: None
- User communication: None
- Shares user location: No
- Personal info collection: No (Advertising ID is not "personal info" under IARC)

Expected rating: **Everyone**

### Target audience
- Target age group: **18 and over** (simplest — no COPPA paperwork)
- Appeals to children: **No**
- Ads from ad networks: **Yes**

### News app
> No, my app is not a news app.

### COVID-19 contact tracing
> My app is not publicly available, OR does not relate to COVID-19.

### Data safety  (see dedicated section below)

### Government app
> No

### Financial features
> No

### Health
> No (we have a disclaimer that we're NOT a medical device — keep this No)

---

## Data safety form — exact answers

**Does your app collect or share any of the required user data types?**
> Yes

### Data collected — add these three:

#### 1. Advertising ID (under "Device or other IDs")
- Collected: **Yes**
- Shared: **No** (AdMob processes it for us, doesn't count as sharing)
- Optional or required: **Required**
- Purposes: **Advertising or marketing** (non-personalized)
- Encrypted in transit: **Yes**
- Users can request deletion: **No** (resettable in Android settings)

#### 2. App interactions  (under "App activity")
- Collected: **Yes** (crash reports)
- Shared: **No**
- Optional or required: **Required**
- Purposes: **App functionality, Analytics**
- Encrypted in transit: **Yes**
- Users can request deletion: **No** (cleared by uninstalling)

#### 3. Purchase history  (under "Financial info")
- Collected: **Yes**
- Shared: **No** (Google Play Billing handles it)
- Optional or required: **Required**
- Purposes: **App functionality** (unlock premium)
- Encrypted in transit: **Yes**
- Users can request deletion: **No** (request refund via Play instead)

**Security practices:**
- ✓ Data is encrypted in transit
- ✗ (Don't claim independent security review unless you've actually had one)

---

## Main store listing

### App name
```
SoundPad — Sleep & Focus Noise
```

### Short description (80 chars max — this is 79)
```
White noise, brown noise, rain, ocean & sleep sounds. Fall asleep faster.
```

### Full description (paste verbatim)
```
SoundPad is your sleep and focus multi-tool. Scientifically-tuned noise colors and immersive nature sounds mask distractions, calm racing minds, and help you fall asleep faster — or focus deeper during work and study.

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

Made for restless sleepers, focused workers, anxious minds, and tired parents. 🌙
```

### Category
- App category: **Health & Fitness**
- Tags (add up to 5): Sleep, Meditation, Relaxation, Focus, White Noise

### Contact details
- Email: `soundpad.sleep@gmail.com`  *(create this Gmail if you haven't)*
- Phone: leave blank
- Website: `https://soundpad-sleep.github.io`

### External marketing
- Allow Google to use your app to promote in ads: **Yes** (free reach)

---

## Graphic assets

| Asset | Spec | File in repo |
|---|---|---|
| App icon | 512×512 PNG, no alpha | `marketing/icon_512.png` |
| Feature graphic | 1024×500 PNG | `marketing/feature_graphic.png` |
| Phone screenshots | 1080×1920+ PNG, 2–8 | Take on your Galaxy: hold Power+VolDown |

---

## In-app products  (Monetize → Products → In-app products)

### One-time
| Field | Value |
|---|---|
| Product ID | `soundpad_pro` |
| Name | SoundPad Pro |
| Description | Unlock all 14 sounds and remove ads forever |
| Default price | $3.99 USD |
| Status | **Active** |

## Subscriptions  (Monetize → Subscriptions)

### Monthly
| Field | Value |
|---|---|
| Product ID | `soundpad_ultimate` |
| Name | SoundPad Ultimate (Monthly) |
| Benefit | "Unlock all 14 premium sounds + remove ads, billed monthly" |
| Base plan ID | `monthly-autorenew` |
| Renewal type | Auto-renewing |
| Billing period | 1 month |
| Default price | $1.99 USD |
| Free trial (offer) | 3 days |
| Status | **Active** |

### Yearly
| Field | Value |
|---|---|
| Product ID | `soundpad_yearly` |
| Name | SoundPad Ultimate (Yearly) |
| Benefit | "All Ultimate features. Best value — save 37%." |
| Base plan ID | `yearly-autorenew` |
| Renewal type | Auto-renewing |
| Billing period | 1 year |
| Default price | $14.99 USD |
| Free trial (offer) | 7 days |
| Status | **Active** |

> Both subscription product IDs and base plan IDs must match these exactly,
> or the in-app purchase flow won't find them.

---

## Release notes (Production → Create new release)

### Release name
```
1.0.1 — Launch
```

### What's new in this release (≤500 chars)
```
🌙 Welcome to SoundPad — your new bedtime ritual.

• 14 sleep & focus sounds: white, pink, brown, blue, violet noise + rain, ocean, fire, wind, thunder, fan + 3 original synthetics
• Sleep timer up to 8 hours
• Background playback — screen off, app closed, still playing
• Optional rewarded ads to try premium sounds free
• One-time $3.99 or subscription with free trial
```

---

## Pricing & distribution  (countries)

- **Pick: all countries available.** No reason to restrict — Play handles tax & currency.
- Marketing: Allow Play to promote outside Google Search & Discover: **Yes**

---

## App signing

- Use **Play App Signing** (default, enabled when you create the first release).
- Your local keystore signs the upload key. Google holds the app signing key. This is the modern standard.

---

## After approval — first-week growth playbook

| Day | Action |
|---|---|
| 0 (live) | Reply "thanks!" to every install/review. Post your first TikTok: "Brown noise for ADHD focus — try it" with a 15s loop of the app playing |
| +1 | Post Reddit: r/sleep, r/ADHD, r/InsomniaHelp — "made a free white noise app, would love feedback" |
| +3 | Add a second TikTok: "I tested pink vs brown noise for 30 days" |
| +7 | Enable a Play Store experiment: A/B test "$2.99" vs "$3.99" for soundpad_pro |
| +14 | First weekly retention review in Play Console → Statistics |
| +30 | If installs > 1k: add interstitial-on-stop for free users (3× ad revenue) |
