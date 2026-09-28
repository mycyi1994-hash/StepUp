-- ════════════════════════════════════════════════════════════════════
--  0045 — 새 신발 도감 70종 (2026-09-28, 사용자가 준 stepup-grade-70)
--
--  "뽑기에 이거 쓰고 확률은 일단 알아서" — 뽑기에서 새 도감의 신발이 나온다. 등급은 그림 폴더 그대로:
--  grade-1 레어 20종, grade-2 에픽 20종, grade-3 레전더리 30종(레전더리 · 레드라인 · 피니시 시리즈 10종씩).
--  새 도감에 일반 등급이 없으므로 뽑기에서 일반은 나오지 않는다(첫 신발은 그대로 일반, 이미 가진 신발도 그대로).
--
--  확률 — 씨앗 결과의 앞 4바이트로 등급을 굴린다(0023 과 같은 방식, 가중치 표만 새것: economy.draw_weight)
--    무료 뽑기   레어 72% · 에픽 22% · 레전더리 6%
--    상급 뽑기   에픽 이상 — 에픽 22/28(약 78.6%) · 레전더리 6/28(약 21.4%). Genesis 도 에픽 이상(예전 그대로)
--  모델은 굴린 등급의 모델 중에서 씨앗 결과 12~13번째 바이트로 고른다 — 같은 등급 안에서는 모두 같은 확률.
--  속성 · 변형도 예전처럼 정해 둔다(예전 앱 · 거래소 묶음이 깨지지 않게). 새 앱은 모델 번호로 그림 · 이름을 고른다.
--
--  체인 — 새 모델은 v2 신발 컨트랙트 도감에 없다(v2 에 모델을 더할 수 있는 것은 관리자 지갑뿐). 그래서 새 모델
--  신발은 v3 로만 올린다: 금고 발행(0044)은 원래 v3 이고, 꺼내기 · 지갑 선물 발행도 새 모델이면 v3 release 로
--  지갑에 바로 발행한다(워커). v3 도감에는 워커가 curator 로 한 번 더한다(attester src/catalog.js).
--
--  도감 행은 tools/gen_shoe_catalog.py 가 design/shoes-2026-09/catalog.json 으로 만든다. 번호 · 등급은 체인에
--  올라가므로 바꾸지 않는다(이름 · 설명만 고칠 수 있다). 새 모델은 뒤 번호로 더한다.
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.sneaker_models (
  id int primary key check (id >= 1000),
  rarity text not null check (rarity in ('COMMON', 'RARE', 'EPIC', 'LEGENDARY')),
  series text not null,
  slug text not null,
  name_en text not null,
  name_ko text not null,
  -- false 면 뽑기에서 나오지 않는다(이미 가진 신발은 그대로)
  active boolean not null default true,
  created_at timestamptz not null default now()
);
comment on table public.sneaker_models is
  '새 신발 도감(0045). 번호는 체인 v3 의 모델 번호와 같다 — 번호 · 등급은 바꾸지 않는다.';
-- 도감은 공개 정보다(앱 · 웹 · 검증하는 사람이 읽는다)
alter table public.sneaker_models enable row level security;
drop policy if exists sneaker_models_read on public.sneaker_models;
create policy sneaker_models_read on public.sneaker_models for select using (true);
revoke all on public.sneaker_models from anon, authenticated;
grant select on public.sneaker_models to anon, authenticated;

-- 도감 시작 (tools/gen_shoe_catalog.py 가 design/shoes-2026-09/catalog.json 으로 만든 파일 — 손으로 고치지 않는다.)
-- 체인에 올라간 번호의 등급은 바꿀 수 없다 — 이미 있는 번호의 등급이 다르면 올리지 않는다(이름만 고칠 수 있다)
do $$
begin
  if exists (select 1 from public.sneaker_models m
              join (values (1101, 'RARE'), (1102, 'RARE'), (1103, 'RARE'), (1104, 'RARE'), (1105, 'RARE'), (1106, 'RARE'), (1107, 'RARE'), (1108, 'RARE'), (1109, 'RARE'), (1110, 'RARE'), (1111, 'RARE'), (1112, 'RARE'), (1113, 'RARE'), (1114, 'RARE'), (1115, 'RARE'), (1116, 'RARE'), (1117, 'RARE'), (1118, 'RARE'), (1119, 'RARE'), (1120, 'RARE'), (1201, 'EPIC'), (1202, 'EPIC'), (1203, 'EPIC'), (1204, 'EPIC'), (1205, 'EPIC'), (1206, 'EPIC'), (1207, 'EPIC'), (1208, 'EPIC'), (1209, 'EPIC'), (1210, 'EPIC'), (1211, 'EPIC'), (1212, 'EPIC'), (1213, 'EPIC'), (1214, 'EPIC'), (1215, 'EPIC'), (1216, 'EPIC'), (1217, 'EPIC'), (1218, 'EPIC'), (1219, 'EPIC'), (1220, 'EPIC'), (1301, 'LEGENDARY'), (1302, 'LEGENDARY'), (1303, 'LEGENDARY'), (1304, 'LEGENDARY'), (1305, 'LEGENDARY'), (1306, 'LEGENDARY'), (1307, 'LEGENDARY'), (1308, 'LEGENDARY'), (1309, 'LEGENDARY'), (1310, 'LEGENDARY'), (1311, 'LEGENDARY'), (1312, 'LEGENDARY'), (1313, 'LEGENDARY'), (1314, 'LEGENDARY'), (1315, 'LEGENDARY'), (1316, 'LEGENDARY'), (1317, 'LEGENDARY'), (1318, 'LEGENDARY'), (1319, 'LEGENDARY'), (1320, 'LEGENDARY'), (1321, 'LEGENDARY'), (1322, 'LEGENDARY'), (1323, 'LEGENDARY'), (1324, 'LEGENDARY'), (1325, 'LEGENDARY'), (1326, 'LEGENDARY'), (1327, 'LEGENDARY'), (1328, 'LEGENDARY'), (1329, 'LEGENDARY'), (1330, 'LEGENDARY')) e(id, rarity) on e.id = m.id
             where e.rarity <> m.rarity) then
    raise exception '도감 번호의 등급이 바뀌었습니다 — 새 번호를 주세요';
  end if;
end $$;
insert into public.sneaker_models (id, rarity, series, slug, name_en, name_ko) values
  (1101, 'RARE', 'RARE', 'g1-01-rare-twin-arch-slide', 'Twin Arch Slide', '트윈 아치 슬라이드'),
  (1102, 'RARE', 'RARE', 'g1-02-rare-heel-loop-mule', 'Heel Loop Mule', '힐 루프 뮬'),
  (1103, 'RARE', 'RARE', 'g1-03-rare-toe-loop-sandal', 'Toe Loop Sandal', '토 루프 샌들'),
  (1104, 'RARE', 'RARE', 'g1-04-rare-fisherman-cage-sandal', 'Fisherman Cage Sandal', '피셔맨 케이지 샌들'),
  (1105, 'RARE', 'RARE', 'g1-05-rare-zero-drop-wide-slip-on', 'Zero Drop Wide Slip-On', '제로 드롭 와이드 슬립온'),
  (1106, 'RARE', 'RARE', 'g1-06-rare-side-zip-commuter-runner', 'Side Zip Commuter Runner', '사이드 집 커뮤터 러너'),
  (1107, 'RARE', 'RARE', 'g1-07-rare-entry-road-runner-split-heel', 'Split Heel Road Runner', '스플릿 힐 로드 러너'),
  (1108, 'RARE', 'RARE', 'g1-08-rare-entry-trail-runner-lug', 'Lug Trail Runner', '러그 트레일 러너'),
  (1109, 'RARE', 'RARE', 'g1-09-rare-entry-strap-road-runner', 'Strap Road Runner', '스트랩 로드 러너'),
  (1110, 'RARE', 'RARE', 'g1-10-rare-amphibious-runner', 'Amphibious Runner', '앰피비어스 러너'),
  (1111, 'RARE', 'RARE', 'g1-11-rare-wide-toe-road-runner', 'Wide Toe Road Runner', '와이드 토 로드 러너'),
  (1112, 'RARE', 'RARE', 'g1-12-rare-entry-stability-road-runner', 'Stability Road Runner', '스태빌리티 로드 러너'),
  (1113, 'RARE', 'RARE', 'g1-13-rare-strap-recovery-clog', 'Strap Recovery Clog', '스트랩 리커버리 클로그'),
  (1114, 'RARE', 'RARE', 'g1-14-rare-retro-canvas-jogger', 'Retro Canvas Jogger', '레트로 캔버스 조거'),
  (1115, 'RARE', 'RARE', 'g1-15-rare-entry-webbing-trail-runner', 'Webbing Trail Runner', '웨빙 트레일 러너'),
  (1116, 'RARE', 'RARE', 'g1-16-rare-rocker-walk-jogger', 'Rocker Walk Jogger', '로커 워크 조거'),
  (1117, 'RARE', 'RARE', 'g1-17-rare-entry-bungee-lace-runner', 'Bungee Lace Runner', '번지 레이스 러너'),
  (1118, 'RARE', 'RARE', 'g1-18-rare-entry-dial-road-runner', 'Dial Road Runner', '다이얼 로드 러너'),
  (1119, 'RARE', 'RARE', 'g1-19-rare-tabi-split-toe-jogger', 'Tabi Split Toe Jogger', '타비 스플릿 토 조거'),
  (1120, 'RARE', 'RARE', 'g1-20-rare-packable-flex-runner', 'Packable Flex Runner', '패커블 플렉스 러너'),
  (1201, 'EPIC', 'EPIC', 'g2-01-epic-performance-knit-wrap-runner', 'Knit Wrap Runner', '니트 랩 러너'),
  (1202, 'EPIC', 'EPIC', 'g2-02-epic-stability-daily-trainer', 'Stability Daily Trainer', '스태빌리티 데일리 트레이너'),
  (1203, 'EPIC', 'EPIC', 'g2-03-epic-tempo-rocker-trainer', 'Tempo Rocker Trainer', '템포 로커 트레이너'),
  (1204, 'EPIC', 'EPIC', 'g2-04-epic-rock-guard-trail-runner', 'Rock Guard Trail Runner', '록 가드 트레일 러너'),
  (1205, 'EPIC', 'EPIC', 'g2-05-epic-long-run-cushion-trainer', 'Long Run Cushion Trainer', '롱런 쿠션 트레이너'),
  (1206, 'EPIC', 'EPIC', 'g2-06-epic-road-trail-crossover-trainer', 'Road-Trail Crossover Trainer', '로드 트레일 크로스오버 트레이너'),
  (1207, 'EPIC', 'EPIC', 'g2-07-epic-rock-plate-trail-trainer', 'Rock Plate Trail Trainer', '록 플레이트 트레일 트레이너'),
  (1208, 'EPIC', 'EPIC', 'g2-08-epic-track-interval-trainer', 'Track Interval Trainer', '트랙 인터벌 트레이너'),
  (1209, 'EPIC', 'EPIC', 'g2-09-epic-reflective-night-trainer', 'Reflective Night Trainer', '리플렉티브 나이트 트레이너'),
  (1210, 'EPIC', 'EPIC', 'g2-10-epic-wet-road-trainer', 'Wet Road Trainer', '웻 로드 트레이너'),
  (1211, 'EPIC', 'EPIC', 'g2-11-epic-low-drop-form-trainer', 'Low Drop Form Trainer', '로우 드롭 폼 트레이너'),
  (1212, 'EPIC', 'EPIC', 'g2-12-epic-low-collar-gaiter-trail-runner', 'Low Collar Gaiter Trail Runner', '로우 칼라 게이터 트레일 러너'),
  (1213, 'EPIC', 'EPIC', 'g2-13-epic-heel-cushion-pod-trainer', 'Heel Cushion Pod Trainer', '힐 쿠션 포드 트레이너'),
  (1214, 'EPIC', 'EPIC', 'g2-14-epic-external-heel-guide-trainer', 'External Heel Guide Trainer', '익스터널 힐 가이드 트레이너'),
  (1215, 'EPIC', 'EPIC', 'g2-15-epic-winter-microgrip-trainer', 'Winter Microgrip Trainer', '윈터 마이크로그립 트레이너'),
  (1216, 'EPIC', 'EPIC', 'g2-16-epic-forefoot-hill-repeat-trainer', 'Forefoot Hill Repeat Trainer', '포어풋 힐 리피트 트레이너'),
  (1217, 'EPIC', 'EPIC', 'g2-17-epic-arch-bridge-road-trainer', 'Arch Bridge Road Trainer', '아치 브리지 로드 트레이너'),
  (1218, 'EPIC', 'EPIC', 'g2-18-epic-polymer-plate-tempo-trainer', 'Polymer Plate Tempo Trainer', '폴리머 플레이트 템포 트레이너'),
  (1219, 'EPIC', 'EPIC', 'g2-19-epic-rocker-distance-trainer', 'Rocker Distance Trainer', '로커 디스턴스 트레이너'),
  (1220, 'EPIC', 'EPIC', 'g2-20-epic-wide-toe-flex-trainer', 'Wide Toe Flex Trainer', '와이드 토 플렉스 트레이너'),
  (1301, 'LEGENDARY', 'LEGENDARY', 'g3-01-legendary-carbon-marathon-training-shoe', 'Carbon Marathon Training Shoe', '카본 마라톤 트레이닝 슈'),
  (1302, 'LEGENDARY', 'LEGENDARY', 'g3-02-legendary-stability-trainer', 'Stability Trainer', '스태빌리티 트레이너'),
  (1303, 'LEGENDARY', 'LEGENDARY', 'g3-03-legendary-carbon-rockplate-trail-runner', 'Carbon Rockplate Trail Runner', '카본 록플레이트 트레일 러너'),
  (1304, 'LEGENDARY', 'LEGENDARY', 'g3-04-legendary-light-midcut-trail-runner', 'Light Midcut Trail Runner', '라이트 미드컷 트레일 러너'),
  (1305, 'LEGENDARY', 'LEGENDARY', 'g3-05-legendary-ultradistance-wave-cushion-trainer', 'Ultradistance Wave Cushion Trainer', '울트라디스턴스 웨이브 쿠션 트레이너'),
  (1306, 'LEGENDARY', 'LEGENDARY', 'g3-06-legendary-forefoot-marathon-interval-trainer', 'Forefoot Marathon Interval Trainer', '포어풋 마라톤 인터벌 트레이너'),
  (1307, 'LEGENDARY', 'LEGENDARY', 'g3-07-legendary-asymmetric-TPU-hill-trainer', 'Asymmetric TPU Hill Trainer', '어시메트릭 TPU 힐 트레이너'),
  (1308, 'LEGENDARY', 'LEGENDARY', 'g3-08-legendary-desert-trail-runner', 'Desert Trail Runner', '데저트 트레일 러너'),
  (1309, 'LEGENDARY', 'LEGENDARY', 'g3-09-legendary-external-heel-cage-stability-trainer', 'Heel Cage Stability Trainer', '힐 케이지 스태빌리티 트레이너'),
  (1310, 'LEGENDARY', 'LEGENDARY', 'g3-10-legendary-full-length-curve-interval-trainer', 'Full-Length Curve Interval Trainer', '풀렝스 커브 인터벌 트레이너'),
  (1311, 'LEGENDARY', 'REDLINE', 'g3-11-redline-100m-sprint-spike', 'Redline 100m Sprint Spike', '레드라인 100m 스프린트 스파이크'),
  (1312, 'LEGENDARY', 'REDLINE', 'g3-12-redline-800m-middle-distance-spike', 'Redline 800m Middle Distance Spike', '레드라인 800m 미들 디스턴스 스파이크'),
  (1313, 'LEGENDARY', 'REDLINE', 'g3-13-redline-5000m-distance-spike', 'Redline 5000m Distance Spike', '레드라인 5000m 디스턴스 스파이크'),
  (1314, 'LEGENDARY', 'REDLINE', 'g3-14-redline-road-5k10k-racing-flat', 'Redline 5K·10K Racing Flat', '레드라인 5K·10K 레이싱 플랫'),
  (1315, 'LEGENDARY', 'REDLINE', 'g3-15-redline-full-marathon-carbon-super-shoe', 'Redline Marathon Carbon Super Shoe', '레드라인 마라톤 카본 슈퍼슈'),
  (1316, 'LEGENDARY', 'REDLINE', 'g3-16-redline-trail-race-shoe', 'Redline Trail Race Shoe', '레드라인 트레일 레이스 슈'),
  (1317, 'LEGENDARY', 'REDLINE', 'g3-17-redline-triathlon-transition-racer', 'Redline Triathlon Transition Racer', '레드라인 트라이애슬론 트랜지션 레이서'),
  (1318, 'LEGENDARY', 'REDLINE', 'g3-18-redline-steeplechase-drain-spike', 'Redline Steeplechase Drain Spike', '레드라인 스티플체이스 드레인 스파이크'),
  (1319, 'LEGENDARY', 'REDLINE', 'g3-19-redline-half-marathon-road-racer', 'Redline Half Marathon Road Racer', '레드라인 하프 마라톤 로드 레이서'),
  (1320, 'LEGENDARY', 'REDLINE', 'g3-20-redline-cross-country-spike', 'Redline Cross Country Spike', '레드라인 크로스컨트리 스파이크'),
  (1321, 'LEGENDARY', 'FINISH', 'g3-21-finish-championship-marathon-super-shoe', 'Finish Championship Marathon Super Shoe', '피니시 챔피언십 마라톤 슈퍼슈'),
  (1322, 'LEGENDARY', 'FINISH', 'g3-22-finish-elite-100m-sprint-spike', 'Finish Elite 100m Sprint Spike', '피니시 엘리트 100m 스프린트 스파이크'),
  (1323, 'LEGENDARY', 'FINISH', 'g3-23-finish-elite-middle-distance-spike', 'Finish Elite Middle Distance Spike', '피니시 엘리트 미들 디스턴스 스파이크'),
  (1324, 'LEGENDARY', 'FINISH', 'g3-24-finish-ultratrail-championship-racer', 'Finish Ultratrail Championship Racer', '피니시 울트라트레일 챔피언십 레이서'),
  (1325, 'LEGENDARY', 'FINISH', 'g3-25-finish-championship-5k-10k-road-flat', 'Finish Championship 5K·10K Road Flat', '피니시 챔피언십 5K·10K 로드 플랫'),
  (1326, 'LEGENDARY', 'FINISH', 'g3-26-finish-championship-cross-country-spike', 'Finish Championship Cross Country Spike', '피니시 챔피언십 크로스컨트리 스파이크'),
  (1327, 'LEGENDARY', 'FINISH', 'g3-27-finish-championship-steeplechase-spike', 'Finish Championship Steeplechase Spike', '피니시 챔피언십 스티플체이스 스파이크'),
  (1328, 'LEGENDARY', 'FINISH', 'g3-28-finish-championship-400m-sprint-spike', 'Finish Championship 400m Sprint Spike', '피니시 챔피언십 400m 스프린트 스파이크'),
  (1329, 'LEGENDARY', 'FINISH', 'g3-29-finish-championship-800m-middle-distance-spike', 'Finish Championship 800m Middle Distance Spike', '피니시 챔피언십 800m 미들 디스턴스 스파이크'),
  (1330, 'LEGENDARY', 'FINISH', 'g3-30-finish-championship-half-marathon-carbon-racer', 'Finish Championship Half Marathon Carbon Racer', '피니시 챔피언십 하프 마라톤 카본 레이서')
on conflict (id) do update
  set series = excluded.series, slug = excluded.slug, name_en = excluded.name_en, name_ko = excluded.name_ko;
-- 도감 끝

-- 신발의 도감 번호 — 새 도감에서 뽑은 신발만. 비어 있으면 예전 52종(속성 × 변형)
alter table public.market_sneakers
  add column if not exists model_id int references public.sneaker_models (id);

-- ══════════════════════════════════════════════════════════════════
-- 뽑기 — 새 가중치 표 · 모델 고르기
-- ══════════════════════════════════════════════════════════════════
-- 뽑기 가중치(0045). 예전 표(economy.rarity_weight 55 · 28 · 13 · 4)는 0045 전의 뽑기를 다시 계산할 때 쓴다.
create or replace function economy.draw_weight(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'RARE' then 72 when 'EPIC' then 22 when 'LEGENDARY' then 6 else 0 end
$$;

-- 씨앗 결과 → 등급. p_min 이 있으면 그 등급 이상만(상급 · Genesis 는 에픽 이상).
-- 가중치 표 안에서 굴리므로 제한이 있어도 위 등급끼리의 비율(22:6)은 그대로다.
create or replace function economy.roll_rarity(p_roll bigint, p_min text default null) returns text
  language plpgsql immutable as $$
declare
  v_order text[] := array['COMMON', 'RARE', 'EPIC', 'LEGENDARY'];
  v_from int := coalesce(economy.rarity_ord(p_min), 0) + 1;
  v_total int := 0;
  v_pick int;
  i int;
begin
  for i in v_from .. 4 loop
    v_total := v_total + economy.draw_weight(v_order[i]);
  end loop;
  v_pick := (p_roll % v_total)::int;
  for i in v_from .. 4 loop
    v_pick := v_pick - economy.draw_weight(v_order[i]);
    if v_pick < 0 then
      return v_order[i];
    end if;
  end loop;
  return v_order[4];
end $$;

-- 굴린 등급의 뽑을 수 있는 모델 중 하나(번호 차례로 p_roll 번째). 없으면 null — 예전 속성 그림을 쓴다.
create or replace function economy.draw_model(p_rarity text, p_roll bigint) returns int
  language sql stable security definer set search_path = public as $$
  select m.id
    from public.sneaker_models m
   where m.rarity = p_rarity and m.active
   order by m.id
  offset (p_roll % greatest((select count(*) from public.sneaker_models x
                              where x.rarity = p_rarity and x.active), 1))
   limit 1
$$;
revoke all on function economy.draw_model(text, bigint) from public;

-- 상급 뽑기는 에픽 이상 — 무료 뽑기가 레어 이상이 되었으므로. 운영 값을 손으로 바꿨으면 그대로 둔다.
update public.economy_settings set value = '"EPIC"'::jsonb
 where key = 'premium_min_rarity' and value = '"RARE"'::jsonb;

/*
 * 뽑은 결과로 신발을 만든다(0023 그대로 + 새 도감 모델). 값은 부르는 쪽이 이미 치렀다.
 * 씨앗 결과 바이트: 0~3 등급 · 4~5 속성 · 6~7 변형 · 8~9 효율 · 10~11 착화감 · 12~13 모델
 */
create or replace function economy.draw_create(
  p_user uuid,
  p_origin text,
  p_min_rarity text default null,
  p_genesis boolean default false
) returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_nonce bigint;
  v_d bytea;
  v_rarity text;
  v_faction text;
  v_variant int;
  v_eff_lo int; v_eff_hi int;
  v_cmf_lo int; v_cmf_hi int;
  v_id bigint;
  v_lock numeric := 0;
begin
  select n.o_nonce, n.o_digest into v_nonce, v_d from economy.draw_digest(p_user) n;

  v_rarity := economy.roll_rarity(economy.bytes_int(v_d, 0, 4), p_min_rarity);
  v_faction := (array['FIRE', 'WATER', 'LIGHTNING', 'WIND'])[economy.bytes_int(v_d, 4, 2) % 4 + 1];
  v_variant := (economy.bytes_int(v_d, 6, 2) % economy.variant_count(v_rarity))::int;
  select lo, hi into v_eff_lo, v_eff_hi from economy.efficiency_range(v_rarity);
  select lo, hi into v_cmf_lo, v_cmf_hi from economy.comfort_range(v_rarity);

  if p_origin in ('FREE_DRAW', 'BONUS_DRAW') then
    v_lock := economy.setting_num('free_shoe_lock_km');
  end if;

  insert into public.market_sneakers (
    owner_id, faction, rarity, variant, level, luck, comfort, durability,
    origin, efficiency_bps, comfort_bps, durability_pts,
    lock_km, withdrawable, draw_nonce, genesis_no, model_id
  ) values (
    p_user, v_faction, v_rarity, v_variant, 1, 1,
    -- 옛 comfort 칸(배율 1.00~1.40)은 옛 앱 화면용으로만 채운다.
    1 + v_cmf_lo / 10000.0, 100,
    p_origin,
    v_eff_lo + (economy.bytes_int(v_d, 8, 2) % (v_eff_hi - v_eff_lo + 1))::int,
    v_cmf_lo + (economy.bytes_int(v_d, 10, 2) % (v_cmf_hi - v_cmf_lo + 1))::int,
    100,
    v_lock, p_origin <> 'STARTER', v_nonce,
    case when p_genesis then nextval('public.genesis_seq')::int end,
    economy.draw_model(v_rarity, economy.bytes_int(v_d, 12, 2))
  ) returning id into v_id;

  return v_id;
end $$;
revoke all on function economy.draw_create(uuid, text, text, boolean) from public;

-- 웹 지갑 페이지의 보너스 뽑기(지갑 선물, 0025) — 앱의 상급 뽑기와 같은 지갑 선물 횟수를 쓰므로 같은 하한(에픽 이상).
-- 그 밖에는 0025 그대로: 첫 번이 Genesis, 지갑으로 바로 발행(새 도감 신발이면 워커가 v3 release 로), 50km 전 잠금.
create or replace function public.bonus_draw_request()
returns uuid
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_wallet text;
  v_genesis boolean;
  v_min text := nullif(economy.setting('premium_min_rarity') #>> '{}', '');
  v_id bigint;
  v_op uuid := gen_random_uuid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform pg_advisory_xact_lock(hashtext('chain:withdraw'));
  v_wallet := economy.withdraw_gate(v_user, false);

  if economy.mints_today('BONUS_MINT') >= economy.setting_num('bonus_mint_global_daily') then
    raise exception '오늘 발행 한도가 찼습니다. 내일 다시 해 주세요' using errcode = '23514';
  end if;

  select g.genesis_used < g.genesis_granted into v_genesis
    from public.draw_grants g where g.user_id = v_user and g.kind = 'BONUS';

  update public.draw_grants
     set used = used + 1,
         genesis_used = genesis_used + case when v_genesis then 1 else 0 end
   where user_id = v_user and kind = 'BONUS' and used < granted;
  if not found then
    raise exception '보너스 뽑기가 남아 있지 않습니다' using errcode = '23514';
  end if;

  v_id := economy.draw_create(v_user, 'BONUS_DRAW',
                              case when v_genesis then 'EPIC' else v_min end, coalesce(v_genesis, false));
  update public.market_sneakers set chain_state = 'WITHDRAWING' where id = v_id;

  insert into public.chain_ops (id, user_id, kind, wallet, sneaker_id, deadline)
  values (v_op, v_user, 'BONUS_MINT', v_wallet, v_id, economy.op_deadline());
  return v_op;
end $$;
revoke all on function public.bonus_draw_request() from public, anon;
grant execute on function public.bonus_draw_request() to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 앱 · 워커가 받는 값에 도감 번호
-- ══════════════════════════════════════════════════════════════════
-- 내 신발(0023) + model_id. 예전 앱은 모르는 칸을 버린다.
drop function if exists public.my_sneakers();
create function public.my_sneakers()
returns table (
  id bigint, faction text, rarity text, variant int, level int, max_level int,
  efficiency_bps int, comfort_bps int, durability numeric,
  equipped boolean, origin text, chain_state text, status text,
  mint_number bigint, genesis_no int, token_id numeric,
  km_run numeric, lock_km numeric, can_withdraw boolean,
  upgrade_cost numeric, repair_cost_per_point numeric,
  model_id int
)
language plpgsql stable security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return query
  select s.id, s.faction, s.rarity, s.variant, s.level, economy.max_level(s.rarity),
         e.efficiency_bps, e.comfort_bps, s.durability_pts,
         s.equipped, s.origin, s.chain_state, s.status,
         s.mint_number, s.genesis_no, s.token_id,
         s.km_run, s.lock_km,
         (s.withdrawable and s.km_run >= s.lock_km and s.chain_state = 'APP' and s.status = 'OWNED'),
         economy.upgrade_cost(s.rarity, s.level),
         economy.repair_cost_per_point(s.rarity, s.level),
         s.model_id
    from public.market_sneakers s,
         lateral economy.sneaker_effective(s.origin, s.rarity, s.level,
           s.efficiency_bps, s.comfort_bps, s.durability_pts) e
   where s.owner_id = v_user
   order by s.equipped desc, s.id;
end $$;
revoke all on function public.my_sneakers() from public, anon;
grant execute on function public.my_sneakers() to authenticated;

-- 어테스터가 서명할 작업(0029) + model_id — 새 모델이면 워커가 v3 로 발행한다
drop function if exists public.attester_op_payload(uuid, uuid);
create function public.attester_op_payload(p_op uuid, p_user uuid)
returns table (
  op_id uuid,
  op_ref text,
  kind text,
  wallet text,
  account_ref text,
  amount numeric,
  deadline_unix bigint,
  run_day date,
  sneaker_id bigint,
  token_id numeric,
  faction text,
  rarity text,
  variant int,
  level int,
  efficiency_bps int,
  comfort_bps int,
  durability numeric,
  genesis_no int,
  transfer_locked boolean,
  model_id int
)
language plpgsql security definer set search_path = public, economy as $$
declare v public.chain_ops;
begin
  perform economy.attester_guard();
  select * into v from public.chain_ops o where o.id = p_op for update;
  if not found or p_user is null or v.user_id is distinct from p_user then
    raise exception '없는 작업입니다' using errcode = '22023';
  end if;
  if v.status not in ('RESERVED', 'SIGNED') or v.deadline <= now() then
    raise exception '서명할 수 없는 작업입니다 (%)', v.status using errcode = '22023';
  end if;
  -- 방금 다른 요청이 서명해 보내는 중이다. 둘이 겹치면 가스비를 두 번 내고 하나는 체인이
  -- 거절한다. 보내기가 실패했으면 잠시 뒤 다시 보낼 수 있다.
  if v.status = 'SIGNED' and v.updated_at > now() - interval '45 seconds' then
    raise exception '이 작업을 보내는 중입니다. 잠시 뒤에 다시 해 주세요' using errcode = '55000';
  end if;
  if (economy.setting('chain_paused') #>> '{}')::boolean then
    raise exception '지금은 체인 작업을 잠시 멈췄습니다' using errcode = '55000';
  end if;

  update public.chain_ops o set status = 'SIGNED', updated_at = now() where o.id = p_op;

  return query
  select v.id, economy.op_ref(v.id), v.kind, v.wallet, economy.account_ref(v.user_id),
         v.amount, extract(epoch from v.deadline)::bigint, economy.game_day(v.created_at),
         s.id, s.token_id, s.faction, s.rarity, s.variant, s.level,
         s.efficiency_bps, s.comfort_bps, s.durability_pts, s.genesis_no,
         coalesce(s.km_run < s.lock_km, false),
         s.model_id
    from (select 1) one
    left join public.market_sneakers s on s.id = v.sneaker_id;
end $$;

/*
 * 보낼 일을 가져간다. 워커가 다룰 수 있는 종류만(p_kinds), 몇 개만(p_limit).
 * 가져간 일은 2분 동안 이 워커 것(CLAIMED) — 그 안에 서명한 거래를 적지 않으면 다음 워커가 다시 가져간다
 * (서명한 거래를 적기 전에는 보내지 않으므로 두 번 나가지 않는다).
 *
 * 돌려주는 payload (종류마다):
 *   RUN_PROOF   recipient · runner · run · day · distance_m · duration_sec
 *   COURSE_RUN  recipient · runner · course · run · day · distance_m · duration_sec
 *   BADGE       recipient · runner · badge · value · day
 *   VAULT_MINT  account · model_id(새 도감, 없으면 null) · faction · rarity · variant · level · efficiency_bps · comfort_bps ·
 *               durability · genesis_no · deadline_unix
 *   STATS_SYNC  token_id · level · durability · deadline_unix
 */
create or replace function public.attester_jobs_claim(p_kinds text[], p_limit int)
returns table (job_id bigint, op_ref text, kind text, payload jsonb)
language plpgsql security definer set search_path = public, economy as $$
declare
  j public.chain_jobs;
  v_left int;
  v_taken int := 0;
  v_want int := greatest(0, least(coalesce(p_limit, 0), 50));
  v_payload jsonb;
  v_wallet text;
  v_owner uuid;
  s public.market_sneakers;
  w public.walk_sessions;
  v_deadline bigint := extract(epoch from now())::bigint
                       + coalesce(economy.setting_num('chain_job_deadline_sec'), 3600)::bigint;
begin
  perform economy.attester_guard();
  if coalesce((economy.setting('chain_paused') #>> '{}')::boolean, false) or not economy.chain_jobs_on() then
    return;
  end if;
  -- 오늘 보낸 수 + 지금 보내는 중인 수가 하루 상한 안에서만
  v_left := coalesce(economy.setting_num('chain_jobs_global_daily'), 0)::int
            - (select count(*) from public.chain_jobs c
                where (c.sent_at >= economy.today_start()) or c.status = 'CLAIMED')::int;
  v_want := least(v_want, greatest(v_left, 0));

  for j in
    select * from public.chain_jobs c
     where c.kind = any (p_kinds)
       and (c.status = 'QUEUED' or (c.status = 'CLAIMED' and c.lease_until < now()))
       and c.next_at <= now()
     order by c.next_at, c.id
     limit 200
     for update skip locked
  loop
    exit when v_taken >= v_want;
    v_payload := null;

    if j.user_id is null then
      perform economy.job_cancel(j.id, 'account deleted');
      continue;
    end if;

    if j.kind in ('RUN_PROOF', 'COURSE_RUN', 'BADGE') then
      if j.kind <> 'BADGE' then
        select * into w from public.walk_sessions where id = j.session_id;
        if not found or w.verdict = 'VOID' then
          perform economy.job_cancel(j.id, 'run gone');
          continue;
        end if;
      end if;
      select address into v_wallet from public.wallet_links where user_id = j.user_id;
      v_payload := jsonb_build_object(
        'recipient', coalesce(v_wallet, '0x0000000000000000000000000000000000000000'),
        'runner', economy.pseudonym('runner', j.user_id::text),
        'day', (j.args ->> 'day')::int);
      if j.kind = 'RUN_PROOF' then
        v_payload := v_payload || jsonb_build_object(
          'run', economy.pseudonym('run', j.session_id::text),
          'distance_m', (j.args ->> 'distance_m')::int,
          'duration_sec', (j.args ->> 'duration_sec')::int);
      elsif j.kind = 'COURSE_RUN' then
        v_payload := v_payload || jsonb_build_object(
          'course', economy.pseudonym('course', j.args ->> 'course_id'),
          'run', economy.pseudonym('run', j.session_id::text),
          'distance_m', (j.args ->> 'distance_m')::int,
          'duration_sec', (j.args ->> 'duration_sec')::int);
      else
        v_payload := v_payload || jsonb_build_object(
          'badge', j.args ->> 'badge',
          'value', (j.args ->> 'value')::int);
      end if;

    else
      -- 신발 — 지금 주인의 꺼내기와 엇갈리지 않게 같은 잠금을 잡는다(앱에서 팔렸으면 새 주인).
      -- 못 잡으면(꺼내는 중) 다음에 — 기다리지 않아 서로 잠금을 기다리며 멈추는 일이 없다
      select owner_id into v_owner from public.market_sneakers where id = j.sneaker_id;
      if v_owner is null then
        perform economy.job_cancel(j.id, 'sneaker gone');
        continue;
      end if;
      if not pg_try_advisory_xact_lock(hashtext('ledger:' || v_owner::text)) then
        continue;
      end if;
      select * into s from public.market_sneakers where id = j.sneaker_id;
      if not found or s.owner_id is distinct from v_owner then
        continue;
      end if;
      if exists (select 1 from public.chain_ops o
                  where o.sneaker_id = s.id and o.status in ('RESERVED', 'SIGNED', 'SUBMITTED')) then
        -- 꺼내는 중 — 끝나거나 만료된 뒤에 다시 본다
        update public.chain_jobs set next_at = now() + interval '10 minutes', updated_at = now() where id = j.id;
        continue;
      end if;
      if j.kind = 'VAULT_MINT' then
        if s.token_id is not null then
          perform economy.job_cancel(j.id, 'already on chain');
          continue;
        end if;
        v_payload := jsonb_build_object(
          'account', economy.pseudonym('runner', s.owner_id::text),
          'model_id', s.model_id,
          'faction', s.faction, 'rarity', s.rarity, 'variant', s.variant, 'level', s.level,
          'efficiency_bps', s.efficiency_bps, 'comfort_bps', s.comfort_bps,
          'durability', s.durability_pts, 'genesis_no', s.genesis_no,
          'deadline_unix', v_deadline);
      else
        if s.token_id is null or s.token_id < economy.v3_first_token() or s.chain_state <> 'APP' then
          perform economy.job_cancel(j.id, 'not in the v3 vault');
          continue;
        end if;
        v_payload := jsonb_build_object(
          'token_id', s.token_id::text, 'level', s.level, 'durability', s.durability_pts,
          'deadline_unix', v_deadline);
      end if;
    end if;

    update public.chain_jobs c
       set status = 'CLAIMED', lease_until = now() + interval '2 minutes', payload = v_payload, updated_at = now()
     where c.id = j.id;
    v_taken := v_taken + 1;
    job_id := j.id;
    op_ref := economy.op_ref(j.op_id);
    kind := j.kind;
    payload := v_payload;
    return next;
  end loop;
end $$;

-- 만료 차례의 작업(0044) + model_id — 새 모델의 첫 발행은 v3 에서 쓰였는지 본다
drop function if exists public.attester_due_ops();
create function public.attester_due_ops()
returns table (op_id uuid, op_ref text, status text, kind text, deadline timestamptz, tx_hash text, early boolean,
               token_id numeric, model_id int)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform economy.attester_guard();
  return query
  select o.id, economy.op_ref(o.id), o.status, o.kind, o.deadline, o.tx_hash,
         o.deadline + make_interval(secs => economy.setting_num('op_expire_margin_sec')::int) >= now(),
         s.token_id, s.model_id
    from public.chain_ops o
    left join public.market_sneakers s on s.id = o.sneaker_id
   where o.status in ('RESERVED', 'SIGNED', 'SUBMITTED')
     and o.deadline < now()
   order by o.deadline
   limit 200;
end $$;

-- 어테스터 함수 권한 — 0025 · 0026 과 같은 규칙(첫 줄의 attester_guard 가 막는다)
do $$
declare r record;
begin
  for r in
    select p.oid::regprocedure as sig from pg_proc p
     where p.pronamespace = 'public'::regnamespace and p.proname like 'attester\_%'
  loop
    execute format('revoke all on function %s from public, anon, authenticated', r.sig);
    execute format('grant execute on function %s to stepup_attester, authenticated', r.sig);
  end loop;
end $$;
