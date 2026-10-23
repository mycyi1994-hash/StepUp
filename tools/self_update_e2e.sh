#!/usr/bin/env bash
# 테스트 APK 스스로 업데이트를 에뮬레이터에서 처음부터 끝까지 — 옛 main 빌드(qa)를 깔고, 진짜 test-apk 릴리스로 올라가는지 본다.
# (2026-10-23 사용자 "자동업데이트가 안 되는 거 같아 확인해봐")
#   A. 밖에서 깐 앱(브라우저 · adb — 설치한 앱이 StepUp 이 아님): 앱 창의 "Update" → (안드로이드 확인 화면) → 새 빌드
#   B. 앱이 스스로 깐 뒤(설치한 앱 = StepUp): 다시 옛 빌드로 → "Update" 만으로 확인 없이 새 빌드
#   C. 앱이 꺼져 있을 때 일꾼: 옛 빌드 → 일꾼을 바로 돌려 → 새 빌드(또는 "새 버전이 있어요" 알림)
# 인자: 옛 qa APK 경로. 결과는 self-update-e2e/ (화면 · 화면 구조 · logcat · 요약).
set -uo pipefail
OLD_APK=${1:?old qa apk}
PKG=com.stepup.android
OUT=self-update-e2e
mkdir -p "$OUT"
REL=https://github.com/mycyi1994-hash/StepUp/releases/download/test-apk
curl -sSL -o "$OUT/release.apk" "$REL/StepUp-test.apk"
curl -sSL -o "$OUT/release.json" "$REL/StepUp-test.json"
RELEASE_SHA=$(sha256sum "$OUT/release.apk" | cut -d' ' -f1)
echo "release: $(cat "$OUT/release.json") sha256=$RELEASE_SHA" | tee "$OUT/summary.txt"

info() { adb shell dumpsys package "$PKG" | grep -E "versionCode=|lastUpdateTime=|installerPackageName=|installInitiatingPackageName=" | sed 's/^ *//' | sort -u; }
installed_sha() { local p; p=$(adb shell pm path "$PKG" | head -1 | sed 's/^package://' | tr -d '\r'); adb pull "$p" "$OUT/installed.apk" >/dev/null 2>&1; sha256sum "$OUT/installed.apk" | cut -d' ' -f1; }
shot() { adb exec-out screencap -p > "$OUT/$1.png" 2>/dev/null; adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/$1.xml" >/dev/null 2>&1; }
# 화면 구조에서 글자가 맞는 칸의 가운데를 누른다
tap_text() {
  local xml=$1; shift
  local xy
  xy=$(python3 - "$xml" "$@" <<'PY'
import re, sys
xml = open(sys.argv[1], encoding='utf-8', errors='ignore').read()
for want in sys.argv[2:]:
    for m in re.finditer(r'<node [^>]*?text="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', xml):
        if m.group(1).strip().lower() == want.lower():
            x1, y1, x2, y2 = map(int, m.group(2, 3, 4, 5))
            print((x1 + x2) // 2, (y1 + y2) // 2); sys.exit(0)
sys.exit(1)
PY
) || return 1
  adb shell input tap $xy
}
fresh_old() { # $1 = install args
  adb uninstall "$PKG" >/dev/null 2>&1
  adb install $1 "$OLD_APK" | tail -1
  adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null
  adb shell appops set "$PKG" REQUEST_INSTALL_PACKAGES allow
}
reinstall_old() { adb install -r -d $1 "$OLD_APK" | tail -1; adb shell appops set "$PKG" REQUEST_INSTALL_PACKAGES allow; }
updated() { [ "$(installed_sha)" = "$RELEASE_SHA" ]; }

# 앱을 켜고 새 버전 창의 "Update" 를 누른 뒤, 안드로이드 확인 화면이 나오면 그것도 누르고, 새 빌드가 깔릴 때까지 기다린다
foreground_case() {
  local tag=$1 confirms=0 pressed=0
  adb logcat -c
  adb shell am start -n "$PKG/.MainActivity" >/dev/null
  for i in $(seq 1 40); do
    sleep 3
    shot "$tag-$i"
    if updated; then break; fi
    if [ $pressed = 0 ] && grep -q 'text="New test build"' "$OUT/$tag-$i.xml"; then
      tap_text "$OUT/$tag-$i.xml" "Update" && pressed=1 && echo "$tag: pressed Update at ${i}" | tee -a "$OUT/summary.txt"
      continue
    fi
    # 안드로이드 설치 확인 화면(패키지 설치 프로그램)
    if grep -q 'package="com.google.android.packageinstaller"\|package="com.android.packageinstaller"' "$OUT/$tag-$i.xml"; then
      tap_text "$OUT/$tag-$i.xml" "Update" "UPDATE" "Install" "INSTALL" && confirms=$((confirms + 1)) && echo "$tag: android confirm at ${i}" | tee -a "$OUT/summary.txt"
    fi
  done
  adb logcat -d > "$OUT/$tag-logcat.txt"
  grep -iE "PackageInstaller|TestUpdate|stepup|PackageManager" "$OUT/$tag-logcat.txt" | grep -viE "chatty" | tail -80 > "$OUT/$tag-logcat-short.txt"
  info > "$OUT/$tag-after.txt"
  if updated; then echo "$tag: UPDATED (android confirms=$confirms, pressed=$pressed)" | tee -a "$OUT/summary.txt"; return 0; fi
  echo "$tag: NOT UPDATED (android confirms=$confirms, pressed=$pressed)" | tee -a "$OUT/summary.txt"
  return 1
}

status=0
echo "== A: installed from outside (adb)" | tee -a "$OUT/summary.txt"
fresh_old ""
info | tee "$OUT/A-before.txt"
foreground_case A || status=1
cat "$OUT/A-after.txt" | tee -a "$OUT/summary.txt"

echo "== B: after a self-update (installer = the app) — old build again" | tee -a "$OUT/summary.txt"
reinstall_old "-i $PKG"
info | tee "$OUT/B-before.txt" | tee -a "$OUT/summary.txt"
foreground_case B || status=1

echo "== C: background worker (app closed)" | tee -a "$OUT/summary.txt"
reinstall_old "-i $PKG"
adb logcat -c
adb shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 8
shot C-open
adb shell input keyevent KEYCODE_HOME
sleep 3
adb shell dumpsys jobscheduler | grep -E "JOB #.*$PKG" > "$OUT/C-jobs.txt"
for id in $(grep -oE "JOB #u[0-9a-z]+/[0-9]+" "$OUT/C-jobs.txt" | sed 's#.*/##'); do adb shell cmd jobscheduler run -f "$PKG" "$id" >/dev/null 2>&1; done
for i in $(seq 1 40); do sleep 3; if updated; then break; fi; done
adb logcat -d > "$OUT/C-logcat.txt"
adb shell dumpsys notification --noredact | grep -A12 "pkg=$PKG" | grep -E "android.title|android.text" > "$OUT/C-notifications.txt"
info > "$OUT/C-after.txt"
if updated; then echo "C: UPDATED in background" | tee -a "$OUT/summary.txt"; else echo "C: NOT UPDATED — notifications: $(tr '\n' ' ' < "$OUT/C-notifications.txt")" | tee -a "$OUT/summary.txt"; status=1; fi
shot C-end
echo "status=$status" | tee -a "$OUT/summary.txt"
exit 0
