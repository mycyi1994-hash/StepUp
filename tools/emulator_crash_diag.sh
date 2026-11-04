#!/usr/bin/env bash
# 임시 진단(.github/workflows/emulator-crash-diag.yml) — 크루 명함 상세 검사 하나를 돌리며 기기 로그를 호스트에 남긴다.
set -uo pipefail
out="diag/${1:-run}"
mkdir -p "$out"
adb shell settings put system system_locales ko-KR || true
adb shell setprop debug.hwui.renderer skiagl || true
adb logcat -c || true
adb logcat -v threadtime > $out/device-live-logcat.txt 2>&1 &
logcat_pid=$!
ps -o pid,args -C qemu-system-x86_64 -C qemu-system-x86_64-headless > $out/emulator-process.txt 2>&1 || true
cat /proc/$(pgrep -f qemu-system | head -1)/limits > $out/emulator-limits.txt 2>&1 || true
result=0
timeout 12m ./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=${DIAG_TEST:-com.stepup.android.CrewCardsDesignTest#listDetailAndJoin} \
  --stacktrace > $out/gradle.txt 2>&1 || result=$?
echo "result=$result" | tee $out/result.txt
adb get-state > $out/emulator-state.txt 2>&1 || echo "emulator gone" >> $out/emulator-state.txt
kill "$logcat_pid" 2>/dev/null || true
cp -R app/build/outputs/androidTest-results "$out"/ 2>/dev/null || true
exit 0
