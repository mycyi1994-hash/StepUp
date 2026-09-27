-- ════════════════════════════════════════════════════════════════════
--  0041 — 동네 이야기: 일반 글에 공개 장소를 붙인다 (2026-09-27 목록형 커뮤니티)
--
--  새 커뮤니티 첫 화면은 위에 지도, 아래에 가까운 곳의 글이다. 글마다 쓴 사람이 고른
--  **공개 장소**(이름 · 주소 · 좌표)가 붙고, 읽는 사람의 폰이 자기 위치에서 그 장소까지의
--  거리를 잰다. 쓴 사람의 실시간 위치는 저장하지 않는다 — 고른 장소만이다.
--
--  post_create(0011)는 번개가 아니면 장소를 버린다. 번개 규칙은 그대로 두고, 장소가 있는
--  일반 글은 story_create 로 쓴다(예전 앱은 계속 post_create 를 부른다). 수정은 story_update —
--  같은 글 번호에 댓글 · 좋아요를 그대로 둔다. 0035 부터 posts 를 직접 고칠 수 없다.
--
--  글은 본문 한 칸으로 쓴다. 앱이 첫 줄을 제목(120자 이내)으로, 나머지를 본문으로 나눠 보낸다.
--  본문의 앞 줄바꿈은 그대로 둔다 — 제목과 본문을 다시 이어 붙여 고칠 때 줄바꿈을 잃지 않게.
-- ════════════════════════════════════════════════════════════════════

alter table public.posts add column if not exists place_address text not null default '';
alter table public.posts drop constraint if exists posts_place_address_len;
alter table public.posts add constraint posts_place_address_len check (length(place_address) <= 120);

comment on column public.posts.place is '글에 붙인 공개 장소 이름(번개는 모임 장소)';
comment on column public.posts.place_address is '공개 장소의 주소 — 지도에서 장소를 확인할 때 보인다';

-- 목록 뷰에 장소 주소를 붙인다. 열은 맨 뒤에 더한다(권한 · 순서 그대로).
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
    p.place_address
  from public.posts p
  left join public.profiles pr on pr.id = p.author_id
  where not public.is_blocked(p.author_id)
    and not public.is_hidden('POST', p.id::text);

grant select on public.post_feed to authenticated;

-- 쓰기 · 고치기가 함께 지키는 규칙. 어기면 앱이 이유를 보여 줄 수 있게 4xx 오류 코드로 알린다.
create or replace function public.story_check(
  p_title text,
  p_body text,
  p_place text,
  p_place_address text,
  p_lat double precision,
  p_lng double precision
)
returns void
language plpgsql
immutable
set search_path = public
as $$
begin
  if length(btrim(coalesce(p_title, ''))) = 0 then
    raise exception '내용을 적어 주세요' using errcode = '22023';
  end if;
  if length(btrim(p_title)) > 120 then
    raise exception '첫 줄이 너무 깁니다' using errcode = '22023';
  end if;
  if length(coalesce(p_body, '')) > 4000 then
    raise exception '글이 너무 깁니다' using errcode = '22023';
  end if;
  if length(btrim(coalesce(p_place, ''))) = 0 then
    raise exception '글을 남길 장소를 골라 주세요' using errcode = '22023';
  end if;
  if length(btrim(p_place)) > 80 or length(btrim(coalesce(p_place_address, ''))) > 120 then
    raise exception '장소 이름이 너무 깁니다' using errcode = '22023';
  end if;
  if p_lat is null or p_lng is null
     or p_lat not between -90 and 90 or p_lng not between -180 and 180 then
    raise exception '장소 좌표가 올바르지 않습니다' using errcode = '22023';
  end if;
end;
$$;

-- 장소가 있는 일반 글. 전체 게시판(crew 없음)의 자유 글이다. 도배 제한은 post_create 와 같다.
create or replace function public.story_create(
  p_title text,
  p_body text,
  p_place text,
  p_place_address text,
  p_lat double precision,
  p_lng double precision
)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform public.story_check(p_title, p_body, p_place, p_place_address, p_lat, p_lng);
  if (select count(*) from public.posts p
       where p.author_id = v_user and p.created_at > now() - interval '1 hour') >= 20 then
    raise exception '글을 너무 자주 쓰고 있습니다. 잠시 뒤에 다시 써 주세요' using errcode = '23514';
  end if;

  insert into public.posts (author_id, category, crew_id, title, body, place, place_address, lat, lng)
  values (
    v_user, 'FREE', null, btrim(p_title), coalesce(p_body, ''),
    btrim(p_place), btrim(coalesce(p_place_address, '')), p_lat, p_lng
  )
  returning id into v_id;
  return v_id;
end;
$$;

-- 내 글 고치기 — 같은 글 번호라 댓글 · 좋아요가 그대로 남는다. 번개와 크루 글은 이 길로 고치지 않는다.
create or replace function public.story_update(
  p_post bigint,
  p_title text,
  p_body text,
  p_place text,
  p_place_address text,
  p_lat double precision,
  p_lng double precision
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform public.story_check(p_title, p_body, p_place, p_place_address, p_lat, p_lng);
  update public.posts
     set title = btrim(p_title),
         body = coalesce(p_body, ''),
         place = btrim(p_place),
         place_address = btrim(coalesce(p_place_address, '')),
         lat = p_lat,
         lng = p_lng
   where id = p_post
     and author_id = auth.uid()
     and category <> 'FLASH'
     and crew_id is null;
  if not found then
    raise exception '내가 쓴 글만 고칠 수 있습니다' using errcode = '42501';
  end if;
end;
$$;

-- 검사 함수는 위 두 함수(소유자 권한)만 부른다 — 앱이 따로 부를 일이 없다
revoke all on function public.story_check(text, text, text, text, double precision, double precision) from public, anon, authenticated;
revoke all on function public.story_create(text, text, text, text, double precision, double precision) from public, anon;
revoke all on function public.story_update(bigint, text, text, text, text, double precision, double precision) from public, anon;
grant execute on function public.story_create(text, text, text, text, double precision, double precision) to authenticated;
grant execute on function public.story_update(bigint, text, text, text, text, double precision, double precision) to authenticated;
