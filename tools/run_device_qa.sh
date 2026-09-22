#!/usr/bin/env bash
set -uo pipefail
mkdir -p qa-captures
adb shell settings put global window_animation_scale 1
adb shell settings put global transition_animation_scale 1
adb shell settings put global animator_duration_scale 1
status=0
./gradlew :app:connectedDebugAndroidTest --stacktrace || status=$?
adb pull /sdcard/Android/data/com.giwa.strideup/files/experience-qa/. qa-captures/ || true
adb logcat -d -s AndroidRuntime > qa-captures/android-crashes.txt || true
exit "$status"
