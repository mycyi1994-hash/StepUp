const heading = document.querySelector('#confirm-heading');
const detail = document.querySelector('#confirm-detail');
const params = new URLSearchParams(location.hash.slice(1));
const token = params.get('confirm');
history.replaceState(null, '', location.pathname);

if (!token || !/^[A-Za-z0-9_-]{32,128}$/.test(token)) {
  heading.textContent = '확인 링크를 찾을 수 없어요.';
  detail.textContent = '메일의 링크를 다시 열어주세요.';
} else {
  try {
    const response = await fetch(window.STEPUP_WAITLIST_CONFIG.endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action: 'confirm', token }),
    });
    const result = await response.json();
    if (!response.ok || !result.confirmed) throw new Error('confirmation failed');
    heading.textContent = '대기 명단에 등록됐어요.';
    detail.textContent = 'StepUp 출시 소식을 이 이메일로 보내드릴게요.';
  } catch (_) {
    heading.textContent = '확인 링크를 사용할 수 없어요.';
    detail.textContent = '링크가 만료됐거나 이미 사용됐을 수 있어요. 홈페이지에서 다시 등록해주세요.';
  }
}
