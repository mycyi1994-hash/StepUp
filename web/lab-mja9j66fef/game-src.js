/* StepUP 타워 — 홈페이지 미니게임(2026-11-13, 서버 0104).
   나선형 탑 하나를 다섯 구간으로 끝까지 오른다. 바깥으로 떨어지면 물(목숨 하나), 안쪽 틈 · 구멍으로 떨어지면 아래층부터 다시(혈압).
   한 판 목숨 2(부활 1번) · 8분. 오늘의 도전은 하루 1판(00시 KST) + SNS 게시물 링크마다 1판(하루 3번까지). 연습은 점수 없이 몇 번이든.
   점수(tower-rules.js) = 통과 구간 × 1000 + 오른 높이 + 시간 보너스 + 완주 순위 · 무부활 + 밀어 떨어뜨린 AI — 서버가 구간 기록으로 다시 센다(tower_*).
   AI 러너 15명은 나와 같은 물리로 달리고 장애물에 맞고 떨어진다(성격 일곱 가지).
   빌드: cd web && npm run build:game  →  lab-mja9j66fef/game.js(공개 전 내부 시험 — 홈 링크 없음) (three.js 를 함께 묶는다 — CSP 가 이 사이트 스크립트만 받는다) */
import * as THREE from 'three';
import { SECTIONS, towerScore, RUN_LIMIT_S } from './tower-rules.js';
import { createFx, PALETTE } from './fx.js';
import { drawDecor, UT } from './decor.js';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';

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
      if (this.state === 'shake') { this.timer -= dt; if (this.timer <= 0) { this.state = 'drop'; this.timer = 3.2; this.enabled = false; if (this.onDrop) this.onDrop(this); } }
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
  constructor(o) { Object.assign(this, o); this.kind = 'sweep'; this.bars = o.bars.map(b => Object.assign({ a: 0 }, b)); }
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
  constructor(o) { Object.assign(this, { len: 7, r: 1.15, amp: 0.95, period: 2.1, phase: 0 }, o); this.kind = 'hammer'; this.update(0); }
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
class Bumper { // 통통 범퍼: 닿으면 튕겨 낸다(길 가운데 · 떨어질 만큼 세지는 않다)
  constructor(o) { Object.assign(this, { r: 0.95, h: 1.5 }, o); this.kind = 'bumper'; this.pulse = 0; }
  touch(b) {
    const p = b.pos; if (p.y > this.y + this.h || p.y + H < this.y) return null;
    const dx = p.x - this.x, dz = p.z - this.z, d = Math.hypot(dx, dz), need = this.r + R; if (d >= need) return null;
    const nx = d > 1e-4 ? dx / d : 1, nz = d > 1e-4 ? dz / d : 0; p.x = this.x + nx * need; p.z = this.z + nz * need; return [nx, nz];
  }
}
class Cannon { // 길 옆 대포: 길을 가로질러 공을 쏜다(side +1 = 바깥에서 기둥 쪽, −1 = 기둥에서 바깥 물 쪽 — 혈압)
  constructor(o) { Object.assign(this, { period: 2.6, phase: 0, speed: 10, r: 0.62 }, o); this.kind = 'cannon'; this.shots = []; this.lastK = null; this.u0 = this.side * (HW + 0.6); this.span = 2 * HW + 2; }
  update(t) {
    const tt = t + this.phase, k = Math.floor(tt / this.period); this.warn = mod(tt, this.period) > this.period - 0.75;
    if (this.lastK !== null && k > this.lastK) { this.shots.push({ t0: k * this.period - this.phase }); this.fired = true; }
    this.lastK = k;
    this.shots = this.shots.filter(sh => t >= sh.t0 && (t - sh.t0) * this.speed < this.span);
    for (const sh of this.shots) { sh.u = this.u0 - this.side * (t - sh.t0) * this.speed; const q = P(this.s, sh.u, this.y + this.r + 0.05); sh.x = q.x; sh.y = q.y; sh.z = q.z; }
  }
  hit(bd) {
    const p = bd.pos;
    for (const sh of this.shots) { const cy = Math.max(p.y + R, Math.min(p.y + H - R, sh.y)); if (Math.hypot(p.x - sh.x, cy - sh.y, p.z - sh.z) < this.r + R) { const a = this.s / RT, d = -this.side; return { vx: Math.cos(a) * d * 10, vz: Math.sin(a) * d * 10, vy: 5, stun: 0.45 }; } }
    return null;
  }
}
function trackY(s) {
  let y = 0;
  for (const [s0, s1, y0, y1] of PROF) { if (s < s0) break; y = s <= s1 ? y0 + (y1 - y0) * (s - s0) / (s1 - s0) : y1; }
  return y;
}
function buildCourse(seed) {
  const W = { solids: [], hazards: [], balls: null, winds: [], secs: [], gates: [], signs: [], group: new THREE.Group(), finishS: 0, flags: [], tilts: [], bumpers: [], zones: [], cannons: [], bananas: [] };
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
  // 새 요소(2026-11-13 밤): 범퍼 · 끈적이 · 가속 발판 · 점프대 · 대포 — 모두 이미 깐 바닥 위(ds 는 지금 s 에서 뒤로)
  const bumper = (ds, u) => { const q = P(s + ds, u, 0); W.bumpers.push(new Bumper({ x: q.x, z: q.z, y: trackY(s + ds), s: s + ds, u })); };
  const zone = (type, ds, u, hs, hu) => W.zones.push({ type, s: s + ds, u, hs, hu, y: trackY(s + ds) });
  const cannon = (ds, side, period) => { const c = new Cannon({ s: s + ds, side, y: trackY(s + ds), period, phase: rnd() * period }); W.hazards.push(c); W.cannons.push(c); };

  // 1 몸풀기 — 2단 막대 · 틈 뛰기
  theme = 0; section('몸풀기');
  floor(14); floor(16, 3);
  bumper(-11, -1.6); bumper(-6.5, 1.4); bumper(-2.5, -0.4);
  floor(34);
  zone('slime', -17, -3, 1.9, 1.7); zone('slime', -13.5, 3.2, 1.7, 1.5);
  sweeper(-25, [{ y0: 0.2, y1: 0.75, spin: 1.6, len: 7.3 }]);
  sweeper(-9, [{ y0: 0.2, y1: 0.75, spin: -1.75, len: 7.3 }, { y0: 1.05, y1: 1.55, spin: 1.2, len: 7.3, phase: 2.1 }]);
  floor(8, 1); gap(3.2); floor(8, 0.8, { u0: -5, u1: 5 }); gap(3.5); floor(8, 0.8, { u0: -4, u1: 4 }); gap(3); floor(6);
  floor(14, 2.4);
  zone('boost', -9, 1.2, 1.6, 1.4);
  // 2 밀치기 — 밀어내는 벽 · 튀어나오는 펀치 벽 · ? 점프대
  theme = 1; section('밀치기');
  floor(8);
  zone('slime', -4.2, -3.5, 1.7, 1.6);
  floor(40);
  [-32, -22, -12].forEach(ds => box(ds, 0, { hx: 2.6, hz: 0.7, top: y + 2.3, h: 2.3, move: { dir: nrm(ds), amp: 4.6, period: 2.0 + rnd() * 0.5, phase: ph() }, color: C_PUSH }));
  floor(10, 3, { belt: -3.2 });
  floor(32);
  [[-27, 1], [-21, -1], [-15, 1], [-9, -1]].forEach(([ds, side], i) => {
    const n = nrm(ds), dir = [-n[0] * side, -n[1] * side];
    box(ds, side * (HW + 1.2), { hx: 3.2, hz: 1.3, top: y + 2.2, h: 2.2, move: { dir, amp: 5.4, period: 2.1 + rnd() * 0.6, phase: rnd(), profile: 'punch' }, color: C_HAZ2, punch: true });
  });
  box(-2.5, (rnd() < 0.5 ? -1 : 1) * 4.4, { hx: 1.5, hz: 1.5, top: y + 0.15, h: 0.4, mystery: true });
  floor(10, 3);
  cannon(-5, 1, 2.4 + rnd() * 0.6);
  // 3 흔들 다리 — 좁은 다리와 망치(안쪽으로 떨어지면 아래층) · 움직이는 발판
  theme = 2; section('흔들 다리');
  floor(8);
  floor(38, 2, { u0: -1.8, u1: 1.8 });
  [-30, -19, -8].forEach(ds => { const q = P(s + ds, 0, 0); W.hazards.push(new Hammer({ x: q.x, z: q.z, py: trackY(s + ds) + 8.3, n: nrm(ds), phase: ph(), period: 2.0 + rnd() * 0.4, a: q.a, s: s + ds })); });
  floor(6, 0, { u0: -5, u1: 5 });
  [4.5, 11, 17.5, 24].forEach((ds, i) => box(ds, 0, { hx: 2.3, hz: 2.3, top: y, h: 1, move: { dir: nrm(ds), amp: 4.4, period: 3.1, phase: i % 2 ? Math.PI : 0 }, color: C_PUSH }));
  gap(28);
  floor(8);
  zone('boost', -6.4, -2, 1.5, 1.4); cannon(-3.6, 1, 2.2 + rnd() * 0.6);
  floor(12, 4);
  bumper(-9.5, -1.8); bumper(-6, 1.5); bumper(-2.6, -0.6);
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
  floor(8);
  cannon(-4, -1, 2.5 + rnd() * 0.6);
  floor(10, 4, { belt: -3 });
  // 5 정상 — 공 비탈 + 바람 · 얼음 2단 막대 · 결승 직전 함정 문
  theme = 4; section('정상');
  floor(6);
  zone('boost', -3, 0, 1.6, 1.6);
  const rampS = s; floor(50, 9);
  zone('pad', -39, -3.6, 1.2, 1.2); zone('pad', -22, -0.4, 1.2, 1.2); zone('slime', -45, 3, 1.8, 1.6);
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
const M1 = new THREE.Matrix4(), Q1 = new THREE.Quaternion(), E1 = new THREE.Euler(), V1 = new THREE.Vector3(), V2 = new THREE.Vector3();
function mat(color, opts) { const k = color + JSON.stringify(opts || {}); return MAT[k] || (MAT[k] = new THREE.MeshLambertMaterial(Object.assign({ color }, opts || {}))); }
function canvasTex(w, h, draw) { const c = document.createElement('canvas'); c.width = w; c.height = h; draw(c.getContext('2d'), w, h); const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; t.anisotropy = 4; return t; }
const TOPTEX = THEMES.map(th => { const t = canvasTex(128, 128, (g, w, h) => { g.fillStyle = th.top; g.fillRect(0, 0, w, h); g.fillStyle = th.stripe; for (let i = 0; i < 4; i++) g.fillRect(0, i * 32, w, 16); g.fillStyle = 'rgba(255,255,255,.55)'; g.fillRect(0, 0, 6, h); g.fillRect(w - 6, 0, 6, h); }); t.wrapS = t.wrapT = THREE.RepeatWrapping; return t; });
const ICETEX = (() => { const t = canvasTex(128, 128, (g, w, h) => { g.fillStyle = '#CFF4FF'; g.fillRect(0, 0, w, h); g.strokeStyle = 'rgba(255,255,255,.9)'; g.lineWidth = 3; for (let i = 0; i < 6; i++) { g.beginPath(); g.moveTo(Math.random() * w, Math.random() * h); g.lineTo(Math.random() * w, Math.random() * h); g.stroke(); } }); t.wrapS = t.wrapT = THREE.RepeatWrapping; return t; })();
const CRACKTEX = ['#6FA8FF', '#5CE1C6'].map(c => canvasTex(64, 64, (g, w, h) => { g.fillStyle = c; g.fillRect(0, 0, w, h); g.strokeStyle = 'rgba(11,26,51,.32)'; g.lineWidth = 1.6; g.beginPath(); g.moveTo(10, 8); g.lineTo(26, 30); g.lineTo(20, 46); g.moveTo(26, 30); g.lineTo(48, 36); g.lineTo(56, 54); g.stroke(); }));
const DOORTEX = (() => { const t = canvasTex(128, 128, (g, w, h) => { for (let i = -2; i < 8; i++) { g.fillStyle = i % 2 ? '#111' : '#FFD23F'; g.beginPath(); g.moveTo(i * 32, 0); g.lineTo(i * 32 + 32, 0); g.lineTo(i * 32 + 32 - 64, h); g.lineTo(i * 32 - 64, h); g.fill(); } }); t.wrapS = t.wrapT = THREE.RepeatWrapping; return t; })();
// 거꾸로 도는 벨트(노란 화살표가 아래로 — 뒤로 민다)
const BELTTEX = (() => { const t = canvasTex(128, 128, (g, w, h) => { g.fillStyle = '#2A3346'; g.fillRect(0, 0, w, h); g.fillStyle = '#3A4560'; for (let i = 0; i < 8; i++) g.fillRect(0, i * 16, w, 7); g.fillStyle = '#FFD23F'; for (const cx of [32, 96]) { g.beginPath(); g.moveTo(cx - 22, 40); g.lineTo(cx, 70); g.lineTo(cx + 22, 40); g.lineTo(cx + 22, 56); g.lineTo(cx, 86); g.lineTo(cx - 22, 56); g.fill(); } g.fillStyle = 'rgba(255,255,255,.6)'; g.fillRect(0, 0, 5, h); g.fillRect(w - 5, 0, 5, h); }); t.wrapS = t.wrapT = THREE.RepeatWrapping; t.repeat.set(4, 1); return t; })();
const CHEVTEX = (() => { const t = canvasTex(64, 128, (g, w, h) => { g.fillStyle = '#FF9F1C'; g.fillRect(0, 0, w, h); g.fillStyle = '#FFE066'; for (const y0 of [8, 72]) { g.beginPath(); g.moveTo(4, y0 + 40); g.lineTo(32, y0); g.lineTo(60, y0 + 40); g.lineTo(60, y0 + 56); g.lineTo(32, y0 + 16); g.lineTo(4, y0 + 56); g.fill(); } }); t.wrapS = t.wrapT = THREE.RepeatWrapping; return t; })();
function textTex(text, bg, fg, w = 512, h = 128) {
  return canvasTex(w, h, (g) => {
    g.fillStyle = bg; g.fillRect(0, 0, w, h);
    g.fillStyle = 'rgba(255,255,255,.14)'; for (let i = 0; i < w / 32; i += 2) { g.fillRect(i * 32, 0, 32, 10); g.fillRect(i * 32 + 32, h - 10, 32, 10); }
    g.fillStyle = fg; g.font = `900 ${Math.round(h * 0.48)}px system-ui, sans-serif`; g.textAlign = 'center'; g.textBaseline = 'middle'; g.fillText(text, w / 2, h / 2 + 4);
  });
}

// 상자: 옆면 다섯을 한 묶음, 윗면 하나 — 그리는 횟수 6 → 2
function boxGeo2(w, h, d) {
  const g = new THREE.BoxGeometry(w, h, d), idx = Array.from(g.index.array);
  g.setIndex([...idx.slice(0, 12), ...idx.slice(18), ...idx.slice(12, 18)]); g.clearGroups(); g.addGroup(0, 30, 0); g.addGroup(30, 6, 1); return g;
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

// 나선 조각을 윗면 · 옆면 두 모양으로(같은 재질끼리 합쳐 한 번에 그린다)
function splitArc(g) {
  const idx = g.index.array, mk = gr => { const h = new THREE.BufferGeometry(); for (const k of ['position', 'normal', 'uv']) h.setAttribute(k, g.attributes[k]); h.setIndex(Array.from(idx.slice(gr.start, gr.start + gr.count))); return h; };
  return [mk(g.groups[0]), mk(g.groups[1])];
}
function drawWorld(W) {
  const G = W.group, merged = new Map(), addTo = (key, matF, geo) => { if (!merged.has(key)) merged.set(key, { m: matF(), list: [] }); merged.get(key).list.push(geo); };
  for (const s of W.solids) {
    const th = THEMES[s.theme];
    let mesh;
    if (s.kind === 'arc') {
      if (s.door) { const top = new THREE.MeshLambertMaterial({ map: DOORTEX, emissive: 0x000000 }); s.flashMat = top; mesh = new THREE.Mesh(arcGeometry(s), [top, mat(th.side)]); G.add(mesh); s.mesh = mesh; continue; }
      const [gt, gs] = splitArc(arcGeometry(s));
      if (s.ice) addTo('ice', () => new THREE.MeshLambertMaterial({ map: ICETEX }), gt);
      else if (s.belt) addTo('belt', () => new THREE.MeshLambertMaterial({ map: BELTTEX }), gt);
      else addTo('top' + s.theme, () => new THREE.MeshLambertMaterial({ map: TOPTEX[s.theme] }), gt);
      addTo('side' + s.theme, () => mat(th.side), gs);
      continue;
    }
    if (s.kind === 'disc') {
      mesh = new THREE.Mesh(new THREE.CylinderGeometry(s.r, s.r, s.h, 48), [mat(th.side), mat(0xB8F0FF), mat(th.side)]);
      const ring = new THREE.Mesh(new THREE.RingGeometry(s.r * 0.2, s.r * 0.95, 48, 1, 0, Math.PI), mat(0x7FD8FF)); ring.rotation.x = -Math.PI / 2; ring.position.y = s.h / 2 + 0.01; mesh.add(ring);
    } else {
      let topM, side;
      if (s.fall) s.onDrop = sd => { if (!seenAt(sd.cx, sd.top, sd.cz)) return; const c = sd.fake ? [0xFF4F8B, 0x6FA8FF, 0x2A4C80] : [sd.tileColor ? 0x5CE1C6 : 0x6FA8FF, 0x2A4C80, 0xFFFFFF]; fx.burst(sd.cx, sd.top, sd.cz, { n: sd.fake ? 26 : 14, speed: 4, up: 3, colors: c, life: 1.2, size: 0.55, size1: 0.4, grav: 18, drag: 0.5, soft: true, shape: 1, jitter: 1 }); if (sd.fake) fx.word(sd.cx, sd.top + 1.8, sd.cz, '와르르', '#FF4F8B', { big: 0.8 }); };
      if (s.fall) { topM = s.fake ? new THREE.MeshLambertMaterial({ map: CRACKTEX[s.tileColor] }) : mat(s.tileColor ? 0x5CE1C6 : 0x6FA8FF); side = mat(0x2A4C80); }
      else if (s.kind === 'tilt') { topM = new THREE.MeshLambertMaterial({ map: textTex('⇄ 얼음 시소 ⇄', '#BFF0FF', '#0B4A6B', 256, 256) }); side = mat(0x7FD8FF); }
      else if (s.mystery) { topM = new THREE.MeshLambertMaterial({ map: textTex('?', '#A77BFF', '#FFFFFF', 128, 128), emissive: 0x2A1060 }); side = mat(0x5B21B6); }
      else if (s.punch) { topM = new THREE.MeshLambertMaterial({ color: s.color, emissive: 0x000000 }); side = topM; s.flashMat = topM; }
      else { topM = mat(s.color != null ? s.color : th.accent); side = mat(s.move ? 0x0B47C4 : th.side); }
      mesh = new THREE.Mesh(boxGeo2(s.hx * 2, s.h, s.hz * 2), [side, topM]);
      if (s.punch) { const fist = new THREE.Mesh(new THREE.SphereGeometry(1.25, 16, 12), s.flashMat); fist.scale.set(0.7, 1, 1.35); fist.position.x = 0; mesh.add(fist); }
    }
    G.add(mesh); s.mesh = mesh;
  }
  for (const { m, list } of merged.values()) G.add(new THREE.Mesh(mergeGeometries(list), m));
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
    const bgeo = new THREE.BoxGeometry(15.6, 1.7, 0.5); bgeo.clearGroups(); bgeo.addGroup(0, 24, 0); bgeo.addGroup(24, 12, 1);
    const banner = new THREE.Mesh(bgeo, [mat(0xffffff), new THREE.MeshBasicMaterial({ map: tex })]);
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
    const pil = new THREE.Mesh(new THREE.CylinderGeometry(PILLAR + 0.02, PILLAR + 0.02, topY + 40, 64, 1, true), new THREE.MeshLambertMaterial({ map: tex, side: THREE.DoubleSide })); pil.position.y = (topY - 40) / 2; G.add(pil);
    const capTop = new THREE.Mesh(new THREE.CylinderGeometry(PILLAR + 1, PILLAR - 0.05, 2, 64), mat(0xFFD23F)); capTop.position.y = topY + 1; G.add(capTop);
    const logo = textTex('StepUP  ·  StepUP  ·  StepUP  ·  StepUP', '#0B1A33', '#FFFFFF', 2048, 128); logo.wrapS = THREE.RepeatWrapping; logo.repeat.set(2, 1);
    for (let yb = 12; yb < topY; yb += 18) { const band = new THREE.Mesh(new THREE.CylinderGeometry(PILLAR + 0.05, PILLAR + 0.05, 3, 64, 1, true), new THREE.MeshBasicMaterial({ map: logo })); band.position.y = yb; G.add(band); }
  }
  // 몸이 보이는 기둥 겉면에서 멈춘다(보이는 기둥과 같은 굵기)
  W.solids.push(new Solid({ kind: 'disc', cx: 0, cz: 0, r: PILLAR, top: topY, h: topY + 60, pillar: true }));
  // 바다 · 섬 · 꾸밈(decor.js — 관중 · 전구 · 깃발 · 풍선 · 구름 · 관람차 · 열기구 · 비행선 …)
  const sea = new THREE.Mesh(new THREE.CircleGeometry(700, 64), new THREE.MeshLambertMaterial({ color: 0x2F8BFF, emissive: 0x0A2A7A })); sea.rotation.x = -Math.PI / 2; sea.position.y = -24; G.add(sea);
  const sand = new THREE.Mesh(new THREE.CylinderGeometry(70, 82, 6, 64), mat(0xFFE3A3)); sand.position.y = -24; G.add(sand);
  const grass = new THREE.Mesh(new THREE.CylinderGeometry(62, 66, 6.5, 64), mat(0x7CDB6E)); grass.position.y = -23.6; G.add(grass);
  W.decor = drawDecor(W, G, { P, trackY, RT, HW, PILLAR, TAU, mulberry, canvasTex, textTex, THEMES });
  // 범퍼(통통) · 끈적이 · 가속 발판 · 점프대 · 대포 · 바나나
  const bumpTex = canvasTex(128, 64, (g, w, h) => { for (let i = 0; i < 8; i++) { g.fillStyle = i % 2 ? '#FFFFFF' : '#A77BFF'; g.fillRect(i * 16, 0, 16, h); } });
  W.bumpers.forEach(bu => {
    const g = new THREE.Group(); g.position.set(bu.x, bu.y, bu.z);
    const body = new THREE.Mesh(new THREE.CylinderGeometry(bu.r, bu.r * 1.08, 1.1, 24), [new THREE.MeshLambertMaterial({ map: bumpTex, emissive: 0x000000 }), mat(0xFF4F8B), mat(0xFF4F8B)]); body.position.y = 0.55; g.add(body);
    const cap = new THREE.Mesh(new THREE.SphereGeometry(bu.r * 0.98, 20, 10, 0, TAU, 0, Math.PI / 2), new THREE.MeshLambertMaterial({ color: 0xFF4F8B, emissive: 0x000000 })); cap.position.y = 1.1; g.add(cap);
    const ring = new THREE.Mesh(new THREE.TorusGeometry(bu.r * 1.1, 0.09, 6, 28), new THREE.MeshBasicMaterial({ color: 0xFFD23F })); ring.rotation.x = Math.PI / 2; ring.position.y = 0.12; g.add(ring);
    bu.mesh = g; bu.mats = [body.material[0], cap.material]; G.add(g);
  });
  W.zones.forEach(z => {
    const q = P(z.s, z.u, z.y), g = new THREE.Group(); g.position.set(q.x, z.y + 0.03, q.z); g.rotation.y = -q.a;
    const slope = Math.atan2(trackY(z.s + 1) - trackY(z.s - 1), 2);
    if (z.type === 'slime') {
      const blob = new THREE.Mesh(new THREE.SphereGeometry(1, 20, 10, 0, TAU, 0, Math.PI / 2), new THREE.MeshLambertMaterial({ color: 0x5BE36A, emissive: 0x0E5A1C })); blob.scale.set(z.hu, 0.16, z.hs); g.add(blob);
      for (let i = 0; i < 4; i++) { const bb = new THREE.Mesh(new THREE.SphereGeometry(0.18 + i * 0.05, 10, 8), mat(0x9BFF8A)); bb.position.set((i - 1.5) * z.hu * 0.4, 0.14, Math.sin(i * 2.1) * z.hs * 0.4); g.add(bb); }
      z.blob = blob;
    } else if (z.type === 'boost') {
      const pl = new THREE.Mesh(new THREE.PlaneGeometry(z.hu * 2, z.hs * 2), new THREE.MeshBasicMaterial({ map: CHEVTEX, polygonOffset: true, polygonOffsetFactor: -4 })); pl.rotation.set(-Math.PI / 2, 0, Math.PI); g.add(pl);
    } else if (z.type === 'pad') {
      const base = new THREE.Mesh(new THREE.CylinderGeometry(z.hu, z.hu * 1.1, 0.24, 24), [mat(0x0B47C4), new THREE.MeshLambertMaterial({ map: textTex('⇧', '#7CFFB2', '#0B4A2B', 128, 128), emissive: 0x000000 }), mat(0x0B47C4)]); base.position.y = 0.1; base.rotation.y = -Math.PI / 2; g.add(base);
      const coil = new THREE.Mesh(new THREE.TorusGeometry(z.hu * 0.92, 0.07, 6, 24), new THREE.MeshBasicMaterial({ color: 0xFFFFFF })); coil.rotation.x = Math.PI / 2; coil.position.y = 0.24; g.add(coil);
      z.top = base;
    }
    g.rotateOnAxis(new THREE.Vector3(1, 0, 0), -slope);
    z.mesh = g; G.add(g);
  });
  W.cannons.forEach(c => {
    const q = P(c.s, c.u0 + c.side * 0.9, c.y), g = new THREE.Group(); g.position.set(q.x, c.y, q.z); g.rotation.y = -q.a + (c.side > 0 ? Math.PI / 2 : -Math.PI / 2);
    if (c.side > 0) { const post = new THREE.Mesh(new THREE.CylinderGeometry(0.35, 0.5, 9, 8), mat(0x1B3A6B)); post.position.y = -3.6; g.add(post); }
    const base = new THREE.Mesh(new THREE.BoxGeometry(1.6, 0.9, 1.6), mat(0x0B1A33)); base.position.y = 0.45; g.add(base);
    const barrel = new THREE.Mesh(new THREE.CylinderGeometry(0.62, 0.78, 2.4, 18), new THREE.MeshLambertMaterial({ color: 0x2A3346, emissive: 0x000000 })); barrel.rotation.x = Math.PI / 2; barrel.position.set(0, 0.75, -0.6); g.add(barrel);
    const lip = new THREE.Mesh(new THREE.TorusGeometry(0.66, 0.12, 6, 18), mat(0xFFD23F)); lip.position.set(0, 0.75, -1.8); g.add(lip);
    const skull = new THREE.Mesh(new THREE.PlaneGeometry(0.9, 0.9), new THREE.MeshBasicMaterial({ map: textTex('💥', '#FF4F8B', '#FFFFFF', 128, 128), side: THREE.DoubleSide })); skull.position.set(0, 1.55, 0); g.add(skull);
    c.mesh = g; c.barrelMat = barrel.material; G.add(g);
    c.balls = [0, 1, 2].map(() => { const m = new THREE.Mesh(new THREE.SphereGeometry(c.r, 16, 12), mat(0x2A2A2A)); const st = new THREE.Mesh(new THREE.TorusGeometry(c.r * 0.98, 0.08, 6, 18), mat(0xFF4F8B)); m.add(st); m.visible = false; G.add(m); return m; });
  });
  { const bg = new THREE.TorusGeometry(0.26, 0.09, 6, 12, Math.PI * 1.1); bg.rotateX(Math.PI / 2);
    W.bananaMesh = new THREE.InstancedMesh(bg, new THREE.MeshLambertMaterial({ color: 0xFFD23F, emissive: 0x3A2A00 }), 16); W.bananaMesh.count = 0; W.bananaMesh.frustumCulled = false; G.add(W.bananaMesh); }
  // 신발 상자(아이템)
  const boxTex = textTex('?', '#1166FF', '#FFFFFF', 128, 128), boxMat = new THREE.MeshLambertMaterial({ map: boxTex, emissive: 0x0A2A7A });
  W.items.forEach(it => {
    const q = P(it.s, it.u, it.y), g = new THREE.Group();
    const cube = new THREE.Mesh(new THREE.BoxGeometry(0.9, 0.9, 0.9), boxMat);
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

function syncWorld(W, now, dt) {
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
  if (W.decor) W.decor.tick(now, dt || 0.016, S);
  for (const bu of W.bumpers) { bu.pulse = Math.max(0, bu.pulse - (dt || 0.016) * 4); const k = 1 + bu.pulse * 0.28; bu.mesh.scale.set(k, 1 - bu.pulse * 0.15, k); const e = bu.pulse > 0.05 ? 0x6A2A8A : 0x000000; bu.mats.forEach(m => m.emissive.setHex(e)); }
  for (const z of W.zones) {
    z.pulse = Math.max(0, (z.pulse || 0) - (dt || 0.016) * 3);
    if (z.type === 'slime' && z.blob) z.blob.scale.y = 0.16 + Math.sin(now / 300 + z.s) * 0.03;
    if (z.type === 'pad' && z.top) { z.top.scale.y = 1 + z.pulse * 2.5; z.top.material[1].emissive.setHex(z.pulse > 0.05 ? 0x2A8A4A : 0x000000); }
    if (z.type === 'slime' && Math.random() < (dt || 0.016) * 2.5 && seenAt(z.mesh.position.x, z.y, z.mesh.position.z, 26)) { const q = P(z.s + (Math.random() - 0.5) * z.hs, z.u + (Math.random() - 0.5) * z.hu, z.y); fx.one(q.x, z.y + 0.2, q.z, 0, 1.2, 0, { color: 0x9BFF8A, size: 0.4, size1: 0.7, life: 0.6, soft: true, shape: 3 }); }
  }
  for (const c of W.cannons) {
    c.barrelMat.emissive.setHex(c.warn && Math.floor(now / 80) % 2 ? 0xAA1100 : 0x000000);
    c.balls.forEach((m, i) => { const sh = c.shots[i]; m.visible = !!sh; if (sh) { m.position.set(sh.x, sh.y, sh.z); m.rotation.z += 0.3; } });
    if (c.fired) { c.fired = false; const q = P(c.s, c.u0, c.y + c.r + 0.1); if (seenAt(q.x, q.y, q.z, 40)) { const a = c.s / RT, d = -c.side; fx.burst(q.x, q.y, q.z, { n: 18, speed: 6, colors: [0xFFD23F, 0xFF9F1C, 0xFFFFFF], life: 0.4, size: 0.7, vx: Math.cos(a) * d * 6, vz: Math.sin(a) * d * 6 }); fx.dust(q.x, q.y - 0.4, q.z, { n: 8, speed: 3, size: 0.8, colors: [0xBBBBBB, 0x888888] }); fx.word(q.x, q.y + 1.6, q.z, '펑!', '#FF9F1C', { big: 0.8 }); if (S.me && Math.hypot(S.me.pos.x - q.x, S.me.pos.z - q.z) < 18) sfx('cannon'); } }
    for (const sh of c.shots) if (!sh.done && Math.abs(sh.u) > HW && Math.sign(sh.u) === -c.side) { sh.done = true; if (seenAt(sh.x, sh.y, sh.z)) fx.burst(sh.x, sh.y, sh.z, { n: 12, speed: 5, colors: [0xFF4F8B, 0xFFFFFF], life: 0.35, size: 0.5 }); }
  }
  if (W.bananaMesh) { const bm = W.bananaMesh; bm.count = W.bananas.length; W.bananas.forEach((bn, i) => { E1.set(0, bn.rot, 0); Q1.setFromEuler(E1); M1.compose(V1.set(bn.x, bn.y + 0.1, bn.z), Q1, V2.set(1, 1, 1)); bm.setMatrixAt(i, M1); }); bm.instanceMatrix.needsUpdate = true; }
  W.items.forEach((it, i) => { if (!it.mesh) return; it.mesh.visible = it.on; it.mesh.rotation.y = now / 600 + i; it.mesh.position.y = it.y + 1.1 + Math.sin(now / 300 + i) * 0.15; });
  BELTTEX_TICK(now);
}
function BELTTEX_TICK(now) { BELTTEX.offset.y = (now / 1000 * 3.1 / 6) % 1; CHEVTEX.offset.y = -(now / 1000 * 1.6) % 1; }

// 러너 모양 — 고를 수 있는 것들(내 러너 꾸미기 · AI 는 판마다 랜덤). 앞은 −z(눈 쪽)
export const LOOK = {
  skin: { name: '피부', list: ['#FFE0C4', '#F5C79A', '#D9A066', '#A86B3C', '#6E4224'] },
  hair: { name: '머리', list: ['짧은 머리', '삐죽 머리', '포니테일', '똥머리', '모히칸', '긴 머리', '곱슬', '민머리'] },
  hairC: { name: '머리색', list: ['#2A1B12', '#6B3E1F', '#E0B04A', '#1B1B1B', '#D9483B', '#7F5AF0', '#F3F3F3', '#2BB3A3', '#FF7EB6'] },
  hat: { name: '모자', list: ['없음', '야구모자', '비니', '벙거지', '머리띠', '리본', '뿔', '더듬이', '왕관', '헤드폰'] },
  top: { name: '옷 색', list: ['#1166FF', '#FF4F8B', '#FF9F1C', '#FFD23F', '#3DDC84', '#14B8A6', '#A77BFF', '#E11D48', '#0B1A33', '#FFFFFF', '#7FD8FF', '#8B5E34'] },
  pattern: { name: '무늬', list: ['민무늬', '줄무늬', '번호표', '번개', '별'] },
  shorts: { name: '바지', list: ['#0B1A33', '#1B3A6B', '#2A2A2A', '#FFFFFF', '#E11D48', '#3DDC84', '#FFD23F'] },
  shoe: { name: '신발', list: ['#FFFFFF', '#1166FF', '#FF4F8B', '#FFD23F', '#111111', '#3DDC84', '#FF9F1C'] },
  acc: { name: '꾸미기', list: ['없음', '안경', '선글라스', '배낭', '목도리', '메달', '날개', '망토'] },
  size: { name: '체형', list: ['보통', '작고 통통', '크고 날씬'] }
};
const LOOK_KEYS = Object.keys(LOOK);
export const DEFAULT_LOOK = { skin: 0, hair: 0, hairC: 0, hat: 4, top: 0, pattern: 2, shorts: 0, shoe: 0, acc: 0, size: 0 };
function randomLook(rnd, topHex) {
  const L = {}; for (const k of LOOK_KEYS) L[k] = (rnd() * LOOK[k].list.length) | 0;
  if (topHex != null) { const hex = '#' + topHex.toString(16).padStart(6, '0').toUpperCase(), i = LOOK.top.list.findIndex(c => c.toUpperCase() === hex); L.topHex = hex; if (i >= 0) L.top = i; }
  if (rnd() < 0.35) L.acc = 0; if (rnd() < 0.25) L.hat = 0;
  return L;
}
function lookColor(L, k) { return k === 'top' && L.topHex ? L.topHex : LOOK[k].list[L[k] % LOOK[k].list.length]; }
function patternTex(kind, base, num) {
  return canvasTex(128, 64, (g, w, h) => {
    g.fillStyle = base; g.fillRect(0, 0, w, h);
    if (kind === 1) { g.fillStyle = 'rgba(255,255,255,.85)'; for (let i = 0; i < 4; i++) g.fillRect(0, 6 + i * 16, w, 6); }
    if (kind === 2) { g.fillStyle = '#FFFFFF'; g.fillRect(w / 2 - 22, 12, 44, 34); g.fillStyle = '#0B1A33'; g.font = '900 24px system-ui, sans-serif'; g.textAlign = 'center'; g.textBaseline = 'middle'; g.fillText(String(num), w / 2, 30); }
    if (kind === 3) { g.fillStyle = '#FFD23F'; g.beginPath(); g.moveTo(w / 2 + 6, 6); g.lineTo(w / 2 - 12, 34); g.lineTo(w / 2, 34); g.lineTo(w / 2 - 6, 58); g.lineTo(w / 2 + 14, 26); g.lineTo(w / 2 + 2, 26); g.fill(); }
    if (kind === 4) { g.fillStyle = 'rgba(255,255,255,.9)'; for (const [x, y] of [[20, 16], [64, 34], [104, 14], [40, 50], [92, 52]]) { g.beginPath(); for (let i = 0; i < 10; i++) { const r = i % 2 ? 3 : 7, a = -Math.PI / 2 + i * Math.PI / 5; g.lineTo(x + Math.cos(a) * r, y + Math.sin(a) * r); } g.fill(); } }
  });
}
function makeRunner(look, mine, num) {
  const L = Object.assign({}, DEFAULT_LOOK, look || {});
  const C = k => lookColor(L, k), skin = mat(C('skin')), hairM = mat(C('hairC')), topC = C('top');
  const g = new THREE.Group(), body = new THREE.Group(); g.add(body);
  const size = L.size % 3, sx = size === 1 ? 1.12 : size === 2 ? 0.92 : 1, sy = size === 1 ? 0.9 : size === 2 ? 1.1 : 1;
  body.scale.set(sx, sy, sx); body.userData.base = [sx, sy, sx];
  // 몸 · 무늬
  const torso = new THREE.Mesh(new THREE.CapsuleGeometry(0.34, 0.36, 6, 16), mat(topC)); torso.position.y = 0.82; body.add(torso);
  const pat = L.pattern % LOOK.pattern.list.length;
  if (pat) { const front = new THREE.Mesh(new THREE.CylinderGeometry(0.346, 0.346, 0.42, 20, 1, true, Math.PI * 0.75, Math.PI * 0.5), new THREE.MeshLambertMaterial({ map: patternTex(pat, topC, num || 7), side: THREE.DoubleSide })); front.position.y = 0.84; body.add(front); }
  const shorts = new THREE.Mesh(new THREE.CylinderGeometry(0.35, 0.33, 0.2, 16), mat(C('shorts'))); shorts.position.y = 0.56; body.add(shorts);
  // 머리 · 얼굴
  const head = new THREE.Mesh(new THREE.SphereGeometry(0.33, 18, 14), skin); head.position.y = 1.36; body.add(head);
  [-0.11, 0.11].forEach(x => { const e = new THREE.Mesh(new THREE.SphereGeometry(0.045, 8, 6), mat(0x0B1A33)); e.position.set(x, 1.38, -0.3); body.add(e); });
  const mouth = new THREE.Mesh(new THREE.TorusGeometry(0.05, 0.012, 4, 10, Math.PI), mat(0x7A2E1F)); mouth.position.set(0, 1.27, -0.315); mouth.rotation.z = Math.PI; body.add(mouth);
  [-0.17, 0.17].forEach(x => { const c = new THREE.Mesh(new THREE.SphereGeometry(0.04, 8, 6), new THREE.MeshBasicMaterial({ color: 0xFF8FA3, transparent: true, opacity: 0.6 })); c.position.set(x, 1.3, -0.28); body.add(c); });
  const hs = L.hair % LOOK.hair.list.length, add = (m, x, y, z) => { m.position.set(x, y, z); body.add(m); return m; };
  if (hs !== 7) add(new THREE.Mesh(new THREE.SphereGeometry(0.345, 18, 10, 0, TAU, 0, Math.PI * (hs === 5 ? 0.55 : 0.42)), hairM), 0, 1.38, 0);
  if (hs === 1) for (let i = 0; i < 6; i++) { const a = i / 6 * TAU, c = add(new THREE.Mesh(new THREE.ConeGeometry(0.07, 0.2, 6), hairM), Math.cos(a) * 0.17, 1.66, Math.sin(a) * 0.17 + 0.03); c.rotation.set(Math.sin(a) * 0.5, 0, -Math.cos(a) * 0.5); }
  if (hs === 2) { add(new THREE.Mesh(new THREE.SphereGeometry(0.1, 10, 8), hairM), 0, 1.5, 0.33); const t = add(new THREE.Mesh(new THREE.CapsuleGeometry(0.07, 0.3, 4, 8), hairM), 0, 1.28, 0.4); t.rotation.x = 0.4; }
  if (hs === 3) add(new THREE.Mesh(new THREE.SphereGeometry(0.15, 12, 10), hairM), 0, 1.74, 0.06);
  if (hs === 4) add(new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.2, 0.56), hairM), 0, 1.7, 0.02);
  if (hs === 5) add(new THREE.Mesh(new THREE.BoxGeometry(0.62, 0.5, 0.18), hairM), 0, 1.18, 0.22);
  if (hs === 6) for (let i = 0; i < 9; i++) { const a = i / 9 * TAU; add(new THREE.Mesh(new THREE.SphereGeometry(0.12, 8, 6), hairM), Math.cos(a) * 0.26, 1.52 + (i % 2) * 0.06, Math.sin(a) * 0.26 + 0.04); }
  // 모자
  const hat = L.hat % LOOK.hat.list.length, hatM = mat(topC === '#FFFFFF' ? '#1166FF' : '#FFFFFF'), accent = mat(topC);
  if (hat === 1) { add(new THREE.Mesh(new THREE.SphereGeometry(0.355, 16, 8, 0, TAU, 0, Math.PI * 0.45), accent), 0, 1.41, 0); add(new THREE.Mesh(new THREE.BoxGeometry(0.4, 0.04, 0.3), accent), 0, 1.52, -0.36); }
  if (hat === 2) { add(new THREE.Mesh(new THREE.SphereGeometry(0.36, 16, 10, 0, TAU, 0, Math.PI * 0.5), accent), 0, 1.42, 0); add(new THREE.Mesh(new THREE.SphereGeometry(0.1, 10, 8), hatM), 0, 1.8, 0); add(new THREE.Mesh(new THREE.CylinderGeometry(0.37, 0.37, 0.1, 18), hatM), 0, 1.44, 0); }
  if (hat === 3) { add(new THREE.Mesh(new THREE.CylinderGeometry(0.28, 0.33, 0.22, 18), accent), 0, 1.66, 0); add(new THREE.Mesh(new THREE.CylinderGeometry(0.5, 0.5, 0.03, 22), accent), 0, 1.56, 0); }
  if (hat === 4) { const b = add(new THREE.Mesh(new THREE.TorusGeometry(0.335, 0.055, 6, 22), mine ? mat(0xFFD23F) : accent), 0, 1.45, 0); b.rotation.x = Math.PI / 2; }
  if (hat === 5) [-1, 1].forEach(sd => { const c = add(new THREE.Mesh(new THREE.ConeGeometry(0.11, 0.22, 8), mat(0xFF4F8B)), sd * 0.12, 1.7, 0.05); c.rotation.z = sd * Math.PI / 2; });
  if (hat === 6) [-1, 1].forEach(sd => { const c = add(new THREE.Mesh(new THREE.ConeGeometry(0.07, 0.26, 8), mat(0xFFF3D6)), sd * 0.18, 1.72, 0); c.rotation.z = -sd * 0.4; });
  if (hat === 7) [-1, 1].forEach(sd => { const st = add(new THREE.Mesh(new THREE.CylinderGeometry(0.015, 0.015, 0.3, 5), mat(0x0B1A33)), sd * 0.1, 1.8, 0); st.rotation.z = -sd * 0.3; add(new THREE.Mesh(new THREE.SphereGeometry(0.06, 8, 6), mat(0xFFD23F)), sd * 0.15, 1.96, 0); });
  if (hat === 8) { add(new THREE.Mesh(new THREE.CylinderGeometry(0.2, 0.22, 0.12, 6, 1, true), new THREE.MeshLambertMaterial({ color: 0xFFC400, side: THREE.DoubleSide })), 0, 1.74, 0); for (let i = 0; i < 6; i++) { const a = i / 6 * TAU; add(new THREE.Mesh(new THREE.ConeGeometry(0.04, 0.1, 5), mat(0xFFC400)), Math.cos(a) * 0.2, 1.84, Math.sin(a) * 0.2); } }
  if (hat === 9) { const band = add(new THREE.Mesh(new THREE.TorusGeometry(0.36, 0.03, 6, 20, Math.PI), mat(0x222222)), 0, 1.42, 0); band.rotation.y = Math.PI / 2; [-1, 1].forEach(sd => { const cup = add(new THREE.Mesh(new THREE.CylinderGeometry(0.11, 0.11, 0.08, 14), accent), sd * 0.35, 1.36, 0); cup.rotation.z = Math.PI / 2; }); }
  // 꾸미기
  const acc = L.acc % LOOK.acc.list.length;
  if (acc === 1) { [-0.11, 0.11].forEach(x => { const r = add(new THREE.Mesh(new THREE.TorusGeometry(0.07, 0.014, 6, 14), mat(0x111111)), x, 1.38, -0.32); }); add(new THREE.Mesh(new THREE.BoxGeometry(0.08, 0.015, 0.015), mat(0x111111)), 0, 1.39, -0.33); }
  if (acc === 2) add(new THREE.Mesh(new THREE.BoxGeometry(0.4, 0.09, 0.05), mat(0x111111)), 0, 1.385, -0.31);
  if (acc === 3) { add(new THREE.Mesh(new THREE.BoxGeometry(0.4, 0.42, 0.18), mat(LOOK.top.list[(L.top + 3) % 12])), 0, 0.9, 0.34); add(new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.14, 0.06), mat(0xFFFFFF)), 0, 0.82, 0.44); }
  if (acc === 4) { const sc = add(new THREE.Mesh(new THREE.TorusGeometry(0.28, 0.07, 8, 18), mat(0xE11D48)), 0, 1.08, 0); sc.rotation.x = Math.PI / 2; add(new THREE.Mesh(new THREE.BoxGeometry(0.1, 0.32, 0.05), mat(0xE11D48)), 0.12, 0.92, 0.3); }
  if (acc === 5) { add(new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.08, 0.02, 16), mat(0xFFC400)), 0, 0.92, -0.35).rotation.x = Math.PI / 2; add(new THREE.Mesh(new THREE.BoxGeometry(0.05, 0.22, 0.02), mat(0x1166FF)), 0, 1.04, -0.33); }
  if (acc === 6) [-1, 1].forEach(sd => { const w = add(new THREE.Mesh(new THREE.CircleGeometry(0.28, 12), new THREE.MeshLambertMaterial({ color: 0xFFFFFF, side: THREE.DoubleSide })), sd * 0.24, 1.0, 0.32); w.rotation.y = sd * 0.9; w.scale.set(1, 0.6, 1); });
  if (acc === 7) { const cape = add(new THREE.Mesh(new THREE.PlaneGeometry(0.62, 0.7), new THREE.MeshLambertMaterial({ color: LOOK.top.list[(L.top + 7) % 12], side: THREE.DoubleSide })), 0, 0.78, 0.36); cape.rotation.x = 0.18; }
  // 다리 · 신발(StepUp 운동화) · 팔
  const shoeC = C('shoe'), soleC = shoeC === '#FFFFFF' ? topC : '#FFFFFF', legs = [];
  [-0.17, 0.17].forEach(x => {
    const leg = new THREE.Group(); leg.position.set(x, 0.5, 0);
    const shin = new THREE.Mesh(new THREE.CylinderGeometry(0.08, 0.08, 0.34, 6), skin); shin.position.y = -0.2; leg.add(shin);
    const sock = new THREE.Mesh(new THREE.CylinderGeometry(0.085, 0.085, 0.08, 8), mat(0xFFFFFF)); sock.position.y = -0.3; leg.add(sock);
    const shoe = new THREE.Mesh(new THREE.BoxGeometry(0.26, 0.17, 0.5), mat(shoeC)); shoe.position.set(0, -0.4, -0.08); leg.add(shoe);
    const sole = new THREE.Mesh(new THREE.BoxGeometry(0.27, 0.06, 0.52), mat(soleC)); sole.position.set(0, -0.49, -0.08); leg.add(sole);
    const stripe = new THREE.Mesh(new THREE.BoxGeometry(0.275, 0.04, 0.24), mat(soleC)); stripe.position.set(0, -0.39, -0.02); leg.add(stripe);
    body.add(leg); legs.push(leg);
  });
  const arms = [];
  [-0.43, 0.43].forEach(x => { const a = new THREE.Group(); a.position.set(x, 0.98, 0); const sl = new THREE.Mesh(new THREE.CapsuleGeometry(0.08, 0.1, 4, 8), mat(topC)); sl.position.y = -0.04; a.add(sl); const fa = new THREE.Mesh(new THREE.CapsuleGeometry(0.065, 0.14, 4, 8), skin); fa.position.y = -0.2; a.add(fa); body.add(a); arms.push(a); });
  const bub = new THREE.Mesh(new THREE.SphereGeometry(1.0, 18, 14), new THREE.MeshBasicMaterial({ color: 0x7FD8FF, transparent: true, opacity: 0.28, depthWrite: false })); bub.position.y = 0.85; bub.visible = false; g.add(bub); g.userData.bubble = bub;
  if (mine) { const ar = new THREE.Mesh(new THREE.ConeGeometry(0.22, 0.4, 4), new THREE.MeshBasicMaterial({ color: 0xFFD23F })); ar.rotation.x = Math.PI; ar.position.y = 2.3; g.add(ar); g.userData.arrow = ar; }
  Object.assign(g.userData, { body, legs, arms, phase: 0 });
  bakeGroup(body); legs.forEach(bakeGroup); arms.forEach(bakeGroup);
  return g;
}
// 한 덩어리의 단색 조각을 정점 색 모양 하나로 합친다(러너 16명 × 조각 40개 → 그리는 횟수를 크게 줄인다)
const RUN_MAT = new THREE.MeshLambertMaterial({ vertexColors: true });
function bakeGroup(group) {
  const parts = [];
  for (const c of [...group.children]) {
    if (!c.isMesh || Array.isArray(c.material) || !c.material.isMeshLambertMaterial || c.material.map || c.material.transparent || c.material.side !== THREE.FrontSide || c.children.length) continue;
    c.updateMatrix(); const g = (c.geometry.index ? c.geometry.toNonIndexed() : c.geometry.clone()); g.applyMatrix4(c.matrix);
    for (const k of Object.keys(g.attributes)) if (k !== 'position' && k !== 'normal') g.deleteAttribute(k);
    const col = c.material.color, n = g.attributes.position.count, arr = new Float32Array(n * 3); for (let i = 0; i < n; i++) { arr[i * 3] = col.r; arr[i * 3 + 1] = col.g; arr[i * 3 + 2] = col.b; }
    g.setAttribute('color', new THREE.BufferAttribute(arr, 3)); parts.push(g); group.remove(c);
  }
  if (parts.length) group.add(new THREE.Mesh(mergeGeometries(parts), RUN_MAT));
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
// skill: 0 ~ 1 — 궤적을 미리 보는 정도 · 조심성 · 반응 속도(에이스 셋은 진짜 잘 달린다)
const PERSONAS = [
  { key: 'ace', name: '에이스', speed: 1.0, wait: 0.7, sloppy: 0.04, skill: 0.95, color: 0x1166FF },
  { key: 'rush', name: '돌진왕', speed: 1.0, wait: 0, sloppy: 0.2, skill: 0.55, rush: 1, color: 0xFF4F8B },
  { key: 'bully', name: '방해꾼', speed: 0.95, wait: 0.5, sloppy: 0.12, skill: 0.72, bully: 1, color: 0xE11D48 },
  { key: 'block', name: '길막이', speed: 0.86, wait: 0.6, sloppy: 0.1, skill: 0.7, block: 1, color: 0xFF9F1C },
  { key: 'copy', name: '따라쟁이', speed: 0.93, wait: 0.5, sloppy: 0.12, skill: 0.76, copy: 1, color: 0xA77BFF },
  { key: 'shy', name: '겁쟁이', speed: 0.9, wait: 1, sloppy: 0.05, skill: 0.8, shy: 1, color: 0x14B8A6 },
  { key: 'goof', name: '엉뚱이', speed: 0.92, wait: 0.4, sloppy: 0.15, skill: 0.5, goof: 1, color: 0xFFC400 },
  { key: 'gamble', name: '도박사', speed: 0.97, wait: 0.2, sloppy: 0.18, skill: 0.5, gamble: 1, color: 0x3DDC84 }
];
const ROSTER = ['ace', 'rush', 'bully', 'copy', 'goof', 'gamble', 'shy', 'ace', 'block', 'bully', 'copy', 'goof', 'rush', 'gamble', 'ace'];

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
const HIT_WORD = { shove: ['퍽!', '#FF4F8B'], hammer: ['쾅!', '#FF9F1C'], punch: ['팡!', '#FFD23F'], sweep: ['휙!', '#24D8FF'], ball: ['쿵!', '#FF6B3D'], bumper: ['뿅!', '#A77BFF'], cannon: ['펑!', '#FF4F8B'], dive: ['퍽!', '#7CFFB2'], slip: ['미끌!', '#FFD23F'] };
function hitBody(b, k, kind) {
  if (!k || b.safe > 0 || b.shieldT > 0) {
    if (k && b.shieldT > 0 && seen(b)) { fx.burst(b.pos.x, b.pos.y + 0.9, b.pos.z, { n: 18, speed: 6, colors: [0x7FD8FF, 0xFFFFFF], life: 0.4, size: 0.5 }); fx.ring(b.pos.x, b.pos.y + 0.9, b.pos.z, { r1: 2.2, color: 0x7FD8FF, normal: [k.vx, 0, k.vz] }); }
    return;
  }
  if (b.onGround) b.airS = b.s;
  if (DEV) b.lastHit = kind + '@' + Math.round(b.s) + (b.lastShover ? ':' + (b.lastShover.persona ? b.lastShover.persona.key : 'me') : '');
  b.vel.x = k.vx; b.vel.z = k.vz; b.vel.y = Math.max(b.vel.y, k.vy); b.stun = k.stun; b.safe = 0.35; b.onGround = false; b.ground = null; b.diving = false;
  if (b === S.me) { sfx('hit'); fx.shake(kind === 'hammer' || kind === 'cannon' ? 0.9 : 0.6); }
  if (seen(b)) {
    const w = HIT_WORD[kind] || HIT_WORD.shove, x = b.pos.x, y = b.pos.y + 1, z = b.pos.z;
    fx.burst(x, y, z, { n: b === S.me ? 30 : 16, speed: 8, colors: [0xFFFFFF, 0xFFD23F, parseInt(w[1].slice(1), 16)], life: 0.45, size: 0.55 });
    fx.burst(x, y, z, { n: 6, speed: 5, colors: [0xFFD23F, 0xFFFFFF], life: 0.9, size: 0.45, shape: 2, grav: 4 });
    fx.ring(x, y, z, { r1: 2.6, color: parseInt(w[1].slice(1), 16), normal: [k.vx, 0.001, k.vz] });
    if (b === S.me || seen(b, 16) && Math.random() < 0.7) fx.word(x, y + 1.2, z, w[0], w[1], { big: b === S.me ? 1.1 : 0.8 });
    if (!b.isMe && Math.random() < 0.55) fx.emote(b, ['😵', '🤕', '😤', '🙀'][(Math.random() * 4) | 0]);
  }
}
// 화면에 보일 만한 곳에서만 효과(시뮬레이션 · 멀리 있는 AI 는 건너뛴다)
function seen(b, d = 30) { if (S.mode === 'sim' || S.mode === 'simdone' || !b) return false; const me = S.me; if (!me) return true; return Math.abs(b.pos.y - me.pos.y) < 14 && Math.hypot(b.pos.x - me.pos.x, b.pos.z - me.pos.z) < d; }
function seenAt(x, y, z, d = 34) { if (S.mode === 'sim' || S.mode === 'simdone') return false; const me = S.me; if (!me) return true; return Math.abs(y - me.pos.y) < 16 && Math.hypot(x - me.pos.x, z - me.pos.z) < d; }

function stepBody(b, dt) {
  const W = S.world, p = b.pos, v = b.vel;
  if (b.ground && b.ground.enabled) b.ground.carry(p);
  let ix = b.ix, iz = b.iz; const il = Math.hypot(ix, iz); if (il > 1) { ix /= il; iz /= il; }
  b.padT = Math.max(0, (b.padT || 0) - dt); b.slipT = Math.max(0, (b.slipT || 0) - dt);
  b.inSlime = false;
  if (b.onGround) {
    const bs = b.s, bu = b.u;
    for (const z of W.zones) {
      if (Math.abs(bs - z.s) > z.hs || Math.abs(bu - z.u) > z.hu || Math.abs(p.y - z.y) > 0.9) continue;
      if (z.type === 'slime') { const e = (bs - z.s) / z.hs, f = (bu - z.u) / z.hu; if (e * e + f * f <= 1) b.inSlime = true; }
      else if (z.type === 'boost') boostPad(b, z);
      else if (z.type === 'pad' && b.padT <= 0) launchPad(b, z);
    }
    if (b.ground && b.ground.belt) { const a = Math.atan2(p.z, p.x), bd = b.ground.belt * dt; p.x += -Math.sin(a) * bd; p.z += Math.cos(a) * bd; }
  }
  const sp = SPEED * b.speedK * (b.boostT > 0 ? 1.45 : 1) * (b.inSlime ? 0.45 : 1), ice = b.onGround && b.ground && (b.ground.ice || S.ev === 'ice');
  const ctl = b.diving ? 0.08 : b.stun > 0 ? 0.12 : ice ? 0.13 : 1;
  const acc = (b.onGround ? ACC_GROUND : ACC_AIR) * ctl;
  const tx = ix * sp, tz = iz * sp, dvx = tx - v.x, dvz = tz - v.z, dl = Math.hypot(dvx, dvz), lim = acc * dt;
  if (dl > lim) { v.x += dvx / dl * lim; v.z += dvz / dl * lim; } else { v.x = tx; v.z = tz; }
  if (b.onGround && b.ground && b.ground.kind === 'tilt') { const g = b.ground, sl = -Math.sin(g.tilt) * 11 * dt; p.x += Math.cos(g.yaw) * sl; p.z += Math.sin(g.yaw) * sl; }
  for (const w of W.winds) if (w.active(S.ht) && b.s >= w.s0 && b.s <= w.s1) { const a = Math.atan2(p.z, p.x); p.x += Math.cos(a) * w.force * dt; p.z += Math.sin(a) * w.force * dt; }
  b.coyote = b.onGround ? COYOTE : Math.max(0, b.coyote - dt);
  b.jumpBuf = Math.max(0, b.jumpBuf - dt);
  if (b.jumpBuf > 0 && b.coyote > 0 && b.stun <= 0) {
    const spring = b.springN > 0; v.y = JUMP_V * (spring ? 1.25 : 1) * (b.inSlime ? 0.78 : 1); if (spring) b.springN--; b.coyote = 0; b.jumpBuf = 0; b.onGround = false; b.ground = null; b.diveUsed = false;
    if (b === S.me) sfx('jump');
    if (seen(b, 18)) { fx.dust(p.x, p.y, p.z, { n: b === S.me ? 7 : 4, speed: 2 }); if (spring) { fx.ring(p.x, p.y + 0.05, p.z, { r1: 2, color: 0x7CFFB2 }); fx.burst(p.x, p.y + 0.2, p.z, { n: 12, speed: 4, colors: [0x7CFFB2, 0xFFFFFF], life: 0.4, size: 0.4 }); } }
  }
  else if (b.jumpBuf > 0 && !b.onGround && !b.diveUsed && b.stun <= 0 && b.coyote <= 0) {
    const f = [-Math.sin(b.yaw), -Math.cos(b.yaw)]; // 다이브: 공중에서 한 번 더 — 보는 쪽으로 몸을 날린다
    v.x = f[0] * (sp + DIVE_V); v.z = f[1] * (sp + DIVE_V); v.y = Math.max(v.y, 3); b.diveUsed = true; b.diving = true; b.jumpBuf = 0; if (b === S.me) sfx('dive');
    if (seen(b, 18)) { fx.burst(p.x, p.y + 0.6, p.z, { n: 10, speed: 3, colors: [0xFFFFFF, 0x7FD8FF], life: 0.35, size: 0.45, vx: -v.x * 0.3, vz: -v.z * 0.3 }); fx.word(p.x, p.y + 2, p.z, '슝!', '#7FD8FF', { big: 0.7 }); }
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
    b.landV = -v.y; p.y = bestTop; v.y = 0; b.onGround = true; b.ground = best; best.trigger();
    if (b.diving) { b.diving = false; b.stun = 0.3; }
    if (best.mystery) mystery(b);
    else if (!was) landed(b);
  } else { b.ground = null; if (was) b.airS = b.s; }
  for (const s of W.solids) {
    if (!s.enabled || s === best || !s.near(p.x, p.y, p.z)) continue;
    const top = s.topAt(p.x, p.z);
    if (p.y >= top - 0.02 || p.y + H <= s.bottom()) continue;
    const pushed = s.pushOut(p);
    if (pushed && s.pillar) slideOffPillar(b);
    if (pushed && s.move) {
      if (s.move.profile === 'punch' && Math.hypot(s.vx, s.vz) > 3 && (s.vx * s.cx + s.vz * s.cz) * Math.sign(s.u) < 0) { const sp2 = Math.hypot(s.vx, s.vz), k2 = Math.min(1, 10 / sp2); hitBody(b, { vx: s.vx * k2, vz: s.vz * k2, vy: 6, stun: 0.45 }, 'punch'); }
      else { v.x += (s.vx - v.x) * 0.35; v.z += (s.vz - v.z) * 0.35; }
    }
  }
  for (const bu of W.bumpers) {
    if (Math.abs(bu.y - p.y) > 3) continue; const n = bu.touch(b); if (!n) continue;
    if (b.safe <= 0 || bu.pulse < 0.2) { bu.pulse = 1; const k = { vx: n[0] * 9, vz: n[1] * 9, vy: 4, stun: 0.2 }; if (b.safe > 0) { v.x = k.vx; v.z = k.vz; } else hitBody(b, k, 'bumper'); if (b === S.me) sfx('boing'); }
  }
  if (b.onGround && W.bananas.length) for (let i = 0; i < W.bananas.length; i++) {
    const bn = W.bananas[i]; if (Math.abs(bn.y - p.y) > 0.7 || Math.hypot(bn.x - p.x, bn.z - p.z) > 0.6 || bn.owner === b && S.t - bn.t0 < 1.5) continue;
    W.bananas.splice(i, 1); slip(b); break;
  }
  if (b.safe <= 0) {
    for (const h of W.hazards) { const k = h.hit(b); if (k) { hitBody(b, k, h.kind); break; } }
    if (W.balls && b.safe <= 0) hitBody(b, W.balls.hit(b), 'ball');
  }
  const a = Math.atan2(p.z, p.x); let da = a - b.lastA; if (da > Math.PI) da -= TAU; if (da < -Math.PI) da += TAU; b.theta += da; b.lastA = a;
}
// 점프대: 위로 높이 + 앞으로 — 공을 넘거나 비탈을 건너뛴다
function launchPad(b, z) {
  const a = b.s / RT, t = [-Math.sin(a), Math.cos(a)], v = b.vel, vt = v.x * t[0] + v.z * t[1], fwd = Math.max(8.5, vt);
  const lx = v.x - t[0] * vt, lz = v.z - t[1] * vt;
  v.set(t[0] * fwd + lx * 0.4, 15.5, t[1] * fwd + lz * 0.4); b.onGround = false; b.ground = null; b.airS = b.s; b.padT = 0.6; b.coyote = 0; b.diveUsed = false; b.jumpBuf = 0; z.pulse = 1;
  if (b === S.me) { sfx('boing'); fx.shake(0.2); }
  if (seen(b)) { const p = b.pos; fx.ring(p.x, p.y + 0.1, p.z, { r1: 2.8, color: 0x7CFFB2 }); fx.burst(p.x, p.y + 0.3, p.z, { n: 22, speed: 6, up: 4, colors: [0x7CFFB2, 0xFFFFFF, 0x24D8FF], life: 0.5, size: 0.5 }); fx.word(p.x, p.y + 2.4, p.z, '슝~!', '#7CFFB2', { big: 0.9 }); }
}
// 가속 발판: 1.4초 부스터(밟는 동안 계속)
function boostPad(b, z) {
  const fresh = !(b.boostT > 0.25); b.boostT = Math.max(b.boostT || 0, 1.4); z.pulse = 1;
  if (fresh) { if (b === S.me) sfx('boost'); if (seen(b, 22)) { const p = b.pos; fx.burst(p.x, p.y + 0.4, p.z, { n: 16, speed: 5, colors: [0xFFD23F, 0x24D8FF, 0xFFFFFF], life: 0.4, size: 0.5 }); fx.word(p.x, p.y + 2.3, p.z, '부웅!', '#FFD23F', { big: 0.8 }); } }
}
// 바나나 껍질: 미끄러져 빙글
function slip(b) {
  const p = b.pos;
  if (seen(b)) fx.burst(p.x, p.y + 0.2, p.z, { n: 10, speed: 4, colors: [0xFFE066, 0xFFFFFF], life: 0.4, size: 0.45 });
  if (b.shieldT > 0) return;
  if (b.onGround) b.airS = b.s;
  b.vel.x *= 0.45; b.vel.z *= 0.45; b.vel.y = 3.2; b.stun = 0.6; b.safe = 0.3; b.slipT = 0.6; b.onGround = false; b.ground = null;
  if (b === S.me) { sfx('slip'); fx.shake(0.3); }
  if (seen(b)) { fx.word(p.x, p.y + 2.3, p.z, '미끌!', '#FFD23F', { big: b === S.me ? 1.1 : 0.8 }); if (!b.isMe) fx.emote(b, '🍌'); }
}
function dropBanana(b) {
  const W = S.world, a = b.s / RT, p = b.pos;
  if (W.bananas.length >= 14) W.bananas.shift();
  W.bananas.push({ x: p.x + Math.sin(a) * 0.9, y: p.y, z: p.z - Math.cos(a) * 0.9, t0: S.t, owner: b, rot: Math.random() * TAU });
  if (seen(b)) { fx.emote(b, '😜'); }
}
// 아래층에 내려앉으면 감긴 각도를 한 바퀴 되돌린다 — 그리고 혈압 문구
function landed(b) {
  while (b.theta > TAU && trackY(b.s) - b.pos.y > 4) b.theta -= TAU;
  const lost = b.airS - b.s;
  if (seen(b, 20) && (b.landV || 0) > 6) { const p = b.pos, big = Math.min(1.6, b.landV / 12); fx.dust(p.x, p.y, p.z, { n: Math.round(6 + 8 * big), speed: 2 + 3 * big, size: 0.5 + 0.3 * big }); if (b.landV > 13) { fx.ring(p.x, p.y + 0.06, p.z, { r1: 2.2 * big + 0.8, color: 0xFFFFFF, life: 0.35 }); if (b === S.me) fx.shake(0.25); } }
  if (!b.isMe && lost > 25 && seen(b)) fx.emote(b, ['😱', '😭', '🤯'][(Math.random() * 3) | 0]);
  if (b === S.me) { sfx('land'); if (lost > 25) { flash(`−${Math.round(lost)}m · ${TAUNTS[Math.random() * TAUNTS.length | 0]}`, 2.4); sfx('fall'); } }
  b.airS = b.s;
}
function mystery(b) { // ? 점프대: 55% 앞으로 멀리 · 45% 뒤로 튕김
  const a = b.s / RT, t = [-Math.sin(a), Math.cos(a)], fwd = Math.random() < 0.55;
  b.vel.set(t[0] * (fwd ? 11 : -10), fwd ? 16 : 14, t[1] * (fwd ? 11 : -10)); b.onGround = false; b.ground = null; b.airS = b.s; b.stun = 0.25;
  if (b === S.me) { sfx('boing'); flash(fwd ? '럭키! 🍀' : '아… 뒤로 😵'); }
  if (seen(b)) { const p = b.pos; fx.burst(p.x, p.y + 0.3, p.z, { n: 28, speed: 8, colors: [0xA77BFF, 0xFFFFFF, 0xFFD23F], life: 0.6, size: 0.6 }); fx.ring(p.x, p.y + 0.1, p.z, { r1: 3.2, color: 0xA77BFF }); fx.word(p.x, p.y + 2.2, p.z, fwd ? '럭키!' : '으악!', fwd ? '#7CFFB2' : '#FF4F8B'); }
}

function bodiesCollide() {
  const L = S.bodies.filter(b => !b.out && b.deadT <= 0 && !b.finished);
  for (let i = 0; i < L.length; i++) for (let j = i + 1; j < L.length; j++) {
    const a = L[i], b = L[j], dx = b.pos.x - a.pos.x, dz = b.pos.z - a.pos.z;
    if (Math.abs(dx) > 1 || Math.abs(dz) > 1 || Math.abs(a.pos.y - b.pos.y) > H) continue;
    const d = Math.hypot(dx, dz), need = R * 2; if (d >= need || d < 1e-4) continue;
    const narrow = x => x.ground && x.ground.kind === 'arc' && x.ground.r1 - x.ground.r0 < 5;
    if (!a.isMe && !b.isMe && (narrow(a) || narrow(b))) continue;
    // 나와 부딪히면 AI 가 비킨다(몸으로 밀려 떨어지지 않게 — 나를 미는 건 밀치기 · 다이브만)
    const ka = a.isMe ? 0 : b.isMe ? 1 : 0.5, kb = 1 - ka;
    const push = need - d, nx = dx / d, nz = dz / d;
    a.pos.x -= nx * push * ka; a.pos.z -= nz * push * ka; b.pos.x += nx * push * kb; b.pos.z += nz * push * kb;
    wallResolve(a); wallResolve(b);
    const rv = (b.vel.x - a.vel.x) * nx + (b.vel.z - a.vel.z) * nz;
    if (rv < 0) { a.vel.x += nx * rv * ka; a.vel.z += nz * rv * ka; b.vel.x -= nx * rv * kb; b.vel.z -= nz * rv * kb; }
    if (a.diving && b.safe <= 0) { hitBody(b, { vx: nx * 8, vz: nz * 8, vy: 4, stun: 0.35 }, 'dive'); b.lastShover = a; b.lastShoveAt = S.t; }
    if (b.diving && a.safe <= 0) { hitBody(a, { vx: -nx * 8, vz: -nz * 8, vy: 4, stun: 0.35 }, 'dive'); a.lastShover = b; a.lastShoveAt = S.t; }
  }
}
// 몸끼리 밀어낸 뒤 벽 · 기둥 안으로 들어갔으면 다시 밖으로(밀치기 뒤 끼임)
function wallResolve(b) {
  const p = b.pos;
  for (const s of S.world.solids) {
    if (!s.enabled || s === b.ground || !s.near(p.x, p.y, p.z)) continue;
    const top = s.topAt(p.x, p.z); if (p.y >= top - 0.02 || p.y + H <= s.bottom()) continue;
    if (s.pushOut(p) && s.pillar) slideOffPillar(b);
  }
}
function slideOffPillar(b) {
  const p = b.pos, v = b.vel, r = Math.hypot(p.x, p.z); if (r < 1e-3) return;
  const nx = p.x / r, nz = p.z / r, vn = v.x * nx + v.z * nz;
  if (vn < 0) { v.x -= nx * vn; v.z -= nz * vn; }
}
function pickItem(b, it) {
  it.on = false; it.t = 9;
  const I = ITEMS[(Math.random() * ITEMS.length) | 0];
  if (I.k === 'boost') b.boostT = I.sec;
  else if (I.k === 'spring') b.springN = 2;
  else if (I.k === 'shield') b.shieldT = I.sec;
  else if (I.k === 'zap') for (const o of S.bodies) { if (o === b || o.out || o.deadT > 0 || o.shieldT > 0) continue; if (Math.hypot(o.pos.x - b.pos.x, o.pos.z - b.pos.z) < 9 && Math.abs(o.pos.y - b.pos.y) < 3) { o.stun = Math.max(o.stun, 1.3); o.vel.set(0, 4, 0); o.onGround = false; o.ground = null; } }
  if (b === S.me) { sfx('item'); flash(`${I.icon} ${I.name}${I.k === 'spring' ? ' · 점프 2번 높이' : I.k === 'zap' ? ' · 주변 AI 기절!' : ` · ${I.sec}초`}`, 1.8); }
  if (seen(b)) { const p = b.pos; fx.burst(p.x, p.y + 1, p.z, { n: 26, speed: 7, colors: [0x24D8FF, 0xFFD23F, 0xFFFFFF], life: 0.6, size: 0.55 }); fx.burst(p.x, p.y + 1, p.z, { n: 8, speed: 4, colors: [0xFFD23F], life: 1, size: 0.5, shape: 2, grav: 3 }); fx.ring(p.x, p.y + 0.9, p.z, { r1: 3, color: 0x24D8FF }); fx.word(p.x, p.y + 2.4, p.z, I.icon + ' ' + I.name.split(' ')[0], '#24D8FF', { big: 0.75 }); }
  if (I.k === 'zap' && seen(b)) for (const o of S.bodies) if (o !== b && !o.out && o.deadT <= 0 && o.stun > 1 && Math.hypot(o.pos.x - b.pos.x, o.pos.z - b.pos.z) < 9) fx.bolt(b.pos.x, b.pos.y + 1.4, b.pos.z, o.pos.x, o.pos.y + 1, o.pos.z);
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
    hitBody(o, { vx: dx * k, vz: dz * k, vy: 4.5, stun: 0.4 }, 'shove'); o.lastShover = b; o.lastShoveAt = S.t; hit = true;
  }
  if (b === S.me) sfx(hit ? 'shove' : 'whiff');
  if (seen(b, 20)) { const p = b.pos; fx.burst(p.x + f[0] * 0.9, p.y + 0.9, p.z + f[1] * 0.9, { n: hit ? 4 : 6, speed: 2.5, colors: [0xFFFFFF], life: 0.3, size: 0.5, soft: true, vx: f[0] * 5, vz: f[1] * 5, grav: 0 }); if (hit && !b.isMe && Math.random() < 0.5) fx.emote(b, ['😎', '😈', '🤭'][(Math.random() * 3) | 0]); }
  return hit;
}

// ── AI ───────────────────────────────────────────────────────────────
function relS(x, z, s) { return (mod(Math.atan2(z, x) - mod(s / RT, TAU) + Math.PI, TAU) - Math.PI) * RT; }
// 미래 모습(움직이는 · 도는 발판, 함정 문): 지금 위치가 아니라 내가 내려앉을 때의 위치로 본다
function poseAt(sd, T) {
  const Q = { cx: sd.cx, cz: sd.cz, yaw: sd.yaw, en: sd.enabled };
  if (sd.move) { const f = sd.offset(T); Q.cx = sd.baseX + sd.move.dir[0] * f; Q.cz = sd.baseZ + sd.move.dir[1] * f; }
  if (sd.spin) Q.yaw = sd.baseYaw + sd.spin * T;
  if (sd.door) Q.en = mod(T / sd.door.period + (sd.door.phase || 0), 1) >= sd.door.open;
  if (sd.fall && sd.state !== 'idle') Q.en = false;
  return Q;
}
function insideAt(sd, Q, x, z, m) {
  if (sd.kind === 'arc') return sd.inside(x, z, m);
  if (sd.kind === 'disc') { const dx = x - Q.cx, dz = z - Q.cz; return dx * dx + dz * dz <= Math.max(0, sd.r + m) ** 2; }
  const [lx, lz] = sd.toLocal(x, z, Q); return Math.abs(lx) <= sd.hx + m && Math.abs(lz) <= sd.hz + m;
}
function topAtPose(sd, Q, x, z) { if (sd.kind === 'arc') return sd.topAt(x, z); if (sd.kind === 'tilt') { const [lx] = sd.toLocal(x, z, Q); return sd.top + Math.tan(sd.tilt) * Math.max(-sd.hx, Math.min(sd.hx, lx)); } return sd.top; }
// 이 점프(또는 다이브)로 어디에 내려앉나 — 공중 궤적을 미리 따라가 본다
function predictLanding(b, dx, dz, spd, vy0, m, careful) {
  const W = S.world, p = b.pos, g = -GRAV * (S.ev === 'lowg' ? 0.5 : 1), hk = S.ev === 'fast' ? 1.4 : 1, step = 1 / 30;
  const cand = []; for (const sd of W.solids) if (sd.enabled !== undefined && !sd.pillar && (sd.kind === 'arc' ? Math.abs(sd.y0 - p.y) < 6 || Math.abs(sd.y1 - p.y) < 6 : true) && (sd.near(p.x + dx * 2.5, p.y, p.z + dz * 2.5) || sd.near(p.x + dx * 5.5, p.y, p.z + dz * 5.5))) cand.push(sd);
  // 공중에서도 몸은 바라는 쪽으로 가속한다(멈췄다 뛰면 덜 간다) — 실제 몸놀림과 같은 셈
  let py = p.y, x = p.x, z = p.z, vx = careful === 'dive' ? dx * spd : b.vel.x, vz = careful === 'dive' ? dz * spd : b.vel.z; const tx = dx * spd, tz = dz * spd, lim = ACC_AIR * step;
  for (let tau = step; tau < 1.8; tau += step) {
    const ddx = tx - vx, ddz = tz - vz, dl = Math.hypot(ddx, ddz); if (dl > lim) { vx += ddx / dl * lim; vz += ddz / dl * lim; } else { vx = tx; vz = tz; }
    x += vx * step; z += vz * step; const ny = p.y + vy0 * tau - 0.5 * g * tau * tau;
    if (vy0 - g * tau < 0) {
      for (const sd of cand) {
        const Q = poseAt(sd, S.ht + tau * hk); if (!Q.en || careful && sd.fake) continue;
        if (!insideAt(sd, Q, x, z, m)) continue;
        const top = topAtPose(sd, Q, x, z);
        if (top <= py + 0.05 && top >= ny - 0.05) return { ok: top > p.y - 2.2, top, tau, sd, x, z };
      }
    }
    py = ny; if (ny < p.y - 3) return null;
  }
  return null;
}
// 이 줄(u)로 갔을 때 발밑이 이어지나(가짜 · 흔들리는 타일은 조심성 있는 AI 에게 구멍)
function laneFloor(near, s, u, y, careful) {
  const q = P(s, u, 0);
  for (const sd of near) {
    if (!sd.enabled || careful && sd.fake || sd.fall && sd.state !== 'idle') continue;
    if (!sd.inside(q.x, q.z, -0.2)) continue; const t = sd.topAt(q.x, q.z); if (t <= y + 0.7 && t >= y - 0.9) return true;
  }
  return false;
}
// 줄 고르기: 바닥 · 장애물 · 아이템 · 가속 발판 · 바나나 · 공 · 대포를 따져 가장 좋은 줄
function planLane(b) {
  const W = S.world, p = b.pos, s = b.s, u = b.u, P0 = b.persona, rnd = b.rnd, sk = P0.skill, careful = b.patient || sk > 0.85;
  const near = W.solids.filter(sd => !sd.pillar && sd.near(p.x, p.y, p.z) || sd.kind === 'arc' && s + 6 >= sd.s0 && s - 1 <= sd.s0 + sd.len && Math.abs(Math.min(sd.y0, sd.y1) - p.y) < 3);
  const spd = SPEED * b.speedK;
  let best = b.planU != null ? b.planU : u, bestC = Infinity, curC = Infinity;
  const windOn = W.winds.some(w => s >= w.s0 - 4 && s <= w.s1 && (w.active(S.ht) || w.warn(S.ht)));
  for (let uc = -HW + 0.8; uc <= HW - 1.4; uc += 0.6) {
    let c = 0.22 * Math.abs(uc - u) + 0.06 * Math.abs(uc - (b.lane || 0));
    for (const ds of [1.4, 2.8, 4.4]) if (!laneFloor(near, s + ds, uc, p.y, careful)) c += 4.2 / (ds * 0.6);
    if (uc > HW - 2) c += 1.2; if (windOn && uc > -2) c += 1 + (uc + 2) * 0.9;
    for (const bu of W.bumpers) { const ds = bu.s - s; if (ds > -0.5 && ds < 5 && Math.abs(bu.u - uc) < 1.7 && Math.abs(bu.y - p.y) < 2) c += 3; }
    for (const z of W.zones) { const ds = z.s - s; if (ds < -z.hs || ds > 6 || Math.abs(z.y - p.y) > 2) continue; const inU = Math.abs(z.u - uc) < z.hu + 0.3; if (!inU) continue;
      if (z.type === 'slime') c += 1 + 2 * sk; else if (z.type === 'boost') c -= 1.6; else if (z.type === 'pad') c -= sk > 0.8 || P0.gamble || P0.rush ? 1.4 : 0.3; }
    for (const bn of W.bananas) { const ds = relS(bn.x, bn.z, s); if (ds > -0.3 && ds < 5 && Math.abs(Math.hypot(bn.x, bn.z) - RT - uc) < 0.9 && Math.abs(bn.y - p.y) < 1) c += 2 + 2 * sk; }
    for (const it of W.items) if (it.on && it.s - s > 0.8 && it.s - s < 8 && Math.abs(it.y - p.y) < 1.5 && Math.abs(it.u - uc) < 0.9) c -= 1.3;
    if (W.balls) for (const bl of W.balls.list) { const ds = bl.s - s; if (ds > -1.5 && ds < 10 && Math.abs(bl.u - uc) < bl.r + 0.9) c += 5; }
    for (const cn of W.cannons) { const ds = cn.s - s; if (ds < -0.6 || ds > 5 || Math.abs(cn.y - p.y) > 2) continue; const tau = Math.max(0, ds) / spd; for (const sh of cn.shots) { const su = sh.u - cn.side * cn.speed * tau; if (Math.abs(su - uc) < 1.4) c += 3; } if (cn.warn && ds < 2.5 && Math.abs(cn.u0 - uc) < 4) c += 1; }
    for (const sd of near) if (sd.punch) { const ds = relS(sd.cx, sd.cz, s); if (ds > -1 && ds < 5.5 && Math.sign(sd.u) === Math.sign(uc) && Math.abs(uc) > 0.8) c += 3; }
    for (const o of S.bodies) if (o !== b && !o.out && o.deadT <= 0) { const ds = o.s - s; if (ds > 0.3 && ds < 2.6 && Math.abs(o.u - uc) < 0.9 && Math.abs(o.pos.y - p.y) < 1) c += 0.7; }
    c += (rnd() - 0.5) * P0.sloppy * (1.4 - sk) * 2;
    if (Math.abs(uc - best) < 0.31) curC = c;
    if (c < bestC) { bestC = c; b._bu = uc; }
  }
  return curC - bestC < 0.35 ? best : b._bu;
}
function aiThink(b, dt) {
  const W = S.world, p = b.pos, s = b.s, u = b.u, P0 = b.persona, rnd = b.rnd, sk = P0.skill;
  const a = s / RT, t = [-Math.sin(a), Math.cos(a)], n = [Math.cos(a), Math.sin(a)];
  if (b.stun > 0) { b.ix = 0; b.iz = 0; return; }
  // 엉뚱이 · 도박사 · 돌진왕의 이상한 짓
  b.goofT = Math.max(0, (b.goofT || 0) - dt);
  if (P0.goof && !b.goofT && rnd() < dt * 0.14) { b.goofT = 0.6 + rnd() * 0.8; b.goofKind = ['stop', 'back', 'hop', 'spin', 'dance'][rnd() * 5 | 0]; if (seen(b, 20) && rnd() < 0.6) fx.emote(b, ['🤪', '💃', '🙃', '🎵'][rnd() * 4 | 0]); }
  if (b.goofT > 0 && b.onGround && !onNarrow(b)) {
    if (b.goofKind === 'stop') { b.ix = b.iz = 0; return; }
    if (b.goofKind === 'back') { b.ix = -t[0] * 0.6; b.iz = -t[1] * 0.6; b.yaw = Math.atan2(-b.ix, -b.iz); return; }
    if (b.goofKind === 'spin' || b.goofKind === 'dance') { const w = S.t * 9; b.ix = Math.cos(w) * 0.4 + t[0] * 0.5; b.iz = Math.sin(w) * 0.4 + t[1] * 0.5; if (b.goofKind === 'dance' && b.onGround && rnd() < dt * 4) b.jumpBuf = JUMP_BUF; return; }
    if (b.goofKind === 'hop' && b.onGround) b.jumpBuf = JUMP_BUF;
  }
  if ((P0.goof || P0.bully) && b.onGround && b.ground && b.ground.kind === 'arc' && b.ground.r1 - b.ground.r0 >= 10 && !b.ground.belt && rnd() < dt / (P0.goof ? 8 : 16)) dropBanana(b);
  // 줄 고르기(성격 · 실력에 따라 자주 · 드물게)
  b.planT = (b.planT || 0) - dt;
  if (b.planT <= 0) { b.planT = 0.07 + (1 - sk) * 0.18 + rnd() * 0.05; b.planU = planLane(b); }
  let ut = b.planU != null ? b.planU : (b.lane || 0), go = 1;
  // 다음 발판(좁은 다리 · 움직이는 발판 · 원판)에 줄 맞추기 — 내려앉을 때의 위치로
  let next = null;
  for (const sd of W.solids) {
    if (!sd.enabled || sd.pillar || !sd.near(p.x, p.y, p.z) && sd.kind !== 'arc') continue;
    if (sd.kind === 'arc') { if (s + 3.4 >= sd.s0 && s + 3.4 <= sd.s0 + sd.len && Math.abs(sd.y0 - p.y) < 3 && sd.r1 - sd.r0 < 13) next = { u: (sd.r0 + sd.r1) / 2 - RT, half: Math.max(0.2, (sd.r1 - sd.r0) / 2 - 0.7) }; continue; }
    if (sd.fall || sd.mystery && !P0.gamble || sd === b.ground) continue;
    const top = sd.topAt(sd.cx, sd.cz); if (top > p.y + 0.7 || top < p.y - 2.5) continue;
    const Q = poseAt(sd, S.ht + 0.55), dsd = relS(Q.cx, Q.cz, s); if (dsd < 0.5 || dsd > 8.5) continue;
    if (!next || next.ds == null || dsd < next.ds) next = { u: Math.hypot(Q.cx, Q.cz) - RT, half: sd.kind === 'disc' ? sd.r * 0.4 : sd.hx * 0.5, ds: dsd };
  }
  if (next) { ut = next.u + Math.max(-next.half, Math.min(next.half, ut - next.u)); if (next.ds == null && Math.abs(u - next.u) > next.half + 0.4) go = Math.max(0.25, 1 - (Math.abs(u - next.u) - next.half) / 2.5); }
  ut = holeDodge(b, s, u, ut, p);
  // 기다릴 때: 밀어내는 벽 · 펀치 · 망치 · 함정 문 · 대포
  for (const sd of W.solids) {
    if (!sd.move || sd.kind !== 'box' || !sd.near(p.x, p.y, p.z) || sd.top <= p.y + 0.7) continue;
    const dsd = relS(sd.cx, sd.cz, s); if (dsd < -1 || dsd > 5.5) continue;
    const su = Math.hypot(sd.cx, sd.cz) - RT;
    if (sd.punch) { if (sd.flash && dsd < 3.2 && Math.sign(sd.u) === Math.sign(u) && b.patient) go = 0; continue; }
    ut = -5;
    const edge = su - sd.hx + (sd.vx * n[0] + sd.vz * n[1]) * 0.3;
    if (dsd < 2.2 && dsd > 0 && edge < u + 0.7 && (b.patient || rnd() < 0.5)) go = 0;
  }
  for (const h of W.hazards) if (h instanceof Hammer) {
    const ds = relS(h.x, h.z, s), rel = Math.abs(h.lat - u), away = Math.sign(h.v) === Math.sign(h.lat - u);
    const win = away && rel > 1.7; // 막 지나 멀어지는 동안(끝까지 갔다 돌아오기 전) — 이때 건넌다
    if (ds > 2.6 && ds < 6) { if (!win) go = Math.min(go, 0.45); }
    else if (ds >= 1.9 && ds <= 2.6 && !win) go = b.patient || sk >= 0.7 ? 0 : Math.min(go, 0.25);
  }
  for (const cn of W.cannons) { const ds = cn.s - s; if (ds > 0.4 && ds < 2.4 && Math.abs(cn.y - p.y) < 1.5 && sk > 0.6) { for (const sh of cn.shots) { const eta = (sh.u - u) * -cn.side / cn.speed; if (eta > -0.1 && eta < 0.55) go = 0; } if (cn.warn && Math.abs(cn.u0 - u) < 5) go = Math.min(go, 0.5); } }
  if (W.door && (W.door.enabled === false || W.door.warn)) { const ds = W.door.s0 - s; if (ds > 0 && ds < 3.5 && b.patient && sk < 0.85) go = 0; }
  // 나와 어울리기: 방해꾼은 옆에서 밀고, 따라쟁이는 따라 뛰고, 다른 AI 는 카메라를 가리지 않는다
  const me = S.me;
  if (me && !me.out && me.deadT <= 0 && !me.finished) {
    const dme = Math.hypot(me.pos.x - p.x, me.pos.z - p.z), tight = next && next.half < 2.2 || onNarrow(b) || onNarrow(me);
    if (P0.bully && dme < 5 && Math.abs(me.pos.y - p.y) < 1.2 && !tight) {
      // 바깥쪽에서 기둥 쪽으로 민다(물로 밀지는 않는다 — 귀찮지만 억울하지 않게)
      ut = me.u + 1.1; const ahead = me.s - s, qa = P(me.s + 2.5, me.u, 0), gAhead = groundTop(qa.x, qa.z, me.pos.y + 0.5);
      if (ahead > -1.5 && ahead < 4) { go = ahead > 0.6 ? 1 : 0.7; if (dme < SHOVE_RANGE - 0.1 && Math.abs(ahead) < 1.2 && u > me.u + 0.3 && me.u < HW - 2.6 && gAhead > me.pos.y - 0.6 && rnd() < dt * 3) { b.yaw = Math.atan2(-(me.pos.x - p.x), -(me.pos.z - p.z)); shove(b); return; } }
    }
    if (P0.copy && dme < 6 && me.s > s && !tight) { const side = b.side || (b.side = u >= me.u ? 1 : -1); ut = me.u + side * 1.6; if (me.s - s < 2.2) go = Math.min(go, 0.6); if (!me.onGround && b.onGround && me.vel.y > 5 && !b.ground.fall) b.jumpBuf = JUMP_BUF; }
    else if (!P0.bully && !tight && dme < 2.2 && me.s > s && Math.abs(me.u - u) < 1.0) ut = me.u + (u >= me.u ? 1.4 : -1.4);
    if (dme < 3 && me.s < s && !b.passedMe && seen(b, 10)) { b.passedMe = true; if (rnd() < 0.5) fx.emote(b, ['😜', '👋', '🏃', '😎'][rnd() * 4 | 0]); }
    if (me.s > s + 4) b.passedMe = false;
  }
  // AI 끼리도 민다(방해꾼 · 돌진왕 — 좁은 다리 위는 빼고)
  if ((P0.bully || P0.rush) && b.shoveCd <= 0 && b.onGround && !onNarrow(b) && rnd() < dt * (P0.bully ? 1.2 : 0.7)) {
    for (const o of S.bodies) { if (o === b || o.isMe || o.out || o.deadT > 0 || o.finished || onNarrow(o)) continue; const dx = o.pos.x - p.x, dz = o.pos.z - p.z, d = Math.hypot(dx, dz); if (d < SHOVE_RANGE - 0.2 && Math.abs(o.pos.y - p.y) < 1 && (dx * t[0] + dz * t[1]) > -0.3) { b.yaw = Math.atan2(-dx, -dz); shove(b); return; } }
  }
  if (P0.block && next && next.half < 2) ut = next.u + Math.sin(S.t * 1.7 + b.seed) * 1.0;
  if (P0.shy) { if (go === 0) b.shyT = 0.5; else if (b.shyT > 0) { b.shyT -= dt; if (b.shyT > 0.38) go = 0; } }
  // 바람: 불거나 불기 직전이면 안쪽(기둥 쪽)으로 먼저
  for (const w of W.winds) if (s >= w.s0 - 3 && s <= w.s1 && (w.active(S.ht) || w.warn(S.ht))) { ut = Math.min(ut, -3); if (u > 0.5) go = Math.min(go, 0.55); }
  // 오래 제자리면(8초) 잠깐 기다림 무시하고 간다
  if (b.maxS > (b.progS || 0) + 1) { b.progS = b.maxS; b.progT = S.t; } else if (S.t - (b.progT || 0) > 8 && b.onGround) { b.frustT = 1.2; b.progT = S.t; if (seen(b, 14)) fx.emote(b, '😤'); }
  if ((b.frustT = Math.max(0, (b.frustT || 0) - dt)) > 0) go = 1;
  ut = Math.max(-HW + 0.8, Math.min(HW - 1.4, ut));
  // 공중: 내려앉을 곳 쪽으로 몸을 틀고, 모자라면 다이브
  if (!b.onGround) { airThink(b, ut, t, n); return; }
  const lat = Math.max(-1, Math.min(1, (ut - u) * 0.9));
  let ix = t[0] * go + n[0] * lat, iz = t[1] * go + n[1] * lat;
  // 땅: 앞이 끊기면(틈 · 발판 끝 · 열린 문) 미리 궤적을 보고 뛴다 — 안 닿으면 기다렸다가
  b.edgeWait = b.edgeWait || 0;
  if (go > 0) {
    const il = Math.hypot(ix, iz) || 1, dx = ix / il, dz = iz / il, spd = SPEED * b.speedK * (b.boostT > 0 ? 1.45 : 1) * (b.inSlime ? 0.45 : 1) * Math.min(1, il);
    const look = 0.42 + Math.min(1, spd / SPEED) * 0.3, q = { x: p.x + dx * look, z: p.z + dz * look };
    const gEdge = groundTop(q.x, q.z, p.y + 0.5), q2 = P(s + 0.9, u, 0), gStep = groundTop(q2.x, q2.z, p.y + 1.2);
    const qf = { x: p.x + dx * (look + 0.55), z: p.z + dz * (look + 0.55) }, gFar = groundTop(qf.x, qf.z, p.y + 0.5);
    const crack = gFar > p.y - 0.6 && gFar < p.y + 0.5;
    const atEdge = (gEdge === -Infinity || gEdge < p.y - 0.6) && !crack;
    let jump = gStep > p.y + 0.35;
    if (atEdge) {
      // 궤적 계산은 0.05초마다(기다리는 동안 매 틱 다시 하지 않는다)
      let L = b.edgeL;
      if (!(b.edgeEvalT > S.t)) {
        const smart = sk > 0.35 && rnd() < 0.5 + sk * 0.6;
        const vy0 = JUMP_V * (b.springN > 0 ? 1.25 : 1) * (b.inSlime ? 0.78 : 1);
        L = b.edgeL = smart ? predictLanding(b, dx, dz, Math.max(spd, SPEED * b.speedK * 0.9), vy0, -0.3 * sk, b.patient) : { ok: true };
        b.edgeEvalT = S.t + 0.05;
      }
      if (L && L.ok && L.sd && L.sd.kind === 'disc' && sk >= 0.6) {
        let bad = false;
        for (const h of W.hazards) if (h instanceof Sweeper && Math.hypot(h.x - L.sd.cx, h.z - L.sd.cz) < 0.6) for (const br of h.bars) {
          const T = S.ht + L.tau * (S.ev === 'fast' ? 1.4 : 1), aT = br.spin * T + (br.phase || 0), phi = Math.atan2(L.z - h.z, L.x - h.x), rs = br.spin - L.sd.spin;
          if ((rs > 0 ? mod(phi - aT, Math.PI) : mod(aT - phi, Math.PI)) / Math.max(0.05, Math.abs(rs)) < 0.5) bad = true;
        }
        if (bad) L = null;
      }
      if (L && L.ok) { jump = true; b.edgeWait = 0; b.aim = L.sd || null; b.aimT = S.ht + (L.tau || 0.7) * (S.ev === 'fast' ? 1.4 : 1); }
      else {
        b.edgeWait += dt;
        const limit = b.ground && b.ground.fall ? 0 : 1.2 + sk * 4 + (b.patient ? 1.5 : 0);
        if (b.edgeWait > limit) { jump = true; b.edgeWait = 0; if (seen(b, 16)) fx.emote(b, '😬'); }
        else { ix = n[0] * lat * 0.6; iz = n[1] * lat * 0.6; if (b.edgeWait > 1.6 && !b.waitEmo && seen(b, 14)) { b.waitEmo = true; fx.emote(b, ['😰', '🤔', '😤'][rnd() * 3 | 0]); } }
      }
    } else b.edgeWait = Math.max(0, b.edgeWait - dt * 2), b.waitEmo = false;
    // 도는 막대 · 굴러오는 공 · 대포알은 닿기 직전에 넘는다
    for (const h of W.hazards) if (h instanceof Sweeper && Math.abs(h.base - p.y) < 0.8) {
      const ddx = p.x - h.x, ddz = p.z - h.z, dist = Math.hypot(ddx, ddz), phi = Math.atan2(ddz, ddx);
      for (const br of h.bars) {
        if (dist > br.len + 0.6 || dist < 0.9) continue;
        const gs = b.ground && b.ground.spin && Math.hypot(b.ground.cx - h.x, b.ground.cz - h.z) < 0.5 ? b.ground.spin : 0;
        const rs = (br.spin - gs) * (S.ev === 'fast' ? 1.4 : 1); if (Math.abs(rs) < 0.05) continue;
        const tt = (rs > 0 ? mod(phi - br.a, Math.PI) : mod(br.a - phi, Math.PI)) / Math.abs(rs);
        const err = (rnd() - 0.5) * P0.sloppy * (1.3 - sk) * 0.6, hi = br.y0 > 0.9;
        if (tt > (hi ? 0.25 : 0.15) + err && tt < (hi ? 0.38 : 0.28) + err) jump = true;
      }
    }
    if (W.balls) for (const bl of W.balls.list) { const ds = bl.s - s; if (Math.abs(bl.u - u) < bl.r + 0.6 && ds > 0 && ds / (bl.v + spd) < 0.32 + (1 - sk) * 0.1 && rnd() < sk) jump = true; }
    for (const cn of W.cannons) { const ds = cn.s - s; if (Math.abs(ds) < 1.4 && Math.abs(cn.y - p.y) < 1.5) for (const sh of cn.shots) { const eta = (sh.u - u) * -cn.side / cn.speed; if (eta > 0.05 && eta < 0.3 && rnd() < 0.3 + sk) jump = true; } }
    if (jump && b.stun <= 0) b.jumpBuf = JUMP_BUF;
    if (P0.gamble && rnd() < dt * 0.03) b.jumpBuf = JUMP_BUF;
    if (P0.rush && !b.diveUsed && rnd() < dt * 0.06 && !atEdge && !onNarrow(b)) b.jumpBuf = JUMP_BUF; // 괜히 점프
  }
  b.ix = ix; b.iz = iz;
  b.yaw = Math.atan2(-ix, -iz);
}
function onNarrow(b) { return b.ground && b.ground.kind === 'arc' && b.ground.r1 - b.ground.r0 < 5; }
function airThink(b, ut, t, n) {
  const p = b.pos, sk = b.persona.skill, v = b.vel;
  let tx = null, tz = null;
  if (b.aim && b.aim.enabled !== false) { const Q = poseAt(b.aim, Math.max(S.ht, b.aimT || S.ht)); if (b.aim.kind !== 'arc') { tx = Q.cx; tz = Q.cz; } }
  const fwd = v.x * t[0] + v.z * t[1];
  if (tx != null && sk > 0.4) {
    const dx = tx - p.x, dz = tz - p.z, lat = dx * n[0] + dz * n[1];
    b.ix = t[0] * 1 + n[0] * Math.max(-1, Math.min(1, lat * 0.6)); b.iz = t[1] * 1 + n[1] * Math.max(-1, Math.min(1, lat * 0.6));
  } else { const lat = Math.max(-1, Math.min(1, (ut - b.u) * 0.7)); b.ix = t[0] + n[0] * lat; b.iz = t[1] + n[1] * lat; }
  b.yaw = Math.atan2(-b.ix, -b.iz);
  // 떨어지는데 발밑이 없으면: 다이브로 닿는지 본다
  if (!b.diveUsed && v.y < 1.5 && b.stun <= 0 && sk > 0.3 && b.rnd() < 0.3 + sk) {
    const il = Math.hypot(b.ix, b.iz) || 1, dx = b.ix / il, dz = b.iz / il, sp = SPEED * b.speedK;
    const now = predictLanding(b, v.x / (Math.hypot(v.x, v.z) || 1), v.z / (Math.hypot(v.x, v.z) || 1), Math.hypot(v.x, v.z), v.y, -0.1, false);
    if (!now || !now.ok) { const d = predictLanding(b, dx, dz, sp + DIVE_V, Math.max(v.y, 3), -0.6, 'dive'); if (d && d.ok && d.sd.kind === 'arc' && d.sd.r1 - d.sd.r0 >= 8 && Math.hypot(d.x, d.z) < d.sd.r1 - 1.5) { b.jumpBuf = JUMP_BUF; b.aim = d.sd; } }
  }
}
// 무너진 타일 구멍(과 눈 밝은 AI 에게는 금 간 타일)을 피해 옆 칸으로
function holeDodge(b, s, u, ut, p) {
  const careful = b.patient || b.persona.skill > 0.85;
  const ok = uu => { const q = P(s + 1.8, uu, 0); for (const sd of S.world.solids) { if (!sd.enabled || sd.pillar || !sd.near(q.x, p.y, q.z) || !sd.inside(q.x, q.z, 0)) continue; const t = sd.topAt(q.x, q.z); if (Math.abs(t - p.y) < 1.2 && !(careful && sd.fake) && sd.state !== 'shake') return true; } return false; };
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
  S.me = new Body({ name: '나', isMe: true }); S.me.place(slots[meSlot][0], slots[meSlot][1]); S.me.mesh = makeRunner(myLook(), true, 1); S.bodies.push(S.me);
  let k = 0;
  slots.forEach((sl, i) => {
    if (i === meSlot) return;
    const P0 = PERSONAS.find(q => q.key === ROSTER[k % ROSTER.length]); k++;
    const b = new Body({ lives: 3, persona: P0, name: P0.name, rnd: mulberry((rnd() * 1e9) | 0), seed: rnd() * 6, lane: (rnd() * 2 - 1) * 2.5, patient: P0.skill >= 0.9 || rnd() < 0.6 + P0.wait * 0.4, speedK: Math.min(0.97, P0.speed * (0.93 + rnd() * 0.09)) }); // 나(1.0)보다 빠른 AI 는 없다
    b.place(sl[0], sl[1]); b.mesh = makeRunner(randomLook(b.rnd, P0.color), false, 2 + (rnd() * 98 | 0)); b.tag = nameTag(P0.name, '#' + P0.color.toString(16).padStart(6, '0')); b.mesh.add(b.tag);
    S.bodies.push(b);
  });
  for (const b of S.bodies) { scene.add(b.mesh); runnerMeshes.push(b.mesh); b.maxS = b.s; }
}
function deathCheck(b) {
  if (b.out || b.deadT > 0 || b.finished || b.onGround) return;
  const ty = trackY(Math.min(b.s, S.world.endS));
  if (b.pos.y > WATER_Y && (b.pos.y > ty - DEATH_DROP || groundTop(b.pos.x, b.pos.z, b.pos.y) > -Infinity)) return;
  const ko = b !== S.me && b.lastShover === S.me && S.t - b.lastShoveAt < 3;
  if (ko) { S.ko++; flash('밀어서 떨어뜨림 +50'); }
  b.lives--;
  if (b === S.me) sfx('splash');
  if (seen(b, 60)) { fx.splash(b.pos.x, -23.6, b.pos.z, b === S.me ? 1.3 : 0.8); if (ko) fx.word(b.pos.x, Math.max(b.pos.y, S.me.pos.y - 2) + 2, b.pos.z, 'KO! +50', '#FFD23F', { big: 1.2, life: 1.3 }); if (b === S.me) fx.shake(0.5); }
  if (b.lives <= 0) { b.out = true; b.mesh.visible = false; if (b === S.me) endRun(false, 'dead'); return; }
  b.deadT = 1.2; b.mesh.visible = false;
  if (b === S.me) { S.revived = true; flash('풍덩! 부활 — 마지막 목숨이에요', 2); }
}
function reviveTick(b, dt) {
  b.deadT -= dt;
  if (b.deadT <= 0) { const sec = S.world.secs[b.sec]; b.place(sec.s + 2.5, b === S.me ? 0 : (b.lane || 0) * 0.5); b.mesh.visible = true; if (seen(b)) { const p = b.pos; fx.burst(p.x, p.y + 0.5, p.z, { n: 30, speed: 3, up: 6, colors: [0x7CFFB2, 0xFFFFFF, 0xFFD23F], life: 0.9, size: 0.5, grav: -2 }); fx.ring(p.x, p.y + 0.05, p.z, { r1: 2.4, color: 0x7CFFB2 }); } }
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
  if (W.bananas.length && S.t - W.bananas[0].t0 > 35) W.bananas.shift();
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
      b.finished = true; S.finishers++; b.rank = S.finishers; if (!b.isMe && seen(b, 40)) fx.emote(b, ['🎉', '🏆', '😎', '🥳'][(Math.random() * 4) | 0]);
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
  if (k < S.world.secs.length) { banner(`<small>STAGE ${k + 1}</small><b>${S.world.secs[k].name}</b>`, 'good', 1.6); celebrateGate(S.world.secs[k].s, S.world.secs[k].y, 1); }
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

const fx = createFx(scene);
// 구간 문 · 결승: 양쪽 색종이 대포 + 폭죽 + 충격파
function celebrateGate(gs, gy, big, quiet) {
  const a = gs / RT, t = [-Math.sin(a), Math.cos(a)], n = [Math.cos(a), Math.sin(a)];
  for (const side of [-1, 1]) { const q = P(gs, side * (HW + 0.4), gy); fx.cannon(q.x, gy + 7.3, q.z, -n[0] * side * 0.6 + t[0] * 0.5, -n[1] * side * 0.6 + t[1] * 0.5, Math.round(45 * big)); }
  const q = P(gs, 0, gy); fx.ring(q.x, gy + 6.3, q.z, { r1: 10 * big, color: 0xFFD23F, life: 0.6, normal: [t[0], 0, t[1]] });
  for (let i = 0; i < Math.round(3 * big); i++) { const r = P(gs + 6 + Math.random() * 10, (Math.random() * 2 - 1) * 12 + 10, 0); fx.firework(r.x, gy + 2, r.z, { delay: i * 0.22, big: 0.8 + 0.3 * big }); }
  if (S.me && !quiet) fx.word(S.me.pos.x, S.me.pos.y + 2.8, S.me.pos.z, big > 1.5 ? '정상!' : '통과!', '#FFD23F', { big: 1.4, life: 1.2 });
}
function confetti(at, n) { fx.cannon(at.x, at.y + 1, at.z, 0, 0, n); }
function confettiTick(dt) { fx.tick(dt, camera); }

const ui = { title: $('title'), hud: $('hud'), stage: $('hudStage'), score: $('hudScore'), time: $('hudTime'), pos: $('hudPos'), lives: $('hudLives'), banner: $('banner'), over: $('over'), board: $('board'), toast: $('toast'), controls: $('controls'), join: $('join'), sns: $('sns'), wind: $('hudWind'), fx: $('hudFx'), look: $('look') };
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
  sfx(finished ? 'win' : 'out'); if (finished) { celebrateGate(S.world.finishS, S.world.finishY, 2.2); S.showT = 6; }
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

// 내 러너 꾸미기 — 이 기기에 저장(tower_look). AI 는 판마다 랜덤
function myLook() {
  let L = null; try { L = JSON.parse(localStorage.getItem('tower_look') || 'null'); } catch (e) { L = null; }
  const out = Object.assign({}, DEFAULT_LOOK);
  if (L && typeof L === 'object') for (const k of LOOK_KEYS) if (Number.isInteger(L[k]) && L[k] >= 0 && L[k] < LOOK[k].list.length) out[k] = L[k];
  return out;
}
let lookDraft = null, lookR = null, lookScene = null, lookCam = null, lookMesh = null, lookOn = false;
function lookRender() {
  if (!lookR) {
    lookR = new THREE.WebGLRenderer({ canvas: $('lookCanvas'), antialias: true, alpha: true }); lookR.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2)); lookR.setSize(220, 220, false); lookR.outputColorSpace = THREE.SRGBColorSpace;
    lookScene = new THREE.Scene(); lookScene.add(new THREE.HemisphereLight(0xEAF4FF, 0x4A5A7B, 1.9)); const d = new THREE.DirectionalLight(0xffffff, 1.4); d.position.set(2, 4, -3); lookScene.add(d);
    lookCam = new THREE.PerspectiveCamera(32, 1, 0.1, 50); lookCam.position.set(0, 1.3, -4.4); lookCam.lookAt(0, 0.95, 0);
  }
  if (lookMesh) { lookScene.remove(lookMesh); lookMesh.traverse(o => { if (o.geometry) o.geometry.dispose(); }); }
  lookMesh = makeRunner(lookDraft, false, 1); lookMesh.userData.bubble.visible = false; lookScene.add(lookMesh);
}
function lookRows() {
  $('lookRows').innerHTML = LOOK_KEYS.map(k => {
    const v = LOOK[k].list[lookDraft[k]], isC = /^#/.test(v);
    return `<div class="lk"><span>${LOOK[k].name}</span><button type="button" data-k="${k}" data-d="-1" aria-label="${LOOK[k].name} 이전">‹</button><b>${isC ? `<i style="background:${v}"></i>` : esc(v)}</b><button type="button" data-k="${k}" data-d="1" aria-label="${LOOK[k].name} 다음">›</button></div>`;
  }).join('');
  [].forEach.call($('lookRows').querySelectorAll('button'), bt => { bt.onclick = () => { const k = bt.dataset.k, n = LOOK[k].list.length; lookDraft[k] = (lookDraft[k] + +bt.dataset.d + n) % n; lookRows(); lookRender(); }; });
}
function openLook() {
  lookDraft = myLook(); show(ui.title, false); show(ui.look, true); lookOn = true;
  try { lookRender(); } catch (e) { /* 미리보기 없이 */ }
  lookRows();
  (function spin() { if (!lookOn) return; requestAnimationFrame(spin); if (lookMesh) { lookMesh.rotation.y += 0.02; animateRunner(lookMesh, 2, false, 0.016, ''); lookR.render(lookScene, lookCam); } })();
}
$('lookBtn').onclick = openLook;
$('lookRandom').onclick = () => { lookDraft = randomLook(Math.random); delete lookDraft.topHex; lookRows(); lookRender(); };
$('lookSave').onclick = () => { try { localStorage.setItem('tower_look', JSON.stringify(lookDraft)); } catch (e) { /* 없음 */ } lookOn = false; show(ui.look, false); show(ui.title, true); flash('내 러너를 저장했어요', 1.4); };
$('lookBack').onclick = () => { lookOn = false; show(ui.look, false); show(ui.title, true); };

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
  else if (k === 'boost') { tone(260, 980, 0.28, 'sawtooth', 0.05); tone(520, 1400, 0.2, 'triangle', 0.04, 0.06); }
  else if (k === 'slip') { tone(1200, 300, 0.35, 'triangle', 0.08); tone(300, 120, 0.25, 'square', 0.05, 0.3); }
  else if (k === 'cannon') { tone(140, 40, 0.35, 'sawtooth', 0.1); tone(80, 30, 0.4, 'square', 0.07); }
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
    // 기둥 쪽(화면 오른쪽)에 붙어 달리면 카메라를 바깥으로 비켜 세워 기둥이 나를 가리지 않게
    const side = 1.5 + Math.max(0, RT - 3.5 - Math.hypot(p.x, p.z)) * 0.8;
    const wantP = new THREE.Vector3(p.x - f[0] * back + n[0] * side, py + up, p.z - f[1] * back + n[1] * side);
    const k = 1 - Math.exp(-dt * 7); camPos.lerp(wantP, k); camLook.lerp(new THREE.Vector3(p.x + f[0] * 6, py + 1, p.z + f[1] * 6), k);
  }
  camera.position.copy(camPos).add(fx.shakeOffset(dt)); camera.lookAt(camLook);
}
function drawBodies(dt) {
  for (const b of S.bodies) {
    if (b.out) continue;
    const m = b.mesh, sp = Math.hypot(b.vel.x, b.vel.z);
    m.position.copy(b.pos);
    if (sp > 0.5 && !b.diving && b.stun <= 0 && b.shoveT <= 0) b.yaw = Math.atan2(-b.vel.x, -b.vel.z);
    let d = b.yaw - m.rotation.y; d = Math.atan2(Math.sin(d), Math.cos(d)); m.rotation.y += d * Math.min(1, dt * 14);
    m.rotation.z = b.stun > 0 ? Math.sin(b.stun * 30) * 0.3 : 0;
    if (b.slipT > 0) m.rotation.y += dt * 22;
    if (b.deadT <= 0) m.visible = !(b.safe > 0.4 && Math.floor(b.safe * 12) % 2);
    animateRunner(m, sp, !b.onGround, dt, b.diving ? 'dive' : b.shoveT > 0 ? 'shove' : '');
    m.userData.bubble.visible = b.shieldT > 0; { const bs = m.userData.body.userData.base || [1, 1, 1], k = b.boostT > 0 ? 1.08 + Math.sin(performance.now() / 60) * 0.04 : 1; m.userData.body.scale.set(bs[0] * k, bs[1] * k, bs[2] * k); }
    if (b.tag) { const dd = S.me ? Math.hypot(b.pos.x - S.me.pos.x, b.pos.z - S.me.pos.z) : 99; b.tag.visible = dd > 2.5 && dd < 11 && Math.abs(b.pos.y - S.me.pos.y) < 4; }
    if (S.mode === 'play' && b.deadT <= 0 && seen(b, 16)) bodyFx(b, sp, dt);
  }
  const me = S.me; if (!me) return;
  const gt = groundTop(me.pos.x, me.pos.z, me.pos.y);
  blob.visible = gt > -Infinity && me.deadT <= 0 && !me.out; if (blob.visible) { blob.position.set(me.pos.x, gt + 0.03, me.pos.z); const s = Math.max(0.35, 1 - (me.pos.y - gt) * 0.12); blob.scale.set(s, s, s); }
}
function bodyFx(b, sp, dt) {
  const p = b.pos;
  if (b.onGround && sp > 3) { b.stepT = (b.stepT || 0) - dt; if (b.stepT <= 0) { b.stepT = 0.24 + Math.random() * 0.06; const ice = b.ground && b.ground.ice; fx.dust(p.x, p.y, p.z, { n: b === S.me ? 2 : 1, speed: 0.8, size: 0.32, life: 0.8, colors: ice ? [0xDFF8FF, 0xFFFFFF] : undefined }); if (ice && Math.random() < 0.5) fx.one(p.x, p.y + 0.1, p.z, (Math.random() - 0.5) * 2, 1.5, (Math.random() - 0.5) * 2, { color: 0xBFF4FF, size: 0.3, life: 0.4, grav: 6, shape: 2 }); } }
  if (b.boostT > 0) for (let i = 0; i < 2; i++) fx.one(p.x + (Math.random() - 0.5) * 0.5, p.y + 0.2 + Math.random() * 1.2, p.z + (Math.random() - 0.5) * 0.5, -b.vel.x * 0.15, 0.4, -b.vel.z * 0.15, { color: Math.random() < 0.5 ? 0xFFD23F : 0x24D8FF, size: 0.55, life: 0.35 });
  if (b.shieldT > 0 && Math.random() < dt * 14) { const a = Math.random() * TAU; fx.one(p.x + Math.cos(a), p.y + 0.4 + Math.random() * 1.2, p.z + Math.sin(a), 0, 0.8, 0, { color: 0xBFF4FF, size: 0.35, life: 0.5, shape: 2 }); }
  if (b.diving) fx.one(p.x, p.y + 0.6, p.z, 0, 0, 0, { color: 0xFFFFFF, size: 0.6, life: 0.25, soft: true, alpha: 0.6 });
}
// 몸 밖 효과: 바람 줄기 · 펀치 불꽃 · 굴러오는 공 먼지 · 왕관 반짝 · 멀리 폭죽
let ambT = 0;
function worldFx(dt) {
  const W = S.world, me = S.me;
  for (const s of W.solids) if (s.punch) { if (s.flash && !s.wasFlash && seenAt(s.cx, s.top, s.cz)) { const q = s.mesh ? s.mesh.position : { x: s.cx, y: s.top, z: s.cz }; fx.burst(q.x, q.y + 0.4, q.z, { n: 10, speed: 5, colors: [0xFFD23F, 0xFF9F1C, 0xFFFFFF], life: 0.35, size: 0.5 }); } s.wasFlash = s.flash; }
  for (const w of W.winds) {
    const on = w.active(S.ht), warn = w.warn(S.ht);
    if ((on || warn) && me && me.s > w.s0 - 20 && me.s < w.s1 + 5) for (let i = 0; i < (on ? 5 : 1); i++) {
      const ws = w.s0 + Math.random() * (w.s1 - w.s0), q = P(ws, -HW + Math.random() * 2 * HW, trackY(ws) + 0.3 + Math.random() * 2.4), a = q.a;
      fx.one(q.x, q.y, q.z, Math.cos(a) * (on ? 14 : 5), 0, Math.sin(a) * (on ? 14 : 5), { color: 0xFFFFFF, size: on ? 0.5 : 0.3, size1: 0.1, life: 0.55, soft: true, alpha: on ? 0.75 : 0.4 });
    }
  }
  if (W.balls) for (const bl of W.balls.list) if (Math.random() < dt * 8 && seenAt(bl.x, bl.y, bl.z, 26)) fx.dust(bl.x, bl.y - bl.r, bl.z, { n: 2, speed: 1.5, size: 0.6 });
  if (W.crown && Math.random() < dt * 6) { const c = W.crown.position, a = Math.random() * TAU; fx.one(c.x + Math.cos(a) * 2.4, c.y + Math.random() * 1.5, c.z + Math.sin(a) * 2.4, 0, 1, 0, { color: 0xFFE27A, size: 0.6, life: 0.7, shape: 2 }); }
  // 멀리 하늘 폭죽(처음 화면은 자주, 경기 중엔 가끔) · 완주 뒤 축하 쇼
  ambT -= dt; const every = S.mode === 'title' ? 0.9 : S.showT > 0 ? 0.25 : 4.5;
  if (S.showT > 0) S.showT -= dt;
  if (ambT <= 0) {
    ambT = every * (0.6 + Math.random() * 0.8);
    const a = Math.random() * TAU, r = S.showT > 0 ? 20 + Math.random() * 40 : 70 + Math.random() * 60, by = S.showT > 0 && me ? me.pos.y - 4 : W.topY * Math.random();
    fx.firework(Math.cos(a) * r, by, Math.sin(a) * r, { vy: 20 + Math.random() * 10, big: S.showT > 0 ? 1.3 : 1.6 });
  }
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
    if (S.intro <= 0.2) { S.mode = 'play'; banner('<b class="num go">GO!</b>', 'count', 0.8); sfx('go'); S.t = 0; celebrateGate(0.5, 0, 1.2, true); fx.shake(0.35); }
  } else if (S.mode === 'play') {
    readInput(); S.acc += dt;
    while (S.acc >= DT && S.mode === 'play') { S.acc -= DT; S.playT += DT; simStep(DT); }
    drawBodies(dt); if (S.mode === 'play') updateHud();
  } else { idleWorld(dt); if (S.me) drawBodies(dt); }
  UT.value = now / 1000; updateCamera(dt); syncWorld(S.world, now, dt); if (S.mode !== 'sim') worldFx(dt); confettiTick(dt);
  renderer.render(scene, camera);
}
newWorld(); S.mode = 'title'; refreshStatus();
requestAnimationFrame(frame);

// 개발용(?dev): 화면 없이 물리를 돌려 코스를 깰 수 있는지 · 시간을 본다
if (DEV) window.__tower = { S, P, RT, HW, trackY, simStep, setupRace, stepBody, groundTop, DT, SECTIONS, towerScore, shove, aiThink, PERSONAS, mulberry, relS, fx, hitBody, celebrateGate, scene, info: () => ({ calls: renderer.info.render.calls, tris: renderer.info.render.triangles, geos: renderer.info.memory.geometries, tex: renderer.info.memory.textures, fx: fx.count }) };
