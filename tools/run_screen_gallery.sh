#!/usr/bin/env bash
set -uo pipefail
status=0
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.stepup.android.ScreenGalleryTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true --stacktrace || status=$?
mkdir -p screen-gallery
adb pull /sdcard/Android/data/com.stepup.android/files/screen-gallery/. screen-gallery/ || status=1
adb logcat -d -s ScreenGallery AndroidRuntime > screen-gallery/capture-log.txt || true
exit "$status"
