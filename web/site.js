(() => {
  'use strict';
  const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  const tabs = [...document.querySelectorAll('[data-scene]')];
  const captions = { run: '오늘도, 나만의 속도로.', crew: '우리 동네에서, 함께.', chat: '첫 인사부터, 편하게.' };
  let current = tabs[0];
  let transitionId = 0;
  function choose(tab, focus = false) {
    if (!tab || tab === current) { if (focus) tab?.focus(); return; }
    const revision = ++transitionId;
    const outgoing = document.getElementById(current.getAttribute('aria-controls'));
    document.querySelectorAll('.screen-panel').forEach(p => { p.getAnimations().forEach(a => a.cancel()); p.hidden = true; p.setAttribute('aria-hidden', 'true'); });
    const panel = document.getElementById(tab.getAttribute('aria-controls'));
    panel.hidden = false;
    panel.setAttribute('aria-hidden', 'false');
    if (!reduceMotion.matches && panel.animate) {
      outgoing.hidden = false;
      const exit = outgoing.animate([{ opacity: 1 }, { opacity: 0 }], { duration: 500, easing: 'ease', fill: 'forwards' });
      exit.finished.then(() => { if (revision === transitionId) outgoing.hidden = true; }).catch(() => {});
      panel.animate([
        { opacity: 0, transform: 'translate3d(7px, 9px, 0) scale(1.01)' },
        { opacity: 1, transform: 'translate3d(0, 0, 0) scale(1)' }
      ], { duration: 750, easing: 'cubic-bezier(.22,1,.36,1)' });
    }
    current = tab;
    tabs.forEach(t => { t.setAttribute('aria-selected', String(t === tab)); t.tabIndex = t === tab ? 0 : -1; });
    document.getElementById('scene-caption').textContent = captions[tab.dataset.scene];
    document.querySelector('.scene-index').textContent = `0${tabs.indexOf(tab) + 1} / 03`;
    if (focus) tab.focus();
  }
  tabs.forEach((tab, i) => {
    tab.addEventListener('click', () => choose(tab));
    tab.addEventListener('keydown', e => {
      let index;
      if (e.key === 'ArrowRight') index = (i + 1) % tabs.length;
      if (e.key === 'ArrowLeft') index = (i + tabs.length - 1) % tabs.length;
      if (e.key === 'Home') index = 0;
      if (e.key === 'End') index = tabs.length - 1;
      if (index !== undefined) { e.preventDefault(); choose(tabs[index], true); }
    });
  });
  if ('IntersectionObserver' in window && !reduceMotion.matches) {
    const observer = new IntersectionObserver(entries => entries.forEach(entry => {
      if (entry.isIntersecting) { entry.target.classList.add('is-visible'); observer.unobserve(entry.target); }
    }), { threshold: .1 });
    document.querySelectorAll('[data-reveal]').forEach(el => { el.classList.add('reveal-ready'); observer.observe(el); });
  }
  reduceMotion.addEventListener?.('change', () => {
    if (reduceMotion.matches) {
      transitionId++;
      document.querySelectorAll('.screen-panel').forEach(el => {
        el.getAnimations().forEach(a => a.cancel());
        el.hidden = el.id !== current.getAttribute('aria-controls');
      });
    }
  });
  document.addEventListener('visibilitychange', () => document.documentElement.toggleAttribute('data-page-hidden', document.hidden));
  const filmDialog = document.getElementById('film-dialog');
  const video = document.getElementById('brand-film');
  const infoDialog = document.getElementById('info-dialog');
  const openers = new WeakMap();
  function open(dialog, opener) { openers.set(dialog, opener); dialog.showModal(); document.documentElement.style.overflow = 'hidden'; }
  document.getElementById('film-open').addEventListener('click', e => {
    if (!filmDialog.showModal) return;
    e.preventDefault(); open(filmDialog, e.currentTarget);
    video.play().catch(() => { /* Native controls remain available if autoplay is disallowed. */ });
  });
  document.getElementById('experience-info').addEventListener('click', e => {
    if (infoDialog.showModal) open(infoDialog, e.currentTarget);
    else window.alert('오프라인 MVP 체험판입니다. 샘플 크루·자동 답장·예시 러닝을 사용하며, 입력한 내용은 기기에만 저장됩니다.');
  });
  document.querySelectorAll('[data-close]').forEach(button => button.addEventListener('click', () => document.getElementById(button.dataset.close).close()));
  [filmDialog, infoDialog].forEach(dialog => {
    dialog.addEventListener('click', e => {
      const rect = dialog.getBoundingClientRect();
      if (e.target === dialog && (e.clientX < rect.left || e.clientX > rect.right || e.clientY < rect.top || e.clientY > rect.bottom)) dialog.close();
    });
    dialog.addEventListener('close', () => { document.documentElement.style.overflow = ''; if (dialog === filmDialog) video.pause(); openers.get(dialog)?.focus(); });
  });
})();
