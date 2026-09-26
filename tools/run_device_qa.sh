#!/usr/bin/env bash
# PR 기기 검사 — 리디자인 이후에는 tools/run_screen_gallery.sh 의 묶음을 그대로 쓴다.
#
# 예전 이 스크립트는 기기 테스트 전체를 한 에뮬레이터에서 연달아 돌리고 스크린샷 455장을
# 요구했다. 리디자인은 테스트를 묶음별로 나눠 돌린다 — 권한 검사는 권한을 거둔 뒤 따로,
# 전체 화면 모음은 여섯 조각으로(Screen Gallery Capture 워크플로). 한데 섞어 돌리면 앞 테스트가
# 허용한 권한이 남아 권한 검사가 깨지고, 화면 모음은 한 번에 끝까지 못 간다.
#
# 여기서는 PR 마다 기능 회귀(interaction) · 권한(permissions)과 S2 디자인 기준 장면을 돌린다.
# 전체 다국어 화면 모음 · 큰 글자 전체는 Screen Gallery Capture 워크플로에서 돈다.
set -uo pipefail
adb shell settings put system system_locales ko-KR || true
status=0
bash tools/run_screen_gallery.sh interaction || status=1
bash tools/run_screen_gallery.sh permissions || status=1
# S2 리디자인: 화면마다 데이터를 채워 찍는 디자인 기준 장면(기본 · 상세/설정 · 기록 · 탐색)을
# PR 마다 같은 리비전으로 찍는다. 뽑기 · 마켓 · 지갑 · 번개 상세처럼 위 묶음이 찍지 않는 화면이 여기서 나온다.
bash tools/run_screen_gallery.sh redesign || status=1
bash tools/run_screen_gallery.sh secondary || status=1
bash tools/run_screen_gallery.sh records || status=1
bash tools/run_screen_gallery.sh explore || status=1
exit "$status"
