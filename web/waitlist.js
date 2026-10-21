import { PLATFORMS, validPostUrl, shareTarget, referralCode, shareUrl, shareText, normalizeEmail, validEmail, normalizePostUrl, isOwnSiteUrl } from './waitlist-core.mjs?v=20261019-qa';
import { t, lang, onLangChange } from './i18n.js?v=20261019-qa';

const SUPABASE_URL = 'https://pupjzcmybuoyhzfwrsdf.supabase.co';
const SUPABASE_KEY = 'sb_publishable_jt74AKM32zdqnJlsFHEo2g_MNHa-WRO';
const RECEIPT_KEY = 'stepup.waitlist.receipt';
const EMAIL_KEY = 'stepup.waitlist.email';
const REF_KEY = 'stepup.waitlist.ref';
const $ = selector => document.querySelector(selector);
const dialog = $('.waitlist-dialog');
const termsDialog = $('.waitlist-terms');
const registerForm = $('.waitlist-form');
const emailInput = registerForm.elements.email;
const consentInput = registerForm.elements.consent;
const emailError = $('#waitlist-email-error');
const consentError = $('#waitlist-consent-error');
const shareSection = $('.waitlist-share');
const shareForm = $('.waitlist-share-form');
const postInput = shareForm.elements.postUrl;
const registerFeedback = $('.waitlist-feedback');
const shareFeedback = $('.waitlist-share-feedback');
const actionFeedback = $('.waitlist-share-action-feedback');
const statusFeedback = $('.waitlist-status-feedback');
const copyFeedback = $('.waitlist-copy-feedback');
const registerSubmit = $('.waitlist-form .waitlist-submit');
const shareSubmit = $('.waitlist-share-form .waitlist-submit');
const platformButtons = [...document.querySelectorAll('[data-platform]')];
const SOCIAL = ['x', 'instagram', 'threads'];
let receipt = '';
let registeredEmail = '';
try {
  receipt = sessionStorage.getItem(RECEIPT_KEY) || '';
  registeredEmail = sessionStorage.getItem(EMAIL_KEY) || '';
} catch { /* Storage can be disabled. */ }
let stage = 'register';
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
const label = key => t(`platform.${key}`);

function setMyCode(value) {
  myCode = referralCode(value);
  $('.waitlist-share-copy').textContent = `${shareText(lang)}\n${shareLink()}`;
}

function saveReceipt(value, email = registeredEmail) {
  receipt = value;
  registeredEmail = value ? email : '';
  try {
    if (value) { sessionStorage.setItem(RECEIPT_KEY, value); sessionStorage.setItem(EMAIL_KEY, registeredEmail); }
    else { sessionStorage.removeItem(RECEIPT_KEY); sessionStorage.removeItem(EMAIL_KEY); }
  } catch { /* The current page still works without session storage. */ }
}

// Instagram gets the square share card (2026-09-30 user choice: lighter than the promo film) — the 0.2 MB JPEG, not the 1.8 MB PNG.
const SHARE_MEDIA = [
  { src: './assets/img/waitlist-share-v2.jpg', name: 'stepup-share.jpg', type: 'image/jpeg' },
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

// The page's own message under the field, in the page language — not only the browser's bubble (which follows the browser language).
function fieldError(input, element, message) {
  element.textContent = message;
  element.hidden = !message;
  if (message) input.setAttribute('aria-invalid', 'true');
  else input.removeAttribute('aria-invalid');
}

async function callWaitlist(name, body) {
  const controller = new AbortController();
  // Slow 3G takes several seconds per request (2 s round trips) — give it room before calling it a network problem.
  const timeout = setTimeout(() => controller.abort(), 20000);
  try {
    let response;
    try {
      response = await fetch(`${SUPABASE_URL}/rest/v1/rpc/${name}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', apikey: SUPABASE_KEY },
        body: JSON.stringify(body), signal: controller.signal,
      });
    } catch {
      const error = new Error('network');
      error.network = true;
      throw error;
    }
    let result = null;
    try { result = await response.json(); } catch { /* Not JSON (gateway error page). */ }
    if (!response.ok || !result?.ok) {
      const error = new Error(result?.message || `HTTP ${response.status}`);
      // PostgREST answers 404 PGRST202 when the function (or this parameter list) does not exist yet.
      error.missingFunction = response.status === 404 || result?.code === 'PGRST202';
      throw error;
    }
    return result;
  } finally { clearTimeout(timeout); }
}

function platformStatus(item, key) {
  if (item) return t({ submitted: 'status.submitted', verified: 'status.verified', rejected: 'status.rejected' }[item.status] || 'status.share');
  return t({ youtube: 'status.youtube', tiktok: 'status.tiktok', reels: 'status.reels' }[key] || 'status.share');
}

function renderClaims(items) {
  claims = items;
  const complete = claims.filter(item => SOCIAL.includes(item.platform) && item.status !== 'rejected').length;
  $('.waitlist-progress').textContent = `${complete} / 3`;
  $('.waitlist-benefit').classList.toggle('is-complete', complete === 3);
  feedback(statusFeedback, complete === 3 ? t('fb.allShared') : '');
  for (const button of platformButtons) {
    const claim = claims.find(item => item.platform === button.dataset.platform);
    button.classList.toggle('is-submitted', !!claim && claim.status !== 'rejected');
    button.querySelector('.platform-status').textContent = platformStatus(claim, button.dataset.platform);
  }
}

// After signing up, every entry point says so — the page used to look exactly as before (still "join").
function renderEntry() {
  const joined = !!receipt;
  document.documentElement.classList.toggle('waitlist-joined', joined);
  $('.waitlist-banner-title').textContent = t(joined ? 'banner.titleDone' : 'banner.title');
  $('.waitlist-banner-text').textContent = joined && registeredEmail ? t('banner.textDone', { email: registeredEmail }) : t('banner.text');
  $('.waitlist-open').textContent = t(joined ? 'banner.buttonDone' : 'banner.button');
  $('.pill-join').textContent = t(joined ? 'header.joined' : 'header.join');
  for (const cta of document.querySelectorAll('.cta[data-waitlist-open]')) cta.textContent = t(joined ? 'cta.joined' : 'cta.join');
  $('.waitlist-done-email').textContent = registeredEmail ? t('done.email', { email: registeredEmail }) : '';
}

async function restoreClaims() {
  const currentReceipt = receipt;
  const revision = claimsRevision;
  if (!currentReceipt) return;
  try {
    const result = await callWaitlist('waitlist_status', { p_receipt: currentReceipt });
    if (receipt !== currentReceipt) return;
    setMyCode(result.referral_code);
    if (revision === claimsRevision) renderClaims(result.claims || []);
  } catch (error) {
    if (receipt !== currentReceipt || revision !== claimsRevision) return;
    if (error.message.includes('invalid waitlist receipt')) receiptLost();
    else feedback(statusFeedback, t('fb.statusFailed'), true);
  }
}

// The same email signed up somewhere else, so this window's receipt no longer counts. Keep the email in the field so one
// more press of the button fixes it (the old message said "register below" while the only button emptied the field).
function receiptLost() {
  const email = registeredEmail;
  saveReceipt('');
  setMyCode('');
  renderClaims([]);
  renderEntry();
  registerForm.reset();
  emailInput.value = email;
  showStage('register');
  feedback(registerFeedback, t('err.receiptLost'), true);
  consentInput.focus();
}

function closeShareForm() {
  platform = '';
  shareForm.hidden = true;
  for (const button of platformButtons) button.setAttribute('aria-pressed', 'false');
}

function showStage(next) {
  stage = next;
  const sharing = next === 'share';
  registerForm.hidden = sharing;
  shareSection.hidden = !sharing;
  dialog.classList.toggle('is-sharing', sharing);
  closeShareForm();
  dialog.setAttribute('aria-labelledby', sharing ? 'waitlist-share-title' : 'waitlist-title');
  for (const element of [registerFeedback, shareFeedback, actionFeedback, statusFeedback, copyFeedback]) feedback(element, '');
  fieldError(emailInput, emailError, '');
  fieldError(consentInput, consentError, '');
  $('.waitlist-scroll').scrollTop = 0;
}

// ── Opening and closing ───────────────────────────────────────────────
// Each open dialog adds one history entry, so the Back button (and Android's back gesture) closes the dialog instead of
// leaving the site. Closing it any other way removes that entry again.
let ignoreNextPop = false;
function openDialog(element, name) {
  if (element.open) return;
  element.showModal();
  element.openedAt = performance.now();
  try {
    history.pushState({ ...(history.state || {}), stepupDialog: name }, '');
    element.pushedAs = name;
  } catch { element.pushedAs = ''; }
}
function forgetHistory(element) {
  const name = element.pushedAs;
  element.pushedAs = '';
  if (name && history.state?.stepupDialog === name) { ignoreNextPop = true; history.back(); }
}
dialog.addEventListener('close', () => forgetHistory(dialog));
termsDialog.addEventListener('close', () => forgetHistory(termsDialog));
window.addEventListener('popstate', () => {
  if (ignoreNextPop) { ignoreNextPop = false; return; }
  for (const element of [termsDialog, dialog]) {
    if (element.open && element.pushedAs) { element.pushedAs = ''; element.close(); return; }
  }
});

// Close on a click outside — but only when the press also started outside, and not within half a second of opening:
// the second click of a double-click on the open button, or a text selection released outside, used to close it at once.
function closeOnBackdrop(element) {
  let pressedOutside = false;
  element.addEventListener('pointerdown', event => { pressedOutside = event.target === element; });
  element.addEventListener('click', event => {
    if (event.target !== element || !pressedOutside) return;
    if (performance.now() - (element.openedAt || 0) < 500) return;
    element.close();
  });
}
closeOnBackdrop(dialog);
closeOnBackdrop(termsDialog);

function openWaitlist() {
  if (dialog.open) return;
  const wanted = receipt ? 'share' : 'register';
  // Closing a dialog does not cancel a request; keep the in-flight form, and keep a half-filled link form as it was.
  if (!busy && stage !== wanted) showStage(wanted);
  if (wanted === 'share') prepareShareMedia();
  openDialog(dialog, 'waitlist');
  if (!busy && receipt) restoreClaims();
  if (!receipt) emailInput.focus();
}
for (const opener of document.querySelectorAll('[data-waitlist-open]')) opener.addEventListener('click', openWaitlist);
$('.waitlist-close').addEventListener('click', () => dialog.close());
$('.waitlist-done-close').addEventListener('click', () => dialog.close());
$('.waitlist-terms-open').addEventListener('click', event => {
  // The link sits inside the consent label; open the summary without toggling the checkbox.
  event.preventDefault();
  openDialog(termsDialog, 'terms');
});
// "동의하기" ticks the box it explains (the old "확인" read as agreeing but left the box empty).
$('.waitlist-terms-agree').addEventListener('click', () => {
  consentInput.checked = true;
  fieldError(consentInput, consentError, '');
  termsDialog.close();
});
$('.waitlist-terms-close').addEventListener('click', () => termsDialog.close());
$('.waitlist-change-email').addEventListener('click', () => {
  if (busy) return;
  saveReceipt('');
  setMyCode('');
  renderClaims([]);
  renderEntry();
  registerForm.reset();
  showStage('register');
  emailInput.focus();
});
$('.waitlist-share-form-close').addEventListener('click', () => {
  if (busy) return;
  const button = platformButtons.find(item => item.dataset.platform === platform);
  closeShareForm();
  button?.focus();
});
emailInput.addEventListener('input', () => { fieldError(emailInput, emailError, ''); feedback(registerFeedback, ''); });
consentInput.addEventListener('change', () => { if (consentInput.checked) fieldError(consentInput, consentError, ''); });

// ── Sign up ─────────────────────────────────────────────────────────
registerForm.addEventListener('submit', async event => {
  event.preventDefault();
  if (busy) return;
  const email = normalizeEmail(emailInput.value);
  let firstInvalid = null;
  if (!email) { fieldError(emailInput, emailError, t('err.emailEmpty')); firstInvalid = emailInput; }
  else if (!validEmail(email)) { fieldError(emailInput, emailError, t('err.emailInvalid')); firstInvalid = emailInput; }
  else fieldError(emailInput, emailError, '');
  if (!consentInput.checked) { fieldError(consentInput, consentError, t('err.consent')); firstInvalid ??= consentInput; }
  else fieldError(consentInput, consentError, '');
  feedback(registerFeedback, '');
  if (firstInvalid) { firstInvalid.focus(); return; }
  busy = true;
  registerSubmit.disabled = true;
  feedback(registerFeedback, t('wl.saving'));
  try {
    const body = { p_email: email, p_consent: true, p_trap: registerForm.elements.website.value };
    let result;
    // A referral must never block registration — but only an older server without p_ref is retried without it
    // (a typo used to send the request twice and still fail).
    if (invitedBy) {
      try { result = await callWaitlist('waitlist_register', { ...body, p_ref: invitedBy }); }
      catch (error) {
        if (!error.missingFunction) throw error;
        result = await callWaitlist('waitlist_register', body);
      }
    } else result = await callWaitlist('waitlist_register', body);
    if (!/^[0-9a-f]{64}$/.test(result.receipt || '')) throw new Error('Missing registration receipt');
    saveReceipt(result.receipt, email);
    setMyCode(result.referral_code);
    renderClaims([]);
    showStage('share');
    renderEntry();
    prepareShareMedia();
    $('#waitlist-share-title').focus({ preventScroll: true });
    // The share buttons work right away; the status check below no longer holds them (slow 3G: 2–6 s of dead taps).
    busy = false;
    registerSubmit.disabled = false;
    await restoreClaims();
  } catch (error) {
    if (error.message.includes('invalid waitlist registration')) {
      // The server said the address is not valid — say so, instead of "try again later" forever.
      fieldError(emailInput, emailError, t('err.emailInvalid'));
      feedback(registerFeedback, '');
      emailInput.focus();
    } else feedback(registerFeedback, t(error.network ? 'err.network' : 'err.server'), true);
  } finally {
    busy = false;
    registerSubmit.disabled = false;
  }
});

// ── Share ───────────────────────────────────────────────────────────
function labelShareForm(selected) {
  const config = PLATFORMS[selected];
  $('.waitlist-platform-title').textContent = t(config.creator ? 'form.creatorTitle' : 'form.shareTitle', { label: label(selected) });
  $('.waitlist-share-help').textContent = t(`help.${selected}`);
  $('.waitlist-creator-guide').hidden = !config.creator;
  $('.waitlist-publish').textContent = t(config.creator ? 'form.creatorPublish' : 'form.sharePublish', { label: label(selected) });
  postInput.placeholder = config.placeholder;
  // Instagram ignores shared text, so show the caption right in its form to copy.
  const caption = $('.waitlist-form-caption');
  caption.hidden = selected !== 'instagram';
  caption.textContent = selected === 'instagram' ? `${shareText(lang)}\n${shareLink()}` : '';
}

function openShareForm(selected) {
  platform = selected;
  const config = PLATFORMS[selected];
  if (selected === 'instagram') prepareShareMedia();
  for (const other of platformButtons) other.setAttribute('aria-pressed', String(other.dataset.platform === selected));
  $(config.creator ? '.waitlist-creator-form-slot' : '.waitlist-social-form-slot').append(shareForm);
  shareForm.hidden = false;
  shareForm.reset();
  postInput.removeAttribute('aria-invalid');
  feedback(shareFeedback, '');
  feedback(actionFeedback, '');
  labelShareForm(selected);
  const verified = claims.some(item => item.platform === selected && item.status === 'verified');
  shareSubmit.disabled = verified;
  postInput.disabled = verified;
  if (verified) feedback(shareFeedback, t('fb.verified'));
  shareForm.scrollIntoView({ block: 'nearest', behavior: 'instant' });
}

for (const button of platformButtons) {
  button.addEventListener('click', () => {
    if (busy) return;
    const selected = button.dataset.platform;
    // Already open: keep the pasted link and do not open another compose window — just go to the link field.
    if (platform === selected && !shareForm.hidden) {
      shareForm.scrollIntoView({ block: 'nearest', behavior: 'instant' });
      postInput.focus({ preventScroll: true });
      return;
    }
    openShareForm(selected);
    // X · Instagram · Threads share on the first tap; the form below takes the post link.
    if (!PLATFORMS[selected].creator) publish();
  });
}

async function copyText(text, element) {
  try {
    await navigator.clipboard.writeText(text);
    feedback(element, t('fb.copied'));
  } catch {
    feedback(element, t('fb.copyFailed'), true);
  }
}
document.querySelectorAll('.waitlist-copy').forEach(button => button.addEventListener('click', () => {
  const creator = shareForm.contains(button) && PLATFORMS[platform]?.creator;
  copyText(`${shareText(lang, creator)}\n${shareLink()}`, shareForm.contains(button) ? actionFeedback : copyFeedback);
}));

// Opens the channel's share flow. Runs straight from a tap, so file sharing keeps the tap it needs.
// A double click opens one window, not two (the second click used to land on "share again" as the form opened).
let lastPublish = { platform: '', at: -Infinity };
async function publish() {
  const selected = platform;
  if (!selected) return;
  const now = performance.now();
  if (lastPublish.platform === selected && now - lastPublish.at < 2000) return;
  lastPublish = { platform: selected, at: now };
  const config = PLATFORMS[selected];
  const message = `${shareText(lang, config.creator)}\n${shareLink()}`;
  if (selected === 'instagram' && touchDevice() && inAppBrowser) {
    feedback(actionFeedback, t('fb.inApp'), true);
    return;
  }
  if (selected === 'instagram' && touchDevice() && !mediaReady) {
    prepareShareMedia();
    lastPublish = { platform: '', at: -Infinity };
    feedback(actionFeedback, t('fb.loadingImage'));
    return;
  }
  // File sharing must start in the click handler, before awaiting any work.
  if (selected === 'instagram' && cardFile && touchDevice()) {
    feedback(actionFeedback, t('fb.opening'));
    try {
      // Share first: a clipboard write before it can use up the tap that the share menu needs (iOS).
      await navigator.share({ files: [cardFile] });
      // Instagram drops shared text, so offer the caption separately.
      const copied = await navigator.clipboard?.writeText(message).then(() => true, () => false);
      if (platform === selected) feedback(actionFeedback, copied ? t('fb.captionCopied') : '');
    } catch (error) {
      if (platform === selected) feedback(actionFeedback, error.name === 'AbortError' ? t('fb.shareCancelled') : t('fb.shareFailed'), error.name !== 'AbortError');
    }
    return;
  }
  window.open(shareTarget(selected, shareLink(), lang), '_blank', 'noopener,noreferrer');
  if (selected === 'instagram' || config.creator) {
    await copyText(message, actionFeedback);
    if (platform === selected && !actionFeedback.classList.contains('is-error')) {
      feedback(actionFeedback, t(config.creator ? 'fb.creatorOpened' : 'fb.instagramOpened'));
    }
  } else feedback(actionFeedback, t('fb.composeOpened'));
}
$('.waitlist-publish').addEventListener('click', publish);

// Clipboard images must be PNG. Build it from the light JPEG (0.2 MB) once, instead of downloading the 1.8 MB PNG again.
let sharePng;
function sharePngBlob() {
  sharePng ||= (async () => {
    const response = await fetch(SHARE_MEDIA[0].src);
    if (!response.ok) throw new Error('image');
    const bitmap = await createImageBitmap(await response.blob());
    const canvas = document.createElement('canvas');
    canvas.width = bitmap.width;
    canvas.height = bitmap.height;
    canvas.getContext('2d').drawImage(bitmap, 0, 0);
    return new Promise((resolve, reject) => canvas.toBlob(blob => (blob ? resolve(blob) : reject(new Error('png'))), 'image/png'));
  })();
  sharePng.catch(() => { sharePng = null; });
  return sharePng;
}
$('.waitlist-copy-image').addEventListener('click', async () => {
  // Say at once that something is happening, and drop the old "copied the text" line.
  feedback(copyFeedback, t('fb.imageCopying'));
  try {
    // Pass a pending blob so Safari keeps the tap that allows the clipboard write.
    await navigator.clipboard.write([new ClipboardItem({ 'image/png': sharePngBlob() })]);
    feedback(copyFeedback, t('fb.imageCopied'));
  } catch {
    feedback(copyFeedback, t('fb.imageCopyFailed'), true);
  }
});

postInput.addEventListener('input', () => {
  // An old success or error line no longer applies to what is in the field now.
  feedback(shareFeedback, '');
  postInput.removeAttribute('aria-invalid');
});

shareForm.addEventListener('submit', async event => {
  event.preventDefault();
  if (busy || !platform) return;
  const selected = platform;
  const currentReceipt = receipt;
  const url = normalizePostUrl(postInput.value);
  const fail = message => {
    feedback(shareFeedback, message, true);
    postInput.setAttribute('aria-invalid', 'true');
    postInput.focus();
  };
  if (!url) return fail(t('err.linkEmpty'));
  if (!validPostUrl(url, selected)) {
    return fail(t(isOwnSiteUrl(url) ? 'err.linkHome' : 'err.linkOther', { label: label(selected) }));
  }
  // Send (and show) the cleaned-up link — the server only takes lower-case hosts.
  postInput.value = url;
  busy = true;
  shareSubmit.disabled = true;
  claimsRevision++;
  for (const button of platformButtons) button.disabled = true;
  feedback(shareFeedback, t('fb.linkSaving'));
  try {
    const result = await callWaitlist('waitlist_submit_share', { p_receipt: currentReceipt, p_platform: selected, p_post_url: url });
    renderClaims(result.claims || [...claims.filter(item => item.platform !== selected), { platform: selected, status: result.status }]);
    feedback(shareFeedback, t(PLATFORMS[selected].creator ? 'fb.creatorSaved' : 'fb.linkSaved', { label: label(selected) }));
  } catch (error) {
    if (error.message.includes('invalid waitlist receipt')) receiptLost();
    else if (error.message.includes('duplicate share URL')) fail(t('fb.duplicate'));
    else if (error.message.includes('invalid share URL')) fail(t('err.linkOther', { label: label(selected) }));
    else feedback(shareFeedback, t(error.network ? 'err.network' : 'fb.saveFailed'), true);
  } finally {
    busy = false;
    shareSubmit.disabled = claims.some(item => item.platform === selected && item.status === 'verified');
    for (const button of platformButtons) button.disabled = false;
  }
});

// ── Language ────────────────────────────────────────────────────────
// i18n.js has just rewritten the fixed text; write the parts this script owns again in the new language.
onLangChange(() => {
  renderEntry();
  renderClaims(claims);
  setMyCode(myCode);
  if (platform && !shareForm.hidden) labelShareForm(platform);
  // A shown error would stay in the old language — clear it; pressing the button again shows it in the new one.
  for (const element of [registerFeedback, shareFeedback, actionFeedback, copyFeedback]) feedback(element, '');
  fieldError(emailInput, emailError, '');
  fieldError(consentInput, consentError, '');
});

setMyCode('');
renderClaims([]);
renderEntry();
// stepupcrew.com/#join (the crew invite page and the 404 page link here) opens the sign-up right away.
if (location.hash === '#join') {
  try { history.replaceState(history.state, '', `${location.pathname}${location.search}#film`); } catch { /* file:// */ }
  openWaitlist();
}
