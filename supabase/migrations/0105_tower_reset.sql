-- ════════════════════════════════════════════════════════════════════
--  0105 — StepUP 타워 시험 기록 초기화 (2026-11-13)
--
--  사용자 "도전 초기화 좀 해줘"(내부 시험 중). 한 번만: 그동안의 판(오늘 쓴 기회 · 점수 · 순위)과 SNS 기회를 비운다.
--  참가 이름(economy.tower_players)은 남긴다. 표시 tower_reset_20261113 — setup.sql 은 배포마다 다시 올라가므로 두 번째부터는 지우지 않는다.
--  다음에 또 비우려면(SQL Editor):
--    delete from economy.tower_sns; delete from economy.tower_runs;                       -- 모두
--    delete from economy.tower_runs where day = economy.walkingday_today();               -- 오늘 기회만
-- ════════════════════════════════════════════════════════════════════
do $$
begin
  if not exists (select 1 from public.economy_settings where key = 'tower_reset_20261113') then
    delete from economy.tower_sns;
    delete from economy.tower_runs;
    insert into public.economy_settings (key, value) values ('tower_reset_20261113', to_jsonb(now()::text));
  end if;
end $$;
