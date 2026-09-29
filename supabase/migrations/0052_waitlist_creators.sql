-- Creator links and receipt-scoped progress. No rewards are verified or paid here.
alter table public.waitlist_share_claims drop constraint if exists waitlist_share_claims_platform_check;
alter table public.waitlist_share_claims add constraint waitlist_share_claims_platform_check
  check (platform in ('x', 'instagram', 'threads', 'youtube', 'tiktok', 'reels'));

create or replace function public.waitlist_normalize_post_url(p_platform text, p_url text)
returns text language plpgsql immutable set search_path = '' as $$
declare
  v_base text := split_part(split_part(btrim(p_url), '#', 1), '?', 1);
  v_id text;
begin
  if p_url is null or length(p_url) > 2048 or p_url ~ '[[:space:]]' then return null; end if;
  if p_platform = 'x' and v_base ~ '^https://(www\.)?(x|twitter)\.com/[A-Za-z0-9_]+/status/[0-9]+/?$' then
    return 'https://x.com/' || regexp_replace(v_base, '^https://(www\.)?(x|twitter)\.com/', '');
  elsif p_platform in ('instagram', 'reels') and v_base ~ '^https://(www\.)?instagram\.com/(p|reel)/[A-Za-z0-9_-]+/?$'
    and (p_platform = 'instagram' or v_base ~ '/reel/') then
    return rtrim(regexp_replace(v_base, '^https://(www\.)?', 'https://'), '/');
  elsif p_platform = 'threads' and v_base ~ '^https://(www\.)?threads\.(com|net)/@[A-Za-z0-9_.]+/post/[A-Za-z0-9_-]+/?$' then
    return rtrim(regexp_replace(v_base, '^https://(www\.)?threads\.(com|net)/', 'https://www.threads.com/'), '/');
  elsif p_platform = 'youtube' then
    if v_base ~ '^https://(www\.)?youtu\.be/[A-Za-z0-9_-]{11}/?$' then
      v_id := substring(v_base from 'youtu\.be/([A-Za-z0-9_-]{11})');
    elsif v_base ~ '^https://(www\.|m\.)?youtube\.com/shorts/[A-Za-z0-9_-]{11}/?$' then
      v_id := substring(v_base from '/shorts/([A-Za-z0-9_-]{11})');
    elsif v_base ~ '^https://(www\.|m\.)?youtube\.com/watch$' then
      v_id := substring(split_part(p_url, '#', 1) from '[?&]v=([A-Za-z0-9_-]{11})(&|$)');
    end if;
    if v_id is not null then return 'https://www.youtube.com/watch?v=' || v_id; end if;
  elsif p_platform = 'tiktok' and (
    v_base ~ '^https://(www\.|m\.)?tiktok\.com/@[A-Za-z0-9_.]+/video/[0-9]+/?$'
    or v_base ~ '^https://(vm|vt)\.tiktok\.com/[A-Za-z0-9_-]+/?$'
    or v_base ~ '^https://(www\.)?tiktok\.com/t/[A-Za-z0-9_-]+/?$'
  ) then
    return rtrim(regexp_replace(v_base, '^https://(www\.|m\.)?', 'https://'), '/');
  end if;
  return null;
end $$;
revoke all on function public.waitlist_normalize_post_url(text, text) from public, anon, authenticated;

create or replace function public.waitlist_status(p_receipt text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare v_waitlist uuid; v_claims jsonb;
begin
  if p_receipt is null or p_receipt !~ '^[0-9a-f]{64}$' then
    raise exception 'invalid waitlist receipt' using errcode = '22023';
  end if;
  select id into v_waitlist from public.waitlist_entries
  where receipt_hash = encode(sha256(convert_to(p_receipt, 'UTF8')), 'hex') and status = 'registered';
  if v_waitlist is null then raise exception 'invalid waitlist receipt' using errcode = '22023'; end if;
  -- Return only statuses: no email, URL, entry id, or timestamps are exposed.
  select coalesce(jsonb_agg(jsonb_build_object('platform', platform, 'status', status) order by platform), '[]'::jsonb)
  into v_claims from public.waitlist_share_claims where waitlist_id = v_waitlist;
  return jsonb_build_object('ok', true, 'claims', v_claims);
end $$;
revoke all on function public.waitlist_status(text) from public, authenticated;
grant execute on function public.waitlist_status(text) to anon;

create or replace function public.waitlist_submit_share(p_receipt text, p_platform text, p_post_url text)
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_waitlist uuid;
  v_platform text := lower(btrim(p_platform));
  v_url text;
  v_status text;
begin
  if p_receipt is null or p_receipt !~ '^[0-9a-f]{64}$' or p_platform is null then
    raise exception 'invalid share claim' using errcode = '22023';
  end if;
  v_url := public.waitlist_normalize_post_url(v_platform, p_post_url);
  if v_url is null then raise exception 'invalid share URL' using errcode = '22023'; end if;
  -- Serialize submissions for one entry so concurrent requests cannot double-count a link.
  select id into v_waitlist from public.waitlist_entries
  where receipt_hash = encode(sha256(convert_to(p_receipt, 'UTF8')), 'hex') and status = 'registered'
  for update;
  if v_waitlist is null then raise exception 'invalid waitlist receipt' using errcode = '22023'; end if;
  if exists (
    select 1 from public.waitlist_share_claims where waitlist_id = v_waitlist and platform <> v_platform
      and public.waitlist_normalize_post_url(platform, post_url) = v_url
  ) then raise exception 'duplicate share URL' using errcode = '22023'; end if;
  insert into public.waitlist_share_claims (waitlist_id, platform, post_url)
  values (v_waitlist, v_platform, v_url)
  on conflict (waitlist_id, platform) do update
    set post_url = excluded.post_url, status = 'submitted', submitted_at = now(), reviewed_at = null
    where public.waitlist_share_claims.status <> 'verified';
  select status into v_status from public.waitlist_share_claims where waitlist_id = v_waitlist and platform = v_platform;
  return public.waitlist_status(p_receipt) || jsonb_build_object('status', v_status);
end $$;
revoke execute on function public.waitlist_submit_share(text, text, text) from public, authenticated;
grant execute on function public.waitlist_submit_share(text, text, text) to anon;

-- Operator-only summary for later bonus decisions, always derived from stored claims.
create or replace view public.waitlist_bonus_candidates as
select e.id, e.email, e.status, e.created_at,
  count(c.id) filter (where c.platform in ('x', 'instagram', 'threads') and c.status <> 'rejected')::integer as social_submitted,
  count(c.id) filter (where c.platform in ('x', 'instagram', 'threads') and c.status <> 'rejected') = 3 as all_three_submitted,
  count(c.id) filter (where c.platform in ('youtube', 'tiktok', 'reels') and c.status <> 'rejected') > 0 as creator_submitted,
  count(c.id) filter (where c.platform in ('x', 'instagram', 'threads') and c.status = 'verified') = 3 as all_three_verified
from public.waitlist_entries e left join public.waitlist_share_claims c on c.waitlist_id = e.id
group by e.id;
revoke all on public.waitlist_bonus_candidates from public, anon, authenticated;
grant select on public.waitlist_bonus_candidates to service_role;
comment on view public.waitlist_bonus_candidates is 'Operator-only waitlist bonus candidates. Submission flags do not mean verification or payment.';
