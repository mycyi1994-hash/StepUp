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
insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, verdict) values
  ('$COACH_U', timestamptz '2026-01-05 07:00+09', timestamptz '2026-01-05 07:30+09', 1458, 4252, 'CLEAN'),
  ('$COACH_U', timestamptz '2026-01-07 07:00+09', timestamptz '2026-01-07 07:30+09', 1458, 4252, 'CLEAN');
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

echo "── setup.sql 재적용 (기록이 쌓인 데이터베이스에서도 배포가 멈추지 않는가) ──"
run -f "$RUNDIR/setup.sql" >/dev/null
echo "OK"
