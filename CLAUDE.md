# StepUp — 작업 메모

## 최신 — 매일 이용자 · 활동 숫자 (2026-11-24, 서버 0124)

사용자 "매일 09:00 기준 실 이용자 수 · 도움될 만한 것 전부" → "그냥 여기다가 해"(Claude 대화로). 셈은 `economy.daily_stats_report(제외 이메일)` 하나 — 어제(한국 날짜)
활성(앱 출석 CHECK_IN ∪ 러닝, 무효 빼고) · 7 · 30일 · 가입 · 다시 온 사람 · 러닝 · SUP · 한 일 · 앱 판 · 체인. 숫자만.
- 받는 길 둘: Claude 대화(`public.daily_stats()` — 머리글 `x-stats-key` 가 `economy.stats_keys` 해시와 맞을 때만, 키는 SQL 창 `select economy.stats_key_new('claude');`)
  · 메신저(`.github/workflows/daily-stats.yml` — 텔레그램 / 디스코드 비밀이 있을 때만). **저장소가 공개라 숫자를 로그 · 이슈에 남기지 않는다.**
- 빼고 셀 이메일: 설정 `stats_exclude_emails`(쉼표) · 워크플로는 변수 `STATS_EXCLUDE_EMAILS`. 서버 검사 "매일 숫자(0124)".
- 서버 검사를 한국 시각 0 ~ 3시에 로컬로 돌릴 때: `apt-get install libfaketime` 뒤 테스트 Postgres 를 `LD_PRELOAD=…/libfaketime.so.1 FAKETIME='-14h'` 로 먼저 띄우면 run.sh 가 그것을 쓴다.

## 최신 — 0118 ~ 0123 나눠 켜기 · 배포 뒤 할 일 한 곳에 (2026-11-23)

[docs/rollout-0118-0123-2026-11-23.md](docs/rollout-0118-0123-2026-11-23.md) — 사용자 "#210 을 나눠서 내보내요". 번호로 자르지 않는다(0122 · 0123 이 0118 ~ 0121 의 구멍을 막는다).
- **2026-11-23 운영: 1 ~ 3단계 끝**(1단계 SQL → #212 합침 · 13:14 배포 → 2 · 3단계 스위치 켬, `REWARD_RULES` 알림은 보내지 않음 — 사용자 "굳이"). 러너 레벨은 앱 30 이 나간 뒤.
- 1단계: 합치기 **전에** SQL 창에서 `supabase/rollout/2026-11-23-before-merge.sql`(새 셈의 스위치 열셋을 예전 값으로 — 마이그레이션은 `on conflict do nothing`) →
  서버 몫만(`app/` 은 29 그대로) 합친다. 2단계 0118 보상 걸음 · 0119 뿌리 → 3단계 0120 함께 달리기 → 4단계 #210 앱 몫(테스트 APK 30) + 러너 레벨. 켜는 SQL · 되돌리기는 문서에.
- 확인: 지금 서버 → 1단계 SQL → 새 서버(두 번)에서 값이 남고, 같은 러닝 넷의 적립 · 닳음 · 에너지가 소수점까지 같다(운 1). 다른 것은 목표 보너스를 서버가 바로 주는 것(예전 앱의 받기는 거절 — 두 번 안 준다).
- 서버 몫 브랜치에서는 `scripts/check-economy-twin.py` 를 main 것으로 둔다(새 것은 앱 30 의 `ForgeXpRule` 을 읽는다 — 4단계에 함께).
- 경제 모의(`tools/economy_sim/flows.sql`)의 시작을 어제 01:00 으로 고정 — `now() − 8시간` 이라 한국 시각 낮에 돌리면 "고칠 곳이 없습니다"로 멈췄다.
- **손일은 [docs/운영-배포후-할일.md](docs/운영-배포후-할일.md) 한 곳에**(push-send · 알림 · OAuth 개발 지문 · Cloudflare Access · 보안 이슈 #184 SQL · 스위치 모음) — 새 손일도 여기에 더한다.

## 최신 — 2차 점검: 꼼수 · 해킹 · 유지보수 (2026-11-22, 서버 0122 보강 · 0123 · 앱 · 웹 · PR #210 에 함께)

사용자 "다시 의심되는 부분 찔러보고 버그 한 번 더 고쳐놔" → "모든 꼼수 / 해킹 등 유지보수 관련해서 전부 할 수 있는 거 다". 여덟 갈래(0122 깨뜨리기 · 아이템/이벤트 · 수리/강화 ·
앱 숫자 · DB 보안 · 꼼수 · 웹/워커/운영자 · 앱 보안) — [docs/점검-2026-11-22-2차-꼼수-보안.md](docs/점검-2026-11-22-2차-꼼수-보안.md)(재현 `tools/together_sim/zz-audit2-*.sql`, CI 밖).
- **`record_session` 은 0123 이 마지막 정의다**(0122 본문 + 태운 신발 · 사본 경로 · 가입 전 · 서버 과속 · 움직인 시간 · 이벤트 시간 몫 · 크루 아이템). `tools/build_0123.py` 가 만든다 — 다시 고칠 때 0123 을 복사한다.
- 0122 보강(아직 운영 배포 전이라 0122 안에서): 묶인 체력은 `crew_pool_held_where`(끝냈는데 함께 달린 러닝 없음 · 5분 소식 없음 → 묶음, 올리면 풀림) · 돌려받기 10분 창은 뺐다 ·
  번개 한 번은 앞 러닝이 함께 달린 사람일 때만 · 목표는 원장이 있을 때만 끝(채운 날로만 남으면 남은 몫으로 다시) · 미룬 정산은 방 자물쇠를 잡아 보기만 · 채우기는 `goal_peek`.
- 0123 서버: 태운 신발은 러닝 시작 뒤 태웠을 때만(닳을 몫 수리비 `burnwear:`) · 사본 경로(시각 간격 90% 같음 + 5m — `route_copy_of`, `walk_sessions.route_sig · route_dts`) ·
  가입 전 러닝 무효(`signup_run_guard`) · 과속은 서버도(25km/h 30초 150m, 신고 없으면 무효 — `track_motion`) · 닳음 · 크루 체력은 경로의 움직인 시간이 바닥 ·
  이벤트 · 부스터 배수는 그 창에 든 몫만큼(`run_reward_mult_span`) · 크루 러닝 아이템은 돌려줌 · 묶인 채 사라진 아이템 여드레 뒤 돌려줌 · 함께 달린 사람은 가입 24시간 뒤(`together_min_account_hours`) ·
  `day_room` × 1 · 보호막은 반나절 넘게 덮어야.
  권한: 프로필 직접 UPDATE · 크루 · 글 · 코스 · 크루원 · 댓글 직접 DELETE · `admin_economy_set` 을 앱 역할에서 거둠(앱 · 예전 앱 1.17.1 은 읽기만 · RPC 로만 쓴다) ·
  좋아요 · 번개 참석자는 보이는 글만 · 이야기 지도 앞뒤 300m · IP 해시 비밀 소금(`economy.app_secrets`) · 운영자 모든 곳 잠금 없앰 · 이벤트 배수 반올림 · 이어 붙인 이벤트는 키 한 번 더 ·
  타워 이름은 처음 참가한 곳에서만 · 예전 레이스 닫음 · 체인 하루 걸음 목표는 서버 값 · 홈 목표 줄(`box_status` 채운 날 0) · 강화 견적 서명.
- 앱: 출발 전 적립 시간은 신발과 에너지 가운데 먼저 끝나는 쪽(`RunV4Lines.runCheckLine`) · 번개 예상 × 1 · 크루 예상 숫자 없음 · 대기실 0120 글 · 고참 에너지 줄 · 첫 동기화 전 에너지 숨김 ·
  **잠금 화면 덮개가 켜진 동안 모든 창 · 시트 · 펼침 목록을 그리지 않는다(`ui/components/LockCover.kt` — 새 창은 `if (LockCover.hidesWindows()) return` 로 시작, `LockCoverTest` 가 지킨다)** ·
  알림 링크는 설치마다 비밀 값(`core/LinkToken.kt`)이 있을 때만 · 방 링크는 들어가지 않고 열린 방 목록 · 지갑 앱 패키지 지정 · 민감 복사 · 연결 주소 표시.
- 웹 · CI: `/draw*` CSP · assetlinks 에서 디버그 키 뺌 · Experience QA 쓰기는 publish 작업만 · 장면 `18-audit-fixes.sql` · `19-cheat-fixes.sql`(CI) · 서버 검사의 **카탈로그 검사**
  (새 표 RLS · search_path · 앱 역할 바로 쓰기 목록 · 내부 함수 · 뷰 — 목록을 늘릴 때는 까닭과 함께) · `check-economy-twin.py` 가 앱 걸음 · 에너지 · 러너 기본값도.
- **사용자 몫**: 비밀값을 main 전용 환경으로 + main 브랜치 보호 · 운영 OAuth 에서 디버그 SHA-1 지우기 · Firebase 키 API 제한 · 대기 명단 재개 전 Turnstile ·
  타워 상품 전 상위 기록 확인 · 부계정 · 지어낸 사본은 기기 확인(Play Integrity)이 있어야 끝까지 막는다.

## 최신 — 경제 점검 고침 열두 가지 · 하루 에너지 상한 10 (2026-11-22, 서버 0122 · PR #210 에 함께)

사용자 "경제적으로 플로우 많이 고쳤는데 또 문제될 만한 거 없어?" → 네 갈래 점검(들어오는 SUP · 여러 계정 · 쓰는 곳 · 튼튼함) → "버그는 다 고치고 · a 는 너가 알아서해봐(인플레이션 막게) ·
b 는 괜찮음 · c 는 한시적". 찾은 것 · 고친 방식 · 정한 것은 [docs/점검-2026-11-22-경제.md](docs/점검-2026-11-22-경제.md)(재현 장면 `tools/together_sim/zz-audit-*.sql` — 고치기 전 규칙으로 쓴 기록, CI 밖).
- `record_session` 은 **0123 이 마지막 정의다**(맨 위 절 — 0122 는 0120 본문 + 아래 ② · ⑤ · ⑦ · ⑧ · ⑨). 0122 는 `tools/build_0122.py` 가 만든다(고칠 함수의 마지막 정의를 복사해 고칠 곳만 바꿈).
- 고친 것: ① 예전 앱 `market_import` 로 상급 상자 6개(다시 올려도 레벨 그대로 · 레벨 상자는 IMPORT · MINT 빼고) ② 크루 러닝 박동을 끊으면 묶인 체력을 쓰던 것
  (`crew_pool_held_except` · 묶는 사람은 2차 점검에서 넓혔다 — 맨 위 절) ③ 상한에 걸린 FLAGGED 러닝도 러너 걸음(과속 · 무효만 뺌, 0122 때 모두 다시 셈)
  ④ 하루 최대에 막혀도 목표를 채운 날(`goal_days.met_at` — 연속 · 7일 상자) ⑤ 크루 효율 8 : 2 는 달린 만큼 무게(남의 무게는 내 무게까지 — +58% → +13%) · 보상 걸음 0 은 함께 달린 사람 아님
  ⑥ 강화 견적의 하루 에너지 = 러너 에너지 ⑦ 정산은 남의 원장 자물쇠를 기다리지 않고 잡아 보고, 바쁘면 미뤄 둔다(`economy.together_pending` — 그 사람이 다음에 올릴 때)
  ⑧ 원격 번개 보너스 글 하나에 한 번 ⑨ ALL 로 올린 크루 러닝은 인원 보너스를 준 것으로 · `runner_level_steps` 는 10,000 부터 ⑩ 코스 · 도전 보상도 하루 최대에(`day_earned`)
  ⑪ 가입 선물 10개는 상자 원장 `SIGNUP` 으로 ⑫ 러너 레벨 상자를 "러닝 상자"로 알리던 것(`box_status` recent).
- **A 인플레이션**: 러너 레벨의 하루 에너지 상한 `runner_energy_cap` **10(36분 — 0121 전 모두의 값)**. 새 계정은 20분에서 Lv10 까지 +2분씩, 상자 · 배지는 그대로.
  **대격변(기본 적립 낮춤)을 켤 때 16.5(60분)로 함께 올린다.** B(레벨 상자로 신발이 빨리 큼)는 그대로 · C(48시간 무료 수리)는 한시적 — 끝낼 때 `free_repair_hours` 0.
- 검사: 서버 검사 "경제 점검 고침(0122)" · 장면 `tools/together_sim/18-audit-fixes.sql`(OK / FAIL — CI) · `check.py` 는 새 계정(5.5)과 고참(에너지 10에 묶음) 둘 다.
  서버 검사의 0116 절은 한국 시각 새벽 0 ~ 3시에 "3시간 전 러닝"이 어제로 잡혀 실패한다(예전 것) — 그 시각에 로컬로 돌리면 Postgres 를 `libfaketime`(FAKETIME='-14h')으로 띄운다.
- **사용자 몫**: 공지(하루 에너지는 Lv10 · 36분까지 · 상자 · 배지는 그대로).

## 최신 — 러너 레벨: 1만 걸음마다 1레벨 · 하루 에너지 · 레벨 상자 · 배지 (2026-11-21, 서버 0121 · PR #210 에 함께)

사용자 "계정 자체의 레벨" → "복잡하게 하지 마 · 걸음 수로 · 좋은 점도" → "줘 · 패치해놔". 설계 [stat-overhaul 14장](docs/stat-overhaul-2026-11-21.md) ·
약속 [docs/runner-level-contract-2026-11-21.md](docs/runner-level-contract-2026-11-21.md). 대격변의 다른 장(능력치 · 행운 · 성장)과 따로 먼저 올렸다.
- 러너 걸음 = 러닝의 보상 걸음(없으면 경로 걸음) — 날마다 48,000 까지(에너지 뒤 · 크루 걸음도, VOID · 과속으로 멈춘 러닝 빼고 — 0122 전엔 FLAGGED 모두 뺐다). 레벨 = 1 + 걸음 ÷ 10,000(최대 30).
- **하루 에너지는 러너 레벨**(`economy.energy_max` — 신발 레벨과 떨어짐): 5.5 + 0.55 × (Lv − 1), 최대 `runner_energy_cap` — **0122 부터 10(Lv10 · 36분, 인플레이션)**, 대격변 때 16.5(Lv21 60분, 분당 165보 기준).
  0121 전 계정은 예전 10(약 36분)보다 줄지 않는다(`runner_levels.legacy_energy`). 되돌리기 `runner_level_rule` 'OFF'(예전 — 가진 신발 가운데 최고 레벨).
- 레벨마다 일반 상자(`box_grant` 'RUNNER:<Lv>' FREE) · 5레벨마다 상급 상자('RUNNER5:<Lv>' GIFT) — 0121 을 올릴 때 지난 걸음으로 채운 레벨도 한 번(`runner_level_20261121`). 배지 Lv10 · 20 · 30.
- 서버: `economy.runner_levels` · 트리거 `walk_sessions_runner` · `runner_refresh` · 앱 함수 `runner_me()` · `runner_badges(uuid[])` · 운영자 손잡이 넷(마흔다섯 개).
  서버 검사 "러너 레벨(0121)" · 예전 절과 장면(`tools/together_sim/helpers.sql`)은 끄고 돈다 · `check.py` 는 켠 채(새 계정 첫날 걷기 55분 62.3 · 달리기 37분 76.3 SUP).
- 앱: `data/remote/RunnerLevelApi.kt` · `data/repo/RunnerLevelRepository.kt`(동기화 때 다시 읽음 · 알림은 닫아야 본 것으로) · `domain/RunnerLevel.kt`(`RunnerLevel` — 같은 파일의 예전 거리 레벨
  `RunnerLevels` 는 홈이 그대로 씀) · 내 정보 카드 · ⓘ 시트 · 레벨업 / 처음 본 요약 시트(`ui/screens/profile/RunnerLevelUi.kt`, 탭 첫 화면에서만 · 러닝 중 아님) ·
  배지(크루 명단 · 크루 대기실 · 번개 대기실) · 신발 화면의 "신발 레벨 → 에너지" 줄은 러너 레벨이 켜져 있으면 숨김. 글 `runner_level.xml`(4개 언어). 서버에 함수가 없으면 모두 숨김.
- **사용자 몫**: 공지(러너 레벨 · 하루 에너지는 걸음으로 · 지난 걸음으로 상자 지급 · 새 계정은 20분부터).

## 최신 — 함께 달리기 보너스 고치기: 탈주 · 하루 최대 SUP (2026-11-21, 서버 0120 · PR #210 에 함께)

점검 [docs/점검-2026-11-21-보너스.md](docs/점검-2026-11-21-보너스.md)(20명 출발 · 18명 즉시 종료 · 둘이 3시간 → 첫 신발로 한 번에 2,639.7 SUP) →
[설계 · 만든 것](docs/together-bonus-fix-spec-2026-11-21.md) 14장. **인원 보너스 절벽(6 · 20명)과 작은 크루가 혼자와 비슷한 것은 의도다**(사용자 — "러닝 크루로 달리라고 거의 강제").
- **`record_session` 은 0123 이 마지막 정의다**(맨 위 절 — 0120 은 0119 본문 + 함께 달린 사람 · 하루 최대) — 다시 고칠 때 0123 을 복사한다.
- 함께 달린 사람 = 그 방 러닝으로 올렸고 · 무효 아님 · 경로가 받쳐 줌 · 확인된 걸음 1,500 · 달린 10분 · **목표 거리의 50%**(크루 코스 · 번개 글의 거리, 자유 러닝이면 바닥만 — `economy.together_qualifies`, 크루 · 번개 모두 `'ACTIVE'`).
  인원 보너스는 그들이 올릴 때마다 이미 올린 모두에게 차이만큼 더한다(`economy.together_settle` — 24시간 · 줄지 않는다 · 원장 `together:<러닝>:<인원>`).
- 탈주(10분 전에 끝냄 · 일시정지 · 앱 강제 종료 · 계정 삭제)한 사람의 공용 체력은 묶는다 — 쓴 만큼만 남는다(`economy.crew_pool_held` · 트리거 `crew_runs_deleted`).
- 하루 최대 SUP = 그 러닝 신발로 1시간(9,900보) 달린 값 × 4 × 적립 배수(가입 7일 안 절반) — 혼자 · 번개 · 크루 · 보너스 · 목표 보너스의 합(`economy.daily_max_of` · `day_earned`).
- 크루 효율 8 : 2(사용자 "8 : 2 오케이" — 설계 14.3장): 함께 달린 사람은 내 신발 80% + 함께 달린 크루원 평균 20%(`crew_blend_share` 0.2). 올릴 때는 아래 끝(`economy.crew_blend_avg`)으로 주고
  사람이 정해질 때마다 `together_settle` 이 차이만 더한다(되돌려 받지 않는다 · `walk_sessions.together_unit` · `crew_runs.blend_sup`). 함께 달리지 못한 사람 · 경로 없는 러닝은 내 신발 그대로.
  예전 하루 금액 상한(`daily_earn_cap`)은 `daily_max_rule = 'CAP'` 일 때만. 되돌리기는 `crew_bonus_rule` · `party_bonus_rule` `'ALL'` + `daily_max_rule` `'CAP'`.
- **강제 업데이트**: 문지기(`public.access_gate`)가 `app_min_version_code`(스토어 번호, 기본 0 = 끔)보다 낮은 앱을 HTTP 426 `update_required` 로 막는다 — 앱은 머리글 `x-stepup-version`(versionCode % 1000).
  로그인 안 한 요청 · 계정 삭제 · 푸시 끊기는 지나간다. 켜는 것은 새 판이 스토어에 나간 뒤 · 공지 뒤에 운영자 페이지 "경제"에서(사용자 몫).
- 검사: 장면 `tools/together_sim/16-together-fix.sql`(OK / FAIL — CI) · 서버 검사 "함께 달리기 보너스 · 강제 업데이트(0120)" · 예전 절은 `'ALL'` · `'CAP'` 으로 돈다(맨 위에서 맞춘다).
  앱(1.21.0 — 설계 14.2장): 대기실 "목표의 절반 이상 함께 달려야 인원" · 달리는 중 "인원은 올린 뒤 정해져요" · 크루 결과 "친구가 올리면 더해져요" · 신발 "하루 최대 약 N SUP" · "업데이트가 필요해요" 화면 ·
  규칙 `TogetherRule`(`rule` 크루 · `flash_rule` 번개) · `DailyMaxRule`(`domain/EconomyV4.kt` — `check-economy-twin.py` 가 0120 과 맞대어 본다).

## 최신 — 능력치 대격변 설계 중 (2026-11-21, 설계만 · 코드 없음 · 사용자와 하나씩 정하는 중)

**설계안은 [docs/stat-overhaul-2026-11-21.md](docs/stat-overhaul-2026-11-21.md)**(첫 초안은 사용자가 "모델이 이상하고 부족"이라 물렸다 — 서버와 맞대지 않았고 숫자를 지어냈다).
- 기준선: `tools/economy_sim/stats_today.py` = 지금 서버의 하루 셈(`economy.points_day_net`) — 장면 `17-stats-baseline.sql` 과 744 경우 0.1 SUP 안(CI). 서버 셈을 바꾸면 함께.
- 5판 모형 `tools/economy_sim/stats_v5.py`(`--check` · `--fit`): 굽이 넷 모두 τ = 만렙 포인트의 20% · 한계 B 와 뽑은 효율 × K 는 등급마다 "만렙 하루 · 레벨 올리는 값 = 지금"으로 맞춘 값(`stats_v5_fit.txt`).
  행운은 끝을 등급마다(40 ~ 100) · 줍는 상자 · 수리는 그 신발 등급의 값.
- **사용자가 정함**(문서 6장): 해결 넷 모두 좋음 · Lv1 에너지 20분 · 높은 등급 만렙 "달린 만큼" · 행운 등급별("에픽 이상은 다르게").
  짧게 달리면 효율 몰빵인 것은 막지 않는다(굽이가 "올라가면 고루"를 만든다). 굽이는 서버에 아직 없다(지금은 곧게 오름).
  에픽 이상 행운(9장 제안): 조각 · 상자 대신 그 신발에 강화 경험치(소 300 · 중 600 · 대 3,100, 만렙이면 가방의 경험치 병) · 수리권 = 그 신발 가득 수리(`LOOT_OF`).
  상급 상자(사용자 "나중엔 쓸모없다") · 수명 회복(피니시 수리비 83 ~ 105% 를 메워 소각이 무너짐)은 버렸다.
  10장(두 번 굴림 · 나눔) · 11.3 ~ 11.6(황금 구간) · 12장(아홉 가지)은 사용자 "별론디?" · "너무 지저분한디"로 걷어냈다(견준 기록 · 모형 `luck_tiers.py` · `luck_catalog.py`).
  남는 것: 11.2 성장 — 신발은 달린 만큼 자란다(보상 걸음 × 0.163 × 3.1^(등급−1) 경험치, 피니시 만렙 1년 — `growth_luck.py`).
  **13장(지금 안)**: 모든 신발의 행운(기획서 그대로, 에픽 이상 줍기는 경험치) 위에 에픽 이상은 경험치 · 강화 효과 — ① 경험치 러닝(러닝 끝, 경험치 × 2 ~ 5) ·
  ② 강화 대성공(× 3 ~ 6, 지금 × 2) · ③ 보너스 포인트(레벨업, + 1 ~ 4) · ④ 잠재 각성(레드라인부터 5레벨마다 가장 낮은 뽑은 값 + 10 · 15%) · ⑤ 한계 돌파(피니시 만렙 뒤 포인트 + 1, 상한 16).
  에픽 · 레전더리 3 · 레드라인 4 · 피니시 5개(사용자 "일단 오케이"). SUP 를 바로 찍지 않는다. 모형 `tools/economy_sim/luck_forge.py --check`(성장도 이것 — growth_luck.py 는 11장 기록).
  **13.6 점검**: 경험치 걸음 ≠ 서버 보상 걸음(에너지 안 · 닳기 전 · 경로 확인 · FLAGGED 0) · ② · ③ · ④ 의 Λ 는 그 레벨에 들어선 때의 값(다시 나누기 꼼수) ·
  만렙 뒤 달리기 경험치는 피니시 ⑤ 만, 병은 줍기에서만 · 대성공은 제 값에만(등급별 설정 묶음) · ④ 는 효율 · 내구도 · 착화감 · 크루 행운은 함께 달린 사람으로(0120 방식) ·
  크루 러닝은 ① 없음. **정함**: 성장 속도 = 피니시 만렙 1년(pace 목표를 13.3 표로) · 만렙 힘은 0포인트 기대 몫만큼 기본(B · K)을 낮춰 맞춘다.
  에너지가 따르는 신발 → 사용자 "계정 자체의 레벨" → "복잡하게 하지 마 · 걸음 수로 · 좋은 점도" → **14장 러너 레벨 — 만듦(서버 0121, 맨 위 절)**: 1만 걸음(러닝의 경로 확인 걸음 —
  에너지 뒤 · 크루도, FLAGGED 빼고 · 하루 48,000)마다 1레벨 · 최대 30 · 레벨마다 하루 에너지 + 2분(20 → Lv21 60분) · 보통 상자 1 · 5레벨마다 상급 상자 · Lv10 · 20 · 30 배지.
  신발 레벨은 포인트 · 성장만. 지금 쓰는 사람은 하루 에너지 10 아래로 안 내림. 지난 걸음으로 채운 레벨에도 상자(사용자 "줘").
  남은 것(7장): 손상 세부 → 레벨 속도 · 불변식 다시 → 서버 0121(꺼 둔 채) · 앱 → 켜기.

사용자 "스텟 대격변도 생각해야 · 무조건 효율이 최고로 만들면 안 돼" — 0119(아래 절)를 올린 뒤 나온 방향. **아직 만들지 않았다.** 정해진 것:
- 닳은 뒤 적립은 **11 ~ 15%**(0119 의 30% 대신) · 0% 신발로 출발하면 **(50 − 레벨)% 로 손상** — 손상은 그 러닝이 끝난 뒤 적용, 손상된 신발은 100% 수리 전까지 적립 0% · 달릴 수 없음(수리하면 풀림). 출발 전 안내문.
- 하루 에너지는 레벨에 따라(포인트 없이 달리기 기준 **Lv1 약 20분 → 만렙 약 60분**) — 0119 의 "10 고정"을 대신한다.
- 능력치 넷: **효율**(걸음마다 SUP) · **내구도**(신발 + 에너지가 오래 감 · 수리비 — 예전 착화감의 에너지 절약을 합침) ·
  **착화감**(최종 SUP +%, 신발 상태가 좋을수록 많이 · 상한) · **행운**(새 능력치). 능력치에 부작용은 없다(0119 의 "효율 포인트 → 빨리 닳음 · 에너지"는 뺀다) —
  대신 **한 능력치에 많이 넣을수록 포인트 하나의 효과가 준다**(어림 모형: 효율 몰빵은 최선의 65 ~ 87%).
- 행운: [docs/luck-spec-2026-11-21.md](docs/luck-spec-2026-11-21.md) — 출발 · 달리는 중 에너지 5 마다(줍기 — 소 · 중 · 대, 연쇄) · 보상 받을 때 확률로, 흐름 · 꾸준함 · 아깝다 게이지.
  보상 행운은 퍼센트(+40%)와 고정 SUP 두 가지를 따로(고정은 % 를 다 계산한 뒤 그대로 더한다). **클로버는 아직 넣지 않는다** · 신발마다 행운 성향은 나중.
  **크루 러닝에서는 확률이 아니라 늘 발동**(사용자 안): 출발 전 크루 전체 효율 · 착화감 +N% · 끝난 뒤 고정 SUP — 함께 출발한 사람들의 행운 합 · 인원 · 크루 레벨로 커진다(기획서 7.5장, 열린 검토 셋).
  숫자는 `python3 tools/economy_sim/luck_model.py`(`--check` 는 기획서 9장의 설계 목표 — `value()` 의 상자 · 수리 값은 인자로, 기본은 레어 가운데). 대격변을 켜는 차례(앱 먼저 → 서버 스위치)는 15.1.
- 남은 것: 위 문서의 1 ~ 7단계를 하나씩 → 사용자 "진행" 뒤에 서버 · 앱. 사용자는 한 번에 하나씩, 구체적인 수식 · 숫자로 보고 싶어 한다. 크루 능력치 합치기는 정함(8 : 2 — 효율은 0120 에서 만듦).

## 최신 — 경제의 뿌리 고침: 내구도 요율 · 능력치 조율 · 통의 굽이 · 에너지 (2026-11-21, 서버 0119 · PR #210 에 함께)

[docs/economy-roots-2026-11-21.md](docs/economy-roots-2026-11-21.md)(찾은 것 · 뿌리 · 만든 것 8장 · 추천안과 다른 곳 8.3) · [서버 ↔ 앱 약속](docs/economy-roots-contract-2026-11-21.md) —
사용자 "뿌리를 뽑아버려야함 · 6번은 빼고" → "진행해 · 효율이 무조건 좋지 않게 스텟을 잘 조율". 문제 6(SUP 쓸 곳이 상자뿐)은 범위 밖.
- `record_session` 은 0123 이 마지막 정의다(맨 위 절 — 0119 는 0118 본문 + 닳은 구간 · 효율 배수 · 보상 걸음).
- 내구도는 문이 아니라 요율: 남은 내구도로 달린 걸음은 다, 그 뒤 걸음은 30%(`worn_rate`) — 0% 신발로도 출발 · 걸음은 다 센다(보상 걸음 = 경로로 확인된 걸음 모두, 크루도).
  닳은 구간의 걸음도 에너지는 그대로 쓴다(덜 쓰면 수리 없이 오래 걷는 쪽이 이득). 까닭 글 `신발이 다 닳은 뒤의 걸음은 일부만 적립했습니다`.
- 능력치 = 축 셋: **효율 = 버는 속도**(포인트 배수 u 만큼 신발이 빨리 닳고 에너지를 더 쓴다 — "1분마다 5"는 5 × u) · **내구도 = 한 켤레로 버는 양**(가득 수리비는 포인트와 상관없다) ·
  **착화감 = 하루에 버는 양**(하루 에너지는 레벨과 상관없이 10 — `energy_per_level` 0). 한 곳에 다 넣는 것보다 섞는 쪽이 남고, 달리는 시간에 따라 답이 갈린다(짧게 = 효율 · 길게 = 내구도 · 착화감).
  포인트 화면은 `sneaker_points_preview`(내 지난 14일 러닝 → "1포인트 = 하루 약 +N SUP" · 추천 배분), 5레벨마다 다시 나누기 한 번(+ 0119 를 올릴 때 모든 신발 한 번).
- 통은 레벨마다 Lv1 통의 30%씩 커진다(`forge_xp_level_growth` — 레어 만렙 24일 · 에픽 약 120일). 예전 앱의 확률 강화 성공률도 같은 굽이(`forge_base_permille` — 더 싸지 않게 3%p 낮춤).
- 되돌리기: 설정 여섯을 예전 값으로 두면 0118 과 똑같이 셈한다 — `worn_rate` 0 · `eff_wear_exp` 0 · `eff_energy_exp` 0 · `dur_repair_exp` 0 · `energy_per_level` 2 · `forge_xp_curve` "CLASSIC"
  (서버 검사의 예전 절은 모두 이 값으로 돈다 — 맨 위에서 맞추고 "경제의 뿌리(0119)" 절이 다시 켠다). 운영자 페이지 "경제"는 서른다섯 개(굽이 · 수리 세기는 SQL 창에서만,
  `worn_rate` 0.45 · `forge_xp_need` 3.2 · `forge_xp_level_growth` 0.33 까지 — 그 밖은 "수리가 늘 이득" · "예전 앱 강화가 더 싸지 않다"가 깨진다).
- 경제 숫자를 바꾸면(CI `test-sql.yml`): `python3 tools/economy_sim/check.py`(불변식 여덟 — 포인트 배분 넷 · 새 사용자 첫날 목표 · 수리하는 쪽이 늘 낫다 · 예전 앱 강화가 더 싸지 않다) ·
  `python3 tools/economy_sim/pace.py --check`(레어 24일 · 에픽 119일 ±20%) · `forge_xp_model.py --check` · `scripts/check-economy-twin.py`(앱 `StatRule.FALLBACK` = 서버).
  장면 `tools/together_sim/13-first-day-starter-shoe.sql` · `14-stat-points-value.sql`(한 곳에 다 넣기 vs 서버 추천) · 실제 사용 숫자 `tools/economy_sim/usage.sql`(SQL 창 · 읽기만).
- 앱: `StatRule` · `ShoeRules.stats`(`domain/EconomyV4.kt` — 서버에 `stats` 묶음이 없으면 `StatRule.LEGACY` 로 예전 글 · 예전 셈). 신발 수명 · 닳음 · 예상 적립은
  `RewardEconomy.shoeSessionReward` · `ShoeRules.lifeSecOf / wearOf / startLifeSec`(`domain/RewardEconomy.kt`) 하나로, 닳은 신발 글은 `walk/RunV4Lines.kt`(`shoeWorn*Text` · `shoeCheckLineText`),
  포인트 화면은 `domain/ShoePointsPreview.kt` · `ShoePointsPreviewApi`(서버에 함수가 없으면 칸을 숨긴다) · `ShoePointsSheet.kt`(값 · 수명 · "추천대로 나누기" · "처음부터 추천대로") ·
  `ShoeStatsV4.kt`(`lifeMinutes(shoe, 내구도, rules)` — 신발 화면의 "분"은 모두 이것). 글은 `economy_roots_run.xml` · `economy_roots_shoe.xml` · `shoe_info.xml` 의 `_v2`(4개 언어).
- 다시 나눌 때 남은 내구도는 같은 비율로 바뀐다(`sneaker_points_set` 0119 — 뺐다 넣어 공짜로 채우지 못하게). 독립 검토에서 나온 것 · 알고 둔 것(달린 시간은 앱이 보낸 값 · 크루 러닝 · 아주 느린 걸음)은 문서 8.5.
- **사용자 몫**: 공지에 0119 줄(기획서 8장 — 닳은 신발도 걸음은 센다 · 능력치 뜻 · 에너지 10 · 다시 나누기) · `energy_per_level` 은 실제 사용 숫자(`usage.sql`)를 보고 굳힌다(문서 8.4).

## 최신 — 강화 경험치 · 오늘의 보상 · 능력치 ⓘ 만듦 (2026-11-19, 서버 0118 · 새 판 30 · 1.21.0 · 폰 DB 판 23)

사용자 "fable 5.2가 써둔 문제 수정 기획서를 전부 반영해서 진행해" — [기획서](docs/forge-xp-daily-spec-2026-11-19.md)(기본값 모두 추천대로, 만든 것 · 다른 점은 10장) ·
[서버 ↔ 앱 약속](docs/forge-xp-daily-contract-2026-11-19.md) · 설계 · 전수 점검 [docs/forge-xp-daily-2026-11-19.md](docs/forge-xp-daily-2026-11-19.md). 함께 뛰기 고침(PR #209)은 먼저 합쳤다(29 · 1.20.1).
- **`record_session` 은 0123 이 마지막 정의다**(맨 위 절 — 0118 은 0117 본문 + 보상 걸음 · 같은 경로 검사를 모든 계정으로 · 기록한 뒤 `economy.goal_settle`) — 다시 고칠 때 0123 을 복사한다.
- 강화(새 앱): `forge_xp_materials` · `forge_xp_quote` · `forge_xp_add` · `forge_xp_result` · `my_shoe_xp` — 통 `market_sneakers.forge_xp`(일반 1켤레값 = 1,000점), 실패 없음 ·
  대성공 · 물려줌 · 닳은 몫은 설정(`forge_xp_*` 여덟). 예전 앱은 예전 강화 그대로, 재료만 내구도 가득(`forge_material_min_durability` 100). 만렙이 되면 통은 0(트리거).
- 오늘의 보상: 상자 · 목표 · 연속 · 스텝 서지는 **보상 걸음**(`walk_sessions.reward_steps` — 경로로 확인된 걸음. 0118 은 신발 내구도가 남은 동안 · 크루는 공용 체력 몫만 셌고 0119 부터 모두 센다) 하나로.
  그 날의 목표 `economy.goal_days`(바꾼 목표는 다음 날부터, 오늘 보상 걸음이 0 이면 오늘부터 · 4,000 ~ 20,000 · 500 단위 · 새 계정 6,000). 목표 보너스는 러닝을 기록할 때 서버가 바로.
  되돌리기는 SQL 창에서 `reward_steps_rule = '"OLD"'`(상자는 적립 걸음 · 목표는 경로 걸음).
- 앱: 강화 화면 `ShoeUpgradeScreen.kt`(경험치 막대 · 재료 줄 · 남는 신발 넣기 · 아까운 재료 확인 — 서버에 0118 이 없으면 예전 강화) · 도메인 `ShoeForge.kt` · `ForgeXpApi` ·
  홈 보상 막대 `HomeScreen.kt` `RewardsHero`(눈금은 서버 `ticks`) · `domain/DailyRewards.kt` · `DailyRewardsApi` · 새로 받은 것 알림(채널 `daily_rewards`) ·
  신발 카드 · 상세의 "다음 레벨까지 %" · 능력치 ⓘ(`ShoeDetailBars.kt` · `ShoePointsSheet.kt`). 글은 `forge_xp.xml` · `daily_rewards.xml` · `shoe_info.xml`(4개 언어).
- 검사: 서버 검사 "강화 경험치 · 오늘의 보상(0118)" · `python3 tools/economy_sim/forge_xp_model.py --check`(CI — 모형 = 서버) · `check.py` 불변식 다섯(닳은 재료) ·
  `scripts/check-economy-twin.py`(강화 · 목표 값). 검사 · 모의 실험의 경로는 사람마다 경도를 0.5m 안쪽으로 달리한다(같은 경로 검사가 모든 계정끼리 맞대어 보므로).
- 올리기 전에 태운 신발로 달린 러닝은 그 신발 값으로(닳음 · 거리는 남기지 않음). 운영 알림 `REWARD_RULES`(예전 앱에도 — 링크 `run`, 사람마다 한 번).
- **사용자 몫**: 배포 전에 공지(기획서 8장 — 상자는 밖에서 · 닳지 않은 신발로 · 목표 4,000보부터 · 다음 날부터 · 강화는 새 판부터 경험치) ·
  배포 뒤 push-send 를 다시 올리고 `select admin_notice_send('REWARD_RULES');` · 1.21.0 AAB 는 테스트 APK 로 며칠 본 뒤.

## 최신 — 함께 뛰기 점검 19건 · 모두 고침 (2026-11-18, 서버 0117 · 새 판 29 · 1.20.1)

[docs/점검-2026-11-18-함께뛰기.md](docs/점검-2026-11-18-함께뛰기.md) — 사용자 "함께뛰기쪽 … 샅샅히 뒤져봐" → "심각성별로 봐봐" → "전부수정해". 장면 검사 `tools/together_sim/run.sh`(실제 서버 규칙, 로컬 Postgres).
- **`record_session` 은 0123 이 마지막 정의다**(맨 위 절 — 0117 위에 0118 보상 걸음 · 0119 닳은 구간) — 크루 러닝을 올릴 때 서버가 세지 못한 달린 시간(앱 달린 초 − `active_sec`)만큼 공용 체력을 쓰고 모자라면 적립에서 뺀다(먼저 올린 사람부터) ·
  경로 없는 러닝 상한 96 을 크루 러닝도 함께 · 번개 보너스는 글 하나에 한 번. 다시 고칠 때 0123 을 복사한다.
- 방: `party_for` 창 출발 −2분 ~ +3분 · 카운트다운 중 `party_leave` 는 오늘 크루 러닝을 돌려준다 · `party_start` 푸시 `PARTY_STARTED` · `PARTY_MISSED`(push-send 를 손으로 다시 올린다) ·
  `party_state` 거절은 `room_started` · 내보냄 · `room_closed` · 번개 대기실은 모이는 시각 60분 뒤 `flash_lobby_closed`, 정원은 글의 정원 · 달리는 중 해산 `crew_running` · 계정 삭제는 진행자를 넘긴다.
- `crew_run_live` 에 `ended_at` · `reward_until` — 혼자 남아 서버가 끝내면(R06 그대로) 앱이 1분 동안 소리 · 진동 · 알림(채널 `crew_run_alert`)으로 알리고 그때 멈춘다.
- 앱: 출발은 러닝이 실제로 켜진 뒤에만 러닝 화면(못 켜면 돌려받고 나옴 — `PartyRunStartHost`) · 돌려받기는 앱 범위에서 다시 시도 · 이 폰이 달린 방은 `CrewRunLedger`(`crew_run_beats` 기록)로 가린다 ·
  모임 러닝을 먼저 올린다(`GROUP_UPLOAD_HOLD_ATTEMPTS`) · 크루 결과는 확정 전 숫자 없음 · 문구는 `together_lobby_fix.xml` · `together_run_fix.xml`(4개 언어).

## 최신 — 경제 4판: 능력치 지수 · 등급별 레벨업 · 운 · 수리 · 상자 · 운영자 (2026-11-18, 서버 0116 · 새 판 28 · 1.20.0)

[docs/economy-v4-2026-11-18.md](docs/economy-v4-2026-11-18.md) — 사용자 "전부 진행해"(점검 [docs/점검-2026-11-18-경제-흐름.md](docs/점검-2026-11-18-경제-흐름.md) · 설계 [docs/shoe-stats-boxes-2026-11-18.md](docs/shoe-stats-boxes-2026-11-18.md) 3판 + 운).
- 능력치는 지수(100 = 기준): 같은 등급 안 100 → 180 · 등급마다 효율 × 1.5 · 시간 × 1.35(일반 22 ~ 40분). 저장은 그대로 `efficiency_bps`(적립 +bps) · `comfort_bps`(에너지 절약 bps).
  레벨마다 포인트 2 · 3 · 4 · 5 · 6 · 8 · 5레벨마다 보너스 + 내구도 가득 + 상급 상자, 포인트 1 = 뽑은 값의 +1.0 ~ +1.8%. SUP 레벨업(`sneaker_upgrade`)은 닫음.
- 걸음당 SUP = 설정 `points_per_step`(0.0118), 수리비 = 신발 효율에 비례(`repair_share` 0.45), 운(보상 0.7 ~ 1.3 · 닳음 0.667 ~ 1.444, `record_session` 이 `reward_luck` 을 돌려준다),
  48시간에 한 번 무료 수리, 상한 · 에너지로 빠진 몫은 닳지 않음, 에너지 최대치는 가진 신발 중 최고 레벨, 부스터 판매 멈춤, 상자(출석 1 + 러닝 2 + 40 SUP · 상급 주간 · 7일 연속 · 레벨 · 600 SUP).
- **`record_session` 은 0123 이 마지막 정의다**(맨 위 절) — 다시 고칠 때 0123 을 복사한다. 0022 · 0059 · 0066 의 표 검사는 4판으로 옮긴 뒤 건너뛴다(다시 배포할 때 옛 식으로 막지 않게).
- 경제 숫자를 바꾸면: `python3 tools/economy_sim/check.py`(CI — 운이 가장 나쁜 날에도 걸어서 남는다 · 강화하면 덜 남지 않는다 · 새 사용자 수리 · 갈아 신어도 에너지),
  `python3 scripts/check-economy-twin.py`(앱 `ShoeRules.FALLBACK` = 서버). 설정은 운영자 페이지 "경제" 탭에서도 바꾼다(열아홉 개 · 늘 키를 한 번 더).
- 예전 스토어 판(v1.17.1)이 부르는 함수 162개는 `supabase/tests/store_app_functions.txt` 로 서버 검사가 맞대어 본다 — 새 판을 스토어에 올리면 그 판으로 다시 만든다.
- 운영자 페이지: 바로 적용(아이템 없이 내구도 가득 · 에너지 되돌리기 — 예전 판에도) · 경제(현황 · 설정) · 지급 때 예전 판 인원 경고. 앱은 동기화 때 판을 알린다(`app_client_report`, 스토어 번호 = versionCode % 1000).
- 앱: `domain/EconomyV4.kt`(규칙 · 지수 · 별 · 유형 · 상자) · `data/repo/ShoeRulesRepository.kt`(`ServiceLocator.shoeRules`) · `ui/screens/items/ShoeStatsV4.kt` ·
  상자 `ui/screens/gacha/BoxShop.kt` · `BoxSheets.kt` · 운 `domain/RunLuck.kt`(폰 DB 판 22 `rewardLuck`). 화면은 문서 "앱 화면".
- 2026-11-18 운영 DB 에서 모든 신발 내구도를 한 번 가득 채웠다(SQL 창, 251켤레). 2026-11-20 또 한 번(12켤레 · 9명, `admin_audit` 의 `durability_reset_sql` —
  조건은 운영자 페이지 "바로 적용 › 내구도 가득"과 같다: 가진 · 앱 안 신발만).

## 최신 — 새 판 1.19.0(27) · 매물 거두기 · 업데이트 보상 · 초대 코드 하루 1 · 운영자 보안 (2026-11-18, 서버 0112 · 0113 · 0114)

- 스토어 판 versionCode 27 · versionName 1.19.0(26 · 1.18.0 은 스토어용 파일을 만든 적이 없다 — 마지막 Release AAB 는 v1.17.1). → 경제 4판에서 28 · 1.20.0 으로 올렸다(27 은 AAB 로 만들었을 수 있어).
- **0112** 배포 때 한 번: 열린 매물 · 입찰을 모두 거둔다(`economy.market_reclaim_20261118`) · 2026-11-18 00:00 KST 전에 **코드로** 들어온 사람
  (`access_grants.source = 'CODE'`, 거둔 권한 · LEGACY · ADMIN 빼고)에게 무료 뽑기 +5 · 상급 뽑기 선물 +1(`draw_grants` kind `GIFT` — 지갑 없이 쓴다,
  상급 뽑기는 GIFT 먼저). 스토어 판도 GIFT 가 있으면 상급 뽑기 칸이 보인다(지갑 · 체인 말 없이). 알림은 **보내지 않았다** —
  `select admin_notice_send('MARKET_RECLAIMED');` · `select admin_notice_send('UPDATE_REWARD');`(새 판이 나간 뒤, push-send 를 먼저 다시 올린다).
  0042 의 kind 검사는 `not valid` 로 바꿨다(다시 올릴 때 GIFT 줄에 막히지 않게 — 0112 가 마지막 목록으로 다시 건다).
- **0113** 사용자 베타 초대 코드는 하루 1개 · 지난 168시간에 3개(`access_user_daily_codes` · `access_user_weekly_codes`), 앱은 `access_code_issue_v2`.
- **0114** 운영자 페이지: 키로 로그인 → 세션(8시간 · 30분 쉬면 끝) · 키 기한 30일(`ops_key_new('이름', 일수)`) · 로그인 잠금(한 곳 5번 · 전체 30번 / 10분) ·
  큰 일(전체 · 200명 넘게 지급, 3배 이상 · 48시간 넘는 이벤트, 되돌리기)은 키를 한 번 더 · 키마다 하루 5,000개. 가장 센 문은 Cloudflare Access(사용자가 대시보드에서 —
  docs/items-2026-11-17.md "운영자 페이지 보안"). 거절은 `{ok:false, code}`(잠금 기록이 남게).
- 체인 기록 현황(공개 탐색기): [docs/체인-기록-현황-2026-11-18.md](docs/체인-기록-현황-2026-11-18.md) — 664건 · 모두 성공. 하루 걸음 기록이 0 이던 까닭은 앱이 `steps_sync` 를 부르지 않아서 — 1.19.0 부터 동기화 때 · 목표를 채운 순간 어제 · 오늘을 올린다(`DailyStepsUploader`, 바뀐 날만 · 계정마다).
- SQL 창에서는 `admin_economy_set` 이 거절된다(로그인 운영자만) — 설정은 `update public.economy_settings …` 로 바꾼다.

## 최신 — 버그 점검: 아이템 · 거래소 고침, 나머지는 목록 (2026-11-17)

[docs/점검-2026-11-17-버그.md](docs/점검-2026-11-17-버그.md) — 사용자 "그외에 버그찾아봐". 여섯 갈래(신발 · 러닝 · 크루/번개 · 아이템 · 거래소 · 계정/푸시/커뮤니티).
- **고친 것은 PR #206 의 코드만**(아이템 · 거래소): 거래 멈춤 거절 코드 22023(55000 은 HTTP 500 — 앱이 "연결 끊김"으로 읽는다) · 예전 시세판에 모델 입찰 안 섞기 ·
  "보유"에서 태운 신발 빼기 · 즉시 판매는 팔러 온 켤레(`sellNowPick`) · 러닝 아이템은 점검에서 "출발"을 누른 그 출발에만(`ItemsRepository.arm` · `clearPicks`) ·
  이미 켜진 배수보다 크지 않은 배수 아이템은 못 고름(`RunItemPicks.covered`) · 운영자 페이지 요청 번호는 보낸 내용에 묶기.
- **나머지 24건도 같은 날 고쳤다**(사용자 "전부수정해", 서버 0109 크루 · 번개 · 0110 신발 · 0111 초대 코드) — 고친 방식은 문서 "모두 고쳤다".
  `record_session` 은 0110 이 마지막 정의다(0109 의 `party_for(사람, 시작, 끝)` 호출 포함) — 다시 고칠 때 0110 을 복사한다.
  푸시 첫 스위치는 "모든 푸시 알림"(끄면 나머지 흐리게) — 8번은 화면을 사실대로 바꾼 것이고 서버 규칙은 그대로다.
- 서버 오류 코드: 앱에 까닭을 보일 거절은 22023 · 23514 · 42501(4xx). `55000` · `P0002` 는 PostgREST 가 500 으로 보내 앱이 "다시 시도"로 읽는다.

## 최신 — 거래소를 도감 모델 단위로 · 팔 수 없는 까닭 · 거래 멈춤 (2026-11-17, 서버 0107 · 0108 · 폰 DB 판 21)

[docs/market-by-model-2026-11-17.md](docs/market-by-model-2026-11-17.md) — 테스터 "판매 누르면 딴 거로 연결"(타비 스플릿 토 조거 → 플레어 시프트).
- **거래는 멈춰 있다**(사용자 "판매하기 버튼도 비활성화 · 거래 하면 안 돼"): 서버 설정 `market_trading_enabled` = false(0108 — 새 매물 · 입찰 · 체결 거절, 거두기는 됨,
  예전 앱도 막힘) + 앱 `MarketFeature.TRADING = false`(판매하기 · 거래 버튼 막고 "지금은 거래를 쉬고 있어요"). 다시 열 때는 둘을 함께.
- 도감 모델이 있는 신발은 모델 번호 하나가 한 장(`ModelKey.ofModel` · 길 `…&model={model}`). 새 앱은 `market_quotes_v2` · `market_asks_v2` · `market_bid_book_v2` ·
  `market_my_bids_v2` · `market_my_trades_v2` 를 읽고 모델 입찰은 `market_bid_model`. 예전 자리는 모양을 바꾸지 않는다(예전 앱 · setup.sql 다시 올리기).
- 팔 수 없는 까닭은 `Sneaker.sellBlock`(서버 값) — 무료 뽑기 신발은 "이 신발로 N km 더 달리면 팔 수 있어요"(`kmRun` · `lockKm`, 폰 DB 판 21).

## 최신 — 러닝 중 음성 코치 설계 (2026-11-17, 설계만 · 앱 코드 없음)

[docs/run-voice-coaching-2026-11-17.md](docs/run-voice-coaching-2026-11-17.md) — 사용자 "규칙 기반 음성 안내로" · "아주 정교하고 실제로 도움이 되는 코칭 방법을 설계".
- 규칙은 `tools/coach_sim.py`(참조 구현 + 가상 러닝 24장면 검증, `--check` 는 기준 미달이면 1). 앱을 만들 때 `domain/runcoach/` 가 같은 값 · 같은 차례를 따른다 — 값을 바꾸면 둘을 함께.
- 뼈대: 적게 말한다(분당 0.2 ~ 0.4번 · 같은 조언 셋까지) · 내 기록이 기준(평소 페이스 · 가장 긴 러닝 · 가장 빠른 1km) · 오늘의 한 가지 · 러닝 뒤 코치 한마디.
- 페이스는 순간 값이 아니라 **목표로 달리는 가상의 러너와 벌어진 거리**로 판단한다(문턱 12m 또는 GPS 오차 × 4, 오차 12m 넘으면 페이스 조언 쉼) —
  제 페이스 러너에게 틀린 조언 2% 이하(1분 페이스 방식은 16 ~ 100%). 목표를 이룬 뒤에는 구간 · 경고만, 끼어드는 것은 과속 경고뿐.
- 고른 기본값(사용자 결정 전): 이어폰을 꽂았을 때만 저절로 · 수준 "보통" · 혼자 + 크루 · 목표는 없음이 기본(`goalKm` 기본 5km 는 목표로 치지 않는다) · 폰 음성 엔진.
  서버가 확인하기 전 SUP 금액은 말하지 않는다.
- 앱에서 함께 고칠 것: 걸음 센서 시각(`StepTracker`) · 구간 시각 사이에서 재기(`withAutoLaps`) · 위치 오차와 나이 넘기기 · 폰 DB 다음 판(22) `coach_runs` · 매니페스트(TTS · 진동).
  켜기 전에 debug · qa 그림자 모드로 실제 러닝에서 다시 잰다(문서 "검증의 한계").

## 최신 — 아이템 · 운영자 페이지 · 보상 이벤트 (2026-11-17, 서버 0106)

[docs/items-2026-11-17.md](docs/items-2026-11-17.md) — 사용자 "지급하고 싶을 때 쉽게 · 알람도" · "운영자 페이지 — 24시간 보상 2배도 설정" · "출발 전에 고르기 · 같은 종류는 겹쳐서 못 쓴다".
- 아이템 다섯: 러닝 부스터(× 1.5) · 보상 2배권 · 에너지 드링크(절반) — 출발 전에 골라 그 러닝 1회, 내구도 초기화권 · 하루 할당량(= 하루 에너지) 초기화권 — 바로.
  값은 `economy.item_kinds`(배포 없이 바꾼다). 가방 `public.user_items`, 앱 함수 `items_me` · `item_use` · `run_items_use(러닝 시작 시각, 종류들)`.
- 적립 배수는 겹치지 않는다 — 24시간 부스터 · 보상 이벤트 · 러닝 아이템 가운데 가장 큰 것 하나(`economy.run_reward_mult`, `record_session` 이 읽는다).
- 운영자 페이지 `web/ops/`(`stepupcrew.com/ops/`, 키 `economy.ops_key_new` — 해시만 서버에): 아이템 지급(이메일 · 최근 7일 · 전체, 푸시 `ITEM_GRANTED`, 되돌리기) ·
  보상 이벤트(`economy.reward_events` — 그 시간에 시작한 러닝 적립 × N, 푸시 `REWARD_EVENT`). 기록은 `admin_audit`.
- 앱: 하단 탭 다섯(러닝 · 함께 뛰기 · 신발 · **아이템** · 내 정보, 길 `bag` — `items` 는 예전 보관함 길) · `BagScreen.kt` · 출발 전 점검의 아이템 고르기
  (`ItemsRepository` — 러닝이 켜질 때 시작 시각에 묶고, 못 보냈으면 올리기 일꾼이 러닝보다 먼저). 푸시 링크 `bag` · `run`. 문서 "앱".
- push-send 는 손으로 다시 올려야 새 푸시가 간다. 아이템 종류를 더하면 서버 표 · 운영자 페이지 · 푸시 문구 · 앱(`ItemKind` · 그림 · `items_bag.xml`)을 함께.

## 최신 — 홈페이지 안드로이드 앱 출시 (2026-11-16)

사용자 "안드로이드 앱이 나와서 홍보" — 첫 화면 영상은 `LUJnsoDJ1QA`(StepUP Official VIDEO, 표지 `assets/video/stepup-official-poster.jpg`), 영상 양옆에 Google Play 받기 카드
(`.film-row` · `.dl-side` — 왼쪽 도시 러닝 사진 + 앱 아이콘, 오른쪽 바다 사진 + 신발, 900px 아래는 영상 밑 버튼 한 줄 `.dl-mobile`).
**출시 이벤트 = StepUP 타워 게임**(사용자 "안드로이드 이벤트 누르면 그 게임으로"): 머리 줄 칩 "NEW 안드로이드 앱 출시 이벤트"와 영상 위 흰 띠의 큰 버튼
(`.banner-game` "출시 이벤트 🏆 StepUP 타워 도전하기")이 `/lab-mja9j66fef/`(utm_campaign=android-launch)로 간다 — 아래 타워 · 레이스 절의 "홈 링크 없음"을 대신한다.
타워 화면은 간단하게(사용자 "미니멀하게 다"): 첫 화면 한 줄 설명 · 규칙 한 줄 · 오늘의 도전 / 연습 · 순위 · 🎨(꾸미기는 오른쪽 위), 자세한 규칙은 `#rules` 창, 닫기는 모두 ✕, 꾸미기는 넷(`LOOK_MAIN`) + 더 꾸미기, 테스트 딱지 없음.
띠 문구는 출시 문구, 띠의 사전 등록 버튼은 CSS 로 숨김(`waitlist.js` 가 그 버튼을 만지므로 지우지 않는다). 머리 줄 · 마지막 화면의 사전 등록은 그대로(타워 게임 참가가 사전 등록 이메일).

## 최신 — 11.11 워킹데이 당첨자 발표 (2026-11-11, 서버 0102)

[docs/walkingday-2026-11-05.md](docs/walkingday-2026-11-05.md) "당첨자 발표". 검토 페이지 「당첨자 발표」 탭에서 초대 순위 11명 채우기 → 「추첨하기」(브라우저에서 — 무릎보호대 5 걸음 인증자 1인 1회, 빼빼로 95 사이트 + SNS 응모권 가중) 또는 붙여 넣기 →
저장(`walkingday_review_winners_set` — 1인 1경품 · 자리 수 · 오타 이메일은 서버가 거절) → 「페이지에 발표」. 이벤트 페이지는 지금 순위 위에 가린 이메일 카드(`walkingday_winners`, 발표 전엔 아무것도 안 나간다).
끝나면 `truncate economy.walkingday_winners`(번호 지울 때 함께).

## 최신 — 미니게임 2판 StepUP 타워 (2026-11-13, 서버 0104) · 공개 전 내부 시험

[docs/game-tower-2026-11-13.md](docs/game-tower-2026-11-13.md) — `web/lab-mja9j66fef/`(아래 레이스를 대신한다, 홈 링크 없음 · noindex). 나선 탑 다섯 구간 · AI 15명(성격 일곱) · 혈압 요소(안쪽으로 떨어지면 아래층부터).
사전 등록 이메일로 참가, 하루 1판(00시 KST) + SNS 링크마다 1판(하루 3) · 부활 1번 · 8분. 점수는 `tower-rules.js` = 서버 `economy.tower_score`(`web/test/tower.test.js`).
효과는 `fx.js`(알갱이 · 고리 · 의성어 · 폭죽), 꾸밈은 `decor.js`(InstancedMesh — 관중 · 전구 · 관람차 …), 새 장애물(범퍼 · 끈적이 · 가속 · 점프대 · 벨트 · 대포 · 바나나)과 AI(궤적 예측 · 줄 고르기 · 실력)는 문서 "밤 2". 구간 길이 · 높이는 규칙과 같게 — 바꾸면 서버도.

## 최신 — 홈페이지 미니게임 StepUp 레이스 (2026-11-12, 서버 0103)

[docs/game-race-2026-11-12.md](docs/game-race-2026-11-12.md) — **공개 전 내부 시험**(2026-11-13 사용자 "홈페이지에 공개하면 안 돼, 우리만 테스트"): `web/lab-mja9j66fef/`(홈 링크 없음 · noindex · 예전 `/game` 은 홈으로 302). 공개하기 전에는 홈에 링크를 달지 않는다. 3D 장애물 달리기 예선 → 준결승 → 결승(고스트 사이 통과 자리),
결승 완주 시 세 판 합계가 순위표(`race_start` · `race_finish` · `race_board` — 서버 시각보다 짧은 기록 · 판별 하한 거절, 기기마다 최고 하나).
원본 `web/lab-mja9j66fef/game-src.js` → `npm run build:game` → `lab-mja9j66fef/game.js`(three.js 묶음, 둘 다 커밋). 난이도는 `ROUNDS[].cut`, `/lab-mja9j66fef/?dev` 로 물리를 화면 없이 돌려 본다.

## 최신 — 앱 UI 피드백 4건: 흰 테두리 · 함께 뛰기 흐름 · 수리 앞으로 · 아이템칸 (2026-11-07)

[docs/redesign/ui-feedback-2026-11-07.md](docs/redesign/ui-feedback-2026-11-07.md) — 클라이언트 "흰색 덧 붙히기 · 함께 뛰기 다시 플로우짜기(특히 이야기탭) · 신발 수리 버튼 앞으로 · 아이템칸 생긴다는 가정하에 디자인".

- 흰 테두리: `StepUpUi.kt` 의 `Modifier.stepUpRim(shape, RimKind)` 하나로 버튼 · 카드 · 사진 · 묶음 목록 · 원 버튼에 둘레 선(다크 흰색, 화이트블루는 흰 카드에 옅은 남색 · 파란 버튼과 사진은 흰 선을 안쪽으로). 끄기는 `StepUpUi.RimOn`.
  비활성은 선 없음, 이미 테두리가 있는 곳은 바꾸기(두 겹 금지). 구분선은 `hairline`(예전 #292E37) — 아래 10-25 절의 "번지는 빛 · 테두리 없이"를 대신한다.
- 함께 뛰기: 세그먼트 **모집 중 · 이야기 · 크루**, 머리 연필 없음(지도 · 크루 채팅 원 — 채팅은 크루가 있을 때만), 쓰기는 세그먼트마다 아래 주 버튼 하나(함께 뛰기 만들기 · 이야기 쓰기 · 크루 모집하기).
  이야기는 지역 줄 → 범위 시트(안에 지역 변경), 크루 밖 글쓰기는 번개러닝만(자유 · 꿀팁은 크루 게시판), 크루 칸 맨 위 "내 크루", 명함은 누르는 곳 하나.
- 신발 수리: 신발 탭 보조 버튼이 "수리하기"(그 화면에서 수리 시트, 보관함은 위 글자 탭), 상세는 내구도 칸 옆 "수리하기"와 신은 신발이 닳았을 때 주 버튼 "수리하기". 강조 기준 `repairUrgent`(출발 전 점검과 같음).
- 아이템칸: 신발 상세 능력치 아래 잠긴 칸 셋(`ShoeItemSlots.kt`, `ItemSlots.OPEN = false`) — 서버 계약 전이라 아이템 · 숫자 없음, 누르면 안내만.

## 최신 — 11.11 워킹데이 이벤트 페이지 (2026-11-05)

`web/11events/`(원본 iinging747/stepup-walkingday — 정적, 날짜별 화면 자동) → `stepupcrew.com/11events/`. CSP 는 `_headers` 의 `/11events*`(인라인 스크립트 없음).
운영은 [docs/walkingday-2026-11-05.md](docs/walkingday-2026-11-05.md) — 서버 0090. 걸음 인증은 아무 걸음 앱 캡처(사용자 결정) — 제출 `/11events/proof`
(사전 등록 이메일만), 승인 `/11events/review`(검토 키 — 해시만 서버에), 합계는 제출된 걸음(대기 + 승인, 거절은 빠짐 — 0095)의 합을 페이지가 15초마다 읽는다(`walkingday_total`, 응모권은 승인 기준), 응모권은 검토 페이지 명단 탭.
QR · UTM 저장은 지금 그대로(사용자 "일단 ok"). SNS 게시물은 링크로 받는다(0093, `/11events/proof#sns` → 검토 SNS 탭 → 추첨기 SNS 명단 CSV). 내 초대 링크는 `/11events/proof#invite`(0094 `walkingday_invite_link`), `?ref=` 로 온 사람은 주 버튼이 사전 등록 그대로. 응모권 기준은 하루 1,111걸음(0097, 사용자 "응모권 많이 뿌리는게 목적" — 11,111 아님).
GA4(2026-11-07, 속성 'StepUp 웹' `G-3D42E2KKG1` — 앱 Firebase 속성과 따로): 워킹데이 페이지는 `11events/ga.js`(측정 ID 는 `assets/config.js` 의 `ga`), 홈페이지는 `web/ga.js` —
같은 ID · 운영 주소에서만 · 광고 기능 끔(`csp.test.js` 가 맞대어 본다). 이벤트 · 인플루언서 utm 규칙은 운영 문서 "2026-11-07 GA4". 개인 정보는 GA 로 보내지 않는다.
걸음 인증은 오늘 날짜만(2026-11-09, 서버 0101, 사용자 "날짜 지난거 입력 못하게") — 끄기는 설정 `walkingday_today_only` = false, 켜고 끌 때 `proof.js` 의 날짜 잠금도 함께.
전화번호(2026-11-09, 서버 0100): 세 탭 모두 휴대폰 번호(꼭, 친구 초대는 동의 칸도) → `economy.walkingday_contacts`(이메일 · 번호, 덮어쓰지 않고 모두 남김), 검토 페이지 카드 · CSV 에 보인다. 끝나면 `truncate`.

## 최신 — 남은 앱 문제 수정 · 스토어 판 26 (2026-11-04, 서버 0089)

- 신발: 러닝 시작 때 신은 신발 id 를 적어(`WalkSessionState.startSneakerId` · 폰 DB 판 20 `sneakerServerId` · 체크포인트 판 5) `record_session(p_sneaker)` 로 보낸다 —
  서버는 내 것 · 쓸 수 있는 신발이면 그 신발로 닳게 하고 아니면 예전처럼 신은 신발(0089, 옛 앱 호출 그대로). 서버에 0089 가 없으면 앱은 p_sneaker 없이 한 번 더 보낸다.
- 0 SUP 까닭: `record_session` 이 `verdict_reason` 도 돌려주고 러닝 기록 상세에 한 줄(`domain/RunVerdictReason.kt`, 서버 문구와 정확히 맞대어 본다 — 서버 문구를 바꾸면 함께).
- 크루 러닝: 앱이 뒤에 있을 때 출발하면 "크루 러닝이 출발했어요" 알림(`service/CrewStartAlert.kt`, 채널 `crew_start`) — 눌러 2분 안에 오면 출발.
- 번개 공유 링크 `stepupcrew.com/f/<id>`(App Links · web/_redirects → invite.html). 거래소의 v3 신발은 모델이 정해지지 않은 줄이면 등급만(`MarketShoeLook`).
- 수리 창: 아직 안 올라간 러닝이 있으면 한 줄 알리고 올린 뒤 다시 견적. 뽑기 업적은 서버가 확인한 모든 뽑기.
- 스토어 versionCode 26 · versionName 1.18.0.

## 최신 — 출시 전 점검 2차 13건 (2026-11-03, 서버 0087 · 0088)

- 크루 러닝 출발은 대기실 화면이 아니라 늘 떠 있는 `PartyRunStartHost`(StepUpRoot)가 한다 — 채팅을 보고 있어도 출발, 준비한 동안 화면 켜 둠.
  서버 `party_start` 는 15초 넘게 대기실에 묻지 않은 준비 인원을 빼고 센다(유령 인원 · 보너스 부풀리기 막기, 빠진 사람은 그날 다시 할 수 있다).
  방 열림 푸시는 크루마다 30분에 한 번 · 링크 `room/<crew>/<party>`. 크루 결과 보상 내역은 못 받으면 다시 묻는다(`NetworkWatch.back`).
- 푸시 링크: 댓글 `post/<id>` · 번개 `flash/<id>` · 방 `room/…` 로 그 화면이 열린다(`InviteLinks.PushTarget`).
- 잠금 화면 · 알림 거리는 앱 화면과 같은 기준(`RunNotificationText.km`).
- 스토어 판: 판매가 빈 거래소로 가던 것(신발 `factionId` 'NONE' 그대로) · Rabby 지갑 `<queries>` 본 매니페스트 · "민팅" 대신 "No." · 뽑기 업적 ·
  준비 안 된 지갑(GIWA) 숨김 · 상급 뽑기 안내 숨김 · 지갑 창 제목 "지갑 연결". 거래소 오류는 4개 언어(`MarketRefusal`), 등록 · 입찰 안내.

## 최신 — 베타 점검 수정 19건 (2026-11-03, 서버 0086)

사용자 "버그 · 편의성 쭉 점검" → "수정 진행". 계정 · 크루 · 러닝 세 갈래.
- 계정: 로그아웃 때 푸시 끊기(`PushRegistrar.unregister` → `push_unregister` · 토큰 지움, 로그인 안 했으면 받은 푸시 버림) · 프로필 사진 지움(`UserPrefs.clearAvatar`) ·
  폰 이름이 비면 서버 이름을 받아 온다(`EconomySync.pullNickname`) · 코드가 통과되면 푸시 · 잔고 · 올리기를 다시(`AccessGate.afterGranted`), 403 `access_required` 면 문을 다시 묻는다 ·
  로그인 직후 첫 답까지 불러오는 중(stage 5, 8초까지) · 코드 창 "다시 확인" · 베타 코드 복사 안내 · 공유 글에 받는 링크 · 붙여 넣은 글에서 코드만 · 알림 설정에서 권한 바로 묻기.
- 크루: 번개 참가자는 집결 뒤 60분까지 대기실(`Post.lobbyOpen`) · 함께 뛰기 탭 돌아오면 다시 읽기 · 글 삭제 확인 · 번개 주인은 "번개 취소" ·
  가입 결과 푸시 `CREW_DECIDED` · 신청 푸시는 `crew/<id>/requests` 로 신청 목록 · 크루 공유에 링크 · 크루 이름 검색(`CrewRules.matches`) · 전국이면 "넓히기" 숨김.
- 러닝: 달리는 중 신발 교체 막음(서버가 올릴 때 신은 신발로 닳게 하므로 — 근본 수정은 시작 신발을 서버로 보내기, 남음) · 결과 내구도는 동기화 뒤까지 예상값 ·
  신발 점검은 아직 안 올라간 러닝만큼 빼서 보인다(보기만) · 다 닳은 신발에 수리 링크(점검 · 결과). 남은 것: 러닝마다 0 SUP 까닭 보이기(서버가 verdict_reason 을 돌려줘야 함).

## 최신 — 베타 피드백: 프로필 이름 서버로 · 크루 목록 전국 기본 (2026-11-03, 서버 0084 · 0085)

- 프로필 이름은 폰(`UserPrefs.setNickname`)과 서버 `profiles.display_name`(`profile_name_set` — 16자 · 비면 '러너') 둘 다에 적는다 — 크루장 · 명단 · 대기실이 서버 이름을 본다.
  저장 때 `EconomySync.pushNickname`, 못 보냈거나 예전에 바꾼 이름은 동기화 때(`nickname_synced` = "계정:이름"). 로그아웃 · 계정 바뀜이면 폰 이름을 비운다.
- 크루 목록은 전국이 기본(`CrewRules.NATIONWIDE` = 0, 키 `crew_radius_v2`) — 범위 시트에서 전국 · 1 · 3 · 5km(내 주변). 가까운 순 정렬은 전국에서도 거리로.
- 베타 초대 코드(0079 접속 코드)는 입장만. 친구 초대 코드(0036, 추천 100 SUP)는 베타 동안 멈춤(서버 0085 `invite_enabled` = false — 새 입력 거절,
  앱 설정 줄 숨김 `InviteFeature.ENABLED`). 이미 입력한 사람의 첫 러닝 적립은 그대로. 사용자 계획: 오픈 베타 · 정식 오픈 때 베타 코드를 없애고 친구 초대를 다시 켠다(둘 다 켜기).

## 최신 — 보안 네 겹: 사람 확인 · CSP · 보안 감시 · Dependabot (2026-11-02, 서버 0083)

[docs/security-2026-11-02.md](docs/security-2026-11-02.md) — 사용자 "보안 4배 — 전부 진행". 1층(계정 2단계 인증 등)은 사용자 몫.
- 대기 명단 사람 확인(Turnstile): 워커 `/v1/waitlist/register`(attester/src/waitlist.js) → `attester_waitlist_register`. 키 전에는 꺼짐 —
  `TURNSTILE_SITE_KEY`(web/waitlist.js) · 워커 비밀 `TURNSTILE_SECRET` 을 넣은 뒤 `waitlist_require_turnstile = true`. 규칙 · 횟수 · 저장은 `economy.waitlist_register_core` 하나.
- CSP(web/_headers 홈 · 초대 · 약관): 인라인 `<script>` 를 고치면 해시도 — `web/test/csp.test.js`.
- 보안 감시 `.github/workflows/security-watch.yml`(한 시간마다 읽기만 → 이슈 `security-alert`), Dependabot `.github/dependabot.yml`.

## 최신 — 대기 명단 등록 단단하게 (2026-11-02, 서버 0082)

11-02 13:05 ~ 13:20(KST) 공개 등록 API 에 자동 점검(example.com · XSS 흉내 @evil.com · 운영자 주소 대신 등록)이 들어왔다. 사용자 결정 "검사 강화 · 가짜와 운영자 주소 줄 지우기".
- 이메일 규칙은 서버 `economy.waitlist_email_ok` 하나(영문 · 숫자 · `._%+-`, 시험용 도메인 거절) — 홈페이지 `validEmail`(web/waitlist-core.mjs)이 같은 규칙. 바꾸면 둘을 함께.
- 같은 곳(IP 해시 — `economy.waitlist_client`, 원문 안 남김) 10분 6 · 하루 30(`waitlist_ip_per_10min` · `waitlist_ip_per_day`). 틀림 · 잦음은 예외 대신 `{ok:false, message}`(시도 기록이 남게).
- 그때 들어온 줄은 한 번만 지웠다(표시 `waitlist_cleanup_20261102` — 다시 배포해도 그 뒤 등록은 안 지운다).

## 최신 — 접속 코드 모달 · 로그인 약관 줄 빼기 · 스토어 판 지갑 주소 연결 (2026-10-29)

- 접속 코드는 로그인 화면(`LoginContent`) 위 가운데 모달(`AccessCodeScreen` → `AccessCodeModal`, 핸드오프 StepUP_AccessCode_Modal_HANDOFF):
  X · 뒤로 가기는 모달만 닫고(앱에 들이지 않는다), 구글 버튼이 다시 연다. 모달 밖 터치로는 안 닫힌다. 확인 중에 닫으면 답을 버리고 서버에 다시 묻는다.
- 로그인 화면 아래 동의 문구 · 이용약관 · 개인정보 링크는 뺐다(모든 판) — 약관은 설정 › 약관 및 정책.
- 스토어 판도 **지갑 주소 연결만** 한다(`BuildConfig.WALLET_LINK` · `WALLET_LINK_URL`, 사용자 "셀프 커스터디 지갑 연동은 VASP 아님") — 출금 · 보너스 뽑기 ·
  체인 줄 · 웹 지갑 페이지(`WALLET_URL`)는 여전히 `WEB3` 판에만. 지갑 화면은 연결 · 연결됨만(`linkOnly`). [docs/PLAY-STORE.md](docs/PLAY-STORE.md) 데이터 보안에 지갑 주소.

## 최신 — 접속 코드: 비공개 베타 · 공개 베타 (2026-10-28)

[docs/access-codes-2026-10-28.md](docs/access-codes-2026-10-28.md) — 서버 0079. 사용자 "closed beta 는 access code 로만 · open beta 는 사전예약자 중 일부 ·
코드 생성 · 차단 · 사용 여부 체크 · 코드로 들어온 사람이 하루 몇 개씩 발행".
- 문 `access_mode`: OPEN(기본) · CLOSED_BETA · OPEN_BETA — `admin_access_mode_set`. 막는 곳은 서버(`pgrst.db_pre_request = public.access_gate` → 403 `access_required`),
  앱은 막힌 사람에게 코드 입력 화면만(`AccessGate` · `ui/screens/access/AccessScreens.kt`, 루트 stage 4). 기존 · 운영자 계정도 코드를 넣어야 한다(0080 — LEGACY 줄은 남지만 문을 열지 않는다).
- 운영자 코드(1인용 · 공용 · 정한 글자) · 사용자 코드(하루 `access_user_daily_codes` 개, 설정 › 베타 초대 코드) · 차단(코드 · 계정, 그 코드로 들어온 사람까지) ·
  확인(`admin_access_codes` · `admin_access_code_uses` · `admin_access_users`) · 공개 베타 고르기(`admin_open_beta_invite`). SQL 은 문서에.
- 출시 때 Play Console › 앱 액세스에 심사용 코드를 적는다.
- 로그아웃(2026-10-28): 설정 › 연결된 계정의 계정 삭제 위 한 줄 → 확인 창. 이 폰의 로그인 · 사본만 지운다(`forgetAccountOnPhone` — 계정 삭제와 같은 정리).

## 최신 — 체인 기록 넓히기 · 나중에 대조 · 개인정보 최소 · 출시 점검 (2026-10-27)

사용자 "거래수가중요해" → "검증은 나중에 필요하면 대조할 정도" → "해시에 개인정보 유출 없게" → "안드로이드 출시에 심혈". 서버 0078, 표는 [docs/온체인-거래-발생-지도.md](docs/온체인-거래-발생-지도.md).

- EAS 기록 15종: 뽑기 · 씨앗 약속 · 씨앗 공개(`draw_verify` 로 누구나 다시 굴림) · 강화 · 수리 · 포인트 · 거래 · 러닝 정산 · 신발 주행 · 크루 러닝 · 함께 출발 명단 · 번개 출석 · 크루 가입 ·
  하루 걸음(목표 달성 여부만) · 앱 출석(`chain_check_in`, `EconomySync` 가 하루 한 번). 러닝 증명은 200m · 하루 30(전 1km · 10). 관문이 아니다 — 실패해도 그 일은 된다.
- 대조: 기록마다 `dataHash`(소금 친 원본 해시). 소금 · 스냅숏은 `economy.chain_audit`(서버만) → `admin_chain_audit(job)` + `attester/scripts/audit.mjs <uid> <hash>`.
  재료 봉인은 `economy.activity_seal`(다시 가져가도 같은 재료), 재료 키 = 워커 `ACTIVITY_SCHEMAS` 칸 이름(`attester/test/activity.test.js` 가 맞대어 본다).
- 개인정보: 사람 가명 = 사람마다 비밀값(`economy.person_pseudonym` · `chain_person_keys` — 계정 삭제 때 cascade 로 지워 다시 못 잇는다), 받는 사람 기본 비움
  (체인 기록 화면 "내 지갑으로 기록 받기" — `chain_recipient_set`), 보내는 시각 10 ~ 60분 무작위 늦춤. 개인정보처리방침 · Play Data safety("공유: 예")에 밝혔다.
- 새 종류를 더하면 셋(서버 `activity_kinds` · `activity_payload`, 워커 스키마, 앱 `ChainRecord.Kind` · `ca_kind_*`)을 함께 고친다. 보내는 속도 상한은 워커 2분마다 20건(하루 약 14,000 — 2026-10-28 유료 요금제로 3 → 20).
- 출시 점검(별도 검토): 테스트 APK 스스로 업데이트 코드는 `src/selfUpdate`(debug · qa)로 옮기고 스토어 판은 `src/noSelfUpdate` 빈 자리, `TestUpdatesTest` 는 `src/testDebug`.
  지갑 앱 찾기(`<queries>`)는 `src/debug` 매니페스트로, Firebase 광고 서비스 권한(`ACCESS_ADSERVICES_*`)은 뺐다. 신발 기록의 체인 등록 줄 · "민팅" 제목은 `WEB3` 판에만.
  러닝 서비스가 전경으로 못 오르면 죽지 않고 알린다(`run_start_failed`). 남은 일은 [docs/PLAY-STORE.md](docs/PLAY-STORE.md).

## 최신 — 러닝 시작 메뉴: 혼자 러닝 · 크루만 열기 (2026-10-27)

사용자 "러닝챌린지 · 다이어트 모드 등 크루달리기랑 혼자 러닝 빼면 비활성화 · 준비중 · 자물쇠". 러닝 시작 메뉴(`RunStartMenuContent`)의
러닝 챌린지 · 추천 코스 · 다이어트 카드와 AI 코칭 줄은 흐리게 · "준비 중" · 자물쇠(`RunModeCard(locked)` · `RunLinkRow(locked)`),
누르면 곧 열린다는 안내만. 다시 열려면 `RunFlowScreens.kt` 의 `RunMenuLocks` 값을 끈다(화면 · 길은 그대로 있다). 내 러닝 기록 · 이번 주 순위는 그대로 열려 있다.
홈의 "오늘 걸음" 첫 글자가 둥근 모서리에 잘리던 것도 고쳤다(걸음 칸을 모서리로 자르지 않는다).

## 최신 — 혼자 러닝 버튼 셋 · 신발 교체 · 번개 제목 (2026-10-26)

- 자유 러닝 중 아래 버튼은 셋 — 일시정지(멈춤이면 다시 달리기) · **정지**(늘 보이고 1초 길게, 짧게 누르면 안내) · 화면 잠금(멈춘 동안도). 위 10-25 절의 "멈춘 뒤 길게"를 대신한다.
- 러닝 전 화면의 취소 · 러닝 시작은 같은 크기 두 버튼.
- 자유러닝 신발 점검: 이 신발로 출발 · 돌아가기 · 신발 교체. 신발 교체나 신발 그림 → 내 신발 목록(`ShoePickList`), 신발은 **1초 꾹** 눌러야 신는다
  (줄이 차오르고 작아짐 · 진동, 착용은 신발 탭과 같은 `equipOnServer` — 확인된 뒤 점검으로).
- 번개러닝 글쓰기: 제목은 기본 제목 10개(`res/values*/flash_titles.xml`) 중 하나로 채워 두고 안 쳐도 올린다. 거리 · 정원 줄은 이름이 쪼개지지 않게(좁으면 위아래).

## 최신 — 잠금 화면 가운데 러닝 현황 (2026-10-26)

사용자(솔라나 시커) "홀드화면(잠금화면) 중앙에 현재 현황이 딱 뜨는 그런 거" — [docs/run-lock-screen-2026-10-18.md](docs/run-lock-screen-2026-10-18.md) "2026-10-26".
달리는 동안 어느 화면에서 잠가도 `LockRunHost`(`ui/screens/walk/LockRunPanel.kt`, `MainActivity` 맨 위 겹)가 앱을 덮고 가운데에 시간 · 거리 · 페이스 ·
일시정지 / 다시 달리기 · "잠금 해제하고 앱 열기"를 보인다(덮개 아래 앱 화면은 못 만진다). 다른 앱 뒤에서 잠갔으면 러닝 알림을 누르면 같은 현황(`LockRunActivity`
— 매니페스트 `showWhenLocked` 는 이 화면에만). 아래 10-19 · 10-18 절의 "러닝 화면에 있을 때만 잠금 위에"는 이것이 대신한다.

## 최신 — 구글 플레이 출시 최적화 2차: 앱 안 확률 표 · 언어 네 개 (2026-10-26)

[docs/PLAY-STORE.md](docs/PLAY-STORE.md) "2026-10-26 출시 최적화" — 뽑기 › 기회 받는 방법 시트에 등급별 확률(서버 0077 `draw_odds`, 뽑기가 쓰는 표
`economy.draw_weight_v3` 그대로 — 앱이 확률을 지어내지 않는다, 못 읽으면 줄을 두지 않는다). `androidResources.localeFilters` 로 ko · en · ja · zh 만 담는다
(`resources.arsc` 3.7 → 1.4MB). 언어를 더하면 여기와 `xml/locales_config` 를 함께 고친다. 스토어 설명 · 콘텐츠 등급 답을 함께 뛰기 탭에 맞췄다.

## 최신 — 화면 글자 정리: 개발자 말투 · 데모 · 준비 중 빼기 (2026-10-26)

사용자 "데모 — 없음 같은 AI가 쓴 듯한 · 개발자들이 볼 만한 말 다 빼". 화면 글자(4개 언어)에서 "서버 확인 중 · 서버에 닿지 못했어요 ·
서버가 거절했습니다 · ○○ 없음" 같은 말을 사람 말("기록 확인 중 · 연결이 끊겼어요 · 요청을 처리하지 못했어요")로 바꿨다.
쓸 수 없는 "준비 중" 자리는 숨긴다 — 건강 앱 연결 줄은 뺐고, 주간 포인트 순위 판은 서버가 순위를 줄 때만(`LeaderboardSource.offersPoints`).
설정의 데모 모드 줄은 이미 켠 사람에게만 보인다(새로 켤 수 없다). **`scripts/check-copy.py`(Build APK 에서 돈다)** 가 서버 · 데모 · 디버그 ·
테스트 · 준비 중을 막는다 — 보이지 않는 글자(디버그 · 테스트 빌드 · 데모 모드 · 블록체인 판 · 옛 화면)와 개인정보 안내만 ALLOW 에 까닭과 함께 둔다.

## 최신 — 함께 뛰기 한 탭: 크루 러닝 · 번개러닝 · 번개도 원격 (2026-10-26)

사용자 "크루달리기 · 번개달리기를 한 탭에서 다 해결" · "번개도 원격 허용" · "무조건 다 보너스". 서버 0076.

- 하단 탭은 러닝(혼자 달리기만) · **함께 뛰기**(예전 커뮤니티 — 길 `community` 그대로, 둘째 자리) · 신발 · 내 정보. 러닝 탭의 자유 · 크루 세그먼트와 크루 카드는 뺐다.
- 함께 뛰기 첫 칸 "모집 중"(`community/TogetherRooms.kt`): 내가 든 **모든 크루**의 열린 대기실(크루 이름을 붙여) + 앞으로 열릴 번개러닝을 한 목록에,
  아래 주 버튼 "함께 뛰기 만들기" → 누구와(크루마다 · 누구나) → 크루는 새 대기실(`crewRoomNew`), 누구나는 번개러닝 모집 글(`postCompose(flash = true)`).
  러닝 시작 메뉴의 "크루"도 이 칸으로 온다(`TogetherFocus`). 크루 대기실 목록 · 만들기 길은 함께 뛰기 탭 밑이다.
- 번개러닝도 원격 뛰기(`posts.run_mode`, `post_create(p_run_mode)`) — 만나는 곳 · 좌표 없이 같은 시각에 각자 자리에서. 번개 대기실은 글의 방식 · 시각 · 장소를 받는다(트리거).
- 번개 · 옛 방의 인원 보너스(`economy.party_for`)도 함께 출발한 사람 모두(출발 명단). 예전 규칙은 설정 `party_bonus_rule = 'NEAR'`. 한 방 한 번 · +10%/명 · 상한은 그대로.
- 대기실(크루 · 번개) 위에 "같이 뛰기 · 오늘 19:00 · ○○에서 만나요 · 정원 N명" 한 줄(`party_state` 의 0074 칸). 꽉 찬 방 안내는 20명이라고 박지 않는다.

## 휴대폰 인증 없음 · 크루 러닝 인원 보너스는 함께 출발한 모두 (2026-10-26)

사용자 "휴대폰 인증은 아예 빼 · 파생 효과도" · "무조건 다 보너스(테스트가 먼저)". 서버 0075 — `crew_run_bonus_eval` 은 같은 대기실에서 같은 크루로
출발한 사람 모두를 센다(거리 500m · 2분 · 원격 · 인증 상관없이, 판정 시각만 출발 2분 뒤 그대로). 예전 규칙은 설정 `crew_bonus_rule = 'NEAR'`.
앱의 인증 안내(대기실 시트 · 줄, 크루 홈 줄, 달리는 중 "인증 전")와 `verified` · `requiresPhone` 값은 지웠다. 아래 10-22 절의 "휴대폰 인증 · 500m"는 이것이 대신한다.
**다음 작업 순서**(사용자 "편한 순서로"): ② 함께 뛰기 탭 통합(위 절, 끝남) → ③ 개발자 말투 · 데모 · 준비 중 문구 정리(위 절, 끝남) → ④ 구글 플레이 출시 최적화(위 절, 끝남).

## 최신 — 전체 디자인 자료: 러닝 탭 세그먼트 · 블랙 테마 · 하단 탭 넷 · 원격 뛰기 (2026-10-26)

[docs/redesign/all-2026-10-26/README.md](docs/redesign/all-2026-10-26/README.md) — 시안 9장 × 블랙 · 화이트, 사람 없는 배경 15장(`assets/run_photos`). 서버 0074.

- 하단 탭은 넷(~~러닝 · 신발 · 커뮤니티 · 내 정보~~ → 위 절: 러닝 · 함께 뛰기 · 신발 · 내 정보). 뽑기는 신발 탭 위 글자 탭(내 신발 · 신발 보관함 · 뽑기, `ShoeSection.DRAW`) — 길은 `Routes.MYSTERY_BOX` 그대로.
- ~~러닝 탭은 자유 러닝 · 크루 러닝 세그먼트~~ → 위 절: 러닝 탭은 혼자 달리기만, 대기실 목록 · 만들기는 함께 뛰기 탭. 신발 카드는 없다(시작 때 신발 점검).
- 새 대기실: 코스 직접 설정 / 자유 러닝 · 같이 뛰기 / 원격 뛰기 · 만나는 곳 · 출발 시각(안내일 뿐 — 출발은 진행자) · 정원 2 ~ 20(`party_create_v2`). ~~원격 방은 인원 보너스 없음~~ — 0075 부터 원격 방도 함께 출발한 모두.
- 어두운 테마는 검정(#000 · 카드 #101216 · 버튼 파랑 #3D74FA). 기본 테마는 그대로. 자유 러닝 중은 지도가 위 · 2 × 2 숫자 · 알약 둘(종료는 일시정지 뒤 길게).
- 내 정보는 사진 틀 거리 카드 · 다음 기록 막대 · 최근 러닝 줄. 번개러닝 글쓰기는 출발 시각 칸(5분 ~ 24시간).

## 최신 — UI 최종 통합안: 다크블루 · 화이트블루 · 29장 (2026-10-25)

[docs/redesign/ui-final-2026-10-25/README.md](docs/redesign/ui-final-2026-10-25/README.md) — 사용자 "이거 확인하고 적용해"(Drive `StepUp_UI_최종_개발전달_2026-10-25.zip` · HANDOFF.md). 화면 대응표 · 시안과 다른 점.

- 글꼴 Paperlogy(한글) · Saira Condensed SemiBold(숫자). 색은 `tokens.json` 다크블루(#070B14) / 화이트블루(#F2F2F7) — 테마 기본값은 **기기 설정 따르기**(아래 10-21 절을 대신한다).
- 새 화면 부품은 `ui/components/StepUpUi.kt`(`StepUpUi.colors` · `StepUpUi.type`) — 주 버튼은 한 화면에 하나, 끝 원 화살표는 다음으로 넘어가는 주 버튼만. 알약 칩 줄 · 번지는 빛 · 체크 칩 · 이니셜 아바타 · 색 배지(LIVE 도 배지 아님)는 두지 않는다.
- 장소 사진은 `assets/backgrounds/<장소 id>/` · `spots.json` · `ui/background/RunBackground.kt`, 날씨 효과(비 · 눈)는 `WeatherEffect.kt` — 앱이 받는 Open-Meteo 코드로(새 요청 없음).
- 홈: 오늘 걸음 · 자주 달리는 곳 사진 카드 · 둥근 시작(신발 줄 없음 — 신발 이름 바꾸기는 신발 탭 연필). 신발 탭: `ShoeCard`(만지면 기울기 · 빛).
- 자유 러닝은 확인창 없이 **멈춘 뒤 1초 길게 눌러 종료**(`run-finish`, 짧게 누르면 안내). 다른 러닝(챌린지 · 코스 · 다이어트 · 크루)은 예전 확인창 그대로.
- 다크 테마의 러닝 키트 지도(`runTone`)는 어둡게 누른 지도, 크루 달리기 지도는 두 테마 모두 밝은 지도 그대로(아래 10-17 절의 "두 테마 모두 밝은 거리 지도"를 대신한다).

## 최신 — 크루 러닝 3번 듀얼 레일: 열린 대기실 · 코스 · 사진 터널 · 출발 연출 (2026-10-24)

[docs/redesign/crew-run-2026-10-24/README.md](docs/redesign/crew-run-2026-10-24/README.md) — 핸드오프 원문 · 70장면 대응표 · 서버 0072 계약 · 시안과 다른 점 · 남은 결정.

- 크루 홈(01) → 열린 대기실(02, `Routes.CREW_ROOMS` — 보기만) → 러닝 만들기(03 · 06, 이름 · 코스 사본 · 요청 번호) → 고른 방(`Routes.room` — `party_join`).
  크루 러닝 입구는 모두 `Routes.crewRun(crewId)`(들어가 있던 방이면 그 방). 예전 `party_open`(저절로 아무 방)은 번개러닝 · 옛 앱만.
- 대기실은 듀얼 레일(`CrewDualRail.kt` `DualRailFrame` — 크루원 · 내 신발 같은 틀) + **사진 터널은 대기실에만**(`LobbyTunnelBackdrop`, 시트 · 카운트다운 · 뒤 · 모션 줄이기면 멈춤).
  나머지 크루 화면은 앱 바탕(`crewColors`). 시트는 `LobbySheetFrame`(#08243A).
- 진행자만 코스를 바꾼다(`party_set_course` — 판 + 1, 진행자 뺀 준비 풀림 D14), 출발은 판을 맞대고(`party_start_rev`). 크루 방 출발 시각은 6초 뒤(번개 4초 그대로).
- 진행자를 넘길 때는 오늘 아직 달리지 않은 사람이 먼저(`economy.party_next_host`). 방 열기 · 들어가기는 사람 · 크루 자물쇠로 하나씩(두 방 · 21명 막기).
- 출발 연출은 앱 맨 위 겹(`CrewLaunchOverlay`)이 서버 출발 시각에 맞춰 자유 러닝 무대(`FreeRunStartStage`)를 그린다 — 출발 시각 = 펼침 시작, 러닝이 켜진 뒤 러닝 화면을 펼친다.
- 경제(공용 체력 · 하루 한 번 · 2분 인원 · 레벨 · 보상)는 0071 그대로다. 37(출발 위치 500m 시트) · 67(자정 안내 화면)은 없다 — 결정 D03 · D05.
- 코스는 골라도 되고 안 골라도 된다(서버 0073, 사용자 "코스를 내가 선택할 수가 없네") — 안 고르면 자유 코스(각자 원하는 길로), 진행자는 대기실에서
  코스를 넣고 · 바꾸고 · 뺀다(판 + 1 · D14). 코스 고르기 맨 위 "지도에서 직접 그리기"(`CrewCourseDraw.kt` — 점 사이 곧은 줄 · 100km · 300점).
  출발 판은 `PartyState.courseRev`(코스가 없어도 판이 있다). 크루장에게는 크루 홈 두 곳(커뮤니티 · 러닝 탭)에 "크루 관리" 줄이 보인다.

## 최신 — 홈페이지 5인 출시 QA 2차 (2026-10-23)

[docs/web/PERSONA-QA-2026-10.md](docs/web/PERSONA-QA-2026-10.md) — 1차 58건 중 41 고쳐짐 · 12 일부 · 5 그대로(모두 사용자 결정: 운영 주체 · 보너스 · 약관 범위 ·
보관 기한 · 약관의 앱 버전). 2차 새 문제는 고쳤다: 스크립트 오기 전 누름은 `index.html` 머리 스크립트가 기억했다가 이어서 한다(`__stepupEarly`),
X 주소 변형 · 끝 마침표 이메일은 보내기 전에 맞춘다, 법적 문서 `#en` 은 머리까지 영어(`web/legal.js`). 웹 파일을 고치면 `?v=` 판을 함께 올린다.

## 최신 — 크루 러닝 공용 체력: 크루 · 하루 하나 · 출발 때 기여 · 실시간 차감 · 2분 인증 인원 (2026-10-22)

[docs/crew-run-shared-pool-2026-10-22.md](docs/crew-run-shared-pool-2026-10-22.md) — 핸드오프 [redesign/crew-run-2026-10-22](docs/redesign/crew-run-2026-10-22/). 서버 0071 이 0070 의 무리별 몫을 대신한다.

- 공용 체력 = 크루 · 한국 날짜에 하나(`economy.crew_day_pools`). 출발할 때 고른 신발 최대 내구도 × (1 + 6% × (Lv − 1))를 더한다. 0/0 · 다 씀 모두 출발 가능.
- 달리는 동안 앱이 5초마다 `crew_run_beat` — 서버가 달리는 사람마다 분당 5 를 뺀다(일시정지 · 30초 무소식은 안 씀). 0 이면 적립만 멈추고(빈 구간은 적립 제외) 기록은 계속.
- 하루 한 번(크루를 바꿔도, `economy.crew_runs` 출발 때 기록). 다른 참가자가 모두 끝내면 마지막 한 사람도 서버가 끝낸다. 크루 방은 준비 2명 이상이어야 출발.
- 보상 = B + B × 인원 + B × 레벨(B = 고른 신발 스냅샷). 인원 = ~~같은 크루 · 출발 무렵과 2분 뒤 500m · 휴대폰 인증~~ 2026-10-26 부터 함께 출발한 같은 크루원 모두(서버 0075, 위 절).
- 열린 결정 D01 ~ D13 은 문서에 고른 값 · 까닭. 앱 값은 `crew_run_today` · `crew_run_room` · `crew_run_beat` · `crew_run_result`(`domain/CrewRunPool.kt`).

## 최신 — 기본 테마는 밝게 (2026-10-21)

사용자 "앱이 전체적으로 어둡다 · 밝고 힘나게" → "밝은 테마를 기본으로". ~~테마를 고른 적 없는 사람은 밝은 테마(`ThemeMode.of` 기본 `LIGHT`)다.~~ 2026-10-25 통합안부터 기본은 기기 설정 따르기(`SYSTEM`)다.
이미 어둡게를 고른 사람은 그대로, 어두운 테마는 설정 › 테마에 그대로 있다. 밝은 테마의 버튼 파랑은 `#1166FF`(예전 `#145BFF`, 흰 글자 4.8:1),
표면 위 파란 글자(`voltText`)는 그대로다. 연출 화면(자유러닝 시작 등)의 남색은 그대로다. 아래 절들의 "기본 모습은 블랙 · 블루"는 이것이 대신한다.

## 최신 — 구글 플레이 출시 준비: 목표 API 36 · 스토어 문서 대조 (2026-10-20)

사용자 "스토어 올리기 적합해?" → "바로 시작해". compileSdk · targetSdk 36(AGP 8.10.1) — Play 는 2026-08-31 부터 API 36 을 요구한다.
[docs/PLAY-STORE.md](docs/PLAY-STORE.md) · `web/privacy.html` · `web/terms.html` 을 지금 앱에 맞췄다(크루 이미지 · 채팅 사진은 서버, 프로필 사진은 기기 ·
지갑 주소 · 테스트넷 출금 · 뽑기 확률 표 · MapTiler · OpenStreetMap · Open-Meteo). iOS 앱은 없다.
**스토어 판(release)은 블록체인 기능을 뺀다** — `BuildConfig.WEB3 = false`(사용자 "니가 제일 좋게": Play 는 2026-01-28 부터 한국 암호화폐 지갑 · 거래소 앱에
가상자산사업자 신고 수리를 요구한다). 지갑 연결 · WEB3 줄 · 상급 뽑기 · GIWA 안내가 숨고, 테스트 APK(qa) · debug 는 그대로다. 새 지갑 · 체인 화면을 만들면 `WEB3` 로 가린다.
쓰지 않던 "메인넷 1:1" 출금 글은 지웠고 "NFT 마켓 · 도감"은 "신발 마켓 · 도감".
**사용자 결정 전**: SUP · 지갑 · 출금 · 거래소 법률 검토, 위치정보 사업 신고.

## 최신 — 러닝 · 게임 메카닉 전수 점검 · 잠금 화면 러닝 화면 (2026-10-19)

[docs/mechanics-audit-2026-10-19.md](docs/mechanics-audit-2026-10-19.md) — 사용자 "메카닉적으로 문제 없는지 전수조사해봐 심각해" · "홀드화면일때 우리 트랙". 서버 0068.

- 거리는 한 기준(`domain/RunDistance.kt` — 경로 100m 부터 GPS, 그 전엔 걸음, 서버 `record_session` 과 같다): 기록 · 랩 · 목표 · 코스 완주 · 크루.
  자유 러닝 화면 · 결과만 예전 설계대로 GPS 거리다.
- 신발은 거리 · 걸음과 상관없이 달린 1분마다 5(사용자 결정 "걍 1분마다 5 닳는거로 바꿔 어떤식으로든간데") — 걸음 0 러닝도 올려 닳는다. 무효는 안 닳는다.
- 내구도를 다 쓰면(`WalkSessionState.shoeWornOut` — 시작할 때 남은 내구도 × 12초) 러닝 중 띠 · 소리 · 잠금화면 알림, 결과에 한 줄.
- 과속 멈춤은 오차 20m 넘는 위치의 튐을 세지 않고, 처음 자리에서 150m 멀어져야 멈춘다(`RunOverspeed.counts` · `STOP_MIN_TRAVEL_M`).
- 잠금 화면: 달리는 동안 러닝 화면에 있으면 그 화면이 잠금 위에 뜬다(`setShowWhenLocked`). 끝나거나 다른 화면이면 끈다.
- 무효 러닝은 내 기록 합계 · 코칭에서 뺀다. 부스터는 시작 시각, 에너지는 시작한 날(한국 시간)로 본다.
- 서버 0068: 상한에 걸린 러닝은 받은 몫만큼만 에너지를 쓴다 · 목표 보너스는 러닝이 시작한 날로(`goal_claim_day`, 최근 7일) ·
  초대 적립은 경로가 받쳐 준 러닝만 · 코칭 거리는 인정 거리(`distance_meters`, 랩 수 ±1).
- 러닝은 서버가 기록하면 바로 확인 — 크루 표시 · 코스 기록은 따로 다시 낸다. 올리기 일꾼은 `APPEND_OR_REPLACE`.
- 자유 러닝은 고른 코스를 풀고 시작한다(녹화한 코스는 저장만). 연속 일수는 끊기면 0, 업적은 계정별 최고값으로 연다. 페이스는 50m 부터.

## 최신 — 테스트 APK 스스로 업데이트 (2026-10-18, 설치 방식 2026-10-23)

[docs/test-apk-self-update-2026-10-18.md](docs/test-apk-self-update-2026-10-18.md) — 사용자 "원격 업데이트 안 된다 · 근본적으로" →
"업데이트 떠도 '앱이 설치되지 않았습니다'". 앱이 설치 세션으로 직접 깔면 Play 프로텍트가 가로막아(에뮬레이터 검사 `tools/self_update_e2e.sh`)
**안드로이드 설치 화면을 연다**(앱 안 "업데이트" → 설치 화면 "업데이트"). 돌아왔는데 옛 버전이면 까닭과 다시 열기. 받기 전에 서명 · 판을 맞대어 본다.
돌아올 때마다 · 1시간마다(일꾼은 미리 받아 두고 알림만) 묻는다. 허용 설정을 열고 돌아오면 이어서, 러닝 중에는 안 한다. 확인 없는 설치는 하지 않는다. debug · qa 에만.

## 최신 — 러닝 중 잠금화면 알림 (2026-10-18)

[docs/run-lock-screen-2026-10-18.md](docs/run-lock-screen-2026-10-18.md) — `service/RunNotification.kt`. 안드로이드 16 이상은 실시간 알림(상태 표시줄 칩 · 잠금화면 고정),
그 아래는 남색 우리 모양(로고 · 흐르는 시계 · 거리 · 페이스). 채널 `run_live`(소리 없음, 잠금화면 공개). 버튼은 일시정지 / 다시 달리기만.
달리는 동안 러닝 화면에 있으면 잠금 화면 위에 러닝 화면(지도 · 경로)이 뜬다(2026-10-19 "홀드화면일때 우리 트랙" — `setShowWhenLocked`,
러닝이 끝나거나 다른 화면으로 가면 끈다 — [mechanics-audit-2026-10-19](docs/mechanics-audit-2026-10-19.md)).
자유러닝 시작의 StepUP 옆 "!"는 뺐다(세로 줄로 보였다).

## 최신 — WEB3 지갑 연결: 지갑을 고르면 바로 (2026-10-18)

[docs/wallet-connect-2026-10-18.md](docs/wallet-connect-2026-10-18.md) — 사용자 "WalletConnect 빼고 OKX · MetaMask · Rabby · GIWA(맨 위, 준비 중)".
"지갑 연결"은 모두 지갑 고르기 창(`WalletConnectSheet`) → 그 지갑 앱 안 브라우저로 연결 페이지(`wallet.html?l=계정.확인번호`, 10분 · 한 번) → 서명 한 번.
로그인 토큰은 지갑 앱에 넘기지 않는다. 처음 연결은 2단계 인증 없음(서버 0067), 붙은 지갑 바꾸기 · 꺼내기는 그대로 2단계 인증.

## 최신 — 러닝 탭 다듬기: 신발 이름 · 이름 바꾸기 · 등급 내구도 바 · 천장 조명 · 하단 탭 (2026-10-17)

[docs/run-tab-polish-2026-10-17.md](docs/run-tab-polish-2026-10-17.md) — 내 정보 › 챌린지 줄은 뺐다. 러닝 탭 신발 이름은 세 줄 · 글자 줄이기로 늘 다 보이고(챌린지 기록은 챌린지 화면 맨 아래 링크),
연필로 이름을 바꾼다(이 폰에만 — `domain/ShoeNickname.kt`, `shoeName()` 이 따른다). 내구도 바는 `TierDurabilityBar`(등급별 꾸밈).
천장 조명은 6초마다 번쩍인다. 하단 탭은 아이콘 · 글자를 타일 가운데에, 고른 글자는 크고 굵게.

## 최신 — 신발 경제 3판: 6등급 · 속성 없음 · 새 뽑기 · 신발 · SUP 초기화 (2026-10-17)

[docs/shoe-economy-v3-2026-10-17.md](docs/shoe-economy-v3-2026-10-17.md) — 서버 0066. 사용자 "6등급 · 속성은 아예 없애 · 상급에선 일반 안 나옴 · 걍 싹다 초기화".

- 등급: 일반 < 레어 < 에픽 < 레전더리 < 레드라인 < 피니시(레드라인 · 피니시도 서버 등급). 속성은 없다(새 신발 `faction = 'NONE'`, 앱 화면에서 지웠다).
- 내구도: 새 신발로 약 10 · 15 · 25 · 40 · 60 · 90분(1분마다 5 그대로). 가득 수리 일반 : 피니시 = 1 : 4. 적립은 0% 일 때만 없고 다 닳은 뒤 걸음은 뺀다.
- 효율 2 ~ 4 … 45 ~ 55%(에픽부터 급히), 최대 레벨 20 · 20 · 26 · 27 · 28 · 30. 레벨업 포인트 = 레벨 기본값(1 · 2 · 3 · 5 · 15 · 30 · 60 · 100 · 150) × 등급 배수(×1 · 2 · 3 · 4 · 6 · 8).
  포인트 1 = 효율 +0.05%p · 착화감 +0.02%p · 최대 내구도 + 기본의 1%. 잠재능력은 보류.
- 뽑기: 무료 일반 79 · 레어 20 · 에픽 1%, 상급 레어 60 · 에픽 28 · 레전더리 9 · 레드라인 2.5 · 피니시 0.5%(일반 없음).
- 배포 때 한 번 모든 신발 · SUP · 거래소 · 뽑기 지급을 비우고(백업 `economy.backup_0066_*`) 첫 신발 + 무료 뽑기 10 을 다시 준다.
- 신발 체인(발행 · 꺼내기 · 보너스 발행)은 멈췄다(`shoe_chain_enabled = false`) — v3 컨트랙트는 4등급만 안다. 아래 "신발 내구도 · 레벨업" 절의 수치는 이것이 대신한다.

## 최신 — 러닝 지도 30% 가까이 · 두 손가락 확대 (2026-10-17)

사용자: "초반에 내가 어디 있는지 지도 자체가 너무 뒤로 가 있다. 확대 축소 가능하게, 지금 기본보다 약 30% 더 확대".

- 지금 자리를 따라가는 지도(`LiveRouteMap(follow = true)` — 러닝 중 · 3-2-1 · 러닝 전 내 자리)는 기본 줌보다 30% 크게 그린다(`MapTiles.FOLLOW_MAGNIFY`).
  타일은 정수 줌만 있어 남는 배율은 타일을 크게 그려 맞춘다(`TilePlan.of(magnify =)`). 따라가지 않는 지도는 그대로다.
- 러닝 중 지도(자유 · 크루 · 챌린지 · 코스 · 다이어트)는 두 손가락으로 매끄럽게 확대 · 축소(`pinchZoom`, 기본의 0.25 ~ 3배).
  한 손가락 끌기는 지도가 받지 않는다 — 화면 스크롤 · 지금 자리 따라가기가 그대로다. 배율은 러닝마다 처음부터.

## 최신 — 러닝 중 과속: 경고 · 30초면 멈춤 · 패널티 (2026-10-17)

[docs/run-overspeed-stop-2026-10-17.md](docs/run-overspeed-stop-2026-10-17.md) — 사용자 결정 "빼는게 아니라 애초에 경고가 뜨고 그렇게 유지되면 강제로 멈추고 패널티".

- 25km/h 를 넘는 구간이 오면 바로 "속도가 너무 빨라요 · N초 뒤 러닝이 멈춰요" + 소리 · 진동 한 번. 과속 구간끼리 30초를 채우면 러닝을 멈춘다(`domain/RunOverspeed.kt`).
  - 사람 속도 구간이 오면 풀리고, 과속 구간 사이가 10초를 넘으면 처음부터 센다(GPS 튐 두 번으로 멈추지 않게).
  - 오차 20m 넘는 위치의 튐은 세지 않고, 30초 동안 처음 자리에서 150m 멀어지지 않았으면 다시 센다(2026-10-19 — 실내 와이파이 튐으로 서 있는 사람이 멈추던 것).
- 패널티(사용자가 고른 값): 신은 신발의 **남은 내구도 절반**(서버 0065 `report_overspeed_stop`) · 멈춘 때부터 **10분 러닝 금지**(앱 · 서버 둘 다 — 그 사이 시작한 러닝은 서버가 무효).
- 멈춘 러닝의 보상 규칙은 그대로다(판정 FLAGGED + 까닭). 신고는 올리기 일꾼이 러닝보다 먼저 보낸다.

## 최신 — 인터넷 끊김 대처 · 러닝 위치 되살리기 · 밝은 지도 · 내구도 1분마다 5 (2026-10-17)

[docs/offline-audit-2026-10-17.md](docs/offline-audit-2026-10-17.md) — 끊김 점검(서버 0062 요청 번호 · 0063 진행자 넘기기)과 러닝 위치 · 지도 · 내구도.

- 인터넷이 다시 되면(`core/NetworkWatch.kt`) 잔고 · 푸시 주소 · 러닝 올리기 · 지도 타일 · 크루 연결을 한 번 다시 한다.
- 거래소 · 수리 · 글 · 댓글 · 좋아요는 응답을 잃고 다시 보내도 한 번만 된다(요청 번호 · 바라는 상태).
- 지도는 못 받은 타일을 15초마다, 다시 연결되면 바로 다시 받는다. 빈 칸 격자는 지도와 함께 흐른다.
- 러닝 · 크루 달리기 지도는 두 테마 모두 밝은 거리 지도다(`BrightMapFilter`, 사용자 "네이버지도처럼"). 예전 남색 지도 필터는 뺐다. (2026-10-25 통합안부터 다크 테마의 러닝 키트 지도는 밝은 타일 + 30% 막, 크루 달리기 지도는 그대로 밝다.)
- 내구도는 달린 1분마다 5(서버 0064, 아래 신발 내구도 절).

## 최신 — 러닝 중 오래 서 있음 경고 (2026-10-17)

[docs/run-still-warning-2026-10-17.md](docs/run-still-warning-2026-10-17.md) — 사용자 결정 "너무 오래 멈췄을 때(30초 이상) 경고" = 러너가 30초 넘게 서 있음.

- 걸음이 늘지도, 경로 점이 생기지도 않은 지 30초(`domain/RunStill.kt`)면 "30초 넘게 멈춰 있어요" + 일시정지 알약 · 소리 · 진동 한 번 · 알림 글.
- 저절로 멈추지 않는다. 다시 움직이면 사라진다. 기록 · 보상 규칙은 그대로다(화면 표시용 `WalkSessionState.still`).

## 최신 — 자유러닝 시작 플로 (2026-10-17 전달본)

[docs/redesign/free-run-start-2026-10-17/README.md](docs/redesign/free-run-start-2026-10-17/README.md) — 자유러닝 시작 → 신발 점검 → 출발 확인 → 3·2·1·0 → StepUP! → 가운데에서 지도 펼침 → 기존 자유 러닝 화면.

- 코드는 `ui/screens/walk/FreeRunStart.kt`(점검창 · 시퀀스 · 그림)와 `FreeRunStartMotion.kt`(시간표 — `FreeRunStartMotionTest`)다.
- 러닝 기록은 펼침 진입(3,760ms)에서 한 번 시작한다(`WalkSessionService.start`). 러닝이 실제로 켜진 것을 본 뒤에 펼친다.
  - 거절되면(`WalkSessionService.startRefusals`) 연출을 거둔다.
- 카운트다운에는 숫자와 원호만 둔다. 제목 · 안내 · 취소 버튼은 다시 넣지 않는다. 기록 전 그만두기는 시스템 뒤로 가기다.
- 로고는 `logo_wordmark_white.png`(전달본 원본)다. 숫자는 앱 숫자 서체(Pretendard)를 이탤릭으로 쓴다.
- 이전 자유 러닝 3-2-1(지도 · 바로 시작 | 취소)은 이것으로 바꿨다. 챌린지 · 코스 · 다이어트의 3-2-1 은 그대로다.

## 최신 — 리더보드 3판 (2026-10-17 전달본)

[docs/redesign/leaderboard-2026-10-17/README.md](docs/redesign/leaderboard-2026-10-17/README.md) — 데이터 연결 · 집계 규칙 · 시안과 다른 점 · 상태 · 남은 결정.

- 입구는 러닝 시작 메뉴(U01)의 "이번 주 순위" 링크. 길은 예전 순위표 길 `Routes.RANKING` 그대로(하단 탭 없이). 판은 크루 · 개인 · 포인트 글자 탭.
- 크루 · 개인은 서버 0061 `weekly_crew_board` · `weekly_runner_board`(읽기만) — 한국 시간 월요일 주, 무효 제외, 지난주 순위는 같은 규칙으로 다시 센다.
  - 크루는 **크루 전체 거리**다(2026-10-17 사용자 결정 — 시안의 "1인당 거리"는 쓰지 않는다).
- 주간 포인트는 앱에서 **'준비 중'으로만** 둔다(2026-10-17 사용자 결정 — 판 · ⓘ 모두). 대기 명단 추천 순위는 앱에 넣지 않는다. P 정의는 아직 없다.
  - 개발용 고정 값은 기기 검사에서만(`LeaderboardSource` 경계).
- 배경은 `LeaderboardBackdrop`(Canvas · 시계 하나) — 시트 · 뒤에 있을 때 · 모션 줄이기면 멈춘다.

## 최신 — 크루 달리기 정리 (2026-10-16 크루 전달본)

[docs/redesign/crew-run-2026-10-16/README.md](docs/redesign/crew-run-2026-10-16/README.md) — 화면별 변경 · CR02~CR21 검수표 · 시안과 다른 점 · 바뀐 사용자 동작.

- 대기실은 3 × 2 참가자 그리드다(6명이 넘으면 다섯 + "+N" · 전체 명단 시트). 준비 글 · 점 · 출발 버튼은 같은 명단에서 센다.
- 달리는 중 · 결과 지도는 밝은 실제 지도다(`CrewBrightMap`). 예상 보상 · 내구도는 러닝 정보(E03)에서 본다.
- 결과는 기록 · 경로가 주인공이다. 보상은 한 줄, 설명은 시트에 있다.
- 공유는 실제 보낼 글(과 그림) 미리보기 하나다. 미리보기와 보내는 글은 같은 값이다. 보내는 동안 뒤로 가기를 막는다.
- CR02 · CR16 은 보기 전용 방 조회 API 가 없어 서버 의존 · 미완료다.

## 최신 — AI 코칭 16장 (2026-10-16)

[docs/redesign/ai-coaching-2026-10/README.md](docs/redesign/ai-coaching-2026-10/README.md) — 데이터 감사 · 화면표 · 시안과 다른 점 · 남은 결정.

- 입구는 러닝 시작 메뉴(U01)의 "내 러닝 기록" 아래 링크 한 줄. 길은 `ui/screens/coaching/CoachingNavGraph.kt`(러닝 탭 아래 `Header.Detail`).
- 숫자 · 그래프 · 주간 띠 · 계획은 코드가 만든다(`domain/coaching/`). AI 는 서버가 만든 근거 · 후보 id 를 고르고 짧은 글만 쓴다.
- 서버 0060(`coaching` 스키마)은 운영 DB 에 적용됐다(#92). 운영 설정은 모두 꺼진 쪽이 기본이다(`coaching_enabled=false` · 공급자 `none` · 한도 · 예산 비어 있음).
  - 꺼진 동안 앱은 AI 칸을 숨기고 동의를 받지 않는다(`shownAi` — 서버는 동의를 먼저 보아 `no_consent` 로 답한다).
- 일꾼 `coach-worker/` 는 모의 공급자만, 배포하지 않았다. 실제 공급자 · 모델 · 예산 · 동의 문구는 사용자 결정 전이다.
- 앱 ↔ 서버 규칙 값은 `scripts/check-coaching-twin.py` 가 맞대어 본다(CI).
- 폰 DB 판 19 — 러닝의 랩(`walk_sessions.laps`)을 남긴다. 그 전 기록은 구간 분석을 하지 않는다.

## 최신 — 러닝 위치: 바로 시작 · 구글 지도처럼 (2026-10-16)

[docs/gps-required-2026-10-16.md](docs/gps-required-2026-10-16.md) — 사용자 결정 "GPS 안 되면 못 하는 거야" → "그래 다 되도록 만들어" →
"위치를 찾고 있어요 이거 안 뜨게(다른 M2E 는 바로 된다)".

- 시작 차례: 권한(걸음 · **정확한** 위치 · 알림) → 위치 기능 → 3-2-1. **위치를 기다리는 화면은 없다** — 3-2-1 동안 찾기 시작해 러닝이 이어 받는다.
  - 위치 안내의 보조 버튼은 모두 "취소"다. 위치 권한 · 위치 기능 없이 시간만 기록하는 시작 길은 없다.
- **러닝 탭에 들어오자마자 위치를 미리 잡는다**(2026-10-17 "키자마자 잡아두면 안되냐 · 시작하고도 10초를 찾는다" — `core/LocationWarmup.kt`).
  - 러닝 탭과 그 아래 화면 · 크루 · 번개 대기실이 화면에 있을 때만 켠다(`AppChromePolicy.warmsLocation`). 다른 탭 · 앱이 뒤로 가면 끈다.
  - 러닝을 시작할 때 미리 잡은 위치가 10초 안 · 오차 50m 안이면 내 점 · GPS 표시를 바로 채운다(`TrackFixRule.warmFix`).
    경로 · 거리 · 속도 기준점에는 넣지 않는다(러닝 전 위치다).
- **러닝 중 "찾는 중" 글은 없다**(2026-10-17 "그 글도 없애고 지도 앱처럼") — 띠 · 상태 · 지도 안내 · "경로 대기 중" 모두.
  위치를 모르는 동안은 아무것도 적지 않고(거리 0.00), 문제(위치 꺼짐 · 대략 위치 · 신호 약함 · 무효 · 속도 오류)만 알린다.
- 위치는 구글 지도와 같은 엔진(구글 위치 서비스, `play-services-location` · `core/LocationEngine.kt`)으로 받는다 — 러닝 서비스도, 지도의 지금 자리(`rememberCurrentLocation`)도.
  - 실내는 와이파이, 밖은 GPS 로 잡힌다. 첫 위치를 정확한 것이 올 때까지 미루지 않는다(`setWaitForAccurateLocation(false)`).
  - Play 서비스가 없으면 휴대폰 위치(GPS · 와이파이 · 기지국)로 받는다. 러닝 중 15초 동안 구글 위치 서비스가 조용하면 휴대폰 위치도 함께 듣는다.
    - 처음뿐 아니라 러닝 중간에도 그렇다. 30초 넘게 조용하면 요청을 다시 건다(2026-10-17 "다시 연결했을 때 내 위치를 못 잡는다").
- 달리는 중 위성 GPS 가 아닌 위치는 `TrackFixRule` 로 거른다(실내 떨림이 거리로 쌓이지 않게):
  - 오차 50m 초과는 지도만 옮긴다 — 위성 GPS 도(2026-10-19). 경로가 생긴 뒤에는 30초 동안 내 점도 옮기지 않는다(`keepHere`).
    흐린 위치는 "잡힌 위치"로 치지 않는다(흐린 위치만 오면 "신호 약함").
  - 기준점에서 오차 두 배보다 멀리 움직였을 때만 경로 · 거리에 넣는다.
  - 훨씬 정확한 위치가 기준점 오차 안에 오면 기준점만 옮긴다(`REFINE`). 와이파이로 시작해 거리가 0 에 멈춰 있던 것("인식도 잘 안 되고 느려").
- 지도의 내 점 · 화살표는 경로 끝이 아니라 받은 위치(`here`)를 따라간다(`RunTrack.liveHead` · `LiveRouteMap(head =)`). 선은 거리로 센 점까지만.
  - 경로 끝에서 100m 넘게 먼 위치는 경로 끝이 20초 넘게 그대로일 때만 따라간다(튄 위치는 거르고, 흐린 위치만 오는 동안 묶이지 않게).
- 크루 · 번개 대기실의 "준비"는 걸음 권한 · 정확한 위치 · 위치 기능만 보고 바로 된다(위치 확인 시트는 없앴다).
- 고친 버그 둘:
  - 달리는 중 권한을 허용해도 위치가 켜지지 않던 것(서비스가 5초마다 확인).
  - Android 10 이하의 위치 리스너 AbstractMethodError.
- 서버 `no_gps_daily_cap` 은 그대로다(0 으로 할지는 사용자 결정).

## 신발 내구도 · 레벨업은 스탯 포인트로 (2026-10-16, 닳는 양 2026-10-17)

[docs/shoe-durability-points-2026-10-16.md](docs/shoe-durability-points-2026-10-16.md) — 서버 0059 · 0064.

- 모든 신발이 **달린 1분마다 5** 닳는다(2026-10-17 사용자 결정 "1분마다 5씩", 서버 0064 — 0059 의 100m 마다 10 을 대신한다).
  - 멈춘 시간은 빼고 1분 미만 나머지는 닳지 않는다. 무효 러닝은 닳지 않는다.
  - 거리 · 걸음과 상관없다 — 걸음 0 · 거리 0 러닝도 켜 둔 시간만큼 닳는다(2026-10-19, 서버 0068 · 앱은 시간이 있는 걸음 0 러닝도 올린다).
  - 달리는 중 화면의 %는 같은 규칙으로 미리 줄여 보인다(`durabilityPercentDuringRun`). 끝난 뒤에는 서버 값이다.
- 최대 내구도는 신발마다 다르다(일반 1,000 ~ 1,500 … 레전더리 2,500 ~ 3,000, 뽑기 씨앗으로 고른다).
- 적립 배율 · 체인 · 예전 앱은 남은 비율(`durability_pts`)을 읽는다.
- 수리 1점 값은 예전 1km 값 ÷ 100 이라 1km 를 고치는 값이 같다.
- 레벨업은 효율 · 착화감을 저절로 올리지 않고 포인트 4 를 준다.
  - 포인트 1 = 효율 +0.25%p · 착화감 +0.1%p · 최대 내구도 +100.
  - 나눈 포인트는 되돌리지 않는다(`sneaker_points_set`).
- 이미 강화한 신발은 (레벨 − 1) × 4 를 효율 2 · 착화감 2 로 나눠 둔 채로 받았고 한 번 다시 나눌 수 있다.
- 앱 화면:
  - 신발 화면은 "남은 / 최대", 러닝 · 홈의 작은 칸은 "%"로 보인다.
  - 포인트는 상세 알림 · ⋯ 의 시트에서 나눈다.
- 폰 DB 판 18 이다.
- 체인 메타데이터는 포인트를 서버(`attester_sneaker_points`)에서 받아 더한다.

## 최신 — 사용자 결정 반영: 테스트 APK 서명 · CI 권한 · versionCode · 탈퇴한 사용자 채팅 (2026-10-16)

[점검 문서의 "사용자 결정 반영"](docs/점검-2026-10-15-크루-통신-인프라.md) — 테스트 APK 는 main 의 `publish` 작업에서만 저장소 밖 키
(GitHub 환경 `test-apk` 의 `TEST_APK_SIGNING_KEY`)로 다시 서명한다(옛 공개 키 → 새 키 서명 이력 — [TEST-APK-SIGNING.md](docs/TEST-APK-SIGNING.md),
키가 없으면 전처럼 debug 키). Build APK 의 빌드 작업은 읽기 권한만. 테스트 · 개발 빌드 versionCode = 데이터베이스 판 × 1000 + 스토어 versionCode
(데이터베이스 판을 올리면 저절로 오른다). 계정을 지운 사람의 크루 채팅 · 공지는 남고 작성자만 빈다(0058, "탈퇴한 사용자"). attester-logs 는 main 에서 손으로만.

## 최신 — 점검 수정: 크루 통신 · 채팅 · 기록 · 배포 (2026-10-15)

[docs/점검-2026-10-15-크루-통신-인프라.md](docs/점검-2026-10-15-크루-통신-인프라.md) — 함께 달리기 보너스 인원은 서버가 "출발 2분 뒤에도 서로
200m 안" 무리로 센다(0056 `party_near`) — 진행자가 방을 닫아도 남은 사람은 자기 러닝이 끝날 때까지 위치를 보낸다. 결제 요청 번호(0057, 앱은 10분 지난 번호를 버린다). 러닝 저장본은 판 4(걸음 흉내 표시). CI 는 `ubuntu-24.04` 고정 · 배포 전 서버 검사. 사용자 결정이 필요한 것(테스트 서명 키 공개 등)은 문서 끝.

## 최신 — 크루 달리기 v2 (2026-10-15)

크루 달리기(진입 · 대기실 · 동시 출발 · 지도 중심 라이브 · 결과 · 공유)는 남색 시안 전달본을 따른다 —
[docs/redesign/crew-run-v2-2026-10/README.md](docs/redesign/crew-run-v2-2026-10/README.md)(CR02~CR21 표 · 크루 출처 경계 · 진행자/참가자 종료).
러닝의 모임(크루 · 번개)은 출발할 때 `WalkSessionState.group` 에 적고 저장본 판 3 에 남긴다 — 화면은 이 값으로 크루 달리기를 가르고,
정산 인원(`partySize`) · 크루 순위(`partyCrewId`)는 따로 둔다. 번개러닝 대기실 · 화면은 예전 모습 그대로다. CR02 · CR16 은 보기 전용 방 조회 API 가 없어 미완료.

## 최신 — 파란 톤 통합 전달본 v4 (2026-10-03)

러닝 밖 화면(뽑기 · 신발 상세 · 수리 · 강화 · 커뮤니티 · 글쓰기 · 크루 · 내 정보 · 설정 · 시작 · 알림, 421개 상태)도 같은 남색 · 전기 파랑으로 맞췄다 —
[docs/redesign/blue-v4-2026-10/IMPLEMENTATION-REPORT.md](docs/redesign/blue-v4-2026-10/IMPLEMENTATION-REPORT.md)
(기능별 상태 · 지시서와 서버가 다른 곳 · [화면별 기록표](docs/redesign/blue-v4-2026-10/implementation-tracker.csv)). 강화는 재료 신발 3개를 태우는 확률 강화다(서버 0054 `forge_*`, 규칙 forge-v2 — 재료는 대상보다 한 등급 아래 이상, [shoe-upgrade-2026-10-01](docs/redesign/shoe-upgrade-2026-10-01/README.md)). 수리는 현행 서버 계약을 따른다.
지갑 WAL01~16 · 알림 NOT27~46 은 시안이 없어 기존 그대로다.

## 최신 — 러닝 전체 리메이크 · 신발 색감 (2026-10-02)

러닝 화면(109장)과 신발 탭 색감은 남색 · 전기 파랑 전달본을 따른다 — [docs/redesign/run-remake-2026-10/README.md](docs/redesign/run-remake-2026-10/README.md)
(화면 대응표 · 시안과 다른 점 · 남은 연동 · [체크리스트](docs/redesign/run-remake-2026-10/implementation-checklist.csv)). 러닝 화면 부품은
`ui/components/RunStyle.kt` 를 쓴다. 신발 탭은 구조를 그대로 두고 색감 · 조명만 바꿨다. 아래 S2 안내 중 러닝 화면의 색 · 버튼은 이 전달본이 대신한다.

## 시작점 — S2 리디자인 인계 (2026-09-26)

**먼저 [docs/redesign/s2/README.md](docs/redesign/s2/README.md)를 읽는다.** 사용자는 Figma S2를 최대한 따르되 확보한 이미지로 진행하도록 했다. 기획·원본 이미지·45개 프레임/34개 기존 경로 대응표·6개 비교 화면을 레포에 정리했다. S2 앱 구현은 진행 중이다 — 진행 상황은 [docs/redesign/s2/STATUS.md](docs/redesign/s2/STATUS.md). 하단 탭은 다섯(러닝 · 신발 · 뽑기 · 커뮤니티 · 내 정보)이고 뽑기는 가운데 독립 탭이다. 신발 탭 위 글자 탭은 내 신발 · 신발 보관함이다(2026-09-28 사용자 결정 — [docs/redesign/shoes-ui-2026-09-28](docs/redesign/shoes-ui-2026-09-28/README.md), 2026-09-26의 '탭 넷 · 뽑기는 신발 안'을 대신한다). 새 작업 범위는 현재 사용자 지시를 따른다.

이하 캐릭터/옷장 관련 설명은 이전 방향의 기록이다. 현행 캐릭터 없는 앱과 새 S2 방향을 우선한다. 비교 HTML은 최종 승인된 설계나 실제 APK 결과가 아니며, 서비스 정책·상품 매핑은 임의로 만들지 않는다. 이미지 내보내기 제한 때문에 사용자에게 동일한 추출을 다시 요구하지 않는다.

## 브랜드 · 디자인 자산 (먼저 볼 것)

- **로고 · 앱 아이콘 · 태그라인**: `design/brand/` — 어느 바탕에 어느 파일을 쓰는지는
  `design/brand/README.md`. 로고가 필요하면 이 파일을 그대로 쓰고, 새로 그리거나
  글꼴로 흉내 내지 않는다. 바탕색에 맞는 변형을 고른다(어두운 바탕 → on-navy,
  밝은 바탕 → on-white, 파란 면 → white-on-blue, 정사각형 → app-icon).
- **캐릭터 이름**: 남자 **RUNO(루노)**, 여자 **LUMI(루미)** — 가이드와 앱 그림 목록은
  `design/characters/README.md`.
- **화면 시안 · 캐릭터**: `design/blue-black-2026-09/` — 적용 현황과 추가로 필요한
  그림은 `적용-현황.md`. 캐릭터는 `res/drawable-nodpi/avatar_*.webp` 완성 그림을 쓰고
  코드로 다시 그리지 않는다. 성별·착장을 속여 보이지 않는다(`domain/AvatarArt.kt`).
- **RUNO · LUMI 장비(신발 52 · 의상 5)**: `design/equipment/`(RUNO) · `design/equipment/lumi/`(LUMI,
  모자는 의상을 따라감 — CAP_RULES.md) — 원본 시트 · 카탈로그 · 요청문,
  앱에 들어간 그림과 남은 그림은 `runtime-assets.md`. 그림을 더하면
  `python3 tools/gen_avatar_res.py` 로 `AvatarArtRes.kt` 를 다시 만든다.

## 지킬 것

- `strideup.db` · `strideup_prefs` 이름은 바꾸지 않는다(설치된 사용자 데이터가 사라진다).
- Supabase service_role / `sb_secret_*` 키, Google `GOCSPX-*` 비밀, 지갑 개인키 ·
  시드는 앱 코드 · 번들 · 로그 · 커밋에 넣지 않는다.
- 서버가 확인하기 전에는 "적립 완료"라고 쓰지 않는다. 예시 숫자로 실제 잔액을
  꾸미지 않는다. 데모는 데모 모드에서만, "데모" 표시와 함께.
- 사용자가 말한 범위 밖은 고치지 않는다. 디자인 작업에서는 기능이 바뀌지 않게 한다.
- 화면은 시안대로 버튼을 적게 — 한 화면에 큰 주 행동 하나. 덜 쓰는 기능은 지우지 말고
  내 정보 › 설정 같은 안쪽으로 옮긴다.

## 빌드 · 확인

로컬에 안드로이드 SDK 가 없다. 푸시하면 CI 가 돈다. (클라우드 세션에 `/opt/android-sdk` 가 있으면 푸시 전에
`ANDROID_HOME=/opt/android-sdk LC_ALL=C.UTF-8 ./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug` —
한글 테스트 이름 때문에 UTF-8 로케일이 필요하다. 서버는 `supabase/tests/run.sh`, 워커는 `cd attester && npm test`.)

- Build APK → `test-apk` 사전 배포에 `StepUp-test.apk`(최적화 빌드 `qa` — R8 · 디버그 불가. debug 를 올리면 화면 전체가 끊긴다.
  서명은 main 의 publish 작업이 테스트 전용 키로 다시 한다 — 작업 브랜치 아티팩트는 debug 키) — **main 에 들어간 것만** 올라간다.
  작업 브랜치의 APK 는 그 실행의 Actions 아티팩트에서 받는다. 사용자가 받을 앱은 main 에 합쳐야 바뀐다.
- 같은 test-apk 사전 배포에 `StepUp-store-preview.apk`(2026-10-27) — 스토어 판(release) 설정 그대로(지갑 · 체인 없음, 자가 업데이트 · 시험 도구 없음)에
  서명 · versionCode 만 테스트 APK 와 같아(빌드 종류 `storePreview`) 깔린 테스트 앱 위에 덮어 깔고 되돌린다. 구글 제출용은 이것이 아니라 Release AAB.
- Experience QA → 에뮬레이터 캡처를 `qa-captures` 사전 배포의 `qa-review.zip` 에.
- 로컬에서 되는 검사: `python3 scripts/check-strings.py`,
  `python3 tools/check_experience_assets.py`.
