# Redesign progress

## Active objective

Complete the whole StepUp application and all its screens/states for GASOK submission and real user testing. Full objective remains active until audited against implementation, build, APK, captures and functional evidence.

## 2026-09-24 — baseline and foundation

- Latest origin/main verified: d066164122a4dc31fbe1b4c88526e4d690f1bbdb.
- Dedicated branch: codex/stepup-cohesive-redesign. Includes two existing screenshot tooling commits, no unrelated user modifications.
- Approved visual direction and product facts are already settled through this conversation; no new direction selection or interview is needed.
- Product/design/agent rules recorded. Eight accepted concepts will be committed as reference only.
- Existing risks: logo calls accept arbitrary sizes; header implementations differ; nav policy uses loosely matched routes; run CTA is below the initial home viewport; screenshots include states, not distinct screens.

## Milestones

| Milestone | State | Evidence / next action |
|---|---|---|
| Full route/state inventory | in progress | Inspect root routes, dialogs, screens and existing capture fixtures |
| Locked shared chrome and guardrails | in progress | Implement tokens, fixed logo variants, route policy, tests |
| Startup → home → run → result/reward → wardrobe | pending | Preserve real initialization, tracking, data and ownership |
| All remaining screens and states | pending | Apply the same system; track every row |
| Native visual/interaction/device validation | pending | Build/CI/emulator, then device-only checks explicitly tracked |
| Final APK, screenshot gallery, change/test report | pending | Must all describe the same candidate revision |

## Environment / unverified dependencies

## 2026-09-24 — startup and home implementation checkpoint

- Foundation revision f7b7239: Build APK run 35897605972 passed. Screen Gallery run 35897605916 stopped before emulator execution because the new test referenced an unavailable Espresso class. Replaced it with the activity's real Back dispatcher; runtime verification is still pending.
- Home now uses a separate cinematic riverside background, the actual equipped avatar, and a pinned native run/resume button. Records, goal, energy, challenge and news remain available in a More sheet. This is an implementation checkpoint, not a visual approval.
- Startup now shows real preparation, a brief original-logo reveal, and retry on failure/timeout. Removed fabricated percentage progress, random shoe promotion and forced minimum loading duration. Reduced motion skips the reveal delay.
- Shared balance shows an em dash until its actual first value arrives.
- Main-tab gallery fixtures now use the production navigation shell. Added startup loading/reveal/error fixtures and a home details capture. Chrome tests also check the pinned CTA and details return at multiple viewport/font/theme settings.
- Generated background source and exact built-in ImageGen prompt are saved in design/redesign-2026-09/assets; native controls are not baked into the art.
- Revision 5de74ee capture APK compilation passed; emulator provisioning failed downloading the Android Emulator archive (`Error on ZipFile unknown archive`), before tests ran. This is not a successful runtime check. The next revision's run must still execute the actual tests.
- Run screen now has the shared focus header/back control, actual-equipped character, timer/metrics and pinned pause/resume/start action plus reachable finish confirmation. Course, goal and estimate controls remain in More. Existing tracking/settlement logic is retained; finishing returns home after clearing the result. Sunset scenery is a separate production asset.
- Local design contract, four-language resources and existing asset integrity checks pass. Next: build this revision, run emulator checks, inspect resulting screenshots, and repair any geometry/navigation defects before extending the run/result/wardrobe flow.

- Local Android SDK/JDK/adb are not on PATH. Existing GitHub Actions build and emulator capture infrastructure is available.
- No claim yet of real-device GPS/background behavior, signed-in financial flows, GIWA wallet integration, release signing or store readiness.
- No completion or blocking conclusion is justified: substantial local implementation remains available.
