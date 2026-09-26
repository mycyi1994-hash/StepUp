-- ════════════════════════════════════════════════════════════════════
--  0036 — 친구 초대 (S2 시안 28, 2026-09-26)
--
--  · 사람마다 초대 코드 하나(STEP-XXXXXX). 서버가 만든다.
--  · 새로 가입한 사람은 가입 7일 안에 코드 하나를 입력할 수 있다(한 번만, 내 코드 · 서로 맞초대 불가).
--  · 초대받은 사람이 무효가 아닌 러닝(기본 1,000걸음 이상)을 처음 마치면 두 사람에게 한 번씩 적립한다.
--  · 적립액은 economy.invite_config.reward_sup 하나로 정한다. **기본 0 — 0이면 아무것도 적립하지 않고
--    앱도 보상 문구를 보이지 않는다.** 금액은 운영자가 정해 이렇게 바꾼다:
--        update economy.invite_config set reward_sup = 5;
--  · 한 사람이 초대로 받는 적립은 max_rewards_per_inviter 명까지(초대받은 사람 몫은 따로 준다).
--  앱은 표를 직접 읽거나 쓰지 않고 아래 함수로만 쓴다.
-- ════════════════════════════════════════════════════════════════════

-- 원장 종류에 초대 적립을 더한다(0022 의 목록 + EARN_INVITE). 최종 목록이라 기존 줄까지 검사한다.
alter table public.sup_ledger drop constraint if exists sup_ledger_kind_check;
alter table public.sup_ledger add constraint sup_ledger_kind_check check (kind in (
  'EARN_WALK', 'EARN_PARTY', 'EARN_EVENT', 'BONUS_GOAL', 'EARN_COURSE', 'EARN_INVITE',
  'SPEND_MINT', 'SPEND_UPGRADE', 'SPEND_BOOST', 'SPEND_DRAW', 'SPEND_REPAIR',
  'ESCROW_LOCK', 'ESCROW_UNLOCK', 'TRADE_BUY', 'TRADE_SELL', 'TRADE_FEE',
  'CHAIN_WITHDRAW', 'CHAIN_REFUND', 'CHAIN_DEPOSIT'
));

create table if not exists economy.invite_config (
  id boolean primary key default true check (id),
  reward_sup numeric(20, 4) not null default 0 check (reward_sup >= 0 and reward_sup <= 1000),
  min_steps int not null default 1000 check (min_steps >= 0),
  max_rewards_per_inviter int not null default 50 check (max_rewards_per_inviter >= 0),
  redeem_window interval not null default interval '7 days'
);
insert into economy.invite_config (id) values (true) on conflict (id) do nothing;
revoke all on economy.invite_config from public;

create table if not exists public.invite_codes (
  user_id uuid primary key references auth.users on delete cascade,
  code text not null unique check (code ~ '^STEP-[A-Z2-9]{6}$'),
  created_at timestamptz not null default now()
);
alter table public.invite_codes enable row level security;
revoke all on public.invite_codes from anon, authenticated;

create table if not exists public.invite_redemptions (
  invitee uuid primary key references auth.users on delete cascade,
  inviter uuid not null references auth.users on delete cascade,
  created_at timestamptz not null default now(),
  rewarded_at timestamptz,
  reward_sup numeric(20, 4),
  check (invitee <> inviter)
);
create index if not exists invite_redemptions_inviter on public.invite_redemptions (inviter, created_at desc);
alter table public.invite_redemptions enable row level security;
revoke all on public.invite_redemptions from anon, authenticated;

/* 내 초대 코드(없으면 만든다)와 초대 현황. 적립액이 0이면 reward_sup 도 0 — 앱은 보상을 말하지 않는다. */
create or replace function public.invite_status()
returns table (code text, reward_sup numeric, invited int, rewarded int, can_redeem boolean, redeemed boolean)
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_code text;
  v_alphabet constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  v_cfg economy.invite_config;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select c.code into v_code from public.invite_codes c where c.user_id = v_user;
  while v_code is null loop
    v_code := 'STEP-' || (
      select string_agg(substr(v_alphabet, 1 + floor(random() * length(v_alphabet))::int, 1), '')
        from generate_series(1, 6));
    begin
      insert into public.invite_codes (user_id, code) values (v_user, v_code);
    exception when unique_violation then
      -- 같은 코드가 이미 있으면 다시 뽑는다. 같은 사람이 동시에 불렀으면 그 코드를 읽는다.
      select c.code into v_code from public.invite_codes c where c.user_id = v_user;
    end;
  end loop;
  select * into v_cfg from economy.invite_config where id;
  return query select
    v_code,
    coalesce(v_cfg.reward_sup, 0)::numeric,
    (select count(*)::int from public.invite_redemptions r where r.inviter = v_user),
    (select count(*)::int from public.invite_redemptions r where r.inviter = v_user and r.rewarded_at is not null),
    not exists (select 1 from public.invite_redemptions r where r.invitee = v_user)
      and exists (select 1 from auth.users u where u.id = v_user
                   and u.created_at > now() - coalesce(v_cfg.redeem_window, interval '7 days')),
    exists (select 1 from public.invite_redemptions r where r.invitee = v_user);
end $$;

/* 내가 초대한 사람 — 이름 · 입력한 때 · 적립 확정 때(서버가 적은 뒤에만 값이 있다) */
create or replace function public.invite_list()
returns table (display_name text, joined_at timestamptz, rewarded_at timestamptz)
language sql stable security definer set search_path = public as $$
  select coalesce(p.display_name, '러너'), r.created_at, r.rewarded_at
    from public.invite_redemptions r
    left join public.profiles p on p.id = r.invitee
   where r.inviter = auth.uid()
   order by r.created_at desc
   limit 100
$$;

/* 초대 코드 입력 — 새로 가입한 사람이 한 번. 초대한 사람 이름을 돌려준다. */
create or replace function public.invite_redeem(p_code text)
returns text
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_code text := upper(regexp_replace(coalesce(p_code, ''), '\s', '', 'g'));
  v_inviter uuid;
  v_cfg economy.invite_config;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if v_code !~ '^STEP-' then
    v_code := 'STEP-' || v_code;
  end if;
  select c.user_id into v_inviter from public.invite_codes c where c.code = v_code;
  if v_inviter is null then
    raise exception '초대 코드를 찾을 수 없어요' using errcode = 'P0002';
  end if;
  if v_inviter = v_user then
    raise exception '내 초대 코드는 입력할 수 없어요' using errcode = '22023';
  end if;
  if exists (select 1 from public.invite_redemptions r where r.invitee = v_user) then
    raise exception '이미 초대 코드를 입력했어요' using errcode = '23505';
  end if;
  -- 서로 맞초대로 두 번 받지 못하게
  if exists (select 1 from public.invite_redemptions r where r.invitee = v_inviter and r.inviter = v_user) then
    raise exception '나를 초대한 사람의 코드는 입력할 수 없어요' using errcode = '22023';
  end if;
  select * into v_cfg from economy.invite_config where id;
  if not exists (select 1 from auth.users u where u.id = v_user
                  and u.created_at > now() - coalesce(v_cfg.redeem_window, interval '7 days')) then
    raise exception '초대 코드는 가입하고 7일 안에만 입력할 수 있어요' using errcode = '22023';
  end if;
  insert into public.invite_redemptions (invitee, inviter) values (v_user, v_inviter);
  return (select coalesce(p.display_name, '러너') from public.profiles p where p.id = v_inviter);
end $$;

/*
 * 초대받은 사람의 첫 러닝 — 러닝이 기록된 뒤(무효 제외) 한 번만 적립한다.
 * 적립액이 0이면 아무것도 하지 않고 확정도 적지 않는다(나중에 금액이 정해지면 다음 러닝에서 준다).
 */
create or replace function economy.invite_reward_after_session()
returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare
  v_r public.invite_redemptions;
  v_cfg economy.invite_config;
  v_amount numeric(20, 4);
begin
  if new.verdict = 'VOID' then
    return null;
  end if;
  select * into v_r from public.invite_redemptions r
   where r.invitee = new.user_id and r.rewarded_at is null
   for update;
  if not found then
    return null;
  end if;
  select * into v_cfg from economy.invite_config where id;
  v_amount := coalesce(v_cfg.reward_sup, 0);
  if v_amount <= 0 or coalesce(new.steps, 0) < coalesce(v_cfg.min_steps, 1000) then
    return null;
  end if;
  perform economy.ledger_apply(new.user_id, 'EARN_INVITE', v_amount, '친구 초대 — 첫 러닝', 'invite:joined');
  if (select count(*) from public.invite_redemptions r
       where r.inviter = v_r.inviter and r.rewarded_at is not null) < v_cfg.max_rewards_per_inviter then
    perform economy.ledger_apply(v_r.inviter, 'EARN_INVITE', v_amount,
      '친구 초대 — 초대한 친구의 첫 러닝', 'invite:' || new.user_id::text);
  end if;
  update public.invite_redemptions set rewarded_at = now(), reward_sup = v_amount
   where invitee = new.user_id;
  return null;
end $$;
revoke all on function economy.invite_reward_after_session() from public;

drop trigger if exists invite_reward_after_session on public.walk_sessions;
create trigger invite_reward_after_session
  after insert on public.walk_sessions
  for each row execute function economy.invite_reward_after_session();

revoke all on function public.invite_status() from public, anon;
revoke all on function public.invite_list() from public, anon;
revoke all on function public.invite_redeem(text) from public, anon;
grant execute on function public.invite_status() to authenticated;
grant execute on function public.invite_list() to authenticated;
grant execute on function public.invite_redeem(text) to authenticated;
