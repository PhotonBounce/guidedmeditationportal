#!/usr/bin/env bash
# Drives the app on the CI emulator and captures Play Store screenshots.
# Run by the android-emulator-runner step — adb is already connected.
set -x
OUT=store_screenshots
mkdir -p "$OUT"
# Debug builds append ".debug" to the applicationId; classes keep the base package.
PKG=com.auroramind.meditation.debug
LAUNCH_COMPONENT="com.auroramind.meditation.debug/com.auroramind.meditation.SplashActivity"

# Let the system settle after boot — launcher ANRs are common right after.
sleep 30
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard || true

# -g pre-grants all runtime permissions so no permission dialog can cover the app
adb install -r -g app/build/outputs/apk/debug/app-debug.apk

# ── helpers ──────────────────────────────────────────────────────────────────
# Finds a UI node whose text OR content-desc contains $1; prints "x y" of center.
# Matches against text, content-desc AND resource-id, case-insensitively.
find_node() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/ui.xml >/dev/null 2>&1
  python3 - "$1" <<'PYEOF'
import re, sys
q = sys.argv[1].lower()
try:
    xml = open('/tmp/ui.xml', encoding='utf-8', errors='replace').read()
except FileNotFoundError:
    sys.exit(1)
for m in re.finditer(r'<node[^>]*>', xml):
    n = m.group(0)
    t = re.search(r'text="([^"]*)"', n)
    d = re.search(r'content-desc="([^"]*)"', n)
    r = re.search(r'resource-id="([^"]*)"', n)
    hay = '|'.join([(t.group(1) if t else ''),
                    (d.group(1) if d else ''),
                    (r.group(1) if r else '')]).lower()
    if q in hay:
        b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', n)
        if b:
            print((int(b.group(1)) + int(b.group(3))) // 2,
                  (int(b.group(2)) + int(b.group(4))) // 2)
            sys.exit(0)
sys.exit(1)
PYEOF
}

# Dismisses an "isn't responding" ANR dialog — only acts if one is actually
# present, so it never false-taps the live UI.
dismiss_anr() {
  if [ -n "$(find_node "isn't responding")" ]; then
    coords=$(find_node "Wait")          # keep waiting for OUR app
    [ -z "$coords" ] && coords=$(find_node "Close app")
    [ -n "$coords" ] && adb shell input tap $coords && sleep 1
  fi
  return 0
}

# Waits for the onboarding dialog to appear (it renders a beat after MainActivity
# loads), then taps its button by resource-id until the dialog is gone. Polling
# for appearance first avoids the race where an early check sees no dialog and
# wrongly concludes there's nothing to dismiss.
dismiss_onboarding() {
  local seen=0
  for i in $(seq 1 30); do
    btn=$(find_node "onboardingOk")
    if [ -n "$btn" ]; then
      seen=1
      adb shell input tap $btn
      sleep 2
      continue
    fi
    # Button gone: done if we already dismissed it, else keep waiting for it.
    [ "$seen" = "1" ] && return 0
    sleep 2
  done
  return 0
}

# True when our app's window has focus.
app_focused() {
  adb shell dumpsys window 2>/dev/null | grep -E "mCurrentFocus|mFocusedApp" | grep -q "$PKG"
}

# Launches the app and blocks until it actually holds the foreground.
launch_app() {
  for attempt in 1 2 3; do
    adb shell am start -n "$LAUNCH_COMPONENT"
    for i in $(seq 1 15); do
      sleep 2
      dismiss_anr
      # A system permission dialog steals window focus from the app — accept it
      # so the focus check underneath can see the real foreground activity.
      if adb shell dumpsys window 2>/dev/null | grep mCurrentFocus | grep -q permissioncontroller; then
        tap_if_present "Allow"
        tap_if_present "While using the app"
      fi
      app_focused && return 0
    done
  done
  echo "FATAL: app never reached foreground"
  echo "--- window focus ---"
  adb shell dumpsys window 2>/dev/null | grep -E "mCurrentFocus|mFocusedApp" || true
  echo "--- recent crashes ---"
  adb logcat -d 2>/dev/null | grep -E "FATAL EXCEPTION|AndroidRuntime|Process .* has died" | tail -40 || true
  echo "--- full logcat tail for our app ---"
  adb logcat -d 2>/dev/null | grep "auroramind" | tail -60 || true
  adb exec-out screencap -p > "$OUT/debug_fail.png" || true
  return 1
}

# Taps the node matching $1, scrolling down up to 3 screens to find it.
tap() {
  for i in 1 2 3 4; do
    dismiss_anr
    coords=$(find_node "$1")
    if [ -n "$coords" ]; then
      adb shell input tap $coords
      sleep 2
      return 0
    fi
    [ $i -lt 4 ] && adb shell input swipe 540 1700 540 800 400 && sleep 1
  done
  echo "WARN: UI node '$1' not found"
  return 1
}

# Taps without scrolling (for dialogs/permission prompts that may not appear).
tap_if_present() {
  coords=$(find_node "$1")
  [ -n "$coords" ] && adb shell input tap $coords && sleep 2
  return 0
}

scroll_top() {
  for i in 1 2 3; do adb shell input swipe 540 800 540 1900 300; sleep 1; done
}

shot() {
  dismiss_anr
  if ! app_focused; then
    echo "WARN: app not focused before $1 — relaunching"
    launch_app
  fi
  adb exec-out screencap -p > "$OUT/$1.png"
  echo "captured $1"
}

# ── clean status bar via SystemUI demo mode ─────────────────────────────────
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0900
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi -e fully true -e level 4

# ── launch & first-run dialogs ───────────────────────────────────────────────
launch_app || exit 1
sleep 5
tap_if_present "Allow"            # POST_NOTIFICATIONS permission (API 33+)
dismiss_onboarding               # onboarding dialog (retries until gone)
sleep 2

# ── 1. Home idle ─────────────────────────────────────────────────────────────
scroll_top
shot 01_home_idle

# ── 2. Home playing (scrub bar visible) ──────────────────────────────────────
tap "Tonglen"
sleep 5
scroll_top
shot 02_home_playing

# ── 3. Sound grid ────────────────────────────────────────────────────────────
adb shell input swipe 540 1700 540 600 500
sleep 1
shot 03_sound_grid

# ── 4. Breathing coach ───────────────────────────────────────────────────────
scroll_top
tap "Breathe"
sleep 7
shot 04_breathing
adb shell input keyevent KEYCODE_BACK
sleep 2
dismiss_onboarding

# ── 5. Spirit chat ───────────────────────────────────────────────────────────
scroll_top
tap "Spirit"
sleep 4
shot 05_spirit
adb shell input keyevent KEYCODE_BACK
sleep 2

# ── 6. Progress dialog ───────────────────────────────────────────────────────
scroll_top
tap "More options"
sleep 1
tap "My Progress"
sleep 2
shot 06_progress

ls -la "$OUT/"
