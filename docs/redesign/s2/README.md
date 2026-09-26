# Claude / Codex 시작점 — StepUp S2

2026-09-26 · 앱 소스 기준 `eb697e5` · 브랜치 `codex/stepup-cohesive-redesign`

**사용자가 선택한 새 시각 기준은 Figma S2 무대다. 확보한 이미지로 진행하고, 기존 앱의 기능 차이까지 정리한다. S2 Android 구현은 진행 중이다 — 진행 상황과 기기 캡처 근거는 [STATUS.md](STATUS.md).**

## 먼저 읽을 순서

1. [확정된 내용 / 제안 / 미정](DECISIONS.md)
2. [전체 리디자인 기획서](PLAN-KO.md)
3. [현재 진행 상태와 다음 작업](STATUS.md)
4. [Figma 45개 프레임 대응표](s2-screen-map.csv), [기존 앱 34개 경로 보존표](existing-route-map.csv)
5. [이미지 자산 안내](../../../design/s2/README.md), [해시·크기·사용 위치](../../../design/s2/manifest.json)
6. [현재 앱 / S2 예상 비교 페이지](preview/index.html)

기존 기능 상세는 [디자이너 인계서](../DESIGNER-HANDOFF-2026-09-25-KO.md)와 [화면 목록](../SCREEN-INVENTORY.md)을 따른다. 오래된 완료 기록은 해당 범위의 증거이며 새 S2의 완료 증거가 아니다.

## 자료의 우선순위

1. 사용자의 최신 지시.
2. 사용자가 지정한 [Figma S2](https://www.figma.com/design/BxZ1zZo74Ym7Rb8mM8cDJn/?node-id=5-2)와 확보한 원본 이미지.
3. 현재 앱의 실제 데이터·계정·기능·플랫폼 제약.
4. 이 폴더의 기획안과 대응표. 제안은 승인된 서비스 정책이 아니다.
5. 비교용 HTML과 작은 PNG. 구성 설명용이며 Figma 원본보다 우선하지 않는다.

예전 문서의 캐릭터·옷장 중심 미술 방향을 복원하지 않는다. 현재 앱에는 러닝/신발/뽑기/커뮤니티/내 정보가 보이고, S2의 네 칸 메뉴로 바꾸는 것은 아직 구현되지 않았다.

## 폴더 구조

```text
docs/redesign/s2/
  README.md                 이 시작점
  DECISIONS.md              사용자 확정 / 기획 제안 / 미정의 경계
  PLAN-KO.md                화면·흐름·상태·구현 순서·완료 기준
  STATUS.md                 실제 완료 범위와 다음 작업
  CLAUDE-START.md            새 세션에 전달할 시작 요청문
  s2-screen-map.csv         Figma 45개 노드 링크와 처리안
  existing-route-map.csv    기존 34개 경로의 새 진입 위치
  preview/                  6개 화면 비교 HTML과 원래 APK 캡처
  comparisons/              채팅용 작은 비교 PNG
  reference/                전체 보드 축소 참고, 정밀 측정용 아님
design/s2/
  originals/                Figma 이미지 원본 17종
  cutouts/                  알파 여백만 정리한 신발/로고 8종
  layer-exports/            Figma 개별 이미지 레이어 렌더 13종
  manifest.json             파일·해시·크기·사용 노드·미확보 항목
  figma-screen-index.json   45개 프레임과 이미지 사용 노드
```

## 비교 페이지 보는 방법

`preview/index.html`을 브라우저로 열거나, 저장소 루트에서 로컬 정적 서버를 실행해 `/docs/redesign/s2/preview/`를 연다. 외부 CDN·로그인·기존 컴퓨터의 output 폴더가 필요하지 않다. 이미지·글꼴·기존 상자는 모두 저장소 안의 상대 경로를 사용한다.

왼쪽은 Android 15에서 얻은 기존 APK 검토 캡처, 오른쪽은 HTML/CSS로 구성한 S2 예상판이다. 화면 선택과 배경/보기 크기 선택만 동작한다. 러닝·뽑기·거래가 구현된 앱으로 취급하지 않는다.

사용자가 비교 PNG의 흐린 화질을 지적했다. 작은 PNG는 화면당 약 210px 폭으로 축소된 설명 자료다. 이를 확대해 앱에 넣거나 폰트/여백의 정확한 기준으로 삼지 않는다. 원본 배경은 780×1200, 신발은 640×640이며 글자·숫자·버튼은 Compose로 구현한다. `preview/before/`의 기존 캡처는 702×1519 원본이다.

## 작업 경계

이 자료를 읽는 것만으로 구현·커밋·푸시·배포를 시작하지 않는다. 사용자가 구현을 요청한 세션에서는 그 범위에 따라 기획의 첫 단계부터 진행한다. 이미 요청된 작업을 매번 다시 승인받을 필요는 없다.

이미지 추출 제한은 추가 작업을 멈출 이유가 아니다. 사용자는 `not allow` 표시를 확인한 뒤 확보분으로 진행하라고 했다. 미확보 4개 합성물의 처리안은 자산 안내에 있다. 사용자에게 같은 내보내기를 다시 요구하지 않는다.

원본 기능·저장소 정체성을 보존하고, 리디자인과 관계없는 내부 개선이나 새 보상 정책으로 범위를 늘리지 않는다. 실제 앱 결과는 같은 소스에서 빌드한 Android 캡처로 보고한다.
