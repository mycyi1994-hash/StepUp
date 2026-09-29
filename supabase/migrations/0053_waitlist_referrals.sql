-- 사전 등록 추천 기록. 등록자마다 공개 추천 코드를 주고, 그 코드의 링크로 새로 등록한 사람을 기록한다.
-- 보상 규칙은 없다(2026-09-29 사용자 결정: 기록만). 추천 수는 운영자 보기에서만 본다.
create or replace function public.waitlist_new_referral_code()
returns text language sql volatile set search_path = '' as $$
  -- 32자(헷갈리는 I·O·0·1 제외)에서 8자를 뽑는다.
  select string_agg(substr('ABCDEFGHJKLMNPQRSTUVWXYZ23456789', get_byte(b, i) % 32 + 1, 1), '' order by i)
  from (select uuid_send(gen_random_uuid()) as b) r, generate_series(0, 7) as i
$$;
revoke all on function public.waitlist_new_referral_code() from public, anon, authenticated;

alter table public.waitlist_entries add column if not exists referral_code text;
alter table public.waitlist_entries add column if not exists referred_by uuid;
update public.waitlist_entries set referral_code = public.waitlist_new_referral_code() where referral_code is null;
alter table public.waitlist_entries alter column referral_code set default public.waitlist_new_referral_code();
alter table public.waitlist_entries alter column referral_code set not null;
do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'waitlist_entries_referral_code_key') then
    alter table public.waitlist_entries add constraint waitlist_entries_referral_code_key unique (referral_code);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'waitlist_entries_referral_code_check') then
    alter table public.waitlist_entries add constraint waitlist_entries_referral_code_check
      check (referral_code ~ '^[A-HJ-NP-Z2-9]{8}$');
  end if;
  -- 추천한 사람이 명단에서 지워지면 기록만 비운다.
  if not exists (select 1 from pg_constraint where conname = 'waitlist_entries_referred_by_fkey') then
    alter table public.waitlist_entries add constraint waitlist_entries_referred_by_fkey
      foreign key (referred_by) references public.waitlist_entries(id) on delete set null;
  end if;
end $$;
create index if not exists waitlist_entries_referred_by_idx on public.waitlist_entries (referred_by);
comment on column public.waitlist_entries.referral_code is '공유 링크(?ref=)에 쓰는 공개 추천 코드. 영수증과 달리 비밀이 아니다.';
comment on column public.waitlist_entries.referred_by is '처음 등록할 때 쓴 추천 코드의 주인. 다시 등록해도 바뀌지 않는다. 보상 지급을 뜻하지 않는다.';

drop function if exists public.waitlist_register(text, boolean, text);
create or replace function public.waitlist_register(p_email text, p_consent boolean, p_trap text default '', p_ref text default '')
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_email text := lower(btrim(p_email));
  v_ref text := upper(btrim(coalesce(p_ref, '')));
  v_referrer uuid;
  v_receipt text;
  v_hash text;
  v_code text;
begin
  if p_trap is distinct from '' then
    return jsonb_build_object('ok', true);
  end if;
  if p_consent is distinct from true or p_email is null or length(p_email) > 254
     or v_email !~ '^[^[:space:]@]+@[^[:space:]@]+\.[^[:space:]@]+$' then
    raise exception 'invalid waitlist registration' using errcode = '22023';
  end if;
  -- 틀리거나 없는 추천 코드는 등록을 막지 않고 무시한다.
  if v_ref ~ '^[A-HJ-NP-Z2-9]{8}$' then
    select id into v_referrer from public.waitlist_entries
    where referral_code = v_ref and status = 'registered' and email <> v_email;
  end if;

  v_receipt := replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', '');
  v_hash := encode(sha256(convert_to(v_receipt, 'UTF8')), 'hex');
  -- 추천인은 처음 등록할 때만 남는다. 같은 이메일로 다시 등록해도 추천 코드와 추천인은 그대로다.
  insert into public.waitlist_entries (email, status, consented_at, receipt_hash, referred_by)
  values (v_email, 'registered', now(), v_hash, v_referrer)
  on conflict (email) do update
    set status = 'registered', consented_at = now(), receipt_hash = excluded.receipt_hash
  returning referral_code into v_code;

  return jsonb_build_object('ok', true, 'receipt', v_receipt, 'referral_code', v_code);
end;
$$;
revoke execute on function public.waitlist_register(text, boolean, text, text) from public, authenticated;
grant execute on function public.waitlist_register(text, boolean, text, text) to anon;

create or replace function public.waitlist_status(p_receipt text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_waitlist uuid; v_code text; v_claims jsonb;
begin
  if p_receipt is null or p_receipt !~ '^[0-9a-f]{64}$' then
    raise exception 'invalid waitlist receipt' using errcode = '22023';
  end if;
  select id, referral_code into v_waitlist, v_code from public.waitlist_entries
  where receipt_hash = encode(sha256(convert_to(p_receipt, 'UTF8')), 'hex') and status = 'registered';
  if v_waitlist is null then raise exception 'invalid waitlist receipt' using errcode = '22023'; end if;
  -- Return only statuses and the public referral code: no email, URL, entry id, or timestamps are exposed.
  select coalesce(jsonb_agg(jsonb_build_object('platform', platform, 'status', status) order by platform), '[]'::jsonb)
  into v_claims from public.waitlist_share_claims where waitlist_id = v_waitlist;
  return jsonb_build_object('ok', true, 'claims', v_claims, 'referral_code', v_code);
end $$;
revoke all on function public.waitlist_status(text) from public, authenticated;
grant execute on function public.waitlist_status(text) to anon;

-- Operator-only referral counts. A separate view, because setup.sql re-runs 0052's bonus view definition.
create or replace view public.waitlist_referral_counts as
select e.id, e.email, e.referral_code,
  count(r.id) filter (where r.status = 'registered')::integer as referred_count
from public.waitlist_entries e left join public.waitlist_entries r on r.referred_by = e.id
group by e.id;
revoke all on public.waitlist_referral_counts from public, anon, authenticated;
grant select on public.waitlist_referral_counts to service_role;
comment on view public.waitlist_referral_counts is 'Operator-only waitlist referral counts. Counts do not mean any reward was granted.';
