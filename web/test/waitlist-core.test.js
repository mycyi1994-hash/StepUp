const { test } = require('node:test');
const assert = require('node:assert/strict');
const core = import('../waitlist-core.mjs');

test('all six channels accept content links, including mobile video links', async () => {
  const { validPostUrl } = await core;
  const urls = {
    x: 'https://x.com/stepup/status/12345', instagram: 'https://www.instagram.com/p/Ab_-123/',
    threads: 'https://www.threads.com/@stepup/post/Ab_123', youtube: 'https://youtu.be/Abcdef123_-?si=share',
    tiktok: 'https://www.tiktok.com/@stepup/video/12345678', reels: 'https://www.instagram.com/reel/Abc_123/',
  };
  for (const [platform, url] of Object.entries(urls)) assert.equal(validPostUrl(url, platform), true, platform);
  for (const url of ['https://youtube.com/watch?feature=shared&v=Abcdef123_-', 'https://m.youtube.com/shorts/Abcdef123_-']) assert.ok(validPostUrl(url, 'youtube'));
  for (const url of ['https://vm.tiktok.com/Z123abc/', 'https://www.tiktok.com/t/Z123abc/']) assert.ok(validPostUrl(url, 'tiktok'));
});

test('rejects profiles, disguised hosts, malformed video ids and wrong channels', async () => {
  const { validPostUrl } = await core;
  for (const url of ['https://x.com/stepup', 'https://x.com.evil.test/stepup/status/123', 'https://evil.test@x.com/stepup/status/123', 'http://x.com/stepup/status/123', 'https://x.com/stepup/status/123/more']) assert.equal(validPostUrl(url, 'x'), false, url);
  assert.equal(validPostUrl('https://youtube.com/@stepup', 'youtube'), false);
  assert.equal(validPostUrl('https://youtube.com/watch?v=abc', 'youtube'), false);
  assert.equal(validPostUrl('https://instagram.com/p/abc', 'reels'), false);
  assert.equal(validPostUrl('https://instagram.com/reel/abc', 'tiktok'), false);
});

test('share intents preserve richer copy and the public homepage link', async () => {
  const { shareTarget, SHARE_TEXT, SHARE_URL, PROMO_VIDEO_URL } = await core;
  const x = new URL(shareTarget('x'));
  assert.equal(x.hostname, 'x.com');
  assert.equal(x.searchParams.get('text'), `${SHARE_TEXT}\n${SHARE_URL}`);
  // The video link goes last so X and Threads show a playable preview.
  assert.equal(x.searchParams.get('url'), PROMO_VIDEO_URL);
  const threads = new URL(shareTarget('threads')).searchParams.get('text');
  assert.ok(threads.includes(SHARE_URL) && threads.endsWith(PROMO_VIDEO_URL));
});

test('referral links carry only a valid public code', async () => {
  const { referralCode, shareUrl, shareTarget, SHARE_URL } = await core;
  assert.equal(referralCode(' ab2cdefg '), 'AB2CDEFG');
  for (const bad of ['', 'ABCDEFG', 'ABCDEFGHJ', 'ABCDEFG0', 'ABCDEFGI', 'ABC<EFG>', null]) assert.equal(referralCode(bad), '', String(bad));
  assert.equal(shareUrl('AB2CDEFG'), `${SHARE_URL}?ref=AB2CDEFG`);
  assert.equal(shareUrl('bad code'), SHARE_URL);
  assert.ok(new URL(shareTarget('x', shareUrl('AB2CDEFG'))).searchParams.get('text').endsWith(`${SHARE_URL}?ref=AB2CDEFG`));
  assert.ok(new URL(shareTarget('threads', shareUrl('AB2CDEFG'))).searchParams.get('text').includes('?ref=AB2CDEFG\n'));
});

test('emails follow the server rule after the same clean-up', async () => {
  const { validEmail, normalizeEmail } = await core;
  // The server lower-cases and trims; full-width ＠ ． from Korean/Japanese keyboards become ASCII.
  assert.equal(normalizeEmail('  Name@Example.COM '), 'name@example.com');
  assert.equal(normalizeEmail('name＠example．com'), 'name@example.com');
  for (const ok of ['a@b.co', 'name@example.com', 'name+tag@sub.example.kr', 'name＠example．com']) assert.equal(validEmail(ok), true, ok);
  // These passed the browser's check but the server refused them, and the page said "try again later" forever.
  for (const bad of ['', '   ', 'a@b', 'name@naver', 'name@gmailcom', 'name@gmail,com', 'a b@c.de', 'no-at-sign.com', '@example.com', `${'x'.repeat(250)}@e.com`]) {
    assert.equal(validEmail(bad), false, bad);
  }
});

test('post links are cleaned like the server expects (lower-case scheme and host)', async () => {
  const { normalizePostUrl, validPostUrl, isOwnSiteUrl } = await core;
  assert.equal(normalizePostUrl('  HTTPS://X.com/someone/status/12345  '), 'https://x.com/someone/status/12345');
  assert.equal(normalizePostUrl('https://www.Threads.com/@me/post/AbC'), 'https://www.threads.com/@me/post/AbC');
  assert.ok(validPostUrl(normalizePostUrl('HTTPS://X.com/someone/status/12345'), 'x'));
  assert.equal(normalizePostUrl('not a url'), 'not a url');
  assert.equal(isOwnSiteUrl('https://stepupcrew.com/?ref=ABCDEFGH'), true);
  assert.equal(isOwnSiteUrl('https://x.com/stepup/status/1'), false);
});

test('share text follows the page language and says it is an event entry', async () => {
  const { shareText, shareTarget, SHARE_TEXT, SHARE_TEXT_EN, CREATOR_TEXT_EN } = await core;
  assert.equal(shareText('ko'), SHARE_TEXT);
  assert.equal(shareText('en'), SHARE_TEXT_EN);
  assert.equal(shareText('en', true), CREATOR_TEXT_EN);
  for (const text of [SHARE_TEXT, SHARE_TEXT_EN]) assert.match(text, /※/);
  assert.equal(/[가-힣]/.test(SHARE_TEXT_EN), false);
  assert.ok(new URL(shareTarget('x', undefined, 'en')).searchParams.get('text').startsWith(SHARE_TEXT_EN));
});

test('placeholders hold no Korean (they are example links)', async () => {
  const { PLATFORMS } = await core;
  for (const [key, config] of Object.entries(PLATFORMS)) assert.equal(/[가-힣]/.test(config.placeholder), false, key);
});
