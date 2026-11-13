/* StepUp 레이스 — 홈페이지 미니게임(2026-11-12).
   장애물 코스 세 판(예선 · 준결승 · 결승). 앞 두 판은 고스트 러너 사이에서 통과 자리 안에 들어와야 다음 판,
   결승을 완주하면 세 판 기록의 합이 순위표(서버 race_*, supabase/migrations/0103_race.sql)에 오른다.
   빌드: cd web && npm run build:game  →  lab-mja9j66fef/game.js(공개 전 내부 시험 — 홈 링크 없음) (three.js 를 함께 묶는다 — CSP 가 이 사이트 스크립트만 받는다) */
import * as THREE from 'three';

const CFG = window.STEPUP_RACE || {};
const DEV = /[?&]dev\b/.test(location.search);

// ── 몸놀림 값(미터 · 초) ─────────────────────────────────────────────
const GRAV = -30, JUMP_V = 11.2, SPEED = 8.6, ACC_GROUND = 60, ACC_AIR = 24, BOUNCE_V = 18.5;
const R = 0.42, H = 1.5, STEP_UP = 0.4, COYOTE = 0.1, JUMP_BUF = 0.13, DT = 1 / 120;

// ── 판 ───────────────────────────────────────────────────────────────
const ROUNDS = [
  // cut = 통과 마지막 자리 고스트가 들어오는 시각(초). 자동 조종 깨끗한 기록: 예선 23.5 · 준결승 19.3 · 결승 23.7초(2026-11-12)
  { name: '예선', title: '스타트 대시', runners: 20, slots: 12, cut: 36, spread: [0.74, 1.45] },
  { name: '준결승', title: '흔들 다리', runners: 12, slots: 6, cut: 31, spread: [0.78, 1.4] },
  { name: '결승', title: '정상 질주', runners: 6, slots: 6, cut: 44, spread: [0.62, 1.0], limit: 120 }
];

const COLORS = {
  top: 0xF2F6FF, side: 0x1B3A6B, alt: 0xD9E6FF, start: 0xFFD23F, hazard: 0xFF4F8B, hazard2: 0xFF9F1C,
  pusher: 0x1166FF, belt: 0x223152, pad: 0xFFD23F, tileA: 0x5CE1C6, tileB: 0x6FA8FF, disc: 0xB8F0FF, ball: 0xFF6B3D
};

// ════════════════════════════════════════════════════════════════════
//  땅(Solid): 상자 · 비탈 · 원판. 위에 서고, 옆은 밀어낸다. 움직이는 것은 서 있는 사람을 함께 옮긴다.
// ════════════════════════════════════════════════════════════════════
class Solid {
  constructor(o) {
    this.kind = o.kind || 'box';
    this.cx = o.x || 0; this.cz = o.z; this.top = o.top || 0; this.h = o.h || 1.2; this.yaw = o.yaw || 0;
    this.hx = (o.w || 4) / 2; this.hz = (o.d || 4) / 2; this.r = o.r || 0;
    this.y0 = o.y0; this.y1 = o.y1; // 비탈: 앞(z 큰 쪽) 높이 y0 → 뒤 y1
    this.move = o.move || null; // {ax:'x', amp, period, phase}
    this.spin = o.spin || 0; this.belt = o.belt || 0; this.bounce = !!o.bounce; this.fall = !!o.fall;
    this.static = !this.move && !this.spin && !this.fall;
    this.enabled = true; this.state = 'idle'; this.timer = 0; this.drop = 0;
    this.baseX = this.cx; this.baseZ = this.cz; this.baseTop = this.top;
    this.p = { cx: this.cx, cz: this.cz, top: this.top, yaw: this.yaw };
    this.vx = 0; this.vz = 0;
  }
  save() { this.p.cx = this.cx; this.p.cz = this.cz; this.p.top = this.top; this.p.yaw = this.yaw; }
  update(t, dt) {
    if (this.move) {
      const m = this.move, w = 2 * Math.PI / m.period, s = m.amp * Math.sin(w * t + (m.phase || 0));
      const v = m.amp * w * Math.cos(w * t + (m.phase || 0));
      if (m.ax === 'x') { this.cx = this.baseX + s; this.vx = v; } else { this.cz = this.baseZ + s; this.vz = v; }
    }
    if (this.spin) this.yaw = this.spin * t;
    if (this.fall) {
      if (this.state === 'shake') { this.timer -= dt; if (this.timer <= 0) { this.state = 'drop'; this.timer = 3.2; this.enabled = false; } }
      else if (this.state === 'drop') { this.timer -= dt; this.drop += dt; if (this.timer <= 0) { this.state = 'idle'; this.enabled = true; this.drop = 0; } }
    }
  }
  trigger() { if (this.fall && this.state === 'idle') { this.state = 'shake'; this.timer = 0.42; } }
  toLocal(x, z, pose) {
    const P = pose || this, dx = x - P.cx, dz = z - P.cz, c = Math.cos(-P.yaw), s = Math.sin(-P.yaw);
    return [dx * c - dz * s, dx * s + dz * c];
  }
  toWorld(lx, lz, pose) {
    const P = pose || this, c = Math.cos(P.yaw), s = Math.sin(P.yaw);
    return [P.cx + lx * c - lz * s, P.cz + lx * s + lz * c];
  }
  inside(x, z, m) {
    if (this.kind === 'disc') { const dx = x - this.cx, dz = z - this.cz; return dx * dx + dz * dz <= (this.r + m) * (this.r + m); }
    const [lx, lz] = this.toLocal(x, z);
    return Math.abs(lx) <= this.hx + m && Math.abs(lz) <= this.hz + m;
  }
  topAt(x, z) {
    if (this.kind !== 'ramp') return this.top;
    const z0 = this.cz + this.hz, z1 = this.cz - this.hz, f = Math.min(1, Math.max(0, (z0 - z) / (z0 - z1)));
    return this.y0 + (this.y1 - this.y0) * f;
  }
  bottom() { return (this.kind === 'ramp' ? Math.min(this.y0, this.y1) : this.top) - this.h; }
  // 옆에서 부딪히면 반지름만큼 밀어낸다. 밀었으면 true
  pushOut(pos) {
    if (this.kind === 'disc') {
      const dx = pos.x - this.cx, dz = pos.z - this.cz, d = Math.hypot(dx, dz), need = this.r + R;
      if (d >= need) return false;
      const k = d > 1e-5 ? need / d : 1; pos.x = this.cx + (d > 1e-5 ? dx * k : need); pos.z = this.cz + (d > 1e-5 ? dz * k : 0);
      return true;
    }
    const [lx, lz] = this.toLocal(pos.x, pos.z);
    const qx = Math.max(-this.hx, Math.min(this.hx, lx)), qz = Math.max(-this.hz, Math.min(this.hz, lz));
    let dx = lx - qx, dz = lz - qz, d = Math.hypot(dx, dz), nx, nz;
    if (d >= R) return false;
    if (d > 1e-5) { nx = qx + dx / d * R; nz = qz + dz / d * R; }
    else { // 가운데가 안에 들어왔다 — 가까운 면으로
      const px = this.hx - Math.abs(lx), pz = this.hz - Math.abs(lz);
      if (px < pz) { nx = Math.sign(lx || 1) * (this.hx + R); nz = lz; } else { nx = lx; nz = Math.sign(lz || 1) * (this.hz + R); }
    }
    const [wx, wz] = this.toWorld(nx, nz); pos.x = wx; pos.z = wz;
    return true;
  }
  // 지난 자세 → 지금 자세로 위에 선 사람을 옮긴다
  carry(pos) {
    if (this.static) return;
    if (this.kind === 'disc' || this.spin) {
      const dx = pos.x - this.p.cx, dz = pos.z - this.p.cz, a = this.yaw - this.p.yaw, c = Math.cos(a), s = Math.sin(a);
      pos.x = this.cx + dx * c - dz * s; pos.z = this.cz + dx * s + dz * c;
    } else { pos.x += this.cx - this.p.cx; pos.z += this.cz - this.p.cz; }
    pos.y += this.top - this.p.top;
  }
}

// ════════════════════════════════════════════════════════════════════
//  장애물(Hazard): 닿으면 튕겨 낸다
// ════════════════════════════════════════════════════════════════════
function segDist(px, pz, ax, az, bx, bz) {
  const vx = bx - ax, vz = bz - az, l2 = vx * vx + vz * vz, t = l2 ? Math.max(0, Math.min(1, ((px - ax) * vx + (pz - az) * vz) / l2)) : 0;
  const qx = ax + vx * t, qz = az + vz * t; return [Math.hypot(px - qx, pz - qz), qx, qz];
}

class Sweeper { // 바닥을 도는 막대 — 뛰어 넘는다
  constructor(o) { Object.assign(this, { x: 0, z: 0, base: 0, len: 6, w: o.w || 1, y0: 0.2, y1: 0.75, spin: 1.8 }, o); this.a = 0; }
  update(t) { this.a = this.spin * t + (this.phase || 0); }
  hit(pl) {
    const yb = this.base + this.y0, yt = this.base + this.y1;
    if (pl.pos.y > yt || pl.pos.y + H < yb) return null;
    const ex = Math.cos(this.a) * this.len, ez = Math.sin(this.a) * this.len;
    const [d, qx, qz] = segDist(pl.pos.x, pl.pos.z, this.x - ex, this.z - ez, this.x + ex, this.z + ez);
    if (d > R + 0.28) return null;
    const side = Math.sign((pl.pos.x - this.x) * -Math.sin(this.a) + (pl.pos.z - this.z) * Math.cos(this.a)) || 1;
    const tx = -Math.sin(this.a) * side, tz = Math.cos(this.a) * side;
    const k = (R + 0.3) / Math.max(d, 1e-3); pl.pos.x = qx + (pl.pos.x - qx) * k; pl.pos.z = qz + (pl.pos.z - qz) * k;
    return { vx: tx * 10, vz: tz * 10, vy: 6.5, stun: 0.42 };
  }
}

class Hammer { // 흔들리는 망치 — 비켜 갈 때를 노린다
  constructor(o) { Object.assign(this, { x: 0, z: 0, py: 7.5, len: 6, r: 1.05, amp: 1.0, period: 2.2, phase: 0 }, o); this.hx = 0; this.hy = 0; this.v = 0; }
  update(t) {
    const w = 2 * Math.PI / this.period, f = this.amp * Math.sin(w * t + this.phase);
    this.f = f; this.v = this.amp * w * Math.cos(w * t + this.phase) * this.len * Math.cos(f);
    this.hx = this.x + this.len * Math.sin(f); this.hy = this.py - this.len * Math.cos(f);
  }
  hit(pl) {
    const cy = Math.max(pl.pos.y + R, Math.min(pl.pos.y + H - R, this.hy));
    const d = Math.hypot(pl.pos.x - this.hx, cy - this.hy, pl.pos.z - this.z);
    if (d > this.r + R) return null;
    const dir = Math.sign(this.v) || Math.sign(pl.pos.x - this.hx) || 1;
    return { vx: dir * 14, vz: 1.5, vy: 7.5, stun: 0.55 };
  }
}

class Balls { // 비탈을 굴러 내려오는 공
  constructor(o) { Object.assign(this, o); this.list = []; this.next = 0.4; this.rnd = mulberry(o.seed || 7); }
  update(t, dt, world, pz) {
    this.next -= dt;
    if (this.next <= 0 && !(pz < this.zTop + 6)) {
      this.next = this.every * (0.7 + this.rnd() * 0.6);
      const r = 1.15 + this.rnd() * 0.5;
      this.list.push({ x: (this.rnd() * 2 - 1) * (this.halfW - r), z: this.zTop + 1, r, v: this.speed * (0.85 + this.rnd() * 0.35), y: 0, spin: 0, mesh: null });
    }
    for (const b of this.list) {
      b.z += b.v * dt; b.spin += b.v * dt / b.r;
      b.y = this.ramp.topAt(b.x, b.z) + b.r;
      if (b.z > this.zEnd) b.dead = true;
    }
    for (const b of this.list) if (b.dead && b.mesh) { world.group.remove(b.mesh); b.mesh = null; }
    this.list = this.list.filter(b => !b.dead);
  }
  hit(pl) {
    for (const b of this.list) {
      const cy = Math.max(pl.pos.y + R, Math.min(pl.pos.y + H - R, b.y));
      if (Math.hypot(pl.pos.x - b.x, cy - b.y, pl.pos.z - b.z) < b.r + R) return { vx: Math.sign(pl.pos.x - b.x) * 3, vz: b.v * 1.25, vy: 7, stun: 0.5 };
    }
    return null;
  }
}

function mulberry(a) { return function () { a |= 0; a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }

// ════════════════════════════════════════════════════════════════════
//  코스
// ════════════════════════════════════════════════════════════════════
function newWorld() { return { solids: [], hazards: [], balls: null, cps: [], finishZ: 0, finishY: 0, spawn: { x: 0, y: 0, z: 3 }, killY: -14, deco: [], group: new THREE.Group(), length: 0 }; }
// z0(앞, 큰 값) → z1(뒤, 작은 값) 상자
function slab(W, z0, z1, o = {}) {
  const s = new Solid(Object.assign({ z: (z0 + z1) / 2, d: z0 - z1, w: 12, top: 0, h: 1.4 }, o));
  W.solids.push(s); return s;
}

function round1() {
  const W = newWorld();
  slab(W, 8, -10, { w: 16, color: COLORS.start, start: true });
  slab(W, -10, -44, { w: 12 });
  W.hazards.push(new Sweeper({ z: -19, len: 6.4, spin: 1.7 }), new Sweeper({ z: -34, len: 6.4, spin: -1.95, phase: 1 }));
  W.cps.push({ z: -44, x: 0, y: 0 });
  slab(W, -44, -52, { w: 12 });
  slab(W, -55, -62, { w: 10, top: 0.6 });
  slab(W, -65.5, -73, { w: 9, top: 1.2 });
  slab(W, -76, -86, { w: 12, top: 1.2 });
  W.cps.push({ z: -76, x: 0, y: 1.2 });
  slab(W, -86, -120, { w: 12, top: 1.2 });
  [[-94, 0], [-104, 2.1], [-114, 4.2]].forEach(([z, ph]) => W.solids.push(new Solid({ x: 0, z, w: 5, d: 1.2, top: 3.4, h: 2.2, move: { ax: 'x', amp: 3.6, period: 2.4, phase: ph }, color: COLORS.pusher })));
  W.cps.push({ z: -120, x: 0, y: 1.2 });
  slab(W, -120, -146, { w: 10, top: 1.2, belt: 3.2, color: COLORS.belt });
  [[-127, -2.4], [-133, 2.4], [-139.5, 0]].forEach(([z, x]) => W.solids.push(new Solid({ x, z, w: 3.2, d: 1.2, top: 2.1, h: 0.9, color: COLORS.hazard2 })));
  slab(W, -146, -153, { w: 10, top: 1.2 });
  W.solids.push(new Solid({ x: 0, z: -150.5, w: 3, d: 3, top: 1.35, h: 0.15, bounce: true, color: COLORS.pad }));
  W.cps.push({ z: -146, x: 0, y: 1.2 });
  slab(W, -155, -178, { w: 14, top: 5.4 });
  W.finishZ = -166; W.finishY = 5.4;
  return W;
}

function round2() {
  const W = newWorld();
  slab(W, 8, -8, { w: 16, color: COLORS.start, start: true });
  slab(W, -8, -46, { w: 2.8 });
  [[-16, 0], [-26, 1.7], [-36, 3.4]].forEach(([z, ph]) => W.hazards.push(new Hammer({ z, phase: ph })));
  slab(W, -46, -52, { w: 8 });
  W.cps.push({ z: -46, x: 0, y: 0 });
  [-56.5, -63, -69.5, -76].forEach((z, i) => W.solids.push(new Solid({ x: 0, z, w: 4.2, d: 4, top: 0, h: 1, move: { ax: 'x', amp: 3.4, period: 3.2, phase: i % 2 ? Math.PI : 0 }, color: COLORS.pusher })));
  slab(W, -80, -88, { w: 10 });
  W.cps.push({ z: -80, x: 0, y: 0 });
  W.solids.push(new Solid({ kind: 'disc', x: 0, z: -95, r: 5.6, top: 0.3, h: 1, spin: 0.9, color: COLORS.disc }));
  W.hazards.push(new Sweeper({ z: -95, base: 0.3, len: 5.3, spin: 2.3, w: 0.8 }));
  W.solids.push(new Solid({ kind: 'disc', x: 0, z: -107.6, r: 5.6, top: 0.3, h: 1, spin: -1.0, color: COLORS.disc }));
  slab(W, -114.6, -120, { w: 10 });
  W.cps.push({ z: -114.6, x: 0, y: 0 });
  for (let row = 0; row < 7; row++) for (let col = 0; col < 4; col++) {
    W.solids.push(new Solid({ x: -4.35 + col * 2.9, z: -122.2 - row * 2.9, w: 2.6, d: 2.6, top: 0, h: 0.5, fall: true, color: (row + col) % 2 ? COLORS.tileA : COLORS.tileB }));
  }
  slab(W, -141.4, -162, { w: 14 });
  W.finishZ = -149; W.finishY = 0;
  return W;
}

function round3() {
  const W = newWorld();
  slab(W, 8, -8, { w: 16, color: COLORS.start, start: true });
  const ramp = new Solid({ kind: 'ramp', z: -33, d: 50, w: 14, y0: 0, y1: 10, h: 2.5 });
  W.solids.push(ramp);
  W.balls = new Balls({ ramp, zTop: -58, zEnd: -7.5, halfW: 7, speed: 10.5, every: 0.8, seed: 11 });
  slab(W, -58, -100, { w: 14, top: 10, h: 2.5 });
  W.cps.push({ z: -58, x: 0, y: 10 });
  W.hazards.push(new Sweeper({ z: -77, base: 10, len: 6.9, spin: 2.0 }), new Sweeper({ z: -91, base: 10, len: 6.9, spin: -2.35, phase: 0.7 }));
  W.cps.push({ z: -98.5, rz: -99.2, x: 0, y: 10 });
  [[-103.5, -3, 10], [-109, 3, 10.6], [-114.5, -3, 11.2], [-120, 3, 11.8]].forEach(([z, x, top]) => W.solids.push(new Solid({ x, z, w: 3.2, d: 3.2, top, h: 1.2, color: COLORS.alt })));
  slab(W, -123, -130, { w: 10, top: 12.4 });
  W.cps.push({ z: -123, x: 0, y: 12.4 });
  slab(W, -130, -150, { w: 10, top: 12.4, belt: 3.5, color: COLORS.belt });
  W.solids.push(new Solid({ x: 0, z: -140, w: 4.6, d: 1.2, top: 14.6, h: 2.2, move: { ax: 'x', amp: 3.2, period: 2.0 }, color: COLORS.pusher }));
  slab(W, -150, -166, { w: 14, top: 12.4 });
  W.finishZ = -157; W.finishY = 12.4; W.crown = true;
  return W;
}

const BUILD = [round1, round2, round3];

// ════════════════════════════════════════════════════════════════════
//  그리기
// ════════════════════════════════════════════════════════════════════
const MAT = {};
function mat(color, opts) {
  const k = color + JSON.stringify(opts || {});
  return MAT[k] || (MAT[k] = new THREE.MeshLambertMaterial(Object.assign({ color }, opts || {})));
}
function stripeTexture(a, b, n, vertical) {
  const c = document.createElement('canvas'); c.width = c.height = 64; const g = c.getContext('2d');
  for (let i = 0; i < n; i++) { g.fillStyle = i % 2 ? b : a; if (vertical) g.fillRect(i * 64 / n, 0, 64 / n, 64); else g.fillRect(0, i * 64 / n, 64, 64 / n); }
  const t = new THREE.CanvasTexture(c); t.wrapS = t.wrapT = THREE.RepeatWrapping; t.colorSpace = THREE.SRGBColorSpace; return t;
}
function textTexture(text, bg, fg, w = 512, h = 128) {
  const c = document.createElement('canvas'); c.width = w; c.height = h; const g = c.getContext('2d');
  g.fillStyle = bg; g.fillRect(0, 0, w, h);
  for (let i = 0; i < w / 32; i++) for (let j = 0; j < 2; j++) if ((i + j) % 2) { g.fillStyle = 'rgba(255,255,255,.12)'; g.fillRect(i * 32, j * (h - 16), 32, 16); }
  g.fillStyle = fg; g.font = `900 ${Math.round(h * 0.5)}px system-ui, sans-serif`; g.textAlign = 'center'; g.textBaseline = 'middle'; g.fillText(text, w / 2, h / 2 + 4);
  const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; return t;
}

function drawWorld(W) {
  const G = W.group;
  let minTop = 0;
  for (const s of W.solids) {
    let mesh;
    const topColor = s.colorTop || (s.color != null ? s.color : COLORS.top);
    if (s.kind === 'disc') {
      const geo = new THREE.CylinderGeometry(s.r, s.r, s.h, 40);
      mesh = new THREE.Mesh(geo, [mat(COLORS.side), mat(topColor), mat(COLORS.side)]);
      const ring = new THREE.Mesh(new THREE.RingGeometry(s.r * 0.15, s.r * 0.95, 40, 1, 0, Math.PI), mat(0x7FD8FF));
      ring.rotation.x = -Math.PI / 2; ring.position.y = s.h / 2 + 0.01; mesh.add(ring);
    } else if (s.kind === 'ramp') {
      const len = Math.hypot(s.hz * 2, s.y1 - s.y0), ang = Math.atan2(s.y1 - s.y0, s.hz * 2);
      const tex = stripeTexture('#F2F6FF', '#DCE7FF', 2, false); tex.repeat.set(1, 10);
      mesh = new THREE.Mesh(new THREE.BoxGeometry(s.hx * 2, s.h, len), [mat(COLORS.side), mat(COLORS.side), new THREE.MeshLambertMaterial({ map: tex }), mat(COLORS.side), mat(COLORS.side), mat(COLORS.side)]);
      mesh.rotation.x = ang; s.meshY = (s.y0 + s.y1) / 2 - s.h / 2 * Math.cos(ang);
    } else {
      let topMat = mat(topColor);
      if (s.belt) { const tex = stripeTexture('#2B3F6A', '#7FA6FF', 4, false); tex.repeat.set(1, (s.hz * 2) / 3); topMat = new THREE.MeshLambertMaterial({ map: tex }); s.beltTex = tex; }
      const side = s.bounce ? mat(0xE0A800) : s.fall ? mat(0x2A4C80) : s.move ? mat(0x0B47C4) : mat(COLORS.side);
      mesh = new THREE.Mesh(new THREE.BoxGeometry(s.hx * 2, s.h, s.hz * 2), [side, side, topMat, side, side, side]);
      if (s.start) {
        const line = new THREE.Mesh(new THREE.PlaneGeometry(s.hx * 2, 0.5), mat(0xffffff));
        line.rotation.x = -Math.PI / 2; line.position.set(0, s.h / 2 + 0.01, s.hz - 5.5); mesh.add(line);
      }
    }
    s.mesh = mesh; G.add(mesh);
    minTop = Math.min(minTop, s.kind === 'ramp' ? s.y0 : s.top);
  }
  for (const h of W.hazards) {
    if (h instanceof Sweeper) {
      const g = new THREE.Group();
      const post = new THREE.Mesh(new THREE.CylinderGeometry(0.45, 0.6, 1.2, 16), mat(COLORS.side)); post.position.y = 0.6; g.add(post);
      const bar = new THREE.Mesh(new THREE.BoxGeometry(h.len * 2, h.y1 - h.y0, 0.5), mat(COLORS.hazard)); bar.position.y = (h.y0 + h.y1) / 2; g.add(bar);
      const cap = new THREE.Mesh(new THREE.CylinderGeometry(0.55, 0.55, 0.2, 16), mat(COLORS.start)); cap.position.y = 1.25; g.add(cap);
      g.position.set(h.x, h.base, h.z); h.mesh = g; G.add(g);
    } else if (h instanceof Hammer) {
      const pivot = new THREE.Group(); pivot.position.set(h.x, h.py, h.z);
      const arm = new THREE.Mesh(new THREE.CylinderGeometry(0.12, 0.12, h.len, 8), mat(0x9AA9C4)); arm.position.y = -h.len / 2; pivot.add(arm);
      const head = new THREE.Mesh(new THREE.CylinderGeometry(h.r, h.r, h.r * 2.3, 20), [mat(COLORS.hazard), mat(0xffffff), mat(0xffffff)]);
      head.rotation.z = Math.PI / 2; head.position.y = -h.len; pivot.add(head);
      const beam = new THREE.Mesh(new THREE.BoxGeometry(3, 0.4, 0.4), mat(COLORS.side)); beam.position.set(0, 0, 0); pivot.add(beam);
      h.mesh = pivot; G.add(pivot);
      const frame = new THREE.Group();
      [-7, 7].forEach(x => { const p = new THREE.Mesh(new THREE.BoxGeometry(0.5, h.py + 4, 0.5), mat(COLORS.side)); p.position.set(x, (h.py - 4) / 2, h.z); frame.add(p); });
      const top = new THREE.Mesh(new THREE.BoxGeometry(14.5, 0.5, 0.5), mat(COLORS.side)); top.position.set(0, h.py + 0.2, h.z); frame.add(top);
      G.add(frame);
    }
  }
  // 결승선 문
  const arch = new THREE.Group();
  [-6.5, 6.5].forEach(x => { const p = new THREE.Mesh(new THREE.BoxGeometry(0.8, 6, 0.8), mat(COLORS.pusher)); p.position.set(x, 3, 0); arch.add(p); });
  const banner = new THREE.Mesh(new THREE.BoxGeometry(14, 1.6, 0.6), [mat(COLORS.pusher), mat(COLORS.pusher), mat(COLORS.pusher), mat(COLORS.pusher),
    new THREE.MeshBasicMaterial({ map: textTexture(W.crown ? 'CHAMPION' : 'FINISH', '#1166FF', '#FFFFFF') }), new THREE.MeshBasicMaterial({ map: textTexture(W.crown ? 'CHAMPION' : 'FINISH', '#1166FF', '#FFFFFF') })]);
  banner.position.y = 6.2; arch.add(banner);
  const line = new THREE.Mesh(new THREE.PlaneGeometry(14, 1.2), new THREE.MeshBasicMaterial({ map: (() => { const t = stripeTexture('#111', '#fff', 8, true); t.repeat.set(4, 1); return t; })() }));
  line.rotation.x = -Math.PI / 2; line.position.y = 0.02; arch.add(line);
  arch.position.set(0, W.finishY, W.finishZ); G.add(arch);
  if (W.crown) {
    const crown = new THREE.Group();
    const ring = new THREE.Mesh(new THREE.CylinderGeometry(1.1, 1.3, 0.9, 6, 1, true), new THREE.MeshLambertMaterial({ color: COLORS.start, side: THREE.DoubleSide }));
    crown.add(ring);
    for (let i = 0; i < 6; i++) { const sp = new THREE.Mesh(new THREE.ConeGeometry(0.3, 0.8, 6), mat(COLORS.start)); const a = i / 6 * Math.PI * 2; sp.position.set(Math.cos(a) * 1.15, 0.8, Math.sin(a) * 1.15); crown.add(sp); }
    crown.position.set(0, W.finishY + 9.2, W.finishZ); W.crownMesh = crown; G.add(crown);
  }
  // 아래 물 · 구름
  const sea = new THREE.Mesh(new THREE.PlaneGeometry(600, 600), new THREE.MeshLambertMaterial({ color: 0x2F7BFF, emissive: 0x0A2A7A }));
  sea.rotation.x = -Math.PI / 2; sea.position.set(0, minTop - 16, -80); G.add(sea);
  W.killY = minTop - 13;
  const rnd = mulberry(5);
  for (let i = 0; i < 18; i++) {
    const cl = new THREE.Group(), n = 3 + (rnd() * 3 | 0);
    for (let j = 0; j < n; j++) { const b = new THREE.Mesh(new THREE.SphereGeometry(2 + rnd() * 2.5, 12, 10), mat(0xffffff)); b.position.set(j * 2.6, rnd() * 1.2, rnd() * 1.5); cl.add(b); }
    const side = rnd() < 0.5 ? -1 : 1;
    cl.position.set(side * (26 + rnd() * 40), -6 + rnd() * 26, 20 - rnd() * 220); G.add(cl);
  }
  W.length = W.spawn.z - W.finishZ;
}

function syncWorld(W) {
  for (const s of W.solids) {
    const m = s.mesh;
    if (s.kind === 'ramp') { m.position.set(s.cx, s.meshY, s.cz); continue; }
    let shake = 0;
    if (s.fall && s.state === 'shake') shake = Math.sin(performance.now() / 18) * 0.06;
    m.position.set(s.cx + shake, s.top - s.h / 2 - (s.fall ? s.drop * s.drop * 9 : 0), s.cz);
    m.rotation.y = -s.yaw;
    m.visible = !(s.fall && s.drop > 1.6);
    if (s.beltTex) s.beltTex.offset.y = -(performance.now() / 1000) * s.belt / 3;
  }
  for (const h of W.hazards) {
    if (h instanceof Sweeper) h.mesh.rotation.y = -h.a;
    else if (h instanceof Hammer) h.mesh.rotation.z = h.f;
  }
  if (W.balls) for (const b of W.balls.list) {
    if (!b.mesh) {
      b.mesh = new THREE.Mesh(new THREE.SphereGeometry(b.r, 20, 14), [mat(COLORS.ball)]);
      const band = new THREE.Mesh(new THREE.TorusGeometry(b.r * 0.98, 0.12, 6, 24), mat(0xffffff)); band.rotation.y = Math.PI / 2; b.mesh.add(band);
      W.group.add(b.mesh);
    }
    b.mesh.position.set(b.x, b.y, b.z); b.mesh.rotation.x = b.spin;
  }
  if (W.crownMesh) { W.crownMesh.rotation.y += 0.02; W.crownMesh.position.y = W.finishY + 9.2 + Math.sin(performance.now() / 500) * 0.3; }
}

// 러너 모양 — 머리 · 몸 · 머리띠 · 큰 운동화(StepUp 신발)
function makeRunner(color, ghost) {
  const o = ghost ? { transparent: true, opacity: 0.38, depthWrite: false } : undefined;
  const g = new THREE.Group(), body = new THREE.Group(); g.add(body);
  const torso = new THREE.Mesh(new THREE.CapsuleGeometry(0.34, 0.36, 6, 14), mat(color, o)); torso.position.y = 0.82; body.add(torso);
  const shirt = new THREE.Mesh(new THREE.CylinderGeometry(0.355, 0.355, 0.1, 16), mat(0xffffff, o)); shirt.position.y = 0.84; body.add(shirt);
  const head = new THREE.Mesh(new THREE.SphereGeometry(0.33, 18, 14), mat(0xFFD9B8, o)); head.position.y = 1.36; body.add(head);
  const hair = new THREE.Mesh(new THREE.SphereGeometry(0.345, 18, 10, 0, Math.PI * 2, 0, Math.PI * 0.42), mat(0x2A1B12, o)); hair.position.y = 1.38; body.add(hair);
  const band = new THREE.Mesh(new THREE.TorusGeometry(0.335, 0.055, 6, 22), mat(color, o)); band.rotation.x = Math.PI / 2; band.position.y = 1.45; body.add(band);
  if (!ghost) [-0.11, 0.11].forEach(x => { const e = new THREE.Mesh(new THREE.SphereGeometry(0.045, 8, 6), mat(0x0B1A33)); e.position.set(x, 1.38, -0.3); body.add(e); });
  const legs = [];
  [-0.17, 0.17].forEach(x => {
    const leg = new THREE.Group(); leg.position.set(x, 0.5, 0);
    const shin = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.08, 0.34, 6), mat(0x0B1A33, o)); shin.position.y = -0.2; leg.add(shin);
    const shoe = new THREE.Mesh(new THREE.BoxGeometry(0.26, 0.17, 0.5), mat(0xffffff, o)); shoe.position.set(0, -0.4, -0.08); leg.add(shoe);
    const sole = new THREE.Mesh(new THREE.BoxGeometry(0.27, 0.06, 0.52), mat(color, o)); sole.position.set(0, -0.49, -0.08); leg.add(sole);
    const swoosh = new THREE.Mesh(new THREE.BoxGeometry(0.275, 0.04, 0.26), mat(color, o)); swoosh.position.set(0, -0.38, -0.02); leg.add(swoosh);
    body.add(leg); legs.push(leg);
  });
  const arms = [];
  [-0.43, 0.43].forEach(x => { const a = new THREE.Mesh(new THREE.CapsuleGeometry(0.075, 0.26, 4, 8), mat(color, o)); a.position.set(x, 0.86, 0); body.add(a); arms.push(a); });
  g.userData = { body, legs, arms, phase: 0 };
  return g;
}
function animateRunner(g, speed, air, dt) {
  const u = g.userData; u.phase += dt * (4 + speed * 1.4);
  const k = air ? 0.25 : Math.min(1, speed / 6), s = Math.sin(u.phase) * 0.9 * k;
  u.legs[0].rotation.x = s; u.legs[1].rotation.x = -s; u.arms[0].rotation.x = -s; u.arms[1].rotation.x = s;
  u.body.position.y = air ? 0 : Math.abs(Math.sin(u.phase)) * 0.08 * k;
}

// ════════════════════════════════════════════════════════════════════
//  게임
// ════════════════════════════════════════════════════════════════════
const $ = id => document.getElementById(id);
const canvas = $('stage');
const renderer = new THREE.WebGLRenderer({ canvas, antialias: (window.devicePixelRatio || 1) < 2, alpha: true, powerPreference: 'high-performance' });
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.75));
renderer.outputColorSpace = THREE.SRGBColorSpace;
const scene = new THREE.Scene();
scene.fog = new THREE.Fog(0xA9D8FF, 70, 190);
scene.add(new THREE.HemisphereLight(0xE6F3FF, 0x3A4A6B, 1.6));
const sun = new THREE.DirectionalLight(0xffffff, 1.7); sun.position.set(6, 12, 5); scene.add(sun);
const camera = new THREE.PerspectiveCamera(60, 1, 0.1, 400);

const player = { pos: new THREE.Vector3(), vel: new THREE.Vector3(), ground: null, onGround: false, coyote: 0, jumpBuf: 0, stun: 0, safe: 0, yaw: 0, cp: null, falls: 0 };
const runnerMesh = makeRunner(0x1166FF, false); scene.add(runnerMesh);
const blob = new THREE.Mesh(new THREE.CircleGeometry(0.5, 20), new THREE.MeshBasicMaterial({ color: 0x000000, transparent: true, opacity: 0.28, depthWrite: false }));
blob.rotation.x = -Math.PI / 2; scene.add(blob);

const S = { mode: 'title', round: 0, world: null, t: 0, acc: 0, playT: 0, splits: [], total: 0, bots: [], finishers: [], intro: 0, paused: false, run: null, runReq: null, lastPos: 1 };
const input = { x: 0, z: 0, keys: new Set(), stickX: 0, stickZ: 0, jumpHeld: false };

function setWorld(i) {
  if (S.world) { scene.remove(S.world.group); S.world.group.traverse(o => { if (o.geometry) o.geometry.dispose(); }); }
  const W = BUILD[i](); drawWorld(W); scene.add(W.group); S.world = W; S.t = 0;
  for (const s of W.solids) { s.update(0, 0); s.save(); }
  for (const h of W.hazards) h.update(0);
  resetPlayer(W.spawn);
}
function resetPlayer(p) {
  player.pos.set(p.x, p.y + 0.01, p.z); player.vel.set(0, 0, 0); player.ground = null; player.onGround = false;
  player.stun = 0; player.safe = 0.9; player.yaw = 0; player.cp = null;
}

// 고스트 러너 — 판마다 완주 시각을 미리 정하고 코스를 따라 그 시각에 들어온다(부딪히지 않는다)
const GHOST_COLORS = [0xFF4F8B, 0xFF9F1C, 0x5CE1C6, 0xA77BFF, 0xFFD23F, 0x3DDC84, 0xFF6B6B, 0x4DD4FF, 0xF78FB3, 0x9BE15D, 0xFFB86B, 0x8E9BFF];
function makeBots(R0) {
  for (const b of S.bots) scene.remove(b.mesh);
  S.bots = [];
  const n = R0.runners - 1, rnd = mulberry((Date.now() & 0xffff) + S.round * 97);
  const [lo, hi] = R0.spread, slotIdx = R0.slots - 1;
  const f = [];
  for (let i = 0; i < n; i++) f.push(lo + (hi - lo) * (i / (n - 1)) + (rnd() - 0.5) * 0.06);
  f.sort((a, b) => a - b);
  // 통과 마지막 자리 고스트가 cut 초 근처로 들어오게 맞춘다(± 6%)
  const pivot = f[Math.min(slotIdx, n - 1)], adj = (0.97 + rnd() * 0.06) / pivot;
  for (let i = 0; i < n; i++) {
    const T = R0.cut * f[i] * adj;
    const m = makeRunner(GHOST_COLORS[i % GHOST_COLORS.length], true); scene.add(m);
    S.bots.push({ T, mesh: m, lane: (rnd() * 2 - 1) * 4.5, wob: rnd() * 6.28, wob2: 0.6 + rnd() * 0.8, s: 0, done: false, y: 0, vy: 0 });
  }
}
function groundTop(x, z, below) {
  let best = -Infinity;
  for (const s of S.world.solids) if (s.enabled && s.inside(x, z, 0.1)) { const t = s.topAt(x, z); if (t <= below + 0.05 && t > best) best = t; }
  return best;
}
function updateBots(dt) {
  const W = S.world, L = W.length;
  for (const b of S.bots) {
    if (b.done) { b.mesh.visible = false; continue; }
    const u = Math.min(1, S.playT / b.T);
    const prog = u + 0.025 * Math.sin(u * 13 + b.wob) * (1 - u) * u * 4;
    b.s = Math.min(L, prog * L);
    const z = W.spawn.z - b.s;
    let half = 6; for (const s of W.solids) if (s.static && s.kind !== 'disc' && s.inside(0, z, 0) && !s.belt) { half = Math.min(half, s.hx - 0.6); break; }
    const x = Math.max(-half, Math.min(half, b.lane * Math.min(1, Math.max(0.15, half / 6)) + Math.sin(S.playT * b.wob2 + b.wob) * 0.6));
    const gt = groundTop(x, z, Infinity), air = gt === -Infinity;
    if (!air) b.y += (gt - b.y) * Math.min(1, dt * 10);
    b.mesh.position.set(x, b.y + (air ? 0.4 : 0), z);
    animateRunner(b.mesh, 7, air, dt);
    if (u >= 1) { b.done = true; S.finishers.push({ bot: true }); }
  }
}

function hitPlayer(k) {
  if (player.safe > 0 || !k) return;
  player.vel.x = k.vx; player.vel.z = k.vz; player.vel.y = Math.max(player.vel.y, k.vy);
  player.stun = k.stun; player.safe = 0.35; player.onGround = false; player.ground = null; sfx('hit');
}

function step(dt) {
  const W = S.world;
  S.t += dt;
  for (const s of W.solids) { s.save(); s.update(S.t, dt); }
  for (const h of W.hazards) h.update(S.t);
  if (W.balls) W.balls.update(S.t, dt, W, player.pos.z);
  const p = player.pos, v = player.vel;
  if (player.ground && player.ground.enabled) player.ground.carry(p);
  // 입력
  let ix = input.x, iz = input.z; const il = Math.hypot(ix, iz); if (il > 1) { ix /= il; iz /= il; }
  const acc = (player.onGround ? ACC_GROUND : ACC_AIR) * (player.stun > 0 ? 0.12 : 1);
  const tx = ix * SPEED, tz = iz * SPEED;
  const dvx = tx - v.x, dvz = tz - v.z, dl = Math.hypot(dvx, dvz), lim = acc * dt;
  if (dl > lim) { v.x += dvx / dl * lim; v.z += dvz / dl * lim; } else { v.x = tx; v.z = tz; }
  if (player.onGround && player.ground && player.ground.belt) p.z += player.ground.belt * dt;
  player.coyote = player.onGround ? COYOTE : Math.max(0, player.coyote - dt);
  player.jumpBuf = Math.max(0, player.jumpBuf - dt);
  if (player.jumpBuf > 0 && player.coyote > 0 && player.stun <= 0) { v.y = JUMP_V; player.coyote = 0; player.jumpBuf = 0; player.onGround = false; player.ground = null; sfx('jump'); }
  v.y = Math.max(-45, v.y + GRAV * dt);
  const prevY = p.y;
  p.x += v.x * dt; p.y += v.y * dt; p.z += v.z * dt;
  player.stun = Math.max(0, player.stun - dt); player.safe = Math.max(0, player.safe - dt);
  // 땅
  let best = null, bestTop = -Infinity;
  if (v.y <= 0.01) for (const s of W.solids) {
    if (!s.enabled) continue;
    const top = s.topAt(p.x, p.z);
    if (!s.inside(p.x, p.z, top > prevY + 0.01 ? R : R * 0.5)) continue;
    const allow = s.kind === 'ramp' || s === player.ground ? STEP_UP + 0.3 : STEP_UP;
    if (p.y <= top + 0.02 && prevY >= top - allow && top > bestTop) { best = s; bestTop = top; }
  }
  const wasGround = player.onGround;
  player.onGround = false;
  if (best) {
    p.y = bestTop; v.y = 0; player.onGround = true; player.ground = best;
    if (best.bounce) { v.y = BOUNCE_V; player.onGround = false; player.ground = null; sfx('boing'); }
    else { best.trigger(); if (!wasGround) sfx('land'); }
  } else player.ground = null;
  // 옆
  for (const s of W.solids) {
    if (!s.enabled || s === best) continue;
    const top = s.topAt(p.x, p.z);
    if (p.y >= top - 0.02 || p.y + H <= s.bottom()) continue;
    if (s.pushOut(p) && s.move) { const sp = s.move.ax === 'x' ? s.vx : s.vz; if (s.move.ax === 'x') v.x += (sp - v.x) * 0.35; }
  }
  // 장애물
  if (player.safe <= 0) {
    for (const h of W.hazards) { const k = h.hit(player); if (k) { hitPlayer(k); break; } }
    if (W.balls) hitPlayer(W.balls.hit(player));
  }
  // 체크포인트 · 떨어짐 · 결승
  if (player.onGround) for (const c of W.cps) if (p.z < c.z && (!player.cp || c.z < player.cp.z)) player.cp = c;
  if (p.y < W.killY) { player.falls++; sfx('fall'); flash('다시 출발!'); const c = player.cp; resetPlayer(c ? { x: c.x, y: c.y, z: c.rz != null ? c.rz : c.z - 1.2 } : W.spawn); player.cp = c; }
  if (p.z < W.finishZ && p.y > W.finishY - 2.5) return 'finish';
  return null;
}

// ── 화면 · 흐름 ───────────────────────────────────────────────────────
const ui = {
  title: $('title'), hud: $('hud'), round: $('hudRound'), time: $('hudTime'), pos: $('hudPos'), slots: $('hudSlots'),
  banner: $('banner'), over: $('over'), board: $('board'), toast: $('toast'), controls: $('controls')
};
function fmt(ms) { const s = Math.max(0, ms) / 1000, m = Math.floor(s / 60), r = s - m * 60; return (m ? m + ':' + (r < 10 ? '0' : '') : '') + r.toFixed(2); }
function show(el, on) { el.hidden = !on; }
let toastT = 0;
function flash(text) { ui.toast.textContent = text; ui.toast.hidden = false; toastT = 1.1; }

function startGame() {
  S.splits = []; S.round = 0; player.falls = 0; S.run = null;
  S.runReq = api('race_start', {}).then(d => { if (d && d.ok) S.run = d.run; }).catch(() => null);
  beginRound(0);
}
function beginRound(i) {
  S.round = i; setWorld(i); S.finishers = []; S.playT = 0; makeBots(ROUNDS[i]);
  S.mode = 'intro'; S.intro = 3.6;
  show(ui.title, false); show(ui.over, false); show(ui.board, false); show(ui.hud, true); show(ui.controls, true);
  const R0 = ROUNDS[i];
  ui.banner.innerHTML = `<small>${i + 1}라운드 · ${R0.name}</small><b>${R0.title}</b><span>${i < 2 ? `${R0.runners}명 중 ${R0.slots}명 통과` : '완주하면 기록이 순위에 올라요'}</span>`;
  ui.banner.hidden = false; ui.banner.className = 'banner';
  ui.round.textContent = `${i + 1}R ${R0.name}`;
  updateHud();
}
function updateHud() {
  const total = S.splits.reduce((a, b) => a + b, 0) + S.playT * 1000;
  ui.time.textContent = fmt(total);
  const R0 = ROUNDS[S.round], W = S.world;
  const myS = Math.max(0, W.spawn.z - player.pos.z);
  let ahead = S.finishers.length; for (const b of S.bots) if (!b.done && b.s > myS) ahead++;
  S.lastPos = ahead + 1;
  ui.pos.textContent = `${S.lastPos}위 / ${R0.runners}`;
  ui.slots.textContent = S.round < 2 ? `통과 ${Math.min(S.finishers.length, R0.slots)}/${R0.slots}` : '결승';
}
function endRound(won, why) {
  const R0 = ROUNDS[S.round];
  S.splits.push(Math.round(S.playT * 1000));
  if (!won) {
    S.mode = 'over'; sfx('out');
    ui.banner.hidden = true; show(ui.controls, false);
    ui.over.innerHTML = why === 'time' ? `<h2>시간 초과…</h2><p>결승은 ${R0.limit}초 안에 완주해야 해요.</p><p class="sub">떨어지지 않는 게 제일 빨라요.</p>` : `<h2>탈락…</h2><p>${S.round + 1}라운드 ${R0.name}에서 통과 자리가 다 찼어요.</p><p class="sub">고스트 ${R0.slots}명이 먼저 들어왔어요. 떨어지지 않는 게 제일 빨라요.</p>`;
    ui.over.innerHTML += `
      <button class="btn" id="again">다시 도전</button><button class="btn ghost" id="toBoard">순위 보기</button>`;
    show(ui.over, true); $('again').onclick = startGame; $('toBoard').onclick = () => openBoard();
    return;
  }
  if (S.round < 2) {
    S.mode = 'qualified'; sfx('win');
    ui.banner.innerHTML = `<b>통과!</b><span>${S.lastPos}위 · ${fmt(S.splits[S.round])}</span><small>다음: ${ROUNDS[S.round + 1].name} · ${ROUNDS[S.round + 1].title}</small>`;
    ui.banner.className = 'banner good'; ui.banner.hidden = false;
    setTimeout(() => beginRound(S.round + 1), 2600);
    return;
  }
  finishGame();
}
function finishGame() {
  S.mode = 'over'; sfx('win'); show(ui.controls, false); ui.banner.hidden = true;
  S.total = S.splits.reduce((a, b) => a + b, 0);
  const name = loadName();
  ui.over.innerHTML = `<h2>${S.lastPos === 1 ? '🏆 우승!' : '결승 완주!'}</h2>
    <p class="big">${fmt(S.total)}</p>
    <p class="sub">예선 ${fmt(S.splits[0])} · 준결승 ${fmt(S.splits[1])} · 결승 ${fmt(S.splits[2])} · 결승 ${S.lastPos}위</p>
    <form id="nameForm" class="nameform"><label for="nick">순위에 올릴 이름</label>
    <div class="row"><input id="nick" maxlength="12" autocomplete="nickname" placeholder="2~12자" value="${esc(name)}"><button class="btn" id="send" type="submit">기록 올리기</button></div>
    <p class="err" id="nameErr"></p></form>
    <button class="btn ghost" id="again">다시 하기</button><button class="btn ghost" id="toBoard">순위 보기</button>`;
  show(ui.over, true);
  $('again').onclick = startGame; $('toBoard').onclick = () => openBoard();
  $('nameForm').onsubmit = e => { e.preventDefault(); submitScore(); };
}
const NAME_RE = /^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9 _.\-]{2,12}$/;
function esc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
function loadName() { try { return localStorage.getItem('race_name') || ''; } catch (e) { return ''; } }
function playerToken() {
  let t = ''; try { t = localStorage.getItem('race_player') || ''; } catch (e) { /* 없음 */ }
  if (!/^[0-9a-f]{32}$/.test(t)) { const a = new Uint8Array(16); crypto.getRandomValues(a); t = [...a].map(b => b.toString(16).padStart(2, '0')).join(''); try { localStorage.setItem('race_player', t); } catch (e) { /* 없음 */ } }
  return t;
}
const REFUSE = {
  name: '이름은 2~12자, 한글 · 영문 · 숫자로 적어 주세요.', busy: '잠시 뒤 다시 올려 주세요.', run: '이번 기록은 확인하지 못했어요. 다시 도전해 주세요.',
  time: '이 기록은 확인하지 못했어요. 다시 도전해 주세요.', used: '이미 올린 기록이에요.'
};
async function submitScore() {
  const nick = $('nick').value.replace(/\s+/g, ' ').trim(), btn = $('send'), err = $('nameErr');
  err.textContent = '';
  if (!NAME_RE.test(nick)) { err.textContent = REFUSE.name; return; }
  try { localStorage.setItem('race_name', nick); } catch (e) { /* 없음 */ }
  btn.disabled = true; btn.textContent = '올리는 중…';
  await S.runReq;
  if (!S.run) { err.textContent = '연결이 끊겨 이번 기록은 올리지 못했어요.'; btn.disabled = false; btn.textContent = '기록 올리기'; return; }
  try {
    const d = await api('race_finish', { p_run: S.run, p_player: playerToken(), p_name: nick, p_ms: S.total, p_splits: S.splits });
    if (d && d.ok) { S.run = null; openBoard(d); return; }
    err.textContent = REFUSE[d && d.code] || '올리지 못했어요. 잠시 뒤 다시 시도해 주세요.';
  } catch (e) { err.textContent = '연결이 끊겼어요. 잠시 뒤 다시 시도해 주세요.'; }
  btn.disabled = false; btn.textContent = '기록 올리기';
}
async function openBoard(mine) {
  show(ui.over, false); show(ui.title, false); show(ui.controls, false); show(ui.board, true);
  if (S.mode === 'title') S.mode = 'board';
  const list = $('boardList'), me = $('boardMe');
  list.innerHTML = '<li class="muted">불러오는 중…</li>'; me.textContent = '';
  if (mine) me.textContent = mine.best_ms === S.total ? `내 최고 기록 ${fmt(mine.best_ms)} · ${mine.rank}위` : `이번 ${fmt(S.total)} · 내 최고 ${fmt(mine.best_ms)} · ${mine.rank}위`;
  try {
    const d = await api('race_board', { p_player: playerToken(), p_limit: 50 });
    const rows = (d && d.top) || [];
    list.innerHTML = rows.length ? rows.map(r => `<li class="${r.me ? 'me' : ''}"><b>${r.rank}</b><span>${esc(r.name)}</span><em>${fmt(r.ms)}</em></li>`).join('') : '<li class="muted">아직 기록이 없어요. 첫 번째 주인공이 되어 보세요.</li>';
    if (!mine && d && d.me) me.textContent = `내 최고 기록 ${fmt(d.me.ms)} · ${d.me.rank}위 / ${d.players}명`;
    else if (!mine && d) me.textContent = d.players ? `참가 ${d.players}명` : '';
  } catch (e) { list.innerHTML = '<li class="muted">순위를 불러오지 못했어요.</li>'; }
}
$('boardClose').onclick = () => { show(ui.board, false); if (S.mode === 'over') show(ui.over, true); else { S.mode = 'title'; show(ui.title, true); } };
$('startBtn').onclick = () => { unlockAudio(); startGame(); };
$('boardBtn').onclick = () => openBoard();

async function api(name, body) {
  if (!CFG.url) throw new Error('no server');
  const r = await fetch(CFG.url + '/rest/v1/rpc/' + name, { method: 'POST', headers: { 'Content-Type': 'application/json', apikey: CFG.key }, body: JSON.stringify(body), cache: 'no-store' });
  if (!r.ok) throw new Error('http ' + r.status);
  return r.json();
}

// ── 조작 ─────────────────────────────────────────────────────────────
const KEYS = { ArrowUp: [0, -1], KeyW: [0, -1], ArrowDown: [0, 1], KeyS: [0, 1], ArrowLeft: [-1, 0], KeyA: [-1, 0], ArrowRight: [1, 0], KeyD: [1, 0] };
addEventListener('keydown', e => {
  if (e.target && e.target.tagName === 'INPUT') return;
  if (KEYS[e.code]) { input.keys.add(e.code); e.preventDefault(); }
  if (e.code === 'Space') { if (!e.repeat) player.jumpBuf = JUMP_BUF; e.preventDefault(); }
});
addEventListener('keyup', e => input.keys.delete(e.code));
function readKeys() {
  let x = 0, z = 0; for (const k of input.keys) { x += KEYS[k][0]; z += KEYS[k][1]; }
  input.x = Math.max(-1, Math.min(1, x + input.stickX)); input.z = Math.max(-1, Math.min(1, z + input.stickZ));
}
const stick = $('stick'), knob = $('knob'), jumpBtn = $('jumpBtn');
let stickId = null, sx = 0, sy = 0;
const STICK_R = 56;
ui.controls.addEventListener('pointerdown', e => {
  if (e.target === jumpBtn || stickId !== null) return;
  if (e.clientX > innerWidth * 0.6) return;
  stickId = e.pointerId; sx = e.clientX; sy = e.clientY;
  stick.style.left = sx + 'px'; stick.style.top = sy + 'px'; stick.classList.add('on');
  ui.controls.setPointerCapture(e.pointerId); e.preventDefault();
});
ui.controls.addEventListener('pointermove', e => {
  if (e.pointerId !== stickId) return;
  let dx = e.clientX - sx, dy = e.clientY - sy; const d = Math.hypot(dx, dy);
  if (d > STICK_R) { dx = dx / d * STICK_R; dy = dy / d * STICK_R; }
  knob.style.transform = `translate(${dx}px, ${dy}px)`;
  const m = Math.min(1, d / STICK_R), dead = 0.12;
  const k = m < dead ? 0 : (m - dead) / (1 - dead) / Math.max(m, 1e-3);
  input.stickX = dx / STICK_R * k; input.stickZ = dy / STICK_R * k;
});
function stickEnd(e) { if (e.pointerId !== stickId) return; stickId = null; input.stickX = input.stickZ = 0; knob.style.transform = ''; stick.classList.remove('on'); }
ui.controls.addEventListener('pointerup', stickEnd); ui.controls.addEventListener('pointercancel', stickEnd);
jumpBtn.addEventListener('pointerdown', e => { player.jumpBuf = JUMP_BUF; jumpBtn.classList.add('on'); e.preventDefault(); e.stopPropagation(); });
['pointerup', 'pointercancel', 'pointerleave'].forEach(t => jumpBtn.addEventListener(t, () => jumpBtn.classList.remove('on')));
document.addEventListener('visibilitychange', () => { S.paused = document.hidden; last = performance.now(); });

// ── 소리(짧은 합성음, 기본 켬 · 끄기 저장) ─────────────────────────────
let actx = null, muted = false;
try { muted = localStorage.getItem('race_mute') === '1'; } catch (e) { /* 없음 */ }
const muteBtn = $('muteBtn');
function paintMute() { muteBtn.textContent = muted ? '🔇' : '🔊'; muteBtn.setAttribute('aria-label', muted ? '소리 켜기' : '소리 끄기'); }
paintMute();
muteBtn.onclick = () => { muted = !muted; paintMute(); try { localStorage.setItem('race_mute', muted ? '1' : '0'); } catch (e) { /* 없음 */ } unlockAudio(); };
function unlockAudio() { if (!actx) { try { actx = new (window.AudioContext || window.webkitAudioContext)(); } catch (e) { actx = null; } } if (actx && actx.state === 'suspended') actx.resume(); }
function tone(f0, f1, dur, type = 'sine', vol = 0.12, delay = 0) {
  if (!actx || muted) return;
  const t0 = actx.currentTime + delay, o = actx.createOscillator(), g = actx.createGain();
  o.type = type; o.frequency.setValueAtTime(f0, t0); o.frequency.exponentialRampToValueAtTime(Math.max(30, f1), t0 + dur);
  g.gain.setValueAtTime(vol, t0); g.gain.exponentialRampToValueAtTime(0.001, t0 + dur);
  o.connect(g).connect(actx.destination); o.start(t0); o.stop(t0 + dur + 0.02);
}
function sfx(k) {
  if (k === 'jump') tone(420, 760, 0.12, 'square', 0.05);
  else if (k === 'land') tone(160, 90, 0.06, 'sine', 0.06);
  else if (k === 'hit') tone(220, 60, 0.22, 'sawtooth', 0.09);
  else if (k === 'boing') tone(300, 1100, 0.25, 'triangle', 0.1);
  else if (k === 'fall') tone(600, 80, 0.5, 'triangle', 0.08);
  else if (k === 'beep') tone(520, 520, 0.12, 'square', 0.06);
  else if (k === 'go') tone(880, 880, 0.3, 'square', 0.07);
  else if (k === 'win') { tone(523, 523, 0.12, 'triangle', 0.1); tone(659, 659, 0.12, 'triangle', 0.1, 0.12); tone(784, 784, 0.25, 'triangle', 0.1, 0.24); }
  else if (k === 'out') { tone(392, 330, 0.25, 'triangle', 0.1); tone(330, 220, 0.4, 'triangle', 0.1, 0.25); }
}

// ── 돌리기 ───────────────────────────────────────────────────────────
function resize() {
  const w = innerWidth, h = innerHeight; renderer.setSize(w, h, false);
  camera.aspect = w / h; camera.fov = w / h < 0.8 ? 72 : 58; camera.updateProjectionMatrix();
}
addEventListener('resize', resize); resize();
const camPos = new THREE.Vector3(0, 8, 14), camLook = new THREE.Vector3();
function updateCamera(dt, idle) {
  const p = player.pos, portrait = camera.aspect < 0.8;
  const want = idle
    ? new THREE.Vector3(Math.sin(S.t * 0.15) * 14, 9, p.z + 12)
    : new THREE.Vector3(p.x * 0.55, p.y + (portrait ? 6.8 : 5.4), p.z + (portrait ? 10.5 : 8.8));
  const k = 1 - Math.exp(-dt * (idle ? 1 : 7));
  camPos.lerp(want, k);
  camLook.lerp(new THREE.Vector3(p.x * 0.75, p.y + 1, p.z - (portrait ? 7 : 6)), k);
  camera.position.copy(camPos); camera.lookAt(camLook);
}
function drawPlayer(dt) {
  const p = player.pos, v = player.vel, sp = Math.hypot(v.x, v.z);
  runnerMesh.position.copy(p);
  if (sp > 0.5) player.yaw = Math.atan2(-v.x, -v.z);
  runnerMesh.rotation.y += (((player.yaw - runnerMesh.rotation.y + Math.PI) % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2) - Math.PI) * Math.min(1, dt * 14);
  runnerMesh.rotation.z = player.stun > 0 ? Math.sin(player.stun * 30) * 0.3 : 0;
  runnerMesh.visible = !(player.safe > 0 && Math.floor(player.safe * 12) % 2);
  animateRunner(runnerMesh, sp, !player.onGround, dt);
  const gt = groundTop(p.x, p.z, p.y);
  blob.visible = gt > -Infinity; if (blob.visible) { blob.position.set(p.x, gt + 0.03, p.z); const s = Math.max(0.35, 1 - (p.y - gt) * 0.12); blob.scale.set(s, s, s); }
}

let last = performance.now();
function frame(now) {
  requestAnimationFrame(frame);
  let dt = Math.min(0.05, (now - last) / 1000); last = now;
  if (S.paused) return;
  readKeys();
  if (toastT > 0) { toastT -= dt; if (toastT <= 0) ui.toast.hidden = true; }
  if (S.mode === 'title' || S.mode === 'board') {
    S.t += dt; for (const s of S.world.solids) { s.save(); s.update(S.t, dt); } for (const h of S.world.hazards) h.update(S.t);
    if (S.world.balls) S.world.balls.update(S.t, dt, S.world);
    syncWorld(S.world); updateCamera(dt, true); runnerMesh.visible = false; blob.visible = false;
  } else if (S.mode === 'intro') {
    const before = Math.ceil(S.intro - 0.6); S.intro -= dt; const after = Math.ceil(S.intro - 0.6);
    if (S.intro < 3 && after > 0) { ui.banner.className = 'banner count'; ui.banner.innerHTML = `<b class="num">${after}</b>`; if (before !== after) sfx('beep'); }
    S.t += dt; for (const s of S.world.solids) { s.save(); s.update(S.t, dt); } for (const h of S.world.hazards) h.update(S.t);
    if (S.world.balls) S.world.balls.update(S.t, dt, S.world);
    syncWorld(S.world); drawPlayer(dt); updateCamera(dt, false); runnerMesh.visible = true;
    for (const b of S.bots) { b.mesh.position.set(Math.max(-6, Math.min(6, b.lane)), 0, S.world.spawn.z + 1 + (S.bots.indexOf(b) % 4)); }
    if (S.intro <= 0.6) { S.mode = 'play'; ui.banner.className = 'banner count'; ui.banner.innerHTML = '<b class="num">GO!</b>'; sfx('go'); }
  } else if (S.mode === 'play') {
    if (S.intro > 0) { S.intro -= dt; if (S.intro <= 0) ui.banner.hidden = true; }
    S.acc += dt; let res = null;
    while (S.acc >= DT) { S.acc -= DT; S.playT += DT; res = step(DT) || res; if (res) break; }
    updateBots(dt);
    syncWorld(S.world); drawPlayer(dt); updateCamera(dt, false); updateHud();
    const R0 = ROUNDS[S.round];
    if (res === 'finish') { const slotsLeft = S.finishers.length < R0.slots; S.finishers.push({ me: true }); S.lastPos = S.finishers.length; endRound(slotsLeft || S.round === 2); }
    else if (S.round < 2 && S.finishers.length >= R0.slots) endRound(false);
    else if (R0.limit && S.playT > R0.limit) endRound(false, 'time');
  } else {
    S.t += dt; for (const s of S.world.solids) { s.save(); s.update(S.t, dt); } for (const h of S.world.hazards) h.update(S.t);
    syncWorld(S.world); updateCamera(dt, false);
  }
  renderer.render(scene, camera);
}
setWorld(0); S.mode = 'title';
requestAnimationFrame(frame);

// 개발용(?dev): 화면 없이 물리를 돌려 코스가 깰 수 있는지 · 걸리는 시간을 본다
if (DEV) window.__race = { S, player, input, step, setWorld, ROUNDS, DT, groundTop: (x, z, y) => groundTop(x, z, y), solids: () => S.world.solids, beginRound, startGame, endRound };
