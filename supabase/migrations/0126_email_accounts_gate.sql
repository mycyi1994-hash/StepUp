-- 0126 — 이메일 계정은 시험 계정만 (2026-11-24 보안 점검)
--
-- 0125 의 시험 계정은 이메일 · 비밀번호로 들어간다. 그러려면 Supabase 의 이메일 로그인이 켜져 있어야 하고, 그러면 누구나
-- /auth/v1/signup 으로 이메일 계정을 만들 수 있다(구글 계정보다 싸게 부계정). 막는 곳은 서버 문지기(public.access_gate) —
-- 이메일 로그인(JWT app_metadata.provider = 'email') 계정은 economy.test_accounts 에 있을 때만 앱 요청이 지나간다(계정 삭제는 지나간다).
-- 목록은 economy.test_account_seed(이메일) 가 채운다(SQL 창 · 운영자만). 끄기: email_accounts_rule = 'ALL'.
-- public.access_gate 는 0126 이 마지막 정의다(0120 본문 + 이메일 계정 줄) — 다시 고칠 때 0126 을 복사한다.

create table if not exists economy.test_accounts (
  user_id uuid primary key references auth.users (id) on delete cascade,
  added_at timestamptz not null default now()
);
alter table economy.test_accounts enable row level security;
revoke all on economy.test_accounts from public, anon, authenticated;

insert into public.economy_settings (key, value) values ('email_accounts_rule', '"TEST_ONLY"') on conflict (key) do nothing;

create or replace function public.access_gate() returns void
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_block boolean := false;
  v_path text;
  v_uid uuid;
  v_min int;
  v_ver int;
  v_old boolean := false;
  v_email_blocked boolean := false;
begin
  -- 0120: 운영자가 정한 판(app_min_version_code, 스토어 번호)보다 낮은 앱은 막는다 — 앱은 요청마다 머리글 x-stepup-version 을 보낸다(없으면 0 = 예전 앱).
  -- 로그인한 앱 요청만(홈페이지 · 운영자 페이지는 anon). 계정 삭제 · 푸시 끊기는 막지 않는다. 고장 나도 앱 전체를 멈추지 않는다
  begin
    v_min := coalesce(economy.setting_num('app_min_version_code'), 0)::int;
    if v_min > 0 and coalesce(auth.jwt() ->> 'role', '') = 'authenticated'
       and coalesce(current_setting('request.path', true), '') !~ '/rpc/(account_delete|push_unregister|is_admin)$' then
      v_ver := coalesce(nullif(regexp_replace(coalesce(current_setting('request.headers', true)::json ->> 'x-stepup-version', ''), '[^0-9]', '', 'g'), '')::bigint % 1000, 0)::int;
      v_old := v_ver < v_min;
    end if;
  exception when others then
    v_old := false;
  end;
  if v_old then
    raise exception 'update_required' using errcode = 'PT426', detail = '새 판으로 업데이트해야 합니다', hint = 'update_required';
  end if;
  -- 0126: 이메일 · 비밀번호 계정은 시험 계정 목록(economy.test_accounts)에 있을 때만 — 시험 계정 때문에 이메일 로그인을 켜 두어도
  -- 누구나 API 로 이메일 가입해 부계정을 만들지 못하게(2026-11-24 보안 점검). 계정 삭제는 지나간다. 끄기: email_accounts_rule = 'ALL'
  begin
    if coalesce(auth.jwt() ->> 'role', '') = 'authenticated'
       and coalesce(auth.jwt() -> 'app_metadata' ->> 'provider', '') = 'email'
       and coalesce(economy.setting('email_accounts_rule') #>> '{}', 'TEST_ONLY') = 'TEST_ONLY'
       and coalesce(current_setting('request.path', true), '') !~ '/rpc/account_delete$'
       and not exists (select 1 from economy.test_accounts t where t.user_id = auth.uid()) then
      v_email_blocked := true;
    end if;
  exception when others then
    v_email_blocked := false;
  end;
  if v_email_blocked then
    raise exception 'access_required' using errcode = 'PT403', detail = '이 계정으로는 들어갈 수 없습니다', hint = 'email_not_allowed';
  end if;
  begin
    if economy.access_mode() <> 'OPEN' then
      v_uid := auth.uid();
      v_path := coalesce(current_setting('request.path', true), '');
      if v_uid is not null
         and coalesce(auth.jwt() ->> 'role', '') = 'authenticated'
         and v_path !~ '/rpc/(access_[a-z_]+|account_delete|is_admin)$'
         and not coalesce(economy.access_ok(v_uid), true) then
        v_block := true;
      end if;
    end if;
  exception when others then
    -- 문지기가 고장 나도 앱 전체를 멈추지 않는다
    v_block := false;
  end;
  if v_block then
    raise exception 'access_required' using errcode = 'PT403',
      detail = '접속 코드가 필요합니다', hint = 'access_required';
  end if;
end $$;
revoke all on function public.access_gate() from public;

create or replace function economy.test_account_seed(p_email text) returns text
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid;
  v_email text := lower(btrim(coalesce(p_email, '')));
  r record;
  v_id bigint;
  v_made int := 0;
  v_bal numeric;
  v_ex text;
begin
  select u.id into v_user from auth.users u where lower(u.email) = v_email;
  if v_user is null then
    raise exception '그 이메일의 계정이 없습니다 — 대시보드 › Authentication › Users › Add user 로 먼저 만드세요' using errcode = '22023';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform economy.seed_ensure(v_user);

  -- 이메일 계정이 들어갈 수 있는 목록(0126 문지기)
  insert into economy.test_accounts (user_id) values (v_user) on conflict do nothing;
  -- 들어가는 문: 운영자가 준 권한(접속 코드 없이)
  insert into economy.access_grants (user_id, source) values (v_user, 'ADMIN')
  on conflict (user_id) do update set source = 'ADMIN', revoked_at = null, revoked_reason = null;

  update public.profiles set display_name = '시험 러너'
   where id = v_user and coalesce(nullif(btrim(display_name), ''), '러너') = '러너';

  -- 등급마다 신발 한 켤레(가운데 값 — 첫 신발과 같은 셈) · 적당한 레벨
  for r in select * from (values ('COMMON', 10), ('RARE', 10), ('EPIC', 12), ('LEGENDARY', 13), ('REDLINE', 14), ('FINISH', 15)) t(rarity, lvl) loop
    if not exists (select 1 from public.market_sneakers s where s.owner_id = v_user and s.rarity = r.rarity and s.origin = 'PAID_DRAW') then
      insert into public.market_sneakers (
        owner_id, faction, rarity, variant, level, luck, comfort, durability,
        origin, efficiency_bps, comfort_bps, durability_pts, withdrawable, dur_base, model_id
      ) values (
        v_user, 'NONE', r.rarity, 0, r.lvl, 1, 1 + economy.stat_comfort(r.rarity, 0.5) / 10000.0, 100,
        'PAID_DRAW', economy.stat_efficiency(r.rarity, 0.5), economy.stat_comfort(r.rarity, 0.5), 100, false,
        economy.stat_durability(r.rarity, 0.5), economy.draw_model(r.rarity, (v_made * 7919) % 65536)
      ) returning id into v_id;
      v_made := v_made + 1;
    end if;
  end loop;
  -- 신은 신발이 없으면 에픽을 신는다
  if not exists (select 1 from public.market_sneakers where owner_id = v_user and equipped) then
    update public.market_sneakers set equipped = true
     where id = (select id from public.market_sneakers where owner_id = v_user and rarity = 'EPIC' order by id limit 1);
  end if;
  -- 내구도 가득(트리거가 dur_max 를 맞춘 뒤)
  update public.market_sneakers set durability = 100, durability_pts = 100 where owner_id = v_user;
  update public.market_sneakers set dur_now = dur_max where owner_id = v_user and dur_max is not null;

  -- 아이템 종류마다 10개
  insert into public.user_items (user_id, kind, qty)
  select v_user, k.kind, 10 from economy.item_kinds k
  on conflict (user_id, kind) do update set qty = greatest(public.user_items.qty, 10), updated_at = now();

  -- 뽑기: 무료 10 · 상급 선물 3 (남은 것이 그보다 적을 때만 채운다)
  insert into public.draw_grants as d (user_id, kind, granted) values (v_user, 'FREE', 10), (v_user, 'GIFT', 3)
  on conflict (user_id, kind) do update
    set granted = d.used + greatest(d.granted - d.used, excluded.granted);

  -- SUP 50,000 까지
  v_bal := economy.balance_of(v_user);
  if v_bal < 50000 then
    perform economy.ledger_apply(v_user, 'EARN_EVENT', 50000 - v_bal, '시험 계정 채움');
  end if;

  -- 러너 레벨 12(12만 걸음) — 레벨 상자는 이미 준 것으로(한꺼번에 쏟아지지 않게)
  insert into economy.runner_levels (user_id, steps, level, boxed_to) values (v_user, 110000, 12, 12)
  on conflict (user_id) do update set steps = greatest(economy.runner_levels.steps, 110000),
    level = greatest(economy.runner_levels.level, 12), boxed_to = greatest(economy.runner_levels.boxed_to, 12), updated_at = now();

  -- 매일 숫자에서 뺀다
  v_ex := coalesce(economy.setting('stats_exclude_emails') #>> '{}', '');
  if not (v_email = any (string_to_array(lower(replace(v_ex, ' ', '')), ','))) then
    insert into public.economy_settings (key, value) values ('stats_exclude_emails', to_jsonb(trim(both ',' from v_ex || ',' || v_email)))
    on conflict (key) do update set value = excluded.value, updated_at = now();
  end if;

  return format('시험 계정 %s — 새 신발 %s켤레(등급마다 하나) · 아이템 종류마다 10 · 무료 뽑기 10 · 상급 3 · SUP %s · 러너 Lv12',
                v_email, v_made, to_char(greatest(v_bal, 50000), 'FM999,999,990'));
end $$;
revoke all on function economy.test_account_seed(text) from public, anon, authenticated;
