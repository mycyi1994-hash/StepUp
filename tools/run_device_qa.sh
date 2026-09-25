#!/usr/bin/env bash
# PR 기기 검사 — 리디자인 이후에는 tools/run_screen_gallery.sh 의 묶음을 그대로 쓴다.
#
# 예전 이 스크립트는 기기 테스트 전체를 한 에뮬레이터에서 연달아 돌리고 스크린샷 455장을
# 요구했다. 리디자인은 테스트를 묶음별로 나눠 돌린다 — 권한 검사는 권한을 거둔 뒤 따로,
# 전체 화면 모음은 여섯 조각으로(Screen Gallery Capture 워크플로). 한데 섞어 돌리면 앞 테스트가
# 허용한 권한이 남아 권한 검사가 깨지고, 화면 모음은 한 번에 끝까지 못 간다.
#
# 여기서는 PR 마다 돌 만한 기능 회귀(interaction)와 권한(permissions) 묶음을 돌린다.
# 전체 화면 모음·큰 글자는 Screen Gallery Capture 워크플로에서 돈다.
set -uo pipefail
adb shell settings put system system_locales ko-KR || true
status=0
bash tools/run_screen_gallery.sh interaction || status=1
bash tools/run_screen_gallery.sh permissions || status=1
exit "$status"
