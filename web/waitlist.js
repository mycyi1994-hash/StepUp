import { SHARE_URL, SHARE_TEXT, CREATOR_TEXT, PLATFORMS, validPostUrl, shareTarget } from './waitlist-core.mjs?v=20260930-creator';

const SUPABASE_URL = 'https://pupjzcmybuoyhzfwrsdf.supabase.co';
const SUPABASE_KEY = 'sb_publishable_jt74AKM32zdqnJlsFHEo2g_MNHa-WRO';
const RECEIPT_KEY = 'stepup.waitlist.receipt';
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
$('.waitlist-share-copy').textContent = `${SHARE_TEXT}\n${SHARE_URL}`;

function saveReceipt(value) {
  receipt = value;
  try {
    if (value) sessionStorage.setItem(RECEIPT_KEY, value);
    else sessionStorage.removeItem(RECEIPT_KEY);
  } catch { /* The current page still works without session storage. */ }
}

function prepareShareCard() {
  if (cardLoading) return;
  cardLoading = fetch('./assets/img/waitlist-share-v2.png')
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
    if (receipt === currentReceipt && revision === claimsRevision) renderClaims(result.claims || []);
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
  prepareShareCard();
  if (!busy && receipt) restoreClaims();
  if (!receipt) registerForm.elements.email.focus();
});
$('.waitlist-close').addEventListener('click', () => dialog.close());
dialog.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });
$('.waitlist-change-email').addEventListener('click', () => {
  if (busy) return;
  saveReceipt('');
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
    const result = await callWaitlist('waitlist_register', {
      p_email: registerForm.elements.email.value.trim(),
      p_consent: registerForm.elements.consent.checked,
      p_trap: registerForm.elements.website.value,
    });
    if (!/^[0-9a-f]{64}$/.test(result.receipt || '')) throw new Error('Missing registration receipt');
    saveReceipt(result.receipt);
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
    for (const other of platformButtons) other.setAttribute('aria-pressed', String(other === button));
    $(config.creator ? '.waitlist-creator-form-slot' : '.waitlist-social-form-slot').append(shareForm);
    shareForm.hidden = false;
    shareForm.reset();
    feedback(shareFeedback, '');
    feedback(actionFeedback, '');
    $('.waitlist-platform-title').textContent = `${config.label} ${config.creator ? '크리에이터 참여' : '공유'}`;
    $('.waitlist-share-help').textContent = config.help;
    $('.waitlist-creator-guide').hidden = !config.creator;
    $('.waitlist-publish').textContent = config.creator ? `${config.label}에 영상 올리기 ↗` : `${config.label}에 공유하기 ↗`;
    shareForm.elements.postUrl.placeholder = config.placeholder;
    const verified = claims.some(item => item.platform === platform && item.status === 'verified');
    shareSubmit.disabled = verified;
    shareForm.elements.postUrl.disabled = verified;
    if (verified) feedback(shareFeedback, '이미 확인이 끝난 게시물이에요. 다시 제출하지 않아도 돼요.');
    shareForm.scrollIntoView({ block: 'nearest', behavior: 'instant' });
  });
}

async function copyText(text, element) {
  try {
    await navigator.clipboard.writeText(text);
    feedback(element, '문구와 홈페이지 링크를 복사했어요.');
  } catch {
    feedback(element, '자동 복사가 안 됐어요. ‘공유 이미지와 소개 문구’를 열고 문구를 직접 복사해주세요.', true);
  }
}
document.querySelectorAll('.waitlist-copy').forEach(button => button.addEventListener('click', () => {
  const creator = shareForm.contains(button) && PLATFORMS[platform]?.creator;
  copyText(`${creator ? CREATOR_TEXT : SHARE_TEXT}\n${SHARE_URL}`, shareForm.contains(button) ? actionFeedback : $('.waitlist-copy-feedback'));
}));

$('.waitlist-publish').addEventListener('click', async () => {
  const selected = platform;
  if (!selected) return;
  const config = PLATFORMS[selected];
  const message = `${config.creator ? CREATOR_TEXT : SHARE_TEXT}\n${SHARE_URL}`;
  // File sharing must start in the click handler, before awaiting any work.
  if (selected === 'instagram' && cardFile && navigator.canShare?.({ files: [cardFile] })) {
    try {
      await navigator.share({ files: [cardFile], title: 'StepUp 사전 등록', text: message });
      if (platform === selected) feedback(actionFeedback, '공유한 게시물의 링크를 아래에 남겨주세요.');
    } catch (error) {
      if (platform === selected) feedback(actionFeedback, error.name === 'AbortError'
        ? '공유를 취소했어요. 다시 눌러 시도할 수 있어요.'
        : '공유 메뉴를 열지 못했어요. 공유 이미지를 저장한 뒤 인스타그램에서 올려주세요.', error.name !== 'AbortError');
    }
    return;
  }
  window.open(shareTarget(selected), '_blank', 'noopener,noreferrer');
  if (selected === 'instagram' || config.creator) {
    await copyText(message, actionFeedback);
    if (platform === selected && !actionFeedback.classList.contains('is-error')) feedback(actionFeedback, config.creator
      ? '영상 업로드 화면을 열고 소개 문구를 복사했어요. 게시 후 링크를 남겨주세요.'
      : '인스타그램을 열고 문구를 복사했어요. 위의 공유 이미지를 저장해 게시해주세요.');
  } else feedback(actionFeedback, '글 작성 화면을 열었어요. 게시한 뒤 링크를 남겨주세요.');
});

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
