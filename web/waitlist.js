const config = window.STEPUP_WAITLIST_CONFIG || {};
const dialog = document.querySelector('.waitlist-dialog');
const form = document.querySelector('.waitlist-form');
const openButton = document.querySelector('.waitlist-open');
const closeButton = document.querySelector('.waitlist-close');
const feedback = document.querySelector('.waitlist-feedback');
const submit = document.querySelector('.waitlist-submit');
const turnstileSlot = document.querySelector('.waitlist-turnstile');
let widgetId;
let challengeToken = '';
let challengeScript;

function message(value, error = false) {
  feedback.textContent = value;
  feedback.classList.toggle('is-error', error);
}

function loadChallenge() {
  if (!config.siteKey || widgetId !== undefined || challengeScript) return;
  challengeScript = document.createElement('script');
  challengeScript.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
  challengeScript.async = true;
  challengeScript.onload = () => {
    if (!window.turnstile || widgetId !== undefined) return;
    widgetId = window.turnstile.render(turnstileSlot, {
      sitekey: config.siteKey,
      action: 'waitlist',
      theme: 'dark',
      callback: token => {
        challengeToken = token;
        if (feedback.textContent.includes('자동 등록 방지')) message('');
      },
      'expired-callback': () => { challengeToken = ''; message('자동 등록 방지 확인이 만료됐어요. 다시 확인해주세요.', true); },
      'error-callback': () => { challengeToken = ''; message('자동 등록 방지 확인을 불러오지 못했어요. 다시 시도해주세요.', true); },
    });
  };
  challengeScript.onerror = () => {
    challengeScript.remove();
    challengeScript = undefined;
    message('자동 등록 방지 확인을 불러오지 못했어요. 잠시 후 다시 시도해주세요.', true);
  };
  document.head.append(challengeScript);
}

openButton.addEventListener('click', () => {
  form.reset();
  challengeToken = '';
  submit.disabled = false;
  submit.textContent = '확인 메일 받기';
  message('');
  dialog.showModal();
  loadChallenge();
  if (widgetId !== undefined && window.turnstile) window.turnstile.reset(widgetId);
  form.elements.email.focus();
});
closeButton.addEventListener('click', () => dialog.close());
dialog.addEventListener('click', event => { if (event.target === dialog) dialog.close(); });
dialog.addEventListener('close', () => {
  if (!dialog.open) challengeToken = '';
});

form.addEventListener('submit', async event => {
  event.preventDefault();
  if (!form.reportValidity()) return;
  if (!config.endpoint || !config.siteKey) {
    message('대기 명단 접수를 준비 중입니다. 잠시 후 다시 시도해주세요.', true);
    return;
  }
  if (!challengeToken) {
    message('자동 등록 방지 확인을 마쳐주세요.', true);
    return;
  }

  submit.disabled = true;
  message('확인 메일을 준비하고 있어요…');
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12000);
  try {
    const response = await fetch(config.endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        action: 'register',
        email: form.elements.email.value.trim(),
        consent: form.elements.consent.checked,
        turnstileToken: challengeToken,
      }),
      signal: controller.signal,
    });
    if (!response.ok) throw new Error(`waitlist ${response.status}`);
    message('신청을 확인했어요. 확인이 필요한 경우 메일함의 링크를 눌러 등록을 마쳐주세요.');
    submit.textContent = '메일을 확인해주세요';
  } catch (_) {
    message('접수하지 못했어요. 잠시 후 다시 시도해주세요.', true);
    submit.disabled = false;
    if (widgetId !== undefined && window.turnstile) window.turnstile.reset(widgetId);
    challengeToken = '';
  } finally {
    clearTimeout(timeout);
  }
});
