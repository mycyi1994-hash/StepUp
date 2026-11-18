# 접속 코드 — 비공개 베타 · 공개 베타 (2026-10-28, 서버 0079)

사용자 결정: "closed beta 는 only access code 로만 · open beta 는 사전예약한 사람들 중 일부 · 코드 생성 · 차단 · 사용 여부 다 체크 ·
코드로 들어온 사람이 하루에 몇 개씩 코드 발행".

## 문 세 가지

| 문(`access_mode`) | 누가 들어오나 |
|---|---|
| `OPEN`(배포 때 기본) | 누구나 |
| `CLOSED_BETA` | 접속 권한이 있는 계정만 — 코드를 넣은 계정 · 운영자가 준 계정. **기존 계정 · 운영자 계정도 코드를 넣어야 한다**(0080, 사용자 "기존 직원도 무조건 다시 입력" — LEGACY 줄은 남지만 문을 열지 않는다) |
| `OPEN_BETA` | 위 + 운영자가 고른 사전 예약 이메일로 구글 로그인한 계정(처음 앱을 열 때 저절로) |

- **막는 곳은 서버다.** PostgREST 요청 전 함수(`pgrst.db_pre_request = public.access_gate`)가 로그인한 사람의 모든 API 요청을 보고,
  권한이 없으면 403 `access_required`. 앱 화면만 막지 않는다(앱을 고쳐도 서버 기능을 못 쓴다).
- 막지 않는 것: 로그인 안 한 요청(홈페이지 사전 등록 · 뉴스), 운영자 · 어테스터 계정, 접속 코드 함수(`access_*`), 계정 삭제(`account_delete`).
- 앱: 막힌 사람은 로그인 뒤 **접속 코드 입력 화면**만 본다(`ui/screens/access/AccessScreens.kt` — `AccessGate` 가 상태를 들고, 앱으로 돌아올 때마다 다시 묻는다).
  예전 둘러보기(로그인 없음)는 "구글로 로그인"부터.

## 코드

- **운영자 코드** — 1인용 여러 개, 또는 여러 명이 쓰는 공용(행사), 만료일 · 메모.
- **사용자 코드** — 권한이 있는 사람(코드로 들어옴 · 운영자가 줌 — `access_issue_sources`, 0080 부터 LEGACY 는 빠짐)이 하루
  `access_user_daily_codes`(~~기본 3~~ 0113 부터 1)개 · 지난 7일에 `access_user_weekly_codes`(3)개(아래 "2026-11-18"), 1인용,
  `access_user_code_days`(기본 14)일 뒤 만료. 앱: 설정 › **베타 초대 코드**(만들 수 있는 사람에게만 보인다).
  내가 만든 코드가 쓰였는지만 보이고, 누가 썼는지는 운영자만 본다.
- 코드는 8자(헷갈리는 0 · O · 1 · I · L 없음), 보일 때 `XXXX-XXXX`. 입력은 대소문자 · 띄어쓰기 · `-` 를 가리지 않는다.
- 틀린 코드는 한 사람 1시간 10번까지. 자기가 만든 코드는 자기가 쓰지 못한다. 차단된 계정은 코드로 되살리지 못한다(운영자만).

## 운영 — Supabase 대시보드 › SQL Editor 에 그대로 붙여 넣는다

```sql
-- 문 바꾸기
select admin_access_mode_set('CLOSED_BETA');   -- 비공개 베타 시작
select admin_access_mode_set('OPEN_BETA');     -- 공개 베타
select admin_access_mode_set('OPEN');          -- 정식 출시(누구나)

-- 코드 만들기
select * from admin_access_codes_create(20);                     -- 1인용 20개(만료 없음)
select * from admin_access_codes_create(5, 1, 7, '직원 지인');    -- 1인용 5개, 7일 뒤 만료, 메모
select admin_access_code_named('STEPUPBETA', 100, 30, '런칭 행사'); -- 정한 글자 공용 코드 100명 · 30일

-- 확인
select * from admin_access_codes();                    -- 전체(상태: OPEN · USED · EXPIRED · REVOKED, 사용 수 / 정원, 만든 사람)
select * from admin_access_codes('USER');              -- 사용자가 만든 코드만
select * from admin_access_codes(null, 'OPEN');        -- 아직 쓸 수 있는 것만
select * from admin_access_code_uses('ABCD-EFGH');     -- 그 코드를 누가 언제 썼나(+ 그 사람이 만든 코드 수)
select * from admin_access_users();                    -- 권한 명단: 들어온 길 · 쓴 코드 · 그 코드를 만든 사람 · 만든 코드 · 쓰인 수

-- 차단
select admin_access_code_revoke('ABCD-EFGH', '유출');              -- 남은 자리만 막는다
select admin_access_code_revoke('ABCD-EFGH', '유출', true);        -- + 그 코드로 들어온 사람 권한 · 그 사람들이 만든 안 쓴 코드까지
select admin_access_revoke_user('someone@gmail.com', '부정 사용'); -- 계정 하나(+ 그 사람의 안 쓴 코드)
select admin_access_grant('staff@gmail.com');                     -- 계정 하나에 직접 권한(먼저 앱에서 한 번 로그인해야 한다)

-- 공개 베타 — 사전 예약자 중 고르기
select admin_open_beta_invite(200);                                -- 먼저 등록한 순서로 200명
select admin_open_beta_invite(null, array['a@x.com', 'b@y.com']);  -- 정한 이메일만(사전 예약한 이메일이어야 한다)
select * from admin_open_beta_list();                             -- 고른 사람 · 들어왔는가

-- 사용자 코드 하루 개수 · 지난 7일 개수 · 만료일 바꾸기(0 이면 그 한도로 만들 수 없다)
select admin_economy_set('access_user_daily_codes', '5');
select admin_economy_set('access_user_weekly_codes', '10');
select admin_economy_set('access_user_code_days', '7');
```

운영자 함수는 앱의 운영자 계정(`app_admins`) 또는 SQL Editor(로그인 없이 postgres)에서만 된다. 모든 바꿈은 `admin_audit` 에 남는다.

## 2026-11-18 — 하루 1개 · 7일에 3개 (서버 0113)

사용자 "3개씩 나오던 코드를 하루에 1개, 일주일에 3개까지만 나올 수 있게 해".

- **하루** — `access_user_daily_codes` 3 → 1. 지금처럼 한국 날짜(00시 KST)로 센다.
  배포가 바꾸는 것은 한 번뿐이고(표시 `access_daily_codes_0113`), 그때 값이 0079 기본값 3 그대로일 때만이다 — 운영자가 이미 다른 값으로
  바꿨거나, 바뀐 뒤 일부러 3 으로 돌렸으면 다시 배포해도 덮지 않는다.
- **일주일** — 새 설정 `access_user_weekly_codes` = 3. 달력의 주(월요일)가 아니라 **지금부터 거슬러 168시간** 안에 만든 사용자 코드를 센다
  (굴러가는 7일 — 주가 바뀌는 날 몰아 만들 수 없다). 가장 오래된 코드가 168시간을 넘기면 한 자리가 다시 열린다.
  세는 것은 하루 한도처럼 만든 코드 모두(차단된 코드도).
- 이미 만든 코드는 그대로 쓸 수 있다(만료 · 차단 규칙도 그대로). 운영자 코드(`admin_access_codes_create` · `admin_access_code_named`)는 한도와 상관없다.
- 서버: `access_status()` 에 `weekly_limit` · `issued_week` · `left_today` · `left_week` · `left` · `next_at`(다 만들었을 때 다음 코드를
  만들 수 있는 시각 — 하루가 막으면 내일 00시, 7일이 막으면 그 자리가 열리는 시각, 둘 다면 늦은 쪽) · `limited_by`(DAY · WEEK)를 더했다(jsonb, 예전 칸 그대로).
  새 앱은 `access_code_issue_v2()` — 한도 거절은 23514(hint `daily_limit` · `weekly_limit`, detail 다음 시각), 권한 없음은 42501(hint `not_allowed`).
  예전 앱의 `access_code_issue()` 는 지금처럼 답 안의 `{ error }`(새 갈래 `weekly_limit` — 예전 앱은 "지금은 초대 코드를 만들 수 없어요"로 보인다).
- 앱: 베타 초대 코드 화면 첫 줄이 "오늘 n개 · 이번 7일 n개 남았어요(하루 1개 · 7일에 3개)", 다 만들었으면 아래에 "다음 코드는 11월 21일 11:04부터
  만들 수 있어요"(폰 시간대). 0113 이 없는 서버면 예전 줄(하루 한도만) · 예전 함수로 만든다(`AccessApi.issue`).

```sql
-- 이 사람이 지난 7일에 만든 코드 · 다음 자리
select created_at, created_at + interval '168 hours' as frees_at, economy.access_pretty(code)
  from economy.access_codes
 where kind = 'USER' and created_at > now() - interval '168 hours'
   and created_by = (select id from auth.users where email = 'someone@gmail.com')
 order by created_at;
```

## 출시 때

- 구글 Play 심사자는 앱을 다 써 볼 수 있어야 한다 — **Play Console › 앱 콘텐츠 › 앱 액세스**에 심사용 계정 · 접속 코드를 적는다
  (예: `select admin_access_code_named('PLAYREVIEW2026', 20, 60, 'Play 심사');`). 적지 않으면 거절된다.
- 다른 길: Play 의 비공개 테스트 트랙(이메일 목록에 있는 사람만 설치)은 코드 없이 Play 가 막는다.
- 정식 출시는 `admin_access_mode_set('OPEN')` 하나 — 코드 · 권한 기록은 그대로 남는다.
