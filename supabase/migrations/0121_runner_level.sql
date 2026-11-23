-- 0121 — 러너 레벨: 걸음으로 오르는 계정 레벨 · 하루 에너지 · 레벨 상자 · 배지 (2026-11-21)
--
-- 설계 docs/stat-overhaul-2026-11-21.md 14장 · 약속 docs/runner-level-contract-2026-11-21.md.
-- 사용자 "계정 자체의 레벨" → "복잡하게 하지 마 · 걸음 수로 레벨업 · 레벨 오르면 좋은 점" → "줘 · 패치해놔"(지난 걸음으로 채운 레벨에도 상자).
--   러너 걸음 = 판정이 CLEAN 인 러닝의 보상 걸음(없으면 경로 걸음) — 한국 날짜마다 48,000 까지. 에너지를 다 쓴 뒤 · 크루 러닝 걸음도 든다.
--   러너 레벨 = 1 + 러너 걸음 ÷ 10,000, 최대 30.
--   하루 에너지 = 5.5 + 0.55 × (Lv − 1), 가장 많이 16.5(에너지 1 = 600보 — 분당 165보로 Lv1 20분 · 레벨마다 2분 · Lv21 60분).
--     0121 전에 있던 계정은 예전 하루 에너지(10)보다 줄지 않는다. 신발 레벨은 더 이상 에너지를 정하지 않는다.
--   레벨이 오르면 보통 상자 1 · 5레벨마다 상급 상자 1 더(box_awards 'RUNNER:<Lv>' · 'RUNNER5:<Lv>', 한 번만) — 올릴 때 지난 걸음으로 채운 레벨도.
--   배지 Lv10 · 20 · 30(앱이 runner_badges 로 읽는다).
-- 되돌리기: runner_level_rule 을 'OFF' — 하루 에너지는 예전처럼 가진 신발 가운데 가장 높은 레벨, 러너 상자는 멈춘다(걸음은 계속 센다).
-- 함수 모양은 바꾸지 않았다(energy_max(uuid) 의 본문만). record_session 은 그대로(0120 이 마지막 정의다) — 러닝을 기록하면 트리거가 센다.

-- ════════════════════════════════════════════════════════════════════
--  1. 설정 · 표
-- ════════════════════════════════════════════════════════════════════
insert into public.economy_settings (key, value) values
  ('runner_level_rule', '"ON"'),         -- 'ON' = 하루 에너지 · 상자를 러너 레벨로 · 'OFF' = 예전(가진 신발 가운데 가장 높은 레벨)
  ('runner_level_steps', '10000'),       -- 한 레벨에 필요한 러닝 걸음
  ('runner_level_max', '30'),            -- 러너 레벨 가장 높이
  ('runner_energy_base', '5.5'),         -- Lv1 하루 에너지(5.5 × 600보 = 3,300보 ≈ 20분)
  ('runner_energy_per_level', '0.55'),   -- 레벨마다 더하는 하루 에너지(≈ 2분)
  ('runner_energy_cap', '16.5'),         -- 하루 에너지 가장 많이(≈ 60분 — Lv21)
  ('runner_legacy_energy', '10'),        -- 0121 전에 있던 계정이 지키는 하루 에너지(예전 값)
  ('runner_box_every', '5')              -- 이 레벨마다 상급 상자 하나 더
on conflict (key) do nothing;

create table if not exists economy.runner_levels (
  user_id uuid primary key references auth.users (id) on delete cascade,
  steps bigint not null default 0,            -- 러너 걸음(판정 CLEAN · 하루 48,000 까지의 합)
  level int not null default 1,               -- 지금 러너 레벨(걸음대로 — 러닝이 무효가 되면 내려갈 수 있다)
  legacy_energy numeric not null default 0,   -- 0121 전에 있던 계정이 지키는 하루 에너지(새 계정은 0)
  boxed_to int not null default 1,            -- 상자를 준 가장 높은 레벨(같은 레벨 상자는 한 번만)
  updated_at timestamptz not null default now()
);
alter table economy.runner_levels enable row level security;
comment on table economy.runner_levels is '러너 레벨 — 걸음으로 오르는 계정 레벨 · 하루 에너지 · 레벨 상자 (0121)';

-- ════════════════════════════════════════════════════════════════════
--  2. 셈
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.runner_on()
returns boolean language sql stable security definer set search_path = public, economy as $$
  select upper(coalesce(economy.setting('runner_level_rule') #>> '{}', 'ON')) = 'ON'
$$;

create or replace function economy.runner_level_steps()
returns int language sql stable security definer set search_path = public, economy as $$
  select greatest(coalesce(economy.fx('runner_level_steps', 10000), 10000), 1000)::int
$$;

create or replace function economy.runner_level_max()
returns int language sql stable security definer set search_path = public, economy as $$
  select least(greatest(coalesce(economy.fx('runner_level_max', 30), 30), 1), 100)::int
$$;

create or replace function economy.runner_level_of(p_steps bigint)
returns int language sql stable security definer set search_path = public, economy as $$
  select least(1 + (greatest(coalesce(p_steps, 0), 0) / economy.runner_level_steps())::int, economy.runner_level_max())
$$;

-- 그 레벨의 하루 에너지(예전 보장 빼고)
create or replace function economy.runner_energy_for(p_level int)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select least(greatest(economy.fx('runner_energy_base', 5.5), 0)
               + greatest(economy.fx('runner_energy_per_level', 0.55), 0) * greatest(coalesce(p_level, 1) - 1, 0),
               greatest(economy.fx('runner_energy_cap', 16.5), 0))
$$;

-- 에너지가 가장 많아지는 레벨
create or replace function economy.runner_energy_cap_level()
returns int language sql stable security definer set search_path = public, economy as $$
  select case when economy.fx('runner_energy_per_level', 0.55) <= 0 then 1
              else least(1 + greatest(ceil((economy.fx('runner_energy_cap', 16.5) - economy.fx('runner_energy_base', 5.5))
                                           / economy.fx('runner_energy_per_level', 0.55)), 0)::int, economy.runner_level_max()) end
$$;

-- 러너 걸음 — 판정이 CLEAN 인 러닝의 보상 걸음(없으면 경로 걸음), 한국 날짜마다 하루 걸음 상한(48,000)까지
create or replace function economy.runner_steps_of(p_user uuid)
returns bigint language sql stable security definer set search_path = public, economy as $$
  select coalesce(sum(d.s), 0)::bigint from (
    select least(sum(greatest(coalesce(s.reward_steps, s.backed_steps, 0), 0)), economy.max_daily_steps()) as s
      from public.walk_sessions s
     where s.user_id = p_user and s.verdict = 'CLEAN'
     group by economy.game_day(s.started_at)) d
$$;

-- 다시 세기 — 걸음 · 레벨을 고치고, 처음 닿은 레벨의 상자를 준다(러너 규칙이 켜져 있을 때만). 지금 레벨을 돌려준다
create or replace function economy.runner_refresh(p_user uuid)
returns int language plpgsql security definer set search_path = public, economy as $$
declare
  v_steps bigint;
  v_level int;
  v_boxed int;
  v_every int := greatest(coalesce(economy.fx('runner_box_every', 5), 5), 1)::int;
  v_day date := economy.game_day(now());
  v_l int;
begin
  if p_user is null then return null; end if;
  insert into economy.runner_levels (user_id) values (p_user) on conflict (user_id) do nothing;
  select boxed_to into v_boxed from economy.runner_levels where user_id = p_user for update;
  v_steps := economy.runner_steps_of(p_user);
  v_level := economy.runner_level_of(v_steps);
  if economy.runner_on() and v_level > v_boxed then
    for v_l in v_boxed + 1 .. v_level loop
      perform economy.box_grant(p_user, 'RUNNER:' || v_l, 'FREE', v_day);
      if v_l % v_every = 0 then
        perform economy.box_grant(p_user, 'RUNNER5:' || v_l, 'GIFT', v_day);
      end if;
    end loop;
    v_boxed := v_level;
  end if;
  update economy.runner_levels set steps = v_steps, level = v_level, boxed_to = v_boxed, updated_at = now()
   where user_id = p_user;
  return v_level;
end $$;

-- 러닝을 기록하거나(보상 걸음 · 판정이 바뀌면) 다시 센다
create or replace function economy.runner_on_session()
returns trigger language plpgsql security definer set search_path = public, economy as $$
begin
  perform economy.runner_refresh(new.user_id);
  return null;
end $$;
drop trigger if exists walk_sessions_runner on public.walk_sessions;
create trigger walk_sessions_runner after insert or update of reward_steps, backed_steps, verdict on public.walk_sessions
  for each row execute function economy.runner_on_session();

-- ── 하루 에너지 — 러너 레벨(규칙이 꺼지면 예전처럼 가진 신발 가운데 가장 높은 레벨) ──
create or replace function economy.energy_max(p_user uuid) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select case when economy.runner_on() then
           greatest(economy.runner_energy_for(coalesce((select r.level from economy.runner_levels r where r.user_id = p_user), 1)),
                    coalesce((select r.legacy_energy from economy.runner_levels r where r.user_id = p_user), 0))
         else economy.energy_max_for_level(coalesce((
           select max(case when s.origin in ('IMPORT', 'MINT') then 1 else s.level end)
             from public.market_sneakers s
            where s.owner_id = p_user and s.chain_state = 'APP' and s.status = 'OWNED'), 1)) end
$$;
revoke all on function economy.energy_max(uuid) from public;

-- ════════════════════════════════════════════════════════════════════
--  3. 앱이 읽는 것
-- ════════════════════════════════════════════════════════════════════
create or replace function public.runner_me()
returns jsonb language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  r economy.runner_levels;
  v_level int;
  v_steps bigint;
  v_per int := economy.runner_level_steps();
  v_max int := economy.runner_level_max();
  v_spe int := economy.steps_per_energy();
  v_energy numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '42501';
  end if;
  select * into r from economy.runner_levels where user_id = v_user;
  v_level := coalesce(r.level, 1);
  v_steps := coalesce(r.steps, 0);
  v_energy := economy.energy_max(v_user);
  return jsonb_build_object(
    'enabled', economy.runner_on(),
    'level', v_level,
    'max_level', v_max,
    'steps', v_steps,
    'level_steps', v_per,
    'level_from', (v_level - 1)::bigint * v_per,
    'next_at', case when v_level >= v_max then null else v_level::bigint * v_per end,
    'to_next', case when v_level >= v_max then null else greatest(v_level::bigint * v_per - v_steps, 0) end,
    'energy_max', v_energy,
    'energy_minutes', round(v_energy * v_spe / 165),
    'level_energy_minutes', round(economy.runner_energy_for(v_level) * v_spe / 165),
    'next_energy_minutes', case when v_level >= v_max then null else round(economy.runner_energy_for(v_level + 1) * v_spe / 165) end,
    'energy_cap_level', economy.runner_energy_cap_level(),
    'legacy_floor', coalesce(r.legacy_energy, 0) > economy.runner_energy_for(v_level),
    'badge', case when v_level >= 30 then 30 when v_level >= 20 then 20 when v_level >= 10 then 10 else 0 end,
    'boxes', jsonb_build_object(
      'free', (select count(*) from economy.box_awards b where b.user_id = v_user and b.source like 'RUNNER:%'),
      'gift', (select count(*) from economy.box_awards b where b.user_id = v_user and b.source like 'RUNNER5:%')),
    'rewards', jsonb_build_object(
      'energy_minutes_per_level', round(greatest(economy.fx('runner_energy_per_level', 0.55), 0) * v_spe / 165),
      'free_every_level', 1,
      'gift_every', greatest(coalesce(economy.fx('runner_box_every', 5), 5), 1)::int,
      'badge_levels', jsonb_build_array(10, 20, 30)));
end $$;
revoke all on function public.runner_me() from public, anon;
grant execute on function public.runner_me() to authenticated;

-- 크루 명단 · 대기실의 배지 — 배지(Lv10 이상)가 있는 사람만 · 한 번에 200명까지
create or replace function public.runner_badges(p_users uuid[])
returns table (user_id uuid, level int, badge int)
language sql stable security definer set search_path = public, economy as $$
  select r.user_id, r.level, case when r.level >= 30 then 30 when r.level >= 20 then 20 else 10 end
    from economy.runner_levels r
   where auth.uid() is not null and economy.runner_on()
     and r.level >= 10
     and r.user_id = any (coalesce(p_users[1:200], '{}'::uuid[]))
$$;
revoke all on function public.runner_badges(uuid[]) from public, anon;
grant execute on function public.runner_badges(uuid[]) to authenticated;

revoke all on function economy.runner_on() from public, anon, authenticated;
revoke all on function economy.runner_level_steps() from public, anon, authenticated;
revoke all on function economy.runner_level_max() from public, anon, authenticated;
revoke all on function economy.runner_level_of(bigint) from public, anon, authenticated;
revoke all on function economy.runner_energy_for(int) from public, anon, authenticated;
revoke all on function economy.runner_energy_cap_level() from public, anon, authenticated;
revoke all on function economy.runner_steps_of(uuid) from public, anon, authenticated;
revoke all on function economy.runner_refresh(uuid) from public, anon, authenticated;
revoke all on function economy.runner_on_session() from public, anon, authenticated;

-- ════════════════════════════════════════════════════════════════════
--  4. 운영자 손잡이 — 0120 목록 + 러너 넷(규칙 스위치 · 예전 보장 · 상급 간격 · 가장 높이는 SQL 창에서만)
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.ops_setting_rule(p_key text, out kind text, out lo numeric, out hi numeric, out label text)
returns record language sql immutable as $$
  select r.kind, r.lo, r.hi, r.label from (values
    ('points_per_step', 'num', 0.001, 0.05, '걸음당 SUP(효율 100 기준)'),
    ('repair_share', 'num', 0.1, 0.9, '수리비 = 걸어서 버는 돈의 몫'),
    ('reward_luck', 'luck', 0.3, 2.0, '보상 운 [가장 작게, 가장 자주, 가장 크게]'),
    ('wear_luck', 'luck', 0.3, 2.5, '닳는 양 운 [가장 작게, 가장 자주, 가장 크게]'),
    ('free_repair_hours', 'num', 0, 720, '무료 수리 간격(시간, 0 = 끔)'),
    ('boost_shop_enabled', 'bool', null, null, '부스터 판매'),
    ('daily_earn_cap', 'num', 100, 100000, '하루 적립 상한(가운데 일반 신발 기준)'),
    ('no_gps_daily_cap', 'num', 0, 10000, '경로 없는 러닝 하루 상한'),
    ('upload_daily_cap', 'num', 100, 200000, '하루에 올릴 수 있는 적립 상한(가운데 일반 기준)'),
    ('daily_free_draws', 'num', 0, 10, '하루 출석 상자'),
    ('run_box_steps', 'steps', 100, 100000, '러닝 상자 걸음(오늘 보상 걸음)'),
    ('weekly_box_steps', 'num', 0, 1000000, '주간 상급 상자 걸음(0 = 끔)'),
    ('streak_box_days', 'num', 0, 60, '연속 목표 상급 상자 날 수(0 = 끔)'),
    ('box_price', 'num', 0, 100000, '상자 값(SUP, 0 = 안 팜)'),
    ('box_daily_buy', 'num', 0, 50, '상자 하루 살 수 있는 수'),
    ('premium_box_price', 'num', 0, 1000000, '상급 상자 값(SUP, 0 = 안 팜)'),
    ('premium_box_weekly_buy', 'num', 0, 50, '상급 상자 한 주 살 수 있는 수'),
    ('forge_material_min_durability', 'num', 0, 100, '예전 강화(예전 앱) 재료 최소 내구도(%)'),
    ('app_current_version_code', 'num', 1, 999, '아이템 가방이 있는 앱 판(이 번호부터)'),
    ('forge_xp_step', 'num', 1.5, 6, '강화 경험치: 등급 한 칸의 값(배)'),
    ('forge_xp_need', 'num', 1, 3.2, '강화 경험치: Lv1 통의 크기(일반 켤레)'),
    ('forge_xp_spec', 'pair', 0.3, 2, '강화 경험치: 재료 스펙 [가장 낮게, 가장 높게]'),
    ('forge_xp_crit_chance', 'num', 0, 0.5, '강화 경험치: 대성공 확률'),
    ('forge_xp_crit_mult', 'num', 1, 5, '강화 경험치: 대성공 배수'),
    ('forge_xp_inherit', 'num', 0, 1, '강화 경험치: 강화한 재료가 물려주는 몫'),
    ('forge_xp_sup_per_unit', 'num', 1, 1000, '강화 경험치: 일반 1켤레값(SUP — 닳은 재료의 수리비를 이것으로 나눠 뺀다)'),
    ('forge_xp_max_materials', 'num', 1, 20, '강화 경험치: 한 번에 넣는 최대 켤레'),
    ('goal_min', 'num', 1000, 20000, '하루 목표 가장 낮게(걸음)'),
    ('goal_max', 'num', 4000, 100000, '하루 목표 가장 높게(걸음)'),
    ('goal_default', 'num', 1000, 100000, '새 사용자 하루 목표(걸음)'),
    ('worn_rate', 'num', 0, 0.45, '다 닳은 신발의 적립 비율(0 = 적립 없음 · 걸음도 안 셈, 0.45 를 넘기면 수리하지 않는 쪽이 이득)'),
    ('eff_wear_exp', 'num', 0, 1, '효율 포인트가 닳는 속도를 올리는 세기(0 = 끔, 1 = 버는 속도만큼)'),
    ('eff_energy_exp', 'num', 0, 1, '효율 포인트가 에너지 소모를 올리는 세기(0 = 끔, 1 = 버는 속도만큼)'),
    ('energy_per_level', 'num', 0, 5, '레벨마다 늘어나는 하루 에너지(가진 신발 중 최고 레벨 기준)'),
    ('forge_xp_level_growth', 'num', 0, 0.33, '강화 경험치: 통이 레벨마다 커지는 몫(Lv1 통의 배)'),
    ('together_min_minutes', 'num', 3, 60, '함께 달린 사람: 그 방에서 달린 시간(분) 이상'),
    ('together_min_steps', 'num', 0, 10000, '함께 달린 사람: 경로로 확인된 걸음 이상'),
    ('daily_max_hour_mult', 'num', 1, 20, '하루 최대 SUP = 그 신발 1시간 값 × 이 값'),
    ('together_goal_share', 'num', 0, 1, '함께 달린 사람: 목표 거리의 몫 이상(0.5 = 절반)'),
    ('app_min_version_code', 'num', 0, 999, '이 판보다 낮은 앱은 업데이트해야 쓴다(스토어 번호 · 0 = 끔)'),
    ('crew_blend_share', 'num', 0, 0.5, '크루 러닝 효율 가운데 함께 달린 크루원 평균의 몫(0.2 = 내 신발 8 : 평균 2 · 0 = 끔)'),
    ('runner_level_steps', 'num', 1000, 100000, '러너 레벨: 한 레벨에 필요한 러닝 걸음'),
    ('runner_energy_base', 'num', 1, 30, '러너 레벨: Lv1 하루 에너지(1 = 600보 · 5.5 = 약 20분)'),
    ('runner_energy_per_level', 'num', 0, 3, '러너 레벨: 레벨마다 늘어나는 하루 에너지(0.55 = 약 2분)'),
    ('runner_energy_cap', 'num', 1, 60, '러너 레벨: 하루 에너지 가장 많이(16.5 = 약 60분)')
  ) r(key, kind, lo, hi, label) where r.key = p_key
$$;

create or replace function public.ops_economy_settings(p_token text)
returns jsonb language plpgsql security definer set search_path = public, economy as $$
declare v_actor text := economy.ops_actor(p_token);
begin
  return jsonb_build_object('ok', true, 'settings', (
    select jsonb_agg(jsonb_build_object('key', k.key, 'value', s.value, 'label', (economy.ops_setting_rule(k.key)).label,
                                        'kind', (economy.ops_setting_rule(k.key)).kind, 'updated_at', s.updated_at) order by k.o)
      from (values ('points_per_step', 1), ('repair_share', 2), ('reward_luck', 3), ('wear_luck', 4), ('free_repair_hours', 5),
                   ('boost_shop_enabled', 6), ('daily_earn_cap', 7), ('no_gps_daily_cap', 8), ('upload_daily_cap', 9),
                   ('daily_free_draws', 10), ('run_box_steps', 11), ('weekly_box_steps', 12), ('streak_box_days', 13),
                   ('box_price', 14), ('box_daily_buy', 15), ('premium_box_price', 16), ('premium_box_weekly_buy', 17),
                   ('forge_material_min_durability', 18), ('app_current_version_code', 19),
                   ('forge_xp_step', 20), ('forge_xp_need', 21), ('forge_xp_spec', 22), ('forge_xp_crit_chance', 23),
                   ('forge_xp_crit_mult', 24), ('forge_xp_inherit', 25), ('forge_xp_sup_per_unit', 26),
                   ('forge_xp_max_materials', 27), ('goal_min', 28), ('goal_max', 29), ('goal_default', 30),
                   ('worn_rate', 31), ('eff_wear_exp', 32), ('eff_energy_exp', 33), ('energy_per_level', 34),
                   ('forge_xp_level_growth', 35), ('together_min_minutes', 36), ('together_min_steps', 37),
                   ('daily_max_hour_mult', 38), ('together_goal_share', 39), ('app_min_version_code', 40),
                   ('crew_blend_share', 41), ('runner_level_steps', 42), ('runner_energy_base', 43),
                   ('runner_energy_per_level', 44), ('runner_energy_cap', 45)) k(key, o)
      left join public.economy_settings s on s.key = k.key));
end $$;

-- ════════════════════════════════════════════════════════════════════
--  5. 한 번만: 지금 있는 계정 — 예전 하루 에너지를 지키고, 지난 러닝 걸음으로 레벨을 채우고, 채운 레벨의 상자를 준다(사용자 "줘")
-- ════════════════════════════════════════════════════════════════════
do $$
begin
  if not exists (select 1 from public.economy_settings where key = 'runner_level_20261121') then
    insert into economy.runner_levels (user_id, legacy_energy)
    select u.id, greatest(coalesce(economy.fx('runner_legacy_energy', 10), 10), 0) from auth.users u
    on conflict (user_id) do update set legacy_energy = greatest(economy.runner_levels.legacy_energy, excluded.legacy_energy);
    perform economy.runner_refresh(u.id) from auth.users u;
    insert into public.economy_settings (key, value) values ('runner_level_20261121', to_jsonb(now()));
  end if;
end $$;
