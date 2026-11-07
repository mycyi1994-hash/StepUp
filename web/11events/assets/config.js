/* 실제 배포 설정. url·key: 공개 합계(walkingday_total — 승인된 걸음의 합, 60초마다)와 인증 제출이 쓰는 공개값(web/waitlist.js 와 같다).
   total: 서버 없이 숫자를 직접 보일 때만(url 을 비우고). ga: Google Analytics(GA4) 측정 ID(G-XXXXXXXX) — 넣으면 ga.js 가 켠다, 비우면 꺼짐. proof: 걸음 인증 페이지 — 11/7 0시~11/11 15:00 동안 주 버튼이 「걸음 인증하기」로 바뀜 */
window.WALKINGDAY = { url: "https://pupjzcmybuoyhzfwrsdf.supabase.co", key: "sb_publishable_jt74AKM32zdqnJlsFHEo2g_MNHa-WRO", total: null, proof: "/11events/proof", share: "https://stepupcrew.com/11events/", preview: location.hash === "#preview", ga: "" };
