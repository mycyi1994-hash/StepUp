import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { createHash } from 'node:crypto';

const headers = readFileSync(new URL('../_headers', import.meta.url), 'utf8');

/** _headers 에서 한 경로 줄 아래의 CSP */
function cspFor(path) {
  const lines = headers.split('\n');
  const at = lines.findIndex(line => line === path);
  assert.ok(at >= 0, `_headers 에 ${path} 가 있다`);
  const line = lines.slice(at + 1).find(l => /^\s+Content-Security-Policy:/.test(l));
  return line.replace(/^\s+Content-Security-Policy:\s*/, '');
}

function inlineHashes(file) {
  const html = readFileSync(new URL(`../${file}`, import.meta.url), 'utf8');
  return [...html.matchAll(/<script>([\s\S]*?)<\/script>/g)]
    .map(m => `'sha256-${createHash('sha256').update(m[1], 'utf8').digest('base64')}'`);
}

// 인라인 스크립트를 고치고 해시를 안 바꾸면 홈페이지 스크립트가 멈춘다 — 여기서 먼저 잡는다
test('every inline script on CSP pages is allowed by its hash', () => {
  for (const [file, paths] of [['index.html', ['/', '/index.html']], ['invite.html', ['/c/*', '/f/*', '/invite*']]]) {
    for (const path of paths) {
      const csp = cspFor(path);
      for (const hash of inlineHashes(file)) assert.ok(csp.includes(hash), `${path} 의 CSP 에 ${file} 인라인 스크립트 해시 ${hash}`);
      assert.match(csp, /object-src 'none'/);
      assert.match(csp, /frame-ancestors 'none'/);
      assert.doesNotMatch(csp, /script-src[^;]*'unsafe-inline'/, `${path} 는 인라인 스크립트를 통째로 열지 않는다`);
    }
  }
  // 워킹데이 페이지는 인라인 스크립트가 없다 — 생기면 여기서 걸린다
  assert.deepEqual(inlineHashes('11events/index.html'), []);
  assert.match(cspFor('/11events*'), /script-src 'self' https:\/\/static\.cloudflareinsights\.com https:\/\/www\.googletagmanager\.com;/);
  for (const path of ['/privacy*', '/terms*', '/delete-account*']) assert.match(cspFor(path), /script-src 'self' https:\/\/static\.cloudflareinsights\.com;/);
});

// Cloudflare Pages 는 같은 경로를 두 번 적으면 뒤 것만 쓴다 — 2026-11-02 '/' 의 CSP 가 아래 Cache-Control 줄에 덮였다
test('no path is listed twice in _headers', () => {
  const paths = headers.split('\n').filter(line => line.startsWith('/'));
  assert.deepEqual(paths.filter((p, i) => paths.indexOf(p) !== i), []);
});

// GA4 를 부르는 페이지는 CSP 가 Google 스크립트 · 전송을 열어 두어야 한다 — 안 열면 통계가 조용히 0 이다(2026-11-07).
// 홈페이지는 web/ga.js, 워킹데이 페이지는 11events/ga.js(측정 ID 는 assets/config.js 의 ga) — 둘은 같은 측정 ID 여야 한 속성에 모인다.
// 검토 페이지(/11events/review)와 초대 · 약관 · 지갑 페이지에는 넣지 않는다.
test('pages that load GA allow Google Analytics in their CSP and share one measurement ID', () => {
  const read = file => readFileSync(new URL(`../${file}`, import.meta.url), 'utf8');
  for (const [file, path, src] of [
    ['index.html', '/', './ga.js'], ['index.html', '/index.html', './ga.js'],
    ['11events/index.html', '/11events*', 'ga.js'], ['11events/proof.html', '/11events*', '/11events/ga.js'],
  ]) {
    assert.ok(read(file).includes(`src="${src}?v=`), `${file} 가 ${src} 를 부른다`);
    const csp = cspFor(path);
    assert.match(csp, /script-src [^;]*https:\/\/www\.googletagmanager\.com/, `${path} script-src`);
    assert.match(csp, /connect-src [^;]*https:\/\/\*\.google-analytics\.com/, `${path} connect-src`);
    assert.match(csp, /connect-src [^;]*https:\/\/\*\.analytics\.google\.com/, `${path} connect-src`);
  }
  for (const file of ['11events/review.html', 'invite.html', 'privacy.html', 'terms.html', 'wallet.html']) assert.doesNotMatch(read(file), /ga\.js/, `${file} 에는 GA 를 넣지 않는다`);
  const home = read('ga.js').match(/'(G-[A-Z0-9]+)'/)?.[1];
  const event = read('11events/assets/config.js').match(/ga: "(G-[A-Z0-9]+)"/)?.[1];
  assert.ok(home, 'web/ga.js 에 측정 ID');
  assert.equal(event, home, '워킹데이 설정의 ga 와 홈페이지 ga.js 가 같은 측정 ID');
  // 두 로더 모두 운영 주소에서만 · 광고 기능 끔(개인정보처리방침과 같게)
  for (const file of ['ga.js', '11events/ga.js']) {
    assert.match(read(file), /allow_google_signals: false/, `${file} Google 신호 끔`);
    assert.match(read(file), /allow_ad_personalization_signals: false/, `${file} 광고 맞춤 끔`);
    assert.match(read(file), /'stepupcrew\.com'/, `${file} 운영 주소에서만`);
  }
});
