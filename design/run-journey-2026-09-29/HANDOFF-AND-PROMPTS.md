# StepUp 화면 수정 및 공통 등급 배지

내장 image_gen으로 생성한 이미지 시안. 앱 코드 변경 없음.

## 이번 수정
- 표시된 순서: 1 러닝 중, 2 메인, 3 러닝 후.
- 러닝 중 평균속도 삭제 요청은 기존 평균 페이스 패널 삭제로 해석. 상단 시간/거리 유지. 하단 현재속도/내구도 한 줄.
- 메인 거리/운동시간 제거. 효율 블루, 착화감 퍼플, 내구도 민트.
- 러닝 후 자유러닝 문구 제거. 결과 화면의 평균속도/보상은 유지.
- 배지: 일반/레어/에픽/레전더리/레드라인/피니시, 채운 색 배경+흰 글씨. 최신 사용자 요청에 따라 이전 장식 배지 디자인을 대체하는 제안.

## 신발 탭 전달 지침
내 신발, 신발 보관함, 메인, 러닝 결과 모두 이름 마지막에 같은 배지 스타일을 사용. 별도 배지 전용 행 금지. 표시 높이24–28dp, 글자11–12sp, 좌우8–10dp 여백. 배지는 정보 표시이며 무의미한 클릭동작을 추가하지 않음. 긴 이름은 마지막 이름줄 끝에 붙임. 색상 채움+흰 글씨만 사용하고 프레임/광택/형광 테두리는 배지에 사용하지 않음. PNG는 시안 기준; 실제 앱에서는 크기와 글자 확대를 지원하는 공통 네이티브 배지 컴포넌트 권장. 이번 작업은 적용 지침만 제공하며 앱 구현은 하지 않음.
기존 rarity는4종이며 REDLINE/FINISH는 LEGENDARY내 series. 시각6종 요청을 새 확률/보상/DB등급으로 확대하지 않음.

## 프롬프트

### 1 · 러닝 중 수정

Use case: ui-mockup. Edit the provided StepUp app screenshot precisely; preserve the existing polished navy/cobalt palette, white Korean typography, all elements not explicitly changed, all data values, resolution853x1844 and full single-screen layout. No phone frame. Do not add features or copy. Input1 edit target. Keep header 자유러닝, prominent top amber speed error, upper runningtime24:18 and distance3.24km, full navy map and same fluorescent green route and position endpoints, and bottom pause/stop buttons unchanged. Replace the ENTIRE bottom two metric/summary panels (currently current speed/averagepace and durability/reward) with EXACTLY ONE integrated navy panel with TWO equal columns IN A SINGLE HORIZONTAL ROW: left label "현재 속도", amber warning icon and value "— km/h"; right label "내구도", value "92/100", small blue horizontal bar92% filled. Delete average pace entirely, delete estimated reward entirely including cube icon, delete shoe outline icon entirely. No shoe icon anywhere in new panel. No average speed or average pace anywhere. Left/right typography aligned and same visual weight; hairline internal divider, understated slate edge18dp corners, no glowingborder. Reclaim removed summary-row height for more visible map, not dead space. Keep remaining top stats and bottom buttons. All visible one screen.

참고: C:/Users/Grabit/OneDrive/Documents/ChatGPT/StepUp/design/run-journey-trio-2026-09-29/02-active-run.png

### 2 · 메인 수정

Use case: ui-mockup. Edit the provided StepUp app screenshot precisely; preserve the existing polished navy/cobalt palette, white Korean typography, all elements not explicitly changed, all data values, resolution853x1844 and full single-screen layout. No phone frame. Do not add features or copy. Input1 edit target home screen; Input2 only reference for FILLED cobalt rounded rarity badge with white Korean letters. Preserve central exact cream/mint/navy shoe, brand header, todaystep6840 goal8000 progress85.5%, shoe name and level, home bottom5nav and record/course secondaryactions. DELETE whole row "거리4.8km" and "운동 시간42분" with both icons, no replacement data. Close the resulting gap tastefully; keep shoe panel and primary runningbutton balanced in viewport rather than adding blank giantspace. Change three statbar fill colors only: 효율 +4.8% bar cobalt #3988FF, 착화감7.5% bar softpurple #A18AF5, 내구도92/100 bar mintteal #46C5AC; retain dark emptytracks and white values. Replace current outlined "레어" badge next to shoe name with solid cobalt #165DDF pill and WHITE "레어" text like Input2. Badge about24dp high, short text-fit width with balanced10dp sidepadding, no glow or outline, no metallic edges. Same filled style will be reused in shoes tab. No distance or workouttime anywhere on home. No other changes.

참고: C:/Users/Grabit/OneDrive/Documents/ChatGPT/StepUp/design/run-journey-trio-2026-09-29/01-home-equipped-shoe.png, C:/Users/Grabit/OneDrive/Documents/ChatGPT/StepUp/design/run-journey-trio-2026-09-29/03-run-summary.png

### 3 · 러닝 후 수정

Use case: ui-mockup. Edit the provided StepUp app screenshot precisely; preserve the existing polished navy/cobalt palette, white Korean typography, all elements not explicitly changed, all data values, resolution853x1844 and full single-screen layout. No phone frame. Do not add features or copy. Input1 edit target. Delete ONLY the text "자유러닝" under the STEPUP logo, leaving clean compact spacing. Preserve runningcomplete header, all reportvalues3.24km,time24:18,averagepace7′30″/km,averagespeed8.0km/h, maproute, exact shoeimage and name, durability, estimatedreward+2.4SUP andpendingstatus, save/share/doneactions. Refine shoe rarity badge into compact flat FILLED cobalt #165DDF pill with WHITE "레어" medium-boldtext, no outline/glow,24dp high with text-fit width and equal10dp sidepadding. This is the master shared gradebadge style, not interactivebutton. Preserve everythingelse. Do not remove averagespeed or reward from this results screen: deletions apply ONLY to active-run screen. No "자유러닝" anywhere.

참고: C:/Users/Grabit/OneDrive/Documents/ChatGPT/StepUp/design/run-journey-trio-2026-09-29/03-run-summary.png

### 등급 배지 · 일반

Use case: ui-mockup. Create ONE ISOLATED reusable Korean rarity pill badge PNG on genuine transparent background, no app screen. The reference screenshot is STYLE REFERENCE ONLY: its small filled cobalt "레어" badge beside shoe name, not the shoe, map, frame or rest of UI. Output ONLY a simple flat colored capsule with centered WHITE Korean text "일반" exactly, one badge only, no other characters. Opaque solid background fill #596777, white #FFFFFF medium-bold clean Pretendard-style Korean sans-serif, crisp edges, optically centered baseline. Rounded pill fully semicircular ends, equal horizontal padding about0.7 times textheight on each side, verticalpadding about0.38 times textheight. Text and pill must be COMPACT, not enormous widebutton; long label naturally makes widerpill. Same fontstyle/fontsize-relative-height as all other badges. Pure flat UI: NO gradients, outline, sheen, shadow, glow, sparkles, symbols, crown, icon, shoe, map, card, rectangular canvas fill, checkerboard illustration or dark background. Transparent exterior with modest even padding. Large high-resolution rendition of a small24dp-high inline statusbadge. Badge interior opaque #596777; exterior alpha0. White text cannot be colored. This design is meant for name-end inline placement in StepUp shoe tab and results screen.

참고: C:/Users/Grabit/OneDrive/Documents/ChatGPT/StepUp/design/run-journey-trio-2026-09-29/03-run-summary.png

### 등급 배지 · 레어

Use case: ui-mockup. Create ONE ISOLATED reusable Korean rarity pill badge PNG on genuine transparent background, no app screen. The reference screenshot is STYLE REFERENCE ONLY: its small filled cobalt "레어" badge beside shoe name, not the shoe, map, frame or rest of UI. Output ONLY a simple flat colored capsule with centered WHITE Korean text "레어" exactly, one badge only, no other characters. Opaque solid background fill #165DDF, white #FFFFFF medium-bold clean Pretendard-style Korean sans-serif, crisp edges, optically centered baseline. Rounded pill fully semicircular ends, equal horizontal padding about0.7 times textheight on each side, verticalpadding about0.38 times textheight. Text and pill must be COMPACT, not enormous widebutton; long label naturally makes widerpill. Same fontstyle/fontsize-relative-height as all other badges. Pure flat UI: NO gradients, outline, sheen, shadow, glow, sparkles, symbols, crown, icon, shoe, map, card, rectangular canvas fill, checkerboard illustration or dark background. Transparent exterior with modest even padding. Large high-resolution rendition of a small24dp-high inline statusbadge. Badge interior opaque #165DDF; exterior alpha0. White text cannot be colored. This design is meant for name-end inline placement in StepUp shoe tab and results screen.

참고: C:/Users/Grabit/OneDrive/Documents/ChatGPT/StepUp/design/run-journey-trio-2026-09-29/03-run-summary.png

### 등급 배지 · 에픽

Use case: ui-mockup. Create ONE ISOLATED reusable Korean rarity pill badge PNG on genuine transparent background, no app screen. The reference screenshot is STYLE REFERENCE ONLY: its small filled cobalt "레어" badge beside shoe name, not the shoe, map, frame or rest of UI. Output ONLY a simple flat colored capsule with centered WHITE Korean text "에픽" exactly, one badge only, no other characters. Opaque solid background fill #7941C6, white #FFFFFF medium-bold clean Pretendard-style Korean sans-serif, crisp edges, optically centered baseline. Rounded pill fully semicircular ends, equal horizontal padding about0.7 times textheight on each side, verticalpadding about0.38 times textheight. Text and pill must be COMPACT, not enormous widebutton; long label naturally makes widerpill. Same fontstyle/fontsize-relative-height as all other badges. Pure flat UI: NO gradients, outline, sheen, shadow, glow, sparkles, symbols, crown, icon, shoe, map, card, rectangular canvas fill, checkerboard illustration or dark background. Transparent exterior with modest even padding. Large high-resolution rendition of a small24dp-high inline statusbadge. Badge interior opaque #7941C6; exterior alpha0. White text cannot be colored. This design is meant for name-end inline placement in StepUp shoe tab and results screen.

참고: C:/Users/Grabit/OneDrive/Documents/ChatGPT/StepUp/design/run-journey-trio-2026-09-29/03-run-summary.png

### 등급 배지 · 레전더리

Use case: ui-mockup. Create ONE ISOLATED reusable Korean rarity pill badge PNG on genuine transparent background, no app screen. The reference screenshot is STYLE REFERENCE ONLY: its small filled cobalt "레어" badge beside shoe name, not the shoe, map, frame or rest of UI. Output ONLY a simple flat colored capsule with centered WHITE Korean text "레전더리" exactly, one badge only, no other characters. Opaque solid background fill #A96710, white #FFFFFF medium-bold clean Pretendard-style Korean sans-serif, crisp edges, optically centered baseline. Rounded pill fully semicircular ends, equal horizontal padding about0.7 times textheight on each side, verticalpadding about0.38 times textheight. Text and pill must be COMPACT, not enormous widebutton; long label naturally makes widerpill. Same fontstyle/fontsize-relative-height as all other badges. Pure flat UI: NO gradients, outline, sheen, shadow, glow, sparkles, symbols, crown, icon, shoe, map, card, rectangular canvas fill, checkerboard illustration or dark background. Transparent exterior with modest even padding. Large high-resolution rendition of a small24dp-high inline statusbadge. Badge interior opaque #A96710; exterior alpha0. White text cannot be colored. This design is meant for name-end inline placement in StepUp shoe tab and results screen.

참고: C:/Users/Grabit/OneDrive/Documents/ChatGPT/StepUp/design/run-journey-trio-2026-09-29/03-run-summary.png

### 등급 배지 · 레드라인

Use case: ui-mockup. Create ONE ISOLATED reusable Korean rarity pill badge PNG on genuine transparent background, no app screen. The reference screenshot is STYLE REFERENCE ONLY: its small filled cobalt "레어" badge beside shoe name, not the shoe, map, frame or rest of UI. Output ONLY a simple flat colored capsule with centered WHITE Korean text "레드라인" exactly, one badge only, no other characters. Opaque solid background fill #C74143, white #FFFFFF medium-bold clean Pretendard-style Korean sans-serif, crisp edges, optically centered baseline. Rounded pill fully semicircular ends, equal horizontal padding about0.7 times textheight on each side, verticalpadding about0.38 times textheight. Text and pill must be COMPACT, not enormous widebutton; long label naturally makes widerpill. Same fontstyle/fontsize-relative-height as all other badges. Pure flat UI: NO gradients, outline, sheen, shadow, glow, sparkles, symbols, crown, icon, shoe, map, card, rectangular canvas fill, checkerboard illustration or dark background. Transparent exterior with modest even padding. Large high-resolution rendition of a small24dp-high inline statusbadge. Badge interior opaque #C74143; exterior alpha0. White text cannot be colored. This design is meant for name-end inline placement in StepUp shoe tab and results screen.

참고: C:/Users/Grabit/OneDrive/Documents/ChatGPT/StepUp/design/run-journey-trio-2026-09-29/03-run-summary.png

### 등급 배지 · 피니시

Use case: ui-mockup. Create ONE ISOLATED reusable Korean rarity pill badge PNG on genuine transparent background, no app screen. The reference screenshot is STYLE REFERENCE ONLY: its small filled cobalt "레어" badge beside shoe name, not the shoe, map, frame or rest of UI. Output ONLY a simple flat colored capsule with centered WHITE Korean text "피니시" exactly, one badge only, no other characters. Opaque solid background fill #128071, white #FFFFFF medium-bold clean Pretendard-style Korean sans-serif, crisp edges, optically centered baseline. Rounded pill fully semicircular ends, equal horizontal padding about0.7 times textheight on each side, verticalpadding about0.38 times textheight. Text and pill must be COMPACT, not enormous widebutton; long label naturally makes widerpill. Same fontstyle/fontsize-relative-height as all other badges. Pure flat UI: NO gradients, outline, sheen, shadow, glow, sparkles, symbols, crown, icon, shoe, map, card, rectangular canvas fill, checkerboard illustration or dark background. Transparent exterior with modest even padding. Large high-resolution rendition of a small24dp-high inline statusbadge. Badge interior opaque #128071; exterior alpha0. White text cannot be colored. This design is meant for name-end inline placement in StepUp shoe tab and results screen.

참고: C:/Users/Grabit/OneDrive/Documents/ChatGPT/StepUp/design/run-journey-trio-2026-09-29/03-run-summary.png

