import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { SECTIONS, towerScore } from '../lab-mja9j66fef/tower-rules.js';

// StepUP 타워(내부 시험) — 게임이 보이는 점수와 서버(0104 economy.tower_sections · tower_score)가 다시 세는 점수가 같아야 한다
test('tower sections match the server table', () => {
  const sql = readFileSync(new URL('../../supabase/migrations/0104_tower.sql', import.meta.url), 'utf8');
  const json = sql.match(/economy\.tower_sections\(\)[\s\S]*?select '(\[.*?\])'::jsonb/)[1];
  assert.deepEqual(JSON.parse(json), SECTIONS.map(s => ({ len: s.len, y0: s.y0, y1: s.y1, par: s.par, min: s.min })));
});
test('tower score reference values (same numbers as supabase/tests/schema_test.sql)', () => {
  assert.equal(towerScore(2, [15000, 20000], 40, false, 0, true, 3), 2748);
  assert.equal(towerScore(5, [15000, 14000, 13000, 13000, 25000], 0, true, 1, false, 2), 8049);
  assert.equal(towerScore(0, [], 999, false, 0, false, 0), 80);
  assert.equal(towerScore(5, [60000, 60000, 60000, 60000, 60000], 0, true, 7, true, 30), 6345);
  assert.equal(towerScore(1, [15000], 0, false, 0, true, 0), 1330);
  assert.equal(towerScore(1, [15000], 40, false, 0, false, 3), 1504);
});
