# 11.11 워킹데이 운영 (2026-11-05)

이벤트 페이지 `stepupcrew.com/11events/`(원본 [iinging747/stepup-walkingday](https://github.com/iinging747/stepup-walkingday)) — 사용자 "아무 앱이든 인증만 하면 되는 거로" · "묻지 말고 완성본".
서버 0090(`supabase/migrations/0090_walkingday.sql`).

## 주소

| 주소 | 누구 | 하는 일 |
|---|---|---|
| `/11events/` | 누구나 | 안내 · 다 같이 걸은 걸음(승인된 걸음의 합, 60초마다). 11/7 ~ 11/12 주 버튼은 「걸음 인증하기」 |
| `/11events/proof` | 참가자 | 사전 등록한 이메일 · 걸은 날(11/7 ~ 11/11) · 걸음 수 · 캡처 · 동의 → 제출. 11/7 0시 ~ 11/12 23:59 만 받는다 |
| `/11events/review` | 운영자 | 검토 키로 열기 → 대기 · 승인 · 거절 · 응모권 명단(CSV) |

## 검토

- 캡처의 날짜 · 걸음 수를 보고 승인 · 거절. 걸음 수가 캡처와 다르면 칸을 고쳐 승인한다(합계에는 고친 값).
- "같은 곳(IP)에서 다른 이메일 N개" 표시는 다중 계정 확인용(같은 와이파이일 수도 있다).
- 승인한 것도 "대기로"로 되돌릴 수 있다. 같은 날을 다시 내면 승인 전까지는 새것으로 바뀐다.
- 검토 키: 첫 키(이름 '대표')는 해시만 0090 에 있다. 더 만들기 · 막기(SQL Editor):

```sql
select economy.walkingday_reviewer_new('담당자 이름');   -- 한 번만 보이는 키
update economy.walkingday_reviewers set revoked_at = now() where label = '대표';
```

## 응모권(명단 탭 · CSV)

- 사전 등록 1
- 11,111걸음 이상 승인된 날이 하루라도 있으면 +3
- 11/11 당일 11,111걸음 이상 승인 +3
- 친구 초대: 내 링크(`?ref=` — 사전 등록 뒤 받는 링크)로 등록한 친구가 11,111걸음 승인을 받으면 친구 한 명마다 나 +2, 그 친구도 +2
- 명단은 초대한 친구 수 → 응모권 순. 초대 순위 경품(1 · 2 ~ 4 · 5 ~ 11위)은 위에서부터. 추첨 · 1인 1경품 · 다중 계정 제외는 사람이 한다.
- 기준 걸음은 설정 `walkingday_goal_steps`(11111). 1,111보 구간을 더하기로 하면 `economy.walkingday_tickets()` 와 페이지 문구를 함께 고친다.

## 끝난 뒤

- 발표(11/16) 뒤 30일 안에 캡처를 지운다(동의 문구와 같다) — 걸음 수 · 상태는 남는다:

```sql
select economy.walkingday_purge_images();
```

- 받는 기간은 설정 `walkingday_open_from` · `walkingday_open_until`(페이지 `proof.js` 의 START · CLOSE 도 같은 값).

## 남은 것(사용자 몫)

- 실제 폰 · QR 스캔 확인, 「빼빼로」 표기 상표 검토, 기부처 확정.
- QR · UTM 채널별 등록 수는 저장하지 않는다(사용자 "일단 ok").
