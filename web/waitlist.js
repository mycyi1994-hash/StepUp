const SUPABASE_URL = 'https://pupjzcmybuoyhzfwrsdf.supabase.co';
const SUPABASE_KEY = 'sb_publishable_jt74AKM32zdqnJlsFHEo2g_MNHa-WRO';
const SHARE_URL = 'https://stepupcrew.com/';
const SHARE_TEXT = 'StepUp 출시 대기 명단에 등록했어요. 함께 걷고 달릴 준비, 지금 시작해요!';
const RECEIPT_KEY = 'stepup.waitlist.receipt';

const dialog = document.querySelector('.waitlist-dialog');
const registerForm = document.querySelector('.waitlist-form');
const shareSection = document.querySelector('.waitlist-share');
const shareForm = document.querySelector('.waitlist-share-form');
const registerFeedback = document.querySelector('.waitlist-feedback');
const shareFeedback = document.querySelector('.waitlist-share-feedback');
const actionFeedback = document.querySelector('.waitlist-share-action-feedback');
const registerSubmit = document.querySelector('.waitlist-form .waitlist-submit');
const shareSubmit = document.querySelector('.waitlist-share-form .waitlist-submit');
let receipt = sessionStorage.getItem(RECEIPT_KEY) || '';
let platform = '';
let cardFile;
let cardLoading;

function prepareShareCard() {
  if (cardLoading) return;
  cardLoading = fetch('./assets/img/waitlist-share-card.png')
    .then(response => response.ok ? response.blob() : Promise.reject(new Error('Share card unavailable')))
    .then(blob => { cardFile = new File([blob], 'stepup-share.png', { type: 'image/png' }); })
    .catch(() => {});
}

function feedback(element, message, isError = false) {
  element.textContent = message;
  element.classList.toggle('is-error', isError);
}

async function callWaitlist(name, body) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12000);
  try {
    const response = await fetch(`${SUPABASE_URL}/rest/v1/rpc/${name}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', apikey: SUPABASE_KEY },
      body: JSON.stringify(body),
      signal: controller.signal,
    });
    if (!response.ok) throw new Error(`Waitlist request failed: ${response.status}`);
    const result = await response.json();
    if (!result?.ok) throw new Error('Waitlist request was not accepted');
    return result;
  } finally {
    clearTimeout(timeout);
  }
}

function showStage(stage) {
  const sharing = stage === 'share';
  registerForm.hidden = sharing;
  shareSection.hidden = !sharing;
  shareForm.hidden = true;
  platform = '';
  dialog.setAttribute('aria-labelledby', sharing ? 'waitlist-share-title' : 'waitlist-title');
  feedback(registerFeedback, '');
  feedback(shareFeedback, '');
  feedback(actionFeedback, '');
}

document.querySelector('.waitlist-open').addEventListener('click', () => {
  showStage(receipt ? 'share' : 'register');
  dialog.showModal();
  prepareShareCard();
  if (!receipt) registerForm.elements.email.focus();
});
document.querySelector('.waitlist-close').addEventListener('click', () => dialog.close());
dialog.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });
document.querySelector('.waitlist-change-email').addEventListener('click', () => {
  receipt = '';
  sessionStorage.removeItem(RECEIPT_KEY);
  registerForm.reset();
  showStage('register');
  registerForm.elements.email.focus();
});

registerForm.addEventListener('submit', async event => {
  event.preventDefault();
  if (!registerForm.reportValidity()) return;
  registerSubmit.disabled = true;
  feedback(registerFeedback, '대기 명단에 등록하고 있어요…');
  try {
    const result = await callWaitlist('waitlist_register', {
      p_email: registerForm.elements.email.value.trim(),
      p_consent: registerForm.elements.consent.checked,
      p_trap: registerForm.elements.website.value,
    });
    if (!/^[0-9a-f]{64}$/.test(result.receipt || '')) throw new Error('Missing registration receipt');
    receipt = result.receipt;
    sessionStorage.setItem(RECEIPT_KEY, receipt);
    showStage('share');
  } catch (_) {
    feedback(registerFeedback, '등록하지 못했어요. 잠시 후 다시 시도해주세요.', true);
  } finally {
    registerSubmit.disabled = false;
  }
});

const targets = {
  instagram: 'https://www.instagram.com/',
  threads: 'https://www.threads.com/',
};
const labels = { x: 'X', instagram: '인스타그램', threads: '스레드' };

document.querySelectorAll('.waitlist-share-platforms button').forEach(button => {
  button.addEventListener('click', async () => {
    platform = button.dataset.platform;
    shareForm.hidden = false;
    shareForm.reset();
    feedback(shareFeedback, '');
    const copied = `${SHARE_TEXT}\n${SHARE_URL}`;
    if (platform === 'x') {
      const url = `https://x.com/intent/tweet?text=${encodeURIComponent(SHARE_TEXT)}&url=${encodeURIComponent(SHARE_URL)}`;
      window.open(url, '_blank', 'noopener,noreferrer');
      feedback(actionFeedback, 'X 글 작성 화면을 열었어요. 게시 후 글 주소를 아래에 붙여넣어 주세요.');
    } else {
      if (navigator.share && (platform === 'threads' || (cardFile && navigator.canShare?.({ files: [cardFile] })))) {
        try {
          const data = platform === 'instagram'
            ? { files: [cardFile], title: 'StepUp', text: SHARE_TEXT }
            : { title: 'StepUp', text: SHARE_TEXT, url: SHARE_URL };
          await navigator.share(data);
          feedback(actionFeedback, `공유 메뉴에서 ${labels[platform]}을 선택해 게시해주세요. 게시 후 글 주소를 아래에 붙여넣어 주세요.`);
          shareForm.elements.postUrl.focus();
          return;
        } catch (error) {
          if (error?.name === 'AbortError') {
            feedback(actionFeedback, '공유를 취소했어요. 다시 눌러 시도할 수 있어요.');
            return;
          }
        }
      }
      const copy = navigator.clipboard?.writeText(copied);
      window.open(targets[platform], '_blank', 'noopener,noreferrer');
      try {
        if (!copy) throw new Error('Clipboard unavailable');
        await copy;
        feedback(actionFeedback, `${labels[platform]}에 붙여넣을 문구와 링크를 복사했어요. 게시 후 글 주소를 아래에 붙여넣어 주세요.`);
      } catch (_) {
        feedback(actionFeedback, '자동 복사가 안 됐어요. stepupcrew.com 링크를 게시물에 넣어주세요.', true);
      }
    }
    shareForm.elements.postUrl.focus();
  });
});

function validPostUrl(value, selectedPlatform) {
  try {
    const url = new URL(value);
    if (url.protocol !== 'https:') return false;
    const host = url.hostname.toLowerCase().replace(/^www\./, '');
    if (selectedPlatform === 'x') return ['x.com', 'twitter.com'].includes(host) && /^\/[^/]+\/status\/\d+\/?$/.test(url.pathname);
    if (selectedPlatform === 'instagram') return host === 'instagram.com' && /^\/(p|reel)\/[^/]+\/?$/.test(url.pathname);
    if (selectedPlatform === 'threads') return ['threads.com', 'threads.net'].includes(host) && /^\/@[^/]+\/post\/[^/]+\/?$/.test(url.pathname);
  } catch (_) { /* Invalid URL. */ }
  return false;
}

shareForm.addEventListener('submit', async event => {
  event.preventDefault();
  if (!shareForm.reportValidity() || !platform) return;
  const url = shareForm.elements.postUrl.value.trim();
  if (!validPostUrl(url, platform)) {
    feedback(shareFeedback, `${labels[platform]} 게시물 주소를 확인해주세요.`, true);
    return;
  }
  shareSubmit.disabled = true;
  feedback(shareFeedback, '공유 기록을 저장하고 있어요…');
  try {
    await callWaitlist('waitlist_submit_share', {
      p_receipt: receipt,
      p_platform: platform,
      p_post_url: url,
    });
    feedback(shareFeedback, `${labels[platform]} 공유 신청을 기록했어요. 게시 확인 후 추가 혜택 대상으로 검토합니다.`);
    const button = document.querySelector(`.waitlist-share-platforms [data-platform="${platform}"]`);
    button.classList.add('is-submitted');
  } catch (_) {
    feedback(shareFeedback, '공유 기록을 저장하지 못했어요. 잠시 후 다시 시도해주세요.', true);
  } finally {
    shareSubmit.disabled = false;
  }
});
