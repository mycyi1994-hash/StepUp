-- ════════════════════════════════════════════════════════════════════
--  0102 — 11.11 워킹데이 당첨자 발표 (2026-11-11)
--
--  사용자 "11월 11일 이벤트 당첨자 발표해야함". 추첨은 사람이 한다(초대 순위 11명 + 추첨기의 빼빼로 95 · 무릎보호대 5).
--  검토 페이지 「당첨자」 탭에서 명단을 저장(walkingday_review_winners_set)하고 「페이지에 발표」(walkingday_review_winners_publish)를 누르면
--  이벤트 페이지가 가린 이메일로 보인다(walkingday_winners — 0096 리더보드와 같은 가림, 원문 · 번호는 나가지 않는다).
--  · 1인 1경품: 이메일이 표의 기본 키라 한 사람은 한 경품만.
--  · 경품마다 자리 수: 요가매트 세트 1 · 폼롤러 3 · 운동양말 7 · 빼빼로 95 · 무릎보호대 5(넘으면 저장 안 함, 모자라면 저장은 된다).
--  · 이벤트에 들어온 적 없는 이메일(사전 등록 · SNS 제출 · 번호 기록 어디에도 없음)은 오타로 보고 저장하지 않는다.
--  · 발표 뒤에도 고칠 수 있다(연락 안 되는 사람 바꾸기) — 페이지는 다음 읽기에 바뀐다.
--  내리기: update public.economy_settings set value = 'false'::jsonb where key = 'walkingday_winners_published';
-- ════════════════════════════════════════════════════════════════════
insert into public.economy_settings (key, value) values ('walkingday_winners_published', 'false'::jsonb)
on conflict (key) do nothing;

create table if not exists economy.walkingday_winners (
  email text primary key check (length(email) <= 254),
  prize text not null check (prize in ('mat', 'roller', 'socks', 'pepero', 'knee')),
  place int check (place between 1 and 200),
  saved_by text,
  saved_at timestamptz not null default now()
);
alter table economy.walkingday_winners enable row level security;
revoke all on economy.walkingday_winners from public;

-- 경품마다 자리 수(이벤트 페이지 경품 칸과 같다)
create or replace function economy.walkingday_prize_seats(p_prize text) returns int
language sql immutable set search_path = '' as $$
  select case p_prize when 'mat' then 1 when 'roller' then 3 when 'socks' then 7 when 'pepero' then 95 when 'knee' then 5 end
$$;
revoke all on function economy.walkingday_prize_seats(text) from public;

create or replace function economy.walkingday_winners_published() returns boolean
language sql stable security definer set search_path = ''
as $$
  select coalesce((select (value #>> '{}')::boolean from public.economy_settings where key = 'walkingday_winners_published'), false)
$$;
revoke all on function economy.walkingday_winners_published() from public;

-- ── 검토 페이지 ─────────────────────────────────────────────────────
-- 지금 저장된 명단(번호 함께 — 경품 보낼 때) · 발표 여부
create or replace function public.walkingday_review_winners(p_token text) returns jsonb
language plpgsql stable security definer set search_path = ''
as $$
begin
  perform economy.walkingday_reviewer(p_token);
  return jsonb_build_object(
    'published', economy.walkingday_winners_published(),
    'rows', coalesce((select jsonb_agg(jsonb_build_object('email', w.email, 'prize', w.prize, 'place', w.place,
                                                          'phone', economy.walkingday_phones(w.email),
                                                          'saved_by', w.saved_by, 'saved_at', w.saved_at)
                                       order by array_position(array['mat', 'roller', 'socks', 'pepero', 'knee'], w.prize), w.place nulls last, w.email)
                      from economy.walkingday_winners w), '[]'::jsonb));
end $$;
revoke all on function public.walkingday_review_winners(text) from public;
grant execute on function public.walkingday_review_winners(text) to anon, authenticated;

-- 명단 통째로 바꾸기. p_rows = [{email, prize, place?}, …]. 틀리면 아무것도 바꾸지 않고 까닭을 돌려준다
create or replace function public.walkingday_review_winners_set(p_token text, p_rows jsonb) returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_label text := economy.walkingday_reviewer(p_token);
  v_rows jsonb;
  v_bad text;
  v_prize text;
begin
  if jsonb_typeof(p_rows) is distinct from 'array' or jsonb_array_length(p_rows) > 111 then
    return jsonb_build_object('ok', false, 'code', 'rows');
  end if;
  -- 이메일은 소문자 · 앞뒤 공백 없이
  v_rows := coalesce((select jsonb_agg(jsonb_build_object(
              'email', lower(btrim(coalesce(r->>'email', ''))), 'prize', coalesce(r->>'prize', ''),
              'place', case when coalesce(r->>'place', '') ~ '^[0-9]{1,3}$' then (r->>'place')::int end))
            from jsonb_array_elements(p_rows) r), '[]'::jsonb);

  select i.prize into v_prize from jsonb_to_recordset(v_rows) as i(email text, prize text, place int)
   where economy.walkingday_prize_seats(i.prize) is null limit 1;
  if found then
    return jsonb_build_object('ok', false, 'code', 'prize', 'prize', v_prize);
  end if;
  select i.email into v_bad from jsonb_to_recordset(v_rows) as i(email text, prize text, place int)
   where i.email !~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$' or length(i.email) > 254 limit 1;
  if found then
    return jsonb_build_object('ok', false, 'code', 'email', 'email', v_bad);
  end if;
  -- 1인 1경품
  select i.email into v_bad from jsonb_to_recordset(v_rows) as i(email text, prize text, place int)
   group by i.email having count(*) > 1 limit 1;
  if found then
    return jsonb_build_object('ok', false, 'code', 'duplicate', 'email', v_bad,
      'prizes', (select jsonb_agg(i.prize) from jsonb_to_recordset(v_rows) as i(email text, prize text, place int) where i.email = v_bad));
  end if;
  select i.prize into v_prize from jsonb_to_recordset(v_rows) as i(email text, prize text, place int)
   group by i.prize having count(*) > economy.walkingday_prize_seats(i.prize) limit 1;
  if found then
    return jsonb_build_object('ok', false, 'code', 'seats', 'prize', v_prize, 'seats', economy.walkingday_prize_seats(v_prize));
  end if;
  -- 이벤트에 들어온 적 없는 이메일 = 오타
  select i.email into v_bad from jsonb_to_recordset(v_rows) as i(email text, prize text, place int)
   where not exists (select 1 from public.waitlist_entries w where w.email = i.email and w.status = 'registered')
     and not exists (select 1 from economy.walkingday_sns s where s.email = i.email)
     and not exists (select 1 from economy.walkingday_contacts c where c.email = i.email)
   limit 1;
  if found then
    return jsonb_build_object('ok', false, 'code', 'unknown', 'email', v_bad);
  end if;

  delete from economy.walkingday_winners where true;
  insert into economy.walkingday_winners (email, prize, place, saved_by)
  select i.email, i.prize, i.place, v_label from jsonb_to_recordset(v_rows) as i(email text, prize text, place int);
  return jsonb_build_object('ok', true, 'count', (select count(*) from economy.walkingday_winners));
end $$;
revoke all on function public.walkingday_review_winners_set(text, jsonb) from public;
grant execute on function public.walkingday_review_winners_set(text, jsonb) to anon, authenticated;

-- 페이지에 발표 · 내리기
create or replace function public.walkingday_review_winners_publish(p_token text, p_on boolean) returns jsonb
language plpgsql security definer set search_path = ''
as $$
begin
  perform economy.walkingday_reviewer(p_token);
  if coalesce(p_on, false) and not exists (select 1 from economy.walkingday_winners) then
    return jsonb_build_object('ok', false, 'code', 'empty');
  end if;
  insert into public.economy_settings (key, value) values ('walkingday_winners_published', to_jsonb(coalesce(p_on, false)))
  on conflict (key) do update set value = excluded.value;
  return jsonb_build_object('ok', true, 'published', coalesce(p_on, false));
end $$;
revoke all on function public.walkingday_review_winners_publish(text, boolean) from public;
grant execute on function public.walkingday_review_winners_publish(text, boolean) to anon, authenticated;

-- ── 이벤트 페이지(누구나) ───────────────────────────────────────────
-- 발표 전이면 {published:false}. 발표 뒤에는 경품마다 가린 이메일만
create or replace function public.walkingday_winners() returns jsonb
language sql stable security definer set search_path = ''
as $$
  select case when not economy.walkingday_winners_published() then jsonb_build_object('published', false)
    else jsonb_build_object('published', true, 'prizes', coalesce((
      select jsonb_object_agg(g.prize, g.who) from (
        select w.prize, jsonb_agg(economy.walkingday_mask_email(w.email) order by w.place nulls last, economy.walkingday_mask_email(w.email)) as who
        from economy.walkingday_winners w group by w.prize) g), '{}'::jsonb))
  end
$$;
revoke all on function public.walkingday_winners() from public;
grant execute on function public.walkingday_winners() to anon, authenticated;
