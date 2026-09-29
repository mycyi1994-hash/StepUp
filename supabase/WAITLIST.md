# 홈페이지 대기 명단

`0051_waitlist.sql`이 이메일 대기 명단을 만들고, `0052_waitlist_creators.sql`이 일반 공유(X·인스타그램·스레드)와 크리에이터 참여(유튜브·틱톡·릴스)를 지원합니다. 홈페이지는 공개 Supabase 키로 `waitlist_register`, `waitlist_submit_share`, `waitlist_status`만 호출합니다. 표의 직접 읽기·쓰기는 공개 역할에 허용되지 않습니다.

- 등록 즉시 이메일이 저장됩니다. 확인 메일은 보내지 않습니다.
- 공유 신청은 게시물 주소와 플랫폼을 `submitted`로 저장합니다. 이 상태는 게시 확인이나 보너스 지급을 뜻하지 않습니다.
- 영수증으로 조회하는 상태 응답에는 플랫폼과 제출·검증·반려 상태만 담습니다. 이메일·게시물 링크·계정 식별자는 반환하지 않습니다. 같은 이메일을 다시 등록하면 영수증을 교체하고 기존 제출 현황을 복원합니다. 이메일 소유권 확인 전이므로 이 상태만으로 보상을 지급하면 안 됩니다.
- 공개 링크의 플랫폼·경로를 서버에서 검증하고 추적 쿼리를 제거합니다. 인스타그램/릴스의 동일 게시물 중복 제출을 막습니다. 틱톡 단축 URL의 실제 목적지, 계정 소유, 내용, 공개 여부는 운영자가 별도로 확인해야 합니다.
- 운영자 전용 `waitlist_bonus_candidates`에서 `social_submitted`, `all_three_submitted`, `creator_submitted`, `all_three_verified`를 조회할 수 있습니다. 실제 지급 원장은 아니며, 이메일별 현재 신청 상태를 저장된 링크에서 계산합니다.
- 나중에 Google 로그인 이메일과 대기 명단 이메일이 일치하는지 서버에서 확인해야 합니다. 보너스 종류·수량·지급 조건과 중복 지급 방지는 별도 구현입니다.
- 공유 게시물은 운영자가 확인한 뒤 `verified` 또는 `rejected`로 바꿀 수 있습니다. 공개 홈페이지에서는 상태 변경 권한이 없습니다.
- 철회 요청은 `stepupofficial@stepupcrew.com`에서 받아 해당 이메일 행을 삭제합니다. 공유 신청은 외래 키의 `on delete cascade`로 함께 삭제됩니다.

`setup.sql`은 `main`에 합쳐질 때 `deploy-sql.yml`이 운영 Supabase에 적용합니다. `SUPABASE_DB_URL` 비밀이 없어 건너뛰면 SQL Editor에서 직접 적용해야 합니다. 출시일이 정해지면 GitHub Actions 변수 `WAITLIST_LAUNCH_DATE=YYYY-MM-DD`를 설정합니다. `purge-waitlist.yml`이 출시 후 90일 이내에 기록을 삭제합니다.
