# StepUp · Claude 통합 전달본 v4

2026-10-03 현재까지 제작·검수한 최신 승인 파란 톤 시안과 구현 지시서다. **이 ZIP 하나만 전달하면 된다.** 기존 통합v3에 알림·공지26개까지 합쳤다. 폐기한 과거 디자인 후보는 구현 기준에 섞지 않았다.

1. [클로드에게 붙여넣기](00-클로드에게-붙여넣기.txt)
2. [통합 구현 지시서](Claude-통합-구현지시서.md)
3. 아래 기능별 지시서와 각 폴더의 `01-screens` PNG
4. [전체 화면 목록](전체-화면목록.md), [구현 기록표](implementation-tracker.csv), [미제작36개](미완료-36개.md)

## 포함 범위

러닝109 + 추가421 = **530개 정적 검수 UI 상태**다. 상태 재사용·별칭을 포함하며 서로 다른 앱 페이지530개라는 뜻은 아니다.

| 묶음 | 검수 상태 | 지시서 |
|---|---:|---|
| 러닝·기록·크루 달리기 | 109 | [지시서](01-packages/stepup-running-blue-remake-v18/04-claude/01-구현지시서.md) |
| 신발 탭 | 2 | [지시서](01-packages/stepup-shoes-blue-claude-v19/01-신발탭-구현지시서.md) |
| 뽑기 | 34 | [지시서](01-packages/stepup-draw-blue-claude-v19/01-구현지시서.md) |
| 강화 | 17 | [지시서](01-packages/stepup-upgrade-blue-claude-v19/01-구현지시서.md) |
| 신발 상세·수리 | 30 | [지시서](01-packages/stepup-detail-repair-blue-claude-v19/01-구현지시서.md) |
| 커뮤니티 기본 | 43 | [지시서](01-packages/stepup-community-core-blue-v19/Claude-구현지시서.md) |
| 코스 글쓰기 | 20 | [지시서](01-packages/stepup-writing-blue-claude-v19/Claude-구현지시서.md) |
| 크루 둘러보기·가입 | 38 | [지시서](01-packages/stepup-crew-browse-blue-claude-v19/Claude-구현지시서.md) |
| 크루 만들기 | 19 | [지시서](01-packages/stepup-crew-create-blue-claude-v19/Claude-구현지시서.md) |
| 크루 운영 | 38 | [지시서](01-packages/stepup-crew-operations-blue-claude-v19/Claude-구현지시서.md) |
| 크루 채팅 | 44 | [지시서](01-packages/stepup-crew-chat-blue-claude-v19/Claude-크루채팅-파란톤-지시서.md) |
| 내 크루 홈 | 32 | [지시서](01-packages/stepup-crew-home-blue-claude-v19/Claude-내크루홈-파란톤-지시서.md) |
| 내 정보·프로필 | 21 | [지시서](01-packages/stepup-profile-blue-claude-v19/Claude-내정보프로필-파란톤-지시서.md) |
| 설정 | 40 | [지시서](01-packages/stepup-settings-blue-claude-v19/Claude-설정-파란톤-지시서.md) |
| 시작·로그인·첫 설정 | 17 | [지시서](01-packages/stepup-onboarding-blue-claude-v19/Claude-시작로그인-파란톤-지시서.md) |
| 알림·공지(부분) | 26 | [지시서](01-packages/stepup-notifications-partial26-blue-claude-v19/Claude-알림공지-파란톤-지시서.md) |

## 남은 범위와 전달물 구분

현재 등록 후보457개 중421개 검수,36개 미제작이다. 알림·공지20개와 지갑16개이며 전체104경로 및 추가 상태 점검도 남아 있다. 완성되지 않은 화면의 기존 기능을 보존한다.

- `01-packages`:16개 기능 묶음의 최신 PNG, 상세 지시서, 상태별 동작, 토큰·로고·참고 자산·폰트와 라이선스.
- `screen-catalog.json`:현재 전체 상태·최신 PNG·해시·검수 기록의 기준.
- `03-audit/source-audits`:실제 코드 조사와 추가 점검 항목. 경로 초안은 완료 판정표가 아니다.
- `03-audit/visual-reviews`:정적 시안의 개별 검수 기록.
- `03-audit/historical-progress`:과거 집계. 현재 수치로 사용하지 않는다.
- `03-audit/사용자제공-기존캡처-검토기준.md`:사용자가 제공한 과거 CI 조건이며 이번 실행 검증이 아니다.
- `FILES-SHA256.json`:패키지 파일 무결성 목록.

Figma 편집 레이어, 모든 그림의 분리 자산, 실제 앱 구현·실행 검증이 완료된 전달물은 아니다. 글자·버튼·바·탭은 실제 UI로 구현한다. 기존 구현 상태는 '미확인'에서 시작하며 미구현이라는 뜻이 아니다.

기능별 문서의 과거 장수·버전은 제작 이력이다. 현재 범위는 이 README·통합 지시서·카탈로그를 우선하고 동작·예외는 기능별 상세 문서를 따른다. 특히 알림 보상 금액은 현재 서버 모드에 따라 달라지므로 그림의 적립 예시를 그대로 복제하지 않는다.

다른 컴퓨터에서는 동봉한 상대 경로를 사용한다. 프롬프트·조사의 과거 로컬 절대 경로는 출처 기록일 뿐이며 생성 스크립트나 웹사이트를 실행할 필요 없다. 이 전달 작업에서 앱 코드를 수정·커밋·푸시하지 않았다.
