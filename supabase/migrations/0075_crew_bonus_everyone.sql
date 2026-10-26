-- ════════════════════════════════════════════════════════════════════
--  0075 — 크루 러닝 인원 보너스: 휴대폰 인증 없이, 함께 출발한 크루원 모두 (2026-10-26)
--         사용자 "휴대폰 인증은 아예 빼버려 · 파생 효과도 빼" · "무조건 다 보너스 주도록 해. 테스트가 먼저라서"
--
--  - 휴대폰 인증은 어디서도 보지 않는다. phone_verified · crew_bonus_person 은 늘 참(옛 응답 칸 'verified' 는 늘 true),
--    설정 crew_bonus_requires_phone 은 false(규칙 응답 requires_phone 도 false).
--  - 인원 = 같은 대기실에서 같은 크루로 출발한 사람 모두(economy.crew_runs 의 같은 party_id). 거리(500m) · 원격 방 ·
--    2분 전에 끝냄 · 위치 없음으로 빼지 않는다. 판정 시각(출발 2분 뒤, 끝낼 때)은 그대로 — 그때까지 출발한 사람을 센다.
--  - 예전 규칙(2분 500m 무리)은 설정 crew_bonus_rule = 'NEAR' 로 되돌린다. 기본은 'ALL'.
--  - 보상 식(B + B × 인원 + B × 레벨) · 공용 체력 · 하루 한 번은 그대로다.
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values ('crew_bonus_rule', '"ALL"'::jsonb)
on conflict (key) do nothing;
update public.economy_settings set value = 'false'::jsonb where key = 'crew_bonus_requires_phone';

create or replace function economy.phone_verified(p_user uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  -- 2026-10-26 — 휴대폰 인증은 쓰지 않는다. 옛 응답 칸을 채우려고만 남긴다
  select true
$$;
revoke all on function economy.phone_verified(uuid) from public;

create or replace function economy.crew_bonus_person(p_user uuid) returns boolean
language sql stable security definer set search_path = public, economy as $$
  select true
$$;
revoke all on function economy.crew_bonus_person(uuid) from public;

create or replace function economy.crew_run_bonus_eval(p_user uuid, p_day date, p_final boolean default false)
returns void
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  r economy.crew_runs;
  v_check timestamptz;
  v_n int;
  v_located boolean;
begin
  select * into r from economy.crew_runs c where c.user_id = p_user and c.day = p_day for update;
  if not found or r.bonus_status <> 'PENDING' then
    return;
  end if;
  v_check := r.started_at + interval '2 minutes';
  if now() < v_check + interval '15 seconds' and not p_final then
    return;
  end if;

  if coalesce(economy.setting('crew_bonus_rule') #>> '{}', 'ALL') = 'ALL' then
    -- 함께 출발한 같은 크루원 모두(나 포함)
    select count(*)::int into v_n
      from economy.crew_runs c
      join public.crew_members m on m.crew_id = r.crew_id and m.user_id = c.user_id
     where c.party_id = r.party_id;
    update economy.crew_runs c
       set bonus_status = 'CONFIRMED', bonus_count = v_n, bonus_pct = economy.crew_headcount_bonus(v_n),
           bonus_reason = case when v_n < 2 then 'TOO_FEW' end
     where c.user_id = p_user and c.day = p_day;
    return;
  end if;

  -- 예전 규칙(NEAR) — 0074 와 같다
  if exists (select 1 from public.parties p where p.id = r.party_id and p.run_mode = 'REMOTE') then
    update economy.crew_runs c set bonus_status = 'UNAVAILABLE', bonus_reason = 'REMOTE', bonus_count = 0, bonus_pct = 0
     where c.user_id = p_user and c.day = p_day;
    return;
  end if;
  if (r.ended_at is not null and r.ended_at < v_check) or now() < v_check then
    update economy.crew_runs c set bonus_status = 'UNAVAILABLE', bonus_reason = 'ENDED_BEFORE_CHECK', bonus_count = 0, bonus_pct = 0
     where c.user_id = p_user and c.day = p_day;
    return;
  end if;
  v_located := exists (select 1 from public.party_members m
                        where m.party_id = r.party_id and m.user_id = p_user and m.pinged_at is not null)
            or exists (select 1 from public.party_near n
                        where n.party_id = r.party_id and (n.user_a = p_user or n.user_b = p_user));
  if not v_located then
    update economy.crew_runs c set bonus_status = 'UNAVAILABLE', bonus_reason = 'NO_LOCATION', bonus_count = 0, bonus_pct = 0
     where c.user_id = p_user and c.day = p_day;
    return;
  end if;
  with recursive e as (
    select n.user_a, n.user_b from public.party_near n
     where n.party_id = r.party_id
       and n.first_at <= r.started_at + interval '90 seconds'
       and n.last_at >= v_check),
  g(u) as (
    select p_user
    union
    select case when e.user_a = g.u then e.user_b else e.user_a end
      from e join g on g.u = e.user_a or g.u = e.user_b)
  select count(*)::int into v_n
    from g
    join economy.crew_runs c on c.user_id = g.u and c.party_id = r.party_id
    join public.crew_members m on m.crew_id = r.crew_id and m.user_id = g.u;
  update economy.crew_runs c
     set bonus_status = 'CONFIRMED', bonus_count = v_n, bonus_pct = economy.crew_headcount_bonus(v_n),
         bonus_reason = case when v_n < 2 then 'TOO_FEW' end
   where c.user_id = p_user and c.day = p_day;
end;
$$;
revoke all on function economy.crew_run_bonus_eval(uuid, date, boolean) from public;
