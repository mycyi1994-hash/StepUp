#!/usr/bin/env bash
set -uo pipefail
mkdir -p qa-captures
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
status=0
# AGP otherwise uninstalls the app and deletes its screenshots before adb can pull them.
./gradlew :app:connectedDebugAndroidTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true --stacktrace || status=$?
adb pull /sdcard/Android/data/com.giwa.strideup/files/experience-qa/. qa-captures/ || status=1
adb logcat -d -s AndroidRuntime > qa-captures/android-crashes.txt || true
captures=$(find qa-captures -maxdepth 1 -name '*.png' | wc -l)
if [ "$captures" -lt 112 ]; then
  echo "Expected 112 Android screenshots, found $captures"
  status=1
fi
exit "$status"
