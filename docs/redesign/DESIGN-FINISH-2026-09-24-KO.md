# 남은 화면 디자인 마감 — 소스 적용 기록

2026-09-24. 기준 HEAD `d1075f0`, 기존 앱 소스 `53fc4b8` 위의 로컬 변경.

사용자 지시: **디자인 마감부터 진행. APK를 만들지 않고 모아서 확인.** 이번 작업에서는 APK/AAB 생성, 빌드 워크플로 실행, 배포, 새 이미지 생성을 하지 않았다.

## 적용 범위

7차 23개 + 8차 9개 + 9차 14개 = **46개 화면·상태에 연결된 소스 변경**. 46개의 독립 페이지를 새로 만들었다는 뜻이 아니며, 실제 앱에서 46개가 검증 완료되었다는 뜻도 아니다. 일부 화면은 공통 부품과 글자/여백 마감이 중심이다.

- 배경: 상세/설정/기록은 테마 색을 따르는 공통 배경. 기존 홈·러닝·옷장의 교체 가능한 풍경과 분리된 UI 유지.
- 부품: `UtilityParts.kt`의 설정 스위치, 선택 행, 안내문, 상태 패널, 네이티브 입력 필드, 기록 숫자 부품. 페이지별 복제 축소.
- 선택 탭: 커뮤니티·기록·랭킹·기간이 같은 구현을 사용. 48dp 이상 터치 영역과 긴 라벨 줄바꿈, 선택 시 글자 굵기 고정.
- 지도: 번개 상세의 가짜 경로 장식을 풍경 영역으로 교체하고 미사용 장식 지도 함수를 제거. 실제 기록 지도 타일/GPS 투영은 유지.
- 데이터: 예시 사용자·모임·잔액·소유 의상 추가 없음. 인증·저장·서버·계정 분리 문제는 이번 디자인 작업에 섞지 않음.

## 화면별 반영

공통 상태: **소스 반영, Android 컴파일·실제 화면 확인 대기**.

| 항목 | 화면/상태 | 이번 변경 |
|---|---|---|
| screen-02 | 커뮤니티 기본 | TogetherTab / CommunityScreen: 빈 모임 패널, 읽기 크기, 공통 전환 탭 |
| screen-06 | 지갑 | RewardsScreen: 빈 원장 아이콘, 로딩 패널, 금액 크기, 불필요한 반복 광택 제거 |
| screen-08 | 업적 | AchievementsScreen: 2열 타일을 가로형 업적 목록으로 교체, 목표·등급·진행률 표시 |
| screen-09 | 통계 기본 | AnalyticsScreen: 기록 숫자 공통 부품, 190dp 차트, 날짜 상세 여백 |
| screen-10 | 알림 목록 | NotificationsScreen: 알림 행 여백·날짜, 로딩 및 빈 상태 |
| screen-11 | 소리·움직임 설정 | ExperienceSettingsScreen: 아이콘·설명·스위치 공통 행, 전체 폭 미리 듣기 |
| screen-12 | 알림 설정 | NotificationSettingsScreen: 공통 스위치 행, 행 전체 터치, 저장 중 비활성 유지 |
| screen-13 | 개인정보·권한 설정 | PrivacyScreen: 권한·설명 본문 가독성, 공통 상세 배경 |
| screen-14 | 고객지원 | SupportScreen: FAQ 48dp 터치 영역, 답변 분리 여백 |
| screen-15 | 연결 계정 | ConnectedAccountsScreen: 공통 상세 페이지, 계정 설명·삭제 대화상자 규격 |
| screen-16 | 언어 설정 | LanguageScreen: 라디오 선택 행·원어 이름·독립 안내문 |
| screen-17 | 랭킹 기본 | RankingScreen: 중복 시상대 제거, 개인 순위 목록 및 상태 패널 |
| screen-18 | 크루 생성 | CrewCreateScreen: 큰 모임 미리보기, 공통 네이티브 입력 필드 |
| screen-19 | 글 작성 기본 | PostComposeScreen: 공통 입력 필드, 본문 공간·포커스·자동 높이 |
| screen-21 | 크루 게시판 | CrewBoardScreen: 게시글 공통 카드, 빈 상태 및 멤버 설명 |
| screen-22 | 번개 모임 상세 | FlashRunDetailScreen: 가짜 경로 장식 제거, 풍경/본문 분리, 참가자·대화 가독성 |
| screen-23 | 크루 대기실 | PartyLobbyScreen: 고정 준비/출발 버튼, 참가자 행과 준비 상태 |
| screen-24 | 로그인 | LoginScreen: 하단 인증 패널 여백·제목 위계, 인증 동작 보존 |
| screen-25 | 테마 설정 | ThemeScreen: 공통 라디오 선택 행·테마 안내문 |
| screen-31 | 기록 지도 기본 | HistoryMapScreen: 공통 기간 선택 탭·설명 크기; 실제 지도 유지 |
| screen-36 | 번개 모임 대기실 | PartyLobbyScreen: 번개 대기실에도 동일 참가자/고정 CTA 적용 |
| extra-community-crews | 내 크루 탭 | CommunityScreen / CrewParts: 검색·크루 안내 크기, 빈/로딩/재로그인 패널 |
| extra-community-stories | 이야기 탭 | CommunityParts: 작성자·시간·분류 정리, 48dp 신고/댓글 버튼 |
| extra-community-meetups | 다른 모임 목록 | CommunityParts: 모임 제목·설명 크기, 참가 버튼 별도 전체 폭 행 |
| extra-profile-settings | 내 정보 설정 목록 | ProfileScreen: 공통 설정 링크 행 및 데모 스위치 |
| extra-analytics-quarter | 분기 통계 | AnalyticsScreen: 큰 차트, 13개 주간 상세 선택 버튼 |
| extra-ranking-time | 시간 랭킹 | RankingScreen: 공통 개인 순위 행과 기간 선택 |
| extra-ranking-speed | 속도 랭킹 | RankingScreen: 공통 개인 순위 행과 기간 선택 |
| extra-ranking-factions | 종족 랭킹 | RankingScreen: 공통 기간 선택·종족 설명/기여도 글자 크기 |
| extra-ranking-sup | SUP 랭킹 | RankingScreen: 공통 개인 순위 행과 기간 선택 |
| extra-ranking-crews | 크루 랭킹 | RankingScreen: 크루 순위 행·거리와 참여 횟수 위계 |
| extra-compose-free | 자유글 작성 | PostComposeScreen / FormField: 자유글 입력 상태 |
| extra-compose-tip | 팁 작성 | PostComposeScreen / FormField: 팁 입력 상태 |
| extra-history-month | 기록 지도 월간 | HistoryMapScreen: 월간 기간 탭·읽기 크기; 실제 경로 유지 |
| extra-history-all | 기록 지도 전체 | HistoryMapScreen: 전체 기간 탭·읽기 크기; 실제 경로 유지 |
| guide-00 | 러닝 첫 사용 안내 | GuideTour: 설명·이전/다음/건너뛰기를 하나의 높이 제한 패널로 배치, 본문 스크롤, 움직임 줄이기 반영 |
| guide-01 | 꾸미기 첫 사용 안내 | GuideTour: 설명·이전/다음/건너뛰기를 하나의 높이 제한 패널로 배치, 본문 스크롤, 움직임 줄이기 반영 |
| guide-02 | 커뮤니티 첫 사용 안내 | GuideTour: 설명·이전/다음/건너뛰기를 하나의 높이 제한 패널로 배치, 본문 스크롤, 움직임 줄이기 반영 |
| guide-03 | 내 정보 첫 사용 안내 | GuideTour: 설명·이전/다음/건너뛰기를 하나의 높이 제한 패널로 배치, 본문 스크롤, 움직임 줄이기 반영 |
| extra-analytics-day-details | 통계 날짜 상세 | AnalyticsScreen: 날짜 상세 패널 패딩·행 간격·긴 라벨 공간 |
| extra-analytics-week-details | 통계 주간 상세 | AnalyticsScreen: 주간 상세 패널 패딩·행 간격·직접 선택 버튼 |
| extra-community-comments-empty | 게시글 댓글 시트·0개 | CommentSheet: 빈 댓글 아이콘·본문 및 네이티브 입력창 |
| extra-community-comments-replies | 댓글·답글·알림 강조 상태 | CommentSheet: 작성자·본문·답글 가독성, 닫기/전송/신고 터치 영역 |
| extra-community-report-dialog | 게시글/댓글 신고 창 | BoardParts: 신고 창 모서리·48dp 사유 행·스크롤 |
| extra-community-board-load-failed | 이야기 게시판 로딩 실패 | BoardParts / StatePanel: 오류 아이콘·설명·재시도 공통 상태 |
| extra-community-board-sign-in | 이야기 게시판 재로그인 필요 | BoardParts / StatePanel: 재로그인 안내와 실제 로그인 행동 |

## 확인 결과와 남은 경계

- 디자인 계약: 33개 경로와 공통 헤더/탭 소유 규칙 통과.
- 문자열/리소스 검사 및 변경 공백 검사 통과.
- 변경 Kotlin 파일 33개 문법 파싱 통과. 이 결과는 타입 검사나 Android 컴파일 성공을 뜻하지 않는다.
- 로컬 Android SDK/JDK가 없어 컴파일하지 않음. 사용자 지시에 따라 원격 APK 빌드도 실행하지 않음.
- 같은 소스의 실제 앱 캡처는 아직 없음. 기존 APK 화면을 이번 결과로 제시하지 않음.
- 모아서 확인할 때: 작은 폰, 큰 글자, 밝은/어두운 테마, 입력 중 키보드, 긴 사용자 이름/본문, 로딩/오류/재로그인 상태를 확인한다. 그 전에는 디자인 100%나 출시 완료로 표시하지 않는다.
- 후속 프로필 작업: 핑크 의상 + 클라우드 러너 조합의 루노·루미 앉은 그림과 독립 벤치를 소스에 연결했다. [프로필 반영 기록](../../design/redesign-2026-09/assets/profile/README-KO.md). 다른 착장의 앉은 자세와 몸·의상·신발 부위 합성은 여전히 미완이다. 앱 컴파일/기기 확인 대기.
- 출시 기능·계정·운영 과제는 [출시 준비 현황](RELEASE-READINESS-2026-09-24-KO.md)을 유지한다.
