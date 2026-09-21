-- 러닝 소식 — 하루 한 번 채워지는 표.
--
-- ── 왜 표를 따로 두는가 ──
--
-- 앱이 바깥 사이트를 직접 읽게 하면 세 가지가 무너진다.
--   1. 기기마다 파싱한다 — 사이트가 모양을 바꾸면 모든 폰이 한꺼번에 깨진다.
--   2. 사용자 수만큼 남의 서버를 두드린다 — 차단당할 짓이다.
--   3. 폰이 켜져 있어야 갱신된다 — "매일 아침 9시"가 지켜지지 않는다.
--
-- 그래서 하루 한 번 한 곳에서 모아 이 표에 넣고, 앱은 이 표만 읽는다.
-- 모으는 일은 GitHub Actions 가 한다(.github/workflows/news-refresh.yml).
-- 서버를 새로 띄우지 않으므로 **드는 비용이 없다**.
--
-- ── 저작권 ──
--
-- 남의 기사 본문은 옮기지 않는다. 제목 · 출처 · 날짜 · 원문 링크까지만
-- 담고, 읽으려면 원문으로 보낸다. 요약을 지어내지도 않는다 — 우리가 쓰지
-- 않은 문장을 우리 것처럼 두면 그게 거짓말이다.

create table if not exists public.news_items (
  id bigint generated always as identity primary key,

  -- 원문 주소. 같은 글이 두 번 들어오지 않게 하는 열쇠이기도 하다.
  url text not null unique,

  title text not null,

  -- 어디서 왔는지. 화면에 그대로 보여 준다.
  source text not null,

  -- 원문이 제공한 한 줄 요약. 없으면 빈 값 — 지어내지 않는다.
  summary text not null default '',

  -- 뉴스 탭의 세 갈래
  kind text not null default 'RUN_EVENT'
    check (kind in ('RUN_EVENT', 'DEAL', 'HEALTH')),

  published_at timestamptz not null,
  fetched_at timestamptz not null default now()
);

comment on table public.news_items is
  '하루 한 번 모아 둔 러닝 소식. 쓰는 것은 수집기(service_role)뿐이고 앱은 읽기만 한다.';

create index if not exists news_items_recent
  on public.news_items (kind, published_at desc);

alter table public.news_items enable row level security;

-- 로그인하지 않아도 읽힌다. 소식은 가려 둘 것이 아니고, 로그인을 강요하면
-- 처음 앱을 연 사람에게 빈 탭을 보여 주게 된다.
do $$ begin
  create policy news_items_read on public.news_items for select using (true);
exception when duplicate_object then null; end $$;

-- 쓰기 정책은 두지 않는다. RLS 가 켜져 있고 정책이 없으면 anon 과
-- authenticated 는 한 줄도 쓸 수 없다. 수집기는 service_role 키로 들어오고
-- 그 키는 RLS 를 지나간다 — 그 키는 GitHub Secrets 에만 있고 앱에는 없다.
