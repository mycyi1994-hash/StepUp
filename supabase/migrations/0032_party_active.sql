-- ════════════════════════════════════════════════════════════════════
--  0032 — 파티 보너스 인원은 실제로 뛴 사람만 센다 (2026-09-25 점검)
--
--  출발 명단(party_runs)의 인원을 그대로 세서, 준비만 누르고 출발 직후 앱을 닫은 사람도
--  보너스 인원에 들어갔다. 친구 계정 몇 개로 방을 채우고 혼자 뛰면 +10%씩 붙었다.
--
--  달리는 동안 앱은 몇 초마다 party_ping 을 보낸다. 그 마지막 시각을 명단에 적어 두고,
--  인원은 아래 중 하나인 사람만 센다.
--    · 적립을 받는 본인
--    · 출발하고 2분이 지나서도 위치를 보낸 사람
--    · 이 파티로 러닝 기록을 올린 사람
-- ════════════════════════════════════════════════════════════════════

alter table public.party_runs add column if not exists last_ping timestamptz;

create or replace function public.party_ping(p_party bigint, p_lat double precision, p_lng double precision)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.party_assert_member(p_party);
  if p_lat is not null and (p_lat not between -90 and 90 or p_lng not between -180 and 180) then
    raise exception '위치가 올바르지 않습니다' using errcode = '22023';
  end if;
  update public.party_members
     set lat = p_lat, lng = p_lng, last_seen = now()
   where party_id = p_party and user_id = auth.uid();
  -- 위치가 있는 보고만 "뛰는 중"으로 친다
  if p_lat is not null and p_lng is not null then
    update public.party_runs
       set last_ping = now()
     where party_id = p_party and user_id = auth.uid() and starts_at <= now();
  end if;
end;
$$;

create or replace function economy.party_for(p_user uuid, p_started_at timestamptz,
                                             out party_id bigint, out size int)
language sql stable security definer set search_path = public as $$
  with p as (
    select r.party_id from public.party_runs r
     where r.user_id = p_user
       and r.starts_at between p_started_at - interval '10 minutes'
                           and p_started_at + interval '10 minutes'
     order by abs(extract(epoch from (r.starts_at - p_started_at)))
     limit 1)
  select p.party_id,
         (select count(*)::int from public.party_runs r2
           where r2.party_id = p.party_id
             and (r2.user_id = p_user
                  or r2.last_ping >= r2.starts_at + interval '2 minutes'
                  or exists (select 1 from public.walk_sessions s
                              where s.user_id = r2.user_id and s.party_id = r2.party_id)))
    from p
$$;
revoke all on function economy.party_for(uuid, timestamptz) from public;
