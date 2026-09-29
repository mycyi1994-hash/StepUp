# 홈페이지 대기 명단

`0051_waitlist.sql`이 이메일 대기 명단과 X·인스타그램·스레드 공유 신청을 만듭니다. 홈페이지는 공개 Supabase 키로 `waitlist_register`와 `waitlist_submit_share` 두 함수만 호출합니다. 표의 직접 읽기·쓰기는 공개 역할에 허용되지 않습니다.

- 등록 즉시 이메일이 저장됩니다. 확인 메일은 보내지 않습니다.
- 공유 신청은 게시물 주소와 플랫폼을 `submitted`로 저장합니다. 이 상태는 게시 확인이나 보너스 지급을 뜻하지 않습니다.
- 나중에 Google 로그인 이메일과 대기 명단 이메일이 일치하는지 서버에서 확인해야 합니다. 보너스 종류·수량·지급 조건과 중복 지급 방지는 별도 구현입니다.
- 공유 게시물은 운영자가 확인한 뒤 `verified` 또는 `rejected`로 바꿀 수 있습니다. 공개 홈페이지에서는 상태 변경 권한이 없습니다.
- 철회 요청은 `support@stepupcrew.com`에서 받아 해당 이메일 행을 삭제합니다. 공유 신청은 외래 키의 `on delete cascade`로 함께 삭제됩니다.

`setup.sql`은 `main`에 합쳐질 때 `deploy-sql.yml`이 운영 Supabase에 적용합니다. `SUPABASE_DB_URL` 비밀이 없어 건너뛰면 SQL Editor에서 직접 적용해야 합니다. 출시일이 정해지면 GitHub Actions 변수 `WAITLIST_LAUNCH_DATE=YYYY-MM-DD`를 설정합니다. `purge-waitlist.yml`이 출시 후 90일 이내에 기록을 삭제합니다.
