#!/usr/bin/env bash
# 폰에 올리는 테스트 APK(qa — R8 · 디버그 불가)를 에뮬레이터에서 실제로 켜 본다.
#
# 기기 검사는 debug 빌드로 돈다. R8 이 줄인 qa 빌드만 깨지는 문제(지워진 클래스 · 직렬화 · 리플렉션)는
# 거기서 드러나지 않으므로, 기기 검사를 마친 뒤 qa APK 를 같은 서명으로 덮어 설치하고 켜서 이리저리 눌러 본다.
# 앱이 뜨지 않거나 R8 이 원인인 오류로 죽으면 실패한다. 결과와 프레임 통계는 screen-gallery/qa-apk-smoke/ 에 남긴다.
set -uo pipefail
apk="app/build/outputs/apk/qa/app-qa.apk"
pkg="com.stepup.android"
out="screen-gallery/qa-apk-smoke"
mkdir -p "$out"
if [[ ! -f "$apk" ]]; then
  echo "::error::$apk 가 없습니다 (assembleQa)"
  exit 1
fi
adb install -r "$apk" > "$out/install.txt" 2>&1 || { cat "$out/install.txt"; echo "::error::qa APK 설치 실패"; exit 1; }
adb shell dumpsys package "$pkg" | grep -E "flags=|versionName" > "$out/package.txt" || true
adb logcat -c || true
adb shell am force-stop "$pkg" || true
adb shell dumpsys gfxinfo "$pkg" reset > /dev/null 2>&1 || true
adb shell am start -W -n "$pkg/.MainActivity" > "$out/start.txt" 2>&1
cat "$out/start.txt"
sleep 8
if [[ -z "$(adb shell pidof "$pkg" | tr -d '\r')" ]]; then
  adb logcat -d -b crash > "$out/crash.txt" 2>&1 || true
  cat "$out/crash.txt"
  echo "::error::qa APK 가 켜지자마자 꺼졌습니다"
  exit 1
fi
adb exec-out screencap -p > "$out/launch.png" || true
# 같은 씨앗으로 매번 같은 순서 — 시스템 키 · 앱 전환은 빼고 앱 안에서만 누르고 민다
adb shell monkey -p "$pkg" -s 4242 --throttle 300 --pct-syskeys 0 --pct-appswitch 0 --pct-anyevent 0 \
  --ignore-security-exceptions -v 400 > "$out/monkey.txt" 2>&1 || true
tail -5 "$out/monkey.txt"
adb exec-out screencap -p > "$out/after-monkey.png" || true
adb shell dumpsys gfxinfo "$pkg" > "$out/gfxinfo.txt" 2>&1 || true
grep -E "Total frames|Janky frames|percentile" "$out/gfxinfo.txt" | head -8 || true
adb logcat -d -b crash > "$out/crash.txt" 2>&1 || true
adb logcat -d > "$out/logcat.txt" 2>&1 || true
# R8 이 원인일 때 나는 오류들 — 이것만 실패로 본다(몽키가 우연히 낸 다른 앱 오류는 기록만)
r8='ClassNotFoundException|NoClassDefFoundError|NoSuchMethodError|NoSuchMethodException|NoSuchFieldError|NoSuchFieldException|AbstractMethodError|IncompatibleClassChangeError|VerifyError|SerializationException|Serializer for class|InstantiationException'
if grep -q "Process: $pkg" "$out/crash.txt"; then
  echo "── qa APK 오류 기록 ──"
  sed -n '1,80p' "$out/crash.txt"
  if grep -Eq "$r8" "$out/crash.txt"; then
    echo "::error::qa APK(R8) 에서만 날 수 있는 오류로 앱이 꺼졌습니다 — screen-gallery/qa-apk-smoke/crash.txt"
    exit 1
  fi
  echo "::warning::qa APK 가 몽키 중 꺼졌습니다(R8 원인 아님) — screen-gallery/qa-apk-smoke/crash.txt"
fi
echo "qa APK smoke OK"
