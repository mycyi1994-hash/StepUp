-- ════════════════════════════════════════════════════════════════════
--  0086 — 크루 가입 알림 다듬기 (2026-11-03)
--
--  · 신청한 사람에게 결과 푸시(CREW_DECIDED) — 크루장이 승인 · 미승인하면 알린다(예전엔 앱을 다시 열어야 알았다).
--    값: crew(크루 이름) · approved('1' 승인 / '0' 미승인). 누르면 그 크루(crew/<id>) — 결과 줄은 크루 목록 위에 그대로 있다.
--    글은 받는 사람의 언어로 push-send 가 짓는다. 알림 설정의 푸시를 끈 사람 · 받을 폰이 없는 사람에게는 적지 않는다(push_enqueue).
--  · 크루장이 받는 가입 신청 푸시(CREW_REQUEST)는 크루 상세 대신 그 크루의 신청 목록을 연다(crew/<id>/requests).
--    예전 앱은 이 링크를 모르면 앱만 연다. 예전 링크(crew/<id>)는 앱이 그대로 읽는다.
--  가입 · 정원 · 신청서 규칙은 0056 그대로다 — 푸시 한 줄만 더했다.
-- ════════════════════════════════════════════════════════════════════

-- 가입 신청이 들어오면 크루장에게 — 0047 과 같고 링크만 신청 목록으로
create or replace function public.push_on_crew_request()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_owner uuid;
  v_name text;
begin
  if new.status <> 'PENDING' then
    return new;
  end if;
  select c.owner_id, c.name into v_owner, v_name from public.crews c where c.id = new.crew_id;
  if v_owner is not null and v_owner <> new.user_id then
    perform public.push_enqueue(
      v_owner, 'CREW_REQUEST',
      jsonb_build_object('name', public.push_display_name(new.user_id), 'crew', coalesce(v_name, '')),
      'crew/' || new.crew_id::text || '/requests');
  end if;
  return new;
end;
$$;

-- 승인 · 미승인 — 0056 과 같고, 정한 뒤 신청한 사람에게 결과 푸시
create or replace function public.crew_application_decide(p_application bigint, p_approve boolean)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_crew_id uuid;
  v_crew public.crews%rowtype;
  v_app public.crew_applications%rowtype;
  v_count int;
begin
  select a.crew_id into v_crew_id from public.crew_applications a where a.id = p_application;
  if not found then
    raise exception 'application_missing' using errcode = '22023', detail = '신청서를 찾을 수 없습니다';
  end if;
  perform public.crew_lock_owner(v_crew_id);
  select * into v_crew from public.crews c where c.id = v_crew_id;
  select * into v_app from public.crew_applications a where a.id = p_application for update;
  if v_app.status <> 'PENDING' then
    raise exception 'application_decided' using errcode = '22023', detail = v_app.status;
  end if;

  select count(*) into v_count from public.crew_members m where m.crew_id = v_crew_id;
  if coalesce(p_approve, false) then
    if v_crew.capacity is not null and v_count >= v_crew.capacity then
      raise exception 'crew_full' using errcode = '22023', detail = '정원이 가득 찼습니다';
    end if;
    insert into public.crew_members (crew_id, user_id, role) values (v_crew_id, v_app.user_id, 'MEMBER')
    on conflict do nothing;
    update public.crew_applications set status = 'APPROVED', decided_at = now() where id = p_application;
  else
    update public.crew_applications set status = 'DECLINED', decided_at = now() where id = p_application;
  end if;

  -- 0086 — 신청한 사람에게 결과를 알린다(받을 폰이 없거나 푸시를 껐으면 push_enqueue 가 건너뛴다)
  perform public.push_enqueue(
    v_app.user_id, 'CREW_DECIDED',
    jsonb_build_object('crew', coalesce(v_crew.name, ''),
                       'approved', case when coalesce(p_approve, false) then '1' else '0' end),
    'crew/' || v_crew_id::text);

  return jsonb_build_object(
    'status', case when coalesce(p_approve, false) then 'APPROVED' else 'DECLINED' end,
    'member_count', (select count(*) from public.crew_members m where m.crew_id = v_crew_id),
    'capacity', v_crew.capacity,
    'pending_count', (select count(*) from public.crew_applications a where a.crew_id = v_crew_id and a.status = 'PENDING'));
end;
$$;
