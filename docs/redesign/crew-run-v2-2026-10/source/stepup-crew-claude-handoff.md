# StepUp 크루 달리기 — Claude 핸드오프 및 구현 지시서

작성일: 2026-10-16 KST  
저장소: https://github.com/mycyi1994-hash/StepUp  
확인 커밋: da6a3d3b4f42a4af1bf4297966e410e6ccfe251b (2026-10-14 13:16:53 UTC)  
디자인 첨부: stepup-crew-running.html

## 1. Claude에게 줄 요청문

이 문서 전체와 HTML 파일을 함께 전달한다.

~~~text
StepUp 크루 달리기를 첨부 HTML의 디자인으로 실제 Android 앱에 반영해줘.

현재 브랜치와 이 문서의 기준 커밋 차이부터 확인하고 AGENTS.md 및 해당 디렉터리 지침을 읽어줘. 이미 있는 크루 러닝 기능에 새 UI를 적용하는 작업이야.

- 기존 남색·파랑 톤을 유지하면서 심플하고 정돈된 스포츠 UI로 만들어줘.
- 크루 이름과 별개로 정확히 “크루 달리기”라는 배지가 눈에 보여야 해.
- 실제 참가자, 공유 위치, 준비 인원, 동시 출발로 함께 달리는 느낌을 표현해줘.
- 지도 중심 라이브 화면 CR09부터 완성하고 진입·대기실·카운트다운·결과·공유로 확장해줘.
- HTML은 시각적 참고야. 바깥 화면 선택 탭, 휴대폰 프레임, 고정 지도·경로, 예시 숫자, 토스트만 뜨는 버튼은 실제 앱에 옮기지 마.
- 네이티브 Compose와 기존 ViewModel·서비스·API를 연결해줘.
- 자유 러닝 v2, 챌린지, 다이어트, 코스, 번개 러닝의 동작과 디자인은 유지해줘.

작업 범위:
1. 기존 18개 크루 상태에 새 디자인 적용.
2. 1명/종료/프로세스 복원에서도 크루 출처가 유지되는지 확인하고, 문제가 재현되면 최소 범위 수정.
3. 진행자 종료 시 방이 닫히는 기존 정책을 확인하고 종료 안내를 실제 동작에 맞게 작성.
4. 알리기·공유 성공 후 기존 크루 채팅 화면으로 가는 동선 연결.
5. CR02·CR16은 보기 전용 조회 API가 없는 별도 연동 항목으로 관리. party_open을 미리보기 조회에 사용하지 말 것.

공동 목표 달성률, 다른 사람의 일시정지 상태, 늦은 합류, 새 보상 정책을 임의로 만들지 마.
서버 의존 항목 때문에 현재 연결 가능한 18개 상태의 UI 작업을 멈추지 마.

변경 후 같은 커밋의 빌드·테스트·화면 캡처로 검증해줘.
최종 보고에는 주요 6단계 화면과 CR02~CR21 체크리스트를 제공하고,
구현 완료 / API 의존 미완료 / 기기 검증 미실시를 구분해줘.
~~~

## 2. 현재 어디까지 되어 있나

**기존 기능은 상당 부분 구현돼 있다. 새 HTML 디자인의 실제 앱 반영은 남아 있다.**

| 구분 | 소스 확인 결과 | 해야 할 일 |
| --- | --- | --- |
| 상태 수 | CR02~CR21 총 20개. 독립 탭 20개가 아니라 공통 화면의 상태·시트 포함 | 기존 route 활용 |
| 기존 구현 | 진입·준비·대기실·동시 출발·라이브·결과·공유 등 18개 | 기능 보존하며 디자인 적용 |
| 기존 미구현 | CR02 준비 중인 방 미리보기, CR16 이미 출발한 방 | 보기 전용 조회 API 필요 |
| 새 시안 | HTML 프로토타입 존재 | Compose로 실제 데이터 연결 |
| 실시간 정보 | 멤버·준비·진행자·공유 위치·거리·출발 시각 존재 | 기존 데이터 사용 |
| 부족한 정보 | 파티 응답에 사진 URL·개인별 정지·공동 목표·일정·모임 장소·코스 없음 | 없는 정보를 만들지 않음 |
| 이번 검증 | 소스 34개 파일 및 HTML 대조 | Android 빌드·실기기·다중 사용자 검증은 구현 후 필요 |

저장소의 2026-10-02 Experience QA(run 37068564191, eaf9f52)는 이전 UI의 증거다. 새 디자인이나 현재 서버 배포를 검증한 것으로 적지 않는다.

기준 커밋은 자유 러닝 v2 변경이다. 자유 러닝의 보상 미표시 정책을 크루 러닝에 일괄 적용하지 않는다.

| 우선순위 | 작업 | 완료 기준 |
| --- | --- | --- |
| P0 | CR09 지도 중심 화면·크루 배지·멤버 줄·버튼 | 실제 데이터로 실행, 크루 달리기 즉시 식별 |
| P0 | CR10/11/12/18 참가자·정지·종료·연결 끊김 | 개인 측정과 파티 상태를 정확히 구분 |
| P0 | 크루 출처 보존 점검 | 1명·2→1명·종료·복원·번개 구분 검증 |
| P1 | 진입·대기실·출발·결과·공유 | 기존 18개 상태에 새 UI 일관 적용 |
| P1 | 전송 성공 후 채팅 이동 | 올바른 크루 방, 중복 전송 방지, 백스택 보존 |
| P2 | CR02/CR16 조회 연동 | 실제 조회 계약과 권한 검증 후 완료 처리 |

## 3. 디자인 기준

사용자는 기존 톤앤매너, 심플함, 세련된 버튼, 명확한 크루 달리기 식별을 중요하게 봤다. 과한 스포츠 장식이나 복잡한 카드는 원하지 않는다.

- 크루 이름 + 단체 아이콘 + 정확한 한글 “크루 달리기” 배지를 유지한다.
- 크루원 아바타/이니셜과 실제 준비·참여 인원으로 협동감을 표현한다.
- 큰 기울임 글자, 사선, 발광, 장식 카드, 임의 팀 점수·공동 진행률은 넣지 않는다.
- 배지는 식별 표식이며 동작이 없으면 버튼처럼 만들지 않는다.
- 시스템 상태바와 하단 인셋은 네이티브로 처리한다. 기존 앱 헤더/하단 바를 중복 생성하지 않는다.

| 요소 | HTML 기준 | 적용 |
| --- | --- | --- |
| 배경 | #05213B | 기존 남색 톤 유지 |
| 주 액션·크루 배지 | #0C59FF | 준비·시작·재개·공유 |
| 주 글자 | #F5F8FF | 제목·주요 수치 |
| 보조 글자 | #AAC3EA | 라벨·설명 |
| 지도 경로 | #19F52E | 실제 내 GPS 경로 |
| 개인 목표 | #00CEE9 | 개인 거리 진행률만 |
| 종료 | 남색 바탕·코랄 포인트 | 주 액션과 구분 |
| 버튼 | 단순한 라운드·일관된 높이 | 최소 48dp 터치 영역, 진행·비활성·실패 상태 |

HTML의 px를 그대로 dp로 옮기지 말고 실제 기기에서 비율을 조절한다.

CR09 화면 순서:
1. 크루 이름과 “크루 달리기” 배지.
2. 지도: 내 경로, 공개가 허용된 멤버 위치, 실제 작동하는 내 위치/확대 버튼.
3. 참가자 줄: 실제 인원·아바타 또는 이니셜. 탭하면 참가자 상세.
4. 내 기록: 거리 중심, 시간·페이스 보조.
5. “내 목표 {goalKm}km”와 개인 진행률.
6. 실제 보상·내구도 상태를 작은 보조 정보로 표시.
7. 일시정지/재개와 종료 버튼.

작은 화면에서도 주요 버튼이 가려지지 않게 지도 높이와 보조 정보 밀도를 조절한다. 멤버가 많으면 수평 목록 또는 +N과 상세 목록을 사용한다.

“참여 6명”과 “지금 6명 모두 달리는 중”은 다른 주장이다. 타인의 정지 상태가 없으므로 참여 정보를 전원 실시간 운동 상태로 표현하지 않는다.

## 4. 상태별 작업표

“기존 있음”은 기존 기능이 있다는 뜻이다. 새 디자인 완료라는 뜻은 아니다. 저장소 ID는 CR02부터 시작한다.

| ID | 화면·상태 | 현재 | 구현·검수 지시 |
| --- | --- | --- | --- |
| CR02 | 준비 중인 방 미리보기 | 조회 API 없어 미구현 | 읽기 전용 조회 후 표시. 진입만으로 생성·참가 금지 |
| CR03 | 크루 진입·대기실 열기 | 기존 있음 | 새 헤더·크루 선택·입장 CTA. 조회 없이 방 없음 단정 금지 |
| CR04 | 참가자 준비 전 | 기존 있음 | 실제 준비 인원, 진행자, 준비 버튼·권한 안내 |
| CR05 | 참가자 준비 완료 | 기존 있음 | 준비 완료·변경·대기 표시. 시작 권한 준수 |
| CR06 | 진행자 대기실 | 기존 있음 | canStart 등 기존 조건 사용, 중복 시작 방지 |
| CR07 | 준비 인원만 출발 확인 | 기존 있음 | 실제 준비/미준비 수. 미준비 멤버 임의 출발 금지 |
| CR08 | 동시 출발 카운트다운 | 기존 있음 | 서버 기준 출발 시각과 명단 유지 |
| CR09 | 함께 달리는 중 | 기존 있음 | 지도 중심 새 배치, 멤버·개인 기록·목표 연결 |
| CR10 | 참가자 목록 | 기존 있음 | 나·진행자·위치 비공개 등 확인 가능한 상태만 |
| CR11 | 내 러닝 일시정지 | 기존 있음 | 재개·종료 명확화, 내 정지를 전원 정지로 표시 금지 |
| CR12 | 내 러닝 종료 확인 | 기존 있음 | 일반 참가자와 진행자 실제 동작에 맞는 안내 |
| CR13 | 크루 러닝 결과 | 기존 있음 | 크루 배지·실제 기록·저장/정산 상태·결과 맥락 보존 |
| CR14 | 가입한 크루 없음 | 기존 있음 | 간결한 빈 상태, 기존 찾기/만들기 연결 |
| CR15 | 혼자 대기 | 기존 있음 | 실제 1명 표시·알리기·기존 시작 가능 정책 유지 |
| CR16 | 이미 출발한 러닝 | 조회 API 없어 미구현 | 늦은 합류 암시 금지, 새 대기실과 구분 |
| CR17 | 권한·위치 공유 안내 | 기존 있음 | 시스템 권한과 위치 공개 분리, 최초 OFF·선택 기억 유지 |
| CR18 | 연결 끊김 | 기존 있음 | 내 측정 지속, 오래된 타인 위치를 현재처럼 표시 금지 |
| CR19 | 내 크루 선택 | 기존 있음 | 실제 소속 목록, 선택 자체로 방 참가하지 않음 |
| CR20 | 크루에게 알리기 | 기존 있음 / 채팅 이동 미연결 | 미리보기→보내기, 중복 방지, 성공 후 채팅 보기 |
| CR21 | 기록 공유 확인 | 기존 있음 / 채팅 이동 미연결 | 실제 결과, 경로 최초 OFF, 성공 후 채팅 보기 |

CR02/CR16의 API 의존과 나머지 18개 상태의 UI 적용을 따로 관리한다.

## 5. 실제 데이터 연결

| 표시 | 기존 소스 | 규칙 |
| --- | --- | --- |
| 크루 이름·ID | PartyState.crewName/crewId, 진입에서 선택한 크루 | 해당 세션 맥락 사용. 이름 미확인 시 일반 명칭, 다른 크루 이름 대입 금지 |
| 참가자 | PartyState.members, PartyMember.id/name/isMe/isHost | 전체 크루 가입 인원과 러닝 참가 인원 구분 |
| 준비·시작 | ready, readyCount, canStart | 기존 권한·조건 보존 |
| 진행자 | isHost | 크루장과 같다고 가정하지 않음. “진행자”로 용어 통일 |
| 사진 | PartyMemberRow에 URL 없음 | 기존 프로필 소스가 있으면 사용, 없으면 이니셜. 가짜 사진 금지 |
| 타인 위치 | PartyMember.point 및 공유·연결 상태 | 유효·공개 허용 좌표만. 비공개·오래된·오프라인 좌표 숨김 |
| 타인 거리 | PartyMember.km | UI 표시용. 보상·정산·랭킹 입력으로 새로 사용 금지 |
| 타인 일시정지 | 서버 필드 없음 | 추정 표시 금지. ready는 대기실 준비 상태 |
| 내 기록 | 기존 WalkSessionService/WalkViewModel | 거리·페이스 미확인 시 “—”. 0과 데이터 없음 구분 |
| 목표 | WalkViewModel.goalKm → WalkSessionService.goalKm | 개인 목표. 유효 거리로 계산·0~100% 제한, 계산 불가 시 미확인 |
| 공동 목표 | 없음 | 임의 생성 금지 |
| 보상·내구도 | 기존 러닝·정산 상태 | 예상/확인 중/확정/제외 구분, HTML 숫자 고정 금지 |
| 출발 | startsAt/serverNow → startsAtLocal | 기존 서버 시계 보정·출발 명단 유지 |
| 일정·장소·코스 | party_state에 없음 | 정보 줄 생략 |
| 공유 대상 | 해당 러닝의 crewId | 현재 선택 목록이나 최근 크루로 임의 대체 금지 |

서버 위치 공개 조건과 네트워크 실패 시 타인 좌표 숨김을 보존한다. 서버의 좌표 유효 시간과 클라이언트의 참가자 유효성 기준은 서로 다른 규칙이므로 임의 통합하지 않는다.

## 6. 우선 검증할 코드 경계

### A. 크루 출처와 인원수·임시 파티 상태

소스 확인 사실:
- WalkScreen.kt의 crewRun은 활성 중 session.isActive와 party.isActive, 결과에서 lastPartySize > 1을 사용한다.
- 결과 crewShareId는 현재 party.crewId에도 의존한다.
- WalkSessionState.partyCrewId는 이미 존재하며 RunCheckpointStore에서 저장/복원한다. 크루 ID 저장이 전혀 없다는 판단은 틀리다.
- beginTracking은 partySize > 1일 때만 partyCrewId를 설정한다.
- 종료 시 새 WalkSessionState에는 lastPartySize가 남지만 결과용 크루 맥락과 명시적 러닝 출처가 충분히 유지되지 않는다.
- 파티에는 번개 러닝도 포함된다.

다음은 코드상 확인이 필요한 시나리오이며, 이번 조사에서 실행해 재현한 장애는 아니다.

1. 크루 대기실에서 기존 조건에 따라 1명으로 시작하고 종료.
2. 2명 이상으로 시작 후 1명만 남아 종료.
3. 활성 세션은 복원됐지만 파티 상태는 아직 비어 있는 상황.
4. 결과 화면 재생성/복원 후 해당 크루에 공유.
5. 번개 러닝, 다른 계정의 체크포인트 복원.

문제 재현 시:
- UI의 러닝 출처/크루 맥락과 보상 계산용 partySize를 분리한다.
- 기존 partyCrewId를 활용하고 필요한 활성·종료 맥락만 보완한다.
- 결과와 공유는 해당 러닝의 crewId/이름/종류를 사용하도록 설계한다.
- 이전 체크포인트 버전 호환과 계정 소유권 검사를 유지한다.
- 크루 출처를 표시한다고 1인 보너스를 부여하지 않는다. 정산 인원·보상 정책 변경 금지.
- party.isActive만으로 번개를 크루 달리기로 표시하지 않는다.

### B. 진행자 종료 정책

CrewRepository.finishParty의 RUNNING 처리:
- 진행자: partyApi.finish(id)
- 일반 참가자: partyApi.leave(id)

0012_course_party.sql의 party_finish는 방을 FINISHED로 만든다. 방이 닫히는 것과 다른 사람의 측정 서비스가 즉시 끝나는 것은 구분해야 한다.

HTML의 “내 기록만 종료되며 크루 전체에는 영향 없음” 취지의 문구를 진행자에게 그대로 적용하지 않는다.

두 계정으로 진행자 종료, 일반 참가자 종료, 남은 멤버 측정·연결 상태를 확인하고 문구를 맞춘다. 정책을 바꾸려고 진행자 자동 승계나 방 유지 기능을 임의 추가하지 않는다. 실행 환경이 없으면 “소스 확인, 다중 사용자 검증 미실시”로 기록한다.

## 7. 수정 위치와 구현 방식

아래는 저장소 루트 기준 경로다. 먼저 기존 파일을 읽고 필요한 부분만 변경한다.

| 파일 | 역할·수정 초점 |
| --- | --- |
| app/src/main/java/com/stepup/android/ui/screens/walk/CrewRunScreens.kt | 진입·크루 선택·준비 안내·출발 확인·알리기·공유 |
| app/src/main/java/com/stepup/android/ui/screens/community/PartyLobbyScreen.kt | 참가자/진행자/혼자 대기·카운트다운. 번개와 공용임에 주의 |
| app/src/main/java/com/stepup/android/ui/screens/community/PartyLobbyViewModel.kt | 준비·공유·시작 등 기존 액션 |
| app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt | 크루 판별·지도 데이터·멤버 시트·종료·결과/공유 |
| app/src/main/java/com/stepup/android/ui/screens/walk/RunLive.kt | 크루 전용 분기/컴포넌트로 라이브·결과 새 배치 |
| app/src/main/java/com/stepup/android/ui/screens/walk/RunStartOverlays.kt | 카운트다운 표현, 서버 기준 유지 |
| app/src/main/java/com/stepup/android/ui/screens/walk/WalkViewModel.kt | 개인 목표와 세션 값 |
| app/src/main/java/com/stepup/android/ui/components/RouteMapLive.kt | 실제 지도·경로·마커·카메라 |
| app/src/main/java/com/stepup/android/ui/components/RunStyle.kt | 기존 공통 토큰. 전역 덮어쓰기 주의 |
| app/src/main/java/com/stepup/android/ui/components/RunKit.kt | 공통 러닝 컴포넌트 |
| app/src/main/java/com/stepup/android/ui/StepUpRoot.kt | route·복귀·콜백·앱 크롬 |
| app/src/main/java/com/stepup/android/ui/screens/community/chat/ChatNavGraph.kt | 기존 채팅 route와 openChatRoom 사용 |
| app/src/main/java/com/stepup/android/data/repo/CrewRepository.kt | 파티 상태·동시 출발·연결·종료 |
| app/src/main/java/com/stepup/android/data/remote/PartyApi.kt | 현재 DTO·API 계약 |
| app/src/main/java/com/stepup/android/service/WalkSessionService.kt | 측정·정산·세션 맥락 |
| app/src/main/java/com/stepup/android/service/RunCheckpointStore.kt | 복원·버전 호환 |
| supabase/migrations/0012_course_party.sql | 생성·참가·출발·종료 정책 |
| supabase/migrations/0032_party_active.sql | 활성 파티 동작 |
| supabase/migrations/0037_party_live.sql | 위치 공유·거리·공개 조건 |
| app/src/androidTest/java/com/stepup/android/RunJourneyDesignTest.kt | crew 상태 캡처 갱신 |
| docs/redesign/run-remake-2026-10/implementation-checklist.csv | 상태별 구현·검증·남은 연동 갱신 |
| docs/redesign/PROGRESS.md | 실제 변경과 검증 증거 |

지도: LiveRouteMap의 others는 현재 List<Pair<GeoPoint, String>>이고 원형 이니셜 마커를 그린다. 사진·안정적인 ID·진행자 표식이 필요하면 크루 전용 어댑터/타입으로 id, name, isMe, isHost, 유효 좌표, 선택적 이미지 참조를 전달한다. 사진 데이터가 없어도 이니셜로 완성 가능해야 한다. routeColor, routeOutline, RouteMarkers.Pin 등 기존 옵션을 활용한다.

멤버 줄에는 위치 비공개 참가자도 나타날 수 있지만 지도 핀을 만들어서는 안 된다.

채팅: ChatNavGraph.kt에 ChatRoutes.room(crewId), NavHostController.openChatRoom(crewId)가 이미 있다. 전송 성공 후 “크루 채팅 보기”를 콜백으로 연결한다. 전송 기능을 재구현하지 않는다. 성공 전 강제 이동·잘못된 크루·중복 전송·뒤로 가기 문제를 검수한다.

## 8. HTML 적용 시 보정할 항목

- 외부 6단계 탭·20상태 선택기·프레임·가짜 상태바·설명·카운트다운 재생은 리뷰 도구다. 앱 UI에서 제외한다.
- 한강크루, 6명, 4명, 3.24km, 24:18, 7'30", 5km, 65%, +12.4 SUP, 92/100은 예시다.
- 고정 지도 타일·SVG 경로·멤버 좌표를 실제 데이터처럼 사용하지 않는다.
- 토스트만 뜨는 버튼은 실제 기존 액션으로 연결한다.
- 로컬 미리보기 타이머 대신 서버 출발 시간 로직을 사용한다.
- 위치 공유는 최초 기본 OFF 후 기존 선택 기억 정책을 유지한다. 매번 강제로 OFF 초기화하지 않는다.
- 기록 공유의 경로 포함은 최초 OFF. OFF면 생성 이미지에서 지도·길 자체를 제외한다.
- “리더”가 isHost를 뜻하면 “진행자”로 통일한다.
- “전원 달리는 중”, “종료해도 전원에게 영향 없음”처럼 보장되지 않는 표현을 제거한다.
- CR02/CR16이 HTML에 보인다고 실제 연동 완료로 집계하지 않는다.
- Sending/Sent에서 토글·버튼 상태와 중복 전송 차단을 유지한다.

### CR02·CR16에 필요한 2차 계약

아래는 요구사항이며 현재 존재하는 API 명세가 아니다.

PartyApi.open은 방 생성·입장을 수행한다. 늦게 들어오면 새 방을 열 수 있으므로 미리보기 조회에 사용하지 않는다.

읽기 전용 조회는 인증·소속 권한을 확인하고, 조회만으로 생성·참가·ready 변경이 발생하지 않아야 한다. 방 없음/준비 중/출발/실패를 구분하고 화면에 필요한 최소 정보만 반환해야 한다. 여러 방 선택 기준, 조회 후 입장 전 상태 변경도 처리해야 한다.

현재 늦은 합류는 지원하지 않는다. CR16 표시가 합류 가능을 뜻하지 않는다. 서버 계약이 없으면 중립적인 CR03 진입·대기실 열기 흐름으로 1차 UI를 완성하고 두 상태를 API 의존으로 보고한다.

## 9. 실행 순서 및 검수

1. 현재 HEAD와 기준 커밋 차이를 확인하고 사용자 로컬 변경을 보존한다.
2. AGENTS.md, docs/redesign/s2/README.md, docs/redesign/HANDOFF.md, PRODUCT.md, DESIGN.md, CLAUDE.md, docs/redesign/PROGRESS.md 및 연결 지침을 읽는다.
3. 현재 크루 화면·흐름을 확인하고 출처 보존 및 진행자 종료 경계를 점검한다.
4. 작은 크루 전용 헤더·멤버·액션 컴포넌트와 CR09를 구현한다.
5. CR10/11/12/18을 완성한 뒤 진입·선택·대기실·권한·출발로 확장한다.
6. 결과·공유·채팅 이동, 해당 러닝의 크루 맥락을 연결한다.
7. 같은 커밋으로 검사·캡처하고 체크리스트·진행 문서를 갱신한다.

20개 독립 화면을 새로 만들지 말고 기존 entry/lobby/run 구조와 시트를 활용한다. 공용 코드를 수정하면 다른 러닝 모드에 대한 회귀를 확인한다. native Compose로 구현하고 HTML/WebView나 화면 이미지를 UI 대용으로 사용하지 않는다. strideup.db, strideup_prefs 및 계정 식별을 보존한다.

### 필수 완료 체크

- [ ] 주요 6단계에서 크루 달리기가 일관되게 식별됨.
- [ ] CR09의 지도·멤버·내 기록·목표·액션 위계가 시안과 맞음.
- [ ] 이름·인원·수치에 샘플 값이 남지 않음.
- [ ] 320/360/390dp 및 글자 크기 1.3배에서 버튼·단위가 잘리지 않음.
- [ ] 로딩·빈 상태·오류·재시도·비활성·전송 중·성공 표현이 있음.
- [ ] 단순 진입/크루 선택이 방 생성·참가를 유발하지 않음.
- [ ] 서버 출발 시각·준비 명단 사용, 중복 시작 방지.
- [ ] 시스템 권한과 타인에게 위치 공개하는 선택을 구분.
- [ ] 비공개·연결 끊김·지연·복구에서 타인 좌표 보호.
- [ ] 위치 없는 기록에서 거리·페이스·진행률을 가짜로 표시하지 않음.
- [ ] 개인 정지·일반 참가자 종료·진행자 종료 문구가 실제 동작과 맞음.
- [ ] 1명·2→1명·종료·복원에서 크루 맥락 검증.
- [ ] 번개·자유 러닝을 크루 러닝으로 잘못 분류하지 않음.
- [ ] 저장·정산·공유의 실패 재시도와 연타에서 중복 처리 없음.
- [ ] 경로 OFF 공유 이미지에 지도·길이 없음.
- [ ] 성공 후 채팅 대상과 백스택이 올바름.
- [ ] 자유 러닝 v2의 보상 미표시·지도 및 다른 모드 유지.
- [ ] CR02/CR16은 실제 API 없으면 미완료로 기록.

### 검사 진입점

실행 환경과 저장소 최신 지침을 우선한다.

~~~bash
python3 tools/check_design_contract.py
python3 scripts/check-strings.py
python3 tools/check_experience_assets.py
./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug
~~~

Windows에서는 환경에 맞는 Python과 gradlew.bat를 사용한다. SDK/JDK가 없으면 통과로 쓰지 말고 가능한 CI 결과 또는 미실시 사유를 남긴다.

관련 기존 테스트:
- PartyApiTest: API 요청·응답·위치 필드.
- FreeRunUiTest: 자유 러닝 회귀.
- RunJourneyDesignTest#crew: 기존 18개 크루 상태 캡처. CR02/CR16 제외.
- 세션/체크포인트 수정 시 관련 복원·저장 테스트를 확인하고 1명·2→1명·복원 지연·번개·계정 불일치를 보강.

정적 스타일 변경에 구현을 그대로 따라 쓰는 테스트를 늘리지 않는다. 실제 위험을 검증한다.

유지하거나 테스트와 함께 갱신할 기존 태그:
crew-entry, crew-pick-sheet, party-ready, party-start, party-alone,
crew-ready-check, crew-start-confirm, crew-notify-preview, party-countdown,
run-together-row, run-together-list, run-finish, run-end-dialog,
run-crew-offline, run-result-crew, run-result-share, crew-share, crew-share-route-toggle.

목 데이터 캡처는 실제 두 기기의 동시 출발·위치 공유·방 종료 검증을 대체하지 않는다. 다중 사용자 검증이 불가능하면 미실시로 분리한다.

### 최종 보고 형식

~~~text
기준/완료 커밋:
변경 파일과 목적:
주요 6단계 화면 캡처:
CR02~CR21별 UI 구현 / API 연결 / 기기 검증:
크루 출처 경계 시나리오 결과:
진행자/일반 참가자 종료 확인 결과:
실행한 명령과 결과:
미실시 검사 및 이유:
남은 서버 의존 항목:
~~~

## 10. 확인 근거와 한계

이번 확인은 소스·HTML 대조다. Android 실행, 실제 GPS, 서버 RPC, DB 배포 확인은 하지 않았다. 코드상 경계 문제를 재현된 장애로 단정하지 않는다. 기존 HTML과 앱 저장소는 수정하지 않았다.

핵심 근거:
- [상태별 구현 체크리스트](https://github.com/mycyi1994-hash/StepUp/blob/da6a3d3b4f42a4af1bf4297966e410e6ccfe251b/docs/redesign/run-remake-2026-10/implementation-checklist.csv)
- [남은 연동·기존 검증 기록](https://github.com/mycyi1994-hash/StepUp/blob/da6a3d3b4f42a4af1bf4297966e410e6ccfe251b/docs/redesign/run-remake-2026-10/README.md)
- [크루 판별·지도·결과 공유](https://github.com/mycyi1994-hash/StepUp/blob/da6a3d3b4f42a4af1bf4297966e410e6ccfe251b/app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt)
- [세션 맥락·시작·종료](https://github.com/mycyi1994-hash/StepUp/blob/da6a3d3b4f42a4af1bf4297966e410e6ccfe251b/app/src/main/java/com/stepup/android/service/WalkSessionService.kt)
- [파티·동시 출발·종료](https://github.com/mycyi1994-hash/StepUp/blob/da6a3d3b4f42a4af1bf4297966e410e6ccfe251b/app/src/main/java/com/stepup/android/data/repo/CrewRepository.kt)
- [현재 DTO·API](https://github.com/mycyi1994-hash/StepUp/blob/da6a3d3b4f42a4af1bf4297966e410e6ccfe251b/app/src/main/java/com/stepup/android/data/remote/PartyApi.kt)
- [서버 방 종료 정책](https://github.com/mycyi1994-hash/StepUp/blob/da6a3d3b4f42a4af1bf4297966e410e6ccfe251b/supabase/migrations/0012_course_party.sql)
- [위치 공유 공개 조건](https://github.com/mycyi1994-hash/StepUp/blob/da6a3d3b4f42a4af1bf4297966e410e6ccfe251b/supabase/migrations/0037_party_live.sql)

디자인 파일: stepup-crew-running.html  
SHA-256: 2b24898e3f5c587193515cbe0c3b25a3b6da69cced28dd03eed22a417d3ef089
