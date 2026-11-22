-- 0122 경제 점검 고침 (2026-11-22) — 사용자 "버그는 다 고치고 · a 는 너가 알아서해봐(인플레이션 막게) · b 는 괜찮음 · c 는 한시적"
-- 점검: docs/점검-2026-11-22-경제.md(네 갈래 — 들어오는 SUP · 여러 계정 · 쓰는 곳 · 튼튼함). tools/build_0122.py 가 만든다(고칠 함수는 마지막 정의를 복사).
--   1. 예전 앱 market_import 가 IMPORT 신발의 레벨을 올려 상급 상자 6개 — 다시 올려도 레벨을 바꾸지 않고, 레벨 상자는 IMPORT · MINT 를 뺀다
--   2. 크루 러닝: 소식 없이 달린 시간을 올릴 때 묶어 둔 체력까지 썼다 — 남의 묶인 몫은 빼고 쓴다. 묶는 사람도 넓힌다:
--      지금 박동을 보내며 달리는 중이 아니고 함께 달린 사람으로 올린 러닝도 없는 사람(10분 가짜 박동 뒤 빠진 부계정 · 박동 없는 사람)
--   3. 러너 걸음: 상한에 걸려 FLAGGED 가 된 러닝도 센다 — 무효 · 과속으로 멈춘 러닝만 뺀다
--   4. 하루 최대 SUP 에 닿아도 목표를 채운 날은 남긴다(연속 · 7일 상급 상자)
--   5. 크루 효율 8 : 2 — 평균에 각자 달린 만큼 무게(남은 내 몫까지), 보상 걸음 0(보류 계정)은 함께 달린 사람이 아니다
--   6. 강화 화면의 하루 에너지는 러너 레벨 에너지 — Lv10 이상이면 모든 재료가 '아까운 재료'로 찍혔다
--   7. 함께 달린 보너스 정산은 남의 원장 자물쇠를 기다리지 않고 잡아 본다 — 바쁘면 미뤄 두고 그 사람이 다음에 올릴 때(동시에 올려 하루 최대를 넘던 것)
--   8. 번개 보너스는 글 하나에 한 번 — 원격 번개도
--   9. 크루 보너스 규칙을 ALL 로 되돌렸다 다시 켜도 인원 보너스를 두 번 주지 않는다 · runner_level_steps 는 10,000 아래로 못 낮춘다
--  10. 코스 완주 · 도전 보상도 하루 최대 SUP 에 든다(코스는 남은 만큼만, 도전은 상금 그대로 — 그 뒤 러닝이 덜 받는다)
--  11. 가입 선물 무료 뽑기 10개 — 첫 러닝 상자가 먼저 와도 받는다
--  12. (0121) 러닝 뒤 "새로 받은 보상"에 러너 레벨 상자가 "러닝 상자"로 섞이던 것
--   A. 인플레이션: 러너 레벨의 하루 에너지 상한을 10(약 36분 — 0121 전 모두의 값)으로. 새 계정은 20분부터 그대로, 레벨 상자 · 배지도 그대로.
--      대격변(기본 적립 낮춤)을 켤 때 runner_energy_cap 을 16.5(약 60분)로 함께 올린다
-- 무료 수리(48시간에 한 켤레 가득)는 한시적(사용자) — 끝낼 때 free_repair_hours = 0.

-- ════════════════════════════════════════════════════════════════════
--  0. 설정 · 칸
-- ════════════════════════════════════════════════════════════════════
alter table economy.goal_days add column if not exists met_at timestamptz;
comment on column economy.goal_days.met_at is '그 날의 목표를 채운 때 — 하루 최대 SUP 에 막혀 목표 보너스를 못 받아도 연속 · 7일 상자는 이것으로 센다 (0122)';

create table if not exists economy.together_pending (
  party_id bigint not null,
  user_id uuid not null references auth.users (id) on delete cascade,
  at timestamptz not null default now(),
  primary key (party_id, user_id)
);
alter table economy.together_pending enable row level security;
comment on table economy.together_pending is '함께 달린 보너스 정산을 미뤄 둔 사람 — 정산할 때 그 사람이 다른 것을 기록하던 중이었다. 그 사람이 다음에 올릴 때 다시 정산한다 (0122)';

-- ════════════════════════════════════════════════════════════════════
--  1. 예전 앱 신발 등록 — 레벨을 따라가지 않는다 · 레벨 상자는 앱 안 신발만
-- ════════════════════════════════════════════════════════════════════
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
  v_count int;
begin
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  select sneaker_id into v_id from public.market_imports
   where user_id = v_user and local_id = p_local_id;
  if v_id is not null then
    -- 0122: 다시 올려도 레벨을 따라가지 않는다 — 폰이 적은 레벨로 서버 신발의 레벨이 오르면 레벨 상자(0116)가 나갔다(아무 계정이나 상급 6개)
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
  '폰의 신발을 거래소 장부에 올린다. 같은 신발은 한 번만, 계정당 상한이 있다. 다시 올려도 레벨은 바꾸지 않는다 (0122)';

-- 신발이 5 · 10 · 15 …레벨에 닿으면 상급 상자 — 계정마다 그 레벨 처음 한 번. 0122: 폰에서 올린 신발(IMPORT) · 체인 신발(MINT)은 뺀다
create or replace function economy.box_on_level() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare v_l int;
begin
  if new.owner_id is null or new.origin in ('IMPORT', 'MINT') or new.level / 5 <= old.level / 5 then
    return null;
  end if;
  for v_l in select generate_series((old.level / 5 + 1) * 5, (new.level / 5) * 5, 5) loop
    perform economy.box_grant(new.owner_id, 'LEVEL:' || v_l, 'GIFT', economy.game_day(now()));
  end loop;
  return null;
end $$;

-- ════════════════════════════════════════════════════════════════════
--  2. 크루 러닝 — 묶인 체력(묶는 사람을 넓힌다 · 남의 묶인 몫은 쓰지 않는다)
-- ════════════════════════════════════════════════════════════════════
-- 묶어 둔 몫(R3) — 0122: 맡긴 것 가운데 안 쓴 몫을 묶는 사람을 넓힌다. 지금 박동을 보내며 달리는 중(30초 안)이 아니고,
-- 함께 달린 사람으로 올린 러닝도 없으면 묶는다. 0120 은 "서버가 센 시간 10분 미만"만 묶어, 부계정이 10분 넘게 가짜 박동을 보내고 빠지면
-- 남은 둘이 그 몫까지 썼다(점검 2차 E1 — 1,217 SUP). 함께 달린 사람으로 올리면 풀린다 · 끝까지 올리지 않으면 쓴 만큼만 남는다
--   · 올렸다 → 함께 달린 사람이 아니면 묶는다(0120 ②)
--   · 박동을 보내며 달리는 중(30초 안) → 묶지 않는다
--   · 끝냈다(ENDED)인데 함께 달린 사람으로 올린 러닝이 없다 → 묶는다(10분 가짜 박동 뒤 끝낸 부계정 — 올리면 풀린다)
--   · 5분 넘게 소식이 없다 → 묶는다(박동 없는 부계정 · 출발을 놓친 사람 — 올리면 풀린다, 돌려받기는 0120 그대로 언제든)
--   · 그 밖(일시정지 · 잠깐 끊김) → 0120 ① 그대로(서버가 센 시간 10분 미만만) — 정직한 러너의 짧은 끊김으로 남의 적립이 멈추지 않게
create or replace function economy.crew_pool_held_where(r economy.crew_runs, p_at timestamptz)
returns boolean language sql stable security definer set search_path = public, economy as $$
  select case
    when r.session_id is not null then
      not exists (select 1 from public.walk_sessions x where x.id = r.session_id and economy.together_qualifies(x))
    when r.state = 'RUNNING' and r.last_beat_at >= p_at - interval '30 seconds' then false
    when r.state = 'ENDED' then true
    when r.last_beat_at < p_at - interval '5 minutes' and r.state <> 'PAUSED' then true
    else r.active_sec < economy.together_min_sec()
  end
$$;
create or replace function economy.crew_pool_held(p_crew uuid, p_day date, p_at timestamptz)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select case when not economy.together_on('CREW') then 0 else coalesce((
    select sum(greatest(r.contribution - r.consumed, 0)) from economy.crew_runs r
     where r.crew_id = p_crew and r.day = p_day and economy.crew_pool_held_where(r, p_at)), 0) end
$$;
-- 그 가운데 p_user 의 줄은 빼고 — 올리는 사람이 자기 맡긴 몫은 쓴다
create or replace function economy.crew_pool_held_except(p_crew uuid, p_day date, p_at timestamptz, p_user uuid)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select case when not economy.together_on('CREW') then 0 else coalesce((
    select sum(greatest(r.contribution - r.consumed, 0)) from economy.crew_runs r
     where r.crew_id = p_crew and r.day = p_day and r.user_id <> p_user and economy.crew_pool_held_where(r, p_at)), 0) end
$$;


-- ════════════════════════════════════════════════════════════════════
--  3. 함께 달린 사람 · 크루 효율 8 : 2 의 무게
-- ════════════════════════════════════════════════════════════════════
-- 0120 + 보상 걸음이 있어야(적립이 보류된 계정 · 경로가 받쳐 주지 않은 걸음은 함께 달린 사람이 아니다)
create or replace function economy.together_qualifies(s public.walk_sessions)
returns boolean language sql stable security definer set search_path = public, economy as $$
  select s.verdict <> 'VOID' and s.gps_backed
     and coalesce(s.reward_steps, 1) > 0
     and coalesce(s.verified_steps, 0) >= economy.together_min_steps()
     and coalesce(s.duration_sec, 0) >= economy.together_min_sec()
     and coalesce(s.gps_credit_m, 0) >= coalesce(economy.together_goal_m(s.party_id, s.user_id), 0)
$$;

-- 크루 평균 효율 배수의 아래 끝 — 0122: 사람마다 무게(효율 뺀 바탕 together_unit = 보상 걸음 × 걸음당 SUP × 배수 × 운)를 두되
-- 남의 무게는 내 무게까지만(least). 10분 달린 센 신발이 60분 달린 약한 신발의 평균을 사람 수만큼 끌어올리던 것을 막는다(+58% → 약 +13%).
-- 아직 올리지 않은 출발한 사람은 무게를 몰라 내 무게로(가장 크게) 넣어 본다 — 그래서 실제 평균은 이 값 아래로 내려가지 않는다(오르기만 한다)
create or replace function economy.crew_blend_avg(p_party bigint, p_user uuid, p_self_m numeric, p_self_w numeric)
returns numeric language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_from timestamptz;
  v_until timestamptz;
  v_w numeric := greatest(coalesce(p_self_w, 0), 0);
  v_sum numeric;
  v_ws numeric;
  m numeric;
begin
  if v_w <= 0 then
    return p_self_m;
  end if;
  v_sum := p_self_m * v_w;
  v_ws := v_w;
  select min(c.started_at) into v_from from economy.crew_runs c where c.party_id = p_party;
  if v_from is null then
    return p_self_m;
  end if;
  v_until := v_from + make_interval(hours => greatest(economy.fx('together_topup_hours', 24), 0)::int);
  select v_sum + coalesce(sum((1 + c.shoe_eff_bps / 10000.0) * least(greatest(coalesce(x.together_unit, 0), 0), v_w)), 0),
         v_ws + coalesce(sum(least(greatest(coalesce(x.together_unit, 0), 0), v_w)), 0)
    into v_sum, v_ws
    from public.walk_sessions x join economy.crew_runs c on c.session_id = x.id and c.user_id = x.user_id
   where x.party_id = p_party and x.user_id <> p_user and x.together_base is not null and x.created_at <= v_until
     and economy.together_qualifies(x);
  if now() <= v_until then
    for m in select 1 + c.shoe_eff_bps / 10000.0 as m from economy.crew_runs c
              where c.party_id = p_party and c.user_id <> p_user and c.session_id is null
              order by 1 loop
      exit when m >= v_sum / v_ws;
      v_sum := v_sum + m * v_w;
      v_ws := v_ws + v_w;
    end loop;
  end if;
  return round(v_sum / v_ws, 6);
end $$;
create or replace function economy.crew_blend_mult(p_party bigint, p_user uuid, p_self_m numeric, p_self_w numeric)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select case when economy.crew_blend_share() <= 0 then p_self_m
              else round((1 - economy.crew_blend_share()) * p_self_m
                         + economy.crew_blend_share() * economy.crew_blend_avg(p_party, p_user, p_self_m, p_self_w), 6) end
$$;
-- 사람 수로 세던 예전 것(0120)은 지운다 — 0120 을 다시 올려도 이 줄이 다시 지운다
drop function if exists economy.crew_blend_mult(bigint, uuid, numeric);
drop function if exists economy.crew_blend_avg(bigint, uuid, numeric);

-- 그날(한국 날짜) 받은 SUP — 0120 + 코스 완주 · 도전 보상(0122, 하루 최대 SUP 밖이던 것)
create or replace function economy.day_earned(p_user uuid, p_day date)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select coalesce((select sum(s.points_awarded) from public.walk_sessions s
                    where s.user_id = p_user and economy.game_day(s.started_at) = p_day), 0)
       + coalesce((select sum(l.amount) from public.sup_ledger l
                    where l.user_id = p_user and l.ref in ('goal:' || p_day, 'course:' || p_day)), 0)
       + coalesce((select sum(l.amount) from public.sup_ledger l
                    where l.user_id = p_user and l.kind = 'EARN_EVENT' and l.amount > 0
                      and l.occurred_at >= economy.game_day_start(p_day) and l.occurred_at < economy.game_day_start(p_day + 1)), 0)
$$;

-- 함께 달린 보너스 정산 — 0120 본문 + 남의 원장 자물쇠는 잡아 보기만(바쁘면 미룸) · 8 : 2 무게(7)
create or replace function economy.together_settle(p_party bigint)
returns int language plpgsql security definer set search_path = public, economy as $$
declare
  v_crew boolean;
  v_from timestamptz;
  v_until timestamptz;
  v_n int;
  v_nn int;
  s public.walk_sessions;
  v_frac numeric;
  v_target numeric;
  v_diff numeric;
  v_room numeric;
  v_pay numeric;
  v_count int := 0;
  v_q boolean;
  v_c economy.crew_runs;
  v_lvl numeric;
  v_tbase numeric;
  v_blend_pay numeric;
  v_blend_base numeric;
  v_ref text;
begin
  select p.crew_id is not null into v_crew from public.parties p where p.id = p_party;
  if not found or not economy.together_on(case when v_crew then 'CREW' else 'FLASH' end) then
    return 0;
  end if;
  perform pg_advisory_xact_lock(hashtext('together:' || p_party));
  v_from := coalesce((select min(c.started_at) from economy.crew_runs c where c.party_id = p_party),
                     (select min(r.starts_at) from public.party_runs r where r.party_id = p_party),
                     (select min(x.started_at) from public.walk_sessions x where x.party_id = p_party));
  if v_from is null then
    return 0;
  end if;
  v_until := v_from + make_interval(hours => greatest(economy.fx('together_topup_hours', 24), 0)::int);
  select count(*)::int into v_n from public.walk_sessions x
   where x.party_id = p_party and x.together_base is not null and x.created_at <= v_until and economy.together_qualifies(x);
  -- 크루는 올린 러닝 모두(함께 달리지 못한 러닝도 — 효율 8 : 2 의 아래 끝으로 받았으면 내 신발 값으로 돌려준다), 번개는 함께 달린 사람만
  for s in select * from public.walk_sessions x
            where x.party_id = p_party and x.together_base is not null and (v_crew or economy.together_qualifies(x))
            order by x.user_id, x.id for update loop
    v_q := economy.together_qualifies(s);
    v_nn := case when s.created_at <= v_until then v_n else v_n + 1 end;
    v_blend_pay := 0;
    if v_crew then
      select * into v_c from economy.crew_runs r where r.user_id = s.user_id and r.party_id = p_party;
      v_frac := case when v_q then economy.crew_headcount_bonus(v_nn) else 0 end;
      v_lvl := coalesce(v_c.level_pct, 0);
      -- 기본 몫: 함께 달린 사람은 내 신발 80% + 함께 달린 크루원 평균 20%(아래 끝), 함께 달리지 못한 사람은 내 신발 그대로
      -- (경로가 받쳐 준 러닝만 — 경로 없는 러닝은 올릴 때 받은 값 그대로, 경로 없는 러닝 상한을 넘지 않게). 올릴 때 받은 값 아래로는 가지 않는다
      v_tbase := s.together_base;
      if s.together_unit is not null and economy.crew_blend_share() > 0 then
        if v_q then
          v_tbase := round(s.together_unit * economy.crew_blend_mult(p_party, s.user_id, 1 + coalesce(v_c.shoe_eff_bps, 0) / 10000.0,
                                                                   coalesce(s.together_unit, 0)), 4);
        elsif s.gps_backed then
          v_tbase := round(s.together_unit * (1 + coalesce(v_c.shoe_eff_bps, 0) / 10000.0), 4);
        end if;
      end if;
      v_tbase := greatest(v_tbase, s.together_base);
      -- 받을 몫 전체 = 기본 × (1 + 레벨 + 인원) — 올릴 때 이미 받은 기본 × (1 + 레벨)을 뺀 것이 더해 줄 몫
      v_target := round(v_tbase * (1 + v_lvl + v_frac) - s.together_base * (1 + v_lvl), 4);
      v_blend_base := greatest(round((v_tbase - s.together_base) * (1 + v_lvl), 4) - coalesce(v_c.blend_sup, 0), 0);
    else
      v_frac := economy.party_multiplier(v_nn) - 1;
      v_target := round(s.together_base * v_frac, 4);
      v_blend_base := 0;
    end if;
    v_diff := v_target - s.together_paid;
    -- 0122: 그 사람의 원장 자물쇠를 기다리지 않고 잡아 본다. 그 사람이 지금 다른 것을 기록하는 중이면(하루 최대를 둘이 함께 읽어 넘던 것)
    -- 미뤄 두고, 그 사람이 다음에 러닝을 올릴 때 이 방을 다시 정산한다(together_pending_run). 내 자물쇠는 이미 잡고 있어 늘 잡힌다
    if v_diff >= 0.0001 and not pg_try_advisory_xact_lock(hashtext('ledger:' || s.user_id::text)) then
      insert into economy.together_pending (party_id, user_id) values (p_party, s.user_id) on conflict do nothing;
      v_diff := 0;
    end if;
    if v_diff >= 0.0001 then
      v_room := case when economy.daily_max_on() then economy.day_room(s.user_id, economy.game_day(s.started_at), s.day_max) else v_diff end;
      v_pay := round(least(v_diff, greatest(v_room, 0)), 4);
      if v_pay > 0 then
        v_blend_pay := least(v_pay, v_blend_base);
        -- 같은 인원에서도 크루 평균이 오르면 다시 더한다 — 같은 원장 이름이면 차례 번호를 붙인다(정산은 방마다 하나씩이라 겹치지 않는다)
        v_ref := 'together:' || s.id || ':' || v_nn;
        if exists (select 1 from public.sup_ledger l where l.user_id = s.user_id and l.ref = v_ref) then
          v_ref := v_ref || ':' || (select count(*) + 1 from public.sup_ledger l where l.user_id = s.user_id and l.ref like 'together:' || s.id || ':%');
        end if;
        insert into public.sup_ledger (user_id, kind, amount, description, ref, occurred_at)
        values (s.user_id, 'EARN_PARTY', v_pay,
                case when v_crew and v_pay - v_blend_pay < 0.0001 then format('크루 평균 효율 (함께 달린 %s명)', v_nn)
                     else format('함께 달린 보너스 (%s명)', v_nn) end,
                v_ref, s.ended_at)
        on conflict do nothing;
        if found then
          update public.walk_sessions x set points_awarded = x.points_awarded + v_pay, together_paid = x.together_paid + v_pay where x.id = s.id;
          if v_crew then
            -- 크루 평균 효율 몫은 기본 · 레벨 줄에(1 : 레벨), 나머지는 인원 줄에
            update economy.crew_runs r
               set base_sup = coalesce(r.base_sup, 0) + round(v_blend_pay / (1 + v_lvl), 4),
                   level_sup = coalesce(r.level_sup, 0) + (v_blend_pay - round(v_blend_pay / (1 + v_lvl), 4)),
                   blend_sup = r.blend_sup + v_blend_pay,
                   headcount_sup = coalesce(r.headcount_sup, 0) + (v_pay - v_blend_pay),
                   total_sup = coalesce(r.total_sup, 0) + v_pay
             where r.user_id = s.user_id and r.party_id = p_party;
          end if;
          v_count := v_count + 1;
        end if;
      end if;
    end if;
    if v_q then
      update public.walk_sessions x set together_n = v_nn, party_size = greatest(v_nn, 1) where x.id = s.id;
      if v_crew then
        update economy.crew_runs r
           set bonus_status = 'CONFIRMED', bonus_count = v_nn, bonus_pct = v_frac, bonus_reason = case when v_nn < 2 then 'TOO_FEW' end
         where r.user_id = s.user_id and r.party_id = p_party;
      end if;
    end if;
  end loop;
  -- 함께 달리지 못한 사람(문턱 아래)은 인원 보너스 없음으로 적는다(결과 화면이 "확인 중"에 머물지 않게)
  if v_crew then
    update economy.crew_runs r
       set bonus_status = 'CONFIRMED', bonus_count = 1, bonus_pct = 0, bonus_reason = 'NOT_TOGETHER'
     where r.party_id = p_party and r.session_id is not null and r.bonus_status = 'PENDING'
       and not exists (select 1 from public.walk_sessions x where x.id = r.session_id and economy.together_qualifies(x));
  end if;
  return v_count;
end $$;

-- 미뤄 둔 정산을 한다 — record_session 이 끝에서(그 사람의 원장 자물쇠를 잡은 채) 부른다
create or replace function economy.together_pending_run(p_user uuid)
returns int language plpgsql security definer set search_path = public, economy as $$
declare v_party bigint; v_n int := 0;
begin
  for v_party in select q.party_id from economy.together_pending q where q.user_id = p_user order by q.party_id loop
    -- 방 자물쇠는 잡아 보기만 — 바쁘면 다음에(두 사람이 서로 다른 차례로 두 방을 기다려 멈추지 않게)
    continue when not pg_try_advisory_xact_lock(hashtext('together:' || v_party));
    delete from economy.together_pending q where q.user_id = p_user and q.party_id = v_party;
    v_n := v_n + coalesce(economy.together_settle(v_party), 0);
  end loop;
  return v_n;
end $$;


-- ════════════════════════════════════════════════════════════════════
--  4. 목표 — 하루 최대에 막혀도 채운 날은 남긴다
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.goal_streak_before(p_user uuid, p_day date, p_max int default 60)
returns int language plpgsql stable security definer set search_path = public, economy as $$
declare v int := 0; d date := p_day - 1;
begin
  while v < p_max loop
    exit when not (
      exists (select 1 from public.sup_ledger l where l.user_id = p_user and l.ref = 'goal:' || d)
      or exists (select 1 from economy.goal_days g where g.user_id = p_user and g.day = d and g.met_at is not null)
      or exists (select 1 from public.boosts b
                  where b.user_id = p_user and b.kind = 'STREAK_SHIELD'
                    and b.starts_at < economy.game_day_start(d + 1) and b.ends_at > economy.game_day_start(d)));
    v := v + 1; d := d - 1;
  end loop;
  return v;
end $$;

-- 0120 본문 + 채운 날을 먼저 남긴다(goal_days.met_at). 하루 최대 SUP 에 막혀 보너스가 0 이어도 연속 · 7일 상급 상자는 그대로 —
-- 예전엔 0 이면 아무것도 남기지 않아 다음 날 연속이 1 로 돌아갔고, 크루에서는 올린 차례로 갈렸다
create or replace function economy.goal_settle(p_user uuid, p_day date)
returns numeric language plpgsql security definer set search_path = public, economy as $$
declare
  v_goal int;
  v_streak int;
  v_bonus numeric;
  v_need int := greatest(coalesce(economy.setting_num('streak_box_days'), 0)::int, 0);
  v_room numeric;
begin
  if p_user is null or p_day is null or p_day > economy.game_day(now()) or p_day < economy.game_day(now()) - 7 then
    return null;
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || p_user::text));
  -- 받은 날이면 끝. 채운 날로만 남아 있으면(하루 최대에 막혔다) 남은 몫이 생겼을 때 다시 준다(같은 날 이벤트로 하루 최대가 커질 때)
  if exists (select 1 from public.sup_ledger l where l.user_id = p_user and l.ref = 'goal:' || p_day) then
    return null;
  end if;
  v_goal := economy.goal_of(p_user, p_day);
  if economy.verified_steps_on(p_user, p_day) < v_goal then
    return null;
  end if;
  v_streak := 1 + economy.goal_streak_before(p_user, p_day, greatest(8, v_need));
  if not exists (select 1 from economy.goal_days g where g.user_id = p_user and g.day = p_day and g.met_at is not null) then
    insert into economy.goal_days (user_id, day, goal, met_at) values (p_user, p_day, v_goal, now())
    on conflict (user_id, day) do update set met_at = coalesce(economy.goal_days.met_at, now());
  end if;
  v_bonus := round((v_goal / 1000.0) * 2.5 * (1 + 0.1 * least(v_streak - 1, 7)), 2);
  -- 하루 최대 SUP 에 목표 보너스도 든다(0120) — 남은 만큼만
  if economy.daily_max_on() then
    v_room := economy.day_room(p_user, p_day, null);
    v_bonus := round(least(v_bonus, greatest(v_room, 0)), 2);
  end if;
  if v_bonus > 0 then
    perform economy.ledger_apply(p_user, 'BONUS_GOAL', v_bonus,
                                 format('목표 달성 보너스 (%s일 연속)', least(v_streak, 8)), 'goal:' || p_day);
    if p_day < economy.game_day(now()) then
      update public.sup_ledger l set occurred_at = economy.game_day_start(p_day + 1) - interval '1 second'
       where l.user_id = p_user and l.ref = 'goal:' || p_day;
    end if;
  end if;
  if v_need > 0 and v_streak >= v_need and not exists (
       select 1 from economy.box_awards a
        where a.user_id = p_user and a.source like 'STREAK:%' and a.day > p_day - v_need and a.day <= p_day) then
    perform economy.box_grant(p_user, 'STREAK:' || p_day, 'GIFT', p_day);
  end if;
  return greatest(v_bonus, 0);
end $$;

-- ════════════════════════════════════════════════════════════════════
--  5. 러너 걸음 — 상한에 걸린 러닝도 센다
-- ════════════════════════════════════════════════════════════════════
-- 0121 은 CLEAN 만 셌다. FLAGGED 는 과속 말고도 하루 걸음 상한 · 하루 최대 SUP · 걸음이 GPS 보다 많아 자름에도 붙어
-- 그 러닝의 걸음이 통째로 빠졌다(오늘의 보상은 같은 러닝을 다 센다). 보상 걸음은 무효 · 보류 · 경로 없음이면 이미 0 이고,
-- 과속이 이어져 멈춘 러닝(run_penalties)만 따로 뺀다
create or replace function economy.runner_steps_of(p_user uuid)
returns bigint language sql stable security definer set search_path = public, economy as $$
  select coalesce(sum(d.s), 0)::bigint from (
    select least(sum(greatest(coalesce(s.reward_steps, s.backed_steps, 0), 0)), economy.max_daily_steps()) as s
      from public.walk_sessions s
     where s.user_id = p_user and s.verdict in ('CLEAN', 'FLAGGED')
       and not exists (select 1 from public.run_penalties x where x.user_id = s.user_id and x.run_started_at = s.started_at)
     group by economy.game_day(s.started_at)) d
$$;

-- ════════════════════════════════════════════════════════════════════
--  6. 강화 화면의 하루 에너지 — 러너 레벨이 켜져 있으면 신발 레벨과 상관없다
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.forge_account_after(p_user uuid, p_target bigint, p_level int, p_burn bigint[],
    out energy numeric, out cap numeric)
language sql stable security definer set search_path = public, economy as $$
  select case when economy.runner_on() then economy.energy_max(p_user)
              else economy.energy_max_for_level(coalesce(max(case when s.id = p_target then p_level
                                                                  when s.origin in ('IMPORT', 'MINT') then 1 else s.level end), 1)) end,
         case when not coalesce((economy.setting('earn_cap_by_shoe') #>> '{}')::boolean, true) then 1
              else greatest(1, coalesce(max(10000 + e.efficiency_bps) / 14000.0, 1)) end
    from public.market_sneakers s
    left join lateral economy.shoe_effective(s) e on true
   where s.owner_id = p_user and s.chain_state = 'APP' and s.status = 'OWNED'
     and not (s.id = any(coalesce(p_burn, '{}'::bigint[])))
$$;

-- ════════════════════════════════════════════════════════════════════
--  7. 코스 완주 보상 — 0024 본문 + 하루 최대 SUP 안에서(10)
-- ════════════════════════════════════════════════════════════════════
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
    -- 코스 거리도 만든 사람이 적은 값 대신 경로로 잰 길이까지만 — 점 하나짜리 코스에 42km 를
    -- 적어 두고 아무 러닝에나 보상을 받지 못하게.
    v_reward := least(floor(least(coalesce(v_course_km, 0), economy.course_length_m(v_course_track) / 1000.0,
                                  v_session.gps_credit_m / 1000.0)), 42);
    -- 0122: 코스 보상도 하루 최대 SUP 에 든다(0120) — 남은 만큼만, 1 SUP 아래면 주지 않는다
    if economy.daily_max_on() then
      perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
      v_reward := least(v_reward, floor(greatest(economy.day_room(v_user, economy.game_day(v_session.started_at), null), 0)));
    end if;
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

-- ════════════════════════════════════════════════════════════════════
--  8. 가입 선물 — 0116 본문 + 상자 원장으로 한 번(11)
-- ════════════════════════════════════════════════════════════════════
create or replace function public.economy_bootstrap() returns void
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
      origin, efficiency_bps, comfort_bps, durability_pts, withdrawable, dur_base
    ) values (
      v_user, 'NONE', 'COMMON', 0, 1, 1, 1 + economy.stat_comfort('COMMON', 0.5) / 10000.0, 100,
      'STARTER', economy.stat_efficiency('COMMON', 0.5), economy.stat_comfort('COMMON', 0.5), 100, false,
      economy.stat_durability('COMMON', 0.5)
    ) returning id into v_id;
    if not exists (select 1 from public.market_sneakers where owner_id = v_user and equipped) then
      update public.market_sneakers set equipped = true where id = v_id;
    end if;
  end if;

  if (select p.created_at from public.profiles p where p.id = v_user) >= v_since then
    -- 0122: 한 번은 상자 원장(SIGNUP)으로 센다 — 예전엔 무료 뽑기 줄이 없을 때만 넣어서, 첫 러닝 상자가 먼저 오면 10개가 사라졌다
    insert into economy.box_awards (user_id, source, kind, day) values (v_user, 'SIGNUP', 'FREE', economy.game_day(now()))
    on conflict (user_id, source) do nothing;
    if found then
      insert into public.draw_grants as d (user_id, kind, granted) values (v_user, 'FREE', 10)
      on conflict (user_id, kind) do update set granted = d.granted + 10;
    end if;
  end if;
end $$;

-- ════════════════════════════════════════════════════════════════════
--  9. 운영자 손잡이 — 0121 목록 + runner_level_steps 는 10,000 부터(9) · 에너지 상한 글
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
    ('runner_level_steps', 'num', 10000, 100000, '러너 레벨: 한 레벨에 필요한 러닝 걸음(낮추면 지난 걸음으로 상자가 나가 되돌릴 수 없어 10,000 아래로는 못 내린다)'),
    ('runner_energy_base', 'num', 1, 30, '러너 레벨: Lv1 하루 에너지(1 = 600보 · 5.5 = 약 20분)'),
    ('runner_energy_per_level', 'num', 0, 3, '러너 레벨: 레벨마다 늘어나는 하루 에너지(0.55 = 약 2분)'),
    ('runner_energy_cap', 'num', 1, 60, '러너 레벨: 하루 에너지 가장 많이(10 = 약 36분 · 0121 전과 같다 — 대격변과 함께 16.5 = 약 60분)')
  ) r(key, kind, lo, hi, label) where r.key = p_key
$$;

-- ════════════════════════════════════════════════════════════════════
--  10. 상자 현황 — 0118 본문 + 러너 레벨 상자는 '러닝 상자'가 아니다(12)
-- ════════════════════════════════════════════════════════════════════
create or replace function public.box_status()
returns jsonb language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_day date := economy.game_day(now());
  v_week date := economy.game_week(economy.game_day(now()));
  v_steps_day bigint;
  v_steps_week bigint;
  v_goal int;
  v_goal_now int;
  v_paid numeric;
  v_before int;
  v_streak int;
  v_best int;
  v_need int := greatest(coalesce(economy.setting_num('streak_box_days'), 0)::int, 0);
  v_last date;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  v_steps_day := economy.box_steps(v_user, v_day, v_day);
  v_steps_week := economy.box_steps(v_user, v_week, v_day);
  v_goal := economy.goal_peek(v_user, v_day);
  select economy.goal_clamp(p.daily_goal) into v_goal_now from public.profiles p where p.id = v_user;
  select l.amount into v_paid from public.sup_ledger l where l.user_id = v_user and l.ref = 'goal:' || v_day;
  v_before := economy.goal_streak_before(v_user, v_day, 400);
  v_streak := v_before + 1;
  select coalesce(max(n), 0) into v_best from (
    select count(*) as n from (
      select d, d - (row_number() over (order by d))::int as grp
        from (select distinct substr(l.ref, 6)::date as d from public.sup_ledger l
               where l.user_id = v_user and l.ref like 'goal:%') x) y
     group by grp) z;
  v_best := greatest(v_best, case when v_paid is not null then v_streak else v_before end);
  select max(a.day) into v_last from economy.box_awards a where a.user_id = v_user and a.source like 'STREAK:%';
  return jsonb_build_object(
    'day', v_day,
    'daily_free', coalesce(economy.setting_num('daily_free_draws'), 0),
    'steps_today', v_steps_day,
    'run_boxes', (select coalesce(jsonb_agg(jsonb_build_object('steps', (t #>> '{}')::int,
                     'got', exists (select 1 from economy.box_awards a where a.user_id = v_user
                                     and a.source = 'RUN' || (t #>> '{}') || ':' || v_day))), '[]'::jsonb)
                    from jsonb_array_elements(coalesce(economy.setting('run_box_steps'), '[]'::jsonb)) t),
    'week_start', v_week,
    'steps_week', v_steps_week,
    'week_target', economy.setting_num('weekly_box_steps'),
    'week_got', exists (select 1 from economy.box_awards a where a.user_id = v_user and a.source = 'WEEK:' || v_week),
    'streak_days', economy.setting_num('streak_box_days'),
    'levels_got', (select coalesce(jsonb_agg(substr(a.source, 7)::int order by substr(a.source, 7)::int), '[]'::jsonb)
                     from economy.box_awards a where a.user_id = v_user and a.source like 'LEVEL:%'),
    'normal_price', economy.setting_num('box_price'),
    'normal_limit', economy.setting_num('box_daily_buy'),
    'normal_bought', (select count(*) from economy.box_buys b where b.user_id = v_user and b.kind = 'NORMAL' and b.day = v_day),
    'premium_price', economy.setting_num('premium_box_price'),
    'premium_limit', economy.setting_num('premium_box_weekly_buy'),
    'premium_bought', (select count(*) from economy.box_buys b where b.user_id = v_user and b.kind = 'PREMIUM'
                         and b.day between v_week and v_week + 6),
    'free_left', coalesce((select g.granted - g.used from public.draw_grants g where g.user_id = v_user and g.kind = 'FREE'), 0),
    'gift_left', coalesce((select g.granted - g.used from public.draw_grants g where g.user_id = v_user and g.kind = 'GIFT'), 0),
    'balance', economy.balance_of(v_user),
    -- 0118 — 오늘의 보상(홈 막대)
    'rule', case when economy.reward_rule_old() then 'OLD' else 'REWARD_STEPS' end,
    'reward_steps_today', economy.reward_steps_on(v_user, v_day),
    'goal', v_goal,
    'goal_next', case when v_goal_now is distinct from v_goal then v_goal_now end,
    'goal_paid', v_paid,
    'goal_bonus', round((v_goal / 1000.0) * 2.5 * (1 + 0.1 * least(v_streak - 1, 7)), 2),
    'streak', v_streak,
    'streak_best', v_best,
    'streak_box_in', case when v_need <= 0 then null
                          else greatest(v_need - v_streak, coalesce(v_last + v_need - v_day, 0),
                                        case when v_paid is not null then 1 else 0 end) end,
    'ticks', (select coalesce(jsonb_agg(x.j order by x.s, x.o), '[]'::jsonb) from (
        select (t #>> '{}')::int as s, 0 as o,
               jsonb_build_object('steps', (t #>> '{}')::int, 'kind', 'BOX',
                 'got', exists (select 1 from economy.box_awards a where a.user_id = v_user
                                 and a.source = 'RUN' || (t #>> '{}') || ':' || v_day)) as j
          from jsonb_array_elements(coalesce(economy.setting('run_box_steps'), '[]'::jsonb)) t
        union all
        select v_goal, 1, jsonb_build_object('steps', v_goal, 'kind', 'GOAL', 'got', v_paid is not null)) x),
    'surge', jsonb_build_object(
        'steps', economy.reward_steps_week(v_user, v_day), 'target', economy.event_target('step_surge'), 'reward', economy.event_reward('step_surge'),
        'claimed', exists (select 1 from public.event_claims c where c.user_id = v_user and c.event_id = 'step_surge'
                             and c.period = economy.event_period('step_surge', 'Asia/Seoul'))),
    'recent', (select coalesce(jsonb_agg(jsonb_build_object('day', r.day, 'kind', r.kind, 'amount', r.amount, 'at', r.at)
                                         order by r.at desc), '[]'::jsonb) from (
        select a.day, case when (a.source like 'RUN%' and a.source not like 'RUNNER%') then 'RUN_BOX' when a.source like 'WEEK:%' then 'WEEK_BOX'
                           when a.source like 'STREAK:%' then 'STREAK_BOX' else 'LEVEL_BOX' end as kind,
               null::numeric as amount, a.granted_at as at
          from economy.box_awards a
         where a.user_id = v_user and a.granted_at > now() - interval '3 days'
           and ((a.source like 'RUN%' and a.source not like 'RUNNER%') or a.source like 'WEEK:%' or a.source like 'STREAK:%' or a.source like 'LEVEL:%')
        union all
        select substr(l.ref, 6)::date, 'GOAL', l.amount, l.created_at
          from public.sup_ledger l
         where l.user_id = v_user and l.ref like 'goal:%' and l.created_at > now() - interval '3 days') r));
end $$;

-- ════════════════════════════════════════════════════════════════════
--  11. record_session — 0120 본문 + 묶인 체력(2) · 8 : 2 무게(5) · 번개 한 번(8) · ALL 의 인원 보너스(9) · 미룬 정산(7)
--      다시 고칠 때는 이것(0122)을 복사한다
-- ════════════════════════════════════════════════════════════════════
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
  p_mock_location boolean default false,
  -- 출발 때 신은 신발(market_sneakers.id, 0089) — 앱이 사실로만 알린다. 내 신발이 아니거나 신을 수 없는 신발이면 예전처럼 지금 신은 신발
  p_sneaker bigint default null
)
returns table (
  session_id bigint,
  verdict text,
  points_awarded numeric,
  balance numeric,
  -- 판정 까닭(0089) — 적립이 0 이거나 줄어든 까닭. 예전 앱은 모르는 칸이라 읽지 않는다
  verdict_reason text,
  -- 이 러닝 보상의 운(0116) — 무효면 null
  reward_luck numeric
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
  -- 이 러닝 · 이 신발의 과속 패널티 — 신고가 먼저 와 신발을 이미 절반으로 깎았다 (0110)
  v_pen public.run_penalties;
  -- 이 러닝이 신발을 닳게 하는 양(크루 러닝은 0, 0110)
  v_wear int := 0;
  v_eff int := 0;
  -- 크루 효율 8 : 2 (0120) — 효율 뺀 바탕 · 이번에 쓴 효율 배수
  v_unit numeric;
  v_cmult numeric;
  v_comfort int := 0;
  v_dur numeric := 100;
  v_energy_left numeric;
  v_energy_per_step numeric;
  v_energy_used numeric := 0;
  v_xp numeric := 1;
  v_recent_void int;
  -- 무효가 잦아 보류 중 — 목표 보너스 · 주간 도전 · 코스 보상에도 이 러닝을 세지 않는다
  v_held boolean := false;
  v_new_account boolean;
  -- 상한(하루 금액 · 경로 없는 러닝 · 올린 날)을 걸기 전 금액 — 상한에 걸려 덜 받은 만큼 에너지도 덜 쓴다 (0068)
  v_points_full numeric := 0;
  -- 크루 러닝(그 크루의 방 · 함께 달린 2명 이상 · 크루원)이면 그 크루 — 크루 체력 · 크루 레벨 거리 (0069)
  v_crew uuid;
  -- 이 러닝의 적립을 받쳐 준 체력 — PERSONAL 개인 에너지 · CREW 크루 체력(그날 나눠 받는 크루원만) (0069)
  v_pool text := 'PERSONAL';
  -- 크루 러닝 3판 (0070)
  v_party_starts timestamptz;
  v_crew_room boolean := false;
  v_crew_level int := 1;
  v_runners int := 0;
  v_pool_pts numeric := 0;
  v_pool_max numeric := 0;
  v_share numeric := 0;
  v_limit_sec int := 0;
  v_wall numeric;
  v_together_sec int := 0;
  v_crew_sec numeric := 0;
  v_crew_used numeric := 0;
  v_headcount numeric := 0;
  v_level_bonus numeric := 0;
  -- 크루 러닝 — 공용 체력 (0071)
  v_cr economy.crew_runs;
  v_is_cr boolean := false;
  v_base numeric := 0;
  v_head_sup numeric := 0;
  v_lvl_sup numeric := 0;
  v_tm double precision;
  v_em double precision;
  v_lm double precision;
  v_frac numeric := 1;
  v_reward_steps int := 0;
  -- 0119 — 닳은 뒤 적립 비율 · 효율 포인트가 닳음 / 에너지를 올리는 배수 · 가득 구간 / 닳은 구간 걸음
  v_worn_rate numeric := 0;
  v_wear_mult numeric := 1;
  v_enr_mult numeric := 1;
  v_life_steps int := 0;
  v_full_steps int := 0;
  v_worn_steps int := 0;
  -- 0120 — 함께 달린 보너스의 바탕(크루 B · 번개는 인원 배수 전 적립) · 이 러닝의 하루 최대
  v_tb numeric;
  v_day_max numeric;
  -- 서버가 이미 끝낸 참여(마지막 한 사람 자동 종료 · 끝냄)면 그때 + 60초(폰 시계 차이) 뒤의 기록은 크루 보상 · 경험치에서 뺀다
  v_cut timestamptz;
  v_xp_frac numeric := 1;
  v_level_now int;
  v_level_was int;
  v_starters int := 1;
  v_xp_m numeric := 0;
  -- 운 (0116) — 보상 × 0.7 ~ 1.3 · 닳는 양 × 0.667 ~ 1.444(평균 1)
  v_luck numeric := 1;
  v_wear_luck numeric := 1;
  -- 하루 걸음 상한 · 에너지로 적립에서 빠진 걸음 — 그 몫만큼은 신발이 닳지 않는다 (0116)
  v_lost_cap int := 0;
  -- 0117 — 서버가 세지 못한 크루 러닝 시간을 정산 때 공용 체력에서
  v_run_sec numeric := 0;
  v_unseen numeric := 0;
  v_need numeric := 0;
  v_take numeric := 0;
  v_uncovered numeric := 0;
  v_pool_row economy.crew_day_pools;
  v_flash bigint;
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
  select s.id, s.verdict, s.points_awarded, coalesce(s.verdict_reason, ''), s.reward_luck
    into v_session_id, v_verdict, v_points, v_reason, v_luck
    from public.walk_sessions s
   where s.user_id = v_user and s.started_at = p_started_at;
  if found then
    return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user), v_reason, v_luck;
    return;
  end if;
  v_reason := '';
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
  -- 경로 거리는 러닝 시간 안에 달릴 수 있는 만큼만(시속 25km). 경로 시각은 앞뒤로 여유를
  -- 두므로, 아주 짧은 러닝 여러 개에 같은 몇 분짜리 경로를 붙여 거리를 여러 번 받지 못하게.
  v_gps_m := least(coalesce(v_gps_m, 0), greatest(v_elapsed, 1) * 7.0);

  -- ── 판정 ──
  if coalesce(p_mock_location, false) then
    v_verdict := 'VOID';
    v_reason := '가짜 위치가 감지되었습니다';

  -- 과속으로 멈춘 뒤 10분(러닝 금지 시간) 안에 시작한 러닝은 적립하지 않는다 (0065)
  elsif exists (
        select 1 from public.run_penalties x
         where x.user_id = v_user
           and p_started_at >= x.stopped_at and p_started_at < x.banned_until) then
    v_verdict := 'VOID';
    v_reason := '과속으로 멈춘 뒤 러닝 금지 시간에 시작한 러닝입니다';

  -- 짧은 러닝도 실제 시간으로 잰다(예전엔 60초로 쳐서 1ms 러닝에 240걸음을 줬다)
  elsif p_steps::numeric * 60 / greatest(v_elapsed, 1) > 240 then
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
        -- 모든 계정의 러닝과 맞대어 본다(0118) — 다른 계정의 경로를 그대로 올려 상자 · 목표를 받지 못하게
        select 1 from public.walk_sessions s
         where s.track <> '' and md5(s.track) = md5(v_track)
           and not (s.user_id = v_user and s.started_at = p_started_at)) then
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

  -- 과속이 30초 넘게 이어져 앱이 멈춘 러닝 — 적립 규칙은 그대로, 판정에 까닭을 남긴다 (0065)
  if v_verdict <> 'VOID' and exists (
        select 1 from public.run_penalties x
         where x.user_id = v_user and x.run_started_at = p_started_at) then
    v_verdict := 'FLAGGED';
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '과속이 이어져 러닝이 멈췄습니다');
  end if;

  -- ── 함께 달리기 · 크루 러닝 (0070 — 걸음 상한 · 신발보다 먼저 정한다) ──
  -- 파티 보너스는 한 파티에서 한 러닝만
  select pf.party_id, pf.size into v_party_id, v_party from economy.party_for(v_user, p_started_at, p_ended_at) pf;  -- 0109 #13: 출발 전에 끝난 러닝은 그 방 러닝이 아니다
  -- 번개러닝 보너스는 번개 글 하나에 한 번(0117) — 방을 닫고 같은 글로 다시 열어 보너스를 되풀이하지 않게
  select x.flash_post_id into v_flash from public.parties x where x.id = v_party_id;
  if v_party_id is null or exists (select 1 from public.walk_sessions s
                                    where s.user_id = v_user and s.party_id = v_party_id)
     or (v_flash is not null and exists (
           select 1 from public.walk_sessions s join public.parties x on x.id = s.party_id
            where s.user_id = v_user and x.flash_post_id = v_flash
              and (s.party_size > 1 or (s.together_base is not null and economy.together_qualifies(s)))
              and s.verdict <> 'VOID')) then  -- 0122: 원격 번개는 인원이 나중에 정해져 처음엔 1명으로 적힌다 — 함께 달린 사람으로 올린 러닝이면 받은 것
    v_party_id := null;
    v_party := 1;
  end if;

  -- 크루 러닝(0071) — 이 러닝의 방에서 서버가 출발을 확정한 참여(economy.crew_runs, party_start)가 있어야 한다.
  -- 앱이 크루 러닝이라고 말해서가 아니다. 참여가 이미 정산됐으면(같은 방 · 같은 날 두 번째 러닝) 보통 러닝이다
  if v_party_id is not null then
    select p.crew_id into v_crew from public.parties p where p.id = v_party_id;
    v_crew_room := v_crew is not null;
    v_crew := null;
    if v_crew_room then
      select * into v_cr from economy.crew_runs c where c.user_id = v_user and c.party_id = v_party_id;
      if found and v_cr.session_id is null and v_cr.crew_id is not null then
        v_is_cr := true;
      elsif found then
        v_party_id := null;
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '오늘 크루 러닝을 이미 했습니다');
      end if;
      -- 크루 방의 보너스는 크루 러닝 보너스 하나다 — 크루 러닝이 아니면 예전 함께 달리기 보너스도 없다(겹치지 않게)
      if not v_is_cr then
        v_party := 1;
      end if;
    end if;
  end if;
  if v_is_cr then
    v_pool := 'CREW';
    v_crew := v_cr.crew_id;
    v_crew_level := v_cr.crew_level;
    v_party_starts := v_cr.started_at;
    select count(*)::int into v_starters from economy.crew_runs c where c.party_id = v_cr.party_id;
    v_party := v_starters;
    if v_cr.state = 'ENDED' and v_cr.terminal_reason in ('USER_END', 'LAST_PARTICIPANT_END') and v_cr.ended_at is not null then
      v_cut := v_cr.ended_at + interval '60 seconds';
    end if;
    -- 공용 체력을 지금까지 정산하고(풀 → 사람 차례로 잠근다) 이 러닝을 끝낸다 — 다른 사람이 모두 끝났으면 마지막 사람도
    perform economy.crew_pool_settle(v_cr.crew_id, v_cr.day, now());
    perform economy.crew_run_close(v_user, v_cr.day, 'RECORDED');
    -- 인원 보너스 — 2분이 지났으면 지금 정하고, 아니면 확인할 수 없다(D01)
    -- 0120: 함께 달린 사람만 센다(ACTIVE) — 인원 보너스는 기록한 뒤 together_settle 이 함께 달린 사람이 올릴 때마다 더한다
    if economy.together_on('CREW') then
      v_headcount := 0;
    else
      perform economy.crew_run_bonus_eval(v_user, v_cr.day, true);
      select * into v_cr from economy.crew_runs c where c.user_id = v_user and c.day = v_cr.day;
      v_headcount := v_cr.bonus_pct;
    end if;
    v_level_bonus := v_cr.level_pct;
  end if;

  -- ── 하루 걸음 상한 (한국 하루) — 개인 러닝만, 크루 러닝 걸음은 세지 않는다 (0070) ──
  if v_rewardable > 0 and v_pool = 'PERSONAL' then
    select coalesce(sum(s.rewarded_steps), 0) into v_already_steps
      from public.walk_sessions s
     where s.user_id = v_user
       and s.verdict in ('CLEAN', 'FLAGGED')
       and s.energy_pool <> 'CREW'
       and economy.game_day(s.started_at) = v_gday;
    if v_rewardable > economy.max_daily_steps() - v_already_steps then
      v_lost_cap := v_lost_cap + v_rewardable - greatest(economy.max_daily_steps() - v_already_steps, 0);
      v_rewardable := greatest(economy.max_daily_steps() - v_already_steps, 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루 적립 상한에 걸렸습니다');
    end if;
  end if;

  -- ── 무효가 잦으면 보류 ──
  -- 적립할 걸음이 없는 러닝도 본다 — 걸음 0 인 경로 러닝이 보류 중에도 잠금 거리 · 꺼내기 조건 거리를 받았다
  if v_verdict <> 'VOID' then
    select count(*) into v_recent_void from public.walk_sessions s
     where s.user_id = v_user and s.verdict = 'VOID'
       and s.started_at > now() - interval '7 days';
    if v_recent_void >= 5 then
      v_rewardable := 0;
      v_held := true;
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '최근 무효 러닝이 많아 적립을 보류합니다');
    end if;
  end if;

  -- ── 신발 ──
  -- 크루 러닝은 출발 때 고른 신발(아직 내 것이면) — 효율 · 착화감은 그 신발 것 (0070)
  if v_pool = 'CREW' then
    -- 출발 때 고른 신발 — 기록에 적는 번호만. 효율은 출발 때 스냅샷(v_cr.shoe_eff_bps)으로 센다
    select x.* into v_shoe from public.market_sneakers x where x.id = v_cr.sneaker_id and x.owner_id = v_user;
  end if;
  -- 개인 러닝은 출발 때 신은 신발(앱이 알린 p_sneaker, 0089) — 올릴 때 신은 신발이 아니라 달린 신발로 닳고 효율을 센다.
  -- 내 것이고 신을 수 있는 신발(앱 안 · 갖고 있음 — 판매 중 · 체인 · 태운 신발은 아니다)일 때만. 아니면 예전처럼 지금 신은 신발
  if v_shoe.id is null and p_sneaker is not null then
    select x.* into v_shoe from public.market_sneakers x
     where x.id = p_sneaker and x.owner_id = v_user and x.chain_state = 'APP' and x.status = 'OWNED';
  end if;
  -- 출발 때 신은 신발(크루 러닝은 고른 신발)을 이 계정이 올리기 전에 강화 재료로 태웠다(다른 폰에서 — 0118) — 그 신발의 마지막 값으로 셈한다.
  -- 예전엔 지금 신은 다른 신발로 정산해, 닳은 신발로 달린 러닝이 내구도 몫만큼 깎이지 않았다. 태운 신발은 닳음 · 거리를 남기지 않는다(아래)
  if v_shoe.id is null and coalesce(case when v_pool = 'CREW' then v_cr.sneaker_id end, p_sneaker) is not null then
    select x.* into v_shoe from public.market_sneakers x
     where x.id = coalesce(case when v_pool = 'CREW' then v_cr.sneaker_id end, p_sneaker) and x.status = 'BURNED'
       and exists (select 1 from public.forge_requests r where r.user_id = v_user and x.id = any(r.material_ids));
  end if;
  if v_shoe.id is null then
    select * into v_shoe from economy.equipped_sneaker(v_user);
  end if;
  if v_shoe.id is null then
    -- 신은 신발이 없으면(꺼내기로 벗었다) 첫 신발을 신긴다. 신발 없이 달리면 내구도 100 · 닳지 않음으로
    -- 쳐서, 닳은 신발을 신고 수리비를 내는 것보다 나았다. 첫 신발을 아직 안 받은 계정(앱은 로그인하자마자
    -- 받는다)은 예전처럼 신발 없이 계산한다 — 앱이 받기 전에 러닝이 먼저 올라가도 적립을 잃지 않게.
    update public.market_sneakers set equipped = true, updated_at = now()
     where id = (select m.id from public.market_sneakers m
                  where m.owner_id = v_user and m.origin = 'STARTER'
                    and m.chain_state = 'APP' and m.status = 'OWNED'
                  limit 1)
       and not exists (select 1 from public.market_sneakers e where e.owner_id = v_user and e.equipped);
    select * into v_shoe from economy.equipped_sneaker(v_user);
  end if;
  -- 과속으로 멈춘 러닝 — 신고(report_overspeed_stop)가 먼저 와 이 신발을 이미 절반으로 깎았다 (0110).
  -- 멈춘 러닝 자체는 평소처럼 정산한다(0065) — 수명 자르기 · 적립 배율은 신고 전 내구도로 센다. 여기서는 v_shoe(이 함수의 사본)만
  -- 되돌린다. 표의 신발은 아래 "신발이 닳는다"에서 (신고 전 − 이 러닝의 닳음)의 절반으로 맞춘다.
  -- 패널티가 다른 신발에 갔으면(예전 앱 · 출발 뒤 신발을 바꿈) 이 신발은 그대로 정산하고 그 신발의 패널티도 그대로다
  if v_shoe.id is not null then
    select x.* into v_pen from public.run_penalties x
     where x.user_id = v_user and x.run_started_at = p_started_at and x.sneaker_id = v_shoe.id
       and x.dur_before is not null and x.dur_after is not null;
    if v_pen.user_id is not null then
      v_shoe.dur_now := least(greatest(v_pen.dur_before, 0), v_shoe.dur_max);
      v_shoe.durability_pts := round(100.0 * v_shoe.dur_now / greatest(v_shoe.dur_max, 1), 2);
    end if;
  end if;
  if v_shoe.id is not null then
    -- 효율 · 착화감은 레벨 1 값 + 나눈 포인트, 내구도는 남은 비율(%) — 이 러닝 전의 값으로 배율을 정한다 (0059)
    select e.efficiency_bps, e.comfort_bps, e.durability into v_eff, v_comfort, v_dur
      from economy.shoe_effective(v_shoe) e;
  end if;

  if v_pool = 'CREW' then
    -- 크루 러닝은 개인 신발 내구도 · 개인 에너지를 쓰지 않는다(R02 · R11). 효율은 출발 때 고른 신발의 스냅샷(R04).
    -- 공용 체력이 0 이던 구간에 달린 걸음은 적립하지 않는다(R03) — 경로를 시간으로 갈라 그 구간의 거리만큼, 경로가 없으면 시간만큼
    v_dur := 100;
    v_eff := v_cr.shoe_eff_bps;
    v_comfort := 0;
    select t.total_m, t.empty_m, t.late_m into v_tm, v_em, v_lm
      from economy.track_meters_empty(v_track, v_cr.crew_id, v_cr.day, v_cut) t;
    if v_tm > 0 then
      v_frac := greatest(1 - v_em / v_tm, 0)::numeric;
      v_xp_frac := greatest(1 - v_lm / v_tm, 0)::numeric;
    else
      -- 경로가 없으면 시간으로. 서버가 끝낸 뒤의 시간은 빼고, 0 이던 몫은 서버가 센 달린 초 중 0 이던 초로(일시정지와 상관없다)
      v_xp_frac := case when v_cut is null or v_cut >= p_ended_at then 1
                        else least(greatest(extract(epoch from (v_cut - p_started_at)), 0)
                                   / greatest(extract(epoch from (p_ended_at - p_started_at)), 1), 1)::numeric end;
      if v_cr.active_sec > 0 then
        v_frac := greatest(1 - v_cr.empty_sec / v_cr.active_sec, 0) * v_xp_frac;
      else
        v_frac := greatest(v_xp_frac - economy.crew_empty_sec(v_cr.crew_id, v_cr.day, p_started_at, least(p_ended_at, coalesce(v_cut, p_ended_at)))
                                       / greatest(extract(epoch from (p_ended_at - p_started_at)), 1), 0);
      end if;
    end if;
    -- 서버가 세지 못한 달린 시간(상태 알림이 오지 않은 동안 — 연결이 끊겼다 · 폰이 출발하지 않았다) 만큼 공용 체력을 지금 쓴다 (0117).
    -- 예전엔 알림이 안 오면 체력이 줄지 않아 러닝 전체가 적립됐다(둘이 3시간 오프라인 485 SUP · 걸음만 6시간 1,388 SUP).
    -- 체력이 모자라면 그 시간만큼 적립에서 뺀다. 0 이던 구간(이미 뺐다)과 서버가 끝낸 뒤(v_cut)는 다시 세지 않는다
    if v_rewardable > 0 then
      v_run_sec := v_elapsed;
      if v_cut is not null then
        v_run_sec := least(v_run_sec, greatest(extract(epoch from (v_cut - p_started_at)), 0));
      end if;
      v_unseen := greatest(v_run_sec - coalesce(v_cr.active_sec, 0), 0);
      v_unseen := greatest(v_unseen - greatest(economy.crew_empty_sec(v_cr.crew_id, v_cr.day, p_started_at,
                                least(p_ended_at, coalesce(v_cut, p_ended_at))) - coalesce(v_cr.empty_sec, 0), 0), 0);
      if v_unseen > 0 then
        select * into v_pool_row from economy.crew_day_pools d
         where d.crew_id = v_cr.crew_id and d.day = v_cr.day for update;
        v_need := v_unseen / economy.durability_sec_per_point()::numeric;
        -- 0122: 10분 전에 끝낸 · 소식이 끊긴 사람이 맡긴 몫(묶어 둔 체력, 0120 R3)은 쓰지 않는다 — 내 몫은 쓴다
        v_take := least(v_need, greatest(coalesce(v_pool_row.capacity, 0) - coalesce(v_pool_row.consumed, 0)
                                         - economy.crew_pool_held_except(v_cr.crew_id, v_cr.day, now(), v_user), 0));
        if v_take > 0 then
          update economy.crew_day_pools d
             set consumed = least(d.consumed + v_take, d.capacity), version = d.version + 1,
                 low_at = case when d.low_at is null and d.capacity > 0
                                    and d.capacity - d.consumed - v_take <= d.capacity * 0.2 then now() else d.low_at end,
                 exhausted_at = case when d.capacity > 0 and d.capacity - d.consumed - v_take <= 0.0001
                                     then coalesce(d.exhausted_at, now()) else d.exhausted_at end
           where d.crew_id = v_cr.crew_id and d.day = v_cr.day
          returning * into v_pool_row;
          update economy.crew_runs c set consumed = c.consumed + v_take
           where c.user_id = v_user and c.day = v_cr.day;
          -- 이 정산으로 0 이 됐다 — 지금부터 0 인 구간을 연다(달리는 다른 사람의 그 뒤 걸음은 적립하지 않는다)
          if v_pool_row.capacity > 0 and v_pool_row.capacity - v_pool_row.consumed <= 0.0001
             and not exists (select 1 from economy.crew_pool_empty x
                              where x.crew_id = v_cr.crew_id and x.day = v_cr.day and x.to_at is null) then
            insert into economy.crew_pool_empty (crew_id, day, from_at) values (v_cr.crew_id, v_cr.day, now())
            on conflict do nothing;
          end if;
        end if;
        v_uncovered := (v_need - v_take) * economy.durability_sec_per_point();
        if v_uncovered > 0 and v_run_sec > 0 then
          v_frac := greatest(v_frac - v_uncovered / v_run_sec, 0);
        end if;
      end if;
    end if;
    if v_rewardable > 0 and v_frac < 1 then
      v_rewardable := floor(v_rewardable * v_frac)::int;
      if v_frac < v_xp_frac then
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '크루 체력을 다 썼습니다');
      end if;
    end if;
    if v_xp_frac < 1 then
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '크루 러닝이 끝난 뒤의 기록은 크루 보상에서 뺐습니다');
    end if;
  else
    -- 내구도는 문이 아니라 요율이다 (0119): 남은 내구도로 달린 걸음(가득 구간)은 다 받고, 다 닳은 뒤의 걸음(닳은 구간)은
    -- worn_rate(30%)만 받는다 — 0% 신발로도 달리고, 걸음은 다 센다. worn_rate 가 0 이면 예전처럼 닳은 뒤 걸음은 적립이 없다(0066).
    -- 효율 포인트는 같은 시간에 더 벌게 하고 그만큼 신발과 에너지를 빨리 쓴다(wear_mult · energy_mult) —
    -- 한 켤레로 버는 양은 내구도, 하루에 버는 양은 착화감이 늘린다. 남은 내구도로 달릴 수 있는 시간 = 내구도 × 12초 ÷ wear_mult
    v_worn_rate := economy.worn_rate();
    if v_shoe.id is not null then
      v_wear_mult := economy.wear_mult(v_shoe);
      v_enr_mult := economy.energy_mult(v_shoe);
    end if;
    v_life_steps := v_rewardable;
    if v_rewardable > 0 and v_shoe.id is not null
       and v_shoe.dur_now * economy.durability_sec_per_point() / v_wear_mult < v_elapsed then
      -- 큰 수(걸음 × 내구도 × 12)가 int 를 넘지 않게 numeric 으로 셈한다
      v_life_steps := floor(v_rewardable::numeric * v_shoe.dur_now * economy.durability_sec_per_point() / v_wear_mult
                            / greatest(v_elapsed, 1))::int;
      if v_worn_rate <= 0 then
        v_rewardable := v_life_steps;
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳은 뒤의 걸음은 적립하지 않습니다');
      end if;
    end if;

    -- 개인 에너지 — 적립 걸음 600 에 1, 착화감만큼 덜. 닳은 구간의 걸음도 에너지는 그대로 쓴다 —
    -- 덜 쓰게 하면 수리하지 않고 오래 걷는 쪽이 하루에 같은 돈을 수리비 없이 번다
    if v_rewardable > 0 then
      -- 에너지 드링크를 고른 러닝은 그 비율만큼만 쓴다 (0106)
      v_energy_per_step := (1 - v_comfort / 10000.0) / economy.steps_per_energy()
                           * economy.run_energy_factor(v_user, p_started_at) * v_enr_mult;
      v_energy_left := economy.energy_left(v_user, v_gday);
      if v_rewardable * v_energy_per_step > v_energy_left then
        v_lost_cap := v_lost_cap + v_rewardable - greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
        v_rewardable := greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '에너지를 다 썼습니다');
      end if;
      v_energy_used := round(v_rewardable * v_energy_per_step, 4);
    end if;
    v_full_steps := least(v_rewardable, v_life_steps);
    v_worn_steps := v_rewardable - v_full_steps;
    if v_worn_steps > 0 then
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '신발이 다 닳은 뒤의 걸음은 일부만 적립했습니다');
    end if;
  end if;

  -- 적립 배수 — 24시간 부스터 · 보상 이벤트 · 이 러닝에 고른 아이템 가운데 가장 큰 것 하나(겹치지 않는다, 0106)
  v_xp := economy.run_reward_mult(v_user, p_started_at);
  v_luck := economy.luck_draw('reward_luck');

  if v_pool = 'CREW' then
    -- 기본 B(고른 신발) + B × 인원 보너스(구간 하나) + B × 레벨 보너스 — 더하기, 각 몫을 4자리로 정해 합이 원장과 같게 (R08 · R10)
    -- 0120 크루 효율 8 : 2 — 효율은 내 신발 80% + 함께 달린 크루원 평균 20%. 올릴 때는 둘 가운데 낮은 값(아래 끝)으로 주고,
    -- 함께 달린 사람이 정해질 때마다 together_settle 이 차이만 더한다(함께 달리지 못했으면 내 신발 값으로). 경로 없는 러닝은 내 신발 그대로
    v_unit := v_rewardable * economy.points_per_step() * v_xp * v_luck;
    v_cmult := 1 + v_eff / 10000.0;
    if economy.crew_blend_share() > 0 and v_verdict <> 'VOID' and v_gps_m >= economy.gps_check_min_m() then
      v_cmult := least(v_cmult, economy.crew_blend_mult(v_cr.party_id, v_user, v_cmult, v_unit));  -- 0122: 평균에 달린 만큼 무게
    else
      v_unit := null;
    end if;
    v_base := round(v_rewardable * economy.points_per_step() * v_cmult * v_xp * v_luck, 4);
    v_head_sup := round(v_base * v_headcount, 4);
    v_lvl_sup := round(v_base * v_level_bonus, 4);
    v_points := v_base + v_head_sup + v_lvl_sup;
    v_tb := v_base;
  else
    -- 가득 구간은 다, 닳은 구간은 worn_rate 만큼(0119). worn_rate 가 0 이면 닳은 구간이 없고 예전 배율(내구도 0 이면 0)을 쓴다
    v_points := round(
      (v_full_steps + v_worn_steps * v_worn_rate) * economy.points_per_step()
        * (1 + v_eff / 10000.0)
        * case when v_worn_rate > 0 then 1 else economy.durability_factor(v_dur) end
        * economy.party_multiplier(case when economy.together_on('FLASH') and v_party_id is not null then 1 else v_party end)
        * v_xp * v_luck,
      4
    );
  end if;
  -- 번개 · 옛 방(0120 ACTIVE): 인원 배수는 기록한 뒤 together_settle 이 함께 달린 사람이 올릴 때마다 더한다 — 바탕은 배수 전 적립
  if v_pool = 'PERSONAL' and v_party_id is not null and economy.together_on('FLASH') then
    v_tb := v_points;
  end if;
  v_points_full := v_points;
  if v_rewardable > 0 and v_worn_rate <= 0 and economy.durability_factor(v_dur) < 1 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳아 적립이 없습니다');
  end if;

  -- ── 하루 금액 상한 — 가입 7일 안은 절반 ──
  -- 아래 상한(하루 금액 · 경로 없는 러닝 · 올린 날)은 개인 러닝만 — 크루 러닝 보상은 따로이고 아직 상한이 없다
  -- (2026-10-21 사용자 결정 "일단 무제한"). 개인 상한의 합에서도 크루 러닝은 뺀다 (0070)
  -- 0120 — 하루 최대 SUP(사용자 결정): 혼자 · 번개 · 크루 · 보너스 · 목표까지 그날 받는 SUP 의 합 ≤ 이 러닝 신발로 1시간 달린 값 × 4
  -- (보상 이벤트 · 2배권이면 그 배수만큼 · 가입 7일 안은 절반). 크루 러닝도 든다 — 크루 러닝에 상한이 없던 것(점검 P1)
  v_day_max := economy.daily_max_of(v_user, v_eff, v_xp);
  if v_points > 0 and economy.daily_max_on() then
    v_already_points := economy.day_earned(v_user, v_gday);
    if v_points > v_day_max - v_already_points then
      v_points := greatest(round(v_day_max - v_already_points, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루 최대 SUP 에 걸렸습니다');
    end if;
  elsif v_points > 0 and v_pool = 'PERSONAL' then
    v_new_account := (select p.created_at from public.profiles p where p.id = v_user)
                     > now() - make_interval(days => economy.setting_num('new_account_days')::int);
    -- 상한은 가진 신발 가운데 가장 센 효율을 따라간다 (0116)
    v_cap := economy.setting_num('daily_earn_cap') * economy.earn_cap_factor(v_user) * case when v_new_account then 0.5 else 1 end;
    select coalesce(sum(s.points_awarded), 0) into v_already_points
      from public.walk_sessions s
     where s.user_id = v_user and s.energy_pool <> 'CREW' and economy.game_day(s.started_at) = v_gday;
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
  -- 크루 러닝도 같은 상한을 함께 쓴다(0117) — 크루 러닝은 다른 상한이 없어 걸음만으로 끝없이 받았다
  if v_points > 0 and not v_gps_backed then
    select economy.setting_num('no_gps_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and not s.gps_backed
       and economy.game_day(s.started_at) = v_gday;
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '경로 없는 러닝의 하루 상한에 걸렸습니다');
    end if;
  end if;
  if v_points > 0 and v_pool = 'PERSONAL' then
    select economy.setting_num('upload_daily_cap') * economy.earn_cap_factor(v_user) - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and s.energy_pool <> 'CREW'
       and economy.game_day(s.created_at) = economy.game_day(now());
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루에 올릴 수 있는 적립 상한에 걸렸습니다');
    end if;
  end if;

  -- 크루 러닝이 상한에 걸렸으면 세 몫(기본 · 인원 · 레벨)을 같은 비율로 줄여 합이 원장과 같게 (0117)
  if v_pool = 'CREW' and v_points_full > 0 and v_points < v_points_full then
    v_base := round(v_base * v_points / v_points_full, 4);
    v_head_sup := round(v_head_sup * v_points / v_points_full, 4);
    v_lvl_sup := v_points - v_base - v_head_sup;
  end if;

  -- 상한에 걸려 덜 받았으면 받은 몫만큼만 에너지를 쓴다 — 쓴 에너지 × 받은 SUP ÷ 상한 전 SUP (0068).
  -- 예전엔 하루 금액 상한에 걸려 0 SUP 를 받은 러닝도 에너지를 다 썼다. 적립 걸음(rewarded_steps)은 그대로 둔다
  if v_energy_used > 0 and v_points < v_points_full then
    v_energy_used := round(v_energy_used * v_points / v_points_full, 4);
  end if;

  v_distance_m := case
    -- 걸음이 있어야 100m 여유를 준다 — 걸음 0 인 경로만으로 거리를 받지 않게
    when v_gps_m >= 100 then least(v_gps_m, greatest(v_verified, 0) * 0.762 * 3
                                          + case when v_verified > 0 then 100 else 0 end)
    else v_step_m
  end;
  if v_verdict = 'VOID' then v_distance_m := 0; end if;

  -- 잠금 거리 · 꺼내기 조건에 쳐 주는 거리 — 경로로 잰 것만, 하루 상한 안에서
  if v_gps_backed and not v_held then
    select greatest(economy.setting_num('gps_km_daily_cap') * 1000 - coalesce(sum(s.gps_credit_m), 0), 0)
      into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and economy.game_day(s.started_at) = v_gday;
    v_credit_m := least(v_distance_m, v_cap_left);
  end if;

  -- ── 보상 걸음 (0118) ──
  -- 상자 · 목표 보너스 · 연속 · 스텝 서지가 세는 걸음 하나. 무효 · 보류 · 경로가 받쳐 주지 않은 러닝은 0.
  -- 0119: 신발이 닳았든 크루 체력이 비었든 경로로 확인된 걸음은 다 센다(내구도는 적립 비율만 정한다).
  -- worn_rate 가 0 이면 0118 처럼 — 혼자 러닝은 남은 내구도로 달릴 수 있던 시간만큼, 크루 러닝은 공용 체력이 받쳐 준 몫만큼.
  -- 에너지 · 하루 상한과는 상관없다(에너지를 다 쓴 뒤의 걸음도 센다). 하루 48,000 은 합할 때(economy.reward_steps_on)
  v_reward_steps := case
    when v_verdict = 'VOID' or v_held or not v_gps_backed or coalesce(v_verified, 0) <= 0 then 0
    when economy.worn_rate() > 0 then v_verified
    when v_pool = 'CREW' then floor(v_verified * least(greatest(v_frac, 0), 1))::int
    when v_shoe.id is null then v_verified
    when coalesce(v_shoe.dur_now, 0) * economy.durability_sec_per_point() / v_wear_mult < v_elapsed
      then floor(v_verified::numeric * coalesce(v_shoe.dur_now, 0) * economy.durability_sec_per_point() / v_wear_mult
                 / greatest(v_elapsed, 1))::int
    else v_verified end;
  if v_verdict <> 'VOID' and not v_held and not v_gps_backed and coalesce(v_verified, 0) > 0 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '경로가 확인되지 않아 보상 걸음에 들지 않았습니다');
  end if;

  -- ── 기록 ──
  insert into public.walk_sessions (
    user_id, started_at, ended_at, duration_sec, steps,
    distance_meters, calories, track, boost_bps, party_size,
    faction, top_speed_kmh, gps_distance_m,
    verdict, verdict_reason, points_awarded, rewarded_steps,
    mock_location, verified_steps, energy_used, sneaker_id,
    gps_backed, backed_steps, gps_credit_m, party_id,
    crew_id, energy_pool, reward_steps, together_base, day_max, together_unit, together_paid
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    v_distance_m, p_steps * 0.04, v_track, least(v_eff, 5000), least(greatest(v_party, 1), 20),
    '', case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable,
    coalesce(p_mock_location, false), v_verified, v_energy_used, v_shoe.id,
    v_gps_backed, case when v_gps_backed and not v_held then v_verified else 0 end, v_credit_m, v_party_id,
    v_crew, v_pool, v_reward_steps, v_tb, v_day_max, case when v_pool = 'CREW' then round(v_unit, 8) end,
    -- 0122: 예전 규칙(ALL)이면 인원 보너스를 적립에 이미 넣었다 — 규칙을 다시 켜도 정산이 한 번 더 주지 않게 준 것으로 적는다
    case when v_pool = 'CREW' and not economy.together_on('CREW') then coalesce(v_head_sup, 0) else 0 end
  )
  returning id into v_session_id;

  if v_points > 0 then
    v_kind := case when v_party > 1 then 'EARN_PARTY' else 'EARN_WALK' end;
    insert into public.sup_ledger (user_id, kind, amount, description, session_id, occurred_at)
    values (v_user, v_kind, v_points, format('러닝 세션 적립 (%s보)', v_rewardable),
            v_session_id, p_ended_at);
  end if;

  -- 개인 에너지는 개인 러닝만 쓴다 — 크루 러닝은 공용 체력(crew_runs · crew_day_pools)
  if v_energy_used > 0 and v_pool = 'PERSONAL' then
    insert into public.energy_days (user_id, day, used) values (v_user, v_gday, v_energy_used)
    on conflict (user_id, day) do update set used = public.energy_days.used + excluded.used;
  end if;

  -- 크루 레벨 — 경험치는 유효 크루 러닝의 사람마다 자기 거리(R09). 보류 · 무효 · 크루를 나간 뒤는 쌓지 않는다. 레벨은 내려가지 않고,
  -- 넘은 레벨마다 레벨 업을 한 번 적는다. 정산은 참여 한 줄에 한 번(같은 러닝을 다시 올리면 위에서 원래 결과를 돌려준다)
  if v_pool = 'CREW' then
    select coalesce(c.level, 1) into v_level_was from public.crews c where c.id = v_crew for update;
    v_level_now := v_level_was;
    -- 서버가 끝낸 뒤의 거리는 빼고(v_xp_frac)
    if v_verdict <> 'VOID' and not v_held and v_distance_m * v_xp_frac > 0
       and exists (select 1 from public.crew_members m where m.crew_id = v_crew and m.user_id = v_user) then
      update public.crews c
         set run_km = c.run_km + round((v_distance_m * v_xp_frac / 1000)::numeric, 3),
             level = greatest(coalesce(c.level, 1),
                              economy.crew_level_for_km(c.run_km + round((v_distance_m * v_xp_frac / 1000)::numeric, 3)))
       where c.id = v_crew
      returning c.level into v_level_now;
      v_xp_m := round((v_distance_m * v_xp_frac)::numeric, 1);
      insert into economy.crew_level_events (crew_id, level, session_id, run_km)
      select v_crew, l, v_session_id, (select run_km from public.crews where id = v_crew)
        from generate_series(v_level_was + 1, v_level_now) l
      on conflict (crew_id, level) do nothing;
    end if;
    update economy.crew_runs c
       set session_id = v_session_id, base_sup = case when v_points > 0 then v_base else 0 end,
           headcount_sup = case when v_points > 0 then v_head_sup else 0 end,
           level_sup = case when v_points > 0 then v_lvl_sup else 0 end, total_sup = v_points,
           xp_m = v_xp_m,
           level_before = v_level_was, level_after = v_level_now, settled_at = now()
     where c.user_id = v_user and c.day = v_cr.day;
  end if;

  -- 신발이 닳는다. 예전 신발(IMPORT·MINT)도 똑같이 닳는다 — 안 닳으면 수리비를 안 내는 신발이 된다.
  -- 모든 신발이 같은 빠르기로 — 달린 1분마다 남은 내구도 5 (0064). 달린 시간은 멈춘 시간을 뺀 값(v_elapsed).
  -- 거리 · 걸음과 상관없다 — 켜 두고 움직이지 않아도 닳는다(0068, 2026-10-19 사용자 결정. 예전엔 거리 0 이면 닳지 않았다).
  -- 무효 러닝은 닳지 않는다. 남은 비율(durability_pts)은 트리거가 다시 센다.
  -- 크루 러닝은 크루 체력을 쓰고 신발은 닳지 않는다 — 거리만 쌓는다 (0070, 사용자 결정 "하루에 내 자유러닝 + 크루러닝")
  --
  -- 과속 패널티가 먼저 깎은 러닝(v_pen, 0110): B = 신고 전 내구도(dur_before), A = 신고가 남긴 값(dur_after = B / 2),
  -- w = 이 러닝의 닳음(크루 러닝은 0). 맞는 끝값은 (B − w) / 2(버림, 0 아래로 가지 않음) — 평소처럼 닳은 뒤 남은 것의 절반.
  -- 표에는 이미 A 가 있으니 그 차이((B − w) / 2 − A, 0 또는 음수)만큼 더한다 — 신고와 정산 사이에 바뀐 값(수리 등)은 그대로 둔다.
  -- 무효 러닝은 닳지 않아(w = 0) 신고가 남긴 A 그대로다
  v_wear_luck := economy.luck_draw('wear_luck');
  -- 하루 걸음 상한 · 에너지 · 하루 금액 상한으로 적립에서 빠진 몫만큼은 닳지 않는다 — 적립이 없는 구간에 수리비만 늘지 않게 (0116).
  -- 내구도가 다 닳아 빠진 몫은 그대로 닳는다(신발이 다 닳았다). 걸음 0 인 러닝은 예전처럼 켜 둔 시간만큼(0068)
  -- 효율 포인트를 넣은 신발은 그 배수만큼 빨리 닳는다(0119 — v_wear_mult, 크루 러닝 · 신발 없음은 1)
  v_wear := case when v_pool = 'CREW' then 0 else round(economy.durability_wear_time(v_elapsed) * v_wear_luck * v_wear_mult
              * least(1, greatest(0, (1 - v_lost_cap::numeric / greatest(v_verified, 1))
                                     * case when v_points_full > 0 then v_points / v_points_full else 1 end)))::int end;
  if v_shoe.id is not null and v_verdict <> 'VOID' and v_shoe.status <> 'BURNED' then
    update public.market_sneakers s
       set km_run = s.km_run + round((v_credit_m / 1000)::numeric, 3),
           dur_now = case when v_pen.user_id is not null
                            then greatest(s.dur_now + (greatest(v_pen.dur_before - v_wear, 0) / 2 - v_pen.dur_after), 0)
                          when v_pool = 'CREW' then s.dur_now
                          else greatest(s.dur_now - v_wear, 0) end,
           updated_at = now()
     where s.id = v_shoe.id;
  end if;

  if v_verdict <> 'VOID' then
    update public.profiles
       set lifetime_km = lifetime_km + (v_distance_m / 1000),
           gps_km = gps_km + (v_credit_m / 1000),
           top_speed_kmh = greatest(top_speed_kmh, v_top_speed)
     where id = v_user;
  end if;

  -- 이 러닝에 묶인 아이템을 러닝 기록에 잇고, 적용된 적립 배수를 적는다 (0106)
  update economy.item_uses u set session_id = v_session_id
   where u.user_id = v_user and u.run_started_at = p_started_at;
  if v_xp <> 1 then
    update public.walk_sessions s set reward_mult = v_xp where s.id = v_session_id;
  end if;
  if v_verdict <> 'VOID' then
    update public.walk_sessions s
       set reward_luck = round(v_luck, 3),
           wear_luck = case when v_pool = 'CREW' then null else round(v_wear_luck, 3) end
     where s.id = v_session_id;
  end if;

  -- 함께 달린 보너스(0120) — 이 방에서 함께 달린 사람(10분 · 경로로 확인된 1,500보)을 세어 이미 올린 모두에게 차이만큼 더한다(나도).
  -- 그 뒤 내 러닝의 적립을 다시 읽어 돌려준다(이미 다른 사람이 올렸으면 내 몫의 인원 보너스가 바로 든다)
  if v_tb is not null and v_party_id is not null and v_verdict <> 'VOID' then
    perform economy.together_settle(v_party_id);
    select s.points_awarded into v_points from public.walk_sessions s where s.id = v_session_id;
  end if;
  -- 0122: 남이 올릴 때 내가 마침 다른 것을 기록하던 중이라 미뤄 둔 정산 — 내 원장 자물쇠를 잡은 지금 한다
  if exists (select 1 from economy.together_pending q where q.user_id = v_user) then
    perform economy.together_pending_run(v_user);
    select s.points_awarded into v_points from public.walk_sessions s where s.id = v_session_id;
  end if;

  -- 목표 보너스 — 그 날의 보상 걸음이 그 날의 목표를 채웠고 아직 안 받았으면 서버가 바로 준다(예전 앱에도, 0118).
  -- 보너스에 문제가 생겨도 러닝 기록은 남긴다
  if v_reward_steps > 0 then
    begin
      perform economy.goal_settle(v_user, v_gday);
    exception when others then
      raise warning 'goal_settle(%, %) 실패: %', v_user, v_gday, sqlerrm;
    end;
  end if;

  return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user), v_reason,
    case when v_verdict = 'VOID' then null else round(v_luck, 3) end;
end;
$$;

comment on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint) is
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발(출발 때 신은 신발 — 내 것일 때만)·에너지·크루 러닝·파티·부스터·보상 이벤트·러닝 아이템·상한 모두 서버 값으로. 판정 까닭도 돌려준다 (0089) · 운 · 상한 배수 (0116) · 보상 걸음 · 목표 보너스 (0118) · 닳은 뒤 적립 비율 · 효율 포인트의 닳음 / 에너지 (0119) · 함께 달린 사람만 · 하루 최대 SUP (0120) · 묶인 체력 · 8 : 2 무게 · 번개 한 번 · 미룬 정산 (0122)';
revoke all on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint) from public, anon;
grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint) to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  12. 권한
-- ════════════════════════════════════════════════════════════════════
revoke all on function economy.crew_pool_held_except(uuid, date, timestamptz, uuid) from public, anon, authenticated;
revoke all on function economy.crew_pool_held(uuid, date, timestamptz) from public, anon, authenticated;
revoke all on function economy.crew_pool_held_where(economy.crew_runs, timestamptz) from public, anon, authenticated;
revoke all on function economy.crew_blend_avg(bigint, uuid, numeric, numeric) from public, anon, authenticated;
revoke all on function economy.crew_blend_mult(bigint, uuid, numeric, numeric) from public, anon, authenticated;
revoke all on function economy.together_pending_run(uuid) from public, anon, authenticated;
revoke all on function economy.together_qualifies(public.walk_sessions) from public, anon, authenticated;
revoke all on function economy.day_earned(uuid, date) from public, anon, authenticated;
revoke all on function economy.together_settle(bigint) from public, anon, authenticated;
revoke all on function economy.goal_settle(uuid, date) from public, anon, authenticated;
revoke all on function economy.goal_streak_before(uuid, date, int) from public, anon, authenticated;
revoke all on function economy.runner_steps_of(uuid) from public, anon, authenticated;
revoke all on function economy.forge_account_after(uuid, bigint, int, bigint[]) from public, anon, authenticated;
revoke all on function economy.box_on_level() from public;
revoke all on function public.market_import(bigint, text, text, int, int, numeric, numeric, int) from public, anon;
grant execute on function public.market_import(bigint, text, text, int, int, numeric, numeric, int) to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  13. 한 번만: 에너지 상한 · 가입 선물 표시 · 러너 걸음 다시 세기 · 하루 최대에 막혔던 목표 날
-- ════════════════════════════════════════════════════════════════════
do $$
begin
  if not exists (select 1 from public.economy_settings where key = 'audit_fix_20261122') then
    -- A. 인플레이션 — 0121 이 넣은 기본값(16.5)일 때만 10 으로. 운영자가 바꿔 둔 값은 건드리지 않는다
    update public.economy_settings set value = '10'::jsonb, updated_at = now()
     where key = 'runner_energy_cap' and value = '16.5'::jsonb;
    -- 11. 이미 무료 뽑기 줄이 있는 계정은 가입 선물을 받은 것으로 센다(새 길이 한 번 더 주지 않게)
    insert into economy.box_awards (user_id, source, kind, day)
    select d.user_id, 'SIGNUP', 'FREE', economy.game_day(d.created_at)
      from public.draw_grants d where d.kind = 'FREE'
    on conflict (user_id, source) do nothing;
    -- 4. 지난 7일 가운데 목표를 채웠는데 하루 최대 SUP 에 막혀 아무것도 남지 않은 날
    insert into economy.goal_days (user_id, day, goal, met_at)
    select u.id, d.day::date, economy.goal_peek(u.id, d.day::date), now()
      from auth.users u
     cross join generate_series(economy.game_day(now()) - 7, economy.game_day(now()) - 1, interval '1 day') d(day)
     where economy.daily_max_on()
       and not exists (select 1 from public.sup_ledger l where l.user_id = u.id and l.ref = 'goal:' || d.day::date)
       and exists (select 1 from public.walk_sessions s where s.user_id = u.id and economy.game_day(s.started_at) = d.day::date)
       and economy.verified_steps_on(u.id, d.day::date) >= economy.goal_peek(u.id, d.day::date)
       and economy.day_room(u.id, d.day::date, null) <= 0
    on conflict (user_id, day) do update set met_at = coalesce(economy.goal_days.met_at, excluded.met_at);
    -- 3. 상한에 걸린 러닝까지 센 걸음으로 레벨을 다시 — 새로 닿은 레벨의 상자를 준다(러너 규칙이 켜져 있을 때만)
    perform economy.runner_refresh(u.id) from auth.users u;
    insert into public.economy_settings (key, value) values ('audit_fix_20261122', to_jsonb(now()));
  end if;
end $$;
