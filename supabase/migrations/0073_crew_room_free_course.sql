-- ════════════════════════════════════════════════════════════════════
--  0073 — 크루 러닝 대기실: 코스 없이 열기 · 코스 빼기
--         (2026-10-24 사용자 "코스를 내가 선택할 수가 없네 … 정해준 것만 해야 되던데 그건 아닌 것 같다")
--
--  0072 는 방마다 코스가 꼭 있어야 했다. 이 판은 코스를 고를 수 있는 것으로 바꾼다.
--
--  - 대기실은 코스 없이도 연다(자유 코스 — 각자 원하는 길로 함께 달린다). 코스 칸(이름 · 거리 · 경로)이 모두 비면 코스 없음,
--    하나라도 있으면 0072 그대로 모두 맞아야 한다(course_invalid). 코스 없이 연 방의 코스 판은 0 이다(옛 party_open 방과 같다).
--  - 진행자는 코스를 빼도 된다(party_set_course 에 모두 비워 보낸다) — 코스 판이 오르고 진행자를 뺀 준비가 풀린다(D14 그대로).
--    지금과 같은 코스를 다시 보내면(답을 잃고 다시 보냄 · 없는 코스를 또 뺌) 판도 준비도 그대로 두고 지금 판을 돌려준다.
--  - 출발 · 하루 한 번 · 2분 인원 · 보상은 그대로다(코스는 보상에 쓰이지 않는다). 코스 없는 출발은 참여 기록의 코스가 빈다.
--  - 폰에서 지도에 직접 그린 코스도 0072 의 코스 사본 그대로 받는다(이름 · 거리 · 경로 — 새 칸이 없다).
-- ════════════════════════════════════════════════════════════════════

-- 코스를 붙였는가 — 세 칸이 모두 비면 false(코스 없음), 하나라도 있으면 0072 의 검사를 모두 지나야 true. 아니면 course_invalid
create or replace function economy.party_course_given(p_name text, p_km numeric, p_track text) returns boolean
language plpgsql immutable set search_path = '' as $$
begin
  if btrim(coalesce(p_name, '')) = '' and p_km is null and btrim(coalesce(p_track, '')) = '' then
    return false;
  end if;
  perform economy.party_course_check(p_name, p_km, p_track);
  return true;
end;
$$;
revoke all on function economy.party_course_given(text, numeric, text) from public;

-- 새 대기실 — 0072 그대로 + 코스는 골라도 되고 안 골라도 된다(코스 없이 연 방은 코스 판 0)
create or replace function public.party_create(p_crew uuid, p_name text, p_course_name text, p_course_km numeric, p_course_track text,
                                               p_request uuid default null)
returns bigint
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_name text;
  v_course boolean;
  v_party bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_crew is null or not public.party_can_enter(p_crew, null) then
    raise exception '크루원만 대기실을 열 수 있습니다' using errcode = '42501';
  end if;
  -- 한 사람의 방 열기 · 들어가기는 하나씩(party_join 과 같은 자물쇠) — 동시에 눌러도 두 방에 남지 않게
  perform pg_advisory_xact_lock(hashtext('party_user:' || v_user::text));
  if p_request is not null then
    select id into v_party from public.parties
     where creator_id = v_user and create_key = p_request and status <> 'FINISHED';
    if found then
      return v_party;
    end if;
  end if;
  v_name := economy.party_room_name(p_name);
  v_course := economy.party_course_given(p_course_name, p_course_km, p_course_track);
  if exists (select 1 from economy.crew_runs c where c.user_id = v_user and c.day = economy.game_day(now())) then
    raise exception 'crew_run_done_today' using errcode = '23514', detail = '오늘 크루 러닝을 이미 했습니다';
  end if;
  perform economy.party_close_stale(p_crew);

  insert into public.parties (crew_id, host_id, creator_id, name, course_name, course_km, course_track, course_rev, create_key)
  values (p_crew, v_user, v_user, v_name,
          case when v_course then btrim(p_course_name) end,
          case when v_course then round(p_course_km, 3) end,
          case when v_course then p_course_track end,
          case when v_course then 1 else 0 end,
          p_request)
  on conflict (creator_id, create_key) where create_key is not null and status <> 'FINISHED' do nothing
  returning id into v_party;
  if v_party is null then
    -- 같은 요청이 동시에 두 번 왔다 — 먼저 연 방
    select id into v_party from public.parties
     where creator_id = v_user and create_key = p_request and status <> 'FINISHED';
    return v_party;
  end if;
  perform economy.party_leave_other_lobbies(v_user, v_party);
  insert into public.party_members (party_id, user_id) values (v_party, v_user);
  return v_party;
end;
$$;
revoke all on function public.party_create(uuid, text, text, numeric, text, uuid) from public, anon;
grant execute on function public.party_create(uuid, text, text, numeric, text, uuid) to authenticated;

-- 진행자가 코스를 바꾸거나 뺀다(출발 전만, 모두 비워 보내면 뺀다). 코스 판이 오르고, 진행자를 뺀 모두의 준비가 풀린다(D14).
-- 지금과 같은 코스면 그대로 둔다. 새(또는 지금) 판을 돌려준다
create or replace function public.party_set_course(p_party bigint, p_course_name text, p_course_km numeric, p_course_track text)
returns int
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_course boolean := economy.party_course_given(p_course_name, p_course_km, p_course_track);
  v_name text := case when v_course then btrim(p_course_name) end;
  v_km numeric := case when v_course then round(p_course_km, 3) end;
  v_track text := case when v_course then p_course_track end;
  v_p public.parties;
  v_rev int;
begin
  -- 방 줄을 잠근다(party_start_rev 와 같은 줄) — 판을 본 출발과 엇갈리지 않게
  select * into v_p from public.parties p
   where p.id = p_party and p.host_id = auth.uid() and p.status = 'LOBBY' and p.crew_id is not null
   for update;
  if not found then
    raise exception '대기실에서 진행자만 코스를 바꿀 수 있습니다' using errcode = '42501';
  end if;
  if (v_p.course_name, v_p.course_km, v_p.course_track) is not distinct from (v_name, v_km, v_track) then
    return v_p.course_rev;
  end if;
  update public.parties p
     set course_name = v_name, course_km = v_km, course_track = v_track, course_rev = p.course_rev + 1
   where p.id = p_party
  returning course_rev into v_rev;
  update public.party_members m set ready = false
   where m.party_id = p_party and m.user_id <> auth.uid() and m.ready;
  return v_rev;
end;
$$;
revoke all on function public.party_set_course(bigint, text, numeric, text) from public, anon;
grant execute on function public.party_set_course(bigint, text, numeric, text) to authenticated;
