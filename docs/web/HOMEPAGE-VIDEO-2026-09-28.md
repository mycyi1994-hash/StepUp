# Homepage video and Korean copy

## Direction contract

- Scope: extend the existing homepage with the user's promotional video first; move the existing running scene to second. Retain the other scenes and interactions.
- Mode and audience: a Korean marketing page for people exploring StepUp. Lead with the actual supplied film, then let visitors explore running, crews, rewards and courses, and download the app.
- Visual language: preserve the incumbent dark navy, blue accents, original wordmark, Pretendard copy and Barlow numbers. This is an extension, not an Android design-system change.
- First viewport: the complete 16:9 film, without cropping, below the existing header. A short heading and an explicit link lead to the running scene. Mobile keeps the film's aspect ratio and stacks the caption and link.
- Behavior: muted inline autoplay with native controls, shared sound toggle, pause offscreen or when the tab is hidden, and preserve a visitor's manual pause. Reduced motion disables automatic playback. Existing fragment links remain valid.
- Finish: verify desktop/mobile rendering, playback and navigation; document asset provenance and actual verification results. No new business or company claims are introduced.

## Asset provenance

The user supplied `StepUp_홍보영상_v5.1.mp4` (164,159,351 bytes, 1920×1080, approximately 60.7 seconds). The complete video and audio are preserved in both web renditions. No new imagery or voiceover was generated.

- `web/assets/video/stepup-promo-v5.1.mp4`: 1920×1080 H.264/AAC, 8,472,231 bytes.
- `web/assets/video/stepup-promo-v5.1-mobile.mp4`: 1280×720 H.264/AAC, 4,058,663 bytes; selected at widths up to 740px.
- `web/assets/video/stepup-promo-v5.1-poster.jpg`: a frame from 00:02 of the supplied video, scaled to 1280×720.
- Both MP4s place metadata before media data (`faststart`) for progressive playback.

## Copy

- Running: 오늘의 한 걸음을 / 나만의 기록으로.
- Crew: 함께 달릴 크루를 만나보세요. → 다 모였네요. 이제 함께 달려요!
- Rewards: 달린 만큼 보상도 차곡차곡.
- Courses: 오늘은 어디를 달려볼까요?
- Start: 가벼운 한 걸음부터, / 함께 시작해요.

Existing illustrative data remains marked as a screen example. Legal pages and app functionality are outside this change.

## Verification

- JavaScript syntax, diff whitespace, shared design-contract and string-resource checks pass; all nine existing web tests pass.
- Desktop 1280×720: 1080p file plays automatically muted; sound toggle, film-to-running navigation and offscreen pause verified.
- Mobile browser viewport 390×844: 720p file selected, muted autoplay verified, full frame and all introduction controls visible, no horizontal overflow.
- Actual wheel input advances from film to running and pauses the offscreen film; PageDown advances from running to crew. No browser console errors were observed.
- Existing course carousel still advances and updates its title and details.
- Automated design warnings concern the incumbent scene effects, card styling and CTA palette; the added film section does not replace that established design.
- Physical mobile devices, reduced-motion playback, manual-pause preservation and media-error recovery were not independently exercised.

## Finish review

Verdict: **ship**. No material findings within the homepage extension scope: supplied film first, existing running scene second, natural Korean copy, responsive presentation and retained scene interactions. Review used the browser and check results above. The existing web visual language remains the authority for this extension; the Android design contract does not require a new system or token specification for this work.
