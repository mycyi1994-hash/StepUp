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
  const { shareTarget, SHARE_TEXT, SHARE_URL } = await core;
  const x = new URL(shareTarget('x'));
  assert.equal(x.hostname, 'x.com');
  assert.equal(x.searchParams.get('text'), SHARE_TEXT);
  assert.equal(x.searchParams.get('url'), SHARE_URL);
  assert.ok(new URL(shareTarget('threads')).searchParams.get('text').includes(SHARE_URL));
});

test('referral links carry only a valid public code', async () => {
  const { referralCode, shareUrl, shareTarget, SHARE_URL } = await core;
  assert.equal(referralCode(' ab2cdefg '), 'AB2CDEFG');
  for (const bad of ['', 'ABCDEFG', 'ABCDEFGHJ', 'ABCDEFG0', 'ABCDEFGI', 'ABC<EFG>', null]) assert.equal(referralCode(bad), '', String(bad));
  assert.equal(shareUrl('AB2CDEFG'), `${SHARE_URL}?ref=AB2CDEFG`);
  assert.equal(shareUrl('bad code'), SHARE_URL);
  assert.equal(new URL(shareTarget('x', shareUrl('AB2CDEFG'))).searchParams.get('url'), `${SHARE_URL}?ref=AB2CDEFG`);
  assert.ok(new URL(shareTarget('threads', shareUrl('AB2CDEFG'))).searchParams.get('text').endsWith('?ref=AB2CDEFG'));
});
