-- ════════════════════════════════════════════════════════════════════
--  0109 — 크루 · 번개 러닝 버그 고침 (2026-11-17, 점검 문서 docs/점검-2026-11-17-버그.md #2 · #3 · #5 · #9 · #13)
--
--  #2  party_start — 자리에 없는(15초 넘게 대기실에 묻지 않은) 준비 인원을 빼면 2명이 안 되는 크루 방은 아무것도 바꾸기 전에
--      거절한다(crew_run_need_two, 23514 — detail 에 자리에 없는 사람 이름). 예전에는 준비를 풀고 · 거절하며 그것까지 되돌려
--      진행자 화면에 "준비 완료"가 그대로 남고 출발을 눌러도 같은 거절만 되풀이됐다. 나머지는 0087 그대로.
--  #3  economy.party_stale — 출발 시각(plan_at)을 정한 대기실은 그 시각 30분 뒤까지 "아무도 안 봄(10분)"으로 닫지 않고,
--      6시간 상한도 정한 시각부터 센다(출발한 방은 출발 시각부터). party_close_stale(크루 목록 · 방 열기)과
--      party_open(번개 · 예전 앱 — 모든 방을 함께 닫는다)이 같은 기준을 쓴다.
--  #5  crew_run_release(p_party) — 출발을 놓친 사람(달린 소식 · 올린 러닝이 없다)이 그 방의 오늘 크루 러닝을 푼다:
--      참여 줄 · 출발 명단을 지우고 공용 체력에서 기여를 뺀다(이미 다른 사람이 쓴 양 아래로는 빼지 않는다).
--      앱은 "출발을 놓쳤어요"에서 부르고, 풀렸을 때만 "오늘의 크루 러닝은 쓰이지 않았어요"라고 말한다.
--  #9  post_delete — 출발한 번개(하루 안)의 글은 지우지 않는다(22023) — 글을 지우면 방 · 출발 명단까지 지워져
--      달리던 사람의 인원 보너스가 없어졌다.
--  #13 economy.party_for(p_user, p_started_at, p_ended_at) — 방이 출발하기 전에 끝난 러닝(1분 여유 — 폰 시계)은 그 방의
--      러닝이 아니다. record_session 이 끝난 시각을 넘긴다(0106 본문 그대로, 그 한 줄만). 예전 두 칸 호출은 그대로 된다.
-- ════════════════════════════════════════════════════════════════════

-- ── #3 오래된 방 ─────────────────────────────────────────────────────

-- 이 방을 닫을까 — 대기실(LOBBY): 정한 출발 시각(plan_at)이 30분 넘게 지나기 전에는 "아무도 안 봄"으로 닫지 않는다.
-- 6시간 상한은 만든 때와 정한 시각 중 늦은 것부터. 출발한 방: 출발 시각(없으면 만든 때)부터 6시간 · 10분 아무도 안 봄
create or replace function economy.party_stale(p public.parties) returns boolean
language sql stable security definer set search_path = public, economy as $$
  select p.status <> 'FINISHED'
     and (case when p.status = 'LOBBY' then greatest(p.created_at, p.plan_at)
               else coalesce(p.starts_at, p.created_at) end < now() - interval '6 hours'
          or ((p.status <> 'LOBBY' or p.plan_at is null or p.plan_at < now() - interval '30 minutes')
              and not exists (
                select 1 from public.party_members m
                 where m.party_id = p.id and m.last_seen > now() - interval '10 minutes')))
$$;
revoke all on function economy.party_stale(public.parties) from public;

-- 0072 그대로 + 기준은 party_stale
create or replace function economy.party_close_stale(p_crew uuid) returns void
language sql security definer set search_path = public, economy as $$
  with closed as (
    update public.parties p set status = 'FINISHED'
     where p.crew_id = p_crew
       and economy.party_stale(p)
    returning p.id)
  update public.party_members m set lat = null, lng = null, km = null
    from closed
   where m.party_id = closed.id
$$;
revoke all on function economy.party_close_stale(uuid) from public;

-- 로비 열기 — 0070 그대로 + 닫는 기준은 party_stale(정한 출발 시각이 남은 대기실을 닫지 않는다)
create or replace function public.party_open(p_crew uuid, p_post bigint)
returns bigint
language plpgsql
security definer
set search_path = public, economy
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

  -- 아무도 안 보는 방과 오래된 방은 닫는다. 닫는 방에 남은 위치 · 거리는 지운다.
  with closed as (
    update public.parties p set status = 'FINISHED'
     where economy.party_stale(p)
    returning p.id)
  update public.party_members m set lat = null, lng = null, km = null
    from closed
   where m.party_id = closed.id;

  -- 이 크루(번개)의 열린 방. 두 사람이 동시에 열어도 방이 둘 생기지 않게 잠근다.
  perform pg_advisory_xact_lock(hashtext(coalesce(p_crew::text, 'post:' || p_post)));

  -- 이미 들어가 있는 방이 먼저 — 꽉 차 새 방이 열린 뒤에도 앞 방 사람은 제 방으로 돌아간다
  select p.id, p.status into v_party, v_status
    from public.parties p
   where p.status <> 'FINISHED'
     and ((p_crew is not null and p.crew_id = p_crew)
          or (p_post is not null and p.flash_post_id = p_post))
   order by exists (select 1 from public.party_members m where m.party_id = p.id and m.user_id = v_user) desc,
            p.created_at desc
   limit 1;

  -- 이미 출발한 방에 늦게 온 사람은 끼워 넣지 않는다. 대신 새 방을 연다.
  if v_party is not null and v_status <> 'LOBBY' and not exists (
    select 1 from public.party_members m where m.party_id = v_party and m.user_id = v_user
  ) then
    v_party := null;
  end if;

  -- 크루 대기실은 20명까지 — 꽉 찬 방에 새로 오는 사람은 새 방을 연다
  if v_party is not null and p_crew is not null and not exists (
    select 1 from public.party_members m where m.party_id = v_party and m.user_id = v_user
  ) and (select count(*) from public.party_members m where m.party_id = v_party) >= economy.crew_room_max() then
    v_party := null;
  end if;

  -- 진행자가 내보낸 방에는 다시 들어가지 않는다
  if v_party is not null and exists (
    select 1 from public.party_kicks k where k.party_id = v_party and k.user_id = v_user
  ) then
    raise exception '진행자가 내보낸 방입니다' using errcode = '42501';
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

-- ── #2 출발 — 자리에 없는 사람을 빼면 2명이 안 되면 아무것도 바꾸지 않고 거절 ─────────

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
  v_away text;
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

  -- 크루 방은 함께 출발할 사람(준비 · 15초 안에 대기실에 물음 · 오늘 아직 안 함 · 크루원)이 2명이 안 되면 바로 거절한다(0109).
  -- 아래에서 준비를 풀고 뺀 뒤 거절하면 그것까지 되돌아가 진행자 화면에 "준비 완료"가 남았다. 자리에 없는 사람 이름을 알린다
  if v_party.crew_id is not null then
    v_day := economy.game_day(v_starts);
    select count(*)::int into v_n
      from public.party_members m
     where m.party_id = p_party and m.ready
       and (m.user_id = auth.uid() or m.last_seen >= now() - interval '15 seconds')
       and not exists (select 1 from economy.crew_runs c where c.user_id = m.user_id and c.day = v_day)
       and exists (select 1 from public.crew_members cm where cm.crew_id = v_party.crew_id and cm.user_id = m.user_id);
    if v_n < 2 then
      select string_agg(coalesce(pr.display_name, '러너'), ', ' order by m.joined_at) into v_away
        from public.party_members m
        left join public.profiles pr on pr.id = m.user_id
       where m.party_id = p_party and m.ready and m.user_id <> auth.uid()
         and m.last_seen < now() - interval '15 seconds';
      raise exception 'crew_run_need_two' using errcode = '23514',
        detail = case when v_away is not null then '대기실을 보고 있지 않은 크루원: ' || v_away
                      else '크루 러닝은 준비한 크루원 2명 이상이 함께 출발합니다' end;
    end if;
  end if;

  -- 진행자는 지금 누르고 있다. 다른 사람은 15초 안에 대기실에 물어 온 사람만 함께 출발한다(0087)
  update public.party_members set last_seen = now() where party_id = p_party and user_id = auth.uid();
  update public.party_members set ready = false
   where party_id = p_party and ready and last_seen < now() - interval '15 seconds';

  delete from public.party_members where party_id = p_party and not ready;

  if v_party.crew_id is not null then
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

-- ── #5 출발을 놓친 크루 러닝 풀기 ─────────────────────────────────────

-- 출발을 놓친 사람이 그 방의 오늘 크루 러닝을 푼다 — 달린 소식(crew_run_beat)도, 올린 러닝도, 스스로 끝냄도 없을 때만.
-- 참여 줄(하루 한 번) · 출발 명단을 지우고, 공용 체력에서 기여 · 쓴 양(출발 직후 30초 동안 달리는 사람으로 세어진 몫)을 뺀다.
-- 총량은 남은 다른 사람이 이미 쓴 양 아래로 내리지 않는다. 다른 사람의 인원 보너스가 판정 시각(2분 15초)을 지났으면
-- 나를 센 채로 먼저 정한다(뒤늦게 줄지 않게). 잠금 차례는 출발과 같다(방 → 공용 체력 → 사람).
-- @return 오늘 크루 러닝이 쓰이지 않았다(풀었거나 처음부터 없었다) · false 면 달린 기록이 있어 그대로 둔다
create or replace function public.crew_run_release(p_party bigint)
returns boolean
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  r economy.crew_runs;
  v_other uuid;
  v_pool economy.crew_day_pools;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform 1 from public.parties p where p.id = p_party for update;
  select * into r from economy.crew_runs c where c.user_id = v_user and c.party_id = p_party;
  if not found then
    return true;
  end if;
  if r.crew_id is not null then
    perform economy.crew_pool_settle(r.crew_id, r.day, now());
  end if;
  -- 공용 체력을 잠근 뒤 다시 본다 — 그사이 닿은 박동 · 올린 러닝이 있으면 그대로 둔다
  select * into r from economy.crew_runs c where c.user_id = v_user and c.day = r.day and c.party_id = p_party for update;
  if not found then
    return true;
  end if;
  if r.session_id is not null
     or r.last_beat_at is distinct from r.started_at
     or r.terminal_reason = 'USER_END'
     or exists (select 1 from public.walk_sessions s where s.user_id = v_user and s.party_id = p_party) then
    return false;
  end if;

  if r.crew_id is not null then
    if now() >= r.started_at + interval '2 minutes 15 seconds' then
      for v_other in
        select c.user_id from economy.crew_runs c
         where c.party_id = p_party and c.user_id <> v_user and c.bonus_status = 'PENDING'
         order by c.user_id
      loop
        perform economy.crew_run_bonus_eval(v_other, r.day, true);
      end loop;
    end if;
    update economy.crew_day_pools d
       set consumed = greatest(d.consumed - r.consumed, 0),
           capacity = greatest(d.capacity - r.contribution, greatest(d.consumed - r.consumed, 0)),
           contributors = greatest(d.contributors - 1, 0),
           version = d.version + 1
     where d.crew_id = r.crew_id and d.day = r.day
    returning * into v_pool;
    if found then
      if v_pool.capacity > v_pool.consumed then
        -- 남아 있다 — 0 이던 구간이 열려 있었으면 닫는다(출발과 같다)
        update economy.crew_day_pools d set exhausted_at = null where d.crew_id = r.crew_id and d.day = r.day;
        update economy.crew_pool_empty x set to_at = now()
         where x.crew_id = r.crew_id and x.day = r.day and x.to_at is null;
      elsif v_pool.capacity > 0 then
        -- 내 기여를 빼니 남은 양이 0 — 0 인 구간을 연다(그 뒤로 달린 걸음은 적립하지 않는다, 정산과 같다)
        update economy.crew_day_pools d set exhausted_at = coalesce(d.exhausted_at, now()) where d.crew_id = r.crew_id and d.day = r.day;
        insert into economy.crew_pool_empty (crew_id, day, from_at)
        select r.crew_id, r.day, now()
         where not exists (select 1 from economy.crew_pool_empty x
                            where x.crew_id = r.crew_id and x.day = r.day and x.to_at is null)
        on conflict do nothing;
      end if;
    end if;
  end if;

  delete from economy.crew_runs c where c.user_id = v_user and c.day = r.day and c.party_id = p_party;
  delete from public.party_runs x where x.party_id = p_party and x.user_id = v_user;
  return true;
end;
$$;
revoke all on function public.crew_run_release(bigint) from public, anon;
grant execute on function public.crew_run_release(bigint) to authenticated;

-- ── #9 출발한 번개는 지우지 않는다 ────────────────────────────────────

-- 0062 그대로 + 이 글의 번개 대기실이 하루 안에 출발했으면 거절(22023). 방 줄을 먼저 잠근다 — 지우는 사이 출발하지 않게
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
  if exists (select 1 from public.posts p where p.id = p_post and p.author_id = auth.uid()) then
    perform 1 from public.parties x where x.flash_post_id = p_post order by x.id for update;
    if exists (select 1 from public.parties x
                where x.flash_post_id = p_post and x.starts_at is not null and x.starts_at > now() - interval '1 day') then
      raise exception '이미 출발한 번개러닝은 취소할 수 없어요' using errcode = '22023', detail = 'flash_started';
    end if;
  end if;
  delete from public.posts where id = p_post and author_id = auth.uid();
  if not found and exists (select 1 from public.posts where id = p_post) then
    raise exception '내가 쓴 글만 지울 수 있습니다' using errcode = '42501';
  end if;
end;
$$;

-- ── #13 방이 출발하기 전에 끝난 러닝은 그 방의 러닝이 아니다 ───────────────

-- 0076 그대로 + p_ended_at(null 이면 보지 않는다) — 방의 출발 시각이 러닝이 끝난 때(1분 여유 — 폰 시계)보다 앞서야 한다.
-- 크루 출발 10분 안에 끝낸 혼자 러닝이 출발 뒤에 올라가 그날의 크루 러닝(참여 줄)을 가져가던 것
create or replace function economy.party_for(p_user uuid, p_started_at timestamptz, p_ended_at timestamptz,
                                             out party_id bigint, out size int)
language sql stable security definer set search_path = public as $$
  with recursive p as (
    select r.party_id, r.starts_at from public.party_runs r
     where r.user_id = p_user
       and r.starts_at between p_started_at - interval '10 minutes'
                           and p_started_at + interval '10 minutes'
       and (p_ended_at is null or r.starts_at < p_ended_at + interval '1 minute')
     order by abs(extract(epoch from (r.starts_at - p_started_at)))
     limit 1),
  e as (
    select n.user_a, n.user_b
      from public.party_near n join p on n.party_id = p.party_id
     where n.last_at >= p.starts_at + interval '2 minutes'),
  g(u) as (
    select p_user
    union
    select case when e.user_a = g.u then e.user_b else e.user_a end
      from e join g on g.u = e.user_a or g.u = e.user_b)
  select p.party_id,
         case when coalesce(economy.setting('party_bonus_rule') #>> '{}', 'ALL') = 'ALL'
              then greatest((select count(*)::int from public.party_runs r where r.party_id = p.party_id), 1)
              else (select count(*)::int from g) end
    from p
$$;
revoke all on function economy.party_for(uuid, timestamptz, timestamptz) from public;

-- 예전 두 칸 — 끝난 시각 없이(예전 record_session 본문 · 다시 붙여넣는 사이)
create or replace function economy.party_for(p_user uuid, p_started_at timestamptz,
                                             out party_id bigint, out size int)
language sql stable security definer set search_path = public as $$
  select f.party_id, f.size from economy.party_for(p_user, p_started_at, null::timestamptz) f
$$;
revoke all on function economy.party_for(uuid, timestamptz) from public;

-- ════════════════════════════════════════════════════════════════════
--  record_session — 0106 본문 그대로, 한 곳만: economy.party_for 에 끝난 시각(p_ended_at)을 넘긴다(#13).
--  칸 · 돌려주는 값이 같아 create or replace 로 바꾼다. 뒤 파일에서 record_session 을 다시 쓰면 이 한 줄을 함께 옮긴다.
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
  verdict_reason text
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
  -- 서버가 이미 끝낸 참여(마지막 한 사람 자동 종료 · 끝냄)면 그때 + 60초(폰 시계 차이) 뒤의 기록은 크루 보상 · 경험치에서 뺀다
  v_cut timestamptz;
  v_xp_frac numeric := 1;
  v_level_now int;
  v_level_was int;
  v_starters int := 1;
  v_xp_m numeric := 0;
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
  select s.id, s.verdict, s.points_awarded, coalesce(s.verdict_reason, '') into v_session_id, v_verdict, v_points, v_reason
    from public.walk_sessions s
   where s.user_id = v_user and s.started_at = p_started_at;
  if found then
    return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user), v_reason;
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

  -- 과속이 30초 넘게 이어져 앱이 멈춘 러닝 — 적립 규칙은 그대로, 판정에 까닭을 남긴다 (0065)
  if v_verdict <> 'VOID' and exists (
        select 1 from public.run_penalties x
         where x.user_id = v_user and x.run_started_at = p_started_at) then
    v_verdict := 'FLAGGED';
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '과속이 이어져 러닝이 멈췄습니다');
  end if;

  -- ── 함께 달리기 · 크루 러닝 (0070 — 걸음 상한 · 신발보다 먼저 정한다) ──
  -- 파티 보너스는 한 파티에서 한 러닝만
  select pf.party_id, pf.size into v_party_id, v_party from economy.party_for(v_user, p_started_at, p_ended_at) pf;
  if v_party_id is null or exists (select 1 from public.walk_sessions s
                                    where s.user_id = v_user and s.party_id = v_party_id) then
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
    -- 내구도가 다 닳은 뒤의 걸음은 적립하지 않는다 (0066) — 남은 내구도로 달릴 수 있던 시간(1분마다 5)만큼만.
    -- 적립 배율은 남은 비율이 0 보다 크면 그대로라(economy.durability_factor) 거의 닳은 신발로 오래 달려도 다 받던 것을 막는다
    if v_rewardable > 0 and v_shoe.id is not null
       and v_shoe.dur_now * economy.durability_sec_per_point() < v_elapsed then
      v_rewardable := floor(v_rewardable * v_shoe.dur_now * economy.durability_sec_per_point()
                            / greatest(v_elapsed, 1)::numeric)::int;
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳은 뒤의 걸음은 적립하지 않습니다');
    end if;

    -- 개인 에너지 — 적립 걸음 600 에 1, 착화감만큼 덜
    if v_rewardable > 0 then
      -- 에너지 드링크를 고른 러닝은 그 비율만큼만 쓴다 (0106)
      v_energy_per_step := (1 - v_comfort / 10000.0) / economy.steps_per_energy()
                           * economy.run_energy_factor(v_user, p_started_at);
      v_energy_left := economy.energy_left(v_user, v_gday);
      if v_rewardable * v_energy_per_step > v_energy_left then
        v_rewardable := greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '에너지를 다 썼습니다');
      end if;
      v_energy_used := round(v_rewardable * v_energy_per_step, 4);
    end if;
  end if;

  -- 적립 배수 — 24시간 부스터 · 보상 이벤트 · 이 러닝에 고른 아이템 가운데 가장 큰 것 하나(겹치지 않는다, 0106)
  v_xp := economy.run_reward_mult(v_user, p_started_at);

  if v_pool = 'CREW' then
    -- 기본 B(고른 신발) + B × 인원 보너스(구간 하나) + B × 레벨 보너스 — 더하기, 각 몫을 4자리로 정해 합이 원장과 같게 (R08 · R10)
    v_base := round(v_rewardable * economy.points_per_step() * (1 + v_eff / 10000.0) * v_xp, 4);
    v_head_sup := round(v_base * v_headcount, 4);
    v_lvl_sup := round(v_base * v_level_bonus, 4);
    v_points := v_base + v_head_sup + v_lvl_sup;
  else
    v_points := round(
      v_rewardable * economy.points_per_step()
        * (1 + v_eff / 10000.0)
        * economy.durability_factor(v_dur)
        * economy.party_multiplier(v_party)
        * v_xp,
      4
    );
  end if;
  v_points_full := v_points;
  if v_rewardable > 0 and economy.durability_factor(v_dur) < 1 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳아 적립이 없습니다');
  end if;

  -- ── 하루 금액 상한 — 가입 7일 안은 절반 ──
  -- 아래 상한(하루 금액 · 경로 없는 러닝 · 올린 날)은 개인 러닝만 — 크루 러닝 보상은 따로이고 아직 상한이 없다
  -- (2026-10-21 사용자 결정 "일단 무제한"). 개인 상한의 합에서도 크루 러닝은 뺀다 (0070)
  if v_points > 0 and v_pool = 'PERSONAL' then
    v_new_account := (select p.created_at from public.profiles p where p.id = v_user)
                     > now() - make_interval(days => economy.setting_num('new_account_days')::int);
    v_cap := economy.setting_num('daily_earn_cap') * case when v_new_account then 0.5 else 1 end;
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
  if v_points > 0 and not v_gps_backed and v_pool = 'PERSONAL' then
    select economy.setting_num('no_gps_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and not s.gps_backed and s.energy_pool <> 'CREW'
       and economy.game_day(s.started_at) = v_gday;
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '경로 없는 러닝의 하루 상한에 걸렸습니다');
    end if;
  end if;
  if v_points > 0 and v_pool = 'PERSONAL' then
    select economy.setting_num('upload_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and s.energy_pool <> 'CREW'
       and economy.game_day(s.created_at) = economy.game_day(now());
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루에 올릴 수 있는 적립 상한에 걸렸습니다');
    end if;
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

  -- ── 기록 ──
  insert into public.walk_sessions (
    user_id, started_at, ended_at, duration_sec, steps,
    distance_meters, calories, track, boost_bps, party_size,
    faction, top_speed_kmh, gps_distance_m,
    verdict, verdict_reason, points_awarded, rewarded_steps,
    mock_location, verified_steps, energy_used, sneaker_id,
    gps_backed, backed_steps, gps_credit_m, party_id,
    crew_id, energy_pool
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    v_distance_m, p_steps * 0.04, v_track, least(v_eff, 5000), least(greatest(v_party, 1), 20),
    '', case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable,
    coalesce(p_mock_location, false), v_verified, v_energy_used, v_shoe.id,
    v_gps_backed, case when v_gps_backed and not v_held then v_verified else 0 end, v_credit_m, v_party_id,
    v_crew, v_pool
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
  if v_shoe.id is not null and v_verdict <> 'VOID' then
    update public.market_sneakers s
       set km_run = s.km_run + round((v_credit_m / 1000)::numeric, 3),
           dur_now = case when v_pool = 'CREW' then s.dur_now
                          else greatest(s.dur_now - economy.durability_wear_time(v_elapsed), 0) end,
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

  return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user), v_reason;
end;
$$;


comment on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint) is
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발(출발 때 신은 신발 — 내 것일 때만)·에너지·크루 러닝·파티·부스터·보상 이벤트·러닝 아이템·상한 모두 서버 값으로. 판정 까닭도 돌려준다 (0089)';

revoke all on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint)
  from public, anon;
grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint)
  to authenticated;
