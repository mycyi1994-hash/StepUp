-- ════════════════════════════════════════════════════════════════════
--  0091 — 데이터 노출 · 남용 막기 (2026-11-04)
--
--  사용자 "다 수정해놔"(서버 점검의 데이터 노출 · 남용 6건).
--
--  1. 로그인 안 한 요청(anon 키 — 앱에 박혀 있어 누구나 꺼낸다)이 모든 사람의 프로필 이름(가입 때의 구글 실명) ·
--     크루 명단 · 거래 · 입찰 · 번개 참가자 · 공개 글(러닝 경로 · 좌표) · 공유 코스(경로)를 내려받을 수 있었다.
--     앱은 이 표 · 뷰를 로그인한 뒤에만(authenticated) 읽고, 로그인 전에는 공지 · 소식 · 출처 · 접속 문 · 대기 명단만 부른다.
--     홈페이지 초대 페이지(/c/<크루> · /f/<번개>)도 크루 정보를 읽지 않는다. 그래서 anon 에서만 거둔다(로그인한 읽기는 그대로).
--     접속 코드 문(0079 access_gate)은 로그인한 사람만 보므로 비공개 베타 중에도 anon 은 열려 있었다.
--  2. 새 가입자의 프로필 이름은 구글 이름(full_name)이 아니라 '러너' — 이미 있는 사람의 이름은 건드리지 않는다.
--     이름은 앱에서 정한다(profile_name_set). 앱은 서버 이름이 '러너'면 받아 오지 않는다(EconomySync.pullNickname).
--  3. 프로필 이름은 PATCH(0001 의 칸 권한)로도 바꿀 수 있어 profile_name_set 의 규칙(제어 문자 · 16자)을 건너뛰었다 —
--     이름이 바뀔 때만 같은 규칙으로 다듬는다(트리거). 이름을 안 바꾸는 갱신(거리 · 연속 등)은 예전 긴 이름이어도 그대로 된다.
--  4. 대기 명단: 이미 있는 이메일로 다시 등록하면 새 영수증을 주어 남의 등록(공유 제출 · 추천 코드)을 가로챌 수 있었다 —
--     있는 이메일은 영수증을 바꾸지 않고 {ok:true, already:true} 만(영수증 · 추천 코드 없이). 영수증은 처음 등록한 창에만 있다.
--     공개 등록(waitlist_register)은 API 로 왔는데(request.headers 있음) 부른 곳(IP)을 모르면 받지 않는다 — 횟수 제한을 건너뛰지 않게.
--     SQL 편집기 · 검사처럼 API 밖에서 부르면(request.headers 없음) 예전처럼 받는다.
--  5. 신고 5건 자동 숨김은 이제 "가입 3일이 지났고 무효 아닌 러닝이 하나라도 있는 계정"의 신고만 센다(새로 만든 계정 여럿으로
--     멀쩡한 글을 내리지 못하게). 신고는 모두 그대로 쌓이고 사람이 본다. 값은 설정(report_reporter_min_days · report_reporter_needs_run).
--  6. 크루 채팅 보내기에 사람마다 횟수 제한 — 1분 20개 · 사진 1시간 30장 · 하루 100장(설정 chat_*). 넘으면
--     'chat_rate_limited' (22023 → HTTP 400). 같은 요청 키로 다시 보내는 것(이미 받은 메시지)은 세지 않는다.
-- ════════════════════════════════════════════════════════════════════

-- ══════════════════════════════════════════════════════════════════
-- 1. 로그인 안 한 요청은 사람 · 크루 · 거래 · 글 표를 읽지 못한다
-- ══════════════════════════════════════════════════════════════════
-- 표마다 RLS 가 "모두에게 보임(true)"이거나 공개 글(크루 없는 글 · 공유 코스)을 열어 두었다. 로그인한 사람에게는 그대로 보인다.
revoke all on
  public.profiles, public.crew_members, public.crews, public.crew_images, public.crew_applications,
  public.flash_participants, public.market_listings, public.market_bids, public.market_trades,
  public.posts, public.comments, public.post_likes, public.courses, public.course_likes
from anon;

-- 위 표를 읽는 뷰(security_invoker — anon 이면 어차피 표 권한에서 막힌다). 오류 대신 권한 없음으로 분명히 둔다
revoke all on
  public.post_feed, public.comment_feed, public.crew_feed, public.course_feed, public.flash_roster,
  public.market_quotes, public.market_asks, public.market_bid_book
from anon;

-- 옛 순위표(0006)도 이름 · 사용자 번호 · 최고 속도 · 적립을 돌려준다 — 기본 권한으로 anon 도 부를 수 있었다.
-- 앱은 로그인한 뒤에만 부른다(StepUpServer.leaderboard). 새 순위표(0061)는 이미 로그인한 사람만
revoke execute on function public.leaderboard(text, int, text) from public, anon;
grant execute on function public.leaderboard(text, int, text) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 2 · 3. 프로필 이름 — 새 가입자는 '러너', 이름이 바뀔 때는 늘 같은 규칙
-- ══════════════════════════════════════════════════════════════════

-- 이름 다듬기 — 제어 문자 · 보이지 않는 방향/폭 문자를 빼고 앞뒤 공백을 떼어 16자(앱 UserPrefs.NICKNAME_MAX). 비면 '러너'.
-- (0084 profile_name_set 의 규칙 + 자른 뒤 끝 공백 · 방향 바꾸기 문자 — 이름이 남의 글자처럼 보이지 않게)
create or replace function economy.profile_name_clean(p_name text) returns text
  language sql immutable set search_path = '' as $$
  select coalesce(
    nullif(btrim(left(btrim(regexp_replace(coalesce(p_name, ''),
                                           '[[:cntrl:]​‎‏‪-‮⁦-⁩﻿]', '', 'g')), 16)), ''),
    '러너')
$$;
revoke all on function economy.profile_name_clean(text) from public;

-- 가입하면 프로필 — 구글 이름(실명)은 가져오지 않는다. 이미 있는 줄은 그대로(on conflict do nothing)
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, display_name)
  values (new.id, '러너')
  on conflict (id) do nothing;
  return new;
end;
$$;

-- 이름이 바뀔 때만 — PATCH 든 서버 함수든 같은 규칙. 바뀌지 않으면(거리 · 연속 갱신) 손대지 않는다
create or replace function economy.profiles_name_guard() returns trigger
  language plpgsql security definer set search_path = '' as $$
begin
  if new.display_name is distinct from old.display_name then
    new.display_name := economy.profile_name_clean(new.display_name);
  end if;
  return new;
end $$;
revoke all on function economy.profiles_name_guard() from public;

drop trigger if exists profiles_name_guard on public.profiles;
create trigger profiles_name_guard
  before update of display_name on public.profiles
  for each row execute function economy.profiles_name_guard();

-- 0084 그대로 — 다듬기만 같은 함수로(돌려주는 이름 = 저장된 이름)
create or replace function public.profile_name_set(p_name text)
returns text
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := auth.uid();
  v_name text := economy.profile_name_clean(p_name);
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  update public.profiles set display_name = v_name where id = v_user;
  if not found then
    insert into public.profiles (id, display_name) values (v_user, v_name)
    on conflict (id) do update set display_name = excluded.display_name;
  end if;
  return v_name;
end $$;

revoke all on function public.profile_name_set(text) from public, anon;
grant execute on function public.profile_name_set(text) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 4. 대기 명단 — 있는 이메일은 영수증을 바꾸지 않는다 · API 로 왔는데 부른 곳을 모르면 받지 않는다
-- ══════════════════════════════════════════════════════════════════

-- 0083 그대로 — 저장 부분만: 새 이메일이면 영수증 · 추천 코드, 있는 이메일이면 등록 상태 · 동의 시각만 새로 하고 {ok, already}
create or replace function economy.waitlist_register_core(p_email text, p_consent boolean, p_trap text, p_ref text, p_client text)
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
  -- 같은 곳에서 너무 자주 — 잘못된 시도도 센다(예외 대신 답으로 끝내 기록이 남는다)
  if p_client is not null then
    delete from economy.waitlist_attempts where ip_hash = p_client and at < now() - interval '1 day';
    if (select count(*) from economy.waitlist_attempts
         where ip_hash = p_client and at > now() - interval '10 minutes')
         >= coalesce((select (value #>> '{}')::int from public.economy_settings where key = 'waitlist_ip_per_10min'), 6)
       or (select count(*) from economy.waitlist_attempts where ip_hash = p_client)
         >= coalesce((select (value #>> '{}')::int from public.economy_settings where key = 'waitlist_ip_per_day'), 30) then
      return jsonb_build_object('ok', false, 'message', 'too many waitlist attempts');
    end if;
    insert into economy.waitlist_attempts (ip_hash) values (p_client);
  end if;

  if p_trap is distinct from '' then
    return jsonb_build_object('ok', true);
  end if;
  if p_consent is distinct from true or p_email is null or length(p_email) > 254
     or not economy.waitlist_email_ok(v_email) then
    return jsonb_build_object('ok', false, 'message', 'invalid waitlist registration');
  end if;
  -- 틀리거나 없는 추천 코드는 등록을 막지 않고 무시한다.
  if v_ref ~ '^[A-HJ-NP-Z2-9]{8}$' then
    select id into v_referrer from public.waitlist_entries
    where referral_code = v_ref and status = 'registered' and email <> v_email;
  end if;

  v_receipt := replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', '');
  v_hash := encode(sha256(convert_to(v_receipt, 'UTF8')), 'hex');
  insert into public.waitlist_entries (email, status, consented_at, receipt_hash, referred_by)
  values (v_email, 'registered', now(), v_hash, v_referrer)
  on conflict (email) do nothing
  returning referral_code into v_code;

  if not found then
    -- 이미 있는 이메일 — 이메일 주인인지 알 길이 없으므로 영수증(공유 제출 · 진행 상황 · 추천 코드의 열쇠)은 바꾸지도 주지도 않는다.
    -- 추천인 · 추천 코드도 그대로. 다시 동의한 것만 적는다(예전처럼).
    update public.waitlist_entries
       set status = 'registered', consented_at = now()
     where email = v_email;
    return jsonb_build_object('ok', true, 'already', true);
  end if;

  return jsonb_build_object('ok', true, 'receipt', v_receipt, 'referral_code', v_code);
end;
$$;
revoke all on function economy.waitlist_register_core(text, boolean, text, text, text) from public;

-- 공개 등록(홈페이지가 바로 부른다) — 사람 확인을 켜면 받지 않는다 · API 로 왔는데 부른 곳(IP)을 모르면 받지 않는다
create or replace function public.waitlist_register(p_email text, p_consent boolean, p_trap text default '', p_ref text default '')
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_client text := economy.waitlist_client();
begin
  if coalesce((select value = 'true'::jsonb from public.economy_settings where key = 'waitlist_require_turnstile'), false) then
    return jsonb_build_object('ok', false, 'message', 'verification required');
  end if;
  -- IP 를 모르면(운영 Supabase 는 직접 부른 요청에 IP 머리글을 넘기지 않는다 — 2026-11-04 확인: 7번 연속에도 횟수 제한이 걸리지 않음)
  -- 같은 곳 횟수는 세지 못한다. 막지는 않는다(막으면 홈페이지 등록이 모두 멈춘다). 실제 방어는 사람 확인(워커 · waitlist_require_turnstile)
  return economy.waitlist_register_core(p_email, p_consent, p_trap, p_ref, v_client);
end;
$$;
revoke execute on function public.waitlist_register(text, boolean, text, text) from public, authenticated;
grant execute on function public.waitlist_register(text, boolean, text, text) to anon;

-- 워커가 Turnstile 을 확인한 뒤 — 어테스터만(0083 그대로). 워커는 Cloudflare 의 cf-connecting-ip 를 늘 넘긴다 — 비면 받지 않는다
create or replace function public.attester_waitlist_register(p_email text, p_consent boolean, p_trap text, p_ref text, p_ip text)
returns jsonb
language plpgsql security definer set search_path = public, economy
as $$
declare
  v_client text;
begin
  perform economy.attester_guard();
  v_client := economy.waitlist_client_of(p_ip);
  if v_client is null then
    return jsonb_build_object('ok', false, 'message', 'unknown waitlist client');
  end if;
  return economy.waitlist_register_core(p_email, p_consent, coalesce(p_trap, ''), coalesce(p_ref, ''), v_client);
end;
$$;
revoke all on function public.attester_waitlist_register(text, boolean, text, text, text) from public, anon;
grant execute on function public.attester_waitlist_register(text, boolean, text, text, text) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 5. 신고 숨김 — 가입 며칠 지났고 러닝을 한 계정의 신고만 센다
-- ══════════════════════════════════════════════════════════════════
insert into public.economy_settings (key, value) values
  -- 신고한 때 계정 나이(일). 0 이면 따지지 않는다
  ('report_reporter_min_days', '3'::jsonb),
  -- 무효 아닌 러닝이 하나라도 있어야 센다
  ('report_reporter_needs_run', 'true'::jsonb)
on conflict (key) do nothing;

-- 0010 그대로 — 5건 · 기각은 빼고. 셀 신고만 좁혔다. 대부분(신고 5건 미만)은 앞의 셈 하나로 끝난다
create or replace function public.is_hidden(p_type text, p_id text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select case
    when (select count(*) from public.content_reports r
           where r.target_type = p_type and r.target_id = p_id and r.status <> 'DISMISSED') < 5 then false
    else (
      select count(*) from public.content_reports r
        join auth.users u on u.id = r.reporter_id
       where r.target_type = p_type
         and r.target_id = p_id
         and r.status <> 'DISMISSED'
         and u.created_at <= r.created_at
               - coalesce(economy.setting_num('report_reporter_min_days'), 3)::float8 * interval '1 day'
         and (coalesce((select s.value <> 'true'::jsonb from public.economy_settings s
                         where s.key = 'report_reporter_needs_run'), false)
              or exists (select 1 from public.walk_sessions w
                          where w.user_id = r.reporter_id and w.verdict <> 'VOID'))
    ) >= 5
  end
$$;

comment on function public.is_hidden is
  '신고가 5건 이상 쌓였는지. 기각된 신고와 새 계정(report_reporter_min_days) · 러닝 없는 계정(report_reporter_needs_run)의 신고는 세지 않는다(0091).';

-- ══════════════════════════════════════════════════════════════════
-- 6. 크루 채팅 보내기 — 사람마다 횟수 제한
-- ══════════════════════════════════════════════════════════════════
insert into public.economy_settings (key, value) values
  ('chat_messages_per_min', '20'::jsonb),
  ('chat_images_per_hour', '30'::jsonb),
  ('chat_images_per_day', '100'::jsonb)
on conflict (key) do nothing;

-- 한 사람이 최근에 보낸 메시지(모든 크루) — 횟수 셈
create index if not exists crew_chat_messages_author_recent
  on public.crew_chat_messages (author_id, created_at) where author_id is not null;

-- 0048 그대로 — 받은 적 없는 새 메시지만 횟수를 본다(같은 요청 키는 처음 메시지를 돌려줄 뿐 세지 않는다)
create or replace function public.crew_chat_send(
  p_crew uuid,
  p_client_id uuid,
  p_body text default '',
  p_reply_to bigint default null,
  p_image text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_msg public.crew_chat_messages%rowtype;
  v_body text := btrim(coalesce(p_body, ''), E' \t\r\n');
  v_rev bigint;
begin
  perform public.crew_chat_member(p_crew, true);
  if p_client_id is null then
    raise exception 'invalid:client' using errcode = '22023';
  end if;
  select * into v_msg from public.crew_chat_messages m where m.author_id = v_me and m.client_id = p_client_id;
  if found then
    if v_msg.crew_id <> p_crew then
      raise exception 'invalid:client' using errcode = '22023';
    end if;
    return public.crew_chat_json(v_msg, v_me);
  end if;
  if p_image is null and v_body = '' then
    raise exception 'invalid:empty' using errcode = '22023', detail = '보낼 내용이 없습니다';
  end if;
  if length(v_body) > 2000 then
    raise exception 'invalid:body' using errcode = '22023', detail = '메시지는 2000자까지입니다';
  end if;
  if p_image is not null and not public.crew_image_ok(p_image) then
    raise exception 'invalid:image' using errcode = '22023', detail = '사진을 읽을 수 없습니다';
  end if;
  if p_reply_to is not null and not exists (
    select 1 from public.crew_chat_messages r where r.id = p_reply_to and r.crew_id = p_crew and r.kind <> 'SYSTEM'
  ) then
    raise exception 'invalid:reply' using errcode = '22023';
  end if;

  -- 횟수 — 한 사람의 보내기를 하나씩(동시에 여러 개를 보내 셈을 건너뛰지 않게). 지운 메시지도 센다
  perform pg_advisory_xact_lock(hashtextextended('crew_chat_send:' || v_me::text, 0));
  if (select count(*) from public.crew_chat_messages m
       where m.author_id = v_me and m.created_at > now() - interval '1 minute')
     >= coalesce(economy.setting_num('chat_messages_per_min'), 20) then
    raise exception 'chat_rate_limited' using errcode = '22023',
      detail = '메시지를 너무 빨리 보내고 있어요. 잠시 뒤에 다시 보내 주세요', hint = 'messages';
  end if;
  if p_image is not null and (
       (select count(*) from public.crew_chat_messages m
         where m.author_id = v_me and m.kind = 'IMAGE' and m.created_at > now() - interval '1 hour')
         >= coalesce(economy.setting_num('chat_images_per_hour'), 30)
       or (select count(*) from public.crew_chat_messages m
            where m.author_id = v_me and m.kind = 'IMAGE' and m.created_at > now() - interval '1 day')
         >= coalesce(economy.setting_num('chat_images_per_day'), 100)) then
    raise exception 'chat_rate_limited' using errcode = '22023',
      detail = '사진을 너무 많이 보냈어요. 잠시 뒤에 다시 보내 주세요', hint = 'images';
  end if;

  v_rev := public.crew_chat_next_rev(p_crew);
  insert into public.crew_chat_messages (crew_id, seq, rev, author_id, kind, body, reply_to, client_id)
  values (p_crew, v_rev, v_rev, v_me, case when p_image is null then 'TEXT' else 'IMAGE' end, v_body, p_reply_to, p_client_id)
  on conflict (author_id, client_id) where client_id is not null do nothing
  returning * into v_msg;
  if v_msg.id is null then
    -- 같은 요청이 동시에 두 번 왔다 — 먼저 끝난 것을 돌려준다
    select * into v_msg from public.crew_chat_messages m where m.author_id = v_me and m.client_id = p_client_id;
    return public.crew_chat_json(v_msg, v_me);
  end if;
  if p_image is not null then
    insert into public.crew_chat_images (message_id, data) values (v_msg.id, p_image);
  end if;
  -- 내가 보낸 것까지는 읽은 것
  insert into public.crew_chat_reads (crew_id, user_id, last_read_seq) values (p_crew, v_me, v_msg.seq)
  on conflict (crew_id, user_id) do update
    set last_read_seq = greatest(public.crew_chat_reads.last_read_seq, excluded.last_read_seq), updated_at = now();
  perform public.crew_chat_push(v_msg);
  return public.crew_chat_json(v_msg, v_me);
end;
$$;
revoke execute on function public.crew_chat_send(uuid, uuid, text, bigint, text) from public, anon;
grant execute on function public.crew_chat_send(uuid, uuid, text, bigint, text) to authenticated;
