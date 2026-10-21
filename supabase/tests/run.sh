#!/usr/bin/env bash
# 로컬 Postgres 에서 스키마 검사를 돌린다.
#
# Supabase 는 건드리지 않는다. 깨끗한 데이터베이스를 새로 만들고, setup.sql 을
# 그대로 올린 뒤, 앱과 같은 권한(authenticated)으로 규칙을 하나씩 두드려 본다.
#
#   supabase/tests/run.sh
#
# 필요한 것: postgresql 16 (initdb/pg_ctl/psql). 서버는 이 스크립트가 띄운다.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PGBIN="${PGBIN:-/usr/lib/postgresql/16/bin}"
RUNDIR="${RUNDIR:-/var/tmp/stepup-pgtest}"
PORT="${PGPORT:-55432}"
DB=stepup_schema_test

export PATH="$PGBIN:$PATH"

# Postgres 는 root 로 돌지 않는다. root 라면 postgres 사용자로 다시 부른다.
if [ "$(id -u)" = "0" ]; then
  mkdir -p "$RUNDIR"
  chown -R postgres "$RUNDIR"
  cp "$ROOT/supabase/setup.sql" "$RUNDIR/setup.sql"
  cp "$ROOT/supabase/tests/schema_test.sql" "$RUNDIR/schema_test.sql"
  cp "$ROOT/supabase/tests/stub_auth.sql" "$RUNDIR/stub_auth.sql"
  cp "$ROOT/supabase/tests/coaching_test.sql" "$RUNDIR/coaching_test.sql"
  chown postgres "$RUNDIR"/*.sql
  exec su postgres -c "RUNDIR='$RUNDIR' PGPORT='$PORT' PGBIN='$PGBIN' STANDALONE=1 bash '$ROOT/supabase/tests/run.sh'"
fi

if [ "${STANDALONE:-0}" != "1" ]; then
  mkdir -p "$RUNDIR"
  cp "$ROOT/supabase/setup.sql" "$RUNDIR/setup.sql"
  cp "$ROOT/supabase/tests/schema_test.sql" "$RUNDIR/schema_test.sql"
  cp "$ROOT/supabase/tests/stub_auth.sql" "$RUNDIR/stub_auth.sql"
  cp "$ROOT/supabase/tests/coaching_test.sql" "$RUNDIR/coaching_test.sql"
fi

if [ ! -s "$RUNDIR/data/PG_VERSION" ]; then
  initdb -D "$RUNDIR/data" -U postgres --auth=trust >/dev/null
fi

if ! pg_isready -h "$RUNDIR" -p "$PORT" >/dev/null 2>&1; then
  pg_ctl -D "$RUNDIR/data" -l "$RUNDIR/pg.log" \
    -o "-k $RUNDIR -h '' -p $PORT" -w start >/dev/null
fi

psql -h "$RUNDIR" -p "$PORT" -U postgres -d postgres -qc "drop database if exists $DB"
psql -h "$RUNDIR" -p "$PORT" -U postgres -d postgres -qc "create database $DB"

run() { psql -h "$RUNDIR" -p "$PORT" -U postgres -d "$DB" -v ON_ERROR_STOP=1 -q "$@"; }

run -f "$RUNDIR/stub_auth.sql"
echo "── setup.sql 적용 ───────────────────────────────────────────────"
run -f "$RUNDIR/setup.sql" >/dev/null
echo "── setup.sql 재적용 (다시 붙여넣어도 안전한가) ──────────────────"
run -f "$RUNDIR/setup.sql" >/dev/null
echo "OK"
run -f "$RUNDIR/schema_test.sql"
# AI 코칭(0060) — 동의 · 소유권 · 큐 · 한도 · 출력 검사(모의 공급자만)
run -f "$RUNDIR/coaching_test.sql"
# 코칭 워커 한 바퀴를 이 데이터베이스의 서버 함수에 붙여 본다(모의 공급자). node 가 없으면 못 본 것이라고 적는다
echo "── AI 코칭 워커 ↔ 서버 함수 ─────────────────────────────────────"
if command -v node >/dev/null 2>&1; then
  node "$ROOT/coach-worker/test/e2e-psql.mjs" "$RUNDIR" "$PORT" "$DB"
else
  echo "  못 봄 — node 가 없어 워커 연결 검사를 돌리지 않았다"
fi

# ── 크루장 일이 동시에 겹칠 때 ─────────────────────────────────────
# 한 세션으로는 겹침을 만들 수 없어 두 세션을 함께 띄운다. 앞 세션은 일을 마치고 1초 동안 커밋하지 않고
# 기다리고, 뒤 세션은 그 사이 같은 크루를 건드린다. 어느 쪽이 먼저 잡든 하나만 되어야 하고, 크루장 행은
# 크루 표의 크루장 한 사람(멤버)뿐이어야 한다.
echo "── 크루장 일이 동시에 겹칠 때 ─────────────────────────────────"
RACE_CREW=48000000-0000-0000-0000-0000000000cc
RACE_A=48000000-0000-0000-0000-00000000000a
RACE_B=48000000-0000-0000-0000-00000000000b
RACE_C=48000000-0000-0000-0000-00000000000c

# 크루장 가 · 멤버 나 · 다
race_setup() {
  run <<SQL
delete from public.crews where id = '$RACE_CREW';
insert into auth.users (id, email, raw_user_meta_data) values
  ('$RACE_A', 'race-a@test', '{"full_name":"가"}'),
  ('$RACE_B', 'race-b@test', '{"full_name":"나"}'),
  ('$RACE_C', 'race-c@test', '{"full_name":"다"}')
on conflict (id) do nothing;
insert into public.crews (id, owner_id, name) values ('$RACE_CREW', '$RACE_A', '동시에');
insert into public.crew_members (crew_id, user_id, role) values
  ('$RACE_CREW', '$RACE_B', 'MEMBER'), ('$RACE_CREW', '$RACE_C', 'MEMBER');
SQL
}

# $1 로그인한 사람, $2 부를 SQL — 끝나고 1초 동안 커밋하지 않는다
race_as() {
  run -c "set role authenticated" \
      -c "select set_config('request.jwt.claim.sub', '$1', false)" \
      -c "begin" -c "$2" -c "select pg_sleep(1)" -c "commit" >/dev/null 2>&1
}

# 두 일을 겹쳐 돌리고 된 일의 수를 찍는다
race() {
  local done=0 first second
  race_as "$1" "$2" & first=$!
  sleep 0.3
  race_as "$3" "$4" & second=$!
  wait "$first" && done=$((done + 1))
  wait "$second" && done=$((done + 1))
  echo "$done"
}

# $1 설명, $2 된 일의 수
race_check() {
  run <<SQL
do \$\$
declare v_owner uuid; v_owners int; v_member boolean;
begin
  select owner_id into v_owner from public.crews where id = '$RACE_CREW';
  select count(*) into v_owners from public.crew_members where crew_id = '$RACE_CREW' and role = 'OWNER';
  v_member := exists (select 1 from public.crew_members
                       where crew_id = '$RACE_CREW' and user_id = v_owner and role = 'OWNER');
  if $2 <> 1 or v_owners <> 1 or not v_member then
    raise exception 'FAIL  % — 된 일 %개 · 크루장 행 %개 · 크루장이 멤버인가 %', '$1', $2, v_owners, v_member;
  end if;
  raise notice '  OK   %', '$1';
end \$\$;
SQL
}

race_setup
race_check "같은 크루를 동시에 두 번 넘겨도 하나만 되고 크루장은 한 사람이다" "$(race \
  "$RACE_A" "select public.crew_transfer_owner('$RACE_CREW', '$RACE_C')" \
  "$RACE_A" "select public.crew_transfer_owner('$RACE_CREW', '$RACE_B')")"
race_setup
race_check "넘겨받는 사람이 그 사이 나가려 해도 크루장은 멤버로 남는다" "$(race \
  "$RACE_A" "select public.crew_transfer_owner('$RACE_CREW', '$RACE_B')" \
  "$RACE_B" "select public.crew_leave('$RACE_CREW')")"
race_setup
race_check "내보내는 중인 멤버에게는 넘어가지 않는다" "$(race \
  "$RACE_A" "select public.crew_member_remove('$RACE_CREW', '$RACE_B')" \
  "$RACE_A" "select public.crew_transfer_owner('$RACE_CREW', '$RACE_B')")"
run -c "delete from public.crews where id = '$RACE_CREW'"

# ── AI 코칭 — 같은 요청이 두 기기에서 동시에 ─────────────────────────
# 같은 러닝의 분석을 동시에 두 번 요청해도 작업은 하나, 한도가 1 이면 서로 다른 두 요청 중 하나만 만들어진다.
echo "── AI 코칭 요청이 동시에 겹칠 때 ───────────────────────────────"
COACH_U=59100000-0000-0000-0000-00000000000c
run <<SQL
insert into auth.users (id, email) values ('$COACH_U', 'coach-race@test') on conflict (id) do nothing;
-- 코칭 거리는 러닝의 인정 거리(distance_meters, 0068) — 걸음 거리(4252 × 0.762m)로 넣는다
insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, distance_meters, verdict) values
  ('$COACH_U', timestamptz '2026-01-05 07:00+09', timestamptz '2026-01-05 07:30+09', 1458, 4252, 3240.024, 'CLEAN'),
  ('$COACH_U', timestamptz '2026-01-07 07:00+09', timestamptz '2026-01-07 07:30+09', 1458, 4252, 3240.024, 'CLEAN');
insert into public.coaching_consents (user_id, version) values ('$COACH_U', 'draft-2026-10-16');
insert into public.coaching_run_splits (session_id, user_id, laps)
  select id, user_id, array[432, 878, 1350] from public.walk_sessions where user_id = '$COACH_U';
update public.economy_settings set value = 'true'::jsonb where key = 'coaching_enabled';
update public.economy_settings set value = '"mock"'::jsonb where key = 'coaching_provider';
update public.economy_settings set value = '5'::jsonb where key = 'coaching_user_daily_limit';
SQL

# $1 설명, $2 기대한 작업 수
coach_check() {
  run <<SQL
do \$\$
declare v_jobs int;
begin
  select count(*) into v_jobs from public.coaching_jobs where user_id = '$COACH_U';
  if v_jobs <> $2 then
    raise exception 'FAIL  % — 작업 %개(기대 %개)', '$1', v_jobs, $2;
  end if;
  raise notice '  OK   %', '$1';
end \$\$;
SQL
}

race "$COACH_U" "select public.coaching_run(timestamptz '2026-01-05 07:00+09', 'ko', true)" \
     "$COACH_U" "select public.coaching_run(timestamptz '2026-01-05 07:00+09', 'ko', true)" >/dev/null
coach_check "같은 러닝을 두 기기에서 동시에 요청해도 작업은 하나다" 1
run -c "delete from public.coaching_jobs where user_id = '$COACH_U'" \
    -c "update public.economy_settings set value = '1'::jsonb where key = 'coaching_user_daily_limit'"
race "$COACH_U" "select public.coaching_run(timestamptz '2026-01-05 07:00+09', 'ko', true)" \
     "$COACH_U" "select public.coaching_run(timestamptz '2026-01-07 07:00+09', 'ko', true)" >/dev/null
coach_check "하루 한도가 1 이면 동시에 온 서로 다른 두 요청 중 하나만 만든다" 1
run -c "delete from auth.users where id = '$COACH_U'" \
    -c "update public.economy_settings set value = 'false'::jsonb where key = 'coaching_enabled'" \
    -c "update public.economy_settings set value = '\"none\"'::jsonb where key = 'coaching_provider'" \
    -c "update public.economy_settings set value = 'null'::jsonb where key = 'coaching_user_daily_limit'"

# ── 크루 체력(0069) — 같은 크루의 하루를 두 사람이 동시에 처음 열 때 ─────────────────
# 크루 체력은 그날 처음 읽거나 쓸 때 한 번 정한다. 둘이 동시에 처음 열어도 둘 다 되고 그날 줄은 하나여야 한다
# (잠금이 없으면 뒤에 온 쪽이 같은 줄을 넣다 실패한다 — 러닝 올리기가 실패한다).
echo "── 크루 체력을 동시에 처음 열 때(0069) ──────────────────────────"
STAMINA_RACE_CREW=69200000-0000-0000-0000-0000000000cc
STAMINA_RACE_A=69200000-0000-0000-0000-00000000000a
STAMINA_RACE_B=69200000-0000-0000-0000-00000000000b
run <<SQL
insert into auth.users (id, email) values ('$STAMINA_RACE_A', 'stamina-a@test'), ('$STAMINA_RACE_B', 'stamina-b@test')
on conflict (id) do nothing;
insert into public.crews (id, owner_id, name) values ('$STAMINA_RACE_CREW', '$STAMINA_RACE_A', '동시 체력');
update public.crew_members set joined_at = now() - interval '30 days' where crew_id = '$STAMINA_RACE_CREW';
insert into public.crew_members (crew_id, user_id, role, joined_at)
values ('$STAMINA_RACE_CREW', '$STAMINA_RACE_B', 'MEMBER', now() - interval '30 days');
insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, distance_meters, verdict) values
  ('$STAMINA_RACE_A', now() - interval '3 days', now() - interval '3 days' + interval '20 minutes', 1200, 2000, 1500, 'CLEAN'),
  ('$STAMINA_RACE_B', now() - interval '3 days', now() - interval '3 days' + interval '20 minutes', 1200, 2000, 1500, 'CLEAN');
SQL
STAMINA_DONE=$(race "$STAMINA_RACE_A" "select public.crew_stamina('$STAMINA_RACE_CREW')" \
                    "$STAMINA_RACE_B" "select public.crew_stamina('$STAMINA_RACE_CREW')")
run <<SQL
do \$\$
declare v_days int; v_members int;
begin
  select count(*) into v_days from economy.crew_stamina_days where crew_id = '$STAMINA_RACE_CREW';
  select count(*) into v_members from economy.crew_stamina_members where crew_id = '$STAMINA_RACE_CREW';
  if $STAMINA_DONE <> 2 or v_days <> 1 or v_members <> 2 then
    raise exception 'FAIL  동시에 처음 열기 — 된 일 %개 · 그날 줄 %개 · 크루원 줄 %개', $STAMINA_DONE, v_days, v_members;
  end if;
  raise notice '  OK   %', '같은 크루의 하루를 두 사람이 동시에 처음 열어도 둘 다 되고 크루 체력은 한 번만 정한다';
end \$\$;
SQL
run -c "delete from public.crews where id = '$STAMINA_RACE_CREW'" \
    -c "delete from auth.users where id in ('$STAMINA_RACE_A', '$STAMINA_RACE_B')"

# ── 크루 러닝 거리 채우기(0069) — 다시 붙여넣어도 이미 센 크루는 그대로, 아직 안 센 크루만 지금까지의 크루 러닝으로 ──
# 위 검사가 크루 러닝으로 쌓은 크루(체력 크루)는 그대로여야 하고, 거리가 0 인 크루는 크루에 묶인 2명 이상 러닝
# (무효 · 보류 · 혼자 빼고)으로 채운다. 아래 재적용이 이 채우기를 다시 돈다.
BACKFILL_CREW=69300000-0000-0000-0000-0000000000cc
BACKFILL_U=69300000-0000-0000-0000-00000000000a
BACKFILL_V=69300000-0000-0000-0000-00000000000b
STAMINA_CREW=69000000-0000-0000-0000-0000000000cc
run <<SQL
insert into auth.users (id, email) values ('$BACKFILL_U', 'backfill-u@test'), ('$BACKFILL_V', 'backfill-v@test')
on conflict (id) do nothing;
insert into public.crews (id, owner_id, name) values ('$BACKFILL_CREW', '$BACKFILL_U', '채우기');
insert into public.crew_members (crew_id, user_id, role) values ('$BACKFILL_CREW', '$BACKFILL_V', 'MEMBER');
insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, distance_meters, verdict,
                                  verdict_reason, crew_id, party_size) values
  ('$BACKFILL_U', timestamptz '2026-01-10 07:00+09', timestamptz '2026-01-10 08:00+09', 3600, 9000, 20000, 'CLEAN', '', '$BACKFILL_CREW', 2),
  ('$BACKFILL_V', timestamptz '2026-01-10 07:00+09', timestamptz '2026-01-10 08:00+09', 3600, 9000, 11000, 'FLAGGED',
   '하루 적립 상한에 걸렸습니다', '$BACKFILL_CREW', 3),
  ('$BACKFILL_U', timestamptz '2026-01-11 07:00+09', timestamptz '2026-01-11 08:00+09', 3600, 9000, 5000, 'CLEAN', '', '$BACKFILL_CREW', 1),
  ('$BACKFILL_V', timestamptz '2026-01-11 07:00+09', timestamptz '2026-01-11 08:00+09', 3600, 9000, 5000, 'VOID',
   '가짜 위치가 감지되었습니다', '$BACKFILL_CREW', 2),
  ('$BACKFILL_U', timestamptz '2026-01-12 07:00+09', timestamptz '2026-01-12 08:00+09', 3600, 9000, 5000, 'FLAGGED',
   '최근 무효 러닝이 많아 적립을 보류합니다', '$BACKFILL_CREW', 2);
SQL
STAMINA_KM_BEFORE=$(run -At -c "select run_km from public.crews where id = '$STAMINA_CREW'")

echo "── setup.sql 재적용 (기록이 쌓인 데이터베이스에서도 배포가 멈추지 않는가) ──"
run -f "$RUNDIR/setup.sql" >/dev/null
echo "OK"

run <<SQL
do \$\$
declare b public.crews; s public.crews;
begin
  select * into b from public.crews where id = '$BACKFILL_CREW';
  select * into s from public.crews where id = '$STAMINA_CREW';
  if b.run_km <> 31 or b.level <> 2 then
    raise exception 'FAIL  크루 러닝 거리 채우기 — %km · 레벨 %', b.run_km, b.level;
  end if;
  raise notice '  OK   %', '거리가 0 인 크루는 지금까지의 크루 러닝(2명 이상 · 무효 · 보류 · 혼자 빼고)으로 채운다 — 31km · 레벨 2 (0069)';
  if s.run_km <> $STAMINA_KM_BEFORE then
    raise exception 'FAIL  다시 붙여넣기 — 이미 센 크루의 거리가 %km 에서 %km 로 바뀌었다', $STAMINA_KM_BEFORE, s.run_km;
  end if;
  raise notice '  OK   %', '다시 붙여넣어도 이미 센 크루의 거리는 그대로다 — 두 번 더하지 않는다';
end \$\$;
SQL
