/* StepUP 타워 점수 규칙 — 서버 0104(tower_sections · economy.tower_score)와 같아야 한다. web/test/tower.test.js 가 맞대어 본다.
   len = 그 구간 시작부터 다음 구간 시작(마지막은 결승선)까지 가운데 줄 거리(m), y0 · y1 = 구간 시작 · 끝 높이, par = 기준 시간(ms), min = 이보다 빠른 구간은 서버가 받지 않는다. */
export const SECTIONS = [
  { name: '몸풀기', len: 117.7, y0: 0, y1: 8, par: 25000, min: 9000 },
  { name: '밀치기', len: 100, y0: 8, y1: 14, par: 24000, min: 8500 },
  { name: '흔들 다리', len: 100, y0: 14, y1: 20, par: 22000, min: 8000 },
  { name: '무너지는 길', len: 103.4, y0: 20, y1: 24, par: 22000, min: 7500 },
  { name: '정상', len: 88, y0: 24, y1: 34.5, par: 26000, min: 9000 }
];
export const PLACE_BONUS = [1000, 600, 300];
export const RUN_LIMIT_S = 480;

// cleared = 통과한 구간 수(5 = 정상), ms = 구간별 시간, progress = 지금 구간에서 가장 멀리 간 거리(m)
export function towerScore(cleared, ms, progress, finished, place, revived, ko) {
  let sc = cleared * 1000;
  for (let i = 0; i < cleared; i++) sc += Math.max(0, Math.floor((SECTIONS[i].par * 1.6 - ms[i]) / 100));
  const k = Math.min(cleared, SECTIONS.length - 1), S0 = SECTIONS[k];
  const h = finished ? SECTIONS[SECTIONS.length - 1].y1 : S0.y0 + Math.min(1, Math.max(0, progress / S0.len)) * (S0.y1 - S0.y0);
  sc += Math.floor(h * 10);
  if (finished) sc += (PLACE_BONUS[place - 1] || 0) + (revived ? 0 : 500);
  return sc + Math.min(Math.max(ko, 0), 20) * 50;
}
