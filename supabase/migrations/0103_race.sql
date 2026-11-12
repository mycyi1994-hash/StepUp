-- ════════════════════════════════════════════════════════════════════
--  0103 — 홈페이지 미니게임 「StepUp 레이스」 순위표 (2026-11-12)
--
--  사용자 "스텝업 홈페이지에 미니게임 — 장애물 달리기, 결승전까지 가는 시간으로 리더보드". 페이지 web/game/(stepupcrew.com/game).
--  · race_start(): 판을 시작할 때 서버 시각을 적은 번호를 준다(같은 곳(IP 해시) 10분 40번).
--  · race_finish(번호, 기기 토큰, 이름, 합계 ms, 판별 ms 셋): 서버가 본 시간(지금 − 시작)보다 짧은 기록 · 판별 하한보다 짧은 판 ·
--    합이 맞지 않는 기록은 받지 않는다. 번호는 한 번만. 이름은 2 ~ 12자(한글 · 영문 · 숫자 · 띄어쓰기 · _ . -).
--    완벽한 부정 방지는 아니다(게임은 브라우저에서 돈다) — 시간을 꾸며도 실제로 그만큼 기다려야 하고 하한 아래는 막는다.
--  · race_board(기기 토큰, 몇 명): 사람(기기)마다 최고 기록 하나로 줄 세운다 + 내 자리.
--  · 기기 토큰은 페이지가 만든 무작위 32자 — 서버에는 해시만. 계정 · 이메일과 잇지 않는다.
--  · 지우기(이름이 부적절할 때): delete from economy.race_scores where name = '…';
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values
  ('race_min_split_ms', '[15000, 12000, 15000]'::jsonb),
  ('race_open', 'true'::jsonb)
on conflict (key) do nothing;

create table if not exists economy.race_runs (
  id uuid primary key default gen_random_uuid(),
  client text,
  started_at timestamptz not null default now(),
  finished_at timestamptz
);
create index if not exists race_runs_client on economy.race_runs (client, started_at);
alter table economy.race_runs enable row level security;
revoke all on economy.race_runs from public;

create table if not exists economy.race_scores (
  id bigint generated always as identity primary key,
  run_id uuid not null unique references economy.race_runs(id) on delete cascade,
  player text not null check (player ~ '^[0-9a-f]{64}$'),
  name text not null check (char_length(name) between 2 and 12),
  ms int not null check (ms between 1 and 3600000),
  splits int[] not null,
  client text,
  created_at timestamptz not null default now()
);
create index if not exists race_scores_player on economy.race_scores (player, ms);
create index if not exists race_scores_ms on economy.race_scores (ms);
alter table economy.race_scores enable row level security;
revoke all on economy.race_scores from public;

create or replace function public.race_start()
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_client text := economy.waitlist_client();
  v_id uuid;
begin
  if coalesce((select (value #>> '{}')::boolean from public.economy_settings where key = 'race_open'), true) is not true then
    return jsonb_build_object('ok', false, 'code', 'closed');
  end if;
  if v_client is not null then
    if (select count(*) from economy.race_runs where client = v_client and started_at > now() - interval '10 minutes') >= 40 then
      return jsonb_build_object('ok', false, 'code', 'busy');
    end if;
  end if;
  -- 끝내지 않은 지 하루 지난 번호는 치운다
  delete from economy.race_runs r where r.finished_at is null and r.started_at < now() - interval '1 day'
    and not exists (select 1 from economy.race_scores s where s.run_id = r.id);
  insert into economy.race_runs (client) values (v_client) returning id into v_id;
  return jsonb_build_object('ok', true, 'run', v_id);
end $$;
revoke all on function public.race_start() from public;
grant execute on function public.race_start() to anon, authenticated;

create or replace function public.race_finish(p_run uuid, p_player text, p_name text, p_ms int, p_splits int[])
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_client text := economy.waitlist_client();
  v_name text := regexp_replace(btrim(coalesce(p_name, '')), '\s+', ' ', 'g');
  v_player text;
  v_run economy.race_runs;
  v_min jsonb := coalesce((select value from public.economy_settings where key = 'race_min_split_ms'), '[15000, 12000, 15000]'::jsonb);
  v_best int;
  v_rank int;
begin
  if coalesce(p_player, '') !~ '^[0-9a-f]{32}$' then
    return jsonb_build_object('ok', false, 'code', 'run');
  end if;
  v_player := encode(sha256(convert_to(p_player, 'UTF8')), 'hex');
  if v_name !~ '^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9 _.\-]{2,12}$' then
    return jsonb_build_object('ok', false, 'code', 'name');
  end if;
  select * into v_run from economy.race_runs where id = p_run for update;
  if not found or v_run.started_at < now() - interval '3 hours' then
    return jsonb_build_object('ok', false, 'code', 'run');
  end if;
  if v_run.finished_at is not null then
    return jsonb_build_object('ok', false, 'code', 'used');
  end if;
  if p_ms is null or p_splits is null or array_length(p_splits, 1) is distinct from 3
     or p_splits[1] < (v_min->>0)::int or p_splits[2] < (v_min->>1)::int or p_splits[3] < (v_min->>2)::int
     or abs(p_splits[1] + p_splits[2] + p_splits[3] - p_ms) > 100
     or p_ms > 3600000
     -- 서버가 본 시간보다 짧을 수 없다(판 사이 안내 · 카운트다운 시간은 기록에 들지 않으므로 늘 더 길다). 2초 여유
     or extract(epoch from (now() - v_run.started_at)) * 1000 < p_ms - 2000 then
    return jsonb_build_object('ok', false, 'code', 'time');
  end if;
  update economy.race_runs set finished_at = now() where id = p_run;
  insert into economy.race_scores (run_id, player, name, ms, splits, client)
  values (p_run, v_player, v_name, p_ms, p_splits, v_client);
  select min(ms) into v_best from economy.race_scores where player = v_player;
  select count(*) + 1 into v_rank from (
    select player, min(ms) as best from economy.race_scores group by player
  ) b where b.best < v_best;
  return jsonb_build_object('ok', true, 'best_ms', v_best, 'rank', v_rank, 'is_best', v_best = p_ms);
end $$;
revoke all on function public.race_finish(uuid, text, text, int, int[]) from public;
grant execute on function public.race_finish(uuid, text, text, int, int[]) to anon, authenticated;

create or replace function public.race_board(p_player text default '', p_limit int default 50)
returns jsonb
language plpgsql stable security definer set search_path = ''
as $$
declare
  v_player text := case when coalesce(p_player, '') ~ '^[0-9a-f]{32}$' then encode(sha256(convert_to(p_player, 'UTF8')), 'hex') end;
  v jsonb;
begin
  with best as (
    select distinct on (s.player) s.player, s.name, s.ms, s.created_at
    from economy.race_scores s order by s.player, s.ms, s.created_at
  ), ranked as (
    select b.*, rank() over (order by b.ms) as rk, row_number() over (order by b.ms, b.created_at) as rn from best b
  )
  select jsonb_build_object(
    'top', coalesce((select jsonb_agg(jsonb_build_object('rank', r.rk, 'name', r.name, 'ms', r.ms, 'at', r.created_at, 'me', r.player = v_player) order by r.rn)
                     from ranked r where r.rn <= least(greatest(coalesce(p_limit, 50), 1), 100)), '[]'::jsonb),
    'me', (select jsonb_build_object('rank', r.rk, 'name', r.name, 'ms', r.ms) from ranked r where r.player = v_player),
    'players', (select count(*) from best)
  ) into v;
  return v;
end $$;
revoke all on function public.race_board(text, int) from public;
grant execute on function public.race_board(text, int) to anon, authenticated;
