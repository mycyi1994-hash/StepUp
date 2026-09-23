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

## 2026-09-24 — focused challenge flow

- Challenges now show one selected daily/weekly/night target at a time, an actual equipped character and a pinned primary action. The action becomes Claim only for an eligible unclaimed target; while a request is active it is disabled, and request errors restore retry ability. Loading progress/claim state is distinct from zero. Original target/reward definitions remain unchanged, rather than copying example values from the reference image.
- Shared SecondaryHeader now uses the fixed 48dp back component and header-height token, and supports a detail title. Challenge captures use the real navigation shell. Added three challenge captures and four-viewport navigation checks from Profile through each selector and Back.
- Inspection of supabase/migrations/0014_events.sql found night progress excludes FLAGGED/VOID server records. App progress previously counted recent 1,000 sessions regardless of verification. It now uses all SIGNED, non-FLAGGED/non-VOID sessions, then applies the existing local-time 20:00 boundary. Added Room coverage proving pending/rejected/flagged/void records are excluded from challenge eligibility while history still retains them. Real server claim reconciliation remains unverified.
- d7283d1 Build APK 35906332879 passed. Its gallery 35906332769 remains live. Downloaded 2bafc3e gallery 35905430477: XML shows 3 tests/0 failures (chrome including profile settings Back, equipment persistence, all-record run totals). This does not validate the new challenge changes.
- Inspected native navigation-profile.png: character and shared chrome render correctly; the wallet row requires scrolling at the gallery viewport. Profile hero height/scene grounding and seated reference pose need further refinement. No whole-app completion claim is supported.

## 2026-09-24 — community together/stories flow

- Community now opens with a decorative warmup group and one actual upcoming public meetup. The selector excludes expired, full, private and unscheduled posts, chooses the soonest start and has no fabricated fallback. Native title/place/date/distance are repository data; View meetup opens the detail before any participation change. The selector refreshes its time boundary while displayed and has unit coverage.
- Together/Stories are the main content choices. Other meetups opens the full flash list; Stories keeps ordinary posts and writing. Crew and map stay reachable through shared icon controls. System Back returns from crews/meetup list; chrome checks and gallery variations now cover these paths. First-use tour copy/target follows the new choices rather than pointing at a sometimes-absent write action.
- Loading/sign-in/failure state remains explicit, including when cached posts exist; only a successful empty response shows no meetups. The group illustration is not a member list. Its original PNG and exact prompt are stored with the production assets; converted WebP is 1536x1024 with alpha extrema 0..254.
- 2bafc3e Build APK 35905430519 failed compiling ClaimUploadTest.FakeDao because the newly added RunTotals query was missing from the fake. Added its real aggregate over the fake rows. Application compilation had passed; unit execution and the new community changes still require fresh verification.
- 737ef71 gallery 35904606624 completed successfully. 2bafc3e gallery 35905430477 was still running during this change. Neither is evidence for the new community layout. Full authenticated meetup/create/join/error flows remain unverified.

## 2026-09-24 — profile redesign and record summary

- Profile now leads with the actual equipped character, nickname, two saved-run totals and three destinations (records, challenges, wallet). Settings stays reachable through a shared icon control; inventory, goal, profile editing and the remaining tools stay inside settings. Character tap opens Customize rather than the unrelated inventory screen. The cinematic sunset uses the root scene while the original main header/navigation remain unchanged.
- Profile no longer briefly renders a default male character before persisted appearance arrives. Record totals have an explicit loading state and aggregate every saved session, independent of passive daily steps or the recent-list limit. Added a Room test with 205 sessions plus passive steps to validate this distinction.
- Profile settings uses the shared focus title/back control; system Back returns to the profile content. Added geometry and Back assertions in all existing four viewport/font/theme scenarios. Screenshot review, profile contrast and full state validation remain pending; the current portrait is standing until a faithful seated-art set is available.
- Run completion explanation now sits on the shared card surface, addressing poor contrast against sunset imagery seen in the 5324bb5 360dp/1.3-font capture.
- Downloaded 22d51bf gallery 35903932113. Its XML reports 2 tests, 0 failures: ChromeNavigationTest and EquipmentPersistenceTest. Equipment switching, missing target, concurrent writes and reopening assertions passed. This does not prove marketplace/server ownership or storage-error recovery.
- 737ef71 Build APK 35904606952 passed including unit tests/debug/release stages. Its gallery 35904606624 is still live. These results precede this profile change and are not validation of it.

## 2026-09-24 — shared floor anchor and starter outfit thumbnails

- CharacterStage now compensates for each source image's transparent bottom margin using generated alpha-derived geometry and the actual Fit image height. It aligns the visible silhouette to the shared floor instead of adding per-screen offsets. Art pixels are unchanged. Regenerate geometry with tools/gen_avatar_res.py after adding sprites; Pillow is required for this read-only measurement.
- Added separate transparent dimensional RUNO zip hoodie/shorts and LUMI pullover/shorts inventory art matching their equipped starter references. Original PNGs and exact generation prompts are saved under design/redesign-2026-09/assets. Runtime WebP assets have alpha extrema 0..255 and 1254px square dimensions. Base ownership and outfit IDs are unchanged. Native thumbnail and grounding review is pending.
- 5324bb5 gallery 35903620682 completed successfully; report/capture download underway. Its Build APK run was cancelled by the next revision, not passed. Equipment revision 22d51bf build 35903932174 and gallery 35903932113 were still live during this work.

## 2026-09-24 — equipment persistence repair

- Replaced separate clear/update equipment writes with one guarded SQL update. A missing target leaves the current equipment untouched; concurrent selections cannot leave multiple equipped rows; only the equipment flag changes, preserving current stats. Repository returns success explicitly, and wardrobe/vault no longer report success for a missing item.
- Added a Room device test for switching, a missing target, concurrent selection requests, database reopening and preserved level/inventory. Included it in the gallery workflow alongside chrome checks. This test has not yet run; storage-error messaging and broader ownership/authentication flows remain outstanding.
- 0ddfc53 gallery 35902803854 completed successfully; artifact download/visual inspection is underway. Its separate Build APK job failed the stale LUMI test described above. Newer 5324bb5 build and gallery remain live; no final candidate is verified yet.
- Downloaded 0ddfc53 to C:/Users/gana0/StepUp-captures/redesign-0ddfc53. Inspected navigation-home.png and screen-28.png: the new starter is sharp and its exterior is transparent. Feet remain visibly above the floor ring, and the base outfit thumbnail is a low-detail outline unlike the dimensional character; both require further visual work. capture-notes.txt still reports the inaccessible signed-out My trades variation. Green gallery status does not resolve that missing capture.

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
