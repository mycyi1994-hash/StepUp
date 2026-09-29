# 홈페이지 대기 명단 등록

이 함수는 로그인하지 않은 방문자의 이메일을 받아 확인 메일을 보냅니다. 메일의 링크를 누른 기록만 `confirmed` 상태가 됩니다. 앱 계정과 보상 지급은 이 함수에서 만들지 않습니다.

## 배포 전 설정

1. `supabase/setup.sql`의 `0051_waitlist.sql` 부분을 운영 데이터베이스에 적용합니다.
2. Cloudflare Turnstile에 `stepupcrew.com`과 `www.stepupcrew.com`용 위젯을 만듭니다. 공개 site key는 `web/waitlist-config.js`의 `siteKey`에 넣습니다.
3. Resend에서 발신 도메인을 인증하고 API 키를 준비합니다.
4. Supabase Edge Function Secrets에 아래 값을 설정합니다. 비밀 값은 Git이나 웹 파일에 넣지 않습니다.

| 이름 | 내용 |
| --- | --- |
| `TURNSTILE_SECRET` | 위젯의 서버 검증 비밀 키 |
| `WAITLIST_TURNSTILE_HOSTNAMES` | `stepupcrew.com,www.stepupcrew.com` |
| `WAITLIST_ALLOWED_ORIGINS` | `https://stepupcrew.com,https://www.stepupcrew.com` |
| `RESEND_API_KEY` | 확인 메일 발송 키 |
| `WAITLIST_FROM_EMAIL` | 인증된 도메인의 발신 주소, 예: `StepUp <hello@stepupcrew.com>` |

`SUPABASE_URL`과 `SUPABASE_SERVICE_ROLE_KEY`는 Supabase가 함수 런타임에 제공합니다. 함수는 공개 요청을 받으므로 `verify_jwt = false`로 배포하되, 등록 요청마다 Turnstile의 `success`, `action=waitlist`, `hostname`을 서버에서 확인합니다. 데이터 표는 `anon`과 `authenticated` 역할에서 직접 읽거나 쓸 수 없습니다.

## 배포·검증 순서

1. `waitlist-register` Edge Function을 배포합니다. `supabase/config.toml`의 함수 설정이 반영됐는지 확인합니다.
2. 실제 도메인에서 등록 → 확인 메일 수신 → 링크 클릭 → `confirmed` 행 확인을 한 번 수행합니다.
3. 같은 이메일 재등록, 잘못된 이메일, 만료·재사용 확인 링크, Turnstile 실패를 확인합니다. 중복 신청 응답은 기존 등록 여부를 드러내지 않습니다.
4. DB와 함수가 준비된 뒤 홈페이지를 배포합니다. `siteKey`가 비어 있으면 홈페이지 폼은 안내 메시지만 보여주고 제출하지 않습니다.

매일 실행되는 `purge-waitlist.yml`이 확인 링크가 만료된 미확인 기록을 지웁니다. 앱 출시일이 정해지면 GitHub Actions 변수 `WAITLIST_LAUNCH_DATE`를 `YYYY-MM-DD`로 설정하세요. 그러면 출시 후 90일 이내에 모든 대기 명단 기록이 삭제됩니다. 철회 요청은 `support@stepupcrew.com`에서 받아 해당 이메일 행을 즉시 삭제합니다.

출시 후 보너스의 종류·수량·지급 규칙은 별도 결정입니다. 그때는 `confirmed` 이메일과 인증된 Google 계정 이메일을 서버에서 비교하고, 별도 1회 지급 기록을 추가합니다.
