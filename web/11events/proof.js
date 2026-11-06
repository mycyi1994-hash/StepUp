/* 11.11 워킹데이 걸음 인증 제출 — 서버 walkingday_proof_submit(supabase/migrations/0090_walkingday.sql) */
(function () {
  'use strict';
  var CFG = window.WALKINGDAY || {};
  var URL_BASE = CFG.url || 'https://pupjzcmybuoyhzfwrsdf.supabase.co';
  var KEY = CFG.key || '';
  var DAY0 = 20764; // 2026-11-07(KST)
  var START = Date.parse('2026-11-07T00:00:00+09:00');
  var CLOSE = Date.parse('2026-11-11T15:00:00+09:00');
  var MAX_CHARS = 580000;
  var $ = function (id) { return document.getElementById(id); };

  var form = $('form'), msg = $('msg'), submit = $('submit'), image = null;

  // 들어온 주소의 ref · utm_* 는 등록 링크와 안내 링크에 그대로 넘긴다(이벤트 페이지와 같다)
  try {
    var qs = new URLSearchParams(location.search), keep = [];
    qs.forEach(function (v, k) { if (v && (k === 'ref' || /^utm_/i.test(k))) keep.push([k, v]); });
    if (keep.length) ['joinLink', 'back'].forEach(function (id) {
      var a = $(id), u = new URL(a.href); keep.forEach(function (p) { u.searchParams.set(p[0], p[1]); }); a.href = u.toString();
    });
  } catch (e) { /* 기본 주소 */ }

  function today() { return Math.floor((Date.now() + 9 * 3600e3) / 86400e3); }

  // 걸은 날 — 오늘(한국)까지만 고를 수 있다
  var names = ['토', '일', '월', '화', '수'];
  var days = $('days');
  for (var i = 0; i < 5; i++) {
    var d = DAY0 + i, l = document.createElement('label');
    if (i === 4) l.className = 'gold';
    l.innerHTML = '<input type="radio" name="day" value="' + d + '"><span>11/' + (7 + i) + '<small>' + names[i] + (i === 4 ? ' · 2배' : '') + '</small></span>';
    if (d > today()) l.querySelector('input').disabled = true;
    days.appendChild(l);
  }
  var pickDefault = Math.min(Math.max(today(), DAY0), DAY0 + 4);
  var def = days.querySelector('input[value="' + pickDefault + '"]');
  if (def && !def.disabled) def.checked = true;

  var state = $('state');
  function closed() { return Date.now() < START || Date.now() >= CLOSE; }
  if (Date.now() < START) { state.hidden = false; state.textContent = '인증은 11/7(토) 0시부터 받아요. 지금은 사전 등록만 할 수 있어요.'; submit.disabled = true; }
  else if (Date.now() >= CLOSE) { state.hidden = false; state.textContent = '인증이 마감됐어요(11/11 15:00). 발표는 11/11(수) 18시예요.'; submit.disabled = true; }

  // 고친 칸의 안내는 바로 지운다
  [['email', 'emailErr'], ['steps', 'stepsErr'], ['consent', 'consentErr']].forEach(function (p) {
    $(p[0]).addEventListener(p[0] === 'consent' ? 'change' : 'input', function () { setErr(p[1], ''); });
  });
  days.addEventListener('change', function () { setErr('dayErr', ''); });

  try { var saved = localStorage.getItem('wd_email'); if (saved) $('email').value = saved; } catch (e) { /* 없음 */ }

  // 캡처 → 긴 변 1600px 안의 JPEG 글(서버 한도 안으로 줄인다)
  function shrink(file) {
    return new Promise(function (resolve, reject) {
      var url = URL.createObjectURL(file), img = new Image();
      img.onload = function () {
        URL.revokeObjectURL(url);
        var tries = [[1600, .72], [1400, .6], [1100, .55], [900, .5]];
        for (var t = 0; t < tries.length; t++) {
          var s = Math.min(1, tries[t][0] / Math.max(img.naturalWidth, img.naturalHeight));
          var c = document.createElement('canvas');
          c.width = Math.max(1, Math.round(img.naturalWidth * s)); c.height = Math.max(1, Math.round(img.naturalHeight * s));
          var g = c.getContext('2d'); g.fillStyle = '#fff'; g.fillRect(0, 0, c.width, c.height); g.drawImage(img, 0, 0, c.width, c.height);
          var data = c.toDataURL('image/jpeg', tries[t][1]);
          if (data.length <= MAX_CHARS && data.indexOf('data:image/jpeg;base64,') === 0) return resolve(data);
        }
        reject(new Error('big'));
      };
      img.onerror = function () { URL.revokeObjectURL(url); reject(new Error('read')); };
      img.src = url;
    });
  }

  $('file').addEventListener('change', function () {
    var f = this.files && this.files[0], pick = $('pick');
    setErr('fileErr', '');
    image = null;
    if (!f) return;
    pick.classList.remove('has'); pick.innerHTML = '<span>캡처를 읽는 중…</span>';
    shrink(f).then(function (data) {
      image = data;
      pick.classList.add('has'); pick.innerHTML = '';
      var im = document.createElement('img'); im.src = data; im.alt = '고른 걸음 캡처'; pick.appendChild(im);
    }, function () {
      pick.innerHTML = '<span>다른 캡처 고르기</span>';
      setErr('fileErr', '이 사진은 읽지 못했어요. 화면 캡처(PNG · JPG)를 골라 주세요.');
    });
  });

  function setErr(id, text) { $(id).textContent = text; }
  function say(text, err, html) { msg.className = 'msg' + (err ? ' err' : ''); if (html) msg.innerHTML = text; else msg.textContent = text; }

  var REASONS = {
    closed: '지금은 인증을 받지 않아요. 기간은 11/7 0시 ~ 11/11 15:00예요.',
    busy: '잠시 뒤 다시 시도해 주세요.',
    consent: '동의에 체크해 주세요.',
    email: '이메일 주소를 다시 확인해 주세요.',
    not_registered: '사전 등록한 이메일이 아니에요. <a href="' + $('joinLink').getAttribute('href') + '">먼저 사전 등록</a>한 뒤 같은 이메일로 인증해 주세요.',
    day: '걸은 날을 다시 골라 주세요. 오늘 이후 날짜는 고를 수 없어요.',
    steps: '걸음 수를 숫자로 적어 주세요.',
    image: '캡처를 다시 골라 주세요.',
    approved: '이 날은 이미 승인됐어요. 다른 날을 골라 주세요.'
  };

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    ['emailErr', 'dayErr', 'stepsErr', 'fileErr', 'consentErr'].forEach(function (id) { setErr(id, ''); });
    say('');
    var email = $('email').value.trim().toLowerCase();
    var dayEl = days.querySelector('input:checked');
    var steps = parseInt(String($('steps').value).replace(/[^\d]/g, ''), 10);
    var bad = false;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { setErr('emailErr', '사전 등록한 이메일을 적어 주세요.'); bad = true; }
    if (!dayEl) { setErr('dayErr', '걸은 날을 골라 주세요.'); bad = true; }
    if (!(steps >= 1 && steps <= 100000)) { setErr('stepsErr', '걸음 수를 적어 주세요.'); bad = true; }
    if (!image) { setErr('fileErr', '걸음 캡처를 골라 주세요.'); bad = true; }
    if (!$('consent').checked) { setErr('consentErr', '동의에 체크해 주세요.'); bad = true; }
    if (bad) return;

    submit.disabled = true; submit.textContent = '보내는 중…';
    fetch(URL_BASE + '/rest/v1/rpc/walkingday_proof_submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', apikey: KEY },
      body: JSON.stringify({ p_email: email, p_day: +dayEl.value, p_steps: steps, p_image: image, p_consent: true, p_trap: $('trap').value }),
      cache: 'no-store'
    }).then(function (r) { return r.ok ? r.json() : Promise.reject(new Error('http ' + r.status)); })
      .then(function (d) {
        if (d && d.ok) {
          try { localStorage.setItem('wd_email', email); } catch (e) { /* 없음 */ }
          var day = 7 + (+dayEl.value - DAY0);
          $('doneText').textContent = '11/' + day + ' · ' + steps.toLocaleString('ko-KR') + '걸음. 담당자가 확인하면 응모권에 반영돼요.';
          form.hidden = true; $('done').hidden = false; window.scrollTo(0, 0);
        } else {
          say(REASONS[d && d.code] || '보내지 못했어요. 잠시 뒤 다시 시도해 주세요.', true, true);
        }
      })
      .catch(function () { say('연결이 끊겼어요. 잠시 뒤 다시 시도해 주세요.', true); })
      .then(function () { submit.disabled = closed(); submit.textContent = '인증 제출하기'; });
  });

  $('again').addEventListener('click', function () {
    image = null; $('file').value = ''; $('steps').value = '';
    var pick = $('pick'); pick.classList.remove('has'); pick.innerHTML = '<span>날짜와 걸음 수가 보이는 화면 캡처 고르기</span>';
    $('done').hidden = true; form.hidden = false; say('');
  });
})();
