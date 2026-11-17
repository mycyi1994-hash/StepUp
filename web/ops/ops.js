/* StepUp 운영자 페이지 — 아이템 지급 · 보상 이벤트(서버 0106).
   운영자 키는 SQL 로 만든다: select economy.ops_key_new('담당자 이름');  막기: update economy.ops_keys set revoked_at = now() where label = '…';
   키는 이 탭의 세션 저장소에만 두고(탭을 닫으면 사라진다) 서버에는 해시만 있다. 모든 일은 서버 함수(ops_*)가 키를 확인하고 한다.
   아이템 이름 · 설명은 여기(운영자용 한국어)와 앱 문구 · 푸시 문구(supabase/functions/push-send)에 따로 있다 — 종류를 더하면 셋을 함께. */
(function () {
  'use strict';
  // 공개값(web/waitlist.js · 11events/assets/config.js 와 같다)
  var URL_BASE = 'https://pupjzcmybuoyhzfwrsdf.supabase.co';
  var KEY = 'sb_publishable_jt74AKM32zdqnJlsFHEo2g_MNHa-WRO';
  var STORE = 'stepup.ops.key';

  var ITEMS = {
    RUN_BOOSTER: { name: '러닝 부스터', desc: function (v) { return '고른 러닝 1회 적립 ×' + v; }, when: '출발 전에 고름' },
    REWARD_DOUBLE: { name: '보상 2배권', desc: function (v) { return '고른 러닝 1회 적립 ×' + v; }, when: '출발 전에 고름' },
    ENERGY_DRINK: { name: '에너지 드링크', desc: function (v) { return '고른 러닝 1회 에너지를 ' + Math.round(v * 100) + '%만 씀'; }, when: '출발 전에 고름' },
    DURABILITY_RESET: { name: '내구도 초기화권', desc: function () { return '신은 신발 내구도를 가득'; }, when: '바로 사용' },
    ENERGY_RESET: { name: '하루 할당량 초기화권', desc: function () { return '오늘 쓴 에너지를 되돌려 가득'; }, when: '바로 사용' }
  };
  var TARGETS = { EMAILS: '이메일', ALL: '전체', RUNNERS_7D: '최근 7일 달린 사람' };

  var $ = function (id) { return document.getElementById(id); };
  var token = '';
  var state = null;        // ops_overview 결과
  var grantRequest = null; // 같은 지급을 다시 보내도 한 번만 되게 — 내용이 바뀌면 새 번호
  var eventRequest = null;
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
          e.denied = /ops key/.test(e.message);
          throw e;
        }
        return d;
      });
    }, function () { var e = new Error('network'); e.network = true; throw e; });
  }
  function num(n) { return (+n || 0).toLocaleString('ko-KR'); }
  function when(t) {
    try { return new Date(t).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul', month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }); }
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
    if (e && e.denied) return '운영자 키가 맞지 않거나 막힌 키예요.';
    if (e && e.network) return '연결이 끊겼어요. 잠시 뒤 같은 버튼을 다시 눌러 주세요(두 번 처리되지 않습니다).';
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
  function itemName(kind) { return (ITEMS[kind] || {}).name || kind; }
  function itemDesc(k) { var i = ITEMS[k.kind]; return i ? i.desc(+k.value) : ''; }

  // ── 들어가기 ──
  function enter(t, fromForm) {
    return rpc('ops_overview', { p_token: t }).then(function (d) {
      token = t; state = d;
      try { sessionStorage.setItem(STORE, t); } catch (e) { /* 저장소가 막혀 있으면 이번 화면에서만 */ }
      $('gate').hidden = true; $('app').hidden = false;
      render();
    }, function (e) {
      try { sessionStorage.removeItem(STORE); } catch (x) { /* 없음 */ }
      $('gate').hidden = false; $('app').hidden = true;
      if (fromForm) say($('gateMsg'), 'err', problem(e));
    });
  }
  function refresh() {
    return rpc('ops_overview', { p_token: token }).then(function (d) { state = d; render(); }, function (e) {
      if (e.denied) { leave(); say($('gateMsg'), 'err', problem(e)); }
    });
  }
  function leave() {
    token = ''; state = null;
    try { sessionStorage.removeItem(STORE); } catch (e) { /* 없음 */ }
    $('app').hidden = true; $('gate').hidden = false; $('key').value = '';
  }
  $('gateForm').addEventListener('submit', function (ev) {
    ev.preventDefault(); say($('gateMsg'), '', '');
    var t = $('key').value.trim(); if (t) enter(t, true);
  });
  $('logout').addEventListener('click', leave);

  // ── 그리기 ──
  function render() {
    $('actor').textContent = state.actor;
    $('stUsers').textContent = num(state.users);
    $('stRunners').textContent = num(state.runners_7d);
    $('stHeld').textContent = num(state.kinds.reduce(function (s, k) { return s + (+k.held || 0); }, 0));
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
      var s = box.querySelector('[data-held="' + k.kind + '"]');
      if (s) s.textContent = '지금 ' + num(k.holders) + '명이 ' + num(k.held) + '개 보유 · 지금까지 ' + num(k.used) + '개 사용';
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
  var TABS = [['tabGrant', 'paneGrant'], ['tabEvent', 'paneEvent'], ['tabLog', 'paneLog']];
  TABS.forEach(function (t) {
    $(t[0]).addEventListener('click', function () {
      TABS.forEach(function (x) { $(x[0]).setAttribute('aria-selected', String(x === t)); $(x[1]).hidden = x !== t; });
      if (t[0] === 'tabLog') refresh();
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
  function grantSummary() {
    if (!state) return;
    var aud = audience(), who;
    $('emailBox').hidden = aud !== 'EMAILS';
    if (aud === 'ALL') who = '전체 ' + num(state.users) + '명';
    else if (aud === 'RUNNERS_7D') who = '최근 7일 달린 ' + num(state.runners_7d) + '명';
    else { var n = emailList().length; who = n ? '이메일 ' + num(n) + '명' : ''; }
    $('grantSummary').textContent = who
      ? who + '에게 ' + itemName(kindNow()) + ' ' + qtyNow() + '개씩' + ($('grantPush').checked ? ' · 푸시 알림 보냄' : ' · 푸시 없음')
      : '받을 사람의 이메일을 넣어 주세요.';
    grantRequest = null; // 내용이 바뀌면 새 지급
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
      }, function () { /* 찾기는 조용히 — 다음 글자에서 다시 */ });
    }, 250);
  });

  $('grantForm').addEventListener('submit', function (ev) {
    ev.preventDefault();
    if (busy) return;
    var aud = audience(), kind = kindNow(), qty = qtyNow(), reason = $('reason').value.trim(), emails = emailList();
    var msg = $('grantMsg');
    if (aud === 'EMAILS' && !emails.length) { say(msg, 'err', '받을 사람의 이메일을 넣어 주세요.'); $('emails').focus(); return; }
    if (reason.length < 2) { say(msg, 'err', '사유를 두 글자 이상 적어 주세요.'); $('reason').focus(); return; }
    var who = aud === 'ALL' ? '전체 ' + num(state.users) + '명' : aud === 'RUNNERS_7D' ? '최근 7일 달린 ' + num(state.runners_7d) + '명' : '이메일 ' + num(emails.length) + '명';
    confirmBox('지급할까요?', who + '에게\n' + itemName(kind) + ' ' + qty + '개씩\n사유: ' + reason + ($('grantPush').checked ? '\n푸시 알림이 갑니다.' : ''), '지급하기').then(function (yes) {
      if (!yes) return;
      busy = true; $('grantBtn').disabled = true; say(msg, '', '');
      grantRequest = grantRequest || uuid();
      rpc('ops_item_grant', {
        p_token: token, p_request: grantRequest, p_kind: kind, p_qty: qty, p_reason: reason,
        p_emails: aud === 'EMAILS' ? emails : null, p_audience: aud, p_push: $('grantPush').checked
      }).then(function (r) {
        var missing = (r && r.missing) || [];
        if (!r.ok) {
          say(msg, 'err', r.message || '지급하지 못했어요.', missing.length ? missing.map(function (m) { return '가입하지 않은 이메일: ' + m; }) : null);
          grantRequest = null;
          return;
        }
        say(msg, missing.length ? 'warn' : 'ok',
          num(r.users) + '명에게 ' + itemName(kind) + ' ' + qty + '개씩 지급했어요.' + (r.replay ? ' (이미 처리된 지급이에요)' : '') +
          (missing.length ? ' 아래 이메일은 가입한 계정이 없어 빠졌어요.' : ''), missing);
        grantRequest = null;
        if (aud === 'EMAILS') $('emails').value = missing.join('\n');
        return refresh();
      }, function (e) {
        // 연결이 끊겼으면 같은 번호를 남겨 둔다 — 다시 눌러도 두 번 주지 않는다
        if (!e.network) grantRequest = null;
        if (e.denied) { leave(); say($('gateMsg'), 'err', problem(e)); return; }
        say(msg, 'err', problem(e));
      }).then(function () { busy = false; $('grantBtn').disabled = false; });
    });
  });

  function revoke(b) {
    if (busy) return;
    confirmBox('이 지급을 되돌릴까요?', itemName(b.kind) + ' ' + b.qty + '개 × ' + num(b.users) + '명\n사유: ' + b.reason + '\n\n아직 갖고 있는 만큼만 거둡니다.', '되돌리기').then(function (yes) {
      if (!yes) return;
      busy = true;
      rpc('ops_grant_revoke', { p_token: token, p_batch: b.id }).then(function (r) {
        if (!r.ok) { say($('logMsg'), 'err', r.message || '되돌리지 못했어요.'); return; }
        say($('logMsg'), r.short ? 'warn' : 'ok', num(r.taken) + '개를 거뒀어요.' + (r.short ? ' ' + num(r.short) + '명은 이미 써서 다 거두지 못했어요.' : ''));
        return refresh();
      }, function (e) { say($('logMsg'), 'err', problem(e)); }).then(function () { busy = false; });
    });
  }

  // ── 보상 이벤트 ──
  function evWhen() { return (document.querySelector('input[name=evWhen]:checked') || {}).value || 'now'; }
  function evStartAt() {
    if (evWhen() !== 'later' || !$('evStart').value) return null;
    var d = new Date($('evStart').value);
    return isNaN(d) ? null : d;
  }
  function eventSummary() {
    var later = evWhen() === 'later';
    $('evLaterBox').hidden = !later;
    $('evPush').disabled = later;
    $('evPushLabel').textContent = later ? '예약한 이벤트는 시작 푸시가 가지 않아요' : '시작 푸시 알림 보내기';
    var mult = +$('evMult').value, hours = +$('evHours').value, start = later ? evStartAt() : new Date();
    var text = '모든 러닝 적립 ×' + (mult || '?') + ' · ' + (hours || '?') + '시간';
    if (start && hours) text += ' · ' + when(start) + ' ~ ' + when(new Date(start.getTime() + hours * 3600e3));
    $('eventSummary').textContent = text;
    $('eventBtn').textContent = later ? '이벤트 예약' : '이벤트 시작';
    eventRequest = null;
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
    confirmBox(later ? '이벤트를 예약할까요?' : '이벤트를 시작할까요?',
      title + '\n모든 사람의 러닝 적립 ×' + mult + '\n' + when(from) + ' ~ ' + when(new Date(from.getTime() + hours * 3600e3)) +
      (!later && $('evPush').checked ? '\n\n전체에게 푸시 알림이 갑니다(이벤트 소식을 끈 사람 제외).' : ''),
      later ? '예약하기' : '시작하기').then(function (yes) {
      if (!yes) return;
      busy = true; $('eventBtn').disabled = true; say(msg, '', '');
      eventRequest = eventRequest || uuid();
      rpc('ops_event_start', {
        p_token: token, p_request: eventRequest, p_title: title, p_mult: mult, p_hours: hours,
        p_starts_at: later ? start.toISOString() : null, p_push: !later && $('evPush').checked
      }).then(function (r) {
        eventRequest = null;
        if (!r.ok) { say(msg, 'err', r.message || '시작하지 못했어요.'); return; }
        say(msg, 'ok', later ? '이벤트를 예약했어요.' : '이벤트를 시작했어요. 지금부터 시작하는 러닝에 적용됩니다.');
        return refresh();
      }, function (e) {
        if (!e.network) eventRequest = null;
        if (e.denied) { leave(); say($('gateMsg'), 'err', problem(e)); return; }
        say(msg, 'err', problem(e));
      }).then(function () { busy = false; $('eventBtn').disabled = false; });
    });
  });
  function cancelEvent(e) {
    if (busy) return;
    confirmBox('이벤트를 끌까요?', e.title + ' · 보상 ×' + (+e.mult) + '\n\n지금부터 꺼집니다. 이미 시작한 러닝은 그대로 받습니다.', '끄기').then(function (yes) {
      if (!yes) return;
      busy = true;
      rpc('ops_event_cancel', { p_token: token, p_id: e.id }).then(function (r) {
        say($('eventMsg'), r.ok ? 'ok' : 'err', r.ok ? '이벤트를 껐어요.' : (r.message || '끄지 못했어요.'));
        return refresh();
      }, function (x) { say($('eventMsg'), 'err', problem(x)); }).then(function () { busy = false; });
    });
  }

  // 이 탭에서 이미 들어왔으면 이어서
  var saved = '';
  try { saved = sessionStorage.getItem(STORE) || ''; } catch (e) { /* 없음 */ }
  if (saved) enter(saved, false);
})();
