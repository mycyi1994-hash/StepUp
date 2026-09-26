// stepupcrew.com 소개 페이지 — 다섯 화면을 한 장씩 넘기는 전체화면 사이트.
// 화면 속 숫자·이름·코스·SUP 는 모두 예시이고, 각 카드에 "화면 예시" 배지가 붙어 있다.
const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];
const pad2 = n => String(n).padStart(2, '0');
const mmss = s => pad2(Math.floor(s / 60)) + ':' + pad2(Math.floor(s % 60));

const SLUGS = ['run', 'crew', 'reward', 'course', 'start'];
const DUR = 1100;        // 트랙 전환 시간
const THRESHOLD = 90;    // 휠 누적 임계값
const START_SECONDS = 1458;
const KM_SECONDS = 298;  // 1km 에 걸리는 예시 시간(4'58")

const root = $('.root');
const track = $('.track');
const pages = $$('.page');
const dotButtons = $$('.dots button');
const vh = window.CSS && CSS.supports('height', '100svh') ? 'svh' : 'vh';

let page = 0;
let lockUntil = 0;
let timers = [];
let rafs = {};

function later(ms, fn) { const t = setTimeout(fn, ms); timers.push(t); return t; }
function every(ms, fn) { const t = setInterval(fn, ms); timers.push(t); return t; }
function frame(key, fn) { rafs[key] = requestAnimationFrame(fn); }
function stopFrame(key) { cancelAnimationFrame(rafs[key]); delete rafs[key]; }
function clearAll() {
  timers.forEach(t => { clearTimeout(t); clearInterval(t); });
  timers = [];
  Object.keys(rafs).forEach(stopFrame);
}

// ── 사운드 (기본 끔, Web Audio 로 합성 — 음원 파일 없음) ─────────────
let soundOn = false;
let ac = null;
function audio() {
  if (!ac) {
    const AC = window.AudioContext || window.webkitAudioContext;
    if (!AC) return null;
    ac = new AC();
  }
  if (ac.state === 'suspended') ac.resume();
  return ac;
}
function snd(type) {
  if (!soundOn) return;
  const ctx = audio();
  if (!ctx) return;
  const t = ctx.currentTime;
  const tone = (f, dur, vol, wave, when = 0, f2) => {
    const o = ctx.createOscillator(), g = ctx.createGain();
    o.type = wave || 'sine';
    o.frequency.setValueAtTime(f, t + when);
    if (f2) o.frequency.exponentialRampToValueAtTime(f2, t + when + dur);
    g.gain.setValueAtTime(vol, t + when);
    g.gain.exponentialRampToValueAtTime(0.0001, t + when + dur);
    o.connect(g).connect(ctx.destination);
    o.start(t + when);
    o.stop(t + when + dur + 0.02);
  };
  const noise = (dur, vol, freq) => {
    const b = ctx.createBuffer(1, ctx.sampleRate * dur, ctx.sampleRate), d = b.getChannelData(0);
    for (let i = 0; i < d.length; i++) d[i] = (Math.random() * 2 - 1) * (1 - i / d.length);
    const s = ctx.createBufferSource(), f = ctx.createBiquadFilter(), g = ctx.createGain();
    s.buffer = b; f.type = 'bandpass'; f.frequency.value = freq; g.gain.value = vol;
    s.connect(f).connect(g).connect(ctx.destination);
    s.start(t);
  };
  if (type === 'step') tone(90, 0.12, 0.35, 'sine', 0, 45);
  if (type === 'step2') tone(80, 0.12, 0.3, 'sine', 0, 40);
  if (type === 'ping') { tone(880, 0.25, 0.12, 'triangle'); tone(1320, 0.3, 0.06, 'sine', 0.06); }
  if (type === 'chime') { tone(660, 0.4, 0.1, 'triangle'); tone(990, 0.5, 0.08, 'triangle', 0.1); }
  if (type === 'reveal') { tone(523, 0.6, 0.1, 'triangle'); tone(784, 0.6, 0.08, 'triangle', 0.08); tone(1046, 0.8, 0.07, 'sine', 0.16); }
  if (type === 'tick') tone(1500, 0.05, 0.06, 'square');
  if (type === 'beep') tone(1046, 0.18, 0.12, 'square');
  if (type === 'whoosh') noise(0.45, 0.25, 900);
}
const soundBtn = $('.sound');
soundBtn.addEventListener('click', () => {
  soundOn = !soundOn;
  if (soundOn) audio();
  soundBtn.setAttribute('aria-pressed', String(soundOn));
  $('.sound-label', soundBtn).textContent = soundOn ? '사운드 켜짐' : '사운드';
});

// ── 01 러닝: 스톱워치 ─────────────────────────────────────────
const swStart = performance.now() - START_SECONDS * 1000;
const run = {
  main: $('.watch-main'), cs: $('.watch-cs'), km: $('.run-km'), bpm: $('.run-bpm'),
  bar: $('.run-bar'), laps: $('.laps'), glow: $('.run-glow'), inner: $('.run-inner')
};
let lastS = -1;
let lapN = 0;
let laps = [];
function runTick(now) {
  const ms = now - swStart, s = ms / 1000, km = s / KM_SECONDS;
  run.main.textContent = mmss(s);
  run.cs.textContent = '.' + pad2(Math.floor((ms % 1000) / 10));
  run.km.textContent = km.toFixed(2);
  run.bar.style.width = ((km % 5) / 5 * 100) + '%';
  if (Math.floor(s) !== lastS) {
    lastS = Math.floor(s);
    run.bpm.textContent = 164 + Math.round(Math.sin(s / 7) * 4 + Math.random() * 2);
  }
  frame('run', runTick);
}
$('.watch').addEventListener('click', () => {
  const s = (performance.now() - swStart) / 1000;
  laps = [{ n: ++lapN, t: mmss(s), km: (s / KM_SECONDS).toFixed(2) }, ...laps].slice(0, 3);
  snd('tick');
  run.laps.replaceChildren(...laps.map(l => {
    const chip = document.createElement('span');
    chip.className = 'lap';
    chip.textContent = `LAP ${l.n} · ${l.t} · ${l.km}KM`;
    return chip;
  }));
});
window.addEventListener('mousemove', e => {
  if (page !== 0) return;
  const r = run.inner.getBoundingClientRect();
  run.glow.style.transform = `translate(${e.clientX - r.left}px,${e.clientY - r.top}px)`;
});
function startRun() {
  lastS = -1;
  frame('run', runTick);
  let alt = false;
  every(350, () => { if (soundOn) { alt = !alt; snd(alt ? 'step' : 'step2'); } });
}
const heroVideo = root.dataset.heroVideo;
if (heroVideo) {
  const v = document.createElement('video');
  Object.assign(v, { src: heroVideo, autoplay: true, muted: true, loop: true, playsInline: true, className: 'run-video' });
  v.setAttribute('aria-hidden', 'true');
  $('.run-pan').replaceWith(v);
}

// ── 02 크루: 러너 여섯 명이 모임 장소로 ──────────────────────────
const RUNNERS = [
  { name: '지우', dist: '1.2km', pace: "5'12\"", sx: 14, sy: 18, dx: 0, dy: -48 },
  { name: '민재', dist: '2.4km', pace: "4'48\"", sx: 86, sy: 14, dx: 42, dy: -24 },
  { name: '서연', dist: '0.8km', pace: "5'35\"", sx: 90, sy: 72, dx: 42, dy: 24 },
  { name: '도윤', dist: '1.9km', pace: "5'02\"", sx: 18, sy: 88, dx: 0, dy: 48 },
  { name: '하린', dist: '3.1km', pace: "6'10\"", sx: 6, sy: 52, dx: -42, dy: 24 },
  { name: '태오', dist: '1.5km', pace: "5'20\"", sx: 70, sy: 94, dx: -42, dy: -24 }
];
const SVG_NS = 'http://www.w3.org/2000/svg';
const crew = {
  card: $('.crew-card'), lines: $('.crew-lines'), runners: $('.crew-runners'), roster: $('.roster'),
  num: $('.crew-num'), status: $('.crew-status'), headline: $('.crew-headline'), items: []
};
RUNNERS.forEach(r => {
  const line = document.createElementNS(SVG_NS, 'line');
  line.setAttribute('x1', r.sx); line.setAttribute('y1', r.sy);
  line.setAttribute('x2', 50); line.setAttribute('y2', 55);
  line.setAttribute('vector-effect', 'non-scaling-stroke');
  crew.lines.append(line);

  const dot = document.createElement('div');
  dot.className = 'runner';
  dot.textContent = r.name[0];
  const tip = document.createElement('span');
  tip.className = 'runner-tip';
  tip.textContent = `${r.name} · 페이스 ${r.pace}`;
  dot.append(tip);
  crew.runners.append(dot);

  const row = document.createElement('div');
  row.className = 'roster-row';
  row.innerHTML = '<span class="roster-name"><i></i></span><span class="roster-st"></span>';
  row.firstChild.append(r.name);
  crew.roster.append(row);

  crew.items.push({ r, line, dot, row, st: row.lastChild });
});
function renderCrew(joined) {
  crew.items.forEach(({ r, line, dot, row, st }, i) => {
    const j = joined > i;
    dot.style.left = j ? `calc(50% + ${r.dx}px)` : r.sx + '%';
    dot.style.top = j ? `calc(55% + ${r.dy}px)` : r.sy + '%';
    dot.classList.toggle('is-joined', j);
    line.classList.toggle('is-joined', j);
    row.classList.toggle('is-joined', j);
    st.textContent = j ? '합류 완료' : `이동 중 · ${r.dist}`;
  });
  const done = joined >= RUNNERS.length;
  crew.card.classList.toggle('is-done', done);
  crew.num.textContent = pad2(joined);
  crew.status.textContent = (done ? '크루 결성 완료' : '크루 모집 중') + ' · 한강 나이트 러너스';
  crew.headline.textContent = done ? '크루 결성 완료. 이제 같이 달린다.' : '같이 뛸 사람들이 모이고 있다.';
}
function startCrew() {
  let joined = 0;
  later(DUR * 0.7, () => {
    const t = every(900, () => {
      if (joined >= RUNNERS.length) { clearInterval(t); return; }
      joined++;
      snd('ping');
      renderCrew(joined);
    });
  });
}

// ── 03 보상: 러닝 완료 → SUP → 신발 ───────────────────────────────
const reward = { card: $('.reward-card'), sup: $('.sup-num') };
function setPhase(ph) {
  const c = reward.card.classList;
  [0, 1, 2, 3].forEach(i => c.toggle('ph' + i, ph >= i));
  c.toggle('flash-on', ph === 2);
}
function startReward() {
  const d = DUR * 0.6;
  later(d, () => setPhase(0));
  later(d + 700, () => {
    setPhase(1);
    snd('chime');
    const t0 = performance.now(), to = 12.5;
    const step = now => {
      const k = Math.min(1, (now - t0) / 1300), e = 1 - Math.pow(1 - k, 3);
      reward.sup.textContent = (to * e).toFixed(2);
      if (k < 1) frame('sup', step);
    };
    frame('sup', step);
  });
  later(d + 2100, () => { setPhase(2); snd('reveal'); });
  later(d + 2900, () => setPhase(3));
}

// ── 04 코스: 세 코스 캐러셀 + 폰 속 GPS 경로 ─────────────────────────
function route(seed, pts) {
  // 기준점 사이를 8등분하고 조금씩 흔든다(시드 고정). 실제 GPX 가 생기면 그 좌표로 바꾼다.
  let s = seed;
  const rnd = () => (s = (s * 9301 + 49297) % 233280) / 233280;
  const out = [];
  for (let i = 0; i < pts.length - 1; i++) {
    const [x1, y1] = pts[i], [x2, y2] = pts[i + 1];
    for (let k = 0; k < 8; k++) {
      const t = k / 8;
      out.push([x1 + (x2 - x1) * t + (rnd() - 0.5) * 7, y1 + (y2 - y1) * t + (rnd() - 0.5) * 7]);
    }
  }
  out.push(pts[pts.length - 1]);
  return out;
}
const COURSES = [
  { label: 'SUNSET 5K', name: '광안리 해변 코스', desc: '해변 산책로를 따라 광안대교를 보며 달리는 평지 코스', km: '5.02', up: '12', level: '쉬움', time: '26:14', pace: "5'13\"", mapT: 'rotate(0deg)', r: route(3, [[40, 60], [120, 110], [180, 150], [250, 250]]) },
  { label: 'NIGHT 7K', name: '민락 수변 나이트 코스', desc: '조명이 켜진 수변공원과 방파제를 잇는 야간 코스', km: '7.18', up: '24', level: '보통', time: '38:02', pace: "5'18\"", mapT: 'rotate(38deg) scale(1.1)', r: route(11, [[60, 270], [90, 180], [160, 150], [200, 80], [260, 60]]) },
  { label: 'RAIN 10K', name: '비 오는 날 해안 코스', desc: '비 오는 날에도 미끄럽지 않은 포장로 위주의 장거리 코스', km: '10.04', up: '41', level: '어려움', time: '55:40', pace: "5'33\"", mapT: 'rotate(-62deg) scale(1.15)', r: route(27, [[50, 90], [110, 70], [170, 120], [150, 200], [220, 240], [260, 290]]) }
];
const slidesEl = $('.ph-slides');
COURSES.forEach((c, i) => {
  const pts = c.r.map(q => q[0].toFixed(1) + ',' + q[1].toFixed(1)).join(' ');
  const [sx, sy] = c.r[0], [ex, ey] = c.r[c.r.length - 1];
  const slide = document.createElement('div');
  slide.className = 'ph-slide' + (i === 0 ? ' is-active' : '');
  slide.innerHTML =
    `<div class="ph-map"><img src="./assets/img/crew-map.webp" alt="" loading="lazy" style="transform:${c.mapT}">` +
    `<svg viewBox="0 0 300 330" preserveAspectRatio="xMidYMid slice">` +
    `<polyline class="route route-out" points="${pts}" pathLength="1"></polyline>` +
    `<polyline class="route route-in" points="${pts}" pathLength="1"></polyline>` +
    `<circle cx="${sx}" cy="${sy}" r="6" fill="#FFFFFF" stroke="#2F6BFF" stroke-width="3"></circle>` +
    `<circle class="route-end" cx="${ex}" cy="${ey}" r="8" fill="none" stroke="#FFFFFF" stroke-width="3"></circle>` +
    `</svg><div class="ph-map-fade"></div></div>` +
    `<div class="ph-info"><span class="ph-name"></span>` +
    `<span class="ph-km">${c.km}<span class="u">KM</span></span>` +
    `<div class="ph-grid"><div><span>시간</span><span>${c.time}</span></div><div><span>페이스</span><span>${c.pace}</span></div><div><span>상승</span><span>${c.up}m</span></div></div>` +
    `<div class="ph-go">이 코스 달리기</div></div>`;
  $('.ph-name', slide).textContent = c.name;
  slidesEl.append(slide);
});
const course = {
  bgs: $$('.c-bg'), labels: $$('.c-label'), segs: $$('.c-seg'), slides: $$('.ph-slide'),
  idx: $('.c-idx'), name: $('.c-name'), desc: $('.c-desc'), km: $('.c-km'), up: $('.c-up'), level: $('.c-level')
};
let ci = 0;
let courseTimer = null;
function renderCourse() {
  const onC = page === 3, c = COURSES[ci];
  course.bgs.forEach((el, i) => el.classList.toggle('is-active', i === ci));
  course.labels.forEach((el, i) => el.classList.toggle('is-active', i === ci));
  course.slides.forEach((el, i) => el.classList.toggle('is-active', i === ci));
  // 경로 그리기·진행 막대는 처음 상태(0)에서 다시 시작해야 하므로, 한 번 지운 뒤 스타일을 확정하고 다시 붙인다.
  [...course.slides, ...course.segs].forEach(el => el.classList.remove('is-anim'));
  if (!onC) return fillCourseText(c);
  void slidesEl.offsetWidth;
  course.slides[ci].classList.add('is-anim');
  course.segs[ci].classList.add('is-anim');
  fillCourseText(c);
}
function fillCourseText(c) {
  course.idx.textContent = '0' + (ci + 1);
  course.name.textContent = c.name;
  course.desc.textContent = c.desc;
  course.km.textContent = c.km;
  course.up.textContent = c.up;
  course.level.textContent = c.level;
}
function autoCourses() {
  clearInterval(courseTimer);
  courseTimer = every(5000, () => { snd('tick'); ci = (ci + 1) % 3; renderCourse(); });
}
function stepCourse(d) {
  if (page !== 3) return;
  snd('tick');
  ci = (ci + d + 3) % 3;
  renderCourse();
  autoCourses();
}
$$('[data-course]').forEach(b => b.addEventListener('click', () => stepCourse(Number(b.dataset.course))));
{
  let px = null, py = null;
  const card = $('.course-card');
  card.addEventListener('pointerdown', e => { px = e.clientX; py = e.clientY; });
  card.addEventListener('pointerup', e => {
    if (px === null) return;
    const dx = e.clientX - px, dy = e.clientY - py;
    if (Math.abs(dx) > 60 && Math.abs(dx) > Math.abs(dy)) stepCourse(dx < 0 ? 1 : -1);
    px = null;
  });
  card.addEventListener('pointercancel', () => { px = null; });
}

// ── 05 시작: 폰 START → 러닝 ──────────────────────────────────────
const phone = { el: $('.start-phone'), t: $('.sp-time'), k: $('.sp-km-v') };
function setP6(p) {
  phone.el.classList.toggle('p6-1', p === 1);
  phone.el.classList.toggle('p6-2', p >= 2);
}
function startPhone() {
  const d = DUR * 0.6;
  phone.t.textContent = '00:00';
  phone.k.textContent = '0.00';
  later(d + 1500, () => { setP6(1); snd('beep'); });
  later(d + 1750, () => {
    setP6(2);
    const t0 = performance.now();
    const step = now => {
      const s = (now - t0) / 1000;
      phone.t.textContent = mmss(s);
      phone.k.textContent = (s / KM_SECONDS).toFixed(2);
      frame('phone', step);
    };
    frame('phone', step);
  });
}

// ── 페이지 전환 ─────────────────────────────────────────────────
function resetPages() {
  renderCrew(0);
  setPhase(-1);
  reward.sup.textContent = '0.00';
  ci = 0;
  setP6(-1);
}
function enter(n) {
  clearAll();
  resetPages();
  page = n;
  pages.forEach((p, i) => p.classList.toggle('is-active', i === n));
  dotButtons.forEach((b, i) => b.setAttribute('aria-current', String(i === n)));
  renderCourse();
  if (n === 0) startRun();
  if (n === 1) startCrew();
  if (n === 2) startReward();
  if (n === 3) autoCourses();
  if (n === 4) startPhone();
}
function to(n, { force = false, instant = false } = {}) {
  n = Math.max(0, Math.min(SLUGS.length - 1, n));
  const now = performance.now();
  if (n === page || (!force && now < lockUntil)) return;
  lockUntil = now + DUR + 150;
  inertia = true;
  acc = 0;
  try { history.replaceState(null, '', '#' + SLUGS[n]); } catch (e) { /* file:// 등 */ }
  if (!instant) snd('whoosh');
  if (instant) {
    track.style.transition = 'none';
    track.style.transform = `translate3d(0,-${n * 100}${vh},0)`;
    void track.offsetHeight;
    track.style.transition = '';
  } else {
    track.style.transform = `translate3d(0,-${n * 100}${vh},0)`;
  }
  root.scrollTop = 0;
  enter(n);
}
const go = d => to(page + d);

// 휠: 누적 임계값 + 트랙패드 관성 무시(잠금이 풀린 뒤에도 180ms 안쪽으로 이어지는 입력은 버린다)
let acc = 0, lastWheel = 0, inertia = false, accReset = null;
window.addEventListener('wheel', e => {
  if (e.ctrlKey) return; // 핀치 확대는 브라우저에 맡긴다
  e.preventDefault();
  const now = performance.now(), gap = now - lastWheel;
  lastWheel = now;
  if (now < lockUntil) return;
  if (inertia) { if (gap < 180) return; inertia = false; }
  acc += e.deltaY;
  clearTimeout(accReset);
  accReset = setTimeout(() => { acc = 0; }, 220);
  if (acc > THRESHOLD) go(1); else if (acc < -THRESHOLD) go(-1);
}, { passive: false });

window.addEventListener('keydown', e => {
  if (e.altKey || e.ctrlKey || e.metaKey) return;
  const onControl = e.target.closest && e.target.closest('a,button,input,textarea,select');
  if (['ArrowDown', 'PageDown'].includes(e.key) || (e.key === ' ' && !onControl)) { e.preventDefault(); go(1); }
  if (['ArrowUp', 'PageUp'].includes(e.key)) { e.preventDefault(); go(-1); }
  if (page === 3 && (e.key === 'ArrowLeft' || e.key === 'ArrowRight') && !onControl) stepCourse(e.key === 'ArrowRight' ? 1 : -1);
});

let touchY = null;
window.addEventListener('touchstart', e => { touchY = e.touches[0].clientY; }, { passive: true });
window.addEventListener('touchend', e => {
  if (touchY === null) return;
  const dy = touchY - e.changedTouches[0].clientY;
  touchY = null;
  if (Math.abs(dy) > 50) go(dy > 0 ? 1 : -1);
}, { passive: true });

$$('[data-go]').forEach(b => b.addEventListener('click', e => { e.preventDefault(); to(Number(b.dataset.go), { force: true }); }));

// Tab 으로 다른 화면의 버튼에 닿으면 그 화면으로 넘긴다(보이지 않는 곳에 포커스가 머물지 않게).
track.addEventListener('focusin', e => {
  const i = pages.findIndex(p => p.contains(e.target));
  if (i >= 0 && i !== page) to(i, { force: true });
  root.scrollTop = 0;
});
root.addEventListener('scroll', () => { root.scrollTop = 0; });

window.addEventListener('hashchange', () => {
  const i = SLUGS.indexOf(location.hash.slice(1));
  if (i >= 0) to(i, { force: true });
});

// 다운로드 추적
$$('[data-dl]').forEach(a => a.addEventListener('click', () => {
  (window.dataLayer = window.dataLayer || []).push({ event: 'apk_download', screen: SLUGS[page] });
}));

// ── 시작 ───────────────────────────────────────────────────────
const i0 = SLUGS.indexOf(location.hash.slice(1));
enter(0);
if (i0 > 0) to(i0, { force: true, instant: true });

// 첫 방문 로더: 폰트 + 첫 화면 이미지 + 최소 800ms
const loader = $('.loader');
if (document.documentElement.classList.contains('seen')) {
  loader.remove();
} else {
  const img = new Image();
  const imgReady = new Promise(r => { img.onload = img.onerror = r; });
  img.src = './assets/img/run-bg.webp';
  Promise.all([document.fonts ? document.fonts.ready : 0, imgReady, new Promise(r => setTimeout(r, 800))]).then(() => {
    loader.classList.add('is-out');
    setTimeout(() => loader.remove(), 500);
    try { localStorage.setItem('stepupHero.seen', '1'); } catch (e) { /* 저장 불가 브라우저 */ }
  });
}
