#!/usr/bin/env bash
# 테스트 APK 스스로 업데이트를 에뮬레이터에서 처음부터 끝까지 — 옛 main 빌드(qa)를 깔고, 진짜 test-apk 릴리스로 올라가는지 본다.
# (2026-10-23 사용자 "자동업데이트가 안 되는 거 같아 확인해봐" · "업데이트 떠도 '앱이 설치되지 않았습니다'")
#   A. 브라우저로 받아 깐 앱(설치한 앱 = Chrome): 앱 창의 "Update" → (안드로이드 확인 화면) → 새 빌드
#   B. 앱이 스스로 깐 뒤(설치한 앱 = StepUp): 다시 옛 빌드로 → "Update" → 새 빌드
#   C. 앱이 꺼져 있을 때 일꾼: 옛 빌드 → 일꾼을 바로 돌려 → 새 빌드(또는 "새 버전이 있어요" 알림)
# 화면마다 보이는 글을 로그에 그대로 적는다 — 안드로이드 · Play 프로텍트가 무엇을 띄웠는지 아티팩트 없이 읽게.
# Play 프로텍트 창이 뜨면 '앱 검사(Scan app)' 쪽을 누른다 — 사람이 고를 만한 쪽.
# 인자: 옛 qa APK 경로, 이름표. 결과는 self-update-e2e/<이름표>/.
set -uo pipefail
OLD_APK=${1:?old qa apk}
LABEL=${2:-run}
PKG=com.stepup.android
OUT=self-update-e2e/$LABEL
mkdir -p "$OUT"
REL=https://github.com/mycyi1994-hash/StepUp/releases/download/test-apk
[ -f self-update-e2e/release.apk ] || curl -sSL -o self-update-e2e/release.apk "$REL/StepUp-test.apk"
RELEASE_SHA=$(sha256sum self-update-e2e/release.apk | cut -d' ' -f1)
log() { echo "[$LABEL] $*" | tee -a "$OUT/summary.txt"; }
log "release sha256=$RELEASE_SHA · android $(adb shell getprop ro.build.version.release | tr -d '\r') · $(adb shell pm list packages | grep -cE 'com.android.vending|com.android.chrome') of vending/chrome"

info() { adb shell dumpsys package "$PKG" | grep -E "versionCode=|lastUpdateTime=|installerPackageName=|initiatingPackageName=|updateOwnerPackageName=" | sed 's/^ *//' | sort -u | tr '\n' ' '; }
installed_sha() { local p; p=$(adb shell pm path "$PKG" 2>/dev/null | head -1 | sed 's/^package://' | tr -d '\r'); [ -n "$p" ] || { echo none; return; }; adb pull "$p" "$OUT/installed.apk" >/dev/null 2>&1; sha256sum "$OUT/installed.apk" | cut -d' ' -f1; }
updated() { [ "$(installed_sha)" = "$RELEASE_SHA" ]; }
shot() { adb exec-out screencap -p > "$OUT/$1.png" 2>/dev/null; adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$OUT/$1.xml" >/dev/null 2>&1; }
# 화면에 보이는 글(앞쪽 몇 개)과 맨 앞 앱
texts() { python3 - "$1" <<'PY'
import re, sys
try: xml = open(sys.argv[1], encoding='utf-8', errors='ignore').read()
except Exception: print('(no dump)'); sys.exit()
pk = re.findall(r'package="([^"]+)"', xml)
t = [x for x in re.findall(r'text="([^"]+)"', xml) if x.strip()]
print((pk[0] if pk else '?') + ' | ' + ' / '.join(t[:14])[:400])
PY
}
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
install_old() { # $1 = installer package (빈 값 = adb)
  adb uninstall "$PKG" >/dev/null 2>&1
  if [ -n "$1" ]; then
    adb push "$OLD_APK" /data/local/tmp/old.apk >/dev/null
    adb shell pm install -r -i "$1" /data/local/tmp/old.apk | tail -1
  else
    adb install -r "$OLD_APK" | tail -1
  fi
  adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS 2>/dev/null
  adb shell pm grant "$PKG" android.permission.ACTIVITY_RECOGNITION 2>/dev/null
  adb shell appops set "$PKG" REQUEST_INSTALL_PACKAGES allow
}
reinstall_old() { adb push "$OLD_APK" /data/local/tmp/old.apk >/dev/null; adb shell pm install -r -d -i "$1" /data/local/tmp/old.apk | tail -1; adb shell appops set "$PKG" REQUEST_INSTALL_PACKAGES allow; }
logs() { # 설치 · 일꾼 · 우리 앱의 줄만
  adb logcat -d | grep -E "PackageInstaller|PackageManager|InstallStart|InstallInstalling|InstallFailed|PackageVerifier|Finsky|PlayProtect|Verifier|WM-|TestUpdate|stepup|AndroidRuntime" \
    | grep -viE "chatty|ResourcesManager" | tail -${1:-40} | sed "s/^/[$LABEL]   log: /"
}

# 앱을 켜고 새 버전 창의 "Update" 를 누르고, 그 뒤 뜨는 안드로이드 · Play 프로텍트 화면의 진행 단추를 누르며 새 빌드가 깔릴 때까지
foreground_case() {
  local tag=$1 pressed=0 last=""
  adb logcat -c
  adb shell am start -n "$PKG/.MainActivity" >/dev/null
  for i in $(seq 1 45); do
    sleep 3
    if updated; then break; fi
    shot "$tag-$i"
    local now; now=$(texts "$OUT/$tag-$i.xml")
    [ "$now" != "$last" ] && log "$tag screen $i: $now" && last=$now
    if [ $pressed = 0 ] && grep -q 'text="New test build"' "$OUT/$tag-$i.xml"; then
      tap_text "$OUT/$tag-$i.xml" "Update" && pressed=1 && log "$tag: pressed Update in StepUp"
      continue
    fi
    if ! grep -q "package=\"$PKG\"" "$OUT/$tag-$i.xml"; then
      # 안드로이드 설치 화면 · Play 프로텍트 — 설치를 이어 가는 단추를 누른다(보고 · 취소 단추는 누르지 않는다)
      tap_text "$OUT/$tag-$i.xml" "Update" "Install" "Install anyway" "Install without scanning" "Scan app" "Send" "OK" "Continue" "Done" >/dev/null 2>&1 && log "$tag: tapped through ($now)"
    fi
  done
  if updated; then log "$tag: UPDATED · $(info)"; logs 15; return 0; fi
  log "$tag: NOT UPDATED (pressed=$pressed) · $(info)"
  logs 60
  return 1
}

status=0
BROWSER=com.android.chrome
adb shell pm list packages | grep -q "package:$BROWSER" || BROWSER=""
log "== A: installed from the browser (${BROWSER:-adb})"
install_old "$BROWSER"
log "A before · $(info)"
foreground_case A || status=1

log "== B: installer = the app — old build again"
reinstall_old "$PKG"
log "B before · $(info)"
foreground_case B || status=1

log "== C: background worker (app closed)"
reinstall_old "$PKG"
adb logcat -c
adb shell am start -n "$PKG/.MainActivity" >/dev/null
sleep 10
adb shell input keyevent KEYCODE_HOME
sleep 3
jobs=$(adb shell dumpsys jobscheduler | grep -E "JOB #u[0-9a-z]+/[0-9]+.*$PKG" | grep -oE "/[0-9]+:" | tr -d '/:' | sort -u | tr '\n' ' ')
log "C jobs: $jobs"
for id in $jobs; do adb shell cmd jobscheduler run -f "$PKG" "$id" >/dev/null 2>&1; done
for i in $(seq 1 40); do sleep 3; if updated; then break; fi; done
if updated; then log "C: UPDATED in background · $(info)"; else
  log "C: NOT UPDATED · notifications: $(adb shell dumpsys notification --noredact | grep -A15 "pkg=$PKG" | grep -oE "android.(title|text)=[^)]*" | tr '\n' ' ')"
  logs 60; status=1
fi
log "status=$status"
exit 0
