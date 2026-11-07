// 홈페이지 방문 통계 — Google Analytics 4(속성 'StepUp 웹', 2026-11-07). 11.11 워킹데이 페이지는 11events/ga.js(설정 assets/config.js 의 ga)가
// 같은 측정 ID · 같은 설정으로 켠다 — ID 를 바꾸면 둘을 함께(csp.test.js 가 맞대어 본다).
// 앱의 Firebase Analytics(stepup-16d6e)와는 다른 속성이다. 인라인 스크립트를 막은 페이지(CSP)라 gtag 기본 코드를 이 파일에 둔다.
// 운영 주소에서만 켠다(미리보기 · 로컬은 세지 않는다). 광고 신호는 끈다(개인정보처리방침 "광고 쿠키 없음").
// 이벤트에는 이메일 · 걸음 수 · 추천 코드 같은 개인 정보를 넣지 않는다(GA 약관). 이 파일을 넣는 페이지는 _headers CSP 에 Google 주소가 있어야 한다.
(function () {
  var ID = 'G-3D42E2KKG1';
  var host = location.hostname;
  if (host !== 'stepupcrew.com' && host !== 'www.stepupcrew.com') return;
  var w = window;
  w.dataLayer = w.dataLayer || [];
  w.gtag = function () { w.dataLayer.push(arguments); };
  w.gtag('js', new Date());
  w.gtag('config', ID, { allow_google_signals: false, allow_ad_personalization_signals: false });
  var s = document.createElement('script');
  s.async = true;
  s.src = 'https://www.googletagmanager.com/gtag/js?id=' + ID;
  document.head.appendChild(s);
})();
