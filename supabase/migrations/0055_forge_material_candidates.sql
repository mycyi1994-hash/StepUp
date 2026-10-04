-- ════════════════════════════════════════════════════════════════════
--  0055 — 강화 재료 후보에 두 등급 이상 낮은 내 신발도 까닭(GRADE)과 함께 돌려준다
--
--  0054 의 forge_materials 는 두 등급 이상 낮은 신발을 후보 목록에서 아예 뺐다. 그러면 에픽 · 레전더리 강화에서
--  내 신발이 있는데도 재료 목록이 비어 보이고, 앱이 그리는 "두 등급 이상 낮아요" 까닭이 나오지 않는다.
--  대상 자신을 뺀 내 신발을 모두 돌려주고, 쓸 수 있는지는 forge_material_block 이 정한다(규칙 forge-v2 그대로 —
--  견적 · 강화는 여전히 두 등급 이상 낮은 재료를 받지 않는다).
-- ════════════════════════════════════════════════════════════════════

create or replace function public.forge_materials(p_target bigint)
returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  t public.market_sneakers;
  v_block text;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select * into t from public.market_sneakers where id = p_target;
  v_block := economy.forge_target_block(v_user, t);
  if v_block = 'TARGET_GONE' then
    return jsonb_build_object('target', jsonb_build_object('id', p_target, 'block', v_block), 'materials', '[]'::jsonb);
  end if;
  return jsonb_build_object(
    'target', economy.forge_target_json(t, t.level)
              || jsonb_build_object('base_permille', economy.forge_base_permille(t.level), 'block', v_block),
    'materials', coalesce((
      select jsonb_agg(jsonb_build_object(
               'id', c.id, 'rarity', c.rarity, 'level', c.level, 'model_id', c.model_id,
               'faction', c.faction, 'variant', c.variant, 'mint_number', c.mint_number,
               'bonus_permille', c.bonus, 'block', c.block)
             order by (c.block is null) desc, c.bonus desc, economy.rarity_ord(c.rarity), c.level desc, c.mint_number, c.id)
        from (select m.*, economy.forge_bonus_permille(t.rarity, m.rarity, m.level) as bonus,
                     economy.forge_material_block(v_user, m, t) as block
                from public.market_sneakers m
               where m.owner_id = v_user and m.id <> t.id) c
    ), '[]'::jsonb));
end $$;
revoke all on function public.forge_materials(bigint) from public, anon;
grant execute on function public.forge_materials(bigint) to authenticated;
