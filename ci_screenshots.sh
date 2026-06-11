#!/usr/bin/env bash
# Captures Play Store screenshots on the CI emulator (Pixel 6, 1080x2400).
#
# The app runs a 60fps Canvas animation that never idles, so `uiautomator dump`
# times out — we drive the UI with FIXED coordinates instead. To return to a
# clean top-of-home we relaunch the activity (a downward swipe near the top
# would pull the notification shade, not scroll the content).
set -x
OUT=store_screenshots
mkdir -p "$OUT"
PKG=com.auroramind.meditation.debug
LAUNCH_COMPONENT="$PKG/com.auroramind.meditation.SplashActivity"
PREFS_FILE=meditation_portal_prefs.xml

adb shell settings put global hide_error_dialogs 1
sleep 25
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard || true
adb install -r -g app/build/outputs/apk/debug/app-debug.apk

# ── Pre-seed prefs so onboarding never shows ────────────────────────────────
adb shell am start -n "$LAUNCH_COMPONENT"
sleep 12
adb shell "run-as $PKG mkdir -p /data/data/$PKG/shared_prefs" || true
adb shell "run-as $PKG sh -c 'cat > /data/data/$PKG/shared_prefs/$PREFS_FILE'" <<'XML'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="onboarding_shown" value="true" />
</map>
XML
adb shell am force-stop "$PKG"
sleep 2

# ── clean status bar via SystemUI demo mode ─────────────────────────────────
adb shell settings put global sysui_demo_allowed 1
assert_demo() {
  adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
  adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0900 >/dev/null
  adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
  adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
  adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi -e fully true -e level 4 >/dev/null
}

tap()  { adb shell input tap "$1" "$2"; sleep 2; }
back() { adb shell input keyevent KEYCODE_BACK; sleep 2; }
shot() { assert_demo; adb exec-out screencap -p > "$OUT/$1.png"; echo "captured $1"; }
main_focused() { adb shell dumpsys window 2>/dev/null | grep -E "mCurrentFocus|mFocusedApp" | grep -q "MainActivity"; }

# Force-stop and relaunch to land on a fresh top-of-home, past the splash.
fresh_home() {
  adb shell am force-stop "$PKG"
  sleep 1
  adb shell am start -n "$LAUNCH_COMPONENT"
  for i in $(seq 1 20); do
    sleep 2
    main_focused && break
  done
  sleep 4   # let the home content settle after the splash transition
}

# Reveal lower content with UPWARD swipes only (never pulls the shade).
swipe_up() { adb shell input swipe 540 1700 540 700 400; sleep 2; }

# ── Drive the app ────────────────────────────────────────────────────────────

# 1. Home idle — fresh launch sits at the top
fresh_home
shot 01_home_idle

# 3. Sound grid — swipe up to reveal the meditation cards
fresh_home
swipe_up
swipe_up
shot 03_sound_grid

# 2. Home playing — play a meditation, capture the mini-player + scrub bar
fresh_home
swipe_up
swipe_up
tap 270 1150          # first meditation card in the grid
sleep 5
swipe_up              # nudge so the now-playing transport row is in view
shot 02_home_playing

# 4. Breathing coach — Breathe tool card (left)
fresh_home
tap 300 1240
sleep 7
shot 04_breathing
back

# 5. Spirit — bottom-nav Spirit tab (center of the 5-tab bar)
fresh_home
tap 540 2315
sleep 5
shot 05_spirit
back

# 6. Progress dialog — overflow (3-dot, top-right) → My Progress (2nd item)
fresh_home
tap 1020 60
sleep 1
tap 850 270
sleep 2
shot 06_progress

ls -la "$OUT/"
