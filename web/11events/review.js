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
      $('auto').hidden = s.auto !== true;
      $('stats').innerHTML = '대기 <b>' + num(s.pending) + '</b> · 승인 <b>' + num(s.approved) + '</b> · 거절 <b>' + num(s.rejected) +
        '</b> · 합계 <b>' + num(s.total) + '</b>걸음 · 인증 <b>' + num(s.people) + '</b>명 · 사전 등록 <b>' + num(s.registered) + '</b>명' +
        (s.sns_pending != null ? ' · SNS 대기 <b>' + num(s.sns_pending) + '</b> · SNS 승인 <b>' + num(s.sns_approved) + '</b>' : '');
    });
  }

  function open(t) {
    tab = t; after = 0;
    [].forEach.call(document.querySelectorAll('[data-tab]'), function (b) { b.setAttribute('aria-selected', String(b.dataset.tab === t)); });
    $('list').innerHTML = '';
    if (t === 'entries') entries(); else if (t === 'winners') winners(); else if (t.indexOf('sns') === 0) snsOpen(t); else more();
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
      '<div class="meta"><b>' + dayName(r.day) + ' · ' + num(r.steps) + '걸음</b><br>' + esc(r.email) + '<br>' + (r.phone ? '☎ ' + esc(r.phone) : '<span class="status">번호 없음(0100 전 제출)</span>') + '<br><span class="status">제출 ' + when(r.submitted_at) +
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
      var head = ['순위', '이메일', '전화번호', '응모권', '친구 초대', '달성 일수', '최고 걸음', '1,111 달성', '11/11 달성', '초대한 사람', '등록'];
      var csv = [head.join(',')];
      var html = '<div class="bar"><span class="status">초대 많은 순 → 응모권 많은 순. 초대 순위 경품은 위에서 11명(친구가 1,111걸음 이상 승인을 받은 수). 달성 일수 = 1,111걸음 이상 승인된 날 — 5일 개근이면 응모권이 이미 1.5배(올림)로 들어가 있어요.</span>' +
        '<button class="btn" type="button" id="csv">CSV 내려받기</button></div><div class="tablewrap"><table><thead><tr>' +
        head.map(function (h, i) { return '<th' + (i >= 3 && i <= 6 ? ' class="n"' : '') + '>' + h + '</th>'; }).join('') + '</tr></thead><tbody>';
      rows.forEach(function (r, i) {
        var cells = [i + 1, r.email, r.phone || '', r.tickets, r.friends, r.approved_days, r.best_steps || '', r.goal_met ? 'O' : '', r.goal_on_1111 ? 'O' : '', r.invited_by || '', when(r.registered_at)];
        csv.push(cells.map(function (c) { c = String(c); return /[",\n]/.test(c) ? '"' + c.replace(/"/g, '""') + '"' : c; }).join(','));
        html += '<tr>' + cells.map(function (c, j) { return '<td' + (j >= 3 && j <= 6 ? ' class="n"' : '') + '>' + esc(j === 6 && c ? num(c) : c) + '</td>'; }).join('') + '</tr>';
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

  /* ── SNS 게시물(0093): 대기 · 승인 · 거절 목록과 추첨기 SNS 명단 CSV ── */
  var CHN = { x: 'X', instagram: '인스타', threads: '스레드' };
  var snsAfter = 0, snsStatus = 'pending';
  function snsMore() {
    var list = $('list'), grid = list.querySelector('.grid'), btn = list.querySelector('.more');
    if (!grid) { grid = document.createElement('div'); grid.className = 'grid'; list.appendChild(grid); }
    if (btn) btn.remove();
    return rpc('walkingday_sns_list', { p_token: token, p_status: snsStatus, p_after: snsAfter }).then(function (rows) {
      rows.forEach(function (r) { grid.appendChild(snsCard(r)); snsAfter = Math.max(snsAfter, r.id); });
      if (!grid.children.length) list.innerHTML = '<p class="empty">' + (snsStatus === 'pending' ? '확인할 게시물이 없어요.' : '없어요.') + '</p>';
      if (rows.length === 20) {
        var m = document.createElement('button'); m.className = 'btn more'; m.type = 'button'; m.textContent = '더 보기';
        m.onclick = function () { m.disabled = true; snsMore(); };
        list.appendChild(m);
      }
    }).catch(fail);
  }
  function snsCard(r) {
    var el = document.createElement('div'); el.className = 'item';
    el.innerHTML =
      '<div class="meta"><b>' + esc(CHN[r.channel] || r.channel) + ' · @' + esc(r.account) + '</b><br>' +
      '<a href="' + esc(r.url) + '" target="_blank" rel="noopener noreferrer" style="color:var(--blue);word-break:break-all">' + esc(r.url) + '</a><br>' +
      esc(r.email) + (r.phone ? ' · ☎ ' + esc(r.phone) : ' · 번호 없음') + (r.registered ? ' · 사전 등록' : ' · 사전 등록 안 함') + ' · 승인된 게시물 ' + num(r.approved_posts) + '개<br>' +
      '<span class="status">제출 ' + when(r.submitted_at) + (r.reviewed_by ? ' · ' + esc(r.reviewed_by) : '') + (r.note ? ' · ' + esc(r.note) : '') + '</span></div>' +
      (r.dup_client > 0 ? '<p class="warn">같은 곳(IP)에서 다른 이메일 ' + r.dup_client + '개가 제출했어요 — 다중 계정 확인</p>' : '') +
      '<p class="status">링크를 열어 해시태그 4개(#스텝업 #스텝업챌린지 #StepUp #StepUpChallenge) · 전체 공개 · 11/7 이후 게시인지 확인</p>' +
      '<div class="row"><input type="text" maxlength="40" aria-label="계정 아이디" value="' + esc(r.account) + '"></div>' +
      '<div class="row"><input type="text" maxlength="200" placeholder="메모(거절 까닭 등)" aria-label="메모" value="' + esc(r.note || '') + '"></div>' +
      '<div class="act">' +
      (r.status !== 'approved' ? '<button class="ok" data-s="approved" type="button">승인</button>' : '') +
      (r.status !== 'rejected' ? '<button class="no" data-s="rejected" type="button">거절</button>' : '') +
      (r.status !== 'pending' ? '<button class="re" data-s="pending" type="button">대기로</button>' : '') + '</div>';
    var inputs = el.querySelectorAll('input');
    [].forEach.call(el.querySelectorAll('[data-s]'), function (b) {
      b.onclick = function () {
        var acct = inputs[0].value.trim();
        [].forEach.call(el.querySelectorAll('button'), function (x) { x.disabled = true; });
        rpc('walkingday_sns_set', { p_token: token, p_id: r.id, p_status: b.dataset.s, p_note: inputs[1].value, p_account: acct !== r.account ? acct : null })
          .then(function (d) {
            if (!d || !d.ok) throw new Error((d && d.code) || 'fail');
            el.style.opacity = '.4';
            el.querySelector('.act').innerHTML = '<p class="status">' + { approved: '승인했어요', rejected: '거절했어요', pending: '대기로 돌렸어요' }[b.dataset.s] + '</p>';
            summary();
          })
          .catch(function (e) { [].forEach.call(el.querySelectorAll('button'), function (x) { x.disabled = false; }); $('list').insertAdjacentHTML('afterbegin', '<p class="warn">저장하지 못했어요: ' + esc(e.message) + '</p>'); });
      };
    });
    return el;
  }
  // 추첨기 SNS 명단 양식(SNS_명단_양식.csv)과 같은 머리줄 — 상태 「확인됨」 줄만 추첨에 들어간다
  function snsRoster() {
    var list = $('list');
    list.innerHTML = '<p class="empty">불러오는 중…</p>';
    rpc('walkingday_sns_roster', { p_token: token }).then(function (rows) {
      var head = ['채널', '계정', '게시 URL', '이메일', '확인자', '확인 시각', '상태', '인정 게시물 수', '전화번호'];
      var csv = [head.join(',')];
      var html = '<div class="bar"><span class="status">승인된 게시물을 이메일 · 채널 · 계정마다 묶었어요. 이 CSV 를 추첨기 「2. SNS 명단」에 그대로 넣으면 돼요.</span>' +
        '<button class="btn" type="button" id="csv">CSV 내려받기</button></div><div class="tablewrap"><table><thead><tr>' +
        head.map(function (h, i) { return '<th' + (i === 7 ? ' class="n"' : '') + '>' + h + '</th>'; }).join('') + '</tr></thead><tbody>';
      rows.forEach(function (r) {
        var cells = [CHN[r.channel] || r.channel, r.account, r.url, r.email, r.reviewed_by || '', when(r.reviewed_at), '확인됨', r.posts, r.phone || ''];
        csv.push(cells.map(function (c) { c = String(c); return /[",\n]/.test(c) ? '"' + c.replace(/"/g, '""') + '"' : c; }).join(','));
        html += '<tr>' + cells.map(function (c, j) { return '<td' + (j === 7 ? ' class="n"' : '') + '>' + esc(c) + '</td>'; }).join('') + '</tr>';
      });
      if (!rows.length) html += '<tr><td colspan="9">아직 승인된 게시물이 없어요.</td></tr>';
      list.innerHTML = html + '</tbody></table></div>';
      $('csv').onclick = function () {
        var blob = new Blob(['﻿' + csv.join('\n')], { type: 'text/csv;charset=utf-8' });
        var a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'SNS_명단.csv'; a.click();
        setTimeout(function () { URL.revokeObjectURL(a.href); }, 1000);
      };
    }).catch(fail);
  }
  function snsOpen(t) {
    if (t === 'sns_roster') return snsRoster();
    snsStatus = t === 'sns_approved' ? 'approved' : t === 'sns_rejected' ? 'rejected' : 'pending'; snsAfter = 0; snsMore();
  }

  /* ── 당첨자 발표(0102): 초대 순위 11명은 채우기 버튼, 빼빼로 · 무릎보호대는 추첨기 결과를 붙여 넣기 → 저장 → 페이지에 발표 ── */
  var PRIZES = [
    { k: 'mat', name: '요가매트 세트', who: '초대 1위', seats: 1, from: 1 },
    { k: 'roller', name: '폼롤러', who: '초대 2~4위', seats: 3, from: 2 },
    { k: 'socks', name: '운동양말', who: '초대 5~11위', seats: 7, from: 5 },
    { k: 'pepero', name: '빼빼로 기프티콘', who: '사이트 · SNS 응모권 추첨', seats: 95 },
    { k: 'knee', name: '무릎보호대', who: '걸음 인증자 추첨', seats: 5 }
  ];
  var EMAIL_RE = /[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g;
  function emailsIn(text) {
    var seen = {}, out = [];
    (String(text || '').match(EMAIL_RE) || []).forEach(function (e) { e = e.toLowerCase(); if (!seen[e]) { seen[e] = 1; out.push(e); } });
    return out;
  }
  function csvCell(c) { c = String(c == null ? '' : c); return /[",\n]/.test(c) ? '"' + c.replace(/"/g, '""') + '"' : c; }
  function winners(note) {
    var list = $('list');
    list.innerHTML = '<p class="empty">불러오는 중…</p>';
    return rpc('walkingday_review_winners', { p_token: token }).then(function (d) {
      var saved = d.rows || [], published = !!d.published;
      var html = '<div class="win"><p class="pub ' + (published ? 'on">지금 이벤트 페이지에 발표돼 있어요(가린 이메일로). 고쳐 저장하면 페이지도 바로 바뀌어요.' : 'off">아직 발표 전이에요. 저장해도 「페이지에 발표」를 누르기 전에는 안 보여요.') + '</p>' +
        '<ol><li>「초대 순위로 채우기」 — 응모권 명단의 초대 순위 위 11명(친구 수 → 응모권 → 먼저 등록한 순, 리더보드와 같은 순서)을 세 칸에 넣어요.</li>' +
        '<li>「빼빼로 · 무릎보호대 추첨하기」 — 초대 순위 11명과 「뺄 사람」 칸을 빼고 <b>무릎보호대 5명(걸음 인증이 승인된 사람, 한 사람 한 번씩)</b> → <b>빼빼로 95명(사이트 응모권 + SNS 응모권, 장 수만큼 확률)</b> 차례로 빈자리만 뽑아요. (다른 추첨기로 뽑았다면 칸에 붙여 넣어도 돼요 — 이메일만 골라 읽어요.)</li>' +
        '<li>다중 계정 · 연락 안 되는 사람은 그 칸에서 지우고 「뺄 사람」 칸에 넣은 뒤 다시 「추첨하기」 — 지운 자리만 새로 뽑아요. 한 사람은 한 경품만(겹치면 저장이 안 돼요).</li>' +
        '<li>「저장」 → 확인 → 「페이지에 발표」. 이벤트 페이지 맨 위 순위 칸 위에 당첨자 명단이 떠요.</li></ol>' +
        '<div class="wbar"><button class="btn sub" type="button" id="wFill">초대 순위로 채우기</button><button class="btn sub" type="button" id="wDraw">빼빼로 · 무릎보호대 추첨하기</button></div>' +
        '<p class="status" id="wTie"></p><p class="status" id="wDrawNote"></p>';
      PRIZES.forEach(function (z) {
        var mine = saved.filter(function (r) { return r.prize === z.k; }).map(function (r) { return r.email; });
        html += '<div class="wf' + (z.seats > 3 ? ' big' : '') + '"><label for="w_' + z.k + '">' + z.name + ' · ' + z.who + ' <span id="c_' + z.k + '"></span></label>' +
          '<textarea id="w_' + z.k + '" spellcheck="false" placeholder="이메일을 한 줄에 하나씩(붙여 넣기)">' + esc(mine.join('\n')) + '</textarea></div>';
      });
      html += '<div class="wf"><label for="w_ex">추첨에서 뺄 사람(다중 계정 등) <span>저장되지 않아요 · 추첨할 때만</span></label>' +
        '<textarea id="w_ex" spellcheck="false" placeholder="이메일을 한 줄에 하나씩"></textarea></div>';
      html += '<p class="wmsg" id="wMsg" role="status"></p><div class="wbar">' +
        '<button class="btn" type="button" id="wSave">저장</button>' +
        '<button class="btn" type="button" id="wPub" style="background:' + (published ? 'var(--red)' : 'var(--green)') + '">' + (published ? '발표 내리기' : '페이지에 발표') + '</button>' +
        '<button class="btn sub" type="button" id="wCsv"' + (saved.length ? '' : ' disabled') + '>저장된 명단 CSV(번호 포함)</button></div></div>';
      list.innerHTML = html;

      function rows() {
        var out = [];
        PRIZES.forEach(function (z) { emailsIn($('w_' + z.k).value).forEach(function (e, i) { out.push({ email: e, prize: z.k, place: z.from ? z.from + i : null }); }); });
        return out;
      }
      function count() {
        var all = rows(), by = {}, dup = {};
        all.forEach(function (r) { by[r.prize] = (by[r.prize] || 0) + 1; dup[r.email] = (dup[r.email] || 0) + 1; });
        PRIZES.forEach(function (z) { var n = by[z.k] || 0, c = $('c_' + z.k); c.textContent = n + ' / ' + z.seats + '명'; c.className = n > z.seats ? 'over' : ''; });
        var twice = Object.keys(dup).filter(function (e) { return dup[e] > 1; });
        var m = $('wMsg');
        if (twice.length) { m.className = 'wmsg bad'; m.textContent = '두 경품에 같은 사람: ' + twice.join(', ') + ' — 한 곳에서 지우세요(1인 1경품).'; }
        else { m.className = 'wmsg'; m.textContent = '모두 ' + all.length + '명'; }
        return { all: all, twice: twice, by: by };
      }
      PRIZES.forEach(function (z) { $('w_' + z.k).addEventListener('input', count); });
      count();
      if (note) { $('wMsg').className = 'wmsg good'; $('wMsg').textContent = note; }

      // 추첨: 빈자리만. 가중치 있는 비복원 추첨(키 = -ln(u) / 장 수, 작은 순) — u 는 crypto 난수
      function rand01() { var a = new Uint32Array(1); crypto.getRandomValues(a); return (a[0] + 1) / 4294967297; }
      function pick(pool, n) {
        if (n <= 0) return [];
        return pool.map(function (p) { return { e: p.email, k: -Math.log(rand01()) / p.w }; })
          .sort(function (a, b) { return a.k - b.k; }).slice(0, n).map(function (x) { return x.e; });
      }
      $('wDraw').onclick = function () {
        var m = $('wMsg');
        var inv = ['mat', 'roller', 'socks'].reduce(function (n, k) { return n + emailsIn($('w_' + k).value).length; }, 0);
        if (!inv) { m.className = 'wmsg bad'; m.textContent = '먼저 「초대 순위로 채우기」를 눌러 주세요 — 초대 순위 당첨자는 추첨에서 빠져야 해요(1인 1경품).'; return; }
        busy(true);
        Promise.all([rpc('walkingday_review_entries', { p_token: token }), rpc('walkingday_sns_roster', { p_token: token })]).then(function (res) {
          var ent = res[0] || [], sns = res[1] || [], taken = {}, posts = {}, tk = {}, perfect = {}, all = {};
          PRIZES.forEach(function (z) { emailsIn($('w_' + z.k).value).forEach(function (e) { taken[e] = 1; }); });
          emailsIn($('w_ex').value).forEach(function (e) { taken[e] = 1; });
          sns.forEach(function (r) { var e = String(r.email || '').toLowerCase(); if (e) { posts[e] = Math.min(15, (posts[e] || 0) + (+r.posts || 0)); all[e] = 1; } });
          ent.forEach(function (r) { var e = String(r.email || '').toLowerCase(); tk[e] = +r.tickets || 0; perfect[e] = (+r.approved_days || 0) >= 5; all[e] = 1; });
          // 무릎보호대: 걸음 인증이 하나라도 승인된 사람, 한 사람 한 번씩
          var kneePool = ent.filter(function (r) { return r.best_steps != null && !taken[String(r.email).toLowerCase()]; })
            .map(function (r) { return { email: String(r.email).toLowerCase(), w: 1 }; });
          var kneeHave = emailsIn($('w_knee').value), kneeNew = pick(kneePool, 5 - kneeHave.length);
          kneeNew.forEach(function (e) { taken[e] = 1; });
          $('w_knee').value = kneeHave.concat(kneeNew).join('\n');
          // 빼빼로: 사이트 응모권(개근 1.5배 이미 들어 있음) + SNS 응모권(개근이면 1.5배 올림)
          var pepPool = Object.keys(all).filter(function (e) { return !taken[e]; }).map(function (e) {
            var p = posts[e] || 0; return { email: e, w: (tk[e] || 0) + (perfect[e] ? Math.ceil(p * 1.5) : p) };
          }).filter(function (p) { return p.w > 0; });
          var pepHave = emailsIn($('w_pepero').value), pepNew = pick(pepPool, 95 - pepHave.length);
          $('w_pepero').value = pepHave.concat(pepNew).join('\n');
          var tickets = pepPool.reduce(function (n, p) { return n + p.w; }, 0);
          $('wDrawNote').textContent = '추첨 ' + new Date().toLocaleString('ko-KR', { timeZone: 'Asia/Seoul' }) + ' · 무릎보호대: 걸음 인증자 ' + kneePool.length + '명 중 ' + kneeNew.length + '명 새로 · ' +
            '빼빼로: ' + pepPool.length + '명 · 응모권 ' + tickets.toLocaleString('ko-KR') + '장 중 ' + pepNew.length + '명 새로. 아직 저장 전이에요 — 확인하고 「저장」.';
          count();
        }).catch(function (e) { if (e && e.denied) return fail(e); m.className = 'wmsg bad'; m.textContent = '명단을 불러오지 못했어요 — 다시 눌러 주세요.'; })
          .then(function () { if ($('wDraw')) busy(false); });
      };
      $('wFill').onclick = function () {
        var b = this; b.disabled = true;
        rpc('walkingday_review_entries', { p_token: token }).then(function (ent) {
          var top = ent.filter(function (r) { return r.friends > 0; });
          ['mat', 'roller', 'socks'].forEach(function (k) {
            var z = PRIZES.filter(function (x) { return x.k === k; })[0];
            $('w_' + k).value = top.slice(z.from - 1, z.from - 1 + z.seats).map(function (r) { return r.email; }).join('\n');
          });
          // 경품이 갈리는 자리(1|2 · 4|5 · 11|12)에서 친구 수가 같으면 무엇으로 갈랐는지 알린다
          var ties = [1, 4, 11].map(function (n) {
            var a = top[n - 1], c = top[n];
            if (!a || !c || a.friends !== c.friends) return '';
            return n + '위 · ' + (n + 1) + '위가 친구 ' + a.friends + '명으로 같아요 → ' +
              (a.tickets !== c.tickets ? '응모권 ' + a.tickets + '장 vs ' + c.tickets + '장으로 갈랐어요' : '응모권도 ' + a.tickets + '장으로 같아 먼저 등록한 사람(' + when(a.registered_at) + ')이 앞이에요') + '.';
          }).filter(Boolean);
          $('wTie').textContent = (top.length < 11 ? '친구를 인증시킨 사람이 ' + top.length + '명뿐이라 ' + top.length + '명만 채웠어요. ' : '') + ties.join(' ');
          count();
        }).catch(fail).then(function () { b.disabled = false; });
      };

      function save() {
        var c = count(), m = $('wMsg');
        if (c.twice.length) return Promise.reject(new Error('같은 사람이 두 경품에 있어요'));
        var over = PRIZES.filter(function (z) { return (c.by[z.k] || 0) > z.seats; });
        if (over.length) return Promise.reject(new Error(over[0].name + ' 자리(' + over[0].seats + '명)를 넘었어요'));
        return rpc('walkingday_review_winners_set', { p_token: token, p_rows: c.all }).then(function (r) {
          if (r && r.ok) { m.className = 'wmsg good'; m.textContent = '저장했어요 · ' + r.count + '명'; return r; }
          var why = !r ? '' : r.code === 'unknown' ? r.email + ' — 이벤트에 들어온 적 없는 이메일이에요(오타?)'
            : r.code === 'duplicate' ? r.email + ' — 두 경품에 같은 사람(1인 1경품)'
            : r.code === 'seats' ? '자리 수를 넘었어요(' + r.prize + ' ' + r.seats + '명)'
            : r.code === 'email' ? r.email + ' — 이메일 모양이 아니에요' : (r.code || '');
          throw new Error(why);
        });
      }
      function busy(on) { ['wSave', 'wPub', 'wFill', 'wDraw'].forEach(function (id) { $(id).disabled = on; }); }
      function oops(e) {
        if (e && e.denied) return fail(e);
        var m = $('wMsg'); m.className = 'wmsg bad'; m.textContent = '저장하지 못했어요: ' + ((e && e.message) || '다시 시도해 주세요');
      }
      $('wSave').onclick = function () {
        busy(true);
        save().then(function (r) { return winners('저장했어요 · ' + r.count + '명'); }).catch(oops).then(function () { if ($('wSave')) busy(false); });
      };
      $('wPub').onclick = function () {
        if (published) {
          if (!confirm('이벤트 페이지에서 당첨자 명단을 내릴까요?')) return;
          busy(true);
          rpc('walkingday_review_winners_publish', { p_token: token, p_on: false }).then(function () { return winners('발표를 내렸어요'); }).catch(oops).then(function () { if ($('wSave')) busy(false); });
          return;
        }
        var c = count(), short = PRIZES.filter(function (z) { return (c.by[z.k] || 0) < z.seats; })
          .map(function (z) { return z.name + ' ' + (c.by[z.k] || 0) + '/' + z.seats; });
        if (!confirm('지금 칸 내용을 저장하고 이벤트 페이지에 발표할까요?\n(이메일은 가려서 보여요 · 번호는 안 나가요)' + (short.length ? '\n\n아직 덜 찬 경품: ' + short.join(', ') : ''))) return;
        busy(true);
        save().then(function () { return rpc('walkingday_review_winners_publish', { p_token: token, p_on: true }); })
          .then(function (r) { if (!r || !r.ok) throw new Error(r && r.code === 'empty' ? '명단이 비어 있어요' : '발표하지 못했어요'); return winners('저장하고 발표했어요 · 이벤트 페이지에서 확인해 보세요'); })
          .catch(oops).then(function () { if ($('wSave')) busy(false); });
      };
      $('wCsv').onclick = function () {
        var name = {}; PRIZES.forEach(function (z) { name[z.k] = z.name; });
        var csv = ['경품,순위,이메일,전화번호'].concat(saved.map(function (r) { return [name[r.prize] || r.prize, r.place || '', r.email, r.phone || ''].map(csvCell).join(','); }));
        var blob = new Blob(['﻿' + csv.join('\n')], { type: 'text/csv;charset=utf-8' });
        var a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = 'walkingday-winners.csv'; a.click();
        setTimeout(function () { URL.revokeObjectURL(a.href); }, 1000);
      };
    }).catch(fail);
  }
})();
