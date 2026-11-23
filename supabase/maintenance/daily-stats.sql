-- 매일 이용자 · 활동 숫자 — .github/workflows/daily-stats.yml 이 읽기만 한다. 셈은 서버 0124 의 economy.daily_stats_report 하나
-- (Claude 대화로 받는 public.daily_stats() 도 같은 함수). psql 변수 exclude = 빼고 셀 이메일(비면 설정 stats_exclude_emails).
\set ON_ERROR_STOP on
\pset tuples_only on
\pset format unaligned
select economy.daily_stats_report(nullif(:'exclude', ''));
