-- ════════════════════════════════════════════════════════════════════
--  0084 — 프로필 이름을 서버에도 (2026-11-03)
--
--  베타 테스터 피드백 "개인 프로필 이름은 바꿨는데 크루장 이름은 그대로". 앱은 이름을 폰에만 적었고,
--  크루장 · 크루원 명단 · 대기실 진행자는 서버 profiles.display_name(가입 때의 구글 이름)을 보였다.
--  앱이 이름을 저장할 때(그리고 예전에 바꿔 둔 이름은 다음 동기화 때 한 번) 이 함수로 서버 이름도 바꾼다.
--  · 앞뒤 공백을 떼고 제어 문자를 빼고 16자로 자른다(앱 UserPrefs.NICKNAME_MAX 와 같다). 비면 '러너'.
-- ════════════════════════════════════════════════════════════════════

create or replace function public.profile_name_set(p_name text)
returns text
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := auth.uid();
  v_name text := left(btrim(regexp_replace(coalesce(p_name, ''), '[[:cntrl:]]', '', 'g')), 16);
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if v_name = '' then
    v_name := '러너';
  end if;
  update public.profiles set display_name = v_name where id = v_user;
  if not found then
    insert into public.profiles (id, display_name) values (v_user, v_name)
    on conflict (id) do update set display_name = excluded.display_name;
  end if;
  return v_name;
end $$;

revoke all on function public.profile_name_set(text) from public, anon;
grant execute on function public.profile_name_set(text) to authenticated;
