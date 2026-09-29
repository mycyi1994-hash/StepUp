-- 홈페이지 대기 명단. 이메일 소유권은 나중에 Google 로그인으로 확인한다.
create table if not exists public.waitlist_entries (
  id uuid primary key default gen_random_uuid(),
  email text not null unique check (email = lower(btrim(email)) and length(email) between 3 and 254),
  status text not null default 'registered' check (status in ('registered', 'withdrawn')),
  consented_at timestamptz not null,
  created_at timestamptz not null default now(),
  receipt_hash text not null unique
);

create table if not exists public.waitlist_share_claims (
  id uuid primary key default gen_random_uuid(),
  waitlist_id uuid not null references public.waitlist_entries(id) on delete cascade,
  platform text not null check (platform in ('x', 'instagram', 'threads')),
  post_url text not null,
  status text not null default 'submitted' check (status in ('submitted', 'verified', 'rejected')),
  submitted_at timestamptz not null default now(),
  reviewed_at timestamptz,
  unique (waitlist_id, platform)
);

comment on table public.waitlist_entries is '홈페이지 대기 명단. Google 로그인 이메일 일치 여부는 출시 후 별도 확인.';
comment on table public.waitlist_share_claims is '플랫폼별 공유 게시물 링크. submitted는 보너스 지급 또는 게시 검증을 뜻하지 않는다.';

alter table public.waitlist_entries enable row level security;
alter table public.waitlist_share_claims enable row level security;
revoke all on public.waitlist_entries, public.waitlist_share_claims from public, anon, authenticated;
grant select, insert, update, delete on public.waitlist_entries, public.waitlist_share_claims to service_role;

-- 공개 API는 두 함수만 제공한다. 이메일·게시물 링크 목록은 공개하지 않는다.
create or replace function public.waitlist_register(p_email text, p_consent boolean, p_trap text default '')
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_email text := lower(btrim(p_email));
  v_receipt text;
  v_hash text;
begin
  if p_trap is distinct from '' then
    return jsonb_build_object('ok', true);
  end if;
  if p_consent is distinct from true or p_email is null or length(p_email) > 254
     or v_email !~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$' then
    raise exception 'invalid waitlist registration' using errcode = '22023';
  end if;

  v_receipt := replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', '');
  v_hash := encode(sha256(convert_to(v_receipt, 'UTF8')), 'hex');
  insert into public.waitlist_entries (email, status, consented_at, receipt_hash)
  values (v_email, 'registered', now(), v_hash)
  on conflict (email) do update
    set status = 'registered', consented_at = now(), receipt_hash = excluded.receipt_hash;

  return jsonb_build_object('ok', true, 'receipt', v_receipt);
end;
$$;

create or replace function public.waitlist_submit_share(p_receipt text, p_platform text, p_post_url text)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_waitlist uuid;
  v_platform text := lower(btrim(p_platform));
  v_url text := btrim(p_post_url);
  v_status text;
begin
  if p_receipt !~ '^[0-9a-f]{64}$' or p_platform is null or p_post_url is null
     or length(v_url) > 2048 then
    raise exception 'invalid share claim' using errcode = '22023';
  end if;
  if (v_platform = 'x' and v_url !~* '^https://(www\.)?(x|twitter)\.com/[^/?#]+/status/[0-9]+([/?#].*)?$')
     or (v_platform = 'instagram' and v_url !~* '^https://(www\.)?instagram\.com/(p|reel)/[^/?#]+([/?#].*)?$')
     or (v_platform = 'threads' and v_url !~* '^https://(www\.)?threads\.(com|net)/@[^/?#]+/post/[^/?#]+([/?#].*)?$')
     or v_platform not in ('x', 'instagram', 'threads') then
    raise exception 'invalid share URL' using errcode = '22023';
  end if;

  select id into v_waitlist from public.waitlist_entries
  where receipt_hash = encode(sha256(convert_to(p_receipt, 'UTF8')), 'hex')
    and status = 'registered';
  if v_waitlist is null then
    raise exception 'invalid waitlist receipt' using errcode = '22023';
  end if;

  insert into public.waitlist_share_claims (waitlist_id, platform, post_url)
  values (v_waitlist, v_platform, v_url)
  on conflict (waitlist_id, platform) do update
    set post_url = excluded.post_url, status = 'submitted', submitted_at = now(), reviewed_at = null
    where public.waitlist_share_claims.status <> 'verified';
  select status into v_status from public.waitlist_share_claims
  where waitlist_id = v_waitlist and platform = v_platform;
  return jsonb_build_object('ok', true, 'status', v_status);
end;
$$;

revoke execute on function public.waitlist_register(text, boolean, text) from public, authenticated;
revoke execute on function public.waitlist_submit_share(text, text, text) from public, authenticated;
grant execute on function public.waitlist_register(text, boolean, text) to anon;
grant execute on function public.waitlist_submit_share(text, text, text) to anon;
