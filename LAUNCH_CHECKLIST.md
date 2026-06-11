# Guided Meditation Portal — v1.4.0 Deployment Checklist

Everything between "code is ready" and "live on Google Play."
Status legend: ✅ done · 🔲 your action needed

---

## 1. Build artifacts

- ✅ Code complete & pushed (PR #1, branch `claude/intelligent-gauss-oexa4d`)
- ✅ CI debug APK builds green (GitHub Actions → latest run → Artifacts)
- 🔲 **Merge PR #1 to main**
- 🔲 **Signed release AAB** — on your PC:
  ```
  cd D:\guidedmeditationapp
  git pull origin main
  build_release_aab.bat
  ```
  Output: `app\build\outputs\bundle\release\app-release.aab` (v1.4.0, code 11)

## 2. On-device QA (your phone — 10 minutes)

Install the debug APK first (`INSTALL_TO_PHONE.bat`), then verify:

- 🔲 Sound plays audibly (volume slider ≥ 70%)
- 🔲 **Scrub bar**: appears while playing, drag → audio jumps, time labels update
- 🔲 Background watermark logo is GONE (only top logo remains)
- 🔲 BGM dropdown shows new names (Soft Horizons, Still Waters, Dusk Veil…) — no "Untitled"
- 🔲 No track list shows two near-identical adjacent names
- 🔲 Focus Music + Energy Music sections present in library
- 🔲 Alarm screen: back button works, time-picker readable
- 🔲 Breathing coach, Spirit chat, Progress dialog all open & readable

## 3. Store screenshots (your phone — 15 minutes)

The old `qa_full/` shots show the removed watermark and lack the scrub bar — **retake these 6** on the v1.4.0 build with a clean status bar:

| # | Screen | State to capture |
|---|---|---|
| 1 | Home idle | Top logo, stats banner, daily quote visible |
| 2 | Home playing | Track playing — scrub bar + full transport visible |
| 3 | Sound grid | Guided Meditations section, monochrome cards |
| 4 | Breathing coach | Mid-inhale, glowing orb |
| 5 | Spirit chat | Welcome message + quick chips |
| 6 | Progress dialog | Streak, 7-day chart, community counter |

Save as 1080×2400 PNG → upload directly to Play Console (2–8 allowed).

## 4. Play Console assets (ready now)

- ✅ Feature graphic 1024×500: `marketing/feature_graphic.png` (v1.4.0 — regenerated)
- ✅ App icon 512×512: `app/src/main/appicon/play_store_icon_512.png`
- ✅ All listing text: `PLAY_CONSOLE_COPY.md` (updated for v1.4.0 — paste verbatim)
- ✅ Release notes EN/DE/FR/ES: in `PLAY_CONSOLE_COPY.md` → "What's new"

## 5. Play Console steps (one sitting, ~20 minutes)

1. 🔲 Upload `app-release.aab` → Production → Create new release
2. 🔲 Release name: `1.4.0 — New Genres, Scrub Bar & Track Names`
3. 🔲 Paste "What's new" (all 4 languages from PLAY_CONSOLE_COPY.md)
4. 🔲 Replace feature graphic + screenshots
5. 🔲 Update full description (from PLAY_CONSOLE_COPY.md — track count is now 55+)
6. 🔲 Verify in-app product `meditation_portal_unlock` is **Active at $2.00**
7. 🔲 Submit for review

## 6. Website

- 🔲 FTP `meditation-portal-site.zip` to photon-bounce.com (or run `ftp_deploy.py`)
- 🔲 Verify privacy/terms URLs resolve (Play requires them live):
  - https://www.photon-bounce.com/meditation-portal/privacy.html
  - https://www.photon-bounce.com/meditation-portal/terms.html

## 7. Known content debt (post-launch OK)

- 16 meditation mp3s contain a spoken "long pause" artifact (list in `_transcripts.json` audit) — re-record when convenient, same filenames in `app/src/main/res/raw/`, then bump versionCode and re-release.
