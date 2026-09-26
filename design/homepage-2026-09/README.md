# Handoff: StepUp 홈페이지 (stepupcrew.com)

## Overview
StepUp Android 러닝 앱의 소개 홈페이지. 5개의 전체화면 "페이지"가 세로로 쌓여 있고, 휠·키보드·스와이프로 한 화면씩 스냅 전환된다. 흐름: **달린다(스톱워치) → 모인다(크루 결성) → 쌓인다(보상·신발) → 고른다(코스) → 시작한다(다운로드)**.

## About the Design Files
이 폴더의 `StepUp Hero.dc.html`은 **HTML로 만든 디자인 레퍼런스(프로토타입)**다. 그대로 배포할 프로덕션 코드가 아니다. 목표는 이 디자인을 **대상 코드베이스 환경에서 다시 구현**하는 것이다.
- 현재 사이트는 레포 `web/` 폴더(Cloudflare Pages, **빌드 없음**, 정적 HTML/CSS/JS)이므로 **vanilla HTML + CSS + JS**로 `web/index.html`, `web/styles.css`, `web/site.js`를 교체하는 것을 권장한다.
- 프로토타입은 내부 런타임(`support.js`, `<x-dc>`, `{{ }}` 템플릿, `sc-for`/`sc-if`)을 쓴다. **이 런타임은 가져가지 말 것.** 마크업·인라인 스타일·로직(클래스 `Component`의 메서드)을 읽고 일반 DOM 코드로 옮긴다.
- 로컬에서 열어보기: 폴더를 정적 서버로 띄우고(`npx serve .`) `StepUp Hero.dc.html` 접속.

## Fidelity
**High-fidelity.** 색·타이포·간격·애니메이션 타이밍이 최종값이다. 픽셀 단위로 재현한다. 단, 화면 속 숫자·이름·코스·SUP 수치는 **예시 데이터**이며 각 카드에 "화면 예시" 배지가 붙어 있다(배지 유지).

---

## 전역 구조

```
body (overflow:hidden, bg #03060D)
└─ .root  position:fixed; inset:0
   ├─ header (absolute, top, 72px, z20)
   ├─ nav.dots (absolute, right, 세로 중앙, z20)  ← 폭 ≤680px에서 숨김
   ├─ .track  (absolute inset:0; transform: translateY(-page*100svh); transition: transform 1100ms cubic-bezier(.76,0,.18,1))
   │   ├─ section#run     (100svh)
   │   ├─ section#crew    (100svh)
   │   ├─ section#reward  (100svh)
   │   ├─ section#course  (100svh)
   │   └─ section#start   (100svh)
   └─ .loader (fixed, z200, 첫 방문만)
```

- **섹션 공통**: `height:100svh; display:flex; justify-content:center; padding:72px clamp(14px,4vw,40px) clamp(16px,3vh,32px)`
- **카드 공통**: `width:min(1200px,100%); height:100%; border-radius:28px; overflow:hidden; background:#060C18`
- 2단 카드: `display:grid; grid-template-columns:repeat(auto-fit,minmax(min(100%,340px),1fr))`. 폭 ≤680px에서 세로 2행으로 바뀌며 행 비율은 아래 각 화면 참조.

### Header
- 좌: 로고 이미지 `stepup-login.png` 높이 22px (클릭 → 1페이지)
- 우: [사운드 토글] [앱 다운로드]
  - 공통 pill: `padding:10px 20px(사운드 10px 14px); border-radius:999px; border:1px solid #ffffff2e; font 600 13–14px`; hover `background:#ffffff14`
  - 사운드: 앞에 7px 점(끔 `#5B6F8E`, 켬 `#24D8FF`), 라벨 "사운드" / "사운드 켜짐", `aria-pressed`

### Dot nav
- 5개 버튼, 세로 gap 4px, 위치 `right:max(14px, calc((100vw - 1200px)/2 - 26px))`
- 막대 4px 폭, 비활성 높이 10px `#ffffff40`, 활성 28px `#F4F8FF`, `transition: height .5s cubic-bezier(.2,.8,.2,1)`
- 활성 라벨(러닝/크루/보상/코스/시작) 12px/700 — **뷰포트 ≥1380px에서만 표시**

### 페이지 전환 규칙 (중요)
- 휠: `deltaY` 누적 > **90**(임계값) → ±1 페이지. 누적은 220ms 무입력 시 0으로 리셋. `preventDefault` 필수(`passive:false`).
- 전환 중 잠금: `lockUntil = now + 1100 + 150ms`.
- **트랙패드 관성 방지**: 잠금이 풀린 뒤에도 휠 이벤트 간격이 180ms 미만이면 계속 무시. 180ms 이상 끊긴 뒤 첫 입력부터 다시 받음.
- 키보드: ↓ / PageDown / Space = 다음, ↑ / PageUp = 이전
- 터치: 세로 스와이프 |dy| > 50px
- URL 해시 동기화: `#run #crew #reward #course #start` (`history.replaceState`). 로드 시 해시가 있으면 해당 페이지로 이동.
- **페이지 전환 오버레이 애니메이션 없음** (의도적으로 제거함. 트랙 슬라이드만).
- 각 페이지의 등장 연출은 "해당 페이지가 활성화될 때마다" 처음부터 재생되고, 떠나면 초기 상태로 리셋.

---

## Screens

### 01 러닝 (#run) — 라이브 스톱워치
**목적**: 첫인상. "지금 달리고 있다"는 감각.

- **배경**: `login-share.jpeg`(서울 야경)를 원본 + 좌우반전 복제본으로 가로로 이어 붙여 `translateX(0 → -50%)` **70s linear infinite** 로 흘림. 위에 그라데이션
  `linear-gradient(90deg,#03060DF2 0%,#03060DB3 45%,#03060D55 75%,#03060D99 100%), linear-gradient(180deg,#03060D66 0%,transparent 30%,transparent 60%,#03060DE6 100%)`
- **속도선** 5개(데스크톱만): 높이 1–2px, 폭 18–34vw, `linear-gradient(90deg,transparent,COLOR,transparent)`, `translateX(-30vw → 130vw)` 1.4–2.2s linear infinite, 지연 0–1.6s. 색 `#F4F8FF/#147BFF/#24D8FF`.
- **마우스 글로우**: 560px 원형 `radial-gradient(circle,#147BFF3D,transparent 65%)`, 카드 내 마우스 좌표를 `transform .35s ease-out`로 추적.
- **콘텐츠**(padding `clamp(24px,5vh,56px) clamp(24px,4.5vw,64px)`, 세로 space-between):
  1. 아이브로: 9px 빨간 점 `#FF3B47`(glow) + "기록 중 · 서울 나이트 런" (Barlow Condensed 700 14px, ls .06em, `#A8BDD9`)
  2. 스톱워치 `MM:SS` + `.cs`(1/100초, `#147BFF`, 0.42em) — Barlow Condensed 800, `font-size:min(19vw,24vh,250px)`, line-height .82, tabular-nums. **rAF로 실시간 증가**, 시작값 1458초(24:18). 클릭 시 랩 기록.
  3. 스탯 행(gap `clamp(20px,4vw,56px)`): 거리(`초/298` km, 소수 2자리) · 페이스 `4'58"/KM`(고정) · 심박(초마다 `164 + round(sin(s/7)*4 + rand*2)`, 옆 10px 빨간 점 `h-beat .72s`)
     - 라벨 13px `#A8BDD9`, 값 Barlow 700 `clamp(28px,5vh,44px)`, 단위 .45em `#A8BDD9`
  4. 랩 칩 행: 랩 없으면 "스톱워치를 누르면 랩이 기록돼요"(12px `#7E93B3`), 있으면 최신 3개 pill `LAP n · MM:SS · x.xxKM`(Barlow 700 15px, bg `#0B1D35CC`, border `#1A3B61`)
  5. H1 "멈추지 않는 사람을 위한<br>러닝 앱, StepUp." — Pretendard 900 `clamp(26px,min(3.2vw,5vh),44px)`, lh 1.2, ls -.045em
  6. CTA 행: [Android 앱 받기] + "Android 8.0 이상 · iPhone은 준비 중이에요"(13px `#A8BDD9`)
- **하단 진행바**: 카드 바닥 4px, 트랙 `#ffffff14`, 채움 `linear-gradient(90deg,#147BFF,#24D8FF)` = `(km % 5)/5`
- **스크롤 힌트**: 카드 우상단(top 62px, right 34px) 2×30px 트랙 + 흰 막대 `h-scroll 1.6s` + "스크롤"
- **신발**: 카드 밖으로 걸침. `right:clamp(-20px,-1vw,0px); bottom:clamp(20px,6vh,70px); width:min(46vw,64vh,600px)`
  - 이미지 `h-stride .7s ease-in-out infinite`: `0/100% translateY(0) rotate(-14deg) → 40% translateY(-22px) rotate(-9deg) → 70% translateY(-4px) rotate(-13deg)`
  - 바닥 그림자 타원 `h-shadow .7s`: scaleX 1→.72, opacity .85→.45
  - 등장 `translateX(120px)→0` 1.1s 지연 .5s
- **배지** "화면 예시": 우상단(top 18, right 22)

### 02 크루 (#crew) — 크루 결성
**목적**: 흩어진 러너가 모임 장소로 모여 크루가 완성되는 장면.
**레이아웃**: 2단. 좌 지도 / 우 현황. 모바일 행 `36% 1fr`.

- **좌 지도**: `no-crew-map.png` cover, 진입 시 `scale(1.15→1)` 2.6s. 오버레이 `linear-gradient(90deg,#060C1800 60%,#060C18 100%), radial-gradient(circle at 50% 55%,transparent 20%,#060C18AA 75%)`
  - 모임점 (50%, 55%): 120px 링 2개 `h-ring 2s` (두 번째 1s 지연), 색 결성 전 `#147BFF` / 완료 `#24D8FF`
  - 라벨 pill "여의도 물빛광장 · 20:30 출발" (모임점 아래 84px)
  - 러너 6명: 38px 원, 이니셜. 대기 `bg #0B1D35, border #3A5578` / 합류 `bg #147BFF, border #24D8FF, glow 0 0 18px #147BFFAA`. `left/top` 트랜지션 1.1s `cubic-bezier(.5,0,.2,1)`
  - 시작 좌표(%, 지도 패널 기준): 지우 (14,18) · 민재 (86,14) · 서연 (90,72) · 도윤 (18,88) · 하린 (6,52) · 태오 (70,94)
  - 도착: 모임점 기준 **반지름 48px 링**(60° 간격) — `left:calc(50% + dx px); top:calc(55% + dy px)`, (dx,dy) = (0,-48)(42,-24)(42,24)(0,48)(-42,24)(-42,-24)
  - 대기 중엔 시작점→모임점 점선(2px `#5B6F8E`, dash 4 4), 합류하면 페이드아웃
  - hover 툴팁: "이름 · 페이스 5'12"" (11px, bg `#03060DEE`)
- **우 현황**:
  - 아이브로: 점(빨강→완료 시 시안) + "크루 모집 중 · 한강 나이트 러너스" / "크루 결성 완료 · 한강 나이트 러너스"
  - 카운터 `NN` (Barlow 800 `min(9vw,16vh,132px)`, 완료 시 `#24D8FF`) + "/ 06"(`#5B6F8E`) — **한 줄 고정(nowrap)**
  - 헤드라인 "같이 뛸 사람들이 모이고 있다." → 완료 시 "크루 결성 완료. 이제 같이 달린다."
  - 명단 6행(border-bottom `#13284A`): 점 + 이름 / 상태 "이동 중 · 1.2km"(`#7E93B3`, 행 opacity .45) → "합류 완료"(`#24D8FF`, opacity 1)
  - CTA [내 크루 만들기]
- **타이밍**: 페이지 진입 후 `1100*0.7`ms 뒤부터 **900ms마다 1명씩** 합류(0→6).

### 03 보상 (#reward) — 러닝 완료 → SUP → 신발
**레이아웃**: 2단, 배경 `radial-gradient(ellipse at 72% 45%,#0F2A52 0%,#060C18 55%,#04080F 100%)`. 모바일 행 `1fr 36%`.
**시퀀스**(진입 후 `1100*0.6`ms 기준 오프셋):
| t | 단계 | 연출 |
|---|---|---|
| 0 | phase0 | 아이브로 "러닝 완료" + `5.00KM`(Barlow 800 `min(10vw,17vh,150px)`) + `24:50.00 · 4'58"/KM` — `scale(1.35)→1` 0.6s `cubic-bezier(.2,1.4,.3,1)` (도장 느낌) |
| 700ms | phase1 | 구분선 `scaleX(0→1)` .8s, "검증 완료 · 적립 예정" + `+SUP` 0→12.50 카운트업 1.3s easeOutCubic, SUP 설명 한 줄 |
| 2100ms | phase2 | 우측: 흰→시안 섬광 원 `h-flash .9s`, 신발 `rotate(-40deg) scale(.4) → rotate(-12deg) scale(1)` 1.1s `cubic-bezier(.2,1.2,.3,1)`, 블루 글로우, 점선 링 24s 회전, "새 신발 획득 / 블루 스트라이드" |
| 2900ms | phase3 | H2 "달린 만큼, 쌓인다." + [Android 앱 받기] 페이드업 |

- SUP 설명: "SUP는 GPS로 검증된 러닝 거리만큼 쌓이는 StepUp 포인트예요. GIWA 네트워크 기반으로 운영됩니다." (13px `#7E93B3`)

### 04 코스 (#course) — 코스 캐러셀 + 인앱 화면
**레이아웃**: 2단. 좌 텍스트 / 우 폰 목업. 모바일 행 `1fr 44%`.
- **배경**: 코스별 사진 크로스페이드(opacity 1.1s, 활성 시 `scale(1→1.08)` 6s linear). 오버레이 `linear-gradient(90deg,#03060DEE,#03060DAA 50%,#03060D66), linear-gradient(180deg,transparent 50%,#03060DCC)`
- **자동 넘김 5초**, ←/→ 버튼(46px 원), 카드 좌우 드래그/스와이프(|dx|>60 & |dx|>|dy|) — 수동 조작 시 타이머 리셋. 진행 세그먼트 3개(36×3px, 활성 채움 `width 0→100%` 5s linear)
- **좌 텍스트**: 아이브로 "러닝 코스" + `0N / 03` · 대형 라벨(Barlow 800 `min(9vw,17vh,150px)`, 크로스페이드) · 코스명 · 설명 · 스탯(거리/누적 상승/난이도) · H2 "오늘 달릴 길을 고른다."
- **코스 데이터(예시)**
| # | 라벨 | 이름 | 설명 | km | 상승 | 난이도 | 시간 | 페이스 | 배경 |
|---|---|---|---|---|---|---|---|---|---|
| 1 | SUNSET 5K | 광안리 해변 코스 | 해변 산책로를 따라 광안대교를 보며 달리는 평지 코스 | 5.02 | 12 | 쉬움 | 26:14 | 5'13" | weather-dusk.jpeg |
| 2 | NIGHT 7K | 민락 수변 나이트 코스 | 조명이 켜진 수변공원과 방파제를 잇는 야간 코스 | 7.18 | 24 | 보통 | 38:02 | 5'18" | gwangalli.jpeg |
| 3 | RAIN 10K | 비 오는 날 해안 코스 | 비 오는 날에도 미끄럽지 않은 포장로 위주의 장거리 코스 | 10.04 | 41 | 어려움 | 55:40 | 5'33" | weather-rain.jpeg |
- **폰 목업**: `height:min(100%,640px); aspect-ratio:9/19; border-radius:44px; border:8px solid #0E1626; bg #040811; shadow 0 50px 90px -30px #000, 0 0 0 1px #1A3B61`. 진입 `translateY(60px) rotate(4deg) → none`
  - 상단 60%: 어두운 지도(코스별 회전 0 / 38deg / -62deg) + **GPS 경로**: 바깥 선 `#2F6BFF` 9px + 안쪽 선 `#FFFFFF` 2.4px, round join/cap, 시작점 흰 원(파란 테), 끝점 흰 링. 코스 활성 시 `stroke-dashoffset 1→0` 2.2s(0.4s 지연), 끝점 2.5s 후 등장
  - 경로 생성: 코스별 기준점 사이를 8등분하고 ±3.5 지터(seeded LCG: `s=(s*9301+49297)%233280`) — 실제 구현에선 실제 GPX로 대체
  - 하단: 코스명 · `x.xxKM`(Barlow 800 46px, 단위 `#6FA0FF`) · 시간/페이스/상승 3열 · [이 코스 달리기] pill `#147BFF`
- 인앱 톤 레퍼런스: `uploads/` 의 지도 캡처(어두운 네이비 지도 + 파란/흰 이중선 경로)

### 05 시작 (#start) — 다운로드
**레이아웃**: 2단. 좌 폰 / 우 CTA. 배경 `weather-day.jpeg` opacity .55 + `linear-gradient(90deg,#03060DCC,#03060D99 50%,#03060DEE)`. 모바일 행 `44% 1fr`. 카드 아래 푸터.
- **폰 시퀀스**: 0 대기화면(로고, "오늘의 목표 5.00KM", 파란 START 원 + `h-pulse 1.6s` 링) → 1500ms START `scale(.86)` 눌림 → 1750ms 러닝 화면(빨간 점 "RUNNING", 타이머 00:00부터 rAF 증가, km) 크로스페이드 .35s
- **우측**: 아이브로 "준비 완료" · H2 "오늘의 첫 1km,<br>지금 시작한다."(Pretendard 900 `clamp(34px,min(4.4vw,7.5vh),64px)`, lh 1.1) · [Android 앱 받기](padding 19px 34px, 18px) · "Android 8.0 이상 · 약 20MB · iPhone은 준비 중이에요"
- **푸터**(13px `#7F8CA3`): "© StepUp · Powered by GIWA" | 개인정보처리방침 · 이용약관 · 문의 · GitHub

### 첫 방문 로더
- `localStorage['stepupHero.seen'] !== '1'` 일 때만. 전체화면 `#03060D`: 로고(≤300px) · "00:00.00" · 2px 진행바 `h-load .8s`
- 폰트(`document.fonts.ready`) + 히어로 이미지 로드 + 최소 800ms 모두 끝나면 opacity .5s 페이드 → 제거 → seen 저장

---

## Interactions & Behavior 요약
- 모든 주 CTA: 링크 `./downloads/StepUp-MVP.apk` + `download`. hover `#2A8BFF`, active `scale(.97)`, focus-visible `outline:3px solid #24D8FF; offset 3px`
- 다운로드 클릭 추적: `window.dataLayer.push({event:'apk_download', screen:<slug>})`
- 사운드(기본 OFF, Web Audio로 합성, 음원 파일 없음): 1페이지 발소리 350ms 간격(90→45Hz / 80→40Hz sine .12s) · 크루 합류 ping(880+1320Hz) · 보상 chime / reveal 화음 · 코스 전환 tick(1500Hz square .05s) · START beep · 페이지 전환 whoosh(밴드패스 노이즈 .45s)
- `prefers-reduced-motion: reduce` → 모든 keyframe 애니메이션 끔
- 폭 ≤680px: 점 네비·속도선 숨김, 2단 카드 세로 2행
- 선택 prop: `heroVideo`(URL) 지정 시 1페이지 배경을 `<video autoplay muted loop playsinline>`으로 교체

## State
`page(0–4)`, `joined(0–6)`, `phase(-1–3)`, `course(0–2)`, `p6(-1–2)`, `laps[]`(최대 3), `sound(bool)`, `hoverR`, `narrow(≤680)`, `wide(≥1380)`, `loading`. 페이지 변경 시 모든 타이머/rAF 정리 후 해당 페이지 시퀀스 시작.

## Design Tokens
**Colors**
| 이름 | 값 |
|---|---|
| bg page | `#03060D` |
| bg card | `#060C18` |
| surface | `#0B1D35` / `#07142A` |
| line | `#13284A` / `#1A3B61` |
| primary blue | `#147BFF` (hover `#2A8BFF`) |
| cyan accent | `#24D8FF` |
| route blue (인앱) | `#2F6BFF` |
| text | `#F4F8FF` |
| text sub | `#A8BDD9` |
| text muted | `#7E93B3` / `#5B6F8E` |
| live red | `#FF3B47` |

**Type**
- Pretendard (400/600, 700–900) — 한글 본문·헤드라인 (`web/assets/fonts/`)
- Barlow Condensed 500/700/800 (Google Fonts) — 숫자·라벨
- H1/H2 900, ls -.045em, lh 1.2 · 본문 13–16px · 라벨 13–14px

**Radius**: 카드 28 · 폰 44 · pill 999 · 칩 8–16
**Easing**: 트랙 `cubic-bezier(.76,0,.18,1)` 1100ms · 등장 `cubic-bezier(.2,.8,.2,1)` · 튕김 `cubic-bezier(.2,1.2,.3,1)`
**Shadow**: CTA `0 12px 36px #147BFF55` · 신발 `drop-shadow(0 30px 40px #000000aa)`

## Assets
| 파일 | 용도 | 출처 |
|---|---|---|
| design/s2/cutouts/brand/stepup-login.png | 로고(헤더·로더·폰) | 레포 S2 |
| design/s2/cutouts/shoes/shoe-main.png | 신발 | 레포 S2 |
| design/s2/originals/backgrounds/login-share.jpeg | 01 배경 | 레포 S2 |
| design/s2/originals/reference/maps/no-crew-map.png | 02 지도, 04 인앱 지도 | 레포 S2 |
| weather-dusk / gwangalli / weather-rain.jpeg | 04 코스 배경 | 레포 S2 |
| weather-day.jpeg | 05 배경 | 레포 S2 |
| design/brand/stepup-app-icon.webp | 파비콘 | 레포 brand |
| og-image.png (1200×630) | 공유 미리보기 | 이번 작업에서 생성 |
| web/assets/fonts/pretendard-*.otf | 폰트 | 레포 web |

**교체 필요(실소재)**: 01 러너 영상/사진, 코스별 실제 현장 사진 3장, 새 다크·블루 앱 실제 캡처, 크루원 프로필, 문의 이메일·사업자 정보·SNS.

## Files
- `StepUp Hero.dc.html` — 전체 디자인(마크업 + 로직 클래스)
- `support.js` — 프로토타입 런타임(열람용, 이식 금지)
- `CLAUDE_CODE_PROMPT.md` — Claude Code에 붙여넣을 구현 지시서
- 나머지 — 에셋(레포와 같은 경로 구조)
