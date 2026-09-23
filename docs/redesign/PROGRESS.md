# Redesign progress

## Active objective

Complete the whole StepUp application and all its screens/states for GASOK submission and real user testing. Full objective remains active until audited against implementation, build, APK, captures and functional evidence.

## 2026-09-24 — inventory before purchase prompts

- Vault now leads with the actual equipped sneaker, before faction statistics and filters. Removed its duplicate inline upgrade purchase panel; the sneaker card opens the detailed cost-confirmation flow implemented in fe3dc17. Owned collection now precedes minting, while filters, copies, mint, boosts and guide targets remain reachable.
- Static checks pass. This layout still needs current native capture review; it is not marked visually complete.
- Downloaded bbe5a72 and 991048c API 34/35 artifacts. Each only retained one failed interaction testcase with an empty failure after emulator loss; neither provides evidence for new detail-screen rendering. 991048c Build APK passed. The 4GB guest-memory configuration first runs in dee6cc3, whose build/capture jobs are still active.

## 2026-09-24 — prevent stale market reads

- Board/model reloads now cancel the preceding read job, preserve coroutine cancellation and check activity before publishing results. Opening another model clears its predecessor's book; reopening a model in error retries instead of accepting its cached book as success. Bid captures the selected model before launching the request.
- Unexpected read/storage exceptions now produce an explicit retryable state instead of leaving loading active. These changes do not retry financial writes or claim server-side transaction idempotency. Native compilation and delayed-response runtime verification remain pending.

## 2026-09-24 — marketplace loading and ownership correctness

- Market balance failure previously became 0 SUP and personal-trades failure became an empty account. Both now expose their actual failure category. During board loading/error the balance is an em dash, and loading no longer renders empty-trade content. Model loading hides transactional content/dialogs until the fetch completes.
- Model sellable inventory was read from a WhileSubscribed StateFlow with no collectors, leaving its initial empty value. It now reads the first real Room inventory emission for the loaded model. Existing equipped restrictions remain intact.
- Added retry callbacks to marketplace error notices on the NFT board, runner market and model detail; authentication still uses the shared sign-in action. Static checks pass; real marketplace server transactions, ownership synchronization and native recovery execution remain unverified.

## 2026-09-24 — system ANR found in failed navigation capture

- Visually inspected ec91211 API 35 failed-wait-community-all-meetups.png: Android's "Pixel Launcher isn't responding" dialog overlays the crew screen. Live log confirms launcher input-dispatch ANR at 21:25:58 and guest CPU pressure avg10 80.40 / memory pressure 9.47. This execution cannot establish a StepUp Back-handler defect; it also does not prove that handler correct.
- Host memory remained available, so guest RAM is now explicitly 4096M with a 512M heap using supported android-emulator-runner inputs. Both API levels, all assertions and failure reporting remain enabled. This is an environment experiment pending runtime evidence, not a claim to have fixed disappearing emulators or IME failures.
- Capture runner now preserves guest memory at startup and last-ANR/window dumps after failures so system dialogs/focus failures can be distinguished from app state. Source-shell syntax validation is required; actual navigation validation remains open.

## 2026-09-24 — shared sign-in recovery

- Reused the existing board reauthentication behavior as SignInAgainButton, with a busy state and storage-error feedback. Board, crew and all MarketProblemNote consumers now share this control; previously crew/market sign-in errors offered no recovery action. It only changes the root login marker and does not clear local records or rewards.
- Static design, localized resources, asset and whitespace checks pass. Real OAuth, returning to the prior destination, and account-specific storage isolation remain unverified/unresolved; this UI change does not claim to solve them.
- fe3dc17 sneaker detail is committed locally. Waiting for bbe5a72 Build APK 35923005016 to finish before pushing so its release build is not cancelled. Its gallery 35923005015 is also live.

## 2026-09-24 — focused sneaker detail and energy evidence

- Sneaker detail now uses the shared pinned action: equip for an unequipped pair, open upgrade confirmation for an equipped upgradable pair. Stats expand on request; selling remains a secondary shared button with the existing equipped restriction. Removed the screen-local ActionTile button implementation and the inert fusion control (there is no fusion operation to preserve).
- Upgrade cost and next level are shown in a dismissible dialog; only explicit confirmation invokes the existing atomic repository upgrade. Added gallery coverage for initial primary-action visibility, opening/cancelling the dialog, unchanged inventory and its actual screenshot. Native compilation/capture is pending.
- ec91211 API 35 interaction XML reports 19 tests, 2 failures: chrome navigation timeout at line 127 and crew keyboard timeout at line 86. Both energy tests passed, including capacity deferral/idempotency and disk reopen recovery. Full gallery still failed; these results do not establish full navigation or capture stability. API 34 had lost the emulator before completing its suite.

## 2026-09-24 — meetup detail action and capture evidence

- Reviewed API 34 e16e1e2 native captures for vault, sneaker detail, meetup detail and market model. Legacy content remains too dense; meetup participation was below the initial viewport. Its primary join/lobby action now uses the shared DetailPage pinned footer, with full/closed states disabled. Leave and like remain secondary actions. Added an initial-viewport gallery assertion, pending native execution.
- Meetup descriptions no longer silently truncate after three lines; place values wrap and key meeting labels use readable text sizes. Removed an unconditional verified-host icon unsupported by any verification field. Like/unlike now has localized accessibility labels.
- e16e1e2 API 34 full gallery passed with empty capture-notes; interaction suite had 18 tests and one ChromeNavigationTest failure. Disk-backed energy close/reopen/retry test passed in that revision. This does not validate the newer energy-capacity test or real process death.
- ec91211 Build APK 35921962092 passed. Its API 34 gallery job lost the emulator during the first interaction test, producing an empty failure instead of a completed suite; physical-display capture did not establish stability. API 35 job still live when checked. Do not count unexecuted tests as passing. Full UI, external-service and device validation remain open.

## 2026-09-24 — preserve purchased energy at capacity

- Found that instant energy purchases charged even when the capped energy value could not increase. New purchases now check room for the full two energy before debit. Atomic DataStore delivery independently checks capacity again, so a refill between debit and delivery cannot silently discard paid energy or consume its receipt.
- Undelivered receipts remain pending; retry delivers without charging again once capacity is available. Already-applied receipts acknowledge idempotently even if current energy is full. Added four-language feedback and tests for full/partly-full no-charge, deferred application and replay after consumption. Native validation is pending; no completion claim.
- e16e1e2 Build APK 35921149269 passed; API 34/35 capture jobs 35921149272 remain running. Queued actual-display capture, dialog synchronization and v12 snapshot are ready for the next candidate.

## 2026-09-24 — finish-dialog synchronization finding

- Downloaded 73d6f71 capture 35920433661. Interaction XML: 18 tests, one failure at ChromeNavigationTest line 168 after system Back; other 17 pass, including new live boost expiry and invalid meetup-number correction. The second dialog opening immediately sent system Back without waiting for the window, unlike its first opening. Added a displayed-title assertion before Back so it targets the dialog window. Needs native confirmation; do not claim the test fixed yet.
- That run's full gallery still lost the emulator. Dialog timing and emulator disappearance are separate findings, not a single established root cause. Latest API 34/35 comparison remains running.

## 2026-09-24 — actual-display capture path

- Gallery, chrome, login and form captures now share UiAutomation.takeScreenshot rather than Compose node captureToImage. Images preserve the physical display (including dialog/IME windows and any surrounding viewport area); they are not cropped virtual-viewport images. Geometry assertions still use the same Compose semantics and all existing interaction/state checks remain.
- Central capture checks PNG writes and recycles the owned screenshot bitmap. This avoids repeated node-render capture and inconsistent dialog handling. It is a capture-path diagnostic, not proven to fix emulator loss. Existing older-node captures remain distinct evidence; fresh display captures must be reviewed.

## 2026-09-24 — compiler-generated v12 schema preserved

- 73d6f71 Build APK 35920433542 passed. Downloaded its StepUp-room-schemas artifact and copied the compiler-generated 12.json into app/schemas. Compared every serialized entity against committed v11: only energy_purchases was added; existing entity definitions are unchanged. This snapshot contains Room's actual generated identity hash.
- Pushed e16e1e2, including disk-reopen recovery coverage and the Android 14/15 comparison matrix, after the preceding build completed. Native results remain pending. Previous 73d6f71 gallery 35920433661 is still active.

## 2026-09-24 — renderer result and cross-version diagnosis

- 18b477c gallery 35919712989 failed during ChromeNavigationTest: expected 18 tests but none completed, emulator disconnected. Software mode selected swangle/lavapipe. Host kernel log again has no recorded OOM/segfault; streamed device logs stop without a StepUp fatal exception. Changing the renderer alone did not fix the issue.
- Added Android API 34 alongside 35 to the same full test/capture workflow, with fail-fast disabled and separate API-labelled artifacts. API 35 remains required and failing until actually repaired; API 34 is a diagnostic comparison and wider compatibility check, not a replacement or a green-only retry. No scene or interaction test was removed.

## 2026-09-24 — disk-backed energy recovery verification

- Upgraded EnergyPurchaseTest to use unique disk-backed Room and preference stores. After injected acknowledgement failure and partial energy consumption it closes Room, cancels/joins the DataStore scope, then recreates both from disk before replay. It verifies retained debit, no repeated energy restoration and one acknowledgement notification. Cleanup targets only this test's UUID-named files.
- This strengthens storage-reopen evidence; it is not an OS force-kill/device reboot test. Native execution is pending. Existing test passes used in-memory Room and must not be cited as proof of disk reopen.
- Build 35919712929 passed for 18b477c; its supported-renderer gallery 35919712989 remains active. Latest pushed 73d6f71 has Build APK 35920433542 and gallery 35920433661 running.

## 2026-09-24 — finish-dialog coverage and account boundary finding

- ChromeNavigationTest now opens finish confirmation, captures its actual dialog window, cancels via its button and system Back, and checks that the run screen remains accessible without main navigation. Also saves the already-asserted void-result state. Runs across the existing compact/large/font/theme configurations; execution is pending.
- Account audit: SessionHolder.signInWithGoogle replaces the stored session; signOut only clears auth. ConnectedAccountsScreen deletion clears server account/session/login marker but does not address local account-owned records. Room tables and preferences still share device storage. Cross-account record/balance isolation and post-deletion local handling remain major unresolved correctness requirements, not covered by existing persistence tests. Do not solve by silently erasing or renaming strideup.db/strideup_prefs.
- Requested any programme-provided GIWA embedding documentation asynchronously; independent implementation and verification continue.

## 2026-09-24 — generated schemas and submission evidence

- Build workflow now preserves generated Room schemas as a separate artifact so v12 can be reviewed/committed from actual Room compiler output. No hand-written identity hash or fake schema export. Existing migration tests passed in a9ff894, but generated v12 snapshot is still missing locally.
- Re-read official GASOK and GIWA connection pages; added GASOK-EVIDENCE.md linking requirements to actual evidence gaps. Public wallet documentation still says under development; no official embedding integration API has been verified. Native UI completion must not be confused with wallet embedding or real-user acquisition evidence.
- Latest published 18b477c Build APK 35919712929 and gallery 35919712989 remain running. Previous 57d916a gallery 35919048227 also remains running; do not restart on observation delay.

## 2026-09-24 — gallery completeness enforcement

- The gallery previously wrote variation/navigation failures to capture-notes.txt but still passed its test. It now gathers all possible captures and fails at the end if any requested state was missed. Earlier green gallery jobs must not be read as complete-state evidence.
- Investigated c469d45's missing My trades state: NftMarketSection returned on authentication/network problems before rendering its sub-tabs. Tabs now remain available in those states, with the truthful error in the selected content area. No balances, ownership or signed-in trading are fabricated. Existing capture attempts exercise both tabs; fresh native verification is pending.

## 2026-09-24 — gallery failure localized; supported renderer experiment

- c1641c1 diagnostic gallery 35918321776 failed. Host-side Android logs end after “Opening scene 5” (profile), before its capture. Host samples still show ~7.4 GB available immediately before emulator disappearance; kernel logs show no OOM kill/segfault, and emulator crash folder has no report. This narrows the failure but does not prove its cause.
- The job uses emulator 37.1.11 with swiftshader_indirect, which official Android documentation marks deprecated since 36.4.9. Switched the gallery to documented `-gpu software` selection while retaining API/device/tests unchanged. This is a renderer experiment, not a claim that crashes are solved; inspect its full gallery results. Source: https://developer.android.com/studio/run/emulator-acceleration

## 2026-09-24 — meetup form validation

- Removed silent numeric fallbacks/filtering from the meetup form: invalid distance/time/capacity remain editable and block submission with a localized explanation. Distance must be positive and finite, departure minutes positive, and capacity 2–200 (matching server bounds). Decimal-comma input is accepted; numeric keyboards are requested. Ordinary story/tip posts do not require meetup fields.
- Added actual Compose form coverage for blank departure, over-capacity, malformed/non-finite distance and recovery with decimal-comma input, without publishing a real event. Local design/string checks pass; native execution remains pending.
- c1641c1 build 35918321735 passed. Pushed through 57d916a for boost expiry, sign-in recovery and terrace blending; that candidate's build/captures are pending. Diagnostic gallery 35918321776 is still running.

## 2026-09-24 — terrace boundary visual correction

- Inspected actual a9ff894 compact profile/community captures. Shared chrome and the nickname/edit spacing are intact, but painting Night over terrace edges still leaves a rectangular boundary against the root gradient.
- TerraceStage now applies horizontal/vertical alpha masks to scenery alone, revealing the actual parent canvas. Character/content remain opaque and keep their existing bounds. This is native rendering, with original art unchanged. Local design/string checks pass; a fresh native capture is required before visual approval.

## 2026-09-24 — receipt tests verified; reauthentication recovery

- Downloaded a9ff894 run 35917606795 artifacts to StepUp-captures/redesign-a9ff894. Interaction XML reports 16 tests, zero failures, including energy delivery interruption/retry without repeated debit/credit, shoe and timed-boost transaction rollback/concurrency, version-six migration and unsupported-version data preservation. Full gallery still failed after the emulator disappeared. These interaction passes do not validate all scenes, live OAuth or GIWA.
- BoardSyncCard now gives SignInRequired a shared “Sign in again” action in four languages. Community Together/Stories and the course board all reuse it. It clears only the login UI marker, so StepUpRoot opens the actual login screen; it does not erase records, equipment or balances. Storage failure shows retry feedback. Native click/visual verification is pending.

## 2026-09-24 — emulator disappearance diagnostics

- Follow-up: a9ff894 Build APK 35917606763 passed. Its native interaction/gallery run 35917606795 remains active. Diagnostics revision c1641c1 has been pushed; Build APK 35918321735 and gallery 35918321776 are running.

- Gallery runs 35913660999 and 35916741243 both lost emulator-5554 during allScreens; their XML contains an empty failure and incomplete test count. Current evidence does not establish app crash, emulator crash or host memory exhaustion. The bounded runner now terminates with failure instead of hanging until the job cap.
- Capture runner now streams Android logs and host memory/process samples to host-side artifacts, captures kernel/emulator diagnostics on exit, and logs each scene before opening it. This preserves failure evidence even when adb can no longer reach the emulator. No test is skipped or retried to turn a failure green.
- Latest purchase receipt revision a9ff894 still awaits native build/test results. Full screen verification and final deliverables remain incomplete.

## 2026-09-24 — live boost expiration

- An active boost previously stayed in the subscribed UI until a Room table change, despite its expiry passing. The flow now schedules its next emission at the nearest expiry and cancels/replaces that schedule when Room emits new data. Empty lists do not run a timer. This updates the existing Items/Run subscribers without requiring navigation or extra database writes.
- Added native coverage that inserts a short-lived boost and keeps one subscription open through its expiry, then verifies the inactive repository state. Local source/resource checks pass; this new test has not run yet. Financial settlement still uses its own actual-time checks and is not proven by this display test.

## 2026-09-24 — recoverable energy purchase delivery

- Added Room v12 energy_purchases receipt table with additive 11→12 migration. Energy purchase debit/receipt/history commit together. DataStore applies energy and records receipt identity in one edit; Room acknowledges delivery and writes the notification together afterward. Startup replays pending receipts; pressing purchase with a pending receipt retries delivery without another debit. Existing ledgers/energy values and storage identities are preserved.
- UserPrefs accepts an optional DataStore for isolated tests; production still uses strideup_prefs. Added test failure injection before receipt creation and after energy delivery but before acknowledgement, including consumption between failure/retry to prove replay cannot restore spent energy again. Included energy and existing migration tests in the capture verification phase. Native execution/schema validation are pending, not claimed passes.
- Receipt IDs remain retained for idempotence; account switching/deletion and receipt retention policy still require the wider account-data audit. This repairs new purchase recovery, not historic partial purchases lacking receipts or actual GIWA settlement.

## 2026-09-24 — timed boost purchase consistency

- Timed boosters now use the same Room database transaction boundary as local shoe purchases, covering duplicate-active checks, available funds, debit, activation and notification. UI storage exceptions show retry feedback. Added native rollback coverage and a concurrent shoe-versus-booster test with funds insufficient for both; tests remain pending CI.
- ENERGY_CELL remains unresolved: it writes energy into DataStore as well as Room, so a Room transaction alone would falsely imply atomicity. It needs a durable receipt/replay design that prevents both lost credit and double restoration across process death. No prior energy/balance entries are removed. Global wallet consistency is not yet established because other writers also remain in the audit.
- Local design/resource/asset checks pass; preceding Build APK 35916741262 remains active, so these queued transactional changes are not pushed yet.

## 2026-09-24 — atomic local shoe purchases

- SneakerRepository now wraps starter creation, upgrade and local mint in Room transactions. Upgrade/mint balance check/debit, equipment mutation and notification succeed or roll back together; concurrent repository purchases serialize before checking funds. Storage failures surface the existing retry message through ItemsViewModel. Existing storage identities, amounts and ownership rules are unchanged.
- Added native tests for concurrent starter creation, injected notification failure after upgrade/mint writes, preserved balance/equipment on rollback, and eight concurrent mint requests with funds for exactly one. Test is pending CI, not a claimed pass. This covers local inventory operations only; it is not on-chain minting or marketplace settlement. Other reward/boost writers still need their own transaction audit.
- Local design/string/asset checks pass. Build 35916741262 for 7bfd37c is still the preceding candidate; this transactional change needs its own build and native result.

## 2026-09-24 — partial native scene review

- Inspected 859bba8's actual 360dp dark Home/Customize/Community/Profile captures, plus Community/Profile at 1.3 font scale. Home/wardrobe starter art and fixed main chrome render; profile's three destinations remain visible. Profile/community characters now stand against the same local ground plane, but the landscape's left/right edges still form a harsh rectangle. Added shared horizontal edge blending to TerraceStage; native follow-up remains required.
- Profile nickname/edit control have no gap in these captures. Added 8dp spacing while retaining the 48dp shared edit target. English Community navigation label ellipsizes at 1.3 font scale; this is observed and still needs a final accessibility/product decision, not hidden as a visual pass.
- Community capture is signed out and shows no meetup. This verifies that state only, not an authenticated meetup or external service. All imagery/geometry assessment here is scoped to the older 859bba8 revision; the recent detail migrations still need native evidence.

## 2026-09-24 — first stalled-gallery evidence recovered

- 859bba8 gallery 35912849285 terminated cancelled at the workflow deadline. Downloaded partial artifacts to C:/Users/gana0/StepUp-captures/redesign-859bba8: 55 PNGs and interaction XML with eight tests, zero failures. The whole gallery did not complete; this is only scoped interaction evidence.
- Logs show the emulator was no longer found at 20:07:23 UTC during gallery artifact transfer, followed by no further progress until cancellation at 20:28:34. The final unbounded adb logcat could wait for a missing device. Added a 20-second logcat limit and a 60-second limit to each existing artifact-transfer attempt, retaining failure status. This addresses diagnostic hanging, not the unproven cause of emulator loss.

## 2026-09-24 — prevent local chrome regression

- Completed a source scan of screen-local wordmark calls: only approved Splash/Login launch roles remain. Extended the global guard to reject other screen-local wordmarks and direct back-arrow chrome in every screen (not just individually migrated files). This enforces shared implementations; it does not prove all screen layouts match the concepts.
- HistoryMap period chips now wrap rather than being constrained to a single Row, retaining all choices at larger fonts. Native map height/filter wrapping still needs capture verification.
- Local design/resource/asset checks pass. Latest Build APK 35915951416 and earlier gallery 35912849285 remain live at this checkpoint.

## 2026-09-24 — secondary button token consistency

- VoltButton and GhostButton now share explicit secondary label/padding tokens and the existing 48dp touch-height token. Labels increase from 13sp to 14sp and center when wrapping; primary action remains the separate 60dp/18sp role. Updated the design contract and source guard against divergence.
- Local source/resource/asset checks pass. This affects many secondary actions, so compact/large-font captures still must establish wrapping and surrounding layout; no visual pass is implied by the guardrail.
- f84b5ca Build APK 35915951416 and Gallery 35915951385 are running. Older 859bba8 gallery 35912849285 is still live, with no completed artifact to inspect.

## 2026-09-24 — equipment save failures

- Vault and shoe-detail equip failures now show retry feedback instead of silently returning or allowing an uncaught storage exception. Customize gender/outfit/shoe writes likewise report storage failures; coroutine cancellation still propagates. Success messages follow the existing successful writes. This does not yet audit upgrade/mint/purchase transactions or serialize all avatar changes.
- Extended the real Room equipment test with an injected failing UPDATE trigger, preservation of the prior equipped shoe, and a successful retry after the storage failure clears. Native execution remains pending; local source/resource/asset checks pass.
- fbd387a Build APK 35915223394 completed successfully. Map/vault/course/capture diagnostics and this equipment follow-up can now be pushed for fresh validation without cancelling that build.

## 2026-09-24 — bounded capture diagnostics

- Earlier gallery 35912849285 remains authoritatively in progress. Its job-log endpoint returns 404 before completion, so the stall location is not known; no inferred cause or restart is justified. NightCanvas and TerraceStage source have no self-running animation that alone establishes a cause.
- Future capture runs now bound each instrumentation phase to nine minutes, preserve nonzero exit status, save a failure display/logcat and record phase exit codes. Only after an actual timeout do they stop the instrumentation/app processes to allow the independent gallery phase to run. This reserves artifact-upload time before the existing 30-minute workflow cap; it does not turn missing tests into a pass. Bash syntax check passes; native behavior of the diagnostic path remains pending.
- fbd387a Build APK 35915223394 remains running. Map/vault/course and diagnostic changes are committed locally pending that result before the next push.

## 2026-09-24 — vault and course detail structure

- Items/Vault and CourseHub now use DetailPage instead of local title/back rows and gutters. Vault keeps its balance and Dex destination in content, with an explicitly labeled shared Dex button replacing the 42dp custom icon target. Store/market/vault tabs and course selection/recording/community actions remain in place. Source rules cover both screens.
- This pass fixes structural consistency only. Vault balance readiness, mint/enhance transaction correctness, course permission/recovery and detailed large-font UI still need review. Local design/resource/asset checks pass; current native jobs remain running.

## 2026-09-24 — map detail chrome and queued build

- Map and HistoryMap now use SecondaryHeader and StepUpDesign.Gutter while preserving their weighted interactive map area, filters and bottom context. Gallery opens both actual routes. Architecture checks reject screen-local back/header code in these map screens. Native large-font, map interaction and GPS permission coverage remain outstanding.
- 0d78b62 Build APK 35914435874 completed successfully. Pushed accumulated changes through fbd387a (meetup/history/collection, shoe/market, party lobby and crew board) for fresh compile/native validation. Earlier galleries 35912849285 and 35913660999 remain live; no final captures or visual approval are available from them yet.

## 2026-09-24 — crew board shared fixed action

- DetailPage now owns an optional primary action label/callback with the existing PrimaryCta and fixed spacing. CrewBoard uses it for member-only writing, replacing a separately styled floating button and guessed bottom padding. Posts scroll in the remaining space without being covered by the writing control. Invite sharing remains a labeled shared button in content; the header is the same back/title as other detail pages.
- CrewBoard gallery now uses its actual navigation route. Local design/resource/asset checks pass. Native compact/large-font footer layout, sharing chooser and writing/Back remain pending; shared slot introduction is not proof of those interactions.

## 2026-09-24 — party lobby shell and exit path

- Party lobby now uses DetailPage with its group identity/member count in content. Added system Back handling matching the existing explicit leave/back action. The activity-permission callback now opens the running screen after starting the service, and checks that the party is still RUNNING before starting; previously it started the service but left the user in the lobby.
- Local design/string/asset checks pass; real multi-user lobby lifecycle, denied-permission recovery and background tracking still require runtime validation. CrewBoard still has its own share header/floating compose control and is the next structural migration.
- Build 35914435874 and gallery 35912849285 are still confirmed in progress. Pending local commits remain unpushed until the build completes, with no workflow restart or completion claim.

## 2026-09-24 — owned shoe and marketplace model details

- SneakerDetail and MarketModel now use the shared DetailPage. Model art moved from a tiny header thumbnail into a dedicated content preview; model identity remains the shared title. Existing enhance/equip/sell and market ask/bid controls are retained. Gallery uses their actual routes and architecture checks reject local back controls.
- Local design, four-language and asset checks pass. These changes and af2480d await a push after the active 0d78b62 Build APK 35914435874 reaches a terminal state. Native layout, long model names and transaction behavior remain unverified for these screens; this migration is not a marketplace completion claim.

## 2026-09-24 — meetup, collection and history detail shell

- Flash-run detail, Sneaker Dex and Analytics now use DetailPage's pinned back/title, gutter and scrolling rules. Removed the meetup screen's separate wordmark and inert notification button. Existing meetup participation/chat, collection filters/details and history/chart destinations remain in the content.
- Gallery fixtures now open all three production navigation routes. Source architecture checks cover their shared chrome. Local design/string checks pass; compilation, native layout and inner-state review are pending.
- 0d78b62 Build APK 35914435874 and gallery 35914436196 are running. Earlier galleries 35912849285 and 35913660999 are still confirmed live, not treated as failures or restarted. Their workflow has a 30-minute cap; wait for terminal state before diagnosing or retrieving final artifacts. New terrace composition has not yet received native visual approval.

## 2026-09-24 — ranking and achievements shared detail chrome

- Ranking and Achievements now use DetailPage, removing per-screen back/title layouts and literal content gutters. Ranking categories/periods/remote states remain reachable; achievement progress stays in its existing summary, avoiding a duplicate counter in the header. Added source guardrails and switched both gallery cases to their real MainScaffold routes. This is structural migration; detailed typography, loading/error coverage and native visual review remain outstanding.
- a71c7e8 Build APK 35913661076 completed successfully, including unit tests, debug APK and release build. This allows pushing the queued invitation/reward changes and this migration without cancelling that build. 859bba8 gallery 35912849285 remains live; no new visual approval is claimed.

## 2026-09-24 — remove unverified notification payouts

- Removed synthetic welcome seeding from production startup and removed NotificationRepository's direct local reward-credit method/dependency. Legacy notification rows and previously credited ledger entries are preserved. Unprocessed legacy reward notices now explain that no verified payment record exists and open the real Challenges route, where server confirmation already gates challenge claims. This does not retroactively verify old credits or implement a welcome campaign on the server.
- Welcome sample data now exists only in androidTest/TestData. Added a production-navigation test checking that the legacy notice reaches Challenges without changing the balance or deleting the notice. Gallery notification capture now uses MainScaffold. Native tests are pending; local resource, design and asset checks pass.
- a71c7e8 Build APK 35913661076 has passed unit tests and debug APK; release build is still running. Prior terrace gallery 35912849285 is still executing. ce5d40b and this follow-up are queued locally to avoid cancelling the active build before its result is known.

## 2026-09-24 — crew invitation confirmation

- Crew invitation handling now checks the actual join result: only Joined/Requested consumes the notification. Failed/missing-target/exception paths retain it for retry; an already processed entity is not resubmitted. The screen gates duplicate accept/decline while joining and shows localized failure, sign-in or approval-request feedback. Coroutine cancellation is propagated and busy state is always cleared.
- Added native coverage for failure, thrown transport error, invalid target, requested membership and repeat handling, using the real notification DAO and controlled server responses. Local resource/design/asset checks pass; native execution remains pending. This test does not establish actual signed-in crew service availability or cross-device consistency.
- Production welcome rewards remain an unresolved separate path: startup unconditionally calls seedWelcome, and local notification claim currently credits the embedded amount. EventRepository's server-confirmed challenge path does not cover this synthetic welcome entry. Preserve existing credited balances when addressing it.

## 2026-09-24 — notification detail and read semantics

- Notifications now uses the fixed DetailPage header/gutter instead of a screen-local back/title layout. Initial repository loading is distinct from an empty inbox. Mark all read calls the read update, not clearAll (which deleted history); the action is shown only for unread items. Added architecture protection and a native Room test preserving ordinary history, pending actions and all fields except read through repeated reads. New native test is pending CI.
- c469d45 downloaded XML confirms eight tests with zero failures, including the strengthened actual-IME visibility test. Its Compose-only capture excludes the keyboard window, so added a full-display capture for direct visual review; do not treat the blank lower part of the Compose capture as the real keyboard appearance.
- 859bba8 Build APK 35912849430 passed debug/release and unit tests. Its gallery remains running. New terrace composition still needs native inspection.
- Further notification audit found production startup seeds a locally claimable welcome reward, and crew invite acceptance marks actioned without checking the join result. These are unresolved financial/action truthfulness defects, not validated features. Existing balances must remain preserved while fixing the payout source and failure handling.

## 2026-09-24 — wallet and preference verification follow-up

- b263e80 downloaded XML has eight tests, zero failures, including full-ledger totals beyond the 100-entry history limit. Native wallet capture confirms shared detail chrome, readable current-status notice and unchanged parent navigation. Increased remaining tiny hero labels to 14sp and allowed amount/unit wrapping for large amounts/fonts; new typography still needs capture review.
- Notification settings previously announced saved and dispatched synchronization before the local write completed. Success now follows the awaited local save; failure exposes retry feedback, cancellation propagates, and controls are gated during initial preference loading/save. Remote delivery/synchronization remains separately unverified.
- c469d45 Build APK 35912132177 passed. Its newly strengthened visible-keyboard test is still running; do not carry forward the earlier weaker pass as proof of keyboard avoidance.

## 2026-09-24 — character-ground composition

- Created a landscape riverside terrace with the built-in image tool, preserving the approved sunset world and reserving a continuous ground plane. Original PNG and exact prompt are in design/redesign-2026-09/assets; WebP is bundled.
- Profile and community now use TerraceStage so landscape and characters share the same responsive bounds; removed their unrelated full-screen sunset background. Actual equipped profile character and decorative community group remain separate and unchanged. Native review must still verify grounding, alpha margins and light/dark blending at all sizes; this is not final visual approval.
- Chrome fixture now includes production NightCanvas, matching StepUpRoot rather than exposing the test activity's white window on non-scenic routes.
- b263e80 wallet gallery 35911348022 completed successfully; detailed totals-test evidence and captures remain to be reviewed.

## 2026-09-24 — shared settings detail page

- Added DetailPage: one pinned SecondaryHeader, fixed design gutter, and scrollable content with shared spacing. Migrated Language, Theme, Notifications, Privacy, Support and Experience settings; removed their separate back/title layouts while preserving controls and destinations.
- This is a structural redesign, not proof that every existing setting/help text is correct. Screen-level typography, persistence failures, real notification delivery and full accessibility remain in the audit.
- Source checks passed. 6a0c053 gallery 35910775823 has now completed successfully; detailed form evidence is being downloaded. b263e80 wallet build/native checks are still running.
- Downloaded 6a0c053 XML confirms seven tests passed, including form input. However, visual inspection found no visible IME and a white fixture background absent from the real StepUpRoot. Added production NightCanvas to the fixture, enabled software keyboard with hardware-keyboard emulation, and require actual IME visibility before accepting form access. The prior pass proves text input/gating, not keyboard avoidance.

## 2026-09-24 — connected accounts

- Replaced screen-local back/title chrome with the shared detail header and scrollable content. Account names/statuses stack to preserve space at larger fonts; status copy is 14sp.
- Login account status now reflects the locally stored session, with an em dash while checking; it no longer always says disconnected. This is not an online token-validity check. GIWA/Health Connect are explicitly unsupported in this build rather than suggesting an active integration or imminent release.
- Account deletion keeps the explicit user confirmation; unexpected exceptions now restore retry availability instead of leaving the dialog permanently busy. No real account deletion was executed. Server deletion/local-data cleanup still require a dedicated authenticated validation.
- Source contract and four-language checks passed; native visual/state validation pending.

## 2026-09-24 — wallet truthfulness and shared detail structure

- Wallet now uses the shared detail header above scrollable content, replacing its separate logo/back/title row. Initial balance/totals show an em dash, and ledger loading is distinct from a confirmed empty history.
- Found cumulative totals summing only the latest 100 ledger entries. Added one full-ledger Room aggregate for balance/positive credits/debits, with a 126-entry native test that also checks the 100-row recent-history window remains bounded. No stored entries or schema identities change.
- Removed the withdrawal button/dialog that performed no withdrawal and asserted unsupported 1:1 conversion, a 1,000-SUP minimum and launch eligibility. Four-language GIWA copy explicitly describes this build's missing connection/withdrawal support and the app-recorded SUP view. This is honest interim UI, NOT completion of the required GIWA integration.
- Wallet gallery now goes through its real root route. Source contract/string checks passed; new aggregate and wallet visuals still need native verification.
- 1b6eb54 Build APK 35910105134 and gallery 35910105018 passed; detailed test XML/captures are being downloaded for review.
- Downloaded 1b6eb54 XML confirms six tests and zero failures, including the transaction rollback/concurrent retry test. This proves local receipt atomicity under the injected failure, not server response-loss recovery. 6a0c053 Build APK 35910775780 also passed; its native form suite is still pending at this checkpoint.

## 2026-09-24 — post composition

- Post composition now uses the same shared focus header and pinned native primary action as crew creation, with scrollable content and IME padding. Categories wrap instead of overflowing at enlarged font sizes; flash-run distance/start/capacity fields stack vertically instead of squeezing three labels into one row.
- Preserved all post categories, crew targeting and flash-run fields. Unexpected write exceptions restore the posting state and keep the draft visible with the existing failure notice; coroutine cancellation propagates. Real server idempotency after ambiguous network responses still needs work.
- Gallery now renders post composition through the production root route, so safe areas and form chrome are actually exercised. Source design/string checks passed; native visual, keyboard and authenticated posting validation remain pending.

## 2026-09-24 — verified space correction and form coverage

- efd1c88 Build APK 35909320324 and gallery 35909320120 passed. Downloaded XML shows five tests, zero failures: navigation/profile actions, equipment persistence, two run-total/verification tests and login presentation. Community Back passed with bounded state synchronization; this single pass does not prove all timing cases.
- Inspected native profile/community captures: all three profile destinations and the meetup title/place/time/distance now fit in the regular viewport above navigation/action. Remaining visual issue: smaller character art appears suspended relative to the fixed scene perspective; faithful seated/grounded artwork or scene composition still needs work. Demo gallery content is fixture data, not a verified live meetup or balance.
- Added CrewFormTest through the actual root route: name/area input, blank-name gating, reachable pinned submit and 1x/1.3x fonts without posting to the server. Capture runner preserves form screenshots; native execution pending.
- Root navigation now consumes Scaffold's applied insets before child IME padding, preventing forms from adding the same system safe-area padding twice. This shared change requires the complete chrome regression suite.

## 2026-09-24 — crew creation form

- Crew creation uses the shared focus header, scrolling inputs and a pinned primary action with IME padding. Removed redundant preview explanation; retained live identity preview and all fields/policy choices.
- Shared community input labels are 14sp, input/placeholder text 16sp, editable targets at least 48dp with accessible labels. Crew policy now uses the shared two-way selector and 14sp explanation. These shared changes also affect post/course forms and crew management; native visual/keyboard validation remains required.
- Creation is guarded synchronously against duplicate taps; exceptions clear busy state and surface the existing failure notice. This does not make a server request idempotent after a lost response; real authenticated creation/recovery is still unverified.
- Local source contract, translated resources and whitespace checks pass. Latest prior efd1c88 build/capture still running while this change was prepared.

## 2026-09-24 — atomic local event receipt

- Replaced the separate claim marker, credit and notification writes with a Room transaction. Duplicate concurrent local responses cannot add a second credit, and a notification/ledger write failure rolls the whole local operation back. Non-finite/non-positive receipt amounts are rejected.
- Added a real Room failure-injection test using a SQLite trigger to abort the last write, then retry ten concurrent calls and verify exactly one receipt, credit and notification. Included it in the native suite; compilation/runtime execution pending. Existing schema and database identity remain unchanged.
- This fixes local atomicity only. A server payout whose response is lost, another-device claim, and legacy markers without corresponding credit still need authenticated receipt reconciliation. The existing server RPC rejects repeat claims, and its response does not carry a receipt amount on rejection; do not invent a recovery credit.

## 2026-09-24 — scene space and evidence review

- Inspected native 0bcd210 challenge and d7283d1 community captures. Community's fixed 250dp illustration pushes the real meetup information under the pinned action; profile's fixed 280dp illustration similarly pushes Wallet below the initial viewport. Replaced both fixed heights with available-space/font-aware art sizing, preserving shared chrome and scroll access. Added a normal-font profile destination visibility assertion and large-font scroll reachability checks. Native verification pending.
- 0bcd210 downloaded XML proves four tests passed: chrome/navigation, exclusive equipment persistence, full-history run totals, and exclusion of unconfirmed/flagged/void runs from challenge progress. The gallery still reports the signed-out My trades variation as missing; a green run does not prove this state covered.
- e7bf047 login compiled and unit tests passed; Build APK 35908357461 and native run 35908357497 were still executing at this checkpoint.
- Follow-up source audit: EventRepository.claim writes the claimed marker before RewardRepository.credit without a shared local transaction. A local write failure between these calls can leave a claimed marker without the local credit, and AlreadyClaimed handling only writes a zero marker. Reconciliation/atomicity needs a dedicated fix and failure-injection tests; no actual money or server state was changed during this audit.
- Wallet screenshot review exposed fabricated fiat conversion (balance * 0.01) and a fixed +0.51% change in the wallet card and reusable token component. Removed both unsupported market-value displays; stored SUP balances and ledger entries are unchanged. Wallet layout, chain integration and ledger truthfulness still need the broader audit.
- e7bf047 Build APK 35908357461 passed. Native run 35908357497 failed ChromeNavigationTest at line 120 after Crew system Back; the login and three storage tests passed (downloaded XML), and nine login fixtures were recovered. Inspected 2x-font error capture: retry/error readable and legal actions were scroll-reachable in the test. Added a bounded wait for the actual community return state and a screenshot on timeout; this is diagnostic synchronization, not proof the navigation defect is resolved.

## 2026-09-24 — login presentation and recovery

- Login now uses the shared launch wordmark, cinematic scene and decorative runner group, one native Google action, readable error/consent copy and reachable terms/privacy links. The official Google G asset replaces the text imitation; source and legal URLs are recorded in the asset note.
- Unexpected sign-in exceptions restore retry availability; coroutine cancellation propagates instead of masquerading as a failed account. Existing account/session persistence remains unchanged.
- Added production LoginContent interaction fixtures at 360x640, 1x/1.3x/2x font: busy input disabled, retry clears an error, and both legal actions are reachable. These are presentation tests, not real OAuth verification; native run pending.
- Source design contract, four-language resources, experience assets and whitespace checks passed. Whole-app completion remains unproven.
- d7283d1 gallery 35906332769: downloaded XML proves 3 navigation/storage tests passed, and gallery test succeeded, but chrome images were not recovered (adb stopped at file enumeration). Added three bounded artifact-transfer attempts without suppressing test failures. The underlying transport cause is unconfirmed.
- 0bcd210 Build APK 35907161448 and gallery 35907161579 succeeded. Capture download/review is in progress; this does not verify real GPS, login, wallet or settlement.

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
