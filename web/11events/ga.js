/* Google Analytics(GA4) — assets/config.js 의 ga 에 측정 ID(G-XXXXXXXX)를 넣으면 켜진다. 비어 있으면 아무것도 하지 않는다.
   utm_source · utm_medium · utm_campaign 은 GA4 가 주소에서 저절로 읽는다(유입처별로 따로 보임).
   페이지 코드는 window.wdTrack(이름, 값) 으로 이벤트를 보낸다 — 걸음 인증 제출 walkingday_proof_submit · SNS 게시물 제출 walkingday_sns_submit.
   CSP(web/_headers 의 /11events*)에 googletagmanager · google-analytics 를 열어 두었다. */
(function () {
  'use strict';
  window.wdTrack = function () {};
  var id = (window.WALKINGDAY || {}).ga;
  if (!id || !/^G-[A-Z0-9]{4,20}$/.test(id)) return;
  window.dataLayer = window.dataLayer || [];
  function gtag() { window.dataLayer.push(arguments); }
  window.gtag = gtag;
  gtag('js', new Date());
  gtag('config', id);
  window.wdTrack = function (name, params) { try { gtag('event', name, params || {}); } catch (e) { /* 없음 */ } };
  var s = document.createElement('script');
  s.async = true;
  s.src = 'https://www.googletagmanager.com/gtag/js?id=' + encodeURIComponent(id);
  document.head.appendChild(s);
})();
