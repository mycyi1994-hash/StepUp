-- 감사 B: 번개 보너스는 "글 하나에 한 번"(0117 — 같은 사람의 그 글 러닝 가운데 party_size > 1 이 있으면 방을 지운다).
-- ACTIVE(0120)에서는 올릴 때 party_size 가 1 이고(원격 방 · 인원은 나중에 together_settle 이 적는다) — 먼저 올린 사람은 다른 사람이 올리기 전까지 1 이다.
-- u1 이 같은 번개 글로 방 A(u2 와) · 방 B(u3 와)를 차례로 달리고, 친구들보다 먼저 둘 다 올린다 → 두 방 모두 × 1.1
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select t.mk(1), t.mk(2), t.mk(3);
do $$
declare post bigint; pa bigint; pb bigint; va timestamptz; vb timestamptz; s record;
begin
  perform t.login(1);
  post := public.post_create('FLASH', null, '번개', '', '', 5, now() + interval '30 minutes', 5, null, null, gen_random_uuid(), 'REMOTE');
  perform t.login(2); perform public.join_flash(post);
  perform t.login(3); perform public.join_flash(post);
  -- 방 A: u1 + u2
  perform t.login(1); pa := public.party_open(null, post); perform public.party_ready(pa, true);
  perform t.login(2); perform public.party_open(null, post); perform public.party_ready(pa, true);
  perform t.login(1); perform public.party_start(pa);
  perform t.age(interval '4 seconds');
  perform t.age(interval '20 minutes');
  select starts_at into va from public.party_runs where party_id = pa and user_id = t.u(1);
  perform t.say('u1 방 A 올림', t.rec(1, va, 1190, 2400));
  perform t.login(1); perform public.party_finish(pa);
  -- 방 B: 같은 글 · u1 + u3
  perform t.login(1); pb := public.party_open(null, post); perform public.party_ready(pb, true);
  perform t.login(3); perform public.party_open(null, post); perform public.party_ready(pb, true);
  perform t.login(1); perform public.party_start(pb);
  perform t.age(interval '4 seconds');
  perform t.age(interval '20 minutes');
  select starts_at into vb from public.party_runs where party_id = pb and user_id = t.u(1);
  select starts_at into va from public.party_runs where party_id = pa and user_id = t.u(1);
  perform t.say('u1 방 B 올림 (방 ' || pb || ', 방 A ' || pa || ')', t.rec(1, vb, 1180, 2400));
  -- 친구들은 나중에
  perform t.say('u2 방 A 올림', t.rec(2, va, 1190, 2400));
  perform t.say('u3 방 B 올림', t.rec(3, vb, 1180, 2400));
  for s in select x.party_id, x.points_awarded, x.together_base, x.together_paid, x.together_n, x.party_size from public.walk_sessions x
            where x.user_id = t.u(1) order by x.started_at loop
    perform t.say('u1 러닝', format('방 %s · %s SUP (바탕 %s · 더한 몫 %s · %s명 · party_size %s)', s.party_id, s.points_awarded, s.together_base, s.together_paid, s.together_n, s.party_size));
  end loop;
  perform t.say('u1 함께 달린 보너스 줄', (select count(*) || '줄 · 합 ' || coalesce(sum(amount), 0) from public.sup_ledger l where l.user_id = t.u(1) and l.ref like 'together:%'));
end $$;
