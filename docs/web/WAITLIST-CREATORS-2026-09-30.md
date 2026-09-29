# Homepage waitlist and creator sharing

## User direction

Bright, simple Toss-inspired registration; larger-bonus and all-three special-benefit copy; creator-only YouTube (red), TikTok (black), Reels (Instagram gradient); improved sharing images and copy; deploy live.

## Implementation

- White modal, slate text, blue actions, readable labels, fixed close button and responsive internal scrolling. Existing dark homepage and film remain.
- Registration → X/Instagram/Threads progress → optional creator video link submissions. Selecting a tile reveals channel-specific publishing help and the link form. Native image sharing is offered where supported; image download and copy are always available. X and Threads use composer intents. No automatic social posting.
- New square artwork and wide OG image; source prompts in `WAITLIST-ART-PROMPTS-2026-09-30.md`. Built-in image generation, inspected Korean text, copied to `web/assets/img/`.
- Supabase keeps per-platform links, submitted/verified/rejected state and timestamps. The operator-only summary derives all-three and creator participation flags. A receipt restores status without exposing email or submitted URLs. Duplicate Instagram/Reels URLs are rejected; verified entries cannot be overwritten.
- Bonuses remain for later: amount, kind, account ownership verification and payout are not implemented. User-facing copy says details follow at launch.

## Validation before deployment

- 13 Node tests passed, including content URL acceptance, spoofed-host/profile rejection and composed share text.
- Browser checks at 1440×1100, 390×844 and 320×740: all six submissions, client URL rejection, failed save + retry, 3/3 state, refresh restore, zero page errors and horizontal overflow. Separate captures cover registration, sharing and completion.
- Design contract (104 routes), string-resource check, JS syntax and diff whitespace checks passed.
- SQL tests added for six platforms, privacy, invalid receipts, duplicate URLs, normalization, candidate flags and verified-claim immutability. CI applies the full schema twice.
- Native OS share sheets and actual social account posting require the visitor's device/account; tested through browser fallbacks and request boundaries, not by publishing to user accounts. Link preview rendering can depend on platform cache.

Local browser evidence: `output/stepup-waitlist-implementation/creator-{desktop,mobile,small}-{register,share,completed}.png` in the chat workspace.
