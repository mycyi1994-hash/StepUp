-- 점검 2 (J): 같은 경로 검사(0118)는 md5 가 똑같을 때만 — 1cm 옮기거나 시각을 1ms 바꾸면 다른 계정이 같은 경로로 다 받는다.
-- 진짜 러닝 하나(60분 · 9.5km/h · 9,900보)를 넷이 올린다. 그리고 같은 사람의 어제 경로를 시각만 하루 밀어 다시.
\ir zz-audit2-J-lib.sql
select t.mk0(n) from generate_series(1, 5) n;
select t.old(n) from generate_series(1, 5) n;
do $$
declare v_start timestamptz := date_trunc('minute', now()) - interval '3 hours'; v_t text; w public.walk_sessions; n int;
begin
  v_t := t.trk(v_start, 3600);
  w := t.up(1, v_start, 3600, 9900, v_t);
  perform t.say('u1 진짜 러닝', t.line(w));
  w := t.up(2, v_start, 3600, 9900, v_t);
  perform t.say('u2 같은 글자 그대로', t.line(w));
  w := t.up(3, v_start, 3600, 9900, t.shift(v_t, 0.0000001, 0));
  perform t.say('u3 위도 +0.0000001(1.1cm)', t.line(w));
  w := t.up(4, v_start, 3600, 9900, t.shift(v_t, 0, 1));
  perform t.say('u4 시각 +1ms(자리는 똑같음)', t.line(w));
  w := t.up(5, v_start, 3600, 9900, t.jit(v_t, 1.5, 5));
  perform t.say('u5 점마다 ±1.5m 흔듦', t.line(w));
  -- u1 이 같은 경로를 시각만 하루 · 이틀 앞으로 밀어 "지난 러닝"으로
  w := t.up(1, v_start - interval '1 day', 3600, 9900, t.shift(v_t, 0, -86400000));
  perform t.say('u1 어제 날짜로 같은 경로', t.line(w));
  w := t.up(1, v_start - interval '2 days', 3600, 9900, t.shift(v_t, 0, -172800000));
  perform t.say('u1 그제 날짜로 같은 경로', t.line(w));
  for n in 1 .. 5 loop perform t.say('계정', t.me(n)); end loop;
end $$;
