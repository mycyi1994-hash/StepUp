// 공개 설정만 둡니다. Turnstile site key는 공개 값이며 secret key는 서버에만 둡니다.
// 실제 등록을 열기 전에 Cloudflare Turnstile site key를 채워야 합니다.
window.STEPUP_WAITLIST_CONFIG = Object.freeze({
  endpoint: 'https://pupjzcmybuoyhzfwrsdf.supabase.co/functions/v1/waitlist-register',
  siteKey: '',
})
