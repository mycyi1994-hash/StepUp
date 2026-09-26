# Claude Code 구현 지시서 (복사해서 붙여넣기)

---

StepUp 레포(`mycyi1994-hash/StepUp`)의 소개 사이트 `web/`을 새 디자인으로 교체해줘.

## 기준 자료
- `design_handoff_stepup_homepage/README.md` — 모든 수치·색·타이밍의 단일 기준. 여기 적힌 값을 그대로 쓸 것.
- `design_handoff_stepup_homepage/StepUp Hero.dc.html` — 디자인 레퍼런스. 마크업과 인라인 스타일, `class Component`의 메서드(스톱워치, 페이지 전환, 크루 합류, 보상 시퀀스, 코스 캐러셀, 폰 시퀀스, 사운드)를 읽고 로직을 옮길 것.
- 이 HTML의 런타임(`support.js`, `<x-dc>`, `{{ }}`, `sc-for`, `sc-if`, `DCLogic`)은 **가져오지 말 것**.

## 제약
- `web/`는 Cloudflare Pages에 **빌드 없이** 올라간다. 번들러·프레임워크·npm 의존성 추가 금지. **vanilla HTML + CSS + JS(ES2019, 모듈 1개)**로 작성.
- 교체 대상: `web/index.html`, `web/styles.css`, `web/site.js`. 다른 페이지(`privacy.html`, `terms.html`, `wallet.html`, `invite.html`, `delete-account.html`, `draw.html`)와 `_redirects`, `.well-known/`은 건드리지 말 것.
- 에셋은 `web/assets/` 아래로 복사해서 참조(`design/` 경로를 직접 참조하지 말 것). 예: `web/assets/img/login-share.jpeg`, `web/assets/img/shoe-main.png`, `web/assets/img/stepup-logo.png`, `web/assets/img/no-crew-map.png`, `web/assets/img/course-*.jpeg`, `web/assets/img/start-bg.jpeg`, `web/assets/og-image.png`. 대용량 JPEG은 WebP로 변환(품질 80)해도 됨.
- 폰트: 기존 `web/assets/fonts/pretendard-*.otf` 재사용, Barlow Condensed는 Google Fonts 링크.
- 다운로드 링크는 기존과 동일하게 `./downloads/StepUp-MVP.apk`.

## 구현 순서
1. **골격**: 고정 루트 + 헤더 + 점 네비 + 5개 섹션 트랙. CSS 변수로 토큰 정의(README "Design Tokens").
2. **페이지 전환**: 휠 누적 임계값 90, 220ms 리셋, 잠금 1250ms, **트랙패드 관성 방지(180ms 간격 규칙)**, 키보드, 터치 스와이프, URL 해시(`#run #crew #reward #course #start`) 동기화 및 초기 진입. **전환 오버레이 애니메이션은 넣지 말 것**(트랙 슬라이드만).
3. **01 러닝**: 흐르는 야경, 속도선, 마우스 글로우, rAF 스톱워치/거리/심박/진행바, 랩 기록(최대 3), 신발 스트라이드, 스크롤 힌트.
4. **02 크루**: 러너 6명 좌표 이동(900ms 간격), 점선, 링, 카운터 nowrap, 명단 상태, hover 툴팁, 완료 상태 전환.
5. **03 보상**: 4단계 시퀀스(0 / 700 / 2100 / 2900ms), SUP 카운트업.
6. **04 코스**: 3개 코스 크로스페이드, 5초 자동, 버튼·드래그, 진행 세그먼트, 폰 목업의 GPS 이중선 경로 draw 애니메이션.
7. **05 시작**: 폰 START 눌림 → 러닝 타이머, CTA, 푸터.
8. **첫 방문 로더**, **사운드(Web Audio, 기본 OFF)**, `dataLayer` 다운로드 추적.
9. **반응형**: ≤680px에서 점 네비·속도선 숨김, 2단 카드 행 비율 README대로. `100svh` 사용.
10. **접근성**: `prefers-reduced-motion`에서 애니메이션 끔, 버튼 `aria-label`, focus-visible 링, 이미지 alt.
11. **메타**: title/description/OG(1200×630)/twitter card/theme-color/favicon.

## 완료 기준
- 1920×1080, 1440×900, 1280×720, 924×540, 390×844에서 겹침·잘림 없음(특히 02 카운터 "/ 06" 한 줄, 01 CTA 옆 iPhone 안내, "화면 예시" 배지 nowrap).
- 트랙패드로 한 번 쓸었을 때 **정확히 한 페이지**만 이동.
- 페이지 재진입 시 각 시퀀스가 처음부터 재생되고, 떠나면 타이머/rAF가 모두 정리됨(콘솔 경고·메모리 누수 없음).
- 콘솔 에러 0, Lighthouse 성능 80+ (이미지 WebP·preload 적용).
- 예시 데이터가 있는 카드에는 "화면 예시" 배지 유지. 실제 보상 수치로 오인되지 않게 할 것.

작업 후 변경 파일 목록, 각 뷰포트 스크린샷, 남은 TODO(실소재 교체 목록)를 보고해줘.
