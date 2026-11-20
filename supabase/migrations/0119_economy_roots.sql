-- 0119 — 경제의 뿌리 고치기: 내구도 요율 · 능력치 조율 · 통의 굽이 · 에너지 · 포인트 미리보기 (2026-11-21)
--
-- 사용자 "이 문제들의 뿌리를 뽑아버려야함"(6번 SUP 쓸 곳은 빼고) → "진행해 · 효율이 무조건 좋지 않게 스텟을 잘 조율" —
-- docs/economy-roots-2026-11-21.md.
--   가) 내구도는 "문"이 아니라 요율: 다 닳은 뒤의 걸음도 다 세고 적립은 30%(worn_rate). 0% 신발로도 달린다.
--       보상 걸음(상자 · 목표)은 경로로 확인된 걸음 모두(신발 · 크루 체력과 상관없이).
--   나) 눈금: 강화 통은 레벨마다 Lv1 통의 30%씩 커진다(레어 만렙 24일 · 에픽 119일 — 예전 확률 강화의 속도, 예전 앱 성공률도 같은 굽이).
--       하루 에너지는 레벨과 떼었다(energy_per_level 0 — 에너지를 늘리는 길은 착화감).
--   다) 능력치: 효율 = 버는 속도(그만큼 신발 · 에너지를 빨리 쓴다) · 내구도 = 한 켤레로 버는 양(수리비는 늘지 않는다) ·
--       착화감 = 하루에 버는 양. 포인트 화면이 내 지난 14일 기록으로 값을 말하고(sneaker_points_preview) 5레벨마다 다시 나눈다.
--
-- 모든 값은 설정이고, 여섯 개를 예전 값으로 돌리면 0118 과 똑같이 셈한다:
--   worn_rate 0 · eff_wear_exp 0 · eff_energy_exp 0 · dur_repair_exp 0 · energy_per_level 2 · forge_xp_curve "CLASSIC".
-- 함수 모양(인자 · 돌려주는 값)은 하나도 바꾸지 않았다. 새 함수는 sneaker_points_preview 하나.
-- record_session 은 0119 가 마지막 정의다 — 다시 고칠 때 0119 를 복사한다(0118 본문 + 닳은 구간 · 효율 배수 · 보상 걸음).

-- ════════════════════════════════════════════════════════════════════
--  1. 설정 — 모두 배포 없이 바꾼다(운영자 페이지 "경제")
-- ════════════════════════════════════════════════════════════════════
insert into public.economy_settings (key, value) values
  ('worn_rate', '0.30'),               -- 신발이 다 닳은 뒤 적립 비율(0 = 예전처럼 적립 없음)
  ('eff_wear_exp', '1'),               -- 효율 포인트가 닳는 속도를 올리는 세기(0 = 끔 · 1 = 버는 속도만큼)
  ('eff_energy_exp', '1'),             -- 효율 포인트가 에너지 소모를 올리는 세기
  ('energy_per_level', '0'),           -- 레벨마다 늘어나는 하루 에너지(예전 2)
  ('forge_xp_level_growth', '0.30'),   -- 통이 레벨마다 커지는 비율(Lv1 통의 30%씩)
  ('dur_repair_exp', '1'),             -- 내구도 포인트가 가득 수리비를 올리지 않는 세기(0 = 예전처럼 늘어난 만큼 더 낸다) — SQL 창에서만
  ('forge_xp_curve', '"LINEAR"')       -- 'CLASSIC' 이면 0118 의 굽이(예전 확률 강화의 평균 비용) — SQL 창에서만
on conflict (key) do nothing;

-- ════════════════════════════════════════════════════════════════════
--  2. 능력치 — 효율 = 버는 속도 · 내구도 = 한 켤레로 버는 양 · 착화감 = 하루에 버는 양
-- ════════════════════════════════════════════════════════════════════
-- 효율 포인트 배수(뽑은 값 위에 곱하는 몫) — 예전 신발은 포인트가 없다
create or replace function economy.eff_point_mult(s public.market_sneakers)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select case when s.id is null or s.origin in ('IMPORT', 'MINT') then 1
              else 1 + economy.point_rate_of(s.rarity) * coalesce(s.pts_eff, 0) / 10000.0 end
$$;
-- 내구도 포인트 배수(최대 내구도 ÷ 뽑은 내구도)
create or replace function economy.dur_point_mult(s public.market_sneakers)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select case when s.id is null or s.origin in ('IMPORT', 'MINT') then 1
              else 1 + economy.point_rate_of(s.rarity) * coalesce(s.pts_dur, 0) / 10000.0 end
$$;
-- 효율 포인트는 같은 시간에 더 벌게 하지만 그만큼 신발과 에너지를 빨리 쓴다(세기는 설정) — 한 켤레 · 하루에 벌 수 있는 양은 효율로 늘지 않는다
create or replace function economy.wear_mult(s public.market_sneakers)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select power(economy.eff_point_mult(s), least(greatest(economy.fx('eff_wear_exp', 1), 0), 1))
$$;
create or replace function economy.energy_mult(s public.market_sneakers)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select power(economy.eff_point_mult(s), least(greatest(economy.fx('eff_energy_exp', 1), 0), 1))
$$;
create or replace function economy.worn_rate()
returns numeric language sql stable security definer set search_path = public, economy as $$
  -- 0.45 를 넘기면 수리하지 않는 쪽이 이득이 된다(걷기는 수리비를 내고 55% 가 남는다) — 그 위는 받지 않는다
  select least(greatest(economy.fx('worn_rate', 0.30), 0), 0.45)
$$;

-- 하루 에너지 — 레벨과 떼었다(설정 energy_per_level, 기본 0). 에너지를 늘리는 길은 착화감이다
create or replace function economy.energy_max_for_level(p_level integer)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select economy.base_max_energy() + greatest(coalesce(p_level, 1) - 1, 0) * greatest(economy.fx('energy_per_level', 0), 0)
$$;

-- 수리비 — 내구도 1점 값. 가득 수리비는 포인트를 어디에 넣든 같다(뽑은 효율 · 뽑은 내구도가 정한다):
--   효율 포인트는 닳는 속도를 올리고(wear_mult) 그만큼 1점 값을 내린다 · 내구도 포인트는 점수를 늘리고 그만큼 1점 값을 내린다.
--   → 분당 수리비 = (뽑은 값의 분당 수리비) × 효율 배수 ÷ 내구도 배수. 내구도 포인트는 누구에게나 수리비를 낮춘다
create or replace function economy.repair_unit_of(s public.market_sneakers)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select round(coalesce(economy.setting_num('repair_share'), 0.45) * 110 * economy.points_per_step() / 5
               * (10000 + (economy.shoe_effective(s)).efficiency_bps) / 10000.0
               / economy.wear_mult(s)
               / power(economy.dur_point_mult(s), least(greatest(economy.fx('dur_repair_exp', 1), 0), 1)), 8)
$$;

-- ════════════════════════════════════════════════════════════════════
--  3. 강화 통 — 레벨마다 Lv1 통의 30%씩 커진다(걸리는 날을 예전 확률 강화와 같게: 레어 24일 · 에픽 119일)
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.forge_curve_classic()
returns boolean language sql stable security definer set search_path = public, economy as $$
  select upper(coalesce(economy.setting('forge_xp_curve') #>> '{}', 'LINEAR')) = 'CLASSIC'
$$;
create or replace function economy.forge_level_factor(p_level int)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select case when economy.forge_curve_classic() then 1.03 / (1.06 - 0.03 * greatest(coalesce(p_level, 1), 1))
              else 1 + greatest(economy.fx('forge_xp_level_growth', 0.30), 0) * (greatest(coalesce(p_level, 1), 1) - 1) end
$$;
create or replace function economy.forge_need(p_rarity text, p_level int)
returns int language sql stable security definer set search_path = public, economy as $$
  select case when p_level is null or economy.rarity_ord(p_rarity) is null or p_level >= economy.max_level(p_rarity) then null
              else round(1000 * economy.fx('forge_xp_need', 2.88)
                         * power(economy.fx('forge_xp_step', 3.1), greatest(economy.rarity_ord(p_rarity) - 1, 0))
                         * economy.forge_level_factor(p_level))::int end
$$;
-- 예전 앱의 확률 강화도 같은 굽이 — 안 맞추면 예전 앱이 싼 뒷문이 된다. 재료 3켤레의 평균 비용(3 ÷ 성공률)이 통보다 싸지 않게:
-- 같은 등급 Lv1 재료 3켤레(보너스 90 — 일반 신발은 이것뿐)일 때 통과 같고, 한 등급 아래 재료(보너스 60)면 조금 더 든다.
-- 통을 아주 크게 키우면(Lv1 통 × 레벨 몫이 33켤레값을 넘으면) 기본 확률이 0 에서 멈춰 재료 보너스만 남고 예전 앱이 다시 싸진다 —
-- 운영자 페이지의 범위(forge_xp_need 3.2 · forge_xp_level_growth 0.33 까지)는 그 안쪽이다. SQL 창에서 더 키울 때는 이 식을 함께 본다
create or replace function economy.forge_base_permille(p_level integer)
returns integer language sql stable security definer set search_path = public, economy as $$
  select case when economy.forge_curve_classic() then greatest(0, 1000 - 30 * p_level)
              else greatest(0, round(3000 / (economy.fx('forge_xp_need', 2.88) * economy.forge_level_factor(p_level)))::int - 90) end
$$;

-- ════════════════════════════════════════════════════════════════════
--  4. 포인트 — 5레벨마다 다시 나누기 · 내 기록으로 값 미리보기
-- ════════════════════════════════════════════════════════════════════
-- 5 · 10 · 15 … 레벨에 닿으면 그 신발 포인트를 한 번 다시 나눌 수 있다(pts_respec — 낮추는 순간 쓴다)
create or replace function economy.sneaker_respec_on_milestone()
returns trigger language plpgsql as $$
begin
  if new.level is not null and old.level is not null and new.level / 5 > old.level / 5 then
    new.pts_respec := true;
  end if;
  return new;
end $$;
drop trigger if exists market_sneakers_respec_milestone on public.market_sneakers;
create trigger market_sneakers_respec_milestone before update of level on public.market_sneakers
  for each row execute function economy.sneaker_respec_on_milestone();

-- 한 번만: 능력치 규칙이 바뀌었으니 포인트를 나눈 모든 신발에 다시 나누기 한 번
do $$
begin
  if not exists (select 1 from public.economy_settings where key = 'stat_rebalance_20261121') then
    update public.market_sneakers set pts_respec = true
     where owner_id is not null and status <> 'BURNED' and pts_eff + pts_comfort + pts_dur > 0 and not pts_respec;
    insert into public.economy_settings (key, value) values ('stat_rebalance_20261121', to_jsonb(now()));
  end if;
end $$;

-- 포인트 나누기 — 0116 본문 그대로, 한 곳만: 내구도 포인트를 바꾸면 남은 내구도는 같은 비율로(가득이면 가득 · 절반이면 절반)
create or replace function public.sneaker_points_set(p_id bigint, p_efficiency integer, p_comfort integer, p_durability integer,
    p_request uuid default null)
returns integer
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v public.market_sneakers;
  v_prev numeric;
  v_total int;
  v_lowered boolean;
  v_free int;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  if p_request is not null then
    select r.result into v_prev from economy.request_receipts r
     where r.user_id = v_user and r.request_id = p_request;
    if found then
      return v_prev::int;
    end if;
  end if;

  v := economy.my_app_sneaker(v_user, p_id);
  if v.origin in ('IMPORT', 'MINT') then
    raise exception '예전 신발은 포인트가 없습니다' using errcode = '22023';
  end if;
  if p_efficiency is null or p_comfort is null or p_durability is null
     or p_efficiency < 0 or p_comfort < 0 or p_durability < 0 then
    raise exception '포인트가 올바르지 않습니다' using errcode = '22023';
  end if;

  v_total := economy.points_total_of(v.origin, v.rarity, v.level);
  if p_efficiency + p_comfort + p_durability > v_total then
    raise exception '포인트가 모자랍니다' using errcode = '23514';
  end if;
  v_lowered := p_efficiency < v.pts_eff or p_comfort < v.pts_comfort or p_durability < v.pts_dur;
  if v_lowered and not v.pts_respec then
    raise exception '나눈 포인트는 되돌릴 수 없습니다' using errcode = '23514';
  end if;
  if p_comfort > v.pts_comfort
     and economy.comfort_with_points(v.comfort_bps, v.rarity, p_comfort) > economy.comfort_cap_bps() then
    raise exception '착화감은 에너지 70%%를 아끼는 데까지 올릴 수 있습니다' using errcode = '23514';
  end if;

  update public.market_sneakers
     set pts_eff = p_efficiency,
         pts_comfort = p_comfort,
         pts_dur = p_durability,
         -- 남은 내구도는 비율을 지킨다(0119) — 예전엔 늘어난 만큼 더하고 줄어든 만큼 뺐다: 다 닳은 신발의 내구도 포인트를
         -- 다시 나누기로 뺐다가 다시 넣으면 늘어난 만큼이 공짜로 찼고(가득 수리의 절반 가까이), 닳은 신발에서 빼면 남은 것이 0 이 됐다
         dur_now = least(round(dur_now::numeric * economy.dur_max_of(v.dur_base, v.rarity, p_durability)
                               / greatest(economy.dur_max_of(v.dur_base, v.rarity, v.pts_dur), 1))::int,
                         economy.dur_max_of(v.dur_base, v.rarity, p_durability)),
         pts_respec = pts_respec and not v_lowered,
         updated_at = now()
   where id = p_id;

  v_free := v_total - p_efficiency - p_comfort - p_durability;
  if p_request is not null then
    insert into economy.request_receipts (user_id, request_id, kind, result)
    values (v_user, p_request, 'POINTS:' || p_id, v_free);
  end if;
  return v_free;
end $$;

-- 포인트를 [효율 pe · 착화감 pc · 내구도 pd]로 나눴을 때 "달린 날 하루에 남는 SUP"(적립 − 수리비) — 내 러닝들을 그 신발로 다시 달려 본다.
-- 러닝마다 신발은 가득에서 출발한다고 본다(러닝 사이에 수리). 정산(record_session)과 같은 구간 셈: 가득 구간 → 닳은 구간, 에너지가 끝나면 거기까지.
-- p_runs: [{d: 날, o: 차례, n: 걸음, t: 초}]
create or replace function economy.points_day_net(s public.market_sneakers, p_runs jsonb, p_energy numeric,
    p_pe int, p_pc int, p_pd int, out net numeric, out worn_runs int, out energy_out_days int, out life_min numeric)
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_rate numeric := economy.point_rate_of(s.rarity) / 10000.0;
  v_u numeric := 1 + v_rate * p_pe;
  v_v numeric := 1 + v_rate * p_pd;
  v_wm numeric := power(v_u, least(greatest(economy.fx('eff_wear_exp', 1), 0), 1));
  v_em numeric := power(v_u, least(greatest(economy.fx('eff_energy_exp', 1), 0), 1));
  v_worn numeric := economy.worn_rate();
  v_comfort numeric := least(economy.comfort_with_points(s.comfort_bps, s.rarity, p_pc), economy.comfort_cap_bps()) / 10000.0;
  v_dmax numeric := economy.dur_max_of(s.dur_base, s.rarity, p_pd);
  v_pps numeric := economy.points_per_step() * (10000 + s.efficiency_bps) / 10000.0 * v_u;      -- 걸음당 SUP(가득 구간)
  v_unit numeric := coalesce(economy.setting_num('repair_share'), 0.45) * 110 * economy.points_per_step() / 5
                    * (10000 + s.efficiency_bps) / 10000.0 * v_u / v_wm
                    / power(v_v, least(greatest(economy.fx('dur_repair_exp', 1), 0), 1));        -- 내구도 1점 수리비
  v_e numeric := (1 - v_comfort) / economy.steps_per_energy() * v_em;                           -- 걸음당 에너지
  r record; v_day text := null; v_left numeric := 0; v_days int := 0; v_total numeric := 0;
  v_life int; v_want int; v_can int; v_full int; v_tail int; v_out boolean := false;
begin
  worn_runs := 0; energy_out_days := 0;
  life_min := round(v_dmax / 5 / v_wm, 1);
  for r in select x ->> 'd' as d, greatest((x ->> 'n')::int, 0) as n, greatest((x ->> 't')::int, 1) as t
             from jsonb_array_elements(coalesce(p_runs, '[]'::jsonb)) x order by x ->> 'd', (x ->> 'o')::int loop
    if v_day is distinct from r.d then
      v_day := r.d; v_days := v_days + 1; v_left := p_energy; v_out := false;
    end if;
    v_life := case when v_dmax * 12 / v_wm < r.t then floor(r.n * v_dmax * 12 / v_wm / r.t)::int else r.n end;
    if v_life < r.n then worn_runs := worn_runs + 1; end if;
    v_want := case when v_worn > 0 then r.n else v_life end;
    v_can := least(v_want, greatest(floor(v_left / v_e)::int, 0));
    if v_can < v_want and not v_out then
      energy_out_days := energy_out_days + 1; v_out := true;
    end if;
    v_full := least(v_can, v_life);
    v_tail := v_can - v_full;
    v_left := greatest(v_left - v_can * v_e, 0);
    v_total := v_total + (v_full + v_tail * v_worn) * v_pps
               - least(v_dmax, 5 * floor(r.t / 60.0) * v_wm * v_can / greatest(r.n, 1)) * v_unit;
  end loop;
  net := case when v_days > 0 then round(v_total / v_days, 2) else 0 end;
end $$;

-- 남은 포인트 p_free 를 조금씩, 하루에 가장 많이 남는 쪽(하나 또는 둘을 반씩)에 넣는다. 같은 값이면 효율 → 내구도 → 착화감
create or replace function economy.points_recommend(s public.market_sneakers, p_runs jsonb, p_energy numeric,
    p_pe int, p_pc int, p_pd int, p_free int, p_cap_pc int, out pe int, out pc int, out pd int)
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_left int := greatest(coalesce(p_free, 0), 0);
  v_step int := greatest(1, ceil(greatest(coalesce(p_free, 0), 0) / 24.0)::int);
  v_k text; v_top numeric; v_net numeric; v_cur numeric;
  a int; b int; c int; t_e int; t_c int; t_d int; v_room int;
begin
  pe := p_pe; pc := p_pc; pd := p_pd;
  while v_left > 0 loop
    v_step := least(v_step, v_left); v_top := null;
    v_room := greatest(p_cap_pc - pc, 0);
    select n.net into v_cur from economy.points_day_net(s, p_runs, p_energy, pe, pc, pd) n;
    foreach v_k in array array['E', 'D', 'C', 'ED', 'EC', 'CD'] loop
      a := case v_k when 'E' then v_step when 'ED' then (v_step + 1) / 2 when 'EC' then (v_step + 1) / 2 else 0 end;
      c := least(case v_k when 'C' then v_step when 'EC' then v_step / 2 when 'CD' then (v_step + 1) / 2 else 0 end, v_room);
      b := case v_k when 'D' then v_step when 'ED' then v_step / 2 when 'CD' then v_step / 2 else 0 end;
      continue when a + b + c <= 0;
      -- 착화감 상한에 막혀 남는 몫은 이번 걸음에 쓰지 않는다(다음 걸음이 다른 능력치에 넣는다) — 포인트당 늘어나는 값으로 견준다
      select n.net into v_net from economy.points_day_net(s, p_runs, p_energy, pe + a, pc + c, pd + b) n;
      if v_top is null or (v_net - v_cur) / (a + b + c) > v_top + 0.00001 then
        v_top := (v_net - v_cur) / (a + b + c); t_e := a; t_c := c; t_d := b;
      end if;
    end loop;
    pe := pe + t_e; pc := pc + t_c; pd := pd + t_d;
    v_left := v_left - t_e - t_c - t_d;
  end loop;
end $$;

-- 포인트 미리보기 — 능력치마다 "다음 1포인트 = 하루 약 +N SUP"(내 지난 14일 기록으로) + 남은 포인트를 나누는 추천.
-- 기록이 없으면 30분 달리기 하루 한 번으로 본다(basis.source = DEFAULT). 읽기만 한다
create or replace function public.sneaker_points_preview(p_id bigint)
returns jsonb language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  s public.market_sneakers;
  v_runs jsonb;
  v_source text := 'MINE';
  v_energy numeric;
  v_total int; v_free int; v_cap_pc int; v_k int;
  v_now record; v_e record; v_c record; v_d record; v_rec record; v_best record; v_all jsonb := null;
  v_n_runs int; v_n_days int; v_avg_min numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select * into s from public.market_sneakers where id = p_id and owner_id = v_user and status <> 'BURNED';
  if not found or s.origin in ('IMPORT', 'MINT') then
    return jsonb_build_object('ok', false, 'code', 'NO_POINTS');
  end if;
  select jsonb_agg(jsonb_build_object('d', economy.game_day(w.started_at), 'o', w.o, 'n', w.n, 't', w.duration_sec)),
         count(*), count(distinct economy.game_day(w.started_at)), round(avg(w.duration_sec) / 60.0)
    into v_runs, v_n_runs, v_n_days, v_avg_min
    from (select x.started_at, x.duration_sec, coalesce(x.verified_steps, x.steps) as n,
                 row_number() over (order by x.started_at) as o
            from public.walk_sessions x
           where x.user_id = v_user and x.verdict <> 'VOID' and x.energy_pool <> 'CREW'
             and x.started_at > now() - interval '14 days' and coalesce(x.verified_steps, x.steps) > 0 and x.duration_sec >= 60
           order by x.started_at desc limit 60) w;
  if v_runs is null then
    v_source := 'DEFAULT'; v_n_runs := 0; v_n_days := 0; v_avg_min := 30;
    v_runs := jsonb_build_array(jsonb_build_object('d', 'x', 'o', 1, 'n', 4950, 't', 1800));
  end if;
  v_energy := economy.energy_max(v_user);
  v_total := economy.points_total_of(s.origin, s.rarity, s.level);
  v_free := greatest(v_total - s.pts_eff - s.pts_comfort - s.pts_dur, 0);
  -- 착화감은 에너지 70% 절약까지 — 거기에 닿는 포인트 수(이미 넘겼으면 지금 값)
  v_cap_pc := greatest(floor(((10000 - s.comfort_bps)::numeric / (10000 - economy.comfort_cap_bps()) - 1) * 10000
                             / greatest(economy.point_rate_of(s.rarity), 1))::int, s.pts_comfort, 0);

  select * into v_now from economy.points_day_net(s, v_runs, v_energy, s.pts_eff, s.pts_comfort, s.pts_dur);
  -- 다음 포인트의 값 — 한 점은 버림에 묻혀 여러 점(전체의 5%쯤)을 넣어 보고 나눈다
  v_k := greatest(1, round(v_total * 0.05)::int);
  select * into v_e from economy.points_day_net(s, v_runs, v_energy, s.pts_eff + v_k, s.pts_comfort, s.pts_dur);
  select * into v_c from economy.points_day_net(s, v_runs, v_energy, s.pts_eff, least(s.pts_comfort + v_k, v_cap_pc), s.pts_dur);
  select * into v_d from economy.points_day_net(s, v_runs, v_energy, s.pts_eff, s.pts_comfort, s.pts_dur + v_k);

  select * into v_rec from economy.points_recommend(s, v_runs, v_energy, s.pts_eff, s.pts_comfort, s.pts_dur, v_free, v_cap_pc);
  select * into v_best from economy.points_day_net(s, v_runs, v_energy, v_rec.pe, v_rec.pc, v_rec.pd);

  -- 다시 나눌 수 있으면: 모든 포인트를 처음부터 나누는 추천
  if s.pts_respec and v_total > 0 then
    declare a record; b record;
    begin
      select * into a from economy.points_recommend(s, v_runs, v_energy, 0, 0, 0, v_total,
        greatest(floor(((10000 - s.comfort_bps)::numeric / (10000 - economy.comfort_cap_bps()) - 1) * 10000
                       / greatest(economy.point_rate_of(s.rarity), 1))::int, 0));
      select * into b from economy.points_day_net(s, v_runs, v_energy, a.pe, a.pc, a.pd);
      v_all := jsonb_build_object('efficiency', a.pe, 'comfort', a.pc, 'durability', a.pd,
                                  'day_net', b.net, 'life_min', b.life_min);
    end;
  end if;

  return jsonb_build_object(
    'ok', true,
    'basis', jsonb_build_object('source', v_source, 'days', v_n_days, 'runs', v_n_runs, 'avg_min', v_avg_min),
    'total', v_total, 'free', v_free, 'respec', s.pts_respec,
    'now', jsonb_build_object('efficiency', s.pts_eff, 'comfort', s.pts_comfort, 'durability', s.pts_dur,
                              'day_net', v_now.net, 'life_min', v_now.life_min,
                              'worn_runs', v_now.worn_runs, 'energy_out_days', v_now.energy_out_days),
    'per_point', jsonb_build_object(
      'efficiency', round(greatest(v_e.net - v_now.net, 0) / v_k, 2),
      'comfort', case when s.pts_comfort >= v_cap_pc then 0 else round(greatest(v_c.net - v_now.net, 0) / least(v_k, v_cap_pc - s.pts_comfort), 2) end,
      'durability', round(greatest(v_d.net - v_now.net, 0) / v_k, 2)),
    'life_min_per_point', jsonb_build_object(
      'efficiency', round((v_e.life_min - v_now.life_min) / v_k, 2),
      'durability', round((v_d.life_min - v_now.life_min) / v_k, 2)),
    'comfort_capped', s.pts_comfort >= v_cap_pc,
    'recommend', jsonb_build_object('efficiency', v_rec.pe - s.pts_eff, 'comfort', v_rec.pc - s.pts_comfort,
                                    'durability', v_rec.pd - s.pts_dur, 'day_net', v_best.net, 'life_min', v_best.life_min),
    'recommend_all', v_all);
end $$;

-- ════════════════════════════════════════════════════════════════════
--  5. 규칙 묶음 · 운영자 손잡이(30 → 35)
-- ════════════════════════════════════════════════════════════════════
create or replace function public.shoe_rules_v4()
returns jsonb language plpgsql stable security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  return jsonb_build_object(
    'points_per_step', economy.points_per_step(),
    'repair_share', coalesce(economy.setting_num('repair_share'), 0.45),
    'reward_luck', economy.setting('reward_luck'),
    'wear_luck', economy.setting('wear_luck'),
    'comfort_cap_bps', economy.comfort_cap_bps(),
    'free_repair_hours', economy.setting_num('free_repair_hours'),
    'free_repair_ready', case when v_user is null then null else economy.free_repair_ready(v_user) end,
    'free_repair_next', case when v_user is null then null else economy.free_repair_next(v_user) end,
    'forge_material_min_durability', economy.setting_num('forge_material_min_durability'),
    'boost_shop_enabled', coalesce((economy.setting('boost_shop_enabled') #>> '{}')::boolean, false),
    'grades', (select jsonb_agg(jsonb_build_object(
        'rarity', g.r,
        'efficiency', jsonb_build_array((economy.efficiency_range(g.r)).lo, (economy.efficiency_range(g.r)).hi),
        'comfort', jsonb_build_array((economy.comfort_range(g.r)).lo, (economy.comfort_range(g.r)).hi),
        'durability', jsonb_build_array((economy.durability_range(g.r)).lo, (economy.durability_range(g.r)).hi),
        'points_per_level', economy.points_per_level_of(g.r),
        'points_milestone', economy.points_milestone_of(g.r),
        'point_rate_bps', economy.point_rate_of(g.r),
        'max_level', economy.max_level(g.r)) order by g.o)
      from (values ('COMMON', 0), ('RARE', 1), ('EPIC', 2), ('LEGENDARY', 3), ('REDLINE', 4), ('FINISH', 5)) g(r, o)),
    -- 0118 — 강화 경험치 · 목표(앱이 미리보기 · 안내 글에 쓴다 — 값은 늘 서버 견적이 정한다)
    'forge', jsonb_build_object(
        'rule_version', 'forge-xp-1',
        'step', economy.fx('forge_xp_step', 3.1),
        'need', economy.fx('forge_xp_need', 2.88),
        'spec', coalesce(economy.setting('forge_xp_spec'), '[0.7, 1.3]'::jsonb),
        'crit_chance', economy.fx('forge_xp_crit_chance', 0.1),
        'crit_mult', economy.fx('forge_xp_crit_mult', 2),
        'inherit', economy.fx('forge_xp_inherit', 0.5),
        'sup_per_unit', economy.fx('forge_xp_sup_per_unit', 24),
        'max_materials', economy.fx('forge_xp_max_materials', 10)::int,
        -- 0119 — 통의 굽이: LINEAR = Lv1 통 × (1 + level_growth × (레벨 − 1)) · CLASSIC = 0118 식
        'curve', case when economy.forge_curve_classic() then 'CLASSIC' else 'LINEAR' end,
        'level_growth', greatest(economy.fx('forge_xp_level_growth', 0.30), 0)),
    -- 0119 — 능력치 규칙(앱이 수명 · 에너지 · 닳은 구간을 미리 보일 때 쓴다 — 정산은 늘 서버)
    --   수명(분) = 최대 내구도 ÷ (wear_per_minute × 효율 포인트 배수 ^ eff_wear_exp) · 걸음당 에너지 × 효율 포인트 배수 ^ eff_energy_exp
    --   가득 수리비 = 뽑은 내구도 × 1점 값(효율 · 내구도 포인트와 상관없다 — dur_repair_exp 1) · 닳은 뒤 적립 = worn_rate
    'stats', jsonb_build_object(
        'rule_version', 'stats-2',
        'worn_rate', economy.worn_rate(),
        'eff_wear_exp', least(greatest(economy.fx('eff_wear_exp', 1), 0), 1),
        'eff_energy_exp', least(greatest(economy.fx('eff_energy_exp', 1), 0), 1),
        'dur_repair_exp', least(greatest(economy.fx('dur_repair_exp', 1), 0), 1),
        'wear_per_minute', 5,
        'energy_base', economy.base_max_energy(),
        'energy_per_level', greatest(economy.fx('energy_per_level', 0), 0),
        'steps_per_energy', economy.steps_per_energy(),
        'respec_every', 5),
    'goal', jsonb_build_object(
        'min', economy.fx('goal_min', 4000)::int, 'max', economy.fx('goal_max', 20000)::int,
        'default', economy.fx('goal_default', 6000)::int, 'step', 500));
end $$;

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
    ('forge_xp_level_growth', 'num', 0, 0.33, '강화 경험치: 통이 레벨마다 커지는 몫(Lv1 통의 배)')
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
                   ('forge_xp_level_growth', 35)) k(key, o)
      left join public.economy_settings s on s.key = k.key));
end $$;

-- ════════════════════════════════════════════════════════════════════
--  6. 러닝 기록 — record_session(0118 본문 복사 + 닳은 구간 · 효율 배수 · 보상 걸음)
-- ════════════════════════════════════════════════════════════════════
-- 0118 본문 그대로, 다섯 곳만 (0119): ① 남은 내구도로 달린 걸음과 닳은 뒤 걸음을 가른다(닳은 뒤는 worn_rate 만큼 적립)
-- ② 효율 포인트 배수만큼 빨리 닳고 ③ 에너지를 더 쓴다 ④ 보상 걸음은 경로로 확인된 걸음 모두 ⑤ 까닭 '신발이 다 닳은 뒤의 걸음은 일부만 적립했습니다'.
-- 다시 고칠 때는 이 본문(0119)을 복사한다.
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
            where s.user_id = v_user and x.flash_post_id = v_flash and s.party_size > 1 and s.verdict <> 'VOID')) then
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
    perform economy.crew_run_bonus_eval(v_user, v_cr.day, true);
    select * into v_cr from economy.crew_runs c where c.user_id = v_user and c.day = v_cr.day;
    v_headcount := v_cr.bonus_pct;
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
        v_take := least(v_need, greatest(coalesce(v_pool_row.capacity, 0) - coalesce(v_pool_row.consumed, 0), 0));
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
    v_base := round(v_rewardable * economy.points_per_step() * (1 + v_eff / 10000.0) * v_xp * v_luck, 4);
    v_head_sup := round(v_base * v_headcount, 4);
    v_lvl_sup := round(v_base * v_level_bonus, 4);
    v_points := v_base + v_head_sup + v_lvl_sup;
  else
    -- 가득 구간은 다, 닳은 구간은 worn_rate 만큼(0119). worn_rate 가 0 이면 닳은 구간이 없고 예전 배율(내구도 0 이면 0)을 쓴다
    v_points := round(
      (v_full_steps + v_worn_steps * v_worn_rate) * economy.points_per_step()
        * (1 + v_eff / 10000.0)
        * case when v_worn_rate > 0 then 1 else economy.durability_factor(v_dur) end
        * economy.party_multiplier(v_party)
        * v_xp * v_luck,
      4
    );
  end if;
  v_points_full := v_points;
  if v_rewardable > 0 and v_worn_rate <= 0 and economy.durability_factor(v_dur) < 1 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳아 적립이 없습니다');
  end if;

  -- ── 하루 금액 상한 — 가입 7일 안은 절반 ──
  -- 아래 상한(하루 금액 · 경로 없는 러닝 · 올린 날)은 개인 러닝만 — 크루 러닝 보상은 따로이고 아직 상한이 없다
  -- (2026-10-21 사용자 결정 "일단 무제한"). 개인 상한의 합에서도 크루 러닝은 뺀다 (0070)
  if v_points > 0 and v_pool = 'PERSONAL' then
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
    crew_id, energy_pool, reward_steps
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    v_distance_m, p_steps * 0.04, v_track, least(v_eff, 5000), least(greatest(v_party, 1), 20),
    '', case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable,
    coalesce(p_mock_location, false), v_verified, v_energy_used, v_shoe.id,
    v_gps_backed, case when v_gps_backed and not v_held then v_verified else 0 end, v_credit_m, v_party_id,
    v_crew, v_pool, v_reward_steps
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
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발(출발 때 신은 신발 — 내 것일 때만)·에너지·크루 러닝·파티·부스터·보상 이벤트·러닝 아이템·상한 모두 서버 값으로. 판정 까닭도 돌려준다 (0089) · 운 · 상한 배수 (0116) · 보상 걸음 · 목표 보너스 (0118) · 닳은 뒤 적립 비율 · 효율 포인트의 닳음 / 에너지 (0119)';

revoke all on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint)
  from public, anon;
grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint)
  to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  7. 권한 — 서버 안의 셈은 앱이 부르지 못한다
-- ════════════════════════════════════════════════════════════════════
revoke all on function public.sneaker_points_set(bigint, integer, integer, integer, uuid) from public, anon;
grant execute on function public.sneaker_points_set(bigint, integer, integer, integer, uuid) to authenticated;
revoke all on function economy.eff_point_mult(public.market_sneakers) from public, anon, authenticated;
revoke all on function economy.dur_point_mult(public.market_sneakers) from public, anon, authenticated;
revoke all on function economy.wear_mult(public.market_sneakers) from public, anon, authenticated;
revoke all on function economy.energy_mult(public.market_sneakers) from public, anon, authenticated;
revoke all on function economy.worn_rate() from public, anon, authenticated;
revoke all on function economy.forge_curve_classic() from public, anon, authenticated;
revoke all on function economy.forge_level_factor(int) from public, anon, authenticated;
revoke all on function economy.points_day_net(public.market_sneakers, jsonb, numeric, int, int, int) from public, anon, authenticated;
revoke all on function economy.points_recommend(public.market_sneakers, jsonb, numeric, int, int, int, int, int) from public, anon, authenticated;
revoke all on function economy.sneaker_respec_on_milestone() from public, anon, authenticated;
revoke all on function public.sneaker_points_preview(bigint) from public, anon;
grant execute on function public.sneaker_points_preview(bigint) to authenticated;

comment on function public.sneaker_points_preview(bigint) is
  '포인트 미리보기 — 내 지난 14일 러닝으로 능력치마다 "1포인트 = 하루 약 +N SUP" · 남은 포인트 추천 · 다시 나누기 추천. 읽기만 (0119)';
