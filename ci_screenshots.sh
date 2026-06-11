#!/usr/bin/env bash
# Captures Play Store screenshots on the CI emulator (Pixel 6, 1080x2400).
#
# The app's home/splash run a 60fps Canvas animation that never idles, so
# `uiautomator dump` (which waits for idle) times out and returns nothing.
# We therefore drive the UI with FIXED proportional coordinates instead of
# querying the view tree. Resolution is pinned by the pixel_6 profile.
set -x
OUT=store_screenshots
mkdir -p "$OUT"
PKG=com.auroramind.meditation.debug
LAUNCH_COMPONENT="$PKG/com.auroramind.meditation.SplashActivity"
PREFS_FILE=meditation_portal_prefs.xml

# Suppress system ANR / crash dialogs so they can't draw over the app.
adb shell settings put global hide_error_dialogs 1
sleep 25
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard || true

# -g pre-grants runtime permissions so no permission dialog appears.
adb install -r -g app/build/outputs/apk/debug/app-debug.apk

# ── Pre-seed prefs so onboarding never shows (app is debuggable) ─────────────
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
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null; }
demo enter
demo clock -e hhmm 0900
demo battery -e level 100 -e plugged false
demo notifications -e visible false
demo network -e wifi -e fully true -e level 4

tap()    { adb shell input tap "$1" "$2"; sleep 2; }
back()   { adb shell input keyevent KEYCODE_BACK; sleep 2; }
shot()   { adb exec-out screencap -p > "$OUT/$1.png"; echo "captured $1"; }
home_focused() { adb shell dumpsys window 2>/dev/null | grep -E "mCurrentFocus|mFocusedApp" | grep -q "MainActivity"; }

# Launch and wait until MainActivity (not the splash) holds focus.
launch_home() {
  adb shell am start -n "$LAUNCH_COMPONENT"
  for i in $(seq 1 20); do
    sleep 2
    home_focused && { sleep 2; return 0; }
  done
  echo "WARN: MainActivity not confirmed focused"
  adb shell dumpsys window 2>/dev/null | grep -E "mCurrentFocus|mFocusedApp" || true
  return 0
}

# Swipes (1080x2400 coordinate space)
scroll_top()  { for i in 1 2 3; do adb shell input swipe 540 700 540 2000 300; sleep 1; done; }
scroll_down() { adb shell input swipe 540 1800 540 700 400; sleep 2; }

# ── Drive the app ────────────────────────────────────────────────────────────
launch_home

# 1. Home idle (top of screen)
scroll_top
shot 01_home_idle

# 2. Sound grid — scroll down to reveal the meditation cards
scroll_down
scroll_down
shot 03_sound_grid

# 2b. Play a meditation, then show the playing home with scrub bar
tap 270 1180          # first card in the grid
sleep 5
scroll_top
shot 02_home_playing

# 4. Breathing coach — Breathe card (left tool card)
scroll_top
tap 310 1245
sleep 7
shot 04_breathing
back

# 5. Spirit — bottom-nav Spirit tab (center)
scroll_top
tap 540 2315
sleep 5
shot 05_spirit
back

# 6. Progress dialog — overflow menu (3-dot top-right) → My Progress (2nd item)
scroll_top
tap 1020 60
sleep 1
tap 850 270
sleep 2
shot 06_progress

ls -la "$OUT/"
