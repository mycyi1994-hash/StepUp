-- ════════════════════════════════════════════════════════════════════
--  0063 — 대기실 진행자가 연결이 끊긴 채 사라지면 남은 사람에게 넘긴다 (2026-10-17 인터넷 끊김 점검)
--
--  진행자는 나가기(party_leave)로만 바뀌었다. 연결이 불안정할 때 진행자가 대기실을 나가면 나가기가 서버에 닿지 않아
--  그 사람이 진행자로 남았고, 뒤에 들어온 사람들은 "출발 기다리는 중"에서 출발할 수 없었다(앱이 죽어도 같다).
--  이제 대기실(LOBBY)에서 진행자가 30초 넘게 보이지 않으면, 상태를 보는 사람 중 지금 보이는 사람 가운데 먼저 들어온
--  사람이 진행자를 맡는다. 앱은 대기실에서 몇 초마다 상태를 보므로, 화면에 있는 진행자는 넘어가지 않는다.
--  출발한 방(COUNTDOWN · RUNNING)은 그대로다.
-- ════════════════════════════════════════════════════════════════════

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

  -- 대기실의 진행자가 30초 넘게 보이지 않는다 — 지금 보이는 사람 중 먼저 들어온 사람에게 넘긴다(0063)
  update public.parties p
     set host_id = (
       select m.user_id from public.party_members m
        where m.party_id = p.id and m.last_seen > now() - interval '30 seconds'
        order by m.joined_at, m.user_id
        limit 1)
   where p.id = p_party
     and p.status = 'LOBBY'
     and not exists (
       select 1 from public.party_members h
        where h.party_id = p.id and h.user_id = p.host_id and h.last_seen > now() - interval '30 seconds');

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
        'seen_sec', extract(epoch from (now() - m.last_seen))::int,
        'ping_sec', case when v.visible and m.pinged_at is not null
                         then extract(epoch from (now() - m.pinged_at))::int end
      ) order by (m.user_id = p.host_id) desc, m.joined_at)
        from public.party_members m
        left join public.profiles pr on pr.id = m.user_id
        cross join lateral (
          select m.user_id = auth.uid()
              or (m.share_location and p.status = 'RUNNING' and m.pinged_at > now() - interval '2 minutes')
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
