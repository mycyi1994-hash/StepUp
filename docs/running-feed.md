# 러닝 이벤트 · 러닝·건강 뉴스 — 운영 안내

앱의 **뉴스 탭** 안에 있는 두 자리를 다룬다.

- **러닝 이벤트** — 바깥에서 열리는 대회. 신청은 주최 측 사이트에서 한다.
- **러닝·건강 뉴스** — 언론사 기사. 읽는 것은 원문에서 한다.

하단의 **이벤트 탭**(챌린지·미션·SUP 보상)과는 **다른 것**이다. 표도 화면도
갈라 두었고, 바깥 대회를 누르거나 기사를 읽는 것으로 SUP 가 생기지 않는다.

---

## 1. 지금 무엇이 되고 무엇이 안 되는가

| 갈래 | 상태 | 비고 |
|---|---|---|
| 대회 목록·검색·필터·정렬·달력 | **동작** | 서버 함수가 거르기·정렬·쪽나눔을 검증한다 |
| 대회 관심 저장 | **동작** | 로그인 필요. 두 번 눌러도 안전하다 |
| 대회 자동 수집 | **없음** | 허용된 공개 API·제휴 피드가 확인되지 않았다. 운영자 등록으로 채운다 |
| 뉴스 목록·검색·주제·언론사 필터 | **동작** | 표에 기사가 들어오면 바로 보인다 |
| 뉴스 자동 수집 | **키 대기** | 네이버 검색 API 키를 넣으면 한 시간마다 돈다 |
| AI 요약 | **꺼짐** | 본문 이용 권한이 확인된 출처가 없다 |
| 이미지 | **꺼짐** | 이미지 사용 권한이 확인된 출처가 없다 |
| 관리자 화면(앱 안) | **없음** | Supabase 대시보드에서 SQL 함수로 운영한다 (아래 5장) |

기사가 하나도 없으면 뉴스 탭은 **"새 소식을 준비하고 있어요"** 를 보여 준다.
대회가 없으면 **"등록된 대회가 아직 없어요"** 를 보여 준다. 가짜 대회나 가짜
기사로 채우지 않는다.

---

## 2. 출처별 연결·콘텐츠 이용 상태표

모두 **꺼진 채로** 들어간다(`manual` 만 켜져 있다). 켜는 것은 이용 조건을
확인한 사람이 `admin_set_source()` 로 한다.

| id | 이름 | 방식 | 켜짐 | 검색 | 설명 표시 | 본문 | AI 요약 | 이미지 | 확인해야 할 것 |
|---|---|---|---|---|---|---|---|---|---|
| `naver-news` | 네이버 뉴스 검색 | SEARCH_API | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | 키 등록. 검색 결과의 표시·보관 범위 |
| `sbs` | SBS 뉴스 | RSS | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | RSS 가 **개인·비상업** 조건을 명시. 상업 서비스 사용은 별도 확인 |
| `kbs` | KBS 뉴스 | RSS | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | RSS/API 주소와 이용 가능 여부 미확인 |
| `mbc` | MBC 뉴스 | RSS | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | RSS/API 주소와 이용 가능 여부 미확인 |
| `jtbc` | JTBC 뉴스 | RSS | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | 안내 페이지만 확인. 이용 조건 미확인 |
| `runable` | 러너블 매거진 | PARTNER_FEED | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | 공개 개발자 API·제휴 권한 확인되지 않음 |
| `kdca` | 질병관리청 국가건강정보포털 | PUBLIC_DATA | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ | 공공 건강정보. 언론 기사와 구분 표시. 이용 범위 확인 |
| `manual` | 운영자 등록 | MANUAL | ✓ | — | ✓ | — | — | — | 운영자가 공식 사이트에서 확인해 직접 등록 |

> **검색 API 를 쓸 수 있다는 것이 본문·이미지 재사용이나 AI 요약을 허락한다는
> 뜻은 아니다.** 그래서 권한을 하나로 묶지 않고 항목마다 따로 둔다.

수집기는 `can_discover` 가 꺼진 출처를 두드리지 않고, `can_show_description` 이
꺼진 출처의 기사는 **설명 없이 제목·링크만** 담는다.

---

## 3. 환경 변수와 키 등록

값은 저장소에 적지 않는다. 필요한 이름만 `.env.example` 에 있다.

| 이름 | 어디에 넣나 | 무엇 |
|---|---|---|
| `SUPABASE_URL` | GitHub Secrets | 대시보드 → Settings → API 의 Project URL |
| `SUPABASE_SERVICE_KEY` | GitHub Secrets | service_role 키. RLS 를 지나간다 — **앱에 넣지 않는다** |
| `NAVER_CLIENT_ID` | GitHub Secrets | 네이버 개발자센터 애플리케이션 ID |
| `NAVER_CLIENT_SECRET` | GitHub Secrets | 같은 애플리케이션의 Secret |

### 네이버 검색 API 키 받기

1. <https://developers.naver.com/> 에 로그인한다.
2. **Application → 애플리케이션 등록** 에서 앱을 만든다.
3. 사용 API 에 **검색** 을 추가한다. (비로그인 오픈 API 라 서비스 URL 만 있으면 된다)
4. 나온 **Client ID / Client Secret** 을 저장소의
   **Settings → Secrets and variables → Actions** 에 위 이름으로 넣는다.
5. Actions 탭에서 **Collect running feed** 를 손으로 한 번 돌려 확인한다.
6. 확인이 끝나면 출처를 켠다(아래 5장).

키가 없으면 수집기는 `naver-news: NAVER_CLIENT_ID / NAVER_CLIENT_SECRET 없음` 만
적고 조용히 끝난다. 그것이 정상 동작이다.

---

## 4. 예약 작업

| 무엇 | 주기 | 어디 |
|---|---|---|
| 뉴스 수집 | 60분 | `.github/workflows/feed-collect.yml` (`cron: 0 * * * *`) |
| 대회 수집 | 360분(설정만) | 어댑터 없음. `content_sources.fetch_interval_minutes` 에 값만 있다 |
| 앱의 목록 캐시 | 10분 | `RunningFeedRepository.LIST_TTL_MS` |

예약 실행은 **기본 브랜치에서만** 돈다(GitHub 규칙). main 에 들어가기 전에는
Actions 탭에서 손으로 돌린다.

수집 실패는 표에 남는다(`content_sources.last_error`, `failure_streak`). 연달아
실패하면 수집기가 물러선다 — 간격이 2배씩 늘고 최대 하루까지 벌어진다. 실패해도
**직전 정상 데이터는 지우지 않는다.**

### 로컬에서 돌려 보기

```bash
python3 scripts/feed-collect.py --self-test   # 네트워크 없이 파서·규칙 검사
python3 scripts/feed-collect.py --dry-run     # 모아서 화면에 찍기만
supabase/tests/run.sh                         # 서버 스키마 검사 (postgresql 16 필요)
./gradlew :app:testDebugUnitTest              # 앱 단위 테스트
```

---

## 5. 운영자가 하는 일

앱 안에는 관리자 화면이 없다. **Supabase 대시보드 → SQL Editor** 에서 아래
함수를 부른다. 모든 함수가 앞에서 운영자인지 확인하고, 누가 무엇을 고쳤는지
`admin_audit` 에 남긴다.

### 운영자 등록 (한 번만, 대시보드에서)

```sql
insert into public.app_admins (user_id, note)
values ('<auth.users 의 uuid>', '운영 담당');
```

앱에서 자기를 운영자로 올릴 길은 없다 — `app_admins` 에는 쓰기 정책이 없다.

### 대회 등록

```sql
select public.admin_upsert_event(
  null,                                   -- 새로 만들 때는 null
  '2026 서울하프마라톤',
  p_organizer  => '서울시',
  p_region     => '서울',
  p_venue      => '여의도',
  p_event_date => date '2026-04-19',
  p_event_type => 'ROAD',                 -- ROAD TRAIL WALK FUNRUN CLASS OTHER
  p_registration_status => 'OPEN',        -- UNKNOWN UPCOMING OPEN CLOSED SOLD_OUT
  p_registration_close_at => timestamptz '2026-03-20 23:59+09',
  p_official_url     => 'https://seoul-marathon.com/main',
  p_registration_url => 'https://.../apply',   -- 확인한 것만. 없으면 빼 둔다
  p_source_id  => 'manual',
  p_edition_year => 2026,
  p_visibility => 'PUBLIC'                -- 검토 중이면 DRAFT
);
```

버튼 문구는 **링크가 정한다.** 접수 URL 이 있으면 "공식 접수 사이트", 공식
안내만 있으면 "대회 정보 보기", 둘 다 없으면 "출처에서 확인" 이 된다. 없는
접수처를 있다고 적을 수 없게 한 것이다.

### 종목 추가 (거리 필터에 잡히려면 필요하다)

```sql
select public.admin_add_discipline(
  '<대회 uuid>', '10K', '10K', 10000,
  timestamptz '2026-03-20 23:59+09', 'OPEN', 30000
);
select public.admin_add_discipline('<대회 uuid>', '하프', 'HALF', 21097);
```

- 첫 인자 뒤의 `'10K'` 는 **원문 그대로**, 그다음 `'10K'` 는 **정규화 값**이다.
  거리 키: `LTE_5K · 10K · HALF · FULL · ULTRA · OTHER · UNKNOWN`
- 원문에 없는 거리를 추측해 넣지 않는다. 모르면 `UNKNOWN` 이다.

### 취소·연기·숨김

```sql
select public.admin_set_event_state('<uuid>', p_cancelled => 'POSTPONED');
select public.admin_set_event_state('<uuid>', p_visibility => 'HIDDEN');
```

취소·연기는 접수 상태보다 먼저 표시되고, 목록에서 뒤로 밀린다.

### 기사 숨김·검수

```sql
select public.admin_set_news_state('<uuid>', p_visibility => 'HIDDEN');
select public.admin_set_news_state('<uuid>', p_review_status => 'APPROVED', p_lock => true);
```

`p_lock => true` 로 잠근 기사는 **다음 자동 수집이 덮어쓰지 않는다.**

### 출처 켜기

이용 조건을 확인한 뒤에만 켠다.

```sql
select public.admin_set_source(
  'naver-news',
  p_enabled              => true,
  p_can_discover         => true,
  p_can_show_description => true,     -- 검색 설명 표시가 허용된 경우에만
  p_verified_note        => '2026-09-22 이용 약관 확인. 제목·설명·링크까지 허용.'
);
```

`p_verified_note` 를 주면 확인 시각이 함께 기록된다. 무엇을 근거로 켰는지
남기지 않으면 나중에 아무도 되짚지 못한다.

### 상태 보기

```sql
select id, name, enabled, last_success_at, failure_streak, last_error
  from public.content_sources order by id;

select * from public.admin_audit order by created_at desc limit 50;
```

---

## 6. 안전 장치

- 표는 **읽기만** 열려 있다. 모든 변경은 SECURITY DEFINER 함수를 거친다.
- 관리자 함수는 앞에서 `is_admin()` 을 확인한다.
- 초안·숨김은 운영자에게만 보인다.
- 주소는 서버(`is_web_url`)와 앱(`SafeUrl`) 양쪽에서 검사한다.
  `javascript:` `data:` `file:` `intent:` 는 거른다.
- 요약과 이미지는 **권한이 확인된 출처의 것만** 서버가 내보낸다. 표에 값이
  남아 있어도 권한이 내려가면 나가지 않는다.
- 외부 사이트는 커스텀 탭으로 연다. 앱 안 iframe 으로 복제하지 않는다.
- 외부 사이트 방문은 **신청 완료가 아니다.** 앱은 신청 여부를 알 수 없고,
  안다고 말하지 않는다.

---

## 7. 확인하지 못한 것

이 저장소에서 작업할 때 바깥 네트워크가 GitHub 말고는 막혀 있었다. 그래서
**아래는 코드만 준비되어 있고 실제 응답으로 확인하지 못했다.**

- 네이버 검색 API 의 실제 응답 형태와 한도 (키가 없어 호출 자체를 못 했다)
- runable.me · 각 언론사 RSS 의 실제 내용과 이용 조건
- 앱 화면의 실기기 동작 (빌드는 CI 에서 통과)

수집기 파서와 필터 규칙은 네트워크 없이 도는 자체 검사 40가지로 확인했다
(`python3 scripts/feed-collect.py --self-test`). 서버 스키마는 실제 Postgres 에
두 번 올려 40여 가지 규칙을 확인했다(`supabase/tests/run.sh`).
