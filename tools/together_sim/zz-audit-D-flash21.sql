-- 감사 D(확인 — 문제 없음): together_settle(0120)은 party_size 에 함께 달린 수를 쓰는데 검사는 1 ~ 20 이다.
-- 번개 글의 정원은 200 까지지만 대기실은 20명에서 새 방으로 나뉘어(party_runs ≤ 20) 21 이 되지 않는다 — 20명 모두 올라간다.
--
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
select count(t.mk(i)) from generate_series(1, 23) i;
do $$
declare post bigint; p bigint; p2 bigint; i int; v_start timestamptz; r text;
begin
  perform t.login(1);
  post := public.post_create('FLASH', null, '큰 번개', '', '여의도', 5, now() + interval '10 minutes', 30, 37.52, 126.93, gen_random_uuid(), 'TOGETHER');
  for i in 2..23 loop perform t.login(i); perform public.join_flash(post); end loop;
  perform t.login(1); p := public.party_open(null, post); perform public.party_ready(p, true);
  for i in 2..23 loop perform t.login(i); p2 := public.party_open(null, post); perform public.party_ready(p2, true); end loop;
  perform t.login(1); perform public.party_start(p);
  perform t.say('출발한 사람', (select count(*) from public.party_runs where party_id = p));
  perform t.age(interval '20 minutes 4 seconds');
  select starts_at into v_start from public.party_runs where party_id = p and user_id = t.u(1);
  for i in 1..23 loop
    begin
      r := t.rec(i, v_start, 1200, 3200);
      if i >= 19 then perform t.say('upload u' || i, r); end if;
    exception when others then
      perform t.say('upload u' || i, 'ERROR ' || sqlstate || ' ' || sqlerrm);
    end;
  end loop;
  perform t.say('올라간 러닝', (select count(*) from public.walk_sessions where party_id = p));
end $$;
