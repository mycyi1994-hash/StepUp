-- ════════════════════════════════════════════════════════════════════
--  0042 — 신발 뽑기는 모두 무료 (2026-09-27 사용자 결정, docs/redesign/shoe-draw)
--
--  무료 뽑기  첫 가입 10회(0023 그대로) + 매일 3회. 매일 몫은 한국 날짜로 세고, 안 쓴 것은
--            다음 날로 넘기지 않는다. 오늘 몫부터 쓰고(자정에 사라지니까), 그다음 가입 선물을 쓴다.
--  상급 뽑기  지갑을 처음 연결하면 10회(0025 그대로, 그 첫 번은 Genesis · 에픽 이상).
--            연결한 **뒤에 시작한** 러닝의 경로 거리(서버가 인정한 gps_credit_m) 1km 마다 1회,
--            하루 10회까지. 1km 에 못 미친 거리는 다음 러닝으로 이어진다(한도를 넘은 1km 는 버린다).
--            상급은 레어 이상에서 굴린다 — 일반 뽑기와 같은 가중치 표의 위쪽이라 씨앗 검증이 그대로다.
--  SUP 로 뽑기(draw_paid)는 닫는다 — 예전 앱이 불러도 SUP 가 나가지 않는다.
--
--  운영 값(economy_settings)으로 둔다: daily_free_draws · premium_run_step_m ·
--  premium_run_daily_cap · premium_min_rarity. 바꿀 때는 admin_economy_set.
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values
  ('daily_free_draws',      '3'::jsonb),
  ('premium_run_step_m',    '1000'::jsonb),
  ('premium_run_daily_cap', '10'::jsonb),
  ('premium_min_rarity',    '"RARE"'::jsonb)
on conflict (key) do nothing;

-- 러닝으로 받은 상급 뽑기('RUN')는 지갑 선물('BONUS')과 따로 센다 — 화면이 출처마다 남은 수를 보인다
alter table public.draw_grants drop constraint if exists draw_grants_kind_check;
alter table public.draw_grants add constraint draw_grants_kind_check check (kind in ('FREE', 'BONUS', 'RUN'));

-- 매일 무료 — 날짜마다 쓴 수. 오늘 줄이 없으면 오늘은 아직 안 썼다.
create table if not exists public.draw_daily (
  user_id uuid not null references auth.users on delete cascade,
  day date not null,
  used int not null default 0 check (used >= 0),
  primary key (user_id, day)
);
alter table public.draw_daily enable row level security;
revoke all on public.draw_daily from anon, authenticated;

-- 상급 뽑기까지 모은 거리 — 1km 에 못 미친 나머지와 오늘 러닝으로 준 수
create table if not exists public.premium_run_progress (
  user_id uuid primary key references auth.users on delete cascade,
  meters double precision not null default 0 check (meters >= 0),
  day date,
  day_granted int not null default 0 check (day_granted >= 0),
  updated_at timestamptz not null default now()
);
alter table public.premium_run_progress enable row level security;
revoke all on public.premium_run_progress from anon, authenticated;

/*
 * 러닝이 기록될 때(record_session 이 walk_sessions 에 넣을 때) 상급 뽑기를 준다.
 * 무효 러닝 · 경로로 잰 거리가 없는 러닝 · 지갑을 연결하기 전에 시작한 러닝은 세지 않는다.
 * 같은 사람의 기록은 record_session 이 잡은 잠금 안에서 들어오고, 진행 줄도 잠근다.
 */
create or replace function economy.premium_run_credit() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare
  v_linked timestamptz;
  v_step double precision := economy.setting_num('premium_run_step_m');
  v_cap int := greatest(coalesce(economy.setting_num('premium_run_daily_cap'), 0)::int, 0);
  v_day date := economy.game_day(now());
  v_meters double precision;
  v_pday date;
  v_given int;
  v_total double precision;
  v_whole int;
  v_give int;
begin
  if new.verdict = 'VOID' or coalesce(new.gps_credit_m, 0) <= 0 or coalesce(v_step, 0) <= 0 then
    return null;
  end if;
  select w.linked_at into v_linked from public.wallet_links w where w.user_id = new.user_id;
  if v_linked is null or new.started_at < v_linked then
    return null;
  end if;

  insert into public.premium_run_progress (user_id) values (new.user_id)
  on conflict (user_id) do nothing;
  select p.meters, p.day, p.day_granted into v_meters, v_pday, v_given
    from public.premium_run_progress p where p.user_id = new.user_id for update;
  if v_pday is distinct from v_day then
    v_given := 0;
  end if;

  v_total := v_meters + new.gps_credit_m;
  v_whole := floor(v_total / v_step)::int;
  v_give := least(v_whole, greatest(v_cap - v_given, 0));

  update public.premium_run_progress
     set meters = v_total - v_whole * v_step,
         day = v_day,
         day_granted = v_given + v_give,
         updated_at = now()
   where user_id = new.user_id;

  if v_give > 0 then
    insert into public.draw_grants (user_id, kind, granted) values (new.user_id, 'RUN', v_give)
    on conflict (user_id, kind) do update set granted = public.draw_grants.granted + excluded.granted;
  end if;
  return null;
end $$;
revoke all on function economy.premium_run_credit() from public;

drop trigger if exists walk_sessions_premium_run on public.walk_sessions;
create trigger walk_sessions_premium_run
  after insert on public.walk_sessions
  for each row execute function economy.premium_run_credit();

-- 무료 뽑기 한 번 — 오늘 몫부터, 그다음 첫 가입 선물. 남은 것이 없으면 23514.
create or replace function public.draw_free()
returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_daily int := greatest(coalesce(economy.setting_num('daily_free_draws'), 0)::int, 0);
  v_day date := economy.game_day(now());
  v_used int;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  select coalesce((select d.used from public.draw_daily d where d.user_id = v_user and d.day = v_day), 0)
    into v_used;
  if v_used < v_daily then
    insert into public.draw_daily (user_id, day, used) values (v_user, v_day, 1)
    on conflict (user_id, day) do update set used = public.draw_daily.used + 1;
  else
    update public.draw_grants set used = used + 1
     where user_id = v_user and kind = 'FREE' and used < granted;
    if not found then
      raise exception '무료 뽑기가 남아 있지 않습니다' using errcode = '23514';
    end if;
  end if;

  return economy.draw_create(v_user, 'FREE_DRAW');
end $$;

-- SUP 로 뽑기는 없다 — 예전 앱이 불러도 돈이 나가지 않는다
create or replace function public.draw_paid()
returns bigint
language plpgsql security definer set search_path = public, economy as $$
begin
  raise exception '지금은 모든 뽑기가 무료예요' using errcode = '23514';
end $$;

/*
 * 상급 뽑기 한 번 — 지갑 첫 연결 선물부터(그 첫 번이 Genesis · 에픽 이상), 그다음 러닝으로 받은 기회.
 * 보너스 뽑기(0025)처럼 체인에서 발행된다. 새 신발 번호를 돌려준다(결과는 서버가 정한 것).
 */
create or replace function public.premium_draw()
returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_wallet text;
  v_genesis boolean := false;
  v_min text := nullif(economy.setting('premium_min_rarity') #>> '{}', '');
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
    from public.draw_grants g
   where g.user_id = v_user and g.kind = 'BONUS' and g.used < g.granted;
  if found then
    update public.draw_grants
       set used = used + 1,
           genesis_used = genesis_used + case when v_genesis then 1 else 0 end
     where user_id = v_user and kind = 'BONUS';
  else
    v_genesis := false;
    update public.draw_grants set used = used + 1
     where user_id = v_user and kind = 'RUN' and used < granted;
    if not found then
      raise exception '상급 뽑기가 남아 있지 않습니다' using errcode = '23514';
    end if;
  end if;

  v_id := economy.draw_create(v_user, 'BONUS_DRAW',
                              case when v_genesis then 'EPIC' else v_min end, coalesce(v_genesis, false));
  update public.market_sneakers set chain_state = 'WITHDRAWING' where id = v_id;

  insert into public.chain_ops (id, user_id, kind, wallet, sneaker_id, deadline)
  values (v_op, v_user, 'BONUS_MINT', v_wallet, v_id, economy.op_deadline());
  return v_id;
end $$;

revoke all on function public.premium_draw() from public, anon;
grant execute on function public.premium_draw() to authenticated;

/*
 * 뽑기 화면 한 장에 필요한 수 — 출처마다 **남은** 수로 준다(처음 준 수나 평생 합과 섞지 않는다).
 *   gift_on_link  아직 지갑을 연결한 적이 없으면 첫 연결에 받을 상급 수(0025 의 10), 있으면 0
 *   run_progress_m  다음 상급 뽑기까지 모은 거리(m) — 1km 에 못 미친 나머지
 */
create or replace function public.draw_status()
returns table (
  daily_left int,
  daily_total int,
  signup_left int,
  signup_granted int,
  wallet_linked boolean,
  gift_on_link int,
  gift_left int,
  run_left int,
  genesis_left int,
  run_progress_m double precision,
  run_step_m int,
  run_today int,
  run_daily_cap int,
  chain_paused boolean
)
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_day date := economy.game_day(now());
  v_daily int := greatest(coalesce(economy.setting_num('daily_free_draws'), 0)::int, 0);
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return query
  select
    greatest(v_daily - coalesce((select d.used from public.draw_daily d
                                  where d.user_id = v_user and d.day = v_day), 0), 0),
    v_daily,
    coalesce((select g.granted - g.used from public.draw_grants g where g.user_id = v_user and g.kind = 'FREE'), 0),
    coalesce((select g.granted from public.draw_grants g where g.user_id = v_user and g.kind = 'FREE'), 0),
    exists (select 1 from public.wallet_links w where w.user_id = v_user),
    case when exists (select 1 from public.wallet_history h where h.user_id = v_user) then 0 else 10 end,
    coalesce((select g.granted - g.used from public.draw_grants g where g.user_id = v_user and g.kind = 'BONUS'), 0),
    coalesce((select g.granted - g.used from public.draw_grants g where g.user_id = v_user and g.kind = 'RUN'), 0),
    coalesce((select g.genesis_granted - g.genesis_used from public.draw_grants g
               where g.user_id = v_user and g.kind = 'BONUS'), 0),
    coalesce((select p.meters from public.premium_run_progress p where p.user_id = v_user), 0),
    coalesce(economy.setting_num('premium_run_step_m'), 1000)::int,
    coalesce((select case when p.day = v_day then p.day_granted else 0 end
                from public.premium_run_progress p where p.user_id = v_user), 0),
    coalesce(economy.setting_num('premium_run_daily_cap'), 0)::int,
    coalesce((economy.setting('chain_paused') #>> '{}')::boolean, false);
end $$;

revoke all on function public.draw_status() from public, anon;
grant execute on function public.draw_status() to authenticated;
