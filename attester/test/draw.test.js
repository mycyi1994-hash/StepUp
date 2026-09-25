import test from 'node:test'
import assert from 'node:assert/strict'
import { drawPrize } from '../src/draw.js'

const address = '0x1111111111111111111111111111111111111111'
const seed = 'deterministic-private-test-seed-32-characters'

test('repeated authorization requests cannot change the outcome at the same nonce', () => {
  const first = drawPrize(seed, address, 0, 7n)
  assert.deepEqual(drawPrize(seed, address, 0, 7n), first)
  assert.ok(first.faction >= 0 && first.faction < 4)
  assert.ok(first.rarity >= 0 && first.rarity < 4)
  assert.ok(first.variant >= 0 && first.variant < [4, 4, 3, 2][first.rarity])
  assert.equal(first.outfitId, 0)
})

test('tracksuit draw stays within the five real catalogue outfits', () => {
  for (let nonce = 0n; nonce < 30n; nonce++) {
    const prize = drawPrize(seed, address, 1, nonce)
    assert.ok(prize.outfitId >= 1 && prize.outfitId <= 5)
    assert.equal(prize.faction, 0)
    assert.equal(prize.rarity, 0)
  }
})
