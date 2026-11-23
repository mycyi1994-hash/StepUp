-- 0124 — 매일 이용자 · 활동 숫자: 한 함수 · 읽기 전용 문 (2026-11-24)
--
-- 사용자 "매일 09:00 기준 실 이용자 수 · 도움될 만한 것 전부" → "그냥 여기다가 해"(Claude 대화로 받기).
--   economy.daily_stats_report(제외 이메일) — 어제(한국 날짜) 숫자를 마크다운 한 덩어리로. 숫자만, 개인 정보 없음.
--     .github/workflows/daily-stats.yml(메신저)과 public.daily_stats()(아래 문)가 같은 셈을 쓴다.
--   public.daily_stats() — 머리글 x-stats-key 가 맞으면 그 보고서만 돌려준다(로그인 없이 · 읽기만).
--     키는 SQL 창에서 select economy.stats_key_new('이름'); — 한 번만 보이고 서버에는 해시만. 거두기는 update economy.stats_keys set revoked_at = now().
--   빼고 셀 이메일: 설정 stats_exclude_emails(쉼표로) — 없으면 시험 주소(@sim.test · @example.com)만 뺀다.

create table if not exists economy.stats_keys (
  id bigint generated always as identity primary key,
  label text not null,
  key_hash text not null unique,
  created_at timestamptz not null default now(),
  last_used_at timestamptz,
  revoked_at timestamptz
);
alter table economy.stats_keys enable row level security;
revoke all on economy.stats_keys from public, anon, authenticated;

create or replace function economy.stats_key_new(p_label text) returns text
language plpgsql security definer set search_path = public, economy as $$
declare v_token text := replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', '');
begin
  insert into economy.stats_keys (label, key_hash)
  values (coalesce(nullif(btrim(p_label), ''), 'stats'), encode(sha256(convert_to(v_token, 'UTF8')), 'hex'));
  return v_token;
end $$;
revoke all on function economy.stats_key_new(text) from public, anon, authenticated;

create or replace function economy.daily_stats_report(p_exclude text default null) returns text
language plpgsql stable security definer set search_path = public, economy as $$
begin
  return (
    with
    p as (
      select economy.game_day(now()) - 1 as d
    ),
    w as (
      select d,
             economy.game_day_start(d)          as d0, economy.game_day_start(d + 1)      as d1,
             economy.game_day_start(d - 1)      as y0,
             economy.game_day_start(d - 6)      as w0, economy.game_day_start(d - 13)     as pw0,
             economy.game_day_start(d - 29)     as m0
        from p
    ),
    ex as (
      select u.id from auth.users u
       where lower(u.email) = any (string_to_array(lower(replace(coalesce(p_exclude, economy.setting('stats_exclude_emails') #>> '{}', ''), ' ', '')), ','))
          or u.email ilike '%@sim.test' or u.email ilike '%@example.com'
    ),
    -- 사람 · 날 — 앱을 열었거나 러닝을 올렸다
    act as (
      select j.user_id, substring(j.ref from '(\d{4}-\d{2}-\d{2})$')::date as day
        from public.chain_jobs j, w
       where j.kind = 'CHECK_IN' and j.user_id is not null and j.created_at >= w.m0 - interval '2 days'
             and j.ref ~ '\d{4}-\d{2}-\d{2}$'
      union
      select s.user_id, economy.game_day(s.started_at)
        from public.walk_sessions s, w
       where s.started_at >= w.m0 and s.verdict <> 'VOID'
    ),
    a as (
      select act.* from act where act.user_id not in (select id from ex)
    ),
    runs as (
      select s.* from public.walk_sessions s, w
       where s.started_at >= w.d0 and s.started_at < w.d1 and s.user_id not in (select id from ex)
    ),
    led as (
      select l.* from public.sup_ledger l, w
       where l.occurred_at >= w.d0 and l.occurred_at < w.d1 and l.user_id not in (select id from ex)
    ),
    jobs as (
      select j.kind, count(*) as n from public.chain_jobs j, w
       where j.created_at >= w.d0 and j.created_at < w.d1
       group by j.kind
    ),
    v as (
      select
        (select d from w) as d,
        -- 활성
        (select count(distinct user_id) from a, w where a.day = w.d)                         as dau,
        (select count(distinct user_id) from a, w where a.day = w.d - 1)                     as dau_prev,
        (select count(distinct user_id) from a, w where a.day = w.d - 7)                     as dau_week_ago,
        (select count(distinct user_id) from a, w where a.day between w.d - 6 and w.d)       as wau,
        (select count(distinct user_id) from a, w where a.day between w.d - 13 and w.d - 7)  as wau_prev,
        (select count(distinct user_id) from a, w where a.day between w.d - 29 and w.d)      as mau,
        -- 가입
        (select count(*) from auth.users u, w where u.created_at >= w.d0 and u.created_at < w.d1 and u.id not in (select id from ex)) as signups,
        (select count(*) from economy.access_grants g, w where g.source = 'CODE' and g.granted_at >= w.d0 and g.granted_at < w.d1
           and g.user_id not in (select id from ex)) as code_in,
        (select count(*) from auth.users u where u.id not in (select id from ex)) as users_total,
        -- 잔존: 1일 · 7일 전 가입한 사람 가운데 어제 활성
        (select count(*) from auth.users u, w where economy.game_day(u.created_at) = w.d - 1 and u.id not in (select id from ex)) as d1_base,
        (select count(*) from auth.users u, w where economy.game_day(u.created_at) = w.d - 1 and u.id not in (select id from ex)
           and exists (select 1 from a where a.user_id = u.id and a.day = w.d)) as d1_back,
        (select count(*) from auth.users u, w where economy.game_day(u.created_at) = w.d - 7 and u.id not in (select id from ex)) as d7_base,
        (select count(*) from auth.users u, w where economy.game_day(u.created_at) = w.d - 7 and u.id not in (select id from ex)
           and exists (select 1 from a where a.user_id = u.id and a.day = w.d)) as d7_back,
        -- 러닝
        (select count(*) from runs where verdict <> 'VOID')                          as run_n,
        (select count(distinct user_id) from runs where verdict <> 'VOID')           as runners,
        (select coalesce(sum(distance_meters), 0) / 1000.0 from runs where verdict <> 'VOID') as km,
        (select coalesce(sum(duration_sec), 0) / 60.0 from runs where verdict <> 'VOID')      as minutes,
        (select count(*) from runs where verdict = 'VOID')                           as run_void,
        (select count(*) from runs where verdict = 'FLAGGED')                        as run_flagged,
        (select count(*) from economy.crew_runs c, w where c.day = w.d and c.user_id not in (select id from ex)) as crew_runners,
        (select count(*) from public.parties pa, w where pa.starts_at >= w.d0 and pa.starts_at < w.d1 and pa.status <> 'LOBBY') as rooms_started,
        -- SUP
        (select coalesce(sum(amount), 0) from led where amount > 0)                  as sup_in,
        (select coalesce(-sum(amount), 0) from led where amount < 0)                 as sup_out,
        (select coalesce(sum(amount), 0) from public.sup_ledger l where l.user_id not in (select id from ex)) as sup_total,
        -- 커뮤니티
        (select count(*) from public.posts po, w where po.created_at >= w.d0 and po.created_at < w.d1 and po.author_id not in (select id from ex)) as posts,
        (select count(*) from public.posts po, w where po.created_at >= w.d0 and po.created_at < w.d1 and po.category = 'FLASH'
           and po.author_id not in (select id from ex)) as flash_posts,
        -- 체인
        (select coalesce(sum(n), 0) from jobs)                                       as chain_jobs,
        (select count(*) from public.chain_jobs j where j.status in ('QUEUED', 'CLAIMED'))  as chain_queue,
        (select count(*) from public.chain_jobs j, w where j.status = 'FAILED' and j.created_at >= w.d0 - interval '6 days') as chain_failed_7d
    ),
    pct as (
      select v.*,
        case when dau_prev > 0 then round(100.0 * (dau - dau_prev) / dau_prev) end as dau_chg,
        case when wau_prev > 0 then round(100.0 * (wau - wau_prev) / wau_prev) end as wau_chg
      from v
    ),
    -- 활동 종류별(어제 줄 선 체인 기록 — 종류마다 하루 상한이 있어 실제보다 적을 수 있다)
    kinds as (
      select string_agg(format('%s %s', case kind
               when 'SHOE_DRAW' then '뽑기' when 'SHOE_FORGE' then '강화' when 'SHOE_REPAIR' then '수리'
               when 'SHOE_POINTS' then '포인트' when 'CREW_JOIN' then '크루 가입' when 'CHECK_IN' then '출석'
               when 'RUN_SETTLE' then '러닝 정산' when 'SHOE_WEAR' then '신발 주행' when 'RUN_PROOF' then '러닝 증명'
               when 'DRAW_SEED' then '씨앗' when 'BADGE' then '배지' when 'CREW_RUN' then '크루 러닝'
               when 'PARTY_START' then '함께 출발' when 'FLASH_RUN' then '번개' when 'DAILY_STEPS' then '하루 걸음'
               else kind end, n), ' · ' order by n desc) as s
        from jobs
    ),
    -- SUP 종류별
    sup_kinds as (
      select string_agg(format('%s %s', kind, to_char(t, 'FM999,999,990.0')), ' · ' order by abs(t) desc) as s
        from (select kind, sum(amount) as t from led group by kind) x
    ),
    -- 앱 판(지난 30일 활성 가운데) — 스토어 번호 = versionCode % 1000, 알리지 않은 사람은 예전 판
    vers as (
      select string_agg(format('%s → %s', ver, n), ' · ' order by ver desc) as s
        from (select coalesce((c.version_code % 1000)::text, '예전') as ver, count(*) as n
                from (select distinct user_id from a, w where a.day between w.d - 29 and w.d) m
                left join public.app_clients c on c.user_id = m.user_id
               group by 1) x
    ),
    lvl as (
      select coalesce(round(avg(level), 1), 0) as avg_level, coalesce(max(level), 0) as max_level, count(*) filter (where level >= 10) as lv10
        from economy.runner_levels r where r.user_id not in (select id from ex)
    )
    select concat_ws(E'\n',
      format('## %s (%s) 이용자 · 활동', to_char(d, 'YYYY-MM-DD'), (array['일','월','화','수','목','금','토'])[extract(dow from d)::int + 1]),
      '',
      '**이용자**',
      format('- 활성(앱을 열었거나 러닝) **%s명** — 전날 %s명(%s) · 지난주 같은 요일 %s명', dau, dau_prev,
             coalesce(case when dau_chg >= 0 then '+' else '' end || dau_chg || '%', '—'), dau_week_ago),
      format('- 7일 활성 %s명(그 전 7일 대비 %s) · 30일 활성 %s명 · 끈적임(DAU ÷ MAU) %s', wau,
             coalesce(case when wau_chg >= 0 then '+' else '' end || wau_chg || '%', '—'), mau,
             case when mau > 0 then round(100.0 * dau / mau) || '%' else '—' end),
      format('- 신규 가입 %s명(코드로 들어온 사람 %s) · 누적 %s명', signups, code_in, users_total),
      format('- 다시 온 사람: 그저께 가입 %s명 중 %s명(%s) · 8일 전 가입 %s명 중 %s명(%s)',
             d1_base, d1_back, case when d1_base > 0 then round(100.0 * d1_back / d1_base) || '%' else '—' end,
             d7_base, d7_back, case when d7_base > 0 then round(100.0 * d7_back / d7_base) || '%' else '—' end),
      '',
      '**러닝**',
      format('- %s번 · %s명 · %s km · %s시간(1인 평균 %s분)', run_n, runners, to_char(km, 'FM999,990.0'),
             to_char(minutes / 60.0, 'FM999,990.0'), case when runners > 0 then round(minutes / runners) else 0 end),
      format('- 크루 러닝 참여 %s명 · 출발한 방(크루 · 번개) %s개', crew_runners, rooms_started),
      format('- 무효 %s번 · 확인 필요(FLAGGED) %s번', run_void, run_flagged),
      format('- 러너 레벨 평균 %s · 최고 %s · Lv10 이상 %s명', (select avg_level from lvl), (select max_level from lvl), (select lv10 from lvl)),
      '',
      '**SUP**',
      format('- 들어옴 %s · 나감 %s · 순증 %s · 1인 평균 적립 %s', to_char(sup_in, 'FM999,999,990.0'), to_char(sup_out, 'FM999,999,990.0'),
             to_char(sup_in - sup_out, 'FM999,999,990.0'), case when dau > 0 then to_char(sup_in / dau, 'FM999,990.0') else '—' end),
      format('- 종류별: %s', coalesce((select s from sup_kinds), '없음')),
      format('- 전체 잔고 합 %s', to_char(sup_total, 'FM999,999,990.0')),
      '',
      '**한 일 · 커뮤니티**',
      format('- %s', coalesce((select s from kinds), '없음')),
      format('- 글 %s개(번개 %s)', posts, flash_posts),
      '',
      '**앱 판(30일 활성)**',
      format('- %s', coalesce((select s from vers), '없음')),
      '',
      '**체인 기록**',
      format('- 어제 줄 선 기록 %s건 · 지금 대기 %s건 · 지난 7일 실패 %s건', chain_jobs, chain_queue, chain_failed_7d)
    )
    from pct
  );
end $$;
revoke all on function economy.daily_stats_report(text) from public, anon, authenticated;

-- 읽기 전용 문 — 키가 맞을 때만 보고서. 틀리면 42501(까닭 없이)
create or replace function public.daily_stats() returns text
language plpgsql security definer set search_path = public, economy as $$
declare
  v_key text;
  v_id bigint;
begin
  begin
    v_key := nullif(current_setting('request.headers', true), '')::jsonb ->> 'x-stats-key';
  exception when others then v_key := null;
  end;
  select k.id into v_id from economy.stats_keys k
   where k.revoked_at is null and k.key_hash = encode(sha256(convert_to(coalesce(v_key, ''), 'UTF8')), 'hex');
  if v_id is null then
    raise exception 'forbidden' using errcode = '42501';
  end if;
  update economy.stats_keys set last_used_at = now() where id = v_id;
  return economy.daily_stats_report(null);
end $$;
revoke all on function public.daily_stats() from public;
grant execute on function public.daily_stats() to anon, authenticated;
