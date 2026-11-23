#!/usr/bin/env bash
# 실제 서버 규칙(supabase/setup.sql)으로 "하루 플레이"를 돌려, 버는 SUP 와 쓰는 SUP 가 맞는지 잰다.
#
# 로컬 Postgres 에 setup.sql 을 그대로 올리고, 앱이 부르는 함수(record_session · sneaker_repair · draw_free …)를
# 그대로 불러 등급 · 레벨별 하루 적립 · 수리비 · 남는 돈을 표로 낸다. Supabase 는 건드리지 않는다.
# 숫자를 바꾸기 전에 돌려 본다 — 2026-11-18 "걸으면 손해 · 강화할수록 손해"가 이 표로 드러났다
# (docs/점검-2026-11-18-경제-흐름.md).
#
#   tools/economy_sim/run.sh                                  # 지금 규칙
#   tools/economy_sim/run.sh tools/economy_sim/tweak-2026-11-18.sql   # 규칙을 바꿔서(그 파일의 SQL 을 먼저 실행)
#   ONLY=forge tools/economy_sim/run.sh                       # 강화 경험치 표만(forge.sql — forge_xp_model.py --check · check.py)
#
# 필요한 것: postgresql 16 (initdb/pg_ctl/psql) — supabase/tests/run.sh 와 같다.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PGBIN="${PGBIN:-/usr/lib/postgresql/16/bin}"
RUNDIR="${RUNDIR:-/var/tmp/stepup-econsim}"
PORT="${PGPORT:-55441}"
DB=stepup_economy_sim
TWEAK="${1:-}"

export PATH="$PGBIN:$PATH"

stage() {
  mkdir -p "$RUNDIR"
  cp "$ROOT/supabase/setup.sql" "$ROOT/supabase/tests/stub_auth.sql" "$RUNDIR/"
  cp "$ROOT/tools/economy_sim/day.sql" "$ROOT/tools/economy_sim/flows.sql" "$ROOT/tools/economy_sim/forge.sql" "$RUNDIR/"
  : > "$RUNDIR/tweak.sql"
  if [ -n "$TWEAK" ]; then cp "$TWEAK" "$RUNDIR/tweak.sql"; fi
}

# Postgres 는 root 로 돌지 않는다. root 라면 postgres 사용자로 다시 부른다.
if [ "$(id -u)" = "0" ]; then
  stage
  chown -R postgres "$RUNDIR"
  exec su postgres -c "RUNDIR='$RUNDIR' PGPORT='$PORT' PGBIN='$PGBIN' ONLY='${ONLY:-}' STANDALONE=1 bash '$ROOT/tools/economy_sim/run.sh'"
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
run() { psql -h "$RUNDIR" -p "$PORT" -U postgres -d "$DB" -v ON_ERROR_STOP=1 -q -P pager=off "$@"; }

run -f "$RUNDIR/stub_auth.sql"
run -f "$RUNDIR/setup.sql" >/dev/null
if [ -s "$RUNDIR/tweak.sql" ]; then
  echo "── 규칙 바꿈: ${TWEAK:-tweak.sql} ──"
  run -f "$RUNDIR/tweak.sql"
fi
echo "── 강화 경험치 표 ──"
run -f "$RUNDIR/forge.sql" 2>&1 | sed -E 's/^psql:[^ ]+ NOTICE: +//'
if [ "${ONLY:-}" = "forge" ]; then exit 0; fi
echo "── 흐름: 새 사용자 첫날 · 신발 바꿔 신기 · 부스터 ──"
run -f "$RUNDIR/flows.sql" 2>&1 | sed -E 's/^psql:[^ ]+ NOTICE: +//'
echo "── 하루 플레이: 에너지를 다 쓸 때까지 달리고, 닳으면 수리 ──"
run -f "$RUNDIR/day.sql"
