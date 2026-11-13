/* StepUP 타워 — 꾸밈(2026-11-13 밤): 관중 · 꼬마전구 · 삼각 깃발 줄 · 풍선 · 구름 · 관람차 · 열기구 · 운동화 비행선 ·
   떠 있는 섬 · 야자수 · 고무 오리 · 금별 · 기둥 네온 고리 · 구간마다 큰 소품 · 해 · 먼 산.
   물리와 상관없다(부딪히지 않는다). 그리는 횟수를 줄이려고 같은 모양은 InstancedMesh · 합친 모양 하나로 그린다. */
import * as THREE from 'three';
import { mergeGeometries } from 'three/addons/utils/BufferGeometryUtils.js';

export const UT = { value: 0 }; // 셰이더 시계(초)
const M4 = new THREE.Matrix4(), Q = new THREE.Quaternion(), E = new THREE.Euler(), V = new THREE.Vector3(), SC = new THREE.Vector3(), COL = new THREE.Color();
const srgb = h => new THREE.Color().setHex(h);
function put(mesh, i, x, y, z, rx = 0, ry = 0, rz = 0, sx = 1, sy = sx, sz = sx) { E.set(rx, ry, rz); Q.setFromEuler(E); M4.compose(V.set(x, y, z), Q, SC.set(sx, sy, sz)); mesh.setMatrixAt(i, M4); }
// 셰이더에 시계를 넣어 GPU 가 흔든다(CPU 일 없음)
function wobble(material, code, key) {
  material.onBeforeCompile = sh => { sh.uniforms.uTime = UT; sh.vertexShader = 'uniform float uTime;\n' + sh.vertexShader.replace('#include <begin_vertex>', '#include <begin_vertex>\n#ifdef USE_INSTANCING\n' + code + '\n#endif'); };
  material.customProgramCacheKey = () => key; return material;
}
// 여러 모양을 색과 함께 하나로(정점 색)
function baked(parts) {
  const geos = parts.map(([geo, m4, hex]) => {
    const g = (geo.index ? geo.toNonIndexed() : geo.clone()); g.applyMatrix4(m4); g.deleteAttribute('uv');
    const c = srgb(hex), n = g.attributes.position.count, a = new Float32Array(n * 3); for (let i = 0; i < n; i++) { a[i * 3] = c.r; a[i * 3 + 1] = c.g; a[i * 3 + 2] = c.b; }
    g.setAttribute('color', new THREE.BufferAttribute(a, 3)); return g;
  });
  return mergeGeometries(geos);
}
const mx = (x, y, z, rx = 0, ry = 0, rz = 0, sx = 1, sy = sx, sz = sx) => { E.set(rx, ry, rz); Q.setFromEuler(E); return new THREE.Matrix4().compose(new THREE.Vector3(x, y, z), Q.clone(), new THREE.Vector3(sx, sy, sz)); };
const VC = (o = {}) => new THREE.MeshLambertMaterial(Object.assign({ vertexColors: true }, o));

export function drawDecor(W, G, K) {
  const { P, trackY, RT, HW, PILLAR, TAU, mulberry, canvasTex, textTex, THEMES } = K;
  const rnd = mulberry(77), cols = [0xFF4F8B, 0xFFD23F, 0x24D8FF, 0x7CFFB2, 0xFF9F1C, 0xA77BFF, 0xFFFFFF, 0x1166FF];
  const D = { anim: [] };
  const arcs = W.solids.filter(s => s.kind === 'arc' && !s.pillar);

  // ── 바깥 가장자리: 빛나는 띠 + 꼬마전구 + 바닥 화살표 ─────────────────
  {
    const trim = [], chev = [];
    const bulbs = [];
    for (const a of arcs) {
      const n = Math.max(2, Math.ceil(a.len / 1.5)), th = THEMES[a.theme], acc = th.accent;
      for (let i = 0; i < n; i++) {
        const k0 = i / n, k1 = (i + 1) / n, a0 = a.a0 + (a.a1 - a.a0) * k0, a1 = a.a0 + (a.a1 - a.a0) * k1, y0 = a.y0 + (a.y1 - a.y0) * k0 + 0.02, y1 = a.y0 + (a.y1 - a.y0) * k1 + 0.02;
        for (const [r0, r1] of [[a.r1 - 0.32, a.r1], [a.r0, a.r0 + 0.22]]) trim.push([r0, r1, a0, a1, y0, y1, (i % 2) ? acc : 0xFFFFFF]);
      }
      for (let d = 1.1; d < a.len; d += 2.2) { const q = P(a.s0 + d, a.r1 - RT + 0.02, 0); bulbs.push([q.x, a.y0 + (a.y1 - a.y0) * d / a.len + 0.16, q.z]); }
      if (a.r1 - a.r0 > 8 && !a.door && !a.ice) for (let d = 5; d < a.len - 2; d += 13) chev.push([a, d]);
    }
    // 띠(정점 색 · 하나로)
    const pos = [], col = [];
    for (const [r0, r1, a0, a1, y0, y1, hex] of trim) {
      const c = srgb(hex), p = (r, an, y) => pos.push(r * Math.cos(an), y, r * Math.sin(an));
      p(r0, a0, y0); p(r1, a0, y0); p(r1, a1, y1); p(r0, a0, y0); p(r1, a1, y1); p(r0, a1, y1);
      for (let i = 0; i < 6; i++) col.push(c.r, c.g, c.b);
    }
    const tg = new THREE.BufferGeometry(); tg.setAttribute('position', new THREE.Float32BufferAttribute(pos, 3)); tg.setAttribute('color', new THREE.Float32BufferAttribute(col, 3));
    const tm = new THREE.Mesh(tg, new THREE.MeshBasicMaterial({ vertexColors: true, side: THREE.DoubleSide, polygonOffset: true, polygonOffsetFactor: -2 })); G.add(tm);
    // 꼬마전구(색이 흘러간다)
    const bm = new THREE.InstancedMesh(new THREE.SphereGeometry(0.13, 8, 6), new THREE.MeshBasicMaterial(), bulbs.length);
    bulbs.forEach(([x, y, z], i) => { put(bm, i, x, y, z); bm.setColorAt(i, srgb(cols[i % 4])); });
    bm.instanceMatrix.needsUpdate = true; G.add(bm);
    let chase = 0, ct = 0;
    D.anim.push((now, dt) => { ct -= dt; if (ct > 0) return; ct = 0.14; chase++; for (let i = 0; i < bulbs.length; i++) { const on = (i + chase) % 4; bm.setColorAt(i, COL.setHex(on === 0 ? 0xFFFFFF : cols[(i + (chase >> 2)) % 4])); } bm.instanceColor.needsUpdate = true; });
    // 바닥 화살표(가는 쪽)
    const ctex = canvasTex(128, 128, (g, w, h) => { g.clearRect(0, 0, w, h); g.fillStyle = 'rgba(17,102,255,.32)'; for (const o of [0, 44]) { g.beginPath(); g.moveTo(10, 40 + o); g.lineTo(64, 4 + o); g.lineTo(118, 40 + o); g.lineTo(118, 62 + o); g.lineTo(64, 26 + o); g.lineTo(10, 62 + o); g.fill(); } });
    const cgeos = chev.map(([a, d]) => { const s = a.s0 + d, q = P(s, 0, 0), y = a.y0 + (a.y1 - a.y0) * d / a.len + 0.03, g = new THREE.PlaneGeometry(3.2, 3.2); g.rotateX(-Math.PI / 2); g.rotateY(-q.a + Math.PI); const slope = Math.atan2(a.y1 - a.y0, a.len); g.applyMatrix4(new THREE.Matrix4().makeRotationAxis(new THREE.Vector3(Math.cos(q.a), 0, Math.sin(q.a)), -slope)); g.translate(q.x, y, q.z); return g; });
    if (cgeos.length) G.add(new THREE.Mesh(mergeGeometries(cgeos), new THREE.MeshBasicMaterial({ map: ctex, transparent: true, depthWrite: false, polygonOffset: true, polygonOffsetFactor: -3 })));
  }

  // ── 깃발(7m 마다) · 삼각 깃발 줄 ────────────────────────────────────
  {
    const fs = []; for (let s = 4; s < W.endS - 3; s += 7) fs.push(s);
    const poles = new THREE.InstancedMesh(new THREE.CylinderGeometry(0.08, 0.08, 6.5, 6), new THREE.MeshLambertMaterial({ color: 0xffffff }), fs.length);
    const fg = new THREE.PlaneGeometry(1.8, 1.1); fg.translate(0.9, 0, 0);
    const flags = new THREE.InstancedMesh(fg, new THREE.MeshLambertMaterial({ side: THREE.DoubleSide }), fs.length);
    const tips = [];
    fs.forEach((s, i) => { const q = P(s, HW + 0.9, 0), y = trackY(s) - 1.6; put(poles, i, q.x, y + 3.25, q.z); flags.setColorAt(i, srgb(cols[i % 6])); tips.push([q.x, y + 6.4, q.z, s]); });
    poles.instanceMatrix.needsUpdate = true; G.add(poles, flags);
    const wind = W.winds[0];
    D.anim.push((now, dt, S) => {
      const gust = wind && wind.active(S.ht), warn = wind && wind.warn(S.ht);
      fs.forEach((s, i) => { const q = P(s, HW + 0.9, 0), inW = wind && s >= wind.s0 && s <= wind.s1, sp = inW && (gust || warn) ? 60 : 300; put(flags, i, q.x, trackY(s) - 1.6 + 5.8, q.z, 0, -q.a + Math.PI / 2 + Math.sin(now / sp + i) * (inW && gust ? 0.9 : 0.35), 0); });
      flags.instanceMatrix.needsUpdate = true;
    });
    // 이웃 깃대 꼭대기 사이에 늘어진 줄 + 삼각 깃발
    const tri = [], tcol = [];
    for (let i = 0; i + 1 < tips.length; i++) {
      const [ax, ay, az] = tips[i], [bx, by, bz] = tips[i + 1]; if (Math.hypot(bx - ax, by - ay, bz - az) > 9) continue;
      for (let k = 0; k < 6; k++) {
        const t0 = (k + 0.15) / 6, t1 = (k + 0.85) / 6, sag = t => -Math.sin(Math.PI * t) * 0.9, at = t => [ax + (bx - ax) * t, ay + (by - ay) * t + sag(t), az + (bz - az) * t];
        const p0 = at(t0), p1 = at(t1), tm = (t0 + t1) / 2, pm = at(tm); pm[1] -= 0.75; const c = srgb(cols[(i + k) % 7]);
        tri.push(...p0, ...p1, ...pm); for (let j = 0; j < 3; j++) tcol.push(c.r, c.g, c.b);
      }
    }
    const bg = new THREE.BufferGeometry(); bg.setAttribute('position', new THREE.Float32BufferAttribute(tri, 3)); bg.setAttribute('color', new THREE.Float32BufferAttribute(tcol, 3)); bg.computeVertexNormals();
    G.add(new THREE.Mesh(bg, new THREE.MeshBasicMaterial({ vertexColors: true, side: THREE.DoubleSide })));
  }

  // ── 풍선(160) · 구간 문 위 풍선 다발 — 흔들림은 GPU ───────────────────
  {
    const list = [];
    for (let i = 0; i < 140; i++) { const a = rnd() * TAU, r = RT + HW + 6 + rnd() * 42; list.push([Math.cos(a) * r, -6 + rnd() * (W.topY + 26), Math.sin(a) * r, cols[i % 7], 1]); }
    for (const g of W.gates.concat([{ s: W.finishS, y: W.finishY }])) for (const side of [-1, 1]) for (let k = 0; k < 5; k++) { const q = P(g.s, side * (HW + 0.4) + (rnd() - 0.5) * 1.4, 0); list.push([q.x + (rnd() - 0.5), g.y + 8 + rnd() * 1.8, q.z + (rnd() - 0.5), cols[(k + 2) % 7], 0.75]); }
    const geo = new THREE.SphereGeometry(1, 12, 10); geo.scale(1, 1.2, 1);
    const m = wobble(new THREE.MeshLambertMaterial(), 'float bph = instanceMatrix[3][0] * 0.31 + instanceMatrix[3][2] * 0.17; transformed.y += sin(uTime * 0.7 + bph) * 1.2 / max(0.5, instanceMatrix[1][1]); transformed.x += sin(uTime * 0.5 + bph * 2.0) * 0.25;', 'balloon');
    const bal = new THREE.InstancedMesh(geo, m, list.length);
    const sg = new THREE.CylinderGeometry(0.02, 0.02, 2.6, 3); sg.translate(0, -2.4, 0);
    const str = new THREE.InstancedMesh(sg, wobble(new THREE.MeshBasicMaterial({ color: 0xffffff }), 'float bph = instanceMatrix[3][0] * 0.31 + instanceMatrix[3][2] * 0.17; transformed.y += sin(uTime * 0.7 + bph) * 1.2 / max(0.5, instanceMatrix[1][1]); transformed.x += sin(uTime * 0.5 + bph * 2.0) * 0.25;', 'balloonStr'), list.length);
    list.forEach(([x, y, z, c, s], i) => { put(bal, i, x, y, z, 0, 0, 0, s); put(str, i, x, y, z, 0, 0, 0, s); bal.setColorAt(i, srgb(c)); });
    G.add(bal, str);
  }

  // ── 구름(40 덩이) ─────────────────────────────────────────────────
  {
    const puffs = [];
    for (let i = 0; i < 40; i++) { const a = rnd() * TAU, r = 120 + rnd() * 160, y = 6 + rnd() * 80, n = 3 + (rnd() * 3 | 0), ta = a + Math.PI / 2; for (let j = 0; j < n; j++) puffs.push([Math.cos(a) * r + Math.cos(ta) * j * 4.2, y + rnd() * 2, Math.sin(a) * r + Math.sin(ta) * j * 4.2, 3 + rnd() * 4]); }
    const cl = new THREE.InstancedMesh(new THREE.SphereGeometry(1, 10, 8), new THREE.MeshLambertMaterial({ color: 0xffffff, emissive: 0x5B6B8A }), puffs.length);
    puffs.forEach(([x, y, z, s], i) => put(cl, i, x, y, z, 0, 0, 0, s, s * 0.8, s));
    G.add(cl);
    D.anim.push((now) => { cl.rotation.y = now / 400000; });
  }

  // ── 관중석(합친 모양 하나) · 관중 1,400명(파도 · 껑충) · 관중석 현수막 ──────
  {
    const parts = [], N = 28, banner = [];
    for (let i = 0; i < N; i++) {
      const a = (i + 0.5) / N * TAU;
      for (let k = 0; k < 4; k++) { const r = 150 + k * 6; parts.push([new THREE.BoxGeometry(34, 4, 6), mx(Math.cos(a) * r, -22 + k * 4, Math.sin(a) * r, 0, -a + Math.PI / 2, 0), cols[(i + k) % 6]]); }
      if (i % 2 === 0) banner.push(a);
    }
    G.add(new THREE.Mesh(baked(parts), VC()));
    const people = [];
    for (let i = 0; i < N; i++) for (let k = 0; k < 4; k++) for (let j = 0; j < 12; j++) {
      const a = (i + (j + 0.5) / 12) / N * TAU, r = 150 + k * 6 + (rnd() - 0.5) * 2; if (rnd() < 0.12) continue;
      people.push([Math.cos(a) * r, -20 + k * 4, Math.sin(a) * r, a]);
    }
    const code = 'vec3 ip = vec3(instanceMatrix[3][0], instanceMatrix[3][1], instanceMatrix[3][2]); float ang = atan(ip.z, ip.x); float wv = pow(max(0.0, sin(ang * 3.0 - uTime * 2.4)), 8.0); float hop = max(0.0, sin(uTime * (5.0 + fract(ip.x * 0.37) * 4.0) + ip.z * 0.7)) * 0.25; transformed.y += (hop + wv * 1.1) / max(0.5, instanceMatrix[1][1]);';
    const bodyG = new THREE.BoxGeometry(0.7, 1.1, 0.5); bodyG.translate(0, 0.55, 0);
    const headG = new THREE.IcosahedronGeometry(0.34, 0); headG.translate(0, 1.42, 0);
    const armG = new THREE.BoxGeometry(1.25, 0.16, 0.16); armG.translate(0, 1.55, 0);
    const bodies = new THREE.InstancedMesh(bodyG, wobble(new THREE.MeshLambertMaterial(), code, 'crowd'), people.length);
    const heads = new THREE.InstancedMesh(headG, wobble(new THREE.MeshLambertMaterial(), code, 'crowdH'), people.length);
    const nArm = Math.round(people.length * 0.35), arms = new THREE.InstancedMesh(armG, wobble(new THREE.MeshLambertMaterial(), code + ' transformed.x *= 1.0 + 0.15 * sin(uTime * 9.0 + ip.x);', 'crowdA'), nArm);
    const skins = [0xFFE0C4, 0xF5C79A, 0xD9A066, 0xA86B3C];
    people.forEach(([x, y, z, a], i) => { put(bodies, i, x, y, z, 0, -a + Math.PI / 2, 0, 2.2); put(heads, i, x, y, z, 0, 0, 0, 2.2); bodies.setColorAt(i, srgb(cols[(i * 7) % 8])); heads.setColorAt(i, srgb(skins[i % 4])); if (i < nArm) { const p = people[(i * 3) % people.length]; put(arms, i, p[0], p[1], p[2], 0, -p[3] + Math.PI / 2, 0, 2.2); arms.setColorAt(i, srgb(skins[(i * 3) % 4])); } });
    G.add(bodies, heads, arms);
    // 관중석 뒤 현수막
    const tex = textTex('StepUP  🏃  혈압 주의  🩸  StepUP', '#1166FF', '#FFFFFF', 1024, 128);
    const bgeo = banner.map(a => { const g = new THREE.PlaneGeometry(30, 3.6); g.rotateY(-a - Math.PI / 2); g.translate(Math.cos(a) * 171, -2.5, Math.sin(a) * 171); return g; });
    G.add(new THREE.Mesh(mergeGeometries(bgeo), new THREE.MeshBasicMaterial({ map: tex, side: THREE.DoubleSide })));
  }

  // ── 관람차 ───────────────────────────────────────────────────────
  {
    const a = 0.6, R = 26, cx = Math.cos(a) * 118, cz = Math.sin(a) * 118, cy = R + 4 - 22;
    const frame = new THREE.Group(); frame.position.set(cx, cy, cz); frame.rotation.y = -a + Math.PI / 2; G.add(frame);
    const wheel = new THREE.Group(); frame.add(wheel);
    const parts = [];
    for (const zz of [-1.6, 1.6]) parts.push([new THREE.TorusGeometry(R, 0.45, 6, 48), mx(0, 0, zz), 0xFF4F8B]);
    for (let i = 0; i < 12; i++) { const an = i / 12 * TAU; for (const zz of [-1.6, 1.6]) parts.push([new THREE.CylinderGeometry(0.18, 0.18, R, 5), mx(Math.cos(an) * R / 2, Math.sin(an) * R / 2, zz, 0, 0, an - Math.PI / 2), 0xFFFFFF]); }
    parts.push([new THREE.CylinderGeometry(1.4, 1.4, 4, 16), mx(0, 0, 0, Math.PI / 2, 0, 0), 0xFFD23F]);
    wheel.add(new THREE.Mesh(baked(parts), VC()));
    const legs = [];
    for (const zz of [-3, 3]) for (const sx of [-1, 1]) legs.push([new THREE.CylinderGeometry(0.6, 0.8, cy + 24, 6), mx(sx * 9, -(cy + 22) / 2 + 1, zz, 0, 0, sx * 0.33), 0x1166FF]);
    frame.add(new THREE.Mesh(baked(legs), VC()));
    const cab = new THREE.InstancedMesh(new THREE.BoxGeometry(3, 2.6, 2.6), new THREE.MeshLambertMaterial(), 12);
    for (let i = 0; i < 12; i++) cab.setColorAt(i, srgb(cols[i % 6]));
    frame.add(cab);
    D.anim.push((now) => { const w = now / 9000; wheel.rotation.z = w; for (let i = 0; i < 12; i++) { const an = i / 12 * TAU + w; put(cab, i, Math.cos(an) * R, Math.sin(an) * R - 2, 0); } cab.instanceMatrix.needsUpdate = true; });
  }

  // ── 열기구 9 · 운동화 비행선 ─────────────────────────────────────────
  {
    const stripe = canvasTex(256, 64, (g, w, h) => { for (let i = 0; i < 8; i++) { g.fillStyle = i % 2 ? '#FFFFFF' : '#DDDDDD'; g.fillRect(i * 32, 0, 32, h); } });
    const eg = new THREE.SphereGeometry(4, 16, 12); eg.scale(1, 1.25, 1);
    const env = new THREE.InstancedMesh(eg, new THREE.MeshLambertMaterial({ map: stripe }), 9);
    const bk = new THREE.InstancedMesh(new THREE.BoxGeometry(1.8, 1.4, 1.8), new THREE.MeshLambertMaterial({ color: 0x8B5E34 }), 9);
    const hb = []; for (let i = 0; i < 9; i++) { hb.push({ a: rnd() * TAU, r: 85 + rnd() * 110, y: 10 + rnd() * 60, sp: (rnd() < 0.5 ? -1 : 1) * (0.004 + rnd() * 0.006) }); env.setColorAt(i, srgb(cols[i % 6])); }
    G.add(env, bk);
    D.anim.push((now) => { hb.forEach((h, i) => { const an = h.a + now / 1000 * h.sp, y = h.y + Math.sin(now / 2600 + i) * 2, x = Math.cos(an) * h.r, z = Math.sin(an) * h.r; put(env, i, x, y, z, 0, an, 0); put(bk, i, x, y - 6.2, z, 0, an, 0); }); env.instanceMatrix.needsUpdate = true; bk.instanceMatrix.needsUpdate = true; });
    // 비행선: 큰 운동화 모양 + StepUP 현수막
    const blimp = new THREE.Group(), parts = [];
    parts.push([new THREE.BoxGeometry(16, 4.4, 6), mx(0, 0, 0), 0xFFFFFF]);
    parts.push([new THREE.BoxGeometry(16.4, 1.4, 6.4), mx(0, -2.6, 0), 0x1166FF]);
    parts.push([new THREE.BoxGeometry(6, 3, 5.4), mx(-4.5, 3.5, 0), 0xFFFFFF]);
    parts.push([new THREE.CylinderGeometry(2.9, 2.9, 6, 16, 1, false, 0, Math.PI), mx(8, -0.6, 0, Math.PI / 2, 0, 0), 0xFFFFFF]);
    parts.push([new THREE.BoxGeometry(10, 0.6, 6.15), mx(1, 0.4, 0), 0xFF4F8B]);
    for (let i = 0; i < 4; i++) parts.push([new THREE.TorusGeometry(0.35, 0.1, 5, 10), mx(-1 + i * 2.4, 1.6, 3.05), 0x0B1A33]);
    blimp.add(new THREE.Mesh(baked(parts), VC()));
    const sign = new THREE.Mesh(new THREE.PlaneGeometry(14, 2.2), new THREE.MeshBasicMaterial({ map: textTex('StepUP · 걸으면 오른다', '#0B1A33', '#FFD23F', 1024, 160), side: THREE.DoubleSide }));
    sign.position.set(0, -6, 0); blimp.add(sign);
    const rope = new THREE.Mesh(new THREE.CylinderGeometry(0.05, 0.05, 3, 4), new THREE.MeshBasicMaterial({ color: 0x0B1A33 })); rope.position.set(0, -4.4, 0); blimp.add(rope);
    G.add(blimp);
    D.anim.push((now) => { const an = now / 1000 * 0.025, r = 96; blimp.position.set(Math.cos(an) * r, W.topY + 22 + Math.sin(now / 3000) * 2, Math.sin(an) * r); blimp.rotation.y = -an; });
  }

  // ── 섬: 야자수 · 파라솔 · 큰 운동화 풍선 · 떠 있는 섬 6 · 고무 오리 ─────
  {
    const trunks = [], leaves = [];
    const palm = (x, y, z, h, ry) => { for (let i = 0; i < 5; i++) trunks.push([x + i * 0.35, y + i * h / 5, z, 0.5 - i * 0.05]); for (let k = 0; k < 6; k++) leaves.push([x + 1.75, y + h, z, ry + k / 6 * TAU]); };
    for (let i = 0; i < 18; i++) { const a = rnd() * TAU, r = 52 + rnd() * 24; palm(Math.cos(a) * r, -21, Math.sin(a) * r, 7 + rnd() * 4, rnd() * TAU); }
    const isl = [];
    for (let i = 0; i < 6; i++) { const a = i / 6 * TAU + 0.3, r = 72 + rnd() * 20, y = 4 + i * 6 + rnd() * 4; isl.push([Math.cos(a) * r, y, Math.sin(a) * r]); palm(Math.cos(a) * r - 1, y + 0.6, Math.sin(a) * r, 5 + rnd() * 2, rnd() * TAU); }
    const tr = new THREE.InstancedMesh(new THREE.CylinderGeometry(1, 1.15, 1.6, 6), new THREE.MeshLambertMaterial({ color: 0x8B5E34 }), trunks.length);
    trunks.forEach(([x, y, z, s], i) => put(tr, i, x, y, z, 0, 0, 0.1, s, 1.4, s));
    const lg = new THREE.ConeGeometry(0.9, 4.2, 4); lg.rotateZ(-Math.PI / 2); lg.translate(2.1, 0, 0); lg.scale(1, 0.25, 1);
    const lf = new THREE.InstancedMesh(lg, wobble(new THREE.MeshLambertMaterial({ color: 0x2FBF5B }), 'transformed.y += sin(uTime * 1.6 + instanceMatrix[3][0]) * 0.12 * transformed.x;', 'palm'), leaves.length);
    leaves.forEach(([x, y, z, ry], i) => put(lf, i, x, y, z, 0, ry, -0.35));
    G.add(tr, lf);
    // 떠 있는 섬(거꾸로 된 원뿔 + 풀)
    const rock = new THREE.InstancedMesh(new THREE.ConeGeometry(6, 9, 7), new THREE.MeshLambertMaterial({ color: 0x9C7A54 }), isl.length);
    const turf = new THREE.InstancedMesh(new THREE.CylinderGeometry(6.2, 6.2, 1.2, 14), new THREE.MeshLambertMaterial({ color: 0x7CDB6E }), isl.length);
    isl.forEach(([x, y, z], i) => { put(rock, i, x, y - 4.5, z, Math.PI, 0, 0); put(turf, i, x, y, z); });
    G.add(rock, turf);
    D.anim.push((now) => { isl.forEach(([x, y, z], i) => { const dy = Math.sin(now / 2200 + i) * 0.8; put(rock, i, x, y - 4.5 + dy, z, Math.PI, 0, 0); put(turf, i, x, y + dy, z); }); rock.instanceMatrix.needsUpdate = true; turf.instanceMatrix.needsUpdate = true; });
    // 파라솔
    const um = new THREE.InstancedMesh(new THREE.ConeGeometry(2.4, 1.2, 8), new THREE.MeshLambertMaterial(), 14), up = new THREE.InstancedMesh(new THREE.CylinderGeometry(0.08, 0.08, 3, 4), new THREE.MeshLambertMaterial({ color: 0xffffff }), 14);
    for (let i = 0; i < 14; i++) { const a = rnd() * TAU, r = 63 + rnd() * 6; put(um, i, Math.cos(a) * r, -18.2, Math.sin(a) * r); put(up, i, Math.cos(a) * r, -19.7, Math.sin(a) * r); um.setColorAt(i, srgb(cols[i % 6])); }
    G.add(um, up);
    // 큰 운동화 풍선(섬 위, 출발 쪽)
    const sh = new THREE.Group(), sp = [];
    sp.push([new THREE.BoxGeometry(12, 3.6, 5), mx(0, 0, 0), 0x1166FF]); sp.push([new THREE.BoxGeometry(12.4, 1.2, 5.3), mx(0, -2.3, 0), 0xFFFFFF]);
    sp.push([new THREE.BoxGeometry(5, 3.4, 4.6), mx(-3.4, 3, 0), 0x1166FF]); sp.push([new THREE.BoxGeometry(8, 0.6, 5.12), mx(0.6, 0.2, 0), 0xFFD23F]);
    for (let i = 0; i < 3; i++) sp.push([new THREE.BoxGeometry(0.3, 0.2, 5.15), mx(0.5 + i * 1.6, 1.85, 0), 0xFFFFFF]);
    sh.add(new THREE.Mesh(baked(sp), VC())); sh.position.set(Math.cos(-0.25) * 58, -15, Math.sin(-0.25) * 58); sh.rotation.y = 0.25 + Math.PI / 2; G.add(sh);
    D.anim.push((now) => { sh.position.y = -15 + Math.sin(now / 900) * 0.6; sh.rotation.z = Math.sin(now / 1300) * 0.06; });
    // 고무 오리
    const dk = 16, db = new THREE.InstancedMesh(new THREE.SphereGeometry(1.4, 10, 8), new THREE.MeshLambertMaterial({ color: 0xFFD23F }), dk * 2), beak = new THREE.InstancedMesh(new THREE.ConeGeometry(0.35, 0.9, 6), new THREE.MeshLambertMaterial({ color: 0xFF9F1C }), dk);
    const ducks = []; for (let i = 0; i < dk; i++) { const a = rnd() * TAU, r = 90 + rnd() * 50; ducks.push([Math.cos(a) * r, Math.sin(a) * r, rnd() * TAU]); }
    G.add(db, beak);
    D.anim.push((now) => { ducks.forEach(([x, z, ry], i) => { const y = -23.4 + Math.sin(now / 700 + i) * 0.25, rr = ry + Math.sin(now / 2500 + i) * 0.4; put(db, i * 2, x, y, z, 0, rr, 0, 1, 0.8, 1.2); const hx = x + Math.sin(rr) * -1.2, hz = z + Math.cos(rr) * -1.2; put(db, i * 2 + 1, hx, y + 1.5, hz, 0, rr, 0, 0.62); put(beak, i, hx + Math.sin(rr) * -0.95, y + 1.4, hz + Math.cos(rr) * -0.95, -Math.PI / 2, rr, 0); }); db.instanceMatrix.needsUpdate = true; beak.instanceMatrix.needsUpdate = true; });
  }

  // ── 바다 물결(섬 둘레) · 흰 물거품 고리 ───────────────────────────────
  {
    const wg = new THREE.RingGeometry(76, 240, 96, 10); wg.rotateX(-Math.PI / 2);
    const wm = new THREE.MeshLambertMaterial({ color: 0x3A9BFF, emissive: 0x0A3A9A, transparent: true, opacity: 0.85 });
    wm.onBeforeCompile = sh => { sh.uniforms.uTime = UT; sh.vertexShader = 'uniform float uTime;\n' + sh.vertexShader.replace('#include <begin_vertex>', '#include <begin_vertex>\nfloat rr = length(transformed.xz); transformed.y += sin(rr * 0.35 - uTime * 1.6) * 0.45 + sin(transformed.x * 0.08 + uTime * 0.9) * 0.35;'); };
    wm.customProgramCacheKey = () => 'waves';
    const wave = new THREE.Mesh(wg, wm); wave.position.y = -23.6; G.add(wave);
    const ftex = canvasTex(256, 16, (g, w, h) => { g.fillStyle = 'rgba(255,255,255,0)'; g.fillRect(0, 0, w, h); for (let i = 0; i < 18; i++) { g.fillStyle = 'rgba(255,255,255,.85)'; g.beginPath(); g.ellipse((i + 0.5) * w / 18, h / 2, 6 + (i % 3) * 3, 5, 0, 0, TAU); g.fill(); } });
    ftex.wrapS = THREE.RepeatWrapping; ftex.repeat.set(24, 1);
    const fg = new THREE.RingGeometry(73.5, 78, 96, 1); fg.rotateX(-Math.PI / 2);
    // 고리 uv 를 둘레 방향으로
    const uv = fg.attributes.uv, pz = fg.attributes.position; for (let i = 0; i < uv.count; i++) { const x = pz.getX(i), z = pz.getZ(i); uv.setXY(i, (Math.atan2(z, x) / TAU + 0.5), Math.hypot(x, z) > 75.7 ? 1 : 0); }
    const foam = new THREE.Mesh(fg, new THREE.MeshBasicMaterial({ map: ftex, transparent: true, depthWrite: false })); foam.position.y = -23.35; G.add(foam);
    D.anim.push((now) => { ftex.offset.x = now / 9000; });
  }

  // ── 금별(탑 둘레를 돈다) ─────────────────────────────────────────────
  {
    const shape = new THREE.Shape(); for (let i = 0; i < 10; i++) { const r = i % 2 ? 0.45 : 1, a = Math.PI / 2 + i * Math.PI / 5; i ? shape.lineTo(Math.cos(a) * r, Math.sin(a) * r) : shape.moveTo(Math.cos(a) * r, Math.sin(a) * r); }
    const sg = new THREE.ExtrudeGeometry(shape, { depth: 0.3, bevelEnabled: false }); sg.center();
    const n = 90, st = new THREE.InstancedMesh(sg, new THREE.MeshLambertMaterial({ color: 0xFFC400, emissive: 0x6A4A00 }), n), sd = [];
    for (let i = 0; i < n; i++) sd.push([rnd() * TAU, RT + HW + 4 + rnd() * 16, -2 + rnd() * (W.topY + 20), 0.6 + rnd() * 0.8, rnd() * TAU]);
    G.add(st);
    D.anim.push((now) => { const t = now / 1000; sd.forEach(([a, r, y, s, ph], i) => { const an = a + t * 0.03; put(st, i, Math.cos(an) * r, y + Math.sin(t + ph) * 0.6, Math.sin(an) * r, 0, t * 1.6 + ph, 0, s); }); st.instanceMatrix.needsUpdate = true; });
  }

  // ── 기둥: 도는 네온 고리 · 구간마다 큰 소품 ───────────────────────────
  {
    const ringY = []; for (let y = 3; y < W.topY + 12; y += 18) ringY.push(y);
    const rg = ringY.map(y => { const g = new THREE.TorusGeometry(PILLAR + 0.1, 0.22, 6, 96); g.rotateX(Math.PI / 2); g.translate(0, y, 0); return g; });
    const neon = new THREE.Mesh(mergeGeometries(rg), new THREE.MeshBasicMaterial({ color: 0x24D8FF })); G.add(neon);
    D.anim.push((now) => { neon.material.color.setHSL((now / 6000) % 1, 0.85, 0.6); });
    const mount = (s, build) => { const y = trackY(s) + 9.5, a = s / RT, g = new THREE.Group(); g.position.set(Math.cos(a) * (PILLAR + 0.6), y, Math.sin(a) * (PILLAR + 0.6)); g.rotation.y = -a + Math.PI / 2; build(g); G.add(g); return g; };
    const mid = i => (W.secs[i].s + (i + 1 < W.secs.length ? W.secs[i + 1].s : W.finishS)) / 2;
    // 1 운동화
    mount(mid(0) - 18, g => { const p = []; p.push([new THREE.BoxGeometry(5, 1.6, 2.2), mx(0, 0, 0.6), 0xFF4F8B]); p.push([new THREE.BoxGeometry(5.2, 0.5, 2.3), mx(0, -1, 0.6), 0xFFFFFF]); p.push([new THREE.BoxGeometry(2.2, 1.5, 2), mx(-1.4, 1.3, 0.6), 0xFF4F8B]); g.add(new THREE.Mesh(baked(p), VC())); });
    // 2 권투 장갑
    const glove = mount(mid(1), g => { const p = []; p.push([new THREE.SphereGeometry(1.8, 14, 10), mx(0, 0, 1.4, 0, 0, 0, 1, 1, 1.15), 0xE11D48]); p.push([new THREE.SphereGeometry(0.8, 10, 8), mx(1.4, 0.6, 1.3), 0xE11D48]); p.push([new THREE.CylinderGeometry(1.1, 1.2, 1.6, 12), mx(0, -2.2, 1.2), 0xFFFFFF]); g.add(new THREE.Mesh(baked(p), VC())); });
    D.anim.push((now) => { glove.children[0].position.z = Math.max(0, Math.sin(now / 400)) * 1.2; });
    // 3 큰 초시계
    const watch = mount(mid(2), g => { const p = []; p.push([new THREE.CylinderGeometry(2.4, 2.4, 0.6, 24), mx(0, 0, 0.5, Math.PI / 2, 0, 0), 0xFFFFFF]); p.push([new THREE.TorusGeometry(2.4, 0.3, 6, 24), mx(0, 0, 0.85), 0xFF9F1C]); p.push([new THREE.CylinderGeometry(0.4, 0.4, 0.8, 10), mx(0, 2.9, 0.5), 0xFF9F1C]); g.add(new THREE.Mesh(baked(p), VC())); const hand = new THREE.Mesh(new THREE.BoxGeometry(0.18, 2, 0.1), new THREE.MeshBasicMaterial({ color: 0x0B1A33 })); hand.geometry.translate(0, 0.9, 0); hand.position.z = 0.9; g.add(hand); g.userData.hand = hand; });
    D.anim.push((now) => { watch.userData.hand.rotation.z = -now / 1000 * TAU / 6; });
    // 4 수정 결정
    const crys = mount(mid(3), g => { const p = []; for (let i = 0; i < 6; i++) p.push([new THREE.OctahedronGeometry(0.9 + (i % 3) * 0.4, 0), mx((i - 2.5) * 1.1, (i % 2) * 0.9, 1, 0, i, 0, 0.8, 1.8, 0.8), [0x24D8FF, 0xA77BFF, 0x7CFFB2][i % 3]]); g.add(new THREE.Mesh(baked(p), VC({ emissive: 0x112244 }))); });
    D.anim.push((now) => { crys.children[0].rotation.y = Math.sin(now / 1500) * 0.3; });
    // 5 트로피
    mount(mid(4) - 10, g => { const p = []; p.push([new THREE.CylinderGeometry(1.8, 0.6, 2.6, 16), mx(0, 1, 1.2), 0xFFC400]); p.push([new THREE.CylinderGeometry(0.3, 0.3, 1.2, 8), mx(0, -0.9, 1.2), 0xFFC400]); p.push([new THREE.BoxGeometry(2.2, 0.7, 1.6), mx(0, -1.8, 1.2), 0x1166FF]); for (const sx of [-1, 1]) p.push([new THREE.TorusGeometry(0.7, 0.18, 6, 12, Math.PI), mx(sx * 1.8, 1.4, 1.2, 0, 0, sx * Math.PI / 2), 0xFFC400]); g.add(new THREE.Mesh(baked(p), VC({ emissive: 0x3A2A00 }))); });
  }

  // ── 해 · 먼 산 ────────────────────────────────────────────────────
  {
    const glowTex = canvasTex(128, 128, (g, w, h) => { const gr = g.createRadialGradient(64, 64, 0, 64, 64, 64); gr.addColorStop(0, 'rgba(255,255,255,1)'); gr.addColorStop(0.25, 'rgba(255,246,200,.95)'); gr.addColorStop(0.5, 'rgba(255,220,120,.35)'); gr.addColorStop(1, 'rgba(255,220,120,0)'); g.fillStyle = gr; g.fillRect(0, 0, w, h); });
    const sun = new THREE.Sprite(new THREE.SpriteMaterial({ map: glowTex, transparent: true, depthWrite: false, fog: false })); sun.scale.set(110, 110, 1); sun.position.set(-260, 190, -300); G.add(sun);
    const mg = [], mc = [];
    for (let i = 0; i < 40; i++) { const a = i / 40 * TAU, b = (i + 1) / 40 * TAU, r = 380, h = 25 + Math.abs(Math.sin(i * 1.7)) * 45 + (i % 3) * 8; mg.push(Math.cos(a) * r, -24, Math.sin(a) * r, Math.cos(b) * r, -24, Math.sin(b) * r, Math.cos((a + b) / 2) * r, h, Math.sin((a + b) / 2) * r); const c = srgb(i % 2 ? 0x7FA8D8 : 0x8FB8E2); for (let j = 0; j < 3; j++) mc.push(c.r, c.g, c.b); }
    const mgeo = new THREE.BufferGeometry(); mgeo.setAttribute('position', new THREE.Float32BufferAttribute(mg, 3)); mgeo.setAttribute('color', new THREE.Float32BufferAttribute(mc, 3));
    G.add(new THREE.Mesh(mgeo, new THREE.MeshBasicMaterial({ vertexColors: true, side: THREE.DoubleSide, fog: false })));
  }

  D.tick = (now, dt, S) => { for (const f of D.anim) f(now, dt, S); };
  return D;
}
