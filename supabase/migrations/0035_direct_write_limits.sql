-- ════════════════════════════════════════════════════════════════════
--  0035 — 앱 권한으로 표에 직접 쓰는 길을 좁힌다 (2026-09-25 점검)
--
--  앱은 가입 · 댓글 · 글을 모두 서버 함수(crew_join · comment_create · post_create)로 쓴다.
--  그런데 표 자체에도 쓰기 권한이 열려 있어, 함수가 하는 검사를 건너뛸 수 있었다.
--    · crew_members: 가입 시각(joined_at)을 과거로 적어, 방장이 나가면 먼저 들어온 사람 대신
--      방장이 된다. 숨긴(신고 누적) 크루에도 들어간다.
--    · comments: 작성 시각을 적고, 다른 글의 댓글에 답글을 달고, 시간당 개수 제한을 건너뛴다 → 직접 쓰기 막음.
--    · posts: 글쓴이가 아무 칸이나 고친다(작성 시각 · 분류 · 번개 모임 시각 · 정원).
--  가입 시각은 쓰지 못하게(기본값 now()), 댓글은 함수로만, 글 고치기는 막는다.
-- ════════════════════════════════════════════════════════════════════

-- 크루 가입 — 가입 시각은 서버가 적는다
revoke insert on public.crew_members from anon, authenticated;
grant insert (crew_id, user_id, role) on public.crew_members to authenticated;

drop policy if exists crew_members_join_self on public.crew_members;
create policy crew_members_join_self on public.crew_members for insert
  with check (
    (select auth.uid()) = user_id
    and role = 'MEMBER'
    and exists (
      select 1 from public.crews c
       where c.id = crew_id and c.join_policy = 'OPEN'
    )
    and not public.is_hidden('CREW', crew_id::text)
  );

-- 댓글 — comment_create 로만 쓴다(시간당 개수 제한 · 답글 검사가 거기 있다). 아래 정책은 혹시 권한이
-- 다시 열려도 답글이 다른 글로 가지 않게 남겨 둔다.
revoke insert on public.comments from anon, authenticated;

drop policy if exists comments_insert_own on public.comments;
create policy comments_insert_own on public.comments for insert
  with check (
    (select auth.uid()) = author_id
    and public.can_see_post(post_id)
    and (parent_id is null or exists (
      select 1 from public.comments p where p.id = parent_id and p.post_id = comments.post_id))
  );

-- 글 고치기 — 앱에는 고치는 기능이 없다(지우고 다시 쓴다)
revoke update on public.posts from anon, authenticated;
