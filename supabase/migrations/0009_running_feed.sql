-- 러닝 이벤트(대회)와 러닝·건강 뉴스.
--
-- ── 무엇이고 무엇이 아닌가 ──
--
-- 여기 담기는 것은 **바깥 세상의 정보**다. 대회는 주최 측 사이트에서 신청하고,
-- 기사는 언론사 원문에서 읽는다. 앱은 찾아 주고 보내 줄 뿐이다.
--
-- 그래서 기존 "이벤트 탭"(챌린지·미션·SUP 보상)과 **표부터 갈라 둔다.** 섞이면
-- 바깥 대회를 눌렀다고 SUP 를 주는 실수가 언젠가 생긴다. 이 파일의 어떤 표도
-- sup_ledger 를 건드리지 않는다.
--
-- ── 모르는 것은 모른다고 적는다 ──
--
-- 날짜·요금·접수 상태는 확인되지 않으면 null 이거나 UNKNOWN 이다. 특히
-- **접수 상태를 날짜만으로 "접수 중"이라고 단정하지 않는다** — 조기 마감과
-- 매진이 실제로 일어나고, 틀린 "접수 중"은 사용자를 헛걸음시킨다.
--
-- ── 쓰기는 함수로만 ──
--
-- 앱에 박힌 anon 키는 누구나 꺼낼 수 있다. 그래서 표는 읽기만 열려 있고,
-- 바꾸는 일은 전부 SECURITY DEFINER 함수를 거친다. 운영 함수는 그 앞에
-- 관리자인지 먼저 묻는다.

-- ══════════════════════════════════════════════════════════════════
-- 관리자
-- ══════════════════════════════════════════════════════════════════
--
-- profiles 에 깃발을 세우지 않고 표를 따로 둔다. profiles 는 본인이 고칠 수
-- 있는 표라, 거기에 관리자 표시를 두면 "내가 나를 관리자로" 하는 길이 열린다.
create table if not exists public.app_admins (
  user_id uuid primary key references auth.users on delete cascade,
  note text not null default '',
  created_at timestamptz not null default now()
);

comment on table public.app_admins is
  '운영자 명단. 대시보드에서만 넣는다 — 앱에서 자기를 넣을 길은 없다.';

alter table public.app_admins enable row level security;

-- 정책을 하나도 만들지 않는다. 앱 역할은 이 표를 읽지도 쓰지도 못한다.
-- 확인은 아래 is_admin() 이 대신한다.

create or replace function public.is_admin() returns boolean
language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.app_admins where user_id = auth.uid())
$$;

comment on function public.is_admin() is '지금 로그인한 사람이 운영자인가.';

-- ══════════════════════════════════════════════════════════════════
-- 주소 검사
-- ══════════════════════════════════════════════════════════════════
--
-- 관리자가 넣는 주소도 검사한다. 사람은 오타를 내고, 붙여넣기는 이상한 것을
-- 함께 가져온다. javascript: 하나가 들어가면 그 카드를 누른 사용자가 위험해진다.
create or replace function public.is_web_url(p_url text) returns boolean
language sql immutable as $$
  select p_url is null
      or (p_url ~* '^https?://[^\s/$.?#].[^\s]*$' and p_url !~* '^(javascript|data|file|intent|content):')
$$;

comment on function public.is_web_url(text) is
  'http(s) 주소인가. javascript·data·file·intent 스킴은 거른다.';

-- ══════════════════════════════════════════════════════════════════
-- 출처
-- ══════════════════════════════════════════════════════════════════

create table if not exists public.content_sources (
  id text primary key,
  name text not null,
  homepage_url text check (public.is_web_url(homepage_url)),

  -- 이 출처의 글로 인정할 도메인. **문자열 포함이 아니라 hostname 으로**
  -- 견준다 — "sbs.co.kr.evil.com" 같은 흉내를 통과시키지 않기 위해서다.
  allowed_domains text[] not null default '{}',

  provider_type text not null default 'MANUAL' check (provider_type in (
    'SEARCH_API',    -- 검색 API (네이버 뉴스 검색 등)
    'RSS',
    'PARTNER_FEED',  -- 제휴로 받은 피드
    'PUBLIC_DATA',   -- 공공 데이터
    'MANUAL'         -- 운영자가 손으로 등록
  )),
  endpoint text check (public.is_web_url(endpoint)),

  -- 켜져 있어야 수집기가 돈다. 권한이 확인되지 않은 어댑터는 꺼 둔다.
  enabled boolean not null default false,

  -- ── 콘텐츠 이용 범위 ──
  --
  -- 하나로 뭉뚱그리지 않는다. "검색해도 된다"가 "본문을 요약해도 된다"를
  -- 뜻하지는 않는다. 확인된 것만 켠다.
  can_discover boolean not null default false,
  can_show_title boolean not null default false,
  can_show_description boolean not null default false,
  can_fetch_body boolean not null default false,
  can_summarize boolean not null default false,
  can_use_image boolean not null default false,
  retention_days int,

  attribution_text text not null default '',
  fetch_interval_minutes int not null default 60 check (fetch_interval_minutes >= 5),

  last_success_at timestamptz,
  last_error text,
  last_error_at timestamptz,
  -- 연달아 실패한 횟수. 수집기가 이 값으로 물러선다(백오프).
  failure_streak int not null default 0 check (failure_streak >= 0),

  terms_url text check (public.is_web_url(terms_url)),
  verified_at timestamptz,
  verified_note text not null default '',

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table public.content_sources is
  '뉴스·대회를 어디서 가져오는지와, 그 출처에서 무엇까지 해도 되는지.';

-- ══════════════════════════════════════════════════════════════════
-- 대회
-- ══════════════════════════════════════════════════════════════════

create table if not exists public.running_events (
  id uuid primary key default gen_random_uuid(),

  title text not null,
  edition_year int check (edition_year between 1900 and 2200),
  organizer text not null default '',

  country text not null default 'KR',
  region text not null default '',
  venue text not null default '',

  -- 여는 날. 모르면 null 이다.
  event_date date,
  event_end_date date,
  -- 시각은 아는 경우에만. 모른다고 00:00 으로 적으면 "자정에 출발"이 된다.
  event_start_time time,
  timezone text not null default 'Asia/Seoul',
  date_precision text not null default 'DATE'
    check (date_precision in ('DATETIME', 'DATE', 'MONTH', 'UNKNOWN')),

  event_type text not null default 'OTHER'
    check (event_type in ('ROAD', 'TRAIL', 'WALK', 'FUNRUN', 'CLASS', 'OTHER')),

  -- 유형과 거리는 따로다. 트레일도 10km 가 있고 걷기도 5km 가 있다.
  -- 거리는 종목 표(event_disciplines)에 있다.

  registration_status text not null default 'UNKNOWN'
    check (registration_status in ('UNKNOWN', 'UPCOMING', 'OPEN', 'CLOSED', 'SOLD_OUT')),
  registration_open_at timestamptz,
  registration_close_at timestamptz,

  fee_min numeric(12, 2) check (fee_min >= 0),
  currency text not null default 'KRW',

  official_url text check (public.is_web_url(official_url)),
  registration_url text check (public.is_web_url(registration_url)),
  source_url text check (public.is_web_url(source_url)),

  -- 카드의 버튼이 무엇이라고 말할지 정한다.
  --   REGISTRATION   공식 접수 사이트 ↗
  --   OFFICIAL_INFO  대회 정보 보기 ↗   (공식 안내는 있지만 접수 링크는 못 찾음)
  --   SOURCE_ONLY    출처에서 확인 ↗    (주최 측 공식 링크를 확인하지 못함)
  destination_type text not null default 'SOURCE_ONLY'
    check (destination_type in ('REGISTRATION', 'OFFICIAL_INFO', 'SOURCE_ONLY')),

  image_url text check (public.is_web_url(image_url)),
  image_usage_status text not null default 'UNKNOWN'
    check (image_usage_status in ('UNKNOWN', 'ALLOWED', 'DENIED')),

  source_id text references public.content_sources on delete set null,
  last_verified_at timestamptz,

  visibility text not null default 'DRAFT'
    check (visibility in ('PUBLIC', 'HIDDEN', 'DRAFT')),

  -- 취소·연기는 접수 상태보다 먼저 보여 준다. "접수 중인 취소된 대회"는 없다.
  cancelled_or_postponed text not null default 'NONE'
    check (cancelled_or_postponed in ('NONE', 'CANCELLED', 'POSTPONED')),

  -- 운영자가 손본 열 이름. 자동 수집이 이 열은 건드리지 않는다.
  locked_fields text[] not null default '{}',
  updated_by uuid references auth.users on delete set null,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table public.running_events is
  '바깥에서 열리는 대회. 신청은 주최 측 사이트에서 한다 — 앱은 보내 줄 뿐이다.';

create index if not exists running_events_date on public.running_events (event_date)
  where visibility = 'PUBLIC';
create index if not exists running_events_region on public.running_events (region);

-- 같은 대회를 두 번 만들지 않게. 연기되어 날짜가 바뀐 것은 같은 대회이므로
-- 날짜는 열쇠에 넣지 않는다.
create unique index if not exists running_events_identity
  on public.running_events (lower(title), coalesce(edition_year, 0), lower(region));

create table if not exists public.event_disciplines (
  id bigint generated always as identity primary key,
  event_id uuid not null references public.running_events on delete cascade,

  -- 원문 그대로. "10K", "하프", "Full Course" 처럼 적힌 대로 둔다.
  raw_name text not null,

  -- 거를 때 쓰는 정규화 값. 원문에 없는 거리를 추측해 넣지 않는다.
  distance_key text not null default 'UNKNOWN'
    check (distance_key in ('LTE_5K', '10K', 'HALF', 'FULL', 'ULTRA', 'OTHER', 'UNKNOWN')),
  distance_meters int check (distance_meters > 0),

  -- 종목마다 접수 일정이 다른 대회가 있다.
  registration_open_at timestamptz,
  registration_close_at timestamptz,
  registration_status text not null default 'UNKNOWN'
    check (registration_status in ('UNKNOWN', 'UPCOMING', 'OPEN', 'CLOSED', 'SOLD_OUT')),
  fee numeric(12, 2) check (fee >= 0),

  created_at timestamptz not null default now()
);

comment on table public.event_disciplines is
  '대회의 종목. 원문 이름과 정규화한 거리를 함께 둔다 — 원문을 잃으면 되돌릴 수 없다.';

create index if not exists event_disciplines_event on public.event_disciplines (event_id);
create index if not exists event_disciplines_distance on public.event_disciplines (distance_key);

-- ══════════════════════════════════════════════════════════════════
-- 뉴스
-- ══════════════════════════════════════════════════════════════════

create table if not exists public.news_articles (
  id uuid primary key default gen_random_uuid(),

  title text not null,
  publisher_name text not null default '',
  publisher_domain text not null default '',

  -- 언론사 원문. 이것이 없으면 출처를 말할 수 없다.
  original_url text not null unique check (public.is_web_url(original_url)),
  fallback_url text check (public.is_web_url(fallback_url)),
  canonical_url text check (public.is_web_url(canonical_url)),

  -- 기사가 나온 때와 우리가 주워 온 때는 다르다. 섞으면 3년 전 건강 자료가
  -- 오늘 뉴스가 된다.
  published_at timestamptz,
  fetched_at timestamptz not null default now(),

  -- 검색 API 가 준 설명. 이것은 요약이 아니다.
  description text not null default '',
  -- 본문 이용이 허락된 출처에서만 채워진다.
  summary text,
  summary_type text not null default 'NONE'
    check (summary_type in ('NONE', 'SEARCH_DESCRIPTION', 'AI_SUMMARY')),

  category text not null default 'RUNNING' check (category in (
    'RUNNING', 'WALK_JOG', 'TRAINING', 'INJURY', 'HEALTH', 'RACE_NEWS', 'PUBLIC_HEALTH'
  )),
  keywords text[] not null default '{}',
  relevance_score numeric(6, 2) not null default 0,

  thumbnail_url text check (public.is_web_url(thumbnail_url)),
  image_usage_status text not null default 'UNKNOWN'
    check (image_usage_status in ('UNKNOWN', 'ALLOWED', 'DENIED')),

  source_id text references public.content_sources on delete set null,
  content_hash text not null default '',

  rights_status text not null default 'LINK_ONLY'
    check (rights_status in ('UNKNOWN', 'LINK_ONLY', 'DESCRIPTION_OK', 'FULL_OK')),
  review_status text not null default 'AUTO'
    check (review_status in ('AUTO', 'NEEDS_REVIEW', 'APPROVED', 'REJECTED')),
  visibility text not null default 'PUBLIC' check (visibility in ('PUBLIC', 'HIDDEN')),

  -- 운영자가 손본 기사는 자동 수집이 덮어쓰지 않는다.
  locked boolean not null default false,
  updated_by uuid references auth.users on delete set null,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table public.news_articles is
  '러닝·건강 기사. 제목·출처·날짜·링크까지만 담고 본문은 원문에서 읽는다.';

create index if not exists news_articles_recent
  on public.news_articles (published_at desc) where visibility = 'PUBLIC';
create index if not exists news_articles_category on public.news_articles (category);

-- ══════════════════════════════════════════════════════════════════
-- 저장(관심)
-- ══════════════════════════════════════════════════════════════════

create table if not exists public.saved_events (
  user_id uuid not null references auth.users on delete cascade,
  event_id uuid not null references public.running_events on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, event_id)
);

create table if not exists public.saved_news (
  user_id uuid not null references auth.users on delete cascade,
  article_id uuid not null references public.news_articles on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, article_id)
);

comment on table public.saved_events is '관심 대회. 열쇠가 (사람, 대회)라 두 번 눌러도 한 줄이다.';

-- ══════════════════════════════════════════════════════════════════
-- 권한
-- ══════════════════════════════════════════════════════════════════

alter table public.content_sources   enable row level security;
alter table public.running_events    enable row level security;
alter table public.event_disciplines enable row level security;
alter table public.news_articles     enable row level security;
alter table public.saved_events      enable row level security;
alter table public.saved_news        enable row level security;

-- 공개된 것만 읽힌다. 초안·숨김은 운영자만 본다.
do $$ begin
  create policy running_events_read on public.running_events
    for select using (visibility = 'PUBLIC' or public.is_admin());
exception when duplicate_object then null; end $$;

do $$ begin
  create policy event_disciplines_read on public.event_disciplines
    for select using (exists (
      select 1 from public.running_events e
       where e.id = event_id and (e.visibility = 'PUBLIC' or public.is_admin())
    ));
exception when duplicate_object then null; end $$;

do $$ begin
  create policy news_articles_read on public.news_articles
    for select using (visibility = 'PUBLIC' or public.is_admin());
exception when duplicate_object then null; end $$;

-- 출처 목록은 공개한다. 어디서 온 정보인지는 감출 것이 아니다.
-- 다만 열쇠가 될 수 있는 endpoint 는 뷰에서 뺀다.
do $$ begin
  create policy content_sources_read on public.content_sources for select using (true);
exception when duplicate_object then null; end $$;

-- 저장 목록은 제 것만.
do $$ begin
  create policy saved_events_own on public.saved_events
    for select using (user_id = auth.uid());
exception when duplicate_object then null; end $$;

do $$ begin
  create policy saved_news_own on public.saved_news
    for select using (user_id = auth.uid());
exception when duplicate_object then null; end $$;

-- 어느 표에도 INSERT/UPDATE/DELETE 정책은 없다. 아래 함수로만 바뀐다.

-- ══════════════════════════════════════════════════════════════════
-- 앱이 읽는 자리
-- ══════════════════════════════════════════════════════════════════

create or replace view public.running_sources_public
with (security_invoker = true) as
  select id, name, homepage_url, provider_type, enabled,
         can_discover, can_show_title, can_show_description,
         can_fetch_body, can_summarize, can_use_image,
         attribution_text, terms_url, verified_at, verified_note,
         last_success_at,
         -- 실패 사유는 사람에게 보여 줄 말이 아니다. 실패했다는 사실만.
         (last_error is not null and last_error <> '') as failing
    from public.content_sources;

comment on view public.running_sources_public is
  '앱·운영 화면이 보는 출처 목록. endpoint 와 오류 원문은 빼고 보낸다.';

-- ══════════════════════════════════════════════════════════════════
-- 대회 목록
-- ══════════════════════════════════════════════════════════════════
--
-- 거르기·정렬·쪽나눔을 서버가 다시 검증한다. 앱이 보낸 값을 그대로 SQL 에
-- 끼워 넣으면 limit 100000 같은 요청 하나로 서버가 주저앉는다.
create or replace function public.list_running_events(
  p_query text default '',
  p_event_type text default 'ALL',
  p_distance text default 'ALL',
  p_region text default 'ALL',
  p_status text default 'ALL',
  p_from date default null,
  p_to date default null,
  p_sort text default 'DATE',
  p_include_past boolean default false,
  p_limit int default 20,
  p_offset int default 0
)
returns table (
  id uuid,
  title text,
  organizer text,
  region text,
  venue text,
  event_date date,
  event_end_date date,
  has_start_time boolean,
  date_precision text,
  event_type text,
  registration_status text,
  registration_close_at timestamptz,
  fee_min numeric,
  currency text,
  destination_type text,
  target_url text,
  last_verified_at timestamptz,
  cancelled_or_postponed text,
  distances text[],
  discipline_names text[],
  saved boolean,
  total_count bigint
)
language sql stable security definer set search_path = public as $$
  with bounds as (
    select greatest(1, least(coalesce(p_limit, 20), 50)) as lim,
           greatest(0, coalesce(p_offset, 0)) as off,
           nullif(btrim(coalesce(p_query, '')), '') as q
  ),
  base as (
    select e.*,
           coalesce(array_agg(distinct d.distance_key)
                      filter (where d.distance_key is not null), '{}') as distances,
           coalesce(array_agg(distinct d.raw_name)
                      filter (where d.raw_name is not null), '{}') as names
      from public.running_events e
      left join public.event_disciplines d on d.event_id = e.id
     where e.visibility = 'PUBLIC'
     group by e.id
  ),
  filtered as (
    select b.* from base b, bounds
     where (bounds.q is null
            or b.title ilike '%' || bounds.q || '%'
            or b.organizer ilike '%' || bounds.q || '%'
            or b.region ilike '%' || bounds.q || '%'
            or b.venue ilike '%' || bounds.q || '%')
       and (p_event_type = 'ALL' or b.event_type = p_event_type)
       and (p_distance = 'ALL' or p_distance = any (b.distances))
       and (p_region = 'ALL' or b.region = p_region)
       and (p_status = 'ALL' or b.registration_status = p_status)
       and (p_from is null or b.event_date is null or b.event_date >= p_from)
       and (p_to is null or b.event_date is null or b.event_date <= p_to)
       -- 지난 대회는 기본 목록에서 뺀다. 날짜를 모르는 대회는 남긴다 —
       -- 모른다고 지나갔다고 칠 수는 없다.
       and (p_include_past or b.event_date is null or b.event_date >= current_date)
  ),
  counted as (select count(*) as n from filtered)
  select f.id, f.title, f.organizer, f.region, f.venue,
         f.event_date, f.event_end_date,
         f.event_start_time is not null,
         f.date_precision, f.event_type,
         f.registration_status, f.registration_close_at,
         f.fee_min, f.currency,
         f.destination_type,
         case f.destination_type
           when 'REGISTRATION' then coalesce(f.registration_url, f.official_url, f.source_url)
           when 'OFFICIAL_INFO' then coalesce(f.official_url, f.source_url)
           else f.source_url
         end,
         f.last_verified_at, f.cancelled_or_postponed,
         f.distances, f.names,
         exists (select 1 from public.saved_events s
                  where s.event_id = f.id and s.user_id = auth.uid()),
         counted.n
    from filtered f, counted, bounds
   order by
     -- 취소·연기는 뒤로 민다. 달릴 수 있는 대회가 먼저 보여야 한다.
     (f.cancelled_or_postponed <> 'NONE'),
     case when p_sort = 'CLOSING' then f.registration_close_at end asc nulls last,
     case when p_sort = 'NEWEST' then f.created_at end desc,
     f.event_date asc nulls last,
     f.title asc
   limit (select lim from bounds) offset (select off from bounds)
$$;

comment on function public.list_running_events(text, text, text, text, text, date, date, text, boolean, int, int) is
  '대회 목록. 거르기·정렬·쪽나눔을 서버가 다시 검증한다.';

create or replace function public.get_running_event(p_id uuid)
returns table (
  id uuid,
  title text,
  organizer text,
  region text,
  venue text,
  event_date date,
  event_end_date date,
  event_start_time time,
  date_precision text,
  event_type text,
  registration_status text,
  registration_open_at timestamptz,
  registration_close_at timestamptz,
  fee_min numeric,
  currency text,
  official_url text,
  registration_url text,
  source_url text,
  destination_type text,
  source_name text,
  attribution_text text,
  last_verified_at timestamptz,
  cancelled_or_postponed text,
  saved boolean
)
language sql stable security definer set search_path = public as $$
  select e.id, e.title, e.organizer, e.region, e.venue,
         e.event_date, e.event_end_date, e.event_start_time, e.date_precision,
         e.event_type, e.registration_status,
         e.registration_open_at, e.registration_close_at,
         e.fee_min, e.currency,
         e.official_url, e.registration_url, e.source_url, e.destination_type,
         coalesce(s.name, ''), coalesce(s.attribution_text, ''),
         e.last_verified_at, e.cancelled_or_postponed,
         exists (select 1 from public.saved_events sv
                  where sv.event_id = e.id and sv.user_id = auth.uid())
    from public.running_events e
    left join public.content_sources s on s.id = e.source_id
   where e.id = p_id and (e.visibility = 'PUBLIC' or public.is_admin())
$$;

create or replace function public.event_disciplines_of(p_id uuid)
returns table (
  raw_name text,
  distance_key text,
  distance_meters int,
  registration_open_at timestamptz,
  registration_close_at timestamptz,
  registration_status text,
  fee numeric
)
language sql stable security definer set search_path = public as $$
  select d.raw_name, d.distance_key, d.distance_meters,
         d.registration_open_at, d.registration_close_at,
         d.registration_status, d.fee
    from public.event_disciplines d
    join public.running_events e on e.id = d.event_id
   where d.event_id = p_id and (e.visibility = 'PUBLIC' or public.is_admin())
   order by d.distance_meters nulls last, d.raw_name
$$;

-- ══════════════════════════════════════════════════════════════════
-- 뉴스 목록
-- ══════════════════════════════════════════════════════════════════

create or replace function public.list_running_news(
  p_query text default '',
  p_category text default 'ALL',
  p_publisher text default 'ALL',
  p_sort text default 'RECENT',
  p_limit int default 20,
  p_offset int default 0
)
returns table (
  id uuid,
  title text,
  publisher_name text,
  publisher_domain text,
  original_url text,
  published_at timestamptz,
  fetched_at timestamptz,
  description text,
  summary text,
  summary_type text,
  category text,
  thumbnail_url text,
  attribution_text text,
  saved boolean,
  total_count bigint
)
language sql stable security definer set search_path = public as $$
  with bounds as (
    select greatest(1, least(coalesce(p_limit, 20), 50)) as lim,
           greatest(0, coalesce(p_offset, 0)) as off,
           nullif(btrim(coalesce(p_query, '')), '') as q
  ),
  filtered as (
    select a.*, coalesce(s.attribution_text, '') as attribution
      from public.news_articles a
      left join public.content_sources s on s.id = a.source_id, bounds
     where a.visibility = 'PUBLIC'
       and a.review_status <> 'REJECTED'
       and (bounds.q is null
            or a.title ilike '%' || bounds.q || '%'
            or a.description ilike '%' || bounds.q || '%')
       and (p_category = 'ALL' or a.category = p_category)
       and (p_publisher = 'ALL' or a.publisher_domain = p_publisher)
  ),
  counted as (select count(*) as n from filtered)
  select f.id, f.title, f.publisher_name, f.publisher_domain,
         f.original_url, f.published_at, f.fetched_at,
         f.description,
         -- 요약은 허락된 경우에만 내보낸다. 표에 남아 있더라도 권한이
         -- 내려갔으면 보여 주지 않는다.
         case when f.summary_type = 'AI_SUMMARY' and f.rights_status = 'FULL_OK'
              then f.summary end,
         case when f.summary_type = 'AI_SUMMARY' and f.rights_status = 'FULL_OK'
              then 'AI_SUMMARY' else
              case when f.description <> '' and f.rights_status in ('DESCRIPTION_OK', 'FULL_OK')
                   then 'SEARCH_DESCRIPTION' else 'NONE' end
         end,
         f.category,
         case when f.image_usage_status = 'ALLOWED' then f.thumbnail_url end,
         f.attribution,
         exists (select 1 from public.saved_news s
                  where s.article_id = f.id and s.user_id = auth.uid()),
         counted.n
    from filtered f, counted, bounds
   order by
     case when p_sort = 'RELEVANCE' then f.relevance_score end desc nulls last,
     f.published_at desc nulls last,
     f.fetched_at desc
   limit (select lim from bounds) offset (select off from bounds)
$$;

comment on function public.list_running_news(text, text, text, text, int, int) is
  '뉴스 목록. 요약과 이미지는 이용 범위가 확인된 것만 내보낸다.';

-- ══════════════════════════════════════════════════════════════════
-- 저장 / 해제
-- ══════════════════════════════════════════════════════════════════
--
-- 같은 요청이 두 번 와도 결과가 같아야 한다. 네트워크가 끊겼다 이어지면
-- 같은 탭 하나가 두 번 도착하는 일이 실제로 있다.
create or replace function public.save_event(p_id uuid, p_on boolean)
returns boolean
language plpgsql security definer set search_path = public as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_on then
    insert into public.saved_events (user_id, event_id) values (v_user, p_id)
    on conflict do nothing;
  else
    delete from public.saved_events where user_id = v_user and event_id = p_id;
  end if;
  return p_on;
end $$;

create or replace function public.save_news(p_id uuid, p_on boolean)
returns boolean
language plpgsql security definer set search_path = public as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_on then
    insert into public.saved_news (user_id, article_id) values (v_user, p_id)
    on conflict do nothing;
  else
    delete from public.saved_news where user_id = v_user and article_id = p_id;
  end if;
  return p_on;
end $$;

comment on function public.save_event(uuid, boolean) is
  '관심 대회 켜고 끄기. 두 번 눌러도 한 줄이다.';

-- ══════════════════════════════════════════════════════════════════
-- 운영
-- ══════════════════════════════════════════════════════════════════
--
-- 앱에는 관리자 화면이 없다. 이 함수들은 Supabase 대시보드나 서버 작업이
-- 부르는 자리이고, 앞에서 운영자인지 반드시 확인한다.

create or replace function public.admin_guard() returns void
language plpgsql stable security definer set search_path = public as $$
begin
  if not public.is_admin() then
    raise exception '운영자만 할 수 있습니다' using errcode = '42501';
  end if;
end $$;

create table if not exists public.admin_audit (
  id bigint generated always as identity primary key,
  actor uuid references auth.users on delete set null,
  action text not null,
  target text not null default '',
  detail jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

comment on table public.admin_audit is '누가 무엇을 언제 고쳤는지. 되돌릴 때 이것부터 본다.';

alter table public.admin_audit enable row level security;
-- 정책 없음 — 앱 역할은 못 읽는다.

create or replace function public.admin_log(p_action text, p_target text, p_detail jsonb)
returns void
language sql security definer set search_path = public as $$
  insert into public.admin_audit (actor, action, target, detail)
  values (auth.uid(), p_action, p_target, coalesce(p_detail, '{}'::jsonb))
$$;

revoke all on function public.admin_log(text, text, jsonb) from public;

/**
 * 대회 한 건을 넣거나 고친다.
 *
 * p_id 를 주면 고치고, 주지 않으면 새로 만든다. 운영자가 손본 열은
 * locked_fields 에 적어 두면 자동 수집이 건드리지 않는다.
 */
create or replace function public.admin_upsert_event(
  p_id uuid,
  p_title text,
  p_organizer text default '',
  p_region text default '',
  p_venue text default '',
  p_event_date date default null,
  p_event_type text default 'ROAD',
  p_registration_status text default 'UNKNOWN',
  p_registration_open_at timestamptz default null,
  p_registration_close_at timestamptz default null,
  p_official_url text default null,
  p_registration_url text default null,
  p_source_url text default null,
  p_source_id text default null,
  p_edition_year int default null,
  p_visibility text default 'DRAFT'
) returns uuid
language plpgsql security definer set search_path = public as $$
declare
  v_id uuid;
  v_destination text;
begin
  perform public.admin_guard();

  if coalesce(btrim(p_title), '') = '' then
    raise exception '대회 이름이 필요합니다' using errcode = '22023';
  end if;
  if not (public.is_web_url(p_official_url) and public.is_web_url(p_registration_url)
          and public.is_web_url(p_source_url)) then
    raise exception '주소가 올바르지 않습니다' using errcode = '22023';
  end if;

  -- 버튼이 무엇이라고 말할지는 링크가 정한다. 접수 링크가 없는데
  -- "공식 접수 사이트"라고 적으면 거짓말이 된다.
  v_destination := case
    when p_registration_url is not null then 'REGISTRATION'
    when p_official_url is not null then 'OFFICIAL_INFO'
    else 'SOURCE_ONLY'
  end;

  if p_id is null then
    insert into public.running_events (
      title, organizer, region, venue, event_date, event_type,
      registration_status, registration_open_at, registration_close_at,
      official_url, registration_url, source_url, destination_type,
      source_id, edition_year, visibility, last_verified_at, updated_by
    ) values (
      p_title, p_organizer, p_region, p_venue, p_event_date, p_event_type,
      p_registration_status, p_registration_open_at, p_registration_close_at,
      p_official_url, p_registration_url, p_source_url, v_destination,
      p_source_id, p_edition_year, p_visibility, now(), auth.uid()
    ) returning id into v_id;
  else
    update public.running_events set
      title = p_title, organizer = p_organizer, region = p_region, venue = p_venue,
      event_date = p_event_date, event_type = p_event_type,
      registration_status = p_registration_status,
      registration_open_at = p_registration_open_at,
      registration_close_at = p_registration_close_at,
      official_url = p_official_url, registration_url = p_registration_url,
      source_url = p_source_url, destination_type = v_destination,
      source_id = p_source_id, edition_year = p_edition_year,
      visibility = p_visibility, last_verified_at = now(),
      updated_by = auth.uid(), updated_at = now()
    where id = p_id
    returning id into v_id;
    if v_id is null then
      raise exception '없는 대회입니다' using errcode = '22023';
    end if;
  end if;

  perform public.admin_log('upsert_event', v_id::text, jsonb_build_object('title', p_title));
  return v_id;
end $$;

create or replace function public.admin_add_discipline(
  p_event_id uuid,
  p_raw_name text,
  p_distance_key text default 'UNKNOWN',
  p_distance_meters int default null,
  p_registration_close_at timestamptz default null,
  p_registration_status text default 'UNKNOWN',
  p_fee numeric default null
) returns bigint
language plpgsql security definer set search_path = public as $$
declare v_id bigint;
begin
  perform public.admin_guard();
  insert into public.event_disciplines (
    event_id, raw_name, distance_key, distance_meters,
    registration_close_at, registration_status, fee
  ) values (
    p_event_id, p_raw_name, p_distance_key, p_distance_meters,
    p_registration_close_at, p_registration_status, p_fee
  ) returning id into v_id;
  perform public.admin_log('add_discipline', v_id::text,
    jsonb_build_object('event', p_event_id, 'name', p_raw_name));
  return v_id;
end $$;

create or replace function public.admin_set_event_state(
  p_id uuid,
  p_visibility text default null,
  p_cancelled text default null
) returns void
language plpgsql security definer set search_path = public as $$
begin
  perform public.admin_guard();
  update public.running_events set
    visibility = coalesce(p_visibility, visibility),
    cancelled_or_postponed = coalesce(p_cancelled, cancelled_or_postponed),
    updated_by = auth.uid(),
    updated_at = now()
  where id = p_id;
  perform public.admin_log('set_event_state', p_id::text,
    jsonb_build_object('visibility', p_visibility, 'cancelled', p_cancelled));
end $$;

create or replace function public.admin_set_news_state(
  p_id uuid,
  p_visibility text default null,
  p_review_status text default null,
  p_lock boolean default null
) returns void
language plpgsql security definer set search_path = public as $$
begin
  perform public.admin_guard();
  update public.news_articles set
    visibility = coalesce(p_visibility, visibility),
    review_status = coalesce(p_review_status, review_status),
    locked = coalesce(p_lock, locked),
    updated_by = auth.uid(),
    updated_at = now()
  where id = p_id;
  perform public.admin_log('set_news_state', p_id::text,
    jsonb_build_object('visibility', p_visibility, 'review', p_review_status));
end $$;

create or replace function public.admin_set_source(
  p_id text,
  p_enabled boolean default null,
  p_can_discover boolean default null,
  p_can_show_description boolean default null,
  p_can_fetch_body boolean default null,
  p_can_summarize boolean default null,
  p_can_use_image boolean default null,
  p_verified_note text default null
) returns void
language plpgsql security definer set search_path = public as $$
begin
  perform public.admin_guard();
  update public.content_sources set
    enabled = coalesce(p_enabled, enabled),
    can_discover = coalesce(p_can_discover, can_discover),
    can_show_description = coalesce(p_can_show_description, can_show_description),
    can_fetch_body = coalesce(p_can_fetch_body, can_fetch_body),
    can_summarize = coalesce(p_can_summarize, can_summarize),
    can_use_image = coalesce(p_can_use_image, can_use_image),
    verified_note = coalesce(p_verified_note, verified_note),
    verified_at = case when p_verified_note is null then verified_at else now() end,
    updated_at = now()
  where id = p_id;
  perform public.admin_log('set_source', p_id, jsonb_build_object('enabled', p_enabled));
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 출처 초기 목록
-- ══════════════════════════════════════════════════════════════════
--
-- 전부 **꺼진 채로** 들어간다. 권한·키·데이터 형식 중 하나라도 확인되지 않은
-- 어댑터를 켜 두면, 켜져 있다는 사실만으로 "연동이 끝났다"고 읽힌다.
--
-- 켜는 것은 운영자가 이용 조건을 확인한 뒤 admin_set_source() 로 한다.
insert into public.content_sources
  (id, name, homepage_url, allowed_domains, provider_type, endpoint, enabled,
   can_discover, can_show_title, can_show_description, attribution_text,
   fetch_interval_minutes, terms_url, verified_note)
values
  ('naver-news', '네이버 뉴스 검색', 'https://developers.naver.com/',
   '{}', 'SEARCH_API', 'https://openapi.naver.com/v1/search/news.json', false,
   false, false, false, '네이버 뉴스 검색',
   60, 'https://developers.naver.com/products/service-api/search/search.md',
   '키(NAVER_CLIENT_ID/SECRET) 미등록. 검색 결과의 표시·보관 범위 확인 필요.'),

  ('sbs', 'SBS 뉴스', 'https://news.sbs.co.kr/',
   '{news.sbs.co.kr,sbs.co.kr}', 'RSS', 'https://news.sbs.co.kr/news/rss.do', false,
   false, false, false, 'SBS 뉴스', 60, 'https://news.sbs.co.kr/news/rss.do',
   'RSS 가 개인·비상업 이용 조건을 명시. 상업 서비스 사용은 별도 확인 필요.'),

  ('kbs', 'KBS 뉴스', 'https://news.kbs.co.kr/',
   '{news.kbs.co.kr,kbs.co.kr}', 'RSS', null, false,
   false, false, false, 'KBS 뉴스', 60, null,
   'RSS/API 주소와 이용 가능 여부 미확인.'),

  ('mbc', 'MBC 뉴스', 'https://imnews.imbc.com/',
   '{imnews.imbc.com,imbc.com}', 'RSS', null, false,
   false, false, false, 'MBC 뉴스', 60, null,
   'RSS/API 주소와 이용 가능 여부 미확인.'),

  ('jtbc', 'JTBC 뉴스', 'https://news.jtbc.co.kr/',
   '{news.jtbc.co.kr,jtbc.co.kr}', 'RSS', 'https://news.jtbc.co.kr/rss', false,
   false, false, false, 'JTBC 뉴스', 60, 'https://news.jtbc.co.kr/rss',
   'RSS 주소는 안내 페이지만 확인. 이용 조건 미확인.'),

  ('runable', '러너블 매거진', 'https://runable.me/',
   '{runable.me}', 'PARTNER_FEED', 'https://runable.me/magazine', false,
   false, false, false, '러너블', 360, null,
   '공개 개발자 API·제휴 권한 확인되지 않음. 자동 수집 비활성.'),

  ('kdca', '질병관리청 국가건강정보포털', 'https://health.kdca.go.kr/',
   '{health.kdca.go.kr,kdca.go.kr}', 'PUBLIC_DATA', null, false,
   false, false, false, '질병관리청 국가건강정보포털', 1440, null,
   '공공 건강정보. 언론 기사와 구분해 표시. 이용 범위 확인 필요.'),

  ('manual', '운영자 등록', null,
   '{}', 'MANUAL', null, true,
   false, true, true, '', 1440, null,
   '운영자가 공식 사이트에서 확인해 직접 등록한 정보.')
on conflict (id) do update set
  -- 이름·도메인·안내문은 최신으로. 켜짐 여부와 확인 기록은 **건드리지 않는다** —
  -- 운영자가 확인해서 켜 둔 것을 다시 붙여넣기 한 번에 되돌리면 안 된다.
  name = excluded.name,
  homepage_url = excluded.homepage_url,
  allowed_domains = excluded.allowed_domains,
  provider_type = excluded.provider_type,
  attribution_text = excluded.attribution_text,
  updated_at = now();
