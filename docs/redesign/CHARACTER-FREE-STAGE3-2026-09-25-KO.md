# 캐릭터 없는 화면 — 3단계 검사 기준 변경

2026-09-25 · 시작 소스 `3e8bdac` · 로컬 작업, APK/푸시/원격 실행 없음.

## 변경

- `ExperienceUiTest`: 러닝·신발·커뮤니티·프로필·챌린지·뽑기를 실제 MainScaffold 안에서 렌더링. 기존 네 탭 역할과 가운데 뽑기 버튼을 구분하고 다섯 진입점, 선택 상태, 새 프로필 설정 버튼을 검사한다.
- 신발 미리보기는 저장값을 바꾸지 않으며 사용하기 이후에만 변경되는지 검사한다. 검사용 사본 하나만 추가하고 finally에서 제거·원래 신발 복원한다.
- 뽑기 준비 전 클릭 차단, 준비 후 콜백, 도감·지갑 연결, 의상 버튼 부재를 검사한다. 거래는 실행하지 않는 콜백 검사다.
- 실제 첫 안내가 러닝 → 신발 → 내 정보의 세 목적지를 방문하고 홈으로 돌아가는지 검사한다.
- `DesignReferenceTest`: 남/녀·체험 의상 조합 7개를 제거하고 10개 새 시안 목적지와 일시정지·GPS 대기·마켓·소식을 포함한 14개 장면으로 교체했다. 네 뷰포트에서 총 56개 캡처를 **생성할 예정**이며 아직 생성하지 않았다.
- 러닝 지도는 검사용 좌표로 표시하고 실제 GPS 대기 상태도 따로 검사한다. 결과는 서버 확인 전 보상을 확정하지 않는지 검사한다. GPS 센서/실제 서버 연동 검증을 대체하지 않는다.
- `MysteryDesignTest`: 삭제된 캐릭터 준비 태그·옛 뽑기 제목·의상 버튼 기대값을 교체했다. 뽑기 활성 여부는 실제 BuildConfig에 따른다.

## 실행 경로

`Screen Gallery Capture`에 `redesign` 선택을 추가하고 기본값으로 사용한다. 기존 main의 interaction/permissions/large-font/gallery 동작은 보존했다. 과거 wardrobe 선택은 수동 호환용으로 남았으며 이번 버전의 검수 근거로 사용하지 않는다.

Android 빌드와 에뮬레이터 준비 후:

```sh
bash tools/run_screen_gallery.sh redesign
```

새 묶음은 주요 상호작용 4개 + 뽑기 전용 검사, 그리고 디자인 기준 캡처를 나눠 실행한다. 전체 4언어 × 2테마 × 2글자 × 31모듈(496개 기본 캡처) 검사는 `ExperienceUiTest#allModulesRenderInFourLanguagesAndLargeText`에 남아 있으며 기본 묶음에서는 실행하지 않는다.

결과 위치: `screen-gallery/redesign-interaction-results`, `screen-gallery/redesign-reference-results`, `screen-gallery/experience-qa`, `screen-gallery/mystery`. 실행 실패/시간 초과는 실패로 유지하고 부분 캡처를 별도로 보존한다.

## 이번 확인 범위

통과: 디자인 계약, 4언어 문자열, 패키지 이미지·사운드·폰트 검사, Bash 문법, 14개 장면의 렌더링 분기/선택한 검사 메서드 존재 확인, diff 공백 검사.

**Android 컴파일·기기 실행은 미실행.** 로컬에 Java/Android SDK가 없으며 원격 워크플로를 호출하거나 푸시하지 않았다. Kotlin 타입 확인, 실제 버튼 동작, 캡처 비교, 글자 잘림 판정은 다음 4단계다. 테스트 코드 수정 완료와 테스트 통과를 구분한다. 기존 전체 갤러리/옛 wardrobe 시나리오의 캐릭터 관련 항목까지 정리했다고 주장하지 않는다.
