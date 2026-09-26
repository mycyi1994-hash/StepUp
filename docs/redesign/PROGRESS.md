# Redesign progress

## 2026-09-25 — interrupted-run recovery connected (PR #22)

- The service now writes `RunCheckpointStore` (`noBackupFilesDir/run-checkpoint.bin`, format 2 adds the fake-location flag and the party crew) every 5 s while recording, writes the SETTLING boundary before settlement and clears the matching checkpoint after durable completion. Settlement does not start if the SETTLING boundary cannot be written (FAILED/Retry instead).
- A new start is refused while an unresolved checkpoint exists; the app asks first. Unreadable checkpoints are moved aside (renamed, not deleted).
- On reopen, `RunRecoveryDialog` offers Resume (paused, no downtime added, fresh step baseline) or Finish and save. A SETTLING checkpoint is re-saved without asking (the Room receipt prevents a second credit). A run from another account (or from before sign-in) is only saved under its original owner, without its details or the current account's shoe/course/crew.
- Exercise time is accumulated from `SystemClock.elapsedRealtime()` deltas excluding pauses instead of counting 1 s ticks.
- Evidence: compile, unit tests, lint, androidTest compile. Native `RunCrashRecoveryTest` (finish saves once and clears; resume is paused with 300 s and keeps the fake-location flag) and `RunCheckpointPersistenceTest.fakeLocationFlagSurvivesReopen` run in the CI interaction suite. Physical process termination on a device has not been run; background GPS behavior and uploading phone-voided runs remain separate work.

## 2026-09-25 — app stage 5 (2): server economy (balance, sneakers, draws)

- The server is the only source of SUP, sneakers, energy and boosts (decision A, server records only). `EconomySync` replaces the phone's `rewards`/`sneakers`/`boosts` tables with the server's (`my_economy`, `my_sneakers`, `sup_ledger`, `draw_grants`, `boosts`) on app start, sign-in, run upload and screen open. Draws, upgrades, repairs, equips and boosts are server functions; the phone never credits or debits.
- Run result: the SUP amount appears only after the server confirms the run; phone estimates are never shown as earned. Background steps and course completions no longer credit on the phone.
- Screens: draw tab performs the server draw and opens the new shoe; the draw card shows remaining free draws; server shoes show efficiency/comfort/durability with a repair action; ledger labels for draw/repair/course/wallet rows. Luck copy removed (FAQ, tour).
- Legacy phone-only shoes are uploaded once per account as IMPORT keepsakes (the default pre-login starter is skipped); phone-only SUP is not carried over.
- Local evidence: compile, all unit tests (new `EconomyApiTest`), lint, release/test APK assembly, string/design checks. Signed-in states (server draw result, repair, free-draw label, confirmed run amount) need a real account and are not captured by the emulator gallery.

## 2026-09-25 — app stage 5 (1): mock GPS, faction board removal, wallet page entry

- Ranking: the faction (종족) board is removed with the server `faction_leaderboard` function (migration 0028) — four boards remain (speed/time/SUP/crew). The gallery's `ranking-factions` scenario is removed.
- Wallet: the GIWA card now has one native "open wallet page" action. It opens `web/wallet.html` in a Custom Tab with the login token after `#`; without a session it shows a sign-in notice, without a token refresh an offline notice. New gallery scenario `extra-wallet-page-sign-in` captures the guest state.
- Not visual: mock-location runs are void and reported to the server; profile multiplier uses the equipped shoe; the login session moved to a backup-excluded file (a restored legacy session is discarded).
- Local evidence: `compileDebugKotlin`, `compileDebugAndroidTestKotlin`, all `testDebugUnitTest`, string and asset checks. Device captures come from the PR #20 Experience QA run on the same revision; the signed-in page itself, real wallet linking and transactions are not captured here.

## 2026-09-25 — character-free stage 9, courses/map/ranking

- Reordered course cards around name/distance, schematic route, reward/record and one native selection action. Removed the duplicate radio control; kept existing selection and ownership data. Short course and ranking-period tabs remain on one line in the 1.6× font capture. Nearby map now links directly to the existing course list.
- Android 14/15 `explore` galleries cover the three routes, three viewport/font combinations and course/territory/ranking inner states. The first final-source pass at `236a084` passed the build and 48 captures; visual review found the map summary claimed off-screen courses were on the visible map. Source `597a639` now separates total registered count from visible pins and distinguishes the current-location marker. Final build/capture links and outcome: [stage 9 report](CHARACTER-FREE-STAGE9-2026-09-25-KO.md).
- The map tile/GPS environment, pin selection, server rankings/territory, other themes/locales, whole-app release, main merge and public APK remain outside this visual checkpoint.

## 2026-09-25 — character-free stage 8, record and wallet routes

- Analytics now shows the chart and summary before its history-map entry; the wallet shows balance, totals and real ledger before the connected-wallet notice. History map, achievements and notifications retain their existing data and empty states.
- A bounded real-device gallery covers five routes, three viewport/font combinations and the quarter/all-period variants: 21 screenshots on each of Android 14 and 15. First review found an ambiguous quarter-chart target line; it was removed. Quarter distance precision, zero-spend tint and empty-map zoom controls were corrected; map controls are 48dp when shown.
- Final app source `a36055c` passed [Build APK 36108963617](https://github.com/mycyi1994-hash/StepUp/actions/runs/36108963617) and [screen gallery 36108963541](https://github.com/mycyi1994-hash/StepUp/actions/runs/36108963541). The first failed gallery was a duplicate text selector in the test; the subsequent and final Android 14/15 runs passed. See [stage 8 report](CHARACTER-FREE-STAGE8-2026-09-25-KO.md) and [final screenshot review](CLAUDE-RECORDS-FINAL-REVIEW-2026-09-25.md). No public APK release or main merge. Real GPS, tiles, external wallet, other states and full-app readiness remain unverified.

## 2026-09-25 — image and sound integration, source applied

- Applied all 29 newly produced WAVs to Android resources. Eighteen short cues now follow their corresponding app events; eight draw transaction cues are also copied to the web DApp and gated on real receipt/result or failure; three scene ambience loops are opt-in and stop when foreground/audio policy does not allow playback.
- Audited 89 prepared illustration files: 67 are directly used, seven are conditionally displayed, and 15 are exploratory/reference-only. The six audio preview mixes are also reference-only. See the [per-file application report](MEDIA-APPLICATION-2026-09-25.csv) and [integration notes](MEDIA-INTEGRATION-2026-09-25-KO.md).
- Web draw bundle, source/resource checks and the per-file audit pass. Android compilation and device audio/visual verification on this exact revision are still pending; the locally available machine has no Android SDK/JDK/adb. Live draw sounds also depend on DApp deployment and contract configuration.

## 2026-09-25 — writing and profile simplification, source only

- Mapped every non-equipment asset used by writing, My Info, settings, profile edit and goal dialogs in [the focused asset map](COMPOSE-PROFILE-ASSETS-2026-09-25-KO.md). Existing independent scenery, bench and exact-match seated character art are reused; controls and icons remain native Compose components. No new bitmap was needed for these screens.
- Writing now opens as the simpler free-post form; choosing Flash reveals its meeting fields. The nested form cards were removed and the route uses the shared calm utility backdrop. The existing Flash form interaction test now selects Flash before checking its numeric fields.
- My Info keeps its separate scenery/character and now presents unboxed totals and light shared destination rows. Settings uses the same row component in four groups over a calm utility backdrop; all existing destinations remain reachable.
- Source design contract, four-language resource check, experience-asset check and whitespace check passed. Local Android SDK/JDK/adb are unavailable, so this revision has not been compiled or captured on a device. Do not mark these screens visually approved or the full redesign complete.

## 2026-09-25 — product art and base-look run frames wired for APK validation

- Replaced the 52 sneaker and 10 premium outfit runtime WebP cuts with 640px transparent versions of the prepared product candidates. The five existing high-resolution base/trial outfit cuts remain in place. Existing catalog IDs and UI mapping are unchanged.
- Added separate 640×960 RUNO/LUMI running frames and a two-frame swap on the live Run screen. It plays only while running, only for the base outfit with base shoes, and stops on pause/background or reduced motion. Other equipped looks keep their existing art and mismatch notice; these frames do not solve arbitrary modular gear combinations.
- Source/design checks and alpha/dimension checks passed. Commit `c129bef` passed the Build APK workflow (unit tests, signed debug APK, R8 release compile) and Android 14/15 wardrobe gallery workflows. Both device captures show the new sneaker and outfit cuts on their cards. The debug APK is 92,410,523 bytes (SHA-256 `c83b0754b77582dd23727efa054df5f869a9337bd4b81878590f72bc41923bae`) at `C:/Users/gana0/StepUp-apk/redesign-c129bef/app-debug.apk`.
- Motion remains a limited pilot: the actual starter WND-010 shoe is equipped by default, so that look continues to show its accurate static figure. The alternating frames apply only with no NFT shoe equipped, and the running screen itself was not in the wardrobe capture suite. Do not report default-look running motion or all-gear modular animation as visually verified.

## 2026-09-24 — full product-art candidate set and running-motion feasibility

- Collected 52 high-resolution transparent sneaker candidates: 32 earlier unintegrated pilots plus 20 newly generated from each catalog ID's small source art. Generated 10 high-resolution outfit cuts; the 5 existing base/trial cuts remain reuse candidates. `tools/verify_product_catalog.py` passed for all 67 catalog IDs and checks size, alpha corners, presence and duplicate files. See `PRODUCT-ASSET-PRODUCTION-2026-09-24-KO.md` and `design/redesign-2026-09/assets/modular/product-catalog-review.html`.
- Generated three new base-look running frame candidates (LUMI A/B and RUNO B), paired with the existing RUNO A in `running-preview.html`. Real frame animation is feasible in Compose, but the current app only bobs a flattened RUNO pose; candidate frame alignment, glow edges and equipped-gear fidelity have not passed. Do not register these as production animation yet.
- No runtime product resource was replaced, no animation was wired into the app, and no APK was built for this asset-only work. The previous preview APK remains the last app build.

## 2026-09-24 — screen design QA first pass, source only (no APK)

- Began [the Korean first-pass design QA](DESIGN-QA-FIRST-PASS-2026-09-24-KO.md) for screen/UI work, leaving character and equipment expansion deferred as the user requested. Opened eight core mockups and compared their composition and states with current source; all 86 backlog IDs now resolve to actual current mockup paths in the generated screen-coverage CSV.
- Home, wardrobe, community and live-map architecture match the intended separation of art and native controls at source level. Found two material draft/source differences to resolve during visual review: run-result content order and login hero illustration. Seven active scene images are about 853×1844 px and need native quality/crop review.
- Design contract (33 routes), string resources, experience assets and inventory generation passed. No local Android SDK/JDK or same-revision native captures; all native screen-review statuses remain pending. No APK/AAB, CI, push, or image generation.

## 2026-09-24 — remaining design assets inventoried (no APK)

- [Full Korean asset checklist](ASSET-REQUIREMENTS-2026-09-24-KO.md) now expands the current catalog into 556 pose-correct modular character packages and 62 high-resolution product redraws. These are logical work units, not 618 guaranteed single-file PNGs or completed assets.
- Separate CSVs enumerate all 556 packages, 72 outfit-to-cap bindings, 67 product candidates, 9 scene/bench reviews, 86 screen/state mappings, 90 current mockups plus 13 superseded drafts and a component board, and all 229 packaged visual/font files. The source-to-resource check found 208 UI drawable names with no missing reference. Five product candidates and the active backgrounds/bench still require visual review.
- This corrects the earlier 8/464 estimates, which omitted base/demo outfits, base shoes, caps, and full pose coverage. No images or APK were generated by this inventory step.

## 2026-09-24 — shared component finish, source only (no APK)

- Refined common hero surfaces, action gradients, outline controls, segmented labels, shortcuts, status/count badges and numeric fitting. See `SHARED-COMPONENT-FINISH-2026-09-24-KO.md`. This is shared-source refinement, not additional completed pages.
- Hero text now uses a theme-paired surface; artwork scales with available width. Controls and badges use an opaque base independent of replaceable scenery. Button fills are separate from bright decorative highlights; long labels and badge counts have room to grow. Existing header/logo policy and real callbacks/data are preserved.
- AdaptiveNumber measures candidate sizes instead of assuming linear font scaling. Exploration is bounded and remembered. Native large-font review remains pending.
- Added source-reference notes for 28 screen functions without changing their existing implementation/verification records. Design contract, strings/resources, assets, whitespace and syntax parsing of 62 changed Kotlin files passed. Computed contrast for the new shared button fill is at least 4.65:1 including peak sheen; this is limited color-pair evidence, not whole-app accessibility verification.
- Android compilation and native visual/interaction review remain pending. Existing equipment/compositing and functional release gaps remain open. No APK/AAB, CI build, release, push or image generation.

## 2026-09-24 — home/catalog/news finish, source only (no APK)

- Applied the remaining 18 source-finish backlog entries for home/details, three launch states, wardrobe/options, inventory/filter/trades, collection, runner market and race/health news. See `HOME-CATALOG-NEWS-FINISH-2026-09-24-KO.md`; the table covers UI source application, not verified visual completion or release readiness.
- Home retains a large equipped character and pinned start action; constrained hero space scrolls. Details use complete numbers and separate units. Launch content scrolls within safe areas and keeps retry outside the body, retaining actual preparation/reveal behavior.
- Wardrobe preserves the reference composition and independent background; compact/large-font screens give more room to controls. Worn/trial labels sit below art. Inventory/collection show full names and readable metadata, with fewer columns at larger text sizes. Boost actions are separate full-width controls.
- Trading rows separate real amounts, metadata and actions; absent listing prices remain unknown. Shared filters and sorting reuse `DialogPanel`. News uses common form fields, 48dp native save controls, wrapping tags and full-width external-link/retry actions. Real ownership, listing/demo state, feed content, callbacks and filter draft/apply semantics are preserved.
- Checks passed: design contract, strings/resources, existing assets, whitespace and syntax parsing of 61 changed Kotlin files. Android compilation and native visual/interaction checks remain pending. No APK/AAB, CI build, release, push or image generation.
- Every backlog row now has a UI source-application record. Full equipment/pose asset coverage, body-part compositing, combined native review and broader functional release work remain open.

## 2026-09-24 — running/map/course finish, source only (no APK)

- Refined 12 existing backlog entries across run ready/active/result, course selection/create/board, nearby map and five territory states. See `RUN-MAP-COURSE-FINISH-2026-09-24-KO.md` for exact mapping; these are source changes, not new pages or native verification.
- Run content now scrolls independently of the primary controls. Shared `AdaptiveNumber` keeps complete timers/amounts within the available width; larger text switches live metrics to a vertical layout. Results separate real reward state, amount, units and readable record values.
- Reused `DialogPanel` in five additional places: stop confirmation, run goal, course save, apply/clear course and course ranking. Native form fields, 48dp course choice/like controls and separate course actions replace compact inline controls.
- Actual map tiles, coordinates, pin/territory hit testing and viewport logic are unchanged. Status/retry and bounded scrollable details sit below the map; selected course/live-path actions sit outside route previews. Added two course-like labels in four locales.
- Design contract, strings/resources, existing assets, whitespace and syntax parsing of 50 changed Kotlin files passed. No Android compilation, same-source native capture or interaction verification. No APK/AAB, CI, release, push or image generation.

## 2026-09-24 — detail/dialog finish, source only (no APK)

- Refined 9 existing backlog entries covering profile edit/goal, three challenge states, store, sneaker detail/upgrade, and market model detail. See `DETAIL-DIALOG-FINISH-2026-09-24-KO.md`; these are source refinements, not 9 new pages or device-verified completions.
- Added `DialogPanel` with shared shape/surface, safe-area/IME handling, fixed header/close and actions, and scrollable content. Eight popup call sites reuse it, including mint result, copies, bid/ask and invite editors. Existing callbacks, limits, equipment/price/reward truth and action eligibility are preserved.
- Profile and trading inputs reuse `FormField`; avatar choices retain their IDs with adaptive 3/4-column layout. Challenge rewards move below the description; store rows separate description and price/status; market rows have independent full-width actions and readable history/empty states.
- Inventory detection now includes `DialogPanel`: 33 routes, 34 screens, 20 overlay declarations. Counts are not completion evidence.
- Design contract, strings/resources, existing assets, whitespace, and syntax parsing of 46 changed Kotlin files passed. No local Android compile/runtime evidence; keyboard, font, theme and interaction verification remain pending. No APK/AAB, CI, release, push or new image generation.

## 2026-09-24 — seated profile composition, source only (no APK)

- Added LUMI/RUNO seated portraits for the exact STUDIO-PINK + WND-010 look, plus an independent transparent bench. `ProfileCharacterStage` scales both layers within one contact-point frame. Scenery, contact shadow, bench, portrait and native UI remain independent; body/clothes/shoes inside the portrait are still a complete image.
- Profile now uses a larger artwork area and its own saved/random initial night/dawn scene with a native background-change button. Names, statistics, account data and settings callbacks remain native and unchanged. Unsupported seated combinations fall back to the prior standing presentation, with an equipment-fidelity note when the existing art cannot depict the selected combination.
- `AvatarArtCatalog` restricts seated assets to the matching gender/outfit/shoe and SIT request. Other poses cannot select them. Added two focused unit tests for these boundaries (not executed locally).
- Browser composition review checked the alpha edges and bench contact on night, dawn and white backgrounds. This is asset-composition evidence, not an Android screen capture. See `design/redesign-2026-09/assets/profile/README-KO.md` and its review HTML.
- Design contract (33 routes), strings, existing experience assets, whitespace and Kotlin syntax parsing (39 changed files including the tests) passed. Android compilation/runtime validation remains pending. No APK/AAB, CI workflow, release or push was performed.

## 2026-09-24 — stages 7–9 design finish, source only (no APK)

- User explicitly requested finishing the remaining design and withholding APK generation until a combined review. No build workflow, APK/AAB, release or push was performed.
- Applied source refinements across the 46 backlog states in stages 7–9 (23 community, 9 records/wallet, 14 account/settings/onboarding). See `DESIGN-FINISH-2026-09-24-KO.md` for the exact per-state mapping; shared refinements are identified as such.
- Added shared preference toggle/choice rows, notes, state panels, native form fields and record metrics. Unified segmented controls, complete labels and touch heights. Utility scenery now follows theme tokens.
- Community: readable post/comment layouts and actions, separate full-width join action, native compose inputs, scrollable report/roster dialogs, authentic empty/error/sign-in states, fixed lobby readiness CTA. Removed invented route art from the meetup hero; real maps are unchanged.
- Rankings now show each participant once in legible rows; achievements use horizontal criteria/progress rows. Analytics gets larger charts and an accessible week-selection lane. Wallet/notification states and profile settings use the same presentation system. Four-step onboarding keeps description and controls in one constrained panel and honors reduced motion.
- Checks: design contract, strings/resources, whitespace and syntax parsing of 33 changed Kotlin files passed. Syntax parsing is not Android compilation. No local Android toolchain or same-source native captures; runtime visual/IME/theme/font validation remains pending. Existing seated-character and compositing art gaps remain open.


## 2026-09-24 — stage 5–6 screen application

- Stage 5: the profile/goal dialogs and three challenge states retain their shared scenery, controls, and actual equipped avatar. News now gives the race/health lists and search priority over a large decorative hero. Feed data, links, and save actions are unchanged.
- Stage 6: the wardrobe, vault, store, collection, sneaker detail, and model detail retain separate scenery and product art. Runner Market now shows full-width image-first product rows on phones and a simpler outfit/shoe switch. Actual listed shoes remain distinct from unavailable outfits. My Trades has distinct empty cards for listings, bids, and history; the collection uses two columns at larger font sizes; the upgrade confirmation uses actual sneaker art and the shared action button.
- This records source application, not one-by-one device review. The seated character/bench and body-part compositing pilots are not production-compatible with every equipped look and remain out of the app. Stages 7–9 and final visual comparison remain outstanding.
- App source `53fc4b8` passed Build APK run `35985313612` (strings, design contract, unit tests, debug APK, stable signature, unsigned release/R8). Published prerelease `redesign-preview-53fc4b8` with `app-debug.apk` (72,618,136 bytes; SHA-256 `2f02e965e6c71d685a67e9a5eedc5eed9982c804328c76cc14d28bc9719e177a`). No same-revision device capture was run for this checkpoint, following the user's request to stop lengthy review and apply the design first.

## Active objective

Complete the whole StepUp application and all its screens/states for GASOK submission and real user testing. Full objective remains active until audited against implementation, build, APK, captures and functional evidence.

## 2026-09-24 — native receipt evidence and real save-error retry test

- Previous turn progressed with 6497786 (save progress/failure handling and presentation checks), kept local while 9cd6746 Build APK 35934709879 remained live. Revalidated the handle; release/R8 was still running, not assumed stalled.
- Downloaded 9cd6746 API 34 interaction XML: 29 tests, one failure. All three checkpoint tests, both atomic settlement/reopen tests, course outbox, energy-purchase and four existing migration tests passed. Chrome failed at challenge entry with 'No compose hierarchies found'; this run completed its other tests and is not an emulator-loss failure. Root cause remains to investigate.
- Both API 34/35 permission jobs passed, including denied baseline, actual Settings launch, coarse grants/Back and precise grant/Back. Inspected API 35 approximate-location image: status is correct, but the isolated screen fixture omits the production canvas/inset shell, leaving a white background/header contrast issue. Changed this test to render the actual MainScaffold privacy route; the new shell-inclusive captures must be inspected separately.
- Added schema-v13 fixture directly from the committed generated export and a native v13→14 test preserving signed claim metadata, account owner, local balance and undelivered energy purchases without fabricating historical settlement receipts. Execution pending.
- Added RunSaveRecoveryTest: injects a Room receipt failure while ending a synthetic run through the real screen/service, checks retained FAILED state, removes the fault and uses Retry saving to reach a saved result with one session row. It runs in the required interaction suite. This is written, not executed; it does not validate GPS, server SUP or process death.

## 2026-09-24 — visible save progress and safe same-process retry

- Previous turn made progress: 9cd6746 connected atomic local settlement, energy receipts and persistence tests. Revalidated its live Build APK 35934709879/gallery 35934709852; neither was assumed finished or restarted.
- Stopping now freezes current state before cancellation/settlement and exposes SAVING. An exception retains the same run as paused/FAILED, resets the in-flight guard and exposes Retry saving. Resume is refused once saving starts; the UI disables the primary action while saving and hides the duplicate Finish action. Four-locale failure copy explicitly asks to keep the app open because durable active-run restoration is not enabled yet.
- Same-process retries do not repeat attempted legacy course/faction follow-ups after scheduling failure. Their full durable recovery/acknowledgement remains incomplete; this is not a replacement for it. Checkpoint construction rejects a non-idle save state mislabeled RECORDING.
- Added saving/failed fixtures, captures and control assertions to all Chrome viewport/theme/font scenarios. These are presentation checks; repository failure/reopen tests and future service-level process-death tests establish different evidence. Native execution is pending.
- Source inventory/design/resources and whitespace checks pass. Current changes are not a completed recovery feature or final release.

## 2026-09-24 — atomic local run settlement and replayable energy consumption

- Previous turn made progress with b2560df (checkpoint storage/tests and guide selector fix), held locally while build 35933717767 was live. Rechecked that build and observed successful completion before pushing the next revision.
- Room 14 adds run_settlements with a composite captured-owner/start identity; existing tables/data remain intact. RunSettlementRepository commits the activity, local ledger, notification and receipt in one transaction. The service uses this path; reward calculation itself no longer writes credits. A repeated identity returns its saved outcome. These are local receipts, not fabricated server acknowledgement.
- DataStore now applies a run energy debit and receipt marker together; Room acknowledgement can fail/retry without a second debit. Previous-day energy cannot consume a later-day refill. Pending receipts reconcile before another run calculation/background settlement. Added native trigger-induced rollback, eight concurrent finish requests, acknowledgement failure, database/preferences reopening and delayed old-day coverage. Compilation/execution/exported schema remain pending.
- Active checkpoint restoration is still not enabled. Account partitioning, accounted-step baseline atomicity, course/faction/party follow-ups, service error/retry UI and actual process-death behavior remain incomplete. RUN-RECOVERY.md distinguishes the newly connected core settlement from these remaining requirements.
- Downloaded 12b8840 API 34 permission XML: all three denied status texts were siblings in the accessibility tree, so the test could not uniquely associate a label. Permission rows now merge their own label/status semantics; the test asserts both on the same node. No claim yet that the settings-return cases passed. That revision's large-font suites passed; other failed suites require their own evidence, not inference from this test.
- Source inventory/design/resources/assets/shell/whitespace checks pass. Whole-app completion and final APK/gallery remain unproven.

## 2026-09-24 — interrupted-run persistence foundation and precise guide assertion

- Previous goal turn made progress: committed 12b8840 added real permission/settings-return verification. Revalidated its live Build APK 35933717767 and gallery 35933717791; no restart or duplicate workflow dispatched.
- Traced service shutdown: ledger/notification/energy settlement precedes the separate walk-session insertion. Automatic restart/replay would therefore risk duplicate mutations. Added RUN-RECOVERY.md with exact integration prerequisites and remaining boundaries.
- Implemented RunCheckpointStore with AtomicFile/versioned data, captured owner, exact route timestamps, steps/duration, laps, goal and integrity metrics. Interrupted recordings recover as paused without invented downtime; interrupted settlement is a distinct non-resumable phase. A different run/account cannot replace unresolved data, stale snapshots fail, and corrupt/unsupported files are preserved. Added three native reopen/interrupted-write/settlement-boundary tests to the required interaction suite. This foundation is deliberately not connected to production service until durable idempotent settlement is implemented; active-run recovery remains incomplete.
- e501763 API 34 interaction passed. Downloaded API 34 gallery failure: guide title '러닝 시작' matches both the overlay title and underlying run button. Added a specific title tag and retained exact expected text/display assertions, fixing selector ambiguity rather than weakening the check. Full four-step captures still need rerun and manual inspection. API 35 failures remain separately unverified.
- Local inventory, design-contract, four-language resources, asset integrity, shell syntax and whitespace checks pass. New checkpoint tests/compilation and guide captures await CI; no whole-app completion claim.

## 2026-09-24 — actual permission-state and settings-return verification

- Revalidated the clean e501763 worktree and live build/gallery jobs. Previous implementation work produced the shorter guide and durable outbox; this continuation adds executable evidence for a remaining user-visible state rather than treating the existing source check as completion.
- Inspected e537c44 API 35 interaction XML: only the first Chrome test is reported, with an empty failure after device loss. Failed gallery logs likewise show the emulator disappearing. Host diagnostics do not establish an out-of-memory cause. Neither job is a navigation/gallery pass, and this evidence does not identify a new product assertion failure. e501763 execution remains independent.
- Added a dedicated API 34/35 permissions suite. It installs the app and revokes activity, location and notification permissions before instrumentation starts, avoiding process termination during tests. PrivacyPermissionTest requires that denied baseline, checks the production screen, opens actual Android app settings, grants coarse location/activity/notifications, returns via system Back, then repeats for precise location. It checks refreshed status text and saves three actual display captures. The system grants are test automation; this does not validate the runtime permission-prompt UX or notification delivery.
- Source design/resource and shell syntax checks pass. New Kotlin/native test execution and permission capture inspection remain pending; no permission verification success is claimed yet. Updated the ownership audit to separate its original findings from the now-executed migration/owner tests.
- e501763 Build APK 35933027929 completed successfully before the next push. Both API large-font jobs passed; remaining native evidence is not yet complete. Inspected e537c44 API 34 privacy capture: the precise-location status and app-settings action fit the fixture; denied/return states require the new suite.

## 2026-09-24 — short first-run flow and durable course outbox

- First-use guide reduced from 11 steps to four everyday destinations: start running, customize, community and profile. Removed the payout-journey UI and its unavailable withdrawal/minimum claim. Updated four-language active copy to match current controls. Completing/skipping returns to Run; the gallery now checks completion reaches the home run action. New four-step captures and return behavior await this revision's execution.
- Visual inspection of 9be0c87 guide-00 still showed the preceding home frame despite semantic availability; later steps were visible. Added a guide-title assertion and an extra Compose frame before each capture. Manual inspection remains required; the earlier successful gallery job alone does not establish image correctness.
- Pending course records previously truncated to the newest 20 before acknowledgement. Removed that truncation: this is an upload outbox, not a recent-history list. Added a native 30-record persistence test covering reopen, repeated non-consuming reads, acknowledgement and a second reopen. Added the test to the required interaction suite; execution pending.
- e537c44 Build APK 35932263780 passed. In run 35932263677, API 34 interaction, gallery and large-font jobs passed; API 35 large-font passed with the strict complete-footer-bounds assertion. Inspected its actual 1.6-font capture: both footer buttons now fully clear system navigation. API 35 interaction/gallery were still running at this checkpoint. This confirms the specific filter regression, not all dialog/IME states.
- Local source/resource/shell checks pass. Account partitioning, active-run recovery, external integrations, all-state design review and final deliverables remain in scope and incomplete.

## 2026-09-24 — evidence-led settings fixes and Android 15 dialog dependency

- Inspected eight native captures and recorded bounded findings/remaining states in VISUAL-REVIEW.md rather than overwriting generated inventory statuses. Wallet amount/unit baseline and faded hero labels corrected. Privacy no longer always paints granted checkmarks: it reads activity, precise/approximate location and app notification access, refreshes on resume, and opens real app settings. Four-language language-setting copy no longer promises automatic translation of every user post.
- 9be0c87 Build APK 35931315935 passed. Its API 35 large-font test correctly fails the new complete-footer-bounds assertion (Reset bottom=1882px); the captured footer is still under navigation. No native success claim for the previous inset-only fix.
- Compared Google's published UI 1.7.6 and 1.8.2 source jars: Dialog's OnApplyWindowInsetsListener/fullscreen measurement handling is absent in the former and present in the latter. Updated BOM 2024.12.01 → 2025.06.00 (UI/Foundation/Runtime 1.8.2, Material3 1.3.2) for that specific Android 15 defect. Links/evidence in VISUAL-REVIEW.md. All build/native suites must run again; no local SDK is available.
- Navigation failure now reports a 40px text layout with a 148px paragraph. The label fills its allocated tab width, keeping shared measurement/centering and the strict overflow test. Both navigation and Dialog fixes still await new runtime evidence.
- Source contract, inventory, four-language resources and asset checks pass for these edits. Privacy permission denial/settings-return and full account partitioning remain outstanding; no completion conclusion.

## 2026-09-24 — Room 13 build and native preservation evidence

- c01b8e4 Build APK 35930751043 completed successfully, including all unit tests, debug signature verification and release/R8 compilation. Retrieved its generated Room 13 schema: the only changed table is walk_sessions, with the recordingOwner TEXT NOT NULL DEFAULT 'legacy' field added.
- API 35 interaction artifact from 35930751025 completed 23 tests with one failure: the previously identified Chrome tab-text assertion. All four DatabaseMigrationTest cases passed, including v12 pending-run preservation without assigning an account, per-account queues after database reopening, v6 preservation and unsupported-version data protection. This is native emulator evidence for those cases only; active-service recovery and local per-account reward/equipment partitioning remain incomplete.
- Filter inset ownership, explicit navigation label style and guide-clock changes are pushed in 9be0c87; their new build/gallery execution is in progress. Do not equate earlier capture success or click success with verification of these fixes.

## 2026-09-24 — complete native runs expose actionable failures

- Downloaded 5c4fe1a API 34 gallery and API 35 interaction evidence from 35929984701. API 35 completed 21 tests: 20 passed (including both separate CrewForm IME cases); Chrome failed its new text-overflow assertion for the first Run tab at 360dp/1.0. This run did not lose its emulator.
- Navigation now measures and renders the same explicit shared bodySmall-based label style instead of independently inheriting text defaults. The strict overflow assertion stays enabled and now reports text, paragraph dimensions and constraints if it fails. Runtime confirmation is pending; no claim yet that all viewport scenarios pass.
- API 34 gallery reached the final guide capture and failed because the guide had not started. The guide-00 image is the ordinary home screen. Its delayed start runs on Compose's virtual clock, whereas the fixture had slept in wall-clock time. Advance the test clock before asserting/capturing guide controls; production timing is unchanged.
- At c01b8e4, Build APK 35930751043 unit tests (including new ownership scenarios) and debug APK steps passed; release R8 step was still running. Native ownership/migration scenarios and generated Room 13 schema remain to be inspected. The local filter inset fix awaits the next push after that build is terminal.

## 2026-09-24 — visual evidence overrides partial-visibility test success

- Inspected the 5c4fe1a API 34/35 1.6-font filter captures. API 34 shows both actions above navigation; API 35 visibly puts the action row under the gesture area/partly offscreen. The prior passing click test is not a visual pass.
- Shared SheetFrame now explicitly owns insets with `decorFitsSystemWindows = false`, retaining its navigation-bar padding. Added a check of the complete action bounds against the system navigation inset. Fix and stronger assertion await native execution; do not claim the overlap resolved before the new capture.

## 2026-09-24 — recorded-account upload guard and independent font evidence

- Implemented the first account-ownership boundary: Room 12→13 preserves unknown historical owners, service captures new-run ownership, upload queries select that account, and run/crew/course requests validate the exact outgoing token identity. Course entries are acknowledged only after server success. Full account isolation remains incomplete; see ACCOUNT-DATA-AUDIT.md for limitations and required follow-up.
- Added six unit scenarios and native migration/reopening queue checks. Local design/resource/asset checks pass. Kotlin compilation and new test execution are pending the next CI revision; generated Room 13 schema must be retrieved from that build.
- Build APK 35929984626 at 5c4fe1a succeeded. Independently isolated large-font jobs in gallery run 35929984701 passed on both API 34 and 35 (real system font scale 1.6, filter selection/apply/reset). Artifacts downloaded to `C:/Users/gana0/StepUp-captures/redesign-5c4fe1a`. Interaction jobs failed; full gallery results still require inspection. These outcomes do not validate the new ownership changes.
- Earlier 3961d68 gallery run 35928586797 again lost its emulator during the first Chrome test, leaving empty failure details/unfinished 21-test suites. No claim that the form/gallery tests executed there.

## 2026-09-24 — account ownership audit traced end to end

- Traced saved runs through ClaimRepository, ServerSessionRecorder, StepUpServer and SQL auth.uid(): no recorded owner is checked before the current account's token is used. SessionHolder replacing a login does not partition local records. This verifies an attribution risk in the code path, not an observed real-user incident.
- Added ACCOUNT-DATA-AUDIT.md with concrete persistence, migration, request-token binding, related stores and required concurrency/reopen cases. Legacy ownership must not be inferred from the present login; preserving data while preventing ambiguous uploads is required. No live account, server financial mutation or destructive migration was performed.
- This closes the investigation gap and establishes a substantive remaining implementation requirement; it is not a fix or account-isolation completion. Current UI build/gallery jobs remain live and independent work continues.

## 2026-09-24 — isolate native verification suites from emulator loss

- Both 580a9e4 API jobs lost their emulator in the first interaction phase, preventing any subsequent gallery/large-font run. The workflow now uses API 34/35 × interaction/gallery/large-font jobs, each with its own emulator. Every existing interaction class and both other suites remain required; fail-fast remains false and failures are not suppressed or retried.
- Runner accepts an explicit suite, validates it before contacting a device, and preserves its existing all-suites default for manual use. Each job uploads an artifact with API and suite in the name. Large-font restores the prior system value as before. This isolates failure impact; it does not claim to cure emulator disappearance or prove device behavior.
- Shell syntax passes; invalid-suite invocation fails before device work. Whitespace/source/resource checks pass. New matrix execution is pending; latest f8d93f7 build/gallery remain live, so no in-flight job was cancelled.

## 2026-09-24 — readable filter summaries and reachable reset

- Shared filter summaries now separate the condition text from reset actions. The previous 12sp clickable labels with 4dp vertical padding are replaced by shared GhostButton controls; condition text uses the central 14sp secondary size and wraps instead of truncating. Up to three named conditions and the existing remaining-count summary are preserved.
- This affects all consumers of FilterSummaryRow consistently, with existing callbacks unchanged. No reset action is added when no condition/extra action exists. Enlarged text layout and native list scrolling still need runtime verification.
- Local inventory/design/resource/asset/whitespace checks pass. 3961d68 Build APK 35928586875 completed successfully; its gallery 35928586797 remains live. Queued unknown-balance, full navigation-label and home-readiness changes can now be pushed without cancelling that build. This build result does not validate those newer changes.

## 2026-09-24 — partial captures survive another emulator loss

- Downloaded and inspected 580a9e4 gallery 35927968501. Both API jobs lost their emulator during ChromeNavigationTest before gallery execution; locale-dialog fix remains unverified. Host kernel logs contain no matched OOM/segfault evidence; no new root cause is claimed. Artifacts are under C:/Users/gana0/StepUp-captures/redesign-580a9e4.
- Background transport preserved several chrome PNGs before device loss. Viewed API 34 partial home PNG: shared chrome/start button are present, but the character is still loading. This is valid loading-state evidence, not a completed-home capture.
- Added a readiness tag only on the persisted-look character stage and a bounded displayed check before normal-home geometry/captures. Test failure remains visible if the character never arrives; no arbitrary sleep, default avatar or data substitution is used. Static checks pass; native execution remains pending.

## 2026-09-24 — complete bottom-tab labels at enlarged text

- Replaced the root navigation's single-line ellipsis rule. The shared bar measures all four localized labels with their actual style, available tab width and system density/font scale, then gives every label the same maximum measured height. Labels wrap while icon, text-top and indicator alignment remain shared; selected state still changes no typography or geometry.
- Added native text-layout overflow assertions for every tab in all existing width/font/theme chrome scenarios, alongside unchanged navigation bounds and baseline checks. This verifies full labels when executed, rather than considering an ellipsis a successful fit.
- Source design contract and whitespace checks pass. Native compilation, measured geometry and actual enlarged-label captures remain pending. Current build/gallery handles were confirmed live; no completion claim or restart was made.

## 2026-09-24 — remaining reward-facing balance initialization

- Walk, customization, challenge and news view models now preserve unknown balance until the repository first emits, matching the inventory/home approach. Run completion renders an em dash with the SUP unit instead of an invented zero while loading. Reward eligibility and credit processing are unchanged.
- Shared SecondaryHeader now keeps its SUP control present whenever it has a wallet destination, even before a balance arrives. Detail headers with neither balance nor wallet destination retain their existing spacer. This prevents loading state from temporarily removing the wallet control and changing the header composition.
- Local inventory/design/resource/asset/whitespace checks pass. Nullable call sites were inspected; native compilation/cold-start captures remain required. 3961d68 build 35928586875 and gallery 35928586797 are running; the 580a9e4 gallery was also live when last checked. No whole-app balance/error-state completion claim is made.

## 2026-09-24 — actual system font coverage for filter dialogs

- Capture runner now runs the production filter interaction test again after setting Android system font scale to 1.6. This reaches the Dialog window itself; the test verifies Activity configuration matches the system setting and names its capture with the observed scale. Applying/resetting/dismissing uses unchanged assertions.
- Standard gallery XML is preserved before the additional instrumentation run, and enlarged-font XML/screenshots have separate artifact folders. Failures still fail the overall run. Original system font is restored by exit cleanup; an unreadable original value fails the phase instead of inventing a default.
- Runner shell syntax, source design contract and whitespace checks pass. Both font runs still require execution; no 1.6x visual approval or whole-app font-coverage claim is made. 580a9e4 build remains live, so changes remain local pending that build's terminal result.

## 2026-09-24 — inventory filter commit/cancel interaction coverage

- Added native coverage using the production ItemFilterSheet: choose a faction then close without applying; reopen and confirm the draft was discarded; apply while preserving other conditions; reopen and reset, verifying the reset remains a draft until Apply. Final assertion checks faction/rarity cleared and equipment/sort restored together.
- The test checks visible footer Apply, actual selected semantics, callback count and committed state, and captures the reset panel. Added it to both API interaction suites. This is default-device-font coverage; a Compose-only density override outside a Dialog does not prove enlarged dialog layout, so real system-font coverage remains outstanding.
- Local source-contract/resource/asset/whitespace and runner syntax checks pass. Test compilation/execution is pending; 580a9e4 build remains live, so these changes remain queued without cancelling that build.

## 2026-09-24 — shared filters adopt fixed control tokens

- Shared filter/sort toolbar and choice cells previously used independent 12/13sp labels, 14/16dp corners and unconstrained touch heights. They now use central secondary label/padding, control-radius and minimum-touch tokens; toolbar icons use the central icon size. Both expose button semantics.
- Labels wrap completely instead of being ellipsized. Choice grids reduce to two columns for enlarged text and one at 1.5x or above; selected choice weight stays constant so selection does not change geometry. This applies through the existing shared component across inventory, events, news and sorting.
- Extended source-contract checks to reject missing control tokens or restored truncation in these two components. Local inventory/design/resource/asset/whitespace checks pass; native compact/large-font layout and touch behavior still require captures. Latest 580a9e4 build/gallery remain live; changes are queued locally to preserve the active release build.

## 2026-09-24 — isolate font scenarios from focused editor disposal

- 2f65026 API 35 log at 22:16:10–11 records overlapping show/hide IME requests, including onShown followed by HIDE_SOFT_INPUT_ON_ANIMATION_STATE_CHANGED. The captured 1.3 form has no keyboard. The test replaced a focused form with key(scale) inside one Activity immediately after the first scenario, making disposal/next-focus interference a plausible fixture cause, not a proven production defect.
- Split normal and enlarged-font scenarios into two rule-owned Activity tests. Both retain the actual IME visibility requirement, entered text, pinned submit checks, empty-name disabling and pre-teardown screenshots; no keyboard forcing or relaxed assertion was added. Native execution remains required to confirm the diagnosis.
- The completed 2f65026 API 34 artifact reports 19 interaction tests/0 failures, with the separate gallery still failing on upgrade-cost locale matching. Its download has completed. 76c942d gallery 35927305914 also ended in failure; the newer 580a9e4 locale-fix build/gallery are still live. Local design/resource/whitespace checks pass.

## 2026-09-24 — upgrade-dialog failure traced to gallery locale scope

- Inspected 2f65026 API 35 extra-sneaker-upgrade-confirm.png: the actual dialog is displayed, with English Enhance/Upgrade/Cancel while the underlying gallery is Korean. The test searched Korean cost text. Thus increasing the wait cannot resolve this mismatch. Native production locale behavior is not disproved by this test-only configuration override.
- Gallery now applies the real app locale before creating its Activity and restores the prior selection after teardown. This gives newly created dialog windows the same locale as the activity, retaining Korean assertions and the pre-teardown failure capture. Existing viewport overrides remain for layout coverage. Fresh native verification is required.
- API 35 XML reports 19 interaction tests/1 failure (CrewFormTest IME wait) and gallery timeout at the upgrade-cost wait. Viewed 1.3-keyboard-wait.png: form and pinned submit are visible, but the keyboard is absent. That separate issue remains unresolved. Artifact download session 87227 is still active; API 35 files are available, API 34 final report inspection pending.

## 2026-09-24 — inventory focuses on shoes before collection statistics

- Based on native screen-03 review, removed the four tall faction-progress cards from the default inventory viewport and integrated their choices/counts into the existing filter sheet. The equipped shoe and owned collection remain primary; faction counts and all previous AND filters remain available.
- The sheet drafts faction alongside rarity/equipment/sort; Apply commits the selection and dismissal leaves the prior conditions. Active faction appears in the visible summary and filter count, and summary reset clears all active conditions. Shared header, tabs, buttons and item ownership are unchanged. Four locales name the new section.
- Local inventory/design/resource/asset/whitespace checks pass; native sheet scrolling, apply/dismiss/reset and large-font screenshots remain pending. Build 35927305897 is still running, so this change remains queued locally. Prior gallery 35926758041 has completed with failure; artifact download started for inspection, with no root-cause conclusion yet.

## 2026-09-24 — ranking authentication recovery uses shared controls

- Viewed native 0383b12 screen-17.png: a sign-in-required explanation offered only Retry, which repeated the authenticated ranking read without opening login. Personal and faction ranking failures now use the existing shared SignInAgainButton for that specific problem; offline/rejected requests retain refresh.
- Replaced the screen-local 12sp clickable Retry text with shared GhostButton, inheriting the central typography and touch target. Loading and empty states keep no fabricated rank or retry action. Real Google login and returning to a previous ranking selection remain unverified.
- Viewed screen-03.png: equipped shoe leads, but faction progress and filters still occupy much of the first viewport; further simplification remains needed. No whole-screen approval is recorded. Local inventory/design/resource/asset/whitespace checks pass; fresh native validation remains required.
- 35926758041 and the new 76c942d build/gallery (35927305897/35927305914) remain running. No restart or speculative failure diagnosis was performed.

## 2026-09-24 — unknown inventory balance is not zero

- ItemsViewModel now keeps balance unknown until the repository emits. The shared SUP pill renders its existing unknown state; mint/boost/upgrade affordability is false until an actual value exists. The upgrade confirmation shows loading instead of insufficient balance while that value is unknown. Actual repository purchase validation and amounts are unchanged.
- Viewed 0383b12 native screen-09.png: weekly chart now shows Korean weekdays and readable stacked goal/instructions; this does not validate expanded details or enlarged fonts. Native scene-20 previously confirmed the pinned primary action, but its dialog remains unverified.
- Local inventory/design/resource/asset/whitespace checks pass. Build APK 35926757996 for 2f65026 has now completed successfully, allowing the queued notification and balance changes to be pushed without cancelling its release build. Gallery 35926758041 remains live. No build/runtime result is yet available for these new changes.

## 2026-09-24 — notification delivery recovery coverage

- Extracted the actual serialized preference-delivery controller from the Firebase registrar so failures and interleaving can be tested without an Android/Firebase runtime. PushRegistrar uses this controller; there is no separate test-only implementation.
- Added bounded coroutine tests for rejected response and offline failure followed by successful retry, queued requests reading the newest persisted choice after a previous request finishes, and cancellation releasing the mutex without claiming success. Tests use deterministic gates rather than sleep or live service requests.
- Source/design/resource checks pass. These new tests are not executed locally because the Android/JDK toolchain remains unavailable. Build 35926757996 and gallery 35926758041 for the preceding pushed revision are still running; do not attribute their eventual results to this local change. Real server, account transition, push delivery and whole-app validation remain outstanding.

## 2026-09-24 — distinguish local notification choices from server acknowledgement

- Push preference uploads now expose Sending/Synced/Pending based on the actual ServerResult, including transport exceptions and cancellation. Previously returned server failures were ignored. The existing serialized latest-value delivery and startup/login retry remain intact.
- Notification settings explicitly say saved on this phone after DataStore succeeds. A failed/unconfirmed server send shows an explanation that previous settings may still apply and offers retry; an active send has a separate progress message. All four locales have equivalent copy. This does not claim actual notification delivery or OS permission/channel success.
- Local inventory, design contract, resource parity, asset and whitespace checks pass. Native compilation, offline/retry interaction and server acknowledgement need execution. Changes remain local while Build APK 35926757996 for 2f65026 is running, to avoid cancelling its release build. Gallery 35926758041 was queued when checked.

## 2026-09-24 — native assertions now reach the upgrade dialog

- Revalidated 0383b12: Build APK 35925659041 succeeded. Gallery 35925659229 failed on both APIs, but neither emulator disappeared in this execution. API 34 interaction XML reports 19 tests/0 failures; API 35 reports 19/1, the keyboard visibility wait in CrewFormTest. This single run does not prove renderer stability.
- Both galleries reached scene 20 and failed the upgrade-cost displayed assertion after clicking Enhance. Viewed API 34 screen-20.png: the primary Enhance button is visibly pinned above navigation. The runner's failure screenshot shows the launcher after test teardown, so it cannot establish the dialog's layout or absence.
- Added a bounded wait for the dialog's separately laid-out window, preserving the required cost visibility assertion. Capture and semantics now run before activity teardown even when that wait fails. Crew keyboard waits likewise capture the actual form in finally without weakening the IME requirement. These changes need execution; they are not a proven dialog or keyboard fix.
- Downloaded artifacts to C:/Users/gana0/StepUp-captures/redesign-0383b12. Local inventory/design/resource/asset/whitespace checks pass. Whole-app visual review, current chart interactions, real services and other outstanding requirements remain incomplete.

## 2026-09-24 — resend persisted notification choices

- PushRegistrar's comment promised a preference retry on next launch, but startup/login only registered the FCM token. Those paths now also resend persisted notification choices independently of token availability, so a failed prior settings upload has a recovery path.
- Preference sends are serialized and read the latest DataStore values inside the lock. Rapid edits cannot queue old captured preference objects behind newer choices. Coroutine cancellation is preserved; local settings survive transport errors. No automatic financial write retry is involved.
- Static checks pass. Server delivery, offline-to-online retry on launch/login and rapid-edit behavior still require runtime validation. The UI's saved message currently confirms local persistence, not server acknowledgement; precise sync-state presentation remains an open item.

## 2026-09-24 — retain partial screenshots before device loss

- 787f31b logs again show emulator offline/disappearance before final adb pulls could retrieve screenshots; post-failure window/ANR dumps are empty when the device is gone. Both explicit-SwiftShader jobs for 0383b12 remain live; do not restart or infer their outcome.
- Added bounded background transport for only the four known screenshot directories into partial-captures. It never retries instrumentation, changes test status, or substitutes partial files for final captures/reports. Transfers are best effort and can catch a file while being written; individual images must be opened before using them as visual evidence.
- Shell syntax and whitespace checks pass. Native capture survival and renderer stability remain unverified. This diagnostic supports the full UI review and does not reduce required device/API coverage.

## 2026-09-24 — chart interaction and accessible values

- Weekly/quarter bars now expose date/range, step count, button role and selected state to accessibility services; previously their visual bars had no readable value. Test tags identify bars and expanded details without depending on translated captions.
- Gallery now clicks a day and a week, checks selection and visible expanded details, captures each, then clicks again and asserts dismissal. This verifies the new progressive disclosure when executed; it is currently pending native execution, not passing evidence.
- Static checks pass. 787f31b gallery has ended in failure; 0383b12 build and explicit-SwiftShader gallery are still active. No full-screen/device verification completion is claimed.

## 2026-09-24 — chart details remain readable when expanded

- Selected day/week details now appear below the chart at full content width instead of 132/150dp overlays that covered bars and constrained enlarged text. Detail labels/values and quarter-chart captions use 14sp; quarter total and instructions are stacked. Day detail weekday formatting follows the app configuration locale.
- Existing selection, totals, goal rates and navigation remain unchanged; safe getOrNull prevents an obsolete quarter selection index from indexing a changed bucket list. Static checks pass. Chart tapping, layout growth and large-font native captures remain pending.

## 2026-09-24 — readable weekly chart labels

- Native analytics capture showed 8–10sp weekday/interaction labels and English weekday initials inside the Korean screen. Weekly chart now resolves weekday names from the app configuration locale, uses 14sp labels with a growing 28dp minimum selection badge, and separates target/instructions below the total to avoid horizontal crowding.
- Step totals, target calculations, chart selection and history are unchanged. Static checks pass; current native layout and enlarged-text review remain pending. Quarter-chart and callout typography still require their own pass.
- dee6cc3 API 34 also lost the emulator with only one failed interaction testcase retained. Thus explicit 4GB guest RAM did not establish stability on either API. The pending explicit SwiftShader experiment is a separate configuration change; no runtime-success claim is supported yet.

## 2026-09-24 — notification permission recovery

- Notification settings now distinguish saved push preferences from Android's app-level notification permission. When push is selected but Android blocks notifications, a localized explanation and shared Open settings action appear. Lifecycle resume rechecks the actual permission after returning from Settings; opening the screen does not prompt for permission.
- Before stored preferences arrive, show loading rather than fabricated enabled switches. Existing local saving/background server sync is unchanged; individual channel configuration and successful remote push delivery remain unverified.
- Static design, four-locale resource, asset and whitespace checks pass. Native denied/granted/resume and enlarged-text captures remain pending. Latest pushed 787f31b build is still active, so these changes and the explicit renderer experiment remain queued locally.

## 2026-09-24 — explicit software renderer experiment

- dee6cc3 API 35 still lost the emulator during the first interaction test. Guest-memory-start.txt confirms 4,014,392 kB total and 3,001,260 kB available at startup; increasing guest RAM alone did not establish stability. API 34 was still running when checked.
- Changed automatic `-gpu software` backend selection to the explicitly supported `-gpu swiftshader`, preserving memory, both API levels and every test. Official Android docs describe software as automatic backend selection and swiftshader as a specific GLES/Vulkan renderer: https://developer.android.com/studio/run/emulator-acceleration . This is an unverified backend comparison, not a proven diagnosis or fix.
- Reviewed e16e1e2 native analytics and notification-settings captures. Analytics still has tiny explanatory labels and a dense summary; notification settings are readable at the captured default size but system permission status/large-font interaction need verification. These older images are not validation of the latest revision.

## 2026-09-24 — persisted appearance across run and wardrobe

- Run/finish no longer initialize with a fabricated default AvatarLook. Running reserves the character area while waiting and keeps timer, metrics and controls independent; result data and Done remain available while art loads.
- Customize and runner market wait for persisted appearance before rendering gender-specific previews or accepting appearance choices. Existing main chrome is owned by the root and remains visible. Profile/challenges already used nullable appearance.
- Design-contract, resource, asset and whitespace checks pass. This closes the identified source-level default-avatar initialization sites, not the cold-start/device verification requirement. Native compilation, loading/error behavior and saved-LUMI relaunch captures remain pending.

## 2026-09-24 — home waits for persisted appearance

- Home no longer emits a fabricated default AvatarLook before reading stored appearance. The character area shows a loading label until actual data arrives; More and start/resume remain accessible. Profile already used nullable appearance; run and wardrobe initial appearance still need the corresponding audit.
- Outfit/shoe equipment feedback now resolves the first persisted appearance after the write instead of copying potentially stale screen state, preserving the actual gender/outfit/shoe combination when checking art availability.
- Static checks pass; cold-start native capture and immediate equipment-switch feedback remain unverified. dee6cc3 build and API 34/35 gallery were still live when checked.

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

## 2026-09-24 — first wardrobe reference replacement

- b018ca5 implements the large wardrobe stage, image-led inventory, shared scene controls and a separate random background pool. Source design/string/asset checks passed.
- Native capture run 35941393573 passed on Android 14 and 15. First visual review found the second grid row slightly clipped and the first display capture taken before the ready frame reached the compositor.
- Follow-up reduces cell height and waits for presentation before capturing. Final native confirmation and APK evidence pending.
- Background compatibility and actual 2D map limitations are recorded in DESIGN-FOUNDATION-KO.md. Pink reference apparel remains a future asset; owned inventory is not fabricated.

- Final app source 1bc6e9b: Build APK run 35941934972 passed (unit tests, debug build/signature check, release compilation). Wardrobe capture run 35941934970 passed on Android 14 and 15; XML reports one test, zero failures/errors/skips per device.
- Confirmation review: six demo cells fit as two rows at normal font size; shoes, More sheet and 1.3 font scaling remain readable. Background changes preserve character bounds. No source changes after this confirmation.
- First capture 01 still contains the loading frame despite ready semantics; excluded from loaded-state evidence. Captures 02–07 show the actual loaded UI. First-entry frame timing is not established by this test. This is a wardrobe checkpoint, not a full regression pass.
- Comparison board: wardrobe-review.html. Unedited selected captures and provenance: captures/. Design foundation and actual map constraints: DESIGN-FOUNDATION-KO.md.
- Pink/pastel reference apparel, remaining screens, additional background families and automatic timed/server-driven background delivery remain subsequent stages.

## 2026-09-24 — second wardrobe asset checkpoint

- Produced nine separate transparent PNGs with built-in ImageGen: pink, lavender and olive; each has LUMI, RUNO and a product thumbnail. Originals were copied without pixel editing. Alpha and dimensions were checked and recorded in design/redesign-2026-09/stage2/manifest.json with the exact prompts alongside it.
- Added the three design samples only to existing demo/trial selection. The owned/NFT catalog remains unchanged. Exact starter-shoe combinations have both genders; mismatched shoe art has an explicit preview note.
- First six trial grid cells follow the reference palette order. Character placement continues to use measured source alpha and the shared foot anchor; backgrounds stay separate.
- Source design/string/asset checks passed. App compilation and native verification are pending for this source revision. Updated wardrobe capture to enter through the real Home tab and exercise all three new outfits for both genders.
- Pose/background compatibility and subsequent work are documented in STAGE-2-ASSET-GUIDE-KO.md. This checkpoint is not every running/seated pose or whole-app redesign completion.

- First stage-2 review at 2d8efb6: build 35944250583 and capture 35944250553 passed (Android 14/15, one wardrobe test each, zero failures/errors/skips). All six new character outfits render; Home-to-wardrobe entry is now captured loaded on both devices. Native review found the night background places feet over water, so it is excluded from the wardrobe pool. Terrace and sunset remain. Short native equip toasts obscured some inventory captures; capture timing now waits for them to clear. This one visual correction requires fresh same-source build/capture confirmation.

- Final stage-2 app source **44e2367**: Build APK **35945052289 passed** (unit tests, debug build/stable signature, unsigned release/R8 compilation); wardrobe capture **35945052211 passed** on Android 14 and 15. Downloaded XML confirms one test, zero failures/errors/skips per device.
- Native confirmation: pink/lavender/olive render at the common foot anchor; two grid rows fit at normal type size; the background switch retains the outfit and shows ground underfoot. The final lavender/olive captures no longer have an equip toast over the inventory. Home-to-wardrobe first entry was captured loaded on both devices in the preceding 2d8efb6 review; no production loading behavior was altered.
- Wardrobe now admits only terrace/sunset scenery; a restored setting removed from the pool falls back to terrace. The unchanged night background remains available to its other existing callers, but is not wardrobe-compatible.
- Final native originals/provenance: captures-stage2/. Reference comparison and all six outfits: wardrobe-stage2-review.html. Interactive separated asset board: ../../design/redesign-2026-09/stage2/index.html; its image paths and character/background controls were checked in browser.
- The nine new images were produced with built-in ImageGen; source prompts and original alpha geometry remain in the stage2 folder. Older low-resolution clothing, running/seated variants, new shoe colors, new background supply/time rotation, map redesign and other screens remain subsequent work. No main merge or whole-app completion claim.

- Published separate prerelease `redesign-preview-44e2367`: https://github.com/mycyi1994-hash/StepUp/releases/tag/redesign-preview-44e2367 . Asset `StepUp-wardrobe-stage2-44e2367.apk`, 62,748,266 bytes; SHA-256 `0cc6705dd358d1391aee1d3353ff3a09c47fb1680ce5bbc09618709063480f8a`. Remote release target and uploaded asset size were verified. Final screenshot comparison was rendered in the local browser.

## 2026-09-24 — modular design plan correction (documentation only)

- User clarified that independent scenery, controls and reusable design modules must be planned before further screen expansion, across at least 50 remaining screens/states. Replaced the screen-only plan with an asset/layer/component/layout implementation plan.
- Defined 18 proposed component families and 9 layout families; mapped the existing 77 capture items to layout, modules, asset needs and work stage. These are proposed mappings, not completed implementations. Unlisted overlays/permission/error states remain an explicit expansion list.
- Next sequence is 3A parts/state board and layer specifications, 3B a one-gender/one-pose/two-outfit/two-shoe compositing pilot plus representative screens, then 3C home/startup. Existing flattened character assets remain available while compatibility is proved. No bulk generation is authorized by this planning update.
- Current production implementation remains the previously verified 44e2367; this turn changed documentation only. Checked all 77 unique IDs, module/layout references and local document links. No APK build or image generation was run.

## 2026-09-25 — mystery draw and ambient motion source checkpoint

- Added a separate colored gift action between Customize and Community in the shared bottom bar. Its new route shows independent mystery-box art plus native sneaker/tracksuit actions. The older vault shoe mint remains a separate local flow. Both new draw actions are gated by public deployment config, so the current app cannot imply a GIWA transaction happened.
- Added home background arrows with a crossfade, slow scenery-only drift, and subtle grounded character breathing. Reduced-motion settings stop decorative loops. These changes need native capture for foot contact, compact layouts and perceived motion.
- Prepared `MysteryDrawNFT` for the 52-shoe/5-tracksuit catalog, a deterministic signed authorization endpoint, and a wallet transaction page. No contract, Worker or website was deployed. The existing GIWA Sepolia demo contract is not usable for this new draw because its roller key was not retained. SUP cost values are constructor parameters; final pricing remains a deployment decision.
- Local checks: new Solidity contract compiled; 2 focused contract checks and 2 roller checks passed; draw web bundle built; source design/string/asset checks passed. Android compilation, native screenshot review and a real GIWA wallet transaction remain unverified.


## 2026-09-25 — character-free asset replacement

- User selected record-first home and retained shoes/draw. Produced 10 screen mockups, 2 independent landscape PNGs and 1 shoe SVG; review pending with Claude.
- Archived and removed exactly 156 packaged character/clothing/old scene images; preserved 58 existing shoe/brand/box images by hash. New banners and shoe vector are installed as separate resources.
- Removed dead drawable references, character stages and outfit draw entrances; routed the former wardrobe through the existing shoe inventory. Reused existing functional screens; native visual matching is not claimed.
- Static design/string/media/resource checks and web draw bundle build pass. No local Android SDK/JDK: Android compile/device validation and visual regression expectations remain pending. No APK/push/deployment.
- See CHARACTER-FREE-ASSETS-2026-09-25.md for deliverables, exact deletion archive and application limits.

## 2026-09-25 — stage 1: preserve redesign and merge current main

- Saved the character-free source/resource work as checkpoint `30bdce4` before merging `origin/main` (`488ee5d`). This is a local integration checkpoint, not a release.
- Backed up all 118 previously untracked files and verified their post-merge hashes. Pre-existing screenshot/review work remains untouched and untracked; the new runtime assets and asset report are in the checkpoint commit.
- Resolved 115 character-image modify/delete conflicts by preserving the explicitly approved removals. Also copied main's updated image versions into the external sync backup before removing them from runtime resources. Regenerated the sole conflicting source file, `AvatarArtRes.kt`, from the current character-free resource inventory.
- Login contains both main's pending-run upload scheduling fix and the new independent landscape. Main backend, database/server changes and QA workflow/script files match `origin/main` exactly in the merged index.
- Checks passed: no unresolved entries, design contract, localized strings, packaged media, staged diff whitespace, and original untracked-file preservation. Android compile/device tests were not run during this merge-only step.
- `ExperienceUiTest` / `DesignReferenceTest` redesign updates remain the separately planned test stage. No push, APK, deployment or next-stage UI work was performed.
- Backup: `C:/Users/gana0/OneDrive/문서/New project 3/output/stepup-main-sync-20260925-134513`.

## 2026-09-25 — stage 2: character-free native screen layouts

- Implemented the ten mockup destinations as native presentation changes: record home, dedicated shoe selection, draw, active run, run result, community, profile, challenges, login and three-step guide.
- Replaced the wardrobe's temporary ItemsScreen wrapper with a real inventory-backed shoe preview/selection UI. Loading, empty, equipped and save-in-flight states are separate; detail, collection, vault and market stay reachable.
- Restored real GPS maps on active/result screens, moved activity results before settlement information, restored actual profile photos and recent activity, and replaced the single meetup card with upcoming real meetups and joined crews.
- Kept illustrations independent of native labels/buttons/charts, existing reward/deployment gates and main's backend/login upload work. No new generated art, APK, push or deployment.
- Checks: design contract, four-locale strings, packaged media and diff whitespace pass. Local Java/Android SDK unavailable; Android compilation, native captures and device interaction remain pending. ExperienceUiTest and DesignReferenceTest expectations remain stage 3.
- Per-screen evidence and scope: CHARACTER-FREE-STAGE2-2026-09-25-KO.md. Inventory marks source implementation only, not visual acceptance.

## 2026-09-25 — stage 3: character-free device-test expectations

- Updated ExperienceUiTest and DesignReferenceTest for native record/shoe/draw layouts, shared chrome, the profile settings action and three-step guide. Replaced gender/trial-outfit reference scenes with 14 current scenes across 4 viewports; 56 captures planned, none produced locally.
- Added bounded checks for preview-versus-confirmed shoe equipment, draw readiness/callbacks, five navigation destinations and the guide. Updated the dependent MysteryDesignTest to stop waiting for removed character tags/outfit controls.
- Added the explicit redesign runner/workflow selection, preserving main's existing split regression suites. Kept the optional multilingual matrix separate from the default checkpoint to avoid running hundreds of captures for this change.
- Source design/string/media, Bash syntax, scene/method mappings and whitespace checks pass. No local Java/Android SDK, so Android compile/instrumentation remains unexecuted and is the next stage. No APK/push/remote run.
- Evidence and exact limits: CHARACTER-FREE-STAGE3-2026-09-25-KO.md. Legacy full-gallery/wardrobe character fixtures outside these three test classes remain historical coverage, not proof of the new design.

## 2026-09-25 — stage 4: native build and device evidence

- Verified runtime/test source `6e327f6` on the redesign branch. Build run `36099618225` passed unit tests, debug/signature validation and unsigned release R8 build. Gallery run `36099618150` passed on Android 14 and 15: five interaction tests and one 56-capture reference test per OS.
- Fixed stale character-asset expectations and instrumentation compilation errors exposed by the first CI attempt. Fixed community capture lookup through merged accessibility nodes without changing production repositories.
- Actual compact captures exposed oversized home/shoe previews and banner controls overlapping copy. Compacted record/goal spacing, used a horizontal shoe preview for normal text, sized the banner to available height, and moved scenery arrows above the copy. Large text remains scrollable.
- Collected 112 reference PNGs (56 viewport/state combinations on each OS), original result XML, raw text-overflow signals and logs. Prepared a local original-image gallery plus normal/large-text contact sheets. Evidence and known limitations: CHARACTER-FREE-STAGE4-2026-09-25-KO.md.
- Source design/string/media checks pass. This is primary-scene/device evidence, not approval of every secondary screen, every raw text-overflow signal, all locales/themes, real outdoor GPS or wallet/backend transactions. Claude visual review is next. No main merge, public release upload or deployment.

## 2026-09-25 — stage 5: Claude visual review returned

- Prepared a portable reference/capture comparison package: 10 mockups, 112 native captures, eight source snapshots, evidence and mappings. Executed Claude Code with read/search tools only in the package directory and received an actual review; this is not a simulated Claude verdict.
- Verdict: changes requested. Six findings consolidate into four required layout groups (large-text navigation, compact running, profile density/duplicate entry, community sign-in notice) and one optional banner-spacing polish. No app-source changes in this stage; verified `app/src` still matches `6e327f6`.
- Preserved Claude's original report and separately corrected its five-versus-six-slot calculation and accessibility/regression risks in the proposed remedies. Do not blindly shrink navigation text to 11sp or restore centered banner arrows.
- See CHARACTER-FREE-STAGE5-2026-09-25-KO.md and CLAUDE-VISUAL-REVIEW-2026-09-25.md. The review is bounded primary-screen coverage, not a whole-app release approval. Next: apply the four required layout groups and recapture the affected screens. No new CI/APK, main merge or public release.

## 2026-09-25 — stage 6: required visual-review fixes verified

- Implemented the four required groups: measured navigation label widths with a centered draw action and unchanged font scaling; distinct compact breakpoints for active/result run statistics and a shorter map on low-height screens; a horizontal profile identity and one records entry; a compact community sign-in notice reusing the existing authentication action.
- Added focused layout assertions to the existing capture test for single-line native labels, centered draw action, complete active-map bounds above controls, visible profile-records access and meetup-title bounds. Retained scene captures when collecting layout assertion failures.
- First run exposed Android 15 map/control clearance; Android 14 captures also exposed a displaced draw action. Corrected both. Final source `29bb16b` passed build run `36102357282` and device run `36102357154`: five interaction tests plus 56 captures on each of Android 14/15, all layout checks PASS.
- Claude read-only recheck of seven final originals judged all four original groups resolved with no remaining blockers within that scope. Preserved original response, before/after board, 112 original captures and execution evidence. See CHARACTER-FREE-STAGE6-2026-09-25-KO.md and CLAUDE-VISUAL-RECHECK-2026-09-25.md.
- Source design/string/asset checks pass. No new image assets, backend/data changes, main merge or public release. Selected map captures show the tile-loading grid; real tile loading, outdoor GPS, auth/transactions, secondary screens, all locales/themes and optional banner polish remain separate coverage.


## 2026-09-25 — character-free stage 7: secondary forms and settings

- Covered 10 routes: post/crew creation, meetup detail, and 7 settings destinations. Shared FormPage; native labels above community form fields; full-width preference descriptions; independent meetup scenery banner and responsive detail information.
- App source be6d0ef passed unit/debug/signature/release checks (36105578228). Test-only correction 7f2df93 passed Android 14/15: 4 form tests + 1 gallery test per OS, 36 gallery images each (36106239083). Final artifacts and XML/hash evidence under stage7 output/verified-api34 and verified-api35.
- Claude reviewed 8 intermediate images, requested floating-label polish, then confirmed both label and empty-banner corrections from 4 final app images. Original review and recheck preserved. Optional wording/alignment and keyboard-header spacing notes remain separate.
- Earlier failed/cancelled attempts remain documented, not counted as passes. No public APK, main merge, account/storage identity change, server mutation, or full release-completion claim. See CHARACTER-FREE-STAGE7-2026-09-25-KO.md.

## 2026-09-25 — bug sweep (PR #25): run finish states and SUP amounts

- Run finish card: a run with no counted steps is never uploaded, so it now shows `finish_no_steps` ("걸음이 잡히지 않아 이번 러닝은 적립 없음") instead of staying on "서버 확인 중" forever. Party result hides the phone-estimated steps/boost line until the server-confirmed amount arrives. No layout change; existing headline slot and styles reused.
- SUP balance text (SupPill, wallet card, rewards header, finish balance, profile, ranking teaser) now rounds down via `formatSupDown` — 499.6 is no longer shown as 500 next to a 500 SUP price. Same fonts, sizes and positions.
- Checks: `python3 scripts/check-strings.py` (new string in values/ko/ja/zh), `./gradlew compileDebugKotlin testDebugUnitTest lintDebug compileDebugAndroidTestKotlin`, and the Experience QA device suite on the PR. No new captures were taken for these two states; they reuse existing finish-card and header layouts.

## 2026-09-26 — S2 planning and asset handoff for Claude

- Added `docs/redesign/s2/` as the current entry point: Korean plan, confirmed/proposed/open decisions, truthful status, 45-frame map, 34-route preservation map, a six-screen before/after HTML preview and a Claude session-start request.
- Added `design/s2/`: 17 hash-verified originals, 8 alpha-trimmed derivatives, 13 individual Figma image-layer renders and portable source metadata. Four unavailable composite assets have explicit fallback proposals; the user chose to proceed with available images after export was not allowed.
- Added pointers to the root guidance and historical handoffs so old character/wardrobe directions do not override S2. The HTML comparison is not a final approved specification or an Android S2 build; small comparison images are not production assets.
- Checked asset hashes and referenced files, complete route/frame coverage, local documentation links and preview asset/font paths; no signed download URLs in the shipped JSON. App source is unchanged. No new APK, device tests or deployment in this documentation checkpoint.
- The user subsequently requested committing and pushing all handoff assets for remote Claude work. This package targets `origin/codex/stepup-cohesive-redesign`; remote users must check out that branch. The delivery response records the final commit and remote verification.

## 2026-09-26 — S2 batch 1: shared parts, home, shoes, active run

- New `ui/components/S2Parts.kt` (stage backdrop, kicker/headline/subtitle, light big numbers, arch scenery, white round action, side info, stat row). Dark `night` is now S2 `#05080E`; tab selection is white label + short blue bar. Tab set unchanged (draw slot pending the user's decision).
- Home, Shoes and active Run follow S2 composition with existing data and actions only; all existing test tags kept. Scenery arrows cycle the six S2 backgrounds (decorative, not weather). Shoe art stays on the 52 catalog assets.
- Checks: compileDebugKotlin, testDebugUnitTest, lintDebug, compileDebugAndroidTestKotlin, check-strings, check_experience_assets. Device captures come from the PR's Experience QA run.

