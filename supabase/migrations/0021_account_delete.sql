-- 계정 삭제 — 앱 안에서 본인이 계정과 서버의 기록을 지운다.
--
-- Play 스토어는 계정을 만들 수 있는 앱에 "앱 안에서 계정 삭제"를 요구한다. 지우면
-- auth.users 한 줄이 사라지고, 그 사람을 가리키는 표(프로필·러닝·원장·글·댓글·
-- 코스·땅 표시·푸시 토큰…)는 전부 on delete cascade 로 함께 지워진다. 거래 기록처럼
-- 상대가 있는 줄은 on delete set null 로 상대 쪽 기록만 남는다.
--
-- 크루장이 지우면 크루가 통째로 사라지는 대신, 가장 오래된 크루원에게 크루장을
-- 넘긴다. 크루원이 없으면 크루도 함께 지워진다.

create or replace function public.account_delete()
returns void
language plpgsql
security definer
set search_path = public, auth
as $$
declare
  v_user uuid := auth.uid();
  r record;
  v_next uuid;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  for r in select c.id from public.crews c where c.owner_id = v_user loop
    select m.user_id into v_next
      from public.crew_members m
     where m.crew_id = r.id and m.user_id <> v_user
     order by m.joined_at, m.user_id
     limit 1;
    if v_next is not null then
      update public.crews set owner_id = v_next where id = r.id;
      update public.crew_members set role = 'OWNER' where crew_id = r.id and user_id = v_next;
    end if;
  end loop;

  delete from auth.users where id = v_user;
end;
$$;

comment on function public.account_delete is
  '본인 계정과 서버의 기록을 지운다. 크루장이면 가장 오래된 크루원에게 넘긴다.';

revoke execute on function public.account_delete() from public, anon;
grant execute on function public.account_delete() to authenticated;
