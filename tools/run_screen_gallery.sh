#!/usr/bin/env bash
set -uo pipefail
status=0
mkdir -p screen-gallery/chrome-reports screen-gallery/chrome-results screen-gallery/chrome
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.stepup.android.ChromeNavigationTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true --stacktrace || status=$?
cp -R app/build/reports/androidTests/. screen-gallery/chrome-reports/ || true
cp -R app/build/outputs/androidTest-results/. screen-gallery/chrome-results/ || true
adb pull /sdcard/Android/data/com.stepup.android/files/chrome-checks/. screen-gallery/chrome/ || status=1
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.stepup.android.ScreenGalleryTest -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true --stacktrace || status=$?
mkdir -p screen-gallery
adb pull /sdcard/Android/data/com.stepup.android/files/screen-gallery/. screen-gallery/ || status=1
adb logcat -d -s ScreenGallery AndroidRuntime > screen-gallery/capture-log.txt || true
exit "$status"
