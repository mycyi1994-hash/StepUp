/* 11.11 워킹데이 인증 검토 — 검토 키는 SQL 로 만든다: select economy.walkingday_reviewer_new('이름'); (0090) */
(function () {
  'use strict';
  var CFG = window.WALKINGDAY || {};
  var URL_BASE = CFG.url || 'https://pupjzcmybuoyhzfwrsdf.supabase.co';
  var KEY = CFG.key || '';
  var DAY0 = 20764;
  var $ = function (id) { return document.getElementById(id); };
  var token = '';
  var tab = 'pending', after = 0;

  function rpc(name, body) {
    return fetch(URL_BASE + '/rest/v1/rpc/' + name, {
      method: 'POST', headers: { 'Content-Type': 'application/json', apikey: KEY },
      body: JSON.stringify(body), cache: 'no-store'
    }).then(function (r) {
      return r.json().catch(function () { return null; }).then(function (d) {
        if (!r.ok) { var e = new Error((d && d.message) || ('http ' + r.status)); e.denied = /review key/.test(e.message); throw e; }
        return d;
      });
    });
  }
  function esc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); }
  function dayName(d) { return '11/' + (7 + d - DAY0); }
  function num(n) { return (+n || 0).toLocaleString('ko-KR'); }
  function when(t) { try { return new Date(t).toLocaleString('ko-KR', { timeZone: 'Asia/Seoul', month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' }); } catch (e) { return ''; } }

  function summary() {
    return rpc('walkingday_review_summary', { p_token: token }).then(function (s) {
      $('stats').innerHTML = '대기 <b>' + num(s.pending) + '</b> · 승인 <b>' + num(s.approved) + '</b> · 거절 <b>' + num(s.rejected) +
        '</b> · 합계 <b>' + num(s.total) + '</b>걸음 · 인증 <b>' + num(s.people) + '</b>명 · 사전 등록 <b>' + num(s.registered) + '</b>명';
    });
  }

  function open(t) {
    tab = t; after = 0;
    [].forEach.call(document.querySelectorAll('[data-tab]'), function (b) { b.setAttribute('aria-selected', String(b.dataset.tab === t)); });
    $('list').innerHTML = '';
    if (t === 'entries') entries(); else more();
  }

  function more() {
    var list = $('list'), grid = list.querySelector('.grid'), btn = list.querySelector('.more');
    if (!grid) { grid = document.createElement('div'); grid.className = 'grid'; list.appendChild(grid); }
    if (btn) btn.remove();
    return rpc('walkingday_review_list', { p_token: token, p_status: tab, p_after: after }).then(function (rows) {
      rows.forEach(function (r) { grid.appendChild(card(r)); after = Math.max(after, r.id); });
      if (!grid.children.length) list.innerHTML = '<p class="empty">' + (tab === 'pending' ? '확인할 인증이 없어요.' : '없어요.') + '</p>';
      if (rows.length === 10) {
        var m = document.createElement('button'); m.className = 'btn more'; m.type = 'button'; m.textContent = '더 보기';
        m.onclick = function () { m.disabled = true; more(); };
        list.appendChild(m);
      }
    }).catch(fail);
  }

  function card(r) {
    var el = document.createElement('div'); el.className = 'item';
    el.innerHTML =
      (r.image ? '<img alt="걸음 캡처" src="' + esc(r.image) + '">' : '<p class="status">캡처를 지웠어요</p>') +
      '<div class="meta"><b>' + dayName(r.day) + ' · ' + num(r.steps) + '걸음</b><br>' + esc(r.email) + '<br><span class="status">제출 ' + when(r.submitted_at) +
      (r.reviewed_by ? ' · ' + esc(r.reviewed_by) : '') + (r.note ? ' · ' + esc(r.note) : '') + '</span></div>' +
      (r.dup_client > 0 ? '<p class="warn">같은 곳(IP)에서 다른 이메일 ' + r.dup_client + '개가 제출했어요 — 다중 계정 확인</p>' : '') +
      '<div class="row"><input type="number" inputmode="numeric" min="1" max="100000" value="' + (+r.steps) + '" aria-label="승인할 걸음 수"></div>' +
      '<div class="row"><input type="text" maxlength="200" placeholder="메모(거절 까닭 등)" aria-label="메모" value="' + esc(r.note || '') + '"></div>' +
      '<div class="act">' +
      (r.status !== 'approved' ? '<button class="ok" data-s="approved" type="button">승인</button>' : '') +
      (r.status !== 'rejected' ? '<button class="no" data-s="rejected" type="button">거절</button>' : '') +
      (r.status !== 'pending' ? '<button class="re" data-s="pending" type="button">대기로</button>' : '') + '</div>';
    var img = el.querySelector('img');
    if (img) img.onclick = function () { var z = $('zoom'); z.querySelector('img').src = r.image; z.hidden = false; };
    var inputs = el.querySelectorAll('input');
    [].forEach.call(el.querySelectorAll('[data-s]'), function (b) {
      b.onclick = function () {
        var steps = parseInt(inputs[0].value, 10);
        [].forEach.call(el.querySelectorAll('button'), function (x) { x.disabled = true; });
        rpc('walkingday_review_set', { p_token: token, p_id: r.id, p_status: b.dataset.s, p_steps: steps >= 1 ? steps : null, p_note: inputs[1].value })
          .then(function (d) {
            if (!d || !d.ok) throw new Error((d && d.code) || 'fail');
            el.style.opacity = '.4';
            el.querySelector('.act').innerHTML = '<p class="status">' + { approved: '승인했어요', rejected: '거절했어요', pending: '대기로 돌렸어요' }[b.dataset.s] + '</p>';
            summary();
          })
          .catch(function (e) { [].forEach.call(el.querySelectorAll('button'), function (x) { x.disabled = false; }); alert('저장하지 못했어요: ' + e.message); });
      };
    });
    return el;
  }

  function entries() {
    var list = $('list');
    list.innerHTML = '<p class="empty">불러오는 중…</p>';
    rpc('walkingday_review_entries', { p_token: token }).then(function (rows) {
      var head = ['순위', '이메일', '응모권', '친구 초대', '승인 일수', '최고 걸음', '11,111 달성', '11/11 달성', '초대한 사람', '등록'];
      var csv = [head.join(',')];
      var html = '<div class="bar"><span class="status">초대 많은 순 → 응모권 많은 순. 초대 순위 경품은 위에서 11명(친구가 11,111걸음 승인을 받은 수).</span>' +
        '<button class="btn" type="button" id="csv">CSV 내려받기</button></div><div class="tablewrap"><table><thead><tr>' +
        head.map(function (h, i) { return '<th' + (i === 2 || i === 3 || i === 4 || i === 5 ? ' class="n"' : '') + '>' + h + '</th>'; }).join('') + '</tr></thead><tbody>';
      rows.forEach(function (r, i) {
        var cells = [i + 1, r.email, r.tickets, r.friends, r.approved_days, r.best_steps || '', r.goal_met ? 'O' : '', r.goal_on_1111 ? 'O' : '', r.invited_by || '', when(r.registered_at)];
        csv.push(cells.map(function (c) { c = String(c); return /[",\n]/.test(c) ? '"' + c.replace(/"/g, '""') + '"' : c; }).join(','));
        html += '<tr>' + cells.map(function (c, j) { return '<td' + (j >= 2 && j <= 5 ? ' class="n"' : '') + '>' + esc(j === 5 && c ? num(c) : c) + '</td>'; }).join('') + '</tr>';
      });
      list.innerHTML = html + '</tbody></table></div>';
      $('csv').onclick = function () {
        var blob = new Blob(['﻿' + csv.join('\n')], { type: 'text/csv;charset=utf-8' });
        var a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'walkingday-entries.csv'; a.click();
        setTimeout(function () { URL.revokeObjectURL(a.href); }, 1000);
      };
    }).catch(fail);
  }

  function fail(e) {
    if (e && e.denied) { lock('검토 키가 맞지 않아요.'); return; }
    $('list').innerHTML = '<p class="empty">불러오지 못했어요. 다시 시도해 주세요.</p>';
  }
  function lock(text) {
    try { sessionStorage.removeItem('wd_review'); } catch (e) { /* 없음 */ }
    token = ''; $('app').hidden = true; $('gate').hidden = false; $('gateErr').textContent = text || ''; $('stats').innerHTML = '';
  }
  function start(t) {
    token = t;
    return summary().then(function () {
      try { sessionStorage.setItem('wd_review', t); } catch (e) { /* 없음 */ }
      $('gate').hidden = true; $('app').hidden = false; open('pending');
    }, function (e) { lock(e && e.denied ? '검토 키가 맞지 않아요.' : '연결하지 못했어요.'); });
  }

  $('gate').addEventListener('submit', function (e) { e.preventDefault(); var v = $('key').value.trim(); if (v) start(v); });
  [].forEach.call(document.querySelectorAll('[data-tab]'), function (b) { b.onclick = function () { open(b.dataset.tab); }; });
  $('zoom').onclick = function () { this.hidden = true; };
  try { var saved = sessionStorage.getItem('wd_review'); if (saved) start(saved); } catch (e) { /* 없음 */ }
})();
