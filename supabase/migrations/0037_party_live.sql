-- ════════════════════════════════════════════════════════════════════
--  0037 — 같이 뛰는 중 실시간 위치 · 거리 (S2 시안 14, 2026-09-26)
--
--  파티런 중 앱은 이미 party_ping 으로 위치를 보낸다(파티 인원 확인용, 0032). 지금까지
--  party_state 는 그 위치를 방 사람 모두에게 그대로 돌려줬다. 이제는
--    · 본인이 "달리는 동안 내 위치 보이기"를 켠 사람만(share_location),
--    · 방이 달리는 중(RUNNING)이고 최근 2분 안에 보낸 위치만
--  다른 사람에게 보인다. 끄면(기본) 위치와 거리 모두 본인에게만 보인다.
--  서버의 파티 인원 확인은 공유 여부와 상관없이 그대로 동작한다.
--  거리(km)는 앱이 보내는 화면용 값이다 — 적립 · 순위 계산에 쓰지 않는다.
-- ════════════════════════════════════════════════════════════════════

alter table public.party_members add column if not exists share_location boolean not null default false;
alter table public.party_members add column if not exists km numeric(8, 3);

/* 달리는 동안 내 위치 · 거리를 같이 뛰는 사람에게 보일지 */
create or replace function public.party_share(p_party bigint, p_share boolean)
returns void
language plpgsql security definer set search_path = public as $$
begin
  perform public.party_assert_member(p_party);
  update public.party_members
     set share_location = coalesce(p_share, false),
         -- 끄면 이미 보낸 거리도 다른 사람에게 남기지 않는다
         km = case when coalesce(p_share, false) then km else null end
   where party_id = p_party and user_id = auth.uid();
end $$;

/* 화면용 거리 보고 — 달리는 방에서만 받는다 */
create or replace function public.party_live(p_party bigint, p_km numeric)
returns void
language plpgsql security definer set search_path = public as $$
begin
  perform public.party_assert_member(p_party);
  if p_km is null or p_km < 0 or p_km > 300 then
    raise exception '거리가 올바르지 않습니다' using errcode = '22023';
  end if;
  update public.party_members m
     set km = round(p_km, 3)
    from public.parties p
   where m.party_id = p_party and m.user_id = auth.uid()
     and p.id = m.party_id and p.status = 'RUNNING';
end $$;

create or replace function public.party_state(p_party bigint)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
  v_state json;
begin
  perform public.party_assert_member(p_party);
  perform public.party_tick(p_party);

  update public.party_members set last_seen = now()
   where party_id = p_party and user_id = auth.uid();

  select json_build_object(
    'id', p.id,
    'status', p.status,
    'host_id', p.host_id,
    'crew_id', p.crew_id,
    'flash_post_id', p.flash_post_id,
    'starts_at', p.starts_at,
    'server_now', now(),
    'members', coalesce((
      select json_agg(json_build_object(
        'user_id', m.user_id,
        'name', coalesce(pr.display_name, '러너'),
        'ready', m.ready,
        'is_me', m.user_id = auth.uid(),
        'is_host', m.user_id = p.host_id,
        'share', m.share_location,
        'lat', case when v.visible then m.lat end,
        'lng', case when v.visible then m.lng end,
        'km', case when m.user_id = auth.uid() or (m.share_location and p.status = 'RUNNING') then m.km end,
        'seen_sec', extract(epoch from (now() - m.last_seen))::int
      ) order by (m.user_id = p.host_id) desc, m.joined_at)
        from public.party_members m
        left join public.profiles pr on pr.id = m.user_id
        cross join lateral (
          select m.user_id = auth.uid()
              or (m.share_location and p.status = 'RUNNING' and m.last_seen > now() - interval '2 minutes')
              as visible
        ) v
       where m.party_id = p.id
    ), '[]'::json)
  ) into v_state
  from public.parties p
  where p.id = p_party;

  return v_state;
end;
$$;

revoke all on function public.party_share(bigint, boolean) from public, anon;
revoke all on function public.party_live(bigint, numeric) from public, anon;
grant execute on function public.party_share(bigint, boolean) to authenticated;
grant execute on function public.party_live(bigint, numeric) to authenticated;
grant execute on function public.party_state(bigint) to authenticated;
