export const SHARE_URL = 'https://stepupcrew.com/';
// The homepage promo film on YouTube. X and Threads preview the last link, so it goes last to show a playable video.
export const PROMO_VIDEO_URL = 'https://youtu.be/5dfGSfGdBPo';
export const SHARE_TEXT = '오늘의 한 걸음, 함께라서 더 멀리. 🏃\n러닝 기록부터 크루와 보상까지, StepUp에서 함께 달릴 날을 기다려요.\n저는 사전 등록했어요. 같이 시작해요!\n#StepUp #스텝업 #러닝크루';
export const CREATOR_TEXT = '오늘의 한 걸음, 함께라서 더 멀리. 🏃\n나의 러닝 루틴과 함께 소개하는 StepUp! 러닝 기록부터 크루와 보상까지, 함께 달리는 즐거움을 준비하고 있어요.\n지금 사전 등록하고 함께 시작해요.\n#StepUp #스텝업 #러닝 #러닝크루\n※ StepUp 사전 등록 공유 이벤트 참여 콘텐츠로, 출시 후 보너스를 받을 수 있습니다.';
export const PLATFORMS = {
  x: { label: 'X', placeholder: 'https://x.com/이름/status/…', help: '공개 게시물의 링크를 붙여넣어 주세요.' },
  instagram: { label: '인스타그램', placeholder: 'https://www.instagram.com/p/…', help: '공개 게시물이나 릴스 링크를 남겨주세요. 스토리는 제출할 수 없어요.' },
  threads: { label: '스레드', placeholder: 'https://www.threads.com/@이름/post/…', help: '공개 게시물의 링크를 붙여넣어 주세요.' },
  youtube: { label: '유튜브', creator: true, target: 'https://www.youtube.com/upload', placeholder: 'https://youtu.be/…', help: '직접 만든 StepUp 소개 영상·쇼츠를 공개로 올리고 링크를 남겨주세요.' },
  tiktok: { label: '틱톡', creator: true, target: 'https://www.tiktok.com/tiktokstudio/upload', placeholder: 'https://www.tiktok.com/@이름/video/…', help: '직접 만든 StepUp 소개 영상을 공개로 올리고 영상 링크를 남겨주세요.' },
  reels: { label: '릴스', creator: true, target: 'https://www.instagram.com/', placeholder: 'https://www.instagram.com/reel/…', help: '직접 만든 StepUp 소개 릴스 링크를 남겨주세요. 일반 공유와 다른 게시물이어야 해요.' },
};

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

export function shareTarget(platform, url = SHARE_URL) {
  if (platform === 'x') return `https://x.com/intent/tweet?text=${encodeURIComponent(`${SHARE_TEXT}\n${url}`)}&url=${encodeURIComponent(PROMO_VIDEO_URL)}`;
  if (platform === 'threads') return `https://www.threads.com/intent/post?text=${encodeURIComponent(`${SHARE_TEXT}\n${url}\n${PROMO_VIDEO_URL}`)}`;
  return PLATFORMS[platform]?.target || 'https://www.instagram.com/';
}
