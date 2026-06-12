#!/usr/bin/env bash
# Captures Play Store screenshots on the CI emulator (Pixel 6, 1080x2400).
#
# The app's 60fps Canvas animation saturates the UI thread on the software-GPU
# emulator, which (a) stops uiautomator ever reaching idle and (b) causes
# instantaneous `input tap` events to be dropped. So we use HELD presses
# (a 140ms swipe-in-place) and retry activity-opening taps until focus changes.
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

adb shell settings put global sysui_demo_allowed 1
assert_demo() {
  adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
  adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0900 >/dev/null
  adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null
  adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
  adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi -e fully true -e level 4 >/dev/null
}

# Held press — survives a busy UI thread where an instantaneous tap is dropped.
press() { adb shell input swipe "$1" "$2" "$1" "$2" 140; sleep 2; }
back()  { adb shell input keyevent KEYCODE_BACK; sleep 2; }
shot()  { assert_demo; adb exec-out screencap -p > "$OUT/$1.png"; echo "captured $1"; }
focused() { adb shell dumpsys window 2>/dev/null | grep -E "mCurrentFocus|mFocusedApp" | grep -q "$1"; }

# Press a coordinate, retrying until the target activity gains focus.
press_until() {
  local x=$1 y=$2 activity=$3
  for i in 1 2 3 4 5; do
    press "$x" "$y"
    sleep 2
    focused "$activity" && return 0
  done
  echo "WARN: $activity not focused after presses at $x,$y"
  return 0
}

# Force-stop + relaunch to a fresh top-of-home, past the splash.
fresh_home() {
  adb shell am force-stop "$PKG"
  sleep 1
  adb shell am start -n "$LAUNCH_COMPONENT"
  for i in $(seq 1 20); do
    sleep 2
    focused "MainActivity" && break
  done
  sleep 4
}

# ── Drive the app ────────────────────────────────────────────────────────────

# 1. Home idle — the splash runs a Canvas animation that fades for ~2.5s after
# MainActivity gains focus, so give it extra settle time before the first shot.
fresh_home
sleep 6
shot 01_home_idle

# 2. Home playing — press the PLAY LAST button (always visible, near the bottom)
fresh_home
press 540 2150
sleep 4
shot 02_home_playing

# 3. Sounds tab — bottom-nav "Sounds" (leftmost) to show the library/grid
fresh_home
press 108 2315
sleep 3
shot 03_sound_grid

# 4. Breathing coach — Breathe tool card (retry until BreathingActivity focuses)
fresh_home
press_until 300 1240 "BreathingActivity"
sleep 6
shot 04_breathing
back

# 5. Spirit — bottom-nav Spirit tab (retry until AiChatActivity focuses)
fresh_home
press_until 540 2315 "AiChatActivity"
sleep 4
shot 05_spirit
back

# 6. Progress dialog — overflow (3-dot) → My Progress (2nd item)
fresh_home
press 1020 60
sleep 1
press 850 270
sleep 2
shot 06_progress

ls -la "$OUT/"
