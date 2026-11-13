-- ════════════════════════════════════════════════════════════════════
--  0104 — StepUP 타워(홈페이지 미니게임, 공개 전 내부 시험 — web/lab-mja9j66fef/) (2026-11-13)
--
--  사용자 "라운드 나누지 말고 빙글빙글 돌며 위로 · 점수제 · 죽으면 한 번만 부활 · 00시 초기화 · SNS 올리면 다시(하루 3번) ·
--  사전 등록 이메일로 참가 · 오늘 순위 + 역대 최고". 0103(StepUp 레이스)은 그대로 두고 따로 센다.
--  · 참가: tower_join(사전 등록 이메일, 닉네임 2~12자). 이메일 소유는 확인하지 않는다(워킹데이와 같다 — 시험용).
--  · 하루 기회 = 1 + 그날 SNS 링크(거절 빼고, 3까지) − 그날 시작한 판. 날짜는 한국 시간(economy.walkingday_today).
--  · 판: tower_start → 구간마다 tower_ping(구간 번호, 구간 ms — 하한 · 서버가 본 시간보다 짧으면 거절) → tower_end.
--    창을 닫아 tower_end 가 안 와도 마지막 ping 까지의 점수가 남는다.
--  · 점수는 서버가 다시 센다(economy.tower_score = web/lab-mja9j66fef/tower-rules.js 의 towerScore — web/test/tower.test.js 가 구간 표를 맞대어 본다).
--    완주 순위 · 부활 여부 · 밀어 떨어뜨린 수는 브라우저가 알려 주는 값이라 믿을 수밖에 없다(시험용 — 상한만 둔다).
--  · SNS: tower_sns_submit — 링크 형식(워킹데이 economy.walkingday_sns_parse) · 같은 게시물 한 번 · 하루 3번. 바로 인정, 나중에 거절할 수 있다:
--      update economy.tower_sns set status = 'rejected' where url = '…';
-- ════════════════════════════════════════════════════════════════════

-- 구간 표: len(m) · y0 · y1 · par · min(ms) — tower-rules.js 의 SECTIONS 와 같은 값
create or replace function economy.tower_sections() returns jsonb
language sql immutable set search_path = '' as $$
  select '[{"len":117.7,"y0":0,"y1":8,"par":25000,"min":9000},{"len":100,"y0":8,"y1":14,"par":24000,"min":8500},{"len":100,"y0":14,"y1":20,"par":22000,"min":8000},{"len":103.4,"y0":20,"y1":24,"par":22000,"min":7500},{"len":88,"y0":24,"y1":34.5,"par":26000,"min":9000}]'::jsonb
$$;
revoke all on function economy.tower_sections() from public;

create or replace function economy.tower_score(p_cleared int, p_ms int[], p_progress double precision, p_finished boolean, p_place int, p_revived boolean, p_ko int)
returns int language plpgsql immutable set search_path = '' as $$
declare
  v_sec jsonb := economy.tower_sections();
  v_sc bigint := greatest(p_cleared, 0) * 1000;
  v_k int := least(greatest(p_cleared, 0), 4);
  v_h double precision;
begin
  for i in 1 .. greatest(p_cleared, 0) loop
    v_sc := v_sc + greatest(0, floor(((v_sec -> (i - 1) ->> 'par')::double precision * 1.6 - coalesce(p_ms[i], 0)) / 100));
  end loop;
  if p_finished then v_h := (v_sec -> 4 ->> 'y1')::double precision;
  else v_h := (v_sec -> v_k ->> 'y0')::double precision
       + least(1, greatest(0, coalesce(p_progress, 0) / (v_sec -> v_k ->> 'len')::double precision))
       * ((v_sec -> v_k ->> 'y1')::double precision - (v_sec -> v_k ->> 'y0')::double precision);
  end if;
  v_sc := v_sc + floor(v_h * 10);
  if p_finished then v_sc := v_sc + (case p_place when 1 then 1000 when 2 then 600 when 3 then 300 else 0 end) + (case when p_revived then 0 else 500 end); end if;
  return v_sc + least(greatest(coalesce(p_ko, 0), 0), 20) * 50;
end $$;
revoke all on function economy.tower_score(int, int[], double precision, boolean, int, boolean, int) from public;

create table if not exists economy.tower_players (
  entry_id uuid primary key references public.waitlist_entries(id) on delete cascade,
  name text not null check (char_length(name) between 2 and 12),
  consented_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create table if not exists economy.tower_runs (
  id uuid primary key default gen_random_uuid(),
  entry_id uuid not null references public.waitlist_entries(id) on delete cascade,
  day int not null,
  started_at timestamptz not null default now(),
  last_at timestamptz not null default now(),
  cleared int not null default 0 check (cleared between 0 and 5),
  ms int[] not null default '{}',
  progress int not null default 0,
  revived boolean not null default true,
  ko int not null default 0,
  place int not null default 0,
  finished boolean not null default false,
  ended_at timestamptz,
  score int not null default 0,
  client text
);
create index if not exists tower_runs_entry_day on economy.tower_runs (entry_id, day);
create index if not exists tower_runs_day_score on economy.tower_runs (day, score desc);
create table if not exists economy.tower_sns (
  id bigint generated always as identity primary key,
  entry_id uuid not null references public.waitlist_entries(id) on delete cascade,
  day int not null,
  channel text not null, account text not null, post_id text not null, url text not null,
  status text not null default 'approved' check (status in ('approved', 'rejected')),
  note text, client text, created_at timestamptz not null default now(),
  unique (channel, post_id)
);
create index if not exists tower_sns_entry_day on economy.tower_sns (entry_id, day);
create table if not exists economy.tower_attempts (ip_hash text not null, at timestamptz not null default now());
create index if not exists tower_attempts_ip on economy.tower_attempts (ip_hash, at);
alter table economy.tower_players enable row level security; revoke all on economy.tower_players from public;
alter table economy.tower_runs enable row level security; revoke all on economy.tower_runs from public;
alter table economy.tower_sns enable row level security; revoke all on economy.tower_sns from public;
alter table economy.tower_attempts enable row level security; revoke all on economy.tower_attempts from public;

-- 같은 곳(IP 해시) 10분 60번 — 넘으면 false
create or replace function economy.tower_throttle() returns boolean
language plpgsql security definer set search_path = '' as $$
declare v_client text := economy.waitlist_client();
begin
  if v_client is null then return true; end if;
  delete from economy.tower_attempts where ip_hash = v_client and at < now() - interval '1 day';
  if (select count(*) from economy.tower_attempts where ip_hash = v_client and at > now() - interval '10 minutes') >= 60 then return false; end if;
  insert into economy.tower_attempts (ip_hash) values (v_client);
  return true;
end $$;
revoke all on function economy.tower_throttle() from public;

create or replace function economy.tower_entry(p_email text) returns uuid
language sql stable security definer set search_path = '' as $$
  select id from public.waitlist_entries where email = lower(btrim(coalesce(p_email, ''))) and status = 'registered'
$$;
revoke all on function economy.tower_entry(text) from public;

create or replace function economy.tower_status(p_entry uuid) returns jsonb
language plpgsql stable security definer set search_path = '' as $$
declare
  v_day int := economy.walkingday_today();
  v_sns int := (select count(*) from economy.tower_sns where entry_id = p_entry and day = v_day and status <> 'rejected');
  v_runs int := (select count(*) from economy.tower_runs where entry_id = p_entry and day = v_day);
begin
  return jsonb_build_object('ok', true,
    'name', (select name from economy.tower_players where entry_id = p_entry),
    'runs_left', greatest(0, 1 + least(v_sns, 3) - v_runs),
    'sns_left', greatest(0, 3 - v_sns),
    'best_today', (select max(score) from economy.tower_runs where entry_id = p_entry and day = v_day),
    'best_all', (select max(score) from economy.tower_runs where entry_id = p_entry));
end $$;
revoke all on function economy.tower_status(uuid) from public;

create or replace function public.tower_join(p_email text, p_name text, p_trap text default '')
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_email text := lower(btrim(coalesce(p_email, '')));
  v_name text := regexp_replace(btrim(coalesce(p_name, '')), '\s+', ' ', 'g');
  v_entry uuid;
begin
  if p_trap is distinct from '' then return jsonb_build_object('ok', false, 'code', 'not_registered'); end if;
  if not economy.tower_throttle() then return jsonb_build_object('ok', false, 'code', 'busy'); end if;
  if length(v_email) > 254 or not economy.waitlist_email_ok(v_email) then return jsonb_build_object('ok', false, 'code', 'email'); end if;
  v_entry := economy.tower_entry(v_email);
  if v_entry is null then return jsonb_build_object('ok', false, 'code', 'not_registered'); end if;
  if v_name !~ '^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9 _.\-]{2,12}$' then return jsonb_build_object('ok', false, 'code', 'name'); end if;
  insert into economy.tower_players (entry_id, name) values (v_entry, v_name)
  on conflict (entry_id) do update set name = excluded.name, updated_at = now();
  return economy.tower_status(v_entry);
end $$;
revoke all on function public.tower_join(text, text, text) from public;
grant execute on function public.tower_join(text, text, text) to anon, authenticated;

create or replace function public.tower_status(p_email text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_entry uuid;
begin
  if not economy.tower_throttle() then return jsonb_build_object('ok', false, 'code', 'busy'); end if;
  v_entry := economy.tower_entry(p_email);
  if v_entry is null then return jsonb_build_object('ok', false, 'code', 'not_registered'); end if;
  if not exists (select 1 from economy.tower_players where entry_id = v_entry) then return jsonb_build_object('ok', false, 'code', 'not_joined'); end if;
  return economy.tower_status(v_entry);
end $$;
revoke all on function public.tower_status(text) from public;
grant execute on function public.tower_status(text) to anon, authenticated;

create or replace function public.tower_start(p_email text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_entry uuid := economy.tower_entry(p_email);
  v_st jsonb;
  v_id uuid;
begin
  if coalesce((select (value #>> '{}')::boolean from public.economy_settings where key = 'tower_open'), true) is not true then return jsonb_build_object('ok', false, 'code', 'closed'); end if;
  if v_entry is null or not exists (select 1 from economy.tower_players where entry_id = v_entry) then return jsonb_build_object('ok', false, 'code', 'not_registered'); end if;
  perform pg_advisory_xact_lock(hashtext('tower:' || v_entry::text));
  v_st := economy.tower_status(v_entry);
  if (v_st ->> 'runs_left')::int <= 0 then return jsonb_build_object('ok', false, 'code', 'no_runs', 'sns_left', (v_st ->> 'sns_left')::int); end if;
  insert into economy.tower_runs (entry_id, day, client, score) values (v_entry, economy.walkingday_today(), economy.waitlist_client(), economy.tower_score(0, '{}', 0, false, 0, true, 0))
  returning id into v_id;
  return jsonb_build_object('ok', true, 'run', v_id, 'runs_left', (v_st ->> 'runs_left')::int - 1);
end $$;
revoke all on function public.tower_start(text) from public;
grant execute on function public.tower_start(text) to anon, authenticated;

create or replace function public.tower_ping(p_run uuid, p_section int, p_ms int)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_run economy.tower_runs;
  v_min int;
  v_ms int[];
begin
  select * into v_run from economy.tower_runs where id = p_run for update;
  if not found or v_run.ended_at is not null or v_run.started_at < now() - interval '30 minutes' then return jsonb_build_object('ok', false, 'code', 'run'); end if;
  if p_section is distinct from v_run.cleared + 1 or p_section > 5 then return jsonb_build_object('ok', false, 'code', 'section'); end if;
  v_min := (economy.tower_sections() -> (p_section - 1) ->> 'min')::int;
  if p_ms is null or p_ms < v_min or p_ms > 900000 or extract(epoch from (now() - v_run.last_at)) * 1000 < p_ms - 2000 then return jsonb_build_object('ok', false, 'code', 'time'); end if;
  v_ms := v_run.ms || p_ms;
  update economy.tower_runs set cleared = p_section, ms = v_ms, last_at = now(),
         score = economy.tower_score(p_section, v_ms, 0, p_section = 5, 0, true, 0)
   where id = p_run;
  return jsonb_build_object('ok', true, 'score', economy.tower_score(p_section, v_ms, 0, p_section = 5, 0, true, 0));
end $$;
revoke all on function public.tower_ping(uuid, int, int) from public;
grant execute on function public.tower_ping(uuid, int, int) to anon, authenticated;

create or replace function economy.tower_rank(p_entry uuid, p_day int) returns int
language sql stable security definer set search_path = '' as $$
  with best as (
    select distinct on (r.entry_id) r.entry_id, r.score, (select coalesce(sum(x), 0) from unnest(r.ms) x) as tot, r.started_at
    from economy.tower_runs r where p_day is null or r.day = p_day
    order by r.entry_id, r.score desc, (select coalesce(sum(x), 0) from unnest(r.ms) x), r.started_at
  )
  select (count(*) + 1)::int from best b, best me
  where me.entry_id = p_entry and (b.score > me.score or b.score = me.score and (b.tot < me.tot or b.tot = me.tot and b.started_at < me.started_at))
$$;
revoke all on function economy.tower_rank(uuid, int) from public;

create or replace function public.tower_end(p_run uuid, p_progress int, p_revived boolean, p_ko int, p_place int)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_run economy.tower_runs;
  v_fin boolean;
  v_len double precision;
  v_prog int; v_place int; v_ko int; v_score int;
begin
  select * into v_run from economy.tower_runs where id = p_run for update;
  if not found or v_run.started_at < now() - interval '30 minutes' then return jsonb_build_object('ok', false, 'code', 'run'); end if;
  if v_run.ended_at is not null then return jsonb_build_object('ok', false, 'code', 'used'); end if;
  v_fin := v_run.cleared >= 5;
  v_len := (economy.tower_sections() -> least(v_run.cleared, 4) ->> 'len')::double precision;
  v_prog := case when v_fin then 0 else least(greatest(coalesce(p_progress, 0), 0), floor(v_len)::int) end;
  v_place := case when v_fin then least(greatest(coalesce(p_place, 16), 1), 16) else 0 end;
  v_ko := least(greatest(coalesce(p_ko, 0), 0), 15);
  v_score := economy.tower_score(v_run.cleared, v_run.ms, v_prog, v_fin, v_place, coalesce(p_revived, true), v_ko);
  update economy.tower_runs set ended_at = now(), progress = v_prog, place = v_place, ko = v_ko, revived = coalesce(p_revived, true), finished = v_fin, score = v_score where id = p_run;
  return jsonb_build_object('ok', true, 'score', v_score,
    'rank_today', economy.tower_rank(v_run.entry_id, v_run.day), 'rank_all', economy.tower_rank(v_run.entry_id, null));
end $$;
revoke all on function public.tower_end(uuid, int, boolean, int, int) from public;
grant execute on function public.tower_end(uuid, int, boolean, int, int) to anon, authenticated;

create or replace function public.tower_board(p_email text default '')
returns jsonb language plpgsql stable security definer set search_path = '' as $$
declare
  v_me uuid := economy.tower_entry(p_email);
  v_day int := economy.walkingday_today();
  v jsonb;
begin
  with runs as (
    select r.*, (select coalesce(sum(x), 0) from unnest(r.ms) x) as tot from economy.tower_runs r
  ), best_today as (
    select distinct on (entry_id) * from runs where day = v_day order by entry_id, score desc, tot, started_at
  ), best_all as (
    select distinct on (entry_id) * from runs order by entry_id, score desc, tot, started_at
  ), rt as (
    select b.*, p.name, row_number() over (order by b.score desc, b.tot, b.started_at) as rk from best_today b join economy.tower_players p using (entry_id)
  ), ra as (
    select b.*, p.name, row_number() over (order by b.score desc, b.tot, b.started_at) as rk from best_all b join economy.tower_players p using (entry_id)
  )
  select jsonb_build_object(
    'today', coalesce((select jsonb_agg(jsonb_build_object('rank', rk, 'name', name, 'score', score, 'cleared', cleared, 'me', entry_id = v_me) order by rk) from rt where rk <= 50), '[]'::jsonb),
    'all', coalesce((select jsonb_agg(jsonb_build_object('rank', rk, 'name', name, 'score', score, 'cleared', cleared, 'me', entry_id = v_me) order by rk) from ra where rk <= 50), '[]'::jsonb),
    'me_today', (select jsonb_build_object('rank', rk, 'score', score) from rt where entry_id = v_me),
    'me_all', (select jsonb_build_object('rank', rk, 'score', score) from ra where entry_id = v_me),
    'players', (select count(*) from ra)) into v;
  return v;
end $$;
revoke all on function public.tower_board(text) from public;
grant execute on function public.tower_board(text) to anon, authenticated;

create or replace function public.tower_sns_submit(p_email text, p_url text, p_account text, p_consent boolean, p_trap text default '')
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_entry uuid := economy.tower_entry(p_email);
  v_day int := economy.walkingday_today();
  v_ch text; v_acct0 text; v_pid text; v_url text; v_account text;
begin
  if p_trap is distinct from '' then return jsonb_build_object('ok', false, 'code', 'url'); end if;
  if not economy.tower_throttle() then return jsonb_build_object('ok', false, 'code', 'busy'); end if;
  if p_consent is distinct from true then return jsonb_build_object('ok', false, 'code', 'consent'); end if;
  if v_entry is null or not exists (select 1 from economy.tower_players where entry_id = v_entry) then return jsonb_build_object('ok', false, 'code', 'not_registered'); end if;
  select x.channel, x.account, x.post_id, x.url into v_ch, v_acct0, v_pid, v_url from economy.walkingday_sns_parse(p_url) x;
  if v_ch is null then return jsonb_build_object('ok', false, 'code', 'url'); end if;
  v_account := coalesce(v_acct0, economy.walkingday_sns_account(p_account));
  if v_account is null then return jsonb_build_object('ok', false, 'code', 'account'); end if;
  perform pg_advisory_xact_lock(hashtext('tower:' || v_entry::text));
  if exists (select 1 from economy.tower_sns where channel = v_ch and post_id = v_pid) then return jsonb_build_object('ok', false, 'code', 'duplicate'); end if;
  if (select count(*) from economy.tower_sns where entry_id = v_entry and day = v_day and status <> 'rejected') >= 3 then return jsonb_build_object('ok', false, 'code', 'day_limit'); end if;
  insert into economy.tower_sns (entry_id, day, channel, account, post_id, url, client) values (v_entry, v_day, v_ch, v_account, v_pid, v_url, economy.waitlist_client());
  return economy.tower_status(v_entry);
end $$;
revoke all on function public.tower_sns_submit(text, text, text, boolean, text) from public;
grant execute on function public.tower_sns_submit(text, text, text, boolean, text) to anon, authenticated;
