-- 감사 E: 번개 "글 하나에 한 번"(0122 #8)이 보너스를 받지 못한 러닝까지 막는다.
-- 방 A 에서 5분만 달려(함께 달린 사람이 아님 — 보너스를 받을 수 없음) 올린 뒤, 같은 글로 방 B 를 열어 친구와 20분 —
-- 0122 는 together_base 가 있다는 것만으로 방 B 러닝을 방에서 떼어 낸다 → 나도 친구도 인원 보너스 없음.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select t.mk(1), t.mk(2), t.mk(3);
do $$
declare post bigint; pa bigint; pb bigint; va timestamptz; vb timestamptz; s1 public.walk_sessions; s3 public.walk_sessions; l1 numeric; l3 numeric;
begin
  perform t.login(1);
  post := public.post_create('FLASH', null, '번개', '', '', 5, now() + interval '30 minutes', 5, null, null, gen_random_uuid(), 'REMOTE');
  perform t.login(2); perform public.join_flash(post);
  perform t.login(3); perform public.join_flash(post);
  -- 방 A: 둘이 출발, u1 은 5분 만에 그만두고 올린다(문턱 10분 · 1,500보 아래), u2 는 달리지 않는다
  perform t.login(1); pa := public.party_open(null, post); perform public.party_ready(pa, true);
  perform t.login(2); perform public.party_open(null, post); perform public.party_ready(pa, true);
  perform t.login(1); perform public.party_start(pa);
  perform t.age(interval '5 minutes 10 seconds');
  select starts_at into va from public.party_runs where party_id = pa and user_id = t.u(1);
  raise notice 'room A %', t.rec(1, va, 300, 600);
  perform t.login(1); perform public.party_finish(pa);
  -- 방 B: 같은 글로 u1 · u3 가 20분
  perform t.login(1); pb := public.party_open(null, post); perform public.party_ready(pb, true);
  perform t.login(3); perform public.party_open(null, post); perform public.party_ready(pb, true);
  perform t.login(1); perform public.party_start(pb);
  perform t.age(interval '20 minutes 4 seconds');
  select starts_at into vb from public.party_runs where party_id = pb and user_id = t.u(1);
  raise notice 'room B %', t.rec(1, vb, 1180, 2400);
  raise notice 'room B %', t.rec(3, vb, 1180, 2400);
  select * into s1 from public.walk_sessions x where x.user_id = t.u(1) and x.started_at = vb;
  select * into s3 from public.walk_sessions x where x.user_id = t.u(3) and x.started_at = vb;
  select coalesce(sum(amount), 0) into l1 from public.sup_ledger where user_id = t.u(1) and ref like 'together:%';
  select coalesce(sum(amount), 0) into l3 from public.sup_ledger where user_id = t.u(3) and ref like 'together:%';
  raise notice 'RESULT room-A 러닝 qualifies=% · u1 방B party=% base=% 보너스 % · u3 n=% 보너스 % (0120: 둘 다 ×1.1)',
    (select economy.together_qualifies(x) from public.walk_sessions x where x.user_id = t.u(1) and x.started_at = va),
    s1.party_id, s1.together_base, l1, s3.together_n, l3;
end $$;
