-- 게시판 — 글·댓글·좋아요·신고·차단을 앱이 부르는 함수로.
--
-- 표와 권한 규칙은 0004 에 이미 있다. 여기서는 앱이 부를 문을 만든다.
-- 앱이 표를 직접 두드리게 두지 않는 이유는 둘이다.
--
--   1. 한 번에 해야 하는 일이 있다. 번개 글을 쓰면 쓴 사람이 참가자 첫 줄이
--      되어야 하는데, 두 번의 요청으로 나누면 사이에서 앱이 죽었을 때
--      "주최자가 참가하지 않은 번개"가 남는다.
--   2. 도배를 막아야 한다. 앱에 박힌 키로 누구나 요청을 보낼 수 있으므로,
--      한 시간에 몇 개까지인지는 서버가 센다.

-- ════════════════════════════════════════════════════════════════════
--  글
-- ════════════════════════════════════════════════════════════════════

create or replace function public.post_create(
  p_category text,
  p_crew uuid,
  p_title text,
  p_body text,
  p_place text,
  p_distance_km double precision,
  p_meet_at timestamptz,
  p_capacity int
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
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_category not in ('FLASH', 'FREE', 'TIP') then
    raise exception '게시판 종류가 올바르지 않습니다' using errcode = '22023';
  end if;
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

  insert into public.posts (
    author_id, category, crew_id, title, body,
    place, distance_km, meet_at, capacity
  )
  values (
    v_user,
    p_category,
    p_crew,
    btrim(p_title),
    btrim(coalesce(p_body, '')),
    case when v_flash then btrim(coalesce(p_place, '')) else '' end,
    case when v_flash then greatest(coalesce(p_distance_km, 0), 0) else 0 end,
    case when v_flash then p_meet_at else null end,
    -- 번개는 둘 이상이 모여야 번개다. 정원 1은 혼자 뛰는 것과 같다.
    case when v_flash then least(greatest(coalesce(p_capacity, 2), 2), 200) else 0 end
  )
  returning id into v_id;

  -- 번개를 연 사람은 참가자 첫 줄이다.
  if v_flash then
    insert into public.flash_participants (post_id, user_id) values (v_id, v_user);
  end if;

  return v_id;
end;
$$;

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
  delete from public.posts where id = p_post and author_id = auth.uid();
  if not found then
    raise exception '내가 쓴 글만 지울 수 있습니다' using errcode = '42501';
  end if;
end;
$$;

-- 좋아요를 누르거나 거둔다. 누른 뒤의 상태(눌렸으면 true)를 돌려준다.
create or replace function public.post_toggle_like(p_post bigint)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not public.can_see_post(p_post) then
    raise exception '글을 찾을 수 없습니다' using errcode = 'P0002';
  end if;

  delete from public.post_likes where post_id = p_post and user_id = v_user;
  if found then
    return false;
  end if;
  insert into public.post_likes (post_id, user_id) values (p_post, v_user);
  return true;
end;
$$;

-- ════════════════════════════════════════════════════════════════════
--  댓글
-- ════════════════════════════════════════════════════════════════════

-- @param p_parent 0 또는 null 이면 새 댓글, 그 외에는 그 댓글에 대한 답글
create or replace function public.comment_create(p_post bigint, p_parent bigint, p_body text)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_parent bigint := nullif(p_parent, 0);
  v_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not public.can_see_post(p_post) then
    raise exception '글을 찾을 수 없습니다' using errcode = 'P0002';
  end if;
  -- 다른 글의 댓글에 답글을 달면 그 답글은 어느 글에도 보이지 않는다.
  if v_parent is not null and not exists (
    select 1 from public.comments c where c.id = v_parent and c.post_id = p_post
  ) then
    raise exception '답글을 달 댓글을 찾을 수 없습니다' using errcode = 'P0002';
  end if;
  if (select count(*) from public.comments c
       where c.author_id = v_user and c.created_at > now() - interval '1 hour') >= 60 then
    raise exception '댓글을 너무 자주 쓰고 있습니다. 잠시 뒤에 다시 써 주세요' using errcode = '23514';
  end if;

  insert into public.comments (post_id, parent_id, author_id, body)
  values (p_post, v_parent, v_user, btrim(p_body))
  returning id into v_id;
  return v_id;
end;
$$;

-- 댓글 지우기. 달린 답글도 함께 지워진다(표의 on delete cascade).
create or replace function public.comment_delete(p_comment bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  delete from public.comments where id = p_comment and author_id = auth.uid();
  if not found then
    raise exception '내가 쓴 댓글만 지울 수 있습니다' using errcode = '42501';
  end if;
end;
$$;

-- ════════════════════════════════════════════════════════════════════
--  신고와 차단
-- ════════════════════════════════════════════════════════════════════

-- 신고. 같은 사람이 같은 대상을 다시 신고해도 한 건이다. 5건이 모이면
-- 목록 뷰에서 사라진다(0010 is_hidden).
create or replace function public.content_report(
  p_type text,
  p_target text,
  p_reason text,
  p_note text
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
  insert into public.content_reports (reporter_id, target_type, target_id, reason, note)
  values (auth.uid(), p_type, p_target, p_reason, left(coalesce(p_note, ''), 1000))
  on conflict (reporter_id, target_type, target_id) do nothing;
end;
$$;

-- 차단. 그 사람의 글·댓글·코스가 내 화면에서 사라진다. 상대에게는 알리지 않는다.
create or replace function public.user_block(p_user uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_user = auth.uid() then
    raise exception '나 자신은 차단할 수 없습니다' using errcode = '22023';
  end if;
  insert into public.user_blocks (blocker_id, blocked_id)
  values (auth.uid(), p_user)
  on conflict do nothing;
end;
$$;

grant execute on function public.post_create(text, uuid, text, text, text, double precision, timestamptz, int)
  to authenticated;
grant execute on function public.post_delete(bigint) to authenticated;
grant execute on function public.post_toggle_like(bigint) to authenticated;
grant execute on function public.comment_create(bigint, bigint, text) to authenticated;
grant execute on function public.comment_delete(bigint) to authenticated;
grant execute on function public.content_report(text, text, text, text) to authenticated;
grant execute on function public.user_block(uuid) to authenticated;
