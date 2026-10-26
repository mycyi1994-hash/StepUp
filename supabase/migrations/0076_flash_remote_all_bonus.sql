-- ════════════════════════════════════════════════════════════════════
--  0076 — 같이 뛰기 한 탭: 번개러닝도 원격 뛰기 · 함께 출발한 사람 모두 보너스 (2026-10-26)
--         사용자 "크루달리기 · 번개달리기를 같이 뛰기 한 탭에서" · "번개도 원격 허용" · "무조건 다 보너스(테스트가 먼저)"
--
--  - 번개 글(posts.category = 'FLASH')에 달리는 방식(run_mode)을 둔다. 같이 뛰기(TOGETHER)는 지금과 같고,
--    원격 뛰기(REMOTE)는 만나는 곳 · 좌표 없이 같은 시각에 각자의 자리에서 출발한다. 글쓰기는 post_create 의 p_run_mode
--    (기본 TOGETHER — 옛 앱은 그대로). 글 목록(post_feed)에 run_mode 를 더한다.
--  - 번개 대기실(parties.flash_post_id)은 열릴 때 글의 방식 · 모임 시각을 받는다(0074 의 대기실 칸 — 크루 방과 같은 모양).
--  - 함께 달리기 보너스 인원(economy.party_for — 번개 · 옛 크루 방)은 이 방에서 함께 출발한 사람 모두(출발 명단 party_runs)를
--    센다. 거리(200m) · 2분 · 원격 상관없이. 예전 규칙(곁에서 같이 뛴 무리)은 설정 party_bonus_rule = 'NEAR'.
--    한 방에 한 번 · +10%/명 · 인원 상한 · 하루 상한은 그대로다(크루 러닝은 0075 의 crew_run_bonus_eval 그대로).
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values ('party_bonus_rule', '"ALL"'::jsonb)
on conflict (key) do nothing;

-- ── 번개 글의 달리는 방식 ────────────────────────────────────────────
alter table public.posts add column if not exists run_mode text not null default 'TOGETHER';
do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'posts_run_mode_sane') then
    alter table public.posts add constraint posts_run_mode_sane check (
      run_mode in ('TOGETHER', 'REMOTE') and (run_mode = 'TOGETHER' or category = 'FLASH'));
  end if;
end $$;
comment on column public.posts.run_mode is
  '번개러닝의 달리는 방식 — TOGETHER 같은 장소에 모여 출발, REMOTE 같은 시각에 각자의 자리에서(장소 · 좌표 없음). 0076';

drop function if exists public.post_create(
  text, uuid, text, text, text, double precision, timestamptz, int, double precision, double precision, uuid
);

-- 0062 그대로 + p_run_mode(번개만 — 원격이면 장소 · 좌표를 비운다)
create or replace function public.post_create(
  p_category text,
  p_crew uuid,
  p_title text,
  p_body text,
  p_place text,
  p_distance_km double precision,
  p_meet_at timestamptz,
  p_capacity int,
  p_lat double precision,
  p_lng double precision,
  p_client_key uuid default null,
  p_run_mode text default null
)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_id bigint;
  v_flash boolean := p_category = 'FLASH';
  v_mode text := upper(btrim(coalesce(p_run_mode, 'TOGETHER')));
  v_remote boolean;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  -- 같은 요청이 다시 왔다(응답을 받지 못하고 다시 올리기) — 앞서 만든 글 번호를 돌려준다(번개 모임이 둘이 되지 않게)
  if p_client_key is not null then
    select p.id into v_id from public.posts p where p.author_id = v_user and p.client_key = p_client_key;
    if v_id is not null then
      return v_id;
    end if;
  end if;
  if p_category not in ('FLASH', 'FREE', 'TIP') then
    raise exception '게시판 종류가 올바르지 않습니다' using errcode = '22023';
  end if;
  if v_mode not in ('TOGETHER', 'REMOTE') then
    raise exception 'room_options_invalid' using errcode = '22023', detail = '달리는 방식을 확인하세요';
  end if;
  v_remote := v_flash and v_mode = 'REMOTE';
  if not public.is_crew_member(p_crew) then
    raise exception '이 크루의 멤버가 아닙니다' using errcode = '42501';
  end if;
  if (select count(*) from public.posts p
       where p.author_id = v_user and p.created_at > now() - interval '1 hour') >= 20 then
    raise exception '글을 너무 자주 쓰고 있습니다. 잠시 뒤에 다시 써 주세요' using errcode = '23514';
  end if;
  if v_flash and (p_meet_at is null or p_meet_at < now()) then
    raise exception '번개러닝은 앞으로의 모임 시각이 있어야 합니다' using errcode = '23514';
  end if;

  if not v_remote and p_lat is not null and (p_lat not between -90 and 90 or p_lng is null
                                             or p_lng not between -180 and 180) then
    raise exception '모임 장소 좌표가 올바르지 않습니다' using errcode = '22023';
  end if;

  insert into public.posts (
    author_id, category, crew_id, title, body,
    place, distance_km, meet_at, capacity, lat, lng, client_key, run_mode
  )
  values (
    v_user,
    p_category,
    p_crew,
    btrim(p_title),
    btrim(coalesce(p_body, '')),
    case when v_flash and not v_remote then btrim(coalesce(p_place, '')) else '' end,
    case when v_flash then greatest(coalesce(p_distance_km, 0), 0) else 0 end,
    case when v_flash then p_meet_at else null end,
    -- 번개는 둘 이상이 모여야 번개다. 정원 1은 혼자 뛰는 것과 같다.
    case when v_flash then least(greatest(coalesce(p_capacity, 2), 2), 200) else 0 end,
    case when v_flash and not v_remote then p_lat else null end,
    case when v_flash and not v_remote then p_lng else null end,
    p_client_key,
    case when v_remote then 'REMOTE' else 'TOGETHER' end
  )
  on conflict (author_id, client_key) where client_key is not null do nothing
  returning id into v_id;

  -- 같은 키의 요청이 동시에 들어와 다른 쪽이 먼저 만들었다 — 그 글 번호를 돌려준다
  if v_id is null then
    select p.id into v_id from public.posts p where p.author_id = v_user and p.client_key = p_client_key;
    return v_id;
  end if;

  -- 번개를 연 사람은 참가자 첫 줄이다.
  if v_flash then
    insert into public.flash_participants (post_id, user_id) values (v_id, v_user);
  end if;

  return v_id;
end;
$$;

revoke all on function public.post_create(
  text, uuid, text, text, text, double precision, timestamptz, int, double precision, double precision, uuid, text
) from public, anon;
grant execute on function public.post_create(
  text, uuid, text, text, text, double precision, timestamptz, int, double precision, double precision, uuid, text
) to authenticated;

-- 글 목록 — 0046 그대로 + run_mode(끝에 더한다)
create or replace view public.post_feed
with (security_invoker = true) as
  select
    p.id,
    p.category,
    p.crew_id,
    p.author_id,
    coalesce(pr.display_name, '러너') as author,
    p.title,
    p.body,
    p.place,
    p.lat,
    p.lng,
    p.distance_km,
    p.meet_at,
    p.capacity,
    p.created_at,
    (select count(*) from public.post_likes l where l.post_id = p.id) as likes,
    (select count(*) from public.comments c where c.post_id = p.id) as comment_count,
    (select count(*) from public.flash_participants f where f.post_id = p.id) as joined_count,
    exists (
      select 1 from public.post_likes l
       where l.post_id = p.id and l.user_id = auth.uid()
    ) as liked,
    exists (
      select 1 from public.flash_participants f
       where f.post_id = p.id and f.user_id = auth.uid()
    ) as joined,
    p.author_id = auth.uid() as mine,
    p.place_address,
    p.run_started_at,
    p.run_ended_at,
    p.run_distance_m,
    p.run_duration_s,
    p.run_route,
    p.run_mode
  from public.posts p
  left join public.profiles pr on pr.id = p.author_id
  where not public.is_blocked(p.author_id)
    and not public.is_hidden('POST', p.id::text);

grant select on public.post_feed to authenticated;

-- ── 번개 대기실은 글의 방식 · 시각을 받는다 ───────────────────────────
create or replace function economy.party_from_flash() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare
  v_post public.posts;
begin
  if new.flash_post_id is null then
    return new;
  end if;
  select * into v_post from public.posts p where p.id = new.flash_post_id;
  if found then
    new.run_mode := coalesce(v_post.run_mode, 'TOGETHER');
    new.plan_at := coalesce(new.plan_at, v_post.meet_at);
    if new.run_mode = 'TOGETHER' then
      new.meet_place := coalesce(new.meet_place, nullif(left(btrim(v_post.place), 60), ''));
      if new.meet_lat is null and v_post.lat is not null and v_post.lng is not null then
        new.meet_lat := v_post.lat;
        new.meet_lng := v_post.lng;
      end if;
    else
      new.meet_place := null;
      new.meet_lat := null;
      new.meet_lng := null;
    end if;
  end if;
  return new;
end $$;
revoke all on function economy.party_from_flash() from public;

drop trigger if exists parties_from_flash on public.parties;
create trigger parties_from_flash before insert on public.parties
  for each row execute function economy.party_from_flash();

-- ── 함께 달리기 보너스 인원 — 함께 출발한 사람 모두 ─────────────────
create or replace function economy.party_for(p_user uuid, p_started_at timestamptz,
                                             out party_id bigint, out size int)
language sql stable security definer set search_path = public as $$
  with recursive p as (
    select r.party_id, r.starts_at from public.party_runs r
     where r.user_id = p_user
       and r.starts_at between p_started_at - interval '10 minutes'
                           and p_started_at + interval '10 minutes'
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
              -- 0076 — 이 방에서 함께 출발한 사람 모두(나 포함)
              then greatest((select count(*)::int from public.party_runs r where r.party_id = p.party_id), 1)
              -- 예전 규칙 — 출발 2분 뒤에 곁이었던 사람들로 이어진 내 무리(0056)
              else (select count(*)::int from g) end
    from p
$$;
revoke all on function economy.party_for(uuid, timestamptz) from public;
