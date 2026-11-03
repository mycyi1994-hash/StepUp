-- ════════════════════════════════════════════════════════════════════
--  0087 — 크루 러닝 출발: 자리에 없는 준비 인원 빼기 · 대기실 열림 푸시 줄이기 (2026-11-03)
--
--  · 출발(party_start)할 때 준비를 눌러 두고 15초 넘게 대기실에 묻지 않은 사람(last_seen — 대기실은 2초마다 묻는다)은
--    준비를 풀고 출발 명단에서 뺀다. 화면이 꺼졌거나 앱을 떠난 사람이 출발한 것으로 적혀 오늘 크루 러닝을 잃던 것.
--    빠진 사람은 크루 러닝 줄(economy.crew_runs) · 공용 체력 기여가 없고 오늘을 그대로 쓴다. "2명 이상"도 남은 사람으로 센다.
--    진행자 자신은 지금 누르고 있으니 늘 자리에 있다고 본다. 나머지 규칙은 0072 그대로다.
--  · 인원 보너스(crew_run_bonus_eval · economy.party_for)는 출발 명단(crew_runs · party_runs)만 센다 — 빠진 사람은 저절로 안 센다.
--  · 크루 러닝 대기실이 열렸다는 푸시(PARTY_OPEN)는 크루마다 30분에 한 번만. 누르면 그 대기실(room/<크루 id>/<방 번호>).
-- ════════════════════════════════════════════════════════════════════

-- 크루마다 마지막으로 대기실 열림 푸시를 적은 시각 — 앱은 볼 수 없다
create table if not exists economy.crew_party_push (
  crew_id uuid primary key references public.crews on delete cascade,
  sent_at timestamptz not null default now()
);
alter table economy.crew_party_push enable row level security;
revoke all on economy.crew_party_push from public, anon, authenticated;

-- 크루 러닝 대기실이 열렸다 → 그 크루원들에게(연 사람 빼고) — 0017 과 같고 크루마다 30분에 한 번 · 링크는 그 대기실
create or replace function public.push_on_party()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_name text;
  v_member uuid;
  v_turn boolean;
begin
  if new.crew_id is null then
    return new;
  end if;
  -- 30분 안에 이미 알렸으면 이번 방은 알리지 않는다(방을 열고 닫기를 되풀이해도 한 번). 같은 줄을 잠가 동시에 열어도 하나만
  insert into economy.crew_party_push as x (crew_id, sent_at) values (new.crew_id, now())
  on conflict (crew_id) do update set sent_at = excluded.sent_at
   where x.sent_at <= now() - interval '30 minutes'
  returning true into v_turn;
  if v_turn is null then
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
      'room/' || new.crew_id::text || '/' || new.id::text);
  end loop;
  return new;
end;
$$;

-- ── 출발 — 0072 그대로 + 15초 넘게 묻지 않은 준비 인원은 준비를 풀고 뺀다 ───────────

create or replace function public.party_start(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_starts timestamptz;
  v_party public.parties;
  v_day date;
  v_level int;
  v_n int;
  v_added numeric;
  v_pool economy.crew_day_pools;
begin
  select * into v_party from public.parties where id = p_party and host_id = auth.uid() and status = 'LOBBY' for update;
  if not found then
    raise exception '로비에서 방장만 출발할 수 있습니다' using errcode = '42501';
  end if;
  -- 크루 방은 6초 뒤 — 대기실은 2초마다 묻는다. 늦게 안 사람도 3 · 2 · 1 · 0 · StepUP! 을 처음부터 본다(0072). 번개러닝은 예전 그대로 4초
  v_starts := now() + case when v_party.crew_id is not null then interval '6 seconds' else interval '4 seconds' end;
  if not exists (
    select 1 from public.party_members
     where party_id = p_party and user_id = auth.uid() and ready
  ) then
    raise exception '방장이 먼저 준비해야 합니다' using errcode = '23514';
  end if;

  -- 진행자는 지금 누르고 있다. 다른 사람은 15초 안에 대기실에 물어 온 사람만 함께 출발한다(0087)
  update public.party_members set last_seen = now() where party_id = p_party and user_id = auth.uid();
  update public.party_members set ready = false
   where party_id = p_party and ready and last_seen < now() - interval '15 seconds';

  delete from public.party_members where party_id = p_party and not ready;

  if v_party.crew_id is not null then
    v_day := economy.game_day(v_starts);
    v_level := greatest(coalesce((select c.level from public.crews c where c.id = v_party.crew_id), 1), 1);
    -- 공용 풀을 먼저 잠근다(모든 크루 러닝 함수가 같은 차례) — 같은 크루의 다른 방이 동시에 출발해도 기여가 섞이지 않게
    perform economy.crew_pool_settle(v_party.crew_id, v_day, now());
    delete from public.party_members m
     where m.party_id = p_party
       and (exists (select 1 from economy.crew_runs c where c.user_id = m.user_id and c.day = v_day)
            or not exists (select 1 from public.crew_members cm where cm.crew_id = v_party.crew_id and cm.user_id = m.user_id));
    select count(*)::int into v_n from public.party_members m where m.party_id = p_party;
    if v_n < 2 then
      raise exception 'crew_run_need_two' using errcode = '23514', detail = '크루 러닝은 준비한 크루원 2명 이상이 함께 출발합니다';
    end if;
    if not exists (select 1 from public.party_members m where m.party_id = p_party and m.user_id = auth.uid()) then
      raise exception 'crew_run_done_today' using errcode = '23514', detail = '오늘 크루 러닝을 이미 했습니다';
    end if;
  end if;

  update public.parties p
     set status = 'COUNTDOWN', starts_at = v_starts,
         crew_level = case when p.crew_id is not null then v_level end
   where p.id = p_party;

  insert into public.party_runs (party_id, user_id, starts_at, sneaker_id, shoe_dur_max)
  select p_party, m.user_id, v_starts, s.id, s.dur_max
    from public.party_members m
    left join lateral economy.party_shoe(m.user_id, m.sneaker_id) s on true
   where m.party_id = p_party
  on conflict do nothing;

  if v_party.crew_id is not null then
    -- 참여 · 신발 스냅샷 · 기여 = 최대 내구도 × (1 + 레벨 체력 보너스). 같은 날 다른 방에서 먼저 출발했으면(동시 출발) 이 방에서 뺀다
    with ins as (
      insert into economy.crew_runs (user_id, day, crew_id, party_id, started_at, state, last_beat_at,
                                     sneaker_id, shoe_dur_max, shoe_eff_bps, crew_level, contribution, level_pct,
                                     party_name, course_name, course_km, course_rev)
      select m.user_id, v_day, v_party.crew_id, p_party, v_starts, 'RUNNING', v_starts,
             s.id, coalesce(s.dur_max, 0), coalesce(e.efficiency_bps, 0), v_level,
             round(coalesce(s.dur_max, 0) * (1 + economy.crew_level_stamina(v_level)), 4), economy.crew_level_reward(v_level),
             v_party.name, v_party.course_name, v_party.course_km, v_party.course_rev
        from public.party_members m
        left join lateral economy.party_shoe(m.user_id, m.sneaker_id) s on true
        left join lateral economy.shoe_effective(s) e on s.id is not null
       where m.party_id = p_party
      on conflict (user_id, day) do nothing
      returning user_id, contribution)
    select count(*)::int, coalesce(sum(ins.contribution), 0) into v_n, v_added from ins;
    if v_n < 2 then
      raise exception 'crew_run_need_two' using errcode = '23514', detail = '크루 러닝은 준비한 크루원 2명 이상이 함께 출발합니다';
    end if;
    delete from public.party_runs r
     where r.party_id = p_party
       and not exists (select 1 from economy.crew_runs c where c.user_id = r.user_id and c.party_id = p_party);
    delete from public.party_members m
     where m.party_id = p_party
       and not exists (select 1 from economy.crew_runs c where c.user_id = m.user_id and c.party_id = p_party);
    update economy.crew_day_pools d
       set capacity = d.capacity + v_added, contributors = d.contributors + v_n, version = d.version + 1,
           exhausted_at = case when d.capacity + v_added > d.consumed then null else d.exhausted_at end,
           low_at = case when d.capacity + v_added - d.consumed > (d.capacity + v_added) * 0.2 then null else d.low_at end
     where d.crew_id = v_party.crew_id and d.day = v_day
    returning * into v_pool;
    -- 새 기여로 다시 생겼으면 0 이던 구간을 닫는다(그 뒤로는 다시 적립 — D06)
    if v_pool.capacity > v_pool.consumed then
      update economy.crew_pool_empty x set to_at = now()
       where x.crew_id = v_party.crew_id and x.day = v_day and x.to_at is null;
    end if;
  end if;
end;
$$;
