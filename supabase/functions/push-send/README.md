# push-send — 푸시 알림 보내기

데이터베이스가 쌓은 "보낼 푸시"(`public.push_outbox`, `0017_push_outbox.sql`)를 FCM 으로 보낸다.

| 언제 쌓이나 | 누구에게 |
|---|---|
| 글에 댓글 | 글쓴이 |
| 댓글에 답글 | 댓글 단 사람 |
| 승인제 크루에 가입 신청 | 크루장 |
| 크루 파티런 로비가 열림 | 크루원(연 사람 빼고) — 알림 설정 "파티 초대"를 끈 사람 제외 |
| 크루 게시판에 번개 | 크루원(쓴 사람 빼고) — 알림 설정 "이벤트 소식"을 끈 사람 제외 |

자기 자신에게는 보내지 않고, 푸시를 끈 사람과 폰이 없는 사람은 쌓지 않는다.

## 켜는 법 (한 번)

1. **Firebase 서비스 계정 키** — Firebase 콘솔 › 프로젝트 설정 › 서비스 계정 ›
   "새 비공개 키 생성" → JSON 파일. **이 파일은 비밀이다. 저장소·채팅에 올리지 않는다.**
2. **함수 올리기** — Supabase › Edge Functions › "Deploy a new function" › Via Editor ›
   이름 `push-send`, 이 폴더의 `index.ts` 내용을 붙여넣고 Deploy.
   "Verify JWT" 는 **끈다**(깨우는 쪽이 아래 비밀 헤더로 증명한다).
3. **비밀 두 개** — Edge Functions › Secrets:
   - `FCM_SERVICE_ACCOUNT` = 1번 JSON 파일 내용 전체
   - `PUSH_WEBHOOK_SECRET` = 아무도 모를 긴 문자열(예: 비밀번호 생성기로 40자)
4. **깨우기** — Database › Webhooks › Create:
   - Table `push_outbox`, Events `Insert`
   - Type: Supabase Edge Functions → `push-send`, Method `POST`
   - HTTP Headers 에 `x-stepup-secret` = 3번의 `PUSH_WEBHOOK_SECRET`

실패한 푸시는 다음에 깨어날 때 다시 보낸다(최대 5번). 앱을 지운 폰의 토큰은 지운다.
