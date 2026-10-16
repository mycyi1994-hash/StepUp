#!/usr/bin/env bash
# 테스트 APK 서명 키 교체(키 회전)를 에뮬레이터에서 확인한다 — 실제 키 없이, 이 자리에서 만든 일회용 키로 같은 절차를 밟는다.
#
#   1) 옛 키(공개 debug 키)로 서명한 qa 앱이 깔린 위에 tools/sign_test_apk.sh 로 회전 서명한 APK 를 덮어 설치 → 되어야 한다
#      (지우지 않고 업데이트 — 폰의 기록 · 로그인이 그대로 남는다)
#   2) 그 위에 옛 키로만 서명한 APK 를 덮어 설치 → 막혀야 한다(공개된 옛 키로는 더 이상 업데이트를 만들 수 없다)
#
# run_qa_apk_smoke.sh 뒤에 돈다(qa APK 가 이미 깔려 있다). 결과는 screen-gallery/qa-apk-smoke/rotation.txt 에 남긴다.
set -uo pipefail
apk="app/build/outputs/apk/qa/app-qa.apk"
pkg="com.stepup.android"
out="screen-gallery/qa-apk-smoke"
mkdir -p "$out"
log="$out/rotation.txt"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT

if [[ ! -f "$apk" ]]; then
  echo "::error::$apk 가 없습니다 (assembleQa)"
  exit 1
fi

# 일회용 키 — 이 검사가 끝나면 지워진다
openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:P-256 -out "$work/key.pem" 2>/dev/null &&
  openssl pkcs8 -topk8 -nocrypt -in "$work/key.pem" -outform DER -out "$work/key.pk8" &&
  openssl req -new -x509 -key "$work/key.pem" -sha256 -days 1 -subj "/CN=StepUp rotation check" -out "$work/cert.pem" 2>/dev/null ||
  { echo "::error::일회용 키를 만들지 못했습니다"; exit 1; }
bash tools/sign_test_apk.sh "$apk" "$work/rotated.apk" "$work/key.pk8" "$work/cert.pem" > "$log" 2>&1 ||
  { cat "$log"; echo "::error::회전 서명에 실패했습니다"; exit 1; }

# 옛 키 앱이 깔린 상태에서 시작한다(앞 검사가 실패해 지워졌을 수도 있다)
adb install -r "$apk" >> "$log" 2>&1 || { cat "$log"; echo "::error::옛 키 qa APK 를 깔지 못했습니다"; exit 1; }
before=$(adb shell dumpsys package "$pkg" | grep -m1 firstInstallTime | tr -d '\r')

if ! adb install -r "$work/rotated.apk" >> "$log" 2>&1; then
  cat "$log"
  echo "::error::새 키로 회전 서명한 APK 가 옛 키 앱 위에 덮어 설치되지 않습니다 — 키를 바꾸면 폰의 테스트 앱을 지워야 합니다"
  exit 1
fi
after=$(adb shell dumpsys package "$pkg" | grep -m1 firstInstallTime | tr -d '\r')
if [[ "$before" != "$after" ]]; then
  echo "::error::회전 서명 APK 가 업데이트가 아니라 새로 설치됐습니다 ($before → $after)"
  exit 1
fi
adb shell dumpsys package "$pkg" | grep -iE "signatures|signing|pastSigning" >> "$log" 2>&1 || true

# 공개된 옛 키로만 서명한 APK 는 이제 막혀야 한다
if adb install -r "$apk" >> "$log" 2>&1; then
  cat "$log"
  echo "::error::새 키로 올라간 앱 위에 옛 키 APK 가 덮어 설치됩니다 — 옛 키의 되돌리기 신뢰가 남아 있습니다"
  exit 1
fi
if ! grep -q "INSTALL_FAILED_UPDATE_INCOMPATIBLE" "$log"; then
  cat "$log"
  echo "::error::옛 키 APK 가 다른 이유로 막혔습니다 — 서명 때문인지 확인하세요"
  exit 1
fi
echo "test APK rotation OK — 옛 키 앱 위에 새 키 APK 업데이트 · 옛 키 APK 는 막힘"
