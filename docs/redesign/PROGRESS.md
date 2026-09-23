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
| Startup → home → run → result/reward → wardrobe | in progress | Startup/home/run implemented; wardrobe migration underway; screenshots and full flow still pending |
| All remaining screens and states | pending | Apply the same system; track every row |
| Native visual/interaction/device validation | pending | Build/CI/emulator, then device-only checks explicitly tracked |
| Final APK, screenshot gallery, change/test report | pending | Must all describe the same candidate revision |

## 2026-09-24 — result accessibility and reward presentation

- Build 35902803927 for 0ddfc53 completed with one failing unit test out of 176: the LUMI sheet-count test still included the newly added independent starter combination. Split the sheet's 52 design identities from the base/WND-010 starter assertion; retained checks for the five outfits and base art. This is a test expectation correction, not a passing build yet.
- The run result now keeps Done outside the scrolling content, removes the duplicate completion title, and uses the shared character stage without a second skyline. Pending rewards show an em dash; rejected/void rewards show zero; only server-confirmed rewards show a positive credit and celebration. Pending copy in all four languages describes a saved run awaiting confirmation.
- Added actual navigation assertions for the result Done action, pending/void reward presentation and restored chrome across the existing four viewport/theme/font scenarios. Fixtures validate presentation/navigation only, not server settlement or GPS.
- Local design contract, translated string/resource checks, asset integrity and whitespace checks pass. Native compilation, new assertions and result screenshot review remain pending. Previous gallery 35902803854 was still running during this change.

## Environment / unverified dependencies

## 2026-09-24 — wardrobe and native Back verification

- Revision 80beb62 native run 35900771499 passed ChromeNavigationTest across all four viewport/font/theme scenarios (XML: 1 test, 0 failures), then completed the gallery. Captures downloaded to C:/Users/gana0/StepUp-captures/redesign-80beb62. The gallery note still reports the signed-out My trades variation as inaccessible; this remains unverified despite the green job.
- Visual inspection found pixelated starter equipment sprites, a floating wardrobe character, and English Community wrapping onto two lines at 1.3 font scale and displacing its icon. Added high-resolution transparent starter RUNO and a new faithful LUMI/base/WND-010 combination, grounded home/wardrobe stages, and a single-line navigation label rule plus icon/label baseline assertions. These follow-up changes need fresh native verification; other equipment art remains to be upgraded.
- Revision 731899d Build APK 35901343580 and gallery 35901343495 both passed. This verifies compilation/tests for the contextual-permission change, not real-device GPS or every denied-permission flow.
- Result review found share copy asserting rewards before server confirmation and the completed state discarding the just-finished route. Pending/rejected/void shares now state exercise only; completed state retains its own track for sharing. End-to-end settlement/recovery verification remains outstanding.

- Revision 04dc0a6 Build APK run 35899906910 passed, including the release build. Capture run 35899907027 compiled and launched the emulator but failed at ChromeNavigationTest line 110 after dismissing the home sheet.
- The test called the activity Back dispatcher directly while a dialog window was open; this finished the activity instead of sending Back to the sheet. Replaced it with a real system Back key. The first viewport's tab/wallet/run assertions reached that line; the remaining viewport scenarios are not yet verified.
- The capture script now preserves chrome reports/screenshots and runs the full gallery even if a chrome assertion fails, while keeping the job failing on test failure. This prevents a test error from hiding all visual evidence.
- Wardrobe now has a large centered equipped/preview character, outfit/shoe categories and a separately scrollable item grid. Equip appears for a new selection. Character selection, market, vault and selected-shoe detail are reachable from More. Ownership and trial rules remain unchanged.
- Wardrobe terrace artwork and exact built-in ImageGen prompt saved with the other production scenes. Native labels and controls remain separate.
- Outstanding: actual screenshot review, wardrobe persistence/ownership interaction tests, result/reward redesign, other routes/states, permission onboarding and external/device functional validation. A default look can briefly precede stored avatar state in current view models; audit readiness before finalizing.
- Permission follow-up: browsing main tabs no longer launches activity/location/notification prompts. The run action requests access; the home step-access control asks only for activity recognition. Location requests include FINE and COARSE together and respect an existing approximate grant. This fixes a documented Android 12+ issue with FINE-only requests: https://developer.android.com/develop/sensors-and-location/location/permissions/runtime . Added policy tests for fresh install, approximate-only grants and older Android versions. Activity denial exposes app settings, and missing location permission is not mislabeled as GPS acquisition.

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
