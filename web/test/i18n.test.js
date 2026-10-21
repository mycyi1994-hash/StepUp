const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const i18n = import('../i18n.js');
const html = fs.readFileSync(path.join(__dirname, '..', 'index.html'), 'utf8');
const squash = text => text.replace(/\s+/g, ' ').trim();

test('every Korean string has an English one and the other way round', async () => {
  const { STRINGS } = await i18n;
  const ko = Object.keys(STRINGS.ko).sort();
  const en = Object.keys(STRINGS.en).sort();
  assert.deepEqual(en, ko);
  for (const key of ko) {
    assert.ok(STRINGS.ko[key].length > 0 && STRINGS.en[key].length > 0, key);
    // A placeholder in one language must exist in the other ({email}, {label}…)
    const holes = text => (text.match(/\{\w+\}/g) || []).sort().join();
    assert.equal(holes(STRINGS.en[key]), holes(STRINGS.ko[key]), key);
  }
});

test('every key the page uses exists, and the Korean in index.html is the dictionary text', async () => {
  const { STRINGS } = await i18n;
  // data-i18n="key">text<  (plain text elements)
  for (const [, key, text] of html.matchAll(/data-i18n="([\w.]+)"[^>]*>([^<]*)</g)) {
    assert.ok(key in STRINGS.ko, `missing key ${key}`);
    assert.equal(squash(text), squash(STRINGS.ko[key]), key);
  }
  // data-i18n-html="key">…</tag>  (text with <br>, links)
  for (const [, key] of html.matchAll(/data-i18n-html="([\w.]+)"/g)) assert.ok(key in STRINGS.ko, `missing key ${key}`);
  for (const [, pairs] of html.matchAll(/data-i18n-attr="([^"]+)"/g)) {
    for (const pair of pairs.split(';')) {
      const key = pair.split(':')[1].trim();
      assert.ok(key in STRINGS.ko, `missing attribute key ${key}`);
    }
  }
});

test('the English text is plain: no Korean left in it', async () => {
  const { STRINGS } = await i18n;
  // The language button names the other language, and the share-image description quotes the Korean slogan on the picture.
  const allowed = new Set(['lang.switch', 'lang.switchLabel']);
  for (const [key, text] of Object.entries(STRINGS.en)) {
    if (allowed.has(key)) continue;
    assert.equal(/[가-힣]/.test(text), false, `${key}: ${text}`);
  }
});

test('t() fills placeholders and falls back to Korean, then the key', async () => {
  const { t } = await i18n;
  assert.equal(t('done.email', { email: 'a@b.co' }), 'a@b.co로 출시 소식을 보내드릴게요.');
  assert.equal(t('no.such.key'), 'no.such.key');
});
