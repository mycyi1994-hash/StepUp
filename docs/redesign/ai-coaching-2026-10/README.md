# AI 코칭 — 구현 기록 (2026-10-16)

전달본: `StepUp-AI-coaching-handoff-16-screens-2026-10-16.zip`
(SHA-256 `86ab94916aaacdac6d263ef026f18129096aaf59a3711d77942cf92f18bdae99`) —
[시작 지시](source/START-HERE.md) · [구현 지시서](source/CLAUDE-HANDOFF.md) · [화면 목록](source/SCREEN-INDEX.md) · [시안 16장](source/mockup/).
시안은 원본 PNG(877×1792)를 600px JPG 로 줄여 넣었다(08 · 09 · 16 은 검수 수정본 `-v2`). 전달본의 `MANIFEST.json` ·
`PROMPTS-*.json` 은 넣지 않았다 — 재생성용 프롬프트이고, 작성자 PC 의 로컬 경로가 들어 있어 공개 저장소에 올리지 않는다.
기준 커밋 `5e1f4cd`. 시안은 픽셀 사양이 아니다 — 지시서의 동작 · 상태표와 기존 공용 부품이 이미지의 우연한 글자 · 간격보다 앞선다.

## 1단계 — 데이터 감사

지시서 5절의 선행 조건을 실제 코드로 확인한 결과다. "확인함"은 코드를 읽어 본 것이고, 기기 동작을 본 것은 아니다.

| 질문 | 코드에서 확인한 사실 | 정한 것 |
| --- | --- | --- |
| 로컬 기록 ID ↔ 서버 기록 ID | 로컬 `WalkSessionEntity.id`(Room 자동 번호)와 서버 `walk_sessions.id` 는 별개다. 서버 번호는 올리기에 성공하면 `ClaimRepository` 가 **`claimDay` 열**에 적는다(이름과 뜻이 다르고, 읽는 곳이 없다). 서버는 `(user_id, started_at)` 고유 색인으로 러닝 하나를 정하고, 뒤따르는 호출(`session_tag_crew` · 코스 기록)도 `p_started_at` 으로 러닝을 가리킨다. | 로컬 id 도 `claimDay` 도 서버로 보내지 않는다. 코칭 요청은 **시작 시각**만 보내고, 서버가 토큰의 사용자(`auth.uid()`)와 시작 시각으로 자기 표에서 러닝을 찾는다 — 남의 러닝은 찾을 수 없다(검증된 서버 조회 경로). |
| 종료 후 구간(랩) 보존 | `RunLap` 은 서비스 상태(`WalkSessionState.laps`)와 러닝 저장본(`RunCheckpointStore`)에만 있고, 저장이 끝나면 저장본과 함께 지워진다. `walk_sessions` 에 랩 열이 없다 — **지금까지 저장된 러닝에는 구간 기록이 없다.** | 정산 때 랩을 새 열 `walk_sessions.laps` 에 남긴다(DB 판 17 → 18). 그 전에 저장된 러닝은 구간 분석을 하지 않는다(09 "구간 정보 없음"). 없는 구간을 경로로 다시 만들지 않는다(아래 일시정지). |
| 랩이 1km 경계인가 | 자동 랩은 걸음 거리(걸음 × 0.762m — 기록의 거리와 같은 값)가 1km 를 넘을 때마다 `km = 순번` 으로 찍힌다. 수동 랩(`recordManualLap`, 아무 km)도 있지만 **화면 어디에서도 부르지 않는다.** 자동 랩은 랩 **개수**로 다음 경계를 셈하므로 수동 랩이 섞이면 경계가 빠진다. | 구간은 "모든 랩이 `km == 순번`(1, 2, 3…)이고 개수가 기록 거리의 정수 km 와 같을 때"만 쓴다. 수동 랩이 다시 켜져 섞이면 그 러닝은 구간 분석을 하지 않는다(틀린 구간보다 없는 구간). |
| 일시정지를 뺀 활성 시간 | 타이머는 일시정지 중 `activeMs` 를 올리지 않고, 걸음 수집기도 일시정지 중 걸음을 버린다. 저장되는 `durationSec` 과 랩의 `splitSec`(누적)은 모두 이 활성 시간이다. 앱이 죽었다 되살아난 동안은 시간 · 걸음 어디에도 들어가지 않는다. GPS 경로의 시각은 벽시계(epoch ms)라 일시정지 시간이 섞이고, 일시정지 구간은 어디에도 남지 않는다(서버도 0033 에서 60초 넘는 빈틈만 뺀다). | 구간 시간은 랩의 활성 시간 차로만 계산한다. 경로 시각으로 구간을 만들지 않는다. 마지막 부분 구간(예: 0.24km)은 `durationSec − 마지막 랩` 으로 따로 두고 1km 구간 비교에서 뺀다. |
| 거리의 정체 | 기록의 `distanceMeters` 는 GPS 가 아니라 **인정 걸음 × 0.762m** 다(VOID 면 0). GPS 거리(`gpsKm`)는 결과 화면용 상태에만 있고 로컬 기록에 저장되지 않는다. | 평균 페이스 = 저장된 활성 시간 ÷ 저장된 거리. 구간도 같은 걸음 거리 기준이라 합이 맞는다. GPS 가 끊겨도 거리 · 페이스는 있다 — 시안 09 처럼 "—" 로 지우는 것은 거리가 0 일 때만. |
| 계정별 소유권 | 기록 주인은 `recordingOwner`(`account:<uuid>` · `guest` · `legacy`)이고 녹화를 시작할 때 정해진다. 내 러닝 기록 화면의 질의는 `recordingOwner IN (:owner, 'legacy')` 로 **옛 기록을 지금 계정에 함께 보인다.** 서버 올리기는 `account:` 기록만 그 계정 토큰으로 한다(`ServerSessionRecorder`). | 코칭은 `recordingOwner = :owner` 만 읽는 질의를 따로 쓴다. `legacy` · `guest` 기록은 코칭에 쓰지 않고, 서버 요청은 `account:` 이면서 서버 확인(`SIGNED`)이 끝난 기록만. |
| 서버 판정 · 보상 | 서버 `record_session` 이 CLEAN · FLAGGED · VOID 와 적립을 정한다. 로컬도 같은 이름의 판정이 있고 VOID 는 걸음 · 거리 0 으로 저장된다. | 코칭은 판정 · 보상을 읽지도 바꾸지도 않는다. 분석 가능 여부는 별도의 품질 규칙(`CoachingQuality`)으로 — 거리 · 시간 · 구간이 있는가, 사람 범위의 값인가. |
| 기록 삭제 계약 | `RunRecordsRepository.delete` 는 이 휴대폰의 기록만 지운다. 서버 러닝 · 받은 SUP · 순위는 그대로다(서버에 삭제 계약이 없다). | 로컬 기록을 지우면 그 기록의 코칭 캐시도 지운다. 서버의 분석을 지웠다고 말하지 않는다. AI 분석 삭제는 화면 15 의 별도 선택(서버 RPC)이다. |
| 계정 삭제 | 서버 `account_delete()` 가 `auth.users` 를 지우고 표들이 `on delete cascade` 로 따라간다. | 코칭 표는 모두 `auth.users` · `walk_sessions` 에 `on delete cascade` 로 묶는다. |
| 정산 경계 | `RunSettlementRepository.settle` 이 러닝 저장 · 영수증을 한 트랜잭션으로 한다. 올리기는 `SessionUploadWorker` 가 따로. | 코칭은 정산 · 올리기 경로에 끼어들지 않는다. 정산이 저장하는 줄에 `laps` 글자 하나를 더할 뿐이고, 구간을 서버에 올리는 일은 사용자가 분석을 요청할 때 따로 한다 — 코칭이 실패해도 러닝 저장 · 정산은 그대로다. |
| 공유 크롬 · 경로 | 하단 탭 다섯(러닝 · 신발 · 뽑기 · 커뮤니티 · 내 정보)은 `AppChromePolicy` 하나가 정한다. 새 길은 정책 · 목록(`docs/redesign/screen-inventory.json`)에 올라야 `tools/check_design_contract.py` 를 통과한다. | 코칭 길은 모두 러닝 탭 아래 `Header.Detail`(하단 탭 보임, 머리는 화면이 그린다)로 올린다. 전역 메뉴는 그대로다. 위쪽 진입 위치는 사용자 결정 — 아래 "남은 결정". |
| 빌드 · 확인 수단 | 이 클라우드 세션은 `dl.google.com`(안드로이드 SDK · Google Maven)이 막혀 앱을 컴파일할 수 없다. 저장소 CI(Build APK)는 `claude/**` 푸시에서 문자열 · 디자인 계약 검사 · 단위 테스트 · APK 빌드를 돌리고, 배포(테스트 APK 올리기 · 서버 스키마 적용)는 `main` 에서만 한다. | 순수 계산은 이 세션에서 kotlinc 2.0.21 + JUnit 으로, 서버는 Postgres 16 으로(`supabase/tests/run.sh`), 워커는 node 로 돌린다. 화면 · Room 코드는 CI 컴파일이 필요하다. |
