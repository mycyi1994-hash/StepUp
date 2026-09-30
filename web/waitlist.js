import { SHARE_TEXT, CREATOR_TEXT, PLATFORMS, validPostUrl, shareTarget, referralCode, shareUrl } from './waitlist-core.mjs?v=20260930-youtube';

const SUPABASE_URL = 'https://pupjzcmybuoyhzfwrsdf.supabase.co';
const SUPABASE_KEY = 'sb_publishable_jt74AKM32zdqnJlsFHEo2g_MNHa-WRO';
const RECEIPT_KEY = 'stepup.waitlist.receipt';
const REF_KEY = 'stepup.waitlist.ref';
const $ = selector => document.querySelector(selector);
const dialog = $('.waitlist-dialog');
const registerForm = $('.waitlist-form');
const shareSection = $('.waitlist-share');
const shareForm = $('.waitlist-share-form');
const registerFeedback = $('.waitlist-feedback');
const shareFeedback = $('.waitlist-share-feedback');
const actionFeedback = $('.waitlist-share-action-feedback');
const statusFeedback = $('.waitlist-status-feedback');
const registerSubmit = $('.waitlist-form .waitlist-submit');
const shareSubmit = $('.waitlist-share-form .waitlist-submit');
const platformButtons = [...document.querySelectorAll('[data-platform]')];
let receipt = '';
try { receipt = sessionStorage.getItem(RECEIPT_KEY) || ''; } catch { /* Storage can be disabled. */ }
let platform = '';
let claims = [];
let cardFile;
let cardLoading;
let busy = false;
let claimsRevision = 0;
// The code from a friend's link (?ref=) that brought this visitor here.
let invitedBy = referralCode(new URLSearchParams(location.search).get('ref'));
try {
  if (invitedBy) sessionStorage.setItem(REF_KEY, invitedBy);
  else invitedBy = referralCode(sessionStorage.getItem(REF_KEY));
} catch { /* Storage can be disabled. */ }
// This visitor's own code, used in the links they share.
let myCode = '';
const shareLink = () => shareUrl(myCode);

function setMyCode(value) {
  myCode = referralCode(value);
  $('.waitlist-share-copy').textContent = `${SHARE_TEXT}\n${shareLink()}`;
}
setMyCode('');

function saveReceipt(value) {
  receipt = value;
  try {
    if (value) sessionStorage.setItem(RECEIPT_KEY, value);
    else sessionStorage.removeItem(RECEIPT_KEY);
  } catch { /* The current page still works without session storage. */ }
}

// Instagram gets the homepage promo film; the square card stays as the fallback.
const SHARE_MEDIA = [
  { src: './assets/video/stepup-promo-v5.1-mobile.mp4', name: 'stepup-promo.mp4', type: 'video/mp4' },
  { src: './assets/img/waitlist-share-v2.png', name: 'stepup-share.png', type: 'image/png' },
];
// Desktop share sheets do not list Instagram, so only phones use the system share menu.
const touchDevice = () => window.matchMedia?.('(pointer: coarse)').matches;
// In-app browsers (KakaoTalk, Instagram, Naver…) cannot hand files to other apps.
const inAppBrowser = /KAKAOTALK|Instagram|FBAN|FBAV|NAVER|Line\/|; wv\)/i.test(navigator.userAgent);
let mediaReady = false;

function prepareShareMedia() {
  if (cardLoading) return;
  if (!touchDevice() || !navigator.canShare) { mediaReady = true; return; }
  cardLoading = (async () => {
    for (const media of SHARE_MEDIA) {
      try {
        const response = await fetch(media.src);
        if (!response.ok) continue;
        const file = new File([await response.blob()], media.name, { type: media.type });
        if (navigator.canShare({ files: [file] })) { cardFile = file; break; }
      } catch { /* Try the next format. */ }
    }
    mediaReady = true;
  })();
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
      body: JSON.stringify(body), signal: controller.signal,
    });
    const result = await response.json();
    if (!response.ok || !result?.ok) throw new Error(result.message || 'Waitlist request failed');
    return result;
  } finally { clearTimeout(timeout); }
}

function renderClaims(items) {
  claims = items;
  const complete = claims.filter(item => ['x', 'instagram', 'threads'].includes(item.platform) && item.status !== 'rejected').length;
  $('.waitlist-progress').textContent = `${complete} / 3`;
  $('.waitlist-benefit').classList.toggle('is-complete', complete === 3);
  feedback(statusFeedback, complete === 3 ? '공유 3개 제출 완료! 특전 참여 기록을 남겼어요.' : '');
  for (const button of platformButtons) {
    const claim = claims.find(item => item.platform === button.dataset.platform);
    button.classList.toggle('is-submitted', !!claim && claim.status !== 'rejected');
    button.querySelector('.platform-status').textContent = claim
      ? ({ submitted: '제출 완료 ✓', verified: '확인 완료 ✓', rejected: '다시 제출' }[claim.status] || '공유하기')
      : ({ youtube: '영상 · 쇼츠', tiktok: '숏폼 영상', reels: '인스타그램' }[button.dataset.platform] || '공유하기');
  }
}

async function restoreClaims() {
  const currentReceipt = receipt;
  const revision = claimsRevision;
  try {
    const result = await callWaitlist('waitlist_status', { p_receipt: currentReceipt });
    if (receipt !== currentReceipt) return;
    setMyCode(result.referral_code);
    if (revision === claimsRevision) renderClaims(result.claims || []);
  } catch (error) {
    if (receipt !== currentReceipt || revision !== claimsRevision) return;
    if (error.message.includes('invalid waitlist receipt')) {
      saveReceipt('');
      showStage('register');
      feedback(registerFeedback, '등록 상태를 다시 확인할게요. 이메일을 입력해주세요.');
    } else feedback(statusFeedback, '기존 제출 현황을 불러오지 못했어요. 창을 다시 열어 확인해주세요.', true);
  }
}

function closeShareForm() {
  platform = '';
  shareForm.hidden = true;
  for (const button of platformButtons) button.setAttribute('aria-pressed', 'false');
}

function showStage(stage) {
  const sharing = stage === 'share';
  if (sharing) prepareShareMedia();
  registerForm.hidden = sharing;
  shareSection.hidden = !sharing;
  dialog.classList.toggle('is-sharing', sharing);
  closeShareForm();
  dialog.setAttribute('aria-labelledby', sharing ? 'waitlist-share-title' : 'waitlist-title');
  for (const element of [registerFeedback, shareFeedback, actionFeedback, statusFeedback, $('.waitlist-copy-feedback')]) feedback(element, '');
  $('.waitlist-scroll').scrollTop = 0;
}

$('.waitlist-open').addEventListener('click', () => {
  // Closing a dialog does not cancel a request; preserve the in-flight form.
  if (!busy) showStage(receipt ? 'share' : 'register');
  dialog.showModal();
  if (!busy && receipt) restoreClaims();
  if (!receipt) registerForm.elements.email.focus();
});
$('.waitlist-close').addEventListener('click', () => dialog.close());
dialog.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });
const termsDialog = $('.waitlist-terms');
$('.waitlist-terms-open').addEventListener('click', event => {
  // The link sits inside the consent label; open the summary without toggling the checkbox.
  event.preventDefault();
  termsDialog.showModal();
});
$('.waitlist-terms-close').addEventListener('click', () => termsDialog.close());
termsDialog.addEventListener('click', event => { if (event.target === termsDialog) termsDialog.close(); });
$('.waitlist-change-email').addEventListener('click', () => {
  if (busy) return;
  saveReceipt('');
  setMyCode('');
  renderClaims([]);
  registerForm.reset();
  showStage('register');
  registerForm.elements.email.focus();
});
$('.waitlist-share-form-close').addEventListener('click', () => {
  if (busy) return;
  const button = platformButtons.find(item => item.dataset.platform === platform);
  closeShareForm();
  button?.focus();
});

registerForm.addEventListener('submit', async event => {
  event.preventDefault();
  if (busy || !registerForm.reportValidity()) return;
  busy = true;
  registerSubmit.disabled = true;
  feedback(registerFeedback, '등록하고 있어요…');
  try {
    const body = {
      p_email: registerForm.elements.email.value.trim(),
      p_consent: registerForm.elements.consent.checked,
      p_trap: registerForm.elements.website.value,
    };
    // A referral must never block registration, e.g. before the server knows p_ref.
    const result = invitedBy
      ? await callWaitlist('waitlist_register', { ...body, p_ref: invitedBy }).catch(() => callWaitlist('waitlist_register', body))
      : await callWaitlist('waitlist_register', body);
    if (!/^[0-9a-f]{64}$/.test(result.receipt || '')) throw new Error('Missing registration receipt');
    saveReceipt(result.receipt);
    setMyCode(result.referral_code);
    renderClaims([]);
    showStage('share');
    $('#waitlist-share-title').focus({ preventScroll: true });
    await restoreClaims();
  } catch {
    feedback(registerFeedback, '등록하지 못했어요. 잠시 후 다시 시도해주세요.', true);
  } finally {
    busy = false;
    registerSubmit.disabled = false;
  }
});

for (const button of platformButtons) {
  button.addEventListener('click', () => {
    if (busy) return;
    platform = button.dataset.platform;
    const config = PLATFORMS[platform];
    if (platform === 'instagram') prepareShareMedia();
    for (const other of platformButtons) other.setAttribute('aria-pressed', String(other === button));
    $(config.creator ? '.waitlist-creator-form-slot' : '.waitlist-social-form-slot').append(shareForm);
    shareForm.hidden = false;
    shareForm.reset();
    feedback(shareFeedback, '');
    feedback(actionFeedback, '');
    $('.waitlist-platform-title').textContent = `${config.label} ${config.creator ? '크리에이터 참여' : '공유'}`;
    $('.waitlist-share-help').textContent = config.help;
    $('.waitlist-creator-guide').hidden = !config.creator;
    $('.waitlist-publish').textContent = config.creator ? `${config.label}에 영상 올리기 ↗` : `${config.label}에 다시 공유하기 ↗`;
    shareForm.elements.postUrl.placeholder = config.placeholder;
    const verified = claims.some(item => item.platform === platform && item.status === 'verified');
    shareSubmit.disabled = verified;
    shareForm.elements.postUrl.disabled = verified;
    if (verified) feedback(shareFeedback, '이미 확인이 끝난 게시물이에요. 다시 제출하지 않아도 돼요.');
    shareForm.scrollIntoView({ block: 'nearest', behavior: 'instant' });
    // X · Instagram · Threads share on the first tap; the form below takes the post link.
    if (!config.creator) publish();
  });
}

async function copyText(text, element) {
  try {
    await navigator.clipboard.writeText(text);
    feedback(element, '문구와 홈페이지 링크를 복사했어요.');
  } catch {
    feedback(element, '자동 복사가 안 됐어요. ‘공유 영상·이미지와 소개 문구’를 열고 문구를 직접 복사해주세요.', true);
  }
}
document.querySelectorAll('.waitlist-copy').forEach(button => button.addEventListener('click', () => {
  const creator = shareForm.contains(button) && PLATFORMS[platform]?.creator;
  copyText(`${creator ? CREATOR_TEXT : SHARE_TEXT}\n${shareLink()}`, shareForm.contains(button) ? actionFeedback : $('.waitlist-copy-feedback'));
}));

// Opens the channel's share flow. Runs straight from a tap, so file sharing keeps the tap it needs.
async function publish() {
  const selected = platform;
  if (!selected) return;
  const config = PLATFORMS[selected];
  const message = `${config.creator ? CREATOR_TEXT : SHARE_TEXT}\n${shareLink()}`;
  if (selected === 'instagram' && touchDevice() && inAppBrowser) {
    feedback(actionFeedback, '이 앱 안의 브라우저에서는 영상을 인스타그램으로 넘길 수 없어요. 오른쪽 위 메뉴에서 ‘다른 브라우저로 열기’(크롬·사파리)를 누른 뒤 다시 공유해주세요.', true);
    return;
  }
  if (selected === 'instagram' && touchDevice() && !mediaReady) {
    prepareShareMedia();
    feedback(actionFeedback, '공유할 홍보 영상을 불러오고 있어요. 잠시 후 다시 눌러주세요.');
    return;
  }
  // File sharing must start in the click handler, before awaiting any work.
  if (selected === 'instagram' && cardFile && touchDevice()) {
    feedback(actionFeedback, '공유 메뉴를 여는 중이에요…');
    try {
      // Share first: a clipboard write before it can use up the tap that the share menu needs (iOS).
      await navigator.share({ files: [cardFile] });
      // Instagram drops shared text, so offer the caption separately.
      const copied = await navigator.clipboard?.writeText(message).then(() => true, () => false);
      if (platform === selected) feedback(actionFeedback, copied
        ? '소개 문구를 복사했어요. 게시할 때 붙여넣고, 올린 게시물의 링크를 아래에 남겨주세요.'
        : '게시할 때 ‘소개 문구 복사’를 눌러 문구를 붙여넣고, 올린 게시물의 링크를 아래에 남겨주세요.');
    } catch (error) {
      if (platform === selected) feedback(actionFeedback, error.name === 'AbortError'
        ? '공유를 취소했어요. 다시 눌러 시도할 수 있어요.'
        : '공유 메뉴를 열지 못했어요. 위 ‘공유 영상·이미지와 소개 문구’에서 홍보 영상을 저장한 뒤 인스타그램에서 올려주세요.', error.name !== 'AbortError');
    }
    return;
  }
  if (selected === 'instagram' && !touchDevice()) {
    // Instagram on a computer has no share link: save the film, open Instagram and copy the caption.
    const save = document.createElement('a');
    save.href = SHARE_MEDIA[0].src;
    save.download = SHARE_MEDIA[0].name;
    save.click();
  }
  window.open(shareTarget(selected, shareLink()), '_blank', 'noopener,noreferrer');
  if (selected === 'instagram' && !touchDevice()) {
    await copyText(message, actionFeedback);
    if (platform === selected && !actionFeedback.classList.contains('is-error')) feedback(actionFeedback,
      '홍보 영상을 내려받고 인스타그램을 열었어요. 새 게시물에 영상을 올리고 복사된 문구를 붙여넣은 뒤, 게시물 링크를 아래에 남겨주세요.');
  } else if (selected === 'instagram' || config.creator) {
    await copyText(message, actionFeedback);
    if (platform === selected && !actionFeedback.classList.contains('is-error')) feedback(actionFeedback, config.creator
      ? '영상 업로드 화면을 열고 소개 문구를 복사했어요. 게시 후 링크를 남겨주세요.'
      : '인스타그램을 열고 문구를 복사했어요. 위 ‘공유 영상·이미지와 소개 문구’에서 홍보 영상이나 이미지를 저장해 게시해주세요.');
  } else feedback(actionFeedback, '글 작성 화면을 열었어요. 게시한 뒤 링크를 남겨주세요.');
}
$('.waitlist-publish').addEventListener('click', publish);

shareForm.addEventListener('submit', async event => {
  event.preventDefault();
  if (busy || !shareForm.reportValidity() || !platform) return;
  const selected = platform;
  const currentReceipt = receipt;
  const url = shareForm.elements.postUrl.value.trim();
  if (!validPostUrl(url, selected)) {
    feedback(shareFeedback, `${PLATFORMS[selected].label} 게시물 주소를 확인해주세요. 프로필이나 채널 주소는 제출할 수 없어요.`, true);
    return;
  }
  busy = true;
  shareSubmit.disabled = true;
  claimsRevision++;
  for (const button of platformButtons) button.disabled = true;
  feedback(shareFeedback, '링크를 저장하고 있어요…');
  try {
    const result = await callWaitlist('waitlist_submit_share', { p_receipt: currentReceipt, p_platform: selected, p_post_url: url });
    renderClaims(result.claims || [...claims.filter(item => item.platform !== selected), { platform: selected, status: result.status }]);
    feedback(shareFeedback, `${PLATFORMS[selected].label} 링크를 제출했어요. ${PLATFORMS[selected].creator ? '크리에이터 참여 기록도 함께 남겼어요.' : '게시물 확인 후 보너스를 안내할게요.'}`);
  } catch (error) {
    feedback(shareFeedback, error.message.includes('duplicate share URL')
      ? '이미 다른 채널에 제출한 게시물이에요. 새 게시물의 링크를 남겨주세요.'
      : error.message.includes('invalid waitlist receipt') ? '등록 상태를 다시 확인해야 해요. 아래에서 이메일을 다시 등록해주세요.'
      : '저장하지 못했어요. 잠시 후 다시 시도해주세요.', true);
  } finally {
    busy = false;
    shareSubmit.disabled = claims.some(item => item.platform === selected && item.status === 'verified');
    for (const button of platformButtons) button.disabled = false;
  }
});
