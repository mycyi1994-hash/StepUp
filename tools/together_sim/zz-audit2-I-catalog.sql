-- 2026-11-22 보안 점검(I): 카탈로그 검사 초안 — 새 표 · 함수가 문을 넓히면 FAIL 을 찍는다(NOTICE)
-- (1) public 표는 모두 RLS (2) SECURITY DEFINER 는 모두 search_path 고정 (3) 앱 역할이 바로 쓸 수 있는 표·명령은 아래 목록만
-- (4) economy · coaching 은 앱 역할에 USAGE 없음 · 그 안의 SECURITY DEFINER 를 PUBLIC 이 부를 수 없음(목록 밖)
-- (5) security_invoker 없는 뷰를 앱 역할이 읽을 수 없음(목록 밖)
do $$
declare r record; n int := 0;
  -- 지금 실제로 열린 직접 쓰기(권한 + 그 명령의 정책이 둘 다 있는 것)
  v_write_ok text[] := array['comments:DELETE','content_reports:INSERT','course_likes:DELETE','course_likes:INSERT',
    'courses:DELETE','crew_members:DELETE','crews:DELETE','post_likes:DELETE','post_likes:INSERT','posts:DELETE',
    'profiles:UPDATE','user_blocks:DELETE','user_blocks:INSERT','user_blocks:UPDATE'];
  v_secdef_public_ok text[] := array['earn_cap_factor','energy_max_for_level','forge_base_permille','free_repair_next',
    'free_repair_ready','points_per_step','repair_cost_per_unit','repair_unit_of'];
  v_definer_views_ok text[] := array['running_sources_public'];
begin
  for r in select c.relname from pg_class c join pg_namespace s on s.oid = c.relnamespace
            where s.nspname = 'public' and c.relkind in ('r', 'p') and not c.relrowsecurity loop
    raise notice 'FAIL catalog: public.% 에 RLS 가 없다', r.relname; n := n + 1;
  end loop;
  for r in select s.nspname, p.proname from pg_proc p join pg_namespace s on s.oid = p.pronamespace
            where s.nspname in ('public', 'economy', 'coaching') and p.prosecdef
              and not exists (select 1 from unnest(coalesce(p.proconfig, '{}')) c where c like 'search_path=%') loop
    raise notice 'FAIL catalog: %.% SECURITY DEFINER 에 search_path 가 없다', r.nspname, r.proname; n := n + 1;
  end loop;
  for r in
    with cmds(cmd, polcmd) as (values ('INSERT', 'a'), ('UPDATE', 'w'), ('DELETE', 'd')),
    w as (
      select c.relname || ':' || k.cmd as k
        from pg_class c join pg_namespace s on s.oid = c.relnamespace cross join cmds k cross join (values ('anon'), ('authenticated')) ro(role)
       where s.nspname = 'public' and c.relkind in ('r', 'p')
         and (has_table_privilege(ro.role, c.oid, k.cmd)
              or (k.cmd <> 'DELETE' and has_any_column_privilege(ro.role, c.oid, k.cmd)))
         and (not c.relrowsecurity or exists (
               select 1 from pg_policy p where p.polrelid = c.oid and p.polcmd in (k.polcmd, '*')
                  and (p.polroles = '{0}' or (select oid from pg_roles where rolname = ro.role) = any (p.polroles)))))
    select distinct k from w where k <> all (v_write_ok) loop
    raise notice 'FAIL catalog: 앱 역할이 바로 쓸 수 있다 — % (목록에 없다)', r.k; n := n + 1;
  end loop;
  for r in select s.nspname from pg_namespace s where s.nspname in ('economy', 'coaching')
              and (has_schema_privilege('anon', s.oid, 'USAGE') or has_schema_privilege('authenticated', s.oid, 'USAGE')) loop
    raise notice 'FAIL catalog: 앱 역할이 %스키마를 쓸 수 있다', r.nspname; n := n + 1;
  end loop;
  for r in select s.nspname, p.proname from pg_proc p join pg_namespace s on s.oid = p.pronamespace
            where s.nspname in ('economy', 'coaching') and p.prosecdef and p.prorettype <> 'trigger'::regtype
              and has_function_privilege('authenticated', p.oid, 'EXECUTE') and p.proname <> all (v_secdef_public_ok) loop
    raise notice 'FAIL catalog: %.% (SECURITY DEFINER) 를 PUBLIC 이 부를 수 있다', r.nspname, r.proname; n := n + 1;
  end loop;
  for r in select c.relname from pg_class c join pg_namespace s on s.oid = c.relnamespace
            where s.nspname = 'public' and c.relkind in ('v', 'm')
              and not coalesce('security_invoker=true' = any (c.reloptions), false)
              and (has_table_privilege('anon', c.oid, 'SELECT') or has_table_privilege('authenticated', c.oid, 'SELECT'))
              and c.relname <> all (v_definer_views_ok) loop
    raise notice 'FAIL catalog: 뷰 public.% 는 만든 사람 권한으로 돌고 앱 역할이 읽는다', r.relname; n := n + 1;
  end loop;
  raise notice '% catalog — 걸린 것 %', case when n = 0 then 'OK' else 'FAIL' end, n;
end $$;
