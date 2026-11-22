-- 감사 E: 미룬 정산(0122 #7, together_pending_run)이 together:<방> 자물쇠를 여러 개 차례로 잡는다 — 두 사람의 미룬 방 차례가 엇갈리면 교착.
-- 이 장면은 dblink 로 연결 셋을 쓰므로 begin … rollback 으로 감싸지 않고 돌린다(만든 줄은 끝에서 지운다).
--   A(901) 미룬 방: P 다음 Q · B(902) 미룬 방: Q 다음 P(둘 다 같은 두 방에서 정산이 미뤄진 적이 있다 — 여기서는 줄을 바로 넣는다)
--   연결 3 이 잠깐 P 를 쥔다 → A 는 P 에서 기다리고, B 는 Q 를 잡은 채 P 를 기다린다 → 연결 3 이 놓으면 A 가 P 를 잡고 Q 를 기다린다 → 교착
create extension if not exists dblink;
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
select t.mk(901), t.mk(902);
do $$
declare post bigint; p bigint; q bigint;
begin
  perform t.login(901);
  post := public.post_create('FLASH', null, '교착 P', '', '', 5, now() + interval '30 minutes', 5, null, null, gen_random_uuid(), 'REMOTE');
  p := public.party_open(null, post);
  post := public.post_create('FLASH', null, '교착 Q', '', '', 5, now() + interval '40 minutes', 5, null, null, gen_random_uuid(), 'REMOTE');
  q := public.party_open(null, post);
  insert into t.fix values ('P', p::text), ('Q', q::text) on conflict (k) do update set v = excluded.v;
  insert into economy.together_pending (party_id, user_id) values (p, t.u(901));
  insert into economy.together_pending (party_id, user_id) values (q, t.u(902));
  insert into economy.together_pending (party_id, user_id) values (q, t.u(901));
  insert into economy.together_pending (party_id, user_id) values (p, t.u(902));
end $$;
select dblink_connect('c1', 'host=/var/tmp/stepup-audit-E port=55471 dbname=stepup_together_sim user=postgres');
select dblink_connect('c2', 'host=/var/tmp/stepup-audit-E port=55471 dbname=stepup_together_sim user=postgres');
select dblink_connect('c3', 'host=/var/tmp/stepup-audit-E port=55471 dbname=stepup_together_sim user=postgres');
select dblink_exec('c3', 'begin');
select * from dblink('c3', format('select pg_advisory_xact_lock(hashtext(%L))::text', 'together:' || (select v from t.fix where k = 'P'))) as x(v text);
select dblink_exec('c1', 'begin');
select dblink_exec('c1', format('select set_config(''request.jwt.claim.sub'', %L, false)', t.u(901)));
select dblink_send_query('c1', 'select verdict from public.record_session(now() - interval ''2 hours'', now() - interval ''2 hours'' + interval ''600 seconds'', 1000, 600, '''')');
select pg_sleep(0.7);
select dblink_exec('c2', 'begin');
select dblink_exec('c2', format('select set_config(''request.jwt.claim.sub'', %L, false)', t.u(902)));
select dblink_send_query('c2', 'select verdict from public.record_session(now() - interval ''3 hours'', now() - interval ''3 hours'' + interval ''600 seconds'', 1000, 600, '''')');
select pg_sleep(0.7);
select dblink_exec('c3', 'rollback');
select 'c1' as conn, * from dblink_get_result('c1', false) as x(v text);
select 'c2' as conn, * from dblink_get_result('c2', false) as x(v text);
select dblink_get_result('c1', false) is null, dblink_get_result('c2', false) is null;
select dblink_exec('c1', 'rollback'), dblink_exec('c2', 'rollback');
select dblink_disconnect('c1'), dblink_disconnect('c2'), dblink_disconnect('c3');
-- 치우기
delete from economy.together_pending where user_id in (t.u(901), t.u(902));
delete from public.parties where id in (select v::bigint from t.fix where k in ('P', 'Q'));
delete from auth.users where id in (t.u(901), t.u(902));
