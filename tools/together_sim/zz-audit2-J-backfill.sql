-- 점검 2 (J): 오늘 가입한 계정이 가입 전 6일 + 오늘을 "지난 러닝"으로 채운다(시작 시각은 7일 안이면 받는다 · 가입 시각과 맞대지 않는다).
-- 날마다 201분 · 48,000보(케이던스 238.8) · 걷는 빠르기(1.75 m/s) 경로 — 걸음 : GPS 비율 2 안. 견줄 것: 같은 날 가입한 정직한 사람의 60분 러닝 하나.
\ir zz-audit2-J-lib.sql
select t.mk0(31, now());
select t.mk0(32, now());
do $$
declare v_now timestamptz := date_trunc('minute', now()); v_start timestamptz; w public.walk_sessions; k int;
begin
  -- 정직: 오늘 60분 · 9,900보
  v_start := v_now - interval '90 minutes';
  w := t.up(32, v_start, 3600, 9900, t.trk(v_start, 3600));
  perform t.say('u32 정직 · 오늘 60분', t.line(w));
  perform t.say('정직 계정', t.me(32));
  -- 부정: 6일 전부터 오늘까지(가입은 오늘)
  for k in reverse 6 .. 0 loop
    v_start := v_now - interval '205 minutes' - make_interval(days => k);
    w := t.up(31, v_start, 12060, 48000, t.trk(v_start, 12060, 1.75));
    perform t.say(format('u31 %s일 전(%s KST)', k, to_char(v_start at time zone 'Asia/Seoul', 'MM-DD HH24:MI')), t.line(w));
  end loop;
  perform t.say('부정 계정', t.me(31));
  perform t.say('가입 시각', (select to_char(created_at at time zone 'Asia/Seoul', 'MM-DD HH24:MI') from public.profiles where id = t.u(31)));
  perform t.say('가입 전 러닝 수', (select count(*) from public.walk_sessions s join public.profiles p on p.id = s.user_id
                                      where s.user_id = t.u(31) and s.started_at < p.created_at and s.verdict <> 'VOID'));
  perform t.say('상자 원장', (select string_agg(a.source || '/' || a.kind, ' ' order by a.granted_at, a.source)
                               from economy.box_awards a where a.user_id = t.u(31)));
end $$;
