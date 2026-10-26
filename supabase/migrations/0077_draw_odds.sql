-- ════════════════════════════════════════════════════════════════════
--  0077 — 뽑기 확률 읽기: 앱 뽑기 화면의 확률 표 (2026-10-26 플레이 출시 최적화)
--
--  - 뽑기 확률은 이용약관(web/terms.html)에만 있었다. Play 정책은 무작위 아이템의 확률을 앱 안에서도 미리 보여 주기를 권하고
--    (유료면 요구), 앱 숫자는 서버 값만 보인다는 규칙이 있어 확률도 서버가 준다 — 뽑기가 실제로 쓰는 표(economy.draw_weight_v3, 0066) 그대로.
--  - 읽기만 한다. 로그인 전에도 볼 수 있다(공개 정보).
-- ════════════════════════════════════════════════════════════════════

create or replace function public.draw_odds()
returns jsonb
language sql
stable
security definer
set search_path = public, economy
as $$
  select jsonb_build_object('pools', coalesce(jsonb_agg(jsonb_build_object(
           'pool', p.pool,
           'odds', (select coalesce(jsonb_agg(jsonb_build_object('rarity', r.rarity, 'permille', economy.draw_weight_v3(p.pool, r.rarity))
                                              order by r.ord), '[]'::jsonb)
                      from unnest(array['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH']) with ordinality as r(rarity, ord)
                     where economy.draw_weight_v3(p.pool, r.rarity) > 0))
         order by p.ord), '[]'::jsonb))
    from unnest(array['FREE', 'PREMIUM']) with ordinality as p(pool, ord)
$$;
revoke all on function public.draw_odds() from public;
grant execute on function public.draw_odds() to anon, authenticated;
