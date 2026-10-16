-- ════════════════════════════════════════════════════════════════════
--  0059 — AI 코칭 (2026-10-16 전달본 · docs/redesign/ai-coaching-2026-10)
--
--  숫자는 코드가 만든다. 이 파일이 서버에 저장된 러닝으로 근거(fact)와 허용 후보(candidate)를 숫자와 함께 만들고,
--  AI 는 그중 id 하나씩을 고르고 짧은 해석 · 이유만 쓴다. 공급자에게 보내는 입력(coaching_jobs.input)에는 좌표 · 이름 ·
--  연락처 · 크루 · 보상 · 메모가 없다 — 거리 · 시간 · 구간 초 · 고른 목표(enum)뿐이다.
--
--  · 앱은 러닝을 "시작 시각"으로 가리킨다. 서버가 토큰의 사용자와 시작 시각으로 자기 표에서 찾는다 — 앱의 로컬 번호 ·
--    사용자 번호 · 계산한 숫자를 믿지 않는다. 남의 러닝은 찾을 수 없다.
--  · 거리는 걸음 거리(걸음 × 0.762m — 앱의 기록 거리 · 랩과 같은 기준)다. walk_sessions.distance_meters 는 경로 거리가
--    섞여 앱 화면의 숫자와 다르다. 판정(CLEAN · FLAGGED · VOID)과 보상은 읽기만 한다 — VOID 는 러닝이 아니므로 건너뛴다.
--  · 작업은 표에 남는 큐다(떠 있는 Promise 가 아니다). 키(사용자 · 종류 · 대상 · 언어 · 입력 지문 · 정책 · 프롬프트 판)가
--    같으면 같은 작업이다 — 다시 눌러도 · 다시 들어와도 새로 만들지 않고 한도도 다시 깎지 않는다.
--  · 워커는 보내기 직전에 동의 · 예산을 다시 보고, 결과는 형식뿐 아니라 id 허용 목록 · 숫자 · 길이 · 언어 · 금지 문구를
--    검사한 뒤에만 저장한다. 공급자는 정해지기 전까지 none(만들지 않음)이다 — 모의(mock)는 시험용이다.
--
--  운영 값(economy_settings)은 처음에 모두 "정하지 않음"이다. 정하지 않은 값으로는 새 분석을 만들지 않는다(닫힌 쪽).
-- ════════════════════════════════════════════════════════════════════

create schema if not exists coaching;

insert into public.economy_settings (key, value) values
  -- 새 AI 분석을 만드는가. 꺼도 이미 저장된 결과는 읽는다(긴급 중지)
  ('coaching_enabled', 'false'::jsonb),
  -- none(만들지 않음) · mock(시험용 고정 문장) · anthropic(모델 · 계약 · 예산이 정해진 뒤에만 — 워커가 아직 부르지 않는다)
  ('coaching_provider', '"none"'::jsonb),
  ('coaching_model', 'null'::jsonb),
  -- 사용자 한 명의 하루(한국 시각) 새 분석 수. null = 정하지 않음 → 만들지 않는다
  ('coaching_user_daily_limit', 'null'::jsonb),
  -- 프로젝트 한 달 원가 상한(백만분의 1 달러)과 호출 하나에 미리 잡을 값. null = 실제 공급자를 부르지 않는다
  ('coaching_monthly_budget_micro', 'null'::jsonb),
  ('coaching_reserve_micro', 'null'::jsonb),
  ('coaching_max_attempts', '3'::jsonb),
  -- 동의 문구의 판 — 문구가 바뀌면 판을 올리고, 옛 판의 동의로는 보내지 않는다
  ('coaching_consent_version', '"draft-2026-10-16"'::jsonb),
  ('coaching_policy_version', '"policy-v1"'::jsonb),
  ('coaching_prompt_version', '"prompt-v1"'::jsonb),
  -- 코칭 워커 계정(어테스터와 따로 — 서명 키와 AI 키를 한 상자에 두지 않는다)
  ('coaching_worker_user_id', 'null'::jsonb)
on conflict (key) do nothing;

-- ══════════════════════════════════════════════════════════════════
-- 규칙 값 — 앱(domain/coaching)과 같은 값. scripts/check-coaching-twin.py 가 맞대어 본다
-- ══════════════════════════════════════════════════════════════════

-- RewardEconomy.STRIDE_METERS
create or replace function coaching.stride_m() returns numeric language sql immutable as $$ select 0.762::numeric $$;
-- CoachingQuality.MIN_SPLIT_SEC · MAX_SPLIT_SEC · MIN_PARTIAL_METERS · EVEN_SPLIT_SEC
create or replace function coaching.min_split_sec() returns int language sql immutable as $$ select 150 $$;
create or replace function coaching.max_split_sec() returns int language sql immutable as $$ select 3600 $$;
create or replace function coaching.min_partial_m() returns numeric language sql immutable as $$ select 10::numeric $$;
create or replace function coaching.even_split_sec() returns int language sql immutable as $$ select 10 $$;
-- 결과 형식의 판 — 워커가 같은 판으로 답해야 저장한다
create or replace function coaching.schema_version() returns text language sql immutable as $$ select 'coach-out-v1' $$;

-- 훈련 템플릿(CoachingTemplates — 제품 검토 전 초안). AI 는 여기 있는 id 만 고를 수 있다
create or replace function coaching.templates()
returns table (id text, family text, minutes int, steps jsonb)
language sql immutable as $$
  values
    ('easy_15', 'easy', 15, '[{"role":"WARMUP","minutes":3},{"role":"EASY_RUN","minutes":9},{"role":"COOLDOWN","minutes":3}]'::jsonb),
    ('easy_25', 'easy', 25, '[{"role":"WARMUP","minutes":5},{"role":"EASY_RUN","minutes":15},{"role":"COOLDOWN","minutes":5}]'::jsonb),
    ('easy_40', 'easy', 40, '[{"role":"WARMUP","minutes":5},{"role":"EASY_RUN","minutes":30},{"role":"COOLDOWN","minutes":5}]'::jsonb),
    ('walk_run_15', 'walk_run', 15, '[{"role":"WARMUP","minutes":3},{"role":"RUN_WALK","minutes":9,"run":2,"walk":1,"rounds":3},{"role":"COOLDOWN","minutes":3}]'::jsonb),
    ('walk_run_25', 'walk_run', 25, '[{"role":"WARMUP","minutes":5},{"role":"RUN_WALK","minutes":15,"run":2,"walk":1,"rounds":5},{"role":"COOLDOWN","minutes":5}]'::jsonb),
    ('walk_run_40', 'walk_run', 40, '[{"role":"WARMUP","minutes":5},{"role":"RUN_WALK","minutes":30,"run":4,"walk":1,"rounds":6},{"role":"COOLDOWN","minutes":5}]'::jsonb)
$$;

-- 다음 주 계획 후보(weekPlan) — 2회 월 · 목, 3회 월 · 수 · 금, 4회 월 · 화 · 목 · 토. 편안한 러닝과 걷기와 달리기를 번갈아
create or replace function coaching.week_plan(p_monday date, p_runs int, p_minutes int) returns jsonb
language sql immutable as $$
  select coalesce(jsonb_agg(jsonb_build_object(
           'date', (p_monday + d.off)::text,
           'template', case when d.n % 2 = 1 then 'easy_' else 'walk_run_' end || p_minutes)
           order by d.n), '[]'::jsonb)
    from unnest(case p_runs when 2 then array[0, 3] when 3 then array[0, 2, 4] else array[0, 1, 3, 5] end)
         with ordinality as d(off, n)
$$;

-- ══════════════════════════════════════════════════════════════════
-- 표
-- ══════════════════════════════════════════════════════════════════

-- AI 이용 동의 — 판 · 시각 · 범위를 남기고, 해제해도 줄을 지우지 않는다(언제 동의했고 언제 거뒀는지)
create table if not exists public.coaching_consents (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,
  version text not null,
  -- 기능 이용 동의(기록 요약 전송) — 자동 분석 · 주간 리포트는 선호의 토글로 따로 켠다
  scope text not null default 'analysis' check (scope in ('analysis')),
  granted_at timestamptz not null default now(),
  withdrawn_at timestamptz,
  -- 해제할 때 "저장된 AI 분석도 삭제"를 골랐는가
  delete_requested boolean not null default false
);
create unique index if not exists coaching_consents_one_active
  on public.coaching_consents (user_id) where withdrawn_at is null;

-- 코칭 선호(06 · 14) — 고른 것만. revision 으로 두 기기의 엇갈린 저장을 가린다
create table if not exists public.coaching_preferences (
  user_id uuid primary key references auth.users on delete cascade,
  goal text check (goal in ('steady', 'finish_5k', 'improve')),
  runs_per_week int check (runs_per_week in (2, 3, 4)),
  minutes int check (minutes in (15, 25, 40)),
  auto_analysis boolean not null default false,
  weekly_report boolean not null default false,
  -- 사용자가 확인하고 적용한 목표 페이스(16). 러닝을 시작하지 않는다
  target_pace_sec int check (target_pace_sec between 150 and 3600),
  target_pace_result bigint,
  revision bigint not null default 1,
  updated_at timestamptz not null default now()
);

-- 적용한 주간 계획(13) — 제안이 아니라 사용자가 "적용"한 것만
create table if not exists public.coaching_plans (
  user_id uuid not null references auth.users on delete cascade,
  week_start date not null check (extract(isodow from week_start) = 1),
  days jsonb not null,
  prefs_revision bigint not null,
  applied_at timestamptz not null default now(),
  primary key (user_id, week_start)
);

-- 서버가 확인한 1km 구간(누적 활성 초) — 앱이 잰 랩을 서버의 러닝과 맞대어 보고 받은 것
create table if not exists public.coaching_run_splits (
  session_id bigint primary key references public.walk_sessions on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  laps int[] not null,
  created_at timestamptz not null default now()
);

-- 작업 큐 — 기록 하나 · 한 주 · 다음 훈련 추천은 서로 다른 종류다
create table if not exists public.coaching_jobs (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,
  kind text not null check (kind in ('run', 'weekly', 'recommendation')),
  source_key text not null,
  session_id bigint references public.walk_sessions on delete cascade,
  locale text not null check (locale in ('ko', 'en', 'ja', 'zh')),
  fingerprint text not null,
  policy_version text not null,
  prompt_version text not null,
  -- 공급자에게 보낼 입력 그대로 — 무엇을 보냈는지 밖에서 읽을 수 있게
  input jsonb not null,
  status text not null default 'QUEUED' check (status in ('QUEUED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
  attempts int not null default 0,
  max_attempts int not null check (max_attempts between 1 and 10),
  lease_until timestamptz,
  next_at timestamptz not null default now(),
  error_code text,
  reserved_micro bigint not null default 0 check (reserved_micro >= 0),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create unique index if not exists coaching_jobs_key on public.coaching_jobs
  (user_id, kind, source_key, locale, fingerprint, policy_version, prompt_version);
create index if not exists coaching_jobs_queue on public.coaching_jobs (status, next_at) where status in ('QUEUED', 'RUNNING');
create index if not exists coaching_jobs_user_day on public.coaching_jobs (user_id, created_at desc);

-- 결과 — 검사를 통과한 것만. 사용자는 읽기만 한다
create table if not exists public.coaching_results (
  id bigint generated always as identity primary key,
  job_id bigint not null unique references public.coaching_jobs on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  kind text not null,
  source_key text not null,
  session_id bigint references public.walk_sessions on delete cascade,
  locale text not null,
  fingerprint text not null,
  schema_version text not null,
  facts jsonb not null,
  candidates jsonb not null,
  fact_id text,
  explanation text,
  candidate_id text,
  action_reason text,
  limitation text,
  provider text not null,
  model text,
  created_at timestamptz not null default now()
);
create index if not exists coaching_results_source on public.coaching_results (user_id, kind, source_key, locale, created_at desc);

-- 원가 장부 — 시도마다(재시도 · 실패 · 잘못된 응답 포함). 사람 정보는 없고 계정이 지워져도 프로젝트 원가는 남는다
create table if not exists public.coaching_costs (
  id bigint generated always as identity primary key,
  job_id bigint references public.coaching_jobs on delete set null,
  attempt int not null,
  provider text not null,
  model text,
  input_tokens int not null default 0 check (input_tokens >= 0),
  output_tokens int not null default 0 check (output_tokens >= 0),
  cache_read_tokens int not null default 0 check (cache_read_tokens >= 0),
  cache_write_tokens int not null default 0 check (cache_write_tokens >= 0),
  cost_micro bigint not null default 0 check (cost_micro >= 0),
  outcome text not null,
  created_at timestamptz not null default now()
);
create index if not exists coaching_costs_month on public.coaching_costs (created_at);

-- ── 권한 ───────────────────────────────────────────────────────────
-- 사용자는 자기 동의 · 선호 · 계획 · 구간 · 결과를 읽기만 한다. 쓰기는 아래 함수로만. 큐 · 원가는 앱이 못 본다
alter table public.coaching_consents enable row level security;
alter table public.coaching_preferences enable row level security;
alter table public.coaching_plans enable row level security;
alter table public.coaching_run_splits enable row level security;
alter table public.coaching_jobs enable row level security;
alter table public.coaching_results enable row level security;
alter table public.coaching_costs enable row level security;

revoke insert, update, delete on public.coaching_consents, public.coaching_preferences, public.coaching_plans,
  public.coaching_run_splits, public.coaching_results from anon, authenticated;
revoke all on public.coaching_jobs, public.coaching_costs from anon, authenticated;

drop policy if exists coaching_consents_own on public.coaching_consents;
create policy coaching_consents_own on public.coaching_consents for select using ((select auth.uid()) = user_id);
drop policy if exists coaching_preferences_own on public.coaching_preferences;
create policy coaching_preferences_own on public.coaching_preferences for select using ((select auth.uid()) = user_id);
drop policy if exists coaching_plans_own on public.coaching_plans;
create policy coaching_plans_own on public.coaching_plans for select using ((select auth.uid()) = user_id);
drop policy if exists coaching_run_splits_own on public.coaching_run_splits;
create policy coaching_run_splits_own on public.coaching_run_splits for select using ((select auth.uid()) = user_id);
drop policy if exists coaching_results_own on public.coaching_results;
create policy coaching_results_own on public.coaching_results for select using ((select auth.uid()) = user_id);

-- ══════════════════════════════════════════════════════════════════
-- 안쪽 함수
-- ══════════════════════════════════════════════════════════════════

create or replace function coaching.setting_text(p_key text) returns text
  language sql stable security definer set search_path = public, economy as $$
  select economy.setting(p_key) #>> '{}'
$$;

-- 새 분석을 만들 수 있는가 — 켜져 있고, 공급자가 정해졌고, 사용자 하루 한도가 정해졌을 때만
create or replace function coaching.enabled() returns boolean
  language sql stable security definer set search_path = public, economy as $$
  select coalesce((economy.setting('coaching_enabled') #>> '{}')::boolean, false)
     and coalesce(economy.setting('coaching_provider') #>> '{}', 'none') in ('mock', 'anthropic')
     and economy.setting_num('coaching_user_daily_limit') is not null
$$;

create or replace function coaching.user_required() returns uuid
  language plpgsql stable as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return auth.uid();
end $$;

create or replace function coaching.locale_ok(p_locale text) returns text
  language plpgsql immutable as $$
begin
  if p_locale is null or p_locale not in ('ko', 'en', 'ja', 'zh') then
    raise exception '지원하지 않는 언어입니다' using errcode = '22023';
  end if;
  return p_locale;
end $$;

-- 사용자 시간대 — 모르는 이름은 받지 않는다(주 경계가 조용히 어긋나지 않게)
create or replace function coaching.tz_ok(p_tz text) returns text
  language plpgsql stable as $$
begin
  if p_tz is null or not exists (select 1 from pg_timezone_names where name = p_tz) then
    raise exception '알 수 없는 시간대입니다' using errcode = '22023';
  end if;
  return p_tz;
end $$;

-- 지금 살아 있는 동의 — 지금 판의 것만
create or replace function coaching.consented(p_user uuid) returns boolean
  language sql stable security definer set search_path = public, coaching as $$
  select exists (select 1 from public.coaching_consents c
                  where c.user_id = p_user and c.withdrawn_at is null
                    and c.version = coaching.setting_text('coaching_consent_version'))
$$;

-- 러닝 하나가 코칭의 러닝인가 — 무효가 아니고 걸음 · 시간이 있다
create or replace function coaching.counts(s public.walk_sessions) returns boolean
  language sql immutable as $$
  select s.verdict <> 'VOID' and s.steps > 0 and s.duration_sec > 0
$$;

-- 그 날짜가 든 주의 월요일 — 세션 시간대와 상관없이 날짜로만 센다
create or replace function coaching.monday(p_day date) returns date
  language sql immutable as $$ select p_day - (extract(isodow from p_day)::int - 1) $$;

-- 사용자 시간대의 오늘
create or replace function coaching.today(p_tz text) returns date
  language sql stable as $$ select (now() at time zone p_tz)::date $$;

-- 그 사용자 시간대의 [월요일 0시, 다음 월요일 0시)
create or replace function coaching.week_bounds(p_monday date, p_tz text, out v_from timestamptz, out v_until timestamptz)
  language sql immutable as $$
  select (p_monday::timestamp at time zone p_tz), ((p_monday + 7)::timestamp at time zone p_tz)
$$;

create or replace function coaching.week_totals(p_user uuid, p_monday date, p_tz text,
  out runs int, out meters numeric, out seconds bigint)
  language sql stable security definer set search_path = public, coaching as $$
  select count(*)::int, coalesce(sum(s.steps * coaching.stride_m()), 0), coalesce(sum(s.duration_sec), 0)::bigint
    from public.walk_sessions s, coaching.week_bounds(p_monday, p_tz) b
   where s.user_id = p_user and s.started_at >= b.v_from and s.started_at < b.v_until and coaching.counts(s)
$$;

-- [p_monday] 주부터 거슬러 러닝이 1회 이상인 주가 이어진 수(그 주 포함, 52주까지)
create or replace function coaching.week_streak(p_user uuid, p_monday date, p_tz text) returns int
  language plpgsql stable security definer set search_path = public, coaching as $$
declare v_n int := 0; v_week date := p_monday;
begin
  while v_n < 52 loop
    exit when (select t.runs from coaching.week_totals(p_user, v_week, p_tz) t) = 0;
    v_n := v_n + 1;
    v_week := v_week - 7;
  end loop;
  return v_n;
end $$;

create or replace function coaching.prefs_json(p public.coaching_preferences) returns jsonb
  language sql immutable as $$
  select case when p.user_id is null then null else jsonb_build_object(
    'goal', p.goal, 'runs_per_week', p.runs_per_week, 'minutes', p.minutes,
    'auto_analysis', p.auto_analysis, 'weekly_report', p.weekly_report,
    'target_pace_sec', p.target_pace_sec, 'revision', p.revision) end
$$;

-- 기록 하나의 입력 — 걸음 거리 · 활성 시간 · 1km 구간(초)과 근거 · 후보. 좌표 · 날짜 · 사람 정보 없음
create or replace function coaching.run_input(s public.walk_sessions, p_laps int[], p_locale text) returns jsonb
  language plpgsql immutable set search_path = public, coaching as $$
declare
  v_m numeric := s.steps * coaching.stride_m();
  v_sec int := s.duration_sec;
  v_pace int := round(s.duration_sec / (v_m / 1000))::int;
  v_n int := coalesce(cardinality(p_laps), 0);
  v_splits int[] := '{}';
  v_prev int := 0;
  v_facts jsonb := '[]'::jsonb;
  v_cands jsonb := '[]'::jsonb;
  v_delta int;
  v_spread int;
  v_target int := (round(round(s.duration_sec / (v_m / 1000)) / 5.0) * 5)::int;
  v_rest_m numeric;
  v_rest_s int;
begin
  for i in 1 .. v_n loop
    v_splits := v_splits || (p_laps[i] - v_prev);
    v_prev := p_laps[i];
  end loop;
  v_facts := v_facts || jsonb_build_object('id', 'run.avg_pace', 'values', jsonb_build_object('pace_sec', v_pace));
  if v_n >= 2 then
    v_delta := v_splits[v_n] - v_splits[1];
    select max(x) - min(x) into v_spread from unnest(v_splits) as x;
    if abs(v_delta) > coaching.even_split_sec() then
      v_facts := v_facts || jsonb_build_object('id', 'run.split_last_vs_first',
        'values', jsonb_build_object('last_km', v_n, 'delta_sec', v_delta));
    end if;
    if v_spread <= coaching.even_split_sec() then
      v_facts := v_facts || jsonb_build_object('id', 'run.split_even', 'values', jsonb_build_object('spread_sec', v_spread));
    end if;
    if v_delta > coaching.even_split_sec() then
      v_cands := v_cands || jsonb_build_object('id', 'pace.start_easier', 'values', jsonb_build_object('pace_sec', v_target));
    end if;
  end if;
  v_cands := v_cands || jsonb_build_object('id', 'pace.keep', 'values', jsonb_build_object('pace_sec', v_target));
  v_rest_m := v_m - v_n * 1000;
  v_rest_s := v_sec - v_prev;
  return jsonb_build_object(
    'kind', 'run', 'locale', p_locale, 'schema_version', coaching.schema_version(),
    'run', jsonb_build_object(
      'distance_m', round(v_m)::int, 'duration_sec', v_sec, 'avg_pace_sec', v_pace,
      'splits_sec', to_jsonb(v_splits),
      'partial', case when v_rest_m >= coaching.min_partial_m() and v_rest_s > 0
                      then jsonb_build_object('meters', round(v_rest_m)::int, 'seconds', v_rest_s) end),
    'facts', v_facts, 'candidates', v_cands);
end $$;

-- 한 주의 입력 — 그 주 합계 · 이어진 주 · 고른 목표. 다음 주 계획 후보는 선호가 다 있을 때만
create or replace function coaching.week_input(p_user uuid, p_monday date, p_tz text, p_locale text) returns jsonb
  language plpgsql stable security definer set search_path = public, coaching as $$
declare
  t record;
  p public.coaching_preferences;
  v_streak int := coaching.week_streak(p_user, p_monday, p_tz);
  v_facts jsonb := '[]'::jsonb;
  v_cands jsonb := '[]'::jsonb;
begin
  select * into t from coaching.week_totals(p_user, p_monday, p_tz);
  select * into p from public.coaching_preferences where user_id = p_user;
  v_facts := v_facts
    || jsonb_build_object('id', 'week.runs', 'values', jsonb_build_object('runs', t.runs, 'goal', coalesce(p.runs_per_week, 0)))
    || jsonb_build_object('id', 'week.distance', 'values', jsonb_build_object('meters', round(t.meters)::int));
  if v_streak >= 2 then
    v_facts := v_facts || jsonb_build_object('id', 'week.streak', 'values', jsonb_build_object('weeks', v_streak));
  end if;
  if p.runs_per_week is not null and p.minutes is not null then
    v_cands := v_cands || jsonb_build_object('id', 'plan.next_week',
      'values', jsonb_build_object('runs', p.runs_per_week, 'minutes', p.minutes));
  end if;
  return jsonb_build_object(
    'kind', 'weekly', 'locale', p_locale, 'schema_version', coaching.schema_version(),
    'week', jsonb_build_object('runs', t.runs, 'distance_m', round(t.meters)::int,
      'duration_sec', t.seconds,
      'avg_pace_sec', case when t.meters > 0 and t.seconds > 0 then round(t.seconds / (t.meters / 1000))::int end),
    'goal', jsonb_build_object('goal', p.goal, 'runs_per_week', p.runs_per_week, 'minutes', p.minutes),
    'facts', v_facts, 'candidates', v_cands);
end $$;

-- 다음 훈련 추천의 입력 — 이번 주 진행과 고른 목표, 후보는 고른 시간의 템플릿들
create or replace function coaching.rec_input(p_user uuid, p_monday date, p_tz text, p_locale text, p public.coaching_preferences)
returns jsonb
  language plpgsql stable security definer set search_path = public, coaching as $$
declare t record;
begin
  select * into t from coaching.week_totals(p_user, p_monday, p_tz);
  return jsonb_build_object(
    'kind', 'recommendation', 'locale', p_locale, 'schema_version', coaching.schema_version(),
    'week', jsonb_build_object('runs_so_far', t.runs),
    'goal', jsonb_build_object('goal', p.goal, 'runs_per_week', p.runs_per_week, 'minutes', p.minutes),
    'facts', jsonb_build_array(jsonb_build_object('id', 'week.runs',
      'values', jsonb_build_object('runs', t.runs, 'goal', p.runs_per_week))),
    'candidates', (select jsonb_agg(jsonb_build_object('id', 'template.' || x.id, 'values', '{}'::jsonb) order by x.id)
                     from coaching.templates() x where x.minutes = p.minutes));
end $$;

create or replace function coaching.result_json(r public.coaching_results) returns jsonb
  language sql immutable as $$
  select case when r.id is null then null else jsonb_build_object(
    'id', r.id, 'kind', r.kind, 'generated_at', r.created_at, 'locale', r.locale,
    'facts', r.facts, 'fact_id', r.fact_id, 'explanation', r.explanation,
    'candidates', r.candidates, 'candidate_id', r.candidate_id, 'action_reason', r.action_reason,
    'limitation', r.limitation, 'provider', r.provider) end
$$;

create or replace function coaching.status(p_state text, p_job bigint default null, p_reason text default null,
  p_retryable boolean default false, p_reset_at timestamptz default null, p_result jsonb default null,
  p_stale boolean default false) returns jsonb
  language sql immutable as $$
  select jsonb_build_object('state', p_state, 'job_id', p_job, 'reason', p_reason, 'retryable', p_retryable,
    'reset_at', p_reset_at, 'result', p_result, 'stale', p_stale)
$$;

-- 이번 달 원가 + 지금 잡아 둔 예약
create or replace function coaching.month_spent() returns bigint
  language sql stable security definer set search_path = public as $$
  select coalesce((select sum(cost_micro) from public.coaching_costs where created_at >= date_trunc('month', now())), 0)::bigint
       + coalesce((select sum(reserved_micro) from public.coaching_jobs where status = 'RUNNING'), 0)::bigint
$$;

/*
 * 상태를 알려 주고, [p_request] 면 작업을 만든다. 같은 키의 작업이 있으면 그 작업의 상태를 돌려준다(다시 만들지 않는다).
 * 저장된 마지막 결과의 입력 지문이 지금과 다르면 stale — 새 기록을 분석한 결과로 보이지 않게.
 * [p_key_extra] 는 공급자에게 보내지 않지만 결과를 가르는 값(사용자 시간대) — 지문에만 들어간다.
 */
create or replace function coaching.status_for(p_user uuid, p_kind text, p_source text, p_locale text,
  p_session bigint, p_input jsonb, p_request boolean, p_key_extra text default null) returns jsonb
  language plpgsql security definer set search_path = public, coaching, economy as $$
declare
  v_fp text := md5(p_input::text || '|' || coalesce(p_key_extra, ''));
  v_policy text := coaching.setting_text('coaching_policy_version');
  v_prompt text := coaching.setting_text('coaching_prompt_version');
  j public.coaching_jobs;
  r public.coaching_results;
  latest public.coaching_results;
  v_latest jsonb;
  v_stale boolean;
  v_limit numeric;
  v_used int;
  v_budget numeric;
  v_reserve numeric;
begin
  select * into latest from public.coaching_results x
   where x.user_id = p_user and x.kind = p_kind and x.source_key = p_source and x.locale = p_locale
   order by x.created_at desc, x.id desc limit 1;
  v_latest := coaching.result_json(latest);
  v_stale := latest.id is not null and latest.fingerprint <> v_fp;

  select * into j from public.coaching_jobs x
   where x.user_id = p_user and x.kind = p_kind and x.source_key = p_source and x.locale = p_locale
     and x.fingerprint = v_fp and x.policy_version = v_policy and x.prompt_version = v_prompt;
  if found then
    if j.status = 'SUCCEEDED' then
      select * into r from public.coaching_results where job_id = j.id;
      return coaching.status('ready', j.id, p_result => coaching.result_json(r));
    end if;
    if j.status in ('QUEUED', 'RUNNING') then
      if not coaching.enabled() then
        return coaching.status('disabled', j.id, p_result => v_latest, p_stale => v_stale);
      end if;
      return coaching.status(lower(j.status), j.id, p_result => v_latest, p_stale => v_stale);
    end if;
    if j.status = 'FAILED' then
      return coaching.status('failed', j.id, j.error_code, j.attempts < j.max_attempts and coaching.enabled(),
        p_result => v_latest, p_stale => v_stale);
    end if;
    -- CANCELLED(동의 해제로 멈춘 작업) — 다시 요청하면 같은 작업을 처음부터
  end if;

  if not coaching.enabled() then
    return coaching.status('disabled', p_result => v_latest, p_stale => v_stale);
  end if;
  if not coalesce(p_request, false) then
    return coaching.status('none', p_result => v_latest, p_stale => v_stale);
  end if;

  -- 한도 — 사용자 하루(한국 시각) · 프로젝트 한 달. 같은 사용자의 요청은 한 줄로 세운다(빠른 연타 · 두 기기)
  perform pg_advisory_xact_lock(hashtext('coaching:' || p_user::text));
  v_limit := economy.setting_num('coaching_user_daily_limit');
  -- 오늘 새로 만든 작업 수 — 같은 작업의 재시도 · 다시 묻기는 새로 만든 것이 아니라 세지 않는다
  select count(*) into v_used from public.coaching_jobs x
   where x.user_id = p_user and x.created_at >= economy.today_start();
  if v_used >= v_limit then
    return coaching.status('limit', p_reason => 'user_daily', p_reset_at => economy.today_start() + interval '1 day',
      p_result => v_latest, p_stale => v_stale);
  end if;
  if coaching.setting_text('coaching_provider') <> 'mock' then
    v_budget := economy.setting_num('coaching_monthly_budget_micro');
    v_reserve := economy.setting_num('coaching_reserve_micro');
    if v_budget is null or v_reserve is null or coaching.month_spent() + v_reserve > v_budget then
      return coaching.status('limit', p_reason => 'service_budget',
        p_reset_at => date_trunc('month', now()) + interval '1 month', p_result => v_latest, p_stale => v_stale);
    end if;
  end if;

  if j.id is not null then
    update public.coaching_jobs
       set status = 'QUEUED', attempts = 0, error_code = null, lease_until = null, next_at = now(),
           reserved_micro = 0, created_at = now(), updated_at = now()
     where id = j.id
    returning * into j;
  else
    insert into public.coaching_jobs (user_id, kind, source_key, session_id, locale, fingerprint, policy_version,
                                      prompt_version, input, max_attempts)
    values (p_user, p_kind, p_source, p_session, p_locale, v_fp, v_policy, v_prompt, p_input,
            greatest(1, least(10, coalesce(economy.setting_num('coaching_max_attempts'), 3)::int)))
    on conflict (user_id, kind, source_key, locale, fingerprint, policy_version, prompt_version) do nothing
    returning * into j;
    if j.id is null then
      -- 같은 키가 방금 다른 요청으로 생겼다 — 그 작업을 알려 준다
      select * into j from public.coaching_jobs x
       where x.user_id = p_user and x.kind = p_kind and x.source_key = p_source and x.locale = p_locale
         and x.fingerprint = v_fp and x.policy_version = v_policy and x.prompt_version = v_prompt;
    end if;
  end if;
  return coaching.status(lower(j.status), j.id, p_result => v_latest, p_stale => v_stale);
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 앱이 부르는 함수
-- ══════════════════════════════════════════════════════════════════

-- 코칭 화면의 첫 물음 — 켜져 있나 · 동의 · 선호 · 적용한 계획 · 오늘 한도
create or replace function public.coaching_state(p_tz text)
returns jsonb
language plpgsql stable security definer set search_path = public, coaching, economy as $$
declare
  v_user uuid := coaching.user_required();
  v_tz text := coaching.tz_ok(p_tz);
  c public.coaching_consents;
  p public.coaching_preferences;
  v_monday date := coaching.monday(coaching.today(v_tz));
begin
  select * into c from public.coaching_consents where user_id = v_user and withdrawn_at is null;
  select * into p from public.coaching_preferences where user_id = v_user;
  return jsonb_build_object(
    'enabled', coaching.enabled(),
    'provider', coaching.setting_text('coaching_provider'),
    'consent_version', coaching.setting_text('coaching_consent_version'),
    'consent', case when c.id is null then null else jsonb_build_object(
      'version', c.version, 'granted_at', c.granted_at,
      'current', c.version = coaching.setting_text('coaching_consent_version')) end,
    'prefs', coaching.prefs_json(p),
    'plans', coalesce((select jsonb_agg(jsonb_build_object('week_start', x.week_start, 'days', x.days,
                                        'prefs_revision', x.prefs_revision, 'applied_at', x.applied_at) order by x.week_start)
                         from public.coaching_plans x where x.user_id = v_user and x.week_start >= v_monday), '[]'::jsonb),
    'daily_limit', economy.setting_num('coaching_user_daily_limit'),
    'reset_at', economy.today_start() + interval '1 day');
end $$;

-- 동의(05) — 지금 판의 문구에만. 이미 동의했으면 그대로
create or replace function public.coaching_consent_grant(p_version text)
returns jsonb
language plpgsql security definer set search_path = public, coaching as $$
declare v_user uuid := coaching.user_required();
begin
  if p_version is distinct from coaching.setting_text('coaching_consent_version') then
    raise exception '동의 문구가 바뀌었습니다. 다시 확인해 주세요' using errcode = '22023';
  end if;
  perform pg_advisory_xact_lock(hashtext('coaching:' || v_user::text));
  -- 옛 판의 동의는 거둔 것으로 닫는다(줄은 남긴다)
  update public.coaching_consents set withdrawn_at = now()
   where user_id = v_user and withdrawn_at is null and version <> p_version;
  insert into public.coaching_consents (user_id, version) values (v_user, p_version)
  on conflict (user_id) where withdrawn_at is null do nothing;
  return jsonb_build_object('ok', true, 'version', p_version);
end $$;

/*
 * 동의 해제(15) — 새 분석 · 자동 분석을 멈추고 대기 · 진행 작업을 취소한다. [p_delete_results] 면 저장된 AI 분석과
 * 그 입력(작업 줄)도 지운다. 이미 공급자에게 보낸 요청을 없던 일로 하지는 못한다 — 앱 문구도 그렇게 말한다.
 * 러닝 기록과 서버가 확인한 구간은 지우지 않는다.
 */
create or replace function public.coaching_consent_withdraw(p_delete_results boolean)
returns jsonb
language plpgsql security definer set search_path = public, coaching as $$
declare
  v_user uuid := coaching.user_required();
  v_cancelled int;
  v_deleted int := 0;
begin
  perform pg_advisory_xact_lock(hashtext('coaching:' || v_user::text));
  update public.coaching_consents set withdrawn_at = now(), delete_requested = coalesce(p_delete_results, false)
   where user_id = v_user and withdrawn_at is null;
  update public.coaching_jobs set status = 'CANCELLED', error_code = 'consent_withdrawn', lease_until = null,
         reserved_micro = 0, updated_at = now()
   where user_id = v_user and status in ('QUEUED', 'RUNNING');
  get diagnostics v_cancelled = row_count;
  update public.coaching_preferences set auto_analysis = false, weekly_report = false, revision = revision + 1, updated_at = now()
   where user_id = v_user and (auto_analysis or weekly_report);
  if coalesce(p_delete_results, false) then
    delete from public.coaching_results where user_id = v_user;
    get diagnostics v_deleted = row_count;
    delete from public.coaching_jobs where user_id = v_user;
  end if;
  return jsonb_build_object('cancelled', v_cancelled, 'deleted_results', v_deleted);
end $$;

/*
 * 선호 저장(06 · 14) — 고른 것만. [p_expected_revision] 이 지금과 다르면 저장하지 않고 conflict(다른 기기가 먼저 바꿨다).
 * 자동 분석 · 주간 리포트는 동의가 있을 때만 켤 수 있다. 저장은 러닝을 시작하지 않고 AI 를 부르지도 않는다.
 */
create or replace function public.coaching_prefs_save(p_goal text, p_runs int, p_minutes int,
  p_auto boolean, p_weekly boolean, p_expected_revision bigint)
returns jsonb
language plpgsql security definer set search_path = public, coaching as $$
declare
  v_user uuid := coaching.user_required();
  p public.coaching_preferences;
begin
  if (p_goal is not null and p_goal not in ('steady', 'finish_5k', 'improve'))
     or (p_runs is not null and p_runs not in (2, 3, 4)) or (p_minutes is not null and p_minutes not in (15, 25, 40)) then
    raise exception '고를 수 없는 값입니다' using errcode = '22023';
  end if;
  if (coalesce(p_auto, false) or coalesce(p_weekly, false)) and not coaching.consented(v_user) then
    raise exception 'AI 코칭 동의가 필요합니다' using errcode = '42501';
  end if;
  perform pg_advisory_xact_lock(hashtext('coaching:' || v_user::text));
  select * into p from public.coaching_preferences where user_id = v_user for update;
  if coalesce(p.revision, 0) <> coalesce(p_expected_revision, 0) then
    return jsonb_build_object('ok', false, 'conflict', true, 'prefs', coaching.prefs_json(p));
  end if;
  insert into public.coaching_preferences as x (user_id, goal, runs_per_week, minutes, auto_analysis, weekly_report)
  values (v_user, p_goal, p_runs, p_minutes, coalesce(p_auto, false), coalesce(p_weekly, false))
  on conflict (user_id) do update
     set goal = excluded.goal, runs_per_week = excluded.runs_per_week, minutes = excluded.minutes,
         auto_analysis = excluded.auto_analysis, weekly_report = excluded.weekly_report,
         revision = x.revision + 1, updated_at = now()
  returning * into p;
  return jsonb_build_object('ok', true, 'prefs', coaching.prefs_json(p));
end $$;

/*
 * 앱이 잰 1km 랩(누적 활성 초)을 서버의 그 러닝과 맞대어 보고 받는다 — 랩 수 = 서버 걸음 거리의 정수 km,
 * 늘어나기만, 서버 활성 시간 안, 사람 범위. 먼저 받은 것을 바꾸지 않는다. 코칭에만 쓰는 값이라 동의가 있을 때만 받는다.
 * 'stored' · 'exists' · 'no_consent' · 'not_found' · 'not_a_run' · 'too_short' · 'mismatch' · 'out_of_range' · 'conflict'
 */
create or replace function public.coaching_splits_put(p_started_at timestamptz, p_laps int[])
returns text
language plpgsql security definer set search_path = public, coaching as $$
declare
  v_user uuid := coaching.user_required();
  s public.walk_sessions;
  v_whole int;
  v_prev int := 0;
  v_old int[];
begin
  if not coaching.consented(v_user) then return 'no_consent'; end if;
  select * into s from public.walk_sessions w where w.user_id = v_user and w.started_at = p_started_at;
  if not found then return 'not_found'; end if;
  if not coaching.counts(s) then return 'not_a_run'; end if;
  v_whole := floor(s.steps * coaching.stride_m() / 1000)::int;
  if v_whole < 1 then return 'too_short'; end if;
  if p_laps is null or cardinality(p_laps) <> v_whole or array_lower(p_laps, 1) <> 1 then return 'mismatch'; end if;
  for i in 1 .. v_whole loop
    if p_laps[i] is null or p_laps[i] <= v_prev or p_laps[i] > s.duration_sec then return 'mismatch'; end if;
    if p_laps[i] - v_prev not between coaching.min_split_sec() and coaching.max_split_sec() then return 'out_of_range'; end if;
    v_prev := p_laps[i];
  end loop;
  insert into public.coaching_run_splits (session_id, user_id, laps) values (s.id, v_user, p_laps)
  on conflict (session_id) do nothing;
  if found then return 'stored'; end if;
  select laps into v_old from public.coaching_run_splits where session_id = s.id;
  return case when v_old = p_laps then 'exists' else 'conflict' end;
end $$;

/*
 * 기록 하나의 코칭(03 · 07 · 09 · 11 · 12) — 상태를 알려 주고 [p_request] 면 작업을 만든다.
 * 미동의 · 서버에 없는 러닝 · 무효 · 1km 미만 · 구간 없음은 작업을 만들지 않는다(공급자를 부르지 않는다).
 */
create or replace function public.coaching_run(p_started_at timestamptz, p_locale text, p_request boolean default false)
returns jsonb
language plpgsql security definer set search_path = public, coaching as $$
declare
  v_user uuid := coaching.user_required();
  v_locale text := coaching.locale_ok(p_locale);
  s public.walk_sessions;
  v_laps int[];
  latest public.coaching_results;
begin
  if not coaching.consented(v_user) then return coaching.status('no_consent'); end if;
  select * into s from public.walk_sessions w where w.user_id = v_user and w.started_at = p_started_at;
  if not found then return coaching.status('not_found'); end if;
  if not coaching.counts(s) then return coaching.status('skipped', p_reason => 'not_a_run'); end if;
  if s.steps * coaching.stride_m() < 1000 then return coaching.status('skipped', p_reason => 'too_short'); end if;
  select laps into v_laps from public.coaching_run_splits where session_id = s.id;
  if v_laps is null then
    select * into latest from public.coaching_results x
     where x.user_id = v_user and x.kind = 'run' and x.source_key = 'session:' || s.id and x.locale = v_locale
     order by x.created_at desc limit 1;
    return coaching.status('skipped', p_reason => 'no_splits', p_result => coaching.result_json(latest));
  end if;
  return coaching.status_for(v_user, 'run', 'session:' || s.id, v_locale, s.id,
    coaching.run_input(s, v_laps, v_locale), p_request);
end $$;

/*
 * 한 주의 코칭(04) — 끝난 주만 분석한다(진행 중인 주 · 미래 주는 숫자만). 주 경계는 앱이 보낸 사용자 시간대로.
 */
create or replace function public.coaching_week(p_week_start date, p_tz text, p_locale text, p_request boolean default false)
returns jsonb
language plpgsql security definer set search_path = public, coaching as $$
declare
  v_user uuid := coaching.user_required();
  v_locale text := coaching.locale_ok(p_locale);
  v_tz text := coaching.tz_ok(p_tz);
  v_today date := coaching.today(v_tz);
begin
  if p_week_start is null or extract(isodow from p_week_start) <> 1 then
    raise exception '주의 시작은 월요일입니다' using errcode = '22023';
  end if;
  if not coaching.consented(v_user) then return coaching.status('no_consent'); end if;
  if p_week_start + 6 >= v_today then
    return coaching.status('skipped', p_reason => case when p_week_start > v_today then 'future_week' else 'week_in_progress' end);
  end if;
  if (select t.runs from coaching.week_totals(v_user, p_week_start, v_tz) t) = 0 then
    return coaching.status('skipped', p_reason => 'no_runs');
  end if;
  return coaching.status_for(v_user, 'weekly', 'week:' || p_week_start, v_locale, null,
    coaching.week_input(v_user, p_week_start, v_tz, v_locale), p_request, v_tz);
end $$;

-- 다음 훈련 추천(01) — 선호가 다 있을 때만. 이번 주 진행 · 선호가 바뀔 때만 입력이 바뀐다
create or replace function public.coaching_recommend(p_tz text, p_locale text, p_request boolean default false)
returns jsonb
language plpgsql security definer set search_path = public, coaching as $$
declare
  v_user uuid := coaching.user_required();
  v_locale text := coaching.locale_ok(p_locale);
  v_tz text := coaching.tz_ok(p_tz);
  v_monday date := coaching.monday(coaching.today(v_tz));
  p public.coaching_preferences;
begin
  if not coaching.consented(v_user) then return coaching.status('no_consent'); end if;
  select * into p from public.coaching_preferences where user_id = v_user;
  if p.goal is null or p.runs_per_week is null or p.minutes is null then
    return coaching.status('skipped', p_reason => 'no_prefs');
  end if;
  return coaching.status_for(v_user, 'recommendation', 'rec:' || v_monday, v_locale, null,
    coaching.rec_input(v_user, v_monday, v_tz, v_locale, p), p_request, v_tz);
end $$;

-- 실패한 작업을 같은 작업으로 다시(11) — 시도 횟수가 남았을 때만
create or replace function public.coaching_job_retry(p_job_id bigint)
returns jsonb
language plpgsql security definer set search_path = public, coaching as $$
declare
  v_user uuid := coaching.user_required();
  j public.coaching_jobs;
begin
  if not coaching.consented(v_user) then return coaching.status('no_consent'); end if;
  if not coaching.enabled() then return coaching.status('disabled', p_job_id); end if;
  update public.coaching_jobs set status = 'QUEUED', next_at = now(), lease_until = null, updated_at = now()
   where id = p_job_id and user_id = v_user and status = 'FAILED' and attempts < max_attempts
  returning * into j;
  if j.id is null then
    select * into j from public.coaching_jobs where id = p_job_id and user_id = v_user;
    if j.id is null then return coaching.status('not_found'); end if;
    return coaching.status(lower(j.status), j.id, j.error_code, false);
  end if;
  return coaching.status('queued', j.id);
end $$;

/*
 * 목표 페이스 적용(16) — 그 결과의 페이스 후보 값 그대로. 러닝을 시작하지 않는다. 같은 값이면 다시 적어도 그대로
 * (연타 · 재시도에 안전). [p_expected_revision] 이 다르면 conflict.
 */
create or replace function public.coaching_apply_pace(p_result_id bigint, p_expected_revision bigint)
returns jsonb
language plpgsql security definer set search_path = public, coaching as $$
declare
  v_user uuid := coaching.user_required();
  r public.coaching_results;
  p public.coaching_preferences;
  v_pace int;
begin
  select * into r from public.coaching_results where id = p_result_id and user_id = v_user;
  if r.id is null or r.kind <> 'run' or r.candidate_id is null or r.candidate_id not like 'pace.%' then
    raise exception '적용할 수 있는 제안이 아닙니다' using errcode = '22023';
  end if;
  select (c -> 'values' ->> 'pace_sec')::int into v_pace
    from jsonb_array_elements(r.candidates) c where c ->> 'id' = r.candidate_id;
  if v_pace is null or v_pace not between coaching.min_split_sec() and coaching.max_split_sec() then
    raise exception '적용할 수 있는 제안이 아닙니다' using errcode = '22023';
  end if;
  perform pg_advisory_xact_lock(hashtext('coaching:' || v_user::text));
  select * into p from public.coaching_preferences where user_id = v_user for update;
  if p.user_id is not null and p.target_pace_sec = v_pace and p.target_pace_result = r.id then
    return jsonb_build_object('ok', true, 'prefs', coaching.prefs_json(p));
  end if;
  if coalesce(p.revision, 0) <> coalesce(p_expected_revision, 0) then
    return jsonb_build_object('ok', false, 'conflict', true, 'prefs', coaching.prefs_json(p));
  end if;
  insert into public.coaching_preferences as x (user_id, target_pace_sec, target_pace_result)
  values (v_user, v_pace, r.id)
  on conflict (user_id) do update
     set target_pace_sec = excluded.target_pace_sec, target_pace_result = excluded.target_pace_result,
         revision = x.revision + 1, updated_at = now()
  returning * into p;
  return jsonb_build_object('ok', true, 'prefs', coaching.prefs_json(p));
end $$;

/*
 * 다음 주 계획 적용(13) — 지금 선호로 서버가 계획을 만들어 적는다(앱이 보낸 날짜 · 템플릿을 믿지 않는다).
 * 이번 주나 앞으로의 주만. 같은 주에 다시 적용하면 바꿔 적는다. [p_expected_revision] 이 다르면 conflict.
 */
create or replace function public.coaching_apply_plan(p_week_start date, p_tz text, p_expected_revision bigint)
returns jsonb
language plpgsql security definer set search_path = public, coaching as $$
declare
  v_user uuid := coaching.user_required();
  v_tz text := coaching.tz_ok(p_tz);
  p public.coaching_preferences;
  v_days jsonb;
begin
  if p_week_start is null or extract(isodow from p_week_start) <> 1
     or p_week_start < coaching.monday(coaching.today(v_tz)) then
    raise exception '이번 주나 다음 주의 월요일이어야 합니다' using errcode = '22023';
  end if;
  perform pg_advisory_xact_lock(hashtext('coaching:' || v_user::text));
  select * into p from public.coaching_preferences where user_id = v_user for update;
  if coalesce(p.revision, 0) <> coalesce(p_expected_revision, 0) then
    return jsonb_build_object('ok', false, 'conflict', true, 'prefs', coaching.prefs_json(p));
  end if;
  if p.runs_per_week is null or p.minutes is null then
    raise exception '먼저 목표를 정해 주세요' using errcode = '22023';
  end if;
  v_days := coaching.week_plan(p_week_start, p.runs_per_week, p.minutes);
  insert into public.coaching_plans (user_id, week_start, days, prefs_revision)
  values (v_user, p_week_start, v_days, p.revision)
  on conflict (user_id, week_start) do update set days = excluded.days, prefs_revision = excluded.prefs_revision, applied_at = now();
  return jsonb_build_object('ok', true, 'week_start', p_week_start, 'days', v_days);
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 코칭 워커가 부르는 함수 — 워커 계정(또는 stepup_coach 역할 토큰)만
-- ══════════════════════════════════════════════════════════════════

create or replace function coaching.worker_guard() returns void
language plpgsql stable security definer set search_path = public, economy, coaching as $$
declare v_id text := coaching.setting_text('coaching_worker_user_id');
begin
  if coalesce(auth.jwt() ->> 'role', '') = 'stepup_coach' then return; end if;
  if v_id is not null and auth.uid() is not null and auth.uid()::text = v_id then return; end if;
  raise exception '코칭 워커만 부를 수 있습니다' using errcode = '42501';
end $$;

-- 원가 한 줄 — usage 가 없으면 0(모의 · 보내기 전 실패)
create or replace function coaching.cost_log(j public.coaching_jobs, p_usage jsonb, p_outcome text) returns void
language sql security definer set search_path = public, coaching as $$
  insert into public.coaching_costs (job_id, attempt, provider, model, input_tokens, output_tokens,
                                     cache_read_tokens, cache_write_tokens, cost_micro, outcome)
  values (j.id, j.attempts, coalesce(p_usage ->> 'provider', coaching.setting_text('coaching_provider')), p_usage ->> 'model',
          greatest(0, coalesce((p_usage ->> 'input_tokens')::int, 0)), greatest(0, coalesce((p_usage ->> 'output_tokens')::int, 0)),
          greatest(0, coalesce((p_usage ->> 'cache_read_tokens')::int, 0)), greatest(0, coalesce((p_usage ->> 'cache_write_tokens')::int, 0)),
          greatest(0, coalesce((p_usage ->> 'cost_micro')::bigint, 0)), p_outcome)
$$;

/*
 * 작업 가져가기 — 보내기 직전에 다시 본다: 켜져 있나, 동의가 살아 있나(아니면 취소), 예산이 남았나(아니면 멈춤).
 * 2분 동안 빌리고(lease), 워커가 죽어 2분이 지나면 다른 워커가 가져간다. 시도 수를 여기서 센다.
 */
create or replace function public.coach_worker_claim(p_limit int)
returns table (job_id bigint, kind text, locale text, input jsonb, attempt int, provider text, model text,
               prompt_version text, schema_version text)
language plpgsql security definer set search_path = public, coaching, economy as $$
declare
  j public.coaching_jobs;
  v_want int := greatest(0, least(coalesce(p_limit, 0), 20));
  v_taken int := 0;
  v_provider text := coaching.setting_text('coaching_provider');
  v_reserve bigint := 0;
  v_budget numeric;
begin
  perform coaching.worker_guard();
  if not coaching.enabled() then return; end if;
  if v_provider <> 'mock' then
    v_reserve := economy.setting_num('coaching_reserve_micro')::bigint;
    v_budget := economy.setting_num('coaching_monthly_budget_micro');
    if v_reserve is null or v_budget is null then return; end if;
  end if;
  for j in
    select * from public.coaching_jobs c
     where (c.status = 'QUEUED' or (c.status = 'RUNNING' and c.lease_until < now())) and c.next_at <= now()
     order by c.next_at, c.id
     limit 100
     for update skip locked
  loop
    exit when v_taken >= v_want;
    if not coaching.consented(j.user_id) then
      update public.coaching_jobs set status = 'CANCELLED', error_code = 'consent_withdrawn', lease_until = null,
             reserved_micro = 0, updated_at = now() where id = j.id;
      continue;
    end if;
    if j.attempts >= j.max_attempts then
      update public.coaching_jobs set status = 'FAILED', error_code = coalesce(j.error_code, 'lease_expired'), lease_until = null,
             reserved_micro = 0, updated_at = now() where id = j.id;
      continue;
    end if;
    if v_provider <> 'mock' and coaching.month_spent() - j.reserved_micro + v_reserve > v_budget then
      exit; -- 남은 예산이 없다 — 대기열에 그대로 둔다
    end if;
    update public.coaching_jobs c set status = 'RUNNING', attempts = c.attempts + 1, lease_until = now() + interval '2 minutes',
           reserved_micro = v_reserve, updated_at = now()
     where c.id = j.id;
    v_taken := v_taken + 1;
    job_id := j.id; kind := j.kind; locale := j.locale; input := j.input; attempt := j.attempts + 1;
    provider := v_provider; model := coaching.setting_text('coaching_model');
    prompt_version := j.prompt_version; schema_version := coaching.schema_version();
    return next;
  end loop;
end $$;

-- 금지 문구 — 진단 · 부상 · 의료, 보상 · 포인트, 남과의 비교. 소문자로 견준다
create or replace function coaching.forbidden_terms() returns text[] language sql immutable as $$
  select array[
    '부상', '진단', '질환', '질병', '통증', '심장', '의학', '치료', '처방', '보상', '포인트', '코인', '토큰', 'sup', '순위', '등수', '크루원', '다른 사람', '남들',
    'injur', 'diagnos', 'disease', 'pain', 'heart', 'medical', 'treatment', 'prescri', 'reward', 'point', 'coin', 'token', 'rank', 'crew', 'other runner',
    '怪我', '診断', '病気', '痛み', '心臓', '医療', '治療', '処方', '報酬', 'ポイント', 'コイン', 'トークン', '順位', 'ランキング', 'クルー', '他の人',
    '受伤', '诊断', '疾病', '疼痛', '心脏', '医疗', '治疗', '处方', '奖励', '积分', '代币', '排名', '跑团', '其他人']
$$;

-- AI 글 한 줄 검사 — 비었거나 길거나 숫자 · 금지 문구가 있거나 언어가 다르면 이유를 돌려준다(통과면 null)
create or replace function coaching.text_problem(p_text text, p_locale text) returns text
language plpgsql immutable set search_path = public, coaching as $$
declare v_lower text;
begin
  if p_text is null then return null; end if;
  if length(btrim(p_text)) = 0 then return 'empty_text'; end if;
  if length(p_text) > 80 then return 'text_too_long'; end if;
  if p_text ~ '[0-9０-９]' then return 'number_in_text'; end if;
  if p_text ~ '[<>{}\[\]\\`]' or p_text ~ 'https?:' then return 'markup_in_text'; end if;
  v_lower := lower(p_text);
  if exists (select 1 from unnest(coaching.forbidden_terms()) t where position(t in v_lower) > 0) then
    return 'forbidden_term';
  end if;
  case p_locale
    when 'ko' then if p_text !~ '[가-힣]' then return 'wrong_language'; end if;
    when 'ja' then if p_text !~ '[ぁ-んァ-ン]' or p_text ~ '[가-힣]' then return 'wrong_language'; end if;
    when 'zh' then if p_text !~ '[一-鿿]' or p_text ~ '[ぁ-んァ-ン가-힣]' then return 'wrong_language'; end if;
    when 'en' then if p_text !~ '^[ -~]+$' or p_text !~ '[A-Za-z]' then return 'wrong_language'; end if;
    else return 'wrong_language';
  end case;
  return null;
end $$;

/*
 * 결과 형식 검사 — 형식이 맞는다고 사실이 맞는 것은 아니다. 근거 · 후보 id 는 입력에 있던 것만, 제한 코드는 정해진 것만,
 * 글은 짧고 숫자 · 금지 문구 없이 그 언어로. 모르는 칸이 있어도 받지 않는다. 통과면 null.
 */
create or replace function coaching.output_problem(p_input jsonb, p_output jsonb) returns text
language plpgsql immutable set search_path = public, coaching as $$
declare
  v_key text;
  v_problem text;
begin
  if p_output is null or jsonb_typeof(p_output) <> 'object' then return 'not_object'; end if;
  for v_key in select jsonb_object_keys(p_output) loop
    if v_key not in ('schema_version', 'kind', 'observation_fact_id', 'explanation', 'next_action_candidate_id',
                     'action_reason', 'limitation_code') then
      return 'unknown_field';
    end if;
  end loop;
  if p_output ->> 'schema_version' is distinct from p_input ->> 'schema_version' then return 'schema_version'; end if;
  if p_output ->> 'kind' is distinct from p_input ->> 'kind' then return 'kind'; end if;
  if p_output ->> 'observation_fact_id' is null then
    if jsonb_array_length(p_input -> 'facts') > 0 then return 'missing_fact'; end if;
  elsif not exists (select 1 from jsonb_array_elements(p_input -> 'facts') f where f ->> 'id' = p_output ->> 'observation_fact_id') then
    return 'unknown_fact';
  end if;
  if p_output ->> 'next_action_candidate_id' is not null
     and not exists (select 1 from jsonb_array_elements(p_input -> 'candidates') c
                      where c ->> 'id' = p_output ->> 'next_action_candidate_id') then
    return 'unknown_candidate';
  end if;
  if p_output ->> 'limitation_code' is not null
     and p_output ->> 'limitation_code' not in ('insufficient_data', 'no_comparison', 'partial_data') then
    return 'unknown_limitation';
  end if;
  if p_output -> 'explanation' is not null and jsonb_typeof(p_output -> 'explanation') not in ('string', 'null') then return 'not_text'; end if;
  if p_output -> 'action_reason' is not null and jsonb_typeof(p_output -> 'action_reason') not in ('string', 'null') then return 'not_text'; end if;
  v_problem := coaching.text_problem(p_output ->> 'explanation', p_input ->> 'locale');
  if v_problem is not null then return v_problem; end if;
  if p_output ->> 'next_action_candidate_id' is null and p_output ->> 'action_reason' is not null then return 'reason_without_action'; end if;
  return coaching.text_problem(p_output ->> 'action_reason', p_input ->> 'locale');
end $$;

-- 시도 하나를 끝낸다 — 시도가 남았으면 뒤로 미뤄 다시, 아니면 FAILED
create or replace function coaching.job_fail(j public.coaching_jobs, p_code text, p_retryable boolean) returns text
language plpgsql security definer set search_path = public, coaching as $$
begin
  if coalesce(p_retryable, false) and j.attempts < j.max_attempts then
    update public.coaching_jobs set status = 'QUEUED', error_code = left(p_code, 80), lease_until = null, reserved_micro = 0,
           next_at = now() + make_interval(secs => least(3600, 30 * power(2, j.attempts)::int)), updated_at = now()
     where id = j.id;
    return 'requeued';
  end if;
  update public.coaching_jobs set status = 'FAILED', error_code = left(p_code, 80), lease_until = null, reserved_micro = 0,
         updated_at = now() where id = j.id;
  return 'failed';
end $$;

/*
 * 결과 받기 — 아직 그 워커가 빌린 작업이어야 하고(취소 · 만료된 작업의 늦은 답은 버린다), 동의가 살아 있어야 한다.
 * 검사를 통과한 것만 저장한다. 통과하지 못하면 시도가 남았을 때 다시. 원가는 어느 쪽이든 적는다(이미 과금됐을 수 있다).
 * 'stored' · 'invalid:<까닭>' · 'cancelled' · 'stale'
 */
create or replace function public.coach_worker_complete(p_job_id bigint, p_attempt int, p_output jsonb, p_usage jsonb)
returns text
language plpgsql security definer set search_path = public, coaching as $$
declare
  j public.coaching_jobs;
  v_problem text;
begin
  perform coaching.worker_guard();
  select * into j from public.coaching_jobs where id = p_job_id for update;
  if j.id is null then return 'stale'; end if;
  if j.status <> 'RUNNING' or j.attempts <> p_attempt then
    perform coaching.cost_log(j, p_usage, 'late');
    return case when j.status = 'CANCELLED' then 'cancelled' else 'stale' end;
  end if;
  if not coaching.consented(j.user_id) then
    perform coaching.cost_log(j, p_usage, 'cancelled');
    update public.coaching_jobs set status = 'CANCELLED', error_code = 'consent_withdrawn', lease_until = null,
           reserved_micro = 0, updated_at = now() where id = j.id;
    return 'cancelled';
  end if;
  v_problem := coaching.output_problem(j.input, p_output);
  if v_problem is not null then
    perform coaching.cost_log(j, p_usage, 'invalid');
    perform coaching.job_fail(j, 'invalid_output:' || v_problem, true);
    return 'invalid:' || v_problem;
  end if;
  insert into public.coaching_results (job_id, user_id, kind, source_key, session_id, locale, fingerprint, schema_version,
    facts, candidates, fact_id, explanation, candidate_id, action_reason, limitation, provider, model)
  values (j.id, j.user_id, j.kind, j.source_key, j.session_id, j.locale, j.fingerprint, coaching.schema_version(),
    j.input -> 'facts', j.input -> 'candidates', p_output ->> 'observation_fact_id', p_output ->> 'explanation',
    p_output ->> 'next_action_candidate_id', p_output ->> 'action_reason', p_output ->> 'limitation_code',
    coalesce(p_usage ->> 'provider', coaching.setting_text('coaching_provider')), p_usage ->> 'model');
  perform coaching.cost_log(j, p_usage, 'ok');
  update public.coaching_jobs set status = 'SUCCEEDED', error_code = null, lease_until = null, reserved_micro = 0,
         updated_at = now() where id = j.id;
  return 'stored';
end $$;

-- 공급자 실패(429 · 5xx · 시간 초과 · 거절 · 중단) — 다시 할 수 있으면 미뤄서, 아니면 FAILED. 'requeued' · 'failed' · 'stale'
create or replace function public.coach_worker_fail(p_job_id bigint, p_attempt int, p_error text, p_retryable boolean, p_usage jsonb)
returns text
language plpgsql security definer set search_path = public, coaching as $$
declare j public.coaching_jobs;
begin
  perform coaching.worker_guard();
  select * into j from public.coaching_jobs where id = p_job_id for update;
  if j.id is null then return 'stale'; end if;
  perform coaching.cost_log(j, p_usage, 'error');
  if j.status <> 'RUNNING' or j.attempts <> p_attempt then return 'stale'; end if;
  return coaching.job_fail(j, coalesce(nullif(p_error, ''), 'provider_error'), p_retryable);
end $$;

-- ── 함수 권한 ───────────────────────────────────────────────────────
-- 안쪽 함수는 아무도 바로 부르지 못한다. 앱 함수는 로그인한 사람만, 워커 함수는 워커 확인을 안에서 한다
do $$
declare f record;
begin
  for f in select p.oid::regprocedure as sig from pg_proc p join pg_namespace n on n.oid = p.pronamespace
            where n.nspname = 'coaching' loop
    execute format('revoke all on function %s from public', f.sig);
  end loop;
end $$;

revoke all on function public.coaching_state(text) from public, anon;
revoke all on function public.coaching_consent_grant(text) from public, anon;
revoke all on function public.coaching_consent_withdraw(boolean) from public, anon;
revoke all on function public.coaching_prefs_save(text, int, int, boolean, boolean, bigint) from public, anon;
revoke all on function public.coaching_splits_put(timestamptz, int[]) from public, anon;
revoke all on function public.coaching_run(timestamptz, text, boolean) from public, anon;
revoke all on function public.coaching_week(date, text, text, boolean) from public, anon;
revoke all on function public.coaching_recommend(text, text, boolean) from public, anon;
revoke all on function public.coaching_job_retry(bigint) from public, anon;
revoke all on function public.coaching_apply_pace(bigint, bigint) from public, anon;
revoke all on function public.coaching_apply_plan(date, text, bigint) from public, anon;
revoke all on function public.coach_worker_claim(int) from public, anon;
revoke all on function public.coach_worker_complete(bigint, int, jsonb, jsonb) from public, anon;
revoke all on function public.coach_worker_fail(bigint, int, text, boolean, jsonb) from public, anon;

grant execute on function public.coaching_state(text) to authenticated;
grant execute on function public.coaching_consent_grant(text) to authenticated;
grant execute on function public.coaching_consent_withdraw(boolean) to authenticated;
grant execute on function public.coaching_prefs_save(text, int, int, boolean, boolean, bigint) to authenticated;
grant execute on function public.coaching_splits_put(timestamptz, int[]) to authenticated;
grant execute on function public.coaching_run(timestamptz, text, boolean) to authenticated;
grant execute on function public.coaching_week(date, text, text, boolean) to authenticated;
grant execute on function public.coaching_recommend(text, text, boolean) to authenticated;
grant execute on function public.coaching_job_retry(bigint) to authenticated;
grant execute on function public.coaching_apply_pace(bigint, bigint) to authenticated;
grant execute on function public.coaching_apply_plan(date, text, bigint) to authenticated;
grant execute on function public.coach_worker_claim(int) to authenticated;
grant execute on function public.coach_worker_complete(bigint, int, jsonb, jsonb) to authenticated;
grant execute on function public.coach_worker_fail(bigint, int, text, boolean, jsonb) to authenticated;
