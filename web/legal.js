// 개인정보처리방침 · 이용약관 — #en 으로 열면(홈페이지 영어 바닥글 · 동의 설명 창) 위 메뉴 · 제목 · 탭 이름 · 문서 언어도 영어로.
// 영어 본문은 같은 페이지 아래 <section id="en"> 에 있다(2차 QA: 영어로 왔는데 머리는 한국어였다).
(function () {
  var ko = new Map();
  function apply() {
    var en = location.hash === '#en';
    document.documentElement.lang = en ? 'en' : 'ko';
    document.querySelectorAll('[data-en]').forEach(function (el) {
      if (!ko.has(el)) ko.set(el, el.textContent);
      el.textContent = en ? el.getAttribute('data-en') : ko.get(el);
    });
    // 위 메뉴도 고른 언어로 이어지게(홈은 ?lang=en, 다른 문서는 #en)
    document.querySelectorAll('nav ul a').forEach(function (a) {
      var base = a.getAttribute('data-href') || a.getAttribute('href');
      a.setAttribute('data-href', base);
      a.setAttribute('href', !en ? base : base === 'index.html' ? 'index.html?lang=en' : /\.html$/.test(base) ? base + '#en' : base);
    });
    document.querySelectorAll('.lang a').forEach(function (a) {
      a.classList.toggle('on', a.getAttribute('href') === (en ? '#en' : '#ko'));
    });
  }
  apply();
  window.addEventListener('hashchange', apply);
})();
