-- 감사 A — 가입 선물(무료 뽑기 10, economy_bootstrap)은 FREE 줄이 없을 때만(on conflict do nothing).
-- 첫 신발을 받기 전에 러닝이 먼저 올라가면(record_session 이 허락하는 길 — "앱이 받기 전에 러닝이 먼저 올라가도 적립을 잃지 않게")
-- 러닝 상자(RUN2000 · 4000)와 러너 레벨 상자(0121)가 FREE 줄을 먼저 만들어 가입 선물 10 이 사라진다.
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
insert into auth.users (id, email) values (t.u(71), 'tg71@test.invalid'), (t.u(72), 'tg72@test.invalid') on conflict do nothing;
update public.profiles set created_at = now() where id in (t.u(71), t.u(72));  -- free_draw_since(0022 배포 시각) 뒤
do $$
declare v_start timestamptz := now() - interval '3 hours';
begin
  -- 71: 러닝 먼저(12,000보) → 그 뒤 첫 로그인 동기화(economy_bootstrap)
  perform t.say('u71 run before bootstrap', t.rec(71, v_start, 4500, 12000));
  perform t.login(71); perform public.economy_bootstrap();
  -- 72: 보통 차례(bootstrap → 같은 러닝)
  perform t.login(72); perform public.economy_bootstrap();
  perform t.say('u72 run after bootstrap', t.rec(72, v_start, 4500, 12000));
  perform t.say('u71 FREE granted (run first)', (select g.granted from public.draw_grants g where g.user_id = t.u(71) and g.kind = 'FREE'));
  perform t.say('u72 FREE granted (bootstrap first)', (select g.granted from public.draw_grants g where g.user_id = t.u(72) and g.kind = 'FREE'));
end $$;
