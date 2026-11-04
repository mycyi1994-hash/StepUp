#!/usr/bin/env bash
# 임시 진단(.github/workflows/emulator-crash-diag.yml) — 크루 명함 상세 검사 하나를 돌리며 기기 로그를 호스트에 남긴다.
set -uo pipefail
mkdir -p diag
adb shell settings put system system_locales ko-KR || true
adb shell setprop debug.hwui.renderer skiagl || true
adb logcat -c || true
adb logcat -v threadtime > diag/device-live-logcat.txt 2>&1 &
logcat_pid=$!
ps -o pid,args -C qemu-system-x86_64 -C qemu-system-x86_64-headless > diag/emulator-process.txt 2>&1 || true
cat /proc/$(pgrep -f qemu-system | head -1)/limits > diag/emulator-limits.txt 2>&1 || true
result=0
timeout 9m ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.stepup.android.CrewCardsDesignTest#listDetailAndJoin \
  --stacktrace > diag/gradle.txt 2>&1 || result=$?
echo "result=$result" | tee diag/result.txt
adb get-state > diag/emulator-state.txt 2>&1 || echo "emulator gone" >> diag/emulator-state.txt
kill "$logcat_pid" 2>/dev/null || true
cp -R app/build/outputs/androidTest-results diag/ 2>/dev/null || true
exit 0
