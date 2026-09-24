# StepUp 사운드 자산 목록

2026-09-25 제작. **효과음 26개 + 선택형 배경 루프 3개 = 원본 사운드 29개**다. 모든 파일은 직접 합성한 원본이며 외부 샘플이나 음원을 사용하지 않았다. 앱 코드·기존 `res/raw` 파일·재생 설정은 건드리지 않았다.

깊은 남색/푸른 발광/은색 금속의 화면에 맞춰 짧은 둥근 타격, 맑은 전자음, 얇은 공기감으로 음색을 통일했다. 공통 행동에는 같은 소리를 재사용한다. 모든 버튼·카드·페이지에 별도 소리를 붙이지 않아 조작음이 과해지지 않도록 했다.

| 구분 | 파일 | 쓰는 순간 |
|---|---|---|
| 공통 | [ui_tap.wav](sfx/ui_tap.wav) | 가벼운 버튼 터치 |
| 공통 | [ui_select.wav](sfx/ui_select.wav) | 하단 탭·선택 확정 |
| 공통 | [ui_back.wav](sfx/ui_back.wav) | 뒤로 가기 |
| 공통 | [ui_toggle.wav](sfx/ui_toggle.wav) | 설정 토글 전환 |
| 공통 | [ui_sheet_open.wav](sfx/ui_sheet_open.wav) | 시트·상세 패널 열기 |
| 공통 | [ui_sheet_close.wav](sfx/ui_sheet_close.wav) | 시트·상세 패널 닫기 |
| 공통 | [ui_background_switch.wav](sfx/ui_background_switch.wav) | 홈 배경 좌우 전환 |
| 꾸미기 | [item_equip.wav](sfx/item_equip.wav) | 의상·신발 착용 확정 |
| 결과 | [action_success.wav](sfx/action_success.wav) | 저장·게시·참여 성공 |
| 결과 | [action_error.wav](sfx/action_error.wav) | 일반 오류·거절 |
| 결과 | [reward_claim.wav](sfx/reward_claim.wav) | 보상 지급이 확정된 순간 |
| 러닝 | [run_countdown.wav](sfx/run_countdown.wav) | 출발 카운트다운의 한 박자. 남은 숫자마다 재생 가능 |
| 러닝 | [run_start.wav](sfx/run_start.wav) | 실제 러닝 시작 |
| 러닝 | [run_pause.wav](sfx/run_pause.wav) | 일시정지 |
| 러닝 | [run_resume.wav](sfx/run_resume.wav) | 일시정지 후 재개 |
| 러닝 | [run_lap.wav](sfx/run_lap.wav) | 랩·구간 완료 |
| 러닝 | [run_finish.wav](sfx/run_finish.wav) | 러닝 결과 확정 |
| 뽑기 | [draw_enter.wav](sfx/draw_enter.wav) | 미스터리 박스 화면 진입 |
| 뽑기 | [draw_charge.wav](sfx/draw_charge.wav) | 거래 확정 후 상자 충전 연출 |
| 뽑기 | [draw_box_open.wav](sfx/draw_box_open.wav) | 상자 열림 연출 |
| 뽑기 | [draw_reveal_common.wav](sfx/draw_reveal_common.wav) | 확정된 일반 결과 공개 |
| 뽑기 | [draw_reveal_rare.wav](sfx/draw_reveal_rare.wav) | 확정된 희귀 결과 공개 |
| 뽑기 | [draw_reveal_epic.wav](sfx/draw_reveal_epic.wav) | 확정된 에픽 결과 공개 |
| 뽑기 | [draw_reveal_legendary.wav](sfx/draw_reveal_legendary.wav) | 확정된 전설 결과 공개 |
| 뽑기 | [draw_cancel.wav](sfx/draw_cancel.wav) | 지갑 서명·거래 취소 |
| 뽑기 | [draw_fail.wav](sfx/draw_fail.wav) | 뽑기 거래 실패 |
| 배경 루프(선택) | [ambience_night_river.wav](ambience/ambience_night_river.wav) | 밤 강변. 홈·러닝·프로필의 밤 장면에 공유 |
| 배경 루프(선택) | [ambience_dawn_river.wav](ambience/ambience_dawn_river.wav) | 새벽·노을 강변 장면에 공유 |
| 배경 루프(선택) | [ambience_wardrobe_terrace.wav](ambience/ambience_wardrobe_terrace.wav) | 옷장 테라스 장면 |

뽑기 결과음은 트랜잭션 영수증으로 결과가 확정된 **뒤에만** 사용한다. 지갑을 열거나 승인을 기다리는 동안 `reward_claim` 또는 희귀도 공개음을 재생하면 실제 지급으로 오해할 수 있다. 배경 루프는 짧은 효과음과 달리 상시 재생이므로 기본 자동 재생 대상으로 보지 않는다. 도입 시 사용자의 소리 설정·무음 모드·다른 음악 재생을 존중하고, 앱이 배경으로 가면 정지해야 한다.

자산 규격은 **44.1 kHz / 모노 / 16-bit PCM WAV**다. 효과음은 0.105~1.82초, 루프는 8초다. 효과음 피크는 -13.2~-9.4 dBFS, 루프는 약 -20.4 dBFS로 출력했다. 버튼의 실제 재생 음량은 나중에 앱 연결 시 조절한다. `manifest.json`에 각 파일의 길이와 피크가 있다.

빠른 확인용 묶음: [대표 효과음 듣기](preview_highlights.mp3) · [효과음 전체 듣기](preview_all.mp3) · [배경 루프 듣기](preview_ambience.mp3). 묶음 파일은 미리듣기용이며 앱 이벤트 하나에 그대로 넣지 않는다. `generate_sfx.py`로 동일한 원본을 재생성할 수 있다.
