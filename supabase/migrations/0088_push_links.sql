-- ════════════════════════════════════════════════════════════════════
--  0088 — 푸시를 누르면 그 글 · 번개로 (2026-11-03)
--
--  · 댓글 · 답글 푸시(COMMENT · REPLY)는 크루 화면(crew/<id>)이나 빈 링크 대신 그 글을 연다(post/<글 번호>).
--    번개러닝 글에 달린 댓글이면 그 번개(flash/<글 번호>) — 번개는 번개 상세에서 본다.
--  · 크루 번개 푸시(CREW_FLASH)는 크루 화면 대신 그 번개(flash/<글 번호>)를 연다.
--  예전 앱은 이 링크를 모르면 앱만 연다(예전에도 크루 밖 글의 댓글은 빈 링크였다). 받는 사람 · 알림 설정 · 글 값은 0017 그대로다 —
--  링크 한 칸만 바꿨다. 대기실 푸시(PARTY_OPEN)는 0087 이 정한다.
-- ════════════════════════════════════════════════════════════════════

-- 댓글 → 글쓴이에게, 답글 → 댓글 단 사람에게. 자기 자신에게는 보내지 않는다 — 0017 과 같고 링크만 그 글로
create or replace function public.push_on_comment()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_to uuid;
  v_kind text;
  v_title text;
  v_category text;
begin
  select p.title, p.category into v_title, v_category from public.posts p where p.id = new.post_id;
  if new.parent_id is not null then
    select c.author_id into v_to from public.comments c where c.id = new.parent_id;
    v_kind := 'REPLY';
  else
    select p.author_id into v_to from public.posts p where p.id = new.post_id;
    v_kind := 'COMMENT';
  end if;
  if v_to is not null and v_to <> new.author_id then
    perform public.push_enqueue(
      v_to, v_kind,
      jsonb_build_object('name', public.push_display_name(new.author_id), 'title', coalesce(v_title, '')),
      case when v_category = 'FLASH' then 'flash/' else 'post/' end || new.post_id::text);
  end if;
  return new;
end;
$$;

-- 크루 게시판에 번개가 올라왔다 → 그 크루원들에게(쓴 사람 빼고) — 0017 과 같고 링크만 그 번개로
create or replace function public.push_on_flash()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_member uuid;
begin
  if new.category <> 'FLASH' or new.crew_id is null then
    return new;
  end if;
  for v_member in
    select m.user_id from public.crew_members m
     where m.crew_id = new.crew_id and m.user_id <> new.author_id
  loop
    perform public.push_enqueue(
      v_member, 'CREW_FLASH',
      jsonb_build_object('name', public.push_display_name(new.author_id), 'title', new.title),
      'flash/' || new.id::text);
  end loop;
  return new;
end;
$$;
