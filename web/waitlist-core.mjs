export const SHARE_URL = 'https://stepupcrew.com/';
// The homepage promo film on YouTube. X and Threads preview the last link, so it goes last to show a playable video.
export const PROMO_VIDEO_URL = 'https://youtu.be/5dfGSfGdBPo';
// The last line marks the post as a waitlist share-event entry (posts made for a bonus must say so).
export const SHARE_TEXT = '오늘의 한 걸음, 함께라서 더 멀리. 🏃\n러닝 기록부터 크루와 보상까지, StepUp에서 함께 달릴 날을 기다려요.\n저는 사전 등록했어요. 같이 시작해요!\n#StepUp #스텝업 #러닝크루\n※ StepUp 사전 등록 공유 이벤트 참여 글이에요.';
export const CREATOR_TEXT = '오늘의 한 걸음, 함께라서 더 멀리. 🏃\n나의 러닝 루틴과 함께 소개하는 StepUp! 러닝 기록부터 크루와 보상까지, 함께 달리는 즐거움을 준비하고 있어요.\n지금 사전 등록하고 함께 시작해요.\n#StepUp #스텝업 #러닝 #러닝크루\n※ StepUp 사전 등록 공유 이벤트 참여 콘텐츠로, 출시 후 보너스를 받을 수 있습니다.';
export const SHARE_TEXT_EN = 'One step today, further together. 🏃\nRun records, crews and rewards: I am waiting to run with StepUp.\nI joined the waitlist. Join me!\n#StepUp #running #runningcrew\n※ This post is an entry in the StepUp waitlist share event.';
export const CREATOR_TEXT_EN = 'One step today, further together. 🏃\nMy running routine, and why I want StepUp: run records, crews and rewards, all together.\nJoin the waitlist and start with me.\n#StepUp #running #runningcrew\n※ This video is an entry in the StepUp waitlist share event and can get a bonus after launch.';

/** 공유 · 크리에이터 문구 — 홈페이지 언어를 따른다(영어로 본 사람이 읽지 못하는 글을 올리지 않게). */
export function shareText(lang = 'ko', creator = false) {
  if (lang === 'en') return creator ? CREATOR_TEXT_EN : SHARE_TEXT_EN;
  return creator ? CREATOR_TEXT : SHARE_TEXT;
}

// label · help 는 화면 언어로(i18n.js 의 platform.* · help.*). 여기에는 채널마다 바뀌지 않는 값만 둔다.
export const PLATFORMS = {
  x: { label: 'X', placeholder: 'https://x.com/name/status/…' },
  instagram: { label: '인스타그램', placeholder: 'https://www.instagram.com/p/…' },
  threads: { label: '스레드', placeholder: 'https://www.threads.com/@name/post/…' },
  youtube: { label: '유튜브', creator: true, target: 'https://www.youtube.com/upload', placeholder: 'https://youtu.be/…' },
  tiktok: { label: '틱톡', creator: true, target: 'https://www.tiktok.com/tiktokstudio/upload', placeholder: 'https://www.tiktok.com/@name/video/…' },
  reels: { label: '릴스', creator: true, target: 'https://www.instagram.com/', placeholder: 'https://www.instagram.com/reel/…' },
};

// The server's rule (waitlist_register): after trim + lower-case, something@something.something, at most 254 characters.
const EMAIL = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/** 입력한 이메일을 서버가 받는 모양으로 — 전각 ＠·．(한글 입력기)을 반각으로, 앞뒤 공백을 빼고 소문자로. */
export function normalizeEmail(value) {
  // 문장에서 복사하며 딸려 온 끝 마침표는 뺀다(2차 QA — 'name@example.com.'이 그대로 등록돼 완료 문구에서 문장 부호처럼 보였다).
  return String(value ?? '').normalize('NFKC').trim().toLowerCase().replace(/\.+$/, '');
}

/** 서버(waitlist_register)와 같은 규칙. 통과하지 못하면 보내지 않고 이메일을 다시 확인하라고 알린다. */
export function validEmail(value) {
  const email = normalizeEmail(value);
  // 서버 규칙에 더해, 메일이 갈 수 없는 '..' · '@.' 모양은 보내기 전에 막는다.
  return email.length >= 3 && email.length <= 254 && EMAIL.test(email) && !/\.\.|@\.|^\./.test(email);
}

/**
 * 붙여넣은 게시물 주소를 서버가 받는 모양으로 — 앞뒤 공백을 빼고, 주소 체계 · 호스트를 소문자로(URL 이 정리한다).
 * `https://X.com/…` · `HTTPS://…` 처럼 대문자가 섞인 실제 게시물 링크를 서버가 거절하던 것. 주소가 아니면 그대로 돌려준다.
 */
export function normalizePostUrl(value) {
  let raw = String(value ?? '').trim();
  // 'x.com/…' 처럼 https:// 없이 붙여넣은 주소(2차 QA)
  if (/^(www\.|m\.|mobile\.)?[a-z0-9-]+\.(com|net|be)\//i.test(raw)) raw = `https://${raw}`;
  try {
    const url = new URL(raw);
    const host = url.hostname.replace(/^(www|mobile|m)\./, '');
    if (url.protocol === 'https:' && (host === 'x.com' || host === 'twitter.com')) {
      // 같은 게시물의 다른 모양 — 사진 · 영상 보기(/photo/1), /i/web/status/…, mobile.twitter.com, 공유 꼬리표(?s=20) —
      // 을 서버가 받는 'https://x.com/<계정>/status/<번호>' 로(2차 QA: 게시물 링크를 프로필 주소라며 거절했다).
      const post = url.pathname.match(/^\/([A-Za-z0-9_]+)\/status\/(\d+)(\/.*)?$/);
      if (post) return `https://x.com/${post[1]}/status/${post[2]}`;
      const anon = url.pathname.match(/^\/i\/web\/status\/(\d+)\/?$/);
      if (anon) return `https://x.com/i/status/${anon[1]}`;
    }
    return url.href;
  } catch { return raw; }
}

/** 이 사이트 링크(내 추천 링크 등)를 게시물 칸에 붙여넣었는가 — 그 경우엔 따로 안내한다. */
export function isOwnSiteUrl(value) {
  try { return /(^|\.)stepupcrew\.com$/i.test(new URL(String(value).trim()).hostname); } catch { return false; }
}

export function validPostUrl(value, platform) {
  if (!PLATFORMS[platform] || value.length > 2048) return false;
  try {
    const url = new URL(value);
    if (url.protocol !== 'https:' || url.username || url.password || url.port) return false;
    const host = url.hostname.toLowerCase().replace(/^www\./, '');
    const path = url.pathname;
    if (platform === 'x') return ['x.com', 'twitter.com'].includes(host) && /^\/[\w]+\/status\/\d+\/?$/.test(path);
    if (platform === 'instagram') return host === 'instagram.com' && /^\/(p|reel)\/[\w-]+\/?$/.test(path);
    if (platform === 'reels') return host === 'instagram.com' && /^\/reel\/[\w-]+\/?$/.test(path);
    if (platform === 'threads') return ['threads.com', 'threads.net'].includes(host) && /^\/@[\w.]+\/post\/[\w-]+\/?$/.test(path);
    if (platform === 'youtube') {
      if (host === 'youtu.be') return /^\/[\w-]{11}\/?$/.test(path);
      return ['youtube.com', 'm.youtube.com'].includes(host) && (
        /^\/shorts\/[\w-]{11}\/?$/.test(path) || (path === '/watch' && /^[\w-]{11}$/.test(url.searchParams.get('v') || ''))
      );
    }
    if (platform === 'tiktok') return (
      ['tiktok.com', 'm.tiktok.com'].includes(host) && /^\/@[\w.]+\/video\/\d+\/?$/.test(path)
    ) || (['vm.tiktok.com', 'vt.tiktok.com'].includes(host) && /^\/[\w-]+\/?$/.test(path))
      || (host === 'tiktok.com' && /^\/t\/[\w-]+\/?$/.test(path));
  } catch { /* Invalid URL. */ }
  return false;
}

// Public referral codes: 8 characters without the easily confused I, O, 0 and 1.
export function referralCode(value) {
  const code = String(value || '').trim().toUpperCase();
  return /^[A-HJ-NP-Z2-9]{8}$/.test(code) ? code : '';
}

export function shareUrl(code) {
  const valid = referralCode(code);
  return valid ? `${SHARE_URL}?ref=${valid}` : SHARE_URL;
}

export function shareTarget(platform, url = SHARE_URL, lang = 'ko') {
  const text = shareText(lang);
  if (platform === 'x') return `https://x.com/intent/tweet?text=${encodeURIComponent(`${text}\n${url}`)}&url=${encodeURIComponent(PROMO_VIDEO_URL)}`;
  if (platform === 'threads') return `https://www.threads.com/intent/post?text=${encodeURIComponent(`${text}\n${url}\n${PROMO_VIDEO_URL}`)}`;
  return PLATFORMS[platform]?.target || 'https://www.instagram.com/';
}
