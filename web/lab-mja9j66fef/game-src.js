/* StepUP 타워 — 홈페이지 미니게임(2026-11-13, 서버 0104).
   나선형 탑 하나를 다섯 구간으로 끝까지 오른다. 바깥으로 떨어지면 물(목숨 하나), 안쪽 틈 · 구멍으로 떨어지면 아래층부터 다시(혈압).
   한 판 목숨 2(부활 1번) · 8분. 오늘의 도전은 하루 1판(00시 KST) + SNS 게시물 링크마다 1판(하루 3번까지). 연습은 점수 없이 몇 번이든.
   점수(tower-rules.js) = 통과 구간 × 1000 + 오른 높이 + 시간 보너스 + 완주 순위 · 무부활 + 밀어 떨어뜨린 AI — 서버가 구간 기록으로 다시 센다(tower_*).
   AI 러너 15명은 나와 같은 물리로 달리고 장애물에 맞고 떨어진다(성격 일곱 가지).
   빌드: cd web && npm run build:game  →  lab-mja9j66fef/game.js(공개 전 내부 시험 — 홈 링크 없음) (three.js 를 함께 묶는다 — CSP 가 이 사이트 스크립트만 받는다) */
import * as THREE from 'three';
import { SECTIONS, towerScore, RUN_LIMIT_S } from './tower-rules.js';

const CFG = window.STEPUP_RACE || {};
const DEV = /[?&]dev\b/.test(location.search);

// ── 몸놀림(미터 · 초) ────────────────────────────────────────────────
const GRAV = -30, JUMP_V = 11.2, SPEED = 8.6, ACC_GROUND = 60, ACC_AIR = 24;
const R = 0.42, H = 1.5, STEP_UP = 0.4, COYOTE = 0.1, JUMP_BUF = 0.13, DT = 1 / 120;
const SHOVE_CD = 0.8, SHOVE_RANGE = 1.8, DIVE_V = 4.5;
// ── 탑 ──────────────────────────────────────────────────────────────
const RT = 40, HW = 7, PILLAR = RT - HW, DEATH_DROP = 7, WATER_Y = -20;
const TAU = Math.PI * 2;

const THEMES = [
  { top: '#EAF4FF', stripe: '#D2E5FF', side: 0x1B3A6B, accent: 0x1166FF },
  { top: '#FFF1DE', stripe: '#FFD9A8', side: 0xC2410C, accent: 0xFF8A00 },
  { top: '#FFE6F1', stripe: '#FFC2DD', side: 0x9D174D, accent: 0xFF4F8B },
  { top: '#E3FBF4', stripe: '#B5F0DF', side: 0x0F766E, accent: 0x14B8A6 },
  { top: '#FFF7D1', stripe: '#FFE58A', side: 0x5B21B6, accent: 0xFFC400 }
];
const C_HAZ = 0xFF4F8B, C_HAZ2 = 0xFF9F1C, C_PUSH = 0x1166FF;
const TAUNTS = ['혈압 주의 🩸', '아래층 손님 입장~', '다시 올라가면 되죠 😇', '그 자리… 아까 지나간 곳 같은데요?', '괜찮아요, 탑은 안 도망가요', '심호흡 한 번 🧘'];

function mulberry(a) { return function () { a |= 0; a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; }; }
const mod = (a, b) => ((a % b) + b) % b;
// 길 위 좌표(s = 가운데 줄 거리, u = 바깥쪽 +) → 세상 좌표
function P(s, u, y) { const a = s / RT, r = RT + u; return { x: r * Math.cos(a), y, z: r * Math.sin(a), a }; }
function segDist(px, pz, ax, az, bx, bz) {
  const vx = bx - ax, vz = bz - az, l2 = vx * vx + vz * vz, t = l2 ? Math.max(0, Math.min(1, ((px - ax) * vx + (pz - az) * vz) / l2)) : 0;
  const qx = ax + vx * t, qz = az + vz * t; return [Math.hypot(px - qx, pz - qz), qx, qz];
}

// ════════════════════════════════════════════════════════════════════
//  땅: 상자(box · 시소 tilt) · 원판(disc) · 나선 조각(arc)
// ════════════════════════════════════════════════════════════════════
class Solid {
  constructor(o) {
    Object.assign(this, { kind: 'box', cx: 0, cz: 0, top: 0, h: 1.4, yaw: 0, hx: 2, hz: 2, r: 0, move: null, spin: 0, belt: 0, fall: false, theme: 0, color: null, ice: false, mystery: false, door: null, baseYaw: 0 }, o);
    this.static = !this.move && !this.spin && !this.fall && this.kind !== 'tilt' && !this.door;
    this.enabled = true; this.state = 'idle'; this.timer = 0; this.drop = 0; this.tilt = 0; this.flash = 0; this.warn = false;
    this.baseX = this.cx; this.baseZ = this.cz; this.vx = 0; this.vz = 0;
    this.p = { cx: this.cx, cz: this.cz, top: this.top, yaw: this.yaw, tilt: 0 };
    this.bounds();
  }
  bounds() {
    if (this.kind === 'arc') {
      let x0 = Infinity, x1 = -Infinity, z0 = Infinity, z1 = -Infinity;
      for (let i = 0; i <= 8; i++) { const a = this.a0 + (this.a1 - this.a0) * i / 8; for (const r of [this.r0, this.r1]) { const x = r * Math.cos(a), z = r * Math.sin(a); x0 = Math.min(x0, x); x1 = Math.max(x1, x); z0 = Math.min(z0, z); z1 = Math.max(z1, z); } }
      this.bb = [x0, x1, z0, z1, Math.min(this.y0, this.y1) - this.h, Math.max(this.y0, this.y1)];
    } else {
      const e = (this.kind === 'disc' ? this.r : Math.hypot(this.hx, this.hz)) + (this.move ? this.move.amp : 0);
      this.bb = [this.cx - e, this.cx + e, this.cz - e, this.cz + e, this.top - this.h - 30, this.top + 3];
    }
  }
  near(x, y, z) { const b = this.bb; return x > b[0] - 1.5 && x < b[1] + 1.5 && z > b[2] - 1.5 && z < b[3] + 1.5 && y > b[4] - 2 && y < b[5] + 3; }
  save() { const p = this.p; p.cx = this.cx; p.cz = this.cz; p.top = this.top; p.yaw = this.yaw; p.tilt = this.tilt; }
  update(t, dt) {
    if (this.move) {
      const m = this.move, f = this.offset(t), f2 = this.offset(t + 0.01);
      this.cx = this.baseX + m.dir[0] * f; this.cz = this.baseZ + m.dir[1] * f;
      this.vx = m.dir[0] * (f2 - f) / 0.01; this.vz = m.dir[1] * (f2 - f) / 0.01;
      if (m.profile === 'punch') { const ph = mod(t / m.period + (m.phase || 0), 1); this.flash = ph > 0.22 && ph < 0.4 ? 1 : 0; }
    }
    if (this.spin) this.yaw = this.baseYaw + this.spin * t;
    if (this.fall) {
      if (this.state === 'shake') { this.timer -= dt; if (this.timer <= 0) { this.state = 'drop'; this.timer = 3.2; this.enabled = false; } }
      else if (this.state === 'drop') { this.timer -= dt; this.drop += dt; if (this.timer <= 0) { this.state = 'idle'; this.enabled = true; this.drop = 0; } }
    }
    if (this.door) { const ph = mod(t / this.door.period + (this.door.phase || 0), 1); this.enabled = ph >= this.door.open; this.warn = ph > 0.72; this.doorPh = ph; }
  }
  offset(t) {
    const m = this.move;
    if (m.profile === 'punch') {
      const ph = mod(t / m.period + (m.phase || 0), 1);
      if (ph < 0.4) return 0; if (ph < 0.47) return m.amp * (ph - 0.4) / 0.07; if (ph < 0.7) return m.amp; return m.amp * (1 - (ph - 0.7) / 0.3);
    }
    return m.amp * Math.sin(TAU * t / m.period + (m.phase || 0));
  }
  trigger() { if (this.fall && this.state === 'idle') { this.state = 'shake'; this.timer = this.fake ? 0.04 : 0.32; } }
  toLocal(x, z, Q0) { const Q = Q0 || this, dx = x - Q.cx, dz = z - Q.cz, c = Math.cos(-Q.yaw), s = Math.sin(-Q.yaw); return [dx * c - dz * s, dx * s + dz * c]; }
  toWorld(lx, lz, Q0) { const Q = Q0 || this, c = Math.cos(Q.yaw), s = Math.sin(Q.yaw); return [Q.cx + lx * c - lz * s, Q.cz + lx * s + lz * c]; }
  arcD(x, z) { return mod(Math.atan2(z, x) - this.a0, TAU); }
  inside(x, z, m) {
    if (this.kind === 'disc') { const dx = x - this.cx, dz = z - this.cz; return dx * dx + dz * dz <= (this.r + m) ** 2; }
    if (this.kind === 'arc') {
      const r = Math.hypot(x, z); if (r < this.r0 - m || r > this.r1 + m) return false;
      const d = this.arcD(x, z), span = this.a1 - this.a0, mm = m / r; return d <= span + mm || d >= TAU - mm;
    }
    const [lx, lz] = this.toLocal(x, z); return Math.abs(lx) <= this.hx + m && Math.abs(lz) <= this.hz + m;
  }
  topAt(x, z) {
    if (this.kind === 'arc') {
      const span = this.a1 - this.a0; let d = this.arcD(x, z); if (d > span) d = d > span + (TAU - span) / 2 ? 0 : span;
      return this.y0 + (this.y1 - this.y0) * d / span;
    }
    if (this.kind === 'tilt') { const [lx] = this.toLocal(x, z); return this.top + Math.tan(this.tilt) * Math.max(-this.hx, Math.min(this.hx, lx)); }
    return this.top;
  }
  bottom() { return (this.kind === 'arc' ? Math.min(this.y0, this.y1) : this.top - (this.kind === 'tilt' ? Math.abs(Math.tan(this.tilt)) * this.hx : 0)) - this.h; }
  pushOut(pos) {
    if (this.kind === 'disc') {
      const dx = pos.x - this.cx, dz = pos.z - this.cz, d = Math.hypot(dx, dz), need = this.r + R;
      if (d >= need) return false; const k = d > 1e-5 ? need / d : 1; pos.x = this.cx + (d > 1e-5 ? dx * k : need); pos.z = this.cz + (d > 1e-5 ? dz * k : 0); return true;
    }
    if (this.kind === 'arc') {
      const r = Math.hypot(pos.x, pos.z), d = this.arcD(pos.x, pos.z), span = this.a1 - this.a0;
      const setR = nr => { const k = nr / r; pos.x *= k; pos.z *= k; };
      const rot = da => { const a = Math.atan2(pos.z, pos.x) + da; pos.x = r * Math.cos(a); pos.z = r * Math.sin(a); };
      if (d <= span) {
        if (r >= this.r0 && r <= this.r1) {
          const opts = [[r - this.r0, () => setR(this.r0 - R)], [this.r1 - r, () => setR(this.r1 + R)], [d * r, () => rot(-(d + R / r))], [(span - d) * r, () => rot(span - d + R / r)]];
          opts.sort((a, b) => a[0] - b[0]); opts[0][1](); return true;
        }
        if (r < this.r0 && this.r0 - r < R) { setR(this.r0 - R); return true; }
        if (r > this.r1 && r - this.r1 < R) { setR(this.r1 + R); return true; }
        return false;
      }
      const ea = d > span + (TAU - span) / 2 ? this.a0 : this.a1, c = Math.cos(ea), s = Math.sin(ea);
      const [dd, qx, qz] = segDist(pos.x, pos.z, this.r0 * c, this.r0 * s, this.r1 * c, this.r1 * s);
      if (dd >= R) return false; const k = R / Math.max(dd, 1e-4); pos.x = qx + (pos.x - qx) * k; pos.z = qz + (pos.z - qz) * k; return true;
    }
    const [lx, lz] = this.toLocal(pos.x, pos.z);
    const qx = Math.max(-this.hx, Math.min(this.hx, lx)), qz = Math.max(-this.hz, Math.min(this.hz, lz));
    const dx = lx - qx, dz = lz - qz, d = Math.hypot(dx, dz); let nx, nz;
    if (d >= R) return false;
    if (d > 1e-5) { nx = qx + dx / d * R; nz = qz + dz / d * R; }
    else { const px = this.hx - Math.abs(lx), pz = this.hz - Math.abs(lz); if (px < pz) { nx = Math.sign(lx || 1) * (this.hx + R); nz = lz; } else { nx = lx; nz = Math.sign(lz || 1) * (this.hz + R); } }
    const [wx, wz] = this.toWorld(nx, nz); pos.x = wx; pos.z = wz; return true;
  }
  carry(pos) {
    if (this.static || this.door) return;
    if (this.kind === 'disc' || this.spin) {
      const dx = pos.x - this.p.cx, dz = pos.z - this.p.cz, a = this.yaw - this.p.yaw, c = Math.cos(a), s = Math.sin(a);
      pos.x = this.cx + dx * c - dz * s; pos.z = this.cz + dx * s + dz * c;
    } else { pos.x += this.cx - this.p.cx; pos.z += this.cz - this.p.cz; }
    if (this.kind === 'tilt') { const [lx] = this.toLocal(pos.x, pos.z); pos.y += (Math.tan(this.tilt) - Math.tan(this.p.tilt)) * lx; }
  }
}

// ════════════════════════════════════════════════════════════════════
//  장애물
// ════════════════════════════════════════════════════════════════════
class Sweeper { // 도는 막대(2단이면 높이 · 방향이 다른 막대 둘)
  constructor(o) { Object.assign(this, o); this.bars = o.bars.map(b => Object.assign({ a: 0 }, b)); }
  update(t) { for (const b of this.bars) b.a = b.spin * t + (b.phase || 0); }
  hit(bd) {
    const p = bd.pos;
    for (const b of this.bars) {
      const yb = this.base + b.y0, yt = this.base + b.y1;
      if (p.y > yt || p.y + H < yb) continue;
      const ex = Math.cos(b.a) * b.len, ez = Math.sin(b.a) * b.len;
      const [d, qx, qz] = segDist(p.x, p.z, this.x - ex, this.z - ez, this.x + ex, this.z + ez);
      if (d > R + 0.28) continue;
      const side = Math.sign((p.x - this.x) * -Math.sin(b.a) + (p.z - this.z) * Math.cos(b.a)) || 1;
      const tx = -Math.sin(b.a) * side, tz = Math.cos(b.a) * side;
      const k = (R + 0.3) / Math.max(d, 1e-3); p.x = qx + (p.x - qx) * k; p.z = qz + (p.z - qz) * k;
      return { vx: tx * 8.5, vz: tz * 8.5, vy: 6, stun: 0.42 };
    }
    return null;
  }
}
class Hammer { // 길을 가로질러 흔들리는 망치
  constructor(o) { Object.assign(this, { len: 7, r: 1.15, amp: 0.95, period: 2.1, phase: 0 }, o); this.update(0); }
  update(t) {
    const w = TAU / this.period, f = this.amp * Math.sin(w * t + this.phase), lat = this.len * Math.sin(f);
    this.f = f; this.v = this.amp * w * Math.cos(w * t + this.phase) * this.len * Math.cos(f);
    this.hx = this.x + this.n[0] * lat; this.hz = this.z + this.n[1] * lat; this.hy = this.py - this.len * Math.cos(f); this.lat = lat;
  }
  hit(bd) {
    const p = bd.pos, cy = Math.max(p.y + R, Math.min(p.y + H - R, this.hy));
    if (Math.hypot(p.x - this.hx, cy - this.hy, p.z - this.hz) > this.r + R) return null;
    const dir = Math.sign(this.v) || 1;
    return { vx: this.n[0] * dir * 14, vz: this.n[1] * dir * 14, vy: 7.5, stun: 0.55 };
  }
}
class Balls { // 나선 비탈을 거꾸로 굴러 내려오는 공
  constructor(o) { Object.assign(this, o); this.list = []; this.next = 0.3; this.rnd = mulberry(o.seed || 7); }
  update(t, dt, world, leadS) {
    this.next -= dt;
    if (this.next <= 0 && !(leadS > this.sTop - 7)) {
      this.next = this.every * (0.6 + this.rnd() * 0.7);
      const r = 1.1 + this.rnd() * 0.6;
      this.list.push({ s: this.sTop, u: (this.rnd() * 2 - 1) * (HW - r - 0.2), r, v: this.speed * (0.85 + this.rnd() * 0.35), spin: 0, mesh: null });
    }
    for (const b of this.list) {
      b.s -= b.v * dt; b.spin += b.v * dt / b.r;
      const q = P(b.s, b.u, trackY(b.s) + b.r); b.x = q.x; b.y = q.y; b.z = q.z; b.a = q.a;
      if (b.s < this.sEnd) b.dead = true;
    }
    for (const b of this.list) if (b.dead && b.mesh) { world.group.remove(b.mesh); b.mesh = null; }
    this.list = this.list.filter(b => !b.dead);
  }
  hit(bd) {
    const p = bd.pos;
    for (const b of this.list) {
      const cy = Math.max(p.y + R, Math.min(p.y + H - R, b.y));
      if (Math.hypot(p.x - b.x, cy - b.y, p.z - b.z) < b.r + R) { const tx = -Math.sin(b.a), tz = Math.cos(b.a); return { vx: -tx * b.v * 1.2, vz: -tz * b.v * 1.2, vy: 7, stun: 0.5 }; }
    }
    return null;
  }
}
class Wind { // 몇 초마다 바깥쪽으로 부는 바람(바깥은 물 — 혈압)
  constructor(o) { Object.assign(this, { period: 4.2, dur: 1.3, force: 5 }, o); }
  phase(t) { return mod(t + (this.offset || 0), this.period); }
  active(t) { return this.phase(t) < this.dur; }
  warn(t) { return this.phase(t) > this.period - 0.9; }
}

// ════════════════════════════════════════════════════════════════════
//  코스 — 나선 다섯 구간
// ════════════════════════════════════════════════════════════════════
let PROF = [];
function trackY(s) {
  let y = 0;
  for (const [s0, s1, y0, y1] of PROF) { if (s < s0) break; y = s <= s1 ? y0 + (y1 - y0) * (s - s0) / (s1 - s0) : y1; }
  return y;
}
function buildCourse(seed) {
  const W = { solids: [], hazards: [], balls: null, winds: [], secs: [], gates: [], signs: [], group: new THREE.Group(), finishS: 0, flags: [], tilts: [] };
  PROF = [];
  let s = 0, y = 0, theme = 0;
  const rnd = mulberry(seed || 4242), ph = () => rnd() * TAU;
  const floor = (len, rise = 0, o = {}) => {
    const r0 = RT + (o.u0 != null ? o.u0 : -HW), r1 = RT + (o.u1 != null ? o.u1 : HW);
    const sd = new Solid({ kind: 'arc', r0, r1, a0: s / RT, a1: (s + len) / RT, y0: y, y1: y + rise, h: o.h || 1.6, theme, belt: o.belt || 0, ice: !!o.ice, door: o.door || null, s0: s, len });
    W.solids.push(sd); PROF.push([s, s + len, y, y + rise]); s += len; y += rise; return sd;
  };
  const gap = len => { PROF.push([s, s + len, y, y]); s += len; };
  const box = (ds, u, o) => { const q = P(s + ds, u, 0); const sd = new Solid(Object.assign({ cx: q.x, cz: q.z, yaw: q.a, baseYaw: q.a, theme, s0: s + ds, u }, o)); W.solids.push(sd); return sd; };
  const nrm = ds => { const a = (s + ds) / RT; return [Math.cos(a), Math.sin(a)]; };
  const sweeper = (ds, bars, extra) => { const q = P(s + ds, 0, 0); bars.forEach(b => { b.phase = ph(); b.spin *= 0.9 + rnd() * 0.25; }); W.hazards.push(new Sweeper(Object.assign({ x: q.x, z: q.z, base: y, bars, s: s + ds }, extra || {}))); };
  const section = name => { W.secs.push({ s, y, name, theme }); W.gates.push({ s, y, k: W.secs.length }); };

  // 1 몸풀기 — 2단 막대 · 틈 뛰기
  theme = 0; section('몸풀기');
  floor(14); floor(16, 3);
  floor(34);
  sweeper(-25, [{ y0: 0.2, y1: 0.75, spin: 1.6, len: 7.3 }]);
  sweeper(-9, [{ y0: 0.2, y1: 0.75, spin: -1.75, len: 7.3 }, { y0: 1.05, y1: 1.55, spin: 1.2, len: 7.3, phase: 2.1 }]);
  floor(8, 1); gap(3.2); floor(8, 0.8, { u0: -5, u1: 5 }); gap(3.5); floor(8, 0.8, { u0: -4, u1: 4 }); gap(3); floor(6);
  floor(14, 2.4);
  // 2 밀치기 — 밀어내는 벽 · 튀어나오는 펀치 벽 · ? 점프대
  theme = 1; section('밀치기');
  floor(8);
  floor(40);
  [-32, -22, -12].forEach(ds => box(ds, 0, { hx: 2.6, hz: 0.7, top: y + 2.3, h: 2.3, move: { dir: nrm(ds), amp: 4.6, period: 2.0 + rnd() * 0.5, phase: ph() }, color: C_PUSH }));
  floor(10, 3);
  floor(32);
  [[-27, 1], [-21, -1], [-15, 1], [-9, -1]].forEach(([ds, side], i) => {
    const n = nrm(ds), dir = [-n[0] * side, -n[1] * side];
    box(ds, side * (HW + 1.2), { hx: 3.2, hz: 1.3, top: y + 2.2, h: 2.2, move: { dir, amp: 5.4, period: 2.1 + rnd() * 0.6, phase: rnd(), profile: 'punch' }, color: C_HAZ2, punch: true });
  });
  box(-2.5, (rnd() < 0.5 ? -1 : 1) * 4.4, { hx: 1.5, hz: 1.5, top: y + 0.15, h: 0.4, mystery: true });
  floor(10, 3);
  // 3 흔들 다리 — 좁은 다리와 망치(안쪽으로 떨어지면 아래층) · 움직이는 발판
  theme = 2; section('흔들 다리');
  floor(8);
  floor(38, 2, { u0: -1.8, u1: 1.8 });
  [-30, -19, -8].forEach(ds => { const q = P(s + ds, 0, 0); W.hazards.push(new Hammer({ x: q.x, z: q.z, py: trackY(s + ds) + 8.3, n: nrm(ds), phase: ph(), period: 2.0 + rnd() * 0.4, a: q.a, s: s + ds })); });
  floor(6, 0, { u0: -5, u1: 5 });
  [4.5, 11, 17.5, 24].forEach((ds, i) => box(ds, 0, { hx: 2.3, hz: 2.3, top: y, h: 1, move: { dir: nrm(ds), amp: 4.4, period: 3.1, phase: i % 2 ? Math.PI : 0 }, color: C_PUSH }));
  gap(28);
  floor(8); floor(12, 4);
  // 4 무너지는 길 — 도는 원판 · 떨어지는 타일(15% 가짜 — 잘 보면 금이 가 있다, 아래층은 2구간 바닥) · 얼음 시소
  theme = 3; section('무너지는 길');
  floor(6);
  box(7.4, 0, { kind: 'disc', r: 6.2, top: y + 0.3, h: 1.2, spin: 0.9 });
  sweeper(7.4, [{ y0: 0.2, y1: 0.75, spin: 1.8, len: 5.8 }], { base: y + 0.3 });
  box(21, 0, { kind: 'disc', r: 6.2, top: y + 0.3, h: 1.2, spin: -1.05 });
  gap(28.4);
  floor(6);
  for (let row = 0; row < 8; row++) for (let col = 0; col < 5; col++) box(1.95 + row * 2.9, -5.8 + col * 2.9, { hx: 1.3, hz: 1.3, top: y, h: 0.5, fall: true, fake: rnd() < 0.15, tileColor: (row + col) % 2 });
  gap(24.6);
  floor(6);
  W.tilts.push(box(7.2, 0, { kind: 'tilt', hx: 7, hz: 6, top: y, h: 0.8, ice: true }));
  gap(14.4);
  floor(8); floor(10, 4);
  // 5 정상 — 공 비탈 + 바람 · 얼음 2단 막대 · 결승 직전 함정 문
  theme = 4; section('정상');
  floor(6);
  const rampS = s; floor(50, 9);
  W.balls = new Balls({ sTop: s - 0.5, sEnd: rampS - 2, speed: 11, every: 0.62, seed: (rnd() * 1e6) | 0 });
  W.winds.push(new Wind({ s0: rampS, s1: s, period: 3.6 + rnd() * 1.4, dur: 1.3, force: 5, offset: rnd() * 4 }));
  floor(14, 0, { ice: true });
  sweeper(-7, [{ y0: 0.2, y1: 0.75, spin: 2.0, len: 7.3 }, { y0: 1.05, y1: 1.55, spin: -1.4, len: 7.3, phase: 1.2 }]);
  floor(3, 0.5);
  W.signs.push({ s, y, text: '거의 다 왔어요 😇' });
  W.door = floor(5, 0.5, { door: { period: 2.6 + rnd() * 0.8, open: 0.36, phase: rnd() } });
  floor(2, 0.5);
  W.finishS = s + 8; W.finishY = y;
  floor(18, 0, { u0: -8, u1: 8 });
  W.endS = s; W.topY = y;
  W.items = [];
  // 넓은 바닥 위에만(틈 · 다리 · 발판 위 허공에 두면 AI 가 쫓아 뛰어내린다)
  for (const [s0, s1] of [[16, 28], [44, 50], [105, 115], [147, 150], [167, 174], [200, 206], [219, 224], [264, 268], [298, 304], [318, 322], [353, 357], [383, 387], [404, 420], [440, 470], [479, 489]]) {
    if (rnd() > 0.6) continue;
    const is = s0 + rnd() * (s1 - s0), iu = (rnd() * 2 - 1) * 3.5;
    W.items.push({ s: is, u: iu, y: trackY(is), on: true, t: 0 });
  }
  for (let fs = 6; fs < W.endS - 4; fs += 11) W.flags.push(fs);
  W.spawnS = 4;
  if (DEV) { // 층 사이 높이 · 구간 길이가 규칙(tower-rules.js)과 맞는지
    const C = TAU * RT; let minGap = Infinity; for (let q = 0; q + C < W.endS; q += 2) minGap = Math.min(minGap, trackY(q + C) - trackY(q));
    const lens = W.secs.map((sc, i) => +((i + 1 < W.secs.length ? W.secs[i + 1].s : W.finishS) - sc.s).toFixed(1));
    console.log('[tower] 층 간격 최소', minGap.toFixed(1), 'm · 구간 길이', lens.join(' '), '· 높이', W.secs.map(q => q.y).join(' '), '→', W.finishY);
    lens.forEach((l, i) => { if (Math.abs(l - SECTIONS[i].len) > 0.05) console.warn('[tower] 구간', i + 1, '길이가 규칙과 다르다', l, SECTIONS[i].len); });
  }
  return W;
}

// ════════════════════════════════════════════════════════════════════
//  그리기
// ════════════════════════════════════════════════════════════════════
const MAT = {};
function mat(color, opts) { const k = color + JSON.stringify(opts || {}); return MAT[k] || (MAT[k] = new THREE.MeshLambertMaterial(Object.assign({ color }, opts || {}))); }
function canvasTex(w, h, draw) { const c = document.createElement('canvas'); c.width = w; c.height = h; draw(c.getContext('2d'), w, h); const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; t.anisotropy = 4; return t; }
const TOPTEX = THEMES.map(th => { const t = canvasTex(128, 128, (g, w, h) => { g.fillStyle = th.top; g.fillRect(0, 0, w, h); g.fillStyle = th.stripe; for (let i = 0; i < 4; i++) g.fillRect(0, i * 32, w, 16); g.fillStyle = 'rgba(255,255,255,.55)'; g.fillRect(0, 0, 6, h); g.fillRect(w - 6, 0, 6, h); }); t.wrapS = t.wrapT = THREE.RepeatWrapping; return t; });
const ICETEX = (() => { const t = canvasTex(128, 128, (g, w, h) => { g.fillStyle = '#CFF4FF'; g.fillRect(0, 0, w, h); g.strokeStyle = 'rgba(255,255,255,.9)'; g.lineWidth = 3; for (let i = 0; i < 6; i++) { g.beginPath(); g.moveTo(Math.random() * w, Math.random() * h); g.lineTo(Math.random() * w, Math.random() * h); g.stroke(); } }); t.wrapS = t.wrapT = THREE.RepeatWrapping; return t; })();
const CRACKTEX = ['#6FA8FF', '#5CE1C6'].map(c => canvasTex(64, 64, (g, w, h) => { g.fillStyle = c; g.fillRect(0, 0, w, h); g.strokeStyle = 'rgba(11,26,51,.32)'; g.lineWidth = 1.6; g.beginPath(); g.moveTo(10, 8); g.lineTo(26, 30); g.lineTo(20, 46); g.moveTo(26, 30); g.lineTo(48, 36); g.lineTo(56, 54); g.stroke(); }));
const DOORTEX = (() => { const t = canvasTex(128, 128, (g, w, h) => { for (let i = -2; i < 8; i++) { g.fillStyle = i % 2 ? '#111' : '#FFD23F'; g.beginPath(); g.moveTo(i * 32, 0); g.lineTo(i * 32 + 32, 0); g.lineTo(i * 32 + 32 - 64, h); g.lineTo(i * 32 - 64, h); g.fill(); } }); t.wrapS = t.wrapT = THREE.RepeatWrapping; return t; })();
function textTex(text, bg, fg, w = 512, h = 128) {
  return canvasTex(w, h, (g) => {
    g.fillStyle = bg; g.fillRect(0, 0, w, h);
    g.fillStyle = 'rgba(255,255,255,.14)'; for (let i = 0; i < w / 32; i += 2) { g.fillRect(i * 32, 0, 32, 10); g.fillRect(i * 32 + 32, h - 10, 32, 10); }
    g.fillStyle = fg; g.font = `900 ${Math.round(h * 0.48)}px system-ui, sans-serif`; g.textAlign = 'center'; g.textBaseline = 'middle'; g.fillText(text, w / 2, h / 2 + 4);
  });
}

// 나선 조각 모양(윗면 · 안팎 옆면 · 아랫면 · 양 끝)
function arcGeometry(s) {
  const n = Math.max(4, Math.ceil((s.a1 - s.a0) * RT / 2.5)), pos = [], uv = [], idx = [], groups = [];
  const span = s.a1 - s.a0, len = span * RT;
  const add = (x, y, z, u, v) => { pos.push(x, y, z); uv.push(u, v); return pos.length / 3 - 1; };
  const quad = (a, b, c, d) => idx.push(a, b, c, a, c, d);
  const yAt = i => s.y0 + (s.y1 - s.y0) * i / n, aAt = i => s.a0 + span * i / n;
  let start = idx.length; const tops = [];
  for (let i = 0; i <= n; i++) { const a = aAt(i), y = yAt(i), v = len * i / n / 6; tops.push([add(s.r0 * Math.cos(a), y, s.r0 * Math.sin(a), 0, v), add(s.r1 * Math.cos(a), y, s.r1 * Math.sin(a), (s.r1 - s.r0) / 14, v)]); }
  for (let i = 0; i < n; i++) quad(tops[i][0], tops[i + 1][0], tops[i + 1][1], tops[i][1]);
  groups.push([start, idx.length - start, 0]);
  start = idx.length;
  const ring = (r, inv) => { const a0 = [], b0 = []; for (let i = 0; i <= n; i++) { const a = aAt(i), y = yAt(i); a0.push(add(r * Math.cos(a), y, r * Math.sin(a), i / n, 1)); b0.push(add(r * Math.cos(a), y - s.h, r * Math.sin(a), i / n, 0)); } for (let i = 0; i < n; i++) inv ? quad(a0[i], b0[i], b0[i + 1], a0[i + 1]) : quad(a0[i], a0[i + 1], b0[i + 1], b0[i]); };
  ring(s.r1, false); ring(s.r0, true);
  const bot = []; for (let i = 0; i <= n; i++) { const a = aAt(i), y = yAt(i) - s.h; bot.push([add(s.r0 * Math.cos(a), y, s.r0 * Math.sin(a), 0, 0), add(s.r1 * Math.cos(a), y, s.r1 * Math.sin(a), 1, 0)]); }
  for (let i = 0; i < n; i++) quad(bot[i][0], bot[i][1], bot[i + 1][1], bot[i + 1][0]);
  for (const i of [0, n]) { const a = aAt(i), y = yAt(i), c = Math.cos(a), sn = Math.sin(a); const q = [add(s.r0 * c, y, s.r0 * sn, 0, 1), add(s.r1 * c, y, s.r1 * sn, 1, 1), add(s.r1 * c, y - s.h, s.r1 * sn, 1, 0), add(s.r0 * c, y - s.h, s.r0 * sn, 0, 0)]; i ? quad(q[0], q[1], q[2], q[3]) : quad(q[0], q[3], q[2], q[1]); }
  groups.push([start, idx.length - start, 1]);
  const g = new THREE.BufferGeometry(); g.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3)); g.setAttribute('uv', new THREE.Float32BufferAttribute(uv, 2)); g.setIndex(idx);
  for (const [a, b, m] of groups) g.addGroup(a, b, m);
  g.computeVertexNormals(); return g;
}

function drawWorld(W) {
  const G = W.group;
  for (const s of W.solids) {
    const th = THEMES[s.theme];
    let mesh;
    if (s.kind === 'arc') {
      let top;
      if (s.door) { top = new THREE.MeshLambertMaterial({ map: DOORTEX, emissive: 0x000000 }); s.flashMat = top; }
      else if (s.ice) top = new THREE.MeshLambertMaterial({ map: ICETEX });
      else top = new THREE.MeshLambertMaterial({ map: TOPTEX[s.theme] });
      mesh = new THREE.Mesh(arcGeometry(s), [top, mat(th.side)]);
      G.add(mesh); s.mesh = mesh; continue;
    }
    if (s.kind === 'disc') {
      mesh = new THREE.Mesh(new THREE.CylinderGeometry(s.r, s.r, s.h, 48), [mat(th.side), mat(0xB8F0FF), mat(th.side)]);
      const ring = new THREE.Mesh(new THREE.RingGeometry(s.r * 0.2, s.r * 0.95, 48, 1, 0, Math.PI), mat(0x7FD8FF)); ring.rotation.x = -Math.PI / 2; ring.position.y = s.h / 2 + 0.01; mesh.add(ring);
    } else {
      let topM, side;
      if (s.fall) { topM = s.fake ? new THREE.MeshLambertMaterial({ map: CRACKTEX[s.tileColor] }) : mat(s.tileColor ? 0x5CE1C6 : 0x6FA8FF); side = mat(0x2A4C80); }
      else if (s.kind === 'tilt') { topM = new THREE.MeshLambertMaterial({ map: textTex('⇄ 얼음 시소 ⇄', '#BFF0FF', '#0B4A6B', 256, 256) }); side = mat(0x7FD8FF); }
      else if (s.mystery) { topM = new THREE.MeshLambertMaterial({ map: textTex('?', '#A77BFF', '#FFFFFF', 128, 128), emissive: 0x2A1060 }); side = mat(0x5B21B6); }
      else if (s.punch) { topM = new THREE.MeshLambertMaterial({ color: s.color, emissive: 0x000000 }); side = topM; s.flashMat = topM; }
      else { topM = mat(s.color != null ? s.color : th.accent); side = mat(s.move ? 0x0B47C4 : th.side); }
      mesh = new THREE.Mesh(new THREE.BoxGeometry(s.hx * 2, s.h, s.hz * 2), [side, side, topM, side, side, side]);
      if (s.punch) { const fist = new THREE.Mesh(new THREE.SphereGeometry(1.25, 16, 12), s.flashMat); fist.scale.set(0.7, 1, 1.35); fist.position.x = 0; mesh.add(fist); }
    }
    G.add(mesh); s.mesh = mesh;
  }
  for (const h of W.hazards) {
    if (h instanceof Sweeper) {
      const g = new THREE.Group();
      const post = new THREE.Mesh(new THREE.CylinderGeometry(0.5, 0.7, 2, 16), mat(0x1B3A6B)); post.position.y = 1; g.add(post);
      const cap = new THREE.Mesh(new THREE.SphereGeometry(0.7, 16, 10), mat(0xFFD23F)); cap.position.y = 2.1; g.add(cap);
      h.meshes = h.bars.map((b, i) => { const bar = new THREE.Mesh(new THREE.BoxGeometry(b.len * 2, b.y1 - b.y0, 0.5), mat(i ? C_HAZ2 : C_HAZ)); bar.position.y = (b.y0 + b.y1) / 2; const piv = new THREE.Group(); piv.add(bar); g.add(piv); return piv; });
      g.position.set(h.x, h.base, h.z); G.add(g);
    } else if (h instanceof Hammer) {
      const frame = new THREE.Group(); frame.position.set(h.x, h.py, h.z); frame.rotation.y = -h.a;
      [-7.8, 7.8].forEach(x => { const p = new THREE.Mesh(new THREE.BoxGeometry(0.6, 12, 0.6), mat(0x9D174D)); p.position.set(x, -4.2, 0); frame.add(p); });
      const top = new THREE.Mesh(new THREE.BoxGeometry(16.2, 0.6, 0.6), mat(0x9D174D)); top.position.y = 1.5; frame.add(top);
      const piv = new THREE.Group(); frame.add(piv);
      const arm = new THREE.Mesh(new THREE.CylinderGeometry(0.13, 0.13, h.len, 8), mat(0xC9D4EA)); arm.position.y = -h.len / 2; piv.add(arm);
      const head = new THREE.Mesh(new THREE.CylinderGeometry(h.r, h.r, h.r * 2.4, 22), [mat(C_HAZ), mat(0xffffff), mat(0xffffff)]); head.rotation.z = Math.PI / 2; head.position.y = -h.len; piv.add(head);
      h.piv = piv; G.add(frame);
    }
  }
  const arch = (s, y, label, bg) => {
    const q = P(s, 0, y), g = new THREE.Group(); g.position.set(q.x, y, q.z); g.rotation.y = -q.a;
    [-HW - 0.4, HW + 0.4].forEach(x => { const p = new THREE.Mesh(new THREE.CylinderGeometry(0.45, 0.45, 7, 12), mat(0xffffff)); p.position.set(x, 3.5, 0); g.add(p); const b = new THREE.Mesh(new THREE.SphereGeometry(0.8, 14, 10), mat(0xFFD23F)); b.position.set(x, 7.2, 0); g.add(b); });
    const tex = textTex(label, bg, '#FFFFFF');
    const banner = new THREE.Mesh(new THREE.BoxGeometry(15.6, 1.7, 0.5), [mat(0xffffff), mat(0xffffff), mat(0xffffff), mat(0xffffff), new THREE.MeshBasicMaterial({ map: tex }), new THREE.MeshBasicMaterial({ map: tex })]);
    banner.position.y = 6.3; g.add(banner); G.add(g); return g;
  };
  W.gates.forEach(gt => { if (gt.k > 1) arch(gt.s, gt.y, `STAGE ${gt.k} · ${W.secs[gt.k - 1].name}`, '#' + THEMES[gt.k - 1].accent.toString(16).padStart(6, '0')); });
  W.signs.forEach(sg => arch(sg.s, sg.y, sg.text, '#FF4F8B'));
  arch(W.finishS, W.finishY, 'StepUP 정상', '#1166FF');
  { const q = P(W.finishS, 0, W.finishY), g = new THREE.Group(); g.position.set(q.x, W.finishY, q.z); g.rotation.y = -q.a;
    const line = new THREE.Mesh(new THREE.PlaneGeometry(16, 1.4), new THREE.MeshBasicMaterial({ map: canvasTex(256, 32, (c) => { for (let i = 0; i < 16; i++) for (let j = 0; j < 2; j++) { c.fillStyle = (i + j) % 2 ? '#111' : '#fff'; c.fillRect(i * 16, j * 16, 16, 16); } }) }));
    line.rotation.x = -Math.PI / 2; line.position.y = 0.03; g.add(line); G.add(g); }
  { const crown = new THREE.Group(), q = P(W.finishS + 6, 0, W.topY);
    const ring = new THREE.Mesh(new THREE.CylinderGeometry(1.6, 1.9, 1.3, 6, 1, true), new THREE.MeshLambertMaterial({ color: 0xFFC400, side: THREE.DoubleSide, emissive: 0x3A2A00 })); crown.add(ring);
    for (let i = 0; i < 6; i++) { const sp = new THREE.Mesh(new THREE.ConeGeometry(0.42, 1.1, 6), mat(0xFFC400)); const a = i / 6 * TAU; sp.position.set(Math.cos(a) * 1.7, 1.1, Math.sin(a) * 1.7); crown.add(sp); const gem = new THREE.Mesh(new THREE.SphereGeometry(0.22, 8, 6), mat(i % 2 ? 0xFF4F8B : 0x24D8FF)); gem.position.set(Math.cos(a) * 1.85, 0.1, Math.sin(a) * 1.85); crown.add(gem); }
    crown.position.set(q.x, W.topY + 4, q.z); W.crown = crown; G.add(crown); }
  // 가운데 기둥(사탕 줄무늬 · StepUP 띠)
  const topY = W.topY + 14;
  { const tex = canvasTex(512, 256, (g, w, h) => { const cols = ['#1166FF', '#FFFFFF', '#24D8FF', '#FFFFFF']; for (let i = 0; i < 20; i++) { g.fillStyle = cols[i % 4]; g.beginPath(); g.moveTo(i * 32 - 64, 0); g.lineTo(i * 32 - 32, 0); g.lineTo(i * 32 - 96, h); g.lineTo(i * 32 - 128, h); g.fill(); } }); tex.wrapS = tex.wrapT = THREE.RepeatWrapping; tex.repeat.set(10, 4);
    const pil = new THREE.Mesh(new THREE.CylinderGeometry(PILLAR - 0.05, PILLAR + 2, topY + 40, 64, 1, true), new THREE.MeshLambertMaterial({ map: tex, side: THREE.DoubleSide })); pil.position.y = (topY - 40) / 2; G.add(pil);
    const capTop = new THREE.Mesh(new THREE.CylinderGeometry(PILLAR + 1, PILLAR - 0.05, 2, 64), mat(0xFFD23F)); capTop.position.y = topY + 1; G.add(capTop);
    const logo = textTex('StepUP  ·  StepUP  ·  StepUP  ·  StepUP', '#0B1A33', '#FFFFFF', 2048, 128); logo.wrapS = THREE.RepeatWrapping; logo.repeat.set(2, 1);
    for (let yb = 12; yb < topY; yb += 18) { const band = new THREE.Mesh(new THREE.CylinderGeometry(PILLAR + 0.15, PILLAR + 0.15, 3, 64, 1, true), new THREE.MeshBasicMaterial({ map: logo })); band.position.y = yb; G.add(band); }
  }
  W.solids.push(new Solid({ kind: 'disc', cx: 0, cz: 0, r: PILLAR - 0.4, top: topY, h: topY + 60, pillar: true }));
  // 깃발 · 풍선 · 바다 · 섬 · 관중석 · 구름
  const cols = [0xFF4F8B, 0xFFD23F, 0x24D8FF, 0x7CFFB2, 0xFF9F1C, 0xA77BFF];
  W.flagMeshes = [];
  W.flags.forEach((fs, i) => {
    const q = P(fs, HW + 0.9, trackY(fs)); const g = new THREE.Group(); g.position.set(q.x, q.y - 1.6, q.z);
    const pole = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.08, 6.5, 6), mat(0xffffff)); pole.position.y = 3.25; g.add(pole);
    const fl = new THREE.Mesh(new THREE.PlaneGeometry(1.8, 1.1), new THREE.MeshLambertMaterial({ color: cols[i % cols.length], side: THREE.DoubleSide })); fl.position.set(0.9, 5.8, 0); g.add(fl);
    g.rotation.y = -q.a + Math.PI / 2; G.add(g); W.flagMeshes.push({ fl, s: fs });
  });
  const rnd = mulberry(9); W.balloons = [];
  for (let i = 0; i < 46; i++) {
    const a = rnd() * TAU, r = RT + HW + 6 + rnd() * 30, y0 = -4 + rnd() * (W.topY + 18);
    const b = new THREE.Mesh(new THREE.SphereGeometry(1, 14, 12), mat(cols[i % cols.length])); b.scale.set(1, 1.2, 1);
    const str = new THREE.Mesh(new THREE.CylinderGeometry(0.02, 0.02, 2.6, 4), mat(0xffffff)); str.position.y = -2.4; b.add(str);
    b.position.set(Math.cos(a) * r, y0, Math.sin(a) * r); b.userData = { y0, ph: rnd() * 6 }; G.add(b); W.balloons.push(b);
  }
  const sea = new THREE.Mesh(new THREE.CircleGeometry(700, 64), new THREE.MeshLambertMaterial({ color: 0x2F8BFF, emissive: 0x0A2A7A })); sea.rotation.x = -Math.PI / 2; sea.position.y = -24; G.add(sea);
  const sand = new THREE.Mesh(new THREE.CylinderGeometry(70, 82, 6, 64), mat(0xFFE3A3)); sand.position.y = -24; G.add(sand);
  const grass = new THREE.Mesh(new THREE.CylinderGeometry(62, 66, 6.5, 64), mat(0x7CDB6E)); grass.position.y = -23.6; G.add(grass);
  for (let i = 0; i < 28; i++) {
    const a = i / 28 * TAU, g = new THREE.Group();
    for (let k = 0; k < 4; k++) { const st = new THREE.Mesh(new THREE.BoxGeometry(34, 4, 6), mat(cols[(i + k) % cols.length])); st.position.set(0, k * 4, k * 6); g.add(st); }
    g.position.set(Math.cos(a) * 150, -22, Math.sin(a) * 150); g.rotation.y = -a - Math.PI / 2; G.add(g);
  }
  for (let i = 0; i < 22; i++) {
    const cl = new THREE.Group(), n = 3 + (rnd() * 3 | 0);
    for (let j = 0; j < n; j++) { const b = new THREE.Mesh(new THREE.SphereGeometry(3 + rnd() * 4, 12, 10), mat(0xffffff)); b.position.set(j * 4, rnd() * 2, rnd() * 2); cl.add(b); }
    const a = rnd() * TAU, r = 110 + rnd() * 120; cl.position.set(Math.cos(a) * r, 10 + rnd() * 60, Math.sin(a) * r); G.add(cl);
  }
  // 신발 상자(아이템)
  const boxTex = textTex('?', '#1166FF', '#FFFFFF', 128, 128);
  W.items.forEach(it => {
    const q = P(it.s, it.u, it.y), g = new THREE.Group();
    const cube = new THREE.Mesh(new THREE.BoxGeometry(0.9, 0.9, 0.9), [0, 1, 2, 3, 4, 5].map(() => new THREE.MeshLambertMaterial({ map: boxTex, emissive: 0x0A2A7A })));
    const rib = new THREE.Mesh(new THREE.BoxGeometry(0.95, 0.18, 0.95), mat(0xFFD23F)); cube.add(rib);
    const rib2 = new THREE.Mesh(new THREE.BoxGeometry(0.18, 0.95, 0.95), mat(0xFFD23F)); cube.add(rib2);
    g.add(cube); g.position.set(q.x, it.y + 1.1, q.z); it.mesh = g; G.add(g);
  });
  // 내 최고 지점 깃발
  const best = new THREE.Group();
  const bp = new THREE.Mesh(new THREE.CylinderGeometry(0.07, 0.07, 3.4, 6), mat(0xffffff)); bp.position.y = 1.7; best.add(bp);
  const bf = new THREE.Mesh(new THREE.PlaneGeometry(1.4, 0.9), new THREE.MeshBasicMaterial({ color: 0xFFD23F, side: THREE.DoubleSide, transparent: true, opacity: 0.85 })); bf.position.set(0.7, 3, 0); best.add(bf);
  best.visible = false; W.bestFlag = best; G.add(best);
}

function syncWorld(W, now) {
  for (const s of W.solids) {
    const m = s.mesh; if (!m) continue;
    if (s.kind === 'arc') {
      if (s.door) { m.visible = s.enabled; s.flashMat.emissive.setHex(s.warn && Math.floor(now / 90) % 2 ? 0x662200 : 0x000000); }
      continue;
    }
    const shake = s.fall && s.state === 'shake' ? Math.sin(now / 16) * 0.07 : 0;
    m.position.set(s.cx + shake, s.top - s.h / 2 - (s.fall ? s.drop * s.drop * 9 : 0), s.cz);
    m.rotation.set(0, -s.yaw, s.kind === 'tilt' ? s.tilt : 0);
    m.visible = !(s.fall && s.drop > 1.6);
    if (s.flashMat) s.flashMat.emissive.setHex(s.flash && Math.floor(now / 90) % 2 ? 0xFF2200 : 0x000000);
  }
  for (const h of W.hazards) {
    if (h instanceof Sweeper) h.bars.forEach((b, i) => { h.meshes[i].rotation.y = -b.a; });
    else if (h instanceof Hammer) h.piv.rotation.z = h.f;
  }
  if (W.balls) for (const b of W.balls.list) {
    if (!b.mesh) { b.mesh = new THREE.Mesh(new THREE.SphereGeometry(b.r, 20, 14), mat(0xFF6B3D)); const band = new THREE.Mesh(new THREE.TorusGeometry(b.r * 0.98, 0.14, 6, 24), mat(0xffffff)); band.rotation.y = Math.PI / 2; b.mesh.add(band); W.group.add(b.mesh); }
    b.mesh.position.set(b.x, b.y, b.z); b.mesh.rotation.set(0, -b.a, 0); b.mesh.children[0].rotation.x = b.spin;
  }
  if (W.crown) { W.crown.rotation.y += 0.02; W.crown.position.y = W.topY + 4 + Math.sin(now / 500) * 0.4; }
  const wind = W.winds[0], gust = wind && wind.active(S.ht);
  W.flagMeshes.forEach((f, i) => { const inWind = wind && f.s >= wind.s0 && f.s <= wind.s1; f.fl.rotation.y = Math.sin(now / (inWind && (gust || wind.warn(S.ht)) ? 60 : 300) + i) * (inWind && gust ? 0.9 : 0.35); });
  W.balloons.forEach(b => { b.position.y = b.userData.y0 + Math.sin(now / 1400 + b.userData.ph) * 1.2; });
  W.items.forEach((it, i) => { if (!it.mesh) return; it.mesh.visible = it.on; it.mesh.rotation.y = now / 600 + i; it.mesh.position.y = it.y + 1.1 + Math.sin(now / 300 + i) * 0.15; });
  BELTTEX_TICK(now);
}
function BELTTEX_TICK() { /* 벨트는 지금 코스에 없다 */ }

// 러너 모양 — 머리 · 몸 · 머리띠 · 큰 운동화(StepUp 신발)
function makeRunner(color, mine) {
  const g = new THREE.Group(), body = new THREE.Group(); g.add(body);
  const torso = new THREE.Mesh(new THREE.CapsuleGeometry(0.34, 0.36, 6, 14), mat(color)); torso.position.y = 0.82; body.add(torso);
  const shirt = new THREE.Mesh(new THREE.CylinderGeometry(0.355, 0.355, 0.1, 16), mat(0xffffff)); shirt.position.y = 0.84; body.add(shirt);
  const head = new THREE.Mesh(new THREE.SphereGeometry(0.33, 18, 14), mat(0xFFD9B8)); head.position.y = 1.36; body.add(head);
  const hair = new THREE.Mesh(new THREE.SphereGeometry(0.345, 18, 10, 0, TAU, 0, Math.PI * 0.42), mat(mine ? 0x2A1B12 : [0x2A1B12, 0x6B3E1F, 0xE0B04A, 0x1B1B1B][color % 4])); hair.position.y = 1.38; body.add(hair);
  const band = new THREE.Mesh(new THREE.TorusGeometry(0.335, 0.055, 6, 22), mat(mine ? 0xFFD23F : color)); band.rotation.x = Math.PI / 2; band.position.y = 1.45; body.add(band);
  [-0.11, 0.11].forEach(x => { const e = new THREE.Mesh(new THREE.SphereGeometry(0.045, 8, 6), mat(0x0B1A33)); e.position.set(x, 1.38, -0.3); body.add(e); });
  const legs = [];
  [-0.17, 0.17].forEach(x => {
    const leg = new THREE.Group(); leg.position.set(x, 0.5, 0);
    const shin = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.08, 0.34, 6), mat(0x0B1A33)); shin.position.y = -0.2; leg.add(shin);
    const shoe = new THREE.Mesh(new THREE.BoxGeometry(0.26, 0.17, 0.5), mat(0xffffff)); shoe.position.set(0, -0.4, -0.08); leg.add(shoe);
    const sole = new THREE.Mesh(new THREE.BoxGeometry(0.27, 0.06, 0.52), mat(color)); sole.position.set(0, -0.49, -0.08); leg.add(sole);
    body.add(leg); legs.push(leg);
  });
  const arms = [];
  [-0.43, 0.43].forEach(x => { const a = new THREE.Mesh(new THREE.CapsuleGeometry(0.075, 0.26, 4, 8), mat(color)); a.position.set(x, 0.86, 0); body.add(a); arms.push(a); });
  const bub = new THREE.Mesh(new THREE.SphereGeometry(1.0, 18, 14), new THREE.MeshBasicMaterial({ color: 0x7FD8FF, transparent: true, opacity: 0.28, depthWrite: false })); bub.position.y = 0.85; bub.visible = false; g.add(bub); g.userData.bubble = bub;
  if (mine) { const ar = new THREE.Mesh(new THREE.ConeGeometry(0.22, 0.4, 4), new THREE.MeshBasicMaterial({ color: 0xFFD23F })); ar.rotation.x = Math.PI; ar.position.y = 2.2; g.add(ar); g.userData.arrow = ar; }
  Object.assign(g.userData, { body, legs, arms, phase: 0 });
  return g;
}
function nameTag(text, color) {
  const tex = canvasTex(256, 64, (g, w, h) => { g.fillStyle = 'rgba(11,26,51,.72)'; g.beginPath(); if (g.roundRect) g.roundRect(4, 6, w - 8, h - 12, 26); else g.rect(4, 6, w - 8, h - 12); g.fill(); g.fillStyle = color; g.font = '800 30px system-ui, sans-serif'; g.textAlign = 'center'; g.textBaseline = 'middle'; g.fillText(text, w / 2, h / 2 + 1); });
  const sp = new THREE.Sprite(new THREE.SpriteMaterial({ map: tex, depthTest: false, transparent: true })); sp.scale.set(1.3, 0.33, 1); sp.position.y = 2.1; sp.renderOrder = 5; return sp;
}
function animateRunner(g, speed, air, dt, lean) {
  const u = g.userData; u.phase += dt * (4 + speed * 1.4);
  const k = air ? 0.25 : Math.min(1, speed / 6), s = Math.sin(u.phase) * 0.9 * k;
  u.legs[0].rotation.x = s; u.legs[1].rotation.x = -s; u.arms[0].rotation.x = -s; u.arms[1].rotation.x = s;
  u.body.position.y = air ? 0 : Math.abs(Math.sin(u.phase)) * 0.08 * k;
  u.body.rotation.x = lean === 'dive' ? -1.25 : lean === 'shove' ? -0.45 : 0; u.body.position.z = lean === 'dive' ? -0.3 : 0;
  if (lean === 'shove') { u.arms[0].rotation.x = -1.5; u.arms[1].rotation.x = -1.5; }
  if (u.arrow) u.arrow.position.y = 2.2 + Math.sin(performance.now() / 200) * 0.12;
}

// ════════════════════════════════════════════════════════════════════
//  몸(나 · AI)
// ════════════════════════════════════════════════════════════════════
const PERSONAS = [
  { key: 'rush', name: '돌진왕', speed: 1.0, wait: 0, sloppy: 0.2, color: 0xFF4F8B },
  { key: 'bully', name: '방해꾼', speed: 0.93, wait: 0.5, sloppy: 0.12, bully: 1, color: 0xE11D48 },
  { key: 'block', name: '길막이', speed: 0.82, wait: 0.6, sloppy: 0.1, block: 1, color: 0xFF9F1C },
  { key: 'copy', name: '따라쟁이', speed: 0.92, wait: 0.5, sloppy: 0.12, copy: 1, color: 0xA77BFF },
  { key: 'shy', name: '겁쟁이', speed: 0.9, wait: 1, sloppy: 0.05, shy: 1, color: 0x14B8A6 },
  { key: 'goof', name: '엉뚱이', speed: 0.9, wait: 0.4, sloppy: 0.15, goof: 1, color: 0xFFC400 },
  { key: 'gamble', name: '도박사', speed: 0.97, wait: 0.2, sloppy: 0.18, gamble: 1, color: 0x3DDC84 }
];
const ROSTER = ['rush', 'bully', 'block', 'copy', 'shy', 'goof', 'gamble', 'rush', 'bully', 'copy', 'shy', 'goof', 'gamble', 'block', 'rush'];

class Body {
  constructor(o) {
    Object.assign(this, { pos: new THREE.Vector3(), vel: new THREE.Vector3(), ground: null, onGround: false, coyote: 0, jumpBuf: 0, stun: 0, safe: 0, yaw: 0, theta: 0, lastA: 0,
      lives: 2, out: false, deadT: 0, shoveCd: 0, shoveT: 0, diveUsed: false, diving: false, sec: 0, maxS: 0, finished: false, lastShover: null, lastShoveAt: -9, ix: 0, iz: 0, airS: 0, speedK: 1 }, o);
  }
  place(s, u) { const y = trackY(s), q = P(s, u, y); this.pos.set(q.x, y + 0.02, q.z); this.vel.set(0, 0, 0); this.theta = s / RT; this.lastA = Math.atan2(q.z, q.x); this.ground = null; this.onGround = false; this.stun = 0; this.safe = 1.0; this.diving = false; this.airS = s; this.yaw = Math.atan2(Math.sin(q.a), -Math.cos(q.a)); }
  get s() { return this.theta * RT; }
  get u() { return Math.hypot(this.pos.x, this.pos.z) - RT; }
}

// ════════════════════════════════════════════════════════════════════
//  세상 · 한 걸음
// ════════════════════════════════════════════════════════════════════
const ITEMS = [
  { k: 'boost', name: '부스터 신발', icon: '⚡', sec: 3.2 },
  { k: 'spring', name: '스프링 신발', icon: '🦘', sec: 0 },
  { k: 'shield', name: '방패', icon: '🛡️', sec: 6 },
  { k: 'zap', name: '번개 운동화', icon: '🌩️', sec: 0 }
];
const EVENTS = [
  { k: 'lowg', name: '저중력', icon: '🪐' },
  { k: 'fast', name: '빨리감기', icon: '⏩' },
  { k: 'ice', name: '꽁꽁', icon: '🧊' }
];
const S = { ev: null, evT: 0, evNext: 40, ht: 0, mode: 'title', world: null, t: 0, acc: 0, playT: 0, bodies: [], me: null, secT0: 0, secMs: [], place: 0, finishers: 0, ko: 0, revived: false, run: null, daily: false, paused: false, shovesOnMe: 0 };

function newWorld() {
  if (S.world) { scene.remove(S.world.group); S.world.group.traverse(o => { if (o.geometry) o.geometry.dispose(); }); }
  const W = buildCourse(S.courseSeed); drawWorld(W); scene.add(W.group); S.world = W; S.t = 0; S.ht = 0;
  for (const s of W.solids) { s.update(0, 0); s.save(); }
  for (const h of W.hazards) h.update(0);
  return W;
}
function secOf(s) { const W = S.world; let k = 0; for (let i = 0; i < W.secs.length; i++) if (s >= W.secs[i].s) k = i; return k; }
function hitBody(b, k) {
  if (!k || b.safe > 0 || b.shieldT > 0) return;
  if (b.onGround) b.airS = b.s;
  b.vel.x = k.vx; b.vel.z = k.vz; b.vel.y = Math.max(b.vel.y, k.vy); b.stun = k.stun; b.safe = 0.35; b.onGround = false; b.ground = null; b.diving = false;
  if (b === S.me) sfx('hit');
}

function stepBody(b, dt) {
  const W = S.world, p = b.pos, v = b.vel;
  if (b.ground && b.ground.enabled) b.ground.carry(p);
  let ix = b.ix, iz = b.iz; const il = Math.hypot(ix, iz); if (il > 1) { ix /= il; iz /= il; }
  const sp = SPEED * b.speedK * (b.boostT > 0 ? 1.45 : 1), ice = b.onGround && b.ground && (b.ground.ice || S.ev === 'ice');
  const ctl = b.diving ? 0.08 : b.stun > 0 ? 0.12 : ice ? 0.13 : 1;
  const acc = (b.onGround ? ACC_GROUND : ACC_AIR) * ctl;
  const tx = ix * sp, tz = iz * sp, dvx = tx - v.x, dvz = tz - v.z, dl = Math.hypot(dvx, dvz), lim = acc * dt;
  if (dl > lim) { v.x += dvx / dl * lim; v.z += dvz / dl * lim; } else { v.x = tx; v.z = tz; }
  if (b.onGround && b.ground && b.ground.kind === 'tilt') { const g = b.ground, sl = -Math.sin(g.tilt) * 11 * dt; p.x += Math.cos(g.yaw) * sl; p.z += Math.sin(g.yaw) * sl; }
  for (const w of W.winds) if (w.active(S.ht) && b.s >= w.s0 && b.s <= w.s1) { const a = Math.atan2(p.z, p.x); p.x += Math.cos(a) * w.force * dt; p.z += Math.sin(a) * w.force * dt; }
  b.coyote = b.onGround ? COYOTE : Math.max(0, b.coyote - dt);
  b.jumpBuf = Math.max(0, b.jumpBuf - dt);
  if (b.jumpBuf > 0 && b.coyote > 0 && b.stun <= 0) { v.y = JUMP_V * (b.springN > 0 ? 1.25 : 1); if (b.springN > 0) b.springN--; b.coyote = 0; b.jumpBuf = 0; b.onGround = false; b.ground = null; b.diveUsed = false; if (b === S.me) sfx('jump'); }
  else if (b.jumpBuf > 0 && !b.onGround && !b.diveUsed && b.stun <= 0 && b.coyote <= 0) {
    const f = [-Math.sin(b.yaw), -Math.cos(b.yaw)]; // 다이브: 공중에서 한 번 더 — 보는 쪽으로 몸을 날린다
    v.x = f[0] * (sp + DIVE_V); v.z = f[1] * (sp + DIVE_V); v.y = Math.max(v.y, 3); b.diveUsed = true; b.diving = true; b.jumpBuf = 0; if (b === S.me) sfx('dive');
  }
  v.y = Math.max(-45, v.y + GRAV * (S.ev === 'lowg' ? 0.5 : 1) * dt);
  b.boostT = Math.max(0, (b.boostT || 0) - dt); b.shieldT = Math.max(0, (b.shieldT || 0) - dt);
  const prevY = p.y;
  p.x += v.x * dt; p.y += v.y * dt; p.z += v.z * dt;
  b.stun = Math.max(0, b.stun - dt); b.safe = Math.max(0, b.safe - dt); b.shoveCd = Math.max(0, b.shoveCd - dt); b.shoveT = Math.max(0, b.shoveT - dt);
  let best = null, bestTop = -Infinity;
  if (v.y <= 0.01) for (const s of W.solids) {
    if (!s.enabled || !s.near(p.x, p.y, p.z)) continue;
    const top = s.topAt(p.x, p.z);
    if (!s.inside(p.x, p.z, top > prevY + 0.01 ? R : R * 0.5)) continue;
    const allow = s.kind === 'arc' || s.kind === 'tilt' || s === b.ground ? STEP_UP + 0.3 : STEP_UP;
    if (p.y <= top + 0.02 && prevY >= top - allow && top > bestTop) { best = s; bestTop = top; }
  }
  const was = b.onGround; b.onGround = false;
  if (best) {
    p.y = bestTop; v.y = 0; b.onGround = true; b.ground = best; best.trigger();
    if (b.diving) { b.diving = false; b.stun = 0.3; }
    if (best.mystery) mystery(b);
    else if (!was) landed(b);
  } else { b.ground = null; if (was) b.airS = b.s; }
  for (const s of W.solids) {
    if (!s.enabled || s === best || !s.near(p.x, p.y, p.z)) continue;
    const top = s.topAt(p.x, p.z);
    if (p.y >= top - 0.02 || p.y + H <= s.bottom()) continue;
    if (s.pushOut(p) && s.move) {
      if (s.move.profile === 'punch' && Math.hypot(s.vx, s.vz) > 3) { const sp2 = Math.hypot(s.vx, s.vz), k2 = Math.min(1, 10 / sp2); hitBody(b, { vx: s.vx * k2, vz: s.vz * k2, vy: 6, stun: 0.45 }); }
      else { v.x += (s.vx - v.x) * 0.35; v.z += (s.vz - v.z) * 0.35; }
    }
  }
  if (b.safe <= 0) {
    for (const h of W.hazards) { const k = h.hit(b); if (k) { hitBody(b, k); break; } }
    if (W.balls && b.safe <= 0) hitBody(b, W.balls.hit(b));
  }
  const a = Math.atan2(p.z, p.x); let da = a - b.lastA; if (da > Math.PI) da -= TAU; if (da < -Math.PI) da += TAU; b.theta += da; b.lastA = a;
}
// 아래층에 내려앉으면 감긴 각도를 한 바퀴 되돌린다 — 그리고 혈압 문구
function landed(b) {
  while (b.theta > TAU && trackY(b.s) - b.pos.y > 4) b.theta -= TAU;
  const lost = b.airS - b.s;
  if (b === S.me) { sfx('land'); if (lost > 25) { flash(`−${Math.round(lost)}m · ${TAUNTS[Math.random() * TAUNTS.length | 0]}`, 2.4); sfx('fall'); } }
  b.airS = b.s;
}
function mystery(b) { // ? 점프대: 55% 앞으로 멀리 · 45% 뒤로 튕김
  const a = b.s / RT, t = [-Math.sin(a), Math.cos(a)], fwd = Math.random() < 0.55;
  b.vel.set(t[0] * (fwd ? 11 : -10), fwd ? 16 : 14, t[1] * (fwd ? 11 : -10)); b.onGround = false; b.ground = null; b.airS = b.s; b.stun = 0.25;
  if (b === S.me) { sfx('boing'); flash(fwd ? '럭키! 🍀' : '아… 뒤로 😵'); }
}

function bodiesCollide() {
  const L = S.bodies.filter(b => !b.out && b.deadT <= 0 && !b.finished);
  for (let i = 0; i < L.length; i++) for (let j = i + 1; j < L.length; j++) {
    const a = L[i], b = L[j], dx = b.pos.x - a.pos.x, dz = b.pos.z - a.pos.z;
    if (Math.abs(dx) > 1 || Math.abs(dz) > 1 || Math.abs(a.pos.y - b.pos.y) > H) continue;
    const d = Math.hypot(dx, dz), need = R * 2; if (d >= need || d < 1e-4) continue;
    const narrow = x => x.ground && x.ground.kind === 'arc' && x.ground.r1 - x.ground.r0 < 5;
    if (!a.isMe && !b.isMe && (narrow(a) || narrow(b))) continue;
    const push = (need - d) / 2, nx = dx / d, nz = dz / d;
    a.pos.x -= nx * push; a.pos.z -= nz * push; b.pos.x += nx * push; b.pos.z += nz * push;
    wallResolve(a); wallResolve(b);
    const rv = (b.vel.x - a.vel.x) * nx + (b.vel.z - a.vel.z) * nz;
    if (rv < 0) { const k = rv * 0.5; a.vel.x += nx * k; a.vel.z += nz * k; b.vel.x -= nx * k; b.vel.z -= nz * k; }
    if (a.diving && b.safe <= 0) { hitBody(b, { vx: nx * 8, vz: nz * 8, vy: 4, stun: 0.35 }); b.lastShover = a; b.lastShoveAt = S.t; }
    if (b.diving && a.safe <= 0) { hitBody(a, { vx: -nx * 8, vz: -nz * 8, vy: 4, stun: 0.35 }); a.lastShover = b; a.lastShoveAt = S.t; }
  }
}
// 몸끼리 밀어낸 뒤 벽 · 기둥 안으로 들어갔으면 다시 밖으로(밀치기 뒤 끼임)
function wallResolve(b) {
  const p = b.pos;
  for (const s of S.world.solids) {
    if (!s.enabled || s === b.ground || !s.near(p.x, p.y, p.z)) continue;
    const top = s.topAt(p.x, p.z); if (p.y >= top - 0.02 || p.y + H <= s.bottom()) continue;
    s.pushOut(p);
  }
}
function pickItem(b, it) {
  it.on = false; it.t = 9;
  const I = ITEMS[(Math.random() * ITEMS.length) | 0];
  if (I.k === 'boost') b.boostT = I.sec;
  else if (I.k === 'spring') b.springN = 2;
  else if (I.k === 'shield') b.shieldT = I.sec;
  else if (I.k === 'zap') for (const o of S.bodies) { if (o === b || o.out || o.deadT > 0 || o.shieldT > 0) continue; if (Math.hypot(o.pos.x - b.pos.x, o.pos.z - b.pos.z) < 9 && Math.abs(o.pos.y - b.pos.y) < 3) { o.stun = Math.max(o.stun, 1.3); o.vel.set(0, 4, 0); o.onGround = false; o.ground = null; } }
  if (b === S.me) { sfx('item'); flash(`${I.icon} ${I.name}${I.k === 'spring' ? ' · 점프 2번 높이' : I.k === 'zap' ? ' · 주변 AI 기절!' : ` · ${I.sec}초`}`, 1.8); }
}
function shove(b) {
  if (b.shoveCd > 0 || b.stun > 0 || b.out || b.deadT > 0) return false;
  b.shoveCd = SHOVE_CD; b.shoveT = 0.25;
  const f = [-Math.sin(b.yaw), -Math.cos(b.yaw)];
  let hit = false;
  for (const o of S.bodies) {
    if (o === b || o.out || o.deadT > 0 || o.finished || o.shieldT > 0) continue;
    const dx = o.pos.x - b.pos.x, dz = o.pos.z - b.pos.z, d = Math.hypot(dx, dz);
    if (d > SHOVE_RANGE || Math.abs(o.pos.y - b.pos.y) > 1.2 || (dx * f[0] + dz * f[1]) / Math.max(d, 1e-3) < 0.35) continue;
    if (o === S.me && b !== S.me) { if (S.shovesOnMe >= 2 + S.me.sec) continue; S.shovesOnMe++; }
    const k = 9.5 / Math.max(d, 0.5);
    hitBody(o, { vx: dx * k, vz: dz * k, vy: 4.5, stun: 0.4 }); o.lastShover = b; o.lastShoveAt = S.t; hit = true;
  }
  if (b === S.me) sfx(hit ? 'shove' : 'whiff');
  return hit;
}

// ── AI ───────────────────────────────────────────────────────────────
function relS(x, z, s) { return (mod(Math.atan2(z, x) - mod(s / RT, TAU) + Math.PI, TAU) - Math.PI) * RT; }
function aiThink(b, dt) {
  const W = S.world, p = b.pos, s = b.s, u = b.u, P0 = b.persona, rnd = b.rnd;
  const a = s / RT, t = [-Math.sin(a), Math.cos(a)], n = [Math.cos(a), Math.sin(a)];
  if (b.stun > 0) { b.ix = 0; b.iz = 0; return; }
  b.goofT = Math.max(0, (b.goofT || 0) - dt);
  if (P0.goof && !b.goofT && rnd() < dt * 0.12) { b.goofT = 0.6 + rnd() * 0.8; b.goofKind = ['stop', 'back', 'hop', 'spin'][rnd() * 4 | 0]; }
  if (b.goofT > 0) {
    if (b.goofKind === 'stop') { b.ix = b.iz = 0; return; }
    if (b.goofKind === 'back') { b.ix = -t[0] * 0.6; b.iz = -t[1] * 0.6; return; }
    if (b.goofKind === 'spin') { const w = S.t * 9; b.ix = Math.cos(w); b.iz = Math.sin(w); return; }
    if (b.goofKind === 'hop' && b.onGround) b.jumpBuf = JUMP_BUF;
  }
  let ut = b.lane || 0, go = 1;
  // 다음 발 디딜 곳(좁은 다리 · 움직이는 발판 · 원판 · 디딤돌)
  let next = null;
  for (const sd of W.solids) {
    if (!sd.enabled || sd.pillar || !sd.near(p.x, p.y, p.z) && sd.kind !== 'arc') continue;
    if (sd.kind === 'arc') {
      if (s + 3.4 >= sd.s0 && s + 3.4 <= sd.s0 + sd.len && Math.abs(sd.y0 - p.y) < 3 && sd.r1 - sd.r0 < 13) { const half = (sd.r1 - sd.r0) / 2 - 0.7; next = { u: (sd.r0 + sd.r1) / 2 - RT, half: Math.max(0.2, half) }; }
      continue;
    }
    if (sd.fall || sd.mystery && !P0.gamble) continue;
    const top = sd.topAt(sd.cx, sd.cz); if (top > p.y + 0.7 || top < p.y - 2.5) continue;
    const dsd = relS(sd.cx, sd.cz, s); if (dsd < 0.5 || dsd > 7.5) continue;
    if (!next || next.ds == null || dsd < next.ds) next = { u: Math.hypot(sd.cx, sd.cz) - RT, half: sd.kind === 'disc' ? sd.r * 0.45 : sd.hx * 0.55, ds: dsd };
  }
  let misaligned = false;
  if (next) { ut = next.u + Math.max(-next.half, Math.min(next.half, ut - next.u)); misaligned = next.ds == null && Math.abs(u - next.u) > next.half + 0.4; if (misaligned) go = Math.max(0.2, 1 - (Math.abs(u - next.u) - next.half) / 2.5); }
  for (const it of W.items) if (it.on && it.s - s > 1 && it.s - s < 7 && Math.abs(it.y - p.y) < 1.5 && Math.abs(it.u - u) < 3.2) { ut = it.u; break; }
  ut = holeDodge(b, s, u, ut, p);
  for (const sd of W.solids) {
    if (!sd.move || sd.kind !== 'box' || !sd.near(p.x, p.y, p.z) || sd.top <= p.y + 0.7) continue;
    const dsd = relS(sd.cx, sd.cz, s); if (dsd < -1 || dsd > 5.5) continue;
    const su = Math.hypot(sd.cx, sd.cz) - RT;
    if (sd.punch) { ut = -Math.sign(sd.u || su) * 3.6; if (sd.flash && dsd < 3.2 && b.patient) go = 0; continue; }
    ut = -5;
    const edge = su - sd.hx + (sd.vx * n[0] + sd.vz * n[1]) * 0.3;
    if (dsd < 2.2 && dsd > 0 && edge < u + 0.7 && (b.patient || rnd() < 0.5)) go = 0;
  }
  for (const h of W.hazards) if (h instanceof Hammer) {
    // 망치가 내 줄을 막 지나 멀어질 때만 건넌다(다가오거나 가까우면 기다림)
    const ds = relS(h.x, h.z, s), rel = Math.abs(h.lat - u), away = Math.sign(h.v) === Math.sign(h.lat - u);
    const win = away && rel > 1.7 && rel < 4.6; // 막 지나가 멀어지는 중 — 이때 건넌다
    if (ds > 2.6 && ds < 6) { if (!win) go = Math.min(go, 0.45); }
    else if (ds >= 1.9 && ds <= 2.6 && !win && b.patient) go = 0; // 망치가 닿는 거리(약 1.6m) 밖에서만 기다리고, 안으로 들어왔으면 그대로 건넌다
  }
  if (W.door && W.door.enabled === false || W.door && W.door.warn) { const ds = W.door.s0 - s; if (ds > 0 && ds < 3.5 && b.patient) go = 0; }
  for (const w of W.winds) if (s >= w.s0 && s <= w.s1 && (w.active(S.ht) || w.warn(S.ht))) ut = Math.min(ut, -2.5);
  if (W.balls) for (const bl of W.balls.list) { const ds = bl.s - s; if (ds > -1 && ds < 8 && Math.abs(bl.u - ut) < bl.r + 1) ut = bl.u > u ? bl.u - bl.r - 1.3 : bl.u + bl.r + 1.3; }
  const me = S.me;
  if (me && !me.out && me.deadT <= 0 && !me.finished) {
    const dme = Math.hypot(me.pos.x - p.x, me.pos.z - p.z);
    if (P0.bully && dme < 5 && Math.abs(me.pos.y - p.y) < 1.2) {
      // 내 바로 뒤에 붙지 않고 옆으로 다가와 민다(뒤에 붙으면 화면에서 나를 가린다)
      const side = b.side || (b.side = u >= me.u ? 1 : -1); ut = me.u + side * 1.1; const ahead = me.s - s;
      if (ahead > -1.5 && ahead < 4) { go = ahead > 0.6 ? 1 : 0.7; if (dme < SHOVE_RANGE - 0.1 && Math.abs(ahead) < 1.2 && !(me.ground && me.ground.kind === 'arc' && me.ground.r1 - me.ground.r0 < 5) && rnd() < dt * 3) { b.yaw = Math.atan2(-(me.pos.x - p.x), -(me.pos.z - p.z)); shove(b); b.side = 0; return; } }
    }
    if (P0.copy && dme < 6 && me.s > s) { const side = b.side || (b.side = u >= me.u ? 1 : -1); ut = me.u + side * 1.6; if (me.s - s < 2.2) go = Math.min(go, 0.6); if (!me.onGround && b.onGround && me.vel.y > 5) b.jumpBuf = JUMP_BUF; }
    // 그 밖의 AI 도 내 바로 뒤 1.5m 안에는 들어오지 않는다(카메라를 가리지 않게)
    else if (!P0.bully && dme < 2.2 && me.s > s && Math.abs(me.u - u) < 1.0) ut = me.u + (u >= me.u ? 1.4 : -1.4);
  }
  if (P0.block && next && next.half < 2) ut = next.u + Math.sin(S.t * 1.7 + b.seed) * 1.0;
  if (P0.shy) { if (go === 0) b.shyT = 0.6; else if (b.shyT > 0) { b.shyT -= dt; if (b.shyT > 0.35) go = 0; } }
  ut = Math.max(-HW + 0.8, Math.min(HW - 1.6, ut));
  const lat = Math.max(-1, Math.min(1, (ut - u) * 0.9));
  b.ix = t[0] * go + n[0] * lat; b.iz = t[1] * go + n[1] * lat;
  if (b.onGround && go > 0) {
    const sl = 1.25 + (rnd() - 0.5) * P0.sloppy * 2 + (P0.gamble ? 0.5 : 0);
    const q = P(s + sl, u + lat * 0.4, 0), q2 = P(s + 0.9, u, 0);
    const gt = groundTop(q.x, q.z, p.y + 0.5), gt2 = groundTop(q2.x, q2.z, p.y + 1.2);
    let jump = gt === -Infinity || gt < p.y - 0.6 || gt2 > p.y + 0.35;
    for (const h of W.hazards) if (h instanceof Sweeper && Math.abs(h.base - p.y) < 0.8) {
      const dx = p.x - h.x, dz = p.z - h.z, dist = Math.hypot(dx, dz), phi = Math.atan2(dz, dx);
      for (const br of h.bars) {
        if (dist > br.len + 0.6 || dist < 0.9) continue;
        const sp2 = Math.abs(br.spin) * (S.ev === 'fast' ? 1.4 : 1), tt = (br.spin > 0 ? mod(phi - br.a, Math.PI) : mod(br.a - phi, Math.PI)) / sp2;
        const err = (rnd() - 0.5) * P0.sloppy * 0.5;
        if (tt > 0.17 + err && tt < 0.3 + err) jump = true;
      }
    }
    if (jump && !(misaligned && (gt === -Infinity || gt < p.y - 0.6))) b.jumpBuf = JUMP_BUF;
    if (P0.gamble && rnd() < dt * 0.03) b.jumpBuf = JUMP_BUF;
  }
  b.yaw = Math.atan2(-b.ix, -b.iz);
}
// 무너진 타일 구멍(과 눈 밝은 AI 에게는 금 간 타일)을 피해 옆 칸으로
function holeDodge(b, s, u, ut, p) {
  const ok = uu => { const q = P(s + 1.8, uu, 0); for (const sd of S.world.solids) { if (!sd.enabled || sd.pillar || !sd.near(q.x, p.y, q.z) || !sd.inside(q.x, q.z, 0)) continue; const t = sd.topAt(q.x, q.z); if (Math.abs(t - p.y) < 1.2 && !(b.patient && sd.fake) && sd.state !== 'shake') return true; } return false; };
  const onTiles = S.world.solids.some(sd => sd.fall && sd.near(p.x, p.y, p.z) && Math.abs(relS(sd.cx, sd.cz, s)) < 4);
  if (!onTiles || ok(ut)) return ut;
  let best = ut, bd = Infinity;
  for (const d of [-2.9, 2.9, -5.8, 5.8, -1.45, 1.45]) { const uu = Math.max(-HW + 0.8, Math.min(HW - 0.8, u + d)); if (ok(uu) && Math.abs(d) < bd) { bd = Math.abs(d); best = uu; } }
  return best;
}
function groundTop(x, z, below) {
  let best = -Infinity;
  for (const s of S.world.solids) if (s.enabled && !s.pillar && s.near(x, below, z) && s.inside(x, z, 0.1)) { const t = s.topAt(x, z); if (t <= below + 0.05 && t > best) best = t; }
  return best;
}

// ── 한 판 흐름 ───────────────────────────────────────────────────────
const runnerMeshes = [];
function setupRace(seed) {
  S.courseSeed = seed != null ? seed * 977 + 13 : (Math.random() * 1e9) | 0;
  newWorld();
  S.ev = null; S.evT = 0; S.evNext = 28 + Math.random() * 14;
  Object.assign(S, { bodies: [], secMs: [], place: 0, finishers: 0, ko: 0, revived: false, playT: 0, secT0: 0, shovesOnMe: 0 });
  for (const m of runnerMeshes) scene.remove(m); runnerMeshes.length = 0;
  const rnd = mulberry(seed != null ? seed : (Date.now() & 0xffffff) ^ 0x5eed);
  const slots = []; for (let r = 0; r < 4; r++) for (let c = 0; c < 4; c++) slots.push([2 + r * 2.4, -4.8 + c * 3.2]);
  const meSlot = 5;
  S.me = new Body({ name: '나', isMe: true }); S.me.place(slots[meSlot][0], slots[meSlot][1]); S.me.mesh = makeRunner(0x1166FF, true); S.bodies.push(S.me);
  let k = 0;
  slots.forEach((sl, i) => {
    if (i === meSlot) return;
    const P0 = PERSONAS.find(q => q.key === ROSTER[k % ROSTER.length]); k++;
    const b = new Body({ lives: 3, persona: P0, name: P0.name, rnd: mulberry((rnd() * 1e9) | 0), seed: rnd() * 6, lane: (rnd() * 2 - 1) * 2.5, patient: rnd() < 0.6 + P0.wait * 0.4, speedK: P0.speed * (0.93 + rnd() * 0.09) });
    b.place(sl[0], sl[1]); b.mesh = makeRunner(P0.color, false); b.tag = nameTag(P0.name, '#' + P0.color.toString(16).padStart(6, '0')); b.mesh.add(b.tag);
    S.bodies.push(b);
  });
  for (const b of S.bodies) { scene.add(b.mesh); runnerMeshes.push(b.mesh); b.maxS = b.s; }
}
function deathCheck(b) {
  if (b.out || b.deadT > 0 || b.finished || b.onGround) return;
  const ty = trackY(Math.min(b.s, S.world.endS));
  if (b.pos.y > WATER_Y && (b.pos.y > ty - DEATH_DROP || groundTop(b.pos.x, b.pos.z, b.pos.y) > -Infinity)) return;
  if (b !== S.me && b.lastShover === S.me && S.t - b.lastShoveAt < 3) { S.ko++; flash('밀어서 떨어뜨림 +50'); }
  b.lives--;
  if (b === S.me) sfx('splash');
  if (b.lives <= 0) { b.out = true; b.mesh.visible = false; if (b === S.me) endRun(false, 'dead'); return; }
  b.deadT = 1.2; b.mesh.visible = false;
  if (b === S.me) { S.revived = true; flash('풍덩! 부활 — 마지막 목숨이에요', 2); }
}
function reviveTick(b, dt) {
  b.deadT -= dt;
  if (b.deadT <= 0) { const sec = S.world.secs[b.sec]; b.place(sec.s + 2.5, b === S.me ? 0 : (b.lane || 0) * 0.5); b.mesh.visible = true; }
}
function simStep(dt) {
  const W = S.world;
  S.t += dt;
  // 돌발 이벤트: 30 ~ 50초마다 8초
  if (S.ev) { S.evT -= dt; if (S.evT <= 0) { S.ev = null; if (S.mode === 'play') flash('이벤트 끝!', 1); } }
  else if ((S.evNext -= dt) <= 0) { const E = EVENTS[(Math.random() * EVENTS.length) | 0]; S.ev = E.k; S.evT = 8; S.evNext = 30 + Math.random() * 20; if (S.mode === 'play') { banner(`<small>돌발 이벤트</small><b>${E.icon} ${E.name}</b><span>8초</span>`, 'event', 1.8); sfx('event'); } }
  const hdt = dt * (S.ev === 'fast' ? 1.4 : 1); S.ht += hdt;
  for (const s of W.solids) s.save();
  for (const tl of W.tilts) { let tq = 0; for (const b of S.bodies) if (b.ground === tl && !b.out) { const [lx] = tl.toLocal(b.pos.x, b.pos.z); tq += lx; } const target = Math.max(-0.3, Math.min(0.3, -tq * 0.035)); tl.tilt += (target - tl.tilt) * Math.min(1, dt * 2.2); }
  for (const s of W.solids) s.update(S.ht, hdt);
  for (const h of W.hazards) h.update(S.ht);
  let lead = 0; for (const b of S.bodies) if (!b.out) lead = Math.max(lead, b.s);
  if (W.balls) W.balls.update(S.ht, hdt, W, S.me && !S.me.out ? S.me.s : lead);
  for (const it of W.items) {
    if (!it.on) { it.t -= dt; if (it.t <= 0) it.on = true; continue; }
    const q = it.q || (it.q = P(it.s, it.u, it.y));
    for (const b of S.bodies) if (!b.out && b.deadT <= 0 && !b.finished && Math.abs(b.pos.x - q.x) < 1.1 && Math.abs(b.pos.z - q.z) < 1.1 && Math.abs(b.pos.y - it.y) < 1.8) { pickItem(b, it); break; }
  }
  for (const b of S.bodies) {
    if (b.out || b.finished) continue;
    if (b.deadT > 0) { reviveTick(b, dt); continue; }
    if (!b.isMe) aiThink(b, dt);
    stepBody(b, dt);
  }
  bodiesCollide();
  for (const b of S.bodies) {
    if (b.out || b.finished || b.deadT > 0) continue;
    if (b.onGround) { b.maxS = Math.max(b.maxS, b.s); const k = secOf(b.s); if (k > b.sec) { b.sec = k; if (b === S.me) sectionCleared(k); } }
    if (b.s >= W.finishS && b.pos.y > W.finishY - 1.5) {
      b.finished = true; S.finishers++; b.place = S.finishers;
      if (b === S.me) { S.place = S.finishers; sectionCleared(W.secs.length); endRun(true); return; }
    }
    deathCheck(b);
    if (S.mode !== 'play' && S.mode !== 'sim') return;
  }
  if (S.playT > RUN_LIMIT_S && S.mode === 'play') endRun(false, 'time');
}
function sectionCleared(k) {
  const ms = Math.round((S.playT - S.secT0) * 1000); S.secMs.push(ms); S.secT0 = S.playT;
  if (S.mode === 'sim') return;
  sfx('win');
  if (k < S.world.secs.length) { banner(`<small>STAGE ${k + 1}</small><b>${S.world.secs[k].name}</b>`, 'good', 1.6); confetti(S.me.pos, 60); }
  if (S.daily && S.run) api('tower_ping', { p_run: S.run, p_section: S.secMs.length, p_ms: ms }).catch(() => null);
}

// ── 화면 ─────────────────────────────────────────────────────────────
const $ = id => document.getElementById(id);
const renderer = new THREE.WebGLRenderer({ canvas: $('stage'), antialias: (window.devicePixelRatio || 1) < 2, powerPreference: 'high-performance' });
renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 1.75));
renderer.outputColorSpace = THREE.SRGBColorSpace;
const scene = new THREE.Scene();
scene.background = canvasTex(4, 256, (g, w, h) => { const gr = g.createLinearGradient(0, 0, 0, h); gr.addColorStop(0, '#3B8EFF'); gr.addColorStop(0.55, '#9ED4FF'); gr.addColorStop(1, '#E8F6FF'); g.fillStyle = gr; g.fillRect(0, 0, w, h); });
scene.fog = new THREE.Fog(0xBFE3FF, 120, 320);
scene.add(new THREE.HemisphereLight(0xEAF4FF, 0x4A5A7B, 1.7));
const sun = new THREE.DirectionalLight(0xffffff, 1.6); sun.position.set(30, 60, 20); scene.add(sun);
const camera = new THREE.PerspectiveCamera(60, 1, 0.1, 900);
const blob = new THREE.Mesh(new THREE.CircleGeometry(0.5, 20), new THREE.MeshBasicMaterial({ color: 0x000000, transparent: true, opacity: 0.28, depthWrite: false })); blob.rotation.x = -Math.PI / 2; scene.add(blob);

const CONF = 400, confGeo = new THREE.BufferGeometry(), confPos = new Float32Array(CONF * 3), confCol = new Float32Array(CONF * 3), confVel = [];
for (let i = 0; i < CONF; i++) { confPos[i * 3 + 1] = -999; confVel.push([0, 0, 0, 0]); const c = new THREE.Color([0xFF4F8B, 0xFFD23F, 0x24D8FF, 0x7CFFB2, 0xffffff][i % 5]); confCol.set([c.r, c.g, c.b], i * 3); }
confGeo.setAttribute('position', new THREE.BufferAttribute(confPos, 3)); confGeo.setAttribute('color', new THREE.BufferAttribute(confCol, 3));
const confPts = new THREE.Points(confGeo, new THREE.PointsMaterial({ size: 0.35, vertexColors: true })); confPts.frustumCulled = false; scene.add(confPts);
let confNext = 0;
function confetti(at, n) { for (let i = 0; i < n; i++) { const k = confNext++ % CONF; confPos.set([at.x, at.y + 2, at.z], k * 3); confVel[k] = [(Math.random() - 0.5) * 10, 6 + Math.random() * 8, (Math.random() - 0.5) * 10, 2.5]; } }
function confettiTick(dt) { for (let i = 0; i < CONF; i++) { const v = confVel[i]; if (v[3] <= 0) continue; v[3] -= dt; v[1] -= 14 * dt; confPos[i * 3] += v[0] * dt; confPos[i * 3 + 1] += v[1] * dt; confPos[i * 3 + 2] += v[2] * dt; if (v[3] <= 0) confPos[i * 3 + 1] = -999; } confGeo.attributes.position.needsUpdate = true; }

const ui = { title: $('title'), hud: $('hud'), stage: $('hudStage'), score: $('hudScore'), time: $('hudTime'), pos: $('hudPos'), lives: $('hudLives'), banner: $('banner'), over: $('over'), board: $('board'), toast: $('toast'), controls: $('controls'), join: $('join'), sns: $('sns'), wind: $('hudWind'), fx: $('hudFx') };
function fmt(ms) { const s = Math.max(0, ms) / 1000, m = Math.floor(s / 60), r = s - m * 60; return (m ? m + ':' + (r < 10 ? '0' : '') : '') + r.toFixed(1); }
const show = (el, on) => { el.hidden = !on; };
let toastT = 0, bannerT = 0;
function flash(text, sec) { if (S.mode === 'sim') return; ui.toast.textContent = text; ui.toast.hidden = false; toastT = sec || 1.3; }
function banner(html, cls, sec) { ui.banner.innerHTML = html; ui.banner.className = 'banner ' + (cls || ''); ui.banner.hidden = false; bannerT = sec || 0; }
function esc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c])); }
function progressNow() { const W = S.world, k = S.secMs.length; return k < W.secs.length ? Math.max(0, Math.round(S.me.maxS - W.secs[k].s)) : 0; }
function updateHud() {
  const me = S.me, W = S.world;
  ui.stage.textContent = `STAGE ${Math.min(me.sec + 1, W.secs.length)} · ${W.secs[Math.min(me.sec, W.secs.length - 1)].name}`;
  ui.score.textContent = towerScore(S.secMs.length, S.secMs, progressNow(), false, 0, S.revived, S.ko).toLocaleString('ko-KR') + '점';
  ui.time.textContent = fmt((RUN_LIMIT_S - S.playT) * 1000);
  let ahead = 0; for (const b of S.bodies) if (b !== me && !b.out && (b.finished || b.s > me.s)) ahead++;
  ui.pos.textContent = `${ahead + 1}위 · 남은 ${S.bodies.filter(b => !b.out).length}명`;
  ui.lives.textContent = me.lives >= 2 ? '❤️❤️' : '❤️🤍';
  const fx = [];
  if (me.boostT > 0) fx.push('⚡ ' + me.boostT.toFixed(1)); if (me.springN > 0) fx.push('🦘 ×' + me.springN); if (me.shieldT > 0) fx.push('🛡️ ' + me.shieldT.toFixed(1));
  if (S.ev) { const E = EVENTS.find(q => q.k === S.ev); fx.push(`${E.icon} ${E.name} ${S.evT.toFixed(0)}`); }
  ui.fx.textContent = fx.join(' · '); ui.fx.hidden = !fx.length;
  const w = W.winds.find(q => me.s >= q.s0 - 6 && me.s <= q.s1);
  ui.wind.hidden = !(w && (w.active(S.ht) || w.warn(S.ht)));
  W.bestFlag.visible = me.maxS - me.s > 15;
  if (W.bestFlag.visible) { const q = P(me.maxS, -HW + 1, trackY(me.maxS)); W.bestFlag.position.set(q.x, q.y, q.z); W.bestFlag.rotation.y = -q.a; }
}

// ── 오늘의 도전 · 서버 ─────────────────────────────────────────────────
const ACC = { email: '', name: '', status: null };
try { ACC.email = localStorage.getItem('tower_email') || ''; ACC.name = localStorage.getItem('tower_name') || ''; } catch (e) { /* 없음 */ }
async function api(name, body) {
  if (!CFG.url) throw new Error('no server');
  const r = await fetch(CFG.url + '/rest/v1/rpc/' + name, { method: 'POST', headers: { 'Content-Type': 'application/json', apikey: CFG.key }, body: JSON.stringify(body), cache: 'no-store' });
  if (!r.ok) throw new Error('http ' + r.status);
  return r.json();
}
const NAME_RE = /^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9 _.\-]{2,12}$/;
const CODES = {
  email: '이메일 주소를 다시 확인해 주세요.', not_registered: '사전 등록한 이메일이 아니에요.', name: '닉네임은 2~12자, 한글 · 영문 · 숫자로 적어 주세요.',
  busy: '잠시 뒤 다시 시도해 주세요.', no_runs: '오늘 기회를 다 썼어요. SNS에 올리면 한 번 더 할 수 있어요.', url: 'X · 인스타 · 스레드 게시물 링크가 아니에요.', account: '계정 아이디를 영문 · 숫자 · 밑줄 · 점으로 적어 주세요.',
  duplicate: '이미 낸 게시물이에요. 새 게시물을 올려 주세요.', day_limit: '오늘은 SNS로 받을 수 있는 기회(3번)를 다 썼어요. 00시에 다시 채워져요.', consent: '동의에 체크해 주세요.', closed: '지금은 게임을 쉬고 있어요.'
};
function paintStatus() {
  const st = ACC.status, who = $('whoLine'), btn = $('dailyBtn');
  if (!st) { who.innerHTML = ACC.email ? '' : '<span class="muted">사전 등록한 이메일로 참가해요</span>'; btn.textContent = '오늘의 도전'; return; }
  who.innerHTML = `<b>${esc(st.name)}</b> · 오늘 최고 ${st.best_today != null ? (+st.best_today).toLocaleString('ko-KR') + '점' : '—'} <button class="link" id="switchAcc" type="button">바꾸기</button>`;
  $('switchAcc').onclick = () => openJoin(true);
  btn.textContent = st.runs_left > 0 ? `오늘의 도전 (${st.runs_left}번 남음)` : 'SNS 올리고 한 번 더 하기';
}
async function refreshStatus() {
  if (!ACC.email) { ACC.status = null; paintStatus(); return; }
  try { const d = await api('tower_status', { p_email: ACC.email }); if (d && d.ok) ACC.status = d; else if (d && (d.code === 'not_joined' || d.code === 'not_registered')) ACC.status = null; } catch (e) { /* 그대로 */ }
  paintStatus();
}
function openJoin() { show(ui.title, false); show(ui.join, true); $('joinEmail').value = ACC.email; $('joinName').value = ACC.name; $('joinErr').textContent = ''; }
$('joinForm').onsubmit = async e => {
  e.preventDefault();
  const email = $('joinEmail').value.trim().toLowerCase(), name = $('joinName').value.replace(/\s+/g, ' ').trim(), err = $('joinErr'), btn = $('joinBtn');
  err.textContent = '';
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { err.textContent = CODES.email; return; }
  if (!NAME_RE.test(name)) { err.textContent = CODES.name; return; }
  if (!$('joinConsent').checked) { err.textContent = CODES.consent; return; }
  btn.disabled = true; btn.textContent = '확인 중…';
  try {
    const d = await api('tower_join', { p_email: email, p_name: name, p_trap: $('joinTrap').value });
    if (d && d.ok) { ACC.email = email; ACC.name = name; ACC.status = d; try { localStorage.setItem('tower_email', email); localStorage.setItem('tower_name', name); } catch (x) { /* 없음 */ } show(ui.join, false); show(ui.title, true); paintStatus(); }
    else { err.textContent = CODES[d && d.code] || '확인하지 못했어요. 잠시 뒤 다시 시도해 주세요.'; if (d && d.code === 'not_registered') err.insertAdjacentHTML('beforeend', ' <a href="/?utm_source=game&amp;utm_medium=web#join">사전 등록하기</a>'); }
  } catch (x) { err.textContent = '연결이 끊겼어요. 잠시 뒤 다시 시도해 주세요.'; }
  btn.disabled = false; btn.textContent = '참가하기';
};
$('joinBack').onclick = () => { show(ui.join, false); show(ui.title, true); };

async function startDaily() {
  if (!ACC.email) return openJoin();
  if (ACC.status && ACC.status.runs_left <= 0) return openSns();
  const btn = $('dailyBtn'); btn.disabled = true;
  try {
    const d = await api('tower_start', { p_email: ACC.email });
    if (d && d.ok) { S.daily = true; S.run = d.run; if (ACC.status) ACC.status.runs_left = d.runs_left; beginRun(); }
    else if (d && d.code === 'not_registered') { btn.disabled = false; return openJoin(); }
    else if (d && d.code === 'no_runs') { if (ACC.status) { ACC.status.runs_left = 0; ACC.status.sns_left = d.sns_left; } paintStatus(); openSns(); }
    else flash(CODES[d && d.code] || '시작하지 못했어요. 잠시 뒤 다시 시도해 주세요.', 2);
  } catch (e) { flash('연결이 끊겨 오늘의 도전을 시작하지 못했어요.', 2); }
  btn.disabled = false;
}
function startPractice() { S.daily = false; S.run = null; beginRun(); }
function beginRun() {
  unlockAudio(); setupRace();
  S.mode = 'intro'; S.intro = 5.2;
  [ui.title, ui.over, ui.board, ui.sns, ui.join].forEach(el => show(el, false)); show(ui.hud, true); show(ui.controls, true);
  banner(`<small>${S.daily ? '오늘의 도전' : '연습 · 점수 없음'}</small><b>StepUP 타워</b><span>정상까지 다섯 구간 · 물에 빠지면 부활 1번</span>`, '', 0);
  updateHud();
}
async function endRun(finished, why) {
  if (S.mode === 'sim') { S.simEnd = { finished, why }; S.mode = 'simdone'; return; }
  S.mode = 'over'; show(ui.controls, false); ui.banner.hidden = true; ui.wind.hidden = true; ui.fx.hidden = true;
  const me = S.me, k = S.secMs.length, prog = finished ? 0 : progressNow();
  const score = towerScore(k, S.secMs, prog, finished, S.place, S.revived, S.ko);
  sfx(finished ? 'win' : 'out'); if (finished) confetti(me.pos, 220);
  const head = finished ? (S.place === 1 ? '🏆 정상 1등!' : '정상 도착!') : why === 'time' ? '시간 끝!' : '탈락… 🩸';
  ui.over.innerHTML = `<h2>${head}</h2><p class="big">${score.toLocaleString('ko-KR')}점</p>
    <p class="sub">${finished ? `${S.place}위로 도착` : `STAGE ${Math.min(k + 1, 5)}까지`} · ${fmt(S.playT * 1000)}${S.ko ? ` · 밀어 떨어뜨린 AI ${S.ko}명` : ''}${S.revived ? ' · 부활 사용' : ''}</p>
    <p class="sub" id="overNote">${S.daily ? '점수를 기록하는 중…' : '연습 판은 점수가 남지 않아요.'}</p>
    <button class="btn" id="againDaily">${S.daily ? '다시 도전' : '오늘의 도전'}</button>
    <div class="row2"><button class="btn ghost" id="againPractice">연습 한 판</button><button class="btn ghost" id="toBoard">순위 보기</button></div>
    <button class="btn ghost" id="toSns">SNS 올리고 한 번 더</button><button class="link" id="toTitle" type="button">처음으로</button>`;
  show(ui.over, true);
  $('againDaily').onclick = () => { show(ui.over, false); startDaily(); };
  $('againPractice').onclick = startPractice; $('toBoard').onclick = () => openBoard(); $('toTitle').onclick = toTitle; $('toSns').onclick = openSns;
  if (!S.daily || !S.run) return;
  const run = S.run; S.run = null;
  try {
    const d = await api('tower_end', { p_run: run, p_progress: prog, p_revived: S.revived, p_ko: S.ko, p_place: finished ? S.place : 0 });
    $('overNote').textContent = d && d.ok ? `기록했어요 · 오늘 ${d.rank_today}위 · 역대 ${d.rank_all}위${d.score !== score ? ` (서버 점수 ${(+d.score).toLocaleString('ko-KR')}점)` : ''}` : '점수를 기록하지 못했어요.';
  } catch (e) { $('overNote').textContent = '연결이 끊겨 마지막으로 통과한 구간까지만 기록됐어요.'; }
  refreshStatus();
}
function toTitle() { S.mode = 'title'; [ui.over, ui.board, ui.sns, ui.join, ui.hud, ui.controls].forEach(el => show(el, false)); show(ui.title, true); for (const m of runnerMeshes) m.visible = false; S.me = null; blob.visible = false; refreshStatus(); }

// SNS 로 한 번 더
// 공개 전(내부 시험)이라 게임 주소 대신 홈 주소 — 공개할 때 게임 주소로 바꾼다
const SNS_TEXT = '나는 StepUP 타워 정상까지 갈 수 있을까? 🏃 혈압 주의 🩸\nhttps://stepupcrew.com/\n#StepUP챌린지 #스텝업 #StepUp';
function openSns() {
  [ui.title, ui.over].forEach(el => show(el, false)); show(ui.sns, true);
  $('snsErr').textContent = ''; $('snsUrl').value = ''; $('snsText').textContent = SNS_TEXT;
  const st = ACC.status; $('snsLeft').textContent = st ? `오늘 SNS로 받을 수 있는 기회 ${st.sns_left}번 남음 · 00시에 다시 3번` : '먼저 사전 등록한 이메일로 참가해 주세요.';
}
$('snsCopy').onclick = () => { (navigator.clipboard ? navigator.clipboard.writeText(SNS_TEXT) : Promise.reject(new Error('x'))).then(() => flash('글을 복사했어요. SNS에 붙여 넣어 올려 주세요.', 2), () => flash('복사가 안 돼요. 글을 길게 눌러 복사해 주세요.', 2)); };
$('snsX').href = 'https://x.com/intent/post?text=' + encodeURIComponent(SNS_TEXT);
$('snsThreads').href = 'https://www.threads.net/intent/post?text=' + encodeURIComponent(SNS_TEXT);
$('snsForm').onsubmit = async e => {
  e.preventDefault();
  const err = $('snsErr'), btn = $('snsSend'); err.textContent = '';
  if (!ACC.email) { show(ui.sns, false); return openJoin(); }
  if (!$('snsConsent').checked) { err.textContent = CODES.consent; return; }
  btn.disabled = true; btn.textContent = '확인 중…';
  try {
    const d = await api('tower_sns_submit', { p_email: ACC.email, p_url: $('snsUrl').value.trim(), p_account: $('snsAcct').value.trim(), p_consent: true, p_trap: '' });
    if (d && d.ok) { if (ACC.status) { ACC.status.runs_left = d.runs_left; ACC.status.sns_left = d.sns_left; } paintStatus(); show(ui.sns, false); show(ui.title, true); S.mode = 'title'; flash('기회 +1! 바로 도전해 보세요.', 2); }
    else err.textContent = CODES[d && d.code] || '확인하지 못했어요. 잠시 뒤 다시 시도해 주세요.';
  } catch (x) { err.textContent = '연결이 끊겼어요. 잠시 뒤 다시 시도해 주세요.'; }
  btn.disabled = false; btn.textContent = '링크 내고 한 번 더';
};
$('snsUrl').addEventListener('input', () => { $('acctRow').hidden = !/instagram\.com|(x|twitter)\.com\/i\//i.test($('snsUrl').value); });
$('snsBack').onclick = () => { show(ui.sns, false); if (S.mode === 'over') show(ui.over, true); else show(ui.title, true); };

// 순위
let boardTab = 'today', boardData = null;
async function openBoard() {
  [ui.over, ui.title, ui.controls].forEach(el => show(el, false)); show(ui.board, true);
  $('boardList').innerHTML = '<li class="muted">불러오는 중…</li>'; $('boardMe').textContent = '';
  try { boardData = await api('tower_board', { p_email: ACC.email || '' }); paintBoard(); }
  catch (e) { $('boardList').innerHTML = '<li class="muted">순위를 불러오지 못했어요.</li>'; }
}
function paintBoard() {
  const d = boardData || {}, rows = (boardTab === 'today' ? d.today : d.all) || [], me = boardTab === 'today' ? d.me_today : d.me_all;
  [].forEach.call(document.querySelectorAll('[data-board]'), b => b.setAttribute('aria-selected', String(b.dataset.board === boardTab)));
  $('boardList').innerHTML = rows.length ? rows.map(r => `<li class="${r.me ? 'me' : ''}"><b>${r.rank}</b><span>${esc(r.name)}<small>${r.cleared >= 5 ? '정상' : 'STAGE ' + (r.cleared + 1)}</small></span><em>${(+r.score).toLocaleString('ko-KR')}</em></li>`).join('')
    : `<li class="muted">${boardTab === 'today' ? '오늘은 아직 기록이 없어요. 첫 번째 주인공이 되어 보세요.' : '아직 기록이 없어요.'}</li>`;
  $('boardMe').textContent = me ? `내 ${boardTab === 'today' ? '오늘' : '역대'} 최고 ${(+me.score).toLocaleString('ko-KR')}점 · ${me.rank}위` : (d.players ? `참가 ${d.players}명` : '');
}
[].forEach.call(document.querySelectorAll('[data-board]'), b => { b.onclick = () => { boardTab = b.dataset.board; paintBoard(); }; });
$('boardClose').onclick = () => { show(ui.board, false); if (S.mode === 'over') show(ui.over, true); else show(ui.title, true); };
$('dailyBtn').onclick = startDaily; $('practiceBtn').onclick = startPractice; $('boardBtn').onclick = () => openBoard();

// ── 조작 ─────────────────────────────────────────────────────────────
const input = { keys: new Set(), stickX: 0, stickZ: 0 };
const KEYS = { ArrowUp: [0, -1], KeyW: [0, -1], ArrowDown: [0, 1], KeyS: [0, 1], ArrowLeft: [-1, 0], KeyA: [-1, 0], ArrowRight: [1, 0], KeyD: [1, 0] };
addEventListener('keydown', e => {
  if (e.target && /INPUT|TEXTAREA/.test(e.target.tagName)) return;
  if (KEYS[e.code]) { input.keys.add(e.code); e.preventDefault(); }
  if (e.code === 'Space') { if (!e.repeat && S.me) S.me.jumpBuf = JUMP_BUF; e.preventDefault(); }
  if ((e.code === 'ShiftLeft' || e.code === 'ShiftRight' || e.code === 'KeyJ') && S.mode === 'play' && S.me) shove(S.me);
});
addEventListener('keyup', e => input.keys.delete(e.code));
let camYaw = 0;
function readInput() {
  const me = S.me; if (!me) return;
  let x = 0, z = 0; for (const k of input.keys) { x += KEYS[k][0]; z += KEYS[k][1]; }
  x = Math.max(-1, Math.min(1, x + input.stickX)); z = Math.max(-1, Math.min(1, z + input.stickZ));
  const f = [-Math.sin(camYaw), -Math.cos(camYaw)], r = [-f[1], f[0]];
  me.ix = f[0] * -z + r[0] * x; me.iz = f[1] * -z + r[1] * x;
}
const stick = $('stick'), knob = $('knob'), jumpBtn = $('jumpBtn'), shoveBtn = $('shoveBtn');
let stickId = null, sx = 0, sy = 0; const STICK_R = 56;
ui.controls.addEventListener('pointerdown', e => {
  if (e.target === jumpBtn || e.target === shoveBtn || stickId !== null || e.clientX > innerWidth * 0.6) return;
  stickId = e.pointerId; sx = e.clientX; sy = e.clientY; stick.style.left = sx + 'px'; stick.style.top = sy + 'px'; stick.classList.add('on');
  ui.controls.setPointerCapture(e.pointerId); e.preventDefault();
});
ui.controls.addEventListener('pointermove', e => {
  if (e.pointerId !== stickId) return;
  let dx = e.clientX - sx, dy = e.clientY - sy; const d = Math.hypot(dx, dy); if (d > STICK_R) { dx = dx / d * STICK_R; dy = dy / d * STICK_R; }
  knob.style.transform = `translate(${dx}px, ${dy}px)`;
  const m = Math.min(1, d / STICK_R), k = m < 0.12 ? 0 : (m - 0.12) / 0.88 / Math.max(m, 1e-3);
  input.stickX = dx / STICK_R * k; input.stickZ = dy / STICK_R * k;
});
function stickEnd(e) { if (e.pointerId !== stickId) return; stickId = null; input.stickX = input.stickZ = 0; knob.style.transform = ''; stick.classList.remove('on'); }
ui.controls.addEventListener('pointerup', stickEnd); ui.controls.addEventListener('pointercancel', stickEnd);
jumpBtn.addEventListener('pointerdown', e => { if (S.me) S.me.jumpBuf = JUMP_BUF; jumpBtn.classList.add('on'); e.preventDefault(); e.stopPropagation(); });
shoveBtn.addEventListener('pointerdown', e => { if (S.mode === 'play' && S.me) shove(S.me); shoveBtn.classList.add('on'); e.preventDefault(); e.stopPropagation(); });
[jumpBtn, shoveBtn].forEach(bt => ['pointerup', 'pointercancel', 'pointerleave'].forEach(t => bt.addEventListener(t, () => bt.classList.remove('on'))));
document.addEventListener('visibilitychange', () => { S.paused = document.hidden; last = performance.now(); });

// ── 소리 ─────────────────────────────────────────────────────────────
let actx = null, muted = false;
try { muted = localStorage.getItem('race_mute') === '1'; } catch (e) { /* 없음 */ }
const muteBtn = $('muteBtn');
function paintMute() { muteBtn.textContent = muted ? '🔇' : '🔊'; muteBtn.setAttribute('aria-label', muted ? '소리 켜기' : '소리 끄기'); }
paintMute();
muteBtn.onclick = () => { muted = !muted; paintMute(); try { localStorage.setItem('race_mute', muted ? '1' : '0'); } catch (e) { /* 없음 */ } unlockAudio(); };
function unlockAudio() { if (!actx) { try { actx = new (window.AudioContext || window.webkitAudioContext)(); } catch (e) { actx = null; } } if (actx && actx.state === 'suspended') actx.resume(); }
function tone(f0, f1, dur, type = 'sine', vol = 0.12, delay = 0) {
  if (!actx || muted || S.mode === 'sim') return;
  const t0 = actx.currentTime + delay, o = actx.createOscillator(), g = actx.createGain();
  o.type = type; o.frequency.setValueAtTime(f0, t0); o.frequency.exponentialRampToValueAtTime(Math.max(30, f1), t0 + dur);
  g.gain.setValueAtTime(vol, t0); g.gain.exponentialRampToValueAtTime(0.001, t0 + dur); o.connect(g).connect(actx.destination); o.start(t0); o.stop(t0 + dur + 0.02);
}
function sfx(k) {
  if (k === 'jump') tone(420, 760, 0.12, 'square', 0.05);
  else if (k === 'land') tone(160, 90, 0.06, 'sine', 0.06);
  else if (k === 'hit') tone(220, 60, 0.22, 'sawtooth', 0.09);
  else if (k === 'dive') tone(600, 300, 0.18, 'triangle', 0.08);
  else if (k === 'shove') { tone(180, 90, 0.12, 'square', 0.1); tone(500, 700, 0.08, 'triangle', 0.06, 0.05); }
  else if (k === 'whiff') tone(300, 220, 0.08, 'sine', 0.05);
  else if (k === 'boing') tone(300, 1100, 0.25, 'triangle', 0.1);
  else if (k === 'fall') { tone(700, 90, 0.7, 'triangle', 0.08); tone(220, 110, 0.5, 'sawtooth', 0.04, 0.3); }
  else if (k === 'splash') { tone(900, 60, 0.6, 'sawtooth', 0.06); tone(200, 40, 0.5, 'square', 0.05, 0.1); }
  else if (k === 'beep') tone(520, 520, 0.14, 'square', 0.07);
  else if (k === 'item') { tone(660, 990, 0.1, 'square', 0.06); tone(990, 1320, 0.12, 'triangle', 0.06, 0.08); }
  else if (k === 'event') { tone(300, 600, 0.2, 'sawtooth', 0.06); tone(600, 300, 0.2, 'sawtooth', 0.06, 0.2); }
  else if (k === 'go') { tone(880, 880, 0.35, 'square', 0.08); tone(1320, 1320, 0.3, 'triangle', 0.05, 0.05); }
  else if (k === 'win') { tone(523, 523, 0.12, 'triangle', 0.1); tone(659, 659, 0.12, 'triangle', 0.1, 0.12); tone(784, 784, 0.25, 'triangle', 0.1, 0.24); }
  else if (k === 'out') { tone(392, 330, 0.25, 'triangle', 0.1); tone(330, 220, 0.4, 'triangle', 0.1, 0.25); }
}

// ── 돌리기 ───────────────────────────────────────────────────────────
function resize() { const w = innerWidth, h = innerHeight; renderer.setSize(w, h, false); camera.aspect = w / h; camera.fov = w / h < 0.8 ? 72 : 60; camera.updateProjectionMatrix(); }
addEventListener('resize', resize); resize();
const camPos = new THREE.Vector3(0, 30, 95), camLook = new THREE.Vector3(0, 14, 0);
function updateCamera(dt) {
  const portrait = camera.aspect < 0.8, me = S.me;
  if (!me || me.out) {
    const a = S.t * 0.08, want = new THREE.Vector3(Math.cos(a) * 95, 32, Math.sin(a) * 95);
    camPos.lerp(want, 1 - Math.exp(-dt * 1.2)); camLook.lerp(new THREE.Vector3(0, 16, 0), 1 - Math.exp(-dt * 1.2));
  } else {
    const s = Math.min(me.s, S.world.endS), a = s / RT, t = [-Math.sin(a), Math.cos(a)], n = [Math.cos(a), Math.sin(a)];
    if (S.mode === 'intro' && S.intro > 3.2) { // 출발 전: 위에서 탑을 내려다보다 내 뒤로 내려온다
      const k = Math.min(1, Math.max(0, (5.2 - S.intro) / 2)), e = k * k * (3 - 2 * k), ang = a - 0.9 * (1 - e), r = RT + 70 * (1 - e) + 10;
      camPos.set(Math.cos(ang) * r, S.world.topY + 30 * (1 - e) + 6, Math.sin(ang) * r);
      camLook.set(me.pos.x * e, 14 * (1 - e) + me.pos.y * e, me.pos.z * e); camYaw = Math.atan2(-t[0], -t[1]);
      camera.position.copy(camPos); camera.lookAt(camLook); return;
    }
    const want = Math.atan2(-t[0], -t[1]); let d = want - camYaw; d = Math.atan2(Math.sin(d), Math.cos(d)); camYaw += d * Math.min(1, dt * 5);
    const back = portrait ? 10.5 : 9, up = portrait ? 6.6 : 5.4, f = [-Math.sin(camYaw), -Math.cos(camYaw)];
    const p = me.pos, py = me.deadT > 0 ? trackY(s) : Math.max(p.y, trackY(s) - 8);
    const wantP = new THREE.Vector3(p.x - f[0] * back + n[0] * 1.5, py + up, p.z - f[1] * back + n[1] * 1.5);
    const k = 1 - Math.exp(-dt * 7); camPos.lerp(wantP, k); camLook.lerp(new THREE.Vector3(p.x + f[0] * 6, py + 1, p.z + f[1] * 6), k);
  }
  camera.position.copy(camPos); camera.lookAt(camLook);
}
function drawBodies(dt) {
  for (const b of S.bodies) {
    if (b.out) continue;
    const m = b.mesh, sp = Math.hypot(b.vel.x, b.vel.z);
    m.position.copy(b.pos);
    if (sp > 0.5 && !b.diving && b.stun <= 0 && b.shoveT <= 0) b.yaw = Math.atan2(-b.vel.x, -b.vel.z);
    let d = b.yaw - m.rotation.y; d = Math.atan2(Math.sin(d), Math.cos(d)); m.rotation.y += d * Math.min(1, dt * 14);
    m.rotation.z = b.stun > 0 ? Math.sin(b.stun * 30) * 0.3 : 0;
    if (b.deadT <= 0) m.visible = !(b.safe > 0.4 && Math.floor(b.safe * 12) % 2);
    animateRunner(m, sp, !b.onGround, dt, b.diving ? 'dive' : b.shoveT > 0 ? 'shove' : '');
    m.userData.bubble.visible = b.shieldT > 0; m.userData.body.scale.setScalar(b.boostT > 0 ? 1.08 + Math.sin(performance.now() / 60) * 0.04 : 1);
    if (b.tag) { const dd = S.me ? Math.hypot(b.pos.x - S.me.pos.x, b.pos.z - S.me.pos.z) : 99; b.tag.visible = dd > 2.5 && dd < 11 && Math.abs(b.pos.y - S.me.pos.y) < 4; }
  }
  const me = S.me; if (!me) return;
  const gt = groundTop(me.pos.x, me.pos.z, me.pos.y);
  blob.visible = gt > -Infinity && me.deadT <= 0 && !me.out; if (blob.visible) { blob.position.set(me.pos.x, gt + 0.03, me.pos.z); const s = Math.max(0.35, 1 - (me.pos.y - gt) * 0.12); blob.scale.set(s, s, s); }
}
function idleWorld(dt) { const W = S.world; S.t += dt; for (const s of W.solids) { s.save(); s.update(S.t, dt); } for (const h of W.hazards) h.update(S.t); if (W.balls) W.balls.update(S.t, dt, W, 0); }

let last = performance.now();
function frame(now) {
  requestAnimationFrame(frame);
  const dt = Math.min(0.05, (now - last) / 1000); last = now;
  if (S.paused || S.mode === 'sim' || S.mode === 'simdone') return;
  if (toastT > 0) { toastT -= dt; if (toastT <= 0) ui.toast.hidden = true; }
  if (bannerT > 0) { bannerT -= dt; if (bannerT <= 0) ui.banner.hidden = true; }
  if (S.mode === 'intro') {
    const before = Math.ceil(S.intro - 0.2); S.intro -= dt; const after = Math.ceil(S.intro - 0.2);
    if (S.intro < 3.2 && after > 0 && before !== after) { banner(`<b class="num">${after}</b><span>${['', '곧 출발', '조금만', '준비'][after] || ''}</span>`, 'count', 0); sfx('beep'); }
    idleWorld(dt); readInput(); drawBodies(dt);
    if (S.intro <= 0.2) { S.mode = 'play'; banner('<b class="num go">GO!</b>', 'count', 0.8); sfx('go'); S.t = 0; }
  } else if (S.mode === 'play') {
    readInput(); S.acc += dt;
    while (S.acc >= DT && S.mode === 'play') { S.acc -= DT; S.playT += DT; simStep(DT); }
    drawBodies(dt); if (S.mode === 'play') updateHud();
  } else { idleWorld(dt); if (S.me) drawBodies(dt); }
  updateCamera(dt); syncWorld(S.world, now); confettiTick(dt);
  renderer.render(scene, camera);
}
newWorld(); S.mode = 'title'; refreshStatus();
requestAnimationFrame(frame);

// 개발용(?dev): 화면 없이 물리를 돌려 코스를 깰 수 있는지 · 시간을 본다
if (DEV) window.__tower = { S, P, RT, HW, trackY, simStep, setupRace, stepBody, groundTop, DT, SECTIONS, towerScore, shove, aiThink, PERSONAS, mulberry, relS };
