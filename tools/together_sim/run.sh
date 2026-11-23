#!/usr/bin/env bash
# 함께 뛰기(크루 러닝 · 번개러닝)를 실제 서버 규칙(supabase/setup.sql)으로 돌려 본다.
#
# 로컬 Postgres 에 setup.sql 을 그대로 올리고, 가짜 사용자 여러 명이 앱이 부르는 함수
# (party_create_v2 · party_join · party_ready · party_start_rev · crew_run_beat · record_session …)를
# 그대로 불러 장면마다 결과를 찍는다. Supabase 는 건드리지 않는다. 장면은 끝나면 되돌린다(서로 섞이지 않게).
# 시계를 돌릴 수 없어 기록을 뒤로 옮긴다(t.age) — supabase/tests/schema_test.sql 과 같은 방식.
#
#   tools/together_sim/run.sh                       # 모든 장면
#   tools/together_sim/run.sh 02-offline-both.sql   # 한 장면
#
# 2026-11-18 점검(docs/점검-2026-11-18-함께뛰기.md)이 이 장면들로 드러났다.
# 필요한 것: postgresql 16 (initdb/pg_ctl/psql) — supabase/tests/run.sh 와 같다.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
HERE="$ROOT/tools/together_sim"
PGBIN="${PGBIN:-/usr/lib/postgresql/16/bin}"
RUNDIR="${RUNDIR:-/var/tmp/stepup-togethersim}"
PORT="${PGPORT:-55442}"
DB=stepup_together_sim
ONE="${1:-}"

export PATH="$PGBIN:$PATH"

stage() {
  mkdir -p "$RUNDIR"
  cp "$ROOT/supabase/setup.sql" "$ROOT/supabase/tests/stub_auth.sql" "$HERE"/*.sql "$RUNDIR/"
}

# Postgres 는 root 로 돌지 않는다. root 라면 postgres 사용자로 다시 부른다.
if [ "$(id -u)" = "0" ]; then
  stage
  chown -R postgres "$RUNDIR"
  exec su postgres -c "RUNDIR='$RUNDIR' PGPORT='$PORT' PGBIN='$PGBIN' STANDALONE=1 bash '$HERE/run.sh' '$ONE'"
fi
if [ "${STANDALONE:-0}" != "1" ]; then stage; fi

if [ ! -s "$RUNDIR/data/PG_VERSION" ]; then
  initdb -D "$RUNDIR/data" -U postgres --auth=trust >/dev/null
fi
# 한국 시각 0 ~ 6시에는 검사용 Postgres 시계를 8시간 앞당겨 띄운다(libfaketime) — 검사들이 "몇 시간 전 러닝"을 오늘로 잡는데
# 그 시각엔 어제가 되어 실패했다(2026-11-24). 검사용 서버에만 — 운영 DB 와 배포는 건드리지 않는다. 라이브러리가 없으면 그대로 띄운다
stepup_pg_start() {
  local hour lib
  hour=$(TZ=Asia/Seoul date +%H)
  lib=$(ls /usr/lib/*/faketime/libfaketime.so.1 2>/dev/null | head -1)
  if [ "$((10#$hour))" -lt 6 ] && [ -n "$lib" ]; then
    echo "── 한국 시각 ${hour}시 — 검사용 Postgres 시계를 8시간 앞당겨 띄운다(libfaketime)" >&2
    LD_PRELOAD="$lib" FAKETIME="-8h" "$@"
  else
    if [ "$((10#$hour))" -lt 6 ]; then echo "── 한국 시각 ${hour}시 — libfaketime 이 없어 시계 그대로(0116 · 0118 절이 실패할 수 있다)" >&2; fi
    "$@"
  fi
}
if ! pg_isready -h "$RUNDIR" -p "$PORT" >/dev/null 2>&1; then
  stepup_pg_start pg_ctl -D "$RUNDIR/data" -l "$RUNDIR/pg.log" -o "-k $RUNDIR -h '' -p $PORT" -w start >/dev/null
fi
psql -h "$RUNDIR" -p "$PORT" -U postgres -d postgres -qc "drop database if exists $DB" 2>/dev/null
psql -h "$RUNDIR" -p "$PORT" -U postgres -d postgres -qc "create database $DB"
run() { psql -h "$RUNDIR" -p "$PORT" -U postgres -d "$DB" -q -P pager=off "$@"; }

run -v ON_ERROR_STOP=1 -f "$RUNDIR/stub_auth.sql"
run -v ON_ERROR_STOP=1 -f "$RUNDIR/setup.sql" >/dev/null
run -v ON_ERROR_STOP=1 -f "$RUNDIR/helpers.sql" >/dev/null 2>&1

for f in "$RUNDIR"/[0-9]*.sql; do
  name="$(basename "$f")"
  if [ -n "$ONE" ] && [ "$name" != "$(basename "$ONE")" ]; then continue; fi
  echo "── $name ──"
  { echo "begin;"; cat "$f"; echo "rollback;"; } > "$RUNDIR/_scene.sql"
  run -f "$RUNDIR/_scene.sql" 2>&1 | sed -E 's/^psql:[^ ]+:[0-9]+: //' | grep -E "^(NOTICE|ERROR|DETAIL)" | sed -E 's/^NOTICE: +//' || true
done
