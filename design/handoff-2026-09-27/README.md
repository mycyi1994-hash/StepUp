# StepUp · 디자이너 작업 패키지

## 무엇을 전달하나요?

- 기본·상세 화면 45개와 위치 없이 시간만 기록하는 보조 화면 1개
- 개별 컴포넌트 58개: 아이콘 16개, 버튼 16개, 액션 카드 6개, 숫자 입력창 5개, 경험 선택 2개, 모달 4개, 공통 부품 9개
- 제목·본문·버튼을 편집할 수 있는 모달 사례 12개
- 색상·간격 변수 37개, 텍스트 스타일 7개
- 전체 네이티브 Figma 생성을 위한 로컬 플러그인, 화면별 SVG, 컴포넌트별 SVG, 동작 명세 85개

## 현재 온라인 Figma와 전체 패키지의 차이

[현재 Figma 파일](https://www.figma.com/design/NkRcexuwUX38qLIzJpjGRW)에는 **기본 6개 화면과 컴포넌트 58개, 변수·글자 스타일**이 실제로 만들어져 있습니다. 텍스트와 도형을 직접 편집하고 인스턴스 속성을 바꿀 수 있습니다.

연결된 Starter 플랜의 Figma 도구 사용 한도에 도달하여, 나머지 39개 화면·전체 프로토타입·작업 안내를 온라인 파일에 추가하지 못했습니다. **전체 45개는 아래 생성 플러그인에 포함되어 있으며, Figma에서 직접 실행하는 단계가 남아 있습니다.** 전체 실행은 Figma에서 검증하지 못했고, 로컬 구조 렌더러와 브라우저에서 검증했습니다. 온라인 파일을 전체 45개가 완성된 파일로 간주하지 마세요.

## 전체 화면을 Figma에 만드는 방법

1. ZIP을 압축 해제합니다. Figma **데스크톱 앱**에서 새 빈 Design 파일을 엽니다.
2. 캔버스 우클릭 → **Plugins → Development → Import plugin from manifest**를 선택합니다.
3. 이 폴더의 `figma-plugin/manifest.json`을 선택합니다.
4. Development에서 **StepUp · Editable UI Kit & 45 Screens**를 실행합니다.
5. `00 작업 안내 & 스타일`, `01 컴포넌트`, `02 화면 & 플로우`의 세 페이지가 생성됩니다. 새 빈 파일에서 한 번만 실행하세요.

설치 절차는 [Figma 공식 플러그인 가져오기 안내](https://help.figma.com/hc/en-us/articles/38457121114263-Create-a-Figma-Design-plugin-with-the-Figma-MCP-server-and-agentic-tools)의 Import and test the plugin에 기반합니다. 별도 프로그램 빌드나 터미널 작업은 필요 없습니다. 폰트 오류가 나면 Noto Sans KR Regular·Medium을 사용할 수 있는지 확인하세요.

이 플러그인은 PNG를 붙이는 방식이 아니라 Figma의 Text, Vector, Frame, Component, Instance, Component Set, Variable, Text Style을 만듭니다. 외부 서버 통신은 사용하지 않습니다. 배포·공개용 플러그인이 아닙니다.

## 수정 방법

|부품|바꿀 수 있는 항목|
|---|---|
|Button|Tone, State, Label, Icon, Show icon|
|Action Card|Title, Description, Icon, Tone, State|
|Number Field|State, Label, Value, Unit, Helper, Show helper|
|Choice|Default / Selected, Label, Description|
|Dialog|Kind, Title, Body, Show tertiary. 중첩된 버튼 인스턴스의 Label|
|Notice / Toast|Title, Body / Message|
|Metric|Label, Value|
|Route Map|도로·공원·코스·현재 위치의 개별 벡터 및 텍스트|

모든 화면은 390 × 844 기준입니다. 공통 컴포넌트의 마스터를 바꾸면 인스턴스에도 반영됩니다. 문구만 바꾸는 경우에는 화면의 인스턴스 속성을 수정하세요. 관련 요소는 Auto Layout으로 묶여 있습니다. 화면에 따라 본문과 하단 버튼을 별도 영역으로 두었습니다.

기본 폰트는 Noto Sans KR Regular·Medium입니다. 기존 Android 코드의 Pretendard와는 다릅니다. 원래 글꼴을 사용할 경우 StepUp 텍스트 스타일을 일괄 변경한 뒤 제목 줄바꿈과 버튼 높이를 점검하세요. 브라우저 SVG 미리보기는 설치된 대체 한글 글꼴로 보일 수 있습니다.

## 폴더 안내

|파일·폴더|용도|
|---|---|
|index.html|전체 화면·컴포넌트 미리보기, 예시 버튼 이동|
|figma-plugin|전체 네이티브 Figma 생성용 manifest.json / code.js|
|screens-svg|45개 화면 + 시간 전용 1개. 도형·텍스트가 분리된 SVG|
|individual-components-svg|각 버튼 상태·아이콘·입력창 등 58개 단독 SVG|
|components-svg|컴포넌트 종류별 모음 SVG 15개|
|modals-svg|배경을 제외한 모달 사례 12개|
|guide-svg|컬러·타이포·편집 방법 안내|
|design-tokens.json|색상·간격·글자 스타일의 값|
|interaction-spec.md|화면 ID와 85개 동작 기준|
|screen-inventory.csv|전체 화면 목록|
|button-routes.csv|원본 동작 연결표|
|verification.json|온라인 생성 범위와 로컬 검사 범위를 구분한 기록|

SVG의 레이어 이름과 실제 `<text>`는 파일에 유지되어 있습니다. **Figma에 SVG만 가져오면 컴포넌트·변수·Auto Layout은 복원되지 않으며, 글자가 윤곽선으로 변환될 수 있습니다. Figma 작업용 원본은 생성 플러그인을 사용하세요.** SVG는 범용 벡터 편집기용 보조 파일입니다.

## 프로토타입과 구현 구분

네이티브 생성 코드에는 주요 버튼 전환, 모드별 카운트다운, 독립 모달 오버레이와 디자이너용 상태 검토 화면이 포함되어 있습니다. 시간·숫자 입력·OS 권한·위치·기록 저장·추천은 **예시 화면 전환**입니다. 입력 검증과 위치·운동·저장 처리는 실제 앱에서 구현해야 합니다. 모달 밖을 누르면 취소하며 삭제나 저장 없이 종료가 실행되지 않게 연결했습니다.

추천 코스는 내 위치에서 출발해 돌아오는 예시 벡터 지도입니다. 실제 경로 탐색이 아닙니다. 다이어트 예시는 준비 걷기 3분 + (러닝 1분 + 걷기 2분) × 3회 + 마무리 걷기 3분으로 총 15분입니다. 키·몸무게는 변화 기록에 사용하고, 러닝 경험을 루틴 구성의 참고값으로 사용합니다.

## 확인한 범위

- 온라인 Figma: 기본 화면 6개, 컴포넌트 58개, 변수 37개, 텍스트 스타일 7개 생성. 시작 화면과 모달 컴포넌트의 실제 Figma 렌더를 확인했습니다.
- 전체 생성 코드: JavaScript 문법 검사 및 로컬 구조 모델 실행. 기본·상세 45개, 보조 화면 1개, 컴포넌트 58개를 확인했습니다.
- 로컬 SVG 미리보기: 본문과 하단 버튼의 겹침 없음. 주요 화면 시각 확인. 브라우저 오류 없음. 메뉴 → 다이어트 버튼 이동 확인.
- 미확인: 전체 플러그인의 실제 Figma 실행, 실제 권한·위치·입력·저장·운동 처리.
