#!/usr/bin/env bash
# Captures Play Store screenshots on a TABLET emulator.
#
# Same held-press technique as ci_screenshots.sh, but tap coordinates are
# scaled from the pixel_6 baseline (1080x2400) to the actual screen size so
# the one script drives both the 7" and 10" profiles.
#
# Usage: ci_tablet_screenshots.sh <output_dir>   e.g. store_screenshots_tablet7
set -x
OUT=${1:-store_screenshots_tablet}
mkdir -p "$OUT"
PKG=com.auroramind.meditation.debug
LAUNCH_COMPONENT="$PKG/com.auroramind.meditation.SplashActivity"
PREFS_FILE=meditation_portal_prefs.xml

adb shell settings put global hide_error_dialogs 1
sleep 25
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard || true
# Tablets boot landscape; lock the display to portrait so the portrait-locked
# app and our scaled tap coordinates line up.
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
sleep 2
adb install -r -g app/build/outputs/apk/debug/app-debug.apk

# Actual screen size → scale factors from the 1080x2400 baseline
SIZE=$(adb shell wm size | grep -oE '[0-9]+x[0-9]+' | tail -1)
SW=${SIZE%x*}; SH=${SIZE#*x}
sx() { echo $(( $1 * SW / 1080 )); }
sy() { echo $(( $1 * SH / 2400 )); }

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

press() { adb shell input swipe "$1" "$2" "$1" "$2" 140; sleep 2; }
back()  { adb shell input keyevent KEYCODE_BACK; sleep 2; }
shot()  { assert_demo; adb exec-out screencap -p > "$OUT/$1.png"; echo "captured $1"; }
focused() { adb shell dumpsys window 2>/dev/null | grep -E "mCurrentFocus|mFocusedApp" | grep -q "$1"; }

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

fresh_home() {
  adb shell am force-stop "$PKG"
  sleep 1
  adb shell am start -W -n "$LAUNCH_COMPONENT"
  for i in $(seq 1 20); do
    sleep 2
    focused "MainActivity" && break
  done
  # Surface any crash so CI logs explain a launcher-only screenshot.
  adb logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime.*$PKG" | tail -20 || true
  sleep 4
}

# ── Drive the app ────────────────────────────────────────────────────────────

# 1. Home idle
fresh_home
sleep 6
shot 01_home

# 2. Breathing coach
fresh_home
press_until "$(sx 300)" "$(sy 1240)" "BreathingActivity"
sleep 6
shot 02_breathing
back

# 3. Spirit chat
fresh_home
press_until "$(sx 540)" "$(sy 2315)" "AiChatActivity"
sleep 4
shot 03_spirit
back

ls -la "$OUT/"
