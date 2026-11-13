/* StepUP 타워 — 팡팡 효과(2026-11-13): 불꽃 · 먼지 · 물보라 · 충격파 고리 · 의성어 글자 · 화면 흔들림 · 폭죽.
   game-src.js 가 묶어 쓴다(npm run build:game). 효과는 화면에만 — 물리 · 점수와 상관없다. */
import * as THREE from 'three';

const hex3 = h => [((h >> 16) & 255) / 255, ((h >> 8) & 255) / 255, (h & 255) / 255];
const VERT = `attribute float size; attribute vec4 rgba; attribute float shape; uniform float uScale;
varying vec4 vC; varying float vS;
void main(){ vC = rgba; vS = shape; vec4 mv = modelViewMatrix * vec4(position, 1.0);
  gl_PointSize = size * uScale / max(0.1, -mv.z); gl_Position = projectionMatrix * mv; }`;
// 모양: 0 둥근 빛 · 1 네모(색종이 · 부스러기) · 2 별 · 3 물방울(테두리 진한 원)
const FRAG = `varying vec4 vC; varying float vS;
void main(){ vec2 d = gl_PointCoord - 0.5; float r = length(d); float a;
  if (vS < 0.5) { if (r > 0.5) discard; a = smoothstep(0.5, 0.0, r); a *= a * 1.6; }
  else if (vS < 1.5) { if (max(abs(d.x), abs(d.y)) > 0.36) discard; a = 1.0; }
  else if (vS < 2.5) { float ang = atan(d.y, d.x); float k = 0.22 + 0.2 * pow(abs(cos(ang * 2.5)), 6.0); if (r > k + 0.06) discard; a = r < k ? 1.0 : 0.5; }
  else { if (r > 0.5) discard; a = r > 0.38 ? 1.0 : 0.75; }
  gl_FragColor = vec4(vC.rgb, vC.a * min(1.0, a)); }`;

class Pool {
  constructor(scene, n, additive) {
    this.n = n; this.next = 0; this.live = 0;
    this.pos = new Float32Array(n * 3); this.col = new Float32Array(n * 4); this.size = new Float32Array(n); this.shape = new Float32Array(n);
    this.vel = new Float32Array(n * 3); this.life = new Float32Array(n); this.max = new Float32Array(n); this.s0 = new Float32Array(n); this.s1 = new Float32Array(n);
    this.grav = new Float32Array(n); this.drag = new Float32Array(n); this.a0 = new Float32Array(n); this.base = new Float32Array(n * 3);
    for (let i = 0; i < n; i++) this.pos[i * 3 + 1] = -9999;
    const g = new THREE.BufferGeometry();
    g.setAttribute('position', new THREE.BufferAttribute(this.pos, 3)); g.setAttribute('rgba', new THREE.BufferAttribute(this.col, 4));
    g.setAttribute('size', new THREE.BufferAttribute(this.size, 1)); g.setAttribute('shape', new THREE.BufferAttribute(this.shape, 1));
    this.geo = g;
    this.mat = new THREE.ShaderMaterial({ uniforms: { uScale: { value: 400 } }, vertexShader: VERT, fragmentShader: FRAG, transparent: true, depthWrite: false,
      blending: additive ? THREE.AdditiveBlending : THREE.NormalBlending });
    this.pts = new THREE.Points(g, this.mat); this.pts.frustumCulled = false; this.pts.renderOrder = additive ? 4 : 3; scene.add(this.pts);
  }
  add(x, y, z, vx, vy, vz, life, s0, s1, color, a0, grav, drag, shape) {
    const i = this.next; this.next = (i + 1) % this.n;
    this.pos[i * 3] = x; this.pos[i * 3 + 1] = y; this.pos[i * 3 + 2] = z;
    this.vel[i * 3] = vx; this.vel[i * 3 + 1] = vy; this.vel[i * 3 + 2] = vz;
    this.life[i] = life; this.max[i] = life; this.s0[i] = s0; this.s1[i] = s1; this.a0[i] = a0; this.grav[i] = grav; this.drag[i] = drag; this.shape[i] = shape;
    this.base[i * 3] = color[0]; this.base[i * 3 + 1] = color[1]; this.base[i * 3 + 2] = color[2];
  }
  tick(dt) {
    const P = this.pos, V = this.vel, C = this.col; let live = 0;
    for (let i = 0; i < this.n; i++) {
      if (this.life[i] <= 0) continue;
      const l = this.life[i] -= dt;
      if (l <= 0) { P[i * 3 + 1] = -9999; this.size[i] = 0; continue; }
      live++;
      const k = 1 - l / this.max[i], dr = Math.max(0, 1 - this.drag[i] * dt);
      V[i * 3] *= dr; V[i * 3 + 2] *= dr; V[i * 3 + 1] = V[i * 3 + 1] * dr - this.grav[i] * dt;
      P[i * 3] += V[i * 3] * dt; P[i * 3 + 1] += V[i * 3 + 1] * dt; P[i * 3 + 2] += V[i * 3 + 2] * dt;
      this.size[i] = this.s0[i] + (this.s1[i] - this.s0[i]) * k;
      C[i * 4] = this.base[i * 3]; C[i * 4 + 1] = this.base[i * 3 + 1]; C[i * 4 + 2] = this.base[i * 3 + 2];
      C[i * 4 + 3] = this.a0[i] * (k < 0.7 ? 1 : 1 - (k - 0.7) / 0.3);
    }
    this.live = live;
    const g = this.geo.attributes; g.position.needsUpdate = true; g.rgba.needsUpdate = true; g.size.needsUpdate = true; g.shape.needsUpdate = true;
  }
}

// 의성어 글자(굵은 테두리 · 색 그라데이션) — 같은 글자 · 색은 한 번만 그린다
const WORD_TEX = new Map();
function wordTex(text, color) {
  const key = text + color; if (WORD_TEX.has(key)) return WORD_TEX.get(key);
  const c = document.createElement('canvas'); c.width = 256; c.height = 128; const g = c.getContext('2d');
  const fs = text.length > 4 ? 58 : 76;
  g.font = `900 ${fs}px "Black Han Sans", "Arial Black", system-ui, sans-serif`; g.textAlign = 'center'; g.textBaseline = 'middle';
  g.lineJoin = 'round'; g.lineWidth = 16; g.strokeStyle = '#0B1A33'; g.strokeText(text, 128, 66);
  g.lineWidth = 7; g.strokeStyle = '#FFFFFF'; g.strokeText(text, 128, 66);
  const gr = g.createLinearGradient(0, 30, 0, 100); gr.addColorStop(0, '#FFFFFF'); gr.addColorStop(0.35, color); gr.addColorStop(1, color);
  g.fillStyle = gr; g.fillText(text, 128, 66);
  const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; WORD_TEX.set(key, t); return t;
}
// 이모지 말풍선(AI 표정)
const EMO_TEX = new Map();
function emoTex(e) {
  if (EMO_TEX.has(e)) return EMO_TEX.get(e);
  const c = document.createElement('canvas'); c.width = 96; c.height = 96; const g = c.getContext('2d');
  g.fillStyle = 'rgba(255,255,255,.94)'; g.beginPath(); g.arc(48, 44, 38, 0, Math.PI * 2); g.fill();
  g.beginPath(); g.moveTo(40, 78); g.lineTo(48, 94); g.lineTo(58, 76); g.fill();
  g.lineWidth = 4; g.strokeStyle = 'rgba(11,26,51,.35)'; g.beginPath(); g.arc(48, 44, 38, 0, Math.PI * 2); g.stroke();
  g.font = '48px "Apple Color Emoji","Segoe UI Emoji","Noto Color Emoji",sans-serif'; g.textAlign = 'center'; g.textBaseline = 'middle'; g.fillText(e, 48, 47);
  const t = new THREE.CanvasTexture(c); t.colorSpace = THREE.SRGBColorSpace; EMO_TEX.set(e, t); return t;
}

export const PALETTE = [0xFF4F8B, 0xFFD23F, 0x24D8FF, 0x7CFFB2, 0xFF9F1C, 0xA77BFF, 0xFFFFFF];

export function createFx(scene) {
  const glow = new Pool(scene, 1800, true), soft = new Pool(scene, 1600, false);
  const rand = (a, b) => a + Math.random() * (b - a), pick = a => a[(Math.random() * a.length) | 0];
  // 충격파 고리
  const ringGeo = new THREE.RingGeometry(0.82, 1, 48), rings = [];
  for (let i = 0; i < 28; i++) {
    const m = new THREE.Mesh(ringGeo, new THREE.MeshBasicMaterial({ color: 0xffffff, transparent: true, opacity: 0, depthWrite: false, blending: THREE.AdditiveBlending, side: THREE.DoubleSide }));
    m.visible = false; m.renderOrder = 4; scene.add(m); rings.push({ m, t: 0, life: 1, r0: 0.5, r1: 3 });
  }
  let ringNext = 0;
  // 글자 · 말풍선
  const words = [];
  for (let i = 0; i < 16; i++) { const sp = new THREE.Sprite(new THREE.SpriteMaterial({ transparent: true, depthTest: false, depthWrite: false })); sp.visible = false; sp.renderOrder = 9; scene.add(sp); words.push({ sp, t: 0, life: 1, follow: null, y0: 0, big: 1, rot: 0 }); }
  let wordNext = 0;
  // 번개(선)
  const boltGeo = new THREE.BufferGeometry(), boltPos = new Float32Array(64 * 6);
  boltGeo.setAttribute('position', new THREE.BufferAttribute(boltPos, 3));
  const bolts = new THREE.LineSegments(boltGeo, new THREE.LineBasicMaterial({ color: 0xBFF4FF, transparent: true, opacity: 0.95, blending: THREE.AdditiveBlending, depthWrite: false }));
  bolts.frustumCulled = false; bolts.renderOrder = 5; scene.add(bolts); let boltT = 0, boltN = 0;
  // 폭죽 로켓
  const rockets = [];
  // 화면 흔들림
  let shakeA = 0; const shakeV = new THREE.Vector3();

  const api = {
    on: true,
    // 퍼지는 알갱이: 빛(glow) 또는 보통(soft)
    burst(x, y, z, o = {}) {
      if (!api.on) return;
      const n = o.n || 20, sp = o.speed || 6, up = o.up != null ? o.up : 3, cols = (o.colors || PALETTE).map(hex3), pool = o.soft ? soft : glow;
      const life = o.life || 0.7, s0 = o.size || 0.5, s1 = o.size1 != null ? o.size1 : s0 * 0.2, grav = o.grav != null ? o.grav : 9, drag = o.drag != null ? o.drag : 1.5, shape = o.shape || 0, flat = o.flat || 0;
      for (let i = 0; i < n; i++) {
        const th = Math.random() * Math.PI * 2, ph = flat ? Math.PI / 2 + rand(-flat, flat) : Math.acos(rand(-1, 1)), v = sp * rand(0.45, 1);
        const vx = Math.sin(ph) * Math.cos(th) * v + (o.vx || 0), vz = Math.sin(ph) * Math.sin(th) * v + (o.vz || 0), vy = Math.abs(Math.cos(ph)) * v * (flat ? 0.3 : 1) * (o.down ? -1 : 1) + up * rand(0.5, 1);
        pool.add(x + rand(-1, 1) * (o.jitter || 0), y + rand(-1, 1) * (o.jitter || 0), z + rand(-1, 1) * (o.jitter || 0), vx, vy, vz, life * rand(0.6, 1.15), s0 * rand(0.7, 1.3), s1, pick(cols), o.alpha || 1, grav, drag, shape);
      }
    },
    // 한 알
    one(x, y, z, vx, vy, vz, o = {}) { if (!api.on) return; (o.soft ? soft : glow).add(x, y, z, vx, vy, vz, o.life || 0.5, o.size || 0.4, o.size1 != null ? o.size1 : 0.05, hex3(o.color != null ? o.color : 0xffffff), o.alpha || 1, o.grav || 0, o.drag || 0, o.shape || 0); },
    ring(x, y, z, o = {}) {
      if (!api.on) return;
      const R = rings[ringNext]; ringNext = (ringNext + 1) % rings.length;
      R.t = 0; R.life = o.life || 0.45; R.r0 = o.r0 || 0.4; R.r1 = o.r1 || 3; R.a = o.alpha || 0.9;
      R.m.material.color.setHex(o.color != null ? o.color : 0xffffff); R.m.position.set(x, y, z);
      if (o.normal) R.m.lookAt(x + o.normal[0], y + o.normal[1], z + o.normal[2]); else R.m.rotation.set(-Math.PI / 2, 0, 0);
      R.m.visible = true;
    },
    word(x, y, z, text, color = '#FFD23F', o = {}) {
      if (!api.on) return;
      const W = words[wordNext]; wordNext = (wordNext + 1) % words.length;
      W.sp.material.map = wordTex(text, color); W.sp.material.needsUpdate = true; W.sp.material.rotation = W.rot = rand(-0.25, 0.25);
      W.t = 0; W.life = o.life || 0.9; W.big = o.big || 1; W.follow = o.follow || null; W.x = x; W.y0 = y; W.z = z; W.ratio = 2; W.sp.visible = true;
    },
    emote(body, e) {
      if (!api.on) return;
      const W = words[wordNext]; wordNext = (wordNext + 1) % words.length;
      W.sp.material.map = emoTex(e); W.sp.material.needsUpdate = true; W.sp.material.rotation = W.rot = 0;
      W.t = 0; W.life = 1.4; W.big = 0.55; W.follow = body; W.ratio = 1; W.sp.visible = true;
    },
    bolt(ax, ay, az, bx, by, bz) {
      if (!api.on) return;
      const segs = 7; let px = ax, py = ay, pz = az;
      for (let i = 1; i <= segs && boltN < 64; i++) {
        const k = i / segs, j = i === segs ? 0 : 0.7;
        const nx = ax + (bx - ax) * k + rand(-j, j), ny = ay + (by - ay) * k + rand(-j, j), nz = az + (bz - az) * k + rand(-j, j);
        boltPos.set([px, py, pz, nx, ny, nz], boltN * 6); boltN++; px = nx; py = ny; pz = nz;
      }
      boltT = 0.28; boltGeo.setDrawRange(0, boltN * 2); boltGeo.attributes.position.needsUpdate = true;
      api.burst(bx, by, bz, { n: 14, speed: 5, colors: [0xBFF4FF, 0xFFFFFF, 0x24D8FF], life: 0.4, size: 0.45 });
    },
    // 폭죽: 꼬리를 그리며 올라가 터진다
    firework(x, y, z, o = {}) {
      if (!api.on) return;
      rockets.push({ x, y, z, vy: o.vy || rand(16, 22), vx: rand(-1.5, 1.5), vz: rand(-1.5, 1.5), t: o.delay || 0, fuse: o.fuse || rand(0.8, 1.15), cols: o.colors || [pick(PALETTE), pick(PALETTE)], big: o.big || 1 });
    },
    explode(x, y, z, cols, big = 1) {
      api.burst(x, y, z, { n: Math.round(70 * big), speed: 13 * big, up: 0, colors: cols, life: 1.4, size: 1.1 * big, size1: 0.15, grav: 5, drag: 1.6 });
      api.burst(x, y, z, { n: Math.round(26 * big), speed: 6 * big, up: 0, colors: [0xFFFFFF, 0xFFF3B0], life: 0.6, size: 0.8 * big, grav: 2 });
      api.burst(x, y, z, { n: Math.round(14 * big), speed: 9 * big, up: 0, colors: cols, life: 1.9, size: 0.6, grav: 7, drag: 1, shape: 2 });
      api.ring(x, y, z, { r0: 0.5, r1: 9 * big, life: 0.55, color: cols[0], normal: [0, 0, 1] });
    },
    // 색종이 대포(옆으로 비스듬히 뿜는다)
    cannon(x, y, z, dx, dz, n = 50) {
      if (!api.on) return;
      for (let i = 0; i < n; i++) {
        const v = rand(8, 16);
        soft.add(x, y, z, dx * v + rand(-2.5, 2.5), rand(9, 17), dz * v + rand(-2.5, 2.5), rand(1.6, 2.6), rand(0.35, 0.6), 0.3, hex3(pick(PALETTE)), 1, 7, 1.2, 1);
      }
      api.burst(x, y, z, { n: 16, speed: 5, colors: [0xFFFFFF, 0xFFD23F], life: 0.4, size: 0.9 });
    },
    // 물보라(떨어진 자리)
    splash(x, y, z, big = 1) {
      if (!api.on) return;
      for (let i = 0; i < 46 * big; i++) { const a = Math.random() * Math.PI * 2, r = rand(0, 1.2); soft.add(x + Math.cos(a) * r, y, z + Math.sin(a) * r, Math.cos(a) * rand(1, 4), rand(8, 17) * big, Math.sin(a) * rand(1, 4), rand(0.9, 1.4), rand(0.45, 0.8), 0.2, hex3(pick([0xFFFFFF, 0xBFE9FF, 0x7FD8FF])), 0.95, 22, 0.2, 3); }
      api.ring(x, y + 0.1, z, { r0: 0.6, r1: 6 * big, life: 0.8, color: 0xBFE9FF });
      api.ring(x, y + 0.1, z, { r0: 0.3, r1: 3.5 * big, life: 0.6, color: 0xFFFFFF });
    },
    dust(x, y, z, o = {}) {
      if (!api.on) return;
      const n = o.n || 8, cols = (o.colors || [0xFFFFFF, 0xEAF4FF, 0xD7E6F5]).map(hex3);
      for (let i = 0; i < n; i++) { const a = Math.random() * Math.PI * 2, v = rand(0.5, 1) * (o.speed || 2.6); soft.add(x, y + 0.1, z, Math.cos(a) * v, rand(0.3, 1.4), Math.sin(a) * v, rand(0.35, 0.6) * (o.life || 1), (o.size || 0.55) * rand(0.7, 1.2), (o.size || 0.55) * 2.2, pick(cols), o.alpha || 0.75, -0.5, 3, 0); }
    },
    shake(a) { shakeA = Math.max(shakeA, a); },
    shakeOffset(dt) {
      shakeA = Math.max(0, shakeA - dt * 2.6);
      const k = shakeA * shakeA; shakeV.set(rand(-1, 1) * k, rand(-1, 1) * k, rand(-1, 1) * k); return shakeV;
    },
    tick(dt, camera) {
      for (let i = rockets.length - 1; i >= 0; i--) {
        const r = rockets[i];
        if (r.t > 0) { r.t -= dt; continue; }
        r.fuse -= dt; r.vy -= 9 * dt; r.x += r.vx * dt; r.y += r.vy * dt; r.z += r.vz * dt;
        glow.add(r.x, r.y, r.z, rand(-0.6, 0.6), rand(-2, -0.5), rand(-0.6, 0.6), 0.45, 0.55, 0.05, hex3(0xFFE9A8), 1, 2, 1, 0);
        if (r.fuse <= 0) { api.explode(r.x, r.y, r.z, r.cols.map(c => c), r.big); rockets.splice(i, 1); }
      }
      glow.tick(dt); soft.tick(dt);
      for (const R of rings) {
        if (!R.m.visible) continue;
        R.t += dt; const k = R.t / R.life; if (k >= 1) { R.m.visible = false; continue; }
        const e = 1 - Math.pow(1 - k, 3), r = R.r0 + (R.r1 - R.r0) * e; R.m.scale.set(r, r, r); R.m.material.opacity = R.a * (1 - k);
      }
      for (const W of words) {
        if (!W.sp.visible) continue;
        W.t += dt; const k = W.t / W.life; if (k >= 1) { W.sp.visible = false; W.follow = null; continue; }
        const pop = k < 0.18 ? 0.4 + 0.9 * (k / 0.18) : k < 0.3 ? 1.3 - 0.3 * ((k - 0.18) / 0.12) : 1;
        const sc = W.big * pop * 1.5; W.sp.scale.set(sc * W.ratio, sc, 1);
        if (W.follow) W.sp.position.set(W.follow.pos.x, W.follow.pos.y + 2.75 + k * 0.4, W.follow.pos.z);
        else W.sp.position.set(W.x, W.y0 + k * 1.6, W.z);
        W.sp.material.opacity = k > 0.75 ? 1 - (k - 0.75) / 0.25 : 1;
      }
      if (boltT > 0) { boltT -= dt; bolts.material.opacity = Math.max(0, boltT / 0.28) * (Math.random() < 0.5 ? 1 : 0.5); if (boltT <= 0) { boltN = 0; boltGeo.setDrawRange(0, 0); } }
      if (camera) { const h = camera.isPerspectiveCamera ? (innerHeight * Math.min(window.devicePixelRatio || 1, 1.75)) / (2 * Math.tan(camera.fov * Math.PI / 360)) : 400; glow.mat.uniforms.uScale.value = h; soft.mat.uniforms.uScale.value = h; }
    },
    get count() { return glow.live + soft.live; }
  };
  return api;
}
