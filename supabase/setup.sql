-- ════════════════════════════════════════════════════════════════════
--  StepUp 서버 스키마 — 전체 설치
--
--  Supabase 대시보드 → SQL Editor 에 이 파일 전체를 붙여넣고 Run 하세요.
--  한 번에 다 만들어집니다.
--
--  끝나면 왼쪽 Table Editor 에 표가 보입니다.
--
--  이 파일은 supabase/migrations/ 의 파일들을 순서대로 이어 붙인 것입니다.
--  내용을 고칠 때는 그쪽을 고치고 scripts/build-setup-sql.py 로 다시 만드세요.
-- ════════════════════════════════════════════════════════════════════

-- 이 파일은 몇 번을 다시 붙여넣어도 안전합니다. 이미 있는 것은 건너뛰고,
-- 달라진 규칙만 새로 씁니다. 기록은 지워지지 않습니다.

begin;

-- "없어서 건너뛴다"는 안내는 처음 설치할 때 잔뜩 나오는데, 문제가 아닌데도
-- 문제처럼 보입니다. 경고 이상만 보여 줍니다.
set local client_min_messages = warning;

-- ══════════════════════════════════════════════════════════════════
-- 0001_profiles.sql
-- ══════════════════════════════════════════════════════════════════

-- 계정과 프로필.
--
-- Supabase 의 auth.users 는 건드리지 않는다. 그 표는 인증이 소유하고, 우리는
-- 곁에 프로필 표를 두어 1:1로 붙인다. 이렇게 두면 인증 방식이 바뀌어도
-- (구글에서 애플로, 또는 익명 계정 추가) 프로필은 그대로 산다.

create table if not exists public.profiles (
  -- auth.users 와 같은 id 를 쓴다. 계정이 지워지면 프로필도 함께 지워진다.
  id uuid primary key references auth.users on delete cascade,

  -- 화면에 보이는 이름. 게시글 작성자로 쓰인다.
  display_name text not null default '러너',
  avatar_id int not null default 0,

  -- 앱 설정 — 기기를 바꿔도 따라오게 서버에 둔다
  daily_goal int not null default 8000 check (daily_goal between 1000 and 100000),
  language text not null default '',

  -- 연속 달성 기록
  streak int not null default 0 check (streak >= 0),
  last_goal_met_day bigint not null default -1,

  -- 랭킹 재료. 세션이 쌓일 때 서버가 갱신한다.
  top_speed_kmh double precision not null default 0 check (top_speed_kmh >= 0),
  lifetime_km double precision not null default 0 check (lifetime_km >= 0),

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table public.profiles is
  '사용자 프로필. auth.users 와 1:1. 기기를 바꿔도 유지되어야 하는 설정이 여기 있다.';

-- ── 가입하면 프로필이 자동으로 생긴다 ──────────────────────────────
--
-- 앱이 따로 만들게 하면 "계정은 있는데 프로필이 없는" 상태가 생긴다.
-- 가입 직후 앱이 죽거나 네트워크가 끊기면 바로 그렇게 된다.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, display_name)
  values (
    new.id,
    -- 구글 로그인이면 이름을 받아 온다. 없으면 기본값.
    coalesce(nullif(new.raw_user_meta_data ->> 'full_name', ''), '러너')
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- ── updated_at 자동 갱신 ───────────────────────────────────────────
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists profiles_touch_updated_at on public.profiles;
create trigger profiles_touch_updated_at
  before update on public.profiles
  for each row execute function public.touch_updated_at();

-- ── 권한 ───────────────────────────────────────────────────────────
--
-- 앱에는 anon 키가 박혀 있다. 그 키는 누구나 꺼내 볼 수 있으므로,
-- 실제 보호는 전부 아래 규칙(RLS)이 한다. RLS 를 켜지 않은 표는
-- 사실상 공개 표다.
alter table public.profiles enable row level security;

-- 프로필은 서로 볼 수 있어야 한다 — 게시글 작성자, 크루 명단, 랭킹.
drop policy if exists profiles_select_all on public.profiles;
create policy profiles_select_all
  on public.profiles for select
  using (true);

-- 고치는 것은 본인만.
drop policy if exists profiles_update_own on public.profiles;
create policy profiles_update_own
  on public.profiles for update
  using ((select auth.uid()) = id)
  with check ((select auth.uid()) = id);

-- INSERT 정책을 두지 않는다. 프로필은 위 트리거만 만든다.
-- DELETE 정책도 두지 않는다. 계정을 지우면 따라 지워진다.

-- ══════════════════════════════════════════════════════════════════
-- 0002_activity.sql
-- ══════════════════════════════════════════════════════════════════

-- 걸음과 러닝 세션.
--
-- 앱은 지금까지 이걸 폰 안에만 두었다. 기기를 바꾸면 통째로 사라졌다는 뜻이다.
-- 온체인으로 가지 않기로 했으므로 이 표가 유일한 보관처다.

-- ── 일별 걸음 ──────────────────────────────────────────────────────
create table if not exists public.daily_steps (
  user_id uuid not null references auth.users on delete cascade,
  -- LocalDate.toEpochDay() 와 같은 값. 시간대 문제를 피하려고 날짜를
  -- 타임스탬프가 아니라 "며칠째"로 센다 — 기기 시간대가 바뀌어도
  -- 어제가 어제로 남는다.
  epoch_day bigint not null,
  steps int not null default 0 check (steps >= 0),
  goal int not null default 8000 check (goal > 0),
  updated_at timestamptz not null default now(),
  primary key (user_id, epoch_day)
);

comment on table public.daily_steps is
  '하루치 걸음 합계. 러닝 세션과 별개로 걸은 것까지 포함한다.';

-- ── 러닝 세션 ──────────────────────────────────────────────────────
create table if not exists public.walk_sessions (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,

  started_at timestamptz not null,
  ended_at timestamptz not null,
  duration_sec int not null check (duration_sec >= 0),

  steps int not null check (steps >= 0),
  distance_meters double precision not null default 0 check (distance_meters >= 0),
  calories double precision not null default 0 check (calories >= 0),

  -- 시각이 붙은 GPS 경로. 앱의 RunTrack.encode 형식 그대로다
  -- ("위도,경도,시각;위도,경도,시각"). 이게 있어야 "정말 뛰었는가"를
  -- 나중에 다시 판정할 수 있다.
  track text not null default '',

  -- 정산 시점의 값. 나중에 다시 계산할 수 없어서 같이 받는다 —
  -- 사용자는 신발을 갈아신고 크루를 옮긴다.
  boost_bps int not null default 0 check (boost_bps between 0 and 2000),
  party_size int not null default 1 check (party_size between 1 and 20),

  -- 서버 판정. 클라이언트가 못 고치게 앱에는 쓰기 권한을 주지 않는다.
  verdict text not null default 'PENDING'
    check (verdict in ('PENDING', 'CLEAN', 'FLAGGED', 'VOID')),
  verdict_reason text not null default '',

  -- 서버가 계산해 실제로 지급한 SUP. 앱이 보낸 값이 아니다.
  points_awarded numeric(20, 4) not null default 0 check (points_awarded >= 0),

  -- 실제로 적립 대상이 된 걸음. 걸은 걸음(steps)과 다를 수 있다 — 하루 상한에
  -- 걸리면 일부만 인정된다. 상한을 셀 때는 반드시 이 값을 더해야 한다.
  -- steps 를 더하면 인정되지도 않은 걸음이 상한을 깎아 다음 세션이 손해를 본다.
  rewarded_steps int not null default 0 check (rewarded_steps >= 0),

  created_at timestamptz not null default now(),

  constraint walk_sessions_ends_after_start check (ended_at >= started_at)
);

comment on column public.walk_sessions.rewarded_steps is
  '적립 대상이 된 걸음. 하루 상한 계산은 이 값을 더한다.';

comment on column public.walk_sessions.points_awarded is
  '서버가 계산해 지급한 금액. 앱이 보낸 값을 그대로 믿지 않는다 — 앱은 고쳐서 다시 설치할 수 있다.';

-- 같은 세션이 두 번 올라오는 것을 막는다. 앱이 재시도할 때 응답을 못 받고
-- 다시 보내면 중복 적립이 된다. (사용자, 시작시각)이면 충분히 유일하다.
create unique index if not exists walk_sessions_user_started_unique
  on public.walk_sessions (user_id, started_at);

create index if not exists walk_sessions_user_recent
  on public.walk_sessions (user_id, started_at desc);

-- ── 첫 배포 뒤에 추가된 열 ─────────────────────────────────────────
--
-- 위의 create table 은 "없으면 만든다"라서, 이미 표가 있는 프로젝트는
-- 통째로 건너뛴다. 나중에 늘어난 열은 여기서 따로 붙여야 그런 프로젝트에도
-- 들어간다. setup.sql 을 다시 붙여넣어도 안전한 이유다.

-- 정산 시점에 신고 있던 신발의 종족. 종족 랭킹의 재료다.
alter table public.walk_sessions
  add column if not exists faction text not null default '';

-- 경로에서 서버가 직접 계산한 최고 속도. 앱이 보낸 값이 아니다 —
-- 속도 랭킹이 있는 이상, 앱이 말하는 속도를 믿으면 랭킹은 타자 연습이 된다.
alter table public.walk_sessions
  add column if not exists top_speed_kmh double precision not null default 0;

do $$
begin
  if not exists (
    select 1 from pg_constraint
     where conrelid = 'public.walk_sessions'::regclass
       and conname = 'walk_sessions_faction_known'
  ) then
    alter table public.walk_sessions
      add constraint walk_sessions_faction_known
      check (faction in ('', 'FIRE', 'WATER', 'LIGHTNING', 'WIND'));
  end if;
end $$;

comment on column public.walk_sessions.top_speed_kmh is
  '서버가 GPS 경로에서 직접 계산한 구간 최고 속도. 경로가 없으면 0.';

-- ── 권한 ───────────────────────────────────────────────────────────
alter table public.daily_steps enable row level security;
alter table public.walk_sessions enable row level security;

-- 걸음은 본인 것만 읽고 쓴다.
drop policy if exists daily_steps_own on public.daily_steps;
create policy daily_steps_own
  on public.daily_steps for all
  using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);

-- 세션은 본인 것만 읽는다.
drop policy if exists walk_sessions_select_own on public.walk_sessions;
create policy walk_sessions_select_own
  on public.walk_sessions for select
  using ((select auth.uid()) = user_id);

-- **INSERT · UPDATE · DELETE 정책을 주지 않는다.**
--
-- 세션을 앱이 직접 넣게 하면 걸음 수를 마음대로 적어 보낼 수 있다.
-- 세션은 반드시 0003 의 record_session() 을 통해서만 들어온다 —
-- 그 함수가 걸음 수를 검사하고 적립액을 직접 계산한다.

-- ══════════════════════════════════════════════════════════════════
-- 0003_ledger.sql
-- ══════════════════════════════════════════════════════════════════

-- SUP 원장 — 이 파일이 v1 의 심장이다.
--
-- SUP 는 이제 앱 안 포인트지만, 그렇다고 클라이언트가 마음대로 적어도 되는
-- 값은 아니다. 앱에는 anon 키가 박혀 있고 그 키는 누구나 꺼내 볼 수 있다.
-- 원장에 직접 INSERT 할 수 있으면 잔고는 숫자놀음이 된다.
--
-- 그래서 원장은 **읽기만** 허용하고, 모든 기록은 아래 두 함수로만 들어온다.
--   - record_session()  적립. 서버가 걸음 수를 검사하고 금액을 직접 계산한다.
--   - spend_sup()       소비. 잔고를 확인하고 차감한다.

create table if not exists public.sup_ledger (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,

  occurred_at timestamptz not null default now(),

  -- 앱의 RewardType 과 같은 값
  kind text not null check (kind in (
    'EARN_WALK', 'EARN_PARTY', 'EARN_EVENT', 'BONUS_GOAL',
    'SPEND_MINT', 'SPEND_UPGRADE', 'SPEND_BOOST'
  )),

  -- 양수 = 적립, 음수 = 사용. 소수 4자리까지 — 0.01 SUP/보 단위를 담기에 충분하고,
  -- double 과 달리 더해도 오차가 쌓이지 않는다. 잔고는 더한 값이므로 중요하다.
  amount numeric(20, 4) not null check (amount <> 0),

  description text not null default '',

  -- 적립이면 어느 세션에서 왔는지. 한 세션은 한 번만 적립된다.
  session_id bigint references public.walk_sessions on delete set null,

  created_at timestamptz not null default now()
);

comment on table public.sup_ledger is
  'SUP 적립·사용 원장. 잔고는 이 표의 합이다. 직접 쓰기는 막혀 있고 함수로만 들어온다.';

create index if not exists sup_ledger_user_recent on public.sup_ledger (user_id, occurred_at desc);

-- 한 세션이 두 번 적립되는 것을 막는다.
create unique index if not exists sup_ledger_session_once
  on public.sup_ledger (session_id)
  where session_id is not null;

-- ── 잔고 ───────────────────────────────────────────────────────────
--
-- 표가 아니라 합계로 둔다. 잔고를 따로 들고 있으면 원장과 어긋날 수 있고,
-- 어긋났을 때 어느 쪽이 맞는지 알 방법이 없다. 합이 곧 잔고라면 어긋날 수가 없다.
create or replace view public.sup_balances
with (security_invoker = true) as
  select user_id, coalesce(sum(amount), 0)::numeric(20, 4) as balance
    from public.sup_ledger
   group by user_id;

comment on view public.sup_balances is
  '원장의 합. 잔고를 따로 저장하지 않는 이유는 어긋날 수 없게 하기 위해서다.';

-- ── 경제 규칙 (앱의 RewardEconomy 와 같은 값) ──────────────────────
--
-- 앱과 서버 양쪽에 같은 숫자가 있는 것은 좋지 않지만, 서버가 앱을 믿지 않으려면
-- 서버도 계산할 줄 알아야 한다. 값이 어긋나면 앱이 보여준 금액과 실제 적립이
-- 달라지므로, 앱의 RewardEconomy.kt 를 고치면 여기도 고친다.
create schema if not exists economy;

create or replace function economy.points_per_step() returns numeric
  language sql immutable as $$ select 0.01::numeric $$;

create or replace function economy.steps_per_energy() returns int
  language sql immutable as $$ select 600 $$;

create or replace function economy.base_max_energy() returns numeric
  language sql immutable as $$ select 10::numeric $$;

-- 하루에 적립 대상이 될 수 있는 걸음의 절대 상한.
-- 에너지 규칙이 이미 더 낮게 묶지만, 규칙이 틀려도 피해가 이 선을 넘지 않게 하는
-- 마지막 방어선이다.
create or replace function economy.max_daily_steps() returns int
  language sql immutable as $$ select 48000 $$;

-- 파티런 보너스 — 본인 제외 1명당 +10%, 5명까지
create or replace function economy.party_multiplier(party_size int) returns numeric
  language sql immutable as $$
  select 1 + 0.10 * least(greatest(coalesce(party_size, 1) - 1, 0), 5)
$$;

-- 스니커즈 부스트 — bps 를 배율로. 1780 → 1.178
create or replace function economy.boost_multiplier(boost_bps int) returns numeric
  language sql immutable as $$
  select 1 + least(greatest(coalesce(boost_bps, 0), 0), 2000)::numeric / 10000
$$;

-- ── GPS 경로에서 속도 읽기 ─────────────────────────────────────────
--
-- 속도 랭킹이 있는 이상, 앱이 말하는 속도를 그대로 받으면 그 랭킹은 달리기가
-- 아니라 타자 실력을 재게 된다. 경로는 이미 올라오니 서버가 직접 잰다.

create or replace function economy.haversine_m(
  lat1 double precision, lng1 double precision,
  lat2 double precision, lng2 double precision
) returns double precision
language sql immutable as $$
  select 2 * 6371000 * asin(least(1, sqrt(
    power(sin(radians(lat2 - lat1) / 2), 2)
      + cos(radians(lat1)) * cos(radians(lat2))
      * power(sin(radians(lng2 - lng1) / 2), 2)
  )))
$$;

-- 한 구간으로 볼 최소 시간(초). 1초짜리 구간으로 재면 GPS 가 몇 미터 튀는
-- 것만으로 시속 수십 km 가 나온다. 10초면 그 흔들림이 묻힌다.
create or replace function economy.speed_window_sec() returns int
  language sql immutable as $$ select 10 $$;

-- 이 이상은 사람의 이동으로 보지 않는다. 사람이 낸 최고 기록이 약 37km/h 이므로
-- 60 은 넉넉히 위다 — 여기 걸리는 구간은 달린 게 아니라 튄 것이거나 탄 것이다.
create or replace function economy.speed_glitch_kmh() returns double precision
  language sql immutable as $$ select 60::double precision $$;

-- 랭킹에 올릴 수 있는 상한. 튀지 않았더라도 세계 기록 위는 기록으로 받지 않는다.
create or replace function economy.speed_record_cap_kmh() returns double precision
  language sql immutable as $$ select 45::double precision $$;

drop function if exists economy.track_speed_stats(text);

/*
 * 경로에서 구간 최고 속도와 "말이 안 되는 구간"의 비율을 뽑는다.
 *
 * 둘을 함께 돌려주는 이유는 판단이 다르기 때문이다. 터널이나 빌딩 사이에서
 * 좌표 하나가 튀는 것은 흔한 일이라 그 구간 하나로 세션 전체를 버리면 정직한
 * 기록이 사라진다. 하지만 구간 대부분이 말이 안 되면 그건 GPS 오류가 아니라
 * 그 사람이 달리지 않은 것이다.
 */
create or replace function economy.track_speed_stats(p_track text)
returns table (top_speed_kmh double precision, glitch_ratio double precision)
language plpgsql immutable as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_chunk text;
  v_parts text[];
  v_lat double precision;
  v_lng double precision;
  v_at bigint;
  -- 비교 기준점. 여기서부터 speed_window_sec 초가 지나야 한 구간으로 친다.
  v_blat double precision;
  v_blng double precision;
  v_bat bigint;
  v_have_base boolean := false;
  v_dt double precision;
  v_kmh double precision;
  v_best double precision := 0;
  v_windows int := 0;
  v_glitches int := 0;
begin
  if p_track is null or p_track = '' then
    return query select 0::double precision, 0::double precision;
    return;
  end if;

  foreach v_chunk in array string_to_array(p_track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    -- 깨진 조각은 건너뛴다. 한 점이 깨졌다고 나머지 경로를 버릴 이유는 없다.
    continue when v_parts is null or array_length(v_parts, 1) <> 3;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num or v_parts[3] !~ '^[0-9]+$';

    v_lat := v_parts[1]::double precision;
    v_lng := v_parts[2]::double precision;
    v_at := v_parts[3]::bigint;  -- epoch 밀리초. 앱의 RunTrack 과 같은 단위다.

    if not v_have_base then
      v_blat := v_lat; v_blng := v_lng; v_bat := v_at; v_have_base := true;
      continue;
    end if;

    v_dt := (v_at - v_bat) / 1000.0;
    -- 시계가 거꾸로 간 점은 기준을 다시 잡는다.
    if v_dt <= 0 then
      v_blat := v_lat; v_blng := v_lng; v_bat := v_at;
      continue;
    end if;
    continue when v_dt < economy.speed_window_sec();

    v_kmh := economy.haversine_m(v_blat, v_blng, v_lat, v_lng) / v_dt * 3.6;
    v_windows := v_windows + 1;
    if v_kmh > economy.speed_glitch_kmh() then
      v_glitches := v_glitches + 1;
    elsif v_kmh > v_best then
      v_best := v_kmh;
    end if;

    v_blat := v_lat; v_blng := v_lng; v_bat := v_at;
  end loop;

  return query select
    least(v_best, economy.speed_record_cap_kmh()),
    case when v_windows = 0 then 0::double precision
         else v_glitches::double precision / v_windows end;
end;
$$;

comment on function economy.track_speed_stats is
  '경로에서 구간 최고 속도와 비정상 구간 비율을 뽑는다. 앱이 보낸 속도는 쓰지 않는다.';

-- ── 지난 버전의 함수를 치운다 ──────────────────────────────────────
--
-- 인자가 늘면 create or replace 로는 못 바꾸고 같은 이름이 둘이 된다. 그러면
-- 호출이 어느 쪽으로 갈지 모호해져 앱이 조용히 옛 규칙으로 적립될 수 있다.
do $$
declare r record;
begin
  for r in
    select oid::regprocedure as sig
      from pg_proc
     where pronamespace = 'public'::regnamespace
       and proname in ('record_session', 'spend_sup')
  loop
    execute 'drop function ' || r.sig;
  end loop;
end $$;

-- ── 적립 ───────────────────────────────────────────────────────────
--
-- 앱이 세션을 올리면 이 함수가 받는다. 앱이 계산한 금액은 받지도 않는다 —
-- 걸음 수와 정산 시점 값만 받아서 서버가 직접 계산한다.
create or replace function public.record_session(
  p_started_at timestamptz,
  p_ended_at timestamptz,
  p_steps int,
  p_duration_sec int,
  p_track text default '',
  p_boost_bps int default 0,
  p_party_size int default 1,
  -- 정산 시점에 신고 있던 신발의 종족. 종족 랭킹에 쌓인다.
  p_faction text default ''
)
returns table (
  session_id bigint,
  verdict text,
  points_awarded numeric,
  balance numeric
)
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_day bigint;
  v_already_today int;
  v_rewardable int;
  v_points numeric(20, 4);
  v_verdict text := 'CLEAN';
  v_reason text := '';
  v_session_id bigint;
  v_kind text;
  v_elapsed int;
  v_faction text;
  v_top_speed double precision := 0;
  v_glitch double precision := 0;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  -- ── 형식 검사 ──
  if p_steps is null or p_steps < 0 then
    raise exception '걸음 수가 올바르지 않습니다' using errcode = '22023';
  end if;
  if p_ended_at < p_started_at then
    raise exception '종료 시각이 시작보다 빠릅니다' using errcode = '22023';
  end if;
  -- 미래에서 온 세션은 받지 않는다. 기기 시계를 앞당기면 내일 몫을 오늘 받는다.
  if p_ended_at > now() + interval '5 minutes' then
    raise exception '종료 시각이 미래입니다' using errcode = '22023';
  end if;

  v_elapsed := greatest(coalesce(p_duration_sec, 0), 0);
  v_day := floor(extract(epoch from p_started_at) / 86400)::bigint;
  v_faction := case when coalesce(p_faction, '') in ('FIRE', 'WATER', 'LIGHTNING', 'WIND')
                    then p_faction else '' end;

  -- 경로에서 속도를 직접 잰다. 경로가 없으면 0 이고, 그 세션은 속도 랭킹에
  -- 올라가지 않는다 — 잴 수 없는 기록은 기록이 아니다.
  select t.top_speed_kmh, t.glitch_ratio
    into v_top_speed, v_glitch
    from economy.track_speed_stats(coalesce(p_track, '')) t;

  -- ── 판정 ──
  --
  -- 사람의 이동으로 보기 어려운 세션은 적립하지 않는다. 여기서는 케이던스만
  -- 본다 — GPS 경로 검사는 좌표를 하나씩 훑어야 해서 Edge Function 쪽이 맞고,
  -- 케이던스는 나눗셈 한 번이라 여기서 끝난다.
  if v_elapsed >= 60 and p_steps::numeric * 60 / v_elapsed > 240 then
    v_verdict := 'VOID';
    v_reason := '케이던스가 사람 범위를 벗어납니다';

  -- 구간 대부분이 사람 속도를 넘으면 GPS 오류가 아니라 타고 간 것이다.
  -- 절반이라는 선은 넉넉하다 — 도심에서 좌표가 튀는 일은 흔해도, 구간의
  -- 절반이 시속 60km 를 넘는 일은 흔하지 않다.
  elsif v_glitch > 0.5 then
    v_verdict := 'VOID';
    v_reason := '이동 속도가 사람 범위를 벗어납니다';
  end if;

  -- ── 적립 대상 걸음 ──
  if v_verdict = 'VOID' then
    v_rewardable := 0;
  else
    -- 오늘 이미 **적립된** 걸음을 빼고 남은 만큼만 인정한다.
    -- 걸은 걸음(steps)이 아니라 적립된 걸음(rewarded_steps)이어야 한다 —
    -- 상한에 걸려 인정되지 않은 몫까지 깎으면 다음 세션이 손해를 본다.
    select coalesce(sum(s.rewarded_steps), 0) into v_already_today
      from public.walk_sessions s
     where s.user_id = v_user
       and s.verdict in ('CLEAN', 'FLAGGED')
       and floor(extract(epoch from s.started_at) / 86400)::bigint = v_day;

    v_rewardable := greatest(
      least(p_steps, economy.max_daily_steps() - v_already_today),
      0
    );

    if v_rewardable < p_steps then
      v_verdict := 'FLAGGED';
      v_reason := '하루 적립 상한에 걸렸습니다';
    end if;
  end if;

  v_points := round(
    v_rewardable * economy.points_per_step()
      * economy.party_multiplier(p_party_size)
      * economy.boost_multiplier(p_boost_bps),
    4
  );

  -- ── 기록 ──
  --
  -- 같은 세션을 다시 올리면(앱이 응답을 못 받고 재시도) 새로 적립하지 않고
  -- 원래 결과를 그대로 돌려준다. 재시도가 중복 적립이 되면 안 된다.
  insert into public.walk_sessions (
    user_id, started_at, ended_at, duration_sec, steps,
    distance_meters, calories, track, boost_bps, party_size,
    faction, top_speed_kmh,
    verdict, verdict_reason, points_awarded, rewarded_steps
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    p_steps * 0.762, p_steps * 0.04, coalesce(p_track, ''), p_boost_bps, p_party_size,
    v_faction, case when v_verdict = 'VOID' then 0 else v_top_speed end,
    v_verdict, v_reason, v_points, v_rewardable
  )
  on conflict (user_id, started_at) do nothing
  returning id into v_session_id;

  if v_session_id is null then
    -- 이미 처리된 세션이다. 원래 결과를 돌려준다.
    select s.id, s.verdict, s.points_awarded
      into v_session_id, v_verdict, v_points
      from public.walk_sessions s
     where s.user_id = v_user and s.started_at = p_started_at;

    return query
      select v_session_id, v_verdict, v_points,
             coalesce((select b.balance from public.sup_balances b where b.user_id = v_user), 0);
    return;
  end if;

  if v_points > 0 then
    v_kind := case when coalesce(p_party_size, 1) > 1 then 'EARN_PARTY' else 'EARN_WALK' end;
    insert into public.sup_ledger (user_id, kind, amount, description, session_id, occurred_at)
    values (v_user, v_kind, v_points, format('러닝 세션 적립 (%s보)', v_rewardable),
            v_session_id, p_ended_at);
  end if;

  -- 랭킹 재료 갱신.
  -- VOID 판정을 받은 세션은 아무것도 남기지 않는다 — 적립을 막아 놓고
  -- 기록만 올려 주면 속도 랭킹은 그쪽으로 뚫린다.
  if v_verdict <> 'VOID' then
    update public.profiles
       set lifetime_km = lifetime_km + (p_steps * 0.762 / 1000),
           top_speed_kmh = greatest(top_speed_kmh, v_top_speed)
     where id = v_user;
  end if;

  return query
    select v_session_id, v_verdict, v_points,
           coalesce((select b.balance from public.sup_balances b where b.user_id = v_user), 0);
end;
$$;

comment on function public.record_session is
  '러닝 세션을 기록하고 적립액을 서버가 직접 계산한다. 앱이 계산한 금액은 받지 않는다.';

-- ── 소비 ───────────────────────────────────────────────────────────
create or replace function public.spend_sup(
  p_kind text,
  p_amount numeric,
  p_description text default ''
)
returns numeric
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_balance numeric(20, 4);
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_kind not in ('SPEND_MINT', 'SPEND_UPGRADE', 'SPEND_BOOST') then
    raise exception '소비 종류가 올바르지 않습니다: %', p_kind using errcode = '22023';
  end if;
  if p_amount is null or p_amount <= 0 then
    raise exception '금액이 올바르지 않습니다' using errcode = '22023';
  end if;

  -- 잔고를 세는 동안 다른 요청이 끼어들지 못하게 이 사용자의 원장 행을 잠근다.
  -- 잠그지 않으면 두 요청이 동시에 "잔고 충분"을 보고 둘 다 통과해 잔고가
  -- 음수가 된다 — 지갑이 없어도 이중지불은 일어난다.
  perform 1 from public.sup_ledger where user_id = v_user for update;

  select coalesce(sum(amount), 0) into v_balance
    from public.sup_ledger where user_id = v_user;

  if v_balance < p_amount then
    raise exception 'SUP가 부족합니다 (보유 %, 필요 %)', v_balance, p_amount
      using errcode = '23514';
  end if;

  insert into public.sup_ledger (user_id, kind, amount, description)
  values (v_user, p_kind, -p_amount, coalesce(p_description, ''));

  return v_balance - p_amount;
end;
$$;

comment on function public.spend_sup is
  'SUP 를 차감한다. 잔고를 확인하고, 동시 요청이 겹쳐 음수가 되지 않게 잠근다.';

-- ── 권한 ───────────────────────────────────────────────────────────
alter table public.sup_ledger enable row level security;

-- 본인 원장만 읽는다.
drop policy if exists sup_ledger_select_own on public.sup_ledger;
create policy sup_ledger_select_own
  on public.sup_ledger for select
  using ((select auth.uid()) = user_id);

-- 쓰기 정책은 두지 않는다. 위 두 함수(security definer)만 쓴다.
--
-- 정책이 없으면 RLS 가 이미 막지만, 권한 자체도 회수해 둔다. 나중에 누군가
-- 실수로 허용 정책을 하나 붙여도 권한이 없으면 여전히 못 쓴다. 잔고가 걸린
-- 표라 방어를 한 겹 더 둔다.
revoke insert, update, delete on public.sup_ledger from anon, authenticated;
revoke insert, update, delete on public.walk_sessions from anon, authenticated;

grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text)
  to authenticated;
grant execute on function public.spend_sup(text, numeric, text) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0004_community.sql
-- ══════════════════════════════════════════════════════════════════

-- 커뮤니티 — 크루, 게시판, 댓글, 코스.
--
-- 지금까지 이 데이터는 전부 폰 안에만 있었다. 그래서 "커뮤니티"라고 부르면서도
-- 실제로는 혼자 쓰는 메모장이었다 — 내가 쓴 글을 아무도 볼 수 없었다.
-- 이 파일이 그걸 진짜 공용 공간으로 바꾼다.
--
-- 앱의 Room 스키마와 한 군데가 다르다. 폰 안에서는 `liked`, `joined`, `mine`
-- 같은 값이 글에 붙은 칸이었지만, 서버에서는 그럴 수 없다. 같은 글이라도
-- 누가 보느냐에 따라 답이 달라지기 때문이다. 그래서 그 값들은 칸이 아니라
-- 별도의 표(post_likes, flash_participants)가 되고, 보는 사람 기준으로
-- 계산해서 내려준다.

-- ════════════════════════════════════════════════════════════════════
--  크루
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.crews (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references auth.users on delete cascade,

  name text not null check (length(name) between 1 and 40),
  monogram text not null default '' check (length(monogram) <= 4),
  tagline text not null default '' check (length(tagline) <= 120),
  area text not null default '' check (length(area) <= 60),

  -- 활동 중심지. "몇 km 떨어져 있나"는 보는 사람 위치가 있어야 나오므로
  -- 서버는 좌표만 들고 있고 거리는 앱이 계산한다.
  lat double precision,
  lng double precision,

  created_at timestamptz not null default now()
);

comment on table public.crews is '러닝 크루. 만든 사람이 자동으로 첫 멤버가 된다.';

create table if not exists public.crew_members (
  crew_id uuid not null references public.crews on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  role text not null default 'MEMBER' check (role in ('OWNER', 'MEMBER')),
  joined_at timestamptz not null default now(),
  primary key (crew_id, user_id)
);

create index if not exists crew_members_user on public.crew_members (user_id);

-- 크루를 만든 사람은 그 자리에서 멤버가 된다.
--
-- 앱이 만들기와 가입을 따로 호출하게 두면, 둘 사이에서 앱이 죽었을 때
-- "주인이 멤버가 아닌 크루"가 남는다. 그 크루는 주인조차 글을 못 쓴다.
create or replace function public.handle_new_crew()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.crew_members (crew_id, user_id, role)
  values (new.id, new.owner_id, 'OWNER')
  on conflict do nothing;
  return new;
end;
$$;

drop trigger if exists on_crew_created on public.crews;
create trigger on_crew_created
  after insert on public.crews
  for each row execute function public.handle_new_crew();

-- ════════════════════════════════════════════════════════════════════
--  차단과 신고
--
--  사용자가 글을 쓸 수 있는 앱은 스토어 정책상 신고와 차단 수단이 있어야
--  한다. 그보다 먼저, 이게 없으면 한 사람이 공간 전체를 망칠 수 있다.
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.user_blocks (
  blocker_id uuid not null references auth.users on delete cascade,
  blocked_id uuid not null references auth.users on delete cascade,
  created_at timestamptz not null default now(),
  primary key (blocker_id, blocked_id),
  constraint user_blocks_not_self check (blocker_id <> blocked_id)
);

comment on table public.user_blocks is
  '차단 목록. 차단하면 그 사람의 글·댓글이 내 화면에서 사라진다. 상대에게는 알리지 않는다.';

create table if not exists public.content_reports (
  id bigint generated always as identity primary key,
  reporter_id uuid not null references auth.users on delete cascade,
  target_type text not null check (target_type in ('POST', 'COMMENT', 'CREW', 'COURSE', 'USER')),
  -- 대상 id. 표마다 자료형이 달라(uuid/bigint) 문자열로 받는다.
  target_id text not null,
  reason text not null check (reason in ('SPAM', 'ABUSE', 'SEXUAL', 'DANGER', 'FRAUD', 'OTHER')),
  note text not null default '' check (length(note) <= 1000),
  status text not null default 'OPEN' check (status in ('OPEN', 'REVIEWED', 'ACTIONED', 'DISMISSED')),
  created_at timestamptz not null default now(),
  -- 같은 사람이 같은 대상을 반복 신고해도 한 건이다.
  unique (reporter_id, target_type, target_id)
);

comment on table public.content_reports is
  '신고 접수함. 처리는 사람이 대시보드에서 한다 — 자동 삭제는 오판했을 때 되돌릴 수 없다.';

-- 차단했는지. 정책과 뷰 양쪽에서 쓰므로 함수로 둔다.
create or replace function public.is_blocked(p_user uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.user_blocks b
     where b.blocker_id = auth.uid() and b.blocked_id = p_user
  )
$$;

-- ════════════════════════════════════════════════════════════════════
--  게시판
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.posts (
  id bigint generated always as identity primary key,
  author_id uuid not null references auth.users on delete cascade,

  category text not null check (category in ('FLASH', 'FREE', 'TIP')),

  -- 비어 있으면(null) 전체 게시판, 값이 있으면 그 크루만 보는 게시판.
  crew_id uuid references public.crews on delete cascade,

  title text not null check (length(title) between 1 and 120),
  body text not null default '' check (length(body) <= 4000),

  -- ── 번개러닝 전용 ──
  place text not null default '' check (length(place) <= 80),
  distance_km double precision not null default 0 check (distance_km >= 0),
  meet_at timestamptz,
  capacity int not null default 0 check (capacity between 0 and 200),

  created_at timestamptz not null default now()
);

create index if not exists posts_feed on public.posts (crew_id, created_at desc);
create index if not exists posts_author on public.posts (author_id);
-- 번개는 "지금부터 가까운 순"으로 보므로 따로 잡아 둔다.
create index if not exists posts_flash_upcoming on public.posts (meet_at)
  where category = 'FLASH';

create table if not exists public.post_likes (
  post_id bigint not null references public.posts on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  created_at timestamptz not null default now(),
  primary key (post_id, user_id)
);

create table if not exists public.flash_participants (
  post_id bigint not null references public.posts on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  joined_at timestamptz not null default now(),
  primary key (post_id, user_id)
);

comment on table public.flash_participants is
  '번개러닝 참가자. 정원을 넘지 않게 join_flash() 로만 들어온다 — 직접 INSERT 는 막혀 있다.';

create table if not exists public.comments (
  id bigint generated always as identity primary key,
  post_id bigint not null references public.posts on delete cascade,
  -- null 이면 최상위 댓글. 앱의 parentId=0 과 같은 뜻이다.
  parent_id bigint references public.comments on delete cascade,
  author_id uuid not null references auth.users on delete cascade,
  body text not null check (length(body) between 1 and 1000),
  created_at timestamptz not null default now()
);

create index if not exists comments_post on public.comments (post_id, created_at);

-- ════════════════════════════════════════════════════════════════════
--  코스
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.courses (
  id bigint generated always as identity primary key,
  owner_id uuid not null references auth.users on delete cascade,

  name text not null check (length(name) between 1 and 60),
  area text not null default '' check (length(area) <= 60),
  distance_km double precision not null default 0 check (distance_km >= 0),
  elevation_m int not null default 0,

  -- 앱의 RunCourse.encode 형식 — "위도,경도;위도,경도". 러닝 경로와 달리
  -- 시각이 없다. 코스는 "언제 지났나"가 아니라 "어디를 지나나"이기 때문이다.
  track text not null default '',

  -- 코스 게시판에 올렸는지. 안 올린 코스는 나만 본다.
  shared boolean not null default false,
  run_count int not null default 0 check (run_count >= 0),

  created_at timestamptz not null default now()
);

create index if not exists courses_shared on public.courses (shared, created_at desc);
create index if not exists courses_owner on public.courses (owner_id);

create table if not exists public.course_likes (
  course_id bigint not null references public.courses on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  created_at timestamptz not null default now(),
  primary key (course_id, user_id)
);

-- ════════════════════════════════════════════════════════════════════
--  누가 무엇을 볼 수 있나
-- ════════════════════════════════════════════════════════════════════

-- 크루 멤버인지. 정책 안에서 여러 번 쓰이므로 함수로 둔다.
-- 크루가 없는 글(전체 게시판)은 누구나 본다.
create or replace function public.is_crew_member(p_crew uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select p_crew is null or exists (
    select 1 from public.crew_members m
     where m.crew_id = p_crew and m.user_id = auth.uid()
  )
$$;

-- 이 글을 볼 수 있는지. 댓글·좋아요 정책이 글의 공개 범위를 따라가야 한다 —
-- 크루 글은 안 보이는데 그 댓글은 보이면 담장에 구멍이 난 것이다.
create or replace function public.can_see_post(p_post bigint)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.posts p
     where p.id = p_post and public.is_crew_member(p.crew_id)
  )
$$;

alter table public.crews enable row level security;
alter table public.crew_members enable row level security;
alter table public.user_blocks enable row level security;
alter table public.content_reports enable row level security;
alter table public.posts enable row level security;
alter table public.post_likes enable row level security;
alter table public.flash_participants enable row level security;
alter table public.comments enable row level security;
alter table public.courses enable row level security;
alter table public.course_likes enable row level security;

-- ── 크루 ──
-- 크루 목록은 가입하기 전에 보여야 한다. 안 보이면 가입할 수가 없다.
drop policy if exists crews_select_all on public.crews;
create policy crews_select_all on public.crews for select using (true);

drop policy if exists crews_insert_own on public.crews;
create policy crews_insert_own on public.crews for insert
  with check ((select auth.uid()) = owner_id);

drop policy if exists crews_update_owner on public.crews;
create policy crews_update_owner on public.crews for update
  using ((select auth.uid()) = owner_id)
  with check ((select auth.uid()) = owner_id);

drop policy if exists crews_delete_owner on public.crews;
create policy crews_delete_owner on public.crews for delete
  using ((select auth.uid()) = owner_id);

-- ── 크루 멤버 ──
drop policy if exists crew_members_select_all on public.crew_members;
create policy crew_members_select_all on public.crew_members for select using (true);

-- 가입은 본인만, 역할은 MEMBER 로만. OWNER 를 직접 넣을 수 있으면
-- 남의 크루에 주인 행세를 하는 행이 생긴다.
drop policy if exists crew_members_join_self on public.crew_members;
create policy crew_members_join_self on public.crew_members for insert
  with check ((select auth.uid()) = user_id and role = 'MEMBER');

-- 탈퇴는 본인만. 주인은 못 나간다 — 나가면 주인 없는 크루가 남는다.
-- 정리하려면 크루를 지워야 한다.
drop policy if exists crew_members_leave_self on public.crew_members;
create policy crew_members_leave_self on public.crew_members for delete
  using ((select auth.uid()) = user_id and role <> 'OWNER');

-- ── 차단·신고 ──
drop policy if exists user_blocks_own on public.user_blocks;
create policy user_blocks_own on public.user_blocks for all
  using ((select auth.uid()) = blocker_id)
  with check ((select auth.uid()) = blocker_id);

-- 신고는 넣을 수만 있고, 내가 넣은 것만 보인다. 처리 상태를 앱이 바꿀 수는 없다.
drop policy if exists content_reports_insert_own on public.content_reports;
create policy content_reports_insert_own on public.content_reports for insert
  with check ((select auth.uid()) = reporter_id);

drop policy if exists content_reports_select_own on public.content_reports;
create policy content_reports_select_own on public.content_reports for select
  using ((select auth.uid()) = reporter_id);

revoke update, delete on public.content_reports from anon, authenticated;

-- ── 게시글 ──
drop policy if exists posts_select_visible on public.posts;
create policy posts_select_visible on public.posts for select
  using (public.is_crew_member(crew_id));

drop policy if exists posts_insert_own on public.posts;
create policy posts_insert_own on public.posts for insert
  with check (
    (select auth.uid()) = author_id
    -- 안 들어간 크루의 게시판에는 쓸 수 없다.
    and (crew_id is null or public.is_crew_member(crew_id))
  );

drop policy if exists posts_update_own on public.posts;
create policy posts_update_own on public.posts for update
  using ((select auth.uid()) = author_id)
  with check ((select auth.uid()) = author_id);

drop policy if exists posts_delete_own on public.posts;
create policy posts_delete_own on public.posts for delete
  using ((select auth.uid()) = author_id);

-- ── 좋아요 ──
-- 누가 눌렀는지는 모두 볼 수 있다. 개수를 세려면 그래야 하고, 좋아요는
-- 원래 드러내는 행동이다.
drop policy if exists post_likes_select_all on public.post_likes;
create policy post_likes_select_all on public.post_likes for select using (true);

drop policy if exists post_likes_insert_own on public.post_likes;
create policy post_likes_insert_own on public.post_likes for insert
  with check ((select auth.uid()) = user_id and public.can_see_post(post_id));

drop policy if exists post_likes_delete_own on public.post_likes;
create policy post_likes_delete_own on public.post_likes for delete
  using ((select auth.uid()) = user_id);

-- ── 번개 참가 ──
-- 읽기만 열어 둔다. 넣고 빼는 것은 아래 join_flash / leave_flash 만 한다 —
-- 정원은 직접 INSERT 로는 지킬 수 없다.
drop policy if exists flash_participants_select_all on public.flash_participants;
create policy flash_participants_select_all on public.flash_participants for select using (true);

revoke insert, update, delete on public.flash_participants from anon, authenticated;

-- ── 댓글 ──
drop policy if exists comments_select_visible on public.comments;
create policy comments_select_visible on public.comments for select
  using (public.can_see_post(post_id));

drop policy if exists comments_insert_own on public.comments;
create policy comments_insert_own on public.comments for insert
  with check ((select auth.uid()) = author_id and public.can_see_post(post_id));

drop policy if exists comments_delete_own on public.comments;
create policy comments_delete_own on public.comments for delete
  using ((select auth.uid()) = author_id);

-- 댓글은 고칠 수 없다. 대화가 오간 뒤에 앞말이 바뀌면 뒷말이 뜻을 잃는다.

-- ── 코스 ──
drop policy if exists courses_select_shared_or_own on public.courses;
create policy courses_select_shared_or_own on public.courses for select
  using (shared or (select auth.uid()) = owner_id);

drop policy if exists courses_insert_own on public.courses;
create policy courses_insert_own on public.courses for insert
  with check ((select auth.uid()) = owner_id);

drop policy if exists courses_update_own on public.courses;
create policy courses_update_own on public.courses for update
  using ((select auth.uid()) = owner_id)
  with check ((select auth.uid()) = owner_id);

drop policy if exists courses_delete_own on public.courses;
create policy courses_delete_own on public.courses for delete
  using ((select auth.uid()) = owner_id);

drop policy if exists course_likes_select_all on public.course_likes;
create policy course_likes_select_all on public.course_likes for select using (true);

drop policy if exists course_likes_insert_own on public.course_likes;
create policy course_likes_insert_own on public.course_likes for insert
  with check ((select auth.uid()) = user_id);

drop policy if exists course_likes_delete_own on public.course_likes;
create policy course_likes_delete_own on public.course_likes for delete
  using ((select auth.uid()) = user_id);

-- ════════════════════════════════════════════════════════════════════
--  번개러닝 참가 — 정원이 있는 일은 함수로만
-- ════════════════════════════════════════════════════════════════════

create or replace function public.join_flash(p_post_id bigint)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_capacity int;
  v_meet_at timestamptz;
  v_crew uuid;
  v_count int;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  -- 이 글을 잠근다. 두 사람이 마지막 한 자리를 동시에 노리면, 잠그지 않는 한
  -- 둘 다 "아직 자리 있음"을 보고 둘 다 들어간다. 정원 10명인 모임에 11명이
  -- 나타나는 일은 그렇게 생긴다.
  select p.capacity, p.meet_at, p.crew_id
    into v_capacity, v_meet_at, v_crew
    from public.posts p
   where p.id = p_post_id and p.category = 'FLASH'
     for update;

  if not found then
    raise exception '번개러닝 글을 찾을 수 없습니다' using errcode = 'P0002';
  end if;
  if not public.is_crew_member(v_crew) then
    raise exception '이 크루의 멤버가 아닙니다' using errcode = '42501';
  end if;
  if v_meet_at is not null and v_meet_at < now() then
    raise exception '이미 지난 모임입니다' using errcode = '23514';
  end if;

  insert into public.flash_participants (post_id, user_id)
  values (p_post_id, v_user)
  on conflict do nothing;

  select count(*) into v_count
    from public.flash_participants f where f.post_id = p_post_id;

  -- 정원 0 은 제한 없음이다.
  if v_capacity > 0 and v_count > v_capacity then
    raise exception '정원이 찼습니다 (%명)', v_capacity using errcode = '23514';
  end if;

  return v_count;
end;
$$;

comment on function public.join_flash is
  '번개러닝에 참가한다. 정원을 넘지 않게 글을 잠그고 센다.';

create or replace function public.leave_flash(p_post_id bigint)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_count int;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  delete from public.flash_participants
   where post_id = p_post_id and user_id = v_user;

  select count(*) into v_count
    from public.flash_participants f where f.post_id = p_post_id;
  return v_count;
end;
$$;

grant execute on function public.join_flash(bigint) to authenticated;
grant execute on function public.leave_flash(bigint) to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  앱이 읽는 모양
--
--  앱은 글 하나를 그릴 때 좋아요 수, 댓글 수, 참가 인원, 그리고 "내가"
--  눌렀는지까지 필요하다. 그걸 앱이 매번 따로 물으면 목록 한 번에 요청이
--  수십 개가 된다. 여기서 한 줄로 만들어 둔다.
--
--  security_invoker = true 는 "이 뷰를 읽는 사람의 권한으로 본다"는 뜻이다.
--  이게 없으면 뷰가 RLS 를 통째로 우회해, 안 보여야 할 크루 글이 뷰를 통해
--  새어 나간다.
-- ════════════════════════════════════════════════════════════════════

drop view if exists public.post_feed;
create view public.post_feed
with (security_invoker = true) as
  select
    p.id,
    p.category,
    p.crew_id,
    p.author_id,
    coalesce(pr.display_name, '러너') as author,
    p.title,
    p.body,
    p.place,
    p.distance_km,
    p.meet_at,
    p.capacity,
    p.created_at,
    (select count(*) from public.post_likes l where l.post_id = p.id) as likes,
    (select count(*) from public.comments c where c.post_id = p.id) as comment_count,
    (select count(*) from public.flash_participants f where f.post_id = p.id) as joined_count,
    exists (
      select 1 from public.post_likes l
       where l.post_id = p.id and l.user_id = auth.uid()
    ) as liked,
    exists (
      select 1 from public.flash_participants f
       where f.post_id = p.id and f.user_id = auth.uid()
    ) as joined,
    p.author_id = auth.uid() as mine
  from public.posts p
  left join public.profiles pr on pr.id = p.author_id
  -- 차단한 사람의 글은 내 화면에서 사라진다. 상대는 이를 알 수 없다 —
  -- 알리면 차단이 다툼의 시작이 된다.
  where not public.is_blocked(p.author_id);

comment on view public.post_feed is
  '게시글 목록. 좋아요·댓글·참가 수와 "내가 눌렀는지"까지 한 줄에 담는다.';

drop view if exists public.comment_feed;
create view public.comment_feed
with (security_invoker = true) as
  select
    c.id,
    c.post_id,
    coalesce(c.parent_id, 0) as parent_id,
    c.author_id,
    coalesce(pr.display_name, '러너') as author,
    c.body,
    c.created_at,
    c.author_id = auth.uid() as mine
  from public.comments c
  left join public.profiles pr on pr.id = c.author_id
  where not public.is_blocked(c.author_id);

drop view if exists public.crew_feed;
create view public.crew_feed
with (security_invoker = true) as
  select
    c.id,
    c.owner_id,
    c.name,
    c.monogram,
    c.tagline,
    c.area,
    c.lat,
    c.lng,
    c.created_at,
    (select count(*) from public.crew_members m where m.crew_id = c.id) as member_count,
    exists (
      select 1 from public.crew_members m
       where m.crew_id = c.id and m.user_id = auth.uid()
    ) as joined,
    c.owner_id = auth.uid() as owned
  from public.crews c;

drop view if exists public.course_feed;
create view public.course_feed
with (security_invoker = true) as
  select
    c.id,
    c.owner_id,
    coalesce(pr.display_name, '러너') as author,
    c.name,
    c.area,
    c.distance_km,
    c.elevation_m,
    c.track,
    c.shared,
    c.run_count,
    c.created_at,
    (select count(*) from public.course_likes l where l.course_id = c.id) as likes,
    exists (
      select 1 from public.course_likes l
       where l.course_id = c.id and l.user_id = auth.uid()
    ) as liked,
    c.owner_id = auth.uid() as mine
  from public.courses c
  left join public.profiles pr on pr.id = c.owner_id
  where not public.is_blocked(c.owner_id);

grant select on public.post_feed, public.comment_feed, public.crew_feed, public.course_feed
  to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0005_ranking.sql
-- ══════════════════════════════════════════════════════════════════

-- 랭킹.
--
-- 지금 앱의 순위표는 코드에 박아 둔 가상의 러너 15명과 나를 섞은 것이다.
-- 그 화면이 처음부터 하려던 말은 "당신은 지금 몇 등입니다"인데, 상대가 가짜면
-- 그 말은 거짓이다. 이 파일이 그 자리에 진짜 사람을 넣는다.
--
-- ── 여기만 다른 규칙: 뷰가 RLS 를 지나간다 ──
--
-- 다른 곳에서는 뷰에 security_invoker 를 붙여 "읽는 사람 권한으로" 보게 했다.
-- 랭킹은 반대여야 한다. 남의 세션은 RLS 가 막는 게 맞고, 그렇지만 순위는
-- 남들과 비교해야 나온다. 그래서 여기 뷰는 소유자 권한으로 돌아 RLS 를 지나고,
-- 대신 **합계 말고는 아무것도 내보내지 않는다.** 원본 행 — 언제 어디를 뛰었는지 —
-- 은 여전히 본인만 본다.

-- ════════════════════════════════════════════════════════════════════
--  러너별 합계
-- ════════════════════════════════════════════════════════════════════

drop view if exists public.runner_stats cascade;
create view public.runner_stats as
  select
    p.id as user_id,
    p.display_name,
    p.avatar_id,
    -- 서버가 GPS 경로에서 직접 잰 값. 앱이 보낸 속도가 아니다.
    p.top_speed_kmh,
    coalesce(a.active_sec, 0)::bigint as active_sec,
    coalesce(a.km, 0)::double precision as km,
    coalesce(l.earned, 0)::numeric(20, 4) as sup
  from public.profiles p
  left join (
    select
      s.user_id,
      sum(s.duration_sec) as active_sec,
      sum(s.distance_meters) / 1000.0 as km
    from public.walk_sessions s
    -- 판정에서 떨어진 세션은 순위에 쓰지 않는다. 적립은 막아 놓고 순위는
    -- 올려 주면, 순위표는 막지 않은 쪽으로 뚫린다.
    where s.verdict <> 'VOID'
    group by s.user_id
  ) a on a.user_id = p.id
  left join (
    -- 누적 "적립"이다. 잔고가 아니다 — 쓴 사람이 순위에서 밀리면
    -- 상점은 아무도 안 쓰는 방이 된다.
    select user_id, sum(amount) filter (where amount > 0) as earned
      from public.sup_ledger
     group by user_id
  ) l on l.user_id = p.id;

comment on view public.runner_stats is
  '러너별 합계. 소유자 권한으로 돌아 RLS 를 지나므로 앱에는 직접 열어 주지 않는다 — 아래 함수로만 나간다.';

-- 앱에서 직접 읽지 못하게 한다. 이 뷰를 그대로 열면 전체 사용자 목록을
-- 통째로 받아 갈 수 있다. 순위에 필요한 건 상위 몇 명과 내 줄뿐이다.
revoke all on public.runner_stats from anon, authenticated;

-- ── 이름 약자 ──
-- 순위표의 동그라미 안에 들어갈 두 글자. "Maya C." → MC, "김러너" → 김러.
create or replace function public.monogram_of(p_name text)
returns text
language sql
immutable
as $$
  select case
    when coalesce(trim(p_name), '') = '' then '??'
    when array_length(regexp_split_to_array(trim(p_name), '\s+'), 1) >= 2 then
      upper(
        substr((regexp_split_to_array(trim(p_name), '\s+'))[1], 1, 1) ||
        substr((regexp_split_to_array(trim(p_name), '\s+'))[2], 1, 1)
      )
    else upper(substr(trim(p_name), 1, 2))
  end
$$;

-- ════════════════════════════════════════════════════════════════════
--  개인 순위
-- ════════════════════════════════════════════════════════════════════

drop function if exists public.leaderboard(text, int);

/*
 * 상위 몇 명과 **내 줄**을 함께 돌려준다.
 *
 * 내 줄을 끼워 주는 것이 핵심이다. 상위 20명만 주면 300등인 사람은 자기가
 * 어디 있는지 영영 모른다. 그러면 순위표는 남의 이야기가 된다.
 *
 * @param p_board TOP_SPEED(최고 속도) · LONGEST_TIME(누적 시간) · TOTAL_SUP(누적 적립)
 */
create or replace function public.leaderboard(
  p_board text default 'TOP_SPEED',
  p_limit int default 20
)
returns table (
  rank int,
  user_id uuid,
  name text,
  monogram text,
  top_speed_kmh double precision,
  active_sec bigint,
  sup numeric,
  is_me boolean,
  -- 순위에 오른 전체 인원. "300명 중 47등"의 300 이다. 상위 몇 명만 받으면
  -- 앱은 이 수를 알 방법이 없어서, 받은 줄 수를 전체인 양 보여주게 된다.
  total int
)
language sql
stable
security definer
set search_path = public
as $$
  with board as (
    select upper(coalesce(nullif(trim(p_board), ''), 'TOP_SPEED')) as kind
  ),
  ranked as (
    select
      s.*,
      count(*) over ()::int as total,
      rank() over (
        order by
          case (select kind from board)
            when 'LONGEST_TIME' then s.active_sec::numeric
            when 'TOTAL_SUP' then s.sup
            else s.top_speed_kmh::numeric
          end desc,
          -- 같은 값이면 이름순. 순서가 매번 흔들리면 순위표를 믿지 않게 된다.
          s.display_name,
          s.user_id
      )::int as rnk
    from public.runner_stats s
    -- 아직 한 번도 안 뛴 사람은 순위에 넣지 않는다. 0 으로 채운 줄이
    -- 수백 개면 순위표가 아니라 가입자 명단이다.
    where s.active_sec > 0 or s.sup > 0 or s.top_speed_kmh > 0
  )
  select
    r.rnk,
    r.user_id,
    r.display_name,
    public.monogram_of(r.display_name),
    r.top_speed_kmh,
    r.active_sec,
    r.sup,
    r.user_id = auth.uid(),
    r.total
  from ranked r
  where r.rnk <= greatest(coalesce(p_limit, 20), 1)
     or r.user_id = auth.uid()
  order by r.rnk
$$;

-- 인자 목록을 적어 둔다. 뒤에 나오는 0006 이 인자를 하나 더한 같은 이름의
-- 함수를 만들기 때문에, 이 파일을 다시 돌릴 때 이름만으로는 어느 쪽인지
-- 가릴 수 없다.
comment on function public.leaderboard(text, int) is
  '상위 p_limit 명과 내 줄. 300등이어도 자기 자리가 보여야 순위표가 내 이야기가 된다.';

-- ════════════════════════════════════════════════════════════════════
--  종족 순위
--
--  개인 순위가 "나 vs 남"이라면 이건 "우리 vs 저쪽"이다. 내가 1등을 못 해도
--  우리 종족은 1등일 수 있다.
-- ════════════════════════════════════════════════════════════════════

drop function if exists public.faction_leaderboard();

create or replace function public.faction_leaderboard()
returns table (
  faction text,
  km double precision,
  my_km double precision,
  runners int
)
language sql
stable
security definer
set search_path = public
as $$
  select
    f.faction,
    coalesce(sum(s.distance_meters), 0) / 1000.0,
    coalesce(sum(s.distance_meters) filter (where s.user_id = auth.uid()), 0) / 1000.0,
    count(distinct s.user_id)::int
  from (values ('FIRE'), ('WATER'), ('LIGHTNING'), ('WIND')) as f(faction)
  left join public.walk_sessions s
    on s.faction = f.faction and s.verdict <> 'VOID'
  group by f.faction
  order by 2 desc, 1
$$;

comment on function public.faction_leaderboard() is
  '종족별 누적 거리와 그중 내 몫. 신발을 고르는 일이 소속을 정하는 일이 된다.';

grant execute on function public.leaderboard(text, int) to authenticated;
grant execute on function public.faction_leaderboard() to authenticated;
grant execute on function public.monogram_of(text) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0006_ranking_period.sql
-- ══════════════════════════════════════════════════════════════════

-- 랭킹의 기간.
--
-- 전체기간만 있으면 순위표는 일찍 시작한 사람의 명단이 된다. 어제 가입한
-- 사람이 아무리 달려도 3년 치 누적을 따라잡을 수 없고, 따라잡을 수 없는
-- 순위표는 두 번 보지 않는다. 오늘·이번 주·이번 달은 누구에게나 0부터다.
--
-- 기간을 "월요일 0시"처럼 달력으로 끊지 않고 최근 N일로 잡았다. 달력으로
-- 끊으면 일요일 밤에 올린 기록이 몇 시간 만에 순위에서 사라지고, 사용자는
-- 기록이 지워졌다고 생각한다. 앱의 RankPeriod.sinceMillis 와 같은 규칙이다.

-- ── 기간의 시작 ────────────────────────────────────────────────────
--
-- now() 를 쓰므로 immutable 이 아니라 stable 이다. 한 질의 안에서는 같은
-- 값이고, 그것이면 충분하다.
create or replace function public.rank_period_start(p_period text default 'ALL')
returns timestamptz
language sql
stable
as $$
  select case upper(coalesce(nullif(trim(p_period), ''), 'ALL'))
    when 'DAY' then now() - interval '1 day'
    when 'WEEK' then now() - interval '7 days'
    when 'MONTH' then now() - interval '30 days'
    -- 전체기간. 어떤 시각과 비교해도 참이 되는 값이라 조건을 따로 두지 않아도 된다.
    else '-infinity'::timestamptz
  end
$$;

comment on function public.rank_period_start(text) is
  '랭킹 기간(DAY·WEEK·MONTH·ALL)의 시작 시각. 달력이 아니라 최근 N일로 끊는다.';

-- ════════════════════════════════════════════════════════════════════
--  개인 순위 — 기간을 받는다
-- ════════════════════════════════════════════════════════════════════

-- 기간을 세는 순위는 runner_stats(전체기간 합계)로는 만들 수 없다. 창을
-- 받아 그 안의 세션만 다시 모은다. 전체기간이면 창이 '-infinity' 라서
-- 예전과 같은 답이 나온다.
--
-- 인자가 하나 늘었으므로 예전 서명을 지운다. 그냥 두면 2개 인자로 부르는
-- 쪽은 기간을 모르는 옛 함수로 계속 간다.
drop function if exists public.leaderboard(text, int);

create or replace function public.leaderboard(
  p_board text default 'TOP_SPEED',
  p_limit int default 20,
  p_period text default 'ALL'
)
returns table (
  rank int,
  user_id uuid,
  name text,
  monogram text,
  top_speed_kmh double precision,
  active_sec bigint,
  sup numeric,
  is_me boolean,
  total int
)
language sql
stable
security definer
set search_path = public
as $$
  with win as (
    select public.rank_period_start(p_period) as since
  ),
  board as (
    select upper(coalesce(nullif(trim(p_board), ''), 'TOP_SPEED')) as kind
  ),
  stats as (
    select
      p.id as user_id,
      p.display_name,
      -- 창 안의 세션 중 가장 빨랐던 구간. 전체기간일 때만 프로필에 적힌
      -- 역대 최고와 견준다 — 세션 표가 생기기 전에 세워진 기록도 있다.
      greatest(
        coalesce(a.top_speed_kmh, 0),
        case
          when (select since from win) = '-infinity'::timestamptz then p.top_speed_kmh
          else 0
        end
      )::double precision as top_speed_kmh,
      coalesce(a.active_sec, 0)::bigint as active_sec,
      coalesce(l.earned, 0)::numeric(20, 4) as sup
    from public.profiles p
    left join (
      select
        s.user_id,
        max(s.top_speed_kmh) as top_speed_kmh,
        sum(s.duration_sec) as active_sec
      from public.walk_sessions s
      -- 판정에서 떨어진 세션은 순위에 쓰지 않는다. 적립은 막아 놓고 순위는
      -- 올려 주면, 순위표는 막지 않은 쪽으로 뚫린다.
      where s.verdict <> 'VOID'
        and s.started_at >= (select since from win)
      group by s.user_id
    ) a on a.user_id = p.id
    left join (
      -- 누적 "적립"이다. 잔고가 아니다 — 쓴 사람이 순위에서 밀리면
      -- 상점은 아무도 안 쓰는 방이 된다.
      select user_id, sum(amount) filter (where amount > 0) as earned
        from public.sup_ledger
       where occurred_at >= (select since from win)
       group by user_id
    ) l on l.user_id = p.id
  ),
  ranked as (
    select
      s.*,
      count(*) over ()::int as total,
      rank() over (
        order by
          case (select kind from board)
            when 'LONGEST_TIME' then s.active_sec::numeric
            when 'TOTAL_SUP' then s.sup
            else s.top_speed_kmh::numeric
          end desc,
          -- 같은 값이면 이름순. 순서가 매번 흔들리면 순위표를 믿지 않게 된다.
          s.display_name,
          s.user_id
      )::int as rnk
    from stats s
    -- 이 기간에 아무것도 안 한 사람은 순위에 넣지 않는다. 0 으로 채운 줄이
    -- 수백 개면 순위표가 아니라 가입자 명단이다.
    where s.active_sec > 0 or s.sup > 0 or s.top_speed_kmh > 0
  )
  select
    r.rnk,
    r.user_id,
    r.display_name,
    public.monogram_of(r.display_name),
    r.top_speed_kmh,
    r.active_sec,
    r.sup,
    r.user_id = auth.uid(),
    r.total
  from ranked r
  where r.rnk <= greatest(coalesce(p_limit, 20), 1)
     or r.user_id = auth.uid()
  order by r.rnk
$$;

comment on function public.leaderboard(text, int, text) is
  '상위 p_limit 명과 내 줄을, p_period(DAY·WEEK·MONTH·ALL) 기간으로. 300등이어도 자기 자리가 보여야 순위표가 내 이야기가 된다.';

-- ════════════════════════════════════════════════════════════════════
--  종족 순위 — 기간을 받는다
-- ════════════════════════════════════════════════════════════════════

drop function if exists public.faction_leaderboard();

create or replace function public.faction_leaderboard(p_period text default 'ALL')
returns table (
  faction text,
  km double precision,
  my_km double precision,
  runners int
)
language sql
stable
security definer
set search_path = public
as $$
  select
    f.faction,
    coalesce(sum(s.distance_meters), 0) / 1000.0,
    coalesce(sum(s.distance_meters) filter (where s.user_id = auth.uid()), 0) / 1000.0,
    count(distinct s.user_id)::int
  from (values ('FIRE'), ('WATER'), ('LIGHTNING'), ('WIND')) as f(faction)
  left join public.walk_sessions s
    on s.faction = f.faction
   and s.verdict <> 'VOID'
   and s.started_at >= public.rank_period_start(p_period)
  group by f.faction
  order by 2 desc, 1
$$;

comment on function public.faction_leaderboard(text) is
  '종족별 누적 거리와 그중 내 몫을, p_period 기간으로. 신발을 고르는 일이 소속을 정하는 일이 된다.';

grant execute on function public.rank_period_start(text) to authenticated;
grant execute on function public.leaderboard(text, int, text) to authenticated;
grant execute on function public.faction_leaderboard(text) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0007_market.sql
-- ══════════════════════════════════════════════════════════════════

-- NFT 마켓 — 러너끼리 스니커즈를 사고파는 자리.
--
-- ── 왜 서버에 두는가 ──
--
-- 거래는 두 사람 사이의 일이다. 내 폰 안에서만 벌어지는 거래는 거래가
-- 아니라 화면 연출이다. 매물도 입찰도 체결도 한 곳에 있어야 상대가 볼 수
-- 있고, 그 한 곳은 서버다.
--
-- 돈이 오가므로 앱을 믿을 수 없다. 앱에는 anon 키가 박혀 있고 누구나 꺼내
-- 볼 수 있다. 그래서 이 파일의 표는 **읽기만** 열려 있고, 모든 변화는 아래
-- 함수로만 들어온다. 원장(0003)과 같은 태도다.
--
-- ── 거래 방식 ──
--
-- 두 가지를 합쳤다.
--
--   * 모델 단위 시세 (KREAM 식)
--     - 같은 모델(속성×등급×변형)끼리 한 줄로 묶어 시세를 본다.
--     - 판매 입찰 = 내 신발 한 켤레에 값을 건다 → 가장 싼 값이 "즉시 구매가"
--     - 구매 입찰 = 모델에 값을 건다(켤레 지정 없음) → 가장 비싼 값이 "즉시 판매가"
--
--   * 아이템 단위 장부 (오픈시·블러 식)
--     - 매물마다 민팅 번호·레벨·스탯이 다르므로 줄마다 그것을 보여 준다.
--     - 체결 내역이 남아 시세 그래프가 된다.
--
-- 스니커즈는 레벨에 따라 값이 다르다. 같은 모델이라고 아무 켤레나 같은 값에
-- 팔리지는 않으므로, **구매 입찰에는 최소 레벨 조건**을 붙인다. 이것이
-- KREAM 의 "같은 모델은 같은 물건" 가정을 이 앱에 맞게 고친 부분이다.
--
-- ── 체결 규칙 ──
--
-- 먼저 걸린 쪽(메이커)의 값으로 체결한다. 거래소의 기본 규칙이고, 나중에
-- 들어온 쪽이 값을 밀어 올리거나 내리지 못하게 한다.
--
-- 자기 매물을 자기가 사는 것은 막는다. 혼자서 값을 지어내는 것(자전거래)을
-- 막기 위해서다. 그래서 **시험하려면 계정이 둘 필요하다**.

-- ══════════════════════════════════════════════════════════════════
-- 경제 규칙
-- ══════════════════════════════════════════════════════════════════

-- 거래 수수료 (bp, 10000 = 100%). 파는 쪽에서 뗀다.
-- 이 SUP 는 어디로도 가지 않고 사라진다 — 원장에서 빠지므로 총량이 준다.
-- 뛰어서 버는 쪽으로 무게를 두기 위한 것이다.
create or replace function economy.market_fee_bps() returns int
  language sql immutable as $$ select 250 $$;

-- 한 계정이 **기존에 갖고 있던** 스니커즈를 거래소에 올릴 수 있는 최대 수.
--
-- 지금까지 스니커즈는 폰 안에만 있었다. 그것을 거래소에 올리려면 서버가
-- 앱의 말을 믿는 수밖에 없다 — 앱이 "나 전설 신발 있어"라고 하면 확인할
-- 방법이 없다. 그래서 수를 묶는다.
--
-- 이 구멍은 **SUP 에 실제 가치가 붙기 전에 반드시 닫아야 한다.** 닫는 길은
-- 민팅을 서버에서 하는 것이다(서버가 뽑고, 서버가 적어 두면 믿을 근거가
-- 생긴다). 지금은 SUP 가 앱 안의 점수라 이 선에서 멈춘다.
create or replace function economy.market_import_cap() returns int
  language sql immutable as $$ select 30 $$;

-- 값을 붙일 수 있는 최소 단위. 0.0001 SUP 짜리 매물로 장부를 채우는 것을 막는다.
create or replace function economy.market_min_price() returns numeric
  language sql immutable as $$ select 1::numeric $$;

-- ══════════════════════════════════════════════════════════════════
-- 원장에 거래 항목을 더한다
-- ══════════════════════════════════════════════════════════════════
--
-- 0003 은 `create table if not exists` 라 이미 만들어진 표의 제약은 바뀌지
-- 않는다. 항목을 늘리려면 제약을 다시 걸어야 한다.
--
--   ESCROW_LOCK    구매 입찰을 걸 때 잠근다 (음수)
--   ESCROW_UNLOCK  입찰을 거둘 때 푼다 (양수)
--   TRADE_BUY      살 때 낸 값 (음수)
--   TRADE_SELL     팔아서 받은 값 (양수)
--   TRADE_FEE      수수료 (음수)
--
-- 잠금을 따로 두지 않고 원장의 음수로 적는 이유는, 잔고가 곧 원장의 합이기
-- 때문이다. 잠근 만큼 잔고가 줄어 있으므로 잠긴 돈을 두 번 쓸 수 없다.
alter table public.sup_ledger drop constraint if exists sup_ledger_kind_check;
alter table public.sup_ledger add constraint sup_ledger_kind_check check (kind in (
  'EARN_WALK', 'EARN_PARTY', 'EARN_EVENT', 'BONUS_GOAL',
  'SPEND_MINT', 'SPEND_UPGRADE', 'SPEND_BOOST',
  'ESCROW_LOCK', 'ESCROW_UNLOCK', 'TRADE_BUY', 'TRADE_SELL', 'TRADE_FEE'
));

-- ══════════════════════════════════════════════════════════════════
-- 등록부 — 거래소가 아는 스니커즈
-- ══════════════════════════════════════════════════════════════════

-- 민팅 번호는 거래소 전체에서 하나씩 늘어난다. 폰 안의 번호는 그 폰에서만
-- 1, 2, 3 이라 다른 사람 것과 겹친다. 장부에 올리는 순간 전체에서 유일한
-- 번호가 필요하다.
create sequence if not exists public.market_mint_seq start 1;

create table if not exists public.market_sneakers (
  id bigint generated always as identity primary key,

  owner_id uuid not null references auth.users on delete cascade,

  faction text not null check (faction in ('FIRE', 'WATER', 'LIGHTNING', 'WIND')),
  rarity text not null check (rarity in ('COMMON', 'RARE', 'EPIC', 'LEGENDARY')),
  variant int not null check (variant between 0 and 10),

  level int not null check (level between 1 and 30),
  luck numeric(6, 3) not null default 1,
  comfort numeric(6, 3) not null default 1,
  durability int not null check (durability between 0 and 100),

  mint_number bigint not null default nextval('public.market_mint_seq'),

  -- IMPORT = 폰에 있던 것을 올렸다, MINT = 서버에서 뽑았다(아직 없음)
  origin text not null default 'IMPORT' check (origin in ('IMPORT', 'MINT')),

  -- OWNED = 그냥 갖고 있다, LISTED = 팔려고 내놨다
  status text not null default 'OWNED' check (status in ('OWNED', 'LISTED')),

  created_at timestamptz not null default now()
);

comment on table public.market_sneakers is
  '거래소에 올라온 스니커즈. 소유자는 이 표가 정한다 — 폰 안의 목록은 이것의 사본이다.';

create index if not exists market_sneakers_owner on public.market_sneakers (owner_id);
create index if not exists market_sneakers_model on public.market_sneakers (faction, rarity, variant);

-- 올린 기록. 소유자가 바뀌어도 "누가 몇 개를 올렸는지"는 여기 남는다.
--
-- 표를 따로 두는 이유: 팔고 나면 market_sneakers 의 owner_id 가 바뀌므로
-- 거기서는 셀 수 없다. 팔았다고 같은 신발을 다시 올릴 수 있으면 상한이
-- 아무 뜻도 없어진다.
create table if not exists public.market_imports (
  user_id uuid not null references auth.users on delete cascade,
  -- 폰 안 sneakers 표의 id
  local_id bigint not null,
  sneaker_id bigint not null references public.market_sneakers on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, local_id)
);

comment on table public.market_imports is
  '폰의 신발을 거래소에 올린 기록. 같은 신발을 두 번 올리는 것과 상한 우회를 막는다.';

-- ══════════════════════════════════════════════════════════════════
-- 판매 입찰 (매물)
-- ══════════════════════════════════════════════════════════════════

create table if not exists public.market_listings (
  id bigint generated always as identity primary key,
  sneaker_id bigint not null references public.market_sneakers on delete cascade,
  seller_id uuid not null references auth.users on delete cascade,
  price numeric(20, 4) not null check (price > 0),
  status text not null default 'OPEN' check (status in ('OPEN', 'SOLD', 'CANCELLED')),
  created_at timestamptz not null default now(),
  closed_at timestamptz
);

comment on table public.market_listings is '판매 입찰. 한 켤레에 값을 건다.';

-- 한 켤레는 한 번에 한 곳에만 걸린다. 두 곳에 걸면 둘 다 팔려 한 켤레가
-- 두 사람에게 간다.
create unique index if not exists market_listing_one_open
  on public.market_listings (sneaker_id) where status = 'OPEN';

create index if not exists market_listings_seller on public.market_listings (seller_id, status);

-- ══════════════════════════════════════════════════════════════════
-- 구매 입찰
-- ══════════════════════════════════════════════════════════════════

create table if not exists public.market_bids (
  id bigint generated always as identity primary key,
  buyer_id uuid not null references auth.users on delete cascade,

  -- 켤레가 아니라 모델에 건다
  faction text not null check (faction in ('FIRE', 'WATER', 'LIGHTNING', 'WIND')),
  rarity text not null check (rarity in ('COMMON', 'RARE', 'EPIC', 'LEGENDARY')),
  variant int not null check (variant between 0 and 10),

  -- 레벨이 값을 가르므로 조건을 붙일 수 있게 한다
  min_level int not null default 1 check (min_level between 1 and 30),

  price numeric(20, 4) not null check (price > 0),
  status text not null default 'OPEN' check (status in ('OPEN', 'FILLED', 'CANCELLED')),
  created_at timestamptz not null default now(),
  closed_at timestamptz
);

comment on table public.market_bids is
  '구매 입찰. 켤레를 고르지 않고 모델과 최소 레벨로 건다. 건 값은 원장에서 잠긴다.';

create index if not exists market_bids_model
  on public.market_bids (faction, rarity, variant, price desc) where status = 'OPEN';
create index if not exists market_bids_buyer on public.market_bids (buyer_id, status);

-- ══════════════════════════════════════════════════════════════════
-- 체결 내역
-- ══════════════════════════════════════════════════════════════════

create table if not exists public.market_trades (
  id bigint generated always as identity primary key,
  sneaker_id bigint references public.market_sneakers on delete set null,

  -- 모델과 레벨을 여기에도 적어 둔다. 신발이 지워져도 시세는 남아야 한다.
  faction text not null,
  rarity text not null,
  variant int not null,
  level int not null,

  seller_id uuid references auth.users on delete set null,
  buyer_id uuid references auth.users on delete set null,

  price numeric(20, 4) not null,
  fee numeric(20, 4) not null default 0,

  -- 누가 먼저 걸려 있었는지. BY_ASK = 매물이 메이커, BY_BID = 구매 입찰이 메이커
  kind text not null check (kind in ('BY_ASK', 'BY_BID')),

  traded_at timestamptz not null default now()
);

comment on table public.market_trades is '체결 내역. 시세 그래프의 원천이다.';

create index if not exists market_trades_model
  on public.market_trades (faction, rarity, variant, traded_at desc);
create index if not exists market_trades_user
  on public.market_trades (traded_at desc);

-- ══════════════════════════════════════════════════════════════════
-- 권한 — 읽기는 모두, 쓰기는 함수만
-- ══════════════════════════════════════════════════════════════════

alter table public.market_sneakers enable row level security;
alter table public.market_imports  enable row level security;
alter table public.market_listings enable row level security;
alter table public.market_bids     enable row level security;
alter table public.market_trades   enable row level security;

-- 장부는 모두에게 보여야 시세가 된다. 남의 신발이라도 무엇이 얼마에 걸려
-- 있는지는 봐야 살지 말지 정한다.
do $$ begin
  create policy market_sneakers_read on public.market_sneakers for select using (true);
exception when duplicate_object then null; end $$;

do $$ begin
  create policy market_listings_read on public.market_listings for select using (true);
exception when duplicate_object then null; end $$;

do $$ begin
  create policy market_bids_read on public.market_bids for select using (true);
exception when duplicate_object then null; end $$;

do $$ begin
  create policy market_trades_read on public.market_trades for select using (true);
exception when duplicate_object then null; end $$;

-- 올린 기록은 내 것만 본다. 남이 몇 개를 올렸는지는 시세와 상관없다.
do $$ begin
  create policy market_imports_read on public.market_imports
    for select using (user_id = auth.uid());
exception when duplicate_object then null; end $$;

-- INSERT/UPDATE/DELETE 정책은 하나도 만들지 않는다. RLS 가 켜진 표에
-- 정책이 없으면 그 동작은 아무에게도 허용되지 않는다 — 그것이 목적이다.

-- ══════════════════════════════════════════════════════════════════
-- 보는 자리 — 화면이 그대로 받아 쓰는 모양
-- ══════════════════════════════════════════════════════════════════

-- 시세판 한 줄 = 모델 하나.
--
-- 아무 일도 없는 모델은 빼둔다. 44줄을 전부 띄우면 41줄이 빈칸이라
-- 무엇이 살아 있는 장인지 알 수 없다.
create or replace view public.market_quotes
with (security_invoker = true) as
with supply as (
  select faction, rarity, variant,
         count(*)::int as supply,
         count(*) filter (where status = 'LISTED')::int as listed
    from public.market_sneakers group by 1, 2, 3
),
asks as (
  select s.faction, s.rarity, s.variant, min(l.price) as ask
    from public.market_listings l
    join public.market_sneakers s on s.id = l.sneaker_id
   where l.status = 'OPEN' group by 1, 2, 3
),
bids as (
  select faction, rarity, variant, max(price) as bid, count(*)::int as bid_count
    from public.market_bids where status = 'OPEN' group by 1, 2, 3
),
last_trade as (
  select distinct on (faction, rarity, variant)
         faction, rarity, variant, price as last_price, traded_at as last_traded_at
    from public.market_trades
   order by faction, rarity, variant, traded_at desc
),
vol as (
  select faction, rarity, variant,
         count(*)::int as trades_24h,
         coalesce(sum(price), 0)::numeric(20, 4) as volume_24h
    from public.market_trades
   where traded_at > now() - interval '24 hours'
   group by 1, 2, 3
),
models as (
  select faction, rarity, variant from supply
  union
  select faction, rarity, variant from bids
)
select m.faction,
       m.rarity,
       m.variant,
       coalesce(s.supply, 0) as supply,
       coalesce(s.listed, 0) as listed,
       a.ask,
       b.bid,
       coalesce(b.bid_count, 0) as bid_count,
       t.last_price,
       t.last_traded_at,
       coalesce(v.trades_24h, 0) as trades_24h,
       coalesce(v.volume_24h, 0)::numeric(20, 4) as volume_24h
  from models m
  left join supply s using (faction, rarity, variant)
  left join asks a using (faction, rarity, variant)
  left join bids b using (faction, rarity, variant)
  left join last_trade t using (faction, rarity, variant)
  left join vol v using (faction, rarity, variant);

comment on view public.market_quotes is
  '모델별 시세 한 줄 — 즉시 구매가(ask) · 즉시 판매가(bid) · 최근 체결가.';

-- 매물 장부. 켤레마다 다른 값(레벨·민팅번호)을 함께 실어 보낸다.
create or replace view public.market_asks
with (security_invoker = true) as
  select l.id as listing_id,
         l.price,
         l.created_at,
         l.seller_id,
         s.id as sneaker_id,
         s.faction, s.rarity, s.variant,
         s.level, s.mint_number, s.luck, s.comfort, s.durability
    from public.market_listings l
    join public.market_sneakers s on s.id = l.sneaker_id
   where l.status = 'OPEN';

comment on view public.market_asks is '열려 있는 매물. 화면의 "판매 입찰" 목록.';

create or replace view public.market_bid_book
with (security_invoker = true) as
  select id as bid_id, buyer_id, faction, rarity, variant, min_level, price, created_at
    from public.market_bids
   where status = 'OPEN';

comment on view public.market_bid_book is '열려 있는 구매 입찰. 화면의 "구매 입찰" 목록.';

-- ══════════════════════════════════════════════════════════════════
-- 함수
-- ══════════════════════════════════════════════════════════════════

-- 로그인 확인 — 함수마다 같은 문장을 쓰지 않으려고 뺐다.
create or replace function public.market_me() returns uuid
language plpgsql stable security definer set search_path = public as $$
declare v uuid := auth.uid();
begin
  if v is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return v;
end $$;

-- 체결 한 번. 이 함수만이 소유자를 바꾸고 원장을 쓴다.
--
-- @param p_escrowed 산 쪽이 이미 입찰로 돈을 잠가 두었는가.
--   잠가 둔 돈은 이미 잔고에서 빠져 있으므로 여기서 또 빼면 두 번 낸다.
create or replace function public.market_settle(
  p_sneaker_id bigint,
  p_seller uuid,
  p_buyer uuid,
  p_price numeric,
  p_kind text,
  p_escrowed boolean
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_fee numeric(20, 4) := round(p_price * economy.market_fee_bps() / 10000.0, 4);
  v_trade bigint;
  v_s record;
begin
  select * into v_s from public.market_sneakers where id = p_sneaker_id for update;
  if not found then
    raise exception '없는 스니커즈입니다' using errcode = '22023';
  end if;

  update public.market_sneakers
     set owner_id = p_buyer, status = 'OWNED'
   where id = p_sneaker_id;

  -- 산 쪽: 잠가 둔 돈이 아니면 여기서 낸다
  if not p_escrowed then
    insert into public.sup_ledger (user_id, kind, amount, description)
    values (p_buyer, 'TRADE_BUY', -p_price, '스니커즈 구매 #' || v_s.mint_number);
  end if;

  -- 판 쪽: 값을 받고 수수료를 뗀다. 두 줄로 적어야 무엇을 얼마나 뗐는지 보인다.
  insert into public.sup_ledger (user_id, kind, amount, description)
  values (p_seller, 'TRADE_SELL', p_price, '스니커즈 판매 #' || v_s.mint_number);

  if v_fee > 0 then
    insert into public.sup_ledger (user_id, kind, amount, description)
    values (p_seller, 'TRADE_FEE', -v_fee, '거래 수수료');
  end if;

  insert into public.market_trades
    (sneaker_id, faction, rarity, variant, level, seller_id, buyer_id, price, fee, kind)
  values
    (p_sneaker_id, v_s.faction, v_s.rarity, v_s.variant, v_s.level,
     p_seller, p_buyer, p_price, v_fee, p_kind)
  returning id into v_trade;

  return v_trade;
end $$;

-- 함수만 부를 수 있어야 한다. 아무나 체결을 지어내면 안 된다.
revoke all on function public.market_settle(bigint, uuid, uuid, numeric, text, boolean) from public;

comment on function public.market_settle(bigint, uuid, uuid, numeric, text, boolean) is
  '체결 한 번 — 소유자 이전, 원장 기록, 체결 내역. 다른 함수 안에서만 불린다.';

-- ── 등록 ──────────────────────────────────────────────────────────
--
-- 폰에 있던 신발을 거래소가 알게 한다. 이미 올린 것이면 레벨만 갱신한다
-- (강화하면 값이 오르므로 장부의 레벨도 따라가야 한다).
create or replace function public.market_import(
  p_local_id bigint,
  p_faction text,
  p_rarity text,
  p_variant int,
  p_level int,
  p_luck numeric,
  p_comfort numeric,
  p_durability int
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_id bigint;
  v_owner uuid;
  v_count int;
begin
  select sneaker_id into v_id from public.market_imports
   where user_id = v_user and local_id = p_local_id;

  if v_id is not null then
    select owner_id into v_owner from public.market_sneakers where id = v_id;
    -- 이미 판 신발이면 손대지 않는다. 지금 주인의 것이다.
    if v_owner = v_user then
      update public.market_sneakers
         set level = greatest(level, p_level),
             durability = p_durability
       where id = v_id;
    end if;
    return v_id;
  end if;

  select count(*) into v_count from public.market_imports where user_id = v_user;
  if v_count >= economy.market_import_cap() then
    raise exception '올릴 수 있는 수를 넘었습니다 (최대 %)', economy.market_import_cap()
      using errcode = '22023';
  end if;

  insert into public.market_sneakers
    (owner_id, faction, rarity, variant, level, luck, comfort, durability)
  values
    (v_user, p_faction, p_rarity, p_variant, p_level, p_luck, p_comfort, p_durability)
  returning id into v_id;

  insert into public.market_imports (user_id, local_id, sneaker_id)
  values (v_user, p_local_id, v_id);

  return v_id;
end $$;

comment on function public.market_import(bigint, text, text, int, int, numeric, numeric, int) is
  '폰의 신발을 거래소 장부에 올린다. 같은 신발은 한 번만, 계정당 상한이 있다.';

-- ── 판매 입찰 ─────────────────────────────────────────────────────
create or replace function public.market_list(
  p_sneaker_id bigint,
  p_price numeric
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_s record;
  v_bid record;
  v_id bigint;
begin
  if p_price < economy.market_min_price() then
    raise exception '값이 너무 낮습니다' using errcode = '22023';
  end if;

  select * into v_s from public.market_sneakers where id = p_sneaker_id for update;
  if not found or v_s.owner_id <> v_user then
    raise exception '내 스니커즈가 아닙니다' using errcode = '42501';
  end if;
  if v_s.status = 'LISTED' then
    raise exception '이미 판매 중입니다' using errcode = '22023';
  end if;

  -- 이미 이 값 이상을 부른 사람이 있으면 바로 팔린다.
  -- 값은 먼저 걸어 둔 쪽(구매 입찰)의 값으로 정해진다.
  select * into v_bid from public.market_bids
   where status = 'OPEN'
     and faction = v_s.faction and rarity = v_s.rarity and variant = v_s.variant
     and min_level <= v_s.level
     and buyer_id <> v_user
     and price >= p_price
   order by price desc, created_at asc
   limit 1
   for update;

  if found then
    update public.market_bids
       set status = 'FILLED', closed_at = now()
     where id = v_bid.id;
    perform public.market_settle(
      p_sneaker_id, v_user, v_bid.buyer_id, v_bid.price, 'BY_BID', true
    );
    return null;
  end if;

  insert into public.market_listings (sneaker_id, seller_id, price)
  values (p_sneaker_id, v_user, p_price)
  returning id into v_id;

  update public.market_sneakers set status = 'LISTED' where id = p_sneaker_id;
  return v_id;
end $$;

comment on function public.market_list(bigint, numeric) is
  '내 스니커즈를 판다. 이미 그 값 이상을 부른 구매 입찰이 있으면 바로 체결된다.';

create or replace function public.market_cancel_listing(p_listing_id bigint)
returns void
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_l record;
begin
  select * into v_l from public.market_listings where id = p_listing_id for update;
  if not found or v_l.seller_id <> v_user then
    raise exception '내 매물이 아닙니다' using errcode = '42501';
  end if;
  if v_l.status <> 'OPEN' then
    return;
  end if;

  update public.market_listings
     set status = 'CANCELLED', closed_at = now() where id = p_listing_id;
  update public.market_sneakers
     set status = 'OWNED' where id = v_l.sneaker_id;
end $$;

comment on function public.market_cancel_listing(bigint) is '내 매물을 거둔다.';

-- ── 즉시 구매 ─────────────────────────────────────────────────────
create or replace function public.market_buy_now(p_listing_id bigint)
returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_l record;
  v_balance numeric;
begin
  select * into v_l from public.market_listings where id = p_listing_id for update;
  if not found or v_l.status <> 'OPEN' then
    raise exception '이미 끝난 매물입니다' using errcode = '22023';
  end if;
  if v_l.seller_id = v_user then
    raise exception '내 매물은 살 수 없습니다' using errcode = '22023';
  end if;

  select coalesce(sum(amount), 0) into v_balance
    from public.sup_ledger where user_id = v_user;
  if v_balance < v_l.price then
    raise exception 'SUP 가 모자랍니다' using errcode = '22023';
  end if;

  update public.market_listings
     set status = 'SOLD', closed_at = now() where id = p_listing_id;

  return public.market_settle(
    v_l.sneaker_id, v_l.seller_id, v_user, v_l.price, 'BY_ASK', false
  );
end $$;

comment on function public.market_buy_now(bigint) is
  '매물을 그 값에 산다. 값은 매물이 정한 값이다.';

-- ── 구매 입찰 ─────────────────────────────────────────────────────
create or replace function public.market_bid(
  p_faction text,
  p_rarity text,
  p_variant int,
  p_min_level int,
  p_price numeric
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_ask record;
  v_balance numeric;
  v_id bigint;
begin
  if p_price < economy.market_min_price() then
    raise exception '값이 너무 낮습니다' using errcode = '22023';
  end if;

  select coalesce(sum(amount), 0) into v_balance
    from public.sup_ledger where user_id = v_user;
  if v_balance < p_price then
    raise exception 'SUP 가 모자랍니다' using errcode = '22023';
  end if;

  -- 그 값 이하의 매물이 이미 있으면 굳이 기다릴 이유가 없다.
  -- 값은 먼저 걸려 있던 매물의 값으로 — 부른 값보다 싸게 산다.
  select l.id as listing_id, l.price, l.seller_id, s.id as sneaker_id
    into v_ask
    from public.market_listings l
    join public.market_sneakers s on s.id = l.sneaker_id
   where l.status = 'OPEN'
     and s.faction = p_faction and s.rarity = p_rarity and s.variant = p_variant
     and s.level >= p_min_level
     and l.seller_id <> v_user
     and l.price <= p_price
   order by l.price asc, l.created_at asc
   limit 1
   for update of l;

  if found then
    update public.market_listings
       set status = 'SOLD', closed_at = now() where id = v_ask.listing_id;
    perform public.market_settle(
      v_ask.sneaker_id, v_ask.seller_id, v_user, v_ask.price, 'BY_ASK', false
    );
    return null;
  end if;

  -- 기다린다. 건 값은 잠근다 — 잠그지 않으면 같은 돈으로 열 곳에 입찰할 수 있다.
  insert into public.market_bids (buyer_id, faction, rarity, variant, min_level, price)
  values (v_user, p_faction, p_rarity, p_variant, p_min_level, p_price)
  returning id into v_id;

  insert into public.sup_ledger (user_id, kind, amount, description)
  values (v_user, 'ESCROW_LOCK', -p_price, '구매 입찰');

  return v_id;
end $$;

comment on function public.market_bid(text, text, int, int, numeric) is
  '모델에 값을 건다. 조건에 맞는 매물이 이미 있으면 그 값에 바로 산다.';

create or replace function public.market_cancel_bid(p_bid_id bigint)
returns void
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_b record;
begin
  select * into v_b from public.market_bids where id = p_bid_id for update;
  if not found or v_b.buyer_id <> v_user then
    raise exception '내 입찰이 아닙니다' using errcode = '42501';
  end if;
  if v_b.status <> 'OPEN' then
    return;
  end if;

  update public.market_bids
     set status = 'CANCELLED', closed_at = now() where id = p_bid_id;

  insert into public.sup_ledger (user_id, kind, amount, description)
  values (v_user, 'ESCROW_UNLOCK', v_b.price, '구매 입찰 취소');
end $$;

comment on function public.market_cancel_bid(bigint) is '내 구매 입찰을 거두고 잠긴 SUP 를 푼다.';

-- ── 즉시 판매 ─────────────────────────────────────────────────────
create or replace function public.market_sell_now(
  p_sneaker_id bigint,
  p_bid_id bigint
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_s record;
  v_b record;
  v_trade bigint;
begin
  select * into v_s from public.market_sneakers where id = p_sneaker_id for update;
  if not found or v_s.owner_id <> v_user then
    raise exception '내 스니커즈가 아닙니다' using errcode = '42501';
  end if;

  select * into v_b from public.market_bids where id = p_bid_id for update;
  if not found or v_b.status <> 'OPEN' then
    raise exception '이미 끝난 입찰입니다' using errcode = '22023';
  end if;
  if v_b.buyer_id = v_user then
    raise exception '내 입찰에는 팔 수 없습니다' using errcode = '22023';
  end if;
  if v_b.faction <> v_s.faction or v_b.rarity <> v_s.rarity or v_b.variant <> v_s.variant then
    raise exception '모델이 다릅니다' using errcode = '22023';
  end if;
  if v_s.level < v_b.min_level then
    raise exception '레벨이 모자랍니다' using errcode = '22023';
  end if;

  -- 팔려고 내놓은 상태였다면 그 매물은 거둔다
  if v_s.status = 'LISTED' then
    update public.market_listings
       set status = 'CANCELLED', closed_at = now()
     where sneaker_id = p_sneaker_id and status = 'OPEN';
  end if;

  update public.market_bids
     set status = 'FILLED', closed_at = now() where id = p_bid_id;

  v_trade := public.market_settle(
    p_sneaker_id, v_user, v_b.buyer_id, v_b.price, 'BY_BID', true
  );
  return v_trade;
end $$;

comment on function public.market_sell_now(bigint, bigint) is
  '걸려 있는 구매 입찰에 내 스니커즈를 판다. 값은 그 입찰의 값이다.';

-- ── 내 것 보기 ────────────────────────────────────────────────────
--
-- 뷰가 아니라 함수인 이유: 앱이 한 번에 받아야 하는 것이 "내 신발 + 내 매물
-- + 내 입찰"인데, 세 번 물어보면 그 사이에 팔려서 앞뒤가 안 맞는 화면이 된다.
create or replace function public.market_my_sneakers()
returns table (
  id bigint,
  local_id bigint,
  faction text,
  rarity text,
  variant int,
  level int,
  mint_number bigint,
  luck numeric,
  comfort numeric,
  durability int,
  status text,
  listing_id bigint,
  listing_price numeric
)
language sql stable security definer set search_path = public as $$
  select s.id,
         i.local_id,
         s.faction, s.rarity, s.variant, s.level, s.mint_number,
         s.luck, s.comfort, s.durability,
         s.status,
         l.id,
         l.price
    from public.market_sneakers s
    left join public.market_imports i on i.sneaker_id = s.id and i.user_id = s.owner_id
    left join public.market_listings l on l.sneaker_id = s.id and l.status = 'OPEN'
   where s.owner_id = auth.uid()
   order by s.created_at desc
$$;

comment on function public.market_my_sneakers() is
  '거래소가 아는 내 스니커즈. 판매 중이면 그 매물도 함께 온다.';

create or replace function public.market_my_bids()
returns table (
  id bigint,
  faction text,
  rarity text,
  variant int,
  min_level int,
  price numeric,
  created_at timestamptz
)
language sql stable security definer set search_path = public as $$
  select id, faction, rarity, variant, min_level, price, created_at
    from public.market_bids
   where buyer_id = auth.uid() and status = 'OPEN'
   order by created_at desc
$$;

comment on function public.market_my_bids() is '내가 걸어 둔 구매 입찰.';

create or replace function public.market_my_trades(p_limit int default 30)
returns table (
  id bigint,
  faction text,
  rarity text,
  variant int,
  level int,
  price numeric,
  fee numeric,
  sold boolean,
  traded_at timestamptz
)
language sql stable security definer set search_path = public as $$
  select id, faction, rarity, variant, level, price, fee,
         seller_id = auth.uid() as sold,
         traded_at
    from public.market_trades
   where seller_id = auth.uid() or buyer_id = auth.uid()
   order by traded_at desc
   limit greatest(1, least(coalesce(p_limit, 30), 100))
$$;

comment on function public.market_my_trades(int) is '내가 사고판 기록.';

-- ══════════════════════════════════════════════════════════════════
-- 0008_news.sql
-- ══════════════════════════════════════════════════════════════════

-- 러닝 소식 — 하루 한 번 채워지는 표.
--
-- ── 왜 표를 따로 두는가 ──
--
-- 앱이 바깥 사이트를 직접 읽게 하면 세 가지가 무너진다.
--   1. 기기마다 파싱한다 — 사이트가 모양을 바꾸면 모든 폰이 한꺼번에 깨진다.
--   2. 사용자 수만큼 남의 서버를 두드린다 — 차단당할 짓이다.
--   3. 폰이 켜져 있어야 갱신된다 — "매일 아침 9시"가 지켜지지 않는다.
--
-- 그래서 하루 한 번 한 곳에서 모아 이 표에 넣고, 앱은 이 표만 읽는다.
-- 모으는 일은 GitHub Actions 가 한다(.github/workflows/news-refresh.yml).
-- 서버를 새로 띄우지 않으므로 **드는 비용이 없다**.
--
-- ── 저작권 ──
--
-- 남의 기사 본문은 옮기지 않는다. 제목 · 출처 · 날짜 · 원문 링크까지만
-- 담고, 읽으려면 원문으로 보낸다. 요약을 지어내지도 않는다 — 우리가 쓰지
-- 않은 문장을 우리 것처럼 두면 그게 거짓말이다.

create table if not exists public.news_items (
  id bigint generated always as identity primary key,

  -- 원문 주소. 같은 글이 두 번 들어오지 않게 하는 열쇠이기도 하다.
  url text not null unique,

  title text not null,

  -- 어디서 왔는지. 화면에 그대로 보여 준다.
  source text not null,

  -- 원문이 제공한 한 줄 요약. 없으면 빈 값 — 지어내지 않는다.
  summary text not null default '',

  -- 뉴스 탭의 세 갈래
  kind text not null default 'RUN_EVENT'
    check (kind in ('RUN_EVENT', 'DEAL', 'HEALTH')),

  published_at timestamptz not null,
  fetched_at timestamptz not null default now()
);

comment on table public.news_items is
  '하루 한 번 모아 둔 러닝 소식. 쓰는 것은 수집기(service_role)뿐이고 앱은 읽기만 한다.';

create index if not exists news_items_recent
  on public.news_items (kind, published_at desc);

alter table public.news_items enable row level security;

-- 로그인하지 않아도 읽힌다. 소식은 가려 둘 것이 아니고, 로그인을 강요하면
-- 처음 앱을 연 사람에게 빈 탭을 보여 주게 된다.
do $$ begin
  create policy news_items_read on public.news_items for select using (true);
exception when duplicate_object then null; end $$;

-- 쓰기 정책은 두지 않는다. RLS 가 켜져 있고 정책이 없으면 anon 과
-- authenticated 는 한 줄도 쓸 수 없다. 수집기는 service_role 키로 들어오고
-- 그 키는 RLS 를 지나간다 — 그 키는 GitHub Secrets 에만 있고 앱에는 없다.

-- ══════════════════════════════════════════════════════════════════
-- 0009_running_feed.sql
-- ══════════════════════════════════════════════════════════════════

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

-- ══════════════════════════════════════════════════════════════════
-- 0010_crew_join.sql
-- ══════════════════════════════════════════════════════════════════

-- 크루 가입 방식, 가입 신청, 그리고 신고가 쌓인 글의 자동 숨김.
--
-- 지금까지 크루는 누구나 누르면 들어가는 방이었다. 동네 러닝 크루는
-- 그렇게 운영되지 않는 곳이 많다 — 모르는 사람이 새벽 6시 집합 장소에
-- 나타나는 것을 크루장이 먼저 알고 싶어 한다. 그래서 크루장이 방식을 고른다.
--
--   OPEN     누르면 바로 가입
--   APPROVAL 가입 신청 → 크루장이 승인하면 가입
--
-- 방식은 나중에 바꿀 수 있다. 승인제에서 자유 가입으로 바꾸면 기다리던
-- 신청은 그 자리에서 모두 받아 준다 — 문을 열었는데 줄 선 사람만 밖에
-- 세워 두는 것은 이상하다.

-- ════════════════════════════════════════════════════════════════════
--  가입 방식
-- ════════════════════════════════════════════════════════════════════

alter table public.crews
  add column if not exists join_policy text not null default 'OPEN';

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'crews_join_policy_check'
  ) then
    alter table public.crews
      add constraint crews_join_policy_check check (join_policy in ('OPEN', 'APPROVAL'));
  end if;
end $$;

comment on column public.crews.join_policy is
  'OPEN = 누르면 바로 가입, APPROVAL = 크루장 승인 후 가입';

-- ════════════════════════════════════════════════════════════════════
--  가입 신청
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.crew_join_requests (
  crew_id uuid not null references public.crews on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  requested_at timestamptz not null default now(),
  primary key (crew_id, user_id)
);

comment on table public.crew_join_requests is
  '승인제 크루의 가입 신청. 넣고 빼는 것은 crew_join / crew_decide / crew_leave 만 한다.';

alter table public.crew_join_requests enable row level security;

-- 신청은 신청한 사람과 그 크루의 크루장만 본다. 누가 어느 크루에
-- 들어가려다 기다리는지는 남이 알 일이 아니다.
drop policy if exists crew_join_requests_select on public.crew_join_requests;
create policy crew_join_requests_select on public.crew_join_requests for select
  using (
    (select auth.uid()) = user_id
    or exists (
      select 1 from public.crews c
       where c.id = crew_id and c.owner_id = (select auth.uid())
    )
  );

revoke insert, update, delete on public.crew_join_requests from anon, authenticated;

-- 직접 가입(INSERT)은 자유 가입 크루에만 열어 둔다. 승인제 크루에 표를
-- 직접 두드려 들어오면 승인이 아무 의미가 없다.
drop policy if exists crew_members_join_self on public.crew_members;
create policy crew_members_join_self on public.crew_members for insert
  with check (
    (select auth.uid()) = user_id
    and role = 'MEMBER'
    and exists (
      select 1 from public.crews c
       where c.id = crew_id and c.join_policy = 'OPEN'
    )
  );

-- ════════════════════════════════════════════════════════════════════
--  신고가 쌓이면 숨긴다
--
--  신고가 5건 모이면 그 글·댓글·크루·코스는 모두의 화면에서 사라진다.
--  운영자가 대시보드에서 신고를 기각(DISMISSED)하면 다시 보인다 — 여러
--  계정으로 멀쩡한 글을 내리는 일이 생겨도 되돌릴 길이 있어야 한다.
-- ════════════════════════════════════════════════════════════════════

create index if not exists content_reports_target
  on public.content_reports (target_type, target_id);

-- 신고함은 "내가 넣은 것"만 보이게 막혀 있다. 그 정책 아래서 세면 모두가
-- 자기 신고 1건만 보게 되므로, 세는 일만 이 함수가 대신 한다.
create or replace function public.is_hidden(p_type text, p_id text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select (
    select count(*) from public.content_reports r
     where r.target_type = p_type
       and r.target_id = p_id
       and r.status <> 'DISMISSED'
  ) >= 5
$$;

comment on function public.is_hidden is
  '신고가 5건 이상 쌓였는지. 기각된 신고는 세지 않는다.';

-- ════════════════════════════════════════════════════════════════════
--  앱이 읽는 모양 — 숨김을 반영해 다시 만든다
-- ════════════════════════════════════════════════════════════════════

drop view if exists public.post_feed;
create view public.post_feed
with (security_invoker = true) as
  select
    p.id,
    p.category,
    p.crew_id,
    p.author_id,
    coalesce(pr.display_name, '러너') as author,
    p.title,
    p.body,
    p.place,
    p.distance_km,
    p.meet_at,
    p.capacity,
    p.created_at,
    (select count(*) from public.post_likes l where l.post_id = p.id) as likes,
    (select count(*) from public.comments c where c.post_id = p.id) as comment_count,
    (select count(*) from public.flash_participants f where f.post_id = p.id) as joined_count,
    exists (
      select 1 from public.post_likes l
       where l.post_id = p.id and l.user_id = auth.uid()
    ) as liked,
    exists (
      select 1 from public.flash_participants f
       where f.post_id = p.id and f.user_id = auth.uid()
    ) as joined,
    p.author_id = auth.uid() as mine
  from public.posts p
  left join public.profiles pr on pr.id = p.author_id
  where not public.is_blocked(p.author_id)
    and not public.is_hidden('POST', p.id::text);

comment on view public.post_feed is
  '게시글 목록. 좋아요·댓글·참가 수와 "내가 눌렀는지"까지 한 줄에 담는다.';

drop view if exists public.comment_feed;
create view public.comment_feed
with (security_invoker = true) as
  select
    c.id,
    c.post_id,
    coalesce(c.parent_id, 0) as parent_id,
    c.author_id,
    coalesce(pr.display_name, '러너') as author,
    c.body,
    c.created_at,
    c.author_id = auth.uid() as mine
  from public.comments c
  left join public.profiles pr on pr.id = c.author_id
  where not public.is_blocked(c.author_id)
    and not public.is_hidden('COMMENT', c.id::text);

drop view if exists public.crew_feed;
create view public.crew_feed
with (security_invoker = true) as
  select
    c.id,
    c.owner_id,
    c.name,
    c.monogram,
    c.tagline,
    c.area,
    c.lat,
    c.lng,
    c.join_policy,
    c.created_at,
    (select count(*) from public.crew_members m where m.crew_id = c.id) as member_count,
    -- 카드에 얼굴 몇 개와 파티런 초대 후보를 그리는 데 쓴다. 크루장이 먼저,
    -- 그다음은 먼저 들어온 순.
    array(
      select coalesce(pr.display_name, '러너')
        from public.crew_members m
        left join public.profiles pr on pr.id = m.user_id
       where m.crew_id = c.id
       order by (m.role = 'OWNER') desc, m.joined_at
       limit 8
    ) as roster,
    exists (
      select 1 from public.crew_members m
       where m.crew_id = c.id and m.user_id = auth.uid()
    ) as joined,
    exists (
      select 1 from public.crew_join_requests r
       where r.crew_id = c.id and r.user_id = auth.uid()
    ) as requested,
    -- 기다리는 신청 수는 크루장에게만 의미가 있다. 다른 사람에게는 0이다.
    case when c.owner_id = auth.uid() then
      (select count(*) from public.crew_join_requests r where r.crew_id = c.id)
    else 0 end as pending_count,
    c.owner_id = auth.uid() as owned
  from public.crews c
  where not public.is_hidden('CREW', c.id::text);

drop view if exists public.course_feed;
create view public.course_feed
with (security_invoker = true) as
  select
    c.id,
    c.owner_id,
    coalesce(pr.display_name, '러너') as author,
    c.name,
    c.area,
    c.distance_km,
    c.elevation_m,
    c.track,
    c.shared,
    c.run_count,
    c.created_at,
    (select count(*) from public.course_likes l where l.course_id = c.id) as likes,
    exists (
      select 1 from public.course_likes l
       where l.course_id = c.id and l.user_id = auth.uid()
    ) as liked,
    c.owner_id = auth.uid() as mine
  from public.courses c
  left join public.profiles pr on pr.id = c.owner_id
  where not public.is_blocked(c.owner_id)
    and not public.is_hidden('COURSE', c.id::text);

grant select on public.post_feed, public.comment_feed, public.crew_feed, public.course_feed
  to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  크루 함수 — 앱은 이것만 부른다
--
--  표를 직접 두드리게 두면 "승인제인지 확인하고 → 신청을 넣는다"가 두 번의
--  요청이 되고, 그 사이에 크루장이 방식을 바꾸면 어긋난다. 한 번에 한다.
-- ════════════════════════════════════════════════════════════════════

-- 크루장 확인. 여러 함수가 같은 말을 하므로 한곳에 둔다.
create or replace function public.crew_assert_owner(p_crew uuid)
returns void
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not exists (
    select 1 from public.crews c where c.id = p_crew and c.owner_id = auth.uid()
  ) then
    raise exception '크루장만 할 수 있습니다' using errcode = '42501';
  end if;
end;
$$;

-- 크루 만들기. 만든 사람은 트리거로 주인 멤버가 된다.
create or replace function public.crew_create(
  p_name text,
  p_monogram text,
  p_tagline text,
  p_area text,
  p_join_policy text
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_id uuid;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_join_policy not in ('OPEN', 'APPROVAL') then
    raise exception '가입 방식이 올바르지 않습니다' using errcode = '22023';
  end if;
  -- 한 사람이 크루를 무한히 찍어 내면 목록이 빈 방으로 덮인다.
  if (select count(*) from public.crews c where c.owner_id = v_user) >= 10 then
    raise exception '크루는 한 사람당 10개까지 만들 수 있습니다' using errcode = '23514';
  end if;

  insert into public.crews (owner_id, name, monogram, tagline, area, join_policy)
  values (
    v_user,
    btrim(p_name),
    left(coalesce(p_monogram, ''), 4),
    btrim(coalesce(p_tagline, '')),
    btrim(coalesce(p_area, '')),
    p_join_policy
  )
  returning id into v_id;

  return v_id;
end;
$$;

-- 가입. 자유 가입이면 바로 들어가고(JOINED), 승인제면 신청만 남긴다(REQUESTED).
create or replace function public.crew_join(p_crew uuid)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_policy text;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  -- 크루를 잠근다. 크루장이 방식을 바꾸는 순간과 겹치면, 잠그지 않는 한
  -- 자유 가입으로 본 사람이 승인제 크루에 그냥 들어간다.
  select c.join_policy into v_policy
    from public.crews c
   where c.id = p_crew and not public.is_hidden('CREW', c.id::text)
     for share;

  if not found then
    raise exception '크루를 찾을 수 없습니다' using errcode = 'P0002';
  end if;

  if exists (
    select 1 from public.crew_members m where m.crew_id = p_crew and m.user_id = v_user
  ) then
    return 'JOINED';
  end if;

  if v_policy = 'OPEN' then
    insert into public.crew_members (crew_id, user_id, role)
    values (p_crew, v_user, 'MEMBER')
    on conflict do nothing;
    delete from public.crew_join_requests where crew_id = p_crew and user_id = v_user;
    return 'JOINED';
  end if;

  insert into public.crew_join_requests (crew_id, user_id)
  values (p_crew, v_user)
  on conflict do nothing;
  return 'REQUESTED';
end;
$$;

-- 탈퇴. 기다리던 신청이 있으면 그것도 거둔다. 크루장은 나갈 수 없다 —
-- 나가면 주인 없는 크루가 남는다.
create or replace function public.crew_leave(p_crew uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if exists (
    select 1 from public.crew_members m
     where m.crew_id = p_crew and m.user_id = v_user and m.role = 'OWNER'
  ) then
    raise exception '크루장은 크루를 나갈 수 없습니다' using errcode = '42501';
  end if;

  delete from public.crew_join_requests where crew_id = p_crew and user_id = v_user;
  delete from public.crew_members where crew_id = p_crew and user_id = v_user;
end;
$$;

-- 가입 방식 바꾸기. 자유 가입으로 열면 기다리던 신청을 모두 받아 준다.
-- 받아 준 인원을 돌려준다.
create or replace function public.crew_set_join_policy(p_crew uuid, p_policy text)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  v_admitted int := 0;
begin
  perform public.crew_assert_owner(p_crew);
  if p_policy not in ('OPEN', 'APPROVAL') then
    raise exception '가입 방식이 올바르지 않습니다' using errcode = '22023';
  end if;

  update public.crews set join_policy = p_policy where id = p_crew;

  if p_policy = 'OPEN' then
    with admitted as (
      delete from public.crew_join_requests r where r.crew_id = p_crew
      returning r.user_id
    )
    insert into public.crew_members (crew_id, user_id, role)
    select p_crew, a.user_id, 'MEMBER' from admitted a
    on conflict do nothing;
    get diagnostics v_admitted = row_count;
  end if;

  return v_admitted;
end;
$$;

-- 크루장이 보는 신청 목록. 먼저 신청한 사람이 위.
create or replace function public.crew_requests(p_crew uuid)
returns table (user_id uuid, name text, requested_at timestamptz)
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  perform public.crew_assert_owner(p_crew);
  return query
    select r.user_id, coalesce(pr.display_name, '러너'), r.requested_at
      from public.crew_join_requests r
      left join public.profiles pr on pr.id = r.user_id
     where r.crew_id = p_crew
     order by r.requested_at;
end;
$$;

-- 승인 또는 거절. 어느 쪽이든 신청은 사라진다.
create or replace function public.crew_decide(p_crew uuid, p_user uuid, p_approve boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.crew_assert_owner(p_crew);

  delete from public.crew_join_requests where crew_id = p_crew and user_id = p_user;
  if not found then
    raise exception '가입 신청을 찾을 수 없습니다' using errcode = 'P0002';
  end if;

  if p_approve then
    insert into public.crew_members (crew_id, user_id, role)
    values (p_crew, p_user, 'MEMBER')
    on conflict do nothing;
  end if;
end;
$$;

revoke execute on function public.crew_assert_owner(uuid) from public, anon;
grant execute on function public.crew_create(text, text, text, text, text) to authenticated;
grant execute on function public.crew_join(uuid) to authenticated;
grant execute on function public.crew_leave(uuid) to authenticated;
grant execute on function public.crew_set_join_policy(uuid, text) to authenticated;
grant execute on function public.crew_requests(uuid) to authenticated;
grant execute on function public.crew_decide(uuid, uuid, boolean) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0011_board.sql
-- ══════════════════════════════════════════════════════════════════

-- 게시판 — 글·댓글·좋아요·신고·차단을 앱이 부르는 함수로.
--
-- 표와 권한 규칙은 0004 에 이미 있다. 여기서는 앱이 부를 문을 만든다.
-- 앱이 표를 직접 두드리게 두지 않는 이유는 둘이다.
--
--   1. 한 번에 해야 하는 일이 있다. 번개 글을 쓰면 쓴 사람이 참가자 첫 줄이
--      되어야 하는데, 두 번의 요청으로 나누면 사이에서 앱이 죽었을 때
--      "주최자가 참가하지 않은 번개"가 남는다.
--   2. 도배를 막아야 한다. 앱에 박힌 키로 누구나 요청을 보낼 수 있으므로,
--      한 시간에 몇 개까지인지는 서버가 센다.

-- ════════════════════════════════════════════════════════════════════
--  글
-- ════════════════════════════════════════════════════════════════════

create or replace function public.post_create(
  p_category text,
  p_crew uuid,
  p_title text,
  p_body text,
  p_place text,
  p_distance_km double precision,
  p_meet_at timestamptz,
  p_capacity int
)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_id bigint;
  v_flash boolean := p_category = 'FLASH';
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_category not in ('FLASH', 'FREE', 'TIP') then
    raise exception '게시판 종류가 올바르지 않습니다' using errcode = '22023';
  end if;
  if not public.is_crew_member(p_crew) then
    raise exception '이 크루의 멤버가 아닙니다' using errcode = '42501';
  end if;
  if (select count(*) from public.posts p
       where p.author_id = v_user and p.created_at > now() - interval '1 hour') >= 20 then
    raise exception '글을 너무 자주 쓰고 있습니다. 잠시 뒤에 다시 써 주세요' using errcode = '23514';
  end if;
  if v_flash and (p_meet_at is null or p_meet_at < now()) then
    raise exception '번개러닝은 앞으로의 모임 시각이 있어야 합니다' using errcode = '23514';
  end if;

  insert into public.posts (
    author_id, category, crew_id, title, body,
    place, distance_km, meet_at, capacity
  )
  values (
    v_user,
    p_category,
    p_crew,
    btrim(p_title),
    btrim(coalesce(p_body, '')),
    case when v_flash then btrim(coalesce(p_place, '')) else '' end,
    case when v_flash then greatest(coalesce(p_distance_km, 0), 0) else 0 end,
    case when v_flash then p_meet_at else null end,
    -- 번개는 둘 이상이 모여야 번개다. 정원 1은 혼자 뛰는 것과 같다.
    case when v_flash then least(greatest(coalesce(p_capacity, 2), 2), 200) else 0 end
  )
  returning id into v_id;

  -- 번개를 연 사람은 참가자 첫 줄이다.
  if v_flash then
    insert into public.flash_participants (post_id, user_id) values (v_id, v_user);
  end if;

  return v_id;
end;
$$;

create or replace function public.post_delete(p_post bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  delete from public.posts where id = p_post and author_id = auth.uid();
  if not found then
    raise exception '내가 쓴 글만 지울 수 있습니다' using errcode = '42501';
  end if;
end;
$$;

-- 좋아요를 누르거나 거둔다. 누른 뒤의 상태(눌렸으면 true)를 돌려준다.
create or replace function public.post_toggle_like(p_post bigint)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not public.can_see_post(p_post) then
    raise exception '글을 찾을 수 없습니다' using errcode = 'P0002';
  end if;

  delete from public.post_likes where post_id = p_post and user_id = v_user;
  if found then
    return false;
  end if;
  insert into public.post_likes (post_id, user_id) values (p_post, v_user);
  return true;
end;
$$;

-- ════════════════════════════════════════════════════════════════════
--  댓글
-- ════════════════════════════════════════════════════════════════════

-- @param p_parent 0 또는 null 이면 새 댓글, 그 외에는 그 댓글에 대한 답글
create or replace function public.comment_create(p_post bigint, p_parent bigint, p_body text)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_parent bigint := nullif(p_parent, 0);
  v_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not public.can_see_post(p_post) then
    raise exception '글을 찾을 수 없습니다' using errcode = 'P0002';
  end if;
  -- 다른 글의 댓글에 답글을 달면 그 답글은 어느 글에도 보이지 않는다.
  if v_parent is not null and not exists (
    select 1 from public.comments c where c.id = v_parent and c.post_id = p_post
  ) then
    raise exception '답글을 달 댓글을 찾을 수 없습니다' using errcode = 'P0002';
  end if;
  if (select count(*) from public.comments c
       where c.author_id = v_user and c.created_at > now() - interval '1 hour') >= 60 then
    raise exception '댓글을 너무 자주 쓰고 있습니다. 잠시 뒤에 다시 써 주세요' using errcode = '23514';
  end if;

  insert into public.comments (post_id, parent_id, author_id, body)
  values (p_post, v_parent, v_user, btrim(p_body))
  returning id into v_id;
  return v_id;
end;
$$;

-- 댓글 지우기. 달린 답글도 함께 지워진다(표의 on delete cascade).
create or replace function public.comment_delete(p_comment bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  delete from public.comments where id = p_comment and author_id = auth.uid();
  if not found then
    raise exception '내가 쓴 댓글만 지울 수 있습니다' using errcode = '42501';
  end if;
end;
$$;

-- ════════════════════════════════════════════════════════════════════
--  신고와 차단
-- ════════════════════════════════════════════════════════════════════

-- 신고. 같은 사람이 같은 대상을 다시 신고해도 한 건이다. 5건이 모이면
-- 목록 뷰에서 사라진다(0010 is_hidden).
create or replace function public.content_report(
  p_type text,
  p_target text,
  p_reason text,
  p_note text
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  insert into public.content_reports (reporter_id, target_type, target_id, reason, note)
  values (auth.uid(), p_type, p_target, p_reason, left(coalesce(p_note, ''), 1000))
  on conflict (reporter_id, target_type, target_id) do nothing;
end;
$$;

-- 차단. 그 사람의 글·댓글·코스가 내 화면에서 사라진다. 상대에게는 알리지 않는다.
create or replace function public.user_block(p_user uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_user = auth.uid() then
    raise exception '나 자신은 차단할 수 없습니다' using errcode = '22023';
  end if;
  insert into public.user_blocks (blocker_id, blocked_id)
  values (auth.uid(), p_user)
  on conflict do nothing;
end;
$$;

grant execute on function public.post_create(text, uuid, text, text, text, double precision, timestamptz, int)
  to authenticated;
grant execute on function public.post_delete(bigint) to authenticated;
grant execute on function public.post_toggle_like(bigint) to authenticated;
grant execute on function public.comment_create(bigint, bigint, text) to authenticated;
grant execute on function public.comment_delete(bigint) to authenticated;
grant execute on function public.content_report(text, text, text, text) to authenticated;
grant execute on function public.user_block(uuid) to authenticated;

commit;

-- ════════════════════════════════════════════════════════════════════
--  끝났습니다. 아래로 확인할 수 있습니다.
-- ════════════════════════════════════════════════════════════════════
select table_name as "만들어진 표"
  from information_schema.tables
 where table_schema = 'public' and table_type = 'BASE TABLE'
 order by table_name;
