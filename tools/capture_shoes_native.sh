#!/usr/bin/env bash
set -uo pipefail
group="$1"
tests="$2"
mkdir -p "shoes-captures/$group"
out="shoes-captures/$group"
git rev-parse HEAD > "$out/revision.txt"
git rev-parse HEAD:app/src/main > "$out/app-main-tree.txt"
adb shell wm size 1080x2340
adb shell wm density 420
adb shell settings put system system_locales ko-KR
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb install -r "$(find capture-apks -name app-debug.apk -print -quit)" || exit 1
adb install -r "$(find capture-apks -name app-debug-androidTest.apk -print -quit)" || exit 1
adb logcat -c
adb logcat -v threadtime > "$out/logcat.txt" 2>&1 &
logger=$!
pull_images() {
  for folder in my-shoes shoe-catalog shoe-detail-v1 shoe-care-blue shoe-upgrade shoe-draw-v3; do
    mkdir -p "$out/$folder"
    timeout 15s adb pull "/sdcard/Android/data/com.stepup.android/files/$folder/." "$out/$folder/" >/dev/null 2>&1 || true
  done
}
(while sleep 15; do pull_images; done) &
collector=$!
status=0
IFS=',' read -ra selections <<< "$tests"
for selection in "${selections[@]}"; do
  logfile="$out/${selection//#/_}.txt"
  timeout 240s adb shell am instrument -w -r -e class "com.stepup.android.$selection" com.stepup.android.test/androidx.test.runner.AndroidJUnitRunner > "$logfile" 2>&1
  result=$?
  cat "$logfile"
  if [ "$result" -ne 0 ] || ! grep -Eq '^OK \([0-9]+ tests?\)' "$logfile"; then
    status=1
    timeout 10s adb exec-out screencap -p > "$out/${selection//#/_}-failure.png" || true
  fi
  pull_images
  timeout 10s adb shell am force-stop com.stepup.android || true
done
kill "$collector" "$logger" 2>/dev/null || true
pull_images
echo "$status" > "$out/exit-status.txt"
exit "$status"
