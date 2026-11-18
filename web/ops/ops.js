/* StepUp 운영자 페이지 — 아이템 지급 · 보상 이벤트(서버 0106 · 보안 0114) · 바로 적용 · 경제 현황 · 설정(0116).
   운영자 키는 SQL 로 만든다: select economy.ops_key_new('담당자 이름', 30);  -- 날 수(1 ~ 365, 한 칸으로 부르면 30일)
   막기: update economy.ops_keys set revoked_at = now() where label = '…';
   키는 로그인(ops_login)할 때 한 번만 보내고 어디에도 남기지 않는다. 이 탭의 세션 저장소에는 서버가 준 세션 번호만 둔다
   (8시간까지 · 30분 동안 쓰지 않으면 끝 — 서버가 정한다). 틀린 로그인이 잦으면 서버가 잠시 막는다.
   큰 일(전체 · 200명 넘는 지급 · 3배 이상이거나 48시간 넘는 이벤트 · 되돌리기)은 그 요청에 키를 한 번 더 넣는다(p_confirm) —
   기준은 서버가 ops_overview 의 confirm 으로 알려 주고, 서버가 다시 확인한다. 하루 지급 한도도 서버가 센다(cap).
   아이템 이름 · 설명은 여기(운영자용 한국어)와 앱 문구 · 푸시 문구(supabase/functions/push-send)에 따로 있다 — 종류를 더하면 셋을 함께. */
(function () {
  'use strict';
  // 공개값(web/waitlist.js · 11events/assets/config.js 와 같다)
  var URL_BASE = 'https://pupjzcmybuoyhzfwrsdf.supabase.co';
  var KEY = 'sb_publishable_jt74AKM32zdqnJlsFHEo2g_MNHa-WRO';
  var STORE = 'stepup.ops.session';
  // 예전(0106) 페이지는 키 자체를 세션 저장소에 두었다 — 남아 있으면 지운다
  try { sessionStorage.removeItem('stepup.ops.key'); } catch (e) { /* 저장소가 막혀 있음 */ }

  var ITEMS = {
    RUN_BOOSTER: { name: '러닝 부스터', desc: function (v) { return '고른 러닝 1회 적립 ×' + v; }, when: '출발 전에 고름' },
    REWARD_DOUBLE: { name: '보상 2배권', desc: function (v) { return '고른 러닝 1회 적립 ×' + v; }, when: '출발 전에 고름' },
    ENERGY_DRINK: { name: '에너지 드링크', desc: function (v) { return '고른 러닝 1회 에너지를 ' + Math.round(v * 100) + '%만 씀'; }, when: '출발 전에 고름' },
    DURABILITY_RESET: { name: '내구도 초기화권', desc: function () { return '신은 신발 내구도를 가득'; }, when: '바로 사용' },
    ENERGY_RESET: { name: '하루 할당량 초기화권', desc: function () { return '오늘 쓴 에너지를 되돌려 가득'; }, when: '바로 사용' }
  };
  var TARGETS = { EMAILS: '이메일', ALL: '전체', RUNNERS_7D: '최근 7일 달린 사람' };

  var $ = function (id) { return document.getElementById(id); };
  var token = '';          // 세션 번호(ops_login 이 준 것) — 운영자 키가 아니다
  var state = null;        // ops_overview 결과
  // 큰 일의 기준(서버 ops_overview.confirm 이 덮어쓴다) — 서버가 늘 다시 확인하므로 여기 값은 미리 묻기 위한 것뿐
  var limits = { recipients: 200, mult: 3, hours: 48 };
  var idleMin = 30, sessionEnds = 0, lastUse = 0;
  // 같은 지급 · 같은 이벤트를 다시 보내도 한 번만 되게 — 번호는 보낸 내용에 묶는다({ id, sig }).
  // 답을 못 받았으면(끊김 · 5xx) 그대로 남겨, 화면을 다시 그리거나 다른 탭을 다녀와도 같은 내용이면 같은 번호로 다시 보낸다.
  // 내용이 달라지면 새 번호, 서버가 분명히 답하면(됐다 · 안 된다) 비운다. 다시 확인용 키는 내용이 아니다(번호에 묶지 않는다).
  var grantRequest = null;
  var eventRequest = null;
  function requestFor(pending, parts) {
    var sig = JSON.stringify(parts);
    return pending && pending.sig === sig ? pending : { id: uuid(), sig: sig };
  }
  var busy = false;

  function uuid() {
    if (window.crypto && crypto.randomUUID) return crypto.randomUUID();
    var b = new Uint8Array(16); crypto.getRandomValues(b);
    b[6] = (b[6] & 0x0f) | 0x40; b[8] = (b[8] & 0x3f) | 0x80;
    var h = [].map.call(b, function (x) { return ('0' + x.toString(16)).slice(-2); }).join('');
    return h.slice(0, 8) + '-' + h.slice(8, 12) + '-' + h.slice(12, 16) + '-' + h.slice(16, 20) + '-' + h.slice(20);
  }
  function rpc(name, body) {
    return fetch(URL_BASE + '/rest/v1/rpc/' + name, {
      method: 'POST', headers: { 'Content-Type': 'application/json', apikey: KEY },
      body: JSON.stringify(body), cache: 'no-store'
    }).then(function (r) {
      return r.json().catch(function () { return null; }).then(function (d) {
        if (!r.ok) {
          var e = new Error((d && d.message) || ('http ' + r.status));
          // 세션이 끝났다(30분 쉼 · 8시간 · 막힌 키 · 기한 지난 키 — 서버 economy.ops_session_of)
          e.denied = /ops session|ops key/.test(e.message);
          // 5xx(게이트웨이 시간 초과 등)는 서버가 이미 처리했을 수 있다 — 끊김처럼 "모름"으로 둔다
          e.unsure = r.status >= 500;
          throw e;
        }
        // 서버는 세션을 쓸 때마다 30분을 다시 센다 — 여기서도 같이 센다
        if (body && body.p_token) lastUse = Date.now();
        return d;
      });
    }, function () { var e = new Error('network'); e.network = true; e.unsure = true; throw e; });
  }
  function num(n) { return (+n || 0).toLocaleString('ko-KR'); }
  function when(t) {
    try { return new Date(t).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul', month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }); }
    catch (e) { return ''; }
  }
  function dayOf(t) {
    try { return new Date(t).toLocaleDateString('ko-KR', { timeZone: 'Asia/Seoul', month: 'numeric', day: 'numeric' }); }
    catch (e) { return ''; }
  }
  function el(tag, attrs, kids) {
    var n = document.createElement(tag);
    Object.keys(attrs || {}).forEach(function (k) {
      if (k === 'text') n.textContent = attrs[k];
      else if (k === 'class') n.className = attrs[k];
      else n.setAttribute(k, attrs[k]);
    });
    (kids || []).forEach(function (c) { if (c) n.appendChild(typeof c === 'string' ? document.createTextNode(c) : c); });
    return n;
  }
  function say(box, kind, text, list) {
    box.hidden = !text;
    box.className = 'msg ' + (kind || '');
    box.textContent = text || '';
    if (list && list.length) {
      var ul = el('ul'); list.forEach(function (x) { ul.appendChild(el('li', { text: x })); }); box.appendChild(ul);
    }
  }
  function problem(e) {
    if (e && e.denied) return '로그인이 끝났어요(30분 동안 쓰지 않았거나, 8시간이 지났거나, 키가 막혔거나 기한이 지났어요). 운영자 키로 다시 로그인해 주세요.';
    if (e && e.network) return '연결이 끊겼어요. 잠시 뒤 같은 버튼을 다시 눌러 주세요(두 번 처리되지 않습니다).';
    if (e && e.unsure) return '답을 받지 못했어요. 잠시 뒤 같은 내용 그대로 같은 버튼을 다시 눌러 주세요(두 번 처리되지 않습니다).';
    return '처리하지 못했어요. 잠시 뒤 다시 시도해 주세요. (' + ((e && e.message) || '알 수 없음') + ')';
  }
  function confirmBox(title, body, yesLabel) {
    var d = $('confirm');
    $('cfTitle').textContent = title; $('cfBody').textContent = body; $('cfYes').textContent = yesLabel || '확인';
    return new Promise(function (resolve) {
      function done(v) { $('cfYes').onclick = $('cfNo').onclick = d.oncancel = null; d.close(); resolve(v); }
      $('cfYes').onclick = function () { done(true); };
      $('cfNo').onclick = function () { done(false); };
      d.oncancel = function (ev) { ev.preventDefault(); done(false); };
      d.showModal();
    });
  }
  // 운영자 키를 한 번 더 — 넣은 키(문자열) 또는 취소면 null. 키는 이 요청에만 쓰고 남기지 않는다
  function askKey(title, body, yesLabel, note, noteKind) {
    var d = $('reauth');
    $('raTitle').textContent = title; $('raBody').textContent = body; $('raYes').textContent = yesLabel || '확인';
    $('raKey').value = '';
    say($('raMsg'), noteKind || 'err', note || '');
    return new Promise(function (resolve) {
      function done(v) {
        $('raForm').onsubmit = $('raNo').onclick = d.oncancel = null;
        $('raKey').value = '';
        d.close(); resolve(v);
      }
      $('raForm').onsubmit = function (ev) {
        ev.preventDefault();
        var k = $('raKey').value.trim();
        if (!k) { say($('raMsg'), 'err', '운영자 키를 넣어 주세요.'); $('raKey').focus(); return; }
        done(k);
      };
      $('raNo').onclick = function () { done(null); };
      d.oncancel = function (ev) { ev.preventDefault(); done(null); };
      d.showModal();
      $('raKey').focus();
    });
  }
  // 확인(큰 일이면 키 입력) → 보내기. 서버가 키를 더 달라고 하거나(confirm_required — 사이에 사람 수가 늘었을 때)
  // 키가 틀렸다고 하면(confirm_wrong) 다시 묻고 같은 요청 번호로 다시 보낸다. 취소하면 null, 그 밖에는 서버의 답.
  function runAction(o) {
    var first = o.big ? askKey(o.title, o.body, o.yes) : confirmBox(o.title, o.body, o.yes).then(function (y) { return y ? '' : null; });
    function go(pass) {
      if (pass === null) return null;
      return o.send(pass).then(function (r) {
        if (r && !r.ok && r.code === 'confirm_required') return askKey(o.title, o.body, o.yes, r.message, 'warn').then(go);
        if (r && !r.ok && r.code === 'confirm_wrong' && !r.logged_out) return askKey(o.title, o.body, o.yes, r.message, 'err').then(go);
        return r;
      });
    }
    return first.then(go);
  }
  function itemName(kind) { return (ITEMS[kind] || {}).name || kind; }
  function itemDesc(k) { var i = ITEMS[k.kind]; return i ? i.desc(+k.value) : ''; }

  // ── 로그인 · 로그아웃 ──
  function forget() {
    token = ''; state = null; sessionEnds = 0;
    try { sessionStorage.removeItem(STORE); } catch (e) { /* 없음 */ }
  }
  function leave(text, kind) {
    forget();
    $('app').hidden = true; $('gate').hidden = false; $('key').value = '';
    say($('gateMsg'), kind || 'err', text || '');
  }
  function enter(t, fromForm) {
    return rpc('ops_overview', { p_token: t }).then(function (d) {
      token = t; state = d;
      try { sessionStorage.setItem(STORE, t); } catch (e) { /* 저장소가 막혀 있으면 이번 화면에서만 */ }
      $('gate').hidden = true; $('app').hidden = false; say($('gateMsg'), '', '');
      render();
    }, function (e) {
      // 다시 열었을 때 세션이 이미 끝났으면 조용히 로그인 화면으로
      leave(fromForm || !e.denied ? problem(e) : '');
    });
  }
  function refresh() {
    return rpc('ops_overview', { p_token: token }).then(function (d) { state = d; render(); }, function (e) {
      if (e.denied) leave(problem(e));
    });
  }
  $('gateForm').addEventListener('submit', function (ev) {
    ev.preventDefault();
    var k = $('key').value.trim();
    if (!k || $('gateBtn').disabled) return;
    say($('gateMsg'), '', '');
    $('gateBtn').disabled = true;
    rpc('ops_login', { p_key: k }).then(function (r) {
      if (!r || !r.ok) { say($('gateMsg'), 'err', (r && r.message) || '로그인하지 못했어요.'); return; }
      $('key').value = '';
      return enter(r.session, true);
    }, function (e) { say($('gateMsg'), 'err', problem(e)); })
      .then(function () { $('gateBtn').disabled = false; });
  });
  $('logout').addEventListener('click', function () {
    var t = token;
    leave('로그아웃했어요.', 'ok');
    // 서버 세션도 끝낸다 — 답을 못 받아도 30분 뒤 저절로 끝난다
    if (t) rpc('ops_logout', { p_token: t }).catch(function () { /* 위 */ });
  });
  // 서버와 같은 기준으로 화면도 닫는다(30분 쉼 · 8시간) — 남은 화면에서 누르다 실패하지 않게
  setInterval(function () {
    if (!token) return;
    var now = Date.now();
    if (sessionEnds && now >= sessionEnds) leave('로그인한 지 8시간이 지나 로그아웃했어요. 운영자 키로 다시 로그인해 주세요.');
    else if (lastUse && now - lastUse >= idleMin * 60e3) leave(idleMin + '분 동안 쓰지 않아 로그아웃했어요. 운영자 키로 다시 로그인해 주세요.');
  }, 20e3);

  // ── 그리기 ──
  function render() {
    if (state.confirm) limits = state.confirm;
    var s = state.session || {};
    idleMin = +s.idle_min || 30;
    sessionEnds = Date.parse(s.expires_at) || 0;
    // 키 기한이 일주일 안이면 미리 알린다(기한이 지나면 로그인도, 열려 있던 세션도 끝난다)
    var keyLeftDays = s.key_expires_at ? Math.ceil((Date.parse(s.key_expires_at) - Date.now()) / 864e5) : null;
    var keySoon = keyLeftDays !== null && keyLeftDays <= 7;
    var info = $('sessionInfo');
    info.textContent = '로그인 ' + when(s.expires_at) + '까지 · ' + idleMin + '분 동안 쓰지 않으면 로그아웃 · 키 기한 ' + dayOf(s.key_expires_at) +
      (keySoon ? ' (' + Math.max(keyLeftDays, 0) + '일 남음 — 새 키를 받아 두세요)' : '');
    info.className = 'session' + (keySoon ? ' warn' : '');

    $('actor').textContent = state.actor;
    $('stUsers').textContent = num(state.users);
    $('stRunners').textContent = num(state.runners_7d);
    $('stHeld').textContent = num(state.kinds.reduce(function (sum, k) { return sum + (+k.held || 0); }, 0));
    $('audAll').textContent = '(' + num(state.users) + '명)';
    $('audRunners').textContent = '(' + num(state.runners_7d) + '명)';
    var ev = state.event;
    $('stEventBox').className = 'stat' + (ev ? ' live' : '');
    $('stEvent').textContent = ev ? '보상 ×' + (+ev.mult) : '없음';
    $('stEventSub').textContent = ev ? when(ev.ends_at) + '까지' : '지금 보상 이벤트';

    // 아이템 고르기(처음 한 번만 — 고른 것을 지우지 않게)
    var box = $('items');
    if (!box.children.length) {
      state.kinds.filter(function (k) { return k.enabled; }).forEach(function (k, i) {
        var input = el('input', { type: 'radio', name: 'kind', value: k.kind });
        if (i === 0) input.checked = true;
        box.appendChild(el('label', { class: 'item' }, [
          input,
          el('b', {}, [itemName(k.kind), el('em', { text: (ITEMS[k.kind] || {}).when || '' })]),
          el('span', { text: itemDesc(k) }),
          el('small', { 'data-held': k.kind })
        ]));
      });
    }
    state.kinds.forEach(function (k) {
      var sm = box.querySelector('[data-held="' + k.kind + '"]');
      if (sm) sm.textContent = '지금 ' + num(k.holders) + '명이 ' + num(k.held) + '개 보유 · 지금까지 ' + num(k.used) + '개 사용';
    });

    renderEvents(); renderLog(); grantSummary(); eventSummary();
  }

  function eventTag(e) {
    var map = { LIVE: ['live', '진행 중'], SCHEDULED: ['wait', '예약'], ENDED: ['off', '끝남'], CANCELLED: ['off', '취소됨'] };
    var m = map[e.state] || ['off', e.state];
    return el('span', { class: 'tag ' + m[0], text: m[1] });
  }
  function eventRow(e, withCancel) {
    var left = el('div', {}, [
      el('b', {}, [eventTag(e), e.title + ' · 보상 ×' + (+e.mult)]),
      el('span', { text: when(e.starts_at) + ' ~ ' + when(e.cancelled_at || e.ends_at) + ' · ' + e.actor })
    ]);
    var kids = [left];
    if (withCancel && (e.state === 'LIVE' || e.state === 'SCHEDULED')) {
      var b = el('button', { class: 'btn danger small', type: 'button', text: '끄기' });
      b.addEventListener('click', function () { cancelEvent(e); });
      kids.push(b);
    }
    return el('div', { class: 'li' }, kids);
  }
  function renderEvents() {
    var open = state.events.filter(function (e) { return e.state === 'LIVE' || e.state === 'SCHEDULED'; });
    var now = $('eventNow'); now.textContent = '';
    if (!open.length) now.appendChild(el('div', { class: 'empty', text: '켜져 있거나 예약된 보상 이벤트가 없어요.' }));
    open.forEach(function (e) { now.appendChild(eventRow(e, true)); });
    var all = $('events'); all.textContent = '';
    if (!state.events.length) all.appendChild(el('div', { class: 'empty', text: '아직 없어요.' }));
    state.events.forEach(function (e) { all.appendChild(eventRow(e, false)); });
  }
  function renderLog() {
    var list = $('batches'); list.textContent = '';
    if (!state.batches.length) list.appendChild(el('div', { class: 'empty', text: '아직 지급한 적이 없어요.' }));
    state.batches.forEach(function (b) {
      var left = el('div', {}, [
        el('b', {}, [b.revoked_at ? el('span', { class: 'tag off', text: '되돌림' }) : null,
          itemName(b.kind) + ' ' + num(b.qty) + '개 × ' + num(b.users) + '명']),
        el('span', { text: when(b.created_at) + ' · ' + (TARGETS[b.target] || b.target) + ' · ' + b.reason + ' · ' + b.actor + (b.push ? '' : ' · 푸시 없음') })
      ]);
      var kids = [left];
      if (!b.revoked_at) {
        var btn = el('button', { class: 'btn danger small', type: 'button', text: '되돌리기' });
        btn.addEventListener('click', function () { revoke(b); });
        kids.push(btn);
      }
      list.appendChild(el('div', { class: 'li' }, kids));
    });
    var kinds = $('kinds'); kinds.textContent = '';
    state.kinds.forEach(function (k) {
      kinds.appendChild(el('div', { class: 'li' }, [el('div', {}, [
        el('b', { text: itemName(k.kind) }),
        el('span', { text: itemDesc(k) + ' · ' + num(k.holders) + '명이 ' + num(k.held) + '개 보유 · ' + num(k.used) + '개 사용' })
      ])]));
    });
  }

  // ── 탭 ──
  var TABS = [['tabGrant', 'paneGrant'], ['tabEvent', 'paneEvent'], ['tabApply', 'paneApply'], ['tabEcon', 'paneEcon'], ['tabLog', 'paneLog']];
  TABS.forEach(function (t) {
    $(t[0]).addEventListener('click', function () {
      TABS.forEach(function (x) { $(x[0]).setAttribute('aria-selected', String(x === t)); $(x[1]).hidden = x !== t; });
      if (t[0] === 'tabLog') refresh();
      if (t[0] === 'tabEcon') loadEcon();
      if (t[0] === 'tabApply') applySummary();
    });
  });

  // ── 아이템 지급 ──
  function audience() { return (document.querySelector('input[name=aud]:checked') || {}).value || 'EMAILS'; }
  function kindNow() { return (document.querySelector('input[name=kind]:checked') || {}).value || ''; }
  function emailList() {
    var seen = {};
    return $('emails').value.split(/[\s,;]+/).map(function (s) { return s.trim().toLowerCase(); })
      .filter(function (s) { if (!s || seen[s]) return false; seen[s] = 1; return true; });
  }
  function qtyNow() { return Math.min(99, Math.max(1, parseInt($('qty').value, 10) || 1)); }
  // 받을 사람 수(화면 기준 — 서버는 가입한 이메일만 센다)
  function recipients(aud, emails) {
    return aud === 'ALL' ? +state.users : aud === 'RUNNERS_7D' ? +state.runners_7d : emails.length;
  }
  function grantBig(aud, n) { return aud === 'ALL' || n > +limits.recipients; }
  function grantSummary() {
    if (!state) return;
    var aud = audience(), who, emails = emailList(), n = recipients(aud, emails), qty = qtyNow();
    $('emailBox').hidden = aud !== 'EMAILS';
    if (aud === 'ALL') who = '전체 ' + num(state.users) + '명';
    else if (aud === 'RUNNERS_7D') who = '최근 7일 달린 ' + num(state.runners_7d) + '명';
    else who = n ? '이메일 ' + num(n) + '명' : '';
    $('grantSummary').textContent = who
      ? who + '에게 ' + itemName(kindNow()) + ' ' + qty + '개씩' + ($('grantPush').checked ? ' · 푸시 알림 보냄' : ' · 푸시 없음')
      : '받을 사람의 이메일을 넣어 주세요.';
    var cap = state.cap || {}, units = n * qty, note = $('grantNote'), parts = [];
    if (cap.limit) parts.push('오늘 이 키로 더 줄 수 있는 아이템 ' + num(cap.left) + '개(하루 ' + num(cap.limit) + '개 — 받는 사람 수 × 개수)');
    if (who && grantBig(aud, n)) parts.push('큰 지급이라 운영자 키를 한 번 더 물어요');
    var over = who && cap.limit && units > +cap.left;
    if (over) parts.unshift('이번 지급(' + num(units) + '개)은 오늘 남은 한도를 넘어요');
    note.textContent = parts.join(' · ');
    note.className = 'note' + (over ? ' warn' : '');
    versionCheck(aud, emails, who);
  }
  // 받는 사람의 앱 판(0116 ops_audience_preview) — 아이템 가방은 새 판(1.19.0)부터. 예전 판에는 "바로 적용"을 권한다
  var versionTimer = 0, versionSeq = 0;
  function versionCheck(aud, emails, who) {
    clearTimeout(versionTimer);
    var box = $('grantVersion');
    if (!who) { box.textContent = ''; return; }
    versionTimer = setTimeout(function () {
      var seq = ++versionSeq;
      rpc('ops_audience_preview', { p_token: token, p_audience: aud, p_emails: aud === 'EMAILS' ? emails : null }).then(function (r) {
        if (seq !== versionSeq || !r || !r.ok) return;
        var old = +r.old_app || 0;
        box.textContent = old
          ? '받는 ' + num(r.users) + '명 중 ' + num(old) + '명은 아이템 가방이 없는 예전 판 앱이에요(새 판을 깔기 전에는 못 써요). ' +
            '내구도 · 에너지는 "바로 적용" 탭에서 바로 채울 수 있어요.'
          : '받는 ' + num(r.users) + '명 모두 아이템 가방이 있는 앱이에요.';
        box.className = 'note' + (old ? ' warn' : '');
      }, function (e) { if (e.denied) leave(problem(e)); });
    }, 300);
  }
  $('grantForm').addEventListener('input', grantSummary);
  $('grantForm').addEventListener('change', grantSummary);
  $('qtyMinus').addEventListener('click', function () { $('qty').value = Math.max(1, qtyNow() - 1); grantSummary(); });
  $('qtyPlus').addEventListener('click', function () { $('qty').value = Math.min(99, qtyNow() + 1); grantSummary(); });
  $('reasonChips').addEventListener('click', function (ev) {
    if (ev.target.classList.contains('chip')) { $('reason').value = ev.target.textContent; grantSummary(); }
  });

  // 사람 찾기
  var findTimer = 0, findSeq = 0;
  $('find').addEventListener('input', function () {
    clearTimeout(findTimer);
    var q = $('find').value.trim(), box = $('found');
    if (q.length < 2) { box.hidden = true; box.textContent = ''; return; }
    findTimer = setTimeout(function () {
      var seq = ++findSeq;
      rpc('ops_user_find', { p_token: token, p_query: q }).then(function (rows) {
        if (seq !== findSeq) return;
        box.textContent = ''; box.hidden = false;
        if (!rows.length) { box.appendChild(el('div', { class: 'empty', text: '찾지 못했어요.' })); return; }
        rows.forEach(function (u) {
          var held = Object.keys(u.items || {}).map(function (k) { return itemName(k) + ' ' + u.items[k]; }).join(' · ');
          var b = el('button', { type: 'button' }, [
            el('b', { text: (u.name || '이름 없음') + ' · ' + u.email }),
            el('span', { text: held || '아이템 없음' })
          ]);
          b.addEventListener('click', function () {
            var list = emailList();
            if (list.indexOf(String(u.email).toLowerCase()) < 0) $('emails').value = ($('emails').value.trim() ? $('emails').value.trim() + '\n' : '') + u.email;
            grantSummary();
          });
          box.appendChild(b);
        });
      }, function (e) { if (e.denied) leave(problem(e)); /* 그 밖에는 조용히 — 다음 글자에서 다시 */ });
    }, 250);
  });

  $('grantForm').addEventListener('submit', function (ev) {
    ev.preventDefault();
    if (busy || !state) return;
    var aud = audience(), kind = kindNow(), qty = qtyNow(), reason = $('reason').value.trim(), emails = emailList();
    var msg = $('grantMsg');
    if (aud === 'EMAILS' && !emails.length) { say(msg, 'err', '받을 사람의 이메일을 넣어 주세요.'); $('emails').focus(); return; }
    if (reason.length < 2) { say(msg, 'err', '사유를 두 글자 이상 적어 주세요.'); $('reason').focus(); return; }
    var n = recipients(aud, emails), big = grantBig(aud, n), push = $('grantPush').checked;
    var who = aud === 'ALL' ? '전체 ' + num(state.users) + '명' : aud === 'RUNNERS_7D' ? '최근 7일 달린 ' + num(state.runners_7d) + '명' : '이메일 ' + num(emails.length) + '명';
    var body = who + '에게\n' + itemName(kind) + ' ' + qty + '개씩 (모두 ' + num(n * qty) + '개)\n사유: ' + reason + (push ? '\n푸시 알림이 갑니다.' : '');
    busy = true; $('grantBtn').disabled = true; say(msg, '', '');
    runAction({
      big: big, title: big ? '큰 지급이에요 — 운영자 키를 한 번 더' : '지급할까요?', body: body, yes: '지급하기',
      send: function (pass) {
        grantRequest = requestFor(grantRequest, [aud, kind, qty, reason, aud === 'EMAILS' ? emails : null, push]);
        var req = {
          p_token: token, p_request: grantRequest.id, p_kind: kind, p_qty: qty, p_reason: reason,
          p_emails: aud === 'EMAILS' ? emails : null, p_audience: aud, p_push: push
        };
        if (pass) req.p_confirm = pass;
        return rpc('ops_item_grant', req);
      }
    }).then(function (r) {
      if (!r) return;
      var missing = r.missing || [];
      if (!r.ok) {
        grantRequest = null;
        if (r.logged_out) { leave(r.message); return; }
        say(msg, 'err', r.message || '지급하지 못했어요.', missing.length ? missing.map(function (m) { return '가입하지 않은 이메일: ' + m; }) : null);
        if (r.code === 'cap') return refresh();
        return;
      }
      say(msg, missing.length ? 'warn' : 'ok',
        num(r.users) + '명에게 ' + itemName(kind) + ' ' + qty + '개씩 지급했어요.' + (r.replay ? ' (이미 처리된 지급이에요)' : '') +
        (missing.length ? ' 아래 이메일은 가입한 계정이 없어 빠졌어요.' : ''), missing);
      grantRequest = null;
      if (aud === 'EMAILS') $('emails').value = missing.join('\n');
      return refresh();
    }, function (e) {
      // 답을 못 받았으면(끊김 · 5xx) 같은 번호를 남겨 둔다 — 같은 내용으로 다시 눌러도 두 번 주지 않는다
      if (!e.unsure) grantRequest = null;
      if (e.denied) { leave(problem(e)); return; }
      say(msg, 'err', problem(e));
    }).then(function () { busy = false; $('grantBtn').disabled = false; });
  });

  function revoke(b) {
    if (busy) return;
    busy = true;
    runAction({
      big: true, title: '이 지급을 되돌릴까요?', yes: '되돌리기',
      body: itemName(b.kind) + ' ' + b.qty + '개 × ' + num(b.users) + '명\n사유: ' + b.reason + '\n\n아직 갖고 있는 만큼만 거둡니다.',
      send: function (pass) {
        var req = { p_token: token, p_batch: b.id };
        if (pass) req.p_confirm = pass;
        return rpc('ops_grant_revoke', req);
      }
    }).then(function (r) {
      if (!r) return;
      if (!r.ok) {
        if (r.logged_out) { leave(r.message); return; }
        say($('logMsg'), 'err', r.message || '되돌리지 못했어요.');
        return;
      }
      say($('logMsg'), r.short ? 'warn' : 'ok', num(r.taken) + '개를 거뒀어요.' + (r.short ? ' ' + num(r.short) + '명은 이미 써서 다 거두지 못했어요.' : ''));
      return refresh();
    }, function (e) {
      if (e.denied) { leave(problem(e)); return; }
      say($('logMsg'), 'err', problem(e));
    }).then(function () { busy = false; });
  }

  // ── 보상 이벤트 ──
  function evWhen() { return (document.querySelector('input[name=evWhen]:checked') || {}).value || 'now'; }
  function evStartAt() {
    if (evWhen() !== 'later' || !$('evStart').value) return null;
    var d = new Date($('evStart').value);
    return isNaN(d) ? null : d;
  }
  function eventBig(mult, hours) { return mult >= +limits.mult || hours > +limits.hours; }
  function eventSummary() {
    var later = evWhen() === 'later';
    $('evLaterBox').hidden = !later;
    $('evPush').disabled = later;
    $('evPushLabel').textContent = later ? '예약한 이벤트는 시작 푸시가 가지 않아요' : '시작 푸시 알림 보내기';
    var mult = +$('evMult').value, hours = +$('evHours').value, start = later ? evStartAt() : new Date();
    var text = '모든 러닝 적립 ×' + (mult || '?') + ' · ' + (hours || '?') + '시간';
    if (start && hours) text += ' · ' + when(start) + ' ~ ' + when(new Date(start.getTime() + hours * 3600e3));
    $('eventSummary').textContent = text;
    $('eventNote').textContent = mult && hours && eventBig(mult, hours)
      ? '×' + num(limits.mult) + ' 이상이거나 ' + num(limits.hours) + '시간이 넘는 이벤트라 운영자 키를 한 번 더 물어요' : '';
    $('eventBtn').textContent = later ? '이벤트 예약' : '이벤트 시작';
  }
  $('eventForm').addEventListener('input', eventSummary);
  $('eventForm').addEventListener('change', eventSummary);
  $('presets').addEventListener('click', function (ev) {
    var b = ev.target; if (!b.classList.contains('chip')) return;
    $('evTitle').value = b.dataset.title; $('evMult').value = b.dataset.mult; $('evHours').value = b.dataset.hours;
    eventSummary();
  });
  $('eventForm').addEventListener('submit', function (ev) {
    ev.preventDefault();
    if (busy) return;
    var msg = $('eventMsg'), title = $('evTitle').value.trim(), mult = +$('evMult').value, hours = +$('evHours').value;
    var later = evWhen() === 'later', start = evStartAt();
    if (title.length < 2) { say(msg, 'err', '이벤트 이름을 두 글자 이상 적어 주세요.'); $('evTitle').focus(); return; }
    if (!(mult > 1 && mult <= 5)) { say(msg, 'err', '배수는 1 보다 크고 5 이하로 정해 주세요.'); $('evMult').focus(); return; }
    if (!(hours >= 1 && hours <= 168)) { say(msg, 'err', '기간은 1 ~ 168시간으로 정해 주세요.'); $('evHours').focus(); return; }
    if (later && !start) { say(msg, 'err', '시작 시각을 정해 주세요.'); $('evStart').focus(); return; }
    var from = later ? start : new Date();
    var evPush = !later && $('evPush').checked, big = eventBig(mult, hours);
    busy = true; $('eventBtn').disabled = true; say(msg, '', '');
    runAction({
      big: big, yes: later ? '예약하기' : '시작하기',
      title: big ? '큰 이벤트예요 — 운영자 키를 한 번 더' : (later ? '이벤트를 예약할까요?' : '이벤트를 시작할까요?'),
      body: title + '\n모든 사람의 러닝 적립 ×' + mult + '\n' + when(from) + ' ~ ' + when(new Date(from.getTime() + hours * 3600e3)) +
        (evPush ? '\n\n전체에게 푸시 알림이 갑니다(이벤트 소식을 끈 사람 제외).' : ''),
      send: function (pass) {
        eventRequest = requestFor(eventRequest, [title, mult, hours, later ? start.toISOString() : null, evPush]);
        var req = {
          p_token: token, p_request: eventRequest.id, p_title: title, p_mult: mult, p_hours: hours,
          p_starts_at: later ? start.toISOString() : null, p_push: evPush
        };
        if (pass) req.p_confirm = pass;
        return rpc('ops_event_start', req);
      }
    }).then(function (r) {
      if (!r) return;
      eventRequest = null;
      if (!r.ok) {
        if (r.logged_out) { leave(r.message); return; }
        say(msg, 'err', r.message || '시작하지 못했어요.');
        return;
      }
      say(msg, 'ok', later ? '이벤트를 예약했어요.' : '이벤트를 시작했어요. 지금부터 시작하는 러닝에 적용됩니다.');
      return refresh();
    }, function (e) {
      if (!e.unsure) eventRequest = null;
      if (e.denied) { leave(problem(e)); return; }
      say(msg, 'err', problem(e));
    }).then(function () { busy = false; $('eventBtn').disabled = false; });
  });
  function cancelEvent(e) {
    if (busy) return;
    confirmBox('이벤트를 끌까요?', e.title + ' · 보상 ×' + (+e.mult) + '\n\n지금부터 꺼집니다. 이미 시작한 러닝은 그대로 받습니다.', '끄기').then(function (yes) {
      if (!yes) return;
      busy = true;
      rpc('ops_event_cancel', { p_token: token, p_id: e.id }).then(function (r) {
        say($('eventMsg'), r.ok ? 'ok' : 'err', r.ok ? '이벤트를 껐어요.' : (r.message || '끄지 못했어요.'));
        return refresh();
      }, function (x) {
        if (x.denied) { leave(problem(x)); return; }
        say($('eventMsg'), 'err', problem(x));
      }).then(function () { busy = false; });
    });
  }


  // ── 바로 적용 (0116 ops_apply_now) ──
  var applyRequest = null;
  var APPLY = { DURABILITY_FULL: '신발 내구도 가득', ENERGY_RESET: '오늘 쓴 에너지 되돌리기' };
  function apAud() { return (document.querySelector('input[name=apAud]:checked') || {}).value || 'EMAILS'; }
  function apAction() { return (document.querySelector('input[name=apAction]:checked') || {}).value || 'DURABILITY_FULL'; }
  function apEmails() {
    var seen = {};
    return $('apEmails').value.split(/[\s,;]+/).map(function (x) { return x.trim().toLowerCase(); })
      .filter(function (x) { if (!x || seen[x]) return false; seen[x] = 1; return true; });
  }
  function apWho(aud, emails) {
    if (!state) return '';
    return aud === 'ALL' ? '전체 ' + num(state.users) + '명' : aud === 'RUNNERS_7D' ? '최근 7일 달린 ' + num(state.runners_7d) + '명'
      : emails.length ? '이메일 ' + num(emails.length) + '명' : '';
  }
  function applySummary() {
    var aud = apAud(), emails = apEmails(), who = apWho(aud, emails);
    $('apEmailBox').hidden = aud !== 'EMAILS';
    $('applySummary').textContent = who ? who + '에게 ' + APPLY[apAction()] : '받을 사람의 이메일을 넣어 주세요.';
  }
  $('applyForm').addEventListener('input', applySummary);
  $('applyForm').addEventListener('change', applySummary);
  $('applyForm').addEventListener('submit', function (ev) {
    ev.preventDefault();
    if (busy || !state) return;
    var aud = apAud(), action = apAction(), emails = apEmails(), reason = $('apReason').value.trim(), msg = $('applyMsg');
    if (aud === 'EMAILS' && !emails.length) { say(msg, 'err', '받을 사람의 이메일을 넣어 주세요.'); $('apEmails').focus(); return; }
    if (reason.length < 2) { say(msg, 'err', '사유를 두 글자 이상 적어 주세요.'); $('apReason').focus(); return; }
    var n = aud === 'EMAILS' ? emails.length : aud === 'ALL' ? +state.users : +state.runners_7d;
    var big = aud === 'ALL' || n > +limits.recipients;
    busy = true; $('applyBtn').disabled = true; say(msg, '', '');
    runAction({
      big: big, title: big ? '크게 적용해요 — 운영자 키를 한 번 더' : '바로 적용할까요?',
      body: apWho(aud, emails) + '에게\n' + APPLY[action] + '\n사유: ' + reason + '\n되돌릴 수 없고, 알림은 가지 않습니다.', yes: '바로 적용',
      send: function (pass) {
        applyRequest = requestFor(applyRequest, [aud, action, reason, aud === 'EMAILS' ? emails : null]);
        var req = { p_token: token, p_request: applyRequest.id, p_action: action, p_reason: reason,
                    p_emails: aud === 'EMAILS' ? emails : null, p_audience: aud };
        if (pass) req.p_confirm = pass;
        return rpc('ops_apply_now', req);
      }
    }).then(function (r) {
      if (!r) return;
      var missing = r.missing || [];
      if (!r.ok) {
        applyRequest = null;
        if (r.logged_out) { leave(r.message); return; }
        say(msg, 'err', r.message || '적용하지 못했어요.', missing.length ? missing.map(function (m) { return '가입하지 않은 이메일: ' + m; }) : null);
        return;
      }
      applyRequest = null;
      say(msg, missing.length ? 'warn' : 'ok',
        num(r.users) + '명에게 적용했어요(' + (action === 'DURABILITY_FULL' ? '신발 ' + num(r.shoes) + '켤레' : '에너지 ' + num(r.shoes) + '명') + ').' +
        (r.replay ? ' (이미 처리된 요청이에요)' : ''), missing.length ? missing.map(function (m) { return '가입하지 않은 이메일: ' + m; }) : null);
      if (aud === 'EMAILS') $('apEmails').value = missing.join('\n');
      return refresh();
    }, function (e) {
      if (!e.unsure) applyRequest = null;
      if (e.denied) { leave(problem(e)); return; }
      say(msg, 'err', problem(e));
    }).then(function () { busy = false; $('applyBtn').disabled = false; });
  });

  // ── 경제 현황 · 설정 (0116 ops_economy_status · ops_economy_settings · ops_economy_set) ──
  function kv(label, value, sub) {
    return el('div', { class: 'li' }, [el('div', null, [el('b', { text: label }), sub ? el('span', { text: sub }) : null]), el('b', { text: value })]);
  }
  function loadEcon() {
    var box = $('econStatus');
    rpc('ops_economy_status', { p_token: token }).then(function (r) {
      box.textContent = '';
      if (!r || !r.ok) { box.appendChild(el('div', { class: 'empty', text: '불러오지 못했어요.' })); return; }
      var run = r.runs_7d || {}, money = r.money_7d || {}, y = r.yesterday || {}, v = r.versions || {};
      box.appendChild(kv('러닝', num(run.runs) + '번 · ' + num(run.people) + '명', '평균 ' + (run.avg_min || 0) + '분 · 10분 넘는 러닝 ' + (run.over_10min_pct || 0) + '%'));
      box.appendChild(kv('잘린 러닝', '내구도 ' + (run.durability_cut_pct || 0) + '% · 에너지 ' + (run.energy_cut_pct || 0) + '%', '0 SUP 러닝 ' + (run.zero_pct || 0) + '% · 평균 운 ×' + (run.avg_luck || 1)));
      box.appendChild(kv('적립 · 수리 · 상자(7일)', num(money.earned) + ' · ' + num(money.repair) + ' · ' + num(money.boxes) + ' SUP',
        '적립한 사람 ' + num(money.earners) + '명 · 수리비가 더 큰 사람 ' + num(money.repair_over_earn) + '명'));
      box.appendChild(kv('어제', num(y.earned) + ' · ' + num(y.repair) + ' · ' + num(y.boxes) + ' SUP', '적립 · 수리 · 상자'));
      var stuck = el('div', { class: 'li' }, [el('div', null, [el('b', { text: '막힌 사람' }),
        el('span', { text: '신발이 모두 0% · 잔고가 수리비보다 적고 · 무료 수리도 아직' })]), el('b', { text: num(r.stuck) + '명' })]);
      box.appendChild(stuck);
      box.appendChild(kv('앱 판', '새 판 ' + num(v.new_app) + '명', '판을 알린 사람 ' + num(v.reported) + '명 · 가입 ' + num(v.users) + '명'));
    }, function (e) { if (e.denied) leave(problem(e)); else { box.textContent = ''; box.appendChild(el('div', { class: 'empty', text: problem(e) })); } });
    rpc('ops_economy_settings', { p_token: token }).then(function (r) {
      var list = $('econSettings'); list.textContent = '';
      ((r && r.settings) || []).forEach(function (x) {
        var b = el('button', { class: 'btn ghost', type: 'button', text: '바꾸기' });
        b.addEventListener('click', function () { editSetting(x); });
        list.appendChild(el('div', { class: 'li' }, [el('div', null, [el('b', { text: x.label || x.key }),
          el('span', { text: x.key + ' = ' + JSON.stringify(x.value) })]), b]));
      });
    }, function (e) { if (e.denied) leave(problem(e)); });
  }
  function editSetting(x) {
    if (busy) return;
    var now = JSON.stringify(x.value);
    var hint = x.kind === 'bool' ? 'true(켬) 또는 false(끔)' : x.kind === 'luck' ? '[가장 작게, 가장 자주, 가장 크게] 예: [0.7, 1, 1.3]'
      : x.kind === 'steps' ? '걸음 수 목록 예: [2000, 4000]' : '숫자';
    var raw = window.prompt((x.label || x.key) + '\n지금: ' + now + '\n새 값(' + hint + ')', now);
    if (raw === null || raw.trim() === now) return;
    var value;
    try { value = JSON.parse(raw.trim()); } catch (e) { say($('econMsg'), 'err', '값을 읽지 못했어요. ' + hint + ' 꼴로 넣어 주세요.'); return; }
    busy = true; say($('econMsg'), '', '');
    runAction({
      big: true, title: '경제 설정을 바꿔요 — 운영자 키를 한 번 더', body: (x.label || x.key) + '\n' + now + ' → ' + JSON.stringify(value) + '\n바로 모든 사람에게 적용됩니다.', yes: '바꾸기',
      send: function (pass) { return rpc('ops_economy_set', { p_token: token, p_setting: x.key, p_value: value, p_confirm: pass || null }); }
    }).then(function (r) {
      if (!r) return;
      if (!r.ok) { if (r.logged_out) { leave(r.message); return; } say($('econMsg'), 'err', r.message || '바꾸지 못했어요.'); return; }
      say($('econMsg'), 'ok', (x.label || x.key) + ' — ' + JSON.stringify(r.old) + ' → ' + JSON.stringify(r.value));
      loadEcon();
    }, function (e) { if (e.denied) { leave(problem(e)); return; } say($('econMsg'), 'err', problem(e)); })
      .then(function () { busy = false; });
  }

  // 이 탭에서 이미 로그인했으면 이어서(세션 번호만 — 끝났으면 로그인 화면)
  var saved = '';
  try { saved = sessionStorage.getItem(STORE) || ''; } catch (e) { /* 없음 */ }
  if (saved) enter(saved, false);
})();
