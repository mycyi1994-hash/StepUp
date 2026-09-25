# StepUp

> Current user decision, 2026-09-25: ship the character-free, record-first direction from selected 05-record.png. Remove mascot/clothing/wardrobe presentation, keep shoe collection/market/draw and existing user data. Navigation is Running / Shoes / Draw / Community / Profile. Scenery is an independent horizontal banner. Older character-related requirements below describe history, not current release requirements. See docs/redesign/CHARACTER-FREE-ASSETS-2026-09-25.md.


<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Ordinary runners and people with limited cryptocurrency experience. Users arrive for exercise, expressive characters and customization, not a requirement to understand Web3. Confirmed by the user on 2026-09-24.

## Product Purpose

Make running inviting, make progress tangible, and offer an understandable path from verified activity to rewards and useful customization. Prepare a working GASOK submission and a real-user-testable Android app.

## Operating Context

Kotlin / Jetpack Compose native Android app. Four primary tabs: running, customize, community, profile. Preserve existing accounts, local running history, equipment, localization and supported themes. GPS/background behavior needs device evidence, not just screenshots.

## Capabilities and Constraints

- Keep `strideup.db` and `strideup_prefs` storage identities and migrations compatible.
- Google/Supabase login, activity tracking, server rewards, marketplace and GIWA integration must be assessed from current implementation and actual service availability.
- A screen capture is not proof of a functioning integration. Mock content is permitted only in explicitly marked test/demo fixtures.
- Do not claim rewards settled before server confirmation or invent a real balance.
- Existing lesser-used functionality remains reachable through appropriate secondary screens.

## Brand Commitments

STEPUP, RUNO and LUMI are established assets. The user approved the cinematic blue-night / sunset riverside direction in `design/redesign-2026-09/concepts/`. Preserve the existing transparent sporty wordmark used in those approved references; a different logo package elsewhere in the repo does not override the user's accepted reference.

## Product Principles

- Rich artwork; few decisions at once.
- Each screen has a clear purpose and a visible next action.
- Shared controls have one implementation and stable geometry.
- Make the first run and return visit work end to end.
- Report unverified external/device behavior explicitly.

## Evidence on Hand

Source baseline: main d066164. The prior screen-gallery capture tooling is retained. Previously captured 76 images include repeated states and are not a count of distinct routes. Approved concepts are illustrative, not production screenshots or factual reward amounts.

## Accessibility & Inclusion

Respect system Back, font scale, safe areas, reduced motion, TalkBack and 48 dp touch targets. Keep the primary action reachable on compact phones. Preserve localization and light/dark theme support.
