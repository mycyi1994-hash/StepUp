-- 0125 — 시험 계정 채우기 (2026-11-24)
--
-- 사용자 "테스트 버전은 등급별로 신발 다 있고 아이템 다 있고 적당한 레벨 가진 어드민 계정 · 구글 로그인 없이 바로 접속".
--   계정은 Supabase 대시보드 › Authentication › Users › Add user(이메일 · 비밀번호 · Auto Confirm)로 만들고,
--   앱(테스트 APK · debug 에만)의 "시험 계정으로 들어가기"에서 그 이메일 · 비밀번호로 들어간다(구글 없이 — 비밀번호는 앱에 넣지 않는다).
--   그 뒤 SQL 창에서 한 번:  select economy.test_account_seed('그 이메일');   — 다시 부르면 모자란 것만 다시 채운다.
-- 운영자 권한(운영자 페이지 · admin_*)은 주지 않는다 — 앱 안의 물건 · 레벨만. 매일 숫자(0124)에서는 뺀다(stats_exclude_emails 에 더한다).
-- 앱 역할은 부르지 못한다(SQL 창 · 운영자만).

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
