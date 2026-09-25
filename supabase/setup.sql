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

-- 본인이 고칠 수 있는 것은 보이는 정보뿐이다. 최고 속도·누적 거리·스트릭은
-- 서버 함수(record_session)만 쓴다 — 열어 두면 PATCH 한 번으로 순위 1등이 된다.
revoke update on public.profiles from anon, authenticated;
grant update (display_name, avatar_id, daily_goal, language) on public.profiles to authenticated;

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

-- 걸음은 본인 것만 읽는다. 쓰기는 steps_sync()(하루 상한·줄지 않음 규칙)만 한다 —
-- 표에 바로 쓰게 두면 999만 보를 적어 주간 걸음 이벤트 보상을 받는다.
drop policy if exists daily_steps_own on public.daily_steps;
create policy daily_steps_own
  on public.daily_steps for select
  using ((select auth.uid()) = user_id);
revoke insert, update, delete on public.daily_steps from anon, authenticated;

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

  -- 잔고를 세는 동안 다른 요청이 끼어들지 못하게 이 사용자의 원장 쓰기를 잠근다.
  -- 잠그지 않으면 두 요청이 동시에 "잔고 충분"을 보고 둘 다 통과해 잔고가
  -- 음수가 된다 — 지갑이 없어도 이중지불은 일어난다.
  -- 원장 행 잠금(for update)으로는 부족하다. **새로 들어오는** 행(다른 요청의 입찰·구매)은
  -- 막지 못한다. 원장을 건드리는 함수가 모두 같은 사용자 잠금을 가장 먼저 잡아 한 줄로 선다.
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

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
-- 달린 횟수(run_count)는 서버가 센다(course_run_submit). 주인이라도 새로 넣거나 고치며
-- 적지 못한다. 경로(track)는 코스를 알아보는 열쇠(md5)라 올린 뒤에는 바꾸지 못한다.
revoke insert, update on public.courses from anon, authenticated;
grant insert (owner_id, name, area, distance_km, elevation_m, track, shared) on public.courses to authenticated;
grant update (name, area, distance_km, elevation_m, shared) on public.courses to authenticated;

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
    raise exception '번개러닝 글을 찾을 수 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
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
      -- 번 것만 센다. 입찰을 걸었다 거두면 ESCROW_UNLOCK(+)이 적히고, 팔면
      -- TRADE_SELL(+)이 적힌다 — 이것까지 세면 입찰·취소를 되풀이해 공짜로 오른다.
      select user_id, sum(amount) filter (
               where amount > 0 and kind in ('EARN_WALK', 'EARN_PARTY', 'EARN_EVENT', 'BONUS_GOAL')
             ) as earned
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
-- not valid: 이 파일은 배포 때마다 다시 돈다. 뒤 파일(0022)이 종류를 더 늘린 뒤에
-- 여기서 옛 목록으로 기존 줄을 검사하면 배포가 통째로 멈춘다. 새 줄만 검사하고,
-- 전체 검사는 최종 목록을 거는 0022 가 한다.
alter table public.sup_ledger add constraint sup_ledger_kind_check check (kind in (
  'EARN_WALK', 'EARN_PARTY', 'EARN_EVENT', 'BONUS_GOAL',
  'SPEND_MINT', 'SPEND_UPGRADE', 'SPEND_BOOST',
  'ESCROW_LOCK', 'ESCROW_UNLOCK', 'TRADE_BUY', 'TRADE_SELL', 'TRADE_FEE'
)) not valid;

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
-- Supabase 는 public 스키마의 새 함수에 anon · authenticated 실행 권한을 따로 붙인다.
-- public 에서만 거두면 그 권한이 남아, 누구나 이 함수로 SUP 를 만들고 남의 신발을 가져간다.
revoke all on function public.market_settle(bigint, uuid, uuid, numeric, text, boolean) from public, anon, authenticated;

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
  -- 잔고를 세고 쓰는 사이에 같은 사람의 다른 결제가 끼면 둘 다 "잔고 충분"을 보고
  -- 잔고가 음수가 된다. 이 사람의 원장 쓰기를 한 줄로 세운다(spend_sup 과 같은 잠금).
  -- 매물 잠금보다 먼저 잡아, 입찰과 이 잠금을 서로 반대 순서로 잡지 않게 한다.
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

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

  -- 잔고 확인과 묶기 사이에 같은 사람의 다른 결제가 끼지 못하게(spend_sup 과 같은 잠금)
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

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

-- 뷰는 만든 쪽 권한으로 읽는다(security_invoker = false). 호출자 권한으로 읽으면
-- 호출자에게 원본 표 읽기를 열어 줘야 하고, 그러면 /content_sources 로 endpoint 와
-- 오류 원문을 그대로 읽을 수 있다. 원본 표는 아래에서 닫는다.
create or replace view public.running_sources_public
with (security_invoker = false) as
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

revoke select on public.content_sources from anon, authenticated;
-- 이 뷰는 표 하나를 그대로 비추므로 Postgres 가 쓰기도 받아 준다. 만든 쪽 권한으로
-- 쓰이면 RLS 를 건너뛰므로, Supabase 가 기본으로 붙이는 쓰기 권한을 거두고 읽기만 준다.
revoke all on public.running_sources_public from anon, authenticated;
grant select on public.running_sources_public to anon, authenticated;

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

-- public 에서만 거두면 Supabase 가 따로 붙인 anon · authenticated 권한이 남는다(가짜 감사 기록).
revoke all on function public.admin_log(text, text, jsonb) from public, anon, authenticated;

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
    raise exception '크루를 찾을 수 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
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
    raise exception '가입 신청을 찾을 수 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
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
--  번개 모임 장소
--
--  번개 글의 "몇 km 떨어져 있나"는 쓴 사람이 손으로 적은 숫자였다. 그 숫자는
--  쓴 사람 기준이라 읽는 사람에게는 뜻이 없다. 모임 장소 좌표를 받아 두고,
--  거리는 읽는 사람의 폰이 자기 위치에서 잰다. distance_km 는 이제 "함께
--  달릴 거리"다.
-- ════════════════════════════════════════════════════════════════════

alter table public.posts add column if not exists lat double precision;
alter table public.posts add column if not exists lng double precision;

comment on column public.posts.distance_km is '번개러닝에서 함께 달릴 거리(km)';

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
    p.lat,
    p.lng,
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

grant select on public.post_feed to authenticated;

-- 번개 참가자 명단. 상세 화면의 "참가자 보기"가 읽는다. 글을 볼 수 없는
-- 사람(남의 크루 글)에게는 posts 규칙이 줄을 걸러 낸다.
drop view if exists public.flash_roster;
create view public.flash_roster
with (security_invoker = true) as
  select
    f.post_id,
    f.user_id,
    coalesce(pr.display_name, '러너') as name,
    f.joined_at,
    f.user_id = p.author_id as is_host,
    f.user_id = auth.uid() as is_me
  from public.flash_participants f
  join public.posts p on p.id = f.post_id
  left join public.profiles pr on pr.id = f.user_id
  where not public.is_blocked(f.user_id);

comment on view public.flash_roster is '번개러닝 참가자 명단 — 주최자와 먼저 온 순서';

grant select on public.flash_roster to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  글
-- ════════════════════════════════════════════════════════════════════

-- 모임 장소 좌표가 없던 첫 판. 인자가 달라 새 함수와 나란히 남으면 앱이
-- 어느 쪽을 부르는지 흐려지므로 지운다.
drop function if exists public.post_create(
  text, uuid, text, text, text, double precision, timestamptz, int
);

create or replace function public.post_create(
  p_category text,
  p_crew uuid,
  p_title text,
  p_body text,
  p_place text,
  p_distance_km double precision,
  p_meet_at timestamptz,
  p_capacity int,
  p_lat double precision,
  p_lng double precision
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

  if p_lat is not null and (p_lat not between -90 and 90 or p_lng is null
                            or p_lng not between -180 and 180) then
    raise exception '모임 장소 좌표가 올바르지 않습니다' using errcode = '22023';
  end if;

  insert into public.posts (
    author_id, category, crew_id, title, body,
    place, distance_km, meet_at, capacity, lat, lng
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
    case when v_flash then least(greatest(coalesce(p_capacity, 2), 2), 200) else 0 end,
    case when v_flash then p_lat else null end,
    case when v_flash then p_lng else null end
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
    raise exception '글을 찾을 수 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
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
    raise exception '글을 찾을 수 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
  end if;
  -- 다른 글의 댓글에 답글을 달면 그 답글은 어느 글에도 보이지 않는다.
  if v_parent is not null and not exists (
    select 1 from public.comments c where c.id = v_parent and c.post_id = p_post
  ) then
    raise exception '답글을 달 댓글을 찾을 수 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
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

grant execute on function public.post_create(
  text, uuid, text, text, text, double precision, timestamptz, int, double precision, double precision
) to authenticated;
grant execute on function public.post_delete(bigint) to authenticated;
grant execute on function public.post_toggle_like(bigint) to authenticated;
grant execute on function public.comment_create(bigint, bigint, text) to authenticated;
grant execute on function public.comment_delete(bigint) to authenticated;
grant execute on function public.content_report(text, text, text, text) to authenticated;
grant execute on function public.user_block(uuid) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0012_course_party.sql
-- ══════════════════════════════════════════════════════════════════

-- 코스 공유와 파티런 로비.
--
-- 코스 게시판은 폰 안의 코스 중 "공유" 표시가 켜진 것이었다. 남이 올린 코스는
-- 볼 수 없었다. 이제 공유한 코스는 서버에 올라가고, 게시판은 서버에서 읽는다.
--
-- 파티런은 로비에 들어온 "크루원"이 폰이 지어낸 사람이었다. 이제 로비는
-- 서버에 있고, 같은 크루의 로비를 연 사람들이 같은 방에 모인다. 앱은 몇 초마다
-- 방의 상태를 물어 준비·출발·거리를 맞춘다.

-- ════════════════════════════════════════════════════════════════════
--  코스 공유
-- ════════════════════════════════════════════════════════════════════

-- 같은 사람이 같은 길을 두 번 올리면 게시판에 같은 코스가 둘 뜬다.
-- 길은 길어서 그대로 색인하지 않고 지문(md5)으로 잡는다.
create unique index if not exists courses_owner_track
  on public.courses (owner_id, md5(track));

-- 코스 올리기. 이미 올린 길이면 이름만 고쳐 다시 공개한다. 서버 번호를 돌려준다.
create or replace function public.course_share(
  p_name text,
  p_area text,
  p_distance_km double precision,
  p_elevation_m int,
  p_track text
)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if coalesce(length(p_track), 0) < 7 or length(p_track) > 200000 then
    raise exception '코스 경로가 올바르지 않습니다' using errcode = '22023';
  end if;
  if p_distance_km is null or p_distance_km < 0.2 or p_distance_km > 200 then
    raise exception '코스 거리가 올바르지 않습니다' using errcode = '22023';
  end if;
  if (select count(*) from public.courses c
       where c.owner_id = v_user and c.created_at > now() - interval '1 day') >= 30 then
    raise exception '코스를 너무 많이 올렸습니다. 내일 다시 올려 주세요' using errcode = '23514';
  end if;

  insert into public.courses (owner_id, name, area, distance_km, elevation_m, track, shared)
  values (
    v_user,
    left(coalesce(nullif(btrim(p_name), ''), 'Course'), 60),
    left(btrim(coalesce(p_area, '')), 60),
    p_distance_km,
    coalesce(p_elevation_m, 0),
    p_track,
    true
  )
  on conflict (owner_id, md5(track)) do update
    set name = excluded.name,
        area = excluded.area,
        shared = true
  returning id into v_id;

  return v_id;
end;
$$;

-- 올린 코스 내리기. 폰에는 내 코스로 그대로 남는다.
create or replace function public.course_unshare(p_track text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  -- 지우지 않고 내리기만 한다. 지우면 다른 러너의 코스 기록·좋아요가 함께 사라지고,
  -- 다시 올리면 새 코스가 되어 이미 받아 간 사람들의 기록이 이어지지 않는다.
  -- (다시 올리면 course_share 의 on conflict 가 shared 를 되돌린다.)
  update public.courses set shared = false
   where owner_id = auth.uid() and md5(track) = md5(coalesce(p_track, ''));
end;
$$;

-- 코스 좋아요를 누르거나 거둔다. 누른 뒤의 상태를 돌려준다.
create or replace function public.course_toggle_like(p_course bigint)
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
  if not exists (
    select 1 from public.courses c
     where c.id = p_course and (c.shared or c.owner_id = v_user)
  ) then
    raise exception '코스를 찾을 수 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
  end if;

  delete from public.course_likes where course_id = p_course and user_id = v_user;
  if found then
    return false;
  end if;
  insert into public.course_likes (course_id, user_id) values (p_course, v_user);
  return true;
end;
$$;

grant execute on function public.course_share(text, text, double precision, int, text) to authenticated;
grant execute on function public.course_unshare(text) to authenticated;
grant execute on function public.course_toggle_like(bigint) to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  파티런 로비
--
--  방 하나는 크루 하나 또는 번개 글 하나에 붙는다. 같은 크루의 로비를 연
--  사람들은 열려 있는 같은 방에 들어간다. 방장은 처음 연 사람이고, 방장이
--  나가면 그다음 먼저 들어온 사람이 이어받는다.
--
--  표에는 앱이 직접 닿지 못한다. 전부 아래 함수로만 한다 — 준비·출발·강퇴는
--  누가 할 수 있는지가 정해져 있고, 표를 열어 두면 그 규칙이 없어진다.
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.parties (
  id bigint generated always as identity primary key,
  crew_id uuid references public.crews on delete cascade,
  flash_post_id bigint references public.posts on delete cascade,
  host_id uuid not null references auth.users on delete cascade,
  status text not null default 'LOBBY'
    check (status in ('LOBBY', 'COUNTDOWN', 'RUNNING', 'FINISHED')),
  -- 카운트다운이 끝나는 시각. 모두가 이 시각에 같이 출발한다.
  starts_at timestamptz,
  created_at timestamptz not null default now(),
  constraint parties_one_target check ((crew_id is null) <> (flash_post_id is null))
);

create index if not exists parties_open_crew on public.parties (crew_id)
  where status <> 'FINISHED';
create index if not exists parties_open_flash on public.parties (flash_post_id)
  where status <> 'FINISHED';

create table if not exists public.party_members (
  party_id bigint not null references public.parties on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  ready boolean not null default false,
  joined_at timestamptz not null default now(),
  -- 마지막으로 방 상태를 물은 시각. 앱을 닫고 사라진 사람을 가려낸다.
  last_seen timestamptz not null default now(),
  -- 달리는 동안의 위치. 방장에게서 너무 멀어진 사람을 가려낸다.
  lat double precision,
  lng double precision,
  primary key (party_id, user_id)
);

alter table public.parties enable row level security;
alter table public.party_members enable row level security;
revoke all on public.parties, public.party_members from anon, authenticated;

-- 이 크루(또는 번개)의 방에 들어갈 수 있는지
create or replace function public.party_can_enter(p_crew uuid, p_post bigint)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select case
    when p_crew is not null then exists (
      select 1 from public.crew_members m where m.crew_id = p_crew and m.user_id = auth.uid()
    )
    when p_post is not null then exists (
      select 1 from public.flash_participants f where f.post_id = p_post and f.user_id = auth.uid()
    ) or exists (
      select 1 from public.posts p where p.id = p_post and p.author_id = auth.uid()
    )
    else false
  end
$$;

-- 방의 멤버인지
create or replace function public.party_assert_member(p_party bigint)
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
    select 1 from public.party_members m where m.party_id = p_party and m.user_id = auth.uid()
  ) then
    raise exception '이 파티에 들어와 있지 않습니다' using errcode = '42501';
  end if;
end;
$$;

-- 로비 열기. 열려 있는 방이 있으면 거기에 들어가고, 없으면 새로 연다.
-- @return 방 번호
create or replace function public.party_open(p_crew uuid, p_post bigint)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_party bigint;
  v_status text;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if (p_crew is null) = (p_post is null) then
    raise exception '크루 또는 번개 하나를 골라야 합니다' using errcode = '22023';
  end if;
  if not public.party_can_enter(p_crew, p_post) then
    raise exception '크루원이나 번개 참가자만 들어갈 수 있습니다' using errcode = '42501';
  end if;

  -- 아무도 안 보는 방과 오래된 방은 닫는다. 안 닫으면 어제 열고 버린 방에
  -- 오늘 들어가게 된다.
  update public.parties p set status = 'FINISHED'
   where p.status <> 'FINISHED'
     and (p.created_at < now() - interval '6 hours'
          or not exists (
            select 1 from public.party_members m
             where m.party_id = p.id and m.last_seen > now() - interval '10 minutes'
          ));

  -- 이 크루(번개)의 열린 방. 두 사람이 동시에 열어도 방이 둘 생기지 않게 잠근다.
  perform pg_advisory_xact_lock(hashtext(coalesce(p_crew::text, 'post:' || p_post)));

  select p.id, p.status into v_party, v_status
    from public.parties p
   where p.status <> 'FINISHED'
     and ((p_crew is not null and p.crew_id = p_crew)
          or (p_post is not null and p.flash_post_id = p_post))
   order by p.created_at desc
   limit 1;

  -- 이미 출발한 방에 늦게 온 사람은 끼워 넣지 않는다. 같이 출발하지 않았으면
  -- 같이 뛴 것이 아니다. 대신 새 방을 연다.
  if v_party is not null and v_status <> 'LOBBY' and not exists (
    select 1 from public.party_members m where m.party_id = v_party and m.user_id = v_user
  ) then
    v_party := null;
  end if;

  if v_party is null then
    insert into public.parties (crew_id, flash_post_id, host_id)
    values (p_crew, p_post, v_user)
    returning id into v_party;
  end if;

  insert into public.party_members (party_id, user_id)
  values (v_party, v_user)
  on conflict (party_id, user_id) do update set last_seen = now();

  return v_party;
end;
$$;

-- 카운트다운이 끝났으면 달리는 중으로 넘긴다. 따로 시계를 돌리지 않고, 누군가
-- 방 상태를 물을 때 넘긴다.
create or replace function public.party_tick(p_party bigint)
returns void
language sql
security definer
set search_path = public
as $$
  update public.parties set status = 'RUNNING'
   where id = p_party and status = 'COUNTDOWN' and starts_at <= now()
$$;

-- 방 상태. 앱이 몇 초마다 부른다. 부를 때마다 "나 아직 여기 있다"가 기록된다.
create or replace function public.party_state(p_party bigint)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
  v_state json;
begin
  perform public.party_assert_member(p_party);
  perform public.party_tick(p_party);

  update public.party_members set last_seen = now()
   where party_id = p_party and user_id = auth.uid();

  select json_build_object(
    'id', p.id,
    'status', p.status,
    'host_id', p.host_id,
    'crew_id', p.crew_id,
    'flash_post_id', p.flash_post_id,
    'starts_at', p.starts_at,
    'server_now', now(),
    'members', coalesce((
      select json_agg(json_build_object(
        'user_id', m.user_id,
        'name', coalesce(pr.display_name, '러너'),
        'ready', m.ready,
        'is_me', m.user_id = auth.uid(),
        'is_host', m.user_id = p.host_id,
        'lat', m.lat,
        'lng', m.lng,
        'seen_sec', extract(epoch from (now() - m.last_seen))::int
      ) order by (m.user_id = p.host_id) desc, m.joined_at)
        from public.party_members m
        left join public.profiles pr on pr.id = m.user_id
       where m.party_id = p.id
    ), '[]'::json)
  ) into v_state
  from public.parties p
  where p.id = p_party;

  return v_state;
end;
$$;

create or replace function public.party_ready(p_party bigint, p_ready boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.party_assert_member(p_party);
  if not exists (select 1 from public.parties where id = p_party and status = 'LOBBY') then
    raise exception '이미 출발한 파티입니다' using errcode = '23514';
  end if;
  update public.party_members set ready = p_ready, last_seen = now()
   where party_id = p_party and user_id = auth.uid();
end;
$$;

-- 나가기. 방장이 나가면 다음으로 먼저 들어온 사람이 방장이 된다. 아무도 안
-- 남으면 방을 닫는다.
create or replace function public.party_leave(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_next uuid;
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  delete from public.party_members where party_id = p_party and user_id = auth.uid();

  select m.user_id into v_next
    from public.party_members m
   where m.party_id = p_party
   order by m.joined_at
   limit 1;

  if v_next is null then
    update public.parties set status = 'FINISHED' where id = p_party;
  else
    update public.parties set host_id = v_next
     where id = p_party and host_id = auth.uid();
  end if;
end;
$$;

-- 방장 — 로비에서 한 사람을 내보낸다.
create or replace function public.party_kick(p_party bigint, p_user uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not exists (
    select 1 from public.parties
     where id = p_party and host_id = auth.uid() and status = 'LOBBY'
  ) then
    raise exception '로비에서 방장만 할 수 있습니다' using errcode = '42501';
  end if;
  if p_user = auth.uid() then
    raise exception '방장은 자신을 내보낼 수 없습니다' using errcode = '22023';
  end if;
  delete from public.party_members where party_id = p_party and user_id = p_user;
end;
$$;

-- 방장 — 출발. 준비한 사람들끼리 뛴다. 준비 안 한 사람은 방에서 빠진다 —
-- 데리고 가면 적립 보너스(인원수)에는 들어가면서 실제로는 안 뛰는 사람이 생긴다.
-- 4초 뒤에 모두 같이 출발한다.
create or replace function public.party_start(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not exists (
    select 1 from public.parties
     where id = p_party and host_id = auth.uid() and status = 'LOBBY'
  ) then
    raise exception '로비에서 방장만 출발할 수 있습니다' using errcode = '42501';
  end if;
  if not exists (
    select 1 from public.party_members
     where party_id = p_party and user_id = auth.uid() and ready
  ) then
    raise exception '방장이 먼저 준비해야 합니다' using errcode = '23514';
  end if;

  delete from public.party_members where party_id = p_party and not ready;
  update public.parties
     set status = 'COUNTDOWN', starts_at = now() + interval '4 seconds'
   where id = p_party;
end;
$$;

-- 달리는 동안의 위치 보고. 멀어진 사람을 가려내는 데만 쓴다.
create or replace function public.party_ping(p_party bigint, p_lat double precision, p_lng double precision)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.party_assert_member(p_party);
  if p_lat is not null and (p_lat not between -90 and 90 or p_lng not between -180 and 180) then
    raise exception '위치가 올바르지 않습니다' using errcode = '22023';
  end if;
  update public.party_members
     set lat = p_lat, lng = p_lng, last_seen = now()
   where party_id = p_party and user_id = auth.uid();
end;
$$;

-- 방장이 러닝을 마치면 방을 닫는다. 다음 파티런은 새 방에서 한다.
create or replace function public.party_finish(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.party_assert_member(p_party);
  update public.parties set status = 'FINISHED'
   where id = p_party and host_id = auth.uid();
end;
$$;

revoke execute on function public.party_assert_member(bigint) from public, anon;
revoke execute on function public.party_tick(bigint) from public, anon;
grant execute on function public.party_open(uuid, bigint) to authenticated;
grant execute on function public.party_state(bigint) to authenticated;
grant execute on function public.party_ready(bigint, boolean) to authenticated;
grant execute on function public.party_leave(bigint) to authenticated;
grant execute on function public.party_kick(bigint, uuid) to authenticated;
grant execute on function public.party_start(bigint) to authenticated;
grant execute on function public.party_ping(bigint, double precision, double precision) to authenticated;
grant execute on function public.party_finish(bigint) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0013_push.sql
-- ══════════════════════════════════════════════════════════════════

-- 푸시 알림 — 폰마다 받는 주소(FCM 토큰)를 적어 둔다.
--
-- 토큰 하나는 폰 하나(앱 설치 하나)다. 한 사람이 폰을 여러 대 쓰면 줄이
-- 여럿이고, 한 폰에서 다른 계정으로 로그인하면 그 토큰은 새 계정으로 옮겨
-- 간다 — 앞 사람의 알림이 뒤 사람 폰에 뜨면 안 되기 때문이다.
--
-- 보내는 일은 서버(Edge Function)가 한다. 앱은 토큰을 적기만 하고, 남의
-- 토큰은 읽을 수 없다.

create table if not exists public.push_tokens (
  token text primary key check (length(token) between 20 and 4096),
  user_id uuid not null references auth.users on delete cascade,
  platform text not null default 'android' check (platform in ('android', 'ios')),
  -- 알림 글을 어느 언어로 쓸지. 앱 언어(ko · en · ja · zh)를 그대로 받는다.
  locale text not null default 'ko' check (length(locale) <= 10),
  updated_at timestamptz not null default now()
);

create index if not exists push_tokens_user on public.push_tokens (user_id);

comment on table public.push_tokens is
  '푸시 알림을 받을 폰(FCM 토큰). 앱은 push_register 로 적기만 하고 읽지 못한다.';

alter table public.push_tokens enable row level security;
revoke all on public.push_tokens from anon, authenticated;

-- 이 폰의 토큰을 지금 로그인한 사람에게 붙인다. 앱을 켤 때마다, 토큰이
-- 바뀔 때마다 부른다.
create or replace function public.push_register(p_token text, p_locale text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if coalesce(length(p_token), 0) not between 20 and 4096 then
    raise exception '알림 토큰이 올바르지 않습니다' using errcode = '22023';
  end if;
  -- 한 사람이 폰을 수십 대 쓰지는 않는다. 오래된 것부터 정리한다.
  delete from public.push_tokens
   where user_id = auth.uid()
     and token <> p_token
     and token not in (
       select t.token from public.push_tokens t
        where t.user_id = auth.uid()
        order by t.updated_at desc
        limit 9
     );

  insert into public.push_tokens (token, user_id, locale, updated_at)
  values (p_token, auth.uid(), left(coalesce(nullif(p_locale, ''), 'ko'), 10), now())
  on conflict (token) do update
    set user_id = excluded.user_id,
        locale = excluded.locale,
        updated_at = now();
end;
$$;

-- 이 폰에서 알림을 끈다(로그아웃·알림 끄기).
create or replace function public.push_unregister(p_token text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  delete from public.push_tokens where token = p_token and user_id = auth.uid();
end;
$$;

grant execute on function public.push_register(text, text) to authenticated;
grant execute on function public.push_unregister(text) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0014_events.sql
-- ══════════════════════════════════════════════════════════════════

-- 도전 보상 — 서버가 확인하고 서버가 지급한다.
--
-- 도전(주간 걸음·나이트 러너)의 "받기"는 폰이 목표를 채웠다고 판단하면 폰 안
-- 원장에만 SUP 를 적었다. 폰을 고치면 몇 번이든 받을 수 있었고, 서버 원장
-- (sup_ledger)에는 남지 않아 기기를 바꾸면 사라졌다.
--
-- 이제 받기는 event_claim() 하나로만 한다. 서버가 자기 기록으로 목표를 다시
-- 재고, 기간마다 한 번만, 원장에 EARN_EVENT 로 적는다. 앱은 서버가 준 금액을
-- 받은 뒤에야 "받음"으로 바꾼다.

-- ── 일별 걸음 올리기 ───────────────────────────────────────────────
--
-- 주간 걸음 도전은 러닝이 아닌 걸음까지 센다. 그 값은 폰에만 있었다. 표(0002
-- daily_steps)에 직접 쓸 수도 있지만, 여기로만 받으면 두 가지를 지킨다.
--   - 하루 걸음이 줄어들지 않는다(걸음은 하루 안에서 늘기만 한다).
--   - 하루 상한(economy.max_daily_steps)을 넘지 못한다.
create or replace function public.steps_sync(p_days json)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_today bigint := (now() at time zone 'UTC')::date - date '1970-01-01';
  v_day json;
  v_epoch bigint;
  v_steps int;
  v_goal int;
  v_count int := 0;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_days is null or json_typeof(p_days) <> 'array' or json_array_length(p_days) > 31 then
    raise exception '걸음 기록이 올바르지 않습니다' using errcode = '22023';
  end if;

  for v_day in select * from json_array_elements(p_days) loop
    v_epoch := (v_day->>'epoch_day')::bigint;
    v_steps := least(greatest(coalesce((v_day->>'steps')::int, 0), 0), economy.max_daily_steps());
    v_goal := least(greatest(coalesce((v_day->>'goal')::int, 8000), 1), 100000);
    -- 시간대 차이로 하루 앞선 날짜까지만 받는다. 먼 미래·먼 과거는 버린다.
    if v_epoch is null or v_epoch > v_today + 1 or v_epoch < v_today - 30 then
      continue;
    end if;
    insert into public.daily_steps (user_id, epoch_day, steps, goal, updated_at)
    values (v_user, v_epoch, v_steps, v_goal, now())
    on conflict (user_id, epoch_day) do update
      set steps = greatest(public.daily_steps.steps, excluded.steps),
          goal = excluded.goal,
          updated_at = now();
    v_count := v_count + 1;
  end loop;
  return v_count;
end;
$$;

-- ── 도전 ───────────────────────────────────────────────────────────

-- 도전 정의. 금액과 목표는 여기가 정본이다(앱의 Events 와 같은 값).
create or replace function economy.event_reward(p_event text) returns numeric
  language sql immutable as $$
    select case p_event when 'step_surge' then 250 when 'night_quest' then 300 end
  $$;

create or replace function economy.event_target(p_event text) returns double precision
  language sql immutable as $$
    select case p_event when 'step_surge' then 80000 when 'night_quest' then 20 end
  $$;

-- 나이트 러너가 세는 시작 시각(현지 시각)
create or replace function economy.night_from_hour() returns int
  language sql immutable as $$ select 20 $$;

create table if not exists public.event_claims (
  user_id uuid not null references auth.users on delete cascade,
  event_id text not null check (event_id in ('step_surge', 'night_quest')),
  -- 한 번씩 받는 단위. 주간 도전은 ISO 주('2026-W39'), 한정 도전은 'once'.
  period text not null,
  amount numeric(20, 4) not null check (amount > 0),
  progress double precision not null default 0,
  claimed_at timestamptz not null default now(),
  primary key (user_id, event_id, period)
);

comment on table public.event_claims is
  '도전 보상을 받은 기록. 기간마다 한 줄 — 같은 기간에 두 번 받을 수 없다.';

alter table public.event_claims enable row level security;
revoke insert, update, delete on public.event_claims from anon, authenticated;
drop policy if exists event_claims_select_own on public.event_claims;
create policy event_claims_select_own on public.event_claims
  for select using ((select auth.uid()) = user_id);
grant select on public.event_claims to authenticated;

-- 도전 기간. 주간 도전은 이번 ISO 주, 나머지는 한 번.
create or replace function economy.event_period(p_event text, p_tz text) returns text
  language sql stable as $$
    select case p_event
      when 'step_surge' then to_char(now() at time zone p_tz, 'IYYY-"W"IW')
      else 'once'
    end
  $$;

-- 서버 기록으로 잰 진행값. 주간 걸음: 최근 7일(오늘 포함) 걸음 합.
-- 나이트 러너: 현지 저녁 8시 이후에 시작한 러닝 거리(km) 합 — 판정에서 걸린
-- 세션(FLAGGED · VOID)은 빼고 센다.
create or replace function public.event_progress(p_event text, p_tz text default 'Asia/Seoul')
returns double precision
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_tz text := coalesce(nullif(p_tz, ''), 'Asia/Seoul');
  v_today bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not exists (select 1 from pg_timezone_names where name = v_tz) then
    v_tz := 'Asia/Seoul';
  end if;
  v_today := (now() at time zone v_tz)::date - date '1970-01-01';

  if p_event = 'step_surge' then
    return coalesce((
      select sum(d.steps) from public.daily_steps d
       where d.user_id = v_user and d.epoch_day between v_today - 6 and v_today
    ), 0);
  elsif p_event = 'night_quest' then
    return coalesce((
      select sum(s.distance_meters) / 1000.0 from public.walk_sessions s
       where s.user_id = v_user
         and s.verdict not in ('FLAGGED', 'VOID')
         and extract(hour from s.started_at at time zone v_tz) >= economy.night_from_hour()
    ), 0);
  end if;
  raise exception '없는 도전입니다' using errcode = '22023';
end;
$$;

-- 도전 보상 받기. 목표를 채웠고 이번 기간에 아직 안 받았으면 원장에 적고 금액을
-- 돌려준다. 이미 받았으면 23505, 목표 미달이면 23514 로 거절한다.
create or replace function public.event_claim(p_event text, p_tz text default 'Asia/Seoul')
returns numeric
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_tz text := coalesce(nullif(p_tz, ''), 'Asia/Seoul');
  v_reward numeric := economy.event_reward(p_event);
  v_progress double precision;
  v_period text;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if v_reward is null then
    raise exception '없는 도전입니다' using errcode = '22023';
  end if;
  if not exists (select 1 from pg_timezone_names where name = v_tz) then
    v_tz := 'Asia/Seoul';
  end if;

  -- 같은 사람의 동시 요청이 둘 다 통과하지 않게 줄 세운다
  perform pg_advisory_xact_lock(hashtext('event_claim:' || v_user::text));

  v_period := economy.event_period(p_event, v_tz);
  if exists (select 1 from public.event_claims
              where user_id = v_user and event_id = p_event and period = v_period) then
    raise exception '이미 받은 보상입니다' using errcode = '23505';
  end if;

  v_progress := public.event_progress(p_event, v_tz);
  if v_progress < economy.event_target(p_event) then
    raise exception '아직 목표를 채우지 않았습니다 (%/%)', round(v_progress::numeric, 1),
      economy.event_target(p_event) using errcode = '23514';
  end if;

  insert into public.event_claims (user_id, event_id, period, amount, progress)
  values (v_user, p_event, v_period, v_reward, v_progress);
  insert into public.sup_ledger (user_id, kind, amount, description)
  values (v_user, 'EARN_EVENT', v_reward, '도전 보상: ' || p_event || ' ' || v_period);
  return v_reward;
end;
$$;

revoke execute on function public.event_progress(text, text) from public, anon;
revoke execute on function public.event_claim(text, text) from public, anon;
revoke execute on function public.steps_sync(json) from public, anon;
grant execute on function public.event_progress(text, text) to authenticated;
grant execute on function public.event_claim(text, text) to authenticated;
grant execute on function public.steps_sync(json) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0015_crew_ranking.sql
-- ══════════════════════════════════════════════════════════════════

-- 크루 순위 — 크루원 모두가 크루로 달린 거리를 서버에서 합친다.
--
-- 크루 순위는 이 폰에서 달린 크루 러닝만 셌다. 크루원이 열 명이어도 순위에는
-- 내 거리만 들어가서, 크루끼리 겨루는 표가 되지 못했다. 이제 러닝이 서버에
-- 올라갈 때 어느 크루로 달렸는지를 적고(session_tag_crew), 순위는 서버가 모든
-- 크루원의 기록으로 센다(crew_leaderboard).

alter table public.walk_sessions
  add column if not exists crew_id uuid references public.crews on delete set null;

create index if not exists walk_sessions_crew
  on public.walk_sessions (crew_id, started_at) where crew_id is not null;

comment on column public.walk_sessions.crew_id is
  '크루 러닝이었다면 그 크루. 러닝을 올린 뒤 session_tag_crew 로 적는다. 크루원일 때만 적힌다.';

-- 방금 올린 러닝에 크루를 적는다. 그 크루원이어야 하고, 한 번 적힌 크루는 바꾸지
-- 않는다 — 크루를 옮겨 다니며 같은 거리를 여러 크루에 얹지 못하게.
create or replace function public.session_tag_crew(p_started_at timestamptz, p_crew uuid)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_crew is null or not public.is_crew_member(p_crew) then
    raise exception '이 크루의 멤버가 아닙니다' using errcode = '42501';
  end if;
  update public.walk_sessions
     set crew_id = p_crew
   where user_id = auth.uid() and started_at = p_started_at and crew_id is null;
  return found;
end;
$$;

-- 크루 순위. 모든 크루가 나온다 — 아직 안 달린 크루는 0 km 로.
-- 판정에서 무효(VOID)가 된 러닝은 빼고, 숨겨진 크루(신고 5건)는 뺀다.
create or replace function public.crew_leaderboard(p_period text default 'ALL')
returns table (
  crew_id uuid,
  km double precision,
  runs int,
  runners int
)
language sql
stable
security definer
set search_path = public
as $$
  select
    c.id,
    coalesce(sum(s.distance_meters), 0) / 1000.0,
    count(s.id)::int,
    count(distinct s.user_id)::int
  from public.crews c
  left join public.walk_sessions s
    on s.crew_id = c.id
   and s.verdict <> 'VOID'
   and s.started_at >= public.rank_period_start(p_period)
  where not public.is_hidden('CREW', c.id::text)
  group by c.id
  order by 2 desc, c.id
$$;

comment on function public.crew_leaderboard(text) is
  '크루별로 크루원 모두가 크루로 달린 거리를, p_period(DAY·WEEK·MONTH·ALL) 기간으로.';

revoke execute on function public.session_tag_crew(timestamptz, uuid) from public, anon;
revoke execute on function public.crew_leaderboard(text) from public, anon;
grant execute on function public.session_tag_crew(timestamptz, uuid) to authenticated;
grant execute on function public.crew_leaderboard(text) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0016_notify_prefs.sql
-- ══════════════════════════════════════════════════════════════════

-- 알림 설정 — 어떤 푸시를 받을지. 폰의 설정 화면과 같은 네 가지.
--
-- 알림은 서버가 보낸다. 그래서 "파티 초대는 받지 않음" 같은 선택도 서버가
-- 알아야 지켜진다. 폰에만 두면 끈 알림이 계속 온다.

create table if not exists public.notify_prefs (
  user_id uuid primary key references auth.users on delete cascade,
  -- 끄면 어떤 푸시도 보내지 않는다
  push boolean not null default true,
  goal_reminder boolean not null default true,
  party_invite boolean not null default true,
  event_news boolean not null default true,
  updated_at timestamptz not null default now()
);

comment on table public.notify_prefs is
  '받을 푸시 종류. 줄이 없으면 전부 받음. 보내는 쪽(Edge Function)이 보내기 전에 본다.';

alter table public.notify_prefs enable row level security;
revoke insert, update, delete on public.notify_prefs from anon, authenticated;
drop policy if exists notify_prefs_select_own on public.notify_prefs;
create policy notify_prefs_select_own on public.notify_prefs
  for select using ((select auth.uid()) = user_id);
grant select on public.notify_prefs to authenticated;

create or replace function public.notify_prefs_set(
  p_push boolean,
  p_goal_reminder boolean,
  p_party_invite boolean,
  p_event_news boolean
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
  insert into public.notify_prefs (user_id, push, goal_reminder, party_invite, event_news, updated_at)
  values (auth.uid(), coalesce(p_push, true), coalesce(p_goal_reminder, true),
          coalesce(p_party_invite, true), coalesce(p_event_news, true), now())
  on conflict (user_id) do update
    set push = excluded.push,
        goal_reminder = excluded.goal_reminder,
        party_invite = excluded.party_invite,
        event_news = excluded.event_news,
        updated_at = now();
end;
$$;

revoke execute on function public.notify_prefs_set(boolean, boolean, boolean, boolean) from public, anon;
grant execute on function public.notify_prefs_set(boolean, boolean, boolean, boolean) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0017_push_outbox.sql
-- ══════════════════════════════════════════════════════════════════

-- 푸시 보낼 목록 — 무엇을 누구에게 보낼지는 여기(데이터베이스)가 정하고,
-- 실제로 보내는 일은 Edge Function(supabase/functions/push-send)이 한다.
--
-- 받는 사람을 고르는 규칙(자기 글에 자기 댓글은 알리지 않음, 알림 설정에서 끈
-- 종류는 빼기)을 SQL 로 두면 테스트로 지킬 수 있다. 보내는 쪽은 이 표를 비우기만
-- 한다 — 행이 생기면 Database Webhook 이 함수를 깨운다.

create table if not exists public.push_outbox (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,
  -- COMMENT · REPLY · CREW_REQUEST · PARTY_OPEN · CREW_FLASH
  kind text not null,
  -- 알림 글에 넣을 값(누가 · 어느 글 · 어느 크루). 글은 받는 사람의 언어로 함수가 짓는다.
  args jsonb not null default '{}'::jsonb,
  -- 알림을 눌렀을 때 열 앱 안 자리(예: crew/<id>). 앱의 InviteLinks 가 읽는다.
  link text not null default '',
  created_at timestamptz not null default now(),
  sent_at timestamptz,
  attempts int not null default 0,
  last_error text not null default ''
);

create index if not exists push_outbox_pending on public.push_outbox (created_at) where sent_at is null;

-- 보내는 쪽이 가져간 시각. 가져간 줄은 잠시 다른 호출에 다시 주지 않는다(아래 push_claim_batch).
alter table public.push_outbox add column if not exists claimed_at timestamptz;

comment on table public.push_outbox is
  '보낼 푸시. 트리거가 채우고 push-send 함수가 보낸 뒤 sent_at 을 적는다. 앱은 볼 수 없다.';

alter table public.push_outbox enable row level security;
revoke all on public.push_outbox from anon, authenticated;

-- 이 사람이 이 종류의 푸시를 받는가. 설정 줄이 없으면 받는다.
create or replace function public.push_allowed(p_user uuid, p_kind text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce((
    select n.push and case p_kind
      when 'PARTY_OPEN' then n.party_invite
      when 'CREW_FLASH' then n.event_news
      else true
    end
    from public.notify_prefs n where n.user_id = p_user
  ), true)
$$;

create or replace function public.push_enqueue(p_user uuid, p_kind text, p_args jsonb, p_link text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if p_user is null or not public.push_allowed(p_user, p_kind) then
    return;
  end if;
  -- 받을 폰이 없으면 적어 둘 까닭이 없다
  if not exists (select 1 from public.push_tokens t where t.user_id = p_user) then
    return;
  end if;
  insert into public.push_outbox (user_id, kind, args, link)
  values (p_user, p_kind, coalesce(p_args, '{}'::jsonb), coalesce(p_link, ''));
end;
$$;

create or replace function public.push_display_name(p_user uuid)
returns text
language sql
stable
security definer
set search_path = public
as $$
  select coalesce((select display_name from public.profiles where id = p_user), '러너')
$$;

-- 댓글 → 글쓴이에게, 답글 → 댓글 단 사람에게. 자기 자신에게는 보내지 않는다.
create or replace function public.push_on_comment()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_to uuid;
  v_kind text;
  v_title text;
  v_crew uuid;
begin
  select p.title, p.crew_id into v_title, v_crew from public.posts p where p.id = new.post_id;
  if new.parent_id is not null then
    select c.author_id into v_to from public.comments c where c.id = new.parent_id;
    v_kind := 'REPLY';
  else
    select p.author_id into v_to from public.posts p where p.id = new.post_id;
    v_kind := 'COMMENT';
  end if;
  if v_to is not null and v_to <> new.author_id then
    perform public.push_enqueue(
      v_to, v_kind,
      jsonb_build_object('name', public.push_display_name(new.author_id), 'title', coalesce(v_title, '')),
      case when v_crew is not null then 'crew/' || v_crew::text else '' end);
  end if;
  return new;
end;
$$;

-- 가입 신청 → 크루장에게
create or replace function public.push_on_crew_request()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_owner uuid;
  v_name text;
begin
  select c.owner_id, c.name into v_owner, v_name from public.crews c where c.id = new.crew_id;
  if v_owner is not null and v_owner <> new.user_id then
    perform public.push_enqueue(
      v_owner, 'CREW_REQUEST',
      jsonb_build_object('name', public.push_display_name(new.user_id), 'crew', coalesce(v_name, '')),
      'crew/' || new.crew_id::text);
  end if;
  return new;
end;
$$;

-- 크루 파티런 로비가 열렸다 → 그 크루원들에게(연 사람 빼고)
create or replace function public.push_on_party()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_name text;
  v_member uuid;
begin
  if new.crew_id is null then
    return new;
  end if;
  select c.name into v_name from public.crews c where c.id = new.crew_id;
  for v_member in
    select m.user_id from public.crew_members m
     where m.crew_id = new.crew_id and m.user_id <> new.host_id
  loop
    perform public.push_enqueue(
      v_member, 'PARTY_OPEN',
      jsonb_build_object('name', public.push_display_name(new.host_id), 'crew', coalesce(v_name, '')),
      'crew/' || new.crew_id::text);
  end loop;
  return new;
end;
$$;

-- 크루 게시판에 번개가 올라왔다 → 그 크루원들에게(쓴 사람 빼고)
create or replace function public.push_on_flash()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_member uuid;
begin
  if new.category <> 'FLASH' or new.crew_id is null then
    return new;
  end if;
  for v_member in
    select m.user_id from public.crew_members m
     where m.crew_id = new.crew_id and m.user_id <> new.author_id
  loop
    perform public.push_enqueue(
      v_member, 'CREW_FLASH',
      jsonb_build_object('name', public.push_display_name(new.author_id), 'title', new.title),
      'crew/' || new.crew_id::text);
  end loop;
  return new;
end;
$$;

drop trigger if exists push_on_comment on public.comments;
create trigger push_on_comment after insert on public.comments
  for each row execute function public.push_on_comment();

drop trigger if exists push_on_crew_request on public.crew_join_requests;
create trigger push_on_crew_request after insert on public.crew_join_requests
  for each row execute function public.push_on_crew_request();

drop trigger if exists push_on_party on public.parties;
create trigger push_on_party after insert on public.parties
  for each row execute function public.push_on_party();

drop trigger if exists push_on_flash on public.posts;
create trigger push_on_flash after insert on public.posts
  for each row execute function public.push_on_flash();

revoke execute on function public.push_allowed(uuid, text) from public, anon, authenticated;
revoke execute on function public.push_enqueue(uuid, text, jsonb, text) from public, anon, authenticated;
revoke execute on function public.push_display_name(uuid) from public, anon, authenticated;

-- ── 보내는 쪽(push-send, service_role)만 쓰는 함수 ────────────────

-- 보낼 것을 한 묶음 가져간다. 함수가 동시에 두 번 깨어나도 같은 줄을 두 번
-- 보내지 않게 잠근 줄은 건너뛴다. 다섯 번 실패한 줄은 더 시도하지 않는다.
-- 행 잠금은 이 호출이 끝나면 풀린다. 보내고 push_mark 로 적기 전까지 다음 호출이
-- 같은 줄을 또 가져가 두 번 울리지 않게, 가져간 시각을 적고 2분은 건너뛴다.
-- 실패한 줄도 2분 뒤에 다시 가져간다 — 곧바로 다시 가져가면 FCM 이 잠깐 아플 때
-- 다섯 번을 몇 초 만에 다 써 버리고 영영 못 보낸다.
create or replace function public.push_claim_batch(p_limit int default 100)
returns table (
  id bigint,
  user_id uuid,
  kind text,
  args jsonb,
  link text,
  tokens jsonb
)
language sql
security definer
set search_path = public
as $$
  with picked as (
    select o.id from public.push_outbox o
     where o.sent_at is null and o.attempts < 5
       and (o.claimed_at is null or o.claimed_at < now() - interval '2 minutes')
     order by o.id
     limit least(greatest(coalesce(p_limit, 100), 1), 500)
     for update skip locked
  ), bumped as (
    update public.push_outbox o
       set attempts = o.attempts + 1,
           claimed_at = now()
      from picked
     where o.id = picked.id
    returning o.id, o.user_id, o.kind, o.args, o.link
  )
  select b.id, b.user_id, b.kind, b.args, b.link,
         coalesce((select jsonb_agg(jsonb_build_object('token', t.token, 'locale', t.locale))
                     from public.push_tokens t where t.user_id = b.user_id), '[]'::jsonb)
    from bumped b
$$;

-- 보냈다고 적는다(실패면 이유를 남긴다)
create or replace function public.push_mark(p_id bigint, p_sent boolean, p_error text default '')
returns void
language sql
security definer
set search_path = public
as $$
  update public.push_outbox
     set sent_at = case when p_sent then now() else sent_at end,
         last_error = left(coalesce(p_error, ''), 500)
   where id = p_id
$$;

-- 더는 받지 않는 폰(앱을 지웠거나 토큰이 바뀜)을 지운다
create or replace function public.push_forget_token(p_token text)
returns void
language sql
security definer
set search_path = public
as $$
  delete from public.push_tokens where token = p_token
$$;

revoke execute on function public.push_claim_batch(int) from public, anon, authenticated;
revoke execute on function public.push_mark(bigint, boolean, text) from public, anon, authenticated;
revoke execute on function public.push_forget_token(text) from public, anon, authenticated;
do $$
begin
  -- 로컬 검사용 Postgres 에는 service_role 이 없을 수 있다
  if exists (select 1 from pg_roles where rolname = 'service_role') then
    grant execute on function public.push_claim_batch(int) to service_role;
    grant execute on function public.push_mark(bigint, boolean, text) to service_role;
    grant execute on function public.push_forget_token(text) to service_role;
    grant select, update on public.push_outbox to service_role;
  end if;
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 0018_gps_verify.sql
-- ══════════════════════════════════════════════════════════════════

-- 서버 GPS 검증 — 경로로 거리를 재고, 걸음과 경로가 서로 맞는지 본다.
--
-- 지금까지 서버는 경로에서 최고 속도와 "말이 안 되는 구간"만 봤다. 거리는
-- 걸음 × 0.762 m 로 셌기 때문에, 폰을 흔들어 걸음만 늘리면 거리와 적립이 같이
-- 늘었다. 이제 서버가 경로로 거리를 직접 재고 다음을 판정한다.
--
--   - 경로 시각이 러닝 시간 밖이면 VOID      — 다른 날 경로를 붙여 올린 것
--   - 이미 올린 경로와 똑같으면 VOID         — 같은 경로를 다시 쓴 것
--   - 1km 넘게 움직였는데 걸음이 거의 없으면 VOID — 자전거·킥보드
--   - 걸음 거리가 GPS 거리의 2배를 넘으면 FLAGGED — 경로가 받쳐 주는 만큼만 적립
--
-- 실내(러닝머신)처럼 경로가 짧은 세션은 경로 검사를 하지 않는다. 그 경우는
-- 케이던스 검사(0003)만 받는다.

-- ── 경로 거리 ──────────────────────────────────────────────────────
drop function if exists economy.track_summary(text);

create or replace function economy.track_summary(p_track text)
returns table (
  gps_m double precision,
  points int,
  first_at bigint,
  last_at bigint
)
language plpgsql immutable as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_chunk text;
  v_parts text[];
  v_lat double precision;
  v_lng double precision;
  v_at bigint;
  v_plat double precision;
  v_plng double precision;
  v_pat bigint;
  v_have boolean := false;
  v_d double precision;
  v_dt double precision;
  v_total double precision := 0;
  v_n int := 0;
  v_first bigint;
  v_last bigint;
begin
  if p_track is null or p_track = '' then
    return query select 0::double precision, 0, null::bigint, null::bigint;
    return;
  end if;

  foreach v_chunk in array string_to_array(p_track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) <> 3;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num or v_parts[3] !~ '^[0-9]+$';

    v_lat := v_parts[1]::double precision;
    v_lng := v_parts[2]::double precision;
    v_at := v_parts[3]::bigint;
    continue when v_lat not between -90 and 90 or v_lng not between -180 and 180;

    v_n := v_n + 1;
    v_first := least(coalesce(v_first, v_at), v_at);
    v_last := greatest(coalesce(v_last, v_at), v_at);

    if v_have then
      v_d := economy.haversine_m(v_plat, v_plng, v_lat, v_lng);
      v_dt := (v_at - v_pat) / 1000.0;
      -- 튄 구간(시속 60km 초과)은 거리에 넣지 않는다. 시각이 같거나 거꾸로 간
      -- 점도 넣지 않는다 — 한 점이 튀었다고 수백 m 가 거저 생기면 안 된다.
      if v_dt > 0 and v_d / v_dt * 3.6 <= economy.speed_glitch_kmh() then
        v_total := v_total + v_d;
      end if;
    end if;
    v_plat := v_lat; v_plng := v_lng; v_pat := v_at; v_have := true;
  end loop;

  return query select v_total, v_n, v_first, v_last;
end;
$$;

comment on function economy.track_summary is
  '경로의 실측 거리(튄 구간 제외)와 점 개수, 첫·마지막 시각(epoch ms).';

-- 경로 검사를 할 만큼 움직였는가. 이보다 짧으면 실내로 본다.
create or replace function economy.gps_check_min_m() returns double precision
  language sql immutable as $$ select 300::double precision $$;

-- 걸음 거리가 GPS 거리의 몇 배까지면 받아 주나. 보폭은 사람마다 다르고
-- GPS 는 모퉁이를 깎아 먹으므로 넉넉하게 2배.
create or replace function economy.step_gps_max_ratio() returns double precision
  language sql immutable as $$ select 2::double precision $$;

-- 경로 시각이 러닝 시간에서 이만큼(ms)까지 벗어나도 봐준다.
create or replace function economy.track_time_slack_ms() returns bigint
  language sql immutable as $$ select 120000::bigint $$;

alter table public.walk_sessions
  add column if not exists gps_distance_m double precision not null default 0;

comment on column public.walk_sessions.gps_distance_m is
  '서버가 경로에서 직접 잰 거리(m). 튄 구간은 빼고 잰다. 경로가 없으면 0.';

create index if not exists walk_sessions_user_track
  on public.walk_sessions (user_id, md5(track)) where track <> '';

-- ── 적립 (0003 을 대신한다) ────────────────────────────────────────
--
-- 인자는 그대로다. 앱은 바뀌지 않아도 된다.
create or replace function public.record_session(
  p_started_at timestamptz,
  p_ended_at timestamptz,
  p_steps int,
  p_duration_sec int,
  p_track text default '',
  p_boost_bps int default 0,
  p_party_size int default 1,
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
  v_gps_m double precision := 0;
  v_points_n int := 0;
  v_first_at bigint;
  v_last_at bigint;
  v_step_m double precision;
  v_distance_m double precision;
  v_track text := coalesce(p_track, '');
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
  if p_ended_at > now() + interval '5 minutes' then
    raise exception '종료 시각이 미래입니다' using errcode = '22023';
  end if;
  -- 오래된 날짜로 세션을 지어 올리면 날마다 하루 상한이 새로 열린다. 온체인 청구
  -- 창(RewardDistributor.CLAIM_WINDOW_DAYS)과 같은 7일까지만 받는다.
  -- 이미 올린 러닝의 재시도는 날짜와 상관없이 원래 결과를 돌려준다(아래 on conflict).
  if p_started_at < now() - interval '7 days' and not exists (
       select 1 from public.walk_sessions s
        where s.user_id = v_user and s.started_at = p_started_at) then
    raise exception '너무 오래된 러닝입니다' using errcode = '22023';
  end if;

  -- 같은 사람의 적립이 동시에 들어오면(재시도 겹침) 둘 다 오늘 합계를 같게 읽어
  -- 하루 상한을 두 번 받는다. 이 사람의 원장 쓰기를 한 줄로 세운다.
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  -- 운동 시간은 앱이 보낸 값을 믿지 않는다. 일시정지를 빼므로 시작~종료보다
  -- 길 수는 없다. 크게 적어 시간 순위에 오르거나 0 으로 적어 케이던스 검사를
  -- 피하지 못하게 한다.
  v_elapsed := least(
    greatest(coalesce(p_duration_sec, 0), 0),
    floor(extract(epoch from (p_ended_at - p_started_at)))::int
  );
  v_day := floor(extract(epoch from p_started_at) / 86400)::bigint;
  v_faction := case when coalesce(p_faction, '') in ('FIRE', 'WATER', 'LIGHTNING', 'WIND')
                    then p_faction else '' end;
  v_step_m := p_steps * 0.762;

  select t.top_speed_kmh, t.glitch_ratio
    into v_top_speed, v_glitch
    from economy.track_speed_stats(v_track) t;

  select t.gps_m, t.points, t.first_at, t.last_at
    into v_gps_m, v_points_n, v_first_at, v_last_at
    from economy.track_summary(v_track) t;

  -- ── 판정 ──
  -- 1분이 안 되는 러닝도 본다. 1분으로 쳐서 240보를 넘으면 사람 걸음이 아니다
  -- (0초에 4만 보를 적어 검사를 건너뛰지 못하게).
  if p_steps::numeric * 60 / greatest(v_elapsed, 60) > 240 then
    v_verdict := 'VOID';
    v_reason := '케이던스가 사람 범위를 벗어납니다';

  elsif v_glitch > 0.5 then
    v_verdict := 'VOID';
    v_reason := '이동 속도가 사람 범위를 벗어납니다';

  -- 경로는 이 러닝 동안 찍힌 것이어야 한다. 어제 뛴 경로를 오늘 세션에 붙이면
  -- 한 번 뛰고 두 번 받는다.
  elsif v_points_n >= 2 and (
        v_first_at < (extract(epoch from p_started_at) * 1000)::bigint - economy.track_time_slack_ms()
     or v_last_at > (extract(epoch from p_ended_at) * 1000)::bigint + economy.track_time_slack_ms()) then
    v_verdict := 'VOID';
    v_reason := '경로 시각이 러닝 시간과 맞지 않습니다';

  -- 똑같은 경로를 다른 세션으로 다시 올렸다. 사람이 두 번 뛰어서 좌표와 시각이
  -- 한 글자도 안 틀리게 같을 수는 없다. (같은 세션의 재시도는 시작 시각이 같아
  -- 여기 걸리지 않고, 아래 on conflict 가 원래 결과를 돌려준다.)
  elsif v_track <> '' and exists (
        select 1 from public.walk_sessions s
         where s.user_id = v_user
           and s.track <> ''
           and md5(s.track) = md5(v_track)
           and s.started_at <> p_started_at) then
    v_verdict := 'VOID';
    v_reason := '이미 올린 경로입니다';

  -- 1km 넘게 움직였는데 걸음이 그 20% 도 안 된다 — 발이 아니라 바퀴다.
  elsif v_gps_m >= 1000 and v_step_m < v_gps_m * 0.2 then
    v_verdict := 'VOID';
    v_reason := '걸음 없이 이동한 거리입니다';
  end if;

  -- ── 적립 대상 걸음 ──
  if v_verdict = 'VOID' then
    v_rewardable := 0;
  else
    v_rewardable := p_steps;

    -- 걸음이 경로보다 훨씬 많다 — 폰을 흔든 몫은 경로가 받쳐 주는 만큼만.
    if v_gps_m >= economy.gps_check_min_m()
       and v_step_m > v_gps_m * economy.step_gps_max_ratio() then
      v_rewardable := floor(v_gps_m * economy.step_gps_max_ratio() / 0.762)::int;
      v_verdict := 'FLAGGED';
      v_reason := '걸음 수가 GPS 거리보다 많습니다';
    end if;

    select coalesce(sum(s.rewarded_steps), 0) into v_already_today
      from public.walk_sessions s
     where s.user_id = v_user
       and s.verdict in ('CLEAN', 'FLAGGED')
       and floor(extract(epoch from s.started_at) / 86400)::bigint = v_day;

    if v_rewardable > economy.max_daily_steps() - v_already_today then
      v_rewardable := greatest(economy.max_daily_steps() - v_already_today, 0);
      v_verdict := 'FLAGGED';
      v_reason := case when v_reason = '' then '하루 적립 상한에 걸렸습니다'
                       else v_reason || ' · 하루 적립 상한에 걸렸습니다' end;
    end if;
  end if;

  v_points := round(
    v_rewardable * economy.points_per_step()
      * economy.party_multiplier(p_party_size)
      * economy.boost_multiplier(p_boost_bps),
    4
  );

  -- 거리 — 경로가 충분하면 경로로, 아니면(실내·GPS 없음) 걸음으로.
  -- 경로 검사에 걸려 걸음이 깎였으면 거리도 인정된 걸음만큼이다.
  v_distance_m := case
    when v_gps_m >= 100 then least(v_gps_m, greatest(v_rewardable, 0) * 0.762 * 3 + 100)
    else v_step_m
  end;
  if v_verdict = 'VOID' then v_distance_m := 0; end if;

  -- ── 기록 ──
  insert into public.walk_sessions (
    user_id, started_at, ended_at, duration_sec, steps,
    distance_meters, calories, track, boost_bps, party_size,
    faction, top_speed_kmh, gps_distance_m,
    verdict, verdict_reason, points_awarded, rewarded_steps
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    -- 계산에 쓴 것과 같은 범위로 적는다. 날것을 적으면 표의 범위 검사에 걸려
    -- 세션이 통째로 거절되고, 재시도해도 같은 이유로 영영 올라가지 않는다.
    v_distance_m, p_steps * 0.04, v_track,
    least(greatest(coalesce(p_boost_bps, 0), 0), 2000),
    least(greatest(coalesce(p_party_size, 1), 1), 20),
    v_faction, case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable
  )
  on conflict (user_id, started_at) do nothing
  returning id into v_session_id;

  if v_session_id is null then
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

  if v_verdict <> 'VOID' then
    update public.profiles
       set lifetime_km = lifetime_km + (v_distance_m / 1000),
           top_speed_kmh = greatest(top_speed_kmh, v_top_speed)
     where id = v_user;
  end if;

  return query
    select v_session_id, v_verdict, v_points,
           coalesce((select b.balance from public.sup_balances b where b.user_id = v_user), 0);
end;
$$;

comment on function public.record_session is
  '러닝 세션을 기록하고 적립액을 서버가 직접 계산한다. 경로로 거리를 재고 걸음과 맞는지 본다.';

grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text)
  to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0019_course_runs.sql
-- ══════════════════════════════════════════════════════════════════

-- 코스별 기록 순위 — 코스를 따라 달린 기록을 서버가 확인하고 순위를 낸다.
--
-- 앱은 "이 러닝이 이 코스였다"고 알려 줄 뿐이다. 서버는 이미 받은 러닝 경로가
-- 정말 코스를 따라갔는지(코스 지점의 80% 를 50m 안으로 지났는지) 직접 보고,
-- 러닝 시간을 그 코스 기록으로 남긴다. 코스의 "달린 횟수"도 여기서 센다.
--
-- 코스는 번호가 아니라 **길**로 찾는다. 앱은 게시판 코스를 받아 폰에 따로
-- 저장하므로 서버 번호를 모른다. 길 문자열(md5)은 둘이 똑같다.

create table if not exists public.course_runs (
  id bigint generated always as identity primary key,
  course_id bigint not null references public.courses on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  session_id bigint not null references public.walk_sessions on delete cascade,
  duration_sec int not null check (duration_sec > 0),
  created_at timestamptz not null default now(),
  unique (course_id, session_id)
);

create index if not exists course_runs_board on public.course_runs (course_id, duration_sec);
create index if not exists courses_track_md5 on public.courses (md5(track));

comment on table public.course_runs is
  '코스를 따라 달린 기록. 서버가 러닝 경로와 코스를 맞춰 본 것만 들어온다.';

alter table public.course_runs enable row level security;
-- 직접 읽고 쓰지 않는다. 아래 함수로만.
revoke all on public.course_runs from anon, authenticated;

-- 코스 지점 중 몇 %를 러닝 경로가 p_meters 안으로 지나갔나.
-- 코스 점은 최대 80개로 솎아서 본다 — 긴 코스도 계산이 금방 끝나게.
create or replace function economy.course_coverage(
  p_course text,
  p_track text,
  p_meters double precision default 50
)
returns double precision
language plpgsql immutable as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_course double precision[][] := '{}';
  v_run_lat double precision[] := '{}';
  v_run_lng double precision[] := '{}';
  v_parts text[];
  v_chunk text;
  v_n int;
  v_step int;
  v_hit int := 0;
  v_seen int := 0;
  v_lat double precision;
  v_lng double precision;
  v_deg double precision := p_meters / 111000.0;
  i int;
  j int;
begin
  foreach v_chunk in array string_to_array(coalesce(p_track, ''), ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) < 2;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num;
    v_run_lat := v_run_lat || v_parts[1]::double precision;
    v_run_lng := v_run_lng || v_parts[2]::double precision;
  end loop;
  if coalesce(array_length(v_run_lat, 1), 0) = 0 then
    return 0;
  end if;

  v_parts := string_to_array(coalesce(p_course, ''), ';');
  v_n := coalesce(array_length(v_parts, 1), 0);
  if v_n = 0 then
    return 0;
  end if;
  v_step := greatest(1, ceil(v_n / 80.0)::int);

  i := 1;
  while i <= v_n loop
    v_chunk := v_parts[i];
    i := i + v_step;
    continue when split_part(v_chunk, ',', 1) !~ v_num or split_part(v_chunk, ',', 2) !~ v_num;
    v_lat := split_part(v_chunk, ',', 1)::double precision;
    v_lng := split_part(v_chunk, ',', 2)::double precision;
    v_seen := v_seen + 1;
    for j in 1 .. array_length(v_run_lat, 1) loop
      -- 위도 차이로 먼저 거른다. 하버사인은 비싸다.
      continue when abs(v_run_lat[j] - v_lat) > v_deg;
      if economy.haversine_m(v_lat, v_lng, v_run_lat[j], v_run_lng[j]) <= p_meters then
        v_hit := v_hit + 1;
        exit;
      end if;
    end loop;
  end loop;

  if v_seen = 0 then
    return 0;
  end if;
  return v_hit::double precision / v_seen;
end;
$$;

-- 이 사용자가 볼 수 있는 코스를 길로 찾는다. 공유된 코스가 먼저, 오래된 것이 먼저.
create or replace function public.course_by_track(p_track text)
returns bigint
language sql
stable
security definer
set search_path = public
as $$
  select c.id
    from public.courses c
   where md5(c.track) = md5(coalesce(p_track, ''))
     and (c.shared or c.owner_id = auth.uid())
   order by c.shared desc, c.id
   limit 1
$$;

-- 방금 올린 러닝을 코스 기록으로 낸다.
-- 서버에 없는 코스(내 폰에만 있는 코스)면 아무 줄도 돌려주지 않는다.
create or replace function public.course_run_submit(p_course_track text, p_started_at timestamptz)
returns table (
  course_id bigint,
  duration_sec int,
  rank int,
  runners int
)
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_course bigint;
  v_course_track text;
  v_session public.walk_sessions%rowtype;
  v_inserted bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  v_course := public.course_by_track(p_course_track);
  if v_course is null then
    return;
  end if;
  select c.track into v_course_track from public.courses c where c.id = v_course;

  select * into v_session
    from public.walk_sessions s
   where s.user_id = v_user and s.started_at = p_started_at;
  if not found then
    raise exception '러닝 기록이 서버에 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
  end if;
  if v_session.verdict = 'VOID' or v_session.duration_sec <= 0 then
    raise exception '인정되지 않은 러닝입니다' using errcode = '23514';
  end if;
  if economy.course_coverage(v_course_track, v_session.track) < 0.8 then
    raise exception '코스를 따라 달리지 않았습니다' using errcode = '23514';
  end if;

  insert into public.course_runs (course_id, user_id, session_id, duration_sec)
  values (v_course, v_user, v_session.id, v_session.duration_sec)
  on conflict do nothing
  returning id into v_inserted;

  if v_inserted is not null then
    update public.courses set run_count = run_count + 1 where id = v_course;
  end if;

  return query
    with best as (
      select r.user_id, min(r.duration_sec) as t
        from public.course_runs r
       where r.course_id = v_course
       group by r.user_id
    )
    select v_course,
           (select b.t from best b where b.user_id = v_user),
           (select count(*)::int + 1 from best b
             where b.t < (select b2.t from best b2 where b2.user_id = v_user)),
           (select count(*)::int from best);
end;
$$;

comment on function public.course_run_submit is
  '러닝이 코스를 따라갔는지 서버가 보고 코스 기록으로 남긴다. 코스의 달린 횟수도 올린다.';

-- 코스 순위 — 사람마다 가장 빠른 기록 하나. 내가 막은 사람은 빼고, 나는 늘 넣는다.
create or replace function public.course_leaderboard(p_course_track text, p_limit int default 20)
returns table (
  rank int,
  display_name text,
  duration_sec int,
  runs int,
  is_me boolean
)
language sql
stable
security definer
set search_path = public
as $$
  with c as (
    select public.course_by_track(p_course_track) as id
  ),
  best as (
    select r.user_id, min(r.duration_sec) as t, count(*)::int as n
      from public.course_runs r, c
     where r.course_id = c.id
       and not exists (
         select 1 from public.user_blocks b
          where b.blocker_id = auth.uid() and b.blocked_id = r.user_id)
     group by r.user_id
  ),
  ranked as (
    select (rank() over (order by b.t))::int as rk, b.*
      from best b
  )
  select k.rk, coalesce(p.display_name, '러너'), k.t, k.n, k.user_id = auth.uid()
    from ranked k
    left join public.profiles p on p.id = k.user_id
   where k.rk <= greatest(1, least(coalesce(p_limit, 20), 100)) or k.user_id = auth.uid()
   order by k.rk, k.user_id
$$;

comment on function public.course_leaderboard is
  '코스별 기록 순위. 사람마다 가장 빠른 기록 하나, 상위 p_limit 명과 나.';

revoke execute on function public.course_by_track(text) from public, anon;
revoke execute on function public.course_run_submit(text, timestamptz) from public, anon;
revoke execute on function public.course_leaderboard(text, int) from public, anon;
grant execute on function public.course_by_track(text) to authenticated;
grant execute on function public.course_run_submit(text, timestamptz) to authenticated;
grant execute on function public.course_leaderboard(text, int) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0020_territory.sql
-- ══════════════════════════════════════════════════════════════════

-- 땅따먹기 — 달린 동네를 육각 칸으로 나눠 크루 색으로 칠한다.
--
-- 규칙
--   - 지도를 한 칸 약 200m 짜리 육각형으로 나눈다(웹 메르카토르 기준 반지름
--     150 — 앱의 Territory.kt 와 같은 식이다).
--   - 러닝이 서버에 올라오면 경로가 지난 칸마다 "표시" 하나를 남긴다. 한 사람이
--     한 칸에 하루 한 번만 — 같은 칸을 빙빙 돈다고 점수가 늘지 않는다.
--   - 표시는 그 사람의 크루 몫이다. 크루 러닝이면 그 크루, 아니면 가장 먼저
--     가입한 크루. 크루가 없으면 표시를 남기지 않는다.
--   - 칸의 주인은 최근 14일 동안 표시가 가장 많은 크루다. 달리지 않으면 땅을
--     잃는다 — 그래야 새 크루도 뺏을 수 있다.
--   - 판정에서 무효(VOID)가 된 러닝은 칸을 칠하지 못한다.
--
-- 경로는 서버가 이미 받은 것을 쓴다. 앱이 "이 칸을 지났다"고 말하는 것을 믿지
-- 않는다 — 그러면 누구나 서울 전체를 칠할 수 있다.

create or replace function economy.territory_hex_size() returns double precision
  language sql immutable as $$ select 150::double precision $$;

create or replace function economy.territory_window_days() returns int
  language sql immutable as $$ select 14 $$;

-- 좌표 → 칸 이름("q:r"). 뾰족한 쪽이 위인 육각형, 축 좌표계.
-- 앱의 Territory.cellOf 와 한 글자도 다르지 않게 유지한다.
create or replace function economy.hex_cell(p_lat double precision, p_lng double precision)
returns text
language plpgsql immutable as $$
declare
  v_r constant double precision := 6378137.0;
  v_s double precision := economy.territory_hex_size();
  v_lat double precision := greatest(-85, least(85, p_lat));
  v_x double precision;
  v_y double precision;
  v_q double precision;
  v_rr double precision;
  v_cx double precision;
  v_cz double precision;
  v_cy double precision;
  v_rx double precision;
  v_ry double precision;
  v_rz double precision;
begin
  v_x := v_r * radians(p_lng);
  v_y := v_r * ln(tan(pi() / 4 + radians(v_lat) / 2));
  v_q := (sqrt(3) / 3 * v_x - v_y / 3) / v_s;
  v_rr := (2.0 / 3 * v_y) / v_s;
  -- 큐브 좌표로 반올림
  v_cx := v_q; v_cz := v_rr; v_cy := -v_cx - v_cz;
  v_rx := round(v_cx); v_ry := round(v_cy); v_rz := round(v_cz);
  if abs(v_rx - v_cx) > abs(v_ry - v_cy) and abs(v_rx - v_cx) > abs(v_rz - v_cz) then
    v_rx := -v_ry - v_rz;
  elsif abs(v_ry - v_cy) > abs(v_rz - v_cz) then
    v_ry := -v_rx - v_rz;
  else
    v_rz := -v_rx - v_ry;
  end if;
  return v_rx::bigint || ':' || v_rz::bigint;
end;
$$;

-- 칸 이름 → 칸 가운데 좌표
create or replace function economy.hex_center(p_cell text)
returns table (lat double precision, lng double precision)
language plpgsql immutable as $$
declare
  v_r constant double precision := 6378137.0;
  v_s double precision := economy.territory_hex_size();
  v_q double precision := split_part(p_cell, ':', 1)::double precision;
  v_rr double precision := split_part(p_cell, ':', 2)::double precision;
  v_x double precision;
  v_y double precision;
begin
  v_x := v_s * (sqrt(3) * v_q + sqrt(3) / 2 * v_rr);
  v_y := v_s * (1.5 * v_rr);
  return query select
    degrees(2 * atan(exp(v_y / v_r)) - pi() / 2),
    degrees(v_x / v_r);
end;
$$;

create table if not exists public.territory_marks (
  cell text not null,
  user_id uuid not null references auth.users on delete cascade,
  day bigint not null,
  crew_id uuid not null references public.crews on delete cascade,
  session_id bigint not null references public.walk_sessions on delete cascade,
  lat double precision not null,
  lng double precision not null,
  created_at timestamptz not null default now(),
  primary key (cell, user_id, day)
);

create index if not exists territory_marks_area on public.territory_marks (lat, lng, day);
create index if not exists territory_marks_session on public.territory_marks (session_id);
create index if not exists territory_marks_crew on public.territory_marks (crew_id, day);

comment on table public.territory_marks is
  '땅따먹기 표시. 한 사람이 한 칸에 하루 한 번. 러닝 경로에서 서버가 만든다.';

alter table public.territory_marks enable row level security;
revoke all on public.territory_marks from anon, authenticated;

-- 사람의 대표 크루 — 가장 먼저 가입한 크루. 숨겨진 크루는 빼고.
create or replace function public.home_crew(p_user uuid)
returns uuid
language sql
stable
security definer
set search_path = public
as $$
  select m.crew_id
    from public.crew_members m
   where m.user_id = p_user
     and not public.is_hidden('CREW', m.crew_id::text)
   order by m.joined_at, m.crew_id
   limit 1
$$;

-- 러닝 하나로 칸을 칠한다. 이미 칠한 러닝이면 크루만 맞춘다.
create or replace function public.territory_credit(p_session bigint)
returns int
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_s public.walk_sessions%rowtype;
  v_crew uuid;
  v_day bigint;
  v_chunk text;
  v_parts text[];
  v_cell text;
  v_cells text[] := '{}';
  v_cap int;
  v_n int := 0;
  v_lat double precision;
  v_lng double precision;
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
begin
  select * into v_s from public.walk_sessions where id = p_session;
  if not found or v_s.verdict = 'VOID' or v_s.track = '' then
    return 0;
  end if;

  v_crew := coalesce(v_s.crew_id, public.home_crew(v_s.user_id));
  if v_crew is null then
    return 0;
  end if;

  -- 이미 칠한 러닝이다(크루를 나중에 적은 경우) — 크루만 바꾼다.
  if exists (select 1 from public.territory_marks t where t.session_id = p_session) then
    update public.territory_marks set crew_id = v_crew where session_id = p_session;
    return 0;
  end if;

  v_day := floor(extract(epoch from v_s.started_at) / 86400)::bigint;
  -- 칸 수 상한 — 경로 길이로 지날 수 있는 만큼만. 150m 칸이면 100m 마다 한 칸이 넉넉하다.
  v_cap := greatest(1, (greatest(v_s.gps_distance_m, 0) / 100)::int + 3);

  foreach v_chunk in array string_to_array(v_s.track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) < 2;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num;
    v_lat := v_parts[1]::double precision;
    v_lng := v_parts[2]::double precision;
    continue when v_lat not between -85 and 85 or v_lng not between -180 and 180;
    v_cell := economy.hex_cell(v_lat, v_lng);
    continue when v_cell = any(v_cells);
    exit when coalesce(array_length(v_cells, 1), 0) >= v_cap;
    v_cells := v_cells || v_cell;
  end loop;

  foreach v_cell in array v_cells loop
    insert into public.territory_marks (cell, user_id, day, crew_id, session_id, lat, lng)
    select v_cell, v_s.user_id, v_day, v_crew, p_session, c.lat, c.lng
      from economy.hex_center(v_cell) c
    on conflict (cell, user_id, day) do nothing;
    if found then
      v_n := v_n + 1;
    end if;
  end loop;
  return v_n;
end;
$$;

revoke execute on function public.territory_credit(bigint) from public, anon, authenticated;

create or replace function public.territory_on_session()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.territory_credit(new.id);
  return new;
end;
$$;

drop trigger if exists territory_after_insert on public.walk_sessions;
create trigger territory_after_insert
  after insert on public.walk_sessions
  for each row execute function public.territory_on_session();

drop trigger if exists territory_after_crew on public.walk_sessions;
create trigger territory_after_crew
  after update of crew_id on public.walk_sessions
  for each row when (new.crew_id is distinct from old.crew_id)
  execute function public.territory_on_session();

-- 지도에 보이는 칸과 주인. 한 번에 너무 넓은 곳은 받지 않는다(약 30km).
create or replace function public.territory_view(
  p_min_lat double precision,
  p_min_lng double precision,
  p_max_lat double precision,
  p_max_lng double precision
)
returns table (
  cell text,
  lat double precision,
  lng double precision,
  crew_id uuid,
  crew_name text,
  score int,
  mine boolean
)
language plpgsql
stable
security definer
set search_path = public, economy
as $$
declare
  v_since bigint := floor(extract(epoch from now()) / 86400)::bigint - economy.territory_window_days();
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_max_lat - p_min_lat > 0.3 or p_max_lng - p_min_lng > 0.3
     or p_max_lat < p_min_lat or p_max_lng < p_min_lng then
    raise exception '지도를 더 확대해 주세요' using errcode = '22023';
  end if;

  return query
    with counts as (
      select t.cell, t.crew_id, count(*)::int as n, max(t.day) as last_day,
             min(t.lat) as lat, min(t.lng) as lng
        from public.territory_marks t
       where t.day >= v_since
         and t.lat between p_min_lat - 0.002 and p_max_lat + 0.002
         and t.lng between p_min_lng - 0.002 and p_max_lng + 0.002
         and not public.is_hidden('CREW', t.crew_id::text)
       group by t.cell, t.crew_id
    ),
    owner as (
      select distinct on (k.cell) k.*
        from counts k
       order by k.cell, k.n desc, k.last_day desc, k.crew_id
    )
    select o.cell, o.lat, o.lng, o.crew_id, c.name, o.n,
           exists (select 1 from public.crew_members m
                    where m.crew_id = o.crew_id and m.user_id = auth.uid())
      from owner o
      join public.crews c on c.id = o.crew_id
     limit 3000;
end;
$$;

-- 크루별 차지한 칸 수. 최근 14일 기준.
create or replace function public.territory_board(p_limit int default 20)
returns table (
  crew_id uuid,
  crew_name text,
  cells int,
  mine boolean
)
language sql
stable
security definer
set search_path = public, economy
as $$
  with counts as (
    select t.cell, t.crew_id, count(*) as n, max(t.day) as last_day
      from public.territory_marks t
     where t.day >= floor(extract(epoch from now()) / 86400)::bigint - economy.territory_window_days()
       and not public.is_hidden('CREW', t.crew_id::text)
     group by t.cell, t.crew_id
  ),
  owner as (
    select distinct on (k.cell) k.cell, k.crew_id
      from counts k
     order by k.cell, k.n desc, k.last_day desc, k.crew_id
  )
  select o.crew_id, c.name, count(*)::int,
         exists (select 1 from public.crew_members m
                  where m.crew_id = o.crew_id and m.user_id = auth.uid())
    from owner o
    join public.crews c on c.id = o.crew_id
   group by o.crew_id, c.name
   order by 3 desc, o.crew_id
   limit greatest(1, least(coalesce(p_limit, 20), 100))
$$;

revoke execute on function public.home_crew(uuid) from public, anon, authenticated;
revoke execute on function public.territory_view(double precision, double precision, double precision, double precision) from public, anon;
revoke execute on function public.territory_board(int) from public, anon;
grant execute on function public.territory_view(double precision, double precision, double precision, double precision) to authenticated;
grant execute on function public.territory_board(int) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0021_account_delete.sql
-- ══════════════════════════════════════════════════════════════════

-- 계정 삭제 — 앱 안에서 본인이 계정과 서버의 기록을 지운다.
--
-- Play 스토어는 계정을 만들 수 있는 앱에 "앱 안에서 계정 삭제"를 요구한다. 지우면
-- auth.users 한 줄이 사라지고, 그 사람을 가리키는 표(프로필·러닝·원장·글·댓글·
-- 코스·땅 표시·푸시 토큰…)는 전부 on delete cascade 로 함께 지워진다. 거래 기록처럼
-- 상대가 있는 줄은 on delete set null 로 상대 쪽 기록만 남는다.
--
-- 크루장이 지우면 크루가 통째로 사라지는 대신, 가장 오래된 크루원에게 크루장을
-- 넘긴다. 크루원이 없으면 크루도 함께 지워진다.

create or replace function public.account_delete()
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_user uuid := auth.uid();
  r record;
  v_next uuid;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  for r in select c.id from public.crews c where c.owner_id = v_user loop
    select m.user_id into v_next
      from public.crew_members m
     where m.crew_id = r.id and m.user_id <> v_user
     order by m.joined_at, m.user_id
     limit 1;
    if v_next is not null then
      update public.crews set owner_id = v_next where id = r.id;
      update public.crew_members set role = 'OWNER' where crew_id = r.id and user_id = v_next;
    end if;
  end loop;

  delete from auth.users where id = v_user;
end;
$$;

comment on function public.account_delete is
  '본인 계정과 서버의 기록을 지운다. 크루장이면 가장 오래된 크루원에게 넘긴다.';

revoke execute on function public.account_delete() from public, anon;
grant execute on function public.account_delete() to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0022_economy_core.sql
-- ══════════════════════════════════════════════════════════════════

-- 서버가 경제의 정본이 된다 (A안) — 잔고 · 신발 · 에너지 · 설정.
--
-- 지금까지 SUP 와 신발은 폰이 먼저 정했다. 폰 값을 체인으로 꺼낼 수 있게 되면
-- 폰을 고친 사람이 진짜 토큰과 NFT 를 만든다. 그래서 이 파일부터는 **체인으로
-- 나갈 수 있는 모든 가치를 서버가 정한다.** 폰은 요청만 보내고 화면을 그린다.
--
-- 이 파일은 더하기만 한다. 지금 깔려 있는 앱이 부르는 함수는 그대로 둔다 —
-- main 에 합치면 바로 배포되므로, 앱이 바뀌기 전에 옛 앱이 깨지면 안 된다.
--
--   economy_settings     운영 값(상한 · 정지 스위치). 다시 배포하지 않고 바꾼다
--   economy.ledger_apply 잔고를 바꾸는 유일한 길. 사용자별로 줄을 세운다
--   market_sneakers      서버 신발 표. 이름은 그대로 두고 스탯·체인 상태를 더한다
--   energy_days          하루 에너지. 폰이 아니라 서버가 센다
--   boosts               부스터(XP ×2 · 스트릭 보호 · 에너지 셀) 기록

-- ══════════════════════════════════════════════════════════════════
-- 게임의 하루 — 한국 시간
-- ══════════════════════════════════════════════════════════════════
--
-- 에너지 리필, 하루 상한, 목표 보너스가 모두 이 하루를 쓴다. 저장은 시각으로
-- 하고 날짜는 읽을 때 계산하므로, 예전 UTC 기준에서 바꿔도 상한이 두 번 열리지
-- 않는다(같은 시각이 두 하루에 동시에 들어가지 않는다).
create or replace function economy.game_day(p_at timestamptz) returns date
  language sql immutable as $$ select (p_at at time zone 'Asia/Seoul')::date $$;

create or replace function economy.game_day_start(p_day date) returns timestamptz
  language sql immutable as $$ select p_day::timestamp at time zone 'Asia/Seoul' $$;

-- ══════════════════════════════════════════════════════════════════
-- 운영 값
-- ══════════════════════════════════════════════════════════════════
--
-- 상한과 정지 스위치는 사고가 났을 때 **바로** 바꿔야 한다. 함수 상수로 두면
-- 바꾸려고 배포를 기다려야 한다. 그래서 표에 둔다. 앱 권한으로는 읽지도 쓰지도
-- 못한다 — 관리자 함수와 서버 함수만 본다.
create table if not exists public.economy_settings (
  key text primary key,
  value jsonb not null,
  updated_at timestamptz not null default now()
);

comment on table public.economy_settings is
  '운영 값(상한·정지 스위치·시작 시각). 앱 권한으로는 못 본다. admin_economy_set 으로 바꾼다.';

alter table public.economy_settings enable row level security;
revoke all on public.economy_settings from anon, authenticated;

-- 처음 설치할 때만 넣는다. 다시 붙여넣어도 운영 중에 바꾼 값을 덮지 않는다.
insert into public.economy_settings (key, value) values
  -- 이 시각 뒤에 가입한 사람만 무료 뽑기 10회를 받는다. 처음 배포한 순간으로 굳는다.
  ('free_draw_since',            to_jsonb(now())),
  -- 사람별 하루 적립 상한(SUP). 걸음 상한(48,000)과 별도로 금액에도 선을 긋는다.
  ('daily_earn_cap',             '600'::jsonb),
  -- 가입 며칠까지를 신규로 보고 적립 상한을 절반으로 할지
  ('new_account_days',           '7'::jsonb),
  -- 꺼내기(체인) 상한과 조건
  ('withdraw_user_daily_sup',    '1000'::jsonb),
  ('withdraw_global_daily_sup',  '50000'::jsonb),
  ('mint_global_daily',          '500'::jsonb),
  ('withdraw_min_account_days',  '7'::jsonb),
  ('withdraw_min_km',            '20'::jsonb),
  ('wallet_cooldown_hours',      '72'::jsonb),
  ('free_shoe_lock_km',          '50'::jsonb),
  -- 서명 유효 시간(초)과, 만료 뒤 되돌리기 전에 더 기다리는 시간(초)
  ('op_deadline_sec',            '600'::jsonb),
  ('op_expire_margin_sec',       '1800'::jsonb),
  -- 정지 스위치. true 면 꺼내기·넣기·보너스 발행을 모두 멈춘다.
  ('chain_paused',               'false'::jsonb)
on conflict (key) do nothing;

create or replace function economy.setting(p_key text) returns jsonb
  language sql stable security definer set search_path = public as $$
  select value from public.economy_settings where key = p_key
$$;

create or replace function economy.setting_num(p_key text) returns numeric
  language sql stable security definer set search_path = public as $$
  select (value #>> '{}')::numeric from public.economy_settings where key = p_key
$$;

revoke all on function economy.setting(text) from public;
revoke all on function economy.setting_num(text) from public;

-- 관리자만 바꾼다. 누가 언제 무엇을 바꿨는지 admin_audit 에 남는다.
create or replace function public.admin_economy_set(p_key text, p_value jsonb)
returns void
language plpgsql security definer set search_path = public as $$
begin
  perform public.admin_guard();
  if not exists (select 1 from public.economy_settings where key = p_key) then
    raise exception '없는 설정입니다: %', p_key using errcode = '22023';
  end if;
  update public.economy_settings set value = p_value, updated_at = now() where key = p_key;
  perform public.admin_log('economy_set', p_key, jsonb_build_object('value', p_value));
end $$;

revoke all on function public.admin_economy_set(text, jsonb) from public, anon;
grant execute on function public.admin_economy_set(text, jsonb) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 원장 — 종류를 더하고, 잔고를 바꾸는 길을 하나로 모은다
-- ══════════════════════════════════════════════════════════════════
--
--   EARN_COURSE     코스 완주 보상
--   SPEND_DRAW      뽑기
--   SPEND_REPAIR    신발 수리 (태운다 — 누구에게도 가지 않는다)
--   CHAIN_WITHDRAW  체인으로 꺼냄 (음수)
--   CHAIN_REFUND    꺼내기가 체인에 안 올라가 만료됨 → 되돌림 (양수)
--   CHAIN_DEPOSIT   체인에서 넣음 (양수)
alter table public.sup_ledger drop constraint if exists sup_ledger_kind_check;
alter table public.sup_ledger add constraint sup_ledger_kind_check check (kind in (
  'EARN_WALK', 'EARN_PARTY', 'EARN_EVENT', 'BONUS_GOAL', 'EARN_COURSE',
  'SPEND_MINT', 'SPEND_UPGRADE', 'SPEND_BOOST', 'SPEND_DRAW', 'SPEND_REPAIR',
  'ESCROW_LOCK', 'ESCROW_UNLOCK', 'TRADE_BUY', 'TRADE_SELL', 'TRADE_FEE',
  'CHAIN_WITHDRAW', 'CHAIN_REFUND', 'CHAIN_DEPOSIT'
));

-- 같은 요청이 두 번 들어와도 한 번만 적히게 하는 꼬리표.
-- 체인 작업 번호 · 목표 보너스의 날짜 같은 것이 들어간다.
alter table public.sup_ledger add column if not exists ref text;
create unique index if not exists sup_ledger_ref_once
  on public.sup_ledger (user_id, ref) where ref is not null;

/*
 * 잔고를 바꾸는 유일한 길.
 *
 * 새로 만드는 함수는 모두 이것을 부른다. 잔고 검사와 기록 사이에 다른 요청이
 * 끼어들지 못하게 이 사람의 원장 잠금을 먼저 잡는다 — record_session ·
 * spend_sup · 거래소가 잡는 것과 같은 잠금이라 모두 한 줄로 선다.
 *
 * 앱 권한으로는 부를 수 없다. economy 스키마는 앱에 열려 있지 않고, 함수 권한도 거둔다.
 */
create or replace function economy.ledger_apply(
  p_user uuid,
  p_kind text,
  p_amount numeric,
  p_description text,
  p_ref text default null
) returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_balance numeric(20, 4);
begin
  if p_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_amount is null or p_amount = 0 then
    raise exception '금액이 올바르지 않습니다' using errcode = '22023';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || p_user::text));

  if p_ref is not null and exists (
       select 1 from public.sup_ledger where user_id = p_user and ref = p_ref) then
    raise exception '이미 처리된 요청입니다' using errcode = '23505';
  end if;

  select coalesce(sum(amount), 0) into v_balance
    from public.sup_ledger where user_id = p_user;

  if p_amount < 0 and v_balance + p_amount < 0 then
    raise exception 'SUP가 부족합니다 (보유 %, 필요 %)', v_balance, -p_amount
      using errcode = '23514';
  end if;

  insert into public.sup_ledger (user_id, kind, amount, description, ref)
  values (p_user, p_kind, round(p_amount, 4), coalesce(p_description, ''), p_ref);

  return v_balance + round(p_amount, 4);
end $$;

revoke all on function economy.ledger_apply(uuid, text, numeric, text, text) from public;

create or replace function economy.balance_of(p_user uuid) returns numeric
  language sql stable security definer set search_path = public as $$
  select coalesce(sum(amount), 0)::numeric(20, 4) from public.sup_ledger where user_id = p_user
$$;
revoke all on function economy.balance_of(uuid) from public;

-- ══════════════════════════════════════════════════════════════════
-- 신발 규칙 — 등급 · 레벨 · 효율성 · 착화감 · 내구도
-- ══════════════════════════════════════════════════════════════════
--
-- 행운과 속성은 스탯에서 뺐다. 속성(불·물·번개·바람)은 모델 그림의 주제로만 남는다.
--
--            확률   최대Lv  효율성(Lv1)    착화감(Lv1)   내구도 -/km  수리계수
--   COMMON    55%     10    +2 ~  4%       0 ~  3%        1.0        1.00
--   RARE      28%     15    +4 ~  7%       3 ~  6%        0.9        1.25
--   EPIC      13%     20    +7 ~ 10%       6 ~  9%        0.8        1.50
--   LEGENDARY  4%     30    +10 ~ 13%      9 ~ 12%        0.7        1.75
--
-- 레벨 한 칸: 효율성 +0.5%p, 착화감 +0.2%p(최대 20%), 에너지 최대 +2칸.
-- 내구도 50 이상 정상 · 20~49 적립 -30% · 20 미만 적립 없음.
-- 값은 bps(1% = 100)로 적는다. 소수로 적으면 더할 때 오차가 쌓인다.

create or replace function economy.rarity_ord(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 0 when 'RARE' then 1
                       when 'EPIC' then 2 when 'LEGENDARY' then 3 end
$$;

create or replace function economy.rarity_weight(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 55 when 'RARE' then 28
                       when 'EPIC' then 13 when 'LEGENDARY' then 4 end
$$;

create or replace function economy.max_level(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 10 when 'RARE' then 15
                       when 'EPIC' then 20 when 'LEGENDARY' then 30 end
$$;

-- 등급 안 모델 수. 앱 도감(domain/Sneaker.kt)과 같다 — 속성마다 13종.
create or replace function economy.variant_count(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 4 when 'RARE' then 4
                       when 'EPIC' then 3 when 'LEGENDARY' then 2 end
$$;

create or replace function economy.efficiency_range(p_rarity text, out lo int, out hi int)
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 200 when 'RARE' then 400
                       when 'EPIC' then 700 else 1000 end,
         case p_rarity when 'COMMON' then 400 when 'RARE' then 700
                       when 'EPIC' then 1000 else 1300 end
$$;

create or replace function economy.comfort_range(p_rarity text, out lo int, out hi int)
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 0 when 'RARE' then 300
                       when 'EPIC' then 600 else 900 end,
         case p_rarity when 'COMMON' then 300 when 'RARE' then 600
                       when 'EPIC' then 900 else 1200 end
$$;

create or replace function economy.efficiency_per_level_bps() returns int
  language sql immutable as $$ select 50 $$;
create or replace function economy.comfort_per_level_bps() returns int
  language sql immutable as $$ select 20 $$;
create or replace function economy.comfort_cap_bps() returns int
  language sql immutable as $$ select 2000 $$;

create or replace function economy.durability_loss_per_km(p_rarity text) returns numeric
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 1.0 when 'RARE' then 0.9
                       when 'EPIC' then 0.8 else 0.7 end::numeric
$$;

-- 내구도에 따른 적립 배율
create or replace function economy.durability_factor(p_durability numeric) returns numeric
  language sql immutable as $$
  select case when p_durability >= 50 then 1.0
              when p_durability >= 20 then 0.7
              else 0 end::numeric
$$;

-- 수리 1칸 값. 좋은 신발일수록 더 벌고 더 든다.
create or replace function economy.repair_cost_per_point(p_rarity text, p_level int) returns numeric
  language sql immutable as $$
  select round((1 + 0.25 * economy.rarity_ord(p_rarity)) * (1 + 0.05 * p_level), 4)
$$;

-- 강화 값 — 앱의 RewardEconomy.sneakerUpgradeCost 와 같다
create or replace function economy.upgrade_cost(p_rarity text, p_level int) returns numeric
  language sql immutable as $$
  select round(p_level * 100.0 * (1 + 0.25 * economy.rarity_ord(p_rarity)))
$$;

create or replace function economy.draw_cost() returns numeric
  language sql immutable as $$ select 500::numeric $$;

create or replace function economy.energy_max_for_level(p_level int) returns numeric
  language sql immutable as $$
  select economy.base_max_energy() + greatest(coalesce(p_level, 1) - 1, 0) * 2
$$;

-- ══════════════════════════════════════════════════════════════════
-- 서버 신발 표
-- ══════════════════════════════════════════════════════════════════
--
-- 거래소 장부(market_sneakers)를 그대로 신발의 정본으로 쓴다. 이미 소유자·
-- 등급·레벨을 들고 있고 거래소 함수가 이 표를 본다. 이름을 바꾸면 옛 앱이 부르는
-- 함수가 깨지므로 이름은 두고 칸을 더한다.
--
-- origin
--   IMPORT      폰에 있던 신발을 올린 것. 폰이 정한 값이라 **믿지 않는다** —
--               적립 계산에서는 일반 1레벨로 치고, 체인으로 꺼낼 수 없다.
--   STARTER     가입 때 주는 첫 신발. 꺼낼 수 없다.
--   FREE_DRAW   신규 무료 뽑기. 이 신발로 50km 를 달려야 꺼낼 수 있다.
--   PAID_DRAW   SUP 로 뽑은 신발
--   BONUS_DRAW  지갑 연결 보너스(체인에서 발행). 50km 전에는 전송이 잠긴다.
--   DEPOSIT     체인에서 처음 넣은 신발(남에게 산 NFT 등)
--   MINT        예전 표기. 쓰지 않는다.
alter table public.market_sneakers
  add column if not exists efficiency_bps int not null default 200,
  add column if not exists comfort_bps int not null default 0,
  add column if not exists durability_pts numeric(6, 2) not null default 100,
  add column if not exists equipped boolean not null default false,
  add column if not exists chain_state text not null default 'APP',
  add column if not exists token_id numeric(78, 0),
  add column if not exists genesis_no int,
  add column if not exists km_run numeric(10, 3) not null default 0,
  add column if not exists lock_km numeric(10, 3) not null default 0,
  add column if not exists withdrawable boolean not null default false,
  add column if not exists draw_nonce bigint,
  add column if not exists updated_at timestamptz not null default now();

alter table public.market_sneakers drop constraint if exists market_sneakers_origin_check;
alter table public.market_sneakers add constraint market_sneakers_origin_check check (origin in (
  'IMPORT', 'MINT', 'STARTER', 'FREE_DRAW', 'PAID_DRAW', 'BONUS_DRAW', 'DEPOSIT'
));

alter table public.market_sneakers drop constraint if exists market_sneakers_chain_state_check;
alter table public.market_sneakers add constraint market_sneakers_chain_state_check check (
  chain_state in ('APP', 'WITHDRAWING', 'ON_CHAIN', 'DEPOSITING')
);

alter table public.market_sneakers drop constraint if exists market_sneakers_stats_check;
alter table public.market_sneakers add constraint market_sneakers_stats_check check (
  efficiency_bps between 0 and 5000
  and comfort_bps between 0 and 2000
  and durability_pts between 0 and 100
  and km_run >= 0 and lock_km >= 0
);

-- 계정을 지워도 체인에 나가 있는 신발의 기록은 남아야 한다. 나중에 산 사람이 넣을
-- 때 그 신발을 찾아야 하기 때문이다. 주인만 비운다.
alter table public.market_sneakers alter column owner_id drop not null;
alter table public.market_sneakers drop constraint if exists market_sneakers_owner_id_fkey;
alter table public.market_sneakers add constraint market_sneakers_owner_id_fkey
  foreign key (owner_id) references auth.users (id) on delete set null;

create unique index if not exists market_sneakers_token_once
  on public.market_sneakers (token_id) where token_id is not null;
create unique index if not exists market_sneakers_genesis_once
  on public.market_sneakers (genesis_no) where genesis_no is not null;
-- 한 사람이 동시에 신는 신발은 하나
create unique index if not exists market_sneakers_one_equipped
  on public.market_sneakers (owner_id) where equipped;

create sequence if not exists public.genesis_seq start 1;
revoke all on sequence public.genesis_seq from anon, authenticated;

comment on table public.market_sneakers is
  '서버 신발 표 — 신발의 정본. 폰 목록은 사본이다. IMPORT 는 폰이 정한 값이라 적립·꺼내기에 쓰지 않는다.';

-- 적립 계산에 쓰는 실효 스탯. 폰이 정한 신발(IMPORT·MINT)은 일반 1레벨로 친다.
create or replace function economy.sneaker_effective(
  p_origin text, p_rarity text, p_level int,
  p_efficiency_bps int, p_comfort_bps int, p_durability numeric,
  out efficiency_bps int, out comfort_bps int, out level int, out durability numeric
) language sql immutable as $$
  select
    case when p_origin in ('IMPORT', 'MINT') then 200
         else p_efficiency_bps + economy.efficiency_per_level_bps() * greatest(p_level - 1, 0) end,
    case when p_origin in ('IMPORT', 'MINT') then 0
         else least(p_comfort_bps + economy.comfort_per_level_bps() * greatest(p_level - 1, 0),
                    economy.comfort_cap_bps()) end,
    case when p_origin in ('IMPORT', 'MINT') then 1 else p_level end,
    case when p_origin in ('IMPORT', 'MINT') then 100 else p_durability end
$$;

-- 지금 신고 있는 신발(서버 기준). 없거나 체인에 나가 있으면 null.
create or replace function economy.equipped_sneaker(p_user uuid)
returns setof public.market_sneakers
language sql stable security definer set search_path = public as $$
  select * from public.market_sneakers
   where owner_id = p_user and equipped and chain_state = 'APP' and status = 'OWNED'
   limit 1
$$;
revoke all on function economy.equipped_sneaker(uuid) from public;

-- 판매 중이거나 체인에 나가 있는 신발은 신을 수도, 강화할 수도, 다시 팔 수도
-- 없다(결정표 X7). 거래소 함수를 고치지 않고 표에서 막는다 — 새 매물을 거는
-- 순간 신발 상태를 본다.
create or replace function economy.guard_listing() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if exists (select 1 from public.market_sneakers
              where id = new.sneaker_id and (chain_state <> 'APP' or equipped)) then
    raise exception '착용 중이거나 체인에 있는 신발은 팔 수 없습니다' using errcode = '22023';
  end if;
  return new;
end $$;

drop trigger if exists market_listings_guard on public.market_listings;
create trigger market_listings_guard before insert on public.market_listings
  for each row execute function economy.guard_listing();

-- ══════════════════════════════════════════════════════════════════
-- 에너지 — 하루(한국 시간)마다 최대치로 찬다
-- ══════════════════════════════════════════════════════════════════
create table if not exists public.energy_days (
  user_id uuid not null references auth.users on delete cascade,
  day date not null,
  -- 쓴 에너지(칸)
  used numeric(10, 4) not null default 0 check (used >= 0),
  -- 에너지 셀로 더 받은 칸
  bonus numeric(10, 4) not null default 0 check (bonus >= 0),
  primary key (user_id, day)
);

comment on table public.energy_days is
  '하루 에너지 사용량. 최대치는 신은 신발 레벨로 정해지고 한국 자정에 다시 찬다.';

alter table public.energy_days enable row level security;
revoke all on public.energy_days from anon, authenticated;
drop policy if exists energy_days_select_own on public.energy_days;
create policy energy_days_select_own on public.energy_days
  for select using ((select auth.uid()) = user_id);
grant select on public.energy_days to authenticated;

create or replace function economy.energy_max(p_user uuid) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select economy.energy_max_for_level(coalesce((
    select e.level from public.market_sneakers s,
           lateral economy.sneaker_effective(s.origin, s.rarity, s.level,
             s.efficiency_bps, s.comfort_bps, s.durability_pts) e
     where s.owner_id = p_user and s.equipped and s.chain_state = 'APP' and s.status = 'OWNED'
     limit 1), 1))
$$;
revoke all on function economy.energy_max(uuid) from public;

-- 남은 에너지(칸)
create or replace function economy.energy_left(p_user uuid, p_day date) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select greatest(economy.energy_max(p_user)
                  + coalesce((select bonus - used from public.energy_days
                               where user_id = p_user and day = p_day), 0), 0)
$$;
revoke all on function economy.energy_left(uuid, date) from public;

-- ══════════════════════════════════════════════════════════════════
-- 부스터 — 앱의 BoostType 과 같은 값
-- ══════════════════════════════════════════════════════════════════
--   ENERGY_CELL    50 SUP   즉시 에너지 2칸
--   STREAK_SHIELD 120 SUP   24시간 동안 목표를 못 채워도 연속 기록 유지
--   XP_BOOSTER    200 SUP   24시간 동안 러닝 적립 ×2
create table if not exists public.boosts (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,
  kind text not null check (kind in ('ENERGY_CELL', 'STREAK_SHIELD', 'XP_BOOSTER')),
  starts_at timestamptz not null default now(),
  ends_at timestamptz not null default now(),
  cost numeric(20, 4) not null check (cost >= 0),
  created_at timestamptz not null default now()
);

create index if not exists boosts_user_active on public.boosts (user_id, ends_at desc);

alter table public.boosts enable row level security;
revoke all on public.boosts from anon, authenticated;
drop policy if exists boosts_select_own on public.boosts;
create policy boosts_select_own on public.boosts
  for select using ((select auth.uid()) = user_id);
grant select on public.boosts to authenticated;

create or replace function economy.boost_cost(p_kind text) returns numeric
  language sql immutable as $$
  select case p_kind when 'ENERGY_CELL' then 50 when 'STREAK_SHIELD' then 120
                     when 'XP_BOOSTER' then 200 end::numeric
$$;

create or replace function economy.boost_active(p_user uuid, p_kind text, p_at timestamptz)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.boosts
                  where user_id = p_user and kind = p_kind
                    and starts_at <= p_at and ends_at > p_at)
$$;
revoke all on function economy.boost_active(uuid, text, timestamptz) from public;

create or replace function public.boost_buy(p_kind text)
returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_cost numeric := economy.boost_cost(p_kind);
  v_day date := economy.game_day(now());
  v_balance numeric;
  v_from timestamptz;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if v_cost is null then
    raise exception '없는 부스터입니다' using errcode = '22023';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  if p_kind = 'ENERGY_CELL' then
    -- 가득 찬데 사면 돈만 사라진다(결정표 X9). 2칸이 들어갈 자리가 있어야 판다.
    if economy.energy_left(v_user, v_day) + 2 > economy.energy_max(v_user) then
      raise exception '에너지가 이미 충분합니다' using errcode = '23514';
    end if;
  end if;

  v_balance := economy.ledger_apply(v_user, 'SPEND_BOOST', -v_cost, '부스터: ' || p_kind);

  if p_kind = 'ENERGY_CELL' then
    insert into public.energy_days (user_id, day, bonus) values (v_user, v_day, 2)
    on conflict (user_id, day) do update set bonus = public.energy_days.bonus + 2;
    insert into public.boosts (user_id, kind, cost) values (v_user, p_kind, v_cost);
  else
    -- 이미 켜져 있으면 끝나는 시각 뒤로 이어 붙인다. 겹쳐 사도 손해가 없게.
    select greatest(now(), coalesce(max(ends_at), now())) into v_from
      from public.boosts where user_id = v_user and kind = p_kind and ends_at > now();
    insert into public.boosts (user_id, kind, starts_at, ends_at, cost)
    values (v_user, p_kind, v_from, v_from + interval '24 hours', v_cost);
  end if;

  return v_balance;
end $$;

revoke all on function public.boost_buy(text) from public, anon;
grant execute on function public.boost_buy(text) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 직접 쓰기 막기 (결정표 X4)
-- ══════════════════════════════════════════════════════════════════
--
-- 게시글·크루는 post_create · crew_create 가 도배 제한과 크루 10개 상한을
-- 검사한다. 표에 직접 INSERT 할 수 있으면 그 검사를 건너뛴다. 앱은 이미 두
-- 함수만 부르므로 직접 쓰기를 닫아도 바뀌는 것이 없다.
drop policy if exists posts_insert_own on public.posts;
drop policy if exists crews_insert_own on public.crews;
revoke insert on public.posts from anon, authenticated;
revoke insert on public.crews from anon, authenticated;

-- 거래소 표도 함수로만 바뀐다. 지금은 RLS 정책이 없어 막혀 있지만, 누가 정책을
-- 하나 잘못 붙이는 순간 열린다. 잔고와 소유권이 걸린 표라 권한부터 거둔다.
revoke insert, update, delete on public.market_sneakers, public.market_imports,
  public.market_listings, public.market_bids, public.market_trades from anon, authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 내 경제 한눈에 — 앱이 화면을 그릴 때 읽는다
-- ══════════════════════════════════════════════════════════════════
create or replace function public.my_economy()
returns table (
  balance numeric,
  energy_max numeric,
  energy_left numeric,
  xp_booster_until timestamptz,
  streak_shield_until timestamptz,
  equipped_id bigint,
  game_day date
)
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_day date := economy.game_day(now());
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return query select
    economy.balance_of(v_user),
    economy.energy_max(v_user),
    economy.energy_left(v_user, v_day),
    (select max(b.ends_at) from public.boosts b
      where b.user_id = v_user and b.kind = 'XP_BOOSTER' and b.ends_at > now()),
    (select max(b.ends_at) from public.boosts b
      where b.user_id = v_user and b.kind = 'STREAK_SHIELD' and b.ends_at > now()),
    (select s.id from economy.equipped_sneaker(v_user) s),
    v_day;
end $$;

revoke all on function public.my_economy() from public, anon;
grant execute on function public.my_economy() to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 0023_sneaker_ops.sql
-- ══════════════════════════════════════════════════════════════════

-- 신발 — 뽑기 · 강화 · 수리 · 착용을 서버가 한다.
--
-- 폰이 난수를 굴리면 원하는 결과가 나올 때까지 다시 굴릴 수 있다. 서버가 굴리고,
-- 결과를 미리 봉해 둔 씨앗(seed)에서 뽑아 나중에 누구든 확인할 수 있게 한다.
--
--   뽑기 공정성: 사람마다 비밀 씨앗이 있고, 그 해시(seed_hash)는 미리 공개된다.
--   n 번째 뽑기의 결과는 sha256(씨앗 || ':' || n) 에서 정해진다. 씨앗을 바꿀 때
--   옛 씨앗을 공개하므로, 공개된 해시와 맞는지·결과가 맞는지 누구나 다시 계산할
--   수 있다. 서버도 결과를 고를 수 없다 — 해시를 먼저 공개했기 때문이다.
--
--   다시 뽑기 막기: 값을 치르는 것과 결과를 정하는 것이 한 트랜잭션이다. 결과를
--   보고 값을 안 치르는 길이 없다.

-- ══════════════════════════════════════════════════════════════════
-- 씨앗
-- ══════════════════════════════════════════════════════════════════
create table if not exists public.draw_seeds (
  user_id uuid primary key references auth.users on delete cascade,
  seed bytea not null,
  seed_hash text not null,
  -- 이 씨앗으로 다음에 뽑을 번호
  nonce bigint not null default 0,
  first_nonce bigint not null default 0,
  created_at timestamptz not null default now()
);

comment on table public.draw_seeds is
  '뽑기 씨앗. 씨앗 자체는 아무도 못 읽는다(앱 권한 없음). 해시만 draw_fairness 로 공개된다.';

alter table public.draw_seeds enable row level security;
revoke all on public.draw_seeds from anon, authenticated;

create table if not exists public.draw_seed_reveals (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,
  seed_hex text not null,
  seed_hash text not null,
  first_nonce bigint not null,
  last_nonce bigint not null,
  revealed_at timestamptz not null default now()
);

alter table public.draw_seed_reveals enable row level security;
revoke all on public.draw_seed_reveals from anon, authenticated;
drop policy if exists draw_seed_reveals_select_own on public.draw_seed_reveals;
create policy draw_seed_reveals_select_own on public.draw_seed_reveals
  for select using ((select auth.uid()) = user_id);
grant select on public.draw_seed_reveals to authenticated;

-- 강한 난수 32바이트. gen_random_uuid 는 운영체제의 강한 난수로 만든다.
create or replace function economy.random_bytes32() returns bytea
  language sql volatile as $$
  select decode(replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', ''), 'hex')
$$;

create or replace function economy.seed_ensure(p_user uuid) returns void
language plpgsql security definer set search_path = public, economy as $$
declare v_seed bytea;
begin
  if exists (select 1 from public.draw_seeds where user_id = p_user) then
    return;
  end if;
  v_seed := economy.random_bytes32();
  insert into public.draw_seeds (user_id, seed, seed_hash)
  values (p_user, v_seed, encode(sha256(v_seed), 'hex'))
  on conflict (user_id) do nothing;
end $$;
revoke all on function economy.seed_ensure(uuid) from public;

-- 다음 뽑기의 32바이트. 번호를 하나 올린다. 부르는 쪽이 이 사람의 잠금을 잡고 있어야 한다.
create or replace function economy.draw_digest(p_user uuid, out o_nonce bigint, out o_digest bytea)
language plpgsql security definer set search_path = public, economy as $$
begin
  perform economy.seed_ensure(p_user);
  update public.draw_seeds d
     set nonce = d.nonce + 1
   where d.user_id = p_user
  returning d.nonce - 1, sha256(d.seed || convert_to(':' || (d.nonce - 1)::text, 'UTF8'))
    into o_nonce, o_digest;
end $$;
revoke all on function economy.draw_digest(uuid) from public;

-- 바이트 몇 개를 음이 아닌 정수로
create or replace function economy.bytes_int(p_b bytea, p_off int, p_len int) returns bigint
  language plpgsql immutable as $$
declare v bigint := 0; i int;
begin
  for i in 0 .. p_len - 1 loop
    v := v * 256 + get_byte(p_b, p_off + i);
  end loop;
  return v;
end $$;

-- 씨앗 결과 → 등급. p_min 이 있으면 그 등급 이상만(예: Genesis 는 EPIC 이상).
-- 가중치 표 안에서 굴리므로 제한이 있어도 위 등급끼리의 비율(13:4)은 그대로다.
create or replace function economy.roll_rarity(p_roll bigint, p_min text default null) returns text
  language plpgsql immutable as $$
declare
  v_order text[] := array['COMMON', 'RARE', 'EPIC', 'LEGENDARY'];
  v_from int := coalesce(economy.rarity_ord(p_min), 0) + 1;
  v_total int := 0;
  v_pick int;
  i int;
begin
  for i in v_from .. 4 loop
    v_total := v_total + economy.rarity_weight(v_order[i]);
  end loop;
  v_pick := (p_roll % v_total)::int;
  for i in v_from .. 4 loop
    v_pick := v_pick - economy.rarity_weight(v_order[i]);
    if v_pick < 0 then
      return v_order[i];
    end if;
  end loop;
  return v_order[4];
end $$;

/*
 * 뽑은 결과로 신발을 만든다. 값은 부르는 쪽이 이미 치렀다.
 *
 *   FREE_DRAW   이 신발로 50km 를 달려야 꺼낼 수 있다(무료 농사 막기)
 *   PAID_DRAW   바로 꺼낼 수 있다
 *   BONUS_DRAW  체인에서 발행된다. 50km 전에는 전송 잠금
 *   STARTER     꺼낼 수 없다
 */
create or replace function economy.draw_create(
  p_user uuid,
  p_origin text,
  p_min_rarity text default null,
  p_genesis boolean default false
) returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_nonce bigint;
  v_d bytea;
  v_rarity text;
  v_faction text;
  v_variant int;
  v_eff_lo int; v_eff_hi int;
  v_cmf_lo int; v_cmf_hi int;
  v_id bigint;
  v_lock numeric := 0;
begin
  select n.o_nonce, n.o_digest into v_nonce, v_d from economy.draw_digest(p_user) n;

  v_rarity := economy.roll_rarity(economy.bytes_int(v_d, 0, 4), p_min_rarity);
  v_faction := (array['FIRE', 'WATER', 'LIGHTNING', 'WIND'])[economy.bytes_int(v_d, 4, 2) % 4 + 1];
  v_variant := (economy.bytes_int(v_d, 6, 2) % economy.variant_count(v_rarity))::int;
  select lo, hi into v_eff_lo, v_eff_hi from economy.efficiency_range(v_rarity);
  select lo, hi into v_cmf_lo, v_cmf_hi from economy.comfort_range(v_rarity);

  if p_origin in ('FREE_DRAW', 'BONUS_DRAW') then
    v_lock := economy.setting_num('free_shoe_lock_km');
  end if;

  insert into public.market_sneakers (
    owner_id, faction, rarity, variant, level, luck, comfort, durability,
    origin, efficiency_bps, comfort_bps, durability_pts,
    lock_km, withdrawable, draw_nonce, genesis_no
  ) values (
    p_user, v_faction, v_rarity, v_variant, 1, 1,
    -- 옛 comfort 칸(배율 1.00~1.40)은 옛 앱 화면용으로만 채운다.
    1 + v_cmf_lo / 10000.0, 100,
    p_origin,
    v_eff_lo + (economy.bytes_int(v_d, 8, 2) % (v_eff_hi - v_eff_lo + 1))::int,
    v_cmf_lo + (economy.bytes_int(v_d, 10, 2) % (v_cmf_hi - v_cmf_lo + 1))::int,
    100,
    v_lock, p_origin <> 'STARTER', v_nonce,
    case when p_genesis then nextval('public.genesis_seq')::int end
  ) returning id into v_id;

  return v_id;
end $$;
revoke all on function economy.draw_create(uuid, text, text, boolean) from public;

-- ══════════════════════════════════════════════════════════════════
-- 뽑기 권리 — 신규 무료 10회 · 지갑 보너스 10회(Genesis 1)
-- ══════════════════════════════════════════════════════════════════
create table if not exists public.draw_grants (
  user_id uuid not null references auth.users on delete cascade,
  kind text not null check (kind in ('FREE', 'BONUS')),
  granted int not null check (granted >= 0),
  used int not null default 0,
  genesis_granted int not null default 0,
  genesis_used int not null default 0,
  created_at timestamptz not null default now(),
  primary key (user_id, kind),
  check (used between 0 and granted),
  check (genesis_used between 0 and genesis_granted)
);

alter table public.draw_grants enable row level security;
revoke all on public.draw_grants from anon, authenticated;
drop policy if exists draw_grants_select_own on public.draw_grants;
create policy draw_grants_select_own on public.draw_grants
  for select using ((select auth.uid()) = user_id);
grant select on public.draw_grants to authenticated;

-- 첫 신발은 한 사람에 하나
create unique index if not exists market_sneakers_one_starter
  on public.market_sneakers (owner_id) where origin = 'STARTER';

/*
 * 앱을 켤 때 부른다. 여러 번 불러도 한 번만 일어난다.
 *   - 서버 신발이 없으면 첫 신발(바람 · 일반)을 주고 신긴다
 *   - 무료 뽑기 시작 시각 뒤에 가입했으면 무료 뽑기 10회
 */
create or replace function public.economy_bootstrap()
returns void
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_since timestamptz := (economy.setting('free_draw_since') #>> '{}')::timestamptz;
  v_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform economy.seed_ensure(v_user);

  if not exists (select 1 from public.market_sneakers where owner_id = v_user and origin = 'STARTER') then
    insert into public.market_sneakers (
      owner_id, faction, rarity, variant, level, luck, comfort, durability,
      origin, efficiency_bps, comfort_bps, durability_pts, withdrawable
    ) values (
      v_user, 'WIND', 'COMMON', 0, 1, 1, 1.01, 100,
      'STARTER', 300, 100, 100, false
    ) returning id into v_id;
    if not exists (select 1 from public.market_sneakers where owner_id = v_user and equipped) then
      update public.market_sneakers set equipped = true where id = v_id;
    end if;
  end if;

  if (select p.created_at from public.profiles p where p.id = v_user) >= v_since then
    insert into public.draw_grants (user_id, kind, granted)
    values (v_user, 'FREE', 10)
    on conflict (user_id, kind) do nothing;
  end if;
end $$;

revoke all on function public.economy_bootstrap() from public, anon;
grant execute on function public.economy_bootstrap() to authenticated;

-- 무료 뽑기 한 번. 남은 횟수가 없으면 23514.
create or replace function public.draw_free()
returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  update public.draw_grants set used = used + 1
   where user_id = v_user and kind = 'FREE' and used < granted;
  if not found then
    raise exception '무료 뽑기가 남아 있지 않습니다' using errcode = '23514';
  end if;

  return economy.draw_create(v_user, 'FREE_DRAW');
end $$;

-- SUP 로 뽑기. 치르는 것과 굴리는 것이 한 번에 일어난다.
create or replace function public.draw_paid()
returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform economy.ledger_apply(v_user, 'SPEND_DRAW', -economy.draw_cost(), '신발 뽑기');
  return economy.draw_create(v_user, 'PAID_DRAW');
end $$;

revoke all on function public.draw_free() from public, anon;
revoke all on function public.draw_paid() from public, anon;
grant execute on function public.draw_free() to authenticated;
grant execute on function public.draw_paid() to authenticated;

-- 공정성 — 지금 씨앗의 해시와 다음 번호
create or replace function public.draw_fairness()
returns table (seed_hash text, next_nonce bigint, first_nonce bigint)
language plpgsql security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform economy.seed_ensure(v_user);
  return query select d.seed_hash, d.nonce, d.first_nonce from public.draw_seeds d where d.user_id = v_user;
end $$;

-- 씨앗을 바꾸고 옛 씨앗을 공개한다. 공개된 씨앗으로 지난 뽑기를 다시 계산해 볼 수 있다.
create or replace function public.draw_rotate_seed()
returns table (seed_hex text, seed_hash text, first_nonce bigint, last_nonce bigint)
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_old public.draw_seeds;
  v_new bytea := economy.random_bytes32();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform economy.seed_ensure(v_user);

  select * into v_old from public.draw_seeds where user_id = v_user for update;
  insert into public.draw_seed_reveals (user_id, seed_hex, seed_hash, first_nonce, last_nonce)
  values (v_user, encode(v_old.seed, 'hex'), v_old.seed_hash, v_old.first_nonce, v_old.nonce - 1);

  -- 번호는 이어서 센다. 씨앗이 바뀌어도 번호가 겹치지 않아 신발의 draw_nonce 로 어느 씨앗인지 안다.
  update public.draw_seeds
     set seed = v_new, seed_hash = encode(sha256(v_new), 'hex'), first_nonce = nonce, created_at = now()
   where user_id = v_user;

  return query select encode(v_old.seed, 'hex'), v_old.seed_hash, v_old.first_nonce, v_old.nonce - 1;
end $$;

revoke all on function public.draw_fairness() from public, anon;
revoke all on function public.draw_rotate_seed() from public, anon;
grant execute on function public.draw_fairness() to authenticated;
grant execute on function public.draw_rotate_seed() to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 강화 · 수리 · 착용
-- ══════════════════════════════════════════════════════════════════

-- 내 신발을 잠그고 가져온다. 앱에서 쓸 수 있는 상태가 아니면 거절.
create or replace function economy.my_app_sneaker(p_user uuid, p_id bigint)
returns public.market_sneakers
language plpgsql security definer set search_path = public as $$
declare v public.market_sneakers;
begin
  select * into v from public.market_sneakers where id = p_id for update;
  if not found or v.owner_id <> p_user then
    raise exception '내 신발이 아닙니다' using errcode = '42501';
  end if;
  if v.chain_state <> 'APP' then
    raise exception '체인에 있는 신발입니다. 먼저 앱으로 넣어 주세요' using errcode = '22023';
  end if;
  if v.status <> 'OWNED' then
    raise exception '판매 중인 신발입니다' using errcode = '22023';
  end if;
  return v;
end $$;
revoke all on function economy.my_app_sneaker(uuid, bigint) from public;

create or replace function public.sneaker_upgrade(p_id bigint)
returns int
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v public.market_sneakers;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  v := economy.my_app_sneaker(v_user, p_id);

  -- 폰에서 올린 신발은 레벨을 서버가 믿지 않으므로 서버에서 강화해도 효과가 없다.
  -- 효과 없는 곳에 SUP 를 받지 않는다.
  if v.origin in ('IMPORT', 'MINT') then
    raise exception '예전 신발은 강화할 수 없습니다' using errcode = '22023';
  end if;
  if v.level >= economy.max_level(v.rarity) then
    raise exception '최대 레벨입니다' using errcode = '23514';
  end if;

  perform economy.ledger_apply(v_user, 'SPEND_UPGRADE', -economy.upgrade_cost(v.rarity, v.level),
                               format('신발 강화 #%s Lv%s→%s', v.mint_number, v.level, v.level + 1));
  update public.market_sneakers set level = level + 1, updated_at = now() where id = p_id;
  return v.level + 1;
end $$;

-- 수리. p_points 를 비우면 가득 채운다. 치른 SUP 는 누구에게도 가지 않는다(소각).
create or replace function public.sneaker_repair(p_id bigint, p_points numeric default null)
returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v public.market_sneakers;
  v_points numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  v := economy.my_app_sneaker(v_user, p_id);

  v_points := least(coalesce(p_points, 100), 100 - v.durability_pts);
  if v_points is null or v_points <= 0 then
    raise exception '고칠 곳이 없습니다' using errcode = '23514';
  end if;
  v_points := round(v_points, 2);

  perform economy.ledger_apply(v_user, 'SPEND_REPAIR',
    -round(v_points * economy.repair_cost_per_point(v.rarity, v.level), 4),
    format('신발 수리 #%s +%s', v.mint_number, v_points));
  update public.market_sneakers
     set durability_pts = least(durability_pts + v_points, 100),
         durability = least(durability_pts + v_points, 100)::int,
         updated_at = now()
   where id = p_id;
  return least(v.durability_pts + v_points, 100);
end $$;

create or replace function public.sneaker_equip(p_id bigint)
returns void
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform economy.my_app_sneaker(v_user, p_id);
  update public.market_sneakers set equipped = false where owner_id = v_user and equipped and id <> p_id;
  update public.market_sneakers set equipped = true, updated_at = now() where id = p_id;
end $$;

revoke all on function public.sneaker_upgrade(bigint) from public, anon;
revoke all on function public.sneaker_repair(bigint, numeric) from public, anon;
revoke all on function public.sneaker_equip(bigint) from public, anon;
grant execute on function public.sneaker_upgrade(bigint) to authenticated;
grant execute on function public.sneaker_repair(bigint, numeric) to authenticated;
grant execute on function public.sneaker_equip(bigint) to authenticated;

-- 내 신발 — 화면에 필요한 실효 스탯까지
create or replace function public.my_sneakers()
returns table (
  id bigint, faction text, rarity text, variant int, level int, max_level int,
  efficiency_bps int, comfort_bps int, durability numeric,
  equipped boolean, origin text, chain_state text, status text,
  mint_number bigint, genesis_no int, token_id numeric,
  km_run numeric, lock_km numeric, can_withdraw boolean,
  upgrade_cost numeric, repair_cost_per_point numeric
)
language plpgsql stable security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return query
  select s.id, s.faction, s.rarity, s.variant, s.level, economy.max_level(s.rarity),
         e.efficiency_bps, e.comfort_bps, s.durability_pts,
         s.equipped, s.origin, s.chain_state, s.status,
         s.mint_number, s.genesis_no, s.token_id,
         s.km_run, s.lock_km,
         (s.withdrawable and s.km_run >= s.lock_km and s.chain_state = 'APP' and s.status = 'OWNED'),
         economy.upgrade_cost(s.rarity, s.level),
         economy.repair_cost_per_point(s.rarity, s.level)
    from public.market_sneakers s,
         lateral economy.sneaker_effective(s.origin, s.rarity, s.level,
           s.efficiency_bps, s.comfort_bps, s.durability_pts) e
   where s.owner_id = v_user
   order by s.equipped desc, s.id;
end $$;

revoke all on function public.my_sneakers() from public, anon;
grant execute on function public.my_sneakers() to authenticated;

-- 첫 신발은 팔 수 없고, 무료로 받은 신발은 잠금 거리를 채우기 전에는 팔 수 없다.
-- 무료 신발을 팔 수 있으면 계정을 여러 개 만들어 무료 뽑기를 SUP 로 바꾼다.
create or replace function economy.guard_listing() returns trigger
language plpgsql security definer set search_path = public as $$
declare v public.market_sneakers;
begin
  select * into v from public.market_sneakers where id = new.sneaker_id;
  if v.chain_state <> 'APP' or v.equipped then
    raise exception '착용 중이거나 체인에 있는 신발은 팔 수 없습니다' using errcode = '22023';
  end if;
  if v.origin = 'STARTER' then
    raise exception '첫 신발은 팔 수 없습니다' using errcode = '22023';
  end if;
  if v.origin in ('IMPORT', 'MINT') then
    raise exception '예전 신발은 팔 수 없습니다' using errcode = '22023';
  end if;
  if v.km_run < v.lock_km then
    raise exception '이 신발로 %km 를 더 달려야 팔 수 있습니다', round(v.lock_km - v.km_run, 1)
      using errcode = '22023';
  end if;
  return new;
end $$;

/*
 * 주인이 바뀌는 모든 길을 한곳에서 막는다.
 *
 * 매물 표 트리거(guard_listing)만으로는 부족하다. 즉시 판매(market_sell_now)와
 * 매물을 거는 순간 맞는 입찰이 있을 때의 체결은 매물 줄을 만들지 않고 바로
 * market_settle 로 주인을 바꾼다. 그래서 신발 표 자체에서 본다.
 *
 * 주인이 바뀌어도 되는 경우:
 *   - 앱 안에 있고(APP), 신고 있지 않고, 첫 신발 · 예전(폰) 신발이 아니고, 잠금 거리를 채운 신발
 *   - 체인에서 넣은 신발(ON_CHAIN → APP) — 어테스터의 넣기 처리만 이 길을 연다
 *   - 계정 삭제로 주인이 비워질 때
 */
create or replace function economy.guard_owner_change() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if new.owner_id is not distinct from old.owner_id then
    return new;
  end if;
  if new.owner_id is null then
    new.equipped := false;
    return new;
  end if;
  if old.chain_state = 'ON_CHAIN' and new.chain_state = 'APP'
     and current_setting('stepup.chain_deposit', true) = 'on' then
    new.equipped := false;
    return new;
  end if;

  if old.chain_state <> 'APP' or new.chain_state <> 'APP' then
    raise exception '체인에 있는 신발은 앱에서 넘길 수 없습니다' using errcode = '22023';
  end if;
  if old.equipped then
    raise exception '신고 있는 신발은 넘길 수 없습니다' using errcode = '22023';
  end if;
  if old.origin in ('STARTER', 'IMPORT', 'MINT') then
    raise exception '넘길 수 없는 신발입니다' using errcode = '22023';
  end if;
  if old.km_run < old.lock_km then
    raise exception '이 신발로 %km 를 더 달려야 넘길 수 있습니다', round(old.lock_km - old.km_run, 1)
      using errcode = '22023';
  end if;
  new.equipped := false;
  return new;
end $$;

drop trigger if exists market_sneakers_owner_guard on public.market_sneakers;
create trigger market_sneakers_owner_guard before update of owner_id on public.market_sneakers
  for each row execute function economy.guard_owner_change();

-- ══════════════════════════════════════════════════════════════════
-- 0024_session_reward.sql
-- ══════════════════════════════════════════════════════════════════

-- 러닝 보상 — 서버가 신발 · 에너지 · 파티 · 부스터까지 모두 계산한다.
--
-- 0018 까지는 폰이 부스트(bps)와 파티 인원을 보냈고 서버는 그 값을 믿었다.
-- 이제 폰이 보내는 값 가운데 믿는 것은 걸음 · 시각 · 경로뿐이고, 그것도 검사한다.
--
--   가짜 위치 앱          폰이 "모의 위치"라고 알리면 보상 0 (VOID)
--   파티 인원             파티 출발 때 서버가 적어 둔 명단으로 센다 (결정표 X2)
--   신발                  서버 신발 표에서 지금 신은 신발의 효율성 · 착화감 · 내구도
--   에너지                서버가 센다. 한국 자정에 다시 찬다
--   하루 상한             걸음 48,000 보 + 금액 상한(신규 계정은 절반)
--   무효가 잦은 사람      최근 7일에 무효 러닝이 5번 이상이면 적립을 보류
--
-- 인자는 옛 앱과 맞춘다. p_boost_bps · p_party_size · p_faction 은 받기만 하고 쓰지 않는다.

-- ══════════════════════════════════════════════════════════════════
-- 세션 표에 칸 더하기
-- ══════════════════════════════════════════════════════════════════
alter table public.walk_sessions
  add column if not exists mock_location boolean not null default false,
  -- 검사를 통과한 걸음(에너지 · 금액 상한을 걸기 전). 목표 달성과 도전에 쓴다.
  add column if not exists verified_steps int,
  add column if not exists energy_used numeric(10, 4) not null default 0,
  add column if not exists sneaker_id bigint,
  -- 경로가 받쳐 준 러닝인가(경로 300m 이상). 걸음만 있는 러닝은 폰이 걸음을 지어낼 수 있다.
  add column if not exists gps_backed boolean not null default false,
  -- 경로가 받쳐 준 걸음. 목표 달성 · 주간 도전은 이것만 센다.
  add column if not exists backed_steps int not null default 0,
  -- 신발 잠금 거리 · 꺼내기 조건에 쳐 주는 거리(m). 경로로 잰 거리만, 하루 상한 안에서.
  add column if not exists gps_credit_m double precision not null default 0,
  -- 파티 보너스를 받은 파티. 한 파티에서 한 러닝만 받는다.
  add column if not exists party_id bigint;

-- 꺼내기 조건(누적 거리)은 경로로 잰 거리만 센다. lifetime_km 는 순위용이라 걸음 거리도 들어간다.
alter table public.profiles add column if not exists gps_km double precision not null default 0;

-- 운영 값 — 걸음만 있는 러닝의 하루 적립 상한, 올린 날 기준 하루 적립 상한, 하루에 쳐 주는 경로 거리
insert into public.economy_settings (key, value) values
  ('no_gps_daily_cap',  '60'::jsonb),
  ('upload_daily_cap',  '1200'::jsonb),
  ('gps_km_daily_cap',  '50'::jsonb)
on conflict (key) do nothing;

-- 효율성은 레전더리 30레벨에서 +27.5% 까지 오른다. 예전 상한(20%)으로는 못 담는다.
alter table public.walk_sessions drop constraint if exists walk_sessions_boost_bps_check;
alter table public.walk_sessions add constraint walk_sessions_boost_bps_check
  check (boost_bps between 0 and 5000);

-- ══════════════════════════════════════════════════════════════════
-- 파티 러닝 명단 — 출발할 때 적어 둔다 (결정표 X2)
-- ══════════════════════════════════════════════════════════════════
--
-- 참가자는 러닝을 마치면 방에서 나가고(party_leave) 방 명단에서 지워진다.
-- 그래서 출발 순간의 명단을 따로 남긴다. 적립 때는 이 명단의 인원만 믿는다.
create table if not exists public.party_runs (
  party_id bigint not null references public.parties on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  starts_at timestamptz not null,
  primary key (party_id, user_id)
);

create index if not exists party_runs_user_time on public.party_runs (user_id, starts_at);

alter table public.party_runs enable row level security;
revoke all on public.party_runs from anon, authenticated;

create or replace function public.party_start(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare v_starts timestamptz := now() + interval '4 seconds';
begin
  if not exists (
    select 1 from public.parties
     where id = p_party and host_id = auth.uid() and status = 'LOBBY'
  ) then
    raise exception '로비에서 방장만 출발할 수 있습니다' using errcode = '42501';
  end if;
  if not exists (
    select 1 from public.party_members
     where party_id = p_party and user_id = auth.uid() and ready
  ) then
    raise exception '방장이 먼저 준비해야 합니다' using errcode = '23514';
  end if;

  delete from public.party_members where party_id = p_party and not ready;
  update public.parties
     set status = 'COUNTDOWN', starts_at = v_starts
   where id = p_party;

  insert into public.party_runs (party_id, user_id, starts_at)
  select p_party, m.user_id, v_starts from public.party_members m where m.party_id = p_party
  on conflict do nothing;
end;
$$;

-- 이 러닝의 파티. 출발 시각이 러닝 시작과 10분 안쪽인 파티 명단에서 찾는다.
-- 인원은 그 명단의 사람 수다.
drop function if exists economy.party_size_for(uuid, timestamptz);
create or replace function economy.party_for(p_user uuid, p_started_at timestamptz,
                                             out party_id bigint, out size int)
language sql stable security definer set search_path = public as $$
  with p as (
    select r.party_id from public.party_runs r
     where r.user_id = p_user
       and r.starts_at between p_started_at - interval '10 minutes'
                           and p_started_at + interval '10 minutes'
     order by abs(extract(epoch from (r.starts_at - p_started_at)))
     limit 1)
  select p.party_id, (select count(*)::int from public.party_runs r2 where r2.party_id = p.party_id)
    from p
$$;
revoke all on function economy.party_for(uuid, timestamptz) from public;

-- ══════════════════════════════════════════════════════════════════
-- 적립 (0018 을 대신한다)
-- ══════════════════════════════════════════════════════════════════

-- 인자가 하나 늘어서(p_mock_location) 옛 8개짜리를 먼저 치운다. 둘 다 있으면
-- 옛 앱의 호출이 어느 쪽인지 모호해져 실패한다.
drop function if exists public.record_session(timestamptz, timestamptz, int, int, text, int, int, text);

create or replace function public.record_session(
  p_started_at timestamptz,
  p_ended_at timestamptz,
  p_steps int,
  p_duration_sec int,
  p_track text default '',
  p_boost_bps int default 0,
  p_party_size int default 1,
  p_faction text default '',
  -- 안드로이드가 "모의 위치"로 표시한 좌표가 하나라도 있었나
  p_mock_location boolean default false
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
  v_gday date;
  v_already_steps int;
  v_already_points numeric;
  v_verified int;
  v_rewardable int;
  v_points numeric(20, 4);
  v_cap numeric;
  v_verdict text := 'CLEAN';
  v_reason text := '';
  v_session_id bigint;
  v_kind text;
  v_elapsed int;
  v_top_speed double precision := 0;
  v_glitch double precision := 0;
  v_gps_m double precision := 0;
  v_points_n int := 0;
  v_first_at bigint;
  v_last_at bigint;
  v_step_m double precision;
  v_distance_m double precision;
  v_track text := coalesce(p_track, '');
  v_party int := 1;
  v_party_id bigint;
  v_gps_backed boolean;
  v_credit_m double precision := 0;
  v_cap_left numeric;
  v_shoe public.market_sneakers;
  v_eff int := 0;
  v_comfort int := 0;
  v_dur numeric := 100;
  v_energy_left numeric;
  v_energy_per_step numeric;
  v_energy_used numeric := 0;
  v_xp numeric := 1;
  v_recent_void int;
  v_new_account boolean;
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
  if p_ended_at > now() + interval '5 minutes' then
    raise exception '종료 시각이 미래입니다' using errcode = '22023';
  end if;
  if p_started_at < now() - interval '7 days' and not exists (
       select 1 from public.walk_sessions s
        where s.user_id = v_user and s.started_at = p_started_at) then
    raise exception '너무 오래된 러닝입니다' using errcode = '22023';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  -- 이미 올린 러닝의 재시도면 원래 결과를 그대로 돌려준다. 아래 계산을 다시 하면
  -- 에너지 · 내구도를 두 번 깎는다.
  select s.id, s.verdict, s.points_awarded into v_session_id, v_verdict, v_points
    from public.walk_sessions s
   where s.user_id = v_user and s.started_at = p_started_at;
  if found then
    return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user);
    return;
  end if;
  v_verdict := 'CLEAN';

  v_elapsed := least(
    greatest(coalesce(p_duration_sec, 0), 0),
    floor(extract(epoch from (p_ended_at - p_started_at)))::int
  );
  v_gday := economy.game_day(p_started_at);
  v_step_m := p_steps * 0.762;

  select t.top_speed_kmh, t.glitch_ratio into v_top_speed, v_glitch
    from economy.track_speed_stats(v_track) t;
  select t.gps_m, t.points, t.first_at, t.last_at
    into v_gps_m, v_points_n, v_first_at, v_last_at
    from economy.track_summary(v_track) t;

  -- ── 판정 ──
  if coalesce(p_mock_location, false) then
    v_verdict := 'VOID';
    v_reason := '가짜 위치가 감지되었습니다';

  elsif p_steps::numeric * 60 / greatest(v_elapsed, 60) > 240 then
    v_verdict := 'VOID';
    v_reason := '케이던스가 사람 범위를 벗어납니다';

  elsif v_glitch > 0.5 then
    v_verdict := 'VOID';
    v_reason := '이동 속도가 사람 범위를 벗어납니다';

  elsif v_points_n >= 2 and (
        v_first_at < (extract(epoch from p_started_at) * 1000)::bigint - economy.track_time_slack_ms()
     or v_last_at > (extract(epoch from p_ended_at) * 1000)::bigint + economy.track_time_slack_ms()) then
    v_verdict := 'VOID';
    v_reason := '경로 시각이 러닝 시간과 맞지 않습니다';

  elsif v_track <> '' and exists (
        select 1 from public.walk_sessions s
         where s.user_id = v_user and s.track <> ''
           and md5(s.track) = md5(v_track) and s.started_at <> p_started_at) then
    v_verdict := 'VOID';
    v_reason := '이미 올린 경로입니다';

  -- 같은 사람의 러닝은 시간이 겹칠 수 없다. 겹치게 여러 개를 올리면 같은 시간을
  -- 여러 번 받는다.
  elsif exists (
        select 1 from public.walk_sessions s
         where s.user_id = v_user and s.verdict <> 'VOID'
           and s.started_at < p_ended_at and s.ended_at > p_started_at) then
    v_verdict := 'VOID';
    v_reason := '다른 러닝과 시간이 겹칩니다';

  elsif v_gps_m >= 1000 and v_step_m < v_gps_m * 0.2 then
    v_verdict := 'VOID';
    v_reason := '걸음 없이 이동한 거리입니다';
  end if;

  -- ── 검사를 통과한 걸음 ──
  if v_verdict = 'VOID' then
    v_verified := 0;
  else
    v_verified := p_steps;
    if v_gps_m >= economy.gps_check_min_m()
       and v_step_m > v_gps_m * economy.step_gps_max_ratio() then
      v_verified := floor(v_gps_m * economy.step_gps_max_ratio() / 0.762)::int;
      v_verdict := 'FLAGGED';
      v_reason := '걸음 수가 GPS 거리보다 많습니다';
    end if;
  end if;
  v_rewardable := v_verified;

  -- ── 하루 걸음 상한 (한국 하루) ──
  if v_rewardable > 0 then
    select coalesce(sum(s.rewarded_steps), 0) into v_already_steps
      from public.walk_sessions s
     where s.user_id = v_user
       and s.verdict in ('CLEAN', 'FLAGGED')
       and economy.game_day(s.started_at) = v_gday;
    if v_rewardable > economy.max_daily_steps() - v_already_steps then
      v_rewardable := greatest(economy.max_daily_steps() - v_already_steps, 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루 적립 상한에 걸렸습니다');
    end if;
  end if;

  -- ── 무효가 잦으면 보류 ──
  if v_rewardable > 0 then
    select count(*) into v_recent_void from public.walk_sessions s
     where s.user_id = v_user and s.verdict = 'VOID'
       and s.started_at > now() - interval '7 days';
    if v_recent_void >= 5 then
      v_rewardable := 0;
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '최근 무효 러닝이 많아 적립을 보류합니다');
    end if;
  end if;

  -- ── 신발 · 에너지 ──
  select * into v_shoe from economy.equipped_sneaker(v_user);
  if found then
    select e.efficiency_bps, e.comfort_bps, e.durability into v_eff, v_comfort, v_dur
      from economy.sneaker_effective(v_shoe.origin, v_shoe.rarity, v_shoe.level,
             v_shoe.efficiency_bps, v_shoe.comfort_bps, v_shoe.durability_pts) e;
  end if;

  if v_rewardable > 0 then
    v_energy_left := economy.energy_left(v_user, v_gday);
    v_energy_per_step := (1 - v_comfort / 10000.0) / economy.steps_per_energy();
    if v_rewardable * v_energy_per_step > v_energy_left then
      v_rewardable := greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '에너지를 다 썼습니다');
    end if;
    v_energy_used := round(v_rewardable * v_energy_per_step, 4);
  end if;

  -- 파티 보너스는 한 파티에서 한 러닝만
  select pf.party_id, pf.size into v_party_id, v_party from economy.party_for(v_user, p_started_at) pf;
  if v_party_id is null or exists (select 1 from public.walk_sessions s
                                    where s.user_id = v_user and s.party_id = v_party_id) then
    v_party_id := null;
    v_party := 1;
  end if;
  if economy.boost_active(v_user, 'XP_BOOSTER', p_started_at) then
    v_xp := 2;
  end if;

  v_points := round(
    v_rewardable * economy.points_per_step()
      * (1 + v_eff / 10000.0)
      * economy.durability_factor(v_dur)
      * economy.party_multiplier(v_party)
      * v_xp,
    4
  );
  if v_rewardable > 0 and economy.durability_factor(v_dur) < 1 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 낮아 적립이 줄었습니다');
  end if;

  -- ── 하루 금액 상한 — 가입 7일 안은 절반 ──
  if v_points > 0 then
    v_new_account := (select p.created_at from public.profiles p where p.id = v_user)
                     > now() - make_interval(days => economy.setting_num('new_account_days')::int);
    v_cap := economy.setting_num('daily_earn_cap') * case when v_new_account then 0.5 else 1 end;
    select coalesce(sum(s.points_awarded), 0) into v_already_points
      from public.walk_sessions s
     where s.user_id = v_user and economy.game_day(s.started_at) = v_gday;
    if v_points > v_cap - v_already_points then
      v_points := greatest(round(v_cap - v_already_points, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루 적립 금액 상한에 걸렸습니다');
    end if;
  end if;

  -- ── 걸음만 있는 러닝 · 한꺼번에 올린 러닝 ──
  --
  -- 경로가 없으면 걸음은 폰이 지어낼 수 있다. 그런 러닝은 하루 적은 몫만 준다.
  -- 7일치를 모아 한 번에 올리면 날마다 상한이 새로 열리므로, 올린 날 기준 상한도 둔다.
  v_gps_backed := v_verdict <> 'VOID' and v_gps_m >= economy.gps_check_min_m();
  if v_points > 0 and not v_gps_backed then
    select economy.setting_num('no_gps_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and not s.gps_backed and economy.game_day(s.started_at) = v_gday;
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '경로 없는 러닝의 하루 상한에 걸렸습니다');
    end if;
  end if;
  if v_points > 0 then
    select economy.setting_num('upload_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and economy.game_day(s.created_at) = economy.game_day(now());
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루에 올릴 수 있는 적립 상한에 걸렸습니다');
    end if;
  end if;

  v_distance_m := case
    when v_gps_m >= 100 then least(v_gps_m, greatest(v_verified, 0) * 0.762 * 3 + 100)
    else v_step_m
  end;
  if v_verdict = 'VOID' then v_distance_m := 0; end if;

  -- 잠금 거리 · 꺼내기 조건에 쳐 주는 거리 — 경로로 잰 것만, 하루 상한 안에서
  if v_gps_backed then
    select greatest(economy.setting_num('gps_km_daily_cap') * 1000 - coalesce(sum(s.gps_credit_m), 0), 0)
      into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and economy.game_day(s.started_at) = v_gday;
    v_credit_m := least(v_distance_m, v_cap_left);
  end if;

  -- ── 기록 ──
  insert into public.walk_sessions (
    user_id, started_at, ended_at, duration_sec, steps,
    distance_meters, calories, track, boost_bps, party_size,
    faction, top_speed_kmh, gps_distance_m,
    verdict, verdict_reason, points_awarded, rewarded_steps,
    mock_location, verified_steps, energy_used, sneaker_id,
    gps_backed, backed_steps, gps_credit_m, party_id
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    v_distance_m, p_steps * 0.04, v_track, least(v_eff, 5000), least(greatest(v_party, 1), 20),
    '', case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable,
    coalesce(p_mock_location, false), v_verified, v_energy_used, v_shoe.id,
    v_gps_backed, case when v_gps_backed then v_verified else 0 end, v_credit_m, v_party_id
  )
  returning id into v_session_id;

  if v_points > 0 then
    v_kind := case when v_party > 1 then 'EARN_PARTY' else 'EARN_WALK' end;
    insert into public.sup_ledger (user_id, kind, amount, description, session_id, occurred_at)
    values (v_user, v_kind, v_points, format('러닝 세션 적립 (%s보)', v_rewardable),
            v_session_id, p_ended_at);
  end if;

  if v_energy_used > 0 then
    insert into public.energy_days (user_id, day, used) values (v_user, v_gday, v_energy_used)
    on conflict (user_id, day) do update set used = public.energy_days.used + excluded.used;
  end if;

  -- 신발이 닳는다. 폰에서 올린 예전 신발은 서버가 값을 믿지 않으므로 거리만 센다.
  if v_shoe.id is not null and v_verdict <> 'VOID' and v_distance_m > 0 then
    update public.market_sneakers s
       set km_run = s.km_run + round((v_credit_m / 1000)::numeric, 3),
           durability_pts = case when s.origin in ('IMPORT', 'MINT') then s.durability_pts
             else greatest(s.durability_pts
               - round((v_distance_m / 1000)::numeric * economy.durability_loss_per_km(s.rarity), 2), 0) end,
           updated_at = now()
     where s.id = v_shoe.id;
    update public.market_sneakers s set durability = floor(s.durability_pts)::int
     where s.id = v_shoe.id and s.origin not in ('IMPORT', 'MINT');
  end if;

  if v_verdict <> 'VOID' then
    update public.profiles
       set lifetime_km = lifetime_km + (v_distance_m / 1000),
           gps_km = gps_km + (v_credit_m / 1000),
           top_speed_kmh = greatest(top_speed_kmh, v_top_speed)
     where id = v_user;
  end if;

  return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user);
end;
$$;

comment on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean) is
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발·에너지·파티·부스터·상한 모두 서버 값으로.';

revoke all on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean)
  from public, anon;
grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean)
  to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 목표 달성 보너스 — 서버가 확인한 걸음으로
-- ══════════════════════════════════════════════════════════════════
--
-- 앱의 RewardEconomy.goalBonus 와 같은 식: 목표 1,000보당 2.5 SUP, 연속 달성
-- 하루당 +10% (7일까지). 걸음은 폰의 걸음 센서가 아니라 서버가 검사한 러닝의
-- 걸음(verified_steps)만 센다 — 폰이 올린 하루 걸음(daily_steps)은 누구나 적어 넣을 수 있다.
create or replace function economy.verified_steps_on(p_user uuid, p_day date) returns int
language sql stable security definer set search_path = public, economy as $$
  select least(coalesce(sum(s.backed_steps), 0), economy.max_daily_steps())::int
    from public.walk_sessions s
   where s.user_id = p_user and s.verdict in ('CLEAN', 'FLAGGED')
     and economy.game_day(s.started_at) = p_day
$$;
revoke all on function economy.verified_steps_on(uuid, date) from public;

create or replace function public.goal_claim()
returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_today date := economy.game_day(now());
  v_goal int;
  v_streak int := 1;
  v_day date;
  v_bonus numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  select least(greatest(coalesce(p.daily_goal, 8000), 1000), 30000) into v_goal
    from public.profiles p where p.id = v_user;

  if economy.verified_steps_on(v_user, v_today) < v_goal then
    raise exception '아직 목표를 채우지 않았습니다' using errcode = '23514';
  end if;

  -- 어제부터 거꾸로 — 보너스를 받았거나 스트릭 보호가 덮은 날이 이어진 만큼
  v_day := v_today - 1;
  while v_streak < 8 loop
    exit when not (
      exists (select 1 from public.sup_ledger l where l.user_id = v_user and l.ref = 'goal:' || v_day)
      or exists (select 1 from public.boosts b
                  where b.user_id = v_user and b.kind = 'STREAK_SHIELD'
                    and b.starts_at < economy.game_day_start(v_day + 1)
                    and b.ends_at > economy.game_day_start(v_day)));
    v_streak := v_streak + 1;
    v_day := v_day - 1;
  end loop;

  v_bonus := round((v_goal / 1000.0) * 2.5 * (1 + 0.1 * least(v_streak - 1, 7)), 2);
  -- 같은 날 두 번 받으면 ref 가 겹쳐 23505 로 거절된다.
  perform economy.ledger_apply(v_user, 'BONUS_GOAL', v_bonus,
                               format('목표 달성 보너스 (%s일 연속)', v_streak), 'goal:' || v_today);
  return v_bonus;
end $$;

revoke all on function public.goal_claim() from public, anon;
grant execute on function public.goal_claim() to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 도전 진행 — 주간 걸음도 서버가 확인한 걸음으로
-- ══════════════════════════════════════════════════════════════════
create or replace function public.event_progress(p_event text, p_tz text default 'Asia/Seoul')
returns double precision
language plpgsql
stable
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_tz text := coalesce(nullif(p_tz, ''), 'Asia/Seoul');
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not exists (select 1 from pg_timezone_names where name = v_tz) then
    v_tz := 'Asia/Seoul';
  end if;

  if p_event = 'step_surge' then
    -- 예전에는 폰이 올린 하루 걸음(daily_steps)을 더했다. 그 값은 폰이 마음대로
    -- 적을 수 있어 250 SUP 가 거저 나갔다. 경로가 받쳐 준 러닝 걸음만, 하루 상한까지 센다.
    return coalesce((
      select sum(economy.verified_steps_on(v_user, d::date))
        from generate_series(economy.game_day(now()) - 6, economy.game_day(now()), interval '1 day') d
    ), 0);
  elsif p_event = 'night_quest' then
    return coalesce((
      select sum(s.distance_meters) / 1000.0 from public.walk_sessions s
       where s.user_id = v_user
         and s.verdict not in ('FLAGGED', 'VOID')
         and extract(hour from s.started_at at time zone v_tz) >= economy.night_from_hour()
    ), 0);
  end if;
  raise exception '없는 도전입니다' using errcode = '22023';
end;
$$;

-- ══════════════════════════════════════════════════════════════════
-- 코스 완주 보상 — 서버가 확인한 완주에만
-- ══════════════════════════════════════════════════════════════════
--
-- 코스 거리는 만든 사람이 적은 값이라 믿지 않는다. 실제로 달린 거리와 코스
-- 거리 중 짧은 쪽으로 1km 당 1 SUP, 최대 42 SUP. 같은 코스는 하루에 한 번만.
create or replace function public.course_run_submit(p_course_track text, p_started_at timestamptz)
returns table (
  course_id bigint,
  duration_sec int,
  rank int,
  runners int
)
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_course bigint;
  v_course_track text;
  v_course_km double precision;
  v_session public.walk_sessions%rowtype;
  v_inserted bigint;
  v_reward numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  v_course := public.course_by_track(p_course_track);
  if v_course is null then
    return;
  end if;
  select c.track, c.distance_km into v_course_track, v_course_km from public.courses c where c.id = v_course;

  select * into v_session
    from public.walk_sessions s
   where s.user_id = v_user and s.started_at = p_started_at;
  if not found then
    raise exception '러닝 기록이 서버에 없습니다' using errcode = '22023';
  end if;
  if v_session.verdict = 'VOID' or v_session.duration_sec <= 0 then
    raise exception '인정되지 않은 러닝입니다' using errcode = '23514';
  end if;
  if economy.course_coverage(v_course_track, v_session.track) < 0.8 then
    raise exception '코스를 따라 달리지 않았습니다' using errcode = '23514';
  end if;

  insert into public.course_runs (course_id, user_id, session_id, duration_sec)
  values (v_course, v_user, v_session.id, v_session.duration_sec)
  on conflict do nothing
  returning id into v_inserted;

  if v_inserted is not null then
    update public.courses set run_count = run_count + 1 where id = v_course;

    -- 남이 만든 코스만, 경로로 잰 거리로, 하루 한 번. 자기 코스를 여러 개 만들어
    -- 러닝 하나로 보상을 여러 번 받지 못하게 한다.
    v_reward := least(floor(least(coalesce(v_course_km, 0), v_session.gps_credit_m / 1000.0)), 42);
    if v_reward >= 1 and (select c.owner_id from public.courses c where c.id = v_course) <> v_user then
      begin
        perform economy.ledger_apply(v_user, 'EARN_COURSE', v_reward, '코스 완주 보상',
          'course:' || economy.game_day(v_session.started_at));
      exception when unique_violation then
        null;  -- 이 날의 코스 보상은 이미 받았다. 기록은 남기고 보상만 건너뛴다.
      end;
    end if;
  end if;

  return query
    with best as (
      select r.user_id, min(r.duration_sec) as t
        from public.course_runs r
       where r.course_id = v_course
       group by r.user_id
    )
    select v_course,
           (select b.t from best b where b.user_id = v_user),
           (select count(*)::int + 1 from best b
             where b.t < (select b2.t from best b2 where b2.user_id = v_user)),
           (select count(*)::int from best);
end;
$$;

-- ══════════════════════════════════════════════════════════════════
-- 0025_wallet_bridge.sql
-- ══════════════════════════════════════════════════════════════════

-- 지갑과 체인 — 연결 · 꺼내기 · 넣기 · 지갑 보너스 · 정지 스위치 · 정산 대조.
--
-- 흐름 (서명과 체인 제출은 어테스터가 한다 — attester/):
--
--   사용자  ──요청──▶  서버: 조건 검사 → 작업(chain_ops) 예약 · 잔고/신발 잠금
--   어테스터 ──서명 전 확인──▶ 서버: 예약된 작업만 서명 재료를 준다 → SIGNED
--   어테스터 ──제출──▶ 체인 → SUBMITTED
--   어테스터 ──확정 블록을 기다린 뒤 이벤트──▶ 서버: CONFIRMED (한 이벤트는 한 번만)
--   만료: 서명 유효 시간 + 안전 마진이 지나고, 체인에서 그 작업 번호가 안 쓰였음을
--         어테스터가 확인했을 때만 되돌린다 → EXPIRED
--
-- 사고를 막는 겹:
--   - 지갑 1개 = 계정 1개, 한 번 쓴 지갑은 다른 계정에 못 붙는다
--   - 지갑을 붙이거나 바꾸면 72시간 꺼내기 금지 (계정 탈취 대비), 알림을 보낸다
--   - 꺼내기는 2단계 인증(aal2) · 가입 7일 · 누적 20km 이후
--   - 사람별 · 전체 하루 상한, 전체 하루 NFT 발행 상한
--   - 정지 스위치(chain_paused) — 어테스터나 관리자가 켠다
--   - 작업 번호마다 체인에서 한 번만 성공한다(컨트랙트가 막는다)

-- ══════════════════════════════════════════════════════════════════
-- 어테스터 역할
-- ══════════════════════════════════════════════════════════════════
--
-- 어테스터는 service_role 을 쓰지 않는다. 이 역할로 서명된 JWT 하나만 들고,
-- 아래 attester_* 함수만 부를 수 있다. 표는 하나도 직접 못 읽는다.
do $$ begin
  create role stepup_attester nologin;
exception when duplicate_object then null; end $$;

do $$ begin
  -- Supabase 의 PostgREST 는 authenticator 로 접속해 JWT 의 role 로 바꾼다.
  if exists (select 1 from pg_roles where rolname = 'authenticator') then
    execute 'grant stepup_attester to authenticator';
  end if;
end $$;

grant usage on schema public to stepup_attester;

-- 2단계 인증을 거친 로그인인가 (Supabase JWT 의 aal)
create or replace function economy.mfa_ok() returns boolean
  language sql stable as $$
  select coalesce(auth.jwt() ->> 'aal', 'aal1') = 'aal2'
$$;

-- 사용자 번호 ↔ 체인의 bytes32. uuid 16바이트를 앞을 0 으로 채워 32바이트로.
create or replace function economy.account_ref(p_user uuid) returns text
  language sql immutable as $$
  select '0x' || lpad(replace(p_user::text, '-', ''), 64, '0')
$$;

create or replace function economy.account_from_ref(p_ref text) returns uuid
  language plpgsql immutable as $$
declare v text := lower(regexp_replace(coalesce(p_ref, ''), '^0x', ''));
begin
  if v !~ '^0{32}[0-9a-f]{32}$' then
    return null;
  end if;
  return (substr(v, 33, 8) || '-' || substr(v, 41, 4) || '-' || substr(v, 45, 4) || '-'
          || substr(v, 49, 4) || '-' || substr(v, 53, 12))::uuid;
end $$;

-- 작업 번호 ↔ bytes32. 컨트랙트가 이 값으로 "한 번만"을 지킨다.
create or replace function economy.op_ref(p_op uuid) returns text
  language sql immutable as $$
  select '0x' || lpad(replace(p_op::text, '-', ''), 64, '0')
$$;

-- ══════════════════════════════════════════════════════════════════
-- 지갑
-- ══════════════════════════════════════════════════════════════════
create table if not exists public.wallet_links (
  user_id uuid primary key references auth.users on delete cascade,
  address text not null unique check (address ~ '^0x[0-9a-f]{40}$'),
  linked_at timestamptz not null default now(),
  -- 붙이거나 바꾼 마지막 시각. 여기서 72시간은 꺼내지 못한다.
  changed_at timestamptz not null default now()
);

-- 한 번 붙은 지갑은 영원히 그 계정 것. 계정을 여러 개 만들어 한 지갑으로 몰아
-- 보너스를 받거나 상한을 나눠 쓰지 못하게 한다.
-- 계정을 지워도 줄은 남는다(주인만 비운다). 지우고 새 계정으로 같은 지갑을 붙여
-- 보너스를 다시 받는 것을 막는다.
create table if not exists public.wallet_history (
  address text primary key,
  user_id uuid references auth.users on delete set null,
  first_linked_at timestamptz not null default now()
);

create table if not exists public.wallet_link_nonces (
  user_id uuid primary key references auth.users on delete cascade,
  nonce text not null,
  created_at timestamptz not null default now()
);

alter table public.wallet_links enable row level security;
alter table public.wallet_history enable row level security;
alter table public.wallet_link_nonces enable row level security;
revoke all on public.wallet_links, public.wallet_history, public.wallet_link_nonces from anon, authenticated;
drop policy if exists wallet_links_select_own on public.wallet_links;
create policy wallet_links_select_own on public.wallet_links
  for select using ((select auth.uid()) = user_id);
grant select on public.wallet_links to authenticated;

-- 지갑이 서명할 문장. 어테스터가 이 문장의 서명을 검증한 뒤 attester_wallet_link 를 부른다.
create or replace function public.wallet_link_challenge()
returns text
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_nonce text := replace(gen_random_uuid()::text, '-', '');
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  -- 지갑을 붙이는 것부터 2단계 인증이 필요하다. 로그인만 뺏은 사람이 자기 지갑을
  -- 붙여 보너스 NFT 를 가져가거나 72시간 뒤 꺼내기를 노리지 못하게.
  if not economy.mfa_ok() then
    raise exception '2단계 인증이 필요합니다' using errcode = '42501';
  end if;
  insert into public.wallet_link_nonces (user_id, nonce) values (v_user, v_nonce)
  on conflict (user_id) do update set nonce = excluded.nonce, created_at = now();
  return economy.wallet_link_message(v_user, v_nonce);
end $$;

create or replace function economy.wallet_link_message(p_user uuid, p_nonce text) returns text
  language sql immutable as $$
  select format(E'StepUp 지갑 연결\n계정: %s\n확인 번호: %s\n\n이 서명은 거래가 아니며 수수료가 들지 않습니다.', p_user, p_nonce)
$$;

revoke all on function public.wallet_link_challenge() from public, anon;
grant execute on function public.wallet_link_challenge() to authenticated;

-- 보안 알림 — 알림 설정과 상관없이 보낸다. 모르는 사이 지갑이 바뀌면 알아야 한다.
create or replace function economy.security_notice(p_user uuid, p_kind text, p_args jsonb)
returns void language plpgsql security definer set search_path = public as $$
begin
  if exists (select 1 from public.push_tokens t where t.user_id = p_user) then
    insert into public.push_outbox (user_id, kind, args, link)
    values (p_user, p_kind, coalesce(p_args, '{}'::jsonb), 'stepup://wallet');
  end if;
end $$;
revoke all on function economy.security_notice(uuid, text, jsonb) from public;

/*
 * 어테스터 — 지갑 서명을 검증한 뒤 부른다.
 * 처음 붙이는 지갑이면 보너스 뽑기 10회(Genesis 1)를 준다.
 */
create or replace function public.attester_wallet_link(p_user uuid, p_address text, p_nonce text)
returns void
language plpgsql security definer set search_path = public, economy as $$
declare
  v_addr text := lower(p_address);
  v_first boolean;
begin
  if v_addr !~ '^0x[0-9a-f]{40}$' then
    raise exception '지갑 주소가 올바르지 않습니다' using errcode = '22023';
  end if;
  if not exists (select 1 from public.wallet_link_nonces
                  where user_id = p_user and nonce = p_nonce
                    and created_at > now() - interval '10 minutes') then
    raise exception '확인 번호가 맞지 않거나 만료되었습니다' using errcode = '22023';
  end if;
  delete from public.wallet_link_nonces where user_id = p_user;

  perform pg_advisory_xact_lock(hashtext('ledger:' || p_user::text));

  if exists (select 1 from public.wallet_history
              where address = v_addr and user_id is distinct from p_user) then
    raise exception '다른 계정에 연결된 적 있는 지갑입니다' using errcode = '23505';
  end if;

  v_first := not exists (select 1 from public.wallet_history where user_id = p_user)
             and not exists (select 1 from public.wallet_history where address = v_addr);

  insert into public.wallet_history (address, user_id) values (v_addr, p_user)
  on conflict (address) do nothing;

  insert into public.wallet_links (user_id, address) values (p_user, v_addr)
  on conflict (user_id) do update
    set address = excluded.address,
        changed_at = case when public.wallet_links.address = excluded.address
                          then public.wallet_links.changed_at else now() end;

  if v_first then
    insert into public.draw_grants (user_id, kind, granted, genesis_granted)
    values (p_user, 'BONUS', 10, 1)
    on conflict (user_id, kind) do nothing;
  end if;

  perform economy.security_notice(p_user, 'WALLET_LINKED',
    jsonb_build_object('address', left(v_addr, 6) || '…' || right(v_addr, 4)));
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 체인 작업
-- ══════════════════════════════════════════════════════════════════
-- 계정을 지워도 작업 기록은 남긴다(주인만 비운다). 정산 대조와 체인 이벤트 처리가
-- 이 기록에 기댄다.
create table if not exists public.chain_ops (
  id uuid primary key default gen_random_uuid(),
  user_id uuid references auth.users on delete set null,
  kind text not null check (kind in ('SUP_WITHDRAW', 'SNEAKER_WITHDRAW', 'BONUS_MINT')),
  status text not null default 'RESERVED'
    check (status in ('RESERVED', 'SIGNED', 'SUBMITTED', 'CONFIRMED', 'EXPIRED')),
  wallet text not null,
  amount numeric(20, 4),
  sneaker_id bigint references public.market_sneakers on delete set null,
  deadline timestamptz not null,
  tx_hash text,
  block_number bigint,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check ((kind = 'SUP_WITHDRAW') = (amount is not null))
);

create index if not exists chain_ops_user_recent on public.chain_ops (user_id, created_at desc);
create index if not exists chain_ops_open on public.chain_ops (status, deadline)
  where status in ('RESERVED', 'SIGNED', 'SUBMITTED');

alter table public.chain_ops enable row level security;
revoke all on public.chain_ops from anon, authenticated;
drop policy if exists chain_ops_select_own on public.chain_ops;
create policy chain_ops_select_own on public.chain_ops
  for select using ((select auth.uid()) = user_id);
grant select on public.chain_ops to authenticated;

-- 체인에서 본 이벤트. (거래, 로그 번호) 하나는 한 번만 처리한다.
create table if not exists public.chain_events (
  tx_hash text not null,
  log_index int not null,
  block_number bigint not null,
  kind text not null,
  data jsonb not null,
  processed_at timestamptz not null default now(),
  primary key (tx_hash, log_index)
);

alter table public.chain_events enable row level security;
revoke all on public.chain_events from anon, authenticated;

-- 꺼내기 공통 조건. 걸리면 이유와 함께 거절한다.
create or replace function economy.withdraw_gate(p_user uuid, p_need_mfa boolean default true)
returns text
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_wallet public.wallet_links;
  v_profile public.profiles;
begin
  if (economy.setting('chain_paused') #>> '{}')::boolean then
    raise exception '지금은 체인 작업을 잠시 멈췄습니다' using errcode = '55000';
  end if;

  select * into v_wallet from public.wallet_links where user_id = p_user;
  if not found then
    raise exception '먼저 지갑을 연결해 주세요' using errcode = '23514';
  end if;

  if p_need_mfa then
    if not economy.mfa_ok() then
      raise exception '2단계 인증이 필요합니다' using errcode = '42501';
    end if;
    if v_wallet.changed_at > now() - make_interval(hours => economy.setting_num('wallet_cooldown_hours')::int) then
      raise exception '지갑을 연결하거나 바꾼 뒤 %시간은 꺼낼 수 없습니다',
        economy.setting_num('wallet_cooldown_hours') using errcode = '23514';
    end if;
    select * into v_profile from public.profiles where id = p_user;
    if v_profile.created_at > now() - make_interval(days => economy.setting_num('withdraw_min_account_days')::int) then
      raise exception '가입 %일이 지나야 꺼낼 수 있습니다',
        economy.setting_num('withdraw_min_account_days') using errcode = '23514';
    end if;
    if v_profile.gps_km < economy.setting_num('withdraw_min_km') then
      raise exception '누적 %km 를 달려야 꺼낼 수 있습니다',
        economy.setting_num('withdraw_min_km') using errcode = '23514';
    end if;
  end if;

  return v_wallet.address;
end $$;
revoke all on function economy.withdraw_gate(uuid, boolean) from public;

create or replace function economy.today_start() returns timestamptz
  language sql stable as $$ select economy.game_day_start(economy.game_day(now())) $$;

-- 오늘(한국) 꺼낸 SUP — 만료된 것은 빼고
create or replace function economy.withdrawn_today(p_user uuid default null) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select coalesce(sum(o.amount), 0) from public.chain_ops o
   where o.kind = 'SUP_WITHDRAW' and o.status <> 'EXPIRED'
     and o.created_at >= economy.today_start()
     and (p_user is null or o.user_id = p_user)
$$;
revoke all on function economy.withdrawn_today(uuid) from public;

-- 오늘 발행 수. 보너스 발행과 꺼내기 발행은 한도를 따로 센다 — 계정을 많이 만들어
-- 보너스로 하루 한도를 채워 정직한 사람의 꺼내기를 막지 못하게.
create or replace function economy.mints_today(p_kind text) returns int
language sql stable security definer set search_path = public, economy as $$
  select count(*)::int from public.chain_ops o
   where o.kind = p_kind and o.status <> 'EXPIRED'
     and o.created_at >= economy.today_start()
$$;
revoke all on function economy.mints_today(text) from public;

insert into public.economy_settings (key, value) values ('bonus_mint_global_daily', '2000'::jsonb)
on conflict (key) do nothing;

create or replace function economy.op_deadline() returns timestamptz
  language sql stable as $$
  select now() + make_interval(secs => economy.setting_num('op_deadline_sec')::int)
$$;

-- ── SUP 꺼내기 ──
create or replace function public.sup_withdraw_request(p_amount numeric)
returns uuid
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_wallet text;
  v_amount numeric := round(p_amount, 4);
  v_op uuid := gen_random_uuid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if v_amount is null or v_amount < 1 then
    raise exception '1 SUP 이상부터 꺼낼 수 있습니다' using errcode = '22023';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  -- 전체 상한은 모두가 함께 쓰므로 전체 잠금 아래에서 센다
  perform pg_advisory_xact_lock(hashtext('chain:withdraw'));
  v_wallet := economy.withdraw_gate(v_user, true);

  if v_amount > economy.setting_num('withdraw_user_daily_sup') - economy.withdrawn_today(v_user) then
    raise exception '오늘 꺼낼 수 있는 양을 넘었습니다' using errcode = '23514';
  end if;
  if v_amount > economy.setting_num('withdraw_global_daily_sup') - economy.withdrawn_today(null) then
    raise exception '오늘 전체 꺼내기 한도가 찼습니다. 내일 다시 해 주세요' using errcode = '23514';
  end if;

  -- 예약하는 순간 잔고에서 뺀다. 서명을 기다리는 동안 같은 돈을 앱에서 쓰지 못하게.
  perform economy.ledger_apply(v_user, 'CHAIN_WITHDRAW', -v_amount, '체인으로 꺼내기', 'op:' || v_op);

  insert into public.chain_ops (id, user_id, kind, wallet, amount, deadline)
  values (v_op, v_user, 'SUP_WITHDRAW', v_wallet, v_amount, economy.op_deadline());
  return v_op;
end $$;

-- ── 신발 꺼내기 ──
create or replace function public.sneaker_withdraw_request(p_sneaker_id bigint)
returns uuid
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_wallet text;
  v public.market_sneakers;
  v_op uuid := gen_random_uuid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform pg_advisory_xact_lock(hashtext('chain:withdraw'));
  v_wallet := economy.withdraw_gate(v_user, true);

  v := economy.my_app_sneaker(v_user, p_sneaker_id);
  if not v.withdrawable or v.origin in ('IMPORT', 'MINT', 'STARTER') then
    raise exception '꺼낼 수 없는 신발입니다' using errcode = '22023';
  end if;
  if v.km_run < v.lock_km then
    raise exception '이 신발로 %km 를 더 달려야 꺼낼 수 있습니다', round(v.lock_km - v.km_run, 1)
      using errcode = '23514';
  end if;
  if v.token_id is null and economy.mints_today('SNEAKER_WITHDRAW') >= economy.setting_num('mint_global_daily') then
    raise exception '오늘 발행 한도가 찼습니다. 내일 다시 해 주세요' using errcode = '23514';
  end if;

  update public.market_sneakers
     set chain_state = 'WITHDRAWING', equipped = false, updated_at = now()
   where id = p_sneaker_id;

  insert into public.chain_ops (id, user_id, kind, wallet, sneaker_id, deadline)
  values (v_op, v_user, 'SNEAKER_WITHDRAW', v_wallet, p_sneaker_id, economy.op_deadline());
  return v_op;
end $$;

-- ── 지갑 보너스 뽑기 (체인에서 발행) ──
-- 첫 번째가 Genesis(희귀 이상 확정)다. 꺼내기가 아니라 받는 것이라 2단계 인증과
-- 72시간 대기는 묻지 않는다 — 받은 NFT 는 50km 를 달리기 전에는 옮길 수 없다.
create or replace function public.bonus_draw_request()
returns uuid
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_wallet text;
  v_genesis boolean;
  v_id bigint;
  v_op uuid := gen_random_uuid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform pg_advisory_xact_lock(hashtext('chain:withdraw'));
  v_wallet := economy.withdraw_gate(v_user, false);

  if economy.mints_today('BONUS_MINT') >= economy.setting_num('bonus_mint_global_daily') then
    raise exception '오늘 발행 한도가 찼습니다. 내일 다시 해 주세요' using errcode = '23514';
  end if;

  select g.genesis_used < g.genesis_granted into v_genesis
    from public.draw_grants g where g.user_id = v_user and g.kind = 'BONUS';

  update public.draw_grants
     set used = used + 1,
         genesis_used = genesis_used + case when v_genesis then 1 else 0 end
   where user_id = v_user and kind = 'BONUS' and used < granted;
  if not found then
    raise exception '보너스 뽑기가 남아 있지 않습니다' using errcode = '23514';
  end if;

  v_id := economy.draw_create(v_user, 'BONUS_DRAW',
                              case when v_genesis then 'EPIC' end, coalesce(v_genesis, false));
  update public.market_sneakers set chain_state = 'WITHDRAWING' where id = v_id;

  insert into public.chain_ops (id, user_id, kind, wallet, sneaker_id, deadline)
  values (v_op, v_user, 'BONUS_MINT', v_wallet, v_id, economy.op_deadline());
  return v_op;
end $$;

revoke all on function public.sup_withdraw_request(numeric) from public, anon;
revoke all on function public.sneaker_withdraw_request(bigint) from public, anon;
revoke all on function public.bonus_draw_request() from public, anon;
grant execute on function public.sup_withdraw_request(numeric) to authenticated;
grant execute on function public.sneaker_withdraw_request(bigint) to authenticated;
grant execute on function public.bonus_draw_request() to authenticated;

-- 내 지갑 · 꺼내기 상태
create or replace function public.my_wallet()
returns table (
  address text,
  linked_at timestamptz,
  withdraw_open_at timestamptz,
  mfa boolean,
  withdrawn_today numeric,
  withdraw_daily_limit numeric,
  bonus_left int,
  genesis_left int,
  paused boolean
)
language plpgsql stable security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return query
  select w.address, w.linked_at,
         w.changed_at + make_interval(hours => economy.setting_num('wallet_cooldown_hours')::int),
         economy.mfa_ok(),
         economy.withdrawn_today(v_user),
         economy.setting_num('withdraw_user_daily_sup'),
         coalesce((select g.granted - g.used from public.draw_grants g
                    where g.user_id = v_user and g.kind = 'BONUS'), 0),
         coalesce((select g.genesis_granted - g.genesis_used from public.draw_grants g
                    where g.user_id = v_user and g.kind = 'BONUS'), 0),
         (economy.setting('chain_paused') #>> '{}')::boolean
    from (select 1) one
    left join public.wallet_links w on w.user_id = v_user;
end $$;

revoke all on function public.my_wallet() from public, anon;
grant execute on function public.my_wallet() to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 어테스터 함수
-- ══════════════════════════════════════════════════════════════════

/*
 * 서명 재료. RESERVED(또는 이미 SIGNED)이고 유효 시간이 남은 작업만 준다.
 * 같은 작업을 다시 물어도 같은 재료가 나온다 — 체인은 작업 번호로 한 번만 받는다.
 */
create or replace function public.attester_op_payload(p_op uuid)
returns table (
  op_id uuid,
  op_ref text,
  kind text,
  wallet text,
  account_ref text,
  amount numeric,
  deadline_unix bigint,
  run_day date,
  sneaker_id bigint,
  token_id numeric,
  faction text,
  rarity text,
  variant int,
  level int,
  efficiency_bps int,
  comfort_bps int,
  durability numeric,
  genesis_no int,
  transfer_locked boolean
)
language plpgsql security definer set search_path = public, economy as $$
declare v public.chain_ops;
begin
  select * into v from public.chain_ops o where o.id = p_op for update;
  if not found then
    raise exception '없는 작업입니다' using errcode = '22023';
  end if;
  if v.status not in ('RESERVED', 'SIGNED') or v.deadline <= now() then
    raise exception '서명할 수 없는 작업입니다 (%)', v.status using errcode = '22023';
  end if;
  if (economy.setting('chain_paused') #>> '{}')::boolean then
    raise exception '지금은 체인 작업을 잠시 멈췄습니다' using errcode = '55000';
  end if;

  update public.chain_ops o set status = 'SIGNED', updated_at = now() where o.id = p_op;

  return query
  select v.id, economy.op_ref(v.id), v.kind, v.wallet, economy.account_ref(v.user_id),
         v.amount, extract(epoch from v.deadline)::bigint, economy.game_day(v.created_at),
         s.id, s.token_id, s.faction, s.rarity, s.variant, s.level,
         s.efficiency_bps, s.comfort_bps, s.durability_pts, s.genesis_no,
         -- 무료로 받은 신발은 잠금 거리를 채우기 전에는 체인에서도 못 옮긴다
         coalesce(s.km_run < s.lock_km, false)
    from (select 1) one
    left join public.market_sneakers s on s.id = v.sneaker_id;
end $$;

create or replace function public.attester_op_submitted(p_op uuid, p_tx text)
returns void
language plpgsql security definer set search_path = public as $$
begin
  if p_tx !~ '^0x[0-9a-fA-F]{64}$' then
    raise exception '거래 해시가 올바르지 않습니다' using errcode = '22023';
  end if;
  update public.chain_ops set status = 'SUBMITTED', tx_hash = lower(p_tx), updated_at = now()
   where id = p_op and status in ('SIGNED', 'SUBMITTED');
end $$;

-- 작업을 확정한다. 이미 확정이면 아무것도 안 한다.
create or replace function economy.op_confirm(p_op uuid, p_tx text, p_block bigint, p_token numeric)
returns void language plpgsql security definer set search_path = public, economy as $$
declare v public.chain_ops;
begin
  select * into v from public.chain_ops where id = p_op for update;
  if not found or v.status = 'CONFIRMED' then
    return;
  end if;
  if v.status = 'EXPIRED' then
    -- 되돌린 뒤에 체인에서 성공했다 — 일어나면 안 되는 일(만료 마진 · 체인 확인이
    -- 막는다). 두 번 받는 것을 막기 위해 되돌린 것을 다시 거두고 경보를 남긴다.
    -- 체인 작업을 모두 멈추고 사람이 본다. 되돌린 금액은 거둘 수 있으면 거둔다.
    update public.economy_settings set value = 'true'::jsonb, updated_at = now() where key = 'chain_paused';
    if v.kind = 'SUP_WITHDRAW' and v.user_id is not null then
      begin
        perform economy.ledger_apply(v.user_id, 'CHAIN_WITHDRAW', -v.amount,
          '만료 뒤 체인 확정 — 되돌린 금액 회수', 'reclaim:' || v.id);
      exception when others then
        null;  -- 잔고가 모자라 못 거뒀다. 체인이 멈춰 있으니 사람이 정리한다.
      end;
    end if;
    perform public.admin_log('chain_late_confirm', v.id::text, jsonb_build_object('tx', p_tx));
  end if;

  update public.chain_ops
     set status = 'CONFIRMED', tx_hash = coalesce(lower(p_tx), tx_hash), block_number = p_block, updated_at = now()
   where id = p_op;

  -- 꺼내는 중인 신발만 체인으로 옮긴다. 이미 다시 넣어 앱에 돌아온 신발을 늦게 온
  -- 이벤트가 체인으로 되돌려 놓지 않게. (어테스터는 이벤트를 블록·로그 순서대로 넘긴다.)
  if v.kind in ('SNEAKER_WITHDRAW', 'BONUS_MINT') then
    update public.market_sneakers
       set chain_state = 'ON_CHAIN', equipped = false,
           token_id = coalesce(p_token, token_id), updated_at = now()
     where id = v.sneaker_id and chain_state = 'WITHDRAWING';
  end if;
end $$;
revoke all on function economy.op_confirm(uuid, text, bigint, numeric) from public;

/*
 * 확정된 체인 이벤트 하나. 어테스터가 확정 블록까지 기다린 뒤, 블록 번호 · 로그 번호
 * 순서대로 부른다(같은 신발의 꺼내기 이벤트가 넣기 이벤트보다 먼저 와야 한다).
 *
 *   SUP_CLAIMED        {"op": opRef}
 *   SNEAKER_RELEASED   {"op": opRef, "tokenId": "123"}
 *   SUP_DEPOSITED      {"account": accountRef, "amount": "12.5"}
 *   SNEAKER_DEPOSITED  {"account": accountRef, "tokenId": "123", "from": "0x…"}
 */
create or replace function public.attester_chain_event(
  p_tx text, p_log int, p_block bigint, p_kind text, p_data jsonb
) returns text
language plpgsql security definer set search_path = public, economy as $$
declare
  v_tx text := lower(p_tx);
  v_op uuid;
  v_user uuid;
  v_token numeric;
  v_amount numeric;
begin
  insert into public.chain_events (tx_hash, log_index, block_number, kind, data)
  values (v_tx, p_log, p_block, p_kind, p_data)
  on conflict do nothing;
  if not found then
    return 'DUPLICATE';
  end if;

  if p_kind in ('SUP_CLAIMED', 'SNEAKER_RELEASED') then
    v_op := economy.account_from_ref(p_data ->> 'op');
    v_token := nullif(p_data ->> 'tokenId', '')::numeric;
    if v_op is null or not exists (
         select 1 from public.chain_ops o
          where o.id = v_op
            and o.kind = any (case p_kind when 'SUP_CLAIMED' then array['SUP_WITHDRAW']
                                          else array['SNEAKER_WITHDRAW', 'BONUS_MINT'] end)) then
      perform public.admin_log('chain_unknown_op', v_tx, p_data);
      return 'UNKNOWN_OP';
    end if;
    perform economy.op_confirm(v_op, v_tx, p_block, v_token);
    return 'CONFIRMED';

  elsif p_kind = 'SUP_DEPOSITED' then
    v_user := economy.account_from_ref(p_data ->> 'account');
    v_amount := round((p_data ->> 'amount')::numeric, 4);
    if v_user is null or not exists (select 1 from auth.users where id = v_user) or v_amount <= 0 then
      -- 받을 계정이 없는 입금. 토큰은 금고에 있으므로 사람이 보고 돌려준다.
      perform public.admin_log('chain_orphan_deposit', v_tx, p_data);
      return 'ORPHAN';
    end if;
    perform economy.ledger_apply(v_user, 'CHAIN_DEPOSIT', v_amount, '체인에서 넣기',
                                 'dep:' || v_tx || ':' || p_log);
    return 'CREDITED';

  elsif p_kind = 'SNEAKER_DEPOSITED' then
    v_user := economy.account_from_ref(p_data ->> 'account');
    v_token := (p_data ->> 'tokenId')::numeric;
    if v_user is null or not exists (select 1 from auth.users where id = v_user)
       or not exists (select 1 from public.market_sneakers where token_id = v_token and chain_state = 'ON_CHAIN') then
      perform public.admin_log('chain_orphan_deposit', v_tx, p_data);
      return 'ORPHAN';
    end if;
    -- 잠금 거리를 못 채운(무료) 신발은 넣은 지갑이 그 계정에 붙은 지갑일 때만 받는다.
    -- 무료 신발을 다른 계정으로 옮겨 몰아주지 못하게.
    if exists (select 1 from public.market_sneakers where token_id = v_token and km_run < lock_km)
       and not exists (select 1 from public.wallet_links w
                        where w.user_id = v_user and w.address = lower(p_data ->> 'from')) then
      perform public.admin_log('chain_locked_deposit_mismatch', v_tx, p_data);
      return 'ORPHAN';
    end if;
    -- 넣은 사람이 새 주인이다(체인에서 샀을 수 있다). 스탯은 꺼낼 때 서버가 적은 값 그대로다.
    -- 주인 바꾸기 트리거는 이 표시가 있을 때만 체인 → 앱 이전을 허락한다.
    perform set_config('stepup.chain_deposit', 'on', true);
    update public.market_sneakers
       set owner_id = v_user, chain_state = 'APP', status = 'OWNED', equipped = false, updated_at = now()
     where token_id = v_token and chain_state = 'ON_CHAIN';
    perform set_config('stepup.chain_deposit', 'off', true);
    return 'CREDITED';
  end if;

  perform public.admin_log('chain_unknown_event', v_tx, jsonb_build_object('kind', p_kind, 'data', p_data));
  return 'IGNORED';
end $$;

-- 되돌릴 차례가 된 작업 — 서명 유효 시간 + 안전 마진이 지난 것
create or replace function public.attester_due_ops()
returns table (op_id uuid, op_ref text, status text, kind text, deadline timestamptz, tx_hash text)
language sql stable security definer set search_path = public, economy as $$
  select o.id, economy.op_ref(o.id), o.status, o.kind, o.deadline, o.tx_hash
    from public.chain_ops o
   where o.status in ('RESERVED', 'SIGNED', 'SUBMITTED')
     and o.deadline + make_interval(secs => economy.setting_num('op_expire_margin_sec')::int) < now()
   order by o.deadline
   limit 200
$$;

/*
 * 만료. 어테스터가 체인에서 이 작업 번호가 쓰였는지 확인하고 부른다.
 *   p_used_on_chain = true   → 이벤트를 놓친 것. 확정으로 처리한다.
 *   p_used_on_chain = false  → 되돌린다(잔고 환불 · 신발 앱으로).
 * 서명한 적 없는 작업(RESERVED)은 체인에 있을 수 없으므로 마진 없이도 되돌린다.
 */
create or replace function public.attester_op_expire(p_op uuid, p_used_on_chain boolean, p_token numeric default null)
returns text
language plpgsql security definer set search_path = public, economy as $$
declare v public.chain_ops;
begin
  select * into v from public.chain_ops where id = p_op for update;
  if not found then
    raise exception '없는 작업입니다' using errcode = '22023';
  end if;
  if v.status in ('CONFIRMED', 'EXPIRED') then
    return v.status;
  end if;
  if v.status <> 'RESERVED'
     and v.deadline + make_interval(secs => economy.setting_num('op_expire_margin_sec')::int) >= now() then
    raise exception '아직 만료 마진이 지나지 않았습니다' using errcode = '55000';
  end if;
  if v.status = 'RESERVED' and v.deadline >= now() then
    raise exception '아직 유효한 작업입니다' using errcode = '55000';
  end if;

  if p_used_on_chain then
    perform economy.op_confirm(p_op, v.tx_hash, v.block_number, p_token);
    return 'CONFIRMED';
  end if;

  update public.chain_ops set status = 'EXPIRED', updated_at = now() where id = p_op;

  if v.kind = 'SUP_WITHDRAW' then
    perform economy.ledger_apply(v.user_id, 'CHAIN_REFUND', v.amount, '꺼내기 만료 — 되돌림', 'refund:' || v.id);
  else
    -- 신발은 앱으로 돌아온다. 보너스 뽑기로 만든 신발도 사라지지 않고 앱에 남는다.
    update public.market_sneakers
       set chain_state = 'APP', updated_at = now()
     where id = v.sneaker_id and chain_state = 'WITHDRAWING';
  end if;
  return 'EXPIRED';
end $$;

-- 이상 징후를 본 어테스터가 스스로 멈춘다. 다시 켜는 것은 관리자만(admin_economy_set).
create or replace function public.attester_pause(p_reason text)
returns void
language plpgsql security definer set search_path = public as $$
begin
  update public.economy_settings set value = 'true'::jsonb, updated_at = now() where key = 'chain_paused';
  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'chain_pause', 'attester', jsonb_build_object('reason', p_reason));
end $$;

-- 정산 대조 — 서버 장부의 합. 어테스터가 체인의 발행·입금 합과 비교한다.
create or replace function public.attester_ledger_totals()
returns table (
  sup_withdrawn_confirmed numeric,
  sup_withdraw_pending numeric,
  sup_deposited numeric,
  sneakers_on_chain bigint,
  sneakers_pending bigint
)
language sql stable security definer set search_path = public as $$
  select
    coalesce((select sum(amount) from public.chain_ops where kind = 'SUP_WITHDRAW' and status = 'CONFIRMED'), 0),
    coalesce((select sum(amount) from public.chain_ops
               where kind = 'SUP_WITHDRAW' and status in ('RESERVED', 'SIGNED', 'SUBMITTED')), 0),
    -- 원장이 아니라 체인 이벤트로 센다. 계정을 지우면 원장 줄은 사라진다.
    coalesce((select sum((data ->> 'amount')::numeric) from public.chain_events where kind = 'SUP_DEPOSITED'), 0),
    (select count(*) from public.market_sneakers where chain_state = 'ON_CHAIN'),
    (select count(*) from public.market_sneakers where chain_state in ('WITHDRAWING', 'DEPOSITING'))
$$;

-- 어테스터 함수는 어테스터 역할만. 앱 권한(anon·authenticated)으로는 부를 수 없다.
do $$
declare r record;
begin
  for r in
    select p.oid::regprocedure as sig from pg_proc p
     where p.pronamespace = 'public'::regnamespace and p.proname like 'attester\_%'
  loop
    execute format('revoke all on function %s from public, anon, authenticated', r.sig);
    execute format('grant execute on function %s to stepup_attester', r.sig);
  end loop;
end $$;

commit;

-- ════════════════════════════════════════════════════════════════════
--  끝났습니다. 아래로 확인할 수 있습니다.
-- ════════════════════════════════════════════════════════════════════
select table_name as "만들어진 표"
  from information_schema.tables
 where table_schema = 'public' and table_type = 'BASE TABLE'
 order by table_name;
