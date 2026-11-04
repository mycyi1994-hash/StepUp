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
  for (const path of ['/privacy*', '/terms*', '/delete-account*']) assert.match(cspFor(path), /script-src 'self' https:\/\/static\.cloudflareinsights\.com;/);
});

// Cloudflare Pages 는 같은 경로를 두 번 적으면 뒤 것만 쓴다 — 2026-11-02 '/' 의 CSP 가 아래 Cache-Control 줄에 덮였다
test('no path is listed twice in _headers', () => {
  const paths = headers.split('\n').filter(line => line.startsWith('/'));
  assert.deepEqual(paths.filter((p, i) => paths.indexOf(p) !== i), []);
});
