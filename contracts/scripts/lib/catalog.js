/**
 * 신발 도감 — 속성 4 × 13종 = 52종. 앱의 domain/Sneaker.kt · 서버(0022)와 같은 표.
 *
 * 모델 번호 = 속성 × 100 + 등급 × 10 + 변형
 *   속성  FIRE 0 · WATER 1 · LIGHTNING 2 · WIND 3   (그림 주제일 뿐 스탯이 아니다)
 *   등급  COMMON 0 · RARE 1 · EPIC 2 · LEGENDARY 3
 *   변형  일반 4 · 레어 4 · 희귀 3 · 레전더리 2
 *
 * 어테스터가 서버 값(faction, rarity, variant)을 이 번호로 바꿔 서명한다.
 */
const FACTIONS = ["FIRE", "WATER", "LIGHTNING", "WIND"];
const RARITIES = ["COMMON", "RARE", "EPIC", "LEGENDARY"];
const VARIANTS = [4, 4, 3, 2];

function modelId(faction, rarity, variant) {
  const f = FACTIONS.indexOf(faction);
  const r = RARITIES.indexOf(rarity);
  if (f < 0 || r < 0 || variant < 0 || variant >= VARIANTS[r]) {
    throw new Error(`알 수 없는 모델: ${faction} ${rarity} ${variant}`);
  }
  return f * 100 + r * 10 + variant;
}

function allModels() {
  const ids = [];
  const rarities = [];
  FACTIONS.forEach((f) =>
    RARITIES.forEach((r, ri) => {
      for (let v = 0; v < VARIANTS[ri]; v++) {
        ids.push(modelId(f, r, v));
        rarities.push(ri);
      }
    }),
  );
  return { ids, rarities };
}

module.exports = { FACTIONS, RARITIES, VARIANTS, modelId, allModels };
