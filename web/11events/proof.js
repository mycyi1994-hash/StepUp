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

  /* 이메일 한 번만(11/8 「인증할 때마다 등록한 이메일 또 쓰는 게 불편」): 이 기기에 남은 이메일로 세 탭을 채운다.
     wd_email = 인증 · SNS · 초대 링크를 낸 이메일, 또는 홈페이지에서 사전 등록한 이메일(waitlist.js).
     stepup.waitlist.email = 같은 탭에서 방금 사전 등록하고 넘어온 경우(sessionStorage) */
  var EMAIL_IDS = ['email', 'snsEmail', 'invEmail'];
  function savedEmail() {
    var v = '';
    try { v = localStorage.getItem('wd_email') || ''; } catch (e) { /* 없음 */ }
    if (!v) { try { v = sessionStorage.getItem('stepup.waitlist.email') || ''; } catch (e) { /* 없음 */ } }
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v) ? v : '';
  }
  function rememberEmail(v) {
    try { localStorage.setItem('wd_email', v); } catch (e) { /* 없음 */ }
    EMAIL_IDS.forEach(function (id) { var el = $(id); if (el && !el.value.trim()) el.value = v; });
  }
  // 한 탭에 적은 이메일은 비어 있는 다른 탭에도 그대로
  EMAIL_IDS.forEach(function (id) {
    $(id).addEventListener('change', function () {
      var v = this.value.trim().toLowerCase();
      if (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v)) EMAIL_IDS.forEach(function (o) { var el = $(o); if (o !== id && !el.value.trim()) el.value = v; });
    });
  });
  var saved = savedEmail();
  if (saved) EMAIL_IDS.forEach(function (id) { $(id).value = saved; });
  if (saved) $('emailHint').insertAdjacentText('afterbegin', '지난번 이메일을 불러왔어요. ');

  /* 휴대폰 번호(11/9 「세 탭 모두 전화번호」 — 서버 0100 walkingday_contacts). 서버 economy.walkingday_phone 과 같은 규칙:
     010 · 011 · 016 · 017 · 018 · 019 휴대폰만, +82 · 하이픈 · 띄어쓰기는 받고 010-1234-5678 모양으로 맞춘다.
     이메일처럼 이 기기에 남겨(wd_phone) 세 탭을 채운다 */
  var PHONE_IDS = ['phone', 'snsPhone', 'invPhone'];
  function phoneNorm(v) {
    var d = String(v || '').replace(/[^\d]/g, '');
    if (/^82/.test(d) && d.length >= 11 && d.length <= 12) d = '0' + d.slice(2);
    if (/^01[016789]\d{8}$/.test(d)) return d.slice(0, 3) + '-' + d.slice(3, 7) + '-' + d.slice(7);
    if (/^01[16789]\d{7}$/.test(d)) return d.slice(0, 3) + '-' + d.slice(3, 6) + '-' + d.slice(6);
    return '';
  }
  function rememberPhone(v) {
    try { localStorage.setItem('wd_phone', v); } catch (e) { /* 없음 */ }
    PHONE_IDS.forEach(function (id) { var el = $(id); if (el && !el.value.trim()) el.value = v; });
  }
  PHONE_IDS.forEach(function (id) {
    $(id).addEventListener('input', function () { setErr(id + 'Err', ''); });
    $(id).addEventListener('change', function () {
      var v = phoneNorm(this.value);
      if (v) { this.value = v; PHONE_IDS.forEach(function (o) { var el = $(o); if (o !== id && !el.value.trim()) el.value = v; }); }
    });
  });
  var savedPhone = '';
  try { savedPhone = phoneNorm(localStorage.getItem('wd_phone')); } catch (e) { /* 없음 */ }
  if (savedPhone) PHONE_IDS.forEach(function (id) { $(id).value = savedPhone; });
  var PHONE_ERR = '휴대폰 번호를 적어 주세요. 예: 010-1234-5678';

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
    approved: '이 날은 담당자가 이미 확인했어요. 다른 날을 골라 주세요.',
    phone: '휴대폰 번호를 다시 확인해 주세요. 예: 010-1234-5678'
  };

  form.addEventListener('submit', function (e) {
    e.preventDefault();
    ['emailErr', 'phoneErr', 'dayErr', 'stepsErr', 'fileErr', 'consentErr'].forEach(function (id) { setErr(id, ''); });
    say('');
    var email = $('email').value.trim().toLowerCase(), phone = phoneNorm($('phone').value);
    var dayEl = days.querySelector('input:checked');
    var steps = parseInt(String($('steps').value).replace(/[^\d]/g, ''), 10);
    var bad = false;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { setErr('emailErr', '사전 등록한 이메일을 적어 주세요.'); bad = true; }
    if (!phone) { setErr('phoneErr', PHONE_ERR); bad = true; }
    if (!dayEl) { setErr('dayErr', '걸은 날을 골라 주세요.'); bad = true; }
    if (!(steps >= 1 && steps <= 100000)) { setErr('stepsErr', '걸음 수를 적어 주세요.'); bad = true; }
    if (!image) { setErr('fileErr', '걸음 캡처를 골라 주세요.'); bad = true; }
    if (!$('consent').checked) { setErr('consentErr', '동의에 체크해 주세요.'); bad = true; }
    if (bad) return;

    submit.disabled = true; submit.textContent = '보내는 중…';
    fetch(URL_BASE + '/rest/v1/rpc/walkingday_proof_submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', apikey: KEY },
      body: JSON.stringify({ p_email: email, p_day: +dayEl.value, p_steps: steps, p_image: image, p_consent: true, p_trap: $('trap').value, p_phone: phone }),
      cache: 'no-store'
    }).then(function (r) { return r.ok ? r.json() : Promise.reject(new Error('http ' + r.status)); })
      .then(function (d) {
        if (d && d.ok) {
          rememberEmail(email); rememberPhone(phone);
          var day = 7 + (+dayEl.value - DAY0);
          $('doneText').textContent = '11/' + day + ' · ' + steps.toLocaleString('ko-KR') + '걸음. ' + (steps >= 1111 ? (d.status === 'approved' ? '응모권에 바로 반영됐어요.' : '담당자가 확인하면 응모권에 반영돼요.') : '1,111걸음보다 적어 응모권은 없지만, 다 같이 걸은 걸음에는 더해져요.');
          form.hidden = true; $('done').hidden = false; window.scrollTo(0, 0);
          if (window.wdTrack) window.wdTrack('walkingday_proof_submit', { day: day });
        } else {
          say(REASONS[d && d.code] || '보내지 못했어요. 잠시 뒤 다시 시도해 주세요.', true, true);
        }
      })
      .catch(function () { say('연결이 끊겼어요. 잠시 뒤 다시 시도해 주세요.', true); })
      .then(function () { submit.disabled = closed(); submit.textContent = '인증 제출하기'; });
  });

  $('again').addEventListener('click', function () {
    image = null; $('file').value = ''; $('steps').value = ''; $('stepsLow').hidden = true;
    var pick = $('pick'); pick.classList.remove('has'); pick.innerHTML = '<span>날짜와 걸음 수가 보이는 화면 캡처 고르기</span>';
    $('done').hidden = true; form.hidden = false; say('');
  });

  /* ── SNS 게시물 링크 제출 — 서버 walkingday_sns_submit(0093) · 친구 초대 링크 walkingday_invite_link(0094). #sns · #invite 로 들어오면 그 칸부터 ── */
  var H = {
    steps: ['걸음 인증하기', '11/7~11/11 하루 1,111걸음 이상. 아무 걷기 앱 캡처면 돼요.'],
    sns: ['SNS 게시물 제출', '해시태그 4개 넣어 올린 게시물 링크를 내면 응모권 1장. 걷지 않아도 돼요.'],
    invite: ['친구 초대', '내 링크로 친구가 등록하고 걸음을 인증하면 둘 다 응모권 2장.']
  };
  var PANES = { steps: ['tabSteps', 'stepsPane'], sns: ['tabSns', 'snsPane'], invite: ['tabInv', 'invPane'] };
  function hashMode() { return location.hash === '#sns' ? 'sns' : location.hash === '#invite' ? 'invite' : 'steps'; }
  function mode(m, push) {
    Object.keys(PANES).forEach(function (k) { $(PANES[k][0]).setAttribute('aria-selected', String(k === m)); $(PANES[k][1]).hidden = k !== m; });
    $('h1').textContent = H[m][0]; $('lede').textContent = H[m][1];
    document.title = H[m][0] + ' · 11.11 워킹데이 | StepUp';
    if (push) { try { history.replaceState(null, '', m === 'steps' ? location.pathname + location.search : location.pathname + location.search + '#' + m); } catch (e) { /* 없음 */ } }
  }
  $('tabSteps').addEventListener('click', function () { mode('steps', true); });
  $('tabSns').addEventListener('click', function () { mode('sns', true); });
  $('tabInv').addEventListener('click', function () { mode('invite', true); });
  window.addEventListener('hashchange', function () { mode(hashMode()); });
  mode(hashMode());

  // 1,111걸음보다 적으면 바로 알림(제출은 막지 않음 — 다 같이 걸은 걸음에는 더해진다)
  $('steps').addEventListener('input', function () {
    var n = parseInt(String(this.value).replace(/[^\d]/g, ''), 10);
    $('stepsLow').hidden = !(n >= 1 && n < 1111);
  });

  var snsForm = $('snsForm'), snsSubmit = $('snsSubmit'), snsMsg = $('snsMsg');
  if (closed()) snsSubmit.disabled = true;

  // 링크 → 채널. 계정 아이디가 링크에 없으면(인스타 · x.com/i/status) 아이디 칸을 연다 — 서버 economy.walkingday_sns_parse 와 같은 규칙
  function linkInfo(v) {
    v = String(v || '').trim().replace(/[?#].*$/, '').replace(/\/+$/, '');
    if (v && !/^https?:\/\//i.test(v)) v = 'https://' + v;
    var m = v.match(/^https?:\/\/(?:www\.|mobile\.|m\.)?(?:x|twitter)\.com\/([A-Za-z0-9_]{1,15})\/status\/\d{5,25}(?:\/(?:photo|video)\/\d+)?$/i);
    if (m && m[1].toLowerCase() !== 'i') return { ch: 'x', acct: true };
    if (/^https?:\/\/(?:www\.|mobile\.|m\.)?(?:x|twitter)\.com\/i\/(?:web\/)?status\/\d{5,25}(?:\/(?:photo|video)\/\d+)?$/i.test(v)) return { ch: 'x', acct: false };
    if (/^https?:\/\/(?:www\.|m\.)?instagram\.com\/(?:[A-Za-z0-9._]{1,30}\/)?(?:p|reel|reels|tv)\/[A-Za-z0-9_-]{5,64}$/i.test(v)) return { ch: 'instagram', acct: false };
    if (/^https?:\/\/(?:www\.)?threads\.(?:net|com)\/@[A-Za-z0-9._]{1,30}\/post\/[A-Za-z0-9_-]{5,64}(?:\/media)?$/i.test(v)) return { ch: 'threads', acct: true };
    return null;
  }
  function cleanAcct(v) { return String(v || '').trim().replace(/^(https?:\/\/)?(www\.)?(instagram\.com|threads\.(net|com)|x\.com)\//i, '').replace(/^@|\/+$/g, '').toLowerCase(); }
  function syncAcct() { var i = linkInfo($('snsUrl').value); $('acctCard').hidden = !(i && !i.acct); }
  $('snsUrl').addEventListener('input', function () { setErr('snsUrlErr', ''); syncAcct(); });
  [['snsEmail', 'snsEmailErr'], ['snsAcct', 'snsAcctErr'], ['snsConsent', 'snsConsentErr']].forEach(function (p) {
    $(p[0]).addEventListener(p[0] === 'snsConsent' ? 'change' : 'input', function () { setErr(p[1], ''); });
  });
  function saySns(text, err) { snsMsg.className = 'msg' + (err ? ' err' : ''); snsMsg.textContent = text; }

  var SNS_REASONS = {
    closed: '지금은 제출을 받지 않아요. 기간은 11/7 0시 ~ 11/11 15:00예요.',
    busy: '잠시 뒤 다시 시도해 주세요.',
    consent: '동의에 체크해 주세요.',
    email: '이메일 주소를 다시 확인해 주세요.',
    url: 'X · 인스타 · 스레드 게시물 링크가 아니에요. 게시물의 공유 → 링크 복사로 받은 주소를 넣어 주세요.',
    account: '계정 아이디를 영문 · 숫자 · 밑줄 · 점으로 적어 주세요.',
    phone: '휴대폰 번호를 다시 확인해 주세요. 예: 010-1234-5678',
    duplicate: '이미 제출된 게시물이에요. 새 게시물을 올려 주세요.',
    day_limit: '오늘은 3개를 다 냈어요. 내일 다시 낼 수 있어요.',
    limit: '한 사람 15개를 다 냈어요.'
  };
  var CHN = { x: 'X', instagram: '인스타', threads: '스레드' };

  snsForm.addEventListener('submit', function (e) {
    e.preventDefault();
    ['snsEmailErr', 'snsPhoneErr', 'snsUrlErr', 'snsAcctErr', 'snsConsentErr'].forEach(function (id) { setErr(id, ''); });
    saySns('');
    var email = $('snsEmail').value.trim().toLowerCase(), url = $('snsUrl').value.trim(), info = linkInfo(url), acct = cleanAcct($('snsAcct').value), phone = phoneNorm($('snsPhone').value), bad = false;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { setErr('snsEmailErr', '이메일을 적어 주세요.'); bad = true; }
    if (!phone) { setErr('snsPhoneErr', PHONE_ERR); bad = true; }
    if (!info) { setErr('snsUrlErr', 'X · 인스타 · 스레드 게시물 링크를 붙여 넣어 주세요.'); bad = true; }
    else if (!info.acct && !/^[a-z0-9._]{1,30}$/.test(acct)) { setErr('snsAcctErr', '계정 아이디를 적어 주세요.'); bad = true; }
    if (!$('snsConsent').checked) { setErr('snsConsentErr', '동의에 체크해 주세요.'); bad = true; }
    if (bad) return;

    snsSubmit.disabled = true; snsSubmit.textContent = '보내는 중…';
    fetch(URL_BASE + '/rest/v1/rpc/walkingday_sns_submit', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', apikey: KEY },
      body: JSON.stringify({ p_email: email, p_url: url, p_account: info.acct ? '' : acct, p_consent: true, p_trap: $('snsTrap').value, p_phone: phone }),
      cache: 'no-store'
    }).then(function (r) { return r.ok ? r.json() : Promise.reject(new Error('http ' + r.status)); })
      .then(function (d) {
        if (d && d.ok) {
          rememberEmail(email); rememberPhone(phone);
          $('snsDoneText').textContent = (d.channel ? CHN[d.channel] + ' @' + d.account + ' · ' : '') + (d.today ? '오늘 ' + d.today + '/3개. ' : '') + (d.status === 'approved' ? '응모권에 바로 반영됐어요.' : '담당자가 확인하면 응모권에 반영돼요.');
          snsForm.hidden = true; $('snsDone').hidden = false; window.scrollTo(0, 0);
          if (window.wdTrack) window.wdTrack('walkingday_sns_submit', { channel: d.channel || '' });
        } else {
          saySns(SNS_REASONS[d && d.code] || '보내지 못했어요. 잠시 뒤 다시 시도해 주세요.', true);
        }
      })
      .catch(function () { saySns('연결이 끊겼어요. 잠시 뒤 다시 시도해 주세요.', true); })
      .then(function () { snsSubmit.disabled = closed(); snsSubmit.textContent = '게시물 제출하기'; });
  });

  $('snsAgain').addEventListener('click', function () {
    $('snsUrl').value = ''; $('snsAcct').value = ''; syncAcct();
    $('snsDone').hidden = true; snsForm.hidden = false; saySns('');
  });

  /* ── 친구 초대: 사전 등록한 이메일 → 내 코드 · 초대 현황(0094). 링크는 이벤트 페이지로(이벤트 페이지가 ref 를 사전 등록 링크에 넘긴다) ── */
  var invForm = $('invForm'), invSubmit = $('invSubmit'), invMsg = $('invMsg'), invUrl = '';
  $('invEmail').addEventListener('input', function () { setErr('invEmailErr', ''); });
  $('invConsent').addEventListener('change', function () { setErr('invConsentErr', ''); });
  function sayInv(html, err) { invMsg.className = 'msg' + (err ? ' err' : ''); invMsg.innerHTML = html; }
  var INV_REASONS = {
    busy: '잠시 뒤 다시 시도해 주세요.',
    email: '이메일 주소를 다시 확인해 주세요.',
    phone: '휴대폰 번호를 다시 확인해 주세요. 예: 010-1234-5678',
    consent: '동의에 체크해 주세요.',
    not_registered: '사전 등록한 이메일이 아니에요. <a href="' + $('invJoin').getAttribute('href') + '">먼저 사전 등록</a>하면 바로 내 링크가 나와요.'
  };
  invForm.addEventListener('submit', function (e) {
    e.preventDefault();
    ['invEmailErr', 'invPhoneErr', 'invConsentErr'].forEach(function (id) { setErr(id, ''); }); sayInv('');
    var email = $('invEmail').value.trim().toLowerCase(), phone = phoneNorm($('invPhone').value), bad = false;
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { setErr('invEmailErr', '사전 등록한 이메일을 적어 주세요.'); bad = true; }
    if (!phone) { setErr('invPhoneErr', PHONE_ERR); bad = true; }
    if (!$('invConsent').checked) { setErr('invConsentErr', '동의에 체크해 주세요.'); bad = true; }
    if (bad) return;
    invSubmit.disabled = true; invSubmit.textContent = '찾는 중…';
    fetch(URL_BASE + '/rest/v1/rpc/walkingday_invite_link', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', apikey: KEY },
      body: JSON.stringify({ p_email: email, p_trap: $('invTrap').value, p_phone: phone, p_consent: true }),
      cache: 'no-store'
    }).then(function (r) { return r.ok ? r.json() : Promise.reject(new Error('http ' + r.status)); })
      .then(function (d) {
        if (d && d.ok && d.code) {
          rememberEmail(email); rememberPhone(phone);
          invUrl = 'https://stepupcrew.com/11events/?ref=' + encodeURIComponent(d.code) + '&utm_source=invite&utm_medium=web&utm_campaign=2611-11111steps';
          $('invLink').textContent = invUrl;
          $('invN').textContent = (+d.invited || 0).toLocaleString('ko-KR');
          $('invOk').textContent = (+d.counted || 0).toLocaleString('ko-KR');
          $('invFb').textContent = '';
          invForm.hidden = true; $('invDone').hidden = false;
          invRankRead(+d.counted || 0);
          if (window.wdTrack) window.wdTrack('walkingday_invite_link', {});
        } else {
          sayInv(INV_REASONS[d && d.code] || '찾지 못했어요. 잠시 뒤 다시 시도해 주세요.', true);
        }
      })
      .catch(function () { sayInv('연결이 끊겼어요. 잠시 뒤 다시 시도해 주세요.', true); })
      .then(function () { invSubmit.disabled = false; invSubmit.textContent = '내 초대 링크 보기'; });
  });
  /* 친구도 걸음 인증까지 해야 둘 다 2장 — 등록만으로는 안 센다 */
  var INV_TEXT = '같이 걷고 빼빼로 받자 🍫 내 링크로 사전 등록하고 11/11 15시까지 하루 1,111걸음 캡처만 올리면 우리 둘 다 응모권 2장 더!';
  /* 초대 탭 경품 카드: 실제 순위로 「몇 명이면 1위」 */
  fetch(URL_BASE + '/rest/v1/rpc/walkingday_leaderboard', { method: 'POST', headers: { 'Content-Type': 'application/json', apikey: KEY }, body: '{}', cache: 'no-store' })
    .then(function (r) { return r.ok ? r.json() : null; })
    .then(function (d) {
      if (!d || !d.invite || Date.now() >= Date.parse('2026-11-11T15:00:00+09:00')) return;
      var top = d.invite.length ? d.invite[0].friends : 0, el = document.querySelector('[data-race]');
      el.textContent = '인증한 친구 ' + (top + 1) + '명이면 지금 1위 · 마감 11/11(수) 15:00'; el.hidden = false;
    })
    .catch(function () { /* 못 읽으면 줄을 숨긴 채로 */ });
  /* 내 자리까지 남은 친구 수 — 공개 순위(0096)와 조회 때 받은 내 인증 친구 수로 조회 때 한 번만 센다(walkingday_invite_link 는 횟수 제한이 있어 되풀이하지 않는다) */
  function invRankText(c, rows) {
    var P = '요가매트 세트', top = rows.length ? +rows[0].friends : 0, full = rows.length >= 11, cut = full ? +rows[10].friends : 0;
    if (c > 0 && (c > top || (c === top && !(rows[1] && +rows[1].friends === top)))) return '지금 초대 1위예요. ' + P + ' 자리를 지켜 주세요';
    if (c > 0 && c === top) return '1위와 동점이에요. 친구 1명 더면 ' + P + ' 단독 1위';
    if (full && c > 0 && c === cut) return '경품 순위 끝자리와 동점이에요. 친구 1명 더면 경품 순위 안';
    var toTop = top - c + 1, toIn = full ? (c > cut ? 0 : cut - c + 1) : (c > 0 ? 0 : 1);
    if (toIn >= toTop) return '친구 ' + toTop + '명 더면 ' + P;
    return (toIn > 0 ? '친구 ' + toIn + '명 더면 경품 순위 · ' : '지금 경품 순위 안이에요 · ') + toTop + '명 더면 ' + P;
  }
  function invRankRead(c) {
    var el = $('invRank');
    el.hidden = true;
    if (Date.now() >= CLOSE) return;
    fetch(URL_BASE + '/rest/v1/rpc/walkingday_leaderboard', { method: 'POST', headers: { 'Content-Type': 'application/json', apikey: KEY }, body: '{}', cache: 'no-store' })
      .then(function (r) { return r.ok ? r.json() : Promise.reject(new Error('http ' + r.status)); })
      .then(function (d) { if (d && d.invite) { el.textContent = invRankText(c, d.invite); el.hidden = false; } })
      .catch(function () { /* 못 읽으면 줄을 두지 않는다 */ });
  }
  function copyText(t) {
    if (navigator.clipboard && navigator.clipboard.writeText) return navigator.clipboard.writeText(t);
    return new Promise(function (ok, no) {
      var a = document.createElement('textarea'); a.value = t; a.setAttribute('readonly', ''); a.style.position = 'fixed'; a.style.opacity = '0';
      document.body.appendChild(a); a.select();
      try { document.execCommand('copy') ? ok() : no(new Error('copy')); } catch (e) { no(e); } finally { a.remove(); }
    });
  }
  $('invCopy').addEventListener('click', function () {
    copyText(INV_TEXT + '\n' + invUrl).then(function () { $('invFb').className = 'msg'; $('invFb').textContent = '초대 문구와 링크를 복사했어요. 카톡 · 텔레그램에 붙여 넣어 주세요.'; },
      function () { $('invFb').className = 'msg err'; $('invFb').textContent = '복사가 안 돼요. 위 링크를 길게 눌러 복사해 주세요.'; });
  });
  $('invShare').addEventListener('click', function () {
    if (navigator.share) { navigator.share({ title: '11.11 StepUp 워킹데이', text: INV_TEXT, url: invUrl }).catch(function () { /* 닫음 */ }); return; }
    copyText(INV_TEXT + '\n' + invUrl).then(function () { $('invFb').className = 'msg'; $('invFb').textContent = '초대 문구와 링크를 복사했어요. 카톡 · 텔레그램에 붙여 넣어 주세요.'; },
      function () { $('invFb').className = 'msg err'; $('invFb').textContent = '복사가 안 돼요. 위 링크를 길게 눌러 복사해 주세요.'; });
  });
  $('invOther').addEventListener('click', function () { $('invDone').hidden = true; $('invRank').hidden = true; invForm.hidden = false; sayInv(''); });
})();
