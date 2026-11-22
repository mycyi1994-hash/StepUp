-- 감사 A: 바탕 값 살피기
select t.say('today', economy.game_day(now()));
select t.say('isodow', extract(isodow from economy.game_day(now())));
select t.say('settings', (select jsonb_object_agg(key, value) from public.economy_settings where key in
  ('points_per_step','daily_max_rule','daily_max_hour_mult','daily_max_hour_steps','box_price','premium_box_price','box_daily_buy','premium_box_weekly_buy',
   'daily_free_draws','run_box_steps','weekly_box_steps','streak_box_days','free_repair_hours','gps_km_daily_cap','max_daily_steps','invite_enabled',
   'forge_xp_step','forge_xp_need','forge_xp_crit_chance','forge_xp_crit_mult','forge_xp_sup_per_unit','forge_xp_spec','runner_level_rule','new_account_days',
   'worn_rate','no_gps_daily_cap','upload_daily_cap','daily_earn_cap','free_shoe_lock_km','crew_bonus_rule','party_bonus_rule','reward_steps_rule','step_gps_max_ratio','gps_check_min_m')));
select t.say('max_daily_steps', economy.max_daily_steps());
select t.say('steps_per_energy', economy.steps_per_energy());
select t.say('base_max_energy', economy.base_max_energy());
