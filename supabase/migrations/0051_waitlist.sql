-- 홈페이지 사전 등록 이메일. 앱 계정과 분리해 두고, 나중에 확인된 Google 이메일과만 연결한다.
create table if not exists public.waitlist_entries (
  id uuid primary key default gen_random_uuid(),
  email text not null unique check (email = lower(btrim(email)) and length(email) between 3 and 254),
  status text not null default 'pending' check (status in ('pending', 'confirmed', 'withdrawn')),
  consented_at timestamptz not null,
  created_at timestamptz not null default now(),
  confirmed_at timestamptz,
  confirmation_token_hash text unique,
  confirmation_expires_at timestamptz,
  confirmation_sent_at timestamptz,
  constraint waitlist_confirmed_at_consistent check (
    (status = 'confirmed' and confirmed_at is not null) or
    (status <> 'confirmed' and confirmed_at is null)
  )
);

comment on table public.waitlist_entries is
  '홈페이지 출시 알림 신청. 확인된 이메일만 나중에 Google 로그인 사전 등록 혜택의 후보가 된다.';

alter table public.waitlist_entries enable row level security;
revoke all on public.waitlist_entries from public, anon, authenticated;
grant select, insert, update, delete on public.waitlist_entries to service_role;
