-- ════════════════════════════════════════════════════════════════════
--  0043 — 앱 공지(알림 › 공지, 2026-09-28 알림·공지 전달본 · docs/redesign/notifications-v1)
--
--  운영자가 올리는 StepUp 안내. 바깥 러닝 소식(0008 news_items · 0009 running_news)과 다른 것이다.
--  이 파일은 표와 읽기 규칙만 만든다 — 글은 넣지 않는다(전달본의 공지 제목 · 날짜 · 본문은 디자인 예시).
--  글이 없으면 앱은 "아직 공지가 없어요"를 보인다.
--
--  앱은 공개했고(visible) 게시 시각이 지난 글만 읽는다. 내리거나 비공개로 바꾸면 다음에 읽을 때
--  목록에서 빠지고, 이미 받아 둔 글을 열면 "지금은 볼 수 없는 공지"가 된다(서버 답이 폰의 사본보다 먼저다).
--
--  title · body  언어 코드별 글 {"ko": "...", "en": "...", "ja": "...", "zh": "..."}
--                — 앱은 지금 언어, 없으면 ko, en 순으로 고른다. 제목은 하나 이상 있어야 한다.
--  body          빈 줄로 문단을 나눈다. "## " 로 시작하는 줄은 소제목, "---" 한 줄은 구분선
--                (구분선 아래 문단은 작은 안내 글자로 보인다).
--  action        글 아래 버튼 하나 — 정해 둔 앱 안 화면만: DRAW(신발 뽑기, 기회를 쓰지 않는다) ·
--                RUN_HISTORY(러닝 기록) · NOTIFICATION_SETTINGS(알림 설정) · PRIVACY_SETTINGS(개인정보 · 앱 권한).
--                비우면 버튼 없이 글만.
--
--  올리기 · 내리기(대시보드 SQL Editor — service_role 로 돈다. 앱에는 쓰기 길이 없다):
--    insert into public.announcements (title, body, visible)
--      values ('{"ko": "제목"}', '{"ko": "본문"}', true);
--    update public.announcements set visible = false, updated_at = now() where id = 1;
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.announcements (
  id bigint generated always as identity primary key,
  title jsonb not null,
  body jsonb not null default '{}'::jsonb,
  action text,
  visible boolean not null default false,
  published_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint announcements_title_text check (jsonb_typeof(title) = 'object' and title <> '{}'::jsonb),
  constraint announcements_body_text check (jsonb_typeof(body) = 'object'),
  constraint announcements_action_known check (
    action is null or action in ('DRAW', 'RUN_HISTORY', 'NOTIFICATION_SETTINGS', 'PRIVACY_SETTINGS')
  )
);

comment on table public.announcements is
  '앱 공지(알림 › 공지). 운영자(service_role)가 쓰고 앱은 공개 · 게시된 글만 읽는다. 예시 글을 넣지 않는다.';

create index if not exists announcements_published
  on public.announcements (published_at desc, id desc) where visible;

alter table public.announcements enable row level security;

-- 로그인하지 않아도 읽힌다(공개 안내). 쓰기 정책은 두지 않는다 — 앱은 한 줄도 쓸 수 없다.
revoke all on public.announcements from anon, authenticated;
grant select on public.announcements to anon, authenticated;

drop policy if exists announcements_read on public.announcements;
create policy announcements_read on public.announcements for select to anon, authenticated
  using (visible and published_at <= now());
