#!/usr/bin/env bash
set -uo pipefail
status=0
suite="${1:-all}"
case "$suite" in
  all|interaction|gallery|large-font|permissions|wardrobe|mystery|redesign|secondary|records|explore|community) ;;
  *) echo "Unknown capture suite: $suite" >&2; exit 2 ;;
esac
original_font_scale=""
# Persist evidence on the host while the emulator is alive: post-failure adb
# cannot recover the last scene or Android logs after the device disappears.
mkdir -p screen-gallery
adb logcat -v threadtime > screen-gallery/device-live-logcat.txt 2>&1 &
logcat_pid=$!
(
  while true; do
    date -u
    free -m
    ps -eo pid,ppid,rss,comm --sort=-rss | head -16
    sleep 10
  done
) > screen-gallery/host-memory.txt 2>&1 &
monitor_pid=$!
# Best-effort transport only; these files never substitute for final test reports.
# A failed emulator otherwise takes all earlier screenshots with it.
mkdir -p screen-gallery/partial-captures
(
  while true; do
    for capture_dir in chrome-checks login-checks form-checks screen-gallery experience-qa community-stories profile-running-path shoe-draw shoe-grade settings-v1 notifications-v1 running-records profile-edit-v1 wallet-v1; do
      destination="screen-gallery/partial-captures/$capture_dir"
      mkdir -p "$destination"
      timeout 10s adb pull "/sdcard/Android/data/com.stepup.android/files/$capture_dir/." "$destination/" || true
    done
    sleep 20
  done
) > screen-gallery/partial-transfer-log.txt 2>&1 &
capture_transfer_pid=$!
collect_diagnostics() {
  if [[ "$original_font_scale" == "null" ]]; then
    timeout 10s adb shell settings delete system font_scale || true
  elif [[ "$original_font_scale" =~ ^[0-9]+([.][0-9]+)?$ ]]; then
    timeout 10s adb shell settings put system font_scale "$original_font_scale" || true
  fi
  kill "$logcat_pid" "$monitor_pid" "$capture_transfer_pid" 2>/dev/null || true
  wait "$logcat_pid" "$monitor_pid" "$capture_transfer_pid" 2>/dev/null || true
  sudo -n dmesg --ctime > screen-gallery/host-kernel.txt 2>&1 || true
  if [[ -d /tmp/android-runner ]]; then
    cp -R /tmp/android-runner screen-gallery/emulator-diagnostics || true
  fi
}
trap collect_diagnostics EXIT
# Record guest capacity as well as host capacity: available host RAM does not
# establish that the Android guest has enough room for its launcher and the app.
timeout 20s adb shell cat /proc/meminfo > screen-gallery/guest-memory-start.txt || true
timeout 20s adb shell dumpsys activity lastanr > screen-gallery/guest-anr-start.txt || true
# Exercise actual IME in form tests even when the emulator exposes a hardware keyboard.
adb shell settings put secure show_ime_with_hard_keyboard 1 || status=1
# adb can fail while enumerating screenshots even after instrumentation passed.
# Retry only artifact transport, never rerun or suppress a failed test.
pull_captures() {
  local source="$1" destination="$2"
  for attempt in 1 2 3; do
    if timeout 60s adb pull "$source" "$destination"; then return 0; fi
    echo "Capture transfer failed (attempt $attempt/3): $source" >&2
    sleep 2
  done
  return 1
}
# Leave time for partial screenshots/reports to upload before the workflow's 30-minute cap.
run_instrumentation() {
  local phase="$1" classes="$2" gallery_part="${3:-}" result=0
  local -a runner_args=("-Pandroid.testInstrumentationRunnerArguments.class=$classes")
  if [[ -n "$gallery_part" ]]; then
    runner_args+=("-Pandroid.testInstrumentationRunnerArguments.galleryPart=$gallery_part")
  fi
  # 부팅 직후 런처가 "응답 없음" 창을 띄우면 그 창이 초점을 가져가 키보드 · 뒤로 가기 검사가
  # 앱과 무관하게 깨진다(2026-09-26 PR #31). 시스템 오류 창을 숨기고, 떠 있는 창은 닫고 시작한다.
  timeout 10s adb shell settings put global hide_error_dialogs 1 || true
  timeout 10s adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null || true
  timeout --signal=TERM --kill-after=20s 9m ./gradlew :app:connectedDebugAndroidTest \
    "${runner_args[@]}" \
    -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true --stacktrace || result=$?
  printf '%s exit=%s\n' "$phase" "$result" >> screen-gallery/phase-results.txt
  if (( result != 0 )); then
    status=$result
    timeout 20s adb exec-out screencap -p > "screen-gallery/$phase-failure-display.png" || true
    timeout 20s adb logcat -d > "screen-gallery/$phase-failure-logcat.txt" || true
    timeout 20s adb shell dumpsys activity lastanr > "screen-gallery/$phase-last-anr.txt" || true
    timeout 20s adb shell dumpsys window > "screen-gallery/$phase-windows.txt" || true
    # A timed-out Gradle client can leave its instrumentation process running on the device.
    if (( result == 124 || result == 137 )); then
      timeout 20s adb shell am force-stop com.stepup.android.test || true
      timeout 20s adb shell am force-stop com.stepup.android || true
    fi
  fi
}
# Character-free checkpoint: bounded interactions and 14 reference scenes x 4 viewports.
# The full multilingual gallery remains a separately selectable test, not a default run.
if [[ "$suite" == "redesign" ]]; then
  run_instrumentation redesign-interaction "com.stepup.android.ExperienceUiTest#mainNavigationAndSettingsAreReachable,com.stepup.android.ExperienceUiTest#shoePreviewOnlyEquipsAfterConfirmation,com.stepup.android.ExperienceUiTest#shoeDrawRespectsReadinessAndTabs,com.stepup.android.ExperienceUiTest#firstGuideVisitsRunningShoesAndProfile,com.stepup.android.MysteryDesignTest"
  mkdir -p screen-gallery/redesign-interaction-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/redesign-interaction-results/ || true
  run_instrumentation redesign-reference "com.stepup.android.DesignReferenceTest#referenceViewports"
  mkdir -p screen-gallery/redesign-reference-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/redesign-reference-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/experience-qa/. screen-gallery/experience-qa/ || status=1
  pull_captures /sdcard/Android/data/com.stepup.android/files/screen-gallery/. screen-gallery/mystery/ || status=1
fi
# Secondary design checkpoint: real writing forms + 10 detail/settings routes at three viewports.
if [[ "$suite" == "secondary" ]]; then
  run_instrumentation secondary-forms "com.stepup.android.CrewFormTest"
  mkdir -p screen-gallery/secondary-forms-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/secondary-forms-results/ || true
  run_instrumentation secondary-reference "com.stepup.android.DesignReferenceTest#secondaryViewports"
  mkdir -p screen-gallery/secondary-reference-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/secondary-reference-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/experience-qa/. screen-gallery/experience-qa/ || status=1
  pull_captures /sdcard/Android/data/com.stepup.android/files/form-checks/. screen-gallery/forms/ || status=1
fi
if [[ "$suite" == "records" ]]; then
  run_instrumentation records-reference "com.stepup.android.DesignReferenceTest#recordViewports"
  mkdir -p screen-gallery/records-reference-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/records-reference-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/experience-qa/. screen-gallery/experience-qa/ || status=1
fi
if [[ "$suite" == "explore" ]]; then
  run_instrumentation explore-reference "com.stepup.android.DesignReferenceTest#exploreViewports"
  mkdir -p screen-gallery/explore-reference-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/explore-reference-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/experience-qa/. screen-gallery/experience-qa/ || status=1
fi
# 동네 이야기(목록형 커뮤니티) — 시안 31개 장면을 실제 화면으로. 위치 권한을 거둔 채 시작해
# 권한 안내 → 지역 직접 선택 경로(시안 26 · 27)를 실제로 탄다. 권한을 거두면 앱이 죽을 수 있어
# 테스트를 띄우기 전에 거둔다(permissions 묶음과 같은 방식).
if [[ "$suite" == "community" ]]; then
  timeout 60s adb install -r app/build/outputs/apk/debug/app-debug.apk || true
  timeout 20s adb shell am force-stop com.stepup.android || true
  for permission in ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION; do
    timeout 20s adb shell pm revoke com.stepup.android "android.permission.$permission" || true
  done
  run_instrumentation community "com.stepup.android.CommunityStoriesTest"
  mkdir -p screen-gallery/community-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/community-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/community-stories/. screen-gallery/community-stories/ || status=1
  # 내 정보 러닝 패스(2026-09-27) — 버튼마다 이동 · 카드 상태
  run_instrumentation profile "com.stepup.android.ProfileRunningPathTest"
  mkdir -p screen-gallery/profile-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/profile-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/profile-running-path/. screen-gallery/profile-running-path/ || status=1
  # 신발 뽑기 무료(2026-09-27) — 시안 세 장면 · 상태, 앱 셸 안에서 한 번 뽑기 → 결과 → 내 신발
  run_instrumentation draw "com.stepup.android.ShoeDrawTest"
  mkdir -p screen-gallery/draw-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/draw-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/shoe-draw/. screen-gallery/shoe-draw/ || status=1
  # 신발 등급 프레임 v8(2026-09-27) — 네 등급 무대 · 보유 칸 · 보관함 · 상세 · 밝은 테마 · 좁은 폭 · 큰 글씨 · 뽑기 결과
  run_instrumentation grade "com.stepup.android.ShoeGradeFrameTest"
  mkdir -p screen-gallery/grade-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/grade-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/shoe-grade/. screen-gallery/shoe-grade/ || status=1
  # 설정 v1(2026-09-28) — 시안 01~28 장면(앱 셸 안의 실제 값 + 기기에서 만들 수 없는 상태)
  run_instrumentation settings "com.stepup.android.SettingsDesignTest"
  mkdir -p screen-gallery/settings-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/settings-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/settings-v1/. screen-gallery/settings-v1/ || status=1
  # 알림·공지 v1(2026-09-28) — 시안 01~23 장면(앱 셸 안의 실제 알림함 + 기기에서 만들 수 없는 상태)
  run_instrumentation notifications "com.stepup.android.NotificationsDesignTest"
  mkdir -p screen-gallery/notifications-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/notifications-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/notifications-v1/. screen-gallery/notifications-v1/ || status=1
  # 내 러닝 기록(2026-09-28) — 시안 01~16 장면(앱 셸 안의 실제 기록 · 기간 · 통계 · 상세 · 경로 · 삭제 + 예시 자료 장면)
  run_instrumentation running-records "com.stepup.android.RecordsDesignTest"
  mkdir -p screen-gallery/running-records-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/running-records-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/running-records/. screen-gallery/running-records/ || status=1
  # 프로필 수정 v1(2026-09-28) — 시안 01~12 장면(앱 셸 안의 실제 편집 · 기본 이미지 · 나가기 · 저장 + 저장 중 · 실패 장면)
  run_instrumentation profile-edit "com.stepup.android.ProfileEditDesignTest"
  mkdir -p screen-gallery/profile-edit-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/profile-edit-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/profile-edit-v1/. screen-gallery/profile-edit-v1/ || status=1
  # 지갑 v1(2026-09-28) — 시안 01~16 장면(앱 셸 안의 실제 원장 · 거르개 · 상세 · 다음 쪽 + 예시 원장 · 기기에서 만들 수 없는 상태)
  run_instrumentation wallet "com.stepup.android.WalletDesignTest"
  mkdir -p screen-gallery/wallet-results
  cp -R app/build/outputs/androidTest-results/. screen-gallery/wallet-results/ || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/wallet-v1/. screen-gallery/wallet-v1/ || status=1
fi
if [[ "$suite" == "wardrobe" ]]; then
  run_instrumentation wardrobe "com.stepup.android.ScreenGalleryTest#wardrobeDesign"
  pull_captures /sdcard/Android/data/com.stepup.android/files/screen-gallery/. screen-gallery/ || status=1
fi
if [[ "$suite" == "mystery" ]]; then
  run_instrumentation mystery "com.stepup.android.MysteryDesignTest"
  pull_captures /sdcard/Android/data/com.stepup.android/files/screen-gallery/. screen-gallery/ || status=1
fi
if [[ "$suite" == "all" || "$suite" == "interaction" ]]; then
mkdir -p screen-gallery/chrome-reports screen-gallery/chrome-results screen-gallery/chrome
run_instrumentation interaction "com.stepup.android.RunSaveRecoveryTest,com.stepup.android.ChromeNavigationTest,com.stepup.android.EquipmentPersistenceTest,com.stepup.android.RunTotalsTest,com.stepup.android.LoginPresentationTest,com.stepup.android.EventClaimPersistenceTest,com.stepup.android.CrewFormTest,com.stepup.android.ShareCardRenderTest,com.stepup.android.RunStartFlowTest,com.stepup.android.S2SetupTest,com.stepup.android.InviteScreenTest,com.stepup.android.RunLocationStateTest,com.stepup.android.ItemFilterInteractionTest,com.stepup.android.NotificationPersistenceTest,com.stepup.android.NotificationNavigationTest,com.stepup.android.AvatarPhotoStoreTest,com.stepup.android.WalletLedgerTest,com.stepup.android.RouteMapFitTest,com.stepup.android.EnergyPurchaseTest,com.stepup.android.CourseQueuePersistenceTest,com.stepup.android.RunCheckpointPersistenceTest,com.stepup.android.RunCrashRecoveryTest,com.stepup.android.RunSettlementPersistenceTest,com.stepup.android.DatabaseMigrationTest"
cp -R app/build/reports/androidTests/. screen-gallery/chrome-reports/ || true
cp -R app/build/outputs/androidTest-results/. screen-gallery/chrome-results/ || true
pull_captures /sdcard/Android/data/com.stepup.android/files/chrome-checks/. screen-gallery/chrome/ || status=1
mkdir -p screen-gallery/login
pull_captures /sdcard/Android/data/com.stepup.android/files/login-checks/. screen-gallery/login/ || status=1
mkdir -p screen-gallery/forms
pull_captures /sdcard/Android/data/com.stepup.android/files/form-checks/. screen-gallery/forms/ || status=1
fi
if [[ "$suite" == "all" || "$suite" == "gallery" ]]; then
run_instrumentation gallery-edge "com.stepup.android.ScreenGalleryTest#edgeStates"
mkdir -p screen-gallery/gallery-edge-results
cp -R app/build/outputs/androidTest-results/. screen-gallery/gallery-edge-results/ || true
pull_captures /sdcard/Android/data/com.stepup.android/files/screen-gallery/. screen-gallery/ || status=1
for part in a b c d e f; do
  run_instrumentation "gallery-base-$part" "com.stepup.android.ScreenGalleryTest#allScreens" "$part"
  mkdir -p "screen-gallery/gallery-base-$part-results"
  cp -R app/build/outputs/androidTest-results/. "screen-gallery/gallery-base-$part-results/" || true
  pull_captures /sdcard/Android/data/com.stepup.android/files/screen-gallery/. screen-gallery/ || status=1
done
fi
# Real system font enlargement reaches separate Dialog windows, unlike a
# CompositionLocal override on only the parent screen. Preserve its own reports.
if [[ "$suite" == "all" || "$suite" == "large-font" ]]; then
original_font_scale="$(timeout 10s adb shell settings get system font_scale | tr -d '\r')"
if [[ "$original_font_scale" == "null" || "$original_font_scale" =~ ^[0-9]+([.][0-9]+)?$ ]]; then
  if timeout 10s adb shell settings put system font_scale 1.6; then
    run_instrumentation large-font "com.stepup.android.ItemFilterInteractionTest"
    mkdir -p screen-gallery/large-font-results screen-gallery/large-font-forms
    cp -R app/build/outputs/androidTest-results/. screen-gallery/large-font-results/ || true
    pull_captures /sdcard/Android/data/com.stepup.android/files/form-checks/. screen-gallery/large-font-forms/ || status=1
  else
    status=1
  fi
else
  echo "Cannot verify original system font scale; enlarged-font validation not run" >&2
  status=1
fi
fi
if [[ "$suite" == "all" || "$suite" == "permissions" ]]; then
  # Revocation can kill the target process. Prepare the emulator before launching
  # instrumentation, in its own suite; never revoke while a test is running.
  prepared=1
  timeout 60s adb install -r app/build/outputs/apk/debug/app-debug.apk || prepared=0
  timeout 20s adb shell am force-stop com.stepup.android || prepared=0
  for permission in ACTIVITY_RECOGNITION ACCESS_FINE_LOCATION ACCESS_COARSE_LOCATION POST_NOTIFICATIONS; do
    timeout 20s adb shell pm revoke com.stepup.android "android.permission.$permission" || prepared=0
  done
  if (( prepared == 1 )); then
    run_instrumentation permissions "com.stepup.android.PrivacyPermissionTest"
    mkdir -p screen-gallery/permissions-results screen-gallery/permissions-forms
    cp -R app/build/outputs/androidTest-results/. screen-gallery/permissions-results/ || true
    pull_captures /sdcard/Android/data/com.stepup.android/files/form-checks/. screen-gallery/permissions-forms/ || status=1
  else
    echo "Permission preparation failed; denied-state test not run" >&2
    status=1
  fi
fi
timeout 20s adb logcat -d -s ScreenGallery AndroidRuntime > screen-gallery/capture-log.txt || true
exit "$status"
